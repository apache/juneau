# TODO-40: Remove Apache HttpClient 4.5 (`org.apache.http.*`) from `juneau-rest-server`

Source: created 2026-05-19. **Revised 2026-05-19 after partial Step 2 execution exposed deeper scope.** See "Reality check" below.

## Goal

`juneau-rest-server` should not pull `org.apache.httpcomponents:httpcore:4.4.16` into its dependency tree, and no `juneau-rest-server/src/main/java` file should import `org.apache.http.*`.

**End state:**

- `mvn -pl juneau-rest/juneau-rest-server dependency:tree | grep org.apache.httpcomponents` returns nothing.
- `rg -n '^import\s+org\.apache\.http\.' juneau-rest/juneau-rest-server/src/main/java` returns nothing.
- Every server-side public type exposes only transport-neutral Juneau types from `org.apache.juneau.http.*`.

## Reality check (what the 2026-05-19 spike revealed)

The original plan inventory only counted files with **direct** `import org.apache.http.*` statements (24 files). Partial execution of Step 2 (`httppart/*`) compiled cleanly, but a broader audit shows the scope is **~3× wider**:

### Direct vs. transitive HC 4.5 coupling

- **24 files** import `org.apache.http.*` directly — the original Step 2–6 list.
- **~65 additional files** import `org.apache.juneau.http.classic.*` (response/header/part/resource/remote) without ever touching `org.apache.http.*` themselves. These classic types extend HC 4.5 base classes, so even after the 24 files are clean, `juneau-rest-server` still transitively pulls `httpcore` via the `juneau-rest-common-classic` dependency.

To meet the "no `httpcomponents` in dep tree" end state, the work must also migrate the ~65 indirect users off `juneau-rest-common-classic`.

### Good news: the non-classic infrastructure is mostly there

A 2026-05-19 audit of `juneau-rest-common/src/main/java/org/apache/juneau/http/**` shows that ~80% of what the indirect users need already exists in transport-neutral form:

| Indirect use in server | Classic source | Non-classic equivalent in `juneau-rest-common` | Status |
|---|---|---|---|
| Response exception classes (`BadRequest`, `InternalServerError`, `Ok`, `NotFound`, …) | `org.apache.juneau.http.classic.response.*` (58 files) | `org.apache.juneau.http.response.*` (62 files) | ✅ Already exists. Different API surface (no `setHeader2` / `setHeaders(List<Header>)`-style fluent setters). |
| `BasicHttpException` | `org.apache.juneau.http.classic.response.BasicHttpException` | `org.apache.juneau.http.response.BasicHttpException` | ✅ Already exists. Slimmer API. |
| `BasicHttpResponse` | `org.apache.juneau.http.classic.response.BasicHttpResponse` | `org.apache.juneau.http.response.BasicHttpResponse` | ✅ Already exists. Backed by `HttpStatusLine` + `List<HttpHeader>` + `HttpBody`. |
| `HttpResource` | `org.apache.juneau.http.classic.resource.BasicResource` (+ subclasses) | `org.apache.juneau.http.resource.HttpResource` (interface only) | ⚠ Interface yes, concrete bean class missing. |
| `HeaderList` (HC's `Header[]` list wrapper) | `org.apache.juneau.http.classic.header.HeaderList` | No equivalent. `HttpHeaders` is a static factory, not a list type. | 🔴 Missing — needs `HttpHeaderList` (or just use `List<HttpHeader>` directly everywhere). |
| `PartList` | `org.apache.juneau.http.classic.part.PartList` | `org.apache.juneau.http.part.PartList` | ✅ Already exists. |
| Typed parts (`BasicBooleanPart`, `BasicStringPart`, `BasicIntegerPart`, `BasicLongPart`, `BasicDatePart`, `BasicUriPart`, `BasicCsvArrayPart`) | `org.apache.juneau.http.classic.part.*` (7 concrete classes) | `org.apache.juneau.http.part.HttpPartBean` only (no typed concretes) | 🔴 Missing concrete typed parts. |
| Typed headers (`BasicEntityTagHeader`, `BasicMediaTypeHeader`, `BasicDateHeader`, `BasicStringHeader`, …) | `org.apache.juneau.http.classic.header.Basic*Header` (13 concrete classes) | `org.apache.juneau.http.header.Http*Header` (abstract bases only) | ⚠ Bases exist but concrete `Basic*` equivalents missing. |
| `RequestLine`, `ProtocolVersion` | `org.apache.http.RequestLine`, `org.apache.http.ProtocolVersion` | none yet | 🔴 Need new `HttpRequestLine` interface + `HttpRequestLineBean`, plus `HttpProtocolVersion` record. |
| `HttpStatusLine.getProtocolVersion()` (currently returns `String`) | n/a | n/a | 🔧 Retype to `HttpProtocolVersion`. |
| RRPC remote metadata (`RrpcInterfaceMeta` / `RrpcInterfaceMethodMeta`) | `org.apache.juneau.http.classic.remote.*` | `org.apache.juneau.http.remote.*` | ✅ Already exists (duplicated). |

### API reconciliation work (the non-trivial part)

The non-classic response classes have a **slimmer fluent setter surface** than their classic counterparts:

- Classic `BadRequest.setHeader2(String, Object)`, `setHeaders(HeaderList)`, `setHeaders(List<Header>)`, `setHeaders2(Header...)`, `setLocale2(Locale)`, `setProtocolVersion(ProtocolVersion)`, `setReasonPhrase2(String)`, `setReasonPhraseCatalog(...)`, `setStatusCode2(int)`, `setStatusLine(BasicStatusLine)`, `setUnmodifiable()`, `setContent(HttpEntity)`, `setContent(String)`.
- Non-classic `BadRequest` only has the constructors (`()`, `(String)`, `(Throwable)`, `(String, Throwable)`) and a copy constructor.

Several `juneau-rest-server` files use the classic setters (`throw new BadRequest().setHeader2(…)`-style fluent builders). Migration needs either:

1. **Bring those setters to non-classic** (adds API surface but preserves call-site ergonomics), or
2. **Replace fluent-builder call sites** with constructor-only construction (more invasive but cleaner).

### Concrete scope numbers

- **Files to edit in `juneau-rest-server/src/main/java`**: ~70 (24 direct + ~46 indirect). Most are simple `import` swaps (`org.apache.juneau.http.classic.X` → `org.apache.juneau.http.X`); a minority touch the classic-only fluent setters and need real refactoring.
- **Non-classic classes to add or extend** before the server migration can compile: roughly 20–25 (HttpRequestLine + bean, HttpProtocolVersion, ~7 typed parts, ~13 concrete typed headers, HttpHeaderList equivalent, BasicResource equivalent, plus fluent-setter overloads on the 62 response classes if option 1 above is picked).
- **Server-side public API breaking changes**: same as the original plan — `RequestHeaders.add(Header...)` → `add(HttpHeader...)`, etc. Hard break for 9.5, no `juneau-rest-server-classic` split.
- **Test files to update outside `rest/client/classic/**`**: ~5 (per original plan, still accurate).

## What was attempted on 2026-05-19 (reverted)

Step 1 + Step 2 were attempted. Step 1 created `HttpProtocolVersion`, `HttpRequestLine`, `HttpRequestLineBean`, retyped `HttpStatusLine.getProtocolVersion()`, and added tests — all clean and compiled. Step 2 migrated all 10 `httppart/*` files (`BasicNamedAttribute`, `RequestHttpPart`, `RequestHeader`, `RequestPathParam`, `RequestQueryParam`, `RequestFormParam`, `RequestHeaders`, `RequestPathParams`, `RequestQueryParams`, `RequestFormParams`) to use `HttpPart` / `HttpHeader` in their public signatures — also clean and compiled. The `RestRequest.addDefault(...)` call sites were patched with two private adapter helpers (`toHttpHeaderList(Header[])` / `toHttpPartList(NameValuePair[])`) to bridge classic→non-classic types.

All of that was reverted to HEAD on 2026-05-19 after discovering the deeper coupling — the partial work would have left the server still pulling `httpcore` transitively while breaking the public API. **The same edits can be reapplied unchanged when execution resumes.**

## Proposed phased plan

Splitting the original 10-step plan into independent phases makes each shippable on its own and avoids the all-or-nothing risk:

### Phase A — Foundation (non-classic infrastructure)

Add the missing transport-neutral types in `juneau-rest-common` so the server has somewhere to migrate **to**. No server-side changes yet.

1. `HttpRequestLine` interface + `HttpRequestLineBean` default (in `org.apache.juneau.http`).
2. `HttpProtocolVersion` record with `protocol()` / `major()` / `minor()`.
3. Retype `HttpStatusLine.getProtocolVersion()` from `String` to `HttpProtocolVersion`; update `HttpStatusLineBean` + its test.
4. Concrete typed parts (`HttpBooleanPart`, `HttpStringPart`, `HttpIntegerPart`, `HttpLongPart`, `HttpDatePart`, `HttpUriPart`, `HttpCsvArrayPart`) in `org.apache.juneau.http.part`.
5. Concrete typed headers (`HttpStringHeaderBean`, `HttpBooleanHeaderBean`, `HttpIntegerHeaderBean`, `HttpLongHeaderBean`, `HttpDateHeaderBean`, `HttpEntityTagHeaderBean`, `HttpEntityTagsHeaderBean`, `HttpMediaTypeHeaderBean`, `HttpMediaRangesHeaderBean`, `HttpStringRangesHeaderBean`, `HttpCsvHeaderBean`, `HttpUriHeaderBean`) in `org.apache.juneau.http.header`. Or: extend existing abstract `Http*Header` bases with a `.of(...)` factory.
6. `HttpHeaderList` (mirror of classic `HeaderList`) OR adopt `List<HttpHeader>` everywhere instead.
7. Concrete `HttpResourceBean` (mirror of classic `BasicResource`) in `org.apache.juneau.http.resource`.
8. Fluent-setter overloads on the existing non-classic response classes (`setHeader`, `setHeaders(List<HttpHeader>)`, `setProtocolVersion(HttpProtocolVersion)`, `setStatusCode`, `setReasonPhrase`, `setLocale`, `setContent(HttpBody)`, `setContent(String)`, `setUnmodifiable()`) — only those actually used by `juneau-rest-server`, audited from current classic call sites.

Compile gate: `mvn -pl juneau-rest/juneau-rest-common -am test-compile`.
Tests: add `Http*_Test.java` for each new type (the Step-1 spike already drafted three of them).

### Phase B — `httppart/*` migration (request side)

Identical to the original Step 2. Migrates `BasicNamedAttribute`, `RequestHttpPart`, the four `Request*Param` leaf classes, `RequestHeader`, and the four `Request*Params` / `RequestHeaders` collection classes off `org.apache.http.*` onto `HttpHeader` / `HttpPart`. Includes the small `RestRequest` adapter helpers used by `addDefault(...)`.

This phase was attempted and shown to be tractable on 2026-05-19. Re-apply that work.

### Phase C — Response infrastructure migration

For each of the ~65 indirect-coupling server files, swap the import package: `org.apache.juneau.http.classic.{response,header,part,resource,remote}` → `org.apache.juneau.http.{response,header,part,resource,remote}`. Reconcile any fluent-setter call sites that depend on classic-only methods (Phase A.8 should have already added the needed overloads).

This is the largest mechanical phase. Best split into sub-PRs by sub-package to keep diffs reviewable:

- C.1 — `org.apache.juneau.http.classic.response.*` → `org.apache.juneau.http.response.*` (the biggest sub-set, ~50 files).
- C.2 — `org.apache.juneau.http.classic.header.*` → `org.apache.juneau.http.header.*` (~12 files).
- C.3 — `org.apache.juneau.http.classic.part.*` → `org.apache.juneau.http.part.*` (~5 files).
- C.4 — `org.apache.juneau.http.classic.resource.*` → `org.apache.juneau.http.resource.*` (~5 files).
- C.5 — `org.apache.juneau.http.classic.remote.*` → `org.apache.juneau.http.remote.*` (2 files).

### Phase D — `RestRequest` / `RestResponse` / sessions

Identical to the original Step 3. Retype `RestRequest.getRequestLine()` → `HttpRequestLine`, `getProtocolVersion()` → `HttpProtocolVersion`, `getAllHeaders()` → `HttpHeader[]`. Migrate `RestResponse`, `RestSession`, `RestOpSession`, `RrpcRestOpSession`. Drop the `RestRequest` adapter helpers introduced in Phase B once the upstream call paths (`RestOpContext.getDefaultRequestHeaders` etc.) are HttpHeader/HttpPart-typed.

### Phase E — Processors

Identical to the original Step 4. Includes the `HttpEntityProcessor` → `HttpBodyProcessor` rename + the migration-guide row.

### Phase F — Static files + assertions + minor leaks

Identical to original Steps 5 and 6. `StaticFiles` / `BasicStaticFiles`, `FluentRequestLineAssertion` / `FluentProtocolVersionAssertion`, `ArgException`, `SeeOtherRoot`.

### Phase G — Drop the classic dep + docs + bookkeeping

Original Steps 7, 9, 10.

- Drop `juneau-rest-common-classic` from `juneau-rest-server/pom.xml`.
- `dependency:tree` no longer mentions `httpcomponents`.
- Update 5 non-classic utest files (original Step 8).
- Release notes (`9.5.0.md`), migration guide (`23.01.V9.5-migration-guide.md`), and topic refresh under `juneau-docs/pages/topics/`.
- Remove TODO-40 bullet, `git mv` to `FINISHED-40-*.md`.

## Decisions (resolved 2026-05-19)

1. **Mitigation: (A) hard break.** No `juneau-rest-server-classic` split, no dual-typed interim.
2. **`HttpProtocolVersion`: typed record.** Added in Phase A, used by `HttpRequestLine` / `HttpStatusLine` / `RestRequest.getProtocolVersion()` / `FluentProtocolVersionAssertion`.
3. **Processor rename: yes.** `HttpEntityProcessor` → `HttpBodyProcessor`. Migration-Guide row required.

## Decisions (resolved 2026-05-19, second round)

4. **`HttpHeaderList`** — yes, add a new collection class in `juneau-rest-common` mirroring classic `HeaderList` semantics (`getFirst/getLast/getAll`/builder-style).
5. **Concrete typed parts/headers** — yes, add all of them (~7 typed parts + ~13 typed headers) so server code keeps the same call ergonomics it had under classic.
6. **Fluent setters on response classes** — yes, bring the classic `setHeader / setHeaders / setProtocolVersion / setStatusCode / setReasonPhrase / setLocale / setContent / setUnmodifiable` family onto non-classic `BasicHttpException` and its 62 subclasses.
7. **Phasing** — work proceeds linearly A → B → C → D → E → F → G but lands as a **single commit** at the end (no per-phase commits). User reviews before commit lands.

## Out of scope

- Touching `juneau-rest-common-classic` (it stays HC 4.5-coupled by design — that's the whole point of the `.classic` package).
- Touching `juneau-rest-client-classic`, `juneau-rest-server-rdf`, `juneau-rest-server-springboot`, transport adapters (`-apache-httpclient-45`, `-apache-httpclient-50`). They keep using HC 4.5 where they need to.
- Renaming the `httppart/` directory or any package paths in `juneau-rest-server`. The change is signature-level only.

## Verification

1. `python3 scripts/test.py --full` — full clean build + tests green.
2. `mvn -pl juneau-rest/juneau-rest-server dependency:tree | grep -i 'httpcomponents'` — no matches.
3. `mvn -pl juneau-rest/juneau-rest-common -am dependency:tree | grep -i 'httpcomponents'` — already empty (confirms TODO-42 work didn't regress).
4. `rg -n '^import\s+org\.apache\.http\.' juneau-rest/juneau-rest-server/src/main/java` — empty.
5. `rg -n '^import\s+org\.apache\.juneau\.http\.classic\.' juneau-rest/juneau-rest-server/src/main/java` — empty.
6. Sample `@RestGet` handler that uses `RequestHeaders.add(...)` / `RestRequest.getAllHeaders()` compiles against the new transport-neutral signatures.

## Notes

- The Step-1 spike code (`HttpProtocolVersion`, `HttpRequestLine`, `HttpRequestLineBean`, retyped `HttpStatusLine`, tests) and the Step-2 spike code (all 10 `httppart/*` files) compiled cleanly and can be re-applied as Phase A.1–A.3 + Phase B once the phasing is approved.
- The original plan's "End state" goal is still correct; the breakdown into 10 numbered steps was just too coarse. The phased plan above keeps the same goal but scopes each phase to something independently shippable.
