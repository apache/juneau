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
Tests for scripts/todo-status-audit.py -- the status/header consistency pre-filter.

The bug this suite guards against just shipped: NEED_INPUT-*.md was documented (module
docstring) as scanned, but omitted from FILENAME_RE in all three copies of this script, so
those files were silently skipped -- not flagged, not scanned, no output at all.
TestDocumentedPrefixesAreScanned reproduces exactly that shape of bug: it parses the
"promised" prefix list straight out of the module's own docstring and cross-checks it against
what FILENAME_RE actually matches, so a future prefix added to the prose without being added
to the pattern fails immediately, instead of staying silent until someone notices missing
output.

This file is byte-for-byte identical across every repo that carries todo-status-audit.py:
the project-specific id letter is read from the companion todo-next-id.py module
(next_id_module fixture), never hardcoded here.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys
import time
from pathlib import Path

import pytest

_DOC_PREFIX_LIST_RE = re.compile(r"Checks every ([A-Za-z_/-]+?)\*\.md")

# Reason codes are documented as the first token of a bullet in the "Checks performed" list,
# either "  - code   Description" or "  - code" with the description on the following line.
_DOC_REASON_CODE_RE = re.compile(r"^ {2}- ([a-z][a-z0-9_]+)", re.MULTILINE)

_LIFECYCLE_MISMATCH_CODES = {
    "todo_prefix_marked_ready",
    "on_hold_status_wrong_prefix",
    "parked_status_wrong_prefix",
    "ready_prefix_waiting_status",
    "maybe_prefix_non_parked",
    "hold_prefix_non_on_hold",
}


def _documented_prefixes(audit_module) -> list[str]:
    """Parse the prefix list out of the module's own docstring (e.g. "Checks every
    TODO-/READY-/MAYBE-/HOLD-/NEED_INPUT-*.md file..."), independently of FILENAME_RE, so the
    comparison in test_documented_prefixes_are_all_recognized_by_filename_re below is a real
    prose-vs-code cross-check, not a self-fulfilling one."""
    match = _DOC_PREFIX_LIST_RE.search(audit_module.__doc__)
    assert match, (
        "module docstring no longer documents the scanned prefix list in the expected "
        "'Checks every A-/B-/*.md' format -- update this regex to match the new wording."
    )
    return [p.rstrip("-") for p in match.group(1).split("/") if p]


def _documented_reason_codes(audit_module) -> list[str]:
    """Parse the reason codes out of the module docstring's "Checks performed" bullet list,
    independently of the source, so the comparison in TestDocumentedChecksAreImplemented is a real
    prose-vs-code cross-check rather than a self-fulfilling one."""
    codes = _DOC_REASON_CODE_RE.findall(audit_module.__doc__)
    assert codes, (
        "module docstring no longer lists reason codes as '  - <code>' bullets under 'Checks "
        "performed' -- update this regex to match the new wording."
    )
    return codes


def _plan_filename(next_id_module, prefix: str, num: int, *, name: str = "sample") -> str:
    letter = next_id_module.ID_PROJECT_LETTER
    return f"{prefix}-{letter}{num:04d}-{name}.md"


def _write_plan(path, *, status: str = "Ready to execute.", complexity: str = "Small", body: str = "", source: str = "") -> None:
    source_line = f"{source}\n\n" if source else ""
    path.write_text(
        f"# Sample plan\n\n{source_line}Current status: {status}\nComplexity: {complexity}\n\n{body}",
        encoding="utf-8",
    )


def _claim_filename(next_id_module, num: int) -> str:
    """"TODO-<letter><num>-CLAIMED.md" -- the exact name claim_next_id() (todo-next-id.py) writes."""
    letter = next_id_module.ID_PROJECT_LETTER
    return f"TODO-{letter}{num:04d}-CLAIMED.md"


def _write_claim_stub(path, *, claimed_by: str = "pid 99999 on test-host.example.com", claimed_at: str = "2026-01-01T00:00:00Z") -> None:
    """Write a stub in the same shape claim_placeholder_text() (todo-next-id.py) produces: no
    'Current status:'/'Complexity:' line, a 'Claimed at:' timestamp, and a 'Claimed by: pid <n> on
    <host>' line. Mirrors the real file's structure rather than reproducing its exact prose."""
    formatted_id = path.name[len("TODO-"):-len("-CLAIMED.md")]
    path.write_text(
        f"# {formatted_id} -- CLAIMED (placeholder, NOT a real plan)\n"
        "\n"
        "This id was atomically reserved by todo-next-id.py so no other session\n"
        "could be handed the same number. No plan has been written yet.\n"
        "\n"
        f"Claimed at:  {claimed_at}\n"
        f"Claimed by:  {claimed_by}\n"
        "Command:     scripts/todo-next-id.py\n",
        encoding="utf-8",
    )


def _age_stub(path, hours: float) -> None:
    """Backdate path's mtime by `hours` so audit_claim_stub()'s age computation sees that age."""
    when = time.time() - hours * 3600
    os.utime(path, (when, when))


def _write_board(root, text: str, *, live_waves=()) -> "object":
    """Write a WAVE.md board at root and (optionally) live waves/WAVE-nnnn-*.md files, returning
    the board path. Mirrors the real board layout: WAVE.md with waves/ as a sibling."""
    board = root / "WAVE.md"
    board.write_text(text, encoding="utf-8")
    if live_waves:
        waves_dir = root / "waves"
        waves_dir.mkdir(exist_ok=True)
        for wave in live_waves:
            (waves_dir / f"{wave}-in-flight.md").write_text("# in flight\n", encoding="utf-8")
    return board


class TestDocumentedPrefixesAreScanned:
    def test_documented_prefixes_are_all_recognized_by_filename_re(self, audit_module, next_id_module):
        letter = next_id_module.ID_PROJECT_LETTER
        prefixes = _documented_prefixes(audit_module)

        assert prefixes, "expected at least one documented prefix"
        for prefix in prefixes:
            sample_name = f"{prefix}-{letter}0001-sample.md"
            assert audit_module.FILENAME_RE.match(sample_name), (
                f"'{prefix}' is documented as scanned in the module docstring, but FILENAME_RE "
                f"does not match '{sample_name}' -- this is exactly the NEED_INPUT- bug: a "
                f"prefix promised in prose, silently skipped by the actual scan pattern."
            )

    @pytest.mark.parametrize("prefix", ["TODO", "READY", "MAYBE", "HOLD", "NEED_INPUT"])
    def test_each_prefix_is_found_and_audited_not_skipped(self, audit_module, next_id_module, tmp_path, prefix):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        path = todo_dir / _plan_filename(next_id_module, prefix, 3)
        # Deliberately malformed (no status/complexity line at all) so it is guaranteed to be
        # flagged -- the point here is "was it looked at", not "what did it get flagged for".
        path.write_text("# Sample plan\n\nNo status or complexity line here.\n", encoding="utf-8")

        found = audit_module.find_plan_files(todo_dir)

        assert path in found, f"{prefix}-prefixed file was not returned by find_plan_files() -- silently skipped."
        flags = audit_module.audit_file(path)
        assert any(code == "missing_status_header" for code, _ in flags)

    def test_finished_and_cancelled_are_explicitly_out_of_scope(self, audit_module, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        (todo_dir / _plan_filename(next_id_module, "FINISHED", 9, name="old")).write_text("no status line\n")
        (todo_dir / _plan_filename(next_id_module, "CANCELLED", 10, name="old")).write_text("no status line\n")

        assert audit_module.find_plan_files(todo_dir) == []

    def test_unrecognized_prefix_is_silently_skipped_by_design(self, audit_module, next_id_module, tmp_path):
        """Sanity check on find_plan_files() itself: an unrecognized prefix truly is invisible
        to it, which is exactly why the documented-prefix cross-check above matters."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        letter = next_id_module.ID_PROJECT_LETTER
        (todo_dir / f"SOMETHING_ELSE-{letter}0001-x.md").write_text("Current status: Done\nComplexity: Small\n")

        assert audit_module.find_plan_files(todo_dir) == []


class TestEmptyStatusValue:
    """Both directions matter: a check that never fires is as useless as one that always
    does. empty_status_value must fire on structurally-blank values and stay silent on the
    free-form descriptive prose the trackers actually use."""

    @pytest.mark.parametrize("status", ["", "   ", "-", "---", "***", "``", "**  **", "____", "-- -- --"])
    def test_fires_on_genuinely_blank_or_decoration_only(self, audit_module, status):
        path_text = f"# Sample\n\nCurrent status: {status}\nComplexity: Small\n"
        assert audit_module.status_value_is_empty(status)  # unit-level: the predicate itself
        # And end-to-end, via audit_file(), on an actual file:
        import tempfile
        from pathlib import Path

        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "TODO-X0001-sample.md"
            path.write_text(path_text, encoding="utf-8")
            flags = audit_module.audit_file(path)
        assert any(code == "empty_status_value" for code, _ in flags), f"status={status!r} should have flagged empty_status_value"

    @pytest.mark.parametrize(
        "status",
        [
            "Done",
            "Done.",
            "**COMPLETE (2026-07-16).**",
            "Blocked on the pin bump.",
            "Blocked on `TODO-J0042` landing first.",
            "Ready to execute -- no open questions remain.",
            "On hold pending operator decision; see notes.",
            "**Parked** -- revisit after the next release.",
            "This is a long, descriptive, bolded narrative paragraph about **why** this item "
            "is where it is, with -- dashes -- and other punctuation sprinkled throughout it "
            "for good measure.",
        ],
    )
    def test_does_not_fire_on_descriptive_prose(self, audit_module, next_id_module, tmp_path, status):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        _write_plan(path, status=status)

        flags = audit_module.audit_file(path)

        assert not any(code == "empty_status_value" for code, _ in flags), f"status={status!r} should NOT have flagged empty_status_value"


class TestMarkdownDecorationDoesNotDefeatChecks:
    def test_ready_to_execute_detected_under_bold_decoration(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        _write_plan(path, status="**Ready to execute** -- nothing left open.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "todo_prefix_marked_ready" in flags

    def test_on_hold_detected_under_backtick_decoration(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        _write_plan(path, status="`On hold` pending a decision.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "on_hold_status_wrong_prefix" in flags

    def test_parked_detected_under_leading_dash_decoration(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        _write_plan(path, status="- Parked until further notice.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "parked_status_wrong_prefix" in flags

    def test_hold_prefix_ok_status_detected_under_blockquote_decoration(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "HOLD", 1)
        _write_plan(path, status="> On hold, see thread for context.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "hold_prefix_non_on_hold" not in flags


class TestLifecycleMismatchChecks:
    def test_todo_prefixed_file_marked_ready_is_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        _write_plan(path, status="Ready to execute.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "todo_prefix_marked_ready" in flags

    def test_todo_prefixed_file_says_on_hold_is_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        _write_plan(path, status="On hold pending review.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "on_hold_status_wrong_prefix" in flags

    def test_todo_prefixed_file_says_parked_is_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        _write_plan(path, status="Parked -- revisit later.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "parked_status_wrong_prefix" in flags

    def test_ready_prefixed_file_still_waiting_is_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "READY", 1)
        _write_plan(path, status="Waiting for user input.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "ready_prefix_waiting_status" in flags

    def test_maybe_prefixed_file_not_parked_is_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "MAYBE", 1)
        _write_plan(path, status="Ready to execute.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "maybe_prefix_non_parked" in flags

    def test_hold_prefixed_file_not_on_hold_is_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "HOLD", 1)
        _write_plan(path, status="Ready to execute.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "hold_prefix_non_on_hold" in flags

    def test_correctly_matched_prefixes_are_not_flagged(self, audit_module, next_id_module, tmp_path):
        cases = [
            ("TODO", "Not yet started."),
            ("READY", "Ready to execute -- no open questions."),
            ("MAYBE", "Parked -- low priority."),
            ("HOLD", "On hold pending a decision."),
        ]
        for prefix, status in cases:
            path = tmp_path / _plan_filename(next_id_module, prefix, 1, name=prefix.lower())
            _write_plan(path, status=status)

            flags = {code for code, _ in audit_module.audit_file(path)}

            assert not (flags & _LIFECYCLE_MISMATCH_CODES), f"{prefix} with status {status!r} should not trigger a lifecycle-mismatch flag, got {flags}"

    def test_ready_but_open_questions_unresolved_is_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "READY", 1)
        body = "## Open questions\n\n1. Should this use approach A or B?\n"
        _write_plan(path, status="Ready to execute.", body=body)

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "ready_but_has_open_questions" in flags

    def test_ready_with_resolved_open_questions_is_not_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "READY", 1)
        body = "## Open questions\n\n1. Should this use approach A or B? Resolved: approach A.\n"
        _write_plan(path, status="Ready to execute.", body=body)

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "ready_but_has_open_questions" not in flags

    def test_need_input_prefix_is_exempt_from_lifecycle_mismatch_checks(self, audit_module, next_id_module, tmp_path):
        """NEED_INPUT files are scanned (structural checks apply) but deliberately not
        subject to the TODO/READY-specific Parked-/On-hold-prefix-mismatch checks."""
        path = tmp_path / _plan_filename(next_id_module, "NEED_INPUT", 1)
        _write_plan(path, status="On hold, parked, ready to execute -- all at once.")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert not (flags & _LIFECYCLE_MISMATCH_CODES)


class TestClosedOutWaves:
    """The board side of the wave cross-check. A wave only counts as closed out when BOTH
    records agree: the board's prose log says accepted AND the WAVE file is gone."""

    def test_missing_board_yields_empty_set(self, audit_module, tmp_path):
        assert audit_module.closed_out_waves(tmp_path / "nope" / "WAVE.md") == frozenset()

    def test_none_board_yields_empty_set(self, audit_module):
        assert audit_module.closed_out_waves(None) == frozenset()

    def test_accepted_wave_with_no_live_file_is_closed_out(self, audit_module, tmp_path):
        board = _write_board(tmp_path, "WAVE-0003 was accepted and pushed 2026-08-25.\n")

        assert audit_module.closed_out_waves(board) == frozenset({"WAVE-0003"})

    def test_multiple_accepted_waves_on_one_line_are_all_collected(self, audit_module, tmp_path):
        board = _write_board(tmp_path, "WAVE-0003 was accepted and pushed; WAVE-0002 was accepted too.\n")

        assert audit_module.closed_out_waves(board) == frozenset({"WAVE-0002", "WAVE-0003"})

    def test_accepted_wave_with_a_live_wave_file_is_not_closed_out(self, audit_module, tmp_path):
        """An in-flight wave still has its file on disk. The filesystem is the non-prose half of
        the check, and it must veto a loose 'accepted' word match on the board."""
        board = _write_board(tmp_path, "WAVE-0004 accepted in principle, still running.\n", live_waves=["WAVE-0004"])

        assert audit_module.closed_out_waves(board) == frozenset()

    @pytest.mark.parametrize(
        "line",
        [
            "WAVE-0004 is not accepted yet.",
            "WAVE-0004 was never accepted.",
            "WAVE-0004 isn't accepted.",
            "WAVE-0004 wasn't accepted.",
        ],
    )
    def test_negated_acceptance_does_not_read_as_accepted(self, audit_module, tmp_path, line):
        """Reading 'not accepted' as accepted would flag a CORRECTLY-queued item -- precisely the
        cry-wolf failure this script was rewritten to eliminate, so it is guarded explicitly."""
        board = _write_board(tmp_path, line + "\n")

        assert audit_module.closed_out_waves(board) == frozenset()

    def test_wave_mentioned_without_acceptance_is_not_closed_out(self, audit_module, tmp_path):
        board = _write_board(tmp_path, "| WAVE-0005 | queued | 3 workers |\n")

        assert audit_module.closed_out_waves(board) == frozenset()


class TestHeaderRegion:
    def test_header_region_stops_at_first_section_heading(self, audit_module):
        text = "# Title\n\nCurrent status: Ready to execute.\n\n## Goal\n\nBody mentions WAVE-0003.\n"

        header = audit_module.header_region(text)

        assert "Current status" in header
        assert "WAVE-0003" not in header

    def test_header_region_is_whole_file_when_there_are_no_headings(self, audit_module):
        text = "# Title\n\nCurrent status: Ready to execute.\n"

        assert audit_module.header_region(text) == text


class TestReadyButWaveAlreadyAccepted:
    """The drift this check exists for: nine items shipped to master in one commit, stayed in the
    active tracker for a day still saying 'Ready to execute ... do not farm until the operator
    starts the WAVE queue', and the stale claim then propagated into downstream sequencing."""

    def test_ready_item_queued_behind_an_accepted_wave_is_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "READY", 1)
        _write_plan(
            path,
            status="Ready to execute. Do not farm until the operator starts the WAVE queue.",
            source="Expanded to a reviewable spec (WAVE-0003 spec-review pass).",
            body="## Goal\n\nShip the thing.\n",
        )

        flags = {code for code, _ in audit_module.audit_file(path, frozenset({"WAVE-0003"}))}

        assert "ready_but_wave_already_accepted" in flags

    def test_detail_names_the_offending_wave(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "READY", 1)
        _write_plan(path, source="Filed under WAVE-0003.", body="## Goal\n\nx\n")

        details = [d for code, d in audit_module.audit_file(path, frozenset({"WAVE-0003"})) if code == "ready_but_wave_already_accepted"]

        assert details and "WAVE-0003" in details[0]

    def test_no_board_means_the_check_is_inert(self, audit_module, next_id_module, tmp_path):
        """Default closed_waves is empty, so every pre-existing single-argument caller keeps its
        old behavior instead of getting a check silently evaluated against an unknown board."""
        path = tmp_path / _plan_filename(next_id_module, "READY", 1)
        _write_plan(path, source="Filed under WAVE-0003.", body="## Goal\n\nx\n")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "ready_but_wave_already_accepted" not in flags

    def test_wave_still_in_flight_is_not_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "READY", 1)
        _write_plan(path, source="Filed under WAVE-0004.", body="## Goal\n\nx\n")

        flags = {code for code, _ in audit_module.audit_file(path, frozenset({"WAVE-0003"}))}

        assert "ready_but_wave_already_accepted" not in flags

    def test_body_only_cross_reference_to_an_accepted_wave_is_not_flagged(self, audit_module, next_id_module, tmp_path):
        """The corpus false-positive case. Four unstarted items reference the accepted wave only to
        say they must be serialized against its members. That is a statement about OTHER items, it
        lives in a body section, and it must not be read as this item's own membership."""
        path = tmp_path / _plan_filename(next_id_module, "TODO", 2)
        _write_plan(
            path,
            status="Ready to execute.",
            body=(
                "## Goal\n\nDo an unrelated thing.\n\n"
                "## Sequencing\n\n"
                "`READY-J0445u` / `READY-J0445v` all edit the same hot file, and the board records "
                "WAVE-0003 as serial on it. This item must be serialized against those.\n"
            ),
        )

        flags = {code for code, _ in audit_module.audit_file(path, frozenset({"WAVE-0003"}))}

        assert "ready_but_wave_already_accepted" not in flags

    def test_non_ready_status_is_not_flagged_even_with_a_header_wave(self, audit_module, next_id_module, tmp_path):
        """The umbrella case: a parent item legitimately names the wave in its header while being
        'In progress' rather than queued. It never claimed not to have run, so there is no
        contradiction to flag."""
        path = tmp_path / _plan_filename(next_id_module, "TODO", 3)
        _write_plan(
            path,
            status="In progress. Do not farm WAVE-0003 until operator kick-off.",
            body="## Goal\n\nUmbrella.\n",
        )

        flags = {code for code, _ in audit_module.audit_file(path, frozenset({"WAVE-0003"}))}

        assert "ready_but_wave_already_accepted" not in flags

    def test_ready_item_with_no_wave_reference_at_all_is_not_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "READY", 4)
        _write_plan(path, body="## Goal\n\nx\n")

        flags = {code for code, _ in audit_module.audit_file(path, frozenset({"WAVE-0003"}))}

        assert "ready_but_wave_already_accepted" not in flags

    @pytest.mark.parametrize("prefix", ["MAYBE", "HOLD", "NEED_INPUT"])
    def test_non_todo_ready_prefixes_are_exempt(self, audit_module, next_id_module, tmp_path, prefix):
        """Same scoping as ready_but_has_open_questions: this is a TODO/READY lifecycle signal, and
        the module docstring's prefix table exempts the others from those."""
        path = tmp_path / _plan_filename(next_id_module, prefix, 5)
        _write_plan(path, source="Filed under WAVE-0003.", body="## Goal\n\nx\n")

        flags = {code for code, _ in audit_module.audit_file(path, frozenset({"WAVE-0003"}))}

        assert "ready_but_wave_already_accepted" not in flags

    def test_naming_symbols_that_already_exist_is_not_a_signal(self, audit_module, next_id_module, tmp_path):
        """The inverse trap, pinned deliberately. An item that MODIFIES existing code names
        symbols that already resolve in the tree; symbol-existence probing would flag it. This
        check reads only tracker records, never the source tree, so a prose-heavy item full of
        real class names cannot be flagged by it."""
        path = tmp_path / _plan_filename(next_id_module, "READY", 6)
        _write_plan(
            path,
            body=(
                "## Scope\n\n"
                "Widen `NestedTableDef.validate()`, extend `ViewTable.emitNestedTable`, and keep "
                "`RowDetailDef.CONTRACT_VERSION` at `\"1\"`. Tests extend `ViewsJs_NestedTable_Test`.\n"
            ),
        )

        flags = {code for code, _ in audit_module.audit_file(path, frozenset({"WAVE-0003"}))}

        assert flags == set(), f"an item naming only pre-existing symbols must stay clean, got {flags}"


class TestHeaderStructureChecks:
    def test_missing_status_header_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        path.write_text("# Sample\n\nComplexity: Small\n", encoding="utf-8")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "missing_status_header" in flags

    def test_missing_complexity_header_flagged(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        path.write_text("# Sample\n\nCurrent status: Done\n", encoding="utf-8")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "missing_complexity_header" in flags

    def test_status_after_first_heading_is_misplaced(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        path.write_text(
            "# Sample\n\nComplexity: Small\n\n## Goal\n\nCurrent status: Done\n",
            encoding="utf-8",
        )

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "status_header_misplaced" in flags

    def test_status_before_first_heading_is_not_misplaced(self, audit_module, next_id_module, tmp_path):
        path = tmp_path / _plan_filename(next_id_module, "TODO", 1)
        _write_plan(path, status="Done")

        flags = {code for code, _ in audit_module.audit_file(path)}

        assert "status_header_misplaced" not in flags


class TestClaimStubHandling:
    """Covers the false-positive fix: an id-reservation stub from todo-next-id.py's
    claim_next_id() must not be audited as an ordinary (malformed) plan file, and must instead be
    checked for staleness on its own terms. See the module docstring's "Claim-stub handling"."""

    def test_claim_stub_filename_matches_claim_stub_re(self, audit_module, next_id_module):
        assert audit_module.CLAIM_STUB_RE.match(_claim_filename(next_id_module, 37))

    def test_claim_stub_is_still_found_by_find_plan_files(self, audit_module, next_id_module, tmp_path):
        """The reservation must keep counting as "taken" for numbering -- it is exempted from the
        plan-file CHECKS, not removed from the scan entirely."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)

        assert stub in audit_module.find_plan_files(todo_dir)

    def test_fresh_claim_stub_would_have_failed_the_old_plan_file_checks(self, audit_module, next_id_module, tmp_path):
        """Sanity check proving the bug shape is real: routed through audit_file() (the pre-fix
        code path), a claim stub is unconditionally flagged on both checks it can never satisfy.
        Guards the exemption test below against passing vacuously."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)

        flags = {code for code, _ in audit_module.audit_file(stub)}

        assert {"missing_status_header", "missing_complexity_header"} <= flags

    def test_fresh_claim_stub_is_exempt_via_audit_claim_stub(self, audit_module, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)  # just written: mtime is "now"

        assert audit_module.audit_claim_stub(stub) == []

    def test_claim_well_under_the_threshold_is_a_live_claim_negative_case(self, audit_module, next_id_module, tmp_path):
        """The case that matters most: a claim mid-allocation -- exactly the shape a real WAVE id
        reservation takes while its plan is being written -- must never trip the stale check."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)
        _age_stub(stub, hours=1)  # a normal claim-to-rename gap, nowhere near STALE_CLAIM_AGE_HOURS

        assert audit_module.audit_claim_stub(stub) == []

    def test_claim_just_under_the_threshold_is_not_flagged(self, audit_module, next_id_module, tmp_path):
        """Boundary case one hour inside the threshold: must not fire."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)
        _age_stub(stub, hours=audit_module.STALE_CLAIM_AGE_HOURS - 1)

        assert audit_module.audit_claim_stub(stub) == []

    def test_claim_past_the_threshold_is_flagged_stale(self, audit_module, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)
        _age_stub(stub, hours=audit_module.STALE_CLAIM_AGE_HOURS + 1)

        flags = {code for code, _ in audit_module.audit_claim_stub(stub)}

        assert flags == {"stale_claim"}

    def test_stale_detail_names_the_age_and_the_recorded_claimant(self, audit_module, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub, claimed_by="pid 11895 on jamesbog-ltmjqf2.internal.salesforce.com")
        _age_stub(stub, hours=48)

        _, detail = audit_module.audit_claim_stub(stub)[0]

        assert "pid 11895 on jamesbog-ltmjqf2.internal.salesforce.com" in detail
        assert "48.0h" in detail

    def test_stub_with_no_claimed_by_line_still_becomes_stale_from_age_alone(self, audit_module, next_id_module, tmp_path):
        """If a stub records no pid (or a caller writes a hand-made one without it), staleness
        must still be decidable from age alone -- the pid is never load-bearing here."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        stub.write_text("# J0037 -- CLAIMED (placeholder, NOT a real plan)\n\nNo metadata lines at all.\n", encoding="utf-8")
        _age_stub(stub, hours=audit_module.STALE_CLAIM_AGE_HOURS + 1)

        flags = audit_module.audit_claim_stub(stub)

        assert flags and flags[0][0] == "stale_claim"
        assert "unrecorded claimant" in flags[0][1]

    def test_end_to_end_live_claim_does_not_dirty_the_audit(self, scripts_dir, next_id_module, tmp_path):
        """The bug's exact symptom, reproduced and proven fixed at the CLI level: a just-claimed
        id (the state every successful reservation is in until the caller writes the plan) must
        no longer flag the scan at all."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)  # fresh mtime: just claimed, exactly like a live reservation

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir)],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 0, result.stdout
        assert "Scanned 1 file(s); 0 flagged." in result.stdout

    def test_end_to_end_stale_claim_is_flagged_stale_not_missing_headers(self, audit_module, scripts_dir, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)
        _age_stub(stub, hours=audit_module.STALE_CLAIM_AGE_HOURS + 1)

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir)],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 1, result.stdout
        assert "stale_claim" in result.stdout
        assert "missing_status_header" not in result.stdout
        assert "missing_complexity_header" not in result.stdout


class TestClaimStubCoexistence:
    """Covers unfinalized_claim_stub: a claim stub and a real plan file coexisting under the
    same numeric id. This is the regression tonight's actual incident produced -- an agent read
    todo-next-id.py's finalize instruction, reconsidered mid-task, and wrote the real plan under
    a fresh slugged filename ("TODO-<id>-dispatch-tool-enforcement.md") instead of finalizing
    the stub ("TODO-<id>-CLAIMED.md") in place, leaving both on disk. Unlike stale_claim, this
    check is deliberately NOT age-gated -- see the module docstring's "Claim-stub handling"."""

    def test_stub_alone_is_not_flagged(self, audit_module, next_id_module, tmp_path):
        """Negative case: a claim stub with no coexisting plan must not trip this check -- that
        is the ordinary, in-progress reservation shape TestClaimStubHandling already covers."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)

        files = audit_module.find_plan_files(todo_dir)
        groups = audit_module.group_claims_by_numeric_id(files)
        flags = {code for code, _ in audit_module.audit_claim_stub(stub, groups[37]["plans"])}

        assert "unfinalized_claim_stub" not in flags

    def test_plan_alone_is_not_flagged(self, audit_module, next_id_module, tmp_path):
        """Negative case: a real plan file with no stub at all has nothing for the coexistence
        logic to fire on -- audit_claim_stub() is never even called for a non-stub filename in
        main()'s dispatch, and its own grouping records no stub for that id."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        plan = todo_dir / _plan_filename(next_id_module, "TODO", 37, name="some-plan")
        _write_plan(plan)

        files = audit_module.find_plan_files(todo_dir)
        groups = audit_module.group_claims_by_numeric_id(files)

        assert groups[37]["stub"] is None
        assert not any(code == "unfinalized_claim_stub" for code, _ in audit_module.audit_file(plan))

    def test_stub_and_plan_for_the_same_id_is_flagged(self, audit_module, next_id_module, tmp_path):
        """Reproduces tonight's actual bug shape directly: a stub and a real plan file coexisting
        under one id. This is the regression that must never come back silently."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)
        plan = todo_dir / _plan_filename(next_id_module, "TODO", 37, name="dispatch-tool-enforcement")
        _write_plan(plan)

        files = audit_module.find_plan_files(todo_dir)
        groups = audit_module.group_claims_by_numeric_id(files)
        flags = {code for code, _ in audit_module.audit_claim_stub(stub, groups[37]["plans"])}

        assert "unfinalized_claim_stub" in flags

    def test_flag_fires_even_on_a_brand_new_stub_not_age_gated(self, audit_module, next_id_module, tmp_path):
        """The distinguishing property from stale_claim: a stub claimed a second ago, sitting
        beside a real plan for the same id, must still be flagged immediately -- and stale_claim
        itself must NOT also fire, since the stub is nowhere near STALE_CLAIM_AGE_HOURS old."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)  # fresh mtime -- would be silent under stale_claim alone
        plan = todo_dir / _plan_filename(next_id_module, "TODO", 37, name="dispatch-tool-enforcement")
        _write_plan(plan)

        flags = audit_module.audit_claim_stub(stub, [plan])

        assert len(flags) == 1
        assert flags[0][0] == "unfinalized_claim_stub"

    def test_detail_names_the_coexisting_plan_file_and_finalize(self, audit_module, next_id_module, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)
        plan_name = _plan_filename(next_id_module, "TODO", 37, name="dispatch-tool-enforcement")
        plan = todo_dir / plan_name
        _write_plan(plan)

        _, detail = audit_module.audit_claim_stub(stub, [plan])[0]

        assert plan_name in detail
        assert "--finalize" in detail

    def test_stub_can_be_both_stale_and_unfinalized_simultaneously(self, audit_module, next_id_module, tmp_path):
        """The two checks are independent and can both fire on the same stub: it can be old AND
        coexist with a real plan at the same time -- neither suppresses the other."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)
        _age_stub(stub, hours=audit_module.STALE_CLAIM_AGE_HOURS + 1)
        plan = todo_dir / _plan_filename(next_id_module, "TODO", 37, name="dispatch-tool-enforcement")
        _write_plan(plan)

        flags = {code for code, _ in audit_module.audit_claim_stub(stub, [plan])}

        assert flags == {"unfinalized_claim_stub", "stale_claim"}

    def test_lettered_child_of_the_same_numeric_id_also_counts_as_coexisting(self, audit_module, next_id_module, tmp_path):
        """A lettered child (e.g. TODO-<id>a-...) sharing the stub's numeric id is just as much
        a plan-shaped file for that number as an unlettered one, so it must count too."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)
        letter = next_id_module.ID_PROJECT_LETTER
        child = todo_dir / f"TODO-{letter}0037a-child-of-37.md"
        _write_plan(child)

        files = audit_module.find_plan_files(todo_dir)
        groups = audit_module.group_claims_by_numeric_id(files)
        flags = {code for code, _ in audit_module.audit_claim_stub(stub, groups[37]["plans"])}

        assert "unfinalized_claim_stub" in flags

    def test_end_to_end_cli_reproduces_and_flags_tonights_incident(self, scripts_dir, next_id_module, tmp_path):
        """The exact filename shape from tonight's incident, run through the real CLI end to
        end: a claim stub and a real plan file, same numeric id, both left on disk."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        stub = todo_dir / _claim_filename(next_id_module, 37)
        _write_claim_stub(stub)
        plan = todo_dir / _plan_filename(next_id_module, "TODO", 37, name="dispatch-tool-enforcement")
        _write_plan(plan)

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir)],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 1, result.stdout
        assert "unfinalized_claim_stub" in result.stdout


class TestDocumentedChecksAreImplemented:
    """Same bug shape as TestDocumentedPrefixesAreScanned, one level up: a check promised in the
    module docstring's "Checks performed" list but never wired into audit_file() is silent
    forever. Parses the promised reason codes out of the docstring and cross-checks them against
    the codes the module actually emits."""

    def test_every_documented_reason_code_is_emitted_somewhere(self, audit_module, scripts_dir):
        documented = _documented_reason_codes(audit_module)
        source = (scripts_dir / "todo-status-audit.py").read_text(encoding="utf-8")

        assert len(documented) >= 10, f"expected the docstring to promise the full check list, parsed {documented}"
        for code in documented:
            assert f'"{code}"' in source, (
                f"'{code}' is documented in the 'Checks performed' list but never appears as a "
                f"flag literal in the script -- a promised check that can never fire."
            )


class TestDeclaredCountsGrammar:
    """The parse half of the declared-counts convention. Nothing here touches a repo: a grammar
    error is a grammar error whether or not the source it names is reachable, which is what lets
    --no-counts still report counts_malformed."""

    def test_none_value_is_a_declaration_not_a_flag(self, audit_module):
        assert audit_module.parse_counts_value("  None.") == ("none", None)

    def test_none_value_is_case_insensitive(self, audit_module):
        assert audit_module.parse_counts_value("  NONE.")[0] == "none"

    def test_well_formed_claim_parses_into_its_parts(self, audit_module):
        outcome, claim = audit_module.parse_counts_value(r"  `console:matches:src/**/*.java:foo\(` = 12")
        assert outcome == "claim"
        assert (claim.repo, claim.kind, claim.pathspec, claim.expected) == ("console", "matches", "src/**/*.java", 12)
        assert claim.pattern.pattern == r"foo\("

    def test_paths_kind_parses_without_a_regex(self, audit_module):
        outcome, claim = audit_module.parse_counts_value("  `board:paths:finished/juneau/FINISHED-*.md` = 23")
        assert outcome == "claim"
        assert claim.kind == "paths"
        assert claim.pattern is None

    def test_regex_may_contain_colons(self, audit_module):
        """maxsplit=3 keeps a ':' inside the regex intact -- otherwise the commonest CSS/Java
        patterns ('font-size:', 'Foo::bar') could not be declared at all."""
        outcome, claim = audit_module.parse_counts_value(r"  `juneau:matches:**/*.css:font-size:\s*[\d.]+em` = 146")
        assert outcome == "claim"
        assert claim.pattern.pattern == r"font-size:\s*[\d.]+em"

    @pytest.mark.parametrize(
        "value",
        [
            "",
            "   ",
            "ten call sites in the rest package",
            "  `console:matches:src/**/*.java:foo` = twelve",
            "  `console:matches:src/**/*.java:foo` = 12 (excluding javadoc)",
            "  `console:matches:src/**/*.java:foo` = 12, `console:files:src/**/*.java:foo` = 9",
            "  `nosuchrepo:paths:x/*.md` = 1",
            "  `console:occurrences:src/**/*.java:foo` = 1",
            "  `console:matches:src/**/*.java` = 1",
            "  `console:matches:src/**/*.java:` = 1",
            "  `board:paths:finished/*.md:extra` = 1",
            "  `console:paths` = 1",
            "  `console:matches:/abs/path/*.java:foo` = 1",
            "  `console:matches:../outside/*.java:foo` = 1",
            "  `console:matches::foo` = 1",
            "  `console:matches:src/**/*.java:[unclosed` = 1",
            # Operator forms that are NOT the two supported ones. Listed because a near-miss operator
            # is the likeliest way to write a floor wrong, and tolerating any of them would mean
            # guessing which comparison the author meant.
            "  `console:matches:src/**/*.java:foo` >= twelve",
            "  `console:matches:src/**/*.java:foo` > 5",
            "  `console:matches:src/**/*.java:foo` <= 5",
            "  `console:matches:src/**/*.java:foo` => 5",
            "  `console:matches:src/**/*.java:foo` >= 5 or so",
        ],
    )
    def test_malformed_values_are_reported_as_malformed(self, audit_module, value):
        assert audit_module.parse_counts_value(value)[0] == "malformed"

    def test_paths_kind_cannot_declare_zero(self, audit_module):
        """`paths ... = 0` is refused at parse time because a zero-file scope is counts_scope_empty
        by rule 2 -- the claim could only ever be reported as a failure, so saying so plainly beats
        letting an author write it."""
        outcome, detail = audit_module.parse_counts_value("  `board:paths:finished/*.md` = 0")
        assert outcome == "malformed"
        assert "rotted" in detail

    def test_matches_kind_may_declare_zero(self, audit_module):
        """'no call sites remain' is a legitimate assertion, unlike 'no files match this glob'."""
        assert audit_module.parse_counts_value("  `console:matches:src/**/*.java:foo` = 0")[0] == "claim"

    def test_malformed_detail_names_the_known_tokens(self, audit_module):
        _, detail = audit_module.parse_counts_value("  `nosuchrepo:paths:x/*.md` = 1")
        for token in ["board", *audit_module.REPO_TOKENS]:
            assert token in detail

    def test_floor_form_parses_and_records_its_operator(self, audit_module):
        """`>=` declares an invariant: "must never drop below N". Equality over a population that
        can legitimately grow flags on benign growth, which invites correcting the number -- and
        correcting an invariant is how a guard gets deleted."""
        outcome, claim = audit_module.parse_counts_value(r"  `juneau:matches:src/**/*.java:foo\(` >= 36")
        assert outcome == "claim"
        assert (claim.operator, claim.expected, claim.is_floor) == (">=", 36, True)

    def test_equality_is_the_default_and_is_not_a_floor(self, audit_module):
        _, claim = audit_module.parse_counts_value(r"  `juneau:matches:src/**/*.java:foo\(` = 36")
        assert (claim.operator, claim.is_floor) == ("=", False)

    def test_floor_of_zero_is_refused(self, audit_module):
        """The one decorative floor catchable without inventing a slack threshold: `>= 0` holds for
        every possible derivation, so it looks like a guard while asserting nothing. Refused for the
        same reason `paths ... = 0` is."""
        outcome, detail = audit_module.parse_counts_value("  `juneau:matches:src/**/*.java:foo` >= 0")
        assert outcome == "malformed"
        assert "asserts nothing" in detail

    def test_equality_may_still_declare_zero(self, audit_module):
        """Refusing `>= 0` must not take `= 0` with it -- "no call sites remain" is a real claim."""
        assert audit_module.parse_counts_value("  `juneau:matches:src/**/*.java:foo` = 0")[0] == "claim"

    def test_rendered_shows_the_operator_actually_applied(self, audit_module):
        """A detail message that printed `= 36` for a claim evaluated as a floor would be unactionable
        in exactly the case where the operator response depends on knowing which it was."""
        _, floor = audit_module.parse_counts_value("  `juneau:paths:src/*.java` >= 2")
        _, exact = audit_module.parse_counts_value("  `juneau:paths:src/*.java` = 2")
        assert floor.rendered().endswith(">= 2")
        assert exact.rendered().endswith("= 2") and ">=" not in exact.rendered()

    def test_malformed_detail_mentions_the_floor_form(self, audit_module):
        """An author who wrote the operator wrong has to be told the floor form exists."""
        _, detail = audit_module.parse_counts_value("  ten call sites")
        assert ">=" in detail


class TestGlobToRegex:
    def test_single_star_does_not_cross_a_slash(self, audit_module):
        """fnmatch's '*' matches '/', which would silently widen every pathspec past the directory
        the author wrote -- a count over a wider file set is a wrong count that looks right."""
        pattern = audit_module.glob_to_regex("src/*.java")
        assert pattern.match("src/A.java")
        assert not pattern.match("src/sub/A.java")

    def test_double_star_slash_matches_zero_directories(self, audit_module):
        pattern = audit_module.glob_to_regex("src/**/*.java")
        assert pattern.match("src/A.java")
        assert pattern.match("src/a/b/A.java")

    def test_question_mark_matches_one_non_slash_character(self, audit_module):
        pattern = audit_module.glob_to_regex("finished/FINISHED-J0445?-*.md")
        assert pattern.match("finished/FINISHED-J0445a-slug.md")
        assert not pattern.match("finished/FINISHED-J0445-slug.md")

    def test_anchored_at_both_ends(self, audit_module):
        pattern = audit_module.glob_to_regex("scripts/*.py")
        assert not pattern.match("other/scripts/x.py")
        assert not pattern.match("scripts/x.pyc")


def _git(root, *args, check: bool = True):
    """Run git in root with a throwaway identity supplied through the ENVIRONMENT.

    Never `git config` (not even --local): the identity is passed per-process so nothing on this
    machine's git configuration is read for authorship or written by these tests.
    """
    env = {
        **os.environ,
        "GIT_AUTHOR_NAME": "audit-test",
        "GIT_AUTHOR_EMAIL": "audit-test@example.invalid",
        "GIT_COMMITTER_NAME": "audit-test",
        "GIT_COMMITTER_EMAIL": "audit-test@example.invalid",
    }
    done = subprocess.run(["git", "-C", str(root), *args], capture_output=True, text=True, env=env, timeout=30)
    if check:
        assert done.returncode == 0, done.stderr
    return done


def _make_repo(root, files: dict):
    """Create a git repo at root with `files` committed, and return root."""
    root.mkdir(parents=True, exist_ok=True)
    _git(root, "init", "-q")
    for name, content in files.items():
        target = root / name
        target.parent.mkdir(parents=True, exist_ok=True)
        if isinstance(content, bytes):
            target.write_bytes(content)
        else:
            target.write_text(content, encoding="utf-8")
    _git(root, "add", "-A")
    _git(root, "commit", "-q", "-m", "seed")
    return root


def _claim(audit_module, descriptor: str, expected: int):
    outcome, claim = audit_module.parse_counts_value(f"  `{descriptor}` = {expected}")
    assert outcome == "claim", claim
    return claim


class TestDeclaredCountsDerivation:
    """The re-derivation half: every number is counted again from committed content on every call."""

    def test_matches_counts_every_occurrence(self, audit_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\nfoo()\n", "src/B.java": "foo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        assert resolver.derive(_claim(audit_module, r"juneau:matches:src/**/*.java:foo\(\)", 3)) == ("ok", 3, "")

    def test_files_counts_distinct_files_not_occurrences(self, audit_module, tmp_path):
        """The 'nine REST classes across ten call sites' shape: two different numbers over one
        pattern, which is why 'files' and 'matches' are separate kinds rather than one."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\nfoo()\n", "src/B.java": "foo()\n", "src/C.java": "bar()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        assert resolver.derive(_claim(audit_module, r"juneau:files:src/**/*.java:foo\(\)", 2))[1] == 2
        assert resolver.derive(_claim(audit_module, r"juneau:matches:src/**/*.java:foo\(\)", 3))[1] == 3

    def test_paths_counts_files_in_scope(self, audit_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"a/x.md": "1", "a/y.md": "2", "a/z.txt": "3"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        assert resolver.derive(_claim(audit_module, "juneau:paths:a/*.md", 2))[1] == 2

    def test_pattern_is_multiline_so_caret_anchors_per_line(self, audit_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"a/x.md": "- 2026-01-01 one\nnot a bullet\n- 2026-01-02 two\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        assert resolver.derive(_claim(audit_module, r"juneau:matches:a/*.md:^- \d{4}-\d{2}-\d{2}", 2))[1] == 2

    def test_many_files_of_differing_sizes_are_read_correctly(self, audit_module, tmp_path):
        """Blobs are fetched through one `git cat-file --batch` (per-file subprocesses turned a
        sub-second check into a minute-long one over a 5500-file pathspec). Batch parsing is
        offset arithmetic, so this covers an empty file and mixed sizes -- the classic off-by-one."""
        files = {"a/empty.txt": "", "a/one.txt": "foo\n", "a/big.txt": "foo\n" * 500, "a/none.txt": "bar\n"}
        repo = _make_repo(tmp_path / "repo", files)
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        assert resolver.derive(_claim(audit_module, "juneau:matches:a/*.txt:foo", 501))[1] == 501
        assert resolver.derive(_claim(audit_module, "juneau:files:a/*.txt:foo", 2))[1] == 2

    def test_binary_files_in_scope_are_skipped_not_decoded(self, audit_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"a/x.bin": b"\x00foo\x00foo", "a/y.txt": "foo\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        assert resolver.derive(_claim(audit_module, "juneau:matches:a/*:foo", 1))[1] == 1

    def test_empty_scope_is_scope_empty_not_a_zero_count(self, audit_module, tmp_path):
        """The specific defence against a stale declaration: a renamed or folded-away file leaves a
        pattern that trivially matches nothing, and 0 == 0 would report success."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        outcome, value, detail = resolver.derive(_claim(audit_module, "juneau:matches:gone/**/*.java:foo", 0))
        assert (outcome, value) == (audit_module.COUNT_SCOPE_EMPTY, None)
        assert "gone/**/*.java" in detail

    def test_missing_checkout_is_unverifiable_not_a_pass(self, audit_module, tmp_path):
        resolver = audit_module.CountsResolver({"juneau": str(tmp_path / "absent")})
        assert resolver.derive(_claim(audit_module, "juneau:paths:a/*.md", 1))[0] == audit_module.COUNT_UNVERIFIABLE

    def test_directory_that_is_not_a_git_repo_is_unverifiable(self, audit_module, tmp_path):
        plain = tmp_path / "plain"
        plain.mkdir()
        (plain / "x.md").write_text("foo\n", encoding="utf-8")
        resolver = audit_module.CountsResolver({"juneau": str(plain)})
        assert resolver.derive(_claim(audit_module, "juneau:paths:*.md", 1))[0] == audit_module.COUNT_UNVERIFIABLE


class TestDeclaredCountsResolveAgainstHead:
    """Counts resolve against committed content, never the working tree. A plan file citing
    working-tree-only files produced a blocked item that two adversarial reviewers read past, and
    these repos are cloned per-WAVE, so a tree-resolved count verifies differently per clone."""

    def test_uncommitted_edit_does_not_change_the_derived_count(self, audit_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\n"})
        (repo / "src/A.java").write_text("foo()\nfoo()\nfoo()\n", encoding="utf-8")
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        assert resolver.derive(_claim(audit_module, r"juneau:matches:src/**/*.java:foo\(\)", 1))[1] == 1

    def test_untracked_file_is_not_counted(self, audit_module, tmp_path):
        """The exact 2026-08-25 shape: files present in this working tree and in nobody's HEAD."""
        repo = _make_repo(tmp_path / "repo", {"scripts/a.py": "x\n"})
        (repo / "scripts/b.py").write_text("x\n", encoding="utf-8")
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        assert resolver.derive(_claim(audit_module, "juneau:paths:scripts/*.py", 1))[1] == 1

    def test_committing_changes_the_count_for_a_fresh_resolver(self, audit_module, tmp_path):
        """Nothing is cached between runs: a new process (here, a new resolver) re-derives from git
        and sees the new commit. This is what makes a stale declaration impossible to pass by
        inertia -- there is no recorded result for it to coast on."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\n"})
        claim = _claim(audit_module, r"juneau:matches:src/**/*.java:foo\(\)", 1)
        assert audit_module.CountsResolver({"juneau": str(repo)}).derive(claim)[1] == 1

        (repo / "src/B.java").write_text("foo()\nfoo()\n", encoding="utf-8")
        _git(repo, "add", "-A")
        _git(repo, "commit", "-q", "-m", "more")
        assert audit_module.CountsResolver({"juneau": str(repo)}).derive(claim)[1] == 3

    def test_board_token_resolves_against_the_filesystem(self, audit_module, tmp_path):
        """The single documented exception: ~/Project Work is not a git repository, and is never
        cloned, so the per-clone divergence that makes tree-resolution wrong cannot arise."""
        board = tmp_path / "Project Work"
        (board / "finished" / "juneau").mkdir(parents=True)
        for name in ("FINISHED-J0001-a.md", "FINISHED-J0002-b.md"):
            (board / "finished" / "juneau" / name).write_text("x\n", encoding="utf-8")
        resolver = audit_module.CountsResolver(project_work=board)
        assert resolver.derive(_claim(audit_module, "board:paths:finished/juneau/FINISHED-*.md", 2))[1] == 2


class TestDeclaredCountsRepoResolution:
    def test_token_naming_this_repo_resolves_to_this_scripts_own_root(self, audit_module):
        """A filesystem fact rather than a guess, so the common case (an item counting inside its
        own project) keeps working from a relocated checkout."""
        self_token = next(t for t, (slug, _) in audit_module.REPO_TOKENS.items() if slug == audit_module.TRACKER_SLUG)
        assert audit_module.CountsResolver().repo_root(self_token) == audit_module.SELF_REPO_ROOT

    def test_other_tokens_resolve_below_home(self, audit_module):
        other = next(t for t, (slug, _) in audit_module.REPO_TOKENS.items() if slug != audit_module.TRACKER_SLUG)
        assert str(audit_module.CountsResolver().repo_root(other)).startswith(str(Path.home()))

    def test_override_wins_over_the_default(self, audit_module, tmp_path):
        resolver = audit_module.CountsResolver({"juneau": str(tmp_path / "elsewhere")})
        assert resolver.repo_root("juneau") == tmp_path / "elsewhere"

    def test_same_path_in_two_repos_is_disambiguated_by_the_token(self, audit_module, tmp_path):
        """Citing the wrong repo was itself one of the 2026-08-25 miscounts: chrome.css exists in
        two of these repos, so there is no implicit 'this repo' default to get wrong."""
        one = _make_repo(tmp_path / "one", {"css/chrome.css": "a{}\n"})
        two = _make_repo(tmp_path / "two", {"css/chrome.css": "a{}\nb{}\n"})
        resolver = audit_module.CountsResolver({"juneau": str(one), "release-manager": str(two)})
        assert resolver.derive(_claim(audit_module, "juneau:matches:css/chrome.css:{}", 1))[1] == 1
        assert resolver.derive(_claim(audit_module, "release-manager:matches:css/chrome.css:{}", 2))[1] == 2


class TestDeclaredCountsFlags:
    """audit_file() wiring: which reason code fires, and -- as importantly -- when none does."""

    def _flags(self, audit_module, tmp_path, next_id_module, counts_lines, resolver=None):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir(exist_ok=True)
        path = todo_dir / _plan_filename(next_id_module, "TODO", 1, name="counted")
        # "In progress." keeps this fixture out of the Ready-gated lifecycle checks, so the
        # assertions below are about the counts codes and nothing else.
        _write_plan(path, status="In progress.", source="\n".join(counts_lines), body="## Goal\n\nx\n")
        return {reason for reason, _ in audit_module.audit_file(path, frozenset(), resolver)}

    def test_absent_counts_line_flags_nothing(self, audit_module, next_id_module, tmp_path):
        """Absence is silence -- the property that lets this check run over 41 pre-convention files
        without a single new flag, and the reason it is not a required field."""
        resolver = audit_module.CountsResolver({"juneau": str(_make_repo(tmp_path / "repo", {"a.md": "x\n"}))})
        assert self._flags(audit_module, tmp_path, next_id_module, [], resolver) == set()

    def test_declared_none_flags_nothing(self, audit_module, next_id_module, tmp_path):
        resolver = audit_module.CountsResolver({"juneau": str(_make_repo(tmp_path / "repo", {"a.md": "x\n"}))})
        assert self._flags(audit_module, tmp_path, next_id_module, ["Counts:  None."], resolver) == set()

    def test_correct_count_flags_nothing(self, audit_module, next_id_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\nfoo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        lines = [r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` = 2"]
        assert self._flags(audit_module, tmp_path, next_id_module, lines, resolver) == set()

    def test_wrong_count_is_flagged_as_mismatch(self, audit_module, next_id_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\nfoo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        lines = [r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` = 3"]
        assert self._flags(audit_module, tmp_path, next_id_module, lines, resolver) == {"counts_mismatch"}

    def test_mismatch_detail_names_both_numbers_and_the_revision(self, audit_module, next_id_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir(exist_ok=True)
        path = todo_dir / _plan_filename(next_id_module, "TODO", 2, name="counted")
        _write_plan(path, status="In progress.", source=r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` = 7", body="## Goal\n\nx\n")
        detail = dict(audit_module.audit_file(path, frozenset(), resolver))["counts_mismatch"]
        assert "= 7" in detail and "re-derived 1" in detail and audit_module.COUNTS_REV in detail

    def test_unanchored_declaration_is_flagged_not_passed(self, audit_module, next_id_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        lines = ["Counts:  `juneau:matches:renamed/**/*.java:foo` = 0"]
        assert self._flags(audit_module, tmp_path, next_id_module, lines, resolver) == {"counts_scope_empty"}

    def test_floor_that_holds_flags_nothing(self, audit_module, next_id_module, tmp_path):
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\nfoo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        lines = [r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` >= 2"]
        assert self._flags(audit_module, tmp_path, next_id_module, lines, resolver) == set()

    def test_floor_breached_by_a_decrease_is_flagged(self, audit_module, next_id_module, tmp_path):
        """The regression a floor exists to catch: the population shrank."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        lines = [r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` >= 2"]
        assert self._flags(audit_module, tmp_path, next_id_module, lines, resolver) == {"counts_below_floor"}

    def test_growth_above_a_floor_stays_green_where_equality_goes_red(self, audit_module, next_id_module, tmp_path):
        """The whole point of the operator, asserted as a contrast rather than two separate tests:
        identical source, identical pattern, and the only difference is `>=` versus `=`. The floor
        passes because nothing was lost; the equality form flags because the number moved. Under the
        equality-only grammar this benign growth is what invited an operator to 'correct' an
        invariant."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\nfoo()\nfoo()\nfoo()\nfoo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        floor = [r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` >= 2"]
        exact = [r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` = 2"]
        assert self._flags(audit_module, tmp_path, next_id_module, floor, resolver) == set()
        assert self._flags(audit_module, tmp_path, next_id_module, exact, resolver) == {"counts_mismatch"}

    def test_empty_scope_under_a_floor_is_scope_empty_not_a_breach(self, audit_module, next_id_module, tmp_path):
        """A rotted pathspec derives 0, which is below any floor -- so without the ordering in
        _flag_declared_counts it would be reported as a regression and send the operator hunting in
        code that is fine. It must be neither a breach nor a pass."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\nfoo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        lines = ["Counts:  `juneau:matches:renamed/**/*.java:foo` >= 2"]
        assert self._flags(audit_module, tmp_path, next_id_module, lines, resolver) == {"counts_scope_empty"}

    def test_breach_and_mismatch_details_are_distinguishable(self, audit_module, next_id_module, tmp_path):
        """The operator response differs -- investigate a regression versus possibly re-baseline -- so
        the two details must not read alike."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir(exist_ok=True)

        floor_path = todo_dir / _plan_filename(next_id_module, "TODO", 3, name="floored")
        _write_plan(floor_path, status="In progress.", source=r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` >= 4", body="## Goal\n\nx\n")
        breach = dict(audit_module.audit_file(floor_path, frozenset(), resolver))["counts_below_floor"]

        exact_path = todo_dir / _plan_filename(next_id_module, "TODO", 4, name="exact")
        _write_plan(exact_path, status="In progress.", source=r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` = 4", body="## Goal\n\nx\n")
        mismatch = dict(audit_module.audit_file(exact_path, frozenset(), resolver))["counts_mismatch"]

        assert ">= 4" in breach and "BREACHED" in breach and "Do NOT lower" in breach
        assert "= 4" in mismatch and "BREACHED" not in mismatch and "floor" not in mismatch

    def test_unreachable_repo_is_flagged_as_unverifiable(self, audit_module, next_id_module, tmp_path):
        resolver = audit_module.CountsResolver({"juneau": str(tmp_path / "absent")})
        lines = ["Counts:  `juneau:paths:src/*.java` = 1"]
        assert self._flags(audit_module, tmp_path, next_id_module, lines, resolver) == {"counts_unverifiable"}

    def test_malformed_line_is_flagged(self, audit_module, next_id_module, tmp_path):
        resolver = audit_module.CountsResolver({"juneau": str(_make_repo(tmp_path / "repo", {"a.md": "x\n"}))})
        assert self._flags(audit_module, tmp_path, next_id_module, ["Counts:  ten call sites"], resolver) == {"counts_malformed"}

    def test_malformed_is_reported_without_a_resolver(self, audit_module, next_id_module, tmp_path):
        """Grammar is checked with or without source access, so --no-counts cannot hide a broken
        declaration -- only an unverified one."""
        assert self._flags(audit_module, tmp_path, next_id_module, ["Counts:  ten call sites"], None) == {"counts_malformed"}

    def test_no_resolver_means_numbers_are_not_reported_as_agreeing(self, audit_module, next_id_module, tmp_path):
        """Silence about a number that was never checked, rather than a manufactured pass."""
        lines = [r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` = 999"]
        assert self._flags(audit_module, tmp_path, next_id_module, lines, None) == set()

    def test_every_declaration_on_a_repeated_field_is_checked(self, audit_module, next_id_module, tmp_path):
        """One declaration per line, field repeated -- so a regex containing a comma is safe, and no
        declaration can hide behind a sibling on the same line."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\nfoo()\n", "src/B.java": "foo()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        lines = [
            r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` = 3",
            r"Counts:  `juneau:files:src/**/*.java:foo\(\)` = 9",
        ]
        flags = [reason for reason, _ in audit_module.audit_file(
            self._plan(audit_module, tmp_path, next_id_module, lines), frozenset(), resolver)]
        assert flags == ["counts_mismatch"]

    def _plan(self, audit_module, tmp_path, next_id_module, counts_lines):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir(exist_ok=True)
        path = todo_dir / _plan_filename(next_id_module, "TODO", 3, name="counted")
        _write_plan(path, status="In progress.", source="\n".join(counts_lines), body="## Goal\n\nx\n")
        return path

    def test_field_name_is_case_insensitive_like_the_other_headers(self, audit_module, next_id_module, tmp_path):
        assert self._flags(audit_module, tmp_path, next_id_module, ["counts:  ten call sites"], None) == {"counts_malformed"}

    def test_zero_declared_and_zero_derived_passes_when_the_scope_still_exists(self, audit_module, next_id_module, tmp_path):
        """'No call sites remain' is a real assertion. Documented residual: this cannot distinguish
        a correctly-zero count from a pattern that has rotted while its files stayed put -- which is
        why the convention advises declaring the positive baseline instead where one exists."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "bar()\n"})
        resolver = audit_module.CountsResolver({"juneau": str(repo)})
        lines = [r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` = 0"]
        assert self._flags(audit_module, tmp_path, next_id_module, lines, resolver) == set()


class TestMainCliEndToEnd:
    def test_declared_count_mismatch_is_reported_end_to_end(self, scripts_dir, next_id_module, tmp_path):
        """Whole-CLI wiring: --repo-root is honoured, the count is re-derived, and the stale number
        is reported with a non-zero exit."""
        repo = _make_repo(tmp_path / "repo", {"src/A.java": "foo()\n"})
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        filename = _plan_filename(next_id_module, "TODO", 1, name="counted")
        _write_plan(todo_dir / filename, status="In progress.", source=r"Counts:  `juneau:matches:src/**/*.java:foo\(\)` = 4", body="## Goal\n\nx\n")

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir), "--repo-root", f"juneau={repo}"],
            capture_output=True,
            text=True,
            timeout=60,
        )

        assert result.returncode == 1, result.stdout
        assert "counts_mismatch" in result.stdout
        assert "re-derived 1" in result.stdout
        assert "re-derived from HEAD this run" in result.stderr  # never silently skipped

    def test_no_counts_announces_itself_and_still_reports_bad_grammar(self, scripts_dir, next_id_module, tmp_path):
        """"No count disagreed" and "no count was checked" must not look the same on the terminal."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        _write_plan(todo_dir / _plan_filename(next_id_module, "TODO", 1, name="counted"),
                    status="In progress.", source="Counts:  eleven call sites", body="## Goal\n\nx\n")

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir), "--no-counts"],
            capture_output=True,
            text=True,
            timeout=60,
        )

        assert result.returncode == 1, result.stdout
        assert "counts_malformed" in result.stdout
        assert "not re-derived" in result.stderr

    def test_malformed_repo_root_override_is_a_hard_error(self, scripts_dir, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir), "--repo-root", "juneau"],
            capture_output=True,
            text=True,
            timeout=60,
        )

        assert result.returncode == 2
        assert "TOKEN=PATH" in result.stderr

    def test_accepted_wave_drift_is_reported_end_to_end(self, scripts_dir, next_id_module, tmp_path):
        """Whole-CLI wiring: --board is read, the wave is resolved as accepted+closed, and the
        stale item is reported with a non-zero exit."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        filename = _plan_filename(next_id_module, "READY", 1, name="shipped")
        _write_plan(
            todo_dir / filename,
            status="Ready to execute. Do not farm until the operator starts the WAVE queue.",
            source="Filed under WAVE-0003.",
            body="## Goal\n\nx\n",
        )
        board = _write_board(tmp_path, "WAVE-0003 was accepted and pushed 2026-08-25. WAVE file deleted.\n")

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir), "--board", str(board)],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 1, result.stderr
        assert "ready_but_wave_already_accepted" in result.stdout
        assert filename in result.stdout
        assert "WAVE-0003" in result.stderr  # the board record is announced, never silently skipped

    def test_missing_board_is_announced_as_inert_not_silently_skipped(self, scripts_dir, next_id_module, tmp_path):
        """A silently-skipped wave check is indistinguishable from a clean one -- the same
        silent-zero trap this script already guards for a missing scan directory."""
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        _write_plan(todo_dir / _plan_filename(next_id_module, "READY", 1), source="Filed under WAVE-0003.", body="## Goal\n\nx\n")

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir), "--board", str(tmp_path / "absent.md")],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 0, result.stdout
        assert "inert" in result.stderr

    def test_need_input_file_is_scanned_end_to_end(self, scripts_dir, next_id_module, tmp_path):
        letter = next_id_module.ID_PROJECT_LETTER
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()
        filename = f"NEED_INPUT-{letter}0001-blocked.md"
        (todo_dir / filename).write_text("# Sample\n\nNo status or complexity header here.\n", encoding="utf-8")

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir)],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 1, result.stderr  # at least one file flagged
        assert filename in result.stdout
        assert "Scanned 1 file(s); 1 flagged." in result.stdout

    def test_clean_tracker_exits_zero(self, scripts_dir, tmp_path):
        todo_dir = tmp_path / "todos"
        todo_dir.mkdir()

        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(todo_dir)],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 0, result.stderr

    def test_missing_directory_is_hard_error(self, scripts_dir, tmp_path):
        result = subprocess.run(
            [sys.executable, str(scripts_dir / "todo-status-audit.py"), "--dir", str(tmp_path / "does-not-exist")],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 2
