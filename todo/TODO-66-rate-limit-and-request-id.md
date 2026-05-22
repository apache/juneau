# TODO-66: Rate-limit guard + request-id propagation filter

Source: split out of TODO-18 brainstorm on 2026-05-22.

## Goal

Two small, additive primitives that every production REST server eventually needs:

1. **`RateLimitGuard`** — a token-bucket `RestGuard` implementation that throttles requests per a configurable key (IP, principal, header value), returning `429 Too Many Requests` with a `Retry-After` header when the bucket is empty.
2. **`RequestIdFilter`** — a `@RestStartCall`-friendly hook that mints (or propagates) an `X-Request-Id` header, stashes it on `RequestAttributes` so downstream code and the call logger can pick it up, and echoes it back on the response.

Both are small enough to ship in the same TODO; both have zero new external dependencies.

End-state developer experience:

```java
@Rest(path="/api")
public class ApiResource {

    // Request id auto-minted, echoed in response header, available in MDC for logging.
    @Bean RequestIdFilter requestIds() { return RequestIdFilter.create().build(); }

    // Per-IP rate limit, 100 req/min, 200-burst.
    @Bean(name="guards") RestGuardList rateLimits() {
        return RestGuardList.of(RateLimitGuard.create()
            .permitsPerMinute(100)
            .burst(200)
            .keyBy(req -> req.getRemoteAddr())
            .build());
    }
}
```

## Why now

- `RestGuard` SPI is mature (today carries `RoleBasedRestGuard` only — the surface is well-defined).
- `RequestAttributes` (separated from session properties in 9.5 per the 9.5 migration guide) is the right home for the request-id stash; the call-logger rework planned in TODO-20 will read from `RequestAttributes` and surface the id in its `DebugFormat` output.
- `429` and `Retry-After` are already supported via `org.apache.juneau.http.response.TooManyRequests` and the `Retry-After` header in `juneau-rest-common`.
- Zero new deps, zero behavioral risk if not registered.

## Scope

**In scope (v1):**

- `org.apache.juneau.rest.guard.RateLimitGuard` extending `RestGuard`. Builder-pattern config: `permitsPerSecond` / `permitsPerMinute` / `permitsPerHour`, `burst`, `keyBy(Function<RestRequest,String>)`, `whenLimitExceeded(BiConsumer<RestRequest, RateLimitInfo>)` callback for logging hooks.
- In-memory token bucket per key, evicting idle keys after a configurable TTL (default 1 hour). `ConcurrentHashMap<String, Bucket>` with size cap (default 100k entries) — exceeding the cap triggers LRU eviction.
- `org.apache.juneau.rest.filter.RequestIdFilter` — minted via `UUID.randomUUID()` (configurable; can swap in a `Supplier<String>`). Honors an incoming `X-Request-Id` if present and well-formed. Echoes on response as `X-Request-Id`. Stashes on `RequestAttributes` under key `requestId`.
- `RequestId` request-attribute key constant in `RestServerConstants` so call-logger / observability layers have a single source of truth.
- Tests in `juneau-utest`: rate-limit happy path, burst, key isolation, eviction, `Retry-After` header value; request-id mint, propagation, response echo.

**Explicitly out of scope (v1):**

- Distributed rate-limiting (Redis-backed). The SPI is split into an interface + in-memory impl so a `RedisRateLimitBackend` can be a sub-module later.
- Sliding-window rate-limit algorithms. Token bucket is enough.
- `CSRF` protection — separate concern, separate TODO if requested.
- API-key authentication — that's TODO-69 (`AuthN guards`).
- Custom rate-limit headers (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`). Emit them; document that they're advisory and unstandardized. Worth shipping in v1 — adds ~10 LOC.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. `RestGuard.guard(RestRequest, RestResponse)` signature — confirmed as the right hook for rate-limit (runs before the handler).
2. `RequestAttributes` write access in `@RestStartCall` — confirmed.
3. `TooManyRequests` constructor accepts a body and supports `setHeader(...)` for `Retry-After` — confirmed via TODO-40's exception-surface uplift.

### Phase 1 — `RateLimitGuard`

1. Add the class, builder, `Bucket` internal type, eviction policy.
2. Tests:
   - `RateLimitGuard_Test` — burst tokens drain, refill at the configured rate, `429 + Retry-After` thrown when empty.
   - `RateLimitGuard_KeyIsolation_Test` — different keys have independent buckets.
   - `RateLimitGuard_Eviction_Test` — buckets idle > TTL get evicted.

### Phase 2 — `RequestIdFilter`

1. Add the class + `RequestId` constant.
2. Tests:
   - `RequestIdFilter_Test` — mint when absent; honor when present + well-formed; reject and re-mint when present but malformed.
   - `RequestIdFilter_Echo_Test` — response `X-Request-Id` matches the request attribute.

### Phase 3 — docs + release notes

1. Release-notes entry under `### juneau-rest-server` for both.
2. New doc page or section: "Rate-limiting and request-id propagation."

## Acceptance criteria

- [ ] `RateLimitGuard` with `permitsPerMinute(100).burst(200).keyBy(req -> req.getRemoteAddr())` allows 200 immediate requests, then throttles to 100/min per IP.
- [ ] `429 Too Many Requests` is thrown when the bucket is empty, with a `Retry-After` header set to the seconds-until-next-token.
- [ ] `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` response headers are populated on every response that passes through the guard.
- [ ] `RequestIdFilter` mints a UUID when no `X-Request-Id` is present; echoes it on the response; stashes it on `RequestAttributes` under key `requestId`.
- [ ] Incoming `X-Request-Id` that fails validation (configurable predicate; default: matches `^[A-Za-z0-9-_]{1,128}$`) is replaced with a freshly-minted one.
- [ ] Coverage ≥ 95% on both classes. Full `./scripts/test.py` green.

## Open questions

1. **Default key.** `req.getRemoteAddr()` (IP-based) is the obvious default. Alternative: header-based (`X-Forwarded-For` aware). Recommend IP-based with `XForwardedFor`-aware as a builder flag.
2. **Storage SPI.** Ship `RateLimitGuard.Storage` interface in v1 (recommended — keeps Redis-backed impl as a sub-module later) or land monolithically and refactor later? Interface is small (`tryAcquire(key, permits)` + `eviction`), recommend ship now.
3. **Validation predicate for `X-Request-Id`.** Default `^[A-Za-z0-9-_]{1,128}$` (recommended — UUIDs + most distributed-tracing ids). Configurable.
4. **`Retry-After` value when bucket is empty.** Seconds until one token is available (recommended) vs HTTP-date. Seconds is simpler and more common.
5. **Probe-path exemption.** Should `/healthz`/`/readyz`/`/livez` (TODO-65) be exempted from rate-limits by default? Recommend yes if both TODOs land — add a `.exemptPaths(String...)` builder method that ships with sensible defaults.

## Risks

- **Memory growth under attack.** A spammer with rotating IPs can fill the bucket map. Mitigation: size cap + LRU eviction (already in scope).
- **Clock-source dependency.** Token-bucket math uses `System.nanoTime()`; safe vs wall-clock jumps but harder to debug. Document.
- **Distributed-deploy footgun.** Per-pod buckets ≠ per-cluster buckets. Users running N replicas need a distributed backend. Document loudly; flag the `Storage` SPI as the extension point.
- **`X-Forwarded-For` spoofing.** If used without a trusted proxy, attackers can defeat the IP-based key. Document; require explicit opt-in.

## Related work

- `todo/FINISHED-40-remove-hc45-from-rest-common-and-server.md` — `TooManyRequests` exception type gained the fluent-setter surface this needs.
- `todo/TODO-20-rest-debug-rethink.md` — call-logger should pick up the request-id from `RequestAttributes` and render it in `DebugFormat` output.
- `todo/TODO-65-health-readiness-liveness-probes.md` (sibling) — probe paths should be exempt from rate-limit by default.
- `todo/TODO-67-observability-micrometer-otel.md` (sibling) — request-id should propagate into OTel span attributes and Micrometer tags.
- `todo/TODO-69-authn-guards-jwt-apikey.md` (sibling) — different concern (authentication), often paired with rate-limit in the guard chain.
