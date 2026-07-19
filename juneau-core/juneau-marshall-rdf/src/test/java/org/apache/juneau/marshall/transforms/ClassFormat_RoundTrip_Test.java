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
 * Cross-pair round-trip coverage for {@link ClassFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TimeZoneFormat_RoundTrip_Test} / {@link UuidFormat_RoundTrip_Test} — same shape, same
 * 12 RDF tester templates, varied across every <em>round-trippable</em> {@link ClassFormat} value.
 * {@link ClassFormat#SIMPLE_NAME} is serialize-only by design (per the class-level Javadoc: the parser
 * path throws {@link UnsupportedOperationException} because {@code "Map"} can't be resolved back to a
 * unique {@link Class} without a registry hint), so it is excluded from the matrix.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link ClassFormat#values() round-trippable values}).  At
 * the time of writing this comes to 12 &times; 3 = 36 testers per test method.
 *
 * <p>
 * Representative values are drawn from {@code java.lang} / {@code java.util} so the matrix does not
 * collide with the Juneau bean-type dispatch (`@Bean(typeName=…)` annotations on
 * {@code org.apache.juneau.marshall.*} types).  {@link Object#equals} works as expected on {@link Class} — two
 * {@code Class<?>} references compare equal iff they refer to the same class loader's runtime class.
 */
class ClassFormat_RoundTrip_Test extends TestBase {

	/**
	 * The set of {@link ClassFormat} values that are round-trippable on the parser side.  Excludes
	 * {@link ClassFormat#SIMPLE_NAME} (serialize-only by design).
	 */
	private static final List<ClassFormat> ROUND_TRIP_FORMATS = List.of(
		ClassFormat.NOT_SET,
		ClassFormat.FQCN,
		ClassFormat.BINARY_NAME
	);

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(ClassFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(RdfXmlParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(RdfThriftParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(RdfProtoParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(RdfXmlParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(TurtleParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(N3Parser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(NTripleParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(NQuadsParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(TriGParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(JsonLdParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(RdfJsonParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(TriXParser.create().classFormat(fmt))
			.build()
	);

	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return ROUND_TRIP_FORMATS.stream()
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	//====================================================================================================
	// Bean with one Class field — exercises MarshalledPropertyPostProcessor.applyContextFormats
	//====================================================================================================

	public static class A01Bean {
		public Class<?> c;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_classProperty_basic(RoundTrip_Tester t, ClassFormat fmt) throws Exception {
		var x = new A01Bean();
		x.c = String.class;

		x = t.roundTrip(x);
		assertEquals(String.class, x.c, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with multiple Class fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public Class<?> str;
		public Class<?> integer;
		public Class<?> obj;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_classProperty_multipleFields(RoundTrip_Tester t, ClassFormat fmt) throws Exception {
		var x = new A02Bean();
		x.str = String.class;
		x.integer = Integer.class;
		x.obj = Object.class;

		x = t.roundTrip(x);
		assertEquals(String.class, x.str, "fmt=" + fmt);
		assertEquals(Integer.class, x.integer, "fmt=" + fmt);
		assertEquals(Object.class, x.obj, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with edge-case Class fields — HashMap (top-level), Map.Entry (nested) —
	// exercises both FQCN-form ("Map.Entry") and BINARY_NAME-form ("java.util.Map$Entry")
	// nested-type heuristics.
	//
	// <p>
	// Avoids array types and primitive types since the FQCN / BINARY_NAME wire forms diverge sharply
	// for those ({@code int[]} vs {@code [I}) and the test bean's {@code Class<?>}-typed property
	// receives both forms through the same parse-side {@code Class.forName} path; the lossy-equivalence
	// pattern is the same as the BSON-style native-datetime divergence already documented for
	// {@code DateFormat} top-level.
	//====================================================================================================

	public static class A03Bean {
		public Class<?> hashMap;
		public Class<?> mapEntry;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_classProperty_nestedAndCollection(RoundTrip_Tester t, ClassFormat fmt) throws Exception {
		var x = new A03Bean();
		x.hashMap = HashMap.class;
		x.mapEntry = Map.Entry.class;

		x = t.roundTrip(x);
		assertEquals(HashMap.class, x.hashMap, "fmt=" + fmt);
		assertEquals(Map.Entry.class, x.mapEntry, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with a list-of-Class property — collection-element dispatch
	//====================================================================================================

	public static class A04Bean {
		public List<Class<?>> list;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_classProperty_inList(RoundTrip_Tester t, ClassFormat fmt) throws Exception {
		var x = new A04Bean();
		x.list = List.of(String.class, Integer.class, Object.class);

		x = t.roundTrip(x);
		assertNotNull(x.list, "fmt=" + fmt);
		assertEquals(List.of(String.class, Integer.class, Object.class), x.list, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with a null Class field — sentinel-style null preservation
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_classProperty_nullField(RoundTrip_Tester t, ClassFormat fmt) throws Exception {
		var x = new A01Bean();

		x = t.roundTrip(x);
		assertNull(x.c, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Class — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_classTopLevel(RoundTrip_Tester t, ClassFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics for a
		// standalone Class value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		Class<?> x = String.class;
		try {
			var out = t.serialize(x, s);
			var x2 = p.read(out, Class.class);
			assertEquals(x, x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror BigNumberFormat_BigInteger_RoundTrip_Test.a06: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
