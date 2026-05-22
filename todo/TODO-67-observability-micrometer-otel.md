# TODO-67: Observability hooks — Micrometer + OpenTelemetry seams via `MethodExecStats`

Source: split out of TODO-18 brainstorm on 2026-05-22.

## Goal

Expose the existing per-method execution statistics (`MethodExecStats` / `RestContextStats`) through pluggable observability backends, so a Juneau REST server can drop into Prometheus / OpenTelemetry pipelines with no hand-rolled instrumentation.

Two complementary surfaces, each in its own opt-in sub-module:

1. **`juneau-rest-server-micrometer`** — a `MetricsRecorder` that bridges `MethodExecStats` counters/timers into a `MeterRegistry` (Prometheus, StatsD, JMX, whatever).
2. **`juneau-rest-server-otel`** — an OpenTelemetry tracer hook that creates a span per request, populates standard HTTP attributes (`http.request.method`, `http.response.status_code`, `http.route`), and propagates the `traceparent` / `tracestate` headers.

End-state developer experience:

```java
// pom: add juneau-rest-server-micrometer + your preferred MeterRegistry (e.g. micrometer-registry-prometheus).
@Configuration
public class ObservabilityConfig {
    @Bean MeterRegistry registry() { return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT); }
    @Bean MetricsRecorder recorder(MeterRegistry r) { return new MicrometerMetricsRecorder(r); }
}
// → MethodExecStats events automatically fan out to the registry; scrape via /actuator/prometheus.

// pom: add juneau-rest-server-otel + OTel SDK.
@Bean OpenTelemetry otel() { return GlobalOpenTelemetry.get(); }
@Bean TracerHook hook(OpenTelemetry otel) { return new OtelTracerHook(otel); }
// → Each request becomes a span; W3C trace context propagates in/out.
```

## Why now

- `MethodExecStats` and `RestContextStats` already track per-method runs / avgTime / maxTime / errors. The data is there — only the wiring is missing.
- `RestStartCall` / `RestEndCall` hooks are the right SPI surface for span creation / closure; both are stable and already used by `BasicCallLogger`.
- TODO-31 made `WritableBeanStore` first-class on the microservice path, so a `@Bean MeterRegistry` flows through automatically.
- TODO-66 (sibling) introduces `RequestIdFilter`; the request id is the natural span attribute / log correlation id.
- TODO-20 (rest debug rethink) is moving call-logging to a structured `DebugFormat` SPI — both efforts want a cleaner observability boundary, so getting this in early reduces churn.

## Scope

**In scope (v1):**

- New SPI in `juneau-rest-server`: `org.apache.juneau.rest.metrics.MetricsRecorder` (interface) — `record(String opName, Duration elapsed, int statusCode, Throwable maybeError)`. Default `NoOpMetricsRecorder` registered by default. `MethodExecStats` invokes the configured recorder at the end of each call.
- New SPI in `juneau-rest-server`: `org.apache.juneau.rest.tracing.TracerHook` (interface) — `Scope startSpan(RestRequest req)` returning an `AutoCloseable` `Scope`; the framework invokes `Scope.close()` in `RestEndCall`. Default `NoOpTracerHook`.
- New sub-module **`juneau-rest-server-micrometer`** in `juneau-rest/`: `MicrometerMetricsRecorder` impl; opt-in dep on `io.micrometer:micrometer-core` (provided scope).
- New sub-module **`juneau-rest-server-otel`** in `juneau-rest/`: `OtelTracerHook` impl + `W3CTracePropagator` for in/out propagation; opt-in dep on `io.opentelemetry:opentelemetry-api` (provided scope).
- W3C `traceparent` / `tracestate` header in/out propagation (in the OTel sub-module) so distributed-tracing context survives.
- Tests in `juneau-utest`: SPI contract tests with a `RecordingMetricsRecorder` / `RecordingTracerHook`. Module-local tests in each sub-module verify the Micrometer / OTel bridge.

**Explicitly out of scope (v1):**

- Structured-logging bridges (SLF4J / Log4j2 structured appender) — TODO-20 owns the call-logger rework; the OTel sub-module can publish a `Logs` event later if requested.
- Custom tag schemes per resource. v1 uses fixed OTel HTTP semantic-convention attribute names.
- Histogram percentile config from annotations — let the user configure the `MeterRegistry`.
- StatsD / Datadog / NewRelic native bridges. Use Micrometer's registries.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. `MethodExecStats.add(...)` (or whatever the per-call invocation hook is) — confirm it's reachable from a recorder. Inspect `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/stats/MethodExecStats.java` and `MethodInvoker.java`.
2. `RestStartCall` / `RestEndCall` annotation invocation timing — confirm `RestEndCall` runs even on exception paths.
3. `RestRequest.getRequestId()` (or its equivalent if TODO-66 doesn't land first) — for span correlation. If TODO-66 hasn't landed, OTel hook generates its own span id and stashes on `RequestAttributes`.

### Phase 1 — SPI seams in `juneau-rest-server`

1. Add `MetricsRecorder` + `NoOpMetricsRecorder`. Wire into `MethodExecStats` end-of-call.
2. Add `TracerHook` + `NoOpTracerHook`. Wire into `RestStartCall` / `RestEndCall`.
3. Tests: `MetricsRecorder_Contract_Test`, `TracerHook_Contract_Test` using a recording impl.

### Phase 2 — Micrometer sub-module

1. Create `juneau-rest/juneau-rest-server-micrometer/`. `pom.xml` mirrors `juneau-rest-server-mcp` (closest sibling — small, single-purpose, opt-in module).
2. `MicrometerMetricsRecorder` translates `(opName, elapsed, statusCode, error)` → `Timer.builder("http.server.requests").tags(...).register(registry).record(elapsed)`.
3. Tests verify Prometheus scrape output via `PrometheusMeterRegistry.scrape()`.

### Phase 3 — OTel sub-module

1. Create `juneau-rest/juneau-rest-server-otel/`. Same pom shape.
2. `OtelTracerHook` creates a span per request, sets standard HTTP attributes, propagates W3C context in/out. `TextMapPropagator` for in/out header carrier.
3. Tests verify span creation + W3C header round-trip.

### Phase 4 — docs + release notes

1. Release-notes entries under `### juneau-rest-server`, `### juneau-rest-server-micrometer (new module)`, `### juneau-rest-server-otel (new module)`.
2. Two new doc pages (one per sub-module).

## Acceptance criteria

- [ ] `MetricsRecorder` SPI receives one event per `@RestOp` call with operation name, elapsed time, status code, and optional throwable.
- [ ] `TracerHook` SPI receives `startSpan` / `Scope.close` exactly once per call (including error paths).
- [ ] `MicrometerMetricsRecorder` registers a `Timer` named `http.server.requests` with tags `{method, uri, status, exception}` matching Spring Boot's convention (eases scrape-config reuse).
- [ ] `OtelTracerHook` produces a span with `http.request.method`, `http.response.status_code`, `http.route` attributes per OTel HTTP semantic conventions.
- [ ] Incoming `traceparent` header continues an existing trace; outgoing `traceparent` propagates the trace id to downstream services.
- [ ] Each sub-module's pom uses `provided` scope on its external dep so the user pulls the version they want.
- [ ] Coverage ≥ 90% on the new SPI in `juneau-rest-server`; ≥ 85% on each sub-module. Full `./scripts/test.py` green.

## Open questions

1. **Module placement.** Two sub-modules under `juneau-rest/` (recommended — matches `juneau-rest-server-mcp` precedent) vs one combined `juneau-rest-server-observability` module. Two modules keep deps isolated.
2. **Metric naming convention.** Spring Boot's `http.server.requests` + `{method, uri, status, exception}` tags (recommended — wide tooling support) vs OTel-native `http.server.duration` + `{http.request.method, http.response.status_code, ...}`. Recommend Spring-style for Micrometer (matches existing dashboards); use OTel-native names only in the OTel sub-module.
3. **Tag cardinality for `uri`.** Raw URI is high-cardinality (each `/users/123`, `/users/124`, … is unique). Use the `@RestOp` template path (`/users/{id}`) as the `uri` tag — recommend. Requires `RestContext` lookup at recording time.
4. **`MetricsRecorder` vs direct `MeterRegistry` bean.** The SPI indirection lets a user swap Micrometer for any other backend (Dropwizard Metrics, custom). Recommend keep the SPI; the bridge is the only Micrometer-dependent class.
5. **OTel `Tracer` source.** Use `GlobalOpenTelemetry.get()` (recommended — standard practice) or require an injected `Tracer`? Both supported; the Configuration class picks.
6. **Logging-bridge (OTel Logs).** Out of scope for v1 — confirm.

## Risks

- **Recorder overhead on hot path.** Default `NoOpMetricsRecorder` must short-circuit cleanly (no allocations). Mitigation: enforce via JMH micro-benchmark in the SPI test.
- **Spec drift.** OTel semantic conventions are still evolving (`http.method` → `http.request.method` happened in 2023). Pin to a stable version range and document.
- **Memory growth from tag cardinality.** A misconfigured tag (raw URI, raw user agent) blows up the metrics registry. Document; default to route-template URIs.
- **Cross-cutting overlap with TODO-20 (debug rethink).** The new `DebugFormat` SPI in TODO-20 may want to read the OTel span id for log correlation. Recommend: TODO-67 stashes `traceId` / `spanId` on `RequestAttributes` under stable keys so TODO-20's formatter can read them.

## Related work

- `todo/FINISHED-31-inject-aware-microservice.md` — `WritableBeanStore` flows `@Bean MeterRegistry` / `@Bean OpenTelemetry` automatically.
- `todo/TODO-20-rest-debug-rethink.md` — call-logger rework should coordinate on tag / id sharing.
- `todo/TODO-66-rate-limit-and-request-id.md` (sibling) — `requestId` from TODO-66 becomes the log-correlation key alongside the OTel span id.
- `todo/TODO-65-health-readiness-liveness-probes.md` (sibling) — Micrometer's `HealthIndicator` could bridge to the TODO-65 SPI (a `MicrometerHealthBridge` adapter, optional).
- Existing: `MethodExecStats` / `RestContextStats` / `ThrownStats` in `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/stats/` — the data sources this TODO consumes.
