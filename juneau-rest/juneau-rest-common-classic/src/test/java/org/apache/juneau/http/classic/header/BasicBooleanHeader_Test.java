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

import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicBooleanHeader}.
 */
class BasicBooleanHeader_Test extends TestBase {

	private static final String NAME = "Foo";

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_booleanValue() {
		var h = BasicBooleanHeader.of(NAME, Boolean.TRUE);
		assertEquals(NAME, h.getName());
		assertEquals("true", h.getValue());
	}

	@Test void a02_of_booleanValue_null_returnsNull() {
		assertNull(BasicBooleanHeader.of(NAME, (Boolean)null));
	}

	@Test void a03_of_wireString() {
		var h = BasicBooleanHeader.of(NAME, "true");
		assertEquals("true", h.getValue());
	}

	@Test void a04_of_wireString_null_returnsNull() {
		assertNull(BasicBooleanHeader.of(NAME, (String)null));
	}

	@Test void a05_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = BasicBooleanHeader.of(NAME, () -> { calls[0]++; return Boolean.TRUE; });
		assertEquals(0, calls[0]);
		assertEquals("true", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void a06_of_supplier_null_returnsNull() {
		assertNull(BasicBooleanHeader.of(NAME, (Supplier<Boolean>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_wireString_parsable() {
		var h = new BasicBooleanHeader(NAME, "true");
		assertEquals(Boolean.TRUE, h.toBoolean());
	}

	@Test void b02_ctor_wireString_empty_treatedAsUnset() {
		// ie(value) is true for an empty string, so the parsed field short-circuits to null rather than calling
		// Boolean.parseBoolean("") (which would otherwise silently yield false).
		var h = new BasicBooleanHeader(NAME, "");
		assertNull(h.toBoolean());
	}

	@Test void b03_ctor_wireString_null() {
		var h = new BasicBooleanHeader(NAME, (String)null);
		assertNull(h.toBoolean());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_asBoolean_present() {
		assertEquals(Boolean.TRUE, new BasicBooleanHeader(NAME, "true").asBoolean().get());
	}

	@Test void c02_asBoolean_absent() {
		assertTrue(new BasicBooleanHeader(NAME, (Boolean)null).asBoolean().isEmpty());
	}

	@Test void c03_isTrue_true() {
		assertTrue(new BasicBooleanHeader(NAME, Boolean.TRUE).isTrue());
	}

	@Test void c04_isTrue_false() {
		assertFalse(new BasicBooleanHeader(NAME, Boolean.FALSE).isTrue());
	}

	@Test void c05_orElse_present() {
		assertEquals(Boolean.TRUE, new BasicBooleanHeader(NAME, Boolean.TRUE).orElse(Boolean.FALSE));
	}

	@Test void c06_orElse_absent() {
		assertEquals(Boolean.FALSE, new BasicBooleanHeader(NAME, (Boolean)null).orElse(Boolean.FALSE));
	}

	@Test void c07_toBoolean() {
		assertEquals(Boolean.TRUE, new BasicBooleanHeader(NAME, Boolean.TRUE).toBoolean());
	}

	@Test void c08_assertBoolean() {
		new BasicBooleanHeader(NAME, Boolean.TRUE).assertBoolean().isTrue();
	}
}
