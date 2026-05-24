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

## Open questions

1. **JWT library choice.** `nimbus-jose-jwt` (recommended — battle-tested, minimal transitive deps) vs `jjwt` (less battle-tested) vs the JDK's own `java.security.spec` primitives (write-it-yourself, error-prone). Recommend nimbus.
2. **JWKS cache TTL default.** 5 minutes (recommended) vs 1 hour vs honoring HTTP `Cache-Control` from the JWKS endpoint. Recommend 5 minutes with manual override.
3. **Algorithm allowlist.** Default `[RS256, ES256]` (recommended; reject HS256 unless explicitly opted-in to prevent algorithm-confusion). Configurable.
4. **`@Auth` annotation name.** `@Auth` (recommended — short, unambiguous) vs `@Principal` (clashes with `java.security.Principal`) vs `@AuthenticatedUser`.
5. **Auto-register a default `RestGuardList`?** No — require explicit `@Bean(name="guards") RestGuardList`. Auto-registration is a footgun (silently enables auth on resources that don't expect it).
6. **`Principal` subtype.** Use `java.security.Principal` (recommended — JDK standard) vs a Juneau-owned `AuthenticatedPrincipal` carrying claims. Recommend JDK `Principal`; ship a `ClaimsPrincipal extends Principal` for token-with-claims callers who want structured access.

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
- `todo/FINISHED-77-mixin-ops-introspection.md` (related, already-landed) — established the `org.apache.juneau.rest.guard` package and shipped `DenyAllGuard` as the secure-by-default placeholder for `BasicAdminResource`. New AuthN guards (`BearerTokenGuard`, `ApiKeyGuard`, JWT verifier) **should land in the same `org.apache.juneau.rest.guard` package** for consistency. `BasicAdminResource` is already wired to `@Rest(guards=DenyAllGuard.class)` with override-via-`@Bean RestGuardList` — once this TODO ships, the canonical integration example is `@Bean RestGuardList adminGuards() { return RestGuardList.of(new BearerTokenGuard(...)); }` on the host. **Do not** modify `BasicAdminResource` source as part of TODO-69; the seam is "any guard the host registers" and the mixin already honors it. Add an end-to-end test under `juneau-utest/src/test/java/org/apache/juneau/rest/ops/` (sibling to the existing `BasicAdminResource_AsMixin_Test`) that confirms a `BearerTokenGuard`-protected admin path returns `401` for missing token / `200` for a valid one — this is the integration smoke test that proves the deny-all-default → AuthN-replacement migration works as documented.
