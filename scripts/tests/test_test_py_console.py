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
import json
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
    run = test_mod._juneau.load()[0]
    return run.subscribe(Events())


def write_baseline(tmp_path):
    path = tmp_path / "perf-baseline.txt"
    path.write_text("suite = 100\n[modules]\nmod-a = 10\nmod-b = 10\n", encoding="utf-8")
    return path


ACTUALS = {"mod-a": 20.0, "mod-b": 9.0, "mod-c": 1.0, "mod-d": 2.0}


def perf(test_mod, monkeypatch, tmp_path, *, enforce, active, elapsed=130.0):
    monkeypatch.setattr(test_mod._juneau.load()[0], "console_owns_screen", lambda: active)
    report = tmp_path / "perf-guard.txt"
    code = test_mod.run_perf_guard(elapsed, write_baseline(tmp_path), dict(ACTUALS), enforce=enforce,
                                   report_file=report)
    return code, report


# ---- PERF-GUARD --------------------------------------------------------------------------------------------------------
def test_perf_breaches_are_warn_notes_and_new_modules_one_info_note(test_mod, bus, monkeypatch, tmp_path):
    monkeypatch.delenv("JUNEAU_CI_PERF_MIN_SECONDS", raising=False)
    code, report = perf(test_mod, monkeypatch, tmp_path, enforce=False, active=True)
    assert code == 0
    notes = [(f["level"], f["text"].splitlines()[0]) for f in bus.of("note")]
    assert ("warn", "PERF-GUARD FAIL [suite]: tests took 130.0s (baseline 100.0s, tolerance ±20%, threshold 120.0s).") \
        in notes
    assert ("warn", "PERF-GUARD FAIL [mod-a]: 20.0s (baseline 10.0s, tolerance ±20% and 3s, threshold 13.0s).") \
        in notes
    assert [n for n in notes if "no baseline" in n[1]] == [
        ("info", "PERF-GUARD: 2 module(s) have no baseline entry yet: mod-c, mod-d")]
    assert all(f["step"] == "perf" for f in bus.of("note"))
    end = [f for f in bus.of("step_end") if f["id"] == "perf"][-1]
    assert end["summary"] == f"⚠ 2 over baseline · 2 without one → {report}"
    assert end["status"] == "ok"
    text = report.read_text(encoding="utf-8")
    assert "PERF-GUARD FAIL [mod-a]" in text and "PERF-GUARD WARN [mod-c]" in text
    assert "PERF-GUARD SUMMARY: 2 module(s) checked, 1 regression(s), 2 new/unknown." in text


def test_enforced_breaches_are_errors_and_fail_the_row(test_mod, bus, monkeypatch, tmp_path):
    code, _ = perf(test_mod, monkeypatch, tmp_path, enforce=True, active=True)
    assert code == 1
    assert {f["level"] for f in bus.of("note") if "FAIL" in f["text"]} == {"error"}
    assert [f for f in bus.of("step_end") if f["id"] == "perf"][-1]["status"] == "fail"


def test_a_clean_perf_run_says_how_many_were_checked(test_mod, bus, monkeypatch, tmp_path):
    monkeypatch.setattr(test_mod._juneau.load()[0], "console_owns_screen", lambda: True)
    report = tmp_path / "perf-guard.txt"
    assert test_mod.run_perf_guard(90.0, write_baseline(tmp_path), {"mod-a": 9.0}, enforce=False,
                                    report_file=report) == 0
    assert [f for f in bus.of("step_end") if f["id"] == "perf"][-1]["summary"] == f"✓ 1 checked → {report}"


def test_without_a_console_perf_prints_as_before(test_mod, bus, monkeypatch, tmp_path, capsys):
    perf(test_mod, monkeypatch, tmp_path, enforce=False, active=False)
    out = capsys.readouterr().out
    assert "PERF-GUARD FAIL [suite]: tests took 130.0s" in out
    assert "PERF-GUARD WARN [mod-c]: 1.0s — no baseline entry yet" in out
    assert "PERF-GUARD: warn-only mode (pass --enforce-perf to hard-fail on breach)." in out
    assert bus.of("note") == []


def test_perf_follows_the_same_screen_ownership_say_uses(test_mod, bus, monkeypatch, tmp_path, capsys):
    run = test_mod._juneau.load()[0]
    monkeypatch.setattr(run, "console_active", lambda: False)
    perf(test_mod, monkeypatch, tmp_path, enforce=False, active=True)
    assert capsys.readouterr().out == ""
    assert any("PERF-GUARD FAIL [suite]" in f["text"] for f in bus.of("note"))


def test_perf_without_a_baseline_still_writes_the_report(test_mod, bus, monkeypatch, tmp_path):
    monkeypatch.setattr(test_mod._juneau.load()[0], "console_owns_screen", lambda: True)
    report = tmp_path / "perf-guard.txt"
    report.write_text("stale\n", encoding="utf-8")
    code = test_mod.run_perf_guard(5.0, tmp_path / "missing.txt", {}, enforce=False, report_file=report)
    assert code == 0
    assert "baseline file not found" in report.read_text(encoding="utf-8")
    assert [f for f in bus.of("step_end") if f["id"] == "perf"][-1]["summary"] == f"no baseline file → {report}"


# ---- browser step scope ------------------------------------------------------------------------------------------------
def test_js_tests_cover_the_views_and_petstore_modules(test_mod, monkeypatch):
    modules = dict(test_mod.JS_TEST_MODULES)
    assert set(modules) == {"juneau-rest/juneau-rest-server-views", "juneau-petstore/juneau-petstore-core",
                       "juneau-rest/juneau-rest-server-terminal"}
    # Package-qualified, so one module's pattern cannot select the other's suite.
    assert modules["juneau-rest/juneau-rest-server-views"] == "org.apache.juneau.rest.server.views.*_BrowserTest"
    assert modules["juneau-petstore/juneau-petstore-core"] == "org.apache.juneau.petstore.**.*_BrowserTest"
    assert modules["juneau-rest/juneau-rest-server-terminal"] == "org.apache.juneau.rest.server.terminal.*_BrowserTest"
    calls = []
    monkeypatch.delenv("JUNEAU_MVN_WRAPPER", raising=False)
    monkeypatch.setattr(test_mod, "run_command", lambda cmd, *a, **k: calls.append(cmd) or (0, ""))
    test_mod.js_tests(installed=False)
    test_mod.js_tests(installed=True)
    cmd = calls[0]
    # Not just installed: one reactor run (upstream modules built from the working tree by --also-make).
    assert calls[0] != calls[1]
    assert cmd.startswith("mvn -Pjs-tests -pl juneau-rest/juneau-rest-server-views,juneau-petstore/juneau-petstore-core,"
                              "juneau-rest/juneau-rest-server-terminal test ")
    assert "-f " not in cmd
    assert ("-Dtest='org.apache.juneau.rest.server.views.*_BrowserTest,"
            "org.apache.juneau.petstore.**.*_BrowserTest,org.apache.juneau.rest.server.terminal.*_BrowserTest'") in cmd
    assert "-Dsurefire.failIfNoSpecifiedTests=false" in cmd
    # Just installed: one mvn -f per module, all at once.
    assert all(f"-f {module}/pom.xml" in calls[1] for module in modules)


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


def test_a_console_session_opens_only_when_the_sinks_are_not_default(test_mod, monkeypatch):
    """The toolkit session always runs; the console view (jr.session) opens only when the sinks aren't the default."""
    monkeypatch.setattr(test_mod, "profile", lambda module: (0, ""))
    monkeypatch.setattr(test_mod, "git_value", lambda args: "b1")
    opened = []
    monkeypatch.setattr(test_mod._juneau.load()[0], "session", lambda title, header=(), **kw: opened.append((title, header)))
    monkeypatch.setattr(sys, "argv", ["test.py", "--profile", "m"])
    assert test_mod.main() == 0
    assert opened == []
    monkeypatch.setattr(sys, "argv", ["test.py", "--console", "none", "--profile", "m"])
    assert test_mod.main() == 0
    assert opened == [("Test", ["Juneau", "b1"])]


def test_test_py_opens_one_toolkit_session_and_one_done(test_mod, monkeypatch, capsys):
    import contextlib
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setattr(test_mod, "git_value", lambda args: "b1")
    test_mod._juneau.load()
    from juneau_scripts import run as toolkit
    real, entered = toolkit.session, []

    @contextlib.contextmanager
    def spy(title, **kw):
        entered.append((title, kw))
        with real(title, **kw) as s:
            yield s

    monkeypatch.setattr(toolkit, "session", spy)
    monkeypatch.setattr(test_mod, "_main", lambda: 3)
    assert test_mod.main() == 3
    assert len(entered) == 1
    title, kw = entered[0]
    root = SCRIPTS_DIR.parent
    assert (title, kw["header"], kw["mode"], kw["project"], Path(kw["repo"]), kw["bell"], kw["exit"]) == \
        ("Test", ["Juneau", "b1"], "test", root.name, root, True, False)
    assert tuple(kw["silence_env"]) == ("JUNEAU_PUSH_NO_SOUND",)
    out = capsys.readouterr().out
    events = [line for line in out.splitlines() if line.startswith("##run ")]
    assert sum('"ev":"run"' in e.replace(" ", "") for e in events) == 1
    dones = [e for e in events if '"ev":"done"' in e.replace(" ", "")]
    assert len(dones) == 1 and '"fail"' in dones[0]


def test_ctrl_c_with_a_console_ends_cancelled(test_mod, bus, monkeypatch):
    run = test_mod._juneau.load()[0]
    monkeypatch.setattr(run, "console_active", lambda: True)
    monkeypatch.setattr(test_mod, "git_value", lambda args: "x")

    def interrupted():
        raise KeyboardInterrupt

    monkeypatch.setattr(test_mod, "_main", interrupted)
    assert test_mod.main() == 130
    assert bus.of("done")[-1]["status"] == "cancelled"


def test_test_py_plain_ctrl_c_is_a_cancel(test_mod, monkeypatch, capsys):
    """Markers off and no console: the toolkit session turns Ctrl-C into "❌ Cancelled." and exit 130 (it used to
    propagate the KeyboardInterrupt silently)."""
    monkeypatch.setattr(test_mod, "git_value", lambda args: "x")

    def interrupted():
        raise KeyboardInterrupt

    monkeypatch.setattr(test_mod, "_main", interrupted)
    assert test_mod.main() == 130
    captured = capsys.readouterr()
    assert "❌ Cancelled." in captured.out
    assert "##run" not in captured.out


# ---- the plan ----------------------------------------------------------------------------------------------------------
def _stub_steps(test_mod, monkeypatch):
    monkeypatch.setattr(test_mod, "build", lambda: (0, ""))
    monkeypatch.setattr(test_mod, "test", lambda no_container=False: (0, ""))
    monkeypatch.setattr(test_mod, "run_perf_guard", lambda *a, **k: 0)
    monkeypatch.setattr(test_mod, "script_tests", lambda *a, **k: 0)
    monkeypatch.setattr(test_mod, "changed_files_vs_origin", lambda: [])
    monkeypatch.setattr(test_mod, "maybe_run_js_tests", lambda *a, **k: 0)


@pytest.mark.parametrize("argv,ids", [
    ([], ["build", "tests", "perf", "py-tests", "js-tests"]),
    (["--full"], ["build", "tests", "perf", "py-tests", "js-tests"]),
    (["--build-only"], ["build"]),
    (["--test-only"], ["tests", "perf", "py-tests", "js-tests"]),
    (["--test-only", "--no-js-tests"], ["tests", "perf", "py-tests"]),
    (["--no-js-tests"], ["build", "tests", "perf", "py-tests"]),
    (["--js-tests"], ["build", "tests", "perf", "py-tests", "js-tests"]),
])
def test_main_declares_the_steps_its_own_flags_will_run(test_mod, bus, monkeypatch, argv, ids):
    _stub_steps(test_mod, monkeypatch)
    monkeypatch.setattr(sys, "argv", ["test.py", *argv])
    assert test_mod._main() == 0
    plans = bus.of("plan")
    assert [p["ids"] for p in plans] == [ids]
    assert set(plans[0]["labels"]) == set(ids)          # every step is labelled, so each draws a pending row


@pytest.mark.parametrize("argv", [["--build-only", "--test-only"], ["--js-tests", "--no-js-tests"], ["--profile", "m"]])
def test_a_refused_combination_or_a_profile_run_declares_no_plan(test_mod, bus, monkeypatch, argv):
    _stub_steps(test_mod, monkeypatch)
    monkeypatch.setattr(test_mod, "profile", lambda module: (0, ""))
    monkeypatch.setattr(sys, "argv", ["test.py", *argv])
    test_mod._main()
    assert bus.of("plan") == []


def test_the_plan_labels_are_push_pys(test_mod):
    """Under push.py the child's pending rows replace the parent's, so a label must read the same in both."""
    spec = importlib.util.spec_from_file_location("_push_labels", SCRIPTS_DIR / "push.py")
    push = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(push)
    assert {i: label for i, label in test_mod.PLAN} == {i: push.PLAN_LABELS[i] for i, _ in test_mod.PLAN}


def test_a_nested_run_under_a_parents_overall_has_an_overall_figure(test_mod, monkeypatch):
    run = test_mod._juneau.load()[0]
    monkeypatch.setenv("JUNEAU_RUN_OVERALL", json.dumps({"elapsed": 60, "done": 2, "pending": [
        ["tests", 120], ["perf", None], ["py-tests", 20], ["js-tests", 40], ["install", 10]]}))
    board = run.Board(nested=True)
    feed = run.subscribe(lambda kind, fields: board.apply(kind, fields))
    try:
        _stub_steps(test_mod, monkeypatch)
        monkeypatch.setattr(sys, "argv", ["test.py", "--test-only"])
        assert board.overall() is None                    # no plan yet: no Overall (today's nested test.py)
        assert test_mod._main() == 0
        assert board.plan == ["tests", "perf", "py-tests", "js-tests"]
        assert board.overall() is not None                # the view's region now ends with ▶ Overall
    finally:
        run.unsubscribe(feed)


def test_skipped_python_tests_still_get_their_row(test_mod, bus, monkeypatch):
    monkeypatch.setenv(test_mod.SCRIPT_TESTS_ACTIVE, "1")
    assert test_mod.script_tests() == 0
    ends = [f for f in bus.of("step_end") if f["id"] == "py-tests"]
    assert [e["status"] for e in ends] == ["skip"]
