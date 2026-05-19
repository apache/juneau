# TODO-40: Remove Apache HttpClient 4.5 (`org.apache.http.*`) from `juneau-rest-common` and `juneau-rest-server`

Source: created on 2026-05-19 as the follow-up to TODO-38 (Option 1 picked — legacy Juneau http types renamed in place to `org.apache.juneau.http.classic.*` inside `juneau-rest-common`).

## Goal

`juneau-rest-common` and `juneau-rest-server` should not reference Apache HttpClient 4.5 types (`org.apache.http.Header`, `org.apache.http.HttpEntity`, `org.apache.http.HttpRequest`, `org.apache.http.HttpResponse`, `org.apache.http.NameValuePair`, `org.apache.http.RequestLine`, `org.apache.http.StatusLine`, `org.apache.http.ProtocolVersion`, etc.) in their **public** APIs or their internals. The HC 4.5 wire model should be confined to `juneau-rest-client-classic` (and any transport adapter that explicitly opts into HC 4.5).

End state:

- `mvn -pl juneau-rest/juneau-rest-common -am dependency:tree` and `mvn -pl juneau-rest/juneau-rest-server -am dependency:tree` contain **no** `org.apache.httpcomponents:httpcore` (or `httpclient`) entries.
- All server-side public types (`RequestHeaders`, `RestRequest`, `RestResponse`, `RequestHttpPart`, etc.) expose only transport-neutral Juneau types from `org.apache.juneau.http.*` (the canonical namespace freed by TODO-38).

## Background — concrete HC 4.5 leakage points

Found 24 source files in `juneau-rest-server` (as of 2026-05-19) and many more in `juneau-rest-common` that `import org.apache.http.*`:

### `juneau-rest-server` — server-side public API surface

- `RequestHeaders` — `add(org.apache.http.Header...)`, `addDefault(org.apache.http.Header...)`, `set(org.apache.http.Header...)`, return values typed `org.apache.http.Header`. **Highest-impact: every `@RestGet` user's request-side code sees these signatures.**
- `RestRequest` — `getAllHeaders()` returns `org.apache.http.Header[]`.
- `RestResponse` — sets headers and entity using HC 4.5 types.
- `RestSession`, `RestOpSession`, `RrpcRestOpSession` — wrap the HC 4.5 request/response model.
- `RequestHttpPart`, `RequestHeader`, `RequestPathParam`, `RequestQueryParam`, `RequestFormParam` — base classes that implement `org.apache.http.NameValuePair` / `org.apache.http.Header`.
- `RequestHeaders`, `RequestPathParams`, `RequestQueryParams`, `RequestFormParams` — collection wrappers around HC 4.5-typed parts.
- `BasicNamedAttribute` — implements `org.apache.http.NameValuePair`.
- `HttpEntityProcessor`, `HttpResponseProcessor`, `ResponseBeanProcessor` — return-value processors keyed on HC 4.5 entity/response types.
- `BasicStaticFiles`, `StaticFiles` — accept `org.apache.http.Header[]` for cache-control.
- `FluentRequestLineAssertion`, `FluentProtocolVersionAssertion` — fluent assertions over `org.apache.http.RequestLine` / `org.apache.http.ProtocolVersion`.
- `SeeOtherRoot`, `ArgException` — minor leaks.

### `juneau-rest-common` (after TODO-38 Option 1 lands)

All HC 4.5 leakage lives under the renamed `org.apache.juneau.http.classic.*` package tree:

- `org.apache.juneau.http.classic.header.*` — every header class implements/wraps `org.apache.http.Header` (~140 files).
- `org.apache.juneau.http.classic.response.*` — every response class implements `org.apache.http.HttpResponse` (~40 files).
- `org.apache.juneau.http.classic.entity.*` — `BasicHttpEntity` implements `org.apache.http.HttpEntity`.
- `org.apache.juneau.http.classic.part.*`, `org.apache.juneau.http.classic.resource.*`, `org.apache.juneau.http.classic.remote.*` — supporting types.
- `org.apache.juneau.http.classic.{HttpHeaders,HttpEntities,HttpResponses,HttpParts,HttpResources,HttpMethod,BasicStatusLine}` — top-level static-factory facades returning HC 4.5-typed values.

### `juneau-rest-common` — the transport-neutral path (after TODO-38 `ng.http` promotion)

The canonical `org.apache.juneau.http.*` namespace will host the **new** transport-neutral types (promoted from `org.apache.juneau.ng.http.*`):

- `org.apache.juneau.http.header.*` — `HttpHeader`-based types implementing only Juneau interfaces, no `org.apache.http.Header`.
- `org.apache.juneau.http.response.*` — `BasicHttpException` / response beans without `org.apache.http.HttpResponse`.
- `org.apache.juneau.http.entity.*` — `HttpBody` / `MultipartBody` / etc.
- `org.apache.juneau.http.part.*`, `org.apache.juneau.http.resource.*`.

The migration in TODO-40 is essentially: **point server-side code at the transport-neutral types instead of the `.classic` ones.**

## Scope (in-tree)

### Module dependency changes

- `juneau-rest-server/pom.xml` — verify that after this TODO, `juneau-rest-common`'s `httpcore` transitive is gone.
- `juneau-rest-common/pom.xml` — `org.apache.httpcomponents:httpcore` becomes an optional / scope=`provided` dependency only for the `.classic` subpackage (or removed entirely if `.classic` moves to its own module — see TODO-38 Step 1 alternatives).

### Server-side type rewrites

For each file in the inventory above:

1. Replace `import org.apache.http.*;` with imports from the transport-neutral `org.apache.juneau.http.*` namespace.
2. Change method signatures from `org.apache.http.Header` → `org.apache.juneau.http.header.HttpHeader` (or a more specific Juneau header type).
3. Replace `org.apache.http.NameValuePair` with `org.apache.juneau.commons.httppart.NameValue` (or equivalent transport-neutral type) where possible.
4. Replace `org.apache.http.HttpEntity` → `org.apache.juneau.http.entity.HttpBody`.
5. Replace `org.apache.http.HttpResponse` with the Juneau response model.
6. Replace `org.apache.http.RequestLine` / `StatusLine` / `ProtocolVersion` with Juneau equivalents (may need new lightweight beans in `juneau-rest-common`).

### Test updates

All `juneau-utest/src/test/java/org/apache/juneau/rest/**` tests that exercise the changed signatures need updates. Estimate: 100+ test files.

### Documentation

- `juneau-docs` — every code example showing `Header[] getAllHeaders()`, `RequestHeaders.add(Header)` etc. has to be rewritten.
- Migration guide (TODO-17) — large "Breaking changes" entry: server-side public API is no longer HC 4.5-typed.
- Release notes 9.5 — same.

## Risk / decisions

- **Breaking change for every existing `@RestGet` user.** Anything that captures `org.apache.http.Header` from a `RequestHeaders` lookup, or returns `org.apache.http.HttpEntity` from a custom processor, must change. This is the same impact as TODO-38's `.ng.` removal — it's a 9.5 breaking change.
- **`juneau-rest-server-classic` follow-up?** Same question raised in TODO-38: if the breaking change is too costly, keep server's HC 4.5 surface around as `juneau-rest-server-classic` and ship a transport-neutral `juneau-rest-server` alongside it. Out of scope unless decided otherwise.
- **`org.apache.juneau.commons.httppart` reuse?** A lot of the transport-neutral abstractions already live in `juneau-commons` (`HttpPartType`, `NameValue`-style beans). Audit before introducing new lightweight beans in `juneau-rest-common`.

## Steps

1. Wait for TODO-38 Step 2 to land (`org.apache.juneau.ng.http.*` → canonical `org.apache.juneau.http.*`). TODO-40 can't usefully start before that namespace move.
2. Per-file rewrite of `juneau-rest-server` types in the inventory order above. Largest-impact files first (`RequestHeaders`, `RestRequest`, `RestResponse`).
3. Per-file rewrite of `juneau-rest-common` legacy facades.
4. Update tests + docs.
5. Verify `dependency:tree` no longer shows `httpcore` / `httpclient` under `juneau-rest-common` or `juneau-rest-server`.

## Verification

1. `./scripts/test.py` (full clean build + tests).
2. `mvn -pl juneau-rest/juneau-rest-common -am dependency:tree | grep -i 'httpcomponents'` returns nothing.
3. `mvn -pl juneau-rest/juneau-rest-server -am dependency:tree | grep -i 'httpcomponents'` returns nothing.
4. `git grep -nE '^import\s+org\.apache\.http\.' juneau-rest/juneau-rest-common juneau-rest/juneau-rest-server` returns nothing.
5. Sample `@RestGet` rewrite compiles against the transport-neutral signatures.

## Notes

- This TODO is a direct consequence of choosing Option 1 in TODO-38: keeping legacy HC 4.5-typed classes inside `juneau-rest-common` (renamed to `.classic.*`) traded "common-is-HC-4.5-free" for "no server→client dependency inversion". TODO-40 cashes back the trade-off.
