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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HttpLongPart_Test extends TestBase {

	private static final String NAME = "X-Size";

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_longValue() {
		var p = HttpLongPart.of(NAME, 42L);
		assertEquals(NAME, p.getName());
		assertEquals("42", p.getValue());
		assertEquals(Long.valueOf(42L), p.toLong());
	}

	@Test void a02_of_nullLong() {
		var p = HttpLongPart.of(NAME, (Long)null);
		assertEquals(NAME, p.getName());
		assertNull(p.getValue());
		assertNull(p.toLong());
	}

	@Test void a03_ofString_validWire() {
		var p = HttpLongPart.ofString(NAME, "9999999999");
		assertEquals(NAME, p.getName());
		assertEquals("9999999999", p.getValue());
		assertEquals(Long.valueOf(9999999999L), p.toLong());
	}

	@Test void a04_ofString_emptyWire() {
		var p = HttpLongPart.ofString(NAME, "");
		assertNull(p.toLong());
	}

	@Test void a05_ofString_nullWire() {
		var p = HttpLongPart.ofString(NAME, (String)null);
		assertNull(p.toLong());
	}

	@Test void a06_ofString_badWire_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Value 'abc' could not be parsed as a long.", () -> HttpLongPart.ofString(NAME, "abc"));
	}

	@Test void a07_ofLazy_present() {
		var p = HttpLongPart.ofLazy(NAME, () -> 7L);
		assertEquals(NAME, p.getName());
		assertEquals("7", p.getValue());
		assertEquals(Long.valueOf(7L), p.toLong());
	}

	@Test void a08_ofLazy_nullSupplied() {
		var p = HttpLongPart.ofLazy(NAME, () -> null);
		assertNull(p.getValue());
		assertNull(p.toLong());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asLong_present() {
		assertEquals(Long.valueOf(5L), HttpLongPart.of(NAME, 5L).asLong().get());
	}

	@Test void b02_asLong_absent() {
		assertTrue(HttpLongPart.of(NAME, (Long)null).asLong().isEmpty());
	}

	@Test void b03_orElse_present() {
		assertEquals(Long.valueOf(5L), HttpLongPart.of(NAME, 5L).orElse(99L));
	}

	@Test void b04_orElse_absent() {
		assertEquals(Long.valueOf(99L), HttpLongPart.of(NAME, (Long)null).orElse(99L));
	}

	@Test void b05_orElse_lazyAbsent() {
		assertEquals(Long.valueOf(99L), HttpLongPart.ofLazy(NAME, () -> null).orElse(99L));
	}
}
