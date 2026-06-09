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
package org.apache.juneau.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

class JsonMap_Test extends TestBase {

	@Test void a01_create() {
		var m = JsonMap.create();
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}

	@Test void a02_ofKeyValuePairs() {
		var m = JsonMap.of("a", 1, "b", "two");
		assertEquals(2, m.size());
		assertEquals(1, m.getInt("a"));
		assertEquals("two", m.getString("b"));
	}

	@Test void a03_ofMap() {
		var src = new LinkedHashMap<String,Object>();
		src.put("x", 10);
		var m = JsonMap.of(src);
		assertEquals(10, m.getInt("x"));
	}

	@Test void a04_ofMapNull() {
		assertNull(JsonMap.of((Map<?,?>) null));
	}

	@Test void a05_ofStringCharSequence() throws Exception {
		var m = JsonMap.ofString("{\"a\":1,\"b\":\"two\"}");
		assertEquals(1, m.getInt("a"));
		assertEquals("two", m.getString("b"));
	}

	@Test void a06_ofStringCharSequenceNull() throws Exception {
		var m = JsonMap.ofString((CharSequence) null);
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}

	@Test void a07_ofStringReader() throws Exception {
		var m = JsonMap.ofString(new StringReader("{\"x\":99}"));
		assertEquals(99, m.getInt("x"));
	}

	@Test void a08_ofStringReaderNull() throws Exception {
		var m = JsonMap.ofString((Reader) null);
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}

	@Test void a09_ofStringWithParser() throws Exception {
		var m = JsonMap.ofString("{\"k\":\"v\"}", JsonParser.DEFAULT);
		assertEquals("v", m.getString("k"));
	}

	@Test void a10_toStringProducesJson() {
		var m = JsonMap.of("a", 1);
		assertEquals("{\"a\":1}", m.toString());
	}

	@Test void a11_toJson() {
		var m = JsonMap.of("a", 1);
		assertEquals("{\"a\":1}", m.toJson());
	}

	@Test void a12_toJson5() {
		var m = JsonMap.of("a", 1);
		assertEquals("{a:1}", m.toJson5());
	}

	@Test void a13_appendFluent() {
		var m = JsonMap.create().append("a", 1).append("b", 2);
		assertEquals(2, m.size());
		assertEquals(1, m.getInt("a"));
		assertEquals(2, m.getInt("b"));
	}

	@Test void a14_appendIfTrue() {
		var m = JsonMap.create().appendIf(true, "a", 1).appendIf(false, "b", 2);
		assertEquals(1, m.size());
		assertTrue(m.containsKey("a"));
		assertFalse(m.containsKey("b"));
	}

	@Test void a15_appendIfAbsent() {
		var m = JsonMap.of("a", 1).appendIfAbsent("a", 99).appendIfAbsent("b", 2);
		assertEquals(1, m.getInt("a"));
		assertEquals(2, m.getInt("b"));
	}

	@Test void a16_typedAccessors() {
		var m = JsonMap.of("i", "42", "b", "true", "l", "9999999999", "s", 123);
		assertEquals(42, m.getInt("i"));
		assertTrue(m.getBoolean("b"));
		assertEquals(9999999999L, m.getLong("l"));
		assertEquals("123", m.getString("s"));
	}

	@Test void a17_getIntWithDefault() {
		var m = JsonMap.of("a", 5);
		assertEquals(5, m.getInt("a", 0));
		assertEquals(0, m.getInt("missing", 0));
	}

	@Test void a18_getBooleanWithDefault() {
		var m = JsonMap.of("a", true);
		assertTrue(m.getBoolean("a", false));
		assertFalse(m.getBoolean("missing", false));
	}

	@Test void a19_getLongWithDefault() {
		var m = JsonMap.of("a", 100L);
		assertEquals(100L, m.getLong("a", 0L));
		assertEquals(0L, m.getLong("missing", 0L));
	}

	@Test void a20_getMapNested() {
		var inner = JsonMap.of("x", 1);
		var m = JsonMap.of("nested", inner);
		var got = m.getMap("nested");
		assertTrue(got instanceof JsonMap);
		assertEquals(1, got.getInt("x"));
	}

	@Test void a21_getMapCreateIfNotExists() {
		var m = JsonMap.create();
		var nested = m.getMap("nested", true);
		assertNotNull(nested);
		assertTrue(nested instanceof JsonMap);
		assertSame(nested, m.getMap("nested"));
	}

	@Test void a22_getListNested() {
		var inner = JsonList.of(1, 2, 3);
		var m = JsonMap.of("arr", inner);
		var got = m.getList("arr");
		assertTrue(got instanceof JsonList);
		assertEquals(3, got.size());
	}

	@Test void a23_getListCreateIfNotExists() {
		var m = JsonMap.create();
		var list = m.getList("items", true);
		assertNotNull(list);
		assertTrue(list instanceof JsonList);
		assertSame(list, m.getList("items"));
	}

	@Test void a24_exclude() {
		var m = JsonMap.of("a", 1, "b", 2, "c", 3);
		var excluded = m.exclude("b");
		assertEquals(2, excluded.size());
		assertTrue(excluded instanceof JsonMap);
		assertFalse(excluded.containsKey("b"));
	}

	@Test void a25_include() {
		var m = JsonMap.of("a", 1, "b", 2, "c", 3);
		var included = m.include("a", "c");
		assertTrue(included instanceof JsonMap);
		assertEquals(2, included.size());
		assertTrue(included.containsKey("a"));
		assertTrue(included.containsKey("c"));
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambdas contain multiple calls; only the primary call throws.
	})
	@Test void a26_unmodifiable() {
		var m = JsonMap.of("a", 1).unmodifiable();
		assertTrue(m.isUnmodifiable());
		assertThrows(UnsupportedOperationException.class, () -> m.put("b", 2));
		assertThrows(UnsupportedOperationException.class, () -> m.remove("a"));
		assertThrows(UnsupportedOperationException.class, () -> m.putAll(Collections.singletonMap("c", 3)));
		assertThrows(UnsupportedOperationException.class, m::clear);
		assertThrows(UnsupportedOperationException.class, () -> m.putIfAbsent("d", 4));
		assertThrows(UnsupportedOperationException.class, () -> m.remove("a", 1));
		assertThrows(UnsupportedOperationException.class, () -> m.replace("a", 99));
		assertThrows(UnsupportedOperationException.class, () -> m.replace("a", 1, 99));
		assertThrows(UnsupportedOperationException.class, () -> m.replaceAll((k, v) -> v));
		assertThrows(UnsupportedOperationException.class, () -> m.compute("a", (k, v) -> 0));
		assertThrows(UnsupportedOperationException.class, () -> m.computeIfAbsent("e", k -> 0));
		assertThrows(UnsupportedOperationException.class, () -> m.computeIfPresent("a", (k, v) -> 0));
		assertThrows(UnsupportedOperationException.class, () -> m.merge("a", 5, (v1, v2) -> v2));
		assertThrows(UnsupportedOperationException.class, () -> {
			var it = m.entrySet().iterator();
			it.next();
			it.remove();
		});
		assertThrows(UnsupportedOperationException.class, () -> {
			var it = m.keySet().iterator();
			it.next();
			it.remove();
		});
		assertThrows(UnsupportedOperationException.class, () -> {
			var it = m.values().iterator();
			it.next();
			it.remove();
		});
		assertEquals(1, m.size());
	}

	@Test void a27_modifiable() {
		var m = JsonMap.of("a", 1).unmodifiable().modifiable();
		assertFalse(m.isUnmodifiable());
		m.put("b", 2);
		assertEquals(2, m.size());
	}

	@Test void a28_inner() {
		var inner = JsonMap.of("a", 1);
		var outer = JsonMap.of("b", 2).inner(inner);
		assertEquals(1, outer.getInt("a"));
		assertEquals(2, outer.getInt("b"));
	}

	@Test void a29_filtered() {
		var m = JsonMap.create().filtered().append("a", null).append("b", 2);
		assertEquals(1, m.size());
		assertFalse(m.containsKey("a"));
		assertTrue(m.containsKey("b"));
	}

	@Test void a30_findString() {
		var m = JsonMap.of("alt", "value");
		assertEquals("value", m.findString("primary", "alt"));
	}

	@Test void a31_findMap() {
		var nested = JsonMap.of("x", 1);
		var m = JsonMap.of("nested", nested);
		var found = m.findMap("missing", "nested");
		assertTrue(found instanceof JsonMap);
		assertEquals(1, found.getInt("x"));
	}

	@Test void a32_findList() {
		var list = JsonList.of(1, 2);
		var m = JsonMap.of("arr", list);
		var found = m.findList("missing", "arr");
		assertTrue(found instanceof JsonList);
		assertEquals(2, found.size());
	}

	@Test void a33_emptyMapConstant() {
		assertTrue(JsonMap.EMPTY_MAP.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> JsonMap.EMPTY_MAP.put("x", 1));
	}

	@Test void a34_putJson() throws Exception {
		var m = JsonMap.create();
		m.putJson("nested", "{\"a\":1}");
		var nested = m.getMap("nested");
		assertNotNull(nested);
		assertEquals(1, nested.getInt("a"));
	}

	@Test void a35_filteredMap() {
		var m = JsonMap.filteredMap("a", 1, "b", 2);
		m.append("c", null);
		assertEquals(2, m.size());
		assertFalse(m.containsKey("c"));
		assertEquals(1, m.getInt("a"));
		assertEquals(2, m.getInt("b"));
	}

	@Test void a36_keepAll() {
		var m = JsonMap.of("a", 1, "b", 2, "c", 3).keepAll("a", "c");
		assertEquals(2, m.size());
		assertTrue(m.containsKey("a"));
		assertFalse(m.containsKey("b"));
		assertTrue(m.containsKey("c"));
	}

	@Test void a37_getFirstKey() {
		var m = JsonMap.of("first", 1, "second", 2);
		assertEquals("first", m.getFirstKey());
	}

	@Test void a38_writeTo() throws Exception {
		var m = JsonMap.of("a", 1);
		var sw = new StringWriter();
		m.writeTo(sw);
		assertEquals("{\"a\":1}", sw.toString());
	}

	@Test void a39_appendMap() {
		var extra = new LinkedHashMap<String,Object>();
		extra.put("c", 3);
		extra.put("d", 4);
		var m = JsonMap.of("a", 1).append(extra);
		assertEquals(3, m.size());
		assertEquals(3, m.getInt("c"));
	}

	@Test void a40_oddKeyValuePairsThrows() {
		assertThrows(IllegalArgumentException.class, () -> JsonMap.of("a", 1, "b"));
	}
}
