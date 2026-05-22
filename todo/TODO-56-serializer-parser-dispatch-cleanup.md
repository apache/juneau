# TODO-56: Serializer/Parser dispatch-chain cleanup (frequency reorder + nested-`instanceof` hoist + `ClassMeta` polish)

Source: filed 2026-05-21 in response to the recent JIT/dispatch analysis on `ClassMeta.isXxx`. This plan is a direct expansion of recommendations **#2, #3, #4, and #5** from the investigator's verdict, plus a second, related concern the user surfaced from `ProtoParserSession.convertValue` (repeated `instanceof X val2 && …` patterns against the same value).

---

## Background

### Why the dispatch chains came up

The JIT investigation looked at the long `if (sType.isXxx()) … else if (sType.isYyy()) …` chains in `*SerializerSession.serializeAnything` and `*ParserSession.parseAnything` and concluded that **branch-order matters more than method micro-shape**. From the verdict's bullet **#2**:

> Order the chain by frequency. This is a real (and free) win on long chains. In the JSON serializer dispatch, `isBean()` is the eighth branch, but for typical REST traffic beans dominate by far; `isMap`/`isCollection` are also further down than they should be. Moving `isBean`, `isMap`, `isCollection`, `isCharSequence` up front shortens the *average* number of mispredicted branches per value. Also worth noting: `isBean()` is the only one in the chain that's *not* a cheap bitmask test (it does `nn(getBeanMeta())`), so putting it first costs only a single null-check on the hot path.

The chains live in every textual format's session (JSON, XML, HTML, UON, URL-encoded, YAML, Hjson, Hocon, MsgPack, CBOR, BSON, CSV, Markdown, TOML, INI, RDF, OpenAPI, Proto, Parquet). They're highly consistent in structure but vary in branch order, and the typical "REST traffic dominated by beans / maps / collections / char sequences" assumption applies uniformly across all of them.

### The nested-`instanceof` pattern the user named

In parallel with the dispatch reorder, the user pointed at `ProtoParserSession.convertValue` (lines 287–330) as an example of a *different* code-shape problem that's worth fixing while we're touching this area:

**Current (bad):**

```java
if (val instanceof Map val2 && JsonMap.class.isAssignableFrom(targetType.inner())) { … }
if (val instanceof Map val2 && targetType.isBean()) { … }
if (val instanceof Map val2 && targetType.isMap()) { … }
```

**Desired (good):**

```java
if (val instanceof Map val2) {
    if (JsonMap.class.isAssignableFrom(targetType.inner())) { … }
    if (targetType.isBean()) { … }
    if (targetType.isMap()) { … }
}
```

The wins are uniform: (a) the `instanceof` check happens once, not three times; (b) the pattern-variable cast (`val2`) is hoisted once, not bound three times; (c) the inner conditions read more clearly as branches off a known type; (d) the JIT sees a smaller, simpler basic-block dispatch. The same shape repeats in `TomlParserSession.convertValue` (lines 388–425), and the user wants us to find every site like this. The good news is the file survey turned up only **two** confirmed sites with the repeated-same-type / different-secondary-condition shape — the rest of the marshalling tree only has single isolated `instanceof X val2 && …` hits that don't refactor.

---

## Scope

### Files in scope

`juneau-core/juneau-marshall/` (text-and-binary marshallers) and `juneau-core/juneau-marshall-rdf/` (RDF text marshallers). No `juneau-rest-*`, no `juneau-microservice-*`, no `juneau-bean-*` modules — pure marshalling layer only.

### Out of scope (deliberately)

- Adding new format types or expanding `MarshallingContext` (no new knobs).
- Touching `juneau-rest-*` or `juneau-microservice-*`.
- Expanding the optional Phase 4 `Category primaryCategory()` switch beyond the temporal cluster pilot (`isDate` / `isCalendar` / `isTemporal` / `isDuration` / `isPeriod`).
- Behavior changes to any dispatch branch — the goal is to **reorder and hoist**, not to change *what* each branch does.
- Touching `Parquet*Session.java` and `OpenApi*Session.java` chains in Phase 1 (they're schema-driven / row-shape-driven and don't fit the canonical "scalar → temporal → bean → map → collection" mold — see notes in the per-file inventory below). These get a Phase-1 carve-out.

---

## Per-file dispatch-chain inventory (Phase 1 — frequency reorder)

The reorder target across all canonical chains is:

```
1.  isBean              ← was deep, promote
2.  isMap               ← was deep, promote
3.  isCollection (and  isCollectionOrArray / isArray as appropriate per file)
4.  isCharSequence (and isChar, where the format groups them)
5.  isNumber  / isBoolean
6.  isUri
7.  isDate
8.  isCalendar
9.  isTemporal
10. isDuration
11. isPeriod
12. isStreamable / isReader / isInputStream
13. <format-specific tail>  (else / fallback)
```

The first-four block is the "REST-traffic-dominant" frequency tier. The temporal cluster stays as a unit (and is a candidate for the Phase 4 switch). Format-specific branches that lead with a wire-token character (`isChar0`, BSON `OBJECT_ID`-fast-path, MsgPack/CBOR `dt == …` pre-dispatch, OpenAPI `t == …` schema-type dispatch, HTML `tag == …` dispatch, YAML character-class dispatch) **stay where they are** — those are correctness gates, not value-type dispatches, and reordering them would be wrong.

### Canonical text-marshaller chains (full reorder)

| # | File | Method | Current lead (top 3 of chain) | Notes |
|---|------|--------|------------------------------|-------|
| 1 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json/JsonSerializerSession.java` | `serializeAnything` (396-496) | `isChar0` → `isNumber\|\|isBoolean` → `isDate` | `sType` hoisted; bean is **8th** branch. Canonical "winner" file from the investigation. Keep `isChar0` as the pre-dispatch null-byte gate. |
| 2 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json/JsonParserSession.java` | `parseAnything` (234-358) | `isObject` (peek-char) → `isBoolean` → `isCharSequence` | Already has `isMap`/`isCollection` *before* `canCreateNewBean`. Re-evaluate: parser chains are driven more by the wire token than by traffic frequency; treat reorder as a tighter polish here than on the serializer side. |
| 3 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/uon/UonSerializerSession.java` | `serializeAnything` (463-550) | `isChar0` → `isBoolean` → `isNumber` | Same shape as JSON serializer. |
| 4 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/uon/UonParserSession.java` | `parseAnything` (336-590) | `isVoid` → `isObject` (peek-char) → `isBoolean` | Has a pre-check for "blank-input URL params" (lines 359-366) that stays at top. |
| 5 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/urlencoding/UrlEncodingSerializerSession.java` | `serializeAnything` (261-314) | `isMap` → `isBean` → `isCollection\|\|isArray` | **Already leads with structural types**; only a minor swap of `isMap` vs `isBean` to align with the canonical order. |
| 6 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/urlencoding/UrlEncodingParserSession.java` | `parseAnything` (248-326) | `isObject` → `isMap` → `builder` | Narrow chain; already structural-first. Minor polish only. |
| 7 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/xml/XmlSerializerSession.java` | `serializeAnything` (908-1158) | (write pass at 1093-1133) `isUri\|\|pMeta.isUri` → `isCharSequence\|\|isChar` → `isNumber\|\|isBoolean` | Two-stage (classify at 988-1004 then write at 1093-1133). `wType` delegate hoist already present. Reorder the *write pass* only; the classify pass at 988 is correctness-load-bearing. |
| 8 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/xml/XmlParserSession.java` | `parseAnything` (839-970) | `isObject` (jsonType sub) → `isBoolean` → `isCharSequence` | Already has `isMap`/`isCollection` precede `isNumber` — opposite of `JsonParserSession`. Decide once whether parser chains follow serializer order or stay wire-token-shaped, then apply uniformly. |
| 9 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlSerializerSession.java` | `serializeAnything` (946-1158) | `isReader\|\|isInputStream` (early) → `isNumber` → `isBoolean` | `isReader`/`isInputStream` short-circuit at the **top** is intentional (HTML can stream them as raw children) — leave it. Re-order the rest. |
| 10 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlParserSession.java` | `parseAnything` (294-~730) | Tag-first dispatch with secondary `sType` sub-cascade inside each `tag==…` branch (lines 348-392, 374-393, 427+) | The top-level shape is tag-driven, not type-driven — **do not reorder the outer dispatch**. Apply the canonical reorder to the *inner* `sType.isObject\|\|isCharSequence` cascades only. |
| 11 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/html/HtmlSchemaSerializerSession.java`, `HtmlSchemaDocSerializerSession.java`, `HtmlStrippedDocSerializerSession.java`, `HtmlDocSerializerSession.java` | (none — inherit) | — | No work — they extend `HtmlSerializerSession` and add doc/schema framing. |
| 12 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/yaml/YamlSerializerSession.java` | `serializeAnything` (247-314) | `isChar0` → `isNumber\|\|isBoolean` → `isBean` | **Omits temporal branches** — falls through to `toString(o)`. Phase 4 may *add* the temporal cluster if the switch lands; Phase 1 just reorders existing branches. |
| 13 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/yaml/YamlParserSession.java` | `parseAnything` (243-358) + `convertToType` (485-511) | Character-class dispatch at top; inner `sType.isObject/isMap/...` cascade per branch | Same shape comment as `HtmlParserSession`. Apply reorder inside the inner cascades. |
| 14 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hjson/HjsonSerializerSession.java` | `serializeAnything` (211-286) | `isChar0` → `isNumber\|\|isBoolean` → `isDate` | Same shape as JSON serializer. |
| 15 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hjson/HjsonParserSession.java` | (no top-level chain — tokenizer + `convertToType`) | — | Apply reorder inside `convertToType`-style helpers if any have the chain shape; otherwise skip. |
| 16 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconSerializerSession.java` | `serializeAnything` (235-310) | `isChar0` → `isNumber\|\|isBoolean` → `isDate` | Identical structure to `HjsonSerializerSession`. |
| 17 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconParserSession.java` | (no top-level chain) | — | Same comment as Hjson parser. |
| 18 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/markdown/MarkdownSerializerSession.java` | `serializeAnything` (240-303) + `serializeInlineValue` (538-581) | `isOptional` → `isBean` → `isMap` | **Already structural-first.** Verify the inner-value formatter (`serializeInlineValue`) chain order. |
| 19 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/markdown/MarkdownParserSession.java` and `MarkdownDocSerializerSession.java` / `MarkdownDocParserSession.java` | (no canonical chain — piecemeal) | — | Check piecemeal sites for ordering; mostly no-op. |
| 20 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/toml/TomlSerializerSession.java` | `writeValue` (264-330) | `isNumber` → `isBoolean` → `isDate` | Two-phase serializer (`serializeBean` walks table-path, `writeValue` is the leaf). Reorder `writeValue`. Phase 2 also touches `TomlParserSession`. |
| 21 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/toml/TomlParserSession.java` | `convertValue` (388-425) | (already hybrid `instanceof` shape — see Phase 2) | Phase 2 file. |
| 22 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ini/IniSerializerSession.java` | `formatSimpleValue` (265-287) + structural dispatch at `serializeBean` | `isNumber` → `isBoolean` → `isDate` | Scalar-only mini-dispatch in `formatSimpleValue`; structural dispatch lives higher. Reorder both. |
| 23 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ini/IniParserSession.java` | `parseSimpleValue` (~225-256) | `isNumber` → `isDate` → `isCalendar` | Scalar parser — minor reorder. |
| 24 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/csv/CsvSerializerSession.java` | `doSerialize` (~330+) + `applySwap` (300-323) | `cm.isArray` → `cm.isCollection` → `cm.isStreamable` | Row-shape decided first, then per-cell formatting. Verify only that the reorder doesn't break the row-vs-cell split. |
| 25 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/csv/CsvParserSession.java` | `parseAnything` (232-314) | `isArray` → `isCollection` → `isBean` | Already structural-first (table shape). Minor polish only — `parseCellValue` (425-451) has the cell-level cascade. |
| 26 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/msgpack/MsgPackSerializerSession.java` | `serializeAnything` (223-306) | `isChar0` → `isBoolean` → `isNumber` | Adds `isByteArray` between `isCollection` and `isArray` (binary-format-only). Preserve that. |
| 27 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/msgpack/MsgPackParserSession.java` | `parseAnything` (192-348) | Wire-type dispatch first; then the merged `isBoolean\|\|isCharSequence\|\|isChar\|\|isNumber\|\|isByteArray` second pass | The merged-scalar `\|\|` chain is unusual but intentional (one wire-type covers many scalar Java types). Reorder the structural branches *after* the merged-scalar branch; don't break the wire-type pre-dispatch. |
| 28 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/cbor/CborSerializerSession.java` | `serializeAnything` (222-298) | Mirror of MsgPack serializer | Same reorder shape as MsgPack. |
| 29 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/cbor/CborParserSession.java` | `parseAnything` (191-360) | Same shape as MsgPack parser; adds CBOR-specific `dt == UNDEFINED\|\|SIMPLE` tail | Same comment as `MsgPackParserSession`. |
| 30 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/bson/BsonSerializerSession.java` | `writeElement` (108-236) | `isChar0` → `isBoolean` → `isNumber` (with int/long/BigDecimal/BigInteger sub-dispatch) | **Places `isCharSequence\|\|isChar\|\|isEnum` near the *bottom*** (vs near top elsewhere). Reorder to align — verify `Decimal128`/`ObjectId` sub-dispatches still hit. Also check the top-level `doSerialize` (322-331) `isBean` / `isMap` / else branch. |
| 31 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/bson/BsonParserSession.java` | (no canonical chain — `convertToType`-driven) | — | Verify `convertToType`-style helpers for ordering; mostly no-op. |
| 32 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoSerializerSession.java` | `serializeAnything` (131-237) + `serializeScalarValue` (426-453) | `treatAsBean` (with `toBeanMap`-probing override) → `isMap` → `isCollection` | Already structural-first. Reorder `serializeScalarValue`'s scalar block. |
| 33 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoParserSession.java` | `convertValue` (287-330) | (already hybrid `instanceof` shape — see Phase 2) | Phase 2 file. |
| 34 | `juneau-core/juneau-marshall-rdf/src/main/java/org/apache/juneau/jena/RdfSerializerSession.java` | `serializeAnything` (316-453) | `isChar0` → `isUri\|\|isURI` → `isCharSequence\|\|isChar` | **Omits temporal branches** (Jena's `createTypedLiteral` handles them). Reorder the rest. `wType` delegate hoist already present. |
| 35 | `juneau-core/juneau-marshall-rdf/src/main/java/org/apache/juneau/jena/RdfParserSession.java` | `parseAnything` (337-470) | RDF_NIL guard → `isObject` (literal/resource/Seq sub) → `isBoolean` → `isCharSequence` | Apply reorder after the RDF-node-type pre-discrimination. |
| 36 | `juneau-core/juneau-marshall-rdf/src/main/java/org/apache/juneau/jena/RdfStreamSerializerSession.java` | `serializeAnything` (268-380) | Same shape as `RdfSerializerSession` | Apply same reorder as #34. |
| 37 | `juneau-core/juneau-marshall-rdf/src/main/java/org/apache/juneau/jena/RdfStreamParserSession.java` | `parseAnything` (329-470) | Same shape as `RdfParserSession` | Apply same reorder as #35. |

### Phase-1 carve-outs

These files have dispatch shapes that don't fit the canonical "scalar → temporal → bean → map → collection" mold and should be **left alone** in Phase 1 (or revisited as their own follow-up if there's evidence the reorder pays off):

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/oapi/OpenApiSerializerSession.java` and `OpenApiParserSession.java` — schema-type-first (`HttpPartType` then `HttpPartSchema.Type`); the inner `ClassMeta` branches are narrow and already structural-first.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parquet/ParquetSerializerSession.java` and `ParquetParserSession.java` — row-oriented; predicates are scattered across `parseRecord` / `prepareMapForBean` / etc. No canonical chain to reorder.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/soap/SoapXmlSerializerSession.java` — wraps SOAP envelope only; defers to inherited XML dispatch.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/jsonl/JsonlSerializerSession.java` / `JsonlParserSession.java` — lightweight wrappers over JSON.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/json5/Json5SerializerSession.java` / `Json5ParserSession.java` — inherit JSON dispatch unchanged.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/sse/SseSerializerSession.java` / `SseParserSession.java` — event-stream; structure-agnostic.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/plaintext/PlainTextSerializerSession.java` / `PlainTextParserSession.java` — single-string pass; no chain.

**Total Phase-1-active files: 32** (37 rows in the table minus 5 "inherit / no chain" entries).

---

## Per-file nested-`instanceof` inventory (Phase 2 — hoist the type test)

The repeated-same-type / different-secondary-condition shape only appears in **two** files. Everything else in the marshalling tree has at most one isolated `instanceof X val2 && …` site per method, which is not a refactor candidate.

### Phase-2 files

| # | File | Method | Lines | Instance types repeated | Hoist plan |
|---|------|--------|-------|------------------------|------------|
| P2-1 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoParserSession.java` | `convertValue(Object, ClassMeta<?>)` | 287-330 | **`Map val2`** (3 sites: lines 290, 292, 304) is the primary candidate; **`Number val2`** (2 sites: 313, 327) is secondary; **`CharSequence val2`** (1 outer site at line 315) is already hoisted — use it as the reference shape. **`List val2`** (1 site, line 306) is single-condition and doesn't need a hoist. | Wrap the three `Map val2` checks in one outer `if (val instanceof Map val2) { … }`. Optionally merge the two `Number val2` sites into a single outer block too. The user-supplied canonical example targets the `Map` case specifically. |
| P2-2 | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/toml/TomlParserSession.java` | `convertValue(Object, ClassMeta<?>)` | 388-425 | **`Map map`** (2 sites: lines 394, 397); **`List list`** (1 site, 402, single-condition); **`String string`** (1 outer site, 412) is already hoisted. | Wrap the two `Map map` checks in one outer `if (val instanceof Map map) { … }`. Mirrors the Proto pattern. |

### Before / after (canonical — `ProtoParserSession.convertValue`)

The user-supplied example, full shape:

**Before** (current):

```java
private Object convertValue(Object val, ClassMeta<?> targetType) throws ParseException, ExecutableException {
    if (val == null) return null;
    if (val instanceof Map val2 && JsonMap.class.isAssignableFrom(targetType.inner()))
        return toJsonMap(val2);
    if (val instanceof Map val2 && targetType.isBean()) {
        // ... bean construction ...
    }
    if (val instanceof Map val2 && targetType.isMap())
        return toJsonMap(val2);
    if (val instanceof List val2 && targetType.isCollectionOrArray()) {
        // ...
    }
    if (val instanceof Number val2 && targetType.isNumber())
        return convertToMemberType(null, val2, targetType);
    if (val instanceof CharSequence val2) {
        if (targetType.isDate())     return parseDate(val2.toString(), targetType);
        if (targetType.isCalendar()) return parseCalendar(val2.toString(), targetType);
        if (targetType.isTemporal()) return parseTemporal(val2.toString(), targetType);
        if (targetType.isDuration()) return parseDuration(val2.toString());
        if (targetType.isPeriod())   return parsePeriod(val2.toString());
    }
    if (val instanceof Number val2 && targetType.isDateOrCalendarOrTemporal())
        return Iso8601Utils.fromEpochMillis(val2.longValue(), targetType, getTimeZone());
    return convertToMemberType(null, val, targetType);
}
```

**After** (hoisted):

```java
private Object convertValue(Object val, ClassMeta<?> targetType) throws ParseException, ExecutableException {
    if (val == null) return null;
    if (val instanceof Map val2) {
        if (JsonMap.class.isAssignableFrom(targetType.inner())) return toJsonMap(val2);
        if (targetType.isBean()) {
            // ... bean construction ...
        }
        if (targetType.isMap()) return toJsonMap(val2);
    }
    if (val instanceof List val2 && targetType.isCollectionOrArray()) {
        // ... (single condition — leave as-is)
    }
    if (val instanceof Number val2) {
        if (targetType.isNumber())
            return convertToMemberType(null, val2, targetType);
        if (targetType.isDateOrCalendarOrTemporal())
            return Iso8601Utils.fromEpochMillis(val2.longValue(), targetType, getTimeZone());
    }
    if (val instanceof CharSequence val2) {
        if (targetType.isDate())     return parseDate(val2.toString(), targetType);
        if (targetType.isCalendar()) return parseCalendar(val2.toString(), targetType);
        if (targetType.isTemporal()) return parseTemporal(val2.toString(), targetType);
        if (targetType.isDuration()) return parseDuration(val2.toString());
        if (targetType.isPeriod())   return parsePeriod(val2.toString());
    }
    return convertToMemberType(null, val, targetType);
}
```

Apply the same shape to `TomlParserSession.convertValue`.

### Single-site (non-Pattern-2) `instanceof X val2 && …` hits noted during the survey

These are listed for completeness so the worker doesn't waste time chasing them — each is a sole site in its method, so there's nothing to hoist:

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hjson/HjsonParserSession.java:265` — `cm.isMap() && val instanceof Map<?,?> val2 && …`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconParserSession.java:447` — same shape
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/toml/TomlSerializerSession.java:143` — `aType.isMap() && v instanceof Map<?,?> nested && …`
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parquet/ParquetSerializerSession.java:117, 187` — isolated sites in different methods
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parquet/ParquetParserSession.java:1105, 1160, 1203` — sites in different methods, not stacked
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/collections/MarshalledMap.java:590-591` — different instance types per check (not Pattern 2)

---

## Phases

### Phase 1 — Frequency reorder

For each file in the **Per-file dispatch-chain inventory** table above:

- Apply the canonical reorder (bean → map → collection → char sequence → number/boolean → uri → temporal cluster → streamable/reader/inputstream → format-tail).
- Preserve all pre-dispatch correctness gates: `isChar0` null-byte gate, MsgPack/CBOR wire-type pre-dispatch, OpenAPI schema-type pre-dispatch, HTML tag-first dispatch, YAML character-class dispatch, BSON `doSerialize` top-level `isBean`/`isMap` fork, XML two-pass classify/write split.
- Recommended commit shape: **one commit per file** to keep the diff reviewable. Roll-up squash is optional after CI is green on each.
- Verify the dispatch is behaviorally identical with `./scripts/test.py` per file (or per module after a batch).
- No edits to `ClassMeta.isXxx` semantics in this phase — branch order only.

### Phase 2 — Nested `instanceof` cleanup

Two files only:

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoParserSession.java` (`convertValue`, lines 287-330).
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/toml/TomlParserSession.java` (`convertValue`, lines 388-425).

Apply the hoisted shape shown in the **Before / after** sub-section above. Recommended commit shape: **one commit per file**, for the same reviewability reason as Phase 1.

### Phase 3 — Minor cleanups on `ClassMeta` (perf-neutral)

From the investigator's bullet **#3**:

- Drop the defensive `cat != null &&` guards in `ClassMeta.isMap()`, `ClassMeta.isCollection()`, and `ClassMeta.isUri()`. `cat` is `private final` and assigned in the constructor body before any of these methods can be called externally, so the null check can never fire on a constructed instance. Current sites:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java:1047` — `isCollection()`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java:1171-1174` — `isMap()`, including its "Defensive null-guard: category is expected to be set, but keep this check for safety." comment which goes away with the guard.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java:1277` — `isUri()`.
- Mark the **`ClassMeta` class itself `final`** (per resolved decision #4). Do **not** add `final` to individual `isXxx` method declarations — class-level `final` already covers them and avoids cluttering every `isXxx` signature. This is documentation, not optimization — C2 already treats them that way under CHA — but it gives reviewers a one-line signal that no one overrides these. Before flipping the bit, confirm there are no `extends ClassMeta` consumers in the marshalling tree or downstream Juneau modules.

### Phase 4 — Optional readability switch (`Category primaryCategory()`)

From the investigator's bullet **#4**, **optional**:

- Add a `Category primaryCategory()` getter on `ClassMeta` that returns the dominant category (`BEAN`, `MAP`, `COLLECTION`, `ARRAY`, `CHARSEQ`, `NUMBER`, `BOOLEAN`, `DATE`, `CALENDAR`, `TEMPORAL`, `DURATION`, `PERIOD`, `URI`, …) for use in switch dispatch.
- Per resolved decision #5, the return type is a **named, public enum** lifted from the existing `cat.is(BEAN)`-style constants inside `ClassMeta`. If those constants already live in a public-visible enum, reuse it; if they're currently package-private/internal, promote them to a public enum (e.g. `org.apache.juneau.ClassCategory`) as part of this phase.
- **Pilot scope**: convert the temporal cluster (`isDate` / `isCalendar` / `isTemporal` / `isDuration` / `isPeriod`) in `JsonSerializerSession` and `JsonParserSession` to a single `switch (sType.primaryCategory())` block. Runtime impact is treated as a wash — the goal is reviewer clarity.
- **Roll-out decision**: only extend the switch pattern to other sessions if the JSON pilot reads cleaner after a careful review. If not, keep the `else if` chains and skip the broader roll-out. Don't fork the codebase into mixed styles.
- This phase is **optional**. Skip if Phases 1+2 already deliver the readability and perf wins the user wants.

### Phase 5 — Empirical validation (JMH)

From the investigator's bullet **#5**:

- Add a new JMH benchmark class alongside `juneau-utest/src/test/java/org/apache/juneau/BenchmarkRunner.java`. Suggested name: `MarshallDispatchBenchmark.java`. It should serialize and parse a **purpose-built** POJO graph (mix of beans, maps, lists, char sequences, numbers, dates) one million times under both the pre- and post-reorder code. Per resolved decision #7, do **not** reuse an existing `juneau-utest` shared POJO fixture — build a known, controlled, bean-heavy / map-heavy / collection-heavy mix so the dispatch-hit distribution stays clean.
- Capture before/after numbers for at least the **JSON serializer dispatch reorder** since that's the most-trafficked path and the one the investigator named.
- **Success criterion is "no regression"**, not "chase nanoseconds". The reorder should either be neutral or a small win on bean-heavy workloads. If a regression appears, investigate; if the win is too small to measure outside noise, that's a valid result and the readability/maintenance argument still stands.
- Optional sibling tool: an async-profiler run against `juneau-examples` (or the `juneau-microservice-jetty` example REST app) gives the same story in ~5 minutes; capture a flame-graph snapshot for the record.

---

## Resolved decisions

1. **Frequency ranking source.** Use the **static canonical tier ranking** (bean → map → collection → char sequence → number → boolean → temporals → other) as the basis for Phase 1 reordering. No upfront profiling run is required to gate the commits; the canonical order is the source of truth. (Phase 5 JMH numbers serve as the post-hoc sanity check, not a pre-commit blocker.)

2. **Parser vs serializer ordering symmetry.** **Mirror the canonical order** in the structural-branch *tail* of `*ParserSession` chains (post-wire-token-pre-dispatch). Pre-dispatch gates (`isObject` peek-char branches, MsgPack/CBOR wire-type dispatch, HTML tag dispatch, YAML character-class dispatch, RDF node-type discrimination, etc.) stay in place at the top — only the structural tail under each gate gets reordered. Document the rule once in `.cursor/skills/code-conventions/SKILL.md` so future formats follow it.

3. **Phase-2 commit granularity.** **One commit per file** for both Phase-2 files (`ProtoParserSession.convertValue` and `TomlParserSession.convertValue`). Same shape as Phase 1.

4. **Phase 3 `final` decision.** **Mark the `ClassMeta` class `final`** (not individual `isXxx` methods). Before flipping the bit, confirm there are no production-side `extends ClassMeta` consumers in the marshalling tree or downstream Juneau modules.

5. **Phase 4 enum surface.** Lift the existing category constants used inside `ClassMeta` (the `cat.is(BEAN)`-style constants — `BEAN`, `MAP`, `COLLECTION`, …) into a **named, public enum** if they aren't already in a public-visible form; otherwise reuse the existing one. `Category primaryCategory()` returns a value from that enum.

6. **Extend `MarshallingContext` precedence to dispatch ordering.** **No** — do not expose a `dispatchOrder` knob on `MarshallingContext.Builder`. Adding it without a real use case reintroduces the per-call-site uncertainty Phase 1 is trying to remove.

7. **Phase 5 benchmark POJO source.** Build a **purpose-built fixture** for `MarshallDispatchBenchmark`. A known, controlled, bean-heavy / map-heavy / collection-heavy mix gives a clean dispatch-hit distribution; reusing an existing `juneau-utest` POJO graph risks accidental coverage of edge cases that distort the numbers.

---

## Acceptance criteria

- All in-scope dispatch chains (the 32 active rows in the Phase 1 inventory) reordered to the canonical convention.
- Both Phase 2 nested-`instanceof` patterns (`ProtoParserSession.convertValue`, `TomlParserSession.convertValue`) hoisted.
- `ClassMeta.isMap()` / `isCollection()` / `isUri()` defensive `cat != null` guards removed; class (or methods) marked `final` per the open-question resolution.
- (If Phase 4 lands) `Category primaryCategory()` shipped with at least the JSON-serializer temporal-cluster switch pilot.
- New JMH benchmark (`MarshallDispatchBenchmark` or equivalent) added to `juneau-utest`, with before/after numbers captured for the JSON serializer dispatch reorder. Result documented in the commit body of the Phase 1 JSON commit, or in a sibling note under `todo/`.
- `mvn clean install` green.
- `./scripts/test.py` green.
- No regression in coverage on touched files (use `./scripts/coverage.py` per file or per module to confirm).
- No regression in SonarQube findings on touched files (use `./scripts/sonarqube.py` to confirm).
- No behavioral changes — all serializer/parser round-trip tests pass without modification.

## Out of scope

- Adding new format types or expanding `MarshallingContext` (no new knobs).
- Touching `juneau-rest-*`, `juneau-microservice-*`, or `juneau-bean-*` modules.
- Implementing `primaryCategory` switch beyond the JSON temporal-cluster pilot in Phase 4.
- Reordering format-specific pre-dispatch gates (`isChar0`, BSON `doSerialize`, MsgPack/CBOR wire-type pre-dispatch, OpenAPI schema-type dispatch, HTML tag-first dispatch, YAML character-class dispatch, XML two-pass classify/write split).
- `Parquet*Session.java`, `OpenApi*Session.java`, `Soap*Session.java`, `Sse*Session.java`, `PlainText*Session.java`, `Jsonl*Session.java`, `Json5*Session.java` chains — carved out per the Phase-1 carve-out list.
- Behavioral changes to any dispatch branch.

## Related plans / references

- The verdict bullets quoted in the **Background** section come from the JIT/dispatch analysis the user just reviewed; this plan is the captured form of recommendations #2, #3, #4, and #5.
- `juneau-utest/src/test/java/org/apache/juneau/BenchmarkRunner.java` — existing JMH benchmark wiring (collection iteration patterns); the new `MarshallDispatchBenchmark` mirrors its annotation setup (`@Fork`, `@Warmup`, `@Measurement`, `Mode.AverageTime`, `OutputTimeUnit.NANOSECONDS`).
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java` — host of all `isXxx` methods touched in Phase 3.
- `todo/TODO-30-classmeta-to-commons.md` — adjacent plan; if `ClassMeta` moves to `juneau-commons`, the Phase-3 `final` decision should land **before** that move (or the commons-side surface inherits the no-`final` shape and is harder to change later).
