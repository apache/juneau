# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
"""Runners for Maven, pytest, Playwright, npm and Gradle.  Each returns run.cmd's Result."""

import os
import shutil
import sys
from pathlib import Path

from . import run
from ._base import Result

# --native-tls lets uv fetch pytest behind a TLS-intercepting proxy.
UV_PYTEST = ("uv", "run", "--native-tls", "--with", "pytest", "pytest")
LAUNCHERS = ("auto", "uv", "venv", "python")


def _java_env(java_home, env):
    env = dict(env if env is not None else os.environ)
    env["JAVA_HOME"] = str(java_home)
    env["PATH"] = f"{Path(java_home) / 'bin'}{os.pathsep}{env.get('PATH', '')}"
    return env


def maven(args, *, cwd=".", java_home=None, parser="maven", title=None, **cmd_kwargs):
    """mvn <args>.  With java_home, JAVA_HOME is set and <java_home>/bin goes first on PATH."""
    env = cmd_kwargs.pop("env", None)
    if java_home is not None:
        env = _java_env(java_home, env)
    return run.cmd(["mvn", *args], title=title or " ".join(["mvn", *args]), tool=parser, cwd=cwd, env=env,
                   **cmd_kwargs)


def _venv_python(repo):
    for name in (".venv", "venv"):
        python = Path(repo) / name / ("Scripts/python.exe" if os.name == "nt" else "bin/python")
        if python.exists():
            return python
    return None


def pytest_cmd(repo=".", *, launcher="auto"):
    """The pytest command line.  auto tries uv, then <repo>/.venv or <repo>/venv, then sys.executable -m pytest."""
    if launcher not in LAUNCHERS:
        raise ValueError(f"launcher must be one of {', '.join(LAUNCHERS)}, not {launcher!r}")
    if launcher == "uv" or (launcher == "auto" and shutil.which("uv")):
        return list(UV_PYTEST)
    if launcher in ("auto", "venv"):
        python = _venv_python(repo)
        if python is not None:
            return [str(python), "-m", "pytest"]
        if launcher == "venv":
            raise ValueError(f"no .venv or venv under {repo}")
    return [sys.executable, "-m", "pytest"]


def pytest(args, *, repo=".", launcher="auto", env=None, **cmd_kwargs):
    """pytest <args> with the pytest parser.  env defaults to run.child_env(): a test suite runs as though
    standalone."""
    return run.cmd([*pytest_cmd(repo, launcher=launcher), *args], title=cmd_kwargs.pop("title", "pytest"),
                   tool="pytest", cwd=repo, env=env if env is not None else run.child_env(), **cmd_kwargs)


def browsers_root():
    """Playwright's browser cache: PLAYWRIGHT_BROWSERS_PATH, else the platform default."""
    override = os.environ.get("PLAYWRIGHT_BROWSERS_PATH")
    if override:
        return Path(override)
    if sys.platform == "darwin":
        return Path.home() / "Library" / "Caches" / "ms-playwright"
    if os.name == "nt":
        return Path(os.environ.get("LOCALAPPDATA") or Path.home() / "AppData" / "Local") / "ms-playwright"
    return Path.home() / ".cache" / "ms-playwright"


def chromium_installed():
    """True when an INSTALLATION_COMPLETE marker sits under a chromium-* directory of the browser cache: what
    `playwright install chromium` itself checks."""
    root = browsers_root()
    return root.is_dir() and any((child / "INSTALLATION_COMPLETE").exists() for child in root.glob("chromium-*"))


def _deps_fresh(directory, probe):
    probe = Path(directory) / probe
    if not probe.exists():
        return False
    lockfile = Path(directory) / "package-lock.json"
    return not lockfile.exists() or probe.stat().st_mtime >= lockfile.stat().st_mtime


def _npm_install(directory, probe):
    """npm ci (with a lockfile) or npm install, unless probe exists and is no older than the lockfile."""
    if _deps_fresh(directory, probe):
        return Result(True, 0)
    cmd = ["npm", "ci"] if (Path(directory) / "package-lock.json").exists() else ["npm", "install"]
    return run.cmd(cmd, title=" ".join(cmd), tool="npm", cwd=directory)


def playwright_ensure(directory):
    """Install the npm dependencies and Chromium only when missing.  A routine run starts no subprocess."""
    r = _npm_install(directory, "node_modules/@playwright/test")
    if not r.ok or chromium_installed():
        return r
    return run.cmd(["npx", "--yes", "playwright", "install", "chromium"], title="playwright install chromium",
                   tool="generic", cwd=directory)


def playwright(args, *, cwd, **cmd_kwargs):
    """npx playwright test <args> with the playwright parser, which adds its reporter flags to this command line.
    A wrapper script that runs Playwright itself must use run.cmd instead."""
    return run.cmd(["npx", "playwright", "test", *args], title=cmd_kwargs.pop("title", "playwright test"),
                   tool="playwright", cwd=cwd, **cmd_kwargs)


def npm(args, *, cwd=".", ensure_install=False, **cmd_kwargs):
    """npm <args> with the npm parser.  ensure_install first runs npm ci/install when node_modules is missing or older
    than package-lock.json."""
    if ensure_install:
        r = _npm_install(cwd, "node_modules")
        if not r.ok:
            return r
    return run.cmd(["npm", *args], title=cmd_kwargs.pop("title", " ".join(["npm", *args])), tool="npm", cwd=cwd,
                   **cmd_kwargs)


def gradle_cmd(cwd="."):
    """./gradlew when <cwd>/gradlew exists, else gradle from PATH."""
    return ["./gradlew"] if (Path(cwd) / "gradlew").exists() else ["gradle"]


def gradle(args, *, cwd=".", java_home=None, **cmd_kwargs):
    """Gradle <args> with the gradle parser (which adds --console=plain)."""
    env = cmd_kwargs.pop("env", None)
    if java_home is not None:
        env = _java_env(java_home, env)
    cmd = [*gradle_cmd(cwd), *args]
    return run.cmd(cmd, title=cmd_kwargs.pop("title", " ".join(["gradle", *args])), tool="gradle", cwd=cwd, env=env,
                   **cmd_kwargs)
