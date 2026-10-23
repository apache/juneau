# Run protocol v1 (`##run` markers)

The canonical text of the protocol. `juneau_run.py` produces it; `run-protocol-v1-golden.txt` and
`run-protocol-v1-golden-extensions.txt` are the conformance transcripts that `test_markers.py` replays.

Structured progress markers that a build/push script prints so a runner UI can show steps, test
results and notable events. Markers are additive: they never replace human-readable output.

## Wire format

- One marker per stdout line: the prefix `##run ` followed by a single-line JSON object.
- The object's `ev` field names the event. Fields whose value would be null are omitted.
- Markers are emitted **only** when the environment has `RUN_MARKERS=1`. With it unset, output is
  byte-for-byte what it was before adoption, with these exceptions: a script may make its test tools
  more verbose when markers are on, so per-suite result lines reach the runner; a clean cancel
  message and exit code 130 on Ctrl-C or SIGTERM; a clean `ERROR:` line instead of a traceback for
  git or tool errors; and a warning line when a commit was made but the push did not complete.
- A marker longer than 65536 bytes (UTF-8, including the prefix) is not emitted.
- Report files a script generates itself go under `$RUN_ARTIFACTS`, never inside the repo. A report
  that a build already writes to a gitignored directory (e.g. `target/surefire-reports`) is
  referenced in place when `$RUN_ARTIFACTS` is unset. When it is set, juneau-run copies the report
  to `$RUN_ARTIFACTS/reports/<step>/` and announces the copy, so a later `clean` cannot take it away.
  A name that is already taken there (another module, a rerun) gets a `-2`, `-3`, ... suffix.
  `run.cmd(..., reports=False)` (`run_tool(..., reports=False)`) announces no reports at all.

## Events

| `ev` | Fields | Meaning |
|---|---|---|
| `run` | `v` (always `1`), `mode` (`push`\|`test`), `project`, `branch`, `head` | First marker. |
| `step` | `id`, `n`, `title`, `parent`? | A step starts. `id` is unique within the run. `parent` is the enclosing step's id (see Extensions). |
| `end` | `id`, `status` (`ok`\|`fail`\|`skip`), `ms`, `exit`? | The step with that `id` ends. |
| `report` | `step`, `kind` (`surefire`\|`junitxml`\|`jest-json`\|`playwright-json`), `path` | Where a step's test results are. `path` is a file, directory or glob, relative to the repo root or `$RUN_ARTIFACTS` (repo root tried first), or absolute. Failsafe reports are announced with `kind` `surefire`; the XML schema is the same. |
| `note` | `level` (`info`\|`warn`\|`error`), `text`, `href`?, `step`? | A notable event (commit made, branch pushed, PR opened). `href` must be `http(s)`. |
| `tests` | `step`, `total`, `fail`, `err`, `skip` | Cumulative test totals for a step (see Extensions). |
| `done` | `status` (`ok`\|`fail`\|`cancelled`), `commit`? | Last marker. |

## Markers file and result file (still `v: 1`; additive)

Two ways for a tool that launches a script to follow the run while a person watches the console view. Neither changes
what stdout carries: with `RUN_MARKERS` unset the console view draws as usual.

### Markers file

`JUNEAU_RUN_MARKERS_FILE=<path>` sends every marker to that file as well, in the wire format above: one `##run {...}`
line each, the same 65536-byte limit (an oversized marker goes to neither), the same nesting rules.
- With `RUN_MARKERS=1` too, stdout and the file get the same markers.
- The process that owns the run empties the file when its `run()` starts, then writes `run` and, at the end, `done`.
  It exports the path made absolute, so nested scripts write the same file wherever they run.
- A nested script (one that inherits `JUNEAU_RUN_ACTIVE`) appends its `step`, `end`, `report`, `note` and `tests`
  markers. Each line is one append, so lines from several processes never interleave.
- The file replays through the consumer rules above. A reader that follows it while it grows treats a last line
  without its `\n` as not yet written.
- A file that can't be written is reported once on stderr; the run goes on without it.
- Under `--pty` (see "PTY mode") the markers file isn't written, and the job doesn't get the variable.

### Result file

`JUNEAU_RUN_RESULT=<path>`: when `done` ends the run (ok, failed, or cancelled by Ctrl-C or SIGTERM), the owner writes
one JSON object there, through a temp file in the same directory and a rename, so a reader never sees half of it.
The owner removes an older file at that path when its `run()` starts. Fields whose value would be null are omitted.

| Field | Meaning |
|---|---|
| `v` | Always `1`. |
| `status` | `ok`, `fail` or `cancelled`, as in `done`. |
| `exit` | The script's exit code (`130` when cancelled). |
| `ms` | The run's duration, from `run` to `done`. |
| `project`, `branch`, `head` | As in `run`. |
| `commit`? | As in `done`. |
| `rows` | The console view's rows, in the order they started, nested scripts' included: the top-level steps (child steps are not rows) and any rows a script draws without a marker (`row()`, such as Perf or Timing). Each: `id`, `label`, `status` (`ok`\|`fail`\|`skip`; a row with no `end` takes the `done` consumer rule's status), `ms`, `summary` (the row's text in the board log: its count and summary, totals or reason, possibly empty) and `tests`? `{total, fail, err, skip}` (the step's last totals). |
| `notes` | The run's `note` markers, in order: `level`, `text`, `href`?. |
| `logs` | `full`? and `board`?: absolute paths of the session's logs, when it has them. |

Nested scripts don't write the file. Each one leaves its rows and notes in a directory the owner names in
`JUNEAU_RUN_RESULT_PARTS` (made by the owner, removed when it has written the result); the owner merges them in.

## `--test-only`

An adopting `push.py` accepts `--test-only`:
- It runs only the build and test gates: the container-tags and BOM checks, the test run and the install build.
  There is no identity check, commit, push or docs follow-up, and the commit message is optional.
- It emits `run` with `mode: "test"`.
- It cannot be combined with `--docs-only`, `--sonarqube`, `--tracker-audit` or `--skip-tests`.

## Consumer rules

- Ignore an unknown `ev`.
- Treat a line with the prefix but bad JSON or no `ev` as an ordinary output line.
- When `done` arrives, close any step that has no `end`: with `fail` if `done.status` is `fail`, and
  with `skip` otherwise.
- If the stream reaches EOF with no `done` (for example the process was killed by SIGTERM or SIGKILL),
  treat it as `done` with status `fail` (or `cancelled` if the consumer itself cancelled the run), and
  close any open step accordingly.

## Extensions (still `v: 1`; additive)

- **`step.parent`** (optional) is the id of the enclosing step.
  - Sub-step ids are `<parent>/<child>`.
  - Sub-steps carry their own `n`, the reactor index, for ordering within the parent.
  - A script's own units are sub-steps too: a Prechecks row emits one `prechecks/<check>` child step per check.
  - A consumer that ignores `parent` shows sub-steps as flat steps, which is still correct.
- **`tests`:** `{ev:"tests", step, total, fail, err, skip}`.
  - The numbers are absolute cumulative totals for that step, not deltas. The latest event for a step supersedes earlier ones.
  - At most one per step every 500 ms, plus a final one before that step's `end`.
  - A parent step's totals are counted directly, not summed from its children.
- **Producer rule:** a producer emits `end` for every open child, children first, before the parent's `end`. The `done` consumer rule above therefore stays valid and unchanged.
- **Consumers ignore unknown fields on a known `ev`.**
- **Open output lines.**
  - **Producer rule:** a marker always starts on a fresh line. A producer that has written unterminated output first writes `\n`.
  - **Consumer rule:** text after the last `\n` is the open trailing output line. It is never parsed as a marker until it is
    terminated, and a consumer may show it while it grows.
  - **Bare `\r`:** a `\r` that is not part of `\r\n` rewrites the line. Consumers show only the text after the last bare `\r` of a
    line, open or closed, and ignore a trailing `\r`. Line parsers match that same last segment.
  - Scripts that write with `print(..., end="")` directly must end their line before the next marker. The `juneau_run.py` helpers
    (`open_line`, `append`, `dot`, `set_tail`, `close_line`) and its marker functions do this for you.

## Nesting

`run()` sets `JUNEAU_RUN_ACTIVE=1` in the environment. A process that inherits it (a script spawned by the one that owns the run)
emits `step`, `end`, `report`, `note` and `tests` markers, but its `run()` and `done()` do nothing. The owner keeps emitting its own `done`.

## PTY mode

`juneau_run.py --pty --full-log LOG --events EVENTS [--size COLSxROWS] [--input stdin] <tool> -- <cmd...>` runs the tool under a pseudo-terminal
of the given size (default `120x40`), so tools that colour, redraw or draw progress bars behave as they do in a terminal.
Every option comes before `<tool>`. POSIX only; anywhere else it exits 2. `--events` appends to its file, so pass a fresh path
for each run.

- **Log.** `LOG` gets the tool's bytes exactly as written. The PTY turns `\n` into `\r\n`. `LOG.size` gets `{"cols":C,"rows":R}`,
  written before the tool starts.
- **Events.** `EVENTS` gets run-view events, one JSON object per line. These are not `##run` markers, and they are written whether or
  not `RUN_MARKERS` is set:
  - `step` → `step {id,title,n,rawOffset}`. `parent` is dropped.
  - `end` → `end {id,status,ms,exit}`.
  - `tests` → a `suite` placeholder `{step,fw:<tool>,suite:<step>,counts:{pass,fail:fail+err,skip},rawOffset}`.
  - `note` → `note`.
  - The last line is always `done {status: ok|fail|cancelled}`.
  - `run` and `report` are not written.
  - Step ids are mapped onto `^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$`: `/` becomes `.`, so `mvn/mod-a` becomes `mvn.mod-a`.
- **`rawOffset`** is the byte offset in `LOG` of the start of the line whose parsing produced the event. A line longer than 1 MiB is force-split for parsing, and an event from its
  continuation carries the offset of the split, in the middle of the line.
- **Parsers** see each line with escape sequences and C0 controls removed, and only the text after its last `\r`.
- **The child** gets `TERM=xterm-256color` and no `RUN_MARKERS`.
- **Exit codes.** The tool's exit code passes through. A tool killed by signal *n* exits `128+n` and adds a `warn` note
  `<step> killed by SIG…`. A command that can't start exits 127.
- **Stdout.** `--console` defaults to `none`; `--console full` also copies the raw bytes to stdout.
- **Log bytes.**  Under `--pty` the full log is always the tool's own bytes.  Without a terminal, a parser may
  rewrite what the full log and console show for each line (`rewrites_log`); the board log and the markers are
  the same either way.

**PTY input.** `--input stdin` (with `--pty`, before `<tool>`; without `--pty` it exits 2) copies the wrapper's stdin to the PTY
master unchanged. EOF on stdin stops the copying but not the tool. The bytes reach `LOG` only if the terminal echoes them, so a
tool that turns echo off (a password prompt) keeps them out of the log. Control bytes act through the terminal's line discipline:
`^C` sends SIGINT to the tool's foreground process group (so an unhandled one exits 130 with a `killed by SIGINT` note), and
`^D` ends a read, or signals end of input at the start of a line. `^Z` sends SIGTSTP, but it does not stop a plain tool: the
tool leads its own session, so nothing could resume it, and the stop is discarded. A shell with job control of its own (`bash -i`)
does stop its foreground job. A tool that puts its terminal in raw mode gets the bytes as they are. Without `--input`, the
tool's terminal gets no input at all.

## Adopting it

See `../README.md`: the bootstrap snippet that loads `juneau_run.py` from the pinned `org.apache.juneau:juneau-run` artifact, the output routing
options (`JUNEAU_RUN_CONSOLE`, `JUNEAU_RUN_FULL_LOG`, `JUNEAU_RUN_BOARD_LOG`) and the parsers (`maven`, `pytest`, `playwright`, `gradle`, `npm`, `bazel`, `generic`; `gradle` adds `--console=plain` unless the command sets `--console`).
Reporters a parser adds when markers are on:
- pytest: `--junitxml=$RUN_ARTIFACTS/<step>-pytest.xml` (a temp directory when `RUN_ARTIFACTS` isn't set)
- Playwright: no flag. juneau-run's own reporter (`juneau_run_playwright.cjs`) is named in `PW_TEST_REPORTER`, and
  `JUNEAU_RUN_PLAYWRIGHT_JSON=$RUN_ARTIFACTS/<step>-playwright.json` when `RUN_ARTIFACTS` is set

Playwright appends the `PW_TEST_REPORTER` reporter to the `reporter:` list in `playwright.config` (or to its default
reporter), so the config's own reporters (html, junit, ...) still run and Playwright prints what a plain run prints,
plus, outside a terminal, the reporter's `##playwright` lines. It sends one per test event, which draws one console line
per running test file and counts the step's bar (once per test, however often it is retried), and writes the JSON report
at the end in a subset of Playwright's json reporter format (suites, specs, tests, results). The bar's total is the
number of tests at the start, so a run stopped by `--max-failures` ends short of it; a test whose retry never ran is
counted at the end with its outcome so far. Outside a terminal `PLAYWRIGHT_HTML_OPEN=never` is set too (unless the
command sets it or `PW_TEST_HTML_REPORT_OPEN`): the html reporter's default opens the report after a failure, serving it
and waiting, whenever stdin is a terminal. `PW_TEST_REPORTER` is undocumented in Playwright: a version that ignores it
runs as a plain run would, without the per-file lines or the JSON report, and once tests have run (not after `--list`,
`--help` or a configuration error) the step notes "Playwright reporter not loaded"; the bar then counts the line
reporter's `[n/N]` progress, when the config uses it. Under a terminal there is no such note.
Without markers, the reporter is added only inside a console session that isn't interactive (a script's, or the one
`juneau_run.py --console board` draws on a terminal); a plain run keeps the command and environment as given.
Under a terminal it is added only for the JSON report, with `JUNEAU_RUN_PLAYWRIGHT_MARKERS=0`, so it prints nothing;
outside one an inherited `JUNEAU_RUN_PLAYWRIGHT_MARKERS=0` is dropped. `JUNEAU_RUN_PLAYWRIGHT_REPORTER=0`, in the command's environment or juneau-run's, leaves it out, and so does a
command that names its own reporter (`--reporter x`, `--reporter=x`, or a `PW_TEST_REPORTER` already set). A report left
at the JSON path by an earlier run is removed first, so a run cancelled before its end reports none.
