# Session Options & Allowlists Refactoring Plan

This document describes all changes made to implement serializer/parser session options, allowlists, `noInherit`, and the separation of request attributes from session properties.

---

## Overview

The changes fall into these major categories:

1. **New constants classes** for shared wire names and server-side settings
2. **New annotation properties** (`allowedSerializerOptions`, `allowedParserOptions`, `noInherit`) on `@Rest` and all `@RestOp`-group annotations
3. **Session option allowlists** computed via memoized fields on `RestContext` and `RestOpContext`, with inheritance and negation support
4. **Separation of request attributes from session properties** — `defaultRequestAttributes` no longer auto-merged into session properties
5. **Programmatic session property setters** on `RestRequest`
6. **REST client wire helpers** for passing session options via headers/query params
7. **Swagger documentation** for session option parameters
8. **Test updates** to accommodate the new behavior
9. **Release notes** documenting the changes

---

## 1. New Files (Added)

### `juneau-rest/juneau-rest-common/src/main/java/org/apache/juneau/rest/RestSharedConstants.java`
Central home for static literals shared across REST modules (server, client, mock). Contains:
- `HEADER_JuneauSerializerOptions` = `"X-Juneau-Serializer-Options"`
- `HEADER_JuneauParserOptions` = `"X-Juneau-Parser-Options"`
- `QUERY_juneauSerializerOptions` = `"juneauSerializerOptions"`
- `QUERY_juneauParserOptions` = `"juneauParserOptions"`

### `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestServerConstants.java`
Server-side static literals:
- `PROPERTY_allowedSerializerOptions` = `"allowedSerializerOptions"`
- `PROPERTY_allowedParserOptions` = `"allowedParserOptions"`
- `PROPERTY_noInherit` = `"noInherit"`
- Various `SETTING_sessionOptions_*` fields
- Various `PROP_*` fields for properties output

### `juneau-utest/src/test/java/org/apache/juneau/rest/NoInherit_Test.java`
Tests for `noInherit` behavior:
- `a01_classNoInherit_skipsParentSerializerAllowlist` — class-level `noInherit` blocks parent context
- `a02_withoutNoInherit_mergesParentSerializerAllowlist` — no `noInherit` merges parent+child
- `a03_methodNoInherit_stillInheritsParentMethodSerializerAllowlist` — method `noInherit` does NOT block parent method annotations (only blocks context)
- `a04_aggregatedNoInherit_includesBothParentAndChild` — aggregated `noInherit` for unrelated props doesn't block `allowedSerializerOptions`
- `a05_methodNoInherit_blocksClassLevelButNotParentMethod` — method `noInherit="allowedSerializerOptions"` blocks class-level keys but still inherits parent method keys

### `juneau-utest/src/test/java/org/apache/juneau/rest/RestOpContext_Test.java`
Tests for `RestOpContext` allowlist computation, `noInherit` aggregation, and annotation discovery.

### `juneau-utest/src/test/java/org/apache/juneau/rest/RestRequest_SessionProperties_Test.java`
Tests for `RestRequest.setSerializerSessionProperty()`, `setParserSessionProperty()`, `setSerializerSessionProperties()`, `setParserSessionProperties()`.

---

## 2. Annotation Changes

### New properties on `@Rest` (`Rest.java`, `RestAnnotation.java`):
- `String[] allowedSerializerOptions() default {};` — CDL of allowed serializer session option keys
- `String[] allowedParserOptions() default {};` — CDL of allowed parser session option keys  
- `String[] noInherit() default {};` — CDL of property names to NOT inherit from parent class hierarchy

### New properties on all `@RestOp`-group annotations:
Same three properties (`allowedSerializerOptions`, `allowedParserOptions`, `noInherit`) added to:
- `RestOp.java` / `RestOpAnnotation.java`
- `RestGet.java` / `RestGetAnnotation.java`
- `RestPost.java` / `RestPostAnnotation.java`
- `RestPut.java` / `RestPutAnnotation.java`
- `RestPatch.java` / `RestPatchAnnotation.java`
- `RestDelete.java` / `RestDeleteAnnotation.java`
- `RestOptions.java` / `RestOptionsAnnotation.java`

Each `*Annotation.java` file also has updated `Builder` with fields, getters, setters, and `hashKey()` updates.

Full Javadocs duplicated on each annotation (not cross-referenced to `@RestOp`).

---

## 3. Core Library Changes

### `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/utils/StringUtils.java`
- Added `public static final String[] EMPTY_STRING_ARRAY = new String[0];`

### `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/utils/CollectionUtils.java`
- Added `removeNegations(List<String>)` — removes entries paired with `-entry` tokens
- Added `treeSet(Comparator, Collection)` — creates TreeSet with comparator from collection
- Added `rstream(List)` — reverse stream of a list

### `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/reflect/AnnotationInfo.java`
- Added `getStringArray(String name)` — returns `Optional<String[]>` for annotation attribute
- Added `getAnnotatable()` — returns the `Annotatable` where the annotation was found

### `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ContextSession.java`
- Added `getSessionProperties()` method returning the session property map

### `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BeanSession.java`
- Minor changes for session property support

### `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/serializer/OutputStreamSerializerSession.java`
- Minor changes for session property support

### `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json/JsonSerializerSession.java`
- Minor changes

### `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/bson/BsonParserSession.java`
- Minor changes

### `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/objecttools/ObjectRest.java`
- Minor changes

---

## 4. RestContext Changes

### `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestContext.java`

Major additions:
- **`allowedSerializerOptionsKeys`** memoizer — computes allowed serializer session option keys from `@Rest` annotations:
  1. Inherits from parent context (router hierarchy) unless `noInherit` blocks it
  2. Pre-pass walks nearest-first to find `noInherit` cut-off point
  3. Collects tokens in parent-to-child order (reversed) via `rstream()` so child negation tokens (`-key`) override parent additions
  4. Calls `removeNegations()` for deferred negation resolution
- **`allowedParserOptionsKeys`** memoizer — same pattern for parser options
- **`getRestAnnotationsForProperty(String name)`** helper — finds cut-off via `noInherit`, returns reversed stream
- **`noInherit`** memoizer — aggregates `noInherit` tokens from nearest `@Rest`
- **`isInherited(String property)`** — checks if a property is blocked by `noInherit` (for parent context inheritance)
- **`getRestAnnotations()`** — returns all `@Rest` annotations on the class hierarchy
- **`getRestAnnotation()`** — returns the nearest (first) `@Rest` annotation
- **`resolveCdl(String...)`** — resolves comma-delimited annotation values with SVL variable resolution
- **`reset()`** — clears memoized allowlists and cascades to op contexts
- Removed `EMPTY_STRING_ARRAY` local constant (now in `StringUtils`)
- Added `import static` for `StringUtils.EMPTY_STRING_ARRAY`

---

## 5. RestOpContext Changes

### `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestOpContext.java`

Major additions:
- **`allowedSerializerOptionsKeys`** memoizer:
  1. Adds context-level keys from `RestContext.getAllowedSerializerOptionsKeys()` if not blocked by aggregated `noInherit`
  2. Always inherits method-level keys from the full method override chain (child + parent methods) — `noInherit` does NOT cut off method-chain climbing
- **`allowedParserOptionsKeys`** memoizer — same pattern
- **`noInherit`** memoizer — aggregates `noInherit` tokens from ALL `@RestOp`-group annotations on the method override chain
- **`isInherited(String property)`** — checks aggregated `noInherit` for context-level inheritance
- **`getRestOpAnnotations()`** — returns all `@RestOp`-group annotations (including `@RestGet`, etc.) on the method and parent methods, child-to-parent order, using `AnnotationInfo.isInGroup(RestOp.class)`
- **`resolveCdl(String...)`** — resolves comma-delimited annotation values
- **`reset()`** — clears memoized allowlists
- Removed `EMPTY_STRING_ARRAY` local constant (now in `StringUtils`)

### Inheritance semantics (two types):
1. **Context inheritance** (from `RestContext`) — controlled by `noInherit`. When `noInherit="allowedSerializerOptions"`, blocks values from the `@Rest` class annotation
2. **Method annotation inheritance** (from parent method `@RestOp` annotations) — ALWAYS inherited, regardless of `noInherit`

---

## 6. RestRequest Changes

### `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/RestRequest.java`

Major additions:
- **`setSerializerSessionProperty(String name, Object value)`** — sets a single serializer session property
- **`setParserSessionProperty(String name, Object value)`** — sets a single parser session property
- **`setSerializerSessionProperties(Map<String,Object> values)`** — sets bulk serializer session properties
- **`setParserSessionProperties(Map<String,Object> values)`** — sets bulk parser session properties
- **`getSerializerSessionPropertyMap()`** — computes effective serializer session properties by merging:
  1. Query parameter `juneauSerializerOptions` (UON format)
  2. Header `X-Juneau-Serializer-Options` (JSON5 format)
  3. Programmatic `serializerSessionProperties` set via setters
  - Validates all keys against the allowlist; throws `BadRequest` for disallowed keys
- **`getParserSessionPropertyMap()`** — same for parser options
- **`isPlainText()`** — checks for `plainText: true` header
- **Private helpers**: `merge(Map...)`, `badRequest(...)`, `parseUonMap(...)`, `parseJsonMap(...)` — all static
- **Removed**: Request attributes (`getAttributes().asMap()`) are no longer merged into session property maps

---

## 7. REST Client Changes

### `juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/RestClient.java`
- Added default session option header/query parameter methods on `RestClient.Builder`
- Uses `RestSharedConstants` wire names

### `juneau-rest/juneau-rest-client/src/main/java/org/apache/juneau/rest/client/RestRequest.java`
- Added per-request session option header/query parameter methods
- Uses `RestSharedConstants` wire names

### `juneau-rest/juneau-rest-mock/src/main/java/org/apache/juneau/rest/mock/MockRestClient.java`
- Overrides session option methods for fluent typing

### `juneau-rest/juneau-rest-mock/src/main/java/org/apache/juneau/rest/mock/MockRestRequest.java`
- Overrides session option methods for fluent typing

---

## 8. Serialized POJO Processor Changes

### `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/processor/SerializedPojoProcessor.java`
- Now calls `req.getSerializerSessionPropertyMap()` to get session properties and passes them to serializer via `.properties()`

### `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/httppart/RequestContent.java`
- Now calls `req.getParserSessionPropertyMap()` to get parser session properties

---

## 9. Swagger Changes

### `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/swagger/BasicSwaggerProviderSession.java`
- Generated Swagger now includes optional documentation parameters for session options:
  - Headers: `X-Juneau-Serializer-Options`, `X-Juneau-Parser-Options`
  - Query params: `juneauSerializerOptions`, `juneauParserOptions`

---

## 10. Config Changes

### `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/config/BasicUniversalConfig.java`
- Added `allowedSerializerOptions="binaryFormat"` and `allowedParserOptions="binaryFormat"` to `@Rest`

### `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/config/DefaultConfig.java`
- Minor changes to allowed header params

---

## 11. Test Updates

### Tests updated to use new programmatic API (separated attributes from session properties):

**`Nls_Test.java`**:
- Removed `defaultRequestAttributes` for session properties from `@Rest`/`@RestGet`
- Methods now accept `RestRequest req` and call `req.setSerializerSessionProperty("TestProperty", req.getMessage("key1"))` explicitly
- Removed fully-qualified `org.apache.juneau.rest.RestRequest` references

**`Rest_DefaultRequestAttributes_Test.java`**:
- Test A (parser): Added `req.setParserSessionProperties(attrs.asMap())` in `@RestPreCall` and in REST method `b()`
- Test B (serializer): Added `req.setSerializerSessionProperties(attrs.asMap())` in `@RestPostCall` and in REST method `b()`

**`Rest_RVars_Test.java`**:
- Added `RestRequest req` parameter to method `a()`
- Added `req.setSerializerSessionProperties(req.getAttributes().asMap())` to bridge attributes to session properties

**`RestClient_Headers_Test.java`**:
- Added `allowedSerializerOptions="simple"` and `allowedParserOptions="addBeanTypes"` to `@RestGet` annotation on `headers()` method in class `A`

**`ContentComboTestBase.java`**:
- MsgPack client: added `queryData("juneauSerializerOptions", "(binaryFormat=BASE64)")` to send session options

### Annotation test updates:
- `RestAnnotation_Test.java` — tests for new `allowedSerializerOptions`, `allowedParserOptions`, `noInherit` properties
- `RestOpAnnotation_Test.java` — same
- `RestGetAnnotation_Test.java` — same
- `RestPostAnnotation_Test.java` — same
- `RestPutAnnotation_Test.java` — same
- `RestDeleteAnnotation_Test.java` — same

### Other test updates:
- `CollectionUtils_Test.java` — tests for `removeNegations()`, `treeSet(Comparator, Collection)`, `rstream()`
- `AnnotationInfo_Test.java` — tests for `getStringArray()`, `getAnnotatable()`
- `RestClient_Query_Test.java` — tests for session option query parameters

---

## 12. Release Notes

### `/docs/pages/release-notes/9.2.1.md` (docs repo, untracked)
Added sections:
- **Request Attributes and Session Properties Separation** — breaking change, new API, migration guidance with code examples
- **`noInherit` Inheritance Semantics** — clarified class-level vs method-level inheritance, with code example showing `noInherit` blocks class-level but not parent method-level keys

---

## Key Design Decisions

### `noInherit` semantics:
1. **On `@Rest`**: Controls two things:
   - Blocks inheritance from parent context (router hierarchy)
   - Stops climbing the class hierarchy annotations (the for-loop breaks)
2. **On `@RestOp`-group annotations**: Only blocks inheritance from `RestContext` (class-level); does NOT cut off method override chain climbing — parent method annotations are always inherited

### Annotation hierarchy walking in `RestContext`:
- Pre-pass walks nearest-first to find `noInherit` cut-off
- Collection pass walks parent-to-child (reversed via `rstream()`) so child negation tokens override parent values
- `removeNegations()` does deferred resolution at the end

### Annotation hierarchy walking in `RestOpContext`:
- Always iterates ALL method annotations (no cut-off for method chain)
- `isInherited()` only controls whether to include `RestContext`-level keys
