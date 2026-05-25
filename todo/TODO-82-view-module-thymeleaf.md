# TODO-82: Thymeleaf view module (`juneau-rest-server-view-thymeleaf`)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23; expanded 2026-05-24 as a sibling to TODO-78 (JSP). One of three view-engine sibling plans — TODO-83 (Mustache), TODO-84 (FreeMarker) follow the same template.

## Goal

Add a new `juneau-rest-server-view-thymeleaf` Maven module that ships a `BasicThymeleafResource` mixin plus a `ThymeleafViewRenderer` `ResponseProcessor` and a `ThymeleafView` impl of the generic `View` interface introduced by TODO-78. Lives in its own module so the Thymeleaf runtime (`org.thymeleaf:thymeleaf`) doesn't bleed into core `juneau-rest-server`. Thymeleaf is Spring Boot's default web view technology, so this is the highest-priority sibling of the three view-engine TODOs (TODO-82/83/84) for the Spring-Boot-on-Juneau migration path; the petstore-springboot sample (TODO-87) will most likely pick Thymeleaf as its view engine.

End-state developer experience:

```java
// pom: add juneau-rest-server-view-thymeleaf + org.thymeleaf:thymeleaf
@Rest(path="/app", mixins=BasicThymeleafResource.class)
public class AppResource extends RestServlet {

    @Bean BasicThymeleafResource thymeleaf() {
        return BasicThymeleafResource.create()
            .basePath("/templates/")
            .build();
    }

    // Method-level: return a View bean that the renderer maps to a Thymeleaf template.
    @RestGet("/hello/{name}")
    public View hello(@Path String name) {
        return ThymeleafView.of("hello")  // resolves to /templates/hello.html
            .attr("name", name)
            .attr("ts", Instant.now());
    }
}

// Default mount also serves raw .html templates from the classpath with
// request-/session-scope attributes only (no Java model):
// GET /app/thymeleaf/about → renders /templates/about.html
```

## Why now

- Thymeleaf is Spring Boot's default web view technology; the very large existing population of Spring-Boot-on-Juneau migrations effectively requires a first-class Thymeleaf bridge before they can move HTML-rendered admin consoles / dashboards / customer portals to Juneau without losing their existing template library.
- Pre-FINISHED-72, a Thymeleaf bridge would have been hard to retrofit because the dispatch into the engine needed a clean response-resolution seam; FINISHED-72's mixin primitives + Juneau's existing `ResponseProcessor` chain (used by TODO-78 for JSP) make this clean.
- Architecturally simpler than JSP (TODO-78): Thymeleaf's `TemplateEngine.process(templateName, context, writer)` writes directly to a `Writer`, so there is no `RequestDispatcher.forward()` dance, no `META-INF/resources/` fat-jar packaging gotcha, no engine-vs-container precedence battles. Most of the Spring-Boot risk surface that TODO-78 has to defuse simply does not exist here.
- The user has explicitly named Thymeleaf on the roadmap and is comfortable with module-level POM deps for templating runtimes.
- Doing it in a separate module preserves the "no extra deps in core" property the rest of `juneau-rest-server` maintains.

## Scope

**In scope (v1):**

- New Maven module `juneau-rest/juneau-rest-server-view-thymeleaf/` with a `pom.xml` mirroring `juneau-rest-server-view-jsp` (sibling pattern from TODO-78) and `juneau-rest-server-mcp` (closest small-single-purpose precedent).
- `org.apache.juneau.rest.view.thymeleaf.BasicThymeleafResource` mixin class (mixin-only — **no class-level `@Rest(paths=...)`**; see "Configurable mount path" section below). Single `@RestGet(path="/thymeleaf/*", swagger=@OpSwagger(ignore=true))` handler (with `@Path("/*") String path` capturing the multi-segment remainder) that:
    - Reads the configured base-path (default `/`).
    - Resolves the template by stripping any trailing extension and asking the `TemplateEngine` for `<basePath><path-without-ext>` (Thymeleaf appends `.html` via the resolver's suffix).
    - Builds a `Context` populated with request + session attributes (no Java model, since this is the raw-rendering path).
    - Writes the engine's output directly to the response writer with `Content-Type: text/html;charset=UTF-8`.
    - **Path-traversal hardening:** the resolved `<basePath><path>` MUST be validated via `FileUtils.resolveVirtualPathSafely(basePath, path)` (added in Phase C2 alongside FINISHED-100; see `BasicJspResource.render` for the canonical caller). The helper throws `IllegalArgumentException` on any `..` segment that escapes `basePath`; the handler maps that to HTTP 403. This is the same hardening pattern used by `DirectoryResource`, `LogsResource`, and `BasicJspResource`.
- `org.apache.juneau.rest.view.thymeleaf.ThymeleafView` value-class — a `View` bean carrying `(templateName, Map<String,Object> attributes)`. `@RestOp` methods can return `ThymeleafView` directly and the framework's `ResponseProcessor` chain dispatches to the renderer.
- `org.apache.juneau.rest.view.thymeleaf.ThymeleafViewRenderer` `ResponseProcessor` impl — detects `ThymeleafView` returns, copies attributes into a `Context`, asks the `TemplateEngine` to render to the response writer.
- POM dependencies (engine-agnostic per resolved decision #2):
    - `provided` scope: `org.thymeleaf:thymeleaf` (engine core API only — `TemplateEngine`, `Context`, `ClassLoaderTemplateResolver`).
    - **No `thymeleaf-spring6` dep in the bridge module.** The bridge talks to the generic `TemplateEngine` interface; under Spring Boot, `spring-boot-starter-thymeleaf` transitively brings in `thymeleaf-spring6` and creates an autoconfigured `SpringTemplateEngine` bean which the bridge picks up via `BeanStore.getBean(TemplateEngine.class)` (Spring's bean is-a `TemplateEngine`).
- **No default `TemplateEngine` ships pre-baked with the bridge.** If no `TemplateEngine` bean is registered, the bridge constructs a sensible default at first use: a `TemplateEngine` with a single `ClassLoaderTemplateResolver` (prefix `/templates/`, suffix `.html`, mode `HTML`, cacheable `true` for prod / `false` for dev — controlled by a builder flag). Documented "Choosing a `TemplateEngine`" matrix in the module README:
    - **Spring Boot:** `spring-boot-starter-thymeleaf` autoconfig provides `SpringTemplateEngine`; bridge picks it up automatically.
    - **Juneau microservice / Jetty:** user registers `@Bean TemplateEngine` in their resource config, OR accepts the bridge default.
    - **Stripped-down deployment:** user supplies their own `TemplateEngine` bean wired with custom resolvers (file-based, DB-backed, etc.).
- Tests in `juneau-utest` (same architecture as TODO-78): MockRest baseline + real `JettyMicroservice` + real `@SpringBootTest` + embedded Tomcat; per-engine real-rendering tests; "no Thymeleaf engine on classpath → clear diagnostic" test.

**Explicitly out of scope (v1):**

- Thymeleaf reactive / WebFlux integration (`thymeleaf-spring6`'s reactive surface) — Juneau is servlet-only today; defer.
- Fragment / inline rendering (`th:include`, `th:replace` from arbitrary network sources) — fragment includes inside templates work transparently via the engine; but exposing the fragment API as a Juneau handler is out of scope.
- Custom dialects (`StandardDialect` ships by default; user-supplied dialects work if the user wires their own `TemplateEngine` with the dialect already attached).
- `TemplateMode` beyond `HTML` — `XML` / `TEXT` / `JAVASCRIPT` / `CSS` / `RAW` work if the user wires a custom engine but the bridge's default is HTML-only.
- Server-Sent Events / streaming output beyond what `process(...)` writes to the writer.

## Dependency-injection notes

- **Mixin instance resolution.** `BasicThymeleafResource` is instantiated via the FINISHED-72 mixin walk: the host's `BeanStore` is consulted first (per the FINISHED-76 framework alignment), no-arg constructor reflection fallback. Both microservice (`BasicBeanStore`) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths use this lookup verbatim.
- **`TemplateEngine` bean resolution.** The bridge calls `getContext().getBeanStore().getBean(TemplateEngine.class)` lazily at first-request time. Under Spring Boot, `spring-boot-starter-thymeleaf`'s autoconfigured `SpringTemplateEngine` (which extends `TemplateEngine`) is returned. Under Juneau microservice, the user supplies a `@Bean TemplateEngine` or accepts the bridge default. The lookup must be lazy (request-time) so a `TemplateEngine` registered later in the lifecycle is still picked up.
- **Builder-time configuration sourcing.** The mixin reads:
    - **Base path** (default `/`). Microservice: `@Bean BasicThymeleafResource thymeleaf() { return BasicThymeleafResource.create().basePath("/templates/").build(); }`. Spring Boot: identical, in a `@Configuration`.
    - **Caching flag** (default `true` — production-safe; users opt into hot-reload by setting `cacheTemplates(false)` on the builder, which then propagates to the default resolver when the bridge constructs its fallback engine).
    - The `ThymeleafViewRenderer` `ResponseProcessor` is auto-registered via `@Rest(responseProcessors={ThymeleafViewRenderer.class})` on `BasicThymeleafResource` itself.
- **Spring-Boot-specific notes (lower risk surface than TODO-78).**
    - **`spring-boot-starter-thymeleaf` autoconfig is the canonical path.** Adding the starter gives the user a `SpringTemplateEngine` bean for free; the bridge picks it up via `BeanStore.getBean(TemplateEngine.class)`. No further wiring needed.
    - **Classpath-resource template resolution works identically under fat-jar and `spring-boot:run`** because the engine writes to a `Writer` rather than forwarding via `RequestDispatcher`. The only constraint is that templates live under `src/main/resources/` so they're packaged into the jar at `BOOT-INF/classes/` — Spring Boot's classloader finds them. Document.
    - **`@Primary`/`@Qualifier` for multiple `TemplateEngine` beans.** Rare, but if a user has both `SpringTemplateEngine` and a custom `TemplateEngine`, Spring's `@Primary` semantics flow through `SpringBeanStore.getBean(TemplateEngine.class)`. Document the precedence.
- **Acceptance bullet** added below: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Configurable mount path (SVL pattern, FINISHED-99)

The default `/thymeleaf/*` mount is pinned at the **op level** by
`@RestGet(path="/thymeleaf/*")` on the `render` handler — **not** by a class-level
`@Rest(paths=...)` declaration. Per the framework Javadoc at `Rest.java:1017-1021` (and
`Rest.java:1081-1085` for `paths()`), `@Rest(path)` / `@Rest(paths)` on a mixin class is
**silently ignored** when the host imports the class via `@Rest(mixins=...)`; the importing
host's own `path()` / `paths()` governs the mount and mixin endpoints land in the host's URL
namespace. This is why the mixin shape in Scope above carries no class-level `paths=...`.

For runtime-configurable mounts, FINISHED-99 lit up Juneau SVL resolution on op paths. The
recommended pattern in this module is:

```java
@RestGet(path="/${myroute:thymeleaf}/*", swagger=@OpSwagger(ignore=true))
public void render(@Path("/*") String path, RestRequest req, RestResponse res) { ... }
```

The `${myroute:thymeleaf}` SVL expression resolves at startup via the
{@link org.apache.juneau.svl.VarResolver VarResolver} on the bean store. Worked examples:

- **System property:** `-Dmyroute=views` → mount at `/views/*`.
- **Environment variable:** with op shape `@RestGet(path="/${E:MYROUTE,thymeleaf}/*")`,
  `MYROUTE=views` in the environment → `/views/*`; default `thymeleaf` otherwise.
- **`Config` override:** with op shape `@RestGet(path="/${C:myroute,thymeleaf}/*")`,
  `juneau.cfg` carries `myroute = views` → `/views/*`.

Sibling bridge modules (TODO-83 Mustache, TODO-84 FreeMarker) inherit this pattern; the only
per-engine variation is the default mount segment (`mustache`, `freemarker`).

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm the generic `View` interface from TODO-78 is in place in core `juneau-rest-server` and has the contract this module needs (`templateName`, `attributes` map). **HARD blocker** — TODO-78 must land first.
2. Confirm `ResponseProcessor` chain accepts a `ThymeleafView`-typed return value via the standard precedence (matches by type before falling through to default serializers). Same seam TODO-78's `JspViewRenderer` uses.
3. Confirm Spring Boot's `spring-boot-starter-thymeleaf` autoconfiguration exposes `TemplateEngine` (not just `SpringTemplateEngine`) so a `BeanStore.getBean(TemplateEngine.class)` query finds it. Verify against current Spring Boot 3.x docs.
4. Confirm `getContext().getResourceSupplier().getResourceClass()` is the right classloader anchor for the bridge's default `ClassLoaderTemplateResolver` — must be the importer's class, not the bridge's, so templates ship in the user's jar.
5. **Engine inventory.** Test matrix coverage for the documented `TemplateEngine` sources:
    - Spring Boot autoconfig: `BasicThymeleafResource_Springboot_Test` exercises the autoconfigured `SpringTemplateEngine`.
    - Microservice bridge default: `BasicThymeleafResource_Jetty_Test` exercises the fallback `TemplateEngine` the bridge constructs when no bean is registered.
    - User-supplied custom engine: `BasicThymeleafResource_CustomEngine_Test` registers a `@Bean TemplateEngine` with a non-default resolver and verifies the bridge uses it.

### Phase 1 — module skeleton + POM

1. New module `juneau-rest/juneau-rest-server-view-thymeleaf/` with `pom.xml` mirroring `juneau-rest-server-mcp` and `juneau-rest-server-view-jsp` (once the latter lands).
2. POM dependencies (engine-agnostic per resolved decision #2):
    - `provided` scope: `org.thymeleaf:thymeleaf`. NO `thymeleaf-spring6`.
    - `test` scope: `org.thymeleaf:thymeleaf` (so the bridge module can compile/test standalone).
3. Module skeleton compiles + ships an empty test to verify the build.
4. New `BasicThymeleafResource_NoEngine_Test` asserts the diagnostic message (Phase 3 / acceptance) when the test classpath is stripped of `org.thymeleaf:thymeleaf` (Surefire system-property toggle or a separate test-classifier).

### Phase 2 — `BasicThymeleafResource` mixin

1. New class with `@Rest(responseProcessors={ThymeleafViewRenderer.class})` (NO `paths=...` — see "Configurable mount path" below) and `@RestGet(path="/thymeleaf/*", swagger=@OpSwagger(ignore=true))` handler (signature: `void render(@Path("/*") String path, RestRequest req, RestResponse res, ...)` writing directly to `res.getWriter()` after `FileUtils.resolveVirtualPathSafely(basePath, path)` validates the resolved target).
2. Builder accepts `basePath(String)`, `cacheTemplates(boolean)`, `templateMode(TemplateMode)` (HTML default).
3. Default-engine construction logic: on first request, if no `TemplateEngine` bean is registered, build one with `ClassLoaderTemplateResolver(prefix=basePath, suffix=".html", templateMode=HTML, cacheable=cacheFlag)` anchored on the importer's classloader.
4. Tests:
    - `BasicThymeleafResource_RawRender_Test` — GET `/thymeleaf/hello` renders `/templates/hello.html` correctly; request/session attributes are available in the template.
    - `BasicThymeleafResource_BasePath_Test` — `basePath("/views/")` resolves correctly.
    - `BasicThymeleafResource_NotFound_Test` — missing template → 404.

### Phase 3 — `ThymeleafView` + `ThymeleafViewRenderer`

1. New value class `ThymeleafView implements View` (`templateName`, `Map<String,Object> attributes`, fluent `.attr(...)` API).
2. New `ResponseProcessor` impl `ThymeleafViewRenderer` that detects `ThymeleafView` returns, builds a `Context` from the attributes + request/session, and calls `templateEngine.process(view.getTemplateName(), context, res.getWriter())`.
3. Auto-register `ThymeleafViewRenderer` via `@Rest(responseProcessors={ThymeleafViewRenderer.class})` on `BasicThymeleafResource`.
4. Tests:
    - `ThymeleafView_Test` — `@RestGet` method returning `ThymeleafView.of("hello").attr("name","world")` renders with the attribute available as `${name}` in the template.
    - `ThymeleafViewRenderer_NoEngine_Test` — when no Thymeleaf engine is on the classpath, the renderer fails with a clear diagnostic naming the missing dependency.

### Phase 4 — example module

1. New module `juneau-examples/juneau-examples-rest-jetty-thymeleaf/` mirroring `juneau-examples-rest-jetty` structure. Adds `org.thymeleaf:thymeleaf` to its own `pom.xml` (so the example actually has an engine on its classpath).
2. Hello-World template at `src/main/resources/templates/hello.html` rendered by `@RestGet("/hello/{name}") public View hello(@Path String name)`.
3. Tests:
    - `JuneauThymeleafExample_Test` — end-to-end smoke test through the example.

### Phase 5 — Spring Boot smoke tests

1. New tests in `juneau-utest` exercising the Spring `BeanStore` adapter + `spring-boot-starter-thymeleaf` autoconfig.
2. Tests:
    - `BasicThymeleafResource_Springboot_Test` — `@SpringBootTest(webEnvironment=RANDOM_PORT)` + embedded Tomcat. Register `BasicThymeleafResource` as a Spring `@Bean`; the autoconfigured `SpringTemplateEngine` is picked up via `BeanStore.getBean(TemplateEngine.class)`. GET `/thymeleaf/hello` renders identically to the microservice form.
    - `ThymeleafView_Springboot_Test` — `@RestGet` returning `ThymeleafView` works under the Spring `BeanStore` adapter using the autoconfigured engine.
    - `BasicThymeleafResource_Springboot_MultiEngine_Test` — two `@Bean TemplateEngine`s, one `@Primary`; the `@Primary` one wins per Spring resolver semantics.

### Phase 6 — docs + release notes

1. Release-notes entries under `### juneau-rest-server-view-thymeleaf (new module)` and a cross-reference under `### juneau-rest-server-springboot`.
2. New topic page `docs/pages/topics/ThymeleafViewSupport.md` walking through microservice and Spring Boot setup; the autoconfig story; the "Choosing a `TemplateEngine`" matrix; template caching for dev vs prod.
3. Update `juneau-examples` index to include the new example module.
4. Module-addition entry in the current release-notes file (9.5.0 or successor).

## Acceptance criteria

- [ ] New module `juneau-rest-server-view-thymeleaf` builds and tests pass standalone.
- [ ] Bridge module's POM contains **no Spring-Thymeleaf dependency** in `main` scope — only `org.thymeleaf:thymeleaf` (`provided`).
- [ ] Rendering works under both Spring Boot's autoconfigured `SpringTemplateEngine` (verified via `BasicThymeleafResource_Springboot_Test`) and a Juneau-microservice bridge-default `TemplateEngine` (verified via the bridge module's own test classpath under Jetty).
- [ ] Example app `juneau-examples-rest-jetty-thymeleaf` renders a template at `/hello/{name}` from a JAR classpath resource.
- [ ] `ThymeleafView`-returning `@RestGet` method dispatches via `ThymeleafViewRenderer` correctly.
- [ ] No Thymeleaf engine on classpath → clear diagnostic error message naming the missing dependency.
- [ ] Default base path `/` works; user opt-in to `/templates/` (or any other prefix) via the builder.
- [ ] Both the raw-rendering path (`/thymeleaf/*` mount) and the `View`-returning path carry `@OpSwagger(ignore=true)` so they don't pollute generated OpenAPI specs.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Coverage ≥ 95% on the new module's main code. Full `./scripts/test.py` green; RAT clean.

## Test architecture

**Decision: tests live in `juneau-utest`, following the codebase convention** (and matching TODO-78's resolved test-architecture pattern, plus FINISHED-74/75/76/77's three-way deployment parity).

**Test class matrix (lives in `juneau-utest/src/test/java/org/apache/juneau/rest/view/thymeleaf/`):**

| Test class | Container | TemplateEngine source | Verifies |
|---|---|---|---|
| `BasicThymeleafResource_MockRest_Test` | `MockRest` baseline | Bridge default (`TemplateEngine` with classpath resolver) | Baseline rendering, attribute passing, 404 on missing, base-path override. Lightest-weight test class. |
| `BasicThymeleafResource_Jetty_Test` | Real embedded Jetty via `MicroserviceTestFixture` | Bridge default | Bridge works under the Juneau-microservice-default container. Catches real-HTTP / real-classloader regressions `MockRest` glosses over. |
| `BasicThymeleafResource_Springboot_Test` | `@SpringBootTest(webEnvironment=RANDOM_PORT)` + embedded Tomcat | `spring-boot-starter-thymeleaf` autoconfigured `SpringTemplateEngine` | Full Spring Boot context + `SpringBeanStore` adapter + autoconfigured engine end-to-end. The most-realistic Spring-Boot-on-Juneau combination and the highest-value test in this matrix. |
| `BasicThymeleafResource_Springboot_MultiEngine_Test` | Same | Two `@Bean TemplateEngine`s, one `@Primary` | `@Primary` resolution flows through `SpringBeanStore.getBean(TemplateEngine.class)`; unmarked-multi raises `BeanDefinitionOverrideException` (documented). |
| `BasicThymeleafResource_NoEngine_Test` | `MockRest` (no real container) | None — Thymeleaf stripped from classpath | Diagnostic message names the missing dep and links to the "Choosing a `TemplateEngine`" matrix. Error-path UX test. |
| `ThymeleafView_Test` / `ThymeleafView_Springboot_Test` | MockRest + Spring Boot | Bridge default + autoconfig | `View`-return dispatch path through `ThymeleafViewRenderer`. |

**New deps `juneau-utest/pom.xml` picks up (all test scope):**

```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-rest-server-view-thymeleaf</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.thymeleaf</groupId>
    <artifactId>thymeleaf</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
    <scope>test</scope>
</dependency>
```

Sibling view-module TODOs (TODO-83/84) inherit this test architecture by default; each follows the same per-engine matrix shape.

## Resolved decisions

1. **Module name — `juneau-rest-server-view-thymeleaf`.** Slots into the `juneau-rest-server-view-*` family established by TODO-78 (resolved decision #1 there).
2. **Engine bundling — engine-agnostic (Option B), mirrored from TODO-78 #2.** Bridge module declares only `org.thymeleaf:thymeleaf` in `provided` scope; `thymeleaf-spring6` is NOT a bridge dep (the bridge talks to the generic `TemplateEngine` interface). Consumers add the engine version they want; under Spring Boot, `spring-boot-starter-thymeleaf` transitively supplies both. Compared to TODO-78's three-way "Choosing an engine" matrix, Thymeleaf's matrix is much simpler — two rows (Spring Boot autoconfig vs. user-supplied bean), no container-version pinning.
3. **Spring Boot support — first-class, headline use case.** This is the most important sibling of TODO-82/83/84 because Thymeleaf is Spring Boot's default web view technology. Phase 5 tests are the highest-priority work in this plan; the petstore-springboot sample (TODO-87) very likely picks Thymeleaf as its view engine.
4. **Auto-register `ThymeleafViewRenderer` on the mixin — yes.** `@Rest(responseProcessors={ThymeleafViewRenderer.class})` is declared on `BasicThymeleafResource` so callers don't have to register the renderer separately.
5. **Default base path — `/`.** Most flexible. Example app uses `/templates/` to demonstrate the conventional Spring-Boot-compatible layout.
6. **`ThymeleafView extends View` — yes.** `View` interface lives in core `juneau-rest-server` per TODO-78 resolved decision #6 (HARD dependency on TODO-78 landing first). `ThymeleafView` is the impl in this bridge module.
7. **Multi-base-path support — documented "register two beans" pattern, no special builder API.** A user with `/templates/` and `/admin/templates/` registers two `BasicThymeleafResource` beans, each with its own op-level `@RestGet(path=...)` override and `basePath`. (The bridge class itself has no class-level `@Rest(paths=...)` — that would be silently ignored under the mixin pattern; see "Configurable mount path" section above.) Same convention as TODO-78 #7.

## Follow-on TODOs to track after this lands

- Coverage of the engine-agnostic POM stance across Tomcat / Jetty / external container deployments (matrix tracking).
- Hot-reload during development (template caching disabled; opt-in via `cacheTemplates(false)` already lands in v1, but a `juneau-microservice-jetty`-style dev-mode auto-detect could land later).
- Localization / i18n integration with Juneau's existing `Messages` infrastructure (Thymeleaf has its own `MessageSource` abstraction; bridge could adapter-wrap `Messages`).
- Reactive / WebFlux integration once Juneau's async story (TODO-70) lands.

## Resolved decisions — OQA

1. **Default `index.html` template in the bridge — RESOLVED 2026-05-25 as NO.** Bridge ships no opinionated default template. The "Choosing a `TemplateEngine`" matrix in the module README + the example module (Phase 4) are the discoverability path; a zero-config deployment with no user-supplied templates returns 404 from `/thymeleaf/*` with a clear "no template named X under base-path Y" message. Aligns with the plan's prior recommendation and matches FINISHED-75's "no opinionated defaults in the mixin" stance.
2. **JVM floor — RESOLVED 2026-05-25 as CONFIRMED (JDK 17).** Juneau's stated floor is already JDK 17 (verified in `juneau-core/pom.xml`), so Thymeleaf 3.1.x is usable without pinning back to 3.0.x and without a per-module floor bump. Phase 0's "verify before settling" check is satisfied; no further action.

## Open questions

_All open questions resolved 2026-05-25. See "Resolved decisions — OQA" above._

## Risks

- **Spring Boot autoconfig collision.** A user with `spring-boot-starter-thymeleaf` and Spring MVC routes already serving HTML via `@Controller` + Thymeleaf could have Juneau's `BasicThymeleafResource` mount overlap with Spring MVC's view-resolver mount. Mitigation: documented in the topic page; recommend distinct path prefixes (e.g. `/api/views/*` for Juneau, `/views/*` for Spring MVC).
- **`TemplateEngine` caching during dev.** With `cacheTemplates(true)` (production default), edits to template files won't be picked up without a server restart — confusing for developers used to Spring Boot DevTools' hot-reload. Mitigation: `cacheTemplates(false)` opt-in documented loudly; an opt-in dev-mode auto-detect lands as a follow-on TODO.
- **Engine selection burden on the user (Option B trade-off).** Bridge ships no default engine; without `org.thymeleaf:thymeleaf` on the classpath, the failure surfaces at first request (`ClassNotFoundException` or a Juneau-wrapped diagnostic). Mitigation: javadoc + module README + `BasicThymeleafResource_NoEngine_Test` confirms the diagnostic is human-readable.
- **`SpringTemplateEngine` is-a `TemplateEngine`.** Verified true in Thymeleaf 3.x — but a future Thymeleaf 4.x breaking change to the class hierarchy could regress this. Mitigation: pin a known-good Thymeleaf major version; verify in CI on upgrade.
- **Module-level POM dep complexity.** Adding a new module is non-trivial — parent `pom.xml` updates, BOM updates, release-process touch-points. Mitigation: model after `juneau-rest-server-mcp` and `juneau-rest-server-view-jsp` (once landed).

## Related work

- `todo/TODO-78-mixin-jsp-module.md` — **HARD dependency.** TODO-78 introduces the generic `View` interface in core `juneau-rest-server` (its resolved decision #6) AND establishes the `ResponseProcessor`-based renderer pattern this module follows. This TODO **cannot land** before TODO-78. Also the closest sibling for module-shape / POM / test-architecture mirroring.
- `todo/TODO-83-view-module-mustache.md` — sibling view-module TODO; same architecture; no inter-sibling dependency.
- `todo/TODO-84-view-module-freemarker.md` — sibling view-module TODO; same architecture; no inter-sibling dependency.
- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives this module builds on (transitively required).
- `todo/FINISHED-81-mixin-sub-context-inheritance.md` — the sub-`RestContext` inheritance model used by every mixin landing post-FINISHED-81; allows this module to declare its own serializer overrides (e.g. HTML-first content negotiation) scoped to mixin endpoints only.
- `todo/FINISHED-74-mixin-api-docs.md` — three-way deployment parity test-architecture template (MockRest baseline + real Jetty + real Spring Boot).
- `todo/FINISHED-75-mixin-static-files.md` — `@OpSwagger(ignore=true)` annotation precedent for excluding the raw-rendering endpoint from OpenAPI specs.
- `todo/FINISHED-76-mixin-convention-endpoints.md` and `todo/FINISHED-77-mixin-ops-introspection.md` — topic-page tone reference for the eventual docs phase.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter; Phase 5 smoke-test target.
- `juneau-microservice/juneau-microservice-jetty/` — Jetty microservice harness; Phase 4 example module structural reference.
- `juneau-examples/juneau-examples-rest-jetty/` — structural reference for `juneau-examples-rest-jetty-thymeleaf`.
