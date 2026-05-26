from dataclasses import dataclass
from typing import Callable

import typer


@dataclass(frozen=True)
class InstallSteps:
    name: str
    check: Callable[[], bool]
    fetch: Callable[[], bool]
    deploy: Callable[[], None]
    configure: Callable[[], None]
    smoke_test: Callable[[], None]


def run_steps(steps: InstallSteps, *, verbose: bool = False) -> bool:
    if steps.check():
        if verbose:
            typer.echo(f"{steps.name} already installed")
        return True

    if not steps.fetch():
        if verbose:
            typer.echo(f"Failed to fetch {steps.name}")
        return False

    steps.deploy()
    steps.configure()
    steps.smoke_test()
    return True
