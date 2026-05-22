# Decouple `juneau-rest-common` from `juneau-marshall`

## Outcome (rescope, 2026-05-22)

Original goal - fully break the `juneau-rest-common -> juneau-marshall` compile
dependency - abandoned. The decisive blocker is Step 6 (`ClassMeta` /
`MarshallingContext` / `AnnotationWorkList` references in `HttpParts` and the
`*BeanMeta` classes), which depended on TODO-30 landing. TODO-30 was also
abandoned (see `FINISHED-30-classmeta-to-commons.md`). Without `ClassMeta` in
commons, `rest-common` cannot drop the marshall dependency.

The four low-effort moves that don't depend on `ClassMeta` are useful on their
own merits and have been carried forward into TODO-60 (`TODO-60-low-effort-
marshall-to-commons-moves.md`):

- Step 1: `Content.java` javadoc (`{@link JsonSchemaSerializer}` go fully-qualified).
- Step 2: `InvalidAnnotationException` -> commons.
- Step 3: `SchemaAnnotation` / `ExternalDocsAnnotation` / `ItemsAnnotation`
  / `SubItemsAnnotation` -> commons.
- Step 4: drop or relocate `@Marshalled(as=MarshalledAs.STRING)` on `EntityTags`.

Steps 5-7 are intentionally dropped:

- Step 5 (`HttpPartSchema` + `HttpPartSerializer/Parser` interfaces -> commons)
  paid off mostly when paired with the dep-break goal.
- Steps 6-7 (`ClassMeta` resolution + pom flip) are ClassMeta-blocked.

The original analysis below is preserved verbatim for historical context.

---

Target module chain:

```
juneau-commons -> juneau-rest-common -> juneau-marshall -> juneau-rest-client
                                                        -> juneau-rest-server
```

`juneau-rest-common/pom.xml` still declares a compile dep on `juneau-marshall`. The remaining gap below is the work needed to drop it.

> Plan refreshed 2026-05-20 after the TODO-38 / TODO-40 / TODO-42 landings. The previous revision is preserved by `git log` on this file.

---

## Already landed (no further action)

- **Phase 1a — commons HTTP types** — `MediaRange` / `MediaRanges` / `MediaType` / `StringRange` / `StringRanges` in `org.apache.juneau.commons.http`.
- **Phase 1b — commons annotations** — `@Schema` / `@Items` / `@SubItems` / `@ExternalDocs` annotation types in `org.apache.juneau.commons.annotation`. The runtime `AnnotationObject` / `AppliedAnnotationObject` / `AppliedOnClassAnnotationObject` / `AnnotationGroup` proxy infrastructure also lives in commons.
- **Phase 2 (initial) — httppart enums + exception in commons** — `HttpPartType`, `HttpPartFormat`, `HttpPartDataType`, `HttpPartCollectionFormat`, `SchemaValidationException` in `org.apache.juneau.commons.httppart`.
- **SVL in commons (TODO-14)** — `VarResolverSession` and friends in `org.apache.juneau.commons.svl`; `HeaderList` / `PartList` no longer pull marshall for variable resolution.
- **BeanCreator → BeanInstantiator (TODO-15)** — `RequestBeanMeta` / `RequestBeanPropertyMeta` / `ResponseBeanPropertyMeta` use `org.apache.juneau.commons.inject.BeanInstantiator`.
- **`juneau-assertions` direct dep** — `juneau-rest-common/pom.xml` declares `juneau-assertions` directly.
- **`Serialized*` bridges moved (TODO-42)** — `SerializedHeader`, `SerializedPart`, `SerializedEntity` and their factory overloads relocated to the new `juneau-rest-common-classic` module under `org.apache.juneau.http.classic.{header,part,entity}`. Rest-common no longer ships these types, so the original "Phase 3" step is satisfied by module split rather than by moving them to marshall.
- **`BasicMediaTypeHeader` javadoc imports** — file moved into `juneau-rest-common-classic` as part of TODO-42, so its `org.apache.juneau.json.*` / `org.apache.juneau.json5.Json5Serializer` imports are no longer a rest-common concern.
- **`AnnotationProvider` / `AnnotationInfo` already in commons** — `RequestBeanPropertyMeta` and `ResponseBeanMeta` resolve these from commons; only the `org.apache.juneau.*` wildcard imports in the four `httppart/bean/*` classes still pull marshall symbols.
- **TODO-40 fallout** — large set of transport-neutral types added to rest-common (`HttpHeaderList`, `HttpProtocolVersion`, `HttpRequestLine`, `HttpRequestLineBean`, `HttpResourceBean`, the typed `Http*Header` / `Http*Part` family, the fluent setters on the response classes). None of this widened the marshall surface — every new file imports only commons / java SE.

> The plan from here lists **only** what is still required to drop the `juneau-marshall` dep from rest-common.

---

## Current marshall surface still imported by rest-common

Verified 2026-05-20 by reading every file under `juneau-rest-common/src/main/java` that imports from a marshall-only package.

### A. Httppart marshalling surface (the big one)

Symbols still in `org.apache.juneau.httppart` (marshall):

- `HttpPartSchema` (+ `Builder`, `DEFAULT`)
- `HttpPartSerializer` / `HttpPartSerializerSession`
- `HttpPartParser` / `HttpPartParserSession`

Rest-common consumers:

- `httppart/bean/RequestBeanMeta`
- `httppart/bean/RequestBeanPropertyMeta`
- `httppart/bean/ResponseBeanMeta`
- `httppart/bean/ResponseBeanPropertyMeta`

(The `HttpPart` interface / `HttpPartMarshalling` facade originally listed here are not actually imported by any rest-common source today — only the four bean-meta classes still hold this surface.)

### B. `InvalidAnnotationException`

- Lives in `org.apache.juneau.annotation` (marshall). Extends `org.apache.juneau.BasicRuntimeException`, which is a shim over the commons version.
- Used by `httppart/bean/MethodInfoUtils` (4 throw sites) and `httppart/bean/ResponseBeanMeta` (static-imports `assertNoInvalidAnnotations`).

### C. `*Annotation` companion helpers

The annotation **types** are already in commons (Phase 1b), but their static `*.DEFAULT` / `empty()` helpers still live in marshall:

- `SchemaAnnotation` (`org.apache.juneau.annotation`) — used by `http/annotation/ResponseAnnotation.java`.
- `ExternalDocsAnnotation` (`org.apache.juneau.annotation`) — used by `http/annotation/TagAnnotation.java`.
- `ItemsAnnotation` / `SubItemsAnnotation` — siblings; not yet imported from rest-common but moving the group together avoids leaving stragglers behind.

### D. `@Marshalled(as=MarshalledAs.STRING)`

- `EntityTags.java` declares `@Marshalled(as=MarshalledAs.STRING)`. The annotation lives in `org.apache.juneau.annotation` (marshall).
- This is a marshall-runtime hint for parser/serializer routing. It can be dropped from rest-common (no functional change to rest-common itself) or the annotation can move to commons.

### E. `ClassMeta` + `MarshallingContext` + `AnnotationWorkList`

Top-level marshall types reached via `import org.apache.juneau.*` in:

- `http/HttpParts.java` — `ClassMeta<?>` used in 8 method signatures / function fields (`HEADER_NAME_FUNCTION`, `QUERY_NAME_FUNCTION`, `getName`, `isHttpPart`, …).
- `httppart/bean/RequestBeanMeta.java` — `ClassMeta`, `MarshallingContext.DEFAULT`, `AnnotationWorkList`.
- `httppart/bean/ResponseBeanMeta.java` — same triple, plus the `Value` reference (which is already in commons but reached via the wildcard).

Removing this dependency depends on **TODO-30** (moving `ClassMeta` and related non-marshalling type metadata into `juneau-commons`). Rest-common cannot fully drop marshall until either `ClassMeta` moves (preferred) or these APIs are reworked to use `ClassInfo` (already in commons) plus annotation-driven helpers.

### F. Javadoc-only marshall imports

Real `import` statements, but only used inside `{@link ...}` comments:

- `http/annotation/Content.java` — `import org.apache.juneau.json.JsonSchemaSerializer;` (used once in javadoc).
- `http/annotation/ContactAnnotation.java`, `LicenseAnnotation.java`, `ResponseAnnotation.java`, `TagAnnotation.java`, `http/header/ContentType.java` — fully-qualified `{@link org.apache.juneau.MarshallingContext.Builder#annotations(Annotation...)}` / `Json5Serializer` references in comments. No import statement; cosmetic only.

---

## Plan

### Step 1 — Quick win

**Difficulty:** Trivial. **Removes 1 marshall import.**

- In `Content.java`, replace `import org.apache.juneau.json.JsonSchemaSerializer;` + `{@link JsonSchemaSerializer}` with fully-qualified `{@link org.apache.juneau.json.JsonSchemaSerializer}`.

### Step 2 — Move `InvalidAnnotationException` to commons

**Difficulty:** Low.

1. Add `org.apache.juneau.commons.annotation.InvalidAnnotationException` extending `org.apache.juneau.commons.BasicRuntimeException`. Carry `assertNoInvalidAnnotations(MethodInfo, Class<? extends Annotation>...)` with it.
2. Make the existing marshall `org.apache.juneau.annotation.InvalidAnnotationException` a deprecated subclass of the commons version for back-compat (mirrors the `BasicRuntimeException` shim pattern).
3. Repoint `httppart/bean/MethodInfoUtils` and `httppart/bean/ResponseBeanMeta` at the commons class.

After this, rest-common stops importing `org.apache.juneau.annotation.InvalidAnnotationException`.

### Step 3 — Move `*Annotation` companion helpers to commons

**Difficulty:** Low–Medium. Mechanical move; the helpers are pure data wrappers around already-commons annotation types.

Move from `org.apache.juneau.annotation` (marshall) → `org.apache.juneau.commons.annotation` (commons):

- `SchemaAnnotation` (+ `Builder`, `DEFAULT`, `empty(Schema)`).
- `ExternalDocsAnnotation`.
- `ItemsAnnotation`.
- `SubItemsAnnotation`.

Risks / hand-offs:

- `SchemaAnnotation` static-imports `org.apache.juneau.jsonschema.SchemaUtils.*`. `SchemaUtils` itself stays in marshall (it pulls `JsonList` / `JsonMap`). Either inline the small set of helpers it uses or factor out a commons-side `SchemaUtilsLite` that exposes just the methods `SchemaAnnotation` needs. The methods at play are mostly value-coercion helpers; the body is short.
- `SchemaAnnotation` imports `org.apache.juneau.parser.ParseException` in its `joinnl` / array-parse paths. Replace with `SchemaValidationException` (already in commons) or with `IllegalArgumentException`.
- Leave deprecated shims at the old FQCNs for one release.

After this, `ResponseAnnotation.schema = SchemaAnnotation.DEFAULT` and `TagAnnotation.externalDocs = ExternalDocsAnnotation.DEFAULT` resolve to commons classes.

### Step 4 — Drop or relocate `@Marshalled(as=MarshalledAs.STRING)` on `EntityTags`

**Difficulty:** Trivial-to-Low.

Two options:

- **(A) Drop the annotation.** `EntityTags` is a value wrapper around `EntityTag[]`. The `@Marshalled` hint tells marshall to treat it as a string (using `toString()`). Verify with the existing `EntityTags_Test` suite that round-trip JSON/XML behavior matches without the annotation; if it does, simply remove it.
- **(B) Move `@Marshalled` + `MarshalledAs` to commons.** They are interface-only types; nothing prevents the move other than the cascade of consumers in marshall.

Recommendation: **(A)** unless a test regression appears. Lowest churn.

### Step 5 — Move httppart marshalling interfaces to commons

**Difficulty:** Medium. Single largest remaining item.

Move into `org.apache.juneau.commons.httppart`:

- `HttpPartSchema` (+ `Builder`).
- `HttpPartSerializer` / `HttpPartSerializerSession` interfaces (`Creator` inner-classes optional — they can stay in marshall as long as the interface is in commons).
- `HttpPartParser` / `HttpPartParserSession` interfaces.

Concrete implementations (`SimplePartSerializer/Parser`, `BaseHttpPartSerializer/Parser`, OpenAPI- / URL-encoded-backed serializers, `HttpPartMarshalling` annotation handling) stay in marshall and `implement` the commons-side interfaces.

Known hand-offs / blockers:

- `HttpPartSchema.Builder.apply(HttpPartMarshalling)` ties to marshall's annotation handling. Either move the `@HttpPartMarshalling` annotation type to commons, or split the apply logic so the annotation-driven branch stays in marshall and the commons-side schema exposes a pluggable hook.
- References to `*Annotation.empty()` (`ItemsAnnotation.DEFAULT`, `SubItemsAnnotation.DEFAULT`) inside `HttpPartSchema` resolve once Step 3 lands.
- `HttpPartSchema` references `ParseException` from marshall in schema-validation paths — switch to commons `SchemaValidationException` or a small commons exception.
- Leave deprecated shims at the old FQCNs for one release.

After Steps 2–5 land, the only remaining marshall coupling is the `ClassMeta` / `MarshallingContext` / `AnnotationWorkList` surface in Step 6.

### Step 6 — Resolve `ClassMeta` / `MarshallingContext` / `AnnotationWorkList` references

**Blocked on TODO-30** (`ClassMeta` → commons feasibility pass).

Two paths once TODO-30 lands:

- **(A) `ClassMeta` + friends move to commons** — rest-common keeps `getBeanInfo()` / `ClassMeta<?>` surface unchanged.
- **(B) `ClassMeta` stays in marshall** — rewrite `HttpParts` helpers, `RequestBeanMeta.getBeanInfo()`, `ResponseBeanMeta.getBeanInfo()`, the `RequestBeanMeta.Builder.apply(Class<?>)` / `ResponseBeanMeta.Builder.apply(Type)` paths to expose `ClassInfo` (already in commons) plus an annotation-driven helper, removing the `ClassMeta` surface from rest-common. `AnnotationWorkList` is a thin wrapper over `AnnotationInfo` (commons) that would need a commons-side equivalent or to be inlined into the caller.

This is the **final blocker** before rest-common can drop marshall from its pom.

### Step 7 — Pom flip

Once Steps 1-6 are merged:

- Remove the `juneau-marshall` `<dependency>` from `juneau-rest-common/pom.xml`.
- Re-run `mvn -pl juneau-rest/juneau-rest-common -am clean install` to confirm rest-common compiles against commons + assertions only.
- Re-run the reactor; rest-client / rest-server / rest-mock keep marshall.

`.classpath` requires no edit — `juneau-rest-common/.classpath` uses the `MAVEN2_CLASSPATH_CONTAINER`, so the marshall entry is implicit via the pom.

---

## Dependency surface (pending rows only)

| Priority | Area | Target |
|----------|------|--------|
| 1 | `Content.java` javadoc-only `JsonSchemaSerializer` import | inline fully-qualified `{@link}` |
| 2 | `InvalidAnnotationException` | commons (`org.apache.juneau.commons.annotation`) |
| 3 | `SchemaAnnotation` / `ExternalDocsAnnotation` / `ItemsAnnotation` / `SubItemsAnnotation` | commons (`org.apache.juneau.commons.annotation`) |
| 4 | `@Marshalled(as=...)` on `EntityTags` | drop, or move `@Marshalled`/`MarshalledAs` to commons |
| 5 | `HttpPartSchema` + `HttpPartSerializer/Parser` interfaces | commons (`org.apache.juneau.commons.httppart`) |
| 6 | `ClassMeta` / `MarshallingContext` / `AnnotationWorkList` in `HttpParts` / `*BeanMeta` | depends on TODO-30 outcome |
| 7 | Drop `juneau-marshall` dep from rest-common pom | rest-common build |

---

## Risk notes

- **Split-package risk** — `org.apache.juneau.httppart` exists in both marshall (concrete impls) and commons (enums + interfaces). Keep new moves under `org.apache.juneau.commons.httppart` to avoid worsening the split. OSGi consumers already see this split for the enums.
- **Binary compatibility** — Steps 2, 3, and 5 must leave deprecated shims at the old marshall FQCNs (`InvalidAnnotationException`, `SchemaAnnotation`, `HttpPartSchema`, etc.) extending / type-aliasing the commons versions for at least one release.
- **`@Schema` ↔ serializer integration** — the schema-aware OpenAPI / URL-encoding serializer paths in marshall must continue to wire through the commons-side `HttpPartSchema` after the move. Existing schema-validation tests in `juneau-utest` cover this.
- **Step 5 ↔ Step 3 ordering** — `HttpPartSchema` references `*Annotation.empty()` and `*Annotation.DEFAULT`. Step 3 must land before (or as part of) Step 5, otherwise the commons-side `HttpPartSchema` would have to import from marshall.
- **Keep phases independently buildable** — each step should be its own commit / PR; the reactor must build green between steps.
