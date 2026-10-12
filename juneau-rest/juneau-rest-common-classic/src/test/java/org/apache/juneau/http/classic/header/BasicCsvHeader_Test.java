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
 * Validates {@link BasicCsvHeader}.
 */
class BasicCsvHeader_Test extends TestBase {

	private static final String NAME = "Allow";

	@Test void a01_of_variants() {
		assertEquals("a, b", BasicCsvHeader.of(NAME, "a,b").getValue());
		assertEquals("a, b", BasicCsvHeader.of(NAME, "a", "b").getValue());
		assertEquals("a, b", BasicCsvHeader.of(NAME, () -> new String[]{"a", "b"}).getValue());
		assertNull(BasicCsvHeader.of(NAME, (String)null));
		assertNull(BasicCsvHeader.of(NAME, (String[])null));
		assertNull(BasicCsvHeader.of(NAME, (Supplier<String[]>)null));
	}

	@Test void a02_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = BasicCsvHeader.of(NAME, () -> { calls[0]++; return new String[]{"a"}; });
		assertEquals(0, calls[0]);
		assertEquals("a", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void b01_asArray_asList_toArray_toList() {
		var h = new BasicCsvHeader(NAME, "a,b");
		assertEquals(2, h.asArray().get().length);
		assertEquals(java.util.List.of("a", "b"), h.asList().get());
		assertArrayEquals(new String[]{"a", "b"}, h.toArray());
		assertEquals(java.util.List.of("a", "b"), h.toList());
		assertThrows(UnsupportedOperationException.class, () -> h.toList().add("c"));
	}

	@Test void b02_arrayIsDefensiveCopy() {
		var h = new BasicCsvHeader(NAME, "a", "b");
		h.toArray()[0] = "changed";
		assertEquals("a, b", h.getValue());
	}

	@Test void b03_orElse() {
		var h = new BasicCsvHeader(NAME, "a");
		assertArrayEquals(new String[]{"a"}, h.orElse(new String[]{"x"}));
		var none = new BasicCsvHeader(NAME, (Supplier<String[]>)() -> null);
		assertArrayEquals(new String[]{"x"}, none.orElse(new String[]{"x"}));
	}

	@Test void b04_assertList() {
		new BasicCsvHeader(NAME, "a,b").assertList().isSize(2);
		assertThrows(AssertionError.class, () -> new BasicCsvHeader(NAME, "a,b").assertList().isSize(3));
	}

	@Test void c01_contains_fixedValue() {
		var h = new BasicCsvHeader(NAME, "GET", "Post");
		assertTrue(h.contains("GET"));
		assertFalse(h.contains("get"));
		assertFalse(h.contains("PUT"));
		assertTrue(h.containsIgnoreCase("get"));
		assertTrue(h.containsIgnoreCase("POST"));
		assertFalse(h.containsIgnoreCase("PUT"));
	}

	@Test void c02_contains_supplier() {
		var h = new BasicCsvHeader(NAME, () -> new String[]{"GET", "Post"});
		assertTrue(h.contains("GET"));
		assertFalse(h.contains("get"));
		assertFalse(h.contains("PUT"));
		assertTrue(h.containsIgnoreCase("get"));
		assertFalse(h.containsIgnoreCase("PUT"));
	}

	@Test void c03_contains_nullSupplierValue() {
		var h = new BasicCsvHeader(NAME, (Supplier<String[]>)() -> null);
		assertFalse(h.contains("GET"));
		assertFalse(h.containsIgnoreCase("GET"));
		assertNull(h.toArray());
		assertNull(h.getValue());
	}

	@Test void c04_contains_readsSupplierOnce() {
		var calls = new int[1];
		var h = new BasicCsvHeader(NAME, () -> { calls[0]++; return calls[0] == 1 ? new String[]{"GET", "Post"} : null; });
		assertTrue(h.contains("Post"));
		assertEquals(1, calls[0]);
		calls[0] = 0;
		assertTrue(h.containsIgnoreCase("post"));
		assertEquals(1, calls[0]);
		calls[0] = 0;
		assertFalse(h.contains("PUT"));
		assertEquals(1, calls[0]);
		calls[0] = 0;
		assertFalse(h.containsIgnoreCase("PUT"));
		assertEquals(1, calls[0]);
	}
}
