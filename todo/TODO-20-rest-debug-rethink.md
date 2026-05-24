# TODO-20: Rethink how debugging works in RestServlet

Source: promoted from `TODO.md` on 2026-05-21.

## Dependency on TODO-35

Phase 3 of this plan (test-time `@TestBean DebugConfig` overlay) **depends on TODO-35
Phase 2** landing first (`@TestBean` annotation + `JuneauBeanStoreExtension` in
`juneau-junit5`). Phases 1, 2, and 4 can land independently of TODO-35 — only the
test-side ergonomics in Phase 3 need the BeanStore overlay machinery.

If TODO-35 slips, Phase 3 can still land using direct
`MockRestClient.Builder.debugConfig(DebugConfig)` wiring (proposed in Phase 3 below),
just without the `@TestBean` declarative form.

## Goal

Replace the current debugging mechanism — split across `DebugEnablement`,
`CallLogger`'s parallel `normalRules`/`debugRules` lists, five `@Rest`/`@RestOp`
attributes, and a single `Boolean` request attribute — with a **single bean +
single annotation** surface that:

1. Configures per-endpoint debug **at compile time** (annotation) and **at runtime**
   (injected bean / programmatic `RestRequest` knob).
2. Carries a **pluggable log format** per matched endpoint (text-with-detail-levels,
   JSON, one-line, SLF4J-structured, capture-for-tests, user-supplied).
3. Plays well with the `BeanStore` so test code can swap the whole debug config (or
   just the format) without rebuilding the resource or subclassing the call logger.
4. Lets different endpoints debug at different fidelities under the same resource
   (today's boolean toggle forces every "on" endpoint through the same rule set).

## Current state (as of 2026-05-21)

Three concerns spread across two beans, five annotation attributes, and a one-bit
request hand-off.

### Beans (`juneau-rest-server`)

- **`DebugEnablement`** (`org.apache.juneau.rest.debug`) — decides whether debug is
  "on" for a `(RestContext|RestOpContext, HttpServletRequest)` pair. Backed by a
  `ReflectionMap<Enablement>` keyed on class/method patterns, plus a single
  `Predicate<HttpServletRequest>` (default: `Debug: true` header) for the
  `CONDITIONAL` case.
- **`BasicDebugEnablement`** — default impl. Builds the map from `@Rest(debug)`,
  `@RestOp(debug)`, and `@Rest(debugOn)` annotations on the resource.
- **`CallLogger`** (`org.apache.juneau.rest.logger`) — separately decides **how** to
  log a request when debug is "on". Two parallel rule lists (`normalRules`,
  `debugRules`), each `CallLoggerRule` carries its own `Enablement`, `enabledTest`,
  `Level`, `requestDetail`, `responseDetail`, status/exception filters.
- **`BasicCallLogger`**, **`BasicTestCallLogger`**, **`BasicTestCaptureCallLogger`**
  — subclasses for common shapes (production / test-with-no-trace / test-capture).

**Hand-off**: a single `Boolean` request attribute `"Debug"`.
- `RestSession.debug(true)` flips the attribute and wraps `req`/`res` in
  `CachingHttpServletRequest` / `CachingHttpServletResponse` so bodies can be
  replayed for logging.
- `CallLogger.isDebug(req)` reads the attribute back later and switches rule lists.

### Annotation surface

- `@Rest(debug)` — `"true"|"false"|"conditional"|""` string for the whole resource.
- `@Rest(debugDefault)` — same semantics as `debug` but resolved to an `Enablement`
  and published into the bean store. Added in 9.5 as the "structured" form, but
  `debug` was kept for back-compat.
- `@Rest(debugOn)` — comma-delimited `class.method=value` list, designed to be
  filled from `$E{DEBUG_ON_SETTINGS}` etc.
- `@Rest(debugEnablement)` — `Class<? extends DebugEnablement>` for full replacement.
- `@RestOp(debug)` — per-method override (`"true"|"false"|"conditional"|""`).
- `@Rest(callLogger)` — class-level `CallLogger` override.

### Runtime knobs

- `RestRequest.setDebug()` / `setDebug(Boolean)` — programmatic per-request toggle
  (calls `CachingHttpServletRequest.wrap()` and sets the `"Debug"` attribute).
- `RestRequest.isDebug()` — reads the attribute.
- `CallLogger` system properties: `juneau.restLogger.{enabled,logger,requestDetail,responseDetail,level}`
  (and matching `JUNEAU_RESTLOGGER_*` env vars).

### Problems

1. **Two-bean split with single-bit hand-off.** `DebugEnablement` decides *whether*,
   `CallLogger` decides *how*, with only a `Boolean` between them. No way to say
   "debug endpoint A at HEADER level and endpoint B at ENTITY level" — once the
   boolean is on, every endpoint goes through `debugRules` indiscriminately.
2. **Five-attribute annotation surface for one concept.** `debug` / `debugDefault` /
   `debugOn` / `debugEnablement` + `@RestOp(debug)` all configure the same thing
   with slightly different semantics. The `debug` vs `debugDefault` distinction is
   a 9.5 transitional wart (see the long comment in `RestContext.debugEnablement`
   memoizer at ~lines 576–602).
3. **Format is baked into `CallLogger`.** `CallLoggingDetail` has three hard-coded
   levels (`STATUS_LINE` / `HEADER` / `ENTITY`) and the format is hand-built in
   `CallLogger.log(req, res)` with `StringBuilder` appends. JSON, SLF4J-structured,
   one-line, etc. all require subclassing `CallLogger` and overriding `log()` —
   too high a barrier.
4. **`debugOn` string-format exists only to work around annotation limitations.**
   Parsing `MyResource=conditional,MyResource.doX=true` out of a `String` is the
   workaround for not being able to put a `Map` in an annotation, and the format
   is awkward to extend (no per-rule level, no per-rule format).
5. **Caching is all-or-nothing.** Turning debug on wraps the streams unconditionally;
   there's no "header-only debug, don't cache the body" mode without custom code.
6. **Testing is heavyweight.** To capture or suppress log output, tests subclass
   `BasicTestCallLogger` / `BasicTestCaptureCallLogger` and wire them via
   `@Rest(callLogger=…)` on a per-test inner-class resource. Changing the log
   format mid-test means rebuilding the `MockRestClient`. Nothing today lets a
   test say "for this test, dial debug up to ENTITY on these endpoints" without
   rebuilding.
7. **Pre-9.5 plumbing still visible.** The `RestContext.debugEnablement` memoizer
   is a long apology for an inheritance chain that landed mid-release (legacy
   pre-9.5 protocol, pre-registered `Enablement` bean, mock-client override, then
   annotation override on top).

## Recommended design

**Collapse to a single bean and a single annotation.**

### 1. `DebugConfig` bean replaces `DebugEnablement` + `CallLogger`'s rule lists

```java
public class DebugConfig {

    public static class Builder {
        Predicate<HttpServletRequest> defaultEnablement;   // default: never
        DebugFormat defaultFormat;                          // default: BasicTextFormat(HEADER)
        Level defaultLevel;                                 // default: INFO
        Logger logger;                                      // default: per-resource java.util.logging.Logger

        // Per-endpoint rules. Each rule carries its OWN enablement + format + level.
        // Replaces both DebugEnablement.enable(...) AND CallLogger.normalRules/debugRules.
        public Builder rule(DebugRule rule);
        public Builder rule(Class<?> target, Consumer<DebugRule.Builder> spec);
        public Builder rule(String classOrMethodKey, Consumer<DebugRule.Builder> spec);
        public Builder rule(Predicate<HttpServletRequest> when, Consumer<DebugRule.Builder> spec);

        // Shortcut for the most common case: "debug on when this header equals this value".
        public Builder conditionalHeader(String name, String value);  // default: "Debug" / "true"
    }

    public DebugResult resolve(RestContext ctx, HttpServletRequest req);
    public DebugResult resolve(RestOpContext ctx, HttpServletRequest req);
}

public record DebugResult(boolean enabled, DebugFormat format, Level level, boolean cacheBodies) {}

public class DebugRule {
    Predicate<HttpServletRequest> enablement;  // when does this rule fire?
    DebugFormat format;                         // how to render
    Level level;                                // log level
    boolean cacheBodies;                        // whether to wrap req/res
    // status/exception filters carry over from CallLoggerRule for error-only logging
}
```

The key shift: **a rule carries both the gating predicate AND the format**, so
different endpoints can debug at different fidelities under the same `DebugConfig`.
`CallLogger.log(req, res)` becomes a one-pager: resolve the rule via `DebugConfig`,
delegate to `format.format(req, res, exception)`, emit through the resolved `Logger`
at the resolved `Level`.

### 2. `DebugFormat` interface — pluggable formatter

```java
public interface DebugFormat {
    String format(DebugContext ctx);  // ctx carries req, res, exception, execTime, thrownStats
}
```

Built-ins:

- **`BasicTextFormat(detail)`** — current `STATUS_LINE` / `HEADER` / `ENTITY`
  output, with `detail` as a constructor arg. Default.
- **`OneLineFormat`** — `[200] HTTP GET /foo (12ms)` single line.
- **`JsonFormat`** — one JSON object per call; suitable for ingest into log
  aggregators.
- **`Slf4jStructuredFormat`** — uses SLF4J's `Logger.atInfo().addKeyValue(...).log()`
  builder; no string concatenation. **Opt-in via new `juneau-rest-server-slf4j`
  sub-module so the core has no SLF4J dep.**
- **`CapturingFormat`** — used by tests; records the rendered output to an
  `AtomicReference<String>` rather than emitting it. Replaces the bespoke
  `BasicTestCaptureCallLogger`. **Implementation should sit on top of
  `org.apache.juneau.commons.logging.LogRecordCapture`** (already in
  `juneau-commons`) — it implements `LogRecordListener` + `Closeable`, gives
  per-test `try-with-resources` capture, and supports format-string-based
  assertions via `LogRecord.formatted(...)`. Reusing it keeps Juneau's debug
  capture surface consistent with the existing logging package's idiom and
  avoids reinventing the listener / formatted-message plumbing.

User-supplied formats resolve via the BeanStore by class, same as today's
`DebugEnablement`.

**Reuse opportunity — `org.apache.juneau.commons.logging.LogRecordCapture`.**
The framework already ships a `LogRecordCapture` class in `juneau-commons` that
implements `LogRecordListener` + `Closeable` and accumulates `LogRecord` events
from a `Logger.captureEvents()` try-with-resources block. It supports formatted
record retrieval (`getRecords("{level}: {msg}")`), `clear()` / `size()` /
`isEmpty()`, and removes itself from the logger on close. **`CapturingFormat`
should be a thin adapter over this** rather than a parallel
`AtomicReference<String>` mechanism — the test author writes
`try (var cap = Logger.getLogger(...).captureEvents()) { ... cap.getRecords(); }`
and the production-side `DebugFormat` simply emits through `Logger` like every
other format. Result: `BasicTestCaptureCallLogger` deletion costs zero new
machinery, and tests inspecting captured debug output get the same API as tests
inspecting any other log channel. See open question #9 below.

### 3. `@Debug` annotation replaces five existing attributes

```java
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
@Inherited
public @interface Debug {
    /** Enablement policy: "always" / "never" / "conditional" / "" (inherit). */
    String value() default "";

    /** Optional format class. */
    Class<? extends DebugFormat> format() default DebugFormat.Default.class;

    /** Optional log level (parsed by Level.parse). */
    String level() default "";

    /** Optional comma-delimited per-endpoint overrides, mirrors today's debugOn. */
    String on() default "";

    /** Custom DebugConfig class for full replacement. */
    Class<? extends DebugConfig> config() default DebugConfig.Default.class;
}
```

- Class-level `@Debug(...)` replaces `@Rest(debug)`, `@Rest(debugDefault)`,
  `@Rest(debugEnablement)`, `@Rest(debugOn)`.
- Method-level `@Debug(...)` replaces `@RestOp(debug)`.
- The five existing attributes are deprecated (with identical semantics retained
  internally for one release) and removed in 9.7.

Compile-time configuration becomes one annotation per place. Runtime configuration
becomes one bean (`DebugConfig`) reachable via the BeanStore.

### 4. Runtime fluent surface on `RestRequest`

Today:

```java
req.setDebug();
req.setDebug(false);
req.isDebug();
```

New (fluent, format-aware; old methods kept as one-liner shortcuts):

```java
req.debug().enable(JsonFormat.class).level(FINE);
req.debug().disable();
req.debug().isEnabled();   // == today's isDebug()
```

The fluent surface sets a `DebugResult` request attribute that overrides the
resolved rule for this single call. `setDebug(...)` keeps working and routes
through the new mechanism.

### 5. Test-time integration with TODO-35

With TODO-35 Phase 2's `@TestBean` extension in place:

```java
@ExtendWith(JuneauBeanStoreExtension.class)
class MyResourceTest {

    @TestBean
    DebugConfig debug = DebugConfig.create()
        .rule(MyResource.class, r -> r.always().format(CapturingFormat.class).level(FINE))
        .build();

    @Test
    void aTest(TestBeanStore store) throws Exception {
        var client = MockRestClient.create(MyResource.class)
            .overridingBeanStore(store)
            .build();
        client.get("/widgets/1").run().assertStatus().is(200);
        store.getBean(CapturingFormat.class).orElseThrow()
            .assertLast().contains("widgets/1");
    }
}
```

No subclassing `BasicTestCaptureCallLogger`, no `@Rest(callLogger=…)` on an inner
class. The test asks for a different `DebugConfig` and a `CapturingFormat`, the
bean store overlay supplies them, the production resource is untouched. Per-test
dial-up of `level` / `format` is just a builder call.

**Without TODO-35**, the same effect is reachable in Phase 3 via a direct
`MockRestClient.Builder.debugConfig(DebugConfig)` method — less ergonomic, but
self-contained.

### 6. Migration

- **9.5.x (current release line)** — land the new `DebugConfig` + `@Debug` +
  `DebugFormat` alongside the existing system. `BasicDebugEnablement` and
  `BasicCallLogger` are reimplemented as thin adapters that read `@Rest(debug)` /
  `@RestOp(debug)` / `@Rest(debugOn)` into a `DebugConfig` builder internally.
  Existing tests and resources keep working with **zero code changes**.
- **9.5.x release notes** — entry in
  `juneau-docs/docs/pages/release-notes/9.5.0.md` (or 9.5.1 if that's open) under
  `juneau-rest-server`: new annotation, new bean, deprecation note for the old
  surface.
- **9.6** — deprecate `@Rest(debug)`, `@Rest(debugDefault)`,
  `@Rest(debugEnablement)`, `@Rest(debugOn)`, `@RestOp(debug)`, `DebugEnablement`,
  `BasicDebugEnablement`, `BasicCallLogger`'s `normalRules` / `debugRules`.
  Migration entry in `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md`
  (TODO-17 territory) with Old → New rows.
- **9.7** — remove deprecated surface.

### 7. Class summary

| Class | Package | Module | Notes |
| --- | --- | --- | --- |
| `DebugConfig` (+ `Builder`) | `org.apache.juneau.rest.debug` | `juneau-rest-server` | Replaces `DebugEnablement` + parts of `CallLogger`. |
| `DebugRule` (+ `Builder`) | `org.apache.juneau.rest.debug` | `juneau-rest-server` | Carries gate + format + level + filters. |
| `DebugResult` (record) | `org.apache.juneau.rest.debug` | `juneau-rest-server` | Resolved per request. |
| `DebugFormat` (interface) | `org.apache.juneau.rest.debug` | `juneau-rest-server` | Pluggable formatter. |
| `BasicTextFormat`, `OneLineFormat`, `JsonFormat`, `CapturingFormat` | `org.apache.juneau.rest.debug.format` | `juneau-rest-server` | Built-in formats. |
| `Slf4jStructuredFormat` | `org.apache.juneau.rest.debug.format.slf4j` | new `juneau-rest-server-slf4j` | SLF4J-only sub-module; keeps core SLF4J-free. |
| `@Debug` | `org.apache.juneau.rest.annotation` | `juneau-rest-server` | Replaces 5 existing attributes. |
| `RestRequest.debug()` fluent | `org.apache.juneau.rest` | `juneau-rest-server` | New runtime knob; `setDebug`/`isDebug` retained as shortcuts. |

## Open questions

(Parked for later review.)

1. **Annotation merge with TODO-17**. The new `@Debug` is a 9.5 → 9.6 breaking-change
   in the migration-guide sense. Should we land it under the TODO-17 audit pass to
   bundle the migration-guide row? Or independent?
2. **`Predicate<HttpServletRequest>` vs `Predicate<RestRequest>` in rule gating**.
   `DebugEnablement` today uses `HttpServletRequest` (it runs early, before
   `RestRequest` is built for the class-match call). Keeping the same signature
   avoids reordering the pipeline. Recommendation: `HttpServletRequest`.
3. **Method-level `@Debug` on `@RestOp` methods vs. on regular methods**. Annotation
   should be allowed on any method that ends up in the op-method scan, not only
   methods carrying `@RestOp/@RestGet/...`. Same scope as today's `@RestOp(debug)`.
4. **Format wiring for `BasicCallLogger` back-compat**. If a user has subclassed
   `CallLogger` (production code we don't control), do we keep the subclass surface
   and have the new system invoke the subclass when no format is configured?
   Recommendation: yes — `DebugConfig` falls back to the configured `CallLogger`
   when no `DebugFormat` is on the matched rule.
5. **`CachingHttpServletRequest` policy**. The new `cacheBodies` flag on `DebugRule`
   lets us avoid stream caching when only `STATUS_LINE` / `OneLineFormat` is in use.
   Default: `true` for `ENTITY`-level formats, `false` otherwise. Worth checking
   what currently breaks if we don't cache (only consumer found is
   `CallLogger.getRequestContent` / `getResponseContent`).
6. **TODO-7 / `juneau-rest-common` boundary**. `DebugConfig` references
   `RestContext` / `RestOpContext`, which are server-only — so the new types live
   in `juneau-rest-server` like `DebugEnablement` does today. No impact on TODO-7.
7. **Eager rule resolution**. Today's `DebugEnablement` resolves per-class and
   per-method up-front and caches a `ReflectionMap`. The new bean can keep that
   exact structure — `DebugConfig` builder fan-outs `@Debug` annotations on
   class+methods into a `ReflectionMap<DebugRule>`. No regression in dispatch cost.
8. **Single `@Debug(on=…)` parser**. Should `on` accept the old `debugOn`
   `class=value,method=value` syntax for ergonomics, or should we go straight to a
   structured form (`@Debug.On({ @Debug.Rule(targets=…, value=…), …})`)?
   Recommendation: keep the string form for SVL-resolved system-property use cases;
   add `@Debug.Rule` for the in-source form.
9. **Integrate `org.apache.juneau.commons.logging` package**. The existing
   `juneau-commons` logging package (`Logger`, `LogRecord`, `LogRecordListener`,
   `LogRecordCapture`) is the right substrate for the new debug surface to sit on:
   - **`CapturingFormat`** is a thin wrapper around `LogRecordCapture` (see
     section 4 above) — `try-with-resources` capture, format-string assertions,
     `LogRecordListener` plumbing, all already exist.
   - **`DebugConfig.logger` field** should accept either a `java.util.logging.Logger`
     (back-compat with today's `CallLogger.Builder.logger(...)`) **or** a
     `org.apache.juneau.commons.logging.Logger` (the extended subclass). The latter
     unlocks `Logger.captureEvents()` and the cleaner formatted-message API for
     framework-internal callers.
   - **Format-string conventions** in the `commons.logging` package
     (`{level}: {msg}`, `{thrown}`, etc.) should be the reference set for any
     placeholder vocabulary `BasicTextFormat` / `OneLineFormat` / `JsonFormat`
     end up exposing.
   Recommendation: explicit dependency on `org.apache.juneau.commons.logging` from
   `org.apache.juneau.rest.debug`; no parallel reinvention of listener / capture /
   formatted-message machinery.
9. **`CapturingFormat` vs `LogRecordCapture`**. Two paths:
   1. Ship a dedicated `CapturingFormat` with its own `AtomicReference<String>`
      mechanism (the original draft above).
   2. **Recommended:** route every `DebugFormat` through `org.apache.juneau.commons.logging.Logger`
      (already wraps `java.util.logging.Logger` and supports `addLogRecordListener` +
      `captureEvents()`), and have tests use `LogRecordCapture` directly — no
      bespoke capturing format needed. The capturing API the user already has
      (`logger.captureEvents()` returning a `LogRecordCapture` `Closeable`) is the
      idiomatic test surface. The new `DebugConfig.Builder.logger(Logger)` would
      accept `org.apache.juneau.commons.logging.Logger` (the framework's
      delegating logger) so this composes for free.
   Cost of (2): we lose the "format-output-as-string" capture niche (e.g. capture
   the exact JSON the prod system would have emitted, byte-for-byte). Mitigation:
   `LogRecord` already carries the rendered message; tests that need the rendered
   string use `cap.getRecords("{msg}")`. Decision needed before Phase 1.

## Out of scope

- Rewriting `juneau-rest-client`'s debug surface (`RestClient.Builder.debug()` /
  `logRequests(...)`). The client side has a different idiom (`DetailLevel`,
  `logToConsole`); this plan touches only the server.
- Replacing `java.util.logging` with SLF4J at the framework level.
  `Slf4jStructuredFormat` is opt-in via the new sub-module; the framework continues
  to default to JUL.
- Migrating per-request body caching to a non-`CachingHttpServletRequest` mechanism
  (e.g. streaming-aware tee). Out of scope for v1; revisit if `cacheBodies=false`
  unlocks real savings.

## Implementation phases

### Phase 1 — bean + format infrastructure, no annotation changes

1. Add `DebugConfig`, `DebugRule`, `DebugResult`, `DebugFormat`, `BasicTextFormat`,
   `OneLineFormat`, `JsonFormat`, `CapturingFormat` in `org.apache.juneau.rest.debug`
   / `…debug.format`.
2. Register `DebugConfig` as a default-supplier-backed bean in
   `RestContext.createBeanStore(...)` (alongside today's `DebugEnablement.class`
   line at ~404).
3. Refactor `CallLogger.log(req, res)` to delegate to
   `DebugConfig.resolve(ctx, req).format().format(...)` when a `DebugConfig` is
   present; fall back to today's `StringBuilder` path otherwise.
4. Tests in `juneau-utest`:
   - `DebugConfig_Test` — builder + resolve semantics.
   - `DebugFormat_Test` — each built-in produces the expected output.
   - `CallLogger_DebugConfig_Test` — end-to-end through `MockRestClient`.

### Phase 2 — `@Debug` annotation + adapter on top of existing annotations

1. Add `@Debug` annotation (class + method scope).
2. `BasicDebugEnablement` reads `@Debug` first, falls back to `@Rest(debug)` /
   `@RestOp(debug)` / `@Rest(debugOn)` for unchanged user code.
3. `RestContext.debugEnablement` memoizer simplifies: collapse the `debug` /
   `debugDefault` priority dance to a single `@Debug.value()` read.
4. Migrate `Rest_Debug_Test` cases over (the file is the canonical 1085-line
   covers-everything test — keep the existing matrix, add a parallel
   `@Debug`-based matrix).
5. Tests:
   - `Debug_Annotation_Test` — full annotation matrix (mirrors today's
     `Rest_Debug_Test`, on the new annotation).
   - `Debug_BackCompat_Test` — confirms old `@Rest(debug)` / `@Rest(debugOn)` /
     `@RestOp(debug)` still produce identical behavior.

### Phase 3 — runtime fluent surface + TODO-35 integration

**Depends on TODO-35 Phase 2 for the `@TestBean` form.** Direct
`MockRestClient.Builder.debugConfig(DebugConfig)` wiring can land independently.

1. Add `RestRequest.debug()` fluent (returns a `DebugScope` per-request handle).
   Keep `setDebug` / `isDebug` as one-liners on top of it.
2. Add `MockRestClient.Builder.debugConfig(DebugConfig)` for direct test wiring
   (parallel to TODO-35's `.overridingBeanStore(...)`).
3. Tests:
   - `Debug_Runtime_Test` — `req.debug().enable(JsonFormat.class).level(FINE)`
     works.
   - `Debug_TestBean_Test` (in `juneau-utest`, after TODO-35 Phase 2 lands) — uses
     `@TestBean DebugConfig` + `JuneauBeanStoreExtension` to swap formats and
     capture output.

### Phase 4 — deprecate the old surface (9.6)

1. Add `@Deprecated(since="9.6", forRemoval=true)` to `@Rest(debug)`,
   `@Rest(debugDefault)`, `@Rest(debugEnablement)`, `@Rest(debugOn)`,
   `@RestOp(debug)`, `DebugEnablement`, `BasicDebugEnablement`,
   `CallLogger.Builder.normalRules` / `debugRules`, `BasicCallLogger`'s per-status
   rule wiring.
2. Release-notes + migration-guide entries
   (`juneau-docs/docs/pages/release-notes/9.5.0.md` and
   `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md`).
3. No code removal yet — that's the 9.7 cycle.
