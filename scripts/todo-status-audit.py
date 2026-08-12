#!/usr/bin/env python3
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
Best-effort status/header consistency pre-filter for Apache Juneau's .work/todo/ plan files.

Checks every TODO-<id>-*.md / READY-<id>-*.md / MAYBE-<id>-*.md file directly under .work/todo/
(FINISHED-/CANCELLED-*.md archives are explicitly out of scope -- per
agents/skills/juneau-todo-management/SKILL.md, "status line is not required in FINISHED archives")
against that skill's "Per-file `Current status:` and `Complexity:` header" rules, and flags
candidate inconsistencies. This is a PRE-FILTER, not a validator: it flags candidates for a human
(or agent) to look at, and will not catch everything on older, format-drifted files -- tolerant,
best-effort markdown-header parsing throughout.

Checks performed (each file may accumulate multiple flags):
  - missing_status_header       No "Current status:" line found anywhere in the file.
  - missing_complexity_header   No "Complexity:" line found anywhere in the file.
  - status_header_misplaced     "Current status:" appears at or after the first "## " section
                                 heading (it must come before it, per the skill's "Placement" rule).
  - unrecognized_status_phrase  The status text doesn't start with one of the skill's documented
                                 phrases for this file's prefix (TODO/READY: "Waiting for user
                                 input on open questions.", "Ready to execute.", "In progress.";
                                 MAYBE: must start with "Parked"). Free-form variants that legitimately
                                 extend a recognized prefix (e.g. "Ready to execute (all items
                                 independently actionable)." ) are NOT flagged -- only prefix
                                 mismatches are.
  - ready_but_has_open_questions
                                 Status starts with "Ready to execute" but the file still has a
                                 "## Open questions" section containing at least one numbered item
                                 whose text doesn't look answered/resolved.
  - ready_prefix_waiting_status A READY-*.md file whose status still says "Waiting for user input"
                                 (READY files should have no unresolved open questions left).
  - todo_prefix_marked_ready    A TODO-*.md file (not yet renamed) whose status already says "Ready
                                 to execute" -- a likely-missed rename to READY-*.md (see the skill's
                                 "OQA lifecycle -> status transitions").
  - parked_status_wrong_prefix  A TODO-*.md/READY-*.md file whose status starts with "Parked" (that
                                 wording is reserved for MAYBE-*.md files).
  - maybe_prefix_non_parked     A MAYBE-*.md file whose status does NOT start with "Parked".

Usage:
    ./scripts/todo-status-audit.py
    ./scripts/todo-status-audit.py --verbose
    ./scripts/todo-status-audit.py --dir /path/to/alternate/todo/dir

Options:
    --dir <path>    Directory to scan (default: .work/todo/ under the repo root). Non-recursive --
                    only *.md files directly in this directory are considered.
    --verbose, -v   Also print files that passed every check (default: only print flagged files).
    --help, -h      Show this help message.

Exit status:
    0   No inconsistencies flagged.
    1   At least one file was flagged.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_TODO_DIR = REPO_ROOT / ".work" / "todo"

FILENAME_RE = re.compile(r"^(TODO|READY|MAYBE)-\d+[a-z]*-.*\.md$")

STATUS_LINE_RE = re.compile(r"^\s*Current status:\s*(.*)$", re.IGNORECASE | re.MULTILINE)
COMPLEXITY_LINE_RE = re.compile(r"^\s*Complexity:\s*(.*)$", re.IGNORECASE | re.MULTILINE)
SECTION_HEADING_RE = re.compile(r"^##\s+(.*)$", re.MULTILINE)
NUMBERED_ITEM_RE = re.compile(r"^\s*\d+[.)]\s+(.*)$", re.MULTILINE)

RESOLVED_MARKERS = ("resolved", "answered", "decided")

# Recognized status prefixes (case-insensitive, checked with str.startswith after lowercasing) per
# the skill's "Status wording rules" -- kept separate from TODO/READY vs MAYBE since the two file
# families use disjoint wording.
TODO_READY_STATUS_PREFIXES = (
    "waiting for user input on open questions",
    "ready to execute",
    "in progress",
)
MAYBE_STATUS_PREFIX = "parked"


def find_plan_files(todo_dir: Path) -> list:
    """Every TODO-/READY-/MAYBE-<id>[<letter>]-*.md file directly under todo_dir, sorted by name."""
    if not todo_dir.is_dir():
        return []
    return sorted(p for p in todo_dir.glob("*.md") if FILENAME_RE.match(p.name))


def file_prefix(path: Path) -> str:
    """"TODO", "READY", or "MAYBE" (the filename already matched FILENAME_RE)."""
    return path.name.split("-", 1)[0]


def extract_section(text: str, heading_text: str) -> str | None:
    """
    Return the body text of the first "## <heading_text>" section (case-insensitive substring match
    on the heading line), from just after that heading line up to (not including) the next "## "
    heading or end of file. Returns None if no matching heading exists.
    """
    headings = list(SECTION_HEADING_RE.finditer(text))
    for i, m in enumerate(headings):
        if heading_text.lower() in m.group(1).strip().lower():
            start = m.end()
            end = headings[i + 1].start() if i + 1 < len(headings) else len(text)
            return text[start:end]
    return None


def open_questions_are_unresolved(section_body: str) -> bool:
    """True if the "## Open questions" section has at least one numbered item that doesn't look answered/resolved."""
    items = list(NUMBERED_ITEM_RE.finditer(section_body))
    if not items:
        # No numbered items at all (empty placeholder, or prose-only) -- nothing concretely
        # "open" to flag; conservative by design (avoid false positives on trivial sections).
        return False
    for i, m in enumerate(items):
        start = m.start()
        end = items[i + 1].start() if i + 1 < len(items) else len(section_body)
        block = section_body[start:end].lower()
        if not any(marker in block for marker in RESOLVED_MARKERS):
            return True
    return False


def status_prefix_ok(prefix: str, status: str) -> bool:
    """True if status's wording matches one of the recognized phrases for this file's TODO/READY/MAYBE prefix."""
    normalized = status.strip().lower()
    if prefix == "MAYBE":
        return normalized.startswith(MAYBE_STATUS_PREFIX)
    return any(normalized.startswith(p) for p in TODO_READY_STATUS_PREFIXES)


def audit_file(path: Path) -> list:
    """Return a list of (reason_code, detail) tuples for this plan file. Empty list means no flags."""
    text = path.read_text(encoding="utf-8")
    prefix = file_prefix(path)
    flags = []

    status_match = STATUS_LINE_RE.search(text)
    complexity_match = COMPLEXITY_LINE_RE.search(text)

    if status_match is None:
        flags.append(("missing_status_header", "No 'Current status:' line found."))
    if complexity_match is None:
        flags.append(("missing_complexity_header", "No 'Complexity:' line found."))

    if status_match is not None:
        status = status_match.group(1).strip()
        first_heading = SECTION_HEADING_RE.search(text)
        if first_heading is not None and status_match.start() >= first_heading.start():
            flags.append(("status_header_misplaced", "'Current status:' appears at/after the first '##' section heading."))

        if not status_prefix_ok(prefix, status):
            flags.append(("unrecognized_status_phrase", f"Status text doesn't match the {prefix} wording rules: '{status}'"))

        normalized = status.lower()
        if prefix in ("TODO", "READY") and normalized.startswith("ready to execute"):
            oq_section = extract_section(text, "Open questions")
            if oq_section is not None and open_questions_are_unresolved(oq_section):
                flags.append(("ready_but_has_open_questions", "Status says 'Ready to execute' but '## Open questions' still has unresolved item(s)."))

        if prefix == "READY" and normalized.startswith("waiting for user input"):
            flags.append(("ready_prefix_waiting_status", "READY-prefixed file but status still says 'Waiting for user input'."))

        if prefix == "TODO" and normalized.startswith("ready to execute"):
            flags.append(("todo_prefix_marked_ready", "TODO-prefixed file already marked 'Ready to execute' -- possible missed rename to READY-*.md."))

        if prefix in ("TODO", "READY") and normalized.startswith("parked"):
            flags.append(("parked_status_wrong_prefix", "Status says 'Parked...' but filename is not MAYBE-prefixed."))

        if prefix == "MAYBE" and not normalized.startswith("parked"):
            flags.append(("maybe_prefix_non_parked", f"MAYBE-prefixed file but status doesn't start with 'Parked': '{status}'"))

    return flags


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Best-effort pre-filter for .work/todo/ Current status:/Complexity: header inconsistencies.",
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--dir", metavar="PATH", help="Directory to scan (default: .work/todo/ under the repo root).")
    parser.add_argument("--verbose", "-v", action="store_true", help="Also print files that passed every check.")
    args = parser.parse_args()

    todo_dir = Path(args.dir) if args.dir else DEFAULT_TODO_DIR
    files = find_plan_files(todo_dir)

    if not files:
        print(f"No TODO-/READY-/MAYBE-*.md files found under {todo_dir}.")
        return 0

    flagged_count = 0
    for path in files:
        flags = audit_file(path)
        if flags:
            flagged_count += 1
            print(f"{path.name}")
            for reason, detail in flags:
                print(f"  [{reason}] {detail}")
            print()
        elif args.verbose:
            print(f"{path.name}  OK")

    print(f"Scanned {len(files)} file(s); {flagged_count} flagged.")
    return 1 if flagged_count else 0


if __name__ == "__main__":
    sys.exit(main())
