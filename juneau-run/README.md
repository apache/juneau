# juneau-run

Build tooling, not a library dependency. It ships `juneau_run.py`, a stdlib-only Python script that wraps
Maven, pytest, Playwright, Gradle, npm and Bazel runs: it prints run-protocol v1 markers (`##run`) when `RUN_MARKERS=1`, routes the
full and condensed output to the console and log files, and parses tool output into sub-steps and test counts.
The wire format is in [docs/run-protocol-v1.md](docs/run-protocol-v1.md).

The module is `pom` packaging. `mvn install` attaches the script as `org.apache.juneau:juneau-run:<version>:py`
and runs the unit tests (`python3 -m unittest discover -s src/test/python`). `-DskipTests` skips them.

## Using it from a push.py

Paste this into the adopting script. It loads the script at the Juneau version the repo already pins, and returns
`None` (after one stderr warning) when it can't, so the adopter then runs its commands as it does today.

```python
def load_juneau_run(version, cache_dir):
    """Returns the juneau_run module, or None (with one warning) if it can't be loaded."""
    import importlib.util, os, subprocess, sys
    path = os.environ.get("JUNEAU_RUN_PY")
    try:
        if not path:
            path = os.path.join(cache_dir, "juneau-run.py")
            if version.endswith("-SNAPSHOT") or not os.path.exists(path):
                subprocess.run(["mvn", "-q", "-nsu", "dependency:copy",
                    f"-Dartifact=org.apache.juneau:juneau-run:{version}:py",
                    f"-DoutputDirectory={cache_dir}", "-Dmdep.stripVersion=true"],
                    check=True, stdout=subprocess.DEVNULL)
        spec = importlib.util.spec_from_file_location("juneau_run", path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        sys.modules["juneau_run"] = mod
        return mod
    except Exception as e:
        print(f"warning: juneau-run unavailable ({e}); running commands without it", file=sys.stderr)
        return None
```

- `cache_dir` is the adopter's choice, for example `target/juneau-run/` under its repo.
- For a `-SNAPSHOT` pin the copy runs on every call, so a freshly installed tree is picked up. The copy is local.
- `JUNEAU_RUN_PY=/path/to/juneau_run.py` skips Maven entirely.

Typical use:

```python
juneau_run = load_juneau_run("10.0.0-SNAPSHOT", "target/juneau-run")
if juneau_run:
    result = juneau_run.run_tool(["mvn", "test", "-T1C"], "maven", "tests", "Tests")
```

Adopters whose stdout is captured by a consumer must use `console="full"` so the consumer's raw log stays
complete. Quiet Maven calls (`-q`) should use the `generic` parser.

When a script calls `run.cmd` (or `run.gate`) without `tool=`, the parser is picked from the command: `mvn`/`mvnw`
→ maven, `gradle`/`gradlew` → gradle, pytest (directly, `python -m pytest` or `uv run pytest`) → pytest, `npm` →
npm, `playwright test` → playwright, and `bazel`/`bazelisk` `build`/`test`/`coverage` → bazel, looking through
`env`, `npx`, `pnpm`, `yarn` and `npm exec`.  Quiet Maven (`-q`/`--quiet`) and anything else get the generic parser.
Parsers other than the built-in ones live in `juneau_scripts.parsers`; if that package can't be found, juneau-run
warns once and uses the generic parser.

Parser notes:

- **Bazel.** The Bazel parser adds `--build_event_json_file=<temp file>` after the command word.  If a `.bazelrc` also
  sets that flag, the injected one overrides it (Bazel takes the last value), so the `.bazelrc` file is not written
  during that run.  To keep it, pass the flag on the command line yourself: the parser then reads that file and
  adds nothing.

Progress on one line: `juneau_run.open_line("ORDERS: filling UID")`, then `dot()` or `append(text)` while working, and
`close_line(" 1819 rows in 4s")` to finish. `set_tail(text)` rewrites everything after the head, as in a counter
(`set_tail("100 of 200 complete")`). The helpers always write, with markers on or off. A marker written while a line is
open first ends that line. The console card shows the open line growing in place.

## PTY mode

`juneau_run.py --pty --full-log LOG --events EVENTS [--size COLSxROWS] <tool> -- <cmd...>` runs the tool under
a pseudo-terminal of a fixed size (default `120x40`), so tools that colour, redraw or draw progress bars behave as they
do in a terminal. `LOG` keeps every byte unchanged, and `EVENTS` gets run-view events with `rawOffset`, for the
terminal region in `juneau-rest-server-terminal`. Every option comes before `<tool>`. POSIX only. See "PTY mode" in
`docs/run-protocol-v1.md`.

## Console view

With `JUNEAU_RUN_CONSOLE=condensed`, a script that calls `session(title, header)` gets a condensed view: one row
per step, with its time, its counts and one line of summary. Raw tool output goes to the full log instead of the
screen. `full` (the default) prints raw output as before; `none` draws nothing. Both still write the logs.

| Variable | Effect |
|---|---|
| `JUNEAU_RUN_CONSOLE=full\|condensed\|none` | Where tool output goes. `condensed` turns the view on. `condensed` and `none` also make `bootstrap.load()` load juneau_run, so a tool's output goes to the logs and not the terminal; `none` draws nothing and emits no markers unless `RUN_MARKERS=1`. |
| `JUNEAU_RUN_DETAIL=summary\|actionable\|modules\|all` | How much the view shows (default `actionable`). `--detail` on `push.py`, `test.py` and `release.py` wins. |
| `JUNEAU_RUN_FULL_LOG`, `JUNEAU_RUN_CONDENSED_LOG` | The raw log, and the view's rows as plain text. The header prints `tail -f <full log>` to paste into another terminal. With `condensed` or `none` and no full log, the full log defaults to `~/.cache/juneau-run/<script>-<YYYYMMDD-HHMMSS>.log` (the newest 20 are kept); nested scripts share it. |
| `JUNEAU_RUN_LOG_DIR` | Where that default full log goes (default `~/.cache/juneau-run`). The tests point it at a temp dir. |
| `JUNEAU_RUN_LIVE=0` | Plain view: each row printed once, when it ends. Also used when stdout isn't a terminal or `TERM=dumb`. |
| `NO_COLOR` | The live view without colour. |
| `JUNEAU_RUN_LABEL_WIDTH` | Minimum width of the row labels (default 10). A parent sets it for a nested script so the rows line up. |
| `JUNEAU_RUN_HISTORY=0` | Turns off recording and estimating run history (default on). Without history the view shows no percent, time estimates or Overall line. |
| `JUNEAU_RUN_HISTORY_DIR` | Replaces `~/.cache/juneau-run/history`, where per-repository run history is kept. The tests point it at a temp dir. |
| `JUNEAU_RUN_PYTEST_PLUGIN=0` | Skips injecting `-p juneau_run_pytest` into pytest runs (the plugin that reports test progress). The suite under test gets only the plugin-only `pytest_plugin/` directory (in the zip, `<zip>/pytest_plugin`) on `PYTHONPATH`, never the engine or toolkit. |

With `RUN_MARKERS=1` stdout carries the run-protocol markers, so no view is drawn there; the condensed log still gets
the rows.

| Level | A finished step | Notes |
|---|---|---|
| `summary` | One row | `error` |
| `actionable` | One row; a failed module keeps its sub-row | `warn`, `error` |
| `modules` | Plus a sub-row per module that ran tests | `warn`, `error` |
| `all` | Plus a sub-row for every module | everything |

Failures, prompts, `show` text, `passthrough` output, watchdog prompts and the final `✅`/`❌`/`⚠` line show at
every level, except under `none`, where nothing is drawn: in a session a watchdog prompt (a tool waiting for input)
appears only in the full log, and a `run_tool` call with no session still echoes it to stdout. Don't use `none` for
interactive runs such as `release.py`.

Script API (all of it works with no session too, printing as before):
- `session(title, header=())`: the header row (`title · header… · tail -f <full log>`) and the view. Ends with
  `done(status)`. A process that inherited a session (`JUNEAU_RUN_SESSION`) draws its rows into the parent's view
  and prints no header or final line.
- `step(id, n, title, *, parent=None, label=None)` and `row(id, title, *, label=None)` yield a `Step`. Set
  `.summary` for the row text, or call `.fail()` or `.skip()` (a dim row). `label` is the short left-hand name
  ("Compile"); markers keep `title`.
- `say(text, level="info", step=None)`: status text, filtered by the detail level. `failure(text, step=None)`:
  always shown.
- `ask(prompt) -> str`: pauses the view and reads a line. `show(text, pager=False)`: pauses and prints the text in
  full, or pipes it to `$PAGER` (`less -R`) on a terminal. Ctrl-C in the pager is the pager's; quitting it returns.
- `passthrough(cmd, step_id, title, …)`: runs an interactive command on the real terminal as a row. No capture,
  nothing in the full log.
- `run_tool(…, tty=False, watchdog=None, capture=False, summarize=None, label=None, statuses=None)`: `tty=True`
  runs the tool under a PTY so gpg, git or svn can prompt, and forwards your keystrokes. It points `GPG_TTY` at the
  PTY, so a terminal pinentry prompts through the row's watchdog prompt, with echo off; a GUI pinentry (pinentry-mac)
  is unaffected. `watchdog=<s>` shows a partial line that has sat silent for `<s>` seconds as a prompt, or
  `quiet <n>s` on the row when there is no partial line. `capture=True` returns the text in `Result.output`.
  `summarize(text)` sets the row's summary. `statuses` maps an exit code to the row's status (`ok`, `fail` or
  `skip`); other codes are `ok` for 0 and `fail` otherwise, so a gate script's skip exit can show as a skip. 127
  (cannot start) and 130 (interrupted) always end as `fail`, whatever `statuses` says.
- `handoff()`: gives the terminal to a child script for a block and yields the env it needs (label width, detail).
  `console_active()`: True when a view owns the terminal.
- `export_detail(flag)`: resolves `--detail` over `JUNEAU_RUN_DETAIL`, exports it for children, and raises
  `ValueError` for an unknown level.

Ctrl-C and SIGTERM close the view, restore the cursor and end the run as `cancelled` (the scripts exit 130). If the
live view fails, the run switches to the plain view with a warning. If the plain view fails too, or the screen can't
be written, the view stops with `⚠ console view stopped (…)`, and script text and the final line print plainly from
then on; a failure in the view's Board also stops the condensed log. A display bug never fails a build.

Known limitation: narrowing a reflowing terminal while the live view runs can leave stale fragments above the
region.

## Writing push and release scripts

`juneau-run` also ships `juneau_scripts`, a stdlib-only toolkit for push and release scripts, as
`org.apache.juneau:juneau-run:<version>:zip`. It holds the helpers every push.py and release.py rewrites: git
readers and writers, a session with exit codes, the bell and the done marker, tool runners, a step registry with
resume, locks and prompts. Policy stays in the script. The toolkit never runs `git add`, and it never decides what
counts as unreviewed.

Vendor the loader once, from the installed zip:

```bash
unzip -p ~/.m2/repository/org/apache/juneau/juneau-run/<v>/juneau-run-<v>.zip bootstrap.py > scripts/bootstrap.py
```

Then:

```python
from bootstrap import load
jr, js = load(pom="pom.xml")
from juneau_scripts import git, run, tools, notify

def main(args):
    with run.session("push", bell=True) as s:
        dirty = git.unstaged_paths() + git.untracked_paths()
        if dirty:                                    # this project's policy
            s.fail("Unreviewed changes", hint=git.stash_hint(dirty))
        if not args.skip_tests:
            s.require(tools.maven(["verify"], title="Build and test"))
        if args.test_only:
            return
        s.require(git.commit_index(args.message))
        s.require(git.push(set_upstream=True))
```

- The exit codes are 0 for success, 1 for failure and 130 for a cancel. Every path prints the summary and writes exactly
  one `done` marker, and the bell rings unless the run is nested or silenced (`SANDBOX_RUNTIME=1`,
  `JUNEAU_SCRIPTS_QUIET=1`, or the script's own `silence_env`).
- With markers off and no TTY, the same code prints plain lines, so it works in CI.
- `load()` runs at import here, which suits a script nothing imports. A script whose tests `import push` calls `load()`
  inside `main()` instead. The `pom` path is relative to the current directory, so pass an absolute one if the script
  can run from elsewhere.

`load()` finds the toolkit in this order, and loads `juneau_run` only for `run=True` or when `RUN_MARKERS=1` or
`JUNEAU_RUN_CONSOLE=condensed` asks for it:
1. `JUNEAU_SCRIPTS_ZIP` (a zip, or a directory holding `juneau_scripts/`) and `JUNEAU_RUN_PY`. With only
   `JUNEAU_RUN_PY`, the toolkit is `<stem>.zip` or a `juneau_scripts/` directory beside it.
2. In-tree: `juneau-run/src/main/python/` under the pom's directory (this repository). No `.pyc` is written.
3. `~/.m2/repository/org/apache/juneau/juneau-run/<v>/`, in place, when both the `.py` and the `.zip` are there.
4. Otherwise `mvn dependency:copy` of both into `target/juneau-run/`. A `-SNAPSHOT` is copied once per process, and a
   release only when it is missing.

The modules: `git`, `run` (`session`, `cmd`, `gate`, `step`, `say`, `handoff`, `child_env`), `tools` (`maven`,
`pytest`, `playwright`, `npm`, `gradle`), `notify`, `prompt`, `java`, `lock`, `proc`, `state`, `steps` and
`testing` (`hermetic_git`, `bare_remote`, `clean_run_env`, `fake_tool`, and pytest fixtures through
`pytest_plugins = ["juneau_scripts.testing.pytest_plugin"]`). Each module's docstring is its reference.

The `load_juneau_run` snippet above still works for a single-file adopter that needs only `juneau_run`.

## In this repository

`scripts/test.py`, `scripts/push.py` and `scripts/release.py` load the script straight from
`juneau-run/src/main/python/` (no Maven involved).
- With `RUN_MARKERS=1`, `push.py` and `test.py` print run-protocol markers: `push.py` owns the run (`run`, `done`,
  its own steps); `test.py` and anything it runs contribute steps only, because `run()` exports
  `JUNEAU_RUN_ACTIVE=1`. `release.py` emits no markers; it warns and ignores `RUN_MARKERS`.
- `test.py --console full|condensed|none --full-log PATH --condensed-log PATH` routes Maven output (the same settings
  can come from `JUNEAU_RUN_CONSOLE`, `JUNEAU_RUN_FULL_LOG`, `JUNEAU_RUN_CONDENSED_LOG`; an argument wins; relative
  paths resolve under `$RUN_ARTIFACTS`). `push.py` and `release.py` take the console settings from the environment.
- `--detail summary|actionable|modules|all` on all three scripts sets the console detail (see "Console view").
  `push.py` passes it, and its label width, to the `test.py` it runs, and hands that `test.py` the terminal while it
  runs.
- `push.py --test-only` runs the build and test gates only (no identity check, commit, push or docs follow-up). It
  is not the same flag as `test.py --test-only`, which means "tests without the build".
- Under a console view, `push.py` runs `git commit` and `git push` with `tty=True`, so ssh and gpg can prompt.
- `push.py`'s timing report reads the repo's run history: the `tests` records `test.py` writes to
  `~/.cache/juneau-run/history/<repo>-<sha8>/test.jsonl`, where `<repo>-<sha8>` is the repo key (the basename and
  the first 8 hex digits of the SHA-1 of its git common dir), so every branch and linked worktree of the repo shares
  it. It flags a module only when it is both `JUNEAU_PUSH_TIMING_THRESHOLD` (default `0.20`) and
  `JUNEAU_PUSH_TIMING_MIN_SECONDS` (default `5`) slower than its median. The full table goes to `test-timings.txt`
  beside `test.jsonl`. With no records yet the Timing row says "no timing history yet" and no table is written; with
  `JUNEAU_RUN_HISTORY=0` it says the run history is off.
- `release.py` always writes its full log to `~/.juneau-release-logs/<release>-<YYYYMMDD-HHMMSS>.log`, whatever the
  console mode. Its Maven commands run with `-B` under a PTY (for gpg), and `run_git_diff` pages the
  `release:prepare` diff through `$PAGER` before `release:perform`. Quit the pager with `q`; `release:perform`
  starts straight after. Ctrl-C in the pager belongs to the pager and does not cancel the release.
- `-B` stops only the release plugin's own prompts (`release.py` passes every version with `-D`). git, svn and
  gpg still prompt through the PTY, and the row shows the prompt after 20 s of silence. To avoid the prompts, set up
  before a release: a git credential helper that knows your `gitbox.apache.org` credentials (see
  `git help credential`), a `<server>` entry for `apache.releases.https` in `~/.m2/settings.xml`, svn's cached
  credentials (run `svn info https://dist.apache.org/repos/dist/dev/juneau` once), and a primed gpg-agent (the PGP
  step).
- With markers off and the default console, `push.py` and `test.py` behave exactly as before.

## Regenerating the test fixtures

The Maven fixtures in `src/test/python/fixtures/` are recorded output, not hand-written:
- `src/test/python/record-maven-fixtures.sh <scratch-dir> <fixtures-dir>` builds a throwaway 3-module project and records the five
  `maven-*-failure/success.log` files. Local paths are replaced with `/work/demo` and `/home/user`.
- `maven-juneau-T1C.log` is a trimmed slice of a real `mvn test -T1C` build of this repository (start of the run, a few Surefire
  class lines, the skipped-test summary and the Reactor Summary), with `/Users/<name>` replaced by `/home/user`.
- `src/test/python/record-gradle-npm-fixtures.sh <scratch-dir> <fixtures-dir>` records the `gradle-*.log` and `npm-*.log`
  files from a throwaway two-project Gradle build and an offline npm project, with the same path scrubbing.
- Bazel: `bash src/test/python/record-bazel-fixtures.sh <scratch> src/test/python/fixtures/parsers/bazel` (needs
  bazelisk and network; `USE_BAZEL_VERSION` pins the release), then review each case with
  `python3 src/test/python/parser_replay.py print <case>` and store it with `... expect <case>`.  The live test runs
  only with `JUNEAU_PARSER_LIVE=1`.

They are plain `.log` files, which RAT excludes. If a Maven upgrade changes its output, re-record, then fix the parser tests that fail.
