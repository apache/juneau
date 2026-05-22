# FINISHED-58 — Harden `BeanPropertyMeta` typed-collection element write path

Completed: 2026-05-22

## Outcome

TODO-58 is complete. The typed-collection write path in `BeanPropertyMeta.setPropertyValue(...)` now uses an explicit-iteration pattern that mirrors the just-merged typed-map fix (TODO-14, commit `affabe50f3`). New focused regression tests in `BeanMap_Test` cover the typed-`Set<EnumType>` / typed-`List<EnumType>` write shapes — including the exact `@BeanProp(type=TreeSet.class, params=EnumType.class) protected Set<EnumType>` shape that the IRS `Suspension` bean uses in `central-routing/irs` PR #1806.

## Important finding — the original symptom did NOT reproduce

The plan was filed on the hypothesis that `BeanMap.put` was *silently dropping* elements when feeding `List<String>` into `Set<EnumType>` against an `@BeanProp(type=TreeSet.class, params=EnumType.class)` field. We could not reproduce that symptom on the current `master` (post `affabe50f3`).

Direct instrumentation of the IRS-style shape (`protected Set<HEnum> s` annotated `@BeanProp(type=TreeSet.class, params=HEnum.class)`) showed:

- `rawTypeMeta` resolved to `TreeSet<HEnum>` (concrete, `canCreateNewInstance() == true`).
- `rawTypeMeta.getElementType()` resolved to `HEnum` (NOT `Object`).
- `BeanMap.put("s", List.of("ONE","TWO"))` produced a `TreeSet` of size 2 containing the **converted** `HEnum.ONE` and `HEnum.TWO` enum values — not strings.

So whatever the IRS team observed on their build, the silent-element-drop is not present in the current commons-side write path. Possible explanations:

1. Their Juneau snapshot predates a fix already shipped on `master`.
2. Their `Suspension` bean has a shape detail that didn't carry over into the reproducer (we tried both `public` and `protected` fields and the `List.of`/`list(...)` source flavor).
3. Their workaround was speculative and the real failure was elsewhere in the change-ledger pipeline (e.g. `ChangeableDaoBean.setProperty`'s exception-swallowing wrapper).

Regardless, the regression tests added under this work item lock in the correct behavior so it can't silently regress later.

## Implemented changes

### 1) Commons-side production change

File:
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java`

Change (concrete-collection write branch of `setPropertyValue(...)`):
- Replaced the `valueList.forEach(x -> { ... propList2.add(x); })` lambda write path with an explicit `for (var x : valueList) { ... propList.add(x); }` loop.
- Removed the now-unnecessary `propList2` capture local (the explicit loop can mutate `propList` directly without needing an effectively-final local for the lambda).
- Element conversion semantics are **unchanged** (still `session.convertToType(x, elementType)` when `! elementType.isObject()`).

Why:
- Symmetry with the TODO-14 fix on the typed-map branch (commit `affabe50f3`), which also moved from `valueMap.forEach((k,v) -> ...)` to an explicit `for (var e : valueMap.entrySet()) { ... }` loop for the same dispatch site.
- Easier to extend in future (e.g. to add per-element `nn(x) && ! elementType.isInstance(x)` short-circuit guards, or to add side-effects between the convert and the `add`) without re-juggling lambda captures.
- More readable: lambda parameter reassignment for the convert-then-add pattern is subtle.

The abstract-collection branch (lines around 1218-1230) was already using explicit `listIterator()` iteration with reassignment, so no change was needed there.

### 2) Regression / coverage test additions

File:
- `juneau-utest/src/test/java/org/apache/juneau/BeanMap_Test.java`

Added tests:
- `a43_typedSetField_coercesStringElementsToEnum` — `@BeanProp(type=TreeSet.class, params=HEnum.class) public Set<HEnum>` field; `BeanMap.put("s", list("ONE","TWO"))`; asserts size, `instanceof HEnum`, and enum contains.
- `a44_typedSetSetter_coercesStringElementsToEnum` — getter/setter-backed `TreeSet<HEnum>` property; same input shape and assertions via `getS()`.
- `a45_typedListField_coercesStringElementsToEnum` — `public List<HEnum>` field (no `@BeanProp`); exercises the abstract-collection branch with element conversion.
- `a46_typedSetFromSetSource_coercesStringElementsToEnum` — same as `a43` but with a `Set<String>` source instead of `List<String>`, to confirm the concrete-collection branch handles non-`List` source collections.
- `a47_typedSetField_stringElementsKeepWorking` — `@BeanProp(type=TreeSet.class, params=String.class)` field with `List<String>` input; confirms the no-conversion-needed case still works.
- `a48_typedSetProtectedField_IRSStyle_coercesStringElementsToEnum` — `@BeanProp(type=TreeSet.class, params=HEnum.class) protected Set<HEnum>` field (exact IRS `Suspension.types` shape) with `List.of(...)` source.

All six tests pass on `master` *before* this change, and continue to pass *after* this change — they are pure regression coverage.

## Verification run

### Focused area
- `mvn -pl juneau-utest -am -Dtest=BeanMap_Test -Dsurefire.failIfNoSpecifiedTests=false test`
  - `org.apache.juneau.BeanMap_Test`: **52 run, 0 failures, 0 errors, 0 skipped**
  - `org.apache.juneau.transforms.BeanMap_Test`: **2 run, 0 failures, 0 errors, 0 skipped**

### Parser regression suite (sibling-shape parser families)
- `mvn -pl juneau-utest -am -Dtest='Hjson*Test,Hocon*Test,Proto*Test,Bson*Test' -Dsurefire.failIfNoSpecifiedTests=false test`
  - **341 run, 0 failures, 0 errors, 2 skipped** (pre-existing skips).

### Enum matrix acceptance check
- `mvn -pl juneau-utest -am -Dtest=EnumFormat_RoundTrip_Test -Dsurefire.failIfNoSpecifiedTests=false test`
  - `EnumFormat_RoundTrip_Test`: **2268 run, 0 failures, 0 errors, 0 skipped**

### Broader suite
- `./scripts/test.py`
  - Build phase: success
  - Test phase: success

## Open questions — answered

1. **What does `rawTypeMeta.getElementType()` actually return for `@BeanProp(type=TreeSet.class, params=HEnum.class) Set<HEnum> s`?**
   - Returns `ClassMeta<HEnum>` (`isObject() == false`). `rawTypeMeta` itself resolves to `TreeSet<HEnum>` with `inner() == TreeSet.class` and `canCreateNewInstance() == true`. So the conversion plumbing has everything it needs.
2. **Does the bug reproduce without the `@BeanProp` annotation?**
   - The "silent drop" symptom does not reproduce **at all** on current `master` — neither with `@BeanProp` nor without. However: a plain `public Set<HEnum> s;` field (abstract `Set`, no setter, no `@BeanProp`) **throws** `BeanRuntimeException("Cannot set property 's' of type 'java.util.Set' to object of type 'java.util.ArrayList' because the assigned map cannot be converted to the specified type because the property type is abstract, and the property value is currently null")` when fed a `List<String>`. That's a real failure mode but it's a hard throw, not a silent drop. See "Follow-up audit" below.
3. **Does the symmetric `List<EnumType>` shape reproduce?**
   - No — `public List<HEnum> l;` correctly coerces `List<String>` input via the abstract-collection branch's existing per-element `listIterator()` conversion path. Test `a45` covers this.
4. **Is the silent-drop actually silent, or is there a warning being swallowed?**
   - Moot — the silent-drop did not reproduce. No exception is being swallowed by `ignoreInvocationExceptionsOnSetters` on the tested shapes. (The IRS `ChangeableDaoBean.setProperty` wrapper does have its own `try/catch (RuntimeException)`; if their actual symptom *was* a throw, that wrapper would swallow it and present as a silent drop one layer up. Worth flagging back to the IRS team.)

## Follow-up audit summary (sibling-shape pass)

Reviewed:
- `BeanPropertyMeta.setPropertyValue(...)` Collection branches (both abstract and concrete).
- Element-type resolution for `@BeanProp(type=..., params=...)` field/setter combinations.

Findings:
- No additional `Collection<E>` element-side coercion gaps in the typed write path itself.
- The concrete-collection branch's `forEach` write loop was correct but stylistically inconsistent with the just-fixed Map branch — harmonized in this work.
- **Latent bug found, NOT fixed under this work item:** An abstract `Set<EnumType>` field with **no** setter and **no** `@BeanProp(type=...)` (e.g. plain `public Set<HEnum> s;`) cannot be populated from a `List<String>` source. The abstract-collection branch's guard `propertyClass.isInstance(valueList) || (nn(setter) && setter.getParameterTypes().get(0).is(Collection.class))` short-circuits to a `throw` because `Set.isInstance(ArrayList) == false` and there is no setter. This isn't the silent-drop the IRS team reported, and it's specifically out-of-scope per TODO-58 section 6 ("`@BeanProp(params=…)` semantics for non-Collection / non-Map shapes — out of scope"), but it is a real failure for an idiomatic field-only shape. If we want to harden this, it'd be a one-line addition to instantiate a default concrete collection (e.g. `LinkedHashSet` for `Set`) via `BeanInstantiator.of(Collection.class).type(...)` in the same way the abstract-map branch already handles its analogous case. Worth filing as a separate TODO if a downstream consumer hits it.

Disposition:
- No new TODO was filed from this audit pass for TODO-58 scope.
- The latent abstract-Set-no-setter-no-`@BeanProp` issue is documented above for whoever runs into it next.

## Phase 4 — downstream cleanup notification (REMAINING)

This phase is left for the parent agent / human to action because it requires reaching out to the IRS team and verifying their snapshot of Juneau:

1. **Verify** that the IRS `Suspension.java#parse(...)` override (lines 330-349 of PR #1806) was actually addressing a real Juneau bug at the snapshot they were testing against. The symptom does not reproduce on current `master`, so either:
   - Their Juneau snapshot was older than `affabe50f3`'s base, or
   - Their real failure was the exception-swallowing wrapper in `ChangeableDaoBean.setProperty`, not the `BeanMap.put` conversion path.
2. **If verified redundant**, notify `#central-routing-irs` (or the IRS team's preferred channel) that the per-bean `parse()` override can be removed when they next pick up Juneau.
3. **Otherwise**, ask the IRS team to share a minimal reproducer against the exact Juneau version where they saw the failure, so we can decide whether to backport or chase the actual cause.
