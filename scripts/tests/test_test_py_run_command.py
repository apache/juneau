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
Tests for scripts/test.py's run_command(): Maven output must stream to the console as it arrives
(push.py's test step otherwise shows nothing until the whole suite ends), while the full output is
still returned for parse_test_results().  Uses a trivial shell command, never mvn.
"""

from __future__ import annotations

import importlib.util
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


@pytest.fixture
def test_mod():
    spec = importlib.util.spec_from_file_location("_undertest_test", SCRIPTS_DIR / "test.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_run_command_streams_and_returns_merged_output(test_mod, capfd):
    code, out = test_mod.run_command("printf 'one\\n'; printf 'two\\n' >&2; printf 'three\\n'; exit 3")
    assert code == 3
    assert out == "one\ntwo\nthree\n"
    assert "one\ntwo\nthree\n" in capfd.readouterr().out


def test_run_command_echoes_lines_before_the_command_exits(test_mod, monkeypatch, tmp_path):
    # The command prints "second" only after a marker file appears, and the fake stdout creates the
    # marker when it receives "first".  A buffering run_command would never deliver "first" in time.
    marker = tmp_path / "marker"
    seen = []
    class Recorder:
        def write(self, s):
            seen.append(s)
            if s == "first\n":
                marker.touch()
        def flush(self):
            pass
    monkeypatch.setattr(test_mod.sys, "stdout", Recorder())
    code, _ = test_mod.run_command(
        f"echo first; for i in $(seq 50); do [ -f '{marker}' ] && echo second && exit 0; sleep 0.1; done; exit 1")
    assert code == 0
    assert "second\n" in seen


def test_a_step_id_command_still_streams_in_plain_mode(test_mod, monkeypatch, tmp_path):
    """Through run_tool (markers off, default sinks), a step's lines still reach stdout before the command exits."""
    marker = tmp_path / "marker"
    seen = []

    class Recorder:
        def write(self, s):
            seen.append(s)
            if "first" in s:
                marker.touch()
            return len(s)

        def flush(self):
            pass

        def isatty(self):
            return False

    monkeypatch.setattr("sys.stdout", Recorder())
    code, out = test_mod.run_command(
        f"echo first; for i in $(seq 50); do [ -f '{marker}' ] && echo second && exit 0; sleep 0.1; done; exit 1",
        "tests", "Java Tests")
    assert code == 0
    assert out == "first\nsecond\n"
    assert any("second" in s for s in seen)


def _recording_wrapper(directory_name, tmp_path):
    """A real executable wrapper under a directory with the given name; it logs its argv, then runs its args."""
    directory = tmp_path / directory_name
    directory.mkdir()
    log = tmp_path / "argv.log"
    script = directory / "lock.sh"
    script.write_text(f"#!/bin/sh\nprintf '%s\\n' \"$*\" >> '{log}'\n[ \"$1\" = --flag ] && shift\nexec \"$@\"\n")
    script.chmod(0o755)
    return script, log


def test_wrapper_path_with_a_space_runs_once_before_mvn(test_mod, monkeypatch, tmp_path):
    script, log = _recording_wrapper("with space", tmp_path)
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", str(script))
    code, _ = test_mod.run_command("mvn -v")
    lines = log.read_text().splitlines()
    assert len(lines) == 1 and lines[0].split()[0] == "mvn"


@pytest.mark.parametrize("form", ["quoted", "unquoted"])
def test_wrapper_with_extra_args_still_works(test_mod, monkeypatch, tmp_path, form):
    script, log = _recording_wrapper("with space", tmp_path)
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", (f"'{script}'" if form == "quoted" else str(script)) + " --flag")
    test_mod.run_command("mvn -v")
    assert log.read_text().splitlines()[0].split()[:2] == ["--flag", "mvn"]


def test_plain_wrapper_value_is_unchanged(test_mod, monkeypatch):
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", "/x/lock.sh")
    assert test_mod.wrap_mvn_command("mvn -v") == "/x/lock.sh mvn -v"
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", "/x/lock.sh --timeout 5")
    assert test_mod.wrap_mvn_command("mvn -v") == "/x/lock.sh --timeout 5 mvn -v"
    monkeypatch.delenv("JUNEAU_MVN_WRAPPER")
    assert test_mod.wrap_mvn_command("mvn -v") == "mvn -v"


def test_bare_path_with_shell_metacharacters_is_not_interpreted(test_mod, monkeypatch, tmp_path):
    marker = tmp_path / "injected"
    script, log = _recording_wrapper(f"a$(touch {marker.name});b $HOME", tmp_path)
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", str(script))
    test_mod.run_command("mvn -v")
    assert not (tmp_path / "injected").exists()
    assert len(log.read_text().splitlines()) == 1
