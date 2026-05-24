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
- Today, static-file serving requires either inheriting from `BasicRestServlet` or hand-rolling a `@RestGet("/static/{file:.*}")` method that delegates to `StaticFiles`. Neither surfaces the multi-prefix pattern cleanly.
- TODO-74 (api-docs mixin) ships Swagger-UI / Redoc static assets through the same `StaticFiles` infrastructure — landing this mixin cleanly establishes the boundary between "REST handlers" and "blob-serving" so the api-docs mixin can lean on it without duplication.

## Scope

**In scope (v1):**

- New class `org.apache.juneau.rest.staticfile.BasicStaticFilesResource` (servlet-class form) with default `@Rest(paths={"/static/*","/htdocs/*"})`.
- Single `@RestGet("/{file:.*}")` (greedy) handler that delegates to the active `StaticFiles` impl from the bean store, returning the `HttpResource` directly with `Cache-Control` headers honored.
- Builder-driven configuration via the existing `BasicStaticFiles` builder — the mixin reads `BeanStore.getBean(StaticFiles.class)` at request time so the importer's `@StaticFile` declarations + classpath defaults are picked up.
- 404 behavior identical to today: missing files surface as `NotFound` thrown from the handler.
- Updated default examples / `juneau-examples-rest` to lean on the mixin in place of inherited static-file mounts.
- Tests in `juneau-utest`: GET hit, GET miss → 404, `Cache-Control` headers, importer's `@StaticFile` overrides default, multi-mount works.

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
4. Greedy path-variable `{file:.*}` — confirm Juneau's path matcher supports the regex syntax for paths like `/static/css/styles.css`.

### Phase 1 — `BasicStaticFilesResource` mixin

1. New class `org.apache.juneau.rest.staticfile.BasicStaticFilesResource` with default `@Rest(paths={"/static/*","/htdocs/*"})`.
2. Single `@RestGet(path="/{file:.*}")` handler that:
    - Reads `getContext().getStaticFiles()`.
    - Calls `staticFiles.resolve(file, request.getLocale()).orElseThrow(NotFound::new)`.
    - Returns the `HttpResource` directly so `Cache-Control` and `Content-Type` headers from `BasicStaticFiles` flow through.
3. Tests:
    - `BasicStaticFilesResource_Test` — GET hit, GET miss, multi-mount via `paths`.
    - `BasicStaticFilesResource_CacheControl_Test` — `Cache-Control: max-age=86400, public` is preserved end-to-end.
    - `BasicStaticFilesResource_ImporterOverride_Test` — importer's `@Bean StaticFiles` overrides the default.

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
- [ ] Importer's `@Bean StaticFiles` overrides the default `BasicStaticFiles` configuration.
- [ ] Path override via TODO-73 (`@Rest(paths={"/assets/*"})` or `@Rest(paths={"$C{static.paths}"})`) reroutes the mount cleanly.
- [ ] No regression in `juneau-examples-rest` static-file behavior after the migration.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Coverage ≥ 95% on `BasicStaticFilesResource`. Full `./scripts/test.py` green.

## Open questions

1. **Wildcard pattern semantics — `/static/*` (servlet mapping) vs `/static/{file:.*}` (Juneau path-variable)?** **Recommend `/static/*` in the `paths` member** since `paths` are servlet-mapping patterns (Jetty multi-pattern mount), and `/{file:.*}` for the inner `@RestGet` path variable since that's Juneau's path matcher. The two layers are different — keep the conventions distinct.
2. **Default classpath base — `static/` vs `htdocs/` vs both?** **Recommend configurable, default to both** — matches `BasicStaticFiles`'s existing zero-arg constructor that already searches both directories.
3. **404 body format.** Default `NotFound` exception body is plain text; should the mixin emit JSON when `apiFormat`/`Accept` indicates? **Recommend let the existing exception-rendering chain handle it** — RFC 7807 problem-details (FINISHED-61) already kicks in if the resource opts in.
4. **Cache-Control default.** `max-age=86400, public` (1 day) is `BasicStaticFiles`'s built-in default. **Recommend keep the same default** — apps with stricter caching needs override via `BasicStaticFiles.create().headers(...)`.
5. **HEAD request handling.** Should the mixin handle `HEAD /static/foo.css` so callers can probe existence without downloading the body? **Recommend yes** — Juneau's `@RestGet` already supports HEAD via the standard servlet contract; verify in tests.
6. **Should the mixin opt out of `apiFormat="openapi"`-driven OpenAPI emission for the static-file path?** A greedy `/{file:.*}` mount is not API-meaningful. **Recommend yes, exclude via `@OpenApi(hidden=true)` or equivalent on the mixin's handler** — keeps the published OpenAPI spec clean.

## Risks

- **Path collision with TODO-74's api-docs mixin.** Swagger-UI and Redoc ship JS/CSS assets via `BasicStaticFiles`; if a user mixes both `BasicApiDocsResource` and `BasicStaticFilesResource` and the static-files mixin's catch-all `/static/*` shadows a Swagger asset URL, the docs render breaks. Mitigation: test the cross-product (`BasicApiDocsResource_StaticFilesCoexistence_Test`).
- **Greedy `{file:.*}` matching unexpected requests.** A `paths={"/api/*"}` user who also mounts `BasicStaticFilesResource` via `paths={"/api/static/*"}` could see `/api/foo` resolve to `/static/foo` if mount precedence is misconfigured. Mitigation: explicit-match paths (FINISHED-72) prevent this; document loudly.
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
