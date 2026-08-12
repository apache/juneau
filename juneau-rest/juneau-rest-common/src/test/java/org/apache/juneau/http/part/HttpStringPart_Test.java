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
 * Tests for {@link HttpStringPart}.
 */
class HttpStringPart_Test extends TestBase {

	@Test void a01_of_nameValue() {
		var p = HttpStringPart.of("foo", "bar");
		assertEquals("foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void a02_of_supplier() {
		var p = HttpStringPart.of("foo", () -> "lazy");
		assertEquals("lazy", p.getValue());
	}

	@Test void b01_ofPair_null_returnsNull() {
		assertNull(HttpStringPart.ofPair(null));
	}

	@Test void b02_ofPair_equalsForm() {
		var p = HttpStringPart.ofPair("foo=bar");
		assertEquals("foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void b03_ofPair_colonForm() {
		var p = HttpStringPart.ofPair("foo:bar");
		assertEquals("foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void b04_ofPair_noDelimiter() {
		var p = HttpStringPart.ofPair("justname");
		assertEquals("justname", p.getName());
		assertEquals("", p.getValue());
	}

	@Test void b05_ofPair_trimsWhitespace() {
		var p = HttpStringPart.ofPair(" foo = bar ");
		assertEquals("foo", p.getName());
		assertEquals("bar", p.getValue());
	}

	@Test void c01_asString_present() {
		var p = HttpStringPart.of("foo", "bar");
		assertTrue(p.asString().isPresent());
		assertEquals("bar", p.asString().get());
	}

	@Test void c02_asString_empty_whenNullValue() {
		var p = HttpStringPart.of("foo", (String)null);
		assertTrue(p.asString().isEmpty());
	}

	@Test void d01_orElse_returnsValue_whenPresent() {
		var p = HttpStringPart.of("foo", "bar");
		assertEquals("bar", p.orElse("default"));
	}

	@Test void d02_orElse_returnsOther_whenNull() {
		var p = HttpStringPart.of("foo", (String)null);
		assertEquals("default", p.orElse("default"));
	}
}
