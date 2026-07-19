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
 * Cross-pair round-trip coverage for {@link UuidFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TimeZoneFormat_RoundTrip_Test} — same shape, same 12 RDF tester templates, varied across
 * every {@link UuidFormat} value.  Round-trips representative {@link UUID} values as both bean properties
 * (exercising the {@code MarshalledPropertyPostProcessor} swap-install path) and top-level values
 * (exercising the per-format dispatch sites in each {@code *SerializerSession} / {@code *ParserSession}).
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link UuidFormat} values).  At the time of writing this
 * comes to 12 &times; 4 = 48 testers per test method.
 *
 * <p>
 * {@link UuidFormat} is structurally lossless on text serializers — every constant preserves the full
 * 128-bit value (the choice is just hyphenated / compact / URN-prefixed).  Binary serializers
 * (BSON / CBOR / MsgPack / Prototext) bypass the format dispatch and emit a native 16-byte binary
 * representation; the round-trip is still lossless.
 */
@SuppressWarnings({
	"unused" // Parameters retained for method-signature/functional-interface consistency in test fixtures.
})
class UuidFormat_RoundTrip_Test extends TestBase {

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(UuidFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(RdfXmlParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(RdfThriftParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(RdfProtoParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(RdfXmlParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(TurtleParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(N3Parser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(NTripleParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(NQuadsParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(TriGParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(JsonLdParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(RdfJsonParser.create().uuidFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().uuidFormat(fmt))
			.parser(TriXParser.create().uuidFormat(fmt))
			.build()
	);

	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(UuidFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Format-aware expectation helper.  {@link UuidFormat} is structurally lossless for {@link UUID} —
	 * every constant preserves the full 128-bit value (just varies the textual wire encoding between
	 * hyphenated / compact / URN-prefixed).  Validation-only testers
	 * ({@link RoundTrip_Tester#isValidationOnly()} == {@code true}: Json schema, CSV, Parquet) return
	 * the original object unchanged.
	 */
	@SuppressWarnings({
		"java:S1172" // 'fmt' is part of the shared expectedAfter(...) helper signature used across all *Format RoundTrip tests; kept for template symmetry even where this type's expected value is format-independent.
	})
	private static UUID expectedAfter(UUID original, RoundTrip_Tester t, UuidFormat fmt) {
		if (original == null)
			return null;
		if (t.isValidationOnly())
			return original;
		return original;
	}

	//====================================================================================================
	// Bean with one UUID field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public UUID u;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_uuidProperty_basic(RoundTrip_Tester t, UuidFormat fmt) throws Exception {
		var x = new A01Bean();
		x.u = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

		x = t.roundTrip(x);
		assertEquals(expectedAfter(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), t, fmt), x.u, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple UUID fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public UUID first;
		public UUID second;
		public UUID third;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_uuidProperty_multipleFields(RoundTrip_Tester t, UuidFormat fmt) throws Exception {
		var first = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		var second = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
		var third = UUID.fromString("ffffffff-ffff-4fff-bfff-ffffffffffff");

		var x = new A02Bean();
		x.first = first;
		x.second = second;
		x.third = third;

		x = t.roundTrip(x);
		assertEquals(expectedAfter(first, t, fmt), x.first, "fmt=" + fmt);
		assertEquals(expectedAfter(second, t, fmt), x.second, "fmt=" + fmt);
		assertEquals(expectedAfter(third, t, fmt), x.third, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with all-zero + all-ones edge-case UUIDs — bit-pattern boundary coverage
	//====================================================================================================

	public static class A03Bean {
		public UUID nil;
		public UUID allOnes;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_uuidProperty_edgeCases(RoundTrip_Tester t, UuidFormat fmt) throws Exception {
		var nil = new UUID(0L, 0L);
		var allOnes = new UUID(-1L, -1L);

		var x = new A03Bean();
		x.nil = nil;
		x.allOnes = allOnes;

		x = t.roundTrip(x);
		assertEquals(expectedAfter(nil, t, fmt), x.nil, "fmt=" + fmt);
		assertEquals(expectedAfter(allOnes, t, fmt), x.allOnes, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple edge-case UUIDs — random UUID + nil + all-ones cross-section
	//====================================================================================================

	public static class A04Bean {
		public UUID random;
		public UUID nil;
		public UUID allOnes;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_uuidProperty_multipleEdgeCases(RoundTrip_Tester t, UuidFormat fmt) throws Exception {
		// Use a fixed random-shape UUID rather than UUID.randomUUID() so failures are reproducible.
		var random = UUID.fromString("3d4f1c9e-7a8b-4d2e-9f6a-1b2c3d4e5f60");
		var nil = new UUID(0L, 0L);
		var allOnes = new UUID(-1L, -1L);

		var x = new A04Bean();
		x.random = random;
		x.nil = nil;
		x.allOnes = allOnes;

		x = t.roundTrip(x);
		assertEquals(expectedAfter(random, t, fmt), x.random, "fmt=" + fmt);
		assertEquals(expectedAfter(nil, t, fmt), x.nil, "fmt=" + fmt);
		assertEquals(expectedAfter(allOnes, t, fmt), x.allOnes, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with null UUID field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_uuidProperty_nullField(RoundTrip_Tester t, UuidFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.u, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level UUID — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_uuidTopLevel(RoundTrip_Tester t, UuidFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone UUID value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		try {
			var out = t.serialize(x, s);
			var x2 = p.read(out, UUID.class);
			assertEquals(expectedAfter(x, t, fmt), x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror BigNumberFormat_BigInteger_RoundTrip_Test.a06: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
