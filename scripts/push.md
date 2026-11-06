# push.py

Builds, tests, commits and pushes Juneau in one command.

## When to use

- You have staged your reviewed changes and want them verified and pushed to the tracked upstream.
- You want only the build and test gates, with no commit or push: use `--test-only`.
- You changed only juneau-docs: use juneau-docs' own `scripts/push.py` (`push juneau-docs`).
- To run just the tests, use `test.py` directly instead.

## Usage

```bash
python3 scripts/push.py "Fixed bug in RestClient"
python3 scripts/push.py --test-only
python3 scripts/push.py "Fix a test" --warn-traces
python3 scripts/push.py "Quick fix" --skip-tests --dry-run
python3 scripts/push.py --test-only --jdk 28
```

The commit message is optional on the command line. It is prompted for when omitted, except with `--test-only`.

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `message` | text (optional positional) | prompted | Git commit message. An empty or missing message aborts with exit 1 (`resolve_commit_message`). |
| `--test-only` | none | off | Run only the build and test gates. No identity check, commit or push. Cannot be combined with `--tracker-audit` or `--skip-tests`. |
| `--skip-tests` | none | off | Skip the test step. The build then runs `mvn clean package install -DskipTests`. |
| `--dry-run` | none | off | Print the steps that would run and exit 0. |
| `--tracker-audit`, `--todo-audit` | none | off | Opt-in gate for maintainers who keep an external work-tracker checkout: blocks the push if its audit flags a file. |
| `--js-tests` | none | auto | Force the headless-browser JS harness. Fails if Node or npm is missing. Mutually exclusive with `--no-js-tests`. |
| `--no-js-tests` | none | off | Never run the JS harness, even when JS, CSS or FTL files changed. |
| `--warn-traces` | none | off | Report stack traces in the test output as a warning instead of failing the Traces row. Allowlist errors still fail. |
| `--jdk` | `N` or path | current `JAVA_HOME` | Run the Maven, javadoc and `test.py` steps on this JDK. A number resolves to `~/jdk/jdk-<N>`, else the single `~/jdk/*<N>*` entry (none or several: exit 2); anything else is a JDK home path. A macOS bundle resolves to its `Contents/Home`. The home must have `bin/java` (else exit 2). Sets `JAVA_HOME` and puts `<home>/bin` first on `PATH`; works with `--dry-run` and `--test-only`. The resolved home and first `java -version` line show in the session header. |
| `--console` | `full`, `board`, `none` | `board` | Maven output on the console. Also `JUNEAU_TERMINAL_CONSOLE`; the flag wins. |
| `--detail` | `summary`, `actionable`, `modules`, `all` | `actionable` | Console detail. Also `JUNEAU_TERMINAL_DETAIL`. |

## What it does

1. Parses and validates the arguments. A bad combination exits 2 (`parse_args`).
2. Prechecks, before any build work (`prechecks`): git `user.email` is the committer identity the script requires (`REQUIRED_GIT_EMAIL`), the tree has no unstaged or untracked-and-not-ignored changes, the branch is not behind its upstream, and every `.github/workflows/*.yml` / `*.yaml` parses as YAML (PyYAML; the refusal names `file:line:col`). Also a `packaging tools` check (`packaging_tools_check`): it fails when `uv`, `npm`, `ruby` or `twine` (`_juneau.PACKAGING_TOOLS`, the one list `test.py --full` also requires via `JUNEAU_TERMINAL_REQUIRE_PACKAGING_TOOLS=1`) is not on `PATH`, naming each missing tool with its install hint (`twine`: `uv tool install --native-tls twine`). It is left out under `--skip-tests`, which runs no packaging tests. `--test-only` skips all prechecks, so it skips this too.
3. Optional gate, when asked: tracker audit (`run_tracker_audit_gate`).
4. Unless `--skip-tests`: `check-container-tags.py`, `check-bom-completeness.py`, `check-javadoc-html.py` (doclint `html` and `accessibility` over every main source tree, with `--java-home` set from `$JAVA_HOME` or `~/jdk/default`; any remaining diagnostic fails the push), then `test.py --full` (which builds, installs and runs the Java, Python and, when needed, browser tests, then fails on any stack trace they printed that [`trace-allowlist.txt`](trace-allowlist.txt) does not allow), then `push-timings.py`.
5. Commits whatever is already staged and pushes to the tracked upstream (`commit_and_push`). It stages nothing itself.
6. Prints a hint (`say_docs_hint`) when the sibling `juneau-docs` has staged changes or unpushed commits: run `push juneau-docs`. This script never commits, pushes or builds juneau-docs; that is juneau-docs' own `scripts/push.py`.

## Output and exit codes

Progress is a board view. Full logs go under `~/.cache/juneau-terminal/`, and the session header prints the `tail -f` line for the log. Test timings are appended to `~/.cache/juneau-terminal/history/<repo>-<sha8>/test.jsonl`.

The commit and push steps run under a PTY with a 20-second watchdog. If git goes silent after a partial line, such as an ssh passphrase or pinentry prompt, that line appears on the board as a prompt. Type the answer as usual.

| Code | Meaning |
|---|---|
| 0 | Pushed, nothing to push, `--dry-run` or `--test-only` passed. |
| 1 | A gate failed, the prechecks refused (including invalid workflow YAML or PyYAML missing), or no commit message was given. |
| 2 | Invalid argument combination, or a `--jdk` value that names no single usable JDK. |
| 130 | Cancelled with Ctrl-C or, with run markers on, SIGTERM. |

## Gotchas

- Maven runs are serialized across pushes and agents: from the first gate until the build and install finish, it holds the shared Maven lock (`mvn_lock.py`, the same `mkdir` lock directory as `~/agents/skills/juneau-worktrees/juneau-mvn-lock.sh`, `/tmp/claude-501/juneau-mvn.lockdir` unless `JUNEAU_MVN_LOCKDIR` is set). When another build holds it, the board says who (pid and command) at once and about every minute, instead of looking hung. A push killed mid-build releases the lock; a lock left by a dead pid is broken by the next waiter. Children (such as `test.py`) see `JUNEAU_MVN_LOCK_HELD` and a `JUNEAU_MVN_WRAPPER` that names the lock script is dropped for them, so they do not wait on their parent. The lock is released before the commit and push. `--dry-run` takes no lock.
- It never stages for you. Anything unstaged or untracked-and-not-ignored makes the prechecks refuse, and the check repeats just before the commit.
- The workflow-YAML guard needs PyYAML whenever the repo has workflow files, and fails closed without it: `pip install pyyaml`, or run the script with `uv run --with pyyaml`. The check repeats just before the commit, and also for a push-only run.
- The commit takes the index exactly as it finds it, so stage the whole set you want pushed.
- JDK pass record: after a green gate, push.py (and a clean-tree `test.py --full` or `--test-only` run) appends the commit and the JDK's major version (`17`, `21`, `28-ea`) to `~/.cache/juneau-terminal/jdk-passes.json` through `jdk_passes.py`, keyed by full commit sha with `jdk`, `time` and `source` per entry; the release manager reads it for the vote email. `JUNEAU_JDK_PASSES` overrides the path. Writes are atomic and best effort; entries older than 90 days and all but the newest 300 commits are pruned.
- `--jdk` resolution lives in `jdk.py` (shared with `test.py --jdk`). The JDK is applied to push.py's own environment, so every child (Maven, `check-javadoc-html.py` via `$JAVA_HOME`, `test.py`) inherits it. `~/jdk/*<N>*` can match more than one entry (for example `jdk-28-ea` and `jdk-28-ea-dl` for `28`, when no `jdk-28` exists): pass a path, or make a `jdk-<N>` link, when it is ambiguous. A JDK outside `~/jdk` (such as one under `/Library/Java/JavaVirtualMachines`) needs a path.
- `JUNEAU_PUSH_NO_SOUND` silences the completion sound.
- `--test-only` here means "gates without a commit or push". `test.py --test-only` means "tests without the build".
- The upstream check does a `git fetch`, so it needs the network. A failed fetch fails closed.

## See also

- `test.py`, `push-timings.py`, `check-container-tags.py`, `check-bom-completeness.py`, `check-javadoc-html.py`
- Docs site: Developer Info, Script: push (`04.02.02.ScriptPush.md` in juneau-docs)
