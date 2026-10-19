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
nothing-staged-vs-nothing-to-push distinction and unreviewed-changes guard (refuses to commit
or push while anything in the tree is unstaged or untracked-and-not-gitignored -- staging is
the operator's record of having reviewed a path, so this script commits the index exactly as
it finds it and never runs `git add .`).

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
same pattern as the board repo's reset-side-clones tests and for the same reason: this function's entire risk
is real git behavior (does a push actually move the remote ref? does a refused commit actually
leave the index untouched?), which a mocked subprocess would let pass while proving nothing.
Temp repos get their identity and config isolation from environment variables, never `git
config`. Nothing here ever touches the real juneau checkout or a real remote.
"""

from __future__ import annotations

import argparse
import builtins
import hashlib
import importlib.util
import inspect
import io
import os
import pathlib
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
# Real-git fixtures for commit_and_push() / prechecks() / get_staged_paths() /
# check_unreviewed_changes(). Mirrors the board repo's reset-side-clones tests' hermetic_git_env and
# _run/_commit helpers.
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
def docs_remote_repo(tmp_path: Path) -> Path:
    """
    A SEPARATE bare repo standing in for the juneau-docs remote -- deliberately distinct from
    remote_repo/repo (the juneau ones), since run_docs_only()/run_docs_followup() push to the
    juneau-docs sibling's own remote, not juneau's. A test that only reused the juneau fixtures
    unchanged could pass by accident even if a fix wired the wrong repo_dir/remote together.
    """
    bare = tmp_path / "docs-remote.git"
    _run(tmp_path, "git", "init", "--bare", "-b", "master", str(bare))
    return bare


@pytest.fixture
def docs_repo(tmp_path: Path, docs_remote_repo: Path, push_module) -> Path:
    """
    A real clone of docs_remote_repo, seeded with a stub scripts/build-docs.py that exits 0 (so
    tests can exercise the real pre_commit_hook smoke-check wiring without a real Docusaurus
    site), living at <tmp_path>/juneau-docs so juneau_root.parent / "juneau-docs" resolves to
    it. Also configures the repo-local (never global) git identity the docs Prechecks' identity
    check reads, so tests that run them see it satisfied.
    """
    seed = tmp_path / "docs-seed"
    seed.mkdir()
    _run(seed, "git", "init", "-b", "master", ".")
    (seed / "README.md").write_text("docs first\n", encoding="utf-8")
    (seed / "scripts").mkdir()
    (seed / "scripts" / "build-docs.py").write_text("import sys\nsys.exit(0)\n", encoding="utf-8")
    _commit(seed, "initial docs")
    _run(seed, "git", "remote", "add", "origin", str(docs_remote_repo))
    _run(seed, "git", "push", "origin", "master")

    work = tmp_path / "juneau-docs"
    _run(tmp_path, "git", "clone", str(docs_remote_repo), str(work))
    _run(work, "git", "config", "user.email", push_module.REQUIRED_GIT_EMAIL)
    _run(work, "git", "config", "user.name", "Push Test")
    return work


@pytest.fixture
def juneau_root(tmp_path: Path, docs_repo: Path) -> Path:
    """
    A juneau_root whose PARENT holds docs_repo at `<parent>/juneau-docs` -- the sibling-checkout
    layout run_docs_only()/run_docs_followup() expect. Does not need to exist itself: neither
    function dereferences juneau_root directly, only juneau_root.parent.
    """
    return tmp_path / "juneau"


def _docs_args(message: str = "docs message") -> argparse.Namespace:
    """Just enough of push.py's parsed-args surface for run_docs_only()."""
    return argparse.Namespace(message=message, dry_run=False, skip_tests=False, sonarqube=False)


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

        def _fake_run(cmd, cwd, **kwargs):
            captured["cmd"] = cmd
            captured["cwd"] = cwd
            return subprocess.CompletedProcess(args=cmd, returncode=0)

        monkeypatch.setattr(push_module.subprocess, "run", _fake_run)

        push_module.run_tracker_audit_gate(Path("/repo"), 1)

        assert captured["cwd"] == Path("/repo")
        assert captured["cmd"][0] == push_module.sys.executable
        # The audit script and the tracker it scans both moved out of this repo on 2026-08-30,
        # consolidated into ~/Project Work/scripts/ and parameterized by --project instead of
        # one copy per repo (see run_tracker_audit_gate()'s docstring).
        assert captured["cmd"][1] == str(Path.home() / "Project Work" / "scripts" / "todo-status-audit.py")
        assert captured["cmd"][2:] == [
            "--project", "juneau",
            "--dir", str(Path.home() / "Project Work" / "todos" / "juneau"),
        ]


class TestUpstreamVerdictAheadBehind:
    """
    The upstream verdict commit_and_push() reads (the toolkit's git.upstream_verdict) against real
    git: the ahead count is the fix, so it needs to be right, not just the pre-existing behind count.
    """

    @staticmethod
    def _verdict(push_module, repo_dir):
        v = push_module.toolkit("git").upstream_verdict(repo_dir)
        return (v.status, v.ahead, v.behind)

    def test_clean_and_synced_is_zero_and_zero(self, push_module, repo):
        assert self._verdict(push_module, repo) == ("ok", 0, 0)

    def test_a_local_only_commit_is_one_ahead_zero_behind(self, push_module, repo):
        _local_only_commit(repo)
        assert self._verdict(push_module, repo) == ("ok", 1, 0)

    def test_an_upstream_only_commit_is_zero_ahead_one_behind(self, push_module, tmp_path, repo, remote_repo):
        other = tmp_path / "other-clone"
        _run(tmp_path, "git", "clone", str(remote_repo), str(other))
        _local_only_commit(other, filename="from-elsewhere.txt", message="pushed by someone else")
        _run(other, "git", "push", "origin", "master")

        assert self._verdict(push_module, repo) == ("behind", 0, 1)

    def test_no_upstream_configured_is_none_none(self, push_module, tmp_path):
        lone = tmp_path / "lone"
        lone.mkdir()
        _run(lone, "git", "init", "-b", "master", ".")
        (lone / "f.txt").write_text("x\n", encoding="utf-8")
        _commit(lone, "no remote at all")

        assert self._verdict(push_module, lone) == ("no-upstream", None, None)


class TestGetStagedPaths:
    """
    The sole definition of "what push.py will commit" now that `git add .` is gone: whatever
    is already in the index, because a staged path is the operator's record of having reviewed
    it.
    """

    def test_a_clean_index_reports_nothing(self, push_module, repo):
        assert push_module.get_staged_paths(repo) == []

    def test_a_staged_file_is_reported(self, push_module, repo):
        (repo / "reviewed.txt").write_text("staged by the operator\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")

        assert push_module.get_staged_paths(repo) == ["reviewed.txt"]

    def test_an_unstaged_modification_alone_is_not_reported(self, push_module, repo):
        (repo / "README.md").write_text("unstaged edit\n", encoding="utf-8")
        assert push_module.get_staged_paths(repo) == []


class TestCheckUnreviewedChanges:
    """
    The other half of the invariant: anything NOT staged. Unstaged modifications to tracked
    files and untracked, non-gitignored files are both "unreviewed"; gitignored untracked
    files are not (see check_unreviewed_changes()'s docstring for why).
    """

    def test_a_clean_tree_reports_nothing(self, push_module, repo):
        assert push_module.check_unreviewed_changes(repo) == []

    def test_an_unstaged_modification_to_a_tracked_file_is_reported(self, push_module, repo):
        (repo / "README.md").write_text("unstaged edit\n", encoding="utf-8")
        assert push_module.check_unreviewed_changes(repo) == ["README.md"]

    def test_an_untracked_non_ignored_file_is_reported(self, push_module, repo):
        (repo / "scratch.txt").write_text("brand new, not staged\n", encoding="utf-8")
        assert push_module.check_unreviewed_changes(repo) == ["scratch.txt"]

    def test_a_gitignored_untracked_file_is_not_reported(self, push_module, repo):
        (repo / ".gitignore").write_text("ignored.log\n", encoding="utf-8")
        _commit(repo, "add gitignore")
        (repo / "ignored.log").write_text("noise\n", encoding="utf-8")

        assert push_module.check_unreviewed_changes(repo) == []

    def test_a_staged_file_alone_is_not_reported(self, push_module, repo):
        """Staged content is reviewed content -- it's the other function's (get_staged_paths()) job."""
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")

        assert push_module.check_unreviewed_changes(repo) == []


class TestCommitAndPushFalseSuccessRegression:
    """
    The defect: a working tree with nothing staged but unpushed local commits used to be
    reported as "nothing to commit and push" (returning success) without ever running
    `git push`.

    This is the test that fails against the pre-fix code: the pre-fix push.py checked only
    whether the working tree was clean and returned success without consulting how far ahead
    of upstream the branch was, so it would report success here while the remote never moved.
    A test that only checked the return status/exit code would pass against that bug -- the
    assertion that matters is that the remote ref ACTUALLY ADVANCED.
    """

    def test_clean_tree_with_unpushed_commits_actually_pushes(self, push_module, repo, remote_repo, git_spy):
        local_head = _local_only_commit(repo)
        assert push_module.get_staged_paths(repo) == [], "index must be clean for this to be the regression case"
        assert push_module.check_unreviewed_changes(repo) == [], "tree must be clean too, or this proves nothing"

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
    """Nothing staged AND nothing ahead of upstream is the one case that is truly a no-op."""

    def test_returns_nothing_to_do_and_touches_nothing(self, push_module, repo, remote_repo, git_spy):
        remote_before = _run(remote_repo, "git", "rev-parse", "master")

        status, step_num = push_module.commit_and_push(repo, "irrelevant message", 4)

        assert status == "nothing_to_do"
        assert step_num == 4, "step_num must be unchanged when nothing happened"
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)

    def test_unstaged_dirt_alone_with_nothing_ahead_is_still_a_no_op_not_a_refusal(
        self, push_module, repo, remote_repo, git_spy
    ):
        """
        Nothing staged and nothing ahead means nothing is about to be committed or pushed, so
        stray unstaged/untracked content sitting in the tree can't taint a push that isn't
        happening -- the unreviewed-changes guard only fires once there's actually something
        to commit and/or push (see commit_and_push()'s docstring). Deliberate scoping choice,
        not an oversight: an unconditional refusal here would fire on essentially any run where
        ANY file anywhere in the repo has a stray edit, whether or not that run would push
        anything at all.
        """
        (repo / "mid-edit.txt").write_text("still being worked on, nothing staged, nothing ahead\n", encoding="utf-8")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")

        status, _ = push_module.commit_and_push(repo, "irrelevant message", 4)

        assert status == "nothing_to_do"
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)


class TestCommitAndPushDirtyTree:
    """
    The ordinary path must keep working: content the operator has reviewed and staged still
    gets committed and pushed. Unlike before, staging is now the caller's job -- push.py
    commits the index as it finds it (see commit_and_push()'s docstring) -- so every test here
    stages explicitly rather than relying on an (now removed) `git add .`.
    """

    def test_staged_changes_are_committed_with_the_given_message_and_pushed(self, push_module, repo, remote_repo):
        (repo / "README.md").write_text("new content\n", encoding="utf-8")
        _run(repo, "git", "add", "README.md")

        status, step_num = push_module.commit_and_push(repo, "the commit message", 4)

        assert status == "ok"
        assert step_num == 6, "one step for the commit, one for the push"
        assert _run(repo, "git", "log", "-1", "--format=%s") == "the commit message"
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_a_staged_untracked_file_is_committed_too(self, push_module, repo, remote_repo):
        (repo / "new-file.txt").write_text("brand new\n", encoding="utf-8")
        _run(repo, "git", "add", "new-file.txt")

        status, _ = push_module.commit_and_push(repo, "add new file", 4)

        assert status == "ok"
        assert _run(remote_repo, "git", "show", "HEAD:new-file.txt") == "brand new"

    def test_git_add_is_never_invoked(self, push_module, repo, remote_repo, git_spy):
        """push.py no longer decides what to stage -- the index already says (see docstring)."""
        (repo / "README.md").write_text("new content\n", encoding="utf-8")
        _run(repo, "git", "add", "README.md")
        git_spy.clear()  # discard the setup `git add` above; only commit_and_push()'s own git calls matter here

        push_module.commit_and_push(repo, "the commit message", 4)

        assert not any(argv[:2] == ["git", "add"] for argv in git_spy)


class TestCommitAndPushUnreviewedChangesGuard:
    """
    Defect 2, redesigned per a workflow fact that inverts the original fix: James stages files
    as he reviews them, so a staged path is his record that he's read it and it's good. The
    original fix refused when the index already held staged content -- which is his NORMAL
    state, and would have fired on essentially every real run. The corrected rule refuses on
    the opposite condition: anything UNSTAGED (an unstaged modification, or an untracked file
    that isn't gitignored) anywhere in the tree, once there's something to commit and/or push.
    That makes "everything committed or pushed has been reviewed" true by construction, and it
    also means push.py no longer runs `git add .` at all -- it commits the index exactly as it
    finds it.
    """

    def test_unstaged_modification_alongside_staged_work_aborts_before_commit_or_push(
        self, push_module, repo, remote_repo, git_spy
    ):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "README.md").write_text("mid-edit, not staged\n", encoding="utf-8")
        head_before = _run(repo, "git", "rev-parse", "HEAD")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")
        git_spy.clear()  # discard the setup `git add` above; only commit_and_push()'s own git calls matter here

        status, _ = push_module.commit_and_push(repo, "my commit message", 4)

        assert status == "error"
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before, "nothing should have been committed"
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before, "the remote must not move"
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)

    def test_untracked_non_ignored_file_alongside_staged_work_aborts(self, push_module, repo, remote_repo):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "scratch.txt").write_text("brand new, not staged\n", encoding="utf-8")

        status, _ = push_module.commit_and_push(repo, "message", 4)

        assert status == "error"
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_a_gitignored_untracked_file_does_not_trip_the_guard(self, push_module, repo, remote_repo):
        (repo / ".gitignore").write_text("build.log\n", encoding="utf-8")
        _run(repo, "git", "add", ".gitignore")
        (repo / "build.log").write_text("noise\n", encoding="utf-8")

        status, _ = push_module.commit_and_push(repo, "add gitignore", 4)

        assert status == "ok"
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_the_staged_file_remains_staged_and_the_unstaged_file_remains_unstaged(self, push_module, repo):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "mine.txt").write_text("mid-edit, not staged\n", encoding="utf-8")

        push_module.commit_and_push(repo, "my commit message", 4)

        assert _run(repo, "git", "diff", "--cached", "--name-only") == "reviewed.txt"
        status_lines = _run(repo, "git", "status", "--porcelain").splitlines()
        assert "?? mine.txt" in status_lines

    def test_the_error_message_names_the_unreviewed_paths(self, push_module, repo, capsys):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "secret-in-progress.txt").write_text("do not push yet\n", encoding="utf-8")

        push_module.commit_and_push(repo, "message", 4)

        assert "secret-in-progress.txt" in capsys.readouterr().out

    def test_the_error_message_includes_a_copy_pasteable_stash_recovery_hint(self, push_module, repo, capsys):
        """
        The recovery hint has to be discoverable at the moment of refusal, and it has to be the
        REAL command for THIS refusal -- with the actual offending paths already filled in --
        not a generic template, so this pins the exact paths into the emitted `git stash push`
        command rather than just checking that a stash mention exists somewhere in the output.
        """
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "one.txt").write_text("mid-edit one\n", encoding="utf-8")
        (repo / "two.txt").write_text("mid-edit two\n", encoding="utf-8")

        push_module.commit_and_push(repo, "message", 4)
        out = capsys.readouterr().out

        assert "git stash push -- one.txt two.txt" in out
        assert "git stash pop" in out

    def test_the_error_message_says_why_the_refusal_exists(self, push_module, repo, capsys):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "mid-edit.txt").write_text("still being worked on\n", encoding="utf-8")

        push_module.commit_and_push(repo, "message", 4)

        assert "should be something you've read" in capsys.readouterr().out.lower()


class TestCommitAndPushPartialReviewIsRefused:
    """
    The exact scenario the task named: some files reviewed and staged, others still mid-edit.
    The strict rule refuses the WHOLE run rather than pushing just the reviewed part -- this is
    the deliberate, current behavior (see commit_and_push()'s docstring for the reasoning, and
    the task report for the escape-hatch question raised but not built).
    """

    def test_reviewed_and_staged_work_is_not_pushed_while_other_files_are_mid_edit(
        self, push_module, repo, remote_repo, git_spy
    ):
        (repo / "reviewed-a.txt").write_text("reviewed a\n", encoding="utf-8")
        (repo / "reviewed-b.txt").write_text("reviewed b\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed-a.txt", "reviewed-b.txt")
        (repo / "mid-edit.txt").write_text("still being worked on\n", encoding="utf-8")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")
        git_spy.clear()

        status, _ = push_module.commit_and_push(repo, "reviewed work", 4)

        assert status == "error"
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)


class TestCommitAndPushAheadOnlyBlockedByUnreviewedChanges:
    """
    The false-success fix's "a clean tree with nothing staged and commits ahead must still
    push" guarantee is specifically scoped to a CLEAN tree. If the tree isn't clean -- even
    though nothing currently staged is what would be pushed -- the ahead-only push is refused
    too, the same as the has-staged-work case.
    """

    def test_an_ahead_only_push_is_blocked_by_unstaged_dirt(self, push_module, repo, remote_repo, git_spy):
        _local_only_commit(repo)  # ahead by 1, index clean
        (repo / "mid-edit.txt").write_text("still being worked on\n", encoding="utf-8")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")
        git_spy.clear()

        status, _ = push_module.commit_and_push(repo, "message", 4)

        assert status == "error"
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)


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


class TestRunDocsOnlyFalseSuccessRegression:
    """
    The same defect, replayed against run_docs_only() specifically rather than assumed to carry
    over from the main-path test: juneau-docs pushes to ITS OWN remote (docs_remote_repo, a
    separate bare repo from the juneau one), so this exercises commit_and_push() wired to a
    genuinely different repo_dir/remote pair, not just a second call with the same fixture.

    Manually replayed against the OLD run_docs_only() logic (Step 3's `check_git_status()`-only
    check) before writing this test -- see the shell replay in the task history -- and confirmed
    the old code would report success (return 0) while docs_remote_repo's `master` never moved.
    """

    def test_clean_tree_with_unpushed_docs_commits_actually_pushes(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, git_spy
    ):
        local_head = _local_only_commit(docs_repo)
        git_spy.clear()  # discard the setup commit above

        exit_code = push_module.run_docs_only(_docs_args(), juneau_root)

        assert exit_code == 0
        remote_head = _run(docs_remote_repo, "git", "rev-parse", "master")
        assert remote_head == local_head, "the juneau-docs remote must actually have moved"
        assert any(argv[:2] == ["git", "push"] for argv in git_spy), "git push must actually have run"

    def test_clean_tree_with_unpushed_docs_commits_skips_the_smoke_check(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, capsys
    ):
        """
        The stub build-docs.py in docs_repo always exits 0, so a passing smoke check alone
        wouldn't prove it was skipped -- check for its own announcement text instead.
        """
        _local_only_commit(docs_repo)
        capsys.readouterr()

        exit_code = push_module.run_docs_only(_docs_args(), juneau_root)

        assert exit_code == 0
        assert "smoke check" not in capsys.readouterr().out.lower()


class TestRunDocsOnlyGenuineNoOp:
    def test_returns_zero_and_touches_nothing(self, push_module, docs_repo, docs_remote_repo, juneau_root, git_spy):
        remote_before = _run(docs_remote_repo, "git", "rev-parse", "master")

        exit_code = push_module.run_docs_only(_docs_args(), juneau_root)

        assert exit_code == 0
        assert _run(docs_remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "add"] for argv in git_spy)


class TestRunDocsOnlyUnreviewedChangesGuard:
    def test_unstaged_docs_content_aborts_before_any_commit_or_push(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, git_spy
    ):
        (docs_repo / "reviewed.md").write_text("reviewed and staged\n", encoding="utf-8")
        _run(docs_repo, "git", "add", "reviewed.md")
        (docs_repo / "mid-edit.md").write_text("still being worked on\n", encoding="utf-8")
        head_before = _run(docs_repo, "git", "rev-parse", "HEAD")
        remote_before = _run(docs_remote_repo, "git", "rev-parse", "master")
        git_spy.clear()

        exit_code = push_module.run_docs_only(_docs_args(), juneau_root)

        assert exit_code == 1
        assert _run(docs_repo, "git", "rev-parse", "HEAD") == head_before
        assert _run(docs_remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)


class TestRunDocsOnlyDirtyTree:
    """
    The ordinary --docs-only path (a real, staged docs change, smoke check included) must keep
    working.
    """

    def test_staged_changes_run_the_smoke_check_commit_and_push(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, capsys
    ):
        (docs_repo / "README.md").write_text("updated docs\n", encoding="utf-8")
        _run(docs_repo, "git", "add", "README.md")

        exit_code = push_module.run_docs_only(_docs_args("docs update"), juneau_root)

        assert exit_code == 0
        assert "smoke check" in capsys.readouterr().out.lower()
        assert _run(docs_repo, "git", "log", "-1", "--format=%s") == "docs update"
        assert _run(docs_remote_repo, "git", "rev-parse", "master") == _run(docs_repo, "git", "rev-parse", "HEAD")


class TestRunDocsFollowup:
    """main()'s Step 6, extracted as run_docs_followup() so it's testable without mvn."""

    def test_no_docs_root_is_a_pure_skip(self, push_module, tmp_path):
        lone_juneau_root = tmp_path / "juneau"
        status, step_num = push_module.run_docs_followup(lone_juneau_root, "message", 6)
        assert (status, step_num) == ("no_docs_root", 6)

    def test_clean_tree_with_unpushed_docs_commits_actually_pushes(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, git_spy
    ):
        """
        Same false-success regression as TestRunDocsOnlyFalseSuccessRegression, replayed at the
        OTHER call site (main()'s Step 6 -- previously gated on a clean-working-tree check,
        which would have skipped this entire follow-up, never even attempting a push).
        """
        local_head = _local_only_commit(docs_repo)
        git_spy.clear()

        status, _ = push_module.run_docs_followup(juneau_root, "message", 6)

        assert status == "ok"
        remote_head = _run(docs_remote_repo, "git", "rev-parse", "master")
        assert remote_head == local_head, "the juneau-docs remote must actually have moved"
        assert any(argv[:2] == ["git", "push"] for argv in git_spy)

    def test_the_docs_identity_is_a_precheck_for_an_ahead_only_push(
        self, push_module, docs_repo, juneau_root
    ):
        """
        With nothing to commit -- only something to push -- juneau-docs is still active, so the
        Prechecks row checks its identity (which replaced the follow-up's own identity hook).
        """
        _local_only_commit(docs_repo)
        _run(docs_repo, "git", "config", "--unset", "user.email")

        listed = {c.name: c for c in push_module.prechecks(juneau_root)}

        assert push_module.toolkit("checks").evaluate(listed["docs identity"]).status == "fail"

    def test_idle_at_prechecks_then_active_with_a_good_identity_proceeds(
        self, push_module, docs_repo, docs_remote_repo, juneau_root
    ):
        local_head = _local_only_commit(docs_repo)

        status, _ = push_module.run_docs_followup(juneau_root, "message", 6, docs_prechecked=False)

        assert status == "ok"
        assert _run(docs_remote_repo, "git", "rev-parse", "master") == local_head

    def test_idle_at_prechecks_then_active_with_a_bad_identity_refuses_before_the_commit(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, git_spy
    ):
        (docs_repo / "README.md").write_text("became active during the build\n", encoding="utf-8")
        _run(docs_repo, "git", "add", "README.md")
        _run(docs_repo, "git", "config", "user.email", "someone@example.com")
        head_before = _run(docs_repo, "git", "rev-parse", "HEAD")
        remote_before = _run(docs_remote_repo, "git", "rev-parse", "master")
        git_spy.clear()

        status, _ = push_module.run_docs_followup(juneau_root, "message", 6, docs_prechecked=False)

        assert status == "error"
        assert _run(docs_repo, "git", "rev-parse", "HEAD") == head_before
        assert _run(docs_remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)

    def test_active_at_prechecks_is_not_checked_again(self, push_module, docs_repo, juneau_root, monkeypatch):
        _local_only_commit(docs_repo)
        _run(docs_repo, "git", "config", "user.email", "someone@example.com")
        checked = []
        monkeypatch.setattr(push_module, "docs_active", lambda root: checked.append(root) or True)

        push_module.run_docs_followup(juneau_root, "message", 6, docs_prechecked=True)

        assert checked == []

    def test_smoke_check_does_not_run_for_an_ahead_only_push(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, capsys
    ):
        _local_only_commit(docs_repo)
        capsys.readouterr()

        status, _ = push_module.run_docs_followup(juneau_root, "message", 6)

        assert status == "ok"
        assert "smoke check" not in capsys.readouterr().out.lower()

    def test_genuine_no_op_returns_ok_and_touches_nothing(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, git_spy
    ):
        remote_before = _run(docs_remote_repo, "git", "rev-parse", "master")

        status, _ = push_module.run_docs_followup(juneau_root, "message", 6)

        assert status == "ok"
        assert _run(docs_remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)

    def test_unstaged_docs_content_aborts_before_any_commit_or_push(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, git_spy
    ):
        (docs_repo / "reviewed.md").write_text("reviewed and staged\n", encoding="utf-8")
        _run(docs_repo, "git", "add", "reviewed.md")
        (docs_repo / "mid-edit.md").write_text("still being worked on\n", encoding="utf-8")
        head_before = _run(docs_repo, "git", "rev-parse", "HEAD")
        remote_before = _run(docs_remote_repo, "git", "rev-parse", "master")
        git_spy.clear()

        status, _ = push_module.run_docs_followup(juneau_root, "message", 6)

        assert status == "error"
        assert _run(docs_repo, "git", "rev-parse", "HEAD") == head_before
        assert _run(docs_remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)

    def test_staged_changes_run_the_smoke_check_commit_and_push(
        self, push_module, docs_repo, docs_remote_repo, juneau_root, capsys
    ):
        (docs_repo / "README.md").write_text("updated docs via step 6\n", encoding="utf-8")
        _run(docs_repo, "git", "add", "README.md")

        status, step_num = push_module.run_docs_followup(juneau_root, "step 6 message", 6)

        assert status == "ok"
        assert step_num == 8, "one step for the commit, one for the push"
        assert "smoke check" in capsys.readouterr().out.lower()
        assert _run(docs_repo, "git", "log", "-1", "--format=%s") == "step 6 message"
        assert _run(docs_remote_repo, "git", "rev-parse", "master") == _run(docs_repo, "git", "rev-parse", "HEAD")


class TestResolveCommitMessage:
    """A missing commit message is prompted for, not rejected; an empty or aborted answer exits."""

    def test_cli_message_used_without_prompting(self, push_module, monkeypatch):
        monkeypatch.setattr("builtins.input", lambda _prompt: pytest.fail("should not prompt"))
        assert push_module.resolve_commit_message("Fix a thing") == "Fix a thing"

    def test_missing_message_is_prompted_for(self, push_module, monkeypatch):
        prompts = []
        monkeypatch.setattr("builtins.input", lambda prompt: prompts.append(prompt) or "  Typed message  ")
        assert push_module.resolve_commit_message(None) == "Typed message"
        assert prompts == ["Commit message: "]

    def test_empty_answer_exits(self, push_module, monkeypatch):
        monkeypatch.setattr("builtins.input", lambda _prompt: "   ")
        with pytest.raises(SystemExit) as e:
            push_module.resolve_commit_message(None)
        assert e.value.code == 1

    def test_empty_commit_message_fails_with_exit_1(self, push_module, monkeypatch, capsys):
        prompt = push_module.toolkit("prompt")
        asked = []
        real = prompt.text
        monkeypatch.setattr(prompt, "text", lambda msg, **k: asked.append(msg) or real(msg, **k))
        monkeypatch.setattr("builtins.input", lambda _prompt: "")
        with pytest.raises(SystemExit) as e:
            push_module.resolve_commit_message(None)
        assert e.value.code == 1
        assert asked == ["Commit message: "]
        assert "commit message cannot be empty" in capsys.readouterr().out

    def test_ctrl_c_at_the_prompt_exits_1(self, push_module, monkeypatch):
        def interrupted(_prompt):
            raise KeyboardInterrupt
        monkeypatch.setattr("builtins.input", interrupted)
        with pytest.raises(SystemExit) as e:
            push_module.resolve_commit_message(None)
        assert e.value.code == 1

    def test_eof_exits(self, push_module, monkeypatch):
        def _eof(_prompt):
            raise EOFError
        monkeypatch.setattr("builtins.input", _eof)
        with pytest.raises(SystemExit) as e:
            push_module.resolve_commit_message(None)
        assert e.value.code == 1


# ---------------------------------------------------------------------------------------
# Characterization: the test step's command, the history .tsv call, and the refusal guard's
# exact text, pinned before the toolkit adoption.
# ---------------------------------------------------------------------------------------
def test_build_test_command_has_no_timing_log(push_module):
    args = argparse.Namespace(js_tests=False, no_js_tests=False)
    assert push_module.build_test_command("t.py", args) == [push_module.sys.executable, "t.py", "--full"]
    assert push_module.build_test_command("t.py", argparse.Namespace(js_tests=True, no_js_tests=False))[-1] == \
        "--js-tests"


def _stub_push_flow(push_module, monkeypatch, *, tests_pass, test_py=True):
    """Every gate and side effect of push.main() replaced; returns the _append_test_run_history calls."""
    calls = []
    for name in ("run_timing_report", "verify_starter_repos"):
        monkeypatch.setattr(push_module, name, lambda *a, **k: True)
    monkeypatch.setattr(push_module, "run_command",
                        lambda cmd, *a, **k: tests_pass if cmd == ["mvn", "test"] else True)
    monkeypatch.setattr(push_module, "_append_test_run_history", lambda *a: calls.append(a))
    monkeypatch.setattr(push_module, "current_branch", lambda root: "main")
    monkeypatch.setenv("JUNEAU_SCRIPTS_QUIET", "1")    # the session's bell and sound
    monkeypatch.setattr(push_module, "run_test_script",
                        lambda cmd, cwd, run, **k: subprocess.CompletedProcess(cmd, 0 if tests_pass else 1))
    if not test_py:
        monkeypatch.setattr(push_module.Path, "exists", lambda self: self.name != "test.py" and Path.is_file(self))
    monkeypatch.setattr(push_module.sys, "argv", ["push.py", "--test-only"])
    return calls


@pytest.mark.parametrize("test_py", [True, False], ids=["test.py", "mvn-fallback"])
def test_append_test_run_history_is_called_once_after_a_passing_test_step(push_module, monkeypatch, test_py):
    calls = _stub_push_flow(push_module, monkeypatch, tests_pass=True, test_py=test_py)
    assert push_module.main() == 0
    assert len(calls) == 1
    root, wall = calls[0]
    assert root == Path(push_module.__file__).resolve().parent.parent and isinstance(wall, int)


@pytest.mark.parametrize("test_py", [True, False], ids=["test.py", "mvn-fallback"])
def test_append_test_run_history_is_not_called_after_a_failing_test_step(push_module, monkeypatch, test_py):
    calls = _stub_push_flow(push_module, monkeypatch, tests_pass=False, test_py=test_py)
    assert push_module.main() == 1
    assert calls == []


APPEND_TEST_RUN_HISTORY_SHA256 = "c8570df4e5c1cce64d15f68b1fc78232d51422dfedad204758d7686145381975"


def test_append_test_run_history_is_unchanged_and_the_only_tsv_writer(push_module, monkeypatch):
    """The tsv writer is byte-identical, and a stubbed --test-only run opens nothing at the real tsv path."""
    source = inspect.getsource(push_module._append_test_run_history)
    assert hashlib.sha256(source.encode()).hexdigest() == APPEND_TEST_RUN_HISTORY_SHA256
    _stub_push_flow(push_module, monkeypatch, tests_pass=True)
    tsv = (Path(push_module.__file__).resolve().parent.parent / "juneau-integration-tests"
           / "test-run-history.tsv").resolve()
    opened = []

    def spy(real):
        def wrapped(*args, **kwargs):
            target = args[0] if args else kwargs.get("file")
            if isinstance(target, (str, bytes, os.PathLike)):
                try:
                    opened.append(Path(os.fsdecode(target)).resolve())
                except (OSError, ValueError):
                    pass
            return real(*args, **kwargs)
        return wrapped

    monkeypatch.setattr(builtins, "open", spy(builtins.open))
    monkeypatch.setattr(io, "open", spy(io.open))
    monkeypatch.setattr(os, "open", spy(os.open))
    monkeypatch.setattr(pathlib.Path, "open", spy(pathlib.Path.open))
    assert push_module.main() == 0
    assert tsv not in opened


def _said(push_module, monkeypatch):
    lines = []
    monkeypatch.setattr(push_module, "say", lambda text="", level="info": lines.append(text))
    return lines


class TestRefusalGuardCharacterization:
    def test_refusal_guard_text_is_exact(self, push_module, repo, remote_repo, git_spy, monkeypatch):
        (repo / "a b.txt").write_text("tracked\n", encoding="utf-8")
        _commit(repo, "add a b")
        _run(repo, "git", "push")
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "a b.txt").write_text("mid-edit\n", encoding="utf-8")
        (repo / "new.txt").write_text("untracked\n", encoding="utf-8")
        head_before = _run(repo, "git", "rev-parse", "HEAD")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")
        git_spy.clear()
        lines = _said(push_module, monkeypatch)

        assert push_module.commit_and_push(repo, "message", 4) == ("error", 4)

        assert lines == [
            "\n🔍 Checking for upstream changes...",
            "\n❌ ERROR: The working tree has unreviewed changes -- staging is how this script knows something has "
            "been reviewed, and these paths are not staged:",
            "   a b.txt",
            "   new.txt",
            "   Why: everything this pushes should be something you've read, not just",
            "   something that happened to be in the tree.",
            "\n   Nothing was committed or pushed. Stage each path above if you've reviewed",
            "   it, or set it aside for this run and restore it after:",
            "     git stash push -- 'a b.txt' new.txt",
            "     git stash pop",
        ]
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] in (["git", "commit"], ["git", "push"]) for argv in git_spy)

    def test_refusal_guard_ignores_gitignored_untracked_files(self, push_module, repo, remote_repo, monkeypatch):
        (repo / ".gitignore").write_text("build.log\n", encoding="utf-8")
        _run(repo, "git", "add", ".gitignore")
        (repo / "build.log").write_text("noise\n", encoding="utf-8")
        lines = _said(push_module, monkeypatch)

        status, _ = push_module.commit_and_push(repo, "add gitignore", 4)

        assert status == "ok"
        assert not any("unreviewed changes" in line for line in lines)

    def test_refusal_guard_fails_closed_when_git_cannot_list(self, push_module, repo, remote_repo, monkeypatch):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        head_before = _run(repo, "git", "rev-parse", "HEAD")

        def broken(repo_dir):
            raise OSError("git is gone")
        monkeypatch.setattr(push_module, "check_unreviewed_changes", broken)
        lines = _said(push_module, monkeypatch)

        assert push_module.commit_and_push(repo, "message", 4) == ("error", 4)
        assert lines[1].startswith("\n❌ ERROR: Could not check for unreviewed changes")
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before


# ---------------------------------------------------------------------------------------
# The git readers come from the juneau_scripts toolkit; the refusal guard around them is unchanged.
# ---------------------------------------------------------------------------------------
@pytest.fixture
def toolkit_git(push_module):
    return push_module.toolkit("git")


def test_staged_paths_come_from_the_toolkit(push_module, toolkit_git, monkeypatch):
    seen = []
    monkeypatch.setattr(toolkit_git, "staged_paths", lambda repo: seen.append(repo) or ["a b.txt"])
    assert push_module.get_staged_paths(Path("/r")) == ["a b.txt"]
    assert seen == [Path("/r")]


def test_unreviewed_changes_are_the_toolkits_unstaged_then_untracked(push_module, toolkit_git, monkeypatch):
    monkeypatch.setattr(toolkit_git, "unstaged_paths", lambda repo: ["m.txt"])
    monkeypatch.setattr(toolkit_git, "untracked_paths", lambda repo: ["new.txt"])
    assert push_module.check_unreviewed_changes(Path("/r")) == ["m.txt", "new.txt"]


def test_the_late_upstream_check_uses_the_toolkit_verdict(push_module, toolkit_git, repo, monkeypatch):
    calls = []

    def verdict(repo_dir, **k):
        calls.append(k)
        return toolkit_git.UpstreamVerdict("behind", 0, 1)
    monkeypatch.setattr(toolkit_git, "upstream_verdict", verdict)
    lines = _said(push_module, monkeypatch)
    assert push_module.commit_and_push(repo, "message", 4, git_cmd="git -C x") == ("error", 4)
    assert calls == [{"git_cmd": "git -C x"}]
    assert any("Nothing was committed or pushed." in line for line in lines)


def test_a_late_upstream_error_fails_closed_before_the_commit(push_module, toolkit_git, repo, git_spy,
                                                              monkeypatch):
    (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
    _run(repo, "git", "add", "reviewed.txt")
    head_before = _run(repo, "git", "rev-parse", "HEAD")
    monkeypatch.setattr(toolkit_git, "upstream_verdict",
                        lambda repo_dir, **k: toolkit_git.UpstreamVerdict("error", reason="could not check upstream: x"))
    lines = _said(push_module, monkeypatch)
    git_spy.clear()

    assert push_module.commit_and_push(repo, "message", 4, git_cmd=push_module.DOCS_GIT) == ("error", 4)

    assert "\n❌ ERROR: could not check upstream: x. Nothing was committed or pushed." in lines
    assert f"   Re-run the push once `{push_module.DOCS_GIT} fetch` works." in lines
    assert _run(repo, "git", "rev-parse", "HEAD") == head_before
    assert not any(argv[:2] in (["git", "commit"], ["git", "push"]) for argv in git_spy)


@pytest.mark.parametrize(("branch", "head", "expected"), [
    ("main", "abc1234", "main"), (None, "abc1234", "HEAD"), (None, None, "unknown")])
def test_current_branch_falls_back_to_unknown(push_module, toolkit_git, monkeypatch, branch, head, expected):
    monkeypatch.setattr(toolkit_git, "current_branch", lambda repo: branch)
    monkeypatch.setattr(toolkit_git, "short_head", lambda repo: head)
    assert push_module.current_branch(Path("/r")) == expected


def test_current_branch_on_a_detached_head_is_still_head(push_module, repo):
    _run(repo, "git", "checkout", "-q", "--detach")
    assert push_module.current_branch(repo) == "HEAD"


@pytest.mark.parametrize("reader", ["get_staged_paths", "check_unreviewed_changes"])
def test_refusal_guard_fails_closed_on_a_git_error(push_module, toolkit_git, repo, monkeypatch, reader):
    (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
    _run(repo, "git", "add", "reviewed.txt")
    head_before = _run(repo, "git", "rev-parse", "HEAD")

    def broken(repo_dir):
        raise toolkit_git.GitError(["git", "diff"], toolkit_git.Result(False, 128, err="boom"))
    monkeypatch.setattr(push_module, reader, broken)
    lines = _said(push_module, monkeypatch)

    assert push_module.commit_and_push(repo, "message", 4) == ("error", 4)
    assert lines[1].startswith("\n❌ ERROR: Could not check ")
    assert _run(repo, "git", "rev-parse", "HEAD") == head_before


# ---------------------------------------------------------------------------------------
# The opt-in gates run through the toolkit's run.gate; each keeps its own exit-code contract.
# ---------------------------------------------------------------------------------------
def _gate_exits(push_module, monkeypatch, returncode):
    monkeypatch.setattr(push_module, "_python310", lambda: push_module.sys.executable)
    monkeypatch.setattr(push_module, "current_branch", lambda root: "master")
    monkeypatch.setattr(push_module.subprocess, "run",
                        lambda cmd, *a, **k: subprocess.CompletedProcess(cmd, returncode))


@pytest.mark.parametrize(("returncode", "expected"), [(0, "pass"), (2, "fail"), (3, "skip"), (1, "error"),
                                                      (77, "error")])
def test_sonar_gate_maps_0_2_3_and_other_to_error(push_module, monkeypatch, returncode, expected):
    _gate_exits(push_module, monkeypatch, returncode)
    assert push_module.run_sonarqube_gate(Path("/repo"), 1) == expected


@pytest.mark.parametrize(("returncode", "expected"), [(0, "pass"), (1, "fail"), (2, "error"), (3, "error"),
                                                      (77, "error")])
def test_tracker_gate_maps_0_pass_1_fail_other_error(push_module, monkeypatch, returncode, expected):
    _gate_exits(push_module, monkeypatch, returncode)
    assert push_module.run_tracker_audit_gate(Path("/repo"), 1) == expected


def test_the_gates_go_through_the_toolkit_gate(push_module, monkeypatch):
    run = push_module.toolkit("run")
    seen = []
    real = run.gate

    def spy(cmd, **kw):
        seen.append({k: kw.get(k) for k in ("title", "id", "label", "cwd", "codes")})
        return real(cmd, **kw)
    monkeypatch.setattr(run, "gate", spy)
    _gate_exits(push_module, monkeypatch, 0)
    push_module.run_sonarqube_gate(Path("/repo"), 1)
    push_module.run_tracker_audit_gate(Path("/repo"), 1)
    assert seen == [
        {"title": "SonarQube gate", "id": "sonar", "label": "Sonar", "cwd": Path("/repo"), "codes": None},
        {"title": "Tracker audit", "id": "tracker", "label": "Tracker", "cwd": Path("/repo"),
         "codes": {0: "pass", 1: "fail"}},
    ]


# ---------------------------------------------------------------------------------------
# Prechecks: unreviewed changes and a behind-upstream branch refuse before any gate runs.
# ---------------------------------------------------------------------------------------
class _PastPrechecks(Exception):
    """Raised by the stubbed first gate: the push got past its Prechecks to the first gate."""


@pytest.fixture
def flow(push_module, repo, monkeypatch):
    """_main() pointed at `repo` (scripts/ beside it) with a stand-in session: the real Prechecks run, and the push
    stops at the first gate.  `gates` records "gate" and "session" when the push got that far."""
    (repo / "scripts").mkdir()
    monkeypatch.setattr(push_module, "__file__", str(repo / "scripts" / "push.py"))
    _run(repo, "git", "config", "user.email", push_module.REQUIRED_GIT_EMAIL)
    gates = []
    run = push_module.toolkit("run")

    class Session:
        def checks(self, id, title, listed):
            result = run.checks(id, title, listed)
            if not result.ok:
                raise SystemExit(1)
            return result

    def in_session(title, body, **kw):
        try:
            return body(Session())
        except SystemExit as e:
            return e.code
        except _PastPrechecks:
            gates.append("session")
            return 0

    def first_gate(*a, **k):
        gates.append("gate")
        raise _PastPrechecks()

    monkeypatch.setattr(push_module, "maybe_run_tracker_audit_gate", first_gate)
    monkeypatch.setattr(push_module, "_in_session", in_session)
    monkeypatch.setattr(push_module, "resolve_commit_message", lambda m: m)
    monkeypatch.setattr(push_module, "current_branch", lambda root: "master")
    # scripts/ is untracked in `repo`; ignore it so it isn't itself an unreviewed change
    (repo / ".git" / "info" / "exclude").write_text("scripts/\n", encoding="utf-8")

    def go(*argv):
        monkeypatch.setattr(push_module.sys, "argv", ["push.py", *argv])
        return push_module._main()

    go.gates = gates
    return go


def _push_from_elsewhere(tmp_path: Path, remote_repo: Path) -> None:
    other = tmp_path / "other-clone"
    _run(tmp_path, "git", "clone", str(remote_repo), str(other))
    _local_only_commit(other, filename="from-elsewhere.txt", message="pushed by someone else")
    _run(other, "git", "push", "origin", "master")


class TestPrechecks:
    def test_up_to_date_and_clean_proceeds_to_the_gates(self, flow):
        assert flow("message") == 0
        assert flow.gates == ["gate", "session"]

    def test_behind_refuses_before_any_gate_with_ff_only_hint(self, flow, tmp_path, remote_repo, capsys):
        _push_from_elsewhere(tmp_path, remote_repo)

        assert flow("message") == 1
        assert flow.gates == []    # no gate, mvn or test.py was reached
        out = capsys.readouterr().out
        assert "✗ upstream: 1 commit behind upstream" in out
        assert "git merge --ff-only @{upstream}" in out
        assert "--autostash" not in out

    def test_behind_with_local_commits_gets_the_stash_rebase_hint(self, flow, tmp_path, repo, remote_repo, capsys):
        _push_from_elsewhere(tmp_path, remote_repo)
        _local_only_commit(repo)

        assert flow("message") == 1
        assert flow.gates == []
        out = capsys.readouterr().out
        assert "git stash push && git pull --rebase && git stash pop --index" in out
        assert "--ff-only" not in out and "--autostash" not in out

    def test_unreviewed_changes_refuse_before_any_gate(self, flow, repo, capsys):
        (repo / "README.md").write_text("edited\n", encoding="utf-8")
        (repo / "new.txt").write_text("new\n", encoding="utf-8")

        assert flow("message") == 1
        assert flow.gates == []
        out = capsys.readouterr().out
        assert "✗ unreviewed: 2 paths are not staged" in out
        assert "README.md" in out and "new.txt" in out
        assert "Nothing was built, committed or pushed." in out
        assert "git stash push -- " in out

    def test_the_wrong_identity_refuses_before_any_gate(self, push_module, flow, repo, capsys):
        _run(repo, "git", "config", "user.email", "someone@example.invalid")

        assert flow("message") == 1
        assert flow.gates == []
        out = capsys.readouterr().out
        assert f"git user.email is someone@example.invalid, not {push_module.REQUIRED_GIT_EMAIL}" in out
        assert f"git config user.email {push_module.REQUIRED_GIT_EMAIL}" in out

    def test_staged_changes_do_not_refuse(self, flow, repo):
        (repo / "README.md").write_text("edited\n", encoding="utf-8")
        _run(repo, "git", "add", "README.md")
        assert flow("message") == 0
        assert "session" in flow.gates

    def test_no_upstream_proceeds(self, flow, repo):
        _run(repo, "git", "branch", "--unset-upstream")
        assert flow("message") == 0
        assert "session" in flow.gates

    def test_fetch_error_fails_closed(self, flow, repo, remote_repo, capsys):
        _run(repo, "git", "remote", "set-url", "origin", str(remote_repo) + "-missing")

        assert flow("message") == 1
        assert flow.gates == []
        assert "✗ upstream: could not check upstream" in capsys.readouterr().out

    def test_skip_tests_still_runs_the_prechecks(self, flow, tmp_path, remote_repo):
        _push_from_elsewhere(tmp_path, remote_repo)

        assert flow("message", "--skip-tests") == 1
        assert flow.gates == []

    def test_test_only_skips_the_prechecks(self, flow, tmp_path, remote_repo):
        _push_from_elsewhere(tmp_path, remote_repo)

        assert flow("--test-only") == 0    # nothing is committed or pushed, so behind-upstream is irrelevant
        assert flow.gates == ["gate", "session"]

    def test_dry_run_reports_would_refuse_and_exits_zero(self, flow, tmp_path, repo, remote_repo, capsys):
        _push_from_elsewhere(tmp_path, remote_repo)
        (repo / "new.txt").write_text("new\n", encoding="utf-8")

        assert flow("message", "--dry-run") == 0
        out = capsys.readouterr().out
        assert "⚠ unreviewed: would refuse: 1 path is not staged" in out
        assert "⚠ upstream: would refuse: 1 commit behind upstream" in out
        assert "Nothing would be built, committed or pushed." in out
        assert "Prechecks" in out
        assert flow.gates == []

    def test_late_check_in_commit_and_push_is_kept(self, push_module, tmp_path, repo, remote_repo):
        _push_from_elsewhere(tmp_path, remote_repo)    # upstream moved after the Prechecks would have passed

        status, _ = push_module.commit_and_push(repo, "message", 4)

        assert status == "error"


# ---------------------------------------------------------------------------------------
# The juneau-docs Prechecks run only when juneau-docs has something to push.
# ---------------------------------------------------------------------------------------
DOCS_CHECKS = ["docs identity", "docs unreviewed", "docs upstream"]


class TestDocsPrechecksWhenIdle:
    @staticmethod
    def _docs_checks(push_module, juneau_root):
        return [c.name for c in push_module.prechecks(juneau_root) if c.name.startswith("docs ")]

    def test_an_idle_docs_tree_is_not_checked_so_its_dirt_does_not_block(self, push_module, docs_repo,
                                                                          juneau_root):
        (docs_repo / "README.md").write_text("mid-edit\n", encoding="utf-8")
        (docs_repo / "scratch.md").write_text("untracked\n", encoding="utf-8")
        _run(docs_repo, "git", "config", "--unset", "user.email")

        assert push_module.docs_active(docs_repo) is False
        assert self._docs_checks(push_module, juneau_root) == []

    def test_staged_docs_changes_are_checked_and_their_dirt_blocks(self, push_module, docs_repo, juneau_root):
        (docs_repo / "reviewed.md").write_text("staged\n", encoding="utf-8")
        _run(docs_repo, "git", "add", "reviewed.md")
        (docs_repo / "scratch.md").write_text("untracked\n", encoding="utf-8")

        listed = {c.name: c for c in push_module.prechecks(juneau_root)}

        assert self._docs_checks(push_module, juneau_root) == DOCS_CHECKS
        verdict = push_module.toolkit("checks").evaluate(listed["docs unreviewed"])
        assert verdict.status == "fail" and "scratch.md" in " ".join(verdict.hint)

    def test_unpushed_docs_commits_are_checked(self, push_module, docs_repo, juneau_root):
        _local_only_commit(docs_repo)
        (docs_repo / "scratch.md").write_text("untracked\n", encoding="utf-8")

        assert push_module.docs_active(docs_repo) is True
        assert self._docs_checks(push_module, juneau_root) == DOCS_CHECKS

    def test_docs_only_always_checks_the_docs(self, push_module, docs_repo):
        assert push_module.docs_active(docs_repo) is False
        assert [c.name for c in push_module.docs_prechecks(docs_repo)] == DOCS_CHECKS


# ---------------------------------------------------------------------------------------
# Console default: flag > JUNEAU_RUN_CONSOLE > condensed.
# ---------------------------------------------------------------------------------------
class TestConsoleDefault:
    @pytest.fixture(autouse=True)
    def _clean_env(self, monkeypatch):
        monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "")    # registers the restore; deleted next so the test starts unset
        monkeypatch.delenv("JUNEAU_RUN_CONSOLE")

    def test_defaults_to_condensed(self, push_module):
        push_module.parse_args(["msg"])
        assert os.environ["JUNEAU_RUN_CONSOLE"] == "condensed"

    def test_environment_beats_the_default(self, push_module, monkeypatch):
        monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "full")
        push_module.parse_args(["msg"])
        assert os.environ["JUNEAU_RUN_CONSOLE"] == "full"

    @pytest.mark.parametrize("mode", ["full", "condensed", "none"])
    def test_flag_beats_the_environment(self, push_module, monkeypatch, mode):
        monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "full" if mode != "full" else "none")
        push_module.parse_args(["msg", "--console", mode])
        assert os.environ["JUNEAU_RUN_CONSOLE"] == mode

    def test_help_says_condensed_is_the_default(self, push_module, monkeypatch, capsys):
        monkeypatch.setattr(push_module.sys, "argv", ["push.py", "--help"])
        with pytest.raises(SystemExit):
            push_module._main()
        assert "condensed (default)" in " ".join(capsys.readouterr().out.split())
