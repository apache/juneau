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
| `--console` | `full`, `condensed` or `none` | `full` | How Maven output appears on the console. |
| `--detail` | `summary`, `actionable`, `modules` or `all` | `actionable` | Console detail level. |
| `--full-log` | path | none | Also append everything Maven prints to this file. |
| `--condensed-log` | path | none | Also append the condensed one-line-per-event stream to this file. |
| `--profile` | module path | none | Run one module's tests under a one-shot JFR profile, then exit. |
| `--help`, `-h` | none | off | Print the usage text and exit 0. |

## What it does

1. Parses the options. Unknown options print the usage text and exit 1 (`_main`).
2. Build: `mvn clean install -T1C -DskipTests` (`build`).
3. Tests: `mvn test -T1C -Drat.skip=true`, plus `-DexcludedGroups=container` with `--no-container` (`test`). Per-module Surefire times and Maven's reactor times are recorded in the run history (`_record_buckets`).
4. Perf guard: compares the suite wall clock with the `suite` line of `perf-baseline.txt`, and each module's reactor time with its `[modules]` entry. The report is written to `target/perf-guard.txt` (`run_perf_guard`).
5. Python tests: runs `scripts/tests` with `python3 -m pytest`, or `uv run --native-tls --with pytest pytest` when pytest is not importable (`script_tests`).
6. Browser tests: with `-Pjs-tests` in the views, petstore-core and terminal modules (`maybe_run_js_tests`). Without `--js-tests` or `--no-js-tests`, they run automatically when a `.js`, `.cjs`, `.css` or `.ftl` file under a `src/` tree differs from `origin/master`.
7. `--profile` runs `mvn test -pl <module> -Drat.skip=true` with a JFR `argLine` and writes `target/profile-results/<module>-<timestamp>.jfr` (`profile`).

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
- `JUNEAU_MVN_WRAPPER` adds a prefix to every `mvn` command, for example a lock script that serializes runs.
- The pytest step is skipped with a notice when neither pytest nor `uv` is available. Nothing is installed.
- The browser tests download Playwright and Chromium on the first run, so they need network access.
- `JUNEAU_RUN_HISTORY=0` turns off the run history, which `--rebaseline` and `push-timings.py` depend on.
- `--profile` replaces `argLine`, which drops the JaCoCo agent so it does not skew the profile.
- The default mode runs `mvn clean install`, which replaces the Juneau artifacts in `~/.m2`.

## See also

- `push.py`, which runs this script
- `push-timings.py`, which reads the run history
- `perf-required.py`
- `coverage.py`
