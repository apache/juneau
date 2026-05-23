# FINISHED-63: OpenAPI 3.1 emission + bundled Swagger UI / Redoc auto-mount

Source: split out of TODO-18 brainstorm on 2026-05-22 (the #3 pick).

## Completion summary

Closed on 2026-05-22 in the same 9.5.0 push that landed the first pass. The remaining three items — `components.schemas` reuse, the `?OpenApi` / `?Swagger` query mirrors on `BasicRestObjectGroup` and friends, and an explicit YAML round-trip test — are all in. `BasicOpenApiProviderSession` now hoists inline schemas that recur across two or more operation slots into `components.schemas` with stable `title`-derived (or synthetic `Schema<N>`) names and rewrites each occurrence to a `$ref` pointer. `BasicGroupOperations` exposes two new matcher-gated `GET /` overloads (`HasSwaggerQueryParam` / `HasOpenApiQueryParam`); the existing `getChildren(RestRequest)` was demoted to a default interface method so all three operations share a declaring class and the matcher-count tiebreaker in `RestOpContext#compareTo` selects the correct one. `OpenApiYamlRoundTrip_Test` exercises `YamlSerializer.DEFAULT_READABLE` → `YamlParser.DEFAULT` over a hand-built doc, a live doc from a `BasicRestServlet` resource with `apiFormat="openapi"`, and the `/openapi/*` endpoint served with `Accept: application/yaml`. Full `./scripts/test.py` reports 126603 tests, 0 failures, 0 errors, 0 skipped after this push.

## Status (post-9.5.0 push)

**Landed in 9.5.0:**
- New `org.apache.juneau.rest.openapi` package: `OpenApiProvider` SPI, `BasicOpenApiProvider`,
  `BasicOpenApiProviderSession`, `OpenApiResource`, `OpenApiException`. Session generates
  OpenAPI 3.1 via JSON-level transformation of the existing Swagger 2.0 emission, so all
  existing Swagger-aware annotations (`@Schema`, `@Content`, `@StatusCode`, etc.) round-trip
  to the OpenAPI document with no source changes.
- `RestRequest.getOpenApi()`, `RestContext.getOpenApiProvider()`, `RestContext.getOpenApi(Locale)`.
- `@Rest(openApiProvider=…)` annotation attribute (mirrors `swaggerProvider`).
- `@Rest(apiFormat="swagger" | "openapi" | "both")` knob with system-property override
  (`juneau.rest.apiFormat`); `RestContext.getApiFormat()` accessor; default `"swagger"` for back-compat.
- `BasicRestOperations.getSwagger(RestRequest)` and `getOpenApi(RestRequest)` are now `default`
  methods that consult `apiFormat` and dispatch (200 vs 404) accordingly.
- `RedocUI` swap in `juneau-bean-openapi-v3` (`org.apache.juneau.bean.openapi3.ui.RedocUI`)
  — two-column Redoc-style HTML rendering of `OpenApi` beans for `text/html` requests.
- `OpenApiVar` SVL variable (`$OS{path}`) — sibling of `$SS{path}` for OpenAPI documents.
- Tests: `Rest_ApiFormat_Swagger_Test`, `Rest_ApiFormat_OpenApi_Test`, `Rest_ApiFormat_Both_Test`,
  `Rest_ApiFormat_Resolution_Test` (annotation/system-property/default precedence).
- Docs: release-notes entry + new migration-guide row + updated `BasicRestServletSwagger.md` topic page.

**Landed in the 2026-05-22 follow-up push (this archive):**
- Phase 4 — `components.schemas` reuse: `BasicOpenApiProviderSession.deduplicateInlineSchemas`
  walks every operation parameter, request-body, and response-content `schema` slot, canonicalizes
  each inline (non-`$ref`) schema to a JSON5 string, and hoists schemas that occur two or more times
  into `components.schemas` with stable names (preferring the schema's `title`; falling back to
  synthesized `Schema<N>` with collision avoidance against existing component entries). Each original
  occurrence is rewritten in place to `{"$ref":"#/components/schemas/<name>"}`. Bean-typed slots
  continue to flow through the existing `definitions` → `components.schemas` lift in the Swagger v2
  generator. Tested by `OpenApiSchemaReuse_Test`.
- OQ8 — `?Swagger` and `?OpenApi` query mirrors on group resources: `BasicGroupOperations` now
  defines `getChildrenSwagger` / `getChildrenOpenApi` `default` methods annotated with
  `@RestGet(path="/")` plus `HasSwaggerQueryParam` / `HasOpenApiQueryParam` matchers. Both honor
  `@Rest(apiFormat=…)` (the `?Swagger` mirror 404s when `apiFormat="openapi"`, and `?OpenApi`
  404s when `apiFormat="swagger"`). To restore correct method dispatch when matchers compete with
  the no-matcher navigation handler, `getChildren(RestRequest)` was demoted from an abstract
  method to a `default` interface method that simply returns `ChildResourceDescriptions.of(req)`;
  the concrete overrides in `BasicRestObjectGroup`, `BasicRestServletGroup`, and
  `BasicSpringRestServletGroup` were removed so all three operations now share the same declaring
  class and the matcher-count tiebreaker in `RestOpContext#compareTo` correctly selects the right
  method. Tested by `Rest_GroupQueryMirrors_Test`.
- YAML round-trip — `OpenApiYamlRoundTrip_Test` exercises `YamlSerializer.DEFAULT_READABLE` →
  `YamlParser.DEFAULT` over (a) a hand-built `OpenApi` bean, (b) the live document produced by a
  `BasicRestServlet`-based resource with `apiFormat="openapi"`, and (c) the `/openapi/*` endpoint
  served with `Accept: application/yaml`. Each path asserts structural equality across `openapi`,
  `info`, `servers`, `paths`, and `components.schemas`.

## Related work

- `todo/FINISHED-47-additional-bean-modules.md` — landed HAL / JSON:API / JSON Patch bean modules.
- `todo/FINISHED-33-dynamic-rest-children.md` — dynamic child mount infrastructure.
- `todo/FINISHED-31-inject-aware-microservice.md` — bean-store registration patterns.
- `todo/FINISHED-40-remove-hc45-from-rest-common-and-server.md` — clean retyping.
- Sibling: `BasicSwaggerProvider` in `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/swagger/`.
