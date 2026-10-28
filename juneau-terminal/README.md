# juneau-terminal

Build tooling, not a library dependency. It ships `juneau_terminal.py`, a stdlib-only Python script that wraps
Maven, pytest, Playwright, Gradle, npm and Bazel runs: it prints terminal-protocol v1 markers (`##terminal`) when `TERMINAL_MARKERS=1`, routes the
full and board output to the console and log files, and parses tool output into sub-steps and test counts.
The wire format is in [docs/terminal-protocol-v1.md](docs/terminal-protocol-v1.md).

The module is `pom` packaging. `mvn install` attaches the script as `org.apache.juneau:juneau-terminal:<version>:py`
and runs the unit tests (`python3 -m unittest discover -s src/test/python`). `-DskipTests` skips them.

## Using it from a push.py

Paste this into the adopting script. It loads the script at the Juneau version the repo already pins, and returns
`None` (after one stderr warning) when it can't, so the adopter then runs its commands as it does today.

```python
def load_juneau_terminal(version, cache_dir):
    """Returns the juneau_terminal module, or None (with one warning) if it can't be loaded."""
    import importlib.util, os, subprocess, sys
    path = os.environ.get("JUNEAU_TERMINAL_PY")
    try:
        if not path:
            path = os.path.join(cache_dir, "juneau-terminal.py")
            if version.endswith("-SNAPSHOT") or not os.path.exists(path):
                subprocess.run(["mvn", "-q", "-nsu", "dependency:copy",
                    f"-Dartifact=org.apache.juneau:juneau-terminal:{version}:py",
                    f"-DoutputDirectory={cache_dir}", "-Dmdep.stripVersion=true"],
                    check=True, stdout=subprocess.DEVNULL)
        spec = importlib.util.spec_from_file_location("juneau_terminal", path)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        sys.modules["juneau_terminal"] = mod
        return mod
    except Exception as e:
        print(f"warning: juneau-terminal unavailable ({e}); running commands without it", file=sys.stderr)
        return None
```

- `cache_dir` is the adopter's choice, for example `target/juneau-terminal/` under its repo.
- For a `-SNAPSHOT` pin the copy runs on every call, so a freshly installed tree is picked up. The copy is local.
- `JUNEAU_TERMINAL_PY=/path/to/juneau_terminal.py` skips Maven entirely.

Typical use:

```python
juneau_terminal = load_juneau_terminal("10.0.0-SNAPSHOT", "target/juneau-terminal")
if juneau_terminal:
    result = juneau_terminal.run_tool(["mvn", "test", "-T1C"], "maven", "tests", "Tests")
```

Adopters whose stdout is captured by a consumer must use `console="full"` so the consumer's raw log stays
complete. Quiet Maven calls (`-q`) should use the `generic` parser.

When a script calls `run.cmd` (or `run.gate`) without `tool=`, the parser is picked from the command: `mvn`/`mvnw`
→ maven, `gradle`/`gradlew` → gradle, pytest (directly, `python -m pytest` or `uv run pytest`) → pytest, `npm` →
npm, `playwright test` → playwright, `bazel`/`bazelisk` `build`/`test`/`coverage` → bazel, and `git clone`/`fetch`/`pull`
→ git (a bar from git's progress output; see below), looking through
`env`, `npx`, `pnpm`, `yarn` and `npm exec`.  Quiet Maven (`-q`/`--quiet`) and anything else get the generic parser.
Parsers other than the built-in ones live in `juneau_terminal_scripts.parsers`; if that package can't be found, juneau-terminal
warns once and uses the generic parser.

Parser notes:

- **Git.** `git clone`, `fetch` and `pull` get the git parser, including when a caller passes `tool="generic"` (the git
  parser behaves as the generic one when git prints no progress; git prints progress to a pipe only with `--progress`).
  It reads git's stderr, which is carriage-return separated, through the parser flag `cr_lines` (the runner then hands the
  parser each CR- or LF-terminated piece as it arrives; other parsers still see the last piece of each line).  One
  20-cell bar covers the transfer: Receiving (or Unpacking) objects 0-70 %, Resolving deltas 70-85 %, Updating files 85-100 %;
  the bar only moves forward and the last cell fills when git exits 0.  The server-side `remote:` phases (Enumerating,
  Counting, Compressing) show as the row's text only.  Test fixture: `src/test/python/fixtures/parsers/git/clone.stderr`
  (a recorded `git clone --progress --no-local`).
- **Bazel.** The Bazel parser adds `--build_event_json_file=<temp file>` after the command word.  If a `.bazelrc` also
  sets that flag, the injected one overrides it (Bazel takes the last value), so the `.bazelrc` file is not written
  during that run.  To keep it, pass the flag on the command line yourself: the parser then reads that file and
  adds nothing.

Progress on one line: `juneau_terminal.open_line("ORDERS: filling UID")`, then `dot()` or `append(text)` while working, and
`close_line(" 1819 rows in 4s")` to finish. `set_tail(text)` rewrites everything after the head, as in a counter
(`set_tail("100 of 200 complete")`). The helpers always write, with markers on or off. A marker written while a line is
open first ends that line. The console card shows the open line growing in place.

## PTY mode

`juneau_terminal.py --pty --full-log LOG --events EVENTS [--size COLSxROWS] <tool> -- <cmd...>` runs the tool under
a pseudo-terminal of a fixed size (default `120x40`), so tools that colour, redraw or draw progress bars behave as they
do in a terminal. `LOG` keeps every byte unchanged, and `EVENTS` gets terminal-view events with `rawOffset`, for the
terminal region in `juneau-terminal-region`. Every option comes before `<tool>`. POSIX only. See "PTY mode" in
`docs/terminal-protocol-v1.md`.

## Console view

With `JUNEAU_TERMINAL_CONSOLE=board`, a script that calls `session(title, header)` gets a board view: one row
per step, with its time, its counts and one line of summary. Raw tool output goes to the full log instead of the
screen. `full` (the default) prints raw output as before; `none` draws nothing. Both still write the logs.
`juneau_terminal.py --console board <tool> -- <cmd...>` on a terminal draws the same view for its one step, unit
lines included (one per running pytest or Playwright test file); with markers on, inside another session, or
off a terminal (`TERM=dumb` and `JUNEAU_TERMINAL_LIVE=0` count as off) it prints the parser's board lines instead.

| Variable | Effect |
|---|---|
| `JUNEAU_TERMINAL_CONSOLE=full\|board\|none` | Where tool output goes. `board` turns the view on. `board` and `none` also make `bootstrap.load()` load juneau_terminal, so a tool's output goes to the logs and not the terminal; `none` draws nothing and emits no markers unless `TERMINAL_MARKERS=1`. |
| `JUNEAU_TERMINAL_DETAIL=summary\|actionable\|modules\|all` | How much the view shows (default `actionable`). `--detail` on `push.py` and `test.py` wins. |
| `JUNEAU_TERMINAL_FULL_LOG`, `JUNEAU_TERMINAL_BOARD_LOG` | The raw log, and the view's rows as plain text. The header prints `tail -f` for each on a line of its own, to paste into another terminal: `Output: tail -f <full log>`, then `Board: tail -f <board log>`. With `board` or `none` and no full log, the full log defaults to `~/.cache/juneau-terminal/<script>-<YYYYMMDD-HHMMSS>.log`, and unless a board log is set (by argument or variable) a `.board.log` beside it, so another tool can follow the view's rows as plain text; the newest 20 of each kind are kept, and nested scripts share them. |
| `JUNEAU_TERMINAL_CANCEL` | A command that cancels the run (for example a pane launcher's own cancel command); the header shows it as a `Cancel: <command>` row after the log rows. Unset, there is no such row. |
| `JUNEAU_TERMINAL_LOG_DIR` | Where those default logs go (default `~/.cache/juneau-terminal`). The tests point it at a temp dir. |
| `JUNEAU_TERMINAL_TERM_GRACE=<seconds>` | How long a cancelled child's process group gets between SIGTERM and SIGKILL (default 5). A `grace=` argument to `run_tool` or `run.cmd` wins for that child; a value that isn't a non-negative number is ignored. |
| `JUNEAU_TERMINAL_LIVE=0` | Plain view: each row printed once, when it ends. Also used when stdout isn't a terminal or `TERM=dumb`. |
| `NO_COLOR` | The live view without colour. |
| `JUNEAU_TERMINAL_LABEL_WIDTH` | Minimum width of the row labels (default 10). A parent sets it for a nested script so the rows line up. |
| `JUNEAU_TERMINAL_PENDING`, `JUNEAU_TERMINAL_OVERALL` | Set by a parent for a nested script: the parent's pending rows, and its elapsed time, done steps and pending steps. With the second, the nested view keeps drawing the whole run's Overall line; without it a nested view draws none. |
| `JUNEAU_TERMINAL_HISTORY=0` | Turns off recording and estimating run history (default on). Without history the view shows no percent, time estimates or Overall line. |
| `JUNEAU_TERMINAL_HISTORY_DIR` | Replaces `~/.cache/juneau-terminal/history`, where per-repository run history is kept. The tests point it at a temp dir. |
| `JUNEAU_TERMINAL_HISTORY_KEY` | Replaces the per-repository key under that directory (`<name>-<hash>` of the git common dir, which linked worktrees already share), so separate clones of one repository can share one history. Made file-name safe. A script passes `history_key=` to `session(...)` instead, which wins over the variable and leaves the environment (and child processes) alone. |
| `JUNEAU_TERMINAL_PLAYWRIGHT_REPORTER=0` | In the command's environment or juneau-terminal's, leaves out juneau-terminal's Playwright reporter (`juneau_terminal_playwright.cjs`, which draws one line per running test file, counts the bar and, under markers with `TERMINAL_ARTIFACTS`, writes `<step>-playwright.json`). It is added through `PW_TEST_REPORTER`, which Playwright appends to the `reporter:` list in `playwright.config`, so the config's reporters still run and the output is a plain run's. Outside a terminal the html reporter is kept from opening its report (`PLAYWRIGHT_HTML_OPEN=never`, unless the command sets it), since it would otherwise serve it and wait. That variable is undocumented in Playwright: a version that ignores it runs as plain, without the per-file lines or the JSON report, and once tests have run the step notes "Playwright reporter not loaded" (never under a terminal). Nothing is added when the command names its own `--reporter` or sets `PW_TEST_REPORTER`, nor without markers outside a non-interactive console session (a script's, or the CLI's board view on a terminal); under a terminal it is added only for the JSON report and prints nothing. From the zip the reporter is extracted to a temp directory removed at exit, since node can't load from a zip. |
| `JUNEAU_TERMINAL_MARKERS_FILE=<path>` | The terminal-protocol markers go to that file too, one `##terminal` line each, while the console view draws as usual; with `TERMINAL_MARKERS=1` stdout and the file both get them. The run's owner empties it when its `run()` starts; nested scripts append. Also makes `bootstrap.load()` load juneau_terminal. See `docs/terminal-protocol-v1.md`, "Markers file". |
| `JUNEAU_TERMINAL_RESULT=<path>` | At the end of the run (ok, failed or cancelled) the owner writes the result there as one JSON object, atomically: status, exit code, time, project, branch, head, commit, the rows (with their test totals), the notes and the log paths. Nested scripts' rows are included. Also makes `bootstrap.load()` load juneau_terminal. See `docs/terminal-protocol-v1.md`, "Result file". |
| `JUNEAU_TERMINAL_PYTEST_PLUGIN=0` | Skips injecting `-p juneau_terminal_pytest` into pytest runs (the plugin that reports test progress). The suite under test gets only the plugin-only `pytest_plugin/` directory (in the zip, `<zip>/pytest_plugin`) on `PYTHONPATH`, never the engine or toolkit. |

With `TERMINAL_MARKERS=1` stdout carries the terminal-protocol markers, so no view is drawn there; the board log still gets
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
- `session(title, header=())`: the header rows (`▶ title: item, item…`, wrapping with a two-space indent, then a dim `›` on `Output: tail -f <full log>`, `Board: tail -f <board log>` and, with `JUNEAU_TERMINAL_CANCEL`, `Cancel: <command>`) and the view, whose last line is `▶ Overall …`. Ends with
  `done(status)`. A process that inherited a session (`JUNEAU_TERMINAL_SESSION`) draws its rows into the parent's view
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
- `run_tool(…, tty=False, watchdog=None, capture=False, summarize=None, label=None, statuses=None, bar=None)`: `tty=True`
  runs the tool under a PTY so gpg, git or svn can prompt, and forwards your keystrokes. It points `GPG_TTY` at the
  PTY, so a terminal pinentry prompts through the row's watchdog prompt, with echo off; a GUI pinentry (pinentry-mac)
  is unaffected. `watchdog=<s>` shows a partial line that has sat silent for `<s>` seconds as a prompt, or
  `quiet <n>s` on the row when there is no partial line. `capture=True` returns the text in `Result.output`.
  `summarize(text)` sets the row's summary. `statuses` maps an exit code to the row's status (`ok`, `fail` or
  `skip`); other codes are `ok` for 0 and `fail` otherwise, so a gate script's skip exit can show as a skip. 127
  (cannot start) and 130 (interrupted) always end as `fail`, whatever `statuses` says. `bar` picks what one block of
  the row's bar stands for (see "Test rows" below); a value the parser doesn't offer raises `ValueError` before the
  command starts.
- `handoff()`: gives the terminal to a child script for a block and yields the env it needs (label width, detail).
  `console_active()`: True when a view owns the terminal.
- `export_detail(flag)`: resolves `--detail` over `JUNEAU_TERMINAL_DETAIL`, exports it for children, and raises
  `ValueError` for an unknown level.

Ctrl-C and SIGTERM close the view, restore the cursor and end the run as `cancelled` (the scripts exit 130). If the
live view fails, the run switches to the plain view with a warning. If the plain view fails too, or the screen can't
be written, the view stops with `⚠ console view stopped (…)`, and script text and the final line print plainly from
then on; a failure in the view's Board also stops the board log. A display bug never fails a build.

Known limitation: narrowing a reflowing terminal while the live view runs can leave stale fragments above the
region.

## Writing push and release scripts

`juneau-terminal` also ships `juneau_terminal_scripts`, a stdlib-only toolkit for push and release scripts, as
`org.apache.juneau:juneau-terminal:<version>:zip`. It holds the helpers every push.py and release.py rewrites: git
readers and writers, a session with exit codes, the bell and the done marker, tool runners, a step registry with
resume, locks and prompts. The zip also carries `juneau_terminal_scripts/bin/tmux-pane.sh` and `watch-run.sh` (mode 0755; see
[The side-pane scripts](#the-side-pane-scripts)) and a root `__main__.py`, so `python3 juneau-terminal-<v>.zip --bin NAME`
works. Policy stays in the script. The toolkit never runs `git add`, and it never decides what
counts as unreviewed.

Vendor the loader once, from the installed zip:

```bash
unzip -p ~/.m2/repository/org/apache/juneau/juneau-terminal/<v>/juneau-terminal-<v>.zip bootstrap.py > scripts/bootstrap.py
```

Then:

```python
from bootstrap import load
jr, js = load(pom="pom.xml")
from juneau_terminal_scripts import git, run, tools, notify

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
  `JUNEAU_TERMINAL_SCRIPTS_QUIET=1`, or the script's own `silence_env`).
- With markers off and no TTY, the same code prints plain lines, so it works in CI.
- `load()` runs at import here, which suits a script nothing imports. A script whose tests `import push` calls `load()`
  inside `main()` instead. The `pom` path is relative to the current directory, so pass an absolute one if the script
  can run from elsewhere.

`load()` finds the toolkit in this order, and loads `juneau_terminal` only for `terminal=True` or when `TERMINAL_MARKERS=1` or
`JUNEAU_TERMINAL_CONSOLE=board` asks for it:
1. `JUNEAU_TERMINAL_SCRIPTS_ZIP` (a zip, or a directory holding `juneau_terminal_scripts/`) and `JUNEAU_TERMINAL_PY`. With only
   `JUNEAU_TERMINAL_PY`, the toolkit is `<stem>.zip` or a `juneau_terminal_scripts/` directory beside it.
2. In-tree: `juneau-terminal/src/main/python/` under the pom's directory (this repository). No `.pyc` is written.
3. `~/.m2/repository/org/apache/juneau/juneau-terminal/<v>/`, in place, when both the `.py` and the `.zip` are there.
4. Otherwise `mvn dependency:copy` of both into `target/juneau-terminal/`. A `-SNAPSHOT` is copied once per process, and a
   release only when it is missing.

The modules: `git`, `run` (`session`, `cmd`, `gate`, `step`, `say`, `err`, `hint`, `next_n`, `handoff`, `child_env`), `tools` (`maven`,
`pytest`, `playwright`, `npm`, `gradle`), `notify`, `prompt`, `java`, `lock`, `proc`, `state`, `steps` and
`testing` (`hermetic_git`, `bare_remote`, `clean_run_env`, `fake_tool`, and pytest fixtures through
`pytest_plugins = ["juneau_terminal_scripts.testing.pytest_plugin"]`). Each module's docstring is its reference.

### Test rows: a block per file

A test row's bar has one block per unit of its tool's default size, and `bar=` on `run.cmd`, `run.gate`, `tools.pytest`
or `tools.playwright` picks another:

| Parser | Block by default | `bar=` values |
|---|---|---|
| `maven` | a module | `"modules"` |
| `pytest` | a test file (with the plugin's per-file counts) | `"files"`, `"tests"` |
| `playwright` | a test file (with the reporter) | `"files"`, `"tests"` |
| any other | (none) | none: any value raises `ValueError` |

A file's block is added when its last test finishes. It fails if any test in the file failed, it is skipped if every
test was skipped, and it passes otherwise. Files still running show as in-flight blocks after the finished ones. On a
file bar the percent and the `~M:SS` total follow the tests run against the history's expected test count, so they
move with every test while the count reads files (`7/32  ~25%`). `bar="tests"` gives one block per test. Without the
pytest plugin's counts or the Playwright reporter, both bars count tests.

### Prompts, pauses and stdin

`prompt` has six kinds: `text`, `secret`, `confirm` (with `expect=` for a type-to-confirm), `choice`, `form` (a list of
`Field`s) and `email` (shows a message to send from the user's own mail client, with a `mailto:` link). `None` always
means cancelled. When a host starts the script with the two prompt descriptors, every prompt is a JSON line to the host
and the answer comes back on the other descriptor; otherwise the prompts are inline on the terminal. A secret is never
published, logged, checkpointed or put on a command line. See `docs/prompt-protocol-v1.md`.

A step stops the run for a human wait by raising `steps.Pause(message, done=True)`. Its row draws ⏸ with the message,
`Registry.run` returns `Result(True, 0, status="paused")`, and with `done=True` a `--resume` starts after the step.
`state.Checkpoint.update({...})` writes several keys in one atomic write (a `None` value removes the key).

`run.cmd(..., parent="step-id")` runs the command as a child row of that step (its `n` defaults to 1, and it takes no number
from the session counter), and `label=` gives the row a short console label. `run.next_n(count=1)` hands out step numbers
for a script that numbers steps itself; it starts after `JUNEAU_TERMINAL_N_BASE`. `run.err(text)` and `run.hint(lines)` are the
failure channel: they go to the console view when it owns the screen, and to stderr otherwise. Inside `run.paused()` the
view is cleared, so `run.say` prints its text as-is. A session that opened the console view ends it when it ends (a
process that runs several sessions starts each one clean); `juneau_terminal.end_session()` ends a view you opened yourself.

`run.cmd(..., input=b"...")` writes the bytes to the command's stdin, then closes it. The bytes reach no log, marker or
command line. It cannot be combined with `tty=True`.

`juneau_terminal.py --pty --prompts stdio` runs the job with a prompt channel and relays it: the job's prompt lines go to this
process's stdout and this process's stdin lines go to the job. EOF on stdin means the host is gone, and the job is
cancelled. It cannot be combined with `--input`.

The `load_juneau_terminal` snippet above still works for a single-file adopter that needs only `juneau_terminal`.

## In this repository

`scripts/test.py` and `scripts/push.py` load the script straight from
`juneau-terminal/src/main/python/` (no Maven involved).
- With `TERMINAL_MARKERS=1`, `push.py` and `test.py` print terminal-protocol markers: `push.py` owns the run (`run`, `done`,
  its own steps); `test.py` and anything it runs contribute steps only, because `run()` exports
  `JUNEAU_TERMINAL_ACTIVE=1`.
- `test.py --console full|board|none --full-log PATH --board-log PATH` routes Maven output (the same settings
  can come from `JUNEAU_TERMINAL_CONSOLE`, `JUNEAU_TERMINAL_FULL_LOG`, `JUNEAU_TERMINAL_BOARD_LOG`; an argument wins; relative
  paths resolve under `$TERMINAL_ARTIFACTS`). `push.py` takes the console settings from the environment.
- `--detail summary|actionable|modules|all` on both scripts sets the console detail (see "Console view").
  `push.py` passes it, and its label width, to the `test.py` it runs, and hands that `test.py` the terminal while it
  runs.
- `push.py --test-only` runs the build and test gates only (no identity check, commit, push or docs follow-up). It
  is not the same flag as `test.py --test-only`, which means "tests without the build".
- Under a console view, `push.py` runs `git commit` and `git push` with `tty=True`, so ssh and gpg can prompt.
- `push.py`'s timing report reads the repo's run history: the `tests` records `test.py` writes to
  `~/.cache/juneau-terminal/history/<repo>-<sha8>/test.jsonl`, where `<repo>-<sha8>` is the repo key (the basename and
  the first 8 hex digits of the SHA-1 of its git common dir), so every branch and linked worktree of the repo shares
  it. It flags a module only when it is both `JUNEAU_PUSH_TIMING_THRESHOLD` (default `0.20`) and
  `JUNEAU_PUSH_TIMING_MIN_SECONDS` (default `5`) slower than its median. The full table goes to `test-timings.txt`
  beside `test.jsonl`. With no records yet the Timing row says "no timing history yet" and no table is written; with
  `JUNEAU_TERMINAL_HISTORY=0` it says the run history is off.
- With markers off and the default console, `push.py` and `test.py` behave exactly as before.

## The side-pane scripts

The zip ships two bash scripts in `juneau_terminal_scripts/bin/`, so a repository gets one maintained copy instead of its own:
- `tmux-pane.sh` runs a command in a tmux side pane with a real TTY, blocks until it ends, and can `cancel`, `list`,
  `close` and `equalize` such panes. It knows nothing about juneau-terminal.
- `watch-run.sh` wraps it for a juneau-terminal based script (`push.py`, `release.py`, `deploy*.py`): it picks the log and
  result paths, opens a follower pane that tails the full log, and prints the result summary when the script ends.

Each script's header comment is its usage text.

### Finding them

`bootstrap.py --bin NAME` prints the path and never runs `mvn`. It looks, in order, at `JUNEAU_TERMINAL_SCRIPTS_ZIP` and
`JUNEAU_TERMINAL_PY`, the zip it was imported from, a `juneau_terminal_scripts/` directory beside itself, the in-tree
`juneau-terminal/src/main/python/`, the `~/.m2` zip for the pom's `<juneau.version>`, and `target/juneau-terminal/juneau-terminal.zip`.
A directory source runs in place; a zip is extracted. Guard the result before using it:

```bash
wr=$(python3 scripts/bootstrap.py --bin watch-run.sh) && [ -x "$wr" ] && "$wr" -d . -t push -- python3 scripts/push.py
```

Without a vendored `bootstrap.py`, run the installed zip itself (its `__main__.py` is the same command line):

```bash
J=~/.m2/repository/org/apache/juneau/juneau-terminal/<v>/juneau-terminal-<v>.zip
wr=$(python3 "$J" --bin watch-run.sh) && [ -x "$wr" ] && "$wr" -d . -t push -- python3 scripts/push.py
```

A zip is extracted to `~/.cache/juneau-terminal/bin/<version>/` (`<version>` comes from `juneau-terminal-<version>.zip`, or is
`sha-` plus 12 hex digits of the file's hash for any other name). Each file is written to a temporary name and renamed
into place, so a script that is running keeps the file it opened, and an unchanged file is left alone. A later extract
removes files the zip no longer carries and temporary files over an hour old. The location is outside `target/` on
purpose: `mvn clean` would otherwise delete the cancel path of a run that is still going. A SNAPSHOT keeps one version
name, so a new build replaces the files under it.

### Requirements

`bash`, `tmux`, `ps` and `pgrep`, plus `perl` for the follower filter. `python3` (for the result summary) and `lsof`
(for the listening ports in the orphan report) are optional. Tested on macOS.

### Using them

```bash
"$wr" -d ~/git/some/repo -t "push" -- python3 scripts/push.py "message"
"$wr" --no-tail -- python3 scripts/release.py
```

Outside tmux (`$TMUX` unset) both scripts exit 2; run the command directly there and `tail -f` its log.

- `tmux-pane.sh` options: `-d DIR`, `-n LINES`, `-t TITLE`, `-k fail|always|never`, `-K MIN`, `--reap`,
  `--keep-on-cancel`, and `--pane STRING` (repeatable, with `--size N%`) for several panes. The subcommands are
  `cancel [PANE|TITLE|--all]`, `list`, `close [--kept|PANE]` and `equalize PANE [--closing]`; a command named like one of
  them goes after `--`.
- The leader's exit code is the script's exit code; 129 means the pane was closed under it, and 2 is a usage error or
  not being inside tmux. `cancel` sends INT, TERM, then KILL, each after `TMUX_PANE_GRACE` seconds (default 10), and
  reaps stragglers by recorded PID and start time. Without `--keep-on-cancel` a cancelled run closes its panes.
- When a run ends the script prints the last lines of the pane, a list of processes the command left running (with
  their listening ports), and `exit=N`. Panes are kept per `-k` until Enter in the leader, `-K` minutes, or `close`.
  Read the last `exit=` line.
- `watch-run.sh` closes kept panes from earlier runs before it opens a new one (running panes are untouched; `--no-close-kept`
  leaves them all open), prints
  `tail -f <log>` and `board: <board log>` first, and ends with a `result:` summary: status, exit, time, one line per
  row, notes and log paths.
- Widths: the pane the script was started from (the agent pane) and each run's column share the window equally (50/50,
  33/33/33, and so on). The columns are equalized when a leader opens and again when a run's panes close, through
  `equalize`. The agent pane is recorded in the `@tmux_agent` pane option. Other panes and windows are not touched, and
  a failed resize only warns. `list` prints one line per pane (id, title, state, age) and nothing when there are none.
- The follower pane shows the log through a filter, so the file is unchanged: U+2028, U+2029 and other format
  characters become a space (tmux and the outer terminal disagree on their width), control characters other than tab,
  newline and ESC, and DEL, are dropped, and invalid UTF-8 becomes U+FFFD. `watch-run.sh --print-tail-filter` prints it.

### Environment

Every pane gets `TMUX_PANE_RUN_ID` (the leader's pane id) and `TMUX_PANE_RUN_CANCEL` (the command that cancels the
run). `watch-run.sh` sets `JUNEAU_TERMINAL_FULL_LOG`, `JUNEAU_TERMINAL_BOARD_LOG`, `JUNEAU_TERMINAL_RESULT` (a file in a temporary
directory removed at exit) and `JUNEAU_TERMINAL_CANCEL`, and takes `JUNEAU_TERMINAL_LOG_DIR` (default `~/.cache/juneau-terminal`) for
where the logs go. These names are a contract with every pinned consumer: a rename keeps exporting the old name until
each consumer has refreshed its copy.

The tmux tests run on macOS, or anywhere with `JUNEAU_TERMINAL_TMUX_TESTS=1`, and skip themselves when no private tmux server
can start. A few slow ones run only with `JUNEAU_TERMINAL_TMUX_TESTS=1`.

## Driving a run from another tool

A tool that starts a script (an AI agent, an IDE task, a CI wrapper) can follow it without scraping the screen:
`JUNEAU_TERMINAL_MARKERS_FILE` collects the run's markers while the console view still draws for a person watching, and
`JUNEAU_TERMINAL_RESULT` gets the outcome as JSON when the run ends. The wrapper rules (ignore unknown events, a missing
`done` means the run failed) and a worked tmux example are in
[Driving a Run from Another Tool](https://juneau.apache.org/docs/topics/DrivingTheTerminal).

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
