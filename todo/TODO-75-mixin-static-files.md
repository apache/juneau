# TODO-75: Static-files mixin (`BasicStaticFilesResource`)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23.

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

- [ ] Mixin form serves a file from a JAR classpath resource at `/static/foo.css` with proper MIME type and `Cache-Control` headers.
- [ ] Mixin form serves the same file at `/htdocs/foo.css` (multi-mount via the default `paths`).
- [ ] GET on a missing path returns `404 Not Found` (not 500).
- [ ] `HEAD /static/foo.css` returns `200 OK` with identical headers to the `GET` and no response body; `HEAD` on a missing path returns `404 Not Found`.
- [ ] Importer's `@Bean StaticFiles` overrides the default `BasicStaticFiles` configuration.
- [ ] Default classpath base searches both `static/` and `htdocs/` directories out of the box; importer can override via `@Bean StaticFiles`.
- [ ] Path override via TODO-73 (`@Rest(paths={"/assets/*"})` or `@Rest(paths={"$C{static.paths}"})`) reroutes the mount cleanly.
- [ ] `Cache-Control: max-age=86400, public` (the `BasicStaticFiles` default) is preserved end-to-end; importer can override via `BasicStaticFiles.create().headers(...)`.
- [ ] 404 body format flows through the existing exception-rendering chain (RFC 7807 problem-details when FINISHED-61 opt-in is active; plain text otherwise) — no special-case handling in the mixin.
- [ ] Published OpenAPI spec from `BasicOpenApiResource` / `BasicSwaggerResource` does NOT include the `/static/*` route when `BasicStaticFilesResource` is mounted alongside them.
- [ ] No regression in `juneau-examples-rest` static-file behavior after the migration.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Coverage ≥ 95% on `BasicStaticFilesResource`. Full `./scripts/test.py` green.

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
