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
scripts/release.py on juneau_run: commands go through run_tool (Maven with -B and a PTY), every prompt is run.ask,
the full log is always written, steps are rows, and run_git_diff is wired in.  Hermetic: a FakeRun stands in for
juneau_run except where a test says otherwise, HOME is a temp dir (so the state file and logs land there), and no
real mvn, svn or gpg runs.
"""

from __future__ import annotations

import ast
import importlib.util
import json
import os
import runpy
import subprocess
import sys
from contextlib import contextmanager
from pathlib import Path
from types import SimpleNamespace

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
RUN_VARIABLES = ("RUN_MARKERS", "RUN_ARTIFACTS", "JUNEAU_RUN_ACTIVE", "JUNEAU_RUN_N_BASE", "JUNEAU_RUN_CONSOLE",
                 "JUNEAU_RUN_FULL_LOG", "JUNEAU_RUN_CONDENSED_LOG", "JUNEAU_RUN_SESSION", "JUNEAU_RUN_DETAIL",
                 "JUNEAU_RUN_LABEL_WIDTH", "JUNEAU_RUN_LIVE")
RELEASE_VARIABLES = ("X_VERSION", "X_NEXT_VERSION", "X_RELEASE", "X_STAGING", "X_USERNAME", "X_EMAIL", "X_GIT_BRANCH",
                     "X_JAVA_HOME", "X_CLEANM2", "X_REPO", "GPG_TTY")
RELEASE = "juneau-9.9.9-RC1"
JAVA_TEXT = 'openjdk version "17.0.12" 2024-07-16\n'
MAVEN_TEXT = "Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)\n"


@pytest.fixture(autouse=True)
def clean_env(monkeypatch, tmp_path):
    saved = {key: os.environ.get(key) for key in RUN_VARIABLES + RELEASE_VARIABLES + ("JAVA_HOME", "PATH")}
    for variable in RUN_VARIABLES + RELEASE_VARIABLES:
        monkeypatch.delenv(variable, raising=False)
    (tmp_path / "home").mkdir()
    monkeypatch.setenv("HOME", str(tmp_path / "home"))
    monkeypatch.setenv("JUNEAU_RUN_LOG_DIR", str(tmp_path / "run-logs"))
    sys.modules.pop("juneau_run", None)
    yield
    sys.modules.pop("juneau_run", None)
    for key, value in saved.items():   # release.py and session() write os.environ directly
        if value is None:
            os.environ.pop(key, None)
        else:
            os.environ[key] = value


@pytest.fixture
def release(clean_env):
    """release.py, loaded after HOME points at the temp dir, so STATE_FILE and RELEASE_LOG_DIR are under it."""
    spec = importlib.util.spec_from_file_location("_release_test", SCRIPTS_DIR / "release.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FakeStep:
    def __init__(self):
        self.status, self.exit, self.summary = "ok", None, None

    def fail(self, exit=None):
        self.status, self.exit = "fail", exit

    def skip(self):
        self.status = "skip"


class FakeRun:
    """Records what release.py asks of juneau_run.  answers feed ask(); tool(cmd) gives run_tool's (exit, output)."""

    def __init__(self, answers=()):
        self.answers = list(answers)
        self.tool = lambda cmd: (0, "")
        self.calls = []
        self.rows = []   # (kind, id, label, status, summary) as each row ends
        self.numbers = []   # (id, n) as each step opens

    def of(self, kind):
        return [(args, kwargs) for name, args, kwargs in self.calls if name == kind]

    def _record(self, kind, *args, **kwargs):
        self.calls.append((kind, args, kwargs))

    def ask(self, prompt):
        self._record("ask", prompt)
        assert self.answers, f"unexpected prompt: {prompt!r}"
        return self.answers.pop(0)

    def say(self, text="", level="info", step=None):
        self._record("say", text, level=level)

    def show(self, text, pager=False):
        self._record("show", text, pager=pager)

    def failure(self, text, step=None):
        self._record("failure", text, step=step)

    def warn(self, text):
        self._record("warn", text)

    def session(self, title, header=(), *, detail=None, sinks=None, repo="."):
        self._record("session", title, list(header))
        self.console = True

    # What juneau_scripts' session, step and prompt ask of juneau_run besides the above.
    console = False

    def enabled(self):
        return False

    def console_active(self):
        return self.console

    def history_start(self, repo=".", history=None):
        pass

    def Sinks(self):  # noqa: N802 - mirrors juneau_run.Sinks
        default = not os.environ.get("JUNEAU_RUN_FULL_LOG") and os.environ.get("JUNEAU_RUN_CONSOLE", "full") == "full"
        return SimpleNamespace(is_default=default)

    def plan(self, ids):
        self._record("plan", list(ids))

    def done(self, status, commit=None):
        self._record("done", status)

    def export_detail(self, flag=None):
        if flag not in (None, "summary", "actionable", "modules", "all"):
            raise ValueError(f"detail must be one of summary, actionable, modules, all, not {flag!r}")
        self._record("export_detail", flag)
        return flag or "actionable"

    def passthrough(self, cmd, step_id, title, **kwargs):
        self._record("passthrough", cmd, step_id, title, **kwargs)
        return SimpleNamespace(exit=0, ms=0)

    def run_tool(self, cmd, parser, step_id, title, **kwargs):
        self._record("run_tool", cmd, parser, step_id, title, **kwargs)
        exit, output = self.tool(cmd)
        return SimpleNamespace(exit=exit, ms=0, summary={}, output=output if kwargs.get("capture") else None)

    @contextmanager
    def _row(self, kind, id, label):
        s = FakeStep()
        try:
            yield s
        except BaseException:
            s.status = "fail"
            raise
        finally:
            self.rows.append((kind, id, label, s.status, s.summary))

    def step(self, id, n, title, *, parent=None, label=None):
        self.numbers.append((id, n))
        return self._row("step", id, label)

    def row(self, id, title, *, label=None):
        return self._row("row", id, label)


def install(monkeypatch, fake):
    """fake as the loaded juneau_run: release.py and juneau_scripts both look it up in sys.modules."""
    monkeypatch.setitem(sys.modules, "juneau_run", fake)
    return fake


def make(release, monkeypatch, fake, **kwargs):
    install(monkeypatch, fake)
    script = release.ReleaseScript(load_env=False, **kwargs)
    script._versions = (JAVA_TEXT, MAVEN_TEXT)   # never probe the real java/mvn
    return script


def calls_to(name, allowed=()):
    """Line numbers of calls to the bare function `name` in release.py outside the functions named in allowed."""
    tree = ast.parse((SCRIPTS_DIR / "release.py").read_text(encoding="utf-8"))
    found = []

    def visit(node, function):
        for child in ast.iter_child_nodes(node):
            inner = child.name if isinstance(child, (ast.FunctionDef, ast.AsyncFunctionDef)) else function
            if (isinstance(child, ast.Call) and isinstance(child.func, ast.Name) and child.func.id == name
                    and inner not in allowed):
                found.append(child.lineno)
            visit(child, inner)

    visit(tree, None)
    return found


# ---- plumbing ------------------------------------------------------------------------------------------------------
def test_release_py_compiles():
    path = SCRIPTS_DIR / "release.py"
    compile(path.read_text(encoding="utf-8"), str(path), "exec")   # no .pyc written, unlike py_compile


def test_maven_gets_batch_mode_a_pty_and_the_watchdog(release, monkeypatch, tmp_path):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    script.current = "run_clean_verify"
    script.run_command(["mvn", "clean", "verify"], cwd=tmp_path)
    [(args, kwargs)] = fake.of("run_tool")
    assert args == (["mvn", "-B", "clean", "verify"], "maven", "run_clean_verify.1", "mvn -B clean verify")
    assert kwargs == {"n": 1, "parent": "run_clean_verify", "cwd": tmp_path, "capture": False, "tty": True,
                      "watchdog": 20}


def test_svn_commit_and_git_push_get_a_pty_and_other_commands_do_not(release, monkeypatch, tmp_path):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    script.current = "create_binary_artifacts"
    script.run_command(["svn", "add", "source/x"], cwd=tmp_path)
    script.run_command(["svn", "commit", "-m", "x"], cwd=tmp_path)
    script.run_command(["git", "push", "origin", ":x"], cwd=tmp_path)
    (add_args, add), (commit_args, commit), (push_args, push) = fake.of("run_tool")
    assert (add_args[1], add["tty"], add["watchdog"]) == ("generic", False, None)
    assert (commit_args[0], commit["tty"], commit["watchdog"]) == (["svn", "commit", "-m", "x"], True, 20)
    assert (push_args[0], push["tty"], push["watchdog"]) == (["git", "push", "origin", ":x"], True, 20)
    assert [args[2] for args, _ in fake.of("run_tool")] == ["create_binary_artifacts.1", "create_binary_artifacts.2",
                                                            "create_binary_artifacts.3"]


def test_a_failed_command_fails_the_release(release, monkeypatch, tmp_path):
    fake = FakeRun()
    fake.tool = lambda cmd: (128, "")
    script = make(release, monkeypatch, fake)
    script.current = "clone_juneau"
    with pytest.raises(SystemExit) as exit_info:
        script.run_command(["git", "clone", "x"], cwd=tmp_path)
    assert exit_info.value.code == 1
    assert fake.of("failure") == [(("❌ Command failed (exit 128): git clone x",), {"step": "clone_juneau"})]


def test_check_false_returns_the_exit_and_capture_returns_the_output(release, monkeypatch, tmp_path):
    fake = FakeRun()
    fake.tool = lambda cmd: (1, "some output\n")
    script = make(release, monkeypatch, fake)
    result = script.run_command(["svn", "update"], cwd=tmp_path, check=False, capture_output=True)
    assert (result.returncode, result.stdout) == (1, "some output\n")
    assert fake.of("run_tool")[0][1]["capture"] is True
    assert fake.of("run_tool")[0][0][2] == "release.1"   # no current step: a top-level row
    assert fake.of("failure") == []


# ---- prompts -------------------------------------------------------------------------------------------------------
def test_no_input_calls_remain():
    assert calls_to("input") == []


def test_prompt_with_default_goes_through_ask(release, monkeypatch):
    fake = FakeRun(answers=["", "", "alice"])
    script = make(release, monkeypatch, fake)
    assert script._prompt_with_default("Git branch", "master") == "master"
    assert script._prompt_with_default("Apache username", "") == "alice"
    assert [args[0] for args, _ in fake.of("ask")] == ["Git branch [master]: ", "Apache username: ",
                                                        "Apache username: "]
    assert fake.of("say") == [(("This field is required. Please enter a value.",), {"level": "warn"})]


def test_yprompt_goes_through_ask_and_defaults_to_yes(release, monkeypatch):
    fake = FakeRun(answers=["", "n", "YES"])
    script = make(release, monkeypatch, fake)
    assert [script.yprompt("Go?") for _ in range(3)] == [True, False, True]
    assert {args[0] for args, _ in fake.of("ask")} == {"Go? (Y/n): "}


def test_ask_rc_retries_until_it_gets_a_number(release, monkeypatch):
    fake = install(monkeypatch, FakeRun(answers=["", "two", "3"]))
    assert release.ask_rc("Release candidate number: ") == 3
    assert [kwargs["level"] for _, kwargs in fake.of("say")] == ["warn", "warn"]
    install(monkeypatch, FakeRun(answers=[""]))
    assert release.ask_rc("Release candidate number [2]: ", "2") == 2


@pytest.mark.parametrize("ask", [
    lambda release, script: script.yprompt("Go?"),
    lambda release, script: script._prompt_with_default("Git branch", "master"),
    lambda release, script: release.ask_rc("Release candidate number: ", "2"),
])
def test_ctrl_d_at_any_prompt_is_a_cancel_never_the_default(release, monkeypatch, ask):
    class EofRun(FakeRun):
        def ask(self, prompt):
            raise EOFError

    script = make(release, monkeypatch, EofRun())
    with pytest.raises(KeyboardInterrupt):
        ask(release, script)


def test_the_staging_repo_prompt_goes_through_ask(release, monkeypatch, tmp_path):
    monkeypatch.setenv("X_STAGING", str(tmp_path / "staging"))
    monkeypatch.setattr(release.subprocess, "Popen", lambda *a, **k: None)   # the browser `open` (:800)
    fake = FakeRun(answers=["1234", "y"])
    script = make(release, monkeypatch, fake)
    script.run_release_perform()
    assert [args[0] for args, _ in fake.of("ask")] == [
        "Enter the staging repository name AFTER CLOSING IT!!!: orgapachejuneau-",
        "X_REPO = orgapachejuneau-1234.  Is this correct? (Y/n): "]
    assert os.environ["X_REPO"] == "orgapachejuneau-1234"
    assert fake.of("run_tool")[0][0][0][:3] == ["mvn", "-B", "release:perform"]


def test_the_revert_rc_prompt_goes_through_ask(release, monkeypatch):
    monkeypatch.setenv("X_VERSION", "9.9.9")   # skips the `mvn help:evaluate` probe (:1408)
    fake = FakeRun(answers=["x", "4", "n"])
    install(monkeypatch, fake)
    with pytest.raises(SystemExit) as exit_info:   # declining the revert is a cancel
        release.main(["--revert"])
    assert exit_info.value.code == 130
    assert [args[0] for args, _ in fake.of("ask")] == [
        "Release candidate number: ", "Release candidate number: ",
        "Are you sure you want to revert release juneau-9.9.9-RC4? This will delete the git tag and clean up SVN "
        "files. (Y/n): "]
    assert os.environ["X_RELEASE"] == "juneau-9.9.9-RC4"
    assert fake.of("done") == [(("cancelled",), {})]


# ---- session, full log, --detail -----------------------------------------------------------------------------------
@pytest.mark.parametrize("body, status, code", [
    (lambda: None, "ok", None),
    (lambda: sys.exit(1), "fail", 1),
    (lambda: (_ for _ in ()).throw(KeyboardInterrupt()), "cancelled", 130),
    (lambda: (_ for _ in ()).throw(RuntimeError("boom")), "fail", 1),
])
def test_the_session_ends_with_done(release, monkeypatch, body, status, code):
    monkeypatch.setenv("X_RELEASE", RELEASE)
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    if code is None:
        script._in_session("Juneau release", body, [RELEASE])
    else:
        with pytest.raises(SystemExit) as exit_info:
            script._in_session("Juneau release", body, [RELEASE])
        assert exit_info.value.code == code
    assert fake.of("session") == [(("🚀 Juneau release", [RELEASE]), {})]
    assert fake.of("done") == [((status,), {})]
    log = Path(os.environ["JUNEAU_RUN_FULL_LOG"])
    assert log.parent == Path.home() / ".juneau-release-logs" and log.name.startswith(f"{RELEASE}-")
    assert log.exists()
    # ok and cancelled final lines carry no log path (juneau_run's format_final), so it is said just before done, at
    # warn: info would hide it at the default detail
    said = [call for call in fake.of("say") if call[0][0].startswith("full log")]
    assert said == ([] if status == "fail" else [((f"full log: {log}",), {"level": "warn"})])


def test_ctrl_d_at_a_prompt_is_cancelled_like_ctrl_c(release, monkeypatch):
    monkeypatch.setenv("X_RELEASE", RELEASE)
    fake = FakeRun()
    script = make(release, monkeypatch, fake)

    def eof():
        raise EOFError

    with pytest.raises(SystemExit) as exit_info:
        script._in_session("Juneau release", eof, [RELEASE])
    assert exit_info.value.code == 130
    assert fake.of("done") == [(("cancelled",), {})]


def test_ctrl_d_before_the_session_exits_quietly(release, monkeypatch, capsys):
    def eof(self, *args, **kwargs):
        raise EOFError

    monkeypatch.setattr(release.argparse.ArgumentParser, "parse_args", eof)
    monkeypatch.setattr(sys, "argv", ["release.py"])
    with pytest.raises(SystemExit) as exit_info:
        runpy.run_path(str(SCRIPTS_DIR / "release.py"), run_name="__main__")
    assert exit_info.value.code == 130
    assert "Traceback" not in capsys.readouterr().err


@pytest.mark.parametrize("console", ["full", "condensed", "none"])
def test_the_full_log_is_written_in_every_console_mode(release, monkeypatch, tmp_path, console):
    """The real juneau_run: a command's output lands in the release log."""
    monkeypatch.setenv("X_RELEASE", RELEASE)
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", console)
    # No PGP prompt: no prompt-pgp-passphrase.py beside it, and _prompt_pgp stubbed out.  That helper's getpass reads
    # /dev/tty, which would hang the test run.
    monkeypatch.setattr(release, "__file__", str(tmp_path / "release.py"))
    script = release.ReleaseScript(load_env=False)
    script._versions = (JAVA_TEXT, MAVEN_TEXT)
    script._prompt_pgp = lambda: None
    script.steps = ["echo_step"]
    script.echo_step = lambda: script.run_command(["/bin/sh", "-c", "echo hello-from-release"])
    try:
        script.run()
    finally:
        sys.modules["juneau_run"]._end_session()
    [log] = (Path.home() / ".juneau-release-logs").glob(f"{RELEASE}-*.log")
    assert "hello-from-release" in log.read_text(encoding="utf-8", errors="replace")


def test_detail_is_exported_and_an_unknown_level_is_a_usage_error(release, monkeypatch, capsys):
    fake = FakeRun()
    install(monkeypatch, fake)
    release.main(["--list-steps", "--detail", "summary"])
    assert fake.of("export_detail") == [(("summary",), {})]
    with pytest.raises(SystemExit) as exit_info:
        release.main(["--list-steps", "--detail", "loud"])
    assert exit_info.value.code == 2
    assert "detail must be one of summary, actionable, modules, all" in capsys.readouterr().err


def test_run_markers_is_ignored_with_a_warning(release, monkeypatch):
    monkeypatch.setenv("RUN_MARKERS", "1")
    fake = FakeRun()
    install(monkeypatch, fake)
    release.main(["--list-steps"])
    assert fake.of("warn") == [(("release.py does not emit run markers; RUN_MARKERS is ignored",), {})]
    assert "RUN_MARKERS" not in os.environ


# ---- steps ---------------------------------------------------------------------------------------------------------
RELEASE_STEPS = ["check_prerequisites", "check_java_version", "check_maven_version", "clean_maven_repo",
                 "make_git_folder", "clone_juneau", "configure_git", "run_clean_verify", "run_deploy",
                 "run_release_prepare", "run_git_diff", "run_release_perform", "create_binary_artifacts",
                 "verify_distribution"]


def stub_steps(script, monkeypatch, behaviour=None):
    """Replace every step method with a recorder.  behaviour maps a step name to what it does instead."""
    ran = []
    for name in script.steps:
        def stub(name=name):
            ran.append(name)
            (behaviour or {}).get(name, lambda: None)()
        monkeypatch.setattr(script, name, stub)
    script._prompt_pgp = lambda: None
    return ran


def test_the_steps_are_in_order_with_run_git_diff_after_prepare(release, monkeypatch):
    fake = install(monkeypatch, FakeRun())
    release.main(["--list-steps"])
    assert [args[0] for args, _ in fake.of("say")] == [f"{n:>2}. {name:<24} {release.STEP_TITLES[name][1]}"
                                                       for n, name in enumerate(RELEASE_STEPS, 1)]


def test_every_step_runs_in_a_labelled_row(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    ran = stub_steps(script, monkeypatch)
    script.run()
    assert ran == RELEASE_STEPS
    assert [row[:4] for row in fake.rows] == [("step", name, release.STEP_TITLES[name][0], "ok")
                                              for name in RELEASE_STEPS]
    assert all(len(label) <= 10 for label, _ in release.STEP_TITLES.values())
    assert set(release.STEP_TITLES) == set(RELEASE_STEPS)
    assert fake.of("done") == [(("ok",), {})]


def test_resume_shows_the_finished_steps_as_done_earlier(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake, resume=True)
    script.state.set("last", "configure_git")
    ran = stub_steps(script, monkeypatch)
    script.run()
    assert ran == RELEASE_STEPS[7:]
    assert fake.rows[:7] == [("step", name, release.STEP_TITLES[name][0], "skip", "(done earlier)")
                             for name in RELEASE_STEPS[:7]]
    assert [row[3] for row in fake.rows[7:]] == ["ok"] * 7


def test_a_skipped_step_is_a_dim_row(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake, skip_steps=["run_deploy"])
    ran = stub_steps(script, monkeypatch)
    script.run()
    assert "run_deploy" not in ran
    assert ("step", "run_deploy", "Deploy", "skip", "(skipped)") in fake.rows


def hint_lines(fake, name):
    """Every say or failure line carrying the resume hint for step name, with its kind and level."""
    hint = f"To resume: python3 scripts/release.py --start-step {name}"
    return [(kind, kwargs.get("level")) for kind, args, kwargs in fake.calls
            if kind in ("say", "failure") and hint in args[0]]


def test_a_failed_step_shows_the_resume_hint(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    stub_steps(script, monkeypatch, {"run_clean_verify": lambda: script.fail("Command failed (exit 1): mvn")})
    with pytest.raises(SystemExit) as exit_info:
        script.run()
    assert exit_info.value.code == 1
    assert fake.rows[-1][:4] == ("step", "run_clean_verify", "Verify", "fail")
    assert [args[0] for args, _ in fake.of("failure")] == ["❌ Command failed (exit 1): mvn"]
    assert fake.of("done") == [(("fail",), {})]
    # one hint, at error level, so every detail level (summary included) shows it exactly once
    assert hint_lines(fake, "run_clean_verify") == [("say", "error")]


def test_ctrl_c_in_a_step_is_cancelled_with_the_resume_hint(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)

    def interrupt():
        raise KeyboardInterrupt

    stub_steps(script, monkeypatch, {"run_deploy": interrupt})
    with pytest.raises(SystemExit) as exit_info:
        script.run()
    assert exit_info.value.code == 130
    assert fake.of("failure") == [(("Interrupted in run_deploy.",), {"step": "run_deploy"})]
    assert hint_lines(fake, "run_deploy") == [("say", "error")]
    assert fake.of("done") == [(("cancelled",), {})]


def test_the_pgp_prompt_runs_on_the_real_terminal(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    monkeypatch.setattr(release.subprocess, "run", lambda *a, **k: pytest.fail(f"gpg run outside passthrough: {a}"))
    script._prompt_pgp()
    assert fake.of("passthrough") == [(([sys.executable, str(SCRIPTS_DIR / "prompt-pgp-passphrase.py")], "pgp",
                                        "Prime the PGP agent"), {"label": "PGP"})]


def test_the_header_names_the_release_java_and_maven(release, monkeypatch):
    monkeypatch.setenv("X_RELEASE", RELEASE)
    script = make(release, monkeypatch, FakeRun())
    assert script.header_lines() == [RELEASE, "Java 17.0.12", "Maven 3.9.9"]


def test_the_version_checks_reuse_the_header_probe(release, monkeypatch):
    script = make(release, monkeypatch, FakeRun())
    monkeypatch.setattr(release.subprocess, "run", lambda *a, **k: pytest.fail(f"unexpected subprocess {a}"))
    script.check_java_version()
    assert script.summary == "Java 17.0.12"
    script.check_maven_version()
    assert script.summary == "Maven 3.9.9"


def test_an_old_java_fails_the_step(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    script._versions = ('openjdk version "11.0.2" 2019-01-15\n', MAVEN_TEXT)
    with pytest.raises(SystemExit):
        script.check_java_version()
    assert fake.of("failure")[0][0][0] == "❌ Java version 11 detected. Java 17 or higher is required."


def test_run_git_diff_pages_the_diff_against_the_tag(release, monkeypatch, tmp_path):
    clone = tmp_path / "staging" / "git" / "juneau"
    clone.mkdir(parents=True)

    def git(*args):
        subprocess.run(["git", "-c", "user.name=t", "-c", "user.email=t@example.org", "-c", "commit.gpgsign=false",
                        *args], cwd=clone, check=True, capture_output=True)

    git("init", "-q")
    (clone / "pom.xml").write_text("<version>9.9.9</version>\n")
    git("add", "pom.xml")
    git("commit", "-qm", "release")
    git("tag", RELEASE)
    (clone / "pom.xml").write_text("<version>9.9.10-SNAPSHOT</version>\n")
    monkeypatch.setenv("X_STAGING", str(tmp_path / "staging"))
    monkeypatch.setenv("X_RELEASE", RELEASE)
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    script.run_git_diff()
    [(args, kwargs)] = fake.of("show")
    assert kwargs == {"pager": True}
    assert "+<version>9.9.10-SNAPSHOT</version>" in args[0]
    assert script.summary == "1 file(s)"

    git("commit", "-qam", "next")
    git("tag", "-f", RELEASE)
    script.run_git_diff()
    assert fake.of("show")[-1] == ((f"No differences against {RELEASE}.",), {"pager": True})
    assert script.summary == "no changes"


def test_a_failing_git_diff_is_a_warning_not_no_differences(release, monkeypatch, tmp_path):
    monkeypatch.setenv("X_STAGING", str(tmp_path / "staging"))
    monkeypatch.setenv("X_RELEASE", RELEASE)
    monkeypatch.setattr(release.subprocess, "run",
                        lambda cmd, **k: subprocess.CompletedProcess(cmd, 128, "", "fatal: bad revision\n"))
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    script.run_git_diff()
    assert fake.of("say")[0][1] == {"level": "warn"} and "failed (exit 128)" in fake.of("say")[0][0][0]
    assert "No differences" not in "".join(args[0] for args, _ in fake.of("show"))
    assert script.summary == "git diff failed (exit 128)"


def test_old_dist_entries_are_removed_in_one_svn_rm(release, monkeypatch, tmp_path):
    dist = tmp_path / "dist"
    for path in ("source/juneau-9.9.8-RC1", "source/juneau-9.9.8-RC2", "binaries/juneau-9.9.8-RC2", "source/.svn"):
        (dist / path).mkdir(parents=True)
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    assert script._svn_rm_contents(dist, dist / "source", dist / "binaries", dist / "missing") == 3
    assert [args[0] for args, _ in fake.of("run_tool")] == [
        ["svn", "rm", "source/juneau-9.9.8-RC1", "source/juneau-9.9.8-RC2", "binaries/juneau-9.9.8-RC2"]]


def test_nothing_is_svn_rm_ed_from_empty_directories(release, monkeypatch, tmp_path):
    (tmp_path / "dist" / "source").mkdir(parents=True)
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    assert script._svn_rm_contents(tmp_path / "dist", tmp_path / "dist" / "source") == 0
    assert fake.of("run_tool") == []


@pytest.mark.parametrize("old, summary", [(2, f"committed {RELEASE} to dist/dev \u00b7 removed 2 old"),
                                          (0, f"committed {RELEASE} to dist/dev")])
def test_create_binary_artifacts_removes_old_source_and_binaries(release, monkeypatch, tmp_path, old, summary):
    staging = tmp_path / "staging"
    monkeypatch.setenv("X_STAGING", str(staging))
    monkeypatch.setenv("X_VERSION", "9.9.9")
    monkeypatch.setenv("X_RELEASE", RELEASE)
    monkeypatch.setenv("X_REPO", "orgapachejuneau-1000")
    script = make(release, monkeypatch, FakeRun())
    commands = []

    def run_command(cmd, cwd=None, check=True, **kwargs):
        commands.append(cmd)
        if cmd[:2] == ["svn", "checkout"]:
            for sub in ("source", "binaries"):
                (staging / "dist" / sub).mkdir(parents=True)
            for sub, name in (("source", "old-a"), ("binaries", "old-b"))[:old]:
                (staging / "dist" / sub / name).mkdir()
        return SimpleNamespace(returncode=0)

    monkeypatch.setattr(script, "run_command", run_command)
    rm_calls = []
    real = script._svn_rm_contents
    monkeypatch.setattr(script, "_svn_rm_contents", lambda dist, *dirs: rm_calls.append(dirs) or real(dist, *dirs))
    script.create_binary_artifacts()
    assert rm_calls == [(staging / "dist" / "source", staging / "dist" / "binaries")]
    assert script.summary == summary


def test_create_test_workspace_is_gone(release):
    assert not hasattr(release.ReleaseScript, "create_test_workspace")
    assert "create_test_workspace" not in (SCRIPTS_DIR / "release.py").read_text(encoding="utf-8")


def test_status_text_goes_through_say_and_show():
    assert calls_to("print") == []


def test_revert_is_four_rows_and_skips_what_is_not_there(release, monkeypatch, tmp_path):
    monkeypatch.setenv("X_STAGING", str(tmp_path / "staging"))
    monkeypatch.setenv("X_VERSION", "9.9.9")
    monkeypatch.setenv("X_RELEASE", RELEASE)
    fake = FakeRun(answers=["y"])
    script = make(release, monkeypatch, fake)
    script.run_revert()
    assert [row[1:4] for row in fake.rows] == [("revert_pull", "Pull", "skip"), ("revert_tag", "Tag", "skip"),
                                               ("revert_versions", "Versions", "skip"), ("revert_svn", "SVN", "skip")]
    assert fake.rows[3][4] == f"no checkout at {tmp_path / 'staging' / 'dist'}"
    assert fake.of("done") == [(("ok",), {})]


def test_revert_removes_the_rc_directories_from_svn(release, monkeypatch, tmp_path):
    dist = tmp_path / "staging" / "dist"
    for path in (".svn", f"binaries/{RELEASE}", f"source/{RELEASE}", "source/KEYS-dir-without-rc"):
        (dist / path).mkdir(parents=True)
    monkeypatch.setenv("X_STAGING", str(tmp_path / "staging"))
    monkeypatch.setenv("X_VERSION", "9.9.9")
    monkeypatch.setenv("X_RELEASE", RELEASE)
    status = f"D       binaries/{RELEASE}\nD       source/{RELEASE}\n"
    monkeypatch.setattr(release.subprocess, "run", lambda cmd, **k: subprocess.CompletedProcess(cmd, 0, status, ""))
    fake = FakeRun(answers=["y", "y"])
    script = make(release, monkeypatch, fake)
    script.run_revert()
    assert [args[0] for args, _ in fake.of("run_tool")] == [
        ["svn", "update"], ["svn", "rm", f"binaries/{RELEASE}"], ["svn", "rm", f"source/{RELEASE}"],
        ["svn", "commit", "-m", f"Remove {RELEASE} release candidate"]]
    assert fake.of("show")[0][0][0] == f"SVN changes ready to commit:\n{status}"
    assert fake.rows[3][1:] == ("revert_svn", "SVN", "ok", "2 removed and committed")


def test_an_unexpected_exception_in_a_step_is_reported_with_the_resume_hint(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)

    def boom():
        raise RuntimeError("boom")

    stub_steps(script, monkeypatch, {"run_deploy": boom})
    with pytest.raises(SystemExit) as exit_info:
        script.run()
    assert exit_info.value.code == 1
    assert not [args for args, _ in fake.of("say") if "ERROR" in args[0]]   # reported once, as the failure below
    assert fake.of("failure") == [(("\u274c Error in step run_deploy: boom",), {"step": "run_deploy"})]
    assert hint_lines(fake, "run_deploy") == [("say", "error")]
    assert fake.of("done") == [(("fail",), {})]


def test_start_step_shows_the_earlier_steps_as_done_earlier(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake, start_step="run_deploy")
    ran = stub_steps(script, monkeypatch)
    script.run()
    assert ran == RELEASE_STEPS[8:]
    assert fake.rows[:8] == [("step", name, release.STEP_TITLES[name][0], "skip", "(done earlier)")
                             for name in RELEASE_STEPS[:8]]


@pytest.mark.parametrize("missing", ["java", "mvn"])
def test_a_missing_java_or_maven_probes_as_empty(release, monkeypatch, missing):
    def run(cmd, **kwargs):
        if cmd[0] == missing:
            raise FileNotFoundError(cmd[0])
        return subprocess.CompletedProcess(cmd, 0, "", JAVA_TEXT if cmd[0] == "java" else MAVEN_TEXT)

    script = make(release, monkeypatch, FakeRun())
    script._versions = None
    monkeypatch.setattr(release.subprocess, "run", run)
    java, maven = script._probe_versions()
    assert (java == "") == (missing == "java") and (maven == "") == (missing == "mvn")
    assert script._probe_versions() is script._versions


# ---- the toolkit CLI (Registry), checkpoint and session plan --------------------------------------------------------
def test_resume_after_a_step_starts_at_the_next_and_shows_done_earlier_rows(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake, resume=True)
    script.state.set("last", "configure_git")
    stub_steps(script, monkeypatch)
    script.run()
    assert (("Resuming after configure_git: starting at run_clean_verify.",), {"level": "info"}) in fake.of("say")
    assert [row[4] for row in fake.rows[:7]] == ["(done earlier)"] * 7


def test_an_unknown_start_step_is_a_usage_error(release, monkeypatch, capsys):
    install(monkeypatch, FakeRun())
    monkeypatch.setattr(release, "ReleaseScript", lambda *a, **k: pytest.fail("no release for a usage error"))
    with pytest.raises(SystemExit) as exit_info:
        release.main(["--start-step", "no_such_step"])
    assert exit_info.value.code == 2
    assert "invalid choice: 'no_such_step'" in capsys.readouterr().err


def test_start_step_and_resume_are_mutually_exclusive(release, monkeypatch, capsys):
    install(monkeypatch, FakeRun())
    with pytest.raises(SystemExit) as exit_info:
        release.main(["--start-step", "run_deploy", "--resume"])
    assert exit_info.value.code == 2
    assert "not allowed with argument" in capsys.readouterr().err


def test_list_steps_uses_the_registry_format(release, monkeypatch):
    fake = install(monkeypatch, FakeRun())
    release.main(["--list-steps"])
    lines = [args[0] for args, _ in fake.of("say")]
    assert lines[0] == " 1. check_prerequisites      Checking prerequisites"
    assert lines[9] == "10. run_release_prepare      Running release:prepare"


def test_state_file_keys(release, monkeypatch):
    assert release.STATE_FILE == Path.home() / ".juneau-release-state.json"
    script = make(release, monkeypatch, FakeRun())
    stub_steps(script, monkeypatch, {"check_java_version": lambda: script.fail("old java")})
    with pytest.raises(SystemExit):
        script.run()
    import json
    assert json.loads(release.STATE_FILE.read_text(encoding="utf-8")) == {"last": "check_prerequisites"}


def test_registry_calls_the_method_looked_up_at_run_time(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)
    registry = script.registry()
    ran = stub_steps(script, monkeypatch)   # after the registry is built
    assert registry.run(script.selection(), None, show_unselected=True).ok
    assert ran == RELEASE_STEPS


def test_a_step_summary_is_its_row_text(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake)

    def deploy():
        script.summary = "deployed"

    stub_steps(script, monkeypatch, {"run_deploy": deploy})
    script.run()
    assert ("step", "run_deploy", "Deploy", "ok", "deployed") in fake.rows


def test_checkpoint_records_each_finished_step_as_last(release, monkeypatch):
    script = make(release, monkeypatch, FakeRun())
    seen = []
    behaviour = {name: (lambda: seen.append(script.state.last())) for name in RELEASE_STEPS[1:4]}
    behaviour["run_deploy"] = lambda: script.fail("deploy failed")
    stub_steps(script, monkeypatch, behaviour)
    with pytest.raises(SystemExit):
        script.run()
    assert seen == RELEASE_STEPS[0:3]
    assert script.state.last() == "run_clean_verify"


def test_a_legacy_last_step_state_file_still_resumes(release, monkeypatch):
    import json
    release.STATE_FILE.write_text(json.dumps({"last_step": "configure_git", "X_RELEASE": RELEASE}), encoding="utf-8")
    fake = FakeRun()
    script = make(release, monkeypatch, fake, resume=True)
    ran = stub_steps(script, monkeypatch)
    script.run()
    assert ran == RELEASE_STEPS[7:]
    assert (("Resuming after configure_git: starting at run_clean_verify.",), {"level": "info"}) in fake.of("say")


def test_success_clears_the_checkpoint(release, monkeypatch):
    script = make(release, monkeypatch, FakeRun())
    script.state.set("X_RELEASE", RELEASE)
    stub_steps(script, monkeypatch)
    script.run()
    assert not release.STATE_FILE.exists()


def test_a_corrupt_checkpoint_warns_and_fails(release, monkeypatch):
    release.STATE_FILE.write_text("{not json", encoding="utf-8")
    fake = install(monkeypatch, FakeRun())
    with pytest.raises(SystemExit) as exit_info:
        release.ReleaseScript(load_env=False)
    assert exit_info.value.code == 1
    [(args, kwargs)] = fake.of("say")
    assert args[0].startswith("Warning: Could not load state file:") and kwargs == {"level": "warn"}
    assert release.STATE_FILE.read_text(encoding="utf-8") == "{not json"   # never cleared on a guess


def test_an_unwritable_checkpoint_during_migration_warns_and_fails(release, monkeypatch):
    """Migrating a legacy last_step file writes it back; if that write fails it is the corrupt-file path: a warning and
    exit 1 before the session, with the file left as it was."""
    release.STATE_FILE.write_text(json.dumps({"last_step": "clone_juneau"}), encoding="utf-8")
    directory = release.STATE_FILE.parent
    mode = directory.stat().st_mode
    directory.chmod(0o500)
    try:
        fake = install(monkeypatch, FakeRun())
        with pytest.raises(SystemExit) as exit_info:
            release.ReleaseScript(load_env=False)
    finally:
        directory.chmod(mode)
    assert exit_info.value.code == 1
    [(args, kwargs)] = fake.of("say")
    assert args[0].startswith("Warning: Could not load state file:") and kwargs == {"level": "warn"}
    assert json.loads(release.STATE_FILE.read_text(encoding="utf-8")) == {"last_step": "clone_juneau"}
    assert fake.of("session") == []


def test_release_steps_are_numbered_from_one_in_order_even_when_started_later(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake, start_step="clone_juneau")
    stub_steps(script, monkeypatch)
    script.run()
    assert fake.numbers == [(name, n) for n, name in enumerate(RELEASE_STEPS, 1)]


def test_release_session_plan_is_the_selected_steps(release, monkeypatch):
    fake = FakeRun()
    script = make(release, monkeypatch, fake, start_step="clone_juneau", skip_steps=["run_deploy"])
    stub_steps(script, monkeypatch)
    script.run()
    assert fake.of("plan") == [(([n for n in RELEASE_STEPS[5:] if n != "run_deploy"],), {})]


def test_revert_session_plan_is_its_four_rows(release, monkeypatch, tmp_path):
    monkeypatch.setenv("X_STAGING", str(tmp_path / "staging"))
    monkeypatch.setenv("X_VERSION", "9.9.9")
    monkeypatch.setenv("X_RELEASE", RELEASE)
    fake = FakeRun(answers=["y"])
    script = make(release, monkeypatch, fake)
    script.run_revert()
    assert fake.of("plan") == [((["revert_pull", "revert_tag", "revert_versions", "revert_svn"],), {})]


def test_yprompt_ctrl_d_is_a_cancel_today(release, monkeypatch):
    monkeypatch.setenv("X_VERSION", "9.9.9")

    class EofRun(FakeRun):
        def ask(self, prompt):
            if not self.answers:
                self._record("ask", prompt)
                raise EOFError
            return super().ask(prompt)

    fake = EofRun(answers=["4"])
    install(monkeypatch, fake)
    with pytest.raises(SystemExit) as exit_info:
        release.main(["--revert"])
    assert exit_info.value.code == 130
    assert fake.of("ask")[-1][0][0].startswith("Are you sure you want to revert release juneau-9.9.9-RC4?")
    assert fake.of("run_tool") == []
    assert fake.of("done") == [(("cancelled",), {})]


def test_the_release_session_never_rings_the_bell_or_plays_a_sound(release, monkeypatch):
    script = make(release, monkeypatch, FakeRun())
    stub_steps(script, monkeypatch)
    notify = release.toolkit("notify")
    monkeypatch.setattr(notify, "bell", lambda **k: pytest.fail("bell"))
    monkeypatch.setattr(notify, "sound", lambda *a, **k: pytest.fail("sound"))
    script.run()
