# TODO-58 — Fix silent element-drop when `BeanMap.put` converts `List<String>` → `Set<EnumType>`

Source: filed 2026-05-22 after the IRS team (`central-routing/irs` PR [#1806](https://git.soma.salesforce.com/central-routing/irs/pull/1806)) was forced to add a per-bean `parse()` override in `Suspension.java` to work around a silent element-drop in Juneau's `BeanMap.put` conversion path. The override exists purely to bypass Juneau and parse the CDL string into a typed `TreeSet<SuspensionType>` up front. Without it, the bean's `Set<SuspensionType>` property ends up empty even though the input string contains valid enum tokens.

---

## 1. Background / context

### The symptom (verbatim from the downstream workaround)

The IRS `ChangeableDaoBean` framework feeds property updates from a "Change" ledger into Juneau beans via:

```154:167:/Users/james.bognar/git/central-routing/irs/irs-server/src/main/java/com/sfdc/irs/dao/ChangeableDaoBean.java
	protected void setProperty(String property, String value) {
		try {
			var bm = beanMap(this);
			var type = ofNullable(bm.getPropertyMeta(property)).orElseThrow(()->new RuntimeException("Property "+property+" not defined on class "+getClass().getSimpleName())).getClassMeta();
			bm.put(property, parse(type, property, value));
		} catch (RuntimeException e) {
```

The base `parse(ClassMeta, String, String)` returns a generic `List<String>` for any Collection-typed property:

```232:235:/Users/james.bognar/git/central-routing/irs/irs-server/src/main/java/com/sfdc/irs/dao/ChangeableDaoBean.java
	protected <T> Object parse(ClassMeta<T> c, String property, String val) {  // NOSONAR
		if (c.isAssignableFrom(Collection.class)) { return cdlToList(val); }
		return val;
	}
```

PR #1806 introduced a new `Suspension` field:

```java
@Beanp(type=TreeSet.class, params=SuspensionType.class)
protected Set<SuspensionType> types;
```

…and discovered that `bm.put("types", List.of("SBX_DEV","SBX_DEVPRO"))` produced an **empty** `TreeSet<SuspensionType>` rather than `{SBX_DEV, SBX_DEVPRO}`. The wire-form `String` tokens were silently discarded instead of being converted to `SuspensionType` enum instances. The override they added (the `parse` method at lines 330-349 of `Suspension.java`) sidesteps Juneau entirely for this one property by producing the already-typed `TreeSet<SuspensionType>` so `BeanMap.put` has no element conversion to do.

Comment from the workaround:

> Juneau's generic `List<String>` → `Set<SuspensionType>` conversion silently drops elements for this property, so we parse the CDL string into a typed `TreeSet<SuspensionType>` up front. Other properties fall through to the base implementation.

### Why the same shape already works for other IRS properties

`Suspension` has several other collection-typed properties (`instances`, `allowList`) that round-trip through the same base `parse()` → `cdlToList()` → `BeanMap.put()` path without dropping elements. Those properties are `SortedSet<String>` / `InstanceNameSet`-style — i.e. the element type is `String` (or a `String`-coercible wrapper). The element drop only manifests when the target element type is an **enum** (or, conjecturally, any type that requires `String → T` conversion via `convertToType`).

### Presumed fix site (not yet verified)

The most likely fix site is the `Collection` branch of `BeanPropertyMeta.setPropertyValue` in `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java`. The branch looks correct on the surface:

```1190:1246:juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java
			} else if (isCollection && (setter == null || ! pcInfo.isAssignableFrom(vc))) {

				if (! (value1 instanceof Collection)) {
					if (value1 instanceof CharSequence value2)
						value1 = session.parseToList(value2);
					else
						throw bex(beanMeta.getBeanInfo(), "Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}''", name, propertyClass.getName(), cn(value1));
				}

				var valueList = (Collection)value1;
				var propList = (Collection)r;
				var elementType = rawTypeMeta.getElementType();

				// If the property type is abstract, then we either need to reuse the existing
				// collection (if it's not null), or try to assign the value directly.
				if (! rawTypeMeta.canCreateNewInstance()) {
					if (propList == null) {
						if (setter == null && field == null)
							throw bex(beanMeta.getBeanInfo(),
								"Cannot set property ''{0}'' of type ''{1}'' to object of type ''{2}'' because no setter or public field is defined, and the current value is null", name,
								propertyClass.getName(), cn(value1));

						if (propertyClass.isInstance(valueList) || (nn(setter) && setter.getParameterTypes().get(0).is(Collection.class))) {
							if (! elementType.isObject()) {
								var l = new ArrayList<>(valueList);
								for (var i = l.listIterator(); i.hasNext();) {
									var v = i.next();
									var needsConversion = v == null ? elementType.isOptional() : ! elementType.isInstance(v);
									if (needsConversion)
										i.set(session.convertToType(v, elementType));
								}
								valueList = l;
							}
							invokeSetter(bean, pName, valueList);
							return r;
						}
```

`elementType` is supposed to be `ClassMeta<SuspensionType>` and `convertToType` should handle `String → Enum` via `Enum.valueOf`. Two leading hypotheses for why elements actually get dropped:

1. **`elementType.isObject()` returns true** because `rawTypeMeta.getElementType()` doesn't honor `@Beanp(params=…)` for the abstract-`Set`-with-concrete-`type` shape. If `elementType` is `Object`, the entire per-element conversion block at lines 1213-1221 is skipped, and `List<String>` is handed straight to the setter as a literal `List<String>` — which then fails to be assigned into `Set<SuspensionType>` (or is silently dropped at a higher level).
2. **`BeanMap.put` routes through a different branch** entirely for `@Beanp(type=TreeSet.class)` on an abstract `Set` field — e.g. the `setter == null` check at line 1190 fires differently when `@Beanp` declares a concrete `type` that overrides the field's declared type, and the value gets handed to `convertToMemberType` (line 1249) instead. `convertToMemberType` may then take a `List<String> → Set<Enum>` shortcut path that doesn't actually iterate elements.

Phase 1's first job is to reproduce the gap in a standalone unit test and tag the precise branch.

---

## 2. Scope

### In scope

- Reproduce the symptom in a standalone `juneau-commons` (or `juneau-utest`) unit test that has no IRS dependency: bean with `@Beanp(type=TreeSet.class, params=EnumType.class) Set<EnumType> prop;`, `BeanMap.put("prop", List.of("FOO","BAR"))`, assert the bean's getter returns `{FOO, BAR}` (not an empty set).
- Identify the actual fix site in `BeanPropertyMeta` (most likely the `Collection` branch around lines 1190-1246) and patch it to convert elements via `session.convertToType(x, elementType)` when the source-list element type doesn't match the target-set element type.
- Confirm the fix doesn't regress the `List<String>` → `Set<String>` shape (the common case that already works), the `List<String>` → `List<EnumType>` shape, or the JSON family's parser-level conversion path.
- Verify the IRS workaround in `Suspension.java#parse(...)` (lines 330-349 of PR #1806) becomes unnecessary, then notify the IRS team so they can remove it once they pick up the next Juneau release.

### Out of scope

- The IRS-side workaround itself — leave it in place until the Juneau fix ships. Removing it is a downstream cleanup, not part of this plan.
- The `Map<K, V>` key-coercion gap tracked under `todo/TODO-14-beanpropertymeta-map-key-coercion.md`. Same file, related shape, but a different branch and a different missing call.
- Generic-arity changes to `@Beanp` — fix is limited to honoring the existing `type` / `params` declarations correctly during the `List<String>` → `Set<EnumType>` path.
- Per-format parser workarounds — this gap surfaces in **direct `BeanMap.put`** usage (no parser involved), so the fix has to be at the commons-side bean-property assignment site, not at a parser dispatch site.

---

## 3. Phases

### Phase 1 — Reproduce the symptom in a standalone test

Land a new unit test under `juneau-utest` that:

1. Defines a small bean with a `Set<TestEnum>` property annotated `@Beanp(type=TreeSet.class, params=TestEnum.class)` — matching the exact `Suspension.java` shape.
2. Builds a `BeanMap` for the bean from a default `BeanSession`.
3. Calls `BeanMap.put("types", List.of("FOO","BAR"))` — i.e. hands a `List<String>` directly to the bean property, bypassing every parser.
4. Asserts that the bean's `Set<TestEnum>` getter returns `{TestEnum.FOO, TestEnum.BAR}`.

The test must **fail** on the current commons-side code (proving the gap is real and matches the IRS symptom) and **pass** after the Phase 2 fix. Use a `TestEnum` defined in the test's own scope to avoid coupling the unit test to any real domain enum.

Add three companion assertion-only variants in the same test file to nail down the precise shape:

- `List<String>` → `Set<String>` (no enum conversion needed) — must pass today, must keep passing.
- `List<String>` → `List<TestEnum>` (target is a `List`, not a `Set`) — exercises the same Collection branch with a different concrete type.
- `Set<String>` → `Set<TestEnum>` (source is already a Set) — confirms the bug isn't specific to `List` source.

Use a debugger or targeted `System.err.println` (removed before commit) to capture which branch of `setPropertyValue` actually fires and what `elementType` / `rawTypeMeta.getElementType()` resolve to in the failing case. Append the answer to the Open Questions section.

### Phase 2 — Implement the commons-side fix

Once Phase 1 has pinned the branch and the failing predicate, the fix is one of:

- **If `elementType.isObject()` is firing on a properly-annotated `@Beanp(params=…)` Set field**, the fix is in `ClassMeta` (or in whatever resolves `rawTypeMeta.getElementType()` for `@Beanp`-overridden types) to honor the `params[]` declaration when the declared field type is parameterized but the `@Beanp(type=…)` overrides to a concrete type.
- **If the value-conversion branch is being skipped for a different reason** (e.g. the `(propertyClass.isInstance(valueList) || setter.getParameterTypes().get(0).is(Collection.class))` guard at line 1212 is short-circuiting and falling through to a `convertToMemberType` shortcut at line 1249), the fix is to extend that branch to also run the per-element coercion before invoking the setter.
- **If `BeanMap.put` is routing through a completely different code path** for this shape, the fix is at that path, not in `setPropertyValue`. Phase 1's debugger pass tells us which.

Either way: no new `ClassMeta` API, no `BeanSession` change, no signature change on `setPropertyValue`. The conversion plumbing (`session.convertToType`, `elementType.isInstance`, `elementType.isOptional`) is already wired through.

### Phase 3 — Regression check across collection shapes

Run targeted tests against the existing collection-property coverage to confirm the fix doesn't regress already-working shapes:

```bash
mvn -pl juneau-utest -am -Dtest='*BeanMap*Test,*BeanPropertyMeta*Test,*ClassMeta*Test' test
```

Then a full sweep:

```bash
./scripts/test.py
```

The full `EnumFormat_RoundTrip_Test` matrix tracked under TODO-57 must stay at its current pass rate (the matrix exercises `Set<Enum>` and `List<Enum>` shapes through every parser; a regression in the commons-side fix would show up there immediately).

### Phase 4 — Downstream cleanup notification

Once the fix ships in a Juneau release:

1. Confirm the IRS `Suspension.java#parse(...)` override (lines 330-349 of PR #1806) becomes a no-op against the new Juneau version — i.e. the override's behavior matches what `BeanMap.put` now does on its own. Run the IRS test added for that override (`SuspensionServiceTest`, `SuspensionTest`, or whichever test covers the change-ledger round trip) against a snapshot Juneau build with this fix.
2. Notify the IRS team (Slack `#central-routing-irs` or the equivalent) that the override can be removed.
3. The IRS team owns the actual removal — it's not part of this plan.

---

## 4. Open questions

1. **What does `rawTypeMeta.getElementType()` actually return for `@Beanp(type=TreeSet.class, params=SuspensionType.class) Set<SuspensionType> types`?** Phase 1 answers this. If it returns `ClassMeta<Object>`, the bug is upstream of `setPropertyValue` (in `ClassMeta` / `BeanPropertyMeta` setup). If it returns `ClassMeta<SuspensionType>`, the bug is downstream (in the Collection branch's conversion call).
2. **Does the bug reproduce without the `@Beanp` annotation?** i.e. plain `Set<SuspensionType> types;` with a setter `setTypes(Set<SuspensionType>)`. If yes, the issue is purely with generic type resolution on `Set<EnumType>` fields and has nothing to do with `@Beanp`. If no, the issue is specific to how `@Beanp(type=…, params=…)` is consumed during property assignment.
3. **Does the symmetric `List<EnumType>` shape (List instead of Set) reproduce?** Phase 1 covers this. If `List<EnumType>` works but `Set<EnumType>` doesn't, the bug is in the abstract-collection branch (`! rawTypeMeta.canCreateNewInstance()` at line 1205) that handles `Set` differently from `List`.
4. **Is the silent-drop actually silent, or is there a warning being swallowed?** `BeanPropertyMeta.setPropertyValue` has an `ignoreInvocationExceptionsOnSetters` flag (line 1258). If the conversion is actually throwing and being swallowed by that flag, the fix is partly to disable the flag for this path, or to log the swallowed exception at debug level.
5. **Relationship to TODO-14.** TODO-14 is about `Map<K, V>` key coercion in the same file; TODO-57 surfaced the same underlying philosophy ("inspect entry values but not entry keys"). This TODO-58 is about `Set<E>` / `List<E>` element coercion where the source element type doesn't match the target element type. The three plans probably share a common root cause (generic-type-aware element inspection during `BeanMap.put`); worth checking after Phase 1 whether one unified fix closes all three or whether each needs its own surgical change.

---

## 5. Acceptance criteria

- New `juneau-utest` test (Phase 1) reproduces the symptom on pre-fix code, passes on post-fix code.
- All four assertion-only variants from Phase 1 (`List<String>→Set<String>`, `List<String>→List<TestEnum>`, `Set<String>→Set<TestEnum>`, plus the headline `List<String>→Set<TestEnum>`) pass on post-fix code.
- `./scripts/test.py` clean across the rest of the suite.
- `EnumFormat_RoundTrip_Test` matrix (tracked under TODO-57) stays at its current pass rate — no parser-level regression.
- Open Questions 1-4 above are answered in writing and appended to this plan.
- IRS `Suspension.java#parse(...)` override (PR #1806 lines 330-349) verified redundant against the fixed Juneau build, and the IRS team notified.

---

## 6. Out of scope

- The IRS-side `Suspension.java#parse(...)` override removal — owned by the IRS team, downstream of the Juneau release.
- The `Map<K, V>` key-coercion gap — see `todo/TODO-14-beanpropertymeta-map-key-coercion.md`.
- Sibling shapes (`Iterable<E>`, generic-typed arrays `T[]`) — flagged under Open Question 5 in TODO-14, not part of this plan's initial fix.
- Performance optimization of `convertToType` itself — orthogonal.
- `@Beanp(params=…)` semantics for non-Collection / non-Map shapes — out of scope.

---

## 7. Related plans / references

- **IRS PR #1806** — [git.soma.salesforce.com/central-routing/irs/pull/1806](https://git.soma.salesforce.com/central-routing/irs/pull/1806). The `Suspension.java#parse(...)` override at lines 330-349 of the new file is the downstream workaround this plan exists to retire.
- **`todo/TODO-14-beanpropertymeta-map-key-coercion.md`** — sibling plan for the `Map<K, V>` key-coercion gap in the same file. Same philosophy ("inspect one side, not the other"), different branch.
- **`todo/TODO-57-format-round-trip-tests.md`** — the round-trip test matrix that surfaced the `Map<K, V>` gap (Bug #7b) and would surface this `Set<E>` / `List<E>` gap if it manifested through any parser. The fact that it doesn't surface there confirms this is a `BeanMap.put`-direct-usage bug, not a parser bug.
- **The `BeanPropertyMeta.setPropertyValue` Collection branch** — `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java` lines 1190-1246. Most likely fix site, pending Phase 1 confirmation.
