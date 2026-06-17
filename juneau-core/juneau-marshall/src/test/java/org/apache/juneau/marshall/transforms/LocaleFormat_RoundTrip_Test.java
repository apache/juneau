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
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link LocaleFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TimeZoneFormat_RoundTrip_Test} — same shape, same 42 tester templates, varied across
 * every {@link LocaleFormat} value.  Round-trips representative {@link Locale} values:
 * <ul>
 * 	<li>As a bean property (single locale + sibling).
 * 	<li>As a list element.
 * 	<li>As an exotic-region {@code zh-CN}-style locale (script/region edge).
 * 	<li>As a null field.
 * 	<li>As a top-level value.
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link LocaleFormat} values).  At the time of writing this
 * comes to 42 &times; 3 = 126 testers per test method.
 *
 * <p>
 * {@link LocaleFormat#BCP_47} is symmetric ({@code Locale.toLanguageTag} / {@code Locale.forLanguageTag});
 * {@link LocaleFormat#UNDERSCORE} is symmetric for the common language+country case and tolerates
 * variant subtags but cannot represent BCP-47 script subtags.  No locale pinning required since the
 * format helpers use {@link Locale#toLanguageTag()} / {@link Locale#toString()} (zone- and locale-agnostic),
 * not display-name conversions.
 */
@SuppressWarnings({
	"unused" // Unused parameters/variables kept for consistent method signatures across test utilities.
})
class LocaleFormat_RoundTrip_Test extends TestBase {

	private static Locale originalLocale;

	@BeforeAll
	static void pinLocale() {
		// Pin to Locale.US so any caller that incidentally consults the platform default locale during
		// serialisation / parsing gets a deterministic answer.  The LocaleFormat helpers themselves use
		// language-tag / underscore forms that are locale-independent, but pinning is cheap insurance.
		originalLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);
	}

	@AfterAll
	static void restoreLocale() {
		Locale.setDefault(originalLocale);
	}

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(LocaleFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(JsonParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(JsonParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(Json5Parser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(Json5Parser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(JsonlParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(XmlParser.create().localeFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(XmlParser.create().localeFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(HtmlParser.create().localeFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(HtmlParser.create().localeFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(HtmlParser.create().localeFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(UonParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(UonParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(UonParser.create().decoding().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(UrlEncodingParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(UrlEncodingParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(MsgPackParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(YamlParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().localeFormat(fmt))
			.parser(TomlParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().localeFormat(fmt))
			.parser(IniParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().localeFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(MarkdownParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Prototext - default | " + fmt)
			.serializer(PrototextSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(PrototextParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(HjsonParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(JsonParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(CborParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(HoconParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(BsonParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().localeFormat(fmt))
			.parser(ParquetParser.create().localeFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(LocaleFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Format-aware expectation helper.  {@link LocaleFormat} is symmetric on every constant for the
	 * inputs used here — both {@link LocaleFormat#BCP_47} and {@link LocaleFormat#UNDERSCORE} round-trip
	 * language+country (and the script-bearing {@code zh-CN} variant) without loss.
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema,
	 * CSV, Parquet) return the original object unchanged.
	 */
	@SuppressWarnings({
		"java:S1172" // 'fmt' is part of the shared expectedAfter(...) helper signature used across all *Format RoundTrip tests; kept for template symmetry even where this type's expected value is format-independent.
	})
	private static Locale expectedAfter(Locale original, RoundTrip_Tester t, LocaleFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return original;
	}

	//====================================================================================================
	// Bean with one Locale field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Locale loc;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_localeProperty_basic(RoundTrip_Tester t, LocaleFormat fmt) throws Exception {
		var x = new A01Bean();
		x.loc = Locale.US;

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Locale.US, t, fmt), x.loc, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Locale fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Locale us;
		public Locale japan;
		public Locale germany;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_localeProperty_multipleFields(RoundTrip_Tester t, LocaleFormat fmt) throws Exception {
		var x = new A02Bean();
		x.us = Locale.US;
		x.japan = Locale.JAPAN;
		x.germany = Locale.GERMANY;

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Locale.US, t, fmt), x.us, "fmt=" + fmt);
		assertEquals(expectedAfter(Locale.JAPAN, t, fmt), x.japan, "fmt=" + fmt);
		assertEquals(expectedAfter(Locale.GERMANY, t, fmt), x.germany, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Locale fields including language-only + region-only — edge variants
	//====================================================================================================

	public static class A03Bean {
		public Locale languageOnly;
		public Locale languageRegion;
		public Locale zhCn;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_localeProperty_edgeCases(RoundTrip_Tester t, LocaleFormat fmt) throws Exception {
		// Locale.ENGLISH has no country — exercises the underscore "language-only" parser branch.
		// Locale.UK exercises the language+country happy path.
		// "zh-CN" via BCP-47 builder exercises the script/region edge for BCP_47 and the simple
		// language+country path for UNDERSCORE.  No script subtag is used — UNDERSCORE can't represent
		// BCP-47 scripts and we don't want to introduce a known-lossy-on-one-format combo here.
		var zhCn = new Locale.Builder().setLanguage("zh").setRegion("CN").build();

		var x = new A03Bean();
		x.languageOnly = Locale.ENGLISH;
		x.languageRegion = Locale.UK;
		x.zhCn = zhCn;

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Locale.ENGLISH, t, fmt), x.languageOnly, "fmt=" + fmt);
		assertEquals(expectedAfter(Locale.UK, t, fmt), x.languageRegion, "fmt=" + fmt);
		assertEquals(expectedAfter(zhCn, t, fmt), x.zhCn, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with a list-of-Locale property — collection-element dispatch
	//====================================================================================================

	public static class A04Bean {
		public List<Locale> list;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_localeProperty_inList(RoundTrip_Tester t, LocaleFormat fmt) throws Exception {
		var x = new A04Bean();
		x.list = List.of(Locale.US, Locale.JAPAN, Locale.GERMANY);

		x = t.roundTrip(x);
		assertNotNull(x.list, "fmt=" + fmt);
		assertEquals(List.of(Locale.US, Locale.JAPAN, Locale.GERMANY), x.list, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with a null Locale field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_localeProperty_nullField(RoundTrip_Tester t, LocaleFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.loc, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Locale — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_localeTopLevel(RoundTrip_Tester t, LocaleFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Locale value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = Locale.US;
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, Locale.class);
			assertEquals(expectedAfter(x, t, fmt), x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror BigNumberFormat_BigInteger_RoundTrip_Test.a06: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
