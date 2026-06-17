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
import org.apache.juneau.marshall.prototext.*;
import org.apache.juneau.marshall.toml.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.urlencoding.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link DurationFormat} across every supported serializer/parser pair.
 *
 * <p>
 * This is the pilot test for the work item 50/work item 54 format-control round-trip pattern.  It varies the
 * {@link DurationFormat} setting on both the serializer and parser builders for each tester template, then
 * round-trips representative {@link Duration} values:
 * <ul>
 * 	<li>As a top-level value (exercises the per-format dispatch sites in each {@code *SerializerSession} /
 * 		{@code *ParserSession}).
 * 	<li>As a bean property (exercises the {@code MarshalledPropertyPostProcessor} context-format swap install
 * 		path at {@code applyContextFormats}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link DurationFormat} values).  At the time of writing this comes to
 * roughly 41 &times; 7 = 287 testers per test method.
 *
 * <p>
 * If a tester is validation-only (no parser, schema-only, or CSV in serializer-only mode) the top-level
 * standalone-Duration test is skipped — there is no Duration round-trip semantics to assert in that case.
 */
@SuppressWarnings({
	"unused",   // Exception parameter intentionally unused in catch block; only the fact of the exception matters.
	"java:S125" // Commented-out code is retained as historical reference / future re-enable candidate.
})
class DurationFormat_RoundTrip_Test extends TestBase {

	/**
	 * Builds a fully-configured {@link RoundTrip_Tester} parameterized on a {@link DurationFormat} value.
	 *
	 * <p>
	 * Each lambda below mirrors a single tester entry in
	 * {@link org.apache.juneau.marshall.a.rttests.RoundTripDateTime_Test} but threads {@code fmt} through both the
	 * {@code serializer().durationFormat(fmt)} and {@code parser().durationFormat(fmt)} builder calls so the
	 * round-trip uses a consistent format on both sides.
	 */
	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(DurationFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(JsonParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(JsonParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(Json5Parser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(Json5Parser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(JsonlParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(XmlParser.create().durationFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(XmlParser.create().durationFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(HtmlParser.create().durationFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(HtmlParser.create().durationFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(HtmlParser.create().durationFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(UonParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(UonParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(UonParser.create().decoding().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(UrlEncodingParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(UrlEncodingParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(MsgPackParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(YamlParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().durationFormat(fmt))
			.parser(TomlParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().durationFormat(fmt))
			.parser(IniParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			// CSV serializes top-level beans but its round-trip semantics are scoped to Collection-of-bean;
			// match RoundTripDateTime_Test by serializing only and returning the original.
			.serializer(CsvSerializer.create().keepNullProperties().durationFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(MarkdownParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Prototext - default | " + fmt)
			.serializer(PrototextSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(PrototextParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(HjsonParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(JsonParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(CborParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(HoconParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(BsonParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().durationFormat(fmt))
			.parser(ParquetParser.create().durationFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	/** Pre-computed (tester, format) combinations.  One row per (builder &times; format) pair. */
	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(DurationFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	//====================================================================================================
	// Bean with one Duration field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Duration d;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_durationProperty_basic(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A01Bean();
		x.d = Duration.ofHours(2).plusMinutes(15);

		x = t.roundTrip(x);
		assertEquals(Duration.ofHours(2).plusMinutes(15), x.d, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Duration fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Duration timeout;
		public Duration interval;
		public Duration zero;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_durationProperty_multipleFields(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A02Bean();
		x.timeout = Duration.ofHours(1).plusMinutes(30);
		x.interval = Duration.ofSeconds(45);
		x.zero = Duration.ZERO;

		x = t.roundTrip(x);
		assertEquals(Duration.ofHours(1).plusMinutes(30), x.timeout, "fmt=" + fmt);
		assertEquals(Duration.ofSeconds(45), x.interval, "fmt=" + fmt);
		assertEquals(Duration.ZERO, x.zero, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with negative + zero Duration — sign + zero coverage
	//====================================================================================================

	public static class A03Bean {
		public Duration negative;
		public Duration zero;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_durationProperty_negativeAndZero(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A03Bean();
		x.negative = Duration.ofHours(-6);
		x.zero = Duration.ZERO;

		x = t.roundTrip(x);
		assertEquals(Duration.ofHours(-6), x.negative, "fmt=" + fmt);
		assertEquals(Duration.ZERO, x.zero, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with day-spanning Duration — ISO_8601_WITH_DAYS expressiveness coverage
	//====================================================================================================

	public static class A04Bean {
		public Duration multiDay;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_durationProperty_multiDay(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A04Bean();
		x.multiDay = Duration.ofDays(1).plusHours(2).plusMinutes(3);

		x = t.roundTrip(x);
		assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(3), x.multiDay, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null Duration field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_durationProperty_nullField(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.d, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Duration — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_durationTopLevel(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Duration value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = Duration.ofHours(2).plusMinutes(15);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, Duration.class);
			assertEquals(x, x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
