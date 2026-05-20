# TODO-48: Replace `null`-on-empty-input with empty-instance return on marshalled-collection factories

Source: created on 2026-05-20 from a SonarLint walkthrough of four `// TODO` `java:S1168` suppressions across `JsonList` / `Json5List` static factories. User direction: **return empty objects** instead of `null`, **consistent across every `MarshalledMap` / `MarshalledList` subclass**, without breaking existing callers.

## Goal

Drop the four `// TODO: ... Consider empty ...` suppressions on `JsonList` / `Json5List` static factory methods by changing them to return a fresh empty instance instead of `null` when the input is null / empty. Apply the same policy symmetrically across all `ofString(...)` overloads on every `MarshalledList` / `MarshalledMap` subclass — including the analogous spots on `JsonMap` / `Json5Map` / `MarshalledMap` / `MarshalledList` that do not carry the `// TODO` marker today but follow the identical null-in-null-out pattern.

In short:

- `ofString(CharSequence | Reader)` and `ofString(...,Parser)` factories: null/empty input now yields `new JsonList()` / `new JsonMap()` / `new Json5List()` / `new Json5Map()` / `new MarshalledList()` / `new MarshalledMap()`, never `null`.
- `JsonList.ofJsonOrCdl(String)` and `Json5List.ofJson5OrCdl(String)`: empty/null input now yields `new JsonList()` / `new Json5List()`.
- All four `java:S1168` suppressions and TODO comments on the flagged sites are removed.
- `of(Collection)` / `of(Map)` factories keep their current null-in-null-out behavior — see Out of scope.

## Why now

- Four `// TODO` markers in the `juneau-marshall` module flag this exact change to SonarLint; the suppressions are intentionally temporary placeholders awaiting a decision.
- Several internal callers already crash on the `null` return today (NPE on `m.putAll(null)` in `BeanMapLoader`, NPE on `Json5Map.ofString(value).session(this)` in `MarshallingSession#parseToMap`). Returning an empty instance turns these latent NPEs into no-ops without changing any successful path.
- The "empty rather than null" idiom is already established in the codebase (`JsonList.EMPTY_LIST`, `JsonMap.EMPTY_MAP`, `MarshalledList.EMPTY_LIST`, `MarshalledMap.EMPTY_MAP`, `opte()` helper, the `getList(key, createIfNotExists)` / `getMap(key, createIfNotExists)` family). Aligning the parser-factory contract with that idiom removes a small but persistent inconsistency.
- The change is locally bounded: all sites live under `org.apache.juneau.collections` and `org.apache.juneau.json5`; no public-API rename, no removed method, only a tightened return contract (non-null instead of `@Nullable`).

## Current behavior (per-file inventory)

All four flagged sites return `null` when their input is null (or empty, for the `OrCdl` variants). The Javadoc on every site explicitly says "or `null` if the input was `null`". The `@SuppressWarnings({"java:S1168"})` annotation + `// TODO` comment marks each one as awaiting this decision.

### Sites flagged with `// TODO` (the four in the user's request)

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json5/Json5List.java`
  - **L214-219** — `ofString(Reader json5)`: returns `null` when `json5 == null`. Javadoc: "A new list or `null` if the input was `null`."
  - **L231-240** — `ofJson5OrCdl(String s)`: returns `null` when `Utils.e(s)` (empty *or* null). Javadoc: "The parsed string." (does not document null return.)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonList.java`
  - **L252-257** — `ofString(Reader json)`: returns `null` when `json == null`. Javadoc: "A new list or `null` if the input was `null`."
  - **L269-278** — `ofJsonOrCdl(String s)`: returns `null` when `Utils.e(s)`. Javadoc: "The parsed string."

### Symmetric sites that do **not** carry a `// TODO` marker today (in scope for consistency)

These follow the identical null-in-null-out pattern; per the user's "consistent across every subclass" requirement they need to move with the flagged sites.

- `Json5List.ofString(CharSequence json5)` — L203-205
- `Json5List.ofString(CharSequence in, Parser p)` — L250-252
- `Json5List.ofString(Reader in, Parser p)` — L265-267
- `JsonList.ofString(CharSequence json)` — L239-241
- `JsonList.ofString(CharSequence in, Parser p)` — L292-294
- `JsonList.ofString(Reader in, Parser p)` — L311-313
- `Json5Map.ofString(CharSequence json5)` — L204-206
- `Json5Map.ofString(Reader json5)` — L216-218
- `Json5Map.ofString(CharSequence in, Parser p)` — L232-234
- `Json5Map.ofString(Reader in, Parser p)` — L247-249
- `JsonMap.ofString(CharSequence json)` — L238-240
- `JsonMap.ofString(Reader json)` — L251-253
- `JsonMap.ofString(CharSequence in, Parser p)` — L267-269
- `JsonMap.ofString(Reader in, Parser p)` — L283-285
- `MarshalledList.ofString(CharSequence in, Parser p)` — L200-202
- `MarshalledList.ofString(Reader in, Parser p)` — L215-217
- `MarshalledMap.ofString(CharSequence in, Parser p)` — L193-195
- `MarshalledMap.ofString(Reader in, Parser p)` — L208-210

### Sites that intentionally stay null-in-null-out (Out of scope — see that section)

- `JsonList.of(Collection<?>)`, `Json5List.of(Collection<?>)`, `MarshalledList.of(Collection<?>)`
- `JsonMap.of(Map<?,?>)`, `Json5Map.of(Map<?,?>)`, `MarshalledMap.of(Map<?,?>)`

## Proposed behavior (per-file changes)

For every `ofString(...)` overload (and the two `OrCdl` factories): replace the `null` branch with `new <Type>()`, drop the `// TODO` comment, drop the `@SuppressWarnings({"java:S1168"})` annotation when it becomes empty, and rewrite the `@return` Javadoc to drop the "or null" language.

Concrete shape (illustrative — exact code is the implementation TODO's responsibility):

Before (`Json5List.ofString(Reader)`):

```java
@SuppressWarnings({
    "java:S1168"     // null input = null output by design. Consider empty Json5List.
})
public static Json5List ofString(Reader json5) throws ParseException {
    return json5 == null ? null : new Json5List(json5);
}
```

After:

```java
public static Json5List ofString(Reader json5) throws ParseException {
    return json5 == null ? new Json5List() : new Json5List(json5);
}
```

Before (`JsonList.ofJsonOrCdl(String)`):

```java
@SuppressWarnings({
    "java:S1168"     // null for empty input. Consider empty JsonList.
})
public static JsonList ofJsonOrCdl(String s) throws ParseException {
    if (Utils.e(s))  // NOAI
        return null;
    if (! isProbablyJsonArray(s, true))
        return new JsonList((Object[])splita(s.trim(), ','));
    return new JsonList(s);
}
```

After:

```java
public static JsonList ofJsonOrCdl(String s) throws ParseException {
    if (Utils.e(s))  // NOAI
        return new JsonList();
    if (! isProbablyJsonArray(s, true))
        return new JsonList((Object[])splita(s.trim(), ','));
    return new JsonList(s);
}
```

The same shape applies symmetrically to every site listed in **Current behavior**.

### "Empty" means a fresh, mutable instance — not the shared `EMPTY_LIST` / `EMPTY_MAP` singleton

The codebase already exposes `JsonList.EMPTY_LIST`, `JsonMap.EMPTY_MAP`, `Json5List.EMPTY_LIST`, `Json5Map.EMPTY_MAP`, `MarshalledList.EMPTY_LIST`, `MarshalledMap.EMPTY_MAP` — but every one of those is an **unmodifiable** anonymous subclass that throws on `add` / `put` / `remove`. Callers of the parser factories almost always mutate the result:

- `BeanMapLoader.load(BeanMap, String)` does `m.putAll(Json5Map.ofString(input))`.
- `MarshallingSession.parseToMap(CharSequence)` does `Json5Map.ofString(value).session(this)`, which internally writes the `session` field on the returned map.
- `SchemaUtils.parseMap(...)` wraps via `new JsonMap(Json5Map.ofString(s))` — works either way, but the inner instance would be needlessly read-only.
- Tests routinely do `Json5Map.ofString("{a:'b'}").put(...)`-style mutation.

Returning the shared singleton would silently break every one of these the first time the null branch was hit. Fresh-instance allocation is the only safe choice; the allocation cost is negligible because the null branch is hit only on null/empty input, which by definition is not a hot path.

## Caller impact assessment

Project-wide audit of every public call site (`Grep` across `*.java` for `JsonList.ofString(`, `JsonMap.ofString(`, `Json5List.ofString(`, `Json5Map.ofString(`, `MarshalledList.ofString(`, `MarshalledMap.ofString(`, `JsonList.ofJsonOrCdl(`, `Json5List.ofJson5OrCdl(`):

### Safe — no observable change

| Site | Why safe |
| --- | --- |
| `SchemaUtils.parseMap(Object)` L81 → `new JsonMap(Json5Map.ofString(s))` | `s` is guaranteed non-empty by guards at L75-79. Null branch unreachable. |
| `SchemaUtils.parseMap(String[])` L108 → same as above | Same guards at L101-105. Unreachable. |
| `SchemaUtils.parseSet(String[])` L128 → `Json5List.ofJson5OrCdl(s).forEach(...)` | `s` is guaranteed non-empty at L125. Unreachable. After the change, the call becomes NPE-free even if a future caller removes the guard — strict improvement. |
| `Entry.asList(Parser)` L354 → `opt(JsonList.ofString(s, parser))` | `s = toString()`, non-null. Null branch unreachable. `opt(emptyList)` → `Optional.of(emptyList)` would only matter if `s` could become null, which it cannot. |
| `Entry.asMap(Parser)` L428 → `opt(JsonMap.ofString(s, parser))` | Same as above. |
| `BasicSwaggerProviderSession.parseListOrCdl(...)` L1042 → `Json5List.ofJson5OrCdl(s)` | Guarded by `o == null` (L1036) and `s.isEmpty()` (L1039). Unreachable. |
| ~200 test-only call sites (`MsgPackSerializerTest`, `OpenApiPartSerializer_Test`, `JsonMap_Test`, `UonSerializer_Test`, …) | All pass literal non-null non-empty strings. Null branch unreachable. |

### Affected — semantic change, but the new behavior is the desired one

| Site | Today | After |
| --- | --- | --- |
| `BeanMapLoader.load(BeanMap<T>, String input)` L53 → `m.putAll(Json5Map.ofString(input))` | NPE if `input == null` (`putAll(null)` is undefined) | No-op when `input == null` |
| `BeanMapLoader.load(BeanMap<T>, Reader r, ReaderParser p)` L72 → `m.putAll(JsonMap.ofString(r, p))` | NPE if `r == null` | No-op when `r == null` |
| `MarshallingSession.parseToMap(CharSequence value)` L1394 → `Json5Map.ofString(value).session(this)` | NPE if `value == null` (Javadoc says "Must not be null" but no runtime check) | Returns an empty session-attached `Json5Map` |

None of these three has a runtime null-guard today, so the only callers that ever exercised the null path were ones that crashed. The new behavior turns three latent NPEs into well-defined no-ops. Worth flagging to James for sign-off, but no caller code needs changing.

### Test-only — needs updating

Four tests assert the current `null`-return contract directly:

| Test | Assertion |
| --- | --- |
| `juneau-utest/.../json5/Json5List_Test.java#a04_factoryOfStringNullInput` L52 | `assertNull(Json5List.ofString((CharSequence)null));` |
| `juneau-utest/.../json5/Json5Map_Test.java#a04_factoryOfStringNullInput` L60 | `assertNull(Json5Map.ofString((CharSequence)null));` |
| `juneau-utest/.../collections/MarshalledList_Test.java#a06_parseViaOfStringNullInput` L73 | `assertNull(MarshalledList.ofString((CharSequence)null, Json5Parser.DEFAULT));` |
| `juneau-utest/.../collections/MarshalledMap_Test.java#a06_parseViaOfStringNullInput` L73 | `assertNull(MarshalledMap.ofString((CharSequence)null, Json5Parser.DEFAULT));` |

Implementation must rewrite each to assert "non-null, empty, mutable":

```java
@Test void a04_factoryOfStringNullInput() throws Exception {
    var l = Json5List.ofString((CharSequence)null);
    assertNotNull(l);
    assertTrue(l.isEmpty());
    l.add("x");                    // confirm result is mutable, not the shared EMPTY_LIST singleton
    assertEquals(1, l.size());
}
```

No other test asserts the null contract on these factories (`Grep` for `assertNull\(.*\.ofString` returns exactly those four hits).

### Three sibling `java:S1168` `// TODO` suppressions in `SchemaUtils.java` (NOT in this TODO's scope)

For context only — `SchemaUtils.java` L65-67, L97-99, L118-120 carry the same SonarLint marker on `parseMap(Object)` / `parseMap(String[])` / `parseSet(String[])`. These return `null` to signal "no schema configured" and are read by downstream `null`-checking callers; flipping them to empty would change schema-resolution semantics in a non-obvious way. Recommend filing a separate TODO if the user wants those reconsidered.

## Cross-subclass consistency checklist

The change must land in every file below in one PR so the `MarshalledList` / `MarshalledMap` family stays uniform.

| File | Sites to update |
| --- | --- |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/MarshalledList.java` | `ofString(CharSequence, Parser)` L200-202; `ofString(Reader, Parser)` L215-217 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/MarshalledMap.java` | `ofString(CharSequence, Parser)` L193-195; `ofString(Reader, Parser)` L208-210 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonList.java` | `ofString(CharSequence)` L239-241; `ofString(Reader)` L252-257 **(flagged)**; `ofString(CharSequence, Parser)` L292-294; `ofString(Reader, Parser)` L311-313; `ofJsonOrCdl(String)` L269-278 **(flagged)** |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonMap.java` | `ofString(CharSequence)` L238-240; `ofString(Reader)` L251-253; `ofString(CharSequence, Parser)` L267-269; `ofString(Reader, Parser)` L283-285 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json5/Json5List.java` | `ofString(CharSequence)` L203-205; `ofString(Reader)` L214-219 **(flagged)**; `ofString(CharSequence, Parser)` L250-252; `ofString(Reader, Parser)` L265-267; `ofJson5OrCdl(String)` L231-240 **(flagged)** |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json5/Json5Map.java` | `ofString(CharSequence)` L204-206; `ofString(Reader)` L216-218; `ofString(CharSequence, Parser)` L232-234; `ofString(Reader, Parser)` L247-249 |

Subclasses that do **not** need source edits:

- `ResolvingMarshalledMap` (extends `MarshalledMap`) — defines no factory methods of its own; inherits the updated base behavior transparently.
- `DelegateList` / `DelegateMap` (extend `JsonList` / `JsonMap`) — same.
- `MarshalledList.UnmodifiableMarshalledList`, `MarshalledMap.UnmodifiableMarshalledMap`, `JsonList.UnmodifiableJsonList`, `JsonMap.UnmodifiableJsonMap`, `Json5List.UnmodifiableJson5List`, `Json5Map.UnmodifiableJson5Map` — instance wrappers, no static factories.
- The `EMPTY_LIST` / `EMPTY_MAP` anonymous singletons on every class — unrelated to the factory contract.

Javadoc updates required on every site:

- Drop `"or null if the input was null"` from the `@return` text.
- Reword to `"A new empty list/map if the input is null."` or `"A new list/map, never null."` to match the runtime contract.
- Drop the `<br>Can be <jk>null</jk>.` annotation on the parameter where it currently appears — the parameter is still nullable, but the return contract change makes the "can be null" guidance redundant. Optional, but consistent.

## Test plan

New tests (or expand existing `*_Test.java` files alongside the four updated assertions):

1. For every updated site, add a "null input ⇒ empty instance" assertion proving:
   - Result is non-null.
   - Result is empty (`isEmpty()` true).
   - Result is mutable (try `add(...)` / `put(...)`; expect success, not `UnsupportedOperationException`).
   - Result type is the precise subclass (`Json5List`, not just `JsonList` etc.).
2. For each `ofJsonOrCdl` / `ofJson5OrCdl` factory, add an explicit "empty-string input ⇒ empty mutable instance" assertion (separate from the null-input case, because `Utils.e(s)` treats both as equivalent).
3. Add a focused regression test in `BeanMapLoader_Test` (create the file if it does not exist — `Grep` should confirm) that calls `BeanMapLoader.load(beanMap, (String) null)` and asserts the bean map is left empty rather than throwing.
4. Add a focused regression test in `MarshallingSession_Test` for `parseToMap((CharSequence) null)` returning an empty session-attached `Json5Map`.
5. Run the existing `Json5List_Test`, `Json5Map_Test`, `JsonList_Test`, `JsonMap_Test`, `MarshalledList_Test`, `MarshalledMap_Test`, `BeanMap_Test`, `MarshalledConfig_Test`, `OpenApiPartSerializer_Test`, `UonSerializer_Test`, `MsgPackSerializerTest`, `ObjectSwap_Test`, `JsonSchemaBeanGenerator_Test` suites unchanged — all 200+ existing `Json*Map/List.ofString("...literal...")` call sites pass literal non-null non-empty strings and must continue to pass.
6. Coverage gate: `./scripts/coverage.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonList.java --run` should not drop coverage on the modified methods; ideally each `null`/empty branch becomes a covered line.

(Tests/coverage run during the implementation TODO; this plan TODO only enumerates them.)

## Out of scope

- **`of(Collection<?>)` / `of(Map<?,?>)` factories.** These are explicit "copy-from-existing-collection" entry points; a `null` argument here is most plausibly a programmer error caller-side, and changing them to return empty would hide that. SonarLint does not flag these methods either. Leave behavior unchanged.
- **The three `// TODO java:S1168` suppressions in `SchemaUtils.java` (L65-67, L97-99, L118-120).** Each one returns `null` to signal "no schema configured" — distinct semantic from "parsed an empty document". Separate decision, separate TODO if the user wants it.
- **`BeanMapLoader.load(...)` / `MarshallingSession.parseToMap(...)` defensive null guards.** Once the underlying factories never return `null`, these callers no longer need their own guards. Adding explicit guards would be redundant.
- **Constructor-side changes (`new JsonList((CharSequence) null)` etc.).** The constructors already tolerate null input gracefully via the `MarshalledList(CharSequence, Parser)` / `MarshalledMap(CharSequence, Parser)` paths (`if (nn(in))` / `if (ne(in))` guards). No edits needed there.
- **Release-notes entry.** This is a behavior-tightening of an undocumented "may return null" contract on internal-leaning static factories; not surprising enough to warrant a release-notes call-out. Confirm with James — if he wants it, the file is `juneau-docs/pages/release-notes/9.5.0.md` per `AGENTS.md` (under `### juneau-marshall`).
- **Updating SonarLint suppression on the three sibling `java:S1168` markers in `SchemaUtils.java`.** Out of scope per above.

## Open questions

These should be settled before implementation starts.

1. **Scope: only the four flagged sites, or every analogous site across the family?** The plan recommends "all symmetric `ofString(...)` + `OrCdl` factories on every subclass" because the user's "consistent across every `MarshalledMap` / `MarshalledList` subclass" wording implies family-wide uniformity. The alternative — fixing only the four `// TODO`-flagged sites — would leave six other overloads (one on `JsonMap.ofString(Reader)`, one on `Json5Map.ofString(Reader)`, plus all the `(CharSequence)` and `(..., Parser)` siblings) silently returning `null`, which fails the consistency goal.

2. **Fresh instance vs shared `EMPTY_LIST` / `EMPTY_MAP` singleton?** The plan recommends fresh `new <Type>()` per call because all three known affected callers (`BeanMapLoader`, `MarshallingSession#parseToMap`, plus `SchemaUtils.parseMap`'s inner copy) need a mutable result. Using the singletons would silently swap a `NullPointerException` for an `UnsupportedOperationException`. Confirm.

3. **Should the `of(Collection<?>)` / `of(Map<?,?>)` "copy" factories move too?** The plan recommends no — null-in null-out is a reasonable contract for an explicit copy operation, and SonarLint does not flag them. Confirm the asymmetry is acceptable.

4. **Should the base `MarshalledList.ofString(...)` / `MarshalledMap.ofString(...)` carry an explicit Javadoc note about the policy ("never returns null"), or rely on the type contract alone?** Recommend explicit one-line `@return A new list/map (empty if the input was null), never null.` on every site for clarity; it's the only durable signal once the `// TODO` comment is gone.

5. **Three sibling `// TODO java:S1168` suppressions in `SchemaUtils.java` (L65, L97, L118) — file as a separate TODO?** These are a related-but-distinct decision because the `null` there signals "no schema configured" rather than "parsed an empty value". Recommend a separate one-line TODO bullet rather than rolling into this work.

6. **Release-notes mention?** Behavior is wire-compatible (no serialized payload changes), library-internal-feeling, and the previous "null return" contract was barely documented. Plan recommends omitting from `9.5.0.md`. Confirm.

## References

- Flagged sites:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json5/Json5List.java` L214-219 (`ofString(Reader)`), L231-240 (`ofJson5OrCdl(String)`).
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonList.java` L252-257 (`ofString(Reader)`), L269-278 (`ofJsonOrCdl(String)`).
- Symmetric sites (full per-file index in **Cross-subclass consistency checklist** above):
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/MarshalledList.java`, `MarshalledMap.java`, `JsonList.java`, `JsonMap.java`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json5/Json5List.java`, `Json5Map.java`.
- Sibling subclasses confirmed to require no edits: `ResolvingMarshalledMap`, `DelegateList`, `DelegateMap`, plus all `Unmodifiable*` private inner classes.
- Existing "empty rather than null" precedent: `JsonList.EMPTY_LIST` (anon read-only), `JsonMap.EMPTY_MAP`, `Json5List.EMPTY_LIST`, `Json5Map.EMPTY_MAP`, `MarshalledList.EMPTY_LIST`, `MarshalledMap.EMPTY_MAP`; `JsonMap.getList(key, createIfNotExists)` family; the `opt(...)` / `opte()` helpers in `org.apache.juneau.commons.utils.Utils`.
- Affected (latent-NPE-becomes-no-op) callers:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanMapLoader.java` L53, L72.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingSession.java` L1394.
- Tests asserting today's `null` contract (must be rewritten):
  - `juneau-utest/src/test/java/org/apache/juneau/json5/Json5List_Test.java` L51-53.
  - `juneau-utest/src/test/java/org/apache/juneau/json5/Json5Map_Test.java` L59-61.
  - `juneau-utest/src/test/java/org/apache/juneau/collections/MarshalledList_Test.java` L72-74.
  - `juneau-utest/src/test/java/org/apache/juneau/collections/MarshalledMap_Test.java` L72-74.
- Conventions: `AGENTS.md` ("save a rule" / TODO workflow), `.cursor/skills/code-conventions/SKILL.md` (Javadoc tags, `@SuppressWarnings` placement, fresh-instance vs singleton precedent).
- SonarLint rule reference: `java:S1168` — "Empty arrays and collections should be returned instead of `null`." Exactly the precedent this TODO codifies.
