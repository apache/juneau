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
 * Tests: {@link BasicBooleanPart}
 */
class BasicBooleanPart_Test extends TestBase {

	private static final String NAME = "X-Flag";

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_value() {
		var p = BasicBooleanPart.of(NAME, Boolean.TRUE);
		assertEquals(NAME, p.getName());
		assertEquals("true", p.getValue());
		assertEquals(Boolean.TRUE, p.toBoolean());
	}

	@Test void a02_of_nullValue_returnsNull() {
		assertNull(BasicBooleanPart.of(NAME, (Boolean)null));
	}

	@Test void a03_of_nullName_returnsNull() {
		assertNull(BasicBooleanPart.of(null, Boolean.TRUE));
	}

	@Test void a04_of_supplier() {
		var p = BasicBooleanPart.of(NAME, () -> Boolean.TRUE);
		assertEquals(NAME, p.getName());
		assertEquals("true", p.getValue());
		assertEquals(Boolean.TRUE, p.toBoolean());
	}

	@Test void a05_of_nullSupplier_returnsNull() {
		assertNull(BasicBooleanPart.of(NAME, (java.util.function.Supplier<Boolean>)null));
	}

	@Test void a06_of_nullName_supplier_returnsNull() {
		assertNull(BasicBooleanPart.of(null, () -> Boolean.TRUE));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ctor_booleanValue() {
		var p = new BasicBooleanPart(NAME, Boolean.FALSE);
		assertEquals("false", p.getValue());
		assertEquals(Boolean.FALSE, p.toBoolean());
	}

	@Test void b02_ctor_booleanValue_null() {
		var p = new BasicBooleanPart(NAME, (Boolean)null);
		assertNull(p.getValue());
		assertNull(p.toBoolean());
	}

	@Test void b03_ctor_stringValue() {
		var p = new BasicBooleanPart(NAME, "true");
		assertEquals(Boolean.TRUE, p.toBoolean());
	}

	@Test void b04_ctor_stringValue_falseWire() {
		assertEquals(Boolean.FALSE, new BasicBooleanPart(NAME, "false").toBoolean());
	}

	@Test void b05_ctor_stringValue_empty() {
		assertNull(new BasicBooleanPart(NAME, "").toBoolean());
	}

	@Test void b06_ctor_stringValue_null() {
		assertNull(new BasicBooleanPart(NAME, (String)null).toBoolean());
	}

	@Test void b07_ctor_supplier() {
		var p = new BasicBooleanPart(NAME, () -> Boolean.TRUE);
		assertEquals(Boolean.TRUE, p.toBoolean());
	}

	@Test void b08_ctor_supplier_nullSupplied() {
		var p = new BasicBooleanPart(NAME, () -> null);
		assertNull(p.getValue());
		assertNull(p.toBoolean());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_asBoolean_present() {
		assertEquals(Boolean.TRUE, BasicBooleanPart.of(NAME, Boolean.TRUE).asBoolean().get());
	}

	@Test void c02_asBoolean_absent() {
		assertTrue(new BasicBooleanPart(NAME, "").asBoolean().isEmpty());
	}

	@Test void c03_orElse_present() {
		assertEquals(Boolean.TRUE, BasicBooleanPart.of(NAME, Boolean.TRUE).orElse(Boolean.FALSE));
	}

	@Test void c04_orElse_absent() {
		assertEquals(Boolean.FALSE, new BasicBooleanPart(NAME, (Boolean)null).orElse(Boolean.FALSE));
	}

	@Test void c05_assertBoolean() {
		BasicBooleanPart.of(NAME, Boolean.TRUE).assertBoolean().isTrue();
	}
}
