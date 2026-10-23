# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
# * with the License.  You may obtain a copy of the License at                                                              *
# *                                                                                                                         *
# *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
# *                                                                                                                         *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
# * specific language governing permissions and limitations under the License.                                              *
"""
Tests for the command line of scripts/create-mvn-site.py: --help and -h print usage and exit 0, and an unknown flag
exits 2, all before anything is removed or any Maven process is started.  Nothing here runs a real mvn.
"""

from __future__ import annotations

import importlib.util
import subprocess
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


@pytest.fixture
def module():
    spec = importlib.util.spec_from_file_location("_undertest_create_mvn_site", SCRIPTS_DIR / "create-mvn-site.py")
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


@pytest.fixture
def forbidden(module, monkeypatch):
    """Make any process spawn or directory removal fail the test."""

    def boom(*a, **k):
        raise AssertionError("nothing may be run or removed while parsing arguments")

    monkeypatch.setattr(module.subprocess, "run", boom)
    monkeypatch.setattr(module.subprocess, "Popen", boom)
    monkeypatch.setattr(module.shutil, "rmtree", boom)
    monkeypatch.setattr(module.shutil, "copytree", boom)


@pytest.mark.parametrize("flag", ["--help", "-h"])
def test_help_exits_zero_and_touches_nothing(module, forbidden, flag, capsys):
    with pytest.raises(SystemExit) as exc:
        module.main([flag])
    assert exc.value.code == 0
    assert "usage" in capsys.readouterr().out.lower()


def test_unknown_flag_exits_two_and_touches_nothing(module, forbidden, capsys):
    with pytest.raises(SystemExit) as exc:
        module.main(["--bogus"])
    assert exc.value.code == 2
    assert "usage" in capsys.readouterr().err.lower()


def test_dry_run_exits_zero_and_touches_nothing(module, forbidden, capsys):
    assert module.main(["--dry-run"]) == 0
    assert "mvn clean compile site" in capsys.readouterr().out


def test_no_args_still_runs_the_build_sequence(module, tmp_path, monkeypatch):
    calls = []
    removed = []
    site = tmp_path / "target" / "site"
    site.mkdir(parents=True)
    (site / "index.html").write_text("x")

    monkeypatch.setattr(module.subprocess, "run", lambda *a, **k: subprocess.CompletedProcess(a, 0, stdout="1.0\n"))
    monkeypatch.setattr(module, "run_maven_command", lambda command, description, cwd: calls.append(command) or True)
    monkeypatch.setattr(module.shutil, "rmtree", lambda p: removed.append(Path(p)))
    monkeypatch.setattr(module, "__file__", str(tmp_path / "scripts" / "create-mvn-site.py"))

    assert module.main([]) == 0
    assert calls == [["mvn", "clean", "compile", "site"]]
    assert site in removed
    assert (tmp_path / "docs" / "static" / "site" / "index.html").exists()
