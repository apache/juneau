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

## Nesting

`run()` sets `JUNEAU_RUN_ACTIVE=1` in the environment. A process that inherits it (a script spawned by the one that owns the run)
emits `step`, `end`, `report`, `note` and `tests` markers, but its `run()` and `done()` do nothing. The owner keeps emitting its own `done`.

## Adopting it

See `../README.md`: the bootstrap snippet that loads `juneau_run.py` from the pinned `org.apache.juneau:juneau-run` artifact, the output routing
options (`JUNEAU_RUN_CONSOLE`, `JUNEAU_RUN_FULL_LOG`, `JUNEAU_RUN_CONDENSED_LOG`) and the parsers (`maven`, `pytest`, `playwright`, `generic`).
Reporter flags a parser adds, only when markers are on and `RUN_ARTIFACTS` is set:
- pytest: `--junitxml=$RUN_ARTIFACTS/<step>-pytest.xml`
- Playwright: `--reporter=line,json` with `PLAYWRIGHT_JSON_OUTPUT_NAME=$RUN_ARTIFACTS/<step>-playwright.json`
