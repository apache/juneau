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
package org.apache.juneau.marshall.protobuf;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.protobuf.ProtobufSerializer_Test.*;
import org.junit.jupiter.api.*;

/**
 * Bounds-check tests for the configurable wire-length maximum on {@link ProtobufParser}, mirroring the
 * BSON/MsgPack/CBOR coverage.
 *
 * <p>
 * A length-delimited block carries a varint length prefix used to size a {@code byte[]} before any payload
 * byte is read.  These tests confirm that an oversized (or wrapped-negative) declared length is rejected up
 * front rather than driving a large allocation.
 */
class ProtobufParser_MaxLength_Test extends TestBase {

	private static byte[] bytes(int...v) {
		var b = new byte[v.length];
		for (var i = 0; i < v.length; i++)
			b[i] = (byte)v[i];
		return b;
	}

	// ================================================================
	// Reader-level guard
	// ================================================================

	@Test
	void a01_oversizedDeclaredLengthRejected() {
		// Length-delimited block declaring 2^31-1 bytes (varint FF FF FF FF 07); a ~5-byte input must not
		// size a giant buffer.
		var r = new ProtobufReader(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0x07));
		assertThrowsWithMessage(IOException.class, "exceeds maximum", r::readLenDelimited);
	}

	@Test
	void a02_wrappedNegativeLengthRejected() {
		// A varint that decodes to a negative long (bit 63 set) must be rejected as negative rather than
		// truncating to a bogus positive int.
		var r = new ProtobufReader(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x01));
		assertThrowsWithMessage(IOException.class, "negative", r::readLenDelimited);
	}

	@Test
	void a03_configurableMaxLengthOnReader() throws Exception {
		// A 4-byte block parses under a generous cap but is rejected under a tiny cap.
		var lenient = new ProtobufReader(bytes(0x04, 0xAA, 0xBB, 0xCC, 0xDD));
		assertEquals(4, lenient.readLenDelimited().length);

		var strict = new ProtobufReader(bytes(0x04, 0xAA, 0xBB, 0xCC, 0xDD));
		strict.setMaxLength(2);
		assertThrowsWithMessage(IOException.class, "exceeds maximum", strict::readLenDelimited);
	}

	@Test
	void a04_maxLengthZeroDisablesCap() throws Exception {
		var r = new ProtobufReader(bytes(0x04, 0xAA, 0xBB, 0xCC, 0xDD));
		r.setMaxLength(0);
		assertEquals(4, r.readLenDelimited().length);
	}

	@Test
	void a05_subReaderInheritsMaxLength() throws Exception {
		// Outer block (length 2) is within the cap, but its inner varint declares 300, which exceeds the
		// propagated cap.  Without propagation the sub-reader would default to 16 MiB and mis-report.
		var outer = new ProtobufReader(bytes(0x02, 0xAC, 0x02));
		outer.setMaxLength(2);
		var sub = outer.readLenDelimitedReader();
		assertThrowsWithMessage(IOException.class, "exceeds maximum", sub::readLenDelimited);
	}

	// ================================================================
	// End-to-end parser
	// ================================================================

	@Test
	void b01_oversizedLenFieldRejectedEndToEnd() {
		// Field 2 (name, LEN/string) declaring a 2^31-1 byte length; a ~6-byte hostile payload must not OOM.
		assertThrowsWithMessage(IOException.class, "exceeds maximum",
			() -> ProtobufParser.DEFAULT.read(bytes(0x12, 0xFF, 0xFF, 0xFF, 0xFF, 0x07), Simple.class));
	}

	@Test
	void b02_legitimatePayloadUnaffected() throws Exception {
		// A normal small string field still parses under the default cap.
		var b = ProtobufParser.DEFAULT.read(bytes(0x12, 0x07, 0x74, 0x65, 0x73, 0x74, 0x69, 0x6E, 0x67), Simple.class);
		assertBean(b, "name", "testing");
	}

	// ================================================================
	// Configurable cap wiring
	// ================================================================

	@Test
	void c01_maxLengthAffectsCacheKey() {
		// Different maxLength values must NOT collide in the parser cache (hashKey wiring).
		var p1 = ProtobufParser.create().maxLength(100).build();
		var p2 = ProtobufParser.create().maxLength(200).build();
		var p3 = ProtobufParser.create().maxLength(100).build();
		assertNotSame(p1, p2);
		assertSame(p1, p3);
		assertEquals(100, p1.getMaxLength());
		assertEquals(200, p2.getMaxLength());
	}
}
