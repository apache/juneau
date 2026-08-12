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
package org.apache.juneau.http.header;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link HttpHeaderBean}.
 */
class HttpHeaderBean_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Factories / basic accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_eager() {
		var h = HttpHeaderBean.of("X-Foo", "bar");
		assertEquals("X-Foo", h.getName());
		assertEquals("bar", h.getValue());
	}

	@Test void a02_of_supplier() {
		var h = HttpHeaderBean.of("X-Foo", () -> "bar");
		assertEquals("bar", h.getValue());
	}

	@Test void a03_of_nullName_rejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpHeaderBean.of(null, "bar"));
	}

	@Test void a04_of_nullSupplier_rejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpHeaderBean.of("X-Foo", (java.util.function.Supplier<String>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// equalsIgnoreCase(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_equalsIgnoreCase_matches() {
		assertTrue(HttpHeaderBean.of("X-Foo", "Bar").equalsIgnoreCase("bar"));
	}

	@Test void b02_equalsIgnoreCase_noMatch() {
		assertFalse(HttpHeaderBean.of("X-Foo", "Bar").equalsIgnoreCase("baz"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// toString()
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_toString() {
		assertEquals("X-Foo: bar", HttpHeaderBean.of("X-Foo", "bar").toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// equals() / hashCode()
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_equals_notAnInstance_returnsFalse() {
		assertNotEquals(HttpHeaderBean.of("X-Foo", "bar"), "X-Foo: bar");
	}

	@Test void d02_equals_null_returnsFalse() {
		assertNotEquals(HttpHeaderBean.of("X-Foo", "bar"), null);
	}

	@Test void d03_equals_sameNameAndValue_ignoresNameCase() {
		var h1 = HttpHeaderBean.of("X-Foo", "bar");
		var h2 = HttpHeaderBean.of("x-foo", "bar");
		assertEquals(h1, h2);
		assertEquals(h1.hashCode(), h2.hashCode());
	}

	@Test void d04_equals_differentName_returnsFalse() {
		assertNotEquals(HttpHeaderBean.of("X-Foo", "bar"), HttpHeaderBean.of("X-Baz", "bar"));
	}

	@Test void d05_equals_differentValue_returnsFalse() {
		assertNotEquals(HttpHeaderBean.of("X-Foo", "bar"), HttpHeaderBean.of("X-Foo", "baz"));
	}

	@Test void d06_equals_sameInstance_returnsTrue() {
		var h = HttpHeaderBean.of("X-Foo", "bar");
		assertEquals(h, h);
	}
}
