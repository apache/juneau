# push.py

Runs the build and test gates for the Release Manager, then commits the already-staged index and pushes it to the branch's upstream.

## When to use

- You have staged your reviewed changes and want them verified, committed and pushed in one command.
- You want only the gates, with no commit or push (works on a dirty tree): use `--test-only`.
- You want to see what would happen without changing anything: use `--dry-run`.
- To run only the Python suite, use pytest directly (see `tests/README.md`).

## Usage

```bash
python3 scripts/push.py "Fix credential validator NPE"
python3 scripts/push.py
python3 scripts/push.py --dry-run "Would-be commit message"
python3 scripts/push.py --test-only
python3 scripts/push.py --skip-e2e "Doc-only change"
python3 scripts/push.py --console full "Fix credential validator NPE"
```

Run it from the repository root. The commit message is prompted for when omitted (not used with `--test-only`). There is no root-level `push.py`.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `message` | text (optional positional) | prompted | Commit message. An empty or aborted prompt ends the run with nothing committed. |
| `--dry-run` | none | off | Show what would happen without changing anything. Compile, Java Tests and Browser Tests are shown as "would run", not executed; Prechecks and Python Tests do run (they only read) and a failure is reported as "would refuse". |
| `--test-only` | none | off | Run the gates only: no Prechecks, no commit, no push. |
| `--skip-tests` | none | off | Skip Compile and Java Tests (the Maven build). |
| `--skip-e2e` | none | off | Skip Browser Tests (the Playwright suite). |
| `--skip-pytest` | none | off | Skip Python Tests (the `scripts/tests` suite). |
| `--console` | `full`, `board`, `none` | `board` | Console view of the steps; also `JUNEAU_TERMINAL_CONSOLE`. The flag wins over the environment. |

## What it does

1. Parses the arguments and loads the `juneau_terminal_scripts` toolkit through `bootstrap.py`, applying `--console` before `juneau_terminal` loads.
2. Prechecks (skipped by `--test-only`): one row of four checks that all run, none stopping at the first failure: git identity, unreviewed changes (any unstaged modification to a tracked file, or any untracked file that is not gitignored), staged (nothing staged ends the run with "nothing to do", before any gate), and upstream (fetches; refuses when the branch is behind its upstream; no upstream proceeds; a fetch error fails closed).
3. Compile: `mvn clean test-compile`. Java Tests: `mvn verify` (the Java tests plus Apache RAT). Never `install`, since this repo only consumes the Juneau snapshot already in `~/.m2`. Both are removed by `--skip-tests`.
4. Python Tests: the `scripts/tests` pytest suite, so a push cannot ship a broken release CLI. The launcher is `python3 -m pytest` when pytest is importable, else `uv run --native-tls --with pytest pytest` when `uv` is on the `PATH`, else the step is skipped with a notice. Removed by `--skip-pytest`.
5. Browser Tests: `npx playwright test` in `e2e/`, whose web server boots the app itself. Skipped with a warning when `npx` is not on the `PATH`. Removed by `--skip-e2e`.
6. A failing gate refuses the push; nothing is committed.
7. Commits the staged index exactly as found (message on stdin) and pushes to the tracked upstream (setting it with `-u` when the branch has none). It never stages anything.

## Output and exit codes

Progress is a board view (rows: Prechecks, Compile, Java Tests, Python Tests, Browser Tests, Commit, Push). The full log path is printed in the session header. On success it prints the branch, short commit and remote URL. When a run ends on a terminal it rings the bell.

| Code | Meaning |
|---|---|
| 0 | Pushed, nothing to commit, `--dry-run` report, or `--test-only` passed. |
| 1 | A precheck or gate failed, the commit or push failed, or the toolkit could not be loaded. |
| 2 | Argparse rejected the arguments, or loading the toolkit raised `ValueError`. |

## Gotchas

- It never runs `git add`, `git config`, `--force` or `--no-verify`. A staged path is treated as reviewed, so any unstaged tracked change or untracked non-ignored file makes the review gate refuse, with no override. Use `git stash push -- <paths>` for genuinely partial work.
- A fully clean tree is a benign no-op and runs no gate.
- Needs `juneau-terminal` 10.0.0-SNAPSHOT in `~/.m2` (build Juneau first).
- `JRM_PUSH_NO_BELL=1` silences the bell (it is also skipped when `SANDBOX_RUNTIME=1`).
- `JAVA_HOME` defaults to `~/jdk/default` when unset and that directory exists.
- `JRM_E2E_REUSE`, `JRM_E2E_BASE_URL` and `JRM_E2E_HOME` are removed from the Playwright environment; `PLAYWRIGHT_HTML_OPEN` defaults to `never`.
- `JRM_SCRIPT_TESTS_ACTIVE` is set in the pytest child and the step is skipped when it is already set, so tests that call `main()` cannot recurse. `JUNEAU_TERMINAL_PY` is exported to the child when unset and the canonical Juneau checkout has `juneau_terminal.py`.
- The toolkit is cached under `~/.cache/juneau-terminal/juneau-release-manager` because Compile's `mvn clean` would delete a copy under `target/`.

## See also

- `release.md`, `coverage.md`
- `tests/README.md`
