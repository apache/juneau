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
package org.apache.juneau.marshall.msgpack;

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
 * Bounds-check tests for the configurable wire-length maximum on {@link MsgPackParser}, mirroring the
 * BSON coverage in {@code BsonConformanceFixes_Test} (category <b>b</b>).
 *
 * <p>
 * A binary/string header carries a length prefix that is used to size a {@code byte[]} before any payload
 * byte is read.  These tests confirm that an oversized declared length is rejected up front rather than
 * driving a large allocation.
 */
@SuppressWarnings({
	"unchecked", // Parser returns Object; cast to Map in tests.
	"resource"   // Token readers are closed via try-with-resources; JDT mis-flags the chained factory call.
})
class MsgPackParser_MaxLength_Test extends TestBase {

	private static <T> T parse(MsgPackParser p, String spacedHex, Class<T> type) throws Exception {
		return p.read(fromSpacedHex(spacedHex), type);
	}

	// ================================================================
	// Default cap: a tiny input declaring an oversized length is rejected
	// ================================================================

	@Test
	void a01_binaryOversizedDeclaredLengthRejected() {
		// bin32 header declaring 16 MiB + 1 bytes (0x01000001); a ~5-byte input must not size a giant buffer.
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> parse(MsgPackParser.DEFAULT, "C6 01 00 00 01", Object.class));
	}

	@Test
	void a02_stringOversizedDeclaredLengthRejected() {
		// str32 header declaring 16 MiB + 1 bytes.
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> parse(MsgPackParser.DEFAULT, "DB 01 00 00 01", Object.class));
	}

	// ================================================================
	// Configurable cap
	// ================================================================

	@Test
	void b01_configurableMaxLengthEnforcedEndToEnd() throws Exception {
		// A document with a 32-byte binary parses with a generous cap but fails with a tiny cap.
		var bytes = MsgPackSerializer.DEFAULT.write(JsonMap.of("data", new byte[32]));

		var lenient = MsgPackParser.create().maxLength(1024).build();
		var parsed = (Map<String,Object>) lenient.read(bytes, Map.class, String.class, Object.class);
		assertEquals(32, ((byte[]) parsed.get("data")).length);

		var strict = MsgPackParser.create().maxLength(8).build();
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> strict.read(bytes, Map.class, String.class, Object.class));
	}

	@Test
	void b02_maxLengthAffectsCacheKey() {
		// Different maxLength values must NOT collide in the parser cache (hashKey wiring).
		var p1 = MsgPackParser.create().maxLength(100).build();
		var p2 = MsgPackParser.create().maxLength(200).build();
		var p3 = MsgPackParser.create().maxLength(100).build();
		assertNotSame(p1, p2);
		assertSame(p1, p3);
		assertEquals(100, p1.getMaxLength());
		assertEquals(200, p2.getMaxLength());
	}

	@Test
	void b03_maxLengthZeroDisablesCap() throws Exception {
		// A non-positive cap disables the max check.
		var bytes = MsgPackSerializer.DEFAULT.write(JsonMap.of("data", new byte[32]));
		var p = MsgPackParser.create().maxLength(0).build();
		var parsed = (Map<String,Object>) p.read(bytes, Map.class, String.class, Object.class);
		assertEquals(32, ((byte[]) parsed.get("data")).length);
	}

	// ================================================================
	// Element-COUNT caps apply only on the databind path (parity with CBOR); the O(1)-memory streaming
	// cursor is intentionally exempt.
	// ================================================================

	@Test
	void c01_arrayOversizedElementCountRejectedOnDatabind() {
		// array32 header declaring 16 MiB + 1 elements.  The databind path materializes a collection, so the
		// element count is bounded here.
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> parse(MsgPackParser.DEFAULT, "DD 01 00 00 01", Object.class));
	}

	@Test
	void c02_mapOversizedElementCountRejectedOnDatabind() {
		// map32 header declaring 16 MiB + 1 pairs.
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> parse(MsgPackParser.DEFAULT, "DF 01 00 00 01", Object.class));
	}

	@Test
	void c03_largeDefiniteArrayAcceptedOnStreamingCursor() throws Exception {
		// The same oversized array32 header is accepted on the O(1)-memory streaming cursor, which never
		// materializes the container (regression guard: the count cap must not leak onto the streaming path).
		try (var r = MsgPackParser.DEFAULT.readTokens(fromSpacedHex("DD 01 00 00 01"))) {
			assertEquals(TokenType.START_ARRAY, r.next());
		}
	}
}
