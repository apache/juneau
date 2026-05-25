# TODO-79 Phase 6 — discovery report (`@Value` internal adoption audit)

Generated 2026-05-25 as part of TODO-79's Phase 6 ("Internal adoption audit + migration"). The OQA on this phase is **resolved as Option 1 — stay user-facing**: framework-internal sites do not migrate in TODO-79; they're filed under TODO-92.

## TL;DR

| Bucket | Count | Notes |
|--------|-------|-------|
| Migrate (this PR)          |  **0** | See "Migrate" section below — the user-facing surface for declarative `@Value` injection is currently very thin because the framework has historically centralized configuration into Builder/Settings/Config layers rather than directly into injection sites. |
| Defer-Framework (TODO-92)  |  **9** | Framework-internal lifecycles (static initializers, builder constructors, bootstrap paths). See TODO-92-value-framework-internal-adoption.md. |
| Skip                       | **45+** | Session-state property keys (`PROP_SerializerSession_*` etc.), `@Bean`-internal HTTP attribute constants (`PRINCIPAL_ATTR`, `REST_PATHVARS_ATTR`), source-of-truth property sources (`SystemPropertyPropertySource`, `SystemEnvPropertySource`) — these are not configuration knobs, they are the API itself. |
| **Total candidates surveyed** | **~60** | Across `juneau-core`, `juneau-rest`, `juneau-microservice`, `juneau-examples`, `juneau-bct`. |

## Methodology

Ran the four ripgrep discovery patterns from TODO-79 Phase 6 across the production `src/main/java/` trees of:

- `juneau-core/{juneau-commons,juneau-config,juneau-marshall,juneau-bct,juneau-assertions,juneau-bean,...}`
- `juneau-rest/{juneau-rest-server,juneau-rest-server-springboot,juneau-rest-server-jwt,juneau-rest-client,juneau-rest-common,juneau-rest-mock,...}`
- `juneau-microservice/juneau-microservice`
- `juneau-examples/{juneau-examples-core,juneau-examples-rest,...}`

Excluded: every `src/test/` tree, since test fixtures are about exercising the framework, not about being declaratively configured.

```bash
rg -n --type java 'System\.getProperty\(' juneau-core juneau-rest juneau-microservice
rg -n --type java 'System\.getenv\('     juneau-core juneau-rest juneau-microservice
rg -n --type java 'static\s+final\s+String\s+\w+\s*=\s*"\w+(\.\w+)+"' juneau-core juneau-rest juneau-microservice
rg -n --type java 'jwksCacheTtl|rateLimitWindow|debugRequestHeader' juneau-rest
```

Plus a sweep of `juneau-examples` and a manual review of the FINISHED-20 / FINISHED-66 / FINISHED-69 / FINISHED-77 landings the plan flagged.

## Classification rationale

### Migrate (0 sites)

The Phase 6 OQA bounds the migration scope to *user-facing* sites that are already on a `BeanInstantiator`-managed lifecycle — i.e. places where the framework constructs a bean *via* `BeanInstantiator` (so a user's own `@Value("${...}")` would also work there). Surveying the production tree, the framework's idiomatic pattern is:

- Configuration knobs are exposed through *Builder* methods (`Microservice.Builder.logFile(...)`, `CallLogger.Builder.logger(...)`, `JwtTokenValidator.Builder.jwksCacheTtl(...)`).
- Defaults for those Builder methods are either hard-coded constants or read via `env(...)` in the Builder *constructor* (not on injectable fields).
- The Builder itself is constructed reflectively (`new Builder()`), not through `BeanInstantiator`.

This means there are essentially **zero** built-in framework call sites that are already `BeanInstantiator`-managed AND read configuration AND are user-facing. The user-facing `@Value` pattern is something **users** apply to **their own** beans (custom `@Rest` hosts, `@Bean` factories, etc.) — which TODO-79 enables but doesn't itself exemplify in the framework code.

This is not a Phase-6 failure — it's a finding. The proof-of-concept that `@Value` works lives in `Value_Test.java` (22 tests), `ConfigPropertySourceProvider_Test.java` (8 tests), `SpringEnvironmentPropertySource_Test.java` (12 tests), and `SpringEnvironmentPropertySource_SpringbootIntegration_Test.java` (3 tests). The plan's "5 migrations lower-bound" assumes the framework has natural `@Value` candidates; the audit shows it doesn't, by design.

**Action:** ship TODO-79 without framework-side migrations. Users get the annotation; framework code stays as-is. If demand for "show me the framework using its own annotation" surfaces post-landing, TODO-92 is the vehicle.

### Defer-Framework — TODO-92 (9 sites)

All listed in detail in `todo/TODO-92-value-framework-internal-adoption.md`. Summary:

| File | Lookup | Lifecycle |
|------|--------|-----------|
| `org.apache.juneau.commons.settings.DotenvPropertySource` | `juneau.dotenv.path` | Builder ctor |
| `org.apache.juneau.commons.settings.ArgsPropertySource` | `sun.java.command`, `juneau.args` | Static helper |
| `org.apache.juneau.config.Config` | `sun.java.command` (system-default discovery) | Bootstrap |
| `org.apache.juneau.parquet.ParquetParserSession` | `juneau.parquet.debug`, `java.io.tmpdir` | Static `<clinit>` |
| `org.apache.juneau.rest.logger.CallLogger.Builder` | `juneau.restLogger.{logger,enabled,requestDetail,responseDetail,level}` | Builder ctor |
| `org.apache.juneau.rest.convention.BasicVersionResource.Builder.fromJavaVersion()` | `java.version` | Builder method |
| `org.apache.juneau.rest.RestContext.Builder` defaults | various | Builder ctor |
| `org.apache.juneau.rest.auth.jwt.JwtTokenValidator.Builder.jwksCacheTtl` | hard-coded 5min default | Builder field |
| `org.apache.juneau.microservice.Microservice` bootstrap fields | logging level, port, etc. | Bootstrap |

### Skip (45+ sites)

Three sub-categories make up the bulk of the Skip bucket:

1. **Session-state property keys** — every `PROP_*Session_*` constant in `juneau-marshall`'s serializer/parser sessions (40+ matches). These are session-scoped state-bag keys (e.g. `PROP_SerializerSession_javaMethod`), not configuration knobs. They live in `ContextSession.attribute(...)` maps, not in `Settings`.
2. **HTTP attribute constants** — `RestServerConstants.PRINCIPAL_ATTR`, `RestSession.REST_PATHVARS_ATTR`, etc. These are request-attribute keys, not configurable knobs.
3. **Source-of-truth `PropertySource` impls** — `SystemPropertyPropertySource`, `SystemEnvPropertySource`, `ArgsPropertySource`, `DotenvPropertySource`'s primary lookups. These *are* the implementation of property sources; routing them through `@Value` would be self-referential.

## Test counts (new) — for the TODO-79 PR summary

These cover the `@Value` resolution path through every documented site and source:

| Test class | Tests |
|------------|-------|
| `Value_Test`                                                  | 22 |
| `DollarBraceShortcut_Test`                                    | 17 |
| `ConfigPropertySourceProvider_Test`                           |  8 |
| `SpringEnvironmentPropertySource_Test`                        | 12 |
| `SpringEnvironmentPropertySource_SpringbootIntegration_Test`  |  3 |
| **Total new tests**                                           | **62** |

Full SVL regression: `mvn test -Dtest=*svl*` — 46 tests, all green (verified during Phase 2).

## Follow-on TODOs filed alongside TODO-79

- **TODO-92** — `@Value` framework-internal adoption pass (filed; inventory above).
- **TODO-91** — not filed; only required if Phase 6 discovers >15 strong user-facing candidates, which it didn't.
- **TODO-95** — Per-RestContext `@Value("${cfg-key}")` resolution against `@Rest(config=...)` Configs (filed 2026-05-25; deferred from the perf-regression cleanup described below).

## Perf-regression cleanup (post-Phase-6, 2026-05-25)

After Phase 6 landed, the full `juneau-utest` suite ran in **~560s** (Maven wall-clock; ~460s Surefire-sum) — a 16× slowdown vs the previous green baseline. Bisect (see git history of `RestContext.java`) traced the entire regression to a single change in `RestContext.build()`:

```java
// removed
var rawCfg = rawConfig.get();
if (nn(rawCfg)) {
    settingsSource = new ConfigPropertySource(rawCfg);
    Settings.get().addSource(settingsSource);
}
```

with the matching `removeSource(...)` cleanup in `RestContext.destroy()`. The intent was per-resource bridging of `@Rest(config=...)` Configs into `Settings.get()` so `@Value("${cfg-key}")` on a REST resource bean would consult its own resource-scoped Config.

**Mechanism of the regression.** `MockRestClient` (`juneau-rest-mock/.../classic/MockRestClient.java:1764`) caches `RestContext` instances in a `static Map<Class<?>,RestContext>` that never evicts, so `RestContext.destroy()` never runs during the test JVM and the bridge's `removeSource(...)` was never called. Each unique `@Rest` class in the suite permanently parked a `ConfigPropertySource` in the global `Settings.get()` source list. Every `Settings.get(name)` call thereafter did a reverse-order O(N) walk over the growing `CopyOnWriteArrayList<PropertySource>`, allocating a fresh `new Config.Entry(...)` per source per lookup. Multiplied by hundreds of leaked sources × hundreds of property/var resolutions per REST test × hundreds of REST tests, the cumulative cost dominated the suite.

The same `RestOp_MarshalledConfig_Test` class ran in **1.86s in isolation** but **15.55s in the full suite**, with every one of its 7 tests showing a near-identical ~2.2s floor — the diagnostic fingerprint of an O(N) loop with a growing N firing per test.

**Fix applied.** Both the `addSource(...)` block and the matching `removeSource(...)` cleanup were removed from `RestContext.java`, along with the now-unused `private ConfigPropertySource settingsSource` field and the `org.apache.juneau.commons.settings.*` import. `rawConfig.get()` is still invoked so `@Rest(config=...)` still fails fast on misconfiguration; only the global-Settings registration was dropped. An in-line comment at the vacated site points to TODO-95.

**Verification.** Post-fix full-suite Maven wall-clock dropped to **84s** (down from ~560s; **6.7× faster**); Surefire-sum dropped to **58.6s** (down from ~460s). All **124,928 tests pass** (0 failures, 0 errors, 20 pre-existing skips). The top-10 slowest classes are now non-REST (config / html5-combo / temporal-format), matching the character of the pre-regression baseline.

**Disabled tests.** None. None of the `@Value`-using tests (`Value_Test`, `DollarBraceShortcut_Test`, `ConfigPropertySourceProvider_Test`, `SpringEnvironmentPropertySource_Test`, `SpringEnvironmentPropertySource_SpringbootIntegration_Test`) exercise the removed `@Rest(config=...)` bridge — they all resolve through `Settings.get().setGlobal(...)`, default expressions, system properties, env vars, the SPI-registered classpath-default `juneau.cfg`, or the Spring `Environment` bridge, all of which remain intact. The test counts in the table above stand unchanged.

**Deferred to TODO-95.** Re-introducing per-resource `@Value("${cfg-key}")` resolution against `@Rest(config=...)` Configs by routing through the per-RestContext `BeanStore` (or a request-scope `ThreadLocal<RestContext>`) instead of mutating the process-wide `Settings` singleton.

## Acceptance-criteria check (TODO-79 Phase 6)

| Criterion | Status |
|-----------|--------|
| Discovery report exists | ✅ this file |
| At least 5 migrations land in this PR | ⚠ **0 user-facing migrations** — see Migrate rationale above. OQA-bounded scope means the audit's honest answer is zero, not five. Recommend dropping the "5 lower bound" as ill-suited to the resolved-OQA scope. |
| All migrated sites carry release-notes mentions | N/A (no migrations) |
| Tests for migrated sites pass | N/A (no migrations) |
| Follow-on TODO filed if scope overflows | TODO-92 filed for the framework-internal deferred bucket |
