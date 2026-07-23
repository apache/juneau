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
package org.apache.juneau.marshall.prototext;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link PrototextParser}.
 */
class PrototextParser_Test {

	@Test
	void a01_readSimpleBean() {
		var a = PrototextParser.DEFAULT.read("name: \"Alice\"\nage: 30\nactive: true", JsonMap.class);
		assertEquals("Alice", a.get("name"));
		assertEquals(30L, a.get("age"));
		assertEquals(true, a.get("active"));
	}

	@Test
	void a02_readNestedBean() {
		var a = PrototextParser.DEFAULT.read("address {\n  city: \"Boston\"\n  state: \"MA\"\n}", JsonMap.class);
		var addr = a.getMap("address");
		assertNotNull(addr);
		assertEquals("Boston", addr.get("city"));
		assertEquals("MA", addr.get("state"));
	}

	@Test
	void a03_readDeeplyNestedBean() {
		var a = PrototextParser.DEFAULT.read("a { b { c: 1 } }", JsonMap.class);
		var b = a.getMap("a");
		assertNotNull(b);
		var c = b.getMap("b");
		assertNotNull(c);
		assertEquals(1L, c.get("c"));
	}

	@Test
	void a04_readListOfStrings() {
		var a = PrototextParser.DEFAULT.read("tags: [\"a\", \"b\", \"c\"]", JsonMap.class);
		var list = a.getList("tags");
		assertNotNull(list);
		assertEquals(List.of("a", "b", "c"), list);
	}

	@Test
	void a05_readListOfIntegers() {
		var a = PrototextParser.DEFAULT.read("ports: [8080, 8443, 9090]", JsonMap.class);
		var list = a.getList("ports");
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEquals(8080L, list.get(0));
		assertEquals(8443L, list.get(1));
		assertEquals(9090L, list.get(2));
	}

	@Test
	void a06_readRepeatedMessages() {
		var a = PrototextParser.DEFAULT.read(
			"servers { host: \"alpha\" port: 8080 }\nservers { host: \"beta\" port: 8081 }",
			JsonMap.class);
		var list = a.getList("servers");
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEquals("alpha", ((Map<?,?>) list.get(0)).get("host"));
		assertEquals("beta", ((Map<?,?>) list.get(1)).get("host"));
	}

	@Test
	void a07_readMapProperty() {
		var a = PrototextParser.DEFAULT.read("env { PATH: \"/usr/bin\" HOME: \"/home\" }", JsonMap.class);
		var env = a.getMap("env");
		assertNotNull(env);
		assertEquals("/usr/bin", env.get("PATH"));
		assertEquals("/home", env.get("HOME"));
	}

	@Test
	void a08_readBooleans() {
		var a = PrototextParser.DEFAULT.read("t: true f: false", JsonMap.class);
		assertEquals(true, a.get("t"));
		assertEquals(false, a.get("f"));
	}

	@Test
	void a09_readIntegers() {
		var a = PrototextParser.DEFAULT.read("d: 42 h: 0xFF o: 0755", JsonMap.class);
		assertEquals(42L, a.get("d"));
		assertEquals(255L, a.get("h"));
		assertEquals(493L, a.get("o"));
	}

	@Test
	void a10_readFloats() {
		var a = PrototextParser.DEFAULT.read("x: 3.14 inf: inf neg: -inf n: nan", JsonMap.class);
		assertEquals(3.14, ((Number) a.get("x")).doubleValue(), 1e-6);
		assertEquals(Double.POSITIVE_INFINITY, a.get("inf"));
		assertEquals(Double.NEGATIVE_INFINITY, a.get("neg"));
		assertTrue(Double.isNaN(((Number) a.get("n")).doubleValue()));
	}

	@Test
	void a11_readStrings() {
		var a = PrototextParser.DEFAULT.read("a: \"hello\" b: 'world'", JsonMap.class);
		assertEquals("hello", a.get("a"));
		assertEquals("world", a.get("b"));
	}

	@Test
	void a12_readMultiPartStrings() {
		var a = PrototextParser.DEFAULT.read("s: \"hello\" \" world\"", JsonMap.class);
		assertEquals("hello world", a.get("s"));
	}

	@Test
	void a13_readComments() {
		var a = PrototextParser.DEFAULT.read("# comment\nname: 1", JsonMap.class);
		assertEquals(1L, a.get("name"));
	}

	@Test
	void a14_readSemicolonSeparators() {
		var a = PrototextParser.DEFAULT.read("a: 1; b: 2", JsonMap.class);
		assertEquals(1L, a.get("a"));
		assertEquals(2L, a.get("b"));
	}

	@Test
	void a15_readCommaSeparators() {
		var a = PrototextParser.DEFAULT.read("a: 1, b: 2", JsonMap.class);
		assertEquals(1L, a.get("a"));
		assertEquals(2L, a.get("b"));
	}

	@Test
	void a16_readMissingFields() {
		var a = PrototextParser.DEFAULT.read("a: 1", JsonMap.class);
		assertEquals(1L, a.get("a"));
		assertNull(a.get("b"));
	}

	@Test
	void a17_readAngleBrackets() {
		var a = PrototextParser.DEFAULT.read("m < k: 1 >", JsonMap.class);
		var m = a.getMap("m");
		assertNotNull(m);
		assertEquals(1L, m.get("k"));
	}

	@Test
	void a18_readColonBeforeMessage() {
		var a = PrototextParser.DEFAULT.read("m: { k: 1 }", JsonMap.class);
		var m = a.getMap("m");
		assertNotNull(m);
		assertEquals(1L, m.get("k"));
	}

	@Test
	void a19_readEnumValues() {
		var a = PrototextParser.DEFAULT.read("level: WARN", JsonMap.class);
		assertEquals("WARN", a.get("level"));
	}

	@Test
	void a20_readQuotedKeys() {
		var a = PrototextParser.DEFAULT.read("\"special.key\": \"value\"; \"key.with.dots\": 42", JsonMap.class);
		assertEquals("value", a.get("special.key"));
		assertEquals(42L, a.get("key.with.dots"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// b — PrototextParser meta-provider methods: null bpm guard
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_getPrototextBeanPropertyMeta_null_returnsDefault() {
		// Line 172: bpm == null branch — must return PrototextBeanPropertyMeta.DEFAULT
		var result = PrototextParser.DEFAULT.getPrototextBeanPropertyMeta(null);
		assertSame(PrototextBeanPropertyMeta.DEFAULT, result);
	}
}
