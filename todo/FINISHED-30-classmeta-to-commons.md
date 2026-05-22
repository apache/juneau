# TODO-30 — Investigate moving `ClassMeta` (and related non-marshalling types) from `juneau-marshall` into `juneau-commons`

## Outcome (rescope, 2026-05-22)

Investigation phase abandoned without execution. After consideration, the
move was judged infeasible at acceptable cost: `ClassMeta` is one of the
most-imported types in the codebase (hundreds of references across
`juneau-marshall`, `juneau-rest-*`, `juneau-bean-*`, `juneau-utest`), the
per-format `*ClassMeta` extension cache and the `MarshallingContext`
side-channel hooks make it not standalone-portable, and the cascading
follow-on moves (`ObjectSwap`, `BeanRegistry`, object tools) all currently
depend transitively on `ClassMeta`. Even Phase 0 (a read-only inventory
deliverable) was deemed not worth running given that the eventual move would
not be executed.

No carry-forward TODO is opened from TODO-30 specifically. The companion
TODO-7 has been similarly rescoped - see `FINISHED-7-decouple-rest-common-
from-marshall.md`. The four low-effort moves that don't depend on `ClassMeta`
landed as TODO-60 (`TODO-60-low-effort-marshall-to-commons-moves.md`).

The original analysis below is preserved verbatim for historical context.

---

Source: promoted from `TODO.md` on 2026-05-13. Companion follow-on to **TODO-5** (which already moved the bean-runtime types — `BeanMap`, `BeanMeta`, `BeanPropertyMeta`, `BeanPropertyValue`, `BeanPropertyConsumer`, `BeanProxyInvocationHandler`, `BeanMapEntry`, `BeanInterceptor` — into `org.apache.juneau.commons.bean`).

---

## Goal

Determine whether the **type-classification / type-metadata layer** of Juneau (currently rooted at `ClassMeta`) can be moved out of `juneau-marshall` and into `juneau-commons`, so that `juneau-marshall` ends up containing **only**:

- Serializers and parsers (`Json*`, `Xml*`, `Html*`, `Csv*`, `Msgpack*`, `Cbor*`, `Bson*`, `Toml*`, `Yaml*`, `Markdown*`, `Hocon*`, `Hjson*`, `Jcs`, `Jsonl`, `Json5`, `Proto*`, `Parquet*`, `OpenApi*`, `Uon*`, `UrlEncoding*`, etc.) and their per-format support code.
- Marshalling sessions / contexts / converters / listeners / exception types.
- Marshalling-specific annotations (`@Marshalled`, `@MarshalledProp`, `@MarshalledCtor`, `@MarshalledConfig`, `@MarshalledIgnore`, `@Swap`, etc.).
- HTTP-part serializer / parser SPI (`HttpPartSerializer`, `HttpPartParser`, `@HttpPartMarshalling`).
- JSON Schema generator.
- Marshaller façades (`Json`, `Xml`, `MarshallUtils`, …).

Everything else (type metadata, bean modeling extensions, swap registry plumbing, generic object tools, generic annotation application machinery) becomes a candidate for `juneau-commons` (existing packages or new sub-packages).

This is an **investigation / feasibility** task. The deliverable is a written analysis with concrete recommendations for what can move, what should stay, and what intermediate SPI work (if any) is needed first. **No code changes** are part of the initial pass — once the analysis is in, individual moves get spun off into their own `TODO-30a/b/c…` plan files (or fresh TODO ids) following the same pattern as TODO-5's phases.

---

## Scope

### In scope (candidates to move)

| Candidate | Current location | Notes |
|---|---|---|
| `ClassMeta<T>` | `org.apache.juneau` | Core type-metadata registry; extends `BeanInfo<T>` (already in commons.bean). |
| `ExtendedClassMeta` | `org.apache.juneau` | Per-format extension hook attached to `ClassMeta`. |
| `BeanDictionaryMap` / `BeanDictionaryList` | `org.apache.juneau` | Pre-registered bean dictionaries. Currently consume `ClassMeta`. |
| `BeanRegistry` | `org.apache.juneau` | Implements commons-side `BeanRegistryLookup`. Concrete impl lives marshalling-side. |
| `ObjectSwap<T,S>` | `org.apache.juneau.swap` | Type-level swap descriptor. Tied to swap registry + media-type matching. |
| Built-in swaps in `org.apache.juneau.swap.*` | `org.apache.juneau.swap` | `TemporalSwap`, `CalendarSwap`, `DateSwap`, `ObjectMapSwap`, `EnumerationSwap` (deleted), `IteratorSwap` (deleted), etc. Each is candidate-by-candidate. |
| Annotation-application framework (`AnnotationApplier`, `@ContextApply`, `AnnotationWorkList`, `ContextProperties`) | `org.apache.juneau.annotation` / `org.apache.juneau` | Generic — drives both bean-config and marshalling-config application. |
| `Context` / `Session` (the base types) | `org.apache.juneau` | Currently parents of `MarshallingContext` / `MarshallingSession`. If they're not marshalling-specific, they can move. |
| Object tools (`ObjectRest`, `ObjectComparator`, `ObjectIntrospector`, `ObjectSearcher`, `ObjectSorter`, `ObjectViewer`) | `org.apache.juneau.objecttools` | Walk POJOs for query / sort / project; depend on `BeanMap` (already in commons) and `ClassMeta`. |
| Per-format `*ClassMeta` extensions (`XmlClassMeta`, `RdfClassMeta`, `HtmlClassMeta`, etc.) | per-format packages in `juneau-marshall` | Stay marshalling-side (format-specific); but their abstract parent `ExtendedClassMeta` is in scope. |
| `Marshaller`-side type-conversion machinery hidden behind `ClassMeta` (default-value table, `canCreateNewInstance`, surrogate detection, builder-class detection) | `org.apache.juneau` | Already part of `ClassMeta` — move with it. |

### Explicitly out of scope (stays in `juneau-marshall`)

- `@Marshalled` / `@MarshalledProp` / `@MarshalledCtor` / `@MarshalledConfig` / `@MarshalledIgnore` and their `*Annotation.Applier` builders.
- `MarshallingContext` / `MarshallingSession` / `MarshallingTraverseContext` / `MarshallingTraverseSession` and their builders.
- `MarshalledFilter` / `MarshalledBeanMetaInitializer` / `MarshalledPropertyPostProcessor` (already implementations of commons-side SPIs).
- All serializers, parsers, and per-format support classes.
- `Swap` annotation, `@SerializerConfig`, `@ParserConfig`.
- `HttpPart*` serializer/parser SPI.
- `JsonSchemaGenerator`.

### Already in commons (no action needed)

- `BeanMap`, `BeanMeta`, `BeanPropertyMeta`, `BeanPropertyValue`, `BeanPropertyConsumer`, `BeanProxyInvocationHandler`, `BeanMapEntry`, `BeanInterceptor` (TODO-5).
- `BeanInfo`, `BeanSession`, `BeanFilter`, `BeanRegistryLookup`, `BeanMetaInitializer`, `BeanPropertyPostProcessor`, `BeanTypeResolver`, `BeanConfigContext` (TODO-5).
- `@BeanType`, `@BeanProp`, `@BeanCtor`, `@BeanIgnore`, `@BeanConfig`, `@Name` (TODO-5).
- `ClassInfo`, `ClassInfoTyped`, `MethodInfo`, `FieldInfo`, `ParameterInfo`, `AnnotationInfo`, `AnnotationProvider`, `Visibility`, `ExecutableException`, `ReflectionUtils`, `TypeVariables` (commons.reflect — pre-existing).
- `BeanStore`, `BeanInstantiator`, `BasicBeanStore` (commons.inject).
- `Utils`, `Settings`, value classes, `ThrowableUtils`, etc.

---

## Phase 0 — Inventory and coupling map (deliverable: a written analysis)

The first concrete output is a **dependency map**: for each candidate type, list which marshalling-only types it currently imports and which marshalling-only types currently import it. This shapes the eventual move order.

### Steps

1. **`ClassMeta` itself.** Enumerate every field, every method, and every type referenced from each. Categorize as:
    - (a) `BeanInfo` / `ClassInfo` / commons-side type — no problem.
    - (b) Bean-modeling-only type that is already in commons (`BeanMeta`, `BeanFilter`, …) — no problem.
    - (c) Marshalling-specific type (`ObjectSwap`, `MarshalledFilter`, `BeanRegistry` concrete, per-format `*ClassMeta` extensions) — these are the move-blockers.
    - (d) Side-channel hooks into `MarshallingContext` (cache lookups, type-name registry, default-value table) — these decide whether `ClassMeta` can be standalone or needs a `MarshallingContext` companion.
2. **`ObjectSwap`.** Same audit. Determine whether `ObjectSwap` is fundamentally tied to `MediaType` matching (a marshalling concept) or just to a `Class<?>` pair plus optional metadata.
3. **`BeanRegistry`.** Already a thin wrapper over `BeanRegistryLookup`. Audit what makes the concrete implementation marshalling-side (annotation reads? type-resolution? exception classes?).
4. **`Context` / `Session` base types.** Determine whether the base APIs (build / apply / copy / set / property bag) are inherently marshalling-specific or generic infrastructure that could host both bean-config and marshalling-config.
5. **Annotation-application framework (`AnnotationApplier`, `AnnotationWorkList`, `ContextProperties`).** Audit whether the framework itself references marshalling types or whether it's a generic dispatcher driven by builder method references.
6. **`BeanDictionaryMap` / `BeanDictionaryList`.** Audit whether they actually need `ClassMeta` or can operate on `BeanInfo` (commons-side seam).
7. **Object tools.** Audit whether `ObjectRest` / `ObjectSearcher` / etc. need `MarshallingContext` or only the commons-side `BeanSession` / `BeanMap` surface.

### Output

Write the analysis as an appendix at the bottom of this plan (or as a separate `TODO-30-analysis.md` if it gets large). Each candidate gets:

- Recommended outcome: **MOVE-AS-IS** / **MOVE-WITH-SPI** / **STAYS** / **SPLIT** (move parent, keep concrete).
- Move-blockers (concrete types that currently couple it to marshalling).
- Proposed SPI seam(s), mirroring the `BeanInfo` / `BeanSession` / `BeanRegistryLookup` pattern from TODO-5.
- Estimated effort (small / medium / large) — order of magnitude.

---

## Phase 1 — Standalone-compile probes

Once the analysis is in, validate the dependency map with empirical probes:

- Create temporary `juneau-commons-probe` source folders that copy the candidate file with minimal edits (replace marshalling-side type refs with `Object` casts, remove imports). Compile `juneau-commons` standalone. The set of compile errors maps the **real** remaining coupling.
- Use these probe builds to refine effort estimates per candidate.

(Same technique TODO-5 used during Phase 5 prep — see `todo/TODO-5-bean-runtime-types-to-commons.md` for the pattern.)

---

## Phase 2+ — Sequencing (to be planned post-analysis)

A likely ordering, **subject to revision** once Phase 0 / 1 are done:

1. Promote `Context` / `Session` base types to commons (if Phase 0 confirms they're generic).
2. Move annotation-application framework to commons.
3. Move `ObjectSwap` (with whatever SPI seam decouples it from `MediaType` / format awareness — possibly `ObjectSwap` itself stays generic and the format-aware filter lives on the marshalling side).
4. Move `ClassMeta` (the big one). Likely requires:
    - A commons-side `ClassMeta` that no longer carries the per-format `ExtendedClassMeta` cache.
    - `MarshallingContext` keeps a side-map keyed by `ClassMeta` for the per-format extensions.
    - Default-value table / `canCreateNewInstance` / surrogate detection stay on `ClassMeta` since they're generic Java-class facts, not marshalling facts.
5. Move `BeanRegistry`, `BeanDictionaryMap`, `BeanDictionaryList` (smaller, likely follow-on).
6. Move object-tools package.
7. Move per-format `*ClassMeta` parent `ExtendedClassMeta` (if Phase 0 finds it's a thin generic hook).

Each step gets its own checkpoint with the same shape as TODO-5's Phase C tasks: identify blockers, lift SPIs, then physical `git mv` + reference sweep + standalone compile verification.

---

## Non-goals (this TODO)

- **No code changes during the investigation phase.** Phase 0 is purely a read-only audit.
- **No coupling to specific release vehicles.** This work straddles releases; lock in version boundaries only when a phase is actually executed.
- **Not a `juneau-marshall` rewrite.** Serializers / parsers stay structurally the same; only their dependency floor changes.

---

## Open questions

1. **Does `ClassMeta` need to know about media types?** If yes (e.g. via `forMediaTypes` matching on swaps cached inside `ClassMeta`), then either the media-type-aware portion stays marshalling-side as `ExtendedClassMeta` or `ClassMeta` needs to expose an `Object`-typed swap-list seam that the marshalling-side narrows.
2. **Does `Context` carry serializer/parser settings?** If `Context.Builder` defines things like `quoteChar`, `useWhitespace`, etc., those are marshalling-specific and `Context` cannot move as-is — only a smaller `BeanContext`-style ancestor would.
3. **Does the annotation-application framework reference `MarshallingContext.Builder` explicitly anywhere, or only through generic `B extends Builder` parameters?** The latter is portable.
4. **Are object tools depended on by `juneau-rest-server`?** If yes, moving them to commons would let rest-server drop its `juneau-marshall` runtime dependency for those features (relates to TODO-7's larger goal).
5. **Where does `BeanFilter.Builder` live?** Currently it sits between commons (`BeanFilter` SPI) and marshall (`MarshalledFilter` impl). The builder may want to live in commons (mirroring TODO-5's tightening of `BeanPropertyPostProcessor.process`).
6. **Coordination with TODO-14 (move SVL to commons).** If `VarResolver` lands in commons first, `Context`/`Session` base-type move becomes much easier — they currently reference `VarResolverSession` in the apply pipeline.

---

## Risks

- **`ClassMeta` is one of the most-imported types in the entire codebase** (hundreds of references across `juneau-marshall`, `juneau-rest-*`, `juneau-bean-*`, `juneau-utest`). A package-path move means a repo-wide import sweep on the same order of magnitude as TODO-5's 138-file BeanInfo rename.
- **Per-format `*ClassMeta` extensions** are cached inside `ClassMeta` itself today. Decoupling that cache requires either keeping a side-map on `MarshallingContext` or accepting that `ClassMeta` carries an `Object`-typed `extendedMeta` lookup. Either way, the per-format `XmlClassMeta` / `HtmlClassMeta` / `RdfClassMeta` classes stay marshalling-side.
- **Annotation-application framework move** can ripple: every `@MarshalledConfig.Applier`, every `@SerializerConfig.Applier`, etc. currently extends commons-or-marshall classes. If the base `AnnotationApplier` moves to commons, the marshalling-side concrete appliers stay in marshall but their parent class import flips. Mechanical but repo-wide.
- **Source-incompatible** even for downstream users who reference `org.apache.juneau.ClassMeta` directly. Coordinate with the v9.5 migration guide.

---

## Definition of done (for the investigation phase)

- [ ] Phase 0 inventory written. Each candidate type has a recommended outcome, blockers, and effort estimate.
- [ ] Phase 1 standalone-compile probes attempted for at least `ClassMeta`, `ObjectSwap`, and one object-tool to validate the analysis.
- [ ] Phase 2+ sequencing locked in based on Phase 0 / 1 findings.
- [ ] Followup `TODO-30a-…` / `TODO-30b-…` plan files (or new top-level TODO ids) created for the individual moves identified in Phase 2+. This plan file then becomes a hub linking to those followups.
