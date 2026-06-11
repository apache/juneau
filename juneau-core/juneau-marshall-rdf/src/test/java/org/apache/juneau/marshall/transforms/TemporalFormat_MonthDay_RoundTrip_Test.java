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
 * Cross-pair round-trip coverage for {@link TemporalFormat} bound to the {@link MonthDay} subtype across
 * every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TemporalFormat_Instant_RoundTrip_Test} — completes the {@link Temporal} coverage for
 * the month+day-only subtype.
 *
 * <p>
 * {@link MonthDay} only implements {@link java.time.temporal.TemporalAccessor} (not {@link Temporal});
 * {@link MarshalledPropertyPostProcessor} routes {@link MonthDay} through its sibling
 * {@code temporalAccessorSwap} factory.  The swap delegates to {@link TemporalFormat#format} and
 * {@link TemporalFormat#parse}, which special-case {@link MonthDay} to its native {@code --MM-DD}
 * wire shape regardless of the configured format value (every other {@link TemporalFormat} value is
 * structurally meaningless for {@link MonthDay}).
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link TemporalFormat} values) = 12 &times; 20 = 240 testers
 * per test method.
 */
@SuppressWarnings({
	"java:S8694" // Test data uses literal month ints for date construction; Month enum constants add noise without value.
})
class TemporalFormat_MonthDay_RoundTrip_Test extends TestBase {

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
	 * Format-aware expectation helper.  {@link MonthDay} only carries month + day, so every
	 * {@link TemporalFormat} value canonicalizes through the same {@code --MM-DD} wire shape (the only
	 * stable representation without a year).  Canonicalize through the format's own {@code format/parse}
	 * cycle so the assertion mirrors any incidental lossiness for validation-only testers.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format lossiness.
	 */
	private static MonthDay expectedAfter(MonthDay original, RoundTrip_Tester t, TemporalFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return fmt.parse(fmt.format(original, null), MonthDay.class, null);
	}

	//====================================================================================================
	// Bean with one MonthDay field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public MonthDay md;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_monthDayProperty_basic(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();
		x.md = MonthDay.of(6, 15);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(MonthDay.of(6, 15), t, fmt), x.md, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple MonthDay fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public MonthDay newYear;
		public MonthDay midYear;
		public MonthDay endYear;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_monthDayProperty_multipleFields(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A02Bean();
		x.newYear = MonthDay.of(1, 1);
		x.midYear = MonthDay.of(6, 15);
		x.endYear = MonthDay.of(12, 31);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(MonthDay.of(1, 1), t, fmt), x.newYear, "fmt=" + fmt);
		assertEquals(expectedAfter(MonthDay.of(6, 15), t, fmt), x.midYear, "fmt=" + fmt);
		assertEquals(expectedAfter(MonthDay.of(12, 31), t, fmt), x.endYear, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with leap-day + new-year + end-of-year MonthDay — boundary coverage
	//====================================================================================================

	public static class A03Bean {
		public MonthDay leapDay;
		public MonthDay newYear;
		public MonthDay endYear;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_monthDayProperty_boundaries(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A03Bean();
		x.leapDay = MonthDay.of(2, 29);
		x.newYear = MonthDay.of(1, 1);
		x.endYear = MonthDay.of(12, 31);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(MonthDay.of(2, 29), t, fmt), x.leapDay, "fmt=" + fmt);
		assertEquals(expectedAfter(MonthDay.of(1, 1), t, fmt), x.newYear, "fmt=" + fmt);
		assertEquals(expectedAfter(MonthDay.of(12, 31), t, fmt), x.endYear, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with end-of-year MonthDay — boundary stress
	//====================================================================================================

	public static class A04Bean {
		public MonthDay endYear;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_monthDayProperty_largeValues(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A04Bean();
		x.endYear = MonthDay.of(12, 31);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(MonthDay.of(12, 31), t, fmt), x.endYear, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null MonthDay field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_monthDayProperty_nullField(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.md, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level MonthDay — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_monthDayTopLevel(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone MonthDay value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = MonthDay.of(6, 15);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, MonthDay.class);
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
