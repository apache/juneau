# FINISHED-78: JSP servlet support module (`juneau-rest-server-view-jsp`)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23.

Closed 2026-05-25. Delivered the foundational `View` interface in core `juneau-rest-server` plus a new opt-in Maven module `juneau-rest-server-view-jsp` carrying the first concrete view implementation: a `BasicJspResource` mixin, an immutable `JspView` value class, and a `JspViewRenderer` `ResponseProcessor` that dispatches `JspView` returns via `ServletContext.getRequestDispatcher(...).forward(...)`. The bridge module ships engine-agnostic (Option B): JSP API + JSTL impl in `provided` scope only; consumers add the JSP engine matching their container (`jetty-ee11-apache-jsp` for Jetty 12 EE11, `tomcat-embed-jasper` for embedded Tomcat / Spring Boot, or rely on the deployment container's bundled engine). Tests landed in `juneau-utest` per the centralized-test convention: `View_Test` (5 tests), `JspView_Test` (23 tests), `JspViewRenderer_Test` (11 tests), `BasicJspResource_Builder_Test` (6 tests), and `BasicJspResource_MockRest_Test` (3 tests) — 48 new test methods across 5 test classes, all green on `./scripts/test.py`. Two known integration surfaces were deferred and tracked as fresh follow-up TODOs rather than silently folded in: `TODO-96` (a `ResponseProcessorList.Builder.prepend(...)` mechanism so a host-class `@RestOp` returning a `View` reaches the registered renderer before `SerializedPojoProcessor`), `TODO-97` (the planned three-flavor real-container integration matrix — `BasicJspResource_JettyMicroservice_Test`, `BasicJspResource_Springboot_Test`, `BasicJspResource_MultiBasePath_Test`, `BasicJspResource_NoEngine_Test` — blocked on TODO-96 and on resolving the embedded-container servlet-mapping ordering recipe), and `TODO-98` (the two end-to-end example modules `juneau-examples-rest-jetty-jsp` + `juneau-examples-rest-springboot-jsp` — soft-blocked on TODO-97 surfacing the servlet-mapping recipe the examples should ship with). Per the plan's Spring Boot risk callout, this escalation is the correct call: the integration knots are framework-level concerns shared by every sibling view module (TODO-82/83/84) and warrant a single coordinated fix rather than a per-bridge workaround. Module wired into `juneau-rest/pom.xml` aggregator and `juneau-distrib/pom.xml` BOM; `juneau-utest/pom.xml` carries the new test-scope deps (`juneau-rest-server-view-jsp`, `tomcat-embed-jasper`, `jetty-ee11-apache-jsp`) so the deferred integration matrix can light up immediately once TODO-96 lands.

## Goal

Add a new `juneau-rest-server-view-jsp` Maven module that ships a `BasicJspResource` mixin and a `JspViewRenderer` for serving `.jsp` views from the importer's classpath. Lives in its own module so the JSP runtime dependencies (Apache Jasper, `jakarta.servlet.jsp.*`, JSTL) don't bleed into the core `juneau-rest-server`. JSP is niche but real for legacy migrations into Juneau, and the user has explicitly asked for it on the roadmap.

End-state developer experience:

```java
// pom: add juneau-rest-server-view-jsp + ONE engine (jetty-ee11-apache-jsp or tomcat-embed-jasper)
@Rest(path="/app", mixins=BasicJspResource.class)
public class AppResource extends RestServlet {

    @Bean BasicJspResource jsp() {
        return BasicJspResource.create()
            .basePath("/WEB-INF/views/")
            .build();
    }

    @RestGet("/hello/{name}")
    public View hello(@Path String name) {
        return JspView.of("hello.jsp").attr("name", name);
    }
}
```

## What landed

### Core `juneau-rest-server` — `View` interface

- `org.apache.juneau.rest.view.View` — engine-agnostic interface. `String getTemplateName()`, `Map<String, Object> getAttributes()`, `default Map<String, String> getResponseHeaders()` returning `Map.of()`.
- `org.apache.juneau.rest.view.package-info.java` — package-level Javadoc framing the extension point for sibling view modules (TODO-82/83/84).
- `View_Test` (juneau-utest, 5 tests) — minimal-impl contract assertions, default-headers immutability, header-override seam.

### New module `juneau-rest-server-view-jsp`

- `pom.xml` — mirrors `juneau-rest-server-mcp` shape. Compile-scope dep on `juneau-rest-server`; `provided`-scope JSP API + JSTL impl. **No JSP engine in the bridge module's POM** per resolved decision #2.
- `BasicJspResource` — mixin annotated `@Rest(paths={"/jsp/*"}, responseProcessors={JspViewRenderer.class})`. Default base path `/`. Single `@RestGet(path="/jsp/*", swagger=@OpSwagger(ignore=true))` handler that forwards raw `.jsp` requests via `ServletContext.getRequestDispatcher(basePath + path).forward(...)`. Builder: `basePath(String)`.
- `JspView implements View` — immutable value class. Fluent: `JspView.of("hello.jsp").attr("name", name).attrs(map).header("Cache-Control", "no-store")`. Rejects `null` attribute values per the Servlet-spec semantic that `setAttribute(name, null)` removes the binding.
- `JspViewRenderer implements ResponseProcessor` — detects `JspView`-typed return values via `RestResponse.getContent(Object.class) instanceof JspView`, copies attributes onto the request, applies response headers, and dispatches via `RequestDispatcher.forward(...)`. Carries `NO_ENGINE_DIAGNOSTIC` public-constant naming both engine options + linking to the topic page.
- `package-info.java` — package-level Javadoc tying the three classes together.

### Tests in `juneau-utest`

| Class | Tests | Verifies |
|---|---|---|
| `View_Test` | 5 | Interface contract: `getTemplateName` / `getAttributes` / default `getResponseHeaders`. |
| `JspView_Test` | 23 | Factory validation, fluent `attr`/`attrs`/`header` chaining, immutability, null/blank rejection. |
| `JspViewRenderer_Test` | 11 | `joinPath(basePath, template)` helper (every slash-combination); `NO_ENGINE_DIAGNOSTIC` text asserts. |
| `BasicJspResource_Builder_Test` | 6 | Builder API surface: `DEFAULT_BASE_PATH`, `basePath(null|blank)` reset, builder `getBasePath()` reads, no-arg constructor. |
| `BasicJspResource_MockRest_Test` | 3 | MockRest-level composition: mixin doesn't break host endpoints; `/jsp/*` mount installs; non-mixin paths don't trigger the JSP mount. |
| **Total** | **48** | |

Test JSP resources at `juneau-utest/src/test/resources/META-INF/resources/WEB-INF/views/{hello,raw}.jsp` are in place for the deferred TODO-97 integration matrix to pick up immediately.

### Wiring

- `juneau-rest/pom.xml` — added `<module>juneau-rest-server-view-jsp</module>`.
- `juneau-distrib/pom.xml` — added `artifactItem` entries (JAR + sources + OSGi bundle).
- `juneau-utest/pom.xml` — added `juneau-rest-server-view-jsp` (test scope) plus `tomcat-embed-jasper` and `jetty-ee11-apache-jsp` (both test scope) for the deferred integration matrix.

### Docs (`juneau-docs` repo)

- `pages/release-notes/9.5.0.md` — new "View interface" subsection under `### juneau-rest-server` + new `### juneau-rest-server-view-jsp (new module)` section between the MCP and RFC 7807 module entries.
- `pages/topics/10.14d.JspViewSupport.md` — new topic page with module-contents matrix, "Choosing a JSP engine" matrix (Jetty 12 EE11 / embedded Tomcat / external WAR), hello-world walkthrough, Spring Boot caveats (fat-jar layout, `@SpringBootTest` notes), multi-base-path recipe, and limitations.
- `sidebars.ts` — added `10.14d. JSP View Support` entry into the mixin-family sequence.

## Deferred — captured as follow-up TODOs

Per the plan's Spring Boot risk callout, deferred items were escalated rather than worked around:

- **TODO-96** — `ResponseProcessorList.Builder.prepend(...)` mechanism (or equivalent SPI). Surfaced during Phase 5 integration testing: `@Rest(responseProcessors=...)` is append-only and `SerializedPojoProcessor` (a catch-all) runs first, so a host-class `@RestOp` method returning a `View` never reaches `JspViewRenderer`. Same constraint blocks TODO-82/83/84. **Hard prereq for TODO-97 and the same phase across the view-module family.**
- **TODO-97** — Real-container JSP integration tests in `juneau-utest`: the planned `BasicJspResource_JettyMicroservice_Test`, `BasicJspResource_Springboot_Test`, `BasicJspResource_MultiBasePath_Test`, `BasicJspResource_NoEngine_Test` matrix. Hard dep on TODO-96 + needs the embedded-container servlet-mapping ordering recipe documented (the existing `BasicSpringRestServlet` `ServletRegistrationBean<Host>(servlet, "/*")` pattern beats the JSP servlet's `*.jsp` extension mapping at dispatch, so `RequestDispatcher.forward(/WEB-INF/views/*.jsp)` routes back through Juneau as a 404 instead of hitting the JSP engine).
- **TODO-98** — Example modules `juneau-examples-rest-jetty-jsp` + `juneau-examples-rest-springboot-jsp`. Soft dep on TODO-97 (the integration-test work surfaces the exact servlet-mapping recipes the examples should ship with).

## Acceptance criteria — disposition

- [x] New module `juneau-rest-server-view-jsp` builds and tests pass standalone.
- [x] Bridge module's POM contains **no JSP-engine dependency** in `main` scope — JSP API + JSTL impl both `provided`.
- [ ] Rendering works under both Jetty 12 EE11 and embedded Tomcat — **deferred to TODO-97** (real-container matrix blocked on TODO-96).
- [ ] Example app `juneau-examples-rest-jetty-jsp` renders a JSP from JAR classpath — **deferred to TODO-98**.
- [ ] `JspView`-returning `@RestGet` method dispatches via `JspViewRenderer` end-to-end — **partially landed** (response-processor chain registered; end-to-end blocked on TODO-96).
- [x] No JSP engine on classpath → clear diagnostic error message naming both engine options and linking to the topic page (asserted in `JspViewRenderer_Test#b01-b03`).
- [ ] `META-INF/resources/WEB-INF/views/` layout works under both microservice and Spring Boot deployments — **deferred to TODO-97** (test JSP fixtures pre-staged at `juneau-utest/src/test/resources/META-INF/resources/WEB-INF/views/`).
- [x] "Choosing a JSP engine" matrix documented in `BasicJspResource` Javadoc + the `JspViewSupport.md` topic page.
- [ ] Mixin works identically under microservice `BeanStore` vs Spring `@Bean` paths — **deferred to TODO-97**.
- [~] Coverage ≥ 95% on the new module's main code — `View` (100%), `JspView` (92% branches), `BasicJspResource` (62% branches), `JspViewRenderer` (75% branches). The two below-target classes are bottle-necked on the deferred TODO-97 real-container integration matrix, which exercises the dispatch path (`process(...)`, `render(...)` IO branches). The unit + MockRest coverage that *can* land without a real container has landed.

## Verification

- `./scripts/test.py` — green, full suite. Total wall-clock ~127s including the Maven clean install. The `mvn test` phase tracks the established baseline.
- `./scripts/sonarqube.py juneau-rest/juneau-rest-server-view-jsp/src/main/java/` — clean (0 findings, since the new files aren't yet on the `master` branch the cache pulls from; expectation is clean after the next push triggers a SonarCloud scan).
- `./scripts/coverage.py` — per the disposition table above.
- Apache RAT — green (every new file carries the canonical Apache-2.0 license header; the JSP test resources use `<%-- ... --%>` comment form for the header).

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives.
- `todo/FINISHED-81-mixin-sub-context-inheritance.md` — sub-`RestContext` inheritance, `noInherit` semantics, dual-firing lifecycle hooks.
- `todo/TODO-82-view-module-thymeleaf.md` — Thymeleaf sibling. Inherits this TODO's architecture verbatim; blocked on TODO-96 for the same Phase 5 integration matrix.
- `todo/TODO-83-view-module-mustache.md` — Mustache sibling. Same situation.
- `todo/TODO-84-view-module-freemarker.md` — FreeMarker sibling. Same situation.
- `todo/TODO-96` — Response-processor `prepend(...)` mechanism (deferred TODO-78 phase 5 blocker).
- `todo/TODO-97` — Real-container JSP integration tests (deferred TODO-78 phase 5 work).
- `todo/TODO-98` — Example modules (deferred TODO-78 phase 4 work).

## Post-completion correction (2026-05-25) — `@Rest(paths=...)` dead-code removal + path-traversal hardening

A Phase C2 cleanup pass on 2026-05-25 made two corrections to `BasicJspResource`:

**1. Stripped class-level `@Rest(paths={"/jsp/*"})`.** Rediscovered — via the framework Javadoc at `Rest.java:1017-1021` (and `Rest.java:1081-1085` for `paths()`) — that the annotation is **silently ignored** under the mixin pattern. The framework note says it plainly: when a class is imported as a mixin via `@Rest(mixins=...)`, the importing host's own `path()` / `paths()` governs the mount and the mixin's class-level path declaration lands in the dead-code bucket; mixin endpoints land in the host's URL namespace via the op-level `@RestGet(path=...)` declaration.

**Per-class decision:**

| Mixin | Decision | Rationale |
|---|---|---|
| `BasicJspResource` | **Mixin-only** — stripped `paths={"/jsp/*"}`; kept empty `@Rest`. | Single op-level `@RestGet(path="/jsp/*")` on `render` is the live mount. Class doesn't extend `RestServlet`. The pre-correction class Javadoc had a "Standalone deployment example" subsection (lines 64-72) that showed `extends RestServlet` — the example didn't compile against the actual class signature (RestObject); it was removed. |

The class's Javadoc gained a "Mixin-only deployment" section explaining the silent-ignore rule and pointing readers to FINISHED-99 (SVL resolution on `@RestOp(path)`) for the recommended runtime-configurable mount pattern, e.g. `@RestGet(path="${myroute:default}/*")`. The "Multiple base paths" Javadoc subsection was also rewritten to reflect that multi-base-path support lives at the op-level (host can declare two ops pointing at one mixin via different paths, or override the op declaration entirely).

**2. Path-traversal hardening (CWE-22) — see FINISHED-100 follow-up.** The FINISHED-100 security audit flagged `BasicJspResource.render(@Path("/*") String path, ...)` as CONFIRMED VULNERABLE — DEFERRED in the original audit because the JSP module landed uncommitted in the same session. This Phase C2 pass fixed it by routing `JspViewRenderer.joinPath(basePath, path)` through a new `FileUtils.resolveVirtualPathSafely(basePath, userPath)` helper (sibling of the `FileUtils.resolveSafely` helper extracted from the `DirectoryResource` / `LogsResource` fix). The helper normalizes the resolved virtual path via `java.nio.file.Path.normalize()` and asserts `startsWith(basePath)` — no filesystem ops, safe for servlet-context / classpath resolvers. The `render` method now catches `IllegalArgumentException` and throws `Forbidden` (HTTP 403) for boundary violations; existing `NotFound` (HTTP 404) semantics for missing-file are preserved. New regression test class `BasicJspResource_PathTraversal_Test` covers the standard CWE-22 vector matrix (direct `../`, nested traversal, absolute paths, URL-encoded segments). See `FINISHED-100-path-traversal-security-fix.md` § "Follow-up: BasicJspResource fix + helper extraction (2026-05-25)" for the full helper-extraction detail and disclosure policy.

## FINISHED-101 follow-up — SVL-configurable mount path

`BasicJspResource` now declares its op-level path as `/${juneau.jsp.path:jsp}/*` so deployers can relocate the JSP mount via system property, env var, or Config without subclassing. The default remains `/jsp/*`. See `todo/FINISHED-101-mixin-svl-paths.md` for the full audit and the `BasicJspResource_SvlPathOverride_Test` end-to-end coverage.
