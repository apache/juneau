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
package org.apache.juneau.json5;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Smoke tests for the JSON5-flavored {@link Json5Map}.
 *
 * <p>
 * These confirm that:
 * <ul>
 * 	<li>{@link Json5Map#toString()} produces JSON5 form.
 * 	<li>JSON5 default-parser constructors and {@code ofString} factories work.
 * 	<li>Covariant overrides on the get/fluent surface return {@code Json5Map} / {@code Json5List}, not the neutral base.
 * 	<li>{@code writeTo(Writer)} uses {@link Json5Serializer#DEFAULT}.
 * </ul>
 */
class Json5Map_Test extends TestBase {

	@Test void a01_emptyToString() {
		var m = new Json5Map();
		assertEquals("{}", m.toString());
	}

	@Test void a02_toStringIsJson5() {
		var m = new Json5Map("a", 1, "b", "two");
		// JSON5: unquoted keys, single-quoted strings.
		assertEquals("{a:1,b:'two'}", m.toString());
	}

	@Test void a03_factoryOfString() throws Exception {
		var m = Json5Map.ofString("{a:1,b:'two'}");
		assertNotNull(m);
		assertEquals(1, m.getInt("a"));
		assertEquals("two", m.getString("b"));
	}

	@Test void a04_factoryOfStringNullInput() throws Exception {
		assertNull(Json5Map.ofString((CharSequence)null));
	}

	@Test void a04b_ofTextAliasStillWorks() throws Exception {
		var m = Json5Map.ofString("{a:1}");
		assertNotNull(m);
		assertEquals(1, m.getInt("a"));
	}

	@Test void a05_factoryOfCreateAppend() {
		var m = Json5Map.create().append("a", 1).append("b", 2);
		assertEquals(2, m.size());
		assertEquals(1, m.getInt("a"));
		assertTrue(m instanceof Json5Map);
	}

	@Test void a06_ctorCharSequenceUsesJson5Parser() throws Exception {
		var m = new Json5Map("{x:42}");
		assertEquals(42, m.getInt("x"));
	}

	@Test void a07_ctorReaderUsesJson5Parser() throws Exception {
		var m = new Json5Map(new StringReader("{x:42}"));
		assertEquals(42, m.getInt("x"));
	}

	@Test void a08_getMapReturnsJson5Map() {
		var nested = new Json5Map("x", 1);
		var m = new Json5Map("nested", nested);
		var got = m.getMap("nested");
		assertNotNull(got);
		assertTrue(got instanceof Json5Map, "Expected Json5Map, got " + got.getClass().getName());
		assertEquals(1, got.getInt("x"));
	}

	@Test void a09_getMapConvertsNeutralToJson5Map() {
		// Even when a MarshalledMap is stored, getMap on Json5Map should convert/return Json5Map.
		var stored = new MarshalledMap("x", 1);
		var m = new Json5Map("nested", stored);
		var got = m.getMap("nested");
		assertNotNull(got);
		assertTrue(got instanceof Json5Map, "Expected Json5Map after narrowing, got " + got.getClass().getName());
	}

	@Test void a10_getListReturnsJson5List() {
		var inner = Json5List.of(1, 2);
		var m = new Json5Map();
		m.put("arr", inner);
		var got = m.getList("arr");
		assertNotNull(got);
		assertTrue(got instanceof Json5List, "Expected Json5List, got " + got.getClass().getName());
		assertEquals(2, got.size());
	}

	@Test void a11_getMapCreateIfNotExistsReturnsJson5Map() {
		var m = Json5Map.create();
		var nested = m.getMap("nested", true);
		assertNotNull(nested);
		assertTrue(nested instanceof Json5Map);
		assertSame(nested, m.getMap("nested"));
	}

	@Test void a12_getListCreateIfNotExistsReturnsJson5List() {
		var m = Json5Map.create();
		var list = m.getList("items", true);
		assertNotNull(list);
		assertTrue(list instanceof Json5List);
		assertSame(list, m.getList("items"));
	}

	@Test void a13_excludeReturnsJson5Map() {
		var m = Json5Map.of("a", 1, "b", 2, "c", 3);
		var excluded = m.exclude("b");
		assertTrue(excluded instanceof Json5Map);
		assertEquals(2, excluded.size());
		assertFalse(excluded.containsKey("b"));
	}

	@Test void a14_includeReturnsJson5Map() {
		var m = Json5Map.of("a", 1, "b", 2, "c", 3);
		var included = m.include("a", "c");
		assertTrue(included instanceof Json5Map);
		assertEquals(2, included.size());
		assertTrue(included.containsKey("a"));
		assertTrue(included.containsKey("c"));
	}

	@Test void a15_unmodifiableReturnsJson5Map() {
		var m = Json5Map.of("a", 1).unmodifiable();
		assertTrue(m instanceof Json5Map);
		assertTrue(m.isUnmodifiable());
		assertThrows(UnsupportedOperationException.class, () -> m.put("b", 2));
	}

	@Test void a16_filteredReturnsJson5Map() {
		var m = Json5Map.create().filtered();
		assertTrue(m instanceof Json5Map);
	}

	@Test void a17_writeToUsesJson5Serializer() throws Exception {
		var m = Json5Map.of("a", 1);
		var sw = new StringWriter();
		m.writeTo(sw);
		// JSON5 form: unquoted key.
		assertEquals("{a:1}", sw.toString());
	}

	@Test void a18_toJson5SynonymForToString() {
		var m = Json5Map.of("a", 1);
		assertEquals(m.toString(), m.toJson5());
	}

	@Test void a19_putJson5() throws Exception {
		var m = new Json5Map();
		m.putJson5("nested", "{x:1,y:'two'}");
		var nested = m.getMap("nested");
		assertNotNull(nested);
		assertEquals(1, nested.getInt("x"));
		assertEquals("two", nested.getString("y"));
	}

	@Test void a20_emptyMapConstant() {
		assertTrue(Json5Map.EMPTY_MAP.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> Json5Map.EMPTY_MAP.put("x", 1));
	}

	@Test void a21_roundTripJson5() throws Exception {
		var original = Json5Map.of("a", 1, "b", "two", "c", true);
		var serialized = original.toString();
		var parsed = Json5Map.ofString(serialized);
		assertEquals(1, parsed.getInt("a"));
		assertEquals("two", parsed.getString("b"));
		assertTrue(parsed.getBoolean("c"));
	}

	@Test void a22_parserProducesJson5Map() throws Exception {
		Object o = Json5Parser.DEFAULT.parse("{a:1,b:[1,2,3]}", Object.class);
		assertTrue(o instanceof Json5Map, "Expected Json5Map, got " + o.getClass().getName());
		Object inner = ((Json5Map)o).get("b");
		assertTrue(inner instanceof Json5List, "Expected Json5List for nested array, got " + inner.getClass().getName());
	}
}
