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


@pytest.fixture(autouse=True)
def clean_env(monkeypatch):
    for variable in ("RUN_MARKERS", "RUN_ARTIFACTS", "JUNEAU_RUN_ACTIVE", "JUNEAU_RUN_N_BASE", "JUNEAU_RUN_CONSOLE",
                     "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_CONDENSED_LOG", "JUNEAU_MVN_WRAPPER"):
        monkeypatch.delenv(variable, raising=False)
    sys.modules.pop("juneau_run", None)
    yield
    sys.modules.pop("juneau_run", None)


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
    assert [e["id"] for e in events if e["ev"] == "step"] == ["container-tags", "bom", "starters"]
    assert events[-2] == {"ev": "note", "level": "info", "text": "Test-only run: nothing was committed or pushed"}
    assert events[-1] == {"ev": "done", "status": "ok"}
    assert not {"identity", "commit", "docs"} & set(gates.calls)
    assert "starters" in gates.calls


def test_push_test_only_does_not_need_a_message(push_mod, gates, monkeypatch):
    monkeypatch.setattr(sys, "argv", ["push.py", "--test-only"])
    assert push_mod.main() == 0


def test_push_without_message_still_requires_one(push_mod, gates, monkeypatch):
    monkeypatch.setattr(sys, "argv", ["push.py"])
    with pytest.raises(SystemExit) as exc:
        push_mod.main()
    assert exc.value.code == 2


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
