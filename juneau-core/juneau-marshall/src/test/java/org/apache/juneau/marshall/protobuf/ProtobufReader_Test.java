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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Golden-byte tests for {@link ProtobufReader} wire primitives.
 */
class ProtobufReader_Test extends TestBase {

	private static ProtobufReader reader(int...bytes) {
		var b = new byte[bytes.length];
		for (var i = 0; i < bytes.length; i++)
			b[i] = (byte)bytes[i];
		return new ProtobufReader(b);
	}

	@Test
	void a01_varint() throws Exception {
		assertEquals(0L, reader(0x00).readVarint());
		assertEquals(1L, reader(0x01).readVarint());
		assertEquals(150L, reader(0x96, 0x01).readVarint());
		assertEquals(300L, reader(0xAC, 0x02).readVarint());
	}

	@Test
	void a02_varintNegativeInt32() throws Exception {
		assertEquals(-1L, reader(0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0x01).readVarint());
	}

	@Test
	void a03_zigzag() throws Exception {
		assertEquals(0, reader(0x00).readZigZag32());
		assertEquals(-1, reader(0x01).readZigZag32());
		assertEquals(1, reader(0x02).readZigZag32());
		assertEquals(-2, reader(0x03).readZigZag32());
		assertEquals(-1L, reader(0x01).readZigZag64());
		assertEquals(1L, reader(0x02).readZigZag64());
	}

	@Test
	void a04_fixed() throws Exception {
		assertEquals(1.0f, Float.intBitsToFloat(reader(0x00,0x00,0x80,0x3F).readFixed32()));
		assertEquals(1.0, Double.longBitsToDouble(reader(0x00,0x00,0x00,0x00,0x00,0x00,0xF0,0x3F).readFixed64()));
	}

	@Test
	void a05_tag() throws Exception {
		var t = reader(0x08).readTag();
		assertEquals(1, ProtobufReader.fieldNumber(t));
		assertEquals(WireType.VARINT, ProtobufReader.wireType(t));

		var t2 = reader(0x12).readTag();
		assertEquals(2, ProtobufReader.fieldNumber(t2));
		assertEquals(WireType.LEN, ProtobufReader.wireType(t2));

		// field 16 -> 2-byte tag.
		var t3 = reader(0x82, 0x01).readTag();
		assertEquals(16, ProtobufReader.fieldNumber(t3));
		assertEquals(WireType.LEN, ProtobufReader.wireType(t3));
	}

	@Test
	void a06_string() throws Exception {
		assertEquals("testing", reader(0x07,0x74,0x65,0x73,0x74,0x69,0x6E,0x67).readString());
		assertEquals("", reader(0x00).readString());
	}

	@Test
	void a07_eofTag() throws Exception {
		assertEquals(ProtobufReader.EOF, reader().readTag());
	}

	@Test
	void a08_skipField() throws Exception {
		// VARINT field, followed by a sentinel tag for field 2.
		var r = reader(0x96, 0x01, 0x10, 0x05);
		r.skipField(WireType.VARINT);
		assertEquals(2, ProtobufReader.fieldNumber(r.readTag()));

		// I32 field (4 bytes) then sentinel.
		var r2 = reader(0x01,0x02,0x03,0x04, 0x10);
		r2.skipField(WireType.I32);
		assertEquals(2, ProtobufReader.fieldNumber(r2.readTag()));

		// I64 field (8 bytes) then sentinel.
		var r3 = reader(1,2,3,4,5,6,7,8, 0x10);
		r3.skipField(WireType.I64);
		assertEquals(2, ProtobufReader.fieldNumber(r3.readTag()));

		// LEN field (len 3 + 3 bytes) then sentinel.
		var r4 = reader(0x03, 0xAA, 0xBB, 0xCC, 0x10);
		r4.skipField(WireType.LEN);
		assertEquals(2, ProtobufReader.fieldNumber(r4.readTag()));
	}
}
