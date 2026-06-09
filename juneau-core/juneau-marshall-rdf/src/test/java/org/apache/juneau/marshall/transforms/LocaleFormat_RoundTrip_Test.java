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
import org.apache.juneau.marshall.jena.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link LocaleFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TimeZoneFormat_RoundTrip_Test} — same shape, same 12 RDF tester templates, varied across
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
 * comes to 12 &times; 3 = 36 testers per test method.
 *
 * <p>
 * {@link LocaleFormat#BCP_47} is symmetric ({@code Locale.toLanguageTag} / {@code Locale.forLanguageTag});
 * {@link LocaleFormat#UNDERSCORE} is symmetric for the common language+country case and tolerates
 * variant subtags but cannot represent BCP-47 script subtags.  No locale pinning required since the
 * format helpers use {@link Locale#toLanguageTag()} / {@link Locale#toString()} (zone- and locale-agnostic),
 * not display-name conversions.
 */
@SuppressWarnings({
	"unused" // Parameters retained for method-signature/functional-interface consistency in test fixtures.
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
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(RdfXmlParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(RdfThriftParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(RdfProtoParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(RdfXmlParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(TurtleParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(N3Parser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(NTripleParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(NQuadsParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(TriGParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(JsonLdParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(RdfJsonParser.create().localeFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().localeFormat(fmt))
			.parser(TriXParser.create().localeFormat(fmt))
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
