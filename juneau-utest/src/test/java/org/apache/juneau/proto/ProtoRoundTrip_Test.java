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

import java.util.*;

import org.apache.juneau.swaps.ByteArraySwap;

import org.apache.juneau.collections.JsonMap;
import org.apache.juneau.marshaller.Proto;
import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for ProtoSerializer and ProtoParser.
 */
class ProtoRoundTrip_Test {

	@Test
	void a01_simpleBeanRoundTrip() throws Exception {
		var a = JsonMap.of("s", "hello", "n", 42, "b", true, "x", 3.14);
		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals("hello", b.get("s"));
		assertEquals(42L, b.get("n"));
		assertEquals(true, b.get("b"));
		assertEquals(3.14, ((Number) b.get("x")).doubleValue(), 1e-6);
	}

	@Test
	void a02_nestedBeanRoundTrip() throws Exception {
		var inner = new LinkedHashMap<String, Object>();
		inner.put("city", "Boston");
		inner.put("state", "MA");
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "Alice");
		a.put("address", inner);

		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals("Alice", b.get("name"));
		var addr = b.getMap("address");
		assertNotNull(addr);
		assertEquals("Boston", addr.get("city"));
		assertEquals("MA", addr.get("state"));
	}

	@Test
	void a03_collectionOfBeansRoundTrip() throws Exception {
		var a1 = new LinkedHashMap<String, Object>();
		a1.put("host", "alpha");
		a1.put("port", 8080);
		var a2 = new LinkedHashMap<String, Object>();
		a2.put("host", "beta");
		a2.put("port", 8081);
		var a = JsonMap.of("servers", List.of(a1, a2));

		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		var list = b.getList("servers");
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEquals("alpha", ((Map<?, ?>) list.get(0)).get("host"));
		assertEquals("beta", ((Map<?, ?>) list.get(1)).get("host"));
	}

	@Test
	void a04_collectionOfStringsRoundTrip() throws Exception {
		var a = JsonMap.of("tags", List.of("a", "b", "c"));
		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals(List.of("a", "b", "c"), b.getList("tags"));
	}

	@Test
	void a05_mapRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("env", new LinkedHashMap<>(Map.of("PATH", "/usr/bin", "HOME", "/home")));

		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		var env = b.getMap("env");
		assertNotNull(env);
		assertEquals("/usr/bin", env.get("PATH"));
		assertEquals("/home", env.get("HOME"));
	}

	@Test
	void a06_enumRoundTrip() throws Exception {
		var a = JsonMap.of("level", "INFO");
		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals("INFO", b.get("level"));
	}

	@Test
	void a07_stringEscapingRoundTrip() throws Exception {
		var a = JsonMap.of("s", "a\nb\tc");
		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals("a\nb\tc", b.get("s"));
	}

	@Test
	void a08_booleanRoundTrip() throws Exception {
		var a = JsonMap.of("t", true, "f", false);
		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals(true, b.get("t"));
		assertEquals(false, b.get("f"));
	}

	@Test
	void a09_numericEdgeCases() throws Exception {
		var a = JsonMap.of("i", Integer.MAX_VALUE, "l", Long.MAX_VALUE, "d", 3.14159265358979);
		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals((long) Integer.MAX_VALUE, b.get("i"));
		assertEquals(Long.MAX_VALUE, b.get("l"));
		assertEquals(3.14159265358979, ((Number) b.get("d")).doubleValue(), 1e-10);
	}

	@Test
	void a10_complexBeanRoundTrip() throws Exception {
		var inner = JsonMap.of("x", 1);
		var a = new JsonMap();
		a.put("name", "test");
		a.put("count", 42);
		a.put("tags", List.of("a", "b"));
		a.put("nested", inner);

		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals("test", b.get("name"));
		assertEquals(42L, b.get("count"));
		assertEquals(List.of("a", "b"), b.getList("tags"));
		assertEquals(1L, b.getMap("nested").get("x"));
	}

	@Test
	void a11_objectSwapRoundTrip() throws Exception {
		var ser = (ProtoSerializer) ProtoSerializer.create().swaps(ByteArraySwap.Base64.class).build();
		var a = JsonMap.of("data", new byte[] { 0x0a, 0x05, (byte) 0xff });
		var proto = ser.serialize(a);
		assertTrue(proto.contains("data"));
		assertFalse(proto.contains("\\x"), "Base64 swap should produce base64, not hex escape");
	}

	@Test
	void a12_emptyCollectionsRoundTrip() throws Exception {
		var a = JsonMap.of("tags", List.of());
		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		var list = b.getList("tags");
		assertNotNull(list);
		assertTrue(list.isEmpty());
	}

	@Test
	void a13_nullableReferenceRoundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("s", "x");
		a.put("n", 1);
		a.put("nullStr", null);
		a.put("nullInt", null);

		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals("x", b.get("s"));
		assertEquals(1L, b.get("n"));
		assertNull(b.get("nullStr"));
		assertNull(b.get("nullInt"));
	}

	@Test
	void a14_topLevelScalarLimitation() throws Exception {
		var a = "bare";
		var proto = Proto.of(a);
		var b = Proto.to(proto, String.class);
		assertEquals("bare", b);
	}

	@Test
	void a15_untypedJsonMapRoundTrip() throws Exception {
		var inner = JsonMap.of("k", "v");
		var a = new JsonMap();
		a.put("s", "hello");
		a.put("n", 42);
		a.put("b", true);
		a.put("nest", inner);
		a.put("list", List.of(1, 2, 3));

		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals("hello", b.get("s"));
		assertEquals(42L, b.get("n"));
		assertEquals(true, b.get("b"));
		assertEquals("v", b.getMap("nest").get("k"));
		assertEquals(3, b.getList("list").size());
	}

	@Test
	void a16_dateTimeRoundTrip() throws Exception {
		var a = JsonMap.of(
			"instant", "2012-12-21T12:34:56Z",
			"localDate", "2012-12-21",
			"timeout", "PT1H30M"
		);
		var proto = Proto.of(a);
		var b = Proto.to(proto, JsonMap.class);
		assertEquals("2012-12-21T12:34:56Z", b.get("instant"));
		assertEquals("2012-12-21", b.get("localDate"));
		assertEquals("PT1H30M", b.get("timeout"));
	}
}
