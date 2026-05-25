# TODO-86: `juneau-petstore-jetty` — canonical Jetty sample application

Source: split out of the TODO.md backlog on 2026-05-23; plan written 2026-05-24.

## Goal

Build a self-contained reference application — the **Juneau Petstore**, deployed on Jetty — that exercises a wide cross-section of Juneau features so prospective users have one canonical place to read "how a real Juneau service is structured." Pet-store domain chosen for direct parity with the Swagger / OpenAPI canonical example so users can compare apples-to-apples against the spec they probably already know.

End-state developer experience:

```bash
cd juneau-petstore/juneau-petstore-jetty
mvn package
java -jar target/juneau-petstore-jetty-9.5.0.jar
# Petstore live on http://localhost:10000
#   /api, /swagger, /openapi, /openapi.json, /openapi.yaml, /redoc   (API docs)
#   /pets, /orders, /customers                                       (CRUD REST)
#   /static/*                                                        (static files)
#   /favicon.ico, /version, /robots.txt                              (conventions)
#   /routes                                                          (ops introspection)
#   /views/about                                                     (Thymeleaf-rendered HTML page)
#   /admin/*                                                         (AuthN-guarded admin)
```

A reader who opens `juneau-petstore-jetty` should be able to point at any feature in the post-FINISHED-77 mixin family — plus runtime-overridable paths (FINISHED-73), AuthN guards (TODO-69), and a non-trivial bean model exercised across the marshall layer — and see a working in-context example.

## Why now

- After FINISHED-74/75/76/77 + FINISHED-73 + FINISHED-81 + (in-flight) TODO-69, Juneau has the broadest "you can compose a real service in five mixins" surface area it's ever had — but no single sample app exercises the whole surface. `juneau-examples-rest-jetty` predates the mixin family and reads as a feature collection rather than as a cohesive application.
- The Swagger Petstore is the de-facto OpenAPI reference application; landing a Juneau version means prospective users can A/B compare against a domain they already understand.
- TODO-85's starter module needs a non-trivial consumer to dogfood — TODO-86 is that consumer.
- TODO-87's Spring Boot mirror needs a shared bean/domain layer to avoid duplication — TODO-86 establishes the `juneau-petstore-core` module that both sample apps depend on.

## Scope

**In scope (v1):**

### 1. New Maven module structure

- `juneau-petstore/` parent POM (`<packaging>pom</packaging>`, artifact `juneau-petstore-parent`, parent: `juneau`). Aggregates three child modules:
    - `juneau-petstore/juneau-petstore-core/` — pure-Java domain model. **No REST, no Jetty, no Spring deps.** Contains:
        - Beans: `Pet`, `Order`, `Customer`, `Category`, `Tag`, enums (`PetStatus = AVAILABLE|PENDING|SOLD`, `OrderStatus = PLACED|APPROVED|DELIVERED`).
        - Repository SPI: `PetRepository`, `OrderRepository`, `CustomerRepository` interfaces.
        - In-memory impls: `InMemoryPetRepository`, `InMemoryOrderRepository`, `InMemoryCustomerRepository` — `ConcurrentHashMap<Long, T>`-backed.
        - Fixture data: `PetstoreFixtures.seed(repo)` populates the in-memory repos with ~20 pets, ~5 customers, ~10 orders spread across the three statuses.
    - `juneau-petstore/juneau-petstore-jetty/` — **this TODO's deliverable.** Wires `juneau-petstore-core` onto JettyMicroservice via TODO-85's starter (with fallback — see below).
    - `juneau-petstore/juneau-petstore-springboot/` — TODO-87's deliverable. Stub `<module>` entry only in v1 of this TODO; TODO-87 fills in the actual code.
- Add `<module>juneau-petstore</module>` to the root `juneau/pom.xml` aggregator.
- Add `juneau-petstore-core` and `juneau-petstore-jetty` artifact entries to `juneau-distrib/pom.xml`'s BOM.

### 2. Dependency on TODO-85 — soft preference, hard fallback

`juneau-petstore-jetty/pom.xml` **prefers** `juneau-microservice-jetty-starter` (TODO-85) once it lands:

```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-microservice-jetty-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

If TODO-86 lands BEFORE TODO-85, the petstore-jetty POM uses `juneau-microservice-jetty` directly and its `App.java` writes the longer `Microservice.create()...configurations(JettyConfiguration.class, AppConfig.class).build().start().join()` boilerplate (modeled after `juneau-examples-rest-jetty/src/main/java/org/apache/juneau/examples/rest/jetty/App.java`). Whichever of TODO-85 / TODO-86 lands second updates the other to switch to the starter dep. The acceptance criterion below allows either path.

### 3. Feature cross-section to exercise

| Feature | TODO/FINISHED | Demonstrated by |
|---|---|---|
| CRUD REST endpoints | core | `PetResource`, `OrderResource`, `CustomerResource` under `org.apache.juneau.petstore.jetty.rest.*` |
| API-docs mixin pack | FINISHED-74 | `PetstoreRootResource` declares `@Rest(mixins={BasicSwaggerUiResource.class, BasicRedocResource.class})` — six API-docs URLs light up at `/api`, `/swagger`, `/openapi`, `/openapi.json`, `/openapi.yaml`, `/redoc`. |
| Static files | FINISHED-75 | `PetstoreRootResource` adds `BasicStaticFilesResource.class` to its mixin set; ships sample assets (`/static/petstore.css`, `/static/logo.png`, `/static/help.html`) under `src/main/resources/static/`. |
| Convention endpoints | FINISHED-76 | Pulls `BasicFaviconResource` + `BasicVersionResource` + `BasicSeoResource` (custom robots policy: `Allow: /pets, /orders` + deny everywhere else). |
| Ops/introspection | FINISHED-77 | `BasicRouteIndexResource` mounted at `/routes` to dump the full endpoint inventory. `BasicEchoResource` mounted Debug-gated at `/debug/echo` (not enabled in default `application.cfg`; enabled in `application-dev.cfg` for local development). |
| View-rendered HTML page | TODO-78 OR TODO-82 | Thymeleaf via TODO-82 — see resolved decision #3. `/views/about` renders a Thymeleaf template summarizing the petstore. |
| Parent-chain mixin aggregation | FINISHED-74 | `AdminPetResource extends PetResource` adds `BasicAdminResource.class` to its inherited mixin set — the `LinkedHashSet` dedupe + parent-first ordering is exercised end-to-end. |
| Runtime-overridable `@Rest(paths=...)` | FINISHED-73 | `PetResource` declares `@Rest(paths={"$C{pet.paths}"})`; `application.cfg` ships `pet.paths=/pets,/api/pets` so the resource mounts at both URLs. `application-tenant-A.cfg` overrides to `/tenant-a/pets` to demonstrate the runtime swap. |
| AuthN guards | TODO-69 | Once landed: `AdminPetResource` declares `@Bean RestGuardList guards()` returning a `BearerTokenGuard` chain. Replaces the FINISHED-77 `DenyAllGuard` default. **Fallback if TODO-69 hasn't landed:** keep the `DenyAllGuard` placeholder and `// TODO: replace with TODO-69's BearerTokenGuard once landed.` |
| Non-trivial bean model + marshall round-trips | core | `Pet` has nested `Category` + `List<Tag>` fields. Tests assert clean JSON / HTML / XML / URL-encoded / plain-text round-trips. YAML round-trip if it survives TODO-88's buffer-underflow fix. |
| `juneau-petstore-core` shared with TODO-87 | TODO-87 | The core module's interfaces / beans / fixtures are 1:1 reusable from the Spring Boot variant. |

### 4. Petstore API surface

Modeled directly on the Swagger Petstore 3.x spec (general knowledge). All endpoints return JSON by default; HTML when `Accept: text/html`; XML / URL-encoded / plain-text on request.

| Method | Path | Operation |
|---|---|---|
| `GET` | `/pets` | List all pets. |
| `GET` | `/pets/{id}` | Get one pet by id. |
| `POST` | `/pets` | Add a new pet. Returns 201 + `Location: /pets/{newId}`. |
| `PUT` | `/pets/{id}` | Update a pet. |
| `DELETE` | `/pets/{id}` | Delete a pet. Returns 204. |
| `GET` | `/pets/findByStatus?status={available\|pending\|sold}` | Filter by status. |
| `GET` | `/pets/findByTag?tag={tag}` | Filter by tag. |
| `GET` | `/orders` | List all orders. |
| `GET` | `/orders/{id}` | Get one order. |
| `POST` | `/orders` | Place a new order. |
| `DELETE` | `/orders/{id}` | Cancel an order. |
| `GET` | `/customers` | List all customers. |
| `GET` | `/customers/{id}` | Get one customer. |
| `POST` | `/customers` | Create a new customer. |
| `PUT` | `/customers/{id}` | Update customer. |
| `DELETE` | `/customers/{id}` | Delete customer. |
| `POST` | `/admin/cache/flush` | Flush in-memory repository caches (guarded by `BearerTokenGuard` once TODO-69 lands). |

### 5. View-rendered HTML page

Thymeleaf via TODO-82 (rationale: resolved decision #3). The sample renders `/views/about` from `src/main/resources/templates/about.html` using a model bean populated by `AboutController.about()`. Demonstrates:

- Importing the Thymeleaf bridge module.
- Registering `BasicThymeleafResource` as a mixin on `PetstoreRootResource`.
- Returning a `ThymeleafView.of("about.html").attr("title", "Juneau Petstore")` from a `@RestGet` method.

**If TODO-82 hasn't landed when TODO-86 starts implementation,** the v1 fallback uses TODO-78's JSP path with the same `/views/about` URL; TODO-87 inherits whichever choice TODO-86 ships with (since the engine choice flows through `juneau-petstore-core`'s `@Bean` factories the same way).

**Explicitly out of scope (v1):**

- Database persistence — pure in-memory `ConcurrentHashMap<Long, T>` only. Defer JDBC / JPA / Spring Data to a v2 sample variant.
- Docker image / Helm chart / Kubernetes manifest — defer; v1 is plain `java -jar` only.
- User-facing UI beyond the single Thymeleaf `/views/about` page + the auto-generated `HtmlSerializer` views of the JSON/XML APIs — no React / Vue / SPA front-end.
- Real OIDC integration — `BearerTokenGuard` uses a static-JWT-validator stub from TODO-69's test harness until a real IdP is wired (out of scope for v1; v2 wires Keycloak or similar).
- Multi-tenant Postgres setup — the tenant-A example above is purely a Config demonstration; no actual tenancy isolation.
- Rate-limiting — TODO-66's `RateLimitGuard` is referenced in `BasicAdminResource`'s `/admin/ratelimit` endpoint but not exercised end-to-end in v1.

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Confirm `juneau-petstore/` is the right home (vs. `juneau-examples/juneau-petstore/`) — see open question #1 below.
2. Confirm the root `juneau/pom.xml` aggregator's `<modules>` list order — petstore parent likely sits AFTER `juneau-examples` so existing example builds aren't reshuffled.
3. Confirm TODO-85 status: if landed, `juneau-petstore-jetty` uses the starter dep; if not, falls back to direct `juneau-microservice-jetty`.
4. Confirm TODO-82 status: if landed, Thymeleaf; if not, JSP fallback per TODO-78.
5. Confirm TODO-69 status: if landed, `BearerTokenGuard`; if not, `DenyAllGuard` placeholder with explicit TODO comment.

### Phase 1 — `juneau-petstore-core` skeleton

1. Create `juneau-petstore/pom.xml` parent POM.
2. Create `juneau-petstore/juneau-petstore-core/` module:
    - Beans: `Pet`, `Order`, `Customer`, `Category`, `Tag` + enums.
    - Repository interfaces: `PetRepository`, `OrderRepository`, `CustomerRepository`.
    - In-memory impls.
    - Fixtures.
3. Coverage target: ≥ 95% line/branch on `juneau-petstore-core` (it's pure-Java, no REST plumbing, so trivial to cover).
4. Tests for the core module live in `juneau-utest/src/test/java/org/apache/juneau/petstore/core/` per the codebase convention.

### Phase 2 — `juneau-petstore-jetty` REST resources

1. `PetstoreRootResource` (router page, `@Rest(children={PetResource.class, OrderResource.class, CustomerResource.class, AboutResource.class, AdminPetResource.class})`) extends either `BasicJettyStarterServlet` (TODO-85 starter path) or `BasicRestServlet` (fallback path).
2. `PetResource` — full CRUD per the API surface table, runtime `@Rest(paths={"$C{pet.paths}"})` override.
3. `OrderResource` — full CRUD.
4. `CustomerResource` — full CRUD.
5. `AdminPetResource extends PetResource` — adds `BasicAdminResource.class` mixin + guard chain.
6. `AboutResource` — Thymeleaf `/views/about` handler (or JSP fallback per TODO-78).
7. `App.java` main entry point: four-line `JuneauJettyStarter.start(args, PetstoreRootResource.class);` if TODO-85 landed, otherwise eight-line `Microservice.create()...` fallback.

### Phase 3 — static files + convention endpoints + ops/introspection

1. Add `BasicStaticFilesResource.class` to `PetstoreRootResource`'s mixin set.
2. Ship sample static assets under `src/main/resources/static/` (e.g. `petstore.css`, `logo.png`, `help.html`).
3. Add `BasicFaviconResource`, `BasicVersionResource`, `BasicSeoResource`, `BasicRouteIndexResource` to the mixin set.
4. Configure `BasicSeoResource` builder with the petstore-specific robots policy.

### Phase 4 — Thymeleaf view (or JSP fallback)

1. Add the bridge module dep (TODO-82's `juneau-rest-server-view-thymeleaf` or TODO-78's `juneau-rest-server-view-jsp`).
2. Add the concrete view-engine impl (`thymeleaf` + `thymeleaf-spring6` for Thymeleaf; `jetty-ee10-apache-jsp` for JSP).
3. `src/main/resources/templates/about.html` (Thymeleaf) or `src/main/resources/META-INF/resources/WEB-INF/views/about.jsp` (JSP).
4. `AboutResource.about()` returns the appropriate `View` impl.

### Phase 5 — AuthN guards (TODO-69 once landed, `DenyAllGuard` fallback)

1. Add `BearerTokenGuard` (TODO-69) to `AdminPetResource`'s `@Bean RestGuardList guards()` once available.
2. Fallback: `BasicAdminResource`'s default `DenyAllGuard` stays in place with an inline `// TODO: replace once TODO-69 lands` comment.
3. Update topic page to call out the migration path.

### Phase 6 — integration test in `juneau-utest`

Model after FINISHED-74's `BasicApiDocs_JettyMicroservice_Test`. Single smoke test class in `juneau-utest/src/test/java/org/apache/juneau/petstore/jetty/`:

- `PetstoreJetty_Smoke_Test` — `@BeforeAll` boots the petstore app on an ephemeral port via `JuneauJettyStarter.start(...)` (or `Microservice.create()...` fallback). `@AfterAll` tears it down. Test methods walk the full feature matrix:
    - `a01_apiDocs_sixUrls` — assert `/api`, `/swagger`, `/openapi`, `/openapi.json`, `/openapi.yaml`, `/redoc` all 200 with the expected `Content-Type`.
    - `a02_pets_crud` — POST a pet, GET back, PUT, DELETE; assert state transitions.
    - `a03_pets_findByStatus` / `a04_pets_findByTag` — filter endpoints.
    - `a05_orders_crud` / `a06_customers_crud` — same shape.
    - `b01_staticFiles` — `GET /static/petstore.css` returns the css with `Cache-Control`.
    - `b02_favicon` / `b03_version` / `b04_robots` — convention endpoints.
    - `c01_routes` — `BasicRouteIndexResource` lists all endpoints.
    - `c02_debugEcho_off` — debug disabled by default; `/debug/echo` 404s.
    - `d01_thymeleafAbout` (or `d01_jspAbout`) — `/views/about` renders the HTML page.
    - `e01_parentChain_adminInheritsPetsMixins` — `AdminPetResource` sees `PetResource`'s inherited mixins + its own `BasicAdminResource` addition.
    - `e02_runtimePathOverride` — `PetResource` mounts at both `/pets` and `/api/pets` per the config-driven `$C{pet.paths}` resolution.
    - `f01_admin_denyByDefault` — `/admin/cache/flush` 403s without the guard override (when TODO-69 hasn't landed) or 401s without a Bearer token (once TODO-69 has).
    - `g01_pet_jsonHtmlXmlRoundTrip` — `GET /pets/1` with `Accept: application/json|text/html|application/xml|application/x-www-form-urlencoded|text/plain` returns the expected representation.

Coverage criteria for sample apps are relaxed — **the app IS the documentation**; coverage targets the test-running code, not the sample. Per FINISHED-74's precedent (sample apps don't have a hard coverage gate), TODO-86's `juneau-petstore-jetty` module doesn't need ≥ 95% line/branch — the smoke test asserting end-to-end behavior is enough. `juneau-petstore-core` DOES need ≥ 95% (it's a real library).

### Phase 7 — docs + release notes

1. New topic page `docs/pages/topics/PetstoreSample.md` walking through:
    - What the sample demonstrates (the table from Scope §3).
    - How to run it (`mvn package && java -jar ...`).
    - Where to look for each feature in the source tree.
    - Screenshots / curl examples for the six API-docs URLs, a couple of CRUD round-trips, and the Thymeleaf `/views/about` page.
2. Release-notes entry under a new `### juneau-petstore (new)` section in `docs/pages/release-notes/9.5.0.md`.
3. Update the docs sidebar to register the new topic page.
4. Cross-reference under `### juneau-microservice-jetty-starter` (if TODO-85 landed) pointing at TODO-86 as the dogfood consumer.

## Acceptance criteria

- [ ] New parent module `juneau-petstore` + child modules `juneau-petstore-core` and `juneau-petstore-jetty` build and tests pass standalone.
- [ ] `juneau-petstore-springboot` module is registered as a `<module>` entry in `juneau-petstore/pom.xml` but its body is owned by TODO-87 — TODO-86's acceptance does not gate on it.
- [ ] Petstore-jetty app starts via `java -jar` (or `JuneauJettyStarter.start(...)`), serves the full API surface from §4, exposes all six API-docs URLs, serves static files, convention endpoints, ops/introspection routes, and the Thymeleaf (or JSP) `/views/about` page.
- [ ] Parent-chain mixin aggregation: `AdminPetResource` inherits `PetResource`'s mixin set + adds `BasicAdminResource`; the inherited+appended union resolves with `LinkedHashSet` dedupe per FINISHED-74.
- [ ] Runtime path override: `PetResource` mounts at the comma-separated paths defined by `$C{pet.paths}`; smoke test asserts both mount points serve.
- [ ] AuthN guards: `AdminPetResource`'s admin paths return 403 (FINISHED-77 default) or 401 (once TODO-69's `BearerTokenGuard` is wired); documented in the topic page.
- [ ] Bean serialization round-trips: `Pet` (with nested `Category` + `List<Tag>`) round-trips JSON / HTML / XML / URL-encoded / plain-text without manual swap registration (relies on Juneau's bean-handling defaults).
- [ ] `juneau-petstore-core` coverage ≥ 95% line/branch.
- [ ] `juneau-petstore-jetty` smoke test class passes; no hard coverage gate on the sample app body itself.
- [ ] New topic page `PetstoreSample.md` landed; release-notes entry under `### juneau-petstore (new)`.
- [ ] No new RAT-header violations; full `./scripts/test.py` green.

## Test architecture

**Decision: tests live in `juneau-utest`, per FINISHED-74's three-way deployment-parity template.** The `juneau-petstore-jetty` module's `src/test/` stays empty (codebase convention). All petstore-jetty smoke tests live in `juneau-utest/src/test/java/org/apache/juneau/petstore/jetty/`; `juneau-petstore-core` unit tests live in `juneau-utest/src/test/java/org/apache/juneau/petstore/core/`. `juneau-utest/pom.xml` picks up `juneau-petstore-jetty` (which transitively brings `juneau-petstore-core`) as test-scope deps.

**Single smoke test class strategy** (rather than per-feature test classes) — the petstore is itself an integration concern, and a single class with ~15 test methods walking the full feature matrix is more readable than 15 single-method classes. The smoke test mirrors the shape of `BasicApiDocs_JettyMicroservice_Test`'s real-HTTP roundtrip approach.

**Three-way deployment parity assertion is the JOINT responsibility of TODO-86 + TODO-87** — TODO-87's integration test in `juneau-utest` re-runs the same fixture assertions against the Spring Boot deployment of the same `juneau-petstore-core`, mirroring FINISHED-74's `BasicApiDocs_JettyMicroservice_Test` + `BasicApiDocs_Springboot_Test` pairing.

## Resolved decisions

1. **Pet-store domain.** Direct parity with the Swagger / OpenAPI canonical example — readers can A/B compare against a spec they already know. Considered: a TodoMVC / blog-post-CRUD domain; rejected because Swagger Petstore has decades of muscle-memory in the OpenAPI ecosystem.
2. **Maven module split — `juneau-petstore-core` + `juneau-petstore-jetty` + `juneau-petstore-springboot`.** Core module shared 1:1 between the two deployment variants. Considered: a single combined module with both Jetty and Spring Boot entry points; rejected because it forces consumers to pull both Jetty AND Spring Boot transitive deps even when they only want one, and because the cleanest "shared bean model" pattern is a separate module.
3. **View engine — Thymeleaf (TODO-82) preferred, JSP (TODO-78) fallback.** Rationale: Thymeleaf is Spring Boot's default web view technology, so TODO-87's Spring Boot variant gets Thymeleaf "for free" via Spring Boot's auto-config — picking the same engine for the Jetty variant gives a cleaner cross-engine showcase and avoids two different template languages across the petstore family. JSP fallback exists for the case where TODO-82 hasn't landed when TODO-86 starts; the swap is local to one resource class (`AboutResource`).
4. **Mixins to exercise — the full FINISHED-74/75/76/77 surface, plus parent-chain aggregation, runtime paths, AuthN guards.** Sample apps should be intentionally feature-rich (this is the documentation surface); a "minimal" pet store doesn't serve the "see how a real Juneau service is structured" goal.
5. **In-memory persistence.** `ConcurrentHashMap<Long, T>` is enough to demonstrate every feature in scope. DB-backed variants (JDBC / JPA / Spring Data) are a v2 concern with a separate `juneau-petstore-jpa` module.
6. **POM packaging — `jar` (NOT `bundle`).** Petstore is an application, not a library; OSGi bundle metadata buys nothing here.
7. **Test home — `juneau-utest`.** Codebase convention (see "Test architecture").

## Open questions

1. **`juneau-petstore/` vs `juneau-examples/juneau-petstore/`.** Plan currently lands the parent module as a sibling of `juneau-examples/` under the root reactor (`juneau-petstore/`). Rationale: clearer domain ownership ("this is THE Juneau petstore", not "an example among many") + matches Swagger's own `swagger-petstore` repo naming + lets the sample have its own release cadence if it ever needs one. **Alternative: under `juneau-examples/juneau-petstore/`** — keeps all sample apps under one umbrella, consistent with `juneau-examples-rest-jetty` / `juneau-examples-rest-springboot`'s placement. User may prefer the latter for filesystem locality; both work technically. **Recommend `juneau-petstore/` (sibling of `juneau-examples/`)** for the domain-ownership reason, but defer to user preference on this one.
2. **Docker image / containerization.** Out of scope for v1; v2 candidate. Question: should the Phase 7 docs page include a Dockerfile snippet as a "going further" pointer, or is that a separate TODO? **Recommend: no Dockerfile in v1**, with a short "running in a container" subsection in `PetstoreSample.md` linking out to standard Java-jar containerization patterns.
3. **Persistence layer.** Pure in-memory in v1 per resolved decision #5. Question: should the `PetRepository` interface be shaped to make a future JDBC / JPA / Spring Data drop-in clean, or kept maximally simple? **Recommend: simple now, refactor later.** The interfaces aren't part of the public API; downstream variants can refactor freely without breakage.

## Risks

- **Sample app drift.** Sample apps tend to atrophy as the codebase evolves — features added after the sample lands don't get back-ported into the sample. Mitigation: the smoke test class is the regression net; new mixins / features ship with a "if this is a flagship feature, add it to the petstore" checklist in the topic page.
- **Hard dep on `juneau-petstore-core` from BOTH TODO-86 and TODO-87.** A breaking change to `juneau-petstore-core` blocks both sample apps simultaneously. Mitigation: treat `juneau-petstore-core` as a library with the same back-compat discipline as `juneau-marshall` — don't refactor lightly.
- **TODO-82 (Thymeleaf) hasn't landed when TODO-86 starts.** JSP fallback per resolved decision #3 absorbs the risk; the swap is local to `AboutResource`.
- **TODO-69 (AuthN guards) hasn't landed when TODO-86 starts.** `DenyAllGuard` placeholder absorbs the risk per FINISHED-77's design; the upgrade path is documented and is a one-bean change.
- **Bean-serialization edge cases.** A nested `Pet` → `Category` + `List<Tag>` may surface JSON / XML / YAML edge cases the marshall layer doesn't currently cover. Mitigation: the smoke test's `g01_pet_jsonHtmlXmlRoundTrip` is the regression net; any new finding becomes a follow-on TODO against the marshall module rather than blocking petstore landing.
- **Existing `juneau-examples-rest-jetty` overlap.** The petstore overlaps in spirit with `juneau-examples-rest-jetty` — readers may not know which to look at first. Mitigation: `PetstoreSample.md` opens with a "Petstore vs juneau-examples" recommendation matrix; `juneau-examples-rest-jetty`'s README gets a "for a more cohesive sample, see Petstore" cross-link.
- **Build-time inflation.** Adding three more modules adds build-time + CI surface. Mitigation: in-memory + no DB keeps Phase 6's smoke test fast (~5s); modules build in parallel with the rest of the reactor.

## Related work

**Dependency relationships:**

- **Hard dependencies** (TODO-86 cannot land without these):
    - FINISHED-74 (API-docs mixin pack) — used by `PetstoreRootResource`.
    - FINISHED-75 (static files mixin) — used by `PetstoreRootResource`.
    - FINISHED-76 (convention endpoints) — used by `PetstoreRootResource`.
    - FINISHED-77 (ops/introspection) — `BasicRouteIndexResource`, `BasicAdminResource`, `BasicEchoResource` used.
    - FINISHED-73 (runtime-overridable `@Rest(paths=...)`) — used by `PetResource`.
- **Soft dependencies** (TODO-86 ships either way, with fallback):
    - TODO-85 (`juneau-microservice-jetty-starter`) — preferred dep; raw `juneau-microservice-jetty` fallback. Whichever lands second updates the other.
    - TODO-69 (AuthN guards) — preferred; `DenyAllGuard` (FINISHED-77) fallback.
    - TODO-82 (Thymeleaf view) OR TODO-78 (JSP view) — exactly one needed; Thymeleaf preferred per resolved decision #3. JSP fallback if TODO-82 hasn't landed.
- **Downstream consumers:**
    - TODO-87 — hard dep on `juneau-petstore-core` (this TODO defines it).

**Recommended landing order in the petstore family:** TODO-85 first (standalone, no deps), then `juneau-petstore-core` + TODO-86 + TODO-87 as a single coherent landing (since they all need the core module to exist; splitting them across separate landings creates a "core exists but no consumers exist yet" awkward intermediate state).

**File references:**

- `juneau-examples/juneau-examples-rest-jetty/` — structural reference for module layout, POM shape, `App.java` shape.
- `juneau-examples/juneau-examples-rest-springboot/` — structural reference for TODO-87's mirror.
- `todo/TODO-85-microservice-jetty-starter.md` — soft-dep starter module.
- `todo/TODO-87-petstore-springboot-app.md` — sibling Spring Boot mirror.
- `todo/FINISHED-74-mixin-api-docs.md` — bundled mixin classes + three-way deployment parity test template (`BasicApiDocs_JettyMicroservice_Test`, `BasicApiDocs_Springboot_Test`).
- `todo/FINISHED-75-mixin-static-files.md` — static-files mixin.
- `todo/FINISHED-76-mixin-convention-endpoints.md` — favicon / version / SEO mixins.
- `todo/FINISHED-77-mixin-ops-introspection.md` — admin / echo / route-index mixins + `DenyAllGuard` migration story.
- `todo/FINISHED-73-rest-paths-runtime-override.md` — runtime-overridable `@Rest(paths=...)`.
- `todo/TODO-78-mixin-jsp-module.md` — JSP fallback view engine.
- `todo/TODO-82` (bullet in TODO.md) — Thymeleaf view engine (preferred).
- `todo/TODO-69-authn-guards-jwt-apikey.md` — AuthN guards integration.
- Swagger Petstore reference spec: <https://petstore.swagger.io/> (general knowledge; no checked-in copy needed).
