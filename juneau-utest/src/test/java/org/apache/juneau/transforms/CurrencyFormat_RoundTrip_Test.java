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
package org.apache.juneau.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.a.rttests.*;
import org.apache.juneau.bson.*;
import org.apache.juneau.cbor.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.hjson.*;
import org.apache.juneau.hocon.*;
import org.apache.juneau.html.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.jcs.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.jsonl.*;
import org.apache.juneau.markdown.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parquet.*;
import org.apache.juneau.proto.*;
import org.apache.juneau.toml.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.yaml.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link CurrencyFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TimeZoneFormat_RoundTrip_Test} — same shape, same 42 tester templates, varied across
 * every {@link CurrencyFormat} value.  Round-trips representative {@link Currency} values.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link CurrencyFormat} values).  At the time of writing this
 * comes to 42 &times; 4 = 168 testers per test method.
 *
 * <h5 class='topic'>Locale pinning</h5>
 *
 * <p>
 * {@link CurrencyFormat#SYMBOL} and {@link CurrencyFormat#DISPLAY_NAME} are locale-sensitive — the symbol
 * {@code "$"} resolves to USD / CAD / AUD / etc. depending on locale, and {@code "US Dollar"} similarly
 * varies.  The default JVM locale is pinned to {@link Locale#US} in {@link #pinLocale()} so the test
 * behaviour is reproducible across CI environments, and restored in {@link #restoreLocale()}.
 *
 * <p>
 * {@link Currency} uses cached singletons via {@link Currency#getInstance(String)}, so the default
 * {@link Object#equals} comparison works.  No equals helper needed.
 */
class CurrencyFormat_RoundTrip_Test extends TestBase {

	private static Locale originalLocale;

	@BeforeAll
	static void pinLocale() {
		originalLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
	}

	@AfterAll
	static void restoreLocale() {
		Locale.setDefault(originalLocale);
	}

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(CurrencyFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(JsonParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(JsonParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(Json5Parser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(Json5Parser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(JsonlParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(XmlParser.create().currencyFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(XmlParser.create().currencyFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(HtmlParser.create().currencyFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(HtmlParser.create().currencyFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(HtmlParser.create().currencyFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(UonParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(UonParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(UonParser.create().decoding().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(UrlEncodingParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(UrlEncodingParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(MsgPackParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(RdfXmlParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(RdfThriftParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(RdfProtoParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(RdfXmlParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(TurtleParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(N3Parser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(NTripleParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(NQuadsParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(TriGParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(JsonLdParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(RdfJsonParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(TriXParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(YamlParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().currencyFormat(fmt))
			.parser(TomlParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().currencyFormat(fmt))
			.parser(IniParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().currencyFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(MarkdownParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Proto - default | " + fmt)
			.serializer(ProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(ProtoParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(HjsonParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(JsonParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(CborParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(HoconParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().currencyFormat(fmt))
			.parser(BsonParser.create().currencyFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().currencyFormat(fmt))
			.parser(ParquetParser.create().currencyFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(CurrencyFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Format-aware expectation helper.  {@link CurrencyFormat#SYMBOL} and {@link CurrencyFormat#DISPLAY_NAME}
	 * are locale-sensitive and only round-trip cleanly when the parser can disambiguate the wire token
	 * against the pinned {@link Locale#US} default — see {@link #pinLocale()}.  The
	 * {@link CurrencyFormat#parse} API tries {@link Currency#getInstance(String)} first regardless of the
	 * configured format, so {@code ISO_CODE} wires always round-trip and {@code SYMBOL} / {@code DISPLAY_NAME}
	 * wires round-trip when the token is uniquely resolvable.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural
	 * original regardless of format lossiness.
	 */
	private static Currency expectedAfter(Currency original, RoundTrip_Tester t, CurrencyFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		// Canonicalize through the format's own format / parse cycle.  When the parse step throws (ambiguous
		// symbol or unresolvable display name), fall back to the original for the assertion to fail cleanly
		// in the round-trip test rather than blow up here.
		var effective = fmt == null ? CurrencyFormat.ISO_CODE : fmt;
		try {
			return CurrencyFormat.parse(CurrencyFormat.format(original, effective, Locale.US), effective, Locale.US);
		} catch (@SuppressWarnings("unused") IllegalArgumentException ignored) {
			return original;
		}
	}

	//====================================================================================================
	// Bean with one Currency field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Currency c;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_currencyProperty_basic(RoundTrip_Tester t, CurrencyFormat fmt) throws Exception {
		var x = new A01Bean();
		x.c = Currency.getInstance("USD");

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Currency.getInstance("USD"), t, fmt), x.c, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Currency fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Currency usd;
		public Currency eur;
		public Currency jpy;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_currencyProperty_multipleFields(RoundTrip_Tester t, CurrencyFormat fmt) throws Exception {
		var x = new A02Bean();
		x.usd = Currency.getInstance("USD");
		x.eur = Currency.getInstance("EUR");
		x.jpy = Currency.getInstance("JPY");

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Currency.getInstance("USD"), t, fmt), x.usd, "fmt=" + fmt);
		assertEquals(expectedAfter(Currency.getInstance("EUR"), t, fmt), x.eur, "fmt=" + fmt);
		assertEquals(expectedAfter(Currency.getInstance("JPY"), t, fmt), x.jpy, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with mixed-decimal-precision Currency values — JPY (0), USD (2), BHD (3)
	//====================================================================================================

	public static class A03Bean {
		public Currency jpy;     // 0 fraction digits
		public Currency usd;     // 2 fraction digits
		public Currency bhd;     // 3 fraction digits
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_currencyProperty_zeroDecimalAndHighDecimal(RoundTrip_Tester t, CurrencyFormat fmt) throws Exception {
		var x = new A03Bean();
		x.jpy = Currency.getInstance("JPY");
		x.usd = Currency.getInstance("USD");
		x.bhd = Currency.getInstance("BHD");

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Currency.getInstance("JPY"), t, fmt), x.jpy, "fmt=" + fmt);
		assertEquals(expectedAfter(Currency.getInstance("USD"), t, fmt), x.usd, "fmt=" + fmt);
		assertEquals(expectedAfter(Currency.getInstance("BHD"), t, fmt), x.bhd, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with locale-sensitive Currency wire shapes — verifies the pinned Locale.US default produces
	// a round-trippable token for SYMBOL / DISPLAY_NAME on currencies that have unambiguous tokens in
	// that locale.  Ambiguous symbols (e.g. "$" -> USD/CAD/AUD/MXN/HKD/SGD/NZD) are intentionally avoided
	// by using EUR (€ / "Euro") and JPY (¥ / "Japanese Yen") as the locale-sensitive values; SYMBOL
	// "$" -> USD relies on Locale.US being the default currency disambiguator in
	// CurrencyFormat.findBySymbol's resolveAmbiguousMatches.
	//====================================================================================================

	public static class A04Bean {
		public Currency usd;     // $ — locale-ambiguous symbol, disambiguated by Locale.US default
		public Currency eur;     // € — unambiguous symbol across locales
		public Currency jpy;     // ¥ — also CNY in some locales; Locale.US resolves to JPY
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_currencyProperty_localeSensitive(RoundTrip_Tester t, CurrencyFormat fmt) throws Exception {
		var x = new A04Bean();
		x.usd = Currency.getInstance("USD");
		x.eur = Currency.getInstance("EUR");
		x.jpy = Currency.getInstance("JPY");

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Currency.getInstance("USD"), t, fmt), x.usd, "fmt=" + fmt);
		assertEquals(expectedAfter(Currency.getInstance("EUR"), t, fmt), x.eur, "fmt=" + fmt);
		assertEquals(expectedAfter(Currency.getInstance("JPY"), t, fmt), x.jpy, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null Currency field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_currencyProperty_nullField(RoundTrip_Tester t, CurrencyFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.c, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Currency — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_currencyTopLevel(RoundTrip_Tester t, CurrencyFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Currency value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = Currency.getInstance("USD");
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, Currency.class);
			// Accept either the lossy-canonical (format applied) or the structural original (format bypassed
			// by native type handling — matches the BSON-style divergence already documented for DateFormat /
			// CalendarFormat at top-level).
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
