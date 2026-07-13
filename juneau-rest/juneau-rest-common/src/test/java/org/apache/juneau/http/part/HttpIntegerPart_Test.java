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

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpIntegerPart_Test extends TestBase {

	private static final String NAME = "X-Count";

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_integerValue() {
		var p = HttpIntegerPart.of(NAME, 42);
		assertEquals(NAME, p.getName());
		assertEquals("42", p.getValue());
		assertEquals(Integer.valueOf(42), p.toInteger());
	}

	@Test void a02_of_nullInteger() {
		var p = HttpIntegerPart.of(NAME, (Integer)null);
		assertEquals(NAME, p.getName());
		assertNull(p.getValue());
		assertNull(p.toInteger());
	}

	@Test void a03_ofString_validWire() {
		var p = HttpIntegerPart.ofString(NAME, "123");
		assertEquals(NAME, p.getName());
		assertEquals("123", p.getValue());
		assertEquals(Integer.valueOf(123), p.toInteger());
	}

	@Test void a04_ofString_emptyWire() {
		var p = HttpIntegerPart.ofString(NAME, "");
		assertNull(p.toInteger());
	}

	@Test void a05_ofString_nullWire() {
		var p = HttpIntegerPart.ofString(NAME, (String)null);
		assertNull(p.toInteger());
	}

	@Test void a06_ofString_badWire_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Value 'abc' could not be parsed as an integer.", () -> HttpIntegerPart.ofString(NAME, "abc"));
	}

	@Test void a07_ofLazy_present() {
		var p = HttpIntegerPart.ofLazy(NAME, () -> 7);
		assertEquals(NAME, p.getName());
		assertEquals("7", p.getValue());
		assertEquals(Integer.valueOf(7), p.toInteger());
	}

	@Test void a08_ofLazy_nullSupplied() {
		var p = HttpIntegerPart.ofLazy(NAME, () -> null);
		assertNull(p.getValue());
		assertNull(p.toInteger());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asInteger_present() {
		assertEquals(Integer.valueOf(5), HttpIntegerPart.of(NAME, 5).asInteger().get());
	}

	@Test void b02_asInteger_absent() {
		assertTrue(HttpIntegerPart.of(NAME, (Integer)null).asInteger().isEmpty());
	}

	@Test void b03_orElse_present() {
		assertEquals(Integer.valueOf(5), HttpIntegerPart.of(NAME, 5).orElse(99));
	}

	@Test void b04_orElse_absent() {
		assertEquals(Integer.valueOf(99), HttpIntegerPart.of(NAME, (Integer)null).orElse(99));
	}

	@Test void b05_orElse_lazyAbsent() {
		assertEquals(Integer.valueOf(99), HttpIntegerPart.ofLazy(NAME, () -> null).orElse(99));
	}
}
