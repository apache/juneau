# Decouple `juneau-rest-common` from `juneau-marshall`

Target module chain:

```
juneau-commons -> juneau-rest-common -> juneau-marshall -> juneau-rest-client
                                                        -> juneau-rest-server
```

Phases **1a** (HTTP types in commons), **1b** (annotation types in commons), and **2** (enums/exceptions/`HttpPartSchema` refactor, annotation cleanup) are **done** — details removed from this plan to avoid noise.

**Current gap:** `juneau-rest-common` **still** compile-depends on `juneau-marshall` (`SchemaAnnotation`, `InvalidAnnotationException`, serializers, refactored-but-not-moved `HttpPartSchema`, bean/httppart meta, etc.).

---

## Phase 2 follow-through — move `HttpPartSchema` to commons (deferred)

`HttpPartSchema` remains in marshall until these are addressed:

- `apply(HttpPartMarshalling)` on the builder ties to marshall’s `HttpPartMarshalling`
- References to `*Annotation.empty()` (e.g. `ItemsAnnotation.DEFAULT`, `SubItemsAnnotation.DEFAULT`)
- `ParseException` import from marshall

**Lower priority:** strip Creator inner classes from `HttpPartSerializer` / `HttpPartParser` (can stay in marshall regardless).

**Keep in marshall by design:** `HttpPartSerializer`, `HttpPartParser`, `HttpPartMarshalling` (serializer/parser surface).

---

## Phase 3 — Extract `Serialized*` bridge classes

**Difficulty:** Medium  
**Impact:** `SerializedHeader`, `SerializedPart`, `SerializedEntity` + factory helpers

Heaviest marshall usage (`httppart`, `oapi`, `serializer`, `urlencoding`). Options:

1. Move bridge types into **marshall**
2. New **`juneau-rest-bridge`** module between commons and marshall
3. Keep in rest-common with optional/reflection loading

**Recommendation:** Move `SerializedHeader` / `SerializedPart` / `SerializedEntity` (and factories on `HttpHeaders` / `HttpParts` / `HttpEntities`) to marshall or a bridge module; keep `Basic*` types serializer-free in rest-common.

---

## Phase 4 — Remove remaining marshall dependencies

**Difficulty:** Low–medium

- **VarResolverSession** — `HeaderList` / `PartList` SVL resolution; optional interface/callback to make SVL optional
- **BeanCreator** (`httppart.bean`) — `RequestBeanMeta` / `ResponseBeanMeta`; consider `ClassInfo.newInstance()` or moving `BeanCreator` to commons
- **Assertions** — rest-common should depend on **`juneau-assertions`** directly, not only transitively through marshall

---

## Dependency surface (pending rows)

| Priority | Area | Target |
|----------|------|--------|
| 3 | `HttpPartSchema` + remaining httppart surface | commons (blocked as above) |
| 4 | `Serialized*` bridge | marshall or bridge module |
| 5 | `VarResolverSession`, `BeanCreator`, assertions wiring | commons / optional / direct deps |

---

## Risk notes (still relevant)

- Split-package / HTTP types in `org.apache.juneau.commons.http`
- Binary compatibility on moves
- `@Schema` / serializer integration complexity
- **`@XApply` split** (`on` / `onClass`) is a separate, release-sized effort if pursued
- Keep phases independently buildable
