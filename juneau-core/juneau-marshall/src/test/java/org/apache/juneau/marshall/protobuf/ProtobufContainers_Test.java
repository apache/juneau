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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Golden-byte + round-trip tests for protobuf container encodings (packed/repeated/map/nested).
 */
class ProtobufContainers_Test extends TestBase {

	private static String ser(Object o) throws Exception {
		return toSpacedHex(ProtobufSerializer.DEFAULT.serialize(o));
	}

	public static class PackedInts {
		public int[] nums;
		public PackedInts() {}
		public PackedInts(int...nums) { this.nums = nums; }
	}

	@Test
	void a01_packedRepeatedInt32() throws Exception {
		assertEquals("0A 06 03 8E 02 9E A7 05", ser(new PackedInts(3, 270, 86942)));
	}

	@Test
	void a01b_packedRoundTrip() throws Exception {
		var bytes = ProtobufSerializer.DEFAULT.serialize(new PackedInts(3, 270, 86942));
		var p = ProtobufParser.DEFAULT.parse(bytes, PackedInts.class);
		assertArrayEquals(new int[]{3, 270, 86942}, p.nums);
	}

	public static class StrMap {
		public Map<String,Integer> m;
		public StrMap() {}
		public StrMap(Map<String,Integer> m) { this.m = m; }
	}

	@Test
	void a02_mapStringInt() throws Exception {
		assertEquals("0A 05 0A 01 61 10 01", ser(new StrMap(map("a", 1))));
	}

	@Test
	void a02b_mapRoundTrip() throws Exception {
		var orig = new StrMap(map("a", 1, "b", 2));
		var bytes = ProtobufSerializer.DEFAULT.serialize(orig);
		var p = ProtobufParser.DEFAULT.parse(bytes, StrMap.class);
		assertEquals(Integer.valueOf(1), p.m.get("a"));
		assertEquals(Integer.valueOf(2), p.m.get("b"));
	}

	public static class Inner {
		public int id;
		public Inner() {}
		public Inner(int id) { this.id = id; }
	}

	public static class Outer {
		public Inner inner;
		public Outer() {}
		public Outer(Inner inner) { this.inner = inner; }
	}

	@Test
	void a03_nestedMessage() throws Exception {
		assertEquals("0A 03 08 96 01", ser(new Outer(new Inner(150))));
	}

	@Test
	void a03b_nestedRoundTrip() throws Exception {
		var bytes = ProtobufSerializer.DEFAULT.serialize(new Outer(new Inner(150)));
		var p = ProtobufParser.DEFAULT.parse(bytes, Outer.class);
		assertEquals(150, p.inner.id);
	}

	public static class StrList {
		public List<String> tags;
		public StrList() {}
		public StrList(List<String> tags) { this.tags = tags; }
	}

	@Test
	void a04_repeatedStringTagged() throws Exception {
		assertEquals("0A 01 61 0A 01 62", ser(new StrList(list("a", "b"))));
	}

	@Test
	void a04b_repeatedStringRoundTrip() throws Exception {
		var bytes = ProtobufSerializer.DEFAULT.serialize(new StrList(list("a", "b", "c")));
		var p = ProtobufParser.DEFAULT.parse(bytes, StrList.class);
		assertEquals(list("a", "b", "c"), p.tags);
	}

	public static class BeanList {
		public List<Inner> items;
		public BeanList() {}
		public BeanList(List<Inner> items) { this.items = items; }
	}

	@Test
	void a05_repeatedBeanRoundTrip() throws Exception {
		var bytes = ProtobufSerializer.DEFAULT.serialize(new BeanList(list(new Inner(1), new Inner(2))));
		var p = ProtobufParser.DEFAULT.parse(bytes, BeanList.class);
		assertEquals(2, p.items.size());
		assertEquals(1, p.items.get(0).id);
		assertEquals(2, p.items.get(1).id);
	}

	@Test
	void a06_unpackedRepeatedAcceptedOnParse() throws Exception {
		// Hand-built UNPACKED repeated int32 for field 1: 08 03  08 8E 02 (two tagged varints).
		var bytes = new byte[]{0x08, 0x03, 0x08, (byte)0x8E, 0x02};
		var p = ProtobufParser.DEFAULT.parse(bytes, PackedInts.class);
		assertArrayEquals(new int[]{3, 270}, p.nums);
	}

	public enum E { A, B, C }

	public static class EnumMap2 {
		public Map<E,Integer> m;
		public EnumMap2() {}
		public EnumMap2(Map<E,Integer> m) { this.m = m; }
	}

	@Test
	void a07_enumKeyMapRoundTrip() throws Exception {
		var orig = new EnumMap2(map(E.B, 5));
		var bytes = ProtobufSerializer.DEFAULT.serialize(orig);
		var p = ProtobufParser.DEFAULT.parse(bytes, EnumMap2.class);
		assertEquals(Integer.valueOf(5), p.m.get(E.B));
	}
}
