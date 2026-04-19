# FINISHED-16c: Phase 3 — pre-flight kill-list + Builder deletion (Phases A, B, C-1, C-2, C-3 Route B)

> **Archived from `TODO-16-restcontext-memoized-fields.md`.** Captures the pre-flight kill-list landings (Decisions #24–#26) and the additive `RestContextInit`-based Builder-deletion phases that landed on **2026-04-19**: Phase A (additive entry points), Phase B (simple-callsite migration), Phase C-1 (pre-build bean-store configurer hook), Phase C-2 (drop the resource-ctor-takes-Builder protocol + delete `RestContext.create(...)`), and **Phase C-3 Route B** (drop the per-op `@RestInit(RestOpContext.Builder)` injection protocol + delete `RestOpContext.create(...)`). The remaining work — outright deletion of `RestContext.Builder` and `RestOpContext.Builder` themselves, plus `RestAnnotation` / `RestOpContextApply` / scanner cleanup — stays in `TODO-16`. Per-decision rationale lives in TODO-16's "Resolved decisions" reference section (#23–#27).

---

## Pre-flight kill-list

Prerequisite cleanups landed before the additive `RestContextInit` ctors so the bootstrap-state count Phase 3 had to thread through ctor signatures was minimized to what Decision #23 actually settled on.

### Decision #25 — delete `defaultClasses` (landed 2026-04-19, path #1)

First attempt (rolled back) tried to register `BasicTestCallLogger` *instances* in the bean store, which broke because `BasicTestCallLogger`'s ctor calls `init(beanStore)` and reads `Logger` / `ThrownStore` beans **immediately** — those beans aren't added until `RestContext.<init>` runs (`bs.addBean(Logger.class, lg)` etc.), so eager instantiation produced a `CallLogger` with `getLogger() == null` and the first request NPEd. The `defaultClasses().get(CallLogger.class).ifPresent(creator::type)` call was precisely **type registration with deferred construction** — `BeanCreator<CallLogger>.type(...)` doesn't instantiate; `new BasicTestCallLogger(beanStore)` happens later inside `findCallLogger()`'s `creator.orElse(null)`, by which time `Logger` / `ThrownStore` are populated. Same applies to `StaticFiles`, `DebugEnablement`, `SwaggerProvider`, and the three `RestOpContext`-side overrides.

**Resolution (path #1):** added `addBeanType(Class<T>, Class<? extends T>)` / `getBeanType(Class<T>)` to `BasicBeanStore` (with parent-chain traversal in `getBeanType`) — gives the deferred-construction semantics in a more general-purpose location. All 12 internal `defaultClasses().get(X.class)` / `defaultClasses.get(X.class)` sites in `RestContext` (5 builder-side + 6 RestContext-side) and `RestOpContext` (3 builder-side) now consult `beanStore.getBeanType(X.class)` instead. `MockRestClient` / `NgMockRestClient` post-`init()` register the binding via `rcBuilder.beanStore().addBeanType(CallLogger.class, BasicTestCallLogger.class)`. `Swagger_Test.getSwaggerWithFile` registers `StaticFiles → TestClasspathFileFinder` the same way. `RestContext.Builder.defaultClasses()` accessor + `defaultClasses(Class<?>...)` setter, the `RestContext.defaultClasses` field, the `parentContext.defaultClasses.copy()` line in the builder ctor, `RestOpContext.Builder.defaultClasses()` accessor, and the `DefaultClassList` / `DefaultClassList_Test` files all deleted. Full build + test clean.

### Decision #26 — delete `defaultSettings` + `DefaultSettingsMap` (landed 2026-04-19)

Added `String debugDefault() default "";` to `@Rest` plus the matching `RestAnnotation.Builder.debugDefault(...)` setter, `Impl.debugDefault` field + ctor copy + getter, and `apply()` bridge. New `findDebugEnablement()` reads the `debugDefault` annotation property via `mergeReplacedStringAttribute(PROPERTY_debugDefault, env(...))`, converts to `Enablement` via `Enablement.fromString(...)`, and publishes the resolved value as an `Enablement` bean (`beanStore.addBean(Enablement.class, e)`). `BasicDebugEnablement.init()` now resolves its default from `beanStore.getBean(Enablement.class).orElse(NEVER)` instead of from the deleted `DefaultSettingsMap`.

**Mock-client migration:** instead of an annotation overlay (the resource class isn't ours to modify), `MockRestClient.preInit` and `NgMockRestClient` register the `Enablement.CONDITIONAL` bean directly via `rcBuilder.beanStore(Enablement.class, CONDITIONAL)` **after** `init()` so the bean store exists. Deleted `Builder.debugDefault(Enablement)`, `Builder.defaultSettings()` accessor, the `RestContext.defaultSettings` field + bean-store registration, the `DefaultSettingsMap` class entirely, and the `PROP_simpleVarResolver` / unused-property-name constants. Full build + test clean.

### Decision #24 — delete `childrenClass` / `opContextClass` + `@Rest(restChildrenClass)` (landed 2026-04-19)

Dropped `Builder.childrenClass`, `Builder.opContextClass` fields, their `restChildrenClass(Class)` / `restOpContextClass(Class)` setters, and the cast sites — `createRestChildren` / `RestOperations.Builder` now hard-code `RestChildren.class` / `RestOpContext.class`. Deleted the `@Rest(restChildrenClass=...)` annotation attribute, the `RestAnnotation.Builder.restChildrenClass(...)` setter, the `RestAnnotation.Impl.restChildrenClass` field + ctor copy + getter, the `apply()` line that bridged annotation→builder, the `restChildrenClass=RestChildren.class` line in `DefaultConfig`, and the now-unreferenced `RestChildren.Void` sentinel. Updated `RestAnnotation_Test` (a1/a2 builders, `assertBean` strings, D1/D2 annotation literals). Full build + test clean (49633 tests pass).

---

## Builder deletion — `RestContextInit` record + 2-arg `RestOpContext` ctor (Decision #23)

### Phase A — Additive entry points (landed 2026-04-19)

- **`RestContextInit` record introduced** at `org.apache.juneau.rest.RestContextInit`. Six fields per Decision #23: `Class<?> resourceClass`, `RestContext parentContext`, `ServletConfig servletConfig`, `Supplier<?> resource`, `String path`, `List<Object> children`. Compact canonical ctor null-coalesces `path` to `""` and `children` to `emptyList()`, and asserts `resourceClass` / `resource` are non-null. Convenience 2-arg ctor `RestContextInit(Class<?>, Supplier<?>)` for the common top-level case.
- **Naming note:** Decision #27 originally settled on the `XArgs` suffix, but `RestContextArgs` is already taken by the `org.apache.juneau.rest.arg.RestContextArgs` `@RestOp`-method parameter resolver (one of an entire family — `RestSessionArgs`, `RestOpContextArgs`, `RestRequestArgs`, etc., all wired through `DefaultConfig.restOpArgs={...}`). Renaming the long-standing public-API arg-resolver was rejected; the new record uses `Init` instead — short, unambiguous, and reads naturally as a parallel to the legacy `Builder.init(Supplier)` method this record consolidates. See Decision #27 for the updated guidance.
- **New `public RestContext(RestContextInit init) throws Exception` ctor** added. Internally delegates to the existing builder via a private `toBuilder(RestContextInit)` helper that runs `new Builder(...).init(supplier).path(p).children(c).build()` — purely additive, zero behavior change. Coexists with `RestContext.create(...)`.
- **New `public RestOpContext(java.lang.reflect.Method, RestContext) throws ServletException` ctor** added. Delegates to existing `protected RestOpContext(Builder)` via `new Builder(method, context)`. Coexists with `RestOpContext.create(...)`.

### Phase B — Migrate simple callers (landed 2026-04-19)

Migrated 8 simple callsites (those without intermediate Builder mutation between `init()` and `build()`):

- `juneau-utest/.../rest/Swagger_Test.java#getSwagger`
- `juneau-utest/.../rest/NoInherit_Test.java` (5 sites: `restContext` helper + 4 `a06`–`a09` test bodies)
- `juneau-utest/.../rest/RestOpContext_OpLevelOverrides_Test.java`
- `juneau-utest/.../rest/RestOpContext_HttpMethodResolution_Test.java`
- `juneau-utest/.../rest/annotation/Rest_BeanCreatorOverrides_Test.java`
- `juneau-utest/.../TestUtils.java#getSwagger(Class)` (also migrates `RestOpContext.create(...).build()` → `new RestOpContext(method, ctx)`)
- `juneau-rest-server/.../rest/servlet/RestServlet.java#init(ServletConfig)`

Full build + tests pass clean after Phase B (49633+ tests, 32s build / 44s test).

**Skipped — deferred to Phase C-2 / C-3 (Builder cannot be removed yet):**

- `RestContext.Builder.createRestChildren` 3 internal sites (`RestChild` instance / `Class<?>` / instance branches). Site 2 passes the `RestContext.Builder` to `BeanCreator.of(oc, beanStore).builder(RestContext.Builder.class, cb).run()` to inject the builder into the child resource's constructor — handled by Phase C-2 below.

### Phase C-1 — Pre-build bean-store configurer hook (landed 2026-04-19)

- Added `Consumer<BasicBeanStore> beanStoreConfigurer` as 7th field of `RestContextInit` (canonical ctor; null-coalesces to a no-op in the compact ctor). Added backward-compat 6-arg ctor overload that defaults the configurer to `null`, plus a 3-arg convenience overload `RestContextInit(Class<?>, Supplier<?>, Consumer<BasicBeanStore>)` for the common test/mock case.
- `RestContext.toBuilder(RestContextInit)` invokes the configurer after `init()` but before any `findXxx()` memoizer can fire — preserves the existing semantic of `rcBuilder.beanStore(...)` mutations between `.init()` and `.build()`.
- Migrated 3 callsites that were skipped in Phase B:
  - `juneau-rest-mock/.../rest/mock/MockRestClient.java#preInit` (was lines 1890–1899)
  - `juneau-rest-mock/.../ng/rest/mock/NgMockRestClient.java#build()` (was lines 222–231)
  - `juneau-utest/.../rest/Swagger_Test.java#getSwaggerWithFile`

  Each now uses `new RestContext(new RestContextInit(o.getClass(), () -> o, bs -> { bs.addBean(...); bs.addBeanType(...); })).postInit().postInitChildFirst()`.
- Full build + tests pass clean after Phase C-1 (49633+ tests, 53s build / 66s test).

### Phase C-2 — Drop the resource-ctor-takes-Builder injection protocol + delete `RestContext.create(...)` (landed 2026-04-19)

- **Resolution chosen (option 2): drop the pattern entirely in 9.5.** The legacy `public MyResource(RestContext.Builder builder)` constructor pattern is removed. Resource classes now declare configuration declaratively via `@Rest(...)` annotation attributes and `@RestInject` members, or pass values through `RestContextInit` when constructing the context directly. No replacement protocol — the use case is fully covered by the existing annotation + `@RestInject` surfaces.
- Migrated 3 internal `createRestChildren` sites in `RestContext.Builder.createRestChildren`: all three branches (`RestChild` / `Class<?>` / instance) now use `new RestContext(new RestContextInit(rc2, restContext, inner, so, path2 == null ? "" : path2, java.util.List.of()))` instead of the old `RestContext.create(...).init(...).path(...).build()` chain. Site 2's `BeanCreator.of(oc, beanStore).builder(RestContext.Builder.class, cb).run()` simplified to `BeanCreator.of(oc, beanStore).run()` — child resources no longer get the builder injected.
- Deleted `public static Builder RestContext.create(Class<?>, RestContext, ServletConfig)` static factory — no remaining callers anywhere in the codebase. The Javadoc on `new RestContext(RestContextInit)` updated to reference the deletion.
- Updated `RestChild.java` Javadoc to remove the old `MyResource(RestContext.Builder builder)` example and document the `@Rest(children=...)` / `RestContextInit.children(...)` replacement patterns. Added a 9.5-removal note for the legacy constructor.
- Added migration guide rows to `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md` covering: (a) the `RestContext.create(...)` chain → `new RestContext(RestContextInit)` migration with the configurer-hook story, and (b) the resource-ctor-takes-Builder protocol removal.
- Full build + tests pass clean after Phase C-2 (49633+ tests, 32s build / 39s test, full run 123s).

### Phase C-3 — Route B: drop the per-op `@RestInit(RestOpContext.Builder)` protocol + delete `RestOpContext.create(...)` (landed 2026-04-19)

**Context.** Coming into this phase, two `RestOpContext.create(mi.inner(), restContext)` callers remained — both inside `RestContext.Builder.createRestOperations`:

- **Site 1** (the standard per-op path, `RestContext.java:2818`): `RestOpContext.Builder rocb = RestOpContext.create(mi.inner(), restContext).beanStore(beanStore).type(RestOpContext.class);` — followed by **builder injection into `@RestInit` methods that take a `RestOpContext.Builder` parameter** (the per-op init protocol), then `rocb.build()`. The injection step required the Builder to be reachable as a bean during init, which is the user-visible surface the design discussion was actually about.
- **Site 2** (the RRPC special case, `RestContext.java:2839`): `RestOpContext.create(mi.inner(), restContext).beanStore(restContext.getRootBeanStore()).type(RrpcRestOpContext.class).build()` — pure fluent build, no `@RestInit` injection. Mechanically simple, but waiting for Site 1's architectural decision so Phase C-3 lands as one cohesive change.

Three routes were on the table — Route A (rename Builder to a single-purpose init record, keep injection), Route B (delete the per-op `@RestInit(RestOpContext.Builder)` protocol outright, mirroring Decision #24's "no real-world callers" precedent), Route C (keep Builder alive but make it package-private, expose `new RestOpContext(method, ctx)` as the public entry).

**Decision: Route B.** A codebase-wide audit confirmed **zero real-world `@RestInit` methods declare a `RestOpContext.Builder` parameter**. The only `RestOpContext.Builder`-parameter usages anywhere in the source tree are:

- 8 internal `*Annotation.apply(AnnotationInfo<X>, RestOpContext.Builder)` methods (annotation-side bridge code, not user-facing).
- The protected `RrpcRestOpContext(RestOpContext.Builder)` ctor (internal subclass plumbing).
- Stale Javadoc examples (one in `RestOpContext.java:1244`, one in `juneau-docs/pages/topics/10.24.RestOpContext.md:20`).

That's exactly the Decision #24 precedent — a publicly-documented hook with zero real users. Drop the protocol; surface the loud-failure signal so any straggler immediately learns at startup.

**Mechanical landing.**

1. **API surface promotions to enable subclass-side construction.** Promoted `RestOpContext.Builder(java.lang.reflect.Method, RestContext)` from package-private to **`public`**, and `RestOpContext.Builder.beanStore(BasicBeanStore)` from `protected` to **`public`**. Both promotions exist solely so internal subclasses living in sibling packages (notably `org.apache.juneau.rest.rrpc.RrpcRestOpContext`) can reach them without going through the now-deleted factory. The Builder type is still on the deletion list for a follow-up session, so these promotions are deliberately short-lived.

2. **Added `public RrpcRestOpContext(java.lang.reflect.Method, RestContext)` 2-arg ctor.** Internally builds `new RestOpContext.Builder(method, context).beanStore(context.getRootBeanStore())` and chains to the existing protected `RrpcRestOpContext(Builder)` ctor. Preserves Site 2's root-bean-store override verbatim — the `getRootBeanStore()` (vs. the resource-layered `getBeanStore()`) is intentional historical behavior for RRPC's builder-time bean creation, and we're not auditing or changing it under this TODO.

3. **Site 1 migration:** `RestOpContext.create(mi.inner(), restContext).beanStore(beanStore).type(RestOpContext.class).build()` → **`new RestOpContext(mi.inner(), restContext)`**. The `.beanStore(beanStore)` override was equivalent to the ctor default (`BasicBeanStore.of(context.getBeanStore())`) — `beanStore` here was the same store that becomes `restContext.getBeanStore()` after build; `.type(RestOpContext.class)` was the default. The entire `BasicBeanStore.of(beanStore).addBean(RestOpContext.Builder.class, rocb)` injection block + the per-op `@RestInit` invocation loop + the `initMap` scan filtered for `y.hasParameter(RestOpContext.Builder.class)` — all gone.

4. **Site 2 migration:** `RestOpContext.create(mi.inner(), restContext).beanStore(restContext.getRootBeanStore()).type(RrpcRestOpContext.class).build()` → **`new RrpcRestOpContext(mi.inner(), restContext)`**. The fluent chain's three overrides all collapse into the new 2-arg ctor.

5. **Dropped the dead `! y.hasParameter(RestOpContext.Builder.class)` exclusion in `runInitHooks`** (the class-level `@RestInit` resolver). Pre-Route-B that exclusion existed because the per-op flow handled those methods separately. Post-Route-B, any user that still declares `@RestInit public void init(RestOpContext.Builder b)` will surface a "Could not find prerequisites: RestOpContext.Builder" error at startup — the desired loud-failure signal, not a silent no-op.

6. **Deleted `public static RestOpContext.create(java.lang.reflect.Method, RestContext)` factory** entirely. Both callers were Site 1 / Site 2; both migrated. Kept a one-block back-pointer comment in `RestOpContext.java` for git-archeology.

7. **Documentation sweep.**
   - `RestInit.java` Javadoc — removed the inaccurate "only valid parameter type is RestContext.Builder" wording (the bean store has always resolved arbitrary types — `ServletConfig`, zero-arg, user-registered beans), documented the actual set, and added a `Note (9.5)` block calling out the dropped per-op variant.
   - `juneau-docs/pages/topics/10.24.RestOpContext.md` — replaced the misleading per-op `@RestInit(RestOpContext.Builder)` example with a pointer to `@RestOp(...)` attributes / `@RestInject` / class-level `@RestInit`, dropped stale `debug(Enablement)` / `defaultCharset(Charset)` / `maxInput(String)` reference rows (all removed in earlier phases), and flagged the table for the broader Phase 4 sweep.
   - `juneau-docs/pages/topics/23.01.V9.5-migration-guide.md` — added rows for: (a) the dropped per-op `@RestInit(RestOpContext.Builder)` protocol with the migration recipe (declarative `@RestOp(...)` attributes / `@RestInject(name=, methodScope=)` / class-level `@RestInit(RestContext.Builder)`), and (b) the deleted `RestOpContext.create(Method, RestContext)` factory with the new 2-arg ctor patterns.

8. **Build + tests pass clean** (`./scripts/test.py -f`); coverage on touched files (`RestContext.java`, `RestOpContext.java`, `RrpcRestOpContext.java`) verified — no new uncovered lines/branches introduced. Two pre-existing defensive-throw branches remain uncovered (the `meta.getMethodsByPath().isEmpty()` "interface defines no remote methods" guard in `RrpcRestOpContext`, and the `mi.isNotPublic()` "@RestOp method must be public" guard in `RestContext.createRestOperations`); both predate this change.

**Why Route B was the right call.** The audit verdict made this almost mechanical — the per-op `@RestInit(RestOpContext.Builder)` protocol was a Decision #24-style ghost API: documented, supported, and untouched. Routes A and C both required preserving an injection mechanism that demonstrably nobody used. Route B both deleted dead surface area *and* moved the codebase one step closer to the Builder-class deletion that's still ahead.

**What this leaves for the next session.** With Phase C-3 Route B landed, the remaining Phase 3 work is the actual deletion of `RestContext.Builder` and `RestOpContext.Builder` themselves — a multi-session refactor that needs the `findXxx()` memoizer plumbing and the `RestAnnotation` / `RestOpContextApply` / `*Annotation.apply()` flows moved off the Builder type before the Builders can be removed. The two Route B side-effect promotions (`Builder(Method, RestContext)` ctor public + `beanStore(BasicBeanStore)` setter public) become moot at that point.

### Phase C-3 pre-flight — drop `dotAll` (Decision #17, landed 2026-04-19)

- Removed `boolean dotAll` field + `dotAll()` setter on `RestOpContext.Builder`; removed `protected final boolean dotAll` field + `dotAll = builder.dotAll;` ctor copy on `RestOpContext`.
- Removed the two `if (dotAll && ! p.endsWith("/*")) p += "/*";` auto-append branches in `Builder.getPathMatchers()`. Users who previously called `builder.dotAll()` (or wrote `@RestOp` paths and relied on the flag) explicitly include `/*` in their paths now — the `UrlPathMatcher` already handles `/*` and `**` natively, matching the migration guide's "URL pattern dictates behavior" wording.
- **RRPC auto-detect convention.** `Builder.getPathMatchers()` now auto-appends `/*` when the path is auto-detected (no explicit `@RestOp(path=...)`) AND the resolved http method equals `"RRPC"` (case-insensitive). RRPC operations are intrinsically "match anything below the method's URL" by design — the convention used to be expressed via the (internal-only) `.dotAll()` call in `RestContext.Builder.createRestOperations`; it now lives at the path-detection point. Users writing `@RestOp(method="RRPC", path="/proxy/*")` continue to work unchanged.
- Removed `.dotAll()` from `RestContext.Builder.createRestOperations` RRPC site. Replaced with a one-line comment back-pointing to Decision #17.
- Removed the stale `RestOpContext.Builder.dotAll()` link from `juneau-docs/pages/topics/10.24.RestOpContext.md`.
- **Coverage.** `Remote_Test.e01_rrpcBasic` (and the rest of the `E*` RRPC test family) exercises the `httpMethod2 == "RRPC"` auto-`/*` branch through `@RestOp(method=HttpMethod.RRPC) public E1 proxy()`. The defensive `&& ! p.endsWith("/*")` guard's true-`!endsWith` branch is the only one hit (`HttpUtils.detectHttpPath` never returns paths ending in `/*`), but the guard is kept as documentation that the auto-append is non-clobbering — the unreached `endsWith` branch is intentional defensive code, not a coverage gap to test.
- **Build + tests pass clean** (49633+ tests, 31s build / ~70s test).

### Class-level `@RestInit(RestContext.Builder)` deletion (landed 2026-04-19, same day as Route B)

The class-level `@RestInit(RestContext.Builder)` injection protocol was deleted in the same chapter as Route B — the audit confirmed only the framework's own `RestInit_Test` exercised it (zero microservice / examples / mock-client / utest production code).

- Audit confirmed **zero non-test callers** of class-level `@RestInit(RestContext.Builder builder)` across `juneau-microservice-*`, `juneau-examples-*`, `juneau-rest-mock`, `juneau-utest` (the only consumer is `RestInit_Test` itself, which existed to verify the protocol). Production codebase has zero internal `beanStore.getBean(RestContext.Builder.class)` lookups apart from `BasicDebugEnablement.init()`.
- **Removed `addBean(Builder.class, this)`** from `RestContext.Builder.init(Supplier)` (line 1149) — the in-flight `RestContext.Builder` is no longer published as a bean. `runInitHooks` now sees no resolvable `RestContext.Builder` parameter, so any straggling `@RestInit(RestContext.Builder b)` method will surface a "Could not find prerequisites: RestContext.Builder" error at startup (the same loud-failure pattern Route B established for the per-op variant).
- **Migrated `BasicDebugEnablement.init(BasicBeanStore)`** off the Builder lookup. Pre-9.5 it pulled `RestContext.Builder` from the bean store solely to call `builder.isDebug()` for the default-enablement fallback. Now `RestContext.findDebugEnablement()` unconditionally pre-publishes an `Enablement` bean (resolution order: `@Rest(debugDefault=...)` → pre-registered `Enablement` bean → `builder.isDebug()` boolean fallback), and `BasicDebugEnablement.init()` simply reads `beanStore.getBean(Enablement.class).orElse(Enablement.NEVER)`.
- **Updated `RestInit_Test.java`** — replaced the three `init1c(RestContext.Builder)` cases with `init1d(MarkerBean)` (a no-op `@RestInject`-supplied bean). The test still covers the three things that actually matter for `@RestInit`: no-arg invocation, `ServletConfig` injection, and `@RestInject`-bean injection — plus the inheritance / override / ordering rules. Updated the leading comment to call out the protocol removal.
- **Updated Javadoc on `RestInit.java`** — rewrote the class-level docs to drop the misleading "most common parameter is `RestContext.Builder`" wording. Added an explicit `Note (9.5)` block listing both deleted Builder-injection protocols (per-op + class-level) with replacement recipes; updated the `PetStoreResource` example to use a no-arg `@RestInit` (the old example's `(RestContext.Builder builder)` parameter was unused — typical of the pattern's real-world usage, even before deletion).
- **Updated `runInitHooks` comment block** to mention both Builder-protocol deletions side-by-side.
- **Added a migration-guide row** (`23.01.V9.5-migration-guide.md`) for the class-level `@RestInit(RestContext.Builder)` removal — explicit replacement recipe and the loud-failure error message users will hit.
- **Doc-page sweep.** Added `:::warning Outdated examples (9.5)` admonitions to the eight topic pages still showing the broken pattern — `10.23.RestContext.md`, `10.16.08.SwaggerModels.md`, `10.13.SvlVariables.md`, `10.12.ConfigurationFiles.md`, `10.06.Marshalling.md`, `10.04.04.JavaMethodReturnTypes.md`, `10.04.03.JavaMethodParameters.md`, `10.03.06.LifecycleHooks.md`. The Lifecycle Hooks page got its example actually fixed (parameter was unused — dropped to no-arg) plus the warning. Updated `10.24.RestOpContext.md` intro paragraph to drop the now-dead "class-level `@RestInit` hooks taking `RestContext.Builder`" wording. Page-level rewrites are deferred to Phase 4's broader doc audit.
- **Build + tests pass clean** (`./scripts/test.py -f`, ~90 s); coverage on touched files: `BasicDebugEnablement` is now 100 % branch coverage (was already 100 %); `RestContext.findDebugEnablement` picks up a couple new branches in the boolean-fallback path that follow the same pre-existing `if (nn(debugDefaultStr) && !debugDefaultStr.isBlank())` partial-coverage pattern (4-branch compound condition fully exercised by `Rest_Debug_Test`, but JaCoCo's per-branch tracking flags the `&&` short-circuit as partial — same as before this change).

### Phase C-3 follow-up — Javadoc cleanup of deleted Builder-injection examples (landed 2026-04-19)

Stale Javadoc examples in `juneau-rest-server` that still showcased the deleted protocols (`MyResource(RestContext.Builder builder)` resource ctor, `@RestInit public void init(RestContext.Builder builder)` injection, and the deleted `RestContext.create(...)` / `RestOpContext.create(...)` static factories) were swept on top of the Phase C-3 landing. None of these examples were testable code — they were only embedded in `<p class='bjava'>` doc snippets — so the only "test" was confirming the build still passes after the cleanup.

- **`RestContext.java` — top-level Javadoc rewrite.** The class-level "To interact with this object, simply pass it in as a constructor argument or in an INIT hook" preamble (with two examples both showing deleted protocols) was rewritten to lead with the `@Rest` + `@RestInject` flow and explicitly call out the 9.5 removal of the Builder-injection patterns plus a pointer to the v9.5 Migration Guide.
- **`RestContext.java` Builder-method examples — 9 setter Javadocs cleaned.** Each stripped the now-incorrect "Option #2/#3 - via builder" sub-examples, leaving the annotation-driven Option #1. Setters cleaned: `children(...)`, `consumes(...)`, `defaultRequestAttributes(...)`, `defaultRequestHeaders(...)`, `defaultResponseHeaders(...)`, `path(...)`, `produces(...)`, `responseProcessors(...)`. The `path(...)` Javadoc additionally calls out `RestContextInit` as the programmatic entry for direct construction.
- **`RestOpContext.java` — `converters(...)` setter.** Same Option #2/#3 cleanup; left only the annotation-driven example.
- **`Rest.java` annotation — 3 "programmatic equivalent" snippets.** The `encoders` / `parsers` / `serializers` attribute docs each closed with `RestContext.Builder <jv>builder</jv> = RestContext.<jsm>create</jsm>(<jv>resource</jv>); <jv>builder</jv>.getXxx().add(<jv>classes</jv>);` — the static factory was deleted in Phase C-2. Replaced with a one-line "contribute an `EncoderSet` / `ParserSet` / `SerializerSet` bean via `@RestInject(name=\"encoders\"|\"parsers\"|\"serializers\")`" pointer.
- **`@RestOp` family — 17 "programmatic equivalent" snippets across `RestGet`, `RestPut`, `RestPost`, `RestDelete`, `RestPatch`, `RestOptions`, `RestOp`.** Each `encoders` / `parsers` / `serializers` attribute javadoc had the same `RestOpContext.Builder <jv>builder</jv> = RestOpContext.<jsm>create</jsm>(<jv>method</jv>,<jv>restContext</jv>);` snippet pointing at the factory deleted in Phase C-3. Replaced en masse via a script with the same `@RestInject` pointer (with a "use methodScope to scope to specific operation methods" hint for the per-op annotations). Total: 17 snippets across 7 files.
- **Build + tests pass clean** (`./scripts/test.py -f`, ~84s).
- **Verified** zero remaining `RestContext.Builder <jv>builder</jv>` / `RestOpContext.Builder <jv>builder</jv>` / `RestContext.<jsm>create</jsm>` / `RestOpContext.<jsm>create</jsm>` references inside `juneau-rest-server/src/main/java`. The remaining `RestContext.Builder` occurrences in source are the Builder class itself + its still-extant member references (legitimate, on the deletion path for a future session). The remaining `RestOpContext.Builder#xxx` `{@link}` doclinks all point at methods that still exist on the Builder (`converters()`, `guards()`, `roleGuard(String)`, `rolesDeclared(String...)`) — they'll be migrated when those Builder methods themselves are deleted.
- **Deleted two unused convenience setters on `RestContext.Builder`:** `defaultAccept(String)` and `defaultContentType(String)`. Both were thin wrappers around `defaultRequestHeaders(accept(value))` / `defaultRequestHeaders(contentType(value))`, with zero callers anywhere in the codebase — `RestContextApply.apply()` does the wrap inline (`string(a.defaultAccept()).map(HttpHeaders::accept).ifPresent(b::defaultRequestHeaders)`), tests use the *annotation* builder's same-named accessors (different class), and microservices/examples never reached for them. Build passes clean.
