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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings("rawtypes")
class SimpleObjects_RoundTripTest extends RoundTripTest_Base {

	@ParameterizedTest
	@MethodSource("testers")
	void a01_null(RoundTrip_Tester t) throws Exception {
		assertNull(t.roundTrip((String)null));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_optional(RoundTrip_Tester t) throws Exception {
		assertFalse(t.roundTrip(opte()).isPresent());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a03_string(RoundTrip_Tester t) throws Exception {
		assertEquals("foobar", t.roundTrip("foobar"));
		assertEquals("", t.roundTrip(""));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a04_optionalContainingString(RoundTrip_Tester t) throws Exception {
		assertEquals("foobar", t.roundTrip(opt("foobar")).get());
		assertEquals("", t.roundTrip(opt("")).get());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a05_stringArray(RoundTrip_Tester t) throws Exception {
		assertArrayEquals(a("foo", null, "null", ""), t.roundTrip(a("foo", null, "null", ""), String[].class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a06_string2dArray(RoundTrip_Tester t) throws Exception {
		var x = a(a("foo", null, "null", ""),null);
		x = t.roundTrip(x, String[][].class);
		assertEquals("foo", x[0][0]);
		assertNull(x[0][1]);
		assertEquals("null", x[0][2]);
		assertEquals("", x[0][3]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a07_int(RoundTrip_Tester t) throws Exception {
		assertEquals(123, t.roundTrip(123).intValue());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a08_intArray(RoundTrip_Tester t) throws Exception {
		assertArrayEquals(ints(1,2,3), t.roundTrip(ints(1,2,3), int[].class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a09_int2dArray(RoundTrip_Tester t) throws Exception {
		var x = new int[][]{{1,2,3},null};
		x = t.roundTrip(x, int[][].class);
		assertEquals(1, x[0][0]);
		assertEquals(2, x[0][1]);
		assertEquals(3, x[0][2]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a10_int3dArray(RoundTrip_Tester t) throws Exception {
		var x = new int[][][]{{{1,2,3},{4,5,6},null},null};
		x = t.roundTrip(x, int[][][].class);
		assertEquals(1, x[0][0][0]);
		assertEquals(2, x[0][0][1]);
		assertEquals(3, x[0][0][2]);
		assertEquals(4, x[0][1][0]);
		assertEquals(5, x[0][1][1]);
		assertEquals(6, x[0][1][2]);
		assertNull(x[0][2]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a11_boolean(RoundTrip_Tester t) throws Exception {
		assertTrue(t.roundTrip(true));
		assertFalse(t.roundTrip(false));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a12_booleanArray(RoundTrip_Tester t) throws Exception {
		assertArrayEquals(booleans(true,false), t.roundTrip(booleans(true,false), boolean[].class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a13_boolean2dArray(RoundTrip_Tester t) throws Exception {
		var x = new boolean[][]{{true,false},null};
		x = t.roundTrip(x, boolean[][].class);
		assertTrue(x[0][0]);
		assertFalse(x[0][1]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a14_char(RoundTrip_Tester t) throws Exception {
		assertEquals('a', t.roundTrip('a', char.class).charValue());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a15_charArray(RoundTrip_Tester t) throws Exception {
		assertArrayEquals(chars('a',(char)0), t.roundTrip(chars('a',(char)0), char[].class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a16_char2dArray(RoundTrip_Tester t) throws Exception {
		var x = new char[][]{{'a',0},null};
		x = t.roundTrip(x, char[][].class);
		assertEquals('a', x[0][0]);
		assertEquals(0, x[0][1]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a17_float(RoundTrip_Tester t) throws Exception {
		assertEquals(1f, t.roundTrip(1f, float.class), 0.1f);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a18_floatArray(RoundTrip_Tester t) throws Exception {
		assertArrayEquals(floats(1f), t.roundTrip(floats(1f), float[].class), 0.1f);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a19_float2dArray(RoundTrip_Tester t) throws Exception {
		var x = new float[][]{{1f},null};
		x = t.roundTrip(x, float[][].class);
		assertEquals(1f, x[0][0], 0.1f);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a20_double(RoundTrip_Tester t) throws Exception {
		assertEquals(1d, t.roundTrip(1d, double.class), 0.1f);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a21_doubleArray(RoundTrip_Tester t) throws Exception {
		assertArrayEquals(doubles(1d), t.roundTrip(doubles(1d), double[].class), 0.1);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a22_double2dArray(RoundTrip_Tester t) throws Exception {
		var x = new double[][]{{1d},null};
		x = t.roundTrip(x, double[][].class);
		assertEquals(1d, x[0][0], 0.1f);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a23_long(RoundTrip_Tester t) throws Exception {
		assertEquals(1L, t.roundTrip(1L, long.class).longValue());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a24_longArray(RoundTrip_Tester t) throws Exception {
		assertArrayEquals(longs(1L), t.roundTrip(longs(1L), long[].class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a25_long2dArray(RoundTrip_Tester t) throws Exception {
		var x = new long[][]{{1L},null};
		x = t.roundTrip(x, long[][].class);
		assertEquals(1L, x[0][0]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a26_short(RoundTrip_Tester t) throws Exception {
		assertEquals((short)1, t.roundTrip((short)1, short.class).shortValue());
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a27_shortArray(RoundTrip_Tester t) throws Exception {
		assertArrayEquals(shorts((short)1), t.roundTrip(shorts((short)1), short[].class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a28_short2dArray(RoundTrip_Tester t) throws Exception {
		var x = new short[][]{{(short)1},null};
		x = t.roundTrip(x, short[][].class);
		assertEquals((short)1, x[0][0]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a29_integer(RoundTrip_Tester t) throws Exception {
		assertEquals(Integer.valueOf(123), t.roundTrip((Integer)123, Integer.class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a30_integerArray(RoundTrip_Tester t) throws Exception {
		var x = a(123, null);
		x = t.roundTrip(x, Integer[].class);
		assertEquals(Integer.valueOf(123), x[0]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a31_integer2dArray(RoundTrip_Tester t) throws Exception {
		var x = a(a(123,null),null);
		x = t.roundTrip(x, Integer[][].class);
		assertEquals(Integer.valueOf(123), x[0][0]);
		assertNull(x[0][1]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a32_integer3dArray(RoundTrip_Tester t) throws Exception {
		var x = a(a(a(123,null),null),null);
		x = t.roundTrip(x, Integer[][][].class);
		assertEquals(Integer.valueOf(123), x[0][0][0]);
		assertNull(x[0][0][1]);
		assertNull(x[0][1]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a33_booleanObject(RoundTrip_Tester t) throws Exception {
		assertTrue(t.roundTrip(Boolean.TRUE, Boolean.class));
		assertFalse(t.roundTrip(Boolean.FALSE, Boolean.class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a34_booleanObjectArray(RoundTrip_Tester t) throws Exception {
		var x = a(true,false,null);
		x = t.roundTrip(x, Boolean[].class);
		assertTrue(x[0]);
		assertFalse(x[1]);
		assertNull(x[2]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a35_booleanObject2dArray(RoundTrip_Tester t) throws Exception {
		var x = a(a(true,false,null),null);
		x = t.roundTrip(x, Boolean[][].class);
		assertTrue(x[0][0]);
		assertFalse(x[0][1]);
		assertNull(x[0][2]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a36_character(RoundTrip_Tester t) throws Exception {
		assertEquals(Character.valueOf('a'), t.roundTrip((Character)'a', Character.class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a37_characterArray(RoundTrip_Tester t) throws Exception {
		var x = a('a',null);
		x = t.roundTrip(x, Character[].class);
		assertEquals(Character.valueOf('a'), x[0]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a38_character2dArray(RoundTrip_Tester t) throws Exception {
		var x = a(a('a',null),null);
		x = t.roundTrip(x, Character[][].class);
		assertEquals(Character.valueOf('a'), x[0][0]);
		assertNull(x[0][1]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a39_floatObject(RoundTrip_Tester t) throws Exception {
		assertEquals(Float.valueOf(1f), t.roundTrip((Float)1f, Float.class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a40_floatObjectArray(RoundTrip_Tester t) throws Exception {
		var x = a(1f, null);
		x = t.roundTrip(x, Float[].class);
		assertEquals(Float.valueOf(1f), x[0]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a41_floatObject2dArray(RoundTrip_Tester t) throws Exception {
		var x = a(a(1f,null),null);
		x = t.roundTrip(x, Float[][].class);
		assertEquals(Float.valueOf(1f), x[0][0]);
		assertNull(x[0][1]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a42_doubleObject(RoundTrip_Tester t) throws Exception {
		assertEquals(Double.valueOf(1d), t.roundTrip((Double)1d, Double.class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a43_doubleObjectArray(RoundTrip_Tester t) throws Exception {
		var x = a(1d,null);
		x = t.roundTrip(x, Double[].class);
		assertEquals(Double.valueOf(1d), x[0]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a44_doubleObject2dArray(RoundTrip_Tester t) throws Exception {
		var x = a(a(1d,null),null);
		x = t.roundTrip(x, Double[][].class);
		assertEquals(Double.valueOf(1d), x[0][0]);
		assertNull(x[0][1]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a45_longObject(RoundTrip_Tester t) throws Exception {
		assertEquals(Long.valueOf(1L), t.roundTrip((Long)1L, Long.class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a46_longObjectArray(RoundTrip_Tester t) throws Exception {
		var x = a(1L, null);
		x = t.roundTrip(x, Long[].class);
		assertEquals(Long.valueOf(1L), x[0]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a47_longObject2dArray(RoundTrip_Tester t) throws Exception {
		var x = a(a(1L,null),null);
		x = t.roundTrip(x, Long[][].class);
		assertEquals(Long.valueOf(1L), x[0][0]);
		assertNull(x[0][1]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a48_shortObject(RoundTrip_Tester t) throws Exception {
		assertEquals(Short.valueOf((short)1), t.roundTrip((Short)(short)1, Short.class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a49_shortObjectArray(RoundTrip_Tester t) throws Exception {
		var x = a((short)1,null);
		x = t.roundTrip(x, Short[].class);
		assertEquals(Short.valueOf((short)1), x[0]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a50_shortObject2dArray(RoundTrip_Tester t) throws Exception {
		var x = a(a((short)1,null),null);
		x = t.roundTrip(x, Short[][].class);
		assertEquals(Short.valueOf((short)1), x[0][0]);
		assertNull(x[0][1]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a51_jsonMap(RoundTrip_Tester t) throws Exception {
		var x = JsonMap.ofJson("{a:'b',c:123,d:false,e:null,f:[123,'abc',true,false,null]}");
		x = t.roundTrip(x);
		assertEquals("b", x.get("a"));
		assertEquals(123, x.get("c"));
		assertEquals(false, x.get("d"));
		assertNull(x.get("e"));
		var x2 = (List)x.get("f");
		assertEquals(123, x2.get(0));
		assertEquals("abc", x2.get(1));
		assertEquals(true, x2.get(2));
		assertEquals(false, x2.get(3));
		assertNull(x2.get(4));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a52_jsonList(RoundTrip_Tester t) throws Exception {
		var x = new JsonList("['abc',123,true,false,null,{a:'b'}]");
		x = t.roundTrip(x);
		assertEquals("abc", x.get(0));
		assertEquals(123, x.get(1));
		assertEquals(true, x.get(2));
		assertEquals(false, x.get(3));
		assertNull(x.get(4));
		var m = (Map)x.get(5);
		assertEquals("b", m.get("a"));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a53_treeMap(RoundTrip_Tester t) throws Exception {
		var x = new TreeMap<String,Object>();
		x.put("a", 1);
		x.put("b", 2);
		x.put("c", 3);
		x = t.roundTrip(x, TreeMap.class);
		assertEquals(1, x.get("a"));
		assertEquals(2, x.get("b"));
		assertEquals(3, x.get("c"));

		x = new TreeMap<>();
		x.put("a", true);
		x.put("b", false);
		x.put("c", null);
		x.put("d", "foo");
		x.put("null", "baz");
		x.put("a\"a", "a\"a");
		x.put("b'b", "b'b");
		x.put("\"cc\"", "\"cc\"");
		x.put("'dd'", "'dd'");
		x = t.roundTrip(x, TreeMap.class);
		assertEquals(true, x.get("a"));
		assertEquals(false, x.get("b"));
		assertNull(x.get("c"));
		assertEquals("foo", x.get("d"));
		assertEquals("baz", x.get("null"));
		assertEquals("a\"a", x.get("a\"a"));
		assertEquals("b'b", x.get("b'b"));
		assertEquals("\"cc\"", x.get("\"cc\""));
		assertEquals("'dd'", x.get("'dd'"));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a54_linkedHashMap(RoundTrip_Tester t) throws Exception {
		var x = new LinkedHashMap<String,Object>();
		x.put("a", true);
		x.put("b", false);
		x.put("c", null);
		x.put("d", "foo");
		x.put(null, "bar");
		x.put("null", "null");
		x.put("true", "true");
		x.put("false", "false");
		x.put("a\"a", "a\"a");
		x.put("b'b", "b'b");
		x.put("\"cc\"", "\"cc\"");
		x.put("'dd'", "'dd'");
		x.put("<ee>", "<ee>");
		x.put("<ff/>", "<ff/>");
		x.put("</gg>", "</gg>");
		x.put("<>", "<>");
		x.put("{}", "{}");
		x.put("[]", "[]");
		x.put("&", "&");
		x.put("?", "?");
		x.put("/", "/");
		x.put("\b", "\b");
		x.put("\\b", "\\b");
		x.put("\n", "\n");
		x.put("\\n", "\\n");
		x.put("\t", "\t");
		x.put("\\t", "\\t");
		x.put("\f", "\f");
		x.put("\\f", "\\f");
		x.put("\\", "\\");
		x.put("\\\\", "\\\\");
		x.put("\u2345", "\u2345");
		x.put("\\u2345", "\\u2345");
		x.put("\\\u2345", "\\\u2345");
		x.put("<>{}[]&?/\b\n\t\f\\\\\u2345", "<>{}[]&?/\b\n\t\f\\\\\u2345");
		x = t.roundTrip(x, LinkedHashMap.class);
		assertEquals(true, x.get("a"));
		assertEquals(false, x.get("b"));
		assertNull(x.get("c"));
		assertEquals("foo", x.get("d"));
		assertEquals("bar", x.get(null));
		assertEquals("null", x.get("null"));
		assertEquals("true", x.get("true"));
		assertEquals("false", x.get("false"));
		assertEquals("a\"a", x.get("a\"a"));
		assertEquals("b'b", x.get("b'b"));
		assertEquals("\"cc\"", x.get("\"cc\""));
		assertEquals("'dd'", x.get("'dd'"));
		assertEquals("<ee>", x.get("<ee>"));
		assertEquals("<ff/>", x.get("<ff/>"));
		assertEquals("</gg>", x.get("</gg>"));
		assertEquals("<>", x.get("<>"));
		assertEquals("{}", x.get("{}"));
		assertEquals("[]", x.get("[]"));
		assertEquals("&", x.get("&"));
		assertEquals("?", x.get("?"));
		assertEquals("/", x.get("/"));
		assertEquals("\b", x.get("\b"));
		assertEquals("\\b", x.get("\\b"));
		assertEquals("\n", x.get("\n"));
		assertEquals("\\n", x.get("\\n"));
		assertEquals("\t", x.get("\t"));
		assertEquals("\\t", x.get("\\t"));
		assertEquals("\f", x.get("\f"));
		assertEquals("\\f", x.get("\\f"));
		assertEquals("\\", x.get("\\"));
		assertEquals("\\\\", x.get("\\\\"));
		assertEquals("\u2345", x.get("\u2345"));
		assertEquals("\\u2345", x.get("\\u2345"));
		assertEquals("\\\u2345", x.get("\\\u2345"));
		assertEquals("<>{}[]&?/\b\n\t\f\\\\\u2345", x.get("<>{}[]&?/\b\n\t\f\\\\\u2345"));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a55_vector(RoundTrip_Tester t) {
		var x = new Vector<Integer>();
		x.add(1);
		x.add(2);
		x.add(3);
		assertDoesNotThrow(()->t.roundTrip(x, Vector.class, Integer.class));
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a56_extendedUnicode(RoundTrip_Tester t) throws Exception {
		// Test 4-byte UTF-8 character
		assertEquals("𤭢𤭢", t.roundTrip("𤭢𤭢"));
	}
}