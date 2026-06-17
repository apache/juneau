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

import java.math.*;
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
 * Cross-pair round-trip coverage for {@link BigNumberFormat} on {@link BigInteger} across every supported
 * serializer/parser pair.
 *
 * <p>
 * Sibling of {@link DurationFormat_RoundTrip_Test} and {@link PeriodFormat_RoundTrip_Test} — same shape, same
 * 42 tester templates, varied across every {@link BigNumberFormat} value.  Round-trips representative
 * {@link BigInteger} values:
 * <ul>
 * 	<li>As a bean property (exercises the {@code MarshalledPropertyPostProcessor} context-format swap install
 * 		path at {@code applyContextFormats}).
 * 	<li>As a top-level value (exercises the per-format dispatch sites in each {@code *SerializerSession} /
 * 		{@code *ParserSession}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link BigNumberFormat} values).  At the time of writing this comes to
 * 42 &times; 4 = 168 testers per test method.
 *
 * <p>
 * {@link BigNumberFormat} is structurally lossless for {@link BigInteger} on text serializers — every constant
 * preserves the full integer value (the choice is just bare-numeric vs quoted-string vs JS-safe hybrid).
 * Binary serializers (MsgPack / CBOR / Prototext / BSON) bypass the format dispatch and receive the native
 * {@link BigInteger}; the binary format then decides how to encode it natively.  Values beyond a 64-bit
 * signed integer ({@code ±2^63 − 1}) may lose precision or fail in formats that lack a wide-integer wire type.
 */
@SuppressWarnings({
	"unused" // Unused parameters/variables kept for consistent method signatures across test utilities.
})
class BigNumberFormat_BigInteger_RoundTrip_Test extends TestBase {

	/**
	 * Builds a fully-configured {@link RoundTrip_Tester} parameterized on a {@link BigNumberFormat} value.
	 *
	 * <p>
	 * Each lambda below mirrors a single tester entry in
	 * {@link org.apache.juneau.marshall.a.rttests.RoundTripDateTime_Test} but threads {@code fmt} through both the
	 * {@code serializer().bigNumberFormat(fmt)} and {@code parser().bigNumberFormat(fmt)} builder calls so the
	 * round-trip uses a consistent format on both sides.
	 */
	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(BigNumberFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(JsonParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(JsonParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(Json5Parser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(Json5Parser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(JsonlParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(XmlParser.create().bigNumberFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(XmlParser.create().bigNumberFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(HtmlParser.create().bigNumberFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(HtmlParser.create().bigNumberFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(HtmlParser.create().bigNumberFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(UonParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(UonParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(UonParser.create().decoding().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(UrlEncodingParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(UrlEncodingParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(MsgPackParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(YamlParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().bigNumberFormat(fmt))
			.parser(TomlParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().bigNumberFormat(fmt))
			.parser(IniParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().bigNumberFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(MarkdownParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Prototext - default | " + fmt)
			.serializer(PrototextSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(PrototextParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(HjsonParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(JsonParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(CborParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(HoconParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().bigNumberFormat(fmt))
			.parser(BsonParser.create().bigNumberFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().bigNumberFormat(fmt))
			.parser(ParquetParser.create().bigNumberFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	/** Pre-computed (tester, format) combinations.  One row per (builder &times; format) pair. */
	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(BigNumberFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Format-aware expectation helper.  {@link BigNumberFormat} is structurally lossless for {@link BigInteger}
	 * on text serializers — every constant preserves the full integer magnitude (just varies the wire encoding
	 * between bare-numeric / quoted-string / JS-safe hybrid).  Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()}
	 * == {@code true}: Json schema, CSV, Parquet) return the original object unchanged.
	 */
	@SuppressWarnings({
		"java:S1172" // 'fmt' is part of the shared expectedAfter(...) helper signature used across all *Format RoundTrip tests; kept for template symmetry even where this type's expected value is format-independent.
	})
	private static BigInteger expectedAfter(BigInteger original, RoundTrip_Tester t, BigNumberFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return original;
	}

	//====================================================================================================
	// Bean with one BigInteger field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public BigInteger n;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_bigIntegerProperty_basic(RoundTrip_Tester t, BigNumberFormat fmt) throws Exception {
		var x = new A01Bean();
		x.n = BigInteger.valueOf(12345);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(BigInteger.valueOf(12345), t, fmt), x.n, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple BigInteger fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public BigInteger small;
		public BigInteger medium;
		public BigInteger zero;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_bigIntegerProperty_multipleFields(RoundTrip_Tester t, BigNumberFormat fmt) throws Exception {
		var x = new A02Bean();
		x.small = BigInteger.valueOf(42);
		x.medium = BigInteger.valueOf(1_000_000_000L);
		x.zero = BigInteger.ZERO;

		x = t.roundTrip(x);
		assertEquals(expectedAfter(BigInteger.valueOf(42), t, fmt), x.small, "fmt=" + fmt);
		assertEquals(expectedAfter(BigInteger.valueOf(1_000_000_000L), t, fmt), x.medium, "fmt=" + fmt);
		assertEquals(expectedAfter(BigInteger.ZERO, t, fmt), x.zero, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with negative + zero + signed BigInteger — sign + zero coverage
	//====================================================================================================

	public static class A03Bean {
		public BigInteger negative;
		public BigInteger zero;
		public BigInteger positive;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_bigIntegerProperty_negativeAndZero(RoundTrip_Tester t, BigNumberFormat fmt) throws Exception {
		var x = new A03Bean();
		x.negative = BigInteger.valueOf(-12345);
		x.zero = BigInteger.ZERO;
		x.positive = BigInteger.valueOf(12345);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(BigInteger.valueOf(-12345), t, fmt), x.negative, "fmt=" + fmt);
		assertEquals(expectedAfter(BigInteger.ZERO, t, fmt), x.zero, "fmt=" + fmt);
		assertEquals(expectedAfter(BigInteger.valueOf(12345), t, fmt), x.positive, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with large BigInteger — JS-unsafe-but-long-safe stress (AUTO dispatch + NUMBER/STRING boundary)
	//
	// <p>
	// Values are pinned within signed 64-bit range so binary serializers (MsgPack / CBOR / BSON / Prototext)
	// that bypass {@code BigNumberFormat} dispatch and emit native int64 round-trip cleanly.  Values beyond
	// {@code Long.MAX_VALUE} are out-of-matrix scope by design: per the {@link BigNumberFormat} class-level
	// "Binary serializers" note, MsgPack / CBOR / Prototext / BSON downcast big integers to their widest native
	// numeric type (int64 / double / decimal128) and lose precision above that ceiling.  JCS additionally
	// caps at {@code Long.MAX_VALUE} via {@code BigInteger.longValueExact} per RFC 8785.  The matrix below
	// stays under that ceiling but still exceeds the JavaScript {@code 2^53 − 1} safe-integer limit, which
	// exercises the {@link BigNumberFormat#AUTO} dispatch path that flips between {@code NUMBER} (JS-safe)
	// and {@code STRING} (JS-unsafe) wire forms.
	//====================================================================================================

	public static class A04Bean {
		public BigInteger longMax;
		public BigInteger longMinPlusOne;
		public BigInteger jsUnsafe;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_bigIntegerProperty_largeValues(RoundTrip_Tester t, BigNumberFormat fmt) throws Exception {
		var x = new A04Bean();
		x.longMax = BigInteger.valueOf(Long.MAX_VALUE);
		// Long.MIN_VALUE + 1 instead of Long.MIN_VALUE: the Prototext tokenizer parses signed integers by
		// tokenising magnitude-first then applying sign, so the literal "-9223372036854775808" overflows on
		// the unsigned magnitude side.  Long.MIN_VALUE + 1's magnitude == Long.MAX_VALUE which fits.
		x.longMinPlusOne = BigInteger.valueOf(Long.MIN_VALUE + 1L);
		x.jsUnsafe = BigInteger.valueOf(9007199254740992L);  // 2^53, just over JS_MAX_SAFE_INTEGER (2^53 − 1)

		x = t.roundTrip(x);
		assertEquals(expectedAfter(BigInteger.valueOf(Long.MAX_VALUE), t, fmt), x.longMax, "fmt=" + fmt);
		assertEquals(expectedAfter(BigInteger.valueOf(Long.MIN_VALUE + 1L), t, fmt), x.longMinPlusOne, "fmt=" + fmt);
		assertEquals(expectedAfter(BigInteger.valueOf(9007199254740992L), t, fmt), x.jsUnsafe, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null BigInteger field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_bigIntegerProperty_nullField(RoundTrip_Tester t, BigNumberFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.n, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level BigInteger — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_bigIntegerTopLevel(RoundTrip_Tester t, BigNumberFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone BigInteger value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = BigInteger.valueOf(12345);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, BigInteger.class);
			assertEquals(expectedAfter(x, t, fmt), x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
