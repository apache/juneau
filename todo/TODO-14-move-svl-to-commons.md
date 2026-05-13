# Move SVL to `juneau-commons`

Relocate `org.apache.juneau.svl` (Simple Variable Language) from `juneau-marshall` into `juneau-commons` so that consumers (notably `juneau-rest-common`) can use `VarResolver` / `VarResolverSession` without dragging in marshall.

**Target package:** `org.apache.juneau.commons.svl` (+ `org.apache.juneau.commons.svl.vars`)

**Target release:** **9.5.0** — a semi-major release that explicitly permits simple breaking changes.

**Related:**
- ~~Blocked by TODO-15~~ — **dependency satisfied**, see Phase 2 below.
- Enables TODO-7 Phase 4 (decouple `HeaderList` / `PartList` from marshall-hosted SVL).

---

## Design decisions

- **New `org.apache.juneau.commons.runtime` package** for the two external-input data carriers that currently extend `JsonMap`:
  - `org.apache.juneau.commons.runtime.Args`
  - `org.apache.juneau.commons.runtime.ManifestFile`
- Both expose an `Optional<String>`-returning API — callers perform any type conversion themselves via `Optional.map(...)`.
- **`Args.get(String key)` returns only the first value** when a flag is repeated. To preserve `$A{key}` behavior for repeated flags, `ArgsVar` calls `getAll(key)` and joins with `","` itself. This keeps the new API clean while preventing a silent semantic change for SVL users.
- **Hard break on the package rename.** No deprecated shim classes left in `org.apache.juneau.svl.*`; callers update their imports. Acceptable because 9.5 is the designated "simple breaking changes" release.
- **Marshall-side `collections.Args` and `utils.ManifestFile` are deleted** in 9.5 — callers move to `org.apache.juneau.commons.runtime.Args` / `ManifestFile`. (No deprecation cycle; 9.5 allows the break.)
- **`Microservice` retypes its public API in 9.5.** `Microservice.Builder.args(...)`, `Microservice.getArgs()`, `Microservice.getManifest()`, `Microservice.executeCommand(...)`, and the `console/*Command` classes all switch to the new `commons.runtime` types. This is a public API break, called out prominently in the 9.5 release notes — see Phase 6.
- **`ResolvingJsonMap` moves to `org.apache.juneau.collections`** (sits next to `JsonMap`). The `org.apache.juneau.svl` package is fully removed from `juneau-marshall`.
- **`ArgsVar` / `ManifestFileVar` gain Supplier-based wiring.** The static `init(Args)` / `init(ManifestFile)` methods stay (non-breaking; their argument type changes to the new `commons.runtime` types). A new `Supplier<Args>` / `Supplier<ManifestFile>` overload lets callers wire per-resolver state without globally mutating an `AtomicReference`. Deprecation of the static path is **not** in scope for 9.5.
- SVL engine (`VarResolver` / `VarResolverSession`) already imports `org.apache.juneau.commons.inject.*` — no further bean-store retargeting required.

---

## Phase 1 — `commons.runtime.Args` / `ManifestFile`

Create lean, `Optional`-returning replacements in `juneau-commons`.

- [ ] Create `org.apache.juneau.commons.runtime` package (add `package-info.java`).

- [ ] **`Args` surface:**
  - `Args.create()` — builder entry point (see `Args.Builder` below).
  - `Args(String line)`, `Args(String[] argv)` — constructors using **default** grammar (delegating to `create().build(...)`); preserve legacy parsing exactly so the existing `Microservice` and `ArgsVar` consumers don't change behavior.
  - `Optional<String> get(String key)` — first value if repeated; empty if unset or valueless.
  - `Optional<String> get(int index)` — **0-based** positional access.
  - `List<String> getAll(String key)` — all values for a repeated flag (empty list if absent).
  - `List<String> positional()` — all positional args in order.
  - `int argCount()` — number of positional args.
  - `int optionCount()` — number of distinct named keys.
  - `boolean isEmpty()` — `argCount() == 0 && optionCount() == 0` (covers both axes; convenience for the common "any args at all?" check).
  - `boolean has(String key)`, `boolean has(int index)`.
  - `Map<String,List<String>> asMap()` — **named keys only**, unmodifiable. Positional args are exposed via `positional()` to avoid the legacy "numeric strings as keys" foot-gun.
  - **No `size()`** — was always ambiguous on the merged map; force callers to choose `argCount()` or `optionCount()`.

- [ ] **`Args.Builder` — grammar hooks (full version):**
  - `allowEquals(boolean)` — recognize `--key=value` / `-Dkey=value` (default **true**).
  - `allowShortFlags(boolean)` — recognize `-k value` style (default **true**, matches legacy).
  - `allowLongFlags(boolean)` — recognize `--key value` style (default **true**).
  - `allowSystemPropStyle(boolean)` — recognize `-Dkey=value` (default **true**).
  - `caseSensitive(boolean)` — key lookups (default **true**, matches legacy).
  - `customPrefix(String... prefixes)` — override default flag prefixes (default `{"-", "--"}`); empty array means "no flags, everything is positional."
  - Final `build(String line)` / `build(String[] argv)`.
  - **Legacy compatibility note:** the no-arg `new Args(...)` constructors must keep parsing identical to the legacy class. Verify by porting every `Args_Test.java` case (currently in `juneau-utest/src/test/java/org/apache/juneau/commons/collections/Args_Test.java`) into the new package and adapting only the API shape, not the inputs.

- [ ] **`ManifestFile` surface:**
  - `ManifestFile(Class<?> anchor)`, `ManifestFile(InputStream in)`, `ManifestFile(Manifest manifest)`, `ManifestFile(File f)`, `ManifestFile(Path p)`, `ManifestFile(Reader r)` — same constructor menu as today's `org.apache.juneau.utils.ManifestFile`, just type-changed return.
  - `Optional<String> get(String key)` — main attributes.
  - `Optional<String> get(String section, String key)` — named sections (new capability vs. old class).
  - `Set<String> sections()` — unmodifiable.
  - `Map<String,String> asMap()` — main attributes only, unmodifiable.
  - `Map<String,String> asMap(String section)` — unmodifiable.

- [ ] Unit tests for each, at minimum covering every parsing path exercised by existing `Args_Test` / `ManifestFile_Test` / `ArgsVar` / `ManifestFileVar` tests, plus the new `get(section, key)` and builder options.

## Phase 2 — TODO-15 verification (no longer a real block)

The original plan made TODO-14 wait for TODO-15. This is now a **one-line check**:

- [ ] Confirm `org.apache.juneau.commons.inject.{BasicBeanStore, BeanInstantiator, BeanStore, WritableBeanStore, Bean}` exist at their final names. **Already true** (see `FINISHED-15-replace-basicbeanstore-with-v2.md`).
- [ ] `VarResolver` already imports `org.apache.juneau.commons.inject.*` — no retargeting needed in Phase 4.
- [ ] Remaining TODO-15 work (cleanup of `cp.ContextBeanCreator` deprecation marker, `BeanCreator` references in `BeanFilter` / `Assertion` / `RestClient`) is **independent** of TODO-14 and not in this plan's path.

## Phase 3 — Tier A: move pure SVL classes

These files depend only on commons/JDK after Phase 1 — package rename only.

- [ ] Move and rename to `org.apache.juneau.commons.svl`:
  - `Var`, `SimpleVar`, `StreamedVar`, `DefaultingVar`, `MapVar`, `MultipartVar`, `MultipartResolvingVar`
  - `VarList`, `VarResolverException`
- [ ] Move and rename to `org.apache.juneau.commons.svl.vars`:
  - `SystemPropertiesVar`, `EnvVariablesVar`, `SwitchVar`, `IfVar`, `CoalesceVar`
  - `PatternMatchVar`, `PatternReplaceVar`, `PatternExtractVar`
  - `UpperCaseVar`, `LowerCaseVar`, `NotEmptyVar`, `LenVar`, `SubstringVar`
- [ ] Move package-info files (`svl/package-info.java`, `svl/vars/package-info.java`).

## Phase 4 — Tier B: move the engine

- [ ] Move `VarResolver` / `VarResolverSession` to `org.apache.juneau.commons.svl`.
- [ ] No bean-store retargeting needed — `VarResolver` already uses `org.apache.juneau.commons.inject.*`.
- [ ] Update `VarResolver.DEFAULT` to register the moved default vars. **Preserve singleton identity** (same instance across resolves) — the current `private static final VarResolver DEFAULT = ...` pattern is wire-compatible after the rename.

## Phase 5 — Tier C: retarget `ArgsVar` / `ManifestFileVar`

- [ ] Move `ArgsVar` and `ManifestFileVar` to `org.apache.juneau.commons.svl.vars`.
- [ ] Switch each var's field type from `org.apache.juneau.collections.Args` / `org.apache.juneau.utils.ManifestFile` to the new `org.apache.juneau.commons.runtime.Args` / `ManifestFile`.
- [ ] **`ArgsVar` resolve semantics** — to preserve the legacy `$A{key}` behavior for repeated flags, the implementation must call `args.getAll(key)` and join with `","` (since the new `args.get(key)` returns only the first value).
- [ ] **`init(...)` methods:** keep the method **name and arity** (`init(Args)` / `init(ManifestFile)`); the **argument type changes** to the new `commons.runtime` types — this is the source-incompatible part of the move. Document in release notes.
- [ ] **New Supplier overloads** (additive, non-breaking):
  - `ArgsVar.create(Supplier<Args> supplier)` returning a `Var` instance with per-instance state instead of the global `AtomicReference`.
  - `ManifestFileVar.create(Supplier<ManifestFile> supplier)` similarly.
  - Static `init(...)` paths remain functional; the supplier path is for callers that want isolated state per resolver. Deprecation of the static path stays out of scope for 9.5.

## Phase 6 — Marshall-side fixes and `Microservice` migration

- [ ] Move `ResolvingJsonMap` from `org.apache.juneau.svl` to `org.apache.juneau.collections` (sits next to `JsonMap`). Update its imports to the new `commons.svl` package for `VarResolverSession`.
- [ ] Update `juneau-utest/.../svl/ResolvingJsonMapTest.java` to its new test package alongside the move.
- [ ] **Delete** `org.apache.juneau.collections.Args` and `org.apache.juneau.utils.ManifestFile` (hard break, 9.5 semantics).
- [ ] **Migrate `Microservice` and friends** (public API break — call out loudly in release notes):
  - `juneau-microservice-core/.../Microservice.java` — retype `args` field, `manifest` field, `Builder.args(Args)`, `getArgs()`, `getManifest()`, `executeCommand(Args, ...)`, plus the `value instanceof ManifestFile` branch in the bean wiring.
  - `juneau-microservice-core/.../console/{ConsoleCommand, ConfigCommand, ExitCommand, RestartCommand, HelpCommand}.java` — update `execute(...)` signatures and any `Args` / `ManifestFile` use.
  - `juneau-microservice-core/.../resources/ConfigResource.java`.
  - `juneau-microservice-jetty/.../JettyMicroservice.java`.
  - `juneau-utest/.../microservice/Microservice_Builder_Test.java` — update test setup.
- [ ] **`RestContext` non-conflict note** — `juneau-rest-server/.../RestContext.java` line ~1136 contains `new Args(...)` referring to a `RestContext.Args` **inner class** (constructor-arg bundle), **not** the marshall `Args`. Phase 7 find/replace must skip this; an explicit grep for `org.apache.juneau.collections.Args` (not the bare class name) is the safe filter.

## Phase 7 — Repo-wide rename

- [ ] Global find/replace of imports:
  - `org.apache.juneau.svl` → `org.apache.juneau.commons.svl`
  - `org.apache.juneau.svl.vars` → `org.apache.juneau.commons.svl.vars`
  - `org.apache.juneau.collections.Args` → `org.apache.juneau.commons.runtime.Args`
  - `org.apache.juneau.utils.ManifestFile` → `org.apache.juneau.commons.runtime.ManifestFile`
  - `org.apache.juneau.svl.ResolvingJsonMap` → `org.apache.juneau.collections.ResolvingJsonMap`
- [ ] **Wildcard import audit:** files using `import org.apache.juneau.collections.*;` or `import org.apache.juneau.utils.*;` may have been resolving `Args` / `ManifestFile` implicitly through the wildcard. After the legacy classes are deleted, these files compile-error unless an explicit `import org.apache.juneau.commons.runtime.{Args,ManifestFile};` is added. Sweep the wildcard-import set in `juneau-microservice` and `juneau-rest-server` first; add explicit imports where needed.
- [ ] Fix callers of the old `JsonMap`-based `Args` / `ManifestFile` API: adapt to the new `Optional<String>`-returning surface. Affected real consumers (verified via `new Args(`/`new ManifestFile(` grep): `Microservice`, `Microservice_Builder_Test`, `ManifestFile_Test`, `Args_Test`, `ArgsVar`. (`RestContext.Args` is the inner-class noted above — not affected.)
- [ ] Update test utility `juneau-utest/.../XVar.java` imports (otherwise trivial).
- [ ] **Touch-point estimate:** ~150 files have `import org.apache.juneau.svl…` (verified via `rg -l 'import org\.apache\.juneau\.svl' -tjava`); the bulk are simple package renames. Plus the small number of `Args` / `ManifestFile` consumers above. Plus `juneau-microservice`, `juneau-rest-*`, `juneau-marshall-*`, `juneau-utest`.
- [ ] Re-run `./scripts/test.py -f` to confirm clean build and green tests.

## Phase 8 — Documentation

- [ ] Update `juneau-docs` (Simple Variable Language topic + any cross-links).
- [ ] Create/update `juneau-docs/pages/release-notes/9.5.0.md` with an entry describing:
  - SVL package move (`org.apache.juneau.svl.*` → `org.apache.juneau.commons.svl.*`).
  - `Args` / `ManifestFile` relocation and new `Optional`-returning API; explicit note about `Args.get(String)` returning only first value (compared to legacy `getArg(String)` which comma-joined multi-value flags) — and that `ArgsVar` adapts internally so `$A{key}` behavior is unchanged.
  - Removal of the legacy `JsonMap`-based `Args` / `ManifestFile`.
  - **`Microservice` public API change** — `Microservice.Builder.args(...)`, `getArgs()`, `getManifest()`, `executeCommand(...)`, and console-command entry points all retype to `commons.runtime.Args` / `ManifestFile`.
  - `ResolvingJsonMap` moves from `org.apache.juneau.svl` to `org.apache.juneau.collections`.
  - New `Args.Builder` grammar hooks (allowEquals / allowShortFlags / allowLongFlags / allowSystemPropStyle / caseSensitive / customPrefix).
  - New `ArgsVar.create(Supplier<Args>)` / `ManifestFileVar.create(Supplier<ManifestFile>)` overloads for per-resolver wiring.
  - Migration notes for external consumers.

## Phase 9 — Update TODO-7

Once the SVL engine lives in `juneau-commons`, several items in TODO-7 either resolve outright or need rethinking.

- [ ] In `todo/TODO-7-decouple-rest-common-from-marshall.md`:
  - [ ] Remove the `VarResolverSession` bullet from Phase 4 (now satisfied — rest-common can depend on commons-hosted SVL directly).
  - [ ] Drop the `VarResolverSession` row from the Phase 5 dependency-surface table.
  - [ ] If `BeanCreator` in rest-common's `httppart.bean` now points at the commons-hosted bean creator, update the corresponding Phase 4 bullet accordingly — otherwise leave for TODO-15.
- [ ] Run `/todo cleanup 7` to prune any other items obsoleted by this work.

---

## Out of scope

- Replacing `Args` / `ManifestFile` with an abstract "property source" hierarchy — defer to a follow-up.
- Adding new vars (e.g. `EnvFile`, `DotenvVar`) — defer to a follow-up.
- Any `BasicBeanStore` / `BeanCreator` consolidation — owned entirely by TODO-15.
- Deprecation shims in the old `org.apache.juneau.svl.*` package — hard break per 9.5 policy.
- Deprecation of `ArgsVar.init(...)` / `ManifestFileVar.init(...)` static state — Supplier overloads are added alongside, but the static path stays as-is for 9.5.

## Risk notes

- **`Microservice` is a public extension surface.** Any downstream microservice subclass will recompile-fail in 9.5 until imports/types are updated. Highlight loudly in release notes; consider an explicit code-snippet diff in the migration section.
- `ArgsVar` / `ManifestFileVar` `init(...)` argument type changes — source-incompatible for any caller importing the old `Args` / `ManifestFile`. New Supplier overloads soften this for callers willing to migrate.
- `VarResolver.DEFAULT` is a widely-referenced singleton — preserve identity semantics (same instance across resolves).
- External users importing `org.apache.juneau.svl.*` will see compile breaks; ~150 internal files alone need import fixes — external callers will see proportionally similar churn.
- `Args.get(String)` returning first-only (vs. legacy comma-join) is a documented behavior change. `ArgsVar` is shielded; bespoke direct callers of `args.getArg(String)` are not.
