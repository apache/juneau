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
Tests for `scripts/test.py --profile`: the generated mvn command (jacoco skipped, JFR flags appended to the module's own
surefire.extraArgs, run from the module directory, absolute per-execution filename with a deep stack) and the check that
a JFR file exists before success is reported.  Nothing here runs mvn.
"""

from __future__ import annotations

import importlib.util
import shlex
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


@pytest.fixture
def test_mod(tmp_path, monkeypatch):
    spec = importlib.util.spec_from_file_location("_undertest_test_profile", SCRIPTS_DIR / "test.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    monkeypatch.setattr(module, "REPO_ROOT", tmp_path)
    monkeypatch.setattr(module, "wrap_mvn_command", lambda cmd: cmd)
    return module


def _module(root, name, extra=None):
    directory = root / name
    directory.mkdir(parents=True)
    prop = f"<properties><surefire.extraArgs>{extra}</surefire.extraArgs></properties>" if extra is not None else ""
    (directory / "pom.xml").write_text(f"<project>{prop}</project>")
    return directory


def _words(command):
    return shlex.split(command)


def test_command_runs_from_module_dir_and_skips_jacoco(test_mod, tmp_path):
    directory = _module(tmp_path, "juneau-core/juneau-marshall")
    words = _words(test_mod.profile_command("juneau-core/juneau-marshall", tmp_path / "x.jfr"))
    assert words[:3] == ["cd", str(directory), "&&"]
    assert "-Djacoco.skip=true" in words
    assert "-pl" not in words
    assert not any(w.startswith("-DargLine") for w in words)


def test_jfr_flags_ride_on_surefire_extra_args_with_deep_stack(test_mod, tmp_path):
    _module(tmp_path, "m")
    out = tmp_path / "target" / "m-1"
    words = _words(test_mod.profile_command("m", out))
    arg = next(w for w in words if w.startswith("-Dsurefire.extraArgs="))
    assert "-XX:FlightRecorderOptions:stackdepth=1024" in arg
    assert f"-XX:StartFlightRecording=filename={out},settings=profile" in arg
    assert Path(out).is_absolute()


def test_module_own_extra_args_are_kept_before_jfr_flags(test_mod, tmp_path):
    _module(tmp_path, "m", extra="-javaagent:${org.mockito:mockito-core:jar}")
    arg = next(w for w in _words(test_mod.profile_command("m", tmp_path / "a.jfr")) if w.startswith("-Dsurefire.extraArgs="))
    value = arg.split("=", 1)[1]
    assert value.startswith("-javaagent:${org.mockito:mockito-core:jar} -XX:")


def test_module_without_extra_args_gets_only_jfr_flags(test_mod, tmp_path):
    _module(tmp_path, "m", extra="")
    arg = next(w for w in _words(test_mod.profile_command("m", tmp_path / "a.jfr")) if w.startswith("-Dsurefire.extraArgs="))
    assert arg.split("=", 1)[1].startswith("-XX:FlightRecorderOptions")


def test_success_without_jfr_file_fails_loudly(test_mod, tmp_path, monkeypatch, capsys):
    _module(tmp_path, "m")
    monkeypatch.setattr(test_mod, "run_command", lambda *a, **k: (0, "BUILD SUCCESS"))
    code, _ = test_mod.profile("m")
    assert code == 1
    assert "no JFR file" in capsys.readouterr().out


def test_success_reports_each_written_file(test_mod, tmp_path, monkeypatch, capsys):
    _module(tmp_path, "m")

    def fake_run(command, *a, **k):
        name = next(w for w in shlex.split(command) if w.startswith("-Dsurefire.extraArgs=")).split("filename=")[1].split(",")[0]
        Path(name, "hotspot-pid-111-id-1.jfr").write_text("x")
        Path(name, "hotspot-pid-222-id-1.jfr").write_text("x")
        return 0, ""

    monkeypatch.setattr(test_mod, "run_command", fake_run)
    code, _ = test_mod.profile("m")
    out = capsys.readouterr().out
    assert code == 0
    assert out.count("JFR profile captured") == 2


def test_mvn_failure_passes_through(test_mod, tmp_path, monkeypatch):
    _module(tmp_path, "m")
    monkeypatch.setattr(test_mod, "run_command", lambda *a, **k: (3, "boom"))
    assert test_mod.profile("m")[0] == 3
