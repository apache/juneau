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
"""
Test helpers for push and release scripts, stdlib only.  The pytest fixtures with the same names live in
juneau_scripts.testing.pytest_plugin (the only file that imports pytest).

bare_remote() seeds its throwaway repository with one `git add`; nothing outside this module does.
"""

import contextlib
import os
import subprocess
import sys
from pathlib import Path


@contextlib.contextmanager
def _environ(remove=(), extra=None):
    saved = dict(os.environ)
    try:
        for key in remove:
            os.environ.pop(key, None)
        os.environ.update(extra or {})
        yield
    finally:
        os.environ.clear()
        os.environ.update(saved)


@contextlib.contextmanager
def hermetic_git(tmp):
    """Remove every inherited GIT_* variable, then point git at no global or system config, stop repository discovery
    at tmp's parent and forbid prompts.  Sets no author or committer variables: identity is repo-local config (see
    bare_remote), so a missing user.email stays missing.  Yields the overrides; restores the environment on exit."""
    overrides = {
        "GIT_CONFIG_GLOBAL": os.devnull,
        "GIT_CONFIG_NOSYSTEM": "1",
        "GIT_CEILING_DIRECTORIES": str(Path(os.path.realpath(tmp)).parent),
        "GIT_TERMINAL_PROMPT": "0",
    }
    with _environ([k for k in os.environ if k.startswith("GIT_")], overrides):
        yield dict(overrides)


def _git(*args, cwd):
    subprocess.run(["git", *args], cwd=cwd, check=True, capture_output=True, text=True)


def bare_remote(tmp):
    """A work repository on branch main with a local identity, commit.gpgsign=false and one commit, tracking a bare
    origin.  Returns (work, origin) paths under tmp."""
    tmp = Path(tmp)
    origin, work = tmp / "origin.git", tmp / "work"
    _git("init", "-q", "--bare", "-b", "main", str(origin), cwd=tmp)
    _git("init", "-q", "-b", "main", str(work), cwd=tmp)
    for key, value in (("user.name", "Test User"), ("user.email", "test@example.com"), ("commit.gpgsign", "false")):
        _git("config", key, value, cwd=work)
    (work / "README.md").write_text("readme\n", encoding="utf-8")
    _git("add", "README.md", cwd=work)
    _git("commit", "-q", "-m", "Initial commit", cwd=work)
    _git("remote", "add", "origin", str(origin), cwd=work)
    _git("push", "-q", "-u", "origin", "main", cwd=work)
    return work, origin


def _run_variables():
    return [k for k in os.environ
            if k in ("RUN_MARKERS", "RUN_ARTIFACTS") or (k.startswith("JUNEAU_RUN_") and k != "JUNEAU_RUN_PY")]


@contextlib.contextmanager
def clean_run_env():
    """Strip RUN_MARKERS, RUN_ARTIFACTS and every JUNEAU_RUN_* variable except JUNEAU_RUN_PY, and forget
    sys.modules["juneau_run"], for the block (then set JUNEAU_RUN_HISTORY=0 so no run writes history).  All are restored
    afterwards."""
    saved = sys.modules.pop("juneau_run", None)
    try:
        with _environ(_run_variables(), {"JUNEAU_RUN_HISTORY": "0"}):
            yield
    finally:
        if saved is not None:
            sys.modules["juneau_run"] = saved
        else:
            sys.modules.pop("juneau_run", None)


def fake_tool(bin_dir, name, *, exit_env=None, log_env=None, stdout=""):
    """Write an executable stub bin_dir/name.  When $log_env is set it appends its argv (name first) as one JSON line
    to that file; it prints stdout, then exits with int($exit_env), or 0 when that is unset."""
    bin_dir = Path(bin_dir)
    bin_dir.mkdir(parents=True, exist_ok=True)
    path = bin_dir / name
    path.write_text(
        f"#!{sys.executable}\n"
        "import json, os, sys\n"
        f"log = os.environ.get({log_env!r}) if {log_env!r} else None\n"
        "if log:\n"
        "    with open(log, 'a', encoding='utf-8') as f:\n"
        f"        f.write(json.dumps([{name!r}, *sys.argv[1:]]) + '\\n')\n"
        f"sys.stdout.write({stdout!r})\n"
        f"code = os.environ.get({exit_env!r}) if {exit_env!r} else None\n"
        "sys.exit(int(code) if code else 0)\n",
        encoding="utf-8")
    path.chmod(0o755)
    return path


__all__ = ["bare_remote", "clean_run_env", "fake_tool", "hermetic_git"]
