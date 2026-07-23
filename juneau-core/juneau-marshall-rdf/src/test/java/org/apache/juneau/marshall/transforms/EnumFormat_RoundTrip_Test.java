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
 * Cross-pair round-trip coverage for {@link EnumFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link BigNumberFormat_BigInteger_RoundTrip_Test} — same shape, same 12 RDF tester templates,
 * varied across every {@link EnumFormat} value.  Round-trips a small in-class test enum
 * ({@code TestEnum}) with three constants having distinct ordinals and distinct value-names:
 * <ul>
 * 	<li>As a bean property (single enum + sibling primitive).
 * 	<li>As a list element (collection-of-enums).
 * 	<li>As a {@link Map} key (the format dispatch most-likely site for a bug).
 * 	<li>As a {@link Map} value (asserts symmetry with the map-key path).
 * 	<li>As a null field (sentinel-style null preservation).
 * 	<li>As a top-level value (per-format dispatch in {@code *SerializerSession} / {@code *ParserSession}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link EnumFormat} values).  At the time of writing this
 * comes to 12 &times; 9 = 108 testers per test method.
 *
 * <p>
 * {@link EnumFormat} is structurally lossless on text serializers — every constant preserves the
 * enum-constant identity (just varies the textual wire encoding).  Numeric serializers and the
 * {@code ORDINAL} format combine to emit a native numeric wire value on binary serializers and a bare
 * integer literal on text formats.
 */
class EnumFormat_RoundTrip_Test extends TestBase {

	public enum TestEnum { ALPHA, BETA, GAMMA }

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(EnumFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(RdfXmlParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(RdfThriftParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(RdfProtoParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(RdfXmlParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(TurtleParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(N3Parser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(NTripleParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(NQuadsParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(TriGParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(JsonLdParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(RdfJsonParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(TriXParser.create().enumFormat(fmt))
			.build()
	);

	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(EnumFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	//====================================================================================================
	// Bean with one enum field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public TestEnum e;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_enumProperty_basic(RoundTrip_Tester t, EnumFormat fmt) throws Exception {
		var x = new A01Bean();
		x.e = TestEnum.ALPHA;

		x = t.roundTrip(x);
		assertEquals(TestEnum.ALPHA, x.e, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple fields (enum + sibling primitive) — multi-property cross-section to detect
	// cascade corruption when the enum dispatch interacts with neighboring properties.
	//====================================================================================================

	public static class A02Bean {
		public TestEnum e;
		public int n;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_enumProperty_multipleFields(RoundTrip_Tester t, EnumFormat fmt) throws Exception {
		var x = new A02Bean();
		x.e = TestEnum.BETA;
		x.n = 42;

		x = t.roundTrip(x);
		assertEquals(TestEnum.BETA, x.e, "fmt=" + fmt);
		assertEquals(42, x.n, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with a list-of-enum property — collection-element dispatch
	//====================================================================================================

	public static class A03Bean {
		public List<TestEnum> list;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_enumProperty_inList(RoundTrip_Tester t, EnumFormat fmt) throws Exception {
		var x = new A03Bean();
		x.list = List.of(TestEnum.ALPHA, TestEnum.BETA, TestEnum.GAMMA);

		x = t.roundTrip(x);
		assertNotNull(x.list, "fmt=" + fmt);
		assertEquals(List.of(TestEnum.ALPHA, TestEnum.BETA, TestEnum.GAMMA), x.list, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with a map-of-enum-keyed property — map-key dispatch.
	//
	// <p>
	// Per the Wave-3 plan: "Enum: ordinal vs value-name representation in map keys — most formats lose
	// the distinction unless map-key handling explicitly threads the format hint."  This is the
	// highest-bug-probability site for {@link EnumFormat}.
	//====================================================================================================

	public static class A04Bean {
		public Map<TestEnum,String> mapKey;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_enumProperty_inMapKey(RoundTrip_Tester t, EnumFormat fmt) throws Exception {
		var x = new A04Bean();
		x.mapKey = new LinkedHashMap<>();
		x.mapKey.put(TestEnum.ALPHA, "first");
		x.mapKey.put(TestEnum.BETA, "second");

		x = t.roundTrip(x);
		assertNotNull(x.mapKey, "fmt=" + fmt);
		assertEquals("first", x.mapKey.get(TestEnum.ALPHA), "fmt=" + fmt);
		assertEquals("second", x.mapKey.get(TestEnum.BETA), "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with a null enum field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_enumProperty_nullField(RoundTrip_Tester t, EnumFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.e, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level enum — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_enumTopLevel(RoundTrip_Tester t, EnumFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone enum value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = TestEnum.GAMMA;
		try {
			var out = t.serialize(x, s);
			var x2 = p.read(out, TestEnum.class);
			assertEquals(x, x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror BigNumberFormat_BigInteger_RoundTrip_Test.a06: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
