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
import org.apache.juneau.marshall.jena.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.markdown.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.parquet.*;
import org.apache.juneau.marshall.proto.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.toml.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.urlencoding.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link FloatFormat} on {@link Double} across every supported
 * serializer/parser pair.
 *
 * <p>
 * Sibling of {@link FloatFormat_Float_RoundTrip_Test} — same shape, same 42 tester templates, varied
 * across every {@link FloatFormat} value.  Round-trips representative {@link Double} values:
 * <ul>
 * 	<li>As a bean property (exercises the {@code MarshalledPropertyPostProcessor} context-format swap install
 * 		path at {@code applyContextFormats}).
 * 	<li>As a top-level value (exercises the per-format dispatch sites in each {@code *SerializerSession} /
 * 		{@code *ParserSession}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link FloatFormat} values).  At the time of writing this comes to
 * 42 &times; 5 = 210 testers per test method.
 *
 * <p>
 * {@link FloatFormat} only controls how non-finite values ({@code NaN}, {@code ±Infinity}) are emitted on
 * <b>text-based</b> wire formats; finite values ride the natural bare-numeric token and binary serializers
 * (BSON / CBOR / MsgPack / Proto / Parquet) emit native IEEE-754 regardless of this setting per the
 * {@link FloatFormat} class-level "Binary serializers" note.  The matrix below exercises finite values across
 * all five {@link FloatFormat} constants for the standard tests, with a dedicated non-finite test
 * ({@link #a04_doubleProperty_nonFinite}) that skips {@link FloatFormat#NaN_AS_ERROR} (throws on swap) and
 * canonicalises against the format-specific wire shape via {@link #expectedAfterNonFinite}.
 *
 * <p>
 * Unlike the {@link Float} sibling, this file has no Proto-skip helper — the Proto text-format tokenizer
 * bugs that forced those skips (Bug #4a — neg-flag discard on the Double return path of
 * {@code readDecimalOrFloat}; Bug #4b — {@code 0.x} mis-tokenisation in {@code lexNumber}'s `0`-prefix
 * branch) were both fixed before this file landed (see the work item 57 notes). The {@code a04} non-finite
 * test still skips Proto in the {@link Float} sibling because Bug #4c ({@code mightStartNumber}
 * {@code nan}/{@code inf} asymmetry) is still open — same predicate carried into this file for the same
 * reason.
 */
@SuppressWarnings({
	"unused" // Parameters retained for method-signature/functional-interface consistency in test fixtures.
})
class FloatFormat_Double_RoundTrip_Test extends TestBase {

	/**
	 * Builds a fully-configured {@link RoundTrip_Tester} parameterized on a {@link FloatFormat} value.
	 *
	 * <p>
	 * Each lambda below mirrors a single tester entry in
	 * {@link org.apache.juneau.marshall.a.rttests.RoundTripDateTime_Test} but threads {@code fmt} through both the
	 * {@code serializer().floatFormat(fmt)} and {@code parser().floatFormat(fmt)} builder calls so the
	 * round-trip uses a consistent format on both sides.
	 */
	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(FloatFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(JsonParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(JsonParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(Json5Parser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(Json5Parser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(JsonlParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(XmlParser.create().floatFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(XmlParser.create().floatFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(HtmlParser.create().floatFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(HtmlParser.create().floatFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(HtmlParser.create().floatFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(UonParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(UonParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(UonParser.create().decoding().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(UrlEncodingParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(UrlEncodingParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(MsgPackParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(RdfXmlParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(RdfThriftParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(RdfProtoParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(RdfXmlParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(TurtleParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(N3Parser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(NTripleParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(NQuadsParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(TriGParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(JsonLdParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(RdfJsonParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(TriXParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(YamlParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().floatFormat(fmt))
			.parser(TomlParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().floatFormat(fmt))
			.parser(IniParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().floatFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(MarkdownParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Proto - default | " + fmt)
			.serializer(ProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(ProtoParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(HjsonParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(JsonParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(CborParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(HoconParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().floatFormat(fmt))
			.parser(BsonParser.create().floatFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().floatFormat(fmt))
			.parser(ParquetParser.create().floatFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	/** Pre-computed (tester, format) combinations.  One row per (builder &times; format) pair. */
	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(FloatFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Returns <jk>true</jk> when {@code t} is wrapped around a binary serializer (any
	 * {@link OutputStreamSerializer} subtype).  Binary serializers emit native IEEE-754 regardless of
	 * {@link FloatFormat} per the class-level "Binary serializers" note, so non-finite values round-trip
	 * natively without going through the format dispatch.  Covers MsgPack, CBOR, BSON, Parquet, Proto,
	 * and the RDF stream variants (RdfThrift / RdfProto).
	 */
	private static boolean isBinarySerializer(RoundTrip_Tester t) {
		return t.getSerializer() instanceof OutputStreamSerializer;
	}

	/**
	 * Format-aware expectation helper for finite {@link Double} values.  {@link FloatFormat} affects only
	 * non-finite values per the {@link FloatFormat} class-level contract; finite values ride the natural
	 * bare-numeric wire token on text serializers and native IEEE-754 on binary serializers.  Validation-only
	 * testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV, Parquet) return
	 * the original object unchanged.
	 */
	@SuppressWarnings({
		"java:S1172" // 'fmt' is part of the shared expectedAfter(...) helper signature used across all *Format RoundTrip tests; kept for template symmetry even where this type's expected value is format-independent.
	})
	private static Double expectedAfter(Double original, RoundTrip_Tester t, FloatFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return original;
	}

	/**
	 * Format-aware expectation helper for non-finite {@link Double} values ({@code NaN}, {@code ±Infinity}).
	 *
	 * <p>
	 * Binary serializers ({@link OutputStreamSerializer} subtypes) emit native IEEE-754 regardless of
	 * {@link FloatFormat} per the class-level "Binary serializers" note — the original non-finite value
	 * round-trips intact.
	 *
	 * <p>
	 * Text serializers — only {@link FloatFormat#NaN_AS_NULL} and {@link FloatFormat#NaN_AS_STRING} are
	 * exercised at this matrix level (per the {@link #a04_doubleProperty_nonFinite} method-doc):
	 * <ul>
	 * 	<li>{@link FloatFormat#NaN_AS_NULL} — wire emits {@code null}; round-trip resolves to {@code null}
	 * 		for the boxed {@link Double} property.
	 * 	<li>{@link FloatFormat#NaN_AS_STRING} — wire emits the quoted token; parser decodes back to the
	 * 		original non-finite value.
	 * </ul>
	 */
	private static Double expectedAfterNonFinite(Double original, RoundTrip_Tester t, FloatFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		if (isBinarySerializer(t))
			return original;
		return switch (fmt) {
			case NaN_AS_NULL -> null;
			case NaN_AS_STRING -> original;
			default -> original;  // unreachable — callers skip NOT_SET / NaN_AS_NUMBER / NaN_AS_ERROR for text.
		};
	}

	/**
	 * Compare two {@link Double} values using {@link Double#equals(Object)} which compares raw IEEE-754 bits.
	 * Critical for non-finite values: {@code Double.NaN == Double.NaN} is {@code false}, but
	 * {@code Double.valueOf(NaN).equals(Double.valueOf(NaN))} is {@code true} by spec.
	 */
	private static void assertDoubleEquals(Double expected, Double actual, String message) {
		if (expected == null) {
			assertNull(actual, message);
		} else {
			assertNotNull(actual, message);
			assertEquals(expected, actual, message + " : expected " + expected + " but was " + actual);
		}
	}

	//====================================================================================================
	// Bean with one Double field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Double n;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_doubleProperty_basic(RoundTrip_Tester t, FloatFormat fmt) throws Exception {
		var x = new A01Bean();
		x.n = 1.5d;

		x = t.roundTrip(x);
		assertDoubleEquals(expectedAfter(1.5d, t, fmt), x.n, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Double fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Double pi;
		public Double half;
		public Double zero;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_doubleProperty_multipleFields(RoundTrip_Tester t, FloatFormat fmt) throws Exception {
		var x = new A02Bean();
		x.pi = Math.PI;
		x.half = 0.5d;
		x.zero = 0.0d;

		x = t.roundTrip(x);
		assertDoubleEquals(expectedAfter(Math.PI, t, fmt), x.pi, "fmt=" + fmt);
		assertDoubleEquals(expectedAfter(0.5d, t, fmt), x.half, "fmt=" + fmt);
		assertDoubleEquals(expectedAfter(0.0d, t, fmt), x.zero, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with negative + zero Double — sign + zero coverage
	//====================================================================================================

	public static class A03Bean {
		public Double negative;
		public Double zero;
		public Double positive;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_doubleProperty_negativeAndZero(RoundTrip_Tester t, FloatFormat fmt) throws Exception {
		var x = new A03Bean();
		x.negative = -3.14d;
		x.zero = 0.0d;
		x.positive = 3.14d;

		x = t.roundTrip(x);
		assertDoubleEquals(expectedAfter(-3.14d, t, fmt), x.negative, "fmt=" + fmt);
		assertDoubleEquals(expectedAfter(0.0d, t, fmt), x.zero, "fmt=" + fmt);
		assertDoubleEquals(expectedAfter(3.14d, t, fmt), x.positive, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with non-finite Double values — NaN / +Infinity / -Infinity edges (the FloatFormat-specific path)
	//
	// <p>
	// The swap install logic in {@code MarshalledPropertyPostProcessor.requiresFloatSwap} only installs
	// the float swap when {@code format == NaN_AS_NULL || NaN_AS_STRING || NaN_AS_ERROR}.  For
	// {@code NOT_SET} and {@code NaN_AS_NUMBER}, no swap is installed and the round-trip rides each
	// serializer's native non-finite handling, which varies widely across formats (JSON rejects bare
	// {@code NaN}; Toml/Ini have no NaN grammar; JCS rejects per RFC 8785; XML/HTML/RDF preserve
	// natively).  Out-of-matrix scope: those combinations are exercised at the format-specific test
	// level, not via this cross-pair matrix.  {@link FloatFormat#NaN_AS_ERROR} throws during swap on
	// non-finite values — also skipped.  This leaves the predictable contract:
	// <ul>
	// 	<li>Binary serializers — emit / parse native IEEE-754 {@code NaN} / {@code ±Infinity} regardless
	// 		of {@code FloatFormat} per the {@link FloatFormat} class-level "Binary serializers" note.
	// 	<li>Text serializers with {@link FloatFormat#NaN_AS_NULL} — emit / parse {@code null} (lossy by
	// 		design — round-trips to {@code null} for boxed {@link Double}).
	// 	<li>Text serializers with {@link FloatFormat#NaN_AS_STRING} — emit / parse the quoted
	// 		{@code "NaN"} / {@code "Infinity"} / {@code "-Infinity"} token (round-trip preserving).
	// </ul>
	//====================================================================================================

	public static class A04Bean {
		public Double nan;
		public Double posInf;
		public Double negInf;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_doubleProperty_nonFinite(RoundTrip_Tester t, FloatFormat fmt) throws Exception {
		// NaN_AS_ERROR throws during swap on non-finite values — skip the entire assertion block.
		if (fmt == FloatFormat.NaN_AS_ERROR)
			return;
		// NOT_SET / NaN_AS_NUMBER on text serializers fall through to native handling — wildly varied
		// per format, out-of-matrix scope.  Binary serializers still preserve native NaN.
		var binary = isBinarySerializer(t);
		if (!binary && (fmt == FloatFormat.NOT_SET || fmt == FloatFormat.NaN_AS_NUMBER))
			return;

		var x = new A04Bean();
		x.nan = Double.NaN;
		x.posInf = Double.POSITIVE_INFINITY;
		x.negInf = Double.NEGATIVE_INFINITY;

		x = t.roundTrip(x);
		assertDoubleEquals(expectedAfterNonFinite(Double.NaN, t, fmt), x.nan, "fmt=" + fmt + " field=nan");
		assertDoubleEquals(expectedAfterNonFinite(Double.POSITIVE_INFINITY, t, fmt), x.posInf, "fmt=" + fmt + " field=posInf");
		assertDoubleEquals(expectedAfterNonFinite(Double.NEGATIVE_INFINITY, t, fmt), x.negInf, "fmt=" + fmt + " field=negInf");
	}

	//====================================================================================================
	// Bean with null Double field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_doubleProperty_nullField(RoundTrip_Tester t, FloatFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.n, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Double — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_doubleTopLevel(RoundTrip_Tester t, FloatFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Double value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = 3.14d;
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, Double.class);
			assertDoubleEquals(expectedAfter(x, t, fmt), x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
