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
import json
import os

import pytest

import release  # scripts/release.py, importable via conftest's sys.path

pytestmark = [pytest.mark.usefixtures("clean_env"),
              pytest.mark.skipif(not os.environ.get("JUNEAU_TERMINAL_PY"), reason="needs JUNEAU_TERMINAL_PY toolkit")]


def run_main(argv, home, capsys, deps=None):
    code = release.main(["--home", str(home), *argv], deps=deps)
    cap = capsys.readouterr()
    return code, cap.out + cap.err


def seed(home, data):
    p = home / "runs" / "9.2.1" / "run.json"
    p.parent.mkdir(parents=True)
    p.write_text(json.dumps({"schema": 1, **data}))


def test_list_steps_phased_with_done_marks(home, capsys):
    seed(home, {"phase": "rc", "last": "build-verify", "details": {"version": "9.2.1", "rc": 1}})
    code, out = run_main(["--list-steps", "--version", "9.2.1"], home, capsys)
    assert code == 0
    assert "Release candidate" in out
    assert "Vote" in out
    assert "Promote" in out
    assert "✔" in out.split("build-verify")[0].splitlines()[-1] + "build-verify"
    assert out.index("prerequisites") < out.index("prechecks") < out.index("vote-tally") < out.index("finalize")


def test_exit_codes(home, capsys):
    assert run_main(["--list-steps"], home, capsys)[0] == 0
    assert run_main(["--resume", "--start-step", "workspace"], home, capsys)[0] == 2   # mutually exclusive
    assert run_main(["--start-step", "nonsense"], home, capsys)[0] == 2


class RunProxy:
    """The real juneau_terminal_scripts.run for session/say, with command execution replaced by a FakeRunner."""

    child_env = None

    def __init__(self, real, fake):
        self._real, self.cmd, self.quiet_cmd = real, fake.cmd, fake.quiet_cmd

    def __getattr__(self, name):
        return getattr(self._real, name)


def toolkit_deps(tmp_path, fake, prompts, **kw):
    import bootstrap
    from conftest import FakeRunner
    kw.setdefault("http_text", lambda url: "pub   rsa4096 2020-01-01 [SC]\n      " + FakeRunner.KEY_FPR + "\n")
    bootstrap.load(release.REPO_DIR / "pom.xml", terminal=False, cache_dir=tmp_path / "tk")
    from juneau_terminal_scripts import lock, run, steps
    return release.Deps(RunProxy(run, fake), steps, prompts, lock, **kw)


ENV_SECRETS = {"JRM_APACHE_PASSWORD": "s3cr3t-pw", "JRM_GPG_PASSPHRASE": "s3cr3t-gpg", "JRM_GITHUB_TOKEN": "s3cr3t-gh"}
IDENTITY = {"apacheId": "jamesbognar", "apacheEmail": "jamesbognar@apache.org", "gpgKeyId": "ABCD1234ABCD1234"}


def params_file(home, version="9.2.1"):
    from test_release_params import GOOD
    p = home / "params" / f"{version}.yml"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(GOOD)
    return p


def new_run_setup(tmp_path, monkeypatch, answers):
    from conftest import CREDS, FakePrompts, FakeNexus, FakeRunner
    from jrm_release import preflight
    monkeypatch.setattr(preflight.shutil, "which", lambda n: "/usr/bin/" + n)
    for k, v in ENV_SECRETS.items():
        monkeypatch.setenv(k, v)
    fake, prompts = FakeRunner(), FakePrompts({**CREDS, **answers})
    fake.script("git", "ls-remote", release.inventory.GIT_URL, "refs/heads/juneau-9.2.1-branch", out="abc\trefs/heads/x\n")
    return fake, prompts, toolkit_deps(tmp_path, fake, prompts, nexus=FakeNexus(), http_ok=lambda u: True,
                                       sleep=lambda s: None)


def test_new_run_with_a_params_file_prechecks_without_any_prompt(home, capsys, tmp_path, monkeypatch):
    params_file(home)
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {"email.propose": None})
    code, out = run_main(["--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 130, out                     # cancelling the propose email ends the run
    assert [a[1] for a in prompts.asked] == ["email.propose"]
    assert "RC tag absent" in out


def test_new_run_with_a_missing_tool_lists_it_and_asks_nothing(home, capsys, tmp_path, monkeypatch):
    from jrm_release import preflight
    params_file(home)
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {})
    monkeypatch.setattr(preflight.shutil, "which", lambda n: None if n in ("svn", "svnmucc") else "/usr/bin/" + n)
    code, out = run_main(["--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 2
    assert prompts.asked == []
    assert "svn: not on PATH" in out
    assert "svnmucc: not on PATH" in out
    assert "brew install subversion" in out
    assert not (home / "runs").exists()


def test_resume_checks_prerequisites_before_any_prompt(home, capsys, tmp_path, monkeypatch):
    from jrm_release import preflight
    seed(home, {"phase": "rc", "last": "workspace", "identity": IDENTITY,
                "details": {"version": "9.2.1", "rc": 1, "branch": "b"}})
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {})
    monkeypatch.setattr(preflight.shutil, "which", lambda n: None if n == "gh" else "/usr/bin/" + n)
    code, out = run_main(["--resume"], home, capsys, deps=deps)
    assert code == 2
    assert prompts.asked == []
    assert "gh: not on PATH" in out
    assert "brew install gh" in out


def test_start_step_nonexistent_prechecks_move_with_prerequisites_first():
    assert release.inventory.NAMES[:3] == ["prerequisites", "prechecks", "compose-propose-email"]


def test_refuses_new_run_when_active(home, capsys):
    seed(home, {"phase": "rc", "last": "workspace", "details": {"version": "9.2.1", "rc": 1}})
    code, out = run_main([], home, capsys)
    assert code == 2
    assert "--resume" in out
    assert "9.2.1" in out


def test_resume_without_run_fails(home, capsys):
    code, out = run_main(["--resume"], home, capsys)
    assert code == 2
    assert "No active release" in out


def test_resume_after_last(home, capsys):
    seed(home, {"phase": "rc", "last": "workspace", "details": {"version": "9.2.1", "rc": 1, "branch": "b"}})
    assert release.plan_for(home, resume=True)[0] == "build-verify"
    assert release.plan_for(home, start="vote-tally")[0] == "vote-tally"


def test_start_step_refuses_before_irreversible(home):
    data = {"phase": "promote", "last": "nexus-release", "details": {"version": "9.2.1", "rc": 1}}
    assert "nexus-release" in release.guard_start("build-verify", data)
    assert release.guard_start("dist-promote", data) is None


def test_start_step_refuses_promote_without_passed_vote(home):
    data = {"phase": "vote", "last": "vote-gate", "details": {"version": "9.2.1", "rc": 1}}
    assert "vote" in release.guard_start("nexus-release", data)
    assert release.guard_start("nexus-release", {**data, "vote": {"outcome": "passed"}}) is None
    assert "vote-gate" in release.guard_start("vote-tally", {"phase": "rc", "last": "dist-verify"})


def test_start_step_refusal_exits_2(home, capsys):
    seed(home, {"phase": "promote", "last": "nexus-release", "vote": {"outcome": "passed"},
                "details": {"version": "9.2.1", "rc": 1}})
    code, out = run_main(["--start-step", "build-verify"], home, capsys)
    assert code == 2
    assert "nexus-release" in out


def test_skip_step_only_skippable(home, capsys):
    code, out = run_main(["--skip-step", "build-verify"], home, capsys)
    assert code == 2
    assert "javadoc-verify" in out


def test_lock_busy_message(home):
    assert release.lock_busy_message(pid=4242, attempt=3) == \
        "Another release.py is running this release (pid 4242, attempt #3)."


def test_second_process_is_refused_while_lock_held(home, capsys, tmp_path, monkeypatch):
    import fcntl
    from conftest import FakeNexus, FakePrompts, FakeRunner
    seed(home, {"phase": "rc", "last": "workspace", "identity": IDENTITY, "details": {"version": "9.2.1", "rc": 1, "branch": "b"}})
    lock_path = home / "runs" / "9.2.1" / ".lock"
    lock_path.write_text("4242 3")
    from jrm_release import preflight
    monkeypatch.setattr(preflight.shutil, "which", lambda n: "/usr/bin/" + n)
    for k, v in ENV_SECRETS.items():
        monkeypatch.setenv(k, v)
    fd = os.open(lock_path, os.O_RDWR)
    fcntl.flock(fd, fcntl.LOCK_EX)
    try:
        deps = toolkit_deps(tmp_path, FakeRunner(), FakePrompts(), nexus=FakeNexus())
        code, out = run_main(["--resume"], home, capsys, deps=deps)
    finally:
        os.close(fd)
    assert code == 1, out
    assert "Another release.py is running this release (pid 4242, attempt #3)." in out


def setup_deps(tmp_path, monkeypatch, answers, opener=None):
    """(fake runner, prompts, nexus fake, deps).  With an opener the real Nexus client is used over it."""
    from conftest import CREDS, FakeNexus, FakePrompts, FakeRunner
    from jrm_release import preflight
    monkeypatch.setattr(preflight.shutil, "which", lambda n: "/usr/bin/" + n)
    for k, v in ENV_SECRETS.items():
        monkeypatch.setenv(k, v)
    fake, prompts, nexus = FakeRunner(), FakePrompts({**CREDS, **answers}), FakeNexus()
    deps = toolkit_deps(tmp_path, fake, prompts, nexus=None if opener else nexus, nexus_opener=opener,
                        http_ok=lambda u: True, sleep=lambda s: None)
    return fake, prompts, nexus, deps


def read_run(home):
    return json.loads((home / "runs" / "9.2.1" / "run.json").read_text())


ACTIVE = {"phase": "rc", "last": "nexus-close", "nexusRepoId": "r-1", "rcCommit": "abc123", "identity": IDENTITY,
          "details": {"version": "9.2.1", "developmentVersion": "9.2.2-SNAPSHOT", "rc": 1,
                      "branch": "juneau-9.2.1-branch", "summary": "x"}}


def test_revert_end_to_end(home, capsys, tmp_path, monkeypatch):
    seed(home, ACTIVE)
    fake, prompts, nexus, deps = setup_deps(tmp_path, monkeypatch, {"confirm.revert": True,
                                                                    "confirm.revert-versions": False})
    code, out = run_main(["--revert"], home, capsys, deps=deps)
    assert code == 0, out
    assert ("drop", "r-1") in nexus.posts
    data = read_run(home)
    assert data["details"]["rc"] == 2
    assert data["phase"] == "rc"
    assert "last" not in data
    assert data["attempts"][-1]["status"] == "ok"
    assert data["attempts"][-1]["command"] == "revert"


def test_revert_refused_in_promote_exits_1(home, capsys, tmp_path, monkeypatch):
    seed(home, {**ACTIVE, "phase": "promote"})
    fake, prompts, nexus, deps = setup_deps(tmp_path, monkeypatch, {})
    code, out = run_main(["--revert"], home, capsys, deps=deps)
    assert code == 1
    assert "public" in out
    assert nexus.posts == []


def test_cancel_exits_130(home, capsys, tmp_path, monkeypatch):
    seed(home, ACTIVE)
    fake, prompts, nexus, deps = setup_deps(tmp_path, monkeypatch, {"confirm.revert": None})
    prompts.confirm = lambda *a, **k: (_ for _ in ()).throw(KeyboardInterrupt())
    code, out = run_main(["--revert"], home, capsys, deps=deps)
    assert code == 130
    assert read_run(home)["attempts"][-1]["status"] == "cancelled"


def test_resume_runs_to_vote_gate_and_pauses_with_exit_0(home, capsys, tmp_path, monkeypatch):
    seed(home, {**ACTIVE, "last": "dist-verify"})
    fake, prompts, nexus, deps = setup_deps(tmp_path, monkeypatch, {"email.vote": True})
    code, out = run_main(["--resume"], home, capsys, deps=deps)
    assert code == 0, out
    data = read_run(home)
    assert data["last"] == "vote-gate"
    assert data["phase"] == "vote"
    assert data["voteDeadline"]
    assert data["attempts"][-1]["status"] == "paused"
    assert "release.py --resume" in out
    # the lock file exists but is no longer held: a second run is not refused
    code2, out2 = run_main(["--resume"], home, capsys, deps=deps)
    assert "Another release.py" not in out2


def test_attempt_record_has_origin_pid_step_exit(home, capsys, tmp_path, monkeypatch):
    seed(home, {**ACTIVE, "last": "dist-verify"})
    fake, prompts, nexus, deps = setup_deps(tmp_path, monkeypatch, {"email.vote": True})
    code, out = run_main(["--resume"], home, capsys, deps=deps)
    att = read_run(home)["attempts"][-1]
    assert att["origin"] == "cli"            # no prompt channel in the test env
    assert isinstance(att["pid"], int)
    assert att["exit"] == code               # the exit code main() returned
    assert att["step"]                       # the step the attempt stopped in


def test_failed_step_exits_1_and_hints_resume(home, capsys, tmp_path, monkeypatch):
    seed(home, {**ACTIVE, "last": "workspace", "nexusRepoId": None})
    fake, prompts, nexus, deps = setup_deps(tmp_path, monkeypatch, {})
    fake.script("mvn", "-B", ok=False)
    code, out = run_main(["--resume"], home, capsys, deps=deps)
    assert code == 1
    assert "Build verify failed" in out
    assert "--start-step build-verify" in out
    assert read_run(home)["last"] == "workspace"
    assert read_run(home)["attempts"][-1]["status"] == "failed"


def test_dry_run_goes_through_every_step_to_vote_gate(home, capsys, tmp_path, monkeypatch):
    params_file(home)
    answers = {"email.propose": True, "review.javadoc": True, "review.diff": True, "email.vote": True}
    from conftest import FakeHttp
    http = FakeHttp({("GET", "/profile_repositories/1a24bc7f954a70"): (401, b"")})   # no password in a dry run
    fake, prompts, nexus, deps = setup_deps(tmp_path, monkeypatch, answers, opener=http)
    fake.script("git", "ls-remote", release.inventory.GIT_URL, "refs/heads/juneau-9.2.1-branch",
                out="abc\trefs/heads/x\n")
    code, out = run_main(["--dry-run", "--version", "9.2.1"], home, capsys, deps=deps)
    assert code == 0, out
    assert not (home / "runs").exists()                       # nothing persisted
    assert http.requests
    assert all(r[0] == "GET" for r in http.requests)  # no Nexus POST
    assert not fake.ran("svnmucc", "--non-interactive")
    assert not fake.ran("mvn", "-B")
    assert not fake.ran("gpg", "--batch")
    for word in ("would run: mvn -B clean verify", "would run: git clone", "would run: svnmucc"):
        assert word in out
    assert "s3cr3t-pw" not in out
    assert "s3cr3t-gpg" not in out
    asked = [a[1] for a in prompts.asked]
    assert "credential.apache.password" not in asked
    assert "credential.gpg.passphrase" not in asked
    assert "Vote is open" in out


def test_dry_run_creates_attempt_full_log_but_no_runs(home, capsys, tmp_path, monkeypatch):
    params_file(home)
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {"email.propose": True})
    run_main(["--dry-run", "--version", "9.2.1"], home, capsys, deps=deps)
    assert (home / "attempts" / "dry-run" / "full.log").exists()
    assert not (home / "runs").exists()


def test_dry_run_honors_a_caller_supplied_full_log(home, capsys, tmp_path, monkeypatch):
    params_file(home)
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {"email.propose": True})
    log = tmp_path / "watch" / "full.log"
    monkeypatch.setenv("JUNEAU_TERMINAL_FULL_LOG", str(log))
    seen = []
    orig = deps.run.session
    monkeypatch.setattr(deps.run, "session",
                        lambda *a, **kw: (seen.append(os.environ.get("JUNEAU_TERMINAL_FULL_LOG")), orig(*a, **kw))[1],
                        raising=False)
    run_main(["--dry-run", "--version", "9.2.1"], home, capsys, deps=deps)
    assert seen == [str(log)]                                           # the session is told to write the caller's file
    link = home / "attempts" / "dry-run" / "full.log"
    assert link.is_symlink() and link.resolve() == log.resolve()
    assert os.environ["JUNEAU_TERMINAL_FULL_LOG"] == str(log)           # restored on exit


def test_dry_run_without_a_caller_log_writes_attempt_full_log(home, capsys, tmp_path, monkeypatch):
    params_file(home)
    fake, prompts, deps = new_run_setup(tmp_path, monkeypatch, {"email.propose": True})
    monkeypatch.delenv("JUNEAU_TERMINAL_FULL_LOG", raising=False)
    seen = []
    orig = deps.run.session
    monkeypatch.setattr(deps.run, "session",
                        lambda *a, **kw: (seen.append(os.environ.get("JUNEAU_TERMINAL_FULL_LOG")), orig(*a, **kw))[1],
                        raising=False)
    run_main(["--dry-run", "--version", "9.2.1"], home, capsys, deps=deps)
    full = home / "attempts" / "dry-run" / "full.log"
    assert full.exists() and not full.is_symlink()
    assert seen == [str(full)]
    assert "JUNEAU_TERMINAL_FULL_LOG" not in os.environ


def test_version_flag_must_look_like_x_y_z(home, capsys):
    code, out = run_main(["--dry-run", "--version", "9.2"], home, capsys)
    assert code == 2
    assert "x.y.z" in out
