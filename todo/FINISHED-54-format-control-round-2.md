# TODO-54: Round 2 of MarshallingContext format-control — Boolean, Float/Double non-finite, Currency, Class

> **Status: IMPLEMENTED.** All four enums (`BooleanFormat`, `FloatFormat`, `CurrencyFormat`, `ClassFormat`) ship in 9.6 along with their wiring, annotation surface, and tests. `ClassSwap` deleted; replaced by a context-driven `ClassFormatSwap` in `DefaultSwaps` plus per-property installation in `MarshalledPropertyPostProcessor`. See the 9.6 release notes section "Boolean / Float / Currency / Class wire format controls (TODO-54)" for the user-facing summary, including the `FloatFormat`-on-primitive caveat that deviates from the plan's "default `NaN_AS_NULL`" wording: primitive `float` / `double` fields skip context-level swap install to preserve Juneau's null-to-primitive-default convention; explicit annotations still install.

Source: user request on 2026-05-20, following completion of the TODO-50 umbrella plan. Round 1 (TODO-50 / TODO-51 / TODO-52 / TODO-53) covered date-time, locale, byte[], enum, UUID, and BigInteger / BigDecimal. The TODO-50 survey **did not cover `Boolean`** at all, **did not cover floating-point non-finite handling** (`NaN` / `Infinity` / `-Infinity`), and explicitly marked `Currency` and `Class` as `SKIP`. The user has chosen to revisit both `SKIP`s and add the two missing slots: this is the round-2 plan covering all four.

This is the **umbrella plan**. The implementation work is intentionally bundled into a single follow-up TODO (reserved `TODO-55`) — see **Phasing** below.

---

## Goal

Continue the work begun by TODO-4 (Duration / Period) and TODO-50 (date-time / locale / byte[] / enum / UUID / BigNumber): replace ad-hoc per-type wire-format defaults with a uniform **`<type>Format` enum + `MarshallingContext.Builder.<type>Format(...)` setting + `@Marshalled` / `@MarshalledProp` / `@MarshalledConfig` annotation surface** for four additional types whose wire-format ambiguity is significant enough to warrant first-class treatment:

| Type | Why first-class now |
| --- | --- |
| `Boolean` / `boolean` | Real-world JSON / CSV / XML wires use `true`/`false`, `0`/`1`, `yes`/`no`, `Y`/`N`, `on`/`off` interchangeably. Today Juneau emits one shape (`true`/`false`) and parsers accept a few of the others as a side-effect of `Boolean.parseBoolean` plus `BasicConverter`. Round-trippable per-property control is missing. |
| `Float` / `Double` non-finite | `NaN`, `Infinity`, `-Infinity` are not valid JSON tokens but appear all the time in scientific / metric pipelines. Today serializers either crash (strict JSON) or emit the bare token (non-standard). A setting that lets callers pick `NaN_AS_NULL` (spec-compliant), `NaN_AS_STRING` ("round-trip-preserving"), `NaN_AS_NUMBER` (lenient), or `NaN_AS_ERROR` (fail-fast) closes a real foot-gun. |
| `Currency` | Marked `SKIP` in TODO-50 on the assumption that ISO codes were the only realistic wire form. Revisiting: `SYMBOL` (`$`) and `DISPLAY_NAME` (`"US Dollar"` / `"Dollar américain"`) are common in user-facing payloads and compose naturally with the existing `MarshallingContext.locale` setting — same pattern that `TimeZoneFormat.DISPLAY_NAME_*` uses for time zones. Worth lifting back into scope. |
| `Class` | Marked `SKIP` in TODO-50 on the basis that `ClassSwap` had no format variants. Revisiting: `FQCN` vs `BINARY_NAME` vs `SIMPLE_NAME` are real distinctions on the wire (nested-type encoding, serialize-only display form), and `ClassSwap` is small enough to delete outright once the format-installed path covers the default behavior. |

The pattern, defaults, and parser-leniency rules are identical to TODO-50 — this plan exists primarily to lock four new enum surfaces and capture the per-type decisions, not to relitigate the architecture.

---

## Compatibility

Same hard-break / no-shim stance as TODO-50. **Ships in 9.6** (the same major release that ships TODO-50's three phases). Per-type breakage notes:

- **`Boolean`** — net-new. No removal, no breakage. Adding `BooleanFormat` and a `booleanFormat(...)` setter is purely additive.
- **`Float` / `Double` non-finite** — net-new enum. The previously-default emit behavior for `NaN` was format-specific (some serializers crashed, some emitted bare `NaN`); the new default `NaN_AS_NULL` is a **wire-shape change** for callers who happened to be relying on the bare-token behavior in JSON. Documented as a 9.6 release-note callout; callers who want the old behavior opt into `NaN_AS_NUMBER` explicitly.
- **`Currency`** — net-new. No removal. ISO code default preserves the natural `Currency.toString()` output that today's serializers fall through to.
- **`Class`** — `ClassSwap` is **deleted outright** (hard break, no delegator). This is the only non-trivial caller-visible break in the round. Callers with `@Swap(impl=ClassSwap.class)` move to `classFormat(ClassFormat.FQCN)`; the default behavior (`FQCN` = `Class.getName()`) is identical so most call sites can simply drop the `@Swap` annotation.

The wire-direction forward-compatibility guarantee from TODO-50 carries forward: parsers stay format-agnostic in the read direction (see **Common precedence and parser leniency** below), so a 9.6 service still reads everything a 9.5 service emitted for these types. Only the source-and-binary API surface breaks (specifically `ClassSwap` for the `Class` row).

---

## Reference pattern

Same template as TODO-50; see its **Reference pattern** section for the full list of moving parts (enum + `NOT_SET` sentinel + `format(T)` / `parse(String)` helpers; context builder field + `PROP_<name>` + `env(...)` default + copy-from constructors + `hashKey()` + getter; `MarshallingContextable.Builder` delegating mixin; `@Marshalled` / `@MarshalledProp` / `@MarshalledConfig` annotation fields + applier wiring; `MarshalledPropertyPostProcessor` precedence branch). Every new enum in this plan reuses the exact same template — the per-type detail is just (a) the constants, (b) whether any swap class is replaced, (c) the default value, and (d) the per-type parser-leniency wrinkle.

The two cleanest reference implementations to copy from are:

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/EnumFormat.java` — net-new format enum with multiple textual variants and a numeric escape (`ORDINAL`). Closest template for `BooleanFormat`.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/LocaleFormat.java` — minimal 2-constant enum with locale-sensitive concerns. Closest template for `CurrencyFormat`.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BinaryFormat.java` — format-agnostic parser sniff. Template for `ClassFormat`'s `FQCN` / `BINARY_NAME` accept-both-decode logic.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BigNumberFormat.java` — formatter that returns `Object` so binary serializers can pick up native wire types via runtime-type branching. Template for `FloatFormat` (which similarly needs to return `String` for `NaN_AS_STRING`, native double for `NaN_AS_NUMBER`, `null` for `NaN_AS_NULL`, etc.).

---

## Per-type proposals

### Verdict table

| Type | Verdict | Enum name | Default | Replaces |
| --- | --- | --- | --- | --- |
| `Boolean` / `boolean` | INCLUDE | `BooleanFormat` | `TRUE_FALSE` | none today; net new |
| `Float` / `Double` non-finite (`NaN`, `±Infinity`) | INCLUDE | `FloatFormat` | `NaN_AS_NULL` | none today; net new |
| `Currency` | INCLUDE (revisit of TODO-50 SKIP) | `CurrencyFormat` | `ISO_CODE` | none today; net new |
| `Class` | INCLUDE (revisit of TODO-50 SKIP) | `ClassFormat` | `FQCN` | `ClassSwap` (DELETE outright — hard break) |

Subsections below detail each row.

---

### Boolean — INCLUDE

- **Existing surface:** none. `boolean` / `Boolean` values flow through each serializer session's `else if (sType.isBoolean())` branch (or the equivalent `JsonWriter.booleanValue(...)` / `XmlWriter.attr(name, value)` path) and end up as bare `true` / `false` on the wire. No swap class. Parsers route through `Boolean.parseBoolean` plus `BasicConverter`'s string-to-boolean fallback, which accepts `"true"` / `"false"` / `"1"` / `"0"` (case-insensitive) as a side-effect rather than a declared contract.
- **Today's default (no setting):** `true` / `false`. JSON emits bare tokens; XML / CSV / HTML / TOML / INI / Markdown / HOCON / HJSON emit unquoted `true` / `false`; MsgPack / CBOR / BSON / Proto emit native boolean wire types.
- **Proposed enum** `org.apache.juneau.BooleanFormat`:
  - `NOT_SET` — sentinel meaning "no value configured" — falls through to the next-higher precedence level.
  - `TRUE_FALSE` — `"true"` / `"false"` (today's default). Bare boolean tokens in JSON, unquoted text elsewhere.
  - `ZERO_ONE` — `0` / `1` as **numeric** tokens (not the string `"0"` / `"1"`). JSON emits `0` / `1`, useful for CSV-shaped JSON consumers and SQL-style payloads.
  - `YES_NO` — `"yes"` / `"no"`. Common in human-friendly CSV and form-encoded payloads.
  - `Y_N` — `"Y"` / `"N"`. Compact single-character form, common in DB and CSV exports.
  - `ON_OFF` — `"on"` / `"off"`. Common in config files (HOCON / INI / TOML) and HTML form checkboxes.
- **Default:** `TRUE_FALSE` (preserves today's wire).
- **Swaps replaced (NONE):** no swap class today; net new.
- **Annotation surface:** yes on all three (`@Marshalled.booleanFormat`, `@MarshalledProp.booleanFormat`, `@MarshalledConfig.booleanFormat`). Example:

  ```java
  public class Subscription {
      @MarshalledProp(booleanFormat=BooleanFormat.Y_N)
      public boolean active;  // emits "Y" / "N" on the wire
  }
  ```

- **Locale interaction:** none. The five textual variants are English-only by design (see **Out of scope** for locale-sensitive boolean handling).
- **Parser leniency:** lenient — accepts every form regardless of setting:
  - Exact tokens (case-insensitive on textual forms): `true`/`false`, `1`/`0`, `yes`/`no`, `y`/`n`, `on`/`off`.
  - Native JSON boolean tokens (`true` / `false`).
  - Native numeric `0` / `1` (when the JSON value is a number rather than a string).
  - The parser-side `BooleanFormat` setting is consulted **only** when a serializer-side hint disambiguates (e.g. an enum that has its own `"yes"` member colliding with a `BooleanFormat.YES_NO`-formatted field — vanishingly unlikely in practice; documented as informational).
- **Binary-serializer note:** **Native boolean wire type regardless of setting** in BSON / CBOR / MsgPack / Proto / Parquet. The setting only affects textual wire formats (JSON when emitted as string, XML attributes, CSV cells, URL-encoded form data, HOCON / TOML / INI / Markdown values). JSON's bare `true` / `false` is the natural wire form for `TRUE_FALSE` and is the default. Document this loudly in the enum Javadoc — mirroring how `BinaryFormat` documents the "native bytes in binary serializers" carve-out.

  Note that when the format is `ZERO_ONE`, JSON emits the **numeric** `0` / `1` token (not the string `"0"` / `"1"`), which means a `Boolean` field configured with `ZERO_ONE` rides the same wire shape as an `int` field. Useful for CSV-shaped JSON consumers and SQL-style payloads.

---

### Float / Double non-finite — INCLUDE

- **Existing surface:** none. `Float` / `Double` / `float` / `double` values flow through each serializer session's `sType.isFloat()` / `isDouble()` / `isNumber()` branches. For finite values, the wire is a bare numeric token (e.g. `3.14`, `-0.5`, `1.0E10`). For **non-finite** values — `Double.NaN`, `Double.POSITIVE_INFINITY`, `Double.NEGATIVE_INFINITY` — today's behavior is format-specific:
  - JSON / JSON5 / Json — `JsonWriter.numberValue(Double)` emits the bare token (`NaN`, `Infinity`, `-Infinity`), which is **not valid JSON** per RFC 8259 §6 but is accepted by lenient parsers (including Juneau's own).
  - XML / HTML / CSV / HOCON / Markdown / TOML / INI — likewise emit the bare token, with no awareness that consumers may reject it.
  - MsgPack / CBOR — native IEEE-754 double, which can represent `NaN` / `Infinity` natively; no escaping needed.
  - BSON — `bson_double` likewise natively supports IEEE-754 specials.
- **Today's default (no setting):** bare numeric token in text formats (often non-spec-compliant); native IEEE-754 in binary formats.
- **Proposed enum** `org.apache.juneau.FloatFormat`:
  - `NOT_SET` — sentinel.
  - `NaN_AS_NULL` (default) — emit `null` for `NaN` / `±Infinity`. **JSON-spec-compliant.** Parsers reading `null` back into a primitive `double` produce `Double.NaN`; reading into a boxed `Double` produces `null`.
  - `NaN_AS_STRING` — emit `"NaN"` / `"Infinity"` / `"-Infinity"` (quoted string). **Round-trip preserving** — the parser unambiguously decodes the string back to the original non-finite value.
  - `NaN_AS_NUMBER` — emit the bare `NaN` / `Infinity` / `-Infinity` token. **Non-standard JSON** per RFC 8259 but accepted by lenient parsers (Juneau's default, V8, several Python libs). Documented as "use only with lenient consumers".
  - `NaN_AS_ERROR` — throw `SerializeException` (or `IOException` for the streaming serializers) when a non-finite value is encountered. **Fail-fast** for data-cleanliness pipelines where a `NaN` indicates upstream corruption.
- **Default:** `NaN_AS_NULL` (JSON-spec-compliant; closest to "least surprise" for the common case where the producer doesn't know whether the consumer is strict).
- **Swaps replaced (NONE):** no swap class today; net new. Finite values are unaffected — the setting only fires when the value is `Double.isNaN(v)` or `Double.isInfinite(v)`.
- **Annotation surface:** yes on all three (`@Marshalled.floatFormat`, `@MarshalledProp.floatFormat`, `@MarshalledConfig.floatFormat`). Example:

  ```java
  public class SensorReading {
      @MarshalledProp(floatFormat=FloatFormat.NaN_AS_STRING)
      public double temperature;  // emits "NaN" when sensor is offline, round-trips losslessly
  }
  ```

- **Locale interaction:** none.
- **Parser leniency:** lenient — accepts **all** forms regardless of setting:
  - Bare numeric `NaN` / `Infinity` / `-Infinity` token (the `NaN_AS_NUMBER` shape).
  - Quoted-string `"NaN"` / `"Infinity"` / `"-Infinity"` (the `NaN_AS_STRING` shape).
  - `null` — only treated as `NaN` / `±Infinity` reconstruction when reading into a primitive `double`; into a boxed `Double` field, `null` stays `null` (no information about which non-finite it was, so the only safe answer is `null`).
  - Finite numeric tokens parse as themselves.
- **Binary-serializer note:** BSON / CBOR / MsgPack / Proto / Parquet emit native IEEE-754 double regardless of setting (native `NaN` / `Infinity` / `-Infinity` representations exist in all of them). The setting only affects text-based serializers. Document this in the enum Javadoc alongside the same note that applies to `BinaryFormat` and `BigNumberFormat`.

---

### Currency — INCLUDE (revisit of TODO-50 SKIP)

- **Existing surface:** none. `Currency` (which is `java.util.Currency`) flows through the generic `Object.toString()` path in every serializer session, which calls `Currency.toString()` — equivalent to `Currency.getCurrencyCode()` (`"USD"`, `"EUR"`, `"JPY"`, …). No swap class. Parsers route through `BasicConverter` which handles `Currency.getInstance(String)` for string input.
- **Today's default (no setting):** ISO 4217 currency code — `"USD"`, `"EUR"`, `"JPY"`.
- **Proposed enum** `org.apache.juneau.CurrencyFormat`:
  - `NOT_SET` — sentinel.
  - `ISO_CODE` (default) — `Currency.getCurrencyCode()`, e.g. `"USD"`. ISO 4217, locale-independent, round-trip safe, the only constant safe for machine-to-machine wires.
  - `SYMBOL` — `Currency.getSymbol(locale)`, e.g. `"$"` for USD in `en_US`, `"US$"` for USD in `en_AU`. **Locale-sensitive** (consults `MarshallingContext.locale`).
  - `DISPLAY_NAME` — `Currency.getDisplayName(locale)`, e.g. `"US Dollar"` in `en_US`, `"Dollar américain"` in `fr_FR`. **Locale-sensitive** (consults `MarshallingContext.locale`).
- **Default:** `ISO_CODE` (preserves today's `Currency.toString()` wire).
- **Swaps replaced (NONE):** no swap class today; net new.
- **Annotation surface:** yes on all three (`@Marshalled.currencyFormat`, `@MarshalledProp.currencyFormat`, `@MarshalledConfig.currencyFormat`). Example:

  ```java
  public class PriceTag {
      @MarshalledProp(currencyFormat=CurrencyFormat.SYMBOL)
      public Currency currency;  // emits "$" / "€" / "¥" per session locale
  }
  ```

- **Locale interaction:** `SYMBOL` and `DISPLAY_NAME` both compose with the existing `MarshallingContext.locale` setting. No new plumbing needed — the format helper reads `session.getLocale()` (or falls back to `Locale.getDefault()` when no session locale is configured). Document this composability in the enum Javadoc.
- **Parser leniency:** lenient with caveats:
  - `ISO_CODE` always parses unambiguously via `Currency.getInstance(String)`.
  - `SYMBOL` parsing is **best-effort and locale-sensitive** — `"$"` could mean USD, CAD, AUD, MXN, HKD, SGD, NZD, etc. depending on locale. The parser tries `Currency.getInstance` first (fails for non-ISO input), then iterates `Currency.getAvailableCurrencies()` filtering by `c.getSymbol(session.getLocale()).equals(input)` and returns the first match; ambiguity falls back to throwing `IllegalArgumentException`. Document the round-trip caveat loudly.
  - `DISPLAY_NAME` parsing has the same caveats and uses the same iterate-and-match strategy against `c.getDisplayName(session.getLocale())`. Even less reliable than `SYMBOL` — `"Dollar"` matches at least eight ISO codes.
  - **Round-trip is strictly guaranteed only for `ISO_CODE`.** Document this as the headline caveat and recommend `ISO_CODE` as the only setting safe for machine-to-machine wires.
- **Binary-serializer note:** UTF-8 string regardless of setting. There is no native `Currency` wire type in BSON / CBOR / MsgPack / Proto / Parquet, so all variants stringify identically — the choice of `ISO_CODE` vs `SYMBOL` vs `DISPLAY_NAME` is purely about the textual content of that string.

---

### Class — INCLUDE (revisit of TODO-50 SKIP)

- **Existing surface:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ClassSwap.java` — single concrete `StringSwap<Class<?>>` (not a family of inner classes like `TemporalSwap`). 8 lines of `swap` (`o.getName()`) and `unswap` (`Class.forName(o)`). Registered as a default swap in `DefaultSwaps.SWAPS`.
- **Today's default (no setting):** `Class.getName()` → `java.util.Map`, `java.util.Map$Entry`, `int`, `[Ljava.lang.String;`. This is the FQCN form.
- **Proposed enum** `org.apache.juneau.ClassFormat`:
  - `NOT_SET` — sentinel.
  - `FQCN` (default) — `Class.getName()`. The internal JVM binary name. For nested types, uses `$` as the separator (`java.util.Map$Entry`). For arrays, uses the JVM descriptor (`[Ljava.lang.String;`, `[I`). **Round-trip safe via `Class.forName(name)`.**
  - `BINARY_NAME` — same as `FQCN` for nested types and arrays (the JLS calls these "binary names"). For non-nested non-array reference types, identical to `FQCN`. **Round-trip safe.** Included for explicitness when callers want to be loud about the encoding choice.
  - `SIMPLE_NAME` — `Class.getSimpleName()` → `Map`, `Entry`, `int`, `String[]`. Discards package and outer-class context. **Not round-trip safe** — there is no unambiguous way to resolve `"Map"` back to a `Class` instance without a hint. **Serialize-only**; the parser path for this constant throws `IllegalArgumentException` ("`SIMPLE_NAME` is serialize-only — use `FQCN` or `BINARY_NAME` for round-trippable wires").
- **Default:** `FQCN` (preserves today's `ClassSwap` default behavior).
- **Swaps replaced (DELETE):** `ClassSwap` is **deleted outright** — hard break, no delegator. Reasoning:
  - The new format-installed path (`MarshalledPropertyPostProcessor` branch on `propertyClass.equals(Class.class)`) is functionally identical to today's `ClassSwap` when `classFormat=FQCN` (the default).
  - `ClassSwap` is the only swap-elimination in this round, and unlike the swap-family removals in TODO-50 Phase 1 (which removed 50+ classes), this is a single class — caller migration is one line per call site.
  - A delegator would just be `ClassSwap` reading `session.getMarshallingContext().getClassFormat()` and dispatching to the helper, which is more indirection than value. Cleaner to delete.
  - Callers with `@Swap(impl=ClassSwap.class)` on a `Class<?>` field drop the annotation entirely (default behavior is unchanged) or migrate to `@MarshalledProp(classFormat=ClassFormat.SIMPLE_NAME)` if they want a different format. Migration guide row added.
- **Annotation surface:** yes on all three (`@Marshalled.classFormat`, `@MarshalledProp.classFormat`, `@MarshalledConfig.classFormat`). Example:

  ```java
  public class TypeDescriptor {
      @MarshalledProp(classFormat=ClassFormat.SIMPLE_NAME)
      public Class<?> displayType;  // emits "Map" instead of "java.util.Map" for UI display
  }
  ```

- **Locale interaction:** none.
- **Parser leniency:** lenient on read regardless of setting:
  - Try `Class.forName(name)` first — handles both `FQCN` and `BINARY_NAME` shapes (they're identical for the input strings the parser cares about).
  - If that fails and the input contains `.`, try substituting `$` for the trailing `.<UpperCaseIdent>` segments (heuristic for nested-type names written with `.` separators by lenient producers — `java.util.Map.Entry` → `java.util.Map$Entry`). This is the same trick `Class.forName` itself doesn't do but several other libraries (e.g. Jackson `TypeFactory.findClass`) implement.
  - **`SIMPLE_NAME` is not supported on the parser side.** There is no defensible way to resolve `"Map"` back to a unique `Class` without a registry hint, and adding a `classRegistry` setting just to support lossy round-trip is out of scope for this round. Document loudly in the `SIMPLE_NAME` Javadoc.
- **Binary-serializer note:** UTF-8 string regardless of setting. There is no native `Class` wire type in BSON / CBOR / MsgPack / Proto / Parquet, so all variants stringify identically.

---

## Common precedence and parser leniency

Both rules below apply uniformly to every new `<type>Format` setting introduced by this plan. They are identical to the rules in TODO-50's **Common precedence and parser leniency** section — repeated here for completeness but not relitigated.

**Precedence (highest to lowest):**

1. `@MarshalledProp(<type>Format=…)` on the bean property (innerField → getter → setter).
2. `@Marshalled(<type>Format=…)` on the bean class (or any superclass / interface — uses normal annotation inheritance).
3. `@MarshalledConfig(<type>Format=…)` on the `@Rest`-annotated class / method, applied via `MarshalledConfigAnnotation.Applier`.
4. Programmatic `MarshallingContext.Builder.<type>Format(…)`.
5. Environment var `MarshallingContext.<type>Format` (consumed in the builder constructor via `env(...)`).
6. The enum's documented default constant.

Implementation note: the precedence chain is encoded in `MarshalledPropertyPostProcessor.process(...)` — each of the four new types adds a parallel branch alongside the existing `propertyClass.equals(Duration.class)` / `Period.class` / `Currency.class` / etc. branches.

**Parser leniency:**

> Parsers SHALL accept any wire shape that the corresponding serializer can produce, regardless of the parser-side `<type>Format` setting. The parser-side setting is consulted only for genuinely ambiguous input. For each type's ambiguous-input rules, see the type's subsection above.

This matches TODO-50's locked decision and is the reason every parser session for these four types works against a serializer configured for any of the new enum constants without explicit coordination.

---

## Swap elimination matrix

Only one entry in this round:

| Swap class / setting | Action in 9.6 | Phase |
| --- | --- | --- |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ClassSwap.java` | **DELETE** outright — subsumed by `ClassFormat`; default behavior (`FQCN`) matches `ClassSwap.swap` exactly. No delegator. Caller migration is one line per call site (drop the `@Swap(impl=ClassSwap.class)` annotation or switch to `@MarshalledProp(classFormat=…)`). Migration guide gets an old→new row. | A |

`BooleanFormat`, `FloatFormat`, and `CurrencyFormat` are all net-new — no swap classes exist today to remove.

---

## Phasing

**Single phase recommended** (reserved `TODO-55`). All four enums are small (between 4 and 6 constants each), have no inter-dependencies, and the only non-trivial caller-visible break (`ClassSwap` deletion) is a one-line migration per call site. Bundling them into a single follow-up TODO mirrors how TODO-53 (Phase 3 of TODO-50) bundled UUID + BigNumber.

The single phase touches:

- 4 new files in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/`: `BooleanFormat.java`, `FloatFormat.java`, `CurrencyFormat.java`, `ClassFormat.java`.
- 1 deleted file: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ClassSwap.java`.
- `MarshallingContext.java` (4 new fields + 4 new `PROP_*` constants + 4 new `env(...)` defaults + 4 new copy-from lines × 2 constructors + 4 new `hashKey()` entries + 4 new getters).
- `MarshallingContextable.java` (4 new delegating setters).
- `Marshalled.java` + `MarshalledAnnotation.java` (4 new attributes + 4 new applier branches).
- `MarshalledProp.java` + `MarshalledPropAnnotation.java` (4 new attributes + 4 new applier branches).
- `MarshalledConfig.java` + `MarshalledConfigAnnotation.java` (4 new attributes + 4 new applier branches).
- `MarshalledPropertyPostProcessor.java` (4 new precedence branches, one per type).
- `DefaultSwaps.java` (remove the `ClassSwap` registration; the new `ClassFormat` path is installed via `MarshalledPropertyPostProcessor` rather than the default-swap registry).
- Per-format serializer sessions touching `sType.isBoolean()` / `isFloat()` / `isDouble()` / `isNumber()`-with-non-finite-check / `Currency.class.equals(...)` / `Class.class.equals(...)` branches — concretely: `JsonSerializerSession`, `Json5SerializerSession`, `XmlSerializerSession`, `HtmlSerializerSession`, `CsvSerializerSession`, `HoconSerializerSession`, `HjsonSerializerSession`, `TomlSerializerSession`, `IniSerializerSession`, `MarkdownSerializerSession`, `UonSerializerSession`, `MsgPackSerializerSession`, `CborSerializerSession`, `BsonSerializerSession`, `ProtoSerializerSession`, `ParquetSerializerSession`. (Most of these are no-op for `Boolean` and `Float` because native wire types apply — the per-session changes are limited to JSON / XML / CSV / HOCON / etc. text formats.)
- Tests in `juneau-utest/src/test/java/org/apache/juneau/transforms/`: a new `BooleanFormat_Test`, `FloatFormat_Test`, `CurrencyFormat_Test`, `ClassFormat_Test` (one per enum, mirroring the structure of `EnumFormat_Test` and `BigNumberFormat_Test`); plus per-serializer round-trip tests modeled on `BuiltInDateTimeSerialization_Test`.

Estimated scope: roughly equivalent to TODO-53 (UUID + BigNumber bundled) — between 1 and 2 days of focused implementation work plus the per-session-branch sweep.

**Alternative split (only if Phase A turns out large):** group as Phase A (`BooleanFormat` + `FloatFormat` — net-new, no swap removal, no locale interaction) and Phase B (`CurrencyFormat` + `ClassFormat` — `Class` has the swap removal, `Currency` has the locale interaction). The split is mechanical and either order works; the recommendation stands at **single phase**.

---

## Locked decisions (implementation-ready)

These mirror TODO-50's locked-decisions numbering format; the user is recommendation-friendly so most decisions are locked, with the single naming choice (`FloatFormat` vs alternatives) surfaced as an open question below.

1. **Enum names locked:** `BooleanFormat`, `FloatFormat`, `CurrencyFormat`, `ClassFormat`. All live in `org.apache.juneau.*` (top-level, alongside `EnumFormat` / `LocaleFormat` / `BinaryFormat` / `BigNumberFormat`). The `FloatFormat` vs `DoubleFormat` vs `NonFiniteFormat` vs `NumberFormat` question is surfaced below but the recommendation is `FloatFormat` (covers `Float` + `Double`, signals "floating-point", avoids the `java.text.NumberFormat` clash).
2. **Defaults locked:** `BooleanFormat.TRUE_FALSE`, `FloatFormat.NaN_AS_NULL`, `CurrencyFormat.ISO_CODE`, `ClassFormat.FQCN`. Three of the four preserve today's wire exactly; only `FloatFormat.NaN_AS_NULL` changes the wire for callers who were relying on bare-token `NaN` emission (documented as a 9.6 release-note callout).
3. **`ClassSwap` deletion locked.** Hard break, no delegator, no shim. Caller migration is one line per call site (drop the `@Swap(impl=ClassSwap.class)` annotation or switch to `@MarshalledProp(classFormat=…)`). Migration guide row added.
4. **All four enums get the full annotation surface** (`@Marshalled`, `@MarshalledProp`, `@MarshalledConfig`). Same uniform-annotation-surface policy as TODO-50 locked decision #5 — even when the bean class isn't user-owned (`Boolean`, `Double`, `Currency`, `Class`), the class-level surface ships for discoverability and symmetry.
5. **`CurrencyFormat.SYMBOL` and `DISPLAY_NAME` parsing is best-effort and locale-sensitive.** Document the round-trip caveat in the enum Javadoc loudly. Recommend `ISO_CODE` as the only setting safe for machine-to-machine wires. The parser-side leniency rule applies (parsers always accept ISO codes regardless of setting) — `SYMBOL` / `DISPLAY_NAME` decode failures fall back to `Currency.getInstance(input)` first before throwing.
6. **`ClassFormat.SIMPLE_NAME` is serialize-only.** The parser path for this constant throws `IllegalArgumentException` with the message `"SIMPLE_NAME is serialize-only — use FQCN or BINARY_NAME for round-trippable wires"`. Document loudly. (There is no `classRegistry` hint setting — out of scope for this round.)
7. **`BooleanFormat.ZERO_ONE` emits the numeric `0` / `1` token** (not the string `"0"` / `"1"`) on JSON / JSON5 / MsgPack-as-text / CBOR-as-text. A `Boolean` field configured with `ZERO_ONE` rides the same wire shape as an `int` field. Useful for CSV-shaped JSON consumers and SQL-style payloads. Documented in the constant's Javadoc.
8. **`FloatFormat.NaN_AS_NUMBER` is non-standard JSON.** Document loudly. JSON parsers in lenient mode (Juneau's default) accept it; strict-mode parsers (e.g. some JavaScript `JSON.parse` consumers) reject it. The constant exists for callers who control both ends of the wire and want the compact bare-token emit without the quote-string overhead.
9. **Binary-serializer carve-out is uniform.** All four enums apply only to text-based wire formats. BSON / CBOR / MsgPack / Proto / Parquet emit native wire types regardless of setting — `Boolean` as native bool, `Float` / `Double` non-finite as native IEEE-754 specials, `Currency` as UTF-8 string (no native type), `Class` as UTF-8 string (no native type). Mirrors TODO-50's locked decision #6 carve-out for `byte[]` (binary serializers always emit native bytes regardless of `BinaryFormat`).
10. **Default-swap-registry rewiring for `Class`.** `DefaultSwaps.SWAPS` loses its `ClassSwap` entry; the `Class` precedence chain is installed via the new `MarshalledPropertyPostProcessor` branch instead, identical to how `Duration` / `Period` are installed today. Default behavior (`FQCN`) is unchanged — only the installation path moves.

---

## Out of scope

- **`Optional<T>` wire shape.** Whether `Optional<String>` serializes as `null` / `""` / a wrapper object is a **schema decision**, not a wire-format choice. Out of scope for this plan and for the format-control pattern in general.
- **`Map<NonString, V>` key serialization.** How a `Map<Integer, V>` key is encoded (bare numeric vs quoted string) is also a schema decision and is already governed by per-serializer behavior (`JsonSerializer.Builder.allowNonStringMapKeys(boolean)` etc.). Out of scope.
- **Number-format scientific-vs-plain notation for `BigDecimal`.** Already covered by `BigNumberFormat` (TODO-53) — the existing `NUMBER` / `STRING` / `AUTO` constants handle this. Not part of this round.
- **Locale-sensitive number formatting** (e.g. `1,234.56` for `en_US` vs `1.234,56` for `de_DE`). The locale story for numbers is non-trivial (`DecimalFormat` patterns, grouping separators, currency symbols position) and intersects with the parser's tolerance rules. Defer as a v2 concern if a real caller surfaces.
- **`URI` / `URL` format.** Still SKIP per TODO-50 — `UrlSwap` stays as-is. Nothing has changed.
- **`InetAddress` / `Charset` / `StackTraceElement` / `MatchResult` format.** Still SKIP per TODO-50. Existing swaps (`StackTraceElementSwap`, `MatchResultSwap`) stay as-is.
- **Per-locale boolean wire forms** (e.g. Spanish `"sí"` / `"no"`, German `"ja"` / `"nein"`, French `"oui"` / `"non"`). The five English variants in `BooleanFormat` are sufficient for the overwhelming majority of real-world wires; locale-sensitive boolean formatting is a v2 concern if asked. A locale-aware `BooleanFormat.LOCALIZED` constant could be added later without breaking the existing surface.
- **`Currency` arithmetic / `MonetaryAmount` (JSR 354) support.** This plan covers `java.util.Currency` only — the wire representation of a currency code / symbol / display name. Monetary-amount types (price + currency) are out of scope and have their own ecosystem (Joda-Money, JSR 354 `MonetaryAmount`).
- **`Class` registry / type-name aliases.** A `MarshallingContext.Builder.classAlias(String, Class<?>)` registry that would let `SIMPLE_NAME` round-trip is conceivable but out of scope for this round. If a real caller requests it, file as a follow-up TODO.
- **A `NumberFormat` enum covering integer formatting** (e.g. hex / octal / binary representation of `int` / `long`). Out of scope; integer values today are unambiguously emitted as bare decimal and there is no real-world demand for alternative bases on the wire.
- **Custom `Function<T, String>` escape hatches on the new enums.** Same exclusion as TODO-50 — users who need full custom rendering can still write a `StringSwap<T>` per field via `@Swap`.
- **Standalone per-type field annotations** (`@BooleanFormat`, `@FloatFormat`, `@CurrencyFormat`, `@ClassFormat`). Settled by TODO-50 locked decision #8 — the typed field on `@MarshalledProp` is the single annotation surface.
- **Deprecation shims for `ClassSwap`.** Excluded by TODO-50 locked decision #7 (hard break, no migrations, major release).

---

## Open questions

Only one — surfaced for the user's confirmation. Everything else is locked.

1. **`FloatFormat` enum name.** The proposed name `FloatFormat` covers both `Float` and `Double` non-finite handling and signals "floating-point". Alternatives considered:
   - `DoubleFormat` — accurate for the `Double.NaN` / `±Infinity` cases but slightly misleading for `Float` fields.
   - `NonFiniteFormat` — explicit about what it does but verbose and reads strangely in context (`@MarshalledProp(nonFiniteFormat=...)`).
   - `NumberFormat` — collides semantically with `java.text.NumberFormat`; bad choice.
   - `FloatingPointFormat` — accurate but cumbersome.

   **Recommendation: `FloatFormat`.** Concise, covers both `Float` and `Double`, signals the floating-point domain. The minor naming concern that `java.text.NumberFormat` exists in the standard library is addressed by **not** calling it `NumberFormat` — the recommended name is sufficiently different.

---

## References

- **Reference plans** (read in full before touching this one):
  - `todo/TODO-50-format-control-extension.md` — round-1 umbrella plan; mirror its structure.
  - `todo/TODO-4-duration-format-control.md` — original Duration / Period plan that established the template.
- **Per-type Java types** (the types whose wire formats this plan governs):
  - `java.lang.Boolean` / `boolean` — primitive boolean and its boxed type.
  - `java.lang.Float` / `float` / `java.lang.Double` / `double` — IEEE-754 floating-point primitives and boxed types. Non-finite values: `Double.NaN`, `Double.POSITIVE_INFINITY`, `Double.NEGATIVE_INFINITY` (and the `Float` equivalents).
  - `java.util.Currency` — ISO 4217 currency holder. `getCurrencyCode()`, `getSymbol(Locale)`, `getDisplayName(Locale)`.
  - `java.lang.Class` — `getName()`, `getSimpleName()`, `Class.forName(String)`.
- **Existing swap classes** (only one is touched by this plan):
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ClassSwap.java` — **DELETED** in the implementation phase.
- **Reference implementations to copy from** (pattern templates):
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/EnumFormat.java` — multi-textual-variant enum with `ORDINAL` numeric escape. Closest template for `BooleanFormat`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/LocaleFormat.java` — minimal 2-constant enum. Closest template for `CurrencyFormat` (locale-sensitive concerns).
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BinaryFormat.java` — format-agnostic parser sniff. Template for `ClassFormat`'s lenient decode logic.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BigNumberFormat.java` — formatter that returns `Object` so binary serializers can pick up native wire types via runtime-type branching. Template for `FloatFormat` (returns `String` / `null` / `double` depending on setting and value).
- **`MarshallingContext` wiring** (search these for the canonical pattern — exact line ranges drift, search by name):
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingContext.java` — search `durationFormat`, `periodFormat`, `enumFormat`, `localeFormat`, `binaryFormat`, `uuidFormat`, `bigNumberFormat` for the field / `PROP_*` / `env(...)` default / copy-from / `hashKey()` / getter pattern that the four new fields slot into.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingContextable.java` — same names, for the delegating-setter pattern.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java` — same names, for the per-type precedence branches and the swap-factory helpers.
- **Annotation surface** (the three sites each new attribute is added):
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/Marshalled.java` + `MarshalledAnnotation.java`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/MarshalledProp.java` + `MarshalledPropAnnotation.java`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/MarshalledConfig.java` + `MarshalledConfigAnnotation.java`.
- **CSV check** (TODO-50 had to call out a name collision with `org.apache.juneau.csv.ByteArrayFormat`):
  - Verified — no `boolean*Format`, `float*Format`, `currency*Format`, or `class*Format` enum exists under `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/csv/`. The four enum names are free.
- **Conventions:** `AGENTS.md`, `.cursor/skills/code-conventions/SKILL.md` (Javadoc, SSLLC, `assertBean`, `hashKey()` updates, fluent-setter style).
