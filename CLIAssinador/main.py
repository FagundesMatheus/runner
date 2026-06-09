from __future__ import annotations

import shlex

from assinador import (
    sign_assinador,
    start_assinador,
    status_assinador,
    stop_assinador,
    validate_assinador,
)

PROMPT = "runner> "
SESSION_STARTED = False
DEFAULT_MODE = "servidor"
DEFAULT_PORT = 9742


def main() -> None:
    global SESSION_STARTED
    try:
        run_repl()
    finally:
        if SESSION_STARTED:
            stop_assinador()
            SESSION_STARTED = False


def run_repl() -> None:
    print("CLI Assinador - Digite 'help' para comandos, 'exit' para sair.")
    while True:
        try:
            line = input(PROMPT).strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break

        if not line:
            continue

        try:
            parts = shlex.split(line)
        except ValueError:
            print("Entrada invalida. Verifique as aspas.")
            continue

        command = parts[0].lower()
        args = parts[1:]

        if command in {"exit", "quit"}:
            break

        if command in {"help", "?"}:
            _print_help()
            continue

        if command == "assinador":
            _handle_assinador(args)
            continue

        print("Comando desconhecido. Digite 'help'.")


def _print_help() -> None:
    print("Comandos disponiveis:")
    print("  assinador start [local|servidor [porta]]  Inicia o assinador.")
    print("  assinador stop                            Encerra o assinador.")
    print("  assinador status                          Exibe o status do assinador.")
    print("  assinador sign <arquivo.json>             Assina um arquivo JSON.")
    print("  assinador validate <arquivo.json>         Valida um arquivo JSON.")
    print("  help              Mostra esta ajuda.")
    print("  exit              Encerra o CLI.")


def _handle_assinador(args: list[str]) -> None:
    global SESSION_STARTED

    if not args:
        print("Uso: assinador start|stop|status")
        return

    action = args[0].lower()

    if action == "start":
        start_args = args[1:]
        mode, port = _parse_start_args(start_args)
        if mode is None:
            return

        if port is None:
            started = start_assinador(mode)
        else:
            started = start_assinador(mode, port)

        if started:
            SESSION_STARTED = True
        return

    if action == "stop":
        stopped = stop_assinador()
        if stopped:
            SESSION_STARTED = False
        return

    if action == "status":
        status_assinador()
        return

    if action == "sign":
        if len(args) != 2:
            print("Uso: assinador sign <arquivo.json>")
            return

        sign_assinador(args[1])
        return

    if action == "validate":
        if len(args) != 2:
            print("Uso: assinador validate <arquivo.json>")
            return

        validate_assinador(args[1])
        return

    print("Subcomando invalido. Use start, stop ou status.")


def _parse_start_args(args: list[str]) -> tuple[str | None, int | None]:
    if not args:
        return DEFAULT_MODE, DEFAULT_PORT

    if len(args) > 2:
        print("Comando nao aceita argumentos adicionais.")
        return None, None

    mode = args[0].lower()
    if mode not in {"local", "servidor"}:
        print("Uso: assinador start [local|servidor [porta]]")
        return None, None

    if mode == "local":
        if len(args) > 1:
            print("O modo local nao aceita porta.")
            return None, None
        return mode, None

    if len(args) == 1:
        return mode, DEFAULT_PORT

    try:
        port = int(args[1])
    except ValueError:
        print("A porta deve ser um numero inteiro valido.")
        return None, None

    if not (1 <= port <= 65535):
        print("A porta deve estar entre 1 e 65535.")
        return None, None

    return mode, port


if __name__ == "__main__":
    main()
