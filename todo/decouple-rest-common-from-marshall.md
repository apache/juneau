# Decouple `juneau-rest-common` from `juneau-marshall`

## Goal

Break juneau-rest-common's compile-time dependency on juneau-marshall so the module chain becomes:

```
juneau-commons -> juneau-rest-common -> juneau-marshall -> juneau-rest-client
                                                        -> juneau-rest-server
```

Currently juneau-rest-common depends on juneau-marshall for:
- `@Schema`, `InvalidAnnotationException` (83 files)
- `MediaType`, `MediaRanges`, `StringRanges` (26 files)
- `HttpPartSerializer`, `HttpPartSchema`, `HttpPartType` (12 files)
- `Serializer`, `SerializerSession` (4 files)
- `OpenApiSerializer` (3 files)
- `BeanCreator` (3 files)
- `VarResolverSession` (2 files)
- `JsonSchemaSerializer`, `Json5Serializer` (2 files)
- `UrlEncodingSerializer` (1 file)

---

## Phase 1 — Move `MediaType`, `MediaRanges`, `StringRanges` to `juneau-commons`

**Difficulty**: Medium  
**Impact**: 26 files in rest-common

These are fundamental HTTP types that don't need serializer/parser infrastructure. Move from `org.apache.juneau` (marshall root package) to `org.apache.juneau.commons.http` or similar in juneau-commons.

### Classes to move
- `MediaType`
- `MediaRange`
- `MediaRanges`
- `StringRange`
- `StringRanges`
- `Constants` (if only used for media type constants)

### Considerations
- These types are used pervasively across marshall, rest-server, and rest-client
- Must assess whether `MediaType` depends on anything else in marshall
- Package rename may be needed to avoid split-package issues

---

## Phase 2 — Move `@Schema` and related annotations to `juneau-commons`

**Difficulty**: Hard  
**Impact**: 83 files (7 HTTP annotations + Swagger model classes)

`@Schema` is the #1 blocker. Every HTTP annotation references it via `Schema schema() default @Schema`. `InvalidAnnotationException` is also used.

### Classes to move
- `org.apache.juneau.annotation.Schema` → `org.apache.juneau.commons.annotation.Schema`
- `org.apache.juneau.annotation.Items` → `org.apache.juneau.commons.annotation.Items`
- `org.apache.juneau.annotation.SubItems` → `org.apache.juneau.commons.annotation.SubItems`
- `org.apache.juneau.annotation.ExternalDocs` → `org.apache.juneau.commons.annotation.ExternalDocs`
- `InvalidAnnotationException`

### Considerations
- `SchemaAnnotation` (empty(), DEFAULT, builder utilities) must also move or be split
- `@Schema` references `JsonSchemaSerializer` in javadoc — javadoc link would break
- `HttpPartSchema` reads `@Schema` attributes — must check if `HttpPartSchema` can also move or if reflective access suffices

---

## Phase 3 — Move `HttpPartSchema` and related types to `juneau-commons`

**Difficulty**: Hard  
**Impact**: 12 files in rest-common

`HttpPartSchema` is the schema model built from `@Schema` annotations. If `@Schema` moves to commons, `HttpPartSchema` should follow.

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
- `HttpPartSchema.Builder` currently has `apply(HttpPartMarshalling)` which reads `serializer()`/`parser()` — this creates a dependency on marshall. Could be split: schema model in commons, marshalling-aware builder extension in marshall.

---

## Phase 4 — Extract `Serialized*` bridge classes

**Difficulty**: Medium  
**Impact**: 3 files (SerializedHeader, SerializedPart, SerializedEntity) + 3 factory helpers

These classes have the heaviest marshall dependencies — they use `httppart`, `oapi`, `serializer`, and `urlencoding` packages. They bridge the gap between REST-common HTTP types and marshall serialization.

### Options
1. **Move to marshall**: These classes logically belong in the serialization layer
2. **Create bridge module**: `juneau-rest-bridge` between commons and marshall
3. **Keep in rest-common with optional dependency**: Use reflection to optionally load serializers

### Recommendation
Move `SerializedHeader`, `SerializedPart`, `SerializedEntity` and their factory methods from `HttpHeaders`/`HttpParts`/`HttpEntities` to juneau-marshall or a new bridge module. The base `Basic*` classes in rest-common would remain serializer-free.

---

## Phase 5 — Remove remaining marshall dependencies

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

## Dependency Surface Summary

| Priority | Package | Files | Target Module |
|----------|---------|-------|---------------|
| 1 | `MediaType`, `MediaRanges`, etc. | 26 | juneau-commons |
| 2 | `@Schema`, `@Items`, `@SubItems` | 83 | juneau-commons |
| 3 | `HttpPartSchema`, `HttpPartType` | 12 | juneau-commons |
| 4 | `Serialized*` bridge classes | 6 | juneau-marshall or bridge |
| 5 | `VarResolverSession`, `BeanCreator` | 5 | juneau-commons or optional |

## Risk Notes

- **Split-package**: Moving `MediaType` from `org.apache.juneau` requires a new package name in commons
- **Binary compatibility**: All moves require recompilation of downstream modules
- **Schema complexity**: `@Schema` has ~50 attributes and deep integration with serializers
- **Incremental approach**: Each phase should be self-contained and buildable independently
