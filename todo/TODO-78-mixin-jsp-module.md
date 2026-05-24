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
- `org.apache.juneau.rest.view.jsp.BasicJspResource` mixin with default `@Rest(paths={"/jsp/*"})`. Single `@RestGet("/*")` handler (with `@Path("/*") String path` capturing the multi-segment remainder) that:
    - Reads the configured base-path (default `/`).
    - Looks up `<basePath>/<path>` from the importer's classpath via `getContext().getResourceSupplier()`.
    - Forwards the request to the embedded JSP engine via `ServletContext.getRequestDispatcher(...).forward(...)`.
- `org.apache.juneau.rest.view.jsp.JspView` value-class — a `View` bean carrying `(templateName, Map<String,Object> attributes)`. `@RestOp` methods can return `JspView` directly and the framework's `ResponseProcessor` chain dispatches to the renderer.
- `org.apache.juneau.rest.view.jsp.JspViewRenderer` `ResponseProcessor` impl — handles `JspView` returns by setting request attributes and forwarding to the JSP engine.
- POM dependencies (all `provided` scope — engine-agnostic stance per resolved decision #2):
    - `jakarta.servlet.jsp:jakarta.servlet.jsp-api` — JSP API spec (interface only; no engine).
    - `org.glassfish.web:jakarta.servlet.jsp.jstl` — JSTL runtime (standards-compliant reference impl; container-agnostic).
- **No default JSP engine ships with the bridge module.** The runtime code uses standard Servlet API (`ServletContext.getRequestDispatcher(...).forward(...)`) and is engine-agnostic; consumers add the engine matching their deployment container. Three documented choices:
    - **Jetty 12 EE10** (e.g. `juneau-microservice-jetty`, Spring Boot embedded Jetty): `org.eclipse.jetty.ee10:jetty-ee10-apache-jsp`.
    - **Embedded Tomcat** (Spring Boot default): `org.apache.tomcat.embed:tomcat-embed-jasper`.
    - **External WAR deploy** (Tomcat / JBoss / WildFly / etc.): engine already on the container's classpath; no additional dep needed.
    The module README and the `JspViewSupport.md` topic page document this "Choosing a JSP engine" matrix. The module's own test classpath supplies `tomcat-embed-jasper` in `test` scope so the bridge module can exercise its rendering path standalone; the `juneau-examples-rest-jetty-jsp` example module supplies `jetty-ee10-apache-jsp` to exercise the Jetty path.
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

1. Confirm Jetty version in use across `juneau-microservice-jetty` and `juneau-examples-rest-jetty`. Inspect parent `pom.xml`s for the canonical Jetty `jetty.version` property. Used by the example module's POM (the bridge module itself does not pin an engine — Option B).
2. Confirm Spring Boot's embedded JSP story for the current supported Spring Boot version (3.x, Jakarta EE 10) — what's the recommended `META-INF/resources/` layout for `.jsp` files? Verify against current Spring docs. Confirm `tomcat-embed-jasper` is the recommended engine for Spring Boot's embedded-Tomcat default.
3. Confirm Juneau's `ResponseProcessor` chain accepts a `JspView`-typed return value via the standard precedence (matches by type before falling through to default serializers).
4. Confirm `RestRequest.getServletContext().getRequestDispatcher(...).forward(...)` works from inside an `@RestOp` method when the surrounding container has a JSP engine registered.
5. **JSP engine inventory.** Verify the three documented engine choices (Jetty 12 EE10, embedded Tomcat, external-WAR container-supplied) work end-to-end against the bridge code. Test matrix coverage:
    - Jetty 12 EE10: `juneau-examples-rest-jetty-jsp` end-to-end test.
    - Embedded Tomcat: bridge module's own `test`-scope `tomcat-embed-jasper` dep exercises `BasicJspResource_RawJsp_Test` / `JspView_Test`.
    - External WAR: documentation-only (no CI gate; container's own integration test surface).

### Phase 1 — module skeleton + POM

1. New module `juneau-rest/juneau-rest-server-view-jsp/` with `pom.xml` mirroring `juneau-rest-server-mcp`.
2. POM dependencies (engine-agnostic per resolved decision #2):
    - `provided` scope: `jakarta.servlet.jsp:jakarta.servlet.jsp-api`, `org.glassfish.web:jakarta.servlet.jsp.jstl`.
    - **No JSP engine dep in the bridge module's main POM.** Consumers pick the engine matching their container.
    - `test` scope: `org.apache.tomcat.embed:tomcat-embed-jasper` (lets the bridge module exercise its rendering path standalone without depending on Jetty).
3. Module skeleton compiles + ships an empty test to verify the build.
4. New `BasicJspResource_NoEngine_Test` asserts the diagnostic message (Phase 3 / acceptance) when the test classpath is stripped of `tomcat-embed-jasper` (Surefire system-property toggle or a separate test-classifier — pick whichever is simpler in Phase 1).

### Phase 2 — `BasicJspResource` mixin

1. New class with default `@Rest(paths={"/jsp/*"})` and `@RestGet("/*")` handler (signature: `Object render(@Path("/*") String path, ...)` capturing the trailing remainder). **Note:** Juneau's path matcher does NOT support Spring/JAX-RS `{var:regex}` syntax for multi-segment matching — use trailing `/*` per `BasicRestServlet.getHtdoc(...)`'s pattern.
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

1. New module `juneau-examples/juneau-examples-rest-jetty-jsp/` mirroring `juneau-examples-rest-jetty` structure. **Adds `org.eclipse.jetty.ee10:jetty-ee10-apache-jsp` to its own `pom.xml`** (Jetty-flavored engine) — the bridge module is engine-agnostic under Option B, so the example explicitly picks Jetty's Jasper bundle for its embedded server.
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
- [ ] Bridge module's POM contains **no JSP-engine dependency** in `main` scope — only the JSP API (`jakarta.servlet.jsp:jakarta.servlet.jsp-api`) and JSTL impl (`org.glassfish.web:jakarta.servlet.jsp.jstl`), both `provided`. `tomcat-embed-jasper` is `test`-scope only.
- [ ] Rendering works under both Jetty 12 EE10 (verified via the example module's end-to-end test) and embedded Tomcat (verified via the bridge module's own test classpath).
- [ ] Example app `juneau-examples-rest-jetty-jsp` renders a JSP at `/jsp/hello.jsp` from a JAR classpath resource; the example's POM explicitly adds `jetty-ee10-apache-jsp`.
- [ ] `JspView`-returning `@RestGet` method dispatches via `JspViewRenderer` correctly.
- [ ] No JSP engine on classpath → clear diagnostic error message naming the missing dependency and linking to the "Choosing a JSP engine" matrix in the module README.
- [ ] `META-INF/resources/WEB-INF/views/` layout works under both microservice (Jetty) and Spring Boot deployments.
- [ ] "Choosing a JSP engine" matrix documented in the module README and the `JspViewSupport.md` topic page (Jetty / embedded Tomcat / external WAR).
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Coverage ≥ 95% on the new module's main code. Full `./scripts/test.py` green.

## Test architecture

**Decision: tests live in `juneau-utest`, following the existing codebase convention.**

Every production module under `juneau-rest/` (including `juneau-rest-server-springboot`, `juneau-rest-server-mcp`, `juneau-rest-server-rdf`, etc.) ships with an empty `src/test/`; ALL juneau-rest tests live in `juneau-utest`. The bridge module `juneau-rest-server-view-jsp` follows the same pattern. `juneau-utest/pom.xml` already pulls in `spring-boot-starter-web`, `juneau-microservice-jetty`, and `juneau-rest-server-springboot`, so adding the JSP bridge + JSP-engine deps to `juneau-utest`'s test-scope dependencies is a small incremental change rather than a structural shift.

**Test class matrix (lives in `juneau-utest/src/test/java/org/apache/juneau/rest/view/jsp/`):**

| Test class | Container | JSP engine | Verifies |
|---|---|---|---|
| `BasicJspResource_Tomcat_Test` | Real embedded Tomcat (started programmatically) | `tomcat-embed-jasper` (test-scope on `juneau-utest`) | Bridge works under the dominant Spring-Boot-default engine. Catches Tomcat-specific Jasper quirks. |
| `BasicJspResource_Jetty_Test` | Real embedded Jetty (via `juneau-microservice-jetty` test harness) | `jetty-ee10-apache-jsp` (test-scope on `juneau-utest`) | Bridge works under the Juneau-microservice-default engine. Catches Jetty-EE10 packaging differences. |
| `BasicJspResource_Springboot_Test` | `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` | Spring Boot embedded Tomcat (default) | Full Spring Boot context + `SpringBeanStore` adapter + auto-configured `RequestDispatcher` end-to-end. The most-realistic Spring-Boot-on-Juneau combination. |
| `BasicJspResource_NoEngine_Test` | `MockRest` (no real container) | None — explicitly absent | Diagnostic message when no JSP engine is on the classpath names the missing dep AND links to the "Choosing a JSP engine" matrix. This is the error-path UX test. |
| `BasicJspResource_MultiBasePath_Test` | Whichever is lightest (likely Tomcat) | `tomcat-embed-jasper` | The documented "register two beans" pattern from resolved decision #7 works under both DI paths. |

**Why centralized rather than split into a dedicated `juneau-rest-tests-jsp` module:**

1. **Convention.** Diverging for one module creates a "where do I find the JSP tests?" cognitive cost. Future view-module siblings (TODO-82/83/84) follow the same convention so the question never gets asked.
2. **Fixture reuse.** A lot of test scaffolding (`MockRestClient` helpers, JUnit 5 base classes, `RestObject` setup) lives in `juneau-utest`. Splitting means extracting or duplicating it.
3. **CI complexity.** A new Maven module adds parent-POM updates, BOM updates, and release-process touch-points (the same kind of overhead the "Risks" section already flags for the bridge module itself).
4. **Multiple-container coexistence is fine.** Tomcat and Jetty embedded containers don't share package roots — they can coexist in one classloader during a test run. The only real cost is per-test-class startup time, which is bounded (5 test classes here, not 50).

**Escape hatch.** If startup time / resource usage proves prohibitive in `juneau-utest` as the `view-*` family grows to 4+ modules with full container matrices each, split view-module real-container tests into a dedicated `juneau-rest-tests-views` test-only module. Not a v1 problem — defer until measurement actually shows it's hurting.

**New deps `juneau-utest/pom.xml` picks up (all test scope):**

```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-rest-server-view-jsp</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-jasper</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.eclipse.jetty.ee10</groupId>
    <artifactId>jetty-ee10-apache-jsp</artifactId>
    <scope>test</scope>
</dependency>
```

(Sibling view-module TODOs — TODO-82/83/84 — inherit this test architecture by default; each follows the same "module-local empty `src/test/`, tests in `juneau-utest`, real-container test classes per engine combo" template.)

## Resolved decisions

All previously open questions resolved 2026-05-24.

1. **Module name — `juneau-rest-server-view-jsp`.** Forward-compatible naming for a `juneau-rest-server-view-*` module family (Thymeleaf, Mustache, FreeMarker tracked as separate TODOs — see "Follow-on TODOs" below). Cleaner convention than retrofitting later.
2. **JSP engine bundling — engine-agnostic (Option B), but BOTH Spring Boot and Jetty must work end-to-end.** The bridge module ships only the JSP API + JSTL impl in `provided` scope; **no default JSP-engine dep**. Consumers pick the engine matching their container — Jetty 12 EE10 (`jetty-ee10-apache-jsp`), embedded Tomcat (`tomcat-embed-jasper`), or external-WAR container-supplied. **Both Spring Boot AND Jetty Microservice are first-class supported configurations** (per resolved decision #3) — Option B's "engine-agnostic POM" does NOT mean "engine-agnostic verification": Phase 5's test matrix MUST exercise both container/engine combos end-to-end (real embedded container, real JSP rendering), even if each requires container-specific configuration documented in the "Choosing a JSP engine" matrix. Rationale: the bridge runtime uses only standard Servlet API, so engine choice is a container-deployment concern, not a framework concern; baking in a Jetty default would put confusing dead-weight in the POM for the substantial fraction of users on Tomcat-flavored deployments (Spring Boot defaults to embedded Tomcat). The example module `juneau-examples-rest-jetty-jsp` explicitly adds `jetty-ee10-apache-jsp` to demonstrate the Jetty path; the bridge module's own test classpath (via `juneau-utest` — see "Test architecture" below) uses `tomcat-embed-jasper` to demonstrate the Tomcat path. A "Choosing a JSP engine" matrix documents the three options in the module README and the topic page. Note: the underlying JSP engine in all cases is Apache Jasper (the Tomcat project's); Jetty's artifact is just a repackaged Jasper bundle aligned with a specific Jakarta EE generation. EE10 is the current alignment; future EE11+ upgrade is a coordinated change across `juneau-microservice-jetty`, this module's test classpath, and the example module.
3. **Spring Boot support — first-class.** Ship `BasicJspResource_Springboot_Test` and the example app with a Spring Boot variant; Phase 5 carries the smoke-test matrix. The Spring Boot path has more risk surface than the microservice one (per the "Spring Boot risk callout" below), but first-class is the right level of investment given how common Spring Boot + Juneau combinations are in practice; the engine-agnostic Option B stance also de-risks the Spring Boot path by letting users pair the bridge with whichever embedded container engine they already have (typically `tomcat-embed-jasper`).
4. **Auto-register `JspViewRenderer` on the mixin — yes.** `@Rest(responseProcessors={JspViewRenderer.class})` is declared on `BasicJspResource` itself so callers who add the mixin don't have to remember to also add the renderer. Reduces boilerplate without added magic; the renderer's behavior is opt-in by virtue of the user explicitly returning a `JspView` from their handler.
5. **Default base path — `/`.** Most flexible default; users who want `/WEB-INF/views/` (or any other prefix) set it explicitly via `BasicJspResource.create().basePath(...)`. The example app demonstrates the `/WEB-INF/views/` convention to keep the canonical Spring-Boot-compatible layout visible.
6. **`JspView` extends a generic `View` interface — yes.** The `View` interface lives in core `juneau-rest-server` (so future view modules — Thymeleaf, Mustache, FreeMarker — don't all have to depend on the JSP module just for the interface). `JspView` is the first impl, lives in the JSP bridge module. Minor design effort up front prevents a breaking change later when sibling view modules ship. Document the interface contract (`templateName`, `attributes` map, optional response-headers seam) on `View` so future impls have a stable target.
7. **Multi-base-path support — documented "register two beans" pattern, no special builder support.** A user with `/views/` JSPs and `/admin/views/` JSPs registers two `BasicJspResource` beans, each with its own `paths` override and `basePath`. Test coverage in `BasicJspResource_MultiBasePath_Test` confirms the pattern works under both microservice and Spring Boot DI paths.

## Follow-on TODOs to track after this lands

Per the "view-module family" naming convention (resolved decision #1), sibling view modules for other JVM templating engines are tracked as separate TODOs. Each follows the same pattern as TODO-78 (bridge module + `View` interface impl + example module + Spring Boot smoke test):

- **TODO-82** — Thymeleaf view module (`juneau-rest-server-view-thymeleaf`). High priority since Thymeleaf is Spring Boot's default web view technology; large existing user base for Spring-Boot-on-Juneau migrations.
- **TODO-83** — Mustache view module (`juneau-rest-server-view-mustache`). Logic-less templates; common choice for content authored by non-Java developers.
- **TODO-84** — FreeMarker view module (`juneau-rest-server-view-freemarker`). Apache FreeMarker; widely used in admin consoles and reporting tools.

(JSF / Facelets is explicitly NOT planned — separately scoped out in this TODO's "Explicitly out of scope" section.)

## Risks

- **Spring Boot JSP support flakiness.** Spring Boot's official position is "we recommend you don't use JSP under embedded containers." Real-world reports show it works but with sharp edges (fat-jar packaging, classpath resource resolution, exploded-vs-jar variance). Mitigation: explicit Spring Boot smoke tests; document loudly; if Phase 5 surfaces gotchas not foreseen here, escalate (call out as a follow-up TODO rather than silently shipping a broken-on-Spring-Boot mixin).
- **Jetty version drift.** A future Jetty upgrade in `juneau-microservice-jetty` could break the JSP module if the JSP engine doesn't keep pace. Mitigation: dependency-pinning + explicit version test in CI; document that the JSP module is tested against the same Jetty version as `juneau-microservice-jetty`.
- **Engine selection burden on the user (Option B trade-off).** The bridge module ships with no default JSP engine — consumers MUST add one (`jetty-ee10-apache-jsp` for Jetty, `tomcat-embed-jasper` for embedded Tomcat, or rely on the deployment container). Without an engine, the failure surfaces at request time (`ClassNotFoundException` or a Juneau-wrapped diagnostic), not build time. Mitigation: javadoc + module README "Choosing a JSP engine" matrix + `BasicJspResource_NoEngine_Test` confirms the diagnostic message is human-readable and names both the missing dependency AND the matrix link so users have a one-click recovery path. The opposite trade-off — bake in a Jetty default — was considered (Option A in the OQ#2 deliberation) and rejected because it puts confusing dead-weight in the POM for the substantial fraction of users on Tomcat-flavored deployments (Spring Boot defaults to embedded Tomcat).
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
