# TODO-47: Additional bean modules for JSON-dialect REST formats

Source: filed 2026-05-19 (split out of TODO-40 follow-up discussion).

## Goal

Add typed-bean modules for three popular JSON-dialect REST formats. All three are just JSON with a documented schema — **no new marshaller is needed**, the existing `JsonSerializer` / `JsonParser` already handle the wire format. Each module just contributes typed beans + a few helper builders.

| Format | MIME | New module | `ContentType` constant |
|---|---|---|---|
| HAL hypermedia | `application/hal+json` | `juneau-bean-hal` | `ContentType.APPLICATION_HAL_JSON` |
| JSON:API | `application/vnd.api+json` | `juneau-bean-jsonapi` | `ContentType.APPLICATION_VND_API_JSON` |
| JSON Patch | `application/json-patch+json` | `juneau-bean-jsonpatch` | `ContentType.APPLICATION_JSON_PATCH` |

## Why bundle these together

- All three follow the **same recipe**: new Maven module under `juneau-bean/`, depends only on `juneau-marshall`, exports beans + builders + tests. No marshaller, no transport, no REST hooks.
- All three pair with a `ContentType` constant that already ships.
- Each one independently is too small to merit its own TODO entry on the front page, but together they're a coherent "modern REST formats" cluster.

This TODO is a **parent** — implementation can land as three separate PRs in any order. Mark each subtask done as the corresponding module ships.

## Subtask A — `juneau-bean-hal`

[HAL spec](https://stateless.group/hal_specification.html) (canonical field list per [draft-kelly-json-hal-08](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08)). Two structural fields on every resource:

- `_links`: `Map<String, HalLink | HalLinkArray>` — relation name → single link or array of links.
- `_embedded`: `Map<String, HalResource | HalResourceArray>` — relation name → single sub-resource or array of sub-resources.

Plus the resource's own arbitrary fields appear at the top level alongside `_links` / `_embedded`.

### Package layout

- Package: `org.apache.juneau.bean.hal`
- `package-info.java` (overview javadoc, mirrors `juneau-bean-jsonschema`'s style)
- `HalResource` — base bean; serializable directly or subclassable for typed payloads. Carries `_links`, `_embedded`, and extension fields.
- `HalLink` — link object with the 8 spec fields.
- `HalLinkArray extends LinkedList<HalLink>` — sibling of `JsonSchemaArray`; serializes as a top-level JSON array when used as a `Map` value.
- `HalResourceArray extends LinkedList<HalResource>` — same shape for `_embedded`.
- Custom `ObjectSwap`s (`HalLinkOrArraySwap`, `HalResourceOrArraySwap`) modeled on `JsonSchema.BooleanOrSchemaSwap` / `BooleanOrSchemaArraySwap` for parse-time single-vs-array disambiguation. **Applied to the `_links` / `_embedded` fields as field-level `@Swap(...)` annotations** (see "Deliverables" for the chosen field shape).
- Optional `HalBuilder` static-factory entry point (only if it materially shortens authoring code; the fluent setters on `HalResource` may be enough).

### `HalLink` fields (all 8 per spec)

`href` (required, URI/URI-template) · `templated` (boolean) · `type` (media-type hint) · `deprecation` (URI) · `name` · `profile` (URI) · `title` · `hreflang`.

### Deliverables

1. `HalResource` bean with three pieces of state, in priority order:
   - `_links` field, typed as `Map<String,HalLink>` and annotated **`@BeanProp("_links") @Swap(HalLinkOrArraySwap.class)`** at the field level. The swap is responsible for the single-vs-array union: on serialize, a single-element list collapses to a bare `HalLink` JSON object; on parse, a JSON array materializes as `HalLinkArray` and a JSON object as a single `HalLink`. This **mirrors the field-level swap pattern** in `JsonSchema` (`BooleanOrSchemaSwap` / `BooleanOrSchemaArraySwap` applied on the property accessor, not on the field type). No `Map<String,Object>` raw values; the swap handles all the union plumbing.
   - `_embedded` field, typed as `Map<String,HalResource>` and annotated **`@BeanProp("_embedded") @Swap(HalResourceOrArraySwap.class)`** — identical shape to `_links`, with the analogous `HalResourceArray` for the multi-resource branch.
   - Extension fields → use the **`@BeanProp("*")` extra-properties triplet** from `OpenApiElement` (`extraKeys()` returning the key set + `get(String)` + `set(String,Object)`). Don't expose a raw `Map<String,Object>` field; the existing sibling-bean pattern is the established convention.
2. `HalLink` bean with the 8 fields above and standard fluent setters per `code-conventions/SKILL.md`.
3. `HalLinkArray` / `HalResourceArray` extending `LinkedList<…>` — straight copy of the `JsonSchemaArray` shape; consumed by the `*OrArraySwap` swaps for the multi-element branch.
4. Builder convenience: `HalResource.create().link("self", "/orders/123").embedded("items", listOfItems).set("total", 99.50).build()` (uses the `set(key,value)` dynamic-property method).
5. Tests: round-trip via `JsonSerializer.DEFAULT` for each spec example, including the multi-link `curies` array. Templated link expansion (URI templates per RFC 6570) **out of scope** for v1.

### Design notes

- **`_embedded` recursion.** HAL embeddings are conceptually a tree, not a graph. Juneau's `BeanTraverseContext` throws on cyclic references by default — acceptable for the spec but worth a one-line javadoc warning on `HalResource.setEmbedded(...)`.

## Subtask B — `juneau-bean-jsonapi`

[JSON:API v1.1 spec](https://jsonapi.org/format/). More structural surface than HAL:

- **Top-level:** `data`, `errors`, `meta`, `jsonapi`, `links`, `included`. A document MUST contain at least one of `data`/`errors`/`meta`; `data` and `errors` MUST NOT coexist.
- **Resource object:** `id`, `type`, `attributes`, `relationships`, `links`, `meta`.
- **Resource identifier object:** `id`, `type`, `meta`.
- **Relationship object:** `data` (resource linkage — null, single identifier, or array of identifiers), `links`, `meta`.
- **Error object:** `id`, `links`, `status`, `code`, `title`, `detail`, `source` (with `pointer`/`parameter`/`header`), `meta`.

### Package layout

- Package: `org.apache.juneau.bean.jsonapi`
- `package-info.java` (overview).
- `JsonApiDocument` — top-level container.
- `JsonApiResource` — resource object (with `id`, `type`, `attributes`, `relationships`, `links`, `meta`).
- `JsonApiResourceIdentifier` — slim form used inside relationship linkage.
- `JsonApiRelationship`.
- `JsonApiError` + `JsonApiErrorSource`.
- `JsonApiLink` — link object with `href` (required URI), plus optional `rel`, `describedby` (URI), `title`, `type` (media-type hint), `hreflang`, and `meta` (`Map<String,Object>`).
- `JsonApiLinkOrStringSwap` — custom `ObjectSwap` modeled on `JsonSchema.BooleanOrSchemaSwap` (same precedent as HAL's swaps). Applied at the field level on every `links` member, since JSON:API permits each value to be either a JSON string URL or a `JsonApiLink` object.
- `JsonApiMeta` is **not** a class — `meta` is always `Map<String,Object>` per spec.
- `JsonApiVersion` (the `jsonapi` member: optional `version`, `ext`, `profile`, `meta`).

### Modeling the polymorphic `data`

The top-level `data` and relationship `data` can each be `null`, a single resource (or identifier), or an array of them. Mirror `JsonSchema`'s union pattern:

- Internal pair of fields (single vs array).
- Public `Object getData()` returning the populated one, annotated `@Swap(JsonApiResourceOrArraySwap.class)` (custom `ObjectSwap` matching the JsonSchema `JsonSchemaOrSchemaArraySwap` shape — pick the array class when parsing a JSON array, single class otherwise).
- Two `setData(...)` overloads (`setData(JsonApiResource)` and `setData(List<JsonApiResource>)`).
- A typed accessor pair (`getDataAsResource()` / `getDataAsResourceArray()`) marked `@BeanIgnore`.

### Deliverables

1. The beans listed above with the standard fluent-setter and `code-conventions` patterns (4-method collection rule for `included`/`errors`).
2. **`links` shape (consistent across all four `links`-bearing types).** On `JsonApiDocument`, `JsonApiResource`, `JsonApiRelationship`, and `JsonApiError`, model `links` as `Map<String, JsonApiLink>` with field-level **`@BeanProp("links") @Swap(JsonApiLinkOrStringSwap.class)`**. The swap accepts either a JSON string URL or a `JsonApiLink` object per spec, and mirrors HAL's field-level `_links` swap pattern (Subtask A); the shared precedent is `JsonSchema.BooleanOrSchemaSwap`. No `Map<String,Object>` raw values; the swap handles the union plumbing.
3. `JsonApiResource.type` is a **plain `String` field**, not a Juneau polymorphic discriminator (see "Design notes").
4. Builders matching JSON:API conventions (`document().data(resource).included(other)…`).
5. Tests against the spec's canonical examples (the "compound document" example is the integration test).
6. **Sparse fieldsets / inclusion query parsing** is *out of scope* for v1 — that belongs in a REST-side helper, not the bean module.

### Design notes

- **`JsonApiResource.type` is a plain `String`, never a polymorphic discriminator.** JSON:API uses `type` as an open-ended string field on resource objects (the entity-type name, e.g. `"articles"`) — the value is chosen by the API and is **not** a closed Java class hierarchy. Juneau's `@Marshalled` discriminator defaults to `_type` (see `Marshalled.typePropertyName()` javadoc — default `_type`), so there is no literal name clash either. **Do NOT** annotate `JsonApiResource` with `@Marshalled(typePropertyName="type", dictionary=…)` — that would conflate JSON:API resource typing with Juneau bean dispatch, break round-tripping of unknown `type` values, and force every API to enumerate its resource types in a Java dictionary up front. Call this out explicitly in the package javadoc so implementers don't reach for it.
- **`attributes` shape.** The spec lets `attributes` be an arbitrary JSON object. Model as `Map<String,Object>` on `JsonApiResource` (plain field, no `@BeanProp("*")` flattening — `attributes` is itself a nested object on the wire, not flattened).
- **`links` modeling mirrors HAL's `_links`.** Each `links` value can be either a JSON string URL or a Link object per spec — the same single-vs-object union shape HAL solves with `HalLinkOrArraySwap`. Use field-level `@BeanProp("links") @Swap(JsonApiLinkOrStringSwap.class)` on every `links`-bearing type (`JsonApiDocument`, `JsonApiResource`, `JsonApiRelationship`, `JsonApiError`); the shared precedent is `JsonSchema.BooleanOrSchemaSwap`.

## Subtask C — `juneau-bean-jsonpatch`

[RFC 6902 — JSON Patch](https://www.rfc-editor.org/rfc/rfc6902). Smallest of the three.

The wire format is a JSON array of operation objects. RFC 6902 defines exactly six ops: `add`, `remove`, `replace`, `move`, `copy`, `test`.

```json
[
  { "op": "test",    "path": "/a/b/c", "value": "foo" },
  { "op": "remove",  "path": "/a/b/c" },
  { "op": "add",     "path": "/a/b/c", "value": ["foo","bar"] },
  { "op": "replace", "path": "/a/b/c", "value": 42 },
  { "op": "move",    "from": "/a/b/c", "path": "/a/b/d" },
  { "op": "copy",    "from": "/a/b/e", "path": "/a/b/d" }
]
```

### Package layout

- Package: `org.apache.juneau.bean.jsonpatch`
- `package-info.java` (overview, links to RFC 6902).
- `JsonPatch extends LinkedList<JsonPatchOperation>` — the document. Mirrors `JsonSchemaArray`; serializes/parses as a top-level JSON array with no wrapper.
- `JsonPatchOperation` — **abstract** base annotated `@Marshalled(typePropertyName="op", dictionary={ AddOp.class, RemoveOp.class, ReplaceOp.class, MoveOp.class, CopyOp.class, TestOp.class })`. Carries shared `path` field.
- `AddOp`, `RemoveOp`, `ReplaceOp`, `MoveOp`, `CopyOp`, `TestOp` — each annotated `@Marshalled(typeName="add")` (etc.) and extending `JsonPatchOperation`.

### Deliverables

1. `JsonPatchOperation` abstract base + 6 concrete subclasses per the dictionary above. Each subclass carries only the fields it needs: `value` on `AddOp`/`ReplaceOp`/`TestOp`, `from` on `MoveOp`/`CopyOp`, just `path` on `RemoveOp`.
2. `JsonPatch` as a `LinkedList<JsonPatchOperation>` subclass with convenience `addOp(JsonPatchOperation...)` varargs adder (matches `JsonSchemaArray.addAll(JsonSchema...)`).
3. **No apply-to-document implementation in v1.** The bean module just defines the shape; an `apply(JsonMap target)` helper can land as a follow-up TODO once we know who needs it.
4. Tests: spec round-trip for each op type, plus a multi-op document.
5. Reader/writer wiring example in the package javadoc showing the per-call config needed for polymorphic dispatch on read:
   ```
   JsonParser parser = JsonParser.create()
       .typePropertyName(JsonPatchOperation.class, "op")
       .build();
   ```
   (modeled on `McpBeans_RoundTrip_Test`'s setup for `Content`).

### Notes on the previous draft

- The previous draft referenced `@Bean(typePropertyName="op")`. The correct annotation is `@Marshalled(typePropertyName="op")` (`org.apache.juneau.annotation.Marshalled` in `juneau-marshall`). Juneau does not have a `@Bean` annotation — class-level bean modelling is `@BeanType` (in `juneau-commons`) and marshalling concerns are `@Marshalled`.
- The previous draft referenced `@Json(wrapperAttr="@v")` to "flatten" the document into a top-level array. `@Json.wrapperAttr` does the **opposite** — it *adds* an outer object wrapper (e.g. `{ "@v": [...] }`). The top-level-array pattern in Juneau is just `extends LinkedList<…>`, as `JsonSchemaArray` demonstrates; no annotation is needed.
- "Sealed/abstract base" → just **abstract base**. Juneau targets Java 17 so sealed classes compile, but the framework's bean dispatch is via the `@Marshalled` dictionary, not Java sealed-type reflection. Plain `abstract` matches every other polymorphic base in the codebase (`Content` in `juneau-bean-mcp`, `OpenApiElement`).

## Cross-cutting design notes

- **Module layout.** Each new module under `juneau-bean/` (sibling of `juneau-bean-atom`, `juneau-bean-mcp`, …). Parent `juneau-bean/pom.xml` aggregates these modules today: `juneau-bean-atom`, `juneau-bean-common`, `juneau-bean-html5`, `juneau-bean-jsonschema`, `juneau-bean-mcp`, `juneau-bean-openapi-v3`, `juneau-bean-swagger-v2`. Add three new `<module>` entries (`juneau-bean-hal`, `juneau-bean-jsonapi`, `juneau-bean-jsonpatch`) — keep the list alphabetically sorted.
- **POM template.** Copy `juneau-bean-mcp/pom.xml` verbatim and edit `artifactId` / `name` / `description`. The `packaging` is `bundle`, the single runtime dep is `org.apache.juneau:juneau-marshall:${project.version}`, and the build wires `maven-bundle-plugin` (OSGi MANIFEST), `maven-source-plugin`, `maven-jar-plugin`, and `jacoco-maven-plugin`.
- **Dependency policy.** Each module depends **only** on `juneau-marshall` (for `@Marshalled`, `@BeanProp`, `JsonSerializer`/`JsonParser` for tests). No REST-layer dependencies.
- **Annotation reference (correct names).**
  - Class-level marshalling — `@Marshalled` (with `typeName`, `typePropertyName`, `dictionary`, …) from `org.apache.juneau.annotation`.
  - Class-level bean modelling — `@BeanType` from `org.apache.juneau.commons.bean`.
  - Property-level — `@BeanProp` from `org.apache.juneau.commons.bean` (positional value `@BeanProp("foo")` or named `@BeanProp(name="foo")`; `@BeanProp("*")` is the "dynamic / extension property" marker used by `OpenApiElement` and `SwaggerElement`).
  - Property-level marshalling — `@MarshalledProp` from `org.apache.juneau.annotation` (`dictionary`, `format`, `properties`).
  - JSON-wrapper — `@Json(wrapperAttr=…)` *adds* an outer object wrapper; it does **not** flatten or unwrap. The top-level-array pattern is `extends LinkedList<…>`, not an annotation.
- **No `ContentType` work** — all three constants ship today (see References below for line numbers).
- **Coverage bar.** ≥ 90% per module (matches sibling bean modules).
- **Tests.** Place under `juneau-utest/src/test/java/org/apache/juneau/bean/<format>/`, matching how `juneau-bean-mcp` tests live in `juneau-utest/.../bean/mcp/`. Use the `code-conventions` patterns (`assertBean`, SSLLC, `@Nested` categories for >20 tests, `TestUtils.jsonRoundTrip(...)`).
- **`package-info.java`** is required for every module (see `juneau-bean-jsonschema/.../package-info.java` for the template — overview, bean class list, spec links, doclink to the topic page).
- **Release notes.** Each new module gets its own entry in `juneau-docs/docs/pages/release-notes/9.5.0.md` under "New Modules".

## Out of scope (for the parent TODO)

- Auto-discovery / class-path wiring of these beans into Juneau's media-type dispatch. Bean modules don't need that; they're consumed explicitly by user code.
- Spring `ProblemDetail` interop (that belongs in TODO-45's `juneau-bean-rfc7807`).
- Server-side helpers like JSON:API pagination links, HAL CURIEs, JSON Patch apply.

## Verification (per subtask)

- `mvn -pl juneau-bean/juneau-bean-<name> -am install` succeeds.
- New module appears in the parent reactor (`juneau-bean/pom.xml`).
- Existing `ContentType` constant referenced from at least one Javadoc / test.
- Round-trip via `JsonSerializer.DEFAULT` (plus the per-test polymorphic-dispatch config for `JsonPatchOperation`) matches at least one canonical example from the format's spec.

## References

### Specifications

- HAL — [stateless.group HAL specification](https://stateless.group/hal_specification.html) and [draft-kelly-json-hal-08](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08) (canonical Link Object field list).
- JSON:API v1.1 — [jsonapi.org/format](https://jsonapi.org/format/).
- RFC 6902 — [JSON Patch](https://www.rfc-editor.org/rfc/rfc6902).

### `ContentType` constants (confirmed present today)

All three constants already ship in `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/http/header/ContentType.java`:

- `APPLICATION_HAL_JSON` — line 43 (`application/hal+json`).
- `APPLICATION_VND_API_JSON` — line 99 (`application/vnd.api+json`).
- `APPLICATION_JSON_PATCH` — line 59 (`application/json-patch+json`).

Each is also exercised by parameterized tests in `juneau-utest/src/test/java/org/apache/juneau/http/header/ContentType_Test.java`. **No `ContentType` work is required by this TODO.**

### Sibling bean modules used as architectural references

- `juneau-bean/juneau-bean-mcp` — polymorphic dispatch pattern (`Content` → `TextContent` / `ImageContent` / `EmbeddedResourceContent` via `@Marshalled(typePropertyName="type", dictionary={…})`, base annotated as an interface with `@Marshalled`, subclasses with `@Marshalled(typeName="…")`). Reference for **Subtask C** (`JsonPatchOperation` dispatch) and the polymorphic-parser test setup in `McpBeans_RoundTrip_Test`.
- `juneau-bean/juneau-bean-jsonschema` — closest analogue for **Subtasks A and B**: the `BooleanOrSchemaSwap` / `JsonSchemaOrSchemaArraySwap` family demonstrates the single-vs-other union pattern that both HAL `_links` / `_embedded` (Subtask A, via `HalLinkOrArraySwap` / `HalResourceOrArraySwap`) and JSON:API `links` (Subtask B, via `JsonApiLinkOrStringSwap`) map values need. `JsonSchemaArray extends LinkedList<JsonSchema>` is the same top-level-array pattern used by `JsonPatch`. `@BeanProp("$defs")` shows the property-renaming pattern for `_links` / `_embedded` / `links`.
- `juneau-bean/juneau-bean-openapi-v3` (and the parallel `juneau-bean-swagger-v2`) — `OpenApiElement` is the canonical `@BeanProp("*")` extra-properties triplet (`extraKeys()` + `get(String)` + `set(String,Object)`) used for HAL's extension fields.
- `juneau-bean/juneau-bean-common` and `juneau-bean/juneau-bean-atom` — referenced for `pom.xml` structure (OSGi bundle, maven-bundle-plugin wiring, jacoco wiring).
