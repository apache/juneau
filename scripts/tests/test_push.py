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
Tests for scripts/push.py: the end-of-run terminal bell (TestEndOfRunBell), the
refuse-on-unreviewed-changes gate, ported from the already-reviewed gate in apache/juneau's
scripts/push.py and sandbox-support-console's scripts/push.py, and the build gates (Script tests,
Compile, Tests, E2E) with their skip flags, --test-only and --dry-run. The review-gate tests cover:
the toolkit's path readers as push.py uses them, and the refuse / no-op / proceed decision (the
three refusal shapes plus the allowed staged-only case). push.py is built on the juneau_scripts
toolkit; the tests drive it through main(argv, repo_dir) and read the exit code.

Real temp git repos, real git -- no mocked subprocess. A bare repo stands in for the remote and
a real clone is the working tree; every test asserts REAL effects (did the remote ref actually
move? did a refused run actually leave the remote ref and the index untouched?) rather than just
an exit code, which a mocked subprocess would let pass while proving nothing. Identity/config is
isolated via environment variables (GIT_AUTHOR_*/GIT_COMMITTER_*), never `git config --global`.
Nothing here touches the real release-manager checkout or a real remote.

The Maven and Playwright gates are stubbed in the push_module fixture (the real ones stay reachable
as _real_compile, _real_maven_tests and _real_e2e, and their tests answer run.cmd through the cmds
fixture), so no test here builds or boots the app.
"""

from __future__ import annotations

import importlib.util
import os
import subprocess
import sys
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parents[2]


def _load_push_module():
    """Load scripts/push.py as a fresh module object."""
    path = REPO_ROOT / "scripts" / "push.py"
    spec = importlib.util.spec_from_file_location("_undertest_push", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture(autouse=True)
def clean_run_env(monkeypatch):
    """Keep the developer's shell (e.g. JUNEAU_RUN_CONSOLE=condensed) out of every push test, and forget any
    juneau_run a previous test loaded.  JUNEAU_RUN_PY / JUNEAU_SCRIPTS_ZIP stay: they locate the toolkit."""
    keep = {"JUNEAU_RUN_PY"}
    for key in [k for k in os.environ if k.startswith("JUNEAU_RUN_") and k not in keep]:
        monkeypatch.delenv(key)
    monkeypatch.delenv("RUN_MARKERS", raising=False)
    monkeypatch.delenv("RUN_ARTIFACTS", raising=False)
    monkeypatch.delitem(sys.modules, "juneau_run", raising=False)
    # Pin the plain view: push.py itself now defaults to condensed; the tests below that want the default unset this.
    monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "full")
    monkeypatch.setenv("JRM_SCRIPT_TESTS_ACTIVE", "1")  # second line of defence against a nested suite run
    monkeypatch.setenv("JRM_PUSH_NO_BELL", "1")  # keep the main()-calling tests quiet under `pytest -s`


class _Push:
    """The loaded push.py, plus commit_and_push(repo, message, dry_run=) as an exit code, via main()."""

    def __init__(self, module):
        self.module = module
        module.toolkit()

    def __getattr__(self, name):
        return getattr(self.module, name)

    def commit_and_push(self, repo, message, *, dry_run=False):
        return self.module.main([*(["--dry-run"] if dry_run else []), message], repo_dir=repo)


@pytest.fixture
def push_module():
    """A fresh scripts/push.py module object (toolkit bound), loaded once per test."""
    module = _load_push_module()
    # The Script tests step would launch this very suite from every main()-calling test (Juneau J0867: a runaway nested
    # pytest), so stub it; TestScriptTests drives the real one through _real_script_tests.
    module.gate_calls = []
    module._real_script_tests = module._script_tests
    module._script_tests = lambda *_a, **_k: module.gate_calls.append("script-tests") or True
    # The Maven and E2E gates would build and boot the real app from every main()-calling test: stub them, recording the
    # order they ran in.  The real ones stay reachable as _real_compile, _real_maven_tests and _real_e2e.
    module._real_compile, module._compile = module._compile, lambda *_a, **_k: module.gate_calls.append("compile")
    module._real_maven_tests, module._maven_tests = (
        module._maven_tests, lambda *_a, **_k: module.gate_calls.append("tests"))
    module._real_e2e, module._e2e = module._e2e, lambda *_a, **_k: module.gate_calls.append("e2e")
    return _Push(module)


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
    """Record every argv the toolkit's git helpers start, while still running real git.  subprocess.run is built on
    Popen (and the console's polled path uses Popen directly), so spying on Popen sees both."""
    calls: list[list[str]] = []
    real_popen = subprocess.Popen

    def _spy(cmd, *args, **kwargs):
        calls.append(list(cmd) if isinstance(cmd, (list, tuple)) else [cmd])
        return real_popen(cmd, *args, **kwargs)

    monkeypatch.setattr(subprocess, "Popen", _spy)
    return calls


class TestGetStagedPaths:
    """
    The sole definition of "what push.py will commit" now that `git add .` is gone: whatever
    is already in the index, because a staged path is the operator's record of having reviewed
    it.
    """

    def test_a_clean_index_reports_nothing(self, push_module, repo):
        assert push_module.git.staged_paths(repo) == []

    def test_a_staged_file_is_reported(self, push_module, repo):
        (repo / "reviewed.txt").write_text("staged by the operator\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")

        assert push_module.git.staged_paths(repo) == ["reviewed.txt"]

    def test_an_unstaged_modification_alone_is_not_reported(self, push_module, repo):
        (repo / "README.md").write_text("unstaged edit\n", encoding="utf-8")
        assert push_module.git.staged_paths(repo) == []


class TestCheckUnreviewedChanges:
    """
    The other half of the invariant: anything NOT staged. Unstaged modifications to tracked
    files and untracked, non-gitignored files are both "unreviewed"; gitignored untracked
    files are not.
    """

    def test_a_clean_tree_reports_nothing(self, push_module, repo):
        assert (push_module.git.unstaged_paths(repo) + push_module.git.untracked_paths(repo)) == []

    def test_an_unstaged_modification_to_a_tracked_file_is_reported(self, push_module, repo):
        (repo / "README.md").write_text("unstaged edit\n", encoding="utf-8")
        assert (push_module.git.unstaged_paths(repo) + push_module.git.untracked_paths(repo)) == ["README.md"]

    def test_an_untracked_non_ignored_file_is_reported(self, push_module, repo):
        (repo / "scratch.txt").write_text("brand new, not staged\n", encoding="utf-8")
        assert (push_module.git.unstaged_paths(repo) + push_module.git.untracked_paths(repo)) == ["scratch.txt"]

    def test_a_gitignored_untracked_file_is_not_reported(self, push_module, repo):
        (repo / ".gitignore").write_text("ignored.log\n", encoding="utf-8")
        _commit(repo, "add gitignore")
        (repo / "ignored.log").write_text("noise\n", encoding="utf-8")

        assert (push_module.git.unstaged_paths(repo) + push_module.git.untracked_paths(repo)) == []

    def test_a_staged_file_alone_is_not_reported(self, push_module, repo):
        """Staged content is reviewed content -- it's the other function's (get_staged_paths()) job."""
        (repo / "reviewed.txt").write_text("staged and reviewed\n", encoding="utf-8")
        _run(repo, "git", "add", "reviewed.txt")

        assert (push_module.git.unstaged_paths(repo) + push_module.git.untracked_paths(repo)) == []


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
        assert bool(push_module.git.user_email(repo)) is True

    def test_unset_identity_is_false(self, push_module, repo):
        _run(repo, "git", "config", "--unset", "user.email")
        assert bool(push_module.git.user_email(repo)) is False


class _FakeStdout:
    """Captures writes; reports whatever isatty() the test asks for."""

    def __init__(self, tty):
        self._tty = tty
        self.written = ""
        self.flushed = False

    def isatty(self):
        return self._tty

    def write(self, text):
        self.written += text

    def flush(self):
        self.flushed = True


def _run_main(push_module, monkeypatch, *, tty, identity=True, commit_result=0, commit_raises=None):
    """Run main() with commit_and_push (the Prechecks, commit and push) stubbed; return (exit_code, fake_stdout)."""
    fake = _FakeStdout(tty)
    monkeypatch.setattr(sys, "stdout", fake)

    def fake_commit_and_push(s, *_args, **_kwargs):
        if not identity:
            s.fail("❌ ERROR: no git identity configured")
        if commit_raises is not None:
            raise commit_raises
        if commit_result:
            s.fail("❌ refused")

    monkeypatch.setattr(push_module.module, "commit_and_push", fake_commit_and_push)
    return push_module.main(["msg"]), fake


class TestEndOfRunBell:
    @pytest.fixture(autouse=True)
    def clean_env(self, monkeypatch):
        monkeypatch.delenv("JRM_PUSH_NO_BELL", raising=False)
        monkeypatch.delenv("SANDBOX_RUNTIME", raising=False)
        monkeypatch.delenv("JUNEAU_SCRIPTS_QUIET", raising=False)

    def test_rings_on_tty_after_success(self, push_module, monkeypatch):
        code, fake = _run_main(push_module, monkeypatch, tty=True)
        assert code == 0
        assert fake.written.count("\a") == 1
        assert fake.written.endswith("\a")
        assert fake.flushed

    def test_rings_on_tty_after_refusal(self, push_module, monkeypatch):
        code, fake = _run_main(push_module, monkeypatch, tty=True, commit_result=1)
        assert code == 1
        assert fake.written.endswith("\a")

    def test_rings_on_tty_when_the_prechecks_fail(self, push_module, monkeypatch):
        code, fake = _run_main(push_module, monkeypatch, tty=True, identity=False)
        assert code == 1
        assert fake.written.endswith("\a")

    def test_rings_on_tty_when_cancelled(self, push_module, monkeypatch):
        code, fake = _run_main(push_module, monkeypatch, tty=True, commit_raises=KeyboardInterrupt())
        assert code == 130
        assert fake.written.endswith("\a")

    def test_silent_when_not_a_tty(self, push_module, monkeypatch):
        _, fake = _run_main(push_module, monkeypatch, tty=False)
        assert "\a" not in fake.written

    def test_still_rings_when_opt_out_is_zero(self, push_module, monkeypatch):
        monkeypatch.setenv("JRM_PUSH_NO_BELL", "0")
        monkeypatch.setenv("SANDBOX_RUNTIME", "0")
        _, fake = _run_main(push_module, monkeypatch, tty=True)
        assert fake.written.endswith("\a")

    def test_rings_and_reports_when_the_push_step_raises(self, push_module, monkeypatch):
        code, fake = _run_main(push_module, monkeypatch, tty=True,
                               commit_raises=subprocess.CalledProcessError(1, ["git", "push"]))
        assert code == 1
        assert "CalledProcessError" in fake.written
        assert fake.written.endswith("\a")

    def test_silent_when_opted_out(self, push_module, monkeypatch):
        monkeypatch.setenv("JRM_PUSH_NO_BELL", "1")
        _, fake = _run_main(push_module, monkeypatch, tty=True)
        assert "\a" not in fake.written

    def test_silent_in_sandbox(self, push_module, monkeypatch):
        monkeypatch.setenv("SANDBOX_RUNTIME", "1")
        _, fake = _run_main(push_module, monkeypatch, tty=True)
        assert "\a" not in fake.written

    def test_ring_bell_never_raises_when_stdout_misbehaves(self, push_module, monkeypatch):
        class Broken(_FakeStdout):
            def write(self, text):
                if text == "\a":
                    raise OSError("closed")
                super().write(text)

        monkeypatch.setattr(sys, "stdout", Broken(True))
        push_module.ring_bell()  # must not raise


class TestConsoleFlag:
    """--console full|condensed|none sets JUNEAU_RUN_CONSOLE before the toolkit is loaded."""

    @pytest.fixture(autouse=True)
    def clean_env(self, monkeypatch):
        monkeypatch.delenv("JRM_PUSH_NO_BELL", raising=False)
        monkeypatch.delenv("JUNEAU_RUN_CONSOLE", raising=False)

    def _main(self, push_module, monkeypatch, *flags):
        monkeypatch.setattr(sys, "stdout", _FakeStdout(False))
        monkeypatch.setattr(push_module.module, "commit_and_push", lambda *_a, **_k: None)
        return push_module.main([*flags, "msg"])

    @pytest.mark.parametrize("mode", ["full", "none"])
    def test_flag_sets_the_env_var(self, push_module, monkeypatch, mode):
        assert self._main(push_module, monkeypatch, "--console", mode) == 0
        assert os.environ["JUNEAU_RUN_CONSOLE"] == mode

    def test_flag_beats_the_env_var(self, push_module, monkeypatch):
        monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "condensed")
        assert self._main(push_module, monkeypatch, "--console", "none") == 0
        assert os.environ["JUNEAU_RUN_CONSOLE"] == "none"

    def test_env_var_is_left_alone_without_the_flag(self, push_module, monkeypatch):
        monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "full")
        assert self._main(push_module, monkeypatch) == 0
        assert os.environ["JUNEAU_RUN_CONSOLE"] == "full"

    def test_no_flag_and_no_env_var_defaults_to_condensed(self, push_module, monkeypatch):
        assert self._main(push_module, monkeypatch) == 0
        assert os.environ["JUNEAU_RUN_CONSOLE"] == "condensed"

    def test_unknown_mode_in_the_environment_is_an_error(self, push_module, monkeypatch):
        monkeypatch.setenv("JUNEAU_RUN_CONSOLE", "loud")
        assert self._main(push_module, monkeypatch) == 2

    def test_unknown_mode_is_a_usage_error(self, push_module):
        with pytest.raises(SystemExit) as exc:
            push_module.main(["--console", "loud", "msg"])
        assert exc.value.code == 2


class TestCondensedConsole:
    def test_a_push_under_condensed_succeeds_and_the_remote_moves(self, push_module, repo, remote_repo, capfd):
        (repo / "README.md").write_text("new content\n", encoding="utf-8")
        _run(repo, "git", "add", "README.md")

        assert push_module.main(["--console", "condensed", "msg"], repo_dir=repo) == 0
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_a_plain_run_loads_no_juneau_run(self, push_module, repo):
        assert push_module.main(["--dry-run", "msg"], repo_dir=repo) == 0
        assert "juneau_run" not in sys.modules


# ---------------------------------------------------------------------------------------
# The upstream gate: a branch behind its upstream is refused before the commit.
# ---------------------------------------------------------------------------------------
def _upstream_commit(repo: Path, name: str = "other.txt") -> None:
    """Land a commit on the remote's master from a second clone, leaving `repo` one commit behind (once it fetches)."""
    remote = Path(_run(repo, "git", "remote", "get-url", "origin"))
    other = repo.parent / f"other-{name}"
    _run(repo.parent, "git", "clone", str(remote), str(other))
    (other / name).write_text("o\n", encoding="utf-8")
    _commit(other, "upstream work")
    _run(other, "git", "push", "origin", "master")


def _stage(repo: Path, name: str = "README.md") -> None:
    (repo / name).write_text("new content\n", encoding="utf-8")
    _run(repo, "git", "add", name)


class TestUpstreamGate:
    def test_behind_is_refused_before_the_commit(self, push_module, repo, remote_repo, capsys):
        _stage(repo)
        _upstream_commit(repo)
        head, remote_head = _run(repo, "git", "rev-parse", "HEAD"), _run(remote_repo, "git", "rev-parse", "master")

        assert push_module.commit_and_push(repo, "msg") == 1

        out = capsys.readouterr().out
        assert "1 commit behind upstream" in out  # the row's reason
        assert "1 commit behind its upstream" in out  # the hint
        assert _run(repo, "git", "rev-parse", "HEAD") == head  # no commit was made
        assert _run(remote_repo, "git", "rev-parse", "master") == remote_head
        assert _run(repo, "git", "diff", "--cached", "--name-only") == "README.md"  # the reviewed index is untouched

    def test_behind_with_no_local_commits_hints_ff_only(self, push_module, repo, capsys):
        _stage(repo)
        _upstream_commit(repo)
        assert push_module.commit_and_push(repo, "msg") == 1
        out = capsys.readouterr().out
        assert "git merge --ff-only @{upstream}" in out
        assert "--autostash" not in out

    def test_behind_with_local_commits_hints_a_rebase_that_keeps_the_index(self, push_module, repo, capsys):
        (repo / "local.txt").write_text("l\n", encoding="utf-8")
        _commit(repo, "local")
        _upstream_commit(repo)
        _stage(repo)
        assert push_module.commit_and_push(repo, "msg") == 1
        out = capsys.readouterr().out
        assert "git stash push && git pull --rebase && git stash pop --index" in out
        assert "--ff-only" not in out and "--autostash" not in out

    def test_up_to_date_proceeds(self, push_module, repo, remote_repo):
        _stage(repo)
        assert push_module.commit_and_push(repo, "msg") == 0
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_ahead_only_proceeds(self, push_module, repo, remote_repo):
        (repo / "local.txt").write_text("l\n", encoding="utf-8")
        _commit(repo, "local")
        _stage(repo)
        assert push_module.commit_and_push(repo, "msg") == 0
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_no_upstream_proceeds_and_the_push_sets_it(self, push_module, repo, remote_repo):
        _run(repo, "git", "branch", "--unset-upstream")
        _stage(repo)
        assert push_module.commit_and_push(repo, "msg") == 0
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")
        assert _run(repo, "git", "rev-parse", "--abbrev-ref", "@{upstream}") == "origin/master"

    def test_a_fetch_error_fails_closed(self, push_module, repo, capsys):
        _stage(repo)
        _run(repo, "git", "remote", "set-url", "origin", str(repo.parent / "missing.git"))
        head = _run(repo, "git", "rev-parse", "HEAD")
        assert push_module.commit_and_push(repo, "msg") == 1
        assert "could not check upstream" in capsys.readouterr().out
        assert _run(repo, "git", "rev-parse", "HEAD") == head

    def test_dry_run_behind_reports_would_refuse_and_exits_zero(self, push_module, repo, capsys):
        _stage(repo)
        _upstream_commit(repo)
        head = _run(repo, "git", "rev-parse", "HEAD")
        assert push_module.commit_and_push(repo, "msg", dry_run=True) == 0
        out = capsys.readouterr().out
        assert "would refuse" in out and "1 commit behind" in out
        assert _run(repo, "git", "rev-parse", "HEAD") == head


# ---------------------------------------------------------------------------------------
# The Prechecks row: all checks run, and the push goes to the tracked branch.
# ---------------------------------------------------------------------------------------
class TestPrechecks:
    def test_every_check_runs_so_one_refusal_reports_all_the_problems(self, push_module, repo, capsys):
        _stage(repo)
        (repo / "scratch.txt").write_text("not staged\n", encoding="utf-8")
        _upstream_commit(repo)

        assert push_module.commit_and_push(repo, "msg") == 1

        out = capsys.readouterr().out
        assert "✗ unreviewed" in out and "✗ upstream" in out
        assert "scratch.txt" in out and "git merge --ff-only @{upstream}" in out

    def test_a_missing_identity_fails_with_the_config_command_and_never_sets_it(self, push_module, repo, capsys, git_spy):
        _run(repo, "git", "config", "--unset", "user.email")
        _stage(repo)
        git_spy.clear()

        assert push_module.commit_and_push(repo, "msg") == 1

        assert "git config user.email" in capsys.readouterr().out
        assert not any(argv[:2] == ["git", "commit"] for argv in git_spy)
        assert not any(argv[:3] == ["git", "config", "user.email"] and len(argv) > 3 and not argv[3].startswith("--")
                       for argv in git_spy), "the script must only read git config"

    def test_a_clean_tree_warns_nothing_staged(self, push_module, repo, capsys):
        assert push_module.commit_and_push(repo, "msg") == 0
        out = capsys.readouterr().out
        assert "⚠ staged" in out and "Nothing to commit" in out

    def test_dry_run_with_a_dirty_tree_still_reports_every_would_refuse(self, push_module, repo, capsys):
        _stage(repo)
        (repo / "scratch.txt").write_text("not staged\n", encoding="utf-8")
        _upstream_commit(repo)

        assert push_module.commit_and_push(repo, "msg", dry_run=True) == 0

        out = capsys.readouterr().out
        assert "⚠ unreviewed: would refuse" in out and "⚠ upstream: would refuse" in out
        assert "DRY RUN" not in out  # no would-commit report when a check would refuse


class TestPushTracked:
    def test_the_push_goes_to_the_branchs_tracked_remote_ref(self, push_module, repo, remote_repo):
        _run(repo, "git", "push", "origin", "master:topic")
        _run(repo, "git", "branch", "--set-upstream-to=origin/topic")
        _stage(repo)

        assert push_module.commit_and_push(repo, "msg") == 0

        head = _run(repo, "git", "rev-parse", "HEAD")
        assert _run(remote_repo, "git", "rev-parse", "topic") == head
        assert _run(remote_repo, "git", "rev-parse", "master") != head

    def test_a_tracked_local_branch_is_refused_after_the_commit_reports_the_local_commit(
        self, push_module, repo, capsys
    ):
        _run(repo, "git", "branch", "side")
        _run(repo, "git", "branch", "--set-upstream-to=side")
        _stage(repo)

        assert push_module.commit_and_push(repo, "msg") == 1

        out = capsys.readouterr().out
        assert "tracks a local branch" in out and "push did not complete" in out

    def test_dry_run_reports_the_real_push_command(self, push_module, repo, capsys):
        _stage(repo)
        assert push_module.commit_and_push(repo, "msg", dry_run=True) == 0
        assert "git push origin HEAD:refs/heads/master" in capsys.readouterr().out


# ---------------------------------------------------------------------------------------
# The Script tests step: the scripts/tests pytest suite, run before the commit.
# ---------------------------------------------------------------------------------------
class _Result:
    def __init__(self, code=0, out=""):
        self.ok, self.code, self.out = code == 0, code, out


class _FakeSession:
    def __init__(self):
        self.failed = None

    def fail(self, text):
        self.failed = text
        raise SystemExit(1)


@pytest.fixture
def cmds(push_module, monkeypatch):
    """Record run.cmd calls (cmd, kwargs); answer them with the code in cmds.code.  tools.maven and tools.playwright
    go through run.cmd, so this sees them too."""

    class Calls(list):
        code = 0
        out = ""

    calls = Calls()

    def fake_cmd(cmd, **kwargs):
        calls.append((list(cmd), kwargs))
        return _Result(calls.code, calls.out)

    monkeypatch.setattr(push_module.run, "cmd", fake_cmd)
    return calls


@pytest.fixture
def rows(push_module, monkeypatch):
    """Record the rows run.step opens, as {id: skip reason or None}."""
    import contextlib

    seen = {}

    @contextlib.contextmanager
    def fake_step(step_id, title, *_a, **_k):
        class St:
            def skip(self, reason=None):
                seen[step_id] = reason

            def fail(self, *_a, **_k):
                seen[step_id] = "failed"

        seen.setdefault(step_id, None)
        yield St()

    monkeypatch.setattr(push_module.run, "step", fake_step)
    return seen


class TestScriptTests:
    @pytest.fixture(autouse=True)
    def live_step(self, monkeypatch):
        monkeypatch.delenv("JRM_SCRIPT_TESTS_ACTIVE", raising=False)


    def _launcher(self, push_module, monkeypatch, *, pytest_importable, uv):
        monkeypatch.setattr(push_module.module.subprocess, "run",
                            lambda *_a, **_k: subprocess.CompletedProcess([], 0 if pytest_importable else 1))
        monkeypatch.setattr(push_module.module.shutil, "which",
                            lambda name: f"/bin/{name}" if name == "python3" or (name == "uv" and uv) else None)
        return push_module.script_tests_launcher()

    def test_launcher_prefers_importable_pytest(self, push_module, monkeypatch):
        assert self._launcher(push_module, monkeypatch, pytest_importable=True, uv=True) == ["/bin/python3", "-m", "pytest"]

    def test_launcher_falls_back_to_uv(self, push_module, monkeypatch):
        assert self._launcher(push_module, monkeypatch, pytest_importable=False, uv=True) == [
            "uv", "run", "--native-tls", "--with", "pytest", "pytest"]

    def test_launcher_is_none_without_pytest_or_uv(self, push_module, monkeypatch):
        assert self._launcher(push_module, monkeypatch, pytest_importable=False, uv=False) is None

    def test_command_runs_scripts_tests_quietly(self, push_module):
        assert push_module.script_tests_command(["p"]) == ["p", "-q", "-p", "no:cacheprovider", "scripts/tests"]

    def test_no_launcher_skips_with_a_notice(self, push_module, monkeypatch, cmds, capsys, tmp_path, rows):
        monkeypatch.setattr(push_module.module, "script_tests_launcher", lambda: None)
        assert push_module._real_script_tests(_FakeSession(), tmp_path, False) is True
        assert not cmds
        assert "Skipping Script tests" in capsys.readouterr().out
        assert rows["script-tests"]  # the row is opened and skipped, not left pending

    def test_passing_suite_runs_in_the_repo_and_lets_the_push_continue(self, push_module, cmds, tmp_path):
        assert push_module._real_script_tests(_FakeSession(), tmp_path, False, launcher=["pytest"]) is True
        cmd, kwargs = cmds[0]
        assert cmd == ["pytest", "-q", "-p", "no:cacheprovider", "scripts/tests"]
        assert kwargs["cwd"] == str(tmp_path)

    def test_failing_suite_refuses_the_push(self, push_module, cmds, tmp_path):
        cmds.code = 1
        s = _FakeSession()
        with pytest.raises(SystemExit):
            push_module._real_script_tests(s, tmp_path, False, launcher=["pytest"])
        assert "nothing committed" in s.failed

    def test_failing_suite_under_dry_run_is_a_would_refuse_warning(self, push_module, cmds, tmp_path, capsys):
        cmds.code = 1
        s = _FakeSession()
        assert push_module._real_script_tests(s, tmp_path, True, launcher=["pytest"]) is False
        assert s.failed is None
        assert "Would refuse" in capsys.readouterr().out

    def test_recursion_guard_skips_the_step(self, push_module, monkeypatch, cmds, tmp_path, capsys, rows):
        monkeypatch.setenv("JRM_SCRIPT_TESTS_ACTIVE", "1")
        assert push_module._real_script_tests(_FakeSession(), tmp_path, False, launcher=["pytest"]) is True
        assert not cmds
        assert "no recursion" in capsys.readouterr().out
        assert rows["script-tests"]

    def test_child_env_drops_the_parent_runs_protocol_variables(self, push_module, monkeypatch):
        for name in ("JUNEAU_RUN_CONSOLE", "JUNEAU_RUN_SESSION", "RUN_MARKERS", "RUN_ARTIFACTS"):
            monkeypatch.setenv(name, "x")
        monkeypatch.setenv("JUNEAU_RUN_PY", "/x/juneau_run.py")
        env = push_module.script_tests_env()
        assert not {"JUNEAU_RUN_CONSOLE", "JUNEAU_RUN_SESSION", "RUN_MARKERS", "RUN_ARTIFACTS"} & set(env)
        assert env["JUNEAU_RUN_PY"] == "/x/juneau_run.py"  # kept: the release CLI tests need it

    def test_child_env_carries_the_recursion_guard(self, push_module, monkeypatch):
        monkeypatch.setenv("JUNEAU_RUN_PY", "/x/juneau_run.py")
        env = push_module.script_tests_env()
        assert env["JRM_SCRIPT_TESTS_ACTIVE"] == "1"
        assert env["JUNEAU_RUN_PY"] == "/x/juneau_run.py"  # a set value is never replaced

    def test_unset_juneau_run_py_is_exported_when_the_canonical_file_exists(self, push_module, monkeypatch, tmp_path):
        fake = tmp_path / "juneau_run.py"
        fake.write_text("", encoding="utf-8")
        monkeypatch.delenv("JUNEAU_RUN_PY", raising=False)
        monkeypatch.setattr(push_module.module, "JUNEAU_RUN_PY_DEFAULT", fake)
        assert push_module.script_tests_env()["JUNEAU_RUN_PY"] == str(fake)

    def test_unset_juneau_run_py_stays_unset_without_the_canonical_file(self, push_module, monkeypatch, tmp_path):
        monkeypatch.delenv("JUNEAU_RUN_PY", raising=False)
        monkeypatch.setattr(push_module.module, "JUNEAU_RUN_PY_DEFAULT", tmp_path / "missing.py")
        assert "JUNEAU_RUN_PY" not in push_module.script_tests_env()

    def test_failing_suite_commits_nothing_through_main(self, push_module, repo, remote_repo, monkeypatch):
        (repo / "README.md").write_text("new content\n", encoding="utf-8")
        _run(repo, "git", "add", "README.md")
        head_before = _run(repo, "git", "rev-parse", "HEAD")
        monkeypatch.setattr(push_module.module, "_script_tests", push_module._real_script_tests)
        monkeypatch.setattr(push_module.module, "script_tests_launcher", lambda: ["pytest"])
        monkeypatch.setattr(push_module.run, "cmd", lambda *_a, **_k: _Result(1, "boom"))

        assert push_module.main(["msg"], repo_dir=repo) != 0
        assert _run(repo, "git", "rev-parse", "HEAD") == head_before
        assert _run(remote_repo, "git", "rev-parse", "master") == head_before


# ---------------------------------------------------------------------------------------
# Location: push.py lives under scripts/, like apache/juneau's and sandbox-support-console's.
# ---------------------------------------------------------------------------------------
class TestLocation:
    def test_push_lives_under_scripts_with_no_root_copy(self):
        assert (REPO_ROOT / "scripts" / "push.py").is_file()
        assert not (REPO_ROOT / "push.py").exists()

    def test_repo_dir_is_the_repo_root(self, push_module):
        assert push_module.REPO_DIR == REPO_ROOT
        assert (push_module.REPO_DIR / "pom.xml").is_file()


# ---------------------------------------------------------------------------------------
# The Compile and Tests rows: mvn clean test-compile, then mvn verify; never install.
# ---------------------------------------------------------------------------------------
class TestBuildGates:
    def test_compile_runs_clean_test_compile_in_the_repo(self, push_module, cmds, tmp_path):
        push_module._real_compile(_FakeSession(), tmp_path, False)
        cmd, kwargs = cmds[0]
        assert cmd == ["mvn", "clean", "test-compile"]
        assert (kwargs["cwd"], kwargs["tool"], kwargs["id"], kwargs["title"]) == (str(tmp_path), "maven", "compile",
                                                                                   "Compile")

    def test_tests_runs_verify_without_clean(self, push_module, cmds, tmp_path):
        push_module._real_maven_tests(_FakeSession(), tmp_path, False)
        cmd, kwargs = cmds[0]
        assert cmd == ["mvn", "verify"]
        assert (kwargs["cwd"], kwargs["id"], kwargs["title"]) == (str(tmp_path), "tests", "Tests")

    def test_neither_row_ever_installs(self, push_module, cmds, tmp_path):
        push_module._real_compile(_FakeSession(), tmp_path, False)
        push_module._real_maven_tests(_FakeSession(), tmp_path, False)
        assert len(cmds) == 2
        assert all("install" not in cmd for cmd, _ in cmds)

    @pytest.mark.parametrize("gate, title", [("_real_compile", "Compile"), ("_real_maven_tests", "Tests")])
    def test_a_failure_refuses_the_push(self, push_module, cmds, tmp_path, gate, title):
        cmds.code = 1
        s = _FakeSession()
        with pytest.raises(SystemExit):
            getattr(push_module, gate)(s, tmp_path, False)
        assert f"{title} failed" in s.failed and "nothing committed" in s.failed

    @pytest.mark.parametrize("gate, title", [("_real_compile", "Compile"), ("_real_maven_tests", "Tests")])
    def test_dry_run_reports_would_run_and_runs_nothing(self, push_module, cmds, tmp_path, capsys, gate, title):
        getattr(push_module, gate)(_FakeSession(), tmp_path, True)
        assert not cmds
        assert f"Would run {title}: mvn" in capsys.readouterr().out

    def test_java_home_defaults_to_the_personal_jdk_when_unset(self, push_module, monkeypatch, tmp_path):
        monkeypatch.delenv("JAVA_HOME", raising=False)
        monkeypatch.setattr(push_module.module, "DEFAULT_JAVA_HOME", tmp_path)
        assert push_module.build_env()["JAVA_HOME"] == str(tmp_path)

    def test_a_set_java_home_is_kept(self, push_module, monkeypatch):
        monkeypatch.setenv("JAVA_HOME", "/opt/jdk")
        assert push_module.build_env()["JAVA_HOME"] == "/opt/jdk"

    def test_the_maven_child_gets_java_home_and_its_bin_on_the_path(self, push_module, cmds, monkeypatch, tmp_path):
        monkeypatch.setenv("JAVA_HOME", "/opt/jdk")
        push_module._real_compile(_FakeSession(), tmp_path, False)
        env = cmds[0][1]["env"]
        assert env["JAVA_HOME"] == "/opt/jdk"
        assert env["PATH"].startswith("/opt/jdk/bin")

    def test_the_default_jdk_is_passed_to_maven_when_java_home_is_unset(self, push_module, cmds, monkeypatch,
                                                                         tmp_path):
        monkeypatch.delenv("JAVA_HOME", raising=False)
        monkeypatch.setattr(push_module.module, "DEFAULT_JAVA_HOME", tmp_path)
        push_module._real_compile(_FakeSession(), tmp_path, False)
        env = cmds[0][1]["env"]
        assert env["JAVA_HOME"] == str(tmp_path)
        assert env["PATH"].startswith(str(tmp_path / "bin"))

    def test_a_missing_default_jdk_leaves_the_environment_alone(self, push_module, cmds, monkeypatch, tmp_path):
        monkeypatch.delenv("JAVA_HOME", raising=False)
        monkeypatch.setattr(push_module.module, "DEFAULT_JAVA_HOME", tmp_path / "missing")
        assert push_module.java_home() is None
        assert "JAVA_HOME" not in push_module.build_env()
        push_module._real_compile(_FakeSession(), tmp_path, False)
        assert "JAVA_HOME" not in (cmds[0][1].get("env") or {})

    def test_the_toolkit_cache_is_outside_target(self, push_module):
        # Compile runs `mvn clean`, which would delete a copy extracted under <repo>/target.
        assert push_module.REPO_DIR / "target" not in [push_module.TOOLKIT_CACHE_DIR,
                                                       *push_module.TOOLKIT_CACHE_DIR.parents]

    def test_maven_parser_is_none_without_a_run_view(self, push_module, monkeypatch):
        monkeypatch.delitem(sys.modules, "juneau_run", raising=False)
        assert push_module.maven_parser() is None


# ---------------------------------------------------------------------------------------
# The E2E row: the full Playwright suite in e2e/, through the playwright parser.
# ---------------------------------------------------------------------------------------
class TestE2EGate:
    @pytest.fixture
    def npx(self, push_module, monkeypatch):
        """npx on the PATH, and a playwright_ensure that installs nothing; returns the directories it was asked for."""
        monkeypatch.setattr(push_module.module.shutil, "which", lambda name: f"/bin/{name}")
        ensured = []
        monkeypatch.setattr(push_module.tools, "playwright_ensure", lambda d: ensured.append(d) or _Result(0))
        return ensured

    def test_runs_the_full_suite_in_e2e_with_the_playwright_parser(self, push_module, cmds, npx, tmp_path):
        push_module._real_e2e(_FakeSession(), tmp_path, False)
        cmd, kwargs = cmds[0]
        assert cmd == ["npx", "playwright", "test"]
        assert (kwargs["tool"], kwargs["id"], kwargs["title"]) == ("playwright", "e2e", "E2E")
        assert kwargs["cwd"] == str(tmp_path / "e2e")
        assert not any(a.startswith("--reporter") for a in cmd)
        assert npx == [tmp_path / "e2e"]

    def test_the_html_report_never_opens_by_default(self, push_module, cmds, npx, monkeypatch, tmp_path):
        monkeypatch.delenv("PLAYWRIGHT_HTML_OPEN", raising=False)
        push_module._real_e2e(_FakeSession(), tmp_path, False)
        assert cmds[0][1]["env"]["PLAYWRIGHT_HTML_OPEN"] == "never"

    def test_a_set_html_open_is_kept(self, push_module, cmds, npx, monkeypatch, tmp_path):
        monkeypatch.setenv("PLAYWRIGHT_HTML_OPEN", "on-failure")
        push_module._real_e2e(_FakeSession(), tmp_path, False)
        assert cmds[0][1]["env"]["PLAYWRIGHT_HTML_OPEN"] == "on-failure"

    @pytest.mark.parametrize("name", ["JRM_E2E_REUSE", "JRM_E2E_BASE_URL", "JRM_E2E_HOME"])
    def test_variables_that_redirect_the_suite_are_removed(self, push_module, cmds, npx, monkeypatch, tmp_path, name):
        monkeypatch.setenv(name, "1")
        push_module._real_e2e(_FakeSession(), tmp_path, False)
        assert name not in cmds[0][1]["env"]

    def test_the_e2e_child_gets_java_home(self, push_module, cmds, npx, monkeypatch, tmp_path):
        monkeypatch.setenv("JAVA_HOME", "/opt/jdk")
        push_module._real_e2e(_FakeSession(), tmp_path, False)
        assert cmds[0][1]["env"]["JAVA_HOME"] == "/opt/jdk"

    def test_a_failing_suite_refuses_the_push(self, push_module, cmds, npx, tmp_path):
        cmds.code = 1
        s = _FakeSession()
        with pytest.raises(SystemExit):
            push_module._real_e2e(s, tmp_path, False)
        assert "E2E failed" in s.failed and "nothing committed" in s.failed

    def test_a_failed_install_fails_the_gate_before_the_suite(self, push_module, cmds, monkeypatch, tmp_path):
        monkeypatch.setattr(push_module.module.shutil, "which", lambda name: f"/bin/{name}")
        monkeypatch.setattr(push_module.tools, "playwright_ensure", lambda _d: _Result(1))
        s = _FakeSession()
        with pytest.raises(SystemExit):
            push_module._real_e2e(s, tmp_path, False)
        assert "E2E setup failed" in s.failed
        assert not cmds

    def test_missing_npx_skips_with_a_warning_and_lets_the_push_continue(self, push_module, cmds, monkeypatch,
                                                                          tmp_path, capsys):
        monkeypatch.setattr(push_module.module.shutil, "which", lambda _name: None)
        s = _FakeSession()
        push_module._real_e2e(s, tmp_path, False)
        assert s.failed is None and not cmds
        assert "Skipping E2E: npx not found" in capsys.readouterr().out

    def test_dry_run_reports_would_run_and_runs_nothing(self, push_module, cmds, npx, tmp_path, capsys):
        push_module._real_e2e(_FakeSession(), tmp_path, True)
        assert not cmds and not npx
        assert "Would run E2E: npx playwright test" in capsys.readouterr().out


# ---------------------------------------------------------------------------------------
# The step plan, the skip flags, --test-only and the gate order through main().
# ---------------------------------------------------------------------------------------
ALL_GATES = ["script-tests", "compile", "tests", "e2e"]


class TestPlannedSteps:
    def _plan(self, push_module, *argv):
        return [step for step, _ in push_module.planned_steps(push_module.build_parser().parse_args(list(argv)))]

    def test_a_push_plans_every_step_in_order(self, push_module):
        assert self._plan(push_module, "msg") == ["prechecks", *ALL_GATES, "commit", "push"]

    def test_the_labels(self, push_module):
        assert dict(push_module.planned_steps(push_module.build_parser().parse_args(["msg"]))) == {
            "prechecks": "Prechecks", "script-tests": "Script tests", "compile": "Compile", "tests": "Tests",
            "e2e": "E2E", "commit": "Commit", "push": "Push"}

    @pytest.mark.parametrize("flag, removed", [("--skip-pytest", ["script-tests"]),
                                               ("--skip-tests", ["compile", "tests"]),
                                               ("--skip-e2e", ["e2e"])])
    def test_each_skip_flag_removes_exactly_its_steps(self, push_module, flag, removed):
        full = ["prechecks", *ALL_GATES, "commit", "push"]
        assert self._plan(push_module, flag, "msg") == [s for s in full if s not in removed]

    def test_test_only_plans_the_gates_alone(self, push_module):
        assert self._plan(push_module, "--test-only") == ALL_GATES


class TestGatesThroughMain:
    def test_a_push_runs_the_gates_in_order_then_commits_and_pushes(self, push_module, repo, remote_repo):
        _stage(repo)
        assert push_module.main(["msg"], repo_dir=repo) == 0
        assert push_module.gate_calls == ALL_GATES
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")

    def test_a_clean_tree_runs_no_gate(self, push_module, repo):
        assert push_module.main(["msg"], repo_dir=repo) == 0
        assert push_module.gate_calls == []

    def test_a_refused_precheck_runs_no_gate(self, push_module, repo):
        _stage(repo)
        (repo / "scratch.txt").write_text("not staged\n", encoding="utf-8")
        assert push_module.main(["msg"], repo_dir=repo) == 1
        assert push_module.gate_calls == []

    @pytest.mark.parametrize("flag, expected, line", [
        ("--skip-pytest", ["compile", "tests", "e2e"], "Skipping Script tests (--skip-pytest)"),
        ("--skip-tests", ["script-tests", "e2e"], "Skipping Compile and Tests (--skip-tests)"),
        ("--skip-e2e", ["script-tests", "compile", "tests"], "Skipping E2E (--skip-e2e)"),
    ])
    def test_each_skip_flag_skips_exactly_its_gates_and_says_so(self, push_module, repo, capsys, flag, expected,
                                                                 line):
        _stage(repo)
        assert push_module.main([flag, "msg"], repo_dir=repo) == 0
        assert push_module.gate_calls == expected
        assert line in capsys.readouterr().out

    @pytest.mark.parametrize("gate, step, ran", [("_compile", "compile", ["script-tests", "compile"]),
                                                 ("_maven_tests", "tests", ["script-tests", "compile", "tests"]),
                                                 ("_e2e", "e2e", ALL_GATES)])
    def test_a_failing_gate_commits_nothing_and_runs_no_later_gate(self, push_module, repo, remote_repo,
                                                                   monkeypatch, gate, step, ran):
        _stage(repo)
        head = _run(repo, "git", "rev-parse", "HEAD")

        def failing(s, *_a, **_k):
            push_module.gate_calls.append(step)
            s.fail(f"❌ ERROR: {step} failed (exit code 1); nothing committed.")

        monkeypatch.setattr(push_module.module, gate, failing)
        assert push_module.main(["msg"], repo_dir=repo) == 1
        assert push_module.gate_calls == ran
        assert _run(repo, "git", "rev-parse", "HEAD") == head
        assert _run(remote_repo, "git", "rev-parse", "master") == head
        assert _run(repo, "git", "diff", "--cached", "--name-only") == "README.md"  # the reviewed index is untouched

    def test_dry_run_reaches_every_build_gate_as_a_dry_run(self, push_module, repo, monkeypatch):
        _stage(repo)
        seen = []
        for name in ("_compile", "_maven_tests", "_e2e"):
            monkeypatch.setattr(push_module.module, name,
                                lambda _s, _repo, dry_run, _name=name: seen.append((_name, dry_run)))
        assert push_module.main(["--dry-run", "msg"], repo_dir=repo) == 0
        assert seen == [("_compile", True), ("_maven_tests", True), ("_e2e", True)]


class TestTestOnly:
    def test_runs_every_gate_on_a_dirty_tree_and_commits_nothing(self, push_module, repo, remote_repo, git_spy):
        _stage(repo)
        (repo / "scratch.txt").write_text("not staged\n", encoding="utf-8")
        head = _run(repo, "git", "rev-parse", "HEAD")
        git_spy.clear()

        assert push_module.main(["--test-only"], repo_dir=repo) == 0

        assert push_module.gate_calls == ALL_GATES
        assert _run(repo, "git", "rev-parse", "HEAD") == head
        assert _run(remote_repo, "git", "rev-parse", "master") == head
        assert not any(argv[:2] in (["git", "commit"], ["git", "push"]) for argv in git_spy)

    def test_runs_the_gates_on_a_clean_tree_too(self, push_module, repo):
        assert push_module.main(["--test-only"], repo_dir=repo) == 0
        assert push_module.gate_calls == ALL_GATES

    def test_says_nothing_was_committed(self, push_module, repo, capsys):
        assert push_module.main(["--test-only"], repo_dir=repo) == 0
        assert "nothing committed or pushed" in capsys.readouterr().out

    def test_skip_flags_still_apply(self, push_module, repo):
        assert push_module.main(["--test-only", "--skip-e2e"], repo_dir=repo) == 0
        assert push_module.gate_calls == ["script-tests", "compile", "tests"]


class TestFlagCombinations:
    def test_all_three_skips_run_the_prechecks_then_commit_and_push(self, push_module, repo, remote_repo):
        _stage(repo)
        assert push_module.main(["--skip-pytest", "--skip-tests", "--skip-e2e", "msg"], repo_dir=repo) == 0
        assert push_module.gate_calls == []
        assert _run(remote_repo, "git", "rev-parse", "master") == _run(repo, "git", "rev-parse", "HEAD")
        assert _run(repo, "git", "log", "-1", "--format=%s") == "msg"

    def test_test_only_with_dry_run_never_commits_or_pushes(self, push_module, repo, remote_repo, git_spy):
        _stage(repo)
        head = _run(repo, "git", "rev-parse", "HEAD")
        git_spy.clear()
        assert push_module.main(["--test-only", "--dry-run"], repo_dir=repo) == 0
        assert _run(repo, "git", "rev-parse", "HEAD") == head
        assert _run(remote_repo, "git", "rev-parse", "master") == head
        assert not any(argv[:2] in (["git", "commit"], ["git", "push"]) for argv in git_spy)

    def test_dry_run_skips_the_commit_and_push_rows(self, push_module, repo, rows):
        _stage(repo)
        assert push_module.main(["--dry-run", "msg"], repo_dir=repo) == 0
        assert rows["commit"] == "dry run" and rows["push"] == "dry run"

    def test_test_only_dry_run_opens_no_commit_or_push_row(self, push_module, repo, rows):
        assert push_module.main(["--test-only", "--dry-run"], repo_dir=repo) == 0
        assert "commit" not in rows and "push" not in rows
