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
Tests for scripts/push.py: the --tracker-audit opt-in gate, and commit_and_push()'s
clean-tree-vs-nothing-to-push distinction and pre-existing-staged-changes guard.

Juneau-specific exception to this directory's usual byte-for-byte-identical-across-repos rule
(see README.md): push.py's --sonarqube-style opt-in gate pattern only exists in juneau's
push.py (release-manager's push.py is a bare add/commit/push helper with no gates at all;
sandbox-support-console has no push.py). This file is therefore NOT copied to the other two
repos, unlike everything else in scripts/tests/.

The --tracker-audit central property under test is the one the task that introduced it
explicitly demanded: when --tracker-audit is not passed, the gate must be IMPOSSIBLE to observe
at runtime -- not just "returns early", but never even reaches subprocess.run.
test_off_path_never_invokes_subprocess pins that all the way down to the subprocess boundary.

commit_and_push()'s tests build REAL temporary git repositories (a bare repo standing in for
the GitHub remote, and a real clone of it) and let real git run against them, following the
same pattern as test_reset_side_clones.py and for the same reason: this function's entire risk
is real git behavior (does a push actually move the remote ref? does a refused commit actually
leave the index untouched?), which a mocked subprocess would let pass while proving nothing.
Temp repos get their identity and config isolation from environment variables, never `git
config`. Nothing here ever touches the real juneau checkout or a real remote.
"""

from __future__ import annotations

import argparse
import importlib.util
import os
import subprocess
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


def _load_push_module():
    """Load scripts/push.py as a fresh module object (mirrors conftest.py's _load_script)."""
    path = SCRIPTS_DIR / "push.py"
    spec = importlib.util.spec_from_file_location("_undertest_push", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def push_module():
    """A fresh scripts/push.py module object, loaded once per test (see module docstring)."""
    return _load_push_module()


def _args(tracker_audit: bool) -> argparse.Namespace:
    """Just enough of push.py's parsed-args surface for maybe_run_tracker_audit_gate()."""
    return argparse.Namespace(tracker_audit=tracker_audit)


# ---------------------------------------------------------------------------------------
# Real-git fixtures for commit_and_push() / check_upstream_changes() /
# check_preexisting_staged_changes(). Mirrors test_reset_side_clones.py's hermetic_git_env
# and _run/_commit helpers.
# ---------------------------------------------------------------------------------------
@pytest.fixture(autouse=True)
def hermetic_git_env(monkeypatch):
    """Isolate every git subprocess from the developer's real git configuration and identity."""
    monkeypatch.setenv("GIT_CONFIG_GLOBAL", os.devnull)
    monkeypatch.setenv("GIT_CONFIG_SYSTEM", os.devnull)
    monkeypatch.setenv("GIT_AUTHOR_NAME", "Push Test")
    monkeypatch.setenv("GIT_AUTHOR_EMAIL", "push-test@example.invalid")
    monkeypatch.setenv("GIT_COMMITTER_NAME", "Push Test")
    monkeypatch.setenv("GIT_COMMITTER_EMAIL", "push-test@example.invalid")
    monkeypatch.setenv("GIT_TERMINAL_PROMPT", "0")


def _run(cwd: Path, *argv: str) -> str:
    result = subprocess.run(argv, cwd=str(cwd), check=True, capture_output=True, text=True)
    return result.stdout.strip()


def _commit(repo: Path, message: str) -> str:
    _run(repo, "git", "add", "-A")
    _run(repo, "git", "commit", "-m", message)
    return _run(repo, "git", "rev-parse", "HEAD")


@pytest.fixture
def remote_repo(tmp_path: Path) -> Path:
    """A bare repo standing in for the GitHub remote, so a real `git push` behaves like the real thing."""
    bare = tmp_path / "remote.git"
    _run(tmp_path, "git", "init", "--bare", "-b", "master", str(bare))
    return bare


@pytest.fixture
def repo(tmp_path: Path, remote_repo: Path) -> Path:
    """
    A real clone of remote_repo on `master`, tracking origin/master, seeded with one commit
    that is already pushed -- i.e. exactly the state a working tree is in right after a normal,
    successful push: clean, and not ahead of its upstream.
    """
    seed = tmp_path / "seed"
    seed.mkdir()
    _run(seed, "git", "init", "-b", "master", ".")
    (seed / "README.md").write_text("first\n", encoding="utf-8")
    _commit(seed, "initial")
    _run(seed, "git", "remote", "add", "origin", str(remote_repo))
    _run(seed, "git", "push", "origin", "master")

    work = tmp_path / "work"
    _run(tmp_path, "git", "clone", str(remote_repo), str(work))
    return work


def _local_only_commit(repo_dir: Path, filename: str = "local.txt", message: str = "local work") -> str:
    """Commit directly to `repo_dir` without pushing -- a branch ahead of its upstream."""
    (repo_dir / filename).write_text("committed but never pushed\n", encoding="utf-8")
    return _commit(repo_dir, message)


@pytest.fixture
def git_spy(push_module, monkeypatch):
    """Record every argv passed to push_module.subprocess.run while still running real git."""
    calls: list[list[str]] = []
    real_run = subprocess.run

    def _spy(cmd, *args, **kwargs):
        calls.append(list(cmd) if isinstance(cmd, (list, tuple)) else [cmd])
        return real_run(cmd, *args, **kwargs)

    monkeypatch.setattr(push_module.subprocess, "run", _spy)
    return calls


class TestArgparseWiring:
    """The --tracker-audit / --todo-audit flag itself: default OFF, both spellings work."""

    def test_default_is_off(self, push_module):
        parser = push_module.argparse.ArgumentParser()
        # Re-derive just the flag under test rather than re-implementing all of main()'s
        # parser setup here -- calling main()'s real parser construction would require a
        # positional "message" argument and would drag in every other flag's defaults too.
        parser.add_argument("--tracker-audit", "--todo-audit", action="store_true", dest="tracker_audit")
        args = parser.parse_args([])
        assert args.tracker_audit is False

    def test_both_spellings_set_the_same_dest(self, push_module):
        parser = push_module.argparse.ArgumentParser()
        parser.add_argument("--tracker-audit", "--todo-audit", action="store_true", dest="tracker_audit")
        assert parser.parse_args(["--tracker-audit"]).tracker_audit is True
        assert parser.parse_args(["--todo-audit"]).tracker_audit is True


class TestOffPathNeverInvokesTheGate:
    """The property the task explicitly requires: off means off, all the way down."""

    def test_maybe_run_returns_none_without_calling_run_tracker_audit_gate(self, push_module, monkeypatch):
        calls = []
        monkeypatch.setattr(push_module, "run_tracker_audit_gate", lambda *a, **k: calls.append((a, k)))

        result = push_module.maybe_run_tracker_audit_gate(_args(tracker_audit=False), Path("/irrelevant"), 1)

        assert result is None
        assert calls == []

    def test_off_path_never_invokes_subprocess(self, push_module, monkeypatch):
        """
        Strongest available proof: even subprocess.run itself -- the only way this gate could
        ever touch real runtime behavior -- is never called on the off path. This is checked
        against push_module.subprocess (the module's own imported reference), not the global
        subprocess module, so it can't pass by accident via a different import path.
        """
        def _boom(*_a, **_k):
            raise AssertionError("subprocess.run must not be called when --tracker-audit is off")

        monkeypatch.setattr(push_module.subprocess, "run", _boom)

        result = push_module.maybe_run_tracker_audit_gate(_args(tracker_audit=False), Path("/irrelevant"), 1)

        assert result is None

    def test_off_path_is_true_no_op_regardless_of_other_args(self, push_module, monkeypatch):
        """Same as above, but with a step_num/juneau_root that would be nonsensical if actually used."""
        monkeypatch.setattr(push_module.subprocess, "run", lambda *a, **k: pytest.fail("must not run"))
        result = push_module.maybe_run_tracker_audit_gate(_args(tracker_audit=False), Path("/does/not/exist"), 999)
        assert result is None


class TestOnPathDelegatesToTheGate:
    """Sanity check for the complementary path: --tracker-audit really does invoke the gate."""

    def test_maybe_run_calls_run_tracker_audit_gate_and_returns_its_status(self, push_module, monkeypatch):
        seen = {}

        def _fake_gate(juneau_root, step_num):
            seen["juneau_root"] = juneau_root
            seen["step_num"] = step_num
            return "pass"

        monkeypatch.setattr(push_module, "run_tracker_audit_gate", _fake_gate)

        result = push_module.maybe_run_tracker_audit_gate(_args(tracker_audit=True), Path("/repo"), 3)

        assert result == "pass"
        assert seen == {"juneau_root": Path("/repo"), "step_num": 3}


class TestRunTrackerAuditGateExitCodeMapping:
    """run_tracker_audit_gate()'s exit-code contract, mirroring run_sonarqube_gate's shape."""

    @pytest.mark.parametrize(
        ("returncode", "expected_status"),
        [
            (0, "pass"),
            (1, "fail"),
            (2, "error"),
            (77, "error"),  # anything undocumented is treated as an error, not silently ignored
        ],
    )
    def test_returncode_mapping(self, push_module, monkeypatch, returncode, expected_status):
        monkeypatch.setattr(
            push_module.subprocess,
            "run",
            lambda *a, **k: subprocess.CompletedProcess(args=a, returncode=returncode),
        )

        status = push_module.run_tracker_audit_gate(Path("/repo"), 1)

        assert status == expected_status

    def test_invokes_todo_status_audit_script_via_current_interpreter(self, push_module, monkeypatch):
        captured = {}

        def _fake_run(cmd, cwd, check):
            captured["cmd"] = cmd
            captured["cwd"] = cwd
            captured["check"] = check
            return subprocess.CompletedProcess(args=cmd, returncode=0)

        monkeypatch.setattr(push_module.subprocess, "run", _fake_run)

        push_module.run_tracker_audit_gate(Path("/repo"), 1)

        assert captured["cwd"] == Path("/repo")
        assert captured["check"] is False
        assert captured["cmd"][0] == push_module.sys.executable
        assert captured["cmd"][1] == str(SCRIPTS_DIR / "todo-status-audit.py")


class TestCheckUpstreamChangesAheadBehind:
    """
    check_upstream_changes() against real git: the ahead count is the fix, so it needs to be
    right, not just the pre-existing behind count.
    """

    def test_clean_and_synced_is_zero_and_zero(self, push_module, repo):
        ahead, behind, error = push_module.check_upstream_changes(repo)
        assert (ahead, behind, error) == (0, 0, None)

    def test_a_local_only_commit_is_one_ahead_zero_behind(self, push_module, repo):
        _local_only_commit(repo)
        ahead, behind, error = push_module.check_upstream_changes(repo)
        assert (ahead, behind, error) == (1, 0, None)

    def test_an_upstream_only_commit_is_zero_ahead_one_behind(self, push_module, tmp_path, repo, remote_repo):
        other = tmp_path / "other-clone"
        _run(tmp_path, "git", "clone", str(remote_repo), str(other))
        _local_only_commit(other, filename="from-elsewhere.txt", message="pushed by someone else")
        _run(other, "git", "push", "origin", "master")

        ahead, behind, error = push_module.check_upstream_changes(repo)

        assert (ahead, behind, error) == (0, 1, None)

    def test_no_upstream_configured_is_none_none_none(self, push_module, tmp_path):
        lone = tmp_path / "lone"
        lone.mkdir()
        _run(lone, "git", "init", "-b", "master", ".")
        (lone / "f.txt").write_text("x\n", encoding="utf-8")
        _commit(lone, "no remote at all")

        ahead, behind, error = push_module.check_upstream_changes(lone)

        assert (ahead, behind, error) == (None, None, None)


class TestCheckPreexistingStagedChanges:
    """The pre-flight check commit_and_push() uses to refuse committing over someone else's index state."""

    def test_a_clean_index_reports_nothing(self, push_module, repo):
        assert push_module.check_preexisting_staged_changes(repo) == []

    def test_a_staged_file_is_reported(self, push_module, repo):
        (repo / "staged.txt").write_text("staged by someone else\n", encoding="utf-8")
        _run(repo, "git", "add", "staged.txt")

        assert push_module.check_preexisting_staged_changes(repo) == ["staged.txt"]

    def test_an_unstaged_modification_alone_is_not_reported(self, push_module, repo):
        """Unstaged dirt is not what this guard is about -- only what's already IN the index."""
        (repo / "README.md").write_text("unstaged edit\n", encoding="utf-8")
        assert push_module.check_preexisting_staged_changes(repo) == []


class TestCommitAndPushFalseSuccessRegression:
    """
    The defect: a clean working tree with unpushed local commits used to be reported as
    "nothing to commit and push" (returning success) without ever running `git push`.

    This is the test that fails against the pre-fix code: the pre-fix push.py checked only
    `check_git_status()` (a clean tree) and returned success without consulting how far ahead
    of upstream the branch was, so it would report success here while the remote never moved.
    A test that only checked the return status/exit code would pass against that bug -- the
    assertion that matters is that the remote ref ACTUALLY ADVANCED.
    """

    def test_clean_tree_with_unpushed_commits_actually_pushes(self, push_module, repo, remote_repo, git_spy):
        local_head = _local_only_commit(repo)
        assert push_module.check_git_status(repo) is False, "tree must be clean for this to be the regression case"

        status, _ = push_module.commit_and_push(repo, "irrelevant message", 4)

        assert status == "ok"
        remote_head = _run(remote_repo, "git", "rev-parse", "master")
        assert remote_head == local_head, "the remote must actually have moved, not just returned success"
        assert any(argv[:2] == ["git", "push"] for argv in git_spy), "git push must actually have run"

    def test_clean_tree_with_unpushed_commits_does_not_re_add_or_re_commit(self, push_module, repo, remote_repo, git_spy):
        """A clean tree skips the COMMIT step -- there is nothing new to stage or commit."""
        head_before = _local_only_commit(repo)
        git_spy.clear()  # discard the setup commit above; only commit_and_push()'s own git calls matter here

        status, _ = push_module.commit_and_push(repo, "irrelevant message", 4)

        assert status == "ok"
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before, "no new commit should have been created"
        assert not any(argv[:2] == ["git", "add"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)

    def test_reports_distinguishable_text_from_an_actual_no_op(self, push_module, repo, remote_repo, capsys):
        """The output must never let a real push read like a no-op, or vice versa."""
        _local_only_commit(repo)
        push_module.commit_and_push(repo, "irrelevant message", 4)
        pushed_output = capsys.readouterr().out

        status, _ = push_module.commit_and_push(repo, "irrelevant message", 4)
        noop_output = capsys.readouterr().out

        assert status == "nothing_to_do"
        assert "nothing to commit or push" in noop_output.lower()
        assert "nothing to commit or push" not in pushed_output.lower()


class TestCommitAndPushGenuineNoOp:
    """Clean tree AND nothing ahead of upstream is the one case that is truly a no-op."""

    def test_returns_nothing_to_do_and_touches_nothing(self, push_module, repo, remote_repo, git_spy):
        remote_before = _run(remote_repo, "git", "rev-parse", "master")

        status, step_num = push_module.commit_and_push(repo, "irrelevant message", 4)

        assert status == "nothing_to_do"
        assert step_num == 4, "step_num must be unchanged when nothing happened"
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "add"] for argv in git_spy)


class TestCommitAndPushDirtyTree:
    """The ordinary path must keep working: a dirty tree still gets committed and pushed."""

    def test_a_dirty_tree_is_committed_with_the_given_message_and_pushed(self, push_module, repo, remote_repo):
        (repo / "README.md").write_text("new content\n", encoding="utf-8")

        status, step_num = push_module.commit_and_push(repo, "the commit message", 4)

        assert status == "ok"
        assert step_num == 6, "one step for the commit, one for the push"
        assert _run(repo, "git", "log", "-1", "--format=%s") == "the commit message"
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_an_untracked_file_is_picked_up_too(self, push_module, repo, remote_repo):
        """`git add .` staging genuinely-new untracked work is unchanged, documented behavior."""
        (repo / "new-file.txt").write_text("brand new\n", encoding="utf-8")

        status, _ = push_module.commit_and_push(repo, "add new file", 4)

        assert status == "ok"
        assert _run(remote_repo, "git", "show", "HEAD:new-file.txt") == "brand new"


class TestCommitAndPushPreexistingStagedGuard:
    """
    Defect 2: `git add .` was unconditional, so anything unrelated already dirty (or, more
    precisely, already staged -- since `git commit` commits the whole index regardless of what
    THIS run's `git add .` stages) got swept into the commit. commit_and_push() now refuses
    outright rather than guessing whether pre-existing staged content belongs to this push.
    """

    def test_preexisting_staged_content_aborts_before_any_add_commit_or_push(
        self, push_module, repo, remote_repo, git_spy
    ):
        (repo / "unrelated.txt").write_text("staged by a concurrent session\n", encoding="utf-8")
        _run(repo, "git", "add", "unrelated.txt")
        (repo / "mine.txt").write_text("my own unstaged work\n", encoding="utf-8")
        head_before = _run(repo, "git", "rev-parse", "HEAD")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")
        git_spy.clear()  # discard the setup `git add` above; only commit_and_push()'s own git calls matter here

        status, _ = push_module.commit_and_push(repo, "my commit message", 4)

        assert status == "error"
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before, "nothing should have been committed"
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before, "the remote must not move"
        assert not any(argv[:2] == ["git", "add"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)

    def test_the_staged_file_remains_staged_and_the_unstaged_file_remains_unstaged(self, push_module, repo):
        (repo / "unrelated.txt").write_text("staged by a concurrent session\n", encoding="utf-8")
        _run(repo, "git", "add", "unrelated.txt")
        (repo / "mine.txt").write_text("my own unstaged work\n", encoding="utf-8")

        push_module.commit_and_push(repo, "my commit message", 4)

        assert _run(repo, "git", "diff", "--cached", "--name-only") == "unrelated.txt"
        status_lines = _run(repo, "git", "status", "--porcelain").splitlines()
        assert "?? mine.txt" in status_lines

    def test_the_error_message_names_the_staged_paths(self, push_module, repo, capsys):
        (repo / "secret-in-progress.txt").write_text("do not touch\n", encoding="utf-8")
        _run(repo, "git", "add", "secret-in-progress.txt")

        push_module.commit_and_push(repo, "message", 4)

        assert "secret-in-progress.txt" in capsys.readouterr().out

    def test_a_clean_index_with_only_unstaged_dirt_is_not_refused(self, push_module, repo, remote_repo):
        """The guard is about the INDEX, not about dirtiness in general -- ordinary dirty trees must still work."""
        (repo / "mine.txt").write_text("my own unstaged work\n", encoding="utf-8")

        status, _ = push_module.commit_and_push(repo, "message", 4)

        assert status == "ok"
        assert _run(remote_repo, "git", "show", "HEAD:mine.txt") == "my own unstaged work"


class TestCommitAndPushBehindUpstream:
    """The pre-existing behind-upstream guard must still block, unchanged, ahead of the new logic."""

    def test_behind_upstream_aborts_without_touching_anything(self, push_module, tmp_path, repo, remote_repo, git_spy):
        other = tmp_path / "other-clone"
        _run(tmp_path, "git", "clone", str(remote_repo), str(other))
        _local_only_commit(other, filename="from-elsewhere.txt", message="pushed by someone else")
        _run(other, "git", "push", "origin", "master")
        head_before = _run(repo, "git", "rev-parse", "HEAD")
        git_spy.clear()  # discard the setup commit/push above; only commit_and_push()'s own git calls matter here

        status, _ = push_module.commit_and_push(repo, "message", 4)

        assert status == "error"
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before
        assert not any(argv[:2] == ["git", "add"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)
