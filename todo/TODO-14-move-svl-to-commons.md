# Move SVL to `juneau-commons`

Relocate `org.apache.juneau.svl` (Simple Variable Language) from `juneau-marshall` into `juneau-commons` so that consumers (notably `juneau-rest-common`) can use `VarResolver` / `VarResolverSession` without dragging in marshall.

**Target package:** `org.apache.juneau.commons.svl` (+ `org.apache.juneau.commons.svl.vars`)

**Target release:** **9.5.0** — a semi-major release that explicitly permits simple breaking changes.

**Related:**
- **Blocked by TODO-15** — `BasicBeanStore` / `BeanCreator` consolidation must land first so SVL can point at the final class names (no `*2` suffix) on its first move.
- Enables TODO-7 Phase 4 (decouple `HeaderList` / `PartList` from marshall-hosted SVL).

---

## Design decisions

- **New `org.apache.juneau.commons.runtime` package** for the two external-input data carriers that currently extend `JsonMap`:
  - `org.apache.juneau.commons.runtime.Args`
  - `org.apache.juneau.commons.runtime.ManifestFile`
- Both expose an `Optional<String>`-returning API — callers perform any type conversion themselves via `Optional.map(...)`.
- **Hard break on the package rename.** No deprecated shim classes left in `org.apache.juneau.svl.*`; callers update their imports. Acceptable because 9.5 is the designated "simple breaking changes" release.
- Marshall-side `collections.Args` and `utils.ManifestFile` are **deleted** in 9.5 — callers move to `org.apache.juneau.commons.runtime.Args` / `ManifestFile`. (No deprecation cycle; 9.5 allows the break.)
- SVL engine (`VarResolver` / `VarResolverSession`) retargets the commons bean-store stack **after** TODO-15 renames the `*2` classes to their final names. TODO-14 never references `BasicBeanStore2` / `BeanCreator2` directly.

---

## Phase 1 — `commons.runtime.Args` / `ManifestFile`

Create lean, `Optional`-returning replacements in `juneau-commons`.

- [ ] Create `org.apache.juneau.commons.runtime` package (add `package-info.java`).
- [ ] `Args` with surface:
  - `Args.create()` — builder entry point (see `Args.Builder` below).
  - `Args(String line)`, `Args(String[] argv)` — constructors using **default** grammar.
  - `Optional<String> get(String key)` — returns first value if repeated; empty if unset or valueless.
  - `Optional<String> get(int index)` — **0-based** positional access.
  - `List<String> getAll(String key)` — all values for a repeated flag (empty list if absent).
  - `int size()`, `boolean isEmpty()`, `boolean has(String key)`.
  - `Map<String,List<String>> asMap()` — unmodifiable.
- [ ] `Args.Builder` for configurable parsing — the grammar hooks that need to be switchable:
  - `allowEquals(boolean)` — recognize `--key=value` / `-Dkey=value` (default **true**).
  - `allowShortFlags(boolean)` — recognize `-k value` style (default **true**).
  - `allowLongFlags(boolean)` — recognize `--key value` style (default **true**).
  - `allowSystemPropStyle(boolean)` — recognize `-Dkey=value` (default **true**).
  - `caseSensitive(boolean)` — key lookups (default **true**).
  - `customPrefix(String... prefixes)` — override the default flag prefixes if callers need something unusual.
  - Final `build(String line)` / `build(String[] argv)`.
- [ ] `ManifestFile` with surface:
  - `ManifestFile(Class<?> anchor)`, `ManifestFile(InputStream in)`, `ManifestFile(Manifest manifest)`.
  - `Optional<String> get(String key)` — main attributes.
  - `Optional<String> get(String section, String key)` — named sections (new capability vs. old class).
  - `Set<String> sections()` — unmodifiable.
  - `Map<String,String> asMap()` — main attributes only, unmodifiable.
  - `Map<String,String> asMap(String section)` — unmodifiable.
- [ ] Unit tests for each, at minimum covering every parsing path exercised by existing `ArgsVar` / `ManifestFileVar` tests, plus the new `get(section, key)` and builder options.

## Phase 2 — Confirm TODO-15 has landed

**TODO-14 does not start Phases 3+ until TODO-15 is complete.**

- [ ] Verify `org.apache.juneau.commons.inject.BasicBeanStore` / `BeanCreator` exist with their final names (no `2` suffix).
- [ ] Verify the legacy `org.apache.juneau.cp.BasicBeanStore` / `BeanCreator` / `BeanBuilder` are deleted, or that SVL can safely ignore them.
- [ ] Any SVL-specific gap in the commons bean-store surface discovered here gets filed back into TODO-15 rather than patched in this plan.

## Phase 3 — Tier A: move pure SVL classes

These files depend only on commons/JDK after Phase 1/2 — package rename only.

- [ ] Move and rename to `org.apache.juneau.commons.svl`:
  - `Var`, `SimpleVar`, `StreamedVar`, `DefaultingVar`, `MapVar`, `MultipartVar`, `MultipartResolvingVar`
  - `VarList`, `VarResolverException`
- [ ] Move and rename to `org.apache.juneau.commons.svl.vars`:
  - `SystemPropertiesVar`, `EnvVariablesVar`, `SwitchVar`, `IfVar`, `CoalesceVar`
  - `PatternMatchVar`, `PatternReplaceVar`, `PatternExtractVar`
  - `UpperCaseVar`, `LowerCaseVar`, `NotEmptyVar`, `LenVar`, `SubstringVar`

## Phase 4 — Tier B: move the engine

- [ ] Move `VarResolver` / `VarResolverSession` to `org.apache.juneau.commons.svl`.
- [ ] Switch bean-store/creator references to `org.apache.juneau.commons.inject.BasicBeanStore` / `BeanCreator` (final names, post-TODO-15).
- [ ] Update `VarResolver.DEFAULT` to register the moved default vars.

## Phase 5 — Tier C: retarget `ArgsVar` / `ManifestFileVar`

- [ ] Update `ArgsVar` to use `org.apache.juneau.commons.runtime.Args`.
- [ ] Update `ManifestFileVar` to use `org.apache.juneau.commons.runtime.ManifestFile`.
- [ ] Move both vars to `org.apache.juneau.commons.svl.vars`.

## Phase 6 — Marshall-side fixes

- [ ] `ResolvingJsonMap` stays in marshall; update its imports to the new commons package.
- [ ] **Delete** `org.apache.juneau.collections.Args` and `org.apache.juneau.utils.ManifestFile` (hard break, 9.5 semantics). Callers move to `org.apache.juneau.commons.runtime.Args` / `ManifestFile` as part of Phase 7.

## Phase 7 — Repo-wide rename

- [ ] Global find/replace of imports:
  - `org.apache.juneau.svl` → `org.apache.juneau.commons.svl`
  - `org.apache.juneau.svl.vars` → `org.apache.juneau.commons.svl.vars`
  - `org.apache.juneau.collections.Args` → `org.apache.juneau.commons.runtime.Args`
  - `org.apache.juneau.utils.ManifestFile` → `org.apache.juneau.commons.runtime.ManifestFile`
- [ ] Fix callers of the old `JsonMap`-based `Args` / `ManifestFile` API: adapt to the new `Optional<String>`-returning surface. Expect ~small number of callsites beyond the two SVL vars.
- [ ] Update test utility `juneau-utest/.../XVar.java` imports (otherwise trivial).
- [ ] Verify ~150+ touch points across `juneau-marshall`, `juneau-rest-*`, `juneau-microservice-*`, `juneau-examples-*`, `juneau-utest`.
- [ ] Re-run `./scripts/test.py -f` to confirm clean build and green tests.

## Phase 8 — Documentation

- [ ] Update `juneau-docs` (Simple Variable Language topic + any cross-links).
- [ ] Create/update `juneau-docs/pages/release-notes/9.5.0.md` with an entry describing:
  - SVL package move (`org.apache.juneau.svl.*` → `org.apache.juneau.commons.svl.*`).
  - `Args` / `ManifestFile` relocation and `Optional`-returning API.
  - Removal of the legacy `JsonMap`-based `Args` / `ManifestFile`.
  - Migration notes for external consumers.

## Phase 9 — Update TODO-7

Once the SVL engine lives in `juneau-commons`, several items in TODO-7 either resolve outright or need rethinking.

- [ ] In `todo/TODO-7-decouple-rest-common-from-marshall.md`:
  - [ ] Remove the `VarResolverSession` bullet from Phase 4 (now satisfied — rest-common can depend on commons-hosted SVL directly).
  - [ ] Drop the `VarResolverSession` row from the Phase 5 dependency-surface table.
  - [ ] If `BeanCreator` in rest-common's `httppart.bean` now points at the commons-hosted (or `*2`) bean creator, update the corresponding Phase 4 bullet accordingly — otherwise leave for TODO-15.
- [ ] Run `/todo cleanup 7` to prune any other items obsoleted by this work.

---

## Out of scope

- Replacing `Args` / `ManifestFile` with an abstract "property source" hierarchy — keep the migration mechanical.
- Adding new vars (e.g. `EnvFile`, `DotenvVar`) — do in a follow-up.
- Any `BasicBeanStore` / `BeanCreator` consolidation — owned entirely by TODO-15.
- Deprecation shims in the old `org.apache.juneau.svl.*` package — hard break per 9.5 policy.

## Risk notes

- **Hard dependency on TODO-15.** If TODO-15 slips, TODO-14 slips too (or we accept a temporary `*2` reference — but the plan currently assumes final names).
- `ArgsVar` / `ManifestFileVar` have `static init(...)` methods that are public API; preserve their signatures even when the underlying types change package.
- `VarResolver.DEFAULT` is a widely-referenced singleton — preserve identity semantics (same instance across resolves).
- External users importing `org.apache.juneau.svl.*` will see compile breaks; highlight prominently in the 9.5 release notes.
