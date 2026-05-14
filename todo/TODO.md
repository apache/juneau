# TODO


- [TODO-2] On RestClient when logging with FULL, calling RestResponse.getContent().asString() causes a stream closed exception.

- [TODO-3] Possibility of adding convenience classes for okhttp3.mockwebserver.Dispatcher?

- [TODO-4] Duration.ofDays(7) serialized in hours?

- [TODO-6] Add an `ai` / `shortDescription` field to `@Schema` (and related annotations) for concise LLM/AI-consumption descriptions that stay under token budgets. See `todo/TODO-6-ai-short-description.md`.

- [TODO-7] Decouple `juneau-rest-common` from `juneau-marshall` by breaking the compile dependency so REST annotations and beans can be used without pulling in the full serialization stack. See `todo/TODO-7-decouple-rest-common-from-marshall.md`.

- [TODO-8] Add typed `JsonSchema` bean output to `JsonSchemaGenerator` (currently returns only `JsonMap`). Requires filling gaps in the `JsonSchema` bean and adding a bridge class in `juneau-bean-jsonschema`. See `todo/TODO-8-jsonschema-bean-generation.md`.

- [TODO-9] Fix remaining skipped Markdown round-trip test cases (tables, nested structures, edge cases). See `todo/TODO-9-markdown-remaining-issues.md`.

- [TODO-10] Move `org.apache.juneau.http.annotation` from `juneau-marshall` into `juneau-rest-common` (already done for the annotation classes — plan tracks remaining follow-on cleanup). See `todo/TODO-10-move-http-annotation-to-rest-common.md`.

- [TODO-11] Next-generation RestClient transport abstraction: decouple `RestClient` from Apache HttpClient 4.5 so any HTTP transport can be plugged in. See `todo/TODO-11-restclient2-transport-abstraction.md`.

- [TODO-12] Schema validation mode for parsers and serializers: wire `@Schema` validation into the bean property get/set lifecycle gated by a new `validateSchema` flag on `MarshallingContext`. See `todo/TODO-12-schema-validation.md`.

- [TODO-17] Audit 9.2.x changes (juneau-docs release notes 9.2.0 / 9.5.0 + git history since 9.1.0) for breaking changes and populate the v9.5 Migration Guide at juneau-docs/pages/topics/23.01.V9.5-migration-guide.md with Old→New rows for each. Focus on removed APIs, renamed annotations/classes/methods, changed default behaviors, and any annotation-attribute semantics changes.

- [TODO-18] Investigate possible useful features to add to juneau-rest-server.

- [TODO-20] Rethink how debugging works in RestServlet.  Can we come up with a simpler system?

- [TODO-22] Add additional SVL vars for common external configuration sources (e.g. `EnvFileVar` / `DotenvVar` for `.env` files, possibly file-glob and JSON-pointer vars). Should land after TODO-14 places SVL in `juneau-commons`.

- [TODO-30] Investigate moving `ClassMeta` and related non-marshalling type metadata from `juneau-marshall` into `juneau-commons` (analysis/feasibility pass). See `todo/TODO-30-classmeta-to-commons.md`.

