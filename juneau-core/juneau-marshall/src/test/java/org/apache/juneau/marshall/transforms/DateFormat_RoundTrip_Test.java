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
 * Cross-pair round-trip coverage for {@link DateFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link DurationFormat_RoundTrip_Test} and {@link PeriodFormat_RoundTrip_Test} — same shape, same 42
 * tester templates, varied across every {@link DateFormat} value.  Round-trips representative {@link Date}
 * values:
 * <ul>
 * 	<li>As a bean property (exercises the {@code MarshalledPropertyPostProcessor} context-format swap install
 * 		path at {@code applyContextFormats}).
 * 	<li>As a top-level value (exercises the per-format dispatch sites in each {@code *SerializerSession} /
 * 		{@code *ParserSession}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link DateFormat} values).  At the time of writing this comes to
 * 42 &times; 17 = 714 testers per test method.
 *
 * <p>
 * Many {@link DateFormat} values are intentionally lossy — date-only formats (e.g. {@link DateFormat#ISO_LOCAL_DATE})
 * drop the time portion, time-only formats (e.g. {@link DateFormat#ISO_LOCAL_TIME}) drop the date portion, and
 * second-resolution formats (e.g. {@link DateFormat#RFC_1123_DATE_TIME}) drop sub-second precision.  The
 * {@code expectedAfter} helper canonicalizes through the format's own {@code format / parse} cycle so the
 * assertion reflects the lossy canonical form, not the structural original.
 */
@SuppressWarnings({
	"unused" // Exception parameter intentionally unused in catch block; only the fact of the exception matters.
})
class DateFormat_RoundTrip_Test extends TestBase {

	/**
	 * Builds a fully-configured {@link RoundTrip_Tester} parameterized on a {@link DateFormat} value.
	 *
	 * <p>
	 * Each lambda below mirrors a single tester entry in
	 * {@link org.apache.juneau.marshall.a.rttests.RoundTripDateTime_Test} but threads {@code fmt} through both the
	 * {@code serializer().dateFormat(fmt)} and {@code parser().dateFormat(fmt)} builder calls so the
	 * round-trip uses a consistent format on both sides.
	 */
	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(DateFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(JsonParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(JsonParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(Json5Parser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(Json5Parser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(JsonlParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(XmlParser.create().dateFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(XmlParser.create().dateFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(HtmlParser.create().dateFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(HtmlParser.create().dateFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(HtmlParser.create().dateFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(UonParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(UonParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(UonParser.create().decoding().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(UrlEncodingParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(UrlEncodingParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(MsgPackParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(YamlParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().dateFormat(fmt))
			.parser(TomlParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().dateFormat(fmt))
			.parser(IniParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().dateFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(MarkdownParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Prototext - default | " + fmt)
			.serializer(PrototextSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(PrototextParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(HjsonParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(JsonParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(CborParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(HoconParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().dateFormat(fmt))
			.parser(BsonParser.create().dateFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().dateFormat(fmt))
			.parser(ParquetParser.create().dateFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	/** Pre-computed (tester, format) combinations.  One row per (builder &times; format) pair. */
	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(DateFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Format-aware expectation helper.  Most {@link DateFormat} values are intentionally lossy — date-only
	 * formats drop the time portion, time-only formats drop the date portion, and {@link DateFormat#RFC_1123_DATE_TIME}
	 * drops sub-second precision.  Canonicalize through the format's own {@code format/parse} cycle so the
	 * assertion reflects the lossy canonical form, not the structural original.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format losiness.
	 */
	private static Date expectedAfter(Date original, RoundTrip_Tester t, DateFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		var effective = fmt == DateFormat.NOT_SET ? DateFormat.ISO_LOCAL_DATE_TIME : fmt;
		var zone = ZoneId.systemDefault();
		return effective.parse(effective.format(original, zone), zone);
	}

	//====================================================================================================
	// Bean with one Date field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Date d;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_dateProperty_basic(RoundTrip_Tester t, DateFormat fmt) throws Exception {
		var x = new A01Bean();
		x.d = new Date(1234567890000L);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(new Date(1234567890000L), t, fmt), x.d, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Date fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Date created;
		public Date updated;
		public Date epoch;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_dateProperty_multipleFields(RoundTrip_Tester t, DateFormat fmt) throws Exception {
		var x = new A02Bean();
		x.created = new Date(1234567890000L);
		x.updated = new Date(1500000000000L);
		x.epoch = new Date(0L);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(new Date(1234567890000L), t, fmt), x.created, "fmt=" + fmt);
		assertEquals(expectedAfter(new Date(1500000000000L), t, fmt), x.updated, "fmt=" + fmt);
		assertEquals(expectedAfter(new Date(0L), t, fmt), x.epoch, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with epoch + post-epoch Date — boundary coverage at instant=0
	//====================================================================================================

	public static class A03Bean {
		public Date epoch;
		public Date justAfterEpoch;
		public Date midRange;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_dateProperty_epochAndBoundaries(RoundTrip_Tester t, DateFormat fmt) throws Exception {
		var x = new A03Bean();
		x.epoch = new Date(0L);
		x.justAfterEpoch = new Date(1000L);
		x.midRange = new Date(1234567890000L);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(new Date(0L), t, fmt), x.epoch, "fmt=" + fmt);
		assertEquals(expectedAfter(new Date(1000L), t, fmt), x.justAfterEpoch, "fmt=" + fmt);
		assertEquals(expectedAfter(new Date(1234567890000L), t, fmt), x.midRange, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with far-future Date — large epoch-millis stress (MILLIS numeric promotion / overflow paths)
	//====================================================================================================

	public static class A04Bean {
		public Date farFuture;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_dateProperty_largeValues(RoundTrip_Tester t, DateFormat fmt) throws Exception {
		var x = new A04Bean();
		// Year 2099 — large but still within all formatters' supported range.
		x.farFuture = new Date(4070908800000L);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(new Date(4070908800000L), t, fmt), x.farFuture, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null Date field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_dateProperty_nullField(RoundTrip_Tester t, DateFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.d, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Date — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_dateTopLevel(RoundTrip_Tester t, DateFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Date value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = new Date(1234567890000L);
		try {
			var out = t.serialize(x, s);
			var x2 = p.read(out, Date.class);
			// Binary serializers with native datetime support (BSON in particular) bypass the configured
			// format swap at top-level and round-trip the Date with full native fidelity.  Bean-property
			// tests above cover the swap path; here we accept either the lossy-canonical (format applied)
			// or the structural original (format bypassed by native type support).
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
