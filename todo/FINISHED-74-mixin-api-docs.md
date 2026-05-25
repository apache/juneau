# FINISHED-74: API-docs mixin pack (Swagger, Swagger-UI, OpenAPI, Redoc)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23. Redesigned 2026-05-24 from a single `BasicApiDocsResource` to a four-class mixin pack, after deciding to retire `BasicRestOperations` / `BasicGroupOperations` entirely as the mixin family lands.

Closed 2026-05-24 after Phases 0–6 completed across two implementation sessions. All acceptance criteria checked (coverage measured at 100% line/branch on the four new `org.apache.juneau.rest.docs` mixin classes). Three-way deployment parity (`MockRest` microservice baseline, real `JettyMicroservice`, real `@SpringBootTest` + embedded Tomcat) is exercised by 16 tests in `juneau-utest/src/test/java/org/apache/juneau/rest/docs/`. The 9.5.0 release notes carry the user-facing breaking-change entries (`apiFormat` removal, `?Swagger`/`?OpenApi` query-mirror removal, `BasicRestOperations`/`BasicGroupOperations` method removals, new default endpoints on `BasicRestServlet`/`BasicRestObject`); the 9.5 migration guide covers the compose-by-class migration path; topic pages `ApiDocsMixins.md` (new), `RestServerComposition.md` (parent-chain aggregation sub-section), and `BasicRestServletSwagger.md` (rewrite) carry the prose. The one carried-over open question — YAML parser buffer underflow on the full `BasicRestServlet` post-migration mixin surface — was promoted to its own follow-on TODO-88 because the root cause is a separate latent parser limitation, not a regression introduced by this work.

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

**Test home: `juneau-utest/src/test/java/org/apache/juneau/rest/docs/`** — same package as the rest of the TODO-74 test suite. Follows the codebase convention (every production module under `juneau-rest/` ships with an empty `src/test/`; all juneau-rest tests live in `juneau-utest`) and the test-architecture template established in TODO-78. `juneau-utest/pom.xml` already pulls in `juneau-rest-server-springboot` and `spring-boot-starter-web` as test deps, so no POM changes needed.

**Test fidelity: real `@SpringBootTest` with embedded Tomcat.** Not Mockito-mocked. The existing Spring Boot tests in `juneau-utest` (`RestPathsRuntimeOverride_Springboot_Test`, `SpringBeanStore_Test`) use mockito-mocked `ApplicationContext` because they're testing **adapter logic** in isolation; Phase 5 is testing **deployment-equivalent behavior**, which means a real Spring context is the right tier. The multi-`OpenApiProvider` `@Primary` case in particular genuinely needs the real Spring resolver — mocking `getBeanProvider(OpenApiProvider.class).getIfAvailable()` to return a specific impl stubs around the `@Primary` machinery rather than exercising it.

**Test classes:**

| Test class | Container | Verifies |
|---|---|---|
| `BasicApiDocs_JettyMicroservice_Test` | Real embedded Jetty via `JettyMicroservice` (test-scope `juneau-microservice-jetty` already on `juneau-utest`) | All four mixins mounted on a `BasicRestServlet` / `BasicRestObject` host under the canonical Juneau microservice path, served over real HTTP. Byte-identical to the `MockRest` baseline at `/api` / `/swagger` / `/openapi` / `/openapi.json` / `/openapi.yaml` / `/redoc`. Catches anything `MockRest` glosses over (real `Accept` negotiation, real wire-format `Content-Type`, real `BasicBeanStore` registration through the microservice lifecycle rather than the simplified `MockRest` mount). |
| `BasicApiDocs_Springboot_Test` | `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` + embedded Tomcat | All four mixins registered as Spring `@Bean`s, mounted via `JuneauRestInitializer`, serve byte-identical content vs. the same baseline + the `JettyMicroservice_Test` results. Confirms three-way deployment parity. |
| `BasicApiDocs_Springboot_MultiOpenApiProvider_Test` | Same | Two `@Bean OpenApiProvider`s in the context, one `@Primary`. `SpringBeanStore.getBean(OpenApiProvider.class)` returns the `@Primary` one; the other is reachable via `@Qualifier`. Two unmarked `@Bean OpenApiProvider`s produce a Spring-side `BeanDefinitionOverrideException` (documented as a known-rough-edge in resolved decisions). |

**Three-way parity assertion.** The `JettyMicroservice` and Spring Boot test classes both compare their wire-output against a shared fixture computed from `MockRest` (the existing `BasicApiDocs_TransitiveDedupe_Test` already produces a byte snapshot we can promote to a shared test resource). This means a regression in any one of the three deployment paths is caught even if the other two still work.

**Why real containers (Jetty + Spring Boot) rather than Mockito-mocked variants:**

1. **Byte-identical assertion.** "Identical content to microservice mount" is only meaningful if each path actually runs the same serializer + content-negotiation pipeline end-to-end — a mock skips most of that. Real HTTP catches `Accept`-header negotiation regressions, `Content-Type` charset drift, and wire-encoding edge cases that `MockRest` alone cannot.
2. **`@Primary` resolution (Spring Boot specifically).** The `SpringBeanStore` adapter delegates to `ApplicationContext.getBeanProvider(...).getIfAvailable()`, which is where `@Primary` is honored. Mocking that call away invalidates the test.
3. **Microservice lifecycle (Jetty specifically).** `JettyMicroservice` registers beans via `BasicBeanStore` through the microservice startup lifecycle (manifest scan, config-driven bean registration, `@Bean` factory methods). `MockRest` simplifies all of that. A real Jetty test catches lifecycle-ordering bugs that `MockRest` cannot surface.
4. **Pattern alignment with TODO-78.** TODO-78's Phase 5 introduces `@SpringBootTest`-style real-container tests for JSP plus a real-Jetty test class. TODO-74's Phase 5 uses the same shape (Jetty + Spring Boot real-container test classes) for the API-docs mixins, so the two TODOs share `juneau-utest` test scaffolding rather than each inventing their own.
5. **Acceptable cost.** One real-Jetty class + two `@SpringBootTest` classes adds ~10-15s of container startup to the full test run, bounded; the existing `juneau-utest` budget absorbs this comfortably. If the `view-*` family + `docs-*` mixins together start to push the budget past pain threshold, the TODO-78 escape hatch (split to `juneau-rest-tests-views` or `juneau-rest-tests-springboot`) covers TODO-74's tests too.

**BeanStore (microservice) parity:** the existing `BasicApiDocs_TransitiveDedupe_Test` and `BasicApiDocs_ParentChain_Test` already exercise the microservice/`BasicBeanStore` path via `MockRest`; Phase 5 only needs to add the Spring side and assert content-parity against the same fixtures.

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

- [x] Four new public classes in `org.apache.juneau.rest.docs`: `BasicSwaggerResource`, `BasicSwaggerUiResource`, `BasicOpenApiResource`, `BasicRedocResource`.
- [x] Mounting `@Rest(mixins=BasicOpenApiResource.class)` on a vanilla `RestServlet` produces `/openapi` content. (Verified by `BasicOpenApiResource_AsMixin_Test`.)
- [x] Mounting `@Rest(mixins=BasicRedocResource.class)` transitively brings in `BasicOpenApiResource` (verified by sub-context map inspection in `BasicRedocResource_AsMixin_Test#a06_subContextsConstructed`); `/openapi`, `/openapi.json`, `/openapi.yaml`, `/redoc` all serve.
- [x] `@Rest(mixins={BasicSwaggerUiResource.class, BasicRedocResource.class})` serves all six URLs (`/api`, `/swagger`, `/openapi`, `/openapi.json`, `/openapi.yaml`, `/redoc`); `LinkedHashSet` deduping prevents double-registration of the spec mixins. (Verified by `BasicApiDocs_TransitiveDedupe_Test`.)
- [x] No-`Accept`-defaults-to-HTML behavior on the UI mixins via `@Rest(defaultAccept="text/html")`. Tested in `BasicSwaggerUiResource_AsMixin_Test` and `BasicRedocResource_AsMixin_Test`.
- [x] `/openapi.json` returns JSON regardless of `Accept` header; `/openapi.yaml` returns YAML regardless. (Implemented via `RestResponse.getDirectWriter(...)`; tested in `BasicOpenApiResource_AsMixin_Test#a02-a05`.)
- [x] Standalone mount: `BasicSwaggerResource_Standalone_Test` covers the `@Rest(mixins=BasicSwaggerResource.class) public class Foo extends RestObject implements BasicUniversalConfig {}` shape.
- [x] `@Rest(apiFormat=...)` removed from the codebase; no compilation errors; all `apiFormat`-referencing tests deleted (`Rest_ApiFormat_Both_Test`, `Rest_ApiFormat_OpenApi_Test`, `Rest_ApiFormat_Resolution_Test`, `Rest_ApiFormat_Swagger_Test`, `Rest_GroupQueryMirrors_Test`) or migrated (`OpenApiYamlRoundTrip_Test`, `OpenApiSchemaReuse_Test`, `Swagger_Test`).
- [x] `?Swagger` / `?OpenApi` query mirrors removed; `BasicGroupOperations` no longer declares the matcher inner classes.
- [x] `BasicRestServlet` and `BasicRestObject` mount both `/swagger` and `/redoc` (and the OpenAPI variants) by default after the migration.
- [x] Each mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test. Real-Jetty parity: `BasicApiDocs_JettyMicroservice_Test`. Real Spring Boot parity: `BasicApiDocs_Springboot_Test` (+ `BasicApiDocs_Springboot_MultiOpenApiProvider_Test` for the `@Primary` / `BeanDefinitionOverrideException` corner cases). All three share a `BasicApiDocsTestFixtures` helper that pins six-URL response-shape assertions, run against `MockRest` baseline + Jetty + Spring Boot.
- [x] Parent-chain mixin aggregation: `BasicApiDocs_ParentChain_Test` covers parent + child union with transitive resolution; remaining cases (three-deep chain, dedupe of identical mixin in parent + child, `noInherit` opt-out) are covered by existing FINISHED-72 / FINISHED-81 tests; Phase 6 docs sub-section landed in `docs/pages/topics/10.07a.RestServerComposition.md`.
- [x] Release notes flag all five breaking changes (annotation member, system property, query mirrors, `BasicRestOperations` methods, `BasicGroupOperations` methods). Updated migration guide and 9.5.0 release-notes top-line summary.
- [x] Coverage ≥ 95% on the four new mixin classes. **100% line + branch across all four** (`BasicSwaggerResource` 9/9, `BasicSwaggerUiResource` 9/9, `BasicOpenApiResource` 39/39, `BasicRedocResource` 9/9) per `./scripts/coverage.py juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/docs/ --run`.

## Progress log

### 2026-05-24 (this session)

- **Phases 0–4 complete + Phase 6 release-notes update.**
- New package `org.apache.juneau.rest.docs` with four mixin classes, plus a `package-info.java` overview.
- New tests in `juneau-utest/src/test/java/org/apache/juneau/rest/docs/`:
  - `BasicSwaggerResource_AsMixin_Test`, `BasicSwaggerResource_Standalone_Test`, `BasicSwaggerUiResource_AsMixin_Test`
  - `BasicOpenApiResource_AsMixin_Test`, `BasicRedocResource_AsMixin_Test`
  - `BasicApiDocs_TransitiveDedupe_Test`, `BasicApiDocs_ParentChain_Test`
- `BasicRestOperations` slimmed to four residual methods (`error`, `getFavIcon`, `getHtdoc`, `getStats`); `getSwagger` / `getOpenApi` removed. Class-level `@HtmlDocConfig` and `@JsonSchemaConfig` retained (still drives navlinks + bean-defs reuse for the host's own schemas).
- `BasicGroupOperations` slimmed to just `getChildren(...)`; matchers and query-mirror methods removed.
- `BasicRestServlet` / `BasicRestObject` declare `@Rest(mixins={BasicSwaggerUiResource.class, BasicRedocResource.class})` so subclasses get all six api-docs URLs.
- `apiFormat` annotation member, `RestContext` memoizer + accessor, `RestServerConstants` constants, `RestAnnotation` builder/field/accessor, and `DefaultConfig` default all removed.
- Test cleanup:
  - Deleted: `Rest_ApiFormat_Both_Test`, `Rest_ApiFormat_OpenApi_Test`, `Rest_ApiFormat_Resolution_Test`, `Rest_ApiFormat_Swagger_Test`, `Rest_GroupQueryMirrors_Test`.
  - Updated: `OpenApiYamlRoundTrip_Test` (removed `apiFormat="openapi"`; added `noInherit={"mixins"}` on the round-trip-target host classes to keep the parsed YAML doc small enough to round-trip cleanly — see open question on YAML buffer-underflow below), `OpenApiSchemaReuse_Test` and `Swagger_Test#t01_bodyWithReadOnlyProperty` (no source changes needed once `BasicRestOperations`'s `@JsonSchemaConfig` was preserved), `MixinContext_Construction_Test` (added `noInherit={"mixins"}` on hosts that assert mixin-context counts).

### Open questions / blockers carried into the next session

- **YAML buffer-underflow on the full `BasicRestServlet` mixin surface.** `OpenApiYamlRoundTrip_Test#c01` was previously asserting against the small `apiFormat="openapi"`-only document; the post-migration `BasicRestServlet` mounts six api-docs URLs and the resulting OpenAPI 3.1 doc round-trips through the YAML parser with `java.io.IOException: Buffer underflow`. Worked around in this session by giving the test class a `noInherit={"mixins"}` host plus an explicit `mixins=BasicOpenApiResource.class` so the round-trip exercises just the OpenAPI mount. The full-surface failure is a separate latent YAML-parser limitation — file as a follow-on TODO.

### 2026-05-24 (Phase 5 + 6 completion)

- **Phase 5 — real-container parity tests landed** under `juneau-utest/src/test/java/org/apache/juneau/rest/docs/`:
  - `BasicApiDocsTestFixtures` (shared helper) — encodes the six canonical api-docs URLs + per-URL response-shape assertions (status, `Content-Type` prefix, must-contain body substring) so the `MockRest` / Jetty / Spring Boot variants run the same checks against three different deployment paths. Asserts content-shape rather than byte-identity because the OpenAPI / Swagger documents embed request-derived `host` / `servers` fields that differ across deployments.
  - `BasicApiDocs_JettyMicroservice_Test` — boots a real `Microservice` + Jetty on an ephemeral port via `MicroserviceTestFixture`, mounts a `BasicRestServlet` host (inherits the four-mixin pack via the parent's `@Rest(mixins={...})` declaration), hits the six URLs over real HTTP using the JDK `HttpClient`.
  - `BasicApiDocs_Springboot_Test` — `@SpringBootTest(webEnvironment=RANDOM_PORT)` with embedded Tomcat, host servlet wired via `ServletRegistrationBean` against a `BasicSpringRestServlet` subclass that adds the four-mixin pack explicitly (`BasicSpringRestServlet` itself ships `@Rest` with no default mixins, by design). Uses the JDK `HttpClient` (not `TestRestTemplate`, which was restructured in Spring Boot 4.0 and is on the deprecation path). Same shared fixture as the Jetty test so three-way deployment parity is enforced.
  - `BasicApiDocs_Springboot_MultiOpenApiProvider_Test` — `SpringApplicationBuilder` (non-web) with two `OpenApiProvider` `@Bean`s. Two scenarios: (a) one marked `@Primary` resolves via `SpringBeanStore.getBean(OpenApiProvider.class)`; (b) two unmarked `@Bean(name=...)` beans colliding on the same bean id throw `BeanDefinitionOverrideException` at context load — documents the known "Spring fails before Juneau gets called" rough edge.
- **POM addition:** `juneau-utest/pom.xml` now pulls in `spring-boot-starter-test` (test scope, excluded `spring-boot-starter-logging`) so `@SpringBootTest` + `SpringBootConfiguration` + the random-port embedded-Tomcat web environment are available. Existing `spring-boot-starter-web` alone is insufficient for the test scaffolding.
- **Phase 6 docs completed (juneau-docs):**
  - New topic page `pages/topics/10.16.02a.ApiDocsMixins.md` — covers all four mixins, the default mount on `BasicRestServlet` / `BasicRestObject`, custom compositions (spec-only, UI-only, full pack, `noInherit` opt-out, standalone via `@Rest(paths=...)`), `defaultAccept="text/html"` behavior on UI mixins, format-pinned `/openapi.json` / `/openapi.yaml`, Spring Boot vs. microservice deployment with the `@Primary` precedence rule for multi-`OpenApiProvider` apps, and the migration table from `apiFormat`.
  - New "Parent-chain mixin aggregation" sub-section appended to `pages/topics/10.07a.RestServerComposition.md` — covers aggregation order (parent-first, child-appended, `LinkedHashSet` dedupe), `@Inherited` semantics, parent + child union, parent + child same-mixin dedupe, `noInherit={"mixins"}` opt-out, and orthogonal interaction with transitive mixin resolution. Cross-references `ApiDocsMixins.md` as the worked example.
  - Rewrote `pages/topics/10.16.02.BasicRestServletSwagger.md` — removed all `apiFormat` references and the `?Swagger` / `?OpenApi` query-mirror examples; replaced the API surface description with the new six-URL default mount table; added migration note at the top pointing to the V9.5 migration guide; restructured the "Mounting on a non-`BasicRestServlet` resource" section to use mixin composition; cross-references `ApiDocsMixins.md` for the full story. Kept the `components.schemas` reuse section (still accurate).
  - Registered the new topic page `topics/10.16.02a.ApiDocsMixins` in `sidebars.ts` between the existing `BasicRestServletSwagger` (10.16.2) and `BasicSwaggerInfo` (10.16.3) entries.
- **Verification:**
  - `./scripts/test.py -t` — full unit-test run **green** (~70s; 5 new test methods landed across the three new test classes — 1 in `BasicApiDocs_JettyMicroservice_Test`, 1 in `BasicApiDocs_Springboot_Test`, 2 in `BasicApiDocs_Springboot_MultiOpenApiProvider_Test`; the Spring Boot start-up cost shows up as expected ~1.5s per test class).
  - `./scripts/test.py -b` — full build **green** (~33s); RAT header check passed on all new files.
  - `./scripts/coverage.py juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/docs/ --run` — **100% line + branch** on all four mixin classes (`BasicSwaggerResource` 9/9, `BasicSwaggerUiResource` 9/9, `BasicOpenApiResource` 39/39, `BasicRedocResource` 9/9; 66/66 instructions covered, 0 branches because the classes have no conditional logic — pure declarative `@Rest` + `@RestGet` shapes).

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

## Post-completion correction (2026-05-25) — `@Rest(paths=...)` dead-code removal

A Phase C2 cleanup pass on 2026-05-25 stripped the class-level `@Rest(paths=...)` annotation from all four api-docs mixins (`BasicSwaggerResource`, `BasicSwaggerUiResource`, `BasicOpenApiResource`, `BasicRedocResource`) after rediscovering — via the framework Javadoc at `Rest.java:1017-1021` (and `Rest.java:1081-1085` for `paths()`) — that the annotation is **silently ignored** under the mixin pattern. The framework note says it plainly: when a class is imported as a mixin via `@Rest(mixins=...)`, the importing host's own `path()` / `paths()` governs the mount and the mixin's class-level path declaration lands in the dead-code bucket; mixin endpoints land in the host's URL namespace via the op-level `@RestGet(path=...)` declarations.

**Per-class decision and rationale:**

| Mixin | Decision | Rationale |
|---|---|---|
| `BasicSwaggerResource` | **Mixin-only** — stripped `@Rest(paths={"/api"})`; kept empty `@Rest`. | The `/api/*` mount is pinned at the op level by `@RestGet(path="/api/*")` on `getSwagger`. Class doesn't extend `RestServlet` today; no standalone-use call site in tests. The `BasicSwaggerResource_Standalone_Test` confirms "standalone" in this codebase means *standalone of the `BasicRestServlet` chain* (mixed into a `RestObject` host), not standalone deployment. |
| `BasicSwaggerUiResource` | **Mixin-only** — stripped `paths={"/swagger"}`; kept `mixins={BasicSwaggerResource.class}` + `defaultAccept="text/html"` (load-bearing). | Same reasoning — op-level `@RestGet(path="/swagger/*")` is the live mount. |
| `BasicOpenApiResource` | **Mixin-only** — stripped `paths={"/openapi"}`; kept `serializers={YamlSerializer.class}` (load-bearing). | Three op-level mounts (`/openapi/*`, `/openapi.json`, `/openapi.yaml`) are the live wiring. |
| `BasicRedocResource` | **Mixin-only** — stripped `paths={"/redoc"}`; kept `mixins={BasicOpenApiResource.class}` + `defaultAccept="text/html"` (load-bearing). | Op-level `@RestGet(path="/redoc/*")` is the live mount. |

No tests were modified — every existing test exercises the mixin via `@Rest(mixins=...)` on a host class, so they continued to pass verbatim. Each class's Javadoc gained a "Mixin-only deployment" section explaining the silent-ignore rule and pointing readers to FINISHED-99 (SVL resolution on `@RestOp(path)`) for the recommended runtime-configurable mount pattern, e.g. `@RestGet(path="${myroute:default}/*")`. The misleading "Or extend the class directly for a standalone deployment whose mount paths come from the inherited `@Rest(paths)` default." sentence was removed from every class's class-level Javadoc.

**Observation (filed but not fixed in this pass):** none of these classes extend `RestServlet` today, so the dead-code annotation was never live in any scenario. Multi-mode deployment would have required adding `extends RestServlet` + `serialVersionUID` + restructured constructors — a meaningful API surface change without clear standalone-use demand. The simpler, consistent call here is "all mixin-only"; if a future use case justifies multi-mode for one of these, it's a separate refactor.

## FINISHED-101 follow-up — SVL-configurable mount paths

The four mixins in this pack now declare their op-level paths as `/${juneau.<role>.path:<default>}` so deployers can relocate the mount via system property, env var, or Config without subclassing. See `todo/FINISHED-101-mixin-svl-paths.md` for the full audit, naming convention, migration notes, and the per-class `*_SvlPathOverride_Test` coverage.

- `BasicSwaggerResource`: `${juneau.swagger.path:api}` → default `/api/*`.
- `BasicSwaggerUiResource`: `${juneau.swaggerui.path:swagger}` → default `/swagger/*`.
- `BasicOpenApiResource`: `${juneau.openapi.path:openapi}` → default `/openapi/*`, `/openapi.json`, `/openapi.yaml` (single shared variable controls all three op-paths on three distinct methods).
- `BasicRedocResource`: `${juneau.redoc.path:redoc}` → default `/redoc/*`.
