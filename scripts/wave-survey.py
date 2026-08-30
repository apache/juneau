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
Cross-project wave-candidate survey for the three wave-scope trackers under ~/Project Work/todos/.

Replaces the by-hand "open all three TODO.md files and eyeball the READY/TODO/MAYBE/HOLD lists"
survey that precedes authoring a wave (see @todo-and-waves's "WAVE / clone assignment / harvest"
section) with one invocation that emits the same candidate table: id, title, lifecycle state,
HIPRI marker, runnable-now vs blocked (with why), and a compact status line, sorted HIPRI-and-
runnable-first.

Covers exactly the three WAVE-SCOPE trackers: juneau, juneau-release-manager (slug
"juneau-release-manager"), sandbox-support-console. IRS is deliberately NOT covered -- IRS
(TODO-Innnn, tracker slug "irs") is tracked and waved independently of the Juneau-scope trio;
see @todo-and-waves. See TRACKERS below to add/remove a tracker.

PLACEMENT (why this lives once in apache/juneau/scripts, unlike todo-next-id.py /
todo-status-audit.py): those two scripts are carried byte-for-byte in all three repos because
each COPY only ever scans its OWN repo's tracker -- the project id letter and tracker slug are
the only things that differ, so one script body serves every repo once those are read from the
script's own constants. This script's whole reason to exist is the opposite shape: ONE invocation
surveys ALL THREE trackers, which means it inherently needs to know about all three repos at
once (see TRACKERS below). Carrying identical copies in all three repos would either (a) have
each copy re-declare the same three-tracker table pointlessly, or (b) tempt each copy to instead
scan only its own tracker, which would just reinvent a worse todo-status-audit.py subset. Neither
is a real byte-for-byte carry candidate, so this lives once, here.

Mirrors (does not import -- see below) todo-status-audit.py's tracker-layout understanding:
trackers live at ~/Project Work/todos/<slug>/, item files use TODO-/READY-/MAYBE-/HOLD-<L>nnnn
[<child-letter>]-<slug>.md filenames (a bare numeric id, optionally one lowercase letter-or-digit
child suffix with NO separator before the slug's own hyphen), an "Open questions" section header
with unresolved numbered items marks a file NOT actually ready despite its filename, and a
"-HIPRI-" segment immediately after the id (before the slug, composing with every lifecycle
prefix) marks a high-priority item per @todo-and-waves's "High-priority (HIPRI) TODOs" section.
Unlike that script, the id LETTER here is captured from each filename rather than hardcoded --
one copy of this script has to recognize "J"/"R"/"C" across the three trackers it surveys in a
single run, where each copy of todo-status-audit.py only ever needs its own repo's letter.

This file deliberately DUPLICATES (in a minimal, single-purpose form) the small pieces of
parsing logic it needs from todo-status-audit.py -- the open-questions-unresolved check and the
status-line/decoration stripping -- rather than importing that module. todo-status-audit.py is a
hyphenated filename (not importable by name without importlib machinery) that is carried
byte-for-byte across three other repos and is not this script's to depend on; a real Python
import would also pull in that module's git/counts machinery this script has no use for.

Lifecycle states surveyed: TODO, READY, MAYBE, HOLD (NEED_INPUT-*.md is not currently populated
in any of the three trackers and is deliberately left out of this first cut -- see the module's
report/follow-up notes; add it to LIFECYCLE_PREFIXES if that changes). "Runnable now" is
deliberately narrow and matches the by-hand survey exactly: a READY-*.md file with no unresolved
"## Open questions" item. Everything else is "blocked", with a one-line reason:
    TODO   -- not yet promoted to READY (still needs design/decisions)
    MAYBE  -- parked / speculative, no trigger
    HOLD   -- committed work parked on indefinite hold
    READY  -- (only if it still has unresolved open questions) the promotion looks premature

Sort order: HIPRI items first (soft-strong priority, per @todo-and-waves -- this ordering is
informational only and never itself gates a wave), then runnable-now before blocked, then
lifecycle (READY, TODO, MAYBE, HOLD), then tracker (in TRACKERS order), then numeric id.

Usage:
    ./scripts/wave-survey.py
    ./scripts/wave-survey.py --json
    ./scripts/wave-survey.py --runnable-only
    ./scripts/wave-survey.py --repo juneau --repo sandbox-support-console
    ./scripts/wave-survey.py --root ~/Project Work

Options:
    --root <path>       Project Work root (default: ~/Project Work).
    --repo <slug>       Restrict the survey to one tracker slug (juneau / juneau-release-manager /
                             sandbox-support-console). Repeatable. Default: all three.
    --runnable-only     Print (or emit as JSON) only the RUNNABLE NOW items.
    --json              Emit a machine-readable JSON object instead of the human-readable table.
    --help, -h          Show this help message.

Exit status:
    0   Survey completed (regardless of how many items were found -- this is a report, not a
            pass/fail gate; an empty tracker is a legitimate, non-error result).
    2   At least one requested tracker directory does not exist. Mirrors todo-status-audit.py's
            missing-vs-empty rule: an unfound tracker must not be silently reported as an empty
            one, so the other tracker(s) are still surveyed and reported, but the exit code and a
            stderr line both say a tracker was skipped rather than legitimately empty.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

PROJECT_WORK = Path.home() / "Project Work"

# The three wave-scope trackers, in the order this script always reports them (also the tie-break
# order in sort_key() below). Add/remove a tracker here -- nowhere else -- to change scope. IRS
# ("irs" slug, TODO-Innnn) is deliberately absent; see the module docstring.
TRACKERS = (
    ("juneau", "Apache Juneau"),
    ("juneau-release-manager", "Apache Juneau Release Manager"),
    ("sandbox-support-console", "Sandbox Support Console"),
)
TRACKER_SLUGS = tuple(slug for slug, _label in TRACKERS)

# See the module docstring's "Lifecycle states surveyed" paragraph for why NEED_INPUT is absent.
LIFECYCLE_PREFIXES = ("READY", "TODO", "MAYBE", "HOLD")
LIFECYCLE_RANK = {prefix: rank for rank, prefix in enumerate(LIFECYCLE_PREFIXES)}

# <prefix>-<LETTER><digits>[<child-suffix>]-<slug>.md. The letter is CAPTURED (group 2), not
# hardcoded, because a single run of this script recognizes J/R/C across all three trackers --
# see the module docstring's "letter captured" paragraph. child-suffix has no separator before
# it (matches todo-next-id.py's own lettered-child shape, e.g. "TODO-C0016a"); anything after the
# next "-" is the slug, which is where an "-o06-"-shaped slug segment (not a child suffix) lands.
FILENAME_RE = re.compile(r"^(TODO|READY|MAYBE|HOLD)-([A-Z])(\d+)([a-z0-9]*)-(.+)\.md$")

# "-HIPRI-" immediately after the id (and any letter-child suffix), before the slug -- see
# @todo-and-waves's "High-priority (HIPRI) TODOs" section. A separate regex (not reusing
# FILENAME_RE's slug capture) for the same reason todo-status-audit.py's HIPRI_MARKER_RE is
# separate from its FILENAME_RE: this is a convenience classification, not a filename-shape gate.
HIPRI_RE = re.compile(r"^(?:TODO|READY|MAYBE|HOLD)-[A-Z]\d+[a-z0-9]*-HIPRI-.*\.md$")

# todo-next-id.py's id-reservation stub ("TODO-<L>nnnn-CLAIMED.md", never lettered -- see that
# script's docstring: "Claim stubs are only ever issued for a bare numeric id"). FILENAME_RE's
# permissive "-(.+)\.md$" slug group would otherwise happily match "CLAIMED" as an ordinary slug
# and surface an in-flight reservation as a bogus survey candidate with placeholder title/status
# text; scan_tracker() checks this first and skips a match instead.
CLAIM_STUB_RE = re.compile(r"^TODO-[A-Z]\d+-CLAIMED\.md$")

# The file's own H1 title line, e.g. "# TODO-J0447: Dogfood / chrome feedback (...)". Only the
# first "# " line in the file is read -- every item file in the corpus opens with exactly one.
HEADING_RE = re.compile(r"^#\s+(.*)$", re.MULTILINE)
# Strips a leading "<PREFIX>-<LETTER><digits>[<child>]: " from that heading, since the id is
# already its own column -- repeating it in the title column would waste table width.
HEADING_ID_PREFIX_RE = re.compile(r"^(?:TODO|READY|MAYBE|HOLD)-[A-Z]\d+[a-z0-9]*:\s*")

# Duplicated (not imported -- see module docstring) from todo-status-audit.py's STATUS_LINE_RE /
# STATUS_DECORATION_RE: the "Current status:" field and the markdown/punctuation decoration the
# corpus commonly wraps its value in (bold, a leading backtick, a leading "-"/em dash).
STATUS_LINE_RE = re.compile(r"^\s*Current status:(.*)$", re.IGNORECASE | re.MULTILINE)
STATUS_DECORATION_RE = re.compile(r"^[\s*_`\"'>-]+")

# Duplicated (not imported) from todo-status-audit.py's SECTION_HEADING_RE / NUMBERED_ITEM_RE /
# RESOLVED_MARKER_RE / extract_section() / open_questions_are_unresolved() -- the minimum needed
# to tell whether a file's own "## Open questions" section still has a live (non-answered)
# numbered item. See that script's module docstring for the fuller rationale; this is the same
# check, not a redesign of it.
SECTION_HEADING_RE = re.compile(r"^##\s(.*)$", re.MULTILINE)
NUMBERED_ITEM_RE = re.compile(r"^\s*\d+[.)]\s(.*)$", re.MULTILINE)
RESOLVED_MARKER_RE = re.compile(r"\b(?:resolved|answered|decided)\b")

# Table-column truncation widths. Full (untruncated) title/status text is always available via
# --json; these only bound the human-readable table so one very long file doesn't blow out every
# row's width.
TITLE_MAX_LEN = 80
STATUS_MAX_LEN = 130


def extract_section(text: str, heading_text: str) -> "str | None":
    """Body text of the first "## <heading_text>" section (case-insensitive substring match), or
    None if no such heading exists. See todo-status-audit.py's identically-named function."""
    headings = list(SECTION_HEADING_RE.finditer(text))
    for i, m in enumerate(headings):
        if heading_text.lower() in m.group(1).strip().lower():
            start = m.end()
            end = headings[i + 1].start() if i + 1 < len(headings) else len(text)
            return text[start:end]
    return None


def open_questions_are_unresolved(section_body: str) -> bool:
    """True if the "## Open questions" section has at least one numbered item that doesn't look
    answered/resolved. See todo-status-audit.py's identically-named function."""
    items = list(NUMBERED_ITEM_RE.finditer(section_body))
    if not items:
        return False
    for i, m in enumerate(items):
        start = m.start()
        end = items[i + 1].start() if i + 1 < len(items) else len(section_body)
        block = section_body[start:end].lower()
        if not RESOLVED_MARKER_RE.search(block):
            return True
    return False


def has_unresolved_open_questions(text: str) -> bool:
    """True if this file's "## Open questions" section (if any) still has a live item."""
    section = extract_section(text, "Open questions")
    return section is not None and open_questions_are_unresolved(section)


def truncate(text: str, max_len: int) -> str:
    """Collapse to one line (whitespace/newlines squashed) and truncate to max_len with an
    ellipsis. The status line is already single-line by construction (STATUS_LINE_RE's `.` does
    not match a newline), but titles occasionally wrap, so this is applied to both."""
    collapsed = " ".join(text.split())
    if len(collapsed) <= max_len:
        return collapsed
    return collapsed[: max_len - 1].rstrip() + "…"


def extract_title(text: str) -> str:
    """The file's H1 heading with a leading "<id>: " prefix stripped (the id is its own column)."""
    m = HEADING_RE.search(text)
    if m is None:
        return "(no title heading)"
    heading = m.group(1).strip()
    stripped = HEADING_ID_PREFIX_RE.sub("", heading, count=1).strip()
    return stripped or heading


def extract_status(text: str) -> str:
    """The "Current status:" field value, decoration-stripped, or a named placeholder if the file
    has none / an empty one. Named rather than blank so an absent header is never confused with a
    file that legitimately said nothing -- the same reason todo-status-audit.py's
    empty_status_value check exists, though this script only reports it, it does not flag it."""
    m = STATUS_LINE_RE.search(text)
    if m is None:
        return "(no 'Current status:' line)"
    value = STATUS_DECORATION_RE.sub("", m.group(1)).strip()
    return value or "(empty status value)"


def classify(lifecycle: str, unresolved_open_questions: bool) -> "tuple[bool, str]":
    """(runnable, block_reason) for one item. See the module docstring's "Lifecycle states
    surveyed" paragraph for the rule; block_reason is "" iff runnable is True."""
    if lifecycle == "READY":
        if unresolved_open_questions:
            return False, "open questions unresolved despite READY"
        return True, ""
    if lifecycle == "TODO":
        return False, "not yet promoted to READY"
    if lifecycle == "MAYBE":
        return False, "parked (MAYBE)"
    if lifecycle == "HOLD":
        return False, "on hold (HOLD)"
    raise ValueError(f"unrecognized lifecycle {lifecycle!r}")  # unreachable -- gated by FILENAME_RE's alternation


def parse_item(path: Path, repo_slug: str, repo_label: str) -> dict:
    """One survey row for a single tracker item file. Reads the file once."""
    text = path.read_text(encoding="utf-8")
    match = FILENAME_RE.match(path.name)
    prefix, letter, digits, child_suffix, _slug = match.groups()
    hipri = bool(HIPRI_RE.match(path.name))
    unresolved = has_unresolved_open_questions(text)
    runnable, block_reason = classify(prefix, unresolved)
    return {
        "repo": repo_slug,
        "repo_label": repo_label,
        "id": f"{prefix}-{letter}{digits}{child_suffix}",
        "numeric_id": int(digits),
        "child_suffix": child_suffix,
        "lifecycle": prefix,
        "hipri": hipri,
        "runnable": runnable,
        "block_reason": block_reason,
        "title": extract_title(text),
        "status": extract_status(text),
        "path": str(path),
    }


def scan_tracker(todo_dir: Path, repo_slug: str, repo_label: str) -> "list | None":
    """Every recognized item file directly under todo_dir, or None if todo_dir does not exist
    (the caller distinguishes that from a legitimately empty, present directory)."""
    if not todo_dir.is_dir():
        return None
    items = []
    for entry in sorted(todo_dir.glob("*.md")):
        if not entry.is_file():
            continue
        if CLAIM_STUB_RE.match(entry.name):
            continue
        if not FILENAME_RE.match(entry.name):
            continue
        items.append(parse_item(entry, repo_slug, repo_label))
    return items


def sort_key(item: dict) -> tuple:
    """Runnable-now first (the primary split -- see the module docstring's "clearly split" design
    goal), then HIPRI first within each of those two groups (soft-strong priority), then lifecycle
    (READY/TODO/MAYBE/HOLD), then tracker (TRACKERS order), then numeric id, then child suffix.
    Runnable outranks HIPRI here so the flat/JSON list order matches the human table's two
    sections exactly -- a HIPRI TODO is still surfaced ahead of every other blocked item, but
    never ahead of a runnable item, which would misread as "start this instead of the READY one"."""
    return (
        0 if item["runnable"] else 1,
        0 if item["hipri"] else 1,
        LIFECYCLE_RANK[item["lifecycle"]],
        TRACKER_SLUGS.index(item["repo"]),
        item["numeric_id"],
        item["child_suffix"],
    )


def collect(project_work: Path, repo_slugs: "tuple | None" = None) -> "tuple[list, list]":
    """(items, missing_dirs) across the requested tracker slugs (default: all of TRACKER_SLUGS),
    sorted per sort_key(). missing_dirs holds the todo_dir path (as a string) for every requested
    tracker whose directory does not exist -- see scan_tracker()'s missing-vs-empty distinction."""
    wanted = repo_slugs if repo_slugs else TRACKER_SLUGS
    items = []
    missing = []
    for slug, label in TRACKERS:
        if slug not in wanted:
            continue
        todo_dir = project_work / "todos" / slug
        found = scan_tracker(todo_dir, slug, label)
        if found is None:
            missing.append(str(todo_dir))
            continue
        items.extend(found)
    items.sort(key=sort_key)
    return items, missing


def _section_table(title: str, rows: list, *, show_reason: bool) -> str:
    """One "== <title> (N) ==" banner plus a padded, human-readable table of rows (or "(none)")."""
    out = [f"== {title} ({len(rows)}) =="]
    if not rows:
        out.append("  (none)")
        out.append("")
        return "\n".join(out)

    headers = ["HIPRI", "ID", "REPO", "STATE", "TITLE"]
    if show_reason:
        headers.append("REASON")
    headers.append("STATUS")

    table_rows = []
    for item in rows:
        row = [
            "HIPRI" if item["hipri"] else "",
            item["id"],
            item["repo"],
            item["lifecycle"],
            truncate(item["title"], TITLE_MAX_LEN),
        ]
        if show_reason:
            row.append(truncate(item["block_reason"], 45))
        row.append(truncate(item["status"], STATUS_MAX_LEN))
        table_rows.append(row)

    widths = [
        max(len(headers[col]), *(len(row[col]) for row in table_rows))
        for col in range(len(headers))
    ]

    def fmt_row(row):
        return "  ".join(cell.ljust(width) for cell, width in zip(row, widths))

    out.append(fmt_row(headers))
    out.append(fmt_row(["-" * w for w in widths]))
    out.extend(fmt_row(row) for row in table_rows)
    out.append("")
    return "\n".join(out)


def format_table(items: list, *, include_blocked: bool = True) -> str:
    """The full human-readable report: a RUNNABLE NOW section, optionally a BLOCKED / NOT READY
    section (see the module docstring's "clearly split" design goal), and a one-line summary."""
    if not items:
        return "(no candidate items found across the surveyed tracker(s))"

    runnable = [i for i in items if i["runnable"]]
    blocked = [i for i in items if not i["runnable"]]

    lines = [_section_table("RUNNABLE NOW (READY, no unresolved open questions)", runnable, show_reason=False)]
    if include_blocked:
        lines.append(_section_table("BLOCKED / NOT READY", blocked, show_reason=True))

    hipri_count = sum(1 for i in items if i["hipri"])
    repos_seen = len({i["repo"] for i in items})
    summary = f"{len(items)} item(s) across {repos_seen} tracker(s): {len(runnable)} runnable now"
    if include_blocked:
        summary += f", {len(blocked)} blocked"
    summary += f"; {hipri_count} HIPRI."
    lines.append(summary)
    return "\n".join(lines)


def to_json_row(item: dict) -> dict:
    """The JSON-emitted shape of one row -- drops fields that exist only to drive sort_key()."""
    return {k: v for k, v in item.items() if k not in ("numeric_id", "child_suffix")}


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Survey the wave-scope trackers (juneau, juneau-release-manager, "
        "sandbox-support-console -- NOT irs) and emit the cross-project wave-candidate table "
        "(see @todo-and-waves).",
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--root", metavar="PATH", help="Project Work root (default: ~/Project Work).")
    parser.add_argument(
        "--repo",
        metavar="SLUG",
        action="append",
        default=[],
        choices=TRACKER_SLUGS,
        help="Restrict the survey to one tracker slug. Repeatable. Default: all three.",
    )
    parser.add_argument("--runnable-only", action="store_true", help="Report only the RUNNABLE NOW items.")
    parser.add_argument("--json", action="store_true", help="Emit a machine-readable JSON object instead of a table.")
    args = parser.parse_args()

    project_work = Path(args.root).expanduser().resolve() if args.root else PROJECT_WORK
    repo_slugs = tuple(args.repo) if args.repo else None

    scanned = [project_work / "todos" / slug for slug in (repo_slugs or TRACKER_SLUGS)]
    print(f"[wave-survey] scanning {', '.join(str(p) for p in scanned)}", file=sys.stderr)

    items, missing = collect(project_work, repo_slugs)
    for missing_dir in missing:
        print(
            f"ERROR: {missing_dir} does not exist -- skipped (not scanned as empty; the other "
            f"tracker(s) are still reported below).",
            file=sys.stderr,
        )

    if args.runnable_only:
        items = [i for i in items if i["runnable"]]

    if args.json:
        print(json.dumps({"items": [to_json_row(i) for i in items], "missing_trackers": missing}, indent=2))
    else:
        print(format_table(items, include_blocked=not args.runnable_only))

    return 2 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
