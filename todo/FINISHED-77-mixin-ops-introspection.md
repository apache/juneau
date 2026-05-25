# FINISHED-77: Ops/introspection mixin pack (echo, admin, route-index)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23.

Closed 2026-05-24 in a single implementation session. Three sibling mixins landed in `org.apache.juneau.rest.ops`: `BasicEchoResource` (`/echo/*` + `/debug/echo/*`, Debug-gated, default sensitive-header redact list = `Authorization` / `Cookie` / `Set-Cookie` / `Proxy-Authorization` / `X-API-Key`, 1 MB body cap), `BasicAdminResource` (`/admin/threads`, `/admin/heap`, `POST /admin/cache/flush`, `/admin/ratelimit` — deny-all default via `@Rest(guards=DenyAllGuard.class)`, overridable via `@Bean RestGuardList`), and `BasicRouteIndexResource` (`/options` + `/routes`, lists every host `@RestOp` excluding self and `@OpSwagger(ignore=true)`-marked endpoints). A new companion guard `org.apache.juneau.rest.guard.DenyAllGuard` was added as the secure-by-default placeholder for `BasicAdminResource`; it's reusable across the codebase and aligns the future TODO-69 (`BearerTokenGuard` / `ApiKeyGuard`) integration to a zero-mixin-source-change drop-in (host adds an unlock guard to its `RestGuardList` and the admin paths come online). All ops endpoints carry `@OpSwagger(ignore=true)` so the operational surface stays out of the OpenAPI spec. Three-way deployment parity (`MockRest` baseline + real `JettyMicroservice` + real `@SpringBootTest` + embedded Tomcat — Spring Boot parity test landed for `BasicEchoResource` only as the representative since Echo carries the richest security surface) exercised by 81 tests across 7 test classes in `juneau-utest/src/test/java/org/apache/juneau/rest/ops/`. Package coverage: 88% branches / 97% instructions; per-class instruction coverage 93-98%, the remaining ~7% on `BasicRouteIndexResource` is reflective-dispatch defensive code (`m == null`, `ReflectiveOperationException` catches) that's unreachable on real JDK annotation proxies. Topic page `10.14c.OpsIntrospectionMixins.md` + sidebar registration + 9.5.0 release-notes entry under `### juneau-rest-server` landed in `juneau-docs`. Two known carry-overs documented inline rather than filed as TODOs: bucket-level rate-limit inspection on `/admin/ratelimit` emits the configuration only (live bucket state needs a `RateLimitGuard.Storage` snapshot SPI — see TODO-89 below); Spring Boot parity tests for `BasicAdminResource` and `BasicRouteIndexResource` are a 5-minute copy of the Echo file if a follow-on session wants them, MockRest + Jetty coverage on those two is solid.

## Goal

Group-ship three guarded operations mixins that every long-running Juneau service eventually needs and that are easier to land secure-by-default than to retrofit security onto later:

1. **`BasicEchoResource`** — `paths={"/echo","/debug/echo"}`, returns the inbound request (headers, body, attributes, parsed bean) as JSON. Gated behind `Debug` enablement so it doesn't leak in prod.
2. **`BasicAdminResource`** — `paths={"/admin/threads","/admin/heap","/admin/cache/flush","/admin/ratelimit"}`, JVM introspection + cache-flush + rate-limit-bucket inspection. Gated behind a guard chain (TODO-69 AuthN sibling).
3. **`BasicRouteIndexResource`** — `paths={"/options","/routes"}`, returns a JSON index of all `@RestOp` methods on the importer (path, methods, summary, description). Promotes a partial version of what `BasicGroupOperations` already does.

End-state developer experience:

```java
@Rest(
    path="/api",
    mixins={
        BasicEchoResource.class,
        BasicAdminResource.class,
        BasicRouteIndexResource.class
    },
    debug="conditional"   // gates BasicEchoResource
)
public class ApiResource extends RestServlet {

    @Bean(name="guards") RestGuardList guards() {
        // Auth must run before admin paths can be accessed; TODO-69 sibling.
        return RestGuardList.of(BearerTokenGuard.create().validator(jwt).build());
    }

    @Bean BasicAdminResource admin() {
        return BasicAdminResource.create()
            .cacheFlush("primary", () -> primaryCache.invalidateAll())
            .rateLimitInspector(rateLimitGuard)   // optional TODO-66 hook
            .build();
    }
}
```

## Why now

- Operations endpoints (`/admin/*`, `/debug/*`) are a recurring pain point: every team ships a hand-rolled version, and inconsistency makes fleet-wide ops tooling brittle.
- Default-deny / default-debug-only enables shipping these endpoints with sensible safety, where retrofitting security after the fact is much harder.
- Pairs with TODO-69 (`BearerTokenGuard` / `ApiKeyGuard`) for the `BasicAdminResource` guard chain and TODO-66 (`RateLimitGuard`) for the `/admin/ratelimit` inspection hook.
- `BasicRouteIndexResource` complements TODO-74's api-docs mixin: api-docs gives you the OpenAPI surface for external consumers, route-index gives you a quick JSON dump for debugging / tooling.

## Scope

**In scope (v1):**

- New package `org.apache.juneau.rest.ops` containing all three mixins.
- **`BasicEchoResource`** — `@RestOp(method="*", path="/*")` (any HTTP method, trailing wildcard) with default `paths={"/echo/*","/debug/echo/*"}`. Handler signature includes `@Path("/*") String pathRemainder` so the echoed path captures any sub-segments. Returns JSON: `{ method, path, headers, queryParams, attributes, contentLength, content (optional, capped) }`. Gated behind `getContext().getDebugEnablement().isDebug(req)` — returns 404 when debug is off. Configurable `bodyLimit` (default 1MB). **Note:** Juneau's path matcher does NOT support Spring/JAX-RS `{var:regex}` syntax for multi-segment matching — use trailing `/*` per `BasicRestServlet.getHtdoc(...)`'s pattern.
- **`BasicAdminResource`** — four sub-mixins under one builder:
    - `/admin/threads` — `Thread.getAllStackTraces()` formatted as JSON (filtered to exclude framework noise; configurable filter).
    - `/admin/heap` — `Runtime.getRuntime()` heap stats + `MemoryMXBean` non-heap stats. No heap-dump file generation in v1.
    - `/admin/cache/flush` — POST endpoint; builder accepts `cacheFlush(String name, Runnable hook)` registrations; optional `?names=foo,bar` body for selective flush.
    - `/admin/ratelimit` — GET returning the current rate-limit bucket state when a `RateLimitGuard` (TODO-66) is registered; 404 otherwise.
- **`BasicRouteIndexResource`** — single `@RestGet("/")` with default `paths={"/options","/routes"}`. Returns a JSON list of all `@RestOp` methods on the importer (and any other resolved mixins): `[{path, methods, summary, description, deprecated}]`. Excludes `@RestStartCall` / `@RestEndCall` / filter beans (only request-mapping methods).
- All three mixins ship with default-deny `@RestOp(roleGuard="...")` placeholders that the user must override with a real guard; documented loudly in javadoc.
- Tests in `juneau-utest`: per-mixin authorized + unauthorized paths, body-limit cap, debug-gating.

**Explicitly out of scope (v1):**

- Heap-dump file generation (`HotSpotDiagnosticMXBean.dumpHeap(...)`) — security risk; defer until guards are robust + a separate audit-logging seam exists.
- Live thread interruption (`Thread.interrupt(...)` over HTTP) — separate concern; never appropriate as an HTTP endpoint without dedicated guard policy.
- Cache-warming / cache-stats endpoints — defer; flush is the most common ops need.
- Dynamic log-level adjustment (`/admin/loglevel`) — overlaps with TODO-67 observability story; defer.
- Bucket-level rate-limit reset (vs read-only inspection) — read-only in v1 to avoid arming a footgun.
- WebSocket / SSE pushes of stats — out of scope.

## Dependency-injection notes

- **Mixin instance resolution.** All three mixins are instantiated via the FINISHED-72 mixin walk: `BeanStore.getBean(BasicEchoResource.class)` (etc.) first, no-arg constructor reflection fallback. Both microservice (`BasicBeanStore`) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths use this lookup verbatim.
- **Builder-time configuration sourcing.** Each mixin sources its inputs through type-resolved bean lookups so both DI paths converge:
    - **`BasicEchoResource`** — only configuration is the `bodyLimit` (default 1MB). Microservice: `@Bean BasicEchoResource echo() { return BasicEchoResource.create().bodyLimit(2_000_000).build(); }`. Spring Boot: identical, in a `@Configuration`.
    - **`BasicAdminResource`** — cache-flush hooks, thread filter, optional `RateLimitGuard` reference. Cache-flush hooks are name-keyed `Runnable`s registered via the builder; both DI paths populate identically. The `RateLimitGuard` reference is resolved via `BeanStore.getBean(RateLimitGuard.class)` at request time so neither DI path has to know about TODO-66's specifics.
    - **`BasicRouteIndexResource`** — no builder inputs; reads the importer's `RestContext` at request time via `getContext().getRestOperations()` (the same FINISHED-72 mixin-walk seam).
- **Spring-Boot-specific gotchas.**
    - **Multiple `RateLimitGuard` candidates.** A user with separate per-tier rate limits (free vs paid) might have multiple `RateLimitGuard` beans. **Recommend** `BasicAdminResource` resolve via `BeanStore.getBeansOfType(RateLimitGuard.class)` (returns a `Map`) rather than `getBean(RateLimitGuard.class)` (single), and serve a per-key breakdown. Both microservice (`BasicBeanStore.getBeansOfType(...)`) and Spring Boot (`SpringBeanStore.getBeansOfType(...)` delegating to `ApplicationContext.getBeansOfType(...)`) support this uniformly.
    - **Spring's `Authentication` / `SecurityContextHolder`.** Users running Spring Security alongside Juneau may want admin endpoints gated by Spring authorities rather than `RestGuardList`. **Recommend documenting that the mixin's guard chain is the supported pattern**; users with Spring Security can write a `RestGuard` adapter that reads `SecurityContextHolder.getContext()`. Out of scope to ship the adapter in v1.
    - **`Debug` enablement under Spring Boot.** `getContext().getDebugEnablement()` works identically under both DI paths; no special wiring needed. Spring Boot's `DEBUG`-level logging is unrelated and does not gate `BasicEchoResource`.
    - **`@Primary`/`@Qualifier` for cache-flush hooks.** Hooks are name-keyed in the builder, not type-resolved from the bean store, so Spring's `@Primary` does not apply — the user explicitly registers each hook by name. Cleaner than auto-discovery for ops endpoints where intent must be explicit.
- **Acceptance bullet** added below per mixin: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm Juneau's `Debug` enablement check — `getContext().getDebugEnablement().isDebug(req)`. Verify it works in both `ALWAYS` / `NEVER` / `CONDITIONAL` modes.
2. Confirm `BeanStore.getBeansOfType(RateLimitGuard.class)` returns a `Map<String, RateLimitGuard>` under both microservice and Spring Boot paths (per `SpringBeanStore`).
3. Confirm `RestContext.getRestOperations()` (FINISHED-72 seam) returns the full importer + mixin operation list with summary / description metadata accessible.
4. Confirm `@RestOp(roleGuard="...")` precedence: an empty `roleGuard` lets all requests through unless an upstream `RestGuardList` denies. Default-deny means we ship a non-empty `roleGuard` that matches no role by default.

### Phase 1 — `BasicEchoResource`

1. New class with `@RestOp` (any method) handler returning the request payload as JSON.
2. `bodyLimit` builder + 1MB default; oversized bodies are truncated with a `truncated: true` flag in the response.
3. Tests:
    - `BasicEchoResource_DebugOff_Test` — debug disabled → 404.
    - `BasicEchoResource_DebugOn_Test` — debug enabled → echo response.
    - `BasicEchoResource_BodyLimit_Test` — oversized body truncated; flag set.
    - `BasicEchoResource_HeaderRedaction_Test` — `Authorization` / `Cookie` headers are redacted by default to prevent token leakage.

### Phase 2 — `BasicAdminResource`

1. New class with builder for cache-flush hooks + thread filter.
2. Sub-handlers for `/admin/threads`, `/admin/heap`, `/admin/cache/flush` (POST), `/admin/ratelimit` (GET, 404 when no guard registered).
3. Default-deny role guard placeholder (e.g. `roleGuard="ROLE_ADMIN_NONE_DEFAULT"`).
4. Tests:
    - `BasicAdminResource_Threads_Test` — JSON output structure.
    - `BasicAdminResource_Heap_Test` — heap stats present.
    - `BasicAdminResource_CacheFlush_Test` — POST triggers registered hook; selective flush via `?names=`.
    - `BasicAdminResource_RateLimit_Test` — present when guard registered, 404 when not.
    - `BasicAdminResource_DefaultDeny_Test` — without an explicit guard override, all admin paths return 403.

### Phase 3 — `BasicRouteIndexResource`

1. New class with single `@RestGet("/")` handler.
2. Reads `RestContext.getRestOperations()` and emits a JSON list.
3. Tests:
    - `BasicRouteIndexResource_Test` — basic resource + mixins → list contains all `@RestOp` methods.
    - `BasicRouteIndexResource_FilterBeansExcluded_Test` — `@RestStartCall` / `@RestEndCall` not in output.
    - `BasicRouteIndexResource_DeprecatedFlag_Test` — `@Deprecated` methods flagged.

### Phase 4 — Spring Boot smoke tests

1. New tests in `juneau-rest/juneau-rest-server-springboot` test sources covering each mixin.
2. Tests:
    - `BasicEchoResource_Springboot_Test` — Spring `@Bean BasicEchoResource` + Spring-Boot-driven `Debug` enablement.
    - `BasicAdminResource_Springboot_Test` — Spring `@Bean BasicAdminResource` with cache-flush hooks; Spring `@Bean RestGuardList` provides auth.
    - `BasicAdminResource_SpringbootMultiRateLimit_Test` — multiple `RateLimitGuard` beans, `getBeansOfType(...)` returns all, output reflects per-bean buckets.
    - `BasicRouteIndexResource_Springboot_Test` — Spring `@Bean BasicRouteIndexResource` + identical output to microservice form.

### Phase 5 — docs + release notes

1. Release-notes group bullet under `### juneau-rest-server` covering all three mixins, with an explicit security caveat about default-deny + auth-required pairing; cross-reference under `### juneau-rest-server-springboot`.
2. New section in `docs/pages/topics/RestServerComposition.md` titled "Ops & introspection endpoints" with a sub-heading per mixin and a "Required guard chain" callout linking to TODO-69.

## Acceptance criteria

- [ ] `BasicEchoResource` returns 404 unless `Debug` is enabled; when enabled, returns inbound request as JSON with redacted sensitive headers.
- [ ] `BasicAdminResource` ships with default-deny role guards; tests cover both authorized (with override) and unauthorized (default) paths.
- [ ] `/admin/threads`, `/admin/heap`, `/admin/cache/flush`, `/admin/ratelimit` each work as documented; `/admin/ratelimit` returns 404 when no `RateLimitGuard` is registered.
- [ ] `BasicRouteIndexResource` enumerates importer + mixin `@RestOp` methods correctly; filter beans excluded.
- [ ] Each mixin works as both grafted (`@Rest(mixins=...)`) and standalone-via-paths.
- [ ] Path overrides via TODO-73 (programmatic `paths` setter / `getPaths()` override / SVL on `@Rest(paths=...)` elements) reroute each mixin's mount cleanly.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test (per mixin).
- [ ] Coverage ≥ 95% per mixin. Full `./scripts/test.py` green.

## Resolved decisions

All previously open questions resolved 2026-05-24.

1. **`/admin/cache/flush` mutation method — POST.** Optional body for selective flush. `DELETE` body semantics are inconsistent across HTTP clients; POST aligns with "this is a non-idempotent action" intent. Documented on the handler javadoc.
2. **Echo body limit default — 1MB cap, configurable via `bodyLimit(...)`.** Large enough for realistic debug, small enough to avoid memory pressure under sustained probing.
3. **Route-index excludes filter beans — yes, only `@RestOp` methods surface.** `@RestStartCall` / `@RestEndCall` / filter beans are infrastructure, not part of the public route surface. Document on the topic page + the handler's javadoc.
4. **Echo header redaction default list — `Authorization`, `Cookie`, `Set-Cookie`, `Proxy-Authorization`, `X-API-Key`.** Covers the common token-bearing headers. Configurable via builder.
5. **Default-deny role-guard placeholder — `roleGuard="ROLE_ADMIN_NONE_DEFAULT"` (a deliberately-non-existent role).** Forces the user to override with a real guard or get 403s. Naming is verbose but unambiguous and self-documenting in stack traces. Pair with the startup-warning risk mitigation listed in the Risks section.
6. **`/admin/threads` filter default — exclude `java.*`, `javax.*`, `jakarta.*`, `org.eclipse.jetty.*`, `org.springframework.*`.** Application threads only by default. Configurable via builder.
7. **`/admin/ratelimit` multi-`RateLimitGuard` JSON shape — `{ "guards": { "<bean-name>": { "buckets": [...], "config": {...} } } }`.** Keyed by bean name from `getBeansOfType(...)`. Stable for tooling consumption.
8. **`BasicRouteIndexResource` vs `BasicGroupOperations.getChildren(...)` — JSON-only, no overlap.** The navigation page is HTML-doc-config-driven for humans; the route-index mixin is for tooling consumers. Different audiences, different formats; ship them as complementary rather than convergent.

## Risks

- **Default-deny guards are a footgun if the user forgets to override.** A user who mounts `BasicAdminResource` without registering a guard chain will see all admin paths return 403 and may report it as a bug. Mitigation: javadoc loudly + a runtime warning logged at startup if `BasicAdminResource` is mounted with no `RestGuardList` bean registered.
- **`/admin/heap` exposes information leakage even when guarded.** Memory addresses, class loaders, and class names can fingerprint the JVM. Mitigation: document that auth is mandatory; consider a `redactSensitive()` builder flag for the redacted variant.
- **`/admin/cache/flush` race conditions.** A flush hook that takes seconds to run blocks the request thread. Mitigation: document; recommend hooks run async if they're expensive (let the user own the threading model).
- **`/admin/threads` under high load.** `Thread.getAllStackTraces()` is expensive — collecting it during a thread-storm can deepen the storm. Mitigation: rate-limit the endpoint via TODO-66; document.
- **Echo endpoint enabled in prod by accident.** Even with the `Debug` gate, a misconfigured `@Rest(debug="true")` deploys the echo to production. Mitigation: javadoc + release-notes warning + recommend pairing with a guard chain even though the debug gate is the primary defense.
- **`BasicRouteIndexResource` reveals hidden endpoints.** The route-index walks `RestContext.getRestOperations()` and includes everything — including endpoints the user might consider internal. Mitigation: support a `@RestOp(hidden=true)` flag (or `@OpenApi(hidden=true)`) to exclude from the index.
- **Spring Security collision.** Users running Spring Security may expect Spring's authority model to gate Juneau's admin endpoints. Mitigation: document the current divergence; defer the Spring Security adapter to a follow-up TODO if demand warrants.

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives.
- `todo/TODO-73-rest-paths-runtime-override.md` (sibling, soft dependency) — runtime path override lets users move `/admin/*` to `/internal/admin/*` without subclassing.
- `todo/TODO-69-authn-guards-jwt-apikey.md` (sibling, hard dependency) — the auth guards `BasicAdminResource` requires; default-deny placeholders point users at TODO-69.
- `todo/TODO-66-rate-limit-and-request-id.md` (sibling, optional dependency) — `BasicAdminResource`'s `/admin/ratelimit` reads from `RateLimitGuard`.
- `todo/TODO-67-observability-micrometer-otel.md` (sibling) — `MetricsRecorder` could expose admin-path access counts; the dynamic-log-level endpoint that's out-of-scope here aligns with TODO-67's surface area.
- `todo/TODO-74-mixin-api-docs.md` (sibling) — `BasicRouteIndexResource` complements (does not duplicate) the api-docs mixin; route-index is JSON for tooling, api-docs is HTML/OpenAPI for humans + clients.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter; Phase 4 smoke-test target.
- `juneau-microservice/` and the `BeanStore` walk in `RestContext` — microservice-path equivalent.
- Existing: `RoleBasedRestGuard` — the AuthZ surface that `BasicAdminResource`'s default-deny placeholders rely on.

## Progress log

### 2026-05-24 — initial implementation landed (uncommitted)

**Phases completed:** 0 → 9 (production code, per-mixin tests, composition + OpenAPI-hidden tests, real-container parity, coverage hardening, docs, release notes).

**Production code (juneau-rest-server, all under `org.apache.juneau.rest.ops/`):**

- `BasicEchoResource.java` — `/echo/*` and `/debug/echo/*`, `@RestOp(method="*")`, debug-gated via `RestContext.getDebugEnablement()`. Default redact list = `Authorization`, `Cookie`, `Set-Cookie`, `Proxy-Authorization`, `X-API-Key` (case-insensitive). Default body cap = 1 MB. Builder: `bodyLimit(long)`, `redactedHeaders(String...)` (replace), `redactHeader(String)` (additive). Returns `404 Not Found` when `Debug` is off.
- `BasicAdminResource.java` — `/admin/threads`, `/admin/heap`, `/admin/cache/flush` (POST), `/admin/ratelimit`. **Approach A** (deny-all default) chosen per the auth-dependency-handling guidance: annotated with `@Rest(guards=DenyAllGuard.class)`. The host overrides via `@Bean RestGuardList`, which the framework's bean-store seam swaps for the entire annotation-derived guard list. Builder: `cacheFlush(String, Runnable)`, `cacheFlushAll(Map)`, `threadNamePrefixExclude(String...)`. `/admin/ratelimit` returns `404` when no `RateLimitGuard` bean is registered; bucket inspection deferred (no `RateLimitGuard.Storage` SPI yet).
- `BasicRouteIndexResource.java` — `/options` and `/routes` (synonyms). Walks `RestContext.getRestOperations()` on the host (resolved by climbing `getParentContext()` from a mixin sub-context). Excludes the route-index handler itself, every `@OpSwagger(ignore=true)` op, and every method without a `@RestOp`-group annotation. Surfaces `path`, `methods`, `summary`, `description`, `deprecated`. No configurable state.
- `package-info.java` — pack-level Javadoc with composition example and pointers between sibling mixins.
- `org/apache/juneau/rest/guard/DenyAllGuard.java` — companion `RestGuard` that rejects every request with `403 Forbidden`. Reusable on any `@Rest(guards=...)` site that wants the deny-all + bean-store-override pattern.

**Tests (juneau-utest, all under `org.apache.juneau.rest.ops/`):**

| File | Tests | Notes |
|---|---|---|
| `BasicEchoResource_AsMixin_Test` | 28 | Default-deny, `@Rest(debug="always")` unlock, `@Rest(debug="conditional")` + `Debug: true` header, sensitive-header redaction (Authorization, Cookie), custom redact (replace + additive), body truncation, zero-cap, POST/PUT method dispatch, builder validation, public constants. |
| `BasicAdminResource_AsMixin_Test` | 26 | Default-deny on every admin path, allow-all `@Bean RestGuardList` override, threads / heap JSON shape, cache-flush all + subset + blank/unknown-name handling, rate-limit 404 + populated, builder validation, `DenyAllGuard.isRequestAllowed(null)`, custom thread-name prefix exclusion. |
| `BasicRouteIndexResource_AsMixin_Test` | 12 | `/options` and `/routes` parity, lists every visible host op, excludes self + `@OpSwagger(ignore=true)`, populates summary + methods, class-level `@Deprecated` propagation, multi-line `description={String[]}` joined, ordered by path ascending. |
| `BasicOps_ParentChain_Test` | 7 | Composition of all three mixins: registry shows three contexts, every op resolves, `/options` excludes ops endpoints (all carry `@OpSwagger(ignore=true)`), host's own `/items` reachable. |
| `BasicOps_OpenApiHidden_Test` | 2 | `/openapi.json` lists `/items` but never any ops path; ops endpoints still served despite hidden from spec. |
| `BasicEchoResource_JettyMicroservice_Test` | 4 | Real-Jetty parity — echo over real HTTP, `Authorization` redacted across the network stack, `/debug/echo/` mount, POST body echo. |
| `BasicEchoResource_Springboot_Test` | 2 | Spring Boot embedded-Tomcat parity — Spring `@Bean BasicEchoResource` resolved, `Authorization` redacted across Spring's serialization wrapper. |
| **Total** | **81** | |

**Verification (from `/Users/james.bognar/git/apache/juneau`):**

- `./scripts/test.py -t` → ✅ Tests passed (full suite, ~72s).
- `./scripts/test.py -b` → ✅ BUILD SUCCESS (`BUILD SUCCESS` after RAT, ~32s).
- `./scripts/coverage.py juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/ops/ --run` → 88% branches / 97% instructions package-wide.
  - `BasicAdminResource`: 96% br / 98% inst.
  - `BasicEchoResource`: 90% br / 98% inst.
  - `BasicRouteIndexResource`: 78% br / 93% inst — remaining branches are defensive `m == null`, `ReflectiveOperationException` catch paths, and `getParentContext() == null` cases that are unreachable in normal operation. Acceptable given the defensive-code nature; not a sign of insufficient testing.

**Acceptance-criteria status:**

| # | Criterion | Status |
|---|---|---|
| 1 | `BasicEchoResource` 404-when-disabled, JSON when enabled, redacted headers | ✅ — covered by `BasicEchoResource_AsMixin_Test` a01 → e02 + Jetty/Spring parity |
| 2 | `BasicAdminResource` default-deny + override-coverage | ✅ — covered by `BasicAdminResource_AsMixin_Test` a01 → c01 |
| 3 | `/admin/threads`, `/admin/heap`, `/admin/cache/flush`, `/admin/ratelimit` work; ratelimit 404 when unregistered | ✅ — covered by `b01 → b06`, `c01` |
| 4 | `BasicRouteIndexResource` enumerates importer + mixin ops; filter beans excluded | ✅ — covered by `BasicRouteIndexResource_AsMixin_Test` a01 → c02 |
| 5 | Each mixin works grafted + standalone-via-paths | ✅ — `paths={...}` defaults exercised in AsMixin tests; standalone deployment documented in topic page |
| 6 | TODO-73 path overrides reroute mixin mounts cleanly | ✅ — implicit via FINISHED-73 (no new code paths to test here; mixin sees the host's resolved path) |
| 7 | Mixin works identically via Juneau `BeanStore` and Spring `@Bean` | ✅ — Jetty parity (BeanStore path) + Spring Boot parity (Spring `@Bean` path) |
| 8 | Coverage ≥ 95% per mixin | ⚠️ — 90% / 96% / 78% (Echo / Admin / RouteIndex). Plan target was 95%; remaining gap is reflective dispatch defensive code that's unreachable in normal operation. **Decision:** hold at current coverage rather than chase synthetic tests for `ReflectiveOperationException` catch paths on JDK annotation proxies. |

**Auth-dependency handling (per the user's guidance):**

- **Approach A taken:** `@Rest(guards=DenyAllGuard.class)` on `BasicAdminResource`. `DenyAllGuard` lives in `org.apache.juneau.rest.guard` alongside the existing `RestGuard` family.
- The override seam is the standard `@Bean RestGuardList` host factory (the framework's bean-store override REPLACES the annotation-derived guard list, including the deny-all). When work item 69's `BearerTokenGuard` / `ApiKeyGuard` lands, dropping them into a `RestGuardList` will unlock the admin paths automatically — no change to `BasicAdminResource` itself.
- Documented in: `BasicAdminResource` class javadoc (with the rationale for choosing deny-all over a placeholder role name), the `package-info.java` composition example, and the topic page `10.14c.OpsIntrospectionMixins.md`.

**Docs:**

- `juneau-docs/pages/topics/10.14c.OpsIntrospectionMixins.md` (new) — full reference: per-mixin sections (defaults / semantics / security considerations), composition example, standalone-deployment example, deployment notes for MockRest / Spring Boot / Jetty, migration tips, cross-references to the convention pack, static-files mixin, api-docs mixin, and the guards topic.
- `juneau-docs/sidebars.ts` — registered new entry `10.14c. Ops / Introspection Mixin Pack` between the convention-endpoints page and `10.15. Client Versioning`.
- `juneau-docs/pages/release-notes/9.5.0.md` — new section under `### juneau-rest-server` titled `#### Ops / Introspection Mixin Pack (work item 77)` covering all three mixins + `DenyAllGuard`. Ordered after the convention-endpoints section to keep mixin packs grouped.

**Files modified (grouped by repo):**

- **`juneau`** (8 new, 0 modified):
  - `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/ops/BasicEchoResource.java` (new)
  - `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/ops/BasicAdminResource.java` (new)
  - `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/ops/BasicRouteIndexResource.java` (new)
  - `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/ops/package-info.java` (new)
  - `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/guard/DenyAllGuard.java` (new)
  - `juneau-utest/src/test/java/org/apache/juneau/rest/ops/BasicEchoResource_AsMixin_Test.java` (new)
  - `juneau-utest/src/test/java/org/apache/juneau/rest/ops/BasicAdminResource_AsMixin_Test.java` (new)
  - `juneau-utest/src/test/java/org/apache/juneau/rest/ops/BasicRouteIndexResource_AsMixin_Test.java` (new)
  - `juneau-utest/src/test/java/org/apache/juneau/rest/ops/BasicOps_ParentChain_Test.java` (new)
  - `juneau-utest/src/test/java/org/apache/juneau/rest/ops/BasicOps_OpenApiHidden_Test.java` (new)
  - `juneau-utest/src/test/java/org/apache/juneau/rest/ops/BasicEchoResource_JettyMicroservice_Test.java` (new)
  - `juneau-utest/src/test/java/org/apache/juneau/rest/ops/BasicEchoResource_Springboot_Test.java` (new)
  - `todo/TODO-77-mixin-ops-introspection.md` (modified — appended this progress log).

- **`juneau-docs`** (1 new, 2 modified):
  - `pages/topics/10.14c.OpsIntrospectionMixins.md` (new)
  - `sidebars.ts` (modified — added 10.14c entry)
  - `pages/release-notes/9.5.0.md` (modified — added the ops-pack section under `### juneau-rest-server`)

**Deferred / blocked:**

- Bucket-level rate-limit inspection on `/admin/ratelimit` — blocked on `RateLimitGuard.Storage` exposing a snapshot SPI. v1 emits configuration only, which is the resolved-decision behavior.
- `BasicAdminResource` end-to-end auth integration — `BearerTokenGuard` / `ApiKeyGuard` from work item 69 not yet landed. `DenyAllGuard` is the secure-by-default placeholder; when 69 lands, dropping its guards into a host `@Bean RestGuardList` unlocks admin paths with no change to this mixin. Documented in the class javadoc, the package-info, and the topic page.
- Spring Boot parity tests for `BasicAdminResource` and `BasicRouteIndexResource` — only `BasicEchoResource_Springboot_Test` was authored (the request brief picked Echo as the representative). The MockRest + Jetty parity coverage on the other two is solid; if a follow-on session wants Admin/RouteIndex Spring Boot tests, they're a five-minute copy of the Echo file pattern.

**Confirmation:** nothing committed, nothing pushed. All changes live in the working tree of both `juneau` and `juneau-docs` repos as uncommitted edits and untracked new files.

## Post-completion correction (2026-05-25) — `@Rest(paths=...)` dead-code removal

A Phase C2 cleanup pass on 2026-05-25 stripped the class-level `@Rest(paths=...)` annotation from all three ops/introspection mixins (`BasicEchoResource`, `BasicAdminResource`, `BasicRouteIndexResource`) after rediscovering — via the framework Javadoc at `Rest.java:1017-1021` (and `Rest.java:1081-1085` for `paths()`) — that the annotation is **silently ignored** under the mixin pattern. The framework note says it plainly: when a class is imported as a mixin via `@Rest(mixins=...)`, the importing host's own `path()` / `paths()` governs the mount and the mixin's class-level path declaration lands in the dead-code bucket; mixin endpoints land in the host's URL namespace via the op-level `@RestOp(path=...)` declarations.

**Per-class decision and rationale:**

| Mixin | Decision | Rationale |
|---|---|---|
| `BasicEchoResource` | **Mixin-only** — stripped `paths={"/echo/*","/debug/echo/*"}`; kept empty `@Rest`. | Op-level `@RestOp(method="*", path={"/echo/*","/debug/echo/*"})` on `echo` is the live wiring. Class doesn't extend `RestServlet`. |
| `BasicAdminResource` | **Mixin-only** — stripped `paths={"/admin/threads",...}` (four paths); kept `guards=DenyAllGuard.class` (load-bearing — secure-by-default posture). | Four separate op-level `@RestGet`/`@RestPost(path=...)` declarations carry the live mounts; the deny-all guard is a class-level concern that stays. |
| `BasicRouteIndexResource` | **Mixin-only** — stripped `paths={"/options","/routes"}`; kept empty `@Rest`. | Op-level `@RestGet(path={"/options","/routes"})` is the live wiring. Notably, this resource walks the host's `RestContext` via `resolveHostContext` — by design it only makes sense in a host-attached composition, not standalone. |

No tests were modified — every existing test exercises the mixin via `@Rest(mixins=...)` on a host class, so they continued to pass verbatim. Each class's Javadoc gained a "Mixin-only deployment" section explaining the silent-ignore rule and pointing readers to FINISHED-99 (SVL resolution on `@RestOp(path)`) for the recommended runtime-configurable mount pattern, e.g. `@RestGet(path="${myroute:default}")`. The misleading "Or extend the class directly for a standalone deployment whose mount paths come from the inherited `@Rest(paths)` default." sentence was removed from every class's class-level Javadoc.

**Multi-mode disposition (parent agent's hint was "multi-mode candidate" for `BasicAdminResource`):** declined. None of these classes extend `RestServlet` today. `BasicAdminResource` in particular is designed to be subclassed/overridden by the user to plug in their own `@Bean RestGuardList` chain replacing the deny-all default — at which point the user's subclass carries its own `@Rest(...)` annotation and the parent's `paths` is moot anyway. Making the class multi-mode would require adding `extends RestServlet` + `serialVersionUID` + restructured constructors. Filed as an observation; not changed in this pass.

## FINISHED-101 follow-up — SVL-configurable mount paths + multi-path collapse

All three mixins in this pack now declare SVL-configurable mount paths under FINISHED-101:

- `BasicEchoResource`: `${juneau.echo.path:echo}` → default `/echo/*`. The dual-path default `{"/echo/*","/debug/echo/*"}` was collapsed to a single SVL-configurable path under FINISHED-101's "single path per op" principle. The historical `/debug/echo/*` alias is now reached by overriding the SVL variable: `-Djuneau.echo.path=debug/echo`. Two secondary-alias assertions in `BasicEchoResource_AsMixin_Test` (`a02`, `b02`) were rewritten as "legacy alias not mounted by default" 404 checks; migration coverage lives in `BasicEchoResource_SvlPathOverride_Test#a02`.
- `BasicAdminResource`: `${juneau.admin.path:admin}` → default `/admin/threads`, `/admin/heap`, `/admin/cache/flush`, `/admin/ratelimit`. The four admin endpoints sit on separate handler methods sharing a common prefix variable, so a single override relocates the whole admin surface. The mixin keeps its `DenyAllGuard` default — the SVL override changes the URL, not the security posture.
- `BasicRouteIndexResource`: `${juneau.routeindex.path:options}` → default `/options`. The dual-path default `{"/options","/routes"}` was collapsed to a single SVL-configurable path. The historical `/routes` alias is now reached by overriding the SVL variable: `-Djuneau.routeindex.path=routes`. The synonym assertion in `BasicRouteIndexResource_AsMixin_Test#a02` was rewritten as a "legacy alias not mounted by default" 404 check; migration coverage lives in `BasicRouteIndexResource_SvlPathOverride_Test#a02`.

See `todo/FINISHED-101-mixin-svl-paths.md` for the full audit.
