# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""
Tests for the root push.py's refuse-on-unreviewed-changes gate (WORK-R0006), ported from the
already-reviewed gate in apache/juneau's scripts/push.py and sandbox-support-console's
scripts/push.py. This repo's push.py is a bare add/commit/push helper with no other gates, so
this file covers only: check_unreviewed_changes(), get_staged_paths(), and commit_and_push()'s
refuse / no-op / proceed decision (the three refusal shapes plus the allowed staged-only case).

Real temp git repos, real git -- no mocked subprocess. A bare repo stands in for the remote and
a real clone is the working tree; every test asserts REAL effects (did the remote ref actually
move? did a refused run actually leave the remote ref and the index untouched?) rather than just
an exit code, which a mocked subprocess would let pass while proving nothing. Identity/config is
isolated via environment variables (GIT_AUTHOR_*/GIT_COMMITTER_*), never `git config --global`.
Nothing here touches the real release-manager checkout or a real remote.

Structural difference from the juneau/ssc references (per WORK-R0006's spec): this repo's
push.py lives at the repo ROOT, not under scripts/, so the module is loaded from
Path(__file__).resolve().parents[2] / "push.py" rather than SCRIPTS_DIR / "push.py".
"""

from __future__ import annotations

import importlib.util
import os
import subprocess
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parents[2]


def _load_push_module():
    """Load the root push.py as a fresh module object."""
    path = REPO_ROOT / "push.py"
    spec = importlib.util.spec_from_file_location("_undertest_push", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def push_module():
    """A fresh root push.py module object, loaded once per test."""
    return _load_push_module()


# ---------------------------------------------------------------------------------------
# Real-git fixtures for commit_and_push() / get_staged_paths() / check_unreviewed_changes().
# Mirrors juneau's scripts/tests/test_push.py fixtures.
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
    that is already pushed and with a repo-local git identity configured (so the identity
    gate in main() -- unchanged, out of scope for this item -- is satisfied for tests that go
    through main() rather than commit_and_push() directly).
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
    _run(work, "git", "config", "user.email", "push-test@example.invalid")
    _run(work, "git", "config", "user.name", "Push Test")
    return work


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
    files are not.
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


class TestCommitAndPushGenuineNoOp:
    """A fully clean tree (nothing staged, nothing unstaged/untracked) is a benign no-op, not a refusal."""

    def test_returns_zero_and_touches_nothing(self, push_module, repo, remote_repo, git_spy):
        remote_before = _run(remote_repo, "git", "rev-parse", "master")

        exit_code = push_module.commit_and_push(repo, "irrelevant message")

        assert exit_code == 0
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "add"] for argv in git_spy)


class TestCommitAndPushUnreviewedChangesGuard:
    """
    The three refusal shapes named in the Acceptance: nothing staged while the tree is dirty,
    staged + unstaged-tracked, and staged + untracked-non-ignored. Each must refuse -- commit
    and push NOTHING -- and leave the remote ref and the index exactly as found.
    """

    def test_nothing_staged_but_tree_dirty_is_refused(self, push_module, repo, remote_repo, git_spy):
        (repo / "mid-edit.txt").write_text("still being worked on, nothing staged\n", encoding="utf-8")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")

        exit_code = push_module.commit_and_push(repo, "irrelevant message")

        assert exit_code == 1
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "add"] for argv in git_spy)

    def test_staged_plus_unstaged_tracked_modification_is_refused(self, push_module, repo, remote_repo, git_spy):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "README.md").write_text("mid-edit, not staged\n", encoding="utf-8")
        head_before = _run(repo, "git", "rev-parse", "HEAD")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")
        git_spy.clear()  # discard the setup `git add` above; only commit_and_push()'s own git calls matter here

        exit_code = push_module.commit_and_push(repo, "my commit message")

        assert exit_code == 1
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before, "nothing should have been committed"
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before, "the remote must not move"
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)

    def test_staged_plus_untracked_non_ignored_file_is_refused(self, push_module, repo, remote_repo):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "scratch.txt").write_text("brand new, not staged\n", encoding="utf-8")

        exit_code = push_module.commit_and_push(repo, "message")

        assert exit_code == 1
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_a_gitignored_untracked_file_does_not_trip_the_guard(self, push_module, repo, remote_repo):
        (repo / ".gitignore").write_text("build.log\n", encoding="utf-8")
        _run(repo, "git", "add", ".gitignore")
        (repo / "build.log").write_text("noise\n", encoding="utf-8")

        exit_code = push_module.commit_and_push(repo, "add gitignore")

        assert exit_code == 0
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_the_staged_file_remains_staged_and_the_unstaged_file_remains_unstaged(self, push_module, repo):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "mine.txt").write_text("mid-edit, not staged\n", encoding="utf-8")

        push_module.commit_and_push(repo, "my commit message")

        assert _run(repo, "git", "diff", "--cached", "--name-only") == "reviewed.txt"
        status_lines = _run(repo, "git", "status", "--porcelain").splitlines()
        assert "?? mine.txt" in status_lines

    def test_the_error_message_names_the_unreviewed_paths(self, push_module, repo, capsys):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "secret-in-progress.txt").write_text("do not push yet\n", encoding="utf-8")

        push_module.commit_and_push(repo, "message")

        assert "secret-in-progress.txt" in capsys.readouterr().out

    def test_the_error_message_includes_a_copy_pasteable_stash_recovery_hint(self, push_module, repo, capsys):
        """
        The recovery hint has to be discoverable at the moment of refusal, and it has to be the
        REAL command for THIS refusal -- with the actual offending paths already filled in.
        """
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "one.txt").write_text("mid-edit one\n", encoding="utf-8")
        (repo / "two.txt").write_text("mid-edit two\n", encoding="utf-8")

        push_module.commit_and_push(repo, "message")
        out = capsys.readouterr().out

        assert "git stash push -- one.txt two.txt" in out
        assert "git stash pop" in out

    def test_the_error_message_says_why_the_refusal_exists(self, push_module, repo, capsys):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "mid-edit.txt").write_text("still being worked on\n", encoding="utf-8")

        push_module.commit_and_push(repo, "message")

        assert "should be something you've read" in capsys.readouterr().out.lower()

    def test_partial_review_is_refused_in_full_not_partially_pushed(self, push_module, repo, remote_repo, git_spy):
        """
        Some files reviewed and staged, others still mid-edit: the whole run is refused rather
        than pushing just the reviewed part.
        """
        (repo / "reviewed-a.txt").write_text("reviewed a\n", encoding="utf-8")
        (repo / "reviewed-b.txt").write_text("reviewed b\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed-a.txt", "reviewed-b.txt")
        (repo / "mid-edit.txt").write_text("still being worked on\n", encoding="utf-8")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")
        git_spy.clear()

        exit_code = push_module.commit_and_push(repo, "reviewed work")

        assert exit_code == 1
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)


class TestCommitAndPushDirtyTreeProceeds:
    """
    The allowed case (Acceptance's 4th case): staged only, tree otherwise clean -- commits the
    staged index and pushes. Unlike before, staging is now the caller's job -- push.py commits
    the index as it finds it -- so every test here stages explicitly rather than relying on a
    (now removed) `git add .`.
    """

    def test_staged_changes_are_committed_with_the_given_message_and_pushed(self, push_module, repo, remote_repo):
        (repo / "README.md").write_text("new content\n", encoding="utf-8")
        _run(repo, "git", "add", "README.md")

        exit_code = push_module.commit_and_push(repo, "the commit message")

        assert exit_code == 0
        assert _run(repo, "git", "log", "-1", "--format=%s") == "the commit message"
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_a_staged_untracked_file_is_committed_too(self, push_module, repo, remote_repo):
        (repo / "new-file.txt").write_text("brand new\n", encoding="utf-8")
        _run(repo, "git", "add", "new-file.txt")

        exit_code = push_module.commit_and_push(repo, "add new file")

        assert exit_code == 0
        assert _run(remote_repo, "git", "show", "HEAD:new-file.txt") == "brand new"

    def test_the_commit_contains_exactly_the_staged_paths(self, push_module, repo, remote_repo):
        (repo / "reviewed.txt").write_text("staged\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")

        push_module.commit_and_push(repo, "exact commit")

        changed = _run(repo, "git", "diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD").splitlines()
        assert changed == ["reviewed.txt"]

    def test_git_add_is_never_invoked(self, push_module, repo, remote_repo, git_spy):
        """push.py no longer decides what to stage -- the index already says (see docstring)."""
        (repo / "README.md").write_text("new content\n", encoding="utf-8")
        _run(repo, "git", "add", "README.md")
        git_spy.clear()  # discard the setup `git add` above; only commit_and_push()'s own git calls matter here

        push_module.commit_and_push(repo, "the commit message")

        assert not any(argv[:2] == ["git", "add"] for argv in git_spy)


class TestCommitAndPushDryRun:
    """--dry-run must report the real outcome and never mutate the repo or the remote."""

    def test_dry_run_on_a_refusing_tree_reports_would_refuse_exits_zero_and_touches_nothing(
        self, push_module, repo, remote_repo, git_spy
    ):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "mid-edit.txt").write_text("still being worked on\n", encoding="utf-8")
        head_before = _run(repo, "git", "rev-parse", "HEAD")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")
        git_spy.clear()

        exit_code = push_module.commit_and_push(repo, "message", dry_run=True)

        assert exit_code == 0
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)

    def test_dry_run_on_a_refusing_tree_mentions_refuse_in_the_output(self, push_module, repo, capsys):
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")
        (repo / "mid-edit.txt").write_text("still being worked on\n", encoding="utf-8")

        push_module.commit_and_push(repo, "message", dry_run=True)

        assert "refuse" in capsys.readouterr().out.lower()

    def test_dry_run_on_a_clean_staged_tree_reports_would_commit_and_touches_nothing(
        self, push_module, repo, remote_repo, git_spy
    ):
        (repo / "README.md").write_text("new content\n", encoding="utf-8")
        _run(repo, "git", "add", "README.md")
        head_before = _run(repo, "git", "rev-parse", "HEAD")
        remote_before = _run(remote_repo, "git", "rev-parse", "master")
        git_spy.clear()

        exit_code = push_module.commit_and_push(repo, "would-be message", dry_run=True)

        assert exit_code == 0
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_before
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:2] == ["git", "push"] for argv in git_spy)

    def test_dry_run_output_never_mentions_git_add(self, push_module, repo, capsys):
        (repo / "README.md").write_text("new content\n", encoding="utf-8")
        _run(repo, "git", "add", "README.md")

        push_module.commit_and_push(repo, "would-be message", dry_run=True)

        assert "git add" not in capsys.readouterr().out


class TestGitIdentityConfigured:
    def test_configured_identity_is_true(self, push_module, repo):
        assert push_module.git_identity_configured(repo) is True

    def test_unset_identity_is_false(self, push_module, repo):
        _run(repo, "git", "config", "--unset", "user.email")
        assert push_module.git_identity_configured(repo) is False
