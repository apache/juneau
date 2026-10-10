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
"""scripts/test.py under a console session: --detail, the session, PERF-GUARD notes and report, the JS -f switch."""

from __future__ import annotations

import importlib.util
import os
import signal
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
RUN_VARIABLES = ("RUN_MARKERS", "RUN_ARTIFACTS", "JUNEAU_RUN_ACTIVE", "JUNEAU_RUN_N_BASE", "JUNEAU_RUN_CONSOLE",
                 "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_CONDENSED_LOG", "JUNEAU_MVN_WRAPPER", "JUNEAU_RUN_SESSION",
                 "JUNEAU_RUN_DETAIL", "JUNEAU_RUN_LABEL_WIDTH", "JUNEAU_RUN_LIVE")


@pytest.fixture(autouse=True)
def clean_env(monkeypatch):
    for variable in RUN_VARIABLES:
        monkeypatch.delenv(variable, raising=False)
    sigterm = signal.getsignal(signal.SIGTERM)
    sys.modules.pop("juneau_run", None)
    yield
    sys.modules.pop("juneau_run", None)
    for variable in RUN_VARIABLES:   # session() and export_detail() write os.environ directly
        os.environ.pop(variable, None)
    signal.signal(signal.SIGTERM, sigterm)


@pytest.fixture
def test_mod():
    spec = importlib.util.spec_from_file_location("_console_test", SCRIPTS_DIR / "test.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class Events:
    def __init__(self):
        self.events = []

    def __call__(self, kind, fields):
        self.events.append((kind, fields))

    def of(self, kind):
        return [f for k, f in self.events if k == kind]


@pytest.fixture
def bus(test_mod):
    run = test_mod.run_module()
    return run.subscribe(Events())


def write_baseline(tmp_path):
    path = tmp_path / "perf-baseline.txt"
    path.write_text("suite = 100\nmod-a/unit = 10\nmod-b/unit = 10\n", encoding="utf-8")
    return path


ACTUALS = {"mod-a/unit": 20.0, "mod-b/unit": 9.0, "mod-c/unit": 1.0, "mod-d/unit": 2.0}


def perf(test_mod, monkeypatch, tmp_path, *, enforce, active, elapsed=130.0):
    monkeypatch.setattr(test_mod, "_actuals_from_surefire", lambda root: dict(ACTUALS))
    monkeypatch.setattr(test_mod.run_module(), "console_owns_screen", lambda: active)
    report = tmp_path / "perf-guard.txt"
    code = test_mod.run_perf_guard(elapsed, write_baseline(tmp_path), None, enforce=enforce, report_file=report)
    return code, report


# ---- PERF-GUARD --------------------------------------------------------------------------------------------------------
def test_perf_breaches_are_warn_notes_and_new_modules_one_info_note(test_mod, bus, monkeypatch, tmp_path):
    code, report = perf(test_mod, monkeypatch, tmp_path, enforce=False, active=True)
    assert code == 0
    notes = [(f["level"], f["text"].splitlines()[0]) for f in bus.of("note")]
    assert ("warn", "PERF-GUARD FAIL [suite]: tests took 130.0s (baseline 100.0s, tolerance ±20%, threshold 120.0s).") \
        in notes
    assert ("warn", "PERF-GUARD FAIL [mod-a/unit]: 20.0s (baseline 10.0s, tolerance ±20%, threshold 12.0s).") in notes
    assert [n for n in notes if "no baseline" in n[1]] == [
        ("info", "PERF-GUARD: 2 module-bucket(s) have no baseline entry yet: mod-c/unit, mod-d/unit")]
    assert all(f["step"] == "perf" for f in bus.of("note"))
    end = [f for f in bus.of("step_end") if f["id"] == "perf"][-1]
    assert end["summary"] == f"⚠ 2 over baseline · 2 without one → {report}"
    assert end["status"] == "ok"
    text = report.read_text(encoding="utf-8")
    assert "PERF-GUARD FAIL [mod-a/unit]" in text and "PERF-GUARD WARN [mod-c/unit]" in text
    assert "PERF-GUARD SUMMARY: 2 module-bucket(s) checked, 1 regression(s), 2 new/unknown." in text


def test_enforced_breaches_are_errors_and_fail_the_row(test_mod, bus, monkeypatch, tmp_path):
    code, _ = perf(test_mod, monkeypatch, tmp_path, enforce=True, active=True)
    assert code == 1
    assert {f["level"] for f in bus.of("note") if "FAIL" in f["text"]} == {"error"}
    assert [f for f in bus.of("step_end") if f["id"] == "perf"][-1]["status"] == "fail"


def test_a_clean_perf_run_says_how_many_were_checked(test_mod, bus, monkeypatch, tmp_path):
    monkeypatch.setattr(test_mod, "_actuals_from_surefire", lambda root: {"mod-a/unit": 9.0})
    monkeypatch.setattr(test_mod.run_module(), "console_owns_screen", lambda: True)
    report = tmp_path / "perf-guard.txt"
    assert test_mod.run_perf_guard(90.0, write_baseline(tmp_path), None, enforce=False, report_file=report) == 0
    assert [f for f in bus.of("step_end") if f["id"] == "perf"][-1]["summary"] == f"✓ 1 checked → {report}"


def test_without_a_console_perf_prints_as_before(test_mod, bus, monkeypatch, tmp_path, capsys):
    perf(test_mod, monkeypatch, tmp_path, enforce=False, active=False)
    out = capsys.readouterr().out
    assert "PERF-GUARD FAIL [suite]: tests took 130.0s" in out
    assert "PERF-GUARD WARN [mod-c/unit]: 1.0s — no baseline entry yet" in out
    assert "PERF-GUARD: warn-only mode (pass --enforce-perf to hard-fail on breach)." in out
    assert bus.of("note") == []


def test_perf_follows_the_same_screen_ownership_say_uses(test_mod, bus, monkeypatch, tmp_path, capsys):
    run = test_mod.run_module()
    monkeypatch.setattr(run, "console_active", lambda: False)
    perf(test_mod, monkeypatch, tmp_path, enforce=False, active=True)
    assert capsys.readouterr().out == ""
    assert any("PERF-GUARD FAIL [suite]" in f["text"] for f in bus.of("note"))


def test_perf_without_a_baseline_still_writes_the_report(test_mod, bus, monkeypatch, tmp_path):
    monkeypatch.setattr(test_mod.run_module(), "console_owns_screen", lambda: True)
    report = tmp_path / "perf-guard.txt"
    report.write_text("stale\n", encoding="utf-8")
    code = test_mod.run_perf_guard(5.0, tmp_path / "missing.txt", None, enforce=False, report_file=report)
    assert code == 0
    assert "baseline file not found" in report.read_text(encoding="utf-8")
    assert [f for f in bus.of("step_end") if f["id"] == "perf"][-1]["summary"] == f"no baseline file → {report}"


# ---- JS -f -------------------------------------------------------------------------------------------------------------
def test_js_tests_use_the_module_pom_after_an_install(test_mod, monkeypatch):
    calls = []
    monkeypatch.setattr(test_mod, "run_command", lambda cmd, *a, **k: calls.append(cmd) or (0, ""))
    test_mod.js_tests(installed=True)
    test_mod.js_tests(installed=False)
    assert calls[0].startswith(f"mvn -Pjs-tests -f {test_mod.JS_TEST_MODULE}/pom.xml test ")
    assert calls[1].startswith(f"mvn -Pjs-tests -pl {test_mod.JS_TEST_MODULE} -am test ")


@pytest.mark.parametrize("argv,installed", [([], True), (["--test-only"], False)])
def test_main_passes_whether_it_installed(test_mod, monkeypatch, argv, installed):
    seen = []
    monkeypatch.setattr(sys, "argv", ["test.py", "--js-tests", *argv])
    monkeypatch.setattr(test_mod, "build", lambda: (0, ""))
    monkeypatch.setattr(test_mod, "test", lambda no_container=False: (0, ""))
    monkeypatch.setattr(test_mod, "run_perf_guard", lambda *a, **k: 0)
    monkeypatch.setattr(test_mod, "maybe_run_js_tests",
                        lambda js, no_js, changed, runner=None, installed=False: seen.append(installed) or 0)
    assert test_mod._main() == 0
    assert seen == [installed]


# ---- --detail, session, Ctrl-C -----------------------------------------------------------------------------------------
def test_detail_is_exported(test_mod, monkeypatch):
    monkeypatch.setattr(sys, "argv", ["test.py", "--detail", "modules", "--profile", "m"])
    monkeypatch.setattr(test_mod, "profile", lambda module: (0, ""))
    assert test_mod._main() == 0
    assert os.environ["JUNEAU_RUN_DETAIL"] == "modules"


def test_an_unknown_detail_level_lists_the_valid_ones(test_mod, monkeypatch, capsys):
    monkeypatch.setattr(sys, "argv", ["test.py", "--detail", "loud"])
    assert test_mod._main() == 2
    assert "summary, actionable, modules, all" in capsys.readouterr().out


def test_a_session_starts_only_when_the_sinks_are_not_default(test_mod, monkeypatch):
    monkeypatch.setattr(test_mod, "profile", lambda module: (0, ""))
    monkeypatch.setattr(sys, "argv", ["test.py", "--profile", "m"])
    test_mod._main()
    assert test_mod.run_module()._console is None
    monkeypatch.setattr(sys, "argv", ["test.py", "--console", "none", "--profile", "m"])
    test_mod._main()
    assert test_mod.run_module()._console is not None


def test_ctrl_c_with_a_console_ends_cancelled(test_mod, bus, monkeypatch):
    run = test_mod.run_module()
    monkeypatch.setattr(run, "console_active", lambda: True)
    monkeypatch.setattr(test_mod, "git_value", lambda args: "x")

    def interrupted():
        raise KeyboardInterrupt

    monkeypatch.setattr(test_mod, "_main", interrupted)
    assert test_mod.main() == 130
    assert bus.of("done")[-1]["status"] == "cancelled"
