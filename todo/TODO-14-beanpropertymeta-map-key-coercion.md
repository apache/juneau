# TODO-14 — Close the latent map-key coercion gap in `BeanPropertyMeta.setPropertyValue`

Source: promoted from **Open Question 6** of `todo/TODO-57-format-round-trip-tests.md` after the Bug #7b per-parser fixes (Hjson / Hocon / Proto / Bson) rendered the underlying commons-side gap unreachable from any tested parser. Filed 2026-05-22 to track defense-in-depth closure of the commons-side site so a fifth parser surfacing with the same shape doesn't need a fifth per-parser fix.

---

## 1. Background / context

### The exact site

`juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java` — inside the private `setPropertyValue(BeanMap, String, Object, Object, boolean, boolean, BeanSession)` method (declared at line 1119), the typed-`Map<K, V>` branch:

- **`needsConversion` predicate (lines 1156–1163).** A `Flag` is set if *any* entry value `v2` fails `valueType.isInstance(v2)`; the predicate is **never** consulted against entry keys. If only the key needs coercion, the flag stays clear and `convertToType` is not called.

  ```1156:1163:juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java
  								var needsConversion = Flag.create();
  								valueMap.forEach((k, v2) -> {
  									if (nn(v2) && ! valueType.isInstance(v2)) {
  										needsConversion.set();
  									}
  								});
  								if (needsConversion.isSet())
  									valueMap = (Map)session.convertToType(valueMap, rawTypeMeta);
  ```

- **Value-only coercion loop (lines 1180–1188).** The second `forEach` that handles the writable-property path converts each `v1` against `valueType` but never inspects `k1` against the property's key type. The key is forwarded to `propMap2.put(k1, v1)` as-is.

  ```1180:1188:juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java
  				// Set the values.
  				var propMap2 = propMap;
  				valueMap.forEach((k1, v1) -> {
  					if (! valueType.isObject())
  						v1 = session.convertToType(v1, valueType);
  					propMap2.put(k1, v1);
  				});
  				if (nn(setter) || nn(field))
  					invokeSetter(bean, pName, propMap);
  ```

Both half-decisions need to inspect the entry key against `rawTypeMeta.getKeyType()` (already available — `ClassMeta` exposes a public `getKeyType()` at line 642 of `ClassMeta.java`) and trigger the symmetric coercion path when the key type doesn't match.

### Why the gap exists

The `needsConversion` predicate was written when bean properties were predominantly `Map<String, V>`, where the key is always a `String` produced by the parser tokenizer and never needs coercion. When typed-`K` map properties (e.g. `Map<TestEnum, String>`) were added, the value-side branch was extended to thread the parameterized `V` type through `valueType.isInstance(v2)`, but the matching key-side branch was never added. The result: a parser that hands a fully-built `Map<String, V>` to `BeanPropertyMeta.setPropertyValue` against a `Map<TestEnum, V>`-typed bean property gets the values coerced but **not** the keys — the bean ends up holding a map whose `keySet()` contains the wire-form `String`s instead of `TestEnum` instances.

### Why it's currently unreachable

Each of the four parsers that exhibited the symptom in the round-trip matrix was fixed at the parser-specific dispatch site for Bug #7b in TODO-57:

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hjson/HjsonParserSession.java` — `propertyType` widened to surface the typed `ClassMeta<K>` so key coercion happens at the parser's key-conversion site (`convertAttrToType` analog).
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconParserSession.java` — `hoconToMap` now threads the key type through to its own key-coercion step.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoParserSession.java` — `convertValue` / `convertMapToType` propagate the key type to the per-key coercion call.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/bson/BsonParserSession.java` — `parseDocument`'s `convertAttrToType` consumes the typed key class.

For comparison, the JSON family already gets this right because `JsonParserSession.parseIntoMap2` (line 559 of `JsonParserSession.java`) accepts `ClassMeta<K> keyType` as a method parameter and calls `convertAttrToType(m, currAttr, keyType)` at line 598 *before* the entry ever lands in the bean property. The map handed to `setPropertyValue` already has `TestEnum` keys, so the commons-side gap is never hit.

Net: every existing parser either (a) handles key coercion before the map reaches `setPropertyValue` (JSON family + the four parsers fixed for Bug #7b) or (b) doesn't need to because the property type is `Map<String, V>` (the common case).

### The systemic risk

The gap is latent, not dead. A future parser implementation — or a regression in any of the five parsers above — that produces a `Map<String, V>` for a typed-`Map<K, V>` property would reproduce the symptom and require *another* per-parser fix. The fix-here-or-fix-the-commons-side decision was deliberately deferred during the Bug #7b closure on the grounds that:

- The four parser sites were simpler than the commons-side change (each parser already had a per-entry key conversion site, so the edit was localized).
- A commons-side change risks regressing the JSON family because the JSON family's parser-level conversion already runs; a commons-side coercion that fires on a key that's already the correct type is a no-op but still adds a `convertToType` call to a hot path.
- The matrix wasn't surfacing it from the JSON family anyway.

This plan revisits that deferral as a defense-in-depth follow-on, not as a correctness fix.

---

## 2. Scope

### In scope

- Single fix site: the typed-`Map<K, V>` branch of `setPropertyValue` in `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java`.
- Extend the `needsConversion` predicate (lines 1156–1163) to also flag when any non-null entry key fails `keyType.isInstance(k)` (with `keyType = rawTypeMeta.getKeyType()`).
- Add a symmetric key-side `convertToType` call in the value-loop at lines 1180–1186 — `if (! keyType.isObject()) k1 = session.convertToType(k1, keyType);` — mirroring the existing value-side pattern.
- A new unit test in `juneau-commons` that reproduces the gap *without going through any parser* (see Phase 1).
- A brief audit pass for sibling-shape gaps elsewhere in `BeanPropertyMeta` (see Phase 4).

### Out of scope

- The four per-parser Bug #7b fixes — they stay in place. The commons-side fix is a backstop, not a replacement.
- Other latent commons-side type-coercion gaps (those go in their own follow-up plans).
- Generic-arity changes to `BeanPropertyMeta` — fix is limited to the `Map<K, V>` key inspection.
- Sibling shapes like `Set<E>` / `Iterable<E>` / generic-typed arrays — those are flagged in Open Questions but not part of this plan's initial fix.

---

## 3. Phases

### Phase 1 — Reproduce the symptom in a commons-side unit test

Land a new unit test under `juneau-utest` (or `juneau-commons`'s own test source if a closer test home exists) that:

1. Defines a small bean with a `Map<TestEnum, String>`-typed property (with setter or public field — both shapes; the writable-property path and the abstract-property path go through different branches of `setPropertyValue`).
2. Builds a `BeanMap` for the bean from a `BeanSession`.
3. Calls `BeanMap.put(propertyName, Map.of("FOO", "v1", "BAR", "v2"))` — i.e. hands a `Map<String, String>` directly to the bean property, bypassing every parser.
4. Asserts that the bean's `Map<TestEnum, String>` getter returns a map whose `keySet()` contains `TestEnum` instances, not `String`s.

The test must **fail** on the current commons-side code (proving the gap is real) and **pass** after the Phase 2 fix. Use a `TestEnum` defined in the test's own scope to avoid coupling the unit test to a real format swap.

Place the test next to existing `BeanPropertyMeta` coverage — search `juneau-utest/src/test/java/org/apache/juneau/commons/bean/` (or the equivalent commons-tests root) for `BeanPropertyMeta_*Test` files to identify the conventional location.

### Phase 2 — Implement the commons-side fix

Edit `BeanPropertyMeta.setPropertyValue` to:

1. **Extend the `needsConversion` predicate (lines 1156–1163).** Pull `rawTypeMeta.getKeyType()` into a local `keyType`. The `forEach` body extends to also set the flag when `nn(k) && ! keyType.isInstance(k)`. Skip the key check if `keyType.isObject()` (the wildcard / abstract-key case — same shape as the existing `! valueType.isObject()` guard).
2. **Extend the value-loop coercion (lines 1180–1186).** Inside the second `forEach`, mirror the value-side coercion for the key: `if (! keyType.isObject()) k1 = session.convertToType(k1, keyType);`. Then call `propMap2.put(k1, v1)` with the coerced key. (`forEach` lambdas can't reassign captured locals — this likely needs to switch to an explicit `for (var e : valueMap.entrySet())` iteration, the same shape used in the `Collection`-branch's `ListIterator` block at lines 1213–1221.)

Both changes are surgical and the existing `convertToType` plumbing is already wired through `session`. No new `ClassMeta` API, no `BeanSession` change, no signature change on `setPropertyValue`.

### Phase 3 — Regression check against the four Bug-#7b parsers

Run the four parsers that received per-parser Bug #7b fixes:

```bash
mvn -pl juneau-utest -am -Dtest='Hjson*Test,Hocon*Test,Proto*Test,Bson*Test' test
```

Expected outcome: green. The per-parser fixes still fire at the parser level (they convert keys before the map reaches `setPropertyValue`), so the commons-side fix is a no-op for those parsers — `keyType.isInstance(k)` returns true, the flag stays clear, the second forEach skips the key-side `convertToType`.

Optional follow-on (defer unless verified clean): consider whether the per-parser fixes can be *simplified* now that the commons-side gap is closed. Default disposition is to **leave them in place** since:

- They're working, well-tested, and have already shipped through the round-trip matrix.
- Removing them shifts the conversion cost from parse-time (early) to bean-property-assignment-time (later) — which is the symmetric cost question discussed in Open Question 1 below.
- A regression in any per-parser fix would now manifest as a silent commons-side coercion rather than a clean parser-level test failure.

Only revisit the per-parser fixes if a measurable performance win is demonstrated by Open Question 1's microbenchmark.

### Phase 4 — Sibling-shape audit pass

Read through `BeanPropertyMeta.setPropertyValue` and `BeanPropertyMeta.set` (the public entry point) looking for other places where the code inspects entry-value types but not entry-key types — or, more broadly, where a parameterized type is consulted in one direction but not the other. Candidates to look at explicitly:

- The `Collection` branch (lines 1190–1247) — element type is inspected; not parameterized (no key analog), so likely fine.
- The `BeanPropertyMeta.set` overload that takes a `Object` directly (above line 1119) — confirm it dispatches through the same `setPropertyValue` site and doesn't have its own parallel logic.
- The dyna-property branch (`isDyna` near line 1097) — confirm it routes through the same conversion machinery or document why it doesn't.

Output of the audit pass: either (a) "no additional sibling gaps found" recorded in this plan, or (b) a list of follow-up TODO items for any sibling shapes that need their own plans.

---

## 4. Open questions

1. **Performance impact.** The existing per-parser Bug #7b fixes coerce keys at parse time using each parser's existing `convertAttrToType` (or analog), which is a tight, format-specific path. The commons-side fix coerces keys *after* the parser has already produced a `Map<String, V>` — adds a second iteration over the map's entries. Worth measuring with a JMH microbenchmark on a typed `Map<Enum, V>` bean property to confirm the impact is negligible. Hypothesis: for maps with `< ~100` entries (the common case for bean properties), the second iteration's cost is dominated by the `convertToType` call itself, which already runs for every value-type-mismatched entry, so the marginal cost is a `keyType.isInstance(k)` check per entry — likely < 5 % overhead. For very large maps the cost grows linearly; might motivate keeping the per-parser fix as the primary path and the commons-side fix as a backstop only.
2. **Backwards compatibility.** The commons-side fix changes the *observable semantics* of `BeanPropertyMeta.setPropertyValue` for any caller that was passing a wire-form-typed `Map` (e.g. a custom parser implementation) and *intentionally* leaving keys as `String`s because the downstream consumer was prepared for that shape. We don't know of any such caller, but the behavior change is potentially observable. Worth considering: (a) a deprecation cycle with a `MarshalledConfig` opt-in flag (e.g. `coerceTypedMapKeys()`), (b) a `@Beta` annotation on the fix, or (c) trust the new test matrix and ship it as a straight semantic fix. Default disposition: straight semantic fix — the pre-fix behavior is a bug, not a contract. Revisit if a downstream test surfaces a regression.
3. **Scope of `needsConversion`.** Should the same inspection extend to:
   - `Set<E>` element types? (probably yes — same shape, single parameterized type per collection)
   - `Iterable<E>` element types? (probably yes — but only if the bean property declares a typed `Iterable`, which is rare)
   - Generic-typed arrays (`T[]` where `T` is bound to a type variable)? (probably yes — but uncovered by any existing test)
   The plan flags these as Phase 4 audit candidates. The initial fix is intentionally narrow to `Map<K, V>` keys; sibling shapes are tracked separately to keep this plan reviewable.
4. **`keyType.isObject()` vs. raw-type detection.** The value-side guard is `! valueType.isObject()`. For map keys, the analogous wildcard case is `Map<?, V>` or a raw `Map`. Need to confirm that `rawTypeMeta.getKeyType()` returns a `ClassMeta` whose `isObject()` is true in both wildcard and raw-map cases (and not, say, `null`). Read `ClassMeta.getKeyType()` (line 642 of `ClassMeta.java`) and trace its behavior for a raw `Map` declaration before relying on the same guard shape.
5. **Lambda vs. for-loop refactor.** Phase 2 notes that the second `forEach` likely needs to switch to an explicit `for (var e : valueMap.entrySet())` to allow reassigning `k1`. Worth checking whether the existing `forEach` was load-bearing (e.g. for a specific `Map` implementation's iteration semantics) before refactoring. Default disposition: switch to the explicit `for`-loop — it's the same shape the `Collection` branch already uses.

---

## 5. Acceptance criteria

- New commons-side unit test (Phase 1) reproduces the gap on pre-fix code, passes on post-fix code.
- All four Bug-#7b parsers stay green: `mvn -pl juneau-utest -am -Dtest='Hjson*Test,Hocon*Test,Proto*Test,Bson*Test' test`.
- `EnumFormat_RoundTrip_Test` matrix stays at **2268 / 0** (the headline `EnumFormat` round-trip count after Wave-3 Bug #7b closure in TODO-57).
- `./scripts/test.py` clean across the rest of the suite.
- Phase 4 audit pass produces a written summary appended to this plan (either "no sibling gaps" or a list of follow-up TODO items).

---

## 6. Out of scope

- The four per-parser Bug #7b fixes remain in place — see Phase 3 disposition.
- Other latent commons-side type-coercion gaps — each gets its own follow-up TODO.
- Generic-arity changes to `BeanPropertyMeta` — fix is limited to the `Map<K, V>` key inspection.
- Performance optimization of `convertToType` itself — orthogonal.
- Sibling shapes (`Set<E>`, `Iterable<E>`, generic-typed arrays) — flagged under Open Question 3, but each gets its own plan if Phase 4 surfaces them.

---

## 7. Related plans / references

- **`todo/TODO-57-format-round-trip-tests.md`** — parent matrix, particularly:
  - **Bug #7b** (around lines 220–233) — the four parser-side fixes that render this commons-side gap unreachable from any tested parser.
  - **Open Question 6** (around line 703) — the latent-gap flag this plan exists to close.
- **The four per-parser Bug #7b fix sites** (for reference, not to be modified):
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hjson/HjsonParserSession.java` — `propertyType` widening + key-coercion thread-through.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconParserSession.java` — `hoconToMap` type-threading.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoParserSession.java` — `convertValue` / `convertMapToType` key type propagation.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/bson/BsonParserSession.java` — `parseDocument` `convertAttrToType` with typed key class.
- **The JSON-family working pattern** (for comparison):
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json/JsonParserSession.java` — `parseIntoMap2` (line 559) accepts `ClassMeta<K> keyType` and calls `convertAttrToType(m, currAttr, keyType)` at line 598 before the entry lands in the bean property.
- **`todo/TODO-30-classmeta-to-commons.md`** — pending move of `ClassMeta` itself into commons. If that move lands first, the `rawTypeMeta.getKeyType()` call in this plan's fix is unchanged (the API surface is the same on either side of the move); if this plan lands first, no coordination needed.
- **`todo/TODO-5` (referenced in TODO-30)** — the bean-runtime types move that brought `BeanPropertyMeta` into `juneau-commons`. The site this plan modifies is post-TODO-5, in its new commons-side home.
