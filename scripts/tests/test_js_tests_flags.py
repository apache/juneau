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
Tests for the --js-tests / --no-js-tests support in scripts/test.py and scripts/push.py:
flag parsing, auto-detect of changed JS/CSS/FTL files (driven by a fake changed-file list, no git),
and the prerequisite-missing path (skip with notice when auto-enabled, fail when explicit).
Nothing here runs mvn, git against a real remote, or push.py's main().
"""

from __future__ import annotations

import argparse
import importlib.util
import shlex
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


def _load(name):
    spec = importlib.util.spec_from_file_location(f"_undertest_{name}", SCRIPTS_DIR / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def test_mod():
    return _load("test")


@pytest.fixture
def push_mod():
    return _load("push")


# ---- auto-detect -----------------------------------------------------------------------------------------------------
@pytest.mark.parametrize("path,expected", [
    ("juneau-rest/juneau-rest-server-views/src/main/resources/juneau-views.js", True),
    ("juneau-rest/x/src/main/resources/style.CSS", True),
    ("juneau-rest/x/src/main/resources/templates/page.ftl", True),
    ("a/src/test/js/row-actions.js", True),
    ("docs/site/app.js", False),            # no src/ segment
    ("src.js", False),                      # file named src, not a directory
    ("juneau-petstore/juneau-petstore-core/src/main/resources/console/petstore.css", True),
    ("juneau-petstore/juneau-petstore-core/src/test/js/petstore-browser.cjs", True),
    ("juneau-rest/juneau-rest-server-terminal/src/main/resources/terminal.js", True),
    ("juneau-rest/juneau-rest-server-terminal/src/test/js/terminal-prober.cjs", True),
    ("juneau-core/src/main/java/Foo.java", False),
    ("a/src/main/resources/readme.md", False),
])
def test_is_js_source(test_mod, path, expected):
    assert test_mod.is_js_source(path) is expected


def test_auto_detect_enabled_only_when_js_changed(test_mod):
    assert test_mod.should_run_js_tests(False, False, ["a/src/x.js"]) == (True, False)
    assert test_mod.should_run_js_tests(False, False, ["a/src/X.java"]) == (False, False)
    assert test_mod.should_run_js_tests(False, False, []) == (False, False)


def test_auto_detect_triggers_on_a_petstore_source_but_not_an_unrelated_change(test_mod):
    petstore = "juneau-petstore/juneau-petstore-core/src/main/resources/templates/store.ftl"
    assert test_mod.should_run_js_tests(False, False, [petstore]) == (True, False)
    assert test_mod.should_run_js_tests(False, False, ["scripts/test.py", "pom.xml"]) == (False, False)
    assert test_mod.should_run_js_tests(False, True, [petstore]) == (False, False)


def test_explicit_flags_override_detection(test_mod):
    assert test_mod.should_run_js_tests(True, False, []) == (True, True)
    assert test_mod.should_run_js_tests(False, True, ["a/src/x.js"]) == (False, False)
    assert test_mod.should_run_js_tests(True, True, ["a/src/x.js"]) == (False, False)


# ---- prereq-missing path ---------------------------------------------------------------------------------------------
def test_prereq_reports_missing_tools(test_mod, monkeypatch):
    monkeypatch.setattr(test_mod.shutil, "which", lambda t: None if t == "npm" else "/bin/" + t)
    assert "npm" in test_mod.js_prereq_problem()
    monkeypatch.setattr(test_mod.shutil, "which", lambda t: "/bin/" + t)
    assert test_mod.js_prereq_problem() is None


def test_auto_enabled_skips_with_notice_when_prereqs_missing(test_mod, monkeypatch, capsys):
    monkeypatch.setattr(test_mod, "js_prereq_problem", lambda: "node not found on the PATH")
    calls = []
    rc = test_mod.maybe_run_js_tests(False, False, ["a/src/x.js"], runner=lambda: calls.append(1) or (0, ""))
    assert rc == 0 and calls == []
    assert "skipping Browser Tests" in capsys.readouterr().out


def test_console_label_says_browser_tests():
    src = (SCRIPTS_DIR / "test.py").read_text()
    assert 'label="Browser Tests"' in src
    assert 'label="JS tests"' not in src
    assert '"js-tests"' in src   # the step id is unchanged


def test_explicit_fails_when_prereqs_missing(test_mod, monkeypatch, capsys):
    monkeypatch.setattr(test_mod, "js_prereq_problem", lambda: "node not found on the PATH")
    calls = []
    rc = test_mod.maybe_run_js_tests(True, False, [], runner=lambda: calls.append(1) or (0, ""))
    assert rc == 1 and calls == []
    assert "--js-tests requested but cannot run" in capsys.readouterr().out


def test_runs_and_propagates_exit_code(test_mod, monkeypatch):
    monkeypatch.setattr(test_mod, "js_prereq_problem", lambda: None)
    assert test_mod.maybe_run_js_tests(True, False, [], runner=lambda: (0, "")) == 0
    assert test_mod.maybe_run_js_tests(True, False, [], runner=lambda: (1, "")) == 1


def test_disabled_never_runs_or_checks_prereqs(test_mod, monkeypatch):
    def boom():
        raise AssertionError("prereqs must not be checked when disabled")
    monkeypatch.setattr(test_mod, "js_prereq_problem", boom)
    assert test_mod.maybe_run_js_tests(False, True, ["a/src/x.js"], runner=boom) == 0
    assert test_mod.maybe_run_js_tests(False, False, ["a/src/X.java"], runner=boom) == 0


def test_changed_files_vs_origin_returns_empty_on_git_failure(test_mod, tmp_path):
    assert test_mod.changed_files_vs_origin(repo_root=tmp_path) == []  # not a git repo


# ---- flag parsing ----------------------------------------------------------------------------------------------------
def test_test_py_rejects_conflicting_flags(test_mod, monkeypatch, capsys):
    monkeypatch.setattr(sys, "argv", ["test.py", "--js-tests", "--no-js-tests"])
    assert test_mod.main() == 1
    assert "Cannot combine --js-tests and --no-js-tests" in capsys.readouterr().out


def test_test_py_help_documents_flags(test_mod):
    assert "--js-tests" in test_mod.USAGE and "--no-js-tests" in test_mod.USAGE


def test_test_py_accepts_flags(test_mod, monkeypatch):
    # --build-only with a stubbed build proves parsing accepts the flags and nothing else runs.
    monkeypatch.setattr(test_mod, "build", lambda: (0, ""))
    monkeypatch.setattr(sys, "argv", ["test.py", "--build-only", "--js-tests"])
    assert test_mod.main() == 0


def test_push_py_doc_documents_flags():
    doc = (SCRIPTS_DIR / "push.md").read_text()
    assert "`--js-tests`" in doc and "`--no-js-tests`" in doc


def test_push_build_test_command_forwards_flags(push_mod):
    base = push_mod.build_test_command("t.py", argparse.Namespace(js_tests=False, no_js_tests=False))
    assert "--js-tests" not in base and "--no-js-tests" not in base and base[2] == "--full"
    assert "--js-tests" in push_mod.build_test_command("t.py", argparse.Namespace(js_tests=True, no_js_tests=False))
    assert "--no-js-tests" in push_mod.build_test_command("t.py", argparse.Namespace(js_tests=False, no_js_tests=True))


def test_push_py_rejects_conflicting_flags(push_mod, monkeypatch, capsys):
    monkeypatch.setattr(sys, "argv", ["push.py", "msg", "--js-tests", "--no-js-tests"])
    with pytest.raises(SystemExit) as e:
        push_mod.main()
    assert e.value.code == 2
    assert "mutually exclusive" in capsys.readouterr().err


# ---- the Browser Tests row is always shown ---------------------------------------------------------------------------
@pytest.fixture
def skipped_rows(test_mod, monkeypatch):
    rows = []
    monkeypatch.setattr(test_mod, "skip_js_tests_row", rows.append)
    return rows


def test_no_js_changes_skips_the_row_with_the_force_hint(test_mod, skipped_rows):
    assert test_mod.maybe_run_js_tests(False, False, ["a/src/X.java"], runner=lambda: (1, "")) == 0
    assert skipped_rows == ["no JS changes since origin/master (--js-tests to force)"]


def test_no_js_tests_flag_skips_the_row_with_the_flag(test_mod, skipped_rows):
    assert test_mod.maybe_run_js_tests(False, True, ["a/src/x.js"], runner=lambda: (1, "")) == 0
    assert skipped_rows == ["--no-js-tests"]


def test_missing_node_skips_the_row_with_the_reason(test_mod, monkeypatch, skipped_rows):
    monkeypatch.setattr(test_mod.shutil, "which", lambda t: None if t == "node" else "/bin/" + t)
    assert test_mod.maybe_run_js_tests(False, False, ["a/src/x.js"], runner=lambda: (1, "")) == 0
    assert len(skipped_rows) == 1 and "node not found on the PATH" in skipped_rows[0]


def test_a_run_or_a_forced_failure_draws_no_skip_row(test_mod, monkeypatch, skipped_rows):
    monkeypatch.setattr(test_mod, "js_prereq_problem", lambda: None)
    assert test_mod.maybe_run_js_tests(True, False, [], runner=lambda: (0, "")) == 0
    monkeypatch.setattr(test_mod, "js_prereq_problem", lambda: "node not found on the PATH")
    assert test_mod.maybe_run_js_tests(True, False, [], runner=lambda: (0, "")) == 1
    assert skipped_rows == []


def test_skip_row_is_a_skipped_browser_tests_row(test_mod, monkeypatch):
    seen = []

    class FakeStep:
        def skip(self, reason):
            seen.append(("skip", reason))

    class FakeRow:
        def __enter__(self):
            return FakeStep()

        def __exit__(self, *exc):
            return False

    class FakeRun:
        def row(self, step_id, title, label=None):
            seen.append(("row", step_id, title, label))
            return FakeRow()
    monkeypatch.setattr(test_mod._juneau, "load", lambda: (FakeRun(),))
    test_mod.skip_js_tests_row("why")
    assert seen == [("row", "js-tests", "Browser Tests", "Browser Tests"), ("skip", "why")]


@pytest.mark.parametrize("js_tests", [False, True])
def test_push_always_plans_the_browser_tests_row(push_mod, js_tests):
    args = argparse.Namespace(test_only=True, sonarqube=False, tracker_audit=False, skip_tests=False, js_tests=js_tests)
    ids = [step_id for step_id, _ in push_mod.push_plan(args, SCRIPTS_DIR, SCRIPTS_DIR.parent, False)]
    assert ids.index("js-tests") == ids.index("py-tests") + 1


# ---- concurrent module runs --------------------------------------------------------------------------------------------
def test_concurrent_command_runs_one_f_process_per_module(test_mod):
    cmd = test_mod.js_tests_concurrent_command()
    for module, pattern in test_mod.JS_TEST_MODULES:
        assert f"-f {module}/pom.xml" in cmd and f"-Dtest='{pattern}'" in cmd
        assert f"[{module}] " in cmd                       # live output is labelled per module
    assert cmd.count("mvn -o -Pjs-tests") == len(test_mod.JS_TEST_MODULES)   # offline: upstream modules come from ~/.m2
    assert "-Djuneau.jsTests.forks=2" in cmd
    assert " wait " in cmd


def test_concurrent_command_fails_when_a_module_leaves_no_exit_status(test_mod):
    cmd = test_mod.js_tests_concurrent_command()
    # A killed subshell writes no status file; reading none must count as a failure, not a pass.
    assert cmd.count("|| echo 1") == len(test_mod.JS_TEST_MODULES)
    assert cmd.rstrip().endswith("do [ \"$c\" = 0 ] || exit 1; done")


def test_js_tests_uses_the_reactor_command_unless_just_installed(test_mod, monkeypatch):
    monkeypatch.delenv("JUNEAU_MVN_WRAPPER", raising=False)
    seen = []
    monkeypatch.setattr(test_mod, "run_command", lambda cmd, *a, **k: seen.append(cmd) or (0, ""))
    test_mod.js_tests(installed=False)
    test_mod.js_tests(installed=True)
    assert seen[0] == test_mod.js_tests_command()
    # The concurrent form needs bash (PIPESTATUS), so it always runs under an explicit bash -c.
    assert seen[1] == f"/bin/bash -c {shlex.quote(test_mod.js_tests_concurrent_command())}"


def test_concurrent_form_keeps_the_mvn_wrapper_around_the_whole_compound(test_mod, monkeypatch):
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", "/x/lock.sh")
    seen = []
    monkeypatch.setattr(test_mod, "run_command", lambda cmd, *a, **k: seen.append(cmd) or (0, ""))
    test_mod.js_tests(installed=True)
    assert seen[0] == f"/x/lock.sh /bin/bash -c {shlex.quote(test_mod.js_tests_concurrent_command())}"
