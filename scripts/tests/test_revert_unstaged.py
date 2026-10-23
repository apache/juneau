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
revert-unstaged.py: restores one file's working-tree edits to the staged version and keeps what is staged. Every test runs
the script in a throwaway git repo under tmp_path, never in a real checkout.
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT = Path(__file__).resolve().parent.parent / "revert-unstaged.py"


def git(repo, *args):
    return subprocess.run(["git", *args], cwd=repo, check=True, capture_output=True, text=True).stdout


def run(repo, *args):
    return subprocess.run([sys.executable, str(SCRIPT), *args], cwd=repo, capture_output=True, text=True)


@pytest.fixture
def repo(tmp_path):
    git(tmp_path, "init", "-q")
    git(tmp_path, "config", "user.email", "t@example.com")
    git(tmp_path, "config", "user.name", "t")
    (tmp_path / "f.txt").write_text("committed\n")
    git(tmp_path, "add", "f.txt")
    git(tmp_path, "commit", "-q", "-m", "init")
    return tmp_path


def test_restores_staged_version_and_keeps_index(repo):
    f = repo / "f.txt"
    f.write_text("A\n")
    git(repo, "add", "f.txt")
    f.write_text("B\n")
    r = run(repo, "f.txt")
    assert r.returncode == 0, r.stdout
    assert f.read_text() == "A\n"
    assert git(repo, "diff", "--cached", "--stat").strip() != ""
    assert git(repo, "show", ":f.txt") == "A\n"
    assert git(repo, "diff").strip() == ""


def test_nothing_to_revert(repo):
    r = run(repo, "f.txt")
    assert r.returncode == 0
    assert "nothing to revert" in r.stdout.lower()
    assert (repo / "f.txt").read_text() == "committed\n"


def test_untracked_file_refused(repo):
    u = repo / "u.txt"
    u.write_text("keep me\n")
    r = run(repo, "u.txt")
    assert r.returncode == 1
    assert "not tracked" in r.stdout
    assert u.read_text() == "keep me\n"


def test_nonexistent_path(repo):
    r = run(repo, "missing.txt")
    assert r.returncode == 1
    assert "not found" in r.stdout.lower()


def test_dash_leading_path_after_double_dash(repo):
    f = repo / "-dash.txt"
    f.write_text("A\n")
    git(repo, "add", "--", "-dash.txt")
    f.write_text("B\n")
    r = run(repo, "--", "-dash.txt")
    assert r.returncode == 0, r.stdout + r.stderr
    assert f.read_text() == "A\n"


def test_help_makes_no_git_call(monkeypatch, capsys):
    import importlib.util
    spec = importlib.util.spec_from_file_location("revert_unstaged", SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)

    def boom(*a, **k):
        raise AssertionError("subprocess.run called")

    monkeypatch.setattr(subprocess, "run", boom)
    with pytest.raises(SystemExit) as e:
        mod.main(["--help"])
    assert e.value.code == 0
    assert "usage" in capsys.readouterr().out.lower()


def test_help_exit_code_via_cli(repo):
    r = run(repo, "-h")
    assert r.returncode == 0
    assert "usage" in r.stdout.lower()
