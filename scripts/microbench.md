# microbench.py

Runs the JMH micro-benchmarks in `juneau-integration-tests` through Maven and checks or reports the results.

## When to use

- You changed a hot path and want to confirm the observability no-op block still allocates nothing (`observability`).
- You want ops/s and allocation numbers for serialization, parsing, class metadata and a mock REST round trip (`runtime`).

## Usage

```bash
python3 scripts/microbench.py observability
python3 scripts/microbench.py observability --threshold 0
python3 scripts/microbench.py runtime --quick
python3 scripts/microbench.py runtime --include 'Json.*' --forks 1 --jfr
python3 scripts/microbench.py --verbose runtime
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `--verbose`, `-v` | none | off | Stream the full Maven output instead of only its tail. Goes before the subcommand. |
| `observability` | subcommand | required choice | Run `ObservabilityNoopBenchmark` and assert `gc.alloc.rate.norm` is within the threshold. |
| `runtime` | subcommand | required choice | Run the runtime suite in `org.apache.juneau.microbench.runtime`. |
| `--threshold` | BYTES (float) | 8.0 | `observability` only. Maximum allowed `gc.alloc.rate.norm` in bytes per operation. |
| `--include` | REGEX | the whole suite | `runtime` only. JMH include regex. May be repeated. |
| `--forks` | N (integer) | per-class value | `runtime` only. Override the fork count. |
| `--warmup` | N (integer) | per-class value | `runtime` only. Override the warmup iteration count. |
| `--iterations` | N (integer) | per-class value | `runtime` only. Override the measurement iteration count. |
| `--time` | SECONDS (integer) | per-class value | `runtime` only. Override the warmup and measurement iteration time. |
| `--quick` | none | off | `runtime` only. Smoke run: 1 fork, 2 measurement iterations, 1 second iterations. |
| `--jfr` | none | off | `runtime` only. Also record a JFR file per benchmark (JMH `-prof jfr`). |
| `--out-dir` | DIR | `juneau-integration-tests/target/jmh-results` | `runtime` only. Where results are written. |

## What it does

1. `observability`: runs `mvn -f juneau-integration-tests/pom.xml -Pmicrobench test-compile exec:java` with the benchmark class as `exec.mainClass` (`run_benchmark`, `cmd_observability`).
2. Reads the newest `juneau-integration-tests/jmh-results/observability-*.json`, extracts the `gc.alloc.rate.norm` secondary metric, and compares it with `--threshold`.
3. `runtime`: runs `mvn -pl juneau-integration-tests -Pmicrobench-runtime process-test-classes -Dmicrobench.args=...` from the reactor, so it measures this checkout's code and needs no earlier `mvn install` (`cmd_runtime`).
4. The runtime results go to `<out-dir>/runtime-<date>-<time>.json`, and with `--jfr` the recordings go to `<out-dir>/jfr-<date>-<time>/<benchmark>/`.
5. Prints ops/s (or time per operation) and `gc.alloc.rate.norm` per benchmark.

## Output and exit codes

A pass or fail line for the allocation check, and the benchmark table for `runtime`. Without `--verbose`, only the tail of the Maven output is shown.

| Code | Meaning |
|---|---|
| 0 | The allocation rate was within the threshold, or the runtime suite ran. |
| 1 | The rate exceeded the threshold, Maven failed, or no result file could be read. |

## Gotchas

- `--verbose` is a top-level option, so it must come before `observability` or `runtime`.
- The threshold default is 8 bytes per operation; the source notes it can be tightened to 0 once confirmed.
- `observability` writes under `juneau-integration-tests/jmh-results/`, while `runtime` writes under `target/`, so runtime runs leave no untracked files.
- `--warmup` also gives `ColdStartBenchmark` warmups, which defeats it. Exclude that benchmark with `--include` when overriding warmup.
- To record a new baseline, run the `observability` check and update the `[observability]` section of `perf-baseline.txt` at the project root by hand.
- A benchmark run can take many minutes. Use `--quick` for a smoke run.

## See also

- `perf-required.py`
