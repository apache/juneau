# FINISHED-11b: RestClient NG — Coverage Closeout + Cross-Transport Remote-Interface Tests

Archived from `TODO-11-restclient-ng-coverage-closeout.md` (May 2026).

## Companion archive

- `FINISHED-11a-restclient-ng-design-plan.md` — original design plan, implementation, docs, and release notes for the NG REST client and HTTP stack.

## Goal (as captured in the original plan)

Close out coverage for the `org.apache.juneau.ng.*` packages once `TODO-31` (inject-aware microservice) and `TODO-33` (dynamic child REST resources) landed. Specifically:

- Build a cross-transport remote-interface test suite against a real Jetty pipeline.
- Bring `org.apache.juneau.ng.http` to **≥95% instruction coverage**.
- Plug any residual gaps in the five transport modules.
- Confirm no regressions and that the full `juneau-utest` suite stayed in the ~33s range.

## Outcome

- **`org.apache.juneau.ng.http`** (`juneau-rest-common`): **95% instructions / 80% branches** (was 38% / 20%).
- **`juneau-utest`** grew from 50,181 tests to **50,491 tests**, 0 failures, 0 errors. Runtime ~33s, unchanged.
- **All transport modules**: 77–88% instructions / 71–100% branches — unchanged here; transport-side branches that need real wire-level fault injection are left for a future targeted pass and the residual hostile-server edge cases remain out of scope (see _Deferred_ below).

## Phase A — Cross-transport remote-interface tests

Boots a real Jetty pipeline on an ephemeral port and runs a focused scenario set through the NG remote-interface proxy against five live transports.

- **`juneau-utest/.../microservice/MicroserviceTestFixture.java`** — JUnit 5 extension that starts a `JettyMicroservice` on port 0 from one or more `@Configuration` classes and exposes `getRootUrl()`. Resolves the actual bound port via `ServerConnector.getLocalPort()` and pins the host to `localhost` to keep the URL deterministic across dev machines.
- **`juneau-utest/.../ng/rest/NgRemoteInterfaceTransport_Test.java`** — parameterized over the five NG transports (`apache-hc45`, `apache-hc5`, `java-http`, `okhttp`, `jetty`). Server side is a single `BasicRestServlet` with `defaultAccept = "text/plain"` so assertions can compare plain wire bytes without HTML wrapping. Twelve scenarios × five transports = **60 tests**.

Covered scenarios:

- GET with `@Path` (single segment) and `@Query` (single value).
- POST with `@Content` (string).
- `@Header` propagation.
- Response status: 200 with body, 204 no-content, 404 → remote exception mapping.
- Concurrent calls via `Executors.newFixedThreadPool` to confirm transports stay thread-safe under contention.

Scenarios that the original plan called for but the **current NG remote client** does not yet support (map/bean `@Query`, `@FormData`, bean `@Content`, `Reader`/`InputStream` `@Content`, end-to-end `@Remote(rrpc=true)`) were intentionally **scoped out**. The classic `RestClient` already covers them through `MockRestClient`; the NG proxy is API-incomplete on these surfaces and we should drive coverage as those features ship rather than write tests that have nothing to bind to.

## Phase B — `org.apache.juneau.ng.http` coverage closeout

A small set of parametric tests cover the bulk of the previously-uncovered surface (named response classes, named header classes, body/part implementations, and the static-factory facades) without committing to one test class per type.

- **`juneau-utest/.../ng/NgPackageScanner.java`** — shared utility that walks the classpath (JARs or exploded `target/classes` directories) and enumerates the concrete classes in a given package. Used by every parametric test below so we don't hand-maintain class lists.
- **`juneau-utest/.../ng/http/response/NgNamedResponses_Test.java`** — parametric over every concrete subclass of `BasicHttpResponse` / `BasicHttpException` in `org.apache.juneau.ng.http.response` (**57 tests**). Reads each class's `STATUS_CODE` / `REASON_PHRASE` constants, exercises every public constructor (including null cause / null message / null body paths), verifies `getStatusCode`, `getStatusLine`, `getHeaders`, `toString`, and runs `withBody(...)` / `withHeader(...)` mutator chains on response classes.
- **`juneau-utest/.../ng/http/header/NgNamedHeaders_Test.java`** — parametric over every concrete `HttpHeaderBean` subclass in `org.apache.juneau.ng.http.header` (**53 tests**). Walks every public static `of(...)`, `ofLazy*(...)` factory, supplying type-appropriate sample values dispatched by the header's base type. Calls the typed accessors that match each base class on **every instance built** — not just the last — so the eager-value, wire-string, and lazy-supplier branches in `getValue` / `toX` / `asX` each get exercised. Special-cases polymorphic headers (`IfRange`, `RetryAfter`) and CSV-token headers (`Allow`, `ContentLanguage`, …) so the right `Supplier` shape gets passed.
- **`juneau-utest/.../ng/http/header/PolymorphicHeaders_Test.java`** — targeted coverage for the `IfRange` and `RetryAfter` polymorphic value branches (**19 tests**). Exercises eager entity-tag, eager date, eager integer, wire-string detection (numeric vs entity-tag-with-quote vs HTTP-date), lazy suppliers returning each of the typed payloads, lazy suppliers returning null, and null-input factory short-circuits.
- **`juneau-utest/.../ng/http/response/HttpStatusLineBean_Test.java`** — covers the `HttpStatusLineBean` factories, custom protocol version, null-reason-phrase `toString` path, null-protocol-version rejection, and `equals` / `hashCode` (**6 tests**).
- **`juneau-utest/.../ng/http/entity/HttpBodies_Test.java`** — covers `StringBody`, `ByteArrayBody`, `FileBody`, `StreamBody`, `HttpBodyBean`, and `MultipartBody`: default vs explicit content type, repeatability flags, defensive copy semantics, `writeTo(OutputStream)`, the multipart builder pattern, and the part factories (**15 tests**).
- **`juneau-utest/.../ng/http/part/HttpParts_Test.java`** — smoke tests for `HttpPartBean` factories / `equals` / `hashCode` / `toString` and for `PartList.getFirst` hit / miss plus the null-value skip path in `PartList.writeTo` / `toString` (**~10 tests**).
- **`juneau-utest/.../ng/http/HttpFactoryFacades_Test.java`** — parametric over every public static method on the three façade classes (`HttpHeaders`, `HttpBodies`, `HttpResponses`). Each is a thin delegation to an underlying factory; reflectively invoking every overload with type-appropriate sample args closes out ~300 otherwise-uncovered delegation instructions (**152 tests**).

## Phase C / D — Verify + archive

- `./scripts/coverage.py --run juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/ng/http/` — **95% / 80%**, ≥95% goal met.
- Full `juneau-utest` suite: **50,491 tests, 0 failures, 0 errors**, ~33s runtime.
- This file replaces the live plan; `[TODO-11]` removed from `todo/TODO.md`.

## Deferred (intentionally not closed by this pass)

- **`org.apache.juneau.ng.http.remote.RrpcInterfaceMeta`** (~132 missed instructions): the RRPC proxy hasn't been wired into the NG remote client yet, so there is no useful end-to-end path to test. Pick this up when the NG remote client grows `@Remote(rrpc=true)` support.
- **NG remote-client gaps** (bean-shaped `@Query` / `@FormData`, `Reader` / `InputStream` request bodies, multi-segment `@Path`, RRPC): write the corresponding `NgRemoteInterfaceTransport_Test` scenarios as those features ship.
- **Hostile-server transport-edge cases** (truncated response body, malformed status line, multi-value header carry-through, idle connection release under abort): the parametric Jetty fixture from Phase A already exercises the happy paths across all five transports. The remaining transport-side branches live behind real wire faults and want a `com.sun.net.httpserver.HttpServer` harness modelled on the existing `ApacheHc45Transport_Test` rather than the Juneau pipeline used here. Track separately if it becomes a real coverage problem.
