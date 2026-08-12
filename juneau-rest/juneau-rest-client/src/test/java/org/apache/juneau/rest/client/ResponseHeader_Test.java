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

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.*;
import org.apache.juneau.http.header.*;
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

	// ==========================================================================
	// f — typed-wrapper convenience accessors (mirror classic asXxxHeader() family)
	// ==========================================================================

	@Test void f01_asBoolean_valid() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Flag", " true "))) {
			assertEquals(Optional.of(true), resp.header("X-Flag").asBoolean());
		}
	}

	@Test void f02_asBoolean_absentIsFalseValued() throws Exception {
		try (var resp = response(TransportResponse.builder().header("X-Flag", "not-a-boolean"))) {
			// Boolean.parseBoolean() treats any non-"true" value as false rather than throwing.
			assertEquals(Optional.of(false), resp.header("X-Flag").asBoolean());
			assertEquals(Optional.empty(), resp.header("X-Missing").asBoolean());
		}
	}

	@Test void f03_asZonedDateTime_valid() throws Exception {
		try (var resp = response(TransportResponse.builder().header("Last-Modified", "Sun, 06 Nov 1994 08:49:37 GMT"))) {
			var zdt = resp.header("Last-Modified").asZonedDateTime();
			assertTrue(zdt.isPresent());
			assertEquals(1994, zdt.get().getYear());
		}
	}

	@Test void f04_asZonedDateTime_absentOrUnparseable() throws Exception {
		try (var resp = response(TransportResponse.builder().header("Last-Modified", "not-a-date"))) {
			assertEquals(Optional.empty(), resp.header("Last-Modified").asZonedDateTime());
			assertEquals(Optional.empty(), resp.header("X-Missing").asZonedDateTime());
		}
	}

	@Test void f05_asUri_valid() throws Exception {
		try (var resp = response(TransportResponse.builder().header("Location", "http://example.com/foo"))) {
			assertEquals(Optional.of(URI.create("http://example.com/foo")), resp.header("Location").asUri());
		}
	}

	@Test void f06_asUri_absentOrUnparseable() throws Exception {
		try (var resp = response(TransportResponse.builder().header("Location", "http://[bad"))) {
			assertEquals(Optional.empty(), resp.header("Location").asUri());
			assertEquals(Optional.empty(), resp.header("X-Missing").asUri());
		}
	}

	@Test void f07_asEntityTag_valid() throws Exception {
		try (var resp = response(TransportResponse.builder().header("ETag", "\"abc123\""))) {
			var et = resp.header("ETag").asEntityTag();
			assertTrue(et.isPresent());
			assertEquals("abc123", et.get().getEntityValue());
		}
	}

	@Test void f08_asEntityTag_absentOrUnparseable() throws Exception {
		try (var resp = response(TransportResponse.builder().header("ETag", "unquoted"))) {
			assertEquals(Optional.empty(), resp.header("ETag").asEntityTag());
			assertEquals(Optional.empty(), resp.header("X-Missing").asEntityTag());
		}
	}

	@Test void f09_asEntityTags_valid() throws Exception {
		try (var resp = response(TransportResponse.builder().header("If-Match", "\"a\", \"b\""))) {
			assertEquals(2, resp.header("If-Match").asEntityTags().toList().size());
		}
	}

	@Test void f10_asEntityTags_absentIsEmpty() throws Exception {
		try (var resp = response(TransportResponse.builder())) {
			assertSame(EntityTags.EMPTY, resp.header("X-Missing").asEntityTags());
		}
	}

	@Test void f11_asStringRanges_valid() throws Exception {
		try (var resp = response(TransportResponse.builder().header("Accept-Encoding", "gzip;q=1.0, identity;q=0.5"))) {
			var ranges = resp.header("Accept-Encoding").asStringRanges();
			assertEquals(2, ranges.toList().size());
		}
	}

	@Test void f12_asStringRanges_absentIsEmpty() throws Exception {
		try (var resp = response(TransportResponse.builder())) {
			assertSame(StringRanges.EMPTY, resp.header("X-Missing").asStringRanges());
		}
	}
}
