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
package org.apache.juneau.marshaller;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.apache.juneau.marshall.marshaller.MarshallUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

class MarshallUtils_Test extends TestBase {

	private static final JsonMap MAP = JsonMap.of("a", "b");

	@Test void a01_json() throws Exception {
		assertString("{\"a\":\"b\"}", json(MAP));
	}

	@Test void a02_json5() throws Exception {
		assertString("{a:'b'}", json5(MAP));
	}

	@Test void a03_charFormats_nonNull() throws Exception {
		assertNotNull(jsonl(MAP));
		assertNotNull(jcs(MAP));
		assertNotNull(hjson(MAP));
		assertNotNull(xml(MAP));
		assertNotNull(html(MAP));
		assertNotNull(uon(MAP));
		assertNotNull(urlEncoding(MAP));
		assertNotNull(yaml(MAP));
		assertNotNull(csv("foo"));
		assertNotNull(openApi(MAP));
		assertNotNull(plainText(MAP));
		assertNotNull(markdown(MAP));
		assertNotNull(markdownDoc(MAP));
		assertNotNull(ini(MAP));
		assertNotNull(toml(MAP));
		assertNotNull(hocon(MAP));
		assertNotNull(proto(MAP));
	}

	@Test void a04_binaryFormats() throws Exception {
		assertTrue(msgPack(MAP).length > 0);
		assertTrue(cbor(MAP).length > 0);
		assertTrue(bson(MAP).length > 0);
		assertTrue(parquet(new A04_Bean()).length > 0);
	}

	public static class A04_Bean {
		public String x = "test";
	}

	@Test void b01_parseJson() throws Exception {
		assertString("foo", json(json("foo"), String.class));
	}

	@Test void b02_parseJson5() throws Exception {
		assertString("foo", json5(json5("foo"), String.class));
	}

	@Test void b03_parseCharFormats() throws Exception {
		assertString("foo", jsonl(jsonl("foo"), String.class));
		assertString("foo", jcs(jcs("foo"), String.class));
		assertString("foo", hjson(hjson("foo"), String.class));
		assertString("foo", xml(xml("foo"), String.class));
		assertString("foo", html(html("foo"), String.class));
		assertString("foo", uon(uon("foo"), String.class));
		assertString("foo", urlEncoding(urlEncoding("foo"), String.class));
		assertString("foo", yaml(yaml("foo"), String.class));
		// ini/toml/hocon/proto require a Map or bean at root, not a bare String
		assertEquals("b", ini(ini(MAP), JsonMap.class).getString("a"));
		assertEquals("b", toml(toml(MAP), JsonMap.class).getString("a"));
		assertEquals("b", hocon(hocon(MAP), JsonMap.class).getString("a"));
		assertEquals("b", proto(proto(MAP), JsonMap.class).getString("a"));
	}

	@Test void b04_parseBinaryFormats() throws Exception {
		assertString("foo", msgPack(msgPack("foo"), String.class));
		assertString("foo", cbor(cbor("foo"), String.class));
		assertString("foo", bson(bson("foo"), String.class));
	}

	@Test void b05_parseParquet() throws Exception {
		var a = new A04_Bean();
		List<A04_Bean> parsed = parquet(parquet(a), A04_Bean.class);
		assertEquals(1, parsed.size());
		assertString("test", parsed.get(0).x);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Serialize-to-output (Writer/StringBuilder) variants for char-based formats
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_serializeToWriter_charFormats() throws Exception {
		var sb = new StringBuilder();
		json(MAP, sb);
		assertNotNull(sb.toString());
		assertTrue(!sb.isEmpty());

		sb.setLength(0); json5(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); jsonl(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); jcs(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); hjson(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); xml(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); html(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); uon(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); urlEncoding(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); yaml(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); csv("foo", sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); openApi(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); plainText(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); markdown(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); markdownDoc(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); ini(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); toml(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); hocon(MAP, sb); assertTrue(!sb.isEmpty());
		sb.setLength(0); proto(MAP, sb); assertTrue(!sb.isEmpty());
	}

	@Test void c02_serializeToOutputStream_binaryFormats() throws Exception {
		var os = new java.io.ByteArrayOutputStream();
		msgPack(MAP, os);
		assertTrue(os.size() > 0);

		os.reset(); cbor(MAP, os); assertTrue(os.size() > 0);
		os.reset(); bson(MAP, os); assertTrue(os.size() > 0);
		os.reset(); parquet(new A04_Bean(), os); assertTrue(os.size() > 0);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Parameterized-type parsing - (Object, Type, Type...)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_parseObjectParameterizedType_charFormats() throws Exception {
		// json("[1,2,3]") parsed into List<Integer>
		String jsonStr = "[1,2,3]";
		List<Integer> r = json((Object) jsonStr, List.class, Integer.class);
		assertEquals(3, r.size());
		assertEquals(Integer.valueOf(1), r.get(0));

		assertNotNull(json5((Object) "[1,2,3]", List.class, Integer.class));
		assertNotNull(jsonl((Object) "1\n2\n3\n", List.class, Integer.class));
		assertNotNull(jcs((Object) "[1,2,3]", List.class, Integer.class));
		assertNotNull(hjson((Object) "[1,2,3]", List.class, Integer.class));
		assertNotNull(uon((Object) "@(1,2,3)", List.class, Integer.class));
		assertNotNull(yaml((Object) "[1,2,3]", List.class, Integer.class));
	}

	@Test void d02_parseObjectParameterizedType_binaryFormats() throws Exception {
		var bytes = msgPack(java.util.List.of(1, 2, 3));
		List<Integer> r = msgPack((Object) bytes, List.class, Integer.class);
		assertEquals(3, r.size());

		var cborBytes = cbor(java.util.List.of(1, 2, 3));
		assertNotNull(cbor((Object) cborBytes, List.class, Integer.class));

		var bsonBytes = bson(java.util.List.of(1, 2, 3));
		assertNotNull(bson((Object) bsonBytes, List.class, Integer.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// String-based (String, Class) and (String, Type, Type...) safe variants
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_parseStringClass_safeWrappers() {
		// These are safe wrappers - no checked exceptions required.
		assertEquals("foo", MarshallUtils.<String>json("\"foo\"", String.class));
		assertEquals("foo", MarshallUtils.<String>json5("'foo'", String.class));
		assertEquals("foo", MarshallUtils.<String>jcs("\"foo\"", String.class));
		assertEquals("foo", MarshallUtils.<String>hjson("\"foo\"", String.class));
		assertEquals("foo", MarshallUtils.<String>jsonl("\"foo\"", String.class));
	}

	@Test void e02_parseStringClass_xml_html_uon() {
		assertEquals("foo", MarshallUtils.<String>xml("<string>foo</string>", String.class));
		assertEquals("foo", MarshallUtils.<String>html("<string>foo</string>", String.class));
		assertEquals("foo", MarshallUtils.<String>uon("foo", String.class));
		assertEquals("foo", MarshallUtils.<String>urlEncoding("_value=foo", String.class));
		assertEquals("foo", MarshallUtils.<String>yaml("foo", String.class));
		assertEquals("foo", MarshallUtils.<String>plainText("foo", String.class));
		assertEquals("foo", MarshallUtils.<String>openApi("foo", String.class));
	}

	@Test void e03_parseStringClass_csvAndMarkdownVariants() throws Exception {
		// csv/markdown/markdownDoc require structured input — round-trip them.
		var csvText = csv("foo");
		assertNotNull(MarshallUtils.<String>csv(csvText, String.class));

		var mdText = markdown("foo");
		assertNotNull(MarshallUtils.<String>markdown(mdText, String.class));

		var mddocText = markdownDoc("foo");
		assertNotNull(MarshallUtils.<String>markdownDoc(mddocText, String.class));
	}

	@Test void e04_parseStringClass_iniTomlHoconProto() throws Exception {
		// These require Map root.
		var iniStr = ini(new A04_Bean(), new StringBuilder()).toString();
		var iniParsed = MarshallUtils.<JsonMap>ini(iniStr, JsonMap.class);
		assertNotNull(iniParsed);

		var tomlStr = toml(new A04_Bean(), new StringBuilder()).toString();
		var tomlParsed = MarshallUtils.<JsonMap>toml(tomlStr, JsonMap.class);
		assertNotNull(tomlParsed);

		var hoconStr = hocon(new A04_Bean(), new StringBuilder()).toString();
		var hoconParsed = MarshallUtils.<JsonMap>hocon(hoconStr, JsonMap.class);
		assertNotNull(hoconParsed);

		var protoStr = proto(new A04_Bean(), new StringBuilder()).toString();
		var protoParsed = MarshallUtils.<JsonMap>proto(protoStr, JsonMap.class);
		assertNotNull(protoParsed);
	}

	@Test void e05_parseStringParameterizedType() throws Exception {
		List<Integer> r = json("[1,2,3]", List.class, Integer.class);
		assertEquals(3, r.size());

		assertNotNull(json5("[1,2,3]", List.class, Integer.class));
		assertNotNull(jsonl("1\n2\n3\n", List.class, Integer.class));
		assertNotNull(jcs("[1,2,3]", List.class, Integer.class));
		assertNotNull(hjson("[1,2,3]", List.class, Integer.class));
		assertNotNull(xml(xml(java.util.List.of(1, 2, 3)), List.class, Integer.class));
		assertNotNull(html(html(java.util.List.of(1, 2, 3)), List.class, Integer.class));
		assertNotNull(uon("@(1,2,3)", List.class, Integer.class));
		assertNotNull(urlEncoding(urlEncoding(java.util.List.of(1, 2, 3)), List.class, Integer.class));
		assertNotNull(yaml("[1,2,3]", List.class, Integer.class));
		assertNotNull(plainText(plainText(java.util.List.of(1, 2, 3)), List.class, Integer.class));
		assertNotNull(openApi(openApi(java.util.List.of(1, 2, 3)), List.class, Integer.class));
	}

	@Test void e06_parseBinaryParameterizedType_byteArrays() throws Exception {
		var mpBytes = msgPack(java.util.List.of(1, 2, 3));
		List<Integer> mpResult = msgPack(mpBytes, List.class, Integer.class);
		assertEquals(3, mpResult.size());

		var cborBytes = cbor(java.util.List.of(1, 2, 3));
		assertNotNull(cbor(cborBytes, List.class, Integer.class));

		var bsonBytes = bson(java.util.List.of(1, 2, 3));
		assertNotNull(bson(bsonBytes, List.class, Integer.class));
	}

	@Test void e07_parquet_byteArrayTypeOverload() throws Exception {
		var bytes = parquet(new A04_Bean());
		// (byte[], Type) overload returns List<T>
		List<A04_Bean> r1 = parquet(bytes, (java.lang.reflect.Type) A04_Bean.class);
		assertEquals(1, r1.size());

		// (byte[], Type, Type...) overload
		List<A04_Bean> r2 = parquet(bytes, List.class, A04_Bean.class);
		assertEquals(1, r2.size());

		// (byte[], Class) overload
		List<A04_Bean> r3 = parquet(bytes, A04_Bean.class);
		assertEquals(1, r3.size());
	}
}
