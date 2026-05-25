# TODO-83: Mustache view module (`juneau-rest-server-view-mustache`)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23; expanded 2026-05-24 as a sibling to TODO-78 (JSP). One of three view-engine sibling plans — TODO-82 (Thymeleaf), TODO-84 (FreeMarker) follow the same template.

## Goal

Add a new `juneau-rest-server-view-mustache` Maven module that ships a `BasicMustacheResource` mixin plus a `MustacheViewRenderer` `ResponseProcessor` and a `MustacheView` impl of the generic `View` interface introduced by TODO-78. Lives in its own module so the Mustache runtime (`com.github.spullara.mustache.java:compiler`) doesn't bleed into core `juneau-rest-server`. Mustache is the smallest of the three view-engine TODOs by every metric — fewest concepts, fewest deps, smallest API surface — making it both the lowest-risk to land and the most appealing for teams whose template authors are non-Java developers (designers, technical writers, marketing content owners) since Mustache is intentionally logic-less and has no Java/expression-language coupling.

End-state developer experience:

```java
// pom: add juneau-rest-server-view-mustache + com.github.spullara.mustache.java:compiler
@Rest(path="/app", mixins=BasicMustacheResource.class)
public class AppResource extends RestServlet {

    @Bean BasicMustacheResource mustache() {
        return BasicMustacheResource.create()
            .basePath("/templates/")
            .build();
    }

    // Method-level: return a View bean that the renderer maps to a Mustache template.
    @RestGet("/hello/{name}")
    public View hello(@Path String name) {
        return MustacheView.of("hello.mustache")
            .attr("name", name)
            .attr("ts", Instant.now());
    }
}

// Default mount also serves raw .mustache templates from the classpath with
// request-/session-scope attributes only (no Java model):
// GET /app/mustache/about.mustache → renders /templates/about.mustache
```

## Why now

- Logic-less templating is a small but real concern when the people writing templates are NOT Java developers (designers, technical writers, marketing-content owners). Thymeleaf / FreeMarker / JSP all bleed expression-language syntax into the template surface; Mustache deliberately does not — `{{name}}` is the entire language.
- Pre-FINISHED-72, dropping in a Mustache bridge would have meant fighting the response-resolution chain; FINISHED-72's mixin primitives + Juneau's existing `ResponseProcessor` chain (used by TODO-78 for JSP) make this clean.
- Architecturally the simplest of the three view-engine TODOs: Mustache's `Mustache.execute(writer, scope)` takes a single attributes map (`Map<String,Object>` or a POJO) and writes to a `Writer`. No template context, no servlet coupling, no fat-jar packaging gotcha, no engine-vs-container precedence battles. Lowest-risk module to land in this family.
- The user has explicitly named Mustache on the roadmap and is comfortable with module-level POM deps for templating runtimes.
- Doing it in a separate module preserves the "no extra deps in core" property the rest of `juneau-rest-server` maintains.

## Scope

**In scope (v1):**

- New Maven module `juneau-rest/juneau-rest-server-view-mustache/` with a `pom.xml` mirroring `juneau-rest-server-view-jsp` (sibling pattern from TODO-78) and `juneau-rest-server-mcp` (closest small-single-purpose precedent).
- `org.apache.juneau.rest.view.mustache.BasicMustacheResource` mixin with default `@Rest(paths={"/mustache/*"})`. Single `@RestGet("/*")` handler (with `@Path("/*") String path` capturing the multi-segment remainder, `swagger=@OpSwagger(ignore=true)`) that:
    - Reads the configured base-path (default `/`).
    - Resolves the template by asking the `MustacheFactory` to compile `<basePath><path>`.
    - Renders with a small built-in scope containing request + session attributes (no Java model, since this is the raw-rendering path).
    - Writes the engine's output directly to the response writer with `Content-Type: text/html;charset=UTF-8`.
- `org.apache.juneau.rest.view.mustache.MustacheView` value-class — a `View` bean carrying `(templateName, Map<String,Object> attributes)`. `@RestOp` methods can return `MustacheView` directly and the framework's `ResponseProcessor` chain dispatches to the renderer.
- `org.apache.juneau.rest.view.mustache.MustacheViewRenderer` `ResponseProcessor` impl — detects `MustacheView` returns, compiles the template via the active `MustacheFactory`, and writes the rendered output to the response writer using the attributes map as the scope.
- POM dependencies (engine-agnostic per resolved decision #2):
    - `provided` scope: `com.github.spullara.mustache.java:compiler` (engine core — `MustacheFactory`, `Mustache`, `DefaultMustacheFactory`).
- **No default `MustacheFactory` ships pre-baked with the bridge.** If no `MustacheFactory` bean is registered, the bridge constructs a sensible default at first use: a `DefaultMustacheFactory` with a classpath-rooted resolver (anchored on the importer's classloader). Documented "Choosing a `MustacheFactory`" matrix in the module README:
    - **Default:** bridge auto-builds `DefaultMustacheFactory` against the importer's classpath.
    - **Custom:** user registers `@Bean MustacheFactory` for non-classpath template sources (filesystem, S3, DB).
    - **Engine swap (`jmustache`):** users who prefer `com.samskivert:jmustache` over `compiler` supply their own `MustacheViewRenderer`-equivalent bean wired to the jmustache APIs. The bridge's `MustacheViewRenderer` is `compiler`-specific; the swap is at the renderer layer, not the factory layer. Documented as a follow-on / advanced path.
- Tests in `juneau-utest` (same architecture as TODO-78): MockRest baseline + real `JettyMicroservice` + real `@SpringBootTest` + embedded Tomcat; per-engine real-rendering tests; "no Mustache engine on classpath → clear diagnostic" test.

**Explicitly out of scope (v1):**

- `com.samskivert:jmustache` first-class support — see resolved decision #2 below. Users can swap via custom renderer; bridge ships `compiler` only.
- Mustache "lambdas" beyond the trivial `Function<String,String>` shape — supported transparently by the engine if used inside a template, but not specifically tested.
- Custom partial resolvers beyond classpath (filesystem / DB / S3) — user wires their own `MustacheFactory` with a custom partial-loader.
- Server-Sent Events / streaming output beyond what `execute(...)` writes to the writer.
- `mustache.java`'s reflection-based bean unwrapping configuration knobs — bridge uses the engine's defaults.

## Dependency-injection notes

- **Mixin instance resolution.** `BasicMustacheResource` is instantiated via the FINISHED-72 mixin walk: the host's `BeanStore` is consulted first (per the FINISHED-76 framework alignment), no-arg constructor reflection fallback. Both microservice (`BasicBeanStore`) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths use this lookup verbatim.
- **`MustacheFactory` bean resolution.** The bridge calls `getContext().getBeanStore().getBean(MustacheFactory.class)` lazily at first-request time. Under both microservice and Spring Boot, the user can register a `@Bean MustacheFactory` with custom configuration; otherwise the bridge default applies. The lookup must be lazy (request-time) so a `MustacheFactory` registered later in the lifecycle is still picked up.
- **Builder-time configuration sourcing.** The mixin reads:
    - **Base path** (default `/`). Microservice: `@Bean BasicMustacheResource mustache() { return BasicMustacheResource.create().basePath("/templates/").build(); }`. Spring Boot: identical, in a `@Configuration`.
    - The `MustacheViewRenderer` `ResponseProcessor` is auto-registered via `@Rest(responseProcessors={MustacheViewRenderer.class})` on `BasicMustacheResource` itself.
- **Spring-Boot-specific notes (lower risk surface than TODO-78).**
    - **Spring Boot has `spring-boot-starter-mustache`.** It autoconfigures a `MustacheCompiler` bean (the JMustache-flavored compiler from `com.samskivert:jmustache`) — NOT the `mustache.java` compiler this bridge depends on. **This is a name collision risk:** a user who adds the starter expecting it to "just work" with `BasicMustacheResource` would be confused. Mitigation documented in the topic page: "use `spring-boot-starter-mustache` only if you swap the bridge's renderer to a jmustache-based one (see resolved decision #2)."
    - **Classpath-resource template resolution** works identically under fat-jar and `spring-boot:run` because `mustache.java` reads from the classpath via `ClassLoader.getResourceAsStream`. Templates live under `src/main/resources/` so they're packaged into the jar at `BOOT-INF/classes/`.
    - **`@Primary`/`@Qualifier` for multiple `MustacheFactory` beans.** Rare. Spring's `@Primary` semantics flow through `SpringBeanStore.getBean(MustacheFactory.class)`. Document.
- **Acceptance bullet** added below: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm the generic `View` interface from TODO-78 is in place in core `juneau-rest-server` and has the contract this module needs. **HARD blocker** — TODO-78 must land first.
2. Confirm `ResponseProcessor` chain accepts a `MustacheView`-typed return value via the standard precedence. Same seam TODO-78's `JspViewRenderer` and TODO-82's `ThymeleafViewRenderer` use.
3. Confirm `mustache.java`'s `DefaultMustacheFactory` can be constructed with a custom `ClassLoader` (or whether the constructor only takes a directory `String`). Required for the bridge's default factory to anchor on the importer's classloader.
4. Confirm `getContext().getResourceSupplier().getResourceClass()` is the right anchor for the bridge's default factory's classpath resolver.
5. **Engine inventory.** Test matrix coverage:
    - Bridge default factory: `BasicMustacheResource_Jetty_Test` exercises the fallback `DefaultMustacheFactory`.
    - User-supplied custom factory: `BasicMustacheResource_CustomFactory_Test` registers a `@Bean MustacheFactory` with a non-default partial-loader and verifies the bridge uses it.

### Phase 1 — module skeleton + POM

1. New module `juneau-rest/juneau-rest-server-view-mustache/` with `pom.xml` mirroring `juneau-rest-server-mcp` and `juneau-rest-server-view-jsp` (once the latter lands).
2. POM dependencies (engine-agnostic per resolved decision #2):
    - `provided` scope: `com.github.spullara.mustache.java:compiler`.
    - `test` scope: `com.github.spullara.mustache.java:compiler` (so the bridge module can compile/test standalone).
3. Module skeleton compiles + ships an empty test to verify the build.
4. New `BasicMustacheResource_NoEngine_Test` asserts the diagnostic message when the test classpath is stripped of `mustache.java:compiler`.

### Phase 2 — `BasicMustacheResource` mixin

1. New class with default `@Rest(paths={"/mustache/*"}, responseProcessors={MustacheViewRenderer.class})` and `@RestGet(path="/*", swagger=@OpSwagger(ignore=true))` handler.
2. Builder accepts `basePath(String)`.
3. Default-factory construction logic: on first request, if no `MustacheFactory` bean is registered, build a `DefaultMustacheFactory` anchored on the importer's classloader, with the configured base-path as its resource-prefix.
4. Tests:
    - `BasicMustacheResource_RawRender_Test` — GET `/mustache/hello.mustache` renders `/templates/hello.mustache` correctly; request/session attributes are available in the template.
    - `BasicMustacheResource_BasePath_Test` — `basePath("/views/")` resolves correctly.
    - `BasicMustacheResource_NotFound_Test` — missing template → 404.
    - `BasicMustacheResource_Partials_Test` — `{{> header}}` partial includes resolve correctly relative to base-path.

### Phase 3 — `MustacheView` + `MustacheViewRenderer`

1. New value class `MustacheView implements View` (`templateName`, `Map<String,Object> attributes`, fluent `.attr(...)` API).
2. New `ResponseProcessor` impl `MustacheViewRenderer` that detects `MustacheView` returns, compiles the template via the active `MustacheFactory`, and calls `mustache.execute(res.getWriter(), view.getAttributes())`.
3. Auto-register `MustacheViewRenderer` via `@Rest(responseProcessors={MustacheViewRenderer.class})` on `BasicMustacheResource`.
4. Tests:
    - `MustacheView_Test` — `@RestGet` returning `MustacheView.of("hello.mustache").attr("name","world")` renders with the attribute available as `{{name}}` in the template.
    - `MustacheView_PojoScope_Test` — passing a POJO as a single-attribute scope renders correctly (reflection-driven property access).
    - `MustacheViewRenderer_NoEngine_Test` — when no Mustache engine is on the classpath, the renderer fails with a clear diagnostic naming the missing dependency.

### Phase 4 — example module

1. New module `juneau-examples/juneau-examples-rest-jetty-mustache/` mirroring `juneau-examples-rest-jetty` structure. Adds `com.github.spullara.mustache.java:compiler` to its own `pom.xml`.
2. Hello-World template at `src/main/resources/templates/hello.mustache` rendered by `@RestGet("/hello/{name}") public View hello(@Path String name)`.
3. Tests:
    - `JuneauMustacheExample_Test` — end-to-end smoke test through the example.

### Phase 5 — Spring Boot smoke tests

1. New tests in `juneau-utest` exercising the Spring `BeanStore` adapter.
2. Tests:
    - `BasicMustacheResource_Springboot_Test` — `@SpringBootTest(webEnvironment=RANDOM_PORT)` + embedded Tomcat. Register `BasicMustacheResource` + a `@Bean MustacheFactory` as Spring beans; GET `/mustache/hello.mustache` renders identically to the microservice form.
    - `MustacheView_Springboot_Test` — `@RestGet` returning `MustacheView` works under the Spring `BeanStore` adapter.
    - `BasicMustacheResource_Springboot_NoStarter_Test` — confirms that `spring-boot-starter-mustache` is NOT required (and in fact would be wrong — it brings jmustache, not mustache.java). The test deliberately omits the starter and verifies the bridge works with just `com.github.spullara.mustache.java:compiler` on the test classpath.

### Phase 6 — docs + release notes

1. Release-notes entries under `### juneau-rest-server-view-mustache (new module)` and a cross-reference under `### juneau-rest-server-springboot`.
2. New topic page `docs/pages/topics/MustacheViewSupport.md` walking through microservice and Spring Boot setup; partials usage; the engine-swap escape hatch for jmustache; the `spring-boot-starter-mustache` collision caveat.
3. Update `juneau-examples` index to include the new example module.
4. Module-addition entry in the current release-notes file (9.5.0 or successor).

## Acceptance criteria

- [ ] New module `juneau-rest-server-view-mustache` builds and tests pass standalone.
- [ ] Bridge module's POM contains **no Mustache-engine dependency** in `main` scope — only `com.github.spullara.mustache.java:compiler` (`provided`).
- [ ] Rendering works under both the bridge-default `DefaultMustacheFactory` (verified via the Jetty test) and a user-supplied custom `MustacheFactory` (verified via `BasicMustacheResource_CustomFactory_Test`).
- [ ] Example app `juneau-examples-rest-jetty-mustache` renders a template at `/hello/{name}` from a JAR classpath resource.
- [ ] `MustacheView`-returning `@RestGet` method dispatches via `MustacheViewRenderer` correctly.
- [ ] Partials (`{{> header}}`) resolve correctly relative to the configured base-path.
- [ ] No Mustache engine on classpath → clear diagnostic error message naming the missing dependency.
- [ ] Default base path `/` works; user opt-in to `/templates/` (or any other prefix) via the builder.
- [ ] Both the raw-rendering path (`/mustache/*` mount) and the `View`-returning path carry `@OpSwagger(ignore=true)` so they don't pollute generated OpenAPI specs.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test.
- [ ] Coverage ≥ 95% on the new module's main code. Full `./scripts/test.py` green; RAT clean.

## Test architecture

**Decision: tests live in `juneau-utest`, following the codebase convention** (and matching TODO-78's resolved test-architecture pattern, plus FINISHED-74/75/76/77's three-way deployment parity).

**Test class matrix (lives in `juneau-utest/src/test/java/org/apache/juneau/rest/view/mustache/`):**

| Test class | Container | MustacheFactory source | Verifies |
|---|---|---|---|
| `BasicMustacheResource_MockRest_Test` | `MockRest` baseline | Bridge default (`DefaultMustacheFactory`) | Baseline rendering, attribute passing, 404 on missing, base-path override, partials. Lightest-weight test class. |
| `BasicMustacheResource_Jetty_Test` | Real embedded Jetty via `MicroserviceTestFixture` | Bridge default | Bridge works under the Juneau-microservice-default container. Catches real-HTTP / real-classloader regressions. |
| `BasicMustacheResource_Springboot_Test` | `@SpringBootTest(webEnvironment=RANDOM_PORT)` + embedded Tomcat | User-supplied `@Bean MustacheFactory` | Full Spring Boot context + `SpringBeanStore` adapter end-to-end. |
| `BasicMustacheResource_Springboot_NoStarter_Test` | Same | No starter; raw `compiler` artifact only | Documents that `spring-boot-starter-mustache` (jmustache) is NOT required; the bridge works without it. |
| `BasicMustacheResource_NoEngine_Test` | `MockRest` (no real container) | None — Mustache stripped from classpath | Diagnostic message names the missing dep and links to the "Choosing a `MustacheFactory`" matrix. Error-path UX test. |
| `MustacheView_Test` / `MustacheView_Springboot_Test` | MockRest + Spring Boot | Bridge default + custom bean | `View`-return dispatch path through `MustacheViewRenderer`. |

**New deps `juneau-utest/pom.xml` picks up (all test scope):**

```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-rest-server-view-mustache</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.github.spullara.mustache.java</groupId>
    <artifactId>compiler</artifactId>
    <scope>test</scope>
</dependency>
```

Sibling view-module TODOs (TODO-82/84) inherit this test architecture by default.

## Resolved decisions

1. **Module name — `juneau-rest-server-view-mustache`.** Slots into the `juneau-rest-server-view-*` family established by TODO-78 (resolved decision #1 there).
2. **Mustache engine — `com.github.spullara.mustache.java:compiler` (`mustache.java`).** Chosen over `com.samskivert:jmustache` because: (a) `mustache.java` is lighter (fewer transitive deps), (b) `mustache.java` is more actively maintained as of the planning date, (c) `mustache.java`'s `MustacheFactory` API is cleaner for the bridge's lazy-resolution pattern. Trade-off: Spring Boot's `spring-boot-starter-mustache` ships jmustache, NOT `mustache.java` — so users on Spring Boot adding the starter expecting autoconfig will be surprised. Mitigation: documented in the topic page; users who prefer jmustache can swap by supplying their own `MustacheViewRenderer`-equivalent bean (the renderer is the engine-specific glue, the rest of the bridge is engine-agnostic).
3. **Engine bundling — engine-agnostic (Option B), mirrored from TODO-78 #2.** Bridge module declares only `com.github.spullara.mustache.java:compiler` in `provided` scope; consumers supply the runtime version. No default-engine bake-in.
4. **Spring Boot support — first-class.** Phase 5 ships explicit Spring Boot smoke tests. The risk surface is lower than TODO-78 (no servlet coupling, no fat-jar packaging gotcha), but the `spring-boot-starter-mustache` name collision (it ships jmustache) needs explicit documentation.
5. **Auto-register `MustacheViewRenderer` on the mixin — yes.** `@Rest(responseProcessors={MustacheViewRenderer.class})` declared on `BasicMustacheResource`.
6. **Default base path — `/`.** Most flexible. Example app uses `/templates/` to demonstrate the conventional layout.
7. **`MustacheView extends View` — yes.** `View` interface in core `juneau-rest-server` per TODO-78 resolved decision #6 (HARD dependency on TODO-78 landing first). `MustacheView` is the impl in this bridge module.
8. **Multi-base-path support — documented "register two beans" pattern, no special builder API.** Same convention as TODO-78 #7 and TODO-82 #7.

## Follow-on TODOs to track after this lands

- Coverage of the engine-agnostic POM stance across Tomcat / Jetty / external container deployments.
- Hot-reload during development (Mustache caching disabled per template); opt-in via the builder or a dev-mode auto-detect.
- Localization / i18n integration with Juneau's existing `Messages` infrastructure — Mustache is logic-less so the i18n integration is a thin shim that passes resolved messages in as scope attributes.
- First-class `com.samskivert:jmustache` bridge (separate `MustacheViewRenderer` variant) for users who prefer jmustache (or who already have `spring-boot-starter-mustache` on the classpath).

## Resolved decisions — OQA

1. **Default `index.mustache` template in the bridge — RESOLVED 2026-05-25 as NO.** Bridge ships no opinionated default template. Zero-config deployment with no user-supplied templates returns 404 with a clear "no template named X under base-path Y" message. Aligns with the plan's prior recommendation, the FINISHED-75 "no opinionated defaults in the mixin" stance, and the parallel TODO-82 OQA decision.
2. **`.mustache` vs `.html` extension default — RESOLVED 2026-05-25 as CONFIGURABLE.** Bridge default behavior is unchanged ("no implicit suffix" — the literal `templateName` passed to `MustacheView.of(...)` is used as-is, which matches `mustache.java`'s own convention). But the builder gains a `.templateSuffix(String suffix)` knob (default `""`/none) so users can opt into `.mustache`, `.html`, or any other implicit suffix without re-typing it at every call site. When set, the bridge appends the suffix to template names that don't already end with it (idempotent). Documented in the topic page (Phase 6) alongside the engine-collision warning. **Implementation note:** add the builder method in Phase 2 alongside `basePath(String)`; add a test case `BasicMustacheResource_TemplateSuffix_Test` in Phase 2 verifying both "no suffix" (default) and `.mustache` / `.html` suffix configurations.

## Open questions

_All open questions resolved 2026-05-25. See "Resolved decisions — OQA" above._

## Risks

- **Engine name collision with `spring-boot-starter-mustache`.** Users adding the starter expecting it to provide the bridge's runtime get jmustache, not `mustache.java`. Result: `ClassNotFoundException` on `com.github.spullara.mustache.java.MustacheFactory`. Mitigation: prominent topic-page warning + `BasicMustacheResource_NoEngine_Test`'s diagnostic message explicitly mentions both the correct dependency AND the starter-collision pitfall.
- **`mustache.java` reflective property access on records / sealed classes.** Newer JDK features (`record`, `sealed`) interact with `mustache.java`'s reflection-based unwrapping in subtle ways depending on the engine version. Mitigation: pin a known-good `mustache.java` version; verify on JDK upgrade.
- **Engine selection burden on the user (Option B trade-off).** Bridge ships no default engine; without `mustache.java:compiler` on the classpath, failure surfaces at first request. Mitigation: javadoc + module README + `BasicMustacheResource_NoEngine_Test` confirms the diagnostic is human-readable.
- **Partials path resolution under Spring Boot fat jars.** `mustache.java`'s `DefaultMustacheFactory` reads partials via the classloader, which works under fat jars — but verify in Phase 5 since this is the one place where the difference between `spring-boot:run` and the deployed jar could surface.
- **Module-level POM dep complexity.** Adding a new module is non-trivial. Mitigation: model after `juneau-rest-server-mcp` and `juneau-rest-server-view-jsp` (once landed).

## Related work

- `todo/TODO-78-mixin-jsp-module.md` — **HARD dependency.** TODO-78 introduces the generic `View` interface in core `juneau-rest-server` (its resolved decision #6) AND establishes the `ResponseProcessor`-based renderer pattern this module follows. This TODO **cannot land** before TODO-78. Also the closest sibling for module-shape / POM / test-architecture mirroring.
- `todo/TODO-82-view-module-thymeleaf.md` — sibling view-module TODO; same architecture; no inter-sibling dependency.
- `todo/TODO-84-view-module-freemarker.md` — sibling view-module TODO; same architecture; no inter-sibling dependency.
- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives this module builds on (transitively required).
- `todo/FINISHED-81-mixin-sub-context-inheritance.md` — sub-`RestContext` inheritance model; allows this module to declare its own serializer overrides scoped to mixin endpoints only.
- `todo/FINISHED-74-mixin-api-docs.md` — three-way deployment parity test-architecture template (MockRest baseline + real Jetty + real Spring Boot).
- `todo/FINISHED-75-mixin-static-files.md` — `@OpSwagger(ignore=true)` annotation precedent for excluding the raw-rendering endpoint from OpenAPI specs.
- `todo/FINISHED-76-mixin-convention-endpoints.md` and `todo/FINISHED-77-mixin-ops-introspection.md` — topic-page tone reference for the eventual docs phase.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter; Phase 5 smoke-test target.
- `juneau-microservice/juneau-microservice-jetty/` — Jetty microservice harness; Phase 4 example module structural reference.
- `juneau-examples/juneau-examples-rest-jetty/` — structural reference for `juneau-examples-rest-jetty-mustache`.
