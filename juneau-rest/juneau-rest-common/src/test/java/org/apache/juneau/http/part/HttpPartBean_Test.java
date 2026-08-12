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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link HttpPartBean}.
 */
class HttpPartBean_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Factories / basic accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_eager() {
		var p = HttpPartBean.of("status", "active");
		assertEquals("status", p.getName());
		assertEquals("active", p.getValue());
	}

	@Test void a02_of_supplier() {
		var p = HttpPartBean.of("page", () -> "2");
		assertEquals("2", p.getValue());
	}

	@Test void a03_of_nullName_rejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpPartBean.of(null, "active"));
	}

	@Test void a04_of_nullSupplier_rejected() {
		assertThrows(IllegalArgumentException.class, () -> HttpPartBean.of("page", (java.util.function.Supplier<String>)null));
	}

	@Test void a05_of_nullValue_isAllowed() {
		var p = HttpPartBean.of("status", (String)null);
		assertNull(p.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// toString()
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_toString() {
		assertEquals("status=active", HttpPartBean.of("status", "active").toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// equals() / hashCode()
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"unlikely-arg-type" // Intentionally comparing to a mismatched type to cover the equals() type-guard branch.
	})
	@Test void c01_equals_notAnInstance_returnsFalse() {
		var p = HttpPartBean.of("status", "active");
		// Deliberately call p.equals(...) directly (not assertNotEquals(str, p), which would invoke
		// String.equals(HttpPartBean) instead and never exercise the `instanceof` check at all).
		assertFalse(p.equals("status=active"));
	}

	@Test void c02_equals_null_returnsFalse() {
		assertFalse(HttpPartBean.of("status", "active").equals(null));
	}

	@Test void c03_equals_sameNameAndValue() {
		var p1 = HttpPartBean.of("status", "active");
		var p2 = HttpPartBean.of("status", "active");
		assertEquals(p1, p2);
		assertEquals(p1.hashCode(), p2.hashCode());
	}

	@Test void c04_equals_differentName_returnsFalse() {
		assertNotEquals(HttpPartBean.of("status", "active"), HttpPartBean.of("state", "active"));
	}

	@Test void c05_equals_differentValue_returnsFalse() {
		assertNotEquals(HttpPartBean.of("status", "active"), HttpPartBean.of("status", "inactive"));
	}

	@Test void c06_equals_sameInstance_returnsTrue() {
		var p = HttpPartBean.of("status", "active");
		assertEquals(p, p);
	}
}
