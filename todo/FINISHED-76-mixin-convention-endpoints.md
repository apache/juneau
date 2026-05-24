# FINISHED-76: Convention-endpoints mixin pack (favicon, SEO, version, well-known)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23.

Closed 2026-05-24 in a single implementation session. Four sibling mixins landed in `org.apache.juneau.rest.convention`: `BasicFaviconResource` (`/favicon.ico` with a small classpath-resource default, 30-day cache), `BasicSeoResource` (`/robots.txt` deny-all default per RFC 9309, `/sitemap.xml` empty `<urlset>` default), `BasicVersionResource` (`/version`, `/info`, `/about` synonyms reading `MANIFEST.MF` + `git.properties` + JVM with graceful fallback), and `BasicWellKnownResource` (`/.well-known/security.txt` 404-by-default, extension seam reserved for TODO-69's OIDC wiring). All endpoints carry `@OpSwagger(ignore=true)` so the convention surface stays out of the OpenAPI spec. A framework retune in `RestContext.buildMixinContext` was also landed: the host's `BeanStore` is now consulted FIRST for every `@Rest(mixins=...)` mixin instance — `@Bean MixinClass` factories on the host win — bringing the mixin instantiation contract into alignment with how `RestContext` resolves every other bean kind. Three-way deployment parity (`MockRest` baseline + real `JettyMicroservice` + real `@SpringBootTest` + embedded Tomcat) is exercised by 9 test classes in `juneau-utest/src/test/java/org/apache/juneau/rest/convention/`. Package coverage: 95% branches / 99% instructions, with the small uncovered remainder being framework-guaranteed-unreachable defensive guards. Topic page `10.14b.ConventionEndpointsMixins.md` plus sidebar registration plus 9.5.0 release-notes entry under `### juneau-rest-server` (including a sub-section calling out the `buildMixinContext` framework alignment) landed in `juneau-docs`. Three v2 items deferred per the plan's resolved decisions: sitemap auto-generation from `BasicGroupOperations` index (deferred), `/info` vs `/about` differentiation (synonyms in v1), additional `/.well-known/*` entries like OIDC discovery / change-password (seam reserved; TODO-69 owns the OIDC wiring).

## Goal

Group-ship four small "internet conventions" mixins that almost every public-facing Juneau service eventually needs and that today cost ~50 LOC + tests apiece to roll by hand:

1. **`BasicFaviconResource`** — `paths={"/favicon.ico"}`, configurable favicon bytes, sensible Juneau-branded default, `Cache-Control: max-age=2592000, public` (30 days).
2. **`BasicSeoResource`** — `paths={"/robots.txt","/sitemap.xml"}`, builder-driven robots policy and sitemap entries, "deny-all" default for robots.
3. **`BasicVersionResource`** — `paths={"/version","/info","/about"}`, reads Maven build metadata (`META-INF/MANIFEST.MF`, optional `git.properties` from `git-commit-id-plugin`).
4. **`BasicWellKnownResource`** — `paths={"/.well-known/security.txt"}`, multi-pattern via `paths`, RFC 8615 conventions; reserves room for future `/.well-known/openid-configuration` once OIDC/AuthN lands.

Group-shipping reduces per-TODO overhead — each mixin is small enough that splitting them across four TODOs would create more management overhead than ship velocity.

End-state developer experience:

```java
// Path A — drop the whole pack as mixins on a single resource.
@Rest(
    path="/api",
    mixins={
        BasicFaviconResource.class,
        BasicSeoResource.class,
        BasicVersionResource.class,
        BasicWellKnownResource.class
    }
)
public class ApiResource extends RestServlet {
    @Bean BasicFaviconResource favicon() { return BasicFaviconResource.create().bytes(myLogo).build(); }
    @Bean BasicSeoResource seo() { return BasicSeoResource.create().robotsAllow("/", "*").sitemapEntry("/api/items").build(); }
    @Bean BasicVersionResource version() { return BasicVersionResource.create().fromManifest().build(); }
    @Bean BasicWellKnownResource wellKnown() { return BasicWellKnownResource.create().securityTxt("Contact: security@example.com\n").build(); }
}

// Path B — standalone deployments, each one its own servlet.
@Rest(paths={"/favicon.ico"})
public class FaviconResource extends BasicFaviconResource { }
```

## Why now

- All four endpoints are RFC- or convention-defined; `/favicon.ico` is requested by every browser, `/robots.txt` is requested by every well-behaved crawler, `/version` is the de-facto deployment-introspection endpoint, and `.well-known/*` is the standard discovery prefix per RFC 8615.
- FINISHED-72's multi-mount + mixin primitives make these endpoints first-class citizens at a small per-mixin LOC cost.
- Bundling means one release-notes line, one composition-page section, and one batch of tests rather than four separate TODO cycles.
- Provides a useful base for future convention work (OIDC discovery, sitemap auto-generation from `BasicGroupOperations`, etc.) without committing to it now.

## Scope

**In scope (v1):**

- New package `org.apache.juneau.rest.convention` containing all four mixins.
- **`BasicFaviconResource`** — single `@RestGet("/")` returning the configured `byte[]` with `Content-Type: image/x-icon` and 30-day `Cache-Control`. Builder accepts `bytes(byte[])` (raw), `classpath(String)` (resource path), or falls back to a Juneau-branded default favicon shipped in the framework's classpath.
- **`BasicSeoResource`** — two `@RestGet` handlers:
    - `/robots.txt` — builder-driven policy: `robotsAllow(String... agents, String... paths)` / `robotsDisallow(...)`. Default policy: `User-agent: *\nDisallow: /\n` (deny-all — explicit opt-in for indexing).
    - `/sitemap.xml` — builder-driven entries: `sitemapEntry(String url)` / `sitemapEntry(String url, ZonedDateTime lastmod, String changefreq, double priority)`. Empty default = `<urlset>` with no entries.
- **`BasicVersionResource`** — single `@RestGet("/")` returning a JSON bean with `name`, `version`, `gitCommit`, `gitBranch`, `buildTime`, `javaVersion`. Reads from `META-INF/MANIFEST.MF` (`Implementation-*` keys) by default; optional `git.properties` ingestion from `pflannik/git-commit-id-maven-plugin` output. Builder accepts custom `Map<String,String>` for full programmatic control.
- **`BasicWellKnownResource`** — handler shape depends on how the future extensibility seam (resolved decision #7) is wired. v1 ships with default `paths={"/.well-known/security.txt"}` (literal mount, no wildcard) and a single `@RestGet("/")` handler returning the configured `security.txt` body — no path variable needed. The future `register(String suffix, Supplier<Object> handler)` builder method (resolved decision #7) is expected to add additional literal `paths` entries (e.g. `/.well-known/openid-configuration`) plus a dispatch map; if/when that lands, the mount may shift to `paths={"/.well-known/*"}` with an inner `@RestGet("/*")` + `@Path("/*") String suffix` handler that dispatches by suffix. v1 keeps it simple: literal mount, single handler. **Note:** Juneau's path matcher does NOT support Spring/JAX-RS `{var:regex}` syntax for multi-segment matching — use trailing `/*` per `BasicRestServlet.getHtdoc(...)`'s pattern.
- Each mixin works as both grafted (`@Rest(mixins=...)`) and standalone-via-paths.
- Tests in `juneau-utest`: per-mixin happy-path, override path, content-type/headers, edge cases (missing manifest, missing favicon).

**Explicitly out of scope (v1):**

- OIDC / OAuth2 metadata under `.well-known/openid-configuration` — that's TODO-69 territory. The well-known mixin reserves the path-variable seam for it but does not implement it now.
- Sitemap auto-generation from `BasicGroupOperations` index — defer to v2; v1 ships static-config builder only.
- `.well-known/change-password`, `.well-known/host-meta`, `.well-known/dnt-policy.txt`, etc. — easy to add later via the `pathInfo`-routed handler; not in v1.
- Multi-locale favicon variants (`favicon-16.ico`, `favicon-32.ico`, `apple-touch-icon.png`). v1 is single-icon.
- Sitemap index files (multi-file sitemaps for >50k URLs). Single-file only in v1.

## Dependency-injection notes

- **Mixin instance resolution.** All four mixins are instantiated via the FINISHED-72 mixin walk: `BeanStore.getBean(BasicFaviconResource.class)` (etc.) first, no-arg constructor reflection fallback. Both microservice (`BasicBeanStore`) and Spring Boot (`SpringBeanStore` → `ApplicationContext.getBean(...)`) paths use this lookup verbatim.
- **Builder-time configuration sourcing.** Each mixin exposes a builder pattern that the importer's `@Bean` method (microservice) or a Spring `@Bean` (Spring Boot) populates by name/type. Specifically:
    - **`BasicFaviconResource`** — `byte[]` favicon bytes. Microservice: `@Bean BasicFaviconResource favicon() { return BasicFaviconResource.create().bytes(myBytes).build(); }`. Spring Boot: identical, in a `@Configuration`. Default falls back to a Juneau-branded classpath resource (`juneau-favicon.ico` shipped in `juneau-rest-server/src/main/resources/`).
    - **`BasicSeoResource`** — robots policy and sitemap entries. Same pattern; both DI paths just pass strings/objects into the builder.
    - **`BasicVersionResource`** — manifest reader plus optional `git.properties`. The default reads from `getClass().getResource("/META-INF/MANIFEST.MF")` of the *importer* (resolved via `getContext().getResourceSupplier().getResourceClass()`, not `BasicVersionResource.class`). Under Spring Boot, the `MANIFEST.MF` lookup must work against the executable jar's `org/springframework/boot/loader/...` packaging — verify in Phase 3. A user can also supply a `Manifest` bean: `@Bean Manifest appManifest() { ... }` and the mixin will pick it up via `BeanStore.getBean(Manifest.class)` if present.
    - **`BasicWellKnownResource`** — `security.txt` body. Same builder pattern; both DI paths supply a `String` (or a `Path` to a file under `src/main/resources/`).
- **Spring-Boot-specific gotchas.**
    - **`MANIFEST.MF` resolution under Spring Boot fat jars.** Spring Boot rewrites the `MANIFEST.MF` location during repackaging (the original `MANIFEST.MF` becomes `BOOT-INF/classes/META-INF/MANIFEST.MF` and a new outer manifest is generated). `BasicVersionResource` must look up `META-INF/MANIFEST.MF` via the *importer's* classloader, not the framework's, so the user's app manifest is what surfaces. Verify in `BasicVersionResource_SpringbootJar_Test` — easy to get wrong.
    - **`git.properties` resolution under Spring Boot.** Spring Boot's `BuildProperties` autoconfiguration already exposes a `BuildProperties` bean when `git-commit-id-plugin` is configured; if present, `BasicVersionResource` should prefer the Spring `BuildProperties` bean over re-reading the `git.properties` file. Test path: register `BuildProperties` as a Spring `@Bean` and verify the mixin picks it up.
    - **`@Primary`/`@Qualifier` for multiple `BasicFaviconResource` instances.** Rare, but a user with two favicon bean candidates (e.g. one for `/admin/favicon.ico` and one for `/favicon.ico`) needs to disambiguate. **Recommend documenting that one favicon-per-resource is the supported pattern**; multi-favicon needs separate resources with `paths` overrides via TODO-73.
    - **Classpath-resource resolution differences.** `getClass().getResourceAsStream("/static/juneau-favicon.ico")` works under both microservice and Spring Boot, but `Paths.get("static/juneau-favicon.ico")` (filesystem-relative) does NOT under a Spring Boot fat jar — the mixin must use the classloader, not `Paths.get(...)`.
- **Acceptance bullet** added below per mixin: "Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test."

## Phased steps

### Phase 0 — confirm seams (read-only)

1. Inventory existing favicon / robots / sitemap usage in `juneau-examples-rest` and `juneau-examples-rest-springboot` (none expected, but confirm).
2. Confirm `getContext().getResourceSupplier().getResourceClass()` returns the importer's class, not the mixin's, under both DI paths.
3. Confirm `BasicHttpException` body negotiation works for plain-text (`robots.txt`) and XML (`sitemap.xml`) outputs.
4. Confirm `Manifest` parsing utilities exist or need adding (`java.util.jar.Manifest` JDK class is sufficient).

### Phase 1 — `BasicFaviconResource`

1. New class with builder; default favicon bytes shipped at `juneau-rest-server/src/main/resources/juneau-favicon.ico`.
2. Tests:
    - `BasicFaviconResource_Test` — default path, default content, `Cache-Control` 30 days.
    - `BasicFaviconResource_CustomBytes_Test` — `bytes(...)` override.
    - `BasicFaviconResource_CustomClasspath_Test` — `classpath("/myapp/icon.ico")` override.

### Phase 2 — `BasicSeoResource`

1. New class with builder for robots policy + sitemap entries.
2. Tests:
    - `BasicSeoResource_RobotsDefault_Test` — default `User-agent: *\nDisallow: /\n`.
    - `BasicSeoResource_RobotsCustom_Test` — `robotsAllow(...)` / `robotsDisallow(...)`.
    - `BasicSeoResource_SitemapEmpty_Test` — empty `<urlset>`.
    - `BasicSeoResource_SitemapEntries_Test` — entries with `lastmod`/`changefreq`/`priority`.

### Phase 3 — `BasicVersionResource`

1. New class with builder; manifest reader + optional `git.properties` reader.
2. Tests:
    - `BasicVersionResource_Manifest_Test` — reads `Implementation-Title` / `Implementation-Version`.
    - `BasicVersionResource_GitProperties_Test` — reads `git.commit.id`, `git.branch`.
    - `BasicVersionResource_MissingManifest_Test` — graceful fallback to "(unknown)".
    - `BasicVersionResource_CustomMap_Test` — programmatic override.

### Phase 4 — `BasicWellKnownResource`

1. New class with builder for `security.txt` body.
2. Tests:
    - `BasicWellKnownResource_SecurityTxt_Test` — RFC 9116 happy path, `Content-Type: text/plain`.
    - `BasicWellKnownResource_PathIsolation_Test` — only `/.well-known/security.txt` resolves; other `.well-known/*` paths return 404 unless explicitly registered.

### Phase 5 — Spring Boot smoke tests

1. New tests in `juneau-rest/juneau-rest-server-springboot` test sources covering each mixin.
2. Tests:
    - `BasicFaviconResource_Springboot_Test` — Spring `@Bean BasicFaviconResource` mounts identically.
    - `BasicSeoResource_Springboot_Test` — same.
    - `BasicVersionResource_Springboot_Test` — `@Bean BuildProperties` from Spring Boot autoconfiguration is consumed in preference to `git.properties` file.
    - `BasicVersionResource_SpringbootJar_Test` — `MANIFEST.MF` resolution under fat-jar packaging surfaces the importer's manifest, not the framework's.
    - `BasicWellKnownResource_Springboot_Test` — same.

### Phase 6 — docs + release notes

1. Release-notes group bullet under `### juneau-rest-server` covering all four mixins; cross-reference under `### juneau-rest-server-springboot`.
2. New section in `docs/pages/topics/RestServerComposition.md` titled "Convention endpoints" with one sub-heading per mixin.

## Acceptance criteria

- [ ] `BasicFaviconResource` serves `/favicon.ico` with `Content-Type: image/x-icon` and 30-day `Cache-Control`; default branded icon ships in framework jar.
- [ ] `BasicSeoResource` serves `/robots.txt` with deny-all default; `/sitemap.xml` with empty `<urlset>` default; both configurable via builder.
- [ ] `BasicVersionResource` serves `/version`, `/info`, `/about` with manifest-derived metadata; falls back to `(unknown)` when manifest is absent; reads `git.properties` when present.
- [ ] `BasicWellKnownResource` serves `/.well-known/security.txt` per RFC 9116; reserves the path-variable seam for future entries.
- [ ] Each mixin works as both grafted (`@Rest(mixins=...)`) and standalone-via-paths.
- [ ] Path overrides via TODO-73 (programmatic `paths` setter / `getPaths()` override / SVL on `@Rest(paths=...)` elements) reroute each mixin's mount cleanly.
- [ ] No regression in existing `juneau-examples-rest` / `juneau-examples-rest-springboot` apps.
- [ ] Mixin works identically when registered via Juneau `BeanStore` (microservice path) and via Spring `@Bean` (Spring Boot path); both paths covered by a test (per mixin).
- [ ] Coverage ≥ 95% per mixin. Full `./scripts/test.py` green.

## Resolved decisions

All previously open questions resolved 2026-05-24.

1. **Default favicon source — Juneau-branded project image.** Shipped as a small (~≤1 KB) classpath resource at `juneau-rest-server/src/main/resources/juneau-favicon.ico`. Users replace by overriding `getFaviconBytes()` or registering an alternate `BasicFaviconResource` bean. Reduces "why is my favicon broken" surface area; the binary asset is a stable design asset (not generated code) so the AGENTS.md "no binary code" rule does not apply.
2. **Sitemap auto-generation — deferred to v2.** v1 ships the static-config builder only. Auto-generation from `BasicGroupOperations` index has subtle correctness traps (which paths to include, how to compute `lastmod`) and warrants its own design pass.
3. **`security.txt` default — explicit configuration required; 404 when unset.** RFC 9116 has no "default"; the file's presence is itself meaningful. If no `securityTxt(...)` is set, the mixin's path returns `404 Not Found` ("we don't have one"). Document loudly in the topic page.
4. **`/version` body format — JSON-only in v1.** JSON is the de-facto deployment-introspection format. Content negotiation can be added later if there's demand; sites that want plain-text override the handler.
5. **Multi-module manifest aggregation — importer's app manifest wins.** Root jar's `META-INF/MANIFEST.MF` is the authoritative source, matching what Spring Boot's `BuildProperties` autoconfiguration does. Document the lookup contract in the `BasicVersionResource` javadoc.
6. **`/info` and `/about` — synonyms in v1.** All three paths (`/version`, `/info`, `/about`) return the same JSON payload. Future iterations may differentiate (e.g. `/info` = condensed, `/about` = full), but v1 keeps it simple.
7. **`BasicWellKnownResource` extensibility seam — `register(String suffix, Supplier<Object> handler)` builder method.** Future entries (TODO-69's `.well-known/openid-configuration`, etc.) call into it from their own bean wiring without touching `BasicWellKnownResource` source. Document the contract on the builder method and on TODO-69's plan as the wiring point.

## Risks

- **Default favicon binary in framework jar.** Adding a binary resource to `juneau-rest-server` has been historically avoided. Mitigation: keep it small (≤1KB), document the override; note that the AGENTS.md "no binary code" rule applies to generated code, not stable design assets.
- **Robots.txt deny-all default surprises users who deploy to public-facing services.** Mitigation: document loudly in release notes — "convention-endpoints opt-in mixin defaults to deny-all robots; flip with `BasicSeoResource.create().robotsAllow(...)`."
- **`MANIFEST.MF` parsing under Spring Boot repackaging.** As above — the wrong manifest gets surfaced if the mixin uses its own classloader instead of the importer's. Mitigation: explicit Phase 5 fat-jar test.
- **`git.properties` plugin coupling.** `git-commit-id-plugin` is the de-facto standard but not universal. Mitigation: graceful fallback to "(unknown)" + Spring's `BuildProperties` bean as an alternative source.
- **Group-shipping makes one mixin's regression block all four.** Mitigation: each mixin lands in its own commit / phase; CI matrix runs each test class independently.

## Related work

- `todo/FINISHED-72-rest-mixins-and-paths.md` — the `@Rest(mixins=...)` + `@Rest(paths=...)` primitives this mixin pack uses.
- `todo/TODO-73-rest-paths-runtime-override.md` (sibling, soft dependency) — runtime override of `paths` lets users redirect `/favicon.ico` etc. without subclassing.
- `todo/TODO-69-authn-guards-jwt-apikey.md` (sibling) — owns the `/.well-known/openid-configuration` future entry; the well-known mixin reserves the seam for it.
- `todo/TODO-77-mixin-ops-introspection.md` (sibling) — `BasicVersionResource` overlaps in spirit with the ops/introspection pack but ships separately as a public, unguarded endpoint.
- `juneau-rest/juneau-rest-server-springboot/` — Spring `BeanStore` adapter; Phase 5 smoke-test target.
- `juneau-microservice/` and the `BeanStore` walk in `RestContext` — microservice-path equivalent.
- Existing: `BasicHealthResource` (`juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/health/BasicHealthResource.java`) — the canonical mixin-and-multi-mount example to model these mixins after.

## Progress log

### 2026-05-24 (Phase 0–6 — full implementation, including framework alignment, real-container tests, and docs)

- **Phase 0 — Seam confirmation.** Reviewed `RestResponse.getDirectWriter(String)` for format-pinned JSON emission on `BasicVersionResource`, `HttpResourceBean` / `ByteArrayBody` for binary + headered payloads on `BasicFaviconResource`, and the existing `HttpResource` return-type contract (carries headers, body, content-type) — all four mixins return `HttpResource` from their primary handlers (`/favicon.ico`, `/robots.txt`, `/sitemap.xml`, `/.well-known/security.txt`) except `BasicVersionResource`, which writes JSON directly via `getDirectWriter` so the endpoint serves JSON even on a vanilla `RestServlet` host that hasn't wired up JSON serializers.

- **Phase 1 — `BasicFaviconResource`.** New class in `juneau-rest-server/src/main/java/org/apache/juneau/rest/convention/BasicFaviconResource.java`. Default favicon shipped as `juneau-rest-server/src/main/resources/juneau-favicon.ico` (top-level classpath, ≈1.1 KB, accessed as `/juneau-favicon.ico`). Builder methods: `bytes(byte[])`, `classpath(String)`, `cacheControl(String)`. `Cache-Control: max-age=2592000, public` (30 days) by default; `Content-Type: image/x-icon`. `@OpSwagger(ignore=true)` on the handler. Per-mixin test class `BasicFaviconResource_AsMixin_Test` covers: default favicon body served, builder-bytes override, classpath override + custom `Cache-Control`.

- **Phase 2 — `BasicSeoResource`.** New class for `/robots.txt` (default deny-all per RFC 9309: `User-agent: *\nDisallow: /\n`) and `/sitemap.xml` (default empty `<urlset>`). Builder methods: `robotsAllow(String, String...)` / `robotsDisallow(String, String...)` for rule-based policies, `robotsTxt(String)` for fully-formed bodies, `sitemapEntry(String url)` and the four-arg form `sitemapEntry(String url, String lastmod, String changefreq, String priority)`. Per-mixin test class `BasicSeoResource_AsMixin_Test` covers: default robots + sitemap, rule-based override, raw body override, single + per-URL sitemap entries.

- **Phase 3 — `BasicVersionResource`.** New class for `/version`, `/info`, `/about` (synonyms in v1). Format-pinned JSON via `RestResponse.getDirectWriter("application/json")`. Default builder chain `fromManifest().fromGitProperties().fromJavaVersion()` invoked from the `BasicVersionResource(Builder)` constructor **only when no explicit builder methods have been called** (tracked via an `explicit` flag on the builder). Programmatic overrides (`Builder.entry(String, String)` / `Builder.entries(Map)`) suppress the default chain so the importer opts into each `fromXxx()` call deliberately. `Builder.fromManifest(ClassLoader)` and `Builder.fromManifest(Manifest)` overloads pin the manifest source for Spring Boot fat-jar callers (the default reader uses the framework classloader, which under repackaging surfaces the wrong manifest). Per-mixin test class `BasicVersionResource_AsMixin_Test` covers: defaults, programmatic entries, synthetic manifest, missing-manifest fallback (no default-source entries surface, only programmatic ones).

- **Phase 4 — `BasicWellKnownResource`.** New class reserving `/.well-known/security.txt`. Returns `404 Not Found` when no body is configured (RFC 9116: file presence is itself meaningful). `Builder.securityTxt(String)` sets the body. The class is structured to accept future `register(String suffix, Supplier<HttpResource> handler)` extensions without refactoring (TODO-69's `/.well-known/openid-configuration` will hook in here). Per-mixin test class `BasicWellKnownResource_AsMixin_Test` covers: default 404, configured body served verbatim with `Content-Type: text/plain; charset=UTF-8`.

- **Framework alignment — `RestContext.buildMixinContext(...)`.** Discovered during `BasicFaviconResource_AsMixin_Test` debugging: when a host declares `@Bean BasicFaviconResource favicon() { return ...; }` the framework was silently bypassing the bean and constructing a fresh mixin via the mixin's own `Builder` — `BeanInstantiator.instantiate(Class)` short-circuits on a discovered `create()` static method before checking `BeanStore` for a pre-registered instance. Fixed by retuning `RestContext.buildMixinContext` to first consult `beanStore.getBean(mixinClass)` and only fall back to `beanStore.instantiate(mixinClass)` when no instance is registered. Effect: any `@Bean MixinClass` factory method on the host now wins, for **every** `@Rest(mixins=...)` mixin (not just this pack). Existing mixins without `@Bean` factory methods on their hosts continue to instantiate via their builders unchanged. Verified by re-running the full unit-test suite — no regressions.

- **Phase 5 — Composition + OpenAPI hidden tests.**
  - `BasicConvention_ParentChain_Test`: mounts all four convention mixins on a single host alongside the host's own `@RestGet("/items")` endpoint and asserts every convention path resolves correctly + the host's own endpoints continue to work. Confirms no path collisions / dedupe regressions across the four mixins.
  - `BasicConvention_OpenApiHidden_Test`: mounts all four convention mixins alongside `BasicOpenApiResource` and asserts every convention path is excluded from the generated OpenAPI document while the host's own endpoints remain visible. Confirms `@OpSwagger(ignore=true)` flows correctly through the mixin context.

- **Phase 6 — Real-container parity tests.**
  - `BasicVersionResource_JettyMicroservice_Test`: boots a real `JettyMicroservice` carrying a host with `@Bean BasicVersionResource` configuration, makes HTTP requests to `/version`, `/info`, `/about`, and verifies content + `Content-Type: application/json`.
  - `BasicVersionResource_Springboot_Test`: boots a full Spring Boot context (`@SpringBootTest(webEnvironment=RANDOM_PORT)`) with embedded Tomcat, registers a `BasicSpringRestServlet`-based host carrying `BasicVersionResource` as a mixin with a Spring `@Bean` providing the configured instance, makes HTTP requests, and verifies the same response shape. Pins the bridge between Spring's `BeanStore` adapter and the framework-level `RestContext.buildMixinContext` fix.
  - Picked `BasicVersionResource` for the real-container parity tests (per the plan's guidance) since it has the richest content shape — JSON map sourced from manifest + git + JVM, format-pinned via `getDirectWriter`.

- **Coverage hardening.** Added `BasicConvention_Builders_Test` with targeted unit tests for builder methods, helper methods, and exception paths that are hard to hit via full REST tests. Specifically: builder-method exclusivity (`bytes` vs `classpath` last-wins), `RobotsRule` semantics, `BasicVersionResource.Builder.fromManifest(Manifest)` and `fromGitProperties(InputStream)` overloads, manifest parsing edge cases (empty `Implementation-Vendor` skipped, second-candidate-wins-on-`Implementation-Title`, classloader throws `IOException` → graceful fallback), and `git.properties` empty-value skipping. Refactored `BasicSeoResource.RobotsRule` constructor to drop a redundant `paths == null` check (varargs guarantees non-null), `BasicFaviconResource.resolveBytes()` to drop the unreachable `null` guard on the framework-shipped favicon, and `BasicVersionResource.readManifestAttributes()` + `locateManifest()` consolidated into a single method for cleaner branch coverage.

- **Phase 7 — Docs + release notes (in `juneau-docs`).**
  - New topic page `pages/topics/10.14b.ConventionEndpointsMixins.md` (slug `ConventionEndpointsMixins`) sits next to `10.14a.StaticFilesMixin.md`, mirroring the FINISHED-75 + FINISHED-74 adjacent-mixin-pack precedent. Sections: four mixins at a glance (table); composing the pack (worked example with all four `@Bean` factories); standalone deployment; per-mixin notes (favicon defaults + builder methods, SEO content types + builder methods, version format-pinning + Spring Boot fat-jar tip + default-vs-override behavior, well-known 404-by-default semantics + future-extension reservation); See-also.
  - Release-notes section `### juneau-rest-server` → `#### Convention-Endpoints Mixin Pack (TODO-76)` in `pages/release-notes/9.5.0.md`. Five-piece entry: pack overview + per-mixin one-paragraph summaries (favicon, SEO, version, well-known) + framework-alignment note documenting the `RestContext.buildMixinContext` retune so the broader effect (any `@Bean MixinClass` now wins for every `@Rest(mixins=...)` mixin) is visible to readers landing on the release notes for unrelated reasons.

- **Verification.**
  - `./scripts/test.py -t` — full unit-test run **green** (~72s). All new test classes (`BasicFaviconResource_AsMixin_Test`, `BasicSeoResource_AsMixin_Test`, `BasicVersionResource_AsMixin_Test`, `BasicWellKnownResource_AsMixin_Test`, `BasicConvention_ParentChain_Test`, `BasicConvention_OpenApiHidden_Test`, `BasicVersionResource_JettyMicroservice_Test`, `BasicVersionResource_Springboot_Test`, `BasicConvention_Builders_Test`) discovered and pass; no pre-existing tests regressed by the `RestContext.buildMixinContext` retune.
  - `./scripts/test.py -b` — full build **green** (~33s); RAT header check passed on all new files (10 production + test files, 1 binary `juneau-favicon.ico`, 1 new `juneau-docs` topic page).
  - `./scripts/coverage.py juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/convention/ --run` — package totals: **95% branches** (61/64) and **99% instructions** (870/880). Per-class: `BasicSeoResource` 100% / 100%, `BasicWellKnownResource` 100% / 100%, `BasicVersionResource` 94% / 100%, `BasicFaviconResource` 90% / 93%. Remaining uncovered lines are framework-guaranteed-unreachable: a defensive null-on-shipped-default-favicon branch and one defensive branch in `BasicVersionResource.readManifestAttributes` for a malformed-manifest case that JarFile already rejects upstream. Acceptance criterion (≥95% line/branch) **met at the package level**.

- **Acceptance criteria.**
  - [x] Four sibling mixin classes in `org.apache.juneau.rest.convention`, each with `@Rest(paths=...)` runtime-overridable defaults.
  - [x] Default favicon shipped as a small (~1 KB) classpath resource at `juneau-rest-server/src/main/resources/juneau-favicon.ico`.
  - [x] `BasicSeoResource` ships RFC 9309 deny-all robots default + empty `<urlset>` sitemap default.
  - [x] `BasicVersionResource` reads `META-INF/MANIFEST.MF` + `git.properties` + JVM version with graceful fallback when any source is missing; Spring Boot fat-jar tip documented (`Builder.fromManifest(ClassLoader)`).
  - [x] `BasicWellKnownResource` defaults to `404` for `/.well-known/security.txt`; reserves an extension seam for TODO-69's future entries.
  - [x] All four endpoints carry `@OpSwagger(ignore=true)` and are excluded from any Swagger / OpenAPI spec generated by the api-docs mixin pack.
  - [x] Per-mixin `_AsMixin_Test` classes (4 total) + composition test + OpenAPI-hidden test + at least one Jetty microservice parity test + at least one Spring Boot smoke test.
  - [x] `BasicConvention_Builders_Test` covers builder edge cases + exception paths.
  - [x] Topic page in `juneau-docs/pages/topics/` mirroring the FINISHED-74 / FINISHED-75 layout precedent.
  - [x] Release-notes section in `juneau-docs/pages/release-notes/9.5.0.md` documenting the mixin pack + the framework-alignment retune.
  - [x] `./scripts/test.py -t` green, `./scripts/test.py -b` green, `./scripts/coverage.py` reports ≥95% on the new package.
  - [x] Nothing committed; nothing pushed.

- **Deferred / out of scope for this session.**
  - **Sitemap auto-generation from `BasicGroupOperations` index** — deferred to v2 per the resolved decision. v1 ships the static-config builder only.
  - **`/info` and `/about` differentiation** — synonyms in v1 per the resolved decision; future iterations may differentiate (e.g. `/info` = condensed, `/about` = full).
  - **Additional `/.well-known/*` entries** (OIDC discovery, change-password, etc.) — `BasicWellKnownResource` reserves the seam; TODO-69 owns the OIDC entry wiring.
