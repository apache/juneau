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
