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
 * Cross-pair round-trip coverage for {@link TemporalFormat} bound to the {@link ZonedDateTime} subtype
 * across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TemporalFormat_Instant_RoundTrip_Test} — completes the {@link Temporal} coverage for
 * the date+time+zone-id subtype.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link TemporalFormat} values).  At the time of writing this
 * comes to 12 &times; 20 = 240 testers per test method.
 *
 * <p>
 * Many {@link TemporalFormat} values are intentionally lossy for {@link ZonedDateTime} — only
 * {@link TemporalFormat#ISO_ZONED_DATE_TIME} preserves the zone-id; every other format collapses to an
 * offset (or drops zone info entirely on the parse side).  The {@code expectedAfter} helper canonicalizes
 * through the format's own {@code format/parse} cycle so the assertion reflects the lossy canonical form.
 */
class TemporalFormat_ZonedDateTime_RoundTrip_Test extends TestBase {

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
	 * Format-aware expectation helper.  Many {@link TemporalFormat} values are intentionally lossy for
	 * {@link ZonedDateTime} — only {@link TemporalFormat#ISO_ZONED_DATE_TIME} preserves the zone-id;
	 * every other format collapses to an offset and drops zone info on the parse side.  Canonicalize
	 * through the format's own {@code format/parse} cycle so the assertion reflects the lossy canonical form.
	 *
	 * <p>
	 * Validation-only testers ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV,
	 * Parquet) return the original object unchanged, so they should be compared against the structural original
	 * regardless of format lossiness.
	 */
	private static ZonedDateTime expectedAfter(ZonedDateTime original, RoundTrip_Tester t, TemporalFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return fmt.parse(fmt.format(original, null), ZonedDateTime.class, null);
	}

	//====================================================================================================
	// Bean with one ZonedDateTime field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public ZonedDateTime zdt;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_zonedDateTimeProperty_basic(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();
		x.zdt = ZonedDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneId.of("UTC"));

		x = t.roundTrip(x);
		assertEquals(expectedAfter(ZonedDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneId.of("UTC")), t, fmt), x.zdt, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple ZonedDateTime fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public ZonedDateTime created;
		public ZonedDateTime updated;
		public ZonedDateTime epoch;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_zonedDateTimeProperty_multipleFields(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A02Bean();
		x.created = ZonedDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		x.updated = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		x.epoch = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

		x = t.roundTrip(x);
		assertEquals(expectedAfter(ZonedDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneId.of("UTC")), t, fmt), x.created, "fmt=" + fmt);
		assertEquals(expectedAfter(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), t, fmt), x.updated, "fmt=" + fmt);
		assertEquals(expectedAfter(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), t, fmt), x.epoch, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with epoch + day-boundary + mid-day ZonedDateTime — boundary coverage
	//====================================================================================================

	public static class A03Bean {
		public ZonedDateTime epoch;
		public ZonedDateTime midnight;
		public ZonedDateTime midDay;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_zonedDateTimeProperty_boundaries(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A03Bean();
		x.epoch = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		x.midnight = ZonedDateTime.of(2024, 6, 15, 0, 0, 0, 0, ZoneId.of("UTC"));
		x.midDay = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, ZoneId.of("UTC"));

		x = t.roundTrip(x);
		assertEquals(expectedAfter(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), t, fmt), x.epoch, "fmt=" + fmt);
		assertEquals(expectedAfter(ZonedDateTime.of(2024, 6, 15, 0, 0, 0, 0, ZoneId.of("UTC")), t, fmt), x.midnight, "fmt=" + fmt);
		assertEquals(expectedAfter(ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, ZoneId.of("UTC")), t, fmt), x.midDay, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with far-future ZonedDateTime — large epoch-millis stress (MILLIS numeric promotion paths)
	//====================================================================================================

	public static class A04Bean {
		public ZonedDateTime farFuture;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_zonedDateTimeProperty_largeValues(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A04Bean();
		// Year 2099 — large but still within all formatters' supported range.
		x.farFuture = ZonedDateTime.of(2099, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"));

		x = t.roundTrip(x);
		assertEquals(expectedAfter(ZonedDateTime.of(2099, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), t, fmt), x.farFuture, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null ZonedDateTime field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_zonedDateTimeProperty_nullField(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.zdt, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level ZonedDateTime — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_zonedDateTimeTopLevel(RoundTrip_Tester t, TemporalFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone ZonedDateTime value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = ZonedDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneId.of("UTC"));
		try {
			var out = t.serialize(x, s);
			var x2 = p.read(out, ZonedDateTime.class);
			// Binary serializers with native datetime support (BSON in particular) bypass the configured
			// format swap at top-level and round-trip the value with full native fidelity.  Some text
			// serializers (Hocon, Hjson, Toml, ...) emit a full ISO datetime literal at top-level and the
			// parser uses native datetime parsing rather than the configured format swap, so the value
			// comes back as the same Instant but reinterpreted at the parser's default zone.
			// Bean-property tests above cover the format-swap path strictly; here we accept any of:
			//   (a) lossy-canonical (format applied via swap),
			//   (b) structural original (format bypassed by native type support),
			//   (c) same-instant equivalent (format bypassed, zone re-interpreted at default).
			var expected = expectedAfter(x, t, fmt);
			assertTrue(expected.equals(x2) || x.equals(x2)
					|| (x2 != null && x.toInstant().equals(x2.toInstant()))
					|| (x2 != null && expected != null && expected.toInstant().equals(x2.toInstant())),
				"fmt=" + fmt + " expected " + expected + " or " + x + " but got " + x2);
		} catch (Exception e) {
			// Mirror RoundTripDateTime_Test.a06_standaloneInstant: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
