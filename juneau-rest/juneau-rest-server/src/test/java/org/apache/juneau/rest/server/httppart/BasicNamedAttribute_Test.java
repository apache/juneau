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
package org.apache.juneau.rest.server.httppart;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link BasicNamedAttribute}.
 *
 * <p>
 * This class is entirely self-contained (no {@code RestRequest} dependency), so unlike the other
 * {@code httppart} classes in this package it can be fully unit tested without any REST-session machinery.
 */
class BasicNamedAttribute_Test {

	@Test void a01_of_value() {
		var a = BasicNamedAttribute.of("foo", "bar");
		assertEquals("foo", a.getName());
		assertEquals("bar", a.getValue());
	}

	@Test void a02_of_supplier() {
		var a = BasicNamedAttribute.of("foo", () -> "bar");
		assertEquals("bar", a.getValue());
	}

	@Test void a03_of_supplier_reEvaluatedEachCall() {
		var counter = new int[]{0};
		var a = BasicNamedAttribute.of("foo", (java.util.function.Supplier<Object>) () -> ++counter[0]);
		assertEquals(1, a.getValue());
		assertEquals(2, a.getValue());
	}

	@Test void a04_constructor() {
		var a = new BasicNamedAttribute("foo", "bar");
		assertEquals("foo", a.getName());
		assertEquals("bar", a.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// ofPair
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ofPair_null() {
		assertNull(BasicNamedAttribute.ofPair(null));
	}

	@Test void b02_ofPair_colon() {
		var a = BasicNamedAttribute.ofPair("foo: bar");
		assertEquals("foo", a.getName());
		assertEquals("bar", a.getValue());
	}

	@Test void b03_ofPair_equals() {
		var a = BasicNamedAttribute.ofPair("foo=bar");
		assertEquals("foo", a.getName());
		assertEquals("bar", a.getValue());
	}

	@Test void b04_ofPair_noSeparator() {
		var a = BasicNamedAttribute.ofPair("justfoo");
		assertEquals("justfoo", a.getName());
		assertEquals("", a.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// get / isPresent / orElse
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_get_present() {
		assertEquals("bar", BasicNamedAttribute.of("foo", "bar").get());
	}

	@Test void c02_get_absent_throws() {
		assertThrows(NoSuchElementException.class, () -> BasicNamedAttribute.of("foo", (Object)null).get());
	}

	@Test void c03_isPresent() {
		assertTrue(BasicNamedAttribute.of("foo", "bar").isPresent());
		assertFalse(BasicNamedAttribute.of("foo", (Object)null).isPresent());
	}

	@Test void c04_orElse() {
		assertEquals("bar", BasicNamedAttribute.of("foo", "bar").orElse("def"));
		assertEquals("def", BasicNamedAttribute.of("foo", (Object)null).orElse("def"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions / toString
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_assertName() {
		BasicNamedAttribute.of("foo", "bar").assertName().is("foo");
	}

	@Test void d02_assertValue() {
		BasicNamedAttribute.of("foo", "bar").assertValue().is("bar");
	}

	@Test void d03_toString() {
		assertEquals("foo=bar", BasicNamedAttribute.of("foo", "bar").toString());
	}
}
