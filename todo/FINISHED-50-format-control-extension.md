# TODO-50: Extend the MarshallingContext format-control pattern beyond Duration/Period

Source: user request on 2026-05-20 — *"I'm liking the pattern of adding durationFormat and periodFormat to MarshallingContext as a general way to define formats shared by both serializers and parsers. I think we should extend this to other common data types as well and perhaps eliminate some of our swaps. For example: calendarFormat, dateFormat, temporalFormat, timeZoneFormat, byteArrayFormat, enumFormat. Can you come up with a plan to extend this feature? Also can you see if it makes sense to add more format options?"*

This is the **umbrella plan**. Each phase below proposes its own follow-up `TODO-<id>-*.md` to be filed when implementation actually starts; this document is the survey + design + phasing only.

---

## Goal

Replace ad-hoc, scattered swap-per-format families (`TemporalCalendarSwap.IsoOffsetDateTime`, `ByteArraySwap.Hex`, `useEnumNames(true)`, etc.) with a uniform **`<type>Format` enum + `MarshallingContext.Builder.<type>Format(...)` setting + `@Marshalled` / `@MarshalledProp` / `@MarshalledConfig` annotation surface**, exactly mirroring what just landed for `Duration` / `Period` in TODO-4. The setting is shared by both serializers and parsers, parsers stay format-agnostic at the wire-shape level, and per-property annotations override the global setting. The existing per-format swap classes are then either **removed outright** (for the format-variant families) or **downgraded to thin delegators** (for the auto-registered single-format swaps).

Net effect for callers:

```java
// Today
JsonSerializer.create().swaps(TemporalCalendarSwap.IsoOffsetDateTime.class).useEnumNames().build();

// After
JsonSerializer.create().calendarFormat(CalendarFormat.ISO_OFFSET_DATE_TIME).enumFormat(EnumFormat.NAME).build();
```

---

## Compatibility

**This is a hard-break, no-migration release.** Shipping in the **next major release (9.6)**.

- Every removed swap class (`TemporalCalendarSwap.*`, `TemporalDateSwap.*`, `TemporalSwap.*`, `ByteArraySwap.*`) is gone on day one — no deprecation window, no shim classes, no `@Deprecated(forRemoval=true)` placeholders. Callers that reference these classes directly (e.g. `@Swap(impl=TemporalSwap.IsoLocalDate.class)`, `swaps(TemporalCalendarSwap.IsoOffsetDateTime.class)`) **will not compile** until they switch to the new `<type>Format` setting.
- The existing `Serializer.Builder.binaryFormat(BinaryFormat)` setter is **removed** and the canonical setting moves to `MarshallingContext.Builder.binaryFormat(BinaryFormat)`. Same for the parser-side `InputStreamParser.Builder.binaryFormat(...)` setter.
- The existing `useEnumNames(boolean)` setter on `MarshallingContext.Builder` is **removed**. Callers use `enumFormat(EnumFormat.NAME)` instead.
- The release notes get a dedicated 9.6 callout listing every removed class and setter with the one-line replacement; the v9.6 migration guide gets old→new rows for each.

Framing: a major version bump's whole purpose is to clean up cruft. The duration/period work already proved the pattern; the rest of this is mechanical follow-through.

The format-agnostic parser story (parsers accept any wire shape any serializer can produce) means the **wire is forward-compatible** in the read direction — a 9.6 service reads everything a 9.5 service emits. Only the source-and-binary API surface breaks.

---

## Reference pattern

The `Duration` / `Period` work that just landed (see `todo/TODO-4-duration-format-control.md`) is the template every new format setting below must match. The moving parts:

| Concern | File / construct |
| --- | --- |
| Public enum, `NOT_SET` sentinel, `format(T)` + `parse(String)` static helpers, `isNumeric()` | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/DurationFormat.java`, `PeriodFormat.java` |
| Context builder field + `PROP_<name>` constant + `env(...)` default + copy-from constructors + `hashKey()` entry + getter | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingContext.java` (search `durationFormat`) |
| Delegating builder mixin so every `Serializer.Builder` / `Parser.Builder` inherits the setter | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingContextable.java` (search `durationFormat`) |
| Class-level annotation field | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/Marshalled.java` + `MarshalledAnnotation.java` |
| Property-level annotation field | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/MarshalledProp.java` + `MarshalledPropAnnotation.java` |
| Config-level annotation field + applier wiring | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/MarshalledConfig.java` + `MarshalledConfigAnnotation.java` (search `durationFormat`) |
| Precedence resolution + swap installation | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java` — see the `propertyClass.equals(Duration.class)` blocks and the `durationSwap(...)` / `periodSwap(...)` helpers at the bottom |
| Parser-side leniency (sniff wire shape, hint only for ambiguous numerics) | `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/utils/Iso8601Utils.java` — `parseDuration(String, DurationFormat)` and `parsePeriod(String, PeriodFormat)` |
| Binary-format native wire types | The `durationSwap(...)` / `periodSwap(...)` lambdas return `Long` / `Double` / `Integer` for numeric formats — BSON/CBOR/MsgPack pick up the native wire type from the `Object` return; text formats stringify it |

Every new `<type>Format` below reuses this exact template — the per-type detail is just (a) the enum constants, (b) which existing swap class(es) it replaces, (c) the default value, and (d) any per-type parser-leniency wrinkle.

---

## Per-type proposals

### Verdict table

Re-sorted: INCLUDEs first (Phase 1 → Phase 2 → Phase 3), then MAYBEs, then SKIPs.

| Type | Verdict | Enum name | Default | Replaces |
| --- | --- | --- | --- | --- |
| `Calendar` (incl. `XMLGregorianCalendar`) | INCLUDE | `CalendarFormat` | `ISO_OFFSET_DATE_TIME` | `TemporalCalendarSwap.*` (17 inner classes); `XMLGregorianCalendarSwap` downgraded to delegator |
| `Date` | INCLUDE | `DateFormat` | `ISO_LOCAL_DATE_TIME` | `TemporalDateSwap.*` (17 inner classes) |
| `Temporal` | INCLUDE | `TemporalFormat` | `DEFAULT` (per-subtype, as today) | `TemporalSwap.*` (18 inner classes) |
| `TimeZone` + `ZoneId` | INCLUDE | `TimeZoneFormat` (shared) | `ID` | `TimeZoneSwap`, `ZoneIdSwap` (kept as delegators) |
| `Locale` | INCLUDE | `LocaleFormat` | `BCP_47` | `LocaleSwap` (kept as delegator) |
| `byte[]` | INCLUDE | (extend `BinaryFormat`) | `BASE64` | `ByteArraySwap.Base64` / `.Hex` / `.SpacedHex` |
| `Enum` | INCLUDE | `EnumFormat` | `TO_STRING` | the `useEnumNames` boolean toggle |
| `UUID` | INCLUDE | `UuidFormat` | `STANDARD` | none today; net new |
| `BigInteger` / `BigDecimal` | MAYBE | `BigNumberFormat` | `NUMBER` | none today; addresses JS-interop |
| `URI` / `URL` | SKIP | — | — | `UrlSwap` stays as-is |
| `Class` | SKIP | — | — | `ClassSwap` stays as-is |
| `Currency` / `Charset` / `StackTraceElement` / `MatchResult` | SKIP | — | — | existing swaps stay |
| `InputStream` / `Reader` content | SKIP | — | — | already covered by the `binaryFormat(BinaryFormat)` setting (now moved to `MarshallingContext.Builder`) |

Subsections below detail each `INCLUDE` and `MAYBE` row. `XMLGregorianCalendar` no longer has its own row — see the one-line note under **Calendar** below.

> Note on the existing `org.apache.juneau.csv.ByteArrayFormat` enum (CSV-only, `BASE64` / `SEMICOLON_DELIMITED`): renamed to `CsvByteArrayCellFormat` in Phase 2 to free up the `ByteArrayFormat` name semantically. The byte[] wire setting itself extends the existing top-level `org.apache.juneau.BinaryFormat` enum — there is no separate `ByteArrayFormat` enum in the final design.

---

### Calendar — INCLUDE

- **Existing surface:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TemporalCalendarSwap.java`. One outer class, 17 named inner subclasses each carrying a `DateTimeFormatter` pattern: `BasicIsoDate`, `IsoDate`, `IsoDateTime`, `IsoInstant`, `IsoLocalDate`, `IsoLocalDateTime`, `IsoLocalTime`, `IsoOffsetDate`, `IsoOffsetDateTime`, `IsoOffsetTime`, `IsoOrdinalDate`, `IsoTime`, `IsoWeekDate`, `IsoZonedDateTime`, `Rfc1123DateTime`. Plus a constructor that accepts any custom `DateTimeFormatter` pattern.
- **Today's default (no swap):** ISO 8601 via `Iso8601Utils.formatCalendar` → `DateTimeFormatter.ISO_OFFSET_DATE_TIME`. (`Iso8601Utils.java` line 117.)
- **Proposed enum** `org.apache.juneau.CalendarFormat` — constants mirroring the 17 swap variants 1-for-1 (same names), plus:
  - `NOT_SET` sentinel.
  - `MILLIS` — epoch millis as a numeric (see locked decision #1). Constant name matches `DurationFormat.MILLIS`.
  - `XML_FORMAT` — the existing `XMLGregorianCalendarSwap.toXMLFormat()` output (ISO-with-timezone). Available on `CalendarFormat` so callers can opt regular `Calendar` / `GregorianCalendar` fields into XML-schema-datetime form for symmetry with their `XMLGregorianCalendar` fields.
  - `CUSTOM` escape hatch that pairs with `MarshallingContext.Builder.calendarPattern(String)` for arbitrary `DateTimeFormatter` patterns.
- **Default:** `ISO_OFFSET_DATE_TIME` (preserves today's wire-compatible default).
- **Swaps replaced (REMOVE):** all 17 `TemporalCalendarSwap.*` inner classes and the outer `TemporalCalendarSwap` class. Callers using `TemporalCalendarSwap(String pattern)` for custom patterns move to `calendarFormat(CalendarFormat.CUSTOM).calendarPattern("...")`.
- **`XMLGregorianCalendar` (folded in):** `XMLGregorianCalendar` **always emits ISO-with-timezone (`toXMLFormat()`) regardless of the `CalendarFormat` setting**. This is the only sensible wire form for `XMLGregorianCalendar` and keeps the special-case logic at the boundary (one type, one rule) rather than threading a "fall back if not XML_FORMAT" branch through every serializer session. `XMLGregorianCalendarSwap` is **kept as a delegator** that reads this fixed behavior; its `DefaultSwaps` auto-registration moves to the new `MarshalledPropertyPostProcessor`-installed path so the per-property override surface still works (a user can put a custom `StringSwap<XMLGregorianCalendar>` on a field via `@Swap` if they need to override the always-XML behavior). The `XML_FORMAT` constant on `CalendarFormat` exists so callers can pick the same wire form for regular `Calendar` / `GregorianCalendar` fields and is what discoverability tooling (IDE autocomplete, Javadoc) points at.
- **Annotation surface:** yes on all three (`@Marshalled.calendarFormat`, `@MarshalledProp.calendarFormat`, `@MarshalledConfig.calendarFormat`). See locked decision #5 — class-level surface is shipped for every INCLUDE'd type uniformly even when the bean class isn't user-owned.
- **Locale / TimeZone interaction:** `MarshallingContext` already carries `locale` and `timeZone` fields (see `MarshallingContext.java` builder around lines 266-267); the per-format `DateTimeFormatter` instance handles locale/zone the same way the swaps do today. No new plumbing needed.
- **Parser leniency:** parsers should sniff: presence of `T` → date-time, presence of `+`/`-`/`Z` zone marker → zoned, etc. (`Iso8601Utils.selectParserFormatter`, line 374, already does this for `parseCalendar`.) The `calendarFormat` setting on the parser is consulted only when sniffing is ambiguous (e.g. `RFC_1123_DATE_TIME` looks nothing like ISO so sniffing handles it without help; `IsoOrdinalDate` vs `IsoLocalDate` is ambiguous for some inputs and may need the hint). For `MILLIS`, the wire is a bare integer — unambiguous on shape.
- **Binary-serializer note:** the `MILLIS` constant emits a native int64 in BSON / CBOR / MsgPack (matches BSON's native `Date` BCON type — see `BsonSerializerSession`). All other `CalendarFormat` variants are textual on the wire; binary serializers emit a UTF-8 string.

### Date — INCLUDE

- **Existing surface:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TemporalDateSwap.java`. Identical shape to `TemporalCalendarSwap` — same 17 inner classes plus custom-pattern constructor.
- **Today's default (no swap):** `Iso8601Utils.formatDate` → `DateTimeFormatter.ISO_LOCAL_DATE_TIME` (`Iso8601Utils.java` line 125). Note the deliberate divergence from `Calendar`: `Date` carries no zone so it emits a local date-time.
- **Proposed enum** `org.apache.juneau.DateFormat` — same 17 constants as `CalendarFormat` + `NOT_SET` + `MILLIS` (epoch millis from `Date.getTime()`) + `CUSTOM`.
- **Default:** `ISO_LOCAL_DATE_TIME` (preserves today's wire).
- **Swaps replaced (REMOVE):** all 17 `TemporalDateSwap.*` inner classes and the outer `TemporalDateSwap` class.
- **Annotation surface:** yes on all three.
- **Locale / TimeZone interaction:** identical to `Calendar`.
- **Parser leniency:** `Iso8601Utils.parseDate` already routes through `selectParserFormatter` (line 334). `MILLIS` is unambiguous on the wire (bare integer) and is the only place where the parser-side `dateFormat` hint matters — bare integers default to `MILLIS` if the hint is unset, mirroring the Duration `MILLIS` default.
- **Binary-serializer note:** `MILLIS` → native int64 in BSON / CBOR / MsgPack.

### Temporal — INCLUDE

- **Existing surface:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TemporalSwap.java`. 18 inner classes (the same 17 as Calendar/Date plus `IsoYear`, `IsoYearMonth`). Covers `Instant`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `OffsetTime`, `ZonedDateTime`, `Year`, `YearMonth`, `HijrahDate`, `JapaneseDate`, `MinguoDate`, `ThaiBuddhistDate`.
- **Today's default (no swap):** per-subtype defaults from `Iso8601Utils.DEFAULT_FORMATTERS` (`Iso8601Utils.java` lines 56-66) — `Instant` → `ISO_INSTANT`, `ZonedDateTime` → `ISO_OFFSET_DATE_TIME`, `LocalDate` → `ISO_LOCAL_DATE`, etc.
- **Proposed enum** `org.apache.juneau.TemporalFormat` — adds a `DEFAULT` constant (= "use the per-subtype default in `Iso8601Utils.DEFAULT_FORMATTERS`") on top of the 17 + 2 named variants. Plus `NOT_SET`, `MILLIS` (epoch millis — per-subtype semantics below, locked decision #12), `CUSTOM`.
  - **v1 deliberately ships only `MILLIS`** for epoch numerics — `SECONDS` / `NANOS` constants are not included in v1 (see locked decision #1). Easy to add later.
  - **`MILLIS` per-subtype semantics (locked decision #12 — midnight UTC for non-instant subtypes):**
    - `Instant` — `instant.toEpochMilli()`. Unambiguous.
    - `OffsetDateTime`, `ZonedDateTime` — `value.toInstant().toEpochMilli()`. Unambiguous.
    - `LocalDateTime` — interpret the local datetime as UTC: `ldt.toInstant(ZoneOffset.UTC).toEpochMilli()`.
    - `LocalDate` — midnight UTC at the start of the day: `ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`.
    - `YearMonth` — first day of the month at midnight UTC: `ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`.
    - `Year` — January 1 at midnight UTC: `Year.of(y).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`.
    - `LocalTime`, `MonthDay` — **no defensible epoch-millis interpretation** (no associated date / year). Fall back to the type's `DEFAULT` (ISO string form, e.g. `"14:23:05"` / `"--12-25"`). This asymmetry — `MILLIS` produces a number for most subtypes and a string for `LocalTime` / `MonthDay` — is documented loudly in the `TemporalFormat.MILLIS` Javadoc.
- **Default:** `DEFAULT` (preserves today's per-subtype wire).
- **Swaps replaced (REMOVE):** all 18 `TemporalSwap.*` inner classes and the outer `TemporalSwap` class. The internal pattern-selection logic moves into `Iso8601Utils` / the new `MarshalledPropertyPostProcessor` branch.
- **Annotation surface:** yes on all three. Particularly useful at the property level — e.g. `@MarshalledProp(temporalFormat=TemporalFormat.MILLIS) Instant createdAt;`.
- **Locale / TimeZone interaction:** the existing `TemporalSwap.convertToSerializable` (line 415) already consults `session.getTimeZoneId()` to convert local types to zoned types for non-zone-optional patterns. The new enum-driven path must preserve that logic; the conversion code moves into a private helper inside the new format pipeline.
- **Parser leniency:** `selectParserFormatter` (line 374) already auto-detects ISO-8601 shape. The parser-side `temporalFormat` hint is consulted only for: numeric input (`MILLIS` — bare integer ambiguity is moot in v1 since it's the only numeric format), and `IsoOrdinalDate` / `IsoWeekDate` which look like dates but aren't standard ISO.
- **Binary-serializer note:** `MILLIS` → native int64. `CUSTOM` and the textual variants → UTF-8 string.

### TimeZone (+ ZoneId) — INCLUDE

- **Existing surface:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TimeZoneSwap.java` (one variant, uses `TimeZone.getID()`); `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ZoneIdSwap.java` (one variant, uses `ZoneId.getId()`). Both are registered as auto-defaults in `org.apache.juneau.swap.DefaultSwaps` (lines 49-50).
- **Today's default:** `getID()` / `getId()` — produces things like `"America/New_York"`, `"GMT-05:00"`, `"UTC"`.
- **Proposed enum** `org.apache.juneau.TimeZoneFormat` (shared across `TimeZone` and `ZoneId`):
  - `NOT_SET`
  - `ID` — `getID()` / `getId()`, today's default.
  - `GMT_OFFSET` — `"GMT-05:00"` always (not the zone name even for IANA zones).
  - `DISPLAY_NAME_SHORT` — `TimeZone.getDisplayName(false, SHORT, locale)` (e.g. `"EST"`). Locale-sensitive.
  - `DISPLAY_NAME_LONG` — `TimeZone.getDisplayName(false, LONG, locale)` (e.g. `"Eastern Standard Time"`). Locale-sensitive.
  - `OFFSET_SECONDS` — integer seconds-from-UTC for the *current* instant.
- **Default:** `ID`.
- **Swaps replaced:** `TimeZoneSwap` and `ZoneIdSwap` are **KEPT** as classes but downgraded to delegators that read `MarshallingContext.getTimeZoneFormat()` at swap-time. They're auto-registered in `DefaultSwaps.SWAPS`; that registration moves to the new `MarshalledPropertyPostProcessor`-installed path so the format setting can actually take effect.
- **Annotation surface:** yes on all three (`timeZoneFormat`).
- **Locale interaction:** `DISPLAY_NAME_*` variants pull from `MarshallingContext`'s `locale`. Already wired.
- **Parser leniency:** parsers should accept any of `ID`, `GMT_OFFSET`, `OFFSET_SECONDS`. `DISPLAY_NAME_*` is parser-hostile (multiple zones share short names like `CST`) — for those formats, parsers consult the hint and fall back to `TimeZone.getTimeZone(str)` which Java already accepts liberally.
- **Binary-serializer note:** `OFFSET_SECONDS` → native int32 in BSON / CBOR / MsgPack; everything else → UTF-8 string.

### Locale — INCLUDE

- **Existing surface:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/LocaleSwap.java` — one variant, uses BCP-47 (`Locale.toLanguageTag()` → `Locale.forLanguageTag()`). Auto-registered in `DefaultSwaps.SWAPS` (line 46).
- **Today's default:** BCP-47 — `en-US`, `pt-BR`, etc.
- **Proposed enum** `org.apache.juneau.LocaleFormat` (smallest sensible set per locked decision):
  - `NOT_SET`
  - `BCP_47` — `en-US`, today's default.
  - `UNDERSCORE` — `en_US`, Java's classic `toString()` form. **This is the whole reason `LocaleFormat` is first-class** — legacy Java APIs and JVM properties consume `en_US`, BCP-47 services consume `en-US`, and round-tripping between the two without a per-field swap is a real pain point.
- **Default:** `BCP_47` (preserves wire).
- **Why no `LANGUAGE_ONLY` / `DISPLAY_NAME`:** `LANGUAGE_ONLY` (`en`) is lossy (drops country/variant) so it cannot round-trip and would only ever be valid as an output-only setting — adding it would force the parser-leniency rules to special-case "I lost information, fill in with `Locale.US`?" which is silly. `DISPLAY_NAME` (`"English (United States)"`) is locale-sensitive on the *output*, not parser-friendly, and definitely silly. Both dropped.
- **Swaps replaced:** `LocaleSwap` is **KEPT** as a delegator. Its `DefaultSwaps.SWAPS` auto-registration moves to the new enum-installed path so the format setting takes effect.
- **Annotation surface:** yes on all three (`localeFormat`).
- **Parser leniency:** accept both `en-US` and `en_US` regardless of setting (the format-agnostic rule from Common precedence below).
- **Binary-serializer note:** all variants → UTF-8 string.

### byte[] — INCLUDE (extend `BinaryFormat`)

- **Existing surface:** `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ByteArraySwap.java` — abstract base + 3 inner subclasses `Base64`, `Hex`, `SpacedHex`. Not auto-registered as a default swap (see `DefaultSwaps.SWAPS` — byte[] is absent), but every JSON/XML/CSV/etc. serializer that hits a `byte[]` field today routes through some explicit encoder (`base64Encode`, `toHex`, `toSpacedHex`). `OpenApiSerializerSession` (lines 341-345 and 506-510) and `CsvSerializerSession` (line 530) call these directly. The existing top-level `org.apache.juneau.BinaryFormat` enum (`SPACED_HEX` / `HEX` / `BASE64`) is used by the existing `OutputStreamSerializer.Builder.binaryFormat(...)` setting.
- **Today's default for byte[]:** Base64 for JSON, JSON5, XML, HTML, CSV; native bytes for MsgPack, CBOR, BSON, Proto (those have `sType.isByteArray()` branches that emit native binary — e.g. `MsgPackSerializerSession:287`, `CborSerializerSession:280`, `BsonSerializerSession:196`).
- **Design decision:** **reuse and extend the existing `org.apache.juneau.BinaryFormat`** enum rather than introduce a parallel `ByteArrayFormat`. The existing enum already exists, is named for the concept, and already has a `binaryFormat(...)` setter on `OutputStreamSerializer.Builder` / `InputStreamParser.Builder`. The byte[] use case and the stream-wrapping use case are the same concept ("how do bytes go on the wire as a string?"); having two enums for them would be the duplication this whole plan is supposed to eliminate.
- **Recommended additions to `BinaryFormat`:**
  - `BASE64_URL` — URL-safe Base64 alphabet (`-` / `_` instead of `+` / `/`), no padding. Clear value for byte[] fields embedded in URLs, cookies, JWT-style tokens. Common-enough on the wire that callers shouldn't have to write a swap for it.
- **Not recommended for v1:**
  - `DECIMAL_ARRAY` (`[26, 43, 60]`) — structurally different from the other constants (produces a real JSON array, not a string). Doesn't fit `CsvSerializer` / `TomlSerializer` / `IniSerializer` cell semantics at all. File as a future-add if a caller actually asks for it.
  - `BASE32` / `BASE85` — exotic. Skip.
- **Default:** `BASE64` (preserves today's wire).
- **Setting placement:** moves from `OutputStreamSerializer.Builder.binaryFormat(BinaryFormat)` / `InputStreamParser.Builder.binaryFormat(BinaryFormat)` to `MarshallingContext.Builder.binaryFormat(BinaryFormat)` per locked decision #6 — all format settings live in one place. The old setters on `OutputStreamSerializer.Builder` and `InputStreamParser.Builder` are **removed** (hard break, no deprecation).
- **Swaps replaced (REMOVE):** `ByteArraySwap.Base64`, `.Hex`, `.SpacedHex`, and the outer abstract `ByteArraySwap` class — gone entirely.
- **CSV-only `org.apache.juneau.csv.ByteArrayFormat` enum:** **renamed** to `org.apache.juneau.csv.CsvByteArrayCellFormat` in this same phase (locked decision #2). The CSV cell-level concern (`BASE64` vs `SEMICOLON_DELIMITED`) is a different axis from the wire-encoding choice — semicolon-delimited is a CSV-specific representation, not a generalizable byte encoding — so the rename clarifies the boundary and frees up the `ByteArrayFormat` name semantically (no enum will actually own it; `BinaryFormat` is the home).
- **Annotation surface:** yes on all three (`binaryFormat` on `@Marshalled` / `@MarshalledProp` / `@MarshalledConfig`).
- **Parser leniency:** parsers should sniff: starts with `0x` or alphanumeric and length is even → HEX; alphanumeric with spaces every 2 chars → SPACED_HEX; otherwise → BASE64 (covers both BASE64 and BASE64_URL since the parser-side decoder can accept either alphabet).
- **Binary-serializer note:** **all binary serializers continue to emit native bytes regardless of the configured `binaryFormat`**. This matches what Duration/Period do for binary serializers (native int / float instead of textual ISO). The setting only affects text formats. Document this loudly in the enum Javadoc.

### Enum — INCLUDE

- **Existing surface:** `MarshallingContext.Builder.useEnumNames(boolean)` (`MarshallingContext.java` line 3627). Read at `ClassMeta.toString(Object)` line 1426 and `ClassMeta.findEnumValues()` line 1591. No swap class — the choice is inlined in `ClassMeta`.
- **Today's default:** `useEnumNames=false` → emits `value.toString()`.
- **Proposed enum** `org.apache.juneau.EnumFormat`:
  - `NOT_SET`
  - `TO_STRING` — `value.toString()`, today's default.
  - `NAME` — `Enum.name()`, equivalent to `useEnumNames(true)` today.
  - `LOWER_HYPHEN` — `MY_ENUM_VALUE` → `"my-enum-value"`. Common API style.
  - `UPPER_HYPHEN` — `MY_ENUM_VALUE` → `"MY-ENUM-VALUE"`.
  - `LOWER_UNDERSCORE` — `MY_ENUM_VALUE` → `"my_enum_value"`.
  - `LOWER` — `name().toLowerCase(Locale.ROOT)`.
  - `UPPER` — `name().toUpperCase(Locale.ROOT)`.
  - `ORDINAL` — `Enum.ordinal()` as a numeric.
- **Default:** `TO_STRING` (preserves wire — locked decision #4).
- **Swaps replaced:** none (no swap class today). The `useEnumNames(boolean)` setter on `MarshallingContext.Builder` is **REMOVED** outright in this phase — no shim, no deprecation, no forwarding (locked decision #7, hard break). Callers migrate to `enumFormat(EnumFormat.NAME)` / `enumFormat(EnumFormat.TO_STRING)`.
- **Annotation surface:** yes on all three (`enumFormat`). Particularly useful per-property — e.g. `@MarshalledProp(enumFormat=EnumFormat.LOWER_HYPHEN) public Status status;`.
- **Locale interaction:** none. `LOWER` / `UPPER` use `Locale.ROOT` for case folding (avoid Turkish-I bugs).
- **Parser leniency:** parsers must accept **all** formats regardless of setting — try `name()` lookup first, then `toString()` round-trip, then case-insensitive match against each format above. The parser-side `enumFormat` is consulted only for genuine collisions (e.g. an enum that has both `name()=Foo` and another constant with `toString()="FOO"`).
- **Binary-serializer note:** `ORDINAL` → native int; all other formats → UTF-8 string.

### UUID — INCLUDE (Phase 3)

- **Existing surface:** none in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/`. `UUID` is handled today via its `Object.toString()` for text formats and via `ParquetSchemaBuilder` (line 316) for Parquet's `LOGICAL_TYPE_UUID`. `HttpPartSchema.isValidUuid` (line 4697) handles validation.
- **Today's default:** `UUID.toString()` → `"550e8400-e29b-41d4-a716-446655440000"`.
- **What other formats are there, concretely?**
  - `STANDARD` — 8-4-4-4-12 hyphenated form (`550e8400-e29b-41d4-a716-446655440000`). Java's `UUID.toString()`. Today's default.
  - `NO_DASHES` — same hex, no hyphens (`550e8400e29b41d4a716446655440000`). Real-world wire format for compact tokens, especially in URL paths and database column keys.
  - `URN` — `urn:uuid:550e8400-...` per RFC 4122. Standards-compliant but niche; surfaces in JSON-LD, SOAP, and some semantic-web payloads.
  - `BASE64` / `BASE64_URL` — 16-byte encoding, 22 chars unpadded. Exotic; overlaps semantically with the byte[] story for no real gain.
  - `BINARY` — native 16 bytes for binary serializers. Per the locked decision on binary serializers, this isn't a textual format — binary serializers always emit native bytes regardless of the textual setting (BSON `binData` subtype 4, MsgPack / CBOR 16-byte binary).
- **Verdict: INCLUDE** with the smallest sensible textual set — `STANDARD` (default), `NO_DASHES`, `URN`. Skip `BASE64` / `BASE64_URL` (overlaps confusingly with the byte[] `BinaryFormat` story). `BINARY` is not an enum constant — it's the implicit binary-serializer behavior, documented in the enum Javadoc alongside the same note that applies to byte[] and Duration.
- **Proposed enum** `org.apache.juneau.UuidFormat`:
  - `NOT_SET`
  - `STANDARD` — today's default; 8-4-4-4-12 hyphenated.
  - `NO_DASHES` — `550e8400e29b41d4a716446655440000`.
  - `URN` — `"urn:uuid:550e8400-..."`.
- **Default:** `STANDARD`.
- **Phasing rationale:** UUID lives in Phase 3 because there are no swaps to remove and the formatting logic is independent of the date/time and binary work — it can land without coordinating with the earlier phases. The value is real (NO_DASHES is genuinely useful) but lower priority than eliminating the 50+ date/time swap classes.
- **Swaps replaced:** none today; net new.
- **Annotation surface:** yes on all three (`uuidFormat`).
- **Parser leniency:** STANDARD / NO_DASHES / URN parse unambiguously by length + character set (32, 36, or 45 chars; presence of `urn:uuid:` prefix; presence of hyphens).
- **Binary-serializer note:** binary serializers always emit native bytes (BSON `binData` subtype 4 in particular has a dedicated UUID subtype). Document this alongside the parallel rule for byte[].

### BigInteger / BigDecimal — MAYBE (Phase 3)

- **Existing surface:** no dedicated swap. Treated as `isNumber()` in every serializer session and emitted as a bare numeric token.
- **Today's default:** bare number — `12345678901234567890` (a `BigInteger`) is emitted as `12345678901234567890` in JSON. This **exceeds `Number.MAX_SAFE_INTEGER` (2^53-1)** and silently loses precision in JS clients. Real foot-gun.
- **Proposed enum** `org.apache.juneau.BigNumberFormat`:
  - `NOT_SET`
  - `NUMBER` — today's default; bare numeric token.
  - `STRING` — quoted string, lossless round-trip.
  - `AUTO` — `NUMBER` when the value fits in `±2^53-1`, else `STRING`. Hybrid for safe JS interop.
- **Default:** `NUMBER` (preserves wire).
- **Verdict reason for MAYBE:** valuable for JS / DataPower / browser-facing services, but most Juneau consumers are server-to-server JVMs that don't care. Defer to Phase 3; revisit if `AUTO` is requested by a real caller.
- **Swaps replaced:** none; net new.
- **Annotation surface:** yes on all three (`bigNumberFormat`).
- **Parser leniency:** parsers always accept both bare numeric and quoted string regardless of setting.
- **Binary-serializer note:** native types in BSON / CBOR / MsgPack regardless of setting (BSON has `decimal128`; CBOR / MsgPack handle big ints via tag 2/3 or string fallback).

---

## Common precedence and parser leniency

Both rules below apply uniformly to **every** new `<type>Format` setting. Repeat them in the Javadoc of each new enum.

**Precedence (highest to lowest):**

1. `@MarshalledProp(<type>Format=…)` on the bean property (innerField → getter → setter).
2. `@Marshalled(<type>Format=…)` on the bean class (or any superclass / interface — uses normal annotation inheritance).
3. `@MarshalledConfig(<type>Format=…)` on the `@Rest`-annotated class / method, applied via `MarshalledConfigAnnotation.Applier`.
4. Programmatic `MarshallingContext.Builder.<type>Format(…)`.
5. Environment var `MarshallingContext.<type>Format` (consumed in the builder constructor via `env(...)`).
6. The enum's documented default constant.

Implementation note: the precedence chain is encoded in `MarshalledPropertyPostProcessor.process(...)` — see the existing `if (b.swap == null && Duration.class.equals(propertyClass) && …)` pattern. Every new type adds a parallel branch.

**Uniform annotation surface (locked decision #5):**

Every INCLUDE'd type ships the class-level `@Marshalled.<type>Format` annotation field alongside the property-level `@MarshalledProp.<type>Format` and the config-level `@MarshalledConfig.<type>Format`. This is true **even for the types whose bean class isn't user-owned** (`Calendar`, `Date`, `byte[]`, `UUID`, …). Reasons:

- Discoverability: builder autocomplete shows every `<type>Format(...)` setter, and `@Marshalled` IDE autocomplete shows the matching attributes for callers who declare a wrapper class around an unannotatable type.
- Symmetry: TODO-4's Duration/Period work already did this; introducing an inconsistency now would be a wart that pays for itself in zero ways.
- Cost: one builder method + one annotation attribute + one applier branch per type. Pattern-replicable.

**Setting placement is uniform (locked decision #6):**

Every new `<type>Format` setting lives on `MarshallingContext.Builder` (and is inherited by every concrete `Serializer.Builder` / `Parser.Builder` via the `MarshallingContextable.Builder` delegator mixin). This includes the **existing** `binaryFormat(BinaryFormat)` setting, which moves from `OutputStreamSerializer.Builder` / `InputStreamParser.Builder` to `MarshallingContext.Builder` as part of Phase 2 (hard break, no shim — the old setters are removed).

**No standalone per-type field annotations (locked decision #8):**

The plan does **not** ship `@CalendarFormat(IsoOffsetDateTime)`, `@DateFormat(...)`, `@TemporalFormat(...)`, `@EnumFormat(...)`, etc. as separate annotations on top of `@MarshalledProp`. The typed enum field on `@MarshalledProp` already covers every realistic use case and is more discoverable when you're already annotating the field for other reasons (column name, ignored-flag, etc.). Adding a parallel annotation surface would be dead weight.

**Parser leniency:**

> Parsers SHALL accept any wire shape that the corresponding serializer can produce, regardless of the parser-side `<type>Format` setting. The parser-side setting is consulted only for genuinely ambiguous input (e.g. a bare integer that could be `EPOCH_MILLIS` vs `EPOCH_SECONDS`). For each type's "ambiguous input" rules, see the type's subsection above.

This matches the locked decision from TODO-4 and is the reason every parser session for `Duration` works against a serializer configured for any of the seven `DurationFormat` constants without explicit coordination.

---

## Swap elimination matrix

All entries are either **REMOVE** (gone in 9.6, no deprecation window, callers update or fail to compile) or **KEEP** (the class stays, possibly downgraded to a delegator).

| Swap class / setting | Action in 9.6 | Phase |
| --- | --- | --- |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TemporalCalendarSwap.java` (outer + 17 inner) | **REMOVE** — subsumed by `CalendarFormat`; custom-pattern constructor replaced by `calendarFormat(CalendarFormat.CUSTOM).calendarPattern(String)` | 1 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TemporalDateSwap.java` (outer + 17 inner) | **REMOVE** — same as `TemporalCalendarSwap` | 1 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TemporalSwap.java` (outer + 18 inner) | **REMOVE** — same; inner classes likely the most-referenced of the three families. Migration guide gets explicit old→new rows | 1 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TimeZoneSwap.java` | **KEEP**, downgrade to delegator that reads `MarshallingContext.getTimeZoneFormat()` | 1 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ZoneIdSwap.java` | **KEEP**, downgrade to delegator | 1 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/LocaleSwap.java` | **KEEP**, downgrade to delegator that reads `MarshallingContext.getLocaleFormat()` | 1 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/XMLGregorianCalendarSwap.java` | **KEEP**, downgrade to delegator. `XMLGregorianCalendar` always emits `toXMLFormat()` regardless of `CalendarFormat`. The `XML_FORMAT` constant on `CalendarFormat` lets callers opt regular `Calendar` fields into the same wire form. | 1 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ByteArraySwap.java` (outer + 3 inner) | **REMOVE** — subsumed by extending `org.apache.juneau.BinaryFormat`. Test references in `juneau-utest/src/test/java/org/apache/juneau/transforms/ByteArrayBase64Swap_ComboRoundTripTest.java` get rewritten to use `binaryFormat(BinaryFormat.BASE64)` instead. | 2 |
| `MarshallingContext.Builder.useEnumNames(boolean)` | **REMOVE** — replaced by `enumFormat(EnumFormat.NAME)` / `enumFormat(EnumFormat.TO_STRING)`. No forwarding shim. | 2 |
| `OutputStreamSerializer.Builder.binaryFormat(BinaryFormat)` | **REMOVE** — moved to `MarshallingContext.Builder.binaryFormat(BinaryFormat)` per locked decision #6. The same setting now controls both byte[] wire encoding and stream-wrapping. | 2 |
| `InputStreamParser.Builder.binaryFormat(BinaryFormat)` | **REMOVE** — same move to `MarshallingContext.Builder` | 2 |
| `org.apache.juneau.csv.ByteArrayFormat` (CSV-only enum) | **RENAME** to `org.apache.juneau.csv.CsvByteArrayCellFormat`. CSV-specific cell-level concern; rename frees up the `ByteArrayFormat` name and clarifies that the cell-level semicolon-delimited representation is a different axis from the wire-encoding choice. | 2 |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/UrlSwap.java` | **KEEP** as-is — no format variants | — |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ClassSwap.java` | **KEEP** as-is — no format variants | — |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/StackTraceElementSwap.java` | **KEEP** as-is — no format variants | — |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/MatchResultSwap.java` | **KEEP** as-is — no format variants | — |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/InputStreamSwap.java` (outer + 3 inner) | **KEEP** for now — covers the niche of *embedding* a stream in a string-only format. Revisit only if a future iteration surfaces a unified design with byte[]. | — |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ReaderSwap.java`, `ParsedReaderSwap.java` | **KEEP** as-is — no format variants | — |
| `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/MarshallingStringSwap.java`, `StringFormatSwap.java` | **KEEP** as-is — internal infrastructure, not user-format swaps | — |

**Breaking-change risk callouts:**

- The inner `Temporal*Swap` classes are publicly referenced as `@Swap(impl=...)` targets in test fixtures (`juneau-utest/src/test/java/org/apache/juneau/transforms/TemporalSwap_StringSwapTest.java`, `TemporalCalendarSwap_StringSwapTest.java`, `TemporalDateSwap_StringSwapTest.java`) and likely in downstream user code. **9.6 source-and-binary breaks** anyone with `@Swap(impl=org.apache.juneau.swaps.TemporalSwap$IsoDate)` on a field — the migration is one-line (`@MarshalledProp(temporalFormat=TemporalFormat.ISO_DATE)`) and gets explicit old→new rows in the v9.6 migration guide.
- `ByteArraySwap.Base64.class` is referenced in `juneau-utest/src/test/java/org/apache/juneau/transforms/ByteArrayBase64Swap_ComboRoundTripTest.java`. Tests are updated to use `binaryFormat(BinaryFormat.BASE64)` instead.
- The wire **is** forward-compatible (a 9.6 service decodes everything a 9.5 service emits — the format-agnostic parser rule guarantees this). Only the source-and-binary API surface breaks.

---

## Phasing

Three phases, in order. **Only this umbrella TODO (TODO-50) gets a file in this pass**; the per-phase TODO ids below are *reservations* to be filed at phase-start. All three phases land in 9.6 (the next major release); the phasing is purely an in-release execution sequence to keep each landing reviewable, not a deprecation cycle.

Reserved TODO ids: **TODO-51** (Phase 1), **TODO-52** (Phase 2), **TODO-53** (Phase 3).

### Phase 1 — Date/time + Locale (reserved `TODO-51`)

- New enums: `CalendarFormat`, `DateFormat`, `TemporalFormat`, `TimeZoneFormat`, `LocaleFormat`.
- **Removes** all 17+17+18+0 inner-class swap variants (`Temporal{Calendar,Date}Swap.*`, `TemporalSwap.*`) and their outer classes. **Downgrades** `TimeZoneSwap`, `ZoneIdSwap`, `LocaleSwap`, `XMLGregorianCalendarSwap` to delegators.
- Touches `Iso8601Utils` (the formatting path becomes enum-aware), `MarshalledPropertyPostProcessor` (five new precedence branches), and every per-format serializer session that has a `sType.isCalendar()` / `isDate()` / `isTemporal()` branch (Cbor, MsgPack, BSON, JSON, JSON5, XML, HTML, CSV, HOCON, HJSON, TOML, INI, UON, Proto, Markdown).
- Phase 1 also lands the `MILLIS` constant on `CalendarFormat` / `DateFormat` / `TemporalFormat` (locked decision #1) and the `XML_FORMAT` constant on `CalendarFormat` (XMLGregorianCalendar fold-in).
- **Why first:** highest value, highest swap-elimination payoff (52 swap classes removed), and largest test surface to settle — best to land while the Duration/Period pattern is still fresh. Locale fits naturally with the i18n-adjacent types and joins this phase rather than getting its own slot.
- **Estimated scope:** roughly the same churn as the Duration/Period work plus the per-session updates.

### Phase 2 — Binary + Enum (reserved `TODO-52`)

- **Extends** `org.apache.juneau.BinaryFormat` with `BASE64_URL`. New enum: `EnumFormat`.
- **Removes** `ByteArraySwap.Base64`, `.Hex`, `.SpacedHex`, the outer `ByteArraySwap` class, the `useEnumNames(boolean)` setter, and the existing `binaryFormat(BinaryFormat)` setter on both `OutputStreamSerializer.Builder` and `InputStreamParser.Builder` (moves to `MarshallingContext.Builder`).
- **Renames** `org.apache.juneau.csv.ByteArrayFormat` to `org.apache.juneau.csv.CsvByteArrayCellFormat`.
- Touches every text-format serializer session (the `isByteArray()` branch becomes format-aware) and `ClassMeta.toString(Object)` / `ClassMeta.findEnumValues()` for the enum side.
- **Why second:** smaller scope than Phase 1, but consolidates the `binaryFormat` setting placement before any new caller can adopt the old location.

### Phase 3 — Long tail (reserved `TODO-53`)

- New enums: `UuidFormat`, `BigNumberFormat`.
- Lowest priority — net-new format settings (no swaps to remove). Each is < 1 day of work given the Phase 1+2 plumbing is in place.
- Land each only if a concrete caller asks for it. If the BigNumber piece doesn't surface a real consumer by 9.6 cut, defer to 9.7 — it's the only `MAYBE` left in the verdict table.

---

## Locked decisions (implementation-ready)

These were the open questions in the previous draft; the user resolved them on 2026-05-20.

1. **`MILLIS` constant on date-family enums.** Add `MILLIS` (epoch milliseconds) to `CalendarFormat`, `DateFormat`, `TemporalFormat` in v1 (Phase 1). Constant name `MILLIS` matches the `DurationFormat.MILLIS` precedent. `SECONDS` / `NANOS` are not included in v1; can be added later if a caller asks.
2. **`ByteArrayFormat` naming collision resolved by rename.** Rename `org.apache.juneau.csv.ByteArrayFormat` to `org.apache.juneau.csv.CsvByteArrayCellFormat` in Phase 2. The byte[] wire setting itself reuses the existing top-level `org.apache.juneau.BinaryFormat` enum — there is no separate `ByteArrayFormat`.
3. **Reuse `BinaryFormat`, audit additions.** The new byte[] format setting extends `org.apache.juneau.BinaryFormat`. Recommended addition: `BASE64_URL` (URL-safe Base64 alphabet). `SPACED_HEX`, `HEX`, `BASE64` (existing) stay. `DECIMAL_ARRAY` and `BASE32` / `BASE85` are deferred / skipped.
4. **`EnumFormat` default is `TO_STRING`.** Preserves today's wire (`useEnumNames=false` was the historical default). Callers SHOULD pick `NAME` for new APIs and the Javadoc should say so.
5. **Class-level annotation surface is uniform.** Ship `@Marshalled.<type>Format` for every INCLUDE'd type even when the bean class isn't user-owned (`Calendar`, `Date`, `byte[]`, `UUID`, `Locale`, `TimeZone`, `Enum`). One builder method + one annotation attribute + one applier branch per type. Convention documented once in **Common precedence** above.
6. **All new format settings on `MarshallingContext.Builder`.** Including `binaryFormat(BinaryFormat)`, which moves from `OutputStreamSerializer.Builder` / `InputStreamParser.Builder` to `MarshallingContext.Builder` in Phase 2. The existing setters on `OutputStreamSerializer.Builder` and `InputStreamParser.Builder` are **removed** outright — no shim, no deprecation.
7. **Hard break, no migrations, major release.** Every removed swap class / setter is gone in 9.6 in the same release the replacement lands — no deprecate-then-remove cycles. The swap-elimination matrix has only `REMOVE` and `KEEP` entries; no `DEPRECATE` rows. Wire is forward-compatible (parsers stay format-agnostic) but source-and-binary API surface breaks for anyone referencing the removed classes or setters. Framed positively in the release notes as the whole purpose of a major version bump.
8. **No standalone per-type field annotations.** Do not ship `@CalendarFormat(...)`, `@DateFormat(...)`, `@TemporalFormat(...)`, `@EnumFormat(...)`, etc. as separate annotations on top of `@MarshalledProp`. The typed enum field on `@MarshalledProp` covers every realistic use case; a parallel annotation surface would be dead weight. (See also **Out of scope** below.)
9. **Locale is a first-class INCLUDE.** Promoted from `MAYBE` because the `UNDERSCORE` (Java-legacy) variant is a real round-trip pain point. Smallest sensible set: `BCP_47` (default), `UNDERSCORE`. `LANGUAGE_ONLY` (lossy) and `DISPLAY_NAME` (locale-sensitive, not round-trippable) dropped as silly.
10. **`XMLGregorianCalendar` folded into `CalendarFormat`.** No separate enum or annotation surface. `XMLGregorianCalendar` always emits `toXMLFormat()` regardless of the `CalendarFormat` setting (cleanest architectural choice — no "fall back if not XML_FORMAT" branch in every serializer session). The `XML_FORMAT` constant exists on `CalendarFormat` so callers can opt regular `Calendar` / `GregorianCalendar` fields into the same wire form for symmetry. `XMLGregorianCalendarSwap` stays as a delegator class.
11. **`UUID` is INCLUDE (Phase 3).** Smallest sensible textual set: `STANDARD` (default), `NO_DASHES`, `URN`. `BASE64` / `BASE64_URL` skipped (overlaps confusingly with the byte[] story). `BINARY` is not an enum constant — it's the implicit binary-serializer behavior (BSON `binData` subtype 4, MsgPack / CBOR 16-byte binary), documented in the enum Javadoc alongside the parallel rule for byte[] and Duration. Phased to Phase 3 because no swaps are being removed and it can land independently of the earlier phases.

12. **`TemporalFormat.MILLIS` semantics for non-instant subtypes: midnight UTC.** For temporal subtypes with no inherent zone or time, `MILLIS` coerces to midnight UTC at the start of the relevant period. The full per-subtype rule lives in the Temporal subsection above; in summary:
    - `Instant` / `OffsetDateTime` / `ZonedDateTime` → `toInstant().toEpochMilli()` (well-defined).
    - `LocalDateTime` → that local datetime interpreted as UTC (`ldt.toInstant(ZoneOffset.UTC).toEpochMilli()`).
    - `LocalDate` → midnight UTC at the start of the day (`ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`).
    - `YearMonth` → first day of the month at midnight UTC.
    - `Year` → January 1 at midnight UTC.
    - `LocalTime` / `MonthDay` → fall back to the type's `DEFAULT` (ISO string form) because no defensible epoch-millis interpretation exists without an associated date / year. Documented as an explicit asymmetry in the `MILLIS` Javadoc.

    **Industry convention.** JavaScript (`new Date(millis)`), SQL (`extract(epoch from date)`), and Python (`datetime.timestamp()`) all interpret date-only values as midnight UTC when coerced to epoch millis. Java's standard idiom is `LocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`. Jackson's `NUMBER` shape on `LocalDate` is the outlier — it emits a `[year, month, day]` array, but that semantics doesn't apply cleanly to a `MILLIS`-typed enum constant. Falling back silently to `DEFAULT` for these subtypes would mean a configured `MILLIS` setting silently produces a string for some types and a number for others within the same context, which is more surprising than midnight-UTC coercion. Midnight UTC is also lossless round-trip when the consumer parses back into the same temporal subtype.

---

## Status

**Complete.** All three phases delivered in 9.5.0:

- **Phase 1 (TODO-51) — date/time + locale + timezone wire format controls.** Done. `CalendarFormat`, `DateFormat`, `TemporalFormat`, `TimeZoneFormat`, `LocaleFormat` enums; full annotation + builder surface; precedence resolution in `MarshalledPropertyPostProcessor`; legacy `Temporal*Swap` family removed (hard break).
- **Phase 2 (TODO-52) — binary + enum wire format controls.** Done. `BinaryFormat` extended (added `NOT_SET` + `BASE64_URL`), `EnumFormat` introduced; full annotation + builder surface; precedence resolution; legacy `ByteArraySwap` family + `useEnumNames` removed (hard break); `csv.ByteArrayFormat` renamed to `csv.CsvByteArrayCellFormat`.
- **Phase 3 (TODO-50, this file) — UUID + BigInteger/BigDecimal wire format controls.** Done. `UuidFormat` (`STANDARD` / `NO_DASHES` / `URN`) and `BigNumberFormat` (`NUMBER` / `STRING` / `AUTO`) enums added with the standard annotation + builder + post-processor surface. Lenient parsers; binary serializers continue to emit native types; `AUTO` solves the JS `Number.MAX_SAFE_INTEGER` foot-gun.

The 12 locked decisions in this umbrella plan covered every design choice the per-phase work needed; no decision was reopened during Phase 1, 2, or 3 implementation.

---

## Out of scope

- **REST-server-side OpenAPI schema generation.** The `@Schema(format=...)` story already exists and isn't what this work is about. The new enums may incidentally improve OpenAPI date-time format hints (because `temporalFormat=ISO_DATE` could map to OpenAPI `format: date`), but that wiring is its own follow-up.
- **`org.threeten.*` backport types.** Grep finds zero hits in the codebase. No compat shim.
- **`ChronoUnit` / `TemporalAmount`.** Too broad.
- **Custom `Function<T,String>` escape hatches on the new enums.** Users who need full custom rendering can still write a `StringSwap<T>` per field. Adding a function-typed enum member breaks the round-trip guarantee.
- **Standalone per-type field annotations (`@CalendarFormat`, `@DateFormat`, `@TemporalFormat`, `@EnumFormat`, `@UuidFormat`, …).** Settled by locked decision #8 — the typed field on `@MarshalledProp` is the single annotation surface.
- **`HUMAN_READABLE` constants on any of the new enums.** The locale story is non-trivial (`7 days` vs `7 Tage` vs `7 jours`). Defer.
- **`LANGUAGE_ONLY` / `DISPLAY_NAME` on `LocaleFormat`.** Dropped — see locked decision #9.
- **`BASE64` / `BASE64_URL` constants on `UuidFormat`.** Skipped — see locked decision #11.
- **`DECIMAL_ARRAY` / `BASE32` / `BASE85` on `BinaryFormat`.** Skipped in this pass; `DECIMAL_ARRAY` is a future-add if a real consumer surfaces.
- **`SECONDS` / `NANOS` constants on date-family enums (`CalendarFormat` / `DateFormat` / `TemporalFormat`).** v1 ships only `MILLIS` (locked decision #1). Adding the other epoch units later is a one-line enum-constant change.
- **`Period` extension to year/month numeric formats.** The current `PeriodFormat` enum (`ISO_8601`, `DAYS`) is a v1 surface; extending it (`MONTHS`, `YEARS`, `ISO_8601_WITH_WEEKS`) is a TODO-4-style follow-up, not part of this umbrella plan.
- **Custom-pattern surface on date-family enums (`CalendarFormat.CUSTOM` + `calendarPattern(String)`).** Listed in the per-type sections but the wiring (where the custom pattern lives, how it serializes through context cache, hash-key participation) is a Phase 1 sub-task — surfacing it here would balloon the umbrella plan. Treat the `CUSTOM` constant as a v2 stub if Phase 1 runs long.
- **A separate top-level `ByteArrayFormat` enum.** Eliminated by locked decision #2 + #3 — the byte[] use case rides on `BinaryFormat` and the CSV cell-level enum is renamed to `CsvByteArrayCellFormat`.
- **Deprecation shims for removed classes / setters.** Excluded by locked decision #7.

---

## References

- Reference plan (Duration/Period): `todo/TODO-4-duration-format-control.md`.
- Pattern file pointers (every line below is "look at this to copy the wiring"):
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingContext.java` — search `durationFormat` / `periodFormat`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshallingContextable.java` — search `durationFormat`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java` — the per-type precedence branches and the `durationSwap(...)` / `periodSwap(...)` swap-factory helpers.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/utils/Iso8601Utils.java` — `parseDuration(String, DurationFormat)` and `parsePeriod(String, PeriodFormat)` for the parser-leniency template.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/DurationFormat.java` and `PeriodFormat.java` — enum template (`NOT_SET` sentinel, `format(T)` / `parse(String)`, `isNumeric()`).
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/Marshalled.java` + `MarshalledAnnotation.java` — class-annotation field template.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/MarshalledProp.java` + `MarshalledPropAnnotation.java` — property-annotation field template.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/annotation/MarshalledConfig.java` + `MarshalledConfigAnnotation.java` — config-annotation + applier template.
- Swap inventories surveyed for this plan:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TemporalCalendarSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TemporalDateSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TemporalSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/TimeZoneSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ZoneIdSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/LocaleSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ByteArraySwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/XMLGregorianCalendarSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/ClassSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/UrlSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/StackTraceElementSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/MatchResultSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/InputStreamSwap.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swap/DefaultSwaps.java`
- Pre-existing infrastructure that interacts with this plan:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BinaryFormat.java` — top-level enum extended by Phase 2.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/csv/ByteArrayFormat.java` — CSV-only enum renamed to `CsvByteArrayCellFormat` in Phase 2.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/serializer/OutputStreamSerializer.java` — existing `binaryFormat(...)` setter location (removed in Phase 2; moves to `MarshallingContext.Builder`).
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parser/InputStreamParser.java` — same.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java` — `useEnumNames` consumed at lines 1426 and 1591; where the new `EnumFormat` plumbing replaces it.
- Conventions: `AGENTS.md`, `.cursor/skills/code-conventions/SKILL.md` (Javadoc, SSLLC, `assertBean`, `hashKey()` updates, fluent-setter style).
