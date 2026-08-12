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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests: {@link BasicIntegerPart}
 */
class BasicIntegerPart_Test extends TestBase {

	private static final String NAME = "X-Count";

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_value() {
		var p = BasicIntegerPart.of(NAME, 123);
		assertEquals(NAME, p.getName());
		assertEquals("123", p.getValue());
		assertEquals(123, p.toInteger());
	}

	@Test void a02_of_nullValue_returnsNull() {
		assertNull(BasicIntegerPart.of(NAME, (Integer)null));
	}

	@Test void a03_of_nullName_returnsNull() {
		assertNull(BasicIntegerPart.of(null, 123));
	}

	@Test void a04_of_supplier() {
		var p = BasicIntegerPart.of(NAME, () -> 123);
		assertEquals("123", p.getValue());
		assertEquals(123, p.toInteger());
	}

	@Test void a05_of_nullSupplier_returnsNull() {
		assertNull(BasicIntegerPart.of(NAME, (java.util.function.Supplier<Integer>)null));
	}

	@Test void a06_of_nullName_supplier_returnsNull() {
		assertNull(BasicIntegerPart.of(null, () -> 123));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_integerValue() {
		var p = new BasicIntegerPart(NAME, 42);
		assertEquals("42", p.getValue());
		assertEquals(42, p.toInteger());
	}

	@Test void b02_ctor_integerValue_null() {
		var p = new BasicIntegerPart(NAME, (Integer)null);
		assertNull(p.getValue());
		assertNull(p.toInteger());
	}

	@Test void b03_ctor_stringValue() {
		assertEquals(42, new BasicIntegerPart(NAME, "42").toInteger());
	}

	@Test void b04_ctor_stringValue_empty() {
		assertNull(new BasicIntegerPart(NAME, "").toInteger());
	}

	@Test void b05_ctor_stringValue_null() {
		assertNull(new BasicIntegerPart(NAME, (String)null).toInteger());
	}

	@Test void b06_ctor_supplier() {
		assertEquals(42, new BasicIntegerPart(NAME, () -> 42).toInteger());
	}

	@Test void b07_ctor_supplier_nullSupplied() {
		var p = new BasicIntegerPart(NAME, () -> null);
		assertNull(p.getValue());
		assertNull(p.toInteger());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_asInteger_present() {
		assertEquals(42, BasicIntegerPart.of(NAME, 42).asInteger().get());
	}

	@Test void c02_asInteger_absent() {
		assertTrue(new BasicIntegerPart(NAME, "").asInteger().isEmpty());
	}

	@Test void c03_orElse_present() {
		assertEquals(42, BasicIntegerPart.of(NAME, 42).orElse(99));
	}

	@Test void c04_orElse_absent() {
		assertEquals(99, new BasicIntegerPart(NAME, (Integer)null).orElse(99));
	}

	@Test void c05_assertInteger() {
		BasicIntegerPart.of(NAME, 42).assertInteger().is(42);
	}
}
