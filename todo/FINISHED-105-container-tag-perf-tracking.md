# FINISHED-105 — Container-test tag isolation + perf-regression tracking + profiling pass

Source plan: [`TODO-105-container-tag-perf-tracking.md`](TODO-105-container-tag-perf-tracking.md).

## Summary

TODO-105 shipped the regression-detection and test-bucketing infrastructure requested after the
FINISHED-79 slowdown incident. The landing keeps `juneau-utest` as a single Maven module while
making container-heavy tests explicit and measurable.

## What landed

1. **Container-tag meta-annotations and adoption.**
   - Added `@SpringbootTest` and `@JettyMicroserviceTest` in
     `juneau-utest/src/test/java/org/apache/juneau/testing/annotations/`.
   - Tagged the inventoried container-booting test classes (Spring Boot + Jetty/Microservice)
     so they can be filtered as `container`, `springboot`, and `jetty`.

2. **Dual Surefire execution buckets in `juneau-utest/pom.xml`.**
   - Core execution excludes `container` and writes to
     `target/surefire-reports/core/`.
   - Container execution includes `container` and writes to
     `target/surefire-reports/container/`.
   - JaCoCo remains a single `.exec` workflow (no coverage-script changes required).

3. **Container-tag discipline guard in push flow.**
   - Added `scripts/check-container-tags.py`.
   - Wired the guard into `scripts/push.py` before tests, so untagged container-booting tests
     fail fast during `/push`.

4. **Per-execution push timing capture and reporting.**
   - Added `scripts/test.py --timing-log ...` plumbing to write JSONL records.
   - Added `scripts/push-timings.py` to print rolling-median timing deltas per execution.
   - `scripts/push.py` now captures and prints timing report output after tests.

5. **Surefire XML rollup utility.**
   - Added `scripts/surefire-rollup.py` for bucketed wall-clock and test-count summaries over
     `core`, `container`, `container.springboot`, and `container.jetty`.

6. **One-shot profiling support.**
   - Added `scripts/test.py --profile <module>` with JFR output under
     `target/profile-results/`.
   - Profiling findings are intentionally tracked as follow-on optimization work in TODO-999,
     not implemented in this landing.

## Scope boundaries respected

- `juneau-utest` was not split into new Maven modules.
- No blocking policy was added for timing regressions; reporting remains warning-only.
- Existing coverage scripts continue to use `juneau-utest/target/jacoco.exec`.

## Follow-ups

- Optimization candidates discovered via profiling remain queued under TODO-999.
