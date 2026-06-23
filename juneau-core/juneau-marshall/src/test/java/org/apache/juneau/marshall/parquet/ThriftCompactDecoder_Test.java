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
package org.apache.juneau.marshall.parquet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for {@link ThriftCompactDecoder}.
 */
@SuppressWarnings({
	"java:S5778" // assertThrows lambdas with chained calls; intermediate calls do not throw in practice.
})
class ThriftCompactDecoder_Test {

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static ThriftCompactDecoder dec(byte... data) {
		return new ThriftCompactDecoder(data);
	}

	/** Encode an unsigned varint into bytes (little-endian 7-bit groups). */
	private static byte[] varint(long v) {
		var out = new ByteArrayOutputStream();
		while (true) {
			var b = (int)(v & 0x7F);
			v >>>= 7;
			if (v != 0) {
				out.write(b | 0x80);
			} else {
				out.write(b);
				break;
			}
		}
		return out.toByteArray();
	}

	private static byte[] concat(byte[]... parts) {
		int total = 0;
		for (var p : parts) total += p.length;
		var result = new byte[total];
		int pos = 0;
		for (var p : parts) {
			System.arraycopy(p, 0, result, pos, p.length);
			pos += p.length;
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// a — readFieldHeader
	// -----------------------------------------------------------------------

	/** EOF (b < 0) returns isStop=true. */
	@Test void a01_readFieldHeader_eof_returns_stop() throws IOException {
		var fh = dec().readFieldHeader();
		assertTrue(fh.isStop);
	}

	/** Byte == 0 returns isStop=true. */
	@Test void a02_readFieldHeader_zero_returns_stop() throws IOException {
		var fh = dec((byte)0).readFieldHeader();
		assertTrue(fh.isStop);
		assertEquals(0, fh.type);
	}

	/** delta != 0: short form (delta encoded in high nibble). */
	@Test void a03_readFieldHeader_delta_encoding() throws IOException {
		// byte = (delta << 4) | type; delta=1, type=BOOLEAN_TRUE(1) → byte=0x11
		var fh = dec((byte)0x11).readFieldHeader();
		assertFalse(fh.isStop);
		assertEquals(ThriftCompactEncoder.BOOLEAN_TRUE, fh.type);
		assertEquals(1, fh.fieldId);
	}

	/** delta == 0: full field-id varint follows. */
	@Test void a04_readFieldHeader_full_fieldid() throws IOException {
		// type=I32(5) in low nibble, delta=0 in high nibble; then varint fieldId=42
		var d = dec((byte)ThriftCompactEncoder.I32, (byte)42);
		var fh = d.readFieldHeader();
		assertFalse(fh.isStop);
		assertEquals(ThriftCompactEncoder.I32, fh.type);
		assertEquals(42, fh.fieldId);
	}

	// -----------------------------------------------------------------------
	// b — readBool
	// -----------------------------------------------------------------------

	@Test void b01_readBool_true() throws IOException {
		assertTrue(dec((byte)ThriftCompactEncoder.BOOLEAN_TRUE).readBool());
	}

	@Test void b02_readBool_false() throws IOException {
		assertFalse(dec((byte)ThriftCompactEncoder.BOOLEAN_FALSE).readBool());
	}

	// -----------------------------------------------------------------------
	// c — skipField: BOOLEAN_TRUE / BOOLEAN_FALSE (no bytes consumed)
	// -----------------------------------------------------------------------

	@Test void c01_skipField_booleanTrue() {
		var d = dec();
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.BOOLEAN_TRUE));
	}

	@Test void c02_skipField_booleanFalse() {
		var d = dec();
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.BOOLEAN_FALSE));
	}

	// -----------------------------------------------------------------------
	// d — skipField: I8
	// -----------------------------------------------------------------------

	@Test void d01_skipField_i8() throws IOException {
		var d = dec((byte)0x42);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.I8));
		assertEquals(0, d.available());
	}

	// -----------------------------------------------------------------------
	// e — skipField: I16 / I32 / I64
	// -----------------------------------------------------------------------

	@Test void e01_skipField_i16() throws IOException {
		// varint for value 10: single byte 10
		var d = dec((byte)10);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.I16));
		assertEquals(0, d.available());
	}

	@Test void e02_skipField_i32() {
		var d = dec((byte)100);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.I32));
	}

	@Test void e03_skipField_i64() {
		var d = dec((byte)0x81, (byte)0x01); // varint 129
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.I64));
	}

	// -----------------------------------------------------------------------
	// f — skipField: DOUBLE (8 bytes)
	// -----------------------------------------------------------------------

	@Test void f01_skipField_double() throws IOException {
		var d = dec((byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.DOUBLE));
		assertEquals(0, d.available());
	}

	// -----------------------------------------------------------------------
	// g — skipField: BINARY
	// -----------------------------------------------------------------------

	@Test void g01_skipField_binary_normal() throws IOException {
		// varint length=3, then 3 payload bytes
		var data = concat(varint(3), new byte[]{(byte)'a',(byte)'b',(byte)'c'});
		var d = new ThriftCompactDecoder(data);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.BINARY));
		assertEquals(0, d.available());
	}

	@Test void g02_skipField_binary_zero_length() {
		var data = varint(0);
		var d = new ThriftCompactDecoder(data);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.BINARY));
	}

	@Test void g03_skipField_binary_too_large_throws() {
		// varint decodes to a value > MAX_BINARY_LENGTH (64 MB)
		// 0xFF 0xFF 0xFF 0xFF 0x1F = 0x1FFFFFFF = 536_870_911 > 67_108_864
		var data = new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0x1F};
		var d = new ThriftCompactDecoder(data);
		assertThrows(IOException.class, () -> d.skipField(ThriftCompactEncoder.BINARY));
	}

	@Test void g04_skipField_binary_negative_long_throws() {
		// varint with bit 63 set → len is a negative long → len < 0 branch
		// 9 continuation bytes (0x80) then 0x01 → bit 63 set = Long.MIN_VALUE
		var data = new byte[]{
			(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x80,
			(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x01
		};
		var d = new ThriftCompactDecoder(data);
		assertThrows(IOException.class, () -> d.skipField(ThriftCompactEncoder.BINARY));
	}

	// -----------------------------------------------------------------------
	// h — skipField: LIST / SET
	// -----------------------------------------------------------------------

	@Test void h01_skipField_list_eof() {
		// read() returns -1 immediately → b < 0 branch
		var d = dec();
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.LIST));
	}

	@Test void h02_skipField_list_short_form() throws IOException {
		// size=2 in high nibble, elemType=I8(3): byte=(2<<4)|3=0x23; then 2 I8 data bytes
		var d = dec((byte)0x23, (byte)1, (byte)2);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.LIST));
		assertEquals(0, d.available());
	}

	@Test void h03_skipField_list_long_form() throws IOException {
		// size==15 → read varint for size; byte=(15<<4)|I8=0xF3, varint 3, then 3 I8 bytes
		var data = concat(new byte[]{(byte)0xF3}, varint(3), new byte[]{1,2,3});
		var d = new ThriftCompactDecoder(data);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.LIST));
		assertEquals(0, d.available());
	}

	@Test void h04_skipField_list_invalid_size_throws() {
		// size > MAX_LIST_SIZE after long-form varint
		var data = concat(new byte[]{(byte)0xF3}, varint(2_000_000));
		var d = new ThriftCompactDecoder(data);
		assertThrows(IOException.class, () -> d.skipField(ThriftCompactEncoder.LIST));
	}

	@Test void h06_skipField_list_negative_size_throws() {
		// long-form: size = (int)readVarint() where varint yields Integer.MIN_VALUE
		// varint for 2^31 = 0x80000000: bytes 0x80,0x80,0x80,0x80,0x08
		var data = concat(
			new byte[]{(byte)0xF3},  // size==15 marker, elemType=I8
			new byte[]{(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x08}
		);
		var d = new ThriftCompactDecoder(data);
		assertThrows(IOException.class, () -> d.skipField(ThriftCompactEncoder.LIST));
	}

	@Test void h05_skipField_set_short_form() throws IOException {
		// SET shares the same branch as LIST
		var d = dec((byte)0x13, (byte)7); // size=1, elemType=I8(3), one data byte
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.SET));
		assertEquals(0, d.available());
	}

	// -----------------------------------------------------------------------
	// i — skipField: MAP
	// -----------------------------------------------------------------------

	@Test void i01_skipField_map_empty() {
		// size varint=0 → break immediately
		var data = varint(0);
		var d = new ThriftCompactDecoder(data);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.MAP));
	}

	@Test void i02_skipField_map_eof_after_size() {
		// size=1 but then EOF → b < 0 branch
		var data = varint(1);
		var d = new ThriftCompactDecoder(data);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.MAP));
	}

	@Test void i03_skipField_map_normal() throws IOException {
		// size=1, key=I8(3), val=I8(3) → kv-type byte=(val<<4)|key=(3<<4)|3=0x33
		var data = concat(varint(1), new byte[]{(byte)0x33, (byte)10, (byte)20});
		var d = new ThriftCompactDecoder(data);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.MAP));
		assertEquals(0, d.available());
	}

	@Test void i04_skipField_map_invalid_size_throws() {
		var data = varint(2_000_000);
		var d = new ThriftCompactDecoder(data);
		assertThrows(IOException.class, () -> d.skipField(ThriftCompactEncoder.MAP));
	}

	@Test void i05_skipField_map_negative_size_throws() {
		// (int)readVarint() yields Integer.MIN_VALUE → size < 0 branch
		var data = new byte[]{(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x08};
		var d = new ThriftCompactDecoder(data);
		assertThrows(IOException.class, () -> d.skipField(ThriftCompactEncoder.MAP));
	}

	// -----------------------------------------------------------------------
	// j — skipField: STRUCT
	// -----------------------------------------------------------------------

	@Test void j01_skipField_struct_empty() {
		// empty struct: stop byte 0x00
		var d = dec((byte)0x00);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.STRUCT));
	}

	@Test void j02_skipField_struct_with_i8_field() throws IOException {
		// one I8 field (delta=1, type=I8=3): byte=(1<<4)|3=0x13, I8 data, stop
		var d = dec((byte)0x13, (byte)42, (byte)0x00);
		assertDoesNotThrow(() -> d.skipField(ThriftCompactEncoder.STRUCT));
		assertEquals(0, d.available());
	}

	// -----------------------------------------------------------------------
	// k — skipField: default (unknown type)
	// -----------------------------------------------------------------------

	@Test void k01_skipField_unknown_type() {
		assertDoesNotThrow(() -> dec().skipField(0xFF));
	}

	// -----------------------------------------------------------------------
	// l — readBinary branches
	// -----------------------------------------------------------------------

	@Test void l01_readBinary_empty() throws IOException {
		// lenLong <= 0 → return empty array
		var d = new ThriftCompactDecoder(varint(0));
		assertArrayEquals(new byte[0], d.readBinary());
	}

	@Test void l02_readBinary_too_large_throws() {
		// lenLong > MAX_BINARY_LENGTH (64 MB) → IOException
		var d = new ThriftCompactDecoder(varint(64L * 1024 * 1024 + 1));
		assertThrows(IOException.class, d::readBinary);
	}

	@Test void l03_readBinary_normal() throws IOException {
		// Happy path: ByteArrayInputStream satisfies the full read in one pass
		var payload = new byte[]{(byte)1, (byte)2, (byte)3};
		var d = new ThriftCompactDecoder(concat(varint(3), payload));
		assertArrayEquals(payload, d.readBinary());
	}

	@Test void l04_readBinary_r_lte_zero_exits_loop() throws IOException {
		// Custom InputStream: length varint reads normally; payload read returns 1 byte then -1
		// to exercise the r<=0 break in the read loop
		var lengthBytes = varint(3);
		var combined = new InputStream() {
			int lenPos = 0;
			int phase = 0;
			@Override public int read() throws IOException {
				if (lenPos < lengthBytes.length)
					return lengthBytes[lenPos++] & 0xFF;
				return -1;
			}
			@Override public int read(byte[] b, int off, int len) throws IOException {
				if (phase == 0) { phase = 1; b[off] = 1; return 1; }
				return -1; // triggers r<=0 break
			}
		};
		var result = new ThriftCompactDecoder(combined).readBinary();
		assertEquals(3, result.length);
		assertEquals(1, result[0]);
	}

	// -----------------------------------------------------------------------
	// m — readListHeader
	// -----------------------------------------------------------------------

	@Test void m01_readListHeader_eof() throws IOException {
		// b < 0 → ListHeader(0,0)
		var lh = dec().readListHeader();
		assertEquals(0, lh.elemType);
		assertEquals(0, lh.size);
	}

	@Test void m02_readListHeader_short_form() throws IOException {
		// size=3 in high nibble, elemType=I8(3): byte=(3<<4)|3=0x33
		var lh = dec((byte)0x33).readListHeader();
		assertEquals(ThriftCompactEncoder.I8, lh.elemType);
		assertEquals(3, lh.size);
	}

	@Test void m03_readListHeader_long_form() throws IOException {
		// size==15 → read varint; byte=(15<<4)|I8=0xF3, varint 100
		var d = new ThriftCompactDecoder(concat(new byte[]{(byte)0xF3}, varint(100)));
		var lh = d.readListHeader();
		assertEquals(ThriftCompactEncoder.I8, lh.elemType);
		assertEquals(100, lh.size);
	}

	@Test void m04_readListHeader_long_form_invalid_throws() {
		// size==15 → varint 2_000_000 > MAX_LIST_SIZE
		var d = new ThriftCompactDecoder(concat(new byte[]{(byte)0xF3}, varint(2_000_000)));
		assertThrows(IOException.class, d::readListHeader);
	}

	@Test void m05_readListHeader_long_form_negative_size_throws() {
		// size==15 → (int)readVarint() = Integer.MIN_VALUE → size < 0 branch
		var data = concat(
			new byte[]{(byte)0xF3},
			new byte[]{(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x08}
		);
		var d = new ThriftCompactDecoder(data);
		assertThrows(IOException.class, d::readListHeader);
	}

	// -----------------------------------------------------------------------
	// n — readStructEnd with empty stack
	// -----------------------------------------------------------------------

	@Test void n01_readStructEnd_empty_stack() {
		// readStructEnd without readStructBegin → stack is empty → lastFieldId = 0
		var d = dec();
		assertDoesNotThrow(d::readStructEnd);
	}

	@Test void n02_readStructBegin_readStructEnd_roundtrip() {
		var d = dec();
		d.readStructBegin();
		assertDoesNotThrow(d::readStructEnd);
	}

	// -----------------------------------------------------------------------
	// o — readVarint edge cases
	// -----------------------------------------------------------------------

	@Test void o01_readVarint_eof_returns_zero() throws IOException {
		assertEquals(0L, dec().readVarint());
	}

	@Test void o02_readVarint_multi_byte() throws IOException {
		// 0x81 0x01 → varint 129
		assertEquals(129L, dec((byte)0x81, (byte)0x01).readVarint());
	}

	@Test void o03_readVarint_shift_overflow() {
		// 10 continuation bytes (high bit set) fill shift past 70 → loop exits
		var data = new byte[11];
		for (int i = 0; i < 10; i++)
			data[i] = (byte)0x80;
		data[10] = (byte)0x01;
		var d = new ThriftCompactDecoder(data);
		assertDoesNotThrow(d::readVarint);
	}

	// -----------------------------------------------------------------------
	// p — readByte, readI16, readI32, readI64, readDouble, readString
	// -----------------------------------------------------------------------

	@Test void p01_readByte() throws IOException {
		assertEquals((byte)0xAB, dec((byte)0xAB).readByte());
	}

	@Test void p02_readI16() throws IOException {
		// zigzag encode 10 → 20; fromZigzag(20) = 10
		assertEquals((short)10, new ThriftCompactDecoder(varint(20)).readI16());
	}

	@Test void p03_readI32() throws IOException {
		// zigzag encode 1 → 2; fromZigzag(2) = 1
		assertEquals(1, new ThriftCompactDecoder(varint(2)).readI32());
	}

	@Test void p04_readI64() throws IOException {
		// zigzag encode 2 → 4; fromZigzag(4) = 2
		assertEquals(2L, new ThriftCompactDecoder(varint(4)).readI64());
	}

	@Test void p05_readDouble() throws IOException {
		var bits = Double.doubleToLongBits(1.0);
		var data = new byte[8];
		for (int i = 0; i < 8; i++)
			data[i] = (byte)((bits >> (i * 8)) & 0xFF);
		assertEquals(1.0, new ThriftCompactDecoder(data).readDouble());
	}

	@Test void p06_readString() throws IOException {
		var hello = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		assertEquals("hello", new ThriftCompactDecoder(concat(varint(hello.length), hello)).readString());
	}

	// -----------------------------------------------------------------------
	// q — fromZigzag static helper
	// -----------------------------------------------------------------------

	@Test void q01_fromZigzag_positive() {
		assertEquals(1L, ThriftCompactDecoder.fromZigzag(2L));
		assertEquals(2L, ThriftCompactDecoder.fromZigzag(4L));
	}

	@Test void q02_fromZigzag_negative() {
		assertEquals(-1L, ThriftCompactDecoder.fromZigzag(1L));
		assertEquals(-2L, ThriftCompactDecoder.fromZigzag(3L));
	}

	// -----------------------------------------------------------------------
	// r — available()
	// -----------------------------------------------------------------------

	@Test void r01_available() throws IOException {
		assertEquals(3, dec((byte)1, (byte)2, (byte)3).available());
	}

	// -----------------------------------------------------------------------
	// s — 3-arg constructor
	// -----------------------------------------------------------------------

	@Test void s01_byteArrayOffsetLength_constructor() throws IOException {
		// reads only the middle byte 0x11: delta=1, type=1 (BOOLEAN_TRUE)
		var d = new ThriftCompactDecoder(new byte[]{(byte)0x00, (byte)0x11, (byte)0x00}, 1, 1);
		var fh = d.readFieldHeader();
		assertFalse(fh.isStop);
		assertEquals(1, fh.fieldId);
	}
}
