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
Tests for scripts/reset-side-clones.py -- the post-harvest side-clone reset.

Juneau-only, like test_push.py: this script exists in juneau only (see README.md), so unlike
conftest.py / test_todo_next_id.py / test_todo_status_audit.py this file is NOT one of the
byte-for-byte-identical-across-repos set, and reset-side-clones.py is loaded here rather than
from a conftest fixture for exactly that reason.

WHAT IS ACTUALLY WORTH ASSERTING HERE. The script's entire risk is real git behavior on real
working trees, so almost everything below builds REAL temporary git repositories -- init, commit,
clone, diverge -- and lets the script drive real git against them. A mocked git would let every
one of these tests pass while the script did something catastrophic to an actual clone, which is
the opposite of proof. Mocking appears in exactly two shapes, both deliberate:

  * subprocess.run is monkeypatched to RAISE, to prove a guard fires before any git runs at all
    (the strongest available form of "it refused"), and
  * subprocess.run is wrapped in a recording spy that still calls the real subprocess.run, so an
    end-to-end apply can be audited for a forbidden argv.

HERMETICITY. Every test passes an explicit --board pointing at a temp file, so the real
~/Project Work/repos.md is never read and never written. The real side clones are never touched:
the only tests that mention a real path are the canonical-refusal ones, which name the real
canonical trees on purpose -- the guard has to be tested against the real constant to mean
anything -- and they assert that NO git command ran, so naming the path is all they do to it.
Temp repos get their identity and config isolation from environment variables (never `git config`,
which the wave skill forbids outright and which this script refuses to run).
"""

from __future__ import annotations

import importlib.util
import inspect
import os
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent

BOARD_HEADER = "| path | project | role | branch | WAVE / session | status |"
BOARD_SEPARATOR = "|---|---|---|---|---|---|"

# The real canonical trees, spelled here so the canonical guard is tested against the real
# constant. Nothing in this file ever runs a command against them.
REAL_CANONICAL_JUNEAU = Path.home() / "git" / "apache" / "juneau"


def _load_reset_module():
    """
    Load scripts/reset-side-clones.py as a fresh module object (mirrors conftest.py).

    Registered in sys.modules before exec_module, unlike conftest.py's loader: this script uses
    `from __future__ import annotations` together with @dataclass, and dataclasses resolves the
    string annotations by looking the defining module up in sys.modules.
    """
    path = SCRIPTS_DIR / "reset-side-clones.py"
    spec = importlib.util.spec_from_file_location("_undertest_reset_side_clones", path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    try:
        spec.loader.exec_module(module)
    except Exception:
        sys.modules.pop(spec.name, None)
        raise
    return module


@pytest.fixture
def mod():
    """A fresh scripts/reset-side-clones.py module object, one per test."""
    return _load_reset_module()


@pytest.fixture(autouse=True)
def hermetic_git_env(monkeypatch):
    """
    Isolate every git subprocess from the developer's real git configuration and identity.

    Done entirely with environment variables because `git config` must never run -- the wave
    skill forbids it and the script under test refuses it, so a test that reached for it would
    be violating the very rule it is here to check.
    """
    monkeypatch.setenv("GIT_CONFIG_GLOBAL", os.devnull)
    monkeypatch.setenv("GIT_CONFIG_SYSTEM", os.devnull)
    monkeypatch.setenv("GIT_AUTHOR_NAME", "Reset Test")
    monkeypatch.setenv("GIT_AUTHOR_EMAIL", "reset-test@example.invalid")
    monkeypatch.setenv("GIT_COMMITTER_NAME", "Reset Test")
    monkeypatch.setenv("GIT_COMMITTER_EMAIL", "reset-test@example.invalid")
    monkeypatch.setenv("GIT_TERMINAL_PROMPT", "0")


# ---------------------------------------------------------------------------------------
# Real-git fixtures.
# ---------------------------------------------------------------------------------------
def _run(cwd: Path, *argv: str) -> str:
    result = subprocess.run(argv, cwd=str(cwd), check=True, capture_output=True, text=True)
    return result.stdout.strip()


def _commit(repo: Path, message: str) -> str:
    _run(repo, "git", "add", "-A")
    _run(repo, "git", "commit", "-m", message)
    return _run(repo, "git", "rev-parse", "HEAD")


@pytest.fixture
def origin_repo(tmp_path: Path) -> Path:
    """
    A real git repository standing in for a canonical tree, on `master` (the juneau pool's
    default branch), carrying one of the script's declared sentinel paths.
    """
    repo = tmp_path / "canonical"
    repo.mkdir()
    _run(repo, "git", "init", "-b", "master", ".")
    (repo / "scripts").mkdir()
    (repo / "scripts" / "todo-next-id.py").write_text("# v1\n", encoding="utf-8")
    (repo / "README.md").write_text("first\n", encoding="utf-8")
    (repo / ".gitignore").write_text(".work/\n", encoding="utf-8")
    _commit(repo, "initial")
    return repo


@pytest.fixture
def side_clone(tmp_path: Path, origin_repo: Path) -> Path:
    """A real clone of origin_repo, with a gitignored .work/ tree like a real side clone."""
    clone = tmp_path / "clone-2"
    _run(tmp_path, "git", "clone", str(origin_repo), str(clone))
    work = clone / ".work"
    work.mkdir()
    (work / "brainstorm.md").write_text("do not lose me\n", encoding="utf-8")
    (work / "nested").mkdir()
    (work / "nested" / "deep.md").write_text("nor me\n", encoding="utf-8")
    return clone


def advance_origin(origin_repo: Path) -> str:
    """Put a new commit on the canonical tree so the clone is genuinely behind."""
    (origin_repo / "scripts" / "todo-next-id.py").write_text("# v2 -- the fix that mattered\n", encoding="utf-8")
    (origin_repo / "NEW.md").write_text("added upstream\n", encoding="utf-8")
    return _commit(origin_repo, "second")


def board_file(
    tmp_path: Path,
    rows: list[tuple[str, str, str, str, str, str]],
    *,
    name: str = "repos.md",
    prose: str = "Living assignment table. Never reset canonical trees.",
) -> Path:
    """Write a temp repos.md whose table matches the real board's shape."""
    lines = ["# Clone board", "", prose, "", BOARD_HEADER, BOARD_SEPARATOR]
    lines += ["| " + " | ".join(cells) + " |" for cells in rows]
    lines += ["", "Skill: ~/Project Work/skills/todo-and-waves/SKILL.md."]
    path = tmp_path / name
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def juneau_row(clone: Path, *, session: str = " ", status: str = "idle", branch: str = "master"):
    return (f"`{clone}`", "Juneau", "worker clone", f"`{branch}`", session, status)


@pytest.fixture
def no_git_allowed(mod, monkeypatch):
    """
    Make ANY subprocess invocation an immediate failure.

    Used by the refusal tests: "the guard fired" is proven far more strongly by "git was never
    invoked" than by "the exit code was 1".
    """
    def _boom(*args, **kwargs):
        raise AssertionError(f"no subprocess may run here, but got: {args!r}")

    monkeypatch.setattr(mod.subprocess, "run", _boom)


@pytest.fixture
def git_spy(mod, monkeypatch):
    """Record every git argv while still running real git."""
    calls: list[list[str]] = []
    real_run = subprocess.run

    def _spy(cmd, *args, **kwargs):
        calls.append(list(cmd))
        return real_run(cmd, *args, **kwargs)

    monkeypatch.setattr(mod.subprocess, "run", _spy)
    return calls


# ---------------------------------------------------------------------------------------
class TestGitWrapperRefusesConfig:
    """`git config` must be unreachable in every form, including a read-only --get."""

    @pytest.mark.parametrize(
        "argv",
        [
            ["config", "user.email", "x@y.z"],
            ["config", "--get", "user.email"],
            ["config", "--global", "user.email", "x@y.z"],
            ["config", "--local", "--unset", "user.email"],
        ],
    )
    def test_config_is_refused(self, mod, side_clone, argv, no_git_allowed):
        with pytest.raises(mod.GitGuardViolation, match="forbidden"):
            mod._git(side_clone, argv)

    def test_forbidden_subcommand_set_contains_config(self, mod):
        assert "config" in mod.FORBIDDEN_GIT_SUBCOMMANDS

    def test_no_git_config_anywhere_in_a_full_apply(self, mod, tmp_path, origin_repo, side_clone, git_spy):
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0

        assert git_spy, "the apply path must actually have run git"
        for argv in git_spy:
            assert "config" not in argv, f"forbidden `git config` in {argv}"


class TestGitCleanCannotReachIgnoredFiles:
    """
    `git clean -x` is the unrecoverable mistake in this procedure (it deletes .work/), so the
    tests below are about UNREACHABILITY, not about remembering to avoid it.
    """

    def test_clean_argv_constant_is_exactly_fd(self, mod):
        assert mod.GIT_CLEAN_ARGV == ("clean", "-fd")

    def test_git_clean_helper_has_no_flag_parameter(self, mod):
        """There is no parameter through which a caller could pass -x."""
        params = list(inspect.signature(mod._git_clean).parameters)
        assert params == ["clone"]

    @pytest.mark.parametrize(
        "argv",
        [
            ["clean", "-fdx"],
            ["clean", "-fdX"],
            ["clean", "-xfd"],
            ["clean", "-fd", "-x"],
            ["clean", "-fd", "-X"],
            ["clean", "-f"],  # not exactly GIT_CLEAN_ARGV either
            ["clean", "-fd", "--", "."],
        ],
    )
    def test_any_other_clean_argv_is_refused(self, mod, side_clone, argv, no_git_allowed):
        with pytest.raises(mod.GitGuardViolation):
            mod._git(side_clone, argv)

    @pytest.mark.parametrize("argv", [["log", "-x"], ["checkout", "-fx", "master"], ["reset", "-X"]])
    def test_ignored_file_flags_are_refused_in_any_subcommand(self, mod, side_clone, argv, no_git_allowed):
        with pytest.raises(mod.GitGuardViolation):
            mod._git(side_clone, argv)

    def test_clean_helper_actually_runs_the_permitted_form(self, mod, side_clone, git_spy):
        mod._git_clean(side_clone)
        cleans = [argv for argv in git_spy if "clean" in argv]
        assert cleans == [["git", "-C", str(side_clone), "clean", "-fd"]]

    def test_work_dir_survives_a_real_clean(self, mod, side_clone):
        """The point of -fd over -fdx, checked against real git rather than argued about."""
        (side_clone / "untracked.txt").write_text("remove me\n", encoding="utf-8")
        mod._git_clean(side_clone)
        assert not (side_clone / "untracked.txt").exists()
        assert (side_clone / ".work" / "brainstorm.md").read_text(encoding="utf-8") == "do not lose me\n"

    def test_no_ignored_file_flag_anywhere_in_a_full_apply(self, mod, tmp_path, origin_repo, side_clone, git_spy):
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0

        for argv in git_spy:
            for token in argv:
                assert not mod._is_untracked_ignored_flag(token), f"ignored-file flag {token!r} in {argv}"


class TestCanonicalTreesAreRefused:
    """
    Refusal by RESOLVED REAL PATH, so a symlink or a relative invocation cannot slip past.

    These tests name the real canonical trees on purpose -- testing the guard against a
    substituted constant would prove nothing -- and assert that no git command runs, so naming
    the path is the only thing that happens to it.
    """

    def test_git_wrapper_refuses_a_canonical_tree_outright(self, mod, no_git_allowed):
        with pytest.raises(mod.GitGuardViolation, match="canonical tree"):
            mod._git(REAL_CANONICAL_JUNEAU, ["status", "--porcelain"])

    def test_all_three_skill_named_canonical_trees_are_listed(self, mod):
        names = {p.name for p in mod.CANONICAL_TREES}
        assert names == {"juneau", "irs-1", "sandbox-support-console"}

    def test_dry_run_refuses_a_canonical_row_without_running_git(self, mod, tmp_path, no_git_allowed, capsys):
        board = board_file(tmp_path, [(f"`{REAL_CANONICAL_JUNEAU}`", "Juneau", "worker clone", "`master`", " ", "idle")])

        exit_code = mod.main(["--clones", str(REAL_CANONICAL_JUNEAU), "--board", str(board)])

        assert exit_code == 1
        assert "canonical_tree" in capsys.readouterr().out

    def test_apply_also_refuses_a_canonical_row_without_running_git(self, mod, tmp_path, no_git_allowed, capsys):
        """--apply must not be a way around this. The guard is not a dry-run-only courtesy."""
        board = board_file(tmp_path, [(f"`{REAL_CANONICAL_JUNEAU}`", "Juneau", "worker clone", "`master`", " ", "idle")])

        exit_code = mod.main(["--clones", str(REAL_CANONICAL_JUNEAU), "--board", str(board), "--apply"])

        assert exit_code == 1
        out = capsys.readouterr().out
        assert "canonical_tree" in out
        assert "0 reset" in out

    def test_a_symlink_to_a_canonical_tree_is_refused(self, mod, tmp_path, no_git_allowed, capsys):
        """Creating a symlink does not touch its target; resolving it is what the guard does."""
        link = tmp_path / "innocent-looking-clone"
        link.symlink_to(REAL_CANONICAL_JUNEAU)
        board = board_file(tmp_path, [(f"`{link}`", "Juneau", "worker clone", "`master`", " ", "idle")])

        exit_code = mod.main(["--clones", str(link), "--board", str(board), "--apply"])

        assert exit_code == 1
        assert "canonical_tree" in capsys.readouterr().out

    def test_a_dot_dot_path_into_a_canonical_tree_is_refused(self, mod, tmp_path, no_git_allowed, capsys):
        sneaky = REAL_CANONICAL_JUNEAU.parent / "juneau-2" / ".." / "juneau"
        board = board_file(tmp_path, [(f"`{sneaky}`", "Juneau", "worker clone", "`master`", " ", "idle")])

        exit_code = mod.main(["--clones", str(sneaky), "--board", str(board), "--apply"])

        assert exit_code == 1
        assert "canonical_tree" in capsys.readouterr().out

    def test_a_board_row_marked_canonical_is_refused_even_for_a_temp_repo(
        self, mod, tmp_path, side_clone, capsys
    ):
        """The board's own `canonical` role is honoured, so a new canonical tree needs no code change."""
        board = board_file(tmp_path, [(f"`{side_clone}`", "Juneau", "canonical", "`master`", " ", "idle")])

        exit_code = mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"])

        assert exit_code == 1
        assert "canonical_row" in capsys.readouterr().out

    def test_a_path_not_on_the_board_cannot_be_selected(self, mod, tmp_path, side_clone, no_git_allowed):
        board = board_file(tmp_path, [juneau_row(tmp_path / "some-other-clone")])
        with pytest.raises(SystemExit) as exc:
            mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"])
        assert exc.value.code == 4


class TestInFlightIsRefused:
    """A clone another subagent is using must never be reset out from under it."""

    def test_in_flight_row_is_refused_and_no_git_runs(self, mod, tmp_path, side_clone, no_git_allowed, capsys):
        board = board_file(tmp_path, [juneau_row(side_clone, session="WAVE-0004 slice a", status="in-flight")])

        exit_code = mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"])

        assert exit_code == 1
        assert "in_flight" in capsys.readouterr().out

    def test_in_flight_clone_is_bit_for_bit_untouched(self, mod, tmp_path, origin_repo, side_clone):
        advance_origin(origin_repo)
        (side_clone / ".work" / "brainstorm.md").write_text("still here\n", encoding="utf-8")
        head_before = _run(side_clone, "git", "rev-parse", "HEAD")
        board = board_file(tmp_path, [juneau_row(side_clone, status="in-flight")])
        board_before = board.read_bytes()

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 1

        assert _run(side_clone, "git", "rev-parse", "HEAD") == head_before
        assert (side_clone / ".work" / "brainstorm.md").read_text(encoding="utf-8") == "still here\n"
        assert board.read_bytes() == board_before

    def test_do_not_use_row_is_refused_on_status_alone(self, mod, tmp_path, side_clone, no_git_allowed, capsys):
        """
        A clone holding the operator's own branch is marked `do-not-use` on the board, and that
        status has to be the whole protection by itself.

        The clone here is CLEAN, and no git may run (no_git_allowed), so neither the dirty check
        nor anything else about the tree can be what saved it -- the board said so and that was
        enough. This is the property that matters for a clone whose working tree is dirty today:
        the moment its owner commits, a dirty-tree check stops firing, and only the status is
        left. The assertion that `dirty` is absent from the report is the other half of the
        proof: the dirty check lives past this guard and was never reached.
        """
        board = board_file(tmp_path, [juneau_row(side_clone, status="do-not-use")])

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 1

        out = capsys.readouterr().out
        assert "do_not_use" in out
        assert "dirty" not in out

    def test_do_not_use_is_a_static_guard_evaluated_before_any_git(self, mod, tmp_path, side_clone):
        """Same property one level down, at the function that decides it."""
        row = mod.parse_board(
            board_file(tmp_path, [juneau_row(side_clone, status="do-not-use")]).read_text(encoding="utf-8")
        )[0]

        assessment = mod.assess_static(row, frozenset())

        assert not assessment.ok
        assert [code for code, _ in assessment.refusals] == ["do_not_use"]
        assert assessment.dirty == [], "the dirty check must not even have been consulted"

    def test_an_idle_row_still_carrying_a_wave_assignment_is_refused(self, mod, tmp_path, side_clone, capsys):
        """
        A self-contradicting board -- status idle but a WAVE still assigned -- is refused rather
        than believed. Harvest is supposed to clear that cell before the reset, so its presence
        means the board and reality have diverged.
        """
        board = board_file(tmp_path, [juneau_row(side_clone, session="WAVE-0004")])
        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 1
        assert "board_inconsistent" in capsys.readouterr().out

    def test_a_free_form_session_note_is_cleared_not_refused(self, mod, tmp_path, origin_repo, side_clone, capsys):
        """
        The skill's step 7 says the reset empties the session cell, so a plain note must not be a
        refusal -- but it is echoed so nothing a human wrote disappears silently.
        """
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone, session="stash: some WIP")])

        assert mod.main(["--clones", str(side_clone), "--board", str(board)]) == 0

        assert "stash: some WIP" in capsys.readouterr().out


class TestDirtyTreesAbort:
    """
    Uncommitted work is reported, never reset over. All nine clones were clean the night this was
    written; the check exists for the night one is not.
    """

    def test_unstaged_modification_aborts_and_survives(self, mod, tmp_path, origin_repo, side_clone, capsys):
        advance_origin(origin_repo)
        (side_clone / "README.md").write_text("precious local edit\n", encoding="utf-8")
        board = board_file(tmp_path, [juneau_row(side_clone)])

        exit_code = mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"])

        assert exit_code == 1
        assert "dirty" in capsys.readouterr().out
        assert (side_clone / "README.md").read_text(encoding="utf-8") == "precious local edit\n"

    def test_staged_work_aborts_and_stays_staged(self, mod, tmp_path, origin_repo, side_clone, capsys):
        advance_origin(origin_repo)
        (side_clone / "README.md").write_text("staged edit\n", encoding="utf-8")
        _run(side_clone, "git", "add", "README.md")
        board = board_file(tmp_path, [juneau_row(side_clone)])

        exit_code = mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"])

        assert exit_code == 1
        out = capsys.readouterr().out
        assert "dirty" in out and "1 staged" in out
        assert _run(side_clone, "git", "diff", "--cached", "--name-only") == "README.md"

    def test_untracked_file_aborts(self, mod, tmp_path, origin_repo, side_clone, capsys):
        """Untracked non-ignored work is uncommitted work, and `git clean -fd` would delete it."""
        advance_origin(origin_repo)
        (side_clone / "scratch.txt").write_text("not committed anywhere\n", encoding="utf-8")
        board = board_file(tmp_path, [juneau_row(side_clone)])

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 1
        assert "dirty" in capsys.readouterr().out
        assert (side_clone / "scratch.txt").exists()

    def test_a_gitignored_work_tree_is_not_dirt(self, mod, tmp_path, origin_repo, side_clone):
        """.work/ must not itself trip the dirty guard, or nothing would ever be resettable."""
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])
        assert mod.main(["--clones", str(side_clone), "--board", str(board)]) == 0


class TestUnreadableBoardFailsClosed:
    """
    An unreadable board must never be interpreted as "nothing is in flight". That is the
    fail-open version of the in-flight guard, so every one of these exits 2 having run no git.
    """

    def test_missing_board_exits_2_without_running_git(self, mod, tmp_path, no_git_allowed, capsys):
        exit_code = mod.main(["--all", "--board", str(tmp_path / "nope.md"), "--apply"])
        assert exit_code == 2
        assert "Nothing was touched" in capsys.readouterr().err

    def test_undecodable_board_exits_2(self, mod, tmp_path, no_git_allowed):
        board = tmp_path / "repos.md"
        board.write_bytes(b"| path | project |\n\xff\xfe\x00 not utf-8")
        assert mod.main(["--all", "--board", str(board), "--apply"]) == 2

    def test_board_with_no_table_exits_2(self, mod, tmp_path, no_git_allowed):
        board = tmp_path / "repos.md"
        board.write_text("# Clone board\n\nAll the prose and no table at all.\n", encoding="utf-8")
        assert mod.main(["--all", "--board", str(board), "--apply"]) == 2

    def test_board_with_unexpected_columns_exits_2(self, mod, tmp_path, no_git_allowed):
        board = tmp_path / "repos.md"
        board.write_text("| path | project | status |\n|---|---|---|\n| `a` | Juneau | idle |\n", encoding="utf-8")
        assert mod.main(["--all", "--board", str(board), "--apply"]) == 2

    def test_board_with_a_malformed_row_exits_2(self, mod, tmp_path, no_git_allowed):
        board = tmp_path / "repos.md"
        board.write_text(
            f"{BOARD_HEADER}\n{BOARD_SEPARATOR}\n| `a` | Juneau | worker clone | `master` | idle |\n",
            encoding="utf-8",
        )
        assert mod.main(["--all", "--board", str(board), "--apply"]) == 2

    def test_board_with_a_header_but_no_rows_exits_2(self, mod, tmp_path, no_git_allowed):
        board = tmp_path / "repos.md"
        board.write_text(f"{BOARD_HEADER}\n{BOARD_SEPARATOR}\n\nprose\n", encoding="utf-8")
        assert mod.main(["--all", "--board", str(board), "--apply"]) == 2

    def test_header_without_a_separator_row_exits_2(self, mod, tmp_path, no_git_allowed):
        board = tmp_path / "repos.md"
        board.write_text(
            f"{BOARD_HEADER}\n| `a` | Juneau | worker clone | `master` |  | idle |\n", encoding="utf-8"
        )
        assert mod.main(["--all", "--board", str(board), "--apply"]) == 2

    def test_a_board_shaped_like_the_real_one_parses(self, mod):
        """
        A parser that fails closed is only useful if it accepts the real board's shape -- without
        this, every test above would still pass if parse_board rejected everything.

        The header and a representative row are pinned as literals copied from
        ~/Project Work/repos.md rather than read from it, so the suite stays hermetic. If the real
        board's column spelling ever drifts from BOARD_COLUMNS, this is the test that says so.
        """
        text = (
            "# Clone board\n\nProse, and a table that starts with a pipe-free paragraph.\n\n"
            "| path | project | role | branch | WAVE / session | status |\n"
            "|---|---|---|---|---|---|\n"
            "| `~/git/apache/juneau` | Juneau | canonical | `master` | at `d9f87f97f8` | dirty-unstaged |\n"
            "| `~/git/apache/juneau-2` | Juneau | worker clone | `master` | | idle |\n"
            "| `~/git/sandbox/sandbox-support-console-1` | Console | worker clone | `main` | | idle |\n"
            "| `~/git/apache/juneau-9.2.1` | Juneau 9.2.1 | maintenance | `x` | | do-not-use |\n"
            "\nSkill: ~/Project Work/skills/todo-and-waves/SKILL.md.\n"
        )

        rows = mod.parse_board(text)

        assert len(rows) == 4
        assert [r.pool for r in rows] == ["juneau", "juneau", "console", None]
        assert rows[0].is_canonical_row
        assert rows[1].role == "worker clone" and rows[1].status == "idle"
        assert rows[2].branch == "main"


class TestDefaultIsADryRun:
    """The safe mode has to be the one you get by typing the command wrong."""

    def test_no_selection_is_a_usage_error_not_an_everything_run(self, mod, tmp_path, side_clone, no_git_allowed):
        board = board_file(tmp_path, [juneau_row(side_clone)])
        with pytest.raises(SystemExit) as exc:
            mod.main(["--board", str(board)])
        assert exc.value.code == 4

    def test_dry_run_changes_nothing_at_all(self, mod, tmp_path, origin_repo, side_clone):
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])
        head_before = _run(side_clone, "git", "rev-parse", "HEAD")
        board_before = board.read_bytes()

        assert mod.main(["--clones", str(side_clone), "--board", str(board)]) == 0

        assert _run(side_clone, "git", "rev-parse", "HEAD") == head_before
        assert board.read_bytes() == board_before

    def test_dry_run_does_not_even_fetch(self, mod, tmp_path, origin_repo, side_clone, git_spy):
        """A dry run stays read-only, so it cannot move remote-tracking refs either."""
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])

        mod.main(["--clones", str(side_clone), "--board", str(board)])

        assert not any("fetch" in argv for argv in git_spy)

    def test_dry_run_prints_the_commands_it_would_run(self, mod, tmp_path, side_clone, capsys):
        board = board_file(tmp_path, [juneau_row(side_clone)])
        mod.main(["--clones", str(side_clone), "--board", str(board)])
        out = capsys.readouterr().out
        assert "DRY RUN" in out
        assert "fetch origin" in out
        assert "checkout master" in out
        assert "reset --hard origin/master" in out
        assert "clean -fd" in out


class TestApplyActuallyResets:
    """The happy path, against real git, since that is the only kind of proof that counts."""

    def test_clone_is_brought_to_the_canonical_commit(self, mod, tmp_path, origin_repo, side_clone, capsys):
        target = advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0

        assert _run(side_clone, "git", "rev-parse", "HEAD") == target
        assert (side_clone / "scripts" / "todo-next-id.py").read_text(encoding="utf-8") == (
            "# v2 -- the fix that mattered\n"
        )
        assert "content verified" in capsys.readouterr().out

    def test_a_leftover_branch_checkout_is_returned_to_master(self, mod, tmp_path, origin_repo, side_clone):
        target = advance_origin(origin_repo)
        _run(side_clone, "git", "checkout", "-b", "cu/leftover")
        board = board_file(tmp_path, [juneau_row(side_clone, branch="cu/leftover")])

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0

        assert _run(side_clone, "git", "rev-parse", "--abbrev-ref", "HEAD") == "master"
        assert _run(side_clone, "git", "rev-parse", "HEAD") == target

    def test_untracked_files_are_cleaned_but_work_is_preserved(self, mod, tmp_path, origin_repo, side_clone):
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])
        # Committed then reverted upstream is not the case here; this file arrives during the
        # reset window, after the dirty check, the way a build artifact would.
        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0
        assert (side_clone / ".work" / "brainstorm.md").read_text(encoding="utf-8") == "do not lose me\n"
        assert (side_clone / ".work" / "nested" / "deep.md").exists()

    def test_apply_is_idempotent(self, mod, tmp_path, origin_repo, side_clone):
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])
        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0
        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0


class TestWorkDirectoryPreservation:
    """.work/ is backed up before the clone is touched and verified after."""

    def test_backup_and_restore_round_trip(self, mod, side_clone):
        backup = mod.backup_work(side_clone)
        assert backup is not None and backup.files == 2

        (side_clone / ".work" / "brainstorm.md").unlink()
        restored, verified = mod.restore_work(backup)

        assert [str(p) for p in restored] == ["brainstorm.md"]
        assert verified
        assert (side_clone / ".work" / "brainstorm.md").read_text(encoding="utf-8") == "do not lose me\n"

    def test_backup_lands_outside_the_clone(self, mod, side_clone):
        """A backup inside the clone could be reached by the very commands it protects against."""
        backup = mod.backup_work(side_clone)
        assert side_clone not in backup.dest.parents

    def test_absent_work_dir_is_not_an_error(self, mod, tmp_path, origin_repo):
        clone = tmp_path / "no-work"
        _run(tmp_path, "git", "clone", str(origin_repo), str(clone))
        assert mod.backup_work(clone) is None
        assert mod.restore_work(None) == ([], True)


class TestContentVerification:
    """Compare file CONTENT against the canonical blob, not the commit hash."""

    def test_tampered_content_is_caught_while_head_still_matches(self, mod, tmp_path, origin_repo, side_clone):
        """
        The whole argument for this check in one test: HEAD is correct, and the file is wrong.
        Anything that trusted the commit id would call this a clean reset.
        """
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])
        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0

        (side_clone / "scripts" / "todo-next-id.py").write_text("# tampered\n", encoding="utf-8")

        with pytest.raises(mod.ResetFailed, match="content mismatch"):
            mod.verify_content(side_clone, "origin/master")

    def test_declared_sentinels_are_used_when_present(self, mod, side_clone):
        paths, note = mod.verify_content(side_clone, "origin/master")
        assert paths == ["scripts/todo-next-id.py"]
        assert note == "declared sentinels"

    def test_a_tree_without_sentinels_still_verifies_something(self, mod, tmp_path):
        """
        "Nothing to verify" must never be reportable as a pass -- an IRS clone carries none of
        the declared sentinel paths, so the fallback sample is the real code path there.
        """
        repo = tmp_path / "sentinel-free"
        repo.mkdir()
        _run(repo, "git", "init", "-b", "master", ".")
        for name in ("a.txt", "b.txt", "c.txt"):
            (repo / name).write_text(f"{name}\n", encoding="utf-8")
        _commit(repo, "no sentinels here")
        clone = tmp_path / "sentinel-free-clone"
        _run(tmp_path, "git", "clone", str(repo), str(clone))

        paths, note = mod.verify_content(clone, "origin/master")

        assert paths == ["a.txt", "b.txt", "c.txt"]
        assert "deterministic sample" in note

    def test_an_empty_target_tree_is_a_failure_not_a_pass(self, mod, tmp_path):
        repo = tmp_path / "empty"
        repo.mkdir()
        _run(repo, "git", "init", "-b", "master", ".")
        _run(repo, "git", "commit", "--allow-empty", "-m", "empty")
        with pytest.raises(mod.ResetFailed, match="nothing could be content-verified"):
            mod.verify_content(repo, "HEAD")


class TestCuBranchDeletionIsConservative:
    """
    Step 5 only where a script can prove safety. "Already harvested" is not mechanically
    knowable -- harvest lands unstaged on canonical, reachable from no ref -- so containment in
    the target ref is the substitute, and everything else is left for a human.
    """

    def test_a_contained_branch_is_deleted(self, mod, tmp_path, origin_repo, side_clone):
        advance_origin(origin_repo)
        _run(side_clone, "git", "fetch", "origin")
        _run(side_clone, "git", "branch", "cu/contained", "origin/master")
        board = board_file(tmp_path, [juneau_row(side_clone)])

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0

        branches = _run(side_clone, "git", "for-each-ref", "--format=%(refname:short)", "refs/heads")
        assert "cu/contained" not in branches.split()

    def test_a_branch_with_unique_commits_is_kept_and_reported(self, mod, tmp_path, origin_repo, side_clone, capsys):
        advance_origin(origin_repo)
        _run(side_clone, "git", "checkout", "-b", "cu/unharvested")
        (side_clone / "unique.txt").write_text("only here\n", encoding="utf-8")
        _commit(side_clone, "unique work")
        _run(side_clone, "git", "checkout", "master")
        board = board_file(tmp_path, [juneau_row(side_clone)])

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0

        out = capsys.readouterr().out
        assert "cu/* KEPT for a human" in out
        assert "cu/unharvested" in out
        branches = _run(side_clone, "git", "for-each-ref", "--format=%(refname:short)", "refs/heads")
        assert "cu/unharvested" in branches.split()


class TestRemoteCrossingAndDisagreement:
    """The IRS/Apache crossing rule, and the one genuinely ambiguous step in the skill."""

    def test_a_remote_pointing_into_another_projects_canonical_tree_is_refused(
        self, mod, tmp_path, origin_repo, capsys
    ):
        """An IRS-pool clone whose remote resolves into the Apache canonical tree is refused."""
        clone = tmp_path / "crossed"
        _run(tmp_path, "git", "clone", str(origin_repo), str(clone))
        _run(clone, "git", "remote", "add", "apache", str(REAL_CANONICAL_JUNEAU))
        board = board_file(tmp_path, [(f"`{clone}`", "IRS", "IRS-only", "`master`", " ", "idle")])

        exit_code = mod.main(["--clones", str(clone), "--board", str(board), "--apply"])

        assert exit_code == 1
        out = capsys.readouterr().out
        assert "remote_crossing" in out
        assert "juneau canonical tree" in out

    def test_disagreeing_canonical_remotes_refuse_rather_than_pick_one(self, mod, tmp_path, origin_repo, side_clone):
        """
        The skill says to reset to "origin/main or the local-canonical remote's main" without
        saying which wins, so a disagreement is an operator decision, not a coin flip.
        """
        other = tmp_path / "other-canonical"
        other.mkdir()
        _run(other, "git", "init", "-b", "master", ".")
        (other / "different.txt").write_text("divergent history\n", encoding="utf-8")
        _commit(other, "different")
        _run(side_clone, "git", "remote", "add", "second", str(other))
        _run(side_clone, "git", "fetch", "second")

        assessment = mod.Assessment(
            row=mod.parse_board(
                board_file(tmp_path, [juneau_row(side_clone)]).read_text(encoding="utf-8")
            )[0],
            pool="juneau",
            branch="master",
        )
        assessment.canonical_remotes = ["origin", "second"]

        with pytest.raises(mod.ResetFailed, match="disagree"):
            mod._resolve_target(assessment)

    def test_all_excludes_the_irs_pool_and_says_so(self, mod, tmp_path, side_clone, capsys):
        """The exclusion is printed, never silent."""
        irs = tmp_path / "irs-clone"
        _run(tmp_path, "git", "clone", str(side_clone), str(irs))
        board = board_file(
            tmp_path,
            [juneau_row(side_clone), (f"`{irs}`", "IRS", "IRS-only", "`master`", " ", "idle")],
        )

        mod.main(["--all", "--board", str(board)])

        out = capsys.readouterr().out
        assert "IRS clones are excluded" in out
        assert str(irs) not in out

    def test_pool_irs_includes_them(self, mod, tmp_path, side_clone, capsys):
        irs = tmp_path / "irs-clone"
        _run(tmp_path, "git", "clone", str(side_clone), str(irs))
        board = board_file(tmp_path, [(f"`{irs}`", "IRS", "IRS-only", "`master`", " ", "idle")])

        mod.main(["--pool", "irs", "--board", str(board)])

        assert str(irs) in capsys.readouterr().out

    def test_pool_defaults_match_the_skills_table(self, mod):
        assert mod.POOL_DEFAULT_BRANCH == {"juneau": "master", "console": "main", "irs": "master"}


class TestBoardRowUpdate:
    """Step 7, and the concurrency question it raises."""

    def test_successful_reset_updates_only_its_own_row(self, mod, tmp_path, origin_repo, side_clone):
        advance_origin(origin_repo)
        other = tmp_path / "clone-3"
        _run(tmp_path, "git", "clone", str(origin_repo), str(other))
        board = board_file(
            tmp_path,
            [
                juneau_row(side_clone, session="harvested, notes elsewhere", status="dirty-unstaged"),
                juneau_row(other, status="in-flight"),
            ],
        )
        before = board.read_text(encoding="utf-8").splitlines()

        assert mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"]) == 0

        after = board.read_text(encoding="utf-8").splitlines()
        assert len(before) == len(after)
        changed = [i for i, (b, a) in enumerate(zip(before, after)) if b != a]
        assert len(changed) == 1
        row = after[changed[0]]
        assert "`master`" in row and row.rstrip().endswith("idle |")
        assert "harvested, notes elsewhere" not in row

    def test_a_held_lock_reports_the_change_instead_of_writing(self, mod, tmp_path, origin_repo, side_clone, capsys):
        """A mangled shared board is worse than a row someone has to paste in by hand."""
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])
        lock = board.with_name(board.name + ".lock")
        lock.write_text("99999\n", encoding="utf-8")
        before = board.read_bytes()

        exit_code = mod.main(["--clones", str(side_clone), "--board", str(board), "--apply"])

        assert exit_code == 5
        assert board.read_bytes() == before
        out = capsys.readouterr().out
        assert "Apply these rows by hand" in out
        assert lock.exists(), "someone else's lock must not be removed"

    def test_no_board_update_flag_reports_instead_of_writing(self, mod, tmp_path, origin_repo, side_clone, capsys):
        advance_origin(origin_repo)
        board = board_file(tmp_path, [juneau_row(side_clone)])
        before = board.read_bytes()

        exit_code = mod.main(
            ["--clones", str(side_clone), "--board", str(board), "--apply", "--no-board-update"]
        )

        assert exit_code == 5
        assert board.read_bytes() == before
        assert "Apply these rows by hand" in capsys.readouterr().out

    def test_render_row_leaves_the_other_columns_alone(self, mod):
        """Matches the real board's own spacing for an emptied cell (`| |`)."""
        raw = "| `~/git/apache/juneau-2` | Juneau | worker clone | `cu/x` | WAVE-0004 | in-flight |"
        out = mod.render_row(raw, "master", "idle")
        assert out == "| `~/git/apache/juneau-2` | Juneau | worker clone | `master` | | idle |"

    def test_write_refuses_a_rewrite_that_no_longer_parses(self, mod, tmp_path, side_clone):
        """A rewrite that would leave an unparseable board is never installed."""
        board = board_file(tmp_path, [juneau_row(side_clone)])
        before = board.read_bytes()
        rows = mod.parse_board(board.read_text(encoding="utf-8"))

        with pytest.raises(mod.BoardError):
            mod.write_board(board, {rows[0].index: "| totally | different | number | of | cells | here | oops |"})

        assert board.read_bytes() == before
        assert not board.with_name(board.name + ".lock").exists()

    def test_a_board_that_changed_under_us_refuses_the_write(self, mod, tmp_path):
        """
        The case _assert_only_intended_lines_changed exists for: another writer edited a
        different row between the read that produced the plan and this write.
        """
        original = "a\nb\nc\n"
        updated = "a\nB-CHANGED\nc-also-changed\n"
        with pytest.raises(mod.BoardError, match="unintended lines"):
            mod._assert_only_intended_lines_changed(original, updated, {1})

    def test_a_write_that_changes_line_count_is_refused(self, mod):
        with pytest.raises(mod.BoardError, match="line count"):
            mod._assert_only_intended_lines_changed("a\nb\n", "a\nb\nc\n", {1})

    def test_lock_is_released_after_a_successful_write(self, mod, tmp_path, side_clone):
        board = board_file(tmp_path, [juneau_row(side_clone)])
        rows = mod.parse_board(board.read_text(encoding="utf-8"))
        mod.write_board(board, {rows[0].index: mod.render_row(rows[0].raw, "master", "idle")})
        assert not board.with_name(board.name + ".lock").exists()


class TestSelectionErrors:
    def test_unknown_pool_is_a_usage_error(self, mod, tmp_path, side_clone, no_git_allowed):
        board = board_file(tmp_path, [juneau_row(side_clone)])
        with pytest.raises(SystemExit) as exc:
            mod.main(["--pool", "nonesuch", "--board", str(board)])
        assert exc.value.code == 4

    def test_ambiguous_clone_name_is_a_usage_error(self, mod, tmp_path, origin_repo, no_git_allowed):
        a = tmp_path / "a" / "clone-2"
        b = tmp_path / "b" / "clone-2"
        a.parent.mkdir()
        b.parent.mkdir()
        board = board_file(tmp_path, [juneau_row(a), juneau_row(b)])
        with pytest.raises(SystemExit) as exc:
            mod.main(["--clones", "clone-2", "--board", str(board)])
        assert exc.value.code == 4

    def test_a_missing_directory_is_refused_not_crashed(self, mod, tmp_path, capsys):
        board = board_file(tmp_path, [juneau_row(tmp_path / "gone")])
        assert mod.main(["--clones", str(tmp_path / "gone"), "--board", str(board), "--apply"]) == 1
        assert "missing_directory" in capsys.readouterr().out

    def test_a_non_git_directory_is_refused(self, mod, tmp_path, capsys):
        plain = tmp_path / "not-a-repo"
        plain.mkdir()
        board = board_file(tmp_path, [juneau_row(plain)])
        assert mod.main(["--clones", str(plain), "--board", str(board), "--apply"]) == 1
        assert "not_a_git_worktree" in capsys.readouterr().out

    def test_a_subdirectory_of_a_clone_is_refused(self, mod, tmp_path, side_clone, capsys):
        inner = side_clone / "scripts"
        board = board_file(tmp_path, [juneau_row(inner)])
        assert mod.main(["--clones", str(inner), "--board", str(board), "--apply"]) == 1
        assert "not_worktree_root" in capsys.readouterr().out
