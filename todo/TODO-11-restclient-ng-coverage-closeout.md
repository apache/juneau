# TODO-11: RestClient NG — Coverage Closeout + Cross-Transport Remote-Interface Tests

> **Status:** Implementation, docs, and release notes for the next-generation REST client and HTTP stack (`org.apache.juneau.ng.*`) have shipped. The original design plan is archived in `todo/FINISHED-11a-restclient-ng-design-plan.md`. This file tracks the **remaining** cleanup before `TODO-11` can be fully retired.

## Remaining work

### A. Cross-transport remote-interface tests  (blocked on `[TODO-31]`)

Existing `org.apache.juneau.http.remote` tests cover remote-interface behavior end-to-end but only against the classic `MockRestClient` — they never go through any real wire transport. The new NG transports each have their own basic coverage against `com.sun.net.httpserver.HttpServer` with canned responses, but nothing exercises a Juneau remote proxy through a real Juneau REST pipeline on the server side.

Once `[TODO-31]` lands, build:

- `MicroserviceTestFixture` in `juneau-utest` — JUnit 5 extension that boots a `JettyMicroservice` on port 0 from one or more `@Configuration` classes and exposes `getRootUrl()`. Tears down in `afterAll`.
- `NgRemoteInterfaceTransport_Test` in `juneau-utest` — parameterized over the five NG transports (`apache-hc45`, `apache-hc5`, `java-http`, `okhttp`, `jetty`) via `@ParameterizedTest` + `@MethodSource`. Server side is a single `@Rest` resource provided as a `@Bean` from a test `@Configuration`.

Focused scenario set per transport (each scenario runs 5x — once per transport):

- GET with `@Path` (single + multi-segment) and `@Query` (single + map + bean).
- POST with `@Content` (string, bean, `Reader`, `InputStream`).
- POST with `@FormData` (single, map, bean).
- Header propagation (`@Header` on parameter, default headers on the client).
- Response status: 200 with body, 204 no-content, 404 -> remote exception mapping.
- One end-to-end RRPC scenario through `@Remote(rrpc=true)` to confirm bidirectional bean marshalling.

5 transports x ~15 scenarios = ~75 executions. This is the primary lift for transport-module coverage.

### B. `org.apache.juneau.ng.http` coverage closeout  (independent — can land before TODO-31)

The bulk of the uncovered surface is in named response classes (`Ok`, `Created`, `NotFound`, ...) and RFC-named header classes (`Accept`, `ContentType`, ...). Cover with two parametric tests:

- `juneau-utest/src/test/java/org/apache/juneau/ng/http/response/NgNamedResponses_Test.java` — reflectively enumerate every public subclass of the response base in `org.apache.juneau.ng.http.response`; for each, invoke every public constructor / factory, call all public getters, run `writeTo(OutputStream)`, verify status code matches the RFC code.
- `juneau-utest/src/test/java/org/apache/juneau/ng/http/header/NgNamedHeaders_Test.java` — same parametric pattern over `org.apache.juneau.ng.http.header.*`. Walks `of(...)` factories, value accessors, and `writeTo` / wire-format paths.
- Fill targeted gaps in `HttpBody`, `HttpHeaders`, `HttpResource` discovered by `./scripts/coverage.py --branches` (small one-shot tests).

### C. Transport-module residual gaps  (after A)

After the parameterized suite from A has run, fill what remains via `./scripts/coverage.py --branches`:

- `close()` / connection-release hooks on each transport's response type.
- Header carry-through for multi-value and quoted-value cases.
- Streaming-body request paths (each transport's `InputStream` consumption code).

For hostile-server edge cases (truncated body, malformed status line), use `com.sun.net.httpserver.HttpServer` following the existing pattern in `ApacheHc45Transport_Test`.

### D. Verify + archive

- `./scripts/coverage.py --run` against `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/ng/http/`; confirm >=95% instruction coverage.
- `./scripts/coverage.py --branches` for every NG package; address remaining gaps.
- `./scripts/test.py` full suite; confirm no regressions and runtime stays in the ~33s range.
- Archive this file as `todo/FINISHED-11b-restclient-ng-coverage-closeout.md` and remove `[TODO-11]` from `todo/TODO.md`.

## Latest coverage measurement (May 2026)

Measured via `./scripts/coverage.py` against `juneau-utest/target/jacoco.exec`.

| Package / Module | Branches | Instructions |
|---|---|---|
| `org.apache.juneau.ng.http` (in `juneau-rest-common`) | 20% | **38%** |
| `org.apache.juneau.ng.rest.client` (in `juneau-rest-client`) | 89% | 94% |
| `org.apache.juneau.ng.rest.mock` (in `juneau-rest-mock`) | 98% | 96% |
| Apache HC 4.5 transport (`juneau-ng-rest-client-apache-httpclient-45`) | 71% | 81% |
| Apache HC 5 transport (`juneau-ng-rest-client-apache-httpclient-50`) | 79% | 85% |
| JDK `HttpClient` transport (`juneau-ng-rest-client-java-httpclient`) | 100% | 78% |
| OkHttp transport (`juneau-ng-rest-client-okhttp`) | 72% | 88% |
| Jetty transport (`juneau-ng-rest-client-jetty`) | 75% | 77% |

## Out of scope

- Promoting the NG stack from beta to stable.
- Deprecating or removing the classic `RestClient` / `juneau-rest-common`.
