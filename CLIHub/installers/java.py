from core import InstallSteps


def check_java() -> bool:
    raise NotImplementedError("Implement java check")


def fetch_java() -> bool:
    raise NotImplementedError("Implement java download")


def deploy_java() -> None:
    raise NotImplementedError("Implement java deploy")


def configure_java() -> None:
    raise NotImplementedError("Implement java config")


def smoke_test_java() -> None:
    raise NotImplementedError("Implement java smoke test")


def java_steps() -> InstallSteps:
    return InstallSteps(
        name="java",
        check=check_java,
        fetch=fetch_java,
        deploy=deploy_java,
        configure=configure_java,
        smoke_test=smoke_test_java,
    )
