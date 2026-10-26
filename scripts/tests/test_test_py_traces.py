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
"""scripts/test.py's Traces row: the wiring around juneau_terminal_scripts.traces, --warn-traces, and push.py's side of it."""

from __future__ import annotations

import argparse
import importlib.util
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
TRACE = ("java.lang.IllegalStateException: boom\n"
         "\tat com.example.Foo.run(Foo.java:10)\n"
         "\tat com.example.Foo.main(Foo.java:4)\n")


def _load(name, filename):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS_DIR / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def test_mod(tmp_path, monkeypatch):
    for variable in ("TERMINAL_MARKERS", "JUNEAU_TERMINAL_ACTIVE", "JUNEAU_TERMINAL_CONSOLE", "JUNEAU_TERMINAL_FULL_LOG"):
        monkeypatch.delenv(variable, raising=False)
    sys.modules.pop("juneau_terminal", None)
    module = _load("_traces_test", "test.py")
    monkeypatch.setattr(module, "TRACE_ALLOWLIST", tmp_path / "trace-allowlist.txt")
    monkeypatch.setattr(module, "TRACE_REPORT", tmp_path / "target" / "trace-guard.txt")
    log = tmp_path / "full.log"
    log.write_text("", encoding="utf-8")
    monkeypatch.setenv("JUNEAU_TERMINAL_FULL_LOG", str(log))
    module.log = log
    yield module
    sys.modules.pop("juneau_terminal", None)


@pytest.fixture
def traces(test_mod):
    test_mod._juneau.load()
    from juneau_terminal_scripts import traces
    return traces


def _stub_steps(test_mod, monkeypatch):
    monkeypatch.setattr(test_mod, "build", lambda: (0, ""))
    monkeypatch.setattr(test_mod, "test", lambda no_container=False: (0, ""))
    monkeypatch.setattr(test_mod, "run_perf_guard", lambda *a, **k: 0)
    monkeypatch.setattr(test_mod, "script_tests", lambda *a, **k: 0)
    monkeypatch.setattr(test_mod, "changed_files_vs_origin", lambda: [])
    monkeypatch.setattr(test_mod, "maybe_run_js_tests", lambda *a, **k: 0)


def test_a_trace_written_after_the_mark_fails_the_row(test_mod, traces):
    since = traces.mark()
    test_mod.log.write_text(TRACE, encoding="utf-8")
    assert test_mod.run_trace_guard(since, False, True) == 1
    assert "IllegalStateException: boom" in test_mod.TRACE_REPORT.read_text(encoding="utf-8")


def test_warn_only_downgrades_a_trace_to_a_pass(test_mod, traces):
    since = traces.mark()
    test_mod.log.write_text(TRACE, encoding="utf-8")
    assert test_mod.run_trace_guard(since, True, True) == 0


def test_an_allowlisted_trace_passes(test_mod, traces):
    test_mod.TRACE_ALLOWLIST.write_text("^java\\.lang\\.IllegalStateException: boom$  # asserted by the Foo test\n",
                                        encoding="utf-8")
    since = traces.mark()
    test_mod.log.write_text(TRACE, encoding="utf-8")
    assert test_mod.run_trace_guard(since, False, True) == 0


def test_the_shipped_allowlist_parses_clean(traces):
    assert traces.load_allowlist(SCRIPTS_DIR / "trace-allowlist.txt").errors == ()


def test_a_full_run_scans_from_a_mark_taken_before_the_build(test_mod, monkeypatch):
    _stub_steps(test_mod, monkeypatch)
    seen = []
    monkeypatch.setattr(test_mod, "run_trace_guard", lambda since, warn, full: seen.append((warn, full)) or 0)
    monkeypatch.setattr(sys, "argv", ["test.py", "--test-only", "--warn-traces"])
    assert test_mod._main() == 0
    assert seen == [(True, True)]


@pytest.mark.parametrize("argv,full_run", [(["--test-only"], True), (["--test-only", "--no-container"], False),
                                           (["--test-only", "--no-js-tests"], False)])
def test_stale_allowlist_reporting_is_for_runs_with_nothing_left_out(test_mod, monkeypatch, argv, full_run):
    _stub_steps(test_mod, monkeypatch)
    seen = []
    monkeypatch.setattr(test_mod, "run_trace_guard", lambda since, warn, full: seen.append(full) or 0)
    monkeypatch.setattr(sys, "argv", ["test.py", *argv])
    assert test_mod._main() == 0
    assert seen == [full_run]


def test_a_skipped_browser_row_is_not_a_full_run(test_mod, monkeypatch):
    _stub_steps(test_mod, monkeypatch)
    monkeypatch.setattr(test_mod, "maybe_run_js_tests", lambda *a, **k: setattr(test_mod, "_js_skipped", True) or 0)
    seen = []
    monkeypatch.setattr(test_mod, "run_trace_guard", lambda since, warn, full: seen.append(full) or 0)
    monkeypatch.setattr(sys, "argv", ["test.py", "--test-only"])
    assert test_mod._main() == 0
    assert seen == [False]


def test_a_build_only_run_never_scans(test_mod, monkeypatch):
    _stub_steps(test_mod, monkeypatch)
    monkeypatch.setattr(test_mod, "run_trace_guard", lambda *a: pytest.fail("the guard ran"))
    monkeypatch.setattr(sys, "argv", ["test.py", "--build-only"])
    assert test_mod._main() == 0


def test_a_failing_guard_fails_the_run(test_mod, monkeypatch):
    _stub_steps(test_mod, monkeypatch)
    monkeypatch.setattr(test_mod, "run_trace_guard", lambda *a: 1)
    monkeypatch.setattr(sys, "argv", ["test.py", "--test-only"])
    assert test_mod._main() == 1


def test_push_forwards_warn_traces_to_test_py():
    push = _load("_traces_push", "push.py")
    plain = push.build_test_command("t.py", argparse.Namespace(js_tests=False, no_js_tests=False, warn_traces=False))
    warned = push.build_test_command("t.py", argparse.Namespace(js_tests=False, no_js_tests=False, warn_traces=True))
    assert "--warn-traces" not in plain and warned[-1] == "--warn-traces"


def test_push_plans_the_traces_row_after_the_browser_tests_unless_tests_are_skipped(tmp_path):
    push = _load("_traces_push_plan", "push.py")
    scripts = tmp_path / "scripts"
    scripts.mkdir()
    (scripts / "test.py").write_text("", encoding="utf-8")
    ran = argparse.Namespace(test_only=True, tracker_audit=False, skip_tests=False)
    skipped = argparse.Namespace(test_only=True, tracker_audit=False, skip_tests=True)
    ids = [i for i, _ in push.push_plan(ran, scripts, tmp_path / "juneau", False)]
    assert ids[ids.index("js-tests") + 1] == "traces"
    assert "traces" not in [i for i, _ in push.push_plan(skipped, scripts, tmp_path / "juneau", False)]


def test_the_rows_labels_match_between_the_two_scripts(test_mod):
    push = _load("_traces_push_labels", "push.py")
    assert dict(test_mod.PLAN)["traces"] == push.PLAN_LABELS["traces"] == "Traces"
