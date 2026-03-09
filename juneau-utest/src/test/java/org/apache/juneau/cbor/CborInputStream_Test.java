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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * CBOR decoding verification for each encoding tier.
 */
class CborInputStream_Test extends TestBase {

	@Test
	void b01_decodePositiveIntegers() throws Exception {
		assertEquals(0, CborParser.DEFAULT.parse(hex("00"), Integer.class));
		assertEquals(23, CborParser.DEFAULT.parse(hex("17"), Integer.class));
		assertEquals(24, CborParser.DEFAULT.parse(hex("18 18"), Integer.class));
		assertEquals(255, CborParser.DEFAULT.parse(hex("18 FF"), Integer.class));
		assertEquals(256, CborParser.DEFAULT.parse(hex("19 01 00"), Integer.class));
		assertEquals(65535, CborParser.DEFAULT.parse(hex("19 FF FF"), Integer.class));
		assertEquals(65536, CborParser.DEFAULT.parse(hex("1A 00 01 00 00"), Long.class));
		assertEquals(0xFFFFFFFFL, CborParser.DEFAULT.parse(hex("1A FF FF FF FF"), Long.class));
	}

	@Test
	void b02_decodeNegativeIntegers() throws Exception {
		assertEquals(-1, CborParser.DEFAULT.parse(hex("20"), Integer.class));
		assertEquals(-24, CborParser.DEFAULT.parse(hex("37"), Integer.class));
		assertEquals(-25, CborParser.DEFAULT.parse(hex("38 18"), Integer.class));
		assertEquals(-256, CborParser.DEFAULT.parse(hex("38 FF"), Integer.class));
		assertEquals(-257, CborParser.DEFAULT.parse(hex("39 01 00"), Integer.class));
	}

	@Test
	void b03_decodeBoolean() throws Exception {
		assertEquals(false, CborParser.DEFAULT.parse(hex("F4"), Boolean.class));
		assertEquals(true, CborParser.DEFAULT.parse(hex("F5"), Boolean.class));
	}

	@Test
	void b04_decodeNull() throws Exception {
		assertNull(CborParser.DEFAULT.parse(hex("F6"), Object.class));
	}

	@Test
	void b05_decodeFloat32() throws Exception {
		assertEquals(0.0f, CborParser.DEFAULT.parse(hex("FA 00 00 00 00"), Float.class));
		assertEquals(1.0f, CborParser.DEFAULT.parse(hex("FA 3F 80 00 00"), Float.class));
	}

	@Test
	void b06_decodeFloat64() throws Exception {
		assertEquals(0.0, CborParser.DEFAULT.parse(hex("FB 00 00 00 00 00 00 00 00"), Double.class));
		assertEquals(1.1, CborParser.DEFAULT.parse(hex("FB 3F F1 99 99 99 99 99 9A"), Double.class));
	}

	@Test
	void b07_decodeStrings() throws Exception {
		assertEquals("", CborParser.DEFAULT.parse(hex("60"), String.class));
		assertEquals("a", CborParser.DEFAULT.parse(hex("61 61"), String.class));
		var s24 = "x".repeat(24);
		assertEquals(s24, CborParser.DEFAULT.parse(CborSerializer.DEFAULT.serialize(s24), String.class));
	}

	@Test
	void b08_decodeBinary() throws Exception {
		assertArrayEquals(new byte[0], CborParser.DEFAULT.parse(hex("40"), byte[].class));
		assertArrayEquals(new byte[] { 1, 2, 3 }, CborParser.DEFAULT.parse(hex("43 01 02 03"), byte[].class));
	}

	@Test
	void b09_decodeArrays() throws Exception {
		var empty = CborParser.DEFAULT.parse(hex("80"), JsonList.class);
		assertNotNull(empty);
		assertTrue(empty.isEmpty());
		var arr = CborParser.DEFAULT.parse(hex("83 01 02 03"), JsonList.class);
		assertEquals(3, arr.size());
		assertEquals(1, arr.getInt(0));
		assertEquals(2, arr.getInt(1));
		assertEquals(3, arr.getInt(2));
	}

	@Test
	void b10_decodeMaps() throws Exception {
		var m = CborParser.DEFAULT.parse(hex("A0"), JsonMap.class);
		assertTrue(m.isEmpty());
		var m2 = CborParser.DEFAULT.parse(hex("A2 61 61 01 61 62 02"), JsonMap.class);
		assertEquals(1, m2.getInt("a"));
		assertEquals(2, m2.getInt("b"));
	}

	@Test
	void b11_decodeUnicodeStrings() throws Exception {
		assertEquals("\u00E9", CborParser.DEFAULT.parse(hex("62 C3 A9"), String.class));
		assertEquals("\u20AC", CborParser.DEFAULT.parse(hex("63 E2 82 AC"), String.class));
	}

	@Test
	void b12_dataTypeDetection() throws Exception {
		assertEquals(0, CborInputStream.getMajorType(0x00));
		assertEquals(1, CborInputStream.getMajorType(0x20));
		assertEquals(3, CborInputStream.getMajorType(0x61));
		assertEquals(4, CborInputStream.getMajorType(0x80));
		assertEquals(5, CborInputStream.getMajorType(0xA0));
		assertEquals(7, CborInputStream.getMajorType(0xF6));
		assertEquals(22, CborInputStream.getAdditionalInfo(0xF6));
	}

	private static byte[] hex(String spaced) {
		return fromHex(spaced.replace(" ", ""));
	}
}
