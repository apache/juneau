# FINISHED-59 — Default concrete types for abstract collection `BeanMap.put` writes

Completed: 2026-05-22

## Outcome

TODO-59 is complete. `BeanPropertyMeta.setPropertyValue(...)` now assigns sensible default concrete collections when writing to abstract, field-only or setter-backed collection properties with null current value and no `@BeanProp(type=...)` hint, instead of throwing.

## Implemented changes

### 1) Commons-side production fix

File:
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java`

Change:
- In the abstract-collection branch of `setPropertyValue(...)`, when `propList == null` and direct assignment cannot be used, the code now creates a default collection and assigns it instead of immediately throwing.
- Added `createDefaultCollectionForAbstractType(Class<?>)` with explicit mappings:
  - `Set` -> `LinkedHashSet`
  - `SortedSet` / `NavigableSet` -> `TreeSet`
  - `Queue` / `Deque` -> `ArrayDeque`
  - `List` / `Collection` -> `ArrayList`
- Existing behavior remains unchanged for already-working direct-assignment paths (e.g. `List`/`Collection` values that are already assignable).

### 2) Regression test coverage

File:
- `juneau-utest/src/test/java/org/apache/juneau/BeanMap_Test.java`

Added tests:
- `a49_abstractSetField_noHint_usesLinkedHashSetAndCoercesElements`
- `a50_abstractSortedSetField_noHint_usesTreeSetAndCoercesElements`
- `a51_abstractQueueField_noHint_usesArrayDequeAndCoercesElements`
- `a52_abstractDequeField_noHint_usesArrayDequeAndCoercesElements`
- `a53_abstractSetSetter_noHint_usesLinkedHashSetAndCoercesElements`

These tests verify element coercion to `HEnum` and concrete type selection for field-only and setter-backed abstract collection shapes.

## Verification run

- `mvn -pl juneau-utest -am -Dtest=BeanMap_Test -Dsurefire.failIfNoSpecifiedTests=false test` -> **PASS**
- `mvn -pl juneau-utest -am -Dtest='Hjson*Test,Hocon*Test,Proto*Test,Bson*Test' -Dsurefire.failIfNoSpecifiedTests=false test` -> **PASS**
- `mvn -pl juneau-utest -am -Dtest=EnumFormat_RoundTrip_Test -Dsurefire.failIfNoSpecifiedTests=false test` -> **PASS**
- `./scripts/test.py` -> **PASS** (build + test)

## Scope notes

- Abstract-map default-concrete selection was intentionally left out-of-scope per TODO-59 boundaries; no map behavior changes were made here.
