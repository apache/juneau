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

class HttpStringHeader_Test extends TestBase {

	private static final String NAME = "X-Custom";

	//------------------------------------------------------------------------------------------------------------------
	// Factories
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_value() {
		var h = HttpStringHeader.of(NAME, "foo");
		assertEquals(NAME, h.getName());
		assertEquals("foo", h.getValue());
	}

	@Test void a02_of_valueSupplier() {
		var h = HttpStringHeader.of(NAME, () -> "foo");
		assertEquals(NAME, h.getName());
		assertEquals("foo", h.getValue());
	}

	@Test void a03_ofPair_null() {
		assertNull(HttpStringHeader.ofPair(null));
	}

	@Test void a04_ofPair_noDelimiter() {
		var h = HttpStringHeader.ofPair("justAName");
		assertEquals("justAName", h.getName());
		assertEquals("", h.getValue());
	}

	@Test void a05_ofPair_colon() {
		var h = HttpStringHeader.ofPair("X-Custom: foo");
		assertEquals("X-Custom", h.getName());
		assertEquals("foo", h.getValue());
	}

	@Test void a06_ofPair_equals() {
		var h = HttpStringHeader.ofPair("X-Custom=foo");
		assertEquals("X-Custom", h.getName());
		assertEquals("foo", h.getValue());
	}
}
