# TODO


- [TODO-2] On RestClient when logging with FULL, calling RestResponse.getContent().asString() causes a stream closed exception.

- [TODO-3] Possibility of adding convenience classes for okhttp3.mockwebserver.Dispatcher?

- [TODO-4] Duration.ofDays(7) serialized in hours?

- [TODO-7] Decouple `juneau-rest-common` from `juneau-marshall` by breaking the compile dependency so REST annotations and beans can be used without pulling in the full serialization stack. See `todo/TODO-7-decouple-rest-common-from-marshall.md`.

- [TODO-9] Fix remaining skipped Markdown round-trip test cases (tables, nested structures, edge cases). See `todo/TODO-9-markdown-remaining-issues.md`.

- [TODO-10] Move `org.apache.juneau.http.annotation` from `juneau-marshall` into `juneau-rest-common` (already done for the annotation classes — plan tracks remaining follow-on cleanup). See `todo/TODO-10-move-http-annotation-to-rest-common.md`.

- [TODO-17] Audit 9.2.x changes (juneau-docs release notes 9.2.0 / 9.5.0 + git history since 9.1.0) for breaking changes and populate the v9.5 Migration Guide at juneau-docs/pages/topics/23.01.V9.5-migration-guide.md with Old→New rows for each. Focus on removed APIs, renamed annotations/classes/methods, changed default behaviors, and any annotation-attribute semantics changes.

- [TODO-18] Investigate possible useful features to add to juneau-rest-server.

- [TODO-20] Rethink how debugging works in RestServlet.  Can we come up with a simpler system?

- [TODO-30] Investigate moving `ClassMeta` and related non-marshalling type metadata from `juneau-marshall` into `juneau-commons` (analysis/feasibility pass). See `todo/TODO-30-classmeta-to-commons.md`.

- [TODO-35] Add support for overriding injected beans for tests (test-time replacement/override of beans in the inject/DI container so tests can swap real beans for stubs/mocks).

- [TODO-39] Add a `/sonarqube` Cursor command (and matching `scripts/sonarqube.py` helper) that runs SonarQube analysis against a given source file, package, or module — analogous to `/coverage` / `scripts/coverage.py`. Should auto-detect the Maven module from the path, invoke the SonarQube/SonarLint scanner, and print a concise per-file summary of issues (rule id, severity, line, message) so the user can quickly triage Sonar findings the same way they do JaCoCo coverage.

- [TODO-40] Remove Apache HttpClient 4.5 (`org.apache.http.*`) references from `juneau-rest-common` and `juneau-rest-server`. The HC 4.5 dependency leaks into server-side public APIs (e.g. `RequestHeaders.add(org.apache.http.Header...)`, `RestRequest#getAllHeaders()` returning `org.apache.http.Header[]`, `HttpEntityProcessor` keyed on `org.apache.http.HttpEntity`, `BasicStaticFiles` taking `org.apache.http.Header[]`, `FluentRequestLineAssertion`/`FluentProtocolVersionAssertion` over `org.apache.http.RequestLine`/`org.apache.http.ProtocolVersion`). Migrate these to the transport-neutral Juneau types from `org.apache.juneau.http.*` (the canonical namespace freed up by TODO-38 once `ng.http.*` is promoted) so that pulling `juneau-rest-server` or `juneau-rest-common` no longer drags in `org.apache.httpcomponents:httpcore`. See `todo/TODO-40-remove-hc45-from-rest-common-and-server.md`.

- [TODO-41] juneau-rest-client should use JavaHttpTransport by default but with options to pull in different transports.
