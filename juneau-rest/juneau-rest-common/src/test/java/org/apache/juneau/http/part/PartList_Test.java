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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link PartList}.
 */
class PartList_Test extends TestBase {

	@Test void a01_empty() {
		var l = PartList.empty();
		assertTrue(l.isEmpty());
		assertEquals(0, l.size());
	}

	@Test void a02_of_varargs() {
		var l = PartList.of(HttpPartBean.of("a", "1"), HttpPartBean.of("b", "2"));
		assertEquals(2, l.size());
	}

	@Test void a03_of_list() {
		var l = PartList.of(List.of(HttpPartBean.of("a", "1"), HttpPartBean.of("b", "2")));
		assertEquals(2, l.size());
		assertEquals("a", l.getParts().get(0).getName());
	}

	@Test void a04_ofPairs() {
		var l = PartList.ofPairs("a", "1", "b", "2");
		assertEquals(2, l.size());
		assertEquals("1", l.getFirst("a").getValue());
	}

	@Test void a05_ofPairs_oddLength_throws() {
		assertThrows(IllegalArgumentException.class, () -> PartList.ofPairs("a", "1", "b"));
	}

	@Test void b01_getFirst_found() {
		var l = PartList.ofPairs("a", "1", "b", "2");
		assertEquals("2", l.getFirst("b").getValue());
	}

	@Test void b02_getFirst_notFound_returnsNull() {
		var l = PartList.ofPairs("a", "1");
		assertNull(l.getFirst("z"));
	}

	@Test void c01_iterator() {
		var l = PartList.ofPairs("a", "1", "b", "2");
		var names = new ArrayList<String>();
		for (var p : l)
			names.add(p.getName());
		assertEquals(List.of("a", "b"), names);
	}

	@Test void d01_httpBody_contentType() {
		assertEquals("application/x-www-form-urlencoded", PartList.empty().getContentType());
	}

	@Test void d02_httpBody_contentLength_isUnknown() {
		assertEquals(-1, PartList.empty().getContentLength());
	}

	@Test void d03_httpBody_isRepeatable() {
		assertTrue(PartList.empty().isRepeatable());
	}

	@Test void d04_writeTo_encodesAndJoins() throws IOException {
		var l = PartList.ofPairs("a b", "1&2", "c", "3");
		var out = new ByteArrayOutputStream();
		l.writeTo(out);
		assertEquals("a+b=1%262&c=3", out.toString("UTF-8"));
	}

	@Test void d05_writeTo_skipsNullValues() throws IOException {
		var l = PartList.of(HttpPartBean.of("a", (String)null), HttpPartBean.of("b", "2"));
		var out = new ByteArrayOutputStream();
		l.writeTo(out);
		assertEquals("b=2", out.toString("UTF-8"));
	}

	@Test void e01_toString_matchesWriteTo() throws IOException {
		var l = PartList.ofPairs("a", "1", "b", "2");
		var out = new ByteArrayOutputStream();
		l.writeTo(out);
		assertEquals(out.toString("UTF-8"), l.toString());
	}

	@Test void e02_toString_skipsNullValues() {
		var l = PartList.of(HttpPartBean.of("a", (String)null));
		assertEquals("", l.toString());
	}
}
