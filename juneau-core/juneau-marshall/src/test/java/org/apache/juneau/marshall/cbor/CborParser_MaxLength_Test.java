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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Bounds-check tests for the configurable wire-length maximum on {@link CborParser}, mirroring the BSON
 * coverage in {@code BsonConformanceFixes_Test} (category <b>b</b>).
 *
 * <p>
 * A byte/text-string header carries a length prefix used to size a {@code byte[]} before any payload byte
 * is read, and an array/map header carries an element count that drives a container loop.  These tests
 * confirm that an oversized declared length or count is rejected up front rather than driving a large
 * allocation.
 */
@SuppressWarnings({
	"unchecked", // Parser returns Object; cast to Map in tests.
	"resource"   // Token readers are closed via try-with-resources; JDT mis-flags the chained factory call.
})
class CborParser_MaxLength_Test extends TestBase {

	// ================================================================
	// Default cap: a tiny input declaring an oversized length is rejected
	// ================================================================

	@Test
	void a01_binaryOversizedDeclaredLengthRejected() {
		// Byte string (major type 2) with a 4-byte length declaring 16 MiB + 1; a ~5-byte input must not
		// size a giant buffer.
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> CborParser.DEFAULT.read(fromSpacedHex("5A 01 00 00 01"), Object.class));
	}

	@Test
	void a02_stringOversizedDeclaredLengthRejected() {
		// Text string (major type 3) with a 4-byte length declaring 16 MiB + 1.
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> CborParser.DEFAULT.read(fromSpacedHex("7A 01 00 00 01"), Object.class));
	}

	@Test
	void a03_arrayOversizedElementCountRejected() {
		// Array (major type 4) with a 4-byte element count declaring 16 MiB + 1.
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> CborParser.DEFAULT.read(fromSpacedHex("9A 01 00 00 01"), Object.class));
	}

	@Test
	void a04_mapOversizedElementCountRejected() {
		// Map (major type 5) with a 4-byte element count declaring 16 MiB + 1.
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> CborParser.DEFAULT.read(fromSpacedHex("BA 01 00 00 01"), Object.class));
	}

	@Test
	void a05_arrayNegativeElementCountRejected() {
		// Array with an 8-byte count of 0xFFFFFFFFFFFFFFFF (beyond int range) is rejected as negative.
		assertThrowsWithMessage(IOException.class, "negative",
			() -> CborParser.DEFAULT.read(fromSpacedHex("9B FF FF FF FF FF FF FF FF"), Object.class));
	}

	// ================================================================
	// Configurable cap
	// ================================================================

	@Test
	void b01_configurableMaxLengthEnforcedEndToEnd() throws Exception {
		// A document with a 32-byte binary parses with a generous cap but fails with a tiny cap.
		var bytes = CborSerializer.DEFAULT.write(JsonMap.of("data", new byte[32]));

		var lenient = CborParser.create().maxLength(1024).build();
		var parsed = (Map<String,Object>) lenient.read(bytes, Map.class, String.class, Object.class);
		assertEquals(32, ((byte[]) parsed.get("data")).length);

		var strict = CborParser.create().maxLength(8).build();
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> strict.read(bytes, Map.class, String.class, Object.class));
	}

	@Test
	void b02_maxLengthAffectsCacheKey() {
		// Different maxLength values must NOT collide in the parser cache (hashKey wiring).
		var p1 = CborParser.create().maxLength(100).build();
		var p2 = CborParser.create().maxLength(200).build();
		var p3 = CborParser.create().maxLength(100).build();
		assertNotSame(p1, p2);
		assertSame(p1, p3);
		assertEquals(100, p1.getMaxLength());
		assertEquals(200, p2.getMaxLength());
	}

	@Test
	void b03_maxLengthZeroDisablesCap() throws Exception {
		// A non-positive cap disables the max check.
		var bytes = CborSerializer.DEFAULT.write(JsonMap.of("data", new byte[32]));
		var p = CborParser.create().maxLength(0).build();
		var parsed = (Map<String,Object>) p.read(bytes, Map.class, String.class, Object.class);
		assertEquals(32, ((byte[]) parsed.get("data")).length);
	}

	// ================================================================
	// Streaming-cursor policy: element-COUNT caps apply only on the databind path; byte/text-string
	// LENGTH caps apply on every path (including the O(1)-memory streaming cursor).
	// ================================================================

	@Test
	void c01_largeDefiniteArrayAcceptedOnStreamingCursor() throws Exception {
		// Array (major type 4) with a 4-byte element count declaring 16 MiB + 1.  The databind path rejects
		// this (see a03), but the O(1)-memory streaming cursor never materializes the container, so it must
		// still emit START_ARRAY without allocating anything of that size.
		try (var r = CborParser.DEFAULT.readTokens(fromSpacedHex("9A 01 00 00 01"))) {
			assertEquals(TokenType.START_ARRAY, r.next());
		}
	}

	@Test
	void c02_indefiniteStringOversizedInAggregateRejected() {
		// Indefinite-length byte string (0x5F) whose individual chunks are each within the cap but whose
		// reassembled total exceeds it must be rejected in aggregate rather than growing without bound.
		var p = CborParser.create().maxLength(4).build();
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> p.read(fromSpacedHex("5F 43 01 02 03 43 04 05 06 FF"), Object.class));
	}

	@Test
	void c03_stringLengthCapHonoredOnStreamingCursor() throws Exception {
		// The configured cap must thread through to the token/streaming path (the cursor builds its own
		// input stream).  A 32-byte byte string declared under an 8-byte cap is rejected before any payload
		// byte is read.
		var p = CborParser.create().maxLength(8).build();
		try (var r = p.readTokens(fromSpacedHex("58 20"))) {
			assertThrowsWithMessage(IOException.class, "exceeds maximum", r::next);
		}
	}

	@Test
	void c04_definiteByteStringWithAllOnesLengthRejected() {
		// Definite-length byte string (major type 2) with an 8-byte argument 0xFFFFFFFFFFFFFFFF must be
		// rejected as an out-of-range (negative) length, NOT mistaken for an indefinite-length string.
		assertThrowsWithMessage(IOException.class, "negative",
			() -> CborParser.DEFAULT.read(fromSpacedHex("5B FF FF FF FF FF FF FF FF"), Object.class));
	}
}
