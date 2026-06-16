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

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Low-level MsgPack {@code writeExt} framing verification (fixext / ext8 / ext16 / ext32 by length).
 */
class MsgPackOutputStream_Test extends TestBase {

	@SuppressWarnings("resource") // writeExt returns the stream (fluent this); the discarded value is os, closed by the try-with-resources.
	private static String enc(int type, byte[] payload) throws IOException {
		var bos = new ByteArrayOutputStream();
		try (var os = new MsgPackOutputStream(bos)) {
			os.writeExt(type, payload);
		}
		return toSpacedHex(bos.toByteArray());
	}

	@Test
	void a01_fixext1() throws Exception {
		assertEquals("D4 05 7F", enc(5, new byte[]{0x7F}));
	}

	@Test
	void a02_fixext2() throws Exception {
		assertEquals("D5 05 01 02", enc(5, new byte[]{1, 2}));
	}

	@Test
	void a03_fixext4() throws Exception {
		assertEquals("D6 05 01 02 03 04", enc(5, new byte[]{1, 2, 3, 4}));
	}

	@Test
	void a04_fixext8() throws Exception {
		assertEquals("D7 05 01 02 03 04 05 06 07 08", enc(5, new byte[]{1, 2, 3, 4, 5, 6, 7, 8}));
	}

	@Test
	void a05_fixext16() throws Exception {
		var p = new byte[16];
		for (var i = 0; i < 16; i++) p[i] = (byte) i;
		assertTrue(enc(5, p).startsWith("D8 05 "));
	}

	@Test
	void a06_ext8_threeBytes() throws Exception {
		// 3 bytes is not a fixext length, so ext8 framing applies.
		assertEquals("C7 03 05 01 02 03", enc(5, new byte[]{1, 2, 3}));
	}

	@Test
	void a07_ext16_257Bytes() throws Exception {
		var p = new byte[257];
		var hex = enc(5, p);
		// C8 = ext16, length is 0x0101, type 0x05, then 257 zero bytes.
		assertTrue(hex.startsWith("C8 01 01 05 "));
	}

	@Test
	void a08_negativeExtType() throws Exception {
		// Type byte is signed; -1 must serialize as 0xFF.
		assertEquals("D4 FF 7F", enc(-1, new byte[]{0x7F}));
	}

	@Test
	void a09_largeNegativeExtType() throws Exception {
		// -128 must serialize as 0x80.
		assertEquals("D4 80 00", enc(-128, new byte[]{0x00}));
	}
}
