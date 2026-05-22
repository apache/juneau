# FINISHED-18: juneau-rest-server feature brainstorm

Archived from the `[TODO-18]` bullet on 2026-05-22 after the brainstorm materialized into ten follow-on TODOs.

## Outcome

The original bullet — *"Investigate possible useful features to add to juneau-rest-server"* — was an investigation request, not an implementation. The investigation has now produced ten concrete follow-on TODOs (`TODO-61` through `TODO-70`), each with its own self-contained plan file under `todo/`. The brainstorm itself produced no code changes.

## Ten follow-on TODOs

Priority-ordered (recommended landing order is roughly numeric; each plan is independent):

| ID | Slug | One-liner |
|----|------|-----------|
| TODO-61 | `rfc7807-server-side-wiring` | Auto-emit `application/problem+json` from uncaught `BasicHttpException`; `Problem` return-value support. |
| TODO-62 | `sse-server-helpers` | `RestResponse.sse()` fluent surface + `SseBroadcaster` fan-out + heartbeat scheduler. |
| TODO-63 | `openapi-3.1-emission` | New `OpenApiProvider` (sibling of `SwaggerProvider`) + bundled Swagger UI auto-mount. |
| TODO-64 | `etag-conditional-get-helpers` | `RestResponse.eTag(...)` / `lastModified(...)` + `RestRequest.checkPreconditions()`. |
| TODO-65 | `health-readiness-liveness-probes` | `/healthz` / `/readyz` / `/livez` + `HealthIndicator` SPI. |
| TODO-66 | `rate-limit-and-request-id` | Token-bucket `RateLimitGuard` + `RequestIdFilter`. |
| TODO-67 | `observability-micrometer-otel` | `MetricsRecorder` + `TracerHook` SPIs, with opt-in Micrometer + OTel sub-modules. |
| TODO-68 | `bean-validation-integration` | Honor `jakarta.validation` constraints on `@Content` / `@FormData` / `@Request` bound beans. |
| TODO-69 | `authn-guards-jwt-apikey` | `BearerTokenGuard`, `ApiKeyGuard`, optional `juneau-rest-server-jwt` sub-module. |
| TODO-70 | `async-completablefuture-virtual-threads` | `AsyncResponseProcessor` for `CompletableFuture` returns + opt-in virtual-thread dispatch. |

The bullets are listed in `todo/TODO.md`; the per-id plan files are at `todo/TODO-<id>-<slug>.md`.

## Brainstorm methodology

1. **Baseline read.** Reviewed `todo/TODO.md`, the three active TODOs (`TODO-20-rest-debug-rethink.md`, `TODO-35-beanstore-test-injection.md`, `TODO-37-agent-instructions-consolidation.md`), and the recent rest-server-touching `FINISHED-*` archives (31, 33, 36, 38, 40, 41, 42, 45, 46, 47) to ground the candidate list in what just landed and avoid duplicating shipped work.
2. **Surveyed `juneau-rest-server`.** Inventoried package structure (`annotation/`, `arg/`, `converter/`, `debug/`, `guard/`, `httppart/`, `logger/`, `matcher/`, `processor/`, `staticfile/`, `stats/`, `swagger/`), spot-checked `juneau-rest-server-springboot` and `juneau-rest-mock` for integration patterns, and grepped for missing seams (async returns, ETag helpers, observability hooks, auth providers, multipart, OpenAPI 3, Problem-details wiring).
3. **Cross-checked against the 9.5 migration guide** (`juneau-docs/pages/topics/23.01.V9.5-migration-guide.md`) and `juneau-docs/pages/release-notes/9.5.0.md` to make sure no candidate duplicated already-shipped work in 9.5.
4. **Cast a wide net** — generated 14 candidate features across observability, security, content-negotiation, OpenAPI/docs, reactive/async, validation, testing, lifecycle/DI, error handling, caching/perf, versioning. Narrowed to the 10 most concrete (dropped: brotli/zstd encoder, JSON Patch wiring, multipart parser improvements, API versioning helpers — each too small or too speculative to merit a separate TODO).
5. **Ranked by impact × feasibility.** The shortlisted top three (TODO-61, TODO-62, TODO-63) became the recommended landing order; the remaining seven (TODO-64 through TODO-70) were ordered by feasibility (S-sized first, then M, then M/L).

## Notes for downstream implementers

- Several of these TODOs *compose*. TODO-61 (Problem-Details) is the natural error-rendering target for TODO-68 (Bean Validation) and TODO-69 (AuthN). TODO-66 (request-id) feeds TODO-67 (observability) and TODO-20 (debug-format). TODO-70 (async) and TODO-62 (SSE) both benefit from virtual-thread dispatch. The per-TODO "Related work" sections call these cross-links out explicitly.
- All ten are post-9.5 — the hard-break window is closed, so every plan is structured as **additive-only**. Any deprecations live in 9.6+.
- No TODO depends on TODO-35 (beanstore test injection) shipping first, but several would have nicer test ergonomics with it in place. Implementer's choice on landing order.
