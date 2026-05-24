# TODO-74: API-docs mixin pack (Swagger, Swagger-UI, OpenAPI, Redoc)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23. Redesigned 2026-05-24 from a single `BasicApiDocsResource` to a four-class mixin pack, after deciding to retire `BasicRestOperations` / `BasicGroupOperations` entirely as the mixin family lands.

## Goal

Replace the current `BasicRestOperations.getSwagger(...)` / `getOpenApi(...)` and `BasicGroupOperations.getChildrenSwagger(...)` / `getChildrenOpenApi(...)` endpoints with **four sibling mixins** in a new `org.apache.juneau.rest.docs` package. Compose-by-name replaces the `apiFormat` string knob: users pick the mixins they want, and the URL surface follows.

| Mixin | Default `paths` | Owns | Content negotiation |
|---|---|---|---|
| `BasicSwaggerResource` | `/api` | Swagger v2 spec emission via `Swagger` bean + `SwaggerUI` swap | Juneau's standard content negotiation (HTML when `Accept: text/html`; serializer default otherwise). |
| `BasicSwaggerUiResource` | `/swagger` | Same Swagger v2 spec, mounted at an HTML-first URL. Declares `@Rest(mixins=BasicSwaggerResource.class)` so transitive resolution brings `/api` along for free. | **No `Accept` header → HTML.** Otherwise honor `Accept` normally. |
| `BasicOpenApiResource` | `/openapi`, `/openapi.json`, `/openapi.yaml` | OpenAPI 3.1 spec emission via `OpenApi` bean + `RedocUI` swap. The `.json` and `.yaml` paths are **format-pinned** (ignore `Accept`). | `/openapi` uses Juneau's standard content negotiation; `/openapi.json` and `/openapi.yaml` force their respective formats. |
| `BasicRedocResource` | `/redoc` | Same OpenAPI 3.1 spec, mounted at an HTML-first URL. Declares `@Rest(mixins=BasicOpenApiResource.class)` so transitive resolution brings `/openapi/*` along for free. | **No `Accept` header → HTML.** Otherwise honor `Accept` normally. |

End-state developer experience:

```java
// I want the OpenAPI 3.1 spec only (no Swagger v2, no UI mounts).
@Rest(path="/api", mixins=BasicOpenApiResource.class)
public class ApiResource extends RestServlet {
    @RestGet("/items") public List<Item> items() { ... }
    // Now also serves /api/openapi, /api/openapi.json, /api/openapi.yaml
}

// I want OpenAPI 3.1 plus the Redoc UI mount.
@Rest(path="/api", mixins=BasicRedocResource.class)   // pulls in BasicOpenApiResource transitively
public class ApiResource extends RestServlet { ... }
// Serves: /api/openapi, /api/openapi.json, /api/openapi.yaml, /api/redoc

// I want everything (legacy BasicRestServlet behavior, minus the apiFormat knob).
@Rest(path="/api", mixins={BasicSwaggerUiResource.class, BasicRedocResource.class})
public class ApiResource extends RestServlet { ... }
// Serves: /api/api, /api/swagger, /api/openapi, /api/openapi.json, /api/openapi.yaml, /api/redoc
// (Transitive resolution dedupes BasicSwaggerResource and BasicOpenApiResource by class identity.)

// Standalone OpenAPI-only deployment via multi-mount.
@Rest(paths={"/openapi","/openapi.json","/openapi.yaml"})
public class OpenApiDocs extends BasicOpenApiResource { }
```

## Why now

- FINISHED-72 added `@Rest(mixins=...)` and `@Rest(paths=...)` precisely so single-purpose servlet bundles like this could be composed in.
- Today every Juneau service that wants Swagger UI / Redoc / OpenAPI 3.1 must extend the `BasicRestServlet` chain and live with the `apiFormat` knob that gates two endpoints in one class. The FINISHED-63 OpenAPI 3.1 work and the existing `apiFormat` knob already produce the artifacts; only the mounting surface needs lifting.
- Four small mixins replace one larger one: cleaner SRP, easier deprecation path when Swagger v2 finally ages out, and `apiFormat` retires at the mixin level (composition expresses the choice).
- First concrete step in the dismantling of `BasicRestOperations` — TODO-75/76/77 absorb the remaining four endpoints; this TODO removes `getSwagger(...)` and `getOpenApi(...)`.
- Pairs naturally with TODO-73 (runtime-overridable paths) — apps with prefixed deployments (`/admin/api`, `/internal/api`) get the override hook so they don't have to subclass.

## Scope

**In scope (v1):**

- New package `org.apache.juneau.rest.docs` containing four classes:
    - `BasicSwaggerResource` — `@Rest(paths={"/api"})` + `@RestGet("/*")` returning `Swagger` bean with `@MarshalledConfig(swaps={SwaggerUI.class})`.
    - `BasicSwaggerUiResource` — `@Rest(paths={"/swagger"}, mixins=BasicSwaggerResource.class)` + `@RestGet("/*")` returning `Swagger` bean with the same swap. Adds the no-`Accept`-defaults-to-HTML content-negotiation tweak.
    - `BasicOpenApiResource` — `@Rest(paths={"/openapi","/openapi.json","/openapi.yaml"})` + three `@RestGet` methods: `/*` (content-negotiated), `/openapi.json` (forced JSON), `/openapi.yaml` (forced YAML).
    - `BasicRedocResource` — `@Rest(paths={"/redoc"}, mixins=BasicOpenApiResource.class)` + `@RestGet("/*")` returning `OpenApi` bean with `@MarshalledConfig(swaps={RedocUI.class})`. Adds the no-`Accept`-defaults-to-HTML tweak.
- Schema generation reuses the existing `OpenApiProvider` / `SwaggerProvider` infrastructure — no new schema code paths.
- **Breaking change (intentional, called out in release notes):**
    - `BasicRestServlet` / `BasicRestObject` migrate to `@Rest(mixins={BasicSwaggerUiResource.class, BasicRedocResource.class})`. Both endpoint families always mount (no `apiFormat` gate).
    - `@Rest(apiFormat=...)` annotation member, `RestContext.getApiFormat()`, `juneau.rest.apiFormat` system property, and the `API_FORMAT_*` constants are **removed**.
    - `BasicGroupOperations.getChildrenSwagger(...)` / `getChildrenOpenApi(...)` (the `?Swagger` / `?OpenApi` query-mirror endpoints) and the `HasSwaggerQueryParam` / `HasOpenApiQueryParam` matchers are **removed**. The new mixins always mount `/api`, `/swagger`, `/openapi`, `/redoc` so the query-mirror overload of `GET /` is no longer needed.
    - `BasicRestOperations` loses `getSwagger(...)` and `getOpenApi(...)`. The four remaining methods (`error`, `getFavIcon`, `getHtdoc`, `getStats`) stay until TODO-75/76/77 absorb them.
- **No-`Accept`-defaults-to-HTML content-negotiation tweak.** A new `@RestOp` arg resolver — or, more simply, a request-handler hook inside `BasicSwaggerUiResource` / `BasicRedocResource` — inspects `req.getHeader("Accept")`. When absent, the handler sets `Accept: text/html` before content negotiation runs so the SwaggerUI / RedocUI swap fires. When present, behavior is unchanged.
- Tests in `juneau-utest`: mount-as-mixin, mount-as-standalone-via-paths, transitive resolution from UI mixin pulls in spec mixin, format-pinned paths, no-Accept-defaults-to-HTML, content negotiation honored when Accept is present, parent-chain mixin aggregation (see section below).
- **Parent-chain mixin aggregation** — verify-and-document deliverable: confirm the existing FINISHED-72 framework behavior works for the new mixins, add explicit test coverage, document in the topic page. Cross-cutting concern landed once here, applies to all subsequent mixins (TODO-75–78).

**Explicitly out of scope (v1):**

- Custom OpenAPI extension fields (`x-foo` namespacing helpers) — separate concern, separate TODO if requested.
- New schema dialects beyond Swagger v2 + OpenAPI 3.0 + 3.1 (already supported in core).
- Redoc / Swagger-UI theme customization beyond the existing `HtmlDocConfig` surface.
- Generation of API docs for `@RestStartCall` / `@RestEndCall` / filter beans — only `@RestOp` methods.
- The remaining `BasicRestOperations` methods (`error`, `getFavIcon`, `getHtdoc`, `getStats`) and `BasicGroupOperations.getChildren(...)` — absorbed by TODO-75/76/77.

## Migration / breaking changes for 9.5.0

This TODO is explicitly a breaking change. The release-notes entry must cover:

| Removed / changed | Replacement |
|---|---|
| `@Rest(apiFormat="swagger" \| "openapi" \| "both")` | Remove the annotation; pick mixins instead. Both `/api` and `/openapi` always mount when both mixins are present. |
| `juneau.rest.apiFormat` system property | Removed. No replacement — apps that toggled this at runtime should pick mixins at compile time. |
| `RestContext.getApiFormat()` | Removed. No replacement. |
| `RestServerConstants.API_FORMAT_SWAGGER / _OPENAPI / _BOTH / SYSPROP_apiFormat / PROPERTY_apiFormat` | Removed. |
| `BasicGroupOperations.getChildrenSwagger(...)` / `getChildrenOpenApi(...)` | Removed. Use `/api` / `/openapi` (or `/swagger` / `/redoc`) directly — they're always mounted. |
| `BasicGroupOperations.HasSwaggerQueryParam` / `HasOpenApiQueryParam` matcher classes | Removed (no consumers left). |
| `BasicRestOperations.getSwagger(...)` / `getOpenApi(...)` | Removed from the interface. `BasicRestServlet` / `BasicRestObject` use the new mixins instead. |
| `/api?Swagger=true` / `/?Swagger=true` query mirror | Removed. Hit `/api` or `/swagger` directly. |
| `/?OpenApi=true` query mirror | Removed. Hit `/openapi` or `/redoc` directly. |

Apps that subclass `BasicRestServlet` / `BasicRestObject` get the new behavior automatically. Apps that depend on `apiFormat="openapi"` to 404 `/api` need to drop one of the two mixins instead:

```java
// Old (9.4.x):
@Rest(apiFormat="openapi")
public class MyApi extends BasicRestServlet { ... }

// New (9.5.0): pick only the OpenAPI mixin.
@Rest(mixins=BasicRedocResource.class, noInherit={"mixins"})   // noInherit cuts off BasicRestServlet's mixins
public class MyApi extends BasicRestServlet { ... }
// Or extend RestServlet directly and pick what you want:
@Rest(mixins=BasicRedocResource.class)
public class MyApi extends RestServlet { ... }
```

## Dependency-injection notes

- **Mixin instance resolution.** Each of the four mixins is instantiated via the FINISHED-72 mixin walk: the importer's bean store is queried via `BeanStore.getBean(<MixinClass>.class)` first, and if no bean is registered, the framework reflects a no-arg constructor. Both microservice (`BasicBeanStore`) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths use this lookup verbatim — no new plumbing required.
- **Transitive resolution.** `BasicSwaggerUiResource` declares `@Rest(mixins=BasicSwaggerResource.class)`; the framework's `collectRestMixins(...)` walk (RestContext.java:1455) already handles transitive resolution and dedupes by class identity via `LinkedHashSet`. So `mixins=BasicSwaggerUiResource.class` alone brings both mixin classes in; `mixins={BasicSwaggerResource.class, BasicSwaggerUiResource.class}` is also valid (no double-registration because `LinkedHashSet` dedupes). Same for `BasicRedocResource` → `BasicOpenApiResource`.
- **Builder-time configuration sourcing.** The mixins read `OpenApiProvider` / `SwaggerProvider` from the bean store (`BeanStore.getBean(OpenApiProvider.class)` etc.). Microservice users register them via `@Bean OpenApiProvider provider() { ... }` on the importer; Spring Boot users register `@Bean OpenApiProvider provider()` in a `@Configuration` and the `SpringBeanStore` adapter exposes them. The mixins must NOT cache the provider in a static — both paths rely on per-`RestContext` lookup.
- **Spring-Boot-specific gotchas.**
    - When a Spring Boot application has multiple `OpenApiProvider` candidates (rare, but possible if a user registers both a default and a custom provider), Spring's `@Primary`/`@Qualifier` semantics flow through `SpringBeanStore.getBean(OpenApiProvider.class)` correctly because the adapter delegates to `ApplicationContext.getBeanProvider(...).getIfAvailable()`. Document the precedence so users who hit the multi-provider case know to mark one `@Primary`.
    - Classpath resource resolution for any embedded HTML/JS assets the Redoc / Swagger-UI handlers serve (`HtmlDocConfig`-driven) must use the importer's classloader, not the mixin's classloader. The existing `ResourceSupplier` lookup handles this; the mixins must use `getContext().getResourceSupplier()` rather than `getClass().getClassLoader()`.
- **Acceptance bullet** added below: "Each mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Parent-chain mixin aggregation

**Status: framework already supports it (FINISHED-72), needs verification + test coverage + documentation.** Folded into TODO-74 as the natural place to validate the pattern end-to-end since this is the first concrete mixin family landing post-FINISHED-72.

### Existing framework behavior

`@Rest` is `@Inherited` ([Rest.java:59](juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/annotation/Rest.java)). The mixin walk uses Juneau's standard property-aggregator `RestContext.getRestAnnotationsForProperty(PROPERTY_mixins)` which returns every `@Rest` annotation in the resource class hierarchy in **parent-to-child** order. The aggregator collects each ancestor's `@Rest(mixins=...)` into a `LinkedHashSet` (insertion-ordered, deduped by class identity), with transitive mixin resolution running orthogonally via `collectRestMixins(...)`.

End result — this works today:

```java
@Rest(mixins=BasicRedocResource.class)
public abstract class CommonApi extends RestServlet { }

@Rest(mixins={SomeOtherMixin.class})  // appended to parent's
public class TenantApi extends CommonApi { }

// TenantApi resolves to: [BasicOpenApiResource, BasicRedocResource, SomeOtherMixin]
// (parent-first, child-appended; LinkedHashSet preserves order, dedupes by class identity;
//  BasicOpenApiResource pulled in transitively from BasicRedocResource)
```

Existing escape hatch: `@Rest(noInherit={"mixins"})` cuts off inheritance for the `mixins` property, so a child can declare its own mixin set without inheriting. This same `noInherit` mechanism already exists for other properties (`converters`, `guards`, etc.) and just works for `mixins` by name.

### Deliverables (folded into Phase 3 + Phase 5)

1. **New test `BasicApiDocs_ParentChainMixin_Test`** in Phase 3 (added to the test list below). Cases:
   - Parent class declares `@Rest(mixins=BasicRedocResource.class)`; child extends parent with no `@Rest` — child inherits the mixin (Java `@Inherited` semantics).
   - Both parent and child declare `@Rest(mixins=...)` with different mixin classes — child resolves to the union, parent-first ordering preserved.
   - Both parent and child declare the same mixin (`BasicSwaggerResource.class`) — `LinkedHashSet` dedupes; mixin instance is constructed once.
   - Three-deep chain: `Grandparent → Parent → Child`, each with distinct `@Rest(mixins=...)`. Child resolves to the full union in `[grandparent, parent, child]` order.
   - **Opt-out:** child declares `@Rest(noInherit={"mixins"}, mixins=...)` — parent's mixins are NOT inherited; child sees only its own.
   - **Transitive resolution:** parent declares `BasicRedocResource`; child inherits the parent's mixins; framework still walks `BasicRedocResource`'s own `@Rest(mixins=BasicOpenApiResource.class)` so `BasicOpenApiResource` is also resolved.

2. **Topic page documentation** in Phase 5: append a "Parent-chain mixin aggregation" sub-section to `docs/pages/topics/10.07a.RestServerComposition.md` covering:
   - The aggregation behavior (parent-first, child-appended, LinkedHashSet dedupe).
   - The `noInherit={"mixins"}` opt-out mechanism.
   - The interaction with transitive mixin resolution (orthogonal — both apply).
   - A worked example using the four api-docs mixins on a `CommonApi` parent class.

3. **Acceptance criterion:** added below.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. `BasicRestOperations.getSwagger(...)` and `getOpenApi(...)` — confirm the signatures and `@HtmlDocConfig` / `@MarshalledConfig` decorations being lifted. Inspect [BasicRestOperations.java:160-235](juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/BasicRestOperations.java).
2. `BasicGroupOperations.getChildrenSwagger(...)` / `getChildrenOpenApi(...)` and the `HasSwaggerQueryParam` / `HasOpenApiQueryParam` matcher classes — confirm no external consumers before removal. Inspect [BasicGroupOperations.java](juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/BasicGroupOperations.java).
3. `BasicRestServlet.getHtdoc(...)` and the static-file integration that backs Swagger UI / Redoc — confirm it doesn't conflict with TODO-75's static-files mixin.
4. `OpenApiProvider` / `SwaggerProvider` `Void.class` defaults — confirm the bean-store fallback resolution chain works the same way it does for `BasicRestServlet`.
5. `@Rest(apiFormat=...)` annotation member, `RestContext.getApiFormat()`, and all `API_FORMAT_*` constants in `RestServerConstants` — confirm the exact removal surface.
6. **Find all callers of `getApiFormat()` and `apiFormat` references** across the codebase (production, tests, examples) so the removal is comprehensive.

### Phase 1 — `BasicSwaggerResource` + `BasicSwaggerUiResource`

1. New package `org.apache.juneau.rest.docs`. New class `BasicSwaggerResource` with `@Rest(paths={"/api"})` and a single `@RestGet("/*")` method returning `Swagger` (lifted from `BasicRestOperations.getSwagger`, minus the `apiFormat` gate).
2. New class `BasicSwaggerUiResource` with `@Rest(paths={"/swagger"}, mixins=BasicSwaggerResource.class)`. Declares its own `@RestGet("/*")` method returning `Swagger` — OR (alternative design) declares no methods and relies on transitive resolution from `BasicSwaggerResource` to mount the spec endpoint at `/swagger/*` automatically. **Decision: declare the method explicitly** so the no-`Accept`-defaults-to-HTML behavior can be attached to this specific endpoint without affecting the `/api` mount.
3. Implement the no-`Accept`-defaults-to-HTML behavior. Options:
    - (a) `@RestStartCall`-style hook on `BasicSwaggerUiResource` that mutates the request's `Accept` header before content negotiation.
    - (b) Custom `RestOpArg` that does the same on a per-method basis.
    - (c) Override `@RestGet(produces="text/html")` on the UI mixin's method (forces HTML and short-circuits content negotiation entirely — but breaks JSON-when-Accept-is-set).
    - **Recommended: (a)** — clean, reusable across `BasicSwaggerUiResource` and `BasicRedocResource`, doesn't break content negotiation when Accept is set.
4. Tests:
    - `BasicSwaggerResource_AsMixin_Test` — mount via `@Rest(mixins=BasicSwaggerResource.class)` on vanilla `RestServlet`. Assert `/api` serves the Swagger bean. With `Accept: text/html` returns HTML; with `Accept: application/json` returns JSON; with no Accept returns the serializer default (current Juneau behavior, no tweak on this mixin).
    - `BasicSwaggerResource_Standalone_Test` — `@Rest(paths={"/api"}) public class Foo extends BasicSwaggerResource { }` mounted directly.
    - `BasicSwaggerUiResource_AsMixin_Test` — mount via `@Rest(mixins=BasicSwaggerUiResource.class)`. Transitive resolution brings in `BasicSwaggerResource`. Verify BOTH `/api` and `/swagger` serve the spec. With no `Accept` header, `/swagger` returns HTML; `/api` returns the serializer default. With `Accept: application/json`, both return JSON.

### Phase 2 — `BasicOpenApiResource` + `BasicRedocResource`

1. New class `BasicOpenApiResource` with `@Rest(paths={"/openapi","/openapi.json","/openapi.yaml"})` and three `@RestGet` methods:
    - `@RestGet("/*")` — content-negotiated, returns `OpenApi` bean with `RedocUI` swap for HTML. Lifted from `BasicRestOperations.getOpenApi`.
    - `@RestGet(path="/openapi.json", produces="application/json")` — JSON-pinned.
    - `@RestGet(path="/openapi.yaml", produces="application/yaml")` — YAML-pinned (assumes YAML serializer is registered; gracefully 406s if not).
2. New class `BasicRedocResource` with `@Rest(paths={"/redoc"}, mixins=BasicOpenApiResource.class)`. Same `@RestGet("/*")` method returning `OpenApi` bean + `RedocUI` swap. Same no-`Accept`-defaults-to-HTML hook as `BasicSwaggerUiResource`.
3. Tests:
    - `BasicOpenApiResource_AsMixin_Test` — mount via `@Rest(mixins=BasicOpenApiResource.class)`. Assert `/openapi`, `/openapi.json`, `/openapi.yaml` all serve the spec correctly. JSON/YAML pins ignore `Accept` header.
    - `BasicOpenApiResource_Standalone_Test` — `@Rest(paths={"/openapi","/openapi.json","/openapi.yaml"}) public class Foo extends BasicOpenApiResource { }`.
    - `BasicRedocResource_AsMixin_Test` — mount via `@Rest(mixins=BasicRedocResource.class)`. Transitive resolution brings in `BasicOpenApiResource`. All four URLs (`/openapi`, `/openapi.json`, `/openapi.yaml`, `/redoc`) work. With no Accept, `/redoc` returns HTML; `/openapi` returns serializer default. With `Accept: application/json`, both return JSON.
    - `BasicOpenApiResource_FormatPins_Test` — `Accept: text/html` on `/openapi.json` still returns JSON (path overrides Accept).

### Phase 3 — composition + parent-chain aggregation

1. Tests:
    - `BasicApiDocs_BothMixins_Test` — `@Rest(mixins={BasicSwaggerUiResource.class, BasicRedocResource.class})`. Verify all six URLs (`/api`, `/swagger`, `/openapi`, `/openapi.json`, `/openapi.yaml`, `/redoc`) work. Transitive resolution dedupes — only four mixin instances are constructed.
    - `BasicApiDocs_TransitiveDedupe_Test` — `@Rest(mixins={BasicSwaggerResource.class, BasicSwaggerUiResource.class})` (explicit + transitive). Verify `BasicSwaggerResource` is instantiated exactly once and `/api` is registered exactly once (no path collision).
    - `BasicApiDocs_ParentChainMixin_Test` — see "Parent-chain mixin aggregation" section above for the full case list (Java `@Inherited`, parent+child union, dedupe, three-deep chain, `noInherit` opt-out, transitive-resolution interaction).
    - `BasicApiDocs_NoAcceptDefaultsToHtml_Test` — for `BasicSwaggerUiResource` and `BasicRedocResource`: `curl` with no Accept returns HTML; `curl -H 'Accept: application/json'` returns JSON; `curl -H 'Accept: */*'` returns HTML (browsers send `*/*` as a tail; HTML default kicks in for the UI mixins).

### Phase 4 — retire `BasicRestOperations.getSwagger` / `getOpenApi`, `apiFormat`, query mirrors

1. Update `BasicRestServlet` / `BasicRestObject` to declare `@Rest(mixins={BasicSwaggerUiResource.class, BasicRedocResource.class})`. Existing endpoints remain at `/api` and `/openapi` (now via the mixins); add `/swagger` and `/redoc` (new); add `/openapi.json` and `/openapi.yaml` (new).
2. Remove `getSwagger(RestRequest)` and `getOpenApi(RestRequest)` from `BasicRestOperations`. The interface is left with `error()`, `getFavIcon()`, `getHtdoc(...)`, `getStats(...)` — to be absorbed by TODO-75/76/77.
3. Remove `getChildrenSwagger(...)` and `getChildrenOpenApi(...)` from `BasicGroupOperations`. Remove the `HasSwaggerQueryParam` and `HasOpenApiQueryParam` matcher classes. The interface is left with `getChildren(RestRequest)` — to be absorbed by TODO-77's `BasicRouteIndexResource` (or a new sibling `BasicNavigationResource` if `getChildren` doesn't fit).
4. Remove `Rest.apiFormat()` annotation member, `RestContext.getApiFormat()` method, `RestServerConstants.API_FORMAT_SWAGGER` / `_OPENAPI` / `_BOTH` / `SYSPROP_apiFormat` / `PROPERTY_apiFormat` constants.
5. Update `juneau-examples-rest-jetty` / `juneau-examples-rest-springboot` if any example exercises `apiFormat`.
6. Update / delete tests that exercise `apiFormat` (move the format-selection test surface to the new compositional mixin tests).
7. Tests:
    - `BasicRestServlet_NewMixinDefaults_Test` — verify `BasicRestServlet` subclasses now serve all six docs URLs by default.
    - `ApiFormatRemoval_Test` (or just: verify the existing test that asserted `apiFormat` behavior is now deleted or rewritten).

### Phase 5 — Spring Boot smoke test

1. New test in `juneau-rest/juneau-rest-server-springboot` test sources.
2. Tests:
    - `BasicApiDocs_Springboot_Test` — register all four mixins as Spring `@Bean`s, mount them via `JuneauRestInitializer`, verify identical content to the microservice mount; also exercise the multi-`OpenApiProvider` Spring case (two `@Bean OpenApiProvider`s, one `@Primary`).

### Phase 6 — docs + release notes

1. Release-notes entry under `### juneau-rest-server` covering:
    - New mixin pack (four classes, package `org.apache.juneau.rest.docs`).
    - **BREAKING:** `@Rest(apiFormat=...)` removed. Migration guide (see "Migration / breaking changes" table above).
    - **BREAKING:** `?Swagger` / `?OpenApi` query mirrors removed. Use `/api` / `/swagger` / `/openapi` / `/redoc` directly.
    - **BREAKING:** `BasicRestOperations.getSwagger(...)` / `getOpenApi(...)` removed. `BasicGroupOperations.getChildrenSwagger(...)` / `getChildrenOpenApi(...)` removed.
    - New default endpoints on `BasicRestServlet` / `BasicRestObject`: `/swagger`, `/openapi.json`, `/openapi.yaml`, `/redoc` (in addition to existing `/api` and `/openapi`).
2. Cross-reference release-notes entry under `### juneau-rest-server-springboot`.
3. New topic `docs/pages/topics/ApiDocsMixins.md` (or update `BasicRestServletSwagger.md`) covering the four-mixin composition pattern, the no-`Accept`-defaults-to-HTML behavior, and the format-pinned `/openapi.json` / `/openapi.yaml` paths.
4. New sub-section in `docs/pages/topics/10.07a.RestServerComposition.md` titled "Parent-chain mixin aggregation" covering aggregation order, the `noInherit={"mixins"}` opt-out, and the interaction with transitive mixin resolution (see "Parent-chain mixin aggregation" section above for the full content brief).
5. Migration note in the 9.5.0 release notes top-level summary calling out `apiFormat` removal as one of the headline breaking changes.

## Acceptance criteria

- [ ] Four new public classes in `org.apache.juneau.rest.docs`: `BasicSwaggerResource`, `BasicSwaggerUiResource`, `BasicOpenApiResource`, `BasicRedocResource`.
- [ ] Mounting `@Rest(mixins=BasicOpenApiResource.class)` on a vanilla `RestServlet` produces identical `/openapi` content to today's `BasicRestServlet` subclass at `/openapi`.
- [ ] Mounting `@Rest(mixins=BasicRedocResource.class)` transitively brings in `BasicOpenApiResource` (verified by single-instance construction count); `/openapi`, `/openapi.json`, `/openapi.yaml`, `/redoc` all serve.
- [ ] `@Rest(mixins={BasicSwaggerUiResource.class, BasicRedocResource.class})` serves all six URLs (`/api`, `/swagger`, `/openapi`, `/openapi.json`, `/openapi.yaml`, `/redoc`); `LinkedHashSet` deduping prevents double-registration of the spec mixins.
- [ ] No-`Accept`-defaults-to-HTML behavior: `BasicSwaggerUiResource` and `BasicRedocResource` return HTML when no `Accept` header is present; honor `Accept` when it is set. `BasicSwaggerResource` and `BasicOpenApiResource` (spec mixins) use Juneau's standard content negotiation unchanged.
- [ ] `/openapi.json` returns JSON regardless of `Accept` header; `/openapi.yaml` returns YAML regardless.
- [ ] Standalone mount via `@Rest(paths={...}) public class Foo extends BasicOpenApiResource { }` works without an importer.
- [ ] `@Rest(apiFormat=...)` removed from the codebase; no compilation errors; all `apiFormat`-referencing tests deleted or rewritten.
- [ ] `?Swagger` / `?OpenApi` query mirrors removed; `BasicGroupOperations` no longer declares the matcher inner classes.
- [ ] `BasicRestServlet` and `BasicRestObject` mount both `/swagger` and `/redoc` (and the OpenAPI variants) by default after the migration.
- [ ] Each mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Parent-chain mixin aggregation works for the four mixins: a child `@Rest`-annotated class extending a `@Rest`-annotated parent inherits the parent's `mixins` (Java `@Inherited`); when both declare `mixins`, the child sees the union in parent-first order; `@Rest(noInherit={"mixins"})` opts out cleanly; deduped by class identity. Tests cover all listed cases. Documented in `RestServerComposition.md`.
- [ ] Release notes flag all five breaking changes (annotation member, system property, query mirrors, `BasicRestOperations` methods, `BasicGroupOperations` methods).
- [ ] Coverage ≥ 95% on the four new mixin classes. Full `./scripts/test.py` green.

## Open questions

1. ~~**YAML serializer registration.**~~ **Resolved by FINISHED-81.** `BasicOpenApiResource` declares `@Rest(serializers={YamlSerializer.class})` on the mixin class. Under the sub-`RestContext` model landed in `juneau-rest-server` 9.5.0, this APPENDS to the host's inherited serializer set for mixin endpoints only — `/openapi.yaml` works without polluting the host's content-negotiation surface. Phase 0 still needs to verify which module ships `YamlSerializer` (could create a hard dependency from `juneau-rest-server` onto a YAML-bearing module). See `todo/FINISHED-81-mixin-sub-context-inheritance.md`.
2. **`BasicGroupOperations.getChildren(...)` — keep on the interface, move to a new navigation mixin, or fold into TODO-77's `BasicRouteIndexResource`?** **Recommend: keep on `BasicGroupOperations` for now**; TODO-77 decides whether to absorb it. The `?Swagger` / `?OpenApi` mirrors are gone but `getChildren()` itself (the navigation page) is a distinct concern.
3. **`error()` endpoint placement.** After TODO-75/76/77 absorb `getFavIcon`, `getHtdoc`, `getStats`, what's left in `BasicRestOperations` is just `error()`. Possible homes: (a) tiny `BasicErrorResource` mixin, (b) move into `BasicRestServlet` directly, (c) keep `BasicRestOperations` as a single-method interface for error-only. **Recommend deciding in a follow-on TODO** after TODO-77 lands; not in scope here.
4. **Path-resolution for `/openapi.json` and `/openapi.yaml`.** Today `/openapi/*` is a single wildcard mount. Adding sibling `/openapi.json` and `/openapi.yaml` paths needs them to NOT be captured by the wildcard. Verify Juneau's path-matching precedence (specific path beats wildcard) in Phase 0. If wildcard wins, `BasicOpenApiResource` needs to use `/openapi` (no `/*`) for the wildcard mount and separate methods for the `.json` and `.yaml` paths.
5. **Naming.** `BasicSwaggerResource` / `BasicSwaggerUiResource` vs `BasicSwaggerSpecResource` / `BasicSwaggerUiResource`. **Recommend: `BasicSwaggerResource`** (no `Spec` suffix) — the URL `/api` already implies the spec; adding `Spec` to the class name is redundant. Same reasoning for `BasicOpenApiResource`.

## Risks

- **Hard back-compat break.** `apiFormat` removal will break apps that set it explicitly. Mitigation: clear migration table in release notes, deprecation cycle TBD (one option: 9.5.0 logs `WARN` on `apiFormat` use and removes it in 9.6.0; another: just remove it in 9.5.0 since the user signed off on the break).
- **`?Swagger` / `?OpenApi` query-mirror removal.** Some users might link to `/?Swagger=true` from external docs. Mitigation: release-notes call-out + suggest `/api` (or `/swagger`) as the direct replacement.
- **Subtle regression in `BasicRestServlet` chain.** Refactoring `BasicGroupOperations` and `BasicRestOperations` to lean on the mixins must not change which `@RestOp` method matches a given URL (other than the intentional `apiFormat` removal). Mitigation: keep the existing tests passing without modification (except for `apiFormat`-specific ones), then add the new mixin-mode tests.
- **Static-asset duplication with TODO-75.** Swagger-UI and Redoc ship JS/CSS assets via the static-files plumbing; if TODO-75's `BasicStaticFilesResource` lands first with a path collision (e.g. `/static/swagger-ui.js`), the importer-wins rule means a user who mixes both could shadow the docs assets. Mitigation: documentation note plus a `BasicApiDocs_StaticFilesCoexistence_Test`.
- **Spring `@Primary` ambiguity for multiple `OpenApiProvider` beans.** Document; the `SpringBeanStore` adapter delegates to `getBeanProvider(...).getIfAvailable()` which honors `@Primary` correctly — but a user with two unmarked beans gets `BeanDefinitionOverrideException` from Spring, not a Juneau-friendly error.
- **YAML serializer dependency.** If the YAML serializer is in a separate optional module, `/openapi.yaml` becomes conditionally-available — needs careful documentation and a graceful 406 fallback.

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives this mixin family builds on.
- `todo/FINISHED-63-openapi-3.1-emission.md` — the OpenAPI 3.1 emission this family exposes via `/openapi/*`.
- `todo/TODO-73-rest-paths-runtime-override.md` (sibling, soft dependency) — runtime override of `paths` lets users move `/api` to `/admin/api` without subclassing.
- `todo/FINISHED-81-mixin-sub-context-inheritance.md` (hard dependency — landed in 9.5.0) — sub-`RestContext` per mixin with parent-linked inheritance for serializers/parsers/etc. `BasicOpenApiResource` uses the new model to declare YAML scoped to mixin endpoints only.
- `todo/TODO-75-mixin-static-files.md` (sibling) — coexistence testing for the static-asset overlap with Swagger-UI / Redoc. Continues the dismantling of `BasicRestOperations` (absorbs `getHtdoc`).
- `todo/TODO-76-mixin-convention-endpoints.md` (sibling) — continues the dismantling (absorbs `getFavIcon`).
- `todo/TODO-77-mixin-ops-introspection.md` (sibling) — continues the dismantling (absorbs `getStats`; possibly `getChildren` via `BasicRouteIndexResource`); also note that the route-index mixin overlaps in spirit with `/api`'s navigation surface — they're complementary, not competing.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter; smoke test target for Phase 5.
- `juneau-microservice/` and the `BeanStore` walk in `RestContext` — microservice-path equivalent that the same mixins run against by default.
- Existing: `BasicRestOperations` ([file](juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/BasicRestOperations.java)) and `BasicGroupOperations` ([file](juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/servlet/BasicGroupOperations.java)) — source of truth for the endpoints being extracted (this TODO) and retired (TODO-74 through TODO-77 collectively).

## Dismantling roadmap for `BasicRestOperations` / `BasicGroupOperations`

This TODO is the first concrete step in retiring both interfaces. Cross-TODO map for reviewers:

| `BasicRestOperations` method | Path | New home | TODO |
|---|---|---|---|
| `getSwagger(RestRequest)` | `/api/*` | `BasicSwaggerResource` | **TODO-74 (this)** |
| `getOpenApi(RestRequest)` | `/openapi/*` | `BasicOpenApiResource` | **TODO-74 (this)** |
| `getHtdoc(String, Locale)` | `/htdocs/*` | `BasicStaticFilesResource` | TODO-75 |
| `getFavIcon()` | `/favicon.ico` | `BasicFaviconResource` | TODO-76 |
| `getStats(RestRequest)` | `/stats` | `BasicRouteIndexResource` (or new `BasicStatsResource`) | TODO-77 |
| `error()` | `/error` | TBD — micro-mixin or `BasicRestServlet` | Follow-on TODO |

| `BasicGroupOperations` method | Path | New home | TODO |
|---|---|---|---|
| `getChildrenSwagger(RestRequest)` | `GET /?Swagger` | **REMOVED** (use `/api` or `/swagger`) | **TODO-74 (this)** |
| `getChildrenOpenApi(RestRequest)` | `GET /?OpenApi` | **REMOVED** (use `/openapi` or `/redoc`) | **TODO-74 (this)** |
| `getChildren(RestRequest)` | `GET /` | TBD — `BasicRouteIndexResource` candidate | TODO-77 |

Once all rows are migrated, `BasicRestOperations` and `BasicGroupOperations` are deleted. Tracked by an implicit follow-on TODO created after TODO-77 lands.
