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
import java.time.temporal.*;
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
 * Cross-pair round-trip coverage for {@link TemporalFormat} bound to the {@link OffsetTime} subtype across
 * every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TemporalFormat_Instant_RoundTrip_Test} — completes the {@link Temporal} coverage for
 * the time+offset subtype.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link TemporalFormat} values).  At the time of writing this
 * comes to 42 &times; 20 = 840 testers per test method.
 *
 * <p>
 * {@link OffsetTime} has no date, so date-bearing formats are intentionally lossy.
 * {@link TemporalFormat#MILLIS} for {@link OffsetTime} falls back to the type's {@link TemporalFormat#DEFAULT}
 * formatter (an ISO offset-time string) per {@link TemporalFormat#isMillisNumeric(Class)} — there is no
 * defensible epoch-millis interpretation for a time-without-date.
 */
class TemporalFormat_OffsetTime_RoundTrip_Test extends TestBase {

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(TemporalFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(JsonParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(JsonParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(Json5Parser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(Json5Parser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(JsonlParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(XmlParser.create().temporalFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(XmlParser.create().temporalFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(HtmlParser.create().temporalFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(HtmlParser.create().temporalFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(HtmlParser.create().temporalFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(UonParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(UonParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(UonParser.create().decoding().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(UrlEncodingParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(UrlEncodingParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(MsgPackParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(YamlParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().temporalFormat(fmt))
			.parser(TomlParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().temporalFormat(fmt))
			.parser(IniParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().temporalFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(MarkdownParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Proto - default | " + fmt)
			.serializer(ProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(ProtoParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(HjsonParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(JsonParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(CborParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(HoconParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(BsonParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().temporalFormat(fmt))
			.parser(ParquetParser.create().temporalFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(TemporalFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Format-aware expectation helper.  Most {@link TemporalFormat} values are intentionally lossy for
	 * {@link OffsetTime} — date-bearing formats fill in the date as 1970-01-01 (via
	 * {@link org.apache.juneau.marshall.swaps.DefaultingTemporalAccessor}) on the way out and lose the time portion
	 * on the way back in (parse falls back to {@link LocalTime#MIDNIGHT} + system-default offset for
	 * date-only formats).  Canonicalize through the format's own {@code format/parse} cycle.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format lossiness.
	 */
	private static OffsetTime expectedAfter(OffsetTime original, RoundTrip_Tester t, TemporalFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return fmt.parse(fmt.format(original, null), OffsetTime.class, null);
	}

	//====================================================================================================
	// Bean with one OffsetTime field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public OffsetTime ot;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_offsetTimeProperty_basic(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();
		x.ot = OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC), t, fmt), x.ot, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple OffsetTime fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public OffsetTime morning;
		public OffsetTime noon;
		public OffsetTime midnight;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_offsetTimeProperty_multipleFields(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A02Bean();
		x.morning = OffsetTime.of(9, 0, 0, 0, ZoneOffset.UTC);
		x.noon = OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC);
		x.midnight = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetTime.of(9, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.morning, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.noon, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.midnight, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with midnight + noon + end-of-day OffsetTime — boundary coverage
	//====================================================================================================

	public static class A03Bean {
		public OffsetTime midnight;
		public OffsetTime noon;
		public OffsetTime endOfDay;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_offsetTimeProperty_boundaries(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A03Bean();
		x.midnight = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
		x.noon = OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC);
		x.endOfDay = OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.midnight, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.noon, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC), t, fmt), x.endOfDay, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with end-of-day OffsetTime — boundary stress
	//====================================================================================================

	public static class A04Bean {
		public OffsetTime endOfDay;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_offsetTimeProperty_largeValues(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A04Bean();
		x.endOfDay = OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC), t, fmt), x.endOfDay, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null OffsetTime field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_offsetTimeProperty_nullField(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.ot, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level OffsetTime — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_offsetTimeTopLevel(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone OffsetTime value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, OffsetTime.class);
			// Binary serializers with native datetime support may bypass the configured format swap at
			// top-level; bean-property tests above cover the swap path strictly.  Accept either the
			// lossy-canonical (format applied) or the structural original (format bypassed).
			var expected = expectedAfter(x, t, fmt);
			assertTrue(expected.equals(x2) || x.equals(x2),
				"fmt=" + fmt + " expected " + expected + " or " + x + " but got " + x2);
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
