# TODO-20: Rethink how debugging works in RestServlet

Source: promoted from `TODO.md` on 2026-05-21.

## Dependency on FINISHED-35

Phase 3 of this plan (test-time `@TestBean DebugConfig` overlay) depends on
**FINISHED-35** (`@TestBean` annotation + `JuneauBeanStoreExtension` in
`juneau-junit5`) — already landed, so this dep is satisfied at the time of
plan-update (2026-05-25). All four phases can proceed without external blockers.

## Goal

Replace the current debugging mechanism — split across `DebugEnablement`,
`CallLogger`'s parallel `normalRules`/`debugRules` lists, five `@Rest`/`@RestOp`
attributes, and a single `Boolean` request attribute — with a **single bean +
single typed annotation slot on `@Rest`/`@RestOp`** that:

1. Configures per-endpoint debug **at compile time** via `@Rest(debug=@Debug(...))`
   and `@RestOp(debug=@Debug(...))` (with standalone `@Debug` as an escape hatch
   for annotation-composition / inherited-`@Rest` scenarios) and **at runtime** via
   an injected `DebugConfig` bean and a programmatic `RestRequest.debug()` knob.
   Putting the debug config inside the `@Rest`/`@RestOp` annotation makes those
   annotations a **source-of-truth for the resource/op's capabilities** — a reader
   scanning `@Rest(...)` sees every configurable capability in one place.
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

### 3. `@Debug` annotation — primary placement on `@Rest`/`@RestOp`, secondary placement standalone

The `@Debug` annotation type definition:

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

`@Rest` and `@RestOp` each gain a new typed slot `debug=@Debug(...)`. The slot
name `debug` is **reused** for the new typed form (the old `String`-typed slot
is removed in the same release — see §6 below). The old four `@Rest` debug
attributes (`debug`, `debugDefault`, `debugEnablement`, `debugOn`) collapse into
this single typed slot.

**Primary placement — nested on `@Rest`/`@RestOp` (source-of-truth pattern, preferred):**

```java
@Rest(
    path="/widgets",
    debug=@Debug(value="conditional", format=JsonFormat.class)
)
public class WidgetResource extends BasicRestServlet {

    @RestGet(path="/", debug=@Debug("always"))
    public List<Widget> list() { ... }
}
```

Putting debug config inside `@Rest`/`@RestOp` makes those annotations the
canonical source-of-truth for the resource/op's capabilities — a reader scanning
`@Rest(...)` sees every configurable capability in one place rather than having
to scan for sibling annotations on the same class.

This pattern generalizes beyond debug. Any future configurable capability that
grows beyond a `String`/`Class` shape should follow the same nested-typed-annotation
placement on `@Rest`/`@RestOp` rather than spawn a new standalone sibling
annotation. See Open Question #10 below for a candidate follow-on TODO that
audits the `@Rest`/`@RestOp` surface for other capabilities that could benefit.

**Secondary placement — standalone (escape hatch):**

```java
@Debug(value="conditional", format=JsonFormat.class)
public class WidgetResource extends BaseResource {  // @Rest is inherited from BaseResource
    ...
}
```

Standalone `@Debug` on class or method is retained for cases where:

- The target class inherits `@Rest` from a base class and doesn't re-declare it.
- An annotation-composition pattern aggregates debug config independently of `@Rest`.
- A method enters the op-method scan via a custom non-`@RestOp` annotation and
  still needs debug config.

In all of these the standalone form composes with whatever `@Rest`/`@RestOp`
configuration is in scope.

**Precedence when both placements are present on the same target:**

`@RestOp(debug=@Debug(...))` on the method beats standalone `@Debug` on the
method, which beats `@Rest(debug=@Debug(...))` on the class, which beats
standalone `@Debug` on the class. Specificity wins.

**Mapping table — old attributes → new typed slot:**

| Old | New |
| --- | --- |
| `@Rest(debug="true")` | `@Rest(debug=@Debug("always"))` |
| `@Rest(debug="false")` | `@Rest(debug=@Debug("never"))` |
| `@Rest(debug="conditional")` | `@Rest(debug=@Debug("conditional"))` |
| `@Rest(debugDefault="true")` | `@Rest(debug=@Debug("always"))` (collapsed) |
| `@Rest(debugEnablement=MyEnablement.class)` | `@Rest(debug=@Debug(config=MyDebugConfig.class))` |
| `@Rest(debugOn="MyResource.doX=true")` | `@Rest(debug=@Debug(on="MyResource.doX=true"))` |
| `@RestOp(debug="true")` | `@RestOp(debug=@Debug("always"))` |
| `@RestOp(debug="conditional")` | `@RestOp(debug=@Debug("conditional"))` |

Compile-time configuration becomes one annotation slot (`debug=@Debug(...)`) on
`@Rest`/`@RestOp` — with optional standalone `@Debug` for escape hatches. Runtime
configuration becomes one bean (`DebugConfig`) reachable via the BeanStore.

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

### 6. Migration — hard break, no deprecation cycle

**Decision (2026-05-25):** no two-release deprecation cycle. The old debug
surface is **removed in 9.5** in the same release that lands the new
`@Debug` / `DebugConfig` / `DebugFormat` surface. Migration is documented but
not automated.

**Rationale:** the old surface is a five-attribute mess (`debug`, `debugDefault`,
`debugEnablement`, `debugOn` on `@Rest`, plus `debug` on `@RestOp`) with a
transitional 9.5 wart already in it (the `debug` vs `debugDefault` distinction
landed mid-release). Carrying it as deprecated-but-functional for a 9.6 → 9.7
window would mean shipping two parallel code paths (the back-compat adapter
chain reading old attributes into a new `DebugConfig` builder, plus the new
typed surface) for a year, doubling the surface area for tests, docs, and
review. The hard break trades one explicit migration step at the 9.5 boundary
for a cleaner long-term shape.

**What gets removed in 9.5 (this TODO's PR):**

- `@Rest(debug)` (String) — replaced by `@Rest(debug=@Debug(...))`.
- `@Rest(debugDefault)` (String) — collapsed into `@Rest(debug=@Debug(...))`.
- `@Rest(debugEnablement)` (Class) — replaced by
  `@Rest(debug=@Debug(config=...))`.
- `@Rest(debugOn)` (String) — replaced by `@Rest(debug=@Debug(on="..."))`.
- `@RestOp(debug)` (String) — replaced by `@RestOp(debug=@Debug(...))`.
- `DebugEnablement` class + `BasicDebugEnablement` impl — replaced by
  `DebugConfig` + `BasicDebugConfig`.
- `CallLogger.Builder.normalRules` / `debugRules` — collapsed into
  `DebugConfig.rule(...)` builders with per-rule format + level.
- `BasicCallLogger`'s parallel-rule wiring — replaced by single-rule resolve
  through `DebugConfig`.
- `BasicTestCallLogger`, `BasicTestCaptureCallLogger` — replaced by
  `CapturingFormat` (or by direct `LogRecordCapture` use per OQ #9).

**What gets preserved:**

- `RestRequest.setDebug()` / `setDebug(Boolean)` / `isDebug()` — kept as
  one-liner shortcuts over the new `req.debug()` fluent surface.
- `CallLogger` class itself — refactored to delegate to `DebugConfig`, not
  removed.
- System-property knobs (`juneau.restLogger.*` / `JUNEAU_RESTLOGGER_*`) — kept;
  they map onto `DebugConfig` builder defaults at construction time.

**Migration notes (delivered with the PR):**

A new migration-guide section lands in
`juneau-docs/pages/topics/23.01.V9.5-migration-guide.md` (TODO-17 territory)
with explicit Old → New rows covering every removed surface. Sections:

1. **Annotation migration** — Old → New table mirroring the mapping table in
   §3 above, with side-by-side code samples.
2. **`DebugEnablement` → `DebugConfig` migration** — for the (small) population
   of users with a custom `DebugEnablement` subclass: pattern for porting
   `ReflectionMap<Enablement>` lookups onto `DebugConfig.Builder.rule(...)`.
3. **`CallLogger` rule-list migration** — pattern for porting parallel
   `normalRules` / `debugRules` setups onto unified `DebugRule` instances that
   carry both gating predicate and format.
4. **Test-side migration** — for users with `BasicTestCallLogger` /
   `BasicTestCaptureCallLogger` subclasses: pattern for replacing them with
   `CapturingFormat` (or with direct `LogRecordCapture` per OQ #9 once
   resolved).
5. **System-property migration** — none required; the `juneau.restLogger.*`
   knobs continue to work and now feed the new `DebugConfig` builder.

**Release-notes entry** in `juneau-docs/docs/pages/release-notes/9.5.0.md`
under `### juneau-rest-server` with a `**Breaking change**` callout pointing
at the migration-guide section.

Future-release impact: 9.6 onwards has no carried-over deprecated debug
surface to remove — TODO-20 lands clean in one shot.

### 7. Class summary

| Class | Package | Module | Notes |
| --- | --- | --- | --- |
| `DebugConfig` (+ `Builder`) | `org.apache.juneau.rest.debug` | `juneau-rest-server` | Replaces `DebugEnablement` + parts of `CallLogger`. |
| `DebugRule` (+ `Builder`) | `org.apache.juneau.rest.debug` | `juneau-rest-server` | Carries gate + format + level + filters. |
| `DebugResult` (record) | `org.apache.juneau.rest.debug` | `juneau-rest-server` | Resolved per request. |
| `DebugFormat` (interface) | `org.apache.juneau.rest.debug` | `juneau-rest-server` | Pluggable formatter. |
| `BasicTextFormat`, `OneLineFormat`, `JsonFormat`, `CapturingFormat` | `org.apache.juneau.rest.debug.format` | `juneau-rest-server` | Built-in formats. |
| `Slf4jStructuredFormat` | `org.apache.juneau.rest.debug.format.slf4j` | new `juneau-rest-server-slf4j` | SLF4J-only sub-module; keeps core SLF4J-free. |
| `@Debug` | `org.apache.juneau.rest.annotation` | `juneau-rest-server` | Used both as `@Rest(debug=@Debug(...))` / `@RestOp(debug=@Debug(...))` (primary, source-of-truth) and standalone on class/method (secondary, escape hatch). |
| `@Rest.debug` slot type change | `org.apache.juneau.rest.annotation` | `juneau-rest-server` | `String` → `@Debug`. Hard break — see §6. |
| `@RestOp.debug` slot type change | `org.apache.juneau.rest.annotation` | `juneau-rest-server` | `String` → `@Debug`. Hard break — see §6. |
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
9. **Integrate `org.apache.juneau.commons.logging` package + `CapturingFormat` vs
   `LogRecordCapture`**. Two related sub-questions, captured together for
   readability.

   **(a) Substrate.** The existing `juneau-commons` logging package
   (`Logger`, `LogRecord`, `LogRecordListener`, `LogRecordCapture`) is the right
   substrate for the new debug surface to sit on:
   - **`CapturingFormat`** is a thin wrapper around `LogRecordCapture` (see §2
     above) — `try-with-resources` capture, format-string assertions,
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

   **(b) `CapturingFormat` vs `LogRecordCapture`.** Two paths:
   1. Ship a dedicated `CapturingFormat` with its own `AtomicReference<String>`
      mechanism (the original draft above).
   2. **Recommended:** route every `DebugFormat` through
      `org.apache.juneau.commons.logging.Logger` (already wraps
      `java.util.logging.Logger` and supports `addLogRecordListener` +
      `captureEvents()`), and have tests use `LogRecordCapture` directly — no
      bespoke capturing format needed. The capturing API the user already has
      (`logger.captureEvents()` returning a `LogRecordCapture` `Closeable`) is
      the idiomatic test surface.

   Cost of (2): we lose the "format-output-as-string" capture niche (e.g. capture
   the exact JSON the prod system would have emitted, byte-for-byte). Mitigation:
   `LogRecord` already carries the rendered message; tests that need the rendered
   string use `cap.getRecords("{msg}")`. Decision needed before Phase 1.

10. **Source-of-truth audit follow-on TODO** (new, surfaced 2026-05-25). User has
    articulated a broader design principle: *"we want the `@Rest` and `@RestOp`
    annotations to be a source-of-truth for capabilities."* TODO-20 implements
    this principle for debug (§3 above). Are there other capabilities on
    `@Rest`/`@RestOp` today that have grown beyond their original `String`/`Class`
    slot shapes and would benefit from the same nested-typed-annotation upgrade?
    Quick scan of candidates (not exhaustive):
    - `@Rest(callLogger=Class)` → potentially `@Rest(callLogger=@CallLogger(...))`
      if `@CallLogger` ever grows fields beyond a class reference. Today it's
      just a class, so no migration needed. **Re-evaluate after TODO-20 lands**
      — if `DebugConfig` ends up subsuming most `CallLogger` configuration, the
      `callLogger` slot may shrink further or be eliminated.
    - `@Rest(swagger=...)` / `@Rest(openApi=...)` (FINISHED-74 territory) —
      these are already nested annotation types. Already source-of-truth-shaped.
      No action.
    - `@Rest(properties=...)` / `@Rest(beanProperties=...)` — already nested
      annotation types. Already source-of-truth-shaped. No action.
    - `@Rest(rolesDeclared)`, `@Rest(roleGuard)` — `String` slots that interact
      with the AuthN guards landed in FINISHED-69. Worth re-evaluating once
      TODO-69's `@Auth` surface is in user hands — they could become
      `@Rest(auth=@Auth(...))`-shaped if the roles-vs-AuthN-guard division turns
      out to be ergonomically awkward.

    Recommendation: file a follow-on **TODO-90 — `@Rest`/`@RestOp` source-of-truth
    annotation pattern audit** after TODO-20 lands, to do a systematic pass over
    every slot on both annotations and identify candidates for the
    nested-typed-annotation upgrade. Don't scope-creep TODO-20 to do this audit
    now — it would balloon the PR.

11. **Confirm the hard-break decision boundary**. §6 commits to a hard break in
    9.5 (this TODO's PR) — no deprecation cycle. This is consistent with the
    user's 2026-05-25 direction. Two micro-confirmations worth raising before
    Phase 2 starts:
    - **`RestRequest.setDebug()` / `setDebug(Boolean)` / `isDebug()`** — kept
      per §6. Confirm these stay as one-liner shortcuts and do NOT get removed
      alongside the annotation surface.
    - **System-property knobs** (`juneau.restLogger.*` / `JUNEAU_RESTLOGGER_*`)
      — kept per §6. Confirm these continue to feed `DebugConfig` builder
      defaults, even though the underlying bean shape changes.

    Recommendation: both kept. The annotation surface is the breaking change;
    the runtime/operator surface stays stable.

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

### Phase 2 — `@Debug` annotation + new typed slots on `@Rest`/`@RestOp` (hard break)

Per §6, this phase **removes** the old debug surface in the same step that lands
the new one — no back-compat adapter, no two-release deprecation cycle.

1. Add `@Debug` annotation (class + method scope per §3).
2. Add typed `debug=@Debug(...)` slot to `@Rest` and `@RestOp`. Old `String`
   `debug` / `debugDefault` / `debugEnablement` / `debugOn` attributes on `@Rest`
   and `String debug` on `@RestOp` are **deleted** in this step (not deprecated).
3. `RestContext.debugEnablement` memoizer is **deleted**. The new
   `RestContext.debugConfig` memoizer reads `@Rest(debug=@Debug(...))`,
   `@RestOp(debug=@Debug(...))`, and standalone `@Debug` per the precedence rules
   in §3, builds a `DebugConfig`, publishes it into the BeanStore.
4. `Rest_Debug_Test` (the canonical 1085-line covers-everything test) is
   **rewritten** to use the new annotation surface — the existing test matrix
   moves over to `@Rest(debug=@Debug(...))` / `@RestOp(debug=@Debug(...))` /
   standalone `@Debug` placements. No "parallel matrix" — the old surface no
   longer exists.
5. Tests:
   - `Debug_Annotation_Test` — full annotation matrix on the new surface, covering
     all three placement options (nested-on-`@Rest`, nested-on-`@RestOp`, standalone)
     and the precedence rules from §3.
   - `Debug_SourceOfTruth_Test` — confirms that the source-of-truth principle
     reads correctly: a single `@Rest(debug=@Debug(...))` produces the same
     resolved `DebugConfig` as the equivalent standalone `@Debug` on the class.

### Phase 3 — runtime fluent surface + FINISHED-35 integration

**FINISHED-35** is landed, so both forms below ship in this phase.

1. Add `RestRequest.debug()` fluent (returns a `DebugScope` per-request handle).
   Keep `setDebug` / `isDebug` as one-liners on top of it.
2. Add `MockRestClient.Builder.debugConfig(DebugConfig)` for direct test wiring
   (parallel to FINISHED-35's `.overridingBeanStore(...)`).
3. Tests:
   - `Debug_Runtime_Test` — `req.debug().enable(JsonFormat.class).level(FINE)`
     works.
   - `Debug_TestBean_Test` — uses `@TestBean DebugConfig` +
     `JuneauBeanStoreExtension` (from FINISHED-35) to swap formats and capture
     output without rebuilding `MockRestClient`.

### Phase 4 — migration notes + release notes (juneau-docs)

No code removal here — Phase 2 already removed the old surface. This phase is
documentation-only.

1. New migration-guide section in
   `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md` (TODO-17 territory)
   covering the five Old → New migration tracks per §6:
   - Annotation migration (Old → New table from §3).
   - `DebugEnablement` → `DebugConfig` migration (custom-subclass porting pattern).
   - `CallLogger` rule-list migration (parallel `normalRules`/`debugRules` →
     unified `DebugRule`).
   - Test-side migration (`BasicTestCallLogger` / `BasicTestCaptureCallLogger` →
     `CapturingFormat` or `LogRecordCapture` per OQ #9).
   - System-property migration (none required; auto-mapped).
2. Release-notes entry in `juneau-docs/docs/pages/release-notes/9.5.0.md` under
   `### juneau-rest-server` with a `**Breaking change**` callout pointing at the
   migration-guide section.
3. New topic page or sub-section under
   `juneau-docs/pages/topics/10.20.RestServerDebug.md` (or equivalent slot — pick
   the cleanest spot in the existing topics tree) walking through the new
   `@Debug` placement options, the source-of-truth principle, and worked
   examples for each `DebugFormat` built-in.
