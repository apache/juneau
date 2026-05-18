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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json5.*;
import org.junit.jupiter.api.*;

/**
 * Smoke tests for the marshaller-neutral {@link MarshalledMap} base class.
 *
 * <p>
 * These tests exercise the neutral base directly to confirm that:
 * <ul>
 * 	<li>{@link MarshalledMap#toString()} produces the inherited {@link LinkedHashMap} form (not JSON).
 * 	<li>{@link MarshalledMap#getMap} / {@link MarshalledMap#getList} return the neutral types.
 * 	<li>Construction via the parser-required ctors works.
 * 	<li>Bean conversion via {@link MarshalledMap#cast(Class)} still works.
 * </ul>
 */
class MarshalledMap_Test extends TestBase {

	@Test void a01_emptyToString() {
		var m = new MarshalledMap();
		assertEquals("{}", m.toString());
	}

	@Test void a02_toStringIsLinkedHashMapForm() {
		var m = new MarshalledMap("a", 1, "b", "two");
		// LinkedHashMap-style output, NOT JSON5.
		assertEquals("{a=1, b=two}", m.toString());
	}

	@Test void a03_factoryOf() {
		var m = MarshalledMap.of("k1", "v1", "k2", 2);
		assertEquals(2, m.size());
		assertEquals("v1", m.getString("k1"));
		assertEquals(2, m.getInt("k2"));
	}

	@Test void a04_factoryCreateAndAppend() {
		var m = MarshalledMap.create().append("a", 1).append("b", 2);
		assertEquals(2, m.size());
		assertEquals(1, m.getInt("a"));
	}

	@Test void a05_parseViaOfText() throws Exception {
		var m = MarshalledMap.ofText("{a:1,b:'two'}", Json5Parser.DEFAULT);
		assertNotNull(m);
		assertEquals(1, m.getInt("a"));
		assertEquals("two", m.getString("b"));
	}

	@Test void a06_parseViaOfTextNullInput() throws Exception {
		assertNull(MarshalledMap.ofText((CharSequence)null, Json5Parser.DEFAULT));
	}

	@Test void a07_getMapReturnsNeutralType() {
		var nested = new MarshalledMap("x", 1);
		var m = new MarshalledMap("nested", nested);
		var got = m.getMap("nested");
		assertTrue(got instanceof MarshalledMap, "Expected MarshalledMap, got " + got.getClass().getName());
		assertFalse(got instanceof JsonMap, "Stored MarshalledMap should not be returned as a JsonMap");
		assertEquals(1, got.getInt("x"));
	}

	@Test void a08_getListReturnsNeutralType() {
		var list = MarshalledList.of(1, 2, 3);
		var m = new MarshalledMap("arr", list);
		var got = m.getList("arr");
		assertTrue(got instanceof MarshalledList, "Expected MarshalledList, got " + got.getClass().getName());
		assertFalse(got instanceof JsonList, "Stored MarshalledList should not be returned as a JsonList");
		assertEquals(3, got.size());
	}

	@Test void a08b_parseProducesJson5MapInternally() throws Exception {
		var m = MarshalledMap.ofText("{nested:{x:1}}", Json5Parser.DEFAULT);
		var nested = m.getMap("nested");
		assertTrue(nested instanceof Json5Map, "Json5Parser produces Json5Map for nested objects");
		assertFalse(nested instanceof JsonMap, "Strict-JSON JsonMap is not used by Json5Parser");
	}

	@Test void a09_getMapCreateIfNotExists() {
		var m = MarshalledMap.create();
		var nested = m.getMap("nested", true);
		assertNotNull(nested);
		assertTrue(nested instanceof MarshalledMap);
		assertSame(nested, m.getMap("nested"));
	}

	@Test void a10_getListCreateIfNotExists() {
		var m = MarshalledMap.create();
		var list = m.getList("items", true);
		assertNotNull(list);
		assertTrue(list instanceof MarshalledList);
		assertSame(list, m.getList("items"));
	}

	@Test void a11_typedAccessors() {
		var m = MarshalledMap.of("i", "42", "b", "true", "l", "9999999999", "s", 123);
		assertEquals(42, m.getInt("i"));
		assertTrue(m.getBoolean("b"));
		assertEquals(9999999999L, m.getLong("l"));
		assertEquals("123", m.getString("s"));
	}

	@Test void a12_filterAndExclude() {
		var m = MarshalledMap.of("a", 1, "b", 2, "c", 3);
		var excluded = m.exclude("b");
		assertEquals(2, excluded.size());
		assertTrue(excluded instanceof MarshalledMap);
		assertFalse(excluded.containsKey("b"));
	}

	@Test void a13_includeReturnsNeutralType() {
		var m = MarshalledMap.of("a", 1, "b", 2, "c", 3);
		var included = m.include("a", "c");
		assertTrue(included instanceof MarshalledMap);
		assertEquals(2, included.size());
		assertTrue(included.containsKey("a"));
		assertTrue(included.containsKey("c"));
	}

	@Test void a14_unmodifiable() {
		var m = MarshalledMap.of("a", 1).unmodifiable();
		assertTrue(m.isUnmodifiable());
		assertThrows(UnsupportedOperationException.class, () -> m.put("b", 2));
	}

	@Test void a15_innerMap() {
		var inner = MarshalledMap.of("a", 1);
		var outer = MarshalledMap.of("b", 2).inner(inner);
		assertEquals(1, outer.getInt("a"));
		assertEquals(2, outer.getInt("b"));
	}

	@Test void a16_findAcrossKeys() {
		var m = MarshalledMap.of("alt", "value");
		assertEquals("value", m.findString("primary", "alt"));
	}

	@Test void a17_castToMapInterfaceReturnsNeutralFallback() throws Exception {
		var m = MarshalledMap.ofText("{a:1,b:2}", Json5Parser.DEFAULT);
		// Casting from a MarshalledMap to the Map interface should fall back to MarshalledMap.
		Map<?,?> casted = m.cast(Map.class);
		assertNotNull(casted);
		assertTrue(casted instanceof MarshalledMap);
		assertFalse(casted instanceof JsonMap);
	}

	@Test void a18_emptyMapConstant() {
		assertTrue(MarshalledMap.EMPTY_MAP.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> MarshalledMap.EMPTY_MAP.put("x", 1));
	}

	@Test void a19_keyValuePairCtor() {
		var m = new MarshalledMap("a", 1, "b", 2);
		assertEquals(1, m.getInt("a"));
		assertEquals(2, m.getInt("b"));
	}

	@Test void a20_oddKeyValuePairsThrows() {
		assertThrows(IllegalArgumentException.class, () -> new MarshalledMap("a", 1, "b"));
	}
}
