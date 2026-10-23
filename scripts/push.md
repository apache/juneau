# push.py

Builds, tests, commits and pushes Juneau (and, when present, the sibling juneau-docs repository) in one command.

## When to use

- You have staged your reviewed changes and want them verified and pushed to the tracked upstream.
- You want only the build and test gates, with no commit or push: use `--test-only`.
- You changed only juneau-docs: use `--docs-only`.
- To run just the tests, use `test.py` directly instead.

## Usage

```bash
python3 scripts/push.py "Fixed bug in RestClient"
python3 scripts/push.py "Updated topic page" --docs-only
python3 scripts/push.py --test-only
python3 scripts/push.py "Quick fix" --skip-tests --dry-run
```

The commit message is optional on the command line. It is prompted for when omitted, except with `--test-only`.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `message` | text (optional positional) | prompted | Git commit message. An empty or missing message aborts with exit 1 (`resolve_commit_message`). |
| `--test-only` | none | off | Run only the build and test gates. No identity check, commit, push or juneau-docs follow-up. Cannot be combined with `--docs-only`, `--sonarqube`, `--tracker-audit` or `--skip-tests`. |
| `--skip-tests` | none | off | Skip the test step. The build then runs `mvn clean package install -DskipTests`. |
| `--dry-run` | none | off | Print the steps that would run and exit 0. |
| `--docs-only` | none | off | Operate only on juneau-docs: skip all Juneau build, test, commit and push steps. The identity and docs-verification gates still run. `--skip-tests` and `--sonarqube` are ignored with a note. |
| `--sonarqube`, `--sonar` | none | off | Opt-in gate: run `sonarqube.py` in whole-repository mode and block the push if SonarCloud reports any issue. |
| `--tracker-audit`, `--todo-audit` | none | off | Opt-in gate for maintainers who keep an external work-tracker checkout: blocks the push if its audit flags a file. |
| `--js-tests` | none | auto | Force the headless-browser JS harness. Fails if Node or npm is missing. Mutually exclusive with `--no-js-tests`. |
| `--no-js-tests` | none | off | Never run the JS harness, even when JS, CSS or FTL files changed. |
| `--console` | `full`, `board`, `none` | `board` | Maven output on the console. Also `JUNEAU_RUN_CONSOLE`; the flag wins. |
| `--detail` | `summary`, `actionable`, `modules`, `all` | `actionable` | Console detail. Also `JUNEAU_RUN_DETAIL`. |

## What it does

1. Parses and validates the arguments. A bad combination exits 2 (`parse_args`).
2. Prechecks, before any build work (`prechecks`): git `user.email` is the committer identity the script requires (`REQUIRED_GIT_EMAIL`), the tree has no unstaged or untracked-and-not-ignored changes, and the branch is not behind its upstream. The same three run for juneau-docs when it is present and has staged changes or unpushed commits.
3. Optional gates, when asked: SonarQube (`run_sonarqube_gate`), tracker audit (`run_tracker_audit_gate`).
4. Unless `--skip-tests`: `check-container-tags.py`, `check-bom-completeness.py`, then `test.py --full` (which builds, installs and runs the Java, Python and, when needed, browser tests), then `push-timings.py`.
5. Commits whatever is already staged and pushes to the tracked upstream (`commit_and_push`). It stages nothing itself.
6. Repeats the follow-up for juneau-docs when it exists (`run_docs_followup`), after its docs build check.

## Output and exit codes

Progress is a board view. Full logs go under `~/.cache/juneau-run/`, and the session header prints the `tail -f` line for the log. Test timings are appended to `~/.cache/juneau-run/history/<repo>-<sha8>/test.jsonl`.

| Code | Meaning |
|---|---|
| 0 | Pushed, nothing to push, `--dry-run`, `--test-only` passed, or `--docs-only` with no changes. |
| 1 | A gate failed, the prechecks refused, or no commit message was given. |
| 2 | Invalid argument combination. |
| 130 | Cancelled with Ctrl-C or, with run markers on, SIGTERM. |

## Gotchas

- It never stages for you. Anything unstaged or untracked-and-not-ignored makes the prechecks refuse, and the check repeats just before the commit.
- The commit takes the index exactly as it finds it, so stage the whole set you want pushed.
- `JUNEAU_PUSH_NO_SOUND` silences the completion sound.
- `--test-only` here means "gates without a commit or push". `test.py --test-only` means "tests without the build".
- The upstream check does a `git fetch`, so it needs the network. A failed fetch fails closed.
- With `--sonarqube`, a branch that SonarCloud has not analyzed yet is skipped with a warning, not failed. The result reflects the last CI analysis, not the local diff.

## See also

- `test.py`, `sonarqube.py`, `push-timings.py`, `check-container-tags.py`, `check-bom-completeness.py`
- Docs site: Developer Info, Script: push (`04.02.02.ScriptPush.md` in juneau-docs)
