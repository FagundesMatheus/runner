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
STATUS_PATH: str | None = None  # A definir: ex. /api/info
SHUTDOWN_PATH: str | None = None  # A definir: ex. /shutdown
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
        _process = None
        if not _prepare_assinador():
            return False
        print("Assinador preparado no modo local.")
        return True

    _current_port = port if port is not None else ASSINADOR_PORT_SERVIDOR

    if not _is_port_available(_current_port):
        if _is_assinador_on_port(_current_port):
            print(f"Assinador ja esta em execucao na porta {_current_port}.")
            return True
        if _is_port_listening(_current_port):
            print(f"Porta {_current_port} em uso por outro servico.")
            return False
        print(f"Porta {_current_port} em uso. Encerrando.")
        return False

    if not _prepare_assinador():
        return False

    java_exe = get_java_executable()
    assinador_path = get_assinador_path()
    assert java_exe is not None and assinador_path is not None

    try:
        _process = _launch_servidor(java_exe, assinador_path, _current_port)
    except (OSError, subprocess.SubprocessError) as exc:
        print(f"Falha ao iniciar o assinador: {exc}")
        return False

    if _wait_for_port(_current_port):
        print(f"Assinador iniciado na porta {_current_port}.")
        return True

    _terminate_process(_process)
    _process = None
    print(
        f"Assinador iniciado, mas a porta {_current_port} "
        "nao respondeu a tempo. Processo encerrado."
    )
    return False


def stop_assinador() -> bool:
    global _process, _current_mode, _current_port

    if _current_mode == "local":
        _current_mode = None
        print("Sessao local encerrada.")
        return True

    if _current_mode != "servidor":
        print("Assinador nao esta em execucao.")
        return False

    port = _current_port
    if port is None:
        print("Assinador nao esta em execucao.")
        return False

    if SHUTDOWN_PATH:
        response = _request("POST", SHUTDOWN_PATH)
        if response is not None:
            status, body = response
            if 200 <= status < 300:
                print("Solicitacao de encerramento enviada.")
                if body:
                    print(body)
                _process = None
                _current_mode = None
                _current_port = None
                return True

            print(f"Falha ao encerrar assinador (HTTP {status}).")
            if body:
                print(body)
            return False

    if _process is not None and _process.poll() is None:
        _terminate_process(_process)
        _process = None
        _current_mode = None
        _current_port = None
        print(f"Assinador encerrado (porta {port}).")
        return True

    print("Assinador nao esta em execucao.")
    return False


def status_assinador() -> bool:
    if _current_mode is None:
        print("Execute 'assinador start' antes de consultar o status.")
        return False

    if _current_mode == "local":
        print("Assinador pronto no modo local.")
        return True

    port = _current_port
    if port is None:
        print("Porta do assinador nao configurada.")
        return False

    if not _is_assinador_on_port(port):
        if _is_port_listening(port):
            print(f"Porta {port} em uso por outro servico.")
        else:
            print("Assinador nao esta em execucao.")
        return False

    if STATUS_PATH:
        response = _request("GET", STATUS_PATH)
        if response is not None:
            status, body = response
            if 200 <= status < 300 and _is_assinador_response(body):
                _print_status(response)
                return True

    print(f"Assinador ativo na porta {port}.")
    return True


def sign_assinador(arquivo: str) -> bool:
    arquivo_path = _validate_json_file(arquivo)
    if arquivo_path is None:
        return False

    if _current_mode is None:
        print("Execute 'assinador start' antes de assinar.")
        return False

    if _current_mode == "local":
        return _run_java_local("--sign", str(arquivo_path.resolve()))

    if _current_port is None or not _is_assinador_on_port(_current_port):
        print("Assinador nao esta em execucao.")
        return False

    try:
        with open(arquivo_path, "r", encoding="utf-8") as handle:
            data = json.load(handle)
    except (json.JSONDecodeError, OSError) as exc:
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
    arquivo_path = _validate_json_file(arquivo)
    if arquivo_path is None:
        return False

    if _current_mode is None:
        print("Execute 'assinador start' antes de validar.")
        return False

    if _current_mode == "local":
        return _run_java_local("--validate", str(arquivo_path.resolve()))

    if _current_port is None or not _is_assinador_on_port(_current_port):
        print("Assinador nao esta em execucao.")
        return False

    try:
        with open(arquivo_path, "r", encoding="utf-8") as handle:
            data = json.load(handle)
    except (json.JSONDecodeError, OSError) as exc:
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


def _prepare_assinador() -> bool:
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

    return True


def _validate_json_file(arquivo: str) -> Path | None:
    arquivo_path = Path(arquivo)
    if not arquivo_path.exists():
        print(f"Arquivo nao encontrado: {arquivo}")
        return None

    if arquivo_path.suffix.lower() != ".json":
        print("O arquivo deve ser um JSON.")
        return None

    return arquivo_path


def _run_java_local(*args: str) -> bool:
    if not _prepare_assinador():
        return False

    java_exe = get_java_executable()
    assinador_path = get_assinador_path()
    assert java_exe is not None and assinador_path is not None

    cmd = [str(java_exe), "-jar", str(assinador_path), "--mode", "local", *args]
    try:
        result = subprocess.run(cmd, check=False)
    except OSError as exc:
        print(f"Falha ao executar o assinador: {exc}")
        return False

    return result.returncode == 0


def _launch_servidor(
    java_exe: Path, assinador_path: Path, port: int
) -> subprocess.Popen[bytes]:
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    log_path = LOG_DIR / "assinador.log"
    cmd = [
        str(java_exe),
        "-jar",
        str(assinador_path),
        "--mode",
        "servidor",
        "--port",
        str(port),
    ]

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
    if _current_mode != "servidor" or _current_port is None:
        return None

    return _request_at_port(method, path, _current_port, body)


def _request_at_port(
    method: str, path: str, port: int, body: str | None = None
) -> Optional[Tuple[int, str]]:
    for scheme in ("https", "http"):
        url = f"{scheme}://{ASSINADOR_HOST}:{port}{path}"
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


def _is_port_listening(port: int) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.settimeout(1)
        try:
            sock.connect((ASSINADOR_HOST, port))
        except OSError:
            return False
    return True


def _is_assinador_on_port(port: int) -> bool:
    if _process is not None and _process.poll() is None:
        return True

    if STATUS_PATH:
        response = _request_at_port("GET", STATUS_PATH, port)
        if response is not None and 200 <= response[0] < 300 and _is_assinador_response(response[1]):
            return True

    response = _request_at_port("GET", SIGN_PATH, port)
    return response is not None and response[0] == 405


def _wait_for_port(port: int) -> bool:
    deadline = time.monotonic() + STARTUP_TIMEOUT_SEC
    while time.monotonic() < deadline:
        if _process is not None and _process.poll() is not None:
            return False
        if _is_assinador_on_port(port):
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

    if not isinstance(payload, dict):
        return False

    return payload.get("valid") is True
