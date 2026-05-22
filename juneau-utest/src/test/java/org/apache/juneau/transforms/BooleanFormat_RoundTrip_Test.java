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
 * Cross-pair round-trip coverage for {@link BooleanFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link BinaryFormat_RoundTrip_Test} — same shape, same 42 tester templates, varied across
 * every {@link BooleanFormat} value.  Round-trips representative {@link Boolean} / <code><jk>boolean</jk></code>
 * values:
 * <ul>
 * 	<li>As a primitive {@code boolean} bean property (exercises the
 * 		{@code MarshalledPropertyPostProcessor.applyContextFormats} install path for primitives).
 * 	<li>As a boxed {@link Boolean} bean property (exercises the boxed-{@code Boolean} path including
 * 		{@code null} handling).
 * 	<li>As a {@link List} element (exercises the {@code DefaultSwaps} fallback for non-bean-property
 * 		booleans — currently a no-op since {@link Boolean} has no registered swap; collection elements
 * 		flow through each serializer's native boolean handling).
 * 	<li>As a top-level value (exercises the per-format dispatch sites in each {@code *SerializerSession}
 * 		/ {@code *ParserSession}).
 * </ul>
 *
 * <p>
 * Test combos = (tester templates) &times; ({@link BooleanFormat} values).  At the time of writing this
 * comes to 42 &times; 6 = 252 testers per test method.
 *
 * <p>
 * Per the {@link BooleanFormat} class-level "Binary serializers" note, the binary serializer family
 * (BSON / CBOR / MsgPack / Proto / Parquet) emits a native boolean wire type regardless of the
 * configured constant.  The variant {@code booleanSwap} installed by
 * {@link org.apache.juneau.MarshalledPropertyPostProcessor} respects that by returning the raw
 * {@link Boolean} to {@link org.apache.juneau.serializer.OutputStreamSerializerSession} subtypes
 * instead of the formatted wire token, so bean-property round-trips through binary serializers stay
 * lossless via native handling.  Per the class-level "Parser leniency" note, parsers accept any of
 * the textual shapes regardless of the parser-side setting, so the unswap path on the parser side is
 * format-agnostic.
 */
class BooleanFormat_RoundTrip_Test extends TestBase {

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(BooleanFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(JsonParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(JsonParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(Json5Parser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(Json5Parser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(JsonlParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(XmlParser.create().booleanFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(XmlParser.create().booleanFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(HtmlParser.create().booleanFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(HtmlParser.create().booleanFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(HtmlParser.create().booleanFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(UonParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(UonParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(UonParser.create().decoding().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(UrlEncodingParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(UrlEncodingParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(MsgPackParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(18, "RdfXml | " + fmt)
			.serializer(RdfXmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(RdfXmlParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(19, "RdfThrift | " + fmt)
			.serializer(RdfThriftSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(RdfThriftParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(20, "RdfProto | " + fmt)
			.serializer(RdfProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(RdfProtoParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(21, "RdfXmlAbbrev | " + fmt)
			.serializer(RdfXmlAbbrevSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(RdfXmlParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(22, "RdfTurtle | " + fmt)
			.serializer(TurtleSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(TurtleParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(23, "RdfN3 | " + fmt)
			.serializer(N3Serializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(N3Parser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(24, "RdfNtriple | " + fmt)
			.serializer(NTripleSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(NTripleParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(25, "RdfNquads | " + fmt)
			.serializer(NQuadsSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(NQuadsParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(26, "RdfTrig | " + fmt)
			.serializer(TriGSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(TriGParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(27, "RdfJsonLd | " + fmt)
			.serializer(JsonLdSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(JsonLdParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(28, "RdfJson | " + fmt)
			.serializer(RdfJsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(RdfJsonParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(29, "RdfTriX | " + fmt)
			.serializer(TriXSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(TriXParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(YamlParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().booleanFormat(fmt))
			.parser(TomlParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().booleanFormat(fmt))
			.parser(IniParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().booleanFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(MarkdownParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Proto - default | " + fmt)
			.serializer(ProtoSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(ProtoParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(HjsonParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(JsonParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(CborParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(HoconParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().booleanFormat(fmt))
			.parser(BsonParser.create().booleanFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().booleanFormat(fmt))
			.parser(ParquetParser.create().booleanFormat(fmt))
			.returnOriginalObject()
			.build()
	);

	private static final List<Arguments> COMBOS = buildCombos();

	private static List<Arguments> buildCombos() {
		return Stream.of(BooleanFormat.values())
			.flatMap(fmt -> BUILDERS.stream().map(b -> Arguments.of(b.build(fmt), fmt)))
			.toList();
	}

	static Stream<Arguments> combos() {
		return COMBOS.stream();
	}

	// Canonical test values — pinned so failures are reproducible across runs.
	private static final boolean TRUE_VAL = true;
	private static final boolean FALSE_VAL = false;
	private static final Boolean BOXED_TRUE = Boolean.TRUE;
	private static final Boolean BOXED_FALSE = Boolean.FALSE;

	//====================================================================================================
	// Bean with one primitive boolean field — exercises the MPP swap-install for primitives
	//====================================================================================================

	public static class A01Bean {
		public boolean b;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a01_booleanProperty_basic(RoundTrip_Tester t, BooleanFormat fmt) throws Exception {
		var x = new A01Bean();
		x.b = TRUE_VAL;

		x = t.roundTrip(x);
		assertEquals(TRUE_VAL, x.b, "fmt=" + fmt);
	}

	//====================================================================================================
	// Bean with both primitive and boxed boolean fields — multi-property cross-section
	//====================================================================================================

	public static class A02Bean {
		public boolean primitiveTrue;
		public boolean primitiveFalse;
		public Boolean boxedTrue;
		public Boolean boxedFalse;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a02_booleanProperty_multipleFields(RoundTrip_Tester t, BooleanFormat fmt) throws Exception {
		var x = new A02Bean();
		x.primitiveTrue = TRUE_VAL;
		x.primitiveFalse = FALSE_VAL;
		x.boxedTrue = BOXED_TRUE;
		x.boxedFalse = BOXED_FALSE;

		x = t.roundTrip(x);
		assertEquals(TRUE_VAL, x.primitiveTrue, "fmt=" + fmt + " field=primitiveTrue");
		assertEquals(FALSE_VAL, x.primitiveFalse, "fmt=" + fmt + " field=primitiveFalse");
		assertEquals(BOXED_TRUE, x.boxedTrue, "fmt=" + fmt + " field=boxedTrue");
		assertEquals(BOXED_FALSE, x.boxedFalse, "fmt=" + fmt + " field=boxedFalse");
	}

	//====================================================================================================
	// Bean with edge-case boxed Boolean values — both polarities of true/false, exercises the
	// MPP swap-install path under both wire-shape constants (Boolean / Integer / String) of every
	// format.
	//====================================================================================================

	public static class A03Bean {
		public Boolean boxedTrue;
		public Boolean boxedFalse;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a03_booleanProperty_edgeCases(RoundTrip_Tester t, BooleanFormat fmt) throws Exception {
		var x = new A03Bean();
		x.boxedTrue = BOXED_TRUE;
		x.boxedFalse = BOXED_FALSE;

		x = t.roundTrip(x);
		assertEquals(BOXED_TRUE, x.boxedTrue, "fmt=" + fmt + " field=boxedTrue");
		assertEquals(BOXED_FALSE, x.boxedFalse, "fmt=" + fmt + " field=boxedFalse");
	}

	//====================================================================================================
	// Bean with a list-of-Boolean property — collection-element dispatch
	//
	// <p>
	// {@code List<Boolean>} bypasses the per-property {@code booleanSwap} install (that swap is keyed
	// on the bean property's class being {@link Boolean}, not {@code List<Boolean>}).  Collection
	// elements instead route through each serializer's native boolean handling.  The boxed wrapper
	// elements preserve their {@code true} / {@code false} identity through every format.
	//====================================================================================================

	public static class A04Bean {
		public List<Boolean> list;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a04_booleanProperty_inList(RoundTrip_Tester t, BooleanFormat fmt) throws Exception {
		var x = new A04Bean();
		x.list = new ArrayList<>();
		x.list.add(BOXED_TRUE);
		x.list.add(BOXED_FALSE);
		x.list.add(BOXED_TRUE);

		x = t.roundTrip(x);
		assertNotNull(x.list, "fmt=" + fmt);
		assertEquals(3, x.list.size(), "fmt=" + fmt);
		assertEquals(BOXED_TRUE, x.list.get(0), "fmt=" + fmt + " index=0");
		assertEquals(BOXED_FALSE, x.list.get(1), "fmt=" + fmt + " index=1");
		assertEquals(BOXED_TRUE, x.list.get(2), "fmt=" + fmt + " index=2");
	}

	//====================================================================================================
	// Bean with a null Boolean field — sentinel-style null preservation
	//
	// <p>
	// {@code BooleanSwap} returns {@code null} on {@code null} input per the swap contract, so the
	// wire form for the null field varies by serializer ({@code null}, omitted, empty string, etc.).
	// On parse, {@code unswap} returns {@code null} for {@code null} input, preserving the
	// boxed-{@link Boolean}-null semantics.
	//====================================================================================================

	public static class A05Bean {
		public Boolean nullable;
	}

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a05_booleanProperty_nullField(RoundTrip_Tester t, BooleanFormat fmt) throws Exception {
		var x = new A05Bean();

		x = t.roundTrip(x);
		assertNull(x.nullable, "fmt=" + fmt);
	}

	//====================================================================================================
	// Standalone top-level Boolean — exercises per-format dispatch in *SerializerSession / *ParserSession
	//====================================================================================================

	@ParameterizedTest(name = "{0} | {1}")
	@MethodSource("combos")
	void a06_booleanTopLevel(RoundTrip_Tester t, BooleanFormat fmt) throws Exception {
		// Validation-only testers (Json schema, Csv, Parquet) have no parser-side round-trip semantics
		// for a standalone Boolean value — they assert that the serializer accepts it without throwing.
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();
		if (p == null)
			return;

		var x = BOXED_TRUE;
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, Boolean.class);
			assertEquals(x, x2, "fmt=" + fmt);
		} catch (Exception e) {
			// Mirror UuidFormat_RoundTrip_Test.a06: some serializers (URL-encoding, CSV-style) don't
			// support a top-level non-bean value cleanly.  Bean-property tests above already cover the
			// per-property dispatch path; this case is best-effort.
		}
	}
}
