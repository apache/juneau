# TODO-62: Server-side SSE helpers (broadcaster, per-event flush, heartbeat)

Source: split out of TODO-18 brainstorm on 2026-05-22 (the #2 pick).

## Goal

Build the server-side ergonomic layer on top of the SSE marshaller landed in 9.5.0 (`FINISHED-46-juneau-marshall-sse.md`). Today `@RestGet Stream<SseEvent>` works end-to-end (per-event flush is wired in `SseSerializerSession`), but writing a real SSE endpoint still requires hand-rolled glue: per-connection broadcaster fan-out, named heartbeat / keepalive scheduling, and a clean `res.sendEvent(name, data).flush()` idiom. Add:

- A `SseResponseSupport` mix-in (or convenience methods on `RestResponse`) that lets a `@RestGet` handler emit individual events without juggling `Writer` state.
- An `SseBroadcaster` bean that fan-outs to N subscribers from a single producer (server-side event bus).
- A `SseHeartbeat` scheduler (`@Bean ScheduledExecutorService`-driven) that emits `: ping` comments at a configurable cadence so corporate proxies don't kill idle SSE streams after 30s.

End-state developer experience:

```java
@RestGet("/stream")
public void stream(RestRequest req, RestResponse res, SseBroadcaster bus) {
    var sub = bus.subscribe(req.getRequestId());
    res.sse()                              // sets Content-Type, disables buffering, starts heartbeat
        .heartbeat(Duration.ofSeconds(15))
        .sendFrom(sub);                     // drains events from this subscriber until disconnect
}
```

## Why now

- The marshaller-side primitives shipped in 9.5 (`SseSerializer`, `SseParser`, `SseEvent`, `SseEventReader`, `SseSerializerSession` with `Writer.flush()` per event). See `FINISHED-46-juneau-marshall-sse.md`.
- The archive plan explicitly parked the server-side ergonomic layer: *"Returning a reactive-streams `Publisher<SseEvent>` is out of scope (Juneau-rest has no reactive-streams plumbing in the response pipeline today)"* — but the simpler push-from-server case is a clean follow-on.
- `juneau-microservice` now exposes a `WritableBeanStore` (TODO-31) so a `SseBroadcaster` registered as `@Bean` is auto-wired into resources.
- `BasicRestServletGroup.addChild(...)` (TODO-33) makes it easy to mount an SSE demo / health-stream child resource dynamically.

## Scope

**In scope (v1):**

- `org.apache.juneau.rest.sse.SseResponseSupport` (or `RestResponse.sse()` accessor) — fluent surface for `setContent-Type` to `text/event-stream`, disable response buffering, expose `sendEvent(SseEvent)` / `sendEvent(String name, Object data)` / `comment(String)` / `flush()` / `close()`.
- `org.apache.juneau.rest.sse.SseBroadcaster` — pub/sub fan-out bean. Methods: `subscribe(String id)` returns a `SseSubscription` (a `BlockingQueue<SseEvent>` wrapper with `Iterator<SseEvent>` and `close()`); `publish(SseEvent)` enqueues to every active subscriber; per-subscriber bounded queue with a configurable overflow policy (default: drop-oldest with a debug log).
- `org.apache.juneau.rest.sse.SseHeartbeat` — `ScheduledFuture`-driven `: ping\n\n` emitter; defaults to 15s cadence; cancellable via the returned handle.
- New `@RestGet`-friendly parameter `SseBroadcaster` / `SseSubscription` injection through the existing `RestOpArg` SPI (sibling of the existing `HttpServletRequestArgs`, `RestRequestArgs`).
- Demo endpoint added under `juneau-examples/juneau-examples-rest` exercising a broadcaster + heartbeat (the SSE-marshalling demo `SseDemoResource` is the obvious place to grow into a broadcaster example).
- Tests in `juneau-utest` covering: single-subscriber drain, multi-subscriber fan-out, slow-subscriber overflow, heartbeat insertion, client-disconnect cleanup (the writer throws — broadcaster must release the subscription).
- Release-notes entry under `### juneau-rest-server` in the active release-notes file; new topic page (`pages/topics/10.08.RestServerSse.md` or similar).

**Explicitly out of scope (v1):**

- Reactive-Streams `Publisher<SseEvent>` return types from `@RestOp` — orthogonal to TODO-70 (`CompletableFuture` + virtual-threads); the brainstorm marked it as "transport-layer change, not marshalling change."
- `Last-Event-ID` resume support (client-side concern; the bean already carries `id`; server-side resume would need a per-resource event journal — defer).
- Cross-JVM broadcasting (Redis / Kafka backplane). The `SseBroadcaster` SPI should be split into interface + in-memory impl so an external-backplane impl can be a sibling sub-module later, but no external impl in v1.
- Client-side SSE consumer ergonomics — `juneau-rest-client` already gets `SseEventReader` from the marshall module; if more is wanted, file a separate TODO.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Re-read `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/SseSerializerSession.java` to confirm the per-event flush contract — it already calls `Writer.flush()` per event, which is what makes the broadcaster path safe.
2. Inspect `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/SerializedPojoProcessor.java` to confirm the response is **not** drained / closed by the framework when a method calls `res.flushBuffer()` and writes directly — this is the seam the `SseResponseSupport` rides on. (Today's `SseDemoResource` proves the pattern works.)
3. Confirm `RestResponse.getNegotiatedWriter()` returns the same `Writer` `SseSerializerSession` operates on — yes, via `FinishablePrintWriter`.

### Phase 1 — `SseResponseSupport` (no broadcaster)

1. Add the new package `org.apache.juneau.rest.sse` in `juneau-rest-server`. Add `SseResponseSupport` with the fluent surface, plus the `RestResponse.sse()` accessor.
2. Add `SseHeartbeat` (a small `Runnable` that writes a comment + flushes) and wire the optional scheduler bean — when absent, `heartbeat(Duration)` is a no-op (no scheduler ⇒ no heartbeat).
3. Tests:
   - `SseResponseSupport_Test` — single-event emit, multi-event emit, comment write, charset is UTF-8, content-type is `text/event-stream` exactly.
   - `SseHeartbeat_Test` — heartbeat fires at the configured cadence, cancels on `close()`.

### Phase 2 — `SseBroadcaster` + arg injection

1. Add `SseBroadcaster` + `SseSubscription`. Default impl is in-memory with per-subscriber `LinkedBlockingQueue<SseEvent>` and a configurable bound (default: 1024 events).
2. Add `SseBroadcasterArg` / `SseSubscriptionArg` `RestOpArg` implementations so handlers can take them as parameters.
3. Tests:
   - `SseBroadcaster_Test` — pub/sub fan-out, slow-subscriber overflow policy, subscriber-disconnect cleanup, concurrent publisher / subscriber smoke.
   - `Rest_SseBroadcaster_IT_Test` (in `juneau-utest`) — `MockRestClient` against a `@RestGet` using the broadcaster; assert both subscribers receive every published event in order.

### Phase 3 — demo + docs

1. Update `juneau-examples/juneau-examples-rest/.../SseDemoResource.java` to demonstrate the broadcaster pattern (keep the existing `Stream<SseEvent>` example; add a new endpoint that uses `SseBroadcaster`).
2. New doc page `juneau-docs/pages/topics/10.08.RestServerSse.md` (slug `RestServerSse`) covering both the simple `Stream<SseEvent>` form and the broadcaster form. Sidebar entry.
3. Release-notes entry under `### juneau-rest-server`.

## Acceptance criteria

- [ ] `RestResponse.sse()` returns an `SseResponseSupport` that sets `Content-Type: text/event-stream`, disables response buffering, and exposes `sendEvent(...)` / `comment(...)` / `flush()` / `close()`.
- [ ] `SseBroadcaster.publish(event)` reaches every active subscriber, in order, with no drops below the per-subscriber bound. Slow-subscriber overflow drops the oldest event and logs at `DEBUG`.
- [ ] `SseHeartbeat` at a 15s cadence inserts `: ping\n\n` between events without corrupting the SSE stream (verified by `SseEventReader` parsing the captured output).
- [ ] Client disconnect → broadcaster releases the subscription within ≤ 1 heartbeat interval (no leak in long-soak test).
- [ ] Demo endpoint in `juneau-examples-rest` is observable via `curl -N` and shows live event delivery.
- [ ] Coverage ≥ 90% on the new package. Full `./scripts/test.py` green.
- [ ] Release-notes + topic page + sidebar entry shipped.

## Open questions

1. **Mix-in vs accessor.** `RestResponse.sse()` accessor (recommended) vs a separate `SseRestResponse extends RestResponse` mix-in. Accessor keeps the API surface small and avoids subclass churn.
2. **Overflow policy default.** Drop-oldest (recommended) vs drop-newest vs block-publisher. Drop-oldest matches what most SSE consumers expect.
3. **Per-subscriber queue bound default.** 1024 events / ~1MB worst case. Configurable per subscriber; configurable per broadcaster via `@Bean SseBroadcasterConfig`.
4. **Heartbeat cadence default.** 15s — under Nginx's default 30s idle timeout and AWS ALB's 60s default. Configurable.
5. **External-backplane SPI surface.** Should v1 ship `SseBroadcaster` as an interface (recommended) or a concrete class with hooks? Interface keeps the door open for Redis / Kafka backplanes as separate sub-modules without breaking changes.
6. **Naming.** `SseBroadcaster` or `SseEventBus`? Recommend `SseBroadcaster` — closer to the spec's "broadcasting" language.

## Risks

- **Servlet container buffering.** Some containers buffer the response despite `flushBuffer()`. Mitigation: the existing SSE demo proves it works in Jetty (per `FINISHED-46-*` verification with `curl -N`); call out Tomcat behavior in the docs if needed.
- **Thread leak on client disconnect.** A subscriber that never reads will pin a queue. Mitigation: per-subscriber bounded queue + a "no read in N heartbeats ⇒ evict" timer.
- **Coupling with TODO-67 (observability).** If TODO-67 introduces `X-Request-Id` propagation and broadcaster subscriptions are keyed by request id, the two need to align on the id source. Recommend `SseBroadcaster.subscribe(String id)` accepts any string — id source is the caller's concern.
- **Memory pressure under broadcast storms.** A 10k-subscriber broadcaster with 1024-event queues each can balloon to 10M events × event size. Document; configurable bound.

## Related work

- `todo/FINISHED-46-juneau-marshall-sse.md` — the marshalling-side SSE primitives this TODO consumes.
- `todo/FINISHED-31-inject-aware-microservice.md` — `WritableBeanStore` auto-wiring for `@Bean SseBroadcaster`.
- `todo/FINISHED-33-dynamic-rest-children.md` — useful for mounting SSE demo / metrics-stream resources at runtime.
- `todo/TODO-67-observability-micrometer-otel.md` (sibling) — `X-Request-Id` is the natural broadcaster-subscription key.
- `todo/TODO-70-async-completablefuture-virtual-threads.md` (sibling) — `Publisher<SseEvent>` return-type support lives there, not here.
