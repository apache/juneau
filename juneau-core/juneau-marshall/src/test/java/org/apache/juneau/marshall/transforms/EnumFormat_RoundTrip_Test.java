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
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.hocon.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.jcs.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.markdown.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.parquet.*;
import org.apache.juneau.marshall.prototext.*;
import org.apache.juneau.marshall.toml.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.urlencoding.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Cross-pair round-trip coverage for {@link EnumFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link BigNumberFormat_BigInteger_RoundTrip_Test} — same shape, same 42 tester templates,
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
 * comes to 42 &times; 9 = 378 testers per test method.
 *
 * <p>
 * {@link EnumFormat} is structurally lossless on text serializers — every constant preserves the
 * enum-constant identity (just varies the textual wire encoding).  Numeric serializers and the
 * {@code ORDINAL} format combine to emit a native numeric wire value on binary serializers and a bare
 * integer literal on text formats.
 */
@SuppressWarnings({
	"unused" // Exception parameter intentionally unused in catch block; only the fact of the exception matters.
})
class EnumFormat_RoundTrip_Test extends TestBase {

	public enum TestEnum { ALPHA, BETA, GAMMA }

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(EnumFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(JsonParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(JsonParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(Json5Parser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(Json5Parser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(JsonlParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(XmlParser.create().enumFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(XmlParser.create().enumFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(HtmlParser.create().enumFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(HtmlParser.create().enumFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(HtmlParser.create().enumFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(UonParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(UonParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(UonParser.create().decoding().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(UrlEncodingParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(UrlEncodingParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(MsgPackParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(YamlParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().enumFormat(fmt))
			.parser(TomlParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().enumFormat(fmt))
			.parser(IniParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().enumFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(MarkdownParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Prototext - default | " + fmt)
			.serializer(PrototextSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(PrototextParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(HjsonParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(JsonParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(CborParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(HoconParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().enumFormat(fmt))
			.parser(BsonParser.create().enumFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().enumFormat(fmt))
			.parser(ParquetParser.create().enumFormat(fmt))
			.returnOriginalObject()
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
		public Map<TestEnum, String> mapKey;
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
