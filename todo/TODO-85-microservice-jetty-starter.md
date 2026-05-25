# TODO-85: `juneau-microservice-jetty-starter` — zero-config quick-start module

Source: split out of the TODO.md backlog on 2026-05-23; plan written 2026-05-24.

## Goal

Add a new `juneau-microservice/juneau-microservice-jetty-starter/` Maven module — conceptually analogous to Spring Boot's `spring-boot-starter-web` — that lets a new user stand up a Juneau service on Jetty with **one Maven dep plus one factory call**:

```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-microservice-jetty-starter</artifactId>
    <version>9.5.0</version>
</dependency>
```

```java
public class App {
    public static void main(String[] args) throws Exception {
        JuneauJettyStarter.start(args, MyResource.class);
    }
}
```

That four-line `main()` brings up an embedded-Jetty service on port `10000`, mounts `MyResource` at its `@Rest(path=...)`, serves JSON/HTML/XML/URL-encoded/plain-text out of the box, and ships the four-mixin API-docs pack (FINISHED-74), the favicon + version convention endpoints (FINISHED-76), and a sensible `application.cfg` / `jetty.xml` / `log4j2.xml` baseline.

Goal: kill the "I need to read three doc pages and copy four files just to ship a hello-world Juneau service" friction that today's `juneau-my-jetty-microservice` archetype only partially solves.

## Why now

- The post-FINISHED-72 mixin family (FINISHED-74/75/76/77) makes the "what should ship by default?" question answerable for the first time — pre-mixin, the only way to add api-docs / favicon / version was to extend `BasicRestServlet`, which forced a specific class hierarchy.
- `JettyConfiguration` (`juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyConfiguration.java`) already exists as the canonical bootstrap seam — the starter wraps it rather than reinventing it.
- The `juneau-petstore-jetty` sample app (TODO-86) and `juneau-petstore-springboot` sample app (TODO-87) both want a zero-config quick-start to point readers at — TODO-85 is the prerequisite that makes those sample apps' `main()` methods actually look minimal.
- Spring Boot's `spring-boot-starter-web` is the conceptual benchmark for "how easy should a hello-world be?"; today Juneau loses that comparison by ~30 LOC and ~4 files. TODO-85 closes the gap.

## Scope

**In scope (v1):**

- New Maven module `juneau-microservice/juneau-microservice-jetty-starter/`. Parent POM: `juneau-microservice/pom.xml` (artifact `juneau-microservice-parent`). Add `<module>juneau-microservice-jetty-starter</module>` to the parent's `<modules>` list.
- POM dependency surface (all `compile` scope so consumers transitively pick everything up):
    - `juneau-microservice-jetty` — the existing Jetty bootstrap runtime.
    - `juneau-rest-server` — the REST runtime.
    - The default marshall stack — `juneau-marshall` brings JSON / HTML / XML / URL-encoded / plain-text; YAML pulled in via the same path the API-docs mixin already needs so `/openapi.yaml` works out of the box.
    - `juneau-config` — `Config` API used by `application.cfg`.
    - (Bundled mixin packs — see below; all are part of `juneau-rest-server`, so no new module dep is needed for them.)
- Default configuration assets shipped from the JAR's classpath (consumer's own `src/main/resources/` overrides via Juneau's standard config-merge precedence):
    - `META-INF/juneau-starter/application.cfg` — sensible defaults: `[Jetty] port=10000`, default host binding, no SSL, default content-types, `[REST] debug=conditional`, `[Logging] logFile=logs/juneau-starter.log`.
    - `META-INF/juneau-starter/jetty.xml` — minimal embedded-Jetty descriptor wired to `$C{Jetty/port}` via `$C` variable resolution; single `ServerConnector`, single `ServletContextHandler` at `/`.
    - `META-INF/juneau-starter/log4j2.xml` — minimal `Console` + `RollingFile` appenders, root level `INFO`, `org.apache.juneau` at `INFO`, `org.eclipse.jetty` at `WARN`.
- New class `org.apache.juneau.microservice.jetty.starter.JuneauJettyStarter` with one public factory:

    ```java
    public static Microservice start(String[] args, Class<?>... resources) throws Exception;
    ```

    The factory does the full bootstrap: builds a `Microservice` with `JettyConfiguration.class` plus a generated `@Configuration` class that registers each `resources[i]` instance as a `@Bean Servlet` (auto-discovered by `JettyServerComponent.onStart` per `juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyServerComponent.java:256`), calls `.start().startConsole().join()`, and returns the `Microservice` handle for advanced callers that want to query bean store / port / shutdown.
- New class `org.apache.juneau.microservice.jetty.starter.BasicJettyStarterServlet extends BasicRestServlet` — bundled default servlet that consumers can extend instead of `BasicRestServlet`. Declared as:

    ```java
    @Rest(
        mixins = {
            BasicSwaggerUiResource.class,    // FINISHED-74 — pulls BasicSwaggerResource transitively
            BasicRedocResource.class,        // FINISHED-74 — pulls BasicOpenApiResource transitively
            BasicFaviconResource.class,      // FINISHED-76
            BasicVersionResource.class       // FINISHED-76
        }
    )
    public abstract class BasicJettyStarterServlet extends BasicRestServlet { }
    ```

    Consumers who want the "everything bundled" experience extend `BasicJettyStarterServlet`; consumers who want fine-grained control extend `RestServlet` directly and pick mixins manually. Subclasses can override the inherited mixin set via the standard `@Rest(noInherit={"mixins"})` opt-out (FINISHED-74 parent-chain aggregation).
- README / module-level javadoc with the canonical four-line `main()` plus a "what's bundled by default, what's NOT" table.
- Tests live in **`juneau-utest/src/test/java/org/apache/juneau/microservice/jetty/starter/`** (per the FINISHED-74 / TODO-78 test-architecture template — see "Test architecture" below).

**Explicitly out of scope (v1):**

- A Maven archetype (`mvn archetype:generate -DarchetypeArtifactId=juneau-microservice-jetty-starter-archetype`) — `juneau-my-jetty-microservice` already serves this role for users who want a full project skeleton; v1 of the starter targets users who already have an empty Maven project and just want a one-dep dropp-in.
- A standalone "fat jar" assembly descriptor — defer; consumers can use `maven-shade-plugin` or `spring-boot-maven-plugin`'s repackage goal directly if they want a single-file deployable.
- A Spring Boot equivalent (`juneau-rest-server-springboot-starter`) — separate concern, separate TODO if requested; the Spring Boot ergonomic story is already largely solved by Spring Boot's own auto-config plus `juneau-rest-server-springboot`'s `BasicSpringRestServlet`.
- TLS / mutual-TLS defaults — port `10000` HTTP only in v1; users who need SSL drop in their own `jetty.xml` (which Juneau's config-merge precedence loads first).
- Built-in metrics / health / readiness endpoints — TODO-67 (observability) and FINISHED-77 (`BasicRouteIndexResource`) cover these; the starter does NOT bundle the ops/introspection pack by default (see resolved decision #2 below).

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm `Microservice.Builder` supports programmatically registering a generated `@Configuration` class (or alternatively confirm that the cleanest path is to register `Servlet` beans directly into the builder's `BeanStore` before `.start()`). Inspect `juneau-microservice/juneau-microservice/src/main/java/org/apache/juneau/microservice/Microservice.java`.
2. Confirm `JettyServerComponent.onStart` walks `store.getBeansOfType(Servlet.class)` — already verified at `juneau-microservice-jetty/.../JettyServerComponent.java:256`.
3. Confirm Juneau's config-merge precedence loads consumer's `src/main/resources/application.cfg` BEFORE the starter JAR's `META-INF/juneau-starter/application.cfg` (so consumer overrides win). Inspect `Microservice.Builder.configName(...)` / `configStore(...)` resolution.
4. Confirm the four bundled mixin classes (`BasicSwaggerUiResource`, `BasicRedocResource`, `BasicFaviconResource`, `BasicVersionResource`) are all reachable from `juneau-rest-server`'s classpath (they are — landed in FINISHED-74 and FINISHED-76).

### Phase 1 — module skeleton + POM

1. Create `juneau-microservice/juneau-microservice-jetty-starter/pom.xml` mirroring `juneau-microservice-jetty/pom.xml`'s shape (parent: `juneau-microservice-parent`, packaging: `jar`). Compile-scope deps per the surface above.
2. Add `<module>juneau-microservice-jetty-starter</module>` to `juneau-microservice/pom.xml`.
3. Add `juneau-microservice-jetty-starter` to the BOM in `juneau-distrib/pom.xml` if the BOM enumerates module artifacts.
4. Empty `package-info.java` + RAT-compliant license headers; module builds standalone.

### Phase 2 — `JuneauJettyStarter` factory + `BasicJettyStarterServlet`

1. New class `org.apache.juneau.microservice.jetty.starter.JuneauJettyStarter` with the `start(String[], Class<?>...)` factory per the Scope section. Implementation walks the `Class<?>...` array, instantiates each via no-arg reflection (with a clear diagnostic if a class lacks one), and registers each as a `Servlet` bean before `microservice.start()`.
2. New abstract class `org.apache.juneau.microservice.jetty.starter.BasicJettyStarterServlet extends BasicRestServlet` with the bundled-mixin `@Rest` declaration.
3. Package-level javadoc covers the bundled-mixin set + the `noInherit` opt-out.

### Phase 3 — default config assets

1. Add `juneau-microservice-jetty-starter/src/main/resources/META-INF/juneau-starter/application.cfg` per the Scope section.
2. Add `juneau-microservice-jetty-starter/src/main/resources/META-INF/juneau-starter/jetty.xml` — minimal `ServerConnector` + `ServletContextHandler` configured via `$C{Jetty/port}`.
3. Add `juneau-microservice-jetty-starter/src/main/resources/META-INF/juneau-starter/log4j2.xml` — `Console` + `RollingFile` appenders with sensible levels.
4. Verify each asset's classpath lookup works under both `mvn test` and a `mvn package`'d JAR.

### Phase 4 — tests

1. Add the starter as a test-scope dep on `juneau-utest/pom.xml`.
2. New test classes in `juneau-utest/src/test/java/org/apache/juneau/microservice/jetty/starter/`:
    - `JuneauJettyStarter_HappyPath_Test` — calls `JuneauJettyStarter.start(new String[0], HelloResource.class)`, asserts a real HTTP `GET /hello` round-trips, asserts the six API-docs URLs (`/api`, `/swagger`, `/openapi`, `/openapi.json`, `/openapi.yaml`, `/redoc`) all respond, asserts `/favicon.ico` and `/version` respond. Tears down via `microservice.stop()` in `@AfterAll`.
    - `JuneauJettyStarter_PortOverride_Test` — consumer's `src/test/resources/application.cfg` overrides `Jetty/port=0` (ephemeral); asserts the chosen port is reported via `Microservice.getInstance().getBeanStore().getBean(JettyServerComponent.class).orElseThrow().getPort()`.
    - `BasicJettyStarterServlet_NoInheritMixins_Test` — subclass declares `@Rest(noInherit={"mixins"}, mixins=BasicSwaggerResource.class)`; asserts only `/api` is mounted (the other five api-docs URLs + favicon + version are NOT).
    - `JuneauJettyStarter_NoArgConstructorMissing_Test` — passing a class without a no-arg constructor yields a clear diagnostic message naming the offending class.

### Phase 5 — docs + release notes

1. Release-notes entry under `### juneau-microservice-jetty-starter (new module)` in `juneau-docs`'s `docs/pages/release-notes/9.5.0.md`.
2. New topic page `docs/pages/topics/JuneauMicroserviceJettyStarter.md` walking through the four-line `main()` and the bundled-vs-not-bundled table.
3. Cross-reference under `### juneau-microservice-jetty` pointing at the starter as the recommended entry point.
4. Update the docs index / sidebar to register the new topic page.

## Acceptance criteria

- [ ] New module `juneau-microservice-jetty-starter` builds and tests pass standalone.
- [ ] Consumer adding the single POM dep + writing the four-line `main()` (`JuneauJettyStarter.start(args, MyResource.class);`) brings up a serving Jetty server on port `10000` with the bundled mixin set mounted on every `BasicJettyStarterServlet` subclass.
- [ ] Consumer extending `RestServlet` directly (NOT `BasicJettyStarterServlet`) gets a clean unfurnished resource with no auto-mounted mixins — the bundling is opt-in via the bundled servlet class.
- [ ] Bundled-by-default mixin set: `BasicSwaggerUiResource`, `BasicRedocResource`, `BasicFaviconResource`, `BasicVersionResource` (per resolved decision #2). `BasicStaticFilesResource` (FINISHED-75) and the ops/introspection pack (FINISHED-77) are NOT bundled — consumers opt in explicitly.
- [ ] Default `application.cfg` / `jetty.xml` / `log4j2.xml` ship in `META-INF/juneau-starter/` on the JAR's classpath; consumer's own `src/main/resources/application.cfg` overrides via Juneau's standard config-merge precedence.
- [ ] `mvn test` and a packaged JAR both resolve the bundled config assets correctly.
- [ ] No new RAT-header violations; full `./scripts/test.py` green; coverage ≥ 90% on the new module's main code.
- [ ] Release-notes entry + topic page landed in `juneau-docs`.

## Test architecture

**Decision: tests live in `juneau-utest`, following the FINISHED-74 / TODO-78 convention.** Every production module under `juneau-microservice/` and `juneau-rest/` ships with an empty `src/test/`; all integration tests live in `juneau-utest`. The starter follows the same pattern. `juneau-utest/pom.xml` already pulls in `juneau-microservice-jetty` as a test-scope dep (per FINISHED-74's Phase 5), so adding `juneau-microservice-jetty-starter` is a one-line POM change rather than a structural shift.

**Alternative considered:** module-local `src/test/` in `juneau-microservice-jetty-starter/` itself (rationale: the starter is intrinsically an integration concern; "the starter actually starts a serving server" is the contract under test, which feels closer to a self-test than a unit test). **Rejected** because: (a) the codebase convention is unambiguous — every existing `juneau-microservice/*` and `juneau-rest/*` module is empty `src/test/`; (b) the starter's tests need `juneau-utest`'s existing JDK `HttpClient` scaffolding to assert real HTTP round-trips; and (c) divergence would create a "where do I find the starter tests?" cognitive cost identical to the one TODO-78 articulated and rejected.

## Resolved decisions

All open questions resolved 2026-05-24.

1. **Module name — `juneau-microservice-jetty-starter`.** Mirrors `spring-boot-starter-web`'s naming convention. The `-starter` suffix is the well-known "this brings in everything you need" signal across the Java ecosystem.
2. **Bundled mixin set: API-docs (FINISHED-74) + favicon + version (FINISHED-76).** Specifically `BasicSwaggerUiResource`, `BasicRedocResource`, `BasicFaviconResource`, `BasicVersionResource` are bundled on `BasicJettyStarterServlet`. **Static-files mixin (FINISHED-75) is NOT bundled** because empty-classpath default + greedy `/*` matcher tends to surprise consumers who weren't expecting it (and Spring Boot makes the same trade-off — static resources are a separate opt-in via `spring-boot-starter-thymeleaf` etc.). **Ops/introspection pack (FINISHED-77) is NOT bundled** because `BasicEchoResource` is Debug-gated (safe) but `BasicAdminResource` ships deny-all and exposing the route would be spec-noise on every starter-deployed service that doesn't actually want admin endpoints. Consumers who want either pack add the mixin explicitly to their resource class — one line.
3. **Bundled servlet class — `BasicJettyStarterServlet extends BasicRestServlet` with `@Rest(mixins=...)` declaring the bundled set.** Considered alternative: ship factory helpers only and force consumers to write `@Rest(mixins={...})` themselves. Rejected — the whole point of a starter is "the boilerplate is gone." Consumers who want fine-grained control extend `RestServlet` directly and opt in mixin-by-mixin, mirroring how Spring Boot consumers can bypass `@SpringBootApplication`'s auto-config via `@EnableAutoConfiguration(exclude=...)`.
4. **Versioning policy — starter version tracks core Juneau version 1:1.** Always-released alongside the parent reactor; no independent versioning. Simplifies the "which starter version pairs with which Juneau core version?" question for users (answer: always the same).
5. **Bootstrap class name — `JuneauJettyStarter`.** Class name matches the module name (`juneau-microservice-jetty-starter` → `JuneauJettyStarter`); factory method `start(String[], Class<?>...)` matches Spring Boot's `SpringApplication.run(Class<?>, String...)` signature shape for muscle-memory portability. Package `org.apache.juneau.microservice.jetty.starter` keeps it cleanly sub-packaged so future siblings (`SpringBootStarter`?) can land at peer paths without name collisions.
6. **Default port `10000`.** Matches the existing `juneau-examples-rest-jetty` default so users moving between the starter and the example app don't have to retune their browser bookmarks. Consumer overrides via `[Jetty] port=...` in their own `application.cfg`.
7. **Test home — `juneau-utest`.** See "Test architecture" above.
8. **POM packaging — `jar`.** Not `bundle` (the OSGi `maven-bundle-plugin` packaging used by `juneau-examples-rest-jetty`) — the starter is a plain library JAR and the OSGi manifest overhead provides no value here.

## Open questions

None anticipated. Design is well-anchored against Spring Boot's `spring-boot-starter-web` precedent and the existing `JettyConfiguration` bootstrap seam. Surface for surprises:

- **Bundled YAML serializer footprint.** `/openapi.yaml` works out of the box (per the bundled `BasicRedocResource` → `BasicOpenApiResource` transitive resolution), which means the YAML serializer module must be on the classpath. Phase 0 step #4 verifies this — if YAML is a separate optional module rather than part of `juneau-rest-server`'s default deps, TODO-85 adds it explicitly. Note the cross-link to TODO-88 (YAML buffer-underflow on large OpenAPI 3.1 docs) — TODO-85 inherits whatever behavior TODO-88 lands.

## Risks

- **Bundled-mixin set is opinionated.** Consumers who disagree with the bundling can extend `RestServlet` directly, but the friction of "I have to know the bundled set isn't what I want" is real. Mitigation: the "what's bundled" table in the topic page is the primary recovery path; the `noInherit={"mixins"}` opt-out gives a per-resource escape hatch without forcing consumers off `BasicJettyStarterServlet`.
- **Config-merge precedence drift.** If a future Juneau core change reorders config-merge precedence so that the JAR's bundled `application.cfg` wins over the consumer's `src/main/resources/application.cfg`, the starter silently breaks consumer configuration. Mitigation: explicit test `JuneauJettyStarter_PortOverride_Test` asserts consumer overrides win — catches the regression.
- **Embedded Jetty version drift.** `juneau-microservice-jetty` pins Jetty 12 EE11 (per the `jetty-ee${jetty.ee.version}` property); a future Jetty 13 / EE12 upgrade in `juneau-microservice-jetty` flows transitively into the starter. Mitigation: documented as a coordinated upgrade — same risk as `juneau-microservice-jetty` itself carries today; no new exposure.
- **Reflection-based no-arg-constructor instantiation in `JuneauJettyStarter.start(...)`.** Consumers who write `@Rest`-annotated resources requiring constructor injection (rare but possible) get an unhelpful `NoSuchMethodException`. Mitigation: the `JuneauJettyStarter_NoArgConstructorMissing_Test` acceptance criterion exercises the diagnostic; consumers needing constructor injection use the full `Microservice.create()...` builder pattern directly rather than the starter factory, which is still supported (and documented in the topic page's "Going beyond the starter" section).

## Related work

**Dependency relationships:**

- **No hard dependencies.** TODO-85 is a standalone module — can land any time.
- **Soft dependencies** (improve TODO-85 if landed first, but TODO-85 ships without them):
    - FINISHED-74 (API-docs mixin pack) — landed; bundled by default.
    - FINISHED-76 (convention endpoints — favicon + version) — landed; bundled by default.
    - FINISHED-75 (static files) — landed; NOT bundled (per resolved decision #2), but available to consumers as a one-line `@Rest(mixins=...)` add.
    - FINISHED-77 (ops/introspection) — landed; NOT bundled (per resolved decision #2), but available similarly.
- **Downstream consumers:**
    - **TODO-86** (`juneau-petstore-jetty` sample app) — soft dep; uses TODO-85's starter if landed, otherwise falls back to direct `juneau-microservice-jetty` + manual `Microservice.create()...` boilerplate. Whichever of TODO-85 / TODO-86 lands second updates the other to use the starter.
    - **TODO-87** (`juneau-petstore-springboot` sample app) — does NOT consume TODO-85 (Spring Boot has its own equivalent ergonomics via `@SpringBootApplication` + `BasicSpringRestServlet`). TODO-87's `pom.xml` does not depend on `juneau-microservice-jetty-starter`.

**File references:**

- `juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyConfiguration.java` — the bootstrap seam this starter wraps.
- `juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyServerComponent.java` — the Servlet auto-discovery seam (`onStart` walks `store.getBeansOfType(Servlet.class)`).
- `juneau-microservice/juneau-my-jetty-microservice/` — the existing archetype-style project skeleton; the starter is the "I don't want a skeleton, I just want a dep" alternative, not a replacement.
- `juneau-examples/juneau-examples-rest-jetty/src/main/java/org/apache/juneau/examples/rest/jetty/App.java` — the canonical eight-line `Microservice.create()...` bootstrap the starter shrinks to four lines.
- `todo/FINISHED-74-mixin-api-docs.md` — bundled mixin classes (`BasicSwaggerUiResource`, `BasicRedocResource`).
- `todo/FINISHED-76-mixin-convention-endpoints.md` — bundled mixin classes (`BasicFaviconResource`, `BasicVersionResource`).
- `todo/TODO-86-petstore-jetty-app.md` — soft-dep consumer; sample app exercises the starter.
