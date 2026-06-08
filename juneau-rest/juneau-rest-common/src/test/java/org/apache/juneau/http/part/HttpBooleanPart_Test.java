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

class HttpBooleanPart_Test extends TestBase {

	private static final String NAME = "X-Flag";

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_trueValue() {
		var p = HttpBooleanPart.of(NAME, Boolean.TRUE);
		assertEquals(NAME, p.getName());
		assertEquals("true", p.getValue());
		assertEquals(Boolean.TRUE, p.toBoolean());
	}

	@Test void a02_of_falseValue() {
		var p = HttpBooleanPart.of(NAME, Boolean.FALSE);
		assertEquals(NAME, p.getName());
		assertEquals("false", p.getValue());
		assertEquals(Boolean.FALSE, p.toBoolean());
	}

	@Test void a03_of_nullBoolean() {
		var p = HttpBooleanPart.of(NAME, (Boolean)null);
		assertEquals(NAME, p.getName());
		assertNull(p.getValue());
		assertNull(p.toBoolean());
	}

	@Test void a04_ofString_trueWire() {
		var p = HttpBooleanPart.ofString(NAME, "true");
		assertEquals(NAME, p.getName());
		assertEquals("true", p.getValue());
		assertEquals(Boolean.TRUE, p.toBoolean());
	}

	@Test void a05_ofString_falseWire() {
		var p = HttpBooleanPart.ofString(NAME, "false");
		assertEquals(Boolean.FALSE, p.toBoolean());
	}

	@Test void a06_ofString_caseInsensitiveTrue() {
		// Boolean.valueOf("TRUE") and "True" both yield TRUE.
		assertEquals(Boolean.TRUE, HttpBooleanPart.ofString(NAME, "TRUE").toBoolean());
		assertEquals(Boolean.TRUE, HttpBooleanPart.ofString(NAME, "True").toBoolean());
	}

	@Test void a07_ofString_nonBooleanIsFalse() {
		// bool() helper uses Boolean.valueOf which yields false for any non-"true" string.
		assertEquals(Boolean.FALSE, HttpBooleanPart.ofString(NAME, "yes").toBoolean());
		assertEquals(Boolean.FALSE, HttpBooleanPart.ofString(NAME, "1").toBoolean());
	}

	@Test void a08_ofString_emptyWire() {
		var p = HttpBooleanPart.ofString(NAME, "");
		assertNull(p.toBoolean());
	}

	@Test void a09_ofString_nullWire() {
		var p = HttpBooleanPart.ofString(NAME, (String)null);
		assertNull(p.toBoolean());
	}

	@Test void a10_ofLazy_present() {
		var p = HttpBooleanPart.ofLazy(NAME, () -> Boolean.TRUE);
		assertEquals(NAME, p.getName());
		assertEquals("true", p.getValue());
		assertEquals(Boolean.TRUE, p.toBoolean());
	}

	@Test void a11_ofLazy_nullSupplied() {
		var p = HttpBooleanPart.ofLazy(NAME, () -> null);
		assertNull(p.getValue());
		assertNull(p.toBoolean());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Public accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asBoolean_present() {
		assertEquals(Boolean.TRUE, HttpBooleanPart.of(NAME, Boolean.TRUE).asBoolean().get());
	}

	@Test void b02_asBoolean_absent() {
		assertTrue(HttpBooleanPart.of(NAME, (Boolean)null).asBoolean().isEmpty());
	}

	@Test void b03_orElse_present() {
		assertEquals(Boolean.TRUE, HttpBooleanPart.of(NAME, Boolean.TRUE).orElse(Boolean.FALSE));
	}

	@Test void b04_orElse_absent() {
		assertEquals(Boolean.FALSE, HttpBooleanPart.of(NAME, (Boolean)null).orElse(Boolean.FALSE));
	}

	@Test void b05_orElse_lazyAbsent() {
		assertEquals(Boolean.TRUE, HttpBooleanPart.ofLazy(NAME, () -> null).orElse(Boolean.TRUE));
	}
}
