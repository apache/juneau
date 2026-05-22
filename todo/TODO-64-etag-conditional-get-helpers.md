# TODO-64: Conditional-GET / ETag / `If-Modified-Since` helpers on `RestResponse`

Source: split out of TODO-18 brainstorm on 2026-05-22.

## Goal

Add convenience methods on `RestResponse` (plus a small helper on `RestRequest`) for conditional-GET handling: `eTag(String)`, `lastModified(Instant)`, and short-circuit helpers that translate `If-None-Match` / `If-Modified-Since` / `If-Match` / `If-Unmodified-Since` request headers into the appropriate `304 Not Modified` or `412 Precondition Failed` response without the handler having to write the check by hand.

Today there is no `RestResponse.eTag(String)` / `lastModified(Instant)` / `notModifiedIfMatch(...)` builder; users hand-roll. The matching exception types (`NotModified` / `PreconditionFailed`) already live in `org.apache.juneau.http.response`, and the `RequestHeaderList` already exposes `If-Match` / `If-None-Match` / `If-Modified-Since` / `If-Unmodified-Since` — only the response-side ergonomic surface is missing.

End-state developer experience:

```java
@RestGet("/{id}")
public Order get(@Path long id, RestRequest req, RestResponse res) {
    var order = repo.find(id);
    var tag = "\"" + order.version() + "\"";
    res.eTag(tag).lastModified(order.updated());
    req.checkPreconditions().orElseThrow();   // → throws NotModified / PreconditionFailed if appropriate
    return order;
}
```

## Why now

- `juneau-rest-server` was decoupled from `org.apache.http.*` in TODO-40 (`FINISHED-40-remove-hc45-from-rest-common-and-server.md`), and `EntityTag` / `EntityTags` moved out of the `.classic.*` package in TODO-42 (`FINISHED-42-split-rest-common-classic.md`) — so the transport-neutral header types are stable and reachable from `juneau-rest-server` without dragging in `juneau-rest-common-classic`.
- The exception types `NotModified` (304) and `PreconditionFailed` (412) gained the full fluent-setter surface in TODO-40 and are ready to throw.
- The 9.5 hard-break window closed all builder migrations; this is the kind of additive ergonomics polish that fits cleanly post-9.5.

## Scope

**In scope (v1):**

- `RestResponse.eTag(String)` / `RestResponse.eTag(EntityTag)` — sets the `ETag` response header.
- `RestResponse.lastModified(Instant)` / `RestResponse.lastModified(ZonedDateTime)` — sets the `Last-Modified` response header in RFC 7231 IMF-fixdate format.
- `RestResponse.cacheControl(String)` and `RestResponse.cacheControl(CacheControlBuilder)` — convenience for the `Cache-Control` response header (e.g. `public, max-age=3600`).
- `RestRequest.checkPreconditions()` — returns an `Optional<HttpException>`: empty if the response should proceed; a `NotModified` if `If-None-Match` matches the current `ETag` (or `If-Modified-Since` is satisfied); a `PreconditionFailed` if `If-Match` / `If-Unmodified-Since` is violated.
- Tests in `juneau-utest` covering each combination per RFC 7232.

**Explicitly out of scope (v1):**

- A response-cache layer (server-side caching of computed responses). This is just the conditional-GET wire layer.
- Weak vs strong ETag policy: `EntityTag` already models both (`isWeak()`), and the helpers honor whatever the caller produced.
- `Vary` header automation — caller still sets `Vary` explicitly.
- `If-Range` for partial-content (`206 Partial Content`) — defer to a separate range-request TODO.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Verify `RequestHeaderList` exposes the four conditional headers — yes (`juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/httppart/RequestHeaderList.java`).
2. Verify `EntityTag` / `EntityTags` are in `org.apache.juneau.http.header` (transport-neutral, post TODO-42) — yes.
3. Verify `NotModified` (`org.apache.juneau.http.response.NotModified`) and `PreconditionFailed` are ready to throw from a handler.

### Phase 1 — response-side setters

1. Add `eTag(String)`, `eTag(EntityTag)`, `lastModified(Instant)`, `lastModified(ZonedDateTime)`, `cacheControl(String)` to `RestResponse`. Each returns `RestResponse` for chaining.
2. Tests: `RestResponse_EtagHelpers_Test` (header values formatted per RFC 7231).

### Phase 2 — request-side `checkPreconditions()`

1. Add `checkPreconditions()` returning `Optional<HttpException>`. Implementation per RFC 7232 §6 ordering: `If-Match` → `If-Unmodified-Since` → `If-None-Match` → `If-Modified-Since`.
2. The check reads from the response's *already-set* `ETag` / `Last-Modified` headers — so the handler sets those first, then calls `checkPreconditions()`. Document this ordering.
3. Tests: `RestRequest_CheckPreconditions_Test` covering all 16 combinations of the 4 headers, plus the weak/strong ETag matching rules.

### Phase 3 — docs + release notes

1. Release-notes entry under `### juneau-rest-server`.
2. New doc page (or section in an existing page) covering the typical "ETag round-trip" pattern.

## Acceptance criteria

- [ ] `RestResponse.eTag("\"v1\"").lastModified(Instant.parse("2026-05-22T00:00:00Z"))` sets `ETag: "v1"` and `Last-Modified: Fri, 22 May 2026 00:00:00 GMT`.
- [ ] `checkPreconditions()` returns a `NotModified` Optional when the client's `If-None-Match` matches the response's `ETag`.
- [ ] `checkPreconditions()` returns a `PreconditionFailed` Optional when `If-Match` is set and does *not* match the response's `ETag`.
- [ ] Weak vs strong ETag matching follows RFC 7232 §2.3.2 (`If-None-Match` allows weak match; `If-Match` requires strong match).
- [ ] All 16 conditional-header combinations have an explicit test.
- [ ] Coverage ≥ 95% on the new methods. Full `./scripts/test.py` green.

## Open questions

1. **Method placement.** Add the helpers directly on `RestResponse` (recommended — matches the existing `addHeader` / `setHeader` / `downloadAs` convenience style) vs a separate `ConditionalResponse` mix-in. Recommend direct.
2. **`checkPreconditions()` return shape.** `Optional<HttpException>` (recommended) vs `void` that throws directly vs `boolean` + caller throws. Optional gives the caller the choice to throw or to handle inline.
3. **Auto-derived `Last-Modified` from `Instant`.** Should accept `Date` too for legacy bean models, or just `Instant` / `ZonedDateTime`? Recommend `Instant` + `ZonedDateTime`; `Date` users convert explicitly.
4. **`Cache-Control` builder.** Ship a typed `CacheControlBuilder` (recommended — covers `public` / `private` / `max-age` / `no-cache` / `no-store` / `must-revalidate`) or rely on the string form? Builder reduces typo risk.

## Risks

- **Header-format edge cases.** `ETag` quoting (strong: `"v1"`; weak: `W/"v1"`) and `Last-Modified` IMF-fixdate formatting have many wrong-way-around traps. Mitigation: use `EntityTag.of(...)` / `Http*Header` formatting via the existing `juneau-rest-common` paths.
- **Ordering coupling.** `checkPreconditions()` must see the `ETag` / `Last-Modified` the handler intends to send. If the handler sets them after `checkPreconditions()`, the check uses stale values. Mitigation: javadoc clearly documents the order.
- **Servlet container's own conditional handling.** Some containers (notably Jetty) short-circuit conditional GETs themselves. Verify this doesn't double-fire. Mitigation: smoke test against Jetty in `juneau-utest`.

## Related work

- `todo/FINISHED-40-remove-hc45-from-rest-common-and-server.md` — gave `BasicHttpException` / `NotModified` / `PreconditionFailed` the fluent-setter surface.
- `todo/FINISHED-42-split-rest-common-classic.md` — moved `EntityTag` / `EntityTags` to the transport-neutral package.
- `todo/TODO-61-rfc7807-server-side-wiring.md` (sibling) — `PreconditionFailed` thrown from `checkPreconditions()` should flow through the Problem-Details processor cleanly.
