# TODO-74: API-docs mixin (`BasicApiDocsResource`)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23.

## Goal

Extract the existing `/api`, `/swagger`, `/openapi`, `/openapi.json`, `/openapi.yaml`, and `/redoc` endpoints from `BasicGroupOperations` / `BasicRestServlet` into a standalone, mixin-able `BasicApiDocsResource`. The first highest-value mixin candidate after `BasicHealthResource` (which landed in FINISHED-72): API documentation today is gated behind inheriting from `BasicRestServlet` / `BasicRestObject`, but plain `RestServlet` apps should be able to opt in via `@Rest(mixins=BasicApiDocsResource.class)` — and standalone-mount it via `@Rest(paths={"/api"}) public class BasicApiDocsResource ...` for apps that want a dedicated docs-only deployment.

End-state developer experience:

```java
// Path A — plain RestServlet gains the api-docs surface as a mixin.
@Rest(path="/api", mixins=BasicApiDocsResource.class)
public class ApiResource extends RestServlet {
    @RestGet("/items") public List<Item> items() { ... }
    // Now also serves /api/swagger, /api/openapi.json, /api/redoc, etc.
}

// Path B — standalone API-docs deployment via the multi-mount story (TODO-73 + FINISHED-72).
@Rest(paths={"/api","/swagger","/openapi","/openapi.json","/openapi.yaml","/redoc"})
public class BasicApiDocsResource extends BasicRestServlet { ... }

// Pinned versioned mounts.
@Rest(mixins=BasicApiDocsResource.class, paths={"/api/v3.1"})
public class V31Docs extends BasicRestServlet {
    @Override public String getApiFormat() { return "openapi"; }
}
```

## Why now

- FINISHED-72 added `@Rest(mixins=...)` and `@Rest(paths=...)` precisely so single-purpose servlet bundles like this could be composed in.
- Today every Juneau service that wants Swagger UI / Redoc / OpenAPI 3.1 must extend the `BasicRestServlet` chain. The FINISHED-63 OpenAPI 3.1 work and the existing `apiFormat` knob already produce the artifacts; only the mounting surface needs lifting.
- Pairs naturally with TODO-73 (runtime-overridable paths) — apps with prefixed deployments (`/admin/api`, `/internal/api`) need the override hook so they don't have to subclass.

## Scope

**In scope (v1):**

- New class `org.apache.juneau.rest.docs.BasicApiDocsResource` (servlet-class form) with default `@Rest(paths={"/api","/swagger","/openapi","/openapi.json","/openapi.yaml","/redoc"})`.
- `@RestOp`-group methods extracted directly from `BasicGroupOperations.getChildrenSwagger(...)` / `getChildrenOpenApi(...)` plus the dedicated `/swagger`, `/openapi.json`, `/openapi.yaml`, `/redoc` endpoints. Honors per-importer `@Rest(apiFormat=...)` so `apiFormat="openapi"` shifts the canonical doc to `/openapi/*` while `apiFormat="both"` keeps both sets live.
- `?Swagger` / `?OpenApi` query mirrors continue to work from the existing `BasicGroupOperations` interface (mixin only adds the dedicated paths, doesn't remove the query overload).
- Schema generation reuses the existing `OpenApiProvider` / `SwaggerProvider` infrastructure — no new schema code paths.
- `BasicRestServlet` / `BasicRestObject` / `BasicGroupOperations` updated to lean on the mixin internally so the user-visible endpoints don't change but duplication is removed (preserves back-compat).
- Tests in `juneau-utest`: mount-as-mixin, mount-as-standalone-via-paths, query-mirror still works, format pinning works, version-pinned mounts work.

**Explicitly out of scope (v1):**

- Custom OpenAPI extension fields (`x-foo` namespacing helpers) — separate concern, separate TODO if requested.
- New schema dialects beyond Swagger v2 + OpenAPI 3.0 + 3.1 (already supported in core).
- Redoc / Swagger-UI theme customization beyond the existing `HtmlDocConfig` surface.
- Generation of API docs for `@RestStartCall` / `@RestEndCall` / filter beans — only `@RestOp` methods.

## Dependency-injection notes

- **Mixin instance resolution.** `BasicApiDocsResource` is instantiated via the FINISHED-72 mixin walk: the importer's bean store is queried via `BeanStore.getBean(BasicApiDocsResource.class)` first, and if no bean is registered, the framework reflects a no-arg constructor. Both microservice (`BasicBeanStore`) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths use this lookup verbatim — no new plumbing required.
- **Builder-time configuration sourcing.** The mixin reads two builder-time inputs — the `apiFormat` value and the active `OpenApiProvider`/`SwaggerProvider`:
    - `apiFormat` is annotation-only on the importer's `@Rest` (resolved via `RestContext.getApiFormat()`); identical under both DI paths.
    - `OpenApiProvider` / `SwaggerProvider` are looked up from the bean store (`BeanStore.getBean(OpenApiProvider.class)` etc.). Microservice users register them via `@Bean OpenApiProvider provider() { ... }` on the importer; Spring Boot users register `@Bean OpenApiProvider provider()` in a `@Configuration` and the `SpringBeanStore` adapter exposes them. The mixin must NOT cache the provider in a static — both paths rely on per-`RestContext` lookup.
- **Spring-Boot-specific gotchas.**
    - When a Spring Boot application has multiple `OpenApiProvider` candidates (rare, but possible if a user registers both a default and a custom provider), Spring's `@Primary`/`@Qualifier` semantics flow through `SpringBeanStore.getBean(OpenApiProvider.class)` correctly because the adapter delegates to `ApplicationContext.getBeanProvider(...).getIfAvailable()`. Document the precedence so users who hit the multi-provider case know to mark one `@Primary`.
    - Classpath resource resolution for any embedded HTML/JS assets the Redoc / Swagger-UI handlers serve (`HtmlDocConfig`-driven) must use the importer's classloader, not the `BasicApiDocsResource` classloader. The existing `ResourceSupplier` lookup handles this; the mixin must use `getContext().getResourceSupplier()` rather than `getClass().getClassLoader()`.
- **Acceptance bullet** added below: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Phased steps

### Phase 0 — confirm seams (read-only)

1. `BasicGroupOperations` interface — confirm the existing `?Swagger` / `?OpenApi` query-mirror endpoints. Inspect `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/BasicGroupOperations.java`.
2. `BasicRestServlet.getHtdoc(...)` and the static-file integration that backs Swagger UI / Redoc — confirm it doesn't conflict with TODO-75's static-files mixin.
3. `OpenApiProvider` / `SwaggerProvider` `Void.class` defaults — confirm the bean-store fallback resolution chain works the same way it does for `BasicRestServlet`.
4. `RestContext.getApiFormat()` resolution — confirm the system-property + annotation precedence is unchanged so the mixin sees the right format.

### Phase 1 — `BasicApiDocsResource` extraction

1. New class `org.apache.juneau.rest.docs.BasicApiDocsResource` with the six default paths and the `@RestOp` methods.
2. Move the `?Swagger` / `?OpenApi` matchers into the docs package as static inner classes (or reference the existing ones in `BasicGroupOperations` directly).
3. Update `BasicRestServlet` / `BasicRestObject` / `BasicGroupOperations` to lean on the mixin via `@Rest(mixins=BasicApiDocsResource.class)` so the user-visible endpoints remain identical, but the mounting code lives in one place.
4. Tests:
    - `BasicApiDocsResource_AsMixin_Test` — mount on a vanilla `RestServlet` via `@Rest(mixins=...)`, assert all six endpoints serve identical content to a `BasicRestServlet` subclass.
    - `BasicApiDocsResource_Standalone_Test` — mount via `paths={"/api","/swagger",...}` directly, no importer, verify endpoints under each path.
    - `BasicApiDocsResource_QueryMirror_Test` — `?Swagger` / `?OpenApi` continue to overload `GET /` on a parent group resource.
    - `BasicApiDocsResource_FormatPinning_Test` — `apiFormat="openapi"` returns 404 on `/swagger` but 200 on `/openapi/*`; `apiFormat="swagger"` is the inverse; `apiFormat="both"` serves both.

### Phase 2 — versioned mounts

1. Confirm version-pinned mounts via `paths={"/openapi/v3.0","/openapi/v3.1"}` work without code change (FINISHED-72 multi-mount handles this).
2. Tests:
    - `BasicApiDocsResource_VersionPinned_Test` — `/openapi/v3.0` returns 3.0 spec; `/openapi/v3.1` returns 3.1 spec on the same resource via paired mixin instances or `apiFormat` overrides.

### Phase 3 — Spring Boot smoke test

1. New test in `juneau-rest/juneau-rest-server-springboot` test sources.
2. Tests:
    - `BasicApiDocsResource_Springboot_Test` — register `BasicApiDocsResource` as a Spring `@Bean`, mount it via `JuneauRestInitializer`, verify identical content to the microservice mount; also exercise the multi-`OpenApiProvider` Spring case (two `@Bean OpenApiProvider`s, one `@Primary`).

### Phase 4 — docs + release notes

1. Release-notes entry under `### juneau-rest-server` (new mixin) and a cross-reference under `### juneau-rest-server-springboot`.
2. New sub-section in `docs/pages/topics/BasicRestServletSwagger.md` titled "Using `BasicApiDocsResource` as a mixin"; link from the `RestServerComposition.md` topic.

## Acceptance criteria

- [ ] Mounting `@Rest(mixins=BasicApiDocsResource.class)` on a vanilla `RestServlet` produces identical `/openapi.json` output to a `BasicRestServlet` subclass.
- [ ] Standalone mount via `paths={"/api","/swagger","/openapi","/openapi.json","/openapi.yaml","/redoc"}` works without any importer mixin.
- [ ] `apiFormat="openapi"` correctly 404s `/swagger` but serves `/openapi/*`; `apiFormat="both"` serves both surfaces.
- [ ] `?Swagger` / `?OpenApi` query mirrors on parent group resources continue to work after the extraction.
- [ ] Version-pinned mounts (`/openapi/v3.0`, `/openapi/v3.1`) coexist on the same resource.
- [ ] No regression in `BasicRestServlet` / `BasicRestObject` user-visible endpoints (full backwards compatibility on the existing surface).
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Coverage ≥ 95% on `BasicApiDocsResource`. Full `./scripts/test.py` green.

## Open questions

1. **Version-pinned mounts (`/openapi/v3.0`, `/openapi/v3.1`) — ship now or defer?** Cheap to ship with FINISHED-72's multi-mount + `apiFormat="both"`. **Recommend ship now** — the FINISHED-63 OpenAPI 3.1 work already produced the dual-format artifacts.
2. **Redoc default theme.** Match Juneau's existing HTML stylesheet conventions (the same approach Swagger-UI uses today via `HtmlDocConfig`)? **Recommend yes** — mirror the Swagger-UI Juneau theme story for visual consistency.
3. **Default mount paths for `apiFormat="openapi"`.** Should the default `paths` list still include `/swagger` (which would 404), or should it adapt at resolution time to drop the irrelevant entry? **Recommend keep static paths**, let `apiFormat` 404 the irrelevant ones — mount-time path resolution that depends on `apiFormat` would couple two unrelated annotation members.
4. **`BasicGroupOperations.getChildrenSwagger(...)` / `getChildrenOpenApi(...)` — kept on the interface or moved to the mixin?** **Recommend keep on the interface** — the query-mirror behavior is tied to `GET /` and is a different routing seam than the dedicated paths.
5. **Naming.** `BasicApiDocsResource` (recommended) vs `BasicApiResource` vs `BasicSwaggerResource`. **Recommend `BasicApiDocsResource`** — neutral across Swagger/OpenAPI, makes the "this is the docs surface" intent clear in import statements.

## Risks

- **Subtle regression in the existing `BasicRestServlet` chain.** Refactoring `BasicGroupOperations` to lean on the mixin must not change which `@RestOp` method matches a given URL. Mitigation: keep the existing tests passing without modification, then add the new mixin-mode tests.
- **`apiFormat` × `paths` cross-product.** Five `apiFormat` values × N possible `paths` overrides explodes the matrix. Mitigation: parameterize the existing `BasicRestServlet_*` tests to also run via the mixin path; rely on `apiFormat`'s existing 404 behavior to keep the matrix tractable.
- **Static-asset duplication with TODO-75.** Swagger-UI and Redoc ship JS/CSS assets via the static-files plumbing; if TODO-75's `BasicStaticFilesResource` lands first with a path collision (e.g. `/static/swagger-ui.js`), the importer-wins rule means a user who mixes both could shadow the docs assets. Mitigation: documentation note plus a `BasicApiDocsResource_StaticFilesCoexistence_Test`.
- **Spring `@Primary` ambiguity for multiple `OpenApiProvider` beans.** Document; the `SpringBeanStore` adapter delegates to `getBeanProvider(...).getIfAvailable()` which honors `@Primary` correctly — but a user with two unmarked beans gets `BeanDefinitionOverrideException` from Spring, not a Juneau-friendly error.

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives this mixin builds on.
- `todo/FINISHED-63-openapi-3.1-emission.md` — the OpenAPI 3.1 emission this mixin exposes via `/openapi/*`.
- `todo/TODO-73-rest-paths-runtime-override.md` (sibling, soft dependency) — runtime override of `paths` lets users move `/api` to `/admin/api` without subclassing.
- `todo/TODO-75-mixin-static-files.md` (sibling) — coexistence testing for the static-asset overlap with Swagger-UI / Redoc.
- `todo/TODO-77-mixin-ops-introspection.md` (sibling) — the route-index mixin overlaps in spirit with `/api`'s navigation surface; document that they're complementary, not competing.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter; smoke test target for Phase 3.
- `juneau-microservice/` and the `BeanStore` walk in `RestContext` — microservice-path equivalent that the same mixin runs against by default.
- Existing: `BasicGroupOperations` (`juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/BasicGroupOperations.java`) — source of truth for the existing endpoints being extracted.
