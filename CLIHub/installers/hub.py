from core import InstallSteps


def check_hub() -> bool:
    raise NotImplementedError("Implement hub check")


def fetch_hub() -> bool:
    raise NotImplementedError("Implement hub download")


def deploy_hub() -> None:
    raise NotImplementedError("Implement hub deploy")


def configure_hub() -> None:
    raise NotImplementedError("Implement hub config")


def smoke_test_hub() -> None:
    raise NotImplementedError("Implement hub smoke test")


def hub_steps() -> InstallSteps:
    return InstallSteps(
        name="hub",
        check=check_hub,
        fetch=fetch_hub,
        deploy=deploy_hub,
        configure=configure_hub,
        smoke_test=smoke_test_hub,
    )
