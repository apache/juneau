# scripts/tests/

Pytest harness for the juneau-only `scripts/` Python that has no other automated coverage:
`push.py`'s tracker-audit gate, `reset-side-clones.py`, `repin-consumers.py`, and
`wave-survey.py`. Everything here is hermetic -- temp directories, synthetic fixture files, and
real temporary git repositories built in fixtures -- and never touches the real
`~/Project Work` trackers, the real `repos.md`, or the real side clones.

**Consolidated 2026-08-30**: `todo-next-id.py`, `todo-status-audit.py`, and their shared tests
(`conftest.py`, `test_todo_next_id.py`, `test_todo_status_audit.py`) used to be carried
byte-for-byte-identical in this repo, `apache/release-manager`, and `sandbox-support-console`.
They have moved to a single parameterized copy at `~/Project Work/scripts/` (`--project
{juneau,release-manager,sandbox-support-console,irs}`), with their tests alongside at
`~/Project Work/scripts/tests/`. See `@todo-and-waves` and `~/agents/AGENTS.md`. Nothing in
*this* directory needs to stay in sync with the other two repos anymore -- every file that
remains here is a juneau-only script with no counterpart elsewhere.

## Running

`pytest` is not vendored anywhere in this repo (it's pure-stdlib elsewhere in `scripts/` on
purpose). Two easy ways to run this suite without installing anything globally:

```bash
# Option A -- uv (fastest, no setup, nothing left behind):
uv run --with pytest pytest scripts/tests

# Option B -- a local venv:
python3 -m venv .venv
.venv/bin/pip install pytest
.venv/bin/pytest scripts/tests
```

Do **not** `pip install --user pytest` / `pip install pytest` on a Homebrew-managed
`python3` -- PEP 668 ("externally managed environment") will refuse it (or force
`--break-system-packages`, which this repo's tooling should not depend on).

## Layout

- `test_push.py` -- covers the `--tracker-audit` opt-in gate in `scripts/push.py`, which only
  exists in juneau's copy (release-manager's `push.py` is a bare add/commit/push helper with no
  gates at all; sandbox-support-console has no `push.py`). Its central assertion is that the
  off path never reaches `subprocess.run` at all -- not just that it returns early -- which is
  the strongest available proof the gate cannot affect push behavior, including runtime, while
  disabled.

- `test_reset_side_clones.py` -- `reset-side-clones.py` is not a per-project script. It reads
  the one global `~/Project Work/repos.md` and drives clones by absolute path, so a single copy
  resets the juneau, console, and IRS pools and there is nothing for a second copy to
  specialize. The tests are about the guards, since that is where the script's value is:
  canonical-tree refusal by resolved real path (including via a symlink and via a `..` path),
  `in-flight` refusal, dirty/staged/untracked abort, fail-closed behavior on an unreadable or
  unparseable board, and the unreachability of `git clean -x` and of `git config`. Those two
  are asserted four ways -- the argv constant, a helper with no flag parameter at all, the
  wrapper refusing every other spelling, and an audit of every argv from a real end-to-end
  apply. Refusals are proven by monkeypatching `subprocess.run` to raise, so "it refused" means
  "no git ran" rather than "the exit code was 1". Everything else builds real temporary git
  repositories and lets real git run against them: a mocked test of a script whose entire risk
  is real git behavior would prove almost nothing. Temp repos get their identity and config
  isolation from environment variables, never `git config`, which is the same rule the script
  enforces.

- `test_repin_consumers.py` -- `repin-consumers.py` re-pins the *other* two repos (Release
  Manager, Support Console) to a fresh local Juneau build, so it lives and is tested only here.
  Covers the script's whole reason to exist -- the dirty-tree guard that refuses to `mvn
  install` Juneau into the shared `~/.m2` unless `git status --porcelain` is empty (modified
  tracked files, staged-only changes, and untracked non-ignored files are each proven to trip
  it; a gitignored untracked file is proven not to) -- plus the small pure helpers around it
  (pom `<version>` parsing, HEAD-vs-`origin/master` comparison) and `main()`'s exit-code
  contract. The refusal path is proven by monkeypatching `run_streaming` to raise if it is ever
  called, so "refused" means "mvn was never invoked", not just "the exit code was 1" -- the same
  style `test_push.py` and `test_reset_side_clones.py` use. Built against real temporary git
  repositories for the same reason those two are: the guard's entire risk is real `git status
  --porcelain` behavior, which a mocked subprocess would let pass while proving nothing. Never
  runs a real `mvn` command.

- `test_wave_survey.py` -- covers `wave-survey.py`, the cross-project wave-candidate survey.
  Deliberately self-contained: it loads its own module under test rather than sharing
  `conftest.py` fixtures with the tracker tests, and does not depend on any of the (now-moved)
  tracker scripts' behavior.
