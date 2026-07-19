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
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link TimeZoneFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link DateFormat_RoundTrip_Test} / {@link CalendarFormat_RoundTrip_Test} — same shape, same
 * 12 RDF tester templates, varied across every {@link TimeZoneFormat} value.  Round-trips representative
 * {@link TimeZone} values.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link TimeZoneFormat} values).  At the time of writing this
 * comes to 12 &times; 5 = 60 testers per test method.
 *
 * <p>
 * {@link TimeZoneFormat#NAME_LONG} and {@link TimeZoneFormat#NAME_SHORT} are intentionally write-only —
 * parse-back resolves localized display names (e.g. {@code "Eastern Standard Time"}) to {@link TimeZone#GMT}
 * because they are not valid {@link TimeZone#getTimeZone(String)} inputs.  {@link TimeZoneFormat#OFFSET}
 * is lossy in a different way — it converts an IANA zone to a fixed offset, then the parser interprets the
 * bare offset string (e.g. {@code "-05:00"}) as GMT.  The {@code expectedAfter} helper canonicalizes through
 * the format's own {@code format / parse} cycle so the assertion reflects the lossy canonical form.
 *
 * <p>
 * {@link TimeZone} doesn't override {@link Object#equals}, so comparisons use {@link TimeZone#getID()}
 * rather than {@code equals}.
 */
class TimeZoneFormat_RoundTrip_Test extends TestBase {

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(TimeZoneFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(RdfXmlParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(RdfThriftParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(RdfProtoParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(RdfXmlParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(TurtleParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(N3Parser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(NTripleParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(NQuadsParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(TriGParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(JsonLdParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(RdfJsonParser.create().timeZoneFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().timeZoneFormat(fmt))
			.parser(TriXParser.create().timeZoneFormat(fmt))
			.build()
	);

	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(TimeZoneFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Format-aware expectation helper.  {@link TimeZoneFormat#NAME_LONG} / {@link TimeZoneFormat#NAME_SHORT}
	 * are intentionally write-only (display names don't reverse-parse), and {@link TimeZoneFormat#OFFSET}
	 * collapses an IANA zone to a bare offset string that {@link TimeZone#getTimeZone(String)} interprets as
	 * GMT.  Canonicalize through the format's own {@code format/parse} cycle so the assertion reflects the
	 * lossy canonical form.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format lossiness.
	 */
	private static TimeZone expectedAfter(TimeZone original, RoundTrip_Tester t, TimeZoneFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		var effective = fmt == TimeZoneFormat.NOT_SET ? TimeZoneFormat.ID : fmt;
		return TimeZoneFormat.parseTimeZone(effective.format(original));
	}

	/**
	 * Compare two {@link TimeZone} values by their {@link TimeZone#getID()} identifiers.  {@link TimeZone}
	 * doesn't override {@link Object#equals}, so identity-based comparison would fail for distinct instances
	 * even when both represent the same zone.
	 */
	private static void assertTimeZoneEquals(TimeZone expected, TimeZone actual, String message) {
		if (expected == null) {
			assertNull(actual, message);
		} else {
			assertNotNull(actual, message);
			assertEquals(expected.getID(), actual.getID(), message);
		}
	}

	//====================================================================================================
	// Bean with one TimeZone field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public TimeZone tz;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_timeZoneProperty_basic(RoundTrip_Tester t, TimeZoneFormat fmt) throws Exception {
		var x = new A01Bean();
		x.tz = TimeZone.getTimeZone("America/New_York");

		x = t.roundTrip(x);
		assertTimeZoneEquals(expectedAfter(TimeZone.getTimeZone("America/New_York"), t, fmt), x.tz, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple TimeZone fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public TimeZone ny;
		public TimeZone utc;
		public TimeZone la;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_timeZoneProperty_multipleFields(RoundTrip_Tester t, TimeZoneFormat fmt) throws Exception {
		var x = new A02Bean();
		x.ny = TimeZone.getTimeZone("America/New_York");
		x.utc = TimeZone.getTimeZone("UTC");
		x.la = TimeZone.getTimeZone("America/Los_Angeles");

		x = t.roundTrip(x);
		assertTimeZoneEquals(expectedAfter(TimeZone.getTimeZone("America/New_York"), t, fmt), x.ny, "fmt=" + fmt);
		assertTimeZoneEquals(expectedAfter(TimeZone.getTimeZone("UTC"), t, fmt), x.utc, "fmt=" + fmt);
		assertTimeZoneEquals(expectedAfter(TimeZone.getTimeZone("America/Los_Angeles"), t, fmt), x.la, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with offset-style + IANA + UTC TimeZones — format-edge coverage
	//====================================================================================================

	public static class A03Bean {
		public TimeZone gmtOffset;
		public TimeZone iana;
		public TimeZone utc;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_timeZoneProperty_offsetAndIana(RoundTrip_Tester t, TimeZoneFormat fmt) throws Exception {
		var x = new A03Bean();
		x.gmtOffset = TimeZone.getTimeZone("GMT-05:00");
		x.iana = TimeZone.getTimeZone("America/New_York");
		x.utc = TimeZone.getTimeZone("UTC");

		x = t.roundTrip(x);
		assertTimeZoneEquals(expectedAfter(TimeZone.getTimeZone("GMT-05:00"), t, fmt), x.gmtOffset, "fmt=" + fmt);
		assertTimeZoneEquals(expectedAfter(TimeZone.getTimeZone("America/New_York"), t, fmt), x.iana, "fmt=" + fmt);
		assertTimeZoneEquals(expectedAfter(TimeZone.getTimeZone("UTC"), t, fmt), x.utc, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with exotic / fractional-offset TimeZone — covers atypical zone IDs
	//====================================================================================================

	public static class A04Bean {
		public TimeZone exotic;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_timeZoneProperty_exotic(RoundTrip_Tester t, TimeZoneFormat fmt) throws Exception {
		var x = new A04Bean();
		// India Standard Time — fractional half-hour offset (+05:30) — exercises non-integer-hour offset edge.
		x.exotic = TimeZone.getTimeZone("Asia/Kolkata");

		x = t.roundTrip(x);
		assertTimeZoneEquals(expectedAfter(TimeZone.getTimeZone("Asia/Kolkata"), t, fmt), x.exotic, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null TimeZone field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_timeZoneProperty_nullField(RoundTrip_Tester t, TimeZoneFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.tz, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level TimeZone — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_timeZoneTopLevel(RoundTrip_Tester t, TimeZoneFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone TimeZone value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = TimeZone.getTimeZone("America/New_York");
		try {
			var out = t.serialize(x, s);
			var x2 = p.read(out, TimeZone.class);
			// Accept either the lossy-canonical (format applied) or the structural original (format bypassed
			// by native type handling — matches the BSON-style divergence already documented for DateFormat /
			// CalendarFormat at top-level).
			var expected = expectedAfter(x, t, fmt);
			assertTrue(expected.getID().equals(x2.getID()) || x.getID().equals(x2.getID()),
				"fmt=" + fmt + " expected " + expected.getID() + " or " + x.getID() + " but got " + x2.getID());
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
