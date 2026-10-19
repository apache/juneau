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
Tests for the Script tests step in scripts/test.py (the scripts/tests pytest suite as its own row): launcher choice,
the skip notice, the pass/fail result, and the row in push.py's plan.  Nothing here runs pytest, mvn or push.py's main().
"""

from __future__ import annotations

import argparse
import importlib.util
import subprocess
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


def _load(name):
    spec = importlib.util.spec_from_file_location(f"_undertest_{name}", SCRIPTS_DIR / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def test_mod(monkeypatch):
    monkeypatch.delenv("JUNEAU_SCRIPT_TESTS_ACTIVE", raising=False)   # conftest sets it for every other test
    return _load("test")


@pytest.fixture
def push_mod():
    return _load("push")


def _fake_which(tools):
    return lambda name: f"/bin/{name}" if name in tools else None


def _fake_import(monkeypatch, test_mod, importable):
    def run(cmd, **kwargs):
        return subprocess.CompletedProcess(cmd, 0 if importable else 1)
    monkeypatch.setattr(test_mod.subprocess, "run", run)


# ---- launcher choice -------------------------------------------------------------------------------------------------
def test_launcher_prefers_python_module_when_pytest_importable(monkeypatch, test_mod):
    monkeypatch.setattr(test_mod.shutil, "which", _fake_which({"python3", "uv"}))
    _fake_import(monkeypatch, test_mod, True)
    assert test_mod.script_tests_launcher() == ["/bin/python3", "-m", "pytest"]


def test_launcher_falls_back_to_uv(monkeypatch, test_mod):
    monkeypatch.setattr(test_mod.shutil, "which", _fake_which({"python3", "uv"}))
    _fake_import(monkeypatch, test_mod, False)
    assert test_mod.script_tests_launcher() == ["uv", "run", "--native-tls", "--with", "pytest", "pytest"]


def test_launcher_none_without_pytest_or_uv(monkeypatch, test_mod):
    monkeypatch.setattr(test_mod.shutil, "which", _fake_which({"python3"}))
    _fake_import(monkeypatch, test_mod, False)
    assert test_mod.script_tests_launcher() is None


def test_command_targets_scripts_tests(test_mod):
    cmd = test_mod.script_tests_command(["uv", "run", "--native-tls", "--with", "pytest", "pytest"])
    assert cmd[-1] == "scripts/tests"
    assert "-q" in cmd


# ---- skip notice and result ------------------------------------------------------------------------------------------
def test_skips_with_visible_notice_when_no_launcher(monkeypatch, test_mod):
    monkeypatch.setattr(test_mod, "script_tests_launcher", lambda: None)
    said = []
    monkeypatch.setattr(test_mod, "say", lambda text, level="info": said.append((text, level)))
    assert test_mod.script_tests() == 0
    assert any("Skipping Script tests" in text and level == "warn" for text, level in said)


@pytest.mark.parametrize("code", [0, 1])
def test_returns_runner_exit_code(monkeypatch, test_mod, code):
    monkeypatch.setattr(test_mod, "say", lambda *a, **k: None)
    seen = {}

    def runner(cmd, env):
        seen["cmd"], seen["env"] = cmd, env
        return code
    assert test_mod.script_tests(launcher=["pytest"], runner=runner) == code
    assert seen["cmd"][0] == "pytest"
    assert seen["env"]["PYTHONDONTWRITEBYTECODE"] == "1"


def test_default_runner_is_its_own_pytest_row(monkeypatch, test_mod):
    monkeypatch.setattr(test_mod, "say", lambda *a, **k: None)
    calls = []

    class FakeRun:
        def run_tool(self, cmd, parser, step_id, title, **kwargs):
            calls.append((parser, step_id, title, kwargs.get("label")))
            return type("R", (), {"exit": 0})()
    monkeypatch.setattr(test_mod._juneau, "load", lambda: (FakeRun(),))
    assert test_mod.script_tests(launcher=["pytest"]) == 0
    assert calls == [("pytest", "py-tests", "Script tests", "Script tests")]


def test_skips_when_already_running_inside_the_suite(monkeypatch, test_mod):
    monkeypatch.setenv("JUNEAU_SCRIPT_TESTS_ACTIVE", "1")
    said = []
    monkeypatch.setattr(test_mod, "say", lambda text, level="info": said.append(text))
    ran = []
    assert test_mod.script_tests(launcher=["pytest"], runner=lambda cmd, env: ran.append(cmd) or 1) == 0
    assert not ran
    assert any("no recursion" in text for text in said)


def test_child_environment_carries_the_guard(monkeypatch, test_mod):
    monkeypatch.setattr(test_mod, "say", lambda *a, **k: None)
    seen = {}
    test_mod.script_tests(launcher=["pytest"], runner=lambda cmd, env: seen.update(env) or 0)
    assert seen["JUNEAU_SCRIPT_TESTS_ACTIVE"] == "1"


# ---- push.py plan ----------------------------------------------------------------------------------------------------
def test_push_plan_lists_the_row_after_tests(push_mod):
    args = argparse.Namespace(test_only=True, sonarqube=False, tracker_audit=False, skip_tests=False, js_tests=False)
    ids = [step_id for step_id, _ in push_mod.push_plan(args, SCRIPTS_DIR, SCRIPTS_DIR.parent, False)]
    assert ids.index("py-tests") == ids.index("tests") + 1
    assert push_mod.PLAN_LABELS["py-tests"] == "Script tests"
