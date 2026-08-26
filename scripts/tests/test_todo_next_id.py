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
Tests for scripts/todo-next-id.py -- the atomic TODO-id allocator.

Two failure modes motivate this suite, both of the "produces no error, just wrong" kind:

1. CONCURRENCY (the historical bug): the old allocator computed `next = 1 + max(existing
   ids)` and simply returned it -- a read, not a reservation. Two sessions racing between
   their scan and their write could be (and were) handed the SAME id. claim_next_id() fixes
   this by atomically creating "TODO-<id>-CLAIMED.md" with os.open(O_CREAT | O_EXCL) before
   ever returning a candidate; see TestConcurrentClaims below for why that makes the "no
   duplicate ids" assertion deterministic rather than a matter of getting lucky with thread
   scheduling.
2. SCANNER COVERAGE: collect_ids() only "sees" a filename if FILENAME_RE recognizes its
   prefix. A prefix it doesn't recognize isn't just skipped harmlessly -- it stops blocking
   allocation of that number, so claim_next_id() can hand out an id an existing (possibly
   archived) item already holds. See TestFilenamePrefixScanning.

This file is byte-for-byte identical across every repo that carries todo-next-id.py: every
project-specific value (id letter, tracker slug) is read from the loaded module itself
(next_id_module fixture, from conftest.py), never hardcoded here.
"""

from __future__ import annotations

import concurrent.futures
import subprocess
import sys

import pytest

# Every filename prefix todo-next-id.py's FILENAME_RE currently recognizes as "consumes an
# id" -- kept as an independent, hand-maintained list (deliberately NOT derived from
# FILENAME_RE itself, which would be circular and could never catch a scanner regression).
# If a future prefix is added to FILENAME_RE, this list must be updated by hand for the new
# prefix to get its own explicit coverage below.
KNOWN_PREFIXES = ["TODO", "READY", "MAYBE", "HOLD", "FINISHED", "CANCELLED", "NEED_INPUT"]


def _plan_filename(next_id_module, prefix: str, num: int, *, name: str = "sample") -> str:
    letter = next_id_module.ID_PROJECT_LETTER
    return f"{prefix}-{letter}{num:04d}-{name}.md"


class TestFilenamePrefixScanning:
    """Every lifecycle prefix must be scanned, in both the live tracker and the archive."""

    @pytest.mark.parametrize("prefix", KNOWN_PREFIXES)
    def test_prefix_recognized_in_live_tracker(self, next_id_module, tmp_path, prefix):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        (todo_dir / _plan_filename(next_id_module, prefix, 7)).write_text("placeholder\n")

        _, numeric_ids = next_id_module.collect_ids(todo_dir, tmp_path / "finished")

        assert 7 in numeric_ids, (
            f"{prefix}-prefixed filename was not counted by collect_ids() -- a scanner gap "
            f"here is exactly the 'silently skipped, no error' failure class this suite "
            f"exists to catch."
        )

    @pytest.mark.parametrize("prefix", KNOWN_PREFIXES)
    def test_prefix_recognized_in_finished_archive(self, next_id_module, tmp_path, prefix):
        todo_dir = tmp_path / "todos"
        finished_dir = tmp_path / "finished"
        todo_dir.mkdir()
        finished_dir.mkdir()
        (finished_dir / _plan_filename(next_id_module, prefix, 11)).write_text("placeholder\n")

        _, numeric_ids = next_id_module.collect_ids(todo_dir, finished_dir)

        assert 11 in numeric_ids, f"{prefix}-prefixed archive filename was not counted."

    def test_unrecognized_prefix_causes_real_id_collision(self, next_id_module, tmp_path):
        """
        Concrete reproduction of the "id REUSE" hazard: an id already in use under a prefix
        FILENAME_RE doesn't recognize is invisible to the scan, so claim_next_id() computes a
        candidate that collides with it -- two different files, same numeric id, both "real".
        This is the same shape of bug as NEED_INPUT- being missing from
        todo-status-audit.py's pattern, just with a worse consequence (silent id reuse
        instead of a silently-skipped audit).
        """
        todo_dir = tmp_path / "todos"
        finished_dir = tmp_path / "finished"
        todo_dir.mkdir()
        finished_dir.mkdir()
        letter = next_id_module.ID_PROJECT_LETTER
        for n in (1, 2, 3, 4):
            (finished_dir / _plan_filename(next_id_module, "FINISHED", n)).write_text("x\n")
        # An id that's genuinely in use, but under a prefix FILENAME_RE does not recognize.
        collided_name = f"WONTFIX-{letter}0005-already-exists.md"
        (finished_dir / collided_name).write_text("placeholder\n")

        _, numeric_ids = next_id_module.collect_ids(todo_dir, finished_dir)
        assert numeric_ids == {1, 2, 3, 4}, "id 5 should be invisible to the scanner -- WONTFIX- is not a recognized prefix"

        claimed = next_id_module.claim_next_id(todo_dir, numeric_ids)

        assert claimed == 5, (
            "claim_next_id() computed a candidate that collides with an id already in use "
            "under an unrecognized prefix -- this IS the severe id-reuse failure."
        )
        assert (finished_dir / collided_name).exists()  # the pre-existing "5" is still there, now duplicated


class TestClaimNextId:
    """Basic claim behavior, collision handling, and the bounded-retry ceiling."""

    def test_claims_first_free_id_on_empty_tracker(self, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()

        claimed = next_id_module.claim_next_id(todo_dir, set())

        assert claimed == 1
        letter = next_id_module.ID_PROJECT_LETTER
        claim_file = todo_dir / f"TODO-{letter}0001-CLAIMED.md"
        assert claim_file.exists()
        body = claim_file.read_text(encoding="utf-8")
        assert "CLAIMED (placeholder, NOT a real plan)" in body
        assert f"{letter}0001" in body

    def test_claim_file_is_itself_scanned_as_taken(self, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        next_id_module.claim_next_id(todo_dir, set())

        _, numeric_ids = next_id_module.collect_ids(todo_dir, tmp_path / "finished")

        assert 1 in numeric_ids  # retired permanently, even though never renamed to a real plan

    def test_collision_retries_to_next_candidate(self, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        letter = next_id_module.ID_PROJECT_LETTER
        # Simulate id 1 already claimed by someone else, moments earlier.
        (todo_dir / f"TODO-{letter}0001-CLAIMED.md").write_text("already claimed\n")

        claimed = next_id_module.claim_next_id(todo_dir, set())

        assert claimed == 2

    def test_bounded_retries_raise_instead_of_spinning_forever(self, next_id_module, tmp_path, monkeypatch):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        monkeypatch.setattr(next_id_module, "MAX_CLAIM_ATTEMPTS", 3)
        letter = next_id_module.ID_PROJECT_LETTER
        for n in (1, 2, 3):
            (todo_dir / f"TODO-{letter}{n:04d}-CLAIMED.md").write_text("x\n")

        with pytest.raises(RuntimeError, match="could not claim an id"):
            next_id_module.claim_next_id(todo_dir, set())


class TestConcurrentClaims:
    """
    Reproduces the exact failure mode that shipped: ids were each handed out more than once
    in one session because the old code read `1 + max(existing ids)` and returned it -- a
    read, not a reservation. claim_next_id() fixes this via os.open(O_CREAT | O_EXCL), which
    the OS guarantees only one caller can ever win, even against another thread racing on the
    exact same syscall at the exact same instant.

    DETERMINISM: this is not timing-based -- there is no sleep, no retry-count assumption, no
    "usually passes". The assertion (every returned id is unique, and the claimed set is
    exactly {start, ..., start+N-1}) follows from the atomicity guarantee of O_CREAT|O_EXCL on
    a local filesystem, so it holds on every run, on every machine, under any thread-
    scheduling order -- it is not flaky. The only way it COULD flake is if the underlying
    filesystem doesn't honor O_CREAT|O_EXCL atomically (true of some network filesystems,
    never of a local disk/tmpfs, which is what pytest's tmp_path fixture uses). We are not
    aware of a way to make this test meaningfully MORE deterministic than "rely on the OS's
    own atomicity guarantee for the exact syscall the fix depends on" -- if that guarantee
    doesn't hold, the fix itself doesn't hold either.
    """

    @pytest.mark.parametrize("concurrency", [25, 30, 60])  # the three manual-run sizes from the original incident
    def test_concurrent_claims_are_all_unique(self, next_id_module, tmp_path, concurrency):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        numeric_ids: set[int] = set()  # every caller races from the same empty snapshot

        with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as pool:
            futures = [pool.submit(next_id_module.claim_next_id, todo_dir, numeric_ids) for _ in range(concurrency)]
            results = [f.result() for f in futures]

        assert len(results) == concurrency
        assert len(set(results)) == concurrency, (
            f"{concurrency}-way concurrent allocation produced duplicate ids: {sorted(results)} -- "
            "this is the exact silent-duplicate-id bug this test exists to catch."
        )
        assert set(results) == set(range(1, concurrency + 1))

    def test_concurrent_claims_respect_preexisting_ids(self, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        numeric_ids = {5, 7, 12}  # as if a prior scan already found ids up to 12

        with concurrent.futures.ThreadPoolExecutor(max_workers=30) as pool:
            futures = [pool.submit(next_id_module.claim_next_id, todo_dir, numeric_ids) for _ in range(30)]
            results = [f.result() for f in futures]

        assert set(results) == set(range(13, 43))


class TestFinalize:
    """
    Covers do_finalize() / --finalize: the atomic stub -> real-plan rename that closes the
    finalization half of the atomicity hole (see the module docstring's "--finalize supersedes
    the older 'rename it yourself' convention" paragraph). claim_next_id()'s O_CREAT|O_EXCL
    already made the CLAIM half of a reservation race-proof (TestConcurrentClaims above); this
    class covers the other half -- the caller no longer performs the rename themselves, so no
    calling convention can leave both names on disk.
    """

    def test_finalize_renames_stub_to_final_path(self, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        letter = next_id_module.ID_PROJECT_LETTER
        stub = todo_dir / f"TODO-{letter}0001-CLAIMED.md"
        stub.write_text("# Real plan content, written in place over the stub\n", encoding="utf-8")

        exit_code = next_id_module.do_finalize(todo_dir, "1", "short-slug")

        assert exit_code == 0
        assert not stub.exists()
        final_path = todo_dir / f"TODO-{letter}0001-short-slug.md"
        assert final_path.is_file()

    def test_finalize_preserves_the_content_written_into_the_stub(self, next_id_module, tmp_path):
        """The rename must carry over whatever the caller already wrote into the stub -- finalize
        never touches content, only the filename."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        letter = next_id_module.ID_PROJECT_LETTER
        stub = todo_dir / f"TODO-{letter}0001-CLAIMED.md"
        stub.write_text("# The Real Plan\n\nCurrent status: Ready to execute.\n", encoding="utf-8")

        next_id_module.do_finalize(todo_dir, "1", "short-slug")

        final_path = todo_dir / f"TODO-{letter}0001-short-slug.md"
        assert final_path.read_text(encoding="utf-8") == "# The Real Plan\n\nCurrent status: Ready to execute.\n"

    def test_finalize_accepts_a_letter_prefixed_id(self, next_id_module, tmp_path):
        """normalize_check_id()'s accepted forms (letter-prefixed, 'TODO-'-prefixed, ...) must
        also work for --finalize, not just the bare digit form."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        letter = next_id_module.ID_PROJECT_LETTER
        stub = todo_dir / f"TODO-{letter}0007-CLAIMED.md"
        stub.write_text("placeholder\n", encoding="utf-8")

        exit_code = next_id_module.do_finalize(todo_dir, f"{letter}7", "short-slug")

        assert exit_code == 0
        assert (todo_dir / f"TODO-{letter}0007-short-slug.md").is_file()

    def test_finalize_fails_loudly_on_missing_stub(self, next_id_module, tmp_path, capsys):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()

        exit_code = next_id_module.do_finalize(todo_dir, "1", "short-slug")

        assert exit_code == 4
        assert not (todo_dir / f"TODO-{next_id_module.ID_PROJECT_LETTER}0001-short-slug.md").exists()
        assert "no claim stub" in capsys.readouterr().err

    def test_finalize_rejects_a_malformed_id_without_touching_the_filesystem(self, next_id_module, tmp_path, capsys):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()

        exit_code = next_id_module.do_finalize(todo_dir, "not-an-id!!", "short-slug")

        assert exit_code == 4
        assert "is not a valid id" in capsys.readouterr().err
        assert list(todo_dir.iterdir()) == []

    def test_finalize_rejects_a_lettered_child_id(self, next_id_module, tmp_path, capsys):
        """Claim stubs are only ever issued for a bare numeric id -- todo-next-id.py never claims
        on behalf of a lettered child -- so a lettered --finalize argument can never have a stub
        and must be refused before even touching the filesystem, not treated as 'missing stub'."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()

        exit_code = next_id_module.do_finalize(todo_dir, "7a", "short-slug")

        assert exit_code == 4
        detail = capsys.readouterr().err
        assert "lettered child" in detail
        assert list(todo_dir.iterdir()) == []

    def test_finalize_rejects_a_malformed_slug(self, next_id_module, tmp_path, capsys):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        letter = next_id_module.ID_PROJECT_LETTER
        stub = todo_dir / f"TODO-{letter}0001-CLAIMED.md"
        stub.write_text("placeholder\n", encoding="utf-8")

        exit_code = next_id_module.do_finalize(todo_dir, "1", "Not_A_Valid_Slug")

        assert exit_code == 4
        assert "not a valid slug" in capsys.readouterr().err
        assert stub.exists()  # refused before touching anything

    def test_finalize_refuses_to_overwrite_an_existing_target(self, next_id_module, tmp_path, capsys):
        """The exact shape of tonight's bug, caught at finalize time instead of only by the
        audit: a real file already sits at the target name (e.g. because a caller already wrote
        it directly instead of finalizing). finalize must not clobber it."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        letter = next_id_module.ID_PROJECT_LETTER
        stub = todo_dir / f"TODO-{letter}0001-CLAIMED.md"
        stub.write_text("placeholder\n", encoding="utf-8")
        final_path = todo_dir / f"TODO-{letter}0001-short-slug.md"
        final_path.write_text("# Already exists\n", encoding="utf-8")

        exit_code = next_id_module.do_finalize(todo_dir, "1", "short-slug")

        assert exit_code == 4
        assert "already exists" in capsys.readouterr().err
        assert stub.exists()  # neither file touched -- both left exactly as they were
        assert final_path.read_text(encoding="utf-8") == "# Already exists\n"

    def test_finalize_does_not_disturb_an_unrelated_ids_stub(self, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        letter = next_id_module.ID_PROJECT_LETTER
        stub_1 = todo_dir / f"TODO-{letter}0001-CLAIMED.md"
        stub_1.write_text("placeholder for 1\n", encoding="utf-8")
        stub_2 = todo_dir / f"TODO-{letter}0002-CLAIMED.md"
        stub_2.write_text("real plan for 2\n", encoding="utf-8")

        exit_code = next_id_module.do_finalize(todo_dir, "2", "short-slug")

        assert exit_code == 0
        assert stub_1.exists()  # id 1's live reservation must be untouched
        assert not stub_2.exists()


class TestCliEndToEnd:
    """A couple of real subprocess invocations, to catch bugs hiding in argument parsing /
    main() wiring that direct function calls above can't see."""

    def test_main_prints_next_id_and_creates_claim_file(self, scripts_dir, next_id_module, tmp_path):
        project_work = tmp_path / "Project Work"
        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-next-id.py"), "--root", str(project_work), "--allow-missing"],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 0, result.stderr
        letter = next_id_module.ID_PROJECT_LETTER
        assert result.stdout.strip() == f"{letter}0001"
        claim_file = project_work / "todos" / next_id_module.TRACKER_SLUG / f"TODO-{letter}0001-CLAIMED.md"
        assert claim_file.exists()

    def test_missing_tracker_is_a_hard_error_without_allow_missing(self, scripts_dir, tmp_path):
        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-next-id.py"), "--root", str(tmp_path / "Project Work")],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 2

    def test_finalize_cli_end_to_end(self, scripts_dir, next_id_module, tmp_path):
        """The full lifecycle through the CLI, not just do_finalize() directly: claim, overwrite
        the stub in place, finalize, and confirm the real filename is what's left on disk."""
        project_work = tmp_path / "Project Work"
        claim_result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-next-id.py"), "--root", str(project_work), "--allow-missing"],
            capture_output=True,
            text=True,
            timeout=15,
        )
        assert claim_result.returncode == 0, claim_result.stderr
        claimed_id = claim_result.stdout.strip()

        todo_dir = project_work / "todos" / next_id_module.TRACKER_SLUG
        stub = todo_dir / f"TODO-{claimed_id}-CLAIMED.md"
        stub.write_text("# The real plan\n", encoding="utf-8")

        finalize_result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-next-id.py"), "--root", str(project_work), "--finalize", claimed_id, "short-slug"],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert finalize_result.returncode == 0, finalize_result.stderr
        assert not stub.exists()
        final_path = todo_dir / f"TODO-{claimed_id}-short-slug.md"
        assert final_path.is_file()
        assert final_path.read_text(encoding="utf-8") == "# The real plan\n"

    def test_finalize_cli_exits_4_on_missing_stub(self, scripts_dir, tmp_path):
        project_work = tmp_path / "Project Work"
        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-next-id.py"), "--root", str(project_work), "--allow-missing", "--finalize", "1", "short-slug"],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 4, result.stdout
