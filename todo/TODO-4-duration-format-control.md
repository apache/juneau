# TODO-4: Duration marshalling — serializer-controlled output format with format-agnostic parsing

Source: promoted from `TODO.md` on 2026-05-20. Original bullet: *"Duration.ofDays(7) serialized in hours?"*

## Goal

Give callers explicit control over the wire format used for `java.time.Duration` (the "Duration.ofDays(7) → `PT168H`" mystery the original bullet flagged), via a new **serializer-side setting**. Parsers continue to round-trip **every** format the serializer can produce, regardless of what the parser itself is configured with, so wire payloads from a differently-configured serializer always decode cleanly.

In short:

- Serializers gain a `durationFormat(DurationFormat)` setting on the appropriate builder.
- Parsers stay (and are improved to be) **format-agnostic** for `Duration`: they sniff the wire shape and decode every format the serializer can emit, with the parser-side setting acting only as a tie-breaker hint for genuinely-ambiguous input.

## Why now

- `Duration.ofDays(7)` serializes to `'PT168H'` today because `java.time.Duration.toString()` deliberately omits any day component — Java's `Duration` is a time-based amount and renders only hours / minutes / seconds / nanos. We currently expose that raw string verbatim, and it surprises every user the first time they hit it.
- `Duration.ofHours(-6)` similarly renders as `'PT-6H'` (sign on the unit, not on the period), which is the literal output of `Duration.toString()` but is rarely what callers expect on the wire. The parser already has custom logic to normalize `PT-` to `-PT` before calling `Duration.parse(...)`, so we have implicit acknowledgement that the round-trip needed help.
- Adjacent first-class types (`Instant`, `LocalDate`, `ZonedDateTime`, …) all go through `Iso8601Utils` which centralizes their formatter selection per type; `Duration` is the only one that effectively bypasses formatter selection and goes straight to `value.toString()`. Bringing it under a configurable knob is a small targeted change that closes a real foot-gun.
- The setting is purely additive — default behavior keeps producing today's `PT168H`-style output, so no existing wire payload changes.

## Spec essentials

### What Java's `Duration` itself supports

- **`Duration.toString()`** emits ISO-8601 with time fields only — never a `D` (day) component. Format: `PT<H>H<M>M<S>[.fraction]S`, with a leading `-` for negative whole-period durations, or an inline sign on the largest field for mixed-sign cases (Java actually emits `PT-6H` for `Duration.ofHours(-6)`).
- **`Duration.parse(CharSequence)`** accepts the broader ISO-8601 duration grammar **including** an optional `D` (day) component before `T`: `[-]P[nD]T[nH][nM][n[.n]S]`. So a serializer is free to emit `P7D` (which Java will parse), even though `Duration.toString()` will never produce it.
- Java's `Duration` cannot represent calendar concepts (months / years) — those belong to `java.time.Period`. Any "extended ISO" format we emit for `Duration` is still bounded by total seconds + nanos.

### What Juneau does today

- `Duration` is a first-class `ClassMeta` category (`DURATION` — `ClassMeta.java:123` and `:235`).
- Every serializer session has a dedicated `else if (sType.isDuration())` branch that calls `value.toString()` either directly (e.g. `JsonSerializerSession.java:452-453`, `HoconSerializerSession.java:271-272`, `HjsonSerializerSession.java:247`) or via `Iso8601Utils.format(...)` (CSV / Markdown / Toml / Ini / XML / Cbor / MsgPack / Uon / Bson / Proto / Html — `Iso8601Utils.format` line 93 unconditionally returns `d.toString()` for any `Duration` argument).
- Every parser session routes `Duration` strings through `Iso8601Utils.parse(...)` → `parseDuration(String)` (line 238). That routine:
  1. Trims optional surrounding quotes.
  2. Normalizes a leading `PT-` to `-PT` (so `PT-6H` is decoded as `Duration.ofHours(-6)`).
  3. Tries a hand-rolled regex parser (`parseDurationManual`) that handles `PTnH`, `PTnM`, `PTn[.n]S`, and any combination, with optional leading `-`.
  4. Falls back to `Duration.parse(s)` on failure.
- The existing tests in `juneau-utest/src/test/java/org/apache/juneau/transforms/BuiltInDateTimeSerialization_Test.java` confirm the exact today-behavior:
  - `Duration.ofHours(1).plusMinutes(30)` → `'PT1H30M'`
  - `Duration.ofHours(48)` → `'PT48H'`  ← this is the "ofDays(7)" symptom in miniature
  - `Duration.ofSeconds(45)` → `'PT45S'`
  - `Duration.ZERO` → `'PT0S'`
  - `Duration.ofHours(-6)` → `'PT-6H'`
  - `Duration.ofSeconds(20, 345000000)` → `'PT20.345S'`
  - `Duration.ofHours(26).plusMinutes(3)` → `'PT26H3M'`

So today's behavior is exactly `java.time.Duration.toString()` (with the parser quietly accepting a slightly wider grammar than the serializer ever produces). The TODO bullet's "serialized in hours?" is not a bug — it is Java's `Duration.toString()` doing exactly what it documents — but it is a knob that should be configurable so callers who want `P7D`, `604800000` (millis), `7d`, etc. can opt in.

### Format families the plan needs to settle

| Family | Example for `Duration.ofDays(7)` | Round-trip notes |
| --- | --- | --- |
| `ISO_8601` (today's default) | `PT168H` | Java's `Duration.toString()` exactly. Parseable by `Duration.parse`. |
| `ISO_8601_WITH_DAYS` | `P7D` | Not emitted by `Duration.toString()`, **is** accepted by `Duration.parse`. Choice of "how many hours roll over into a day" is unambiguous (24h). |
| `NANOS` | `604800000000000` | Numeric, lossless. Wire shape is a JSON number, not a string — needs care across formats that quote strings unconditionally. |
| `MILLIS` | `604800000` | Numeric. **Lossy** for sub-millisecond Durations. |
| `SECONDS` | `604800` or `604800.000000000` | Numeric. Lossless when fractional form is used; otherwise lossy at the nanosecond level. |
| `HOCON` | `7d` (or `168h`, `10080m`, `604800s`, `604800000ms`, …) | Compact human-friendly form mirroring HOCON's duration grammar; supports `ns` / `us` / `ms` / `s` / `m` / `h` / `d` suffixes. Picks the largest exact unit when emitting. |
| `HUMAN_READABLE` | `7 days` | Locale-neutral English text. Lossy for fractional / mixed durations unless we cascade ("7 days 1 hour 30 minutes"). |
| custom (pattern or `Function<Duration,String>`) | — | Escape hatch. Probably v2. |

The plan recommends **`ISO_8601`** stay the v1 default (preserves all existing wire payloads) and that the v1 enum surface ship `ISO_8601`, `ISO_8601_WITH_DAYS`, `NANOS`, `MILLIS`, `SECONDS`, and `HOCON`. `HUMAN_READABLE` and the function/pattern escape hatch are deferred to v2 — see **Open Questions** below for confirmation.

## Deliverables

### New types

1. **`org.apache.juneau.swaps.DurationFormat`** — public enum in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/`. Lives next to the existing `TemporalSwap` / `XMLGregorianCalendarSwap` / `ZoneIdSwap` family. Constants (v1):
   - `ISO_8601` — Java's `Duration.toString()` (today's behavior). Default.
   - `ISO_8601_WITH_DAYS` — like `ISO_8601` but rolls 24h chunks into a leading `P<n>D` segment (`P7D`, `P7DT3H`, `P1DT0.5S`, …).
   - `NANOS` — total nanoseconds as a JSON number.
   - `MILLIS` — total milliseconds as a JSON number. Sub-millisecond precision is **truncated**, not rounded — document this loudly.
   - `SECONDS` — total seconds with fractional component (e.g. `604800.000000000`). Always emitted with the nine-digit nanos fraction so round-trip is lossless; trailing zeros are not trimmed.
   - `HOCON` — HOCON-style compact form (`7d`, `5m`, `30s`, `100ms`, `2500us`, `7ns`, `2.5h`). Emit picks the largest unit that produces an exact value (no fractional unit smaller than `ms`); fall back to `ns` if no larger unit divides cleanly.
   - **No** `CUSTOM` / `PATTERN` / `FUNCTION` member in v1.
   - Includes per-constant `format(Duration)` and `parse(String)` static helpers so the enum itself is the round-trip oracle. Centralizes the wire-format logic in one place.

2. **`org.apache.juneau.swaps.DurationParseUtil`** (or fold into `Iso8601Utils.parseDuration`) — the format-agnostic decoder. Given a raw wire string (possibly with quotes stripped), it:
   1. Strips optional surrounding quotes (existing behavior).
   2. If it starts with a digit / minus / decimal point, tries `NANOS` / `MILLIS` / `SECONDS` decoders **with disambiguation rules** (see Design notes — "Numeric ambiguity" below).
   3. If it matches the HOCON shape (digits + suffix letter), decodes via the `HOCON` path.
   4. If it starts with `P` or `-P` (possibly via the `PT-` normalization that already exists), decodes via `ISO_8601` / `ISO_8601_WITH_DAYS` (the same code-path — `Duration.parse` already accepts the wider grammar).
   5. Falls back to today's `Iso8601Utils.parseDurationManual` regex parser for tolerance.
   6. As a last resort, calls `Duration.parse(s)` to surface a clear `ParseException` with the original input.
   The parser-side `durationFormat` setting (if added — see Open Question 4) is consulted only in step 2 to break ties (e.g. is a bare integer `nanos`, `millis`, or `seconds`?).

### Builder plumbing

- **Setting placement (recommended):** `org.apache.juneau.serializer.Serializer.Builder.durationFormat(DurationFormat)` (single-arg fluent setter, defaulting to `DurationFormat.ISO_8601`). Adding it at `Serializer.Builder` keeps the knob output-only and inherits to every existing serializer builder (`JsonSerializer.Builder`, `XmlSerializer.Builder`, `MsgPackSerializer.Builder`, `CborSerializer.Builder`, `CsvSerializer.Builder`, `HoconSerializer.Builder`, `HjsonSerializer.Builder`, `Json5Serializer.Builder`, `MarkdownSerializer.Builder`, `YamlSerializer.Builder`, `TomlSerializer.Builder`, `IniSerializer.Builder`, `UonSerializer.Builder`, `UrlEncodingSerializer.Builder`, `OpenApiSerializer.Builder`, `HtmlSerializer.Builder`, `HtmlDocSerializer.Builder`, `HtmlSchemaSerializer.Builder`, `HtmlStrippedDocSerializer.Builder`, `JsonSchemaSerializer.Builder`, `PlainTextSerializer.Builder`, `SoapXmlSerializer.Builder`, `BsonSerializer.Builder`, `ProtoSerializer.Builder`, `ParquetSerializer.Builder`).
  - Backing field on `Serializer.Builder`: `private DurationFormat durationFormat = DurationFormat.ISO_8601;`
  - Include in `Serializer.Builder.hashKey()` so differently-configured serializer instances do not collide in the shared `Cache<HashKey, Serializer>` (required per the **`code-conventions` skill** — "Adding Settings to Serializers/Parsers" → "Update hashKey() Method").
  - Each concrete `Serializer.Builder` subclass gets the boilerplate `@Override public Builder durationFormat(DurationFormat v) { super.durationFormat(v); return this; }` for fluent-chain return-type narrowing (matches the pattern used by every other inherited setting — see `JsonSerializer.Builder.useEnumNames()` for the canonical example).
- **Parser-side (recommended, conditional on Open Question 4):** mirror setter `Parser.Builder.durationFormat(DurationFormat)` used **only as a hint for ambiguous numeric input**. The parser-side default is `null` (= "best-effort sniff with no hint"); if set, ambiguous integer values are interpreted in that unit. Same fluent-setter narrowing across every parser builder.
- **`MarshallingContext.Builder` (not preferred):** placing the setting at the `MarshallingContext` (bean-context) layer would push the knob into the bean dictionary cache and affect every consumer of `BeanContext`, including ones that have nothing to do with serialization (e.g. `BeanMap` introspection). The setting is output-format-only, so `Serializer.Builder` is the right boundary.

### Wiring inside the serializer sessions

- `SerializerSession` gains a `protected DurationFormat getDurationFormat()` accessor that reads from the `Serializer` context.
- `Iso8601Utils.format(Object, ClassMeta<?>, TimeZone)` grows an overload (or a parallel `formatDuration(Duration, DurationFormat)` helper) so each session can pass its configured format. Existing callers that don't care keep their no-arg behavior (which defaults to `ISO_8601`).
- Every site that currently calls `o.toString()` or `Iso8601Utils.format(o, sType, getTimeZone())` for a `Duration` is updated to route through the new helper — concretely:
  - `JsonSerializerSession.java:452-453`
  - `HoconSerializerSession.java:271-272`
  - `HjsonSerializerSession.java:247`
  - `MsgPackSerializerSession.java:270` (note: `MILLIS` / `NANOS` should encode as MsgPack integers, not strings — see Design notes)
  - `CborSerializerSession.java:263`
  - `BsonSerializerSession.java:167`
  - `XmlSerializerSession.java:1103`
  - `HtmlSerializerSession.java:1073`
  - `CsvSerializerSession.java:311` (and the `Iso8601Utils.format` call site on `:312`)
  - `MarkdownSerializerSession.java:280, :549`
  - `TomlSerializerSession.java:282`
  - `IniSerializerSession.java:271`
  - `UonSerializerSession.java:515`
  - `ProtoSerializerSession.java:437`
  - `ParquetSchemaBuilder.java:310` (schema-side — confirm it doesn't need to participate)
  - `SerializerSession.toString(Object)` line 940 (top-level path for non-typed Duration emit).

### Wiring inside the parser sessions

- The parser path is much smaller because everyone routes through `Iso8601Utils.parse(...)` → `parseDuration(String)` (`ParserSession.java:953-954` + every per-format parser session site). The plan upgrades `parseDuration` to be **format-agnostic**:
  - Move the new sniff-and-decode logic into `DurationParseUtil.parse(String, DurationFormat hint)` (where `hint == null` means "no parser-side preference").
  - `Iso8601Utils.parseDuration` delegates to it for backwards-compatibility.
  - Parser session sites pass `this.getDurationFormat()` (the hint) if Open Question 4 is settled in favor of "hint, not strict mode"; otherwise `null`.

### Numeric vs string wire shape

- `ISO_8601`, `ISO_8601_WITH_DAYS`, and `HOCON` always emit a quoted string in text formats (JSON, JSON5, XML attribute, CSV cell, …) and a UTF-8 string in binary formats (MsgPack `str`, CBOR text-string, BSON UTF-8, …).
- `NANOS`, `MILLIS`, `SECONDS` emit a JSON / JSON5 number, an XML attribute that looks like a number but is still text on the wire, a MsgPack signed integer (or float for fractional `SECONDS`), a CBOR integer (major type 0/1, or major type 7 float for fractional `SECONDS`), a BSON `int64` (or `double` for fractional `SECONDS`). In CSV / TOML / INI / TXT / HTML they're written as bare numeric text.
- Parser-side sniff must accept **either** wire shape regardless of how the parser is configured, because the round-trip story across heterogeneous services depends on it.

### Annotation surface

- **Recommended:** `@Marshalled(durationFormat = DurationFormat.HOCON)` at the **type level** is moot for `Duration` (we can't `@Marshalled` annotate `java.time.Duration` itself), so the realistic annotation surface is **per-property** via `@BeanProp` (a new attribute on `BeanProp` would need adding) or via **`@Swap(DurationFormatSwap.class)`** using a pre-built swap subclass per format (similar pattern to `TemporalSwap.IsoDate` / `BasicIsoDate` / …).
- **v1 surface (recommended):** ship the global serializer setting and the `DurationFormat` enum **only**. Per-field annotation override deferred to v2 unless a concrete caller demand surfaces. (See Open Question 6.)

### Tests

Location: `juneau-utest/src/test/java/org/apache/juneau/transforms/` (alongside the existing `BuiltInDateTimeSerialization_Test`, `DefaultSwaps_Test`).

- **`DurationFormat_Test.java`** — unit tests on the enum's `format(Duration)` / `parse(String)` static helpers:
  - For each format constant, assert the round-trip on the canonical test vectors: `Duration.ZERO`, `Duration.ofNanos(1)`, `Duration.ofMillis(1)`, `Duration.ofSeconds(45)`, `Duration.ofSeconds(20, 345000000)`, `Duration.ofMinutes(30)`, `Duration.ofHours(1).plusMinutes(30)`, `Duration.ofHours(26).plusMinutes(3)`, `Duration.ofHours(48)`, `Duration.ofDays(7)`, `Duration.ofDays(365)`, `Duration.ofHours(-6)`, and a single fractional / mixed-sign value.
  - For each format, assert the **wire string** matches the documented shape (`ISO_8601` → `PT168H`, `ISO_8601_WITH_DAYS` → `P7D`, `NANOS` → `604800000000000`, `MILLIS` → `604800000`, `SECONDS` → `604800.000000000`, `HOCON` → `7d`).
- **`DurationSerialization_Test.java`** — per-serializer format coverage, modeled on `BuiltInDateTimeSerialization_Test.java`. For each serializer (JSON, JSON5, XML, CSV, HOCON, …):
  - Set `durationFormat(...)` to each constant.
  - Serialize `Duration.ofHours(1).plusMinutes(30)` and `Duration.ofDays(7)` and assert the wire string.
  - Round-trip back through the default parser (no parser-side setting) and assert the `Duration` matches.
- **`DurationCrossFormat_Test.java`** — round-trip across serializer config A and parser config B. For every (A, B) pair drawn from the enum constants, serialize on A and parse on B; assert the resulting `Duration` equals the input. This is the "wire payload from a foreign serializer" guarantee from the TODO bullet, and is the regression test for "parser must round-trip every format the serializer can produce".
- **`DurationEdgeCases_Test.java`** — `null`, `Duration.ZERO`, negative durations, fractional seconds, maximum / minimum supported magnitudes, surrounding-quotes tolerance, and (for HOCON) malformed unit-suffix recovery.
- Conventions: follow the `code-conventions` skill — `TestBase`, SSLLC, `assertBean(...)` for bean-property assertions, nested `A_basicTests` / `B_serialization` / `C_extraProperties` groupings.

### Release notes

Append a `9.5.0.md` entry under `### juneau-marshall` describing:

- The new `DurationFormat` enum and `Serializer.Builder.durationFormat(...)` setting.
- The default (`ISO_8601`) — wire-compatible with 9.4.x and earlier.
- The format-agnostic parser behavior (no opt-in needed for callers who only care about parsing).
- Optional `Parser.Builder.durationFormat(...)` hint and what it does (tie-break for ambiguous numeric input).

### Docs

- Update **`juneau-docs/pages/topics/02.11.02.DefaultSwaps.md`** — the existing 9.5.0 callout already mentions `Duration` as natively handled; add a follow-up `:::note` referencing the new `DurationFormat` setting for callers who want a non-default wire shape.
- No new dedicated `JuneauDurationFormat.md` topic page is needed for v1 — the enum's Javadoc plus the release-notes entry is enough surface area. Revisit if the v2 work expands the format surface.

## Design notes

### Why a serializer setting and **not** a swap-only solution

Juneau already exposes `TemporalSwap` and its sibling variants (`IsoDate`, `BasicIsoDate`, `IsoLocalDateTime`, …) so the precedent for "different wire format per type" is to register a swap. We could ship `DurationSwap.IsoFormat` / `DurationSwap.HoconFormat` / `DurationSwap.NanosFormat` as subclasses and have callers register them via `MarshallingContext.Builder.swaps(...)` — that would work without any new serializer-builder plumbing.

We prefer the **builder setting** for three reasons:

1. The user explicitly asked for "a serializer setting" — matches their stated mental model.
2. The setting is purely a wire-format knob (no bean-mapping or type-translation behavior), so registering it as a swap pulls in unnecessary `ObjectSwap` machinery just to flip an enum branch in one method.
3. Discoverability: settings show up alongside `useEnumNames`, `sortMaps`, `addBeanTypes`, etc., in IDE autocomplete on the builder. A swap class buried in `org.apache.juneau.swaps` is invisible until you know to look for it.

The swap path stays available as an **escape hatch** for callers who genuinely want per-field control — they can still register their own `StringSwap<Duration>` via `@Swap`.

### Where the enum lives

`org.apache.juneau.swaps.DurationFormat` keeps it next to the other type-formatting concerns (`TemporalSwap`, `ZoneIdSwap`, `LocaleSwap`, …). The alternative — a new `org.apache.juneau.transforms` package — does not currently exist in the codebase (Glob found zero hits). Adding a new package for a single enum has no benefit; the existing `swaps` package is the right home.

### Numeric ambiguity

A bare integer on the wire (`604800000`) could mean any of `NANOS` (0.0006s), `MILLIS` (7d), or `SECONDS` (19,178 years). The format-agnostic parser **cannot** disambiguate on shape alone. Resolution order:

1. If the parser-side `durationFormat` hint is set, use it.
2. Otherwise, fall back to **`MILLIS`** (the most common informal wire convention).
3. Document this loudly so callers who care set the hint.

Fractional numerics (`604800.000000000`) are unambiguously `SECONDS`. Numerics with a unit suffix (`7d`, `100ms`) are unambiguously `HOCON`. Strings starting with `P` or `-P` are unambiguously ISO-8601. So the ambiguity zone is **only** bare positive integers.

### Why parsers are format-agnostic by default

The TODO bullet's user-intent statement is explicit: *"Parsers would need to be able to handle all formats produced by the serializer."* A parser that errored on a format it wasn't configured for would force every service in a heterogeneous deployment to coordinate `durationFormat` settings end-to-end — defeating the whole point of allowing per-serializer choice. Format-agnostic decode is the only sensible default; the parser-side `durationFormat` setting (if accepted — Open Question 4) exists solely to tie-break the bare-integer ambiguity above.

### Binary-format encoding (MsgPack / CBOR / BSON / Proto)

For binary formats, the serializer respects the configured format **but** routes through the format's native type when sensible:

- `NANOS` / `MILLIS` → integer wire type (MsgPack `int`, CBOR major type 0/1, BSON `int64`).
- `SECONDS` (fractional) → float wire type (MsgPack `float64`, CBOR major type 7 / float, BSON `double`).
- `ISO_8601` / `ISO_8601_WITH_DAYS` / `HOCON` → string wire type.

This keeps the binary wire compact for the numeric formats (today they're emitted as UTF-8 strings of `Duration.toString()`, which wastes space). Confirm with the user whether MsgPack / CBOR / BSON behavior is in scope for v1 or should be deferred — see Open Question 9.

### Why `ISO_8601` stays the default

- **Wire compatibility:** the cross-version wire payload for `Duration` doesn't change unless the caller asks. Existing services keep talking to existing services.
- **Already documented:** the `DefaultSwaps.md` topic and the existing 9.5.0 release notes already commit to ISO-8601 as the native format. Switching the default would require a coordinated migration step and would surprise downstream consumers.
- **Round-trip safe:** Java's `Duration.toString()` + `Duration.parse(...)` is fully lossless for any `Duration` value.

The "Duration.ofDays(7) → `PT168H` is weird" complaint is settled by **making the setting available**, not by changing the default. Callers who want `P7D` flip a single knob; callers who liked the old behavior never have to do anything.

### `Iso8601Utils.parseDuration` already does extra-careful normalization

The existing manual parser at `Iso8601Utils.parseDurationManual` (line 259) — handling `PT-6H`, quoted strings, mixed-case `pt` prefix, decimal seconds — should be **kept as the inner ISO-decoder**. The new `DurationParseUtil.parse(String, DurationFormat)` wraps that plus the new numeric / HOCON sniff paths. No regression risk to the today-behavior; the new logic is additive.

### What happens to the `Iso8601Utils.format` overload that ignores `ClassMeta`

The `format(Object, ClassMeta<?>, TimeZone)` signature already accepts a `ClassMeta` parameter that is currently `@SuppressWarnings("java:S1172")`-suppressed because no formatter selection uses it for `Duration`. The new `DurationFormat` plumbing flows through the **session**, not through `ClassMeta`, so this suppression stays. Don't change the signature solely to thread the format through — pass it via a new `formatDuration(Duration, DurationFormat)` overload instead.

## Out of scope

- **`java.time.Period` coverage.** Period is a date-based amount (years / months / days) and Juneau has no `isPeriod()` ClassMeta branch today — bytes-on-the-wire for `Period` currently come out as whatever `value.toString()` happens to produce (`P7D`, `P2M`, `P1Y6M`, …). A `PeriodFormat` enum mirroring this work is a natural follow-up TODO but **not** part of this v1. See Open Question 5 in case the user wants it in scope.
- **`long durationMs` / `int seconds` plain primitive fields.** Users who model durations as `long` on their beans get a JSON number regardless of any setting; this plan does not touch the primitive paths.
- **`org.threeten.*` backport types.** Not present anywhere in the Juneau codebase today (Grep found zero hits). No compatibility shim needed.
- **`ChronoUnit` / `TemporalAmount` / `TemporalUnit` general support.** Too broad. Locked to `java.time.Duration` for v1.
- **Per-property `@DurationFormat(...)` or `@BeanProp(durationFormat=...)` annotation.** Deferred — see Open Question 6.
- **`Locale`-sensitive `HUMAN_READABLE` rendering** ("7 days", "7 Tage", "7 jours"). Deferred — Juneau already passes session `Locale` around but the i18n surface for this would be its own conversation.
- **Custom-pattern / `Function<Duration,String>` escape hatch on the `DurationFormat` enum.** Deferred — users who need it can write a `StringSwap<Duration>` today.
- **REST-server-side OpenAPI `duration` schema-format mapping.** RFC 3339 and OpenAPI both gesture at a `duration` format but the mapping is not what this TODO is about; covered separately by whatever the OpenAPI / `@Schema` work does.

## Verification

- `./scripts/test.py --full` — full build + unit tests pass.
- `mvn -pl juneau-core/juneau-marshall -am install` succeeds.
- `mvn -pl juneau-utest test -Dtest='org.apache.juneau.transforms.Duration*Test'` passes the new test classes.
- `./scripts/coverage.py juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/DurationFormat.java --run` reports ≥ 90% on the new enum and helper.
- Existing tests that hard-code the today-behavior continue to pass unchanged:
  - `BuiltInDateTimeSerialization_Test#l01..l07` (the explicit `PT...` wire-shape assertions).
  - `BuiltInDateTimeSerialization_Test#m01`, `n01`, `o01` (the bean-with-Duration round-trips).
  - `DefaultSwaps_Test#j01..j03` (the `Duration` swap-override smoke tests).
  - `RoundTripDateTime_Test#a03` (cross-format round-trip).
  - `YamlParser_Test#a14_parseBeanWithDuration` (cross-format round-trip for YAML in particular).
- No new wire-payload changes when the setting is not explicitly opted into (`ISO_8601` default). This is the headline backwards-compatibility commitment.

## Open Questions

These need the user's call before implementation starts. Numbered for direct response.

1. **Setting placement.** Confirm we should add `durationFormat(DurationFormat)` to **`Serializer.Builder`** (preferred — matches `useEnumNames` / `quoteCharOverride` style, inherited by every concrete serializer builder) rather than:
   - `MarshallingContext.Builder` (would pull a wire-format knob into the bean-context cache layer — not preferred), or
   - a pre-built `DurationSwap.*` swap family that callers register via `swaps(...)` (functional today, but not how the user phrased the ask).

2. **Default format.** Keep today's behavior (`ISO_8601` — wire-compatible with 9.4.x, produces `'PT168H'` for `Duration.ofDays(7)`) as the v1 default? Or switch the default to something more humane (candidates: `ISO_8601_WITH_DAYS` → `'P7D'`, or `HOCON` → `'7d'`)? Changing the default is a wire-incompatible behavior change for every existing caller and probably wants its own migration callout in the release notes; my recommendation is to keep `ISO_8601` and let opt-in handle the rest.

3. **Format enum surface for v1.** The recommended list is `ISO_8601`, `ISO_8601_WITH_DAYS`, `NANOS`, `MILLIS`, `SECONDS`, `HOCON`. Should we also include:
   - `HUMAN_READABLE` ("7 days") in v1? (Locale story is non-trivial; recommend defer.)
   - A custom `String` pattern? (Mirrors `DateTimeFormatter` patterns; recommend defer to v2 escape-hatch story.)
   - A `Function<Duration, String>` callback? (Powerful but does not round-trip; recommend defer.)

4. **Parser flexibility.** Confirm parsers should be **format-agnostic by default** — they sniff the wire shape and accept every format the serializer can produce. Should we **also** offer an optional `Parser.Builder.durationFormat(DurationFormat)` hint, used only as a tie-breaker for bare-integer numeric input (where the shape is ambiguous between `NANOS` / `MILLIS` / `SECONDS`)? My recommendation: yes — format-agnostic by default, hint-only when set; default the hint to `null` and fall back to `MILLIS` for ambiguous input.

5. **`java.time.Period` coverage.** Three options:
   - **(a)** Same enum, same setting — extend `DurationFormat` to also drive `Period` output and add an `isPeriod()` ClassMeta branch. Awkward because `Period`'s natural format surface is different (`P1Y6M3D`, no time component, no `NANOS`/`MILLIS`).
   - **(b)** Separate `PeriodFormat` enum + `Serializer.Builder.periodFormat(...)` setting in v1.
   - **(c)** **Out of scope for v1** — file as a sibling TODO ("Add native `java.time.Period` support and a `PeriodFormat` setting") and revisit once we have a concrete caller. My recommendation.

6. **Annotation-level control.** Should `@Marshalled` (or a new `@DurationFormat`) annotation let an individual bean field override the global setting? E.g. `@DurationFormat(HOCON) public Duration retryAfter;`. My recommendation: defer to v2 — the global setting plus the existing `@Swap(...)` escape hatch covers every realistic use case for v1.

7. **`null` and zero handling.** Today the behavior is "null fields are emitted unless `keepNullProperties(true)`; `Duration.ZERO` serializes as `'PT0S'`". Should the plan preserve that exactly across every new format (`NANOS` → `0`, `MILLIS` → `0`, `SECONDS` → `0.000000000`, `HOCON` → `0s`, `ISO_8601_WITH_DAYS` → `'PT0S'`), or should we introduce something like an `omitZero` companion knob? My recommendation: preserve today's behavior verbatim — `Duration.ZERO` is a real value and should not be silently omitted.

8. **Negative durations.** Today `Duration.ofHours(-6)` round-trips as `'PT-6H'` (the parser already normalizes `PT-` → `-PT`). Confirm policy for each new format:
   - `ISO_8601_WITH_DAYS` → recommend `-P6H` (leading sign, no inline sign).
   - `NANOS` / `MILLIS` / `SECONDS` → just emit the negative number (`-21600`, `-21600000`, `-21600.000000000`).
   - `HOCON` → recommend `-6h` (leading sign, no per-unit signs).
   My recommendation is to settle this with the above defaults and have parsers accept both shapes.

9. **Binary-format wire shape for numeric formats.** Confirm that in MsgPack / CBOR / BSON / Proto, `NANOS` / `MILLIS` should emit as **native integers** (MsgPack `int`, CBOR major-type 0/1, BSON `int64`) and `SECONDS` as a **native float** (MsgPack `float64`, CBOR major-type 7, BSON `double`) — rather than UTF-8 strings of the decimal representation. My recommendation: yes, route through the native numeric type. This is a small wire-shape change for `Duration` round-tripped through a configured non-ISO format, but only opt-in.

## References

- Original `TODO.md` bullet (removed by this expansion): *"Duration.ofDays(7) serialized in hours?"*
- Current serialization path:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/utils/Iso8601Utils.java` — `format(...)` line 92, `parseDuration(...)` line 238, `parseDurationManual(...)` line 259.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassMeta.java` — `DURATION` category (line 123, 235), `isDuration()` (line 1259).
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/serializer/SerializerSession.java` — top-level `toString(Object)` (line 940).
  - Per-format `isDuration()` branches: `JsonSerializerSession:452`, `HoconSerializerSession:271`, `HjsonSerializerSession:247`, `MsgPackSerializerSession:270`, `CborSerializerSession:263`, `BsonSerializerSession:167`, `XmlSerializerSession:1103`, `HtmlSerializerSession:1073`, `CsvSerializerSession:311`, `MarkdownSerializerSession:280, :549`, `TomlSerializerSession:282`, `IniSerializerSession:271`, `UonSerializerSession:515`, `ProtoSerializerSession:437`.
- Today's wire-shape regression tests:
  - `juneau-utest/src/test/java/org/apache/juneau/transforms/BuiltInDateTimeSerialization_Test.java` (`l01..l07`, `m01`, `n01`, `o01`).
  - `juneau-utest/src/test/java/org/apache/juneau/transforms/DefaultSwaps_Test.java` (`j01..j03`).
  - `juneau-utest/src/test/java/org/apache/juneau/a/rttests/RoundTripDateTime_Test.java` (`a03`).
  - `juneau-utest/src/test/java/org/apache/juneau/yaml/YamlParser_Test.java` (`a14`).
- Settings-pattern prior art:
  - `useEnumNames` end-to-end: `MarshallingContext.Builder.useEnumNames()` (line 3593), every serializer/parser `Builder.useEnumNames()` override, `hashKey()` inclusion (line 2299).
  - `quoteCharOverride` end-to-end (`WriterSerializer`-only setting): `WriterSerializer.Builder.quoteCharOverride(char)` (line 695), per-builder narrowing overrides.
- Sibling format-control swaps for reference (the pattern we are deliberately not using for the global knob, but keeping available for per-field overrides): `org.apache.juneau.swaps.TemporalSwap` and its inner classes (`BasicIsoDate`, `IsoDate`, `IsoLocalDateTime`, …).
- Default-swap docs that already advertise native Duration support: `juneau-docs/pages/topics/02.11.02.DefaultSwaps.md`.
- Release notes file to update: `juneau-docs/pages/release-notes/9.5.0.md`.
- Conventions: `AGENTS.md`, `.cursor/skills/code-conventions/SKILL.md` (Javadoc tags, SSLLC, `assertBean`, fluent-setter style, **"Adding Settings to Serializers/Parsers" — `hashKey()` update is required**).
- Java standard-library references:
  - [`Duration.toString()`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#toString()) — explicitly documents the "no day component" behavior.
  - [`Duration.parse(CharSequence)`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) — does accept the `PnDTnH…` form.
  - [HOCON duration format](https://github.com/lightbend/config/blob/main/HOCON.md#duration-format) — `ns`, `us`, `ms`, `s`, `m`, `h`, `d` suffix vocabulary.
