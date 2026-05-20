# FINISHED-38: Renamed `juneau-rest-client` → `juneau-rest-client-classic` and `juneau-ng-rest-client-*` → `juneau-rest-client-*`

Created 2026-05-18. Landed 2026-05-19 across commits `05df0323e1` (rest-common `.classic` split + `ng.http` → canonical promotion), `27bd4960ed` (REST client + HTTP stack canonical promotion + `juneau-rest-client-classic` module), and `6a041405e5` (juneau-docs).

## Outcome

The next-generation REST client family was promoted from `org.apache.juneau.ng.*` / `juneau-ng-rest-client-*` to the canonical `org.apache.juneau.rest.client.*` / `juneau-rest-client-*` namespace.  The pre-existing HC-4.5-based client was moved into a new sibling `juneau-rest-client-classic` Maven module under `org.apache.juneau.rest.client.classic.*` so both stacks can coexist on the same classpath.

Verification baselines at completion:

- `mvn -DskipTests install` — full project clean build green.
- `mvn test-compile` — full project test-compile green, including `juneau-utest`.
- `mvn -pl juneau-rest/juneau-rest-client dependency:tree | grep org.apache.httpcomponents` — empty (canonical client carries no HC dep).
- `mvn -pl juneau-rest/juneau-rest-client-classic dependency:tree | grep org.apache.httpcomponents` — `httpclient:jar:4.5.14` (classic client still carries HC 4.5 by design).
- `rg -l 'org\.apache\.juneau\.ng\.|NgRestClient|NgMockRestClient' juneau-* juneau-utest` — source-tree clean (only matches are in `todo/` history and one auto-generated AI artifact).

## What landed

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

### Classic split inside `juneau-rest-client` (Step 3)
- Legacy `org.apache.juneau.rest.client.*` source moved to `org.apache.juneau.rest.client.classic.*`.
- Legacy `assertion` and `remote` subpackages moved to `.classic.assertion` / `.classic.remote`.
- New `juneau-rest-client-classic` Maven module created at `juneau-rest/juneau-rest-client-classic/`:
  - Owns all classic source under `org.apache.juneau.rest.client.classic.*` (28 files).
  - Depends on `juneau-rest-common` + `org.apache.httpcomponents:httpclient:4.5.14`.
  - OSGi bundle exports `org.apache.juneau.rest.client.classic`, `.classic.assertion`, `.classic.remote`.
- `juneau-rest-client/pom.xml` updated to remove the HC 4.5 dependency (it now hosts only the canonical NG client).
- Aggregator `juneau-rest/pom.xml` lists both modules. Consumers depending on legacy types add the new `juneau-rest-client-classic` artifact (already done in `juneau-rest-mock` and `juneau-microservice`).

### Type renames (the new client gets the canonical names)
- `NgRestClient` → `RestClient` (`org.apache.juneau.rest.client.RestClient`).
- `NgRestRequest` → `RestRequest`.
- `NgRestResponse` → `RestResponse`.
- `NgRestCallException` → `RestCallException`.
- `NgRemoteClient` → `RemoteClient`.
- `NgMockRestClient` → `MockRestClient` (Step 4).

### Adapter packages
- All five transport adapters live under canonical `org.apache.juneau.rest.client.<adapter>.*` (e.g. `org.apache.juneau.rest.client.apachehttpclient45.*`).
- `META-INF/services/org.apache.juneau.rest.client.HttpTransportProvider` SPI files added in each adapter module.
- Obsolete `META-INF/services/org.apache.juneau.ng.rest.client.HttpTransportProvider` SPI files removed.

### Mock module (Step 4)
- `org.apache.juneau.ng.rest.mock` → `org.apache.juneau.rest.mock` package promotion.
- `MockHttpTransport` lives at canonical `org.apache.juneau.rest.mock.MockHttpTransport`.
- **`NgMockRestClient` renamed to `MockRestClient`** at canonical `org.apache.juneau.rest.mock.MockRestClient`.
- Legacy `MockRestClient` / `MockRestRequest` / `MockRestResponse` / `MockHttpClientConnectionManager` / `MockConsole` / `MockLogger` moved to `org.apache.juneau.rest.mock.classic.*` (still in `juneau-rest-mock` module).
- Shared types stay at `org.apache.juneau.rest.mock.*`: `MockServletRequest`, `MockServletResponse`, `MockHttpSession`, `MockPathResolver`. `MockServletRequest.debug(boolean)`, `MockServletResponse.getHeaders()`, and `MockPathResolver` were widened to public so the classic mock client can access them across the new package boundary.

### Common-module rename (Step 1 — Option 1)

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

The follow-up to actually **remove** the HC 4.5 (`org.apache.http.*`) dependency from `juneau-rest-common` and `juneau-rest-server` is tracked separately in **TODO-40** (see `todo/TODO-40-remove-hc45-from-rest-common-and-server.md`).

### Step 2 — `org.apache.juneau.ng.http.*` → canonical `org.apache.juneau.http.*`

After Step 1 freed the canonical namespace, the ng.http promotion landed:

- All `org.apache.juneau.ng.http.{entity,header,part,resource,response}` directories promoted to `org.apache.juneau.http.{entity,header,part,resource,response}`.
- `org.apache.juneau.ng.http.remote.RrpcInterfaceMeta` / `RrpcInterfaceMethodMeta` → `org.apache.juneau.http.remote.*`.
- Top-level facades promoted: `HttpBodies`, `HttpBody`, `HttpHeader`, `HttpHeaders`, `HttpPart`, `HttpParts`, `HttpResponses`, `HttpStatusLine` → `org.apache.juneau.http.*`.
- All ~74 source references updated to canonical.

### Step 5 — Shaded / distrib renames
- `juneau-shaded/juneau-shaded-rest-client/pom.xml` — adds `juneau-rest-client-classic` alongside `juneau-rest-client`; description updated.
- `juneau-shaded/juneau-shaded-all/pom.xml` — adds `juneau-rest-client-classic` alongside `juneau-rest-client`.
- `juneau-distrib/pom.xml` — emits sources + jar + OSGi bundle for the new `juneau-rest-client-classic` artifact (filename `apache-juneau-rest-client-classic-<version>*.jar`, OSGi bundle `org.apache.juneau.rest.client.classic_<version>.jar`).

### Step 6 — Test renames
- `juneau-utest/src/test/java/org/apache/juneau/ng/rest/Ng*_Test.java` → `juneau-utest/src/test/java/org/apache/juneau/rest/client/*_Test.java` (with the `Ng` prefix dropped).
- `juneau-utest/src/test/java/org/apache/juneau/ng/rest/Ng?MockRestClient_Test.java` → `juneau-utest/src/test/java/org/apache/juneau/rest/mock/MockRestClient_Test.java`.
- `juneau-utest/src/test/java/org/apache/juneau/ng/NgPackageScanner.java` → `juneau-utest/src/test/java/org/apache/juneau/PackageScanner.java` (test utility).
- Legacy mock tests `MockRestClient_Coverage_Test` / `MockRestClient_PathVars_Test` moved into `juneau-utest/src/test/java/org/apache/juneau/rest/mock/classic/` to mirror the source split.
- Three remaining `Ng*_Test` filenames (`NgHttp_Test`, `NgNamedHeaders_Test`, `NgNamedResponses_Test`) renamed to drop the `Ng` prefix (`Http_Test`, `NamedHeaders_Test`, `NamedResponses_Test`).
- 216 consumer test files updated: `import org.apache.juneau.rest.mock.*;` → `import org.apache.juneau.rest.mock.classic.*;` (selectively — only where the file uses legacy types and not shared servlet-mock helpers). Two mixed-usage files (`Swagger_Test.java`, `mock2/MockServletRequest_Coverage_Test.java`) get explicit named imports for `MockRestClient`/`MockRestRequest` from `.classic`.
- `juneau-utest/src/test/java/org/apache/juneau/rest/client/classic/` hosts the legacy classic `RestClient_*` test suite.

### Step 8 — Examples + microservice templates
- `juneau-examples/juneau-examples-rest-jetty-ftest` source files (`ContentComboTestBase`, `RootResourcesTest`, `SamplesMicroservice`) repointed at `org.apache.juneau.rest.client.classic.RestClient` since they exercise the legacy fluent API (`json5()`, `plainText()`, `closeQuietly()`, `rootUrl(URI)`, `serializer(...)`).
- `juneau-microservice/juneau-microservice/pom.xml` — adds dependency on `juneau-rest-client-classic` alongside `juneau-rest-client` so downstream consumers transitively get both flavors.
- Microservice templates (`juneau-my-jetty-microservice`, `juneau-my-springboot-microservice`) inherit through `juneau-microservice` — no per-template change required.

### Step 9 — IDE / build-tooling files
- All `.project` / `.classpath` / `.settings/*.prefs` files are local IDE files (gitignored), so Eclipse will regenerate them on re-import; no manual edits required.
- OSGi `Bundle-SymbolicName` / `Export-Package` entries are auto-generated by the `maven-bundle-plugin` for the new `juneau-rest-client-classic` module — verified that the produced manifest exports `org.apache.juneau.rest.client.classic`, `.classic.assertion`, and `.classic.remote`.
- `scripts/*.py` / `.cursor/commands/*.md` reviewed — no hard-coded references to the old `juneau-ng-rest-client*` artifact IDs.

### Step 7 — Docs (`juneau-docs`)

Sourced changes in `juneau-docs/`:
- `pages/release-notes/9.5.0.md` — "Next-Generation REST Client and HTTP Stack" section rewritten to describe the canonical names (no `Ng*`, no `org.apache.juneau.ng.*`); added new "Classic Module Split" subsection; rewrote the migration-path bullets.
- `pages/topics/12.15.NextGenRestClient.md` — full rewrite. New package-layout table now distinguishes canonical (`rest.client`, `rest.mock`, `http`) vs. classic (`rest.client.classic`, `rest.mock.classic`, `http.classic`) and the new `juneau-rest-client-classic` module. Mock-transport section uses canonical `MockRestClient`.
- `pages/topics/23.01.V9.5-migration-guide.md` — new "REST Client and HTTP Stack Promotion (TODO-38)" section with four sub-tables: Maven module changes, package renames, class renames (early-snapshot only), and mock-client layout, plus an import-diff for `juneau-rest-common` consumers.
- `pages/topics/13.02.MockRestClientOverview.md` — added an info-block introducing the two flavors of `MockRestClient`; deep-linked all `<a>` apidocs links to `.classic.MockRestClient` / `.classic.MockRestRequest`.
- `pages/topics/01.05.RestClient.md` — added a stack-distinguishing intro paragraph; deep-linked `MockRestClient` apidoc to `.classic`.
- `pages/topics/20.03.JuneauShadedRestClient.md` — listing updated to include `juneau-rest-client-classic`; external-dependency text fixed (HC 4.5.x, not 5.2+).
- `pages/topics/20.06.JuneauShadedAll.md` — module list now includes `juneau-rest-client-classic`; external-dependency text updated.

Carried over as low-priority cleanup (not blocking TODO-38):
- `static/ai/juneau-knowledge.jsonl` — auto-generated AI artifact; regenerates with the next artifact-bound knowledge batch.
- `pages/topics/12.01.JuneauRestClientBasics.md` and similar end-to-end docs — accurate for the classic client; readers route to `NextGenRestClient.md` for the canonical stack.
- Sidebar config (`sidebars.ts`) — `NextGenRestClient` stays in the "Beta" sidebar slot until the canonical stack is declared stable in a later release.

## Follow-ups that spun out of TODO-38

- **TODO-40** — Remove the residual Apache HC 4.5 (`org.apache.http.*`) dependency from `juneau-rest-common` and `juneau-rest-server`. After TODO-38, `juneau-rest-common` exports both the new (HC-free) `org.apache.juneau.http.*` packages **and** the classic `.classic.*` packages, the latter still pulling HC 4.5 onto every consumer's classpath.
- **TODO-41** (shipped) — Folded the unreleased `juneau-rest-client-java-httpclient` adapter back into `juneau-rest-client` and promoted the JDK `HttpClient` transport to the built-in default. See `FINISHED-41-merge-java-httpclient-default-transport.md`.
- **TODO-42** (shipped) — Split `juneau-rest-common` into transport-neutral + classic Maven artifacts. See `FINISHED-42-split-rest-common-classic.md`.

## Notes

- The promoted ng package contents now live at canonical names (no more `org.apache.juneau.ng.http.*` / `org.apache.juneau.ng.rest.client.*` / `org.apache.juneau.ng.rest.mock.*`).
- FINISHED archive files (`FINISHED-11a-restclient-ng-design-plan.md`, `FINISHED-11b-restclient-ng-coverage-closeout.md`) hold the historical record of the `.ng.` scaffolding — do not edit.
