#!/usr/bin/env python3
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
Next-free TODO-id calculator for this repository's .work/todo/ tracker.

Repo-agnostic: the repository root is derived from this file's own location
(<root>/scripts/todo-next-id.py -> <root>), never hardcoded, so the same body works in
every repository that adopts the convention. Only the REPO_LABEL / SKILL_NAME constants
below and the license header differ between copies.

**Ids are per-repository and start at 1 in each.** This script only ever scans the tree it
lives in. It prints the resolved root to stderr on every run precisely so that a run made
from the wrong working tree is visible rather than silent.

Mirrors the exact scan scope documented in this repo's TODO-management skill, in its
"Adding a new item" and "MAYBE Numbering" sections:

  1. Every "[TODO-n]" and bare "TODO-n" token in .work/todo/TODO.md (a trailing lowercase
     letter suffix, e.g. "TODO-174a", is stripped -- only the numeric part counts). A
     qualified cross-repo citation such as "juneau:TODO-42" is NOT counted; it names an id in
     another repo's tracker.

     Note that this scan cannot distinguish an illustrative id from a live one: writing
     "for example, TODO-5" anywhere in TODO.md permanently consumes id 5. Write "TODO-<n>" in
     prose.
  2. Every "TODO-"/"READY-"/"MAYBE-"/"FINISHED-"/"CANCELLED-<n>[<letter>]-*.md" filename
     directly under .work/todo/ and .work/todo/finished/.

next = 1 + max(all numeric ids found). A child's letter suffix (TODO-174a, FINISHED-337f, ...)
is ignored for this computation -- only its numeric part counts, per the skill's "Numbering"
rule -- so promoting/splitting a lettered child never consumes a new sequential id.

.work/ is gitignored, so this is pure filesystem/text scanning; no git needed.

A MISSING .work/todo/ directory is a hard error (exit 2), not an empty scan. Silently
returning "1" from a tree that has no tracker is how ids get reused: it is the correct answer
in a freshly-seeded repo and a catastrophic one in a repo whose tracker you failed to find.
Pass --allow-missing if you genuinely want the empty-tree answer.

Usage:
    ./scripts/todo-next-id.py
    ./scripts/todo-next-id.py --list
    ./scripts/todo-next-id.py --check 12
    ./scripts/todo-next-id.py --root /path/to/other/repo

Options:
    --root <path>   Repository root to scan (default: the parent of this script's directory).
    --list          Print every id currently in use (letter suffixes preserved), one per
                    line, sorted numerically then by letter, instead of the next free id.
    --check <id>    Exit 1 with a message if <id> (e.g. "12" or "7a"; a leading "TODO-"
                    is tolerated) is already in use; exit 0 with a message if it's free.
    --allow-missing Treat an absent .work/todo/ as an empty tracker instead of an error.
    --help, -h      Show this help message.

Exit status:
    0   Success (or --check found the id free).
    1   --check found the id already taken, or a malformed --check argument.
    2   .work/todo/ does not exist under the resolved root (and --allow-missing was not given).
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
REPO_LABEL = "Juneau Release Manager"
SKILL_NAME = "release-manager-todo-management"

DEFAULT_REPO_ROOT = Path(__file__).resolve().parent.parent

# "[TODO-42]" or bare "TODO-42" in TODO.md prose. A trailing run of lowercase letters (the
# child-letter suffix, possibly more than one for grandchildren like "17fa") is captured
# separately so it can be preserved for --list/--check but ignored for numbering.
#
# The leading lookbehind rejects a qualified cross-repo citation -- "juneau:TODO-42",
# "support-console:TODO-42" -- which names an id in ANOTHER repo's tracker and must not
# consume one here. Ids are bare and per-repository, so without this the act of writing down
# that another repo's item blocks you would silently burn a local id.
TODO_TOKEN_RE = re.compile(r"(?<![\w:])TODO-(\d+)([a-z]*)\b")

# Every lifecycle-state filename directly under .work/todo/ or .work/todo/finished/.
FILENAME_RE = re.compile(r"^(?:TODO|READY|MAYBE|FINISHED|CANCELLED)-(\d+)([a-z]*)-.*\.md$")


def collect_ids(todo_dir: Path) -> tuple[set, set]:
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

    todo_md = todo_dir / "TODO.md"
    if todo_md.is_file():
        text = todo_md.read_text(encoding="utf-8")
        for m in TODO_TOKEN_RE.finditer(text):
            raw_ids.add(m.group(1) + m.group(2))
            numeric_ids.add(int(m.group(1)))

    for directory in (todo_dir, todo_dir / "finished"):
        if not directory.is_dir():
            continue
        for entry in directory.iterdir():
            if not entry.is_file():
                continue
            m = FILENAME_RE.match(entry.name)
            if not m:
                continue
            raw_ids.add(m.group(1) + m.group(2))
            numeric_ids.add(int(m.group(1)))

    return raw_ids, numeric_ids


def sort_key(raw_id: str):
    """Sort key for a raw id string: numeric part first, then its letter suffix."""
    m = re.match(r"^(\d+)([a-z]*)$", raw_id)
    if not m:
        return (0, raw_id)
    return (int(m.group(1)), m.group(2))


def normalize_check_id(raw: str) -> str | None:
    """Normalize a --check argument (optionally "TODO-"-prefixed) to a bare "<digits><letters>" id, or None if malformed."""
    candidate = raw.strip()
    if candidate.upper().startswith("TODO-"):
        candidate = candidate[len("TODO-"):]
    m = re.match(r"^(\d+)([a-zA-Z]*)$", candidate)
    if not m:
        return None
    return m.group(1) + m.group(2).lower()


def main():
    parser = argparse.ArgumentParser(
        description=f"Compute the next free .work/todo/ TODO id for {REPO_LABEL} (see @{SKILL_NAME}).",
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--root", metavar="PATH", help="Repository root to scan (default: parent of this script's directory).")
    parser.add_argument("--list", action="store_true", help="Print every id currently in use, sorted.")
    parser.add_argument("--check", metavar="ID", help="Exit non-zero if ID is already taken.")
    parser.add_argument("--allow-missing", action="store_true", help="Treat an absent .work/todo/ as empty instead of an error.")
    args = parser.parse_args()

    repo_root = Path(args.root).resolve() if args.root else DEFAULT_REPO_ROOT
    todo_dir = repo_root / ".work" / "todo"

    # Always announce the tree actually scanned. Ids are per-repository and bare, so
    # "TODO-5" is a different item in each repo; a wrong-tree run must not look identical
    # to a right-tree one. stderr, so `NEXT=$(./scripts/todo-next-id.py)` still works.
    print(f"[{REPO_LABEL}] scanning {todo_dir}", file=sys.stderr)

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

    raw_ids, numeric_ids = collect_ids(todo_dir)

    if args.check is not None:
        normalized = normalize_check_id(args.check)
        if normalized is None:
            print(f"ERROR: '{args.check}' is not a valid id (expected e.g. '12' or '7a').", file=sys.stderr)
            return 1
        if normalized in raw_ids:
            print(f"TAKEN: {normalized} is already in use.")
            return 1
        print(f"FREE: {normalized} is not currently in use.")
        return 0

    if args.list:
        for raw_id in sorted(raw_ids, key=sort_key):
            print(raw_id)
        return 0

    next_id = 1 + max(numeric_ids, default=0)
    print(next_id)
    return 0


if __name__ == "__main__":
    sys.exit(main())
