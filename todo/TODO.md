# TODO


- [TODO-1] Update REST server API to use new BeanStore2.

- [TODO-2] On RestClient when logging with FULL, calling RestResponse.getContent().asString() causes a stream closed exception.
- [TODO-3] Possibility of adding convenience classes for okhttp3.mockwebserver.Dispatcher?

- [TODO-4] Duration.ofDays(7) serialized in hours?

- [TODO-16] Completed. See `todo/FINISHED-16.md` (consolidated closeout) and `todo/FINISHED-16a-per-setting-inventory.md`, `todo/FINISHED-16b-phases-1-2-execution.md`, `todo/FINISHED-16c-phase-3-builder-deletion.md`.

- [TODO-17] Audit 9.2.x changes (juneau-docs release notes 9.2.0 / 9.2.1 + git history since 9.1.0) for breaking changes and populate the v9.5 Migration Guide at juneau-docs/pages/topics/23.01.V9.5-migration-guide.md with Old→New rows for each. Focus on removed APIs, renamed annotations/classes/methods, changed default behaviors, and any annotation-attribute semantics changes.

- [TODO-18] Investigate possible useful features to add to juneau-rest-server.

- [TODO-19] Remove encoders support from juneau-marshall and juneau-rest-server.

- [TODO-20] Rethink how debugging works in RestServlet.  Can we come up with a simpler system?

- [TODO-22] In RestServlet, consider renaming simpleVarResolver->bootstrapVarResolver and rootBeanStore->bootstrapBeanStore.

- [TODO-23] New feature support in org.apache.juneau.commons.inject — roadmap for a simplified inject API (not a Spring replacement). See `todo/TODO-23-commons-inject-framework-roadmap.md`.

- [TODO-24] JSR-330 alignment (no `jakarta.inject-api` dependency) + selective Spring-lite features for `commons.inject`. See `todo/TODO-24-jsr330-and-spring-lite-support.md`.

- [TODO-25] Update the versions in our pom files to 9.5.0-SNAPSHOT.

- [TODO-26] Personal Slack bot + Python CLI for Cursor agent notifications / Q&A (per-channel, optional poll for answers). See `todo/TODO-26-cursor-slack-agent-alerts.md`.

- [TODO-29] Swap the sorted/unsorted default across all serializers so that sorted output is the default behavior and unsorted is the opt-in. Once that is done, replace the json()/json(String,Class) helpers in TestUtils with MarshallUtils equivalents (currently they go through Json5.DEFAULT_SORTED, which will become just json5() after the default flips).
