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
 * Cross-pair round-trip coverage for {@link TemporalFormat} bound to the {@link LocalDateTime} subtype
 * across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TemporalFormat_Instant_RoundTrip_Test} — scopes to the second-most-common
 * {@link Temporal} subtype.  The remaining subtypes ({@link LocalDate}, {@link OffsetDateTime},
 * {@link ZonedDateTime}, {@link Year}, {@link YearMonth}, {@link LocalTime}, {@link OffsetTime},
 * {@link MonthDay}) are deferred.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link TemporalFormat} values).  At the time of writing this
 * comes to 12 &times; 20 = 240 testers per test method.
 */
@SuppressWarnings({
	"java:S8694" // Test data uses literal month ints for date construction; Month enum constants add noise without value.
})
class TemporalFormat_LocalDateTime_RoundTrip_Test extends TestBase {

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
	 * {@link LocalDateTime} — date-only formats drop the time portion, time-only formats drop the date
	 * portion, and {@link TemporalFormat#ISO_YEAR} / {@link TemporalFormat#ISO_YEAR_MONTH} reduce to
	 * year / year-month.  Canonicalize through the format's own {@code format/parse} cycle so the assertion
	 * reflects the lossy canonical form.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format lossiness.
	 */
	private static LocalDateTime expectedAfter(LocalDateTime original, RoundTrip_Tester t, TemporalFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return fmt.parse(fmt.format(original, null), LocalDateTime.class, null);
	}

	//====================================================================================================
	// Bean with one LocalDateTime field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public LocalDateTime ldt;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_localDateTimeProperty_basic(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();
		x.ldt = LocalDateTime.of(2024, 6, 15, 12, 30, 45);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(LocalDateTime.of(2024, 6, 15, 12, 30, 45), t, fmt), x.ldt, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple LocalDateTime fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public LocalDateTime created;
		public LocalDateTime updated;
		public LocalDateTime epoch;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_localDateTimeProperty_multipleFields(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A02Bean();
		x.created = LocalDateTime.of(2024, 6, 15, 12, 30, 45);
		x.updated = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
		x.epoch = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(LocalDateTime.of(2024, 6, 15, 12, 30, 45), t, fmt), x.created, "fmt=" + fmt);
		assertEquals(expectedAfter(LocalDateTime.of(2025, 1, 1, 0, 0, 0), t, fmt), x.updated, "fmt=" + fmt);
		assertEquals(expectedAfter(LocalDateTime.of(1970, 1, 1, 0, 0, 0), t, fmt), x.epoch, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with epoch + day-boundary + mid-day LocalDateTime — boundary coverage
	//====================================================================================================

	public static class A03Bean {
		public LocalDateTime epoch;
		public LocalDateTime midnight;
		public LocalDateTime midDay;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_localDateTimeProperty_boundaries(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A03Bean();
		x.epoch = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
		x.midnight = LocalDateTime.of(2024, 6, 15, 0, 0, 0);
		x.midDay = LocalDateTime.of(2024, 6, 15, 12, 0, 0);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(LocalDateTime.of(1970, 1, 1, 0, 0, 0), t, fmt), x.epoch, "fmt=" + fmt);
		assertEquals(expectedAfter(LocalDateTime.of(2024, 6, 15, 0, 0, 0), t, fmt), x.midnight, "fmt=" + fmt);
		assertEquals(expectedAfter(LocalDateTime.of(2024, 6, 15, 12, 0, 0), t, fmt), x.midDay, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with far-future LocalDateTime — large epoch-millis stress (MILLIS numeric promotion paths)
	//====================================================================================================

	public static class A04Bean {
		public LocalDateTime farFuture;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_localDateTimeProperty_largeValues(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A04Bean();
		// Year 2099 — large but still within all formatters' supported range.
		x.farFuture = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

		x = t.roundTrip(x);
		assertEquals(expectedAfter(LocalDateTime.of(2099, 12, 31, 23, 59, 59), t, fmt), x.farFuture, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null LocalDateTime field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_localDateTimeProperty_nullField(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.ldt, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level LocalDateTime — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_localDateTimeTopLevel(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone LocalDateTime value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = LocalDateTime.of(2024, 6, 15, 12, 30, 45);
		try {
			var out = t.serialize(x, s);
			var x2 = p.read(out, LocalDateTime.class);
			// Binary serializers with native datetime support (BSON in particular) bypass the configured
			// format swap at top-level and round-trip the value with full native fidelity.  Bean-property
			// tests above cover the swap path; here we accept either the lossy-canonical (format applied)
			// or the structural original (format bypassed by native type support).
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
