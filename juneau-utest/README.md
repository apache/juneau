# juneau-utest

Apache Juneau unit + integration test module.

## Running Tests

```bash
# Full clean build + all tests (recommended before a push)
python3 scripts/test.py --full

# Tests only (skip Maven compile)
python3 scripts/test.py --test-only

# Single module with Maven
mvn -f juneau-utest/pom.xml test
```

## Benchmarks

JMH micro-benchmarks live in `src/test/java/.../microbench/` and are **excluded from the normal
test run** (Surefire only picks up `**/*Test.class`).  Run them explicitly via the wrapper script
or Maven directly.

### Observability NoOp benchmark (TODO-116)

Asserts the `RestOpInvoker` observability hot path — `MetricsRecorder` + `TracerHook` resolved
to their NoOp singletons — allocates **zero objects per invocation**.

**Run via script (recommended):**

```bash
python3 scripts/microbench.py observability
```

The script compiles the benchmark, runs JMH with the GC allocation profiler (`-prof gc`), reads
the JSON output, and asserts `gc.alloc.rate.norm ≤ 8 bytes/op`.  Exit code 0 = pass.

**Custom threshold:**

```bash
python3 scripts/microbench.py observability --threshold 0
```

**Run directly via Maven:**

```bash
mvn -f juneau-utest/pom.xml -Pmicrobench test-compile exec:java \
    -Dexec.mainClass=org.apache.juneau.marshall.microbench.observability.ObservabilityNoopBenchmark
```

**JSON results** are written to `juneau-utest/jmh-results/observability-YYYY-MM-DD.json`.

### Performance baseline

The recorded baseline lives in `perf-baseline.txt` (project root) under the `[observability]`
section.  (The file was relocated from `juneau-utest/perf-baseline.txt` to the project root by
TODO-160 so the per-module test-perf guard can track every reactor module.)  To update after an
intentional regression is accepted:

1. Run `python3 scripts/microbench.py observability` and note the `gc.alloc.rate.norm` value.
2. Edit the `[observability]` section in `perf-baseline.txt` manually.

### Adding new benchmarks

1. Place the class in `src/test/java/org/apache/juneau/microbench/<area>/`.
2. Annotate with `@Benchmark`, `@State`, `@BenchmarkMode`, etc.
3. Add a `public static void main(String[] args)` using `OptionsBuilder` with `forks(0)`
   (required for the `exec:java` invocation to work).
4. Add a subcommand to `scripts/microbench.py`.
5. Document in this file and in `perf-baseline.txt`.
