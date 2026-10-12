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
 * Validates {@link BasicIntegerHeader}.
 */
class BasicIntegerHeader_Test extends TestBase {

	private static final String NAME = "Max-Forwards";

	@Test void a01_of_typedValue() {
		var h = BasicIntegerHeader.of(NAME, 5);
		assertEquals(NAME, h.getName());
		assertEquals("5", h.getValue());
	}

	@Test void a02_of_typedValue_null_returnsNull() {
		assertNull(BasicIntegerHeader.of(NAME, (Integer)null));
	}

	@Test void a03_of_wireString() {
		assertEquals("7", BasicIntegerHeader.of(NAME, "7").getValue());
		assertNull(BasicIntegerHeader.of(NAME, (String)null));
	}

	@Test void a04_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = BasicIntegerHeader.of(NAME, () -> { calls[0]++; return 9; });
		assertEquals(0, calls[0]);
		assertEquals("9", h.getValue());
		assertEquals(1, calls[0]);
		assertNull(BasicIntegerHeader.of(NAME, (Supplier<Integer>)null));
	}

	@Test void a05_constructors_nullValues() {
		assertNull(new BasicIntegerHeader(NAME, (String)null).asInteger().orElse(null));
	}

	@Test void b01_asInteger_presentAndAbsent() {
		assertEquals(5, new BasicIntegerHeader(NAME, "5").asInteger().get());
		assertTrue(new BasicIntegerHeader(NAME, (Integer)null).asInteger().isEmpty());
		assertEquals(3, new BasicIntegerHeader(NAME, () -> 3).asInteger().get());
	}

	@Test void b02_orElse() {
		assertEquals(5, new BasicIntegerHeader(NAME, "5").orElse(1));
		assertEquals(1, new BasicIntegerHeader(NAME, (Integer)null).orElse(1));
	}

	@Test void b03_toInteger() {
		assertEquals(5, new BasicIntegerHeader(NAME, 5).toInteger());
		assertNull(new BasicIntegerHeader(NAME, (Supplier<Integer>)() -> null).toInteger());
		assertNull(new BasicIntegerHeader(NAME, (Supplier<Integer>)() -> null).getValue());
	}

	@Test void b04_assertInteger() {
		new BasicIntegerHeader(NAME, "5").assertInteger().is(5);
		assertThrows(AssertionError.class, () -> new BasicIntegerHeader(NAME, "5").assertInteger().is(6));
	}

	@Test void b05_wireString_invalid_throws() {
		assertThrows(IllegalArgumentException.class, () -> new BasicIntegerHeader(NAME, "abc"));
	}
}
