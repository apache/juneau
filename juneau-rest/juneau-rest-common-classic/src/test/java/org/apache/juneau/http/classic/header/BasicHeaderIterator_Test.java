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
package org.apache.juneau.http.classic.header;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicHeaderIterator}.
 */
class BasicHeaderIterator_Test extends TestBase {

	private static Header h(String name, String value) {
		return new BasicHeader(name, value);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructor validation
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_ctor_nullHeaders_throws() {
		assertThrows(IllegalArgumentException.class, () -> new BasicHeaderIterator(null, null, false));
	}

	//------------------------------------------------------------------------------------------------------------------
	// filter(): name == null -- iterate everything unfiltered.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_noNameFilter_iteratesAllHeaders() {
		Header[] headers = {h("A", "1"), h("B", "2"), h("A", "3")};
		var it = new BasicHeaderIterator(headers, null, false);
		var names = new ArrayList<String>();
		while (it.hasNext())
			names.add(it.nextHeader().getName());
		assertEquals(List.of("A", "B", "A"), names);
	}

	//------------------------------------------------------------------------------------------------------------------
	// filter(): name != null, match found (eq() == true) and no match (eq() == false), case-sensitive and
	// case-insensitive variants, to cover all 4 branch outcomes on the (name == null) || eq(...) expression.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b02_nameFilter_caseSensitive_matchesExactCaseOnly() {
		Header[] headers = {h("Foo", "1"), h("foo", "2"), h("Bar", "3")};
		var it = new BasicHeaderIterator(headers, "Foo", true);
		assertTrue(it.hasNext());
		assertEquals("1", it.nextHeader().getValue());
		assertFalse(it.hasNext());
	}

	@Test void b03_nameFilter_caseInsensitive_matchesBothCases() {
		Header[] headers = {h("Foo", "1"), h("foo", "2"), h("Bar", "3")};
		var it = new BasicHeaderIterator(headers, "Foo", false);
		assertTrue(it.hasNext());
		assertEquals("1", it.nextHeader().getValue());
		assertTrue(it.hasNext());
		assertEquals("2", it.nextHeader().getValue());
		assertFalse(it.hasNext());
	}

	@Test void b04_nameFilter_noMatches_iteratorEmpty() {
		Header[] headers = {h("Foo", "1"), h("Bar", "2")};
		var it = new BasicHeaderIterator(headers, "Baz", true);
		assertFalse(it.hasNext());
	}

	//------------------------------------------------------------------------------------------------------------------
	// next() / nextHeader() / hasNext() / remove()
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_next_returnsHeaderObject() {
		Header[] headers = {h("A", "1")};
		var it = new BasicHeaderIterator(headers, null, false);
		assertSame(headers[0], it.next());
	}

	@Test void c02_nextHeader_exhausted_throws() {
		Header[] headers = {h("A", "1")};
		var it = new BasicHeaderIterator(headers, null, false);
		it.nextHeader();
		assertThrows(NoSuchElementException.class, it::nextHeader);
	}

	@Test void c03_empty_hasNextFalse() {
		var it = new BasicHeaderIterator(new Header[0], null, false);
		assertFalse(it.hasNext());
	}

	@Test void c04_remove_throws() {
		var it = new BasicHeaderIterator(new Header[0], null, false);
		assertThrows(UnsupportedOperationException.class, it::remove);
	}
}
