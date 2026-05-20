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

- [TODO-43] - Rename ofText() methods to ofString().

- [TODO-44] Fix invalid MIME type `octal/msgpack` in `MsgPackSerializer`/`MsgPackParser`. `octal` is not a valid MIME top-level type per RFC 6838; the RFC-standard value is `application/msgpack`. Switch `produces(...)` / `consumes(...)` to `application/msgpack` and keep `octal/msgpack` as a parser-side alias so existing wire values still decode. The `ContentType.APPLICATION_MSGPACK` constant already exists.

- [TODO-45] Add `juneau-bean-rfc7807` module with bean types for `application/problem+json` (RFC 7807 problem details). Five-field structure (`type`, `title`, `status`, `detail`, `instance`) plus open-ended extensions. Pairs naturally with `BasicHttpException` / `BasicHttpResponse`. See `todo/TODO-45-juneau-bean-rfc7807.md`.

- [TODO-46] Add `juneau-marshall-sse` module — Server-Sent Events serializer/parser for `text/event-stream`. Distinct framing format (`event:`, `data:`, `id:`, `retry:`) per WHATWG. Would integrate with `juneau-rest-server` and `juneau-rest-client` to unblock streaming APIs. See `todo/TODO-46-juneau-marshall-sse.md`.

- [TODO-47] Add bean modules for additional JSON-based REST formats: `juneau-bean-hal` (`application/hal+json`), `juneau-bean-jsonapi` (`application/vnd.api+json`), `juneau-bean-jsonpatch` (`application/json-patch+json`). All three are just JSON dialects with documented schemas — no new marshaller, just typed beans. See `todo/TODO-47-additional-bean-modules.md`.
