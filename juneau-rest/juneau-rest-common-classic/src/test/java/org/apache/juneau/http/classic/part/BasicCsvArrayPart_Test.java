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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class BasicCsvArrayPart_Test extends TestBase {

	private static final String NAME = "X-Csv";

	private static BasicCsvArrayPart nullArray() {
		return new BasicCsvArrayPart(NAME, (String[])null);
	}

	private static BasicCsvArrayPart singleArray(String value) {
		return new BasicCsvArrayPart(NAME, new String[]{value});
	}

	//------------------------------------------------------------------------------------------------------------------
	// of(String, String...) — array factory
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_multipleValues() {
		var p = BasicCsvArrayPart.of(NAME, "a", "b", "c");
		assertEquals(NAME, p.getName());
		assertEquals("a,b,c", p.getValue());
	}

	@Test void a02_of_nullName_returnsNull() {
		// 2+ varargs elements avoid ambiguity with the inherited BasicPart.of(String,Object) overload.
		assertNull(BasicCsvArrayPart.of(null, "a", "b"));
	}

	@Test void a03_of_nullArray_returnsNull() {
		assertNull(BasicCsvArrayPart.of(NAME, (String[])null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// of(String, Supplier<String[]>) — lazy factory
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_of_supplier_present() {
		var p = BasicCsvArrayPart.of(NAME, () -> new String[]{"p", "q"});
		assertEquals(NAME, p.getName());
		assertEquals("p,q", p.getValue());
	}

	@Test void b02_of_supplier_nullName_returnsNull() {
		assertNull(BasicCsvArrayPart.of(null, () -> new String[]{"x"}));
	}

	@Test void b03_of_supplier_nullSupplier_returnsNull() {
		assertNull(BasicCsvArrayPart.of(NAME, (java.util.function.Supplier<String[]>)null));
	}

	@Test void b04_of_supplier_suppliesNull_getValueIsNull() {
		var p = BasicCsvArrayPart.of(NAME, () -> null);
		assertNull(p.getValue());
	}

	@Test void b05_of_supplier_evaluatedLazily() {
		var holder = new String[][]{{"first"}};
		var p = BasicCsvArrayPart.of(NAME, () -> holder[0]);
		holder[0] = new String[]{"second"};
		assertEquals("second", p.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Wire-string constructor -- BasicCsvArrayPart(String, String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_ctor_wireString() {
		var p = new BasicCsvArrayPart(NAME, "x,y,z");
		assertEquals(NAME, p.getName());
		assertArrayEquals(new String[]{"x", "y", "z"}, p.toArray());
	}

	@Test void c02_ctor_wireString_null() {
		var p = new BasicCsvArrayPart(NAME, (String)null);
		assertNull(p.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// toArray()
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_toArray_fromArray() {
		var p = BasicCsvArrayPart.of(NAME, "a", "b");
		assertArrayEquals(new String[]{"a", "b"}, p.toArray());
	}

	@Test void d02_toArray_nullValue() {
		assertNull(nullArray().toArray());
	}

	@Test void d03_toArray_fromLazyNull() {
		var arr = BasicCsvArrayPart.of(NAME, () -> null).toArray();
		assertNotNull(arr);
		assertEquals(0, arr.length);
	}

	@Test void d04_toArray_returnsCopy() {
		var p = BasicCsvArrayPart.of(NAME, "a", "b");
		assertNotSame(p.toArray(), p.toArray());
	}

	//------------------------------------------------------------------------------------------------------------------
	// toList()
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_toList_fromArray() {
		var p = BasicCsvArrayPart.of(NAME, "a", "b", "c");
		assertEquals(List.of("a", "b", "c"), p.toList());
	}

	@Test void e02_toList_nullValue() {
		assertNull(nullArray().toList());
	}

	//------------------------------------------------------------------------------------------------------------------
	// asArray()
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_asArray_present() {
		var p = BasicCsvArrayPart.of(NAME, "a", "b");
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
	// assertList()
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_assertList_isFluent() {
		BasicCsvArrayPart.of(NAME, "a", "b").assertList().isSize(2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// contains(String) — case-sensitive
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_contains_true() {
		assertTrue(BasicCsvArrayPart.of(NAME, "a", "b", "c").contains("b"));
	}

	@Test void i02_contains_false() {
		assertFalse(BasicCsvArrayPart.of(NAME, "a", "b").contains("z"));
	}

	@Test void i03_contains_caseSensitive() {
		assertFalse(singleArray("A").contains("a"));
	}

	@Test void i04_contains_nullVal() {
		assertFalse(singleArray("a").contains(null));
	}

	@Test void i05_contains_nullArray_throwsNpe() {
		// contains() (unlike orElse()/asArray()/toArray()) doesn't null-check value() before iterating,
		// so a part built from a null array throws NPE instead of returning false as its common-module counterpart
		// (HttpCsvArrayPart.contains) does. Pinning the current (buggy) behavior rather than fixing it here.
		assertThrows(NullPointerException.class, () -> nullArray().contains("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// containsIgnoreCase(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_containsIgnoreCase_matchesLower() {
		assertTrue(singleArray("Hello").containsIgnoreCase("hello"));
	}

	@Test void j02_containsIgnoreCase_matchesUpper() {
		assertTrue(singleArray("Hello").containsIgnoreCase("HELLO"));
	}

	@Test void j03_containsIgnoreCase_noMatch() {
		assertFalse(singleArray("Hello").containsIgnoreCase("world"));
	}

	@Test void j04_containsIgnoreCase_nullVal() {
		assertFalse(singleArray("a").containsIgnoreCase(null));
	}

	@Test void j05_containsIgnoreCase_nullArray_throwsNpe() {
		// See i05_contains_nullArray_throwsNpe -- same missing null-check bug in containsIgnoreCase().
		assertThrows(NullPointerException.class, () -> nullArray().containsIgnoreCase("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// orElse(String[])
	//------------------------------------------------------------------------------------------------------------------

	@Test void k01_orElse_present() {
		var fallback = new String[]{"x"};
		assertArrayEquals(new String[]{"a", "b"}, BasicCsvArrayPart.of(NAME, "a", "b").orElse(fallback));
	}

	@Test void k02_orElse_absent() {
		var fallback = new String[]{"x"};
		assertArrayEquals(fallback, nullArray().orElse(fallback));
	}

	@Test void k03_orElse_lazyNull() {
		var fallback = new String[]{"x"};
		var result = BasicCsvArrayPart.of(NAME, () -> null).orElse(fallback);
		assertNotNull(result);
		assertEquals(0, result.length);
	}
}
