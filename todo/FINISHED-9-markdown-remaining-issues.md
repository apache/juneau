# Markdown Round-Trip: Remaining Issues (Finished)

This document tracks the tests that *were* skipped for the Markdown tester (tester [23] in
`RoundTripBeanMaps_Test`, tester [25] in `RoundTripAddClassAttrs_Test`) and the fixes that closed
each one. **All six issues are now closed and all round-trip tests pass.**

## Outcome summary

| Test(s) | Original Issue | Resolution |
|---------|----------------|------------|
| `BeanMaps.a04` | `implClass` not copied to embedded `Json5Parser` | Already worked — `marshallingContext((MarshallingContext) getContext())` shares the parent context (and hence the impl-class registry) with the embedded `Json5Parser`. Only the skip guard needed removal. |
| `BeanMaps.a05` | `@BeanIgnore` / getter-type mismatch | Already worked — the `needsJson5Path()` classification + shared bean context already routed `A` through the JSON5 path correctly. Only the skip guard needed removal. |
| `BeanMaps.a16, a18, a19` | Non-static inner classes (G/G1/G2, I/I1/I2, J/J2) need outer propagation | Already worked — the `parseRow()` / `parseKeyValueTable()` bean branches were already passing `m.getBean(false)` as the outer when recursing into nested cells, which is sufficient for the G/I/J cases. Only the skip guards needed removal. |
| `BeanMaps.a17` | Inner classes inside a `Map`-extending bean (H/H1/H2) need outer propagation through the map | **Production fix** — `parseKeyValueTable()`'s map branch and `parseRow()`'s map branch were passing `null` as the outer when parsing each value cell. They now pass the partially-built `map` instance, so a non-static inner class whose enclosing class **is** the map type (e.g. `H1` inside `H extends LinkedHashMap`) can resolve its synthetic `$outer` constructor argument correctly. |
| `BeanMaps.a22, a24` | Config-annotation `@WrapperAttr` not seen by embedded serializer | Already worked — same root cause as A3: the `marshallingContext((MarshallingContext) getContext())` plumbing carries config-annotation registrations into the embedded `Json5Serializer`. Only the skip guards needed removal. |
| `AddClassAttrs.a04–a07` | `_type` in multi-column tables not resolved for `Object` element type | Already worked — the existing `cast(m, null, eType)` call at the end of the Object branch in `parseRow()` already resolves `_type`-discriminated rows via the bean registry. Only the skip guards needed removal. |

## Fixed in this pass

### Production code

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/markdown/MarkdownParserSession.java`
  - `parseKeyValueTable()` (line ~392): pass the in-progress `map` as the outer when parsing each value cell, replacing the previous `null`.
  - `parseRow()` map branch (line ~563): same change for the multi-column path, for consistency.

### Test code (skip guards removed)

- `juneau-utest/src/test/java/org/apache/juneau/a/rttests/RoundTripBeanMaps_Test.java`
  - `a04_implMap`, `a05_implMap2`, `a16_memberClass`, `a17_memberClassWithMapClass`, `a18_memberClassWithListClass`, `a19_memberClassWithStringConstructor`, `a22_wrapperAttrAnnotationOnBean_usingConfig`, `a24_WrapperAttrAnnotationOnNonBean_usingConfig`: removed `if (isMarkdown(t)) return;` guards.
- `juneau-utest/src/test/java/org/apache/juneau/a/rttests/RoundTripAddClassAttrs_Test.java`
  - `a04_mapsWithTypeParams`: removed `if (isMarkdown(t)) return;` guard.
  - `a05_mapsWithoutTypeParams`, `a06_beanWithListProps`, `a07_beanWithListOfArraysProps`: removed the `|| isMarkdown(t)` part of the skip-guard expression while keeping the `isRdfStream(t)` skip.

## Verification

- `mvn -pl juneau-utest -am test -Dtest='RoundTrip*' -Drat.skip=true -Dsurefire.failIfNoSpecifiedTests=false` — 2264 tests pass, 0 failures, 0 errors (18 skipped from unrelated `RoundTripBeanChannel_Test` / `RoundTripLargeObjects_Test` guards that pre-date this work).
- `mvn -pl juneau-utest -am test -Dtest='*Markdown*' -Drat.skip=true -Dsurefire.failIfNoSpecifiedTests=false` — 116 Markdown-specific tests pass, 0 failures.
- `./scripts/test.py` — full clean build + test pass.

---

## Original issue analysis (kept for historical context)

---

## 1. Non-static inner classes — `a16`, `a17`, `a18` in `RoundTripBeanMaps_Test`

### Description
Classes `G`, `H`, and `I` contain non-static inner classes (`G1`, `G2`, `H1`, `H2`, `I1`, `I2`).
Non-static inner classes can only be instantiated with a reference to their enclosing outer instance.
During deserialization, the outer instance must be passed to the constructor.

### How JSON handles it
`JsonParserSession` threads the `outer` object through its recursive `parseAnything()` calls.
When it constructs a non-static inner class it passes `outer` to the constructor.

### Why Markdown fails
When the Markdown parser delegates complex bean types to the embedded `Json5Parser` via
`parseWithOuter(json5, type, outer)`, the `outer` object is passed correctly at the top level.
However, for deeply nested inner classes (e.g. `G1` inside `G`), the outer passed to the JSON5
parser is the root outer, not the intermediate bean instance that would be required to construct
`G1` (whose outer is an instance of `G`, not the session's `getOuter()`).

### Fix idea
When `needsJson5Path()` is true and the parser delegates to JSON5, the `outer` argument should be
the *parent bean instance* at each nesting level. The most direct fix is to pass the partially-built
bean as the outer when calling `parseWithOuter` for each nested property. This requires the
Markdown parser to be aware of the bean being constructed so it can pass the right outer at each
recursive step — essentially tracking the "current bean instance" stack during parse. Alternatively,
`JsonParserSession.parseAnything()` could be made `protected` so `MarkdownParserSession` can
subclass and override just the outermost dispatch, letting the JSON5 machinery handle the rest
(including inner-class outer propagation) natively.

---

## 2. Member class with string constructor — `a19` in `RoundTripBeanMaps_Test`

### Description
Class `J` contains a non-static inner class `J2` that has a `String` constructor:
```java
public class J2 {
    public J2(String v) { a2 = Integer.parseInt(v); }
}
```
`J2` is serialized as a simple value (its `toString()` form), which is a numeric string like `"2"`.

### Why Markdown fails
The Markdown serializer wraps numeric-looking strings in JSON5 backtick syntax (e.g. `` `'2'` ``)
to prevent auto-detection as a number during parsing. When the parser sees `` `'2'` `` it delegates
to the JSON5 parser with `J2.class` as the target type. The JSON5 parser then tries to construct a
`J2` from the string `"2"` but `J2` is a non-static inner class, so its constructor requires an
outer `J` instance that is not available in that context.

### Fix idea
The same outer-propagation fix from issue #1 would help here. Additionally, the serializer could
detect non-static inner classes with string constructors and emit the value *without* the
backtick wrapping (relying on `convertToType` rather than the JSON5 parser), which already handles
`fromString`/`valueOf`/`String`-constructor lookup and passes the correct `outer` object.

---

## 3. `implClass` on `Map` values — `a04` in `RoundTripBeanMaps_Test`

### Description
`Map<String, IBean>` where the declared value type is an interface (`IBean`) but the actual
implementation class is `CBean`. The serializer records the concrete type via `_type`; the parser
uses `implClass` or the bean dictionary to resolve the interface to the concrete class.

### Why Markdown fails
When parsing a `Map<String, IBean>`, each map value cell may be backtick-wrapped JSON5 (because the
value is a complex bean). The `parseWithOuter` call receives the *declared* value type `IBean`, but
resolving `IBean` → `CBean` requires the parser session's `implClass` configuration to be consulted
first. The embedded `Json5Parser` instance does not automatically inherit `implClass` mappings from
the Markdown parser session.

### Fix idea
When constructing the embedded `Json5Parser` in `getJson5Parser()`, copy the `implClass` mappings
from the current Markdown parser/session into the Json5Parser builder:
```java
Json5Parser.create().marshallingContext((MarshallingContext) getContext())
    .implClasses(getImplClasses())
    .build();
```
This would allow the JSON5 parser to resolve interface-to-implementation mappings at inline-value
parse time.

---

## 4. `@BeanIgnore` / getter-only properties — `a05` in `RoundTripBeanMaps_Test`

### Description
Class `A` has a property where one getter is annotated `@BeanIgnore` and a second getter (with
`@Beanp(name="a")`) is the intended readable property, while the setter accepts a different type.
The test verifies that Juneau's property-matching logic selects the correct getter/setter pair.

### Why Markdown fails
The precise mechanism is still TBD (the skip comment says "@BeanIgnore / getter-only properties").
Most likely, when `needsJson5Path()` is true for the bean, the delegated JSON5 round-trip works
fine for other serializers. For Markdown, the bean is serialized via `serializeBeanMap` (which
respects `@BeanIgnore` on `canRead()` checks), but on parse the reconstructed value does not match
because the property type exposed to the Markdown parser differs from what the setter expects.

### Fix idea
Investigate the exact failure with a targeted test. Likely the fix is to ensure `needsJson5Path()`
returns `true` for beans with ambiguous getter/setter type mismatches, so the full JSON5 round-trip
path is used rather than the Markdown-native property-setting path.

---

## 5. `@WrapperAttr` with config annotation — `a22`, `a24` in `RoundTripBeanMaps_Test`

### Description
Classes `L2` (bean) and `M2` (non-bean with `valueOf`) have `@Json(wrapperAttr="foo")` applied via
a config annotation class rather than directly on the target class. The JSON serializer wraps the
object in `{"foo": ...}` on output. The JSON parser sees the wrapper key and unwraps it.

### Why Markdown fails (config-annotation variant only)
`a21` (`L` with `@Json` directly on class) and `a23` (`M` with `@Json` directly) already pass —
the Markdown serializer detects `wrapperAttr` via `getJsonClassMeta(sType).getWrapperAttr()` and
the embedded JSON5 path handles the wrapping.

For the *config-annotation* variants (`L2`, `M2`), the `@Json(on="L2", ...)` annotation is
applied via a separate config class. The Markdown serializer's embedded `Json5Serializer` instance
is constructed without the bean context configuration that registers this config annotation, so it
does not see the `wrapperAttr` setting and serializes the bean without the wrapper. On parse, the
parser then receives an unwrapped bean where a wrapped one is expected.

### Fix idea
Ensure the embedded `Json5Serializer` in `getJson5Serializer()` is built from the full bean context
(including all config-annotation registrations), not just the base `MarshallingContext`. Since the
Markdown session's `getContext()` already carries the resolved bean context, passing it as-is
(which we do via `.marshallingContext((MarshallingContext) getContext())`) *should* carry config annotations.
The actual root cause may be more subtle — it is worth running the test with added diagnostics to
confirm whether the `wrapperAttr` is visible to the embedded serializer or not, then adjusting the
`Json5Serializer` builder to copy the relevant setting explicitly if needed.

---

## 6. `addBeanTypes` on arrays/lists with untyped element type — `a04`–`a07` in `RoundTripAddClassAttrs_Test`

### Description
These tests verify that when `addBeanTypes` is enabled, beans stored in arrays, lists, or maps
declared as `Object[]` / `List<Object>` / `Map<String, Object>` carry a `_type` discriminator
so the parser can reconstruct the concrete type without any declared type hint.

### Why Markdown fails
In Markdown, multi-column tables are used for collections of beans. When elements are typed as
`Object`, the `_type` column is emitted per-row. However, when parsing back, the element type
is `Object.class`, so `parseRow()` / `parseMultiColumnTable()` falls through to returning a
`JsonMap` rather than resolving the `_type` column to the original concrete class.
The `cast(resultMap, null, eType)` call at the end of `parseKeyValueTable` only fires for
key-value tables, not for multi-column tables, and `eType` is `Object` so there is no bean
registry to consult.

### Fix idea
In `parseMultiColumnTable()` and `parseRow()`, after a row is assembled as a `JsonMap`,
check for a `_type` key in the assembled map and attempt to resolve it via:
1. The session's `getBeanRegistry()` (which carries the globally registered bean dictionary)
2. `cast(resultMap, null, objectType)` — which already does this for key-value tables

Applying the same `cast` logic after each row-level `JsonMap` is built in the multi-column path
would allow `_type`-discriminated rows to be resolved to their concrete types. The bean registry
must be populated via `@Bean(typeName=...)` on the target classes and enabled on the Markdown
serializer/parser via `addBeanTypes()` / `addRootType()`.

---

## Summary Table

| Test(s) | Issue | Effort |
|---------|-------|--------|
| `BeanMaps.a16–a18` | Non-static inner classes need outer propagation through JSON5 delegation | Medium — requires outer-stack tracking or `JsonParserSession` inheritance |
| `BeanMaps.a19` | Non-static inner class with string constructor | Low — same outer-propagation fix; or detect and bypass backtick wrapping |
| `BeanMaps.a04` | `implClass` not copied to embedded `Json5Parser` | Low — add `.implClasses(...)` when building the embedded parser |
| `BeanMaps.a05` | `@BeanIgnore` / getter-type mismatch | Low-Medium — investigate exact failure; likely a `needsJson5Path` classification fix |
| `BeanMaps.a22, a24` | Config-annotation `@WrapperAttr` not seen by embedded serializer | Low-Medium — verify bean context propagation; may just need a targeted copy of `wrapperAttr` |
| `AddClassAttrs.a04–a07` | `_type` in multi-column tables not resolved for `Object` element type | Medium — apply the existing `cast()` logic to the multi-column table row assembly path |
