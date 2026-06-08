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
package org.apache.juneau.marshall.json5;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Smoke tests for the JSON5-flavored {@link Json5List}.
 */
class Json5List_Test extends TestBase {

	@Test void a01_emptyToString() {
		var l = new Json5List();
		assertEquals("[]", l.toString());
	}

	@Test void a02_toStringIsJson5() {
		var l = new Json5List(1, "two", true);
		assertEquals("[1,'two',true]", l.toString());
	}

	@Test void a03_factoryOfString() throws Exception {
		var l = Json5List.ofString("[1,'two',true]");
		assertNotNull(l);
		assertEquals(3, l.size());
		assertEquals(1, l.getInt(0));
		assertEquals("two", l.getString(1));
		assertTrue(l.getBoolean(2));
	}

	@Test void a04_factoryOfStringNullInput() throws Exception {
		var l = Json5List.ofString((CharSequence)null);
		assertNotNull(l);
		assertTrue(l.isEmpty());
		assertEquals(Json5List.class, l.getClass());
		l.add("x");
		assertEquals(1, l.size());
	}

	@Test void a04c_factoryOfStringNullReader() throws Exception {
		var l = Json5List.ofString((Reader)null);
		assertNotNull(l);
		assertTrue(l.isEmpty());
		assertEquals(Json5List.class, l.getClass());
		l.add("x");
		assertEquals(1, l.size());
	}

	@Test void a04d_factoryOfStringNullCharSequenceWithParser() throws Exception {
		var l = Json5List.ofString((CharSequence)null, Json5Parser.DEFAULT);
		assertNotNull(l);
		assertTrue(l.isEmpty());
		assertEquals(Json5List.class, l.getClass());
		l.add("x");
		assertEquals(1, l.size());
	}

	@Test void a04e_factoryOfStringNullReaderWithParser() throws Exception {
		var l = Json5List.ofString((Reader)null, Json5Parser.DEFAULT);
		assertNotNull(l);
		assertTrue(l.isEmpty());
		assertEquals(Json5List.class, l.getClass());
		l.add("x");
		assertEquals(1, l.size());
	}

	@Test void a04f_factoryOfJson5OrCdlNullInput() throws Exception {
		var l = Json5List.ofJson5OrCdl(null);
		assertNotNull(l);
		assertTrue(l.isEmpty());
		assertEquals(Json5List.class, l.getClass());
		l.add("x");
		assertEquals(1, l.size());
	}

	@Test void a04g_factoryOfJson5OrCdlEmptyInput() throws Exception {
		var l = Json5List.ofJson5OrCdl("");
		assertNotNull(l);
		assertTrue(l.isEmpty());
		assertEquals(Json5List.class, l.getClass());
		l.add("x");
		assertEquals(1, l.size());
	}

	@Test void a04b_ofTextAliasStillWorks() throws Exception {
		var l = Json5List.ofString("[1,2,3]");
		assertNotNull(l);
		assertEquals(3, l.size());
	}

	@Test void a05_factoryCreateAndAppend() {
		var l = Json5List.create().append("a").append("b");
		assertEquals(2, l.size());
		assertEquals("a", l.getString(0));
	}

	@Test void a06_ctorCharSequenceUsesJson5Parser() throws Exception {
		var l = new Json5List("[1,2,3]");
		assertEquals(3, l.size());
	}

	@Test void a07_ctorReaderUsesJson5Parser() throws Exception {
		var l = new Json5List(new StringReader("[1,2,3]"));
		assertEquals(3, l.size());
	}

	@Test void a08_getMapReturnsJson5Map() {
		var nested = new Json5Map("x", 1);
		var l = new Json5List();
		l.add(nested);
		var got = l.getMap(0);
		assertNotNull(got);
		assertEquals(1, got.getInt("x"));
	}

	@Test void a09_getMapConvertsNeutralToJson5Map() {
		var stored = new MarshalledMap("x", 1);
		var l = new Json5List();
		l.add(stored);
		var got = l.getMap(0);
		assertNotNull(got);
	}

	@Test void a10_getListReturnsJson5List() {
		var inner = Json5List.of(1, 2);
		var l = new Json5List();
		l.add(inner);
		var nested = l.getList(0);
		assertNotNull(nested);
		assertEquals(2, nested.size());
	}

	@Test void a11_unmodifiableReturnsJson5List() {
		var l = Json5List.of("a", "b").unmodifiable();
		assertTrue(l.isUnmodifiable());
		assertThrows(UnsupportedOperationException.class, () -> l.add(0, "c"));
	}

	@Test void a12_modifiable() {
		var ro = Json5List.of("a").unmodifiable();
		var mod = ro.modifiable();
		assertFalse(mod.isUnmodifiable());
	}

	@Test void a13_appendReverseReturnsJson5List() {
		var l = Json5List.create().appendReverse("a", "b", "c");
		assertEquals("c", l.getString(0));
		assertEquals("a", l.getString(2));
	}

	@Test void a14_writeToUsesJson5Serializer() throws Exception {
		var l = Json5List.of(1, 2, 3);
		var sw = new StringWriter();
		l.writeTo(sw);
		assertEquals("[1,2,3]", sw.toString());
	}

	@Test void a15_toJson5SynonymForToString() {
		var l = Json5List.of(1, 2, 3);
		assertEquals(l.toString(), l.toJson5());
	}

	@Test void a16_emptyListConstant() {
		assertTrue(Json5List.EMPTY_LIST.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> Json5List.EMPTY_LIST.add(0, "x"));
	}

	@Test void a17_roundTripJson5() throws Exception {
		var original = Json5List.of(1, "two", true);
		var serialized = original.toString();
		var parsed = Json5List.ofString(serialized);
		assertEquals(1, parsed.getInt(0));
		assertEquals("two", parsed.getString(1));
		assertTrue(parsed.getBoolean(2));
	}

	@Test void a18_ofJson5OrCdl_array() throws Exception {
		var l = Json5List.ofJson5OrCdl("[1,2,3]");
		assertNotNull(l);
		assertEquals(3, l.size());
	}

	@Test void a19_ofJson5OrCdl_cdl() throws Exception {
		var l = Json5List.ofJson5OrCdl("a, b, c");
		assertNotNull(l);
		assertEquals(3, l.size());
		assertEquals("a", l.getString(0));
	}
}
