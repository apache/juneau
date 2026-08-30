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
Post-harvest side-clone reset, automating the "After harvest (reset the side clone)" procedure
in ~/Project Work/skills/todo-and-waves/SKILL.md.

That skill is the authority for this procedure. Where this script and the skill disagree, the
skill wins and this script is the bug. The seven numbered steps below are the skill's, in the
skill's order:

  1. Confirm the clone is not `in-flight` in repos.md and not a canonical tree.
  2. git fetch the clone's own origin.
  3. git checkout master (Juneau / IRS) or main (Console).
  4. git reset --hard to the canonical remote-tracking branch.
  5. Delete leftover local cu/* branches if skill-safe.
  6. git clean -fd for untracked non-ignored files. Never git clean -x (preserve .work/).
  7. Update repos.md: branch = master/main, WAVE / session empty, status idle.

WHY THIS EXISTS. It is not a keystroke saver. It runs at a wave boundary -- the exact moment a
hand-run drops a guard -- and it performs `reset --hard` plus `clean` across up to nine working
trees. Its whole value is that the guards below cannot be skipped, so every one of them is a
REFUSAL (nothing destructive is attempted on that clone) rather than a warning.

GUARDS
  * Canonical trees are refused by RESOLVED REAL PATH, so a symlink, a relative invocation, or a
    board row pointing at one cannot slip past. Belt and braces: the hardcoded CANONICAL_TREES
    list, plus any repos.md row whose role says "canonical", plus a refusal inside the git
    wrapper itself so no code path can run git in a canonical tree at all.
  * Only clones the board knows about can be selected. An arbitrary path is refused.
  * `in-flight` (and `do-not-use`) rows are refused.
  * An unreadable or unparseable repos.md aborts everything before any clone is touched. Treating
    a board we could not read as "nothing is in flight" is the fail-open version of that check,
    so a missing file, a decode error, a missing table, an unexpected header, or a malformed row
    all exit 2 having done nothing.
  * A clone with uncommitted, staged, or untracked-non-ignored work is refused and reported, not
    reset over.
  * A remote whose URL is a local path resolving into ANOTHER project's canonical tree is refused.
    That is the "never cross IRS with Apache remotes" rule made mechanical. Fetches never pass a
    URL or a remote outside the clone's own configured remotes, so a crossing cannot originate
    here; this guard catches one that was configured elsewhere.
  * `.work/` is copied out before the clone is touched and restored after, and the copy is only
    deleted once the post-reset `.work/` verifies byte-identical to it.
  * `git clean` can only ever run as GIT_CLEAN_ARGV == ("clean", "-fd"). There is deliberately no
    flag, parameter, or code path that can add -x: _git_clean() takes no flag argument, and _git()
    refuses a `clean` argv that is not exactly GIT_CLEAN_ARGV as well as any -x/-X style token in
    any argv. `git clean -x` is the one unrecoverable mistake in this procedure (it deletes the
    gitignored .work/ brainstorm tree), so it is unreachable by construction rather than by
    remembering not to pass it.
  * `git config` is refused by the wrapper in every form, including a read-only --get.

DEFAULT IS A DRY RUN. Acting requires --apply. The safe mode is the one you get by typing the
command wrong, forgetting a flag, or hitting Enter early. A dry run performs no fetch either --
it stays entirely read-only -- so the target commit it prints is the CURRENT remote-tracking sha
and is labelled as such; the real target is resolved after the fetch in step 2.

VERIFICATION compares actual file CONTENT against the canonical blob rather than trusting the
commit hash: after the reset, `git hash-object` over the real working-tree file is compared with
the blob id that the fetched canonical commit records for that path (`git rev-parse <ref>:<path>`).
A matching HEAD sha only proves the ref moved; it says nothing about what is on disk, which is
precisely the confusion this check exists to remove. SENTINEL_PATHS are checked when the target
ref carries them, and a deterministic sample of the target tree is used when it does not, so
"nothing to verify" can never be reported as a pass.

POOLS come from the board, not from this file -- the only per-pool knowledge here is the default
branch, mirroring the skill's pool table (Juneau master, IRS master, Console main). --all covers
the juneau and console pools; the IRS pool must be named explicitly with --pool irs, because the
skill isolates IRS as a separate hat and one command that resets Apache and IRS trees together
mixes them. The exclusion is printed, never silent.

Usage:
    ./scripts/reset-side-clones.py --all                          # dry run (default)
    ./scripts/reset-side-clones.py --all --apply
    ./scripts/reset-side-clones.py --pool juneau --apply
    ./scripts/reset-side-clones.py --pool irs --apply
    ./scripts/reset-side-clones.py --clones juneau-2,juneau-7 --apply

Options:
    --all                Every side clone in the juneau and console pools (IRS excluded; see above).
    --pool <name>        juneau | console | irs. Repeatable.
    --clones <list>      Comma-separated board basenames or paths. Repeatable.
    --apply              Actually perform the reset. Without this, nothing is written or run.
    --board <path>       repos.md to read (default: ~/Project Work/repos.md).
    --no-board-update    Do the reset but only REPORT the repos.md row change (step 7 by hand).
    --help, -h           Show this help message.

Exit status:
    0   Everything requested completed (or a dry run produced a plan with no refusals).
    1   At least one clone was refused by a guard. Nothing destructive was attempted on it.
    2   repos.md could not be read or parsed. NOTHING was done to any clone.
    3   A reset or a post-reset verification failed. Read the report; that clone needs attention.
    4   Usage error (no selection given, unknown pool, unknown or ambiguous clone name).
    5   Resets completed but one or more repos.md rows could not be written safely. The needed
        change is printed; apply it by hand. Reported rather than risking a mangled board.
    When several apply, the most severe is returned in the order 3, 5, 1.
"""

from __future__ import annotations

import argparse
import filecmp
import os
import re
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path

# ---------------------------------------------------------------------------------------
# Canonical trees. Refused by resolved real path -- never by string comparison against the
# spelling the caller happened to use. These are the three the skill names as canonical
# ("Never reset canonical juneau / irs-1 / sandbox-support-console"); board rows whose role
# says "canonical" are refused too, so a canonical tree added to the board later is covered
# without a code change here.
# ---------------------------------------------------------------------------------------
CANONICAL_TREES = (
    Path.home() / "git" / "apache" / "juneau",
    Path.home() / "git" / "central-routing" / "irs-1",
    Path.home() / "git" / "sandbox" / "sandbox-support-console",
)

BOARD_DEFAULT = Path.home() / "Project Work" / "repos.md"

# The board's own vocabulary. A row is a candidate only if its project and role match EXACTLY --
# an allowlist, not a substring test, which is what keeps "IRS-only (workspace)" (irs-1) and
# "IRS-only (FIT)" (irs-falcon-6) out while letting plain "IRS-only" side clones in.
PROJECT_TO_POOL = {"Juneau": "juneau", "Console": "console", "IRS": "irs"}
SELECTABLE_ROLES = frozenset({"worker clone", "IRS-only"})

# The skill's pool table. The only per-pool knowledge in this file.
POOL_DEFAULT_BRANCH = {"juneau": "master", "console": "main", "irs": "master"}

# --all deliberately omits irs; see the module docstring.
POOLS_IN_ALL = ("juneau", "console")

BOARD_COLUMNS = ("path", "project", "role", "branch", "wave / session", "status")

# ---------------------------------------------------------------------------------------
# git clean, in the only shape it is ever allowed to take. -x deletes gitignored files, which
# in a side clone means the .work/ brainstorm tree -- unrecoverable, and the reason this is a
# constant rather than a parameter. _git_clean() below has no flag argument at all, and _git()
# refuses any clean argv that is not exactly this tuple.
# ---------------------------------------------------------------------------------------
GIT_CLEAN_ARGV = ("clean", "-fd")

# Never run, in any form, including a read-only `git config --get user.email`.
FORBIDDEN_GIT_SUBCOMMANDS = frozenset({"config"})

# Content-verified after the reset when the target ref carries them (see verify_content()).
# Was ("scripts/todo-next-id.py", "scripts/todo-status-audit.py") until 2026-08-30, when those
# two moved out of this repo to ~/Project Work/scripts/ (consolidated, parameterized by
# --project) -- swapped for two files that DO still live in this repo, so clone-reset
# verification keeps the same teeth instead of silently checking sentinels that no longer exist.
SENTINEL_PATHS = ("scripts/push.py", "scripts/wave-survey.py")
SENTINEL_SAMPLE_SIZE = 5

WAVE_TOKEN_RE = re.compile(r"\bWAVE-\d{4}\b")
CU_BRANCH_PREFIX = "cu/"


class GitGuardViolation(RuntimeError):
    """A git invocation was refused before it ran. Always a bug in this script, never input."""


class BoardError(RuntimeError):
    """repos.md could not be read or parsed. Fail closed: nothing is touched."""


class ResetFailed(RuntimeError):
    """A step of the reset, or its verification, failed for one clone."""


def _real(path: Path | str) -> Path:
    """Resolve to a real path (expanding ~, following symlinks, collapsing '..')."""
    return Path(os.path.realpath(os.path.expanduser(str(path))))


CANONICAL_REAL = frozenset(_real(p) for p in CANONICAL_TREES)


# ---------------------------------------------------------------------------------------
# The single git choke point.
# ---------------------------------------------------------------------------------------
def _git(clone: Path, argv: list[str], *, check: bool = True) -> subprocess.CompletedProcess:
    """
    Run `git -C <clone> <argv>` after the guards that must hold for EVERY git invocation.

    Every git call in this script goes through here, which is what makes the guards properties
    of the script rather than habits of its callers:

      * a canonical tree is never the working directory of any git command,
      * `git config` never runs, in any form,
      * `git clean` runs only as GIT_CLEAN_ARGV,
      * no argv carries an -x/-X style token (redundant with the clean rule on purpose).

    A violation raises rather than returning an error, because every one of them is a
    programming mistake in this file, not a condition to recover from.
    """
    if not argv:
        raise GitGuardViolation("empty git argv")

    subcommand = argv[0]
    if subcommand in FORBIDDEN_GIT_SUBCOMMANDS:
        raise GitGuardViolation(f"refusing to run `git {subcommand}`: forbidden by the wave skill")

    if subcommand == "clean" and tuple(argv) != GIT_CLEAN_ARGV:
        raise GitGuardViolation(
            f"refusing `git {' '.join(argv)}`: clean may only run as `git {' '.join(GIT_CLEAN_ARGV)}`"
        )

    for token in argv:
        if _is_untracked_ignored_flag(token):
            raise GitGuardViolation(
                f"refusing `git {' '.join(argv)}`: that flag reaches gitignored files (.work/)"
            )

    real = _real(clone)
    if real in CANONICAL_REAL:
        raise GitGuardViolation(f"refusing to run git in a canonical tree: {real}")

    return subprocess.run(
        ["git", "-C", str(clone), *argv],
        check=check,
        capture_output=True,
        text=True,
    )


def _is_untracked_ignored_flag(token: str) -> bool:
    """
    True for the flags that would widen a git command onto gitignored files.

    Spelled as a character test rather than a literal list so that a bundled short form
    (-fdx, -fdX) is caught as well as the standalone flag.
    """
    ignored_letters = {"x", "X"}
    if token in {f"-{letter}" for letter in ignored_letters}:
        return True
    if token.startswith("-") and not token.startswith("--"):
        return bool(set(token[1:]) & ignored_letters)
    return False


def _git_clean(clone: Path) -> subprocess.CompletedProcess:
    """
    Step 6. Takes no flag argument BY DESIGN -- there is no parameter through which a caller
    could pass -x, which is why this is a function rather than an inline _git() call.
    """
    return _git(clone, list(GIT_CLEAN_ARGV))


def _git_out(clone: Path, argv: list[str]) -> str:
    return _git(clone, argv).stdout.strip()


# ---------------------------------------------------------------------------------------
# The board.
# ---------------------------------------------------------------------------------------
@dataclass(frozen=True)
class BoardRow:
    index: int  # 0-based index into the board's lines, so a row can be rewritten in place
    raw: str
    path_cell: str
    path: Path
    real: Path
    project: str
    role: str
    branch: str
    session: str
    status: str

    @property
    def name(self) -> str:
        return self.path.name

    @property
    def pool(self) -> str | None:
        return PROJECT_TO_POOL.get(self.project)

    @property
    def is_canonical_row(self) -> bool:
        return "canonical" in self.role.lower()


def _cells(line: str) -> list[str] | None:
    """Split a markdown table row into its cells, or None if the line is not a table row."""
    stripped = line.strip()
    if not stripped.startswith("|") or not stripped.endswith("|"):
        return None
    return [cell.strip() for cell in stripped[1:-1].split("|")]


def parse_board(text: str) -> list[BoardRow]:
    """
    Parse the clone-assignment table out of repos.md.

    Fails closed on anything unexpected. An unreadable or surprising board must never be
    interpreted as an empty one: "no rows" would read as "no clone is in flight", which is the
    fail-open version of the in-flight guard.
    """
    lines = text.splitlines()
    header_index = None
    for i, line in enumerate(lines):
        cells = _cells(line)
        if cells and tuple(cell.lower() for cell in cells) == BOARD_COLUMNS:
            header_index = i
            break

    if header_index is None:
        raise BoardError(
            "no clone-assignment table found: expected a header row "
            f"| {' | '.join(BOARD_COLUMNS)} |"
        )

    separator = _cells(lines[header_index + 1]) if header_index + 1 < len(lines) else None
    if not separator or not all(set(cell) <= set("-: ") and cell for cell in separator):
        raise BoardError("clone-assignment table header is not followed by a separator row")

    rows: list[BoardRow] = []
    for i in range(header_index + 2, len(lines)):
        cells = _cells(lines[i])
        if cells is None:
            break  # the table ends at the first non-table line
        if len(cells) != len(BOARD_COLUMNS):
            raise BoardError(
                f"repos.md line {i + 1} has {len(cells)} cells, expected {len(BOARD_COLUMNS)}: {lines[i]!r}"
            )
        path_cell = cells[0]
        bare = path_cell.strip().strip("`").strip()
        if not bare:
            raise BoardError(f"repos.md line {i + 1} has an empty path cell")
        path = Path(os.path.expanduser(bare))
        rows.append(
            BoardRow(
                index=i,
                raw=lines[i],
                path_cell=path_cell,
                path=path,
                real=_real(path),
                project=cells[1],
                role=cells[2],
                branch=cells[3].strip("`"),
                session=cells[4],
                status=cells[5],
            )
        )

    if not rows:
        raise BoardError("clone-assignment table has no rows")
    return rows


def load_board(board_path: Path) -> tuple[str, list[BoardRow]]:
    try:
        text = board_path.read_text(encoding="utf-8")
    except OSError as exc:
        raise BoardError(f"cannot read {board_path}: {exc}") from exc
    except UnicodeDecodeError as exc:
        raise BoardError(f"cannot decode {board_path} as UTF-8: {exc}") from exc
    return text, parse_board(text)


# ---------------------------------------------------------------------------------------
# Assessment: every guard, evaluated before anything is touched.
# ---------------------------------------------------------------------------------------
@dataclass
class Assessment:
    row: BoardRow
    pool: str | None = None
    branch: str | None = None
    refusals: list[tuple[str, str]] = field(default_factory=list)  # (code, detail)
    remotes: dict[str, str] = field(default_factory=dict)
    canonical_remotes: list[str] = field(default_factory=list)
    dirty: list[str] = field(default_factory=list)
    cu_branches: list[str] = field(default_factory=list)
    current_branch: str | None = None
    current_target_sha: str | None = None  # remote-tracking sha BEFORE any fetch

    @property
    def ok(self) -> bool:
        return not self.refusals

    def refuse(self, code: str, detail: str) -> None:
        self.refusals.append((code, detail))


def assess_static(row: BoardRow, canonical_real: frozenset[Path]) -> Assessment:
    """Guards that need no git and no filesystem writes. Ordered cheapest-and-hardest first."""
    a = Assessment(row=row)

    if row.real in CANONICAL_REAL:
        a.refuse("canonical_tree", f"{row.real} is a canonical tree (hardcoded); never reset")
    if row.real in canonical_real:
        a.refuse("canonical_row", f"repos.md marks {row.path_cell} role={row.role!r}")

    pool = row.pool
    if pool is None or row.role not in SELECTABLE_ROLES:
        a.refuse(
            "not_a_side_clone",
            f"project={row.project!r} role={row.role!r} is not a selectable side clone",
        )
    else:
        a.pool = pool
        a.branch = POOL_DEFAULT_BRANCH[pool]

    status = row.status.strip().lower()
    if status == "in-flight":
        a.refuse("in_flight", "repos.md marks this clone in-flight; a subagent is using it")
    if status == "do-not-use":
        a.refuse("do_not_use", "repos.md marks this clone do-not-use")

    wave = WAVE_TOKEN_RE.search(row.session)
    if wave:
        a.refuse(
            "board_inconsistent",
            f"session cell still carries a wave assignment ({wave.group(0)}) while status is "
            f"{row.status!r}; clear the assignment on the board first",
        )

    if not row.path.is_dir():
        a.refuse("missing_directory", f"{row.path} does not exist")

    return a


def assess_git(a: Assessment) -> Assessment:
    """Read-only git guards. Not reached if a static guard already refused."""
    row = a.row
    try:
        toplevel = _git_out(row.path, ["rev-parse", "--show-toplevel"])
    except (subprocess.CalledProcessError, GitGuardViolation) as exc:
        a.refuse("not_a_git_worktree", f"{row.path}: {exc}")
        return a

    if _real(toplevel) != row.real:
        a.refuse(
            "not_worktree_root",
            f"{row.path} is inside {toplevel}, not its own worktree root",
        )
        return a

    a.current_branch = _git_out(row.path, ["rev-parse", "--abbrev-ref", "HEAD"])

    for line in _git_out(row.path, ["remote"]).splitlines():
        remote = line.strip()
        if not remote:
            continue
        a.remotes[remote] = _git_out(row.path, ["remote", "get-url", remote])

    _check_remote_crossing(a)
    if not a.ok:
        return a

    a.canonical_remotes = _canonical_remote_candidates(a)
    if not a.canonical_remotes:
        a.refuse("no_remote", "clone has no remotes to fetch from")
        return a

    a.dirty = [line for line in _git_out(row.path, ["status", "--porcelain"]).splitlines() if line]
    if a.dirty:
        staged = sum(1 for line in a.dirty if line[:1] not in {" ", "?"})
        a.refuse(
            "dirty",
            f"{len(a.dirty)} uncommitted path(s), {staged} staged -- reporting instead of "
            "resetting over them",
        )
        return a

    a.cu_branches = [
        b.strip()
        for b in _git_out(row.path, ["for-each-ref", "--format=%(refname:short)", f"refs/heads/{CU_BRANCH_PREFIX}*"]).splitlines()
        if b.strip()
    ]

    a.current_target_sha = _existing_remote_tracking_sha(a)
    return a


def _check_remote_crossing(a: Assessment) -> None:
    """
    Refuse a clone whose remote is a local path inside another project's canonical tree.

    This is "IRS clones must never be crossed with Apache remotes" made mechanical, in both
    directions. A crossing cannot originate in this script -- fetches only ever name a remote
    the clone already has, and `git config` never runs, so no remote is ever created or
    repointed here -- but one configured by hand elsewhere would be silently honoured by a
    fetch, so it is refused instead.
    """
    own_canonical = _canonical_for_pool(a.pool)
    for remote, url in a.remotes.items():
        local = _local_remote_path(url)
        if local is None:
            continue
        for pool, canonical in _pool_canonicals().items():
            if local == canonical or canonical in local.parents:
                if own_canonical is None or canonical != own_canonical:
                    a.refuse(
                        "remote_crossing",
                        f"remote {remote!r} -> {url} resolves into the {pool} canonical tree "
                        f"while this clone is in the {a.pool} pool",
                    )
                break


def _pool_canonicals() -> dict[str, Path]:
    """Pool -> canonical tree real path, derived from CANONICAL_TREES by pool default branch."""
    by_name = {p.name: p for p in CANONICAL_TREES}
    return {
        "juneau": _real(by_name["juneau"]),
        "irs": _real(by_name["irs-1"]),
        "console": _real(by_name["sandbox-support-console"]),
    }


def _canonical_for_pool(pool: str | None) -> Path | None:
    return _pool_canonicals().get(pool) if pool else None


def _local_remote_path(url: str) -> Path | None:
    """Real path of a remote URL that names a local directory, else None (an ssh/https URL)."""
    if "://" in url or re.match(r"^[^/]+@[^/]+:", url):
        return None
    candidate = Path(os.path.expanduser(url))
    if not candidate.is_absolute():
        return None
    real = _real(candidate)
    if real.name == ".git":
        real = real.parent
    return real


def _canonical_remote_candidates(a: Assessment) -> list[str]:
    """
    The remotes that could carry the canonical branch, in preference order.

    `origin` always counts (step 2 names it). Console clones also carry a second remote pointing
    at the local canonical console tree, which the skill mentions as "the local-canonical remote
    if present" -- included here so a disagreement between the two is detected rather than
    silently resolved in favour of whichever this script happened to prefer.
    """
    own_canonical = _canonical_for_pool(a.pool)
    candidates = ["origin"] if "origin" in a.remotes else []
    for remote, url in sorted(a.remotes.items()):
        if remote == "origin":
            continue
        local = _local_remote_path(url)
        if local is not None and own_canonical is not None and local == own_canonical:
            candidates.append(remote)
    return candidates


def _existing_remote_tracking_sha(a: Assessment) -> str | None:
    """Current (pre-fetch) sha of <origin>/<branch>, for the dry-run report only."""
    for remote in a.canonical_remotes:
        try:
            return _git_out(a.row.path, ["rev-parse", "--verify", f"{remote}/{a.branch}"])
        except subprocess.CalledProcessError:
            continue
    return None


# ---------------------------------------------------------------------------------------
# .work/ preservation.
# ---------------------------------------------------------------------------------------
@dataclass
class WorkBackup:
    src: Path
    dest: Path
    root: Path
    files: int


def backup_work(clone: Path) -> WorkBackup | None:
    """
    Copy the clone's .work/ out of the tree before anything touches it (the skill's git-safety
    rule: back up .work/ before checkout/reset, restore after).

    The copy lands in the system temp directory -- outside the clone -- so it cannot itself be
    reached by anything running inside the clone.
    """
    src = clone / ".work"
    if not src.is_dir():
        return None
    root = Path(tempfile.mkdtemp(prefix="reset-side-clones-work-"))
    dest = root / ".work"
    shutil.copytree(src, dest, symlinks=True)
    return WorkBackup(src=src, dest=dest, root=root, files=_count_files(dest))


def _count_files(root: Path) -> int:
    return sum(1 for p in root.rglob("*") if p.is_file())


def _relative_files(root: Path) -> list[Path]:
    return sorted(p.relative_to(root) for p in root.rglob("*") if p.is_file())


def restore_work(backup: WorkBackup | None) -> tuple[list[Path], bool]:
    """
    Put back anything under .work/ that did not survive, then verify.

    Returns (restored paths, verified). `git clean -fd` leaves gitignored files alone, so in
    practice nothing needs restoring and this is the check that proves it rather than assuming
    it. The backup directory is deleted only when the post-reset .work/ is byte-identical to it;
    otherwise it is kept and its location reported.
    """
    if backup is None:
        return [], True

    restored: list[Path] = []
    for rel in _relative_files(backup.dest):
        target = backup.src / rel
        if not target.exists():
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(backup.dest / rel, target)
            restored.append(rel)

    verified = _trees_identical(backup.dest, backup.src)
    if verified:
        shutil.rmtree(backup.root, ignore_errors=True)
    return restored, verified


def _trees_identical(a: Path, b: Path) -> bool:
    files_a, files_b = _relative_files(a), _relative_files(b)
    if files_a != files_b:
        return False
    return all(filecmp.cmp(a / rel, b / rel, shallow=False) for rel in files_a)


# ---------------------------------------------------------------------------------------
# The reset itself (steps 2-6) and its verification.
# ---------------------------------------------------------------------------------------
@dataclass
class ResetResult:
    target_ref: str
    target_sha: str
    fetched: list[str]
    deleted_cu: list[str]
    kept_cu: list[str]
    work_restored: list[Path]
    verified_paths: list[str]
    verify_note: str


def reset_clone(a: Assessment) -> ResetResult:
    """Steps 2-6 in the skill's order, then verification. Raises ResetFailed on any problem."""
    clone, branch = a.row.path, a.branch
    assert branch is not None  # guaranteed by assess_static

    backup = backup_work(clone)

    try:
        # Step 2 -- fetch. Only ever a remote name the clone already has; never a URL, so this
        # cannot introduce a remote or cross two projects' remotes.
        fetched = []
        for remote in a.canonical_remotes:
            try:
                _git(clone, ["fetch", remote])
            except subprocess.CalledProcessError as exc:
                raise ResetFailed(f"git fetch {remote} failed: {_stderr(exc)}") from exc
            fetched.append(remote)

        target_ref, target_sha = _resolve_target(a)

        # Step 3 -- checkout the pool's default branch.
        if _branch_exists(clone, branch):
            try:
                _git(clone, ["checkout", branch])
            except subprocess.CalledProcessError as exc:
                raise ResetFailed(f"git checkout {branch} failed: {_stderr(exc)}") from exc
        else:
            try:
                _git(clone, ["checkout", "-b", branch, target_ref])
            except subprocess.CalledProcessError as exc:
                raise ResetFailed(f"git checkout -b {branch} {target_ref} failed: {_stderr(exc)}") from exc

        # Step 4 -- reset --hard to the canonical remote-tracking branch.
        try:
            _git(clone, ["reset", "--hard", target_ref])
        except subprocess.CalledProcessError as exc:
            raise ResetFailed(f"git reset --hard {target_ref} failed: {_stderr(exc)}") from exc

        # Step 5 -- leftover cu/* branches, only when provably safe (see _delete_cu_branches).
        deleted_cu, kept_cu = _delete_cu_branches(a, target_ref, branch)

        # Step 6 -- clean, in its only permitted form.
        try:
            _git_clean(clone)
        except subprocess.CalledProcessError as exc:
            raise ResetFailed(f"git clean failed: {_stderr(exc)}") from exc

    finally:
        work_restored, work_verified = restore_work(backup)

    if backup is not None and not work_verified:
        raise ResetFailed(f".work/ did not survive byte-identical; backup kept at {backup.dest}")

    head = _git_out(clone, ["rev-parse", "HEAD"])
    if head != target_sha:
        raise ResetFailed(f"HEAD is {head}, expected {target_sha}")

    residue = [line for line in _git_out(clone, ["status", "--porcelain"]).splitlines() if line]
    if residue:
        raise ResetFailed(f"working tree still not clean after reset: {residue[:5]}")

    verified_paths, note = verify_content(clone, target_ref)

    return ResetResult(
        target_ref=target_ref,
        target_sha=target_sha,
        fetched=fetched,
        deleted_cu=deleted_cu,
        kept_cu=kept_cu,
        work_restored=work_restored,
        verified_paths=verified_paths,
        verify_note=note,
    )


def _stderr(exc: subprocess.CalledProcessError) -> str:
    """Last meaningful line of a failed git command, for a one-line report."""
    lines = ((exc.stderr or "") + "\n" + (exc.stdout or "")).strip().splitlines()
    return lines[-1].strip() if lines else str(exc)


def _resolve_target(a: Assessment) -> tuple[str, str]:
    """
    The ref to reset to, and its sha.

    A Console clone can have both `origin` (the remote) and a local-canonical remote. The skill
    says to reset to "origin/main or the local-canonical remote's main" without saying which
    wins, so when the two disagree this refuses and reports both shas rather than picking one.
    Choosing silently is exactly the kind of guess that makes a wave boundary untrustworthy.
    """
    seen: dict[str, str] = {}
    for remote in a.canonical_remotes:
        ref = f"{remote}/{a.branch}"
        try:
            seen[ref] = _git_out(a.row.path, ["rev-parse", "--verify", ref])
        except subprocess.CalledProcessError:
            continue

    if not seen:
        raise ResetFailed(
            f"no canonical remote-tracking branch found (looked for "
            f"{', '.join(f'{r}/{a.branch}' for r in a.canonical_remotes)})"
        )

    if len(set(seen.values())) > 1:
        detail = ", ".join(f"{ref}={sha[:10]}" for ref, sha in sorted(seen.items()))
        raise ResetFailed(
            f"canonical remotes disagree on {a.branch} ({detail}); which one is canonical is an "
            "operator decision, so nothing was reset"
        )

    # They agree, so any of them names the same commit; report the one step 4 names first.
    ref = next(f"{remote}/{a.branch}" for remote in a.canonical_remotes if f"{remote}/{a.branch}" in seen)
    return ref, seen[ref]


def _branch_exists(clone: Path, branch: str) -> bool:
    try:
        _git(clone, ["rev-parse", "--verify", f"refs/heads/{branch}"])
        return True
    except subprocess.CalledProcessError:
        return False


def _delete_cu_branches(a: Assessment, target_ref: str, branch: str) -> tuple[list[str], list[str]]:
    """
    Step 5, restricted to the part a script can prove.

    The skill permits deleting a leftover cu/* branch when it is "already harvested". No script
    can determine that -- harvest lands as UNSTAGED files on canonical, so a harvested slice is
    not reachable from any ref. What is mechanically provable is containment: a cu/* branch whose
    tip is an ancestor of the target ref holds nothing unique, so deleting it cannot lose work.
    Anything else is reported and left alone for a human, because `git branch -D` on a branch
    with unique commits is unrecoverable in the way this script exists to prevent.
    """
    deleted: list[str] = []
    kept: list[str] = []
    worktree_branches = _branches_in_other_worktrees(a.row.path)

    for cu in a.cu_branches:
        if cu == branch or cu in worktree_branches:
            kept.append(f"{cu} (checked out)")
            continue
        try:
            _git(a.row.path, ["merge-base", "--is-ancestor", cu, target_ref])
        except subprocess.CalledProcessError:
            kept.append(f"{cu} (has commits not in {target_ref}; cannot prove it was harvested)")
            continue
        try:
            _git(a.row.path, ["branch", "-d", cu])
        except subprocess.CalledProcessError as exc:
            kept.append(f"{cu} (delete refused: {_stderr(exc)})")
            continue
        deleted.append(cu)

    return deleted, kept


def _branches_in_other_worktrees(clone: Path) -> set[str]:
    out = _git_out(clone, ["worktree", "list", "--porcelain"])
    return {
        line.split(" ", 1)[1].strip().removeprefix("refs/heads/")
        for line in out.splitlines()
        if line.startswith("branch ")
    }


def verify_content(clone: Path, target_ref: str) -> tuple[list[str], str]:
    """
    Compare real file CONTENT against the canonical blob, not the commit hash.

    `git hash-object` re-hashes the bytes actually on disk; `git rev-parse <ref>:<path>` is the
    blob id the fetched canonical commit records for that path. Equal ids mean the file on disk
    IS the canonical file. A matching HEAD proves only that a ref moved, which is why the manual
    version of this check was what made the nine-clone report trustworthy.

    SENTINEL_PATHS are used when the target ref carries them. When it carries none, a
    deterministic sample of the tree is used instead -- "there was nothing to verify" must never
    be reportable as a pass.
    """
    present = [p for p in SENTINEL_PATHS if _blob_id(clone, target_ref, p) is not None]
    note = "declared sentinels"
    if not present:
        listing = _git_out(clone, ["ls-tree", "-r", "--name-only", target_ref]).splitlines()
        present = sorted(listing)[:SENTINEL_SAMPLE_SIZE]
        note = f"no declared sentinel in {target_ref}; deterministic sample of the tree"

    if not present:
        raise ResetFailed(f"{target_ref} is empty: nothing could be content-verified")

    for path in present:
        expected = _blob_id(clone, target_ref, path)
        actual = _git_out(clone, ["hash-object", "--", path])
        if expected != actual:
            raise ResetFailed(
                f"content mismatch for {path}: on disk {actual[:10]}, canonical {str(expected)[:10]}"
            )
    return present, note


def _blob_id(clone: Path, ref: str, path: str) -> str | None:
    try:
        return _git_out(clone, ["rev-parse", "--verify", f"{ref}:{path}"])
    except subprocess.CalledProcessError:
        return None


# ---------------------------------------------------------------------------------------
# Step 7 -- the board row.
# ---------------------------------------------------------------------------------------
def render_row(raw: str, branch: str, status: str) -> str:
    """
    Rewrite one board row's branch / session / status cells, leaving every other byte alone.

    Operates on the original line's own `|` split so the surrounding prose, the other columns,
    and the file's spacing style are untouched.
    """
    parts = raw.split("|")
    if len(parts) != len(BOARD_COLUMNS) + 2:
        raise BoardError(f"cannot rewrite malformed row: {raw!r}")
    parts[4] = f" `{branch}` "
    parts[5] = " "
    parts[6] = f" {status} "
    return "|".join(parts)


def write_board(board_path: Path, updates: dict[int, str]) -> None:
    """
    Install row rewrites atomically, refusing anything that would mangle the board.

    Concurrency: a sibling lock file created with O_CREAT|O_EXCL. A held lock is NOT stolen and
    no stale-lock heuristic is applied -- the caller reports the needed change instead, because a
    mangled shared board is worse than a row someone has to fix by hand.

    Safety: the rewritten text is re-parsed and compared line by line against the original, and
    the write is refused unless exactly the intended lines changed.
    """
    lock = board_path.with_name(board_path.name + ".lock")
    try:
        fd = os.open(str(lock), os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o644)
    except FileExistsError as exc:
        raise BoardError(f"{lock} is held by another writer; not touching the board") from exc
    try:
        os.write(fd, f"{os.getpid()}\n".encode())
        os.close(fd)

        original = board_path.read_text(encoding="utf-8")
        lines = original.splitlines(keepends=True)
        for index, new_line in updates.items():
            if index >= len(lines):
                raise BoardError("board changed under us: row index out of range")
            ending = "\n" if lines[index].endswith("\n") else ""
            lines[index] = new_line + ending

        updated = "".join(lines)
        _assert_only_intended_lines_changed(original, updated, set(updates))
        parse_board(updated)  # a rewrite that no longer parses is never installed

        tmp = board_path.with_name(board_path.name + f".tmp.{os.getpid()}")
        tmp.write_text(updated, encoding="utf-8")
        os.replace(tmp, board_path)
    finally:
        lock.unlink(missing_ok=True)


def _assert_only_intended_lines_changed(original: str, updated: str, intended: set[int]) -> None:
    before, after = original.splitlines(), updated.splitlines()
    if len(before) != len(after):
        raise BoardError("refusing to write: line count changed")
    changed = {i for i, (b, a) in enumerate(zip(before, after)) if b != a}
    if not changed <= intended:
        raise BoardError(f"refusing to write: unintended lines would change: {sorted(changed - intended)}")


# ---------------------------------------------------------------------------------------
# Selection.
# ---------------------------------------------------------------------------------------
def select_rows(rows: list[BoardRow], *, all_pools: bool, pools: list[str], names: list[str]) -> list[BoardRow]:
    """Resolve the requested selection to board rows. Raises SystemExit(4) on a usage error."""
    selected: dict[int, BoardRow] = {}

    wanted_pools = list(POOLS_IN_ALL) if all_pools else []
    for pool in pools:
        if pool not in POOL_DEFAULT_BRANCH:
            _usage_error(f"unknown pool {pool!r}; known pools: {', '.join(sorted(POOL_DEFAULT_BRANCH))}")
        if pool not in wanted_pools:
            wanted_pools.append(pool)

    for row in rows:
        if row.pool in wanted_pools and row.role in SELECTABLE_ROLES:
            selected[row.index] = row

    for name in names:
        matches = [r for r in rows if r.name == name or r.real == _real(name) or r.path_cell.strip("`") == name]
        if not matches:
            _usage_error(
                f"{name!r} is not a row in the board. Only clones the board knows about can be "
                "selected; add it there first."
            )
        if len(matches) > 1:
            _usage_error(f"{name!r} matches {len(matches)} board rows; use the full path")
        selected[matches[0].index] = matches[0]

    return [selected[i] for i in sorted(selected)]


def _usage_error(message: str) -> None:
    print(f"error: {message}", file=sys.stderr)
    raise SystemExit(4)


# ---------------------------------------------------------------------------------------
# Reporting.
# ---------------------------------------------------------------------------------------
def print_plan(a: Assessment) -> None:
    row = a.row
    print(f"\n{row.name}  ({row.path})  pool={a.pool}")
    if not a.ok:
        for code, detail in a.refusals:
            print(f"  REFUSED [{code}] {detail}")
        if a.dirty:
            for line in a.dirty[:10]:
                print(f"    {line}")
            if len(a.dirty) > 10:
                print(f"    ... {len(a.dirty) - 10} more")
        return

    target = f"{a.canonical_remotes[0]}/{a.branch}"
    sha = a.current_target_sha[:10] if a.current_target_sha else "(unknown until fetched)"
    print(f"  on branch:  {a.current_branch}   clean")
    for remote in a.canonical_remotes:
        print(f"  would run:  git -C {row.name} fetch {remote}")
    print(f"  would run:  git -C {row.name} checkout {a.branch}")
    print(f"  would run:  git -C {row.name} reset --hard {target}     (currently {sha}, re-resolved after fetch)")
    print(f"  would run:  git -C {row.name} {' '.join(GIT_CLEAN_ARGV)}     (never -x; .work/ preserved)")
    if a.cu_branches:
        print(f"  cu/* branches:  {', '.join(a.cu_branches)}  (deleted only if contained in the target ref)")
    else:
        print("  cu/* branches:  none")
    work = row.path / ".work"
    print(f"  .work/:  {'backed up and verified after' if work.is_dir() else 'absent'}")
    print(f"  would verify content of:  {', '.join(SENTINEL_PATHS)}  (blob hash vs canonical)")
    print(f"  would set board row:  branch=`{a.branch}`  session=(empty)  status=idle")
    if row.session.strip():
        print(f"  NOTE: clearing a non-empty session cell: {row.session.strip()!r}")


def print_result(a: Assessment, result: ResetResult) -> None:
    row = a.row
    print(f"\n{row.name}  ({row.path})  pool={a.pool}")
    print(f"  fetched:   {', '.join(result.fetched)}")
    print(f"  reset to:  {result.target_ref}  {result.target_sha[:10]}")
    print(f"  cleaned:   git {' '.join(GIT_CLEAN_ARGV)}")
    print(f"  .work/:    preserved and verified"
          f"{f' ({len(result.work_restored)} entries restored)' if result.work_restored else ''}")
    if result.deleted_cu:
        print(f"  cu/* deleted (contained in target): {', '.join(result.deleted_cu)}")
    for kept in result.kept_cu:
        print(f"  cu/* KEPT for a human: {kept}")
    print(f"  content verified ({result.verify_note}): {', '.join(result.verified_paths)}")


# ---------------------------------------------------------------------------------------
def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="reset-side-clones.py",
        description="Reset harvested WAVE side clones to a fresh default branch (dry run unless --apply).",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=f"Pools: {', '.join(sorted(POOL_DEFAULT_BRANCH))}. --all covers {', '.join(POOLS_IN_ALL)}.",
    )
    parser.add_argument("--all", action="store_true", help=f"every side clone in the {', '.join(POOLS_IN_ALL)} pools")
    parser.add_argument("--pool", action="append", default=[], metavar="NAME", help="pool to reset (repeatable)")
    parser.add_argument("--clones", action="append", default=[], metavar="LIST", help="comma-separated board names (repeatable)")
    parser.add_argument("--apply", action="store_true", help="actually perform the reset (default: dry run)")
    parser.add_argument("--board", type=Path, default=BOARD_DEFAULT, help=f"repos.md to read (default: {BOARD_DEFAULT})")
    parser.add_argument("--no-board-update", action="store_true", help="report the repos.md row change instead of writing it")
    args = parser.parse_args(argv)

    names = [n.strip() for group in args.clones for n in group.split(",") if n.strip()]
    if not (args.all or args.pool or names):
        _usage_error("nothing selected; pass --all, --pool <name>, or --clones <list>")

    # Fail closed on the board BEFORE anything else. An unreadable board must never be read as
    # "nothing is in flight".
    try:
        _, rows = load_board(args.board)
    except BoardError as exc:
        print(f"error: {exc}", file=sys.stderr)
        print("Nothing was touched. The board is the in-flight guard; it must be readable.", file=sys.stderr)
        return 2

    canonical_real = frozenset(r.real for r in rows if r.is_canonical_row)
    selection = select_rows(rows, all_pools=args.all, pools=args.pool, names=names)

    mode = "APPLY" if args.apply else "DRY RUN (no flag given; nothing will be changed)"
    print(f"reset-side-clones: {mode}")
    print(f"board: {args.board}")
    if args.all and "irs" not in args.pool:
        print("note: --all covers the juneau and console pools. IRS clones are excluded; pass "
              "--pool irs to include them (the skill keeps IRS a separate hat).")
    if not selection:
        print("no clones selected.")
        return 0

    assessments = []
    for row in selection:
        a = assess_static(row, canonical_real)
        if a.ok:
            a = assess_git(a)
        assessments.append(a)

    refused = [a for a in assessments if not a.ok]
    actionable = [a for a in assessments if a.ok]

    if not args.apply:
        for a in assessments:
            print_plan(a)
        print(f"\n{len(actionable)} clone(s) would be reset, {len(refused)} refused. "
              f"Re-run with --apply to act.")
        return 1 if refused else 0

    for a in refused:
        print_plan(a)

    failures: list[tuple[Assessment, str]] = []
    board_updates: dict[int, str] = {}
    succeeded: list[Assessment] = []

    for a in actionable:
        try:
            result = reset_clone(a)
        except (ResetFailed, GitGuardViolation) as exc:
            print(f"\n{a.row.name}  ({a.row.path})")
            print(f"  FAILED: {exc}")
            failures.append((a, str(exc)))
            continue
        print_result(a, result)
        succeeded.append(a)
        assert a.branch is not None
        board_updates[a.row.index] = render_row(a.row.raw, a.branch, "idle")

    board_deferred = False
    if board_updates and not args.no_board_update:
        try:
            write_board(args.board, board_updates)
            print(f"\nboard: updated {len(board_updates)} row(s) in {args.board}")
        except BoardError as exc:
            board_deferred = True
            print(f"\nboard: NOT updated -- {exc}")
    elif board_updates:
        board_deferred = True
        print("\nboard: --no-board-update given; not written")

    if board_deferred:
        print("Apply these rows by hand (step 7):")
        for index in sorted(board_updates):
            print(f"  line {index + 1}: {board_updates[index]}")

    print(f"\nsummary: {len(succeeded)} reset, {len(refused)} refused, {len(failures)} failed.")

    if failures:
        return 3
    if board_deferred:
        return 5
    if refused:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
