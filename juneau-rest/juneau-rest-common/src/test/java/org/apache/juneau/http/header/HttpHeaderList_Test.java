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
package org.apache.juneau.http.header;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.junit.jupiter.api.*;

class HttpHeaderList_Test extends TestBase {

	@Test void a01_create_emptyList() {
		var list = HttpHeaderList.create();
		assertTrue(list.isEmpty());
		assertEquals(0, list.size());
	}

	@Test void a02_of_varargs() {
		var list = HttpHeaderList.of(HttpHeaderBean.of("A", "1"), HttpHeaderBean.of("B", "2"));
		assertEquals(2, list.size());
		assertEquals("1", list.getFirst("A").getValue());
		assertEquals("2", list.getFirst("B").getValue());
	}

	@Test void a03_ofPairs() {
		var list = HttpHeaderList.ofPairs("Accept", "text/xml", "Content-Type", "text/json");
		assertEquals(2, list.size());
		assertEquals("text/xml", list.getFirst("Accept").getValue());
	}

	@Test void a04_ofPairs_rejectsOddCount() {
		assertThrows(IllegalArgumentException.class, () -> HttpHeaderList.ofPairs("a", "b", "c"));
	}

	@Test void a05_append() {
		var list = HttpHeaderList.create()
			.append(HttpHeaderBean.of("A", "1"))
			.append("B", "2");
		assertEquals(2, list.size());
		assertEquals("1", list.getFirst("A").getValue());
		assertEquals("2", list.getFirst("B").getValue());
	}

	@Test void a06_caseInsensitiveByDefault() {
		var list = HttpHeaderList.create().append("Accept", "text/xml");
		assertNotNull(list.getFirst("accept"));
		assertNotNull(list.getFirst("ACCEPT"));
		assertTrue(list.contains("accept"));
	}

	@Test void a07_caseSensitiveToggle() {
		var list = HttpHeaderList.create().caseSensitive(true).append("Accept", "text/xml");
		assertNull(list.getFirst("accept"));
		assertNotNull(list.getFirst("Accept"));
	}

	@Test void a08_setDefault_firstWins() {
		var list = HttpHeaderList.create()
			.setDefault("Accept", "first")
			.setDefault("Accept", "second");
		assertEquals(1, list.size());
		assertEquals("first", list.getFirst("Accept").getValue());
	}

	@Test void a09_set_replacesByName() {
		var list = HttpHeaderList.create()
			.append("Accept", "first")
			.set("Accept", "second");
		assertEquals(1, list.size());
		assertEquals("second", list.getFirst("Accept").getValue());
	}

	@Test void a10_set_caseInsensitive() {
		var list = HttpHeaderList.create()
			.append("Accept", "first")
			.set("ACCEPT", "second");
		assertEquals(1, list.size());
		assertEquals("second", list.getFirst("Accept").getValue());
	}

	@Test void a11_removeAll() {
		var list = HttpHeaderList.create()
			.append("X-Trace", "a")
			.append("X-Trace", "b")
			.append("Accept", "text/xml")
			.removeAll("X-Trace");
		assertEquals(1, list.size());
		assertNull(list.getFirst("X-Trace"));
	}

	@Test void a12_getAll_returnsAllMatches() {
		var list = HttpHeaderList.create()
			.append("X-Trace", "a")
			.append("X-Trace", "b")
			.append("Accept", "text/xml");
		var traces = list.getAll("X-Trace");
		assertEquals(2, traces.size());
		assertEquals("a", traces.get(0).getValue());
		assertEquals("b", traces.get(1).getValue());
	}

	@Test void a13_getLast() {
		var list = HttpHeaderList.create()
			.append("X-Trace", "a")
			.append("X-Trace", "b");
		assertEquals("b", list.getLast("X-Trace").getValue());
		assertNull(list.getLast("Accept"));
	}

	@Test void a14_copy_isIndependent() {
		var original = HttpHeaderList.create().append("A", "1");
		var copy = original.copy();
		copy.append("B", "2");
		assertEquals(1, original.size());
		assertEquals(2, copy.size());
	}

	@Test void a15_voidSentinel() {
		var v = new HttpHeaderList.Void();
		assertTrue(v.isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Coverage backfill — overloads not exercised by the cases above.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_of_list() {
		var list = HttpHeaderList.of(List.of(HttpHeaderBean.of("A", "1"), HttpHeaderBean.of("B", "2")));
		assertEquals(2, list.size());
	}

	@Test void b02_of_nullList_ignored() {
		var list = HttpHeaderList.of((List<HttpHeader>)null);
		assertTrue(list.isEmpty());
	}

	@Test void b03_of_varargs_nullsIgnored() {
		var list = HttpHeaderList.of((HttpHeader)null, HttpHeaderBean.of("A", "1"), null);
		assertEquals(1, list.size());
	}

	@Test void b04_ofPairs_nullArrayTreatedAsEmpty() {
		var list = HttpHeaderList.ofPairs((String[])null);
		assertTrue(list.isEmpty());
	}

	@Test void b05_append_nullIgnored() {
		var list = HttpHeaderList.create().append((HttpHeader)null);
		assertTrue(list.isEmpty());
	}

	@Test void b06_append_nullArrayIgnored() {
		var list = HttpHeaderList.create().append((HttpHeader[])null);
		assertTrue(list.isEmpty());
	}

	@Test void b07_append_nullListIgnored() {
		var list = HttpHeaderList.create().append((List<HttpHeader>)null);
		assertTrue(list.isEmpty());
	}

	@Test void b08_setDefault_nullIgnored() {
		var list = HttpHeaderList.create().setDefault((HttpHeader)null);
		assertTrue(list.isEmpty());
	}

	@Test void b09_setDefault_varargs() {
		var list = HttpHeaderList.create().setDefault(
			HttpHeaderBean.of("A", "1"),
			HttpHeaderBean.of("B", "2"),
			HttpHeaderBean.of("A", "ignored")  // duplicate name, skipped
		);
		assertEquals(2, list.size());
		assertEquals("1", list.getFirst("A").getValue());
	}

	@Test void b10_setDefault_varargs_nullArrayIgnored() {
		var list = HttpHeaderList.create().setDefault((HttpHeader[])null);
		assertTrue(list.isEmpty());
	}

	@Test void b11_set_nullIgnored() {
		var list = HttpHeaderList.create().append("A", "1").set((HttpHeader)null);
		assertEquals(1, list.size());
	}

	@Test void b12_set_varargs() {
		var list = HttpHeaderList.create()
			.append("A", "old")
			.set(HttpHeaderBean.of("A", "new"), HttpHeaderBean.of("B", "1"));
		assertEquals(2, list.size());
		assertEquals("new", list.getFirst("A").getValue());
		assertEquals("1", list.getFirst("B").getValue());
	}

	@Test void b13_set_varargs_nullArrayIgnored() {
		var list = HttpHeaderList.create().append("A", "1").set((HttpHeader[])null);
		assertEquals(1, list.size());
	}

	@Test void b14_getAll_asArray() {
		var list = HttpHeaderList.create().append("A", "1").append("B", "2");
		var arr = list.getAll();
		assertEquals(2, arr.length);
	}

	@Test void b15_forEach_byName() {
		var list = HttpHeaderList.create()
			.append("X-Trace", "a")
			.append("X-Trace", "b")
			.append("Accept", "text/xml");
		var counter = new AtomicInteger();
		list.forEach("X-Trace", h -> counter.incrementAndGet());
		assertEquals(2, counter.get());
	}

	@Test void b16_toHeaderArray() {
		var list = HttpHeaderList.create().append("A", "1").append("B", "2");
		var arr = list.toHeaderArray();
		assertEquals(2, arr.length);
	}

	@Test void b17_append_lazySupplier() {
		var list = HttpHeaderList.create().append("X-Lazy", () -> "computed");
		assertEquals("computed", list.getFirst("X-Lazy").getValue());
	}

	@Test void b18_copy_preservesCaseSensitive() {
		var original = HttpHeaderList.create().caseSensitive(true).append("Accept", "text/xml");
		var copy = original.copy();
		assertNull(copy.getFirst("accept"));
		assertNotNull(copy.getFirst("Accept"));
	}
}
