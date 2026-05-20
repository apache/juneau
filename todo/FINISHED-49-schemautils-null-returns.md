# FINISHED-49: SchemaUtils parse helpers — null vs empty returns

Archived from `TODO-49-schemautils-null-returns.md` on 2026-05-20.

## What shipped

The three flagged helpers in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/jsonschema/SchemaUtils.java` — `parseMap(Object)`, `parseMap(String[])`, `parseSet(String[])` — now return `new JsonMap()` / `new LinkedHashSet<>()` on null or empty input instead of `null`. All three `@SuppressWarnings({"java:S1168"})` annotations and their `// TODO` comments are removed. Javadoc on every site rewritten to reflect the new contract. The `parseMap("IGNORE")` sentinel path still returns `{"ignore": true}` unchanged.

The required caller audit covered every external call site across the repo and patched **zero** of them — all three callers (`SchemaAnnotation.merge` × 2, `SubItemsAnnotation.merge` × 1) feed the result through `appendFirst(nec, …)` where `nec = Utils::ne` for `Collection<?>` returns false for both null and empty, so the emitted schema is byte-identical to today's output. The `parseMap(...)` overloads have zero external callers (the `parseMap` references in `BasicSwaggerProviderSession` are a private same-named method on that class, not `SchemaUtils.parseMap`).

## Files delivered

Source (1):

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/jsonschema/SchemaUtils.java`

Tests (1):

- `juneau-utest/src/test/java/org/apache/juneau/jsonschema/SchemaUtils_Test.java` (new — 12 cases covering null / empty / `IGNORE` / populated paths on all three methods)

No release-notes entry, no caller patches, no wider `java:S1168` sweep.

## Verification

- `SchemaUtils_Test` green (12/12).
- Phase-2 focused cluster green: `SchemaUtils_Test`, `JsonSchemaGeneratorTest`, `JsonSchemaConfigAnnotationTest`, `SchemaAnnotation_Test`, `SubItemsAnnotation_Test`, `SchemaApplyAnnotation_Test`, `JsonSchemaBeanGenerator_Test`.
- Full `./scripts/test.py`: BUILD SUCCESS.
- Coverage on `SchemaUtils.java` improved versus pre-change because the new tests exercise the null/empty branches that were previously dead-coded — 75% instr / 73% br. Remaining gaps are pre-existing in unrelated methods.
- `ReadLints` clean.

## Decisions (locked in 2026-05-20)

1. **Direction** = B — switch to empty returns. Dropped `@SuppressWarnings({"java:S1168"})` and `// TODO` markers.
2. **Caller audit** = completed during implementation. 3 sites classified Safe, 0 Affected, 0 patches needed.
3. **Escalation gate** = not triggered (audit surfaced no non-trivial caller changes).

## Caller audit reference

| Call site | Result | Classification |
| --- | --- | --- |
| `SchemaAnnotation.merge` (× 2 occurrences of `SchemaUtils.parseSet(...)`) | Feeds into `appendFirst(nec, ...)` with `nec = Utils::ne` for `Collection<?>` — empty and null both produce false, so the emitter behaves identically. | Safe |
| `SubItemsAnnotation.merge` (× 1 occurrence of `SchemaUtils.parseSet(...)`) | Same `appendFirst(Utils::ne, ...)` pattern as above. | Safe |
| `SchemaUtils.parseMap(Object)` / `parseMap(String[])` | Zero external callers anywhere in the repo. (The `parseMap` references in `BasicSwaggerProviderSession` are a private same-named method on that class.) | Safe by virtue of being unused externally. |

## Why these were carved out from TODO-48

TODO-48 sites are parsers — `Json5Map.ofString("")` is asking "what data did you give me?" and an empty document is a perfectly good answer. `SchemaUtils.parseMap(...)` is asking "did the user configure this annotation field at all?" — historically null was the documented "not configured" signal, distinct from "configured to empty". The audit confirmed that no caller currently differentiates the two states (every caller funnels the result through an emptiness-aware helper), so the semantic distinction was theoretical rather than wire-format-impacting. Carving it out as a separate TODO + audit was the right call regardless — if any caller had been Affected, we'd have known to patch the emitter alongside.

## References

- SonarLint rule: `java:S1168` — "Empty arrays and collections should be returned instead of `null`."
- Sibling archive: `todo/FINISHED-48-empty-return-marshalled-collections.md`.
- Conventions: `AGENTS.md`, `.cursor/skills/code-conventions/SKILL.md`.
