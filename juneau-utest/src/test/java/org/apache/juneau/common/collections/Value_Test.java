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
package org.apache.juneau.common.collections;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.junit.jupiter.api.*;

class Value_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Value defined on parent class.
	//-----------------------------------------------------------------------------------------------------------------

	public static class A extends Value<A1>{}
	public static class A1 {}

	@Test void a01_testSubclass() {
		assertEquals(A1.class, ClassUtils.getParameterType(A.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getAndSet(T)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_getAndSet_withExistingValue() {
		var v = Value.of("old");
		var old = v.getAndSet("new");
		assertEquals("old", old, "Should return old value");
		assertEquals("new", v.get(), "Should have new value");
	}

	@Test
	void b02_getAndSet_withNullValue() {
		var v = Value.of(null);
		var old = v.getAndSet("new");
		assertNull(old, "Should return null");
		assertEquals("new", v.get(), "Should have new value");
	}

	@Test
	void b03_getAndSet_toNull() {
		var v = Value.of("old");
		var old = v.getAndSet(null);
		assertEquals("old", old, "Should return old value");
		assertNull(v.get(), "Should have null value");
	}

	@Test
	void b04_getAndSet_multiple() {
		var v = Value.of(1);
		assertEquals(1, v.getAndSet(2), "First getAndSet");
		assertEquals(2, v.getAndSet(3), "Second getAndSet");
		assertEquals(3, v.getAndSet(4), "Third getAndSet");
		assertEquals(4, v.get(), "Final value");
	}

	@Test
	void b05_getAndSet_withEmptyValue() {
		var v = Value.empty();
		var old = v.getAndSet("new");
		assertNull(old, "Should return null");
		assertEquals("new", v.get(), "Should have new value");
	}

	@Test
	void b06_getAndSet_chainability() {
		var v = Value.of("old");
		v.getAndSet("new");
		assertEquals("new", v.get(), "Value should be set after getAndSet");
	}

	@Test
	void b07_getAndSet_withListener() {
		var v = Value.of("old");
		var sb = new StringBuilder();
		v.listener((val) -> sb.append(val));
		
		var old = v.getAndSet("new");
		
		assertEquals("old", old, "Should return old value");
		assertEquals("new", v.get(), "Should have new value");
		assertEquals("new", sb.toString(), "Listener should be called with new value");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// is(T)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_is_equalStrings() {
		var v = Value.of("hello");
		assertTrue(v.is("hello"), "Should be equal to same string");
		assertFalse(v.is("world"), "Should not be equal to different string");
	}

	@Test
	void c02_is_withNull() {
		var v = Value.empty();
		assertTrue(v.is(null), "Empty value should equal null");
		assertFalse(v.is("test"), "Empty value should not equal non-null");
		
		v.set("test");
		assertFalse(v.is(null), "Non-null value should not equal null");
	}

	@Test
	void c03_is_equalIntegers() {
		var v = Value.of(42);
		assertTrue(v.is(42), "Should be equal to same integer");
		assertFalse(v.is(43), "Should not be equal to different integer");
	}

	@Test
	void c04_is_equalBooleans() {
		var v = Value.of(true);
		assertTrue(v.is(true), "Should be equal to true");
		assertFalse(v.is(false), "Should not be equal to false");
	}

	@Test
	void c05_is_equalArrays() {
		var v = Value.of(new int[]{1, 2, 3});
		assertTrue(v.is(new int[]{1, 2, 3}), "Should be equal to same array content");
		assertFalse(v.is(new int[]{1, 2, 4}), "Should not be equal to different array content");
		assertFalse(v.is(new int[]{1, 2}), "Should not be equal to shorter array");
	}

	@Test
	void c06_is_equalLists() {
		var v = Value.of(Utils.list("a", "b", "c"));
		assertTrue(v.is(Utils.list("a", "b", "c")), "Should be equal to same list content");
		assertFalse(v.is(Utils.list("a", "b", "d")), "Should not be equal to different list content");
	}

	@Test
	void c07_is_emptyVsEmpty() {
		var v1 = Value.empty();
		var v2 = Value.empty();
		assertTrue(v1.is(v2.get()), "Two empty values should be equal");
	}

	@Test
	void c08_is_differentTypes() {
		Value<Object> v = Value.of("42");
		assertFalse(v.is(42), "String '42' should not equal Integer 42");
	}

	@Test
	void c09_is_sameObject() {
		var s = "test";
		var v = Value.of(s);
		assertTrue(v.is(s), "Should be equal to same object reference");
	}

	@Test
	void c10_is_equalCustomObjects() {
		// Using A1 class defined at the top
		var obj1 = new A1();
		var obj2 = new A1();
		
		var v = Value.of(obj1);
		assertTrue(v.is(obj1), "Should be equal to same object");
		// Note: A1 doesn't override equals(), so different instances won't be equal
		assertFalse(v.is(obj2), "Should not be equal to different instance without equals override");
	}

	@Test
	void c11_is_afterSet() {
		var v = Value.of("initial");
		assertTrue(v.is("initial"));
		
		v.set("updated");
		assertFalse(v.is("initial"), "Should not equal old value after set");
		assertTrue(v.is("updated"), "Should equal new value after set");
	}
}

