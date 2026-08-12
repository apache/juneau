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
import java.util.function.*;

import org.apache.http.message.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicHeader}.
 */
class BasicHeader_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Constructors / factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_ctor_nameAndValue() {
		var h = new BasicHeader("Foo", "bar");
		assertEquals("Foo", h.getName());
		assertEquals("bar", h.getValue());
	}

	@Test void a02_ctor_nonStringValue_convertsViaToString() {
		var h = new BasicHeader("Foo", 123);
		assertEquals("123", h.getValue());
	}

	@Test void a03_ctor_nullValue() {
		var h = new BasicHeader("Foo", (Object)null);
		assertNull(h.getValue());
		assertTrue(h.asString().isEmpty());
	}

	@Test void a04_ctor_supplierValue_delaysEvaluation() {
		var calls = new int[1];
		var h = new BasicHeader("Foo", () -> { calls[0]++; return "bar"; });
		assertEquals(0, calls[0]);
		assertEquals("bar", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void a05_ctor_nameNull_throws() {
		assertThrows(IllegalArgumentException.class, () -> new BasicHeader(null, "bar"));
	}

	@Test void a06_ctor_nameEmpty_throws() {
		assertThrows(IllegalArgumentException.class, () -> new BasicHeader("", "bar"));
	}

	@Test void a07_of_nameValuePair() {
		var h = BasicHeader.of(new BasicNameValuePair("Foo", "bar"));
		assertEquals("Foo", h.getName());
		assertEquals("bar", h.getValue());
	}

	@Test void a08_of_nameAndValue() {
		var h = BasicHeader.of("Foo", "bar");
		assertEquals("Foo", h.getName());
		assertEquals("bar", h.getValue());
	}

	@Test void a09_of_nameAndValue_null_returnsNull() {
		assertNull(BasicHeader.of("Foo", null));
	}

	@Test void a10_copy() {
		var h = new BasicHeader("Foo", "bar");
		var h2 = h.copy();
		assertNotSame(h, h2);
		assertEquals(h.getName(), h2.getName());
		assertEquals(h.getValue(), h2.getValue());
	}

	@Test void a11_copy_preservesCachedElements() throws Exception {
		var h = new BasicHeader("Foo", "a=1; b=2");
		h.getElements();
		var h2 = h.copy();
		assertEquals(h.getElements().length, h2.getElements().length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// equals() / hashCode()
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_equals_sameNameAndValue_isEqual() {
		var a = new BasicHeader("Foo", "bar");
		var b = new BasicHeader("Foo", "bar");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test void b02_equals_differentName_notEqual() {
		var a = new BasicHeader("Foo", "bar");
		var b = new BasicHeader("Baz", "bar");
		assertNotEquals(a, b);
	}

	@Test void b03_equals_differentValue_notEqual() {
		var a = new BasicHeader("Foo", "bar");
		var b = new BasicHeader("Foo", "baz");
		assertNotEquals(a, b);
	}

	@Test void b04_equals_null_returnsFalse() {
		var a = new BasicHeader("Foo", "bar");
		assertFalse(a.equals(null));
	}

	@SuppressWarnings({
		"unlikely-arg-type" // Intentionally comparing to a mismatched type to cover the equals() type-guard branch.
	})
	@Test void b05_equals_notAHeader_returnsFalse() {
		var a = new BasicHeader("Foo", "bar");
		assertFalse(a.equals("not a header"));
	}

	@Test void b06_equals_reflexive() {
		var a = new BasicHeader("Foo", "bar");
		assertEquals(a, a);
	}

	@Test void b07_equals_crossImplementation_otherHeaderType() {
		// equals() gates on the Header *interface*, not this concrete class, so any Header impl with the same
		// name+value compares equal.
		var a = new BasicHeader("Foo", "bar");
		var b = new org.apache.http.message.BasicHeader("Foo", "bar");
		assertEquals(a, b);
	}

	//------------------------------------------------------------------------------------------------------------------
	// equalsIgnoreCase()
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_equalsIgnoreCase_match() {
		var h = new BasicHeader("Foo", "BAR");
		assertTrue(h.equalsIgnoreCase("bar"));
	}

	@Test void c02_equalsIgnoreCase_noMatch() {
		var h = new BasicHeader("Foo", "bar");
		assertFalse(h.equalsIgnoreCase("baz"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// get() / isPresent() / isNotEmpty() / orElse() / asString()
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_get_present() {
		var h = new BasicHeader("Foo", "bar");
		assertEquals("bar", h.get());
	}

	@Test void d02_get_absent_throws() {
		var h = new BasicHeader("Foo", (Object)null);
		assertThrows(NoSuchElementException.class, h::get);
	}

	@Test void d03_isPresent_true() {
		assertTrue(new BasicHeader("Foo", "bar").isPresent());
	}

	@Test void d04_isPresent_false() {
		assertFalse(new BasicHeader("Foo", (Object)null).isPresent());
	}

	@Test void d05_isNotEmpty_true() {
		assertTrue(new BasicHeader("Foo", "bar").isNotEmpty());
	}

	@Test void d06_isNotEmpty_false_whenAbsent() {
		assertFalse(new BasicHeader("Foo", (Object)null).isNotEmpty());
	}

	@Test void d07_isNotEmpty_false_whenEmptyString() {
		assertFalse(new BasicHeader("Foo", "").isNotEmpty());
	}

	@Test void d08_orElse_present() {
		assertEquals("bar", new BasicHeader("Foo", "bar").orElse("default"));
	}

	@Test void d09_orElse_absent() {
		assertEquals("default", new BasicHeader("Foo", (Object)null).orElse("default"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// getElements()
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_getElements_nullValue_returnsEmptyArray() throws Exception {
		var h = new BasicHeader("Foo", (Object)null);
		assertEquals(0, h.getElements().length);
	}

	@Test void e02_getElements_parsesAndCaches() throws Exception {
		var h = new BasicHeader("Foo", "a=1, b=2");
		var e1 = h.getElements();
		assertEquals(2, e1.length);
		// Second call returns from cache (copy-on-read), not a re-parse.
		var e2 = h.getElements();
		assertEquals(2, e2.length);
		assertNotSame(e1, e2);
	}

	@Test void e03_getElements_withSupplier_notCached() throws Exception {
		// supplier != null skips the elements-cache assignment.
		var h = new BasicHeader("Foo", (Supplier<Object>) () -> "a=1");
		var e1 = h.getElements();
		assertEquals(1, e1.length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// assertions / toString
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_assertName() {
		new BasicHeader("Foo", "bar").assertName().is("Foo");
	}

	@Test void f02_assertStringValue() {
		new BasicHeader("Foo", "bar").assertStringValue().is("bar");
	}

	@Test void f03_toString() {
		assertEquals("Foo: bar", new BasicHeader("Foo", "bar").toString());
	}
}
