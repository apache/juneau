# TODO-84: FreeMarker view module (`juneau-rest-server-view-freemarker`)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23; expanded 2026-05-24 as a sibling to TODO-78 (JSP). One of three view-engine sibling plans — TODO-82 (Thymeleaf), TODO-83 (Mustache) follow the same template.

## Goal

Add a new `juneau-rest-server-view-freemarker` Maven module that ships a `BasicFreemarkerResource` mixin plus a `FreemarkerViewRenderer` `ResponseProcessor` and a `FreemarkerView` impl of the generic `View` interface introduced by TODO-78. Lives in its own module so the FreeMarker runtime (`org.freemarker:freemarker`) doesn't bleed into core `juneau-rest-server`. FreeMarker is an Apache Software Foundation project — same family as Juneau itself — and is widely used for admin consoles, internal reporting tools, and email-template generation. It is the most feature-rich of the three view-engine TODOs (TODO-82/83/84) in terms of expression-language and template-composition capabilities.

End-state developer experience:

```java
// pom: add juneau-rest-server-view-freemarker + org.freemarker:freemarker
@Rest(path="/app", mixins=BasicFreemarkerResource.class)
public class AppResource extends RestServlet {

    @Bean BasicFreemarkerResource freemarker() {
        return BasicFreemarkerResource.create()
            .basePath("/templates/")
            .build();
    }

    // Method-level: return a View bean that the renderer maps to a FreeMarker template.
    @RestGet("/hello/{name}")
    public View hello(@Path String name) {
        return FreemarkerView.of("hello.ftlh")
            .attr("name", name)
            .attr("ts", Instant.now());
    }
}

// Default mount also serves raw .ftl / .ftlh templates from the classpath with
// request-/session-scope attributes only (no Java model):
// GET /app/freemarker/about.ftlh → renders /templates/about.ftlh
```

## Why now

- FreeMarker has a long-standing reputation in the JVM templating space as the engine of choice for **admin consoles, reporting / email-template generation, and code-generation pipelines** — categories where Juneau is a natural REST front-end. Teams migrating an internal admin tool from a hand-rolled Servlet stack to Juneau typically have an existing FreeMarker template library they want to keep.
- It is an **Apache Software Foundation project** — same family as Juneau itself — so its licensing, governance, and release cadence are familiar and predictable inside the ASF ecosystem. A small but meaningful alignment bonus.
- Pre-FINISHED-72, a FreeMarker bridge would have been hard to retrofit because the dispatch into the engine needed a clean response-resolution seam; FINISHED-72's mixin primitives + Juneau's existing `ResponseProcessor` chain (used by TODO-78 for JSP) make this clean.
- Architecturally simpler than JSP (TODO-78): FreeMarker's `Template.process(model, writer)` writes directly to a `Writer`, so there is no `RequestDispatcher.forward()` dance, no `META-INF/resources/` fat-jar packaging gotcha, no engine-vs-container precedence battles.
- The user has explicitly named FreeMarker on the roadmap and is comfortable with module-level POM deps for templating runtimes.
- Doing it in a separate module preserves the "no extra deps in core" property the rest of `juneau-rest-server` maintains.

## Scope

**In scope (v1):**

- New Maven module `juneau-rest/juneau-rest-server-view-freemarker/` with a `pom.xml` mirroring `juneau-rest-server-view-jsp` (sibling pattern from TODO-78) and `juneau-rest-server-mcp` (closest small-single-purpose precedent).
- `org.apache.juneau.rest.view.freemarker.BasicFreemarkerResource` mixin with default `@Rest(paths={"/freemarker/*"})`. Single `@RestGet("/*")` handler (with `@Path("/*") String path` capturing the multi-segment remainder, `swagger=@OpSwagger(ignore=true)`) that:
    - Reads the configured base-path (default `/`).
    - Resolves the template by asking the `Configuration` for `<basePath><path>`.
    - Builds a model `Map<String,Object>` populated with request + session attributes (no Java model, since this is the raw-rendering path).
    - Writes the engine's output directly to the response writer with `Content-Type: text/html;charset=UTF-8` (for `.ftlh` / `.html` templates; FreeMarker's `OutputFormat` auto-selects HTML escaping for `.ftlh`).
- `org.apache.juneau.rest.view.freemarker.FreemarkerView` value-class — a `View` bean carrying `(templateName, Map<String,Object> attributes)`. `@RestOp` methods can return `FreemarkerView` directly and the framework's `ResponseProcessor` chain dispatches to the renderer.
- `org.apache.juneau.rest.view.freemarker.FreemarkerViewRenderer` `ResponseProcessor` impl — detects `FreemarkerView` returns, asks the active `Configuration` for the named template, and calls `template.process(view.getAttributes(), res.getWriter())`.
- POM dependencies (engine-agnostic per resolved decision #2):
    - `provided` scope: `org.freemarker:freemarker` (engine core — `Configuration`, `Template`, `ClassTemplateLoader`, `FileTemplateLoader`).
- **No default `Configuration` ships pre-baked with the bridge.** If no `Configuration` bean is registered, the bridge constructs a sensible default at first use: a `Configuration(Configuration.VERSION_2_3_X)` with a `ClassTemplateLoader` anchored on the importer's classloader, prefix `basePath`, `OutputFormat` of `HTMLOutputFormat`, `IncompatibleImprovements` pinned to the major version. Documented "Choosing a `Configuration`" matrix in the module README:
    - **Default:** bridge auto-builds `Configuration` against the importer's classpath.
    - **Custom (filesystem):** user registers `@Bean Configuration` with a `FileTemplateLoader(new File("/etc/myapp/templates"))`.
    - **Custom (multi-source):** user registers a `Configuration` with `MultiTemplateLoader` for layered overrides (classpath fallback + filesystem override).
- Two template loaders supported in v1: `ClassTemplateLoader` (classpath, bridge default) and `FileTemplateLoader` (filesystem, user-wired). Additional loaders (`URLTemplateLoader`, DB-backed loaders, etc.) work transparently if the user supplies their own `Configuration`; not specifically tested.
- Tests in `juneau-utest` (same architecture as TODO-78): MockRest baseline + real `JettyMicroservice` + real `@SpringBootTest` + embedded Tomcat; per-engine real-rendering tests; "no FreeMarker engine on classpath → clear diagnostic" test.

**Explicitly out of scope (v1):**

- SQL / database template loaders — supported transparently if the user wires a custom `Configuration` with a custom `TemplateLoader`, but not specifically tested.
- FreeMarker's JSP-tag-library bridge — JSP integration is TODO-78's concern, not this module's.
- Programmatic `Macro` registration via the bridge API — supported transparently inside templates; not exposed as a builder API.
- `OutputFormat` customization beyond the HTML / plain-text auto-detection by file extension — users wire their own `Configuration` for more exotic output formats (XML, RTF, etc.).
- Reactive / streaming output beyond what `Template.process(...)` writes to the writer.

## Dependency-injection notes

- **Mixin instance resolution.** `BasicFreemarkerResource` is instantiated via the FINISHED-72 mixin walk: the host's `BeanStore` is consulted first (per the FINISHED-76 framework alignment), no-arg constructor reflection fallback. Both microservice (`BasicBeanStore`) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths use this lookup verbatim.
- **`Configuration` bean resolution.** The bridge calls `getContext().getBeanStore().getBean(Configuration.class)` lazily at first-request time. Under Spring Boot, `spring-boot-starter-freemarker` autoconfigures a `freemarker.template.Configuration` bean — bridge picks it up automatically. Under Juneau microservice, the user supplies a `@Bean Configuration` or accepts the bridge default. The lookup must be lazy (request-time) so a `Configuration` registered later in the lifecycle is still picked up.
- **`Configuration` class disambiguation.** FreeMarker's `Configuration` class is `freemarker.template.Configuration` — distinct from Spring's `org.springframework.context.annotation.Configuration` annotation. The bridge uses the fully-qualified type in its `BeanStore.getBean(...)` call so the disambiguation is unambiguous; documentation must be careful with the unqualified `Configuration` name when describing the bean.
- **Builder-time configuration sourcing.** The mixin reads:
    - **Base path** (default `/`). Microservice: `@Bean BasicFreemarkerResource freemarker() { return BasicFreemarkerResource.create().basePath("/templates/").build(); }`. Spring Boot: identical, in a `@Configuration` class.
    - **Caching flag** (default `true` — production-safe; users opt into hot-reload by setting `cacheTemplates(false)` which propagates to the default `Configuration`'s `setTemplateUpdateDelayMilliseconds(0)`).
    - The `FreemarkerViewRenderer` `ResponseProcessor` is auto-registered via `@Rest(responseProcessors={FreemarkerViewRenderer.class})` on `BasicFreemarkerResource` itself.
- **Spring-Boot-specific notes (lower risk surface than TODO-78).**
    - **`spring-boot-starter-freemarker` autoconfig is the canonical path.** Adding the starter gives the user a `freemarker.template.Configuration` bean for free; the bridge picks it up via `BeanStore.getBean(freemarker.template.Configuration.class)`. No further wiring needed.
    - **Classpath-resource template resolution works identically under fat-jar and `spring-boot:run`** because the engine writes to a `Writer` rather than forwarding via `RequestDispatcher`. Templates live under `src/main/resources/` so they're packaged into the jar at `BOOT-INF/classes/` — Spring Boot's classloader finds them. Document.
    - **`@Primary`/`@Qualifier` for multiple `Configuration` beans.** Rare. Spring's `@Primary` semantics flow through `SpringBeanStore.getBean(freemarker.template.Configuration.class)`. Document.
- **Acceptance bullet** added below: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm the generic `View` interface from TODO-78 is in place in core `juneau-rest-server` and has the contract this module needs. **HARD blocker** — TODO-78 must land first.
2. Confirm `ResponseProcessor` chain accepts a `FreemarkerView`-typed return value via the standard precedence. Same seam TODO-78's `JspViewRenderer`, TODO-82's `ThymeleafViewRenderer`, and TODO-83's `MustacheViewRenderer` use.
3. Confirm Spring Boot's `spring-boot-starter-freemarker` autoconfiguration exposes `freemarker.template.Configuration` so a `BeanStore.getBean(...)` query finds it. Verify against current Spring Boot 3.x docs.
4. Confirm `getContext().getResourceSupplier().getResourceClass()` is the right anchor for the bridge's default `ClassTemplateLoader`.
5. Confirm the current FreeMarker minor version (`Configuration.VERSION_2_3_X`) the bridge pins to; understand the `IncompatibleImprovements` settings policy.
6. **Engine inventory.** Test matrix coverage:
    - Spring Boot autoconfig: `BasicFreemarkerResource_Springboot_Test` exercises the autoconfigured `Configuration`.
    - Microservice bridge default: `BasicFreemarkerResource_Jetty_Test` exercises the fallback `Configuration` the bridge constructs when no bean is registered.
    - User-supplied custom `Configuration`: `BasicFreemarkerResource_CustomConfig_Test` registers a `@Bean Configuration` with a `FileTemplateLoader` and verifies the bridge uses it.

### Phase 1 — module skeleton + POM

1. New module `juneau-rest/juneau-rest-server-view-freemarker/` with `pom.xml` mirroring `juneau-rest-server-mcp` and `juneau-rest-server-view-jsp` (once the latter lands).
2. POM dependencies (engine-agnostic per resolved decision #2):
    - `provided` scope: `org.freemarker:freemarker`.
    - `test` scope: `org.freemarker:freemarker` (so the bridge module can compile/test standalone).
3. Module skeleton compiles + ships an empty test to verify the build.
4. New `BasicFreemarkerResource_NoEngine_Test` asserts the diagnostic message when the test classpath is stripped of `org.freemarker:freemarker`.

### Phase 2 — `BasicFreemarkerResource` mixin

1. New class with default `@Rest(paths={"/freemarker/*"}, responseProcessors={FreemarkerViewRenderer.class})` and `@RestGet(path="/*", swagger=@OpSwagger(ignore=true))` handler.
2. Builder accepts `basePath(String)`, `cacheTemplates(boolean)`, `version(freemarker.template.Version)` (default pinned to the bridge-tested minor).
3. Default-configuration construction logic: on first request, if no `Configuration` bean is registered, build one with `ClassTemplateLoader(importerClass, basePath)`, `setDefaultEncoding("UTF-8")`, `setOutputFormat(HTMLOutputFormat.INSTANCE)`, `setIncompatibleImprovements(VERSION_2_3_X)`, `setTemplateUpdateDelayMilliseconds(cacheFlag ? Long.MAX_VALUE : 0)`.
4. Tests:
    - `BasicFreemarkerResource_RawRender_Test` — GET `/freemarker/hello.ftlh` renders `/templates/hello.ftlh` correctly; request/session attributes are available in the template.
    - `BasicFreemarkerResource_BasePath_Test` — `basePath("/views/")` resolves correctly.
    - `BasicFreemarkerResource_NotFound_Test` — missing template → 404.
    - `BasicFreemarkerResource_FtlExtensions_Test` — both `.ftl` and `.ftlh` extensions work; `.ftlh` triggers HTML auto-escaping.

### Phase 3 — `FreemarkerView` + `FreemarkerViewRenderer`

1. New value class `FreemarkerView implements View` (`templateName`, `Map<String,Object> attributes`, fluent `.attr(...)` API).
2. New `ResponseProcessor` impl `FreemarkerViewRenderer` that detects `FreemarkerView` returns, asks the active `Configuration` for the named template, and calls `template.process(view.getAttributes(), res.getWriter())`.
3. Auto-register `FreemarkerViewRenderer` via `@Rest(responseProcessors={FreemarkerViewRenderer.class})` on `BasicFreemarkerResource`.
4. Tests:
    - `FreemarkerView_Test` — `@RestGet` returning `FreemarkerView.of("hello.ftlh").attr("name","world")` renders with the attribute available as `${name}` in the template.
    - `FreemarkerView_AutoEscape_Test` — `.ftlh` template auto-escapes HTML in attribute values; `.ftl` does not.
    - `FreemarkerViewRenderer_NoEngine_Test` — when no FreeMarker engine is on the classpath, the renderer fails with a clear diagnostic naming the missing dependency.

### Phase 4 — example module

1. New module `juneau-examples/juneau-examples-rest-jetty-freemarker/` mirroring `juneau-examples-rest-jetty` structure. Adds `org.freemarker:freemarker` to its own `pom.xml`.
2. Hello-World template at `src/main/resources/templates/hello.ftlh` rendered by `@RestGet("/hello/{name}") public View hello(@Path String name)`.
3. Tests:
    - `JuneauFreemarkerExample_Test` — end-to-end smoke test through the example.

### Phase 5 — Spring Boot smoke tests

1. New tests in `juneau-utest` exercising the Spring `BeanStore` adapter + `spring-boot-starter-freemarker` autoconfig.
2. Tests:
    - `BasicFreemarkerResource_Springboot_Test` — `@SpringBootTest(webEnvironment=RANDOM_PORT)` + embedded Tomcat. Register `BasicFreemarkerResource` as a Spring `@Bean`; the autoconfigured `freemarker.template.Configuration` is picked up via `BeanStore.getBean(...)`. GET `/freemarker/hello.ftlh` renders identically to the microservice form.
    - `FreemarkerView_Springboot_Test` — `@RestGet` returning `FreemarkerView` works under the Spring `BeanStore` adapter using the autoconfigured `Configuration`.
    - `BasicFreemarkerResource_Springboot_MultiConfig_Test` — two `@Bean Configuration`s, one `@Primary`; the `@Primary` one wins per Spring resolver semantics.

### Phase 6 — docs + release notes

1. Release-notes entries under `### juneau-rest-server-view-freemarker (new module)` and a cross-reference under `### juneau-rest-server-springboot`.
2. New topic page `docs/pages/topics/FreemarkerViewSupport.md` walking through microservice and Spring Boot setup; the autoconfig story; the "Choosing a `Configuration`" matrix; template caching for dev vs prod; the `.ftlh` auto-escape behavior.
3. Update `juneau-examples` index to include the new example module.
4. Module-addition entry in the current release-notes file (9.5.0 or successor).

## Acceptance criteria

- [ ] New module `juneau-rest-server-view-freemarker` builds and tests pass standalone.
- [ ] Bridge module's POM contains **no FreeMarker-engine dependency** in `main` scope — only `org.freemarker:freemarker` (`provided`).
- [ ] Rendering works under both Spring Boot's autoconfigured `Configuration` (verified via `BasicFreemarkerResource_Springboot_Test`) and a Juneau-microservice bridge-default `Configuration` (verified via the bridge module's own test classpath under Jetty).
- [ ] Both `.ftl` (no auto-escape) and `.ftlh` (HTML auto-escape) templates render correctly.
- [ ] Example app `juneau-examples-rest-jetty-freemarker` renders a template at `/hello/{name}` from a JAR classpath resource.
- [ ] `FreemarkerView`-returning `@RestGet` method dispatches via `FreemarkerViewRenderer` correctly.
- [ ] No FreeMarker engine on classpath → clear diagnostic error message naming the missing dependency.
- [ ] Default base path `/` works; user opt-in to `/templates/` (or any other prefix) via the builder.
- [ ] Both the raw-rendering path (`/freemarker/*` mount) and the `View`-returning path carry `@OpSwagger(ignore=true)` so they don't pollute generated OpenAPI specs.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Coverage ≥ 95% on the new module's main code. Full `./scripts/test.py` green; RAT clean.

## Test architecture

**Decision: tests live in `juneau-utest`, following the codebase convention** (and matching TODO-78's resolved test-architecture pattern, plus FINISHED-74/75/76/77's three-way deployment parity).

**Test class matrix (lives in `juneau-utest/src/test/java/org/apache/juneau/rest/view/freemarker/`):**

| Test class | Container | Configuration source | Verifies |
|---|---|---|---|
| `BasicFreemarkerResource_MockRest_Test` | `MockRest` baseline | Bridge default (`Configuration` with `ClassTemplateLoader`) | Baseline rendering, attribute passing, 404 on missing, base-path override, `.ftl`/`.ftlh` extension handling. Lightest-weight test class. |
| `BasicFreemarkerResource_Jetty_Test` | Real embedded Jetty via `MicroserviceTestFixture` | Bridge default | Bridge works under the Juneau-microservice-default container. Catches real-HTTP / real-classloader regressions. |
| `BasicFreemarkerResource_Springboot_Test` | `@SpringBootTest(webEnvironment=RANDOM_PORT)` + embedded Tomcat | `spring-boot-starter-freemarker` autoconfigured `Configuration` | Full Spring Boot context + `SpringBeanStore` adapter + autoconfigured engine end-to-end. |
| `BasicFreemarkerResource_Springboot_MultiConfig_Test` | Same | Two `@Bean Configuration`s, one `@Primary` | `@Primary` resolution flows through `SpringBeanStore.getBean(freemarker.template.Configuration.class)`; unmarked-multi raises `BeanDefinitionOverrideException` (documented). |
| `BasicFreemarkerResource_NoEngine_Test` | `MockRest` (no real container) | None — FreeMarker stripped from classpath | Diagnostic message names the missing dep and links to the "Choosing a `Configuration`" matrix. Error-path UX test. |
| `FreemarkerView_Test` / `FreemarkerView_Springboot_Test` / `FreemarkerView_AutoEscape_Test` | MockRest + Spring Boot | Bridge default + autoconfig | `View`-return dispatch path through `FreemarkerViewRenderer`; `.ftlh` HTML auto-escape verified. |

**New deps `juneau-utest/pom.xml` picks up (all test scope):**

```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-rest-server-view-freemarker</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.freemarker</groupId>
    <artifactId>freemarker</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-freemarker</artifactId>
    <scope>test</scope>
</dependency>
```

Sibling view-module TODOs (TODO-82/83) inherit this test architecture by default.

## Resolved decisions

1. **Module name — `juneau-rest-server-view-freemarker`.** Slots into the `juneau-rest-server-view-*` family established by TODO-78 (resolved decision #1 there). Note the spelling: `freemarker` (lowercase, single word) matches the upstream artifactId `org.freemarker:freemarker` and the package name `freemarker.template.*`. Prose docs may still use the canonical "FreeMarker" capitalization.
2. **Engine bundling — engine-agnostic (Option B), mirrored from TODO-78 #2.** Bridge module declares only `org.freemarker:freemarker` in `provided` scope; consumers supply the runtime version. No default-engine bake-in.
3. **Spring Boot support — first-class.** Phase 5 ships explicit Spring Boot smoke tests using `spring-boot-starter-freemarker`'s autoconfigured `Configuration`. Risk surface is lower than TODO-78 (no servlet coupling, no fat-jar packaging gotcha).
4. **Auto-register `FreemarkerViewRenderer` on the mixin — yes.** `@Rest(responseProcessors={FreemarkerViewRenderer.class})` declared on `BasicFreemarkerResource`.
5. **Default base path — `/`.** Most flexible. Example app uses `/templates/` to demonstrate the conventional layout.
6. **`FreemarkerView extends View` — yes.** `View` interface in core `juneau-rest-server` per TODO-78 resolved decision #6 (HARD dependency on TODO-78 landing first). `FreemarkerView` is the impl in this bridge module.
7. **Multi-base-path support — documented "register two beans" pattern, no special builder API.** Same convention as TODO-78 #7, TODO-82 #7, TODO-83 #7.
8. **Template loaders in v1 — `ClassTemplateLoader` (bridge default) + `FileTemplateLoader` (user-wired).** SQL / DB / URL / S3-backed loaders work transparently if the user supplies a custom `Configuration`, but are not specifically tested in v1.

## Follow-on TODOs to track after this lands

- Coverage of the engine-agnostic POM stance across Tomcat / Jetty / external container deployments.
- Hot-reload during development (`setTemplateUpdateDelayMilliseconds(0)`); opt-in via the builder or a dev-mode auto-detect.
- Localization / i18n integration with Juneau's existing `Messages` infrastructure — FreeMarker has its own message-localization machinery (`Configuration.setLocalizedLookup`); bridge could adapter-wrap `Messages`.
- SQL / database template loader sample showing how to supply a custom `Configuration` for non-classpath template sources.
- `OutputFormat` customization beyond HTML / plain-text (XML, RTF, custom escape policies).

## Open questions

1. **Default `index.ftlh` template in the bridge?** Should the bridge ship a tiny `index.ftlh` template so a zero-config "hello world" deployment renders SOMETHING at `/freemarker/index.ftlh`? **Recommend: NO**, but flagged for user review.
2. **`Configuration.VERSION_2_3_X` pin.** The bridge pins `IncompatibleImprovements` to a specific minor version. Newer FreeMarker minors may change defaults (auto-escape rules, exception-handling defaults) — the pin protects against silent behavior drift, but means the bridge has to opt-in to newer behavior via a version bump. Worth flagging in case the user wants a different pinning policy (e.g. always-latest).

## Risks

- **Engine selection burden on the user (Option B trade-off).** Bridge ships no default engine; without `org.freemarker:freemarker` on the classpath, the failure surfaces at first request (`ClassNotFoundException` or a Juneau-wrapped diagnostic). Mitigation: javadoc + module README + `BasicFreemarkerResource_NoEngine_Test` confirms the diagnostic is human-readable.
- **`Configuration` API name collision.** FreeMarker's `freemarker.template.Configuration` shares a simple name with Spring's `@Configuration` annotation; user code importing both unqualified will not compile. Mitigation: documentation always uses fully-qualified names in examples; bridge code uses the fully-qualified type in its `BeanStore.getBean(...)` call.
- **`Configuration` thread-safety.** FreeMarker's `Configuration` is documented thread-safe AFTER construction but NOT during; users who mutate the autoconfigured Spring Boot `Configuration` post-construction risk concurrency issues. Mitigation: documentation explicitly calls out the "construct then freeze" pattern.
- **`Template` cache memory pressure.** With `cacheTemplates(true)` (production default), FreeMarker caches compiled templates indefinitely by default. A pathological app with very many templates could grow the cache unboundedly. Mitigation: documentation calls out `setCacheStorage(...)` for users who need a bounded cache; default is `SoftCacheStorage` so JVM-pressure-driven eviction works.
- **`.ftl` vs `.ftlh` auto-escape default surprise.** Users new to FreeMarker may not know `.ftl` does NOT auto-escape HTML; an XSS regression could land via a `.ftl`-extension template. Mitigation: documentation strongly recommends `.ftlh` for HTML output; the bridge's default `OutputFormat` is `HTMLOutputFormat` so `Configuration.getDefaultOutputFormat()` falls back to HTML escaping for templates without an explicit extension cue.
- **Module-level POM dep complexity.** Adding a new module is non-trivial. Mitigation: model after `juneau-rest-server-mcp` and `juneau-rest-server-view-jsp` (once landed).

## Related work

- `todo/TODO-78-mixin-jsp-module.md` — **HARD dependency.** TODO-78 introduces the generic `View` interface in core `juneau-rest-server` (its resolved decision #6) AND establishes the `ResponseProcessor`-based renderer pattern this module follows. This TODO **cannot land** before TODO-78. Also the closest sibling for module-shape / POM / test-architecture mirroring.
- `todo/TODO-82-view-module-thymeleaf.md` — sibling view-module TODO; same architecture; no inter-sibling dependency.
- `todo/TODO-83-view-module-mustache.md` — sibling view-module TODO; same architecture; no inter-sibling dependency.
- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives this module builds on (transitively required).
- `todo/FINISHED-81-mixin-sub-context-inheritance.md` — sub-`RestContext` inheritance model; allows this module to declare its own serializer overrides scoped to mixin endpoints only.
- `todo/FINISHED-74-mixin-api-docs.md` — three-way deployment parity test-architecture template (MockRest baseline + real Jetty + real Spring Boot).
- `todo/FINISHED-75-mixin-static-files.md` — `@OpSwagger(ignore=true)` annotation precedent for excluding the raw-rendering endpoint from OpenAPI specs.
- `todo/FINISHED-76-mixin-convention-endpoints.md` and `todo/FINISHED-77-mixin-ops-introspection.md` — topic-page tone reference for the eventual docs phase.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter; Phase 5 smoke-test target.
- `juneau-microservice/juneau-microservice-jetty/` — Jetty microservice harness; Phase 4 example module structural reference.
- `juneau-examples/juneau-examples-rest-jetty/` — structural reference for `juneau-examples-rest-jetty-freemarker`.
