# TODO-76: Convention-endpoints mixin pack (favicon, SEO, version, well-known)

Source: split out of the post-FINISHED-72 mixin-pack planning on 2026-05-23.

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
- **`BasicWellKnownResource`** — single `@RestGet("/{path:.*}")` with default `paths={"/.well-known/security.txt"}`. Builder accepts `securityTxt(String)` (RFC 9116). Reserves the seam for future `oidcConfiguration(...)`, `assetlinks(...)`, etc. — but only `security.txt` ships in v1.
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

## Open questions

1. **Default favicon source — Juneau project image vs blank 1x1 transparent?** **Recommend Juneau project image** — locked-in via classpath resource so users replace by overriding `getFaviconBytes()` or registering an alternate `BasicFaviconResource` bean. Reduces "why is my favicon broken" surface area.
2. **Sitemap auto-generation from `BasicGroupOperations` index — yes/no?** **Recommend defer to v2** — v1 ships static-config builder only. Auto-generation has subtle correctness traps (which paths to include, how to compute `lastmod`).
3. **`security.txt` default — empty 404 vs explicit `Disallow:` placeholder?** RFC 9116 has no "default" — the file's presence is itself meaningful. **Recommend require explicit configuration**: if no `securityTxt(...)` was set, the mixin's path returns 404 (cleanest semantic — "we don't have one"). Document loudly.
4. **`/version` body format.** JSON bean (recommended) vs plain-text key/value vs both via content negotiation. **Recommend JSON-only** — content negotiation can be added later; JSON is the de-facto deployment-introspection format. Sites that want plain-text override the handler.
5. **Multi-module manifest aggregation.** A Spring Boot fat-jar contains many `MANIFEST.MF` files (one per dependency); which one wins? **Recommend the importer's app manifest** (root jar's `META-INF/MANIFEST.MF`), matching what Spring Boot's `BuildProperties` autoconfiguration does. Document.
6. **`/info` and `/about` — synonyms or differentiated payloads?** **Recommend synonyms in v1** — same JSON payload at all three paths. Future iterations may differentiate (`/info` = condensed, `/about` = full); keep it simple now.
7. **Inheriting future `.well-known/openid-configuration` from TODO-69.** The seam should be path-variable-routed so TODO-69 can register a handler without touching `BasicWellKnownResource` source. **Recommend the well-known mixin expose a `register(String suffix, Supplier<Object> handler)` builder method** — TODO-69 calls into it from its own bean wiring. Document the contract.

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
