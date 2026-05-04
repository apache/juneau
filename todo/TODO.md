# TODO


- [TODO-1] Update REST server API to use new BeanStore2.

- [TODO-2] On RestClient when logging with FULL, calling RestResponse.getContent().asString() causes a stream closed exception.

- [TODO-3] Possibility of adding convenience classes for okhttp3.mockwebserver.Dispatcher?

- [TODO-4] Duration.ofDays(7) serialized in hours?

- [TODO-6] Add an `ai` / `shortDescription` field to `@Schema` (and related annotations) for concise LLM/AI-consumption descriptions that stay under token budgets. See `todo/TODO-6-ai-short-description.md`.

- [TODO-7] Decouple `juneau-rest-common` from `juneau-marshall` by breaking the compile dependency so REST annotations and beans can be used without pulling in the full serialization stack. See `todo/TODO-7-decouple-rest-common-from-marshall.md`.

- [TODO-8] Add typed `JsonSchema` bean output to `JsonSchemaGenerator` (currently returns only `JsonMap`). Requires filling gaps in the `JsonSchema` bean and adding a bridge class in `juneau-bean-jsonschema`. See `todo/TODO-8-jsonschema-bean-generation.md`.

- [TODO-9] Fix remaining skipped Markdown round-trip test cases (tables, nested structures, edge cases). See `todo/TODO-9-markdown-remaining-issues.md`.

- [TODO-10] Move `org.apache.juneau.http.annotation` from `juneau-marshall` into `juneau-rest-common` (already done for the annotation classes — plan tracks remaining follow-on cleanup). See `todo/TODO-10-move-http-annotation-to-rest-common.md`.

- [TODO-11] Next-generation RestClient transport abstraction: decouple `RestClient` from Apache HttpClient 4.5 so any HTTP transport can be plugged in. See `todo/TODO-11-restclient2-transport-abstraction.md`.

- [TODO-12] Schema validation mode for parsers and serializers: wire `@Schema` validation into the bean property get/set lifecycle gated by a new `validateSchema` flag on `BeanContext`. See `todo/TODO-12-schema-validation.md`.

- [TODO-13] Convert Juneau system properties to the `Settings` class in `juneau-commons`. See `todo/TODO-13-system-properties-to-settings-conversion.md`.

- [TODO-14] Move SVL (`org.apache.juneau.svl`) from `juneau-marshall` into `juneau-commons` so `VarResolver` can be used without the full marshall dependency. See `todo/TODO-14-move-svl-to-commons.md`.

- [TODO-15] Replace `BasicBeanStore` / `BeanCreator` with the v2 equivalents in `juneau-commons.inject`, then drop the `2` suffix and remove the legacy classes. See `todo/TODO-15-replace-basicbeanstore-with-v2.md`.

- [TODO-17] Audit 9.2.x changes (juneau-docs release notes 9.2.0 / 9.5.0 + git history since 9.1.0) for breaking changes and populate the v9.5 Migration Guide at juneau-docs/pages/topics/23.01.V9.5-migration-guide.md with Old→New rows for each. Focus on removed APIs, renamed annotations/classes/methods, changed default behaviors, and any annotation-attribute semantics changes.

- [TODO-18] Investigate possible useful features to add to juneau-rest-server.

- [TODO-19] Remove encoders support from juneau-marshall and juneau-rest-server.

- [TODO-20] Rethink how debugging works in RestServlet.  Can we come up with a simpler system?

- [TODO-21] Rename and relocate bean/inject annotations: reduce confusion with Spring naming, align annotation vocabulary with what each actually does, and move resource/store contribution annotations into `org.apache.juneau.commons.inject`. See `todo/TODO-21-bean-annotations-inject-package.md`.

- [TODO-23] New feature support in org.apache.juneau.commons.inject — roadmap for a simplified inject API (not a Spring replacement). See `todo/TODO-23-commons-inject-framework-roadmap.md`.

- [TODO-24] JSR-330 alignment (no `jakarta.inject-api` dependency) + selective Spring-lite features for `commons.inject`. See `todo/TODO-24-jsr330-and-spring-lite-support.md`.
