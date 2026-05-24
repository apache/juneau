# TODO

## Execution order (decided 2026-05-23)

Recommended order for the TODO-67 through TODO-78 family. TODO-20 (rest debug rethink) and TODO-37 (agent instruction consolidation) are not in this order — TODO-20 is parked pending user review, TODO-37 is unscoped.

1. ~~**TODO-73** — Runtime-overridable `@Rest(paths=...)`. Foundational; unblocks TODO-74–78.~~ ✅ done — see `todo/FINISHED-73-rest-paths-runtime-override.md`.
2. ~~**TODO-81** — Mixin sub-`RestContext` inheritance. Each `@Rest(mixins=...)` class gets its own `RestContext` parent-linked to the host. Foundational for TODO-74's `BasicOpenApiResource` (YAML isolation) and for TODO-77's per-mixin guard/debug/logger overrides; cross-cuts all mixins in TODO-74–78.~~ ✅ done — see `todo/FINISHED-81-mixin-sub-context-inheritance.md`. TODO-74 OQ1 (YAML for `/openapi.yaml`) is now unblocked.
3. **TODO-69** — AuthN guards. Unblocks TODO-77's admin guard chain.
4. ~~**TODO-74** — API-docs mixin pack (Swagger, Swagger-UI, OpenAPI, Redoc). Consumes the mixin sub-context model from FINISHED-81.~~ ✅ done — see `todo/FINISHED-74-mixin-api-docs.md`.
5. **TODO-75** — Static-files mixin (`BasicStaticFilesResource`).
6. **TODO-76** — Convention-endpoints pack (favicon / SEO / version / well-known).
7. **TODO-77** — Ops/introspection pack (echo / admin / route-index). Uses TODO-69.
8. **TODO-78** — JSP module (`juneau-rest-server-view-jsp`).
9. **TODO-67** — Observability (Micrometer + OpenTelemetry).
10. **TODO-68** — Bean Validation (Jakarta Validation 3.x).
11. **TODO-70** — `CompletableFuture` + virtual threads.
12. **TODO-71** — Doc-site script + Docusaurus search swap.

Natural review seams: foundations (TODO-73 + TODO-81 + TODO-69) → mixin family (TODO-74–78) → server features (TODO-67 + TODO-68 + TODO-70) → tooling (TODO-71).

## Items

- [TODO-20] - Rest debug rethink.

- [TODO-37] - Agent instruction consolidation.

- [TODO-67] Observability hooks — Micrometer + OpenTelemetry seams via `MethodExecStats`. See `todo/TODO-67-observability-micrometer-otel.md`.

- [TODO-68] Bean Validation (Jakarta Validation 3.x) integration on request beans. See `todo/TODO-68-bean-validation-integration.md`.

- [TODO-69] AuthN guards — `BearerTokenGuard`, `ApiKeyGuard`, optional JWT verification. See `todo/TODO-69-authn-guards-jwt-apikey.md`.

- [TODO-70] `CompletableFuture<?>` return-type support + optional virtual-thread per-request dispatch. See `todo/TODO-70-async-completablefuture-virtual-threads.md`.

- [TODO-71] Move doc site updates from a github hook to a script that gets executed locally.  Change docusaurus search functionality to @easyops-cn/docusaurus-search-local. 

- [TODO-75] Static-files mixin (`BasicStaticFilesResource`) — wrap `BasicStaticFiles` in a multi-mount mixin with default mounts `/static/*` and `/htdocs/*`. See `todo/TODO-75-mixin-static-files.md`.

- [TODO-76] Convention-endpoints mixin pack — `BasicFaviconResource`, `BasicSeoResource` (`/robots.txt`, `/sitemap.xml`), `BasicVersionResource` (`/version`, `/info`, `/about`), `BasicWellKnownResource` (`/.well-known/*`). See `todo/TODO-76-mixin-convention-endpoints.md`.

- [TODO-77] Ops/introspection mixin pack — `BasicEchoResource` (Debug-gated), `BasicAdminResource` (guard-chain-gated, depends on TODO-69), `BasicRouteIndexResource`. See `todo/TODO-77-mixin-ops-introspection.md`.

- [TODO-78] JSP servlet support module (`juneau-rest-server-view-jsp`) — new module shipping `BasicJspResource` mixin + `JspViewRenderer`; isolates Apache Jasper / `jakarta.servlet.jsp.*` / JSTL deps from core. See `todo/TODO-78-mixin-jsp-module.md`.

- [TODO-79] Juneau `@Value` annotation + Spring Boot `application.yaml` bridge for the Config API — introduce a `@Value("${...}")` annotation on top of `Config` so beans / fields / setters can read configuration values declaratively (analog to Spring's `@Value`); add a Spring Boot integration so values defined in `application.yaml` / `application.properties` are accessible through the Juneau `Config` API uniformly with native `*.cfg` files. Plan file TBD.

- [TODO-82] Thymeleaf view module (`juneau-rest-server-view-thymeleaf`) — sibling to TODO-78's JSP module. New Maven module shipping `BasicThymeleafResource` mixin + `ThymeleafViewRenderer` + `ThymeleafView` impl of the `View` interface introduced by TODO-78. High priority since Thymeleaf is Spring Boot's default web view technology; large existing user base for Spring-Boot-on-Juneau migrations. Same engine-agnostic POM stance as TODO-78 (Option B): bridge module declares Thymeleaf core API in `provided` scope only; example module supplies the concrete `thymeleaf` + `thymeleaf-spring6` deps. Plan file TBD.

- [TODO-83] Mustache view module (`juneau-rest-server-view-mustache`) — sibling to TODO-78's JSP module. New Maven module shipping `BasicMustacheResource` mixin + `MustacheViewRenderer` + `MustacheView` impl of the `View` interface introduced by TODO-78. Logic-less templates; common choice for content authored by non-Java developers. Same engine-agnostic POM stance as TODO-78 (Option B): bridge declares the Mustache API in `provided` scope; example supplies the concrete impl (e.g. `com.github.spullara.mustache.java:compiler` or `com.samskivert:jmustache`). Plan file TBD.

- [TODO-84] FreeMarker view module (`juneau-rest-server-view-freemarker`) — sibling to TODO-78's JSP module. New Maven module shipping `BasicFreemarkerResource` mixin + `FreemarkerViewRenderer` + `FreemarkerView` impl of the `View` interface introduced by TODO-78. Apache FreeMarker; widely used in admin consoles and reporting tools. Same engine-agnostic POM stance as TODO-78 (Option B): bridge declares `org.freemarker:freemarker` in `provided` scope; example supplies the runtime version. Plan file TBD.

- [TODO-85] New `juneau-microservice-jetty-starter` module — zero-config quick-start for `JettyMicroservice`, conceptually analogous to Spring Boot's `spring-boot-starter-web`. Sits on top of the existing `juneau-microservice-jetty` runtime and bundles the dependency surface a new user typically wants (microservice-jetty + rest-server + the default marshall stack + sensible config defaults + a default `application.cfg`/`jetty.xml` baseline served from the JAR). Goal: `pom.xml` adds one dep, `main()` calls one factory, app comes up serving JSON/HTML over Jetty with the four-mixin API-docs pack (TODO-74) mounted by default. Mirrors the "you should not have to think about transitive deps to ship a hello-world Juneau service" ergonomic that Spring Boot solved on its side. Plan file TBD.

- [TODO-86] New `juneau-petstore-jetty` sample application — a self-contained reference app deployed on top of `JettyMicroservice` (preferably via the TODO-85 starter once it lands) that exercises a wide cross-section of Juneau features so prospective users have one canonical place to read "how a real Juneau service is structured". Suggested feature coverage: CRUD REST resources composed with the API-docs mixin pack (TODO-74), static-files mixin (TODO-75), convention endpoints (TODO-76), at least one ops/introspection mixin (TODO-77), at least one view-rendered HTML page (JSP via TODO-78 OR Thymeleaf via TODO-82, pick one and document why), parent-chain mixin aggregation, `@Rest(paths=...)` runtime overrides (FINISHED-73), AuthN guards (TODO-69 once landed), and a non-trivial bean model with serialization round-trips across JSON/HTML/XML/YAML to showcase the marshall layer. Pet-store domain chosen for parity with the Swagger/OpenAPI canonical example so users can compare apples-to-apples against the spec. Plan file TBD.

- [TODO-87] New `juneau-petstore-springboot` sample application — identical domain + feature coverage as TODO-86, but deployed on Spring Boot via `JuneauRestInitializer` + `SpringBeanStore`. Mounted into a `@SpringBootApplication` so `mvn spring-boot:run` (or `java -jar`) brings up the same pet-store API with the same six API-docs URLs, same view-rendered pages, same guards, etc. Demonstrates the "byte-identical content across deployment modes" claim TODO-74 makes concrete in tests; also serves as the canonical reference for users migrating an existing Spring Boot app onto Juneau REST. Beans wired as `@Bean`s in a `@Configuration` class so the wiring style is recognizable to Spring devs. Shares the bean/domain model with TODO-86 — likely factored into a sibling `juneau-petstore-core` Maven module both sample apps depend on, rather than duplicating the bean code. Plan file TBD.

- [TODO-88] YAML parser buffer-underflow on large OpenAPI 3.1 documents — `OpenApiYamlRoundTrip_Test#c01` currently asserts against the small OpenAPI mount via `noInherit={"mixins"}` + `mixins=BasicOpenApiResource.class` because round-tripping the full post-FINISHED-74 `BasicRestServlet` mixin surface (six api-docs URLs, full schema set) through the YAML parser throws `java.io.IOException: Buffer underflow`. Surfaced during FINISHED-74's `apiFormat` removal but the root cause is a separate latent limitation in the YAML parser's buffer management on large docs, not a regression introduced by the mixin pack. Investigate the parser's read-buffer sizing and growth strategy (likely in `juneau-marshall`'s YAML parser core), reproduce with a focused parser-level test that bypasses REST entirely, fix the underflow, then remove the workaround from `OpenApiYamlRoundTrip_Test#c01`. Acceptance: full `BasicRestServlet` post-FINISHED-74 OpenAPI 3.1 doc round-trips JSON → YAML → JSON without `Buffer underflow`. Plan file TBD.

