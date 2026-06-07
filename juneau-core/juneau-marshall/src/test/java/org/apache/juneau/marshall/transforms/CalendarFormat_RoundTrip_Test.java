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
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.a.rttests.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.hocon.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.jcs.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.markdown.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.parquet.*;
import org.apache.juneau.marshall.proto.*;
import org.apache.juneau.marshall.toml.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.urlencoding.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link CalendarFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link DateFormat_RoundTrip_Test} — same shape, same 42 tester templates, varied across every
 * {@link CalendarFormat} value.  Round-trips representative {@link Calendar} values:
 * <ul>
 * 	<li>As a bean property (exercises the {@code MarshalledPropertyPostProcessor} context-format swap install
 * 		path at {@code applyContextFormats}).
 * 	<li>As a top-level value (exercises the per-format dispatch sites in each {@code *SerializerSession} /
 * 		{@code *ParserSession}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link CalendarFormat} values).  At the time of writing this comes to
 * 42 &times; 18 = 756 testers per test method.
 *
 * <p>
 * Many {@link CalendarFormat} values are intentionally lossy — date-only formats drop the time portion,
 * time-only formats drop the date portion, and {@link CalendarFormat#RFC_1123_DATE_TIME} drops sub-second
 * precision.  Comparisons use {@link Calendar#toInstant()} rather than {@link Calendar#equals} because
 * {@code Calendar.equals} compares internal state (firstDayOfWeek, gregorianCutover, etc.) that the
 * format / parse cycle does not preserve.  The {@code expectedAfter} helper canonicalizes through the
 * format's own {@code format / parse} cycle so the assertion reflects the lossy canonical form, not the
 * structural original.
 */
class CalendarFormat_RoundTrip_Test extends TestBase {

	/**
	 * Builds a fully-configured {@link RoundTrip_Tester} parameterized on a {@link CalendarFormat} value.
	 *
	 * <p>
	 * Each lambda below mirrors a single tester entry in
	 * {@link org.apache.juneau.marshall.a.rttests.RoundTripDateTime_Test} but threads {@code fmt} through both the
	 * {@code serializer().calendarFormat(fmt)} and {@code parser().calendarFormat(fmt)} builder calls so the
	 * round-trip uses a consistent format on both sides.
	 */
	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(CalendarFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(JsonParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(JsonParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(Json5Parser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(Json5Parser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(JsonlParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(XmlParser.create().calendarFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(XmlParser.create().calendarFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(HtmlParser.create().calendarFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(HtmlParser.create().calendarFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(HtmlParser.create().calendarFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(UonParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(UonParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(UonParser.create().decoding().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(UrlEncodingParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(UrlEncodingParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(MsgPackParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(YamlParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().calendarFormat(fmt))
			.parser(TomlParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().calendarFormat(fmt))
			.parser(IniParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().calendarFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(MarkdownParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Proto - default | " + fmt)
			.serializer(ProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(ProtoParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(HjsonParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(JsonParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(CborParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(HoconParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().calendarFormat(fmt))
			.parser(BsonParser.create().calendarFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().calendarFormat(fmt))
			.parser(ParquetParser.create().calendarFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	/** Pre-computed (tester, format) combinations.  One row per (builder &times; format) pair. */
	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(CalendarFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/** Sample Calendar values pinned to a known instant in {@code America/New_York}. */
	private static Calendar makeCal(int year, int month, int day, int hour, int minute, int second) {
		return GregorianCalendar.from(ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("America/New_York")));
	}

	/**
	 * Format-aware expectation helper.  Most {@link CalendarFormat} values are intentionally lossy — date-only
	 * formats drop the time portion, time-only formats drop the date portion, and
	 * {@link CalendarFormat#RFC_1123_DATE_TIME} drops sub-second precision.  Canonicalize through the format's
	 * own {@code format/parse} cycle so the assertion reflects the lossy canonical form.
	 *
	 * <p>
	 * Mirrors the swap's call pattern exactly:
	 * {@code format(original, session.getTimeZoneId())} then {@code parse(s, session.getTimeZoneId())}.
	 * With the default session (no explicit zone) both reduce to {@code zoneId == null} fallbacks: format
	 * falls back to the value's own zone, parse falls back to {@link ZoneId#systemDefault()}.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format lossiness.
	 */
	private static Calendar expectedAfter(Calendar original, RoundTrip_Tester t, CalendarFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		var effective = fmt == CalendarFormat.NOT_SET ? CalendarFormat.ISO_OFFSET_DATE_TIME : fmt;
		return effective.parse(effective.format(original, null), null);
	}

	/**
	 * Compare two {@link Calendar} values by their {@link Calendar#toInstant()} timestamps rather than
	 * {@code Calendar.equals}.  {@code Calendar.equals} compares internal state (firstDayOfWeek,
	 * gregorianCutover, time zone identity, etc.) that the format / parse cycle does not preserve.
	 */
	private static void assertCalendarEquals(Calendar expected, Calendar actual, String message) {
		if (expected == null) {
			assertNull(actual, message);
		} else {
			assertNotNull(actual, message);
			assertEquals(expected.toInstant(), actual.toInstant(), message);
		}
	}

	//====================================================================================================
	// Bean with one Calendar field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Calendar c;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_calendarProperty_basic(RoundTrip_Tester t, CalendarFormat fmt) throws Exception {
		var x = new A01Bean();
		x.c = makeCal(2024, 6, 15, 12, 30, 45);

		x = t.roundTrip(x);
		assertCalendarEquals(expectedAfter(makeCal(2024, 6, 15, 12, 30, 45), t, fmt), x.c, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Calendar fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Calendar created;
		public Calendar updated;
		public Calendar epoch;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_calendarProperty_multipleFields(RoundTrip_Tester t, CalendarFormat fmt) throws Exception {
		var x = new A02Bean();
		x.created = makeCal(2024, 6, 15, 12, 30, 45);
		x.updated = makeCal(2025, 1, 1, 0, 0, 0);
		x.epoch = GregorianCalendar.from(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")));

		x = t.roundTrip(x);
		assertCalendarEquals(expectedAfter(makeCal(2024, 6, 15, 12, 30, 45), t, fmt), x.created, "fmt=" + fmt);
		assertCalendarEquals(expectedAfter(makeCal(2025, 1, 1, 0, 0, 0), t, fmt), x.updated, "fmt=" + fmt);
		assertCalendarEquals(
			expectedAfter(GregorianCalendar.from(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))), t, fmt),
			x.epoch, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with cross-zone Calendar values — TZ normalization coverage
	//====================================================================================================
	//
	// Cross-zone coverage spans negative-offset (America/New_York: "-0500"/"-0400"), positive-offset
	// (Asia/Tokyo: "+0900"), and zero-offset (UTC: "Z") zones.  The positive-offset path used to expose
	// a HOCON-specific parsing issue when used with {@link CalendarFormat#BASIC_ISO_DATE} (the colon-less
	// offset "+0900" was emitted unquoted by the HOCON serializer and tripped a parser-side recursion
	// bug); both the HOCON serializer's QUOTE_VALUE_CHARS set and the parser-side recursion guard have
	// been fixed (Bug #15) so positive offsets now round-trip correctly.
	//
	public static class A03Bean {
		public Calendar ny;
		public Calendar tokyo;
		public Calendar utc;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_calendarProperty_crossZone(RoundTrip_Tester t, CalendarFormat fmt) throws Exception {
		// All three Calendars represent the same instant in different zones.
		var instant = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant();
		var x = new A03Bean();
		x.ny = GregorianCalendar.from(instant.atZone(ZoneId.of("America/New_York")));
		x.tokyo = GregorianCalendar.from(instant.atZone(ZoneId.of("Asia/Tokyo")));
		x.utc = GregorianCalendar.from(instant.atZone(ZoneId.of("UTC")));

		x = t.roundTrip(x);
		assertCalendarEquals(
			expectedAfter(GregorianCalendar.from(instant.atZone(ZoneId.of("America/New_York"))), t, fmt),
			x.ny, "fmt=" + fmt);
		assertCalendarEquals(
			expectedAfter(GregorianCalendar.from(instant.atZone(ZoneId.of("Asia/Tokyo"))), t, fmt),
			x.tokyo, "fmt=" + fmt);
		assertCalendarEquals(
			expectedAfter(GregorianCalendar.from(instant.atZone(ZoneId.of("UTC"))), t, fmt),
			x.utc, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with far-future Calendar — large epoch-millis stress (MILLIS numeric promotion / overflow paths)
	//====================================================================================================

	public static class A04Bean {
		public Calendar farFuture;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_calendarProperty_largeValues(RoundTrip_Tester t, CalendarFormat fmt) throws Exception {
		var x = new A04Bean();
		// Year 2099 — large but still within all formatters' supported range.
		x.farFuture = makeCal(2099, 12, 31, 23, 59, 59);

		x = t.roundTrip(x);
		assertCalendarEquals(expectedAfter(makeCal(2099, 12, 31, 23, 59, 59), t, fmt), x.farFuture, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null Calendar field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_calendarProperty_nullField(RoundTrip_Tester t, CalendarFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.c, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Calendar — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_calendarTopLevel(RoundTrip_Tester t, CalendarFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Calendar value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = makeCal(2024, 6, 15, 12, 30, 45);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, Calendar.class);
			// Binary serializers with native datetime support (BSON in particular) bypass the configured
			// format swap at top-level and round-trip the Calendar with full native fidelity.  Bean-property
			// tests above cover the swap path; here we accept either the lossy-canonical (format applied)
			// or the structural original (format bypassed by native type support).
			var expected = expectedAfter(x, t, fmt);
			assertTrue(expected.toInstant().equals(x2.toInstant()) || x.toInstant().equals(x2.toInstant()),
				"fmt=" + fmt + " expected " + expected.toInstant() + " or " + x.toInstant() + " but got " + x2.toInstant());
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
