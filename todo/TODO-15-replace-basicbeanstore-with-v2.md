# Replace `BasicBeanStore` / `BeanCreator` with v2 equivalents

Eliminate the legacy injection stack in `org.apache.juneau.cp` (`BasicBeanStore`, `BeanCreator`, `BeanBuilder`, `BeanCreateMethodFinder`, `ContextBeanCreator`) in favor of the rewritten classes already present in `org.apache.juneau.commons.inject` (`BasicBeanStore2`, `BeanInstantiator`, `BeanStore`, `CreatableBeanStore`, etc.). Once the replacement lands, the `2` suffix is dropped from `BasicBeanStore2` (→ `BasicBeanStore`) and the legacy classes are removed. `BeanInstantiator` already has its final name.

**Target release:** **9.5.0** — semi-major release permitting simple breaking changes. Hard-break migration (no deprecation cycle on the final rename).

**Sequencing:**
- **Lands before TODO-14.** TODO-14 (SVL → commons) consumes the final, un-suffixed class names produced by this TODO, so this work must complete first.
- ~~TODO-1 (REST server API → `BeanStore2`)~~ — **DONE.** `RestContext` and all rest-server memoizers now use `BasicBeanStore2` / `WritableBeanStore` directly. No `cp.BasicBeanStore` imports remain in `juneau-rest-server` source files. Legacy references in `RestInject.java` / `RestInit.java` are **Javadoc-only**.
- ~~`RestContext.java` / `RestOpContext.java` `BeanCreateMethodFinder` migration (49 sites)~~ — **DONE (PR landed 2026-05-07).** All 49 call sites rewritten to `bs.createBeanFromMethod(...).ifPresent(...)`. Field types flipped to `WritableBeanStore`. Mock-test clients (`MockRestClient`, `NgMockRestClient`, `Swagger_Test`) re-pointed at the v2 surface. `MethodInvoker.invoke(BasicBeanStore, Object)` → `invoke(BeanStore, Object)`. Full build + tests green.
- ~~`BeanInstantiator` loose-builder relaxation~~ — **DONE (2026-05-07).** Strict "Builder.build() must return exact beanSubType" check replaced with a runtime check + factory-method/constructor fallthrough. Existing `BeanInstantiator_Test#d28` rewritten to assert the new behavior. Full build + tests green.
- ~~Cascade-builders signature flip (Phase 3 sub-task)~~ — **DONE (2026-05-08).** Sixteen utility-class `create(BasicBeanStore)` factories flipped to `create(WritableBeanStore)`: `RestConverterList`, `RestGuardList`, `RestMatcherList`, `RestOpArgList`, `ResponseProcessorList`, `RestOperations`, `RestChildren`, `MethodExecStore`, `ThrownStore`, `FileFinder`, `StaticFiles`, `BasicStaticFiles.create(...)`, `DebugEnablement`, `SwaggerProvider`, `EncoderSet`, `ParserSet`, `SerializerSet`. `BeanBuilder<T>` parent constructor still takes legacy `BasicBeanStore`; downcast is performed inside each `Builder` constructor (acceptable transitional state — legacy `BasicBeanStore implements WritableBeanStore` makes the downcast safe). Casts at the call sites in `RestContext` / `RestOpContext` removed. `BasicBeanStore implements WritableBeanStore` ensures all existing legacy callers passing `BasicBeanStore` still compile (auto-upcast to interface). Full build + tests green.
- ~~`BeanCreator.of(Class, BeanStore)` overload~~ — **DONE (2026-05-08).** Widened `BeanCreator` to accept any `BeanStore` parent (legacy or v2): added `of(Class, BeanStore)` static factory, changed protected ctor to `(Class, BeanStore)` with internal branch (legacy parent → `BasicBeanStore.of(legacy)`; v2 parent → `new BasicBeanStore2(parent)`), widened `store` field to `WritableBeanStore`, replaced `store.add(...)` with `store.addBean(...)`. Legacy `of(Class, BasicBeanStore)` overload retained for binary compat. Net effect: every `BeanCreator.of(...)` call site in `RestContext` / `RestOpContext` / `DebugEnablement.Builder` dropped its `(BasicBeanStore)` downcast (6 casts removed). The remaining `(BasicBeanStore)` casts in `RestContext` / `RestOpContext` are all on direct legacy-static API (`BasicBeanStore.of(...)`, `BasicBeanStore.create().overridingParent(...)`), not `BeanCreator.of(...)`.
- ~~`BeanBuilder<T>` widening to `WritableBeanStore`~~ — **DONE (2026-05-08).** Field, constructor, and public `beanStore()` accessor all flipped from `BasicBeanStore` to `WritableBeanStore` (chosen over read-only `BeanStore` so callers retaining write access — e.g. `VarResolver.Builder.bean(...)` calling `super.beanStore().addBean(...)` — keep working). All 14 cascade-builder `super(X.class, (BasicBeanStore) beanStore)` casts dropped. Two standalone Builders (`SwaggerProvider.Builder.beanStore`, `ThrownStats.Builder.beanStore`) and one constructor (`BasicSwaggerProvider(BasicBeanStore)`) flipped to match. Two cascade-followon factories (`MethodExecStats.create(...)`, `ThrownStats.create(...)`) flipped. Three internal helper signatures (`ResponseProcessorList.instantiate(...)`, `EncoderSet.instantiate(...)`, `VarResolver.toVar(...)`) widened to `BeanStore`. Two field types (`MethodExecStore.beanStore`, `ThrownStore.beanStore`) flipped to `WritableBeanStore`. Single residual cast: `VarResolver` line 257 still calls `BasicBeanStore.of((BasicBeanStore) builder.beanStore())` because the legacy static `BasicBeanStore.of(...)` requires a legacy parent — will go away when that static is widened or removed in Phase 4. Full build + tests + jetty-ftest green.
- ~~Four-memoizer migration to `BeanInstantiator` (Phase 3 sub-task)~~ — **ATTEMPTED + REVERTED (2026-05-08).** `callLogger` / `debugEnablement` / `staticFiles` / `swaggerProvider` were migrated to `BeanInstantiator.of(...).beanSubType(...).run()`; this broke documented `@Rest(callLogger=…)` / `@Rest(debugEnablement=…)` / `@Rest(swaggerProvider=…)` annotation overrides (3 `Rest_BeanCreatorOverrides_Test` failures) plus `juneau-examples-rest-jetty-ftest` (static files no longer served — `/htdocs/themes/dark.css` 404s). Root causes: (a) `BeanInstantiator` builder discovery picks parent-type builders whose `build()` produces empty-default state — `BasicStaticFiles.Builder` etc. never apply the bundled `htdocs/` mapping; (b) `Basic*.init(BeanStore)` methods zero-out builder defaults via `.orElse(null)` when optional beans aren't yet registered. All four memoizers reverted to legacy `BeanCreator.of(...)`. Cascade work + `BeanInstantiator` improvements (loose-builder fallthrough, interface-builder search via `getAllParents()`, debug log) preserved. Follow-up work tracked under **TODO-25** (`todo/TODO-25-revisit-rest-context-memoizer-migration.md`).
- ~~Phase 3 batch-3: drop vestigial `(BasicBeanStore)` casts at framework-defaults call sites~~ — **DONE (2026-05-08).** Sixteen `(BasicBeanStore) bs` casts in `RestContext.java` (lines 494, 515, 584, 676, 753, 768, 914, 937, 957, 987, 1008, 1029, 1064, 1110, 1125) and one in `RestOpContext.java` (line 437) dropped. These were vestigial leftovers from before the utility-class `create(...)` factories were widened to `WritableBeanStore` in the cascade-builder work. The cast was no longer required — utility methods accept `WritableBeanStore` directly, `BeanCreator.of(Class, BeanStore)` accepts any `BeanStore` parent. Migrated 5 transient internal-bean-store sites in `RestOpContext.java` (lines 216, 546, 664, 680) and `RestContext.java:2670` from `BasicBeanStore.of((BasicBeanStore) beanStore())` to `new BasicBeanStore2(beanStore())`. Did NOT migrate `RestContext.java:1199` (`bs = BasicBeanStore.create().overridingParent(...)`) because the resulting bean store is exposed via `RestContext.getBeanStore()` and downstream consumers (`RestSession.java:240`, `VarResolver.java:260`) still downcast that result to legacy `BasicBeanStore` — those sites need either field widening or v2 `BasicBeanStore.of(...)` ports first (Phase 4).

**Current remaining footprint (as of 2026-05-08, post-batch-3):**

| Location | Legacy symbol | Nature |
|---|---|---|
| `RestContext.java` | `BasicBeanStore`, `BeanCreator` | 6 `BeanCreator.of(...)` call sites (4 memoizers + user-child-resource path + `findRestOperationArgs`) — **all cast-free** thanks to `BeanCreator.of(Class, BeanStore)`; staying on legacy `BeanCreator` until **TODO-25** lands. Plus 2 direct legacy Builder-API calls (`BasicBeanStore.create().overridingParent((BasicBeanStore) X).build()` lines 348, 1206) using the legacy static API. |
| `RestOpContext.java` | `BasicBeanStore`, `BeanCreator` | 1 `BasicBeanStore.of((BasicBeanStore) context.getBootstrapBeanStore())` (line 1097) — registers the result as `BasicBeanStore.class` for user-facing `@RestPostCall`/`@RestStartCall` hook param resolution; cannot migrate without breaking that contract. + `BeanCreator.of(HttpPartSerializer.class).type(c)` for `partSerializer` (no cast — uses no-arg overload). |
| `RestSession.java` | `cp.BasicBeanStore` | 1 `BasicBeanStore.of((BasicBeanStore) context.getBeanStore())` site (line 240); requires widening private `beanStore` field + public `getBeanStore()` return type to migrate. |
| `VarResolver.java` | `cp.BasicBeanStore` | 1 `BasicBeanStore.of((BasicBeanStore) builder.beanStore())` site (line 260); requires widening package-private field + public `createSession(BasicBeanStore)` method. |
| ~~`BeanBuilder<T>` cascade builders~~ | ~~`BasicBeanStore` cast in `super(...)`~~ | **RESOLVED (2026-05-08).** All 14 cascade-builder casts dropped after widening `BeanBuilder<T>` to accept `WritableBeanStore`. |
| ~~`RestInject.java`, `RestInit.java`~~ | ~~`cp.BasicBeanStore`~~ | **RESOLVED (2026-05-08, batch 2).** Javadoc-only refs flipped to v2 `BeanStore`. |
| ~~`McpPage.java`, `McpTypedHandlers.java`, `McpEndpoint.java`, `McpRestServlet.java`~~ | ~~`cp.BasicBeanStore`~~ | **RESOLVED (2026-05-08, batch 2).** rest-server-mcp module fully migrated to v2 `BeanStore`. |
| ~~`Name.java`, `Named.java`~~ | ~~`cp.BasicBeanStore`~~ | **RESOLVED (2026-05-08, batch 2).** Javadoc / annotation `@see` only — flipped to v2 `BeanStore`. |
| `HttpPartParser.java`, `HttpPartSerializer.java` | `ContextBeanCreator` | `Creator` inner class extends `ContextBeanCreator<...>` — gated by `ContextBeanCreator` migration to v2 (Phase 4). |
| `BeanStore_Test.java` | `BeanCreateMethodFinder` | 2 test references — guards legacy class behavior; stays until removal. |

### Residual casts in `RestContext` / `RestOpContext` — root causes

1. ~~**Legacy utility-class signatures (largest source).**~~ — **RESOLVED (2026-05-08).** All ~16 utility classes have been flipped to `create(WritableBeanStore)`. The downcast moved into each `Builder` constructor where it's a transitional implementation detail.

2. ~~**`BeanCreator.of(Class, BasicBeanStore)` casts at call sites.**~~ — **RESOLVED (2026-05-08).** Added `BeanCreator.of(Class, BeanStore)` overload accepting any `BeanStore` (legacy or v2). Six casts dropped from `RestContext` (4 memoizers + user-child-resource + 1 in `DebugEnablement.Builder`).

3. **`BeanInstantiator` four-memoizer migration blocker.** Migrating `callLogger` / `debugEnablement` / `staticFiles` / `swaggerProvider` to `BeanInstantiator` broke documented `@Rest(callLogger=…)` / `@Rest(debugEnablement=…)` / `@Rest(swaggerProvider=…)` annotation overrides plus static-file serving (loose-builder fallthrough still picks empty-default builders for `BasicStaticFiles` etc.; `Basic*.init(BeanStore)` zero-outs builder defaults via `.orElse(null)` when optional beans aren't yet registered). Reverted on 2026-05-08; tracked under **TODO-25** (`todo/TODO-25-revisit-rest-context-memoizer-migration.md`).

4. **Direct `BasicBeanStore.of(...)` / `BasicBeanStore.create().overridingParent(...)` calls (partially resolved 2026-05-08).** Five transient internal sites in `RestOpContext` (4 lambdas) and `RestContext.findRestOperationArgs` migrated to `new BasicBeanStore2(beanStore())`. Four sites remain blocked: `RestContext.java:348` (Builder fluent chain with `.type(...)`/`.impl(...)` — needs equivalent v2 fluent API), `RestContext.java:1206` (per-resource bean store re-wrap exposed via public `RestContext.getBeanStore()` — downstream consumers cast back to legacy), `RestOpContext.java:1097` (registers result under `BasicBeanStore.class` for `@RestPostCall` hook param resolution — user-facing breaking change), `RestSession.java:240` + `VarResolver.java:260` (need field/return-type widening to `WritableBeanStore`/`BeanStore`). Final cleanup deferred until either (a) field/return-type widenings land OR (b) Phase 4 cutover when the v2 absorbs the legacy statics.

5. ~~**`BeanBuilder<T>` parent-ctor signature.**~~ — **RESOLVED (2026-05-08).** `BeanBuilder<T>` field + ctor + public `beanStore()` accessor all flipped to `WritableBeanStore`. All 14 cascade-builder casts dropped.

6. ~~**Vestigial `(BasicBeanStore) bs` casts at framework-defaults call sites.**~~ — **RESOLVED (2026-05-08, batch 3).** Sixteen casts in `RestContext.java` and one in `RestOpContext.java` dropped — utility-class `create(WritableBeanStore)` factories already accept the supertype, casts were leftover from before the cascade-builder migration.

### v2 API additions made in this PR (already landed)

Promoted to the v2 surface so the `RestContext` / `RestOpContext` migration could land without forcing legacy casts inside those files:

- `BeanStore.getBeanType(Class)` — default returns empty.
- `BeanStore.hasAllParams(ExecutableInfo, Object)` / `getParams(...)` / `getMissingParams(...)` — default impls reading via `getBean` / `hasBean`.
- `WritableBeanStore.addBeanType(Class, Class<? extends T>)`.
- `WritableBeanStore.hasDefaultSupplier(Class)` / `(Class, String)` (added in earlier session).
- `BasicBeanStore2` — concrete `addBeanType` / `getBeanType` with parent-chain traversal.
- `MethodInvoker.invoke(BeanStore, Object)` (was `invoke(BasicBeanStore, Object)`).

Legacy `cp.BasicBeanStore` was made to `implements WritableBeanStore` as a transitional bridge so the field-type swap could land without rewriting every cascade consumer in lockstep.

**Subtype-binding semantics — lesson learned.** The plan originally suggested replacing mock-test `bs.addBeanType(X, Y)` with `bs.addSupplier(X, () -> BeanInstantiator.of(Y, bs).run())`. That broke `@Rest(callLogger=…)` overrides because the supplier-shim eagerly built the mock subclass and shadowed the annotation chain. The actual fix kept `addBeanType` (now on `WritableBeanStore`) and its "default subtype that annotations can override" semantics. The plan's earlier text still under "Subtype-binding rewrite" is preserved below for context but should be considered superseded.

---

## Phase 0 — Interim: deprecate the legacy classes

Low-risk, independent of the rest of this plan. Can land in an earlier release than 9.5 if desired.

- [x] **DONE (2026-05-08).** Annotated with `@Deprecated(since = "9.5.0")` and `@deprecated` Javadoc pointing at `org.apache.juneau.commons.inject`:
  - `org.apache.juneau.cp.BasicBeanStore` → `BeanStore` / `WritableBeanStore` / `BasicBeanStore2`
  - `org.apache.juneau.cp.BeanCreator` → `BeanInstantiator`
  - `org.apache.juneau.cp.BeanCreateMethodFinder` → `BeanStore.createBeanFromMethod(...)`
  - `org.apache.juneau.cp.ContextBeanCreator` → migrate to `BeanInstantiator` once v2 grows context-builder hooks (still extended by `HttpPartParser.Creator` / `HttpPartSerializer.Creator`)
- [x] **`forRemoval = true` deliberately not set** — remaining internal consumers still wired to these. Deletion happens in Phase 4.
- ~~`org.apache.juneau.BeanBuilder` (consumer class)~~ — **NOT deprecated.** Lives outside `cp` package; it's the user-facing builder superclass. Internals widened to `WritableBeanStore` (v2 interface) on 2026-05-08, so it stays as a permanent public API.

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
- ~~**`BeanCreateMethodFinder` → v2 equivalent**~~ — **DONE.** Replaced by `BeanStore.createBeanFromMethod(Class<T>, Object, Predicate<MethodInfo>, Object...)`. The legacy DSL pattern:
  ```java
  new BeanCreateMethodFinder<>(Type.class, resource, beanStore)
      .addBean(...)
      .find(RestContext::isRestInjectMethod)
      .run(result -> ...);
  ```
  migrates to:
  ```java
  beanStore.createBeanFromMethod(Type.class, resource,
      RestContext::isRestInjectMethod, extraBean)
      .ifPresent(result -> ...);
  ```
  Also introduced `BeanCreationException` (unchecked) and renamed `BeanCreator2` → `BeanInstantiator` to disambiguate the two APIs.
- ~~**Executable-resolution helpers**~~ — **DONE (2026-05-07).** `hasAllParams`, `getParams`, `getMissingParams` are now default methods on the `BeanStore` interface (deriving from `getBean` / `hasBean`). `MethodInvoker` now takes the interface, not the legacy concrete type.
- ~~**Type-binding helpers**~~ — **DONE (2026-05-07).** `getBeanType(Class)` on `BeanStore`, `addBeanType(Class, Class<? extends T>)` on `WritableBeanStore`, concrete impl on `BasicBeanStore2` with parent-chain traversal.
- ~~**`BeanInstantiator` builder-return-type strictness — relaxation phase 1**~~ — **DONE (2026-05-07).** `BeanInstantiator.findBeanImpl` now operates in "loose-builder" mode: when an auto-detected `Builder.build()` declares a parent return type, the runtime instance is examined and accepted only if it's assignment-compatible with `beanSubType`. If not, the call falls through to factory-method / constructor resolution on `beanSubType` instead of throwing. This fixes the legitimate "subclass adds its own constructor" pattern (e.g. `D28_ChildBeanForBuilderMethod(Builder builder)` — `BeanInstantiator_Test#d28` was rewritten to assert the new fallthrough behavior).
- ~~**`BeanInstantiator` improvements (2026-05-08)**~~ — extended loose-builder fallthrough so a `Builder.build()` returning a parent-type runtime instance triggers factory-method / constructor resolution on the requested `beanSubType` (rather than accepting the parent-type instance silently). Builder discovery (`findBuilderType`) now walks both superclasses and implemented interfaces via `getAllParents()` so interface-level builders (e.g. `SwaggerProvider.Builder`, `StaticFiles.Builder`) are found.
- [ ] **`BeanInstantiator` four-memoizer blocker — phase 2: `init(BeanStore)` semantics + builder-default propagation.** Tracked under **TODO-25** (`todo/TODO-25-revisit-rest-context-memoizer-migration.md`). The migration of `callLogger` / `debugEnablement` / `staticFiles` / `swaggerProvider` to `BeanInstantiator` was attempted on 2026-05-08 and reverted — see TODO-25 for failure modes (annotation-override path returns null; `BasicStaticFiles.Builder` defaults aren't applied) and viable approaches (tighten `Basic*.init(...)` defenses vs rework `RestContext` init order).

### Still to survey

- [ ] **`BasicBeanStore.Entry<T>`** (public/extendable entry record, exposed via protected `createEntry(...)`) — confirm no external subclassers before deleting.
- [ ] Diff `cp.BeanCreator` vs `commons.inject.BeanInstantiator` — note any legacy-only methods / behaviors that need to be ported.
- [ ] Diff `cp.BeanCreateMethodFinder` and `cp.ContextBeanCreator` against `BeanInstantiator` — confirm their functionality is subsumed; list any callers that need rewriting rather than a rename.
- [ ] Verify SVL's needs (`VarResolver(.Builder)` / `VarResolverSession`): constructor-based `Var` instantiation, optional session bean lookup, bean-store copy semantics. Cover any gaps here before TODO-14 starts.
- [ ] Document any residual deltas (missing methods, renamed methods, behavior differences) in this file before proceeding to Phase 2.

## Phase 2 — Consumer inventory

**Much of Phase 2 is already resolved.** The REST server `BasicBeanStore` migration (TODO-1) has landed, leaving only the items in the table above.

Remaining inventory work:
- ~~Confirm the `BeanCreateMethodFinder` callers in `RestContext` (33) and `RestOpContext` (16) are all mechanical once the v2 finder API is settled.~~ — **DONE.** All 49 sites migrated.
- [x] **DONE (2026-05-08).** `rest-server-mcp` migrated to v2 `BeanStore`. Public handler interfaces (`McpToolHandler`, `McpPromptHandler`, `McpResourceHandler`, `McpTypedToolHandler`, `McpTypedPromptHandler`, `McpCursor`) now take `BeanStore ctx` instead of `BasicBeanStore ctx` (chosen over `WritableBeanStore` because handlers don't mutate). `Mcp.handle(...)`, `McpDispatcher.dispatch(...)` and 7 internal dispatcher methods, plus internal `McpTypedHandlers.adaptTool`/`adaptPrompt` flipped. Servlets (`McpRestServlet`, `McpEndpoint`) replaced legacy `BasicBeanStore.of((BasicBeanStore) restReq.getContext().getBeanStore())` with `new BasicBeanStore2(restReq.getContext().getBeanStore())` — the previous `// TODO: Why do we need a cast?` comments are gone. Six test files updated in lockstep (`McpDispatcher_Test`, `McpTypedHandlers_Test`, `McpRestServlet_Test`, `McpServerConfig_Test`, `McpHandlerDefaults_Test`, `McpCursor_Test`); `BasicBeanStore.create().build()` test ctx instances → `new BasicBeanStore2()`. Added a no-arg `BasicBeanStore2()` constructor (equivalent to `new BasicBeanStore2(null)`) for standalone parent-less stores. Full build + tests + jetty-ftest green. Breaking change: any external consumer implementing `McpToolHandler` / `McpPromptHandler` / `McpResourceHandler` / typed variants / `McpCursor` must change `BasicBeanStore ctx` → `BeanStore ctx`.
- [x] **DONE (2026-05-08).** Enumerate direct callers in `juneau-microservice-*` and `juneau-config`. **Result: zero.** Neither `juneau-microservice-core` (28 source files), `juneau-microservice-jetty`, nor `juneau-core/juneau-config` (24 source files) reference `BasicBeanStore`, `BeanCreator`, `BeanCreateMethodFinder`, or `ContextBeanCreator`. Both modules are already free of legacy injection types — no Phase-3 migration work needed there.
- [ ] List any public API surfaces in `juneau-marshall` that still expose `BasicBeanStore` / `BeanCreator` (these are the hard breaking changes). `Name.java` / `Named.java` Javadoc refs are trivial.
- ~~**Cascade-builders inventory.**~~ — **DONE (2026-05-08).** 16 of the listed 25 factories flipped (those actually called from `RestContext` / `RestOpContext`): `RestConverterList`, `RestGuardList`, `RestMatcherList`, `RestOpArgList`, `ResponseProcessorList`, `RestOperations`, `RestChildren`, `MethodExecStore`, `ThrownStore`, `FileFinder`, `StaticFiles`, `BasicStaticFiles.create(...)`, `DebugEnablement`, `SwaggerProvider`, `EncoderSet`, `ParserSet`, `SerializerSet`. The remaining ~9 (`BasicCallLogger`, `BasicDebugEnablement`, `BasicSwaggerProvider`, `Messages`, `JsonSchemaGenerator.Builder`, `HttpPartParser.Creator`, `HttpPartSerializer.Creator`, `VarResolver.Builder`, `UrlPathMatcherList`, `NamedAttributeMap`) are not on the `RestContext`/`RestOpContext` cascade path; flip them lazily as Phase 4 / TODO-14 needs them, or as part of the legacy-`BeanCreator` retirement.

## Phase 3 — Migrate consumers to `*2`

- ~~Settle and implement the `BeanCreateMethodFinder` → v2 replacement API~~ — **DONE.** `BeanStore.createBeanFromMethod()` is live.
- ~~Migrate `RestContext` and `RestOpContext` (49 `BeanCreateMethodFinder` call sites)~~ — **DONE (2026-05-07).** Field types flipped, all call sites rewritten, mock-test clients updated, full build green.
- ~~**Cascade-builders signature flip.**~~ — **DONE (2026-05-08).** Sixteen utility-class factories now accept `WritableBeanStore`: list types (`RestConverterList` / `RestGuardList` / `RestMatcherList` / `RestOpArgList` / `ResponseProcessorList`), rest-server misc (`RestOperations` / `RestChildren` / `MethodExecStore` / `ThrownStore` / `FileFinder` / `StaticFiles` / `BasicStaticFiles.create(...)` / `DebugEnablement` / `SwaggerProvider`), and marshall builders (`EncoderSet` / `ParserSet` / `SerializerSet`). All cascade-induced casts removed from `RestContext` / `RestOpContext`. `BeanBuilder<T>` parent constructor still takes legacy `BasicBeanStore`; the downcast lives inside each `Builder` constructor as a transitional bridge.
- [ ] **Retire remaining `BeanCreator.of(...)` memoizers.** — punted to **TODO-25**. Six holdouts in `RestContext` (`callLogger`, `debugEnablement`, `staticFiles`, `swaggerProvider`, the user-child-resource `BeanCreator.of(rc2, ...)` path, and `findRestOperationArgs`) plus `RestOpContext.createPartSerializer`. All six call sites already cast-free (via the `BeanCreator.of(Class, BeanStore)` overload landed 2026-05-08). Migration to `BeanInstantiator` blocked on `Basic*.init(BeanStore)` zero-out semantics + builder-default propagation — see TODO-25 plan.
- [ ] Migrate `McpPage` / `McpTypedHandlers` / `McpEndpoint` / `McpRestServlet` in `rest-server-mcp`.
- [ ] Migrate remaining `juneau-microservice-*` / `juneau-config` consumers (if any).
- [ ] Update Javadoc-only references in `RestInject.java`, `RestInit.java`, `Name.java`, `Named.java`, `HttpPartParser.java`, `HttpPartSerializer.java`.
- [ ] Migrate each public API surface in `juneau-marshall`. Since 9.5 allows simple breaking changes, replace rather than overload — document each signature change in the 9.5 release notes.

## Phase 4 — Cutover rename

Once nothing still references the legacy classes:

- [ ] Delete `org.apache.juneau.cp.BasicBeanStore`, `BeanCreator`, `BeanBuilder`, `BeanCreateMethodFinder`, `ContextBeanCreator`.
- [ ] Rename the commons classes in place (drop the `2` suffix):
  - `BasicBeanStore2` → `BasicBeanStore`
  - `BeanInstantiator` — **no rename needed** (already has its final name; was `BeanCreator2`)
  - (update any other `*2`-suffixed types and their references)
- [ ] Repo-wide find/replace of remaining `BasicBeanStore2` / `BeanInstantiator` references.
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

2. ~~**`BeanCreateMethodFinder` replacement.**~~ **RESOLVED.** `BeanStore.createBeanFromMethod(Class<T>, Object, Predicate<MethodInfo>, Object...)` is the v2 equivalent. `BeanInstantiator` handles self-instantiation; `createBeanFromMethod` handles external factory-method discovery.

3. **`ContextBeanCreator` replacement.** Thin wrapper used during `Context.Builder` initialization. Is it subsumed by `BeanInstantiator`, or does it need to be ported (possibly renamed)?

4. **`BasicBeanStore.Void` sentinel.** Pick one of the two paths listed in Phase 1 (port `Void` vs. redesign `@Rest.beanStore()` default). Affects `@Rest` annotation processing code in rest-server.

5. **`INSTANCE` constant placement.** On `BasicBeanStore2` (concrete class) or on `BeanStore` (interface, as a `static final`)? The latter is cleaner; confirm no serialization/reflection code depends on the concrete type.

6. **Executable-resolution helper migration.** Are the three legacy methods (`getMissingParams`, `getParams`, `hasAllParams`) exposed anywhere outside `juneau-rest-server`'s internals? If so, migrating callers (rather than keeping compat methods) may push past the 9.5 "simple breaking changes" budget — verify the blast radius before committing to delete.

7. **Phase 0 deprecations — ship separately?** The `@Deprecated` annotations in Phase 0 could land in a 9.4.x patch to give downstream consumers a heads-up before the 9.5 break. Decide whether to ship Phase 0 early or roll it into the same 9.5 PR train.
