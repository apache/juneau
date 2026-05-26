# TODO-105: Container-test tag isolation + perf-regression tracking + profiling pass

Source: TODO.md headline bullet expanded 2026-05-26; pivoted same day from a module-split approach to a JUnit-5 tag-isolation approach after a revisit showed the tag mechanism solves the same goal without complicating the single-`.exec` JaCoCo workflow.

## Goal

Build the automatic regression-detection seam the FINISHED-79 incident proved we need (a `RestContext.build()` → `Settings.get().addSource(...)` bridge leaked `ConfigPropertySource` instances into a process-wide singleton via `MockRestClient`'s static `RestContext` cache, blowing the utest suite from ~35s to ~560s; caught only by a human noticing the slowdown), then take a one-shot profiling pass to file optimization candidates into TODO-999.

Four sub-deliverables:

- **(a) Container-test tag isolation.** Introduce JUnit-5 meta-annotations (`@SpringbootTest`, `@JettyMicroserviceTest`) bundling `@Tag("container")` + a container-type sub-tag. Annotate the ~22 container-booting classes. Add a dual `<execution>` block to Surefire so `mvn test` runs core-only AND container suites separately with their own `reportsDirectory`. Single Maven module, single JaCoCo `.exec`, no coverage-workflow churn.
- **(b) Per-execution timing capture in `/push`.** Enrich `scripts/push.py` so every `/push` appends a per-Surefire-execution wall-clock + test-count record to a `.push-timings.jsonl` log; add a reporter that flags >20% deltas from the rolling median, separately per execution (core vs container).
- **(c) One-shot profiling pass.** `scripts/test.py --profile <module>` plumbing via JFR (or async-profiler). Run over the heaviest modules; file findings into [`TODO-999-final-prerelease-cleanup.md`](TODO-999-final-prerelease-cleanup.md)'s "Working notes" section.
- **(d) Surefire XML rollup with container-type bucketing.** Python script reads `target/surefire-reports/core/` + `target/surefire-reports/container/` (the dual-execution output dirs from deliverable a) and produces a per-bucket wall-clock table (`core` / `container.springboot` / `container.jetty`) per push.

## Implementation status (2026-05-26)

- [x] Completed and archived as [`FINISHED-105-container-tag-perf-tracking.md`](FINISHED-105-container-tag-perf-tracking.md).
- [x] Phase 1 implemented: container meta-annotations added, 22 inventoried classes tagged, dual-execution Surefire split in `juneau-utest/pom.xml`, and `scripts/check-container-tags.py` wired into `scripts/push.py`.
- [x] Phase 2 implemented: `scripts/test.py --timing-log` writes per-execution JSONL metrics to `~/.cache/juneau-push-timings/<branch>.jsonl`; `scripts/push.py` runs `scripts/push-timings.py` and always prints timing numbers with >5% warning banners.
- [x] Phase 3 implemented: `scripts/surefire-rollup.py` aggregates `core`, `container`, `container.springboot`, and `container.jetty` buckets from Surefire XML.
- [x] Phase 4 support implemented: `scripts/test.py --profile <module>` emits one-shot JFR recordings under `target/profile-results/`.

## Why

- **The FINISHED-79 incident**: the perf regression sat in master for an unknown window before a human noticed `./scripts/test.py` was suddenly slow. No automated detection. The root cause turned out to be a process-wide singleton interaction (`MockRestClient`'s static `RestContext` cache + `Settings.get().addSource(...)` from `RestContext.build()`), invisible to per-test timing because *every* test slowed down proportionally.
- **Today's `scripts/push.py` captures pass/fail only** — no wall-clock, no per-module breakdown. The signal needed to catch FINISHED-79-class regressions exists in Surefire output but is never persisted.
- **The container-booting tests are a small, slow tail.** Spring Boot context init + Jetty bind-port-and-handshake takes seconds per test class; a regression in core marshall code that's amortized over ~1237 fast in-process tests gets visually washed out by the ~22 slow container tests in the combined timing. **Isolating** the slow tail (without physically moving its classes) lets the core suite's wall-clock be a sensitive signal again.
- **`MockRest`-based tests are NOT container tests.** They run in-process (Servlet API surface mocked via `juneau-rest-mock`), no socket bind, no servlet container. They stay in the core bucket.

## Why tags instead of a Maven-module split

Considered both. Comparison summary that drove the pivot:

| Concern | Module split | JUnit `@Tag` (this plan) |
|---|---|---|
| JaCoCo coverage | Two `.exec` files; must merge via `mvn jacoco:merge` or change `scripts/coverage.py` | **Unchanged** — single `.exec`, `scripts/coverage.py` untouched |
| Maven dep graph | New POM, repackage shared test utils, possibly extract a `juneau-utest-common` | **Unchanged** — single module |
| Classpath / IDE config | Two test source roots, two run configs | **Unchanged** — single source root |
| Eclipse Run-As JUnit ergonomics | Pick the right project before launching | Tags-tab include/exclude works in any test launcher |
| Per-bucket timing isolation | Surefire reports naturally bucketed by module | Two Surefire `<execution>` blocks → bucketed `reportsDirectory` |
| Initial migration cost | ~22 file moves + new POM + dep wiring | ~22 one-line annotation additions |
| Reversibility | Painful to undo | Trivial — delete the annotations |
| Future drift risk | Module boundary enforces physically | Soft — relies on a discipline-guard script (`scripts/check-container-tags.py`) |

The discipline-drift risk is the only real cost; closing it with a guard script is sub-second and runs in `scripts/push.py` as a precondition. Everything else is strictly cheaper.

## Non-goals

- **Not splitting `juneau-utest` into multiple Maven modules.** Single module stays single module. JaCoCo `.exec`, `scripts/coverage.py`, dependency graph all untouched.
- **Not implementing the optimization fixes** surfaced by the Phase 4 profiling pass. Those land via TODO-999's release-cleanup batch. TODO-105 ships the infrastructure; TODO-999 ships the fixes. Hard scope boundary — this PR must not grow optimization patches.
- **Not changing test-framework versions or test idioms.** Annotate existing tests, don't rewrite them.
- **Not splitting `org.apache.juneau.rest.*` MockRest tests out** of the core bucket. They stay in `!container`. MockRest is in-process.
- **Not splitting REST-client tests** (rest/client + http/classic) out of the core bucket. They use MockRest, in-process.
- **Not gating `/push` on a regression flag.** The reporter is warning-only. A >20% delta prints a banner; it does not block the commit. False positives kill the signal otherwise.
- **Not building a long-term timing-trend dashboard.** Just the rolling-median delta detection.

## Current test landscape (inventory baseline, captured 2026-05-26 at SHA `ba158b21ebf2589df375f0ff6d0defdb0ce85282`)

Total `*Test.java` under `juneau-utest/src/test/java`: **1259**.

### Container-bound tests (the annotation scope)

**Spring Boot (10 classes using `@SpringBootTest` or equivalent boot):**

| Class | Naming form |
|---|---|
| `rest/convention/BasicVersionResource_Springboot_Test.java` | clean suffix |
| `rest/ops/BasicEchoResource_Springboot_Test.java` | clean suffix |
| `rest/staticfiles/BasicStaticFilesResource_Springboot_Test.java` | clean suffix |
| `rest/docs/BasicApiDocs_Springboot_Test.java` | clean suffix |
| `rest/springboot/RestPathsRuntimeOverride_Springboot_Test.java` | clean suffix |
| `rest/staticfiles/BasicStaticFilesResource_SpringbootMetaInf_Test.java` | **drift** — `_SpringbootMetaInf_Test` |
| `rest/springboot/SpringEnvironmentPropertySource_SpringbootIntegration_Test.java` | **drift** — `_SpringbootIntegration_Test` |
| `rest/docs/BasicApiDocs_Springboot_MultiOpenApiProvider_Test.java` | **drift** — `_Springboot` mid-name, qualifier suffix |
| `rest/springboot/SpringEnvironmentPropertySource_Test.java` | **drift** — `@SpringBootTest` annotation, no `Springboot` in class name |
| `rest/springboot/SpringBeanStore_Test.java` | **drift** — `@SpringBootTest` annotation, no `Springboot` in class name |

(5 clean / 5 drift.)

**JettyMicroservice / Microservice (12 classes booting embedded Jetty via `JettyMicroservice` or `Microservice` builder):**

| Class | Naming form |
|---|---|
| `rest/convention/BasicVersionResource_JettyMicroservice_Test.java` | clean suffix |
| `rest/ops/BasicEchoResource_JettyMicroservice_Test.java` | clean suffix |
| `rest/staticfiles/BasicStaticFilesResource_JettyMicroservice_Test.java` | clean suffix |
| `rest/docs/BasicApiDocs_JettyMicroservice_Test.java` | clean suffix |
| `microservice/Microservice_Builder_Test.java` | **drift** — boots, class-name-as-SUT convention |
| `microservice/Microservice_Inject_Test.java` | **drift** — same |
| `microservice/Microservice_Listener_Fanout_Test.java` | **drift** — same |
| `microservice/Microservice_OverridingBeanStore_Test.java` | **drift** — same |
| `microservice/Microservice_PushPopOverlay_Test.java` | **drift** — same |
| `microservice/jetty/JettyConfiguration_Test.java` | **drift** — same |
| `microservice/jetty/Rest_Paths_Test.java` | **drift** — same |
| `microservice/jetty/RestPathsRuntimeOverride_JettyMount_Test.java` | **drift** — `_JettyMount_Test` |

(4 clean / 8 drift.)

**Microservice/* tests confirmed NOT container-bound (stay in `!container`):** `BasicMicroserviceListener_Test`, `LogConfig_Test`, `LogParser_Test`, `LogEntryFormatter_Test`, `ConsoleCommand_Test`, `LogsResource_Action_Test`, `LogsResource_PathTraversal_Test`, `DirectoryResource_Action_Test`, `DirectoryResource_PathTraversal_Test`, `microservice/jetty/JettyLogger_Test`.

### Annotation-scope summary

- **Classes that get `@SpringbootTest`: 10.**
- **Classes that get `@JettyMicroserviceTest`: 12.**
- **Classes left untagged (default `!container` bucket): ~1237.**
- **Naming-convention drift: 13 of 22** (59%) — those classes carry container semantics but don't follow the `_<Container>_Test.java` suffix. OQ #8 asks whether to rename them as a one-time consistency pass.

### Push / test / coverage script integration seams

- **`scripts/push.py`** runs `python3 scripts/test.py --full` once (currently lines 442-468) and inspects only the exit code. The natural insertion points for deliverable (b) and the discipline guard are *before* and *after* the test invocation respectively — `check-container-tags.py` runs before tests as a precondition; the timing-log writer runs after tests complete.
- **`scripts/test.py`** is the natural home for `--profile <module>` (deliverable c) and `--timing-log <path>` (deliverable b). It already shells `mvn test -Drat.skip=true`; the `--profile` variant becomes `mvn test -pl <module> -Drat.skip=true -DargLine='-XX:StartFlightRecording=...'`. The seam is the existing `run_command(...)` helper.
- **`scripts/coverage.py`** reads exactly one `.exec` file: `juneau-utest/target/jacoco.exec`. **Unchanged by this plan.** The dual Surefire `<execution>` writes to the same `.exec` (JaCoCo appends across executions by default).

## Meta-annotation design

Two sibling typed meta-annotations under `juneau-utest/src/test/java/org/apache/juneau/testing/annotations/`:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("container")
@Tag("springboot")
public @interface SpringbootTest {}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("container")
@Tag("jetty")
public @interface JettyMicroserviceTest {}
```

Naming rationale: matches the existing `_Springboot_Test` / `_JettyMicroservice_Test` class-suffix convention so the annotation reads as a natural alias for the class name. The two-annotation pattern (not a parameterized enum) is forced by Java: a custom annotation with an enum `value()` cannot dynamically derive a second `@Tag` from its argument, so the sub-tag must be statically declared.

Adoption pattern:

```java
@SpringbootTest
public class BasicVersionResource_Springboot_Test { ... }

@JettyMicroserviceTest
public class BasicEchoResource_JettyMicroservice_Test { ... }
```

JUnit-5 tag-discovery (`AnnotationUtils.findRepeatableAnnotations(testClass, Tag.class)`) walks meta-annotation hierarchies, so `@SpringbootTest` matches a filter on `container`, `springboot`, or `container & springboot` identically to a direct `@Tag` would. Confirmed compatibility:

- **Surefire** (`<groups>` / `<excludedGroups>`).
- **Eclipse Run-As → JUnit Test → Tags tab** (passes the expression through to the JUnit-platform `Launcher` directly).
- **IntelliJ Run Configurations → Test kind: Tags** (same mechanism).

### Developer ergonomics enabled

CLI:
- `mvn test` — runs both executions (core + container).
- `mvn test -DexcludedGroups=container` — core-only (fast inner loop, ~30-something seconds today).
- `mvn test -Dgroups=container` — container-only.
- `mvn test -Dgroups=springboot` — just Spring Boot tests.
- `mvn test -Dgroups=jetty` — just Jetty Microservice tests.
- `mvn test -Dgroups='container & jetty'` — boolean expression.
- `./scripts/test.py --no-container` — convenience wrapper (sets `-DexcludedGroups=container`).

Eclipse: Run As → JUnit Test → Run Configurations → Tags tab → enter `container` in Exclude (or any expression). Re-runs honor the saved filter.

IntelliJ: Run Configurations → Test kind: Tags → enter `container` in Exclude.

## Resolved design decisions

_All eight OQAs answered 2026-05-26. Verbatim originals preserved below for archive value — each carries an inline **Resolved** line._

1. **Meta-annotation naming.** `@SpringbootTest` + `@JettyMicroserviceTest` (matches existing class-suffix convention, longer) vs `@Springboot` + `@Jetty` (shorter, but `@Jetty` is ambiguous — could be misread as a generic Jetty marker rather than "this test boots `JettyMicroservice`") vs `@SpringbootContainerTest` + `@JettyMicroserviceContainerTest` (explicit but verbose).
   - **Resolved 2026-05-26**: `@SpringbootTest` + `@JettyMicroserviceTest` (default, suffix-aligned).
2. **Push-timings log location.** Three flavors:
   - **`target/.push-timings.jsonl`** (gitignored under existing `**/target/` rule) — per-developer, no sharing.
   - **`scripts/.push-timings.jsonl`** (checked in) — team-shared rolling history; every push grows the log → noisy diffs + merge conflicts.
   - **`~/.cache/juneau-push-timings/<branch>.jsonl`** (out-of-repo) — per-developer, branch-keyed, no repo footprint.

   The "team-shared" flavor catches a regression *another* developer introduces; the per-developer flavors only catch regressions you yourself just pushed.
   - **Resolved 2026-05-26**: out-of-repo (`~/.cache/juneau-push-timings/<branch>.jsonl`).
3. **Regression-detection threshold.** Originally proposed >20% delta from rolling median.
   - Per-execution (core vs container, separately) or global?
   - Adjustable knob?
   - Warn-only on first delta, escalate on N consecutive?
   - What threshold value is reasonable?
   - **Resolved 2026-05-26**:
     - **Threshold = 5%** (default `JUNEAU_PUSH_TIMING_THRESHOLD=0.05`). Tighter than the originally-proposed 20% because catching subtler regressions early is the goal — even a 5% perf creep across a few PRs is signal worth surfacing.
     - **Per-execution** (core vs container, independent thresholds).
     - **Warn-only always.** No escalation tier for v1.
     - **Always print the numbers** at the end of every `/push`, regardless of whether the threshold is breached. The threshold only controls whether a ⚠ warning banner is emitted ON TOP of the numbers. Per-execution timing report format:
       ```
       📊 Push-timing report (last 20 runs on this branch):
          juneau-utest/core     : 28.4s  (median 27.9s, delta +1.8%)
          juneau-utest/container: 18.6s  (median 18.2s, delta +2.2%)
       ```
       And when threshold breached:
       ```
       📊 Push-timing report (last 20 runs on this branch):
          juneau-utest/core     : 41.2s  (median 27.9s, delta +47.7%)
          ⚠ juneau-utest/core wall-clock 41.2s is +47.7% above rolling median 27.9s (last 20 runs, threshold 5%)
          juneau-utest/container: 18.6s  (median 18.2s, delta +2.2%)
       ```
4. **Profiling tooling pick.**
   - **JFR** — bundled with JDK, no install, slightly less detail on native frames, JSON/binary `.jfr` viewer in JDK Mission Control.
   - **async-profiler** — better flame graphs, native installer required (`brew install async-profiler` on macOS).
   - **Resolved 2026-05-26**: JFR. Drop in async-profiler ad hoc if the v1 pass surfaces a native-frame-shaped problem JFR can't see clearly.
5. **Profiling output destination.** `target/profile-results/<module>-<ts>.jfr` (gitignored under `**/target/`) vs upload to a shared store.
   - **Resolved 2026-05-26**: local-only (`target/profile-results/<module>-<ts>.jfr`).
6. **Sequencing relative to TODO-94a/b/c** (auth-filter framework). Original plan said "suggested before TODO-94a/b/c land" so the auth-filter PRs land with regression-tracking in place. The narrower tag-based scope makes that sequencing low-cost — TODO-105 is small enough to land in days.
   - **Resolved 2026-05-26**: before TODO-94a/b/c. Update TODO.md execution order accordingly when TODO-105 lands (TODO-105 already sits earlier in Phase E than TODO-94 in Phase F, so the existing order already reflects this).
7. **Container-detection heuristics in `check-container-tags.py`.** The guard script needs to identify a "container-booting test" without the tag. Proposed signals (the script flags any class that matches one but is not tagged):
   - Class has `@SpringBootTest` annotation (any of its forms — `@SpringBootTest`, `@AutoConfigureMockMvc`, etc.).
   - Class has `@ExtendWith(SpringExtension.class)`.
   - Class body or any nested class calls `JettyMicroservice.create()` / `JettyMicroservice.builder()`.
   - Class body calls `Microservice.create()` / `Microservice.builder()` (excluding the legitimate `microservice/Microservice_*_Test` cases which actually want the tag).
   - Class `extends` a known container-boot base class (none today, but TBD list).
   - **Resolved 2026-05-26**: heuristic list above accepted as-is. Phase 1 also adds raw `org.springframework.boot.SpringApplication.run(...)` and raw `org.eclipse.jetty.*.Server(...)` instantiation patterns to the scan to future-proof against bypass.
8. **Drift-cleanup rename pass.** Should Phase 1 also rename the 13 drift-cased classes to consistent `_Springboot_Test` / `_JettyMicroservice_Test` suffixes (for IDE-grep friendliness + signaling consistency), or leave the names as-is since the tag is the source of truth and renaming touches blame history?
   - **Resolved 2026-05-26**: leave names as-is. The tag is the source of truth; drift-rename can land as a TODO-999 cleanup item if desired.

## Phases

### Phase 1 — Tag introduction & adoption (deliverable a)

1. **Create the meta-annotations** under `juneau-utest/src/test/java/org/apache/juneau/testing/annotations/`: `SpringbootTest.java`, `JettyMicroserviceTest.java`.
2. **Annotate the 22 inventoried classes** with the appropriate meta-annotation. Mechanical — one line per class.
3. **Add `scripts/check-container-tags.py`** — discipline guard. Scans `juneau-utest/src/test/java/**/*Test.java`, applies the OQ #7 heuristics to identify container-booting classes, asserts each carries `@SpringbootTest` or `@JettyMicroserviceTest`. Exits non-zero on a mismatch with a per-file list. Sub-second runtime.
4. **Configure dual-execution Surefire** in `juneau-utest/pom.xml`:
   ```xml
   <plugin>
       <artifactId>maven-surefire-plugin</artifactId>
       <executions>
           <execution>
               <id>default-test</id>
               <configuration>
                   <excludedGroups>container</excludedGroups>
                   <reportsDirectory>${project.build.directory}/surefire-reports/core</reportsDirectory>
               </configuration>
           </execution>
           <execution>
               <id>container-test</id>
               <phase>test</phase>
               <goals><goal>test</goal></goals>
               <configuration>
                   <groups>container</groups>
                   <reportsDirectory>${project.build.directory}/surefire-reports/container</reportsDirectory>
               </configuration>
           </execution>
       </executions>
   </plugin>
   ```
   Both `<execution>`s run during the `test` phase. The `reportsDirectory` separation produces naturally bucketed Surefire output for deliverable (d).
5. **Wire `scripts/check-container-tags.py` into `scripts/push.py`** as a precondition (before the test step). Failed check blocks the push with a clear error message and a per-class hint (`Add @SpringbootTest to <class>`).
6. **Verify** — `./scripts/test.py --full` is green; `mvn test -DexcludedGroups=container` runs only the core suite; `mvn test -Dgroups=container` runs only the container suite; `mvn test -Dgroups=springboot` runs only the 10 Spring Boot classes. Eclipse Run-As → JUnit with `container` in the Tags exclude list runs only the core suite.

**Acceptance:** all 22 container-bound classes tagged; `scripts/check-container-tags.py` passes (and fails appropriately on a fabricated unTagged-but-container-booting test); dual-execution Surefire produces two `reportsDirectory` outputs; `scripts/coverage.py` produces identical aggregate coverage figures to pre-tag.

### Phase 2 — Push-timing infrastructure (deliverable b)

1. **`scripts/test.py --timing-log <path>` flag.** When set, captures per-execution wall-clock and writes a JSONL record. Maven's Reactor Summary prints per-module times; Surefire's per-execution times come from the `target/surefire-reports/<dir>/TEST-*.xml` files (sum the `<testsuite time="…">` across the bucket dir). Record shape:
   ```json
   {"ts": "...", "branch": "...", "commit": "...", "module": "juneau-utest", "execution": "core", "wallclock_s": 28.4, "test_count": 1237, "passed": true}
   {"ts": "...", "branch": "...", "commit": "...", "module": "juneau-utest", "execution": "container", "wallclock_s": 18.6, "test_count": 22, "passed": true}
   ```
2. **`scripts/push-timings.py` reporter.** Reads the last N records (default 20) from the JSONL log, computes per-`(module, execution)` rolling median, compares the just-appended record's `wallclock_s` against the median.
   - **Always print the numbers** — every push prints a `📊 Push-timing report` block with one row per `(module, execution)`, showing current wall-clock, rolling median, and percent delta. Format per OQ #3 resolution:
     ```
     📊 Push-timing report (last 20 runs on this branch):
        juneau-utest/core     : 28.4s  (median 27.9s, delta +1.8%)
        juneau-utest/container: 18.6s  (median 18.2s, delta +2.2%)
     ```
   - **Warning banner only when threshold breached** — if `|current - median| / median > threshold` (default `0.05` per OQ #3, env-var-tunable as `JUNEAU_PUSH_TIMING_THRESHOLD`), inject a ⚠ banner line immediately after the offending row:
     ```
        juneau-utest/core     : 41.2s  (median 27.9s, delta +47.7%)
        ⚠ juneau-utest/core wall-clock 41.2s is +47.7% above rolling median 27.9s (last 20 runs, threshold 5%)
     ```
   - Warning-only — never returns non-zero exit code. Doesn't block the push.
3. **Wire into `scripts/push.py`.** After the `python3 scripts/test.py --full` invocation succeeds:
   - Pass `--timing-log <path>` per OQ #2 (`~/.cache/juneau-push-timings/<branch>.jsonl` if going with out-of-repo).
   - Run `python3 scripts/push-timings.py --log <path>` to surface deltas. Capture stdout, prefix with `🔍 Push-timing report:` for the user.
   - Continue to the build + commit + push steps regardless of timing-report output.
4. **Bootstrap the log.** First-run-with-empty-log behavior: skip delta check, just write the baseline record. Subsequent runs have data to compare against.

**Acceptance:** running `./scripts/push.py "test message" --dry-run` (after extending dry-run to include timing read/write) on a clean working tree prints "first run, no baseline" → second `/push` on the same branch prints the per-execution timing block with delta percentages → fabricated 10% wall-clock blow-up in a unit test triggers the warning banner on the affected execution (above the 5% threshold) while the unaffected execution prints only its baseline numbers → `JUNEAU_PUSH_TIMING_THRESHOLD=0.15 ./scripts/push.py …` suppresses the banner for the same 10% blow-up but still prints the numbers.

### Phase 3 — Container-type rollup (deliverable d)

1. **`scripts/surefire-rollup.py`** — walks both bucketed Surefire dirs:
   - `<module>/target/surefire-reports/core/TEST-*.xml`
   - `<module>/target/surefire-reports/container/TEST-*.xml`
2. **Sub-bucket the container dir** by reading each test class's `@SpringbootTest` / `@JettyMicroserviceTest` annotation (or by class-suffix as a fallback for the drift cases). Two sub-buckets: `container.springboot`, `container.jetty`.
3. **Aggregate** per-module per-bucket wall-clock totals. Output:
   ```
   juneau-utest/
     core                : ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ (1237 tests, 28.412s)
     container.springboot: ▓▓▓▓▓▓▓▓▓▓▓ (10 tests, 11.812s)
     container.jetty     : ▓▓▓▓▓▓▓▓▓▓ (12 tests, 6.781s)
   ```
4. **Integrate with Phase 2.** Same `--timing-log` writer optionally writes per-sub-bucket records; reporter optionally summarizes with `--by-container`.

**Acceptance:** running the rollup over a fresh `target/surefire-reports/{core,container}/` produces a per-bucket table matching the inventory baseline (1237 core, 10 springboot, 12 jetty) → numbers match a hand-count of `<testsuite time="…">` values across each bucket dir.

### Phase 4 — Profiling pass + TODO-999 handoff (deliverable c)

1. **`scripts/test.py --profile <module>` flag.** Adds `-DargLine='-XX:StartFlightRecording=…'` (JFR — see OQ #4) or `-agentpath:` (async-profiler) to the `mvn test` invocation. Writes the `.jfr` (or `.html` flame graph) output to `target/profile-results/<module>-<ts>.jfr` per OQ #5.
2. **One-shot profiling run** over the heaviest tagged buckets:
   - `juneau-utest` core (1237 tests, the heavy work).
   - `juneau-utest` container.springboot (slowest per-test).
3. **Open each `.jfr` in JDK Mission Control** (or render flame graphs if using async-profiler). Look for:
   - Hot allocation sites in `BeanContext.getClassMeta(...)` (recurring suspect per FINISHED-103's findings).
   - Regex re-compilation in `UrlPathMatcher` and similar.
   - Redundant `VarResolver.resolve(...)` calls (FINISHED-103 already addressed many via `VarTemplateCompiler` — expect fewer findings here than initially anticipated).
   - Serializer-session warm-up cost (first-`MockRestClient.create()`-per-class spike).
   - Spring `Environment.getProperty(...)` walks on unknown keys (FINISHED-79's `SpringEnvironmentPropertySource` should memoize, but verify).
4. **File findings into [`TODO-999-final-prerelease-cleanup.md`](TODO-999-final-prerelease-cleanup.md)'s "Working notes" section.** Format per existing TODO-999 convention: bullet per finding with file/line pointer + one-sentence summary + flame-graph screenshot or `.jfr` path. **Do NOT inline the fixes here.**

**Acceptance:** `./scripts/test.py --profile juneau-utest` produces a `.jfr` file under `target/profile-results/` → at least one actionable finding is filed to TODO-999's working-notes section with a precise pointer (`juneau-marshall/.../BeanContext.java:LXXX`) → the finding rationale references the flame-graph evidence.

## Acceptance criteria

- **Phase 1**: 22 container-bound classes carry `@SpringbootTest` / `@JettyMicroserviceTest`; `scripts/check-container-tags.py` passes on the current tree and fails on a fabricated untagged container-booting test; dual-execution Surefire produces bucketed `reportsDirectory` outputs; `scripts/coverage.py` aggregate coverage matches pre-tag.
- **Phase 2**: `scripts/push.py` invocation appends per-execution JSONL records per push; `scripts/push-timings.py` always prints the per-execution numbers + delta percentages, and surfaces a ⚠ warning banner when a fabricated >5% delta is introduced.
- **Phase 3**: `scripts/surefire-rollup.py` produces a per-bucket table consistent with the inventory baseline.
- **Phase 4**: at least one `.jfr` profile captured; at least one actionable optimization finding filed into TODO-999's "Working notes".

## Risks

- **Tag-discipline drift** — a new test boots a container without `@SpringbootTest` / `@JettyMicroserviceTest`, sneaking into the core bucket and re-introducing the original "slow tests hide perf regressions" problem. Mitigation: `scripts/check-container-tags.py` as a `/push` precondition catches this on the next push attempt.
- **`scripts/check-container-tags.py` heuristic gaps.** The detection heuristics in OQ #7 might miss a new container-boot pattern (e.g. a test that uses raw `SpringApplication.run(...)` instead of `@SpringBootTest`). Mitigation: review the heuristics quarterly; treat any FINISHED-79-class incident as a signal to extend the heuristic set.
- **JUnit 5 tag-discovery quirks with meta-annotations.** JUnit-platform discovery walks meta-annotation hierarchies (verified upstream behavior) — but a wrong-Surefire-plugin-version could behave differently. Mitigation: pin Surefire >= 3.0.0 in the parent POM; verify the dual-execution config with a smoke test before merging Phase 1.
- **Surefire dual-execution interaction with `<testFailureIgnore>`.** Default behavior: if the first execution fails, the second doesn't run, which would hide container-test failures behind core-test failures. Mitigation: set `<testFailureIgnore>true</testFailureIgnore>` on the first execution and check the aggregate result in `scripts/test.py` post-hoc; OR sequence both executions and report combined pass/fail explicitly.
- **JaCoCo append-on-second-execution.** JaCoCo's `prepare-agent` goal must use `append=true` for the second Surefire execution to add to (not overwrite) the first execution's `.exec` file. Default behavior; verify.
- **JFR profiling startup overhead.** A short test run with JFR enabled has the JFR-init cost visible in the flame graph, which can mask real hotspots. Mitigation: profile only the heaviest buckets; use `-XX:StartFlightRecording=settings=profile` (medium-detail preset); ignore the first few seconds.
- **Out-of-repo timing log (OQ #2) means CI runs lose history.** A push from a fresh `~/.cache/` produces no warnings ever. Acceptable if `/push` is developer-local-only; needs revisiting if `/push` ever runs in CI.

## Related work

- **FINISHED-79** ([`FINISHED-79-value-annotation-config-bridge.md`](FINISHED-79-value-annotation-config-bridge.md)) — the perf-incident lesson learned. The `RestContext.build()` → `Settings.get().addSource(...)` bridge dropped during cleanup is what this TODO is designed to catch automatically next time.
- **FINISHED-103** ([`FINISHED-103-varresolver-template-compilation.md`](FINISHED-103-varresolver-template-compilation.md)) — already addressed many `VarResolver.resolve(...)` hotspots via the `VarTemplateCompiler` retrofit. Expect Phase 4 profiling to find *less* on the SVL side than initially anticipated; findings will skew toward `BeanContext` / serializer-session cold-start.
- **TODO-94a/b/c** (auth-filter framework + SAML + OAuth) — sequenced after TODO-105 so the auth-test PRs land with regression-tracking in place. The tag-based scope makes this sequencing cheap.
- **TODO-999** ([`TODO-999-final-prerelease-cleanup.md`](TODO-999-final-prerelease-cleanup.md)) — recipient of the Phase 4 profiling findings. Do NOT migrate any of those fixes into TODO-105. The drift-rename pass from OQ #8 is also a TODO-999 candidate if not done here.
- **TODO-95** (per-RestContext `@Value("${cfg-key}")` resolution follow-on to FINISHED-79). Independent of TODO-105 but motivated by the same incident.
