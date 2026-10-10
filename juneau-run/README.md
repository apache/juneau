# juneau-run

Build tooling, not a library dependency. It ships `juneau_run.py`, a stdlib-only Python script that wraps
Maven, pytest and Playwright runs: it prints run-protocol v1 markers (`##run`) when `RUN_MARKERS=1`, routes the
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
| `JUNEAU_RUN_CONSOLE=full\|condensed\|none` | Where tool output goes. `condensed` turns the view on. |
| `JUNEAU_RUN_DETAIL=summary\|actionable\|modules\|all` | How much the view shows (default `actionable`). `--detail` on `push.py`, `test.py` and `release.py` wins. |
| `JUNEAU_RUN_FULL_LOG`, `JUNEAU_RUN_CONDENSED_LOG` | The raw log, and the view's rows as plain text. The header names the full log. |
| `JUNEAU_RUN_LIVE=0` | Plain view: each row printed once, when it ends. Also used when stdout isn't a terminal or `TERM=dumb`. |
| `NO_COLOR` | The live view without colour. |
| `JUNEAU_RUN_LABEL_WIDTH` | Minimum width of the row labels (default 10). A parent sets it for a nested script so the rows line up. |

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
- `session(title, header=())`: the header row (`title · header… · full log: <path>`) and the view. Ends with
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
- `run_tool(…, tty=False, watchdog=None, capture=False, summarize=None, label=None)`: `tty=True` runs the tool
  under a PTY so gpg, git or svn can prompt, and forwards your keystrokes. It points `GPG_TTY` at the PTY, so a
  terminal pinentry prompts through the row's watchdog prompt, with echo off; a GUI pinentry (pinentry-mac) is
  unaffected. `watchdog=<s>` shows a partial line
  that has sat silent for `<s>` seconds as a prompt, or `quiet <n>s` on the row when there is no partial line.
  `capture=True` returns the text in `Result.output`. `summarize(text)` sets the row's summary.
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
- `push.py`'s timing report flags a module only when it is both `JUNEAU_PUSH_TIMING_THRESHOLD` (default `0.20`)
  and `JUNEAU_PUSH_TIMING_MIN_SECONDS` (default `5`) slower than its median. The full table goes to
  `~/.cache/juneau-push-timings/<branch>.txt`. On a first run the Timing row says "no timing history yet" and no
  table is written.
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

They are plain `.log` files, which RAT excludes. If a Maven upgrade changes its output, re-record, then fix the parser tests that fail.
