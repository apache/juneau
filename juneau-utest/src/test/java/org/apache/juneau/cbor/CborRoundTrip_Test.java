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
package org.apache.juneau.cbor;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for CBOR serialization.
 */
class CborRoundTrip_Test extends TestBase {

	@Test
	void e01_simpleBeanRoundTrip() throws Exception {
		var a = new CborSerializer_Test.Bean1("x", 42, true);
		var bytes = CborSerializer.DEFAULT.serialize(a);
		var b = CborParser.DEFAULT.parse(bytes, CborSerializer_Test.Bean1.class);
		assertEquals(a.s, b.s);
		assertEquals(a.i, b.i);
		assertEquals(a.b, b.b);
	}

	@Test
	void e02_nestedBeanRoundTrip() throws Exception {
		var a = new CborSerializer_Test.Bean2("outer", new CborSerializer_Test.Bean1("inner", 1, false));
		var bytes = CborSerializer.DEFAULT.serialize(a);
		var b = CborParser.DEFAULT.parse(bytes, CborSerializer_Test.Bean2.class);
		assertEquals(a.name, b.name);
		assertEquals(a.child.s, b.child.s);
	}

	@Test
	void e03_collectionOfBeansRoundTrip() throws Exception {
		var list = list(new CborSerializer_Test.Bean1("a", 1, true), new CborSerializer_Test.Bean1("b", 2, false));
		var bytes = CborSerializer.DEFAULT.serialize(list);
		var parsed = CborParser.DEFAULT.parse(bytes, JsonList.class);
		assertEquals(2, parsed.size());
		assertEquals("a", parsed.getMap(0).getString("s"));
		assertEquals("b", parsed.getMap(1).getString("s"));
	}

	@Test
	void e04_mapRoundTrip() throws Exception {
		var m = JsonMap.of("a", 1, "b", "x", "c", list(1, 2, 3));
		var bytes = CborSerializer.DEFAULT.serialize(m);
		var parsed = CborParser.DEFAULT.parse(bytes, JsonMap.class);
		assertEquals(1, parsed.getInt("a"));
		assertEquals("x", parsed.getString("b"));
		assertEquals(3, parsed.getList("c").size());
	}

	@Test
	void e05_allPrimitiveTypesRoundTrip() throws Exception {
		var m = JsonMap.of("i", 1, "l", 2L, "f", 3.0f, "d", 4.0, "b", true, "s", "x");
		var bytes = CborSerializer.DEFAULT.serialize(m);
		var p = CborParser.DEFAULT.parse(bytes, JsonMap.class);
		assertEquals(1, p.getInt("i"));
		assertEquals(2L, p.getLong("l"));
		assertEquals(3.0f, ((Number)p.get("f")).floatValue(), 0.001f);
		assertEquals(4.0, ((Number)p.get("d")).doubleValue(), 0.001);
		assertTrue(p.getBoolean("b"));
		assertEquals("x", p.getString("s"));
	}

	@Test
	void e06_nullRoundTrip() throws Exception {
		var bytes = CborSerializer.DEFAULT.serialize(null);
		var parsed = CborParser.DEFAULT.parse(bytes, Object.class);
		assertNull(parsed);
	}

	@Test
	void e07_binaryDataRoundTrip() throws Exception {
		var data = new byte[] { 1, 2, 3, 4, 5 };
		var bytes = CborSerializer.DEFAULT.serialize(data);
		assertArrayEquals(data, CborParser.DEFAULT.parse(bytes, byte[].class));
	}

	@Test
	void e08_stringEdgeCasesRoundTrip() throws Exception {
		assertEquals("", roundTrip("", String.class));
		assertEquals("\u20AC", roundTrip("\u20AC", String.class));
	}

	@Test
	void e09_integerBoundariesRoundTrip() throws Exception {
		assertEquals(23, roundTrip(23, Integer.class));
		assertEquals(24, roundTrip(24, Integer.class));
		assertEquals(255, roundTrip(255, Integer.class));
		assertEquals(256, roundTrip(256, Integer.class));
	}

	@Test
	void e10_enumRoundTrip() throws Exception {
		assertEquals(CborSerializer_Test.Size.LARGE, roundTrip(CborSerializer_Test.Size.LARGE, CborSerializer_Test.Size.class));
	}

	@Test
	void e12_complexBeanRoundTrip() throws Exception {
		var m = JsonMap.of(
			"name", "test",
			"tags", list("a", "b"),
			"meta", JsonMap.of("x", 1, "y", 2));
		var bytes = CborSerializer.DEFAULT.serialize(m);
		var p = CborParser.DEFAULT.parse(bytes, JsonMap.class);
		assertEquals("test", p.getString("name"));
		assertEquals(2, p.getList("tags").size());
		assertEquals(1, p.getMap("meta").getInt("x"));
	}

	@Test
	void e13_emptyCollectionsRoundTrip() throws Exception {
		var emptyList = CborParser.DEFAULT.parse(CborSerializer.DEFAULT.serialize(list()), JsonList.class);
		assertTrue(emptyList.isEmpty());
		var emptyMap = CborParser.DEFAULT.parse(CborSerializer.DEFAULT.serialize(JsonMap.ofJson("{}")), JsonMap.class);
		assertTrue(emptyMap.isEmpty());
	}

	private static <T> T roundTrip(T o, Class<T> type) throws Exception {
		var bytes = CborSerializer.DEFAULT.serialize(o);
		return CborParser.DEFAULT.parse(bytes, type);
	}
}
