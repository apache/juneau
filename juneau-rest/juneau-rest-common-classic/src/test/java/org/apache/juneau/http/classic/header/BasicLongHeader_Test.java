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
 * Validates {@link BasicLongHeader}.
 */
class BasicLongHeader_Test extends TestBase {

	private static final String NAME = "Content-Length";

	@Test void a01_of_typedValue() {
		var h = BasicLongHeader.of(NAME, 5L);
		assertEquals(NAME, h.getName());
		assertEquals("5", h.getValue());
	}

	@Test void a02_of_typedValue_null_returnsNull() {
		assertNull(BasicLongHeader.of(NAME, (Long)null));
	}

	@Test void a03_of_wireString() {
		assertEquals("7", BasicLongHeader.of(NAME, "7").getValue());
		assertNull(BasicLongHeader.of(NAME, (String)null));
	}

	@Test void a04_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = BasicLongHeader.of(NAME, () -> { calls[0]++; return 9L; });
		assertEquals(0, calls[0]);
		assertEquals("9", h.getValue());
		assertEquals(1, calls[0]);
		assertNull(BasicLongHeader.of(NAME, (Supplier<Long>)null));
	}

	@Test void a05_constructors_nullValues() {
		assertNull(new BasicLongHeader(NAME, (String)null).asLong().orElse(null));
	}

	@Test void b01_asLong_presentAndAbsent() {
		assertEquals(5L, new BasicLongHeader(NAME, "5").asLong().get());
		assertTrue(new BasicLongHeader(NAME, (Long)null).asLong().isEmpty());
		assertEquals(3L, new BasicLongHeader(NAME, () -> 3L).asLong().get());
	}

	@Test void b02_orElse() {
		assertEquals(5L, new BasicLongHeader(NAME, "5").orElse(1L));
		assertEquals(1L, new BasicLongHeader(NAME, (Long)null).orElse(1L));
	}

	@Test void b03_toLong() {
		assertEquals(5L, new BasicLongHeader(NAME, 5L).toLong());
		assertNull(new BasicLongHeader(NAME, (Supplier<Long>)() -> null).toLong());
		assertNull(new BasicLongHeader(NAME, (Supplier<Long>)() -> null).getValue());
	}

	@Test void b04_assertLong() {
		new BasicLongHeader(NAME, "5").assertLong().is(5L);
		assertThrows(AssertionError.class, () -> new BasicLongHeader(NAME, "5").assertLong().is(6L));
	}

	@Test void b05_wireString_invalid_throws() {
		assertThrows(IllegalArgumentException.class, () -> new BasicLongHeader(NAME, "abc"));
	}
}
