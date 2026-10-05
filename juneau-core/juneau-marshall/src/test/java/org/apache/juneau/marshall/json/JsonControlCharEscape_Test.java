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
package org.apache.juneau.marshall.json;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

/**
 * Tests JSON string escaping of carriage returns and control characters (RFC 8259).
 */
class JsonControlCharEscape_Test extends TestBase {

	private static String expectedEscape(int c) {
		return switch (c) {
			case '\b' -> "\\b";
			case '\t' -> "\\t";
			case '\n' -> "\\n";
			case '\f' -> "\\f";
			case '\r' -> "\\r";
			default -> String.format("\\u%04x", c);
		};
	}

	// NOTE: ParserReader uses DEL (0x7f) as its internal escape-hole filler, so the parser cannot round-trip a DEL
	// character (pre-existing limitation; a raw DEL in input is also dropped).  DEL round-trip is therefore not asserted.
	private static boolean roundTrips(int c) {
		return c != 0x7f;
	}

	private static List<Integer> controlChars() {
		var l = new ArrayList<Integer>();
		for (var c = 0; c <= 0x1f; c++)
			l.add(c);
		l.add(0x7f);
		return l;
	}

	@Test void a01_valueEscapes_json() throws Exception {
		for (var c : controlChars()) {
			var s = "a" + (char)(int)c + "b";
			var out = JsonSerializer.DEFAULT.write(s);
			assertEquals("\"a" + expectedEscape(c) + "b\"", out, "char 0x" + Integer.toHexString(c));
			if (roundTrips(c))
				assertEquals(s, JsonParser.DEFAULT.read(out, String.class), "roundtrip 0x" + Integer.toHexString(c));
		}
	}

	@Test void a02_valueEscapes_json5() throws Exception {
		for (var c : controlChars()) {
			var s = "a" + (char)(int)c + "b";
			var out = Json5Serializer.DEFAULT.write(s);
			assertEquals("'a" + expectedEscape(c) + "b'", out, "char 0x" + Integer.toHexString(c));
			if (roundTrips(c))
				assertEquals(s, Json5Parser.DEFAULT.read(out, String.class), "roundtrip 0x" + Integer.toHexString(c));
		}
	}

	@Test void a03_mapKeys_json() throws Exception {
		for (var c : controlChars()) {
			var k = "k" + (char)(int)c;
			var m = new LinkedHashMap<String,Object>();
			m.put(k, 1);
			var out = JsonSerializer.DEFAULT.write(m);
			assertEquals("{\"k" + expectedEscape(c) + "\":1}", out, "char 0x" + Integer.toHexString(c));
			var m2 = JsonParser.DEFAULT.read(out, Map.class);
			assertEquals(1, m2.size());
			if (roundTrips(c))
				assertTrue(m2.containsKey(k), "roundtrip key 0x" + Integer.toHexString(c));
		}
	}

	@Test void a04_mapKeys_json5() throws Exception {
		for (var c : controlChars()) {
			var k = "k" + (char)(int)c;
			var m = new LinkedHashMap<String,Object>();
			m.put(k, 1);
			var out = Json5Serializer.DEFAULT.write(m);
			assertEquals("{'k" + expectedEscape(c) + "':1}", out, "char 0x" + Integer.toHexString(c));
			var m2 = Json5Parser.DEFAULT.read(out, Map.class);
			assertEquals(1, m2.size());
			if (roundTrips(c))
				assertTrue(m2.containsKey(k), "roundtrip key 0x" + Integer.toHexString(c));
		}
	}

	@Test void a05_crlf() throws Exception {
		var s = "line1\r\nline2\rline3\n";
		var m = new LinkedHashMap<String,Object>();
		m.put("a\r\nb", s);
		var json = "{\"a\\r\\nb\":\"line1\\r\\nline2\\rline3\\n\"}";
		assertEquals(json, JsonSerializer.DEFAULT.write(m));
		assertEquals(s, JsonParser.DEFAULT.read(json, Map.class).get("a\r\nb"));
		assertEquals("{'a\\r\\nb':'line1\\r\\nline2\\rline3\\n'}", Json5Serializer.DEFAULT.write(m));
		assertEquals(s, Json5Parser.DEFAULT.read(Json5Serializer.DEFAULT.write(m), Map.class).get("a\r\nb"));
	}

	@Test void a06_escapeSolidusStillWorks() throws Exception {
		var ser = JsonSerializer.create().escapeSolidus().build();
		assertEquals("\"a\\/\\r\\u0001\"", ser.write("a/\r\u0001"));
		assertEquals("\"a/\\r\\u0001\"", JsonSerializer.DEFAULT.write("a/\r\u0001"));
	}

	@Test void a07_noEscapeNeeded() throws Exception {
		assertEquals("\"plain text é€\"", JsonSerializer.DEFAULT.write("plain text é€"));
	}
}
