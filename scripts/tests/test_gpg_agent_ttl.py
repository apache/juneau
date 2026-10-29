#!/usr/bin/env python3
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
"""Tests for gpg-agent-ttl.py (temp homedirs and a faked gpgconf; the real ~/.gnupg is never touched)."""
import importlib.util
import stat
import subprocess
from pathlib import Path

import pytest

SCRIPT = Path(__file__).resolve().parents[1] / "gpg-agent-ttl.py"
spec = importlib.util.spec_from_file_location("gpg_agent_ttl", SCRIPT)
tool = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tool)

LISTING = ("default-cache-ttl:24:0:x:3:3:N:600::\nmax-cache-ttl:24:0:y:3:3:N:7200::14400\n")


@pytest.fixture
def gpg(monkeypatch):
    """Fake gpgconf on PATH; records calls and can be told to fail the reload."""
    calls = []
    state = {"reload_rc": 0}

    def fake_run(cmd, **kw):
        calls.append(cmd)
        if "--reload" in cmd:
            return subprocess.CompletedProcess(cmd, state["reload_rc"], "", "boom" if state["reload_rc"] else "")
        return subprocess.CompletedProcess(cmd, 0, LISTING, "")

    monkeypatch.setattr(tool.shutil, "which", lambda name: "/fake/gpgconf")
    monkeypatch.setattr(tool.subprocess, "run", fake_run)
    monkeypatch.delenv("GNUPGHOME", raising=False)
    state["calls"] = calls
    return state


def reloads(gpg):
    return [c for c in gpg["calls"] if "--reload" in c]


def run(home, *extra):
    return tool.main(["--homedir", str(home), *extra])


def test_create_from_nothing(tmp_path, gpg, capsys):
    home = tmp_path / "gnupg"
    assert run(home) == 0
    conf = home / "gpg-agent.conf"
    lines = conf.read_text().splitlines()
    assert "default-cache-ttl 7200" in lines and "max-cache-ttl 14400" in lines
    assert lines[0].startswith("#")
    assert stat.S_IMODE(conf.stat().st_mode) == 0o600
    assert stat.S_IMODE(home.stat().st_mode) == 0o700
    out = capsys.readouterr().out
    assert "create" in out and "reloaded gpg-agent" in out and "effective: default-cache-ttl=600, max-cache-ttl=14400" in out
    assert reloads(gpg)[0][:3] == ["gpgconf", "--homedir", str(home)]


def test_raise_low_value_in_place(tmp_path, gpg, capsys):
    (tmp_path / "gpg-agent.conf").write_text("default-cache-ttl 600\nmax-cache-ttl   20000\n")
    assert run(tmp_path) == 0
    assert (tmp_path / "gpg-agent.conf").read_text() == "default-cache-ttl 7200\nmax-cache-ttl   20000\n"
    assert "raise default-cache-ttl from 600 to 7200" in capsys.readouterr().out


def test_keep_higher_value(tmp_path, gpg):
    text = "default-cache-ttl 9000\nmax-cache-ttl 50000\n"
    (tmp_path / "gpg-agent.conf").write_text(text)
    assert run(tmp_path) == 0
    assert (tmp_path / "gpg-agent.conf").read_text() == text
    assert reloads(gpg) == []


def test_append_missing_key_and_keep_other_lines(tmp_path, gpg):
    (tmp_path / "gpg-agent.conf").write_text("# my comment\npinentry-program /x/pin\n# default-cache-ttl 1\nmax-cache-ttl 15000\n")
    assert run(tmp_path) == 0
    lines = (tmp_path / "gpg-agent.conf").read_text().splitlines()
    assert lines[:4] == ["# my comment", "pinentry-program /x/pin", "# default-cache-ttl 1", "max-cache-ttl 15000"]
    assert lines[-1] == "default-cache-ttl 7200"
    assert sum(l.startswith("max-cache-ttl") for l in lines) == 1


def test_idempotent_second_run_does_not_rewrite(tmp_path, gpg, capsys):
    assert run(tmp_path) == 0
    conf = tmp_path / "gpg-agent.conf"
    before = conf.stat().st_ino
    gpg["calls"].clear()
    capsys.readouterr()
    assert run(tmp_path) == 0
    assert conf.stat().st_ino == before
    assert "already OK" in capsys.readouterr().out
    assert reloads(gpg) == []


def test_preserves_file_mode(tmp_path, gpg):
    conf = tmp_path / "gpg-agent.conf"
    conf.write_text("default-cache-ttl 1\n")
    conf.chmod(0o640)
    assert run(tmp_path) == 0
    assert stat.S_IMODE(conf.stat().st_mode) == 0o640


def test_dry_run_writes_and_reloads_nothing(tmp_path, gpg, capsys):
    home = tmp_path / "gnupg"
    assert run(home, "--dry-run") == 0
    assert not home.exists()
    assert reloads(gpg) == []
    assert "would" in capsys.readouterr().out


def test_dry_run_leaves_existing_file(tmp_path, gpg):
    (tmp_path / "gpg-agent.conf").write_text("default-cache-ttl 1\n")
    assert run(tmp_path, "--dry-run") == 0
    assert (tmp_path / "gpg-agent.conf").read_text() == "default-cache-ttl 1\n"


def test_no_reload_flag(tmp_path, gpg):
    assert run(tmp_path, "--no-reload") == 0
    assert reloads(gpg) == []
    assert (tmp_path / "gpg-agent.conf").exists()


def test_reload_failure_only_warns(tmp_path, gpg, capsys):
    gpg["reload_rc"] = 2
    assert run(tmp_path) == 0
    err = capsys.readouterr().err
    assert "run it by hand" in err


def test_max_below_default_exits_2(tmp_path, gpg):
    assert run(tmp_path, "--default-ttl", "5000", "--max-ttl", "4000") == 2
    assert not (tmp_path / "gpg-agent.conf").exists()


def test_gnupghome_honoured(tmp_path, gpg, monkeypatch):
    monkeypatch.setenv("GNUPGHOME", str(tmp_path / "envhome"))
    assert tool.main([]) == 0
    assert (tmp_path / "envhome" / "gpg-agent.conf").exists()
    assert reloads(gpg)[0][:2] == ["gpgconf", "--reload"]


def test_missing_gpgconf_warns(tmp_path, gpg, monkeypatch, capsys):
    monkeypatch.setattr(tool.shutil, "which", lambda name: None)
    assert run(tmp_path) == 0
    assert "gpgconf not found" in capsys.readouterr().err
