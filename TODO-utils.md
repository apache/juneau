# TODO: Utility Classes Cleanup and Consolidation Plan (Updated)

This document outlines the remaining work to clean up duplication and consolidate utility classes across the Juneau codebase, centralizing common functionality in `org.apache.juneau.common.utils.*`.

## Completed Work

- Moved `CollectionUtils` to `org.apache.juneau.common.utils.CollectionUtils`.
- Moved `ArrayUtils` to `org.apache.juneau.common.utils.ArrayUtils`.
- Updated imports where applicable and removed the old internal classes.

## Goals

- Deduplicate and move remaining shared utilities into `org.apache.juneau.common.utils` where possible.
- Then deduplicate overlapping methods within `org.apache.juneau.common.utils` itself (remove/merge aliases, prefer single canonical APIs).

## Scope Snapshot

### Common utils (target home)
- `org.apache.juneau.common.utils`: `StringUtils`, `Utils`, `ThrowableUtils`, `IOUtils`, `SystemUtils`, `CollectionUtils`, `ArrayUtils`, `AsciiMap`, `AsciiSet`, `Snippet`, `ThrowingSupplier`.

### Remaining internal utils (candidates to move)
- `org.apache.juneau.internal`: `ClassUtils`, `DateUtils`, `FileUtils`, `HttpUtils`, `AnnotationUtils`, `ConsumerUtils`, `ResourceBundleUtils`, `ConverterUtils` (complex; analyze first).

### Format/area-specific utils (keep local; remove duplication only)
- `org.apache.juneau.xml.XmlUtils`, `org.apache.juneau.uon.UonUtils`, `org.apache.juneau.jsonschema.SchemaUtils`, `org.apache.juneau.jena.RdfUtils`, `org.apache.juneau.rest.util.RestUtils`, `org.apache.juneau.http.remote.RemoteUtils`.

## Phase 1 — Move remaining internal utilities to common (2–3 weeks)

Prioritize simple, low-risk movers first. Keep package-private helpers in internal.

1) Class and reflection utilities → `common.utils.ClassUtils`
- Move: `className`, `simpleClassName`, `getClasses`, `getMatchingArgs`, `isVoid/isNotVoid`, `setAccessible` (Constructor/Field/Method), `toClass`.
- Check overlap: `Utils.classNameOf` vs `ClassUtils.className`.

2) HTTP utilities → `common.utils.HttpUtils`
- Move: `detectHttpMethod`, `detectHttpPath`.
- Consumers: mainly REST/server code.

3) Annotation utilities → `common.utils.AnnotationUtils`
- Move: `equals(Annotation,Annotation)`, `hashCode(Annotation)`.

4) Consumer/predicate utilities → `common.utils.ConsumerUtils`
- Move: `consume(..)`, `test(..)` overloads.

5) Resource bundle utilities → `common.utils.ResourceBundleUtils`
- Move: `empty()`, `findBundle(..)` (no-throw lookup).

6) Date/time utilities → `common.utils.DateUtils`
- Move: `clearThreadLocal`, `formatFor`, `formatDate`, `getFormatter`, `parseISO8601Calendar`, `toValidISO8601DT`.
- Check overlap with `StringUtils` ISO helpers.

7) File utilities → `common.utils.FileUtils`
- Move: `create`, `createTempFile(..)`, `delete`, `exists`, `getBaseName`, `getExtension`, `getFileName`, `hasExtension`, `mkdirs(..)`, `modifyTimestamp`.

8) Converter utilities (analyze before move) → `common.utils.ConverterUtils`
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

- Create per-class tasks:
  - Move `ClassUtils` → common
  - Move `HttpUtils` → common
  - Move `AnnotationUtils` → common
  - Move `ConsumerUtils` → common
  - Move `ResourceBundleUtils` → common
  - Move `DateUtils` → common (+ dedupe with `StringUtils` ISO helpers)
  - Move `FileUtils` → common
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