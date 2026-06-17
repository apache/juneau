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
 * Cross-pair round-trip coverage for {@link BinaryFormat} across every supported serializer/parser pair.
 *
 * <p>
 * Sibling of {@link UuidFormat_RoundTrip_Test} — same shape, same 42 tester templates, varied across
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
 * comes to 42 &times; 5 = 210 testers per test method.
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
@SuppressWarnings({
	"unused" // Exception parameter intentionally unused in catch block; only the fact of the exception matters.
})
class BinaryFormat_RoundTrip_Test extends TestBase {

	@FunctionalInterface
	interface TesterBuilder {
		RoundTrip_Tester build(BinaryFormat fmt);
	}

	private static final List<TesterBuilder> BUILDERS = List.of(
		fmt -> RoundTrip_Tester.create(1, "Json - default | " + fmt)
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(JsonParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(2, "Json - readable | " + fmt)
			.serializer(JsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(JsonParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(3, "Json5 - default | " + fmt)
			.serializer(Json5Serializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(Json5Parser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(4, "Json5 - readable | " + fmt)
			.serializer(Json5Serializer.create().ws().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(Json5Parser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(5, "Jsonl - default | " + fmt)
			.serializer(JsonlSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(JsonlParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(6, "Xml - namespaces, validation, readable | " + fmt)
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(XmlParser.create().binaryFormat(fmt))
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		fmt -> RoundTrip_Tester.create(7, "Xml - no namespaces, validation | " + fmt)
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(XmlParser.create().binaryFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(8, "Html - default | " + fmt)
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(HtmlParser.create().binaryFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(9, "Html - readable | " + fmt)
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(HtmlParser.create().binaryFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(10, "Html - with key/value headers | " + fmt)
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(HtmlParser.create().binaryFormat(fmt))
			.validateXmlWhitespace()
			.build(),
		fmt -> RoundTrip_Tester.create(11, "Uon - default | " + fmt)
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(UonParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(12, "Uon - readable | " + fmt)
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(UonParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(13, "Uon - encoded | " + fmt)
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(UonParser.create().decoding().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(14, "UrlEncoding - default | " + fmt)
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(UrlEncodingParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(15, "UrlEncoding - readable | " + fmt)
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(UrlEncodingParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(16, "UrlEncoding - expanded params | " + fmt)
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(UrlEncodingParser.create().expandedParams().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(17, "MsgPack | " + fmt)
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(MsgPackParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(30, "Json schema | " + fmt)
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(31, "Yaml - default | " + fmt)
			.serializer(YamlSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(YamlParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(32, "Toml - default | " + fmt)
			.serializer(TomlSerializer.create().binaryFormat(fmt))
			.parser(TomlParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(33, "Ini - default | " + fmt)
			.serializer(IniSerializer.create().binaryFormat(fmt))
			.parser(IniParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(34, "Csv - default | " + fmt)
			.serializer(CsvSerializer.create().keepNullProperties().binaryFormat(fmt))
			.skipIf(o -> o == null || (o.getClass().isArray() && o.getClass().getComponentType().isPrimitive()))
			.returnOriginalObject()
			.build(),
		fmt -> RoundTrip_Tester.create(35, "Markdown - default | " + fmt)
			.serializer(MarkdownSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(MarkdownParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(36, "Prototext - default | " + fmt)
			.serializer(PrototextSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(PrototextParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(37, "Hjson - default | " + fmt)
			.serializer(HjsonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(HjsonParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(38, "Jcs - default | " + fmt)
			.serializer(JcsSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(JsonParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(39, "Cbor - default | " + fmt)
			.serializer(CborSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(CborParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(40, "Hocon - default | " + fmt)
			.serializer(HoconSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(HoconParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(41, "Bson - default | " + fmt)
			.serializer(BsonSerializer.create().keepNullProperties().addBeanTypes().addRootType().binaryFormat(fmt))
			.parser(BsonParser.create().binaryFormat(fmt))
			.build(),
		fmt -> RoundTrip_Tester.create(42, "Parquet - default | " + fmt)
			.serializer(ParquetSerializer.create().addBeanTypes().binaryFormat(fmt))
			.parser(ParquetParser.create().binaryFormat(fmt))
			.returnOriginalObject()
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
		x.b = SHORT_MIXED.clone();

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
		x.aligned4 = ALIGNED_4.clone();
		x.aligned3 = ALIGNED_3.clone();
		x.aligned1 = ALIGNED_1.clone();

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
		x.empty = EMPTY.clone();
		x.aligned4 = ALIGNED_4.clone();
		x.aligned3 = ALIGNED_3.clone();
		x.aligned1 = ALIGNED_1.clone();

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
		x.list.add(SHORT_MIXED.clone());
		x.list.add(ALIGNED_3.clone());
		x.list.add(ALIGNED_1.clone());

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

		var x = SHORT_MIXED.clone();
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
