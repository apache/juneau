# FINISHED-29 — Sorted Properties Default + MarshallUtils Expansion

## Summary

Flipped the default bean property serialization order from natural JVM order to alphabetical
(sorted) across all serializers, with `@Bean(unsorted=true)` / `unsortedProperties()` as
the opt-out.  Also fully removed the deprecated `sortProperties`-flavored API and expanded
`MarshallUtils` with a complete suite of overloads.

---

## Phase 1 — Default flip and new API

- Inverted the internal `sortProperties` flag in `BeanContext`, `BeanFilter`, `BeanSession`,
  and `BeanMeta` so sorted is now the default and unsorted is the opt-in.
- Added `@Bean(unsorted)` annotation attribute (replaces `@Bean(sort)`).
- Added `BeanContext.Builder.unsortedProperties()` and `unsortedProperties(Class<?>...on)`.
- Added `BeanContextable.Builder.unsortedProperties()` and `unsortedProperties(Class<?>...on)`.
- Added `BeanFilter.Builder.unsortedProperties()` and `BeanFilter.isUnsortedProperties()`.
- Added `BeanSession.isUnsortedProperties()`.
- Deprecated the old `sortProperties*` / `isSortProperties()` family with `@Deprecated(forRemoval=true)`.

## Phase 2 — Serializer/parser builder overrides

Added `unsortedProperties()` overrides across all ~55 serializer and parser builder classes
(JSON, XML, HTML, YAML, UON, URL Encoding, CSV, OpenAPI, MsgPack, CBOR, BSON, RDF, etc.)
so fluent builder chains remain type-safe.

Removed the corresponding `sortProperties()` overrides from those same builders.

## Phase 3 — Test and assertion updates

- Updated all test expected-output strings to match the new alphabetical default.
- Removed `DEFAULT_SORTED` static constants from serializer classes (now redundant).
- Updated test utilities (`TestUtils`, `BeanTester`) to use `MarshallUtils` static imports
  instead of `Json5.DEFAULT.write/read`.
- Fixed `BeanMeta.getPropertyMeta(null)` NPE (added null guard for `TreeMap.get(null)`).

## Phase 4 — Removed deprecated API

### `@Bean(sort)` / `BeanAnnotation.sort()`
- Removed `boolean sort() default false;` from `Bean.java`.
- Removed `Builder.sort(boolean)`, `Builder.sort` field, `Object.sort` field, and
  `Object.sort()` override from `BeanAnnotation.java`.
- Removed the `else if (x.sort()) unsortedProperties(false)` branch from `BeanFilter.Builder`.

### `sortProperties` methods on `BeanContext.Builder`
- Removed `sortProperties()`, `sortProperties(boolean)`, `sortProperties(Class<?>...on)`.
- Removed `isSortProperties()` deprecated getter.

### `sortProperties` methods on `BeanContextable.Builder`
- Removed `sortProperties()` and `sortProperties(Class<?>...on)`.

### `sortProperties` methods on `BeanFilter.Builder`
- Removed `sortProperties()` and `sortProperties(boolean)`.
- Removed `BeanFilter.isSortProperties()`.

### `isSortProperties()` on `BeanSession` and `BeanMeta`
- Removed the deprecated `isSortProperties()` getters.

### Serializer/parser builder `sortProperties()` overrides
- Removed all `@Override sortProperties()` and `@Override sortProperties(Class<?>...on)`
  methods from ~42 builder classes.

### `FluentObjectAssertion` and `JcsSerializer`
- Removed `.sortProperties()` chaining from `FluentObjectAssertion.JSON_SORTED`.
- Removed `.sortProperties()` from `JcsSerializer` builder chain.

## Phase 5 — `BeanMeta` internal rename

- Renamed `private final boolean sortProperties` → `private final boolean unsortedProperties`.
- Renamed local temp var `sortPropertiesTemp` → `unsortedPropertiesTemp`.
- Inverted the assignment logic: `unsortedPropertiesTemp = isUnsorted || bfo.isUnsorted || !fixedProps.isEmpty()`.
- Inverted map selection: `unsortedPropertiesTemp ? map() : sortedMap()`.
- Updated `isUnsortedProperties()` getter: `return unsortedProperties` (no longer `!sortProperties`).

## Phase 6 — `MarshallUtils` expansion

Added the full set of overloads to `MarshallUtils` for every format, matching the static API
on each marshaller class:

| Overload signature | Purpose |
|---|---|
| `xformat(Object, Object)` | Serialize to Writer / OutputStream / File / StringBuilder |
| `xformat(Object, Class<T>)` | Parse from any input (throws IOException) |
| `xformat(Object, Type, Type...)` | Parse from any input with parameterized type |
| `xformat(String, Type, Type...)` | Parse from String with parameterized type |
| `xformat(byte[], Type, Type...)` | Parse from bytes with parameterized type (binary formats) |

Also completed the static API on two marshaller classes that were missing overloads:
- **`Jcs`** — added `of(Object, Object)`, `to(Object, Class)`, `to(Object, Type, Type...)`,
  `to(String, Type, Type...)` (previously only had `of(Object)` and `to(String, Class)`).
- **`Parquet`** — added `of(Object, Object)`, `to(Object, Class)`, `to(Object, Type)`,
  `to(byte[], Type, Type...)` (the write-to-stream and generic-input variants).

Converted callers:
- `TestUtils.java` and `BeanTester.java`: `Json5.DEFAULT.write/read` → `json5(...)` with static import.
- `MarkdownSerializer_Test.java`, `MarkdownEdgeCases_Test.java`: `MarkdownSerializer.DEFAULT.serialize` /
  `MarkdownParser.DEFAULT.parse` → `Markdown.of` / `Markdown.to`.
- `MsgPackSerializerTest.java`: `MsgPackSerializer.DEFAULT.serialize` → `msgPack(...)`.
- `ProxyBeanTest.java`: multiple `XParser.DEFAULT.parse` calls → `MarshallUtils` static imports.
- `Records_RoundTripTest.java`: `Json5Serializer.DEFAULT.serialize` / `JsonParser.DEFAULT.parse`
  → `json5(...)` / `json(...)`.
