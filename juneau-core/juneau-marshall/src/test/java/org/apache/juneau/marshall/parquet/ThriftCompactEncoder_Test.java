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
 * Branch-coverage tests for {@link ThriftCompactEncoder}.
 */
class ThriftCompactEncoder_Test {

	private static byte[] encode(ThriftCompactEncoder.ThriftWriteAction action) throws IOException {
		return ThriftCompactEncoder.encodeToBytes(action);
	}

	// -----------------------------------------------------------------------
	// a — writeFieldBegin: short form (delta 1–15) vs long form
	// -----------------------------------------------------------------------

	@Test void a01_writeFieldBegin_short_form_delta1() throws IOException {
		// delta=1, type=I32(5): byte = (1<<4)|5 = 0x15
		var bytes = encode(e -> e.writeFieldBegin(ThriftCompactEncoder.I32, 1));
		assertEquals(1, bytes.length);
		assertEquals(0x15, bytes[0] & 0xFF);
	}

	@Test void a02_writeFieldBegin_short_form_delta15() throws IOException {
		// delta=15, type=I8(3): byte = (15<<4)|3 = 0xF3
		var bytes = encode(e -> {
			e.writeFieldBegin(ThriftCompactEncoder.I8, 1);  // advance lastFieldId to 1
			e.writeFieldBegin(ThriftCompactEncoder.I8, 16); // delta=15
		});
		assertEquals(2, bytes.length);
		assertEquals(0xF3, bytes[1] & 0xFF);
	}

	@Test void a03_writeFieldBegin_long_form_delta16() throws IOException {
		// delta=16 > 15 → long form: low nibble=type, then varint(delta)
		var bytes = encode(e -> {
			e.writeFieldBegin(ThriftCompactEncoder.I8, 1);   // lastFieldId=1
			e.writeFieldBegin(ThriftCompactEncoder.I8, 17);  // delta=16 → long form
		});
		// Second field: byte=type(3), then varint(16)=0x10
		assertEquals(3, bytes.length);
		assertEquals(ThriftCompactEncoder.I8, bytes[1] & 0x0F);
		assertEquals(16, bytes[2] & 0xFF);
	}

	@Test void a04_writeFieldBegin_long_form_delta_zero() throws IOException {
		// delta=0 → long form (delta not > 0)
		var bytes = encode(e -> {
			e.writeFieldBegin(ThriftCompactEncoder.I8, 5);  // lastFieldId=5
			e.writeFieldBegin(ThriftCompactEncoder.I8, 5);  // delta=0 → long form
		});
		// Second field: byte=type, then varint(0)=0x00
		assertEquals(3, bytes.length);
		assertEquals(0, bytes[2] & 0xFF);
	}

	// -----------------------------------------------------------------------
	// b — writeFieldStop: restores from non-empty stack
	// -----------------------------------------------------------------------

	@Test void b01_writeFieldStop_empty_stack() throws IOException {
		var bytes = encode(e -> e.writeFieldStop());
		assertEquals(1, bytes.length);
		assertEquals(0, bytes[0]);
	}

	@Test void b02_writeFieldStop_restores_from_stack() throws IOException {
		// writeStructBegin pushes lastFieldId onto stack; writeFieldStop should pop it
		var bytes = encode(e -> {
			e.writeFieldBegin(ThriftCompactEncoder.I8, 3); // lastFieldId=3
			e.writeStructBegin();                          // pushes 3
			e.writeFieldStop();                            // pops 3 → lastFieldId=3 again
		});
		assertNotNull(bytes);
	}

	// -----------------------------------------------------------------------
	// c — writeBool
	// -----------------------------------------------------------------------

	@Test void c01_writeBool_true() throws IOException {
		var bytes = encode(e -> e.writeBool(true));
		assertEquals(1, bytes.length);
		assertEquals(ThriftCompactEncoder.BOOLEAN_TRUE, bytes[0]);
	}

	@Test void c02_writeBool_false() throws IOException {
		var bytes = encode(e -> e.writeBool(false));
		assertEquals(1, bytes.length);
		assertEquals(ThriftCompactEncoder.BOOLEAN_FALSE, bytes[0]);
	}

	// -----------------------------------------------------------------------
	// d — writeBinary: null, empty, non-empty
	// -----------------------------------------------------------------------

	@Test void d01_writeBinary_null() throws IOException {
		// null → writeVarint(0), skip write
		var bytes = encode(e -> e.writeBinary(null));
		assertEquals(1, bytes.length);
		assertEquals(0, bytes[0]); // varint(0)
	}

	@Test void d02_writeBinary_empty() throws IOException {
		// data.length==0 → writeVarint(0), skip write
		var bytes = encode(e -> e.writeBinary(new byte[0]));
		assertEquals(1, bytes.length);
		assertEquals(0, bytes[0]);
	}

	@Test void d03_writeBinary_non_empty() throws IOException {
		var payload = new byte[]{(byte)1, (byte)2, (byte)3};
		var bytes = encode(e -> e.writeBinary(payload));
		assertEquals(4, bytes.length);
		assertEquals(3, bytes[0]); // varint(3)
		assertArrayEquals(payload, new byte[]{bytes[1], bytes[2], bytes[3]});
	}

	// -----------------------------------------------------------------------
	// e — writeString: null, empty, non-empty
	// -----------------------------------------------------------------------

	@Test void e01_writeString_null() throws IOException {
		// null → empty byte[], writeVarint(0), skip write
		var bytes = encode(e -> e.writeString(null));
		assertEquals(1, bytes.length);
		assertEquals(0, bytes[0]);
	}

	@Test void e02_writeString_empty() throws IOException {
		// "" → bytes.length==0 → writeVarint(0), skip write
		var bytes = encode(e -> e.writeString(""));
		assertEquals(1, bytes.length);
		assertEquals(0, bytes[0]);
	}

	@Test void e03_writeString_non_empty() throws IOException {
		var bytes = encode(e -> e.writeString("hi"));
		assertEquals(3, bytes.length);
		assertEquals(2, bytes[0]); // varint(2)
		assertEquals('h', bytes[1]);
		assertEquals('i', bytes[2]);
	}

	// -----------------------------------------------------------------------
	// f — writeListBegin: short form (size 0–14) vs long form (size > 14)
	// -----------------------------------------------------------------------

	@Test void f01_writeListBegin_short_form_size0() throws IOException {
		var bytes = encode(e -> e.writeListBegin(ThriftCompactEncoder.I8, 0));
		assertEquals(1, bytes.length);
		assertEquals(ThriftCompactEncoder.I8, bytes[0] & 0x0F);
		assertEquals(0, (bytes[0] >> 4) & 0x0F);
	}

	@Test void f02_writeListBegin_short_form_size14() throws IOException {
		var bytes = encode(e -> e.writeListBegin(ThriftCompactEncoder.I8, 14));
		assertEquals(1, bytes.length);
		assertEquals(14, (bytes[0] >> 4) & 0x0F);
	}

	@Test void f03_writeListBegin_long_form_size15() throws IOException {
		// size=15 → long form: 0xF0|type, then varint(15)
		var bytes = encode(e -> e.writeListBegin(ThriftCompactEncoder.I8, 15));
		assertEquals(2, bytes.length);
		assertEquals(0xF3, bytes[0] & 0xFF); // 0xF0 | I8(3)
		assertEquals(15, bytes[1]);
	}

	@Test void f04_writeListBegin_long_form_negative_size() throws IOException {
		// size < 0 → falls into else branch (size >= 0 is false)
		var bytes = encode(e -> e.writeListBegin(ThriftCompactEncoder.I8, -1));
		assertTrue(bytes.length >= 2);
		assertEquals(0xF3, bytes[0] & 0xFF);
	}

	// -----------------------------------------------------------------------
	// g — writeVarint: single-byte and multi-byte
	// -----------------------------------------------------------------------

	@Test void g01_writeVarint_single_byte() throws IOException {
		var bytes = encode(e -> e.writeVarint(127));
		assertEquals(1, bytes.length);
		assertEquals(127, bytes[0]);
	}

	@Test void g02_writeVarint_multi_byte() throws IOException {
		// 128 → 0x80 0x01
		var bytes = encode(e -> e.writeVarint(128));
		assertEquals(2, bytes.length);
		assertEquals(0x80, bytes[0] & 0xFF);
		assertEquals(0x01, bytes[1]);
	}

	// -----------------------------------------------------------------------
	// h — toZigzag static helper
	// -----------------------------------------------------------------------

	@Test void h01_toZigzag_positive() {
		assertEquals(2L, ThriftCompactEncoder.toZigzag(1L));
		assertEquals(4L, ThriftCompactEncoder.toZigzag(2L));
	}

	@Test void h02_toZigzag_negative() {
		assertEquals(1L, ThriftCompactEncoder.toZigzag(-1L));
		assertEquals(3L, ThriftCompactEncoder.toZigzag(-2L));
	}

	// -----------------------------------------------------------------------
	// i — round-trip encode/decode
	// -----------------------------------------------------------------------

	@Test void i01_roundtrip_i32() throws IOException {
		var bytes = encode(e -> e.writeI32(42));
		assertEquals(42, new ThriftCompactDecoder(bytes).readI32());
	}

	@Test void i02_roundtrip_string() throws IOException {
		var bytes = encode(e -> e.writeString("hello"));
		assertEquals("hello", new ThriftCompactDecoder(bytes).readString());
	}
}
