# TODO-5 — Move bean-modeling runtime types (`BeanMap`, `BeanMeta`, `BeanPropertyMeta`, …) into `commons.bean` (remainder of Phase 5)

This is the remaining work from **Phase 5 of the bean-layer split**. Phase 5a (the `MarshallingInterceptor` → `commons.bean.BeanInterceptor` move+rename) shipped in the same commit that created this plan. The big runtime types still live in `juneau-marshall` because they are deeply entangled with `ClassMeta` / `MarshallingContext` / `ObjectSwap` / `BeanRegistry`, and untangling them is the bulk of the work.

---

## Status (as of Phase 5b checkpoint)

**Step 1 complete.** A `BeanConfigContext` POJO + builder now lives in `commons.bean`; `MarshallingContext.getBeanConfigContext()` returns a memoized snapshot view. The eight runtime types still live in `juneau-marshall` and still use `MarshallingContext` directly — Step 1 is purely additive infrastructure that future steps can lean on.

- [x] **Step 1** — `BeanConfigContext` POJO + builder in `commons.bean`. Carries: visibility settings, all `beans*Require*` toggles, `findFluentSetters`, `unsortedProperties`, `useInterfaceProxies`, `useJavaBeanIntrospector`, `ignoreMissingSetters`, `ignoreTransientFields`, `ignoreUnknownBeanProperties`, `propertyNamer`, `beanTypePropertyName`, `notBeanPackageNames` / `notBeanPackagePrefixes` / `notBeanClasses`, `BeanStore`, `AnnotationProvider`, optional `Predicate<ClassInfo>` override for `isNotABean`. Includes `DEFAULT` singleton, `create()` builder factory, `copy()`. `MarshallingContext` exposes a memoized `getBeanConfigContext()` returning a snapshot. Unit tests at `juneau-utest/src/test/java/org/apache/juneau/commons/bean/BeanConfigContext_Test.java` cover 100% instructions / 97% branches.
- [x] **Step 2** — Replaced `ClassMeta` with `ClassInfo` for pure-reflection access inside `BeanMeta`. Added a `classInfo` field (a re-typed view of the same instance as `classMeta`, since `ClassMeta extends ClassInfoTyped extends ClassInfo`) and routed all reflection calls (`inner()`, `isMemberClass()`, `isNotStatic()`, `isAnonymousClass()`, `isRecord()`, `isInterface()`, `getRecordComponents()`, `getName()`, `getParentsAndInterfaces()`, `getPublicConstructors()`, `getDeclaredConstructors()`, `getPublicConstructor()`, `getNoArgConstructor()`) through `classInfo`. The `classMeta` field is kept (it still backs the public `getClassMeta()` getter and the two surviving marshalling-aware `BeanRegistry.getTypeName(ClassMeta<?>)` calls inside `findDictionaryName`). The two outliers: (a) `cm.getProxyInvocationHandler()` in `newBean()` was replaced with a direct call to the already-memoized `beanProxyInvocationHandler` field on the BeanMeta itself (ClassMeta was just delegating back to this BeanMeta anyway), and (b) the synthetic `_type` property no longer pulls `marshallingContext.string()` — `BeanPropertyMeta.Builder` got a new `rawMetaType(Class<?>)` overload that resolves to a `ClassMeta` internally via its existing `bc` reference, so `BeanMeta` simply passes `String.class`. `BeanPropertyMeta` itself was otherwise untouched — its remaining `ClassMeta` references (`rawTypeMeta`, `typeMeta`, the `applyChildPropertiesFilter` parameter, and the `Builder.bc` / `Builder.swap` / `Builder.beanRegistry` fields) are all tied to property-type-resolution or `ObjectSwap`/`BeanRegistry`/`MarshallingSession` behavior that Steps 3–5 will untangle. Full test suite green.
- [ ] **Step 3** — Remove swap-aware `get/set` from `BeanPropertyMeta`. Add identity-default `BiFunction<Object,Object,Object>` callbacks (or a small `BeanPropertyTransform` SPI) so the marshalling layer installs swap-aware behavior at session construction.
- [ ] **Step 4** — Remove `MarshallingSession` back-pointer from `BeanMap`. After Step 3, `BeanMap.get/put` are raw property reads/writes; `MarshallingSession.toBeanMap` wraps a `BeanMap` for serialization and applies swaps externally.
- [ ] **Step 5** — Remove `BeanRegistry` field from `BeanPropertyMeta`. Lift dictionary metadata into a marshalling-side companion (`MarshalledPropertyMeta` or a side-map keyed by `BeanPropertyMeta`).
- [ ] **Step 6** — `BeanMeta` becomes constructible by both `ClassMeta` and direct `commons.bean` callers via `BeanMeta.of(MyClass.class, BeanConfigContext.DEFAULT)`. `ClassMeta` becomes a *consumer* of `BeanMeta` rather than its creator.
- [ ] **Step 7** — Re-check whether `ExtendedBeanMeta` and per-format extensions (`XmlBeanMeta`, `RdfBeanMeta`, `HtmlBeanMeta`) need to follow `BeanMeta` to `commons.bean`. Default expectation: they stay in `juneau-marshall`.
- [ ] **Step 8** — `git mv` the eight runtime types into `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/`. Verify `juneau-commons` still compiles standalone (`cd juneau-core/juneau-commons && mvn clean compile`).
- [ ] **Step 9** — Reference sweep: 80–120 unique files (mostly inside `juneau-marshall`). Update imports, Javadoc `{@link …}` references, package-info docs.
- [ ] **Step 10** — Update `juneau-docs` release notes / migration guide (`docs/pages/release-notes/9.5.0.md`, `## Package Moves` section) with the bean-runtime relocations.

The "incomplete-but-documented over broken-build" rule from Phase 5a still applies. When picking up the next slice of this work, Step 3 is the recommended next checkpoint — Step 2 removed the general `ClassMeta` reflection coupling, so the next blocker is the swap-aware `get`/`set` paths in `BeanPropertyMeta` (and the associated `rawTypeMeta` / `typeMeta` / `ObjectSwap` / `BeanRegistry` fields that Step 2 intentionally left in place).

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
