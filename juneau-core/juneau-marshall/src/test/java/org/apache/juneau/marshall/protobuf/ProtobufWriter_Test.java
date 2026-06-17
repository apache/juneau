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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.function.*;
import org.junit.jupiter.api.*;

/**
 * Golden-byte tests for {@link ProtobufWriter} wire primitives.
 */
class ProtobufWriter_Test extends TestBase {

	private static String write(ThrowingConsumer<ProtobufWriter> c) throws Exception {
		var baos = new ByteArrayOutputStream();
		var w = new ProtobufWriter(baos);
		c.accept(w);
		w.flush();
		return toSpacedHex(baos.toByteArray());
	}

	@Test
	void a01_varintSmall() throws Exception {
		assertEquals("00", write(w -> w.writeVarint(0)));
		assertEquals("01", write(w -> w.writeVarint(1)));
		assertEquals("96 01", write(w -> w.writeVarint(150)));
		assertEquals("AC 02", write(w -> w.writeVarint(300)));
	}

	@Test
	void a02_varintNegativeInt32Is10Bytes() throws Exception {
		// int32 -1 sign-extends to 64 bits -> 10-byte varint.
		assertEquals("FF FF FF FF FF FF FF FF FF 01", write(w -> w.writeVarint(-1L)));
	}

	@Test
	void a03_zigzag32() throws Exception {
		assertEquals("00", write(w -> w.writeZigZag32(0)));
		assertEquals("01", write(w -> w.writeZigZag32(-1)));
		assertEquals("02", write(w -> w.writeZigZag32(1)));
		assertEquals("03", write(w -> w.writeZigZag32(-2)));
		assertEquals("04", write(w -> w.writeZigZag32(2)));
	}

	@Test
	void a04_zigzag64() throws Exception {
		assertEquals("00", write(w -> w.writeZigZag64(0L)));
		assertEquals("01", write(w -> w.writeZigZag64(-1L)));
		assertEquals("02", write(w -> w.writeZigZag64(1L)));
	}

	@Test
	void a05_fixed() throws Exception {
		// little-endian
		assertEquals("00 00 80 3F", write(w -> w.writeFixed32(Float.floatToIntBits(1.0f))));
		assertEquals("00 00 00 00 00 00 F0 3F", write(w -> w.writeFixed64(Double.doubleToLongBits(1.0))));
	}

	@Test
	void a06_tag() throws Exception {
		assertEquals("08", write(w -> w.writeTag(1, WireType.VARINT)));
		assertEquals("12", write(w -> w.writeTag(2, WireType.LEN)));
		assertEquals("22", write(w -> w.writeTag(4, WireType.LEN)));
		// field 16 -> 2-byte tag.
		assertEquals("82 01", write(w -> w.writeTag(16, WireType.LEN)));
	}

	@Test
	void a07_stringLenDelimited() throws Exception {
		assertEquals("07 74 65 73 74 69 6E 67", write(w -> w.writeLenDelimited("testing".getBytes(UTF_8))));
		assertEquals("07 74 65 73 74 69 6E 67", write(w -> w.writeString("testing")));
		assertEquals("00", write(w -> w.writeString("")));
	}
}
