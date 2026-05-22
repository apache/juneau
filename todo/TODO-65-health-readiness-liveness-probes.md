# TODO-65: Health / readiness / liveness probe endpoints + `HealthIndicator` SPI

Source: split out of TODO-18 brainstorm on 2026-05-22.

## Goal

Add standard Kubernetes-style probe endpoints out of the box: a `BasicHealthResource` opt-in that mounts `/healthz`, `/readyz`, `/livez` (or whatever paths the user prefers), aggregating status from any number of `HealthIndicator` beans pulled from the bean store. Each indicator reports `UP` / `DOWN` / `UNKNOWN` plus optional structured details; the resource composes them into a single response and sets the HTTP status (`200 OK` if all `UP`, `503 Service Unavailable` if any `DOWN`).

End-state developer experience:

```java
// User config class
@Configuration
public class AppConfig {
    @Bean
    HealthIndicator dbHealth(DataSource ds) {
        return () -> {
            try (var c = ds.getConnection()) {
                return Health.up("db").detail("validationQueryMs", 12).build();
            } catch (SQLException e) {
                return Health.down("db", e).build();
            }
        };
    }
}

// In the microservice bootstrap
Microservice.create()
    .configurations(JettyConfiguration.class, AppConfig.class, HealthProbeConfiguration.class)
    .build().start();
// → GET /healthz → {"status":"UP","components":{"db":{"status":"UP","details":{"validationQueryMs":12}}}}
// → 503 if any component is DOWN.
```

## Why now

- `juneau-microservice` now exposes a `WritableBeanStore` populated from `@Configuration` (TODO-31, `FINISHED-31-inject-aware-microservice.md`); `getBeansOfType(HealthIndicator.class)` is a one-liner.
- `BasicRestObjectGroup.addChild(...)` (TODO-33, `FINISHED-33-dynamic-rest-children.md`) makes the probe resource dynamically mountable — no static `@Rest(children=...)` ceremony required.
- Every container deployment expects these endpoints; today users hand-roll one.
- Pairs naturally with TODO-67 (observability) — Micrometer's `HealthIndicator` interface is the obvious shape to model on.

## Scope

**In scope (v1):**

- `org.apache.juneau.rest.health.HealthIndicator` SPI (single method `Health check()`).
- `Health` value object (status + name + details map + optional throwable). Static builders: `Health.up(name)`, `Health.down(name, throwable)`, `Health.unknown(name)`.
- `BasicHealthResource` — a `BasicRestObject` subclass with three `@RestGet` methods (`/healthz`, `/readyz`, `/livez`). All three aggregate from the bean store; the difference is the indicator filter (see Open Question #2).
- `HealthProbeConfiguration` — a `@Configuration` class that contributes the `BasicHealthResource` as a `@Bean Servlet` so the Jetty auto-mount machinery picks it up.
- Tests in `juneau-utest`: aggregator semantics, status-code mapping, structured-details serialization.

**Explicitly out of scope (v1):**

- Pull-style /push-style metrics (that's TODO-67).
- Spring Boot Actuator-style discovery of `/actuator/*` siblings (`/info`, `/env`, `/loggers`, `/threads`). The probe endpoints are the minimum viable surface; broader actuator parity is a follow-on.
- Persistent health history. Each call evaluates fresh.
- Caching of indicator results across probes. (DB indicators may want this; let the indicator implementer do it themselves — keep the framework dumb.)

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm `BasicRestObject` / `BasicRestObjectGroup` is the right base for the probe resource — yes, has `@RestGet` ergonomics and bean-store access.
2. Confirm `WritableBeanStore.getBeansOfType(HealthIndicator.class)` returns the right shape — yes (TODO-24).

### Phase 1 — SPI + value object + resource

1. Add the SPI + `Health` + builders.
2. Add `BasicHealthResource` with the three `@RestGet` methods.
3. Add `HealthProbeConfiguration` so users get the probes by just adding the `@Configuration` class to `Microservice.Builder.configurations(...)`.
4. Tests:
   - `HealthIndicator_Test` — aggregator returns `UP` when all `UP`, `DOWN` when any `DOWN`, status code matches.
   - `BasicHealthResource_Test` — `MockRestClient` against a resource with two indicators (one `UP`, one `DOWN`) returns 503 with the expected body shape.

### Phase 2 — docs + release notes

1. Release-notes entry under `### juneau-rest-server` + `### juneau-microservice-jetty` (for `HealthProbeConfiguration`).
2. New doc page (`pages/topics/14.11.HealthProbes.md` or sibling slot).

## Acceptance criteria

- [ ] `HealthIndicator` SPI is a single-method functional interface; `@Bean HealthIndicator` registrations are auto-discovered via `BeanStore.getBeansOfType(...)`.
- [ ] `/healthz` returns 200 + `{"status":"UP",...}` when all indicators are `UP`.
- [ ] `/healthz` returns 503 + `{"status":"DOWN","components":{...}}` when any indicator is `DOWN`.
- [ ] `/livez` and `/readyz` operate on indicator subsets (see Open Question #2 for the filter mechanism).
- [ ] `HealthProbeConfiguration` adds the resource without the user touching `@Rest(children=...)`.
- [ ] Coverage ≥ 95%. Full `./scripts/test.py` green.

## Open questions

1. **Status-code policy.** `503` on any `DOWN` (recommended — k8s convention) vs always-200 with the structured body carrying the status. K8s probes match on HTTP status, so 503-on-down is correct.
2. **Live vs ready filtering mechanism.** Three options: (a) tag indicators with a `Set<Probe>` (`LIVE`, `READY`, `STARTUP`) — recommended; (b) separate `LivenessIndicator` / `ReadinessIndicator` interfaces; (c) one indicator, run for all three probes. Option (a) keeps the SPI surface single.
3. **Response format.** Recommend the Spring Boot Actuator-style `{status, components: {name: {status, details}}}` JSON. Alternative: custom JSON, or content-negotiated XML/JSON.
4. **Auto-include the resource by default?** Recommend opt-in via `HealthProbeConfiguration` (matches the `@Configuration` pattern). Auto-include via `juneau-microservice-core` would be a behavioral change for existing users.
5. **Probe path defaults.** `/healthz`, `/readyz`, `/livez` (k8s convention, recommended) vs `/health`, `/ready`, `/live` vs `/actuator/health` (Spring convention). Make configurable; default to `/healthz` etc.

## Risks

- **Probe latency.** A slow indicator (e.g. DB query) stalls the probe; k8s may mark the pod unhealthy spuriously. Mitigation: a per-indicator timeout (configurable; default 1s) wrapping the check call.
- **Indicator throws unchecked exception.** The aggregator catches `Throwable` and converts to `DOWN` with the throwable in `details.error`.
- **Concurrent probe storms.** Multiple probes hitting expensive indicators simultaneously. Mitigation: document; indicator implementer can add a `CompletableFuture`-cached value if needed.

## Related work

- `todo/FINISHED-31-inject-aware-microservice.md` — `WritableBeanStore` makes `getBeansOfType(HealthIndicator.class)` trivial.
- `todo/FINISHED-33-dynamic-rest-children.md` — dynamic mount of the probe resource without `@Rest(children=...)` ceremony.
- `todo/FINISHED-36-jetty-as-bean.md` — `@Bean Servlet` auto-mount picks up the probe resource at `@Rest(path=...)`.
- `todo/TODO-67-observability-micrometer-otel.md` (sibling) — Micrometer's `HealthIndicator` interface is the reference shape; if TODO-67 wires Micrometer, the indicators could bridge to its registry.
- `todo/TODO-66-rate-limit-and-request-id.md` (sibling) — probe paths should be exempt from rate-limit guards by default.
