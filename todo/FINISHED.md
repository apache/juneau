# FINISHED

Completion log for the Juneau 9.5.0 development cycle. New entries are added at the **top** of the "Recent completions" section as TODOs land; older entries are summarized in the compact index below. Each entry points to its detailed `FINISHED-<id>-<slug>.md` archive file in this directory.

The live work plan is in [`TODO.md`](TODO.md). Items are moved here when they land (and removed from `TODO.md`) — never marked `[x]` or struck-through in place.

---

## Recent completions (9.5.0)

In completion order, oldest-to-newest within the foundations + mixin family + recent feature work that drove the 9.5.0 cycle's headline changes.

### Foundations + mixin family

1. **TODO-73** — Runtime-overridable `@Rest(paths=...)`. Foundational; unblocked TODO-74–78. See [`FINISHED-73-rest-paths-runtime-override.md`](FINISHED-73-rest-paths-runtime-override.md).
2. **TODO-81** — Mixin sub-`RestContext` inheritance. Promoted mixin classes to embedded sub-resources with their own `RestContext` parent-linked to the host. Lazy initialization, annotation inheritance walks, `noInherit` semantics, dual-firing lifecycle hooks. See [`FINISHED-81-mixin-sub-context-inheritance.md`](FINISHED-81-mixin-sub-context-inheritance.md).
3. **TODO-74** — API-docs mixin pack (Swagger, Swagger-UI, OpenAPI, Redoc). Introduced `BasicSwaggerResource`, `BasicSwaggerUiResource`, `BasicOpenApiResource`, `BasicRedocResource` + `@OpSwagger(ignore=true)`. 100% test coverage, real-container (Jetty, Spring Boot) parity tests. See [`FINISHED-74-mixin-api-docs.md`](FINISHED-74-mixin-api-docs.md).
4. **TODO-75** — Static-files mixin (`BasicStaticFilesResource`). HEAD support, OpenAPI spec exclusion, `HttpResourceProcessor` enhancement for HEAD requests. 100% coverage. See [`FINISHED-75-mixin-static-files.md`](FINISHED-75-mixin-static-files.md).
5. **TODO-76** — Convention-endpoints pack (`BasicFaviconResource`, `BasicSeoResource`, `BasicVersionResource`, `BasicWellKnownResource`). `RestContext.buildMixinContext` retune for `BeanStore`-first mixin lookup. 95% branch coverage. See [`FINISHED-76-mixin-convention-endpoints.md`](FINISHED-76-mixin-convention-endpoints.md).
6. **TODO-77** — Ops/introspection pack (`BasicEchoResource`, `BasicAdminResource`, `BasicRouteIndexResource`). Introduced `org.apache.juneau.rest.guard.DenyAllGuard` as secure-by-default placeholder for `BasicAdminResource`. 88% branch coverage. See [`FINISHED-77-mixin-ops-introspection.md`](FINISHED-77-mixin-ops-introspection.md).

### Phase A — auth

7. **TODO-69** — AuthN guards + isolated `juneau-rest-server-jwt` sub-module. `BearerTokenGuard`, `ApiKeyGuard`, `TokenValidator` SPI, `ApiKeyStore` SPI, `@Auth Principal` arg-resolver, `AuthenticationException`, `ClaimsPrincipal`. `nimbus-jose-jwt` in dedicated module (`provided` scope) to avoid dependency bleed. Secure defaults for JWKS TTL, algorithm allowlist, mandatory claims. See [`FINISHED-69-authn-guards-jwt-apikey.md`](FINISHED-69-authn-guards-jwt-apikey.md).

### Phase B — debug rethink

8. **TODO-20** — Rest debug rethink. Collapsed five `@Rest`/`@RestOp` debug attributes + `DebugEnablement` + parallel `CallLogger` rule lists into a single `DebugConfig` bean + a typed `@Debug` annotation. **Hard break** (no deprecation cycle; migration notes in `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md`). **Source-of-truth pattern** — `@Debug` nested as `@Rest(debug=@Debug(...))` and `@RestOp(debug=@Debug(...))` so the `@Rest`/`@RestOp` annotation is the canonical capability surface. See [`FINISHED-20-rest-debug-rethink.md`](FINISHED-20-rest-debug-rethink.md).

---

## Earlier completions (9.5.0 cycle, compact index)

Plans landed earlier in the 9.5.0 development cycle. One-line summary per entry; the linked archive file is the source of truth for design context, decisions, and migration notes.

- **TODO-4** — Duration format control. See [`FINISHED-4-duration-format-control.md`](FINISHED-4-duration-format-control.md).
- **TODO-5** — Bean runtime types moved to `juneau-commons`. See [`FINISHED-5-bean-runtime-types-to-commons.md`](FINISHED-5-bean-runtime-types-to-commons.md).
- **TODO-6** — AI short-description scaffolding. See [`FINISHED-6-ai-short-description.md`](FINISHED-6-ai-short-description.md).
- **TODO-7** — Decouple `juneau-rest-common` from `juneau-marshall`. See [`FINISHED-7-decouple-rest-common-from-marshall.md`](FINISHED-7-decouple-rest-common-from-marshall.md).
- **TODO-8** — JSON Schema bean generation. See [`FINISHED-8-jsonschema-bean-generation.md`](FINISHED-8-jsonschema-bean-generation.md).
- **TODO-9** — Markdown remaining issues cleanup. See [`FINISHED-9-markdown-remaining-issues.md`](FINISHED-9-markdown-remaining-issues.md).
- **TODO-10** — Move `@Http` annotation to `juneau-rest-common`. See [`FINISHED-10-move-http-annotation-to-rest-common.md`](FINISHED-10-move-http-annotation-to-rest-common.md).
- **TODO-11a** — RestClient NG design plan. See [`FINISHED-11a-restclient-ng-design-plan.md`](FINISHED-11a-restclient-ng-design-plan.md).
- **TODO-11b** — RestClient NG coverage closeout. See [`FINISHED-11b-restclient-ng-coverage-closeout.md`](FINISHED-11b-restclient-ng-coverage-closeout.md).
- **TODO-12** — Schema validation. See [`FINISHED-12-schema-validation.md`](FINISHED-12-schema-validation.md).
- **TODO-13** — System-properties → `Settings` conversion. See [`FINISHED-13-system-properties-to-settings-conversion.md`](FINISHED-13-system-properties-to-settings-conversion.md).
- **TODO-14** — `BeanPropertyMeta` map-key coercion. See [`FINISHED-14-beanpropertymeta-map-key-coercion.md`](FINISHED-14-beanpropertymeta-map-key-coercion.md).
- **TODO-15** — Replace `BasicBeanStore` with v2. See [`FINISHED-15-replace-basicbeanstore-with-v2.md`](FINISHED-15-replace-basicbeanstore-with-v2.md).
- **TODO-16** — `@Bean` property-meta inventory + builder deletion (multi-phase). See [`FINISHED-16.md`](FINISHED-16.md), [`FINISHED-16a-per-setting-inventory.md`](FINISHED-16a-per-setting-inventory.md), [`FINISHED-16b-phases-1-2-execution.md`](FINISHED-16b-phases-1-2-execution.md), [`FINISHED-16c-phase-3-builder-deletion.md`](FINISHED-16c-phase-3-builder-deletion.md).
- **TODO-17** — 9.2.x → 9.5.0 migration-guide audit. See [`FINISHED-17-9.2.x-9.5.0-migration-guide-audit.md`](FINISHED-17-9.2.x-9.5.0-migration-guide-audit.md).
- **TODO-18** — REST-server feature brainstorm (catalogued follow-on TODOs). See [`FINISHED-18-rest-server-feature-brainstorm.md`](FINISHED-18-rest-server-feature-brainstorm.md).
- **TODO-21a** — `@Bean` / `@Inject` annotation rename. See [`FINISHED-21-bean-inject-annotation-rename.md`](FINISHED-21-bean-inject-annotation-rename.md).
- **TODO-21b** — `@Bean` annotations moved to the `inject` package. See [`FINISHED-21-bean-annotations-inject-package.md`](FINISHED-21-bean-annotations-inject-package.md).
- **TODO-22** — SVL external config vars. See [`FINISHED-22-svl-external-config-vars.md`](FINISHED-22-svl-external-config-vars.md).
- **TODO-24** — JSR-330 + Spring-lite support. See [`FINISHED-24-jsr330-and-spring-lite-support.md`](FINISHED-24-jsr330-and-spring-lite-support.md).
- **TODO-25** — Revisit `RestContext` memoizer migration. See [`FINISHED-25-revisit-rest-context-memoizer-migration.md`](FINISHED-25-revisit-rest-context-memoizer-migration.md).
- **TODO-26** — Replace `BeanBuilder` with `BeanInstantiator`. See [`FINISHED-26-replace-beanbuilder-with-beaninstantiator.md`](FINISHED-26-replace-beanbuilder-with-beaninstantiator.md).
- **TODO-29** — See [`FINISHED-29.md`](FINISHED-29.md).
- **TODO-30** — `ClassMeta` moved to `juneau-commons`. See [`FINISHED-30-classmeta-to-commons.md`](FINISHED-30-classmeta-to-commons.md).
- **TODO-31** — Inject-aware Microservice. See [`FINISHED-31-inject-aware-microservice.md`](FINISHED-31-inject-aware-microservice.md).
- **TODO-33** — Dynamic REST children. See [`FINISHED-33-dynamic-rest-children.md`](FINISHED-33-dynamic-rest-children.md).
- **TODO-34** — `MarshalledMap` / `MarshalledList`. See [`FINISHED-34-marshalledmap-marshalledlist.md`](FINISHED-34-marshalledmap-marshalledlist.md).
- **TODO-35** — `@TestBean` + `JuneauBeanStoreExtension` (test-time bean injection). See [`FINISHED-35-beanstore-test-injection.md`](FINISHED-35-beanstore-test-injection.md).
- **TODO-36** — Jetty as a bean. See [`FINISHED-36-jetty-as-bean.md`](FINISHED-36-jetty-as-bean.md).
- **TODO-38** — Rename `juneau-rest-client` to `juneau-rest-client-classic`. See [`FINISHED-38-rename-rest-client-to-classic.md`](FINISHED-38-rename-rest-client-to-classic.md).
- **TODO-39** — `/sonarqube` command + `scripts/sonarqube.py`. See [`FINISHED-39-sonarqube-command.md`](FINISHED-39-sonarqube-command.md).
- **TODO-40** — Remove HttpClient 4.5 from `juneau-rest-common` + `juneau-rest-server`. See [`FINISHED-40-remove-hc45-from-rest-common-and-server.md`](FINISHED-40-remove-hc45-from-rest-common-and-server.md).
- **TODO-41** — Merge Java HttpClient default transport. See [`FINISHED-41-merge-java-httpclient-default-transport.md`](FINISHED-41-merge-java-httpclient-default-transport.md).
- **TODO-42** — Split `juneau-rest-common` / classic. See [`FINISHED-42-split-rest-common-classic.md`](FINISHED-42-split-rest-common-classic.md).
- **TODO-45** — `juneau-bean-rfc7807`. See [`FINISHED-45-juneau-bean-rfc7807.md`](FINISHED-45-juneau-bean-rfc7807.md).
- **TODO-46** — `juneau-marshall-sse` module. See [`FINISHED-46-juneau-marshall-sse.md`](FINISHED-46-juneau-marshall-sse.md).
- **TODO-47** — Additional bean modules. See [`FINISHED-47-additional-bean-modules.md`](FINISHED-47-additional-bean-modules.md).
- **TODO-48** — Empty-return marshalled collections. See [`FINISHED-48-empty-return-marshalled-collections.md`](FINISHED-48-empty-return-marshalled-collections.md).
- **TODO-49** — `SchemaUtils` null returns. See [`FINISHED-49-schemautils-null-returns.md`](FINISHED-49-schemautils-null-returns.md).
- **TODO-50** — Format-control extension. See [`FINISHED-50-format-control-extension.md`](FINISHED-50-format-control-extension.md).
- **TODO-54** — Format-control round 2. See [`FINISHED-54-format-control-round-2.md`](FINISHED-54-format-control-round-2.md).
- **TODO-56** — Serializer/parser dispatch cleanup. See [`FINISHED-56-serializer-parser-dispatch-cleanup.md`](FINISHED-56-serializer-parser-dispatch-cleanup.md).
- **TODO-57** — Format round-trip tests. See [`FINISHED-57-format-round-trip-tests.md`](FINISHED-57-format-round-trip-tests.md).
- **TODO-58** — `BeanMap` typed set-element coercion. See [`FINISHED-58-beanmap-typed-set-element-coercion.md`](FINISHED-58-beanmap-typed-set-element-coercion.md).
- **TODO-59** — `BeanMap` abstract-collection default type. See [`FINISHED-59-beanmap-abstract-collection-default-type.md`](FINISHED-59-beanmap-abstract-collection-default-type.md).
- **TODO-61** — RFC 7807 server-side wiring. See [`FINISHED-61-rfc7807-server-side-wiring.md`](FINISHED-61-rfc7807-server-side-wiring.md).
- **TODO-62** — SSE server helpers. See [`FINISHED-62-sse-server-helpers.md`](FINISHED-62-sse-server-helpers.md).
- **TODO-63** — OpenAPI 3.1 emission. See [`FINISHED-63-openapi-3.1-emission.md`](FINISHED-63-openapi-3.1-emission.md).
- **TODO-64** — ETag / conditional-GET helpers. See [`FINISHED-64-etag-conditional-get-helpers.md`](FINISHED-64-etag-conditional-get-helpers.md).
- **TODO-65** — Health / readiness / liveness probes. See [`FINISHED-65-health-readiness-liveness-probes.md`](FINISHED-65-health-readiness-liveness-probes.md).
- **TODO-66** — Rate limit + request-id. See [`FINISHED-66-rate-limit-and-request-id.md`](FINISHED-66-rate-limit-and-request-id.md).
- **TODO-72** — REST mixins + `@Rest(paths=...)` prelude. See [`FINISHED-72-rest-mixins-and-paths.md`](FINISHED-72-rest-mixins-and-paths.md).

---

## Conventions

- **Add an entry here when the work lands.** Workflow: when a TODO is finished, archive its plan to `todo/FINISHED-<id>-<slug>.md` (per the existing `/todo cleanup` flow), remove the bullet from `TODO.md`, and add a new entry to the **top** of "Recent completions" above with a one-paragraph summary + link to the archive file.
- **Promote rich entries to the compact index over time.** As "Recent completions" grows beyond ~10–15 entries, move the oldest ones into "Earlier completions" with one-line summaries so the recent section stays focused on the current dev cycle's headline work.
- **Never re-introduce a finished bullet in `TODO.md`.** Re-opening work means filing a new TODO with a fresh id (and back-pointer to the finished entry here).
