# TODO-5 — Move bean-modeling runtime types (`BeanMap`, `BeanMeta`, `BeanPropertyMeta`, …) into `commons.bean` (remainder of Phase 5)

This is the remaining work from **Phase 5 of the bean-layer split**. Phase 5a (the `MarshallingInterceptor` → `commons.bean.BeanInterceptor` move+rename) shipped in the same commit that created this plan. The big runtime types still live in `juneau-marshall` because they are deeply entangled with `ClassMeta` / `MarshallingContext` / `ObjectSwap` / `BeanRegistry`, and untangling them is the bulk of the work.

---

## Status (as of Phase 5g checkpoint)

**Step 7 complete (no-op + hardening).** Per-format extension survey confirmed: `ExtendedBeanMeta` (composes `BeanMeta<?>`) lives in `juneau-marshall`; `XmlBeanMeta` and `RdfBeanMeta` extend `ExtendedBeanMeta`; no `HtmlBeanMeta` exists. All three are marshalling-side types (built only from `XmlSerializer`/`XmlParser`/`RdfSerializer`/`RdfParser`, always over a marshalling-built `BeanMeta` where `classMeta` is non-null). They stay in `juneau-marshall` unchanged.

**Pre-Step-8 hardening landed.** The unguarded `BeanPropertyMeta` and `BeanMap` paths flagged in the Step-6 risk notes are now defensive against commons-built properties/maps:
- `BeanPropertyMeta.add(BeanMap, String, Object)` and `add(BeanMap, String, String, Object)` — strategy (b): throw `UnsupportedOperationException` with a clear message ("Property '...' was built via the bean-modeling-only path; ... add operations require a marshalling context.") when `rawTypeMeta == null`. Documented in Javadoc.
- `BeanPropertyMeta.setArray(Object, List)` — strategy (b): same UOE pattern. Documented.
- `BeanPropertyMeta.applyChildPropertiesFilter(MarshallingSession, ClassMeta, Object)` — strategy (b): UOE when `bc == null`. Caller `swapAndFilterProperty` was also hardened: the `if (nn(properties))` guard now also checks `nn(rawTypeMeta)` so we never attempt the marshalling-side child-properties branching on a commons-built property. The defensive UOE inside `applyChildPropertiesFilter` itself is therefore unreachable in practice — kept as a belt-and-braces.
- `BeanPropertyMeta.setPropertyValue` (the back half of `set`) — already gated by the existing early-return `if (rawTypeMeta == null) { invokeSetter(...); return old; }` block in `set`. No change needed; documented in the Step-7 narrative.
- `BeanMap.getBean()` — strategy (c): the `Optional<X>` initialization loops (over `meta.getProperties()` and `meta.getHiddenProperties()`) now null-check `v.getClassMeta()` before reading `cm.isOptional()` / `cm.getOptionalDefault()`. Properties built via the bean-modeling-only path are skipped — their `Optional<X>` field is left at its constructor-assigned value. Documented in `BeanMap.getBean()` Javadoc.
- `BeanMap.getBean(boolean create)` — the constructor-args path (only triggered when `bean == null && create && ne(meta.getConstructorArgs())`) is unreachable in practice on the commons-side path because `BeanMap.of(T, BeanMeta<T>)` always supplies a non-null bean. Left as-is (would NPE on `session.convertToType` if a user built a commons `BeanMap` with a null bean for a `@BeanCtor` class — a pathological combination).

Test coverage: `BeanMeta_Test` grew from 10 → 14 scenarios. New tests:
- `e01_add_collection_throwsOnCommonsBuiltProperty` — verifies `add(BeanMap, String, Object)` on a commons-built Collection property raises `UnsupportedOperationException` with the expected message.
- `e02_add_map_throwsOnCommonsBuiltProperty` — same for the `add(BeanMap, String, String, Object)` Map overload.
- `f01_getBean_skipsOptionalInitOnCommonsBuiltProperty` — verifies `BeanMap.getBean()` on a commons-built bean with an `Optional<X>` property no longer NPEs and returns the wrapped bean (Optional field stays at its constructor-assigned value).
- `f02_getBean_returnsBeanForSimplePojo` — sanity check that `BeanMap.getBean()` round-trips on a simple commons-built bean.

Full test suite green (`scripts/test.py --full`).

**Step 6 complete.** `BeanMeta` and `BeanPropertyMeta` can now be constructed without a `MarshallingContext`. Public surface added:
- `BeanMeta.of(Class<T>, BeanConfigContext)` and `BeanMeta.of(Class<T>)` static factories.
- `protected BeanMeta(Class<T>, BeanConfigContext)` constructor.
- `BeanMeta.getConfig()` and `BeanMeta.getClassInfo()` — always non-null accessors.
- `BeanMap.of(T, BeanMeta<T>)` static factory for pairing a bean with a commons-built `BeanMeta` (no session).

Internal restructuring:
- Existing `BeanMeta(ClassMeta<T>, MarshalledFilter, String[], ClassInfo)` constructor preserved (still used by `ClassMeta.findBeanMeta()`/`BeanMetaFiltered`). Both constructors now delegate to a private all-args constructor that builds property metadata once.
- New `BeanMeta.config` field (non-null `BeanConfigContext`) is the source of truth for ALL settings reads — visibility, namers, ignores, `findFluentSetters`, `isUnsortedProperties`, `isBeansRequireDefaultConstructor`/`SomeProperties`, `getBeanTypePropertyName`, `getAnnotationProvider`, `getBeanStore`, `isUseJavaBeanIntrospector`, `isUseInterfaceProxies`, `isIgnoreTransientFields`. The marshalling-side path sources it from `marshallingContext.getBeanConfigContext()` (wired in Step 1); the commons-side path uses the explicit context.
- `BeanMeta.classMeta` and `BeanMeta.marshallingContext` are now documented-nullable. They stay non-null only on the marshalling-side construction path; commons-side construction leaves them null. `classInfo` and `config` are always non-null.
- `BeanMeta.findBeanRegistry()` returns `null` when `marshallingContext == null` (BeanRegistry is a marshalling-side concern). `BeanMeta.findDictionaryName()`'s parents/interfaces `marshallingContext::getClassMeta` stream is gated behind a non-null check. The synthetic `_type` property is built unconditionally (rawMetaType is a no-op when bc is null) but the side-map entry pairing it with the bean-level registry is skipped on the commons-side path.

`BeanPropertyMeta` changes:
- `BeanPropertyMeta.Builder` carries both `MarshallingContext bc` (nullable on commons-side) and `BeanConfigContext config` (always non-null, sourced from the owning `BeanMeta`).
- `BeanPropertyMeta.Builder.rawMetaType(Class<?>)` no-ops when `bc == null` — leaves `rawTypeMeta` null and the property runs in raw-reflection mode.
- `BeanPropertyMeta.Builder.validate(...)`: annotation reads route through `bc.getAnnotationProvider()` when bc is non-null, falling back to `config.getAnnotationProvider()`. All `bc.resolveClassMeta(...)` / `bc.getClassMeta(...)` / `bc.object()` calls are guarded by `nn(bc)`. Type-aware validation (isAssignableTo checks for getter/setter/field type compatibility, isDyna value-type resolution) is wrapped in `if (nn(rawTypeMeta))`. The `if (rawTypeMeta == null) return false` bailout was relaxed: it now fires only when `bc != null && rawTypeMeta == null` (marshalling-side type-resolution failure); on the commons-side path the property is accepted with null type meta.
- `BeanPropertyMeta` instance carries the same `bc` (nullable) plus a non-null `config` field mirrored from the builder. The `ap` field is sourced from `bc.getAnnotationProvider()` when bc is non-null, else `config.getAnnotationProvider()`.
- `BeanPropertyMeta.getRaw(...)`: catch block guards `bc.isIgnoreInvocationExceptionsOnGetters()` and `rawTypeMeta.isPrimitive()` with null checks.
- `BeanPropertyMeta.getInner(...)`: same catch-block guards. Error message now uses `beanMeta.getClassInfo()` instead of `beanMeta.getClassMeta()` so it works on both construction paths.
- `BeanPropertyMeta.set(...)`: new early-return raw-reflection path when `rawTypeMeta == null` — calls `invokeSetter(bean, pName, value1)` directly after capturing the old value via `get(...)`. Skips all the `isMap/isCollection/setPropertyValue` machinery that depends on type metadata.
- `BeanPropertyMeta.set(...)` settings reads route through `config.isIgnoreMissingSetters()` and guard `bc.isIgnoreUnknownNullBeanProperties()` with non-null check.
- `BeanPropertyMeta.invokeGetter`/`invokeSetter` error messages: `beanMeta.getClassMeta()` → `beanMeta.getClassInfo()`. New private `classNameForError()` helper handles the type-name format (uses `getClassMeta().getName()` when non-null, else falls back to `classInfo.getName()`).

`BeanMap` changes:
- `BeanMap.add(...)` and `BeanMap.put(...)`: `meta.getMarshallingContext().isIgnoreUnknownBeanProperties()` → `meta.getConfig().isIgnoreUnknownBeanProperties()`. Error-message argument switched from `meta.getClassMeta()` to `meta.getClassInfo()` for compatibility with commons-built `BeanMeta`.
- New `BeanMap.of(T bean, BeanMeta<T> meta)` static factory — the commons-side counterpart to `BeanMap.of(T bean)` (which still routes through `MarshallingContext.DEFAULT_SESSION.toBeanMap(bean)`).

Test coverage: new `BeanMeta_Test` at `juneau-utest/src/test/java/org/apache/juneau/commons/bean/BeanMeta_Test.java` covers 10 scenarios — `BeanMeta.of(Class)` / `BeanMeta.of(Class, BeanConfigContext.DEFAULT)` factories, property discovery, raw getter/setter invocation via `BeanPropertyMeta.get/set/getRaw`, `BeanMap.get/put` round-trips, and a custom `BeanConfigContext` with `findFluentSetters` enabled. Verifies `getClassMeta() == null` on commons-built `BeanMeta` and `getClassInfo() != null` always. Full test suite green.

Known limitations of the commons-side path (acceptable for Step 6, scoped for later cleanup):
- `BeanPropertyMeta.set` short-circuits to raw setter invocation when `rawTypeMeta == null` — no type conversion, no collection/map setter-fallback, no `swap.unswap` path. Bean-modeling-only callers should pass values already of the property type.
- `BeanPropertyMeta.add(BeanMap, String, Object)` / `add(BeanMap, String, String, Object)` (Collection/array/Map helpers) still read `rawTypeMeta` unconditionally — calling them on a commons-built `BeanPropertyMeta` will NPE. Same applies to `applyChildPropertiesFilter`, `setArray`, and the second half of `setPropertyValue`. None of these paths are reached by the test, and none are needed for the bean-modeling minimum.
- `MarshalledProp` swap detection in `validate(...)` still runs even when bc is null (it doesn't require a `MarshallingContext`), but no `installSwapAwareTransforms` is invoked since the marshalling-side install helper short-circuits when `rawTypeMeta == null`.

**Step 5 complete.** `BeanPropertyMeta` no longer carries a `BeanRegistry` field. Picked **Option B** (side-map keyed by `BeanPropertyMeta`) — there are only 3 marshalling-side call sites that read the per-property registry (`ParserSession.getClassMeta(...)`, `SerializerSession.getBeanTypeName(...)`, `XmlParserSession.parseIntoMap-mixed-content`), and they all use the existing `pMeta.getBeanRegistry()` public accessor. The accessor stays as a backwards-compat delegate that routes to the side-map on `BeanMeta`. The per-property `BeanRegistry` construction (previously inside `BeanPropertyMeta.Builder.validate(...)` at line ~460) moved to `BeanMeta` itself: `Builder.validate(...)` now just accumulates `dictionaryClasses` (package-private `List<ClassInfo>`) during annotation scanning, and `BeanMeta` constructs the registry after `v.build()` and stores it in a `Map<BeanPropertyMeta,BeanRegistry> propertyBeanRegistries` field (sealed via `u(...)` after construction). The `parentBeanRegistry` parameter on `Builder.validate(...)` is gone (the only caller was `BeanMeta`, and the parent is always the bean-level registry). Synthetic `_type` property no longer calls `.beanRegistry(beanRegistry.get())` — instead BeanMeta adds the typeProperty entry to the side-map mapped to the bean-level registry. Two surviving `this.classMeta` references inside `BeanMeta.findDictionaryName(...)` were lifted out: added a `BeanRegistry.getTypeName(Class<?>)` overload (the existing `getTypeName(ClassMeta<?>)` just calls `c.inner()` anyway) and `findDictionaryName` now calls `br.getTypeName(classInfo.inner())` for both the local and parents/interfaces paths. The `marshallingContext::getClassMeta` map call in the stream stays (it builds ClassMetas for parent classes/interfaces to read THEIR BeanRegistry) — that's a separate Step 6 concern. Full test suite green.

**Step 4 complete.** `BeanMap` no longer takes a `MarshallingSession` in its constructor. Picked **Option (c)** (transitional setter) per the plan — minimum-disturbance and no behavioral change. The `private final MarshallingSession session` field became `private MarshallingSession session` (no longer final, defaults to null), the constructor signature dropped to `BeanMap(T bean, BeanMeta<T> meta)`, and a new `protected void setMarshallingSession(MarshallingSession value)` is called by the marshalling layer immediately after construction. Every `new BeanMap(session, ...)` call site (`MarshallingSession.toBeanMap`, `MarshallingSession.newBeanMap`, `BeanPropertyMeta.applyChildPropertiesFilter`, `DelegateBeanMap`) was updated to construct + `setMarshallingSession(session)`. The `typePropertyName` field — previously seeded from `session.getBeanTypePropertyName(meta.getClassMeta())` — now comes directly from `meta.getTypePropertyName()` (which `BeanMeta` already memoizes during construction with the same fallback chain), so the constructor no longer needs the session at all. The `getMarshallingSession()` accessor stays on `BeanMap` (still read by `BeanPropertyMeta.add` / `BeanPropertyMeta.set` / `applyChildPropertiesFilter` for `convertToType`/`JsonList`/`JsonMap` construction, and by `ParserSession.parseIntoCollection`); after Step 5+ removes `BeanRegistry`/`MarshallingSession` field usage from `BeanPropertyMeta`, the back-pointer can be deleted entirely. `BeanMap.of(T)` static factory continues to work unchanged (it goes through `MarshallingContext.DEFAULT_SESSION.toBeanMap(bean)`, which now does construct + `setMarshallingSession`). Full test suite green.

**Step 3 complete.** `BeanPropertyMeta.get`/`set` no longer call `ObjectSwap.swap`/`unswap` directly. The class now carries two install-time `BiFunction<MarshallingSession,Object,Object>` callbacks (`readTransform`/`writeTransform`) that default to identity. `BeanMeta` installs swap-aware closures via a new private helper `installSwapAwareTransforms(BeanPropertyMeta.Builder)` immediately after `Builder.validate()` succeeds (only when `Builder.swap != null` or `Builder.rawTypeMeta.hasChildSwaps()`). The `BeanPropertyMeta.swap` field stays as metadata (still read by the defensive double-unswap check inside `setPropertyValue`, which now routes the actual unswap call through `writeTransform`). The private `swap()` / `unswap()` methods on `BeanPropertyMeta` are gone. Eight runtime types still live in `juneau-marshall`.

- [x] **Step 1** — `BeanConfigContext` POJO + builder in `commons.bean`. Carries: visibility settings, all `beans*Require*` toggles, `findFluentSetters`, `unsortedProperties`, `useInterfaceProxies`, `useJavaBeanIntrospector`, `ignoreMissingSetters`, `ignoreTransientFields`, `ignoreUnknownBeanProperties`, `propertyNamer`, `beanTypePropertyName`, `notBeanPackageNames` / `notBeanPackagePrefixes` / `notBeanClasses`, `BeanStore`, `AnnotationProvider`, optional `Predicate<ClassInfo>` override for `isNotABean`. Includes `DEFAULT` singleton, `create()` builder factory, `copy()`. `MarshallingContext` exposes a memoized `getBeanConfigContext()` returning a snapshot. Unit tests at `juneau-utest/src/test/java/org/apache/juneau/commons/bean/BeanConfigContext_Test.java` cover 100% instructions / 97% branches.
- [x] **Step 2** — Replaced `ClassMeta` with `ClassInfo` for pure-reflection access inside `BeanMeta`. Added a `classInfo` field (a re-typed view of the same instance as `classMeta`, since `ClassMeta extends ClassInfoTyped extends ClassInfo`) and routed all reflection calls (`inner()`, `isMemberClass()`, `isNotStatic()`, `isAnonymousClass()`, `isRecord()`, `isInterface()`, `getRecordComponents()`, `getName()`, `getParentsAndInterfaces()`, `getPublicConstructors()`, `getDeclaredConstructors()`, `getPublicConstructor()`, `getNoArgConstructor()`) through `classInfo`. The `classMeta` field is kept (it still backs the public `getClassMeta()` getter and the two surviving marshalling-aware `BeanRegistry.getTypeName(ClassMeta<?>)` calls inside `findDictionaryName`). The two outliers: (a) `cm.getProxyInvocationHandler()` in `newBean()` was replaced with a direct call to the already-memoized `beanProxyInvocationHandler` field on the BeanMeta itself (ClassMeta was just delegating back to this BeanMeta anyway), and (b) the synthetic `_type` property no longer pulls `marshallingContext.string()` — `BeanPropertyMeta.Builder` got a new `rawMetaType(Class<?>)` overload that resolves to a `ClassMeta` internally via its existing `bc` reference, so `BeanMeta` simply passes `String.class`. `BeanPropertyMeta` itself was otherwise untouched — its remaining `ClassMeta` references (`rawTypeMeta`, `typeMeta`, the `applyChildPropertiesFilter` parameter, and the `Builder.bc` / `Builder.swap` / `Builder.beanRegistry` fields) are all tied to property-type-resolution or `ObjectSwap`/`BeanRegistry`/`MarshallingSession` behavior that Steps 3–5 will untangle. Full test suite green.
- [x] **Step 3** — Removed swap-aware `get`/`set` from `BeanPropertyMeta`. Picked option (a) (pluggable callbacks). Added `BiFunction<MarshallingSession,Object,Object>` `readTransform` / `writeTransform` fields with identity defaults; exposed corresponding `Builder.readTransform(...)` / `Builder.writeTransform(...)` setters. The bean-modeling `get`/`set` paths inside `BeanPropertyMeta` no longer call `ObjectSwap.swap` / `ObjectSwap.unswap` directly — instead they invoke the installed transforms. The marshalling-side install is centralized in a new `BeanMeta.installSwapAwareTransforms(BeanPropertyMeta.Builder)` static helper called from `validateAndRegisterProperty` after `validate()` succeeds. The helper closes over the builder's `swap` and `rawTypeMeta` to construct the same `(sw, sw.swap)` / `(child swap, child swap.swap)` chain that previously lived inline in `BeanPropertyMeta.swap()` / `unswap()`, wrapping checked `Exception` from `ObjectSwap` into `SerializeException`/`ParseException` (both `RuntimeException` subclasses, so callers don't need new try-catch blocks). The `Builder.swap` / `Builder.rawTypeMeta` fields became package-private so `BeanMeta` (same package) can read them; the `swap` field on `BeanPropertyMeta` itself stays as metadata (still used by `setPropertyValue`'s defensive double-unswap *check*, but the actual unswap call routes through `writeTransform`). The private `swap(MarshallingSession,Object)` / `unswap(MarshallingSession,Object)` methods on `BeanPropertyMeta` were deleted. Identity-only properties (e.g. the synthetic `_type` property built in `BeanMeta` line ~610, the `DelegateBeanMap` override/delegate properties) skip the install helper entirely and run with the identity defaults. Full test suite green.
- [x] **Step 4** — Removed `MarshallingSession` from the `BeanMap` constructor signature (Option c — transitional setter). `BeanMap` now exposes `setMarshallingSession(MarshallingSession)` that the marshalling layer wires in immediately after construction. The `session` field defaults to null on direct construction; only `BeanMap.getBean()` (for read-only beans with constructor args), `BeanPropertyMeta.add`/`set`, and child-properties-filter operations need it set, and they all go through marshalling-layer entry points (`MarshallingSession.toBeanMap` / `MarshallingSession.newBeanMap` / `BeanPropertyMeta.applyChildPropertiesFilter` / `DelegateBeanMap`) that always wire the session. `typePropertyName` now comes from `meta.getTypePropertyName()` instead of `session.getBeanTypePropertyName(meta.getClassMeta())` since `BeanMeta` already memoizes the fallback. Full test suite green.
- [x] **Step 5** — Removed `BeanRegistry` field from `BeanPropertyMeta`. Picked **Option B** — side-map keyed by `BeanPropertyMeta` lives on `BeanMeta` (`Map<BeanPropertyMeta,BeanRegistry> propertyBeanRegistries`). Per-property registries are constructed by `BeanMeta` after `v.build()` from the builder's package-private `dictionaryClasses` field (populated during `Builder.validate(...)`). `Builder.beanRegistry(...)` public setter, the `beanRegistry` builder field, and the `parentBeanRegistry` parameter on `validate(...)` are all gone. `BeanPropertyMeta.getBeanRegistry()` survives as a backwards-compat delegate that calls `beanMeta.getPropertyBeanRegistry(this)`; the three marshalling-side call sites (`ParserSession`, `SerializerSession`, `XmlParserSession`) need no source changes. The two surviving `this.classMeta` references inside `BeanMeta.findDictionaryName(...)` from Step 2 were lifted to `classInfo.inner()` via a new `BeanRegistry.getTypeName(Class<?>)` overload. Full test suite green.
- [x] **Step 6** — `BeanMeta.of(Class<T>, BeanConfigContext)` factory + `protected BeanMeta(Class<T>, BeanConfigContext)` constructor wired up. `BeanMeta` now carries a non-null `BeanConfigContext config` facade for all settings reads; the `marshallingContext` and `classMeta` fields are documented-nullable and stay null on the commons-side path. `BeanPropertyMeta.Builder.bc` and `BeanPropertyMeta.bc` similarly nullable; new mirrored `config` field on both. `Builder.validate(...)` accepts a null `bc` and skips type-resolution (rawTypeMeta/typeMeta stay null). `BeanPropertyMeta.set/getRaw/getInner` guard `bc.*` and `rawTypeMeta.*` reads; `set` has an early-return raw-reflection path when `rawTypeMeta == null`. `BeanMap.add/put` route through `meta.getConfig().isIgnoreUnknownBeanProperties()` instead of `meta.getMarshallingContext()`. New `BeanMap.of(T, BeanMeta<T>)` static factory for the commons-side path. New test `BeanMeta_Test` (10 tests) verifies factory construction, property discovery, raw get/set via `BeanPropertyMeta` and `BeanMap.get/put`, and a custom `BeanConfigContext` with fluent setters. Full test suite green. **Known limitations**: `BeanPropertyMeta.add` (Collection/Map/array helpers), `applyChildPropertiesFilter`, `setArray`, and the back half of `setPropertyValue` still read `rawTypeMeta` unconditionally — callable only on marshalling-built `BeanPropertyMeta`.
- [x] **Step 7** — Per-format extension survey + pre-Step-8 hardening. (a) Survey result: `ExtendedBeanMeta` (composes `BeanMeta<?>`), `XmlBeanMeta` (extends `ExtendedBeanMeta`), `RdfBeanMeta` (extends `ExtendedBeanMeta`); no `HtmlBeanMeta` exists. All marshalling-side, all stay in `juneau-marshall`. (b) Hardened `BeanPropertyMeta.add(BeanMap,String,Object)` / `add(BeanMap,String,String,Object)` / `setArray` / `applyChildPropertiesFilter` (all throw `UnsupportedOperationException` with a clear "bean-modeling-only path" message when called on commons-built properties); hardened `swapAndFilterProperty` to short-circuit when `rawTypeMeta == null`; hardened `BeanMap.getBean()` to null-check `v.getClassMeta()` before invoking `cm.isOptional()` / `cm.getOptionalDefault()` on per-property `ClassMeta`. New tests `e01`/`e02`/`f01`/`f02` in `BeanMeta_Test`. Full test suite green.
- [ ] **Step 8** — `git mv` the eight runtime types into `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/`. Verify `juneau-commons` still compiles standalone (`cd juneau-core/juneau-commons && mvn clean compile`).
- [ ] **Step 9** — Reference sweep: 80–120 unique files (mostly inside `juneau-marshall`). Update imports, Javadoc `{@link …}` references, package-info docs.
- [ ] **Step 10** — Update `juneau-docs` release notes / migration guide (`docs/pages/release-notes/9.5.0.md`, `## Package Moves` section) with the bean-runtime relocations.

The "incomplete-but-documented over broken-build" rule from Phase 5a still applies. When picking up the next slice of this work, **Step 8 is the recommended next checkpoint** — the physical `git mv` of the eight runtime types into `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/`. Pre-flight is now clean: per-format `BeanMeta` extensions (`ExtendedBeanMeta`/`XmlBeanMeta`/`RdfBeanMeta`) are confirmed marshalling-side and stay put (Step 7); the previously unguarded `BeanPropertyMeta` and `BeanMap` paths flagged in Step-6's risk notes (`add(BeanMap,String,Object)`, `add(BeanMap,String,String,Object)`, `applyChildPropertiesFilter`, `setArray`, the back half of `setPropertyValue`, and `BeanMap.getBean()`'s Optional-init loops) are now defensive — they either throw `UnsupportedOperationException` with a clear bean-modeling-only message, or null-check `v.getClassMeta()` and skip the marshalling-side initialization, depending on whether a sensible raw fallback exists.

**Remaining risks/surprises for Step 8:**
- The eight files reference `MarshallingContext`/`MarshallingSession`/`ClassMeta`/`BeanRegistry`/`ObjectSwap` in many places that survived Step 6/7 (the `installSwapAwareTransforms` install path, the `BeanRegistry` side-map on `BeanMeta`, the `MarshallingSession` field on `BeanMap`, the `ClassMeta classMeta` field on `BeanMeta`, etc.). Physically moving the files into `commons.bean` will require either: (a) leaving these fields in place but moving the types, accepting that `juneau-commons` then takes a compile-time dependency on the marshalling types (defeats the goal); or (b) extracting interfaces/SPI seams in `commons.bean` that the marshalling layer implements. Option (b) is the right answer but is a meaningful chunk of work; expect Step 8 to expand into "Step 8a/8b/8c" once the seam shape is sketched.
- `BeanPropertyMeta.applyChildPropertiesFilter` directly takes a `ClassMeta` parameter — the signature itself encodes a marshalling-side type. If it stays on `BeanPropertyMeta` after the move, `commons.bean` will need a forward reference to `ClassMeta` (which is in `juneau-marshall`). Likely needs to migrate to a side helper in the marshalling layer.
- `BeanMap.getBean(boolean create)`'s constructor-args path uses `session.convertToType(rawVal, cm)` — this entire block is `@BeanCtor` parser-side behavior. Should migrate to a marshalling-side helper after the move (Step 9 or later).

---

## Goal

Move these eight `BeanXxx` runtime types out of `org.apache.juneau` (in `juneau-marshall`) into `org.apache.juneau.commons.bean` (in `juneau-commons`) so the **bean-modeling runtime** is independently usable without dragging in the full marshalling stack:

1. `BeanMap.java` (~727 lines)
2. `BeanMapEntry.java` (~129 lines)
3. `BeanMeta.java` (~1547 lines)
4. `BeanMetaFiltered.java` (~49 lines)
5. `BeanPropertyMeta.java` (~1423 lines)
6. `BeanPropertyValue.java` (~120 lines)
7. `BeanPropertyConsumer.java` (~23 lines)
8. `BeanProxyInvocationHandler.java` (~156 lines)

Total: ~4,170 lines of code with ~140 cross-references to marshalling-aware types (`ClassMeta`, `MarshallingContext`, `ObjectSwap`, `BeanRegistry`, etc.).

---

## Why Phase 5 wasn't fully landed in one commit

A first-pass survey revealed that the dependencies are too tangled to do in a single sweep without risking a broken build. The shape of the entanglement:

### `BeanPropertyMeta` (74 marshalling refs)
- `BeanPropertyMeta.Builder` carries a `MarshallingContext bc` field used everywhere.
- Carries `ClassMeta<?> rawTypeMeta`, `ClassMeta<?> typeMeta`, `ObjectSwap swap`, `BeanRegistry beanRegistry`.
- `get(BeanMap, String)` / `set(BeanMap, String, Object)` paths apply `ObjectSwap` transforms inline.
- Constructor-arg detection reads `MarshallingContext.getBeanStore()`.
- Property-type resolution flows through `MarshallingContext.resolveClassMeta(...)`.

### `BeanMeta` (26 marshalling refs)
- `marshallingContext = cm.getMarshallingContext()` is the central field; settings used:
  - `isFindFluentSetters()`, `isBeansRequireSerializable()`, `isBeansRequireDefaultConstructor()`, `isBeansRequireSomeProperties()`, `isUnsortedProperties()`, `isIgnoreTransientFields()`, `isUseJavaBeanIntrospector()`, `isUseInterfaceProxies()`, `isNotABean()`
  - `getBeanClassVisibility()`, `getBeanConstructorVisibility()`, `getBeanFieldVisibility()`, `getBeanMethodVisibility()`
  - `getPropertyNamer()`, `getBeanStore()`, `getAnnotationProvider()`
  - `string()` returns a `ClassMeta<String>` used to seed a property meta.
- `ClassMeta` is used for `classMeta.inner()`, `classMeta.isMemberClass()`, `classMeta.isAnonymousClass()`, `classMeta.isAssignableTo(...)`, `classMeta.getRecordComponents()`, `classMeta.getProxyInvocationHandler()`, `classMeta.getClassLoader()`, plus `cm.getModifiers()`.

### `BeanMap` (29 marshalling refs)
- Constructed by `MarshallingSession.toBeanMap(...)`.
- `get`/`put` go through `MarshallingSession` swap-aware property access.
- Holds a back-pointer to its session and uses it for type narrowing.

### `BeanProxyInvocationHandler` (5 refs), `BeanMapEntry` (3), `BeanPropertyValue` (4), `BeanMetaFiltered` (1)
- All transitively reference `ClassMeta` and/or `MarshallingContext`.

### Other affected types that stay in `juneau-marshall`
- `BeanRegistry` (polymorphic dispatch — marshalling concern)
- `BeanDictionaryMap`, `BeanDictionaryList` (same)
- `MarshalledFilter` (reads marshalling-only annotation attributes; bean-modeling attributes now read via `commons.bean` annotation processors)
- `MarshallingContext`, `MarshallingSession` (stay; `MarshallingContext` will compose a new `BeanConfigContext` POJO)
- `ClassMeta` (marshalling-aware type metadata; will *consume* a `commons.bean.BeanMeta` after the move)

---

## Strategy

The whole point is to make `commons.bean` self-contained, so the untangling has to address every leaf of the dependency graph. Recommended sequencing:

### Step 1 — Introduce `BeanConfigContext` (runtime POJO) in `commons.bean`

Create `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanConfigContext.java` as a **plain immutable POJO** with builder. Suggested name: **`BeanConfigContext`** (not `BeanConfig` — that name is already taken by the existing annotation in the same package from Phase 3). Alternatives if `BeanConfigContext` reads awkwardly at use sites: `BeanModelContext`, `BeanIntrospectionConfig`.

Carries:
- `Visibility beanClassVisibility`, `Visibility beanConstructorVisibility`, `Visibility beanFieldVisibility`, `Visibility beanMethodVisibility`
- `boolean findFluentSetters`
- `boolean sortProperties`, `Set<Class<?>> unsortedProperties` (or equivalent)
- `PropertyNamer propertyNamer`
- `boolean beansRequireSomeProperties`, `boolean beansRequireDefaultConstructor`, `boolean beansRequireSerializable`, `boolean beansRequireSettersForGetters`
- `boolean useJavaBeanIntrospector`, `boolean useInterfaceProxies`, `boolean ignoreTransientFields`
- `boolean ignoreUnknownBeanProperties`, `boolean ignoreMissingSetters`
- A lambda/predicate for `isNotABean(Class<?>)`
- Reference to a commons-compatible `BeanStore` (already exists at `commons.inject.BeanInstantiator` — confirm scope)
- Reference to a commons-compatible `AnnotationProvider` (need to confirm where this currently lives — probably `commons.reflect`)

`MarshallingContext` retains all these settings (they're already there), and composes (or delegates to) a `BeanConfigContext` view so the marshalling layer reads them transparently. `MarshallingContext.Builder` produces a `BeanConfigContext` snapshot on `build()`.

### Step 2 — Replace `ClassMeta<T>` with `ClassInfo`

`BeanMeta` and `BeanPropertyMeta` use `ClassMeta` for: `inner()`, `isMemberClass()`, `isAnonymousClass()`, `isAssignableTo(...)`, `isInterface()`, `isRecord()`, `getRecordComponents()`, `getModifiers()`, `getClassLoader()`. **All of these are pure reflection** and already available on `ClassInfo`. Swap them.

Two `ClassMeta`-only methods used by `BeanMeta`:
- `classMeta.getProxyInvocationHandler()` — used to build the proxy when wrapping an interface. Move this responsibility into `BeanMeta` itself or to a hook in `BeanConfigContext`. `BeanProxyInvocationHandler` already takes a `BeanMeta<T>`; the construction site is the only thing that needs to change.
- `cm.getMarshallingContext().string()` — used as the raw type for the synthetic `_type` property. After the move, store a plain `Class<String>` or `ClassInfo` and let the marshalling layer wrap that into a `ClassMeta` if it needs to.

### Step 3 — Swap-aware logic in `BeanPropertyMeta`

Recommendation (a): **Remove swap-aware get/set in `BeanPropertyMeta`**. The marshalling layer (already in `MarshallingSession`) applies swaps externally — `BeanPropertyMeta` does raw getter/setter invocation only. This keeps the bean model lean.

If consumers still want a swap-aware extension point, expose two `BiFunction<Object,Object,Object>` callbacks on `BeanPropertyMeta` (or a small `BeanPropertyTransform` interface) that default to identity. The marshalling layer installs swap-aware callbacks at session construction.

### Step 4 — `BeanMap.get/put` decoupling

Same idea: after the move, `BeanMap.get`/`put` do raw property access (calling `BeanPropertyMeta`'s raw getter/setter). `MarshallingSession` wraps a `BeanMap` for serialization and applies swaps externally per property as needed.

### Step 5 — `BeanRegistry` lives in `juneau-marshall` (unchanged)

`BeanRegistry` is a marshalling concern (polymorphic dispatch via `@Marshalled(typeName=...)` / `@Marshalled(dictionary=...)`). After the move, `BeanPropertyMeta` in `commons.bean` does **not** carry a `BeanRegistry` field. Instead:
- The bean-model `BeanPropertyMeta` exposes the raw `@BeanType` / `@BeanProp` metadata.
- The marshalling layer reads `@Marshalled` dictionary metadata separately and pairs it with the bean property via a `MarshalledPropertyMeta` (new) or a side-map keyed by `BeanPropertyMeta`.

### Step 6 — `BeanMeta` is constructed by both `ClassMeta` and direct `BeanConfigContext` callers

After the move:
- A direct `commons.bean` consumer does `BeanMeta.of(MyClass.class, BeanConfigContext.DEFAULT)`.
- `ClassMeta` does the same internally and stores the resulting `BeanMeta` as a member.

This makes `ClassMeta` a *consumer* of `BeanMeta` rather than its creator.

### Step 7 — `ExtendedBeanMeta` (marshalling-side `BeanMeta` extension) stays

`ExtendedBeanMeta` (and per-format extensions like `XmlBeanMeta`, `RdfBeanMeta`, `HtmlBeanMeta`) currently extend or compose `BeanMeta`. Re-check whether they need to follow `BeanMeta` to `commons.bean` (probably not — they're marshalling-aware and stay).

### Step 8 — Test fallout

- `BeanMap_Test`, `BeanMeta_Test`, `AnnotationInheritance_Test`, plus any test that imports `org.apache.juneau.BeanMap`/`BeanMeta`/`BeanPropertyMeta` needs an import update (rename `org.apache.juneau.BeanMap` to `org.apache.juneau.commons.bean.BeanMap` etc.).
- Many tests use these types via the unqualified name from inside `org.apache.juneau` — those need explicit imports added.

### Step 9 — Reference sweep across the repo

Estimated reference counts (from a survey before Phase 5a):
- ~50 files reference `BeanMeta` directly
- ~50 files reference `BeanMap` directly
- ~50 files reference `BeanPropertyMeta` directly
- Lots of overlap, but expect 80–120 unique files to need import updates (most are inside `juneau-marshall` and currently use the in-package short name).
- Plus `juneau-docs` (release notes, topic pages).

### Step 10 — Module dependency check

`juneau-commons/pom.xml` must NOT add a dependency on `juneau-marshall`. Verify by building `juneau-commons` standalone after the move:
```bash
cd juneau-core/juneau-commons && mvn clean compile
```

---

## Build verification

Build module-by-module as you go. Don't try to move everything then build:

```bash
# After each set of changes
python3 scripts/test.py --build-only
# Periodically
python3 scripts/test.py --full
```

---

## Commit message

`refactor: move BeanMap/BeanMeta/BeanPropertyMeta runtime types to commons.bean (Phase 5 of bean-layer split)`

---

## Risks and open questions

1. **`AnnotationProvider`** — currently lives in `juneau-marshall` (`org.apache.juneau.commons.reflect.AnnotationProvider`? need to verify exact location and module). If it's already in `commons`, great. If not, it has to move first.
2. **`BeanStore`** — `commons.inject` has `BeanInstantiator`. Is that enough for `BeanMeta`'s constructor-resolution path, or does it need the richer `BeanStore` that lives in `juneau-marshall`?
3. **`ObjectSwap` removal from `BeanPropertyMeta`** — there are likely a handful of test cases that exercise swap-aware `BeanMap.get`/`put` directly. They will need to be rewritten to go through a `MarshallingSession`.
4. **`@Marshalled(properties=…)` / `@Marshalled(dictionary=…)`** reading inside `BeanMeta` — the bean-modeling layer should only read `@BeanType` / `@BeanProp`. The `@Marshalled`-specific bits (typeName/dictionary/properties-as-include-list) need to be lifted out into the marshalling-side `MarshalledFilter` or an analog.
5. **`BeanRegistry` removal from `BeanPropertyMeta`** — see Step 5; the side-map design needs to be sketched before any code moves.

---

## Definition of done

- [ ] All 8 files physically located in `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/`.
- [ ] None of those 8 files reference `ClassMeta`, `MarshallingContext`, `MarshallingSession`, `ObjectSwap`, or `BeanRegistry`.
- [ ] `mvn clean compile` of `juneau-commons` succeeds standalone.
- [ ] `scripts/test.py --build-only` and `--full` both pass.
- [ ] Reference sweep done: no stale `import org.apache.juneau.BeanMap;` etc. anywhere in the repo.
- [ ] `juneau-docs` release-notes / migration-guide updated with the package moves.
- [ ] Remove this TODO from `todo/TODO.md` (final cleanup step).
