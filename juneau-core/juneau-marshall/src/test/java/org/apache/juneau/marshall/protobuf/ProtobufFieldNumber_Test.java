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
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the field-number/scalar-type table on {@link ProtobufClassMeta}.
 */
class ProtobufFieldNumber_Test extends TestBase {

	private static ProtobufClassMeta pcm(Class<?> c) {
		return ProtobufSerializer.DEFAULT.getProtobufClassMeta(MarshallingContext.DEFAULT.getClassMeta(c));
	}

	public static class A01 {
		public String zebra;
		public String apple;
		public String mango;
	}

	@Test
	void a01_autoNumbersAlphabetical() {
		var pcm = pcm(A01.class);
		assertEquals(1, pcm.fieldNumber("apple"));
		assertEquals(2, pcm.fieldNumber("mango"));
		assertEquals(3, pcm.fieldNumber("zebra"));
		assertEquals(-1, pcm.fieldNumber("nope"));
	}

	public static class A02 {
		@Protobuf(fieldNumber=5)
		public String apple;
		public String mango;
		public String zebra;
	}

	@Test
	void a02_explicitOverrideReservesNumber() {
		var pcm = pcm(A02.class);
		assertEquals(5, pcm.fieldNumber("apple"));
		assertEquals(1, pcm.fieldNumber("mango"));
		assertEquals(2, pcm.fieldNumber("zebra"));
	}

	public static class A03 {
		@Protobuf(fieldNumber=1)
		public String b;
		public String a;  // wants 1, but it's taken -> next free = 2
		public String c;
	}

	@Test
	void a03_autoFillsAroundExplicit() {
		var pcm = pcm(A03.class);
		assertEquals(1, pcm.fieldNumber("b"));
		assertEquals(2, pcm.fieldNumber("a"));
		assertEquals(3, pcm.fieldNumber("c"));
	}

	public static class A04 {
		@Protobuf(type=ProtobufScalarType.SINT32)
		public int delta;
	}

	@Test
	void a04_explicitTypeOverride() {
		var pcm = pcm(A04.class);
		assertEquals(ProtobufScalarType.SINT32, pcm.entryFor(pcm.fieldNumber("delta")).scalarType());
	}

	public static class A05 {
		public boolean flag;
		public int count;
		public long big;
		public float f;
		public double d;
		public String s;
		public MyEnum e;
	}

	public enum MyEnum { RED, GREEN, BLUE }

	@Test
	void a05_defaultScalarMapping() {
		var pcm = pcm(A05.class);
		assertEquals(ProtobufScalarType.INT64, pcm.entryForName("big").scalarType());
		assertEquals(ProtobufScalarType.INT32, pcm.entryForName("count").scalarType());
		assertEquals(ProtobufScalarType.DOUBLE, pcm.entryForName("d").scalarType());
		assertEquals(ProtobufScalarType.ENUM_INT, pcm.entryForName("e").scalarType());
		assertEquals(ProtobufScalarType.FLOAT, pcm.entryForName("f").scalarType());
		assertEquals(ProtobufScalarType.BOOL, pcm.entryForName("flag").scalarType());
		assertEquals(ProtobufScalarType.STRING, pcm.entryForName("s").scalarType());
	}

	@Test
	void a06_tableIsCached() {
		var cm = MarshallingContext.DEFAULT.getClassMeta(A01.class);
		var p1 = ProtobufSerializer.DEFAULT.getProtobufClassMeta(cm);
		var p2 = ProtobufSerializer.DEFAULT.getProtobufClassMeta(cm);
		assertSame(p1, p2);
	}

	@Test
	void a07_entriesOrderedByFieldNumber() {
		var pcm = pcm(A02.class);
		var nums = pcm.entries().stream().map(ProtobufFieldEntry::fieldNumber).toList();
		assertEquals(java.util.List.of(1, 2, 5), nums);
	}
}
