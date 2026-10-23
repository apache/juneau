# surefire-rollup.py

Totals the test counts and run times from a module's Surefire XML reports into core and container buckets.

## When to use

- You want to see how a module's test time splits between plain tests and tests that boot a Spring Boot or Jetty container, without reading the XML by hand.

## Usage

```bash
python3 scripts/surefire-rollup.py
python3 scripts/surefire-rollup.py juneau-integration-tests
```

## Options

| Option | Argument | Default | Meaning |
|---|---|---|---|
| `module` | path (optional positional) | `juneau-integration-tests` | Module directory, relative to the repository root, whose `target/surefire-reports` is read. |

## What it does

1. Resolves `<repo>/<module>/target/surefire-reports` (`main`).
2. If a `core/` or `container/` subdirectory exists there, sums the `tests` and `time` attributes of each `TEST-*.xml` in them (`parse_xml_dir`) and splits the container reports into Spring Boot and Jetty buckets (`parse_container_subbuckets`).
3. Otherwise treats the reports as one flat directory and assigns each report to a bucket (`parse_flat_reports`). A report is a container test when its source file mentions `SpringbootTest` or `JettyMicroserviceTest`, or when the class name contains `springboot`, `jetty` or `microservice`.
4. Prints one row per bucket with a bar, the test count and seconds.

## Output and exit codes

```
juneau-integration-tests/
  core                : ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ (1200 tests, 41.200s)
  container.springboot: ...
  container.jetty     : ...
  container           : ...
```

Bars are scaled to the largest bucket, up to 20 blocks. The `container` row is the sum of the two container rows.

| Code | Meaning |
|---|---|
| 0 | The report was printed, including when no reports exist (all rows are zero). |

## Gotchas

- Container test sources are looked up under `src/test/java` of the module, derived from the class name. A missing source falls back to the class-name hints.
- In the split `container/` layout, a report that matches neither framework is counted under `container.jetty`.
- A malformed report XML raises an uncaught parse error.

## See also

- `test.py`
- `push-timings.py`
