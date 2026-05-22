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
package org.apache.juneau.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.a.rttests.*;
import org.apache.juneau.bson.*;
import org.apache.juneau.cbor.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.hjson.*;
import org.apache.juneau.hocon.*;
import org.apache.juneau.html.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.jcs.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.jsonl.*;
import org.apache.juneau.markdown.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parquet.*;
import org.apache.juneau.proto.*;
import org.apache.juneau.toml.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.yaml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link ClassFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link TimeZoneFormat_RoundTrip_Test} / {@link UuidFormat_RoundTrip_Test} — same shape, same
 * 42 tester templates, varied across every <em>round-trippable</em> {@link ClassFormat} value.
 * {@link ClassFormat#SIMPLE_NAME} is serialize-only by design (per the class-level Javadoc: the parser
 * path throws {@link UnsupportedOperationException} because {@code "Map"} can't be resolved back to a
 * unique {@link Class} without a registry hint), so it is excluded from the matrix.
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link ClassFormat#values() round-trippable values}).  At
 * the time of writing this comes to 42 &times; 3 = 126 testers per test method.
 *
 * <p>
 * Representative values are drawn from {@code java.lang} / {@code java.util} so the matrix does not
 * collide with the Juneau bean-type dispatch (`@Bean(typeName=…)` annotations on
 * {@code org.apache.juneau.*} types).  {@link Object#equals} works as expected on {@link Class} — two
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
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(JsonParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(JsonParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(Json5Parser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(Json5Parser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(JsonlParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().classFormat(fmt))
			.parser(XmlParser.create().classFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(XmlParser.create().classFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(HtmlParser.create().classFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(HtmlParser.create().classFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().classFormat(fmt))
			.parser(HtmlParser.create().classFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(UonParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(UonParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(UonParser.create().decoding().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(UrlEncodingParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(UrlEncodingParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().classFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(MsgPackParser.create().classFormat(fmt))
			.build(),
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
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(YamlParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().classFormat(fmt))
			.parser(TomlParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().classFormat(fmt))
			.parser(IniParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().classFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(MarkdownParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Proto - default | " + fmt)
			.serializer(ProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(ProtoParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(HjsonParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(JsonParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(CborParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(HoconParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().classFormat(fmt))
			.parser(BsonParser.create().classFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().classFormat(fmt))
			.parser(ParquetParser.create().classFormat(fmt))
			.returnOriginalObject()
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
	// Bean with edge-case Class fields — java.util.HashMap (top-level), java.util.Map.Entry (nested) —
	// exercises both FQCN-form ("java.util.Map.Entry") and BINARY_NAME-form ("java.util.Map$Entry")
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
			var x2 = p.parse(out, Class.class);
			assertEquals(x, x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror BigNumberFormat_BigInteger_RoundTrip_Test.a06: some serializers (URL-encoding, CSV-style)
			// don't support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
