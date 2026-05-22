# FINISHED-14 — Close the latent map-key coercion gap in `BeanPropertyMeta.setPropertyValue`

Completed: 2026-05-22

## Outcome

TODO-14 is complete. `BeanPropertyMeta.setPropertyValue(...)` now performs symmetric key/value type checks and coercion for typed `Map<K,V>` properties, so `BeanMap.put(...)` no longer leaves wire-form keys (for example `String`) in typed-key maps (for example `Map<Enum, String>`).

## Implemented changes

### 1) Commons-side production fix

File:
- `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/bean/BeanPropertyMeta.java`

Changes:
- In the typed-map branch of `setPropertyValue(...)`, pulled `keyType` from `rawTypeMeta.getKeyType()` alongside `valueType`.
- Extended `needsConversion` detection to include key-side mismatches:
  - `nn(k) && ! keyType.isObject() && ! keyType.isInstance(k)`
- Kept value-side mismatch detection in place.
- Replaced the map `forEach` write loop with an explicit `for (Map.Entry...)` loop so both key and value can be reassigned after conversion.
- Added key-side conversion before insertion:
  - `if (! keyType.isObject()) k1 = session.convertToType(k1, keyType);`
- Retained value-side conversion behavior.

Result:
- Both abstract-map and concrete-map writable paths now coerce keys to the declared key type.

### 2) Regression/coverage test additions

File:
- `juneau-utest/src/test/java/org/apache/juneau/BeanMap_Test.java`

Added tests:
- `a41_typedMapField_coercesStringKeysToEnum`
  - Exercises `BeanMap.put("m", Map<String,String>)` into `Map<HEnum,String>` field property.
- `a42_typedMapSetter_coercesStringKeysToEnum`
  - Exercises setter-backed `HashMap<HEnum,String>` property.

Assertions verify:
- Enum-key lookups succeed.
- Raw `"ONE"` key is absent after coercion.
- All resulting keys are `HEnum` instances.

## Verification run

### Focused area
- `mvn -pl juneau-utest -am -Dtest=BeanMap_Test -Dsurefire.failIfNoSpecifiedTests=false test`
  - `org.apache.juneau.BeanMap_Test`: **46 run, 0 failures, 0 errors**

### Parser regression suite (Bug #7b parser families)
- `mvn -pl juneau-utest -am -Dtest='Hjson*Test,Hocon*Test,Proto*Test,Bson*Test' -Dsurefire.failIfNoSpecifiedTests=false test`
  - Hjson/Hocon/Proto/Bson suites remained green (no failures/errors).

### Enum matrix acceptance check
- `mvn -pl juneau-utest -am -Dtest=EnumFormat_RoundTrip_Test -Dsurefire.failIfNoSpecifiedTests=false test`
  - `EnumFormat_RoundTrip_Test`: **2268 run, 0 failures, 0 errors, 0 skipped**

### Broader suite
- `./scripts/test.py`
  - Build phase: success
  - Test phase: success

## Phase 4 audit summary (sibling-shape pass)

Reviewed:
- `BeanPropertyMeta.set(...)` dispatch path
- `BeanPropertyMeta.setPropertyValue(...)` map/collection branches
- dyna-property path (`isDyna`) in setter/getter dispatch

Findings:
- No additional `Map<K,V>` key-side mismatch/coercion gaps remain in this method.
- `Collection` branch handles only element type (`E`) and has no key-side analog.
- Public `set(...)` path routes through the same corrected `setPropertyValue(...)` map logic.
- Dyna property writes are name-based map/bean extras routing and do not represent typed `Map<K,V>` key coercion semantics.

Disposition:
- No new TODO was required from this audit pass for TODO-14 scope.
- Existing separate work on sibling shapes (for example typed set element coercion) remains tracked independently (see TODO-58).
