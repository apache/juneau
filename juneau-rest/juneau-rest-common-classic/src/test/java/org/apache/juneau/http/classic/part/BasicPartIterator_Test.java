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
package org.apache.juneau.http.classic.part;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicPartIterator}.
 */
class BasicPartIterator_Test extends TestBase {

	private static NameValuePair p(String name, String value) {
		return new BasicStringPart(name, value);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructor validation
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_ctor_nullParts_throws() {
		assertThrows(IllegalArgumentException.class, () -> new BasicPartIterator(null, null, false));
	}

	//------------------------------------------------------------------------------------------------------------------
	// filter(): name == null -- iterate everything unfiltered.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_noNameFilter_iteratesAllParts() {
		NameValuePair[] parts = {p("A", "1"), p("B", "2"), p("A", "3")};
		var it = new BasicPartIterator(parts, null, false);
		var names = new ArrayList<String>();
		while (it.hasNext())
			names.add(it.next().getName());
		assertEquals(List.of("A", "B", "A"), names);
	}

	//------------------------------------------------------------------------------------------------------------------
	// filter(): name != null, match found (eq() == true) and no match (eq() == false), case-sensitive and
	// case-insensitive variants, to cover all 4 branch outcomes on the (name == null) || eq(...) expression.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b02_nameFilter_caseSensitive_matchesExactCaseOnly() {
		NameValuePair[] parts = {p("Foo", "1"), p("foo", "2"), p("Bar", "3")};
		var it = new BasicPartIterator(parts, "Foo", false);
		assertTrue(it.hasNext());
		assertEquals("1", it.next().getValue());
		assertFalse(it.hasNext());
	}

	@Test void b03_nameFilter_caseInsensitive_matchesBothCases() {
		NameValuePair[] parts = {p("Foo", "1"), p("foo", "2"), p("Bar", "3")};
		var it = new BasicPartIterator(parts, "Foo", true);
		assertTrue(it.hasNext());
		assertEquals("1", it.next().getValue());
		assertTrue(it.hasNext());
		assertEquals("2", it.next().getValue());
		assertFalse(it.hasNext());
	}

	@Test void b04_nameFilter_noMatches_iteratorEmpty() {
		NameValuePair[] parts = {p("Foo", "1"), p("Bar", "2")};
		var it = new BasicPartIterator(parts, "Baz", false);
		assertFalse(it.hasNext());
	}

	//------------------------------------------------------------------------------------------------------------------
	// next() / hasNext() / remove()
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_next_returnsPartObject() {
		NameValuePair[] parts = {p("A", "1")};
		var it = new BasicPartIterator(parts, null, false);
		assertSame(parts[0], it.next());
	}

	@Test void c02_next_exhausted_throws() {
		NameValuePair[] parts = {p("A", "1")};
		var it = new BasicPartIterator(parts, null, false);
		it.next();
		assertThrows(NoSuchElementException.class, it::next);
	}

	@Test void c03_empty_hasNextFalse() {
		var it = new BasicPartIterator(new NameValuePair[0], null, false);
		assertFalse(it.hasNext());
	}

	@Test void c04_remove_throws() {
		var it = new BasicPartIterator(new NameValuePair[0], null, false);
		assertThrows(UnsupportedOperationException.class, it::remove);
	}
}
