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
 * Edge-numeric and presence/null hardening tests for the protobuf binary codec.
 */
class ProtobufEdgeCases_Test extends TestBase {

	private static String ser(Object o) throws Exception {
		return toSpacedHex(ProtobufSerializer.DEFAULT.serialize(o));
	}

	private static <T> T roundTrip(T o, Class<T> c) throws Exception {
		return ProtobufParser.DEFAULT.parse(ProtobufSerializer.DEFAULT.serialize(o), c);
	}

	public static class Ints {
		public int i;
		public long l;
		public Ints() {}
		public Ints(int i, long l) { this.i = i; this.l = l; }
	}

	@Test
	void a01_int32MinMax() throws Exception {
		assertEquals(Integer.MAX_VALUE, roundTrip(new Ints(Integer.MAX_VALUE, 0), Ints.class).i);
		assertEquals(Integer.MIN_VALUE, roundTrip(new Ints(Integer.MIN_VALUE, 0), Ints.class).i);
	}

	@Test
	void a02_int64MinMax() throws Exception {
		assertEquals(Long.MAX_VALUE, roundTrip(new Ints(0, Long.MAX_VALUE), Ints.class).l);
		assertEquals(Long.MIN_VALUE, roundTrip(new Ints(0, Long.MIN_VALUE), Ints.class).l);
	}

	public static class Uint {
		@Protobuf(type=ProtobufScalarType.UINT64)
		public long v;
		public Uint() {}
		public Uint(long v) { this.v = v; }
	}

	@Test
	void a03_uint64RawLongBits() throws Exception {
		// R5: long-typed uint64 carries the raw bits (may surface negative) and round-trips losslessly.
		assertEquals(-1L, roundTrip(new Uint(-1L), Uint.class).v);
		assertEquals(Long.MIN_VALUE, roundTrip(new Uint(Long.MIN_VALUE), Uint.class).v);
	}

	public static class ZigZags {
		@Protobuf(type=ProtobufScalarType.SINT32)
		public int a;
		@Protobuf(type=ProtobufScalarType.SINT64)
		public long b;
		public ZigZags() {}
		public ZigZags(int a, long b) { this.a = a; this.b = b; }
	}

	@Test
	void a04_zigzagExtremes() throws Exception {
		var r = roundTrip(new ZigZags(Integer.MIN_VALUE, Long.MIN_VALUE), ZigZags.class);
		assertEquals(Integer.MIN_VALUE, r.a);
		assertEquals(Long.MIN_VALUE, r.b);
	}

	public static class Floats {
		public double d;
		public float f;
		public Floats() {}
		public Floats(double d, float f) { this.d = d; this.f = f; }
	}

	@Test
	void a05_nanAndInfinities() throws Exception {
		var nan = roundTrip(new Floats(Double.NaN, Float.NaN), Floats.class);
		assertTrue(Double.isNaN(nan.d));
		assertTrue(Float.isNaN(nan.f));
		var pinf = roundTrip(new Floats(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), Floats.class);
		assertEquals(Double.POSITIVE_INFINITY, pinf.d);
		assertEquals(Float.POSITIVE_INFINITY, pinf.f);
		var ninf = roundTrip(new Floats(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY), Floats.class);
		assertEquals(Double.NEGATIVE_INFINITY, ninf.d);
		assertEquals(Float.NEGATIVE_INFINITY, ninf.f);
	}

	public static class Strings {
		public String s;
		public Strings() {}
		public Strings(String s) { this.s = s; }
	}

	@Test
	void a06_emptyString() throws Exception {
		// non-null empty string -> field 1 LEN, len 0 -> 0A 00
		assertEquals("0A 00", ser(new Strings("")));
		assertEquals("", roundTrip(new Strings(""), Strings.class).s);
	}

	public static class Bytes {
		public byte[] data;
		public Bytes() {}
		public Bytes(byte[] data) { this.data = data; }
	}

	@Test
	void a07_emptyBytes() throws Exception {
		assertEquals("0A 00", ser(new Bytes(new byte[0])));
		assertArrayEquals(new byte[0], roundTrip(new Bytes(new byte[0]), Bytes.class).data);
	}

	public static class Lists {
		public List<Integer> nums;
		public List<String> strs;
		public Lists() { /* Public no-arg constructor required for Juneau bean recognition. */ }
	}

	@Test
	void a08_emptyCollectionsOmitted() throws Exception {
		var a = new Lists();
		a.nums = list();
		a.strs = list();
		assertEquals("", ser(a));
	}

	public static class Maps {
		public Map<String,Integer> m;
		public Maps() { /* Public no-arg constructor required for Juneau bean recognition. */ }
	}

	@Test
	void a09_emptyMapOmitted() throws Exception {
		var a = new Maps();
		a.m = map();
		assertEquals("", ser(a));
	}

	@Test
	void a10_nullInsideCollectionSkipped() throws Exception {
		var a = new Lists();
		a.strs = new ArrayList<>(Arrays.asList("a", null, "b"));
		// nulls skipped -> two tagged strings.
		assertEquals("12 01 61 12 01 62", ser(a));
	}
}
