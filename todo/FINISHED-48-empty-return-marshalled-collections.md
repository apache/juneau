# FINISHED-48: empty-return on marshalled-collection factories

Archived from `TODO-48-empty-return-marshalled-collections.md` on 2026-05-20.

## What shipped

Every `ofString(...)` overload (plus `ofJsonOrCdl(...)` / `ofJson5OrCdl(...)`) on `MarshalledList`, `MarshalledMap`, `JsonList`, `JsonMap`, `Json5List`, `Json5Map` now returns a fresh empty instance of the precise subclass when input is null or empty, instead of `null`. The four flagged `// TODO java:S1168` markers and their `@SuppressWarnings` annotations are gone. Javadoc on every affected site now reads `@return A new list/map (empty if the input was null), never null.` `of(Collection<?>)` / `of(Map<?,?>)` copy factories deliberately keep their existing null-in-null-out behavior.

Two latent NPE call sites became well-defined no-ops without any source change in the caller — `BeanMapLoader.load(BeanMap, String)`, `BeanMapLoader.load(BeanMap, Reader, ReaderParser)`, and `MarshallingSession.parseToMap(CharSequence)` all relied on the factories returning non-null; once the factories never return null, `m.putAll(empty)` / `empty.session(this)` are no-ops by definition. `MarshallingSession.parseToMap`'s Javadoc was tightened to reflect the new tolerant contract.

## Files delivered

Source (7):

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/MarshalledList.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/MarshalledMap.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonList.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonMap.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json5/Json5List.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json5/Json5Map.java`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingSession.java` (Javadoc only)

Tests (8):

- `juneau-utest/src/test/java/org/apache/juneau/json5/Json5List_Test.java` (rewrote `a04_factoryOfStringNullInput`, added empty-input regression coverage)
- `juneau-utest/src/test/java/org/apache/juneau/json5/Json5Map_Test.java` (same)
- `juneau-utest/src/test/java/org/apache/juneau/collections/JsonList_Test.java` (added empty-input regression coverage)
- `juneau-utest/src/test/java/org/apache/juneau/collections/JsonMap_Test.java` (same)
- `juneau-utest/src/test/java/org/apache/juneau/collections/MarshalledList_Test.java` (rewrote `a06_parseViaOfStringNullInput`, added empty-input coverage)
- `juneau-utest/src/test/java/org/apache/juneau/collections/MarshalledMap_Test.java` (same)
- `juneau-utest/src/test/java/org/apache/juneau/BeanMapLoader_Test.java` (new — 3 tests, including null-input no-op regression)
- `juneau-utest/src/test/java/org/apache/juneau/MarshallingSession_Test.java` (new — 3 tests, including null-input → empty session-attached `Json5Map`)

No release-notes entry (per locked-in decision — wire-compatible, internal-leaning contract).

## Verification

- All 9 focused-cluster classes green (`Json5List_Test`, `Json5Map_Test`, `JsonList_Test`, `JsonMap_Test`, `MarshalledList_Test`, `MarshalledMap_Test`, `BeanMap_Test`, `BeanMapLoader_Test`, `MarshallingSession_Test`).
- Full `./scripts/test.py`: BUILD SUCCESS.
- Coverage on touched files (no regression vs. pre-change):
  - `JsonList.java` — 42% instr / 46% br.
  - `JsonMap.java` — 34% instr / 18% br.
  - `MarshalledList.java` — 78% instr / 62% br.
  - `MarshalledMap.java` — 65% instr / 59% br.
  - `Json5List.java` — 64% instr / 62% br.
  - `Json5Map.java` — 77% instr / 68% br.
- Remaining coverage gaps are pre-existing in unrelated methods (`ofArrays`, `ofCollections`, deep getter overloads).
- `ReadLints` clean across all 15 files.

## Decisions (locked in 2026-05-20)

1. **Scope** = all symmetric `ofString(...)` / `ofJsonOrCdl(...)` / `ofJson5OrCdl(...)` factories on every `MarshalledList` / `MarshalledMap` subclass. Family-wide uniformity.
2. **Return value** = fresh `new <Type>()` per call. NOT shared `EMPTY_LIST` / `EMPTY_MAP` singletons. Callers mutate the result.
3. **`of(Collection<?>)` / `of(Map<?,?>)` copy factories** = leave as-is. They still return `null` on `null` input. SonarLint doesn't flag them and "copy" semantics legitimately differ from "parse".
4. **Javadoc** = explicit one-line `@return A new list/map (empty if the input was null), never null.` on every affected site. Drop the `// TODO` comments and the `or null if...` clauses.
5. **Sibling `SchemaUtils.parseMap` / `parseSet`** = spun out to `TODO-49` / `FINISHED-49` as a separate plan (different semantics — config-loading helpers rather than user-input parsers).
6. **Release notes** = OMIT. No entry in `9.5.0.md`. Wire-compatible, internal-leaning contract.

## Caller impact reference

The audit at plan time classified callers into three buckets; the implementation confirmed each bucket's behavior:

### Safe (no observable change)

- `SchemaUtils.parseMap(Object)` L81, `SchemaUtils.parseMap(String[])` L108, `SchemaUtils.parseSet(String[])` L128 — all guarded against null/empty before calling the factory; null branch unreachable.
- `Entry.asList(Parser)` L354, `Entry.asMap(Parser)` L428 — `s = toString()` is non-null; null branch unreachable.
- `BasicSwaggerProviderSession.parseListOrCdl(...)` L1042 — guarded; null branch unreachable.
- ~200 test-only call sites — pass literal non-null non-empty strings.

### Affected — new behavior is the desired one

| Site | Today | After |
| --- | --- | --- |
| `BeanMapLoader.load(BeanMap<T>, String input)` L53 | NPE if `input == null` | No-op |
| `BeanMapLoader.load(BeanMap<T>, Reader r, ReaderParser p)` L72 | NPE if `r == null` | No-op |
| `MarshallingSession.parseToMap(CharSequence value)` L1394 | NPE if `value == null` | Returns empty session-attached `Json5Map` |

None of these had a runtime null-guard; the new behavior turns three latent NPEs into well-defined no-ops.

### Tests rewritten to assert the new contract

- `Json5List_Test#a04_factoryOfStringNullInput`
- `Json5Map_Test#a04_factoryOfStringNullInput`
- `MarshalledList_Test#a06_parseViaOfStringNullInput`
- `MarshalledMap_Test#a06_parseViaOfStringNullInput`

Each was updated to assert non-null + `isEmpty()` + mutable (`add`/`put` succeeds) + exact-subclass.

## Cross-subclass consistency reference

| File | Sites updated |
| --- | --- |
| `MarshalledList.java` | `ofString(CharSequence, Parser)`, `ofString(Reader, Parser)` |
| `MarshalledMap.java` | `ofString(CharSequence, Parser)`, `ofString(Reader, Parser)` |
| `JsonList.java` | `ofString(CharSequence)`, `ofString(Reader)` (flagged), `ofString(CharSequence, Parser)`, `ofString(Reader, Parser)`, `ofJsonOrCdl(String)` (flagged) |
| `JsonMap.java` | `ofString(CharSequence)`, `ofString(Reader)`, `ofString(CharSequence, Parser)`, `ofString(Reader, Parser)` |
| `Json5List.java` | `ofString(CharSequence)`, `ofString(Reader)` (flagged), `ofString(CharSequence, Parser)`, `ofString(Reader, Parser)`, `ofJson5OrCdl(String)` (flagged) |
| `Json5Map.java` | `ofString(CharSequence)`, `ofString(Reader)`, `ofString(CharSequence, Parser)`, `ofString(Reader, Parser)` |

Subclasses that needed no edits because they inherit the new behavior transparently or are instance wrappers without static factories: `ResolvingMarshalledMap`, `DelegateList`, `DelegateMap`, `MarshalledList.UnmodifiableMarshalledList`, `MarshalledMap.UnmodifiableMarshalledMap`, `JsonList.UnmodifiableJsonList`, `JsonMap.UnmodifiableJsonMap`, `Json5List.UnmodifiableJson5List`, `Json5Map.UnmodifiableJson5Map`. The anonymous `EMPTY_LIST` / `EMPTY_MAP` singletons on every class were unrelated to the factory contract.

## Why fresh-instance, not the EMPTY_LIST / EMPTY_MAP singleton

The codebase already exposes unmodifiable `EMPTY_LIST` / `EMPTY_MAP` singletons on every class, but every one is an unmodifiable anonymous subclass that throws on `add` / `put` / `remove`. The three known affected callers (`BeanMapLoader.load`, `MarshallingSession.parseToMap`, `SchemaUtils.parseMap`'s inner `new JsonMap(...)`) mutate the result; tests routinely do `Json5Map.ofString("{a:'b'}").put(...)`. Returning the shared singleton would silently swap `NullPointerException` for `UnsupportedOperationException`. Fresh-instance allocation is the only safe choice; the allocation cost is negligible because the null branch is only hit on null/empty input.

## References

- SonarLint rule: `java:S1168` — "Empty arrays and collections should be returned instead of `null`."
- Sibling archive for the `SchemaUtils` carve-out: `todo/FINISHED-49-schemautils-null-returns.md`.
- Conventions: `AGENTS.md`, `.cursor/skills/code-conventions/SKILL.md` (Javadoc tags, `@SuppressWarnings` placement, fresh-instance vs singleton precedent).
- Existing "empty rather than null" precedent in the codebase: the `EMPTY_LIST` / `EMPTY_MAP` singletons on every collection class, `JsonMap.getList(key, createIfNotExists)` family, and the `opt(...)` / `opte()` helpers in `org.apache.juneau.commons.utils.Utils`.
