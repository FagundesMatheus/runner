import typer

from core import run_steps
from installers.hub import hub_steps
from installers.java import java_steps

app = typer.Typer(add_completion=False, no_args_is_help=False)


@app.callback(invoke_without_command=True)
def root(ctx: typer.Context) -> None:
	if ctx.invoked_subcommand is not None:
		return

	ok = run_steps(java_steps(), verbose=False)
	if ok:
		ok = run_steps(hub_steps(), verbose=False)

	if not ok:
		raise typer.Exit(code=1)


def main() -> None:
	app()


if __name__ == "__main__":
	main()
