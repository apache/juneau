# TODO-26 — Retire `org.apache.juneau.BeanBuilder<T>`

**Status:** plan

**Target release:** 9.5.0 (ship together with TODO-15 Phase 4 — legacy `BeanCreator` deletion).

**Why this is its own ticket:** `BeanBuilder<T>` is the last live consumer of legacy `BeanCreator`
from `juneau-marshall` source. It is also a leaky abstraction that bakes DI dispatch into a
captive base class — eighteen public-API `Builder` subclasses inherit `type()` / `impl()` slots
they mostly don't need, and the one place those slots *do* matter (test-driven subclass
substitution on `MethodExecStore` / `ThrownStore`) already has a cleaner expression via
`BeanInstantiator`.

---

## Background

`org.apache.juneau.BeanBuilder<T>` lives in `juneau-marshall` (root package). It is a thin base
for "X.Builder" classes whose terminal `build()` method either:

1. returns an explicit `impl` if the user called `impl(value)`,
2. delegates to subclass-supplied `buildDefault()` when no `type` override is set, or
3. instantiates an alternate subtype via `BeanCreator.of(type, beanStore).builder(BeanBuilder.class, this).run()`.

The legacy `BeanCreator.builder(Class, Object)` call in `creator()` walks the *runtime* class
hierarchy of `this` and registers the builder under each superclass type up to (but not equal
to) `BeanBuilder.class`. That is how subclass constructors like `A1(MethodExecStore.Builder b)`
resolve their builder argument when the test does
`MethodExecStore.create().type(A1.class).build()`.

### The 18 subclasses, by actual usage

A full grep of `.type(...)` and `.impl(...)` call sites against these classes reveals three
distinct tiers:

#### Tier 1 — list builders (5 classes)

`juneau-rest-server`:
- `org.apache.juneau.rest.converter.RestConverterList.Builder`
- `org.apache.juneau.rest.guard.RestGuardList.Builder`
- `org.apache.juneau.rest.matcher.RestMatcherList.Builder`
- `org.apache.juneau.rest.processor.ResponseProcessorList.Builder`
- `org.apache.juneau.rest.arg.RestOpArgList.Builder`

Properties:
- `.type(...)` — **zero** external call sites (main code or tests).
- `.impl(...)` — exactly one pattern, in `RestContext` / `RestOpContext`:
  ```java
  bs.createBeanFromMethod(ResponseProcessorList.class, ...).ifPresent(x -> v.get().impl(x));
  return v.get().build().toArray();
  ```
  (i.e. "if `@RestInject` produced a complete replacement, use that; else build from collected
  entries.") Trivially expressible as `x != null ? x : b.build()` at the call site.

These have no business extending `BeanBuilder`. They are pure collection builders.

#### Tier 2 — heavy / user-facing builders (10 classes)

`juneau-marshall`:
- `org.apache.juneau.svl.VarResolver.Builder`
- `org.apache.juneau.parser.ParserSet.Builder`
- `org.apache.juneau.serializer.SerializerSet.Builder`
- `org.apache.juneau.encoders.EncoderSet.Builder`
- `org.apache.juneau.cp.Messages.Builder`
- `org.apache.juneau.cp.FileFinder.Builder`

`juneau-rest-server`:
- `org.apache.juneau.rest.RestChildren.Builder`
- `org.apache.juneau.rest.RestOperations.Builder`
- `org.apache.juneau.rest.staticfile.StaticFiles.Builder`
- `org.apache.juneau.rest.logger.CallLoggerRule.Builder`

Properties:
- `.type(...)` calls in main code: exactly **one**, and it's a no-op:
  ```java
  // RestContext.java line 1120 — sets type to its own default. Pure noise.
  var v = Value.of(RestChildren.create(bs).type(RestChildren.class));
  ```
- `.impl(...)` is used the same way as Tier 1 — `@RestInject`-replaces-the-result.

These builders inherit `type()` as dead API. They drop `BeanBuilder` cleanly; the `impl()`
"replace the result" semantic moves to the call site, same as Tier 1.

#### Tier 3 — data-store builders (3 classes)

`juneau-rest-server`:
- `org.apache.juneau.rest.stats.MethodExecStore.Builder`
- `org.apache.juneau.rest.stats.MethodExecStats.Builder`
- `org.apache.juneau.rest.stats.ThrownStore.Builder`

This is where `.type(...)` **actually matters**. Eleven test sites exercise it:

```java
// juneau-utest MethodExecStore_Test
assertInstanceOf(A1.class, MethodExecStore.create().type(A1.class).build());
assertThrowsWithMessage(..., ()->MethodExecStore.create().type(A4.class).build());
assertInstanceOf(A5c.class, MethodExecStore.create(bs).type(A5c.class).build());
// ...etc, plus the equivalent set on ThrownStore_Test

// juneau-marshall-rdf-utest
assertNotNull(RdfSerializer.create().type(RdfSerializer.class).build());  // also Tier 3-flavoured
assertNotNull(RdfParser.create().type(RdfParser.class).build());

// juneau-utest config stores
var fs = MemoryStore.create().type(MemoryStore.class).build();
var fs = ClasspathStore.create().type(ClasspathStore.class).build();
```

This is documented public API: "give me a subclass-substituted instance, resolved through the
bean store, with this builder injected as a constructor parameter." Preserve as a native
method on each Tier-3 builder, backed by `BeanInstantiator` directly.

(`MemoryStore.Builder`, `ClasspathStore.Builder`, `RdfSerializer.Builder`,
`RdfParser.Builder` are **not** `BeanBuilder` subclasses — they hit `type()` via a different
inheritance chain (`ConfigStore.Builder` / `Serializer.Builder` / `Parser.Builder`). Their
`type()` semantics are out of scope here, but kept on the radar in case they trip the same
breakage.)

### Already-migrated reference cases

These already follow the target pattern — RestContext drives `type` / `impl` via
`BeanInstantiator`, no `BeanBuilder`-derived builder involved:

```992:1004:juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java
private final Memoizer<StaticFiles> staticFiles = memoizer(() -> {
    var bs = beanStore();
    var creator = BeanInstantiator.of(StaticFiles.class, bs).type(BasicStaticFiles.class).noBuilder();
    bs.getBeanType(StaticFiles.class).ifPresent(creator::type);
    getRestAnnotationsForProperty(PROPERTY_staticFiles)
        .map(ai -> ai.inner().staticFiles())
        .filter(c -> c != StaticFiles.Void.class)
        .reduce((first, second) -> second)
        .ifPresent(creator::type);
    bs.createBeanFromMethod(StaticFiles.class, resource().get(), RestContext::isRestInjectMethod).ifPresent(creator::impl);
    return creator.asOptional().orElse(null);
});
```

Same pattern is used for `SwaggerProvider`, `CallLogger`, and `DebugEnablement`. This is the
template Tier 1 and Tier 2 will converge on (with the `creator` call replaced by direct
builder invocation when there's a real `Builder` with config state).

### Decision: full retirement (Option D)

Three options were originally considered and a fourth surfaced in review:

| Option | Outcome | Verdict |
| ------ | ------- | ------- |
| A | Rewrite `BeanBuilder<T>` body to use `BeanInstantiator` internally. Keep public API of 18 subclasses unchanged. | Superseded. Unblocks TODO-15 Phase 4 but keeps the leaky abstraction alive. |
| B | Make `BeanBuilder<T>` extend `BeanInstantiator.Builder<T>`. | Rejected — `build()` semantics conflict. |
| C | Delete `BeanBuilder<T>` entirely, inline state into each subclass. | Rejected as too coarse — was framed as 18-class churn for no win. |
| **D** | **Strip `extends BeanBuilder<T>` from all 18 subclasses across three tiers; move `type`/`impl` semantics to call sites or native methods; delete `BeanBuilder<T>`.** | **Selected.** Same churn as C but with surgical handling per tier — Tier 3 keeps the `type()`/`impl()` public API natively, Tiers 1 & 2 drop dead inherited API. |

The conceptual model becomes: **builders collect data; `BeanInstantiator` drives DI**. The
already-migrated `StaticFiles` / `SwaggerProvider` / `CallLogger` / `DebugEnablement` paths are
the template.

---

## Phases

### Phase A — Tier 1: list builders (5 classes)

Strip `extends BeanBuilder<T>` from:

- `RestConverterList.Builder`
- `RestGuardList.Builder`
- `RestMatcherList.Builder`
- `ResponseProcessorList.Builder`
- `RestOpArgList.Builder`

For each:

1. Remove `extends BeanBuilder<TheClass>`.
2. Add own `BeanStore` (or `WritableBeanStore`) field + accessor.
3. Constructor takes the bean store directly and stores it.
4. Remove `@Override public Builder impl(Object)`, `@Override public Builder type(Class<?>)`,
   `@Override protected TheClass buildDefault()`.
5. Rename `buildDefault()` body into a plain `public TheClass build()` that returns
   `new TheClass(this)`.

At the RestContext / RestOpContext call sites, replace the `impl()` REPLACE pattern with
explicit value selection:

```java
// Before
var v = Value.of(ResponseProcessorList.create(bs));
getRestAnnotationsForProperty(PROPERTY_responseProcessors)
    .forEach(ai -> v.get().add(ai.inner().responseProcessors()));
bs.createBeanFromMethod(ResponseProcessorList.class, resource().get(), RestContext::isRestInjectMethod, v.get())
    .ifPresent(x -> v.get().impl(x));
return v.get().build().toArray();

// After
var b = ResponseProcessorList.create(bs);
getRestAnnotationsForProperty(PROPERTY_responseProcessors)
    .forEach(ai -> b.add(ai.inner().responseProcessors()));
var override = bs.createBeanFromMethod(ResponseProcessorList.class, resource().get(),
        RestContext::isRestInjectMethod, b).orElse(null);
return (nn(override) ? override : b.build()).toArray();
```

Run `./scripts/test.py -t`. Expect zero test breakage in this phase — `.type()` is never called
on these in any test.

### Phase B — Tier 2: heavy / user-facing builders (10 classes)

Same surgery as Phase A on:

`juneau-marshall`:
- `VarResolver.Builder`
- `ParserSet.Builder`
- `SerializerSet.Builder`
- `EncoderSet.Builder`
- `Messages.Builder`
- `FileFinder.Builder`

`juneau-rest-server`:
- `RestChildren.Builder`
- `RestOperations.Builder`
- `StaticFiles.Builder`
- `CallLoggerRule.Builder`

Specific notes:

- **`RestChildren`** — drop the no-op `.type(RestChildren.class)` call from
  `RestContext.restChildren` memoizer; it sets the type to its own default.
- **`VarResolver.Builder`** — the existing copy constructor `Builder(VarResolver copyFrom)`
  passes `copyFrom.getClass()` to `super(...)`. After removing `BeanBuilder`, this becomes
  dead state. Verify and drop.
- **`StaticFiles.Builder`** — `buildDefault()` returns `new BasicStaticFiles(this)`, **not**
  `new StaticFiles(this)`. Keep that — the natural `build()` returns the concrete impl
  directly. The interface stays, the builder just produces `BasicStaticFiles` like before.
- **`Messages.Builder`** and **`FileFinder.Builder`** — confirm no `.type()` callers outside
  the BeanBuilder-inherited contract before stripping.

For each Tier 2 builder, sweep RestContext / RestOpContext / call sites for `.impl(x)` and
rewrite to the "explicit override variable" pattern from Phase A:

```java
// RestContext lines 685, 710, 777, 792-793, 821-822, 927, 950, 966, 1039, 1057, 1100, 1147
bs.createBeanFromMethod(EncoderSet.class, ..., v.get()).ifPresent(x -> v.get().impl(x));
// → rewrite caller to select `override ?? b.build()` once at the top of the memoizer.
```

Run full test suite at the end of Phase B. The `.impl(x)` rewrites are mechanical but
numerous; this is where the highest risk of subtle ordering / `Memoizer<Builder>` plumbing
regressions lives. Smoke tests to run:

- `RestContext_Test`, `RestContext_Precedence_Test`
- `RestOpContext_Test`, `RestOpContext_*_Test`
- `Rest_BeanCreatorOverrides_Test` (verifies `@RestInject` override path)
- All `juneau-rest` integration tests under `juneau-rest/juneau-rest-mock`
- `MessagesTest`, `FileFinderTest`, `VarResolverTest`, `EncoderSetTest`, `SerializerSetTest`,
  `ParserSetTest`

### Phase C — Tier 3: data-store builders (3 classes)

Strip `extends BeanBuilder<T>` from:

- `MethodExecStore.Builder`
- `MethodExecStats.Builder`
- `ThrownStore.Builder`

**Preserve `type()` and `impl()` as native methods** on each builder, backed by
`BeanInstantiator` directly. Reference shape (illustrated on `MethodExecStore.Builder`):

```java
public static class Builder {

    private final BeanStore beanStore;
    private Class<? extends MethodExecStore> type;
    private MethodExecStore impl;
    private ThrownStore thrownStore;
    private Class<? extends MethodExecStats> statsImplClass;

    protected Builder(BeanStore beanStore) {
        this.beanStore = beanStore;
    }

    public BeanStore beanStore() { return beanStore; }

    public Builder impl(Object value) {
        @SuppressWarnings("unchecked")
        var t = (MethodExecStore) value;
        impl = t;
        return this;
    }

    public Builder type(Class<?> value) {
        @SuppressWarnings("unchecked")
        var t = (Class<? extends MethodExecStore>) value;
        type = t;
        return this;
    }

    // ...existing fluent setters: thrownStore(...), statsImplClass(...) etc.

    public MethodExecStore build() {
        if (nn(impl))
            return impl;
        if (type == null || type == MethodExecStore.class)
            return new MethodExecStore(this);
        return BeanInstantiator.of(type, beanStore)
            .noBuilder()
            .addBean(Builder.class, this)
            .run();
    }
}
```

Key points:

- `.noBuilder()` is critical (same reason as the existing `StaticFiles` migration — see
  TODO-15 Phase 9 history). Without it, `BeanInstantiator` will auto-detect a builder type
  and may invoke an inherited static `create()` factory on the superclass, returning the wrong
  type.
- The legacy `BeanCreator.builder(BeanBuilder.class, this)` semantics walked the runtime class
  hierarchy of `this` up to (but not equal to) `BeanBuilder`, registering the builder under
  each superclass type. Now there *is* no superclass hierarchy — the only relevant type is the
  builder's own class. `addBean(Builder.class, this)` matches the parameter-resolution
  contract that test subclass constructors like `A1(MethodExecStore.Builder b)` need.

Tests affected (all should pass unchanged):

- `MethodExecStore_Test` — `a02_builder_implClass`, `a04_builder_implClass_bad`,
  `a05_builder_beanFactory`
- `ThrownStore_Test` — `b01..b05`, `b06_statsImplClass`
- `MethodExecStats_Test` (if any `.type()` paths)

If exception text changes (BeanCreator → BeanInstantiator differences from TODO-15 Phase 9):
update the test's `assertThrowsWithMessage` to the v2 wording. Do **not** mimic legacy text.

### Phase D — Delete `BeanBuilder<T>`

Once Phases A–C are merged:

1. Confirm `grep -r "extends BeanBuilder<" juneau-core juneau-rest` returns **zero**
   results.
2. Confirm `grep -r "BeanBuilder" juneau-core/juneau-marshall/src/main/java` returns only the
   file itself (and any incidental Javadoc references — which will be cleaned up in step 4).
3. Delete `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanBuilder.java`.
4. Sweep remaining Javadoc `{@link BeanBuilder}` / `{@link org.apache.juneau.BeanBuilder}`
   references and either remove or retarget.
5. Resume TODO-15 Phase 4 (delete legacy `BeanCreator`, `BasicBeanStore`,
   `BeanCreateMethodFinder`, the captive `BeanBuilder` in `cp` if any; rename
   `BasicBeanStore2` → `BasicBeanStore`).

---

## Public API risk

This is a hard break in 9.5.0. The breaking surfaces:

1. **Tier 1 & Tier 2 `.type()` removal.** Removed from 15 `Builder` classes that inherited it
   from `BeanBuilder`. Grep across the codebase (main + tests) found zero call sites for
   these classes. External callers (if any) that rely on the inherited `.type()` on, e.g.,
   `VarResolver.Builder` would break. Risk assessment: extremely low — every observed in-repo
   `.type()` use is on Tier 3 or on already-`BeanInstantiator`-based paths.

2. **Tier 1 & Tier 2 `.impl()` removal.** Removed from same 15 classes. The semantic moves to
   the caller (`override ?? b.build()`). External callers that used `.impl(...)` directly on,
   e.g., a `VarResolver.Builder` would break. Again, no in-repo call sites outside the
   `createBeanFromMethod` pattern, which is internal to `RestContext` / `RestOpContext`.

3. **Tier 3 `.type()` / `.impl()` preserved as native methods.** Signatures unchanged. The
   exception message text from a failed subclass instantiation may differ (BeanCreator → v2
   BeanInstantiator); already accepted in TODO-15 Phase 9.

4. **`BeanBuilder<T>` removed from `juneau-marshall`.** Any external code that:
   - subclassed `BeanBuilder<T>` directly, **or**
   - referenced it via `protected BeanCreator<? extends T> creator()` overrides
   - referenced it via instanceof / cast paths

   Mitigations:
   - Repo has zero internal `extends BeanBuilder` outside the 18 enumerated subclasses.
   - The class is framework infrastructure with effectively no documented external use.
   - Ships in 9.5.0 alongside many other deliberate breaking changes from the TODO-15 cutover.
   - Release notes will call this out under `juneau-marshall` / `juneau-rest-server` breaking-
     change sections with the migration recipe ("subclass directly; use `BeanInstantiator` for
     DI-driven instantiation").
   - No `@Deprecated` transition is offered (consistent with the TODO-15 hard-break policy).

---

## Acceptance criteria

1. `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanBuilder.java` is deleted.
2. Zero matches for `extends BeanBuilder<` across `juneau-core/` and `juneau-rest/`.
3. All 18 enumerated `Builder` classes compile, with:
   - Tier 1 (5 classes): no `type()` / `impl()` methods.
   - Tier 2 (10 classes): no `type()` / `impl()` methods.
   - Tier 3 (3 classes): `type()` / `impl()` preserved as native methods, backed by
     `BeanInstantiator`.
4. `RestContext` / `RestOpContext` `@RestInject` override paths use the explicit
   `override ?? b.build()` selection pattern, not `.impl(x)` on the builder.
5. The full `juneau-utest` suite passes, including all 11 `.type()` test sites on Tier 3
   builders.
6. TODO-15 Phase 4 (`BeanCreator` deletion + `BasicBeanStore2` → `BasicBeanStore` rename) is
   unblocked.

---

## Out of scope

- Moving the remaining builders (`VarResolver`, `Messages`, `FileFinder`, etc.) to
  `juneau-commons`. Once they no longer depend on `BeanBuilder`, this becomes trivial — but
  it's a separate packaging concern.
- The `type()` semantics on `MemoryStore.Builder`, `ClasspathStore.Builder`,
  `RdfSerializer.Builder`, `RdfParser.Builder`. These hit `type()` via different inheritance
  chains (`ConfigStore.Builder` / `Serializer.Builder` / `Parser.Builder`) and are unaffected
  by this work.
- Inlining the few remaining `type()` / `impl()` slots on Tier 3 builders into their
  containing classes (i.e. removing the `Builder` static nested class entirely). Possible but
  not driven by this ticket.
