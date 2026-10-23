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
revert-staged.py: restores one file from HEAD. Every test runs the script in a throwaway git repo under tmp_path, never in a
real checkout.
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT = Path(__file__).resolve().parent.parent / "revert-staged.py"


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
    (tmp_path / "-dash.txt").write_text("committed\n")
    git(tmp_path, "add", "--", "f.txt", "-dash.txt")
    git(tmp_path, "commit", "-q", "-m", "init")
    return tmp_path


def test_restores_working_tree_from_head(repo):
    f = repo / "f.txt"
    f.write_text("edited\n")
    r = run(repo, "f.txt")
    assert r.returncode == 0, r.stdout
    assert f.read_text() == "committed\n"


def test_dash_leading_path_after_double_dash(repo):
    f = repo / "-dash.txt"
    f.write_text("edited\n")
    r = run(repo, "--", "-dash.txt")
    assert r.returncode == 0, r.stdout + r.stderr
    assert f.read_text() == "committed\n"


def test_path_named_like_a_ref(repo):
    git(repo, "branch", "f.txt")
    f = repo / "f.txt"
    f.write_text("edited\n")
    r = run(repo, "f.txt")
    assert r.returncode == 0, r.stdout + r.stderr
    assert f.read_text() == "committed\n"


def test_untracked_file_refused(repo):
    u = repo / "u.txt"
    u.write_text("keep me\n")
    r = run(repo, "u.txt")
    assert r.returncode == 1
    assert "not tracked" in r.stdout
    assert u.read_text() == "keep me\n"


def test_no_arguments_is_usage_error(repo):
    r = run(repo)
    assert r.returncode == 2
    assert "usage" in r.stderr.lower()


def test_help_makes_no_git_call(monkeypatch, capsys):
    import importlib.util
    spec = importlib.util.spec_from_file_location("revert_staged", SCRIPT)
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
