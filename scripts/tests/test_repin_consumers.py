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
Tests for scripts/repin-consumers.py: the critical dirty-tree guard (this script's entire
reason to exist -- see its module docstring), plus the small pure helpers around it (pom
version parsing, HEAD-vs-origin/master comparison).

Juneau-specific exception to this directory's usual byte-for-byte-identical-across-repos rule
(see README.md): repin-consumers.py is a Juneau-owning dev tool, not part of the carried
todo-next-id.py / todo-status-audit.py set (it re-pins the *other* two repos rather than living
in them), so this file is not copied anywhere else.

check_dirty() and main()'s guard wiring are exercised against real temporary git repositories,
following test_reset_side_clones.py's and test_push.py's own rationale for doing the same: the
guard's entire risk is real `git status --porcelain` behavior across modified/staged/
untracked-ignored cases, which a mocked subprocess would let pass while proving nothing. The
refusal path is additionally proven by monkeypatching run_streaming to raise if it is ever
called -- so "refused" means "mvn was never invoked", not just "the exit code was 1", the same
style test_push.py and test_reset_side_clones.py use for their own refusal proofs.

Nothing here ever runs a real `mvn` command (run_streaming is always stubbed) or touches the
real juneau / release-manager / sandbox-support-console checkouts.
"""

from __future__ import annotations

import importlib.util
import os
import subprocess
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent


def _load_module():
    """Load scripts/repin-consumers.py as a fresh module object (mirrors conftest.py's _load_script)."""
    path = SCRIPTS_DIR / "repin-consumers.py"
    spec = importlib.util.spec_from_file_location("_undertest_repin_consumers", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def module():
    """A fresh scripts/repin-consumers.py module object, loaded once per test."""
    return _load_module()


@pytest.fixture(autouse=True)
def hermetic_git_env(monkeypatch):
    """Isolate every git subprocess from the developer's real git configuration and identity."""
    monkeypatch.setenv("GIT_CONFIG_GLOBAL", os.devnull)
    monkeypatch.setenv("GIT_CONFIG_SYSTEM", os.devnull)
    monkeypatch.setenv("GIT_AUTHOR_NAME", "Repin Test")
    monkeypatch.setenv("GIT_AUTHOR_EMAIL", "repin-test@example.invalid")
    monkeypatch.setenv("GIT_COMMITTER_NAME", "Repin Test")
    monkeypatch.setenv("GIT_COMMITTER_EMAIL", "repin-test@example.invalid")
    monkeypatch.setenv("GIT_TERMINAL_PROMPT", "0")


def _run(cwd: Path, *argv: str) -> str:
    result = subprocess.run(argv, cwd=str(cwd), check=True, capture_output=True, text=True)
    return result.stdout.strip()


def _init_repo(path: Path) -> Path:
    path.mkdir(parents=True, exist_ok=True)
    _run(path, "git", "init", "-q", "-b", "master", ".")
    return path


def _commit_all(repo: Path, message: str) -> str:
    _run(repo, "git", "add", "-A")
    _run(repo, "git", "commit", "-q", "-m", message)
    return _run(repo, "git", "rev-parse", "HEAD")


# ---------------------------------------------------------------------------------------
# check_dirty() -- the guard's core predicate.
# ---------------------------------------------------------------------------------------
def test_check_dirty_clean_repo_is_empty(module, tmp_path):
    repo = _init_repo(tmp_path / "clean")
    _commit_all(repo, "init")
    assert module.check_dirty(repo) == []


def test_check_dirty_flags_untracked_file(module, tmp_path):
    repo = _init_repo(tmp_path / "untracked")
    _commit_all(repo, "init")
    (repo / "new.txt").write_text("junk\n", encoding="utf-8")
    dirty = module.check_dirty(repo)
    assert len(dirty) == 1
    assert dirty[0].startswith("??")
    assert "new.txt" in dirty[0]


def test_check_dirty_flags_modified_tracked_file(module, tmp_path):
    repo = _init_repo(tmp_path / "modified")
    (repo / "f.txt").write_text("a\n", encoding="utf-8")
    _commit_all(repo, "init")
    (repo / "f.txt").write_text("b\n", encoding="utf-8")
    dirty = module.check_dirty(repo)
    assert len(dirty) == 1
    assert "f.txt" in dirty[0]


def test_check_dirty_flags_staged_only_change(module, tmp_path):
    repo = _init_repo(tmp_path / "staged")
    (repo / "f.txt").write_text("a\n", encoding="utf-8")
    _commit_all(repo, "init")
    (repo / "g.txt").write_text("c\n", encoding="utf-8")
    _run(repo, "git", "add", "g.txt")
    dirty = module.check_dirty(repo)
    assert len(dirty) == 1
    assert dirty[0].startswith("A")


def test_check_dirty_ignores_gitignored_untracked_file(module, tmp_path):
    repo = _init_repo(tmp_path / "ignored")
    (repo / ".gitignore").write_text("ignored.txt\n", encoding="utf-8")
    _commit_all(repo, "init")
    (repo / "ignored.txt").write_text("junk\n", encoding="utf-8")
    assert module.check_dirty(repo) == []


# ---------------------------------------------------------------------------------------
# read_pom_version()
# ---------------------------------------------------------------------------------------
def test_read_pom_version_extracts_root_version(module, tmp_path):
    pom = tmp_path / "pom.xml"
    pom.write_text(
        "<project>\n"
        "  <groupId>org.apache.juneau</groupId>\n"
        "  <artifactId>juneau</artifactId>\n"
        "  <version>10.0.0-SNAPSHOT</version>\n"
        "  <packaging>pom</packaging>\n"
        "</project>\n",
        encoding="utf-8",
    )
    assert module.read_pom_version(pom) == "10.0.0-SNAPSHOT"


def test_read_pom_version_missing_file_returns_none(module, tmp_path):
    assert module.read_pom_version(tmp_path / "does-not-exist.xml") is None


def test_read_pom_version_unrecognized_shape_returns_none(module, tmp_path):
    pom = tmp_path / "pom.xml"
    pom.write_text("<project><version>1.0.0</version></project>\n", encoding="utf-8")
    assert module.read_pom_version(pom) is None


# ---------------------------------------------------------------------------------------
# describe_head(): HEAD vs (locally known) origin/master comparison.
# ---------------------------------------------------------------------------------------
def test_describe_head_not_unpushed_when_head_matches_origin(module, tmp_path):
    bare = tmp_path / "remote.git"
    _run(tmp_path, "git", "init", "-q", "--bare", "-b", "master", str(bare))
    repo = _init_repo(tmp_path / "work")
    (repo / "f.txt").write_text("a\n", encoding="utf-8")
    _commit_all(repo, "init")
    _run(repo, "git", "remote", "add", "origin", str(bare))
    _run(repo, "git", "push", "-q", "origin", "master")

    short_hash, subject, unpushed = module.describe_head(repo)
    assert subject == "init"
    assert unpushed is False
    assert len(short_hash) >= 7


def test_describe_head_unpushed_when_head_is_ahead(module, tmp_path):
    bare = tmp_path / "remote.git"
    _run(tmp_path, "git", "init", "-q", "--bare", "-b", "master", str(bare))
    repo = _init_repo(tmp_path / "work")
    (repo / "f.txt").write_text("a\n", encoding="utf-8")
    _commit_all(repo, "init")
    _run(repo, "git", "remote", "add", "origin", str(bare))
    _run(repo, "git", "push", "-q", "origin", "master")

    (repo / "f.txt").write_text("b\n", encoding="utf-8")
    _commit_all(repo, "unpushed change")

    _, _, unpushed = module.describe_head(repo)
    assert unpushed is True


def test_describe_head_none_when_no_origin(module, tmp_path):
    repo = _init_repo(tmp_path / "no-origin")
    _commit_all(repo, "init")
    _, _, unpushed = module.describe_head(repo)
    assert unpushed is None


# ---------------------------------------------------------------------------------------
# main(): the guard end-to-end, proven never to reach mvn when it refuses, and proven to
# actually let a clean tree through (so the guard isn't silently refusing everything).
# ---------------------------------------------------------------------------------------
def test_main_refuses_dirty_tree_without_ever_invoking_mvn(module, tmp_path, monkeypatch, capsys):
    repo = _init_repo(tmp_path / "dirty")
    _commit_all(repo, "init")
    (repo / "untracked.txt").write_text("junk\n", encoding="utf-8")

    def _boom(*args, **kwargs):
        raise AssertionError("run_streaming must never be called when the tree is dirty")

    monkeypatch.setattr(module, "run_streaming", _boom)
    monkeypatch.setattr("sys.argv", ["repin-consumers.py", "--juneau-dir", str(repo)])

    exit_code = module.main()

    assert exit_code == 1
    captured = capsys.readouterr()
    assert "REFUSED" in captured.err
    assert "untracked.txt" in captured.err
    assert "allow-dirty" in captured.err  # documents that there is no override flag


def test_main_exits_2_for_nonexistent_dir(module, tmp_path, monkeypatch):
    monkeypatch.setattr("sys.argv", ["repin-consumers.py", "--juneau-dir", str(tmp_path / "nope")])
    assert module.main() == 2


def test_main_exits_2_for_non_git_dir(module, tmp_path, monkeypatch):
    not_git = tmp_path / "not-git"
    not_git.mkdir()
    monkeypatch.setattr("sys.argv", ["repin-consumers.py", "--juneau-dir", str(not_git)])
    assert module.main() == 2


def test_main_proceeds_past_guard_when_clean(module, tmp_path, monkeypatch):
    """A clean tree must reach run_streaming -- proves the guard does not falsely refuse."""
    repo = _init_repo(tmp_path / "clean")
    _commit_all(repo, "init")

    calls = []

    def _record(argv, cwd, env):
        calls.append(argv)
        return 0  # pretend mvn succeeded so main() proceeds to the summary

    monkeypatch.setattr(module, "run_streaming", _record)
    monkeypatch.setattr("sys.argv", ["repin-consumers.py", "--juneau-dir", str(repo)])

    exit_code = module.main()

    assert exit_code == 0
    assert calls == [module.MVN_INSTALL_ARGV]


def test_main_returns_3_when_install_fails(module, tmp_path, monkeypatch):
    repo = _init_repo(tmp_path / "clean")
    _commit_all(repo, "init")

    monkeypatch.setattr(module, "run_streaming", lambda argv, cwd, env: 1)
    monkeypatch.setattr("sys.argv", ["repin-consumers.py", "--juneau-dir", str(repo)])

    assert module.main() == 3


def test_main_verify_consumers_skips_missing_dirs_and_returns_4(module, tmp_path, monkeypatch):
    """--verify-consumers reports a missing consumer dir as a failure (exit 4) without crashing."""
    repo = _init_repo(tmp_path / "clean")
    _commit_all(repo, "init")

    monkeypatch.setattr(module, "run_streaming", lambda argv, cwd, env: 0)
    monkeypatch.setattr(
        "sys.argv",
        [
            "repin-consumers.py",
            "--juneau-dir", str(repo),
            "--verify-consumers",
            "--release-manager-dir", str(tmp_path / "no-such-rm"),
            "--support-console-dir", str(tmp_path / "no-such-ssc"),
        ],
    )

    assert module.main() == 4
