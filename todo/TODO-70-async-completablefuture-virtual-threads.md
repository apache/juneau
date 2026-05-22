# TODO-70: `CompletableFuture<?>` return-type support + optional virtual-thread per-request dispatch

Source: split out of TODO-18 brainstorm on 2026-05-22.

## Goal

Unblock asynchronous and long-running I/O handlers in two complementary ways:

1. **`CompletableFuture` / `CompletionStage` return-type support.** A new `AsyncResponseProcessor` slotted ahead of `SerializedPojoProcessor` unwraps `CompletableFuture` / `CompletionStage` / `Future` return values, bridges to the servlet's `AsyncContext`, and feeds the completed value into the existing serialization pipeline once ready. Handlers can return `CompletableFuture<Order>` and the framework handles the dispatch.
2. **Virtual-thread per-request dispatch (Java 21+, opt-in).** A `@Rest(virtualThreads=true)` flag (or a `@Bean ExecutorService` matched by a well-known name) causes the resource's dispatcher to hand each call off to a virtual thread via `Thread.ofVirtual().factory()`, removing the thread-per-request blocking constraint.

End-state developer experience:

```java
@Rest(path="/orders", virtualThreads=true)
public class OrderResource {

    @RestGet("/{id}")
    public CompletableFuture<Order> get(@Path long id) {
        return orderService.lookupAsync(id);    // resource thread is not blocked
    }
}
```

## Why now

- `Stream<SseEvent>` return-type support landed via TODO-46 (`FINISHED-46-juneau-marshall-sse.md`), and that plan explicitly parked `Publisher<SseEvent>` and `CompletableFuture` as out-of-scope follow-ons: *"Returning a reactive-streams `Publisher<SseEvent>` is out of scope (Juneau-rest has no reactive-streams plumbing in the response pipeline today)."*
- Java 21 (virtual threads, stable since LTS) is widely deployed; the Juneau project floor is currently Java 17 but the virtual-thread path can be an opt-in code path that compiles under 17 and runs only on 21+ via reflective `Thread.ofVirtual()` invocation.
- `ResponseProcessor` chain is the right seam — `SerializedPojoProcessor` is the existing reference for "unwrap something, hand it to a serializer."
- `AsyncContext` is mature Servlet 3.1+ API; the framework already runs on Jakarta Servlet.

## Scope

**In scope (v1):**

- `org.apache.juneau.rest.processor.AsyncResponseProcessor` — slotted into the default `ResponseProcessorList` ahead of `SerializedPojoProcessor`. Detects `CompletableFuture` / `CompletionStage` / `Future` return values, starts servlet `AsyncContext`, registers a completion callback that re-feeds the unwrapped value into the rest of the chain.
- Default timeout for async completion (configurable; default 30s) — on timeout, write `504 Gateway Timeout` and abort the `AsyncContext`.
- `@Rest(virtualThreads=true)` flag (and per-op equivalent). When true, the `RestOpInvoker` dispatches handler invocation onto `Thread.ofVirtual().factory()`-backed `Executor`. Implementation is reflective (compile-time Java 17, runtime Java 21+ check); on Java 17 runtime the flag is silently ignored with a warning at context-init time.
- Tests in `juneau-utest`: `CompletableFuture<String>` return → expected body; `CompletableFuture` that throws → expected error path; async timeout → 504; virtual-thread dispatch confirmed via `Thread.currentThread().isVirtual()` in a handler (skipped on Java 17 runtime).
- Release-notes entry under `### juneau-rest-server`.

**Explicitly out of scope (v1):**

- Reactive-Streams `Publisher<T>` return types (Project Reactor / RxJava). Larger surface; defer to a sibling TODO if a concrete caller emerges.
- `Mono<T>` / `Flux<T>` direct support. Same — defer.
- `Flow.Publisher<SseEvent>` SSE streaming (TODO-62 owns server-side SSE; cross-coordinate when both land).
- Async filter chain (today's `@RestPreCall` / `@RestPostCall` are synchronous). Defer; the `AsyncResponseProcessor` only changes the *return* path.
- Coroutines / Kotlin `suspend` functions. Out of scope.
- Per-request `ThreadLocal` migration — see Risks; document that handlers must not assume thread-local persistence across the async boundary.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Re-read `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/SerializedPojoProcessor.java` and `ResponseProcessorList.java` to confirm the processor SPI return contract.
2. Re-read `RestOpInvoker.java` (or whatever invokes the handler method) to find the dispatch seam for virtual-thread switching.
3. Confirm `AsyncContext` is started before any other write/flush to the response — Servlet 3.1 requires this.
4. Inspect `RequestAttributes`, `VarResolverSession`, `Locale` — what's `ThreadLocal`-backed today vs request-scoped? (Most should be request-scoped via `RestSession`; document any thread-local leaks.)

### Phase 1 — `AsyncResponseProcessor`

1. Add the class. Detects `CompletableFuture` / `CompletionStage` / `Future` in `res.getContent(Object.class)`.
2. Calls `req.getRequest().startAsync()`, registers `whenComplete((value, error) -> ...)` callback that:
   - On success: stuffs `value` back into `RestResponse.setContent(value)` and re-invokes the response-processor chain (skipping `AsyncResponseProcessor` itself to avoid recursion) under the async context, then `complete()`.
   - On failure: stuffs the throwable into `RestResponse.setException(...)`, re-invokes the chain (so `ThrowableProcessor` / `ProblemDetailsProcessor` handle it), then `complete()`.
3. Timeout: register an `AsyncListener.onTimeout` that writes 504 and completes.
4. Tests:
   - `AsyncResponseProcessor_Test` — `CompletableFuture<String>` happy path.
   - `AsyncResponseProcessor_Error_Test` — `CompletableFuture` that throws → `ThrowableProcessor` handles it.
   - `AsyncResponseProcessor_Timeout_Test` — never-completing future → 504 after the configured timeout.

### Phase 2 — virtual-thread dispatch (Java 21+, opt-in)

1. Add `virtualThreads` attribute to `@Rest` and the per-op annotations.
2. At context-init, if `virtualThreads=true` and `Runtime.version().feature() >= 21`, build a `Thread.ofVirtual().factory()`-backed `Executor` via reflection and stash on `RestContext`. If runtime is Java 17/18/19/20, log a warning and proceed as if `virtualThreads=false`.
3. `RestOpInvoker` (or equivalent dispatch class) submits handler invocation to the executor when present; falls back to direct (caller-thread) invocation otherwise.
4. Tests:
   - `VirtualThreadDispatch_Test` (annotated `@DisabledOnJre(JRE.JAVA_17)` or similar) — handler reports `Thread.currentThread().isVirtual() == true`.
   - `VirtualThreadDispatch_Java17_Warning_Test` — context-init under Java 17 logs the configured-but-unsupported warning and proceeds.

### Phase 3 — docs + release notes

1. Release-notes entry under `### juneau-rest-server`.
2. New doc page (or section) covering both flavors, with a thread-local caveats callout.

## Acceptance criteria

- [ ] `@RestGet` returning `CompletableFuture<String>` sends the unwrapped string body to the client when the future completes.
- [ ] Same handler returning a future that fails with `NotFound` propagates the exception through `ThrowableProcessor` and produces a 404.
- [ ] A handler returning a future that never completes results in a 504 response after the configured timeout (default 30s).
- [ ] `@Rest(virtualThreads=true)` on Java 21+ dispatches handler invocation on a virtual thread.
- [ ] `@Rest(virtualThreads=true)` on Java 17/18/19/20 logs a warning at context-init and falls back to caller-thread dispatch — no runtime error.
- [ ] Existing synchronous handlers (no `CompletableFuture`, no `virtualThreads=true`) have zero behavioral change.
- [ ] Coverage ≥ 90% on `AsyncResponseProcessor`; ≥ 85% on the virtual-thread dispatch path (some paths skipped on Java 17 CI runs). Full `./scripts/test.py` green on both Java 17 and Java 21 if both are available in CI.

## Open questions

1. **Java floor.** Stay on Java 17 for the project floor; gate virtual-threads behind runtime detection (recommended) vs bump the floor to Java 21. Recommend stay on 17 — bumping is a larger ecosystem decision.
2. **Default async timeout.** 30 seconds (recommended — matches typical proxy timeouts) vs unlimited. Configurable.
3. **Reactor / Mono / Flux integration.** Out of scope for v1 — confirm. Could ship as a `juneau-rest-server-reactor` sub-module later (mirror the TODO-67 pattern of opt-in sub-modules for external deps).
4. **`Future<T>` support.** Bare `java.util.concurrent.Future` (not `CompletableFuture`) requires polling. Recommend honor it via `ForkJoinPool.commonPool().submit(future::get)` rather than blocking the request thread — but it's a footgun. Alternative: reject bare `Future` and require `CompletableFuture`. Recommend reject with a clear error message.
5. **Thread-local handling.** `RequestAttributes` and `VarResolverSession` are request-scoped (not `ThreadLocal`) so they survive the async boundary. Confirm nothing else relies on `ThreadLocal` (logging MDC is the obvious one — document the MDC contract change).
6. **Sub-module placement.** Land the async processor in `juneau-rest-server` directly (recommended — uses only JDK APIs) vs a sub-module. Sub-module makes sense if we later add Reactor.

## Risks

- **Thread-local leakage across the async boundary.** Anything carried via `ThreadLocal` (SLF4J MDC, security contexts, OTel scope) silently breaks when the handler returns a `CompletableFuture` that completes on a different thread. Mitigation: document loudly; recommend `MDC.put`-equivalents move to `RequestAttributes`; the OTel hook in TODO-67 should use `Scope` (which `OtelTracerHook` closes in `RestEndCall`, not bound to any specific thread).
- **AsyncContext error-handling subtleties.** `complete()` must be called exactly once; double-complete throws. Mitigation: atomic state machine in `AsyncResponseProcessor`.
- **Virtual-thread pinning.** Synchronized blocks and JNI calls pin a virtual thread to its carrier thread, defeating the benefit. Mitigation: document; recommend `ReentrantLock` over `synchronized` in handlers.
- **Test flakiness.** Async tests with timeouts are easy to make flaky. Mitigation: use `Awaitility` or equivalent for assertion polling; keep timeout tests deterministic.
- **Cross-cutting overlap with TODO-62 (SSE).** SSE handlers are inherently long-running; virtual-thread dispatch is the obvious fit. Both should land before users heavily adopt SSE.

## Related work

- `todo/FINISHED-46-juneau-marshall-sse.md` — explicitly parked `Publisher<SseEvent>` and `CompletableFuture` as future work.
- `todo/TODO-62-sse-server-helpers.md` (sibling) — server-side SSE handlers benefit from virtual-thread dispatch; coordinate landing order.
- `todo/TODO-67-observability-micrometer-otel.md` (sibling) — OTel `Scope` must survive the async boundary; coordinate on the `TracerHook` lifecycle.
- `todo/TODO-61-rfc7807-server-side-wiring.md` (sibling) — Problem-Details rendering must work for both sync and async returns; the `AsyncResponseProcessor` re-invokes the chain so this is automatic, but verify in tests.
- Existing: `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/SerializedPojoProcessor.java` — the literal template for the new processor's serialization path.
