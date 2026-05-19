# TODO-38: Rename `juneau-rest-client` → `juneau-rest-client-classic` and `juneau-ng-rest-client-*` → `juneau-rest-client-*`

Source: created on 2026-05-18. Updated 2026-05-19 (Option 1 landed; ng.http promoted to canonical).

## Status: IN PROGRESS — Step 1 (Option 1) and Step 2 done; full project test-compile passing

The new client family (`juneau-rest-client-*`), the classic split of the existing client, **and** the `juneau-rest-common` common-module rename + `ng.http` → canonical promotion are now landed.

`mvn test-compile` passes across all modules (full project, not just `juneau-rest,juneau-utest`).

## What's done

### Module renames (transport adapters)
- All five transport adapter modules: `juneau-ng-rest-client-*` → `juneau-rest-client-*` (directory + artifactId).
  - `juneau-rest-client-apache-httpclient-45`
  - `juneau-rest-client-apache-httpclient-50`
  - `juneau-rest-client-java-httpclient`
  - `juneau-rest-client-okhttp`
  - `juneau-rest-client-jetty`
- `juneau-rest/pom.xml` aggregator updated.
- `juneau-utest/pom.xml` test dependencies updated.

### Package promotions inside `juneau-rest-client`
- `org.apache.juneau.ng.rest.client` → `org.apache.juneau.rest.client` (canonical).
- `org.apache.juneau.ng.rest.client.assertion` → `org.apache.juneau.rest.client.assertion`.
- `org.apache.juneau.ng.rest.client.remote` → `org.apache.juneau.rest.client.remote`.

### Classic split inside `juneau-rest-client`
- Legacy `org.apache.juneau.rest.client.*` source moved to `org.apache.juneau.rest.client.classic.*` (same module — the physical split into a separate `juneau-rest-client-classic` artifact is still pending; see Step 3 below).
- Legacy `assertion` and `remote` subpackages moved to `.classic.assertion` / `.classic.remote`.
- All consumers (server, mock, tests) updated to point at `.classic` for legacy types and canonical for the new client.

### Type renames (the new client gets the canonical names)
- `NgRestClient` → `RestClient` (`org.apache.juneau.rest.client.RestClient`).
- `NgRestRequest` → `RestRequest`.
- `NgRestResponse` → `RestResponse`.
- `NgRestCallException` → `RestCallException`.
- `NgRemoteClient` → `RemoteClient`.

### Adapter packages
- All five transport adapters live under canonical `org.apache.juneau.rest.client.<adapter>.*` (e.g. `org.apache.juneau.rest.client.apachehttpclient45.*`).
- `META-INF/services/org.apache.juneau.rest.client.HttpTransportProvider` SPI files added in each adapter module.
- Obsolete `META-INF/services/org.apache.juneau.ng.rest.client.HttpTransportProvider` SPI files removed.

### Mock module
- `org.apache.juneau.ng.rest.mock` → `org.apache.juneau.rest.mock` package promotion.
- `NgMockRestClient` and `MockHttpTransport` now live under canonical `org.apache.juneau.rest.mock`.
- `NgMockRestClient` is **not yet renamed to `MockRestClient`** — would collide with legacy `org.apache.juneau.rest.mock.MockRestClient`. Pending mock-module classic split (Step 4).

### Common-module rename (Step 1 — completed via Option 1)

The user picked **Option 1** ("rename legacy in place to `org.apache.juneau.http.classic.*` inside `juneau-rest-common`"). Implemented as follows (~171 files renamed):

- Legacy subpackages relocated inside `juneau-rest-common`:
  - `org.apache.juneau.http.entity` → `org.apache.juneau.http.classic.entity` (8 files)
  - `org.apache.juneau.http.header` → `org.apache.juneau.http.classic.header` (74 files)
  - `org.apache.juneau.http.part` → `org.apache.juneau.http.classic.part` (15 files)
  - `org.apache.juneau.http.resource` → `org.apache.juneau.http.classic.resource` (8 files)
  - `org.apache.juneau.http.response` → `org.apache.juneau.http.classic.response` (58 files)
- Legacy top-level facades relocated:
  - `BasicStatusLine`, `HttpEntities`, `HttpHeaders`, `HttpParts`, `HttpResources`, `HttpResponses` → `org.apache.juneau.http.classic.*` (6 files)
- Legacy RPC helpers relocated (annotations stay at canonical):
  - `RrpcInterfaceMeta`, `RrpcInterfaceMethodMeta` → `org.apache.juneau.http.classic.remote` (2 files)
- **Stayed at canonical `org.apache.juneau.http`** (no HC 4.5 coupling):
  - `HttpMethod` (constants only)
  - `org.apache.juneau.http.annotation.*` (entire subdir of Juneau annotations — `@Header`, `@Path`, `@Query`, `@FormData`, `@Content`, `@Response`, etc.)
  - `org.apache.juneau.http.remote.*` annotation classes (`Remote`, `RemoteGet`, `RemotePost`, ...) and `RemoteReturn`, `RemoteUtils` helpers
- Test files mirrored:
  - All `juneau-utest/src/test/java/org/apache/juneau/http/{entity,header,part,resource,response}/*` → `.../http/classic/{entity,header,part,resource,response}/*` (137 files)
  - Top-level test files for moved facades → `.../http/classic/` (10 files: `BasicStatusLine_Test`, `HttpHeaders_Test`, `HttpParts_Test`, `BasicHeader_Test`, `BasicHttpResource_Test`, `BasicPart_Test`, `EntityTag_Test`, `SerializedHeader_Test`, `SerializedHttpEntity_Test`, `SerializedPart_Test`)
- Consumer imports updated across `juneau-rest-server`, `juneau-rest-client`, `juneau-rest-mock`, `juneau-rest-server-springboot`, `juneau-utest`, `juneau-examples`, `juneau-microservice`.
- Ambiguity resolution: after the ng.http promotion (below), files that previously had both `import org.apache.juneau.http.*;` and `import org.apache.juneau.http.classic.*;` star-imports had the canonical wildcard dropped to disambiguate (since canonical now hosts ng-promoted facades). Three server files (`RrpcServlet`, `RrpcRestOpContext`, `RrpcRestOpSession`) and `RestClient.java` in classic get explicit `import org.apache.juneau.http.classic.remote.RrpcInterfaceMeta;` / `RrpcInterfaceMethodMeta;`.

The follow-up to actually **remove** the HC 4.5 (`org.apache.http.*`) dependency from `juneau-rest-common` and `juneau-rest-server` is tracked separately in **TODO-40** (see `todo/TODO-40-remove-hc45-from-rest-common-and-server.md`).

### Step 2 — `org.apache.juneau.ng.http.*` → canonical `org.apache.juneau.http.*` (completed)

After Step 1 freed the canonical namespace, the ng.http promotion landed:

- `juneau-rest-common/.../ng/http/entity` → `.../http/entity`
- `juneau-rest-common/.../ng/http/header` → `.../http/header`
- `juneau-rest-common/.../ng/http/part` → `.../http/part`
- `juneau-rest-common/.../ng/http/resource` → `.../http/resource`
- `juneau-rest-common/.../ng/http/response` → `.../http/response`
- `juneau-rest-common/.../ng/http/remote/RrpcInterfaceMeta.java` → `.../http/remote/RrpcInterfaceMeta.java`
- `juneau-rest-common/.../ng/http/remote/RrpcInterfaceMethodMeta.java` → `.../http/remote/RrpcInterfaceMethodMeta.java`
- Top-level promotion: `HttpBodies`, `HttpBody`, `HttpHeader`, `HttpHeaders`, `HttpPart`, `HttpParts`, `HttpResponses`, `HttpStatusLine` → `org.apache.juneau.http.*`.
- The original `juneau-rest-common/.../ng/http/package-info.java` removed.
- All ~74 files referencing `org.apache.juneau.ng.http.*` updated to canonical `org.apache.juneau.http.*` across the codebase.

### Test reorganisation (juneau-utest)
- All 22 legacy `juneau-utest/src/test/java/org/apache/juneau/rest/client/*Test.java` files moved to `juneau-utest/src/test/java/org/apache/juneau/rest/client/classic/`, with matching package declarations.
- Test imports for transport adapters re-pointed at canonical (no `.classic.<adapter>` paths).

### Example/ftest module repointed at classic
- `juneau-examples-rest-jetty-ftest` source files (`ContentComboTestBase`, `RootResourcesTest`, `SamplesMicroservice`) repointed at `org.apache.juneau.rest.client.classic.RestClient` since they exercise the legacy fluent API (`json5()`, `plainText()`, `closeQuietly()`, `rootUrl(URI)`, `serializer(...)`).

## What's left

### Step 3. Physical `juneau-rest-client` module split

Currently `juneau-rest-client` hosts both:
- Canonical promoted source (`org.apache.juneau.rest.client.*`) — the new transport-agnostic client.
- `.classic` subpackage source (`org.apache.juneau.rest.client.classic.*`) — the legacy HC 4.5 client.

Per the TODO-38 plan, these should be **two separate Maven modules**:
- `juneau-rest/juneau-rest-client` — new client only, no HC 4.5 dependency.
- `juneau-rest/juneau-rest-client-classic` — legacy client.

Until this split lands, anyone depending on `juneau-rest-client` still pulls HC 4.5 transitively (since classic source lives in the same jar and `juneau-rest-common` still leaks HC 4.5 via the `.classic.*` subtree — see TODO-40).

### Step 4. Mock module classic split + `NgMockRestClient` rename

- Move legacy `org.apache.juneau.rest.mock.*` → `org.apache.juneau.rest.mock.classic.*` (including `MockRestClient`, `MockRestRequest`, `MockRestResponse`, `MockServletRequest`, `MockServletResponse`, etc.).
- Rename `NgMockRestClient` → `MockRestClient` once the namespace is free.
- Update all test imports.

### Step 5. Shaded / distrib renames

- `juneau-shaded/juneau-shaded-rest-client` — point at the new client. Optionally add `juneau-shaded-rest-client-classic`.
- `juneau-shaded/juneau-shaded-all` — include both.
- `juneau-distrib/pom.xml` — emit bundles for both new and classic clients.

### Step 6. Test renames

- Move `juneau-utest/src/test/java/org/apache/juneau/ng/rest/Ng*_Test.java` → `juneau-utest/src/test/java/org/apache/juneau/rest/client/*_Test.java` with the `Ng` prefix dropped.
- Move `juneau-utest/src/test/java/org/apache/juneau/ng/http/*` → `juneau-utest/src/test/java/org/apache/juneau/http/*` (canonical, where the ng-promoted types now live).
- Update package declarations and imports.

### Step 7. Docs (`juneau-docs`)

Per the original plan:
- `pages/topics/12.15.NextGenRestClient.md` — fold into main RestClient page.
- `pages/topics/12.01.JuneauRestClientBasics.md` — rewrite to describe the new client.
- `pages/topics/01.05.RestClient.md`, `01.03.EndToEndRest.md`, `19.02.JuneauExamplesRest.md`, `20.0[1|3|6]*.md` — replace `NgRestClient` references.
- `sidebars.ts` — drop dedicated "NextGenRestClient" entry.
- `static/ai/juneau-knowledge.jsonl` — regenerate.
- `README.md`, `src/pages/downloads.md`, `src/pages/about.md` — replace `juneau-ng-rest-client` artifact IDs.
- Release notes `pages/release-notes/9.5.0.md` — add Breaking changes entries (including the legacy → `.classic.*` rename inside `juneau-rest-common`).
- Migration guide `pages/topics/23.01.V9.5-migration-guide.md` — add old → new rows.

### Step 8. Examples + microservice templates

- `juneau-examples/juneau-examples-rest-*/pom.xml` — switch to new client where appropriate.
- `juneau-microservice/juneau-my-jetty-microservice` template.
- `juneau-microservice/juneau-microservice-core/pom.xml`.

### Step 9. IDE / build-tooling files

- `.project`, `.settings/*.prefs` for renamed modules.
- OSGi `Bundle-SymbolicName` / `Export-Package` entries in renamed module poms (esp. `juneau-rest-common`: need to export both `org.apache.juneau.http.*` and `org.apache.juneau.http.classic.*`).
- `scripts/*.py` shortcuts, `.cursor/commands/*.md` that hardcode old artifact IDs.

## Verification (after all steps land)

1. `./scripts/test.py` (full clean build + tests).
2. `mvn -pl juneau-rest/juneau-rest-client -am dependency:tree` — confirm no `org.apache.httpcomponents` on the new client classpath. (Blocked by TODO-40 + Step 3.)
3. `mvn -pl juneau-rest/juneau-rest-client-classic -am dependency:tree` — confirm HC 4.5 still pulled, only `.classic.*` source. (Blocked by Step 3.)
4. `git grep -nE 'juneau-ng-rest-client|org\.apache\.juneau\.ng\.'` returns zero hits in source. (Currently a few `ng.rest` paths remain inside `juneau-rest-mock`, `juneau-rest-client-jetty`, `juneau-rest-client-apache-httpclient-50`, etc. — to be cleaned in Step 4 / Step 3 follow-ups.)
5. Eclipse re-import — no module name conflicts.

## Open questions still outstanding

- **`juneau-rest-server` Apache HC 4.5 dependency.** Tracked by **TODO-40**.
- **Shaded "RestClient" jar.** Plan defaults to shading the new client; confirm if HC 4.5 users are the dominant audience.

## Notes

- Compile-green checkpoint: `mvn test-compile` (full project) passes as of 2026-05-19.
- The promoted ng package contents now live at canonical names (no more `org.apache.juneau.ng.http.*`).
- FINISHED archive files (`FINISHED-11a-restclient-ng-design-plan.md`, `FINISHED-11b-restclient-ng-coverage-closeout.md`) are historical record of the `.ng.` scaffolding — do not edit.
