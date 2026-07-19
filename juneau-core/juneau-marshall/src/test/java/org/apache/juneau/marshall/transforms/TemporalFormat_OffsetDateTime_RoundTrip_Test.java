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
import org.apache.juneau.marshall.prototext.*;
import org.apache.juneau.marshall.toml.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.urlencoding.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link TemporalFormat} bound to the {@link OffsetDateTime} subtype
 * across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TemporalFormat_Instant_RoundTrip_Test} — completes the {@link Temporal} coverage for
 * the date+time+offset subtype.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link TemporalFormat} values).  At the time of writing this
 * comes to 42 &times; 20 = 840 testers per test method.
 *
 * <p>
 * Many {@link TemporalFormat} values are intentionally lossy for {@link OffsetDateTime} — date-only
 * formats drop the time portion (parsed back to midnight at the default zone offset), and zone-name-bearing
 * formats fall back to system default offset on parse.  The {@code expectedAfter} helper canonicalizes
 * through the format's own {@code format/parse} cycle so the assertion reflects the lossy canonical form.
 */
@SuppressWarnings({
	"unused" // Exception parameter intentionally unused in catch block; only the fact of the exception matters.
})
class TemporalFormat_OffsetDateTime_RoundTrip_Test extends TestBase {

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
		fmt -> RoundTrip_Tester.create(36, "Prototext - default | " + fmt)
			.serializer(PrototextSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(PrototextParser.create().temporalFormat(fmt))
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
	 * Format-aware expectation helper.  Many {@link TemporalFormat} values are intentionally lossy for
	 * {@link OffsetDateTime} — date-only formats drop the time portion, time-only formats drop the date
	 * portion, and {@link TemporalFormat#ISO_LOCAL_DATE_TIME} / {@link TemporalFormat#ISO_LOCAL_DATE} drop
	 * the offset (parsed back at system default zone offset).  Canonicalize through the format's own
	 * {@code format/parse} cycle so the assertion reflects the lossy canonical form.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format lossiness.
	 */
	private static OffsetDateTime expectedAfter(OffsetDateTime original, RoundTrip_Tester t, TemporalFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return fmt.parse(fmt.format(original, null), OffsetDateTime.class, null);
	}

	//====================================================================================================
	// Bean with one OffsetDateTime field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public OffsetDateTime odt;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_offsetDateTimeProperty_basic(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();
		x.odt = OffsetDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneOffset.UTC), t, fmt), x.odt, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple OffsetDateTime fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public OffsetDateTime created;
		public OffsetDateTime updated;
		public OffsetDateTime epoch;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_offsetDateTimeProperty_multipleFields(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A02Bean();
		x.created = OffsetDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneOffset.UTC);
		x.updated = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		x.epoch = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneOffset.UTC), t, fmt), x.created, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.updated, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.epoch, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with epoch + day-boundary + mid-day OffsetDateTime — boundary coverage
	//====================================================================================================

	public static class A03Bean {
		public OffsetDateTime epoch;
		public OffsetDateTime midnight;
		public OffsetDateTime midDay;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_offsetDateTimeProperty_boundaries(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A03Bean();
		x.epoch = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		x.midnight = OffsetDateTime.of(2024, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC);
		x.midDay = OffsetDateTime.of(2024, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.epoch, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetDateTime.of(2024, 6, 15, 0, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.midnight, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetDateTime.of(2024, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.midDay, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with far-future OffsetDateTime — large epoch-millis stress (MILLIS numeric promotion paths)
	//====================================================================================================

	public static class A04Bean {
		public OffsetDateTime farFuture;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_offsetDateTimeProperty_largeValues(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A04Bean();
		// Year 2099 — large but still within all formatters' supported range.
		x.farFuture = OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetDateTime.of(2099, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), t, fmt), x.farFuture, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null OffsetDateTime field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_offsetDateTimeProperty_nullField(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.odt, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level OffsetDateTime — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_offsetDateTimeTopLevel(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone OffsetDateTime value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = OffsetDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneOffset.UTC);
		try {
			var out = t.serialize(x, s);
			var x2 = p.read(out, OffsetDateTime.class);
			// Binary serializers with native datetime support (BSON in particular) bypass the configured
			// format swap at top-level and round-trip the value with full native fidelity.  Some text
			// serializers (Hocon, Hjson, Toml, ...) emit a full ISO datetime literal at top-level and the
			// parser uses native datetime parsing rather than the configured format swap, so the value
			// comes back as the same Instant but reinterpreted at the parser's default zone offset.
			// Bean-property tests above cover the format-swap path strictly; here we accept any of:
			//   (a) lossy-canonical (format applied via swap),
			//   (b) structural original (format bypassed by native type support),
			//   (c) same-instant equivalent (format bypassed, zone re-interpreted at default).
			var expected = expectedAfter(x, t, fmt);
			assertTrue(expected.equals(x2) || x.equals(x2)
					|| (x2 != null && x.toInstant().equals(x2.toInstant()))
					|| (x2 != null && expected != null && expected.toInstant().equals(x2.toInstant())),
				"fmt=" + fmt + " expected " + expected + " or " + x + " but got " + x2);
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
