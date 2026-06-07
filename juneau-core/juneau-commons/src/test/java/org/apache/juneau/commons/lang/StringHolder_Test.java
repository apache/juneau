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
package org.apache.juneau.commons.lang;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

class StringHolder_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var a = StringHolder.create();
		assertNull(a.get());
		assertFalse(a.isPresent());
	}

	@Test
	void a02_of() {
		var a = StringHolder.of("Hello");
		assertEquals("Hello", a.get());
		assertTrue(a.isPresent());
	}

	@Test
	void a03_of_null() {
		var a = StringHolder.of(null);
		assertNull(a.get());
		assertFalse(a.isPresent());
	}

	@Test
	void a04_constructor_default() {
		var a = new StringHolder();
		assertNull(a.get());
	}

	@Test
	void a05_constructor_withValue() {
		var a = new StringHolder("Test");
		assertEquals("Test", a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// is() tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_is_match() {
		var a = StringHolder.of("John");
		assertTrue(a.is("John"));
	}

	@Test
	void b02_is_noMatch() {
		var a = StringHolder.of("John");
		assertFalse(a.is("Jane"));
	}

	@Test
	void b03_is_null() {
		var a = StringHolder.create();
		assertTrue(a.is(null));
		assertFalse(a.is("something"));
	}

	@Test
	void b04_is_nullArg() {
		var a = StringHolder.of("John");
		assertFalse(a.is(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// isAny(...) tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_isAny_match() {
		var a = StringHolder.of("John");
		assertTrue(a.isAny("John", "Jane", "Bob"));
	}

	@Test
	void c02_isAny_noMatch() {
		var a = StringHolder.of("John");
		assertFalse(a.isAny("Alice", "Charlie"));
	}

	@Test
	void c03_isAny_empty() {
		var a = StringHolder.of("John");
		assertFalse(a.isAny());
	}

	@Test
	void c04_isAny_withNull() {
		var a = StringHolder.create();
		assertTrue(a.isAny("John", null, "Jane"));
	}

	@Test
	void c05_isAny_allNull() {
		var a = StringHolder.create();
		assertTrue(a.isAny((String)null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// setIf() tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_setIf_true() {
		var a = StringHolder.of("old");
		a.setIf(true, "new");
		assertEquals("new", a.get());
	}

	@Test
	void d02_setIf_false() {
		var a = StringHolder.of("old");
		a.setIf(false, "new");
		assertEquals("old", a.get());
	}

	@Test
	void d03_setIf_chain() {
		var a = StringHolder.of("start");
		a.setIf(false, "skip1").setIf(true, "set").setIf(false, "skip2");
		assertEquals("set", a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// update() tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_update_basic() {
		var a = StringHolder.of("hello");
		a.update(String::toUpperCase);
		assertEquals("HELLO", a.get());
	}

	@Test
	void e02_update_null() {
		var a = StringHolder.create();
		a.update(String::toUpperCase);
		assertNull(a.get());  // Should be no-op
	}

	@Test
	void e03_update_chain() {
		var a = StringHolder.of("hello");
		a.update(String::toUpperCase).update(s -> s + "!");
		assertEquals("HELLO!", a.get());
	}

	@Test
	void e04_update_withTrim() {
		var a = StringHolder.of("  spaces  ");
		a.update(String::trim);
		assertEquals("spaces", a.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<String> functionality tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_set() {
		var a = StringHolder.create();
		a.set("Value");
		assertEquals("Value", a.get());
	}

	@Test
	void f02_setIfEmpty() {
		var a = StringHolder.of("existing");
		a.setIfEmpty("new");
		assertEquals("existing", a.get());  // Should not change

		a.set(null);
		a.setIfEmpty("new");
		assertEquals("new", a.get());  // Should change
	}

	@Test
	void f03_getAndSet() {
		var a = StringHolder.of("old");
		var b = a.getAndSet("new");
		assertEquals("old", b);
		assertEquals("new", a.get());
	}

	@Test
	void f04_getAndUnset() {
		var a = StringHolder.of("value");
		var b = a.getAndUnset();
		assertEquals("value", b);
		assertNull(a.get());
		assertFalse(a.isPresent());
	}

	@Test
	void f05_orElse() {
		var a = StringHolder.of("value");
		assertEquals("value", a.orElse("default"));

		a.set(null);
		assertEquals("default", a.orElse("default"));
	}

	@Test
	void f06_map() {
		var a = StringHolder.of("test");
		var b = a.map(s -> s.length());
		assertEquals(4, b.get());

		a.set(null);
		var c = a.map(s -> s.length());
		assertNull(c.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_trackLastValue() {
		var a = StringHolder.create();

		var list = l("a", "b", "c", "d", "e");
		list.forEach(a::set);

		assertEquals("e", a.get());
	}

	@Test
	void g02_conditionalUpdate() {
		var a = StringHolder.create();

		var list = l("apple", "banana", "apricot", "avocado");
		list.forEach(x -> a.setIf(x.startsWith("a"), x));

		assertEquals("avocado", a.get());  // Last "a" word
	}

	@Test
	void g03_transformationPipeline() {
		var a = StringHolder.of("  hello world  ");
		a.update(String::trim)
			.update(String::toUpperCase)
			.update(s -> s.replace(" ", "_"));

		assertEquals("HELLO_WORLD", a.get());
	}
}

