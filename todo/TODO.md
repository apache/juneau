# TODO

## Execution order (decided 2026-05-23)

Recommended order for the TODO-67 through TODO-78 family. TODO-20 (rest debug rethink) and TODO-37 (agent instruction consolidation) are not in this order — TODO-20 is parked pending user review, TODO-37 is unscoped.

1. ~~**TODO-73** — Runtime-overridable `@Rest(paths=...)`. Foundational; unblocks TODO-74–78.~~ ✅ done — see `todo/FINISHED-73-rest-paths-runtime-override.md`.
2. ~~**TODO-81** — Mixin sub-`RestContext` inheritance. Each `@Rest(mixins=...)` class gets its own `RestContext` parent-linked to the host. Foundational for TODO-74's `BasicOpenApiResource` (YAML isolation) and for TODO-77's per-mixin guard/debug/logger overrides; cross-cuts all mixins in TODO-74–78.~~ ✅ done — see `todo/FINISHED-81-mixin-sub-context-inheritance.md`. TODO-74 OQ1 (YAML for `/openapi.yaml`) is now unblocked.
3. **TODO-69** — AuthN guards. Unblocks TODO-77's admin guard chain.
4. **TODO-74** — API-docs mixin pack (Swagger, Swagger-UI, OpenAPI, Redoc). Consumes the mixin sub-context model from FINISHED-81.
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

- [TODO-74] API-docs mixin pack (`BasicSwaggerResource`, `BasicSwaggerUiResource`, `BasicOpenApiResource`, `BasicRedocResource`) in new `org.apache.juneau.rest.docs` package. Replaces `BasicRestOperations.getSwagger`/`getOpenApi` and removes the `apiFormat` knob + `?Swagger`/`?OpenApi` query mirrors (breaking changes). First step in retiring `BasicRestOperations`/`BasicGroupOperations` (TODO-75/76/77 finish the job). See `todo/TODO-74-mixin-api-docs.md`.

- [TODO-75] Static-files mixin (`BasicStaticFilesResource`) — wrap `BasicStaticFiles` in a multi-mount mixin with default mounts `/static/*` and `/htdocs/*`. See `todo/TODO-75-mixin-static-files.md`.

- [TODO-76] Convention-endpoints mixin pack — `BasicFaviconResource`, `BasicSeoResource` (`/robots.txt`, `/sitemap.xml`), `BasicVersionResource` (`/version`, `/info`, `/about`), `BasicWellKnownResource` (`/.well-known/*`). See `todo/TODO-76-mixin-convention-endpoints.md`.

- [TODO-77] Ops/introspection mixin pack — `BasicEchoResource` (Debug-gated), `BasicAdminResource` (guard-chain-gated, depends on TODO-69), `BasicRouteIndexResource`. See `todo/TODO-77-mixin-ops-introspection.md`.

- [TODO-78] JSP servlet support module (`juneau-rest-server-view-jsp`) — new module shipping `BasicJspResource` mixin + `JspViewRenderer`; isolates Apache Jasper / `jakarta.servlet.jsp.*` / JSTL deps from core. See `todo/TODO-78-mixin-jsp-module.md`.

- [TODO-79] Juneau `@Value` annotation + Spring Boot `application.yaml` bridge for the Config API — introduce a `@Value("${...}")` annotation on top of `Config` so beans / fields / setters can read configuration values declaratively (analog to Spring's `@Value`); add a Spring Boot integration so values defined in `application.yaml` / `application.properties` are accessible through the Juneau `Config` API uniformly with native `*.cfg` files. Plan file TBD.

