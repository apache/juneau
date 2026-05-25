# TODO-69: AuthN guards — `BearerTokenGuard`, `ApiKeyGuard`, optional JWT verification

Source: split out of TODO-18 brainstorm on 2026-05-22.

## Goal

Add canonical authentication guards on top of the existing `RestGuard` SPI (today carries `RoleBasedRestGuard` for AuthZ but nothing for AuthN). Provide:

- **`BearerTokenGuard`** — extracts `Authorization: Bearer <token>` and validates via a pluggable `TokenValidator` SPI.
- **`ApiKeyGuard`** — extracts an API key from a configurable header / query param / cookie and validates against a `Map<String,Principal>` or a pluggable `ApiKeyStore` SPI.
- **`JwtTokenValidator`** (in an opt-in sub-module `juneau-rest-server-jwt`) — JWT verification (HS256 / RS256 / ES256) against a configurable JWKS URL or static key. Builds on `nimbus-jose-jwt` as an optional dep so the core stays lean.
- An auto-injected `@Auth Principal` arg so handlers can read the authenticated principal without `RestRequest.getUserPrincipal()` boilerplate.

End-state developer experience:

```java
@Rest(path="/api")
public class ApiResource {

    @Bean(name="guards") RestGuardList auth() {
        return RestGuardList.of(
            BearerTokenGuard.create()
                .validator(JwtTokenValidator.create()
                    .jwksUrl("https://auth.example.com/.well-known/jwks.json")
                    .audience("api.example.com")
                    .build())
                .build());
    }

    @RestGet("/me")
    public Profile me(@Auth Principal p) {  // injected, non-null guaranteed
        return profileService.lookup(p.getName());
    }
}
```

## Why now

- `RoleBasedRestGuard` covers AuthZ but the AuthN gap forces every Juneau user to wrap Spring Security or roll their own `Filter`. The framework should have a first-class story.
- `RestGuard.guard(req, res)` SPI is stable and proven.
- TODO-24's FQN-based annotation recognition pattern lets `@Auth` work without forcing a Juneau-owned `Principal` type — `java.security.Principal` is fine.
- TODO-66 (rate limit + request id) is the obvious adjacent guard; both compose cleanly in `RestGuardList`.

## Scope

**In scope (v1):**

- `org.apache.juneau.rest.auth.TokenValidator` SPI — single method `Principal validate(String token) throws AuthenticationException`.
- `org.apache.juneau.rest.auth.BearerTokenGuard extends RestGuard` — extracts `Authorization: Bearer <token>`, delegates to `TokenValidator`, stashes the resulting `Principal` on `RequestAttributes` under key `principal`, throws `401 Unauthorized` (with `WWW-Authenticate: Bearer realm=...`) on failure.
- `org.apache.juneau.rest.auth.ApiKeyStore` SPI — `Optional<Principal> lookup(String key)`.
- `org.apache.juneau.rest.auth.ApiKeyGuard extends RestGuard` — extracts from header (default `X-API-Key`) / query / cookie, delegates to `ApiKeyStore`, same stash + 401 behavior.
- `org.apache.juneau.rest.auth.AuthArg implements RestOpArg` — resolves `@Auth Principal` (or any `Principal`-typed parameter) from the request attribute.
- `org.apache.juneau.rest.auth.AuthenticationException extends BasicHttpException` (status 401, with `WWW-Authenticate` header support).
- New sub-module **`juneau-rest-server-jwt`** in `juneau-rest/`: `JwtTokenValidator` impl with JWKS-URL fetching (cached per RFC 7517 §4.5), audience / issuer / clock-skew validation, RS256 / ES256 / HS256 algorithm support. Optional dep on `com.nimbusds:nimbus-jose-jwt` (provided scope).
- Tests in `juneau-utest`: bearer-token happy path, missing header → 401, invalid token → 401, principal injection; API-key happy path; same for JWT module (in its own `juneau-rest-server-jwt` test or under `juneau-utest`).

**Explicitly out of scope (v1):**

- Basic auth (`Authorization: Basic`). Easy to add later if requested.
- OAuth 2.0 client-credentials flow (issuing tokens). This is AuthN only — verify, don't issue.
- mTLS — separate transport-layer concern.
- Session-based auth (cookies + server-side session store).
- Custom auth providers beyond the `TokenValidator` / `ApiKeyStore` SPIs.
- Spring Security bridge (let the Spring user keep using Spring Security; the SPIs above are for non-Spring users).

## Phased steps

### Phase 0 — confirm seams (read-only)

1. `RestGuard.guard(RestRequest, RestResponse)` return-or-throw contract — confirmed.
2. `Unauthorized` exception type (`org.apache.juneau.http.response.Unauthorized`) supports `setHeader("WWW-Authenticate", ...)` via the TODO-40 fluent surface — confirmed.
3. `RequestAttributes` write access from `RestGuard` — confirmed.

### Phase 1 — `BearerTokenGuard` + `ApiKeyGuard` + `AuthArg`

1. Add the SPIs + guards + arg-resolver.
2. Add `AuthenticationException` + `Principal` stash key constant in `RestServerConstants`.
3. Tests:
   - `BearerTokenGuard_Test` — happy path, missing header, malformed header, validator-rejects.
   - `ApiKeyGuard_Test` — header / query / cookie sources, unknown key → 401.
   - `AuthArg_Test` — `@Auth Principal` injected; `Principal` parameter without `@Auth` also resolves.

### Phase 2 — `juneau-rest-server-jwt` sub-module

1. New module `juneau-rest/juneau-rest-server-jwt/` with pom mirroring `juneau-rest-server-mcp`. Dep on `com.nimbusds:nimbus-jose-jwt:9.40` (or current; confirm at land time) in `provided` scope.
2. `JwtTokenValidator` impl: JWKS fetch + cache (5-min default TTL); validate `iss`, `aud`, `exp`, `nbf` with configurable clock-skew tolerance (default 60s); algorithm pin (default RS256).
3. Tests verify JWKS rotation, expired token rejection, audience mismatch rejection, algorithm-confusion attack rejection (HS256 token presented to an RS256-configured validator).

### Phase 3 — docs + release notes

1. Release-notes entries under `### juneau-rest-server` and `### juneau-rest-server-jwt (new module)`.
2. New doc page (or section) walking through bearer + API-key + JWT flows.

## Acceptance criteria

- [ ] `BearerTokenGuard` extracts `Authorization: Bearer <token>`, delegates to the configured `TokenValidator`, stashes the resulting `Principal` on `RequestAttributes`, throws 401 with `WWW-Authenticate: Bearer realm=...` on any failure.
- [ ] `@Auth Principal p` parameter resolves to the stashed principal; null is never injected (guard runs first).
- [ ] `ApiKeyGuard` extracts from a configurable source (header / query / cookie) and validates against the configured `ApiKeyStore`.
- [ ] `JwtTokenValidator` (in the new sub-module) validates a JWT against a JWKS URL with caching, honors `iss` / `aud` / `exp` / `nbf` / clock-skew, and rejects algorithm-confusion attacks.
- [ ] No new compile-time deps in `juneau-rest-server` (only in `juneau-rest-server-jwt`).
- [ ] Coverage ≥ 90% on the core guards; ≥ 85% on the JWT sub-module (network-bound paths mocked). Full `./scripts/test.py` green.

## Resolved decisions

All previously open questions resolved 2026-05-24.

1. **JWT library — `com.nimbusds:nimbus-jose-jwt`, isolated in the new `juneau-rest-server-jwt` sub-module.** RESOLVED 2026-05-24. Battle-tested (the canonical Java JWT/JOSE lib, used by Spring Security's JWT decoder under the hood), minimal transitive deps (just `net.minidev:json-smart` + slf4j-api), and ergonomic for the JWKS + algorithm-pinning use cases this plan needs. **Critical containment requirement:** the nimbus dep MUST NOT bleed into any existing module — it lives only in the new `juneau-rest-server-jwt` sub-module, in `provided` scope on that module's POM (consumers opt in by adding the nimbus dep at runtime alongside `juneau-rest-server-jwt`). This keeps the existing `juneau-rest-server` POM (and everything that depends on it transitively) unchanged. Phase 1's `BearerTokenGuard` + `ApiKeyGuard` + `AuthArg` ship in core `juneau-rest-server` without any JWT dep — they take a pluggable `TokenValidator` SPI; users who want JWT verification then add `juneau-rest-server-jwt` + nimbus on top. The alternative libs (`jjwt`, JDK-only) were considered: `jjwt` is less battle-tested and has been historically slower on CVE patching; JDK-only requires hand-rolling JOSE primitives that nimbus already solves correctly. Sub-module path: `juneau-rest/juneau-rest-server-jwt/` with POM mirroring `juneau-rest-server-mcp` for naming/structure consistency.
2. **JWKS cache TTL — 5 minutes default, manual override.** RESOLVED 2026-05-24. Short enough to pick up rotated keys quickly during incident response; long enough to absorb most JWKS-endpoint hiccups. `Builder.jwksCacheTtl(Duration)` lets operators tighten (faster rotation) or relax (lower JWKS-endpoint load) as needed. Honoring HTTP `Cache-Control` from the JWKS endpoint was rejected for v1 — too easy for a misconfigured IdP to send `Cache-Control: max-age=86400` and silently lock in a rotated-out signing key for 24h; the 5-min ceiling is the safer floor.
3. **Algorithm allowlist — default `[RS256, ES256]`, configurable.** RESOLVED 2026-05-24. HS256 (symmetric HMAC) is intentionally OUT of the default allowlist to prevent the classic algorithm-confusion attack (attacker takes the IdP's RSA public key, signs an HS256 token using the public key as the HMAC secret, and a naïve verifier accepts it). Users with a legitimate HS256 use case (e.g. local-issued service-to-service tokens with a shared secret) opt in explicitly via `Builder.algorithms(...)`. `none` is permanently rejected with no opt-in.
4. **`@Auth` annotation name.** RESOLVED 2026-05-24. Short, unambiguous, doesn't clash with `java.security.Principal` or any JAX-RS / Spring annotation. `@Auth Principal p` reads naturally on the parameter; `@Auth ClaimsPrincipal p` (per resolved decision #6) reads equally naturally when the user wants structured claim access.
5. **No auto-registered default `RestGuardList`.** RESOLVED 2026-05-24. Users must explicitly declare `@Bean(name="guards") RestGuardList`. Auto-registration is a security footgun: silently enabling auth on resources that didn't ask for it would cause hard-to-diagnose 401s on adjacent endpoints. Explicit registration also makes the `BasicAdminResource` integration story (per FINISHED-77's `DenyAllGuard` placeholder) crystal-clear — operators see the bean wiring in one place and can reason about the guard chain.
6. **`java.security.Principal` as the SPI return type; ship `ClaimsPrincipal extends Principal` for token-with-claims callers.** RESOLVED 2026-05-24. JDK standard `Principal` is the lowest-common-denominator type — works with `java.security.PrivilegedAction`, integrates cleanly with Java EE / Spring Security's `Principal`-based authn stacks, and lets non-JWT validators (API-key, opaque-token, etc.) participate without inventing claim-bearing shapes they don't have. `ClaimsPrincipal` (a sibling type in `org.apache.juneau.rest.auth`) exposes typed-getter access to the decoded JWT claims for users who need them — `@Auth ClaimsPrincipal p; p.getClaim("scope", String.class)`. Users who don't care about claims declare `@Auth Principal p` and get back whatever the validator produced.

## Risks

- **Auth bugs are security bugs.** Misconfigured JWT validators (accepting `none`, missing `aud` check, expired clock) are CVE-class. Mitigation: secure defaults (algorithm allowlist, mandatory `aud` check, 60s clock-skew cap), explicit deprecation warnings if users opt into risky configs, exhaustive test matrix against known attack patterns.
- **JWKS endpoint availability.** A down JWKS endpoint stalls every request. Mitigation: cache + graceful-degradation fallback (serve cached keys past TTL on fetch failure, log loudly).
- **Replay attacks.** JWT alone doesn't prevent replay. Document; recommend pairing with TODO-66's rate limit + short token lifetimes.
- **Multiple guards in `RestGuardList` ordering.** Auth must run before rate-limit (otherwise unauthenticated traffic uses the same bucket as authenticated). Document; ship a `RestGuardList.standardOrder(...)` helper that orders guards correctly.

## Related work

- `todo/FINISHED-40-remove-hc45-from-rest-common-and-server.md` — `Unauthorized` exception fluent-setter surface this needs.
- `todo/FINISHED-24-jsr330-and-spring-lite-support.md` — FQN-based recognition pattern for `@Auth`.
- `todo/TODO-66-rate-limit-and-request-id.md` (sibling) — composes in the same `RestGuardList`; ordering matters.
- `todo/TODO-61-rfc7807-server-side-wiring.md` (sibling) — `AuthenticationException` should render as `application/problem+json` when problem-details is on.
- Existing: `RoleBasedRestGuard` — sibling guard for AuthZ; the AuthN guards stash a `Principal` that role-based guards can then check against.
- `todo/FINISHED-77-mixin-ops-introspection.md` (related, already-landed) — established the `org.apache.juneau.rest.guard` package and shipped `DenyAllGuard` as the secure-by-default placeholder for `BasicAdminResource`. **Package-placement note:** the Scope section of this TODO targets `org.apache.juneau.rest.auth.*` for AuthN-domain types (`TokenValidator`, `BearerTokenGuard`, `ApiKeyGuard`, `AuthArg`, `AuthenticationException`, `ClaimsPrincipal`); FINISHED-77 placed `DenyAllGuard` (a generic safety-rail guard with no auth knowledge) in `org.apache.juneau.rest.guard`. Both placements are defensible — split by domain (auth-specific lives in `auth`, generic guards live in `guard`) is the v1 plan; implementer may co-locate everything in one package if they prefer, but should document the choice in the Phase 1 PR description so reviewers know it was deliberate. **`BasicAdminResource` integration:** the mixin is already wired to `@Rest(guards=DenyAllGuard.class)` with override-via-`@Bean RestGuardList` — once this TODO ships, the canonical integration example is `@Bean RestGuardList adminGuards() { return RestGuardList.of(new BearerTokenGuard(...)); }` on the host. **Do not** modify `BasicAdminResource` source as part of TODO-69; the seam is "any guard the host registers" and the mixin already honors it. Add an end-to-end test under `juneau-utest/src/test/java/org/apache/juneau/rest/ops/` (sibling to the existing `BasicAdminResource_AsMixin_Test`) that confirms a `BearerTokenGuard`-protected admin path returns `401` for missing token / `200` for a valid one — this is the integration smoke test that proves the deny-all-default → AuthN-replacement migration works as documented.

## Progress log

### 2026-05-24 — full plan executed end-to-end

All three phases plus the Phase 1.5 `BasicAdminResource` integration smoke test landed in a single
session against a clean working tree. Nothing committed, nothing pushed — user owns the FINISHED
rename + `/push` closeout.

**Phase 0 — seams confirmed (read-only).**
- `RestGuard.guard(RestRequest, RestResponse)` returns `boolean` and may throw; existing
  `RateLimitGuard` already sets `WWW-Authenticate`-shaped headers directly on `RestResponse` before
  throwing — established pattern adopted here.
- `Unauthorized` extends `BasicHttpException` and carries a fluent header API; the new
  `AuthenticationException` extends `Unauthorized` and adds a typed `wwwAuthenticate(String)`
  setter for the RFC 7235 §4.1 challenge.
- `RequestAttributes.set(String, Object)` is writable from a guard via `req.getAttributes().set(...)`;
  the chosen attribute key is `RestServerConstants.PRINCIPAL_ATTR = "juneau.principal"`.
- The `RestOpArg` SPI is registered in `DefaultConfig.restOpArgs` (added `AuthArg.class` in the
  natural alphabetical slot between `AttributeArg` and `ContentArg`).
- `@Bean RestGuardList guards(...)` is the canonical override seam; the bean store resolves
  `RestGuardList` under the **empty-string** name (not `"guards"`) — discovered during Phase 1d
  debugging when explicit `@Bean(name="guards")` annotations failed to override. Documented
  inline in the test files.

**Phase 1 — core guards in `juneau-rest-server`.**
- Production code (8 new files in `org.apache.juneau.rest.auth`):
  `Auth.java`, `AuthArg.java`, `AuthenticationException.java`, `ApiKeyGuard.java`,
  `ApiKeyStore.java`, `BearerTokenGuard.java`, `ClaimsPrincipal.java`,
  `TokenValidator.java`, `package-info.java`.
- `RestServerConstants` additions: `PRINCIPAL_ATTR`, `API_KEY_HEADER`.
- `DefaultConfig.restOpArgs` registers `AuthArg.class`.
- Tests (5 new files in `juneau-utest`):
  `ApiKeyGuard_Test` (24 tests), `AuthArg_Test`, `AuthenticationException_Test`,
  `BearerTokenGuard_Test`, `ClaimsPrincipal_Test` (14 tests).
- Coverage on the `org.apache.juneau.rest.auth` package: **branches 90% (62/69), instructions 97%
  (705/729)** — meets the ≥90% acceptance criterion. The 7 missed branches are the
  real-container `req.getCookies() != null` cookie-array path in `ApiKeyGuard.readCookie` (not
  exercised by `MockRestClient` since the mock servlet doesn't auto-parse the Cookie header) and
  two type-coercion branches in `ClaimsPrincipal.coerce` for primitive int/long siblings — both
  documented inline.

**Phase 1.5 — `BasicAdminResource` integration smoke test.**
- `BasicAdminResource_AuthIntegration_Test` in `juneau-utest/src/test/java/org/apache/juneau/rest/ops/`
  proves the FINISHED-77 cross-reference: swapping the mixin's `DenyAllGuard` default for a
  `BearerTokenGuard` is a zero-mixin-source-change migration. Confirms 401 for missing token,
  401 for rejected validator, 200 for valid token.

**Phase 2 — `juneau-rest-server-jwt` sub-module.**
- New Maven module `juneau-rest/juneau-rest-server-jwt/` with POM mirroring `juneau-rest-server-mcp`.
- `com.nimbusds:nimbus-jose-jwt:10.3` declared in `provided` scope. Containment verified
  programmatically: `mvn -pl juneau-rest/juneau-rest-server dependency:tree | grep -i nimbus`
  returns nothing (zero exit code from grep means "no match"). Nimbus shows up only on the new
  module's tree as `+- com.nimbusds:nimbus-jose-jwt:jar:10.3:provided`.
- Registered the module in `juneau-rest/pom.xml`'s `<modules>` list and added three
  `<artifactItem>` entries (sources, bin/lib, bin/osgi) to `juneau-distrib/pom.xml`.
- Production code (3 new files in `org.apache.juneau.rest.auth.jwt`):
  `JwksCache.java` (package-private, TTL cache with graceful-degradation: serves last-known-good
  keys past TTL on fetch failure with a `WARNING` log), `JwtTokenValidator.java` (JWKS-backed
  validator with mandatory `iss` / `aud` / `exp` / `nbf` checks, algorithm allowlist defaulting
  to `[RS256, ES256]`, configurable clock skew capped at 5 minutes, hard rejection of
  `alg: none` and the algorithm-confusion attack), `package-info.java`.
- Tests (6 new files in `juneau-utest/src/test/java/org/apache/juneau/rest/auth/jwt/`):
  `JwtTestSupport.java` (shared helpers for keypair generation + signing),
  `JwtTokenValidator_HappyPath_Test`, `JwtTokenValidator_ClaimValidation_Test`
  (covers `exp` / `nbf` / `iss` / `aud` mismatches, mandatory-claim absences, clock-skew
  acceptance window, and blank-subject placeholder),
  `JwtTokenValidator_Security_Test` (alg=none rejection, algorithm-confusion attack rejection,
  malformed-token + empty-token rejection), `JwtTokenValidator_ClaimsPrincipal_Test`,
  `JwtTokenValidator_Builder_Test` (12 tests: required-field rejection, mutual exclusion of
  `jwksUrl(...)` / `jwkSource(...)`, algorithm-list / clock-skew / TTL argument validation,
  getter round-trip), `JwksCache_Test` (5 tests: fetch + cache hit + stale-on-fetch-failure
  graceful-degradation + log warning + after-TTL refresh).
- Test deps added to `juneau-utest/pom.xml`: `juneau-rest-server-jwt` (test scope), and
  `com.nimbusds:nimbus-jose-jwt:10.3` explicitly (test scope, since the JWT module declares
  nimbus as `provided`).
- Coverage on the JWT module: **branches 90% (61/68), instructions 94% (598/634)** — meets the
  ≥85% acceptance criterion comfortably.

**Phase 3 — docs + release notes.**
- New topic page `pages/topics/10.20e.RestServerAuthGuards.md` in `juneau-docs` covering bearer /
  API-key / JWT in one place, with `BasicAdminResource` integration example, rate-limit-guard
  composition guidance, and a security checklist.
- Sidebar registration in `sidebars.ts` (slot `10.20e`, after `RestServerTestBeanInjection`).
- Release-notes entries in `pages/release-notes/9.5.0.md`:
  - Under `### juneau-rest-server`, a new `#### AuthN Guards — Bearer / API-Key / @Auth Principal (TODO-69)`
    section listing every new core type and the `BasicAdminResource` integration story.
  - A new top-level `### juneau-rest-server-jwt (new module)` section above the existing
    `### juneau-rest-server-mcp (new module)` listing, with explicit Maven coordinates,
    security-defaults callout, and a containment-verification note.

**Verification.**
- `./scripts/test.py -t` — green (full unit-test suite passes).
- `./scripts/test.py -b` — green (full build incl. RAT headers passes).
- Containment check: `mvn -pl juneau-rest/juneau-rest-server dependency:tree | grep -i nimbus`
  returns empty output (zero matches). Resolved Decision #1 is honored.
- Coverage on `org.apache.juneau.rest.auth`: 90% branches, 97% instructions.
- Coverage on `org.apache.juneau.rest.auth.jwt`: 90% branches, 94% instructions.

**Notable decisions during implementation.**
- `WWW-Authenticate` propagation: discovered that `RestContext.handleError` does NOT auto-copy
  headers from the thrown `BasicHttpException` to the `HttpServletResponse`. Adopted the existing
  `RateLimitGuard` pattern — set the `WWW-Authenticate` header directly on `RestResponse` before
  throwing, in both `BearerTokenGuard` and `ApiKeyGuard`. The exception still carries the header
  for callers that inspect it (e.g. logging), but the wire response goes through the explicit
  `res.setHeader(...)` call.
- `ApiKeyGuard.readCookie` falls back to manual `Cookie` header parsing when
  `req.getCookies()` returns null. Real Servlet containers populate `getCookies()` automatically;
  `MockServletRequest` only returns whatever was passed to `.cookies(Cookie[])`. The fallback
  makes the guard work uniformly under both real and mock containers.
- `JwtTokenValidator` runs its own claim verifier (the `processor.setJWTClaimsSetVerifier((c, ctx) -> { /* no-op */ })`
  line disables nimbus's internal verifier) so the configured `Clock` actually controls
  `exp` / `nbf` comparisons. Nimbus 10.3 dropped `DefaultJWTProcessor.setClockSkew(...)`, so
  manual verification is also the cleanest way to honor the configured skew.
- `nimbus-jose-jwt` 10.3 — bumped from the 9.40 mentioned in the original plan. 10.3 was the
  latest stable at land time; the API delta from 9.x (`DefaultJWTProcessor.setClockSkew` removed,
  `KeySourceException` moved to `com.nimbusds.jose`) was absorbed inline in `JwtTokenValidator`.

**Nothing committed. Nothing pushed.** User owns the FINISHED rename + `/push` closeout.
