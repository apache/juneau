# Decouple `juneau-rest-common` from `juneau-marshall`

Target module chain:

```
juneau-commons -> juneau-rest-common -> juneau-marshall -> juneau-rest-client
                                                        -> juneau-rest-server
```

`juneau-rest-common/pom.xml` still declares a compile dep on `juneau-marshall`. The remaining gap below is the work needed to drop it.

---

## Already landed (no further action)

- **Phase 1a — commons HTTP types** — `MediaRange` / `MediaRanges` / `MediaType` / `StringRange` / `StringRanges` live in `org.apache.juneau.commons.http`.
- **Phase 1b — commons annotations** — `@Schema` family lives in `org.apache.juneau.commons.annotation`.
- **Phase 2 (initial) — httppart enums + exception in commons** — `HttpPartType`, `HttpPartFormat`, `HttpPartDataType`, `HttpPartCollectionFormat`, `SchemaValidationException` live in `org.apache.juneau.commons.httppart`.
- **SVL in commons (TODO-14)** — `VarResolverSession` and friends in `org.apache.juneau.commons.svl`; `HeaderList` / `PartList` no longer pull marshall for variable resolution.
- **BeanCreator → BeanInstantiator (TODO-15)** — rest-common's `RequestBeanMeta` / `RequestBeanPropertyMeta` / `ResponseBeanPropertyMeta` now use `org.apache.juneau.commons.inject.BeanInstantiator` (commons). The "move `BeanCreator` or use `ClassInfo.newInstance()`" Phase 4 row is done.
- **`juneau-assertions` direct dep** — `juneau-rest-common/pom.xml` already declares `juneau-assertions` directly (no longer transitive only through marshall).

> The plan from here lists **only** what is still required to drop the `juneau-marshall` dep from rest-common.

---

## Current marshall surface still imported by rest-common

Verified by `rg ^import org\.apache\.juneau\.(serializer|parser|oapi|urlencoding|json|jsonschema|annotation\.[A-Z]|httppart\.[A-Z])` under `juneau-rest-common/src/main/java`.

### A. Httppart marshalling surface (the big one)

Symbols still in `org.apache.juneau.httppart` (marshall):

- `HttpPartSchema` (+ `Builder`, `DEFAULT`)
- `HttpPartSerializer` / `HttpPartSerializerSession`
- `HttpPartParser` / `HttpPartParserSession`
- `HttpPart` interface
- `HttpPartMarshalling`

Rest-common consumers:

- `httppart/bean/RequestBeanMeta`, `ResponseBeanMeta`, `RequestBeanPropertyMeta`, `ResponseBeanPropertyMeta`
- `http/header/HeaderBeanMeta`, `http/header/SerializedHeader`
- `http/part/PartBeanMeta`, `http/part/SerializedPart`
- `http/entity/SerializedEntity`
- `http/HttpHeaders` (`serializedHeader(...)` factories)
- `http/HttpEntities` (`serializedEntity(...)` factory)
- `http/HttpParts` (uses `HttpPartType` + `ClassMeta<?>`)

### B. Serializer surface (Phase 3 — `Serialized*` bridges)

- `org.apache.juneau.serializer.{Serializer, SerializerSession, SerializeException, SchemaValidationException}`
- `org.apache.juneau.oapi.OpenApiSerializer`
- `org.apache.juneau.urlencoding.UrlEncodingSerializer`

Rest-common consumers: `SerializedHeader`, `SerializedPart`, `SerializedEntity`, `HttpHeaders`, `HttpEntities`.

### C. `InvalidAnnotationException`

- Lives in `org.apache.juneau.annotation` (marshall).
- Used by `httppart/bean/MethodInfoUtils` and `httppart/bean/ResponseBeanMeta` (static import).
- Only base class tying it to marshall is the legacy `org.apache.juneau.BasicRuntimeException` shim, which itself just extends `org.apache.juneau.commons.BasicRuntimeException`.

### D. Javadoc-only marshall imports

Real imports, but only used inside `{@link ...}` comments:

- `Content.java` — `import org.apache.juneau.json.JsonSchemaSerializer;`
- `BasicMediaTypeHeader.java` — `import org.apache.juneau.json.*;` (`JsonSerializer`) and `import org.apache.juneau.json5.Json5Serializer;`

### E. `ClassMeta` (cross-cutting)

`ClassMeta<?>` (in `org.apache.juneau`, marshall) is referenced by:

- `http/HttpParts` (`HEADER_NAME_FUNCTION`, `QUERY_NAME_FUNCTION`, `getName`, `isHttpPart`, …)
- `httppart/bean/RequestBeanMeta.getBeanInfo()` / `cm` field
- `httppart/bean/ResponseBeanMeta.getBeanInfo()` / `cm` field

Removing this dependency depends on **TODO-30** (moving `ClassMeta` and related non-marshalling type metadata into `juneau-commons`). Rest-common cannot fully drop marshall until either `ClassMeta` moves (preferred) or these APIs are reworked to use `ClassInfo` + helpers that already exist in commons.

---

## Plan

### Step 1 — Quick wins (no semantic moves)

**Difficulty:** Trivial. **Removes 0 marshall types but removes some javadoc-coupled imports.**

1. In `Content.java`, replace `import org.apache.juneau.json.JsonSchemaSerializer;` + `{@link JsonSchemaSerializer}` with fully-qualified `{@link org.apache.juneau.json.JsonSchemaSerializer}`.
2. In `BasicMediaTypeHeader.java`, replace `import org.apache.juneau.json.*;` and `import org.apache.juneau.json5.Json5Serializer;` with fully-qualified Javadoc `{@link ...}` references.

Net effect: eliminates the only two marshall imports that exist solely for Javadoc.

### Step 2 — Move `InvalidAnnotationException` to commons

**Difficulty:** Low.

1. Add `org.apache.juneau.commons.annotation.InvalidAnnotationException` extending `org.apache.juneau.commons.BasicRuntimeException` (parent of the marshall shim).
2. Make the existing `org.apache.juneau.annotation.InvalidAnnotationException` a deprecated subclass of the commons version for backwards compatibility (mirrors the existing pattern used for `BasicRuntimeException`).
3. Repoint `httppart/bean/MethodInfoUtils` and `httppart/bean/ResponseBeanMeta` at the commons class.

After this, rest-common no longer imports `org.apache.juneau.annotation.InvalidAnnotationException`.

### Step 3 — Phase 2 follow-through: move `HttpPartSchema` (+ part marshalling surface) to commons

**Difficulty:** Medium. Single largest item.

Goal: move the **interfaces and the schema data type**, keep the **default serializer/parser implementations** in marshall.

Move into `org.apache.juneau.commons.httppart`:

- `HttpPartSchema` (+ `Builder`)
- `HttpPart` interface
- `HttpPartSerializer` / `HttpPartSerializerSession` interfaces (Creator inner-classes optional — they can stay in marshall as long as the interface is in commons)
- `HttpPartParser` / `HttpPartParserSession` interfaces
- `HttpPartMarshalling` (annotation + facade)

Known blockers / hand-offs (still applicable from the previous plan revision):

- `apply(HttpPartMarshalling)` on the builder ties to marshall's `HttpPartMarshalling` annotation handling → move the annotation type to commons or split apply logic.
- References to `*Annotation.empty()` (`ItemsAnnotation.DEFAULT`, `SubItemsAnnotation.DEFAULT`) → already covered by Phase 1b annotation moves; verify no marshall-only annotations remain.
- `ParseException` import from marshall in schema-validation paths → either move a minimal exception to commons or replace with `SchemaValidationException` (already in commons).

> The concrete implementations (`SimplePartSerializer/Parser`, `BaseHttpPartSerializer/Parser`, OpenAPI- / URL-encoded-backed serializers, etc.) stay in marshall and `implement` the commons-side interfaces.

### Step 4 — Phase 3: handle `Serialized*` bridge types

**Difficulty:** Medium. **Impact:** `SerializedHeader`, `SerializedPart`, `SerializedEntity` + factories on `HttpHeaders` / `HttpParts` / `HttpEntities`.

These types intrinsically need access to `Serializer` / `SerializerSession` / `OpenApiSerializer` / `UrlEncodingSerializer`, so they cannot live in rest-common once it stops depending on marshall. Options (pick one):

1. **Move bridges to marshall** — `Serialized*` types move out of `org.apache.juneau.http.{header,part,entity}` (rest-common) into a marshall package. `Basic*` types (without serializers) stay in rest-common. Factories on `HttpHeaders` / `HttpEntities` / `HttpParts` either move with them or get split (rest-common keeps the no-serializer overloads; serializer-aware overloads move to marshall).
2. **New `juneau-rest-bridge` module** between commons and marshall hosting `Serialized*` types.
3. **Keep in rest-common with optional/reflective loading.** Not recommended — defeats the goal.

**Recommendation:** Option 1 — move `Serialized*` (and the serializer-aware factory overloads) into marshall under `org.apache.juneau.httppart.bridge` (or similar), keep the non-serializer Basic surface in rest-common.

### Step 5 — Resolve `ClassMeta` references

**Blocked on TODO-30** (`ClassMeta` → commons feasibility pass).

Two paths once TODO-30 lands:

- **(A) `ClassMeta` moves to commons** — rest-common keeps `getBeanInfo()` / `ClassMeta<?>` surface unchanged.
- **(B) `ClassMeta` stays in marshall** — rewrite `HttpParts` helpers, `RequestBeanMeta.getBeanInfo()`, `ResponseBeanMeta.getBeanInfo()` to expose `ClassInfo` (already in commons) plus an annotation-driven helper, removing the `ClassMeta` surface from rest-common.

This is the **final blocker** before rest-common can drop marshall from its pom.

### Step 6 — Pom + Eclipse `.classpath` flip

Once steps 1-5 are merged:

- Remove `juneau-marshall` from `juneau-rest-common/pom.xml`.
- Drop the corresponding `<classpathentry>` from `juneau-rest-common/.classpath`.
- Re-run `mvn -pl juneau-rest-common -am clean install` to confirm rest-common compiles against commons + assertions + httpcore only.
- Re-run reactor tests; rest-client / rest-server / rest-mock keep marshall.

---

## Dependency surface (pending rows only)

| Priority | Area | Target |
|----------|------|--------|
| 1 | Javadoc-only `json.*` imports in `Content.java` / `BasicMediaTypeHeader.java` | inline fully-qualified `{@link}` |
| 2 | `InvalidAnnotationException` | commons (`org.apache.juneau.commons.annotation`) |
| 3 | `HttpPartSchema` + httppart marshalling **interfaces** | commons (`org.apache.juneau.commons.httppart`) |
| 4 | `Serialized*` bridges (+ factory overloads) | marshall (or new `juneau-rest-bridge` module) |
| 5 | `ClassMeta<?>` surface in `HttpParts` / `*BeanMeta` | depends on TODO-30 outcome |
| 6 | Drop `juneau-marshall` dep from rest-common pom + `.classpath` | rest-common build |

---

## Risk notes

- **Split-package risk** — `org.apache.juneau.httppart` exists in both marshall (concrete impls) and commons (enums + interfaces). OSGi consumers will see split packages; this is already the case for `org.apache.juneau.commons.httppart` vs `org.apache.juneau.httppart`. Keep using the **`org.apache.juneau.commons.httppart`** namespace for moved interfaces to avoid worsening the split.
- **Binary compatibility** — Steps 2 and 3 must leave deprecated shims in `org.apache.juneau.annotation.InvalidAnnotationException` and `org.apache.juneau.httppart.HttpPartSchema` (etc.) extending / type-aliasing the commons versions for at least one release.
- **`@Schema` ↔ serializer integration** — the schema-aware OpenAPI / URL-encoding serializer paths in marshall must continue to wire through the commons-side `HttpPartSchema` after the move; verify via existing schema-validation tests.
- **`@XApply` split** (`on` / `onClass`) is a separate, release-sized effort — out of scope here.
- **Keep phases independently buildable** — each Step above should be its own commit / PR; the reactor must build green between steps.
