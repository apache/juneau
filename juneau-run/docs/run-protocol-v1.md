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
  referenced in place.

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

## `--test-only`

An adopting `push.py` accepts `--test-only`:
- It runs only the build and test gates: the container-tags and BOM checks, the test run, the install build and the
  starter verification. There is no identity check, commit, push or docs follow-up, and the commit message is optional.
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

`juneau_run.py --pty --full-log LOG --events EVENTS [--size COLSxROWS] <tool> -- <cmd...>` runs the tool under a pseudo-terminal
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

## Adopting it

See `../README.md`: the bootstrap snippet that loads `juneau_run.py` from the pinned `org.apache.juneau:juneau-run` artifact, the output routing
options (`JUNEAU_RUN_CONSOLE`, `JUNEAU_RUN_FULL_LOG`, `JUNEAU_RUN_CONDENSED_LOG`) and the parsers (`maven`, `pytest`, `playwright`, `generic`).
Reporter flags a parser adds, only when markers are on and `RUN_ARTIFACTS` is set:
- pytest: `--junitxml=$RUN_ARTIFACTS/<step>-pytest.xml`
- Playwright: `--reporter=line,json` with `PLAYWRIGHT_JSON_OUTPUT_NAME=$RUN_ARTIFACTS/<step>-playwright.json`
