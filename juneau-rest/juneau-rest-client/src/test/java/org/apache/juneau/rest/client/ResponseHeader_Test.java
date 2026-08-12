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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ResponseHeader}.
 */
class ResponseHeader_Test extends TestBase {

	@SuppressWarnings({
		"resource" // Factory returns a Closeable for the caller to close; Eclipse JDT @Owning warning is by design.
	})
	private static RestResponse response(TransportResponse.Builder b) {
		return new RestResponse(b.build(), null);
	}

	// ==========================================================================
	// a — presence + single-value accessors
	// ==========================================================================

	@Test void a01_isPresent_true() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Foo", "bar"))) {
			assertTrue(resp.header("x-foo").isPresent());
		}
	}

	@Test void a02_isPresent_false() throws Exception {
		try (var resp = response(TransportResponse.builder())) {
			assertFalse(resp.header("X-Foo").isPresent());
		}
	}

	@Test void a03_getName() throws Exception {
		try (var resp = response(TransportResponse.builder())) {
			assertEquals("X-Foo", resp.header("X-Foo").getName());
		}
	}

	@Test void a04_getValue_presentAndAbsent() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Foo", "bar"))) {
			assertEquals("bar", resp.header("X-Foo").getValue());
			assertNull(resp.header("X-Missing").getValue());
		}
	}

	@Test void a05_orElse_presentAndAbsent() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Foo", "bar"))) {
			assertEquals("bar", resp.header("X-Foo").orElse("default"));
			assertEquals("default", resp.header("X-Missing").orElse("default"));
		}
	}

	@Test void a06_asOptional_presentAndAbsent() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Foo", "bar"))) {
			assertEquals(Optional.of("bar"), resp.header("X-Foo").asOptional());
			assertEquals(Optional.empty(), resp.header("X-Missing").asOptional());
		}
	}

	// ==========================================================================
	// b — numeric accessors
	// ==========================================================================

	@Test void b01_asInteger_valid() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Count", " 42 "))) {
			assertEquals(42, resp.header("X-Count").asInteger());
		}
	}

	@Test void b02_asInteger_absentOrUnparseable() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Count", "not-a-number"))) {
			assertEquals(-1, resp.header("X-Count").asInteger());
			assertEquals(-1, resp.header("X-Missing").asInteger());
		}
	}

	@Test void b03_asLong_valid() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Count", " 9999999999 "))) {
			assertEquals(9999999999L, resp.header("X-Count").asLong());
		}
	}

	@Test void b04_asLong_absentOrUnparseable() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Count", "not-a-number"))) {
			assertEquals(-1L, resp.header("X-Count").asLong());
			assertEquals(-1L, resp.header("X-Missing").asLong());
		}
	}

	// ==========================================================================
	// c — getValues() (repeated headers)
	// ==========================================================================

	@Test void c01_getValues_matchesOnlySameNameCaseInsensitively() throws Exception {
		try (var resp = response(TransportResponse.builder()
				.header("X-Tag", "a")
				.header("X-Other", "z")
				.header("x-tag", "b"))) {
			assertEquals(List.of("a", "b"), resp.header("X-Tag").getValues());
		}
	}

	@Test void c02_getValues_absentIsEmptyList() throws Exception {
		try (var resp = response(TransportResponse.builder())) {
			assertEquals(List.of(), resp.header("X-Missing").getValues());
		}
	}

	// ==========================================================================
	// d — asCsvList()
	// ==========================================================================

	@Test void d01_asCsvList_splitsAndTrims() throws Exception {
		try (var resp = response(TransportResponse.builder().header("Accept", "text/html, application/json ,text/plain"))) {
			assertEquals(List.of("text/html", "application/json", "text/plain"), resp.header("Accept").asCsvList());
		}
	}

	@Test void d02_asCsvList_absentIsEmptyList() throws Exception {
		try (var resp = response(TransportResponse.builder())) {
			assertEquals(List.of(), resp.header("X-Missing").asCsvList());
		}
	}

	@Test void d03_asCsvList_emptyTokensAreFilteredOut() throws Exception {
		// A trailing/doubled comma yields a blank-after-trim token that must be dropped, not returned as "".
		try (var resp = response(TransportResponse.builder().header("X-List", "a,,b,"))) {
			assertEquals(List.of("a", "b"), resp.header("X-List").asCsvList());
		}
	}

	// ==========================================================================
	// e — toString()
	// ==========================================================================

	@Test void e01_toString_presentAndAbsent() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Foo", "bar"))) {
			assertEquals("X-Foo: bar", resp.header("X-Foo").toString());
			assertEquals("X-Missing: <absent>", resp.header("X-Missing").toString());
		}
	}
}
