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
package org.apache.juneau.proto;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.parser.ParseException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProtoTokenizer} behavior, exercised through {@link ProtoParser}.
 */
class ProtoTokenizer_Test {

	@Test
	void a01_bareIdentifier() throws Exception {
		var a = ProtoParser.DEFAULT.parse("field_name: 1", JsonMap.class);
		assertEquals(1L, a.get("field_name"));

		var b = ProtoParser.DEFAULT.parse("_private: 2", JsonMap.class);
		assertEquals(2L, b.get("_private"));

		var c = ProtoParser.DEFAULT.parse("camelCase: 3", JsonMap.class);
		assertEquals(3L, c.get("camelCase"));
	}

	@Test
	void a02_quotedString() throws Exception {
		var a = ProtoParser.DEFAULT.parse("s: \"hello\"", JsonMap.class);
		assertEquals("hello", a.get("s"));

		var b = ProtoParser.DEFAULT.parse("s: 'world'", JsonMap.class);
		assertEquals("world", b.get("s"));
	}

	@Test
	void a03_multiPartString() throws Exception {
		var a = ProtoParser.DEFAULT.parse("s: \"hello\" \" world\"", JsonMap.class);
		assertEquals("hello world", a.get("s"));
	}

	@Test
	void a04_decimalInteger() throws Exception {
		var a = ProtoParser.DEFAULT.parse("n: 42", JsonMap.class);
		assertEquals(42L, a.get("n"));

		var b = ProtoParser.DEFAULT.parse("n: -17", JsonMap.class);
		assertEquals(-17L, b.get("n"));

		var c = ProtoParser.DEFAULT.parse("n: +99", JsonMap.class);
		assertEquals(99L, c.get("n"));
	}

	@Test
	void a05_hexInteger() throws Exception {
		var a = ProtoParser.DEFAULT.parse("n: 0xDEAD", JsonMap.class);
		assertEquals(0xDEADL, a.get("n"));

		var b = ProtoParser.DEFAULT.parse("n: 0xFF", JsonMap.class);
		assertEquals(255L, b.get("n"));
	}

	@Test
	void a06_octalInteger() throws Exception {
		var a = ProtoParser.DEFAULT.parse("n: 0755", JsonMap.class);
		assertEquals(0755L, a.get("n"));

		var b = ProtoParser.DEFAULT.parse("n: 0123", JsonMap.class);
		assertEquals(83L, b.get("n"));
	}

	@Test
	void a07_floatLiteral() throws Exception {
		var a = ProtoParser.DEFAULT.parse("x: 3.14", JsonMap.class);
		assertEquals(3.14, a.get("x"));

		var b = ProtoParser.DEFAULT.parse("x: 1e06", JsonMap.class);
		assertEquals(1e6, ((Number) b.get("x")).doubleValue(), 1e-6);

		var c = ProtoParser.DEFAULT.parse("x: 10f", JsonMap.class);
		assertEquals(10.0, ((Number) c.get("x")).doubleValue(), 1e-6);
	}

	@Test
	void a08_specialFloats() throws Exception {
		var a = ProtoParser.DEFAULT.parse("x: inf", JsonMap.class);
		assertEquals(Double.POSITIVE_INFINITY, a.get("x"));

		var b = ProtoParser.DEFAULT.parse("x: -inf", JsonMap.class);
		assertEquals(Double.NEGATIVE_INFINITY, b.get("x"));

		var c = ProtoParser.DEFAULT.parse("x: nan", JsonMap.class);
		assertTrue(Double.isNaN(((Number) c.get("x")).doubleValue()));
	}

	@Test
	void a09_booleans() throws Exception {
		var a = ProtoParser.DEFAULT.parse("b: true", JsonMap.class);
		assertEquals(true, a.get("b"));

		var b = ProtoParser.DEFAULT.parse("b: false", JsonMap.class);
		assertEquals(false, b.get("b"));
	}

	@Test
	void a10_comments() throws Exception {
		var a = ProtoParser.DEFAULT.parse("# comment\nname: 1", JsonMap.class);
		assertEquals(1L, a.get("name"));

		var b = ProtoParser.DEFAULT.parse("name: 1  # trailing comment", JsonMap.class);
		assertEquals(1L, b.get("name"));
	}

	@Test
	void a11_structuralTokens() throws Exception {
		var a = ProtoParser.DEFAULT.parse("m { k: 1 }", JsonMap.class);
		var m = a.getMap("m");
		assertNotNull(m);
		assertEquals(1L, m.get("k"));

		var b = ProtoParser.DEFAULT.parse("arr: [1, 2]", JsonMap.class);
		var arr = b.getList("arr");
		assertNotNull(arr);
		assertEquals(2, arr.size());

		var c = ProtoParser.DEFAULT.parse("a: 1; b: 2", JsonMap.class);
		assertEquals(1L, c.get("a"));
		assertEquals(2L, c.get("b"));

		var d = ProtoParser.DEFAULT.parse("a: 1, b: 2", JsonMap.class);
		assertEquals(1L, d.get("a"));
		assertEquals(2L, d.get("b"));
	}

	@Test
	void a12_stringEscapes() throws Exception {
		var a = ProtoParser.DEFAULT.parse("s: \"\\n\"", JsonMap.class);
		assertEquals("\n", a.get("s"));

		var b = ProtoParser.DEFAULT.parse("s: \"\\t\"", JsonMap.class);
		assertEquals("\t", b.get("s"));

		var c = ProtoParser.DEFAULT.parse("s: \"\\\\\"", JsonMap.class);
		assertEquals("\\", c.get("s"));

		var d = ProtoParser.DEFAULT.parse("s: \"\\\"\"", JsonMap.class);
		assertEquals("\"", d.get("s"));
	}

	@Test
	void a13_edgeCaseNumberIdent() {
		assertThrows(ParseException.class, () ->
			ProtoParser.DEFAULT.parse("n: 10bar", JsonMap.class));
	}

	@Test
	void a14_whitespaceHandling() throws Exception {
		var a = ProtoParser.DEFAULT.parse("  name  :  42  ", JsonMap.class);
		assertEquals(42L, a.get("name"));

		var b = ProtoParser.DEFAULT.parse("a: 1\nb: 2", JsonMap.class);
		assertEquals(1L, b.get("a"));
		assertEquals(2L, b.get("b"));

		var c = ProtoParser.DEFAULT.parse("a: 1\r\nb: 2", JsonMap.class);
		assertEquals(1L, c.get("a"));
		assertEquals(2L, c.get("b"));
	}
}
