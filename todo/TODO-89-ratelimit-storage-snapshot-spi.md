# TODO-89: `RateLimitGuard.Storage` snapshot SPI + admin endpoint enrichment

Source: surfaced during FINISHED-77's `BasicAdminResource` work; TODO.md headline bullet expanded 2026-05-24.

## Goal

Add a read-side / snapshot operation to the existing `RateLimitGuard.Storage` SPI (FINISHED-66) so that `BasicAdminResource#getRateLimit(...)` (FINISHED-77) can emit **live bucket state** alongside the static configuration it already emits. Operators today can see "what the rate-limit policy is" but not "which buckets are currently throttled and at what fill level"; this TODO closes that gap with a small backwards-compatible additive SPI extension.

End-state operator experience:

```bash
$ curl -H 'Authorization: Bearer …' https://api.example.com/admin/ratelimit
{
  "guards": {
    "rateLimit": {
      "config": {
        "class": "org.apache.juneau.rest.guard.RateLimitGuard",
        "limit": 100,
        "permitsPerSecond": 1.67,
        "xForwardedForAware": false,
        "exemptPaths": ["/healthz","/readyz","/livez"]
      },
      "snapshot": [
        {"key":"203.0.113.42","tokens":23.0,"remaining":23,"throttled":false,"lastRequest":"2026-05-24T14:00:42Z"},
        {"key":"203.0.113.99","tokens":0.5,"remaining":0,"throttled":true,"lastRequest":"2026-05-24T14:00:58Z"}
      ]
    }
  }
}
```

## Why now

- `BasicAdminResource#getRateLimit(...)` already exists and already does multi-bean lookup (FINISHED-77 line 333 — `collectRateLimitGuards(bs)`). It emits `{ "config": {...}, "buckets": [] }` today; the empty `buckets` array is a TODO marker that this work fills in. See FINISHED-77 line 300: `bucket.put("buckets", List.of());`.
- `RateLimitGuard.Storage` is already an SPI with a non-trivial in-memory implementation (`InMemoryStorage`, `juneau-rest-server/.../guard/RateLimitGuard.java` lines 450-486) backed by a `ConcurrentHashMap<String,Bucket>`. The data is already there; the SPI just doesn't expose it.
- Default-impl backwards-compatibility (return empty `Map`) means external storages (Redis-backed, DynamoDB-backed, etc.) that don't update keep compiling; the change is purely additive.
- Closes a known carry-over from FINISHED-77 (called out explicitly in its line 274: "Bucket-level rate-limit inspection on `/admin/ratelimit` — blocked on `RateLimitGuard.Storage` exposing a snapshot SPI").
- TODO-69's `BasicAdminResource_AuthIntegration_Test` is already in flight (visible in `juneau-utest/.../ops/`). The snapshot-enriched response shape works inside the same auth chain TODO-69 builds — no cross-dependency on the AuthN work.

## Research findings (verified 2026-05-24)

Significant facts from reading the current `RateLimitGuard` source that shape the SPI design:

1. **`Storage` is `public interface` already** (line 391). Has two methods: `AcquireResult tryAcquire(String key, int capacity, double permitsPerSecond)` and `void evict(Duration ttl)`, plus static factories `inMemory()` / `inMemory(int maxKeys)`. Adding a `default Map<String,BucketState> snapshot()` is additive and binary-compatible.
2. **`InMemoryStorage` is package-private final** (line 450). Already has a package-private `size()` helper (line 476). Exposing it as the default `Storage` impl requires only adding a `snapshot()` override — no class-visibility change.
3. **`Bucket` is package-private** (line 496) with the minimal token-bucket state: `double tokens`, `long lastNanos` (monotonic, NOT a wall-clock Instant), plus `synchronized tryAcquire(...)` and `synchronized lastTouchedNanos()`. The plan needs accessors for the snapshot path; recommend adding a package-private `synchronized double tokens()` for read-only access, leaving the existing `lastTouchedNanos()` alone.
4. **`RateLimitGuard` has NO public `getStorage()` accessor.** The `storage` field at line 117 is `private final`. `BasicAdminResource#getRateLimit(...)` therefore cannot reach the storage today. Plan needs to add either a public `Storage getStorage()` accessor on `RateLimitGuard`, or a convenience `Map<String,BucketState> snapshot()` on `RateLimitGuard` that delegates to `storage.snapshot()`. Recommendation: ship both — `getStorage()` for general SPI access, `snapshot()` as the ergonomic shortcut `BasicAdminResource` calls.
5. **`Bucket.lastNanos` is `System.nanoTime()`-based** (monotonic, not wall-clock). Mapping it to `java.time.Instant` for the snapshot requires capturing the wall-clock offset at first-touch. Two options: (a) add a second `long lastWallMillis` field to `Bucket` — cheap, accurate, increases memory by 8 bytes per bucket; (b) compute `Instant.now().minusNanos(System.nanoTime() - lastNanos)` at snapshot-time — no memory cost, but slight skew if the JVM has been suspended (laptops, paused containers). **Recommendation: option (a)** — 8 bytes × 100k buckets = 800KB worst case, an acceptable cost for accurate operator-facing timestamps.
6. **Token-bucket vs counter-based vocabulary.** The user-query's suggested `BucketState` field set (`count`, `windowStart`, `remaining`, `lastRequest`) is counter-based vocabulary. The real implementation is a continuous-refill token bucket: there's no discrete "window start" (refills are continuous via `tokens + elapsedSeconds * permitsPerSecond`), and "count" is best read as `capacity - floor(tokens)` (tokens spent since last refill). Plan adopts token-bucket vocabulary for `BucketState` to match the runtime reality; see "Resolved decisions" for the final field set.
7. **`BasicAdminResource#getRateLimit(...)` response shape.** Today (FINISHED-77 line 296-305): `{ "guards": { "<bean-name>": { "config": {...}, "buckets": [] } } }`. The empty `buckets` array is the slot to fill. Plan renames the field to `snapshot` (matches the SPI method name) and emits a sorted array of `BucketState` records. The existing `config` field stays unchanged — additive change.

## Scope

**In scope (v1):**

- **SPI extension** — new `default Map<String,BucketState> snapshot()` method on `RateLimitGuard.Storage`. Default returns `Map.of()` (immutable empty).
- **`BucketState` value type** — new public nested record on `RateLimitGuard` capturing per-bucket runtime state. See "Resolved decisions" for the field set.
- **`InMemoryStorage` override** — implements `snapshot()` by iterating the internal `buckets` map and emitting one `BucketState` per entry. Returns an immutable copy (`Map.copyOf(...)`).
- **`Bucket` accessor** — package-private `synchronized double tokens()` helper for snapshot read-access; package-private `synchronized long lastWallMillis()` paired with a new `long lastWallMillis` field updated alongside `lastNanos` in `tryAcquire(...)`.
- **`RateLimitGuard` accessors** — public `Storage getStorage()` and public `Map<String,BucketState> snapshot()` (latter delegates to `storage.snapshot()`).
- **`BasicAdminResource#getRateLimit(...)` enrichment** — replace `bucket.put("buckets", List.of())` with `bucket.put("snapshot", g.snapshot().values().stream().sorted(...).toList())`. Sort by `key` ascending for stable operator-tooling output. Also enrich `describeRateLimitGuard(...)` (currently emits only `class`) with `limit`, `permitsPerSecond`, `xForwardedForAware`, `exemptPaths` — these are accessible via new package-private / public getters on `RateLimitGuard` (TBD how invasive; see Phase 2.4 below).
- **Tests** in `juneau-utest`:
    - `RateLimitGuard_Snapshot_Test` — `Storage.inMemory()` returns a non-empty map after a few requests; values reflect refill state.
    - `RateLimitGuard_SnapshotExternal_Test` — a custom `Storage` impl that doesn't override `snapshot()` returns `Map.of()` (default behavior); does not throw.
    - `BasicAdminResource_RateLimitSnapshot_Test` — in `juneau-utest/src/test/java/org/apache/juneau/rest/ops/` (sibling to existing admin tests). Wires `BasicRateLimitGuard` to an `@Rest` endpoint; calls it several times from different `X-Forwarded-For` keys; asserts `/admin/ratelimit` returns the snapshot with the expected bucket keys + throttled flags.

**Explicitly out of scope (v1):**

- **Streaming `Iterator<BucketState> snapshotStream()` variant.** For Redis-backed / DynamoDB-backed storages with potentially millions of buckets, a `Map` return shape risks OOM. Out of scope for v1 — see "Open questions" for the v2 follow-up rationale.
- **Bucket reset / mutation operations.** Read-only snapshot only; mutating bucket state from `/admin/ratelimit` is a footgun (FINISHED-77 line 71 already scoped this out for the same reason).
- **Snapshot-time filtering** (e.g. `?throttledOnly=true`). Operator can grep client-side; YAGNI at the SPI level.
- **Historical bucket history** (`Iterable<BucketState> history(String key)`). Token-bucket state is point-in-time only; per-key history would be a different data structure (time-series).
- **Cross-guard snapshot aggregation.** Multiple `RateLimitGuard` beans (per-tier free/paid/etc.) are already keyed by bean name in the response (FINISHED-77 line 333 `collectRateLimitGuards(bs)`); snapshot is per-guard, aggregation is the operator's job.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm `RateLimitGuard.Storage` is `public interface` in 9.5.0 release — yes, at `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/guard/RateLimitGuard.java` line 391.
2. Confirm Java 17 floor (record support) — yes, `pom.xml` line 39 `maven.compiler.release>17`.
3. Confirm `InMemoryStorage` package-private visibility — yes, line 450. No visibility change required.
4. Confirm `BasicAdminResource#getRateLimit(...)` is the only consumer of the existing rate-limit data — yes, single grep hit in `juneau-rest-server-rdf` / springboot has no rate-limit-specific code.
5. Confirm coverage tooling (`scripts/coverage.py`) can target `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/guard/` for the post-change coverage check.

### Phase 1 — SPI extension

1. Add `default Map<String,BucketState> snapshot()` to `RateLimitGuard.Storage` returning `Map.of()`. Javadoc: "Optional snapshot operation for operational visibility. Default impl returns an empty map (backwards-compatible — external storages that can't cheaply enumerate buckets opt out by leaving the default). In-memory storage impls SHOULD override to expose live bucket state."
2. Add `public record BucketState(String key, double tokens, int remaining, boolean throttled, Instant lastRequest)` on `RateLimitGuard`. Javadoc each component. Place it near the existing `RateLimitInfo` record (line 379) for cohesion.
3. Add a unit test asserting an interface anonymous impl that doesn't override `snapshot()` returns `Map.of()` and doesn't throw.

### Phase 2 — `InMemoryStorage` override

1. Add `long lastWallMillis` field to `Bucket` alongside `lastNanos`; update it in the synchronized `tryAcquire(...)` block at line 506.
2. Add package-private `synchronized double tokens()` and `synchronized long lastWallMillis()` accessors on `Bucket`.
3. Override `snapshot()` in `InMemoryStorage`:
    ```java
    @Override public Map<String,BucketState> snapshot() {
        var out = new LinkedHashMap<String,BucketState>();
        for (var e : buckets.entrySet()) {
            var b = e.getValue();
            var tokens = b.tokens();
            out.put(e.getKey(), new BucketState(
                e.getKey(),
                tokens,
                (int) Math.floor(tokens),
                tokens < 1.0,
                Instant.ofEpochMilli(b.lastWallMillis())
            ));
        }
        return Map.copyOf(out);
    }
    ```
4. Add `public Storage getStorage()` and `public Map<String,BucketState> snapshot()` accessors on `RateLimitGuard`. Plus public getters for the configuration that `BasicAdminResource` will emit: `int getCapacity()`, `double getPermitsPerSecond()`, `boolean isXForwardedForAware()`, `Set<String> getExemptPaths()`. (The fields exist as `private final` at lines 111-117; this is straight expose-existing-state, no behavior change.)
5. Tests:
    - `RateLimitGuard_Snapshot_Test#a01` — after 3 requests against `Storage.inMemory()`, `guard.snapshot()` returns a 1-entry map; `BucketState.tokens` is in `(capacity-3, capacity-3+epsilon)`; `BucketState.remaining` matches `floor(tokens)`; `throttled` is false.
    - `RateLimitGuard_Snapshot_Test#a02` — after burst exhaustion, `BucketState.tokens < 1.0`; `BucketState.remaining == 0`; `BucketState.throttled == true`.
    - `RateLimitGuard_Snapshot_Test#a03` — multi-key snapshot: 3 keys hit the guard, snapshot has 3 entries, each with the expected key.
    - `RateLimitGuard_Snapshot_Test#a04` — anonymous `Storage` impl that doesn't override `snapshot()` returns `Map.of()`.

### Phase 3 — `BasicAdminResource` enrichment

1. Replace `bucket.put("buckets", List.of())` (FINISHED-77 line 300) with `bucket.put("snapshot", e.getValue().snapshot().values().stream().sorted(Comparator.comparing(BucketState::key)).toList())`.
2. Enrich `describeRateLimitGuard(g)` (FINISHED-77 line 345-349) — currently emits only `{"class": g.getClass().getName()}`. New shape: `{"class":..., "limit":g.getCapacity(), "permitsPerSecond":g.getPermitsPerSecond(), "xForwardedForAware":g.isXForwardedForAware(), "exemptPaths":g.getExemptPaths()}`.
3. Tests:
    - `BasicAdminResource_RateLimitSnapshot_Test#a01` — single `BasicRateLimitGuard` bean registered; hit `/items` 3 times from one key, then GET `/admin/ratelimit`. Assert response contains `guards.rateLimit.snapshot` with 1 entry, the right key, `remaining == capacity - 3`, `throttled == false`.
    - `BasicAdminResource_RateLimitSnapshot_Test#a02` — exhaust the bucket; assert `snapshot[0].throttled == true`, `snapshot[0].remaining == 0`.
    - `BasicAdminResource_RateLimitSnapshot_Test#a03` — multi-key (3 distinct `X-Forwarded-For` values), `xForwardedForAware(true)`, assert snapshot has 3 entries sorted ascending by key.
    - `BasicAdminResource_RateLimitSnapshot_Test#b01` — multi-`RateLimitGuard` bean topology (per-tier free/paid); assert each entry in `guards.<bean-name>.snapshot` is populated independently.
    - `BasicAdminResource_RateLimitSnapshot_Test#c01` — custom `Storage` that doesn't override `snapshot()`: response has `guards.<name>.snapshot == []`, no exception.

### Phase 4 — docs + release notes

1. Update `juneau-docs/pages/topics/10.14c.OpsIntrospectionMixins.md` `/admin/ratelimit` sub-section to show the new response shape (`snapshot` array alongside `config` block).
2. Update `juneau-docs/pages/topics/RestServerRateLimitAndRequestId` (the rate-limit topic page) — add a new "Inspecting bucket state via `/admin/ratelimit`" sub-section pointing at `Storage.snapshot()` and `RateLimitGuard.snapshot()`.
3. Release-notes entry under `### juneau-rest-server` in the current version's release-notes file: "`RateLimitGuard.Storage` now has a `snapshot()` SPI (default empty; in-memory storage overrides). `BasicAdminResource`'s `/admin/ratelimit` endpoint emits live bucket state via `guards.<name>.snapshot[]`."

## Acceptance criteria

- [ ] `RateLimitGuard.Storage.snapshot()` exists as a `default` method returning `Map.of()`.
- [ ] `RateLimitGuard.BucketState` record exists with the field set in resolved decision #1.
- [ ] Default `InMemoryStorage` overrides `snapshot()` to enumerate live bucket state.
- [ ] `RateLimitGuard` exposes `getStorage()`, `snapshot()`, `getCapacity()`, `getPermitsPerSecond()`, `isXForwardedForAware()`, `getExemptPaths()` as public accessors.
- [ ] `BasicAdminResource#getRateLimit(...)` emits `guards.<bean-name>.snapshot[]` populated from `guard.snapshot()`; sorted by `key` ascending.
- [ ] `BasicAdminResource#getRateLimit(...)` emits enriched `config` with `limit`, `permitsPerSecond`, `xForwardedForAware`, `exemptPaths`.
- [ ] Custom `Storage` impl that doesn't override `snapshot()` still works end-to-end; admin endpoint emits empty `snapshot[]` for that bean.
- [ ] All five existing `RateLimitGuard_*_Test` files continue to pass.
- [ ] New `RateLimitGuard_Snapshot_Test` + new `BasicAdminResource_RateLimitSnapshot_Test` pass.
- [ ] Coverage ≥ 95% on `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/guard/` post-change.
- [ ] Full `./scripts/test.py` green.

## Resolved decisions

All previously open questions resolved 2026-05-24.

1. **`BucketState` field set.** `record BucketState(String key, double tokens, int remaining, boolean throttled, Instant lastRequest)`. Rationale:
    - **`tokens` (double)** instead of the user-query's suggested `count` — matches the token-bucket runtime vocabulary; "count" is counter-based vocabulary that doesn't fit the continuous-refill model.
    - **`remaining` (int)** mirrors the existing `X-RateLimit-Remaining` advisory header (line 103); operators expect this name.
    - **`throttled` (boolean)** — convenience for operator tooling. Yes, it's redundant with `remaining == 0`, but explicit is better than computed for an at-a-glance dashboard. Per the user-query's open question, ergonomics wins.
    - **`lastRequest` (Instant)** — wall-clock-accurate (via `Bucket.lastWallMillis`, per research finding #5). Mapped from `Bucket.lastNanos` would be slightly drift-prone under JVM suspension; the 8 bytes/bucket cost is worth it.
    - **No `windowStart`** — token-bucket doesn't have a discrete window; reporting one would be misleading. Operators get `tokens` (the fill level) and `lastRequest` (the last activity), which together describe the bucket completely.
    - **No `key` duplication?** Yes, `key` is the field AND the map key in `Storage.snapshot()`. This is intentional: `BasicAdminResource` emits a sorted *list* (not a map) in JSON, and the list entries need to be self-describing.
2. **`Map<String,BucketState>` return shape, not `Iterator`.** Map is simpler; the admin endpoint is operator-only behind a guard; operators with millions-of-buckets stores can subclass + paginate themselves. `Iterator<BucketState> snapshotStream()` is a v2 follow-up if a real Redis-backed user reports OOM. Documented as such in the `Storage.snapshot()` javadoc.
3. **`BucketState` is a Java `record`** (Java 17+, per `pom.xml` `maven.compiler.release` = 17). Matches the existing `RateLimitInfo` record (line 379) and `AcquireResult` record (line 440).
4. **Default returns empty `Map.of()` (immutable).** Backwards-compatible; external storage impls that don't update keep compiling; admin endpoint just emits `snapshot[]: []` for them. NOT a breaking change.
5. **Admin endpoint response shape — additive change.** The existing `config` field stays at top of each guard entry; the empty-placeholder `"buckets": []` field is REPLACED with `"snapshot": [...]` (renamed to match the SPI method name). The rename is acceptable because the existing field is documented as a placeholder (FINISHED-77 line 100-101) and emits `[]` today — no real consumer expects "buckets". Capture this rename in the release notes.
6. **`Bucket.lastWallMillis` paired with existing `lastNanos`.** 8 bytes/bucket × 100k buckets = 800KB worst case; trivial. Accuracy trumps the memory micro-optimization.
7. **Single-PR landing.** Small change set: ~30 lines on `RateLimitGuard.java`, ~10 lines on `BasicAdminResource.java`, 2 new test files. Phases 1-4 land in one PR. No need to split.
8. **Public `getStorage()` AND public `snapshot()` convenience.** `getStorage()` is the general SPI access point (lets users implement custom dashboards); `snapshot()` is the ergonomic shortcut `BasicAdminResource` uses internally. Both are cheap to provide.

## Open questions

1. **`Iterator<BucketState> snapshotStream()` for huge-cardinality storages.** Recommend NO for v1 (see resolved decision #2). Flag as a v2 follow-up TODO if/when a Redis-backed user reports OOM. The decision is reversible — adding a second default method later is also backwards-compatible.
2. **Should `BucketState` expose `secondsUntilReset`?** The existing `Bucket.secondsUntilFull(...)` math at line 522 could populate this field, matching the `X-RateLimit-Reset` advisory header. Recommend NO for v1 — operators can compute it from `(capacity - tokens) / permitsPerSecond` if needed; adding a field is non-breaking later. Defer.
3. **Bean-store-keyed beanName as a `BucketState` field too?** No — the bean name is already the JSON map key in `guards.<bean-name>` (FINISHED-77 line 304), so duplicating it inside each `BucketState` would be noise.

## Risks

- **Cardinality blow-up under attack.** An attacker probing with millions of distinct API keys or distinct `X-Forwarded-For` values fills the `InMemoryStorage` bucket map. `snapshot()` then becomes expensive (single-allocation `LinkedHashMap` of all entries) and produces a multi-megabyte JSON response from `/admin/ratelimit`. Mitigations:
    - `InMemoryStorage`'s existing 100k-entry LRU cap (line 420 `inMemory()` factory; line 480-485 `evictOldest()`) bounds the worst case at ~100k entries × ~100 bytes/entry JSON = ~10MB; large but not catastrophic.
    - `/admin/ratelimit` is guard-protected by `DenyAllGuard` (FINISHED-77 line 121); only authenticated operators can hit it. A malicious operator querying during an attack could still allocate the response, but the threat surface is operator-bounded, not internet-bounded.
    - Document the operator's responsibility to set sensible burst budgets and to avoid hitting `/admin/ratelimit` during active mitigation. Capture in the Risks section of the topic page.
- **`snapshot()` thread-safety vs. `tryAcquire(...)`.** Each `Bucket.tryAcquire(...)` is synchronized on the bucket (line 506); `snapshot()` reads `b.tokens()` and `b.lastWallMillis()` via the package-private synchronized accessors, so individual reads are consistent. The overall snapshot is NOT a global point-in-time consistent view across buckets (the snapshot iterates the `ConcurrentHashMap` entries while concurrent writes may be modifying other buckets) — acceptable for operator visibility, document explicitly.
- **`Bucket.lastWallMillis` cost.** 8 bytes/bucket on a hot allocation path. Microbenchmark Phase 2 with the existing `RateLimitGuard_Test` (currently passes; verify no perf regression with a quick JMH if anyone has the appetite — out of scope for v1).
- **Field-name churn on the response shape.** Renaming `buckets` → `snapshot` in the `/admin/ratelimit` JSON is a tiny consumer-facing break for anything that was already parsing the placeholder. FINISHED-77 left it as `[]` so realistically no one is parsing it, but capture the rename in release notes prominently.
- **Custom Storage authors and the new `getCapacity()` / `getPermitsPerSecond()` accessors.** These are read-only getters on `RateLimitGuard` — no SPI impact. Documented as such.

## Related work

- `todo/FINISHED-66-rate-limit-and-request-id.md` — the rate-limit landing this builds on. Defines the `RateLimitGuard` + `Storage` SPI shape. Carries the original "Storage SPI" open question (line 99) that this TODO answers concretely.
- `todo/FINISHED-77-mixin-ops-introspection.md` — `BasicAdminResource`'s `/admin/ratelimit` endpoint. Lines 271-306 (existing endpoint code) + line 274 (the deferred-snapshot carry-over note) are the consumer-side hook this TODO completes.
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/guard/RateLimitGuard.java` — primary file modified. Lines 391-441 (SPI + factory), lines 450-486 (`InMemoryStorage`), lines 496-526 (`Bucket`).
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/ops/BasicAdminResource.java` — secondary file modified. Lines 285-306 (endpoint) + lines 333-349 (helper methods).
- `juneau-utest/src/test/java/org/apache/juneau/rest/guard/` — existing `RateLimitGuard_*_Test` test siblings to mirror for the new `RateLimitGuard_Snapshot_Test`.
- `juneau-utest/src/test/java/org/apache/juneau/rest/ops/` — existing `BasicAdminResource_*_Test` test siblings (including the in-flight `BasicAdminResource_AuthIntegration_Test` from TODO-69) for the new `BasicAdminResource_RateLimitSnapshot_Test`.
- `juneau-docs/pages/topics/10.14c.OpsIntrospectionMixins.md` — `/admin/ratelimit` doc to update with the new response shape.
- TODO.md line 56 — the headline bullet this plan expands.
