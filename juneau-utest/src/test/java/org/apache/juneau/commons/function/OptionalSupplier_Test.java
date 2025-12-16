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
package org.apache.juneau.commons.function;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class OptionalSupplier_Test extends TestBase {

	//====================================================================================================
	// Static factory methods
	//====================================================================================================
	@Test
	void a01_of() {
		AtomicInteger callCount = new AtomicInteger();
		OptionalSupplier<String> supplier = OptionalSupplier.of(() -> {
			callCount.incrementAndGet();
			return "value";
		});

		assertEquals("value", supplier.get());
		assertEquals(1, callCount.get());
		assertEquals("value", supplier.get());
		assertEquals(2, callCount.get()); // Called again (not cached)
	}

	@Test
	void a02_of_nullSupplier() {
		assertThrows(IllegalArgumentException.class, () -> OptionalSupplier.of(null));
	}

	@Test
	void a03_ofNullable() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertEquals("value", supplier.get());
	}

	@Test
	void a04_ofNullable_null() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		assertNull(supplier.get());
	}

	@Test
	void a05_empty() {
		OptionalSupplier<String> supplier = OptionalSupplier.empty();
		assertNull(supplier.get());
	}

	//====================================================================================================
	// isPresent() / isEmpty()
	//====================================================================================================
	@Test
	void b01_isPresent_true() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertTrue(supplier.isPresent());
		assertFalse(supplier.isEmpty());
	}

	@Test
	void b02_isPresent_false() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		assertFalse(supplier.isPresent());
		assertTrue(supplier.isEmpty());
	}

	//====================================================================================================
	// map()
	//====================================================================================================
	@Test
	void c01_map_present() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("hello");
		OptionalSupplier<Integer> mapped = supplier.map(String::length);
		assertEquals(5, mapped.get());
	}

	@Test
	void c02_map_empty() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		OptionalSupplier<Integer> mapped = supplier.map(String::length);
		assertNull(mapped.get());
	}

	@Test
	void c03_map_nullMapper() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertThrows(IllegalArgumentException.class, () -> supplier.map(null));
	}

	@Test
	void c04_map_returnsNull() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		OptionalSupplier<String> mapped = supplier.map(s -> null);
		assertNull(mapped.get());
	}

	//====================================================================================================
	// flatMap()
	//====================================================================================================
	@Test
	void d01_flatMap_present() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("hello");
		OptionalSupplier<Integer> mapped = supplier.flatMap(s -> OptionalSupplier.ofNullable(s.length()));
		assertEquals(5, mapped.get());
	}

	@Test
	void d02_flatMap_empty() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		OptionalSupplier<Integer> mapped = supplier.flatMap(s -> OptionalSupplier.ofNullable(s.length()));
		assertNull(mapped.get());
	}

	@Test
	void d03_flatMap_returnsEmpty() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("hello");
		OptionalSupplier<Integer> mapped = supplier.flatMap(s -> OptionalSupplier.empty());
		assertNull(mapped.get());
	}

	@Test
	void d04_flatMap_returnsNull() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("hello");
		OptionalSupplier<Integer> mapped = supplier.flatMap(s -> null);
		assertNull(mapped.get());
	}

	@Test
	void d05_flatMap_nullMapper() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertThrows(IllegalArgumentException.class, () -> supplier.flatMap(null));
	}

	//====================================================================================================
	// filter()
	//====================================================================================================
	@Test
	void e01_filter_matches() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("hello");
		OptionalSupplier<String> filtered = supplier.filter(s -> s.length() > 3);
		assertEquals("hello", filtered.get());
	}

	@Test
	void e02_filter_noMatch() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("hi");
		OptionalSupplier<String> filtered = supplier.filter(s -> s.length() > 3);
		assertNull(filtered.get());
	}

	@Test
	void e03_filter_empty() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		OptionalSupplier<String> filtered = supplier.filter(s -> s.length() > 3);
		assertNull(filtered.get());
	}

	@Test
	void e04_filter_nullPredicate() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertThrows(IllegalArgumentException.class, () -> supplier.filter(null));
	}

	//====================================================================================================
	// orElse()
	//====================================================================================================
	@Test
	void f01_orElse_present() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertEquals("value", supplier.orElse("default"));
	}

	@Test
	void f02_orElse_empty() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		assertEquals("default", supplier.orElse("default"));
	}

	@Test
	void f03_orElse_nullDefault() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		assertNull(supplier.orElse(null));
	}

	//====================================================================================================
	// orElseGet()
	//====================================================================================================
	@Test
	void g01_orElseGet_present() {
		AtomicInteger callCount = new AtomicInteger();
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		String result = supplier.orElseGet(() -> {
			callCount.incrementAndGet();
			return "default";
		});
		assertEquals("value", result);
		assertEquals(0, callCount.get()); // Should not be called
	}

	@Test
	void g02_orElseGet_empty() {
		AtomicInteger callCount = new AtomicInteger();
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		String result = supplier.orElseGet(() -> {
			callCount.incrementAndGet();
			return "default";
		});
		assertEquals("default", result);
		assertEquals(1, callCount.get());
	}

	@Test
	void g03_orElseGet_nullSupplier() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertThrows(IllegalArgumentException.class, () -> supplier.orElseGet(null));
	}

	//====================================================================================================
	// orElseThrow()
	//====================================================================================================
	@Test
	void h01_orElseThrow_present() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertEquals("value", supplier.orElseThrow(() -> new RuntimeException("should not throw")));
	}

	@Test
	void h02_orElseThrow_empty() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		assertThrows(RuntimeException.class, () -> supplier.orElseThrow(() -> new RuntimeException("expected")));
	}

	@Test
	void h03_orElseThrow_nullSupplier() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertThrows(IllegalArgumentException.class, () -> supplier.orElseThrow(null));
	}

	//====================================================================================================
	// ifPresent()
	//====================================================================================================
	@Test
	void i01_ifPresent_present() {
		AtomicInteger callCount = new AtomicInteger();
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		supplier.ifPresent(s -> callCount.incrementAndGet());
		assertEquals(1, callCount.get());
	}

	@Test
	void i02_ifPresent_empty() {
		AtomicInteger callCount = new AtomicInteger();
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		supplier.ifPresent(s -> callCount.incrementAndGet());
		assertEquals(0, callCount.get());
	}

	@Test
	void i03_ifPresent_nullAction() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertThrows(IllegalArgumentException.class, () -> supplier.ifPresent(null));
	}

	//====================================================================================================
	// ifPresentOrElse()
	//====================================================================================================
	@Test
	void j01_ifPresentOrElse_present() {
		AtomicInteger presentCount = new AtomicInteger();
		AtomicInteger emptyCount = new AtomicInteger();
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		supplier.ifPresentOrElse(
			s -> presentCount.incrementAndGet(),
			() -> emptyCount.incrementAndGet()
		);
		assertEquals(1, presentCount.get());
		assertEquals(0, emptyCount.get());
	}

	@Test
	void j02_ifPresentOrElse_empty() {
		AtomicInteger presentCount = new AtomicInteger();
		AtomicInteger emptyCount = new AtomicInteger();
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		supplier.ifPresentOrElse(
			s -> presentCount.incrementAndGet(),
			() -> emptyCount.incrementAndGet()
		);
		assertEquals(0, presentCount.get());
		assertEquals(1, emptyCount.get());
	}

	@Test
	void j03_ifPresentOrElse_nullAction() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertThrows(IllegalArgumentException.class, () -> supplier.ifPresentOrElse(null, () -> {}));
	}

	@Test
	void j04_ifPresentOrElse_nullEmptyAction() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		assertThrows(IllegalArgumentException.class, () -> supplier.ifPresentOrElse(s -> {}, null));
	}

	//====================================================================================================
	// toOptional()
	//====================================================================================================
	@Test
	void k01_toOptional_present() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable("value");
		Optional<String> optional = supplier.toOptional();
		assertTrue(optional.isPresent());
		assertEquals("value", optional.get());
	}

	@Test
	void k02_toOptional_empty() {
		OptionalSupplier<String> supplier = OptionalSupplier.ofNullable(null);
		Optional<String> optional = supplier.toOptional();
		assertFalse(optional.isPresent());
	}
}

