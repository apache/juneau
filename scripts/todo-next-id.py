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
Next-free TODO-id calculator for this project's tracker under ~/Project Work/todos/juneau-release-manager/.

Trackers live in ~/Project Work (todos/<slug>/ + finished/<slug>/), not in the repo.
Only REPO_LABEL / SKILL_NAME / ID_PROJECT_LETTER / TRACKER_SLUG differ between copies.
CUs read ~/Project Work; do not copy .work/todo into clones.

**Ids are per-project and start at 1 in each.** This script scans ~/Project Work/todos/<slug>
and ~/Project Work/finished/<slug>. It prints those paths to stderr on every run.

Mirrors the exact scan scope documented in this repo's TODO-management skill, in its
"Adding a new item" and "MAYBE Numbering" sections:

  1. Every "[TODO-n]" and bare "TODO-n" token in ~/Project Work/todos/juneau-release-manager/TODO.md (a trailing lowercase
     letter suffix, e.g. "TODO-174a", is stripped -- only the numeric part counts). A
     qualified cross-repo citation such as "otherproject:TODO-42" is NOT counted; it names
     an id in another repo's tracker.

     Note that this scan cannot distinguish an illustrative id from a live one: writing
     "for example, TODO-5" anywhere in TODO.md permanently consumes id 5. Write "TODO-<n>" in
     prose.
  2. Every "TODO-"/"READY-"/"MAYBE-"/"HOLD-"/"FINISHED-"/"CANCELLED-"/"NEED_INPUT-<n>[<letter>]-*.md"
     filename directly under ~/Project Work/todos/juneau-release-manager/ and ~/Project Work/finished/juneau-release-manager/.

next = 1 + max(all numeric ids found), but that candidate is not simply returned: it is
ATOMICALLY CLAIMED first. Every id-granting call creates "TODO-<id>-CLAIMED.md" under the
live tracker with an exclusive (O_CREAT | O_EXCL) file create before printing anything, so
two sessions racing on the same tracker can never receive the same id -- one of them loses
the create and silently retries the next candidate instead. See claim_next_id() below. A
child's letter suffix (TODO-174a, FINISHED-337f, ...) is ignored for this computation --
only its numeric part counts, per the skill's "Numbering" rule -- so promoting/splitting a
lettered child never consumes a new sequential id, and never needs a claim of its own.

The claim file IS the reservation, not a courtesy: it satisfies FILENAME_RE, so it is
counted as "taken" by every future scan (this process's and everyone else's) exactly like a
real plan file. The caller is expected to overwrite it in place -- write the real Goal /
Steps / Notes into that same file (any tool, any number of edits) -- and then run
`--finalize <id> <slug>` to rename it to add a slug
("TODO-<id>-CLAIMED.md" -> "TODO-<id>-<short-slug>.md") as ONE atomic operation performed by
this script (os.rename), not by the caller. A caller that crashes before finalizing leaves
the stub sitting in the tracker, loudly labelled "CLAIMED (placeholder, NOT a real plan)"
with the claiming pid/host/timestamp -- diagnosable, not a plausible-looking empty TODO.
`--list-claims` finds every such stub and its age; nothing deletes them automatically, and
the retired number is never reused even if the stub is later removed.

`--finalize` supersedes the older "rename it yourself" convention, which is no longer
documented as an option: a caller that renames the stub by hand, or -- the failure this
closes -- writes the real plan under a brand-new filename and abandons the stub in place,
produces exactly the bug this exists to prevent: a stub and a real plan coexisting under
the same id. This script cannot intercept an arbitrary file write by the caller, so it
cannot refuse that outright; todo-status-audit.py's `unfinalized_claim_stub` check is the
backstop that catches it immediately (unconditionally of age) if a caller bypasses
`--finalize` anyway. `--finalize` fails loudly rather than guessing if the id is malformed
or lettered (claim stubs are only ever issued for a bare numeric id), if no stub exists for
it, or if the final path already exists -- see do_finalize() below.

Trackers are outside the git repo (~/Project Work); this is a pure filesystem scan.

A MISSING ~/Project Work/todos/juneau-release-manager/ directory is a hard error (exit 2), not an empty scan. Silently
returning "1" from a tree that has no tracker is how ids get reused: it is the correct answer
in a freshly-seeded repo and a catastrophic one in a repo whose tracker you failed to find.
Pass --allow-missing if you genuinely want the empty-tree answer (this also creates the
directory so the claim file below has somewhere to land).

Usage:
    ./scripts/todo-next-id.py
    ./scripts/todo-next-id.py --list
    ./scripts/todo-next-id.py --list-claims
    ./scripts/todo-next-id.py --check 12
    ./scripts/todo-next-id.py --finalize 12 short-slug
    ./scripts/todo-next-id.py --root ~/Project Work

Options:
    --root <path>   Project Work root (default: ~/Project Work).
    --list          Print every id currently in use (letter suffixes preserved), one per
                    line, sorted numerically then by letter, instead of the next free id.
    --list-claims   Print every "<id>-CLAIMED.md" placeholder stub and its age, for auditing
                    sessions that claimed an id and never wrote (or renamed) the real plan.
                    Diagnostic only; never deletes anything.
    --check <id>    Exit 1 with a message if <id> (e.g. "12" or "7a"; a leading "TODO-"
                    is tolerated) is already in use; exit 0 with a message if it's free.
    --finalize <id> <slug>
                    Atomically rename that id's claim stub ("TODO-<id>-CLAIMED.md") to its
                    final plan filename ("TODO-<id>-<slug>.md") via a single os.rename() --
                    the ONE supported way to complete a claim. The caller must have already
                    written the real plan into the stub file in place before calling this.
                    Fails loudly (never falls back to a permissive outcome) if <id> is
                    malformed or lettered, if no stub exists for it, or if the final path
                    already exists. See do_finalize() and the module docstring above.
    --allow-missing Treat an absent ~/Project Work/todos/juneau-release-manager/ as empty instead of an error.
    --help, -h      Show this help message.

Exit status:
    0   Success (or --check found the id free, or --list-claims ran, or --finalize renamed
        the stub into place).
    1   --check found the id already taken, or a malformed --check argument.
    2   ~/Project Work/todos/juneau-release-manager/ does not exist (and --allow-missing was not given).
    3   Could not claim an id after MAX_CLAIM_ATTEMPTS collisions (the tracker directory is
        likely unwritable -- this is not normal contention).
    4   --finalize could not complete -- malformed/lettered id, malformed slug, no claim
        stub found for that id, or the final path already exists. The printed message says
        which; see do_finalize()'s docstring for why each of these fails loudly instead of
        guessing.
"""

from __future__ import annotations

import argparse
import os
import re
import socket
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

# ---------------------------------------------------------------------------------------
# The ONLY repo-specific values in this file. Everything below is identical across every
# copy of this script; keep it that way so a fix lands once and is copied verbatim.
# ---------------------------------------------------------------------------------------
REPO_LABEL = "Juneau Release Manager"
SKILL_NAME = "todo-and-waves"
# Project letter in the id: Juneau "J", Console "C", Release App "R", IRS "I" -- this
# copy's project uses "R". Numeric part is always 4 digits. This scanner matches only
# TODO-R<digits>; ids from the other three projects are not counted here.
ID_PROJECT_LETTER = "R"
TRACKER_SLUG = "juneau-release-manager"

PROJECT_WORK = Path.home() / "Project Work"

# "[TODO-R0042]" or bare "TODO-R0042" in TODO.md prose. Digits follow the project letter R.
# A trailing child-letter suffix (TODO-R0174a) is captured separately so it can be preserved
# for --list/--check but ignored for numbering.
#
# The leading lookbehind rejects a qualified cross-repo citation -- e.g.
# "otherproject:TODO-R0042" -- naming an id in ANOTHER repo's tracker, which must not
# consume one here.
TODO_TOKEN_RE = re.compile(r"(?<![\w:])TODO-R(\d+)([a-z0-9]*)\b")

# Every lifecycle-state filename under ~/Project Work/todos/juneau-release-manager/ or finished/juneau-release-manager/.
FILENAME_RE = re.compile(
    r"^(?:TODO|READY|MAYBE|HOLD|FINISHED|CANCELLED|NEED_INPUT)-R(\d+)([a-z0-9]*)-.*\.md$"
)

# Migration to TODO-Rnnnn is complete. Do not scan unprefixed TODO-<digits>.
LEGACY_TODO_TOKEN_RE = None
LEGACY_FILENAME_RE = None

# ---------------------------------------------------------------------------------------
# Atomic id claiming. The old code computed "next = 1 + max(existing ids)" from a scan
# and simply returned it -- a read, not a reservation. Two sessions racing between their
# scan and their write get handed the SAME number (this is exactly how TODO-C0021 and
# TODO-C0022 were each handed out twice on 2026-08-25). Fix: before returning a candidate,
# atomically create a placeholder claim file for it with os.O_CREAT | os.O_EXCL, which the
# OS guarantees only one caller can ever succeed at, even against another process racing
# on the same directory at the same instant. Losing that race just means someone else got
# there first, so bump the candidate and try again -- bounded, so a systemic problem
# (read-only tracker, exhausted disk, ...) fails loudly instead of spinning forever.
#
# The claim file's name IS the reservation: "TODO-<id>-CLAIMED.md" matches FILENAME_RE, so
# it is picked up by every subsequent scan (this run's and everyone else's) exactly like a
# real plan file, permanently retiring that number until someone deletes the stub.
# ---------------------------------------------------------------------------------------
CLAIM_SUFFIX = "-CLAIMED.md"
MAX_CLAIM_ATTEMPTS = 500  # ids only ever increase; this many collisions in a row means
                          # something else is wrong, not real contention.

# A short-slug for --finalize: lowercase letters/digits, single hyphens between words, no
# leading/trailing hyphen. Deliberately stricter than the "*" in FILENAME_RE's "-.*\.md" --
# this is the one place a HUMAN OR AGENT is asked to type a slug by hand, so it is validated
# before it is trusted to become a filename, rather than accepted verbatim.
SLUG_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")


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
    """Format a stored raw id as letter + 4-digit number + child suffix (e.g. R0447, R0445o)."""
    m = re.match(r"^(\d+)([a-z0-9]*)$", raw_id)
    if not m:
        return f"{ID_PROJECT_LETTER}{raw_id}" if ID_PROJECT_LETTER else raw_id
    return f"{ID_PROJECT_LETTER}{int(m.group(1)):04d}{m.group(2)}"


def normalize_check_id(raw: str) -> str | None:
    """Normalize a --check argument to a bare "<digits><letters>" id, or None if malformed.

    Accepts `12`, `7a`, `TODO-12`, and (for prefixed projects) `R12` / `TODO-R12`.
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


def claim_placeholder_text(formatted_id: str) -> str:
    """
    Body written into a freshly claimed "<id>-CLAIMED.md" file.

    Deliberately loud and un-plan-shaped: a crashed session must leave something a human
    (or the next agent) immediately recognizes as an abandoned reservation, not a
    plausible-looking empty TODO that gets mistaken for real, unstarted work.
    """
    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    return (
        f"# {formatted_id} -- CLAIMED (placeholder, NOT a real plan)\n"
        "\n"
        f"This id was atomically reserved by {Path(sys.argv[0]).name} so no other session\n"
        "could be handed the same number. No plan has been written yet.\n"
        "\n"
        f"Claimed at:  {now}\n"
        f"Claimed by:  pid {os.getpid()} on {socket.gethostname()}\n"
        f"Command:     {' '.join(sys.argv)}\n"
        "\n"
        "If you are that session: overwrite this file in place with the real plan (Goal /\n"
        "Steps / Notes per the todo-and-waves skill), then run\n"
        f"`{Path(sys.argv[0]).name} --finalize {formatted_id} <short-slug>` to rename it into place --\n"
        "do NOT rename it yourself, and do NOT write the real plan under a brand-new filename\n"
        "while leaving this stub behind: either produces a stub and a real plan coexisting under\n"
        "the same id, which the tracker audit now flags immediately (unfinalized_claim_stub),\n"
        "regardless of age.\n"
        "\n"
        "If you are not that session and this file is stale (the claiming session crashed\n"
        "or never followed up): the id stays retired regardless -- never hand out\n"
        f"{formatted_id} again. Confirm nothing references it, then delete this stub so the\n"
        "tracker stops showing a phantom entry; the next call to this script will move past\n"
        "it on its own. `--list-claims` lists every stub of this kind with its age.\n"
    )


def claim_next_id(todo_dir: Path, numeric_ids: set) -> int:
    """
    Atomically claim and return the next free numeric id.

    Starts at 1 + max(existing ids) from the scan -- the same formula the old code used --
    but then proves the claim by creating "TODO-<id>-CLAIMED.md" under todo_dir with
    O_CREAT | O_EXCL. That create either succeeds (we own the id; no one else ever will)
    or raises FileExistsError (someone else claimed it first), in which case we bump the
    candidate and retry. See the module docstring for what happens to the stub afterward.
    """
    candidate = 1 + max(numeric_ids, default=0)
    for _ in range(MAX_CLAIM_ATTEMPTS):
        formatted = format_id(str(candidate))
        claim_path = todo_dir / f"TODO-{formatted}{CLAIM_SUFFIX}"
        try:
            fd = os.open(claim_path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o644)
        except FileExistsError:
            candidate += 1
            continue
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as handle:
                handle.write(claim_placeholder_text(formatted))
        except BaseException:
            claim_path.unlink(missing_ok=True)
            raise
        return candidate
    raise RuntimeError(
        f"could not claim an id after {MAX_CLAIM_ATTEMPTS} consecutive collisions "
        f"starting at {format_id(str(candidate))} -- check that {todo_dir} is writable."
    )


def do_finalize(todo_dir: Path, raw_id: str, raw_slug: str) -> int:
    """
    Perform the stub -> real-plan transition as a single atomic operation (os.rename), so no
    calling convention -- a manual rename, or (the bug this closes) writing the real plan
    under a brand-new filename and abandoning the stub -- can produce a window where both
    names exist on disk. Returns a process exit code (0 on success).

    Takes a final PATH (id + slug), not final CONTENT: the caller is expected to have already
    composed the real plan by editing the claim stub's own file in place (any tool, any number
    of edits) before calling this. Funnelling an entire plan's markdown through a CLI argument
    would be unwieldy and buys no extra safety -- the risky step was never "writing content"
    (safe under a name only the claimant can rename), it was "which filename ends up holding
    the final state", which this makes the one atomic step the script itself owns.

    Fails loudly on each of the following -- an ambiguous or unreadable state must not resolve
    to the permissive outcome -- rather than guessing or silently overwriting:
      - raw_id is malformed, or names a lettered child (claim stubs are only ever issued for a
        bare numeric id -- todo-next-id.py never claims on behalf of a lettered child; see
        claim_next_id()'s docstring). Either way no such stub could ever exist.
      - raw_slug is not a valid slug per SLUG_RE.
      - no claim stub exists at the computed path (never claimed, already finalized, or
        removed by someone else).
      - the final path already exists (refuses to overwrite it; the caller must resolve the
        collision by hand -- see the printed message for the two ways that can legitimately
        happen).
    """
    normalized = normalize_check_id(raw_id)
    if normalized is None:
        example = f"{ID_PROJECT_LETTER}0012" if ID_PROJECT_LETTER else "0001"
        print(f"ERROR: '{raw_id}' is not a valid id (expected e.g. '{example}' or '7a').", file=sys.stderr)
        return 4

    id_match = re.match(r"^(\d+)([a-z0-9]*)$", normalized)
    digits, suffix = id_match.group(1), id_match.group(2)
    if suffix:
        print(
            f"ERROR: '{raw_id}' names a lettered child (suffix '{suffix}'). Claim stubs are only "
            f"ever issued for a bare numeric id -- todo-next-id.py never claims on behalf of a "
            f"lettered child (see claim_next_id()'s docstring) -- so no such stub can exist. "
            f"Finalize the parent id instead.",
            file=sys.stderr,
        )
        return 4

    if not SLUG_RE.match(raw_slug):
        print(
            f"ERROR: '{raw_slug}' is not a valid slug (expected lowercase letters, digits, and "
            f"single hyphens between words, e.g. 'short-slug').",
            file=sys.stderr,
        )
        return 4

    formatted = format_id(digits)
    stub_path = todo_dir / f"TODO-{formatted}{CLAIM_SUFFIX}"
    final_path = todo_dir / f"TODO-{formatted}-{raw_slug}.md"

    if not stub_path.is_file():
        print(
            f"ERROR: no claim stub at {stub_path} -- nothing to finalize. Either {formatted} was "
            f"never claimed, was already finalized, or the stub was removed by someone else. Check "
            f"{todo_dir} (or run --list-claims) before re-claiming.",
            file=sys.stderr,
        )
        return 4

    if final_path.exists():
        print(
            f"ERROR: {final_path} already exists -- refusing to overwrite it. If {formatted} was "
            f"already finalized under this slug, there is nothing left to do here, but the stub at "
            f"{stub_path} is now an orphan the tracker audit will flag (unfinalized_claim_stub) -- "
            f"delete it once you've confirmed the existing file is the complete, correct plan. "
            f"Otherwise, {final_path} belongs to something else; pick a different slug.",
            file=sys.stderr,
        )
        return 4

    os.rename(stub_path, final_path)
    print(str(final_path))
    return 0


def main():
    parser = argparse.ArgumentParser(
        description=f"Compute the next free TODO id for {REPO_LABEL} under ~/Project Work (see @{SKILL_NAME}).",
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--root", metavar="PATH", help="Project Work root (default: ~/Project Work).")
    parser.add_argument("--list", action="store_true", help="Print every id currently in use, sorted.")
    parser.add_argument(
        "--list-claims",
        action="store_true",
        help="List '<id>-CLAIMED.md' placeholder stubs (claimed but never turned into a "
             "real plan), with their age. Diagnostic only -- never deletes anything.",
    )
    parser.add_argument("--check", metavar="ID", help="Exit non-zero if ID is already taken.")
    parser.add_argument(
        "--finalize",
        nargs=2,
        metavar=("ID", "SLUG"),
        help="Atomically rename ID's claim stub to 'TODO-<ID>-<SLUG>.md' (see do_finalize()). "
             "The real plan must already be written into the stub file in place.",
    )
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

    if args.finalize is not None:
        return do_finalize(todo_dir, args.finalize[0], args.finalize[1])

    if args.list_claims:
        claim_re = re.compile(rf"^TODO-{re.escape(ID_PROJECT_LETTER)}(\d+){re.escape(CLAIM_SUFFIX)}$")
        stubs = []
        if todo_dir.is_dir():
            stubs = sorted(
                (entry for entry in todo_dir.iterdir() if entry.is_file() and claim_re.match(entry.name)),
                key=lambda entry: entry.name,
            )
        if not stubs:
            print("No claim stubs found.")
            return 0
        for entry in stubs:
            age_minutes = (time.time() - entry.stat().st_mtime) / 60
            age = f"{age_minutes:.0f}m" if age_minutes < 60 else f"{age_minutes / 60:.1f}h"
            print(f"{entry.name}\t(age: {age})\t{entry}")
        return 0

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

    if not todo_dir.is_dir():
        # Only reachable via --allow-missing (the hard-error branch above already
        # returned otherwise). Create it now so the claim below has somewhere to land.
        todo_dir.mkdir(parents=True, exist_ok=True)

    try:
        next_id = claim_next_id(todo_dir, numeric_ids)
    except RuntimeError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 3
    print(format_id(str(next_id)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
