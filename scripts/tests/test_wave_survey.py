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
Tests for scripts/wave-survey.py -- the cross-project wave-candidate survey.

Deliberately self-contained: unlike test_todo_next_id.py / test_todo_status_audit.py, this file
does NOT use conftest.py's `next_id_module` / `audit_module` fixtures (or add a new fixture to
conftest.py itself). wave-survey.py was authored under a strict file-scope constraint -- a
concurrent session was simultaneously editing todo-next-id.py, todo-status-audit.py, and every
file under scripts/tests/, including conftest.py -- so this file loads its own module under test
(see `survey_module` below, mirroring conftest.py's `_load_script()` shape but duplicated locally
rather than imported) and never touches, imports from, or depends on the behavior of any of those
concurrently-edited files. It also predates and does not depend on any HIPRI registry entry those
files may add for this script -- see the module's own PLACEMENT docstring section and the
delivery report for that owed follow-up.

wave-survey.py surveys THREE trackers at once (unlike todo-next-id.py / todo-status-audit.py,
which are single-repo copies), so several tests here exercise that directly: multiple id letters
(J/R/C) in the same run, cross-tracker sort order, and one tracker directory missing while the
others are still reported.
"""

from __future__ import annotations

import importlib.util
import json
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
SCRIPT_PATH = SCRIPTS_DIR / "wave-survey.py"


def _load_survey_module():
    """Load scripts/wave-survey.py (hyphenated -- not `import`able by name) as a fresh module
    object. Duplicated from conftest.py's `_load_script()` rather than imported from it -- see
    the module docstring's "Deliberately self-contained" paragraph."""
    spec = importlib.util.spec_from_file_location("_undertest_wave_survey", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def survey_module():
    """A fresh scripts/wave-survey.py module object per test (never shared/cached across tests,
    same reasoning as conftest.py's module fixtures: a test that monkeypatches a module-level
    constant must never leak that mutation into an unrelated test)."""
    return _load_survey_module()


def _write_item(
    tracker_dir: Path,
    filename: str,
    *,
    title: str = "Sample item",
    status: str = "Ready to execute.",
    body: str = "",
) -> Path:
    """Write one synthetic tracker item file with the minimal real shape (H1 title, 'Current
    status:' line, optional body/sections)."""
    path = tracker_dir / filename
    path.write_text(f"# {title}\n\nCurrent status: {status}\n\n{body}", encoding="utf-8")
    return path


class TestFilenameRecognition:
    @pytest.mark.parametrize("prefix", ["TODO", "READY", "MAYBE", "HOLD"])
    @pytest.mark.parametrize("letter", ["J", "R", "C"])
    def test_recognizes_every_lifecycle_prefix_and_letter(self, survey_module, prefix, letter):
        m = survey_module.FILENAME_RE.match(f"{prefix}-{letter}0042-sample-slug.md")
        assert m is not None
        assert m.groups() == (prefix, letter, "0042", "", "sample-slug")

    def test_recognizes_a_lettered_child_suffix(self, survey_module):
        m = survey_module.FILENAME_RE.match("TODO-C0016a-irs-artwork-override.md")
        assert m is not None
        assert m.groups() == ("TODO", "C", "0016", "a", "irs-artwork-override")

    def test_a_hyphenated_non_child_segment_is_not_mistaken_for_a_child_suffix(self, survey_module):
        """"MAYBE-J0469-o06-vacuous-....md" -- "o06" is separated from the digits by a hyphen, so
        it is part of the slug, not a lettered child (which has no separator, e.g. "C0016a")."""
        m = survey_module.FILENAME_RE.match("MAYBE-J0469-o06-vacuous-border-radius-assertion.md")
        assert m is not None
        assert m.groups() == ("MAYBE", "J", "0469", "", "o06-vacuous-border-radius-assertion")

    def test_does_not_match_the_tracker_index_file(self, survey_module):
        assert survey_module.FILENAME_RE.match("TODO.md") is None

    def test_does_not_match_finished_or_cancelled_or_need_input(self, survey_module):
        """Out of scope by design -- see the module docstring's "Lifecycle states surveyed"
        paragraph. FINISHED/CANCELLED also physically live in finished/, not todos/, but the
        regex itself should not accept them either."""
        assert survey_module.FILENAME_RE.match("FINISHED-J0001-old.md") is None
        assert survey_module.FILENAME_RE.match("CANCELLED-J0001-old.md") is None
        assert survey_module.FILENAME_RE.match("NEED_INPUT-J0001-blocked.md") is None

    def test_filename_re_alone_matches_a_claim_stub_shape(self, survey_module):
        """FILENAME_RE's permissive slug group would happily accept "CLAIMED" as an ordinary
        slug -- the exclusion lives in CLAIM_STUB_RE / scan_tracker(), not here. This test pins
        that FILENAME_RE's own behavior doesn't quietly change out from under that design."""
        assert survey_module.FILENAME_RE.match("TODO-J0099-CLAIMED.md") is not None

    def test_claim_stub_re_matches_a_reservation_stub(self, survey_module):
        """todo-next-id.py's id-reservation stub is deliberately not a survey candidate -- it
        carries no title/status worth reporting. Claim stubs are only ever issued for a bare
        numeric TODO- id (never lettered, never any other lifecycle prefix)."""
        assert survey_module.CLAIM_STUB_RE.match("TODO-J0099-CLAIMED.md")
        assert not survey_module.CLAIM_STUB_RE.match("READY-J0099-CLAIMED.md")
        assert not survey_module.CLAIM_STUB_RE.match("TODO-J0099a-CLAIMED.md")


class TestHipriDetection:
    @pytest.mark.parametrize("prefix", ["TODO", "READY", "MAYBE", "HOLD"])
    def test_matches_hipri_segment_for_every_prefix(self, survey_module, prefix):
        assert survey_module.HIPRI_RE.match(f"{prefix}-C0051-HIPRI-retire-manual-fetch.md")

    def test_matches_hipri_after_a_lettered_child_suffix(self, survey_module):
        assert survey_module.HIPRI_RE.match("TODO-J0174a-HIPRI-sample-slug.md")

    def test_does_not_match_a_non_hipri_filename(self, survey_module):
        assert not survey_module.HIPRI_RE.match("TODO-C0051-retire-manual-fetch.md")

    def test_does_not_match_hipri_appearing_only_inside_the_slug(self, survey_module):
        """The marker must sit immediately after the id, not merely appear somewhere in the
        slug -- a slug that happens to start with a different word containing similar text must
        not be misread as the marker."""
        assert not survey_module.HIPRI_RE.match("TODO-C0051-not-hipri-related-slug.md")


class TestOpenQuestions:
    def test_no_open_questions_section_is_not_unresolved(self, survey_module):
        text = "# Sample\n\nCurrent status: Ready to execute.\n\n## Goal\n\nDo the thing.\n"
        assert survey_module.has_unresolved_open_questions(text) is False

    def test_empty_open_questions_section_is_not_unresolved(self, survey_module):
        text = "# Sample\n\n## Open questions\n\nNothing is open.\n"
        assert survey_module.has_unresolved_open_questions(text) is False

    def test_unnumbered_open_questions_prose_is_not_unresolved(self, survey_module):
        text = "# Sample\n\n## Open questions\n\nAll open questions were answered on 2026-08-25.\n"
        assert survey_module.has_unresolved_open_questions(text) is False

    def test_unresolved_numbered_item_is_unresolved(self, survey_module):
        text = "# Sample\n\n## Open questions\n\n1. What color should the button be?\n"
        assert survey_module.has_unresolved_open_questions(text) is True

    def test_resolved_numbered_item_is_not_unresolved(self, survey_module):
        text = "# Sample\n\n## Open questions\n\n1. What color should the button be? Resolved: blue.\n"
        assert survey_module.has_unresolved_open_questions(text) is False

    def test_one_resolved_one_unresolved_is_unresolved(self, survey_module):
        text = (
            "# Sample\n\n## Open questions\n\n"
            "1. What color? Decided: blue.\n"
            "2. What size?\n"
        )
        assert survey_module.has_unresolved_open_questions(text) is True


class TestTitleAndStatusExtraction:
    def test_strips_leading_id_prefix_from_title(self, survey_module):
        text = "# TODO-J0447: Dogfood / chrome feedback\n\nCurrent status: In progress.\n"
        assert survey_module.extract_title(text) == "Dogfood / chrome feedback"

    def test_keeps_title_with_no_id_prefix_as_is(self, survey_module):
        text = "# Cron-expression builder field\n\nCurrent status: Parked.\n"
        assert survey_module.extract_title(text) == "Cron-expression builder field"

    def test_missing_heading_reports_a_named_placeholder(self, survey_module):
        assert survey_module.extract_title("Current status: Done.\n") == "(no title heading)"

    def test_extracts_and_strips_decoration_from_status(self, survey_module):
        text = "# Sample\n\nCurrent status:  **Ready to execute.**\n"
        assert survey_module.extract_status(text) == "Ready to execute.**"

    def test_missing_status_line_reports_a_named_placeholder(self, survey_module):
        assert survey_module.extract_status("# Sample\n\nNo status here.\n") == "(no 'Current status:' line)"

    def test_empty_status_value_reports_a_named_placeholder(self, survey_module):
        assert survey_module.extract_status("# Sample\n\nCurrent status: \n") == "(empty status value)"


class TestClassify:
    def test_ready_with_no_open_questions_is_runnable(self, survey_module):
        runnable, reason = survey_module.classify("READY", False)
        assert runnable is True
        assert reason == ""

    def test_ready_with_open_questions_is_blocked(self, survey_module):
        runnable, reason = survey_module.classify("READY", True)
        assert runnable is False
        assert "open questions" in reason

    def test_todo_is_always_blocked(self, survey_module):
        runnable, reason = survey_module.classify("TODO", False)
        assert runnable is False
        assert "READY" in reason

    def test_maybe_is_always_blocked(self, survey_module):
        runnable, reason = survey_module.classify("MAYBE", False)
        assert runnable is False
        assert "MAYBE" in reason

    def test_hold_is_always_blocked(self, survey_module):
        runnable, reason = survey_module.classify("HOLD", False)
        assert runnable is False
        assert "HOLD" in reason

    def test_unrecognized_lifecycle_raises(self, survey_module):
        with pytest.raises(ValueError):
            survey_module.classify("BOGUS", False)


class TestParseItem:
    def test_parses_a_full_item(self, survey_module, tmp_path):
        path = _write_item(
            tmp_path,
            "READY-C0002-HIPRI-sample-fix.md",
            title="TODO-C0002: Sample fix",
            status="Ready to execute. No open questions.",
        )

        item = survey_module.parse_item(path, "sandbox-support-console", "Sandbox Support Console")

        assert item["id"] == "READY-C0002"
        assert item["repo"] == "sandbox-support-console"
        assert item["lifecycle"] == "READY"
        assert item["hipri"] is True
        assert item["runnable"] is True
        assert item["block_reason"] == ""
        assert item["title"] == "Sample fix"
        assert item["status"] == "Ready to execute. No open questions."
        assert item["numeric_id"] == 2
        assert item["child_suffix"] == ""
        assert item["path"] == str(path)

    def test_parses_a_blocked_item_with_unresolved_open_questions(self, survey_module, tmp_path):
        path = _write_item(
            tmp_path,
            "READY-J0001-not-actually-ready.md",
            status="Ready to execute.",
            body="## Open questions\n\n1. Is this actually safe to ship?\n",
        )

        item = survey_module.parse_item(path, "juneau", "Apache Juneau")

        assert item["runnable"] is False
        assert "open questions" in item["block_reason"]


class TestScanTracker:
    def test_missing_directory_returns_none(self, survey_module, tmp_path):
        assert survey_module.scan_tracker(tmp_path / "does-not-exist", "juneau", "Apache Juneau") is None

    def test_empty_present_directory_returns_empty_list(self, survey_module, tmp_path):
        tracker_dir = tmp_path / "todos"
        tracker_dir.mkdir()
        assert survey_module.scan_tracker(tracker_dir, "juneau", "Apache Juneau") == []

    def test_ignores_the_index_file_and_non_matching_files(self, survey_module, tmp_path):
        tracker_dir = tmp_path / "todos"
        tracker_dir.mkdir()
        (tracker_dir / "TODO.md").write_text("# TODO\n\n## Status\n")
        (tracker_dir / "README.md").write_text("# README\n")
        (tracker_dir / "TODO-J0099-CLAIMED.md").write_text("CLAIMED placeholder\n")
        _write_item(tracker_dir, "TODO-J0001-real-item.md")

        items = survey_module.scan_tracker(tracker_dir, "juneau", "Apache Juneau")

        assert [i["id"] for i in items] == ["TODO-J0001"]

    def test_ignores_subdirectories(self, survey_module, tmp_path):
        """Trackers carry non-item subdirectories (e.g. _cu-prompts/, scratch/, reviews/) --
        these must never be mistaken for item files even if a directory name happened to end
        in something glob-shaped."""
        tracker_dir = tmp_path / "todos"
        tracker_dir.mkdir()
        (tracker_dir / "scratch").mkdir()
        _write_item(tracker_dir, "TODO-J0001-real-item.md")

        items = survey_module.scan_tracker(tracker_dir, "juneau", "Apache Juneau")

        assert [i["id"] for i in items] == ["TODO-J0001"]


class TestCollectAndSortOrder:
    def _make_trackers(self, tmp_path):
        juneau = tmp_path / "todos" / "juneau"
        rm = tmp_path / "todos" / "juneau-release-manager"
        console = tmp_path / "todos" / "sandbox-support-console"
        juneau.mkdir(parents=True)
        rm.mkdir(parents=True)
        console.mkdir(parents=True)
        return juneau, rm, console

    def test_missing_tracker_is_reported_but_others_still_scanned(self, survey_module, tmp_path):
        juneau, _rm, console = self._make_trackers(tmp_path)
        # Delete the release-manager tracker dir after creating the root, so it is "missing" (not
        # merely empty) exactly like scan_tracker()'s missing-vs-empty distinction requires.
        (tmp_path / "todos" / "juneau-release-manager").rmdir()
        _write_item(juneau, "TODO-J0001-a.md")
        _write_item(console, "TODO-C0001-b.md")

        items, missing = survey_module.collect(tmp_path)

        assert len(missing) == 1
        assert missing[0].endswith("juneau-release-manager")
        assert {i["id"] for i in items} == {"TODO-J0001", "TODO-C0001"}

    def test_repo_filter_restricts_scan_to_requested_slugs(self, survey_module, tmp_path):
        juneau, _rm, console = self._make_trackers(tmp_path)
        _write_item(juneau, "TODO-J0001-a.md")
        _write_item(console, "TODO-C0001-b.md")

        items, missing = survey_module.collect(tmp_path, repo_slugs=("juneau",))

        assert missing == []
        assert [i["id"] for i in items] == ["TODO-J0001"]

    def test_runnable_items_sort_before_blocked_items(self, survey_module, tmp_path):
        juneau, _rm, _console = self._make_trackers(tmp_path)
        _write_item(juneau, "TODO-J0001-blocked.md", status="Not yet started.")
        _write_item(juneau, "READY-J0002-runnable.md", status="Ready to execute.")

        items, _missing = survey_module.collect(tmp_path)

        assert [i["id"] for i in items] == ["READY-J0002", "TODO-J0001"]

    def test_hipri_sorts_first_within_the_blocked_group_but_not_ahead_of_runnable(self, survey_module, tmp_path):
        juneau, _rm, _console = self._make_trackers(tmp_path)
        _write_item(juneau, "TODO-J0001-HIPRI-urgent-but-blocked.md", status="Not yet started.")
        _write_item(juneau, "TODO-J0002-ordinary-blocked.md", status="Not yet started.")
        _write_item(juneau, "READY-J0003-runnable.md", status="Ready to execute.")

        items, _missing = survey_module.collect(tmp_path)

        # Runnable-now is the primary split (see sort_key()'s docstring): the READY item leads
        # even though neither TODO item is HIPRI-marked ahead of it.
        assert [i["id"] for i in items] == ["READY-J0003", "TODO-J0001", "TODO-J0002"]

    def test_cross_tracker_order_follows_trackers_declaration_order(self, survey_module, tmp_path):
        juneau, rm, console = self._make_trackers(tmp_path)
        _write_item(console, "TODO-C0001-a.md", status="Not yet started.")
        _write_item(rm, "TODO-R0001-b.md", status="Not yet started.")
        _write_item(juneau, "TODO-J0001-c.md", status="Not yet started.")

        items, _missing = survey_module.collect(tmp_path)

        assert [i["id"] for i in items] == ["TODO-J0001", "TODO-R0001", "TODO-C0001"]

    def test_numeric_id_orders_correctly_past_ten(self, survey_module, tmp_path):
        """A naive string sort would put "TODO-J0010" before "TODO-J0002" -- numeric_id must be
        compared as an int, not as the zero-padded digit string."""
        juneau, _rm, _console = self._make_trackers(tmp_path)
        _write_item(juneau, "TODO-J0010-later.md", status="Not yet started.")
        _write_item(juneau, "TODO-J0002-earlier.md", status="Not yet started.")

        items, _missing = survey_module.collect(tmp_path)

        assert [i["id"] for i in items] == ["TODO-J0002", "TODO-J0010"]


class TestFormatTable:
    def test_no_items_reports_a_named_empty_result(self, survey_module):
        assert "no candidate items" in survey_module.format_table([])

    def test_includes_both_section_banners_by_default(self, survey_module, tmp_path):
        path_runnable = _write_item(tmp_path, "READY-J0001-a.md", status="Ready to execute.")
        path_blocked = _write_item(tmp_path, "TODO-J0002-b.md", status="Not yet started.")
        items = [
            survey_module.parse_item(path_runnable, "juneau", "Apache Juneau"),
            survey_module.parse_item(path_blocked, "juneau", "Apache Juneau"),
        ]

        table = survey_module.format_table(items)

        assert "RUNNABLE NOW" in table
        assert "BLOCKED / NOT READY" in table
        assert "READY-J0001" in table
        assert "TODO-J0002" in table

    def test_include_blocked_false_omits_the_blocked_section(self, survey_module, tmp_path):
        path_runnable = _write_item(tmp_path, "READY-J0001-a.md", status="Ready to execute.")
        items = [survey_module.parse_item(path_runnable, "juneau", "Apache Juneau")]

        table = survey_module.format_table(items, include_blocked=False)

        assert "RUNNABLE NOW" in table
        assert "BLOCKED / NOT READY" not in table

    def test_empty_group_prints_none_placeholder(self, survey_module, tmp_path):
        path_blocked = _write_item(tmp_path, "TODO-J0002-b.md", status="Not yet started.")
        items = [survey_module.parse_item(path_blocked, "juneau", "Apache Juneau")]

        table = survey_module.format_table(items)

        assert "(none)" in table  # the RUNNABLE NOW section has nothing to show


class TestToJsonRow:
    def test_drops_internal_sort_only_fields(self, survey_module, tmp_path):
        path = _write_item(tmp_path, "TODO-J0001-a.md")
        item = survey_module.parse_item(path, "juneau", "Apache Juneau")

        row = survey_module.to_json_row(item)

        assert "numeric_id" not in row
        assert "child_suffix" not in row
        assert row["id"] == "TODO-J0001"


class TestCli:
    """End-to-end CLI tests via subprocess, the same style test_todo_status_audit.py uses for its
    exit-code assertions -- this proves the real argv/stdout/exit-code contract, not just the
    underlying functions."""

    def _make_root(self, tmp_path):
        root = tmp_path / "project-work"
        (root / "todos" / "juneau").mkdir(parents=True)
        (root / "todos" / "juneau-release-manager").mkdir(parents=True)
        (root / "todos" / "sandbox-support-console").mkdir(parents=True)
        return root

    def test_exit_zero_and_empty_report_for_present_but_empty_trackers(self, tmp_path):
        root = self._make_root(tmp_path)

        result = subprocess.run(
            [sys.executable, str(SCRIPT_PATH), "--root", str(root)],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 0, result.stdout + result.stderr
        assert "no candidate items" in result.stdout

    def test_exit_two_when_a_tracker_directory_is_missing(self, tmp_path):
        root = tmp_path / "project-work"
        (root / "todos" / "juneau").mkdir(parents=True)
        # release-manager and console tracker dirs deliberately not created.

        result = subprocess.run(
            [sys.executable, str(SCRIPT_PATH), "--root", str(root)],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 2
        assert "does not exist" in result.stderr

    def test_json_output_round_trips(self, tmp_path):
        root = self._make_root(tmp_path)
        _write_item(root / "todos" / "juneau", "READY-J0001-a.md", status="Ready to execute.")
        _write_item(root / "todos" / "sandbox-support-console", "TODO-C0051-HIPRI-b.md", status="Not yet started.")

        result = subprocess.run(
            [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--json"],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 0, result.stdout + result.stderr
        payload = json.loads(result.stdout)
        ids = {item["id"] for item in payload["items"]}
        assert ids == {"READY-J0001", "TODO-C0051"}
        hipri_item = next(item for item in payload["items"] if item["id"] == "TODO-C0051")
        assert hipri_item["hipri"] is True
        assert payload["missing_trackers"] == []

    def test_repo_flag_restricts_output(self, tmp_path):
        root = self._make_root(tmp_path)
        _write_item(root / "todos" / "juneau", "TODO-J0001-a.md", status="Not yet started.")
        _write_item(root / "todos" / "sandbox-support-console", "TODO-C0001-b.md", status="Not yet started.")

        result = subprocess.run(
            [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--repo", "juneau", "--json"],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 0, result.stdout + result.stderr
        payload = json.loads(result.stdout)
        assert [item["id"] for item in payload["items"]] == ["TODO-J0001"]

    def test_unknown_repo_flag_is_rejected_by_argparse(self, tmp_path):
        root = self._make_root(tmp_path)

        result = subprocess.run(
            [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--repo", "not-a-real-tracker"],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode != 0
        assert "invalid choice" in result.stderr

    def test_runnable_only_omits_blocked_items_and_section(self, tmp_path):
        root = self._make_root(tmp_path)
        _write_item(root / "todos" / "juneau", "READY-J0001-a.md", status="Ready to execute.")
        _write_item(root / "todos" / "juneau", "TODO-J0002-b.md", status="Not yet started.")

        result = subprocess.run(
            [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--runnable-only"],
            capture_output=True,
            text=True,
            timeout=15,
        )

        assert result.returncode == 0, result.stdout + result.stderr
        assert "READY-J0001" in result.stdout
        assert "TODO-J0002" not in result.stdout
        assert "BLOCKED / NOT READY" not in result.stdout
