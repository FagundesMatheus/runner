from __future__ import annotations

import json
import shutil
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Optional

from core import InstallSteps

ASSINADOR_HOME = Path.home() / ".assinador"
DOWNLOAD_DIR = ASSINADOR_HOME / "downloads"
ASSINADOR_STATE_FILE = ASSINADOR_HOME / "assinador_path.txt"
ASSINADOR_VERSION_FILE = ASSINADOR_HOME / "assinador_version.txt"

RELEASE_JSON_URL = "https://raw.githubusercontent.com/FagundesMatheus/runner/main/release.json"
USER_AGENT = "CLIAssinador/1.0"


def check_assinador() -> bool:
    assinador_path = _load_assinador_path()
    if assinador_path is None or not assinador_path.exists():
        return False

    release_version = _latest_assinador_version()
    if release_version is None:
        return True

    local_version = _load_assinador_version()
    return local_version == release_version


def fetch_assinador() -> bool:
    if check_assinador():
        return True

    release_data = _load_release_json()
    if release_data is None:
        return False

    url = _assinador_url_from_release(release_data)
    if not url:
        return False

    release_version = _assinador_version_from_release(release_data)
    download_path = _download_path(url)
    if release_version and _load_assinador_version() == release_version and download_path.exists():
        _save_assinador_version(release_version)
        return True

    DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)
    try:
        urllib.request.urlretrieve(url, download_path)
    except OSError:
        return False

    if release_version:
        _save_assinador_version(release_version)

    return True


def deploy_assinador() -> None:
    if check_assinador():
        return

    source_path = _downloaded_assinador_asset()
    if source_path is None or not source_path.exists():
        raise FileNotFoundError("Assinador archive not found; run fetch first")

    ASSINADOR_HOME.mkdir(parents=True, exist_ok=True)
    assinador_path = ASSINADOR_HOME / source_path.name
    shutil.copy2(source_path, assinador_path)
    _save_assinador_path(assinador_path)
    release_version = _load_assinador_version()
    if release_version:
        _save_assinador_version(release_version)


def configure_assinador() -> None:
    return


def smoke_test_assinador() -> None:
    assinador_path = _load_assinador_path()
    if assinador_path is None or not assinador_path.exists():
        raise RuntimeError("Assinador smoke test failed")


def assinador_steps() -> InstallSteps:
    return InstallSteps(
        name="assinador",
        check=check_assinador,
        fetch=fetch_assinador,
        deploy=deploy_assinador,
        configure=configure_assinador,
        smoke_test=smoke_test_assinador,
    )


def get_assinador_path() -> Optional[Path]:
    return _load_assinador_path()


def get_assinador_version() -> Optional[str]:
    return _load_assinador_version()


def _load_release_json() -> Optional[dict]:
    request = urllib.request.Request(RELEASE_JSON_URL, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            return json.load(response)
    except (OSError, json.JSONDecodeError):
        return None


def _assinador_url_from_release(data: dict) -> Optional[str]:
    jar = data.get("jarAssinador", {})
    return jar.get("url")


def _assinador_version_from_release(data: dict) -> Optional[str]:
    jar = data.get("jarAssinador", {})
    version = jar.get("version")
    return version.strip() if isinstance(version, str) and version.strip() else None


def _download_path(url: str) -> Path:
    name = Path(urllib.parse.urlparse(url).path).name
    if not name:
        name = "assinador.bin"
    return DOWNLOAD_DIR / name


def _downloaded_assinador_asset() -> Optional[Path]:
    data = _load_release_json()
    if data is not None:
        url = _assinador_url_from_release(data)
        if url:
            path = _download_path(url)
            if path.exists():
                return path
    return None


def _save_assinador_path(path: Path) -> None:
    ASSINADOR_STATE_FILE.parent.mkdir(parents=True, exist_ok=True)
    ASSINADOR_STATE_FILE.write_text(str(path), encoding="utf-8")


def _load_assinador_path() -> Optional[Path]:
    if not ASSINADOR_STATE_FILE.exists():
        return None
    content = ASSINADOR_STATE_FILE.read_text(encoding="utf-8").strip()
    return Path(content) if content else None


def _save_assinador_version(version: str) -> None:
    ASSINADOR_VERSION_FILE.parent.mkdir(parents=True, exist_ok=True)
    ASSINADOR_VERSION_FILE.write_text(version, encoding="utf-8")


def _load_assinador_version() -> Optional[str]:
    if not ASSINADOR_VERSION_FILE.exists():
        return None
    content = ASSINADOR_VERSION_FILE.read_text(encoding="utf-8").strip()
    return content if content else None


def _latest_assinador_version() -> Optional[str]:
    data = _load_release_json()
    if data is None:
        return None
    return _assinador_version_from_release(data)
