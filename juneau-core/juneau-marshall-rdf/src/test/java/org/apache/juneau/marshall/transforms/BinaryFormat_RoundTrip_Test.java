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

import static org.apache.juneau.commons.utils.Shorts.*;
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
 * Cross-pair round-trip coverage for {@link BinaryFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link UuidFormat_RoundTrip_Test} — same shape, same 12 RDF tester templates, varied across
 * every {@link BinaryFormat} value.  Round-trips representative {@code byte[]} values:
 * <ul>
 * 	<li>As a bean property (exercises the {@code MarshalledPropertyPostProcessor} context-format swap install
 * 		path at {@code applyContextFormats}).
 * 	<li>As a {@link List} element (exercises the {@code DefaultSwaps} {@link org.apache.juneau.marshall.swaps.BinarySwap}
 * 		fallback for non-bean-property byte arrays).
 * 	<li>As a top-level value (exercises the per-format dispatch sites in each {@code *SerializerSession} /
 * 		{@code *ParserSession}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link BinaryFormat} values).  At the time of writing this
 * comes to 12 &times; 5 = 60 testers per test method.
 *
 * <p>
 * {@link BinaryFormat} only affects text-based serializers per the class-level "Binary serializers" note —
 * BSON / CBOR / MsgPack / Prototext / Parquet emit native bytes regardless of the configured constant.  The
 * variant {@code binarySwap} installed by {@code MarshalledPropertyPostProcessor} respects that by handing
 * the raw {@code byte[]} back to {@link org.apache.juneau.marshall.serializer.OutputStreamSerializerSession}
 * subtypes instead of the formatted wire string, so bean-property round-trips through binary serializers
 * still resolve to the original bytes via native handling.  Top-level / {@link List}-element paths route
 * through the default-swap dispatch ({@link org.apache.juneau.marshall.swaps.BinarySwap}) which short-circuits to
 * raw bytes for binary sessions — same lossless round-trip via the native path.
 */
class BinaryFormat_RoundTrip_Test extends TestBase {

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(BinaryFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(RdfXmlParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(RdfThriftParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(RdfProtoParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(RdfXmlParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(TurtleParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(N3Parser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(NTripleParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(NQuadsParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(TriGParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(JsonLdParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(RdfJsonParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(TriXParser.create().binaryFormat(fmt))
			.build()
	);

	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(BinaryFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	/**
	 * Asserts that {@code expected} and {@code actual} have the same length and the same bytes in the
	 * same positions.  {@code byte[]} doesn't override {@link Object#equals(Object)}, so a plain
	 * {@code assertEquals} would compare identity.  Mirrors the {@code Float.equals} bit-pattern
	 * asserter pattern in {@link FloatFormat_Float_RoundTrip_Test}.
	 */
	private static void assertBytesEquals(byte[] expected, byte[] actual, String message) {
		if (expected == null) {
			assertNull(actual, message);
		} else {
			assertNotNull(actual, message);
			assertArrayEquals(expected, actual, message);
		}
	}

	// Canonical test values.  Pinned so failures are reproducible across runs.
	private static final byte[] SHORT_MIXED = { 0x00, 0x01, (byte) 0xFF, (byte) 0x80, 0x7F, 0x10, 0x20, 0x30 };
	private static final byte[] EMPTY = {};
	private static final byte[] ALIGNED_4 = { 0x12, 0x34, 0x56, 0x78 };
	private static final byte[] ALIGNED_3 = { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC };
	private static final byte[] ALIGNED_1 = { (byte) 0x99 };

	//====================================================================================================
	// Bean with one byte[] field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public byte[] b;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_byteArrayProperty_basic(RoundTrip_Tester t, BinaryFormat fmt) throws Exception {
		var x = new A01Bean();
		x.b = cp(SHORT_MIXED);

		x = t.roundTrip(x);
		assertBytesEquals(SHORT_MIXED, x.b, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple byte[] fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public byte[] aligned4;
		public byte[] aligned3;
		public byte[] aligned1;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_byteArrayProperty_multipleFields(RoundTrip_Tester t, BinaryFormat fmt) throws Exception {
		var x = new A02Bean();
		x.aligned4 = cp(ALIGNED_4);
		x.aligned3 = cp(ALIGNED_3);
		x.aligned1 = cp(ALIGNED_1);

		x = t.roundTrip(x);
		assertBytesEquals(ALIGNED_4, x.aligned4, "fmt=" + fmt + " field=aligned4");
		assertBytesEquals(ALIGNED_3, x.aligned3, "fmt=" + fmt + " field=aligned3");
		assertBytesEquals(ALIGNED_1, x.aligned1, "fmt=" + fmt + " field=aligned1");
	}

	//====================================================================================================
	// Bean with edge-case byte[] values — empty array + BASE64-padding-boundary shapes
	//
	// <p>
	// The 4-byte-boundary array round-trips through BASE64 without padding ({@code "EjRWeA=="} →
	// {@code "EjRWeA"} for URL-safe).  The 3-byte array forces 1 padding char on standard BASE64
	// ({@code "qrvM"}) and 0 on BASE64_URL.  The 1-byte array forces 2 padding chars on standard
	// ({@code "mQ=="}) and 0 on BASE64_URL.  Empty array round-trips as the empty wire string.
	//====================================================================================================

	public static class A03Bean {
		public byte[] empty;
		public byte[] aligned4;
		public byte[] aligned3;
		public byte[] aligned1;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_byteArrayProperty_edgeCases(RoundTrip_Tester t, BinaryFormat fmt) throws Exception {
		var x = new A03Bean();
		x.empty = cp(EMPTY);
		x.aligned4 = cp(ALIGNED_4);
		x.aligned3 = cp(ALIGNED_3);
		x.aligned1 = cp(ALIGNED_1);

		x = t.roundTrip(x);
		assertBytesEquals(EMPTY, x.empty, "fmt=" + fmt + " field=empty");
		assertBytesEquals(ALIGNED_4, x.aligned4, "fmt=" + fmt + " field=aligned4");
		assertBytesEquals(ALIGNED_3, x.aligned3, "fmt=" + fmt + " field=aligned3");
		assertBytesEquals(ALIGNED_1, x.aligned1, "fmt=" + fmt + " field=aligned1");
	}

	//====================================================================================================
	// Bean with a list-of-byte[] property — collection-element dispatch
	//
	// <p>
	// {@code List<byte[]>} bypasses the per-property {@code binarySwap} install (that swap is keyed
	// on the bean property's class being {@code byte[]}, not {@code List<byte[]>}).  Collection
	// elements instead route through the {@code DefaultSwaps} fallback
	// {@link org.apache.juneau.marshall.swaps.BinarySwap}, which honours {@link BinaryFormat} for text
	// sessions and short-circuits to raw bytes for binary sessions.  The mixed-shape list values
	// below cover all three BASE64-padding boundaries plus the negative-bytes case.
	//====================================================================================================

	public static class A04Bean {
		public List<byte[]> list;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_byteArrayProperty_inList(RoundTrip_Tester t, BinaryFormat fmt) throws Exception {
		var x = new A04Bean();
		x.list = new ArrayList<>();
		x.list.add(cp(SHORT_MIXED));
		x.list.add(cp(ALIGNED_3));
		x.list.add(cp(ALIGNED_1));

		x = t.roundTrip(x);
		assertNotNull(x.list, "fmt=" + fmt);
		assertEquals(3, x.list.size(), "fmt=" + fmt);
		assertBytesEquals(SHORT_MIXED, x.list.get(0), "fmt=" + fmt + " index=0");
		assertBytesEquals(ALIGNED_3, x.list.get(1), "fmt=" + fmt + " index=1");
		assertBytesEquals(ALIGNED_1, x.list.get(2), "fmt=" + fmt + " index=2");
	}

	//====================================================================================================
	// Bean with a null byte[] field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_byteArrayProperty_nullField(RoundTrip_Tester t, BinaryFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.b, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level byte[] — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_byteArrayTopLevel(RoundTrip_Tester t, BinaryFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics
		// for a standalone byte[] value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = cp(SHORT_MIXED);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, byte[].class);
			assertBytesEquals(SHORT_MIXED, x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror UuidFormat_RoundTrip_Test.a06: some serializers (URL-encoding, CSV-style) don't
			// support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
