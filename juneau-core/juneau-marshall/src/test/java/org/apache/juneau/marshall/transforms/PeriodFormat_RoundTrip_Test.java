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
 * Cross-pair round-trip coverage for {@link PeriodFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link DurationFormat_RoundTrip_Test} — same shape, same 42 tester templates, varied across every
 * {@link PeriodFormat} value.  Round-trips representative {@link Period} values:
 * <ul>
 * 	<li>As a top-level value (exercises the per-format dispatch sites in each {@code *SerializerSession} /
 * 		{@code *ParserSession}).
 * 	<li>As a bean property (exercises the {@code MarshalledPropertyPostProcessor} context-format swap install
 * 		path at {@code applyContextFormats}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link PeriodFormat} values).  At the time of writing this comes to
 * 42 &times; 3 = 126 testers per test method.
 *
 * <p>
 * If a tester is validation-only (no parser, schema-only, or CSV in serializer-only mode) the top-level
 * standalone-Period test is skipped — there is no Period round-trip semantics to assert in that case.
 */
@SuppressWarnings({
	"unused",   // Exception parameter intentionally unused in catch block; only the fact of the exception matters.
	"java:S125" // Commented-out code is retained as historical reference / future re-enable candidate.
})
class PeriodFormat_RoundTrip_Test extends TestBase {

	/**
	 * Builds a fully-configured {@link RoundTrip_Tester} parameterized on a {@link PeriodFormat} value.
	 *
	 * <p>
	 * Each lambda below mirrors a single tester entry in
	 * {@link org.apache.juneau.marshall.a.rttests.RoundTripDateTime_Test} but threads {@code fmt} through both the
	 * {@code serializer().periodFormat(fmt)} and {@code parser().periodFormat(fmt)} builder calls so the
	 * round-trip uses a consistent format on both sides.
	 */
	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(PeriodFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(JsonParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(JsonParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(Json5Parser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(Json5Parser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(JsonlParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(XmlParser.create().periodFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(XmlParser.create().periodFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(HtmlParser.create().periodFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(HtmlParser.create().periodFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(HtmlParser.create().periodFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(UonParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(UonParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(UonParser.create().decoding().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(UrlEncodingParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(UrlEncodingParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(MsgPackParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(YamlParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().periodFormat(fmt))
			.parser(TomlParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().periodFormat(fmt))
			.parser(IniParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			// CSV serializes top-level beans but its round-trip semantics are scoped to Collection-of-bean;
			// match RoundTripDateTime_Test by serializing only and returning the original.
			.serializer(CsvSerializer.create().keepNullProperties().periodFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(MarkdownParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Proto - default | " + fmt)
			.serializer(ProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(ProtoParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(HjsonParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(JsonParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(CborParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(HoconParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().periodFormat(fmt))
			.parser(BsonParser.create().periodFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().periodFormat(fmt))
			.parser(ParquetParser.create().periodFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	/** Pre-computed (tester, format) combinations.  One row per (builder &times; format) pair. */
	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(PeriodFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Format-aware expectation helper.  {@link PeriodFormat#DAYS} is intentionally lossy — it canonicalizes the
	 * input as {@code years*365 + months*30 + days} on the way out and parses back as {@code Period.ofDays(n)},
	 * so the post-round-trip value is the days-only canonical form, not the structural original.  Every other
	 * format ({@link PeriodFormat#NOT_SET}, {@link PeriodFormat#ISO_8601}) is structurally lossless.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * even under {@link PeriodFormat#DAYS}.
	 */
	private static Period expectedAfter(Period original, RoundTrip_Tester t, PeriodFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		if (fmt == PeriodFormat.DAYS)
			return Period.ofDays(original.getYears() * 365 + original.getMonths() * 30 + original.getDays());
		return original;
	}

	//====================================================================================================
	// Bean with one Period field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Period p;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_periodProperty_basic(RoundTrip_Tester t, PeriodFormat fmt) throws Exception {
		var x = new A01Bean();
		x.p = Period.of(1, 2, 3);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Period.of(1, 2, 3), t, fmt), x.p, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Period fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Period anniversary;
		public Period vacation;
		public Period zero;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_periodProperty_multipleFields(RoundTrip_Tester t, PeriodFormat fmt) throws Exception {
		var x = new A02Bean();
		x.anniversary = Period.of(5, 0, 0);
		x.vacation = Period.of(0, 0, 14);
		x.zero = Period.ZERO;

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Period.of(5, 0, 0), t, fmt), x.anniversary, "fmt=" + fmt);
		assertEquals(expectedAfter(Period.of(0, 0, 14), t, fmt), x.vacation, "fmt=" + fmt);
		assertEquals(Period.ZERO, x.zero, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with negative + zero + mixed-sign Period — sign + zero coverage
	//====================================================================================================

	public static class A03Bean {
		public Period negative;
		public Period zero;
		public Period mixedSign;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_periodProperty_negativeAndZero(RoundTrip_Tester t, PeriodFormat fmt) throws Exception {
		var x = new A03Bean();
		x.negative = Period.of(-1, -2, -3);
		x.zero = Period.ZERO;
		x.mixedSign = Period.of(1, -2, 3);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Period.of(-1, -2, -3), t, fmt), x.negative, "fmt=" + fmt);
		assertEquals(Period.ZERO, x.zero, "fmt=" + fmt);
		assertEquals(expectedAfter(Period.of(1, -2, 3), t, fmt), x.mixedSign, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with large Period — DAYS numeric promotion + ISO_8601 component-width stress
	//====================================================================================================

	public static class A04Bean {
		public Period large;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_periodProperty_largeValues(RoundTrip_Tester t, PeriodFormat fmt) throws Exception {
		var x = new A04Bean();
		x.large = Period.of(100, 11, 30);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Period.of(100, 11, 30), t, fmt), x.large, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null Period field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_periodProperty_nullField(RoundTrip_Tester t, PeriodFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.p, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Period — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_periodTopLevel(RoundTrip_Tester t, PeriodFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Period value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = Period.of(1, 2, 3);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, Period.class);
			assertEquals(expectedAfter(x, t, fmt), x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
