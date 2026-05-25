# TODO

## Execution order (re-evaluated 2026-05-25)

Four foundational TODOs (TODO-73, TODO-81, TODO-69) and four mixin packs (TODO-74–77) have landed. **TODO-20 (rest debug rethink) was un-parked 2026-05-25** with a hard-break decision + source-of-truth nested-annotation pattern added to the plan; it is now the next-in-flight item ahead of TODO-78. Plans for TODO-71, TODO-82–84, TODO-85–87, and TODO-89 are fleshed out (2026-05-24). TODO-37 (agent instruction consolidation) remains parked.

### Completed foundations + mixin family

1. ~~**TODO-73** — Runtime-overridable `@Rest(paths=...)`. Foundational; unblocked TODO-74–78.~~ ✅ done — see `todo/FINISHED-73-rest-paths-runtime-override.md`.
2. ~~**TODO-81** — Mixin sub-`RestContext` inheritance.~~ ✅ done — see `todo/FINISHED-81-mixin-sub-context-inheritance.md`.
3. ~~**TODO-74** — API-docs mixin pack (Swagger, Swagger-UI, OpenAPI, Redoc).~~ ✅ done — see `todo/FINISHED-74-mixin-api-docs.md`.
4. ~~**TODO-75** — Static-files mixin (`BasicStaticFilesResource`).~~ ✅ done — see `todo/FINISHED-75-mixin-static-files.md`.
5. ~~**TODO-76** — Convention-endpoints pack (favicon / SEO / version / well-known).~~ ✅ done — see `todo/FINISHED-76-mixin-convention-endpoints.md`.
6. ~~**TODO-77** — Ops/introspection pack (echo / admin / route-index).~~ ✅ done — see `todo/FINISHED-77-mixin-ops-introspection.md`. Landed `org.apache.juneau.rest.guard.DenyAllGuard` as the secure-by-default placeholder for `BasicAdminResource`; TODO-69 just needs to register its `BearerTokenGuard` / `ApiKeyGuard` into the host `RestGuardList` to unlock the admin paths (no source change to the mixin).

### Live execution order (next, in dependency order)

**Phase A — auth (completed):**

7. ~~**TODO-69** — AuthN guards + isolated `juneau-rest-server-jwt` sub-module.~~ ✅ done — see `todo/FINISHED-69-authn-guards-jwt-apikey.md`.

**Phase B — debug rethink (next in flight):**

8. ~~**TODO-20** — Rest debug rethink. Collapses five `@Rest`/`@RestOp` debug attributes + `DebugEnablement` + parallel `CallLogger` rule lists into a single `DebugConfig` bean + a typed `@Debug` annotation. **Hard break** (no deprecation cycle; migration notes in `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md`). **Source-of-truth pattern** — `@Debug` is nested as `@Rest(debug=@Debug(...))` and `@RestOp(debug=@Debug(...))` so the `@Rest`/`@RestOp` annotation is the canonical capability surface, with standalone `@Debug` retained as an escape hatch. FINISHED-35 satisfies the only external dep.~~ ✅ done — see `todo/FINISHED-20-rest-debug-rethink.md`.

**Phase C — view infrastructure (hard prereq for Phase D):**

9. **TODO-78** — JSP module (`juneau-rest-server-view-jsp`). Introduces the generic `View` interface in core `juneau-rest-server` and the `ResponseProcessor`-based renderer pattern that TODO-82/83/84 build on. Engine-agnostic POM (Option B) stance baked in. HARD prereq for TODO-82/83/84.

**Phase D — view module siblings (parallelizable after TODO-78 lands; no inter-sibling deps):**

10. **TODO-82** — Thymeleaf view module. Highest priority of the three (Spring Boot's default web view; biggest existing-user-base migration path). Plan: `todo/TODO-82-view-module-thymeleaf.md`.
11. **TODO-83** — Mustache view module. Smallest surface area, lowest risk. Plan: `todo/TODO-83-view-module-mustache.md`.
12. **TODO-84** — FreeMarker view module. Apache-family alignment bonus. Plan: `todo/TODO-84-view-module-freemarker.md`.

**Phase E — application showcase tier:**

13. **TODO-85** — `juneau-microservice-jetty-starter`. Standalone, no hard deps; can land any time after FINISHED-74/75/76/77 (which it bundles). Plan: `todo/TODO-85-microservice-jetty-starter.md`.
14. **TODO-86** — `juneau-petstore-jetty` sample app. Soft deps on TODO-85 (starter fallback to raw `juneau-microservice-jetty`), TODO-69 (auth fallback to `DenyAllGuard`), TODO-82 (preferred view engine: Thymeleaf, fallback to TODO-78 JSP). Defines the shared `juneau-petstore-core` module consumed by TODO-87. Plan: `todo/TODO-86-petstore-jetty-app.md`.
15. **TODO-87** — `juneau-petstore-springboot` sample app. HARD dep on `juneau-petstore-core` from TODO-86; should land alongside or immediately after TODO-86 as a coherent landing. Plan: `todo/TODO-87-petstore-springboot-app.md`.

> **Open question for TODO-86/87** (surfaced by planning worker): factor REST resource classes into a third sibling `juneau-petstore-rest` module so both sample apps consume identical resource classes? Worth resolving before either starts implementation.

**Phase F — quality-of-life follow-ons (parallelizable, any order, anytime after Phase A):**

16. **TODO-89** — `RateLimitGuard.Storage.snapshot()` SPI + `BasicAdminResource` enrichment. Small (~30 LOC + 2 test files), closes a known carry-over from FINISHED-77 (`"buckets": []` placeholder). Plan: `todo/TODO-89-ratelimit-storage-snapshot-spi.md`.
17. **TODO-71** — Doc-site script + Docusaurus search swap. Mostly already done (planning worker found `juneau-docs/scripts/build-docs.py` + `.github/workflows/deploy-docs.yml.disabled` already in place); this TODO is now a cutover/cleanup + the Algolia → `@easyops-cn/docusaurus-search-local` swap. Plan: `todo/TODO-71-docs-site-script-search-swap.md`.
18. **TODO-88** — YAML parser buffer-underflow on large OpenAPI 3.1 documents. Latent parser bug in `juneau-marshall`; opportunistic. Plan file TBD.

**Phase G — server-feature track (independent; can interleave anywhere after Phase A):**

19. **TODO-67** — Observability (Micrometer + OpenTelemetry). Plan: `todo/TODO-67-observability-micrometer-otel.md`.
20. **TODO-68** — Bean Validation (Jakarta Validation 3.x). Plan: `todo/TODO-68-bean-validation-integration.md`.
21. **TODO-70** — `CompletableFuture<?>` return-type + virtual-thread per-request dispatch. Plan: `todo/TODO-70-async-completablefuture-virtual-threads.md`.
22. **TODO-79** — Juneau `@Value` annotation + Spring Boot `application.yaml` bridge for the Config API. Plan: `todo/TODO-79-value-annotation-config-bridge.md`.

### Parked / unscheduled

- **TODO-37** — Agent instruction consolidation (unscoped).

### Natural review seams

Foundations (TODO-73 + TODO-81 + TODO-69) → mixin family (TODO-74–77) → debug rethink (TODO-20) → view infrastructure + siblings (TODO-78 + TODO-82/83/84) → application showcase (TODO-85/86/87) → quality-of-life (TODO-89 + TODO-71 + TODO-88) → server-feature track (TODO-67 + TODO-68 + TODO-70 + TODO-79).

## Items

- [TODO-37] - Agent instruction consolidation.

- [TODO-67] Observability hooks — Micrometer + OpenTelemetry seams via `MethodExecStats`. See `todo/TODO-67-observability-micrometer-otel.md`.

- [TODO-68] Bean Validation (Jakarta Validation 3.x) integration on request beans. See `todo/TODO-68-bean-validation-integration.md`.

- [TODO-70] `CompletableFuture<?>` return-type support + optional virtual-thread per-request dispatch. See `todo/TODO-70-async-completablefuture-virtual-threads.md`.

- [TODO-71] Move doc site updates from a github hook to a script that gets executed locally. Change docusaurus search functionality to @easyops-cn/docusaurus-search-local. See `todo/TODO-71-docs-site-script-search-swap.md`.

- [TODO-78] JSP servlet support module (`juneau-rest-server-view-jsp`) — new module shipping `BasicJspResource` mixin + `JspViewRenderer`; isolates Apache Jasper / `jakarta.servlet.jsp.*` / JSTL deps from core. See `todo/TODO-78-mixin-jsp-module.md`.

- [TODO-82] Thymeleaf view module (`juneau-rest-server-view-thymeleaf`) — sibling to TODO-78's JSP module. New Maven module shipping `BasicThymeleafResource` mixin + `ThymeleafViewRenderer` + `ThymeleafView` impl of the `View` interface introduced by TODO-78. High priority since Thymeleaf is Spring Boot's default web view technology; large existing user base for Spring-Boot-on-Juneau migrations. Same engine-agnostic POM stance as TODO-78 (Option B): bridge module declares Thymeleaf core API in `provided` scope only; example module supplies the concrete `thymeleaf` + `thymeleaf-spring6` deps. See `todo/TODO-82-view-module-thymeleaf.md`.

- [TODO-83] Mustache view module (`juneau-rest-server-view-mustache`) — sibling to TODO-78's JSP module. New Maven module shipping `BasicMustacheResource` mixin + `MustacheViewRenderer` + `MustacheView` impl of the `View` interface introduced by TODO-78. Logic-less templates; common choice for content authored by non-Java developers. Same engine-agnostic POM stance as TODO-78 (Option B): bridge declares the Mustache API in `provided` scope; example supplies the concrete impl (resolved decision: `com.github.spullara.mustache.java:compiler` over `jmustache`). See `todo/TODO-83-view-module-mustache.md`.

- [TODO-84] FreeMarker view module (`juneau-rest-server-view-freemarker`) — sibling to TODO-78's JSP module. New Maven module shipping `BasicFreemarkerResource` mixin + `FreemarkerViewRenderer` + `FreemarkerView` impl of the `View` interface introduced by TODO-78. Apache FreeMarker; widely used in admin consoles and reporting tools. Same engine-agnostic POM stance as TODO-78 (Option B): bridge declares `org.freemarker:freemarker` in `provided` scope; example supplies the runtime version. See `todo/TODO-84-view-module-freemarker.md`.

- [TODO-85] New `juneau-microservice-jetty-starter` module — zero-config quick-start for `JettyMicroservice`, conceptually analogous to Spring Boot's `spring-boot-starter-web`. Sits on top of the existing `juneau-microservice-jetty` runtime and bundles the dependency surface a new user typically wants (microservice-jetty + rest-server + the default marshall stack + sensible config defaults + a default `application.cfg`/`jetty.xml` baseline served from the JAR). Goal: `pom.xml` adds one dep, `main()` calls one factory, app comes up serving JSON/HTML over Jetty with a curated mixin set (API-docs + favicon + version) mounted by default. Mirrors the "you should not have to think about transitive deps to ship a hello-world Juneau service" ergonomic that Spring Boot solved on its side. See `todo/TODO-85-microservice-jetty-starter.md`.

- [TODO-86] New `juneau-petstore-jetty` sample application — a self-contained reference app deployed on top of `JettyMicroservice` (preferably via the TODO-85 starter once it lands) that exercises a wide cross-section of Juneau features so prospective users have one canonical place to read "how a real Juneau service is structured". Feature coverage: CRUD REST resources composed with the API-docs mixin pack (FINISHED-74), static-files mixin (FINISHED-75), convention endpoints (FINISHED-76), at least one ops/introspection mixin (FINISHED-77), at least one view-rendered HTML page (resolved decision: Thymeleaf via TODO-82 with JSP fallback), parent-chain mixin aggregation, `@Rest(paths=...)` runtime overrides (FINISHED-73), AuthN guards (TODO-69 once landed), and a non-trivial bean model with serialization round-trips across JSON/HTML/XML/URL-encoded/plain-text to showcase the marshall layer. Pet-store domain chosen for parity with the Swagger/OpenAPI canonical example so users can compare apples-to-apples against the spec. Defines the shared `juneau-petstore-core` Maven module also consumed by TODO-87. See `todo/TODO-86-petstore-jetty-app.md`.

- [TODO-87] New `juneau-petstore-springboot` sample application — identical domain + feature coverage as TODO-86, but deployed on Spring Boot via `BasicSpringRestServlet` + `ServletRegistrationBean` + `SpringBeanStore` (NOT `JuneauRestInitializer` — that class does not exist in the codebase; the plan file documents the correction). Mounted into a `@SpringBootApplication` so `mvn spring-boot:run` (or `java -jar`) brings up the same pet-store API with the same six API-docs URLs, same view-rendered pages, same guards, etc. Demonstrates the "byte-identical content across deployment modes" claim FINISHED-74 makes concrete in tests; also serves as the canonical reference for users migrating an existing Spring Boot app onto Juneau REST. Beans wired as `@Bean`s in a `@Configuration` class so the wiring style is recognizable to Spring devs. Shares the bean/domain model with TODO-86 via the `juneau-petstore-core` Maven module defined there. See `todo/TODO-87-petstore-springboot-app.md`.

- [TODO-89] `RateLimitGuard.Storage` snapshot SPI — surfaced during FINISHED-77's `BasicAdminResource` work. The `/admin/ratelimit` endpoint currently emits the rate-limit guard's static configuration only because `RateLimitGuard.Storage` has no read-side / snapshot operation; operators can see "what the rate-limit policy is" but not "which buckets are currently throttled and at what fill level". Add a `default Map<String, BucketState> snapshot()` to `RateLimitGuard.Storage` plus a new token-bucket-vocabulary `BucketState` record, override it in the bundled in-memory storage, and enrich `BasicAdminResource#getRateLimit(...)` to include live bucket state alongside configuration. Keep the SPI optional (default returns `Map.of()` so external Redis-backed storages keep compiling). Acceptance: `/admin/ratelimit` returns both config + live bucket state on the default storage. See `todo/TODO-89-ratelimit-storage-snapshot-spi.md`.

- [TODO-88] YAML parser buffer-underflow on large OpenAPI 3.1 documents — `OpenApiYamlRoundTrip_Test#c01` currently asserts against the small OpenAPI mount via `noInherit={"mixins"}` + `mixins=BasicOpenApiResource.class` because round-tripping the full post-FINISHED-74 `BasicRestServlet` mixin surface (six api-docs URLs, full schema set) through the YAML parser throws `java.io.IOException: Buffer underflow`. Surfaced during FINISHED-74's `apiFormat` removal but the root cause is a separate latent limitation in the YAML parser's buffer management on large docs, not a regression introduced by the mixin pack. Investigate the parser's read-buffer sizing and growth strategy (likely in `juneau-marshall`'s YAML parser core), reproduce with a focused parser-level test that bypasses REST entirely, fix the underflow, then remove the workaround from `OpenApiYamlRoundTrip_Test#c01`. Acceptance: full `BasicRestServlet` post-FINISHED-74 OpenAPI 3.1 doc round-trips JSON → YAML → JSON without `Buffer underflow`. Plan file TBD.

