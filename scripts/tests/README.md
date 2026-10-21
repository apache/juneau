# scripts/tests/

Pytest harness for the juneau-only `scripts/` Python that has no other automated coverage:
`push.py`'s tracker-audit gate, the `--js-tests` flags, and `repin-consumers.py`. (The side-clone reset and
wave-survey scripts and their tests live in the board repo's `scripts/`.) Everything here is hermetic -- temp directories, synthetic fixture files, and
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

The suite must also pass under a push-like environment (`JUNEAU_RUN_SESSION`, `JUNEAU_PUSH_ACTIVE`, `JUNEAU_RUN_ACTIVE`, `JUNEAU_RUN_N_BASE` set), because `push.py`'s Python tests step runs it that way; any test that depends on those variables being unset must clear them itself.

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

- `test_js_tests_flags.py` -- the `--js-tests` / `--no-js-tests` support in `scripts/test.py` and
  `scripts/push.py`: flag parsing (incl. conflict rejection and push.py forwarding the
  flags to test.py), auto-detect of changed `.js`/`.css`/`.ftl` files under `src/` (driven by a fake
  changed-file list, no git), and the Node/npm-missing path (skip with notice when auto-enabled, fail
  when explicit). Never runs mvn or push.py's real flow.

- `test_test_py_run_command.py` -- `scripts/test.py`'s `run_command()` streams each output line to
  the console as it arrives (stderr merged in) instead of buffering until the command exits, and
  still returns the full output and exit code. The second test proves a line is echoed while the
  command is still running. Uses trivial shell commands, never mvn.

- `test_run_adoption.py` -- `push.py` and `test.py` on juneau-run: marker streams from a fake `mvn` on `PATH`,
  `push.py --test-only`, console routing, the skipped second install, the commit summary, the Timing row, the
  nested `test.py` handoff and `--detail`. It also pins the default (markers off) behaviour.

- `test_test_py_console.py` -- `test.py`'s console session: `--detail`, the PERF-GUARD notes and their levels under
  `--enforce-perf`, and `-f` on the JS-test command.

- `test_push_timings.py` -- `push-timings.py`'s thresholds: a module is flagged only when it is slower by both the
  relative and the absolute margin, faster modules get the `↓` mark, the env overrides, and the `--report` table
  file. Synthetic run-history records in a temp dir.

- `test_release_run.py` -- `release.py` on juneau-run and juneau_scripts, with a fake juneau_run module installed in
  `sys.modules` (where release.py and the toolkit both look it up): every prompt goes through `prompt.text` (Ctrl-D
  cancels, never takes the default), Maven gets `-B` and a PTY, the full log is always written, the steps are a
  `steps.Registry` (its CLI, resume and checkpoint, with a legacy `last_step` file still resuming), each step is a
  labelled row (dim when skipped or done earlier), `run_git_diff` follows
  `run_release_prepare` and pages through `show`, and the revert is four rows. `HOME` is a temp dir, so the state
  file and logs never touch yours. Never runs mvn, svn or gpg.

Shared fixtures in `conftest.py`: `run_history` (F1) turns run history on for one test under `tmp_path/history`,
with `argv[0]` set so the writer's script name is `test`; `fake_reports` (F2) builds a scratch Surefire tree; and
`history_record` (F3) builds one run-history record.

- `test_repin_consumers.py` -- `repin-consumers.py` re-pins the *other* two repos (Release
  Manager, Support Console) to a fresh local Juneau build, so it lives and is tested only here.
  Covers the script's whole reason to exist -- the dirty-tree guard that refuses to `mvn
  install` Juneau into the shared `~/.m2` unless `git status --porcelain` is empty (modified
  tracked files, staged-only changes, and untracked non-ignored files are each proven to trip
  it; a gitignored untracked file is proven not to) -- plus the small pure helpers around it
  (pom `<version>` parsing, HEAD-vs-`origin/master` comparison) and `main()`'s exit-code
  contract. The refusal path is proven by monkeypatching `run_streaming` to raise if it is ever
  called, so "refused" means "mvn was never invoked", not just "the exit code was 1" -- the same
  style `test_push.py` uses. Built against real temporary git
  repositories for the same reason that one is: the guard's entire risk is real `git status
  --porcelain` behavior, which a mocked subprocess would let pass while proving nothing. Never
  runs a real `mvn` command.
