# FINISHED-5 — Move bean-modeling runtime types into `commons.bean` (Phase 5 of bean-layer split)

> **Archived from `TODO-5-bean-runtime-types-to-commons.md`.** The move is complete: all 7 bean-runtime types (`BeanMap`, `BeanMapEntry`, `BeanMeta`, `BeanPropertyMeta`, `BeanPropertyValue`, `BeanPropertyConsumer`, `BeanProxyInvocationHandler`) now live in `org.apache.juneau.commons.bean`; `juneau-commons` compiles standalone with no `juneau-marshall` dependency; full test suite green; release-notes and migration-guide entries landed in `juneau-docs`. Kept as a record of the SPI extractions, blocker resolutions, and phased sequencing used to untangle `ClassMeta` / `MarshallingContext` / `ObjectSwap` / `BeanRegistry` from the bean-modeling layer.

---

Original plan content (Phase 5 of the bean-layer split). Phase 5a (the `MarshallingInterceptor` → `commons.bean.BeanInterceptor` move+rename) shipped in the same commit that created this plan. The big runtime types lived in `juneau-marshall` because they were deeply entangled with `ClassMeta` / `MarshallingContext` / `ObjectSwap` / `BeanRegistry`, and untangling them was the bulk of the work.

---

## Status (Physical `git mv` LANDED — uncommitted)

**All 5 remaining blockers cleared and the 7 bean-runtime files physically moved into `commons.bean`.** `juneau-commons` compiles standalone with no dependency on `juneau-marshall`. Full test suite (`scripts/test.py --test-only`) passes.

### New file locations (post-`git mv`)

| Old path | New path |
| --- | --- |
| `juneau-core/juneau-marshall/.../BeanMap.java` | `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanMap.java` |
| `juneau-core/juneau-marshall/.../BeanMapEntry.java` | `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanMapEntry.java` |
| `juneau-core/juneau-marshall/.../BeanMeta.java` | `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanMeta.java` |
| `juneau-core/juneau-marshall/.../BeanPropertyMeta.java` | `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java` |
| `juneau-core/juneau-marshall/.../BeanPropertyValue.java` | `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyValue.java` |
| `juneau-core/juneau-marshall/.../BeanPropertyConsumer.java` | `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyConsumer.java` |
| `juneau-core/juneau-marshall/.../BeanProxyInvocationHandler.java` | `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanProxyInvocationHandler.java` |

### Blockers resolved

- **Blocker 1 (BeanPropertyValue.getClassMeta)** — Return type widened from `ClassMeta<?>` to `BeanTypeInfo<?>`. Marshalling-side call sites cast back to `ClassMeta<?>` where needed (~5 sites in serializer sessions).
- **Blocker 2 (BeanPropertyMeta.getBeanRegistry)** — Return type widened from `BeanRegistry` to `BeanRegistryLookup` (commons SPI). Marshalling-side call sites cast back to `BeanRegistry` where the concrete type is required.
- **Blocker 3 (BeanMeta.create signature)** — Factory method accepts `BeanTypeInfo<T>` instead of `ClassMeta<T>`. `ClassMeta<T>` still works as the argument because `ClassMeta extends BeanTypeInfo` (post Phase C Task 1).
- **Blocker 4 (BeanMeta.getMarshallingContext)** — Picked **option (a)** — renamed to a new `Object`-returning accessor that holds the marshalling-side resolver, fronted by `BeanTypeInfo.getMarshallingContext()`. Marshalling-side `ClassMeta` provides a covariant `MarshallingContext`-typed override so existing callers compile unchanged.
- **Blocker 5 (MarshalledBeanMetaInitializer / MarshalledPropertyPostProcessor SPI)** — Two new commons-side SPI interfaces:
  - `org.apache.juneau.commons.bean.BeanMetaInitializer` — with `buildBeanRegistry(...)`, `findTypeNameInParents(...)`, `resolveTypePropertyName(...)`, `findMarshalledTypeName(...)`, `hasBeanRegistrationAnnotation(...)`, `buildBeanFilter(BeanTypeInfo<?>)`. Has a `NOOP` default. `MarshalledBeanMetaInitializer` (marshalling-side) implements it and exposes `INSTANCE`.
  - `org.apache.juneau.commons.bean.BeanPropertyPostProcessor` — with `process(Object marshallingContext, Object builder)`. Has a `NOOP` default. `MarshalledPropertyPostProcessor` (marshalling-side) implements it and casts the inputs back to `MarshallingContext` / `BeanPropertyMeta.Builder` internally.
  - `BeanConfigContext` exposes both via `getBeanMetaInitializer()` / `getBeanPropertyPostProcessor()`. `MarshallingContext` wires the marshalling-side instances in via `BeanConfigContext.create()`.
  - `BeanMeta` calls `config.getBeanMetaInitializer().X(...)` and `config.getBeanPropertyPostProcessor().process(...)` instead of static helper calls.

### Build / test status

- `mvn clean compile -pl juneau-core/juneau-commons -am` — **GREEN** (standalone, no `juneau-marshall` dependency).
- `mvn clean install -DskipTests` — **GREEN** (all 16 modules build).
- `python3 scripts/test.py --test-only` — **GREEN** (full test suite passes).

### Visibility changes required by the move

Several previously-protected or package-private members had to be widened so that marshalling-side callers (and `juneau-marshall-rdf` extensions) can still reach them after the package boundary changed:

- `BeanMeta.BeanMetaValue` record made `public` (and its `optBeanMeta()` accessor).
- `BeanMeta` accessors `getConstructor()`, `getConstructorArgs()`, `hasConstructor()`, `newBean(Object)` widened `protected → public`.
- `BeanMap` constructor `(T, BeanMeta<T>)` and `setMarshallingSession(BeanSession)` widened `protected → public`.
- `BeanPropertyMeta.Builder` fields (`innerField`, `getter`, `setter`, `rawTypeMeta`, `swap`, `readTransform`, `writeTransform`, `dictionaryClasses`, `isUri`, `typeMeta`) widened package-private → `public` (consumed by `MarshalledPropertyPostProcessor`).
- `BeanPropertyValue.properties()` widened `protected → public` (consumed by `BeanPropertyValue_Test`).

### Import sweep counts (uncommitted working tree)

- 141 files updated in the automated sweep that added `import org.apache.juneau.commons.bean.X` for the 7 moved classes.
- 5 manual import fixups for files the sweep missed: `CsvParserSession`, `ParquetSerializerSession`, `YamlParserSession`, `ParquetSchemaBuilder`, `TomlParserSession`, plus two `juneau-marshall-rdf` parser sessions (`RdfParserSession`, `RdfStreamParserSession`) and one rest-server file (`RestRequest`).

### Follow-up checkpoints (not part of this move)

- Release notes / migration guide entries for the package move (`org.apache.juneau.Bean{Map,Meta,PropertyMeta,...}` → `org.apache.juneau.commons.bean.*`) in `juneau-docs/docs/pages/release-notes/9.5.0.md`.
- `package-info.java` for `org.apache.juneau.commons.bean` — verify it covers the new arrivals.
- Eventual: move `BeanRegistry` itself to commons (currently `BeanRegistry` implements `BeanRegistryLookup` from marshalling-side; deferred until type-resolution code is similarly decoupled).

---

## Status (Phase C Task 5 Step F — import cleanup landed, uncommitted)

**Step F audit follow-up — Tasks 1–8 LANDED in the working tree (uncommitted).** Build + full test green (`scripts/test.py --full`). The 7 bean-runtime files now import only JDK packages + `org.apache.juneau.commons.*` (plus `org.apache.juneau.BeanMeta.MethodType.*` static self-import on `BeanMeta`).

### Tasks completed in this checkpoint

- [x] **Task 1 — `@Marshalled` annotation reads lifted from `BeanMeta` to `MarshalledBeanMetaInitializer`.** Four call sites in `BeanMeta` (lines 226, 232, 453, 1178) now call `MarshalledBeanMetaInitializer.hasBeanRegistrationAnnotation(...)` or `MarshalledBeanMetaInitializer.resolveTypePropertyName(...)` instead of inlining the `ap.has(Marshalled.class, cm)` / `ap.find(Marshalled.class, classInfo).stream()...` reads. Javadoc references to `@Marshalled` in `BeanMeta` are now fully-qualified (`{@link org.apache.juneau.annotation.Marshalled @Marshalled}`).

- [x] **Task 2 — `@Name` annotation moved to `commons.bean`.** `git mv juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/Name.java juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/Name.java`; package declaration updated; Javadoc references to `@Named` are now fully-qualified. The associated builder/processor (`NameAnnotation.java`) does not exist — `@Name` has no `*Annotation` companion. The lone direct import in `ParameterInfo_Test.java` was updated; fully-qualified `@org.apache.juneau.annotation.Name(...)` references in the same test file were rewritten to `@org.apache.juneau.commons.bean.Name(...)`. Javadoc references in `commons.bean.BeanCtor`, `commons.bean.BeanProp`, and `commons.reflect.AnnotationInfo` were updated to point at the new location.

- [x] **Task 3 — `BeanPropertyMeta.setPropertyValue` parameter retyped to `BeanSession`.** Mechanical retype; `session.convertToType`, `session.convertToMemberType`, `session.parseToMap`, `session.parseToList` all exist on `BeanSession`. Caller in `set(...)` reads `m.getBeanSession()` (renamed in Task 5) and passes through.

- [x] **Task 4 — Defensive `(ObjectSwap) swap` cast removed from `setPropertyValue`.** Picked **option (a)**: deleted the belt-and-braces double-unswap branch entirely. The `writeTransform` already runs the unswap path at the top of `set(...)` (line ~1074), so by the time control reaches the cast site `value1` is already in its unswapped form. The else-branch is now an unconditional `session.convertToMemberType(bean, value1, rawTypeMeta)` call followed by `invokeSetter`. No tests broke — full suite green.

- [x] **Task 5 — `BeanMap.session` field retyped to `BeanSession`.** Field type changed from `MarshallingSession` to `BeanSession`. `setMarshallingSession(...)` parameter retyped to `BeanSession` (name preserved — `MarshallingSession implements BeanSession` so existing call sites `bm.setMarshallingSession(this)` in `MarshallingSession.java` and `DelegateBeanMap.java` are unchanged). Getter **renamed** to `getBeanSession()` returning `BeanSession`. The four call sites in `BeanPropertyMeta` (`session = m.getMarshallingSession()` → `session = m.getBeanSession()`) updated. The one external caller on `BeanMap` (`ParserSession.java` line 902) was left alone — turned out to be `JsonMap.getMarshallingSession()`, not `BeanMap.getMarshallingSession()`. Two narrow casts (`((Map) session.toBeanMap(b)).put(key, v)`) were added inside `BeanPropertyMeta.add(BeanMap, String, String, Object)` because `BeanSession.toBeanMap` returns `Object` (the `BeanMap` type itself lives in `juneau-marshall` until the physical move).

- [x] **Task 6 — `FilteredKeyMap` usage in `BeanMap.getProperties(String...)` replaced with JDK construct.** Picked **option (b)**: the only production caller of `FilteredKeyMap` was `BeanMap.getProperties(String...)` passing `null` as the `classMeta` argument. Replaced with an inline `AbstractMap` over an `ArrayList<Map.Entry>` whose entries delegate `getValue()` / `setValue(...)` back to `this` (live view, identical semantics to `FilteredKeyMap`'s entry behavior). `FilteredKeyMap` itself remains in `org.apache.juneau.internal` (still exercised by `FilteredMapTest` in `juneau-utest`); it is now orphaned in production code but staying put for now since it would need its own `ClassMeta → BeanTypeInfo` retype to move to `commons.collections`.

- [x] **Task 7 — `ParseException` / `SerializeException` catch in `BeanPropertyMeta.set(...)` retyped.** Picked **option (a)/(b) hybrid** — narrowed the catch from `ParseException` to `org.apache.juneau.commons.BasicRuntimeException`. Behavior preserved: `ParseException extends marshall.BasicRuntimeException extends commons.BasicRuntimeException`, so the previous wrapping behavior still fires for `ParseException` / `SerializeException` raised by `session.parseToMap` / `session.parseToList` / `writeTransform.apply`. `BeanRuntimeException` (which extends `RuntimeException`, NOT `BasicRuntimeException`) continues to propagate unwrapped exactly as before. `import org.apache.juneau.parser.*;` and `import org.apache.juneau.serializer.*;` removed from `BeanPropertyMeta`.

- [x] **Task 8 — Wildcard imports cleaned up in the 7 target files.** Per-file:
  - `BeanMap.java`: dropped `import org.apache.juneau.annotation.*;`, `import org.apache.juneau.internal.*;`, `import org.apache.juneau.swap.*;`. Javadoc refs to `MarshallingContext` / `Marshalled` / `ObjectSwap` / `Swap` fully-qualified.
  - `BeanMapEntry.java`: dropped `import org.apache.juneau.annotation.*;`, `import org.apache.juneau.swap.*;`. Javadoc refs to `ObjectSwap` / `Swap` fully-qualified.
  - `BeanMeta.java`: dropped `import org.apache.juneau.annotation.*;`. `BeanIgnore` and `BeanCtor` resolve through `import org.apache.juneau.commons.bean.*;`. Javadoc refs to `Marshalled` / `MarshalledProp` / `Bean#typeName()` etc. fully-qualified.
  - `BeanPropertyMeta.java`: dropped `import org.apache.juneau.annotation.*;`, `import org.apache.juneau.parser.*;`, `import org.apache.juneau.serializer.*;`, `import org.apache.juneau.swap.*;`. Javadoc refs to `MarshallingSession` / `ObjectSwap` / `MarshalledProp` / `MarshallingContext` / `ClassMeta` / `BeanRegistry` / `MarshalledPropertyPostProcessor` fully-qualified. `Builder.rawMetaType(ClassMeta<?>)` overload retyped to accept `BeanTypeInfo<?>` (the `ClassMeta` overload was unused in production code; the `Class<?>` overload is the only one BeanMeta calls).
  - `BeanPropertyValue.java`, `BeanPropertyConsumer.java`, `BeanProxyInvocationHandler.java`: already clean (no changes needed).

### Per-file move-readiness summary

After Tasks 1–8:

- ✅ **`BeanMapEntry.java`** — fully move-ready. Imports only `java.util.*`. No code-level `juneau-marshall` references.
- ✅ **`BeanPropertyConsumer.java`** — fully move-ready. Imports only `org.apache.juneau.commons.function.*`.
- ✅ **`BeanProxyInvocationHandler.java`** — fully move-ready. Imports only JDK + `commons.*`.
- ⚠️ **`BeanMap.java`** — imports clean, but a few code-level move-blockers remain:
  - `getMarshallingSession()` was renamed to `getBeanSession()` (returns `BeanSession`); the old name is gone. Callers updated.
  - `Map<String,BeanPropertyValue>` references (lines 292+) — `BeanPropertyValue` itself migrates with the cluster, so no issue.
- ⚠️ **`BeanPropertyValue.java`** — imports clean, but `getClassMeta()` still returns `ClassMeta<?>` (line 78). External callers (`BsonSerializerSession`, `MsgPackSerializerSession`, `CborSerializerSession`, `BeanMap`) consume the return type as `ClassMeta<?>`. Retype to `BeanTypeInfo<?>` would require casts at each call site. **Move-blocker — needs a follow-up checkpoint.**
- ⚠️ **`BeanPropertyMeta.java`** — imports clean, but code-level references remain:
  - `public BeanRegistry getBeanRegistry()` (line 883) returns marshalling-side `BeanRegistry`. Many callers (parser/serializer sessions). **Move-blocker — needs either `BeanRegistry` move to commons or wide return type.**
  - `Builder.rawMetaType(BeanTypeInfo<?>)` (was `ClassMeta<?>`) — now commons-friendly.
- ⚠️ **`BeanMeta.java`** — imports clean, but several code-level move-blockers remain:
  - `static <T> BeanMetaValue<T> create(ClassMeta<T> cm, ClassInfo implClass)` — `ClassMeta` parameter type.
  - `getMarshallingContext()` — returns `MarshallingContext`.
  - Many calls into `MarshalledBeanMetaInitializer.*` and `MarshalledPropertyPostProcessor.process(...)` — these helpers live in `org.apache.juneau` (marshalling-side). For the move, either: (a) move both helpers to commons (but they reference marshalling annotations/types internally), or (b) add SPI hooks on `BeanConfigContext` so the bean-side code can call into the marshalling helpers via the SPI seam.

### Build / test status

- `python3 scripts/test.py --build-only` — **GREEN**.
- `python3 scripts/test.py --full` — **GREEN**.

### Remaining work before the physical `git mv`

1. **`BeanPropertyValue.getClassMeta()` → `BeanTypeInfo<?>`** — retype, sweep ~5 call sites in serializer sessions to cast.
2. **`BeanPropertyMeta.getBeanRegistry()` → `Object` (or move `BeanRegistry` to commons)** — narrowing cast at call sites.
3. **`BeanMeta.create(ClassMeta<T>, ClassInfo)`** — accept `BeanTypeInfo<T>` instead; cast at call sites; or keep marshalling-side and add a parallel `BeanMeta.of(BeanTypeInfo<T>, ...)` factory.
4. **`BeanMeta.marshallingContext` field + `getMarshallingContext()` accessor** — field is already `Object`-typed; the accessor returns `MarshallingContext` via cast. For the move, expose only `Object` or a `BeanTypeResolver` accessor; marshalling-side callers cast.
5. **`MarshalledBeanMetaInitializer` / `MarshalledPropertyPostProcessor` SPI seams** — the cleanest path is to add hook methods on `BeanConfigContext` (e.g. `hasBeanRegistrationAnnotation(ClassInfo)`, `resolveTypePropertyName(ClassInfo)`, `findMarshalledTypeName(ClassInfo)`, `process(BeanPropertyMeta.Builder)`) so `BeanMeta` can call through the SPI without referencing the marshalling-side helpers directly. The marshalling-side `MarshallingContext` implementation delegates each hook to the corresponding helper. This is the bulk of the remaining structural work.

After items 1–5 land, the physical `git mv` should be a one-pass mechanical operation followed by a wide reference sweep.

---

## Status (as of `@MarshalledProp(properties)` removal, uncommitted)

**`@MarshalledProp(properties=...)` dropped per user direction (breaking change, v9.5).** With breaking changes authorized throughout TODO-5, the per-property child-property filter feature has been removed entirely. This shrinks the Phase C Task 5 surface meaningfully:

- `BeanPropertyMeta.applyChildPropertiesFilter(MarshallingSession, ClassMeta, Object)` — **gone**. The `MarshallingSession` + `ClassMeta` parameters that blocked the physical move are no longer present.
- `BeanPropertyMeta.swapAndFilterProperty(...)` — **inlined** into `getInner(...)` as a single `readTransform.apply(session, o)` call.
- `BeanPropertyMeta.newBeanMap(MarshallingSession, Object, BeanMetaFiltered)` private static helper — **gone**.
- `BeanPropertyMeta.properties` field + `getProperties()` accessor + `Builder.properties` field — **gone**.
- `BeanMetaFiltered` class — **deleted** (file removed). Its sole consumer (`DelegateBeanMap.getMeta()`) had its override removed too; `super.getMeta()` (raw `BeanMeta`) is what `entrySet()`/`keySet()` were already filtering against.
- `JsonSchemaGeneratorSession.getSchema(...)` — `pNames` parameter and the `BeanMetaFiltered` wrap-and-iterate path removed; the recursive nested-bean schema generation no longer carries an override-list.
- `MarshalledPropertyPostProcessor` — `ne(mp.properties()) → b.properties = split(...)` reads stripped from all three `(innerField, getter, setter)` annotation walks.
- `MarshalledProp.properties()` annotation attribute + `MarshalledPropAnnotation.Builder.properties(...)` setter + corresponding `Object#properties()` impl — **gone**.

**Phase C Task 5 gap inventory now shrunk (compared to the previous checkpoint):**

- One of the three `((MarshallingContext) bc).X()` casts in `BeanPropertyMeta` is gone (the `getBeanMeta(o.getClass())` cast inside `applyChildPropertiesFilter`). Two casts remain: `Builder.rawMetaType(Class<?>)` → `getClassMeta(value)`, and the constructor `ap` initialization → `getAnnotationProvider()`.
- The `BiFunction<MarshallingSession,Object,Object>` field types still need retyping to `BiFunction<BeanSession,Object,Object>` (or `BiFunction<Object,Object,Object>`), but with `applyChildPropertiesFilter` gone the only remaining `MarshallingSession`-typed parameter is on `setPropertyValue` — and that's the only place inside `BeanPropertyMeta` that still needs a marshalling-side session. The `swapAndFilterProperty` indirection is gone.
- `BeanMetaFiltered` is no longer one of the 8 target types; the cluster shrinks to **7**: `BeanMap`, `BeanMapEntry`, `BeanMeta`, `BeanPropertyMeta`, `BeanPropertyValue`, `BeanPropertyConsumer`, `BeanProxyInvocationHandler`.

Test changes:
- `MarshalledPropAnnotation_Test` — `.properties(...)` setter calls and `properties="e"` annotation attributes removed from `a1`/`a2`/`D1`/`D2`; `assertBean` field list trimmed from `description,dictionary,format,properties` to `description,dictionary,format`.
- `juneau-utest/src/test/java/org/apache/juneau/{json,json5,xml,html,uon,urlencoding}/Common_*Test.java` — all `a05_beanPropertyProperies` / `a06_beanPropertyPropertiesOnListOfBeans` tests and their `E1`/`E2`/`F`/`Test7b` helper classes deleted. 7 test files, 12 test methods deleted.
- `juneau-utest/src/test/java/org/apache/juneau/xml/Xml_Test.java` — `a10_elementNameOnBeansOfCollection` and its `J1`/`J2` helper classes deleted.

Docs:
- `juneau-docs/pages/topics/02.04.04.BeanpAnnotation.md` — `@MarshalledProp(properties)` paragraph + example removed.
- `juneau-docs/pages/release-notes/9.5.0.md` — new "Removed `@MarshalledProp(properties)` Attribute (breaking)" section under the `juneau-marshall` heading.

Build + full test green (`scripts/test.py --full`).

**Recommended next checkpoint:** resume Phase C Task 5 (physical `git mv` + reference sweep) with the simplified surface. The remaining work is still substantial (~20 annotation-read lifts, two `((MarshallingContext) bc).X()` casts, `BiFunction` retypes, `BeanProxyInvocationHandler` SPI routing, Javadoc cleanup), but the most awkward seam — `applyChildPropertiesFilter` with its `MarshallingSession`+`ClassMeta` parameters and the `BeanMetaFiltered` wrapper class — is gone.

---

## Status (Phase C Task 5 — Steps A-E1 landed, uncommitted)

**Phase C Task 5 Steps A-E1 LANDED in the working tree (uncommitted).** Build + targeted tests green. The bean-runtime cluster files are not yet moved (Steps G-J), but additional SPI decoupling is in place:

### Steps completed in this checkpoint

- **Step A — Two `((MarshallingContext) bc).X()` casts replaced with `BeanTypeResolver` SPI calls.**
  - `BeanPropertyMeta.Builder.bc` and `BeanPropertyMeta.bc` instance fields retyped from `Object` to `BeanTypeResolver`. `MarshallingContext` implements `BeanTypeResolver`, so the marshalling-side construction path is unchanged.
  - `Builder.rawMetaType(Class<?>)` now calls `bc.resolveType(null, info(value), null)` instead of `((MarshallingContext) bc).getClassMeta(value)`.
  - `BeanPropertyMeta` constructor `ap` initialization now reads `bc.getAnnotationProvider()` directly without a `MarshallingContext` cast.

- **Step B — `BiFunction<MarshallingSession,Object,Object>` retyped to `BiFunction<BeanSession,Object,Object>`.**
  - `BeanPropertyMeta.Builder.readTransform` / `writeTransform` fields plus `BeanPropertyMeta.readTransform` / `writeTransform` instance fields and the corresponding Builder setter signatures all retyped to use the `BeanSession` SPI.
  - Lambdas inside `MarshalledPropertyPostProcessor.installSwapAwareTransforms` now narrow `session` to `MarshallingSession` via a local `var ms = (MarshallingSession) session;` since `ObjectSwap.swap`/`ObjectSwap.unswap` require the marshalling-side session. Comment documents that the marshalling-side `BeanMap` always wires a `MarshallingSession`, so the narrowing cast is safe.

- **Step C — `BeanProxyInvocationHandler.equals` routed off `meta.getMarshallingContext().toBeanMap(arg)`.**
  - Replaced with `BeanMap.of(arg, (BeanMeta<Object>) BeanMeta.of(arg.getClass(), meta.getConfig()))` (the `BeanMap.of(T, BeanMeta<T>)` static factory shipped in Step 6).
  - Equality semantics preserved: builds a fresh `BeanMeta` against the same `BeanConfigContext`, then compares property maps. No session is wired; equality only reads.

- **Step D — `BeanMap.of(T)` static factory inlined.**
  - Replaced `MarshallingContext.DEFAULT_SESSION.toBeanMap(bean)` with `new BeanMap<>(bean, BeanMeta.of((Class<T>) bean.getClass()))`.
  - **Behavioral change:** The returned `BeanMap` no longer carries a `MarshallingSession`, so `ObjectSwap` transformations are not applied through `get`/`put`. The previous behavior was session-aware via `DEFAULT_SESSION`; the new behavior is bean-modeling-only. Verified by running `BeanMap_Test`, `BeanProxyInvocationHandler_Test`, `BeanMeta_Test` (all green) and the full `--build-only` check.

- **Step E1 — `@Uri` annotation reads lifted from `BeanPropertyMeta.Builder.validate()` to `MarshalledPropertyPostProcessor.process()`.**
  - The three `isUri |= ap.has(Uri.class, ifi/gi/si)` reads inside `validate()` are gone; the equivalent reads now live in the marshalling-side post-processor and update the same `b.isUri` flag (now package-private on the builder so the post-processor can write it).
  - The `rawTypeMeta.isUri()` reads remain in `validate()` — they are bean-modeling concerns (`BeanTypeInfo.isUri()` is the commons SPI).

### Build / test status

- `python3 scripts/test.py --build-only` — **GREEN** after each step.
- Targeted tests run (`BeanProxyInvocationHandler_Test`, `BeanMap_Test`, `BeanMeta_Test`, `transforms/BeanMap_Test`, `UriAnnotation_Test`) — all 86 tests green.
- Full test suite not re-run (per "incomplete-but-documented over broken-build" rule below).

### Step F audit — remaining juneau-marshall coupling in the 7 files

A full audit reveals the cluster is **not yet move-ready**. The 7 files still reference these `juneau-marshall` types:

**`BeanMap.java`** still depends on:
- `MarshallingSession` — held as the `private MarshallingSession session` field, returned from `getMarshallingSession()`, accepted by `setMarshallingSession(MarshallingSession)`. Used in `getBean(boolean)` for `session.convertToType(rawVal, cm)` (constructor-args path).
- `ClassMeta` — used as `var cm = pm.getClassMeta()` in `getBean(boolean)` and via `meta.getClassMeta()` in Javadoc/error formatting.
- `org.apache.juneau.annotation.*` (wildcard) — only used for Javadoc cross-references (`@Marshalled`, `@Swap`, `@MarshalledProp` etc.).
- `org.apache.juneau.internal.*` — used for `FilteredKeyMap` (instantiated in `keySet()`). FilteredKeyMap itself depends on `ClassMeta`.
- `org.apache.juneau.swap.*` — Javadoc-only references to `ObjectSwap`.

**`BeanMapEntry.java`** still depends on:
- `org.apache.juneau.annotation.*` — wildcard import. Need to audit whether any non-Javadoc reference survives.
- `org.apache.juneau.swap.*` — Javadoc-only references.

**`BeanMeta.java`** still depends on:
- `org.apache.juneau.annotation.*` — actively reads `@Marshalled` (lines 226, 232, 453, 1178) for bean detection, `typePropertyName`, and constructor-visibility relaxation. Reads `@Name` (~6 sites) for property-name resolution.
- `Marshalled` annotation reads still need lifting into `MarshalledBeanMetaInitializer` (helper methods that take the `AnnotationProvider` + `ClassInfo` and return: (1) `isBean(...)`, (2) `typePropertyName(...)`, (3) `allowsPrivateConstructor(...)`).
- `@Name` could either move to `commons.bean` (clean but ~26 files would need import updates) or stay in `juneau-marshall.annotation` and have its reads abstracted behind a marshalling-side helper.
- `MarshalledBeanMetaInitializer.findMarshalledFilter(cm)` and `.classInfoOf(cm)`, etc. — already in place; bean-side path works without them.
- `MarshalledPropertyPostProcessor.process((MarshallingContext) marshallingContext, p)` — called via cast; `marshallingContext` field is `Object`-typed already. The post-processor itself stays in juneau-marshall.
- `MarshallingContext` — referenced only via the cast above plus three Javadoc cross-references.
- `BeanRegistry` — narrowing cast inside `getBeanRegistry()` and `getPropertyBeanRegistry()`. Could stay marshalling-side-only since the bean-side type is the wider `BeanRegistryLookup`.
- `MarshalledFilter` — narrowing cast inside `getMarshalledFilter()`. Same pattern.
- `BeanProxyInvocationHandler` — instantiated in the `beanProxyInvocationHandler` supplier. Both types move together.

**`BeanPropertyMeta.java`** still depends on:
- `MarshallingSession` — parameter type on the private `setPropertyValue(BeanMap<?>, String, Object, Object, boolean, boolean, MarshallingSession)`. Method is invoked from `set(...)` with `session = m.getMarshallingSession()`. Retyping to `BeanSession` is mechanically straightforward (the `session.parseToMap` / `session.parseToList` / `session.convertToType` / `session.convertToMemberType` methods are already on `BeanSession`), but **the method body still casts `swap` to `ObjectSwap` at line 1249** for the defensive double-unswap branch and instantiates new collections via `BeanInstantiator.of(Map.class).type(rawTypeMeta)...`.
- `ObjectSwap` — narrowing cast at line 1249 (`((ObjectSwap) swap).getSwapClass()`). Need to either: (a) extract this branch into a marshalling-side helper, (b) add a separate `Class<?> swapClass` field on the builder to avoid the cast, or (c) accept the cast lives at a narrow site that can stay `Object`-typed plus runtime reflection on a method reference.
- `BeanInstantiator` — used in two sites inside `setPropertyValue` to build empty collections/maps. Lives in `commons.inject`, so it's already commons-side. **Re-checked: `BeanInstantiator` is in `org.apache.juneau.commons.inject`, NOT marshalling-side. No action needed.**
- `ParseException` / `SerializeException` — caught/thrown inside `set`/`setPropertyValue`. Both live in `juneau-marshall.parser` / `juneau-marshall.serializer`. Move-blocker.
- `BeanRegistry` — narrowing cast inside `getBeanRegistry()`. Stays marshalling-side.
- `ClassMeta` — Javadoc cross-references only.
- `Uri` — fully lifted (Step E1).
- `Name` — Javadoc-only references after Step E1. Worth confirming with a grep before move.

**`BeanPropertyValue.java`** — clean. Only `commons.*` imports.

**`BeanPropertyConsumer.java`** — clean. Only `commons.*` imports.

**`BeanProxyInvocationHandler.java`** — clean. Only commons static imports after Step C.

### Why the move is not yet safe (summary)

The two stubborn move-blockers are:

1. **`BeanMap.session` typed as `MarshallingSession`** and the `getBean(boolean)` constructor-args path that calls `session.convertToType`. The field could be retyped to `BeanSession`, the public `getMarshallingSession()` removed/renamed to `getBeanSession()`, and the one external caller (`ParserSession` at line 902) adjusted to cast. That's still 20-30 lines of mechanical work but introduces a public-API breaking change to `BeanMap`.
2. **`BeanPropertyMeta.setPropertyValue(..., MarshallingSession session)`** retains an `(ObjectSwap) swap` cast inside the function body. Cleanest fix: capture the swap class as a side-data on the builder when `installSwapAwareTransforms` runs, replacing the cast with a `Class<?>` comparison. Mechanical but adds one more SPI field.

Plus the secondary work:

3. **`BeanMeta` `@Marshalled` reads** — three call sites need lift-out to `MarshalledBeanMetaInitializer`. Mechanical.
4. **`@Name` handling** — either move to `commons.bean` (clean but ~26 file import-fixups) or hide behind a marshalling-side helper (extra plumbing for property-name resolution).
5. **`FilteredKeyMap` relocation** — move to `commons.collections`, retype its `classMeta` field to `BeanTypeInfo`.
6. **`ParseException` / `SerializeException` references in `BeanPropertyMeta`** — replace with `RuntimeException` rethrows or a commons-side `BeanRuntimeException` wrapper.

### Next step recommendation

Pick up the remaining SPI cleanup as a fresh checkpoint focused on items 1-6 above. The cluster is much closer to move-ready than it was; the heavy lifts from Phase 5a/8a/8b are all in place. After the remaining items land, Steps G-J (the physical `git mv` + reference sweep + standalone compile verification) should be a couple of hours of mechanical work plus the import-fix wave.

---

## Status (as of Phase C Tasks 1-2-3-4-4-deferred checkpoint, uncommitted)

**Phase C Tasks 1, 2, 3, 4, 4-deferred LANDED (working tree, uncommitted).** Build + full test green. See "Phase C status" block under Step 8b-ii for full detail. Summary:
- **Task 1** — Public getter widening (`getClassMeta()` → `BeanTypeInfo<?>`, `getBeanRegistry()` → `BeanRegistryLookup`) committed locally at `48621576d7`.
- **Task 2** — `validate(...)` body lift-out via new `BeanTypeResolver` SPI committed locally.
- **Task 3** — `BeanMeta` constructor body lift-out via new `MarshalledBeanMetaInitializer` (this checkpoint). Includes relocation of `installSwapAwareTransforms` from `BeanMeta` into `MarshalledPropertyPostProcessor`, retype of `BeanMeta.marshallingContext` field to `Object`, and removal of `parser.*`/`serializer.*`/`swap.*` imports from `BeanMeta`.
- **Task 4** — `setPropertyValue` Collection-branch JsonList wrap → ArrayList wrap (committed earlier).
- **Task 4-deferred** — CharSequence-parsing sites now route through new `BeanSession.parseToMap(CharSequence)` / `BeanSession.parseToList(CharSequence)` SPI methods (this checkpoint). `BeanPropertyMeta` dropped its `import org.apache.juneau.collections.*`.

**Phase C Task 5 (physical `git mv` + reference sweep) — NOT STARTED.** Survey revealed the remaining marshalling-side coupling on the 8 files is deeper than the original prep scope estimated. The bulk of remaining cleanup is lifting ~20 marshalling-side annotation reads (`@Marshalled`, `@BeanProp`, etc.) from `BeanMeta`/`BeanPropertyMeta` to `MarshalledBeanMetaInitializer`, plus retyping `BiFunction<MarshallingSession,...>` field types to use the `BeanSession` SPI seam, plus relocating `applyChildPropertiesFilter` to the marshalling side. Estimated as a fresh checkpoint pass focused exclusively on Task 5 prep + the physical move + ~80-120-file reference sweep.

**Step 8a complete (SPI-seam extraction).** Commit `3a74fcd50a`. The minimum SPI surface that the 8 target types need from the marshalling layer is now in place:

- New SPI: `org.apache.juneau.commons.bean.BeanSession`. Captures the session-aware operations (`convertToType`, `convertToMemberType`, `toBeanMap`) that `BeanMap`/`BeanPropertyMeta` need from `MarshallingSession`. The `targetType` parameters are typed as `Object` so `commons.bean` does not need to import `ClassMeta`. The `toBeanMap` method is generic (`<T> Object toBeanMap(T bean)`) so `MarshallingSession`'s existing covariant `<T> BeanMap<T> toBeanMap(T)` satisfies it without a bridge method.
- `MarshallingSession` now implements `BeanSession`. Two new bridge methods (`convertToType(Object,Object)` / `convertToMemberType(Object,Object,Object)`) dispatch `Object`-typed `targetType` arguments into the existing `ClassMeta` / `Class` typed overloads. `IllegalArgumentException` is thrown for unsupported `targetType` kinds.
- `BeanConfigContext` now carries four additional bean-modeling settings that previously only lived on `MarshallingContext`: `beanMapPutReturnsOldValue`, `ignoreInvocationExceptionsOnGetters`, `ignoreInvocationExceptionsOnSetters`, `ignoreUnknownNullBeanProperties`. Defaults match `MarshallingContext`'s historical defaults. Builder, copy() and DEFAULT are wired through.
- `MarshallingContext.buildBeanConfigContext()` populates the four new settings from its own resolved values, so behavior is unchanged.
- `BeanPropertyMeta` reads those four settings through `config.isX()` instead of `bc.isX()`. The corresponding `nn(bc) && bc.isX()` guards from Step 6 are simplified to unconditional `config.isX()` reads.
- `BeanConfigContext_Test` extended to exercise the new defaults, builder setters, and copy() preservation.

What the 8 target types still pull from `juneau-marshall` (left for Step 8b's SPI work + physical move):
1. `ClassMeta` — used as the type metadata for property `rawTypeMeta` / `typeMeta`, the `BeanMeta.classMeta` field, the `applyChildPropertiesFilter` parameter, and `BeanMap.getBean()`'s Optional-init / constructor-args paths. Needs either: a `BeanTypeInfo` SPI typed in `commons.bean`, or `Object`-typed field with marshalling-side narrowing, or migration of consumption to marshalling-side helpers.
2. `BeanRegistry` — used by `BeanMeta`'s side-map (`Map<BeanPropertyMeta,BeanRegistry> propertyBeanRegistries`) and per-property registry construction inside `BeanMeta` from `Builder.dictionaryClasses`. Needs a `BeanRegistryLookup` SPI (or migrate the side-map to the marshalling-side).
3. `ObjectSwap` — referenced by `BeanPropertyMeta.swap` field and `BeanMap.getBean()`'s Optional handling. The actual swap/unswap calls already route through `readTransform`/`writeTransform` (Step 3); the field itself is just metadata for the defensive double-unswap check. Needs either: a `BeanPropertySwap` SPI, or move the field to a side-map on `BeanMeta`.
4. `MarshalledFilter` — used by `BeanMeta` (composed via `getMarshalledFilter()`), by `BeanMetaFiltered` (passes through to `BeanMeta`'s old constructor), and by `MarshalledProp` annotation reads inside `BeanPropertyMeta.Builder.validate()`. Likely stay-behind on the marshalling side; `BeanMetaFiltered` migrates with `BeanMeta` and the `MarshalledFilter` parameter on the old constructor stays `Object`-typed (or moves to a marshalling-side wrapper).
5. `MarshalledProp` annotation — read in `BeanPropertyMeta.Builder.validate()` to detect filtered properties. Marshalling-side annotation; needs either a `commons.bean` mirror or be lifted out of `validate()` into a marshalling-side post-processor.
6. `Json5Serializer` — used by `BeanProxyInvocationHandler.toString()` only. Trivial to replace with a `BeanSession`-style hook or just inline minimal JSON5-ish formatting.
7. `JsonMap` / `JsonList` — used inside `BeanPropertyMeta`/`BeanMap` for collection/map building during set operations. Likely route through `BeanSession.toBeanMap`/converter callbacks, or accept `Map`/`List` instead.

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
- [x] **Step 8a** — SPI seams in `commons.bean` (commit `3a74fcd50a`). `BeanSession` interface created, `MarshallingSession` implements it, four marshalling-only-historically settings migrated to `BeanConfigContext` so `BeanPropertyMeta`/`BeanMap` can stop reading them through `MarshallingContext`. See "Status (as of Phase 5h checkpoint)" above for full detail.
- [x] **Step 8b-i** — Foundational SPI extraction (uncommitted, working tree). Three new SPI seams and one type relocation, all build- and test-green:
  - **`BeanTypeInfo<T>`** in `commons.bean` (abstract class extending `ClassInfoTyped<T>`). Declares the bean-modeling-side type-classification surface needed by the 8 target types: `isUri()`, `isOptional()`, `isBean()`, `isObject()`, `getElementType()`, `getKeyType()`, `getValueType()`, `canCreateNewInstance()`, `canCreateNewInstance(Object)`, `newInstance()` (throws `ExecutableException` from commons), `getOptionalDefault()` (returns `Object` because marshalling-side `ClassMeta.getOptionalDefault()` returns `Optional<?>` and Java does not allow narrowing `Optional<?>` to `T`). `ClassMeta<T>` now extends `BeanTypeInfo<T>` instead of `ClassInfoTyped<T>` directly — every existing concrete method on `ClassMeta` satisfies the new abstract surface via covariant returns and matching signatures (no implementation changes were needed in `ClassMeta` itself).
  - **`BeanFilter`** interface in `commons.bean`. Declares the per-class filter surface (property includes/excludes, read/write-only sets, propertyNamer, beanDictionary, interfaceClass/stopClass/implClass, typeName, example, fluentSetters flag, unsortedProperties flag, plus `readProperty`/`writeProperty` BeanInterceptor wrappers). `MarshalledFilter` now `implements BeanFilter` — no source changes inside `MarshalledFilter` were needed (every method on `BeanFilter` already existed on `MarshalledFilter`). `BeanMeta.beanFilter` field is retyped from `MarshalledFilter` to `BeanFilter`; both protected and private BeanMeta constructors retype the `bf` parameter accordingly. Public `getMarshalledFilter()` retained on `BeanMeta` (single in-tree caller in `ClassMeta`) — narrows the SPI-typed field back to `MarshalledFilter` via a safe cast (only concrete implementation in-tree).
  - **`BeanRegistryLookup`** interface in `commons.bean` (minimal surface: `String getTypeName(Class<?>)` and `boolean hasName(String)`). `BeanRegistry` implements it. The `Map<BeanPropertyMeta,BeanRegistry>` side-map on `BeanMeta` is unchanged for now (retyping it to `BeanRegistryLookup` would force casts on the 3 marshalling-side callers in `ParserSession`/`SerializerSession`/`XmlParserSession` that call `BeanRegistry`-only methods like `getClassMeta(typeName)` on the result — deferred to Step 8b-ii).
  - **`Delegate<T>` moved** from `org.apache.juneau` to `org.apache.juneau.commons.bean` via `git mv`. The interface's `getClassMeta()` method now returns `BeanTypeInfo<T>` instead of `ClassMeta<T>`. All five in-tree `Delegate` implementations (`BeanMap`, `DelegateBeanMap`, `DelegateList`, `DelegateMap`, `FilteredKeyMap`) keep their `ClassMeta<T>` return types via Java covariant returns (no source changes needed in those classes). Five marshalling-side files that used the raw-typed `(Delegate)o).getClassMeta()` idiom (Html/XML/RDF serializer sessions) added `import org.apache.juneau.commons.bean.*;` and an explicit `(ClassMeta)` / `(ClassMeta<?>)` cast on the call site (5 modified files, ~10 line delta).
  - **Build/test verification.** `python3 scripts/test.py --full` passed clean (juneau + juneau-rest + juneau-microservice + examples + utests, ~70k tests). `cd juneau-core/juneau-commons && mvn clean compile` passes standalone — `juneau-commons` still compiles without depending on `juneau-marshall`. `ReadLints` clean on all modified files.
- [ ] **Step 8b-ii** — Continuation of the bean-runtime move. Remaining work to physically relocate the 8 target files (`BeanMap`, `BeanMapEntry`, `BeanMeta`, `BeanMetaFiltered`, `BeanPropertyMeta`, `BeanPropertyValue`, `BeanPropertyConsumer`, `BeanProxyInvocationHandler`) into `commons.bean`. The SPI seams from 8b-i are in place; this step is the field-retype + cross-module-leakage cleanup pass:

  **Phase A status (uncommitted, working tree) — COMPLETE, build + full test green (49,912 tests pass).**

  Lift-out items landed:
  - **Sub-item 12 (`throws ParseException` declaration on `set`)** — removed the `throws ParseException` from the private `setPropertyValue` helper inside `BeanPropertyMeta`. `ParseException` is a `RuntimeException` so the declaration was documentation-only; the catch in `set(...)` already wraps it via `bex(e2)`.
  - **Sub-item 9 (`Json5Serializer.DEFAULT.toString(...)`)** — `BeanMap.getBean(true)` error message now uses `Arrays.toString(getClasses(args))`; `BeanProxyInvocationHandler.toString()` now uses `Objects.toString(this.beanProps)`. Both files dropped the `import org.apache.juneau.json5.*;`. The two remaining substring-based test assertions still match because both still produce a bracketed list of property/class names.
  - **Sub-item 13 (`Surrogate.class` reference in `swapSwap`)** — moved out of `BeanPropertyMeta.Builder` along with the rest of the `@Swap` annotation logic (see sub-items 4 + 14 below). The new home is a marshalling-side post-processor that retains the `unsupportedOp("TODO - Surrogate swaps not yet supported on bean properties.")` guard.
  - **Sub-items 4 + 14 (`@MarshalledProp` annotation reads + `StringFormatSwap` instantiation)** — created new marshalling-side helper `MarshalledPropertyPostProcessor` in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java`. It owns the `marshalledPropSwap(AnnotationInfo<MarshalledProp>)` and `swapSwap(AnnotationInfo<Swap>)` helpers that used to live as `private static` methods on `BeanPropertyMeta.Builder`, plus the `forEach(@MarshalledProp ...)` / `findFirst(@Swap ...)` walks across `field` / `getter` / `setter` that used to live inside `Builder.validate(...)`. The post-processor reads from `BeanPropertyMeta.Builder`'s package-private fields (`typeMeta`, `properties`, `swap`, `dictionaryClasses`, plus the `bc`, `field`, `getter`, `setter`, `name` reads via package access) and writes back to the same fields. `BeanMeta.validateAndRegisterProperty(...)` calls `MarshalledPropertyPostProcessor.process(marshallingContext, p)` exactly once per property, immediately after `p.validate(...)` succeeds and before `installSwapAwareTransforms(p)`, but only when `marshallingContext != null` (commons-side path skips this entirely — the per-property registry side-map likewise stays unchanged on the bean-modeling-only path). `BeanPropertyMeta.Builder.typeMeta` and `properties` flipped from `private` to package-private, and the `dictionaryClasses` field is now initialized to an empty `list()` inside `validate(...)` before the post-processor runs (the post-processor `addAll(...)`s any additional `@MarshalledProp(dictionary)` entries it finds). The `org.apache.juneau.swaps.*`, `org.apache.juneau.annotation.Swap`, and `org.apache.juneau.annotation.MarshalledProp` imports are gone from `BeanPropertyMeta.java`.
  - **Sub-item 10 (`JsonList` / `JsonMap` → JDK collections)** — partial. The `propertyCache` allocations inside `BeanPropertyMeta.add(BeanMap,String,Object)` and `add(BeanMap,String,String,Object)` now use `new ArrayList<>()` / `new LinkedHashMap<>()` (test-invisible — they're internal caches for read-only beans). The `c = new JsonList(session)` / `map = new JsonMap(session)` fallback constructions in `add(...)` likewise flipped to `new ArrayList<>()` / `new LinkedHashMap<>()`. **Deferred to Phase C** (test-observable behavior change): `setPropertyValue(...)` still creates a `new JsonList(valueList)` in the abstract-Collection branch — tests `BeanMap_Test.a05_arrayProperties`, `a06_arrayProperties_usingConfig`, `a09_beanPropertyAnnotation` assert that the field's concrete class is `JsonList`. Similarly, the two `JsonMap.ofJson(...)` / `new JsonList(value2).setBeanSession(session)` calls that parse a `CharSequence` value inside `setPropertyValue(...)` stay — those are the marshalling-side JSON parser entry points and should move to a marshalling-side conversion helper as part of Phase C (when these flip to plain `LinkedHashMap`/`ArrayList`, the tests will need to update their expected class names too).
  - **Sub-item 8 (`BeanMap.load(...)` → `BeanMapLoader`)** — created `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanMapLoader.java` with `static <T> BeanMap<T> load(BeanMap<T> m, String input) throws ParseException` and `static <T> BeanMap<T> load(BeanMap<T> m, Reader r, ReaderParser p) throws ParseException, IOException`. Removed `BeanMap.load(Reader, ReaderParser)` and `BeanMap.load(String)` from `BeanMap.java`. Updated 2 call sites in `MarshallingContext.java` (`bs.newBeanMap(toMeta.inner()).load(in.toString()).getBean()` → `BeanMapLoader.load(bs.newBeanMap(toMeta.inner()), in.toString()).getBean()`). Updated 9 call sites across 2 test files (`BeanMap_Test.java`, `Annotations_Test.java`) similarly. `BeanMap.load(Map)` (a plain `putAll`-style fluent helper) stays on `BeanMap` — no marshalling deps. `BeanMap.java` lost its `java.io.*`, `org.apache.juneau.collections.*`, and `org.apache.juneau.parser.*` imports.

  **Phase C status (uncommitted, working tree) — Tasks 1, 2, 3, 4, 4-deferred LANDED, build + full test green. Task 5 NOT STARTED (deeper coupling discovered).**

  Phase C-tasks-1-2-3-4-and-4-deferred landed:
  - **Task 1 (public getter widening) — COMPLETE.** `BeanPropertyMeta.getClassMeta()` now returns `BeanTypeInfo<?>` (was `ClassMeta<?>`). `BeanMeta.getClassMeta()` now returns `BeanTypeInfo<T>` (was `ClassMeta<T>`). `BeanMeta.getBeanRegistry()` and `BeanMeta.getPropertyBeanRegistry(BeanPropertyMeta)` now return `BeanRegistryLookup` (was `BeanRegistry`). `BeanMap.getClassMeta()` now returns `BeanTypeInfo<T>` (was `ClassMeta<T>`). The cascade across ~30 marshalling-side files needed explicit `(ClassMeta<?>)` and `(BeanRegistry)` casts at the call sites where `ClassMeta`-only / `BeanRegistry`-only methods are invoked (`getSerializedClassMeta(this)`, `getMarshallingContext()`, `getNameProperty()`, `getParentProperty()`, `getValueType()`-as-`ClassMeta`, `newInstance(Object outer)`, `getClassMeta(typeName)`, `parseCellValue`, `convertToType`, `cast`, etc.). Files touched: `BeanPropertyMeta`, `BeanMeta`, `BeanMap`, `BeanPropertyValue`, `ClassMeta`, and 26 marshalling-side session/parser/serializer files across `bson`, `cbor`, `csv`, `hjson`, `hocon`, `html`, `ini`, `jcs`, `json`, `jsonschema`, `markdown`, `msgpack`, `parquet`, `proto`, `toml`, `uon`, `urlencoding`, `xml`, `yaml`, `jena` (`juneau-marshall-rdf`), `objecttools/ObjectRest`, `collections/JsonMap`, plus `juneau-rest-server/BeanDescription`. Build + full test green.
  - **Task 2 (`validate(...)` body lift-out) — COMPLETE (signature seam).** Introduced new SPI `BeanTypeResolver` in `org.apache.juneau.commons.bean` (3-method interface: `resolveType(AnnotationInfo<BeanProp>, ClassInfo, TypeVariables)`, `objectType()`, `getAnnotationProvider()`). `MarshallingContext` now `implements BeanTypeResolver` and exposes two new public bridge methods (`resolveType` delegating to the existing protected `resolveClassMeta`, and `objectType` returning `cmObject`). `BeanPropertyMeta.Builder.validate(...)` parameter retyped from `MarshallingContext bc` → `BeanTypeResolver bc`. The four `bc.resolveClassMeta(...)` calls inside the validate body switched to `bc.resolveType(...)`; the two `bc.object()` calls switched to `bc.objectType()`. The single caller (`BeanMeta.validateAndRegisterProperty(...)`) passes the same `marshallingContext` instance — Java picks up the `BeanTypeResolver` interface implementation automatically (no cast required). Build + full test green. **Note:** the `Builder.bc` field stays `Object`-typed (from Phase B). Three other marshalling-only seams on `BeanPropertyMeta` remain (cast `((MarshallingContext) bc).getClassMeta(value)` inside `rawMetaType(Class<?>)`, `((MarshallingContext) bc).getAnnotationProvider()` inside the constructor, `((MarshallingContext) bc).getBeanMeta(o.getClass())` inside `applyChildPropertiesFilter`); those follow a similar lift-out pattern in a future pass.
  - **Task 3 (`BeanMeta` constructor body lift-out) — COMPLETE.** New marshalling-side helper `MarshalledBeanMetaInitializer` in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledBeanMetaInitializer.java`. It owns the marshalling-aware helpers that used to live as private code paths inside `BeanMeta`'s constructor and helpers: `buildBeanRegistry(Object marshallingContext, BeanFilter, ClassInfo, BeanConfigContext)` (replaces `findBeanRegistry()` body), `buildPropertyBeanRegistry(Object marshallingContext, BeanRegistryLookup parent, List<ClassInfo>)` (replaces the inline `new BeanRegistry(marshallingContext, ...)` inside the property iteration loop), `findTypeNameInParents(Object marshallingContext, ClassInfo, Class<?>)` (replaces the parents-and-interfaces stream in `findDictionaryName`), `findMarshalledTypeName(BeanConfigContext, ClassInfo)` (the `@Marshalled(typeName)` annotation read used at the tail of `findDictionaryName`), plus three static extractors (`configOf`, `contextOf`, `classInfoOf`) used by the protected `BeanMeta(BeanTypeInfo<T>, BeanFilter, String[], ClassInfo)` constructor to obtain the marshalling-side context objects from a `BeanTypeInfo<T>` parameter without referencing `ClassMeta` directly, and `findMarshalledFilter(ClassMeta)` (moved verbatim out of `BeanMeta.create`). The `installSwapAwareTransforms(BeanPropertyMeta.Builder)` method moved from `BeanMeta` into `MarshalledPropertyPostProcessor` and is now invoked from inside `process(MarshallingContext, BeanPropertyMeta.Builder)` (gated by `nn(marshallingContext)` — the commons-side construction path skips the post-processor entirely). `BeanMeta.marshallingContext` field retyped from `final MarshallingContext` to `final Object` so the field can live in `commons.bean` without referencing `MarshallingContext`; the public `getMarshallingContext()` getter casts back. `BeanMeta` dropped its `parser.*`, `serializer.*`, `swap.*` imports as a result. Build + full test green.
  - **Task 4 (`new JsonList(valueList)` → `new ArrayList<>(valueList)`) — COMPLETE.** `BeanPropertyMeta.setPropertyValue`'s abstract-Collection branch (the path that builds a typed collection by copying then converting elements) now constructs `new ArrayList<>(valueList)` instead of `new JsonList(valueList)`. Three `BeanMap_Test` assertions (`a05_arrayProperties`, `a06_arrayProperties_usingConfig`, `a09_beanPropertyAnnotation`) updated to expect `ArrayList` for the corresponding `lb1`/`l3`/`m3` properties. Critically, the wrapping stays gated by `if (! elementType.isObject())` — when the element type is `Object` (raw `List`/`Collection` fields), the original collection (often a `JsonList`) is passed through unchanged. Tests `a03_collectionFieldProperties` and `a04_collectionMethodProperties` validate that the existing `JsonList`-pass-through behavior is preserved for `l1`/`c1`/`jl1`/`l2`/`m2`/`hm2`/`jm2`/`jl2` (raw `List`/`Collection`/`Map`/`HashMap`/`JsonMap`/`JsonList` fields). Build + full test green.
  - **Task 4-deferred (CharSequence-parsing sites in `setPropertyValue`) — COMPLETE.** Added two new methods to the `BeanSession` SPI in `commons.bean`: `Map<?,?> parseToMap(CharSequence)` and `Collection<?> parseToList(CharSequence)`. `MarshallingSession` implements both as bridge methods that delegate to `JsonMap.ofJson(value).session(this)` / `new JsonList(value).setBeanSession(this)` respectively (the original inline calls). `BeanPropertyMeta.setPropertyValue`'s two `CharSequence` branches (Map branch line 1147, Collection branch line 1205) now invoke `session.parseToMap(value21)` / `session.parseToList(value2)`. The `import org.apache.juneau.collections.*` was dropped from `BeanPropertyMeta` — the file no longer references `JsonMap`/`JsonList` directly. Build + full test green.

  **Phase C Task 5 status — NOT STARTED.** Surveying the 8 target files in light of the lifts from Tasks 1-4-deferred revealed that the remaining marshalling-side coupling is deeper than the original Task 5 prep scope. To be done in a follow-up pass; this checkpoint stops at "all SPI extractions + helper lifts done; physical move blocked on deeper coupling cleanup".

  Inventory of what's still blocking the physical `git mv` (remaining marshalling-side surface area on the 8 target files):

  - **`BeanPropertyMeta`** — still imports `annotation.*`, `internal.*`, `parser.*`, `serializer.*`, `swap.*`.
    - `org.apache.juneau.annotation.*` — Javadoc references to `@MarshalledProp`, `@Marshalled`, plus `AnnotationInfo`/`AnnotationProvider` (those live in `commons.reflect` though — confirm).
    - `org.apache.juneau.internal.*` — uses `TypeVariables`/`ClassInfo` helpers (`TypeVariables` is in `internal`, check it's not commons-reachable).
    - `org.apache.juneau.parser.ParseException` (catch on line 1120 of `set(...)`).
    - `org.apache.juneau.swap.ObjectSwap` (cast on line 1259 of `setPropertyValue`'s defensive double-unswap check).
    - `MarshallingSession`-typed parameter on `setPropertyValue`, `applyChildPropertiesFilter(MarshallingSession, ClassMeta, Object)`, `swapAndFilterProperty(MarshallingSession, Object)`, plus `BiFunction<MarshallingSession,Object,Object>` field types on `readTransform`/`writeTransform` and `Builder.readTransform`/`Builder.writeTransform` setter parameters.
    - Three remaining `((MarshallingContext) bc).X()` casts: `Builder.rawMetaType(Class<?>)` (`getClassMeta(value)`), the constructor's `ap` initialization (`getAnnotationProvider()`), and `applyChildPropertiesFilter` (`getBeanMeta(o.getClass())`).
    - `applyChildPropertiesFilter` signature: `(MarshallingSession session, ClassMeta cm, Object o)` — both `MarshallingSession` and `ClassMeta` are marshalling-side; lifting this method to a marshalling-side post-processor (or accepting an `Object`-typed signature with marshalling-side narrowing) is the cleanest move.
    - `newBeanMap(MarshallingSession session, Object o, BeanMetaFiltered meta)` private static helper — `BeanMetaFiltered` is one of the 8 (moves with cluster); `MarshallingSession` is the blocker.
    - `BeanMeta#installSwapAwareTransforms` — already moved to `MarshalledPropertyPostProcessor`, but `BeanPropertyMeta.Builder` still has package-private `swap` / `rawTypeMeta` fields read from `MarshalledPropertyPostProcessor` (cross-package access becomes a problem after the move).
  - **`BeanMeta`** — still imports `annotation.*`.
    - `@Marshalled`, `@BeanType`, `@BeanProp`, `@BeanCtor`, `@BeanIgnore`, `@Transient`, `@Name`, `@Uri` annotation reads scattered across `create(...)`, the constructor body (line ~453: `ap.find(Marshalled.class, classInfo)`), `findBeanConstructor` (`ap.has(BeanType.class, cm)` / `ap.has(Marshalled.class, cm)`), `findFluentSetters` (uses `@Marshalled`), `findClassFieldMeta` (uses `@BeanProp`, `@Name`, `@BeanIgnore`, `@Transient`), `findGetters`/`findSetters` (uses `@BeanProp`, `@Name`, `@BeanIgnore`), `findInnerBeanField` (uses `@BeanProp`). Lifting all of these out to `MarshalledBeanMetaInitializer` (or moving the bean-modeling-relevant ones like `@BeanType`/`@Name`/`@Uri` to `commons.bean`) is the bulk of the remaining Task 3-style work.
    - `MarshalledFilter` (`beanFilter` is typed `BeanFilter` per Step 8b-i but `MarshalledFilter` cast surfaces in `getMarshalledFilter()`).
    - `create(ClassMeta<T>, ClassInfo)` static factory — uses `ClassMeta`/`MarshallingContext.getAnnotationProvider()`; could relocate to `MarshalledBeanMetaInitializer` as `MarshalledBeanMetaInitializer.create(...)` returning `BeanMetaValue<T>`.
  - **`BeanMap`** — still imports `annotation.*`, `internal.*`, `swap.*`.
    - `@Marshalled` Javadoc reference and the `BeanMap.of(T)` factory which routes through `MarshallingContext.DEFAULT_SESSION.toBeanMap(bean)` — easiest cleanup is to delete `BeanMap.of(T)` and update callers to use `MarshallingContext.DEFAULT_SESSION.toBeanMap(bean)` directly (callers in `juneau-assertions` need updating; `juneau-assertions` already depends on `juneau-marshall`).
    - `ObjectSwap` cast inside `BeanMap.getBean()` Optional handling.
    - `org.apache.juneau.internal.*` — likely `ClassInfo` and `TypeVariables`-like helpers; check if commons-reachable.
  - **`BeanMapEntry`** — imports `annotation.*` (Javadoc references to `@Marshalled` / `@MarshalledProp`) + `swap.*` (Javadoc `ObjectSwap`). Body references: none that survive Phase B/C. Should be a trivial cleanup: replace Javadoc fully-qualified names with `{@link org.apache.juneau.swap.ObjectSwap}` form so the imports can be dropped.
  - **`BeanMetaFiltered`** — imports `annotation.*` (just `MarshalledFilter` and `@Marshalled` Javadoc). Single line `super(innerMeta.getClassMeta(), (MarshalledFilter) innerMeta.getMarshalledFilter(), pNames, null);` — body is marshalling-aware. After the cluster moves, this becomes a marshalling-side type that lives in `commons.bean` purely because `BeanMeta` does; the constructor stays unchanged because it threads marshalling-side data through `BeanMeta`'s protected constructor.
  - **`BeanPropertyValue`** — clean as of Phase C Task 1 (no marshalling-side imports survive). Ready to move.
  - **`BeanPropertyConsumer`** — clean (functional interface, no marshalling-side references). Ready to move.
  - **`BeanProxyInvocationHandler`** — still uses `meta.getMarshallingContext().toBeanMap(arg)` inside `equals(Object)`. Route this through `BeanSession.toBeanMap(arg)` using the SPI seam; the field is already typed against an SPI that `MarshallingContext` exposes.

  **Phase C Task 5 prerequisites (estimated effort):**
  - Lift the ~20 `@Marshalled`/`@BeanProp`/`@BeanCtor`/`@BeanIgnore`/`@Transient`/`@Name`/`@Uri` annotation reads from `BeanMeta` / `BeanPropertyMeta` to `MarshalledBeanMetaInitializer` (or move `@BeanType`/`@Name`/`@Uri` to `commons.bean.annotation` since they're bean-modeling concerns). Estimated: ~150-200 lines of helper code, ~30 call-site changes in `BeanMeta`/`BeanPropertyMeta`.
  - Replace the three `((MarshallingContext) bc).X()` casts in `BeanPropertyMeta` with SPI calls (`Builder.rawMetaType(Class<?>)`, constructor `ap`, `applyChildPropertiesFilter`). Either extend `BeanTypeResolver` with `getClassMeta(Class<?>)` and `getBeanMeta(Class<?>)` methods, or move the call sites to a marshalling-side helper.
  - Move `applyChildPropertiesFilter(MarshallingSession, ClassMeta, Object)` to a marshalling-side helper (it's not callable from the commons-side path anyway — it throws `UnsupportedOperationException` when `bc == null`).
  - Retype `BiFunction<MarshallingSession,Object,Object>` fields on `BeanPropertyMeta`/`BeanPropertyMeta.Builder` to `BiFunction<BeanSession,Object,Object>` (or `BiFunction<Object,Object,Object>`), updating the marshalling-side `MarshalledPropertyPostProcessor.installSwapAwareTransforms` to cast back.
  - Route `BeanProxyInvocationHandler`'s `meta.getMarshallingContext().toBeanMap(arg)` through `BeanSession.toBeanMap(arg)` (the SPI already supports this — just need to use a `BeanSession`-typed handle).
  - Delete `BeanMap.of(T)` (and update `juneau-assertions` callers to use `MarshallingContext.DEFAULT_SESSION.toBeanMap` directly) to remove the `MarshallingContext.DEFAULT_SESSION` reference from `BeanMap`.
  - Clean up Javadoc references in `BeanMapEntry`, `BeanMap`, `BeanMeta`, `BeanPropertyMeta` to fully-qualify marshalling-side links so the wildcard imports can be dropped.
  - **Then** the physical `git mv` + reference sweep across ~80-120 files + verify `juneau-commons` standalone compile + full test suite.

  Total estimated effort for Task 5 completion: ~600-1000 lines of helper code lifts + careful import surgery on the 8 files + the actual physical move + reference sweep. Recommend a fresh checkpoint pass focused exclusively on Task 5 prep + Task 5.

  **Phase B status (uncommitted, working tree) — COMPLETE, build + full test green (49,912 tests pass).**

  Field retypes landed (all use the "type the field against the commons.bean SPI seam; keep the public getter returning the marshalling-side type via a cast" pattern, so external callers don't need to change):

  - **Sub-item 2 (`BeanPropertyMeta.swap` → `Object`)** — both `Builder.swap` and `BeanPropertyMeta.swap` retyped. The single use site inside `setPropertyValue` (the defensive double-unswap check) keeps the `nn(swap) && value1 != null && ((ObjectSwap)swap).getSwapClass().isAssignableFrom(value1.getClass())` guard with a local cast. Marshalling-side consumers (`BeanMeta.installSwapAwareTransforms` reading `Builder.swap`, `MarshalledPropertyPostProcessor` reading `b.swap.getSwapClass()`) cast back to `ObjectSwap` at their use sites.
  - **Sub-item 3 (`BeanPropertyMeta.Builder.bc` → `Object`)** — both `Builder.bc` and `BeanPropertyMeta.bc` retyped. The few callers that need MarshallingContext-only methods (`Builder.rawMetaType(Class<?>)` calls `bc.getClassMeta(value)`, `applyChildPropertiesFilter` calls `bc.getBeanMeta(o.getClass())`, the post-constructor body calls `bc.getAnnotationProvider()`) cast back to `MarshallingContext` at the use site. `Builder.validate(MarshallingContext bc, ...)`'s parameter is unchanged — the method shadows the field cleanly. Note: lifting the `bc.resolveClassMeta(...)` / `bc.object()` calls inside `validate(...)` out to a marshalling-side post-processor remains a Phase C cleanup; for Phase B, the field is `Object` but the parameter to `validate(...)` retains its `MarshallingContext` type, which is fine because `validate(...)` lives on the bean-modeling-side `Builder` only when invoked from marshalling-side code.
  - **Sub-item 5 (`BeanMeta.beanRegistry` / `propertyBeanRegistries` → `BeanRegistryLookup`)** — `Supplier<BeanRegistry>` → `Supplier<BeanRegistryLookup>`, `Map<BeanPropertyMeta,BeanRegistry>` → `Map<BeanPropertyMeta,BeanRegistryLookup>`. `findBeanRegistry()` return narrowed to `BeanRegistryLookup`. Public getters `getBeanRegistry()` and `getPropertyBeanRegistry(BeanPropertyMeta)` keep their `BeanRegistry` return types via internal `(BeanRegistry) beanRegistry.get()` / `(BeanRegistry) propertyBeanRegistries.get(p)` casts — only one concrete impl in-tree. The 10+ marshalling-side callers (`ParserSession.getClassMeta(...)`, `SerializerSession` dictionary-name resolution, `JsonMap.cast(...)`, `MarkdownParserSession`, etc.) need zero source changes. Inside `BeanMeta` itself, one site (`new BeanRegistry(marshallingContext, beanRegistry.get(), v.dictionaryClasses)` in the per-property side-map build) casts `beanRegistry.get()` back to `BeanRegistry` to satisfy the parent-registry constructor parameter. **Phase C will need to widen the public getter return types to `BeanRegistryLookup`** so `BeanMeta` itself can move to `commons.bean` — but that's a downstream concern.
  - **Sub-item 1 (`BeanPropertyMeta.rawTypeMeta` / `typeMeta` / `BeanMeta.classMeta` → `BeanTypeInfo<?>`)** — all three fields retyped (both Builder copies and the final `BeanPropertyMeta` instance fields). The public getters `BeanPropertyMeta.getClassMeta()` and `BeanMeta.getClassMeta()` keep their `ClassMeta<?>` / `ClassMeta<T>` return types via internal `(ClassMeta<?>) typeMeta` / `(ClassMeta<T>) classMeta` casts. Internal call sites that pass `rawTypeMeta` to a `ClassMeta`-typed parameter (specifically `applyChildPropertiesFilter(ClassMeta, …)` and `new DelegateList(ClassMeta)`) cast at the call site. `BeanMeta.installSwapAwareTransforms` casts `p.rawTypeMeta` back to `ClassMeta<?>` for the local `rtm` alias. The ~30+ marshalling-side files that call `pMeta.getClassMeta().<method>` need zero source changes because the public getter still returns `ClassMeta<?>`. **Phase C will need to widen `getClassMeta()` to `BeanTypeInfo<?>`** so `BeanPropertyMeta`/`BeanMap`/`BeanMeta` can move to `commons.bean`; the cascade across ~30+ marshalling-side files needs careful audit (some sites call `ClassMeta`-only methods like `getSerializedClassMeta(this)` / `getSwap(...)` and will need explicit `(ClassMeta<?>)` casts then).
  - **Sub-items 6 + 7 (`BeanMeta` constructor refactor)** — picked option (a) (typed against `BeanTypeInfo<T>`, marshalling-side body casts back). The protected `BeanMeta(ClassMeta<T>, BeanFilter, String[], ClassInfo)` constructor signature changed to `BeanMeta(BeanTypeInfo<T>, BeanFilter, String[], ClassInfo)`; the body casts `cm` back to `ClassMeta<T>` to call `getMarshallingContext()` / `getBeanConfigContext()`. `BeanMetaFiltered.super(...)` (passes `innerMeta.getClassMeta()` which still returns `ClassMeta<T>`) compiles unchanged via Java's `ClassMeta<T> isa BeanTypeInfo<T>`. The 7-param private `BeanMeta(ClassMeta<T>, ClassInfo, BeanConfigContext, MarshallingContext, BeanFilter, String[], ClassInfo)` stays as-is (private; only this file calls it). **Phase C will need to move this constructor body out to a marshalling-side helper** (e.g. `ClassMetaBeanMetaBuilder.build(ClassMeta, BeanFilter, String[], ClassInfo)`) so the marshalling-only assignments (`marshallingContext = mc;`, `classMeta = cm;`) and the `cm.getMarshallingContext()` walk live outside `BeanMeta` proper.
  - **Sub-item 11 (`BeanInstantiator.type(rawTypeMeta)`)** — verified compatible. `BeanInstantiator.type(ClassInfo)` accepts any `ClassInfo` subtype; `BeanTypeInfo<T>` extends `ClassInfoTyped<T>` which extends `ClassInfo`, so `BeanInstantiator.of(Collection.class).type(rawTypeMeta).preferZeroArgConstructor().run()` compiles unchanged. No `BeanInstantiator` changes needed.
  - **`MarshallingSession` bridge fix** — `convertToType(Object value, Object targetType)` and `convertToMemberType(Object outer, Object value, Object targetType)` now treat `targetType == null` as a `(ClassMeta<?>) null` dispatch so the typed `ClassMeta`-aware overloads can apply their `object()` fallback. Without this fix, the `getElementType()`/`getValueType()`/`getKeyType()` calls that legitimately return `null` for non-collection/non-map types throw `"Unsupported targetType for convertToType: null"` once the call site widens to `BeanTypeInfo<?>` (the original `<T> T convertToType(Object, ClassMeta<T>)` overload happily took `null` and the underlying `convertToMemberType` substituted `object()` for a null `to`). Affected 586 round-trip tests across `html5`, `marshall`, `bson`, `cbor`, etc. before the fix.

  Remaining sub-items (deferred to Phase C):

  - **Phase C Task 1 — Public API widen.** — DONE. See Phase C status block above. `BeanPropertyMeta.getClassMeta()` → `BeanTypeInfo<?>`, `BeanMeta.getClassMeta()` → `BeanTypeInfo<T>`, `BeanMeta.getBeanRegistry()` / `getPropertyBeanRegistry(...)` → `BeanRegistryLookup`, `BeanMap.getClassMeta()` → `BeanTypeInfo<T>`. ~26 marshalling-side callers updated with explicit `(ClassMeta<?>)` / `(BeanRegistry)` casts at the call sites that invoke `ClassMeta`-only / `BeanRegistry`-only methods. Build + full test green.
  - **Phase C Task 2 — `validate(...)` body lift-out.** — DONE (signature seam). See Phase C status block above. New `BeanTypeResolver` SPI in `commons.bean`. `MarshallingContext` implements it. `Builder.validate(BeanTypeResolver bc, ...)` parameter retyped. `bc.resolveClassMeta(...)` → `bc.resolveType(...)`, `bc.object()` → `bc.objectType()`. Build + full test green. Note: three other marshalling-only seams on `BeanPropertyMeta` remain (Builder `rawMetaType(Class<?>)`, constructor `ap` initialization, `applyChildPropertiesFilter`) — they cast `bc` to `MarshallingContext` and are not yet covered by the SPI.
  - **Phase C Task 3 — Marshalling-side `BeanMeta` factory helper.** — DONE. See Phase C status block above. New `MarshalledBeanMetaInitializer` in `juneau-marshall` owns the marshalling-aware helpers (`buildBeanRegistry`, `buildPropertyBeanRegistry`, `findTypeNameInParents`, `findMarshalledTypeName`, plus extractors `configOf`/`contextOf`/`classInfoOf` and the relocated `findMarshalledFilter`). `installSwapAwareTransforms` relocated from `BeanMeta` to `MarshalledPropertyPostProcessor` (now invoked from inside `process(...)`). `BeanMeta.marshallingContext` retyped to `Object` so the field can live in `commons.bean`; getter casts back. `BeanMeta` dropped its `parser.*`/`serializer.*`/`swap.*` imports. Build + full test green.
  - **Phase C Task 4 — `JsonList`/`JsonMap` final cleanup.** — DONE. The remaining CharSequence-handling sites (`JsonMap.ofJson(value21).session(session)` and `new JsonList(value2).setBeanSession(session)`) now route through new `BeanSession` SPI methods `parseToMap(CharSequence)` / `parseToList(CharSequence)`. `MarshallingSession` implements both as bridge methods delegating to the original `JsonMap.ofJson` / `new JsonList` calls. `BeanPropertyMeta` dropped its `import org.apache.juneau.collections.*` — no longer references `JsonMap`/`JsonList` directly. Build + full test green.
  - **Phase C Task 5 — Physical `git mv` + reference sweep.** — NOT STARTED. Prerequisites partially met (Tasks 1-4-deferred done). The remaining marshalling-side coupling in the 8 files is deeper than the original Task 5 prep scope (see "Phase C Task 5 status" block above for full inventory). Recommend a fresh checkpoint pass focused exclusively on: (a) lifting ~20 `@Marshalled`/`@BeanProp`/`@BeanCtor`/`@BeanIgnore`/`@Transient`/`@Name`/`@Uri` annotation reads from `BeanMeta`/`BeanPropertyMeta` to `MarshalledBeanMetaInitializer`, (b) replacing the three `((MarshallingContext) bc).X()` casts in `BeanPropertyMeta` with SPI calls, (c) moving `applyChildPropertiesFilter` to a marshalling-side helper, (d) retyping `BiFunction<MarshallingSession,...>` fields to `BiFunction<BeanSession,...>`, (e) routing `BeanProxyInvocationHandler` through `BeanSession.toBeanMap`, (f) deleting `BeanMap.of(T)` and updating `juneau-assertions` callers, (g) Javadoc cleanup, (h) then `git mv` + reference sweep.
- [ ] **Step 8c** — (optional) Cleanup pass for anything that comes up during 8b-ii: deprecated bridges, stale imports, package-info docs, etc.
- [ ] **Step 9** — Reference sweep: 80–120 unique files (mostly inside `juneau-marshall`). Update imports, Javadoc `{@link …}` references, package-info docs.
- [ ] **Step 10** — Update `juneau-docs` release notes / migration guide (`docs/pages/release-notes/9.5.0.md`, `## Package Moves` section) with the bean-runtime relocations.

The "incomplete-but-documented over broken-build" rule from Phase 5a still applies. **The recommended next checkpoint is Step 8b** — extracting the remaining SPI seams that the 8 types need from the marshalling layer, then doing the physical `git mv`. Step 8a is the prerequisite (`BeanSession` is in place and `BeanConfigContext` already covers the bean-modeling boolean settings); Step 8b can build on those.

**Remaining risks/surprises for Step 8b:**
- The 8 target types are tightly clustered: `BeanMap` ↔ `BeanPropertyMeta` ↔ `BeanMeta` ↔ `BeanMapEntry` ↔ `BeanPropertyValue` ↔ `BeanPropertyConsumer` ↔ `BeanMetaFiltered` ↔ `BeanProxyInvocationHandler`. They reference each other in field declarations and method signatures, so they have to move **as a unit**. Moving just the leaf types (e.g. `BeanPropertyConsumer`, `BeanMapEntry`) would create circular cross-module references because they reference `BeanPropertyMeta`/`BeanMap` which stay in `juneau-marshall` until the cluster moves.
- `ClassMeta` is the biggest seam to design. Options:
  - **(a)** Introduce `BeanTypeInfo` interface in `commons.bean` (minimum surface: `inner()`, `isPrimitive()`, `isOptional()`, `getOptionalDefault()`, `isAssignableTo`, `isCollection()`, `isMap()`, `isString()`, `isObject()`, `getElementType()`, `getValueType()`, `getKeyType()`, `getInfo()`); have `ClassMeta` implement it. Pro: minimal API churn on the 8 types. Con: large interface to design carefully.
  - **(b)** Retype the fields as `Object` on the bean-modeling side; cast to `ClassMeta` in narrow marshalling-side helpers. Pro: zero new SPI surface. Con: stringly-typed; harder to read.
  - **(c)** Move the consumption sites (e.g. `BeanMap.getBean()`'s Optional-init, `applyChildPropertiesFilter`) into marshalling-side helpers; leave `BeanMap`/`BeanPropertyMeta` with no `ClassMeta` references at all. Pro: cleanest commons.bean. Con: refactors marshalling-side code along with the move.
- `BeanRegistry` side-map (`Map<BeanPropertyMeta,BeanRegistry> propertyBeanRegistries`) on `BeanMeta` is currently typed against the concrete marshalling-side `BeanRegistry`. Either: keep the map but type its value as `Object` (commons-side narrowing-as-needed), or introduce a `BeanRegistryLookup` interface (`String getTypeName(Class<?>)` + `Class<?> getClassMeta(String)` is probably all that's needed).
- `ObjectSwap` field on `BeanPropertyMeta` is only metadata after Step 3 — the actual swap/unswap calls go through `readTransform`/`writeTransform`. Probably the cleanest move is to retype `swap` as `Object` and let the few callers (`setPropertyValue`'s defensive double-unswap *check*, `BeanMapEntry`'s Javadoc) live with that.
- `BeanMetaFiltered` extends `BeanMeta` and calls `super(innerMeta.getClassMeta(), innerMeta.getMarshalledFilter(), pNames, null)`. The old constructor signature (`BeanMeta(ClassMeta<T>, MarshalledFilter, String[], ClassInfo)`) carries `ClassMeta` and `MarshalledFilter` as parameters. After the move, that constructor either: stays on `BeanMeta` typed against `Object`s, or moves to a marshalling-side factory helper. The new `BeanMeta.of(Class, BeanConfigContext)` factory chain from Step 6 is the commons-side path; the legacy `BeanMeta(ClassMeta, ...)` is the marshalling-side path. Both must coexist.
- `MarshalledProp` annotation read inside `BeanPropertyMeta.Builder.validate()` is the only direct marshalling-annotation reference in the 8 types' source. Either: lift the read out into a marshalling-side post-processor that mutates the `Builder` before `build()`, or move `MarshalledProp` to `commons.bean` (it's already paired with `MarshalledFilter` which is squarely marshalling-side, so the lift-out is cleaner).
- `BeanProxyInvocationHandler.toString()` uses `Json5Serializer.DEFAULT.toString(...)`. Trivially replaceable with `Objects.toString(...)` or routed through a `BeanSession`-style formatter.

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
