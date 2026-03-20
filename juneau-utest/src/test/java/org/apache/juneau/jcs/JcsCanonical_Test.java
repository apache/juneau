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
package org.apache.juneau.jcs;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.*;
import java.security.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

class JcsCanonical_Test extends TestBase {

	@Test
	void d01_rfcExample1() throws Exception {
		// RFC 8785 Section 3.2.2: numbers, string, literals
		var numbers = list(333333333.33333329, 1E30, 4.50, 2e-3, 0.000000000000000000000000001);
		var s = Character.toString((char) 0x20AC) + "$" + Character.toString((char) 0x000F) + "\nA'B\"\\\\\"/";
		var literals = list((Object) null, true, false);
		var m = JsonMap.of("numbers", numbers, "string", s, "literals", literals);
		var out = JcsSerializer.DEFAULT.serialize(m);
		// Keys sorted: literals, numbers, string
		assertTrue(out.startsWith("{\"literals\":"));
		assertTrue(out.contains("\"numbers\":"));
		assertTrue(out.contains("\"string\":"));
		assertFalse(out.contains(" "));
	}

	@Test
	void d02_rfcExample2Sorted() throws Exception {
		// Sorted version per RFC 3.2.3
		var numbers = list(333333333.33333329, 1E30, 4.50, 2e-3, 0.000000000000000000000000001);
		var literals = list((Object) null, true, false);
		var m = JsonMap.of("literals", literals, "numbers", numbers);
		var out = JcsSerializer.DEFAULT.serialize(m);
		assertTrue(out.startsWith("{\"literals\":[null,true,false],\"numbers\":"));
	}

	@Test
	void d03_noWhitespace() throws Exception {
		var m = JsonMap.of("a", 1, "b", 2);
		var s = JcsSerializer.DEFAULT.serialize(m);
		assertFalse(s.contains(" "));
		assertFalse(s.contains("\n"));
		assertFalse(s.contains("\t"));
	}

	@Test
	void d04_simpleBeanCanonical() throws Exception {
		var m = JsonMap.of("name", "Alice", "age", 30);
		var s = JcsSerializer.DEFAULT.serialize(m);
		assertEquals("{\"age\":30,\"name\":\"Alice\"}", s);
	}

	@Test
	void d05_nestedBeanCanonical() throws Exception {
		var inner = JsonMap.of("zip", "80201", "city", "Denver");
		var outer = JsonMap.of("address", inner, "name", "Alice");
		var s = JcsSerializer.DEFAULT.serialize(outer);
		assertTrue(s.contains("\"address\":{\"city\":\"Denver\",\"zip\":\"80201\"}"));
		assertTrue(s.contains("\"name\":\"Alice\""));
	}

	@Test
	void d06_mixedTypesCanonical() throws Exception {
		var m = JsonMap.of("n", 42, "s", "hi", "b", true, "x", (Object) null, "a", list(1, 2));
		var s = JcsSerializer.DEFAULT.serialize(m);
		assertFalse(s.contains(" "));
		// All keys present, sorted
		assertTrue(s.contains("\"a\":"));
		assertTrue(s.contains("\"b\":true"));
		assertTrue(s.contains("\"n\":42"));
		assertTrue(s.contains("\"s\":\"hi\""));
		assertTrue(s.contains("\"x\":null"));
	}

	@Test
	void d07_deterministicRoundTrip() throws Exception {
		var m = JsonMap.of("z", 3, "a", 1, "m", 2);
		var s1 = JcsSerializer.DEFAULT.serialize(m);
		var s2 = JcsSerializer.DEFAULT.serialize(m);
		assertEquals(s1, s2);
	}

	@Test
	void d08_hashStability() throws Exception {
		var m = JsonMap.of("c", 3, "a", 1, "b", 2);
		var s1 = JcsSerializer.DEFAULT.serialize(m);
		var s2 = JcsSerializer.DEFAULT.serialize(m);
		var md = MessageDigest.getInstance("SHA-256");
		var bytes1 = s1.getBytes(StandardCharsets.UTF_8);
		var bytes2 = s2.getBytes(StandardCharsets.UTF_8);
		assertArrayEquals(bytes1, bytes2);
		assertEquals(bytesToHex(md.digest(bytes1)), bytesToHex(md.digest(bytes2)));
	}

	@Test
	void d09_emptyObject() throws Exception {
		assertEquals("{}", JcsSerializer.DEFAULT.serialize(JsonMap.of()));
	}

	@Test
	void d10_alphabeticalOrder() throws Exception {
		var m = JsonMap.of("c", 3, "a", 1, "b", 2);
		assertEquals("{\"a\":1,\"b\":2,\"c\":3}", JcsSerializer.DEFAULT.serialize(m));
	}

	private static String bytesToHex(byte[] bytes) {
		var sb = new StringBuilder();
		for (var b : bytes)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}
}
