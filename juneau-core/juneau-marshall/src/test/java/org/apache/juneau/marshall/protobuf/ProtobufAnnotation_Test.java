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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link Protobuf @Protobuf} annotation (explicit field numbers + scalar-type overrides).
 */
class ProtobufAnnotation_Test extends TestBase {

	private static String ser(Object o) throws Exception {
		return toSpacedHex(ProtobufSerializer.DEFAULT.write(o));
	}

	public static class ExplicitNumber {
		@Protobuf(fieldNumber=10)
		public int x;
		public ExplicitNumber() {}
		public ExplicitNumber(int x) { this.x = x; }
	}

	@Test
	void a01_explicitFieldNumber() throws Exception {
		// field 10, varint -> tag (10<<3)|0 = 80 = 0x50; value 5.
		assertEquals("50 05", ser(new ExplicitNumber(5)));
		var p = ProtobufParser.DEFAULT.read(ProtobufSerializer.DEFAULT.write(new ExplicitNumber(5)), ExplicitNumber.class);
		assertBean(p, "x", "5");
	}

	public static class ZigZag {
		@Protobuf(type=ProtobufScalarType.SINT32)
		public int delta;
		public ZigZag() {}
		public ZigZag(int delta) { this.delta = delta; }
	}

	@Test
	void a02_zigzagType() throws Exception {
		// delta=-1, sint32 zigzag -> 1.  field 1 varint -> 08 01
		assertEquals("08 01", ser(new ZigZag(-1)));
		var p = ProtobufParser.DEFAULT.read(ProtobufSerializer.DEFAULT.write(new ZigZag(-75)), ZigZag.class);
		assertBean(p, "delta", "-75");
	}

	public static class Fixed {
		@Protobuf(type=ProtobufScalarType.FIXED32)
		public int n;
		public Fixed() {}
		public Fixed(int n) { this.n = n; }
	}

	@Test
	void a03_fixed32Type() throws Exception {
		// n=1, fixed32 -> field 1 I32 tag (1<<3)|5 = 0x0D; little-endian 01 00 00 00
		assertEquals("0D 01 00 00 00", ser(new Fixed(1)));
		var p = ProtobufParser.DEFAULT.read(ProtobufSerializer.DEFAULT.write(new Fixed(123456)), Fixed.class);
		assertBean(p, "n", "123456");
	}

	public enum Color { RED, GREEN, BLUE }

	public static class EnumName {
		@Protobuf(type=ProtobufScalarType.ENUM_STRING)
		public Color color;
		public EnumName() {}
		public EnumName(Color color) { this.color = color; }
	}

	@Test
	void a04_enumNameType() throws Exception {
		// color=1 LEN "BLUE" -> 0A 04 42 4C 55 45
		assertEquals("0A 04 42 4C 55 45", ser(new EnumName(Color.BLUE)));
		var p = ProtobufParser.DEFAULT.read(ProtobufSerializer.DEFAULT.write(new EnumName(Color.GREEN)), EnumName.class);
		assertBean(p, "color", "GREEN");
	}

	public static class Unsigned {
		@Protobuf(type=ProtobufScalarType.UINT64)
		public java.math.BigInteger v;
		public Unsigned() {}
		public Unsigned(java.math.BigInteger v) { this.v = v; }
	}

	@Test
	void a05_uint64BigIntegerLossless() throws Exception {
		// value > Long.MAX_VALUE round-trips losslessly via BigInteger magnitude (R5).
		var big = new java.math.BigInteger("18446744073709551615"); // 2^64 - 1
		var p = ProtobufParser.DEFAULT.read(ProtobufSerializer.DEFAULT.write(new Unsigned(big)), Unsigned.class);
		assertBean(p, "v", "18446744073709551615");
	}
}
