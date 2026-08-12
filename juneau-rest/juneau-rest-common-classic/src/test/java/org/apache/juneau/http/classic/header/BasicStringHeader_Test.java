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
 * Validates {@link BasicStringHeader}.
 */
class BasicStringHeader_Test extends TestBase {

	private static final String NAME = "Host";

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_of_stringValue() {
		var h = BasicStringHeader.of(NAME, "foo.com");
		assertEquals(NAME, h.getName());
		assertEquals("foo.com", h.getValue());
	}

	@Test void a02_of_stringValue_null_returnsNull() {
		assertNull(BasicStringHeader.of(NAME, (String)null));
	}

	@Test void a03_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = BasicStringHeader.of(NAME, () -> { calls[0]++; return "foo.com"; });
		assertEquals(0, calls[0]);
		assertEquals("foo.com", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void a04_of_supplier_null_returnsNull() {
		assertNull(BasicStringHeader.of(NAME, (Supplier<String>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// ofPair()
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_ofPair_colonDelimited() {
		var h = BasicStringHeader.ofPair("Foo: bar");
		assertEquals("Foo", h.getName());
		assertEquals("bar", h.getValue());
	}

	@Test void b02_ofPair_equalsDelimited() {
		var h = BasicStringHeader.ofPair("Foo=bar");
		assertEquals("Foo", h.getName());
		assertEquals("bar", h.getValue());
	}

	@Test void b03_ofPair_noDelimiter_wholeStringIsName() {
		var h = BasicStringHeader.ofPair("FooBar");
		assertEquals("FooBar", h.getName());
		assertEquals("", h.getValue());
	}

	@Test void b04_ofPair_null_returnsNull() {
		assertNull(BasicStringHeader.ofPair(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_ctor_stringValue_null() {
		var h = new BasicStringHeader(NAME, (String)null);
		assertNull(h.getValue());
		assertTrue(h.asString().isEmpty());
	}

	@Test void c02_ctor_nameNull_throws() {
		assertThrows(IllegalArgumentException.class, () -> new BasicStringHeader(null, "foo.com"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Accessors
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_asString_present() {
		assertEquals("foo.com", new BasicStringHeader(NAME, "foo.com").asString().get());
	}

	@Test void d02_asString_absent() {
		assertTrue(new BasicStringHeader(NAME, (String)null).asString().isEmpty());
	}

	@Test void d03_orElse_present() {
		assertEquals("foo.com", new BasicStringHeader(NAME, "foo.com").orElse("default"));
	}

	@Test void d04_orElse_absent() {
		assertEquals("default", new BasicStringHeader(NAME, (String)null).orElse("default"));
	}

	@Test void d05_assertString() {
		new BasicStringHeader(NAME, "foo.com").assertString().is("foo.com");
	}
}
