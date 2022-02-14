// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.a.rttests;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"unchecked","rawtypes"})
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripSimpleObjectsTest extends RoundTripTest {

	public RoundTripSimpleObjectsTest(String label, Serializer.Builder s, Parser.Builder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// testNull
	//====================================================================================================
	@Test
	public void testNull() throws Exception {
		String t = null;
		t = roundTrip(t);
		assertNull(t);
	}

	//====================================================================================================
	// testOptional
	//====================================================================================================
	@Test
	public void testOptional() throws Exception {
		Optional<String> o = empty();
		o = roundTrip(o);
		assertFalse(o.isPresent());
	}

	//====================================================================================================
	// testString
	//====================================================================================================
	@Test
	public void testString() throws Exception {
		String t = "foobar";
		t = roundTrip(t);
		assertEquals("foobar", t);
		t = "";
		t = roundTrip(t);
		assertEquals("", t);
	}

	//====================================================================================================
	// testOptional
	//====================================================================================================
	@Test
	public void testOptionalContainingString() throws Exception {
		Optional<String> o = optional("foobar");
		o = roundTrip(o);
		assertEquals("foobar", o.get());
		o = optional("");
		o = roundTrip(o);
		assertEquals("", o.get());
	}

	//====================================================================================================
	// testStringArray
	//====================================================================================================
	@Test
	public void testStringArray() throws Exception {
		String[] t = {"foo", null, "null", ""};
		t = roundTrip(t, String[].class);
		assertEquals("foo", t[0]);
		assertNull(t[1]);
		assertEquals("null", t[2]);
		assertEquals("", t[3]);
	}

	//====================================================================================================
	// testString2dArray
	//====================================================================================================
	@Test
	public void testString2dArray() throws Exception {
		String[][] t = {{"foo", null, "null", ""},null};
		t = roundTrip(t, String[][].class);
		assertEquals("foo", t[0][0]);
		assertNull(t[0][1]);
		assertEquals("null", t[0][2]);
		assertEquals("", t[0][3]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testInt
	//====================================================================================================
	@Test
	public void testInt() throws Exception {
		int t = 123;
		t = roundTrip(t);
		assertEquals(123, t);
	}

	//====================================================================================================
	// testIntArray
	//====================================================================================================
	@Test
	public void testIntArray() throws Exception {
		int[] t = roundTrip(new int[]{1,2,3}, int[].class);
		assertEquals(1, t[0]);
		assertEquals(2, t[1]);
		assertEquals(3, t[2]);
	}

	//====================================================================================================
	// testInt2dArray
	//====================================================================================================
	@Test
	public void testInt2dArray() throws Exception {
		int[][] t = {{1,2,3},null};
		t = roundTrip(t, int[][].class);
		assertEquals(1, t[0][0]);
		assertEquals(2, t[0][1]);
		assertEquals(3, t[0][2]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testInt3dArray
	//====================================================================================================
	@Test
	public void testInt3dArray() throws Exception {
		int[][][] t = {{{1,2,3},{4,5,6},null},null};
		t = roundTrip(t, int[][][].class);
		assertEquals(1, t[0][0][0]);
		assertEquals(2, t[0][0][1]);
		assertEquals(3, t[0][0][2]);
		assertEquals(4, t[0][1][0]);
		assertEquals(5, t[0][1][1]);
		assertEquals(6, t[0][1][2]);
		assertNull(t[0][2]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testBoolean
	//====================================================================================================
	@Test
	public void testBoolean() throws Exception {
		boolean t = true;
		t = roundTrip(t);
		assertTrue(t);
		t = false;
		t = roundTrip(t);
		assertFalse(t);
	}

	//====================================================================================================
	// testBooleanArray
	//====================================================================================================
	@Test
	public void testBooleanArray() throws Exception {
		boolean[] t = {true,false};
		t = roundTrip(t, boolean[].class);
		assertTrue(t[0]);
		assertFalse(t[1]);
	}

	//====================================================================================================
	// testBoolean2dArray
	//====================================================================================================
	@Test
	public void testBoolean2dArray() throws Exception {
		boolean[][] t = {{true,false},null};
		t = roundTrip(t, boolean[][].class);
		assertTrue(t[0][0]);
		assertFalse(t[0][1]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testChar
	//====================================================================================================
	@Test
	public void testChar() throws Exception {
		char t = 'a';
		t = roundTrip(t, char.class);
		assertEquals('a', t);
	}

	//====================================================================================================
	// testCharArray
	//====================================================================================================
	@Test
	public void testCharArray() throws Exception {
		char[] t = {'a',0};
		t = roundTrip(t, char[].class);
		assertEquals('a', t[0]);
		assertEquals(0, t[1]);
	}

	//====================================================================================================
	// testChar2dArray
	//====================================================================================================
	@Test
	public void testChar2dArray() throws Exception {
		char[][] t = {{'a',0},null};
		t = roundTrip(t, char[][].class);
		assertEquals('a', t[0][0]);
		assertEquals(0, t[0][1]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testFloat
	//====================================================================================================
	@Test
	public void testFloat() throws Exception {
		float t = 1f;
		t = roundTrip(t, float.class);
		assertEquals(1f, t, 0.1f);
	}

	//====================================================================================================
	// testFloatArray
	//====================================================================================================
	@Test
	public void testFloatArray() throws Exception {
		float[] t = {1f};
		t = roundTrip(t, float[].class);
		assertEquals(1f, t[0], 0.1f);
	}

	//====================================================================================================
	// testFloat2dArray
	//====================================================================================================
	@Test
	public void testFloat2dArray() throws Exception {
		float[][] t = {{1f},null};
		t = roundTrip(t, float[][].class);
		assertEquals(1f, t[0][0], 0.1f);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testDouble
	//====================================================================================================
	@Test
	public void testDouble() throws Exception {
		double t = 1d;
		t = roundTrip(t, double.class);
		assertEquals(1d, t, 0.1f);
	}

	//====================================================================================================
	// testDoubleArray
	//====================================================================================================
	@Test
	public void testDoubleArray() throws Exception {
		double[] t = {1d};
		t = roundTrip(t, double[].class);
		assertEquals(1d, t[0], 0.1f);
	}

	//====================================================================================================
	// testDouble2dArray
	//====================================================================================================
	@Test
	public void testDouble2dArray() throws Exception {
		double[][] t = {{1d},null};
		t = roundTrip(t, double[][].class);
		assertEquals(1d, t[0][0], 0.1f);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testLong
	//====================================================================================================
	@Test
	public void testLong() throws Exception {
		long t = 1l;
		t = roundTrip(t, long.class);
		assertEquals(1l, t);
	}

	//====================================================================================================
	// testLongArray
	//====================================================================================================
	@Test
	public void testLongArray() throws Exception {
		long[] t = {1l};
		t = roundTrip(t, long[].class);
		assertEquals(1l, t[0]);
	}

	//====================================================================================================
	// testLong2dArray
	//====================================================================================================
	@Test
	public void testLong2dArray() throws Exception {
		long[][] t = {{1l},null};
		t = roundTrip(t, long[][].class);
		assertEquals(1l, t[0][0]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testShort
	//====================================================================================================
	@Test
	public void testShort() throws Exception {
		short t = (short)1;
		t = roundTrip(t, short.class);
		assertEquals(1l, t);
	}

	//====================================================================================================
	// testShortArray
	//====================================================================================================
	@Test
	public void testShortArray() throws Exception {
		short[] t = {(short)1};
		t = roundTrip(t, short[].class);
		assertEquals(1l, t[0]);
	}

	//====================================================================================================
	// testShort2dArray
	//====================================================================================================
	@Test
	public void testShort2dArray() throws Exception {
		short[][] t = {{(short)1},null};
		t = roundTrip(t, short[][].class);
		assertEquals((short)1, t[0][0]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testInteger
	//====================================================================================================
	@Test
	public void testInteger() throws Exception {
		Integer t = 123;
		t = roundTrip(t, Integer.class);
		assertEquals(new Integer(123), t);
	}

	//====================================================================================================
	// testIntegerArray
	//====================================================================================================
	@Test
	public void testIntegerArray() throws Exception {
		Integer[] t = {123, null};
		t = roundTrip(t, Integer[].class);
		assertEquals(new Integer(123), t[0]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testInteger2dArray
	//====================================================================================================
	@Test
	public void testInteger2dArray() throws Exception {
		Integer[][] t = {{123,null},null};
		t = roundTrip(t, Integer[][].class);
		assertEquals(new Integer(123), t[0][0]);
		assertNull(t[0][1]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testInteger3dArray
	//====================================================================================================
	@Test
	public void testInteger3dArray() throws Exception {
		Integer[][][] t = {{{123,null},null},null};
		t = roundTrip(t, Integer[][][].class);
		assertEquals(new Integer(123), t[0][0][0]);
		assertNull(t[0][0][1]);
		assertNull(t[0][1]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testBooleanObject
	//====================================================================================================
	@Test
	public void testBooleanObject() throws Exception {
		Boolean t = Boolean.TRUE;
		t = roundTrip(t, Boolean.class);
		assertTrue(t);
		t = Boolean.FALSE;
		t = roundTrip(t, Boolean.class);
		assertFalse(t);
	}

	//====================================================================================================
	// testBooleanObjectArray
	//====================================================================================================
	@Test
	public void testBooleanObjectArray() throws Exception {
		Boolean[] t = {true,false,null};
		t = roundTrip(t, Boolean[].class);
		assertTrue(t[0]);
		assertFalse(t[1]);
		assertNull(t[2]);
	}

	//====================================================================================================
	// testBooleanObject2dArray
	//====================================================================================================
	@Test
	public void testBooleanObject2dArray() throws Exception {
		Boolean[][] t = {{true,false,null},null};
		t = roundTrip(t, Boolean[][].class);
		assertTrue(t[0][0]);
		assertFalse(t[0][1]);
		assertNull(t[0][2]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testCharacter
	//====================================================================================================
	@Test
	public void testCharacter() throws Exception {
		Character t = 'a';
		t = roundTrip(t, Character.class);
		assertEquals(new Character('a'), t);
	}

	//====================================================================================================
	// testCharacterArray
	//====================================================================================================
	@Test
	public void testCharacterArray() throws Exception {
		Character[] t = {'a',null};
		t = roundTrip(t, Character[].class);
		assertEquals(new Character('a'), t[0]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testCharacter2dArray
	//====================================================================================================
	@Test
	public void testCharacter2dArray() throws Exception {
		Character[][] t = {{'a',null},null};
		t = roundTrip(t, Character[][].class);
		assertEquals(new Character('a'), t[0][0]);
		assertNull(t[0][1]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testFloatObject
	//====================================================================================================
	@Test
	public void testFloatObject() throws Exception {
		Float t = 1f;
		t = roundTrip(t, Float.class);
		assertEquals(new Float(1f), t);
	}

	//====================================================================================================
	// testFloatObjectArray
	//====================================================================================================
	@Test
	public void testFloatObjectArray() throws Exception {
		Float[] t = {1f, null};
		t = roundTrip(t, Float[].class);
		assertEquals(new Float(1f), t[0]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testFloatObject2dArray
	//====================================================================================================
	@Test
	public void testFloatObject2dArray() throws Exception {
		Float[][] t = {{1f,null},null};
		t = roundTrip(t, Float[][].class);
		assertEquals(new Float(1f), t[0][0]);
		assertNull(t[0][1]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testDoubleObject
	//====================================================================================================
	@Test
	public void testDoubleObject() throws Exception {
		Double t = 1d;
		t = roundTrip(t, Double.class);
		assertEquals(new Double(1d), t);
	}

	//====================================================================================================
	// testDoubleObjectArray
	//====================================================================================================
	@Test
	public void testDoubleObjectArray() throws Exception {
		Double[] t = {1d,null};
		t = roundTrip(t, Double[].class);
		assertEquals(new Double(1d), t[0]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testDoubleObject2dArray
	//====================================================================================================
	@Test
	public void testDoubleObject2dArray() throws Exception {
		Double[][] t = {{1d,null},null};
		t = roundTrip(t, Double[][].class);
		assertEquals(new Double(1d), t[0][0]);
		assertNull(t[0][1]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testLongObject
	//====================================================================================================
	@Test
	public void testLongObject() throws Exception {
		Long t = 1l;
		t = roundTrip(t, Long.class);
		assertEquals(new Long(1l), t);
	}

	//====================================================================================================
	// testLongObjectArray
	//====================================================================================================
	@Test
	public void testLongObjectArray() throws Exception {
		Long[] t = {1l, null};
		t = roundTrip(t, Long[].class);
		assertEquals(new Long(1l), t[0]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testLongObject2dArray
	//====================================================================================================
	@Test
	public void testLongObject2dArray() throws Exception {
		Long[][] t = {{1l,null},null};
		t = roundTrip(t, Long[][].class);
		assertEquals(new Long(1l), t[0][0]);
		assertNull(t[0][1]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testShortObject
	//====================================================================================================
	@Test
	public void testShortObject() throws Exception {
		Short t = (short)1;
		t = roundTrip(t, Short.class);
		assertEquals(new Short((short)1), t);
	}

	//====================================================================================================
	// testShortObjectArray
	//====================================================================================================
	@Test
	public void testShortObjectArray() throws Exception {
		Short[] t = {(short)1,null};
		t = roundTrip(t, Short[].class);
		assertEquals(new Short((short)1), t[0]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testShortObject2dArray
	//====================================================================================================
	@Test
	public void testShortObject2dArray() throws Exception {
		Short[][] t = {{(short)1,null},null};
		t = roundTrip(t, Short[][].class);
		assertEquals(new Short((short)1), t[0][0]);
		assertNull(t[0][1]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// testOMap
	//====================================================================================================
	@Test
	public void testOMap() throws Exception {
		OMap t = OMap.ofJson("{a:'b',c:123,d:false,e:null,f:[123,'abc',true,false,null]}");
		t = roundTrip(t);
		assertEquals("b", t.get("a"));
		assertEquals(123, t.get("c"));
		assertEquals(false, t.get("d"));
		assertNull(t.get("e"));
		List l = (List)t.get("f");
		assertEquals(123, l.get(0));
		assertEquals("abc", l.get(1));
		assertEquals(true, l.get(2));
		assertEquals(false, l.get(3));
		assertNull(l.get(4));
	}

	//====================================================================================================
	// testOList
	//====================================================================================================
	@Test
	public void testOList() throws Exception {
		OList t = new OList("['abc',123,true,false,null,{a:'b'}]");
		t = roundTrip(t);
		assertEquals("abc", t.get(0));
		assertEquals(123, t.get(1));
		assertEquals(true, t.get(2));
		assertEquals(false, t.get(3));
		assertNull(t.get(4));
		Map m = (Map)t.get(5);
		assertEquals("b", m.get("a"));
	}

	//====================================================================================================
	// testTreeMap
	//====================================================================================================
	@Test
	public void testTreeMap() throws Exception {
		TreeMap t = new TreeMap();
		t.put("a", 1);
		t.put("b", 2);
		t.put("c", 3);
		t = roundTrip(t, TreeMap.class);
		assertEquals(1, t.get("a"));
		assertEquals(2, t.get("b"));
		assertEquals(3, t.get("c"));

		t = new TreeMap();
		t.put("a", true);
		t.put("b", false);
		t.put("c", null);
		t.put("d", "foo");
		t.put("null", "baz");
		t.put("a\"a", "a\"a");
		t.put("b'b", "b'b");
		t.put("\"cc\"", "\"cc\"");
		t.put("'dd'", "'dd'");
		t = roundTrip(t, TreeMap.class);
		assertEquals(true, t.get("a"));
		assertEquals(false, t.get("b"));
		assertNull(t.get("c"));
		assertEquals("foo", t.get("d"));
		assertEquals("baz", t.get("null"));
		assertEquals("a\"a", t.get("a\"a"));
		assertEquals("b'b", t.get("b'b"));
		assertEquals("\"cc\"", t.get("\"cc\""));
		assertEquals("'dd'", t.get("'dd'"));
	}

	//====================================================================================================
	// testLinkedHashMap
	//====================================================================================================
	@Test
	public void testLinkedHashMap() throws Exception {
		LinkedHashMap t = new LinkedHashMap();
		t.put("a", true);
		t.put("b", false);
		t.put("c", null);
		t.put("d", "foo");
		t.put(null, "bar");
		t.put("null", "null");
		t.put("true", "true");
		t.put("false", "false");
		t.put("a\"a", "a\"a");
		t.put("b'b", "b'b");
		t.put("\"cc\"", "\"cc\"");
		t.put("'dd'", "'dd'");
		t.put("<ee>", "<ee>");
		t.put("<ff/>", "<ff/>");
		t.put("</gg>", "</gg>");
		t.put("<>", "<>");
		t.put("{}", "{}");
		t.put("[]", "[]");
		t.put("&", "&");
		t.put("?", "?");
		t.put("/", "/");
		t.put("\b", "\b");
		t.put("\\b", "\\b");
		t.put("\n", "\n");
		t.put("\\n", "\\n");
		t.put("\t", "\t");
		t.put("\\t", "\\t");
		t.put("\f", "\f");
		t.put("\\f", "\\f");
		t.put("\\", "\\");
		t.put("\\\\", "\\\\");
		t.put("\u2345", "\u2345");
		t.put("\\u2345", "\\u2345");
		t.put("\\\u2345", "\\\u2345");
		t.put("<>{}[]&?/\b\n\t\f\\\\\u2345", "<>{}[]&?/\b\n\t\f\\\\\u2345");
		t = roundTrip(t, LinkedHashMap.class);
		assertEquals(true, t.get("a"));
		assertEquals(false, t.get("b"));
		assertNull(t.get("c"));
		assertEquals("foo", t.get("d"));
		assertEquals("bar", t.get(null));
		assertEquals("null", t.get("null"));
		assertEquals("true", t.get("true"));
		assertEquals("false", t.get("false"));
		assertEquals("a\"a", t.get("a\"a"));
		assertEquals("b'b", t.get("b'b"));
		assertEquals("\"cc\"", t.get("\"cc\""));
		assertEquals("'dd'", t.get("'dd'"));
		assertEquals("<ee>", t.get("<ee>"));
		assertEquals("<ff/>", t.get("<ff/>"));
		assertEquals("</gg>", t.get("</gg>"));
		assertEquals("<>", t.get("<>"));
		assertEquals("{}", t.get("{}"));
		assertEquals("[]", t.get("[]"));
		assertEquals("&", t.get("&"));
		assertEquals("?", t.get("?"));
		assertEquals("/", t.get("/"));
		assertEquals("\b", t.get("\b"));
		assertEquals("\\b", t.get("\\b"));
		assertEquals("\n", t.get("\n"));
		assertEquals("\\n", t.get("\\n"));
		assertEquals("\t", t.get("\t"));
		assertEquals("\\t", t.get("\\t"));
		assertEquals("\f", t.get("\f"));
		assertEquals("\\f", t.get("\\f"));
		assertEquals("\\", t.get("\\"));
		assertEquals("\\\\", t.get("\\\\"));
		assertEquals("\u2345", t.get("\u2345"));
		assertEquals("\\u2345", t.get("\\u2345"));
		assertEquals("\\\u2345", t.get("\\\u2345"));
		assertEquals("<>{}[]&?/\b\n\t\f\\\\\u2345", t.get("<>{}[]&?/\b\n\t\f\\\\\u2345"));
	}

	//====================================================================================================
	// testVector
	//====================================================================================================
	@Test
	public void testVector() throws Exception {
		Vector<Integer> t = new Vector<>();
		t.add(1);
		t.add(2);
		t.add(3);
		t = roundTrip(t, Vector.class, Integer.class);
	}

	//====================================================================================================
	// testNull
	//====================================================================================================
	@Test
	public void testExtendedUnicode() throws Exception {
		// Test 4-byte UTF-8 character
		String t = "𤭢𤭢";
		t = roundTrip(t);
		assertEquals("𤭢𤭢", t);
	}
}
