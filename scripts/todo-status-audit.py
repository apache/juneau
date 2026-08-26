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
# The docstring is a raw string so the declared-counts example below can show a real regex (with its
# backslash escapes) verbatim, instead of a de-escaped one that would not work if copied out.
r"""
Best-effort status/header consistency pre-filter for this project's plan files under ~/Project Work/todos/juneau-release-manager/.

Repo-agnostic: the repository root is derived from this file's own location
(<root>/scripts/todo-status-audit.py -> <root>), never hardcoded, so the same body works in
every repository that adopts the convention. Only the REPO_LABEL / SKILL_NAME constants
below and the license header differ between copies.

Checks every TODO-/READY-/MAYBE-/HOLD-/NEED_INPUT-*.md file directly under
~/Project Work/todos/juneau-release-manager/ (FINISHED-/CANCELLED-*.md archives are explicitly
out of scope -- per this repo's TODO-management skill, "status line is not required in FINISHED
archives")
against that skill's "Per-file `Current status:` and `Complexity:` header" rules, and flags
candidate inconsistencies. This is a PRE-FILTER, not a validator: it flags candidates for a
human (or agent) to look at, and will not catch everything on format-drifted files -- tolerant,
best-effort markdown-header parsing throughout.

Checks performed (each file may accumulate multiple flags):
  - missing_status_header       No "Current status:" line found anywhere in the file.
  - missing_complexity_header   No "Complexity:" line found anywhere in the file.
  - status_header_misplaced     "Current status:" appears at or after the first "## " section
                                 heading (it must come before it, per the skill's "Placement" rule).
  - empty_status_value          The "Current status:" field is present but has no real content once
                                 markdown/punctuation decoration is stripped (e.g. the header line was
                                 never filled in). Deliberately permissive about WHAT the value says --
                                 the trackers have settled on free-form descriptive prose ("Done.",
                                 "Blocked on the Juneau pin bump.", "**COMPLETE (2026-07-16).**", etc.)
                                 rather than a fixed phrase set, so only a structurally-absent value is
                                 flagged here. (This replaces a former unrecognized_status_phrase check
                                 that enumerated a fixed set of exact phrases and consequently flagged
                                 the majority of files once the trackers settled on descriptive prose
                                 instead of that enumerated set -- an enumeration is exactly what rots.)
  - ready_but_has_open_questions
                                 The file declares itself ready -- either by its READY-*.md filename
                                 prefix or by a status starting with "Ready to execute" -- but still
                                 has a "## Open questions" section containing at least one numbered
                                 item whose text doesn't look answered/resolved. The filename prefix
                                 counts on its own because it, not the status prose, is the board's
                                 structural declaration that nothing is left open; keying the check
                                 to the phrase alone let a READY- file hide unresolved questions
                                 behind different opening words.
  - ready_but_wave_already_accepted
                                 Status starts with "Ready to execute" and the file's header region
                                 (everything before the first "## " heading) declares membership in a
                                 WAVE-nnnn that the wave board records as ALREADY ACCEPTED and whose
                                 WAVE file is gone. The item is claiming to be queued behind a wave
                                 that has already been accepted and pushed, which cannot both be
                                 true: either the item shipped in that wave and was never archived
                                 (the "shipped but not archived" drift this check exists for), or it
                                 was dropped from the wave and its header reference is stale. Both
                                 readings need an operator edit, so the flag is actionable either way.
                                 See "Wave-acceptance cross-check" below for why this is a
                                 cross-record consistency check and not a guess at what shipped.
  - ready_prefix_waiting_status A READY-*.md file whose status still says "Waiting for user input"
                                 (READY files should have no unresolved open questions left).
  - todo_prefix_marked_ready    A TODO-*.md file (not yet renamed) whose status already says "Ready
                                 to execute" -- a likely-missed rename to READY-*.md (see the skill's
                                 "OQA lifecycle -> status transitions").
  - parked_status_wrong_prefix  A TODO-*.md/READY-*.md file whose status starts with "Parked" (that
                                 wording is reserved for MAYBE-*.md files).
  - maybe_prefix_non_parked     A MAYBE-*.md file whose status does NOT start with "Parked".
  - on_hold_status_wrong_prefix A TODO-*.md/READY-*.md file whose status starts with "On hold" (that
                                 wording is reserved for HOLD-*.md files).
  - hold_prefix_non_on_hold     A HOLD-*.md file whose status does NOT start with "On hold".
  - counts_malformed            A "Counts:" line exists but its value is neither the literal "None." nor
                                 the declared-counts grammar `<repo>:<kind>:<pathspec>[:<regex>]` = <int>.
                                 Kept distinct from an absent line for the same reason the Key-symbols
                                 tri-state is: "the author wrote nothing" and "the author wrote something
                                 that did not parse" are different facts.
  - counts_mismatch             A declared count written with '=' does not equal the count re-derived
                                 from source THIS RUN. Often routine re-baselining: an '=' declaration is
                                 usually a BASELINE, and the item's own work is what changed the number.
  - counts_below_floor          A declared count written with '>=' re-derived BELOW its floor. Kept
                                 distinct from counts_mismatch because the operator response differs: a
                                 floor breach is a regression to investigate and never a number to
                                 correct, where an equality mismatch frequently is exactly that.
  - counts_scope_empty          A declared count's pathspec resolves to ZERO files, so the declaration is
                                 no longer anchored to anything and its number cannot be tested. Flagged
                                 loudly rather than passing vacuously -- this is the specific defence
                                 against a declaration that outlives the code it described.
  - counts_unverifiable         A declared count names a repo that cannot be resolved to a git worktree
                                 with a readable revision (missing checkout, no git, unreadable HEAD).
  - stale_claim                 An id-reservation stub ("TODO-<id>-CLAIMED.md", written by
                                 todo-next-id.py's claim_next_id() to atomically reserve an id) is
                                 older than STALE_CLAIM_AGE_HOURS with no real plan ever written in
                                 its place -- the claiming session most likely crashed. See "Claim-
                                 stub handling" below for what the stub records and why this check
                                 is age-based, not liveness-based. Claim stubs are otherwise EXEMPT
                                 from every other check above -- see that section for why.
  - unfinalized_claim_stub      A "TODO-<id>-CLAIMED.md" reservation stub (see stale_claim just
                                 above) coexists with a real (non-stub) plan file under the SAME
                                 numeric id. Deliberately NOT age-gated, unlike stale_claim: an id
                                 must never simultaneously name a live reservation and a finished
                                 plan, so this is wrong the instant it exists, no matter how fresh
                                 the stub is. See "Claim-stub handling" below for how this happens
                                 and how to resolve it.

NEED_INPUT-*.md files (blocked on operator answers, per the skill's prefix table) are scanned like
TODO-/READY- files for the structural checks above, but are deliberately not subject to the
Parked-/On-hold-prefix-mismatch checks, or to ready_but_wave_already_accepted, which are all
TODO/READY-specific signals.

Claim-stub handling
--------------------
todo-next-id.py's claim_next_id() reserves an id by creating "TODO-<id>-CLAIMED.md" BEFORE any
plan content exists (see that module's docstring and claim_placeholder_text()). The stub is
deliberately un-plan-shaped -- headed "CLAIMED (placeholder, NOT a real plan)", with no "Current
status:" or "Complexity:" line -- so a human immediately recognizes an abandoned reservation
instead of mistaking it for a plausible-looking empty TODO. That filename satisfies FILENAME_RE by
design: the whole point of the claim is that it counts as "taken" by every future scan, this
process's and everyone else's, exactly like a real plan file.

Treating a claim stub as an ordinary plan file was itself the bug this handling exists to fix: it
can never carry a "Current status:" or "Complexity:" line, so EVERY successful id reservation
flagged missing_status_header and missing_complexity_header for as long as the reservation
lasted -- which is by design, not a defect, for however long the caller has not yet written the
real plan. A claim stub (recognized separately via CLAIM_STUB_RE) is therefore exempt from every
plan-file check above and instead audited by audit_claim_stub() for two things: whether it
coexists with a real plan under the same id (unfinalized_claim_stub, unconditional -- see below),
and whether the reservation looks abandoned by age (stale_claim).

unfinalized_claim_stub fires the instant a real (non-stub) file is found sharing the stub's exact
numeric id -- group_claims_by_numeric_id() below builds that grouping once per run from the same
file list find_plan_files() already returned, so this costs one extra pass over filenames already
in hand, not a second directory scan. This is what tonight's actual incident looked like on disk:
todo-next-id.py's finalize step was, at the time, only a documented convention ("overwrite this
file in place ... then rename it"), not code -- an agent read that instruction, reconsidered
mid-task, and wrote the real plan under a fresh slugged filename instead of renaming the stub,
leaving both on disk. todo-next-id.py's `--finalize` (see that module) now performs the rename as
one atomic operation the SCRIPT owns, closing that window; this check is the backstop for any
caller that still bypasses it -- deliberately not age-gated, because the coexistence is wrong
immediately, not just once it has sat around long enough to look abandoned like a lone stub does.

What the stub records (claim_placeholder_text() in todo-next-id.py): a "Claimed at:" ISO-8601 UTC
timestamp, and a "Claimed by: pid <n> on <hostname>" line. The pid and hostname are NOT used to
decide staleness here, despite being logged for exactly that purpose in the stub's own "if this
file is stale" paragraph: the recorded pid belongs to the one-shot todo-next-id.py CLI invocation
itself, which prints the id and exits within a fraction of a second of writing the stub -- in the
SUCCESSFUL case every bit as much as in the crashed one. Checking whether that pid is still
running would therefore report "dead" for essentially every claim, live or not, moments after it
was made: a guaranteed false positive, not a hardened signal. A recycled pid or a stub written on
another host (the failure modes liveness checks are usually distrusted for) would only make such a
check less reliable on top of that; the structural problem above is sufficient on its own to rule
it out. Age -- the stub's file mtime, the same signal todo-next-id.py's own --list-claims already
reports -- is therefore the only input to staleness.

STALE_CLAIM_AGE_HOURS is chosen so a normal, in-progress reservation never trips it: per
todo-next-id.py's own docstring the caller claims the id and writes the real plan into that same
file in one sitting ("overwrite it in place ... and then run `--finalize`"), and nothing in this
tracker's corpus -- including its largest plan files, drafted in a single session -- shows that
sitting outlasting a working day. The threshold is comfortably longer than any such session,
including one paused for a lunch break, while still catching a stub that has survived past the
session that claimed it.

Wave-acceptance cross-check (ready_but_wave_already_accepted)
------------------------------------------------------------
This is the only check that reads anything outside the scanned file, and it deliberately reads
the OTHER tracker record rather than the source tree. It exists because a whole wave of items
shipped to master and stayed in the active tracker for a day, silently, still saying "Ready to
execute ... do not farm until the operator starts the WAVE queue" -- and the stale claim then
propagated into downstream planning, which built serialization constraints against items that
had already shipped.

It is a CROSS-RECORD CONSISTENCY check, structurally the same as the prefix-vs-status checks
above: two records that the convention requires to agree (the wave board's accepted-wave log,
and the item's own "I am queued behind wave N" claim) are compared, and a contradiction is
flagged. It does NOT try to infer from prose which code an item was supposed to produce and
then look for that code in the tree. That was evaluated and rejected -- see the notes at
closed_out_waves() and header_region() -- because deliverables live in free prose with no
machine-readable field, and an item that MODIFIES an existing symbol names a symbol that
already resolves, which makes symbol-existence probing a false-positive generator.

Three independent conditions must all hold, so the check is inert wherever any of them is
absent (no board file, no wave reference, or a status that isn't a pre-kickoff claim):
  1. A wave board (WAVE.md, resolved as a sibling of todos/, or --board) records the wave as
     accepted, AND no live waves/WAVE-nnnn*.md file remains. Per the board's own documented
     lifecycle the WAVE file is deleted at accept, so requiring BOTH a prose "accepted" mention
     and the file's absence keeps a merely-discussed wave from reading as accepted.
  2. The item names that WAVE-nnnn in its header region (before the first "## " heading), i.e.
     as part of its own identity, not as a cross-reference buried in a body section. Items that
     merely mention another wave's members ("must be serialized against ...") reference the wave
     deep in the body and are not flagged.
  3. The item's status still starts with "Ready to execute" (the same narrow, already-documented
     READY_TO_EXECUTE_PREFIX phrase) -- i.e. it still claims not to have run. Unlike
     ready_but_has_open_questions, this check is NOT widened to the READY- filename prefix: what it
     needs is the item's own live claim that it has not yet run, and the filename outlives that
     claim (a READY- file whose status has moved on to "Done." has run).

Declared counts (counts_* checks)
--------------------------------
The second check family that reads outside the scanned file, and the only one that reads source.
It exists because numeric claims in plan files rot invisibly: a count written once ("ten call
sites", "23 archives", "146 `em` declarations") is prose from the moment it is typed, and a reader
has no way to tell a still-true number from a stale one without counting the thing again. Seven
separate counts were re-derived across these trackers on 2026-08-25 and every one came back
different from what the tracker said. None was found by reading; each was found by counting again.

This check does NOT infer what to count. That was the failure mode of the rejected
deliverable-probing approach (see closed_out_waves() and header_region()): prose does not say
what it is asserting, so a tool guessing at it manufactures false positives. Instead the ITEM
DECLARES what to count and what it expects, exactly the way `Key symbols:` has the item declare
its new symbols, and the audit does the counting. A declaration is a `Counts:` header line:

    Counts:  `console:files:src/main/java/**/rest/**/*.java:requiredConfirmation\(|armPhrase\(` = 10

read as "<repo token>:<kind>:<pathspec>[:<regex>] <op> <expected integer>", where <op> is `=` or
`>=`. Equality is the default and is the right shape for a BASELINE -- a fact about the state before
the item's work, which the item exists to change. `>=` declares a FLOOR ("this must never drop below
N'"), which is what an INVARIANT actually means. The floor form exists because an equality test over a
growable population is actively harmful: benign growth flags, and the operator is then invited to
"correct" the number, which is how a regression guard gets deleted by accident. The field may repeat,
one declaration per line; the literal value `None.` declares "nothing here is mechanically countable".
Kinds: `matches` (total regex occurrences), `files` (files with at least one occurrence), `paths`
(files matching the pathspec, no regex). Repo tokens: the three tracker repos plus `board` for
~/Project Work itself. Full grammar and rationale: the "Declared counts" section of @todo-and-waves.

Three properties keep a STALE declaration from passing, which is the risk the convention had to be
designed against -- a declaration nobody re-checks is no better than the sentence it replaced:

  1. Every number is re-derived from source on every run. The declared integer is only ever the
     EXPECTED side of a comparison; it is never read as an answer, never cached to disk, and no
     derived result is written back anywhere. There is no state for a previous run to leave behind.
     (Within a single run, blob reads and path listings for one immutable revision are memoized --
     that is deduplication of identical reads of the same commit, not a cached verdict.)
  2. A pathspec that resolves to zero files is counts_scope_empty, NOT a pass. This is the case
     that would otherwise rot silently: a file renamed or folded away leaves a declaration whose
     pattern trivially matches nothing, and comparing 0 to 0 would report success. One of the seven
     real failures (a claim about N per-letter FINISHED archives, after those archives had been
     folded into one file and deleted) is exactly this shape. Because a zero-file scope is always
     loud, a `paths ... = 0` declaration is deliberately NOT expressible -- "this glob matches
     nothing" and "this glob has rotted" are indistinguishable, so the convention refuses the claim
     instead of guessing which one it is.
  3. The grammar is strict, so a half-edited declaration becomes counts_malformed rather than being
     skipped. A line that no longer parses is reported, never quietly ignored.

DELIBERATE NON-GOAL: a floor set far below reality is NOT detected. `>= 1` over a population of 500
never fails, and nothing here reports that. Detecting it would need a threshold on how much slack is
legitimate, and any such threshold recreates the exact failure the floor form was added to remove --
it would flag populations that had merely grown, and invite raising the floor to match reality, which
is re-baselining a guard. The one unambiguous case IS refused: `>= 0` is rejected at parse time,
because it holds for every possible derivation and so asserts nothing. Past that, a floor's slack is
a judgment the author makes and the audit does not second-guess it. What still protects a floor is
property 2 above: a pathspec that has rotted to zero matches is counts_scope_empty -- never a pass,
and never the misreading "well, 0 is below the floor, so it fails as a regression".

Counts resolve against COMMITTED CONTENT (`HEAD`) in the named repo, never the working tree. Two
reasons, one of them paid for the hard way: a plan file citing working-tree-only files (three .py
files present locally and in nobody's HEAD) produced a blocked item on 2026-08-25 that two
adversarial reviewers both read past; and these repos are cloned per-WAVE, so a tree-resolved count
means the same declaration verifies differently in juneau-3 than in juneau-7 -- which is a coin
flip, not a re-derivation. HEAD is the one referent the operator, a subagent and a reviewer share.
The `board` token is the single documented exception: ~/Project Work is not a git repository, so
its counts necessarily resolve against the filesystem. That is sound rather than a concession --
the board is never cloned, so the per-clone divergence that makes tree-resolution wrong for a repo
cannot arise, and there is no HEAD to resolve against in the first place.

Absent is silence. No `Counts:` line means no claim, and nothing is flagged -- the field is
optional by design (see the skill), and a check that fired on absence would flag every
pre-convention file, which is precisely the cry-wolf failure this script was rewritten to remove.

A MISSING scan directory is a hard error (exit 2). An EMPTY-but-present one is a clean pass
(exit 0). The original version conflated the two and returned 0 for both, so pointing the
script at a tree with no tracker produced a reassuring "nothing to flag" -- the same silent-zero
trap as running `rg` over the gitignored .work/ without --no-ignore.

Usage:
    ./scripts/todo-status-audit.py
    ./scripts/todo-status-audit.py --verbose
    ./scripts/todo-status-audit.py --root ~/Project Work
    ./scripts/todo-status-audit.py --dir /path/to/alternate/todo/dir
    ./scripts/todo-status-audit.py --board /path/to/WAVE.md
    ./scripts/todo-status-audit.py --repo-root juneau=/path/to/juneau
    ./scripts/todo-status-audit.py --no-counts

Options:
    --root <path>   Project Work root (default: ~/Project Work). Ignored if --dir is given.
    --dir <path>    Exact directory to scan, overriding --root. Non-recursive -- only *.md
                    files directly in this directory are considered.
    --board <path>  Wave board file for the ready_but_wave_already_accepted check. Defaults to
                    WAVE.md beside the scanned tracker's parent (i.e. <project-work>/WAVE.md).
                    If the board cannot be found, that one check is skipped and every other
                    check still runs.
    --repo-root <token>=<path>
                    Override where a `Counts:` repo token resolves to. Repeatable. Without it a
                    token naming THIS repo resolves to this script's own repo root (a filesystem
                    fact, not a guess) and any other token to its default $HOME-relative path.
    --no-counts     Do not re-derive declared counts. Grammar errors are still reported --
                    counts_malformed needs no source access -- but counts_mismatch /
                    counts_scope_empty / counts_unverifiable are not evaluated. For running
                    without the checkouts (or without git) present; NOT a way to silence a
                    failing count.
    --verbose, -v   Also print files that passed every check (default: only print flagged files).
    --help, -h      Show this help message.

Exit status:
    0   No inconsistencies flagged (including the legitimately-empty-tracker case).
    1   At least one file was flagged.
    2   The scan directory does not exist.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

# ---------------------------------------------------------------------------------------
# The ONLY repo-specific values in this file: REPO_LABEL / TRACKER_SLUG / FILENAME_RE (which
# embeds this project's id letter) and CLAIM_STUB_RE (the same letter, one more place it
# appears). Everything below is identical across every copy of this script; keep it that way
# so a fix lands once and is copied verbatim.
# ---------------------------------------------------------------------------------------
REPO_LABEL = "Juneau Release Manager"
SKILL_NAME = "todo-and-waves"
TRACKER_SLUG = "juneau-release-manager"

PROJECT_WORK = Path.home() / "Project Work"

FILENAME_RE = re.compile(r"^(TODO|READY|MAYBE|HOLD|NEED_INPUT)-R\d+[a-z0-9]*-.*\.md$")

# An id-reservation stub written by todo-next-id.py's claim_next_id() -- see the module
# docstring's "Claim-stub handling" section. Always a bare numeric TODO- id (todo-next-id.py
# never claims on behalf of a lettered child) with no slug -- "-CLAIMED.md" is the whole
# filename tail, not something a claimant appends -- so this matches a strict subset of
# FILENAME_RE's TODO- branch. A claim stub is still found by find_plan_files() (the
# reservation must keep counting as "taken"), but is routed to audit_claim_stub() below
# instead of audit_file(): it is a distinct file kind, not a malformed plan file.
CLAIM_STUB_RE = re.compile(r"^TODO-R\d+-CLAIMED\.md$")

# The leading numeric id shared by a claim stub and any real (non-stub) plan filename for the
# same numbered item -- letter suffix and slug excluded. Used only to group files by id for the
# unfinalized_claim_stub check below (see group_claims_by_numeric_id()); never for numbering,
# which is todo-next-id.py's job, not this script's.
ID_FROM_FILENAME_RE = re.compile(r"^(?:TODO|READY|MAYBE|HOLD|NEED_INPUT)-R(\d+)")

# Each pattern's trailing "whatever follows" group used to be preceded by its own separate `\s*`/`\s+`
# quantifier (e.g. `\s*(.*)$`), which is superlinear-backtracking-prone: `\s` and `.` overlap (both match a
# space/tab), so on a non-matching line the engine can retry the split point between the two quantifiers
# across every position before giving up. Every caller already `.strip()`s the captured group (or, for
# NUMBERED_ITEM_RE, never inspects its content at all -- only the match's start position matters), so the
# extra quantifier was redundant: dropping it (STATUS_LINE_RE/COMPLEXITY_LINE_RE) or collapsing it to a single
# mandatory character (SECTION_HEADING_RE/NUMBERED_ITEM_RE) removes the overlap without changing what matches.
STATUS_LINE_RE = re.compile(r"^\s*Current status:(.*)$", re.IGNORECASE | re.MULTILINE)
COMPLEXITY_LINE_RE = re.compile(r"^\s*Complexity:(.*)$", re.IGNORECASE | re.MULTILINE)
SECTION_HEADING_RE = re.compile(r"^##\s(.*)$", re.MULTILINE)
NUMBERED_ITEM_RE = re.compile(r"^\s*\d+[.)]\s(.*)$", re.MULTILINE)

# Matched on word boundaries. A bare substring test reads "unanswered" / "unresolved" -- the
# most natural wording for an OPEN question -- as containing "answered" / "resolved", which
# silently disables the ready_but_has_open_questions check for exactly the case it exists to
# catch. Still best-effort: an explicit negation like "not resolved" reads as resolved.
RESOLVED_MARKER_RE = re.compile(r"\b(?:resolved|answered|decided)\b")

# The corpus commonly wraps the start of a status value in markdown emphasis/quoting (bold
# "**Done.**", a leading backtick, a leading "-"/em dash before "Parked" or "On hold"). Stripping
# that decoration before a phrase-prefix comparison is a generic, content-agnostic normalization --
# it encodes which PUNCTUATION is decoration, not which WORDS are expected -- so it doesn't
# reintroduce the kind of phrase enumeration that rotted the old unrecognized_status_phrase check.
STATUS_DECORATION_RE = re.compile(r"^[\s*_`\"'>-]+")

# Recognized status-prefix phrases (case-insensitive, checked with str.startswith after
# normalize_status_text()) for the specific lifecycle-mismatch signals below -- narrow, targeted
# substrings tied 1:1 to a documented filename-prefix convention, not an exhaustive enumeration of
# every valid status wording (that enumeration is exactly what status_value_is_empty() replaces).
READY_TO_EXECUTE_PREFIX = "ready to execute"
MAYBE_STATUS_PREFIX = "parked"
HOLD_STATUS_PREFIX = "on hold"

# --- Wave-acceptance cross-check inputs (see the module docstring) ----------------------------
# The board file and the live-WAVE-file directory, both resolved relative to the Project Work
# root rather than hardcoded, so the check is inert (not wrong) when pointed at a scratch tree.
BOARD_FILENAME = "WAVE.md"
WAVES_DIRNAME = "waves"

# A wave id is a fixed-width literal token, which is the whole reason this check can be exact:
# there is no prose to interpret on either side of the comparison, just the same token appearing
# in two records that the convention requires to agree.
WAVE_ID_RE = re.compile(r"WAVE-\d{4}")

# "accepted" is matched on a word boundary on the SAME board line as the wave id. Deliberately
# not a phrase enumeration: the board's accepted-wave log is free prose ("was accepted and pushed
# 2026-08-25", "WAVE-0002 ... was accepted and pushed"), so pinning an exact sentence would rot
# the way the removed unrecognized_status_phrase check did. The negation guard below plus the
# live-WAVE-file check in closed_out_waves() are what keep a loose word match from over-firing.
BOARD_ACCEPTED_RE = re.compile(r"\baccepted\b", re.IGNORECASE)

# A board line saying a wave is NOT accepted must not read as accepted. RESOLVED_MARKER_RE above
# documents the same hazard and accepts it; here the cost of being wrong is flagging a
# CORRECTLY-queued item, i.e. exactly the cry-wolf failure this script was rewritten to remove,
# so the common negations are matched explicitly instead.
BOARD_NOT_ACCEPTED_RE = re.compile(r"\b(?:not|never|un-?|isn't|aren't|wasn't|weren't)\s*accepted\b", re.IGNORECASE)

# --- Claim-stub inputs (see the module docstring's "Claim-stub handling" section) --------------
# Tolerant parses of the two fields claim_placeholder_text() (todo-next-id.py) writes, used only
# to name the claimant/timestamp in a flag's detail message -- NOT to decide staleness; an
# unparseable or absent field here still leaves age (file mtime) as the deciding signal.
CLAIM_CLAIMED_AT_RE = re.compile(r"^Claimed at:\s*(\S+)", re.MULTILINE)
CLAIM_CLAIMED_BY_RE = re.compile(r"^Claimed by:\s*pid\s+(\d+)\s+on\s+(\S+)", re.MULTILINE)

# How old a claim stub must be, by file mtime, before it is flagged stale rather than treated as
# a normal in-progress reservation. See "Claim-stub handling" in the module docstring for why
# this is age alone (not the pid/hostname the stub also records) and why 24h specifically.
STALE_CLAIM_AGE_HOURS = 24

# --- Declared-counts inputs (see the module docstring) ----------------------------------------
# Parsed with the same tolerant "^\s*<Field>:" shape as the other header fields, and deliberately
# NOT restricted to the header region: a count declaration is a checkable assertion wherever it is
# written, and the check's own strictness (the descriptor must parse, the pathspec must resolve)
# is what keeps it from over-firing, so there is no need to also guess at placement.
COUNTS_LINE_RE = re.compile(r"^\s*Counts:(.*)$", re.IGNORECASE | re.MULTILINE)

# The literal empty-declaration value, spelled exactly as `Key symbols:` spells its own -- one
# convention, one way to say "nothing", so a reader never has to learn a second spelling.
COUNTS_NONE_VALUE = "none."

# `<descriptor>` <op> <integer>, and nothing else on the line. Strict on purpose: a trailing human
# label or a second claim on the same line would have to be tolerated by ignoring text, and
# ignoring text is how a half-edited declaration passes. Anything unparseable is counts_malformed.
# '>=' is listed before '=' in the alternation so the two-character operator is never read as a
# bare '=' with a stray '>' in front of it.
COUNT_CLAIM_RE = re.compile(r"^`([^`]+)`\s*(>=|=)\s*(\d+)$")

# matches = total regex occurrences; files = files with at least one occurrence; paths = files
# matching the pathspec (no regex). These three are not a guess at what items might want to count:
# they are the three shapes the real mis-counted claims actually took -- occurrence counts ("ten
# call sites", "146 `em` declarations", "six tracker references"), distinct-file counts ("nine REST
# classes"), and file-set sizes ("23 archives").
COUNT_KIND_MATCHES = "matches"
COUNT_KIND_FILES = "files"
COUNT_KIND_PATHS = "paths"
COUNT_KINDS = (COUNT_KIND_MATCHES, COUNT_KIND_FILES, COUNT_KIND_PATHS)

# Committed content, always. See the module docstring's "Counts resolve against COMMITTED CONTENT"
# paragraph for why the working tree is not offered as an option rather than merely not defaulted.
COUNTS_REV = "HEAD"

# ~/Project Work itself. Verified not to be a git repository, which is why it is a separate token
# with its own documented resolution rule rather than a fourth entry in REPO_TOKENS.
BOARD_REPO_TOKEN = "board"

# token -> (tracker slug, $HOME-relative default path). Identical in every copy of this script, so
# a cross-repo declaration means the same thing whichever copy evaluates it. The token is MANDATORY
# in a descriptor and there is no implicit default, deliberately: citing the wrong repo was itself
# one of the 2026-08-25 miscounts (a `chrome.css` path that exists in two of these repos), and an
# implicit "this repo" default would have hidden exactly that choice instead of exposing it.
REPO_TOKENS = {
    "juneau": ("juneau", "git/apache/juneau"),
    "console": ("sandbox-support-console", "git/sandbox/sandbox-support-console"),
    "release-manager": ("juneau-release-manager", "git/apache/release-manager"),
}

# This script lives at <repo-root>/scripts/, so its own repo root is a filesystem fact. Used in
# preference to the $HOME-relative default whenever a declaration names THIS repo, which keeps the
# common case (an item counting inside its own project) working from a relocated checkout.
SELF_REPO_ROOT = Path(__file__).resolve().parent.parent

GIT_TIMEOUT_SECONDS = 60

# The two comparison forms. '=' is the default and the right shape for a baseline; '>=' declares a
# floor, which is what an invariant means. Two operators rather than a separate `kind` because the
# thing being counted is unchanged -- only the assertion about the number differs.
COUNT_OP_EQ = "="
COUNT_OP_FLOOR = ">="
COUNT_OPS = (COUNT_OP_EQ, COUNT_OP_FLOOR)

# Derivation outcomes, distinguished for the same reason the tri-state is: a number that could not
# be derived must never be reported as a number that matched.
COUNT_OK = "ok"
COUNT_SCOPE_EMPTY = "scope_empty"
COUNT_UNVERIFIABLE = "unverifiable"


class CountClaim:
    """One parsed `Counts:` declaration: what to count, where, the operator, and the expected total."""

    def __init__(self, repo: str, kind: str, pathspec: str, pattern: "re.Pattern | None", expected: int, descriptor: str, operator: str = COUNT_OP_EQ):
        self.repo = repo
        self.kind = kind
        self.pathspec = pathspec
        self.pattern = pattern
        self.expected = expected
        self.descriptor = descriptor
        self.operator = operator

    @property
    def is_floor(self) -> bool:
        return self.operator == COUNT_OP_FLOOR

    def rendered(self) -> str:
        """The declaration as the author wrote it, for every flag detail.

        Built from the parsed parts rather than the raw line so a detail message can never show an
        operator the checker did not actually apply -- reporting '= 36' for a claim evaluated as a
        floor would make the flag unactionable in the one case where the response depends on it.
        """
        return f"`{self.descriptor}` {self.operator} {self.expected}"


def glob_to_regex(pathspec: str) -> "re.Pattern":
    """
    Compile a slash-delimited glob into an anchored regex over repo-relative POSIX paths.

    Hand-translated rather than delegated to fnmatch because fnmatch's `*` matches `/`, which would
    silently widen every pathspec past the directory the author wrote -- a count over a wider file
    set than declared is a wrong count that looks right. Supported: `**/` (zero or more directories),
    a trailing/bare `**`, `*` and `?` (within one segment), and `[...]`/`[!...]` character classes.
    """
    out = ["^"]
    i, n = 0, len(pathspec)
    while i < n:
        if pathspec.startswith("**/", i):
            out.append("(?:[^/]*/)*")
            i += 3
        elif pathspec.startswith("**", i):
            out.append(".*")
            i += 2
        elif pathspec[i] == "*":
            out.append("[^/]*")
            i += 1
        elif pathspec[i] == "?":
            out.append("[^/]")
            i += 1
        elif pathspec[i] == "[":
            close = pathspec.find("]", i + 1)
            if close == -1:
                out.append(re.escape(pathspec[i]))
                i += 1
            else:
                body = pathspec[i + 1 : close]
                out.append("[" + ("^" + body[1:] if body.startswith("!") else body) + "]")
                i = close + 1
        else:
            out.append(re.escape(pathspec[i]))
            i += 1
    out.append("$")
    return re.compile("".join(out))


def parse_counts_value(value: str) -> tuple:
    """
    Parse one `Counts:` field value into ("none", None) / ("claim", CountClaim) / ("malformed", reason).

    Three outcomes, mirroring the Key-symbols tri-state, with Absent handled by the caller (no line
    at all). Nothing here touches the filesystem: a grammar error is a grammar error whether or not
    the repos are reachable, which is what lets --no-counts still report counts_malformed.
    """
    stripped = value.strip()
    if stripped == "":
        return ("malformed", "'Counts:' line has no value -- write 'None.' to declare that nothing here is mechanically countable.")
    if stripped.lower() == COUNTS_NONE_VALUE:
        return ("none", None)

    claim_match = COUNT_CLAIM_RE.match(stripped)
    if claim_match is None:
        return ("malformed", f"expected `<repo>:<kind>:<pathspec>[:<regex>]` = <integer> (or >= <integer> for a floor), got: {stripped}")

    descriptor, operator, expected = claim_match.group(1), claim_match.group(2), int(claim_match.group(3))
    # maxsplit=3 so a regex containing ':' stays intact; the pathspec therefore may not contain one,
    # which also excludes git's magic-pathspec syntax (":(glob)...") -- documented, not accidental.
    fields = descriptor.split(":", 3)
    if len(fields) < 3:
        return ("malformed", f"descriptor needs at least <repo>:<kind>:<pathspec>, got: {descriptor}")

    repo, kind, pathspec = fields[0].strip(), fields[1].strip(), fields[2].strip()
    pattern_source = fields[3] if len(fields) == 4 else None

    if repo != BOARD_REPO_TOKEN and repo not in REPO_TOKENS:
        known = ", ".join(sorted(list(REPO_TOKENS) + [BOARD_REPO_TOKEN]))
        return ("malformed", f"unknown repo token '{repo}' (known: {known})")
    if kind not in COUNT_KINDS:
        return ("malformed", f"unknown kind '{kind}' (known: {', '.join(COUNT_KINDS)})")
    if pathspec == "":
        return ("malformed", f"empty pathspec in: {descriptor}")
    if pathspec.startswith("/") or ".." in pathspec.split("/"):
        return ("malformed", f"pathspec must be repo-relative with no '..': {pathspec}")

    if kind == COUNT_KIND_PATHS:
        if pattern_source is not None:
            return ("malformed", f"kind 'paths' counts files and takes no regex, but one was given: {descriptor}")
        if expected == 0:
            # A zero-file scope is counts_scope_empty by rule 2, so `paths ... = 0` could only ever
            # be reported as a failure. Rejecting it at parse time says so plainly instead of
            # letting an author write a claim that is defined to flag.
            return ("malformed", "kind 'paths' cannot declare 0 -- a glob matching nothing is indistinguishable from a glob that has rotted (see counts_scope_empty).")
        pattern = None
    else:
        if not pattern_source:
            return ("malformed", f"kind '{kind}' needs a regex after the pathspec: {descriptor}")
        try:
            pattern = re.compile(pattern_source, re.MULTILINE)
        except re.error as exc:
            return ("malformed", f"regex does not compile ({exc}): {pattern_source}")

    # A floor of 0 holds for every possible derivation, so it asserts nothing while looking like a
    # guard -- the one decorative-floor case that can be caught without inventing a slack threshold
    # (see the module docstring's DELIBERATE NON-GOAL). Refused for the same reason `paths ... = 0` is.
    if operator == COUNT_OP_FLOOR and expected == 0:
        return ("malformed", "a floor of 0 asserts nothing -- every derivation satisfies `>= 0`. Declare the floor you actually mean, or use '=' if the claim is that the count is exactly 0.")

    return ("claim", CountClaim(repo, kind, pathspec, pattern, expected, descriptor, operator))


class CountsResolver:
    """
    Re-derives declared counts from committed content.

    Holds no persistent state and writes nothing: the only thing it remembers is, within one run,
    the path listing and blob text of a single immutable revision, so that two declarations over the
    same commit do not re-read it. That is deduplication of identical reads, not a cached verdict --
    a fresh process re-derives every number from git from scratch, which is the property that makes
    a stale declaration impossible to pass by inertia.
    """

    def __init__(self, repo_overrides: "dict | None" = None, project_work: "Path | None" = None):
        self._overrides = dict(repo_overrides or {})
        self._project_work = project_work or PROJECT_WORK
        self._paths_cache = {}
        self._blob_cache = {}

    def repo_root(self, token: str) -> "Path | None":
        """Where a repo token resolves to, or None if the token is unknown."""
        if token in self._overrides:
            return Path(self._overrides[token]).expanduser()
        if token == BOARD_REPO_TOKEN:
            return self._project_work
        entry = REPO_TOKENS.get(token)
        if entry is None:
            return None
        slug, home_relative = entry
        # A token naming THIS repo resolves to this script's own root, which is derived rather than
        # assumed; other tokens fall back to the shared default location.
        return SELF_REPO_ROOT if slug == TRACKER_SLUG else Path.home() / home_relative

    def _git(self, root: Path, args: list) -> "str | None":
        """Run a read-only git command in root, returning stdout or None on any failure."""
        try:
            done = subprocess.run(
                ["git", "-C", str(root), *args],
                capture_output=True,
                text=True,
                timeout=GIT_TIMEOUT_SECONDS,
                check=False,
            )
        except (OSError, subprocess.SubprocessError):
            return None
        return done.stdout if done.returncode == 0 else None

    def _repo_paths(self, token: str, root: Path) -> "list | None":
        """Every file path in scope for this token: the revision's tree, or the board's filesystem."""
        if token in self._paths_cache:
            return self._paths_cache[token]

        if token == BOARD_REPO_TOKEN:
            # Not a git repository (verified), so the filesystem IS the record here -- see the
            # module docstring for why that is sound for the board and wrong for a cloned repo.
            if not root.is_dir():
                paths = None
            else:
                paths = sorted(str(p.relative_to(root).as_posix()) for p in root.rglob("*") if p.is_file())
        else:
            listing = self._git(root, ["ls-tree", "-r", "--name-only", "-z", COUNTS_REV])
            paths = None if listing is None else sorted(p for p in listing.split("\0") if p)

        self._paths_cache[token] = paths
        return paths

    @staticmethod
    def _decode(raw: "bytes | None") -> "str | None":
        """Decoded text, or None for unreadable/binary content.

        A NUL byte means binary; counting a regex over decoded binary is meaningless, and skipping
        it matches what `git grep -I` / `rg` already do, so a declared count never silently picks
        up noise from a jar or an image.
        """
        if raw is None or b"\0" in raw:
            return None
        return raw.decode("utf-8", errors="replace")

    def _read_all(self, token: str, root: Path, paths: list) -> dict:
        """
        {path: text-or-None} for every requested path, populating the per-run blob cache.

        For a git repo the blobs are fetched through a SINGLE `git cat-file --batch` process rather
        than one `git show` per file: a pathspec like "**/*.java" over a large repo selects thousands
        of files, and per-file subprocesses turned a sub-second check into a minute-long one.
        """
        wanted = [p for p in paths if (token, p) not in self._blob_cache]

        if wanted and token == BOARD_REPO_TOKEN:
            for path in wanted:
                try:
                    raw = (root / path).read_bytes()
                except OSError:
                    raw = None
                self._blob_cache[(token, path)] = self._decode(raw)
        elif wanted:
            for path, raw in self._cat_file_batch(root, wanted).items():
                self._blob_cache[(token, path)] = self._decode(raw)

        # Anything cat-file declined to answer for is recorded as unreadable rather than retried, so
        # one bad path cannot make the whole declaration silently count fewer files than it names.
        for path in wanted:
            self._blob_cache.setdefault((token, path), None)

        return {p: self._blob_cache[(token, p)] for p in paths}

    def _cat_file_batch(self, root: Path, paths: list) -> dict:
        """{path: bytes} for the given paths at COUNTS_REV, via one `git cat-file --batch`."""
        request = "".join(f"{COUNTS_REV}:{p}\n" for p in paths).encode("utf-8")
        try:
            done = subprocess.run(
                ["git", "-C", str(root), "cat-file", "--batch"],
                input=request,
                capture_output=True,
                timeout=GIT_TIMEOUT_SECONDS,
                check=False,
            )
        except (OSError, subprocess.SubprocessError):
            return {}
        if done.returncode != 0:
            return {}

        out, offset, blobs = done.stdout, 0, {}
        for path in paths:
            newline = out.find(b"\n", offset)
            if newline == -1:
                break
            header = out[offset:newline].split(b" ")
            offset = newline + 1
            # "<oid> <type> <size>" for a hit; "<request> missing" (or "ambiguous") otherwise, and a
            # non-blob (a path that resolves to a tree) carries no content to skip past either.
            if len(header) != 3 or header[1] != b"blob":
                continue
            size = int(header[2])
            blobs[path] = out[offset : offset + size]
            offset += size + 1  # git writes a trailing newline after each object's contents
        return blobs

    def derive(self, claim: CountClaim) -> tuple:
        """
        Re-derive this claim's count NOW. Returns (outcome, value, detail).

        outcome is COUNT_OK (value is the freshly counted integer), COUNT_SCOPE_EMPTY (the pathspec
        matched no file, so there is nothing to count and the declaration is unanchored), or
        COUNT_UNVERIFIABLE (the repo or revision could not be read at all).
        """
        root = self.repo_root(claim.repo)
        if root is None:
            return (COUNT_UNVERIFIABLE, None, f"unknown repo token '{claim.repo}'")
        if not root.is_dir():
            return (COUNT_UNVERIFIABLE, None, f"'{claim.repo}' does not resolve to a directory ({root})")

        paths = self._repo_paths(claim.repo, root)
        if paths is None:
            return (COUNT_UNVERIFIABLE, None, f"cannot read {claim.repo} at {COUNTS_REV} ({root})")

        matcher = glob_to_regex(claim.pathspec)
        in_scope = [p for p in paths if matcher.match(p)]
        if not in_scope:
            return (COUNT_SCOPE_EMPTY, None, f"pathspec '{claim.pathspec}' matches no file in {claim.repo}")

        if claim.kind == COUNT_KIND_PATHS:
            return (COUNT_OK, len(in_scope), "")

        occurrences, hit_files = 0, 0
        for text in self._read_all(claim.repo, root, in_scope).values():
            if text is None:
                continue
            found = len(claim.pattern.findall(text))
            if found:
                hit_files += 1
                occurrences += found
        return (COUNT_OK, occurrences if claim.kind == COUNT_KIND_MATCHES else hit_files, "")


def find_plan_files(todo_dir: Path) -> list:
    """Every TODO-/READY-/MAYBE-/HOLD-/NEED_INPUT-<id>[<letter>]-*.md file directly under todo_dir,
    sorted by name. Includes claim stubs (CLAIM_STUB_RE) -- an id reservation must keep counting
    as "taken" -- but the caller routes those to audit_claim_stub() instead of audit_file(); see
    the module docstring's "Claim-stub handling" section."""
    return sorted(p for p in todo_dir.glob("*.md") if FILENAME_RE.match(p.name))


def group_claims_by_numeric_id(files: list) -> dict:
    """
    Map numeric id (int, letter suffix stripped) -> {"stub": Path | None, "plans": [Path, ...]},
    from files (as returned by find_plan_files(), which already includes claim stubs).

    Feeds exactly one check: unfinalized_claim_stub in audit_claim_stub() below. A stub and ANY
    real (non-stub) file sharing the same numeric id -- including a lettered child of that id --
    means the id was reserved but the reservation was never consumed by finalizing it, because a
    plan-shaped file for that same number already exists under a different name. Grouping by the
    bare numeric id (not the raw digit string) matters here for the same reason todo-next-id.py
    itself keys everything off int(...): "TODO-R0037-..." and a hypothetically-unpadded
    "TODO-R37-..." name the same id and must land in the same bucket.
    """
    groups: dict[int, dict] = {}
    for path in files:
        m = ID_FROM_FILENAME_RE.match(path.name)
        if not m:
            continue
        numeric_id = int(m.group(1))
        entry = groups.setdefault(numeric_id, {"stub": None, "plans": []})
        if CLAIM_STUB_RE.match(path.name):
            entry["stub"] = path
        else:
            entry["plans"].append(path)
    return groups


def file_prefix(path: Path) -> str:
    """"TODO", "READY", "MAYBE", "HOLD", or "NEED_INPUT" (the filename already matched FILENAME_RE)."""
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


def header_region(text: str) -> str:
    """
    Everything before the first "## " section heading (the whole file if there is none).

    This is the region where a plan file states what it IS -- title, Source, Current status,
    Complexity, Parent -- as opposed to the body sections, where it discusses other items. That
    split is what separates "I am a member of WAVE-nnnn" from "I must be serialized against
    WAVE-nnnn's members": in the real corpus the four items that only cross-reference an accepted
    wave do so from a body section ("## Sequencing", "## Notes"-adjacent prose), several hundred
    lines in, while every actual member names its wave in the header region.

    Body-vs-header alone is not load-bearing, and is not meant to be: the status-prefix condition
    in _flag_ready_but_wave_accepted() independently excludes all four of those cross-referencing
    items, so the two conditions are belt-and-braces rather than a single fuzzy guess.
    """
    first_heading = SECTION_HEADING_RE.search(text)
    return text[: first_heading.start()] if first_heading is not None else text


def closed_out_waves(board_path: "Path | None") -> frozenset:
    """
    The set of WAVE-nnnn ids the board records as accepted AND that have no live WAVE file left.

    Both halves are required, and each covers the other's failure mode:
      - The board's accepted-wave log is prose, so a loose "accepted" word match could in
        principle pick up a wave that is only being discussed.
      - The waves/ directory is a filesystem fact, not prose: per the board's own documented
        lifecycle ("After the operator accepts ... delete that WAVE-nnnn-*.md"), an in-flight or
        merely-planned wave still has its file on disk. Requiring the file to be GONE means a
        wave has to be closed out in both records before any item can be flagged against it.

    Returns an empty set for a missing/unreadable board, which makes the whole
    ready_but_wave_already_accepted check inert rather than wrong -- the same reason this script
    treats a missing scan directory as a hard error but an empty one as a clean pass: never
    manufacture a verdict from an absent input.
    """
    if board_path is None or not board_path.is_file():
        return frozenset()
    try:
        board_text = board_path.read_text(encoding="utf-8")
    except OSError:
        return frozenset()

    accepted = set()
    for line in board_text.splitlines():
        if BOARD_ACCEPTED_RE.search(line) and not BOARD_NOT_ACCEPTED_RE.search(line):
            accepted.update(WAVE_ID_RE.findall(line))

    waves_dir = board_path.parent / WAVES_DIRNAME
    if waves_dir.is_dir():
        live = {w for p in waves_dir.glob("*.md") for w in WAVE_ID_RE.findall(p.name)}
        accepted -= live
    return frozenset(accepted)


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
        if not RESOLVED_MARKER_RE.search(block):
            return True
    return False


def normalize_status_text(status: str) -> str:
    """Status text with leading markdown emphasis/quote decoration stripped and case-folded, used by
    the phrase-prefix lifecycle-mismatch checks below (e.g. recognizing 'ready to execute' under a
    leading '**')."""
    return STATUS_DECORATION_RE.sub("", status).strip().lower()


def status_value_is_empty(status: str) -> bool:
    """
    True if the "Current status:" field is present but carries no real content once markdown/
    punctuation decoration is stripped -- e.g. a header line left unfilled, or a value that is only
    dashes/asterisks. Deliberately permissive about WHAT the remaining text says: the trackers have
    settled on free-form descriptive prose rather than a fixed phrase set, so this only catches the
    value being structurally absent, never any particular wording.
    """
    stripped = re.sub(r"[\s*_`\"'\u2010-\u2015.,:;()\[\]-]+", "", status)
    return stripped == ""


def _flag_missing_headers(status_match: "re.Match | None", complexity_match: "re.Match | None", flags: list) -> None:
    """Appends missing_status_header / missing_complexity_header for headers absent from the file."""
    if status_match is None:
        flags.append(("missing_status_header", "No 'Current status:' line found."))
    if complexity_match is None:
        flags.append(("missing_complexity_header", "No 'Complexity:' line found."))


def _flag_status_placement(text: str, status_match: "re.Match", flags: list) -> None:
    """Appends status_header_misplaced if the status line appears at/after the first '##' heading."""
    first_heading = SECTION_HEADING_RE.search(text)
    if first_heading is not None and status_match.start() >= first_heading.start():
        flags.append(("status_header_misplaced", "'Current status:' appears at/after the first '##' section heading."))


def _flag_open_questions_if_ready(prefix: str, normalized_status: str, text: str, flags: list) -> None:
    """Appends ready_but_has_open_questions if a file that declares itself ready -- by READY- filename
    prefix or by status wording -- still has an unresolved '## Open questions'."""
    if prefix not in ("TODO", "READY"):
        return
    # Union of the two declarations, not the status phrase alone. The READY- prefix IS the board's
    # structural declaration that an item has no unresolved open questions left (see the prefix table
    # above), so gating on the prose phrase instead made the check dodgeable by wording: a READY- file
    # whose status opens with anything else ("In progress -- ...", "No open decision blocks this item.")
    # carried its unresolved questions straight past this check. The phrase condition is kept so an item
    # that declares readiness in prose without the rename is still gated.
    declares_ready = prefix == "READY" or normalized_status.startswith(READY_TO_EXECUTE_PREFIX)
    if not declares_ready:
        return
    oq_section = extract_section(text, "Open questions")
    if oq_section is not None and open_questions_are_unresolved(oq_section):
        flags.append((
            "ready_but_has_open_questions",
            "File declares itself ready (READY- filename prefix, or a 'Ready to execute' status) but "
            "'## Open questions' still has unresolved item(s).",
        ))


def _flag_ready_but_wave_accepted(prefix: str, normalized_status: str, text: str, closed_waves: frozenset, flags: list) -> None:
    """
    Appends ready_but_wave_already_accepted for a Ready-to-execute file still queued behind a wave
    the board has already accepted and closed out.

    Scoped to TODO-/READY- for the same reason the other Ready-gated check is: this is a
    TODO/READY lifecycle signal, and the docstring's prefix table exempts NEED_INPUT- from those.
    """
    if not closed_waves or prefix not in ("TODO", "READY"):
        return
    if not normalized_status.startswith(READY_TO_EXECUTE_PREFIX):
        return
    declared = sorted(set(WAVE_ID_RE.findall(header_region(text))) & closed_waves)
    if declared:
        waves = ", ".join(declared)
        flags.append((
            "ready_but_wave_already_accepted",
            f"Status still says 'Ready to execute' but the board records {waves} as accepted (WAVE file gone) -- "
            f"archive the item if it shipped in that wave, or drop the stale wave reference.",
        ))


def _flag_status_prefix_transitions(prefix: str, status: str, normalized_status: str, flags: list) -> None:
    """Appends the READY/TODO/MAYBE/HOLD prefix-vs-status-wording mismatch flags (missed renames, wrong prefix)."""
    if prefix == "READY" and normalized_status.startswith("waiting for user input"):
        flags.append(("ready_prefix_waiting_status", "READY-prefixed file but status still says 'Waiting for user input'."))

    if prefix == "TODO" and normalized_status.startswith(READY_TO_EXECUTE_PREFIX):
        flags.append(("todo_prefix_marked_ready", "TODO-prefixed file already marked 'Ready to execute' -- possible missed rename to READY-*.md."))

    if prefix in ("TODO", "READY") and normalized_status.startswith(MAYBE_STATUS_PREFIX):
        flags.append(("parked_status_wrong_prefix", "Status says 'Parked...' but filename is not MAYBE-prefixed."))

    if prefix == "MAYBE" and not normalized_status.startswith(MAYBE_STATUS_PREFIX):
        flags.append(("maybe_prefix_non_parked", f"MAYBE-prefixed file but status doesn't start with 'Parked': '{status}'"))

    if prefix in ("TODO", "READY") and normalized_status.startswith(HOLD_STATUS_PREFIX):
        flags.append(("on_hold_status_wrong_prefix", "Status says 'On hold...' but filename is not HOLD-prefixed."))

    if prefix == "HOLD" and not normalized_status.startswith(HOLD_STATUS_PREFIX):
        flags.append(("hold_prefix_non_on_hold", f"HOLD-prefixed file but status doesn't start with 'On hold': '{status}'"))


def _flag_declared_counts(text: str, counts_resolver: "CountsResolver | None", flags: list) -> None:
    """
    Appends counts_malformed / counts_mismatch / counts_below_floor / counts_scope_empty /
    counts_unverifiable.

    An ABSENT `Counts:` line produces nothing, which is the whole reason this check can be turned on
    over an existing corpus: a file that makes no declaration is making no claim. Grammar errors are
    reported with or without a resolver, because a line that does not parse is wrong regardless of
    whether the source it names is reachable.
    """
    for line_match in COUNTS_LINE_RE.finditer(text):
        outcome, parsed = parse_counts_value(line_match.group(1))
        if outcome == "none":
            continue
        if outcome == "malformed":
            flags.append(("counts_malformed", f"'Counts:' line does not parse -- {parsed}"))
            continue
        if counts_resolver is None:
            continue

        derived_outcome, derived, detail = counts_resolver.derive(parsed)
        if derived_outcome == COUNT_UNVERIFIABLE:
            flags.append((
                "counts_unverifiable",
                f"declared count {parsed.rendered()} could not be re-derived: {detail}. "
                f"An unverified count is not a verified one -- fix the repo token/path, or pass --repo-root.",
            ))
        elif derived_outcome == COUNT_SCOPE_EMPTY:
            # Checked BEFORE either comparison, and that ordering is load-bearing for floors: a
            # pathspec that has rotted to nothing would otherwise derive 0 and be reported as a floor
            # breach, sending the operator to hunt a regression in code that is fine. An unanchored
            # declaration is its own failure whichever operator it was written with.
            flags.append((
                "counts_scope_empty",
                f"declared count {parsed.rendered()} is no longer anchored: {detail} "
                f"at {COUNTS_REV}. The number cannot be tested, so it is flagged rather than passed.",
            ))
        elif parsed.is_floor:
            if derived < parsed.expected:
                flags.append((
                    "counts_below_floor",
                    f"declared floor {parsed.rendered()} BREACHED -- re-derived {derived} "
                    f"from {parsed.repo} at {COUNTS_REV}. A floor is an invariant: investigate the drop. "
                    f"Do NOT lower the floor to match -- that deletes the guard.",
                ))
        elif derived != parsed.expected:
            flags.append((
                "counts_mismatch",
                f"declared count {parsed.rendered()} but re-derived {derived} "
                f"from {parsed.repo} at {COUNTS_REV}.",
            ))


def audit_claim_stub(path: Path, coexisting_plans: "list | tuple" = ()) -> list:
    """
    Return [(reason_code, detail)] for one "TODO-<id>-CLAIMED.md" claim stub (CLAIM_STUB_RE).

    A claim stub is exempt from every check in audit_file() -- it is a reservation, not a plan,
    and is deliberately un-plan-shaped by claim_placeholder_text() (todo-next-id.py) so a human
    recognizes an abandoned reservation instead of a plausible-looking empty TODO. Two things are
    checked here instead:
      - unfinalized_claim_stub (coexisting_plans, from group_claims_by_numeric_id()): whether a
        real plan file already exists for this SAME numeric id. Unconditional -- checked
        regardless of the stub's age, because it is wrong the instant it exists. coexisting_plans
        defaults to empty so every existing single-argument caller keeps its previous behavior.
      - stale_claim: whether the reservation looks abandoned by age. See STALE_CLAIM_AGE_HOURS and
        the module docstring's "Claim-stub handling" section for why age -- not the pid/hostname
        the stub also records -- is the signal.
    """
    flags = []

    if coexisting_plans:
        names = ", ".join(p.name for p in sorted(coexisting_plans))
        plural = len(coexisting_plans) > 1
        flags.append((
            "unfinalized_claim_stub",
            f"this id-reservation stub coexists with {'real plan files' if plural else 'a real plan file'} "
            f"under the SAME numeric id: {names}. An id must never simultaneously name a reservation and a "
            f"finished plan -- this is wrong regardless of age (distinct from 'stale_claim' below, if that is "
            f"also flagged). Most likely the claiming session wrote the real plan under a brand-new filename "
            f"instead of finalizing this stub in place (see todo-next-id.py's --finalize). If {names} is the "
            f"complete, correct plan for this id, delete this stub -- do not run --finalize, it will refuse "
            f"since the target already exists. If you are not certain the plan is complete, investigate "
            f"before deleting anything.",
        ))

    age_hours = (time.time() - path.stat().st_mtime) / 3600
    if age_hours < STALE_CLAIM_AGE_HOURS:
        return flags

    text = path.read_text(encoding="utf-8")
    claimed_at = CLAIM_CLAIMED_AT_RE.search(text)
    claimed_by = CLAIM_CLAIMED_BY_RE.search(text)
    when = claimed_at.group(1) if claimed_at else "an unrecorded time (no 'Claimed at:' line)"
    who = (
        f"pid {claimed_by.group(1)} on {claimed_by.group(2)}"
        if claimed_by
        else "an unrecorded claimant (no 'Claimed by:' line)"
    )

    flags.append((
        "stale_claim",
        f"id-reservation stub is {age_hours:.1f}h old (threshold {STALE_CLAIM_AGE_HOURS}h) -- "
        f"claimed at {when} by {who} -- and was never turned into a real plan or removed. The "
        f"claiming session likely crashed; confirm nothing references this id, then delete the "
        f"stub per its own instructions ('todo-next-id.py --list-claims' finds every stub of "
        f"this kind).",
    ))
    return flags


def audit_file(path: Path, closed_waves: frozenset = frozenset(), counts_resolver: "CountsResolver | None" = None) -> list:
    """
    Return a list of (reason_code, detail) tuples for this plan file. Empty list means no flags.

    closed_waves defaults to empty, which makes ready_but_wave_already_accepted inert: every
    caller that has no wave board (and every existing single-argument caller) gets exactly the
    previous behavior instead of a check silently running against an unknown board.

    counts_resolver defaults to None on the same principle: without one, declared counts are checked
    for GRAMMAR only and never reported as agreeing, so a caller that cannot reach the source trees
    gets silence about the numbers rather than a manufactured pass.
    """
    text = path.read_text(encoding="utf-8")
    prefix = file_prefix(path)
    flags = []

    _flag_declared_counts(text, counts_resolver, flags)

    status_match = STATUS_LINE_RE.search(text)
    complexity_match = COMPLEXITY_LINE_RE.search(text)
    _flag_missing_headers(status_match, complexity_match, flags)

    if status_match is not None:
        status = status_match.group(1).strip()
        normalized = normalize_status_text(status)

        _flag_status_placement(text, status_match, flags)

        if status_value_is_empty(status):
            flags.append(("empty_status_value", "'Current status:' field is present but has no content."))

        _flag_open_questions_if_ready(prefix, normalized, text, flags)
        _flag_ready_but_wave_accepted(prefix, normalized, text, closed_waves, flags)
        _flag_status_prefix_transitions(prefix, status, normalized, flags)

    return flags


def main() -> int:
    parser = argparse.ArgumentParser(
        description=f"Best-effort pre-filter for {REPO_LABEL} tracker header inconsistencies (see @{SKILL_NAME}).",
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--root", metavar="PATH", help="Project Work root (default: ~/Project Work).")
    parser.add_argument("--dir", metavar="PATH", help="Exact directory to scan, overriding --root.")
    parser.add_argument("--board", metavar="PATH", help=f"Wave board file (default: {BOARD_FILENAME} beside the tracker's parent).")
    parser.add_argument("--repo-root", metavar="TOKEN=PATH", action="append", default=[], help="Override where a Counts: repo token resolves to. Repeatable.")
    parser.add_argument("--no-counts", action="store_true", help="Do not re-derive declared counts (grammar errors are still reported).")
    parser.add_argument("--verbose", "-v", action="store_true", help="Also print files that passed every check.")
    args = parser.parse_args()

    repo_overrides = {}
    for override in args.repo_root:
        token, _, override_path = override.partition("=")
        if not token or not override_path:
            print(f"ERROR: --repo-root expects TOKEN=PATH, got {override!r}.", file=sys.stderr)
            return 2
        repo_overrides[token] = override_path

    if args.dir:
        todo_dir = Path(args.dir).resolve()
    else:
        project_work = Path(args.root).expanduser().resolve() if args.root else PROJECT_WORK
        todo_dir = project_work / "todos" / TRACKER_SLUG

    # Inferred as a sibling of todos/, i.e. <project-work>/WAVE.md, which holds for --root and for
    # the default. Under --dir the inference can miss, and missing is the correct outcome there:
    # closed_out_waves() then returns empty and the wave check stays inert instead of comparing
    # against some unrelated board that happened to sit two levels up.
    board_path = Path(args.board).expanduser().resolve() if args.board else todo_dir.parent.parent / BOARD_FILENAME

    # Always announce the tree actually scanned, for the same reason todo-next-id.py does:
    # bare per-repository ids make a wrong-tree run indistinguishable from a right-tree one.
    print(f"[{REPO_LABEL}] scanning {todo_dir}", file=sys.stderr)

    # A missing directory and an empty one are NOT the same answer. Missing means the caller
    # is looking at the wrong tree (or the scaffolding was never installed) and must be told;
    # empty means a genuinely clean tracker and is a legitimate pass.
    if not todo_dir.is_dir():
        print(f"ERROR: {todo_dir} does not exist -- nothing was scanned. Check the working tree, or pass --dir.", file=sys.stderr)
        return 2

    files = find_plan_files(todo_dir)

    if not files:
        print(f"No TODO-/READY-/MAYBE-/HOLD-/NEED_INPUT-*.md files found under {todo_dir} (directory exists and is empty of plan files).")
        return 0

    # Announced for the same reason the scan directory is: a silently-skipped wave check looks
    # identical to a clean one, which is the trap this script keeps trying to close.
    closed_waves = closed_out_waves(board_path)
    if closed_waves:
        print(f"[{REPO_LABEL}] wave board {board_path}: accepted+closed {', '.join(sorted(closed_waves))}", file=sys.stderr)
    else:
        print(f"[{REPO_LABEL}] no accepted+closed waves from {board_path} -- ready_but_wave_already_accepted is inert this run.", file=sys.stderr)

    # Announced for the same reason the wave check is: "no count disagreed" and "no count was
    # checked" must not look the same on the terminal.
    if args.no_counts:
        counts_resolver = None
        print(f"[{REPO_LABEL}] --no-counts: declared counts are grammar-checked only, not re-derived.", file=sys.stderr)
    else:
        counts_resolver = CountsResolver(repo_overrides, project_work=todo_dir.parent.parent)
        print(f"[{REPO_LABEL}] declared counts re-derived from {COUNTS_REV} this run (nothing cached between runs).", file=sys.stderr)

    claim_groups = group_claims_by_numeric_id(files)

    flagged_count = 0
    for path in files:
        if CLAIM_STUB_RE.match(path.name):
            numeric_id = int(ID_FROM_FILENAME_RE.match(path.name).group(1))
            coexisting_plans = claim_groups.get(numeric_id, {}).get("plans", [])
            flags = audit_claim_stub(path, coexisting_plans)
        else:
            flags = audit_file(path, closed_waves, counts_resolver)
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
