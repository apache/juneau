# Replace `BasicBeanStore` / `BeanCreator` with v2 equivalents

Eliminate the legacy injection stack in `org.apache.juneau.cp` (`BasicBeanStore`, `BeanCreator`, `BeanBuilder`, `BeanCreateMethodFinder`, `ContextBeanCreator`) in favor of the rewritten classes already present in `org.apache.juneau.commons.inject` (`BasicBeanStore2`, `BeanCreator2`, `BeanStore`, `CreatableBeanStore`, etc.). Once the replacement lands, the `2` suffix is dropped and the legacy classes are removed.

**Target release:** **9.5.0** — semi-major release permitting simple breaking changes. Hard-break migration (no deprecation cycle on the final rename).

**Sequencing:**
- **Lands before TODO-14.** TODO-14 (SVL → commons) consumes the final, un-suffixed class names produced by this TODO, so this work must complete first.
- TODO-1 (REST server API → `BeanStore2`) — prerequisite consumer migration, folded into Phase 3 below.

**Related 9.5 work that shrinks this migration's footprint:**
- **TODO-16** — replaces `RestContext.Builder` configuration with memoized/resettable fields on `RestContext` (reference pattern: `RestContext.allowedParserOptions`). Many of the `BeanBuilder<T>` / `BasicBeanStore` consumers currently living under `juneau-rest-server` will disappear as part of that refactor, independent of this TODO. **Consumer inventory (Phase 2 below) must run after TODO-16 Phase 1–2 lands** so we aren't migrating builders that are about to be deleted.

---

## Phase 0 — Interim: deprecate the legacy classes

Low-risk, independent of the rest of this plan. Can land in an earlier release than 9.5 if desired.

- [ ] Annotate with `@Deprecated` and Javadoc pointing at `org.apache.juneau.commons.inject`:
  - `org.apache.juneau.cp.BasicBeanStore`
  - `org.apache.juneau.cp.BeanCreator`
  - `org.apache.juneau.cp.BeanBuilder` *(consumer class `org.apache.juneau.BeanBuilder` — re-evaluate)*
  - `org.apache.juneau.cp.BeanCreateMethodFinder`
  - `org.apache.juneau.cp.ContextBeanCreator`
- [ ] Do **not** use `forRemoval = true` yet — too many internal consumers still wired to these. Deletion happens in Phase 4.

---

## Phase 1 — API parity audit

Goal: decide, class-by-class, what to port from legacy → v2 before any migrations. **Owns** the parity work for TODO-14's SVL consumer as well — any gap surfaced by the SVL move gets fixed here, not in TODO-14.

### Resolved decisions (already made)

The legacy `BasicBeanStore.Builder` fluent API (`create().readOnly().threadSafe().type().impl().parent().build()`) is **dropped wholesale**. Rationale and replacement paths:

- **Read-only by default** — the base `BeanStore` interface is read-only. Callers that need mutation use `WritableBeanStore` / `BasicBeanStore2`. No `readOnly()` toggle needed.
- **Thread-safe by default** — `BasicBeanStore2` already uses `ConcurrentHashMap` at both levels (type → name → supplier). No `threadSafe()` toggle needed.
- **`type(Class)`** — one caller (a single test). Drop and rewrite that test.
- **`impl(BasicBeanStore)`** — zero callers in the tree. Drop.
- **`parent(...)`** — preserved via `new BasicBeanStore2(parent)` constructor.

Net new v2 surface from this decision: none. The `Builder` class simply goes away.

### Still to port (must be added to v2 before Phase 3 starts)

- [ ] **`BasicBeanStore.INSTANCE`** — widely used as a shared read-only empty store (`SerializerSet`, `ParserSet`, `EncoderSet`, `SwaggerUI`, `OpenApiUI`, `ThrownStore`, several tests). Add a `BasicBeanStore2.INSTANCE` constant (or equivalent on `BeanStore`).
- [ ] **`BasicBeanStore.Void.class`** — used as an annotation-default sentinel in `@Rest.beanStore()` and `RestAnnotation`. Either:
  - port a `BasicBeanStore2.Void` sentinel class, or
  - redesign those annotations (e.g., treat `BasicBeanStore2.class` itself as the sentinel and interpret "equal to default" as "unset").
- [ ] **Executable-resolution helpers** — `getMissingParams(ExecutableInfo, Object)`, `getParams(ExecutableInfo, Object)`, `hasAllParams(ExecutableInfo, Object)`. v2 provides the same capability via `MethodInfo.canResolveAllParameters(store, extras)` and `ClassInfo.inject(bean, store)`. Plan: **migrate callers to the new API** rather than adding compat methods to the store. Verify no external API exposes these three legacy methods.

### Still to survey

- [ ] **`BasicBeanStore.Entry<T>`** (public/extendable entry record, exposed via protected `createEntry(...)`) — confirm no external subclassers before deleting.
- [ ] Diff `cp.BeanCreator` vs `commons.inject.BeanCreator2` — note any legacy-only methods / behaviors that need to be ported.
- [ ] Diff `cp.BeanCreateMethodFinder` and `cp.ContextBeanCreator` against `BeanCreator2` — confirm their functionality is subsumed; list any callers that need rewriting rather than a rename.
- [ ] Verify SVL's needs (`VarResolver(.Builder)` / `VarResolverSession`): constructor-based `Var` instantiation, optional session bean lookup, bean-store copy semantics. Cover any gaps here before TODO-14 starts.
- [ ] Document any residual deltas (missing methods, renamed methods, behavior differences) in this file before proceeding to Phase 2.

## Phase 2 — Consumer inventory

- [ ] Enumerate direct callers of each legacy class across all modules (`juneau-marshall`, `juneau-rest-*`, `juneau-microservice-*`, `juneau-config`, `juneau-utest`).
- [ ] Classify each caller as:
  - **mechanical** — straightforward switch to the `*2` equivalent
  - **non-trivial** — needs an interface/adapter change (likely callers holding `BasicBeanStore` in public API)
- [ ] List the public API surfaces that expose `BasicBeanStore` / `BeanCreator` directly (these are the hardest part — every such method is a breaking change when renamed).

## Phase 3 — Migrate consumers to `*2`

- [ ] Migrate internal consumers first (no public API change, no compat risk).
- [ ] Migrate each public API surface. Since 9.5 allows simple breaking changes, the old overloads can be **replaced** rather than overloaded — pick whichever path yields the cleaner API per case. Document any breaking signature change in the 9.5 release notes.
- [ ] Include TODO-1 (REST server) as part of or prior to this phase.

## Phase 4 — Cutover rename

Once nothing still references the legacy classes:

- [ ] Delete `org.apache.juneau.cp.BasicBeanStore`, `BeanCreator`, `BeanBuilder`, `BeanCreateMethodFinder`, `ContextBeanCreator`.
- [ ] Rename the commons classes in place (drop the `2` suffix):
  - `BasicBeanStore2` → `BasicBeanStore`
  - `BeanCreator2`   → `BeanCreator`
  - (update any other `*2`-suffixed types and their references)
- [ ] Repo-wide find/replace of remaining `BasicBeanStore2` / `BeanCreator2` references.
- [ ] Re-run `./scripts/test.py -f`.

## Phase 5 — Cleanup

- [ ] Remove any transitional overloads that weren't already pruned in Phase 4.
- [ ] Update `juneau-docs` (any injection / BeanStore topics).
- [ ] Add release-notes entry to `juneau-docs/pages/release-notes/9.5.0.md` describing:
  - Deletion of `org.apache.juneau.cp.{BasicBeanStore,BeanCreator,BeanBuilder,BeanCreateMethodFinder,ContextBeanCreator}`.
  - New canonical home at `org.apache.juneau.commons.inject.*` (un-suffixed).
  - Migration guide for external consumers.

---

## Out of scope

- New features in the injection framework (e.g. scoping, lifecycle callbacks) — track separately.
- Merging `BeanStore` and `CreatableBeanStore` interfaces — if wanted, plan after the rename.

## Risk notes

- `BasicBeanStore` appears on several public builder APIs (e.g. `BeanBuilder`, `VarResolver.Builder`, various `Context.Builder` subclasses). 9.5's "simple breaking changes" budget covers this, but every such signature change needs a release-note entry.
- Test code across `juneau-utest` consumes the legacy classes heavily — migrate in batches per module to keep PRs reviewable.
- Coordinate with any in-flight PRs that still use the legacy classes before deletion in Phase 4.
- **TODO-14 is blocked on completion of Phase 4.** Land Phase 4 before starting TODO-14 to avoid dragging the SVL move through a rename cycle.

---

## Open questions

Answer these before (or early in) Phase 1. They drive the shape of the v2 classes that Phase 3 will migrate onto.

1. **`BeanBuilder<T>` fate.** The legacy `org.apache.juneau.BeanBuilder<T>` (note: root package, *not* `cp`) is the base class for many domain fluent builders and carries a `BeanStore` + `type`/`impl` slot.
   - **Context:** TODO-16 deletes a large fraction of the REST-side `BeanBuilder<T>` consumers by replacing `RestContext.Builder` with memoized/resettable fields. Defer this decision until TODO-16 Phases 1–2 land so the choice is made against the *actual* post-refactor footprint.
   - Options:
     - a) Keep it; retarget its internals to `BeanStore` (commons). Remaining (non-REST) domain builders are unaffected.
     - b) Delete it and rewrite each surviving domain builder to hold a plain `BeanStore` reference.
     - c) Replace with a minimal commons equivalent (`org.apache.juneau.commons.inject.BeanBuilder`) with a trimmed surface.

2. **`BeanCreateMethodFinder` replacement.** Used by rest-server / context builders to locate `static Optional<T> createX(...)` factory methods. Does `BeanCreator2`'s existing method discovery cover this, or do we port a dedicated finder?

3. **`ContextBeanCreator` replacement.** Thin wrapper used during `Context.Builder` initialization. Is it subsumed by `BeanCreator2`, or does it need to be ported (possibly renamed)?

4. **`BasicBeanStore.Void` sentinel.** Pick one of the two paths listed in Phase 1 (port `Void` vs. redesign `@Rest.beanStore()` default). Affects `@Rest` annotation processing code in rest-server.

5. **`INSTANCE` constant placement.** On `BasicBeanStore2` (concrete class) or on `BeanStore` (interface, as a `static final`)? The latter is cleaner; confirm no serialization/reflection code depends on the concrete type.

6. **Executable-resolution helper migration.** Are the three legacy methods (`getMissingParams`, `getParams`, `hasAllParams`) exposed anywhere outside `juneau-rest-server`'s internals? If so, migrating callers (rather than keeping compat methods) may push past the 9.5 "simple breaking changes" budget — verify the blast radius before committing to delete.

7. **Phase 0 deprecations — ship separately?** The `@Deprecated` annotations in Phase 0 could land in a 9.4.x patch to give downstream consumers a heads-up before the 9.5 break. Decide whether to ship Phase 0 early or roll it into the same 9.5 PR train.
