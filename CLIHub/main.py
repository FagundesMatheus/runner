from __future__ import annotations

import shlex

from simulador import start_simulator, status_simulator, stop_simulator

PROMPT = "runner> "
SESSION_STARTED_SIMULATOR = False


def main() -> None:
    global SESSION_STARTED_SIMULATOR
    try:
        run_repl()
    finally:
        if SESSION_STARTED_SIMULATOR:
            stop_simulator()
            SESSION_STARTED_SIMULATOR = False


def run_repl() -> None:
    print("CLI HubSaude - Digite 'help' para comandos, 'exit' para sair.")
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

        if command == "simulador":
            _handle_simulador(args)
            continue

        print("Comando desconhecido. Digite 'help'.")


def _print_help() -> None:
    print("Comandos disponiveis:")
    print("  simulador start   Inicia o simulador.")
    print("  simulador stop    Encerra o simulador.")
    print("  simulador status  Exibe o status do simulador.")
    print("  help              Mostra esta ajuda.")
    print("  exit              Encerra o CLI.")


def _handle_simulador(args: list[str]) -> None:
    global SESSION_STARTED_SIMULATOR

    if not args:
        print("Uso: simulador start|stop|status")
        return

    action = args[0].lower()
    if len(args) > 1:
        print("Comando nao aceita argumentos adicionais.")
        return

    if action == "start":
        started = start_simulator()
        if started:
            SESSION_STARTED_SIMULATOR = True
        return

    if action == "stop":
        stopped = stop_simulator()
        if stopped:
            SESSION_STARTED_SIMULATOR = False
        return

    if action == "status":
        status_simulator()
        return

    print("Subcomando invalido. Use start, stop ou status.")


if __name__ == "__main__":
	main()
