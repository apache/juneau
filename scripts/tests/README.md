# scripts/tests/

This repo's `scripts/todo-next-id.py` and `scripts/todo-status-audit.py` (and their tests,
`conftest.py` / `test_todo_next_id.py` / `test_todo_status_audit.py`) were consolidated
2026-08-30 into a single parameterized copy (`--project
release-manager`) in the shared tracker scripts, which live outside this repo, with tests alongside them. See
`@todo-and-waves` and the operator's agent instructions.

This repo's `scripts/push.py` (moved here from the repo root, matching `apache/juneau` and
`sandbox-support-console`) runs the build and test gates before it commits: Prechecks, Compile (`mvn clean test-compile`), Java Tests (`mvn verify`), Python Tests
(this suite), Browser Tests (`npx playwright test` in `e2e/`), Commit, Push. The suite never writes the real run history:
`conftest.py` points `JUNEAU_RUN_HISTORY_DIR` at a temp dir and sets `JUNEAU_RUN_HISTORY=0`. Its review gate refuses to commit or push **anything** when the working tree has
any unstaged modification to a tracked file, or any untracked file that isn't gitignored (`_prechecks()` /
`commit_and_push()`, built on the `juneau_scripts` toolkit's `git`, `run` and `tools` modules, loaded through
the vendored `scripts/bootstrap.py`, which needs juneau-run 10.0.0-SNAPSHOT in `~/.m2` -- build Juneau first;
the tests drive `push.main(argv, repo_dir)` and read its exit code).
A staged path is the operator's record of having reviewed it, so `push.py` commits the index
exactly as it finds it and never runs `git add`. `test_push.py` covers the three refusal
shapes (nothing-staged-while-dirty, staged+unstaged-tracked, staged+untracked-non-ignored)
plus the allowed staged-only case, the genuine-clean-tree no-op, and `--dry-run`'s real-outcome
reporting, against real temporary git repositories (a bare repo standing in for the remote). It also
covers the gates: step order, the skip flags, `--test-only`, `--dry-run`'s "would run", and each gate's
command and environment. Maven and Playwright are stubbed in these tests; only a real
`python3 scripts/push.py --test-only` runs them.

This repo's `push.py` is its own thing — deliberately **not** carried byte-for-byte to
`apache/juneau` or `sandbox-support-console`. Those two repos have their own, independently
maintained `push.py` and `test_push.py` covering the same underlying gate (same three refusal
shapes) plus their own repo-specific machinery (SSC's rebase and push lock, Juneau's reactor install) that
this repo's does not need; this repo also has no `reset-side-clones.py` or `repin-consumers.py` of its own
(juneau's `test_reset_side_clones.py` / `test_repin_consumers.py` cover those, and stay in
juneau since the scripts they test do too).

## Script docs checker

`test_script_docs.py` keeps `scripts/` documented. It is read-only: it parses files and never runs a script. It fails when an entry point (a `*.sh`, an extensionless file with a shell or Python shebang, or a `*.py` with a top-level `if __name__ == "__main__":`) has no `<name>.md` beside it; when a script doc (H1 `# <name>.py`) has no script beside it; when a script doc lacks the seven headings in order (When to use, Usage, Options, What it does, Output and exit codes, Gotchas, See also); and when a flag the script declares (found with `ast`, or the option `case` in shell) is not in backticks in the doc's Options section. An Options row for a flag the code no longer declares is only a warning. `tests/`, `vendor/`, `node_modules/`, `fixtures/`, `__pycache__/` and dot-directories are skipped.

`scripts/script-docs-exclude.txt` holds the exceptions, one per line: `<pattern>  # reason` skips an entry point (fnmatch, relative to `scripts/`), `flags: <pattern>  # reason` skips only the flag check (needed when argparse flags are added dynamically, reported as `FLAGS-UNVERIFIABLE`), and `packages: a, b` lists library directories that must have a `README.md` (here `jrm_release`). Every path and `flags:` line needs a reason, and a pattern that matches nothing fails the test.

## Running

`pytest` is not vendored anywhere in this repo. Two easy ways to run this suite without
installing anything globally:

```bash
# Option A -- uv (fastest, no setup, nothing left behind):
uv run --with pytest pytest scripts/tests/test_push.py

# Option B -- a local venv:
python3 -m venv .venv
.venv/bin/pip install pytest
.venv/bin/pytest scripts/tests/test_push.py
```

Do **not** `pip install --user pytest` / `pip install pytest` on a Homebrew-managed `python3`
— PEP 668 ("externally managed environment") will refuse it.

## Release CLI tests

`test_release_*.py` cover `scripts/release.py` and the `scripts/jrm_release/` package. They use fakes only
(`conftest.py`: FakeRunner, FakePrompts, FakeNexus, FakeHttp), so no network, Nexus, SVN, GitHub, gpg key or
real release is touched.

| File | Covers |
|---|---|
| `test_release_runfile.py` | the `run.json` checkpoint (atomic write, corrupt-file refusal) |
| `test_release_steps.py` | each rc, vote and promote step against the fakes |
| `test_release_emails.py` | vote, result and announce emails, `mailto:` length handling |
| `test_release_revert.py` | `--revert` checks and actions |
| `test_release_cli.py` | arguments, guards, lock, resume, exit codes (needs `JUNEAU_RUN_PY`) |
| `test_release_dryrun.py` | `--dry-run` runs read-only commands only and never writes `run.json` |
| `test_release_secrets.py` | no secret on argv, in `run.json` or in logs; `askpass.py` |

`test_release_cli.py` loads the shared toolkit through `scripts/bootstrap.py`, so point it at the Juneau checkout:

```bash
export JUNEAU_RUN_PY=$HOME/git/apache/juneau/juneau-run/src/main/python/juneau_run.py
uv run --offline --with pytest pytest -q -p no:cacheprovider scripts/tests/
```
