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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests: {@link Thrown}, {@link Thrown.Part}
 */
class Thrown_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A — String constructor / Part(String) parsing
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_ctor_string_singleEntryWithMessage() {
		var x = new Thrown("java.lang.RuntimeException;boom");
		assertEquals("Thrown", x.getName());
		assertEquals("java.lang.RuntimeException;boom", x.getValue());
		var parts = x.asParts().get();
		assertEquals(1, parts.size());
		assertEquals("java.lang.RuntimeException", parts.get(0).getClassName());
		assertEquals("boom", parts.get(0).getMessage());
		assertEquals("java.lang.RuntimeException;boom", parts.get(0).toString());
	}

	@Test void a02_ctor_string_noMessage() {
		var x = new Thrown("java.lang.RuntimeException");
		var parts = x.asParts().get();
		assertEquals("java.lang.RuntimeException", parts.get(0).getClassName());
		assertNull(parts.get(0).getMessage());
	}

	@Test void a03_ctor_string_multipleEntries() {
		var x = new Thrown("RuntimeException;a, IllegalArgumentException;b");
		var parts = x.asParts().get();
		assertEquals(2, parts.size());
		assertEquals("RuntimeException", parts.get(0).getClassName());
		assertEquals("IllegalArgumentException", parts.get(1).getClassName());
	}

	@Test void a04_ctor_string_null() {
		var x = new Thrown((String)null);
		assertNull(x.getValue());
		assertTrue(x.asParts().isEmpty());
	}

	@Test void a05_of_string() {
		var x = Thrown.of("RuntimeException;boom");
		assertEquals("Thrown", x.getName());
		assertEquals("RuntimeException;boom", x.getValue());
	}

	@Test void a06_of_string_null_returnsNull() {
		assertNull(Thrown.of((String)null));
	}

	@Test void a07_empty_constant() {
		assertNull(Thrown.EMPTY.getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// B — Throwable factory / Part(Throwable)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_of_throwable_withMessage() {
		var t = new RuntimeException("boom");
		var x = Thrown.of(t);
		assertEquals("Thrown", x.getName());
		assertEquals("java.lang.RuntimeException;boom", x.getValue());
		var parts = x.asParts().get();
		assertEquals("java.lang.RuntimeException", parts.get(0).getClassName());
		assertEquals("boom", parts.get(0).getMessage());
	}

	@Test void b02_of_throwable_nullMessage() {
		// urlEncode(null) renders the literal text "null" (does not special-case null the way most codecs do).
		var x = Thrown.of(new RuntimeException());
		assertEquals("java.lang.RuntimeException;null", x.getValue());
	}

	@Test void b03_of_throwable_multiple() {
		var a = new RuntimeException("first");
		var b = new IllegalArgumentException("second");
		var x = Thrown.of(a, b);
		assertEquals("java.lang.RuntimeException;first, java.lang.IllegalArgumentException;second", x.getValue());
	}

	@Test void b04_of_throwable_emptyArray() {
		var x = Thrown.of(new Throwable[0]);
		assertEquals("", x.getValue());
		assertTrue(x.asParts().get().isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// C — List<Part> constructor
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_ctor_listOfParts() {
		var part = new Thrown.Part("RuntimeException;boom");
		var x = new Thrown(List.of(part));
		assertEquals("Thrown", x.getName());
		assertEquals("RuntimeException;boom", x.getValue());
		assertEquals(1, x.asParts().get().size());
	}

	@Test void c02_ctor_listOfParts_multiple() {
		var p1 = new Thrown.Part("RuntimeException;a");
		var p2 = new Thrown.Part("IllegalArgumentException;b");
		var x = new Thrown(List.of(p1, p2));
		assertEquals("RuntimeException;a, IllegalArgumentException;b", x.getValue());
	}
}
