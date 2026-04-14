# Split `on`/`onClass` into separate `@XApply` annotations

## Goal

Remove `on()` and `onClass()` from all context-appliable annotations and move them into companion `@XApply` annotations. This separates annotation **content** (what to configure) from **targeting** (where to apply it), enabling annotations like `@Schema` to live in `juneau-commons` while dynamic application machinery stays in `juneau-marshall`.

## Background

Today, annotations like `@Schema`, `@Bean`, `@Json`, etc. serve two roles:

1. **Inline declaration** — placed directly on a class/method/field, no `on`/`onClass` set.
2. **Dynamic application** — placed on a config class with `on`/`onClass` set, applied later via `BeanContext.Builder.applyAnnotations()`.

The `@ContextApply` meta-annotation and `AnnotationApplier` machinery in marshall handles role 2. The applier checks `on`/`onClass`; if populated, it stores the annotation for later target matching.

**Problem:** `on`/`onClass` and `@ContextApply` are **marshall-only** concepts, but they live on annotations that should be usable in `juneau-commons` or `juneau-rest-common` without pulling in marshall.

## Design

### Before (current)

```java
@Schema(on = "com.example.Foo", format = "date-time")
@Bean(onClass = Foo.class, sort = true)
public class MyConfig {}
```

### After (proposed)

```java
@SchemaApply(on = "com.example.Foo", value = @Schema(format = "date-time"))
@BeanApply(onClass = Foo.class, value = @Bean(sort = true))
public class MyConfig {}
```

- `@Schema` has **no** `on`, `onClass`, or `@ContextApply` — it is a pure data annotation.
- `@SchemaApply` lives in marshall, carries `on`/`onClass` and `@ContextApply`, and wraps the annotation via a `value()` member.
- Breaking change: existing `@Schema(on = ...)` usages must be rewritten.

---

## Annotations in scope

### Group A: `org.apache.juneau.annotation` — both `on()` and `onClass()`

These have the `AppliedOnClassAnnotationObject` builder base and target types (classes).

| Annotation | Builder base | Applier target | Companion file |
|---|---|---|---|
| `@Schema` | `BuilderTMF` | `Context.Builder` | `SchemaAnnotation.java` |
| `@Swap` | `BuilderTMF` | `BeanContext.Builder` | `SwapAnnotation.java` |
| `@Bean` | `BuilderT` | `BeanContext.Builder` | `BeanAnnotation.java` |
| `@BeanIgnore` | `BuilderTMFC` | `BeanContext.Builder` | `BeanIgnoreAnnotation.java` |
| `@Uri` | `BuilderTMF` | `BeanContext.Builder` | `UriAnnotation.java` |
| `@Marshalled` | `BuilderT` | `BeanContext.Builder` | `MarshalledAnnotation.java` |
| `@Example` | `BuilderTMF` | `BeanContext.Builder` | `ExampleAnnotation.java` |

### Group B: `org.apache.juneau.annotation` — `on()` only (no `onClass()`)

These have the `AppliedAnnotationObject` builder base and target methods/fields/constructors only.

| Annotation | Builder base | Companion file |
|---|---|---|
| `@Beanp` | `BuilderMF` | `BeanpAnnotation.java` |
| `@Beanc` | `BuilderC` | `BeancAnnotation.java` |
| `@ParentProperty` | `BuilderMF` | `ParentPropertyAnnotation.java` |
| `@NameProperty` | `BuilderMF` | `NamePropertyAnnotation.java` |

### Group C: Format-specific annotations (marshall subpackages) — both `on()` and `onClass()`

| Annotation | Package |
|---|---|
| `@Json` | `json/annotation` |
| `@Xml` | `xml/annotation` |
| `@Html` | `html/annotation` |
| `@HtmlLink` | `html/annotation` |
| `@Csv` | `csv/annotation` |
| `@Uon` | `uon/annotation` |
| `@UrlEncoding` | `urlencoding/annotation` |
| `@MsgPack` | `msgpack/annotation` |
| `@OpenApi` | `oapi/annotation` |
| `@PlainText` | `plaintext/annotation` |
| `@SoapXml` | `soap/annotation` |
| `@Cbor` | `cbor/annotation` |
| `@Bson` | `bson/annotation` |
| `@Proto` | `proto/annotation` |
| `@Hjson` | `hjson/annotation` |
| `@Hocon` | `hocon/annotation` |
| `@Ini` | `ini/annotation` |
| `@Markdown` | `markdown/annotation` |
| `@Parquet` | `parquet/annotation` |

### Group D: marshall-rdf

| Annotation | Package |
|---|---|
| `@Rdf` | `jena/annotation` |

### Group E: REST annotations (juneau-rest-server)

These annotations also have `on()`/`onClass()` but use a different application mechanism (RestContext, not BeanContext). They are **out of scope** for this plan — their `on`/`onClass` serve a different purpose (REST method/class targeting) and do not need the `XApply` split.

| Annotation |
|---|
| `@Rest`, `@RestGet`, `@RestPut`, `@RestPost`, `@RestDelete`, `@RestPatch`, `@RestOptions`, `@RestOp` |
| `@RestStartCall`, `@RestPreCall`, `@RestPostCall`, `@RestEndCall`, `@RestInit`, `@RestPostInit`, `@RestDestroy` |

Note: REST operation annotations (`@Rest` through `@RestOp`) have `@ContextApply` but their appliers configure the REST context, not `BeanContext`. Their `on()` identifies which REST class/method the annotation applies to. REST lifecycle annotations (`@RestStartCall` through `@RestDestroy`) have `on()` but no `@ContextApply`.

---

## Implementation plan

### Step 1: Create the `@XApply` template pattern

Define the structural pattern that all `@XApply` annotations will follow.

For an annotation `@X` with both `on` and `onClass`:

```java
// In the same package as the original @X (stays in marshall)
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(XApply.Array.class)
@ContextApply(XApplyAnnotation.Applier.class)
public @interface XApply {
    X value();             // the wrapped annotation
    String[] on() default {};
    Class<?>[] onClass() default {};

    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface Array {
        XApply[] value();
    }
}
```

For `@X` with `on()` only (no `onClass`):

```java
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(XApply.Array.class)
@ContextApply(XApplyAnnotation.Applier.class)
public @interface XApply {
    X value();
    String[] on() default {};

    @Documented @Target(TYPE) @Retention(RUNTIME)
    public @interface Array { XApply[] value(); }
}
```

### Step 2: Create `XApplyAnnotation` companion classes

Each `@XApply` gets a companion `XApplyAnnotation.java` with:

- Builder class extending the appropriate `AppliedAnnotationObject` or `AppliedOnClassAnnotationObject` builder base
- `Applier` inner class that:
  1. Reads `on`/`onClass` from the `@XApply`
  2. Reads the nested `@X` annotation
  3. Calls `b.annotations(...)` to register the nested annotation against the targets
- `copy()` method for VarResolver resolution
- `DEFAULT`, `create()`, `Array` container

The applier logic is essentially the same as today's applier, just relocated from `XAnnotation.Apply` to `XApplyAnnotation.Applier`.

### Step 3: Strip `on`/`onClass`/`@ContextApply` from `@X` annotations

For each annotation in Groups A-D:

1. Remove `String[] on() default {}` attribute
2. Remove `Class<?>[] onClass() default {}` attribute (where present)
3. Remove `@ContextApply(XAnnotation.Applier.class)` meta-annotation
4. Remove the `Applier`/`Apply` inner class from `XAnnotation.java`
5. Remove `on`/`onClass` from the `XAnnotation.java` Builder class
6. Change builder base from `AppliedOnClassAnnotationObject.BuilderTMF` to a non-targeting base
7. Change private Object class from `AppliedOnClassAnnotationObject` to base `AnnotationObject` or similar
8. Keep `@Repeatable(XAnnotation.Array.class)` on `@X` itself (repeatability for inline use is still valid)

### Step 4: Migrate all call sites

For each usage of `@X(on = ..., ...)` or `@X(onClass = ..., ...)` in the codebase:

1. **Declarative (annotation on class):** Rewrite to `@XApply(on = ..., value = @X(...))`
2. **Programmatic (builder API):** Rewrite from `XAnnotation.create().on(...).build()` to `XApplyAnnotation.create().on(...).value(XAnnotation.create()...build()).build()`

### Step 5: Update tests

Every `*Annotation_Test.java` file needs updates for the new structure. Also update integration tests that use `on`/`onClass` declaratively.

### Step 6: Update `AnnotationWorkList` and `Context.Builder`

The annotation discovery machinery (`CONTEXT_APPLY_FILTER`, `traverse()`, `applyAnnotation()`) should work unchanged because `@XApply` carries `@ContextApply` and its applier handles the nesting. Verify this with tests.

### Step 7: Release notes

Document the breaking change with before/after examples for common patterns.

---

## Ordering

This work is a **prerequisite** for Phase 2 of `decouple-rest-common-from-marshall.md` (moving `@Schema` to commons). However, it can be done incrementally:

1. **Start with one annotation** (e.g. `@Schema`) as a proof-of-concept
2. **Sweep remaining Group A** annotations
3. **Sweep Group B** (`on()`-only annotations)
4. **Sweep Group C** (format-specific annotations)
5. **Sweep Group D** (`@Rdf`)

Each step should compile and pass tests independently.

---

## Files affected (estimated)

| Category | Count |
|---|---|
| New `@XApply` annotation files | ~30 |
| New `XApplyAnnotation.java` companion files | ~30 |
| Modified `@X` annotation files (strip `on`/`onClass`) | ~30 |
| Modified `XAnnotation.java` files (remove Applier) | ~30 |
| Migrated call sites (declarative `on`/`onClass` usage) | ~80 |
| Updated test files | ~40 |
| Release notes | 1 |
