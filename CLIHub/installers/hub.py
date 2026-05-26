from __future__ import annotations

import json
import shutil
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Optional

from core import InstallSteps

APP_DIR = Path.home() / ".hubsaude"
HUB_DIR = APP_DIR / "hub"
DOWNLOAD_DIR = APP_DIR / "downloads"
HUB_STATE_FILE = APP_DIR / "hub_path.txt"

RELEASE_JSON_URL = "https://raw.githubusercontent.com/FagundesMatheus/runner/main/release.json"


def check_hub() -> bool:
    hub_path = _load_hub_path()
    return hub_path is not None and hub_path.exists()


def fetch_hub() -> bool:
    if check_hub():
        return True

    release_data = _load_release_json()
    if release_data is None:
        return False

    url = _hub_url_from_release(release_data)
    if not url:
        return False

    DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)
    download_path = _download_path(url)
    if download_path.exists():
        return True

    try:
        urllib.request.urlretrieve(url, download_path)
    except OSError:
        return False

    return True


def deploy_hub() -> None:
    if check_hub():
        return

    source_path = _downloaded_hub_asset()
    if source_path is None or not source_path.exists():
        raise FileNotFoundError("Hub archive not found; run fetch first")

    if HUB_DIR.exists():
        shutil.rmtree(HUB_DIR)
    HUB_DIR.mkdir(parents=True, exist_ok=True)

    hub_path = HUB_DIR / source_path.name
    shutil.copy2(source_path, hub_path)
    _save_hub_path(hub_path)


def configure_hub() -> None:
    return


def smoke_test_hub() -> None:
    hub_path = _load_hub_path()
    if hub_path is None or not hub_path.exists():
        raise RuntimeError("Hub smoke test failed")


def hub_steps() -> InstallSteps:
    return InstallSteps(
        name="hub",
        check=check_hub,
        fetch=fetch_hub,
        deploy=deploy_hub,
        configure=configure_hub,
        smoke_test=smoke_test_hub,
    )


def _load_release_json() -> Optional[dict]:
    request = urllib.request.Request(RELEASE_JSON_URL, headers={"User-Agent": "CLIHub/1.0"})
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            return json.load(response)
    except (OSError, json.JSONDecodeError):
        return None


def _hub_url_from_release(data: dict) -> Optional[str]:
    jar = data.get("jarHUB", {})
    return jar.get("url")


def _download_path(url: str) -> Path:
    name = Path(urllib.parse.urlparse(url).path).name
    if not name:
        name = "hub.bin"
    return DOWNLOAD_DIR / name


def _downloaded_hub_asset() -> Optional[Path]:
    data = _load_release_json()
    if data is not None:
        url = _hub_url_from_release(data)
        if url:
            path = _download_path(url)
            if path.exists():
                return path
    return None


def _save_hub_path(path: Path) -> None:
    HUB_STATE_FILE.parent.mkdir(parents=True, exist_ok=True)
    HUB_STATE_FILE.write_text(str(path), encoding="utf-8")


def _load_hub_path() -> Optional[Path]:
    if not HUB_STATE_FILE.exists():
        return None
    content = HUB_STATE_FILE.read_text(encoding="utf-8").strip()
    return Path(content) if content else None


