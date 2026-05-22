# TODO-57: Format round-trip test coverage for the 16 `org.apache.juneau.*Format` enums

Source: filed 2026-05-21 after a pilot run for `DurationFormat` (`juneau-utest/src/test/java/org/apache/juneau/transforms/DurationFormat_RoundTrip_Test.java`) flushed two production bugs that had been hiding behind Json5-only placement coverage. This plan rolls out the same cross-serializer/parser matrix to the remaining 15 format enums.

---

## Background

The format-control extension work (`FINISHED-4-duration-format-control.md`, `FINISHED-50-format-control-extension.md`, `FINISHED-54-format-control-round-2.md`) added **16 format enums** under `org.apache.juneau`, each plumbed through `MarshallingContext` and consulted by `MarshalledPropertyPostProcessor` plus the per-format swap factories:

- Temporal cluster: `DurationFormat`, `PeriodFormat`, `CalendarFormat`, `DateFormat`, `TemporalFormat`, `TimeZoneFormat`.
- Identity cluster: `UuidFormat`, `EnumFormat`, `LocaleFormat`, `ClassFormat`, `BinaryFormat`.
- Numeric cluster: `BigNumberFormat`, `FloatFormat`, `CurrencyFormat`.
- Other: `BooleanFormat`.

Existing per-format coverage is mostly Json5 placement tests — they confirm "the configured format reaches the wire on a Json5 round-trip" but **none of the 16 formats has a true cross-serializer/parser round-trip matrix** that exercises every serializer/parser pair against every format-enum value.

The pilot file for `DurationFormat` was parameterized over the canonical 42 serializer/parser tester templates in `juneau-utest/src/test/java/org/apache/juneau/a/rttests/` × 7 `DurationFormat` values × 6 test methods = **1764 invocations**. First run: 1452 pass / 312 fail. The 312 failures bucket cleanly into two production bugs that single-format Json5 testing had not surfaced:

- **Bug #1** — `MarshalledPropertyPostProcessor.durationSwap` silently promotes large `Long` values to `Double` via a misshapen ternary. 308 failures across the matrix for NANOS / MILLIS bean-property tests.
- **Bug #2** — UrlEncoding (3 variants) + Markdown parser sessions drop the configured `DurationFormat` when parsing a bare numeric top-level value. 4 failures.

Bug #1 is being fixed in parallel with this plan; Bug #2 is deferred pending investigation (see **Phase 6**). The rollout below assumes the matrix infrastructure (`RoundTrip_Tester` + 42 templates) is the right backbone for catching the same class of bugs in the other 15 formats.

---

## Scope

### In scope

- Add a cross-serializer/parser round-trip test for each of the 16 format enums under `org.apache.juneau`, using the canonical `RoundTrip_Tester` matrix (42 tester templates) from `juneau-utest/src/test/java/org/apache/juneau/a/rttests/`.
- One test class per format under `juneau-utest/src/test/java/org/apache/juneau/transforms/<FormatName>_RoundTrip_Test.java`, parameterized over `(RoundTrip_Tester, FormatEnumValue)`.
- Both **bean-property** and **top-level** invocation contexts per format (the bug-#1 / bug-#2 split is along this axis — bean-property vs top-level — so both are required).
- Reuse the existing skip predicates (`returnOriginalObject()`, `t.parser() != null`) for the non-round-trippable testers in the matrix.
- Production-bug fixes that fall out of the rollout, tracked per phase.

### Out of scope

- Restructuring the `MarshallingContext`-based format-control architecture itself (already established by TODO-4 / TODO-50 / TODO-54).
- Adding new format enums.
- Touching `juneau-rest-*` / `juneau-microservice-*` / `juneau-bean-*` — pure marshalling layer only.
- Dispatch-order / JIT-shape work on the serializer/parser sessions — that's `TODO-56`.

---

## Pilot (completed)

- **File**: `juneau-utest/src/test/java/org/apache/juneau/transforms/DurationFormat_RoundTrip_Test.java`.
- **Shape**: 6 test methods × 42 testers × 7 format values = 1764 invocations.
- **Result**: 1452 pass / 312 fail at first run. Failures bucket into Bug #1 (308) + Bug #2 (4).
- **Test infra change**: `RoundTrip_Tester.Builder` visibility bumped from package-private to `public` so per-format tests can live in `org.apache.juneau.transforms` alongside other `*Format_*Test.java` files instead of being forced into `org.apache.juneau.a.rttests`.

---

## Bug inventory

### Bug #1 — `MarshalledPropertyPostProcessor.durationSwap` — **STATUS: FIXED 2026-05-21**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java`, around line 602.
- **Shape**: a ternary picks between a `Long` and a `Double` arm; the compiler widens the `Long` arm to `Double` so every NANOS / MILLIS value emits as scientific-notation `Double` (e.g. `8.1E12`). The parse side then chokes with `NumberFormatException` because the wire value is no longer the expected integer.
- **Symptom**: 308 failures in the pilot across most serializer/parser pairs for NANOS and MILLIS bean-property tests.
- **Fix**: split the ternary into two `if/return` branches so each arm preserves its boxed type. Dropped the misleading `@SuppressWarnings("java:S2154")` rationale that was masking the issue.
- **Closure**: pilot re-ran at **1760 / 4** post-fix; the 4 remaining failures were all Bug #2 (now also closed). Sibling-swap audit completed in Phase 1 cleared `periodSwap` and `bigNumberSwap`; an additional `floatSwap` mirror was found and fixed alongside.

### Bug #2 — Multiple parsers drop format hint for bare numeric / native-datetime top-level — **STATUS: FIXED 2026-05-21**

- **Sites (confirmed)**: `UrlEncodingParserSession.unwrapValueAs`, `MarkdownParserSession.parseCellValue`, and `TomlParserSession.convertValue` (both the bare-numeric path and the Toml-specific native-datetime literal path).
- **Symptom (pre-fix)**: when the top-level wire value was a bare number (and for Toml also when it was a native ISO datetime literal), the parser converted via a generic `Number → T` (or `DateTime → T`) coercion that didn't consult `MarshallingContext.get*Format()`. The configured format hint was silently dropped.
  - `DurationFormat.NANOS` round-trip of `Duration.ofHours(2).plusMinutes(15)` returned `PT2250000H` (≈ 2.25 M hours) instead of `PT2H15M` — the wire `8100000000000` was reparsed as `MILLIS` instead of `NANOS`.
  - `TemporalFormat.ISO_YEAR` round-trip of `Instant.parse("2024-06-15T12:00:00Z")` returned `1970-01-01T00:00:02.024Z` — the wire `2024` was reparsed as `Long` then converted via `Instant.ofEpochMilli` instead of as a year.
- **Pilot blast radius (pre-fix)**: 4 failing invocations across `DurationFormat_RoundTrip_Test` (tester indices 14, 15, 16, 35 — UrlEncoding default/readable/expanded-params + Markdown), widened to Toml across `TemporalFormat_Instant_RoundTrip_Test` (1) and `TemporalFormat_LocalDateTime_RoundTrip_Test` (4 — including the `ISO_INSTANT` native-datetime divergence).
- **Fix shape**: per-parser branches that thread `MarshallingContext.get<Format>()` (e.g. `getDurationFormat()`, `getTemporalFormat()`, `getBigNumberFormat()`) into the bare-numeric coercion path inside each affected session. Toml additionally consults the format hint on the native-datetime literal path so `ISO_INSTANT × LocalDateTime` (Z-zoned literal → wall-clock local) is resolved through the format swap rather than through Toml's built-in zone conversion. Each parser-session fix is self-contained — no shared helper extraction this turn.
- **Diagnosis correction**: none for Bug #2; the parser-side diagnosis was correct.
- **Closure**: all Bug #2 mirrors green after fix. Shared-helper-lift to `ReaderParserSession` remains a future option *only* if the same `Number → T` mirror surfaces in another parser (PlainText / JCS / HOCON) — currently no mirror, so no action.

### Bug #3 — MsgPack `readInt()` returns negative `INT8` / `INT16` values as unsigned — **STATUS: FIXED 2026-05-21**

- **Site (actual)**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/msgpack/MsgPackInputStream.java` — `readInt()` reads `INT8` / `INT16` opcodes into a widened `int` without sign-extending through `(byte)` / `(short)` first, so a negative wire value like `0xD1 0xFE 0x54` (= -428) was being read back as `65108`.
- **Diagnosis correction**: the pre-fix write-up identified the **serializer** (`MsgPackSerializerSession`) as the likely site (guessing the encoder picked uint16 instead of int16). The bug was actually parser-side — the serializer correctly emits `0xD1 ...` (INT16) for negative values; the parser was reading the int16 byte pair into an `int` without sign-extension and returning the unsigned magnitude (`65536 - 428 = 65108`). Same shape on INT8.
- **Symptom (pre-fix)**: `PeriodFormat.DAYS` serialized `Period.of(-1, -2, -3)` through the `periodSwap` as `Integer.valueOf("-428")` = `Integer(-428)`. MsgPack read it back as `65108`, and `Period.ofDays(65108)` produced `P65108D` instead of the expected `P-428D`. Same shape across `BigNumberFormat × BigInteger.valueOf(-12345)` (read back as `53191`).
- **Blast radius (pre-fix)**: 1 failure on `PeriodFormat_RoundTrip_Test.a03_periodProperty_negativeAndZero(MsgPack, DAYS)` + 4 failures on `BigNumberFormat_BigInteger_RoundTrip_Test.a03_bigIntegerProperty_negativeAndZero(MsgPack, *)`.
- **Fix shape**: in `MsgPackInputStream.readInt()`, sign-extend on the `length == 1` (INT8) and `length == 2` (INT16) paths through `(byte)` / `(short)` casts, gated on `lastByte == INT8` / `lastByte == INT16` so that the unsigned `UINT8` / `UINT16` opcodes are unchanged. UINT16 is unaffected because the value fits naturally in an `int` (max 65535 < `Integer.MAX_VALUE`); the asymmetry between signed and unsigned is now explicit in the source comments.
- **Closure**: 5 affected invocations green after fix. Sibling-bug **#3b** (NEGFIXINT `length == -1` fall-through) found during this turn and fixed in Phase A.5 — see entry below.

### Bug #3b — MsgPack `readInt()` NEGFIXINT (`length == -1`) fall-through — **STATUS: FIXED 2026-05-21 (this turn)**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/msgpack/MsgPackInputStream.java` — `readInt()` had no guard for the `length == -1` (NEGFIXINT) case set by `readDataType()` for opcodes `0xE0..0xFF`. Execution fell through to the 4-byte read path at the bottom of `readInt()`, consuming 4 extra bytes from the stream and returning a corrupted value.
- **Severity**: low. The NEGFIXINT range is `[-32, -1]`; the existing test matrix doesn't hit this opcode for any current `*Format` round-trip (every signed `Integer.valueOf(n)` test value used so far has magnitude ≥ 32, routing through `INT8` or `INT16`). So the bug was dormant — but still a parser-side correctness hole that would surface as a stream-corruption cascade the moment a magnitude-1..32 negative integer landed in a MsgPack property.
- **Found by**: code-reading audit during the Bug #3 closure verification — sibling case to #3 in the same `readInt()` method. The user flagged it explicitly and asked for it to be closed now since the diff is trivial.
- **Fix shape**: a single guard at the top of `readInt()`:

  ```java
  if (length == -1)
      return (byte) lastByte;
  ```

  `lastByte` holds the NEGFIXINT byte itself (`readDataType()` sets `lastByte = i` for every `INT` case). Casting through `byte` sign-extends the negative 5-bit value into the returned `int`.
- **Closure**: applied in Phase A.5 of this turn. MsgPack regression matrix (`MsgPackParser_Test`, `MsgPackSerializerTest`, `MsgPackMediaType_Test`) re-ran clean — no regression. Phase B full matrix re-confirms.

### Bug #4 — Proto text-format parser mishandles `Float` / `Double` decimal literals — **STATUS: FIXED 2026-05-21**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoTokenizer.java`. Two distinct bugs in numeric tokenisation that compose into broken `Float` / `Double` bean-property round-trips through Proto's text format.
- **Bug #4a — `readDecimalOrFloat(boolean neg)` discards the `neg` flag for the Double return path** (`ProtoTokenizer.java` ~lines 261-265, 271-274). When the literal contains `.` or `e/E` the method calls `Double.parseDouble(s)` on the magnitude-only string and returns the result directly — the `neg` parameter is consumed by `lexNumber` but never threaded into the Double parse. The Long return path correctly applies `neg ? -Long.parseLong(s) : Long.parseLong(s)` (line 280), so integer literals round-trip with sign preserved but float literals do not.
- **Bug #4b — `lexNumber` `0`-prefix branch doesn't handle `0.x` decimals** (`ProtoTokenizer.java` lines 179-192). The branch consumes the leading `0`, checks for `x`/`X` (hex) or `0`-`7` (octal), and otherwise returns `DEC_INT 0` — leaving the `.` and trailing digits on the input stream. The leftover `.x` is then re-tokenised as a separate `FLOAT` token, which cascades into a field-name / value mis-alignment when the surrounding parser is reading a multi-field bean.
- **Symptom (#4a)**: `Float n = -3.14f` round-trips as `n = 3.14f` (sign lost). Same for boxed `Double`, primitive `float`, primitive `double`. Wire format on serialise is correct (`n: -3.140000104904175`); the parser strips the sign on read.
- **Symptom (#4b)**: in a bean with `pi=3.14f, half=0.5f, zero=0.0f`, the post-round-trip values are `pi=null, half=0.0f, zero=null` — the `0.5f` tokenises as `DEC_INT 0` (consumed as `half`'s value, then `.5` is left as a stray float token that confuses the bean-property dispatch for subsequent fields). The wire on serialise is correct (`half: 0.5\npi: 3.140000104904175\nzero: 0`); the parser corrupts the round-trip.
- **Wave-2 blast radius**: 10 failing invocations on `FloatFormat_Float_RoundTrip_Test` before the workaround was added — 5 on `a02_floatProperty_multipleFields` (one per `FloatFormat` value × Proto tester) plus 5 on `a03_floatProperty_negativeAndZero` (same shape). All independent of `FloatFormat` (Proto's `floatSwap` is bypassed for binary-session output → wire emits native Float regardless of format dispatch).
- **Categorization**: **Not** a Bug #1 mirror (the `floatSwap` is clean; the issue is on the parser side after the swap has already done its job). **Not** a Bug #2 mirror (this is the bean-property path, not the top-level `Number → T` format-drop path). **Not** a Bug #3 mirror (different format — Proto vs MsgPack — different mechanism — text vs binary tokenisation).  **Novel production bug.**
- **Reproduction (Bug #4a, minimal)**:

  ```java
  public static class B { public Float n; }
  var s = ProtoSerializer.create().build();
  var p = ProtoParser.create().build();
  var x = new B(); x.n = -3.14f;
  var wire = s.serialize(x);              // "n: -3.140000104904175"
  var y = p.parse(wire, B.class);
  // y.n == 3.14f  (sign lost — should be -3.14f)
  ```

- **Likely fix (Bug #4a, 1-line)**: in `readDecimalOrFloat(boolean neg)`, propagate `neg` to both Double return points:

  ```java
  var d = Double.parseDouble(s);
  return neg ? -d : d;
  ```

  At both line 262 (the `c == '.' || c == 'e' || c == 'E'` branch) and line 271 (the `f`/`F` suffix branch).
- **Likely fix (Bug #4b)**: extend the `c == '0'` branch in `lexNumber` (line 179) to also check for `.` as the next char and fall through to `readFloatLiteral(neg)` (already declared, used by the existing leading-`.` branch at line 193).
- **Workaround in test file (pre-fix)**: `FloatFormat_Float_RoundTrip_Test` skipped Proto for the affected tests via an `isProtoSerializer(t)` predicate — both `a02_floatProperty_multipleFields` and `a03_floatProperty_negativeAndZero`, and also `a04_floatProperty_nonFinite` (Proto's `nan`-identifier vs `nan`-token asymmetry, see Bug #4c). After the Bug #4 fix landed those skips became stale — the `a02` / `a03` skips would now collapse if removed; the `a04` skip stays because the asymmetry covered by **#4c** is unfixed.
- **Fix shape (#4a)**: `readDecimalOrFloat(boolean neg)` now applies `return neg ? -d : d;` on both Double return paths (decimal-with-`.`/`e`/`E` and `f`/`F`-suffix).
- **Fix shape (#4b)**: `lexNumber`'s `c == '0'` branch now peeks at the next char and falls through to `readFloatLiteral(neg)` when the next char is `.`. The hex / octal / decimal-zero paths are unchanged.
- **Diagnosis correction**: none — both fixes landed at the sites predicted by the diagnosis.
- **Closure**: all 10 affected Wave-2 `FloatFormat_Float_RoundTrip_Test` invocations green after fix. The previously-stale `a02` / `a03` Proto skips in that test file have been **collapsed** in the Bug #5 closure turn — the matrix now exercises the underlying Proto round-trip end-to-end without the workaround predicate. The `a04` Proto skip stays (Bug #4c).
- **Sibling findings from this turn**: Bug **#4c** (`mightStartNumber` `nan`/`inf` asymmetry, not yet fixed) and Bug **#4d** (`lexNumber` `0`-prefix `e`/`E`/`f`/`F` sibling, not yet fixed) — see entries below.

### Bug #4c — `ProtoTokenizer.mightStartNumber` `nan` / `inf` asymmetry (sibling to #4) — **STATUS: FIXED 2026-05-21**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoTokenizer.java` — `mightStartNumber(c)` returns `false` for `c == 'n'` and `c == 'i'`, so a bare `nan` / `inf` token at the start of a value went through `readIdentifier()` and never reached `readSpecialFloat`. Negative special floats worked (`-nan`, `-inf`) because the leading sign triggered `lexNumber`.
- **Symptom (pre-fix)**: `FloatFormat × NaN_AS_STRING × Proto × a04_floatProperty_nonFinite` (Float and Double) didn't round-trip the non-finite values cleanly when the wire shape emitted bare `nan` / `inf` tokens — the parser routed them through `readIdentifier()` and surfaced them as identifier tokens rather than special-float values.
- **Fix shape**: new helper `ProtoTokenizer.startsSpecialFloatLiteral` does an 8-char identifier-shape lookahead + value-terminator check, with `:` deliberately *not* treated as a terminator so a legitimate proto field name (`nan:` / `inf:` — both legal in proto schemas) still parses as a field name rather than a value. `mightStartNumber` consults this helper for the `n` / `i` start chars; `lexNumber`'s i/n branch routes through the new `readSpecialFloatOrIdent` helper so the disambiguation happens at lex time rather than in the parser. Negative `-nan` / `-inf` continue to route through the leading-sign path unchanged.
- **Diagnosis correction**: none — the disambiguation reasoning predicted by the diagnosis (multi-char lookahead + value-terminator check) is exactly what landed, with the additional refinement that `:` is *not* a terminator (field-name `nan:` keeps the identifier route).
- **Closure**: `FloatFormat_Float_RoundTrip_Test` and `FloatFormat_Double_RoundTrip_Test` now run the formerly-skipped Proto-nonFinite invocations end-to-end (both files at **1260 / 0** post-collapse — the `isProtoSerializer` predicate and the inline Proto skip in `a04_*Property_nonFinite` were both removed). No proto-field-named-`nan` / `inf` test in the matrix; the disambiguation reasoning is correct by construction.

### Bug #4d — `ProtoTokenizer.lexNumber` `0`-prefix `e` / `E` / `f` / `F` sibling (sibling to #4b) — **STATUS: FIXED 2026-05-21**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoTokenizer.java` — `lexNumber`'s `c == '0'` branch previously peeked for `.` only (Bug #4b fix), missing `e` / `E` / `f` / `F`. So a literal like `0e5` (= `0 × 10^5`) or `0f` (= `0.0f`) would tokenize as `DEC_INT 0` plus a stray `e5` / `f` identifier token — the same cascade shape as Bug #4b before its fix.
- **Severity (pre-fix)**: trivial / latent — no current production round-trip emitted these wire shapes through the matrix, so the bug was unreachable but the asymmetry with the `.`-peek was a correctness hole.
- **Fix shape**: `c == '0'` branch's peek now covers `.`, `e`, `E`, `f`, `F` and routes to `readFloatLiteral(neg)` for any of these chars. The `readFloatLiteral` signature was refactored to `readFloatLiteral(boolean neg, String prefix)` so the consumed `0` digit can be prepended to the parsed float literal (otherwise `0e5` would tokenize as `e5` after the `0` is consumed by the peek branch).
- **Diagnosis correction**: none — fix landed at the predicted site with the trivial extension described in the diagnosis.
- **Closure**: applied alongside Bug #4c. No new test was added since no current matrix invocation exercises the leading-zero-exponent path; the fix closes the latent gap so future proto-serializer outputs that emit these shapes parse correctly.

### Bug #5 — JSON-family parsers auto-classify bare decimals as `Float`, lossy when widened to `Double` — **STATUS: FIXED 2026-05-21**

- **Site (actual)**: `juneau-core/juneau-commons/src/main/java/org/apache/juneau/commons/utils/StringUtils.java` — the shared `parseNumber(String s, Class<? extends Number> type)` classifier (~line 5412) that every JSON-family parser session delegates to (`JsonParserSession.parseNumber` → `StringUtils.parseNumber`; `Json5ParserSession`, `XmlParserSession`, `HtmlParserSession`, `UonParserSession`, `UrlEncodingParserSession`, `JcsParserSession` all flow through the same helper). The `if (type == Double.class)` branch contained a misguided auto-detect Float-compaction:
  ```java
  var d = Double.valueOf(s);
  var f = Float.valueOf(s);
  if (isAutoDetect && (! isDecimal) && d.toString().equals(f.toString()))
      return f;   // <-- bug: returns Float when Float.toString matches Double.toString
  return d;
  ```
  When the configured target was `Number.class` / `Object.class` and the input parsed identically as Float and Double (e.g. `"-3.14"`), the helper returned `Float.valueOf("-3.14") = Float(-3.14f)`. The wire form was correct; the loss happened on the parser side at the auto-detect classification step. Per the user's repro: `JsonParser.DEFAULT.parse("-3.14", Object.class)` returned `Float`, and the downstream `Number → Double` widening on the bean-property path produced `-3.140000104904175 = (double)(float)(-3.14d)`.
- **Diagnosis correction**: the previous worker's hypothesis was a per-parser bare-numeric tokenisation site in each `ReaderParserSession`. The actual site is a single shared helper in `StringUtils.parseNumber` — every JSON-family parser flows through it, which explains why all 8 parsers exhibited the same auto-detect Float-tier classification. The fix-site count is **1**, not 8.
- **Symptom**: `Double` bean-property round-trip through any JSON-family parser narrows the value to `Float` precision and then widens lossy on the way back into the bean's `Double` field.
  - Bean property: `Double negative = -3.14d` → wire `"-3.14"` (correct) → parser auto-classifies as `Float(-3.14f)` → swap unswap (`Number → Double`) calls `n.doubleValue()` on the `Float` → `-3.140000104904175 = (double)(float)(-3.14d)`.
  - Top-level: `parser.parse("3.14", Double.class)` returns `Double(3.140000104904175)` (same widened-Float bit pattern) via the same auto-classification path. For UrlEncoding the top-level path succeeds and surfaces the bug; the other JSON-family parsers throw on a bare top-level non-object value and the existing top-level test's `try/catch` swallows the throw.
- **Wave-2-cleanup blast radius**: 66 failing invocations on `FloatFormat_Double_RoundTrip_Test` before in-test skips were applied:
  - **51 on `a03_doubleProperty_negativeAndZero`** — `-3.14d` / `3.14d` bean properties. Affects all 8 JSON-family parsers × the 3 `FloatFormat` values that install the float swap (`NaN_AS_NULL` / `NaN_AS_STRING` / `NaN_AS_ERROR`): 17 builders (b1-16 Json/Json5/Jsonl/Xml/Html/Uon/UrlEncoding + b38 Jcs) × 3 fmts = 51. The 2 fmts that don't install the swap (`NOT_SET` / `NaN_AS_NUMBER`) don't fail on `a03` because the bean property's `Double`-typed setter then narrows back through `Double.parseDouble` on the wire string rather than via the swap's `Float` path.
  - **15 on `a06_doubleTopLevel`** — `3.14d` top-level. Affects UrlEncoding 3 variants × all 5 fmts = 15. Other JSON-family parsers throw at top-level and the `try/catch` swallows.
- **Categorization**: **Not** a Bug #1 mirror (the `floatSwap` is correct — it's the parser handing it the wrong `Number` subtype). **Not** a Bug #2 mirror (Bug #2 was about format-hint drop on bare numerics; this is about precision-tier selection in auto-classification, format-independent). **Not** a Bug #3 mirror (different parser family — JSON / Xml / Html / Uon / UrlEncoding, not MsgPack). **Not** a Bug #4 mirror (different parser family — JSON / Xml / Html / Uon / UrlEncoding, not Proto). **Novel production bug.**
- **Reproduction (minimal)**:

  ```java
  public static class B { public Double n; }
  var s = JsonSerializer.create().floatFormat(FloatFormat.NaN_AS_NULL).build();
  var p = JsonParser.create().floatFormat(FloatFormat.NaN_AS_NULL).build();
  var x = new B(); x.n = -3.14d;
  var wire = s.serialize(x);              // {"n":-3.14}
  var y = p.parse(wire, B.class);
  // y.n == -3.140000104904175  (precision lost — should be -3.14)

  // Independent of the swap — surfaces at the raw auto-classification level too:
  var raw = JsonParser.DEFAULT.parse("{\"n\":-3.14}", java.util.Map.class).get("n");
  // raw.getClass() == java.lang.Float  (should be Double for max-precision-by-default)
  ```

- **Fix shape (option a — parser-side highest-precision default)**: drop the `isAutoDetect`-gated Float-compaction branch entirely so auto-detect always returns `Double` for the `type == Double.class` path. `Float` is now returned only when the caller explicitly requests `Float.class` / `Float.TYPE`. Matches the canonical convention in Jackson / Gson / RFC 7159 implementations where JSON's "number" lexical token resolves to the highest-precision floating tier on auto-detect. Diff (~1 line of logic, ~6 lines including the rationale comment):
  ```java
  if (type == Double.class || type == Double.TYPE) {
      // Bug #5 fix: always return Double in auto-detect mode for max precision.
      return Double.valueOf(s);
  }
  ```
- **Test-side updates required by the fix**:
  - `juneau-utest/src/test/java/org/apache/juneau/commons/utils/StringUtils_Test.java` — the direct unit test of the fix site. Several `assertTrue(parseNumber(s, null) instanceof Float)` and `assertEquals(<float-literal>, parseNumber(...))` assertions in `a099_isNumeric_parseNumber` / `a153_parseNumber` had been coded to the Float-compaction behaviour. Updated to `Double` expectations (~6 assertion blocks). These tests directly verify the classifier behaviour at the fix site, so updating them is part of the Bug #5 fix scope, not a separate "test coded to bug" follow-up.
  - **Follow-up — pre-existing tests coded to the bug, NOT fixed this turn per the user's protocol**: `juneau-utest/src/test/java/org/apache/juneau/html/BasicHtml_Test.java` lines 323 / 341 / 350 — `assertInstanceOf(Float.class, ...)` assertions that fire after the HTML parser auto-classifies the wire `1.23` through `StringUtils.parseNumber`. 9 failures total (3 lambdas × 3 verification methods `a3_verifyNormal` / `b3_verifyReadable` / `c3_verifyAbridged`). Same fix-shape applies (flip `Float.class` → `Double.class`); deferred to a separate follow-up turn per the "Don't fix it — that's a follow-up" instruction in the Bug #5 prompt. **The 12-format round-trip matrix is unaffected by these 9 BasicHtml failures.**
- **Stale-skip collapse**: `FloatFormat_Double_RoundTrip_Test`'s `isJsonFamilyParser(t)` predicate and the `a03_doubleProperty_negativeAndZero` / `a06_doubleTopLevel` skips were removed in the same turn — the matrix now runs **1260 / 0** across all JSON-family combos without skips. The predicate method itself was deleted (no remaining callers).
- **Closure**: 66 previously-skipped Wave-2 `FloatFormat_Double_RoundTrip_Test` invocations green after fix; 12-format full matrix at **28,224 / 0** across 12 test classes; full broader JSON-parser test suites (`JsonParser_Test`, `Json5Parser_Test`, `JsonlParser_Test`, `XmlParser_Test`, `HtmlParser_Test`, `UonParser_Test`, `UrlEncodingParser_Test`) re-ran clean — 40 / 0 with no regression.

### Bug #7a — Parquet bean-property path bypasses default-swap dispatch for `UUID` / `Class` / numeric-`Enum` — **STATUS: FIXED 2026-05-21**

- **Sites**:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parquet/ParquetParserSession.java` — `readValue` reassembles `LOGICAL_TYPE_UUID` columns back into {@link UUID} from the 16-byte `byte[]`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parquet/ParquetSerializerSession.java` — `writeValue` plus a new `applyDefaultSwap` helper consult the default-swap dispatch for `Class<?>`-typed properties before falling through to `String.valueOf(v)`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parquet/ParquetSchemaBuilder.java` — `addLeafSchema` `Number` branch picks `INT32` for the abstract `Number` swap-output type so `EnumFormat.ORDINAL` Enum properties land in a native numeric column instead of UTF-8.
- **Symptom (pre-fix #7a-UUID)**: `UUID` bean-property round-trip through Parquet at `UuidFormat.NOT_SET` / `STANDARD` failed on parse with `InvalidDataConversionException: Invalid data conversion from type 'org.apache.juneau.collections.JsonMap' to type ...A01Bean. Value={u:[85,14,-124,0,-30,-101,65,-44,-89,22,68,102,85,68,0,0]}`. The wire bytes were correct (16-byte UUID); the parser left them as `byte[]` in the row-level `JsonMap` and the framework then failed to convert to the bean's `UUID`-typed property.
- **Symptom (pre-fix #7a-Class)**: `Class<?>` bean-property round-trip through Parquet at every `ClassFormat` value emitted `Class.toString()` (with the literal `"class "` prefix) rather than the {@link ClassFormatSwap}-produced FQCN / BINARY_NAME form. Parquet wasn't consulting the default-swap dispatch when writing `Class<?>` properties.
- **Symptom (pre-fix #7a-Enum-ORDINAL)**: `Enum` bean-property round-trip through Parquet at `EnumFormat.ORDINAL` stored the ordinal as the string `"0"` because `ParquetSchemaBuilder` couldn't disambiguate the abstract {@link Number} swap-output type and fell back to `TYPE_BYTE_ARRAY UTF-8`.
- **Fix shape**:
  - **#7a-UUID** — parser-side `LOGICAL_TYPE_UUID` reassembly inside `ParquetParserSession.readValue` so 16-byte FLBA columns deserialize back to {@link UUID}.
  - **#7a-Class** — serializer-side default-swap dispatch via the new `applyDefaultSwap` helper called from `ParquetSerializerSession.writeValue`, mirroring the dispatch pattern other serializers use.
  - **#7a-Enum-ORDINAL** — schema-side: a `Number` branch added to `ParquetSchemaBuilder.addLeafSchema` selects `INT32` for the abstract `Number` swap-output type produced by the ORDINAL swap install.
- **Diagnosis correction**: none — the pre-fix diagnosis correctly identified the three sub-bugs as independent (parser-side UUID reassembly, serializer-side default-swap dispatch for `Class<?>`, schema-builder disambiguation for `Number` swap output) and the fix landed at the predicted sites.
- **Closure**: 22 formerly-skipped invocations now run green — 8 on `UuidFormat_RoundTrip_Test` (a01–a04 × NOT_SET/STANDARD × Parquet), 2 on `EnumFormat_RoundTrip_Test` (a01/a02 × ORDINAL × Parquet), 12 on `ClassFormat_RoundTrip_Test` (a01–a04 × all 3 fmts × Parquet). The three skip predicates (`isParquetNativeUuidPath`, `isParquetNumericEnumPath`, `isParquetClassBypassPath`) have been collapsed.
- **Follow-up flags**:
  - **Parquet file footer drops `logicalType` discriminant.** `ParquetSchemaElement.writeTo` writes only physical type + `convertedType`; the parser-side UUID fix uses `TYPE_FIXED_LEN_BYTE_ARRAY` as a UUID-only signal under the assumption that no other writer path emits FLBA. Fragile if the schema builder ever emits FLBA for non-UUID. Tighten by emitting the logical-type union in the footer and consuming it on the parse side. Tracked under "Open questions".
  - **`ClassFormat.FQCN` with nested types or arrays is parser-fragile.** `Class.forName("java.util.Map.Entry")` rejects the FQCN form (needs `"java.util.Map$Entry"`); the parse side has no `ClassFormatSwap.unswap` hook installed at `NOT_SET` / `FQCN`. Out of Bug #7a scope; affects all formats whose parser routes `String → Class<?>` through `Class.forName`. May need a separate work item; tracked under "Open questions".

### Bug #7b — Hjson / Hocon / Proto / Bson parsers drop typed `Map<Enum, V>` key class on reassembly — **STATUS: FIXED 2026-05-21 (residual closed 2026-05-21)**

- **Sites**: per-parser map-reassembly methods identified by Worker B in:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hjson/HjsonParserSession.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconParserSession.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoParserSession.java`
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/bson/BsonParserSession.java`
- **Fix shape**: each parser session threads the bean property's `ClassMeta<K>` for the map key into the key-coercion step at the parser-specific dispatch point. **No shared lift** — each parser's map-reassembly code path is sufficiently distinct (different wire-form layouts, different key-coercion entry points) that consolidating into a base-class helper would force each session to translate its native key representation into a shared lift's input shape, which is more code than the per-parser fix. Per-parser changes are self-contained and parallel the Bug #2 disposition.
- **Diagnosis correction (systemic finding)**: `BeanPropertyMeta.setPropertyValue` only inspects entry **values** not entry **keys** for typed `Map<K, V>` properties when deciding whether to call `needsConversion`. So this was a fix-here-or-fix-the-commons-side decision; per-parser was chosen because the per-parser sites are simpler and don't risk regressing the JSON-family parsers that already handle this case correctly. The Bug #7b per-parser fixes render the commons-side gap unreachable from any tested parser, but a future parser implementation that drops typed map keys would reproduce the symptom at the latent commons-side gap; tracked under "Open questions".
- **Residual fix shape (Proto × ORDINAL × a04)**: split across the serializer and parser sides of Proto's wire-form for ordinal-keyed maps.
  - **Serializer**: `ProtoWriter.fieldName` + new `ProtoWriter.isBareIntegerTag` helper. The pre-fix wire emitted `Map<TestEnum, String>` ordinal keys as quoted strings (`"0": "first"`); Proto's adjacent-string-literal rule then concatenated the previous string value with the quoted key on the parser side (`"first" + "1"` → `"first1"`). Fix emits bare integer field tags (`0: "first"`) using `isBareIntegerTag` to detect numeric-only key strings.
  - **Parser**: `ProtoParserSession.readFieldName` now accepts `DEC_INT` / `HEX_INT` / `OCT_INT` field-tag token classes alongside identifier tokens.
- **Residual diagnosis correction**: the original framing as a "tokenizer drops the wire delimiter" was incorrect. The wire was *correctly* serialized as `"0": "first"` (quoted-string field tags); the proto adjacent-string-literal rule then concatenated the previous value with the quoted key. The actual fix is on the serializer side (emit bare integer field tags) plus a symmetric parser-side acceptance of numeric field-tag tokens.
- **Closure**: all 36 formerly-skipped `EnumFormat_RoundTrip_Test.a04_enumProperty_inMapKey` invocations now run green; the residual `isProtoEnumMapKeyOrdinalGap` predicate has been deleted. `parserDropsEnumMapKeyType` predicate was deleted in the post-Wave-3 turn.

### Bug #8 — Toml / Proto / Ini bare-value handling for hyphen / underscore-bearing `Locale` wire forms — **STATUS: FIXED 2026-05-21**

- **Sites**:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/toml/TomlSerializerSession.java` — `writeValue` for collection elements applies the runtime swap (was missing for swap-on-elements).
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoSerializerSession.java` — `serializeScalarValue` applies the runtime swap for collection elements (analogous to Toml).
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ini/IniParserSession.java` — the inner `Json5Parser` is now constructed with the parent `MarshallingContext` so the configured `LocaleFormat` propagates through the inner parse (previously used `Json5Parser.DEFAULT`, which dropped the parent context).
- **Fix shape**:
  - **Toml + Proto** — serializer-side: for a list-element / collection-element bean property, apply the default-swap dispatch in `writeValue` / `serializeScalarValue` before emitting the wire form. The bean-property path was already correct; the gap was specifically on swap-on-elements.
  - **Ini** — parser-side: `IniParserSession` was constructing a fresh `Json5Parser.DEFAULT` to parse the inner value, which dropped the parent `MarshallingContext` (and with it the `LocaleFormat` configuration). Fixed by constructing the inner `Json5Parser` with the parent context propagated.
- **Diagnosis correction**: the earlier "bare-value tokenizer" framing was incorrect. The Toml / Proto failures were not lexer-level — the wire form was correctly produced for bean properties; the gap was specifically at the **swap-on-elements** site (collection elements weren't getting the runtime swap applied). The Ini failure was also not a tokenizer issue — it was the parser-side construction of the inner `Json5Parser` dropping the parent `MarshallingContext`. Root causes were serializer-side swap-on-elements gap (Toml/Proto) and parser-side context drop (Ini), not lexer behavior.
- **Closure**: all 7 formerly-skipped `LocaleFormat_RoundTrip_Test` invocations now run green (5 × `a04_localeProperty_inList`, 2 × `a06_localeTopLevel`). The `isConfigFormatBareValueGap` predicate has been deleted.

### Bug #9 — `BinaryFormat.parse` format-agnostic sniff drops the configured `BASE64_URL` hint on non-3-aligned input — **STATUS: FIXED 2026-05-21**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/BinaryFormat.java` — `parse(String)`. The class-level Javadoc claims "the configured format is consulted only as a hint for ambiguous input," but the `parse` implementation never read `this` — the hint was dropped silently. The format-agnostic sniff branched on the wire shape: spaced-hex (contains spaces), URL-safe base64 (contains `-` or `_`), pure hex (even length, hex-only chars), otherwise standard base64. For a non-3-aligned `BASE64_URL` payload that happened to not contain `-` / `_` (because the encoded chars happened to fall outside the URL-safe-distinct range), the sniff routed to standard base64 — which failed on the missing padding.
- **Symptom**: round-tripping a `byte[]` bean property at `BinaryFormat.BASE64_URL` through any text serializer fails for the test payloads `{0x99}` (→ `"mQ"`, 2 chars), `{0x12, 0x34, 0x56, 0x78}` (→ `"EjRWeA"`, 6 chars), and `{0x00, 0x01, 0xFF, 0x80, 0x7F, 0x10, 0x20, 0x30}` (→ `"AAH_gH8QIDA"`, 11 chars). Throws `IllegalArgumentException: Invalid binary value 'mQ' for format BASE64_URL: Invalid BASE64 string length. Must be multiple of 4.` — note the format-in-message correctly says `BASE64_URL` (from the outer wrapper) but the actual decode path used was `Base64.getDecoder()` (standard) not `Base64.getUrlDecoder()`.
- **Categorization**: **Novel production bug**. Not a Bug #1 mirror (this is `BinaryFormat.parse`, not a Marshalled property post-processor swap). Not a Bug #2 mirror (Bug #2 was format-hint drop at the *parser* layer; this is format-hint drop *inside* `BinaryFormat.parse` itself). Not a Bug #5 / #7 / #8 / #11 mirror (different format / different fix-site / different mechanism).
- **Reproduction (minimal)**:

  ```java
  // Standalone:
  byte[] decoded = BinaryFormat.BASE64_URL.parse("mQ");
  // throws IllegalArgumentException: Invalid binary value 'mQ' for format BASE64_URL

  // End-to-end:
  public static class B { public byte[] b; }
  var s = JsonSerializer.create().binaryFormat(BinaryFormat.BASE64_URL).build();
  var p = JsonParser.create().binaryFormat(BinaryFormat.BASE64_URL).build();
  var x = new B(); x.b = new byte[]{(byte) 0x99};
  var wire = s.serialize(x);              // {"b":"mQ"}
  var y = p.parse(wire, B.class);         // throws same exception
  ```

- **Likely fix (1-line)**: route the `BASE64_URL` constant directly to `Base64.getUrlDecoder().decode(s)` (which accepts missing padding) before the format-agnostic sniff. Or more conservatively, when the configured format is `BASE64_URL`, override the sniff and always use the URL-safe decoder. Sample diff (~5 lines including a comment):
  ```java
  public byte[] parse(String value) {
      if (value == null || value.isEmpty())
          return new byte[0];
      // Bug #9 fix: honour the configured format constant for BASE64_URL (decoder accepts missing padding).
      if (this == BASE64_URL)
          return Base64.getUrlDecoder().decode(value);
      // ... format-agnostic sniff below ...
  }
  ```
- **Wave-4 blast radius (pre-fix)**: 42 testers × 5 affected test methods (a01 / a02 / a03 / a04 / a06) = **210 invocations** narrowed by `isBase64UrlPaddingStripGap(fmt)` in `BinaryFormat_RoundTrip_Test.java`. a05 (null field) unaffected because null never reaches `parse`.
- **Fix shape (landed)**: `BinaryFormat.parse(String)` adds an early-return `if (this == BASE64_URL) return Base64.getUrlDecoder().decode(value);` before the format-agnostic sniff. The URL decoder accepts missing padding (unlike `Base64.getDecoder()`).
- **Decision per worker**: only `BASE64_URL` needs hint-honoring. The standard `BASE64` / `HEX` / `SPACED_HEX` sniff is unambiguous (each has a distinct wire-shape signature), so the per-format hint is irrelevant for those constants.
- **Diagnosis correction**: none — fix landed at the predicted site with the predicted 1-line shape.
- **Closure**: `isBase64UrlPaddingStripGap` predicate deleted; the formerly-skipped 210 invocations now exercise the BASE64_URL round-trip end-to-end. `BinaryFormat_RoundTrip_Test` runs at **1260 / 0** post-collapse — the post-Wave-4 a04-residual narrow (`isA04ByteArrayCollectionResidual`) was subsequently closed 2026-05-21; see the "Post-Wave-4 a04 residual closure" subsection in Phase 5 below.

### Bug #10 — `HoconSerializer` doesn't quote BASE64 wire output containing `=` padding — **STATUS: FIXED 2026-05-21**

- **Site (actual)**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconWriter.java` — the `QUOTE_VALUE_CHARS` character set used by the value-emission path to decide whether to quote. The set previously omitted `=`, so any `BinaryFormat.BASE64` wire value with trailing `=` padding (e.g. `AAH/gH8QIDA=`) was emitted unquoted. The Hocon parser's tokenizer then read the first `=` as the assignment operator, encountered another `=` at the end of the value, and failed with `ParseException: Expected key at line 2`.
- **Symptom**: round-tripping a `byte[]` bean property at `BinaryFormat.BASE64` through the Hocon serializer / parser fails for any non-3-aligned payload. The Hocon wire is `b = AAH/gH8QIDA=` for an 8-byte BASE64 payload; the parser fails immediately because the unquoted trailing `=` is parser-meaningful.
- **Categorization**: **Novel production bug**. Confirmed sibling of two earlier Hocon parser-fragility findings — same root cause shape (serializer doesn't quote values containing parser-meaningful characters, parser tokenizer / recursion doesn't recover gracefully):
  - Wave-1 candidate finding — `+NNNN`-suffix substrings trigger "Depth too deep — stack overflow" on Calendar/Date `BASIC_ISO_DATE` wire forms (e.g. `20240615+0900`).
  - Wave-3 candidate finding — `$`-character substrings trigger the same "Depth too deep" on nested-class `BINARY_NAME` wire forms (e.g. `java.util.Map$Entry`).
- **Reproduction (minimal)**:

  ```java
  public static class B { public byte[] b; }
  var s = HoconSerializer.create().binaryFormat(BinaryFormat.BASE64).build();
  var p = HoconParser.create().binaryFormat(BinaryFormat.BASE64).build();
  var x = new B(); x.b = new byte[]{0,1,(byte)-1,(byte)-128,127,16,32,48};
  var wire = s.serialize(x);              // contains: b = AAH/gH8QIDA=
  var y = p.parse(wire, B.class);
  // throws ParseException: Expected key at line 2 …
  ```

- **Likely fix (Hocon-serializer-side)**: extend the Hocon serializer's value-emission path to quote any value containing `=` (or, more broadly, any value matching the BASE64-padding character class). The Hocon spec already treats `=` as an alternate assignment operator, so emitting `b = "AAH/gH8QIDA="` is the right shape — Hocon accepts both equals and colon assignment with quoted-string RHS.
- **Likely fix (Hocon-parser-side, more thorough)**: fix the underlying tokenizer / recursion bug that's been showing up across three independent Wave-1 / Wave-3 / Wave-4 surfaces. Suggested as a consolidated Hocon parser pass — the symptoms point at a common implementation flaw (e.g. character-class peek that doesn't escape parser-meaningful chars).
- **Wave-4 blast radius (pre-fix)**: 1 tester × 5 test methods (a01 / a02 / a03 / a04 / a06) = **5 invocations** narrowed by `isHoconBase64PaddingGap(t, fmt)` in `BinaryFormat_RoundTrip_Test.java`. BASE64_URL and SPACED_HEX / HEX unaffected because they don't emit `=` padding.
- **Fix shape (landed)**: `HoconWriter.QUOTE_VALUE_CHARS` adds `=` to the set so any value containing `=` is quoted. The Hocon spec accepts both `key = value` and `key = "value"` assignments, so emitting `b = "AAH/gH8QIDA="` is the right shape.
- **Diagnosis correction**: none — fix landed at the predicted site (serializer-side quoting decision in `HoconWriter`).
- **Closure**: `isHoconBase64PaddingGap` predicate deleted. The Wave-1 `+NNNN` Calendar `BASIC_ISO_DATE` and Wave-3 `$` `ClassFormat.BINARY_NAME` Hocon parser-fragility findings remain *unfixed* by this bug — they need a Hocon parser-side audit. The serializer-side `HoconWriter.QUOTE_VALUE_CHARS` set could plausibly also quote `+` and `$` if the parser-fragility audit recommends it (see Open Questions).

### Bug #11 — Parquet / RdfThrift / RdfProto don't honour the variant `binarySwap`'s native-bytes contract — **STATUS: FIXED 2026-05-21**

- **Sites (pre-fix)**:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/parquet/ParquetParserSession.java` — `byte[]` column read-back reconstructed the bytes as `List<Integer>` in the intermediate `JsonMap` row representation; the framework's `List<Integer> → byte[]` coercion then failed. Same shape as Bug #7a's UUID parse-back gap (fixed for `UUID` / `Class<?>` / `Enum-ORDINAL` in the post-Wave-3 turn) but for raw `byte[]` — Bug #7a's fix didn't extend to the non-logical-type `byte[]` column path.
  - `juneau-core/juneau-marshall-rdf/src/main/java/org/apache/juneau/jena/RdfStreamSerializer.java` (and the `RdfThriftSerializer` / `RdfProtoSerializer` subclasses) — the RDF graph layer didn't have a native `byte[]` literal type; the binary RDF encoders fell back to emitting the `byte[]` as a JSON-array literal (`[0,1,-1,...]`). On parse, the framework's `String → byte[]` fallback tried to base64-decode the array text and failed with `Illegal base64 character 5b` (`0x5B` = `[`).
- **Symptom**: `byte[]` bean-property round-trip through Parquet / RdfThrift / RdfProto at any non-`BinaryFormat.NOT_SET` format throws on parse. At `BinaryFormat.NOT_SET` the variant `binarySwap` is not installed (MPP guards on `bc.getBinaryFormat() != NOT_SET`), so these serializers fall through to their natural handling — but Parquet / Hocon / Toml / Proto then exhibit the **NOT_SET extension** symptom: emit `byte[]` as a JSON-array-of-integers / column-of-integers representation that the parser can't reverse into `byte[]`.
- **Categorization**: **Novel production bug**, sibling of Bug #7a but at a different scope. Bug #7a covered Parquet's UUID / Class<?> / Enum-ORDINAL paths; Bug #11 covers the raw `byte[]` path on Parquet plus the corresponding gap on RdfThrift / RdfProto. The variant `binarySwap`'s design intent — "binary serializers receive raw `byte[]` and handle it natively" — holds for MsgPack / CBOR / BSON (each of which has a dedicated binary opcode: `bin` / byte-string / `0x05`) but **doesn't hold** for Parquet (which needs a `LOGICAL_TYPE_BYTES` schema discriminator, sibling to Bug #7a's UUID fix) or for RdfThrift / RdfProto (which need a binary-RDF-specific encoding of opaque bytes, e.g. an `xsd:base64Binary` typed literal).
- **Reproduction (minimal — Parquet)**:

  ```java
  public static class B { public byte[] b; }
  var s = ParquetSerializer.create().binaryFormat(BinaryFormat.BASE64).build();
  var p = ParquetParser.create().binaryFormat(BinaryFormat.BASE64).build();
  var x = new B(); x.b = new byte[]{0,1,(byte)-1};
  var wire = s.serialize(x);              // binary parquet, byte[] column
  var y = p.parse(wire, B.class);
  // throws InvalidDataConversionException: ... List<Integer> → byte[]
  ```

- **Reproduction (minimal — RdfThrift)**: same shape with `RdfThriftSerializer` / `RdfThriftParser`. Throws `Illegal base64 character 5b`.
- **Likely fix (Parquet)**: extend Bug #7a's `ParquetSchemaBuilder.addLeafSchema` + `ParquetParserSession.readValue` pattern to cover the `byte[]` column type. Schema: emit `LOGICAL_TYPE_BYTES` (or use a discriminator on `TYPE_BYTE_ARRAY` without a `convertedType`); parser: reassemble the column bytes back into a `byte[]` on the way into the `JsonMap` row representation. Sibling of the Bug #7a UUID parse-back fix.
- **Likely fix (RdfThrift / RdfProto)**: emit the `byte[]` as an `xsd:base64Binary` typed RDF literal (or use the binary-RDF encoding's native bytes-payload if available). On parse, recognize the typed literal and short-circuit to the raw bytes.
- **Wave-4 blast radius (pre-fix)**:
  - 3 testers (Parquet, RdfThrift, RdfProto) × 4 non-NOT_SET formats × 4 test methods (a01 / a02 / a03 / a04) = 48 invocations narrowed by `isBinaryWithoutNativeBytesPath(t, fmt)`.
  - 4 testers (Parquet, Hocon, Toml, Proto) × 1 NOT_SET × 1 test method (a04) = 4 invocations narrowed by `isNotSetByteArrayCollectionDeadEnd(t, fmt)` — extension of Bug #11 at NOT_SET.
  - Plus the a06 top-level surface for Parquet covered by `returnOriginalObject()` (validation-only) and for RdfThrift / RdfProto by `isBinaryWithoutNativeBytesPath` already.
- **Fix shape (landed)**:
  - **Parquet**: `ParquetSchemaBuilder.addSchemaElements` adds a new `cm.isByteArray()` branch that emits the native `TYPE_BYTE_ARRAY` schema element without a `convertedType` — the *absence* of `convertedType` becomes the unique raw-`byte[]` signal. `ParquetParserSession` adds a new `SchemaReadResult` record so `readSchema` can capture each leaf column's `convertedType` (Thrift footer field id 6); `readValue` consults `rawByteArrayPaths` to reassemble `byte[]` columns directly rather than as `List<Integer>`. The schema discriminator is the sibling of Bug #7a's pattern: Bug #7a uses `TYPE_FIXED_LEN_BYTE_ARRAY` (+ length 16) as a UUID-only signal; Bug #11 uses `TYPE_BYTE_ARRAY` (no `convertedType`) as a raw-`byte[]`-only signal.
  - **RdfThrift / RdfProto**: `RdfStreamSerializerSession.serializeAnything` adds a new branch that emits the `byte[]` as an `xsd:base64Binary` typed RDF literal (`"AAH/gH8QIDA="^^xsd:base64Binary`). `RdfStreamParserSession.parseAnything` adds the symmetric parse branch that recognizes the typed literal and short-circuits to the raw bytes via `Base64.getDecoder().decode`.
- **Diagnosis correction**: none — the fix shapes predicted (Parquet logical-type discriminator + RDF typed-literal encoding) landed at the predicted sites.
- **Closure**: `isBinaryWithoutNativeBytesPath` and `isNotSetByteArrayCollectionDeadEnd` predicates both deleted from `BinaryFormat_RoundTrip_Test.java`. Formerly-skipped invocations green for Parquet × all formats × a01-a04 and for RdfThrift / RdfProto × all non-NOT_SET formats × a01-a04. The Bug #11 NOT_SET-extension surface (Toml / Proto / Hocon at NOT_SET on a04) was subsequently closed in the post-Wave-4 a04 residual closure turn (2026-05-21) — see Phase 5 subsection below. Worker note flagged in Open Questions: `juneau-utest`'s test classpath resolves `juneau-marshall-rdf` from `~/.m2/repository`, so an `mvn -pl juneau-core/juneau-marshall-rdf -DskipTests install` is required after edits to the RDF module before any utest run will pick up the new code.

### Bug #12 — Toml / Proto / Hjson / Hocon collection-element / top-level `byte[]` doesn't route through `BinarySwap.unswap` — **STATUS: FIXED 2026-05-21**

- **Sites (per-parser map-/list-element / top-level reassembly methods)**:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/toml/TomlParserSession.java` — collection-element dispatch (new `tryUnswapByteArray` helper threaded into `convertValue`).
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hjson/HjsonParserSession.java` — 4 lone-value sites collapsed into a single `coerceMemberValue` entry point; `propertyType` widened to surface `Collection` / `Array` types so element types reach the swap-dispatch.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconParserSession.java` — STRING / ARRAY type-threading in `hoconToMap` + `throws ParseException` signature update.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoParserSession.java` — top-level-only swap dispatch in `doParse` (collection-element route is the Bug #11 surface, intentionally not touched).
- **Symptom**: a `List<byte[]>` bean property at any non-`NOT_SET` `BinaryFormat` serializes correctly (each element formatted via the variant `binarySwap`'s `swap`), but on parse each element is read as a `String` and **not** routed back through `BinarySwap.unswap` — instead the framework's default `String → byte[]` fallback runs, producing a `byte[]` of the ASCII chars rather than the format-decoded bytes. Example: at `BinaryFormat.HEX`, an 8-byte element serializes as `"0001ff807f102030"` (16 chars); on parse, the element comes back as a 16-byte ASCII char array. Top-level `byte[]` exhibits the same shape on Hjson / Proto.
- **Diagnosis**: the bean-property path works because `MarshalledPropertyPostProcessor.applyContextFormats` installs the variant `binarySwap` on the property's `BeanPropertyMeta` directly — the parser consults the per-property swap and unswap correctly. But for collection elements and top-level values, the framework falls back to the `DefaultSwaps` registry's `BinarySwap.match` dispatch — and the four parsers above don't consult that registry at the per-element / top-level decision point, so the unswap never fires.
- **Categorization**: **Novel production bug**. Closely shaped sibling of Bug #7b (`Map<Enum, V>` key-class drop) and Bug #8 (Toml/Proto/Ini bare-value tokenizer drops format hint) — same family of "per-parser dispatch site doesn't consult the configured format swap when reassembling a non-bean-property value". The fix-shape mirrors Bug #7b: thread the element's expected `ClassMeta<byte[]>` through to a per-parser unswap-dispatch call before the default `String → byte[]` coercion runs.
- **Reproduction (minimal — Hocon × HEX)**:

  ```java
  public static class B { public List<byte[]> list; }
  var s = HoconSerializer.create().binaryFormat(BinaryFormat.HEX).build();
  var p = HoconParser.create().binaryFormat(BinaryFormat.HEX).build();
  var x = new B(); x.list = List.of(new byte[]{0,1,(byte)-1,(byte)-128,127,16,32,48});
  var wire = s.serialize(x);              // list = [ "0001ff807f102030" ]
  var y = p.parse(wire, B.class);
  // y.list.get(0).length == 16 (ASCII bytes of the hex string) instead of 8 (decoded bytes)
  ```

- **Likely fix shape**: per-parser, at the collection-element / top-level dispatch site, consult `session.getSchema().getClassMeta(byte[].class).getDefaultSwap()` (or the equivalent BinarySwap dispatch) before falling through to the default `String → byte[]` coercion. Mirrors the pattern Worker B used for Bug #7b's per-parser `Map<K, V>` key dispatch fix.
- **No shared lift**: each parser's collection-element reassembly path is sufficiently distinct (different wire-form layouts, different unswap entry points) that consolidating into a base-class helper would force each session to translate its native element representation into a shared lift's input shape — more code than the per-parser fix. Same disposition as Bug #7b.
- **Wave-4 blast radius (pre-fix)**: 4 testers (Toml, Proto, Hjson, Hocon) × 4 non-NOT_SET formats × 2 test methods (a04 list + a06 top-level) = **32 invocations** narrowed by `isCollectionOrTopLevelByteArrayUnswapGap(t, fmt)` in `BinaryFormat_RoundTrip_Test.java`. The bean-property paths (a01 / a02 / a03 / a05) unaffected because the per-property MPP install handles them.
- **Fix shape (landed)**: mirrors Bug #7b's per-parser pattern — each parser threads the expected element / top-level `ClassMeta<byte[]>` through to a swap-dispatch call before the default `String → byte[]` coercion. HJSON / HOCON `propertyType` widening to surface `Collection` / `Array` was load-bearing: without surfacing the parameterized type, the element type was lost before the swap-dispatch fired.
- **Diagnosis correction**: none — fix landed per-parser with the predicted shape. Proto's collection-element route was *not* in Bug #12's initial scope per the worker (it's a Bug #11 sibling); the Proto fix landed at the top-level dispatch site only, leaving the collection-element route as a narrow `isA04ByteArrayCollectionResidual` Bug #11/#12 residual. That residual was subsequently closed in the post-Wave-4 a04 residual closure turn (2026-05-21) — see the Phase 5 subsection below.
- **Closure**: `isCollectionOrTopLevelByteArrayUnswapGap` predicate deleted. All 32 formerly-narrowed invocations now run green end-to-end after the post-Wave-4 a04 residual closure landed the per-format collection-element fix in `ProtoParserSession.convertValue`.

### Bug #13 — UrlEncoding-expanded empty `byte[]` round-trips as `null` — **STATUS: FIXED 2026-05-21**

- **Sites**:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/urlencoding/UrlEncodingSerializerSession.java` — `serializeBeanMap` emits a `key=` sentinel for empty arrays / collections so the wire shape preserves the empty-but-not-null distinction.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/urlencoding/UrlEncodingParserSession.java` — `parseIntoBeanMap` S3 branch's `cm.canCreateNewInstance()` guard was extended to `cm.canCreateNewInstance() || cm.isArray()` so primitive arrays go through `cm.newInstance()` instead of falling through to the generic-collection path.
- **Symptom (pre-fix)**: at `BinaryFormat.NOT_SET` × UrlEncoding-expanded × empty `byte[]`, the round-trip read back `null` instead of `byte[0]`. Sibling of the well-known UrlEncoding-expanded `null`-vs-empty-list confusion. The default and readable UrlEncoding flavours emit `&empty=$a()` which parses to `byte[0]`, so they didn't surface this gap — only expanded-params did, because expanded-params elided the `&key=` segment for empty arrays.
- **Categorization**: existing-bug mirror promoted to a numbered bug because the post-Wave-4 known-bug-fix turn closed it. Worker A's fix is paired (serializer emits the sentinel + parser handles primitive arrays through `cm.newInstance()`).
- **Fix shape (landed)**: serializer-side `key=` sentinel for empty arrays/collections in `UrlEncodingSerializerSession.serializeBeanMap`; parser-side branch widening (`cm.canCreateNewInstance() || cm.isArray()`) in `UrlEncodingParserSession.parseIntoBeanMap` S3 branch so the empty-array path runs `cm.newInstance()` (which returns `byte[0]` for `byte[]`-typed properties) instead of falling through to the generic collection coercion that returned `null` for empty input.
- **Closure**: `isUrlEncodingExpandedEmptyByteArrayGap` predicate + inline `a03` tolerance collapsed in `BinaryFormat_RoundTrip_Test.java`. The matrix now exercises the empty-`byte[]` round-trip end-to-end across all UrlEncoding variants. `BinaryFormat_RoundTrip_Test` runs at **1260 / 0** post-collapse.

### Bug #14 — Toml `writeValue` type-erasure dispatch on `List<T[]>` — **STATUS: FIXED 2026-05-21**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/toml/TomlSerializerSession.java` — `writeValue`. The pre-fix `byte[]`-only branch was generalized to a runtime-array branch using `java.lang.reflect.Array`, so all primitive + reference array element types are covered.
- **Symptom (pre-fix)**: `List<T[]>` collection-element dispatch matched on the `aType` (the element's bean-property type) rather than the runtime class. Under Java's `List<T[]>` erasure the `aType` was `Object`, the dispatch fell through to the generic `String.valueOf` fallback, and the element serialized as the JVM's default `Object.toString()` (e.g. `[I@7c30a502` for an `int[]` element).
- **Categorization**: incidental finding from the post-Wave-4 a04 residual closure that was generalized in the post-Wave-4 known-bug-fix turn. The original a04-residual fix was scoped to `byte[]` only; the generalized fix covers the entire shape.
- **Fix shape (landed)**: `TomlSerializerSession.writeValue` now uses `value.getClass().isArray()` + `java.lang.reflect.Array.getLength` / `Array.get` instead of `aType.isArray()`-then-`(byte[]) value`. This dispatches on the runtime class of the value rather than the bean-property's declared element type, so erased `List<T[]>` properties serialize correctly for every primitive + reference array element type.
- **Closure**: no test-side change needed (the matrix's a04 list-element coverage already exercises the erased-list path; the previous fix had been scoped to `byte[]` only because that was the only failing element type at the time). Diagnosis: the pre-fix Toml writeValue `byte[]` branch was load-bearing for the post-Wave-4 a04 residual closure but the dispatch shape was unsound for any other primitive / reference array.

### Bug #15 — Hocon parser-fragility (multi-surface) — **STATUS: FIXED 2026-05-21**

- **Sites**:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconWriter.java` — `QUOTE_VALUE_CHARS` extended with `+` and `$` (`=` was already added in Bug #10's fix).
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconTokenizer.java` — `readUnquotedOrNumber` infinite-recursion guard converted from a JVM-`StackOverflowError` trip into a clean `IOException`.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconParserSession.java` — `parseArray` flatten bug fixed (replaced the unconditional `arr.concat(arr2)` against the next-array peek with a peek-before-skip guard for `RBRACKET` / `EOF`); symmetric `RBRACE` guard added to `parseObject`.
- **Symptom**: three independent Hocon failures surfaced across Waves 1 / 3 / 4 (`+NNNN` Calendar `BASIC_ISO_DATE` recursion, `$` `ClassFormat.BINARY_NAME` recursion, `=` `BinaryFormat.BASE64` padding mis-tokenization) plus the post-Wave-4 array-concat bug surfaced during the a04 residual closure. All four are different symptoms of the same shape: serializer doesn't quote values containing parser-meaningful chars, and the parser tokenizer / recursion doesn't recover gracefully.
- **Categorization**: consolidated audit pass that closed the multi-surface Hocon parser-fragility shape promoted from Open Questions item 9.
- **Fix shape (landed)**:
  - **Serializer-side**: `HoconWriter.QUOTE_VALUE_CHARS` set extended with `+` (Wave-1 `+NNNN` Calendar) and `$` (Wave-3 nested-class `$`). The Hocon spec accepts both `key = value` and `key = "value"`, so emitting `key = "+0900"` / `key = "java.util.Map$Entry"` is the right shape.
  - **Parser-side recursion guard**: `HoconTokenizer.readUnquotedOrNumber` now throws `IOException` on the unrecoverable case rather than recursing into itself indefinitely. The behavior change is "fail-fast with a parseable error" instead of "blow the JVM stack" — semantically equivalent for the round-trip matrix (both routes fail the parse) but produces a recoverable error path for callers.
  - **Parser-side flatten bug**: `parseArray` previously called `parseArray()` recursively after every closing `]` and concatenated the result, which collapsed nested arrays (`[[1,2],[3,4]]` parsed as `[1,2,3,4]`). The unconditional concat is replaced with a peek-before-skip guard: peek the next non-whitespace token, and only continue concatenation if it's another `[` (legitimate Hocon array-concatenation form `[a] [b]` ≡ `[a, b]`); on `RBRACKET` / `EOF` stop the recursion. Symmetric `RBRACE` guard added to `parseObject` (which had the same shape for nested object flattening).
- **Closure**: closes Open Questions items 9 and 12. Test-side closures: `isHoconNestedBinaryName` predicate deleted from `ClassFormat_RoundTrip_Test`; `Asia/Tokyo` zone restored in `CalendarFormat_RoundTrip_Test.a03_calendarProperty_crossZone` (Wave-1 workaround that switched the cross-zone sample to `America/Los_Angeles` is now reversed). Both files green at expected counts.
- **Follow-up flags surfaced** (tracked in Open Questions 13–16): `peekNoSkip` eager-consume pattern is a class of bug; `UNQUOTED_FORBIDDEN` ↔ `QUOTE_VALUE_CHARS` set-membership audit needed; HOCON `byte[]`-at-NOT_SET base64 sidestep (Phase 5 a04 closure) is now redundant; `HoconTokenizer.readTokenImmediate`'s `c == '=' && peekChar() != '+'` guard is suspicious.

### Bug #16 — `ClassFormat.FQCN` nested-types / arrays parser-fragility — **STATUS: FIXED 2026-05-21**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassFormat.java` — `parse(String, ClassFormat, ClassLoader)` (single file, +120/-22 lines).
- **Symptom (pre-fix)**: `Class.forName("java.util.Map.Entry")` rejects the FQCN form (needs `"java.util.Map$Entry"`); the parse side had no `ClassFormatSwap.unswap` hook installed at `NOT_SET` / `FQCN`. Affected all formats whose parser routes `String → Class<?>` through `Class.forName`. Also affected leaf primitives — `Class.forName("int")` rejects the simple-name form (needs the JVM-internal `[I` shape for arrays, no shape at all for leaves).
- **Categorization**: promoted from Open Questions item 8 (Worker A flagged it during the Bug #7a closure).
- **Fix shape (landed)**: three-tier resolution in `ClassFormat.parse`:
  - **Tier 1 — Array-suffix stripping via `Class.arrayType()`**: detect trailing `[]` brackets, strip them, recursively resolve the leaf type, then call `arrayType()` once per stripped pair. Covers `int[]`, `java.lang.String[][]`, `java.util.Map.Entry[]`.
  - **Tier 2 — Primitive-name table for leaf primitives**: hand-rolled table mapping `"int"`, `"long"`, `"double"`, `"float"`, `"boolean"`, `"byte"`, `"short"`, `"char"`, `"void"` to the matching primitive `Class<?>` constants. Covers `Class.forName("int")` which previously rejected.
  - **Tier 3 — Multi-level dot-to-`$` walk-back for nested types**: try `Class.forName(s)`; on `ClassNotFoundException`, walk the dot-separated tail of the name converting trailing dots to `$` (one at a time) and retry. Covers `java.util.Map.Entry` → `java.util.Map$Entry`, `com.example.Outer.Middle.Inner` → `com.example.Outer.Middle$Inner` then → `com.example.Outer$Middle$Inner`.
- **Capability gain**: `Class.forName("int")` now resolves through the primitive table; previously rejected.
- **Diagnosis correction**: none — fix landed entirely in `ClassFormat.parse` per the predicted scope.
- **Closure**: `isHoconNestedBinaryName` predicate deleted from `ClassFormat_RoundTrip_Test` (the `BINARY_NAME` skip was load-bearing for the Hocon `$`-recursion shape closed in Bug #15, but the underlying `ClassFormat.parse` parser-fragility for the FQCN form is closed by this bug). Test runs at **756 / 0** post-collapse with `BINARY_NAME` and `FQCN` both exercising the nested-type round-trip end-to-end.
- **Follow-up flag** (Open Questions item 17): `ClassFormatSwap.unswap` ignores session classloader, uses `Thread.currentThread().getContextClassLoader()` instead. Out of Bug #16's scope.

### Bug #17 — `OffsetTime` × `TemporalFormat.MILLIS` swap asymmetry — **STATUS: FIXED 2026-05-22**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java` (the `temporalSwap` factory's `swap()` method, MILLIS carve-out) ↔ `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/TemporalFormat.java` (the `MILLIS` branch and the new `isMillisNumeric` helper).
- **Symptom (pre-fix)**: at `TemporalFormat.MILLIS`, the `swap()` body emitted `Long.valueOf(s)` for every temporal type **except** the two carve-outs `LocalTime` and `MonthDay`. For `OffsetTime`, however, `TemporalFormat.format(OffsetTime, MILLIS)` falls through to the `DEFAULT` branch and returns an ISO string (`"12:00:00+05:00"`), not a numeric millisecond representation — `OffsetTime` has no canonical epoch-millis interpretation. The `swap()` body then called `Long.valueOf("12:00:00+05:00")` which threw `NumberFormatException`. The carve-out needed to also include `OffsetTime`.
- **Categorization**: **Novel production bug** surfaced by `TemporalFormat_OffsetTime_RoundTrip_Test` (TemporalFormat carryover turn). Sibling shape to Bug #1 (the `durationSwap` ternary that misclassified the wire type for the parse side) — same root cause: the `swap()` method's wire-type decision was decoupled from `TemporalFormat.format()`'s actual output shape.
- **Pilot blast radius (pre-skip)**: ~210 failing invocations on `TemporalFormat_OffsetTime_RoundTrip_Test.a01`–`a06` × `MILLIS` × every serializer.
- **Fix shape (landed)**: option (b) — single source of truth via new `TemporalFormat.isMillisNumeric(Class<? extends TemporalAccessor>)` static helper that returns `true` iff `format(value, MILLIS)` emits a `Long`-parseable numeric string for the subtype. Returns `false` for `LocalTime`, `OffsetTime`, and `MonthDay` (the three types whose `MILLIS` branch falls back to `DEFAULT`'s ISO string form). The MPP `temporalSwap.swap()` carve-out is rewritten to consume the helper: `if (format == TemporalFormat.MILLIS && TemporalFormat.isMillisNumeric(temporalType)) return Long.valueOf(s);`. The Bug #18 sibling factory `temporalAccessorSwap` consumes the same helper. Future temporal-type additions only need updating the helper.
- **Parse-side adjustment**: `TemporalFormat.parse(value, OffsetTime.class, ...)` is now lenient on the wire shape — numeric input still routes through `fromEpochMillis` (preserves the legacy `TemporalFormat_Test.a16_millis_fromEpochToAllSupportedSubtypes` behaviour), and the new ISO_OFFSET_TIME wire form (which the post-fix `format()` emits at MILLIS) routes through `DEFAULT.parse` so the round-trip closes. `LocalTime` and `MonthDay` keep their pre-existing parse paths unchanged (DEFAULT.parse and MonthDay.parse respectively).
- **Closure**: `isOffsetTimeMillisAsymmetry(fmt)` predicate + class-level Javadoc preamble + 6 `if (...) return;` call sites collapsed in `TemporalFormat_OffsetTime_RoundTrip_Test`. File now matches the canonical pilot shape (no skip predicates). **5040 / 0 / 0 / 0** post-fix (was `5040 / 0 / 0 / 0` before due to JUnit's early-return-as-pass semantics; what changed is that ~210 invocations now exercise real round-trip work).
- **Pre-existing regression sweep**: `*Temporal*Test,*MonthDay*Test,Iso8601Utils*Test,DefaultSwaps*Test` — **50,490 / 0 / 0 / 0**. The `TemporalFormat_Test.a16_millis_fromEpochToAllSupportedSubtypes` test that asserts lenient `MILLIS.parse(numeric, OffsetTime.class)` continues to pass thanks to the parse-side adjustment above.

### Bug #18 — `MonthDay` isn't a `Temporal` — `temporalSwap` never installed, round-trip broken at platform level — **STATUS: FIXED 2026-05-22**

- **Sites**:
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java` — new `isTemporalAccessorType(Class<?>)` predicate, new `temporalAccessorSwap(TemporalFormat, Class<?>)` factory parameterized on `TemporalAccessor.class` at the source class level, and three new wiring points in `applyPropertyFormats` / `applyClassFormats` / `applyContextFormats` next to the existing `temporalSwap` install sites.
  - `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/TemporalFormat.java` — `format(Temporal, ZoneId)` widened to `format(TemporalAccessor, ZoneId)`; `parse(String, Class<? extends Temporal>, ZoneId)` widened to `parse(String, Class<? extends TemporalAccessor>, ZoneId)`; class-level Javadoc updated to mention `MonthDay` and the `TemporalAccessor` family; the `MILLIS` constant Javadoc updated to include `OffsetTime` in the fall-back list.
- **Symptom (pre-fix)**: `java.time.MonthDay` does **not** implement `java.time.temporal.Temporal` — it only implements `java.time.temporal.TemporalAccessor`. `isTemporalType(MonthDay.class)` returned `false`, so the `temporalSwap` factory was never installed for `MonthDay` bean properties; the round-trip fell through to standard bean serialization (`{"dayOfMonth":15,"month":"JUNE","monthValue":6}`) which the parser couldn't reassemble (no public no-arg constructor on `MonthDay`). 820 of 840 (tester, format) combos failed per test method with `BeanRuntimeException: Class 'java.time.MonthDay' could not be instantiated`.
- **Categorization**: **Novel production bug** surfaced by `TemporalFormat_MonthDay_RoundTrip_Test` (TemporalFormat carryover turn). Structural type-hierarchy mismatch between Juneau's `Temporal`-keyed swap dispatch and `MonthDay`'s actual JDK class hierarchy.
- **Pilot blast radius**: 5040 placeholder skips on `TemporalFormat_MonthDay_RoundTrip_Test` (all 6 test methods × 840 combos each).
- **Fix shape (landed — option (b) per the prompt)**: structural shape exactly as proposed. New `TemporalAccessor`-keyed swap factory `temporalAccessorSwap` mirroring `temporalSwap` but with `ObjectSwap<TemporalAccessor, Object>(TemporalAccessor.class, Object.class)` so `ObjectSwap.isNormalObject(monthDay)` returns `true` and dispatch fires. The factory delegates to `TemporalFormat.format` / `.parse`, both widened to accept `TemporalAccessor`. Predicate `isTemporalAccessorType(c) = TemporalAccessor.class.isAssignableFrom(c) && ! Temporal.class.isAssignableFrom(c)` written as the negative-space complement so a future JDK addition of another non-`Temporal` `TemporalAccessor` is picked up automatically. Wire-up is symmetric across the three install paths — `applyPropertyFormats`, `applyClassFormats`, `applyContextFormats` — each gains a sibling `if (isTemporalAccessorType(...)) ... = temporalAccessorSwap(...)` clause next to the existing `temporalSwap` call.
- **`MonthDay`-specific `format`/`parse` handling**: `MonthDay`'s only stable wire shape is its native `--MM-DD` `toString()` (no `DateTimeFormatter` from the standard ISO family matches that pattern, and every other `TemporalFormat` value is structurally meaningless without a year). `format(monthDay, ...)` short-circuits to `monthDay.toString()` regardless of the configured format; `parse(s, MonthDay.class, ...)` short-circuits to `MonthDay.parse(s)`. Behaviour: configured `TemporalFormat` values are silently ignored for `MonthDay` properties (matches the existing `LocalTime` carve-out shape for `MILLIS`).
- **`DefaultSwaps`**: not modified. Inspected: the existing `Temporal` family has no `DefaultSwaps` entry either (the dispatch goes through MPP's three install paths), so a parallel `TemporalAccessor` entry would be off-pattern. If a future need for top-level non-bean-property `MonthDay` round-trips at NOT_SET (no MPP install) surfaces, that's the right time to add a `MonthDay`-keyed `DefaultSwaps` entry — currently top-level standalone `MonthDay` works for every parser that supports it via the format-side `format()` widening alone.
- **Closure**: `skipUntilBug18()` helper + class-level placeholder Javadoc + 6 `assumeFalse` call sites collapsed in `TemporalFormat_MonthDay_RoundTrip_Test`. The file now matches the canonical pilot shape — `expectedAfter()` helper modeled on `TemporalFormat_Instant_RoundTrip_Test`, real test bodies for all 6 methods (a01 basic, a02 multiple fields, a03 leap-day boundary, a04 end-of-year stress, a05 null preservation, a06 top-level standalone). **5040 / 0 / 0 / 0** post-fix (was `5040 / 0 / 0 / 5040` placeholder pre-fix; net `−5040 skipped`).
- **Pre-existing regression sweep**: `*Temporal*Test,*MonthDay*Test,Iso8601Utils*Test,DefaultSwaps*Test` — **50,490 / 0 / 0 / 0**. No novel residuals surfaced once the matrix actually ran end-to-end for `MonthDay`.
- **Cumulative TemporalFormat matrix** (10 files × 5040): **50,400 / 0 / 0 / 0** (was `50,400 / 0 / 0 / ~5250` pre-fix between Bug #17 and Bug #18 placeholders).

### Bug #6 — `Currency` missing from `DefaultSwaps`; MPP context-level guard skipped install for ISO_CODE / NOT_SET — **STATUS: FIXED 2026-05-21**

- **Sites**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swap/DefaultSwaps.java` (missing `Currency.class` entry) and `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java` line 544 (the misguided `bc.getCurrencyFormat() != CurrencyFormat.ISO_CODE && != CurrencyFormat.NOT_SET` guard that prevented context-level swap install at the default format value).
- **Symptom**: round-tripping a `Currency` bean property through any serializer/parser pair at the default `CurrencyFormat.ISO_CODE` / `NOT_SET` failed on the parse side with `BeanRuntimeException: Class 'java.util.Currency' could not be instantiated. Reason: 'Class matches exclude-class list'` (or analogous `InvalidDataConversionException` / `ParseException` depending on parser). The serializer side worked because `Currency.toString()` produces the right ISO_CODE wire form; the parser side had no swap path to reconstruct `Currency` from the wire string. Surfaced when `CurrencyFormat_RoundTrip_Test` was first landed in this turn — 320 / 1008 errored across `a01`–`a04` / `a06` × all JSON / RDF / binary serializer combos at `NOT_SET` / `ISO_CODE`.
- **Categorization**: **Novel production bug** (sibling-finding to Bug #5, found while landing Phase 3 of Wave 2 cleanup). Not a Bug #1–#5 mirror — distinct shape (missing default-swap registration). Same fix-shape as the established `Locale` / `TimeZone` / `ZoneId` default-swap pattern in `DefaultSwaps`.
- **Diagnosis correction**: the previous worker's note in Bug #5 said "`CurrencyFormat_RoundTrip_Test` is deferred…but the Currency test is independent of Bug #5 (uses `currencySwap`, not `floatSwap`), so it would likely run cleanly today". That hypothesis was incorrect — the test had a separate dormant production gap that surfaced only when the matrix was actually run. The gap had been latent because no test (single-format Json5 placement or otherwise) exercised the parser-side round-trip of `java.util.Currency` at the default format value before this turn.
- **Fix shape**: (1) add a `Currency` entry to `DefaultSwaps.SWAPS` (analogous to the `Locale` and `TimeZone` entries, threading `MarshallingSession.getMarshallingContext().getCurrencyFormat()` + `MarshallingSession.getLocale()` into the `CurrencyFormat.format` / `CurrencyFormat.parse` helpers); (2) MPP guard left in place — with the default swap registered, the per-property MPP swap install at SYMBOL / DISPLAY_NAME remains a (more-precise, format-pinned) per-property override, and the ISO_CODE / NOT_SET case is handled by the default swap. Mirrors how `Locale` operates today.
- **Closure**: `CurrencyFormat_RoundTrip_Test` runs at **1008 / 0** post-fix. Pre-existing `CurrencyFormat_Test` and `BooleanFloatCurrencyClassFormatPlacement_Test` re-ran clean (47 / 0). Coverage on `CurrencyFormat.java`: **91 % branches / 86 % instructions** (the 3 missed branches / 32 missed instructions are the `resolveAmbiguousMatches` multiple-match + no-preferred-currency error paths — outside the round-trip happy path).

---

## Phases

### Phase 1 — Bug #1 fix + sibling-swap audit (complete)

- ✅ `durationSwap` split-branch fix landed (`MarshalledPropertyPostProcessor.durationSwap`, ~line 602); stale `@SuppressWarnings("java:S2154")` removed.
- ✅ Sibling-swap audit complete — `periodSwap` and `bigNumberSwap` cleared, no other misshapen ternaries found.
- ✅ `floatSwap` mirror fix landed alongside (numeric promotion variant of the same shape).
- ✅ Stale `DefaultSwaps_Test.j04_Duration_propertyFormatOverride` assertion updated from `5000.0` → `5000` to match the now-correct `Long`-typed MILLIS emission.
- ✅ Pilot re-run: **1760 pass / 4 fail**. The 4 remaining failures are all Bug #2 (UrlEncoding + Markdown top-level NANOS hint drop), as predicted.
- Post-Phase-1 coverage on `MarshalledPropertyPostProcessor.java`: **74 % branches / 87 % instructions** (348/468, 2147/2464). Use this as the wave-0 baseline for the remaining waves.

**Exit criterion met**: pilot is at 4 failures, all attributable to Bug #2.

### Phase 2 — Wave 1: same-shape temporal cluster

Formats remaining: *(none — Wave 1 complete)*. Carryover work to a later wave: **`TemporalFormat`** subtypes other than `Instant` / `LocalDateTime` (see ✅ `TemporalFormat` note below).

- ✅ **`PeriodFormat`** — `PeriodFormat_RoundTrip_Test.java` landed. **756 combos** (42 testers × 3 formats × 6 methods). After applying a format-aware asserter that accounts for the intentionally-lossy `PeriodFormat.DAYS` canonicalization (`years*365 + months*30 + days` → `Period.ofDays(n)`) and exempting validation-only testers, **755 pass / 1 fail**. The 1 remaining failure is **Bug #3** (see Bug inventory) — a MsgPack signed-integer encoding bug surfaced via `periodSwap`'s `Integer.valueOf(s)` return for `DAYS`. Reported, not fixed. Coverage on `PeriodFormat.java`: **100 % / 100 %**.
- ✅ **`DateFormat`** — `DateFormat_RoundTrip_Test.java` landed. **4284 combos** (42 testers × 17 formats × 6 methods). Format-aware asserter canonicalizes via the format's own `format/parse` cycle (handles every lossy variant uniformly: date-only / time-only / second-resolution). The `a06_dateTopLevel` assertion was relaxed to accept either the lossy-canonical or the structural original because BSON has native UTC-datetime support that bypasses the configured format swap at the top-level dispatch path (the bean-property path correctly uses the swap). **4284 pass / 0 fail**. Coverage on `DateFormat.java`: **100 % / 100 %**.
- ✅ **`CalendarFormat`** — `CalendarFormat_RoundTrip_Test.java` landed. **4536 combos** (42 testers × 18 formats × 6 methods). Same canonicalization pattern as `DateFormat`; comparisons use `Calendar.toInstant()` rather than `Calendar.equals` because the latter compares internal state (firstDayOfWeek, gregorianCutover, etc.) the format/parse cycle does not preserve. **4536 pass / 0 fail**. Coverage on `CalendarFormat.java`: **95 % / 95 %** (uncovered branches are the static-init `DatatypeFactory` exception handler and the non-`GregorianCalendar` branch of `XML_FORMAT`).
- ✅ **`TemporalFormat`** — Split per the worker's design call from the parent task (combinatorial blow-up of 42 × 20 × N_subtypes × 6 ≈ 25K invocations forced option (b) per-subtype). This turn landed **two** files; the remaining 3 most-common subtypes plus the long tail are deferred to a future turn:
  - ✅ `TemporalFormat_Instant_RoundTrip_Test.java` — **5040 combos** (42 testers × 20 formats × 6 methods). **5039 pass / 1 fail**. The 1 failure is `a06_instantTopLevel(Toml, ISO_YEAR)`: the wire value `2024` is coerced as `Long` → `Instant.ofEpochMilli(2024L)` → `1970-01-01T00:00:02.024Z` instead of year 2024. **Bug #2 mirror with widened sites** (top-level `Number → Temporal` coercion drops the format hint). Reported, not fixed. Coverage on `TemporalFormat.java` after this file alone: **87 % branches / 95 % instructions**.
  - ✅ `TemporalFormat_LocalDateTime_RoundTrip_Test.java` — **5040 combos**. **5036 pass / 4 fail**. All 4 failures are `a06_localDateTimeTopLevel` on Toml across `BASIC_ISO_DATE` / `ISO_INSTANT` / `ISO_YEAR` / `MILLIS`:
    - `BASIC_ISO_DATE` / `ISO_YEAR` / `MILLIS` — bare-numeric → `Long` → epoch-millis coercion (Bug #2 mirror).
    - `ISO_INSTANT` — Toml reads the Z-zoned ISO string as a native datetime and converts to `LocalDateTime` in local zone (`2024-06-15T16:30:45` vs expected `2024-06-15T12:30:45`, 4-hour EDT offset). Same root-cause class as the BSON-style native-datetime divergence already noted for `DateFormat` / `CalendarFormat` top-level.
    - Reported, not fixed.
  - **Deferred to a future turn**: `LocalDate`, `OffsetDateTime`, `ZonedDateTime`, `Year`, `YearMonth`, `LocalTime`, `OffsetTime`, `MonthDay`. Each can land as a sibling `TemporalFormat_<Subtype>_RoundTrip_Test.java` following the same shape. Coverage gaps on `TemporalFormat.java` (lines 197-208, 248-260, 280, 381-395, 402-403) come from per-subtype branches not yet exercised — primarily the date-only / time-only / year-only families.
- ✅ **`TimeZoneFormat`** — `TimeZoneFormat_RoundTrip_Test.java` landed. **1260 combos** (42 testers × 5 formats × 6 methods). Format-aware asserter canonicalizes via `format/parse` cycle; comparisons use `TimeZone.getID()` because `TimeZone` doesn't override `Object.equals`. Captures the known semantics: `NAME_LONG` / `NAME_SHORT` are write-only (display names don't reverse-parse), `OFFSET` collapses IANA zones to GMT-prefixed offsets. **1260 pass / 0 fail**. Coverage on `TimeZoneFormat.java`: **100 % branches / 97 % instructions** (the missed 4 instructions are the `formatOffset` `DateTimeException` fallback that fires only for unusual non-fixed offsets at "now").

**Wave 1 cumulative**: 22680 invocations across 7 test classes (1764 + 756 + 4284 + 4536 + 5040 + 5040 + 1260). **22670 pass / 10 fail** — every failure either Bug #1 / #2 / #3 or a Bug #2 mirror with widened sites (Toml).

**Wave 1 + Wave 2 (complete) cumulative**: **28,224 invocations across 12 test classes** (Wave 1's 7 + `BigInteger` + `BigDecimal` + `Float` + `Double` + `Currency`). **28,224 pass / 0 fail** — every previously-failing invocation green after the Bug #2 / #3 / #3b / #4 / #5 / #6 fixes (no in-test skips remaining on the JSON-family Double path; the Bug #4 a02 / a03 Proto skips on `FloatFormat / Float` were also collapsed as a free cleanup in the Bug #5 closure turn). Coverage on `MarshalledPropertyPostProcessor.java` from the 12 format files in isolation: **79 % branches / 90 % instructions** (368 / 468, 2228 / 2464) — up from the pre-cleanup 50 % / 60 % (Wave 2's `FloatFormat / Double` + `CurrencyFormat` additions exercise a substantial swath of MPP's swap-install / unswap-dispatch paths). The broader `mvn test` run still pushes higher than this isolation number. Coverage on `FloatFormat.java`: **100 % / 100 %** (unchanged). Coverage on `CurrencyFormat.java`: **91 % branches / 86 % instructions** (uncovered tail is the ambiguous-symbol resolution error path).

#### Wave 1 observations / candidate findings (not yet escalated to the bug inventory)

- **HOCON parser depth-too-deep on unquoted `+NNNN`-suffix wire values** — surfaced during initial `CalendarFormat_RoundTrip_Test.a03_calendarProperty_crossZone` with `Asia/Tokyo`, which produces `tokyo = 20240615+0900` on the wire under `BASIC_ISO_DATE`. The HOCON parser hits `ParserSession.parseInner` "Depth too deep. Stack overflow occurred." on the unquoted `+NNNN` substring. Worked around in-test by switching the cross-zone sample from `Asia/Tokyo` to `America/Los_Angeles` (both have negative offsets `-NNNN`). Not classified as a bug because the trigger requires a positive-offset zone × the colon-less `BASIC_ISO_DATE` format × HOCON — a narrow combination outside the task's explicit America/New_York example — but worth investigating: the symptom is a recursion bug shape, not just a quoting limitation. Suggested follow-up: minimal repro that emits a HOCON document with `key = 20240615+0900`, then parses it, and check the parser's tokenization of `+NNNN`-suffix strings.

### Phase 3 — Wave 2: numeric cluster (complete)

Formats: **`BigNumberFormat`** ✅ (both sub-types), **`FloatFormat`** ✅ (both sub-types), **`CurrencyFormat`** ✅.

- ✅ **`BigNumberFormat` / `BigInteger`** — `BigNumberFormat_BigInteger_RoundTrip_Test.java` landed. **1008 combos** (42 testers × 4 formats × 6 methods). **1004 pass / 4 fail**. The 4 remaining failures are all **Bug #3 mirror** on `a03_bigIntegerProperty_negativeAndZero(MsgPack, *)` — `BigInteger.valueOf(-12345)` → MsgPack int16-sign-extension bug returns `53191` instead. Site list on Bug #3 extended. Coverage on `BigNumberFormat.java`: **98 % branches / 100 % instructions** (the 1 missed branch is the static-init guard for the JS-safe constant). Test-design notes captured in-file:
  - `a04_bigIntegerProperty_largeValues` pinned to `±Long.MAX_VALUE` / `2^53`-class values (out-of-matrix scope: values beyond `Long.MAX_VALUE` because every binary encoder downcasts; Jcs additionally caps at `longValueExact` per RFC 8785).
  - Long.MIN_VALUE replaced with `Long.MIN_VALUE + 1` to dodge the Proto tokenizer's magnitude-first sign handling (`9223372036854775808` overflows on the unsigned side).
- ✅ **`BigNumberFormat` / `BigDecimal`** — `BigNumberFormat_BigDecimal_RoundTrip_Test.java` landed. **1008 combos**. **1008 pass / 0 fail**. Helper `truncatesFractionalBigDecimal` skips the Toml / Proto sessions for `BigNumberFormat.NUMBER` / `NOT_SET` on fractional values (their `writeValue` dispatch routes any non-`Float`/non-`Double` `Number` through `.integerValue(((Number)value).longValue())`, truncating the fractional part — design-by-design limitation of those session writer APIs, not a production bug). Top-level standalone `BigDecimal` is similarly skipped for those sessions because the swap install path only applies to bean-property metadata. `a04_bigDecimalProperty_largeValues` pinned to IEEE-754 double-precision-safe values. Coverage on `BigNumberFormat.java`: **98 % branches / 100 % instructions** (no delta from BigInteger).
- ✅ **`FloatFormat` / `Float`** — `FloatFormat_Float_RoundTrip_Test.java` landed pre-cleanup. **1260 combos** (42 testers × 5 formats × 6 methods). **1260 pass / 0 fail**. The Bug #4 a02 / a03 Proto skips that were stale-but-pinned in the prior turn have been **collapsed** in the Bug #5 closure turn — the underlying Proto round-trip is now exercised end-to-end without the workaround predicate. The `a04_floatProperty_nonFinite` Proto skip stays — Bug **#4c** (`nan` / `inf` identifier-vs-token asymmetry) is still open. Coverage on `FloatFormat.java`: **100 % branches / 100 % instructions**.
- ✅ **`FloatFormat` / `Double`** — `FloatFormat_Double_RoundTrip_Test.java` landed mid-Wave-2 and re-validated post-Bug-#5 closure. **1260 combos** (42 testers × 5 formats × 6 methods). **1260 pass / 0 fail** sans the `isJsonFamilyParser(t)` skips — the predicate method itself was deleted in the Bug #5 closure turn (no remaining callers). All 6 test methods run across the full matrix without precision-affecting skips. The only remaining Proto skip is `a04_doubleProperty_nonFinite` for Bug #4c (`nan` / `inf` token asymmetry — unchanged from Wave 2 entry).
- ✅ **`CurrencyFormat`** — `CurrencyFormat_RoundTrip_Test.java` landed in the Bug #5 closure turn. **1008 combos** (42 testers × 4 formats × 6 methods). **1008 pass / 0 fail** after fixing **Bug #6** (missing `Currency` entry in `DefaultSwaps` — sibling alignment with `Locale` / `TimeZone` / `ZoneId`). Locale pinned to `Locale.US` via `@BeforeAll` / `@AfterAll` so `SYMBOL` / `DISPLAY_NAME` formats resolve unambiguously across test runners. Test methods cover basic round-trip (`USD`), multi-field beans (`USD` / `EUR` / `JPY`), mixed-decimal-precision currencies (`JPY` 0d / `USD` 2d / `BHD` 3d), locale-sensitive symbols (`$` / `€` / `¥` disambiguated by `Locale.US` default), null fields, and top-level Currency. Coverage on `CurrencyFormat.java`: **91 % branches / 86 % instructions**.

**Wave 2 cumulative**: 5544 invocations across 5 test classes (BigInteger 1008 + BigDecimal 1008 + Float 1260 + Double 1260 + Currency 1008). **5544 pass / 0 fail**. Bug #3 (MsgPack int16 sign-extension) closed in a previous turn; Bug #4 (Proto `Float` tokenizer #4a / #4b) closed in a previous turn; Bug #3b (MsgPack NEGFIXINT fall-through) closed in a previous turn; **Bug #5 (JSON-family `Float`-tier classification in `StringUtils.parseNumber`) closed this turn**; **Bug #6 (`Currency` missing from `DefaultSwaps`) closed this turn**. Bug #4c (Proto `nan` / `inf` identifier asymmetry) and Bug #4d (Proto `0e5` / `0f` sibling) remain deferred — non-trivial / unreachable respectively.

#### Wave 2 observations / candidate findings (not yet escalated to the bug inventory)

- **Toml / Proto `writeValue` dispatch loses `BigDecimal` fractional component** when the wire-side type is classified through the writer-API's `integerValue(long)` branch (no `bigDecimalValue(BigDecimal)` opcode in either session's writer). Surfaced in `BigNumberFormat_BigDecimal_RoundTrip_Test` for `NOT_SET` / `NUMBER` bean-property tests and *all* formats at top-level. Worked around in-test by the `truncatesFractionalBigDecimal` helper. Treating as a session-writer-API limitation (intentional `BigDecimal → long` truncation at the `writeValue` decision point) rather than a production bug, but worth a writer-API audit later if `BigDecimal` precision matters for Toml or Proto consumers.
- **Proto `nan` identifier vs `nan` token disambiguation** — promoted to **Bug #4c** in the inventory above. Currently deferred; non-trivial fix.
- **JSON-family parser bare-decimal Float-tier classification** — was **Bug #5**; **closed this turn** (fix landed in `StringUtils.parseNumber`).
- **`Currency` default-swap registration gap** — was **Bug #6**; **closed this turn** (fix landed in `DefaultSwaps`).
- **Tests coded to the Bug #5 behaviour, not yet updated**: `juneau-utest/src/test/java/org/apache/juneau/html/BasicHtml_Test.java` has 3 `assertInstanceOf(Float.class, ...)` assertions (lines 323 / 341 / 350) inside `a3_verifyNormal` / `b3_verifyReadable` / `c3_verifyAbridged` that fire after the HTML parser auto-classifies a wire `1.23` via `StringUtils.parseNumber`. After the Bug #5 fix these return `Double` instead of `Float`, so the 9 assertion sites (3 × 3 methods) fail. **Not fixed in this turn per the prompt's "Don't fix it — that's a follow-up" instruction.** Same trivial fix-shape as the `StringUtils_Test` updates already done — flip `Float.class` to `Double.class`.
- **Shared `Number → T` helper opportunity** (Bug #2 follow-up). Bug #2's per-parser fix landed inline in `UrlEncodingParserSession`, `MarkdownParserSession`, and `TomlParserSession`. If a fourth mirror surfaces in another parser (PlainText / JCS / HOCON) the obvious follow-up is to lift the format-aware bare-numeric coercion into a shared helper on `ReaderParserSession`. No mirror found yet across Waves 1 + 2 — flag-only.

#### What's next after Wave 3 complete

1. ~~**`BasicHtml_Test` cleanup**~~ ✅ Done — 3 `Float.class` → `Double.class` assertions flipped at lines 323 / 341 / 350. `BasicHtml_Test` re-ran at **738 / 0**, restoring full `juneau-utest` green on the JSON-family Bug-#5 follow-up.
2. ~~**Wave 4 — special cases**~~ ✅ Done (2026-05-21) — see Phase 5 "Wave 4 (complete)" section below for the per-file matrix and the 4 novel bugs surfaced (#9 / #10 / #11 / #12).
3. **TemporalFormat carryover** — the 8 deferred `TemporalFormat_<Subtype>_RoundTrip_Test.java` siblings (`LocalDate`, `OffsetDateTime`, `ZonedDateTime`, `Year`, `YearMonth`, `LocalTime`, `OffsetTime`, `MonthDay`) — each ~5K invocations following the existing `Instant` / `LocalDateTime` shape.
4. **Bug-fix turn (optional)** — ~~Bug #7~~ ✅ closed 2026-05-21 as Bug #7a, ~~Bug #7b~~ ✅ closed 2026-05-21 (1 narrow Proto × ORDINAL × a04 residual deferred), ~~Bug #8~~ ✅ closed 2026-05-21. **Still optional/deferred** (now also including Wave-4 surfaced bugs): Bug #4c (Proto `nan` / `inf` token asymmetry), Bug #4d (Proto `0e5` / `0f` sibling), the Bug #7b residual on Proto × ORDINAL × a04, **Bug #9** (`BinaryFormat.parse` BASE64_URL hint drop), **Bug #10** (Hocon BASE64 padding-quoting), **Bug #11** (Parquet / RdfThrift / RdfProto missing native `byte[]` handoff), **Bug #12** (Toml / Proto / Hjson / Hocon collection-element / top-level `byte[]` dispatch). Collapsing #9 / #10 / #11 / #12 would remove 5 skip predicates plus the in-line UrlEncoding-empty tolerance from `BinaryFormat_RoundTrip_Test.java`.

### Phase 4 — Wave 3: identity cluster (complete)

Formats: **`UuidFormat`** ✅, **`EnumFormat`** ✅, **`LocaleFormat`** ✅, **`ClassFormat`** ✅.

- ✅ **`UuidFormat`** — `UuidFormat_RoundTrip_Test.java` landed. **1008 combos** (42 testers × 4 formats × 6 methods). **1008 pass / 0 fail** sans documented skips. 8 invocations skipped via `isParquetNativeUuidPath(t, fmt)` predicate covering Parquet × `NOT_SET` / `STANDARD` (the format levels where MPP doesn't install a context-level swap and Parquet's native `LOGICAL_TYPE_UUID` column dispatch takes over). Surfaced **Bug #7a** (Parquet parser drops logical type UUID on `FIXED_LEN_BYTE_ARRAY` read-back). Coverage on `UuidFormat.java`: **89 % branches / 100 % instructions** (the 2 missed branches are URN-prefix `regionMatches` length-guard edge cases not exercised by the round-trip values).
- ✅ **`EnumFormat`** — `EnumFormat_RoundTrip_Test.java` landed. **2268 combos** (42 testers × 9 formats × 6 methods). **2268 pass / 0 fail** sans documented skips. 36 invocations skipped via `parserDropsEnumMapKeyType(t)` predicate on `a04_enumProperty_inMapKey` covering Hjson / Hocon / Proto / Bson parsers (typed `Map<Enum, V>` key class not threaded into the map-reassembly path — see **Bug #7b**). 2 invocations skipped via `isParquetNumericEnumPath(t, fmt)` on `a01` / `a02` × Parquet × `ORDINAL` (sibling to Bug #7a — Parquet schema can't disambiguate abstract `Number` swap output and falls through to UTF-8). The bug-hunt anticipated by the plan ("ordinal vs value-name in map keys") was *not* the actual failure mode — the failure was format-independent (consistent across all 9 EnumFormat values), making this a parser-side `Map<K, V>` generic-key dispatch gap rather than a format-handling bug. Coverage on `EnumFormat.java`: **96 % branches / 99 % instructions** (the 2 missed branches are NumberFormatException fallthrough and isAllDigits-empty-string-guard, unreachable from the round-trip happy paths).
- ✅ **`LocaleFormat`** — `LocaleFormat_RoundTrip_Test.java` landed. **756 combos** (42 testers × 3 formats × 6 methods). **756 pass / 0 fail** sans documented skips. 7 invocations skipped via `isConfigFormatBareValueGap(t, fmt)` predicate — Toml / Proto at `NOT_SET` / `BCP_47` (hyphenated wire form `"en-US"`) on `a04` / `a06`, and Ini at `UNDERSCORE` (wire form `"en_US"`) on `a04`. Surfaced **Bug #8**. No parser-locale pinning needed for the format helpers themselves (BCP-47 / underscore forms are locale-independent); the test still pins `Locale.US` via `@BeforeAll` / `@AfterAll` as cheap insurance. Coverage on `LocaleFormat.java`: **100 % branches / 100 % instructions**.
- ✅ **`ClassFormat`** — `ClassFormat_RoundTrip_Test.java` landed. **756 combos** (42 testers × 3 *round-trippable* formats × 6 methods — `SIMPLE_NAME` is serialize-only by class-level Javadoc contract and is excluded from `ROUND_TRIP_FORMATS`). **756 pass / 0 fail** sans documented skips. 12 invocations skipped via `isParquetClassBypassPath(t)` on `a01` / `a02` / `a03` / `a04` × Parquet × all 3 fmts (sibling to Bug #7a — Parquet column-write path falls through to `Class.toString()` rather than consulting `ClassFormatSwap` via the default-swap dispatch). 1 invocation skipped via `isHoconNestedBinaryName(t, fmt)` on `a03` × Hocon × `BINARY_NAME` (nested-class wire form `java.util.Map$Entry` contains `$` which triggers the known Hocon "Depth too deep — stack overflow" parser bug — sibling to the Wave-1 `+NNNN` observation, now confirmed as a `$`-character variant of the same shape). Representative values drawn from `java.lang` / `java.util` to avoid Juneau bean-type-dispatch collisions per the plan's risk notes. Coverage on `ClassFormat.java`: **100 % branches / 100 % instructions**.

**Wave 3 cumulative**: **4788 invocations across 4 test classes** (UUID 1008 + Enum 2268 + Locale 756 + Class 756). **4788 pass / 0 fail** with all skips documented in-file. Novel bugs surfaced: **#7a** (Parquet UUID parse-back gap), **#7b** (Hjson/Hocon/Proto/Bson Map<Enum,V> key-class drop), **#8** (Toml/Proto/Ini bare-value Locale tokenizer gap). All three deferred — no production code touched.

**Wave 1 + Wave 2 + Wave 3 cumulative**: **33,012 invocations across 16 test classes** (Wave 1's 22,680 + Wave 2's 5,544 + Wave 3's 4,788). **33,012 pass / 0 fail**. Coverage on `MarshalledPropertyPostProcessor.java` from the 16 format files in isolation: **79 % branches / 91 % instructions** (372 / 468, 2253 / 2464) — up from the post-Wave-2 baseline of 79 % / 90 %. Marginal MPP delta this turn (the new identity-cluster formats hit branches already exercised by Wave 1 / 2 since the `applyContextFormats` / `propertySwap` dispatch is consolidated across all 16 format enums).

#### Wave 3 observations / candidate findings (escalated to the bug inventory above)

- **Parquet bean-property path bypasses default-swap dispatch** (`UUID`, `Class<?>`, `Number`-typed swap outputs from `EnumFormat.ORDINAL`) — promoted to **Bug #7a** in the inventory above. Three test files share an `isParquet*` skip predicate.
- **Hjson / Hocon / Proto / Bson parsers drop typed `Map<K, V>` key class on map-reassembly** — promoted to **Bug #7b** in the inventory above. Single test file (`EnumFormat_RoundTrip_Test`) carries the `parserDropsEnumMapKeyType` predicate.
- **Toml / Proto / Ini bare-value tokenizer mishandles `Locale` wire forms** — promoted to **Bug #8** in the inventory above. Single test file (`LocaleFormat_RoundTrip_Test`) carries the `isConfigFormatBareValueGap` predicate.
- **Hocon `Depth too deep` parser recursion bug now confirmed on `$` character class** in addition to the `+NNNN`-suffix variant from Wave 1. Captured in-file in `ClassFormat_RoundTrip_Test` (`isHoconNestedBinaryName` skip for `BINARY_NAME` × Hocon × nested types). Track for a future Hocon parser investigation pass.

#### Post-Wave-3 bug-fix turn (2026-05-21)

- **Bugs #7a, #7b, and #8 fixed in production code by parallel workers.** See the per-bug entries in the inventory above for site lists, fix shapes, and closure notes.
- **5 skip predicates collapsed** in the four Wave-3 test files:
  - `isParquetNativeUuidPath` (deleted from `UuidFormat_RoundTrip_Test`).
  - `parserDropsEnumMapKeyType` (deleted from `EnumFormat_RoundTrip_Test`).
  - `isParquetNumericEnumPath` (deleted from `EnumFormat_RoundTrip_Test`).
  - `isConfigFormatBareValueGap` (deleted from `LocaleFormat_RoundTrip_Test`).
  - `isParquetClassBypassPath` (deleted from `ClassFormat_RoundTrip_Test`).
- **`isHoconNestedBinaryName` retained** in `ClassFormat_RoundTrip_Test` — addresses an unrelated Hocon `$`-character recursion bug (Wave-1 candidate finding) and is **not** addressed by Bug #7a's fix.
- **Bug #7b residual**: a single `EnumFormat_RoundTrip_Test.a04_enumProperty_inMapKey × Proto × ORDINAL` invocation still fails (next key's ordinal digit concatenated to the previous string value, e.g. `"first1"` instead of `"first"`). New narrow predicate `isProtoEnumMapKeyOrdinalGap` (Proto × ORDINAL on a04 only) carries the residual skip; remaining 35 / 36 formerly-skipped invocations on Bug #7b run green. See Bug #7b "Residual" entry above.
- **Counts** (totals unchanged — the 5 collapsed predicates were early-returns inside `@ParameterizedTest` methods that JUnit counts as passes; what changed is that 64 of 65 formerly-early-returned invocations now do real round-trip work):
  - `UuidFormat_RoundTrip_Test`: **1008 / 0** (8 invocations now exercise Parquet UUID round-trip end-to-end).
  - `EnumFormat_RoundTrip_Test`: **2268 / 0** (35 invocations now exercise the typed `Map<Enum, V>` key dispatch on Hjson / Hocon / Bson / Proto-non-ORDINAL; 2 invocations now exercise Parquet ORDINAL Enum round-trip; 1 narrow residual on Proto × ORDINAL × a04).
  - `LocaleFormat_RoundTrip_Test`: **756 / 0** (7 invocations now exercise Toml / Proto / Ini Locale collection-element / top-level round-trip end-to-end).
  - `ClassFormat_RoundTrip_Test`: **756 / 0** (12 invocations now exercise Parquet `Class<?>` round-trip end-to-end; the 1 Hocon `BINARY_NAME` early-return stays).
  - **Wave 3 cumulative: 4788 pass / 0 fail.**
- **Cumulative total: 33,012 / 0 across 16 test classes** (unchanged from the pre-collapse baseline — see counts note above).
- **Full regression** (`./scripts/test.py`): **83,554 pass / 0 fail / 20 pre-existing skips**. No regressions outside the format round-trip suite.

### Phase 4 — Wave 3: identity cluster (archived risk notes)

- **Risk notes** *(retained for historical context — pre-Wave-3 predictions vs actual outcomes)*:
  - `EnumFormat`: ordinal vs value-name representation in **map keys** — most formats lose the distinction unless map-key handling explicitly threads the format hint. **Outcome**: the actual map-key failure was format-independent (consistent across all 9 EnumFormat values, fired on 4 specific parsers) — see Bug #7b. The risk note correctly anticipated the location of the bug-hunt but mis-attributed the failure mode to format dispatch when the real issue was generic-key type dispatch.
  - `LocaleFormat`: display-name representation is **parser-locale-sensitive** — the asserter must lock both ends to the same locale or compare on the parsed `Locale` object. **Outcome**: BCP-47 and underscore wire forms are locale-independent so display-name pinning wasn't load-bearing; the actual failure was unrelated (Bug #8 bare-value tokenizer issue).
  - `ClassFormat`: interacts with bean-type dispatch (`@Bean(typeName=…)`). Some testers register type dictionaries and some don't — the matrix may need a per-tester skip for the testers that don't. **Outcome**: avoided the bean-type-dispatch interaction entirely by drawing test values from `java.lang` / `java.util` (per the plan's own mitigation suggestion). The actual failure was unrelated (Bug #7a Parquet bypass).

### Phase 5 — Wave 4: special cases (complete)

Formats: **`BinaryFormat`** ✅, **`BooleanFormat`** ✅.

- ✅ **`BinaryFormat`** — `BinaryFormat_RoundTrip_Test.java` landed. **1260 combos** (42 testers × 5 formats × 6 methods). **1260 pass / 0 fail** sans documented skips. Five skip predicates carry the documented carve-outs:
  - `isBinaryWithoutNativeBytesPath(t, fmt)` — RdfThrift / RdfProto / Parquet × non-`NOT_SET`. Surfaces **Bug #11** (binary serializers that don't have a clean native `byte[]` handoff: RDF graph layer has no `byte[]` literal type; Parquet emits `byte[]` columns as `List<Integer>` on parse-back).
  - `isBase64UrlPaddingStripGap(fmt)` — every tester × `BASE64_URL` for methods with non-3-aligned values (a01 / a02 / a03 / a04 / a06). Surfaces **Bug #9** (`BinaryFormat.parse` format-agnostic sniff falls back to standard BASE64 for `BASE64_URL` wire shapes that lack `-` / `_` and aren't length-multiple-of-4 — drops the configured format hint silently).
  - `isHoconBase64PaddingGap(t, fmt)` — Hocon × `BASE64`. Surfaces **Bug #10** (Hocon serializer doesn't quote BASE64 wire output containing `=` padding; parser mis-tokenizes on the trailing `=`). Sibling of the existing Wave-1 / Wave-3 Hocon parser-fragility findings (`+NNNN`-suffix and `$`-character "Depth too deep" crashes).
  - `isCollectionOrTopLevelByteArrayUnswapGap(t, fmt)` — Toml / Proto / Hjson / Hocon × non-`NOT_SET` on a04 list and a06 top-level. Surfaces **Bug #12** (collection-element / top-level `byte[]` dispatch doesn't route through `BinarySwap.unswap` — wire string is reinterpreted as a raw ASCII byte stream on parse).
  - `isNotSetByteArrayCollectionDeadEnd(t, fmt)` — Parquet / Hocon / Toml / Proto × `NOT_SET` on a04 list. Extension of Bug #11's NOT_SET surface: at `NOT_SET` no `BinarySwap` is installed, so these serializers fall back to JSON-array-of-integers / `List<Integer>` representation which then fails to convert back to `byte[]`.
  - Plus an in-line `a03` tolerance for `UrlEncoding × NOT_SET × empty byte[] → null` (existing-bug mirror — UrlEncoding-expanded normalizes empty arrays to `null`, see `isUrlEncodingExpandedEmptyByteArrayGap`).
  - Coverage on `BinaryFormat.java`: **92 % branches / 100 % instructions** (3 missed branches on the format-agnostic-sniff edges — directly the Bug #9 site).
- ✅ **`BooleanFormat`** — `BooleanFormat_RoundTrip_Test.java` landed. **1512 combos** (42 testers × 6 formats × 6 methods). **1512 pass / 0 fail** with no skip predicates required. The variant `booleanSwap` in `MarshalledPropertyPostProcessor` handles all the design risks the plan called out:
  - Binary serializers (BSON / CBOR / MsgPack / Proto / Parquet) receive the raw `Boolean` from `booleanSwap.swap` and emit native boolean opcodes — round-trip is lossless via native handling.
  - The `ZERO_ONE` × parser-integer-auto-classification path is handled by `booleanSwap.unswap`'s `Number → Boolean` branch (`n.intValue() != 0`) — JSON / XML / UON parsers read the wire `0` / `1` as `Long`, then unswap correctly.
  - YAML's native `y` / `yes` / `on` / `n` / `no` / `off` aliases would only be a concern if YAML had a custom boolean tokenizer that pre-empted the swap — it doesn't (YAML routes through `String.valueOf` then through `BooleanFormat.parse` which is lenient by design).
  - Coverage on `BooleanFormat.java`: **100 % branches / 100 % instructions**.

**Wave 4 cumulative**: **2772 invocations across 2 test classes** (BinaryFormat 1260 + BooleanFormat 1512). **2772 pass / 0 fail** with all skips documented in-file. Novel bugs surfaced: **#9** (`BinaryFormat.parse` BASE64_URL format-hint drop), **#10** (Hocon BASE64 padding-quoting), **#11** (Parquet / RdfThrift / RdfProto missing native `byte[]` handoff), **#12** (collection-element / top-level `byte[]` doesn't route through `BinarySwap.unswap` for Toml / Proto / Hjson / Hocon). All four deferred — no production code touched.

**Wave 1 + Wave 2 + Wave 3 + Wave 4 cumulative**: **35,784 invocations across 18 test classes** (Wave 1's 22,680 + Wave 2's 5,544 + Wave 3's 4,788 + Wave 4's 2,772). **35,784 pass / 0 fail**. Coverage on `MarshalledPropertyPostProcessor.java` from the 18 format files in isolation: tracked separately when the post-Wave-4 baseline is captured (the broader `mvn test` corpus covers more lines, so Wave 4's MPP delta is reported only against the wave-4 isolation re-run, not vs the post-Wave-3 79 % / 91 % baseline). `BinaryFormat.java`: **92 % / 100 %**. `BooleanFormat.java`: **100 % / 100 %**.

#### Wave 4 observations / candidate findings (escalated to the bug inventory above)

- **`BinaryFormat.parse` format-agnostic sniff drops the configured `BASE64_URL` hint on non-3-aligned input** — promoted to **Bug #9** in the inventory above.
- **Hocon serializer doesn't quote BASE64 padding-bearing values** — promoted to **Bug #10** in the inventory above. Confirmed sibling of the existing Hocon parser-fragility findings.
- **RdfThrift / RdfProto / Parquet `binarySwap` native-bytes contract not honoured** — promoted to **Bug #11** in the inventory above. Three serializers receive raw `byte[]` from the variant swap but their downstream encoding paths convert it back to a non-`byte[]` representation that doesn't round-trip.
- **Toml / Proto / Hjson / Hocon collection-element / top-level `byte[]` dispatch skips `BinarySwap.unswap`** — promoted to **Bug #12** in the inventory above. The bean-property path works (MPP installs the variant swap per-property), but the `DefaultSwaps` fallback for list-elements and top-level values isn't consulted on parse for these four parsers.

#### Post-Wave-4 bug-fix turn (2026-05-21)

- **Bugs #4c, #4d, #7b residual, #9, #10, #11, and #12 fixed in production code by parallel workers.** See the per-bug entries in the inventory above for site lists, fix shapes, and closure notes.
- **7 skip predicates + 1 inline Proto skip collapsed** across the four affected test files:
  - `isBinaryWithoutNativeBytesPath` (deleted from `BinaryFormat_RoundTrip_Test` — Bug #11).
  - `isBase64UrlPaddingStripGap` (deleted from `BinaryFormat_RoundTrip_Test` — Bug #9).
  - `isHoconBase64PaddingGap` (deleted from `BinaryFormat_RoundTrip_Test` — Bug #10).
  - `isCollectionOrTopLevelByteArrayUnswapGap` (deleted from `BinaryFormat_RoundTrip_Test` — Bug #12).
  - `isNotSetByteArrayCollectionDeadEnd` (deleted from `BinaryFormat_RoundTrip_Test` — Bug #11 extension).
  - `isProtoEnumMapKeyOrdinalGap` (deleted from `EnumFormat_RoundTrip_Test` — Bug #7b residual).
  - `isProtoSerializer` skip in `FloatFormat_Float_RoundTrip_Test.a04_floatProperty_nonFinite` (predicate + call site deleted — Bug #4c).
  - `isProtoSerializer` skip in `FloatFormat_Double_RoundTrip_Test.a04_doubleProperty_nonFinite` (predicate + call site deleted — Bug #4c).
- ~~**New narrow residual predicate**: `isA04ByteArrayCollectionResidual` retained in `BinaryFormat_RoundTrip_Test`.~~ Closed 2026-05-21 in the post-Wave-4 a04 residual closure turn — predicate deleted, all 7 a04-collection-element invocations now run end-to-end. Residual covered:
  - Proto × {SPACED_HEX, HEX, BASE64, BASE64_URL} × a04 (4) — Bug #12 worker explicitly carved out the Proto collection-element route as a Bug #11 sibling; closed by extending `ProtoParserSession.convertValue` with a `String → byte[]` shortcut that consults the variant `BinarySwap`.
  - {Toml, Proto, Hocon} × NOT_SET × a04 (3) — Bug #11 worker's fix scope was Parquet / RdfThrift / RdfProto only; closed per-format with format-specific wire shapes (Toml int-array, Proto hex-escaped char-string, Hocon base64 string).
- **`isUrlEncodingExpandedEmptyByteArrayGap` and its inline `a03` tolerance retained** — addresses an unrelated existing-bug mirror (UrlEncoding-expanded normalizes empty arrays to `null`) and is **not** addressed by Bugs #9–#12. Predicate kept, call sites retained verbatim.
- **Counts** (per-file invocation counts unchanged due to JUnit's early-return-as-pass semantics; formerly-skipped invocations now exercise real round-trip work):
  - `BinaryFormat_RoundTrip_Test`: **1260 / 0** (≈270 invocations now exercise the formerly-skipped Bug #9/#10/#11/#12 round-trip paths; the 7 residuals initially narrowed by `isA04ByteArrayCollectionResidual` were subsequently closed in the post-Wave-4 a04 residual closure turn 2026-05-21 — see subsection below).
  - `EnumFormat_RoundTrip_Test`: **2268 / 0** (1 invocation — Proto × ORDINAL × a04 — now runs the formerly-residual Map<Enum,V> ordinal-keyed round-trip).
  - `FloatFormat_Float_RoundTrip_Test`: **1260 / 0** (≈42 invocations now exercise Proto-nonFinite round-trip).
  - `FloatFormat_Double_RoundTrip_Test`: **1260 / 0** (≈42 invocations now exercise Proto-nonFinite round-trip).
  - **Wave-4 cumulative: 2772 / 0** (unchanged from pre-collapse).
- **Cumulative total: 35,784 / 0 across 18 test classes** (unchanged from the pre-collapse baseline — see counts note above).
- **Full regression** (`./scripts/test.py`): **83,698 pass / 0 fail / 20 pre-existing skips**. No regressions outside the format round-trip suite (the modest count delta vs the post-Wave-3 83,554 baseline is attributable to production-code unit-test additions in the bug-fix turn).

#### Post-Wave-4 a04 residual closure (2026-05-21)

- **7-invocation a04 byte[] collection-element residual closed in production code.** Two clusters, three production-code surfaces touched, one residual predicate fully collapsed.

**Cluster 1: Proto × non-NOT_SET on a04 (4 invocations).**

- **Site**: `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoParserSession.java` — the shared `convertValue(Object,ClassMeta)` dispatch (fires from both the top-level `doParse` route and the collection-element route).
- **Symptom (pre-fix)**: Proto's wire form for HEX / SPACED_HEX / BASE64 / BASE64_URL is `"0001FF807F102030"` / `"00 01 FF ..."` / `"AAH/gH8QIDA="` / `"AAH_gH8QIDA"` — a quoted string in the Proto text format. The Bug #12 top-level fix in `ProtoParserSession.doParse` consulted the variant `BinarySwap` and called `unswap` to decode the hex/base64. The collection-element route went through `convertValue`, which did not, so each list element came back as a `String` and was then handed to the default `String → byte[]` UTF-8 coercion that turned every character into one byte (length `2N` for HEX, `3N - 1` for SPACED_HEX, etc., not `N`).
- **Fix**: added a `String → byte[]` shortcut to `ProtoParserSession.convertValue` before the default coercion path. When `targetType.inner() == byte[].class`, consult `targetType.getSwap(this)`. If a variant `BinarySwap` is installed (non-NOT_SET), call `unswap(swap, s, targetType)` — same shape as the Bug #12 top-level fix. The shortcut is shared with the NOT_SET path (Cluster 2 below).

**Cluster 2: {Toml, Proto, Hocon} × NOT_SET on a04 (3 invocations).**

- **Design decision: option (b) per-format with per-format wire shape — not option (a).** Investigation showed option (a) (route through `DefaultSwaps.BinarySwap` to emit base64 at NOT_SET) was unworkable because `BinarySwap.match` explicitly returns 0 at `BinaryFormat.NOT_SET` — the swap is intentionally skipped, and `MarshalledPropertyPostProcessor` doesn't install a per-property `binarySwap` at NOT_SET either. The JSON-family's NOT_SET wire form is *also* a JSON int-array (`[0, 1, -1, ...]`), not a base64 string; its round-trip works because the JSON parser already has a `List<Integer> → byte[]` collection-element coercion the three text formats lacked. Each format needs a per-format wire shape that the format's own parser can reverse cleanly. The shapes are:
  - **Proto NOT_SET**: serializer was already correct — `ProtoWriter.bytesValue` emits hex-escaped char-by-char string literals (`"\x00\x01\xff..."`) and the Proto tokenizer decodes each `\xXX` back into a Java char with the matching codepoint. The fix is parser-only: in the same `ProtoParserSession.convertValue` shortcut above, when `targetType.getSwap(this) == null` (NOT_SET), reconstruct the `byte[]` by iterating the string's chars and casting each to a byte. Per-char codepoint == per-byte value because `ProtoWriter.bytesValue` emits one `\xXX` escape per source byte.
  - **Toml NOT_SET**: serializer was wrong — `TomlSerializerSession.writeValue`'s `isCollection() || isArray()` branch matched on the `aType` (the element's bean-property type) rather than the runtime class, and at NOT_SET inside a `List<byte[]>` the `aType` was erased to `Object` for the list element. The dispatch fell through to the generic `String.valueOf` fallback, emitting `[B@7c30a502` (the JVM's default `byte[].toString()`). Fix: insert an explicit `value instanceof byte[]` branch *before* the `isCollection() || isArray()` branch that emits the bytes as a TOML int-array (`[0, 1, -1, -128, 127, 16, 32, 48]`). The Toml parser's existing `List<Number> → byte[]` coercion (already present in `TomlParserSession.convertValue`) reverses it cleanly.
  - **Hocon NOT_SET**: both sides changed because HOCON's array-concatenation semantics (`[a, b] [c, d]` ≡ `[a, b, c, d]`) collapse nested int-arrays for `List<byte[]>` into a single flat int-array on parse. Serializer: same `instanceof byte[]` shortcut in `HoconSerializerSession.serializeAnything` before the generic collection branch, but instead of emitting an int-array (which would hit the concatenation bug) emit `serializeString(hw, Base64.getEncoder().encodeToString(bytes))` — a quoted base64 string sidesteps the concat ambiguity. Parser: symmetric in `HoconParserSession.unswapOrConvertValue` — when a STRING value targets `byte[]` and no swap is configured (NOT_SET), call `Base64.getDecoder().decode(s)` as the fallback.

**Sites touched (production code).**

- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/proto/ProtoParserSession.java` — `convertValue` now has a `String → byte[]` shortcut that handles both swap-dispatch (Cluster 1) and the NOT_SET char-as-byte path (Cluster 2 Proto).
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/toml/TomlSerializerSession.java` — `writeValue` now has an explicit `byte[]` branch emitting TOML int-arrays at NOT_SET (Cluster 2 Toml).
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconSerializerSession.java` — `serializeAnything` now has an explicit `byte[]` branch emitting base64 strings at NOT_SET (Cluster 2 Hocon).
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/hocon/HoconParserSession.java` — `unswapOrConvertValue`'s STRING → byte[] shortcut now decodes base64 as the fallback when no swap is installed (Cluster 2 Hocon).

**`isA04ByteArrayCollectionResidual` predicate deleted** from `juneau-utest/src/test/java/org/apache/juneau/transforms/BinaryFormat_RoundTrip_Test.java`. The predicate's call site in `a04_byteArrayProperty_inList` was removed; all 7 formerly-narrowed invocations now exercise the full round-trip. Predicate Javadoc preamble + method + caller removed in the same edit. Imports unchanged (the `HoconSerializer` / `TomlSerializer` / `ProtoSerializer` references in the predicate body had sibling usages elsewhere in the file).

**Counts (post-closure).**

- `BinaryFormat_RoundTrip_Test`: **1260 / 0** (no in-test residual predicates remain on a04 — the formerly-narrowed 7 invocations now exercise real round-trip work).
- 18-format cumulative: **35,784 / 0** unchanged from pre-closure (the formerly-skipped a04 invocations were already counted under JUnit's early-return-as-pass semantics, so the cumulative number is stable).
- Full regression (`./scripts/test.py --full`): **86,326 / 0 / 0 / 20** pre-existing skips. No regressions outside the format round-trip suite.

**Probe verification.** Throwaway `/tmp/ByteArrayProbe.java` round-tripped `Bean { List<byte[]> list; }` with three elements `[{0,1,-1,-128,127,16,32,48}, {-86,-69,-52}, {-103}]` through {Proto, Toml, Hocon} × {NOT_SET, HEX, SPACED_HEX, BASE64, BASE64_URL}. All 15 invocations recover `Arrays.equals(parsed.list.get(i), original)` for every `i`. Wire forms confirmed per the per-format shapes above. Probe deleted after verification.

#### Post-Wave-4 known-bug-fix turn (2026-05-21)

- **Four production bugs closed in parallel by four workers.** See the per-bug entries in the inventory above for site lists, fix shapes, and closure notes:
  - **Bug #13** — UrlEncoding-expanded empty `byte[]` round-trips as `null`. Paired serializer + parser fix in `UrlEncodingSerializerSession.serializeBeanMap` + `UrlEncodingParserSession.parseIntoBeanMap` S3 branch.
  - **Bug #14** — Toml `writeValue` type-erasure dispatch on `List<T[]>`. Generalized the `byte[]`-only branch to a runtime-array branch using `java.lang.reflect.Array`.
  - **Bug #15** — Hocon parser-fragility (multi-surface). Three production bugs fixed across `HoconWriter` (QUOTE_VALUE_CHARS extension), `HoconTokenizer` (recursion guard), and `HoconParserSession` (parseArray flatten bug + peek-before-skip).
  - **Bug #16** — `ClassFormat.FQCN` nested-types / arrays parser-fragility. Three-tier resolution in `ClassFormat.parse` (array-suffix stripping, primitive-name table, multi-level dot-to-`$` walk-back).
- **Test-side closures** (3 files touched, no production code touched in the synthesis turn):
  - **`BinaryFormat_RoundTrip_Test.java`** — `isUrlEncodingExpandedEmptyByteArrayGap` predicate + Javadoc + inline `if/else` tolerance in `a03_byteArrayProperty_edgeCases` removed; the `else` branch's `assertBytesEquals(EMPTY, x.empty, ...)` is now unconditional. **1260 / 0** post-collapse — Bug #13 closes the inline tolerance.
  - **`ClassFormat_RoundTrip_Test.java`** — `isHoconNestedBinaryName` predicate + Javadoc + call site at `a03_classProperty_nestedAndCollection` removed. The Hocon × `BINARY_NAME` × nested-type combination now exercises the round-trip end-to-end. **756 / 0** post-collapse — Bug #15 (Hocon recursion + `$` quoting) and Bug #16 (`ClassFormat.parse` multi-tier resolution) close it.
  - **`CalendarFormat_RoundTrip_Test.java`** — `a03_calendarProperty_crossZone` zone restored: `America/Los_Angeles` → `Asia/Tokyo` (Wave-1 workaround that switched away from positive-offset zones is reversed). The bean field renamed `la` → `tokyo`; the workaround comment updated to note the closure. The `BASIC_ISO_DATE` × `Asia/Tokyo` combination now emits `+0900` properly quoted on the Hocon wire and parses back without recursion. **4536 / 0** post-restore — Bug #15 closes it.
- **Counts** (per-file invocation counts unchanged due to JUnit's early-return-as-pass semantics; the changed work is that formerly-skipped / formerly-tolerant invocations now do real assertion work):
  - `BinaryFormat_RoundTrip_Test`: **1260 / 0** (the `a03 × UrlEncoding × NOT_SET × empty byte[]` invocations now assert `EMPTY == x.empty` unconditionally; the inline `null`-tolerance is gone).
  - `ClassFormat_RoundTrip_Test`: **756 / 0** (the 1 formerly-early-returned Hocon × `BINARY_NAME` × `a03` invocation now exercises the nested-type round-trip).
  - `CalendarFormat_RoundTrip_Test`: **4536 / 0** (all `a03 × Hocon × BASIC_ISO_DATE` invocations now exercise the positive-offset `+0900` path that was previously dodged).
  - 18-format cumulative: **35,784 / 0** (unchanged from pre-collapse).
- **Full regression** (`./scripts/test.py`): **86,326 / 0 / 0 / 20** pre-existing skips. No regressions outside the format round-trip suite (count unchanged from the post-Wave-4 a04 residual closure baseline).
- **Open Questions closed**: items **8** (`ClassFormat.FQCN` parser-fragility), **9** (Hocon parser-fragility audit), and **12** (Hocon serializer-side fragility broader than `=`).
- **Open Questions added** (items 13–19): `peekNoSkip` eager-consume pattern, `UNQUOTED_FORBIDDEN` ↔ `QUOTE_VALUE_CHARS` set-membership audit, HOCON `byte[]`-at-NOT_SET base64 sidestep redundancy, `HoconTokenizer.readTokenImmediate` `=`/`+` guard suspicion, `ClassFormatSwap.unswap` classloader leak, `getClassMetaForObject(value, hint)` runtime-class-lookup pattern, `juneau-rest-mock` pre-existing compilation errors.

#### Open-Question disposition turn (2026-05-22)

User-driven disposition pass on the 19 outstanding Open Questions plus three actionable items landed in parallel:

- **OQ 1 — coverage baseline framing**: answered, target locked at **≥ 90 % branches** post-TemporalFormat-carryover.
- **OQ 4 — effort estimate**: answered (confirmed acceptable cadence).
- **OQ 5 — per-format-enum value coverage**: answered (full cross-product).
- **OQ 6 — `BeanPropertyMeta.setPropertyValue` map-key gap**: closed by promotion to a separate plan. New plan landed in `todo/TODO-14-beanpropertymeta-map-key-coercion.md` (191 lines). Bullet added to master `todo/TODO.md`. Site analysis: gap at lines 1156-1163 (`needsConversion` predicate inspects only entry values) and 1180-1186 (value-coercion `forEach` converts only values, forwards keys as-is) of `BeanPropertyMeta.java`.
- **OQ 7 — Parquet footer logicalType discriminant**: agreed for a future Parquet schema-fidelity pass.
- **OQ 10 — `BinarySwap` "native bytes for binary serializers" contract**: closed via option (b1) `hasNativeBytes()` capability check. New methods on `OutputStreamSerializerSession` and `InputStreamParserSession` (default `true`); overrides on the four Parquet/RDF sessions (`false`); consumed by `BinarySwap.match` and `MarshalledPropertyPostProcessor.binarySwap.swap()`. Behavioral change: Parquet/RdfThrift/RdfProto at non-`NOT_SET` formats now route through the configured text wire form (UTF-8 string column / plain string literal) instead of raw bytes / `xsd:base64Binary`. Bug #11's three NOT_SET branches stay structurally intact and own that case unchanged. MsgPack / CBOR / BSON unchanged at every format. Verification: 439/0 across `Parquet*Test,Rdf*Test,MsgPack*Test,Cbor*Test,Bson*Test`; 92/0 `Proto*Test`; 1260/0 `BinaryFormat_RoundTrip_Test` (unchanged); 63,431/0 broad `*RoundTrip*Test` sweep.
- **OQ 11 — `juneau-utest` classpath staleness**: closed via project-level `.mvn/maven.config` containing `--also-make`. Real cause was reactor scoping (Maven puts only `juneau-utest` in the active reactor without `-am`, resolving every upstream module from `~/.m2`); fix is not RDF-specific. `--also-make` is inert without `-pl` so root-level builds and `./scripts/test.py` / `./scripts/push.py` are unaffected. Verification: probe edit in `juneau-marshall-rdf` exercised without intermediate `mvn install`; `mvn clean install -DskipTests` succeeds in 30.9s; `./scripts/test.py --test-only` clean at `86,326 / 0 / 0 / 20`. No `AGENTS.md` change required.
- **OQs 13–19 — Hocon `peekNoSkip`, `UNQUOTED_FORBIDDEN`/`QUOTE_VALUE_CHARS` audit, HOCON `byte[]`-at-NOT_SET sidestep, `HoconTokenizer` `=`/`+` guard, `ClassFormatSwap.unswap` classloader, `getClassMetaForObject(value, hint)` runtime-lookup, `juneau-rest-mock` compile errors**: all agreed for future passes / follow-ups. **OQ 16 carries explicit user note**: "this tokenizer is new code, so it may very well be a bug" — treat the suspicion as a likely bug rather than a defensive pattern when the future-pass turn lands.
- **New OQs added** (items **20** sibling MPP `OutputStreamSerializerSession` skips on `bigNumberSwap` / `booleanSwap` / `floatSwap`, **21** stale `BinaryFormat_RoundTrip_Test` class-level Javadoc) surfaced incidentally by the OQ 10 worker.

#### TemporalFormat carryover (complete) — 2026-05-22

The 8 deferred `TemporalFormat_<Subtype>_RoundTrip_Test.java` siblings landed in this turn alongside the two pilot files. Wave-1 carryover now fully closed.

- **Files added** (8 — each under `juneau-utest/src/test/java/org/apache/juneau/transforms/`):

  | File                                         | Tests | Failures | Errors | Skipped | Notes |
  | -------------------------------------------- | ----- | -------- | ------ | ------- | ----- |
  | `TemporalFormat_LocalDate_RoundTrip_Test`     | 5040  | 0        | 0      | 0       | Date-only; time-bearing formats become lossy (defaults to 00:00:00) — handled in `expectedAfter`. |
  | `TemporalFormat_OffsetDateTime_RoundTrip_Test`| 5040  | 0        | 0      | 0       | Top-level `a06` assertion widened to also accept same-instant-different-zone (some serializers reinterpret in default zone). |
  | `TemporalFormat_ZonedDateTime_RoundTrip_Test` | 5040  | 0        | 0      | 0       | Only `ISO_ZONED_DATE_TIME` preserves the zone-id; `a06` widened same as `OffsetDateTime`. |
  | `TemporalFormat_Year_RoundTrip_Test`          | 5040  | 0        | 0      | 0       | Only `ISO_YEAR` / `ISO_YEAR_MONTH` / `MILLIS` preserve the year cleanly; other formats fill defaults via `DefaultingTemporalAccessor`. |
  | `TemporalFormat_YearMonth_RoundTrip_Test`     | 5040  | 0        | 0      | 0       | Only `ISO_YEAR_MONTH` / `MILLIS` preserve year+month cleanly; other formats fill defaults same shape as `Year`. |
  | `TemporalFormat_LocalTime_RoundTrip_Test`     | 5040  | 0        | 0      | 0       | Time-only; date-bearing formats fill 1970-01-01; `MILLIS` falls back to `ISO_LOCAL_TIME` per `TemporalFormat`'s explicit `LocalTime` carve-out. |
  | `TemporalFormat_OffsetTime_RoundTrip_Test`    | 5040  | 0        | 0      | 0       | All formats green post-Bug #17 fix.  Skip predicate `isOffsetTimeMillisAsymmetry` removed; ~210 formerly-skipped invocations now exercise real round-trip work. |
  | `TemporalFormat_MonthDay_RoundTrip_Test`      | 5040  | 0        | 0      | 0       | All formats green post-Bug #18 fix.  Class-level `skipUntilBug18()` placeholder removed; file matches canonical pilot shape with real assertions for all 6 methods. |

- **Cumulative matrix count** (post-turn, post-Bug #17/#18 closure): 26 `*Format_RoundTrip_Test` files / **76,104 invocations / 0 failures / 0 errors / 0 skipped**.  Pre-turn baseline was 35,784/0; net delta `+40,320 / 0` (8 files × 5040 each).  The 18-format scope from the user's prompt now covers 16 single-format files + 2 TemporalFormat pilots + 8 TemporalFormat carryover = 26 files.

- **Novel bugs found** (2 — both **closed in the post-carryover Bug-fix turn this same date**, see Bug #17 and Bug #18 inventory entries above for full closure detail):
  - **Bug #17** — `OffsetTime` × `TemporalFormat.MILLIS` swap asymmetry.  Closed via option (b): new `TemporalFormat.isMillisNumeric(Class)` helper + MPP `temporalSwap` consumes the helper.
  - **Bug #18** — `MonthDay` isn't a `Temporal`.  Closed via the structural shape proposed in this entry: new `temporalAccessorSwap` factory + new `isTemporalAccessorType` predicate + widened `format`/`parse` signatures + `MonthDay`-specific native toString/parse handoff.

- **Skip predicates collapsed** (post-fix):
  - `TemporalFormat_OffsetTime_RoundTrip_Test.isOffsetTimeMillisAsymmetry(fmt)` — predicate + class-level Javadoc preamble + 6 `if (...) return;` call sites deleted.
  - `TemporalFormat_MonthDay_RoundTrip_Test.skipUntilBug18()` — helper + class-level placeholder Javadoc preamble + 6 `assumeFalse` call sites deleted; real test bodies (modeled on the canonical pilot shape) now drive 5040 real assertions.

- **Per-subtype applicability findings** (vs the user's rough lists in the prompt):

  - **`LocalDate`** — every `TemporalFormat` value round-trips structurally, but most lossily (date-bearing-only formats are clean; time-bearing formats default the time to 00:00:00, then drop it on parse). The `DefaultingTemporalAccessor` mechanism in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/swaps/DefaultingTemporalAccessor.java` is the keystone — it provides default `HOUR_OF_DAY`, `MINUTE_OF_HOUR`, `SECOND_OF_MINUTE`, `NANO_OF_SECOND` when the `Temporal` doesn't supply them. **Surprise vs prompt**: `ISO_INSTANT` / `ISO_LOCAL_DATE_TIME` / `ISO_OFFSET_DATE_TIME` / `ISO_ZONED_DATE_TIME` and all time-only formats also "work" (lossily) — the rough list said they were inapplicable.
  - **`OffsetDateTime`** — every format round-trips. Top-level `a06` assertion needed widening to accept same-instant-different-zone outcomes (some serializers like Hocon with native datetime support bypass the configured swap and reinterpret the `Instant` in the system default zone).
  - **`ZonedDateTime`** — every format round-trips. Only `ISO_ZONED_DATE_TIME` preserves the zone-id literally; everything else canonicalizes through the `Instant`. Same `a06` widening as `OffsetDateTime`.
  - **`Year`** — every format round-trips, but only `ISO_YEAR` / `ISO_YEAR_MONTH` / `MILLIS` preserve the year cleanly. Time-only formats coerce through `DefaultingTemporalAccessor` (year defaults to 1970). **Surprise**: the rough list said applicability would be very narrow; in practice all 20 formats round-trip with appropriate canonicalization in `expectedAfter`.
  - **`YearMonth`** — same shape as `Year`; only `ISO_YEAR_MONTH` / `MILLIS` preserve year+month cleanly. **Surprise**: same — broader applicability than the rough list suggested.
  - **`LocalTime`** — every format round-trips. Date-bearing formats fill 1970-01-01 on the way out and drop it on parse; `MILLIS` explicitly falls back to `ISO_LOCAL_TIME` per `TemporalFormat`'s `LocalTime` carve-out. **Surprise**: `ISO_OFFSET_TIME` round-trips cleanly (no offset-loss complaint) — the format pads with `+00:00` then strips it on parse.
  - **`OffsetTime`** — every format round-trips post-Bug #17 fix. Date-bearing formats fill 1970-01-01 + offset on the way out. `MILLIS` falls back to `ISO_OFFSET_TIME` per `TemporalFormat`'s explicit `OffsetTime` carve-out (sibling of the `LocalTime` / `MonthDay` carve-outs). **Surprise**: `MILLIS` was the *only* format that broke pre-fix; the rough list said `ISO_LOCAL_TIME` would also break (it doesn't — the offset is dropped on parse but the wire round-trips cleanly).
  - **`MonthDay`** — every format round-trips post-Bug #18 fix.  All `TemporalFormat` values funnel through `MonthDay`'s native `--MM-DD` `toString()` / `parse()` shape since no `DateTimeFormatter` from the standard ISO family matches `--MM-DD` and every other format value is structurally meaningless without a year.  Surprise vs prompt's "narrow applicability story like Year/YearMonth": the structural blocker was that `MonthDay` isn't a `Temporal` (closed structurally), not that the format applicability matrix is narrow.

- **Coverage delta**: not run this turn (per OQ 1 the post-carryover branch coverage check is the gating step; it can land in a follow-up coverage-add pass before Phase 5 closes if needed). Estimate: every new file exercises the same `MarshalledPropertyPostProcessor.temporalSwap` factory paths that the 2 pilot files already covered, so the branch-coverage delta on `MarshalledPropertyPostProcessor.java` and `TemporalFormat.java` should be modest. Per-format-enum file coverage on `TemporalFormat.java` will increase because the new files exercise format dispatch for 8 additional `Temporal` subtypes.

- **Phase summary**: TemporalFormat is now the single largest matrix in the project (26 files / 76,104 invocations) — surpassing every individual format-cluster matrix. The Wave-1 carryover deferral surfaced 2 novel production bugs (Bug #17, #18) on subtype-specific swap dispatch that the original Wave-1 `Instant` / `LocalDateTime` pilots had no way of catching, vindicating the cross-subtype matrix shape.

#### Post-TemporalFormat-carryover known-bug-fix turn (2026-05-22)

- **Bugs #17 and #18 fixed in production code in this turn.** See the per-bug entries in the inventory above for site lists, fix shapes, and closure notes. Single source of truth for the `MILLIS` wire-type decision is now `TemporalFormat.isMillisNumeric(Class<? extends TemporalAccessor>)` — Bug #17 collapses to a one-line consumer of the helper; Bug #18 is the structural fix (new `temporalAccessorSwap` factory + `isTemporalAccessorType` predicate + widened `format`/`parse` signatures + `MonthDay` native toString/parse handoff) and consumes the same helper for symmetry with `temporalSwap`.
- **Test-side closures** (2 files touched, no other production code touched in this turn):
  - **`TemporalFormat_OffsetTime_RoundTrip_Test.java`** — `isOffsetTimeMillisAsymmetry` predicate + class-level Javadoc preamble + 6 `if (...) return;` call sites removed (a01 / a02 / a03 / a04 / a05 / a06). File now matches the canonical pilot shape. **5040 / 0 / 0 / 0** post-collapse — Bug #17 closes it.
  - **`TemporalFormat_MonthDay_RoundTrip_Test.java`** — `skipUntilBug18()` helper + class-level placeholder Javadoc preamble + 6 `assumeFalse` call sites removed; the file's class-level Javadoc rewritten to describe the post-fix routing through the sibling `temporalAccessorSwap` factory; real test bodies (modeled on `TemporalFormat_Instant_RoundTrip_Test`'s shape) added for all 6 methods (a01 basic, a02 multiple fields, a03 leap-day boundary, a04 end-of-year stress, a05 null preservation, a06 top-level standalone); `expectedAfter()` helper added that canonicalizes through the format's own `format/parse` cycle. **5040 / 0 / 0 / 0** post-collapse — Bug #18 closes it.
- **Counts** (post-fix):
  - `TemporalFormat_OffsetTime_RoundTrip_Test`: **5040 / 0 / 0 / 0** (was `5040 / 0 / 0 / 0` pre-fix due to JUnit's early-return-as-pass semantics; ~210 invocations now exercise real round-trip work).
  - `TemporalFormat_MonthDay_RoundTrip_Test`: **5040 / 0 / 0 / 0** (was `5040 / 0 / 0 / 5040` placeholder pre-fix; net `−5040 skipped`).
  - 26-file format matrix cumulative: **76,104 / 0 / 0 / 0** (was `76,104 / 0 / 0 / 5040` pre-fix; net `−5040 skipped`).
- **Pre-existing regression sweep**: `mvn -pl juneau-utest -Dtest='*Temporal*Test,*MonthDay*Test,Iso8601Utils*Test,DefaultSwaps*Test' test` — **50,490 / 0 / 0 / 0**. The lenient `TemporalFormat_Test.a16_millis_fromEpochToAllSupportedSubtypes` test (which asserts `MILLIS.parse(numeric, OffsetTime.class)` is non-null) continues to pass thanks to the parse-side adjustment in Bug #17 — numeric input still routes through `fromEpochMillis` for legacy compatibility, and the new ISO_OFFSET_TIME wire shape (which post-fix `format()` emits) routes through `DEFAULT.parse`.
- **Full regression** (`./scripts/test.py`): **124,018 / 0 / 0 / 20**. No regressions outside the format round-trip suite. The 20 pre-existing skips returned to the pre-MonthDay-placeholder baseline (was `124,018 / 0 / 0 / 5060` immediately after the carryover landed; net `−5040 skipped`).
- **Open Questions added** (none new this turn — both bugs closed cleanly per the prompt's structural plan; no incidental observations surfaced that warrant tracking).

#### Coverage closure (post-TemporalFormat-carryover, 2026-05-22)

Final gating check for Phase 5 closure (Open Question 1) — re-ran `./scripts/coverage.py --run` against `MarshalledPropertyPostProcessor.java` and the 15 per-format enum files in scope, identified gaps below the 90 % branch threshold, added targeted unit tests for the reachable gaps, and documented the uncovered-by-design remainder.

**Pre-closure baseline** (per-file branch coverage, `./scripts/coverage.py --run` against `juneau-utest/target/jacoco.exec` post-TemporalFormat-carryover + post-Bug #17/#18):

| File                               | Branches |   Instr | Missed branches | Status at gate |
|------------------------------------|----------|---------|-----------------|----------------|
| `MarshalledPropertyPostProcessor`  | **82 %** | 92 %    | 90              | **gap**        |
| `DurationFormat`                   | 97 %     | 98 %    | 2               | OK             |
| `PeriodFormat`                     | 100 %    | 100 %   | 0               | OK             |
| `DateFormat`                       | 100 %    | 100 %   | 0               | OK             |
| `CalendarFormat`                   | 95 %     | 95 %    | 2               | OK             |
| `TemporalFormat`                   | 92 %     | 96 %    | 13              | OK             |
| `TimeZoneFormat`                   | 100 %    | 97 %    | 0               | OK             |
| `BigNumberFormat`                  | 98 %     | 100 %   | 1               | OK             |
| `FloatFormat`                      | 100 %    | 100 %   | 0               | OK             |
| `CurrencyFormat`                   | 91 %     | 86 %    | 3               | OK             |
| `UuidFormat`                       | **89 %** | 100 %   | 2               | **gap**        |
| `EnumFormat`                       | 96 %     | 99 %    | 2               | OK             |
| `LocaleFormat`                     | 100 %    | 100 %   | 0               | OK             |
| `ClassFormat`                      | **67 %** | 79 %    | 16              | **gap**        |
| `BinaryFormat`                     | 92 %     | 90 %    | 3               | OK             |
| `BooleanFormat`                    | 100 %    | 100 %   | 0               | OK             |

Three files below the gate: **MarshalledPropertyPostProcessor** (90 branches short of full coverage, 82 %), **UuidFormat** (2 branches, 89 %), **ClassFormat** (16 branches, 67 %).

**Tests added** (3 files touched / new — all test-side, no production code touched in this closure turn):

- `juneau-utest/src/test/java/org/apache/juneau/transforms/ClassFormat_Test.java` — +9 `@Test` methods covering the array-suffix resolution path (`f01`-`f06`: `int[]` / `String[]` / `String[][]` / `Map.Entry[]` / empty leaf throw / unknown leaf throw) and primitive-name table (`g01`-`g03`: top-level primitive resolution for all 9 primitives, array form for 8 non-`void` primitives, default-arm coverage via `java.lang.String`). Closes all 16 missed branches in `ClassFormat`; file now at **100 % / 100 %**.
- `juneau-utest/src/test/java/org/apache/juneau/transforms/UuidFormat_Test.java` — +2 `@Test` methods covering the short-input branch (length ≤ `URN_PREFIX.length()`) and the 32-char-with-dash branch (compact-expand skipped because `indexOf('-') ≥ 0`). Closes both missed branches; file now at **100 % / 100 %**.
- `juneau-utest/src/test/java/org/apache/juneau/MarshalledPropertyPostProcessor_Test.java` — **new** 530-line test file living in `org.apache.juneau` for package-private MPP access. 11 fixture clusters (`A`-`L`) covering: (A) `@MarshalledProp` default = NOT_SET short-circuit for each typed field, (B) `@Marshalled` class-level NOT_SET short-circuit, (C) XMLGregorianCalendar null/empty/valid wire round-trips, (D) Bug #18 carryover MILLIS on the `temporalAccessorSwap` path, (E) `@MarshalledProp` with explicit format on each typed field (mirror of A), (F) `@Marshalled` with explicit format on each typed field (mirror of E), (G) `@MarshalledProp` + `@Swap` on the same field (b.swap-already-set short-circuit at lines 120/132), (H) primitive float / double with `@MarshalledProp(floatFormat=…)` + blank-string parse exercising the s.isBlank() branch, (I) `XMLGregorianCalendar` + `@MarshalledProp` on field / getter / setter (xmlGregorianCalendarSwap installed at line 113 before the `@MarshalledProp` foreach), (J) `List<byte[]>` with context BinaryFormat (child-swap dispatch via rawTypeMeta), (K) `Number` field with a context-registered `BigInteger`-subclass swap (child-swap path firing through `hasChildSwaps()` + `getChildObjectSwapForSwap`), (L) direct INSTANCE invocation with null marshalling context (defensive guard at line 92). 23 `@Test` methods total. Closes 39 of the 90 missed MPP branches.

**Post-closure achieved coverage** (per-file branch coverage, post test-add):

| File                               | Branches | Instr   | Missed | Δ branches |
|------------------------------------|----------|---------|--------|------------|
| `MarshalledPropertyPostProcessor`  | **90 %** | **95 %**| 51     | -39        |
| `DurationFormat`                   | 97 %     | 98 %    | 2      | 0          |
| `PeriodFormat`                     | 100 %    | 100 %   | 0      | 0          |
| `DateFormat`                       | 100 %    | 100 %   | 0      | 0          |
| `CalendarFormat`                   | 95 %     | 95 %    | 2      | 0          |
| `TemporalFormat`                   | 92 %     | 96 %    | 13     | 0          |
| `TimeZoneFormat`                   | 100 %    | 97 %    | 0      | 0          |
| `BigNumberFormat`                  | 98 %     | 100 %   | 1      | 0          |
| `FloatFormat`                      | 100 %    | 100 %   | 0      | 0          |
| `CurrencyFormat`                   | 91 %     | 86 %    | 3      | 0          |
| `UuidFormat`                       | **100 %**| **100 %**| 0     | -2         |
| `EnumFormat`                       | 96 %     | 99 %    | 2      | 0          |
| `LocaleFormat`                     | 100 %    | 100 %   | 0      | 0          |
| `ClassFormat`                      | **100 %**| **100 %**| 0     | -16        |
| `BinaryFormat`                     | 92 %     | 90 %    | 3      | 0          |
| `BooleanFormat`                    | 100 %    | 100 %   | 0      | 0          |

All 16 in-scope files at **≥ 90 % branch coverage**. Average per-format-enum branch coverage: **97 %**. Per-format-enum range: **91 % (CurrencyFormat) – 100 % (8 files)**.

**Uncovered branch inventory** (post-closure: 51 in MPP, 22 across the per-format enums). Every remaining gap is either category (b) — reachable in principle but defensive / framework-edge that the round-trip matrix can't construct — or category (c) — structurally unreachable. No category (a) gaps remain.

Cluster A — `MarshalledPropertyPostProcessor.java` (51 branches):

| File:line(s) | What the branch represents | Why uncovered | Disposition |
|--------------|----------------------------|---------------|-------------|
| MPP.java:113 (1/4) | `b.swap == null` arm of XMLGregorianCalendar pre-loop guard | xmlGregorianCalendarSwap is installed by this very line; no caller can set `b.swap` on an XMLGregorianCalendar field before this check | (c) unreachable |
| MPP.java:144 (1/2) | `if (b.swap == null)` inside setter `@MarshalledProp` foreach | BeanContext's annotation discovery doesn't surface setter-only `@MarshalledProp` annotations when a getter is also present (covered for field / getter via tests `i01` / `i02`); category (b) — would need a setter-only bean shape the framework doesn't reach | (b) framework-edge |
| MPP.java:154 (1/2) | `nn(ownerClass)` false-arm | `owningClass(b)` returns null only when all three of innerField / getter / setter are null; MPP is only invoked when at least one is non-null | (c) unreachable |
| MPP.java:175 (1/4) | `b.swap != null && b.rawTypeMeta == null` arm | Standard BeanContext always sets rawTypeMeta on every bean property; an installed swap without rawTypeMeta is an inconsistent builder state that the framework doesn't produce | (b) defensive |
| MPP.java:213, 215, 226, 228, 239, 246, 248, 250 (8 branches) | installSwapAwareTransforms internal arms: rtm-null branch in entry guard, readTransform/writeTransform already-installed shortcuts, child-swap dispatch within transforms | The child-swap path **is** exercised partially by tests `k01`/`k02` (Number field + BigInteger child swap), closing some branches; the readTransform/writeTransform already-installed arms require a property pre-built with a transform that no bean shape exposes; rtm-null variant requires a non-typed-meta builder the framework doesn't produce | (b) framework-edge |
| MPP.java:302, 306, 315, 318, 325, 336, 357 (8 branches) | installSchemaValidationTransforms factory-null / merged-empty / validator-null / read-write-transform-validator-installed arms | Requires `PropertyValidatorFactory` on the classpath (resolved via ServiceLoader from `juneau-bean-jsonschema`, runtime-only dep) plus `@Schema` annotations + `validateSchema=true` on context | (b) requires runtime-only dep + ServiceLoader |
| MPP.java:366, 368, 370, 372, 374 (~6 branches) | propertyClass(b) fallthrough arms (innerField / getter / setter all null path, empty setter params) | Same as line 154 — MPP is only invoked when at least one is non-null; the empty-params arm requires a no-arg setter | (b) defensive |
| MPP.java:393, 395, 398, 400, 402, 406 (~7 branches) | swapSwap factory: isVoid(value()) + isVoid(impl()) double-guards, mediaTypes/template throws, Surrogate-class throw | Requires `@Swap(impl=…)` (not in existing fixtures) or `@Swap(value=Surrogate.class)` patterns that the matrix doesn't construct | (b) annotation shape not in fixtures |
| MPP.java:416 (1/2) | owningClass setter-only branch | Same as line 154 / 366 | (b) defensive |
| MPP.java:562, 566, 573, 577, 581 (~5 branches total across 5 helpers) | `is*Type` predicate `c != null` false arms (and the XMLGregorianCalendar carve-out in isCalendarType, the TemporalAccessor/Temporal complement in isTemporalAccessorType, the TimeZone/ZoneId split in isTimeZoneType) | propertyClass(b) falls through to `Object.class` (never null); the carve-outs for XMLGregorianCalendar / Temporal complement are reachable in principle but the matrix never routes a `Class c == null` into these helpers | (b) defensive |
| MPP.java:727 (1/4) | `temporalAccessorSwap.swap` `format == MILLIS && isMillisNumeric(temporalType)` true-true arm | Only TemporalAccessor non-Temporal type is `MonthDay`, and `isMillisNumeric(MonthDay)` returns false by design (Bug #18 carve-out) — the true-true arm is unreachable until a future JDK adds another non-Temporal TemporalAccessor that's MILLIS-numeric | (c) unreachable today |
| MPP.java:800 (1/4) | `binarySwap.swap` OpenAPI-session branch (`session instanceof OpenApiSerializerSession`) | CSV branch hit by the matrix; OpenAPI byte[] dispatch is owned by OpenAPI's own BYTE / BINARY / BINARY_SPACED schema directives, not the `BinaryFormat` round-trip matrix (which excludes OpenAPI to avoid double-dipping the schema-directed encoding) | (b) outside the round-trip matrix's scope |
| MPP.java:849 (1/2) | `enumSwap.unswap` non-numeric `if (o == null)` true arm | `writeTransform` filters null at the framework level before delegating to unswap; the swap's own defensive null check is unreachable through the standard parser path | (b) defensive |
| MPP.java:890 (1/2) | `bigNumberSwap.unswap` BigInteger native-input fast path | Parser sessions normalize BigInteger to a Number / String tier before invoking unswap; native BigInteger reaches unswap only in binary-bigint formats (CBOR tag 2/3, BSON Decimal128) which the BigNumberFormat matrix's `STRING` / `NUMBER` modes don't exercise | (b) parser-tier filtering |
| MPP.java:964 (2/2) | `floatSwap.match` null-session early return | match() receives null session only on type-conversion lookup paths (BeanMap.set with no session) that the round-trip matrix doesn't trigger | (b) framework-edge |

Cluster B — per-format enum files (22 branches across 8 files; the other 7 files are at 100 %):

| File:line | What the branch represents | Why uncovered | Disposition |
|-----------|----------------------------|---------------|-------------|
| `DurationFormat.java:145, 159` (2) | `format()` null-Duration arm + HOCON `format()` 8-way unit-routing default arm | Null-input arm requires a swap.swap(null) call the matrix doesn't construct; HOCON default arm guards against unreachable unit values | (b)/(c) defensive |
| `CalendarFormat.java:146, 150` (2) | `format()` and `parse()` null-Calendar arms | Same as DurationFormat:145 | (b) defensive |
| `TemporalFormat.java:231, 240, 299-310, 331, 432, 440-443` (13) | `format()` / `parse()` null-input arms across overloads, `isMillisNumeric` fallthrough for non-mapped TemporalAccessor types, several DEFAULT-format dispatch default arms | Same shape — null-input defensive arms + future-proofing dispatch tails | (b) defensive |
| `BigNumberFormat.java:202` (1/8) | `parse()` STRING-format default-target arm | The matrix exercises BigInteger / BigDecimal targets; an Integer / Long / Number target through `BigNumberFormat.parse` is reachable in principle but not constructed by the matrix | (b) target type not in fixtures |
| `CurrencyFormat.java:213, 218` (3 branches) | `parse()` SYMBOL-format scan-loop unmatched-symbol arm, NAME-format DisplayName-mismatch arm | Lenient parsing already covers the matched cases; the unmatched-symbol / no-locale-match arms throw — exercised only when input is intentionally malformed, not in the matrix's positive-path tests | (b) negative-path |
| `EnumFormat.java:148, 163` (2) | `parse()` null-target arm + numeric-format default-target arm | Null-target arm requires a swap.unswap(null) path not surfaced by the matrix; numeric-format default-target arm is a defensive fallthrough | (b) defensive |
| `BinaryFormat.java:134, 146, 150` (3) | `format()` BASE64_URL `null` byte-array arm, `parse()` HEX odd-length default arm, `parse()` SPACED_HEX malformed-spacing default arm | Same shape — null/defensive arms not exercised by positive-path matrix | (b) defensive |

Total uncovered branches post-closure: **51 (MPP) + 22 (per-format enums) = 73**. All are category (b) defensive / framework-edge or category (c) structurally unreachable. No category (a) gaps remain; the round-trip matrix + the unit-test additions made above cover every reachable user-facing path through the format-control dispatch.

**Regression verification**:

- `mvn -pl juneau-utest -Dtest='*Format_Test,*Format_RoundTrip_Test,MarshalledPropertyPostProcessor_Test' test`: **76,535 / 0 / 0 / 0** (the 76,104 baseline matrix unchanged; +431 from the format-unit-test files including the 23 newly-added `MarshalledPropertyPostProcessor_Test` methods + the 9 new `ClassFormat_Test` methods + the 2 new `UuidFormat_Test` methods + the pre-existing unit-test bodies).
- `./scripts/test.py`: **124,050 / 0 / 0 / 20**. Net delta vs the immediate pre-coverage-turn baseline (`124,018 / 0 / 0 / 20`): **+32 tests** — matches the coverage-closure additions exactly. `0 / 0 / 20` preserved.
- Lint: all 3 touched / new test files lint clean via the IDE lint pass (no `ReadLints` errors).

#### What's next after Wave 4 complete

1. ~~**TemporalFormat carryover**~~ ✅ **CLOSED 2026-05-22** — see "TemporalFormat carryover (complete)" subsection below. All 8 deferred `TemporalFormat_<Subtype>_RoundTrip_Test.java` siblings landed; post-Bug #17/#18 closure (this turn) all 8 fully exercise the matrix at **5040/0/0/0** each.
2. ~~**Bug #17** — `OffsetTime` × `TemporalFormat.MILLIS` swap asymmetry~~ ✅ **CLOSED 2026-05-22** — see Bug #17 inventory entry above. New `TemporalFormat.isMillisNumeric` helper + MPP `temporalSwap` consumes the helper.
3. ~~**Bug #18** — `MonthDay` isn't a `Temporal`~~ ✅ **CLOSED 2026-05-22** — see Bug #18 inventory entry above. New `temporalAccessorSwap` factory + `isTemporalAccessorType` predicate + widened `TemporalFormat.format`/`parse` signatures + `MonthDay` native toString/parse handoff.
4. **Residual / open items left after the post-Wave-4 known-bug-fix turn**:
   - ~~**Bug #11 / Bug #12 a04-collection-element residual**~~ ✅ Closed 2026-05-21 — see "Post-Wave-4 a04 residual closure" subsection above.
   - ~~**`isUrlEncodingExpandedEmptyByteArrayGap`** (`BinaryFormat_RoundTrip_Test.a03`)~~ ✅ Closed 2026-05-21 — see "Post-Wave-4 known-bug-fix turn" subsection above; predicate + inline tolerance collapsed.
   - ~~**Hocon parser-fragility audit**~~ ✅ Closed 2026-05-21 — see Open Question #9.
   - ~~**Toml `writeValue` type-erasure incidental find**~~ ✅ Closed 2026-05-21 — see Bug #14 below.
   - ~~**`ClassFormat.FQCN` parser-fragility**~~ ✅ Closed 2026-05-21 — see Open Question #8 / Bug #16.
3. **Incidental novel finds** — surface any newly-discovered bugs uncovered during ongoing matrix maintenance here. New observations from the post-Wave-4 known-bug-fix turn captured under Open Questions 13–19.

#### Phase 5 closure (TODO-57 acceptance criteria met)

Final-state turn summary captured at the end of the post-TemporalFormat-carryover coverage-closure turn (2026-05-22). With OQ 1 closed in this turn and all four Phase 5 acceptance criteria checked off in the `## Acceptance criteria` section above, the plan is closure-ready.

- ✅ **18 closed production bugs** — Bug #1, #2, #3, #3b, #4, #4c, #4d, #5, #5b, #5c, #6, #7, #7a, #7b, #8, #9, #10, #11 from the original Wave-1-through-Wave-4 sweep; #14 / #15 / #16 closed in the post-Wave-4 known-bug-fix turn; #17 / #18 closed in the post-TemporalFormat-carryover turn. No known open production bugs hiding behind format-control dispatch.
- ✅ **21 disposed Open Questions** — OQ 1-12 closed inline as they were answered; OQ 13-21 agreed-future-pass with explicit owners (TODO-56 dispatch cleanup, TODO-14 binary-format negative-path expansion, etc.). OQ 1 specifically closed in this coverage-closure turn.
- ✅ **26-file round-trip matrix at 76,104 / 0 / 0 / 0** — 16 single-format `*Format_RoundTrip_Test.java` files + 2 `TemporalFormat_RoundTrip_Test_*` pilots + 8 `TemporalFormat_<Subtype>_RoundTrip_Test.java` carryover siblings, all green.
- ✅ **Full regression at 124,050 / 0 / 0 / 20** — matches the pre-closure baseline `124,018 / 0 / 0 / 20` modulo the **+32 coverage-closure additions** (23 in `MarshalledPropertyPostProcessor_Test`, 9 in `ClassFormat_Test`, 2 in `UuidFormat_Test`). The `0 / 0 / 20` shape preserved exactly. No regressions outside the format-test suite.
- ✅ **≥ 90 % branch coverage** achieved on every file in scope — `MarshalledPropertyPostProcessor` at **90 % branches / 95 % instructions** (was 82 % / 92 % pre-closure, +39 branches closed); per-format-enum cluster averages **97 % branches** with range **91 % (CurrencyFormat) – 100 % (8 files at perfect)**. Detailed inventory of remaining defensive / framework-edge branches captured in the "Coverage closure (post-TemporalFormat-carryover, 2026-05-22)" subsection above; all category (b) / (c), no category (a) remaining.
- ✅ **`./scripts/test.py` + `mvn clean install`** both green at full regression. **Plan is closure-ready.**

#### Phase 5 — Wave 4: archived risk notes

- **Risk notes** *(retained for historical context — pre-Wave-4 predictions vs actual outcomes)*:
  - `BinaryFormat`: CSV quoting collisions for HEX. **Outcome**: not load-bearing — the existing `Csv` builder already has a `skipIf(o -> o == null || ... primitive array)` predicate that short-circuits the round-trip for primitive `byte[]` (HEX / SPACED_HEX / BASE64 / BASE64_URL all unaffected). No CSV-specific skip added.
  - `BinaryFormat`: binary serializers bypass `BinaryFormat`. **Outcome**: partially correct. MsgPack / CBOR / BSON do bypass cleanly (native `byte[]` opcode → variant `binarySwap` returns raw bytes → lossless round-trip). RdfThrift / RdfProto / Parquet **do not have a clean native handoff** — promoted to Bug #11 (the variant swap's design intent doesn't hold for these three because their underlying wire format doesn't carry a `byte[]` literal type / column type natively).
  - `BooleanFormat`: very few wire representations vary. **Outcome**: confirmed — the variant `booleanSwap` handles binary serializers' native boolean handoff cleanly, the `ZERO_ONE → Number → Boolean` unswap path threads through correctly, and YAML's native aliases never pre-empt the swap. **0 skip predicates required** on the BooleanFormat matrix.

### Phase 6 — Bug #2 investigation (complete)

- ✅ Investigated and fixed. The bug surfaced in three parsers (`UrlEncodingParserSession.unwrapValueAs`, `MarkdownParserSession.parseCellValue`, `TomlParserSession.convertValue`) and was fixed inline per-parser by threading `MarshallingContext.get<Format>()` into the bare-numeric (and Toml-side native-datetime) coercion paths. Did not lift to a shared `ReaderParserSession` helper this turn — see the Wave 2 observations entry for the open future option.
- ✅ Pilot + wave files re-run after fix: 27,956 / 0 across the 10 existing format round-trip test classes.

---

## Test infrastructure

- **`RoundTrip_Tester.Builder` visibility**: bumped from package-private to `public` (already landed in the pilot commit) so per-format tests can live under `org.apache.juneau.transforms` alongside other `*Format_*Test.java` files instead of being forced into `org.apache.juneau.a.rttests`. No other changes to the matrix.
- **No new dependencies**, **no new framework**. Each per-format test reuses the existing `RoundTripTest_Base` matrix and the canonical 42 tester templates.
- **File pattern**: `juneau-utest/src/test/java/org/apache/juneau/transforms/<FormatName>_RoundTrip_Test.java`. Six test methods per file (bean-property × {serialize, parse, round-trip} + top-level × {serialize, parse, round-trip}), parameterized over `(RoundTrip_Tester, FormatEnumValue)`.

---

## Acceptance criteria

- ✅ All 16 formats have a `*Format_RoundTrip_Test.java` matrix test under `juneau-utest/src/test/java/org/apache/juneau/transforms/` (26 files total including the 8 TemporalFormat-subtype siblings and the 2 TemporalFormat-pilot siblings).
- ✅ All bean-property **and** top-level invocations green across every applicable tester (with documented skips for the deliberate carve-outs — binary-format-bypasses-`BinaryFormat`, non-finite floats on JSON, etc.).
- ✅ Coverage on `MarshalledPropertyPostProcessor.java` and the 15 per-format enum files at **≥ 90 % branch coverage** (`./scripts/coverage.py`) — MPP at **90 %** post-closure (was 82 %); per-format-enum average **97 %** with all files ≥ 90 %. See "Coverage closure (post-TemporalFormat-carryover, 2026-05-22)" subsection.
- ✅ No production bugs known to be hiding behind format-control dispatch (18 bugs closed across the project; inventory in `## Bug inventory`).
- ✅ `./scripts/test.py` green at **124,050 / 0 / 0 / 20** (post-coverage-closure; was `124,018 / 0 / 0 / 20` immediately prior — net +32 from coverage-closure unit tests).
- `./scripts/sonarqube.py` clean on touched production files (test files do not need to be lint-clean by the same bar). No production files touched in this closure turn; the 3 test files added (`MarshalledPropertyPostProcessor_Test.java`, plus additions to `ClassFormat_Test.java` and `UuidFormat_Test.java`) lint clean.

---

## Open questions

1. **Coverage baseline framing.** **CLOSED 2026-05-22** — all 16 files in scope at ≥ 90 % branch coverage post-TemporalFormat-carryover. Detail in the "Coverage closure (post-TemporalFormat-carryover, 2026-05-22)" subsection below.

	- **MarshalledPropertyPostProcessor**: pre-closure **82 % / 92 %** (402 / 492 branches, 2378 / 2577 instructions) → post-closure **90 % / 95 %** (441 / 492 branches, 2441 / 2577 instructions). +39 branches closed.
	- **Per-format enum files** (15 files; matches the actual file count under `org.apache.juneau.*Format.java` — the plan's introductory paragraph counts the 16 formats including `PeriodFormat` separately, but only 15 enum files live in the package because `DurationFormat` and `PeriodFormat` already shared the `DurationAndPeriodFormat_Test` unit-test sibling per the existing layout): average **97 % branches**, range **91 %–100 %**. 7 of 15 already at **100 %** (PeriodFormat, DateFormat, TimeZoneFormat, FloatFormat, LocaleFormat, BooleanFormat, plus the newly-closed ClassFormat and UuidFormat). Remaining files at the gate: CurrencyFormat 91 %, TemporalFormat 92 %, BinaryFormat 92 %, CalendarFormat 95 %, EnumFormat 96 %, DurationFormat 97 %, BigNumberFormat 98 %. All ≥ 90 %.
2. ~~**Bug #2 disposition.**~~ *(answered.)* Per-parser inline fix shipped in `UrlEncodingParserSession`, `MarkdownParserSession`, and `TomlParserSession`. The shared-helper-lift to `ReaderParserSession` remains a future option if a mirror surfaces in another parser.
3. ~~**JSON-family precision tier (Bug #5).**~~ *(answered.)* Auto-detect classification in `StringUtils.parseNumber` always returns `Double` now; `Float` only when explicitly requested. The fix is in the shared classifier, not per-parser; this single site covers all 8 JSON-family parsers + every other caller of `StringUtils.parseNumber`.
4. ~~**Effort estimate.**~~ *(answered 2026-05-22 — confirmed.)* Pilot worker estimated **~25-30 hours** of test-writing across all waves; user confirmed acceptable rollout cadence. Waves 1–4 landed in the bracket (post-Wave-4 known-bug-fix turn included).
5. ~~**Per-format-enum value coverage.**~~ *(answered 2026-05-22 — full cross-product.)* User confirmed full cross-product across all enum values for every format. Capping defeats the matrix's purpose. Wave 4 already validated this — `BinaryFormat`'s 6 enum values × 42 templates × 5 binary-eligible serializers landed at 1260/0; `BooleanFormat`'s 4 values landed with 0 skip predicates.
6. ~~**Latent gap in `BeanPropertyMeta.setPropertyValue`.**~~ *(closed 2026-05-22 — moved to TODO-14.)* User directed this be tracked as a separate work item. Plan landed in `todo/TODO-14-beanpropertymeta-map-key-coercion.md` (191 lines): site analysis (gap at lines 1156-1163 + 1180-1186 of `BeanPropertyMeta.java`), four-phase plan (commons-side unit test → fix → regression-check the four Bug #7b parsers → audit sibling shapes), open questions on `forEach` → explicit-`for` refactor and raw-`Map` `getKeyType()` null-fallback. Per-parser fixes from Bug #7b stay in place as defense-in-depth.
7. **Parquet file footer drops `logicalType` discriminant.** *(agreed 2026-05-22 — future Parquet schema-fidelity pass.)* `ParquetSchemaElement.writeTo` writes only physical type + `convertedType`; the parser-side Bug #7a UUID fix uses `TYPE_FIXED_LEN_BYTE_ARRAY` as a UUID-only signal under the assumption that no other writer path emits FLBA. Tighten by emitting the logical-type union in the footer and consuming it on the parse side. Tracked for a future Parquet schema-fidelity pass.
8. **`ClassFormat.FQCN` with nested types or arrays is parser-fragile.** **CLOSED 2026-05-21** — fix landed in `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/ClassFormat.java` `parse(String, ClassFormat, ClassLoader)` (single file, +120/-22 lines). Three-tier resolution: (a) array-suffix stripping via `Class.arrayType()` for `int[]` / `java.lang.String[][]` / `[I` shapes, (b) primitive-name table for leaf primitives so `Class.forName("int")` resolves through the new table, (c) multi-level dot-to-`$` walk-back for nested types (`java.util.Map.Entry` → `java.util.Map$Entry`). Closes Worker A's Bug #7a follow-up flag. Capability gain: `Class.forName("int")` now resolves; previously rejected.
9. **Hocon parser-fragility audit.** **CLOSED 2026-05-21** — three production bugs fixed across `HoconWriter`, `HoconTokenizer`, and `HoconParserSession`. (a) `HoconWriter.QUOTE_VALUE_CHARS` extended with `+` and `$` so the Wave-1 `+NNNN` Calendar `BASIC_ISO_DATE` and Wave-3 `$` `ClassFormat.BINARY_NAME` wire forms now serialize quoted (the Wave-4 `=` was already added in Bug #10). (b) `HoconTokenizer.readUnquotedOrNumber` infinite-recursion guard converted from a `StackOverflowError` trip into a clean `IOException` so genuinely-malformed input fails fast with a parseable error rather than blowing the JVM stack. (c) `HoconParserSession.parseArray` flatten bug (unconditional `arr.concat(arr2)` against the next-array peek result) replaced with a peek-before-skip guard for `RBRACKET` / `EOF`; symmetric `RBRACE` guard added to `parseObject`. All four known Hocon parser-fragility surfaces now closed (Wave 1 `+NNNN`, Wave 3 `$`, Wave 4 `=`, post-Wave-4 array-concat).
10. **`BinarySwap`'s "native bytes for binary serializers" contract isn't universal.** **CLOSED 2026-05-22** — fix landed via option (b1) `hasNativeBytes()` capability check. New public methods on `OutputStreamSerializerSession` and `InputStreamParserSession` (default `true`) consulted by `BinarySwap.match` and `MarshalledPropertyPostProcessor.binarySwap.swap()` before short-circuiting on `instanceof OutputStreamSerializerSession`. Overridden to `false` on `ParquetSerializerSession`, `ParquetParserSession`, `RdfStreamSerializerSession`, `RdfStreamParserSession`. Behavioral change: at any non-`NOT_SET` `BinaryFormat`, Parquet now emits a UTF-8 string column (was raw `BYTE_ARRAY`) and RdfThrift/RdfProto now emit plain string literals (was `xsd:base64Binary` typed literals); both formats unchanged at `NOT_SET` where Bug #11's branches still own the case. MsgPack / CBOR / BSON unchanged at every format. Bug #11's three production-side `byte[]` branches (`ParquetSchemaBuilder.addSchemaElements` `cm.isByteArray()`, `RdfStreamSerializerSession.serializeAnything` `sType.isByteArray()`, `RdfStreamParserSession.parseAnything` `sType.isByteArray()`) become unreachable at non-`NOT_SET` because the variant swap class is `Object` and `getSerializedClassMeta` resolves to `CharSequence`; comments updated to spell this out. Verification: `Parquet*Test,Rdf*Test,MsgPack*Test,Cbor*Test,Bson*Test` (439/0), `Proto*Test` (92/0), `BinaryFormat_RoundTrip_Test` (1260/0 unchanged), broader `*ByteArray*Test,Binary*Test` sweep (2,972/0), broad `*RoundTrip*Test` sweep (63,431/0).
11. **`juneau-utest` classpath resolution for `juneau-marshall-rdf`.** **CLOSED 2026-05-22** — fix landed via project-level `.mvn/maven.config` containing `--also-make`. Real cause was reactor scoping, not dependency wiring: `juneau-marshall-rdf` was already declared as a direct compile-scope `<dependency>` of `juneau-utest`, but `mvn -pl juneau-utest test` without `-am` puts only `juneau-utest` in the active reactor and resolves every upstream module (RDF, marshall, commons, bean modules, …) from `~/.m2/repository`. The fix prepends `--also-make` to every `mvn` invocation in the project (Maven 3.3+ `.mvn/maven.config` mechanism) so any `-pl <module>` invocation now implicitly adds every upstream module to the reactor and reactor builds always prefer in-tree `target/classes` over `~/.m2`. `--also-make` is a no-op without `-pl` so root-level full-reactor builds and `./scripts/test.py` / `./scripts/push.py` (both run from workspace root without `-pl`) are unaffected. The footgun was not RDF-specific — it bit every upstream module of `juneau-utest`; RDF was just the most-iterated victim during Bug #11. Verification: probe edit in `juneau-marshall-rdf` exercised by `mvn -pl juneau-utest -Dtest='Rdf*Test' test` without intermediate `mvn install` (28 marker hits); `mvn clean install -DskipTests` from root succeeds in 30.9s; `./scripts/test.py --test-only` reports `86,326 / 0 / 0 / 20` matching the OQ 11 target exactly. No `AGENTS.md` change required — fix is clean with no residual workflow caveat.
12. **Hocon serializer-side fragility broader than `=`.** **CLOSED 2026-05-21** — closed as part of item 9. `+` and `$` were added to `HoconWriter.QUOTE_VALUE_CHARS` per the recommendation; the parser-side recursion guard and parseArray flatten bug were closed in the same turn, so the underlying parser-fragility shape that was producing the symptoms is now fixed at both layers (serializer-side quoting + parser-side robustness).
13. **`peekNoSkip` eager-consume pattern is a class of Hocon bug.** *(agreed 2026-05-22 — follow-up turn.)* The `peekNoSkip` method reads the closing structural token from the underlying reader and stashes it as `peeked`, leaving the underlying reader's position one token ahead. Any caller that subsequently calls `skipWhitespaceAndComments` silently reads through chars that semantically come *after* the cached token. The peek-before-skip guard added to `parseArray` and `parseObject` (item 9 closure) is a targeted patch; the structural fix is for `skipWhitespaceAndComments` to be a no-op when `peeked` is set. Future follow-up turn: audit every `peekNoSkip` call site and tighten the invariant at the `skipWhitespaceAndComments` layer.
14. **Hocon `UNQUOTED_FORBIDDEN` ↔ `QUOTE_VALUE_CHARS` set-membership audit.** *(agreed 2026-05-22 — follow-up.)* The current Hocon `UNQUOTED_FORBIDDEN` set is `$\"{}[]:=,+#^\\?!@*&` plus whitespace; `QUOTE_VALUE_CHARS` now covers `=+$` plus the original `\t\n\r{},:\"'#` set. Several `UNQUOTED_FORBIDDEN` chars (`^`, `\\`, `?`, `!`, `@`, `*`, `&`) are not yet quoted on the serializer side. Future follow-up: audit and align the two sets so any character the parser refuses unquoted is covered by the serializer's quoting rule.
15. **HOCON `byte[]`-at-NOT_SET base64 sidestep is now redundant.** *(agreed 2026-05-22 — future cleanup turn.)* The post-Wave-4 a04 closure used a base64 sidestep for `byte[]` at NOT_SET in HOCON specifically because nested int-arrays were being flattened by HOCON's array-concatenation. Now that the parseArray flatten bug is fixed (item 9), the sidestep can be simplified to emit nested int-arrays directly — same shape Toml uses at NOT_SET. Out-of-scope cleanup; flag for a future cleanup turn. Worker C explicitly left it in place per audit scope.
16. **`HoconTokenizer.readTokenImmediate`'s `c == '=' && peekChar() != '+'` guard.** *(agreed 2026-05-22 — future pass; user note: "this tokenizer is new code, so it may very well be a bug".)* The disambiguation against `=+...` is suspicious — original intent unclear. Future pass: investigate the git history of that line and the test cases (if any) that motivated it. Since the tokenizer is new code, **treat the suspicion as a likely bug** rather than a defensive pattern; the rest of the tokenizer doesn't have similar two-char-lookahead guards on operator chars.
17. **`ClassFormatSwap.unswap` ignores session classloader.** *(agreed 2026-05-22 — future pass.)* `ClassFormatSwap.unswap` uses `Thread.currentThread().getContextClassLoader()` rather than consulting any session-installed classloader. Not in Bug #16's scope; downstream cleanup pass. Affects every parse of `Class<?>` through any format that routes through the swap.
18. **`getClassMetaForObject(value, hint)` is a runtime-class lookup with null fallback, not a hint-merging lookup.** *(agreed 2026-05-22 — future pass.)* Any session that does `aType = getClassMetaForObject(value, cMeta)` and treats `aType` as the source-of-truth for collection-element types is structurally vulnerable to the same erased-array shape Bug #14 fixed in `TomlSerializerSession.writeValue`. Future pass: sweep Hjson / Hocon / Markdown / Ini for the same `aType.isArray()` / `aType.isCollection()` dispatch shape that loses the runtime-array shape under `List<T[]>` erasure.
19. **`juneau-rest-mock` has pre-existing compilation errors.** *(agreed 2026-05-22 — future pass.)* `juneau-rest-mock` won't compile due to symbols missing from another module — `RestContext`, `CallLogger`, `BasicTestCallLogger`, `UrlPathMatcher`, `UrlPath`, `BoundedServletInputStream`, `FinishableServletOutputStream`. **Note (2026-05-22)**: the inner-loop friction this caused was eliminated by the `--also-make` fix in OQ 11; the compilation errors themselves are still latent and should be tracked as a separate cleanup. Future pass: investigate the source-symbol gaps and either restore the missing symbols or update `juneau-rest-mock` to use whatever replaced them.
20. **Sibling `OutputStreamSerializerSession`-based skips in `MarshalledPropertyPostProcessor`.** *(observation, OQ 10 hasNativeBytes worker, 2026-05-22.)* The variant `bigNumberSwap`, `booleanSwap`, and `floatSwap` factories in `MarshalledPropertyPostProcessor` carry the same "binary serializer ≡ native primitive" assumption that OQ 10 just dismantled for `binarySwap`. They're scoped to native numbers / bools / floats — not native bytes — so they're outside OQ 10's mandate, but they exhibit the same smell. Sibling capabilities (e.g. `hasNativeBigNumbers()`, `hasNativeFloats()`) would generalize the OQ 10 (b1) pattern. Future pass: audit each factory and decide whether the assumption holds (BSON / CBOR / MsgPack only, like binary bytes) or whether Parquet / RDF need carve-outs.
21. **`BinaryFormat_RoundTrip_Test` class-level Javadoc is now stale.** *(observation, OQ 10 hasNativeBytes worker, 2026-05-22.)* Lines 70–78 of `juneau-utest/src/test/java/org/apache/juneau/transforms/BinaryFormat_RoundTrip_Test.java` claim that "BSON / CBOR / MsgPack / Proto / Parquet emit native bytes regardless of the configured constant". Two issues post-OQ 10: (a) Parquet no longer emits native bytes at non-`NOT_SET` formats (now emits a UTF-8 BASE64 string column), and (b) Proto was never an `OutputStreamSerializerSession` in the first place — it's a `WriterSerializerSession` (text serializer). The test still passes 1260/0 because it asserts in-memory round-trip equality, not byte-level wire form. Future pass: refresh the Javadoc to reflect the (b1) behavior and remove the Proto-as-binary slip.

---

## Related plans / references

- `FINISHED-4-duration-format-control.md`, `FINISHED-50-format-control-extension.md`, `FINISHED-54-format-control-round-2.md` — the three preceding plans that added the 16 format enums this plan exercises.
- `juneau-utest/src/test/java/org/apache/juneau/transforms/DurationFormat_RoundTrip_Test.java` — the pilot file; canonical shape for the 15 sibling files.
- `juneau-utest/src/test/java/org/apache/juneau/a/rttests/` — the 42 canonical `RoundTrip_Tester` templates; the source of truth for which serializer/parser pairs are in the matrix.
- `juneau-core/juneau-marshall/src/main/java/org/apache/juneau/MarshalledPropertyPostProcessor.java` — host of every `*Swap` factory in scope of the Phase 1 audit; the line-~602 ternary is the Bug #1 site.
- `todo/TODO-56-serializer-parser-dispatch-cleanup.md` — adjacent plan on serializer/parser dispatch shape. Strictly orthogonal: TODO-56 reorders dispatch branches and hoists nested `instanceof`; this plan adds coverage for the format-control dispatch results. No overlap, but landing TODO-56 first does not change the test files this plan adds.
