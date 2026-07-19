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

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Golden-byte tests for {@link ProtobufSerializer} scalar bean serialization.
 */
class ProtobufSerializer_Test extends TestBase {

	private static String ser(Object o) throws Exception {
		return toSpacedHex(ProtobufSerializer.DEFAULT.write(o));
	}

	// Alphabetical: id=1, name=2.
	public static class Simple {
		public int id;
		public String name;
		public Simple() {}
		public Simple(int id, String name) { this.id = id; this.name = name; }
	}

	@Test
	void a01_intAndString() throws Exception {
		assertEquals("08 96 01 12 07 74 65 73 74 69 6E 67", ser(new Simple(150, "testing")));
	}

	@Test
	void a02_nullOmitted() throws Exception {
		assertEquals("08 96 01", ser(new Simple(150, null)));
	}

	public static class Nums {
		public boolean b;
		public double d;
		public float f;
		public long l;
		public Nums() {}
		public Nums(boolean b, double d, float f, long l) { this.b = b; this.d = d; this.f = f; this.l = l; }
	}

	@Test
	void a03_doubleFloatBoolLong() throws Exception {
		// Alphabetical: b=1(varint), d=2(I64), f=3(I32), l=4(varint).
		// b=true -> 08 01 ; d=1.0 -> 11 00..F0 3F ; f=1.0f -> 1D 00 00 80 3F ; l=2 -> 20 02
		assertEquals("08 01 11 00 00 00 00 00 00 F0 3F 1D 00 00 80 3F 20 02", ser(new Nums(true, 1.0, 1.0f, 2L)));
	}

	public enum Color { RED, GREEN, BLUE }

	public static class WithEnum {
		public Color color;
		public WithEnum() {}
		public WithEnum(Color color) { this.color = color; }
	}

	@Test
	void a04_enumOrdinalDefault() throws Exception {
		// color=1, ordinal of BLUE = 2 -> 08 02
		assertEquals("08 02", ser(new WithEnum(Color.BLUE)));
		assertEquals("08 00", ser(new WithEnum(Color.RED)));
	}

	public static class WithBytes {
		public byte[] data;
		public WithBytes() {}
		public WithBytes(byte[] data) { this.data = data; }
	}

	@Test
	void a05_bytes() throws Exception {
		// data=1 LEN, len 3 + AA BB CC
		assertEquals("0A 03 AA BB CC", ser(new WithBytes(new byte[]{(byte)0xAA,(byte)0xBB,(byte)0xCC})));
	}

	@Test
	void a06_nullRoot() throws Exception {
		assertEquals("", ser(null));
	}

	public static class WithBig {
		public java.math.BigInteger bi;
		public WithBig() {}
		public WithBig(java.math.BigInteger bi) { this.bi = bi; }
	}

	@Test
	void a07_bigIntegerAsString() throws Exception {
		// bi=1 LEN, "123" -> 0A 03 31 32 33
		assertEquals("0A 03 31 32 33", ser(new WithBig(new java.math.BigInteger("123"))));
	}
}
