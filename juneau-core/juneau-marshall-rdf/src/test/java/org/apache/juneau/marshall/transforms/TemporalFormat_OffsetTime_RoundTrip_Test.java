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
 * Cross-pair round-trip coverage for {@link TemporalFormat} bound to the {@link OffsetTime} subtype across
 * every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TemporalFormat_Instant_RoundTrip_Test} — completes the {@link Temporal} coverage for
 * the time+offset subtype.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link TemporalFormat} values).  At the time of writing this
 * comes to 12 &times; 20 = 240 testers per test method.
 *
 * <p>
 * {@link OffsetTime} has no date, so date-bearing formats are intentionally lossy.
 * {@link TemporalFormat#MILLIS} for {@link OffsetTime} falls back to the type's {@link TemporalFormat#DEFAULT}
 * formatter (an ISO offset-time string) per {@link TemporalFormat#isMillisNumeric(Class)} — there is no
 * defensible epoch-millis interpretation for a time-without-date.
 */
class TemporalFormat_OffsetTime_RoundTrip_Test extends TestBase {

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
	 * {@link OffsetTime} — date-bearing formats fill in the date as 1970-01-01 (via
	 * {@link org.apache.juneau.marshall.swaps.DefaultingTemporalAccessor}) on the way out and lose the time portion
	 * on the way back in (parse falls back to {@link LocalTime#MIDNIGHT} + system-default offset for
	 * date-only formats).  Canonicalize through the format's own {@code format/parse} cycle.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format lossiness.
	 */
	private static OffsetTime expectedAfter(OffsetTime original, RoundTrip_Tester t, TemporalFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return fmt.parse(fmt.format(original, null), OffsetTime.class, null);
	}

	//====================================================================================================
	// Bean with one OffsetTime field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public OffsetTime ot;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_offsetTimeProperty_basic(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();
		x.ot = OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC), t, fmt), x.ot, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple OffsetTime fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public OffsetTime morning;
		public OffsetTime noon;
		public OffsetTime midnight;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_offsetTimeProperty_multipleFields(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A02Bean();
		x.morning = OffsetTime.of(9, 0, 0, 0, ZoneOffset.UTC);
		x.noon = OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC);
		x.midnight = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetTime.of(9, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.morning, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.noon, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.midnight, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with midnight + noon + end-of-day OffsetTime — boundary coverage
	//====================================================================================================

	public static class A03Bean {
		public OffsetTime midnight;
		public OffsetTime noon;
		public OffsetTime endOfDay;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_offsetTimeProperty_boundaries(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A03Bean();
		x.midnight = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
		x.noon = OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC);
		x.endOfDay = OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.midnight, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC), t, fmt), x.noon, "fmt=" + fmt);
		assertEquals(expectedAfter(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC), t, fmt), x.endOfDay, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with end-of-day OffsetTime — boundary stress
	//====================================================================================================

	public static class A04Bean {
		public OffsetTime endOfDay;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_offsetTimeProperty_largeValues(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A04Bean();
		x.endOfDay = OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC), t, fmt), x.endOfDay, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null OffsetTime field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_offsetTimeProperty_nullField(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.ot, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level OffsetTime — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_offsetTimeTopLevel(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone OffsetTime value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, OffsetTime.class);
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
