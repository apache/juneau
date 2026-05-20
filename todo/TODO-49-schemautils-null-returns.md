# TODO-49 — `SchemaUtils` parse helpers: null vs empty returns

## Goal

Decide whether the three `parseMap` / `parseSet` helpers in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/jsonschema/SchemaUtils.java` should keep their current `null = not configured` semantics or switch to returning empty collections. If we change them, also drop the `@SuppressWarnings({"java:S1168"})` and the `// TODO` comment on each site.

## Why now

SonarLint flags all three as `java:S1168` ("Empty arrays and collections should be returned instead of null"). The suppressions carry a `// TODO` that the on-the-fly check then re-flags. Either we commit to the current contract (drop the `// TODO`, keep the suppression, update the rationale) or change the contract (return empty, drop the suppression).

This was spun out of TODO-48 because the semantics here are different from the `MarshalledList` / `MarshalledMap` `ofString(...)` family:

- TODO-48 sites parse user-supplied input — null/empty input genuinely means "empty result".
- These sites are config-loading helpers — `null` is the documented "not configured" signal, distinct from "configured to empty".

## Sites

All in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/jsonschema/SchemaUtils.java`:

- Line 65 — `parseMap(Object o)`
- Line 97 — `parseMap(String[] ss)`
- Line 118 — `parseSet(String[] ss)`

## Decisions needed

1. Keep `null = not configured` (then drop the `// TODO` and reword the rationale), or switch to empty returns and update every caller that distinguishes `null` from `isEmpty()`?
2. If switching: do callers in `JsonSchemaBeanGenerator` / annotation-driven schema introspection rely on `null` to skip section emission, or do they already `nn(...)` / `ne(...)` defensively?
3. If keeping: is `java:S1168` justifiable here as a documented "tri-state" return, and should the rationale comment make that explicit?

## Out of scope

- TODO-48's `MarshalledList` / `MarshalledMap` `ofString(...)` family — that lives in `todo/TODO-48-empty-return-marshalled-collections.md`.
- Any wider `java:S1168` sweep elsewhere in the codebase.

## References

- `todo/TODO-48-empty-return-marshalled-collections.md` — sibling plan, recommends the empty-return policy but explicitly excludes these `SchemaUtils` sites.
- SonarSource `java:S1168` rule description.
