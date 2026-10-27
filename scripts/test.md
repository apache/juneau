# test.py

Builds Juneau, runs the Maven tests, then checks timings, runs the scripts' own pytest suite, and optionally the browser tests.

## When to use

- You want the standard local verification: a clean build plus the full test run.
- You want only the build (`--build-only`) or only the tests (`--test-only`).
- You want to profile one module's tests (`--profile`) or refresh the timing baselines (`--rebaseline`).
- `push.py` runs this script for its build and test steps.

## Usage

```bash
python3 scripts/test.py
python3 scripts/test.py --test-only --no-container
python3 scripts/test.py --build-only
python3 scripts/test.py --enforce-perf
python3 scripts/test.py --console board --warn-traces
python3 scripts/test.py --profile juneau-core/juneau-marshall
python3 scripts/test.py --rebaseline --window 20 --min-runs 5
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--build-only`, `-b` | none | off | Only build; skip the tests. |
| `--test-only`, `-t` | none | off | Only run the tests; no build. Cannot be combined with `--build-only`. |
| `--full`, `-f` | none | on when no other mode is given | Clean build, then the tests. |
| `--verbose`, `-v` | none | off | Accepted for compatibility. Maven output always streams live. |
| `--no-container` | none | off | Exclude tests tagged `container` (`-DexcludedGroups=container`). |
| `--timing-log` | path | none | Ignored, with a warning. Timings are in the run history; the option goes away in a later release. |
| `--enforce-perf` | none | off | Fail on a perf-guard breach instead of only warning. |
| `--rebaseline` | none | off | Rewrite the `suite` line and `[modules]` section of `perf-baseline.txt` from the run history, then exit without building. |
| `--window` | N (integer) | 20 | With `--rebaseline`: runs per key whose median is taken. A module whose window spreads by more than 50% takes the 75th percentile instead. |
| `--min-runs` | N (integer) | 5 | With `--rebaseline`: skip keys with fewer runs. |
| `--js-tests` | none | auto | Also run the headless-browser tests (`mvn -Pjs-tests`). Fails if Node or npm are missing. |
| `--no-js-tests` | none | off | Never run the browser tests. Cannot be combined with `--js-tests`. |
| `--warn-traces` | none | off | Report stack traces in the test output as a warning instead of failing the Traces row. Allowlist errors still fail. |
| `--console` | `full`, `board` or `none` | `full` | How Maven output appears on the console. |
| `--detail` | `summary`, `actionable`, `modules` or `all` | `actionable` | Console detail level. |
| `--full-log` | path | none | Also append everything Maven prints to this file. |
| `--board-log` | path | none | Also append the board one-line-per-event stream to this file. |
| `--profile` | module path | none | Run one module's tests under a one-shot JFR profile, then exit. |
| `--help`, `-h` | none | off | Print the usage text and exit 0. |

## What it does

1. Parses the options. Unknown options print the usage text and exit 1 (`_main`).
2. Build: `mvn clean install -T1C -DskipTests` (`build`).
3. Tests: `mvn test -T1C -Drat.skip=true`, plus `-DexcludedGroups=container` with `--no-container` (`test`). Per-module Surefire times and Maven's reactor times are recorded in the run history (`_record_buckets`).
4. Perf guard: compares the suite wall clock with the `suite` line of `perf-baseline.txt`, and each module's reactor time with its `[modules]` entry. The report is written to `target/perf-guard.txt` (`run_perf_guard`).
5. Python tests: runs `scripts/tests` with `python3 -m pytest`, or `uv run --native-tls --with pytest --with pyyaml pytest` when pytest is not importable (`script_tests`).
6. Browser tests: with `-Pjs-tests` in the views, petstore-core and terminal modules (`maybe_run_js_tests`). Without `--js-tests` or `--no-js-tests`, they run automatically when a `.js`, `.cjs`, `.css` or `.ftl` file under a `src/` tree differs from `origin/master`.
7. Traces: scans what steps 3 to 6 printed (read back from the run's full log, from a mark taken before the build) for stack traces, and fails on any whose header line is not matched by [`trace-allowlist.txt`](trace-allowlist.txt) (`run_trace_guard`). With the default `--console full` there is no full log, so the row is skipped with a warning; use `--console board`. A full log that cannot be read fails the row. The report of every trace, allowed or not, is `target/trace-guard.txt`.
8. `--profile` runs `mvn test -Drat.skip=true -Djacoco.skip=true` from the module's directory, with the JFR flags (including `stackdepth=1024`) appended to the module's own `-Dsurefire.extraArgs`. It writes one `hotspot-pid-<pid>-id-<n>-<time>.jfr` per forked JVM under `<repo>/target/profile-results/<module>-<timestamp>/`, and fails if Maven passes but no file exists (`profile`). Read a recording with `jfr print --json --stack-depth 300 <file>`; the default depth of 5 hides every Juneau frame.

## Output and exit codes

Maven output, a status line per step, and the perf-guard report. The run header and markers come from the shared run toolkit. A standalone run rings the terminal bell at the end; `JUNEAU_PUSH_NO_SOUND=1` silences it.

| Code | Meaning |
|---|---|
| 0 | Every requested step passed. |
| 1 | Conflicting or unknown options, a failed build or test run, or `--rebaseline` with history turned off. |
| 2 | An invalid `--detail` value. |
| 130 | Cancelled with Ctrl-C or SIGTERM. |
| other | The exit code of the failing Maven, perf-guard or pytest step. |

## Gotchas

- The perf guard only warns unless `--enforce-perf` is given. A suite breaches above baseline times `1 + JUNEAU_CI_PERF_THRESHOLD` (default 0.20). A module breaches only when it is over that and more than `JUNEAU_CI_PERF_MIN_SECONDS` (default 3) over its baseline. A module with no baseline entry warns and never fails.
- `JUNEAU_MVN_WRAPPER` adds a prefix to every `mvn` command, for example a lock script that serializes runs. The value is a command plus arguments, not a shell fragment: it is split like a shell would (so `'/a b/lock.sh' --x` works), then re-quoted word by word, so a path with spaces or `$`/`;` is never interpreted. An unquoted path with spaces also works, taken as the longest leading run of words that names an existing file. A word that is exactly `&&`, `||`, `;` or `|` stays a shell operator, so `lock.sh --x &&` still chains.
- `scripts/trace-allowlist.txt` holds one header regex per line followed by `# <reason>` (the reason is everything after the first ` # `, so it may hold ` # ` itself); a trace joined from two JavaScript errors in one console block is allowed only when every one of its headers is matched; a line with no reason, a bad regex or a duplicate regex fails the Traces row even with `--warn-traces`. An entry that matched nothing is reported as stale (a warning) only when the browser tests ran and neither `--no-container` nor `--no-js-tests` was given.
- The pytest step is skipped with a notice when neither pytest nor `uv` is available. Nothing is installed.
- The browser tests download Playwright and Chromium on the first run, so they need network access.
- `JUNEAU_TERMINAL_HISTORY=0` turns off the run history, which `--rebaseline` and `push-timings.py` depend on.
- `--profile` skips JaCoCo with `-Djacoco.skip=true`, because its `prepare-agent` goal rewrites `argLine` after any `-DargLine`. Upstream modules are not built or tested: the run starts in the module directory, not from the root with `-pl`.
- The default mode runs `mvn clean install`, which replaces the Juneau artifacts in `~/.m2`.

## See also

- `push.py`, which runs this script
- `push-timings.py`, which reads the run history
- `perf-required.py`
- `coverage.py`
