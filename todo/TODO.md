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

- [TODO-34] Rename `JsonMap`/`JsonList` into `MarshalledMap`/`MarshalledList` and implement new `XMap`/`XList` variants for all supported marshalled languages. See `todo/TODO-34-marshalledmap-marshalledlist.md`.

- [TODO-35] Add support for overriding injected beans for tests (test-time replacement/override of beans in the inject/DI container so tests can swap real beans for stubs/mocks).

- [TODO-36] Rework `juneau-microservice-jetty` as a predefined `@Configuration` bean on top of a single `Microservice` class (rename `juneau-microservice-core` → `juneau-microservice`, delete `JettyMicroservice`, drive Jetty lifecycle through a fanned-out `MicroserviceListener`). See `todo/TODO-36-jetty-as-bean.md`.

- [TODO-38] Rename `juneau-rest-client` → `juneau-rest-client-classic` and promote the `juneau-ng-rest-client-*` family + `org.apache.juneau.ng.*` packages to the canonical `juneau-rest-client` / `org.apache.juneau.*` names. See `todo/TODO-38-rename-rest-client-to-classic.md`.

