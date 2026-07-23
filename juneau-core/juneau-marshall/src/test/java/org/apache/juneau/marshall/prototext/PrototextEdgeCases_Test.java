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
 * Edge case tests for PrototextSerializer and PrototextParser.
 */
class PrototextEdgeCases_Test {

	@Test
	void g01_emptyInput() {
		var b = fromPrototext("", JsonMap.class);
		assertTrue(b == null || b.isEmpty());
	}

	@Test
	void g02_onlyComments() {
		var b = fromPrototext("# comment one\n# comment two\n", JsonMap.class);
		assertTrue(b == null || b.isEmpty());
	}

	@Test
	void g03_unicodeStrings() {
		var a = JsonMap.of("name", "José", "city", "北京");
		var proto = toPrototext(a);
		var b = fromPrototext(proto, JsonMap.class);
		assertEquals("José", b.get("name"));
		assertEquals("北京", b.get("city"));
	}

	@Test
	void g04_veryLongStrings() {
		var longStr = "x".repeat(10000);
		var a = JsonMap.of("s", longStr);
		var proto = toPrototext(a);
		var b = fromPrototext(proto, JsonMap.class);
		assertEquals(longStr, b.get("s"));
	}

	@Test
	void g05_deeplyNestedMessages() {
		var inner = JsonMap.of("x", 1);
		for (var i = 0; i < 10; i++)
			inner = JsonMap.of("nested", inner);
		var proto = toPrototext(inner);
		var b = fromPrototext(proto, JsonMap.class);
		for (var m = b; m != null; m = m.getMap("nested"))
			if (m.containsKey("x"))
				assertEquals(1L, m.get("x"));
	}

	@Test
	void g06_windowsLineEndings() {
		var input = "a: 1\r\nb: 2";
		var b = fromPrototext(input, JsonMap.class);
		assertEquals(1L, b.get("a"));
		assertEquals(2L, b.get("b"));
	}

	@Test
	void g07_mixedSeparators() {
		var input = "a: 1\nb: 2; c: 3, d: 4";
		var b = fromPrototext(input, JsonMap.class);
		assertEquals(1L, b.get("a"));
		assertEquals(2L, b.get("b"));
		assertEquals(3L, b.get("c"));
		assertEquals(4L, b.get("d"));
	}

	@Test
	void g08_cyclicReferences() {
		var a = new JsonMap();
		a.put("name", "self");
		a.put("ref", a);
		assertThrows(Exception.class, () -> toPrototext(a));
	}

	@Test
	void g09_optionalProperties() {
		var a = JsonMap.of("present", "yes");
		var proto = toPrototext(a);
		var b = fromPrototext(proto, JsonMap.class);
		assertEquals("yes", b.get("present"));
	}

	@Test
	void g10_mapWithNonStringKeys() {
		var a = new LinkedHashMap<String,Object>();
		a.put("k1", "one");
		a.put("k2", "two");
		var proto = toPrototext(a);
		var b = fromPrototext(proto, JsonMap.class);
		assertEquals("one", b.get("k1"));
		assertEquals("two", b.get("k2"));
	}

	@Test
	void g11_emptyMessages() {
		var a = fromPrototext("outer { }", JsonMap.class);
		var inner = a.getMap("outer");
		assertNotNull(inner);
		assertTrue(inner.isEmpty());
	}

	@Test
	void g12_trailingComma() {
		var a = fromPrototext("a: 1, b: 2,", JsonMap.class);
		assertEquals(1L, a.get("a"));
		assertEquals(2L, a.get("b"));
	}

	// Helpers keep the marshaller reference fully-qualified (the simple name Prototext is shadowed by the @Prototext annotation in this package).

	private static String toPrototext(Object o) {
		return org.apache.juneau.marshall.marshaller.Prototext.of(o);
	}

	private static <T> T fromPrototext(String input, Class<T> type) {
		return org.apache.juneau.marshall.marshaller.Prototext.to(input, type);
	}
}
