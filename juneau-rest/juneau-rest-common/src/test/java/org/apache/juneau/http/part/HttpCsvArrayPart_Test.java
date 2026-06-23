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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpCsvArrayPart_Test extends TestBase {

	private static final String NAME = "X-Csv";

	/** Construct via the array constructor with a null array (avoids HttpPartBean.of(String,String) ambiguity). */
	private static HttpCsvArrayPart nullArray() {
		return new HttpCsvArrayPart(NAME, (String[])null);
	}

	/** Construct via the array constructor with a single element (avoids HttpPartBean.of(String,String) ambiguity). */
	private static HttpCsvArrayPart singleArray(String value) {
		return new HttpCsvArrayPart(NAME, new String[]{value});
	}

	//------------------------------------------------------------------------------------------------------------------
	// of(String, String...) — array constructor
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_singleValue() {
		var p = singleArray("a");
		assertEquals(NAME, p.getName());
		assertEquals("a", p.getValue());
	}

	@Test void a02_of_multipleValues() {
		var p = HttpCsvArrayPart.of(NAME, "a", "b", "c");
		assertEquals(NAME, p.getName());
		assertEquals("a,b,c", p.getValue());
	}

	@Test void a03_of_emptyArray() {
		// of(NAME) — varargs with zero elements
		var p = HttpCsvArrayPart.of(NAME);
		assertEquals(NAME, p.getName());
		assertEquals("", p.getValue());
	}

	@Test void a04_of_nullArray() {
		// Use constructor directly: of(NAME, (String[])null) is ambiguous against HttpPartBean.of(String,String)
		var p = nullArray();
		assertEquals(NAME, p.getName());
		assertNull(p.getValue());
	}

	@Test void a05_of_singleNullElement() {
		// null element inside a non-null array — join still produces a wire value
		var p = HttpCsvArrayPart.of(NAME, new String[]{null});
		assertNotNull(p.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// ofString(String, String) — wire-string constructor
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ofString_wireString() {
		var p = HttpCsvArrayPart.ofString(NAME, "x,y,z");
		assertEquals(NAME, p.getName());
		assertEquals("x,y,z", p.getValue());
	}

	@Test void b02_ofString_nullWire() {
		var p = HttpCsvArrayPart.ofString(NAME, (String)null);
		assertNull(p.getValue());
	}

	@Test void b03_ofString_emptyWire() {
		var p = HttpCsvArrayPart.ofString(NAME, "");
		assertEquals("", p.getValue());
	}

	@Test void b04_ofString_singleToken() {
		var p = HttpCsvArrayPart.ofString(NAME, "solo");
		assertEquals("solo", p.getValue());
		var arr = p.toArray();
		assertNotNull(arr);
		assertEquals(1, arr.length);
		assertEquals("solo", arr[0]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ofLazy(String, Supplier<String[]>) — supplier constructor
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_ofLazy_present() {
		var p = HttpCsvArrayPart.ofLazy(NAME, () -> new String[]{"p", "q"});
		assertEquals(NAME, p.getName());
		assertEquals("p,q", p.getValue());
	}

	@Test void c02_ofLazy_nullSupplied() {
		var p = HttpCsvArrayPart.ofLazy(NAME, () -> null);
		assertNull(p.getValue());
	}

	@Test void c03_ofLazy_evaluatedLazily() {
		var holder = new String[][]{{"first"}};
		var p = HttpCsvArrayPart.ofLazy(NAME, () -> holder[0]);
		holder[0] = new String[]{"second"};
		// getValue() re-invokes the supplier each time
		assertEquals("second", p.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// toArray()
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_toArray_fromArray() {
		var p = HttpCsvArrayPart.of(NAME, "a", "b");
		assertArrayEquals(new String[]{"a", "b"}, p.toArray());
	}

	@Test void d02_toArray_fromWire() {
		var p = HttpCsvArrayPart.ofString(NAME, "x,y,z");
		assertArrayEquals(new String[]{"x", "y", "z"}, p.toArray());
	}

	@Test void d03_toArray_nullValue() {
		assertNull(nullArray().toArray());
	}

	@Test void d04_toArray_fromLazyNull() {
		// supplier returns null → value() returns EMPTY array → copyOf returns empty (not null)
		var arr = HttpCsvArrayPart.ofLazy(NAME, () -> null).toArray();
		assertNotNull(arr);
		assertEquals(0, arr.length);
	}

	@Test void d05_toArray_returnsCopy() {
		var p = HttpCsvArrayPart.of(NAME, "a", "b");
		assertNotSame(p.toArray(), p.toArray());
	}

	//------------------------------------------------------------------------------------------------------------------
	// toList()
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_toList_fromArray() {
		var p = HttpCsvArrayPart.of(NAME, "a", "b", "c");
		assertEquals(List.of("a", "b", "c"), p.toList());
	}

	@Test void e02_toList_nullValue() {
		assertNull(nullArray().toList());
	}

	@Test void e03_toList_fromWire() {
		var p = HttpCsvArrayPart.ofString(NAME, "1,2");
		assertEquals(List.of("1", "2"), p.toList());
	}

	//------------------------------------------------------------------------------------------------------------------
	// asArray()
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_asArray_present() {
		var p = HttpCsvArrayPart.of(NAME, "a", "b");
		assertTrue(p.asArray().isPresent());
		assertArrayEquals(new String[]{"a", "b"}, p.asArray().get());
	}

	@Test void f02_asArray_absent() {
		assertTrue(nullArray().asArray().isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// asList()
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_asList_present() {
		var p = singleArray("x");
		assertTrue(p.asList().isPresent());
		assertEquals(List.of("x"), p.asList().get());
	}

	@Test void g02_asList_absent() {
		assertTrue(nullArray().asList().isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// contains(String) — case-sensitive
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_contains_true() {
		assertTrue(HttpCsvArrayPart.of(NAME, "a", "b", "c").contains("b"));
	}

	@Test void h02_contains_false() {
		assertFalse(HttpCsvArrayPart.of(NAME, "a", "b").contains("z"));
	}

	@Test void h03_contains_caseSensitive() {
		assertFalse(singleArray("A").contains("a"));
	}

	@Test void h04_contains_nullVal() {
		assertFalse(singleArray("a").contains(null));
	}

	@Test void h05_contains_nullArray() {
		assertFalse(nullArray().contains("a"));
	}

	@Test void h06_contains_fromWire() {
		assertTrue(HttpCsvArrayPart.ofString(NAME, "p,q,r").contains("q"));
	}

	@Test void h07_contains_fromLazy() {
		assertTrue(HttpCsvArrayPart.ofLazy(NAME, () -> new String[]{"x", "y"}).contains("y"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// containsIgnoreCase(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_containsIgnoreCase_matchesLower() {
		assertTrue(singleArray("Hello").containsIgnoreCase("hello"));
	}

	@Test void i02_containsIgnoreCase_matchesUpper() {
		assertTrue(singleArray("Hello").containsIgnoreCase("HELLO"));
	}

	@Test void i03_containsIgnoreCase_noMatch() {
		assertFalse(singleArray("Hello").containsIgnoreCase("world"));
	}

	@Test void i04_containsIgnoreCase_nullVal() {
		assertFalse(singleArray("a").containsIgnoreCase(null));
	}

	@Test void i05_containsIgnoreCase_nullArray() {
		assertFalse(nullArray().containsIgnoreCase("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// orElse(String[])
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_orElse_present() {
		var fallback = new String[]{"x"};
		assertArrayEquals(new String[]{"a", "b"}, HttpCsvArrayPart.of(NAME, "a", "b").orElse(fallback));
	}

	@Test void j02_orElse_absent() {
		var fallback = new String[]{"x"};
		assertArrayEquals(fallback, nullArray().orElse(fallback));
	}

	@Test void j03_orElse_lazyNull() {
		// supplier returns null → value() returns EMPTY (non-null) → orElse returns EMPTY, not the fallback
		var fallback = new String[]{"x"};
		var result = HttpCsvArrayPart.ofLazy(NAME, () -> null).orElse(fallback);
		assertNotNull(result);
		assertEquals(0, result.length);
	}
}
