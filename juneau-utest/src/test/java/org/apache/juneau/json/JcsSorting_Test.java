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
package org.apache.juneau.json;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for JCS property/key sorting per RFC 8785 (UTF-16 code unit order).
 */
class JcsSorting_Test extends TestBase {

	@Test
	void c01_alphabeticalOrder() throws Exception {
		var m = JsonMap.of("a", 1, "b", 2, "c", 3);
		assertEquals("{\"a\":1,\"b\":2,\"c\":3}", JcsSerializer.DEFAULT.serialize(m));
	}

	@Test
	void c02_reverseInputOrder() throws Exception {
		var m = JsonMap.of("c", 3, "b", 2, "a", 1);
		assertEquals("{\"a\":1,\"b\":2,\"c\":3}", JcsSerializer.DEFAULT.serialize(m));
	}

	@Test
	void c03_nestedObjectsSorted() throws Exception {
		var inner = JsonMap.of("z", 1, "y", 2, "x", 3);
		var outer = JsonMap.of("b", inner, "a", "top");
		// Outer keys sorted: a, b. Inner keys sorted: x, y, z.
		assertEquals("{\"a\":\"top\",\"b\":{\"x\":3,\"y\":2,\"z\":1}}", JcsSerializer.DEFAULT.serialize(outer));
	}

	@Test
	void c04_arrayOrderPreserved() throws Exception {
		var m = JsonMap.of("arr", list(3, 1, 2));
		// Array order preserved; object keys sorted
		assertEquals("{\"arr\":[3,1,2]}", JcsSerializer.DEFAULT.serialize(m));
	}

	@Test
	void c05_utf16SortOrder() throws Exception {
		// RFC 8785: keys sorted by UTF-16 code unit order
		var m = new LinkedHashMap<String, Integer>();
		m.put(Character.toString((char) 0x00F6), 1);   // ö
		m.put("1", 2);
		m.put("\r", 3);  // carriage return
		m.put(Character.toString((char) 0x20AC), 4);   // €
		m.put(Character.toString((char) 0x80), 5);    // C1 control
		m.put(Character.toString((char) 0xFB33), 6);  // Hebrew letter
		var s = JcsSerializer.DEFAULT.serialize(m);
		// Parse and verify key order: \r < 1 < U+0080 < ö < € < Hebrew
		var parsed = JsonParser.DEFAULT.parse(s, JsonMap.class);
		var keys = parsed.keySet().stream().toList();
		assertEquals(6, keys.size());
		assertEquals("\r", keys.get(0));
		assertEquals("1", keys.get(1));
		assertEquals(Character.toString((char) 0x80), keys.get(2));
		assertEquals(Character.toString((char) 0x00F6), keys.get(3));
		assertEquals(Character.toString((char) 0x20AC), keys.get(4));
		assertEquals(Character.toString((char) 0xFB33), keys.get(5));
	}

	@Test
	void c06_mapKeysSorted() throws Exception {
		var m = new HashMap<String, Object>();
		m.put("z", 1);
		m.put("a", 2);
		m.put("m", 3);
		assertEquals("{\"a\":2,\"m\":3,\"z\":1}", JcsSerializer.DEFAULT.serialize(m));
	}

	@Test
	void c07_emptyObject() throws Exception {
		assertEquals("{}", JcsSerializer.DEFAULT.serialize(JsonMap.of()));
	}

	@Test
	void c08_singleProperty() throws Exception {
		var m = JsonMap.of("a", 1);
		assertEquals("{\"a\":1}", JcsSerializer.DEFAULT.serialize(m));
	}

	@Test
	void c09_numericStringKeys() throws Exception {
		// Lexicographic (UTF-16), not numeric: "1" < "10" < "2"
		var m = JsonMap.of("10", 1, "2", 2, "1", 3);
		assertEquals("{\"1\":3,\"10\":1,\"2\":2}", JcsSerializer.DEFAULT.serialize(m));
	}

	@Test
	void c10_caseSensitive() throws Exception {
		// UTF-16: "A" (U+0041) < "a" (U+0061)
		var m = JsonMap.of("a", 1, "A", 2, "B", 3, "b", 4);
		assertEquals("{\"A\":2,\"B\":3,\"a\":1,\"b\":4}", JcsSerializer.DEFAULT.serialize(m));
	}
}
