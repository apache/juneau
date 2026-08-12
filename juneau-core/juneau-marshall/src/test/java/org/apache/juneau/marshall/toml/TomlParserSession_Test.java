/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.toml;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link TomlParserSession}.
 *
 * <p>Targets doRead's null/empty/_value-wrapper dispatch, the readTomlDocument main loop
 * (table headers, array-of-tables, dotted keys, redefinition errors), readValue's full type
 * dispatch (strings, booleans, arrays, inline tables, radix integers, special floats,
 * date-like literals, invalid-number errors), and convertValue's Map/List/Number/String/
 * TemporalAccessor coercion paths.
 */
class TomlParserSession_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a. doRead top-level dispatch
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_nullInputReturnsNull() throws Exception {
		var r = TomlParser.DEFAULT.read((Object) null, JsonMap.class);
		assertNull(r);
	}

	@Test void a02_emptyDocumentReturnsNewInstanceWhenInstantiable() throws Exception {
		var r = TomlParser.DEFAULT.read("", A02_Bean.class);
		assertNotNull(r);
	}

	@Test void a03_emptyDocumentReturnsNullWhenNotInstantiable() throws Exception {
		// Map targets can't be "newInstance()"d via canCreateNewBean, so an empty document
		// parsed to a non-bean, non-instantiable target returns null.
		var r = TomlParser.DEFAULT.read("   \n  ", A03_Iface.class);
		assertNull(r);
	}

	@Test void a04_valueWrapperUnwrapsToScalar() throws Exception {
		var r = TomlParser.DEFAULT.read("_value = 42", Integer.class);
		assertEquals(42, r);
	}

	@Test void a05_directMapToType() throws Exception {
		var r = TomlParser.DEFAULT.read("name = \"Alice\"\nage = 30", JsonMap.class);
		assertEquals("Alice", r.getString("name"));
		assertEquals(30, r.getInt("age"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b. readTomlDocument main loop
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_simpleTableHeader() throws Exception {
		var r = TomlParser.DEFAULT.read("[server]\nhost = \"localhost\"", JsonMap.class);
		var server = r.getMap("server");
		assertEquals("localhost", server.get("host"));
	}

	@Test void b02_arrayOfTablesFirstOccurrence() throws Exception {
		var r = TomlParser.DEFAULT.read("[[fruits]]\nname = \"apple\"", JsonMap.class);
		var list = r.getList("fruits");
		assertEquals(1, list.size());
	}

	@Test void b02b_arrayOfTablesSecondOccurrenceAppends() throws Exception {
		var r = TomlParser.DEFAULT.read("[[fruits]]\nname = \"apple\"\n[[fruits]]\nname = \"pear\"", JsonMap.class);
		var list = r.getList("fruits");
		assertEquals(2, list.size());
	}

	@Test void b03_arrayOfTablesRedefinedAsScalarThrows() throws Exception {
		assertThrows(ParseException.class,
			() -> TomlParser.DEFAULT.read("fruits = 1\n[[fruits]]\nname = \"apple\"", JsonMap.class));
	}

	@Test void b04_dottedKeyPath() throws Exception {
		var r = TomlParser.DEFAULT.read("a.b.c = 1", JsonMap.class);
		var a = r.getMap("a");
		var b = (Map<?,?>) a.get("b");
		assertEquals(1L, b.get("c"));
	}

	@Test void b05_emptyKeyThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("= 1", JsonMap.class));
	}

	@Test void b06_missingEqualsThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("foo 1", JsonMap.class));
	}

	@Test void b07_unrecognizedLeadingCharacterBreaksLoop() throws Exception {
		// '}' at document top level matches none of the dispatch conditions ('[', key-start chars),
		// so the main loop's else-branch breaks immediately, yielding an empty document.
		var r = TomlParser.DEFAULT.read("}", JsonMap.class);
		assertTrue(r == null || r.isEmpty());
	}

	@Test void b08_keyStartingWithUnderscoreOrDash() throws Exception {
		var r = TomlParser.DEFAULT.read("_a = 1\n-b = 2", JsonMap.class);
		assertEquals(1L, r.get("_a"));
		assertEquals(2L, r.get("-b"));
	}

	@Test void b09_quotedKeyAtTopLevel() throws Exception {
		// A leading '"' or '\'' also satisfies the main loop's key-start dispatch condition.
		var r = TomlParser.DEFAULT.read("'a.b' = 1\n\"c d\" = 2", JsonMap.class);
		assertEquals(1L, r.get("a.b"));
		assertEquals(2L, r.get("c d"));
	}

	@Test void b10_dottedArrayOfTablesHeader() throws Exception {
		// A multi-segment array-of-tables path exercises getOrCreateAt()/setAt()'s intermediate-segment
		// loop (parts.length > 1), unlike the single-segment "[[fruits]]" header in b02.
		var r = TomlParser.DEFAULT.read("[[fruits.varieties]]\nname = \"red\"", JsonMap.class);
		var fruits = r.getMap("fruits");
		var varieties = fruits.getList("varieties");
		assertEquals(1, varieties.size());
	}

	@Test void b11_valueTerminatedByTabOrHash() throws Exception {
		// readUntilValueEnd()'s terminator scan must recognize '\t' and an immediately-adjacent '#'
		// (no space) as stop characters, not just ' '/','/']'/'}'.
		var r = TomlParser.DEFAULT.read("a = 1\tb = 2#c\n", JsonMap.class);
		assertEquals(1L, r.get("a"));
		assertEquals(2L, r.get("b"));
	}

	@Test void b11b_valueTerminatedByBareCarriageReturn() throws Exception {
		// readUntilValueEnd()'s terminator scan must also recognize a bare '\r' (CRLF line ending)
		// as a stop character, distinct from the '\n' case exercised implicitly by every other test.
		var r = TomlParser.DEFAULT.read("a = 1\r\nb = 2\r\n", JsonMap.class);
		assertEquals(1L, r.get("a"));
		assertEquals(2L, r.get("b"));
	}

	@Test void b12_emptyTableHeader() throws Exception {
		// readTablePath's "key == null" break fires immediately when the path is empty (no bare-key
		// or quoted-key characters before the closing ']'), unlike every other table-header test
		// where the loop always finds at least one path segment before breaking on '.'/']'.
		var r = TomlParser.DEFAULT.read("[]\na = 1", JsonMap.class);
		var empty = r.getMap("");
		assertEquals(1L, empty.get("a"));
	}

	@Test void b13_missingClosingBracketOnTableHeaderAtEof() throws Exception {
		// readTablePath's "if (t.peek() == ']') t.read();" checks normally find ']' to consume for a
		// well-formed "[table]" header; this covers their false arm instead -- input reaches EOF
		// before any ']' appears, but the table path itself ("table") was still read successfully,
		// so parsing tolerantly proceeds and registers an empty table under that (unterminated) name.
		var r = TomlParser.DEFAULT.read("[table", JsonMap.class);
		assertTrue(r.containsKey("table"));
		assertTrue(((Map<?,?>) r.get("table")).isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c. readValue type dispatch
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_tripleQuoteBasicString() throws Exception {
		var r = TomlParser.DEFAULT.read("s = \"\"\"hello\"\"\"", JsonMap.class);
		assertEquals("hello", r.getString("s"));
	}

	@Test void c02_literalString() throws Exception {
		var r = TomlParser.DEFAULT.read("s = 'raw\\path'", JsonMap.class);
		assertEquals("raw\\path", r.getString("s"));
	}

	@Test void c03_tripleQuoteLiteralString() throws Exception {
		var r = TomlParser.DEFAULT.read("s = '''raw\\path'''", JsonMap.class);
		assertEquals("raw\\path", r.getString("s"));
	}

	@Test void c04_booleanTrueAndFalse() throws Exception {
		var r = TomlParser.DEFAULT.read("a = true\nb = false", JsonMap.class);
		assertEquals(true, r.get("a"));
		assertEquals(false, r.get("b"));
	}

	@Test void c05_inlineArrayWithTrailingComma() throws Exception {
		var r = TomlParser.DEFAULT.read("a = [1, 2, 3,]", JsonMap.class);
		var list = r.getList("a");
		assertEquals(3, list.size());
	}

	@Test void c06_inlineTable() throws Exception {
		var r = TomlParser.DEFAULT.read("point = { x = 1, y = 2 }", JsonMap.class);
		var point = r.getMap("point");
		assertEquals(1L, point.get("x"));
		assertEquals(2L, point.get("y"));
	}

	@Test void c06b_inlineTableMissingEqualsThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("point = { x 1 }", JsonMap.class));
	}

	@Test void c06c_inlineTableEmptyKeyThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("point = { = 1 }", JsonMap.class));
	}

	@Test void c07_hexIntegerLiteral() throws Exception {
		var r = TomlParser.DEFAULT.read("a = 0xFF", JsonMap.class);
		assertEquals(255L, r.get("a"));
	}

	@Test void c07b_octalIntegerLiteral() throws Exception {
		var r = TomlParser.DEFAULT.read("a = 0o17", JsonMap.class);
		assertEquals(15L, r.get("a"));
	}

	@Test void c07c_binaryIntegerLiteral() throws Exception {
		var r = TomlParser.DEFAULT.read("a = 0b101", JsonMap.class);
		assertEquals(5L, r.get("a"));
	}

	@Test void c08_signedSpecialFloats() throws Exception {
		var r = TomlParser.DEFAULT.read("a = +inf\nb = -inf\nc = +nan", JsonMap.class);
		assertEquals(Double.POSITIVE_INFINITY, r.get("a"));
		assertEquals(Double.NEGATIVE_INFINITY, r.get("b"));
		assertTrue(Double.isNaN((Double) r.get("c")));
	}

	@Test void c08b_bareSpecialFloats() throws Exception {
		// Leading char 'i'/'n' (no sign) dispatches through the separate inf/nan-only branch.
		var r = TomlParser.DEFAULT.read("a = inf\nb = nan", JsonMap.class);
		assertEquals(Double.POSITIVE_INFINITY, r.get("a"));
		assertTrue(Double.isNaN((Double) r.get("b")));
	}

	@Test void c08c_bareSpecialFloatInvalidThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("a = ignored", JsonMap.class));
	}

	@Test void c09_yearMonthLikeStringKeptAsString() throws Exception {
		// "2024-06" matches isDateLikeString (length 7, dash at index 4) and is kept as a raw
		// String for convertValue to later coerce into YearMonth/Year on the bean-property path.
		var r = TomlParser.DEFAULT.read("ym = 2024-06", JsonMap.class);
		assertEquals("2024-06", r.get("ym"));
	}

	@Test void c10_localDateTimeLiteral() throws Exception {
		var r = TomlParser.DEFAULT.read("ts = 2024-01-15T10:30:00", JsonMap.class);
		assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), r.get("ts"));
	}

	@Test void c11_floatWithDecimalPoint() throws Exception {
		var r = TomlParser.DEFAULT.read("pi = 3.14", JsonMap.class);
		assertEquals(3.14, r.get("pi"));
	}

	@Test void c12_invalidNumberThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("a = 123x", JsonMap.class));
	}

	@Test void c13_numberLikeTimeStringFallsBackToString() throws Exception {
		// "12:30" fails Long.parseLong but contains ':', so it falls through as a raw String
		// (line: "if (s.contains(\"-\") || s.contains(\":\")) return s;").
		var r = TomlParser.DEFAULT.read("t = 12:30", JsonMap.class);
		assertEquals("12:30", r.get("t"));
	}

	@Test void c15_unexpectedCharacterInValueThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("a = @", JsonMap.class));
	}

	@Test void c16_negativeNanLiteral() throws Exception {
		var r = TomlParser.DEFAULT.read("a = -nan", JsonMap.class);
		assertTrue(Double.isNaN((Double) r.get("a")));
	}

	@Test void c17_exponentWithoutDecimalPoint() throws Exception {
		var r = TomlParser.DEFAULT.read("a = 1e10", JsonMap.class);
		assertEquals(1e10, r.get("a"));
	}

	@Test void c18_dateTimeWithColonButNoTOrSpaceSeparator() throws Exception {
		// readValue's date-time dispatch OR-condition also accepts a bare ":" (without "T") as long
		// as there's no separator between the date and time parts -- an unusual but syntactically
		// reachable shape that routes into TomlTokenizer.parseDateTimeString(), which in turn can't
		// make sense of the un-separated date+time text and throws.
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("ts = 2024-01-1510:30:00", JsonMap.class));
	}

	@Test void c18b_spaceSeparatedDateTimeLiteralNowRecognized() throws Exception {
		// FIXED: the standard TOML space-separated datetime variant, e.g. "2024-01-15 10:30:00", used to
		// never be recognized by readValue's date-time dispatch at all -- readUntilValueEnd() treats ' '
		// as a hard value terminator, so the scan was truncated to just the bare date "2024-01-15" before
		// the time portion was ever read, silently dropping the time and mis-parsing the value as a plain
		// LocalDate-shaped string. readValue now special-cases a bare 10-char date immediately followed by
		// " <digit>" and continues reading the time portion past the space instead of stopping there.
		var r = TomlParser.DEFAULT.read("ts = 2024-01-15 10:30:00", JsonMap.class);
		assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), r.get("ts"));
	}

	@Test void c19_uppercaseRadixPrefixes() throws Exception {
		var r = TomlParser.DEFAULT.read("a = 0XFF\nb = 0O17\nc = 0B101", JsonMap.class);
		assertEquals(255L, r.get("a"));
		assertEquals(15L, r.get("b"));
		assertEquals(5L, r.get("c"));
	}

	@Test void c20_hexPrefixWithNoDigitsThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("a = 0x", JsonMap.class));
	}

	@Test void c21_radixIntegerOverflowThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("a = 0xFFFFFFFFFFFFFFFFF", JsonMap.class));
	}

	@Test void c22_unterminatedLiteralStringAtEofThrows() throws Exception {
		// isTripleQuote()'s c2 < 0 arm: a single opening quote immediately followed by EOF.
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("a = '", JsonMap.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d. convertValue dispatch
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_mapToJsonMapTarget() throws Exception {
		var r = TomlParser.DEFAULT.read("inner = { a = 1 }", D01_JsonMapBean.class);
		assertEquals(1, r.inner.getInt("a"));
	}

	@Test void d02_mapToBeanTarget() throws Exception {
		var r = TomlParser.DEFAULT.read("[inner]\na = \"x\"", D02_OuterBean.class);
		assertEquals("x", r.inner.a);
	}

	@Test void d03_listToCollectionTarget() throws Exception {
		var r = TomlParser.DEFAULT.read("items = [1, 2, 3]", D03_ListBean.class);
		assertEquals(List.of(1, 2, 3), r.items);
	}

	@Test void d04_listToArrayTarget() throws Exception {
		var r = TomlParser.DEFAULT.read("items = [1, 2, 3]", D04_ArrayBean.class);
		assertArrayEquals(new int[]{1, 2, 3}, r.items);
	}

	@Test void d05_topLevelNumberIntoDuration() throws Exception {
		// Bare integer literal is sniffed by Iso8601Utils.parseDuration and interpreted per the
		// configured DurationFormat hint (MILLIS here) rather than as strict ISO-8601 text.
		var p = TomlParser.create().durationFormat(DurationFormat.MILLIS).build();
		var r = p.read("_value = 30000", Duration.class);
		assertEquals(Duration.ofMillis(30000), r);
	}

	@Test void d06_topLevelNumberIntoPeriod() throws Exception {
		var p = TomlParser.create().periodFormat(PeriodFormat.DAYS).build();
		var r = p.read("_value = 5", Period.class);
		assertEquals(Period.ofDays(5), r);
	}

	@Test void d07_topLevelNumberIntoDate() throws Exception {
		var p = TomlParser.create().dateFormat(DateFormat.MILLIS).build();
		var r = p.read("_value = 1699999999000", java.util.Date.class);
		assertEquals(1699999999000L, r.getTime());
	}

	@Test void d08_topLevelNumberIntoCalendar() throws Exception {
		var p = TomlParser.create().calendarFormat(CalendarFormat.MILLIS).build();
		var r = p.read("_value = 1699999999000", java.util.Calendar.class);
		assertEquals(1699999999000L, r.getTimeInMillis());
	}

	@Test void d09_topLevelNumberIntoTemporal() throws Exception {
		var p = TomlParser.create().temporalFormat(TemporalFormat.MILLIS).build();
		var r = p.read("_value = 1699999999000", Instant.class);
		assertEquals(1699999999000L, r.toEpochMilli());
	}

	@Test void d10_topLevelStringIntoDate() throws Exception {
		var r = TomlParser.DEFAULT.read("_value = \"2023-01-01T00:00:00Z\"", java.util.Date.class);
		assertNotNull(r);
	}

	@Test void d10b_topLevelStringIntoCalendar() throws Exception {
		var r = TomlParser.DEFAULT.read("_value = \"2023-01-01T00:00:00Z\"", java.util.Calendar.class);
		assertNotNull(r);
	}

	@Test void d10c_topLevelStringIntoTemporal() throws Exception {
		var r = TomlParser.DEFAULT.read("_value = \"2023-01-01T00:00:00Z\"", Instant.class);
		assertNotNull(r);
	}

	@Test void d10d_topLevelStringIntoDuration() throws Exception {
		var r = TomlParser.DEFAULT.read("_value = \"PT30S\"", Duration.class);
		assertEquals(Duration.ofSeconds(30), r);
	}

	@Test void d10e_topLevelStringIntoPeriod() throws Exception {
		var r = TomlParser.DEFAULT.read("_value = \"P1Y2M3D\"", Period.class);
		assertEquals(Period.of(1, 2, 3), r);
	}

	@Test void d11_nativeTemporalLiteralIntoTemporalTarget() throws Exception {
		// Native TOML datetime literal (unquoted) is tokenized as a java.time.* TemporalAccessor
		// by readValue's date-time branch, then re-routed through readTemporal by convertValue.
		var r = TomlParser.DEFAULT.read("_value = 2024-01-15T10:30:00Z", OffsetDateTime.class);
		assertNotNull(r);
	}

	@Test void d12_topLevelByteArrayWithRealBinarySwap() throws Exception {
		var s = TomlSerializer.create().binaryFormat(BinaryFormat.HEX).build();
		var p = TomlParser.create().binaryFormat(BinaryFormat.HEX).build();
		var toml = s.write(new byte[]{9, 8, 7});
		var result = p.read(toml, byte[].class);
		assertArrayEquals(new byte[]{9, 8, 7}, result);
	}

	@Test void d13_nullValueSentinelConfigured() throws Exception {
		var r = TomlParser.DEFAULT.read("name = \"<NULL>\"", D13_Bean.class);
		assertNull(r.name);
	}

	@Test void d14_mapPropertyRawMapNotJsonMapNotBean() throws Exception {
		// convertValue's Map branch: not JsonMap.class-assignable and not isBean() -- falls through
		// past both guards to the generic Map-target handling below (targetType.isMap()).
		var r = TomlParser.DEFAULT.read("[props]\na = 1", D14_RawMapBean.class);
		assertEquals(1L, r.props.get("a"));
	}

	@Test void d15_nestedMapWithinJsonMapTarget() throws Exception {
		// toJsonMap's recursive "v instanceof Map" arm for a nested table inside a JsonMap-typed
		// bean property (as opposed to d01's single-level table).
		var r = TomlParser.DEFAULT.read("inner = { a = { b = 1 } }", D01_JsonMapBean.class);
		var nested = (JsonMap) r.inner.get("a");
		assertEquals(1L, nested.get("b"));
	}

	@Test void d16_mapTargetInterfaceUsesGenericMap() throws Exception {
		// convertMapToType's Map branch: Map.class (an interface) isn't directly instantiable via
		// canCreateNewInstance(), so it falls back to newGenericMap() instead of newInstance().
		var r = TomlParser.DEFAULT.read("a = 1", Map.class);
		assertEquals(1L, r.get("a"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e. populateBeanMap unknown-property handling
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_unknownPropertyIgnoredWhenConfigured() throws Exception {
		var p = TomlParser.create().ignoreUnknownBeanProperties().build();
		var r = p.read("name = \"Alice\"\nunknown = \"x\"", D13_Bean.class);
		assertEquals("Alice", r.name);
	}

	@Test void e02_unknownPropertyThrows() throws Exception {
		assertThrows(ParseException.class, () -> TomlParser.DEFAULT.read("unknown = \"x\"", D13_Bean.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f. RecordReadable delegation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_readRecordsDelegatesToRecordAdapter() throws Exception {
		try (var reader = TomlParser.DEFAULT.readRecords("name = \"Alice\"")) {
			assertTrue(reader.canRead());
			var bean = reader.read(D13_Bean.class);
			assertEquals("Alice", bean.name);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test fixture beans
	//-----------------------------------------------------------------------------------------------------------------

	public static class A02_Bean {
		public String name;
	}

	public interface A03_Iface {
		// Not instantiable; used only to force canCreateNewBean(...) == false.
	}

	public static class D01_JsonMapBean {
		public JsonMap inner;
	}

	public static class D02_InnerBean {
		public String a;
	}

	public static class D02_OuterBean {
		public D02_InnerBean inner;
	}

	public static class D03_ListBean {
		public List<Integer> items;
	}

	public static class D04_ArrayBean {
		public int[] items;
	}

	public static class D13_Bean {
		public String name;
	}

	public static class D14_RawMapBean {
		public Map<String,Object> props;
	}
}
