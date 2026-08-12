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
Next-free TODO-id calculator for Apache Juneau's .work/todo/ tracker.

Mirrors the exact scan scope documented in agents/skills/juneau-todo-management/SKILL.md's
"Adding a new item" and "MAYBE Numbering" sections:

  1. Every "[TODO-n]" and bare "TODO-n" token in .work/todo/TODO.md (a trailing lowercase
     letter suffix, e.g. "TODO-174a", is stripped -- only the numeric part counts).
  2. Every "TODO-"/"READY-"/"MAYBE-"/"FINISHED-"/"CANCELLED-<n>[<letter>]-*.md" filename
     directly under .work/todo/ and .work/todo/finished/.

next = 1 + max(all numeric ids found). A child's letter suffix (TODO-174a, FINISHED-337f, ...)
is ignored for this computation -- only its numeric part counts, per the skill's "Numbering"
rule -- so promoting/splitting a lettered child never consumes a new sequential id.

.work/ is gitignored in apache/juneau, so this is pure filesystem/text scanning; no git needed.

Usage:
    ./scripts/todo-next-id.py
    ./scripts/todo-next-id.py --list
    ./scripts/todo-next-id.py --check 174a

Options:
    --list          Print every id currently in use (letter suffixes preserved), one per
                    line, sorted numerically then by letter, instead of the next free id.
    --check <id>    Exit 1 with a message if <id> (e.g. "347" or "174a"; a leading "TODO-"
                    is tolerated) is already in use; exit 0 with a message if it's free.
    --help, -h      Show this help message.

Examples:
    ./scripts/todo-next-id.py
    ./scripts/todo-next-id.py --list
    ./scripts/todo-next-id.py --check 347
    ./scripts/todo-next-id.py --check TODO-174a
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
TODO_DIR = REPO_ROOT / ".work" / "todo"
FINISHED_DIR = TODO_DIR / "finished"

# "[TODO-42]" or bare "TODO-42" in TODO.md prose. A trailing run of lowercase letters (the
# child-letter suffix, possibly more than one for grandchildren like "175fa") is captured
# separately so it can be preserved for --list/--check but ignored for numbering.
TODO_TOKEN_RE = re.compile(r"\bTODO-(\d+)([a-z]*)\b")

# Every lifecycle-state filename directly under .work/todo/ or .work/todo/finished/.
FILENAME_RE = re.compile(r"^(?:TODO|READY|MAYBE|FINISHED|CANCELLED)-(\d+)([a-z]*)-.*\.md$")


def collect_ids() -> tuple[set, set]:
    """
    Scan every source described in the module docstring.

    Returns (raw_ids, numeric_ids):
      - raw_ids:     every distinct id token as it actually appears (e.g. "174", "174a",
                      "312f"), for --list / --check.
      - numeric_ids: just the base integer part of each id (letter suffix stripped), for
                      computing the next free id.
    """
    raw_ids = set()
    numeric_ids = set()

    todo_md = TODO_DIR / "TODO.md"
    if todo_md.is_file():
        text = todo_md.read_text(encoding="utf-8")
        for m in TODO_TOKEN_RE.finditer(text):
            raw_ids.add(m.group(1) + m.group(2))
            numeric_ids.add(int(m.group(1)))

    for directory in (TODO_DIR, FINISHED_DIR):
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
        description="Compute the next free .work/todo/ TODO id (see agents/skills/juneau-todo-management/SKILL.md).",
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--list", action="store_true", help="Print every id currently in use, sorted.")
    parser.add_argument("--check", metavar="ID", help="Exit non-zero if ID is already taken.")
    args = parser.parse_args()

    raw_ids, numeric_ids = collect_ids()

    if args.check is not None:
        normalized = normalize_check_id(args.check)
        if normalized is None:
            print(f"ERROR: '{args.check}' is not a valid id (expected e.g. '347' or '174a').", file=sys.stderr)
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
