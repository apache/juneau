# TODO: Utility Classes Cleanup and Consolidation Plan (Updated)

This document outlines the remaining work to clean up duplication and consolidate utility classes across the Juneau codebase, centralizing common functionality in `org.apache.juneau.common.utils.*`.

## Completed Work

- Moved `CollectionUtils` to `org.apache.juneau.common.utils.CollectionUtils`.
- Moved `ArrayUtils` to `org.apache.juneau.common.utils.ArrayUtils`.
- Updated imports where applicable and removed the old internal classes.
- **Static Import Cleanup**: Replaced `Utils.` calls with direct static imports in high-priority files:
  - `StringUtils.java` - 22 Utils calls replaced
  - `UonParserSession.java` - 2 Utils calls replaced  
  - `CallLogger.java` - 8 Utils calls replaced
  - `RestContext.java` - 19 Utils calls replaced
  - `ConfigMap.java` - 9 Utils calls replaced
  - `SpringBeanStore.java` - 5 Utils calls replaced
  - `UrlPath.java` - 4 Utils calls replaced
  - `BeanMeta.java` - 2 Utils calls replaced
  - `HtmlParserSession.java` - 1 Utils call replaced
  - `HtmlSerializerSession.java` - 2 Utils calls replaced
  - `CsvSerializerSession.java` - 1 Utils call replaced
  - `JsonParserSession.java` - 1 Utils call replaced
- **New Utility Classes**: Created `PredicateUtils` with `and()`, `or()`, and `isType()` methods
- **Builder Classes**: Added comprehensive Javadocs and unit tests for `ListBuilder`, `MapBuilder`, `SetBuilder`
- **Convenience Methods**: Added Javadocs for `CollectionUtils.listb()`, `setb()`, `mapb()` methods
- **Utility Consolidation**: 
  - `AnnotationUtils` moved to `org.apache.juneau.common.utils.AnnotationUtils`
  - `FileUtils` moved to `org.apache.juneau.common.utils.FileUtils`
  - `HttpUtils` moved to `org.apache.juneau.rest.common.HttpUtils` (REST-specific location)
  - `ConsumerUtils` removed (functionality consolidated elsewhere)

## Goals

- Deduplicate and move remaining shared utilities into `org.apache.juneau.common.utils` where possible.
- Then deduplicate overlapping methods within `org.apache.juneau.common.utils` itself (remove/merge aliases, prefer single canonical APIs).
- Complete static import cleanup for remaining `Utils.` references.

## Remaining Static Import Work

### High Priority Files (2 remaining)
- `juneau-microservice/juneau-microservice-jetty/src/main/java/org/apache/juneau/microservice/jetty/JettyMicroservice.java` (2 `Utils.` calls)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/urlencoding/UrlEncodingParserSession.java` (1 `Utils.` call)

### Medium Priority Files (8 remaining)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/JsonList.java` (1 `Utils.` call - `isEmpty` conflict resolved)
- `juneau-rest/juneau-rest-server/src/main/java/org/apache/juneau/rest/util/RestUtils.java` (multiple `Utils.` calls)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/internal/CollectionUtils.java` (1 `Utils.` call)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlParserSession.java` (1 `Utils.` call)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlSerializerSession.java` (2 `Utils.` calls)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/csv/CsvSerializerSession.java` (1 `Utils.` call)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json/JsonParserSession.java` (1 `Utils.` call)
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/reflect/ExecutableInfo.java` (1 `Utils.` call)

### Low Priority Files (10+ remaining)
- Various other files with 1-2 `Utils.` calls each

## Scope Snapshot

### Common utils (target home)
- `org.apache.juneau.common.utils`: `StringUtils`, `Utils`, `ThrowableUtils`, `IOUtils`, `SystemUtils`, `CollectionUtils`, `ArrayUtils`, `AsciiMap`, `AsciiSet`, `Snippet`, `ThrowingSupplier`, `AnnotationUtils`, `FileUtils`.

### Remaining internal utils (candidates to move)
- `org.apache.juneau.internal`: `ClassUtils`, `DateUtils`, `ResourceBundleUtils`, `ConverterUtils` (complex; analyze first).

### Format/area-specific utils (keep local; remove duplication only)
- `org.apache.juneau.xml.XmlUtils`, `org.apache.juneau.uon.UonUtils`, `org.apache.juneau.jsonschema.SchemaUtils`, `org.apache.juneau.jena.RdfUtils`, `org.apache.juneau.rest.util.RestUtils`, `org.apache.juneau.http.remote.RemoteUtils`, `org.apache.juneau.rest.common.HttpUtils`.

## Phase 1 — Move remaining internal utilities to common (2–3 weeks)

Prioritize simple, low-risk movers first. Keep package-private helpers in internal.

1) Class and reflection utilities → `common.utils.ClassUtils`
- Move: `className`, `simpleClassName`, `getClasses`, `getMatchingArgs`, `isVoid/isNotVoid`, `setAccessible` (Constructor/Field/Method), `toClass`.
- Check overlap: `Utils.classNameOf` vs `ClassUtils.className`.

2) Resource bundle utilities → `common.utils.ResourceBundleUtils`
- Move: `empty()`, `findBundle(..)` (no-throw lookup).

3) Date/time utilities → `common.utils.DateUtils`
- Move: `clearThreadLocal`, `formatFor`, `formatDate`, `getFormatter`, `parseISO8601Calendar`, `toValidISO8601DT`.
- Check overlap with `StringUtils` ISO helpers.

4) Converter utilities (analyze before move) → `common.utils.ConverterUtils`
- Complex swap/type conversion paths; validate dependencies to avoid cycles.

Deliverable for Phase 1:
- New classes in `common.utils` with parity methods.
- All imports updated.
- Old internal classes removed (or deprecated then removed after migration window).

## Phase 2 — Deduplicate within common utils (1–2 weeks)

Canonicalize APIs and remove internal duplication/aliases.

1) Class name retrieval duplication
- Duplicate: `Utils.classNameOf(Object)` vs `ClassUtils.className(Object)`.
- Action: Keep `ClassUtils.className`, deprecate `Utils.classNameOf`, update call sites.

2) Date parsing/formatting duplication
- Potential overlap: `StringUtils.parseIsoDate` / `toIsoDate` vs `DateUtils.parseISO8601Calendar` / `formatDate`.
- Action: Centralize in `DateUtils`; `StringUtils` delegates or deprecates.

3) Array/list conversion duplication
- Duplicate: `Utils.arrayToList(Object)` vs `ArrayUtils.toObjectList(Object)`.
- Action: Keep `ArrayUtils.toObjectList`, deprecate `Utils.arrayToList`, update call sites.

4) Collection/array creation duplication
- Duplicate: `Utils.array(Collection, Class)` vs `ArrayUtils.toArray(Collection, Class)`.
- Action: Keep `ArrayUtils.toArray`, deprecate `Utils.array`, update call sites.
- Clarify `Utils.alist(..)` vs `CollectionUtils.addAll(..)` use-cases (creation vs mutation) and document.

5) Naming and placement normalization
- Ensure class-appropriate placement (e.g., file ops in `FileUtils`, reflection in `ClassUtils`).
- Prefer singular canonical method names; remove `eq/eqic` style duplicates where clearer names exist.

Deliverable for Phase 2:
- Deprecated methods annotated and documented with replacements.
- Updated call sites across modules.
- Removal of deprecated methods after migration window (separate task).

## Non-goals

- Do not move format/area-specific helpers (e.g., `XmlUtils`, `UonUtils`) unless they contain generic logic duplicated in common utils; only deduplicate by calling common utils.
- Do not change public behavior; API renames go through deprecation first.

## Risks and mitigations

- API churn: Use deprecations and migration notes; batch updates per module.
- Cycles from `ConverterUtils`: Validate dependencies; split if needed (e.g., `TypeUtils`, `SwapUtils`).
- Test stability: Run full build after each module batch; prioritize high-coverage modules first.

## Testing strategy

- Unit tests for moved methods in new locations.
- Search-and-replace assisted updates; compile after each batch.
- Run utests and REST integration tests; smoke-test examples.

## Tracking tasks

### Completed Tasks
- ✅ Static import cleanup for high-priority files (12 files completed)
- ✅ Created `PredicateUtils` utility class
- ✅ Added Javadocs and unit tests for builder classes
- ✅ Added Javadocs for convenience methods
- ✅ Moved `AnnotationUtils` to common utils
- ✅ Moved `FileUtils` to common utils
- ✅ Moved `HttpUtils` to REST common package
- ✅ Removed `ConsumerUtils` (consolidated elsewhere)

### Remaining Static Import Tasks
- 🔄 Complete high-priority files (2 remaining)
- 🔄 Complete medium-priority files (8 remaining)  
- 🔄 Complete low-priority files (10+ remaining)

### Future Utility Consolidation Tasks
- Create per-class tasks:
  - Move `ClassUtils` → common
  - Move `ResourceBundleUtils` → common
  - Move `DateUtils` → common (+ dedupe with `StringUtils` ISO helpers)
  - Analyze/move `ConverterUtils` → common (or split)
- Create dedupe tasks:
  - Deprecate `Utils.classNameOf` → `ClassUtils.className`
  - Deprecate `Utils.arrayToList` → `ArrayUtils.toObjectList`
  - Deprecate `Utils.array` → `ArrayUtils.toArray`
  - Centralize ISO date helpers in `DateUtils`; deprecate `StringUtils` variants

## Outcome

- Common, centralized utility surface in `org.apache.juneau.common.utils`.
- Reduced duplication and clearer ownership per utility area.
- Easier discoverability and maintenance across the codebase.