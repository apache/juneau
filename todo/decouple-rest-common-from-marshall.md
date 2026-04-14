# Decouple `juneau-rest-common` from `juneau-marshall`

## Goal

Break juneau-rest-common's compile-time dependency on juneau-marshall so the module chain becomes:

```
juneau-commons -> juneau-rest-common -> juneau-marshall -> juneau-rest-client
                                                        -> juneau-rest-server
```

**Status (current):**

- **Done:** HTTP media/range types in commons (Phase 1a below).
- **Done:** `@Schema`, `@Items`, `@SubItems`, `@ExternalDocs` live in `org.apache.juneau.commons.annotation`; `@Repeatable` uses `Schema.Array` on `Schema`; marshall-side `SchemaAnnotation` / apply types import them from commons. `juneau-utest` declares a **direct** `juneau-commons` dependency so IDE classpaths resolve types such as `ExternalDocs` referenced indirectly from `Swagger`.
- **Still on marshall:** `SchemaAnnotation` (large), `InvalidAnnotationException`, serializers, `HttpPartSchema`, bean/httppart meta, etc. **`juneau-rest-common` still depends on `juneau-marshall`** until later phases shrink that surface.

---

## Phase 1a — Move `MediaType`, `MediaRanges`, `StringRanges` to `juneau-commons` (done)

**Completed:** The following now live in `org.apache.juneau.commons.http` in **juneau-commons**:

- `MediaType`, `MediaRange`, `MediaRanges`, `StringRange`, `StringRanges`

**Also in commons (parser support):** `NameValuePair`, `BasicNameValuePair`, `HeaderElement`, `HeaderValueParser` (replacing HttpCore usage for these types).

**Follow-ups:**

- `juneau-rest-common` declares a **direct** dependency on `juneau-commons` (in addition to `juneau-marshall`) so HTTP types are explicit on the classpath.
- `@org.apache.juneau.commons.annotation.BeanIgnore` marks these types as non-beans; `BeanMeta` treats it like `org.apache.juneau.annotation.BeanIgnore` at class level so JSON remains the canonical string form.
- `Constants` was **not** moved; it is unrelated to this HTTP surface.
- Third-party name clashes (e.g. OkHttp `MediaType` vs Juneau) are resolved with fully qualified names or by omitting the Juneau import where only OkHttp's type is used.

---

## Phase 1b — Move `@Schema` and related annotations to `juneau-commons` (done for annotation types)

**Difficulty**: Hard  
**Impact**: Large (HTTP annotations, Swagger models)

**Completed (annotation surface):**

- `org.apache.juneau.commons.annotation.Schema` (including inner `Schema.Array` for `@Repeatable`)
- `org.apache.juneau.commons.annotation.Items`, `SubItems`, `ExternalDocs`
- Javadoc on commons `Schema` / `Items` avoids `{@link}` to marshall-only types (neutral text / external doclinks).
- Call sites updated across marshall, REST, beans, examples, tests.

**Not done in this phase (still marshall or follow-up):**

- **`SchemaAnnotation`** remains in `juneau-marshall` (apply machinery, generator hooks); it imports the commons annotation types.
- **`InvalidAnnotationException`** remains in `org.apache.juneau.annotation` (marshall); moving it to commons is optional for a later “rest-common compiles without marshall” milestone.
- **Global `@XApply` split** (removing `on` / `onClass` from context-appliable annotations into companion `@XApply` types) is still a **separate**, release-shaping effort if pursued; it was listed as a prerequisite in an older draft but the annotation **types** were moved without blocking on that split.

**Tooling / classpath:**

- Any test or sample module that compiles against REST types which reference commons annotations should depend on **`juneau-commons` explicitly** when the IDE does not expand transitive deps cleanly (e.g. `juneau-utest`).

**Still true for next phases:**

- **`HttpPartSchema`** reads `@Schema` from commons today; Phase 2 (below) may move `HttpPartSchema` itself to commons.

---

## Phase 2 — Move `HttpPartSchema` and related types to `juneau-commons`

**Difficulty**: Hard  
**Impact**: 12 files in rest-common

`HttpPartSchema` is the schema model built from `@Schema` annotations. `@Schema` is already in commons; moving the **model** (`HttpPartSchema` and related enums) is the next big step toward shrinking rest-common’s marshall dependency.

### Classes to move
- `org.apache.juneau.httppart.HttpPartSchema`
- `org.apache.juneau.httppart.HttpPartType`
- `org.apache.juneau.httppart.HttpPartDataType`
- `org.apache.juneau.httppart.HttpPartCollectionFormat`
- `org.apache.juneau.httppart.HttpPartFormat`

### Classes to keep in marshall
- `HttpPartSerializer` and `HttpPartParser` (interface + implementations depend on serializer infrastructure)
- `HttpPartMarshalling` (references serializer/parser types)

### Considerations
- `HttpPartSchema.Builder` currently has `apply(HttpPartMarshalling)` which reads `serializer()`/`parser()` -- this creates a dependency on marshall. Could be split: schema model in commons, marshalling-aware builder extension in marshall.

---

## Phase 3 — Extract `Serialized*` bridge classes

**Difficulty**: Medium  
**Impact**: 3 files (SerializedHeader, SerializedPart, SerializedEntity) + 3 factory helpers

These classes have the heaviest marshall dependencies -- they use `httppart`, `oapi`, `serializer`, and `urlencoding` packages. They bridge the gap between REST-common HTTP types and marshall serialization.

### Options
1. **Move to marshall**: These classes logically belong in the serialization layer
2. **Create bridge module**: `juneau-rest-bridge` between commons and marshall
3. **Keep in rest-common with optional dependency**: Use reflection to optionally load serializers

### Recommendation
Move `SerializedHeader`, `SerializedPart`, `SerializedEntity` and their factory methods from `HttpHeaders`/`HttpParts`/`HttpEntities` to juneau-marshall or a new bridge module. The base `Basic*` classes in rest-common would remain serializer-free.

---

## Phase 4 — Remove remaining marshall dependencies

**Difficulty**: Low-Medium  

### VarResolverSession (2 files)
- `HeaderList` and `PartList` use SVL variable resolution
- Option: Make SVL resolution optional via interface/callback

### BeanCreator (3 files in httppart.bean)
- Used by `RequestBeanMeta`/`ResponseBeanMeta` for bean construction
- Option: Use `ClassInfo.newInstance()` from commons instead, or move `BeanCreator` to commons

### Assertion types (19 files)
- `juneau-assertions` is already a separate module
- Rest-common should depend on `juneau-assertions` directly, not transitively through marshall

---

## Dependency surface summary

| Priority | Package | Files | Target module | Status |
|----------|---------|-------|----------------|--------|
| 1 | `MediaType`, `MediaRanges`, `StringRanges`, etc. | many | juneau-commons (`org.apache.juneau.commons.http`) | Done |
| 2 | `@Schema`, `@Items`, `@SubItems`, `@ExternalDocs` (+ marshall `SchemaAnnotation` / apply) | many | commons annotations + marshall | Annotation types **done**; `SchemaAnnotation` / `InvalidAnnotationException` still marshall |
| 3 | `HttpPartSchema`, `HttpPartType`, … | 12 | juneau-commons | Pending |
| 4 | `Serialized*` bridge classes | 6 | juneau-marshall or bridge | Pending |
| 5 | `VarResolverSession`, `BeanCreator` | 5 | juneau-commons or optional | Pending |

## Risk notes

- **Split-package**: HTTP media types moved out of `org.apache.juneau` into `org.apache.juneau.commons.http`
- **Binary compatibility**: All moves require recompilation of downstream modules
- **Schema complexity**: `@Schema` has ~50 attributes and deep integration with serializers
- **Breaking `XApply` migration**: Replacing `on`/`onClass` on context-appliable annotations touches many call sites; plan for a single major release boundary and migration notes
- **Incremental approach**: Each phase should be self-contained and buildable independently
