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
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link TransportRequest}.
 */
class TransportRequest_Test extends TestBase {

	private static TransportBody repeatableBody() {
		// SerializerBody re-serializes on every writeTo(...), so it is always repeatable.
		return TransportBody.of(SerializerBody.of(JsonSerializer.DEFAULT, "x"));
	}

	private static TransportBody nonRepeatableBody() {
		// RecordStreamBody defaults to non-repeatable (single-shot streaming) unless opted in via repeatable().
		return TransportBody.of(RecordStreamBody.records(w -> {}));
	}

	// ==========================================================================
	// a — builder + accessors
	// ==========================================================================

	@Test void a01_builder_setsAllFields() {
		var body = repeatableBody();
		var timeout = Duration.ofSeconds(5);
		var req = TransportRequest.builder()
			.method("POST")
			.uri(URI.create("https://example.com/api"))
			.header("X-Foo", "bar")
			.body(body)
			.timeout(timeout)
			.build();
		assertEquals("POST", req.getMethod());
		assertEquals(URI.create("https://example.com/api"), req.getUri());
		assertEquals("bar", req.getFirstHeader("x-foo").value());
		assertSame(body, req.getBody());
		assertEquals(timeout, req.getTimeout());
	}

	@Test void a02_builder_uriFromString() {
		var req = TransportRequest.builder().method("GET").uri("https://example.com/x").build();
		assertEquals(URI.create("https://example.com/x"), req.getUri());
	}

	@Test void a03_builder_uriFromString_invalidThrows() {
		assertThrows(IllegalArgumentException.class, () -> TransportRequest.builder().method("GET").uri("http://[bad"));
	}

	@Test void a04_builder_headersCollection() {
		var req = TransportRequest.builder().method("GET").uri("https://example.com")
			.headers(List.of(TransportHeader.of("A", "1"), TransportHeader.of("B", "2")))
			.build();
		assertEquals(2, req.getHeaders().size());
	}

	@Test void a05_getFirstHeader_missingReturnsNull() {
		var req = TransportRequest.builder().method("GET").uri("https://example.com").build();
		assertNull(req.getFirstHeader("X-Missing"));
	}

	@Test void a06_noBodyOrTimeout_defaultsToNull() {
		var req = TransportRequest.builder().method("GET").uri("https://example.com").build();
		assertNull(req.getBody());
		assertNull(req.getTimeout());
	}

	@Test void a07_builder_requiresMethod() {
		assertThrows(IllegalArgumentException.class, () -> TransportRequest.builder().uri("https://example.com").build());
	}

	@Test void a08_builder_requiresUri() {
		assertThrows(IllegalArgumentException.class, () -> TransportRequest.builder().method("GET").build());
	}

	// ==========================================================================
	// b — isSafeToReplay(): idempotent method ∧ (no body ∨ repeatable body)
	// ==========================================================================

	@Test void b01_idempotentMethod_noBody_isSafe() {
		var req = TransportRequest.builder().method("GET").uri("https://example.com").build();
		assertTrue(req.isSafeToReplay());
	}

	@Test void b02_idempotentMethod_repeatableBody_isSafe() {
		var req = TransportRequest.builder().method("PUT").uri("https://example.com").body(repeatableBody()).build();
		assertTrue(req.isSafeToReplay());
	}

	@Test void b03_idempotentMethod_nonRepeatableBody_isNotSafe() {
		var req = TransportRequest.builder().method("DELETE").uri("https://example.com").body(nonRepeatableBody()).build();
		assertFalse(req.isSafeToReplay());
	}

	@Test void b04_nonIdempotentMethod_noBody_isNotSafe() {
		var req = TransportRequest.builder().method("POST").uri("https://example.com").build();
		assertFalse(req.isSafeToReplay());
	}

	@Test void b05_nonIdempotentMethod_repeatableBody_isNotSafe() {
		var req = TransportRequest.builder().method("POST").uri("https://example.com").body(repeatableBody()).build();
		assertFalse(req.isSafeToReplay());
	}

	@Test void b06_methodMatchedCaseInsensitively() {
		// IDEMPOTENT_METHODS is upper-case; a lowercase method must still match via toUpperCase(...).
		var req = TransportRequest.builder().method("get").uri("https://example.com").build();
		assertTrue(req.isSafeToReplay());
	}

	@Test void b07_headOptionsTraceAreIdempotent() {
		for (var m : List.of("HEAD", "OPTIONS", "TRACE"))
			assertTrue(TransportRequest.builder().method(m).uri("https://example.com").build().isSafeToReplay(), m);
	}
}
