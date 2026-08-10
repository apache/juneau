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
package org.apache.juneau.msgpack;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Bounds-check tests for the wire-length maximum enforced by {@link MsgPackInputStream}.
 *
 * <p>
 * A binary/string header carries a length prefix that is used to size a {@code byte[]} before any payload
 * byte is read.  These tests confirm that an oversized declared length is rejected up front rather than
 * driving a large allocation.
 */
class MsgPackParser_MaxLength_Test extends TestBase {

	private static InputStream is(String spacedHex) {
		return new ByteArrayInputStream(fromSpacedHex(spacedHex));
	}

	@Test void a01_binaryOversizedDeclaredLengthRejected() {
		// bin32 header (0xC6) declaring 16 MiB + 1 bytes (0x01000001); a ~5-byte input must not size a giant buffer.
		assertThrowsWithMessage(Exception.class, "exceeds maximum",
			() -> MsgPackParser.DEFAULT.parse(is("C6 01 00 00 01"), Object.class));
	}

	@Test void a02_stringOversizedDeclaredLengthRejected() {
		// str32 header (0xDB) declaring 16 MiB + 1 bytes.
		assertThrowsWithMessage(Exception.class, "exceeds maximum",
			() -> MsgPackParser.DEFAULT.parse(is("DB 01 00 00 01"), Object.class));
	}

	@Test void a03_binaryWithinCapAccepted() throws Exception {
		// bin8 header (0xC4) declaring 2 bytes, followed by the 2 payload bytes; parses normally within the cap.
		var r = MsgPackParser.DEFAULT.parse(is("C4 02 61 62"), Object.class);
		assertNotNull(r);
	}
}
