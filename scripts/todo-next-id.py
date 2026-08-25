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
Next-free TODO-id calculator for this project's tracker under ~/Project Work/todos/juneau/.

Trackers live in ~/Project Work (todos/<slug>/ + finished/<slug>/), not in the repo.
Only REPO_LABEL / SKILL_NAME / ID_PROJECT_LETTER / TRACKER_SLUG differ between copies.
CUs read ~/Project Work; do not copy .work/todo into clones.

**Ids are per-project and start at 1 in each.** This script scans ~/Project Work/todos/<slug>
and ~/Project Work/finished/<slug>. It prints those paths to stderr on every run.

Mirrors the exact scan scope documented in this repo's TODO-management skill, in its
"Adding a new item" and "MAYBE Numbering" sections:

  1. Every "[TODO-n]" and bare "TODO-n" token in ~/Project Work/todos/juneau/TODO.md (a trailing lowercase
     letter suffix, e.g. "TODO-174a", is stripped -- only the numeric part counts). A
     qualified cross-repo citation such as "juneau:TODO-42" is NOT counted; it names an id in
     another repo's tracker.

     Note that this scan cannot distinguish an illustrative id from a live one: writing
     "for example, TODO-5" anywhere in TODO.md permanently consumes id 5. Write "TODO-<n>" in
     prose.
  2. Every "TODO-"/"READY-"/"MAYBE-"/"HOLD-"/"FINISHED-"/"CANCELLED-<n>[<letter>]-*.md" filename
     directly under ~/Project Work/todos/juneau/ and ~/Project Work/finished/juneau/.

next = 1 + max(all numeric ids found). A child's letter suffix (TODO-174a, FINISHED-337f, ...)
is ignored for this computation -- only its numeric part counts, per the skill's "Numbering"
rule -- so promoting/splitting a lettered child never consumes a new sequential id.

Trackers are outside the git repo (~/Project Work); this is a pure filesystem scan.

A MISSING ~/Project Work/todos/juneau/ directory is a hard error (exit 2), not an empty scan. Silently
returning "1" from a tree that has no tracker is how ids get reused: it is the correct answer
in a freshly-seeded repo and a catastrophic one in a repo whose tracker you failed to find.
Pass --allow-missing if you genuinely want the empty-tree answer.

Usage:
    ./scripts/todo-next-id.py
    ./scripts/todo-next-id.py --list
    ./scripts/todo-next-id.py --check 12
    ./scripts/todo-next-id.py --root ~/Project Work

Options:
    --root <path>   Project Work root (default: ~/Project Work).
    --list          Print every id currently in use (letter suffixes preserved), one per
                    line, sorted numerically then by letter, instead of the next free id.
    --check <id>    Exit 1 with a message if <id> (e.g. "12" or "7a"; a leading "TODO-"
                    is tolerated) is already in use; exit 0 with a message if it's free.
    --allow-missing Treat an absent ~/Project Work/todos/juneau/ as empty instead of an error.
    --help, -h      Show this help message.

Exit status:
    0   Success (or --check found the id free).
    1   --check found the id already taken, or a malformed --check argument.
    2   ~/Project Work/todos/juneau/ does not exist (and --allow-missing was not given).
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

# ---------------------------------------------------------------------------------------
# The ONLY repo-specific values in this file. Everything below is identical across every
# copy of this script; keep it that way so a fix lands once and is copied verbatim.
# ---------------------------------------------------------------------------------------
REPO_LABEL = "Apache Juneau"
SKILL_NAME = "todo-and-waves"
# Project letter in the id: Juneau = "J" (TODO-Jnnnn). Other projects: "C" Console,
# "R" Release App, "I" IRS. Numeric part is always 4 digits. This scanner matches only
# TODO-J<digits>, so TODO-C0015 / TODO-R0001 / TODO-I0001 are not counted here.
ID_PROJECT_LETTER = "J"
TRACKER_SLUG = "juneau"

PROJECT_WORK = Path.home() / "Project Work"

# "[TODO-J0042]" or bare "TODO-J0042" in TODO.md prose. Digits follow the project letter J.
# A trailing child-letter suffix (TODO-J0174a) is captured separately so it can be preserved
# for --list/--check but ignored for numbering.
#
# The leading lookbehind rejects a qualified cross-repo citation -- "juneau:TODO-J0042",
# "support-console:TODO-C0015" -- which names an id in ANOTHER repo's tracker and must not
# consume one here.
TODO_TOKEN_RE = re.compile(r"(?<![\w:])TODO-J(\d+)([a-z0-9]*)\b")

# Every lifecycle-state filename under ~/Project Work/todos/juneau/ or finished/juneau/.
FILENAME_RE = re.compile(
    r"^(?:TODO|READY|MAYBE|HOLD|FINISHED|CANCELLED|NEED_INPUT)-J(\d+)([a-z0-9]*)-.*\.md$"
)

# Migration to TODO-Jnnnn is complete. Do not scan unprefixed TODO-<digits>.
LEGACY_TODO_TOKEN_RE = None
LEGACY_FILENAME_RE = None


def collect_ids(todo_dir: Path, finished_dir: Path | None = None) -> tuple[set, set]:
    """
    Scan every source described in the module docstring.

    Returns (raw_ids, numeric_ids):
      - raw_ids:     every distinct id token as it actually appears (e.g. "17", "17a",
                      "12f"), for --list / --check.
      - numeric_ids: just the base integer part of each id (letter suffix stripped), for
                      computing the next free id.
    """
    raw_ids = set()
    numeric_ids = set()

    def _record(regex, source, *, filename=False):
        if regex is None:
            return
        matches = (regex.match(source),) if filename else regex.finditer(source)
        for m in matches:
            if not m:
                continue
            raw_ids.add(str(int(m.group(1))) + m.group(2))
            numeric_ids.add(int(m.group(1)))

    todo_md = todo_dir / "TODO.md"
    if todo_md.is_file():
        text = todo_md.read_text(encoding="utf-8")
        _record(TODO_TOKEN_RE, text)
        _record(LEGACY_TODO_TOKEN_RE, text)

    dirs = [todo_dir, finished_dir if finished_dir is not None else todo_dir / "finished"]
    for directory in dirs:
        if not directory.is_dir():
            continue
        for entry in directory.iterdir():
            if not entry.is_file():
                continue
            _record(FILENAME_RE, entry.name, filename=True)
            _record(LEGACY_FILENAME_RE, entry.name, filename=True)

    return raw_ids, numeric_ids


def sort_key(raw_id: str):
    """Sort key for a raw id string: numeric part first, then its letter suffix."""
    m = re.match(r"^(\d+)([a-z0-9]*)$", raw_id)
    if not m:
        return (0, raw_id)
    return (int(m.group(1)), m.group(2))


def format_id(raw_id: str) -> str:
    """Format a stored raw id as letter + 4-digit number + child suffix (e.g. J0447, J0445o)."""
    m = re.match(r"^(\d+)([a-z0-9]*)$", raw_id)
    if not m:
        return f"{ID_PROJECT_LETTER}{raw_id}" if ID_PROJECT_LETTER else raw_id
    return f"{ID_PROJECT_LETTER}{int(m.group(1)):04d}{m.group(2)}"


def normalize_check_id(raw: str) -> str | None:
    """Normalize a --check argument to a bare "<digits><letters>" id, or None if malformed.

    Accepts `12`, `7a`, `TODO-12`, and (for prefixed projects) `C12` / `TODO-C12`.
    """
    candidate = raw.strip()
    if candidate.upper().startswith("TODO-"):
        candidate = candidate[len("TODO-"):]
    if (
        ID_PROJECT_LETTER
        and len(candidate) >= 2
        and candidate[0].upper() == ID_PROJECT_LETTER
        and candidate[1].isdigit()
    ):
        candidate = candidate[1:]
    m = re.match(r"^(\d+)([a-z0-9]*)$", candidate, re.I)
    if not m:
        return None
    return str(int(m.group(1))) + m.group(2).lower()


def main():
    parser = argparse.ArgumentParser(
        description=f"Compute the next free TODO id for {REPO_LABEL} under ~/Project Work (see @{SKILL_NAME}).",
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--root", metavar="PATH", help="Project Work root (default: ~/Project Work).")
    parser.add_argument("--list", action="store_true", help="Print every id currently in use, sorted.")
    parser.add_argument("--check", metavar="ID", help="Exit non-zero if ID is already taken.")
    parser.add_argument("--allow-missing", action="store_true", help="Treat an absent tracker as empty instead of an error.")
    args = parser.parse_args()

    project_work = Path(args.root).expanduser().resolve() if args.root else PROJECT_WORK
    todo_dir = project_work / "todos" / TRACKER_SLUG
    finished_dir = project_work / "finished" / TRACKER_SLUG

    # Always announce the tree actually scanned. stderr, so NEXT=$(./scripts/todo-next-id.py) still works.
    print(f"[{REPO_LABEL}] scanning {todo_dir} + {finished_dir}", file=sys.stderr)

    if not todo_dir.is_dir():
        if not args.allow_missing:
            print(
                f"ERROR: {todo_dir} does not exist.\n"
                f"       Ids are per-repository, so an unfound tracker must not be reported as an empty\n"
                f"       one -- that silently hands out id 1 and reuses live ids. Check you are in the\n"
                f"       right working tree, or pass --allow-missing if this repo genuinely has no tracker yet.",
                file=sys.stderr,
            )
            return 2
        print(f"WARNING: {todo_dir} does not exist; treating as empty (--allow-missing).", file=sys.stderr)

    raw_ids, numeric_ids = collect_ids(todo_dir, finished_dir)

    if args.check is not None:
        normalized = normalize_check_id(args.check)
        if normalized is None:
            example = f"{ID_PROJECT_LETTER}0012" if ID_PROJECT_LETTER else "0001"
            print(f"ERROR: '{args.check}' is not a valid id (expected e.g. '{example}' or '7a').", file=sys.stderr)
            return 1
        if normalized in raw_ids:
            print(f"TAKEN: {format_id(normalized)} is already in use.")
            return 1
        print(f"FREE: {format_id(normalized)} is not currently in use.")
        return 0

    if args.list:
        for raw_id in sorted(raw_ids, key=sort_key):
            print(format_id(raw_id))
        return 0

    next_id = 1 + max(numeric_ids, default=0)
    print(format_id(str(next_id)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
