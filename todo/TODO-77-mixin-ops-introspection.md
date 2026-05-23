# TODO-77: Ops/introspection mixin pack (echo, admin, route-index)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23.

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
- **`BasicEchoResource`** — `@RestOp` (any method) at `/{path:.*}` with default `paths={"/echo","/debug/echo"}`. Returns JSON: `{ method, path, headers, queryParams, attributes, contentLength, content (optional, capped) }`. Gated behind `getContext().getDebugEnablement().isDebug(req)` — returns 404 when debug is off. Configurable `bodyLimit` (default 1MB).
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
- [ ] Path overrides via TODO-73 (`paths` setter / `pathsKey`) reroute each mixin's mount cleanly.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test (per mixin).
- [ ] Coverage ≥ 95% per mixin. Full `./scripts/test.py` green.

## Open questions

1. **`/admin/cache/flush` mutation method — POST vs DELETE?** **Recommend POST** with optional body for selective flush — `DELETE` body semantics are inconsistent across HTTP clients, and POST aligns with "this is a non-idempotent action" intent. Document.
2. **Echo body limit default.** **Recommend 1MB cap, configurable via `bodyLimit(...)`** — large enough for realistic debug, small enough to avoid memory pressure under sustained probing.
3. **Route-index excludes filter beans (`@RestStartCall` / `@RestEndCall`)?** **Recommend yes — only `@RestOp` methods** — filters are infrastructure, not part of the public route surface. Document.
4. **Echo header redaction default list.** **Recommend `Authorization`, `Cookie`, `Set-Cookie`, `Proxy-Authorization`, `X-API-Key`** — covers the common token-bearing headers. Configurable.
5. **Default-deny role-guard placeholder.** **Recommend `roleGuard="ROLE_ADMIN_NONE_DEFAULT"` (a deliberately-non-existent role)** — forces the user to override with a real guard or get 403s. Naming is verbose but unambiguous.
6. **`/admin/threads` filter default.** **Recommend exclude `java.*`, `javax.*`, `jakarta.*`, `org.eclipse.jetty.*`, `org.springframework.*`** — application threads only by default. Configurable.
7. **`/admin/ratelimit` returns multi-`RateLimitGuard` breakdown — JSON shape?** **Recommend `{ "guards": { "<bean-name>": { "buckets": [...], "config": {...} } } }`** — keyed by bean name from `getBeansOfType(...)`. Stable for tooling.
8. **Should `BasicRouteIndexResource` overlap with `BasicGroupOperations.getChildren(...)` (the navigation page) or stay JSON-only?** **Recommend JSON-only** — the navigation page is HTML-doc-config-driven for humans; the route-index mixin is for tooling. Different consumers.

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
