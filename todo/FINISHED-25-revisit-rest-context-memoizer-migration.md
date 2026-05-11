# Revisit `RestContext` four-memoizer migration to v2 inject API

> Spawned out of TODO-15 ("Replace `BasicBeanStore` / `BeanCreator` with v2
> equivalents") on 2026-05-08 after an attempted migration of the four
> memoizers below produced regressions on documented `@Rest` annotation
> contracts plus a real Jetty integration-test failure. The four memoizers
> were reverted to legacy `BeanCreator.of(...)` and parked here for follow-up.

## Scope

Migrate the following sites in
`juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java`
from legacy `BeanCreator.of(...).type(...).run()` to v2
`BeanInstantiator.of(...).beanSubType(...).run()`:

| Memoizer | Default impl | RestContext line (approx) |
|---|---|---|
| `callLogger` | `BasicCallLogger` | ~508 |
| `debugEnablement` | `BasicDebugEnablement` | ~560 |
| `staticFiles` | `BasicStaticFiles` | ~976 |
| `swaggerProvider` | `BasicSwaggerProvider` | ~996 |

Plus the related `BeanCreator.of(rc2, (BasicBeanStore) bs).run()` user-supplied
child-resource path at `RestContext` ~line 1120, and `RestOpContext`'s
`partSerializer` `BeanCreator.of(...)` site (1 hit).

Also reconsider whether `BasicCallLogger.init(BeanStore)` should keep its
post-revert state or have its `.ifPresent(...)` defensive style restored
(currently restored to legacy `.orElse(null)` / equivalent — see "Outcome of
2026-05-08 attempt" below).

## Why this is hard (root-cause analysis from 2026-05-08 attempt)

Two distinct failure modes surfaced when the migration was attempted in-tree
on 2026-05-08:

### 1. Annotation-override path silently returns `null`

Test fixtures like `@Rest(debugEnablement=CustomDebugEnablement.class)` where
`CustomDebugEnablement extends BasicDebugEnablement` produce `null` instead of
the configured custom class. The chain
`getRestAnnotationsForProperty(...).reduce(...).ifPresent(creator::beanSubType)`
correctly drives `creator.beanSubType(CustomDebugEnablement.class)`, but
`creator.asOptional()` returns empty — i.e. `BeanInstantiator` silently fails
to instantiate the subtype.

Root cause is one or more of:

- **Builder discovery picks the wrong builder.** `CustomDebugEnablement`
  doesn't declare its own builder; it inherits `BasicDebugEnablement.Builder`
  (or further up `DebugEnablement.Builder` on the interface). Legacy
  `BeanCreator.findBeanImpl` walked superclass + interface chains; v2
  `BeanInstantiator.findBuilderType` originally only walked superclasses.
  Already partially fixed in TODO-15 by switching to `getAllParents()`, but
  the loose-builder fallthrough is fragile when the discovered
  `Builder.build()` returns the parent type and the runtime instance isn't
  an instance of the requested subtype.

- **`asOptional().orElse(null)` swallows the diagnostic.** Even when
  `run()` would throw a useful exception ("Could not find a way to
  instantiate `CustomDebugEnablement`"), `asOptional()` converts it to an
  empty Optional and the memoizer hands back `null`, which surfaces in the
  test as `expected: <CustomDebugEnablement> but was: <null>`. Use of
  `.run()` (which propagates the exception) plus a try/catch is preferable
  for diagnostic visibility, but doesn't fix the underlying instantiation
  failure.

### 2. `init(BeanStore)` zero-out semantics

Several `Basic*` defaults override builder-configured fields with
`.orElse(null)` of a missing bean lookup. Concretely:

- `BasicCallLogger.init(beanStore)` does
  `b.logger(beanStore.getBean(Logger.class).orElse(null))` and
  `b.thrownStore(beanStore.getBean(ThrownStore.class).orElse(null))` —
  fine when the live REST bean store has those beans, but during early
  init the `Logger` / `ThrownStore` may not yet be registered, so the
  builder-set defaults get nulled.
- `BasicStaticFiles` / its builder wires up roots/paths via the
  `BasicStaticFiles.Builder` set up by the legacy constructor
  `BasicStaticFiles(BasicBeanStore)`. The v2 path discovers
  `StaticFiles.Builder` (interface-level) and calls `.build()` with default
  state — producing a `BasicStaticFiles` with no configured roots and no
  bundled `htdocs/` mapping. The default static-file mapping is never
  applied, hence the 404 on `/htdocs/themes/dark.css`.
- Same shape for `BasicSwaggerProvider` (needs `Messages` /
  `JsonSerializer.Builder` / `JsonParser.Builder` / `JsonSchemaGenerator`
  from the bean store at construction time; v2 path doesn't supply them).
- Same shape for `BasicDebugEnablement` (needs `ResourceSupplier` /
  `VarResolver` / `Enablement`).

The legacy `BeanCreator.of(...).type(BasicStaticFiles.class).run()` selected
the `BasicStaticFiles(BasicBeanStore)` constructor, which **directly**
constructs a configured builder from the bean store. The v2
`BeanInstantiator.of(...).beanSubType(BasicStaticFiles.class).run()` picks the
builder path first and never invokes that constructor — even when it falls
through, the builder it discovered has empty defaults.

## Two viable approaches

### (a) Tighten each `Basic*.init(BeanStore)` to tolerate missing beans

Per-class change. For each affected default, replace `.orElse(null)`
patterns with `.orElseGet(() -> sensibleDefault)` or `.ifPresent(...)`.
Specifically:

- **`BasicCallLogger.init`** — switch the two `.orElse(null)` calls to
  `.ifPresent(b::logger)` / `.ifPresent(b::thrownStore)` so builder-set
  defaults stick when the runtime store doesn't have them. Strictly safer
  than legacy.
- **`BasicDebugEnablement.init`** — must tolerate missing `ResourceSupplier`
  / `VarResolver` / `Enablement`. Audit all `.orElse(null)` in the init
  chain.
- **`BasicStaticFiles` / `BasicStaticFiles.Builder`** — must apply the
  bundled `htdocs/` mapping even when the v2 path hands the constructor an
  empty builder. May require a `Builder.applyDefaults()` that the legacy
  direct-constructor path skipped (because it never went through the
  builder). This is the **hardest** of the four to fix.
- **`BasicSwaggerProvider`** — must tolerate missing `Messages` /
  `JsonSerializer.Builder` / `JsonParser.Builder` / `JsonSchemaGenerator`.

**Plus** debug the `BeanInstantiator` annotation-override path until
`creator.beanSubType(CustomDebugEnablement.class)` actually instantiates
`CustomDebugEnablement`. Likely requires the loose-builder fallthrough to
guarantee a constructor pass when the discovered builder produces the wrong
runtime type.

### (b) Rework `RestContext` init order so framework prerequisites land first

Make the `RestContext` constructor / init sequence register all the
prerequisite beans (`Logger`, `Messages`, `ResourceSupplier`,
`VarResolver`, `JsonSchemaGenerator`, etc.) into the bean store **before**
any of the four memoizers can fire. With every prerequisite present,
`Basic*.init(beanStore).orElse(null)` becomes a non-issue.

Larger surgery in `RestContext.java`. Lower per-class blast radius but
needs careful audit of the actual init order to avoid deadlocks
(memoizers calling memoizers).

## Outcome of 2026-05-08 attempt

The four memoizers were migrated to v2, then **reverted** after these
regressions surfaced:

**`juneau-utest`** — 3 failures in
`juneau-utest/src/test/java/org/apache/juneau/rest/annotation/Rest_BeanCreatorOverrides_Test.java`:

1. `a01_customDebugEnablement_viaAnnotation` —
   `@Rest(debugEnablement=CustomDebugEnablement.class)` returns `null`.
2. `c01_customSwaggerProvider_viaAnnotation` —
   `@Rest(swaggerProvider=CustomSwaggerProvider.class)` returns `null`.
3. `d01_childAnnotationOverridesParent` — most-derived
   `@Rest(debugEnablement=CustomDebugEnablement2.class)` on a subclass
   returns `null`.

These are documented public contracts of `@Rest`, not internal parity drift.

**`juneau-examples-rest-jetty-ftest`** — 1 failure in
`ContentComboTestBase`: GETs `/?plainText=true&Accept=text/html`, which
embeds `@import '/htdocs/themes/dark.css'`, then GETs the CSS. With the
`staticFiles` migration in place the second GET 404s, so the assertion that
the served CSS contains `.menu-item {` fails.

**Salvaged** (kept under TODO-15, not reverted):

- 16 utility-class factories flipped to `WritableBeanStore` (full list in
  `TODO-15-replace-basicbeanstore-with-v2.md` lines 12 / 131).
- `BeanInstantiator` improvements: loose-builder fallthrough,
  interface-builder search in `findBuilderType`, debug-log support.
- `BasicBeanStore2` / `WritableBeanStore` / `BeanStore` v2 surface
  additions.

## Suggested order of operations when revisiting

1. **Pick approach (a) vs (b).** Approach (a) is per-class and easier to
   review; approach (b) is more invasive but makes the bean-store contract
   easier to reason about going forward.

2. **Add a focused regression test before changing anything.** A
   `RestContext_Memoizer_Init_Test` that asserts
   - `@Rest(callLogger=Custom)` instantiates the custom class,
   - same for `debugEnablement` / `staticFiles` / `swaggerProvider`,
   - `@Rest(staticFiles=…)` plus a `BasicStaticFiles` default both serve
     `/htdocs/themes/dark.css`,
   - `BasicCallLogger.log(...)` does not NPE when the bean store has no
     `Logger` registered.

3. **Audit `Basic*.init(BeanStore)` zero-out semantics.** Each of the four
   classes should be checked for `.orElse(null)` on optional bean lookups,
   plus any sub-class-overridable hooks they expose.

4. **Make the migration small.** Migrate one memoizer at a time, run the
   regression test plus full `./scripts/test.py -f`, commit, repeat.

5. **Address the related sites** (`RestContext` user-child-resource path,
   `RestOpContext.partSerializer`) only after all four memoizers are clean.

## Acceptance criteria

- All four memoizers in `RestContext.java` use `BeanInstantiator.of(...)`.
- The user-child-resource `BeanCreator.of(rc2, ...)` path migrated.
- `RestOpContext.partSerializer` migrated.
- `BasicCallLogger` / `BasicDebugEnablement` / `BasicStaticFiles` /
  `BasicSwaggerProvider` `init` methods tolerate missing optional beans
  without zeroing builder-set defaults.
- All `Rest_BeanCreatorOverrides_Test` cases pass.
- `juneau-examples-rest-jetty-ftest` passes (static files served).
- `./scripts/test.py -f` green end-to-end.
- This file moved to `todo/FINISHED-25-*.md` per the archive convention.
