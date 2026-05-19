# TODO-38: Rename `juneau-rest-client` → `juneau-rest-client-classic` and `juneau-ng-rest-client-*` → `juneau-rest-client-*`

Source: created on 2026-05-18. Updated 2026-05-19 (Option 1 landed; ng.http promoted to canonical). Updated 2026-05-19 (Steps 3–6 + 8–9 landed; docs underway).

## Status: NEAR-COMPLETE — Steps 1–6, 8, 9 done; Step 7 (docs) substantially done; awaiting final verification + commit

The new client family (`juneau-rest-client-*`), the classic split of the existing client, the `juneau-rest-common` common-module rename + `ng.http` → canonical promotion, the physical `juneau-rest-client-classic` Maven module, the mock-module split + `NgMockRestClient` → `MockRestClient` rename, the test renames, and the shaded/distrib updates are all landed.

`mvn -DskipTests install` (full project) is green. `mvn test-compile` (full project, including `juneau-utest`) is green.

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

### Classic split inside `juneau-rest-client` (Step 3 — completed 2026-05-19)
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

### Mock module (Step 4 — completed 2026-05-19)
- `org.apache.juneau.ng.rest.mock` → `org.apache.juneau.rest.mock` package promotion.
- `MockHttpTransport` lives at canonical `org.apache.juneau.rest.mock.MockHttpTransport`.
- **`NgMockRestClient` renamed to `MockRestClient`** at canonical `org.apache.juneau.rest.mock.MockRestClient`.
- Legacy `MockRestClient` / `MockRestRequest` / `MockRestResponse` / `MockHttpClientConnectionManager` / `MockConsole` / `MockLogger` moved to `org.apache.juneau.rest.mock.classic.*` (still in `juneau-rest-mock` module).
- Shared types stay at `org.apache.juneau.rest.mock.*`: `MockServletRequest`, `MockServletResponse`, `MockHttpSession`, `MockPathResolver`. `MockServletRequest.debug(boolean)`, `MockServletResponse.getHeaders()`, and `MockPathResolver` were widened to public so the classic mock client can access them across the new package boundary.

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

The follow-up to actually **remove** the HC 4.5 (`org.apache.http.*`) dependency from `juneau-rest-common` and `juneau-rest-server` is tracked separately in **TODO-40** (see `todo/TODO-40-remove-hc45-from-rest-common-and-server.md`).

### Step 2 — `org.apache.juneau.ng.http.*` → canonical `org.apache.juneau.http.*` (completed)

After Step 1 freed the canonical namespace, the ng.http promotion landed:

- All `org.apache.juneau.ng.http.{entity,header,part,resource,response}` directories promoted to `org.apache.juneau.http.{entity,header,part,resource,response}`.
- `org.apache.juneau.ng.http.remote.RrpcInterfaceMeta` / `RrpcInterfaceMethodMeta` → `org.apache.juneau.http.remote.*`.
- Top-level facades promoted: `HttpBodies`, `HttpBody`, `HttpHeader`, `HttpHeaders`, `HttpPart`, `HttpParts`, `HttpResponses`, `HttpStatusLine` → `org.apache.juneau.http.*`.
- All ~74 source references updated to canonical.

### Step 5 — Shaded / distrib renames (completed 2026-05-19)
- `juneau-shaded/juneau-shaded-rest-client/pom.xml` — adds `juneau-rest-client-classic` alongside `juneau-rest-client`; description updated.
- `juneau-shaded/juneau-shaded-all/pom.xml` — adds `juneau-rest-client-classic` alongside `juneau-rest-client`.
- `juneau-distrib/pom.xml` — emits sources + jar + OSGi bundle for the new `juneau-rest-client-classic` artifact (filename `apache-juneau-rest-client-classic-<version>*.jar`, OSGi bundle `org.apache.juneau.rest.client.classic_<version>.jar`).

### Step 6 — Test renames (completed 2026-05-19)
- `juneau-utest/src/test/java/org/apache/juneau/ng/rest/Ng*_Test.java` → `juneau-utest/src/test/java/org/apache/juneau/rest/client/*_Test.java` (with the `Ng` prefix dropped).
- `juneau-utest/src/test/java/org/apache/juneau/ng/rest/Ng?MockRestClient_Test.java` → `juneau-utest/src/test/java/org/apache/juneau/rest/mock/MockRestClient_Test.java`.
- `juneau-utest/src/test/java/org/apache/juneau/ng/NgPackageScanner.java` → `juneau-utest/src/test/java/org/apache/juneau/PackageScanner.java` (test utility).
- Legacy mock tests `MockRestClient_Coverage_Test` / `MockRestClient_PathVars_Test` moved into `juneau-utest/src/test/java/org/apache/juneau/rest/mock/classic/` to mirror the source split.
- Three remaining `Ng*_Test` filenames (`NgHttp_Test`, `NgNamedHeaders_Test`, `NgNamedResponses_Test`) renamed to drop the `Ng` prefix (`Http_Test`, `NamedHeaders_Test`, `NamedResponses_Test`).
- 216 consumer test files updated: `import org.apache.juneau.rest.mock.*;` → `import org.apache.juneau.rest.mock.classic.*;` (selectively — only where the file uses legacy types and not shared servlet-mock helpers). Two mixed-usage files (`Swagger_Test.java`, `mock2/MockServletRequest_Coverage_Test.java`) get explicit named imports for `MockRestClient`/`MockRestRequest` from `.classic`.
- `juneau-utest/src/test/java/org/apache/juneau/rest/client/classic/` hosts the legacy classic `RestClient_*` test suite.

### Step 8 — Examples + microservice templates (completed 2026-05-19)
- `juneau-examples/juneau-examples-rest-jetty-ftest` source files (`ContentComboTestBase`, `RootResourcesTest`, `SamplesMicroservice`) repointed at `org.apache.juneau.rest.client.classic.RestClient` since they exercise the legacy fluent API (`json5()`, `plainText()`, `closeQuietly()`, `rootUrl(URI)`, `serializer(...)`).
- `juneau-microservice/juneau-microservice/pom.xml` — adds dependency on `juneau-rest-client-classic` alongside `juneau-rest-client` so downstream consumers transitively get both flavors.
- Microservice templates (`juneau-my-jetty-microservice`, `juneau-my-springboot-microservice`) inherit through `juneau-microservice` — no per-template change required.

### Step 9 — IDE / build-tooling files (completed 2026-05-19)
- All `.project` / `.classpath` / `.settings/*.prefs` files are local IDE files (gitignored), so Eclipse will regenerate them on re-import; no manual edits required.
- OSGi `Bundle-SymbolicName` / `Export-Package` entries are auto-generated by the `maven-bundle-plugin` for the new `juneau-rest-client-classic` module — verified that the produced manifest exports `org.apache.juneau.rest.client.classic`, `.classic.assertion`, and `.classic.remote`.
- `scripts/*.py` / `.cursor/commands/*.md` reviewed — no hard-coded references to the old `juneau-ng-rest-client*` artifact IDs.

### Step 7 — Docs (`juneau-docs`) — substantially complete 2026-05-19

Sourced changes in `juneau-docs/`:
- `pages/release-notes/9.5.0.md` — "Next-Generation REST Client and HTTP Stack" section rewritten to describe the canonical names (no `Ng*`, no `org.apache.juneau.ng.*`); added new "Classic Module Split" subsection; rewrote the migration-path bullets.
- `pages/topics/12.15.NextGenRestClient.md` — full rewrite. New package-layout table now distinguishes canonical (`rest.client`, `rest.mock`, `http`) vs. classic (`rest.client.classic`, `rest.mock.classic`, `http.classic`) and the new `juneau-rest-client-classic` module. Mock-transport section uses canonical `MockRestClient`.
- `pages/topics/23.01.V9.5-migration-guide.md` — new "REST Client and HTTP Stack Promotion (TODO-38)" section with four sub-tables: Maven module changes, package renames, class renames (early-snapshot only), and mock-client layout, plus an import-diff for `juneau-rest-common` consumers.
- `pages/topics/13.02.MockRestClientOverview.md` — added an info-block introducing the two flavors of `MockRestClient`; deep-linked all `<a>` apidocs links to `.classic.MockRestClient` / `.classic.MockRestRequest`.
- `pages/topics/01.05.RestClient.md` — added a stack-distinguishing intro paragraph; deep-linked `MockRestClient` apidoc to `.classic`.
- `pages/topics/20.03.JuneauShadedRestClient.md` — listing updated to include `juneau-rest-client-classic`; external-dependency text fixed (HC 4.5.x, not 5.2+).
- `pages/topics/20.06.JuneauShadedAll.md` — module list now includes `juneau-rest-client-classic`; external-dependency text updated.

Still TBD (low priority):
- `static/ai/juneau-knowledge.jsonl` — auto-generated AI artifact; regenerate when the next batch of artifact-bound knowledge ships.
- `pages/topics/12.01.JuneauRestClientBasics.md` (and similar end-to-end docs) — still describes the classic surface. Treating as "stays accurate for the classic client" — readers route to `NextGenRestClient.md` for the new stack.
- Sidebar config (`sidebars.ts`) — left as-is; `NextGenRestClient` entry is still relevant as a "Beta" topic.

## Verification

1. `mvn -DskipTests install` — full clean build green. ✓
2. `mvn test-compile` — full project test-compile green. ✓
3. `mvn -pl juneau-rest/juneau-rest-client dependency:tree | grep org.apache.httpcomponents` — empty. ✓ (no HC dep on the canonical client)
4. `mvn -pl juneau-rest/juneau-rest-client-classic dependency:tree | grep org.apache.httpcomponents` — `httpclient:jar:4.5.14`. ✓
5. `rg -l 'org\.apache\.juneau\.ng\.|NgRestClient|NgMockRestClient' juneau-* juneau-utest` — only matches in `todo/` plan files and `juneau-docs/static/ai/juneau-knowledge.jsonl`. ✓ (Source code is clean of `ng.*` references except in `FINISHED-*` archive markdowns, which are historical record.)
6. Eclipse re-import — pending (user action).

## Open follow-ups

- **TODO-40** — Remove the residual Apache HC 4.5 (`org.apache.http.*`) dependency from `juneau-rest-common` and `juneau-rest-server`. Currently `juneau-rest-common` exports both the new (HC-free) `org.apache.juneau.http.*` packages **and** the classic `.classic.*` packages that still leak HC 4.5 onto every consumer's classpath.
- **Sidebar / IA review** — consider promoting `NextGenRestClient.md` out of the "Beta" sidebar slot once the canonical stack is declared stable in a later release.

## Notes

- Compile-green checkpoint: `mvn -DskipTests install` (full project) passes as of 2026-05-19.
- The promoted ng package contents now live at canonical names (no more `org.apache.juneau.ng.http.*` / `org.apache.juneau.ng.rest.client.*` / `org.apache.juneau.ng.rest.mock.*`).
- FINISHED archive files (`FINISHED-11a-restclient-ng-design-plan.md`, `FINISHED-11b-restclient-ng-coverage-closeout.md`) are historical record of the `.ng.` scaffolding — do not edit.
