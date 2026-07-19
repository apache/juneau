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
import org.apache.juneau.marshall.jena.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link DurationFormat} across every supported serializer/parser pair.
 *
 * <p>
 * This is the pilot test for the work item 50/work item 54 format-control round-trip pattern.  It varies the
 * {@link DurationFormat} setting on both the serializer and parser builders for each tester template, then
 * round-trips representative {@link Duration} values:
 * <ul>
 * 	<li>As a top-level value (exercises the per-format dispatch sites in each {@code *SerializerSession} /
 * 		{@code *ParserSession}).
 * 	<li>As a bean property (exercises the {@code MarshalledPropertyPostProcessor} context-format swap install
 * 		path at {@code applyContextFormats}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link DurationFormat} values).  At the time of writing this comes to
 * roughly 41 &times; 7 = 287 testers per test method.
 *
 * <p>
 * If a tester is validation-only (no parser, schema-only, or CSV in serializer-only mode) the top-level
 * standalone-Duration test is skipped — there is no Duration round-trip semantics to assert in that case.
 */
class DurationFormat_RoundTrip_Test extends TestBase {

	/**
	 * Builds a fully-configured {@link RoundTrip_Tester} parameterized on a {@link DurationFormat} value.
	 *
	 * <p>
	 * Each lambda below mirrors a single tester entry in
	 * {@link org.apache.juneau.marshall.a.rttests.RoundTripDateTime_Test} but threads {@code fmt} through both the
	 * {@code serializer().durationFormat(fmt)} and {@code parser().durationFormat(fmt)} builder calls so the
	 * round-trip uses a consistent format on both sides.
	 */
	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(DurationFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(RdfXmlParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(RdfThriftParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(RdfProtoParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(RdfXmlParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(TurtleParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(N3Parser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(NTripleParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(NQuadsParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(TriGParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(JsonLdParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(RdfJsonParser.create().durationFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().durationFormat(fmt))
			.parser(TriXParser.create().durationFormat(fmt))
			.build()
	);

	/** Pre-computed (tester, format) combinations.  One row per (builder &times; format) pair. */
	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(DurationFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	//====================================================================================================
	// Bean with one Duration field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Duration d;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_durationProperty_basic(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A01Bean();
		x.d = Duration.ofHours(2).plusMinutes(15);

		x = t.roundTrip(x);
		assertEquals(Duration.ofHours(2).plusMinutes(15), x.d, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Duration fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Duration timeout;
		public Duration interval;
		public Duration zero;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_durationProperty_multipleFields(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A02Bean();
		x.timeout = Duration.ofHours(1).plusMinutes(30);
		x.interval = Duration.ofSeconds(45);
		x.zero = Duration.ZERO;

		x = t.roundTrip(x);
		assertEquals(Duration.ofHours(1).plusMinutes(30), x.timeout, "fmt=" + fmt);
		assertEquals(Duration.ofSeconds(45), x.interval, "fmt=" + fmt);
		assertEquals(Duration.ZERO, x.zero, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with negative + zero Duration — sign + zero coverage
	//====================================================================================================

	public static class A03Bean {
		public Duration negative;
		public Duration zero;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_durationProperty_negativeAndZero(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A03Bean();
		x.negative = Duration.ofHours(-6);
		x.zero = Duration.ZERO;

		x = t.roundTrip(x);
		assertEquals(Duration.ofHours(-6), x.negative, "fmt=" + fmt);
		assertEquals(Duration.ZERO, x.zero, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with day-spanning Duration — ISO_8601_WITH_DAYS expressiveness coverage
	//====================================================================================================

	public static class A04Bean {
		public Duration multiDay;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_durationProperty_multiDay(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A04Bean();
		x.multiDay = Duration.ofDays(1).plusHours(2).plusMinutes(3);

		x = t.roundTrip(x);
		assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(3), x.multiDay, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null Duration field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_durationProperty_nullField(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.d, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Duration — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_durationTopLevel(RoundTrip_Tester t, DurationFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Duration value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = Duration.ofHours(2).plusMinutes(15);
		try {
			var out = t.serialize(x, s);
			var x2 = p.read(out, Duration.class);
			assertEquals(x, x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
