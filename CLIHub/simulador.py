from __future__ import annotations

import socket
import ssl
import json
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Optional, Tuple

from core import run_steps
from installers.hub import get_hub_path, hub_steps
from installers.java import get_java_executable, java_steps

APP_DIR = Path.home() / ".hubsaude"
LOG_DIR = APP_DIR / "logs"

SIMULATOR_HOST = "127.0.0.1"
SIMULATOR_PORT = 8443
STATUS_PATH = "/api/info"
SHUTDOWN_PATH = "/shutdown"
STARTUP_TIMEOUT_SEC = 6.0
POLL_INTERVAL_SEC = 0.5


def start_simulator() -> bool:
    if not _is_port_available():
        info = _request("GET", STATUS_PATH)
        if info is not None:
            status, body = info
            if 200 <= status < 300 and _is_simulator_response(body):
                print("Simulador ja esta em execucao.")
                _print_status(info)
                return True
            print("Porta 8443 em uso por outro serviço.")
            if body:
                print(body)
            return False
        print("Porta 8443 em uso. Encerrando.")
        return False

    ok = run_steps(java_steps())
    if ok:
        ok = run_steps(hub_steps())
    if not ok:
        print("Falha ao preparar o Java ou o simulador.")
        return False

    java_exe = get_java_executable()
    hub_path = get_hub_path()
    if java_exe is None or hub_path is None or not hub_path.exists():
        print("Nao foi possivel localizar o Java ou o simulador.")
        return False

    try:
        process = _launch_simulator(java_exe, hub_path)
    except (OSError, subprocess.SubprocessError) as exc:
        print(f"Falha ao iniciar o simulador: {exc}")
        return False

    if _wait_for_info():
        print("Simulador iniciado.")
        return True

    _terminate_process(process)
    print("Simulador iniciado, mas ainda nao respondeu em /api/info. Processo encerrado.")
    return False


def stop_simulator() -> bool:
    response = _request("POST", SHUTDOWN_PATH)
    if response is None:
        print("Simulador nao esta em execucao.")
        return False

    status, body = response
    if 200 <= status < 300:
        print("Solicitacao de encerramento enviada.")
        if body:
            print(body)
        return True

    print(f"Falha ao encerrar simulador (HTTP {status}).")
    if body:
        print(body)
    return False


def status_simulator() -> bool:
    response = _request("GET", STATUS_PATH)
    if response is None:
        print("Simulador nao esta em execucao.")
        return False
    status, body = response
    if 200 <= status < 300 and _is_simulator_response(body):
        _print_status(response)
        return True

    print("A porta 8443 esta ocupada.")
    if body:
        print(body)
    return False


def _launch_simulator(java_exe: Path, hub_path: Path) -> subprocess.Popen[bytes]:
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    log_path = LOG_DIR / "simulador.log"
    with open(log_path, "a", encoding="utf-8") as handle:
        return subprocess.Popen(
            [str(java_exe), "-jar", str(hub_path)],
            stdin=subprocess.DEVNULL,
            stdout=handle,
            stderr=handle,
        )


def _terminate_process(process: subprocess.Popen[bytes]) -> None:
    if process.poll() is not None:
        return

    try:
        process.terminate()
        process.wait(timeout=5)
    except (OSError, subprocess.TimeoutExpired):
        try:
            process.kill()
            process.wait(timeout=5)
        except (OSError, subprocess.TimeoutExpired):
            pass


def _request(method: str, path: str) -> Optional[Tuple[int, str]]:
    for scheme in ("https", "http"):
        url = f"{scheme}://{SIMULATOR_HOST}:{SIMULATOR_PORT}{path}"
        context = ssl._create_unverified_context() if scheme == "https" else None
        request = urllib.request.Request(url, method=method)
        try:
            with urllib.request.urlopen(request, timeout=3, context=context) as response:
                body = response.read().decode("utf-8", errors="replace")
                return response.status, body
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            return exc.code, body
        except OSError:
            continue
    return None


def _is_port_available() -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        try:
            sock.bind((SIMULATOR_HOST, SIMULATOR_PORT))
        except OSError:
            return False
    return True


def _wait_for_info() -> bool:
    deadline = time.monotonic() + STARTUP_TIMEOUT_SEC
    while time.monotonic() < deadline:
        response = _request("GET", STATUS_PATH)
        if response is not None and 200 <= response[0] < 300 and _is_simulator_response(response[1]):
            return True
        time.sleep(POLL_INTERVAL_SEC)
    return False


def _print_status(info: Tuple[int, str]) -> None:
    status, body = info
    print(f"Simulador ativo (HTTP {status}).")
    if body:
        print(body)


def _is_simulator_response(body: str) -> bool:
    if not body.strip():
        return False

    try:
        payload = json.loads(body)
    except json.JSONDecodeError:
        return False

    if not isinstance(payload, dict):
        return False

    name = payload.get("name")
    version = payload.get("version")
    return (
        isinstance(name, str)
        and name.casefold() == "hubsaúde simulador".casefold()
        and isinstance(version, str)
        and bool(version.strip())
    )
