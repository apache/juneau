# TODO-63: OpenAPI 3.1 emission + bundled Swagger UI / Redoc auto-mount

Source: split out of TODO-18 brainstorm on 2026-05-22 (the #3 pick).

## Goal

Bring the unused `juneau-bean-openapi-v3` module online server-side. `BasicSwaggerProvider` only emits Swagger v2 today; the bean module has 33 fully-populated source files (Operation, PathItem, Components, SchemaInfo, …) sitting waiting for a generator. Land:

- A new `OpenApiProvider` SPI (sibling of `SwaggerProvider`) that emits an OpenAPI 3.1 document from the same `@Rest` / `@RestOp` metadata.
- A `BasicOpenApiProvider` implementation that walks the `RestContext` and produces an `OpenApi` bean, serialized via the existing `JsonSerializer.DEFAULT`.
- A bundled Swagger UI (or Redoc) static-resource mount, auto-attached when `@Rest(openapi=true)` is set, exposing the spec at `/openapi.json` and the UI at `/openapi/ui`.

End-state developer experience:

```java
@Rest(path="/petstore", openapi=true)
public class PetStoreResource extends BasicRestObjectGroup {
    // ... @RestGet / @RestPost methods
}
// → GET /petstore/openapi.json  → application/json (OpenAPI 3.1 doc)
// → GET /petstore/openapi/ui    → text/html         (Swagger UI)
```

## Why now

- `juneau-bean-openapi-v3` shipped fully built (TODO-47 cluster delivered HAL / JSON:API / JSON Patch but the OpenAPI v3 bean module pre-dates and is already in tree). 33 source files, full builders, full tests.
- Swagger v2 is **end-of-life** for the broader ecosystem (the OpenAPI Initiative stopped maintaining it in 2021); every modern API gateway, code-gen tool, and AI-assist plugin expects OpenAPI 3.x.
- `BasicSwaggerProvider` is a clean clonable pattern — the new provider follows the same shape.
- Dynamic child mount (TODO-33) makes auto-mounting the UI resource trivial — the spec endpoint + UI endpoint become two `addChild(...)` calls during context build.

## Scope

**In scope (v1):**

- New SPI `org.apache.juneau.rest.openapi.OpenApiProvider` (interface), `BasicOpenApiProvider` (default impl), `BasicOpenApiProviderSession` (per-call session) under `juneau-rest-server`. Cloned from `SwaggerProvider` / `BasicSwaggerProvider` / `BasicSwaggerProviderSession`.
- Generator maps `@Rest` → `OpenApi`, `@RestOp` → `Operation`, `RequestBeanMeta` → `Parameter` / `RequestBodyInfo`, `ResponseBeanMeta` → `Response`, request/response bean class → `SchemaInfo` (via `JsonSchemaGenerator`).
- `openapi` attribute on `@Rest` (and per-op on `@RestGet`/`@RestPost`/etc): when `true`, the provider auto-emits the spec doc at `/openapi.json` and (optionally) the UI at `/openapi/ui`.
- Bundled Swagger UI static resources (HTML + CSS + JS) added under `juneau-rest-server/src/main/resources/htdocs/openapi-ui/`. Pulled from the upstream `swagger-ui-dist` npm package and committed as static files (do not add a build-time dep on `swagger-ui-dist`). Mounted via `BasicStaticFiles`. **Locked decision needed: Swagger UI vs Redoc vs both** — see Open Question #1.
- Tests in `juneau-utest`: `OpenApiProvider_Test` (generator), `Rest_OpenApi_Annotation_Test` (end-to-end with `MockRestClient` — assert `/openapi.json` returns a parseable OpenAPI 3.1 doc).
- Release-notes entry under `### juneau-rest-server`; new topic page (`pages/topics/14.10.RestServerOpenApi.md` or sibling slot).

**Explicitly out of scope (v1):**

- Removing or deprecating the existing `BasicSwaggerProvider` (Swagger v2). Both providers coexist; Swagger v2 stays as the default for backwards compatibility.
- OpenAPI 3.1 client code-gen (a code-gen tool that consumes the spec and produces `@Remote`-annotated interfaces). Worth doing eventually; orthogonal scope.
- Schema dialect bridging beyond what `JsonSchemaGenerator` already supports. OpenAPI 3.1 aligns with JSON Schema 2020-12; `JsonSchemaGenerator` emits draft-04-ish — accept lossy mapping in v1 and document the gap.
- `webhooks` and `callbacks` sections of OpenAPI 3.1 — out for v1; emit empty.
- Server `Components.examples` / `Components.parameters` *reuse* (DRY-via-references). v1 emits inline; reference deduplication is a later optimization.

## Phased steps

### Phase 0 — clone the seam (read-only)

1. Re-read `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/swagger/{SwaggerProvider,BasicSwaggerProvider,BasicSwaggerProviderSession}.java` to capture the SPI shape, builder pattern, and bean-store wiring.
2. Re-read `juneau-bean/juneau-bean-openapi-v3/src/main/java/org/apache/juneau/bean/openapi3/OpenApi.java` (and the top-level builder `OpenApiBuilder`) to confirm the target type's shape.
3. Inventory the `JsonSchemaGenerator` API and identify the smallest possible glue from `BeanInfo` / `ClassMeta` to `OpenApi.SchemaInfo`.
4. Decide UI choice (Swagger UI vs Redoc) — see Open Question #1.

### Phase 1 — `OpenApiProvider` SPI + `BasicOpenApiProvider`

1. New package `org.apache.juneau.rest.openapi` in `juneau-rest-server`. Add the three classes (`OpenApiProvider`, `BasicOpenApiProvider`, `BasicOpenApiProviderSession`) mirroring the Swagger v2 trio.
2. Register `OpenApiProvider` as a default-supplier-backed bean alongside `SwaggerProvider` in `RestContext.createBeanStore(...)`.
3. Implement the generator. Sources, in order:
   - `@Rest(title, description, version, license, externalDocs, tags, servers)` → `OpenApi.Info` + top-level fields.
   - `@RestGet/@RestPost/etc` → `OpenApi.PathItem.Operation` entries.
   - `@Path` / `@Query` / `@Header` / `@FormData` → `Operation.parameters[]` via `RequestBeanMeta`.
   - `@Content` (request body) → `Operation.requestBody` via `RequestBeanMeta` and `JsonSchemaGenerator`.
   - Return type → `Operation.responses["200"].content[mediaType].schema` via `ResponseBeanMeta` + `JsonSchemaGenerator`.
   - Status-code annotations + `@Response(code=...)` → additional `Operation.responses` entries.
4. Tests:
   - `OpenApiProvider_Test` (unit) — given a synthetic `RestContext` with a couple of operations, the emitted `OpenApi` round-trips through `JsonSerializer.DEFAULT` / `JsonParser.DEFAULT` and contains the expected `Path/Operation/Parameter/Response` shape.
   - `OpenApiSchemaMapping_Test` — bean shapes (primitives, nested, arrays, Maps, Optionals) map to expected `SchemaInfo` shapes.

### Phase 2 — annotation + spec/UI mount

1. Add `openapi` boolean attribute on `@Rest`. When `true`:
   - Build the `OpenApi` doc once at context-init time (cached); expose via a `RestChildren.addChild("/openapi.json", new OpenApiSpecResource(...))`-style synthetic child.
   - Build the UI static-files mount (`/openapi/ui`) — a `BasicStaticFiles` instance rooted at `htdocs/openapi-ui/`, with a template `index.html` whose `url` field points back at `/openapi.json`.
2. Tests:
   - `Rest_OpenApi_Annotation_Test` — end-to-end with `MockRestClient` — `GET /openapi.json` returns 200 + valid OpenAPI 3.1 JSON; `GET /openapi/ui` returns 200 + HTML; UI HTML contains the correct `url` reference.
3. Release-notes entry + topic page (`pages/topics/14.10.RestServerOpenApi.md`) + sidebar entry.

### Phase 3 — Redoc as alternative UI (optional, deferred)

1. Add a second static-files bundle `htdocs/openapi-redoc/` and a `@Rest(openapi=true, openapiUi=REDOC)` enum value.
2. Both UIs share the same `/openapi.json` source.

### Phase 4 — `Components.schemas` reuse (optional, deferred)

1. Detect bean-class repeated use across operations, lift the inline schema to `Components.schemas[BeanClassName]`, replace inline uses with `{ "$ref": "#/components/schemas/BeanClassName" }`.

## Acceptance criteria

- [ ] `BasicOpenApiProvider` emits an `OpenApi` bean that, when serialized to JSON, conforms to OpenAPI 3.1.0 (validated against the official schema; use the `OpenApi` bean's own round-trip as the smoke test in v1).
- [ ] All `@Rest` / `@RestOp` metadata that `BasicSwaggerProvider` extracts also appears in the `OpenApi` output (parity check).
- [ ] `@Rest(openapi=true)` end-to-end through `MockRestClient`: `GET /openapi.json` returns the spec; `GET /openapi/ui` returns Swagger UI; the UI's `url` reference resolves.
- [ ] Existing `BasicSwaggerProvider` Swagger v2 behavior unchanged when `openapi=false` (default).
- [ ] Coverage ≥ 85% on `BasicOpenApiProvider` + `BasicOpenApiProviderSession` (a touch lower than other bars; the generator has many switch-on-type branches that are exhaustively but lightly exercised). Bean-side `juneau-bean-openapi-v3` retains its existing 100% coverage.
- [ ] Release-notes entry + topic page + sidebar entry.
- [ ] Full `./scripts/test.py` green.

## Open questions (need user direction before Phase 1)

1. **UI bundle: Swagger UI vs Redoc vs both.** Recommend Swagger UI in v1 (more familiar, bigger ecosystem); Redoc as Phase 3. Alternative: ship both side-by-side from the start. Cost: ~2MB of static files committed per UI.
2. **Bundle distribution mechanism.** Commit the UI bundle as static files in `juneau-rest-server/src/main/resources/htdocs/openapi-ui/` (recommended — no build-time dep), or pull `swagger-ui-dist` at build time? Static-commit keeps the build hermetic and licensing explicit.
3. **Schema dialect mapping fidelity.** OpenAPI 3.1 uses JSON Schema 2020-12 (`$dynamicRef`, `unevaluatedProperties`, `prefixItems`, `if/then/else`). `JsonSchemaGenerator` emits draft-04. v1 accepts lossy mapping (document the gap) or invests in a 3.1-aware generator. Recommend lossy + document.
4. **One provider vs two.** Should the new `OpenApiProvider` *replace* `SwaggerProvider` (with Swagger v2 emission as a configurable mode) or live alongside it (recommended for v1)? Replacement is cleaner long-term but a larger break.
5. **Auto-mount paths.** `/openapi.json` + `/openapi/ui` (recommended) or configurable via `@Rest(openapiSpecPath=..., openapiUiPath=...)`? Recommend defaults + configurable attributes.
6. **YAML output.** OpenAPI specs are commonly served as YAML. Today Juneau has a YAML serializer (`juneau-marshall` `YamlSerializer`); serving `/openapi.yaml` is a one-line add. Recommend ship in v1.
7. **Versioning the spec endpoint.** Should `/openapi.json` include the OpenAPI version in the URL (`/openapi/v3.1/spec`)? Recommend no — single endpoint, version is in the doc.
8. **Coexistence with `SwaggerResource`.** Today `BasicRestObjectGroup` exposes Swagger v2 at the `?Swagger` query param. Should `?OpenApi` query param mirror this? Recommend yes — same convention.

## Risks

- **Generator complexity.** OpenAPI 3.1 has many edge cases (polymorphic schemas via `discriminator`, `oneOf`/`anyOf`/`allOf`, callbacks, links). v1 should aim for ~80% coverage of `@Rest`/`@RestOp` features and explicitly defer the rest.
- **Schema-dialect mismatch.** `JsonSchemaGenerator` emits draft-04-ish; OpenAPI 3.1 wants 2020-12. Spec-validators may flag the difference. Mitigation: document the gap; provide a config flag to opt out of the strict 3.1 dialect declaration if needed.
- **UI bundle staleness.** Swagger UI ships frequent releases. Committing the static bundle means manual updates. Mitigation: document a "how to refresh the UI bundle" runbook; pin the upstream version in a `VERSION.txt` in the resource dir.
- **Spec-doc cache invalidation.** The doc is built once at context-init. Dynamic child resources (TODO-33) added at runtime won't appear in the cached doc until next restart. Mitigation: a `RestChildren.onMutate` callback invalidates the cache; document the behavior.
- **Coupling with TODO-20 (Rest Debug Rethink).** Debug-mode info should not leak into OpenAPI output. Low risk — the providers are read-only over `RestContext` metadata.

## Related work

- `todo/FINISHED-47-additional-bean-modules.md` — landed HAL / JSON:API / JSON Patch bean modules; `juneau-bean-openapi-v3` (already in tree) is the next bean module to grow a generator on top of.
- `todo/FINISHED-33-dynamic-rest-children.md` — used to auto-mount `/openapi.json` + `/openapi/ui` at context-init time.
- `todo/FINISHED-31-inject-aware-microservice.md` — bean-store registration for `OpenApiProvider`.
- `todo/FINISHED-40-remove-hc45-from-rest-common-and-server.md` — clean retyping that the generator's parameter mapping rides on.
- Sibling: `BasicSwaggerProvider` in `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/swagger/` — the literal template for the new code.
