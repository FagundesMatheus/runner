from __future__ import annotations

import json
import os
import platform
import re
import shutil
import subprocess
import tarfile
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path
from typing import Optional

from core import InstallSteps

APP_DIR = Path.home() / ".hubsaude"
JRE_DIR = APP_DIR / "jre"
DOWNLOAD_DIR = APP_DIR / "downloads"

RELEASE_JSON_URL = "https://raw.githubusercontent.com/FagundesMatheus/runner/main/release.json"
VERSION_PATTERN = re.compile(r'version "([^"]+)"')
USER_AGENT = "CLIHub/1.0"


def check_java() -> bool:
    required_major = _required_java_major()
    java_exe = _find_java_in_dir(JRE_DIR)
    return java_exe is not None and _run_java_version(java_exe, required_major=required_major)


def fetch_java() -> bool:
    if _find_java_in_dir(JRE_DIR):
        return True

    url = _jre_url()
    if not url:
        return False

    DOWNLOAD_DIR.mkdir(parents=True, exist_ok=True)
    archive_path = _archive_path(url)
    if archive_path.exists():
        return True

    try:
        _download_file(url, archive_path)
    except OSError:
        return False

    return True


def deploy_java() -> None:
    if _find_java_in_dir(JRE_DIR):
        return

    url = _jre_url()
    if not url:
        raise FileNotFoundError("JRE URL not available")

    archive_path = _archive_path(url)
    if not archive_path.exists():
        raise FileNotFoundError("JRE archive not found; run fetch first")

    if JRE_DIR.exists():
        shutil.rmtree(JRE_DIR)
    JRE_DIR.mkdir(parents=True, exist_ok=True)

    if zipfile.is_zipfile(archive_path):
        with zipfile.ZipFile(archive_path) as archive:
            archive.extractall(JRE_DIR)
    elif tarfile.is_tarfile(archive_path):
        with tarfile.open(archive_path) as archive:
            archive.extractall(JRE_DIR)
    else:
        raise ValueError("Unsupported JRE archive format")


def configure_java() -> None:
    java_exe = _find_java_in_dir(JRE_DIR)
    if java_exe is None:
        return

    java_home = java_exe.parent.parent
    os.environ["JAVA_HOME"] = str(java_home)
    _prepend_path(str(java_exe.parent))


def smoke_test_java() -> None:
    required_major = _required_java_major()
    java_exe = _find_java_in_dir(JRE_DIR)
    if java_exe is None or not _run_java_version(java_exe, required_major=required_major):
        raise RuntimeError("Java smoke test failed")


def java_steps() -> InstallSteps:
    return InstallSteps(
        name="java",
        check=check_java,
        fetch=fetch_java,
        deploy=deploy_java,
        configure=configure_java,
        smoke_test=smoke_test_java,
    )


def _load_release_json() -> Optional[dict]:
    request = urllib.request.Request(RELEASE_JSON_URL, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            return json.load(response)
    except (OSError, json.JSONDecodeError):
        return None


def _jre_url_from_release(data: dict) -> Optional[str]:
    jre = data.get("jre", {})
    os_key, arch_key = _platform_key()
    if os_key is None:
        return None
    key = f"{os_key}_{arch_key}"
    return jre.get(key)


def _jre_url() -> Optional[str]:
    data = _load_release_json()
    if data is None:
        return None
    return _jre_url_from_release(data)


def _required_java_major() -> Optional[int]:
    url = _jre_url()
    if not url:
        return None
    return _major_from_jre_url(url)


def _major_from_jre_url(url: str) -> Optional[int]:
    match = re.search(r"/latest/(\d+)/", url)
    if match:
        return int(match.group(1))
    return None


def _platform_key() -> tuple[Optional[str], str]:
    system = platform.system().lower()
    if system.startswith("windows"):
        os_key = "windows"
    elif system.startswith("linux"):
        os_key = "linux"
    elif system.startswith("darwin"):
        os_key = "mac"
    else:
        os_key = None

    machine = platform.machine().lower()
    arch_key = "x64" if machine in {"amd64", "x86_64", "x64"} else machine
    return os_key, arch_key


def _archive_path(url: str) -> Path:
    name = Path(urllib.parse.urlparse(url).path).name
    if not name:
        name = "jre"
    return DOWNLOAD_DIR / name


def _find_java_in_dir(base_dir: Path) -> Optional[Path]:
    if not base_dir.exists():
        return None

    exe_name = "java.exe" if os.name == "nt" else "java"
    for path in base_dir.rglob(exe_name):
        if path.parent.name == "bin":
            return path
    return None


def _run_java_version(java_exe: Path, *, required_major: Optional[int]) -> bool:
    try:
        result = subprocess.run(
            [str(java_exe), "-version"],
            capture_output=True,
            text=True,
            check=False,
            timeout=10,
        )
    except (OSError, subprocess.TimeoutExpired):
        return False

    if result.returncode != 0:
        return False

    version_text = (result.stdout or "") + (result.stderr or "")
    if required_major is None:
        return True

    major = _parse_java_major(version_text)
    return major == required_major


def _parse_java_major(version_text: str) -> Optional[int]:
    match = VERSION_PATTERN.search(version_text)
    if not match:
        return None
    version = match.group(1)
    if version.startswith("1."):
        parts = version.split(".")
        if len(parts) > 1 and parts[1].isdigit():
            return int(parts[1])
        return None
    head = version.split(".")[0]
    return int(head) if head.isdigit() else None


def _prepend_path(path_value: str) -> None:
    current = os.environ.get("PATH", "")
    paths = current.split(os.pathsep) if current else []
    if path_value not in paths:
        os.environ["PATH"] = os.pathsep.join([path_value] + paths)


def _download_file(url: str, target_path: Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        with open(target_path, "wb") as handle:
            shutil.copyfileobj(response, handle)


