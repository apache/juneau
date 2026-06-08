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
package org.apache.juneau.http.part;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.junit.jupiter.api.*;

class HttpPartList_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_create_emptyList() {
		var list = HttpPartList.create();
		assertTrue(list.isEmpty());
		assertEquals(0, list.size());
	}

	@Test void a02_of_varargs() {
		var list = HttpPartList.of(HttpPartBean.of("a", "1"), HttpPartBean.of("b", "2"));
		assertEquals(2, list.size());
		assertEquals("1", list.getFirst("a").getValue());
		assertEquals("2", list.getFirst("b").getValue());
	}

	@Test void a03_of_list() {
		var list = HttpPartList.of(List.of(HttpPartBean.of("a", "1"), HttpPartBean.of("b", "2")));
		assertEquals(2, list.size());
	}

	@Test void a04_of_nullList_ignored() {
		var list = HttpPartList.of((List<HttpPart>)null);
		assertTrue(list.isEmpty());
	}

	@Test void a05_of_varargs_nullsIgnored() {
		var list = HttpPartList.of((HttpPart)null, HttpPartBean.of("a", "1"), null);
		assertEquals(1, list.size());
	}

	@Test void a06_ofPairs() {
		var list = HttpPartList.ofPairs("foo", "1", "bar", "2");
		assertEquals(2, list.size());
		assertEquals("1", list.getFirst("foo").getValue());
	}

	@Test void a07_ofPairs_rejectsOddCount() {
		assertThrows(IllegalArgumentException.class, () -> HttpPartList.ofPairs("a", "b", "c"));
	}

	@Test void a08_ofPairs_nullArrayTreatedAsEmpty() {
		var list = HttpPartList.ofPairs((String[])null);
		assertTrue(list.isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Append / case sensitivity
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_append() {
		var list = HttpPartList.create()
			.append(HttpPartBean.of("a", "1"))
			.append("b", "2");
		assertEquals(2, list.size());
		assertEquals("1", list.getFirst("a").getValue());
		assertEquals("2", list.getFirst("b").getValue());
	}

	@Test void b02_append_nullIgnored() {
		var list = HttpPartList.create().append((HttpPart)null);
		assertTrue(list.isEmpty());
	}

	@Test void b03_append_nullArrayIgnored() {
		var list = HttpPartList.create().append((HttpPart[])null);
		assertTrue(list.isEmpty());
	}

	@Test void b04_append_nullListIgnored() {
		var list = HttpPartList.create().append((List<HttpPart>)null);
		assertTrue(list.isEmpty());
	}

	@Test void b05_caseSensitiveByDefault() {
		var list = HttpPartList.create().append("foo", "1");
		assertNotNull(list.getFirst("foo"));
		assertNull(list.getFirst("FOO"));
		assertFalse(list.contains("FOO"));
	}

	@Test void b06_caseInsensitiveToggle() {
		var list = HttpPartList.create().caseInsensitive(true).append("foo", "1");
		assertNotNull(list.getFirst("FOO"));
		assertNotNull(list.getFirst("Foo"));
		assertTrue(list.contains("foo"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// setDefault / set
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_setDefault_firstWins() {
		var list = HttpPartList.create()
			.setDefault("foo", "first")
			.setDefault("foo", "second");
		assertEquals(1, list.size());
		assertEquals("first", list.getFirst("foo").getValue());
	}

	@Test void c02_setDefault_nullIgnored() {
		var list = HttpPartList.create().setDefault((HttpPart)null);
		assertTrue(list.isEmpty());
	}

	@Test void c03_setDefault_varargs() {
		var list = HttpPartList.create().setDefault(
			HttpPartBean.of("a", "1"),
			HttpPartBean.of("b", "2"),
			HttpPartBean.of("a", "ignored")  // duplicate name, skipped
		);
		assertEquals(2, list.size());
		assertEquals("1", list.getFirst("a").getValue());
	}

	@Test void c04_setDefault_varargs_nullArrayIgnored() {
		var list = HttpPartList.create().setDefault((HttpPart[])null);
		assertTrue(list.isEmpty());
	}

	@Test void c05_set_replacesByName() {
		var list = HttpPartList.create()
			.append("foo", "first")
			.set("foo", "second");
		assertEquals(1, list.size());
		assertEquals("second", list.getFirst("foo").getValue());
	}

	@Test void c06_set_nullIgnored() {
		var list = HttpPartList.create().append("a", "1").set((HttpPart)null);
		assertEquals(1, list.size());
	}

	@Test void c07_set_varargs() {
		var list = HttpPartList.create()
			.append("a", "old")
			.set(HttpPartBean.of("a", "new"), HttpPartBean.of("b", "1"));
		assertEquals(2, list.size());
		assertEquals("new", list.getFirst("a").getValue());
		assertEquals("1", list.getFirst("b").getValue());
	}

	@Test void c08_set_varargs_nullArrayIgnored() {
		var list = HttpPartList.create().append("a", "1").set((HttpPart[])null);
		assertEquals(1, list.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// removeAll / getAll / getLast / forEach
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_removeAll() {
		var list = HttpPartList.create()
			.append("trace", "a")
			.append("trace", "b")
			.append("foo", "1")
			.removeAll("trace");
		assertEquals(1, list.size());
		assertNull(list.getFirst("trace"));
	}

	@Test void d02_getAll_byName() {
		var list = HttpPartList.create()
			.append("trace", "a")
			.append("trace", "b")
			.append("foo", "1");
		var traces = list.getAll("trace");
		assertEquals(2, traces.size());
		assertEquals("a", traces.get(0).getValue());
		assertEquals("b", traces.get(1).getValue());
	}

	@Test void d03_getAll_asArray() {
		var list = HttpPartList.create().append("a", "1").append("b", "2");
		var arr = list.getAll();
		assertEquals(2, arr.length);
	}

	@Test void d04_getLast() {
		var list = HttpPartList.create()
			.append("trace", "a")
			.append("trace", "b");
		assertEquals("b", list.getLast("trace").getValue());
		assertNull(list.getLast("foo"));
	}

	@Test void d05_forEach_byName() {
		var list = HttpPartList.create()
			.append("trace", "a")
			.append("trace", "b")
			.append("foo", "1");
		var counter = new AtomicInteger();
		list.forEach("trace", p -> counter.incrementAndGet());
		assertEquals(2, counter.get());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Copy / array / Void
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_copy_isIndependent() {
		var original = HttpPartList.create().append("a", "1");
		var copy = original.copy();
		copy.append("b", "2");
		assertEquals(1, original.size());
		assertEquals(2, copy.size());
	}

	@Test void e02_copy_preservesCaseInsensitive() {
		var original = HttpPartList.create().caseInsensitive(true).append("foo", "1");
		var copy = original.copy();
		assertNotNull(copy.getFirst("FOO"));
	}

	@Test void e03_toPartArray() {
		var list = HttpPartList.create().append("a", "1").append("b", "2");
		var arr = list.toPartArray();
		assertEquals(2, arr.length);
	}

	@Test void e04_voidSentinel() {
		var v = new HttpPartList.Void();
		assertTrue(v.isEmpty());
	}
}
