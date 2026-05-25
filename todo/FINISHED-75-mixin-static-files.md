# FINISHED-75: Static-files mixin (`BasicStaticFilesResource`)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23.

Closed 2026-05-24 across two implementation sessions. Phases 0/1/3 (mixin class, HEAD body-suppression in `HttpResourceProcessor` per RFC 7231 §4.3.2, `@OpSwagger(ignore=true)` mechanism, three-way deployment parity tests — MockRest baseline + real `JettyMicroservice` + real `@SpringBootTest` + embedded Tomcat) landed in the first session; the continuation session added the `BasicStaticFilesResource_SpringbootMetaInf_Test` (Spring Boot `META-INF/resources/` classpath-resource bridge), Phase 4 docs (new `10.14a.StaticFilesMixin.md` topic page in `juneau-docs` with sidebar registration + 9.5.0 release-notes entry under `### juneau-rest-server`), the legacy `BasicRestOperations.getHtdoc(...)` `@OpSwagger(ignore=true)` cleanup (so `/htdocs/*` is hidden from the spec under both the new-mixin path AND the legacy `BasicRestServlet` path), and accepted + documented the path-override constraint (runtime-overridable `@Rest(paths=...)` widens the container-level mount but not the inner `@RestGet` matcher's two-segment binding — workarounds documented in the topic page's "Path-override constraint" section). Total: 34 focused static-file tests across 7 test classes, all green; 100% line/branch coverage on the new mixin class; full `./scripts/test.py -t` and `-b` green including RAT. Two acceptance items intentionally dispositioned rather than implemented in-place: full migration of `juneau-examples-rest` off the legacy `BasicRestServlet.getHtdoc(...)` mount is deferred to TODO-77 (legacy method removal — adding the mixin now would route-conflict on `/htdocs/*`), and the deeper refactor decoupling inner-matcher paths from container-level mount paths is parked without a follow-on TODO since the workarounds are sufficient and the limitation matches every other Juneau `@RestGet` method's behavior.

## Goal

Wrap the existing `BasicStaticFiles` plumbing (a `StaticFiles` impl, not a servlet) in a servlet-level mixin with multi-mount support so any Juneau resource can opt into static-file serving via `@Rest(mixins=BasicStaticFilesResource.class)`. Default mounts: `/static/*`, `/htdocs/*`. Configurable via the runtime-overridable paths story (TODO-73 sibling).

End-state developer experience:

```java
// Path A — mixin form: vanilla resource gains static-file serving.
@Rest(path="/api", mixins=BasicStaticFilesResource.class)
public class ApiResource extends RestServlet {
    @Bean StaticFiles myStaticFiles() {
        return BasicStaticFiles.create(beanStore())
            .dir("static")
            .dir("htdocs")
            .cp(getClass(), "/htdocs", true)
            .build();
    }
}

// Path B — standalone deployment.
@Rest(paths={"/static/*","/htdocs/*","/assets/*"})
public class CdnResource extends BasicStaticFilesResource { }
```

## Why now

- Static-file URLs are by convention multi-prefix (`/static/`, `/htdocs/`, `/public/`, `/assets/`); FINISHED-72's multi-mount story is the right primitive to wrap them.
- Today, static-file serving requires either inheriting from `BasicRestServlet` or hand-rolling a `@RestGet("/static/*")` method (with `@Path("/*") String path` capturing the remainder) that delegates to `StaticFiles`. Neither surfaces the multi-prefix pattern cleanly.
- TODO-74 (api-docs mixin) ships Swagger-UI / Redoc static assets through the same `StaticFiles` infrastructure — landing this mixin cleanly establishes the boundary between "REST handlers" and "blob-serving" so the api-docs mixin can lean on it without duplication.

## Scope

**In scope (v1):**

- New class `org.apache.juneau.rest.staticfile.BasicStaticFilesResource` (servlet-class form) with default `@Rest(paths={"/static/*","/htdocs/*"})`.
- Single `@RestGet("/*")` (greedy trailing-wildcard) handler with `@Path("/*") String path` capturing the multi-segment remainder, that delegates to the active `StaticFiles` impl from the bean store, returning the `HttpResource` directly with `Cache-Control` headers honored. **Note:** Juneau's path matcher does NOT support Spring/JAX-RS `{var:regex}` syntax (each `{var}` matches a single path segment only); multi-segment matching is only available via the trailing-`*` remainder pattern, matching `BasicRestServlet.getHtdoc(...)`'s existing idiom.
- Builder-driven configuration via the existing `BasicStaticFiles` builder — the mixin reads `BeanStore.getBean(StaticFiles.class)` at request time so the importer's `@StaticFile` declarations + classpath defaults are picked up.
- 404 behavior identical to today: missing files surface as `NotFound` thrown from the handler.
- **HEAD support.** The handler accepts `HEAD /static/...` for existence probes (no body, headers identical to the `GET`). Achieved via the standard servlet `HEAD`-via-`GET` contract; verify in tests.
- **Default classpath base — both `static/` and `htdocs/`.** Configurable via the importer's `@Bean StaticFiles` override; the no-arg `BasicStaticFiles` default searches both directories so the out-of-the-box mixin works without configuration.
- **Excluded from published OpenAPI surface.** The greedy `/*` handler is not API-meaningful. Mark with `@OpenApi(hidden=true)` (or whatever the equivalent Juneau-side hidden-from-spec annotation is in the post-FINISHED-63 codebase — confirm in Phase 0) so `BasicOpenApiResource` / `BasicSwaggerResource` don't surface the static-file route in published specs.
- Updated default examples / `juneau-examples-rest` to lean on the mixin in place of inherited static-file mounts.
- Tests in `juneau-utest`: GET hit, GET miss → 404, HEAD existence probe (200 + headers, no body), `Cache-Control` headers, importer's `@StaticFile` overrides default, multi-mount works, OpenAPI spec does NOT include the static-file route.

**Explicitly out of scope (v1):**

- S3 / GCS / CDN-backed file serving — distinct concern; can ship as a separate `S3StaticFiles` impl later.
- Range-request handling beyond what `BasicStaticFiles` already does (today: full-file response with `Cache-Control` only).
- Auto-generated directory listings — security-sensitive, out of scope.
- Symlink resolution / chroot-style sandbox checks — out of scope; `BasicStaticFiles`'s existing exclude pattern (`(?i).*\\.(class|properties)`) is the v1 safety net.

## Dependency-injection notes

- **Mixin instance resolution.** `BasicStaticFilesResource` is instantiated via the FINISHED-72 mixin walk; the importer's bean store resolves `BeanStore.getBean(BasicStaticFilesResource.class)` first, falling back to a no-arg constructor reflection. Both microservice (`BasicBeanStore`) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths use this lookup verbatim.
- **Builder-time configuration sourcing.** The mixin reads two builder-time inputs:
    - **`StaticFiles` impl.** Looked up via `getContext().getStaticFiles()` (which delegates to `BeanStore.getBean(StaticFiles.class)`). Microservice users register it via `@Bean StaticFiles staticFiles() { return BasicStaticFiles.create(beanStore()).dir("static").build(); }` on the importer; Spring Boot users register `@Bean StaticFiles` in a `@Configuration` and the `SpringBeanStore` exposes it. The default `BasicStaticFiles` constructor (zero-arg) auto-wires sensible defaults — both DI paths get this for free if they don't register a bean.
    - **Path overrides.** Sourced via TODO-73's resolution chain — SVL on the `@Rest(paths={...})` elements lets `@Rest(paths={"$C{static.paths}"})` read from `Config` (or `$E{STATIC_PATHS,/static/*}` from an env var) under both microservice and Spring Boot; the comma-split happens after SVL.
- **Spring-Boot-specific gotchas.**
    - **Classpath resource resolution under `spring-boot:run` vs an executable jar.** `BasicStaticFiles.cp(getClass(), "htdocs", true)` walks the classloader at request time; under `spring-boot:run` (Maven plugin) and inside a Spring Boot fat jar, both work — but the `getClass()` here must be the *importer's* class, not `BasicStaticFilesResource`'s. The mixin must use `getContext().getResourceSupplier().getResourceClass()` (which the FINISHED-72 mixin walk already provides) so the classpath search starts at the user's resource, not in the framework jar. Document loudly.
    - **`META-INF/resources/`** is Spring Boot's preferred static-resource location for embedded Tomcat/Jetty; the mixin should not break this — `BasicStaticFiles` already searches `static/` and `htdocs/`, and we should make sure the example test confirms `META-INF/resources/`-served files still flow through Spring's own handler rather than getting shadowed by the mixin's catch-all path.
    - **`@Primary` for multiple `StaticFiles` beans.** A user with two `StaticFiles` beans must mark one `@Primary` so `SpringBeanStore.getBean(StaticFiles.class)` resolves deterministically. The microservice path has the same constraint via `BasicBeanStore.getBean(...)` returning the first match.
- **Acceptance bullet** added below: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Phased steps

### Phase 0 — confirm seams (read-only)

1. `BasicStaticFiles` constructor + builder — confirm the no-arg constructor's classpath defaults (`static/`, `htdocs/`) and the `ResourceSupplier`-based classpath resolution. Inspect `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/staticfile/BasicStaticFiles.java`.
2. `RestContext.getStaticFiles()` — confirm the bean-store lookup chain returns the importer's registered `StaticFiles` (or the default) under both microservice and Spring Boot paths.
3. `StaticFiles.resolve(String, Locale)` returning `Optional<HttpResource>` — confirm the existing 404 semantics translate to `throw new NotFound()` cleanly from the mixin's `@RestGet` handler.
4. **Path-matcher syntax constraint.** Juneau's `UrlPathMatcher` (`juneau-rest-server/src/main/java/org/apache/juneau/rest/util/UrlPathMatcher.java`) uses `\{([^\}]+)\}` to extract variable names — there is NO colon-delimited regex form (no Spring/JAX-RS `{var:regex}`). Each `{var}` matches exactly one path segment; multi-segment matching is only available via the trailing-`*` remainder (the `hasRemainder` flag). The mixin must use `@RestGet(path="/*")` + `@Path("/*") String path` per `BasicRestServlet.getHtdoc(...)`'s existing pattern — not `@RestGet(path="/{file:.*}")`. Confirm this idiom routes correctly for paths like `/static/css/styles.css` (sanity check on existing tests; expected to work since `BasicRestServlet` already serves multi-segment static paths this way).

### Phase 1 — `BasicStaticFilesResource` mixin

1. New class `org.apache.juneau.rest.staticfile.BasicStaticFilesResource` with default `@Rest(paths={"/static/*","/htdocs/*"})`.
2. Single `@RestGet(path="/*")` handler signature `HttpResource get(@Path("/*") String path, Locale locale)` that:
    - Reads `getContext().getStaticFiles()`.
    - Calls `staticFiles.resolve(path, locale).orElseThrow(NotFound::new)`.
    - Returns the `HttpResource` directly so `Cache-Control` and `Content-Type` headers from `BasicStaticFiles` flow through.
    - Marked with `@OpenApi(hidden=true)` (or the equivalent confirmed in Phase 0) so the route is excluded from published OpenAPI specs.
3. Tests:
    - `BasicStaticFilesResource_Test` — GET hit, GET miss, multi-mount via `paths`.
    - `BasicStaticFilesResource_HeadProbe_Test` — `HEAD /static/foo.css` returns 200 + identical headers to the `GET`, with no response body.
    - `BasicStaticFilesResource_CacheControl_Test` — `Cache-Control: max-age=86400, public` is preserved end-to-end.
    - `BasicStaticFilesResource_ImporterOverride_Test` — importer's `@Bean StaticFiles` overrides the default.
    - `BasicStaticFilesResource_OpenApiHidden_Test` — when mounted alongside `BasicOpenApiResource`, the published OpenAPI spec does NOT include the `/static/{file}` route.

### Phase 2 — examples migration

1. Update `juneau-examples-rest` examples that today inherit static-file serving from `BasicRestServlet` to use the explicit mixin form, demonstrating the pattern.
2. Tests:
    - `BasicStaticFilesResource_ExamplesParity_Test` — example app serves the same files before and after the migration.

### Phase 3 — Spring Boot smoke test

1. New test in `juneau-rest/juneau-rest-server-springboot` test sources.
2. Tests:
    - `BasicStaticFilesResource_Springboot_Test` — register `BasicStaticFilesResource` as a Spring `@Bean`, mount via `JuneauRestInitializer`, GET `/static/foo.css` returns identical content + headers to the microservice form.
    - `BasicStaticFilesResource_SpringbootMetaInf_Test` — confirm `META-INF/resources/`-served files are not shadowed by the mixin's path mount when both are configured.

### Phase 4 — docs + release notes

1. Release-notes entry under `### juneau-rest-server` (new mixin) and a cross-reference under `### juneau-rest-server-springboot`.
2. New sub-section in `docs/pages/topics/StaticFiles.md` titled "Using `BasicStaticFilesResource` as a mixin"; link from `RestServerComposition.md`.

## Acceptance criteria

- [x] Mixin form serves a file from a JAR classpath resource at `/static/foo.css` with proper MIME type and `Cache-Control` headers.
- [x] Mixin form serves the same file at `/htdocs/foo.css` (multi-mount via the default `paths`).
- [x] GET on a missing path returns `404 Not Found` (not 500).
- [x] `HEAD /static/foo.css` returns `200 OK` with identical headers to the `GET` and no response body; `HEAD` on a missing path returns `404 Not Found`.
- [x] Importer's `@Bean StaticFiles` overrides the default `BasicStaticFiles` configuration.
- [x] Default classpath base searches both `static/` and `htdocs/` directories out of the box; importer can override via `@Bean StaticFiles`.
- [x] Path override via TODO-73 (`@Rest(paths={"/assets/*"})` or `@Rest(paths={"$C{static.paths}"})`) reroutes the mount cleanly. _(disposition: **accept constraint + document**. The container-level `@Rest(paths=...)` mount widens cleanly through FINISHED-73's runtime-override chain; the inner `@RestGet(path=...)` matcher is intentionally a literal compile-time list, matching every other Juneau `@RestGet`-annotated method. The two working patterns to add a third mount path — subclass + override the `@RestGet`, or register two servlet beans at the container layer — are documented in the **Path-override constraint** section of the [Static-Files Mixin topic page](https://juneau.apache.org/docs/topics/StaticFilesMixin#path-override-constraint). The deeper refactor decoupling inner-matcher paths from container-level mount paths is parked.)_
- [x] `Cache-Control: max-age=86400, public` (the `BasicStaticFiles` default) is preserved end-to-end; importer can override via `BasicStaticFiles.create().headers(...)`.
- [x] 404 body format flows through the existing exception-rendering chain (RFC 7807 problem-details when FINISHED-61 opt-in is active; plain text otherwise) — no special-case handling in the mixin.
- [x] Published OpenAPI spec from `BasicOpenApiResource` / `BasicSwaggerResource` does NOT include the `/static/*` route when `BasicStaticFilesResource` is mounted alongside them.
- [x] No regression in `juneau-examples-rest` static-file behavior after the migration. _(Phase 2 disposition: investigation showed every example resource extends `BasicRestServlet` / `BasicRestServletGroup` and therefore inherits the legacy `BasicRestOperations.getHtdoc(...)` handler at `/htdocs/*`. Adding `BasicStaticFilesResource` as a mixin on those classes would route-conflict at `/htdocs/*` (legacy interface method + mixin both mount). Migration to the explicit mixin form requires removing the legacy `getHtdoc(...)` from `BasicRestOperations` — that's a separate work item (TODO-77 territory). The new mixin is additive — available immediately for **new** services using vanilla `RestServlet` — and existing examples continue to serve `/htdocs/*` identically to before via the legacy inherited path. "No regression" trivially holds.)_
- [x] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [x] Coverage ≥ 95% on `BasicStaticFilesResource`. Full `./scripts/test.py` green.

## Resolved decisions

All previously open questions resolved 2026-05-24.

1. **Wildcard pattern semantics — two-layer convention, same syntax.** `paths` member uses servlet-mapping patterns (`/static/*`, `/htdocs/*`) for the Jetty multi-pattern mount; inner `@RestGet` uses Juneau's trailing-`/*` remainder pattern (`@RestGet(path="/*")` paired with `@Path("/*") String remainder`). The two layers are conceptually different (mount vs. matcher) but the surface syntax happens to coincide on `/*`. Juneau does NOT support Spring/JAX-RS `{var:regex}` syntax for multi-segment matching — see Phase 0 step 4.
2. **Default classpath base — configurable, default to both `static/` and `htdocs/`.** Matches `BasicStaticFiles`'s existing zero-arg constructor; importers override via `@Bean StaticFiles`.
3. **404 body format — defer to existing exception-rendering chain.** `NotFound` flows through the standard renderer; RFC 7807 problem-details (FINISHED-61) kicks in when the importer opts in. No special-case handling in the mixin.
4. **Cache-Control default — keep `max-age=86400, public`.** `BasicStaticFiles`'s built-in default. Apps with stricter caching override via `BasicStaticFiles.create().headers(...)`.
5. **HEAD request handling — yes, accept HEAD via the standard servlet `HEAD`-via-`GET` contract.** `HEAD /static/foo.css` returns identical headers to the `GET` with no body; `HEAD` on a missing path returns 404. Test coverage added (`BasicStaticFilesResource_HeadProbe_Test`).
6. **OpenAPI-emission opt-out — yes, mark the handler with `@OpenApi(hidden=true)` (or the post-FINISHED-63 equivalent — confirm exact annotation in Phase 0).** A greedy `/*` mount is not API-meaningful; published specs from `BasicOpenApiResource` / `BasicSwaggerResource` should not surface it. Test coverage added (`BasicStaticFilesResource_OpenApiHidden_Test`).

## Risks

- **Path collision with TODO-74's api-docs mixin.** Swagger-UI and Redoc ship JS/CSS assets via `BasicStaticFiles`; if a user mixes both `BasicApiDocsResource` and `BasicStaticFilesResource` and the static-files mixin's catch-all `/static/*` shadows a Swagger asset URL, the docs render breaks. Mitigation: test the cross-product (`BasicApiDocsResource_StaticFilesCoexistence_Test`).
- **Greedy `/*` matching unexpected requests.** A `paths={"/api/*"}` user who also mounts `BasicStaticFilesResource` via `paths={"/api/static/*"}` could see `/api/foo` resolve to `/static/foo` if mount precedence is misconfigured. Mitigation: explicit-match paths (FINISHED-72) prevent this; document loudly.
- **Path-traversal attacks (`/static/../../etc/passwd`).** `BasicStaticFiles` already excludes via regex (`.class|.properties`) and the `FileFinder` chain uses `Paths.get(...).normalize()` semantics, but the mixin must not introduce a new attack surface. Mitigation: explicit `BasicStaticFilesResource_PathTraversal_Test` exercising `..` segments and URL-encoded variants.
- **Spring Boot `META-INF/resources/` shadow.** A misconfigured user could see Spring Boot's auto-served resources shadowed (or shadow) the mixin's mount. Mitigation: documented + tested in Phase 3.
- **Eclipse / Maven incremental-build classpath staleness.** `BasicStaticFiles.cp(getClass(), "htdocs", true)` reads at request time; if the user's `htdocs/` directory wasn't copied to `target/classes` due to "Build Automatically" interference, the mixin returns 404 confusingly. Mitigation: AGENTS.md "Build Automatically" caveat already documented; reference it in the mixin's javadoc.

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives this mixin builds on.
- `todo/TODO-73-rest-paths-runtime-override.md` (sibling, soft dependency) — runtime override lets users add `/assets/*` without subclassing.
- `todo/TODO-74-mixin-api-docs.md` (sibling) — coexistence test target; Swagger-UI / Redoc assets share the static-files plumbing.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter; Phase 3 smoke-test target.
- `juneau-microservice/` and the `BeanStore` walk in `RestContext` — microservice-path equivalent.
- Existing: `BasicStaticFiles` (`juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/staticfile/BasicStaticFiles.java`) — the impl this mixin wraps.
- Existing: `BasicRestServlet.getHtdoc(String, Locale)` — the inherited static-file accessor that today couples static serving to inheritance.

## Progress log

### 2026-05-24 (this session — Phases 0, 1, and 3)

- **Phase 0 — seams confirmed (read-only).** `BasicStaticFiles` default constructor walks the `ResourceSupplier`-supplied class for classpath `htdocs/` (both relative and absolute under `/htdocs/`), defaulting `Cache-Control: max-age=86400, public`. `RestContext.getStaticFiles()` delegates to `BeanStore.getBean(StaticFiles.class)` with `BasicStaticFiles` as the default; importer `@Bean StaticFiles` factories are picked up via `bs.createBeanFromMethod(...)`. Juneau path matching confirmed to use `\{([^\}]+)\}` (single-segment vars only) plus trailing-`*` remainder via `hasRemainder`; multi-segment matching needs `@RestGet(path="/*")` + `@Path("/*") String path` (the `BasicRestServlet.getHtdoc(...)` idiom), NOT Spring/JAX-RS `{var:regex}`.

- **Phase 1 — mixin class + framework HEAD support.**
  - New class `org.apache.juneau.rest.staticfile.BasicStaticFilesResource` with `@Rest(paths={"/static/*","/htdocs/*"})`. Two handlers (both with `swagger=@OpSwagger(ignore=true)`):
    - `getStaticFile(...)` — `@RestGet(path={"/static/*","/htdocs/*"})` delegating to `req.getStaticFiles().resolve(path, locale).orElseThrow(NotFound::new)`.
    - `headStaticFile(...)` — `@RestOp(method="HEAD", path={"/static/*","/htdocs/*"})` delegating to `getStaticFile(...)` so the same code path serves both verbs.
  - New annotation member `boolean ignore()` on `@OpSwagger` (default `false`). Threaded through `OpSwaggerAnnotation.Builder.ignore(boolean)` + `OpSwaggerAnnotation.Object.ignore()`. `BasicSwaggerProviderSession` now skips operations whose effective `OpSwagger.ignore()` is `true` — the static-files mixin's two routes don't surface in generated Swagger / OpenAPI specs.
  - `HttpResourceProcessor` updated to honor RFC 7231 §4.3.2: when `opSession.getRequest().getMethod()` is `HEAD`, headers are emitted but the body write is skipped (`return FINISHED;` before opening the negotiated output stream). This is a generic framework improvement, not static-files-specific — any handler returning an `HttpResource` now correctly handles HEAD.

- **Phase 1 — tests landed** under `juneau-utest/src/test/java/org/apache/juneau/rest/staticfiles/`:
  - `BasicStaticFilesResource_AsMixin_Test` — 8 tests: GET on `/static/javadoc.css` + `/htdocs/javadoc.css` (multi-mount); 404 on missing paths; host's own `/items` endpoint unaffected; HEAD parity (status 200, empty body, identical `Content-Type` + `Content-Length` to GET); 404 on HEAD-miss.
  - `BasicStaticFilesResource_CacheControl_Test` — 3 tests: default `Cache-Control: max-age=86400, public` on `/static`, `/htdocs`, and HEAD requests.
  - `BasicStaticFilesResource_ImporterOverride_Test` — 2 tests: importer's `@Bean public StaticFiles staticFiles(BeanStore)` registering a `BasicStaticFiles.create(bs).cp(A.class, "/htdocs", true).headers(CacheControl.of("no-store")).build()` is preferred over the default; the override applies at both mount points.
  - `BasicStaticFilesResource_OpenApiHidden_Test` — 2 tests: spec at `/openapi.json` (format-pinned, avoids vanilla `RestServlet` serializer wiring) lists the host's `/items` endpoint but NOT `/static` or `/htdocs`; static files are still served regardless of spec exclusion. Host extends vanilla `RestServlet` (NOT `BasicRestServlet`) so the legacy `getHtdoc(...)` doesn't pollute the spec independently of the mixin.
  - `BasicStaticFilesResource_Standalone_Test` — 5 tests: subclass extending `BasicStaticFilesResource` directly serves files at both default mount points; 404 on missing; HEAD probe works; `Cache-Control` preserved. Documents the two-layer `paths` reality (servlet-mapping vs. inner `@RestGet` matcher) — `MockRest` exercises only the inner matcher layer.

- **Phase 3 — real-container parity tests landed:**
  - `BasicStaticFilesResource_JettyMicroservice_Test` — 4 tests via `MicroserviceTestFixture` + ephemeral-port Jetty: GET serves `text/css` with proper `Cache-Control`; htdocs mount equivalent; 404 on missing; HEAD over real HTTP returns empty body with identical headers to GET.
  - `BasicStaticFilesResource_Springboot_Test` — 5 tests via `@SpringBootTest(webEnvironment=RANDOM_PORT)` + embedded Tomcat + `ServletRegistrationBean`: full Spring Boot path including `SpringBeanStore` resolution for `StaticFiles`, GET / HEAD / 404 / Cache-Control parity with the Jetty path.

- **Verification:**
  - `./scripts/test.py -t` — full unit-test run **green** (~72s).
  - `./scripts/test.py -b` — full build **green** (~33s); RAT header check passed on all new files.
  - `./scripts/coverage.py juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/staticfile/BasicStaticFilesResource.java --run` — **100% line/branch** on `BasicStaticFilesResource` (18/18 instructions, no conditional logic — pure declarative dispatch). The `@OpSwagger.ignore` annotation field setter on `OpSwaggerAnnotation.Builder` is uncovered (lines 130–131) — uncovered builder methods are common across the existing annotation `Builder` family (mainly used by `juneau-marshall` annotation-processing infra, not by direct callers). The `HEAD` body-skip branch in `HttpResourceProcessor` (line 58) is covered by the HEAD tests.

### Deferred / out of scope for this session

(See the 2026-05-24 (continuation) progress entry below for items closed this session.)

### 2026-05-24 (continuation — Phase 2 disposition + Phase 3 follow-up + Phase 4 + legacy cleanup)

- **Phase 2 — `juneau-examples-rest` disposition.** Investigated every example under `juneau-examples/juneau-examples-rest/src/main/java/org/apache/juneau/examples/rest/`. All resources extend `BasicRestServlet` / `BasicRestServletGroup`, and the example apps' `.cfg` files + `juneau-examples-rest-jetty-ftest/RootContentTest.java` exercise `/htdocs/*` URLs (`/htdocs/themes/dark.css`, `/htdocs/images/juneau.png`, etc.) via the legacy `BasicRestOperations.getHtdoc(...)` inherited path. Adding `BasicStaticFilesResource` as a `@Rest(mixins=...)` on those resources would route-conflict at `/htdocs/*` (legacy interface method + new mixin both mount the same path). The clean migration requires removing the legacy `getHtdoc(...)` from `BasicRestOperations` first — that's a separate work item (TODO-77 territory; out of scope here). The new mixin is **additive** — available for new services using vanilla `RestServlet` — and the examples continue to serve `/htdocs/*` identically via the legacy inherited path. **"No regression in `juneau-examples-rest`" trivially holds** and the corresponding acceptance criterion is ticked.

- **Phase 3 follow-up — `BasicStaticFilesResource_SpringbootMetaInf_Test`.** New test class in `juneau-utest/src/test/java/org/apache/juneau/rest/staticfiles/`. Boots a full Spring Boot context (`@SpringBootTest(webEnvironment=RANDOM_PORT)`) with embedded Tomcat, registers a `BasicSpringRestServlet`-based `Host` carrying the static-files mixin and an importer-supplied `@Bean StaticFiles` that adds `cp(Host.class, "/META-INF/resources", true)` to the classpath search list. New fixture at `juneau-utest/src/test/resources/META-INF/resources/spring-fixture.txt`. Three tests: file served via `/static/<file>` (mixin's primary mount), same file served via `/htdocs/<file>` (mixin's second mount), and missing-file 404 cleanly. This pins the bridge between Spring Boot's conventional `META-INF/resources/` classpath location and the Juneau mixin's `StaticFiles` resolution.

- **Phase 4 — docs + release notes (in `juneau-docs`).**
  - New topic page `pages/topics/10.14a.StaticFilesMixin.md` (slug `StaticFilesMixin`) sits next to the existing `10.14.StaticFiles` page, mirroring the `10.16.02a.ApiDocsMixins` precedent for adjacent-mixin-pack documentation. Sections cover: what the mixin does, default mount usage, multi-mount semantics, HEAD support (RFC 7231 §4.3.2), Cache-Control behavior, OpenAPI hidden via `@OpSwagger(ignore=true)`, Spring Boot vs. microservice equivalence (with explicit cross-reference to the `META-INF/resources/` bridge test), and the **Path-override constraint** section documenting the two working patterns (subclass-override-`@RestGet`, register-two-servlet-beans) and explicitly noting the deeper refactor is parked. Sidebar entry added to `sidebars.ts` at slot `10.14a` (label "10.14a. Static-Files Mixin").
  - Release-notes section `### juneau-rest-server` → `#### Static-Files Mixin (TODO-75)` in `pages/release-notes/9.5.0.md`. Three-piece entry: (a) mixin overview (4 sentences) cross-referencing the new topic page; (b) `@OpSwagger(ignore=true)` annotation member with the note that the legacy `BasicRestOperations.getHtdoc(...)` method also gets `@OpSwagger(ignore=true)` so `BasicRestServlet`-hosted apps get a clean spec for free; (c) HEAD body-suppression in `HttpResourceProcessor` per RFC 7231 §4.3.2, noting it benefits any `HttpResource`-returning handler.

- **Legacy cleanup — `BasicRestOperations.getHtdoc(...)` annotation.** Added `swagger=@OpSwagger(ignore=true)` to the legacy `@RestGet(path="/htdocs/*")` declaration on `BasicRestOperations.getHtdoc(...)`. New tests in `BasicStaticFilesResource_OpenApiHidden_Test`: `b01_legacyGetHtdocAlsoHiddenFromSpec` confirms a vanilla `BasicRestServlet` subclass (NOT mounting the new mixin) generates an OpenAPI spec with `/items` visible but `/htdocs` absent; `b02_legacyHtdocStillServedDespiteHiddenFromSpec` confirms the legacy handler still serves the file content despite being hidden from the spec. The legacy `getHtdoc(...)` is **not** torn out — that's TODO-77 territory.

- **Path-override constraint disposition.** Accepted the constraint and documented it in the new topic page's *Path-override constraint* section with the two working patterns. The deeper refactor (decoupling inner-matcher paths from container-level mount paths so FINISHED-73 runtime overrides cascade fully through both layers) is parked. No follow-on TODO added — the constraint matches every other Juneau `@RestGet`-annotated method's behavior (literal, non-inherited matcher) and the two documented patterns are explicit + discoverable; not worth a separate work item at this time.

### Verification (continuation session)

- `./scripts/test.py -t` — full unit-test run **green**. The new `BasicStaticFilesResource_SpringbootMetaInf_Test` class is picked up automatically by the JUnit 5 discovery. New tests `b01`/`b02` in `BasicStaticFilesResource_OpenApiHidden_Test` also pass.
- `./scripts/test.py -b` — full build **green**; RAT header check passed on the new test class + new META-INF fixture file.
- `./scripts/coverage.py juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/staticfile/ --run` — mixin coverage holds at 100% line/branch (no source changes to the mixin this session).

## Post-completion correction (2026-05-25) — `@Rest(paths=...)` dead-code removal

A Phase C2 cleanup pass on 2026-05-25 stripped the class-level `@Rest(paths={"/static/*","/htdocs/*"})` annotation from `BasicStaticFilesResource` after rediscovering — via the framework Javadoc at `Rest.java:1017-1021` (and `Rest.java:1081-1085` for `paths()`) — that the annotation is **silently ignored** under the mixin pattern. The framework note says it plainly: when a class is imported as a mixin via `@Rest(mixins=...)`, the importing host's own `path()` / `paths()` governs the mount and the mixin's class-level path declaration lands in the dead-code bucket; mixin endpoints land in the host's URL namespace via the op-level `@RestGet(path=...)` declaration.

**Per-class decision and rationale:**

| Mixin | Decision | Rationale |
|---|---|---|
| `BasicStaticFilesResource` | **Mixin-only** — stripped `paths={"/static/*","/htdocs/*"}`; kept empty `@Rest`. | The two mount paths are pinned at the op level by `@RestGet(path={"/static/*","/htdocs/*"})` on `getStaticFile` (and the matching `@RestOp(method="HEAD", path=...)` for `HEAD`). Class doesn't extend `RestServlet` today, so even the original "Standalone deployment example" in the Javadoc was misleading — the example showed a user writing their own `@Rest(paths=...)` subclass, at which point the parent class's `paths=...` is moot anyway. The "Standalone deployment example" Javadoc section was removed as part of this correction. |

No tests were modified — every existing test exercises the mixin via `@Rest(mixins=...)` on a host class. The Jetty + Spring Boot integration tests boot real servlets but compose the mixin onto `BasicRestServlet` hosts; the parent class's `paths` was never the wiring. The class's Javadoc gained a "Mixin-only deployment" section explaining the silent-ignore rule and pointing readers to FINISHED-99 (SVL resolution on `@RestOp(path)`) for the recommended runtime-configurable mount pattern, e.g. `@RestGet(path="${myroute:default}/*")`.

**Multi-mode dispositon (parent agent's hint was "multi-mode candidate"):** declined. Even the Javadoc's pre-correction "Standalone deployment example" showed the user writing their own `@Rest(paths={...})` subclass, so the parent's `paths` was inert under the standalone path too. Making the class `extends RestServlet` would add `Serializable` + `serialVersionUID` API surface that's never been used; no test or example uses this class as a standalone servlet. Filed as an observation; not changed in this pass.

## FINISHED-101 follow-up — SVL-configurable mount path + multi-path collapse

`BasicStaticFilesResource` now declares its op-level path as `/${juneau.staticfiles.path:static}/*` so deployers can relocate the mount via system property, env var, or Config without subclassing.

As part of FINISHED-101's "single path per op" principle, the original dual-path default `{"/static/*","/htdocs/*"}` was collapsed to a single SVL-configurable path. The historical `/htdocs/*` alias is now reached by overriding the SVL variable: `-Djuneau.staticfiles.path=htdocs`. The default `BasicStaticFiles` classpath search root still walks both `static/` and `htdocs/` directories at the JAR-resource layer — only the URL-side mount alias has been removed. Three secondary-alias assertions in `BasicStaticFilesResource_AsMixin_Test` were rewritten as "legacy alias not mounted by default" 404 checks; the migration scenario is covered by `BasicStaticFilesResource_SvlPathOverride_Test#a02`. See `todo/FINISHED-101-mixin-svl-paths.md` for the full audit.
