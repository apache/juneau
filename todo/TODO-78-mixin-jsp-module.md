# TODO-78: JSP servlet support module (`juneau-rest-server-view-jsp`)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23.

## Goal

Add a new `juneau-rest-server-view-jsp` Maven module that ships a `BasicJspResource` mixin and a `JspViewRenderer` for serving `.jsp` views from the importer's classpath. Lives in its own module so the JSP runtime dependencies (Apache Jasper, `jakarta.servlet.jsp.*`, JSTL) don't bleed into the core `juneau-rest-server`. JSP is niche but real for legacy migrations into Juneau, and the user has explicitly asked for it on the roadmap.

End-state developer experience:

```java
// pom: add juneau-rest-server-view-jsp
@Rest(path="/app", mixins=BasicJspResource.class)
public class AppResource extends RestServlet {

    @Bean BasicJspResource jsp() {
        return BasicJspResource.create()
            .basePath("/WEB-INF/views/")
            .build();
    }

    // Method-level: return a View bean that the renderer maps to a JSP.
    @RestGet("/hello/{name}")
    public View hello(@Path String name) {
        return JspView.of("hello.jsp")
            .attr("name", name)
            .attr("ts", Instant.now());
    }
}

// Default mount paths serve raw .jsp resources from the classpath.
// GET /app/jsp/hello.jsp → renders /WEB-INF/views/hello.jsp
```

## Why now

- JSP is a niche but real concern for legacy server-side-rendered apps migrating into Juneau — particularly internal admin consoles and reporting tools that already have a tested `.jsp` template library and don't want to rewrite to React / Thymeleaf.
- Pre-FINISHED-72, JSP would have been hard to retrofit because integrating a `JspServlet` alongside Juneau's `RestServlet` meant fighting servlet-mapping precedence; FINISHED-72's multi-mount + mixin primitives make this clean.
- The user has explicitly named this on the roadmap and is fine with module-level POM deps for JSP runtimes.
- Doing it in a separate module preserves the "no extra deps in core" property the rest of `juneau-rest-server` maintains.

## Scope

**In scope (v1):**

- New Maven module `juneau-rest/juneau-rest-server-view-jsp/` with a `pom.xml` mirroring `juneau-rest-server-mcp` (closest sibling — small, single-purpose, opt-in module).
- `org.apache.juneau.rest.view.jsp.BasicJspResource` mixin with default `@Rest(paths={"/jsp/*"})`. Single `@RestGet("/{path:.*}")` handler that:
    - Reads the configured base-path (default `/`).
    - Looks up `<basePath>/<path>` from the importer's classpath via `getContext().getResourceSupplier()`.
    - Forwards the request to the embedded JSP engine via `ServletContext.getRequestDispatcher(...).forward(...)`.
- `org.apache.juneau.rest.view.jsp.JspView` value-class — a `View` bean carrying `(templateName, Map<String,Object> attributes)`. `@RestOp` methods can return `JspView` directly and the framework's `ResponseProcessor` chain dispatches to the renderer.
- `org.apache.juneau.rest.view.jsp.JspViewRenderer` `ResponseProcessor` impl — handles `JspView` returns by setting request attributes and forwarding to the JSP engine.
- POM dependencies (in `provided` scope so the consumer pulls the version they want):
    - `org.glassfish.web:jakarta.servlet.jsp.jstl` — JSTL runtime.
    - Jetty EE10 JSP engine (`org.eclipse.jetty.ee10:jetty-ee10-apache-jsp`) — to align with the rest of the Jetty stack used in `juneau-microservice-jetty`. **Confirm exact version pinning at land time.**
- New example module: `juneau-examples/juneau-examples-rest-jetty-jsp/` with a Hello-World JSP rendering at `/jsp/hello.jsp`.
- Tests in the new module + `juneau-examples-rest-jetty-jsp` exercising both `JspView`-returning and raw-`.jsp`-serving paths.

**Explicitly out of scope (v1):**

- JSF (JavaServer Faces) — separate concern; can ship as `juneau-rest-server-view-jsf` if ever requested.
- Custom tag libraries beyond JSTL — users supply their own taglibs via classpath dependencies.
- Server-side-includes (SSI) — separate concern, not commonly mixed with JSP.
- Pre-compiled JSP classfile loading (skipping the JSP engine entirely) — performance optimization, defer to v2.
- JSP fragment includes (`.jspf`) under arbitrary paths — supported transparently by the JSP engine if used inside a `.jsp` template, but not specially routed.

## Dependency-injection notes

- **Mixin instance resolution.** `BasicJspResource` is instantiated via the FINISHED-72 mixin walk: `BeanStore.getBean(BasicJspResource.class)` first, no-arg constructor reflection fallback. Both microservice (`BasicBeanStore`) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths use this lookup verbatim — no new plumbing required.
- **Builder-time configuration sourcing.** The mixin reads two builder-time inputs:
    - **Base path** (default `/`). Microservice: `@Bean BasicJspResource jsp() { return BasicJspResource.create().basePath("/WEB-INF/views/").build(); }`. Spring Boot: identical, in a `@Configuration`.
    - **`ServletContext`**. Required at request time for `getRequestDispatcher(...).forward(...)`. Resolved from `RestRequest.getServletContext()` — works identically under both DI paths because the `ServletContext` is a runtime-supplied servlet-API object, not a bean.
    - The `JspViewRenderer` `ResponseProcessor` is registered via `@Rest(responseProcessors={JspViewRenderer.class})` on `BasicJspResource` itself, or by the user adding it to their own resource's `responseProcessors` list. **Recommend the mixin auto-registers the renderer** so callers who add the mixin don't have to remember to also add the renderer.
- **Spring-Boot-specific gotchas.** This mixin has the most Spring-Boot-specific risk surface of any in this batch. Calling them out explicitly:
    - **Spring Boot embedded JSP support is notoriously finicky.** Spring Boot's reference docs explicitly recommend Thymeleaf / FreeMarker / Mustache *over* JSP for embedded servlet containers. JSP works under embedded Tomcat but historically required `war` packaging (not `jar`); embedded Jetty support has improved but still has gotchas. Mitigation: ship the mixin with a Jetty-EE10 baseline (matches `juneau-microservice-jetty`); add a `BasicJspResource_NotSupported_Test` that fails loudly with a documented diagnostic message if no JSP engine is on the classpath.
    - **Spring Boot fat-jar packaging.** A Spring Boot fat jar (`spring-boot-maven-plugin` repackaged) does not place `.jsp` files where the embedded JSP engine expects them by default. The user must put `.jsp` resources under `src/main/resources/META-INF/resources/WEB-INF/views/...` (the `META-INF/resources/` prefix is a Servlet 3.0 convention that Spring Boot honors for embedded servlets). The `BasicJspResource` documentation must call this out explicitly; the example app demonstrates the layout.
    - **`spring-boot:run` (Maven plugin) vs deployed jar.** `spring-boot:run` reads from `src/main/resources/` and `src/main/webapp/` directly; the deployed jar reads from the classpath via `META-INF/resources/`. Both modes must work. **Recommend** the example app uses `META-INF/resources/WEB-INF/views/` as the canonical layout — works under both modes.
    - **`@Configuration`-class registration of `BasicJspResource`.** Standard pattern; no special wiring beyond what the `SpringBeanStore` already does. Smoke-tested in Phase 5.
    - **`@Primary`/`@Qualifier`.** Single-instance pattern recommended; multiple `BasicJspResource` beans with different base paths require separate resources via TODO-73 path overrides. Document.
- **Acceptance bullet** added below: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm Jetty version in use across `juneau-microservice-jetty` and `juneau-examples-rest-jetty`. Inspect parent `pom.xml`s for the canonical Jetty `jetty.version` property.
2. Confirm Spring Boot's embedded JSP story for the current supported Spring Boot version (3.x, Jakarta EE 10) — what's the recommended `META-INF/resources/` layout for `.jsp` files? Verify against current Spring docs.
3. Confirm Juneau's `ResponseProcessor` chain accepts a `JspView`-typed return value via the standard precedence (matches by type before falling through to default serializers).
4. Confirm `RestRequest.getServletContext().getRequestDispatcher(...).forward(...)` works from inside an `@RestOp` method when the surrounding container has a JSP engine registered.

### Phase 1 — module skeleton + POM

1. New module `juneau-rest/juneau-rest-server-view-jsp/` with `pom.xml` mirroring `juneau-rest-server-mcp`.
2. POM dependencies in `provided` scope: `org.eclipse.jetty.ee10:jetty-ee10-apache-jsp`, `org.glassfish.web:jakarta.servlet.jsp.jstl`. Confirm versions align with the rest of the Jetty stack at land time.
3. Module skeleton compiles + ships an empty test to verify the build.

### Phase 2 — `BasicJspResource` mixin

1. New class with default `@Rest(paths={"/jsp/*"})` and `@RestGet("/{path:.*}")` handler.
2. Builder accepts `basePath(String)`.
3. Forwarding logic: `req.getServletContext().getRequestDispatcher(basePath + path).forward(req.getHttpServletRequest(), res.getHttpServletResponse())`.
4. Tests:
    - `BasicJspResource_RawJsp_Test` — GET `/jsp/hello.jsp` renders the JSP from `META-INF/resources/WEB-INF/views/hello.jsp` correctly.
    - `BasicJspResource_BasePath_Test` — `basePath("/views/")` resolves correctly.
    - `BasicJspResource_NotFound_Test` — missing JSP → 404.

### Phase 3 — `JspView` + `JspViewRenderer`

1. New value class `JspView` (`templateName`, `Map<String,Object> attributes`).
2. New `ResponseProcessor` impl `JspViewRenderer` that detects `JspView` returns, copies attributes onto the request, and forwards to the JSP engine.
3. Auto-register `JspViewRenderer` via `@Rest(responseProcessors={JspViewRenderer.class})` on `BasicJspResource`.
4. Tests:
    - `JspView_Test` — `@RestGet` method returning `JspView.of("hello.jsp").attr("name","world")` renders with the attribute available as `${name}` in the JSP.
    - `JspViewRenderer_NoEngine_Test` — when no JSP engine is on the classpath, the renderer fails with a clear diagnostic naming the missing dependency.

### Phase 4 — example module

1. New module `juneau-examples/juneau-examples-rest-jetty-jsp/` mirroring `juneau-examples-rest-jetty` structure.
2. Hello-World JSP at `META-INF/resources/WEB-INF/views/hello.jsp` rendered by `@RestGet("/hello/{name}") public View hello(@Path String name)`.
3. Tests:
    - `JuneauJspExample_Test` — end-to-end smoke test through the example.

### Phase 5 — Spring Boot smoke tests

1. New tests in the JSP module exercising the Spring `BeanStore` adapter.
2. Tests:
    - `BasicJspResource_Springboot_Test` — register `BasicJspResource` as a Spring `@Bean`, mount via `JuneauRestInitializer`, GET `/jsp/hello.jsp` renders identically to the microservice form.
    - `BasicJspResource_SpringbootMetaInfResources_Test` — confirm the `META-INF/resources/WEB-INF/views/` layout works under Spring Boot's embedded Jetty.
    - `BasicJspResource_SpringbootFatJar_Test` — repackaged fat-jar smoke test (may run only on a CI matrix that builds the jar; document if local-only run).
    - `JspViewRenderer_Springboot_Test` — `@RestGet` returning `JspView` works under the Spring `BeanStore` adapter.

### Phase 6 — docs + release notes

1. Release-notes entries under `### juneau-rest-server-view-jsp (new module)` and a cross-reference under `### juneau-rest-server-springboot`.
2. New topic page `docs/pages/topics/JspViewSupport.md` walking through both microservice and Spring Boot setup, including the `META-INF/resources/` layout caveat.
3. Update `juneau-examples` index to include the new example module.
4. Module-addition entry in the 9.5.0 → 9.6.0 (or current release) release-notes file.

## Acceptance criteria

- [ ] New module `juneau-rest-server-view-jsp` builds and tests pass standalone.
- [ ] Example app `juneau-examples-rest-jetty-jsp` renders a JSP at `/jsp/hello.jsp` from a JAR classpath resource.
- [ ] `JspView`-returning `@RestGet` method dispatches via `JspViewRenderer` correctly.
- [ ] No JSP engine on classpath → clear diagnostic error message naming the missing dependency.
- [ ] `META-INF/resources/WEB-INF/views/` layout works under both microservice (Jetty) and Spring Boot deployments.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Coverage ≥ 95% on the new module's main code. Full `./scripts/test.py` green.

## Open questions

1. **Module name — `juneau-rest-server-jsp` vs `juneau-rest-server-view-jsp` (anticipating `-view-thymeleaf`, `-view-mustache` in future)?** **Recommend `juneau-rest-server-view-jsp`** — forward-compatible naming for a future view-renderer module family. Cleaner convention than retrofitting later.
2. **Jetty version pinning.** Does the JSP engine pin a Jetty 10/11/12 version that conflicts with `juneau-microservice-jetty`? **Recommend align with Jetty 12 EE10** (`org.eclipse.jetty.ee10:jetty-ee10-apache-jsp`) to match the rest of the stack. Confirm at land time by inspecting the parent POM.
3. **Spring Boot support — best-effort or first-class?** **Recommend first-class** — ship `BasicJspResource_Springboot_Test` and the example app with a Spring Boot variant. The Spring Boot path has more risk surface than the microservice one but the user's note says they're fine with the module-level POM deps.
4. **Auto-register `JspViewRenderer` on the mixin?** **Recommend yes** — `@Rest(responseProcessors={JspViewRenderer.class})` on `BasicJspResource` itself, so callers who add the mixin don't have to remember to also add the renderer. Reduces boilerplate without added magic.
5. **Default base path.** `/` (recommended) vs `/WEB-INF/views/` vs `/META-INF/resources/WEB-INF/views/`. **Recommend `/`** — most flexible default; users who want `/WEB-INF/views/` set it explicitly. The example app demonstrates the convention.
6. **Should `JspView` extend a generic `View` interface (anticipating future `ThymeleafView`, `MustacheView`)?** **Recommend yes** — minor design effort up front prevents a breaking change later. The `View` interface lives in core `juneau-rest-server` (so future view modules don't all have to depend on the JSP module just for the interface), with the `JspView` impl in the JSP module.
7. **Multi-base-path support.** A user with `/views/` and `/admin/views/` JSPs — multiple `BasicJspResource` beans, each with its own `paths` override and `basePath`. **Recommend documented and tested**, no special builder support beyond the standard "register two beans" pattern.

## Risks

- **Spring Boot JSP support flakiness.** Spring Boot's official position is "we recommend you don't use JSP under embedded containers." Real-world reports show it works but with sharp edges (fat-jar packaging, classpath resource resolution, exploded-vs-jar variance). Mitigation: explicit Spring Boot smoke tests; document loudly; if Phase 5 surfaces gotchas not foreseen here, escalate (call out as a follow-up TODO rather than silently shipping a broken-on-Spring-Boot mixin).
- **Jetty version drift.** A future Jetty upgrade in `juneau-microservice-jetty` could break the JSP module if the JSP engine doesn't keep pace. Mitigation: dependency-pinning + explicit version test in CI; document that the JSP module is tested against the same Jetty version as `juneau-microservice-jetty`.
- **`provided`-scope deps confusion.** Users who add the JSP module without also explicitly adding the JSP runtime (because the deps are `provided`) will see runtime `ClassNotFoundException`. Mitigation: javadoc + a `JspViewRenderer_NoEngine_Test` that confirms the diagnostic message is human-readable.
- **`forward()`-based dispatch eats the response cleanly.** `RequestDispatcher.forward()` resets the response output stream; if Juneau has already written response headers (e.g. via a `RestPreCall` hook), the forward may fail or produce malformed responses. Mitigation: document the constraint; the `JspViewRenderer` runs at response-resolution time so most user code never hits this, but `RestPreCall` interactions need testing.
- **CSP / security headers.** JSP-rendered HTML may need different `Content-Security-Policy` than Juneau's default JSON responses. Mitigation: out of scope for v1; document the integration point with TODO-69 / future security-headers TODOs.
- **JSTL version skew across Jakarta EE 10 / 11.** `jakarta.servlet.jsp.jstl` artifact coordinates have shifted. Mitigation: pin to the EE10 coordinates that match Jetty 12; document.
- **Module-level POM dep complexity.** Adding a new module is non-trivial — parent `pom.xml` updates, BOM updates, release-process touch-points. Mitigation: model after `juneau-rest-server-mcp` end-to-end; checklist in the doc page.

**Spring Boot risk callout (per amendment instructions).** The Spring Boot path here has notably more risk surface than any of the other five mixins in this batch. If Phase 5 smoke testing surfaces issues that require non-trivial workarounds (e.g. needing to ship a custom `JspServletConfigurer` to coax embedded Tomcat/Jetty into finding the JSPs), **call those out as a follow-up TODO** rather than silently fold them in — the user's amendment specifically asked for this kind of escalation rather than a silent "we made it work somehow."

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives.
- `todo/TODO-73-rest-paths-runtime-override.md` (sibling, soft dependency) — runtime path override lets users move `/jsp/*` to `/views/*` without subclassing.
- `todo/TODO-75-mixin-static-files.md` (sibling) — static-files mixin coexists with JSP rendering; document the path-precedence interaction.
- `juneau-rest/juneau-rest-server-mcp/` — sibling small-single-purpose-opt-in module; this TODO mirrors its POM and module-skeleton shape.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter; Phase 5 smoke-test target. The most-at-risk DI path for this TODO.
- `juneau-microservice/juneau-microservice-jetty/` — the Jetty-version-pinning reference for the JSP engine alignment.
- `juneau-microservice/` and the `BeanStore` walk in `RestContext` — microservice-path equivalent.
- `juneau-examples/juneau-examples-rest-jetty/` — the structural reference for the new `juneau-examples-rest-jetty-jsp` example.
