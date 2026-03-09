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
package org.apache.juneau.cbor;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Low-level CBOR encoding verification against expected byte sequences (RFC 8949).
 */
@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in encoding verification
})
class CborOutputStream_Test extends TestBase {

	private static void enc(Object input, String expected) throws Exception {
		var b = CborSerializer.DEFAULT.serialize(input);
		assertEquals(expected, toSpacedHex(b));
	}

	@Test
	void a01_positiveFixint() throws Exception {
		for (int i = 0; i <= 23; i++)
			enc(i, String.format("%02X", i));
	}

	@Test
	void a02_uint8() throws Exception {
		enc(24, "18 18");
		enc(255, "18 FF");
	}

	@Test
	void a03_uint16() throws Exception {
		enc(256, "19 01 00");
		enc(65535, "19 FF FF");
	}

	@Test
	void a04_uint32() throws Exception {
		enc(65536, "1A 00 01 00 00");
		enc(0xFFFFFFFFL, "1A FF FF FF FF");
	}

	@Test
	void a05_uint64() throws Exception {
		enc(0x100000000L, "1B 00 00 00 01 00 00 00 00");
	}

	@Test
	void a06_negativeFixint() throws Exception {
		enc(-1, "20");
		enc(-24, "37");
	}

	@Test
	void a07_nint8() throws Exception {
		enc(-25, "38 18");
		enc(-256, "38 FF");
	}

	@Test
	void a08_nint16() throws Exception {
		enc(-257, "39 01 00");
		enc(-65536, "39 FF FF");
	}

	@Test
	void a09_nint32() throws Exception {
		enc(-65537, "3A 00 01 00 00");
	}

	@Test
	void a10_nint64() throws Exception {
		enc(Long.MIN_VALUE + 1, "3B 7F FF FF FF FF FF FF FE");
	}

	@Test
	void a11_integerBoundaries() throws Exception {
		enc(23, "17");
		enc(24, "18 18");
		enc(255, "18 FF");
		enc(256, "19 01 00");
		enc(65535, "19 FF FF");
		enc(65536, "1A 00 01 00 00");
	}

	@Test
	void a12_null() throws Exception {
		enc(null, "F6");
	}

	@Test
	void a13_true() throws Exception {
		enc(true, "F5");
	}

	@Test
	void a14_false() throws Exception {
		enc(false, "F4");
	}

	@Test
	void a15_float32() throws Exception {
		enc(0.0f, "FA 00 00 00 00");
		enc(1.0f, "FA 3F 80 00 00");
	}

	@Test
	void a16_float64() throws Exception {
		enc(0.0, "FB 00 00 00 00 00 00 00 00");
		enc(1.1, "FB 3F F1 99 99 99 99 99 9A");
	}

	@Test
	void a17_floatSpecial() throws Exception {
		enc(Float.POSITIVE_INFINITY, "FA 7F 80 00 00");
		enc(Float.NEGATIVE_INFINITY, "FA FF 80 00 00");
		enc(Double.POSITIVE_INFINITY, "FB 7F F0 00 00 00 00 00 00");
		enc(Double.NEGATIVE_INFINITY, "FB FF F0 00 00 00 00 00 00");
	}

	@Test
	void a18_emptyString() throws Exception {
		enc("", "60");
	}

	@Test
	void a19_shortString() throws Exception {
		enc("a", "61 61");
		enc("hello", "65 68 65 6C 6C 6F");
	}

	@Test
	void a20_mediumString() throws Exception {
		var s = "a".repeat(24);
		var b = CborSerializer.DEFAULT.serialize(s);
		assertEquals(26, b.length);
		assertEquals("78", String.format("%02X", b[0] & 0xFF));
		assertEquals(24, b[1] & 0xFF);
	}

	@Test
	void a21_longString() throws Exception {
		var s = "a".repeat(256);
		var b = CborSerializer.DEFAULT.serialize(s);
		assertEquals(259, b.length);
		assertEquals(0x79, b[0] & 0xFF);
		assertEquals(1, b[1] & 0xFF);
		assertEquals(0, b[2] & 0xFF);
	}

	@Test
	void a22_unicodeString() throws Exception {
		enc("\u00E9", "62 C3 A9");
		enc("\u20AC", "63 E2 82 AC");
	}

	@Test
	void a23_emptyBinary() throws Exception {
		enc(new byte[0], "40");
	}

	@Test
	void a24_shortBinary() throws Exception {
		enc(new byte[] { 1, 2, 3 }, "43 01 02 03");
	}

	@Test
	void a25_longBinary() throws Exception {
		var data = new byte[24];
		Arrays.fill(data, (byte) 0xFF);
		var b = CborSerializer.DEFAULT.serialize(data);
		assertEquals(26, b.length);
		assertEquals(0x58, b[0] & 0xFF);
		assertEquals(24, b[1] & 0xFF);
	}

	@Test
	void a26_emptyArray() throws Exception {
		enc(ints(), "80");
	}

	@Test
	void a27_shortArray() throws Exception {
		enc(ints(1, 2, 3), "83 01 02 03");
		enc(list("a", "b"), "82 61 61 61 62");
	}

	@Test
	void a28_longArray() throws Exception {
		var l = new ArrayList<Integer>();
		for (int i = 0; i < 24; i++)
			l.add(i);
		var b = CborSerializer.DEFAULT.serialize(l);
		assertTrue(toSpacedHex(b).startsWith("98 18"));
	}

	@Test
	void a29_emptyMap() throws Exception {
		enc(JsonMap.ofJson("{}"), "A0");
	}

	@Test
	void a30_shortMap() throws Exception {
		enc(JsonMap.ofJson("{\"a\":1,\"b\":2}"), "A2 61 61 01 61 62 02");
	}

	@Test
	void a31_longMap() throws Exception {
		var m = new LinkedHashMap<String, Integer>();
		for (int i = 0; i < 24; i++)
			m.put("k" + i, i);
		var b = CborSerializer.DEFAULT.serialize(m);
		assertTrue(toSpacedHex(b).startsWith("B8 18"));
	}
}
