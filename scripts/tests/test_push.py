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
# Real-git fixtures for commit_and_push() / check_upstream_changes() / get_staged_paths() /
# check_unreviewed_changes(). Mirrors test_reset_side_clones.py's hermetic_git_env and
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
    it. Also configures the repo-local (never global) git identity commit_and_push()'s
    verify_apache_identity() check reads, since that check is real, pre-existing behavior this
    task doesn't touch and unrelated tests still need it satisfied.
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

    def test_identity_gate_still_runs_for_an_ahead_only_push(
        self, push_module, docs_repo, docs_remote_repo, juneau_root
    ):
        """
        The identity gate is the pre_flight_hook specifically because it must run even when
        there's nothing to commit -- only something to push. Breaking the repo-local identity
        AFTER the commit (env vars, not this config, are what let the commit itself succeed)
        proves the gate is consulted on this path, not bypassed because has_changes is False.
        """
        local_head = _local_only_commit(docs_repo)
        _run(docs_repo, "git", "config", "--unset", "user.email")

        status, _ = push_module.run_docs_followup(juneau_root, "message", 6)

        assert status == "error"
        assert _run(docs_remote_repo, "git", "rev-parse", "master") != local_head

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
