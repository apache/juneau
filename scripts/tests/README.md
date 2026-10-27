# scripts/tests/

Pytest harness for the juneau-only `scripts/` Python that has no other automated coverage:
`push.py`'s tracker-audit gate, the `--js-tests` flags, and `repin-consumers.py`. (The side-clone reset and
wave-survey scripts and their tests live in the board repo's `scripts/`.) Everything here is hermetic -- temp directories, synthetic fixture files, and
real temporary git repositories built in fixtures -- and never touches the real
`~/Project Work` trackers, the real `repos.md`, or the real side clones.

The shared todo tracker scripts and their tests no longer live in this directory. Every file that
remains here is a juneau-only script with no counterpart elsewhere.

## Running

The suite must also pass under a push-like environment (`JUNEAU_TERMINAL_SESSION`, `JUNEAU_PUSH_ACTIVE`, `JUNEAU_TERMINAL_ACTIVE`, `JUNEAU_TERMINAL_N_BASE` set), because `push.py`'s Python Tests step runs it that way. `conftest.py` strips every live-run `JUNEAU_TERMINAL_*` variable (`LIVE_RUN_ENV`: logs, cancel, header, markers, result, session and pending/overall rows) before each test, so a test never writes into the real run's logs (`test_env_isolation.py`); a test that needs one sets it itself.

`pytest` is not vendored anywhere in this repo (it's pure-stdlib elsewhere in `scripts/` on
purpose). Two easy ways to run this suite without installing anything globally:

```bash
# Option A -- uv (fastest, no setup, nothing left behind):
uv run --with pytest --with pyyaml pytest scripts/tests

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
  disabled. It also covers the workflow-YAML guard (`TestWorkflowYamlGuard`), which needs PyYAML:
  run it with `uv run --with pytest --with pyyaml pytest scripts/tests/test_push.py`.

- `test_js_tests_flags.py` -- the `--js-tests` / `--no-js-tests` support in `scripts/test.py` and
  `scripts/push.py`: flag parsing (incl. conflict rejection and push.py forwarding the
  flags to test.py), auto-detect of changed `.js`/`.css`/`.ftl` files under `src/` (driven by a fake
  changed-file list, no git), and the Node/npm-missing path (skip with notice when auto-enabled, fail
  when explicit). Never runs mvn or push.py's real flow.

- `test_test_py_run_command.py` -- `scripts/test.py`'s `run_command()` streams each output line to
  the console as it arrives (stderr merged in) instead of buffering until the command exits, and
  still returns the full output and exit code. The second test proves a line is echoed while the
  command is still running. Uses trivial shell commands, never mvn.

- `test_run_adoption.py` -- `push.py` and `test.py` on juneau-terminal: marker streams from a fake `mvn` on `PATH`,
  `push.py --test-only`, console routing, the skipped second install, the commit summary, the Timing row, the
  nested `test.py` handoff and `--detail`. It also pins the default (markers off) behaviour.

- `test_test_py_console.py` -- `test.py`'s console session: `--detail`, the PERF-GUARD notes and their levels under
  `--enforce-perf`, and `-f` on the JS-test command.

- `test_push_timings.py` -- `push-timings.py`'s thresholds: a module is flagged only when it is slower by both the
  relative and the absolute margin, faster modules get the `↓` mark, the env overrides, and the `--report` table
  file. Synthetic run-history records in a temp dir.

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

- `test_script_docs.py` -- the script-docs checker. Every runnable script under `scripts/` (a `.py`
  file with a `__main__` guard, a `.sh` file, or an extensionless file with a shell or Python
  shebang) needs a `<name>.md` beside it with the seven standard sections (When to use, Usage,
  Options, What it does, Output and exit codes, Gotchas, See also), and every flag the script
  declares must appear in backticks in the doc's Options section. It reads source with `ast` and
  text matching and never runs a script. Flags come from literal `add_argument` and `add_parser`
  calls, or from the option `case` block of a shell script; an argparse script with no literal
  calls fails as `FLAGS-UNVERIFIABLE` unless excluded. A doc listing a flag the script does not
  declare only warns. Exceptions go in `scripts/script-docs-exclude.txt`, one per line as
  `pattern  # reason` (reason required), with a `flags:` prefix to skip only the flag check and a
  `packages:` prefix for library directories that need a `README.md` instead; a pattern that
  matches nothing fails. Tests, vendor, node_modules, `__pycache__` and fixtures directories are
  never scanned. The checker also unit-tests its own parsers and proves each check fails when
  broken.
