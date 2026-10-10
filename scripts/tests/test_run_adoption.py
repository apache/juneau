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
Tests for push.py and test.py's adoption of juneau-run: marker streams with a fake `mvn` on PATH, the
`--test-only` mode of push.py, and the pins that keep the default (markers off) behaviour unchanged.
Hermetic: no real Maven, no git writes, no sounds; push.py's gates are replaced by recorders.
"""

from __future__ import annotations

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

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
PREFIX = "##run "


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS_DIR / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


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
    module = sys.modules.pop("juneau_run", None)
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

def test_test_py_default_path_never_touches_run_tool(test_mod, fake_mvn, monkeypatch, capfd):
    run = test_mod.run_module()
    monkeypatch.setattr(run, "run_tool", lambda *a, **k: pytest.fail("run_tool must not be used by default"))
    code, out = test_mod.run_command("mvn test", "tests", "Tests")
    assert code == 0
    assert out == "fake mvn: test\n"
    assert "##run" not in capfd.readouterr().out


def test_test_py_markers_on_runs_the_command_as_a_step(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("RUN_MARKERS", "1")
    code, out = test_mod.run_command("mvn test -T1C -Dtest='A B'", "tests", "Tests")
    assert code == 0
    assert out == "fake mvn: test -T1C -Dtest=A B\n"
    shown = capfd.readouterr().out
    assert markers(shown) == [
        {"ev": "step", "id": "tests", "n": 1, "title": "Tests"},
        {"ev": "end", "id": "tests", "status": "ok"},
    ]
    assert "fake mvn: test" in shown


def test_test_py_step_numbers_continue_after_n_base(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setenv("JUNEAU_RUN_N_BASE", "3")
    test_mod.run_command("mvn test", "tests", "Tests")
    assert markers(capfd.readouterr().out)[0]["n"] == 4


def test_test_py_failure_exit_code_is_returned_and_marked(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setenv("FAKE_MVN_EXIT", "3")
    code, _ = test_mod.run_command("mvn test", "tests", "Tests")
    assert code == 3
    assert markers(capfd.readouterr().out)[-1] == {"ev": "end", "id": "tests", "status": "fail", "exit": 3}


def test_test_py_routing_flags_work_without_markers(test_mod, fake_mvn, monkeypatch, capfd, tmp_path):
    log = tmp_path / "full.log"
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "none")
    monkeypatch.setenv("JUNEAU_RUN_FULL_LOG", str(log))
    code, out = test_mod.run_command("mvn test", "tests", "Tests")
    assert code == 0 and out == "fake mvn: test\n"
    assert log.read_text() == "fake mvn: test\n"
    assert "fake mvn" not in capfd.readouterr().out


def test_test_py_wrapper_prefix_stays_in_the_command(test_mod, monkeypatch, capfd):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", "echo wrapped")
    _, out = test_mod.run_command("mvn test", "tests", "Tests")
    assert out == "wrapped mvn test\n"


def test_test_py_under_an_active_run_emits_steps_but_no_run_or_done(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setenv("JUNEAU_RUN_ACTIVE", "1")
    monkeypatch.setattr(sys, "argv", ["test.py", "--build-only"])
    assert test_mod.main() == 0
    events = markers(capfd.readouterr().out)
    assert [e["ev"] for e in events] == ["step", "end"]
    assert events[0]["id"] == "build"


def test_test_py_standalone_with_markers_owns_run_and_done(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setattr(sys, "argv", ["test.py", "--build-only"])
    assert test_mod.main() == 0
    events = markers(capfd.readouterr().out)
    assert [e["ev"] for e in events] == ["run", "step", "end", "done"]
    assert events[0]["mode"] == "test"
    assert events[-1]["status"] == "ok"


def test_test_py_console_flag_routes_the_output(test_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setattr(sys, "argv", ["test.py", "--build-only", "--console", "none"])
    assert test_mod.main() == 0
    assert "fake mvn" not in capfd.readouterr().out
    assert os.environ["JUNEAU_RUN_CONSOLE"] == "none"
    monkeypatch.delenv("JUNEAU_RUN_CONSOLE")


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
    monkeypatch.setattr(push_mod, "play_sound", rec("sound"))
    monkeypatch.setattr(push_mod, "run_command", rec("run_command"))
    monkeypatch.setattr(push_mod, "run_timing_report", rec("timing"))
    monkeypatch.setattr(push_mod, "verify_starter_repos", rec("starters"))
    monkeypatch.setattr(push_mod, "verify_apache_identity", rec("identity"))
    monkeypatch.setattr(push_mod, "commit_and_push", lambda *a, **k: rec.calls.append("commit") or ("ok", 1))
    monkeypatch.setattr(push_mod, "run_docs_followup", lambda *a, **k: rec.calls.append("docs") or ("ok", 1))
    monkeypatch.setattr(push_mod, "_append_test_run_history", rec("history"))
    monkeypatch.setattr(push_mod, "current_branch", lambda root: "main")
    monkeypatch.setattr(push_mod, "git_short_head", lambda root: "abc1234")
    monkeypatch.setattr(push_mod, "run_test_script", lambda cmd, cwd, run: rec.calls.append("test.py") or
                        subprocess.CompletedProcess(cmd, 0))
    monkeypatch.setattr(push_mod.subprocess, "run", lambda *a, **k: type("R", (), {"returncode": 0})())
    return rec


def test_push_test_only_emits_mode_test_and_never_commits(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    events = markers(capsys.readouterr().out)
    project = Path(push_mod.__file__).resolve().parent.parent.name
    assert events[0] == {"ev": "run", "v": 1, "mode": "test", "project": project}
    assert [e["id"] for e in events if e["ev"] == "step"] == ["container-tags", "bom", "install", "starters"]
    assert events[-2] == {"ev": "note", "level": "info", "text": "Test-only run: nothing was committed or pushed"}
    assert events[-1] == {"ev": "done", "status": "ok"}
    assert not {"identity", "commit", "docs"} & set(gates.calls)
    assert "starters" in gates.calls


def test_push_test_only_does_not_need_a_message(push_mod, gates, monkeypatch):
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0


def test_push_without_message_still_requires_one(push_mod, gates, monkeypatch):
    monkeypatch.setattr(sys, "argv", ["push.py"])
    monkeypatch.setattr("builtins.input", lambda _prompt: "")
    with pytest.raises(SystemExit) as exc:
        push_mod.main()
    assert exc.value.code == 1


@pytest.mark.parametrize("flag", ["--docs-only", "--sonarqube", "--tracker-audit", "--skip-tests"])
def test_push_test_only_rejects_flags_that_commit_or_gate(push_mod, gates, monkeypatch, flag):
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only", flag])
    with pytest.raises(SystemExit) as exc:
        push_mod.main()
    assert exc.value.code == 2


def test_push_markers_off_prints_no_markers_and_reaches_the_commit(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setattr(sys, "argv", ["push.py", "a message"])
    assert push_mod.main() == 0
    assert "##run" not in capsys.readouterr().out
    assert "identity" in gates.calls and "commit" in gates.calls


def test_push_full_run_emits_push_mode_and_done_ok(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setattr(sys, "argv", ["push.py", "a message"])
    assert push_mod.main() == 0
    events = markers(capsys.readouterr().out)
    assert events[0]["mode"] == "push"
    assert events[-1] == {"ev": "done", "status": "ok"}


def test_push_failed_gate_ends_the_run_with_done_fail(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setattr(push_mod, "run_command", lambda *a, **k: False)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 1
    events = markers(capsys.readouterr().out)
    assert events[1] == {"ev": "step", "id": "container-tags", "n": 1, "title": "Container test tags"}
    assert events[2]["status"] == "fail"
    assert events[-1] == {"ev": "done", "status": "fail"}


def test_push_run_command_with_a_tool_emits_the_step_and_keeps_the_result_lines(push_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("RUN_MARKERS", "1")
    ok = push_mod.run_command(["mvn", "install"], "Build", tool="maven", step_id="install", title="Build and install", n=7)
    shown = capfd.readouterr().out
    assert ok is True
    assert markers(shown)[0] == {"ev": "step", "id": "install", "n": 7, "title": "Build and install"}
    assert "✅ Build - SUCCESS" in shown


def test_push_run_command_markers_off_keeps_the_exact_original_call(push_mod, fake_mvn, monkeypatch, capfd, tmp_path):
    run = push_mod.run_module()
    monkeypatch.setattr(run, "run_tool", lambda *a, **k: pytest.fail("run_tool must not be used with markers off"))
    seen = []
    real_run = subprocess.run
    monkeypatch.setattr(push_mod.subprocess, "run", lambda *a, **k: seen.append((a, k)) or real_run(*a, **k))
    ok = push_mod.run_command(["mvn", "install"], "Build", tmp_path, tool="maven", step_id="install", title="Build", n=1)
    assert ok is True
    assert "##run" not in capfd.readouterr().out
    assert seen == [((["mvn", "install"],), {"cwd": tmp_path, "shell": False, "check": True, "capture_output": False,
                                             "text": True})]


def test_push_run_command_failure_returns_false(push_mod, fake_mvn, monkeypatch, capfd):
    monkeypatch.setenv("FAKE_MVN_EXIT", "2")
    assert push_mod.run_command(["mvn", "install"], "Build", tool="maven", step_id="install") is False
    assert "❌ Build - FAILED (exit code: 2)" in capfd.readouterr().out


def test_push_starter_steps_are_children_of_the_starters_step(push_mod, fake_mvn, monkeypatch, tmp_path, capfd):
    starter = tmp_path / "demo-starter"
    starter.mkdir()
    monkeypatch.setattr(push_mod, "STARTER_REPO_PATHS", [starter])
    monkeypatch.setenv("RUN_MARKERS", "1")
    assert push_mod.verify_starter_repos(9) is True
    step = markers(capfd.readouterr().out)[0]
    assert step == {"ev": "step", "id": "starters/demo-starter", "n": 1, "title": "demo-starter", "parent": "starters"}


def test_push_reserves_step_numbers_for_the_steps_test_py_contributes(push_mod, gates, monkeypatch):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert os.environ["JUNEAU_RUN_N_BASE"] == "2"
    monkeypatch.delenv("JUNEAU_RUN_N_BASE")


def test_push_test_only_fallback_runs_mvn_test_as_a_maven_step(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("RUN_MARKERS", "1")
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
    run = push_mod.run_module()
    assert push_mod.run_test_script(["x"], tmp_path, run).returncode == 0
    assert seen == [((["x"],), {"cwd": tmp_path, "check": False})]


def test_run_test_script_markers_on_gives_the_child_its_own_session(push_mod, monkeypatch, tmp_path):
    monkeypatch.setenv("RUN_MARKERS", "1")
    script = tmp_path / "child.py"
    script.write_text("import os\nraise SystemExit(0 if os.getsid(0) == os.getpid() else 7)\n", encoding="utf-8")
    run = push_mod.run_module()
    assert push_mod.run_test_script([sys.executable, str(script)], tmp_path, run).returncode == 0


def test_test_py_wrapper_with_and_and_markers_on_runs_through_a_shell(test_mod, monkeypatch):
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setenv("JUNEAU_MVN_WRAPPER", "echo first &&")
    code, out = test_mod.run_command("mvn --version", "tests", "Tests")
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
    env = {**os.environ, "RUN_MARKERS": "1", "PATH": f"{bin_dir}{os.pathsep}{os.environ['PATH']}"}
    for variable in ("JUNEAU_RUN_ACTIVE", "JUNEAU_RUN_N_BASE", "JUNEAU_MVN_WRAPPER"):
        env.pop(variable, None)
    # A scratch tree holding only copies of the two scripts (and the real juneau-run), so push.py's other gates
    # (container tags, BOM, PGP prompt) have nothing to run and nothing real is built.
    root = tmp_path / "tree"
    (root / "scripts").mkdir(parents=True)
    for name in ("push.py", "test.py"):
        shutil.copy(SCRIPTS_DIR / name, root / "scripts" / name)
    (root / "juneau-run").symlink_to(SCRIPTS_DIR.parent / "juneau-run")
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
    """A condensed session; stdout is captured, so it draws in plain mode.  Returns (juneau_run, Events)."""
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "condensed")
    run = push_mod.run_module()
    run.session("🚀 Juneau push · test")
    assert run.console_active()
    events = run.subscribe(Events())
    return run, events


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
    monkeypatch.setenv("RUN_MARKERS", "1")
    commands = recorded_commands(push_mod, monkeypatch)
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    install = [e for e in markers(capsys.readouterr().out) if e.get("id") == "install"]
    assert [(e["ev"], e.get("status")) for e in install] == [("step", None), ("end", "skip")]
    assert INSTALL not in commands


def test_the_skipped_install_shows_done_by_tests(push_mod, gates, console, monkeypatch):
    _, events = console
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    [install] = [f for f in events.of("step_end") if f["id"] == "install"]
    assert (install["status"], install["summary"]) == ("skip", "(done by Tests)")


class FakeTimings:
    written = []

    @staticmethod
    def load_records(path):
        return [{"stub": True}]

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
    push_mod.run_timing_report(tmp_path, tmp_path / "master.jsonl")
    report = tmp_path / "master.txt"
    assert FakeTimings.written == [(3, report)]
    assert [(f["level"], f["text"]) for f in events.of("note")] == [
        ("warn", "juneau-core  surefire  +31%  +9.0s"), ("info", "juneau-rest  surefire  -25%  -6.0s")]
    [timing] = [f for f in events.of("step_end") if f["id"] == "timing"]
    assert (timing["status"], timing["summary"]) == ("ok", f"1 slower than 20% and 5s · 1 faster → {report}")


def test_push_exports_detail_to_test_py(push_mod, gates, monkeypatch):
    seen = []
    monkeypatch.setattr(push_mod, "run_test_script", lambda cmd, cwd, run:
                        seen.append(os.environ.get("JUNEAU_RUN_DETAIL")) or subprocess.CompletedProcess(cmd, 0))
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only", "--detail", "modules"])
    assert push_mod.main() == 0
    assert seen == ["modules"]


def test_push_rejects_an_unknown_detail_level(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only", "--detail", "loud"])
    with pytest.raises(SystemExit) as exc:
        push_mod.main()
    assert exc.value.code == 2
    assert "actionable" in capsys.readouterr().err


def test_push_session_header_shows_identity_and_pgp(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "condensed")
    events = push_mod.run_module().subscribe(Events())
    monkeypatch.setattr(sys, "argv", ["push.py", "Quick fix", "--skip-tests"])
    assert push_mod.main() == 0
    [session] = events.of("session")
    assert session["title"] == "🚀 Juneau push · main"
    assert session["header"][:2] == ["jamesbognar@apache.org ✓", "PGP ✓"]
    out = capsys.readouterr().out
    assert "=" * 70 not in out
    assert "Juneau Build and Push Script" not in out


def test_push_prompts_for_pgp_before_the_gates(push_mod, gates, monkeypatch):
    order = []
    monkeypatch.setattr(push_mod.subprocess, "run", lambda cmd, *a, **k: order.append(
        "pgp" if "prompt-pgp-passphrase.py" in " ".join(map(str, cmd)) else "other") or
        type("R", (), {"returncode": 0})())
    monkeypatch.setattr(push_mod, "run_sonarqube_gate", lambda root, n: order.append("sonar") or "pass")
    monkeypatch.setattr(sys, "argv", ["push.py", "Quick fix", "--skip-tests", "--sonarqube"])
    assert push_mod.main() == 0
    assert order.index("pgp") < order.index("sonar")


def test_push_hands_the_console_to_test_py(push_mod, gates, console, monkeypatch):
    run, _ = console
    calls = []
    monkeypatch.setattr(run._console, "pause", lambda: calls.append("pause"))
    monkeypatch.setattr(run._console, "resume", lambda: calls.append("resume"))
    monkeypatch.setattr(push_mod, "run_test_script", lambda cmd, cwd, r: calls.append(
        ("test.py", os.environ.get("JUNEAU_RUN_LABEL_WIDTH"))) or subprocess.CompletedProcess(cmd, 0))
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert calls[:3] == ["pause", ("test.py", "10"), "resume"]   # the final line may pause again


def test_push_ctrl_c_with_a_console_is_a_clean_cancel(push_mod, gates, console, monkeypatch):
    _, events = console

    def interrupted(cmd, cwd, run):
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
    """commit_and_push's collaborators, stubbed so it reaches the commit and the push; returns the sound calls."""
    sounds = []
    monkeypatch.setattr(push_mod, "check_upstream_changes", lambda repo: (1, 0, None))
    monkeypatch.setattr(push_mod, "get_staged_paths", lambda repo: ["a.txt"])
    monkeypatch.setattr(push_mod, "check_unreviewed_changes", lambda repo: [])
    monkeypatch.setattr(push_mod, "upstream_name", lambda repo: upstream)
    monkeypatch.setattr(push_mod, "play_sound", lambda success=True: sounds.append(success))
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


def test_the_identity_failure_is_an_error_note_under_a_console(push_mod, console, monkeypatch):
    _, events = console
    monkeypatch.setattr(push_mod.subprocess, "run", lambda *a, **k: type("R", (), {"stdout": "x@y.org\n"})())
    assert push_mod.verify_apache_identity(Path(".")) is False
    texts = [text for level, text in say_events(events) if level == "error"]
    assert texts[0].startswith("❌ ERROR: Git identity is not the ASF committer identity.")
    assert any("git config user.email" in text for text in texts)
    assert all(level == "error" for level, _ in say_events(events))


def test_the_identity_failure_is_printed_without_a_console(push_mod, monkeypatch, capsys):
    monkeypatch.setattr(push_mod.subprocess, "run", lambda *a, **k: type("R", (), {"stdout": "x@y.org\n"})())
    assert push_mod.verify_apache_identity(Path(".")) is False
    assert "❌ ERROR: Git identity is not the ASF committer identity." in capsys.readouterr().out


@pytest.fixture
def docs_tree(push_mod, monkeypatch, tmp_path):
    """A scratch juneau/juneau-docs pair that push.py believes it lives in."""
    (tmp_path / "juneau" / "scripts").mkdir(parents=True)
    (tmp_path / "juneau-docs").mkdir()
    monkeypatch.setattr(push_mod, "__file__", str(tmp_path / "juneau" / "scripts" / "push.py"))
    monkeypatch.setattr(push_mod, "verify_apache_identity", lambda repo: True)
    monkeypatch.setattr(push_mod, "current_branch", lambda root: "main")
    monkeypatch.setattr(push_mod, "play_sound", lambda success=True: None)
    return tmp_path


def test_docs_only_under_a_console_has_one_session_and_one_done(push_mod, docs_tree, monkeypatch):
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "condensed")
    events = push_mod.run_module().subscribe(Events())
    monkeypatch.setattr(push_mod, "commit_and_push", lambda *a, **k: ("ok", 6))
    monkeypatch.setattr(sys, "argv", ["push.py", "--docs-only", "Docs fix"])
    assert push_mod.main() == 0
    [session] = events.of("session")
    assert session["title"] == "📚 Juneau docs push · main"
    assert [f["status"] for f in events.of("done")] == ["ok"]


def test_docs_only_with_markers_on_emits_no_done_marker(push_mod, docs_tree, monkeypatch, capsys):
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "condensed")
    monkeypatch.setenv("RUN_MARKERS", "1")
    monkeypatch.setattr(push_mod, "commit_and_push", lambda *a, **k: ("ok", 6))
    monkeypatch.setattr(sys, "argv", ["push.py", "--docs-only", "Docs fix"])
    assert push_mod.main() == 0
    assert "done" not in [e["ev"] for e in markers(capsys.readouterr().out)]


@pytest.mark.parametrize("flags", [[], ["--docs-only"]])
def test_a_dry_run_under_a_console_still_says_so(push_mod, docs_tree, monkeypatch, capsys, flags):
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "condensed")
    monkeypatch.setattr(sys, "argv", ["push.py", "--dry-run", "a message", *flags])
    assert push_mod.main() == 0
    assert "DRY RUN MODE" in capsys.readouterr().out


class NoHistoryTimings(FakeTimings):
    @staticmethod
    def load_records(path):
        return []

    @staticmethod
    def latest_run(records):
        return [], "no timing history yet"


def test_the_timing_row_on_a_first_run_says_there_is_no_history(push_mod, console, monkeypatch, tmp_path):
    _, events = console
    monkeypatch.setattr(push_mod, "load_timings_module", lambda script_dir: NoHistoryTimings)
    NoHistoryTimings.written.clear()
    push_mod.run_timing_report(tmp_path, tmp_path / "master.jsonl")
    assert NoHistoryTimings.written == []
    assert events.of("note") == []
    [timing] = [f for f in events.of("step_end") if f["id"] == "timing"]
    assert (timing["status"], timing["summary"]) == ("ok", "no timing history yet")


def test_the_timing_row_with_history_still_writes_the_report(push_mod, console, monkeypatch, tmp_path):
    monkeypatch.setattr(push_mod, "load_timings_module", lambda script_dir: FakeTimings)
    FakeTimings.written.clear()
    push_mod.run_timing_report(tmp_path, tmp_path / "master.jsonl")
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
    assert sounds == [False]


def test_the_docs_followup_keeps_its_rows_apart_from_juneaus(push_mod, console, monkeypatch, tmp_path):
    run, _ = console
    (tmp_path / "juneau-docs").mkdir()
    stub_push_flow(push_mod, monkeypatch)
    monkeypatch.setattr(push_mod, "verify_apache_identity", lambda repo: True)
    calls = routed_calls(run, monkeypatch)
    assert push_mod.run_docs_followup(tmp_path / "juneau", "msg", 5) == ("ok", 7)
    assert [(c["step_id"], c["label"]) for c in calls] == [
        ("docs-smoke", "Docs build"), ("docs-commit", "Docs"), ("docs-push", "Docs push")]


def test_main_uses_the_timing_row_under_a_console_and_the_cli_otherwise(push_mod, gates, console, monkeypatch):
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
    assert any("push-timings.py" in " ".join(map(str, cmd)) for cmd in commands)


def test_the_upstream_is_only_looked_up_under_a_console(push_mod, monkeypatch):
    stub_push_flow(push_mod, monkeypatch)
    monkeypatch.setattr(push_mod, "upstream_name", lambda repo: pytest.fail("no console, no upstream lookup"))
    monkeypatch.setattr(push_mod, "run_command", lambda *a, **k: True)
    assert push_mod.commit_and_push(Path("."), "msg", 5) == ("ok", 7)


def test_run_marked_passes_the_label_to_its_fallback(push_mod, monkeypatch):
    seen = []
    monkeypatch.setattr(push_mod, "run_command", lambda *a, **k: seen.append(k) or True)
    assert push_mod.run_marked("bom", "BOM", ["x"], "Checking", label="BOM")
    assert seen == [{"label": "BOM"}]


def test_the_skipped_install_is_not_repeated_as_a_note_under_a_console(push_mod, gates, console, monkeypatch):
    _, events = console
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0
    assert not [text for _, text in say_events(events) if "already done by test.py" in text]


@pytest.mark.parametrize("gate", ["sonar", "tracker"])
def test_a_gate_that_could_not_run_points_at_the_full_log_under_a_console(push_mod, gates, console, monkeypatch, gate):
    _, events = console
    monkeypatch.setattr(push_mod, "run_sonarqube_gate", lambda root, n: "error")
    monkeypatch.setattr(push_mod, "run_tracker_audit_gate", lambda root, n: "error")
    monkeypatch.setattr(sys, "argv", ["push.py", "msg", "--sonarqube" if gate == "sonar" else "--tracker-audit"])
    assert push_mod.main() == 1
    texts = [text for _, text in say_events(events)]
    assert any("full log" in text for text in texts)
    assert not any("reported above" in text for text in texts)


def test_a_gate_that_could_not_run_still_points_above_without_a_console(push_mod, gates, monkeypatch, capsys):
    monkeypatch.setattr(push_mod, "run_sonarqube_gate", lambda root, n: "error")
    monkeypatch.setattr(sys, "argv", ["push.py", "msg", "--sonarqube"])
    assert push_mod.main() == 1
    assert "Fix the issue reported above" in capsys.readouterr().out
