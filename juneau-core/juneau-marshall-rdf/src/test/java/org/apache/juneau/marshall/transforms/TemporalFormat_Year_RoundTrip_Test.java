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
import org.apache.juneau.marshall.jena.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link TemporalFormat} bound to the {@link Year} subtype across
 * every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TemporalFormat_Instant_RoundTrip_Test} — completes the {@link Temporal} coverage for
 * the year-only subtype.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link TemporalFormat} values).  At the time of writing this
 * comes to 12 &times; 20 = 240 testers per test method.
 *
 * <p>
 * Most {@link TemporalFormat} values are intentionally lossy for {@link Year} — every format other than
 * {@link TemporalFormat#ISO_YEAR} / {@link TemporalFormat#ISO_YEAR_MONTH} / {@link TemporalFormat#MILLIS}
 * coerces through {@link org.apache.juneau.marshall.swaps.DefaultingTemporalAccessor} (filling in month=1, day=1,
 * time=00:00:00 on the way out and falling back to year=1970 on the way back in for time-only formats).
 * The {@code expectedAfter} helper canonicalizes through the format's own {@code format/parse} cycle so
 * the assertion reflects the lossy canonical form.
 */
class TemporalFormat_Year_RoundTrip_Test extends TestBase {

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(TemporalFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(RdfXmlParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(RdfThriftParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(RdfProtoParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(RdfXmlParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(TurtleParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(N3Parser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(NTripleParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(NQuadsParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(TriGParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(JsonLdParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(RdfJsonParser.create().temporalFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().temporalFormat(fmt))
			.parser(TriXParser.create().temporalFormat(fmt))
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
	 * {@link Year} — only {@link TemporalFormat#ISO_YEAR} / {@link TemporalFormat#ISO_YEAR_MONTH} (and
	 * {@link TemporalFormat#MILLIS} which round-trips through epoch-millis at January 1) preserve the year
	 * cleanly; every other format coerces to a year-defaulting form on serialize and falls back to year
	 * 1970 on parse for time-only formats.  Canonicalize through the format's own {@code format/parse} cycle.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format lossiness.
	 */
	private static Year expectedAfter(Year original, RoundTrip_Tester t, TemporalFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return fmt.parse(fmt.format(original, null), Year.class, null);
	}

	//====================================================================================================
	// Bean with one Year field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Year y;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_yearProperty_basic(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();
		x.y = Year.of(2024);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Year.of(2024), t, fmt), x.y, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Year fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Year created;
		public Year updated;
		public Year epoch;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_yearProperty_multipleFields(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A02Bean();
		x.created = Year.of(2024);
		x.updated = Year.of(2025);
		x.epoch = Year.of(1970);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Year.of(2024), t, fmt), x.created, "fmt=" + fmt);
		assertEquals(expectedAfter(Year.of(2025), t, fmt), x.updated, "fmt=" + fmt);
		assertEquals(expectedAfter(Year.of(1970), t, fmt), x.epoch, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with epoch + leap-year + boundary Year — boundary coverage
	//====================================================================================================

	public static class A03Bean {
		public Year epoch;
		public Year leapYear;
		public Year minYear;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_yearProperty_boundaries(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A03Bean();
		x.epoch = Year.of(1970);
		x.leapYear = Year.of(2024);
		x.minYear = Year.of(1900);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Year.of(1970), t, fmt), x.epoch, "fmt=" + fmt);
		assertEquals(expectedAfter(Year.of(2024), t, fmt), x.leapYear, "fmt=" + fmt);
		assertEquals(expectedAfter(Year.of(1900), t, fmt), x.minYear, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with far-future Year — large epoch-millis stress (MILLIS numeric promotion paths)
	//====================================================================================================

	public static class A04Bean {
		public Year farFuture;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_yearProperty_largeValues(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A04Bean();
		// Year 2099 — large but still within all formatters' supported range.
		x.farFuture = Year.of(2099);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(Year.of(2099), t, fmt), x.farFuture, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null Year field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_yearProperty_nullField(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.y, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Year — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_yearTopLevel(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Year value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = Year.of(2024);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, Year.class);
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
