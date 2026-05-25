# TODO-87: `juneau-petstore-springboot` — Spring Boot mirror of the petstore sample

Source: split out of the TODO.md backlog on 2026-05-23; plan written 2026-05-24.

## Goal

Spring Boot mirror of TODO-86's `juneau-petstore-jetty` sample app. Shares the bean / domain / repository layer 1:1 with TODO-86 via the `juneau-petstore-core` module, deploys via Spring Boot's `@SpringBootApplication` + `ServletRegistrationBean` + `BasicSpringRestServlet`, and serves the **same** petstore API surface at the **same** URLs that TODO-86 serves on Jetty.

End-state developer experience:

```bash
cd juneau-petstore/juneau-petstore-springboot
mvn spring-boot:run
# (or)
mvn package && java -jar target/juneau-petstore-springboot-9.5.0.jar
# Petstore live on http://localhost:5000
#   /api, /swagger, /openapi, /openapi.json, /openapi.yaml, /redoc
#   /pets, /orders, /customers
#   /favicon.ico, /version, /robots.txt
#   /routes
#   /views/about
#   /admin/*
```

Most of TODO-86's content carries over verbatim — this plan is mostly **delta**: what changes between the Jetty deployment and the Spring Boot deployment, and what test surface proves they're equivalent.

## Why now

- Demonstrates the "byte-identical content across deployment modes" claim FINISHED-74 makes concrete via the `BasicApiDocs_JettyMicroservice_Test` + `BasicApiDocs_Springboot_Test` parity pairing. TODO-87 extends that template from "the api-docs mixin pack works on both" to "an entire real sample app works on both."
- Serves as the canonical reference for users migrating an existing Spring Boot app onto Juneau REST — they have a working `@SpringBootApplication` to read alongside the Jetty equivalent.
- Closes the petstore-family triplet: TODO-85 (starter, no deps) → TODO-86 (Jetty sample) → TODO-87 (Spring Boot sample, same domain). Lands as part of the petstore family's coherent landing.

## Scope

**In scope (v1):**

### 1. Maven module

- `juneau-petstore/juneau-petstore-springboot/` — sibling to `juneau-petstore-jetty` under the same parent (defined by TODO-86). Already registered as a `<module>` entry in `juneau-petstore/pom.xml` by TODO-86; TODO-87 fills in the actual code.
- POM modeled after `juneau-examples/juneau-examples-rest-springboot/pom.xml`:
    - `juneau-petstore-core` (compile) — bean/domain layer shared with TODO-86.
    - `juneau-rest-server-springboot` (compile) — `BasicSpringRestServlet` + `SpringBeanStore` adapter.
    - `spring-boot-starter-web` (compile, exclude `spring-boot-starter-logging`) — embedded Tomcat + DispatcherServlet plumbing.
    - View-engine deps matching TODO-86's choice — Thymeleaf preferred (`thymeleaf` + `thymeleaf-spring6`), Spring Boot's auto-config picks it up automatically. (JSP fallback per TODO-86 resolved decision #3 routes through `tomcat-embed-jasper`.)
    - `spring-boot-maven-plugin` with `repackage` goal so `java -jar` works on the fat jar.

### 2. Hard dependency on `juneau-petstore-core` — shared 1:1 with TODO-86

Every bean (`Pet`, `Order`, `Customer`, `Category`, `Tag`), every repository interface (`PetRepository`, `OrderRepository`, `CustomerRepository`), and every fixture (`PetstoreFixtures.seed(...)`) comes from `juneau-petstore-core`. **No bean or repository class is re-declared in `juneau-petstore-springboot`.**

### 3. Spring-idiomatic bean factories

The `@SpringBootApplication` class wires the petstore via `@Bean` factories in the Spring-native style:

```java
@SpringBootApplication
@Controller
public class App {

    public static void main(String[] args) {
        new SpringApplicationBuilder(App.class).run(args);
    }

    // --- shared core ---
    @Bean public PetRepository petRepository() {
        var repo = new InMemoryPetRepository();
        PetstoreFixtures.seedPets(repo);
        return repo;
    }
    @Bean public OrderRepository orderRepository() { ... }
    @Bean public CustomerRepository customerRepository() { ... }

    // --- REST resources (auto-discovered by BasicSpringRestServlet via SpringBeanStore) ---
    @Bean public PetstoreRootResource petstoreRootResource() { return new PetstoreRootResource(); }
    @Bean public PetResource petResource() { return new PetResource(); }
    @Bean public OrderResource orderResource() { return new OrderResource(); }
    @Bean public CustomerResource customerResource() { return new CustomerResource(); }
    @Bean public AdminPetResource adminPetResource() { return new AdminPetResource(); }
    @Bean public AboutResource aboutResource() { return new AboutResource(); }

    // --- Juneau mixin/provider beans ---
    @Bean public OpenApiProvider openApiProvider() { return new BasicOpenApiProvider(); }
    @Bean public BasicVersionResource version() { return BasicVersionResource.create().fromManifest().build(); }
    @Bean public BasicSeoResource seo() { return BasicSeoResource.create().robotsAllow("/pets", "/orders").build(); }
    @Bean public BasicFaviconResource favicon() { return BasicFaviconResource.create().build(); }
    @Bean public BasicStaticFilesResource staticFiles() { return new BasicStaticFilesResource(); }

    // --- AuthN guards (TODO-69 once landed; DenyAllGuard fallback) ---
    @Bean public RestGuardList authGuards() {
        // TODO: once TODO-69 lands, return RestGuardList.of(BearerTokenGuard.create()...).
        return RestGuardList.of(new DenyAllGuard());
    }

    // --- root servlet registration ---
    @Bean
    public ServletRegistrationBean<jakarta.servlet.Servlet> rootServlet(PetstoreRootResource root) {
        return new ServletRegistrationBean<>(root, "/*");
    }
}
```

`PetstoreRootResource` extends `BasicSpringRestServletGroup` (NOT `BasicSpringRestServlet`) since it's a router page with `@Rest(children=...)`. Child resources are `BasicSpringRestServlet` subclasses so the `SpringBeanStore` resolution chain works for nested dependency lookups.

### 4. Same six API-docs URLs as TODO-86

`PetstoreRootResource` declares the same `@Rest(mixins={BasicSwaggerUiResource.class, BasicRedocResource.class, BasicStaticFilesResource.class, BasicFaviconResource.class, BasicVersionResource.class, BasicSeoResource.class, BasicRouteIndexResource.class, BasicEchoResource.class, BasicAdminResource.class})` declaration as the Jetty variant. The mixin-instance resolution uses `SpringBeanStore` instead of `BasicBeanStore` but the URL surface is identical.

### 5. Three-way deployment parity (FINISHED-74 template)

TODO-87's integration test in `juneau-utest` is a near-clone of TODO-86's, asserting the SAME content shapes against the SAME URLs come back from the SAME `juneau-petstore-core` repositories — only the deployment seam changes. Per FINISHED-74's `BasicApiDocs_JettyMicroservice_Test` + `BasicApiDocs_Springboot_Test` precedent, the two test classes (TODO-86's smoke test + TODO-87's smoke test) share a `PetstoreTestFixtures` helper that encodes the canonical assertions, so a regression in either deployment path is caught by the other's smoke test.

**Explicitly out of scope (v1):**

- Spring Cloud / Eureka / service-discovery integration — separate concern; sample is a single-instance deployment.
- Spring Security integration — `BearerTokenGuard` (TODO-69) is the canonical Juneau AuthN seam; users wanting Spring Security write a `RestGuard` adapter (per FINISHED-77's notes), but the sample doesn't ship one.
- Spring Data / JPA repositories — `juneau-petstore-core`'s in-memory impls are used as-is; database persistence is a TODO-86 / TODO-87 shared v2 concern.
- Actuator endpoints — overlap with FINISHED-77's `BasicAdminResource`; sample sticks with the Juneau mixin to keep the cross-deployment story symmetric.
- Spring Boot DevTools / live-reload — useful for development; not a sample-app feature.
- Reactive (Spring WebFlux) variant — Juneau is servlet-stack; reactive is a separate axis.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm Spring Boot version pinned by the rest of the codebase (`spring.version` property in the root `juneau/pom.xml` or `juneau-distrib/pom.xml`). TODO-87 tracks whatever version `juneau-rest-server-springboot/pom.xml` already uses; no new version dial.
2. Confirm `BasicSpringRestServlet` + `BasicSpringRestServletGroup` + `SpringBeanStore` are the canonical bean-store seam (per `juneau-rest/juneau-rest-server-springboot/src/main/java/org/apache/juneau/rest/springboot/`). **Note: `JuneauRestInitializer` does not exist as a class in the current codebase** — the actual Spring Boot wiring pattern is `BasicSpringRestServlet` (auto-uses `SpringBeanStore` via the surrounding `ApplicationContext`) + `ServletRegistrationBean` for the top-level mount, as already demonstrated by `juneau-examples-rest-springboot` and by FINISHED-74's `BasicApiDocs_Springboot_Test`.
3. Confirm TODO-86 has landed `juneau-petstore-core` (hard dep). If TODO-86 hasn't landed, TODO-87 cannot start — petstore-family landing order matters.
4. Confirm the view-engine choice TODO-86 picked (Thymeleaf preferred, JSP fallback). TODO-87 inherits whichever choice TODO-86 shipped with — the `AboutResource` source is shared from `juneau-petstore-core`'s resource layer if both deployments use the same engine.
5. Confirm `BasicVersionResource`'s `MANIFEST.MF` lookup works under Spring Boot fat-jar packaging — per FINISHED-76's dependency-injection notes, the importer's classloader path is the right one; `BasicVersionResource_SpringbootJar_Test` (landed in FINISHED-76) is the regression net.

### Phase 1 — `juneau-petstore-springboot` skeleton

1. Create `juneau-petstore/juneau-petstore-springboot/pom.xml` per the Scope §1 deps.
2. Create the package layout: `org.apache.juneau.petstore.springboot.{App, *Resource, *Configuration}`. Reuse resource classes from a shared `org.apache.juneau.petstore.rest.*` package if TODO-86 factored them out into `juneau-petstore-core`; otherwise duplicate the resource layer in `org.apache.juneau.petstore.springboot.rest.*` (cleaner alternative: factor the REST resources into a `juneau-petstore-rest` module shared between Jetty and Spring Boot — see open question #1).

### Phase 2 — `App.java` Spring Boot entry point

1. `@SpringBootApplication` + `@Controller` class per the Scope §3 template.
2. `@Bean` factories for repos, REST resources, mixin beans, guard list, and `ServletRegistrationBean`.
3. `application.yml` (Spring Boot idiom) configures: `server.port=5000`, Thymeleaf prefix/suffix if needed, logging levels.

### Phase 3 — verify mixin parity with TODO-86

Manual verification that every mixin from TODO-86's `PetstoreRootResource` declaration also lands on TODO-87's. The Phase 4 smoke test is the regression net; this phase is a code-review checklist:

- `BasicSwaggerUiResource`, `BasicRedocResource` (FINISHED-74)
- `BasicStaticFilesResource` (FINISHED-75)
- `BasicFaviconResource`, `BasicVersionResource`, `BasicSeoResource` (FINISHED-76)
- `BasicRouteIndexResource`, `BasicEchoResource`, `BasicAdminResource` (FINISHED-77)
- Thymeleaf or JSP view mixin (TODO-82 or TODO-78)

### Phase 4 — integration test in `juneau-utest`

Model after FINISHED-74's `BasicApiDocs_Springboot_Test`. Single smoke test class in `juneau-utest/src/test/java/org/apache/juneau/petstore/springboot/`:

- `PetstoreSpringboot_Smoke_Test` — `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = App.class)`. Test methods walk the **same** matrix as TODO-86's `PetstoreJetty_Smoke_Test` (`a01_apiDocs_sixUrls`, `a02_pets_crud`, `b01_staticFiles`, etc.), sharing a `PetstoreTestFixtures` helper class with the Jetty smoke test.
- Per FINISHED-74's `BasicApiDocsTestFixtures` precedent, the fixture asserts **content shape** (status code, `Content-Type` prefix, must-contain body substring), not byte-identity — request-derived fields like `host` / `servers` in the OpenAPI doc legitimately differ between deployments.
- HTTP client: JDK `HttpClient` (not `TestRestTemplate`, which was restructured in Spring Boot 4.0 and is on the deprecation path — per FINISHED-74's session notes).

### Phase 5 — docs + release notes

1. Update `docs/pages/topics/PetstoreSample.md` (created by TODO-86) with a new "Deploying on Spring Boot" sub-section walking through `App.java`, the `@Bean` factories, the `ServletRegistrationBean` mount, and `mvn spring-boot:run` / `java -jar` modes.
2. Release-notes entry under the same `### juneau-petstore (new)` section TODO-86 created.
3. Cross-reference under `### juneau-rest-server-springboot` pointing at TODO-87 as the canonical Spring Boot reference app.

## Acceptance criteria

- [ ] New module `juneau-petstore-springboot` builds and tests pass standalone.
- [ ] `mvn spring-boot:run` and `java -jar` both bring up the petstore on port `5000` serving the full API surface from TODO-86's §4 table, the same six API-docs URLs, the same static files, the same convention endpoints, the same ops/introspection routes, and the same Thymeleaf (or JSP) `/views/about` page.
- [ ] `juneau-petstore-springboot` depends on `juneau-petstore-core` only — no bean / repository / fixture class re-declaration.
- [ ] Spring-idiomatic `@Bean` factories wire every REST resource + every mixin bean + the `RestGuardList` + the `ServletRegistrationBean`. No `JuneauRestInitializer` reference (it does not exist in the codebase per Phase 0 #2).
- [ ] `PetstoreSpringboot_Smoke_Test` shares its assertion fixture with TODO-86's `PetstoreJetty_Smoke_Test` via `PetstoreTestFixtures`; both pass.
- [ ] FINISHED-74's three-way deployment parity claim extends to the petstore family — same fixture green on MockRest baseline (implicit via existing tests), real Jetty (TODO-86's smoke test), and real `@SpringBootTest` + embedded Tomcat (this TODO's smoke test).
- [ ] AuthN guards: `AdminPetResource`'s admin paths return 403 (FINISHED-77 default) or 401 (once TODO-69's `BearerTokenGuard` is wired); migration documented.
- [ ] No new RAT-header violations; full `./scripts/test.py` green.

## Test architecture

**Decision: tests live in `juneau-utest`, mirroring TODO-86 and FINISHED-74's Spring Boot Phase 5 template.** The `juneau-petstore-springboot` module's `src/test/` stays empty. `juneau-utest/pom.xml` already pulls in `spring-boot-starter-test` (excluded `spring-boot-starter-logging`) per FINISHED-74's session notes, so `@SpringBootTest(webEnvironment=RANDOM_PORT)` is available without further POM changes — TODO-87 only needs to add `juneau-petstore-springboot` as a test-scope dep.

**Single smoke test class strategy** mirrors TODO-86; the petstore-springboot's full feature matrix lives in `PetstoreSpringboot_Smoke_Test`, with the shared `PetstoreTestFixtures` helper enforcing parity against TODO-86's Jetty smoke test.

**Fidelity: real `@SpringBootTest` + embedded Tomcat, not Mockito-mocked `ApplicationContext`.** Per FINISHED-74's session-notes rationale, "identical content across deployment modes" is only meaningful if each path actually runs the same serializer + content-negotiation pipeline end-to-end. Mocking the Spring context invalidates the parity claim.

## Resolved decisions

1. **Spring Boot version pin.** Track whatever the rest of the codebase uses (`spring.version` property in `juneau/pom.xml`). No new version dial introduced by TODO-87.
2. **Deployment seam — `BasicSpringRestServlet` + `ServletRegistrationBean` + `SpringBeanStore`.** Matches the existing `juneau-examples-rest-springboot/.../App.java` precedent and FINISHED-74's Phase 5 test infrastructure. Considered: a custom `WebApplicationInitializer`-style entry point (`JuneauRestInitializer` per the original TODO-87 source bullet); rejected because **`JuneauRestInitializer` does not exist as a class in the current codebase** — the canonical Spring Boot wiring is the `BasicSpringRestServlet` + `ServletRegistrationBean` pair, already proven by FINISHED-74's `BasicApiDocs_Springboot_Test`.
3. **Thymeleaf as the view engine.** Matches TODO-86's choice. Spring Boot's auto-config picks up Thymeleaf the moment the `thymeleaf` dep is on the classpath, which makes the Spring Boot variant strictly simpler than the Jetty variant for this feature — good for the cross-engine showcase.
4. **No `spring-boot-starter-parent`.** The `juneau-rest-server-springboot` consumer pattern (already proven by `juneau-examples-rest-springboot`) uses Spring Boot as a regular dep with `spring-boot-maven-plugin` for the repackage step. Mirroring keeps consistency.
5. **`mvn spring-boot:run` AND `java -jar` both supported.** Per `juneau-examples-rest-springboot/pom.xml` precedent; the repackage goal produces a fat jar at `target/juneau-petstore-springboot-9.5.0.jar`.
6. **Default port `5000`.** Matches the existing `juneau-examples-rest-springboot/.../App.java` default; differs from TODO-85's `10000` Jetty default. Documented in `PetstoreSample.md`'s "Deploying on Spring Boot" sub-section.
7. **Spring `@Primary` / `@Qualifier` for multiple `OpenApiProvider` candidates** — per FINISHED-74's `BasicApiDocs_Springboot_MultiOpenApiProvider_Test` precedent, the `SpringBeanStore` adapter honors `@Primary` correctly. TODO-87's sample ships a single `@Bean OpenApiProvider` so the multi-provider case doesn't come up; documented as a Spring-side rough edge in `PetstoreSample.md`.
8. **`application.yml` (NOT `application.cfg`) for Spring-side config.** The Spring Boot half uses Spring's idiomatic config format; the Jetty half uses Juneau's `Config` API + `application.cfg`. Documented in `PetstoreSample.md` as an intentional asymmetry — each deployment uses its host platform's native config format.

## Open questions

1. **REST resource class sharing between TODO-86 and TODO-87.** TODO-86 currently lands `PetResource` / `OrderResource` / `CustomerResource` / `AdminPetResource` / `AboutResource` under `juneau-petstore-jetty/src/main/java/org/apache/juneau/petstore/jetty/rest/`. TODO-87 has three options:
    - **(a)** Duplicate the resource classes in `juneau-petstore-springboot/src/main/java/.../springboot/rest/` — simple, but bug-fix surface is doubled.
    - **(b)** Promote the resource layer to `juneau-petstore-core` (or a new `juneau-petstore-rest` module) — shared 1:1; TODO-86 and TODO-87 only contribute their respective `App.java` bootstrap and deployment wiring. **Recommend (b)** — matches the "core is shared" pattern already established for beans/repositories; resource classes are themselves deployment-agnostic (a `@RestGet` method doesn't know whether it's mounted on Jetty or Spring Boot).
    - **(c)** Leave them in `juneau-petstore-jetty` and have `juneau-petstore-springboot` depend on `juneau-petstore-jetty` — rejected: forces Spring Boot consumers to pull Jetty transitive deps, which is the opposite of "the deployments are independent."
    - **Suggested resolution:** (b), promoted to a new `juneau-petstore-rest` module under `juneau-petstore/`. TODO-86's plan can be updated to land that module as part of its Phase 1 (instead of putting REST resources under `juneau-petstore-jetty/.../rest/`). This is a coordination point between TODO-86 and TODO-87 worth flagging to the reviewer before either lands.

## Risks

- **`JuneauRestInitializer` name confusion.** The original TODO.md source bullet (line 52) references `JuneauRestInitializer` as the deployment seam. That class does not exist in the current codebase per Phase 0 #2. Mitigation: this plan documents the actual seam (`BasicSpringRestServlet` + `ServletRegistrationBean` + `SpringBeanStore`); the original bullet language can stay in TODO.md historically.
- **Spring Boot fat-jar manifest resolution.** `BasicVersionResource`'s `MANIFEST.MF` lookup needs to find the importer's manifest under Spring Boot's repackaged layout (`BOOT-INF/classes/META-INF/MANIFEST.MF` per FINISHED-76's notes). Mitigation: FINISHED-76's `BasicVersionResource_SpringbootJar_Test` is the regression net.
- **Bean-name collisions.** Spring's bean-name resolution may collide if a `@Bean` method name shadows an autoconfigured Spring Boot bean. Mitigation: explicit `@Bean(name="...")` qualifiers on anything that risks collision; smoke test surfaces collisions at context-load time.
- **Spring Boot version drift.** A future Spring Boot major-version bump may shift the `ServletRegistrationBean` API or the `BeanDefinitionOverrideException` behavior. Mitigation: same risk every Spring Boot consumer carries; tracked at the `juneau-rest-server-springboot` level, not introduced fresh here.
- **Petstore-family landing order.** TODO-87 has a HARD dep on `juneau-petstore-core` (defined in TODO-86). If TODO-87 lands before TODO-86, the build breaks. Mitigation: the recommended landing order in TODO-86's "Related work" section makes the dep explicit; reviewer enforces "TODO-86 lands first, or TODO-86 + TODO-87 land together."
- **Spring Security users feeling left out.** Sample uses `BearerTokenGuard` (TODO-69) rather than Spring Security. Mitigation: `PetstoreSample.md` includes a "Using Spring Security instead of Juneau guards" sub-section pointing at the FINISHED-77 documentation for the adapter pattern; not implemented in v1.

## Related work

**Dependency relationships:**

- **Hard dependencies:**
    - `juneau-petstore-core` — defined by TODO-86; TODO-87 consumes the same beans/repositories/fixtures.
    - FINISHED-74 (API-docs mixin pack — Spring Boot path proven by `BasicApiDocs_Springboot_Test`).
    - FINISHED-75/76/77 (mixin family — each has a Spring Boot smoke test landed alongside the FINISHED work).
    - FINISHED-73 (runtime-overridable `@Rest(paths=...)` — Spring Boot proven by `RestPathsRuntimeOverride_Springboot_Test`).
    - `juneau-rest-server-springboot` — the `BasicSpringRestServlet` + `SpringBeanStore` adapter.
- **Soft dependencies:**
    - TODO-69 (AuthN guards) — preferred for `AdminPetResource`'s admin guard chain; `DenyAllGuard` (FINISHED-77) fallback per TODO-86's resolved decision.
    - TODO-82 (Thymeleaf view) preferred; TODO-78 (JSP) fallback. TODO-87 inherits whichever TODO-86 ships with.
- **No dependency on TODO-85.** Spring Boot has its own equivalent ergonomics via `@SpringBootApplication` + `BasicSpringRestServlet`; the `juneau-microservice-jetty-starter` module is Jetty-only by name and scope. TODO-87's `pom.xml` does NOT pull TODO-85 in.

**Recommended landing order in the petstore family:** TODO-85 first (standalone, no deps), then `juneau-petstore-core` + TODO-86 + TODO-87 as a single coherent landing.

**File references:**

- `juneau-examples/juneau-examples-rest-springboot/pom.xml` — POM shape reference.
- `juneau-examples/juneau-examples-rest-springboot/src/main/java/org/apache/juneau/examples/rest/springboot/App.java` — `@SpringBootApplication` + `@Bean` factory pattern reference.
- `juneau-rest/juneau-rest-server-springboot/src/main/java/org/apache/juneau/rest/springboot/BasicSpringRestServlet.java` — REST servlet base class.
- `juneau-rest/juneau-rest-server-springboot/src/main/java/org/apache/juneau/rest/springboot/BasicSpringRestServletGroup.java` — router base class for `PetstoreRootResource`.
- `juneau-rest/juneau-rest-server-springboot/src/main/java/org/apache/juneau/rest/springboot/SpringBeanStore.java` — `BeanStore` adapter.
- `juneau-utest/src/test/java/org/apache/juneau/rest/docs/BasicApiDocs_Springboot_Test.java` — Phase 4 smoke test template.
- `juneau-utest/src/test/java/org/apache/juneau/rest/docs/BasicApiDocs_Springboot_MultiOpenApiProvider_Test.java` — `@Primary` / `BeanDefinitionOverrideException` precedent.
- `todo/TODO-86-petstore-jetty-app.md` — Jetty sibling; source of truth for the `juneau-petstore-core` module + the shared fixture / feature matrix.
- `todo/TODO-85-microservice-jetty-starter.md` — Jetty-only starter (NOT consumed by TODO-87).
- `todo/FINISHED-74-mixin-api-docs.md` — three-way deployment parity test template (`BasicApiDocsTestFixtures` precedent).
- `todo/FINISHED-77-mixin-ops-introspection.md` — `DenyAllGuard` placeholder + TODO-69 migration path.
