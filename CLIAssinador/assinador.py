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
from installers.assinador import get_assinador_path, assinador_steps
from installers.java import get_java_executable, java_steps

APP_DIR = Path.home() / ".assinador"
LOG_DIR = APP_DIR / "logs"

ASSINADOR_HOST = "127.0.0.1"
ASSINADOR_PORT_SERVIDOR = 9042
STATUS_PATH = "/api/info"
SHUTDOWN_PATH = "/shutdown"
SIGN_PATH = "/api/sign"
VALIDATE_PATH = "/api/validate"
STARTUP_TIMEOUT_SEC = 6.0
POLL_INTERVAL_SEC = 0.5

_current_mode: str | None = None
_current_port: int | None = None
_process: subprocess.Popen[bytes] | None = None


def start_assinador(mode: str = "servidor", port: int | None = None) -> bool:
    global _current_mode, _current_port, _process

    if mode not in {"local", "servidor"}:
        print("Modo deve ser 'local' ou 'servidor'.")
        return False

    _current_mode = mode

    if mode == "local":
        _current_port = None
        ok = run_steps(java_steps())
        if ok:
            ok = run_steps(assinador_steps())
        if not ok:
            print("Falha ao preparar o Java ou o assinador.")
            return False

        java_exe = get_java_executable()
        assinador_path = get_assinador_path()
        if java_exe is None or assinador_path is None or not assinador_path.exists():
            print("Nao foi possivel localizar o Java ou o assinador.")
            return False

        try:
            _process = _launch_assinador(java_exe, assinador_path, mode, None)
        except (OSError, subprocess.SubprocessError) as exc:
            print(f"Falha ao iniciar o assinador: {exc}")
            return False

        print("Assinador iniciado no modo local.")
        return True

    else:
        _current_port = port or ASSINADOR_PORT_SERVIDOR

        if not _is_port_available(_current_port):
            info = _request("GET", STATUS_PATH)
            if info is not None:
                status, body = info
                if 200 <= status < 300 and _is_assinador_response(body):
                    print("Assinador ja esta em execucao.")
                    _print_status(info)
                    return True
                print(f"Porta {_current_port} em uso por outro serviço.")
                if body:
                    print(body)
                return False
            print(f"Porta {_current_port} em uso. Encerrando.")
            return False

        ok = run_steps(java_steps())
        if ok:
            ok = run_steps(assinador_steps())
        if not ok:
            print("Falha ao preparar o Java ou o assinador.")
            return False

        java_exe = get_java_executable()
        assinador_path = get_assinador_path()
        if java_exe is None or assinador_path is None or not assinador_path.exists():
            print("Nao foi possivel localizar o Java ou o assinador.")
            return False

        try:
            _process = _launch_assinador(java_exe, assinador_path, mode, _current_port)
        except (OSError, subprocess.SubprocessError) as exc:
            print(f"Falha ao iniciar o assinador: {exc}")
            return False

        if _wait_for_info():
            print("Assinador iniciado.")
            return True

        _terminate_process(_process)
        print("Assinador iniciado, mas ainda nao respondeu em /api/info. Processo encerrado.")
        return False


def stop_assinador() -> bool:
    response = _request("POST", SHUTDOWN_PATH)
    if response is None:
        print("Assinador nao esta em execucao.")
        return False

    status, body = response
    if 200 <= status < 300:
        print("Solicitacao de encerramento enviada.")
        if body:
            print(body)
        return True

    print(f"Falha ao encerrar assinador (HTTP {status}).")
    if body:
        print(body)
    return False


def status_assinador() -> bool:
    response = _request("GET", STATUS_PATH)
    if response is None:
        print("Assinador nao esta em execucao.")
        return False
    status, body = response
    if 200 <= status < 300 and _is_assinador_response(body):
        _print_status(response)
        return True

    print("A porta esta ocupada.")
    if body:
        print(body)
    return False


def sign_assinador(arquivo: str) -> bool:
    arquivo_path = Path(arquivo)
    if not arquivo_path.exists():
        print(f"Arquivo nao encontrado: {arquivo}")
        return False

    if not arquivo_path.suffix.lower() == ".json":
        print("O arquivo deve ser um JSON.")
        return False

    try:
        with open(arquivo_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except (json.JSONDecodeError, IOError) as exc:
        print(f"Erro ao ler arquivo: {exc}")
        return False

    response = _request("POST", SIGN_PATH, json.dumps(data))
    if response is None:
        print("Assinador nao esta em execucao.")
        return False

    status, body = response
    if 200 <= status < 300:
        print("Arquivo assinado com sucesso.")
        if body:
            print(body)
        return True

    print(f"Falha ao assinar arquivo (HTTP {status}).")
    if body:
        print(body)
    return False


def validate_assinador(arquivo: str) -> bool:
    arquivo_path = Path(arquivo)
    if not arquivo_path.exists():
        print(f"Arquivo nao encontrado: {arquivo}")
        return False

    if not arquivo_path.suffix.lower() == ".json":
        print("O arquivo deve ser um JSON.")
        return False

    try:
        with open(arquivo_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except (json.JSONDecodeError, IOError) as exc:
        print(f"Erro ao ler arquivo: {exc}")
        return False

    response = _request("POST", VALIDATE_PATH, json.dumps(data))
    if response is None:
        print("Assinador nao esta em execucao.")
        return False

    status, body = response
    if 200 <= status < 300 and _is_valid_response(body):
        print("Arquivo validado com sucesso.")
        if body:
            print(body)
        return True

    print(f"Falha ao validar arquivo (HTTP {status}).")
    if body:
        print(body)
    return False


def _launch_assinador(java_exe: Path, assinador_path: Path, mode: str, port: int | None) -> subprocess.Popen[bytes]:
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    log_path = LOG_DIR / "assinador.log"

    if mode == "local":
        cmd = [str(java_exe), "-jar", str(assinador_path), "--mode", "local"]
    else:
        cmd = [str(java_exe), "-jar", str(assinador_path), "--mode", "servidor", "--port", str(port)]

    with open(log_path, "a", encoding="utf-8") as handle:
        return subprocess.Popen(
            cmd,
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


def _request(method: str, path: str, body: str | None = None) -> Optional[Tuple[int, str]]:
    if _current_mode == "local" or _current_port is None:
        return None

    for scheme in ("https", "http"):
        url = f"{scheme}://{ASSINADOR_HOST}:{_current_port}{path}"
        context = ssl._create_unverified_context() if scheme == "https" else None
        request = urllib.request.Request(url, method=method)
        if body is not None:
            request.add_header("Content-Type", "application/json")
            request.data = body.encode("utf-8")
        try:
            with urllib.request.urlopen(request, timeout=3, context=context) as response:
                response_body = response.read().decode("utf-8", errors="replace")
                return response.status, response_body
        except urllib.error.HTTPError as exc:
            response_body = exc.read().decode("utf-8", errors="replace")
            return exc.code, response_body
        except OSError:
            continue
    return None


def _is_port_available(port: int) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        try:
            sock.bind((ASSINADOR_HOST, port))
        except OSError:
            return False
    return True


def _wait_for_info() -> bool:
    deadline = time.monotonic() + STARTUP_TIMEOUT_SEC
    while time.monotonic() < deadline:
        response = _request("GET", STATUS_PATH)
        if response is not None and 200 <= response[0] < 300 and _is_assinador_response(response[1]):
            return True
        time.sleep(POLL_INTERVAL_SEC)
    return False


def _print_status(info: Tuple[int, str]) -> None:
    status, body = info
    print(f"Assinador ativo (HTTP {status}).")
    if body:
        print(body)


def _is_assinador_response(body: str) -> bool:
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
        and name.casefold() == "hubsaúde assinador".casefold()
        and isinstance(version, str)
        and bool(version.strip())
    )


def _is_valid_response(body: str) -> bool:
    if not body.strip():
        return False

    try:
        payload = json.loads(body)
    except json.JSONDecodeError:
        return False

    return isinstance(payload, dict) and isinstance(payload.get("valid"), bool)
