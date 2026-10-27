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
Tests for push.py and test.py's adoption of juneau-terminal: marker streams with a fake `mvn` on PATH, the
`--test-only` mode of push.py, and the pins that keep the default (markers off) behaviour unchanged.
Hermetic: no real Maven, no git writes, no sounds; push.py's gates are replaced by recorders.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import os
import shutil
import signal
import stat
import subprocess
import sys
import time
from pathlib import Path
from types import SimpleNamespace

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
PREFIX = "##terminal "


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS_DIR / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


RUN_VARIABLES = ("TERMINAL_MARKERS", "TERMINAL_ARTIFACTS", "JUNEAU_TERMINAL_ACTIVE", "JUNEAU_TERMINAL_N_BASE", "JUNEAU_TERMINAL_CONSOLE",
                 "JUNEAU_TERMINAL_FULL_LOG", "JUNEAU_TERMINAL_BOARD_LOG", "JUNEAU_MVN_WRAPPER", "JUNEAU_TERMINAL_SESSION",
                 "JUNEAU_TERMINAL_DETAIL", "JUNEAU_TERMINAL_LABEL_WIDTH", "JUNEAU_TERMINAL_LIVE", "JUNEAU_PUSH_ACTIVE")


@pytest.fixture(autouse=True)
def clean_env(monkeypatch, tmp_path):
    monkeypatch.setenv("JUNEAU_TERMINAL_LOG_DIR", str(tmp_path / "run-logs"))
    for variable in RUN_VARIABLES:
        monkeypatch.delenv(variable, raising=False)
    # push.py now defaults to the board console; these tests assert on the full-console output.  Set (not just
    # unset) so teardown restores the variable and a main() that exports the default can't leak it to other tests.
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "full")
    sigterm = signal.getsignal(signal.SIGTERM)
    sys.modules.pop("juneau_terminal", None)
    yield
    module = sys.modules.pop("juneau_terminal", None)
    if module is not None and hasattr(module, "_end_session"):
        module._end_session()
    for variable in RUN_VARIABLES:   # session() and export_detail() write os.environ directly
        os.environ.pop(variable, None)
    signal.signal(signal.SIGTERM, sigterm)


@pytest.fixture
def test_mod():
    return load("_adopt_test", "test.py")


@pytest.fixture
def push_mod():
    return load("_adopt_push", "push.py")


@pytest.fixture
def fake_mvn(tmp_path, monkeypatch):
    """A `mvn` on PATH that prints one line and exits 0 (or $FAKE_MVN_EXIT)."""
    bin_dir = tmp_path / "bin"
    bin_dir.mkdir()
    mvn = bin_dir / "mvn"
    mvn.write_text("#!/bin/sh\necho \"fake mvn: $*\"\nexit ${FAKE_MVN_EXIT:-0}\n", encoding="utf-8")
    mvn.chmod(mvn.stat().st_mode | stat.S_IEXEC)
    monkeypatch.setenv("PATH", f"{bin_dir}{os.pathsep}{os.environ['PATH']}")


def markers(text):
    found = []
    for line in text.splitlines():
        if line.startswith(PREFIX):
            event = json.loads(line[len(PREFIX):])
            for volatile in ("ms", "head", "branch"):
                event.pop(volatile, None)
            found.append(event)
    return found


# ----------------------------------------------------------------------------------------------------------------------
# test.py
# ----------------------------------------------------------------------------------------------------------------------

def test_test_py_default_path_records_history_through_run_tool(test_mod, fake_mvn, run_history, tmp_path, capfd):
    """Markers off and default sinks: a step-id command still goes through run_tool, so its row is recorded."""
    run_history.start(tmp_path)
    code, out = test_mod.run_command("mvn test", "tests", "Java Tests")
    assert (code, out) == (0, "fake mvn: test\n")
    assert "##terminal" not in capfd.readouterr().out
    records = run_history.store("test", repo=tmp_path).read()
    assert [r["step"] for r in records] == ["tests"]


def test_test_py_default_path_output_is_unchanged(test_mod, fake_mvn, capfd):
    code, _ = test_mod.run_command("mvn test", "tests", "Java Tests")
    assert code == 0
    assert capfd.readouterr().out == "Running: mvn test\n" + "-" * 80 + "\nfake mvn: test\n"


def test_mvn_wrapper_with_and_still_runs_through_the_shell(test_mod, monkeypatch):
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", "echo first &&")
    monkeypatch.setattr(test_mod._juneau.load()[0], "run_tool", _counting_run_tool(test_mod._juneau.load()[0].run_tool))
    _, out = test_mod.run_command("mvn --version", "tests", "Java Tests")
    assert out.startswith("first\n")  # the && part ran in the same shell as the rest of the command
    assert test_mod._juneau.load()[0].run_tool.calls == 1


def test_a_command_without_a_step_id_keeps_the_popen_path(test_mod, monkeypatch, capfd):
    run = test_mod._juneau.load()[0]
    monkeypatch.setattr(run, "run_tool", lambda *a, **k: pytest.fail("no step id: run_tool must not be used"))
    code, out = test_mod.run_command("echo hi; exit 3")
    assert (code, out) == (3, "hi\n")
    assert "hi\n" in capfd.readouterr().out


def _counting_run_tool(real):
    def counted(*a, **k):
        counted.calls += 1
        return real(*a, **k)
    counted.calls = 0
    return counted


def test_test_py_markers_on_runs_the_command_as_a_step(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    code, out = test_mod.run_command("mvn test -T1C -Dtest='A B'", "tests", "Java Tests")
    assert code == 0
    assert out == "fake mvn: test -T1C -Dtest=A B\n"
    shown = capfd.readouterr().out
    assert markers(shown) == [
        {"ev": "step", "id": "tests", "n": 1, "title": "Java Tests"},
        {"ev": "end", "id": "tests", "status": "ok"},
    ]
    assert "fake mvn: test" in shown


def test_test_py_step_numbers_continue_after_n_base(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setenv("JUNEAU_TERMINAL_N_BASE", "3")
    test_mod.run_command("mvn test", "tests", "Java Tests")
    assert markers(capfd.readouterr().out)[0]["n"] == 4


def test_test_py_failure_exit_code_is_returned_and_marked(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setenv("FAKE_MVN_EXIT", "3")
    code, _ = test_mod.run_command("mvn test", "tests", "Java Tests")
    assert code == 3
    assert markers(capfd.readouterr().out)[-1] == {"ev": "end", "id": "tests", "status": "fail", "exit": 3}


def test_test_py_routing_flags_work_without_markers(test_mod, fake_mvn, monkeypatch, capfd, tmp_path):
    log = tmp_path / "full.log"
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "none")
    monkeypatch.setenv("JUNEAU_TERMINAL_FULL_LOG", str(log))
    code, out = test_mod.run_command("mvn test", "tests", "Java Tests")
    assert code == 0 and out == "fake mvn: test\n"
    assert log.read_text() == "fake mvn: test\n"
    assert "fake mvn" not in capfd.readouterr().out


def test_test_py_wrapper_prefix_stays_in_the_command(test_mod, monkeypatch, capfd):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", "echo wrapped")
    _, out = test_mod.run_command("mvn test", "tests", "Java Tests")
    assert out == "wrapped mvn test\n"


def test_test_py_under_an_active_run_emits_steps_but_no_run_or_done(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setenv("JUNEAU_TERMINAL_ACTIVE", "1")
    monkeypatch.setattr(sys, "argv", ["test.py", "--build-only"])
    assert test_mod.main() == 0
    events = markers(capfd.readouterr().out)
    assert [e["ev"] for e in events] == ["step", "end"]
    assert events[0]["id"] == "build"


def test_test_py_standalone_with_markers_owns_run_and_done(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setattr(sys, "argv", ["test.py", "--build-only"])
    assert test_mod.main() == 0
    events = markers(capfd.readouterr().out)
    assert [e["ev"] for e in events] == ["run", "step", "end", "done"]
    assert events[0]["mode"] == "test"
    assert events[-1]["status"] == "ok"


def test_test_py_console_flag_routes_the_output(test_mod, fake_mvn, monkeypatch, capfd):
    # Registered before test.py exports the flag, so teardown removes it again (a delenv afterwards would restore it).
    monkeypatch.delenv("JUNEAU_TERMINAL_CONSOLE", raising=False)
    monkeypatch.setattr(sys, "argv", ["test.py", "--build-only", "--console", "none"])
    assert test_mod.main() == 0
    assert "fake mvn" not in capfd.readouterr().out
    assert os.environ["JUNEAU_TERMINAL_CONSOLE"] == "none"


# ----------------------------------------------------------------------------------------------------------------------
# push.py
# ----------------------------------------------------------------------------------------------------------------------

class Recorder:
    def __init__(self):
        self.calls = []

    def __call__(self, name, result=True):
        def record(*args, **kwargs):
            self.calls.append(name)
            return result
        return record


@pytest.fixture
def gates(push_mod, monkeypatch):
    """Replace every gate and side effect of push.main() with a recorder; returns the recorder."""
    rec = Recorder()
    monkeypatch.setattr(push_mod, "run_command", rec("run_command"))
    monkeypatch.setattr(push_mod, "run_timing_report", rec("timing"))
    # The Prechecks row with no checks: test_push.TestPreflight covers them against real git.
    monkeypatch.setattr(push_mod, "prechecks", lambda *a, **k: rec.calls.append("identity") or [])
    monkeypatch.setattr(push_mod, "commit_and_push", lambda *a, **k: rec.calls.append("commit") or ("ok", 1))
    monkeypatch.setattr(push_mod, "run_docs_followup", lambda *a, **k: rec.calls.append("docs") or ("ok", 1))
    monkeypatch.setattr(push_mod, "_append_test_run_history", rec("history"))
    monkeypatch.setattr(push_mod, "current_branch", lambda root: "main")
    monkeypatch.setenv("JUNEAU_TERMINAL_SCRIPTS_QUIET", "1")    # the session's bell and sound
    monkeypatch.setattr(push_mod, "run_test_script", lambda cmd, cwd, run, **k: rec.calls.append("test.py") or
                        subprocess.CompletedProcess(cmd, 0))
    monkeypatch.setattr(push_mod.subprocess, "run", lambda cmd, *a, **k: subprocess.CompletedProcess(cmd, 0, "", ""))
    return rec


def test_push_test_only_emits_mode_test_and_never_commits(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    events = markers(capsys.readouterr().out)
    project = Path(push_mod.__file__).resolve().parent.parent.name
    assert events[0] == {"ev": "run", "v": 1, "mode": "test", "project": project}
    assert [e["id"] for e in events if e["ev"] == "step"] == ["container-tags", "bom", "install"]
    assert events[-2] == {"ev": "note", "level": "info", "text": "Test-only run: nothing was committed or pushed"}
    assert events[-1] == {"ev": "done", "status": "ok"}
    assert not {"identity", "commit", "docs"} & set(gates.calls)


def test_push_test_only_does_not_need_a_message(push_mod, gates, monkeypatch):
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0


def test_push_without_message_still_requires_one(push_mod, gates, monkeypatch):
    monkeypatch.setattr(sys, "argv", ["push.py"])
    monkeypatch.setattr("builtins.input", lambda _prompt: "")
    with pytest.raises(SystemExit) as exc:
        push_mod.main()
    assert exc.value.code == 1


@pytest.mark.parametrize("flag", ["--docs-only", "--tracker-audit", "--skip-tests"])
def test_push_test_only_rejects_flags_that_commit_or_gate(push_mod, gates, monkeypatch, flag):
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only", flag])
    with pytest.raises(SystemExit) as exc:
        push_mod.main()
    assert exc.value.code == 2


def test_push_markers_off_prints_no_markers_and_reaches_the_commit(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setattr(sys, "argv", ["push.py", "a message"])
    assert push_mod.main() == 0
    assert "##terminal" not in capsys.readouterr().out
    assert "identity" in gates.calls and "commit" in gates.calls


def test_push_full_run_emits_push_mode_and_done_ok(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setattr(sys, "argv", ["push.py", "a message"])
    assert push_mod.main() == 0
    events = markers(capsys.readouterr().out)
    assert events[0]["mode"] == "push"
    assert events[-1] == {"ev": "done", "status": "ok"}


def test_push_failed_gate_ends_the_run_with_done_fail(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setattr(push_mod, "run_command", lambda *a, **k: False)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 1
    events = markers(capsys.readouterr().out)
    assert events[1] == {"ev": "step", "id": "container-tags", "n": 1, "title": "Container test tags"}
    assert events[2]["status"] == "fail"
    assert events[-1] == {"ev": "done", "status": "fail"}


def test_push_run_command_with_a_tool_emits_the_step_and_keeps_the_result_lines(push_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    ok = push_mod.run_command(["mvn", "install"], "Build", tool="maven", step_id="install", title="Build and install", n=7)
    shown = capfd.readouterr().out
    assert ok is True
    assert markers(shown)[0] == {"ev": "step", "id": "install", "n": 7, "title": "Build and install"}
    assert "✅ Build - SUCCESS" in shown


def test_push_run_command_markers_off_keeps_the_exact_original_call(push_mod, fake_mvn, monkeypatch, capfd, tmp_path):
    run = push_mod._juneau.load()[0]
    monkeypatch.setattr(run, "run_tool", lambda *a, **k: pytest.fail("run_tool must not be used with markers off"))
    seen = []
    real_run = subprocess.run
    monkeypatch.setattr(push_mod.subprocess, "run", lambda *a, **k: seen.append((a, k)) or real_run(*a, **k))
    ok = push_mod.run_command(["mvn", "install"], "Build", tmp_path, tool="maven", step_id="install", title="Build", n=1)
    assert ok is True
    assert "##terminal" not in capfd.readouterr().out
    assert seen == [((["mvn", "install"],), {"cwd": tmp_path, "shell": False, "check": True, "capture_output": False,
                                             "text": True})]


def test_push_run_command_failure_returns_false(push_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("FAKE_MVN_EXIT", "2")
    assert push_mod.run_command(["mvn", "install"], "Build", tool="maven", step_id="install") is False
    assert "❌ Build - FAILED (exit code: 2)" in capfd.readouterr().out


def test_push_reserves_step_numbers_for_the_steps_test_py_contributes(push_mod, gates, monkeypatch):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    env = _capture_child_env(push_mod, monkeypatch, gates)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert env["JUNEAU_TERMINAL_N_BASE"] == "2"    # after Tags and BOM


def test_push_test_only_fallback_runs_mvn_test_as_a_maven_step(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setattr(push_mod.Path, "exists", lambda self: self.name != "test.py" and Path.is_file(self))
    calls = []
    monkeypatch.setattr(push_mod, "run_command", lambda cmd, *a, **k: calls.append((cmd, k)) or True)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    tests = [k for cmd, k in calls if cmd == ["mvn", "test"]]
    assert len(tests) == 1
    assert tests[0]["tool"] == "maven" and tests[0]["step_id"] == "tests"


def test_run_test_script_markers_off_is_the_plain_subprocess_run(push_mod, monkeypatch, tmp_path):
    seen = []
    monkeypatch.setattr(push_mod.subprocess, "run", lambda *a, **k: seen.append((a, k)) or
                        subprocess.CompletedProcess(a[0], 0))
    run = push_mod._juneau.load()[0]
    assert push_mod.run_test_script(["x"], tmp_path, run, env={"A": "1"}).returncode == 0
    assert seen == [((["x"],), {"cwd": tmp_path, "check": False, "env": {"A": "1"}})]


@pytest.mark.parametrize("markers_on", [True, False])
def test_run_test_script_keeps_returncode_for_its_call_site(push_mod, monkeypatch, tmp_path, markers_on):
    if markers_on:
        monkeypatch.setenv("TERMINAL_MARKERS", "1")
    proc = push_mod.toolkit("proc")
    seen = []
    monkeypatch.setattr(proc, "run_group", lambda cmd, **k: seen.append(k) or proc.Result(False, 3))
    monkeypatch.setattr(push_mod.subprocess, "run", lambda cmd, **k: subprocess.CompletedProcess(cmd, 3))
    result = push_mod.run_test_script(["x"], tmp_path, push_mod._juneau.load()[0], env={"A": "1"})
    assert result.returncode == 3
    assert seen == ([{"cwd": tmp_path, "env": {"A": "1"}}] if markers_on else [])


def test_run_test_script_markers_on_gives_the_child_its_own_session(push_mod, monkeypatch, tmp_path):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    script = tmp_path / "child.py"
    script.write_text("import os\nraise SystemExit(0 if os.getsid(0) == os.getpid() else 7)\n", encoding="utf-8")
    run = push_mod._juneau.load()[0]
    assert push_mod.run_test_script([sys.executable, str(script)], tmp_path, run).returncode == 0


def test_test_py_wrapper_with_and_and_markers_on_runs_through_a_shell(test_mod, monkeypatch):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", "echo first &&")
    code, out = test_mod.run_command("mvn --version", "tests", "Java Tests")
    assert out.startswith("first\n")  # the && part ran in the same shell as the rest of the command


def _wait_for(path, seconds=30):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if path.exists():
            return True
        time.sleep(0.05)
    return False


def _alive(pid):
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    return True


@pytest.mark.parametrize("script", ["push.py", "test.py"])
def test_sigterm_ends_the_run_as_cancelled_and_leaves_no_orphan(script, tmp_path):
    """SIGTERM a real run under a fake mvn that sleeps: expect done(cancelled), exit 130 and no surviving mvn."""
    bin_dir = tmp_path / "bin"
    bin_dir.mkdir()
    pidfile = tmp_path / "mvn.pid"
    ready = tmp_path / "mvn.ready"
    mvn = bin_dir / "mvn"
    mvn.write_text(f"#!/bin/sh\necho $$ > {pidfile}\necho '[INFO] Scanning for projects...'\ntouch {ready}\n"
                   "exec sleep 300\n", encoding="utf-8")
    mvn.chmod(mvn.stat().st_mode | stat.S_IEXEC)
    env = {**os.environ, "TERMINAL_MARKERS": "1", "PATH": f"{bin_dir}{os.pathsep}{os.environ['PATH']}"}
    for variable in ("JUNEAU_TERMINAL_ACTIVE", "JUNEAU_TERMINAL_N_BASE", "JUNEAU_MVN_WRAPPER"):
        env.pop(variable, None)
    # A scratch tree holding only copies of the two scripts (and the real juneau-terminal), so push.py's other gates
    # (container tags, BOM) have nothing to run and nothing real is built.
    root = tmp_path / "tree"
    (root / "scripts").mkdir(parents=True)
    for name in ("push.py", "test.py", "_juneau.py"):
        shutil.copy(SCRIPTS_DIR / name, root / "scripts" / name)
    (root / "juneau-terminal").symlink_to(SCRIPTS_DIR.parent / "juneau-terminal")
    args = [sys.executable, "-B", str(root / "scripts" / script)] + (["--test-only", "--no-js-tests"] if script == "push.py"
                                                                      else ["--build-only"])
    proc = subprocess.Popen(args, cwd=root, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, start_new_session=True)
    try:
        assert _wait_for(ready), "the fake mvn never started"
        mvn_pid = int(pidfile.read_text().strip())
        proc.send_signal(signal.SIGTERM)
        output, _ = proc.communicate(timeout=60)
    finally:
        if proc.poll() is None:
            os.killpg(proc.pid, signal.SIGKILL)
    assert proc.returncode == 130
    events = markers(output)
    assert events[-1] == {"ev": "done", "status": "cancelled"}
    assert sum(e["ev"] == "done" for e in events) == 1
    deadline = time.monotonic() + 10
    while _alive(mvn_pid) and time.monotonic() < deadline:
        time.sleep(0.05)
    assert not _alive(mvn_pid), "the fake mvn survived the cancellation"


class Events:
    """Bus subscriber that keeps (kind, fields) pairs."""

    def __init__(self):
        self.events = []

    def __call__(self, kind, fields):
        self.events.append((kind, fields))

    def of(self, kind):
        return [f for k, f in self.events if k == kind]


@pytest.fixture
def console(push_mod, monkeypatch):
    """A board session; stdout is captured, so it draws in plain mode.  Returns (juneau_terminal, Events)."""
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "board")
    run = push_mod._juneau.load()[0]
    run.session("Push", ["Juneau", "test"])
    assert run.console_active()
    events = run.subscribe(Events())
    return run, events


@pytest.fixture
def standalone(push_mod, monkeypatch):
    """A board console that push.main() opens itself, as a push run from the shell does: no session is open
    first, so the push session is not nested (its own run marker, summary and bell).  Yields (juneau_terminal, Events,
    nested), nested being each _nested_env() value the session saw; teardown checks push opened exactly one, un-nested."""
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "board")
    run = push_mod._juneau.load()[0]
    toolkit_run = push_mod.toolkit("run")
    real, nested = toolkit_run._nested_env, []
    monkeypatch.setattr(toolkit_run, "_nested_env", lambda: nested.append(real()) or nested[-1])
    events = run.subscribe(Events())
    yield run, events, nested
    assert nested == [False], "push.main() did not open one standalone session"


INSTALL = ["mvn", "clean", "package", "install", "-DskipTests"]


def recorded_commands(push_mod, monkeypatch):
    commands = []
    monkeypatch.setattr(push_mod, "run_command", lambda cmd, *a, **k: commands.append(cmd) or True)
    return commands


def test_run_command_goes_through_run_tool_when_a_console_is_active(push_mod, console, monkeypatch):
    run, _ = console
    calls = []
    monkeypatch.setattr(run, "run_tool", lambda cmd, tool, step_id, title, **k:
                        calls.append((cmd, tool, step_id, title, k["label"])) or run.Result(0, 1))
    assert push_mod.run_command(["git", "push"], "Pushing", step_id="push", title="Push", label="Push")
    assert calls == [(["git", "push"], "generic", "push", "Push", "Push")]


def test_run_command_without_a_console_or_markers_runs_as_before(push_mod, monkeypatch, capsys):
    ran = []
    monkeypatch.setattr(push_mod.subprocess, "run", lambda cmd, **k: ran.append(cmd))
    assert push_mod.run_command(["git", "push"], "Pushing", step_id="push", title="Push", label="Push")
    assert ran == [["git", "push"]]
    assert "Running: git push" in capsys.readouterr().out


def test_commit_summary_reads_hash_subject_and_file_count(push_mod):
    text = ("[master 2e35151837] Add the console bus topic\r\n"
            " 227 files changed, 9 insertions(+)\r\n create mode 100644 a.txt\r\n")
    assert push_mod.commit_summary(text) == '2e35151837  "Add the console bus topic"  (227 files)'
    assert push_mod.commit_summary("[detached HEAD abc1234] x\n 1 file changed\n") == 'abc1234  "x"  (1 file)'
    assert push_mod.commit_summary("nothing recognisable") is None


def test_commit_summary_cuts_a_long_subject(push_mod):
    summary = push_mod.commit_summary(f"[master abc1234] {'s' * 80}\n")
    assert summary == f'abc1234  "{"s" * 59}…"'


def test_push_skips_the_install_after_an_installing_test_py(push_mod, gates, monkeypatch):
    commands = recorded_commands(push_mod, monkeypatch)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert INSTALL not in commands


def test_push_installs_with_skip_tests(push_mod, gates, monkeypatch):
    commands = recorded_commands(push_mod, monkeypatch)
    monkeypatch.setattr(sys, "argv", ["push.py", "Quick fix", "--skip-tests"])
    assert push_mod.main() == 0
    assert INSTALL in commands


def test_push_test_only_marks_the_install_step_skipped(push_mod, gates, monkeypatch, capsys):
    """With markers on (the release manager), the skipped install is still the `install` step, ending with skip."""
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    commands = recorded_commands(push_mod, monkeypatch)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    install = [e for e in markers(capsys.readouterr().out) if e.get("id") == "install"]
    assert [(e["ev"], e.get("status")) for e in install] == [("step", None), ("end", "skip")]
    assert INSTALL not in commands


def test_a_test_only_session_is_a_test_of_the_branch_with_no_message(push_mod, gates, standalone, monkeypatch):
    _, events, _ = standalone
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    [session] = events.of("session")
    assert (session["title"], session["header"]) == ("Test", ["Juneau", "main"])


def test_the_skipped_install_shows_done_by_tests(push_mod, gates, standalone, monkeypatch):
    _, events, _ = standalone
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    [install] = [f for f in events.of("step_end") if f["id"] == "install"]
    assert (install["status"], install["summary"]) == ("skip", "done by Java Tests")


class FakeTimings:
    written = []
    asked = []

    @staticmethod
    def history_off():
        return False

    @staticmethod
    def load_history(repo=None, path=None):
        FakeTimings.asked.append((repo, path))
        return [{"stub": True}]

    @staticmethod
    def report_path(repo=None, path=None):
        return Path(repo) / "test-timings.txt"

    @staticmethod
    def latest_run(records):
        return records, None

    @staticmethod
    def analyse(records):
        return [{"flag": "slower", "line": "juneau-core  surefire  +31%  +9.0s"},
                {"flag": "faster", "line": "juneau-rest  surefire  -25%  -6.0s"},
                {"flag": None, "line": "juneau-bean  surefire  +1%  +0.1s"}]

    @staticmethod
    def write_report(entries, path):
        FakeTimings.written.append((len(entries), path))

    @staticmethod
    def counts(entries):
        return "1 slower than 20% and 5s · 1 faster"


def test_the_timing_row_counts_slower_entries_and_names_the_report(push_mod, console, monkeypatch, tmp_path):
    _, events = console
    monkeypatch.setattr(push_mod, "load_timings_module", lambda script_dir: FakeTimings)
    FakeTimings.written.clear()
    FakeTimings.asked.clear()
    push_mod.run_timing_report(tmp_path, tmp_path)
    report = tmp_path / "test-timings.txt"
    assert FakeTimings.asked == [(tmp_path, None)]
    assert FakeTimings.written == [(3, report)]
    assert [(f["level"], f["text"]) for f in events.of("note")] == [
        ("warn", "juneau-core  surefire  +31%  +9.0s"), ("info", "juneau-rest  surefire  -25%  -6.0s")]
    [timing] = [f for f in events.of("step_end") if f["id"] == "timing"]
    assert (timing["status"], timing["summary"]) == ("ok", f"1 slower than 20% and 5s · 1 faster → {report}")


def test_push_exports_detail_to_test_py(push_mod, gates, monkeypatch):
    seen = []
    monkeypatch.setattr(push_mod, "run_test_script", lambda cmd, cwd, run, env=None:
                        seen.append(env.get("JUNEAU_TERMINAL_DETAIL")) or subprocess.CompletedProcess(cmd, 0))
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only", "--detail", "modules"])
    assert push_mod.main() == 0
    assert seen == ["modules"]


def test_push_console_flag_sets_the_env_var_and_beats_it(push_mod, gates, monkeypatch):
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "board")
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only", "--console", "none"])
    assert push_mod.main() == 0
    assert os.environ["JUNEAU_TERMINAL_CONSOLE"] == "none"


def test_push_rejects_an_unknown_console_mode(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only", "--console", "loud"])
    with pytest.raises(SystemExit) as exc:
        push_mod.main()
    assert exc.value.code == 2
    assert "board" in capsys.readouterr().err


def test_push_rejects_an_unknown_detail_level(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only", "--detail", "loud"])
    with pytest.raises(SystemExit) as exc:
        push_mod.main()
    assert exc.value.code == 2
    assert "actionable" in capsys.readouterr().err


def test_push_identity_is_a_row_not_a_header_mark(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "board")
    events = push_mod._juneau.load()[0].subscribe(Events())
    monkeypatch.setattr(sys, "argv", ["push.py", "Quick fix", "--skip-tests"])
    assert push_mod.main() == 0
    [session] = events.of("session")
    assert session["title"] == "Push"
    assert session["header"] == ["Juneau", "main"]
    assert session["lines"] == ["Commit Message: Quick fix"]
    assert not [line for line in session["header"] if "✓" in line]
    assert [f["id"] for f in events.of("step_start")][:1] == ["prechecks"]
    out = capsys.readouterr().out
    assert "=" * 70 not in out
    assert "Juneau Build and Push Script" not in out


def test_push_hands_the_console_to_test_py(push_mod, gates, standalone, monkeypatch):
    run, _, _ = standalone
    calls = []
    monkeypatch.setattr(run.Console, "pause", lambda self: calls.append("pause"))
    monkeypatch.setattr(run.Console, "resume", lambda self: calls.append("resume"))
    monkeypatch.setattr(push_mod, "run_test_script", lambda cmd, cwd, r, env=None: calls.append(
        ("test.py", env.get("JUNEAU_TERMINAL_LABEL_WIDTH"))) or subprocess.CompletedProcess(cmd, 0))
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    # the final line may pause again
    assert calls[:3] == ["pause", ("test.py", "13"), "resume"]


def test_push_ctrl_c_with_a_console_is_a_clean_cancel(push_mod, gates, standalone, monkeypatch):
    _, events, _ = standalone

    def interrupted(cmd, cwd, run, **k):
        raise KeyboardInterrupt

    monkeypatch.setattr(push_mod, "run_test_script", interrupted)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 130
    assert events.of("done")[-1]["status"] == "cancelled"


# ----------------------------------------------------------------------------------------------------------------------
# push.py under a console view
# ----------------------------------------------------------------------------------------------------------------------

def say_events(events):
    return [(f["level"], f["text"]) for f in events.of("say")]


def stub_push_flow(push_mod, monkeypatch, *, upstream="origin/master"):
    """commit_and_push's collaborators, stubbed so it reaches the commit and the push; returns the notify calls."""
    git = push_mod.toolkit("git")
    monkeypatch.setattr(git, "upstream_verdict", lambda repo, **k: git.UpstreamVerdict("ok", 1, 0))
    monkeypatch.setattr(git, "push_command", lambda repo: ["git", "push"])
    monkeypatch.setattr(push_mod, "get_staged_paths", lambda repo: ["a.txt"])
    monkeypatch.setattr(push_mod, "check_unreviewed_changes", lambda repo: [])
    monkeypatch.setattr(push_mod, "upstream_name", lambda repo: upstream)
    notify, sounds = push_mod.toolkit("notify"), []
    monkeypatch.setattr(notify, "bell", lambda **k: sounds.append("bell"))
    monkeypatch.setattr(notify, "sound", lambda ok, **k: sounds.append(ok))
    return sounds


def test_the_opening_banner_keeps_its_closing_rule(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    lines = capsys.readouterr().out.splitlines()
    assert lines[lines.index("🧪 TEST-ONLY MODE (--test-only) - build and test gates only; nothing is committed or "
                             "pushed") + 1] == "=" * 70


def test_every_line_of_the_unreviewed_paths_refusal_is_an_error(push_mod, monkeypatch):
    stub_push_flow(push_mod, monkeypatch)
    monkeypatch.setattr(push_mod, "check_unreviewed_changes", lambda repo: ["a b.txt", "c.txt"])
    said = []
    monkeypatch.setattr(push_mod, "say", lambda text="", level="info": said.append((level, text)))
    assert push_mod.commit_and_push(Path("."), "msg", 5)[0] == "error"
    start = next(i for i, (_, text) in enumerate(said) if "unreviewed changes" in text)
    assert len(said) - start >= 9
    assert {level for level, _ in said[start:]} == {"error"}


def test_the_test_history_line_goes_through_say(push_mod, monkeypatch, tmp_path, capsys):
    said = []
    monkeypatch.setattr(push_mod, "say", lambda text="", level="info": said.append((level, text)))
    monkeypatch.setattr(push_mod, "current_branch", lambda root: "main")
    (tmp_path / "juneau-integration-tests").mkdir()
    push_mod._append_test_run_history(tmp_path, 12)
    assert [(level, "Test metrics appended" in text) for level, text in said] == [("info", True)]
    assert capsys.readouterr().out == ""


def test_a_test_history_failure_is_a_warning_through_say(push_mod, monkeypatch, tmp_path, capsys):
    said = []
    monkeypatch.setattr(push_mod, "say", lambda text="", level="info": said.append((level, text)))
    monkeypatch.setattr(push_mod, "current_branch", lambda root: "main")
    (tmp_path / "juneau-integration-tests").write_text("not a directory", encoding="utf-8")
    push_mod._append_test_run_history(tmp_path, 12)
    assert [(level, "Could not append test metrics" in text) for level, text in said] == [("warn", True)]
    assert capsys.readouterr().out == ""


@pytest.fixture
def docs_tree(push_mod, monkeypatch, tmp_path):
    """A scratch juneau/juneau-docs pair that push.py believes it lives in."""
    (tmp_path / "juneau" / "scripts").mkdir(parents=True)
    (tmp_path / "juneau-docs").mkdir()
    monkeypatch.setattr(push_mod, "__file__", str(tmp_path / "juneau" / "scripts" / "push.py"))
    # the scratch juneau-docs isn't a repo: Prechecks has nothing to check
    monkeypatch.setattr(push_mod, "prechecks", lambda *a, **k: [])
    monkeypatch.setattr(push_mod, "docs_prechecks", lambda *a, **k: [])
    monkeypatch.setattr(push_mod, "current_branch", lambda root: "main")
    monkeypatch.setenv("JUNEAU_TERMINAL_SCRIPTS_QUIET", "1")
    return tmp_path


def test_docs_only_under_a_console_has_one_session_and_one_done(push_mod, docs_tree, monkeypatch):
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "board")
    events = push_mod._juneau.load()[0].subscribe(Events())
    monkeypatch.setattr(push_mod, "commit_and_push", lambda *a, **k: ("ok", 6))
    monkeypatch.setattr(sys, "argv", ["push.py", "--docs-only", "Docs fix"])
    assert push_mod.main() == 0
    [session] = events.of("session")
    assert session["title"] == "Push"
    assert session["header"] == ["Juneau Docs", "main"]
    assert session["lines"] == ["Commit Message: Docs fix"]
    assert [f["status"] for f in events.of("done")] == ["ok"]
    assert [p["ids"] for p in events.of("plan")] == [["prechecks", "docs-smoke", "commit", "push"]]


def test_docs_only_with_markers_on_emits_run_and_done_markers(push_mod, docs_tree, monkeypatch, capsys):
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "board")
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    monkeypatch.setattr(push_mod, "commit_and_push", lambda *a, **k: ("ok", 6))
    monkeypatch.setattr(sys, "argv", ["push.py", "--docs-only", "Docs fix"])
    assert push_mod.main() == 0
    events = markers(capsys.readouterr().out)
    assert events[0]["ev"] == "run" and events[0]["mode"] == "push" and events[0]["project"] == "juneau-docs"
    assert events[-1] == {"ev": "done", "status": "ok"}


@pytest.mark.parametrize("flags", [[], ["--docs-only"]])
def test_a_dry_run_under_a_console_still_says_so(push_mod, docs_tree, monkeypatch, capsys, flags):
    monkeypatch.setenv("JUNEAU_TERMINAL_CONSOLE", "board")
    monkeypatch.setattr(sys, "argv", ["push.py", "--dry-run", "a message", *flags])
    assert push_mod.main() == 0
    assert "DRY RUN MODE" in capsys.readouterr().out


class NoHistoryTimings(FakeTimings):
    @staticmethod
    def load_history(repo=None, path=None):
        return []

    @staticmethod
    def latest_run(records):
        return [], "no timing history yet"


def test_the_timing_row_on_a_first_run_says_there_is_no_history(push_mod, console, monkeypatch, tmp_path):
    _, events = console
    monkeypatch.setattr(push_mod, "load_timings_module", lambda script_dir: NoHistoryTimings)
    NoHistoryTimings.written.clear()
    push_mod.run_timing_report(tmp_path, tmp_path)
    assert NoHistoryTimings.written == []
    assert events.of("note") == []
    [timing] = [f for f in events.of("step_end") if f["id"] == "timing"]
    assert (timing["status"], timing["summary"]) == ("ok", "no timing history yet")


def test_the_timing_row_with_history_still_writes_the_report(push_mod, console, monkeypatch, tmp_path):
    monkeypatch.setattr(push_mod, "load_timings_module", lambda script_dir: FakeTimings)
    FakeTimings.written.clear()
    push_mod.run_timing_report(tmp_path, tmp_path)
    assert len(FakeTimings.written) == 1


def routed_calls(run, monkeypatch, exit=0):
    calls = []

    def run_tool(cmd, tool, step_id, title, **k):
        calls.append({"cmd": cmd, "step_id": step_id, **k})
        return run.Result(exit, 1)

    monkeypatch.setattr(run, "run_tool", run_tool)
    return calls


def test_the_commit_and_the_push_run_with_a_tty(push_mod, console, monkeypatch):
    run, _ = console
    stub_push_flow(push_mod, monkeypatch)
    calls = routed_calls(run, monkeypatch)
    assert push_mod.commit_and_push(Path("."), "msg", 5) == ("ok", 7)
    assert [(c["step_id"], c["tty"]) for c in calls] == [("commit", True), ("push", True)]


def test_the_commit_and_the_push_run_with_the_watchdog(push_mod, console, monkeypatch):
    """A prompt git raises (ssh passphrase, pinentry) is a partial line; the watchdog puts it on the board."""
    run, _ = console
    stub_push_flow(push_mod, monkeypatch)
    calls = routed_calls(run, monkeypatch)
    push_mod.commit_and_push(Path("."), "msg", 5)
    assert [(c["step_id"], c["watchdog"]) for c in calls] == [
        ("commit", push_mod.WATCHDOG_SECONDS), ("push", push_mod.WATCHDOG_SECONDS)]


def test_a_git_passphrase_prompt_reaches_the_board(push_mod, console, monkeypatch, tmp_path):
    """End to end through the real run_tool: a git that prints a prompt with no newline and waits."""
    run, events = console
    stub_push_flow(push_mod, monkeypatch)
    monkeypatch.setattr(push_mod, "WATCHDOG_SECONDS", 0.3)
    fake = tmp_path / "fakegit.py"
    fake.write_text("import sys, time\n"
                    "sys.stdout.write(\"Enter passphrase for key '/x/id_ed25519': \")\n"
                    "sys.stdout.flush()\ntime.sleep(1.5)\nprint()\n")
    prompts = []
    real = run.publish
    monkeypatch.setattr(run, "publish", lambda kind, **k: prompts.append((kind, k.get("text"))) or real(kind, **k))
    push_mod.run_command([sys.executable, str(fake)], "Pushing", step_id="push", title="Push", tty=True,
                         watchdog=push_mod.WATCHDOG_SECONDS)
    assert ("prompt", "Enter passphrase for key '/x/id_ed25519':") in [(k, t and t.strip()) for k, t in prompts]


def test_the_push_summary_names_the_upstream(push_mod, console, monkeypatch):
    run, _ = console
    stub_push_flow(push_mod, monkeypatch)
    calls = routed_calls(run, monkeypatch)
    push_mod.commit_and_push(Path("."), "msg", 5)
    push = next(c for c in calls if c["step_id"] == "push")
    assert push["summarize"]("anything") == "origin/master ✓"


def test_a_failing_routed_command_returns_false_under_a_console(push_mod, console, monkeypatch):
    run, events = console
    routed_calls(run, monkeypatch, exit=2)
    assert push_mod.run_command(["git", "push"], "Pushing", step_id="push", title="Push") is False
    assert ("error", "❌ Pushing - FAILED (exit code: 2)") in say_events(events)


def test_a_failing_push_stops_commit_and_push_under_a_console(push_mod, console, monkeypatch):
    run, _ = console
    sounds = stub_push_flow(push_mod, monkeypatch)
    monkeypatch.setattr(run, "run_tool", lambda cmd, tool, step_id, title, **k:
                        run.Result(0 if step_id == "commit" else 1, 1))
    assert push_mod.commit_and_push(Path("."), "msg", 5) == ("error", 6)
    assert sounds == []    # the session sounds the run's end


def test_the_docs_followup_keeps_its_rows_apart_from_juneaus(push_mod, console, monkeypatch, tmp_path):
    run, _ = console
    (tmp_path / "juneau-docs").mkdir()
    stub_push_flow(push_mod, monkeypatch)
    calls = routed_calls(run, monkeypatch)
    assert push_mod.run_docs_followup(tmp_path / "juneau", "msg", 5, docs_prechecked=True) == ("ok", 7)
    assert [(c["step_id"], c["label"]) for c in calls] == [
        ("docs-smoke", "Docs build"), ("docs-commit", "Docs"), ("docs-push", "Docs push")]


def test_main_uses_the_timing_row_under_a_console_and_the_cli_otherwise(push_mod, gates, standalone, monkeypatch):
    commands = recorded_commands(push_mod, monkeypatch)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert "timing" in gates.calls
    assert not any("push-timings.py" in " ".join(map(str, cmd)) for cmd in commands)


def test_main_runs_the_timing_cli_without_a_console(push_mod, gates, monkeypatch):
    commands = recorded_commands(push_mod, monkeypatch)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert "timing" not in gates.calls
    [timing] = [cmd for cmd in commands if "push-timings.py" in " ".join(map(str, cmd))]
    assert timing[2:] == ["--repo", str(Path(push_mod.__file__).resolve().parent.parent)]


def test_the_upstream_is_only_looked_up_under_a_console(push_mod, monkeypatch):
    stub_push_flow(push_mod, monkeypatch)
    monkeypatch.setattr(push_mod, "upstream_name", lambda repo: pytest.fail("no console, no upstream lookup"))
    monkeypatch.setattr(push_mod, "run_command", lambda *a, **k: True)
    assert push_mod.commit_and_push(Path("."), "msg", 5) == ("ok", 7)


def test_the_tags_and_bom_rows_summarise_their_checks(push_mod):
    assert push_mod.tags_summary("✅ Container tag guard passed (39 container tests tagged).\n") == \
        "39 container tests tagged"
    assert push_mod.bom_summary("OK: juneau-bom enumerates all 73 consumer-facing modules.\n") == \
        "73 modules in juneau-bom"
    assert push_mod.tags_summary("something else") is None
    assert push_mod.bom_summary(None) is None


def test_run_marked_passes_the_summarizer_under_a_console(push_mod, monkeypatch):
    seen = []
    monkeypatch.setitem(sys.modules, "juneau_terminal", SimpleNamespace(enabled=lambda: False, console_active=lambda: True))
    monkeypatch.setattr(push_mod, "run_command", lambda *a, **k: seen.append(k) or True)
    assert push_mod.run_marked("bom", "BOM", ["x"], "Checking", label="BOM", summarize=push_mod.bom_summary)
    assert seen[0]["summarize"] is push_mod.bom_summary


def test_run_marked_passes_the_label_to_its_fallback(push_mod, monkeypatch):
    seen = []
    monkeypatch.setattr(push_mod, "run_command", lambda *a, **k: seen.append(k) or True)
    assert push_mod.run_marked("bom", "BOM", ["x"], "Checking", label="BOM")
    assert seen == [{"label": "BOM"}]


def test_the_skipped_install_is_not_repeated_as_a_note_under_a_console(push_mod, gates, standalone, monkeypatch):
    _, events, _ = standalone
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert not [text for _, text in say_events(events) if "already done by test.py" in text]


def test_a_gate_that_could_not_run_points_at_the_full_log_under_a_console(push_mod, gates, standalone, monkeypatch):
    _, events, _ = standalone
    monkeypatch.setattr(push_mod, "run_tracker_audit_gate", lambda root, n: "error")
    monkeypatch.setattr(sys, "argv", ["push.py", "msg", "--tracker-audit"])
    assert push_mod.main() == 1
    # the failed row says what stopped; the fix goes out as a hint
    texts = [text for _, text in say_events(events)] + [line for f in events.of("hint") for line in f["lines"]]
    assert any("full log" in text for text in texts)
    assert not any("reported above" in text for text in texts)
    assert not any("Push aborted" in text for text in texts)


def test_a_gate_that_could_not_run_still_points_above_without_a_console(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setattr(push_mod, "run_tracker_audit_gate", lambda root, n: "error")
    monkeypatch.setattr(sys, "argv", ["push.py", "msg", "--tracker-audit"])
    assert push_mod.main() == 1
    assert "Fix the issue reported above" in capsys.readouterr().out


# ----------------------------------------------------------------------------------------------------------------------
# Characterization: push.py's handoff to test.py and its plain-mode Ctrl-C
# ----------------------------------------------------------------------------------------------------------------------

def _capture_child_env(push_mod, monkeypatch, gates):
    seen = {}

    def runner(cmd, cwd, run, *rest, **kw):
        seen.update(kw.get("env") or os.environ)
        gates.calls.append("test.py")
        return subprocess.CompletedProcess(cmd, 0)
    monkeypatch.setattr(push_mod, "run_test_script", runner)
    return seen


def test_push_child_env_carries_push_active_and_n_base_under_markers(push_mod, gates, monkeypatch):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    env = _capture_child_env(push_mod, monkeypatch, gates)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert env["JUNEAU_PUSH_ACTIVE"] == "1"
    assert env["JUNEAU_TERMINAL_N_BASE"] == "2"    # after Tags and BOM


def test_push_child_env_has_push_active_but_no_n_base_without_markers(push_mod, gates, monkeypatch):
    env = _capture_child_env(push_mod, monkeypatch, gates)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert env["JUNEAU_PUSH_ACTIVE"] == "1"
    assert "JUNEAU_TERMINAL_N_BASE" not in env


@pytest.mark.parametrize("markers_on", [True, False])
def test_push_does_not_mutate_os_environ_for_the_child(push_mod, gates, monkeypatch, markers_on):
    if markers_on:
        monkeypatch.setenv("TERMINAL_MARKERS", "1")
    env = _capture_child_env(push_mod, monkeypatch, gates)
    later = []
    monkeypatch.setattr(push_mod, "_append_test_run_history", lambda *a: later.append(
        (os.environ.get("JUNEAU_PUSH_ACTIVE"), os.environ.get("JUNEAU_TERMINAL_N_BASE"))))
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert env["JUNEAU_PUSH_ACTIVE"] == "1"
    assert later == [(None, None)]
    assert "JUNEAU_PUSH_ACTIVE" not in os.environ and "JUNEAU_TERMINAL_N_BASE" not in os.environ


def test_push_reserves_exactly_four_numbers_for_test_py(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    env = _capture_child_env(push_mod, monkeypatch, gates)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    n_base = int(env["JUNEAU_TERMINAL_N_BASE"])
    steps = [e for e in markers(capsys.readouterr().out) if e["ev"] == "step" and "parent" not in e]
    ids = [e["id"] for e in steps]
    after = steps[ids.index("bom") + 1]
    assert after["id"] == "install"
    assert after["n"] == n_base + 5
    assert max(e["n"] for e in steps[:ids.index("bom") + 1]) == n_base


def test_push_plain_ctrl_c_before_the_session_exits_130(push_mod, monkeypatch, capsys):
    def interrupted():
        raise KeyboardInterrupt
    monkeypatch.setenv("JUNEAU_TERMINAL_SCRIPTS_QUIET", "1")
    monkeypatch.setattr(push_mod, "_main", interrupted)
    assert push_mod.main() == 130
    assert "❌ Cancelled." in capsys.readouterr().out


def test_push_plain_ctrl_c_in_the_session_exits_130(push_mod, gates, monkeypatch, capsys):
    def interrupted(cmd, cwd, run, **k):
        raise KeyboardInterrupt
    monkeypatch.setattr(push_mod, "run_test_script", interrupted)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 130
    out = capsys.readouterr().out
    assert "❌ Cancelled." in out and "Traceback" not in out


@pytest.mark.parametrize("argv, expected", [
    (["--test-only"], ["container-tags", "bom", "build", "tests", "perf", "py-tests", "js-tests", "traces", "timing", "install"]),
    (["Quick fix", "--skip-tests", "--tracker-audit"],
     ["prechecks", "tracker", "tests", "install", "commit", "push"]),
])
def test_push_session_plan_lists_the_steps_this_run_will_take(push_mod, gates, standalone, monkeypatch, argv, expected):
    _, events, _ = standalone
    monkeypatch.setattr(push_mod, "maybe_run_tracker_audit_gate", lambda args, root, n: "pass")
    monkeypatch.setattr(sys, "argv", ["push.py", *argv])
    assert push_mod.main() == 0
    root = Path(push_mod.__file__).resolve().parent.parent
    docs = ["docs-smoke", "docs-commit", "docs-push"] if (root.parent / "juneau-docs").exists() else []
    [plan] = events.of("plan")
    assert plan["ids"] == expected + (docs if "commit" in expected else [])
    assert all(plan["labels"].get(step_id) for step_id in plan["ids"])    # every planned row has a label


def test_push_session_rings_the_bell_and_plays_the_sound_once_when_it_ends(push_mod, gates, monkeypatch):
    notify = push_mod.toolkit("notify")
    calls = []
    monkeypatch.setattr(notify, "bell", lambda **k: calls.append(("bell", k)))
    monkeypatch.setattr(notify, "sound", lambda ok, **k: calls.append(("sound", ok, k)))
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    silence = {"silence_env": (push_mod.PUSH_NO_SOUND_ENV,)}
    assert calls == [("bell", silence), ("sound", True, silence)]


def test_push_plan_puts_the_opt_in_gates_first_and_forced_browser_tests_after_tests(push_mod, tmp_path):
    script_dir = tmp_path / "juneau" / "scripts"
    script_dir.mkdir(parents=True)
    for name in ("test.py", "check-bom-completeness.py", "push-timings.py"):
        (script_dir / name).touch()
    (tmp_path / "juneau-docs").mkdir()
    args = argparse.Namespace(tracker_audit=True, skip_tests=False, js_tests=True, test_only=False)
    assert [step_id for step_id, _ in push_mod.push_plan(args, script_dir, script_dir.parent, False)] == [
        "prechecks", "tracker", "bom", "build", "tests", "perf", "py-tests", "js-tests", "traces", "install", "commit", "push",
        "docs-smoke", "docs-commit", "docs-push"]  


# The one loader push.py and test.py share (scripts/_juneau.py).

@pytest.fixture
def juneau_loader():
    """scripts/_juneau.py, freshly loaded.  bootstrap leaves sys.dont_write_bytecode set; restore it afterwards."""
    saved = sys.dont_write_bytecode
    try:
        yield load("_juneau_under_test", "_juneau.py")
    finally:
        sys.dont_write_bytecode = saved


def test_the_loader_imports_the_in_tree_toolkit_and_juneau_terminal(juneau_loader):
    jr, js = juneau_loader.load()
    python_src = (SCRIPTS_DIR.parent / "juneau-terminal" / "src" / "main" / "python").resolve()
    assert Path(js.__file__).resolve() == python_src / "juneau_terminal_scripts" / "__init__.py"
    assert Path(jr.__file__).resolve() == python_src / "juneau_terminal.py"
    assert sys.modules["juneau_terminal"] is jr
    assert juneau_loader.load() == (jr, js)


def test_the_loader_reuses_an_already_loaded_juneau_terminal(juneau_loader, monkeypatch):
    sentinel = SimpleNamespace(__file__="elsewhere/juneau_terminal.py")
    monkeypatch.setitem(sys.modules, "juneau_terminal", sentinel)
    assert juneau_loader.load()[0] is sentinel


def test_the_loader_writes_no_bytecode_into_the_source_tree(tmp_path, monkeypatch):
    """A fresh process, without -B, on a scratch copy of the tree: loading and the toolkit's late imports write no
    __pycache__ under juneau-terminal (the Maven build's RAT check scans it)."""
    python_src = SCRIPTS_DIR.parent / "juneau-terminal" / "src" / "main" / "python"
    tree = tmp_path / "tree"
    shutil.copytree(python_src, tree / "juneau-terminal" / "src" / "main" / "python",
                    ignore=shutil.ignore_patterns("__pycache__"))
    (tree / "scripts").mkdir()
    shutil.copy(SCRIPTS_DIR / "_juneau.py", tree / "scripts" / "_juneau.py")
    code = ("import sys; sys.path.insert(0, sys.argv[1]); import _juneau; jr, js = _juneau.load(); "
            "from juneau_terminal_scripts import git, run, tools, notify, steps, state, prompt; print(jr.__file__)")
    # a push or test.py run exports where the real engine lives; the child must still find the copy beside _juneau.py
    monkeypatch.setenv("JUNEAU_TERMINAL_PY", str(python_src / "juneau_terminal.py"))
    monkeypatch.setenv("JUNEAU_TERMINAL_SCRIPTS_ZIP", str(python_src))
    env = {k: v for k, v in os.environ.items() if k not in ("PYTHONDONTWRITEBYTECODE", "JUNEAU_TERMINAL_PY", "JUNEAU_TERMINAL_SCRIPTS_ZIP")}
    done = subprocess.run([sys.executable, "-c", code, str(tree / "scripts")], env=env, capture_output=True,
                          text=True, check=False)
    assert done.returncode == 0, done.stderr
    assert Path(done.stdout.strip()).resolve().is_relative_to(tree.resolve())
    assert list((tree / "juneau-terminal").rglob("__pycache__")) == []


# ----------------------------------------------------------------------------------------------------------------------
# push.py's steps take their numbers from the toolkit's session counter
# ----------------------------------------------------------------------------------------------------------------------

def test_push_steps_share_the_toolkit_counter(push_mod, monkeypatch, capsys):
    monkeypatch.setenv("TERMINAL_MARKERS", "1")
    run = push_mod.toolkit("run")
    monkeypatch.setattr(run, "_n", 0)
    with push_mod.marked_step("a", "A"):
        pass
    with run.step("b", "B"):
        pass
    assert push_mod.next_step_number() == 3
    assert [(e["id"], e["n"]) for e in markers(capsys.readouterr().out) if e["ev"] == "step"] == [("a", 1), ("b", 2)]


class HistoryOffTimings(FakeTimings):
    @staticmethod
    def history_off():
        return True


def test_the_timing_row_says_when_history_is_off(push_mod, console, monkeypatch, tmp_path):
    _, events = console
    monkeypatch.setattr(push_mod, "load_timings_module", lambda script_dir: HistoryOffTimings)
    HistoryOffTimings.written.clear()
    push_mod.run_timing_report(tmp_path, tmp_path)
    assert HistoryOffTimings.written == []
    [timing] = [f for f in events.of("step_end") if f["id"] == "timing"]
    assert timing["summary"] == "run history is off (JUNEAU_TERMINAL_HISTORY=0)"


def test_the_timing_row_reads_the_test_store_not_the_push_store(push_mod, console, run_history, history_record,
                                                               monkeypatch, tmp_path):
    """The real push-timings module on F1's store: the Timing row reads test.py's records for the repo, and the
    report lands beside them as test-timings.txt."""
    _, events = console
    repo = tmp_path / "repo"
    repo.mkdir()
    run_history.start(repo)
    store = run_history.store("test", repo=repo)
    for wall in (100, 101, 160):
        store.append(history_record(step="tests", wall_s=wall, series={"surefire_s": {"juneau-core.core": wall}}))
    push_store = run_history.store("push", repo=repo)
    push_store.append(history_record(step="tests", wall_s=999, series={"surefire_s": {"decoy.core": 1}}))
    push_mod.run_timing_report(push_mod.Path(push_mod.__file__).parent, repo)
    [timing] = [f for f in events.of("step_end") if f["id"] == "timing"]
    report = store.path.with_name("test-timings.txt")
    assert timing["summary"].endswith(f"→ {report}")
    text = report.read_text(encoding="utf-8")
    assert "160.0s" in text and "999" not in text
