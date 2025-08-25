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
package org.apache.juneau.oapi;

import static org.apache.juneau.httppart.HttpPartSchema.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.DateUtils.*;
import static org.junit.Assert.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

import jakarta.xml.bind.*;

/**
 * Tests the OpenApiSerializer and OpenApiParser classes.
 */
public class OpenApiTest extends SimpleTestBase {

	public static final OpenApiSerializer DS = OpenApiSerializer.DEFAULT;
	public static final OpenApiParser DP = OpenApiParser.DEFAULT;

	private String serialize(HttpPartSchema schema, Object in) throws Exception {
		return DS.serialize(null, schema, in);
	}

	private <T> T parse(HttpPartSchema schema, String in, Class<T> c, Class<?>...args) throws Exception {
		return DP.parse(null, schema, in, DP.getClassMeta(c, args));
	}

	@BeforeEach void before() {
		TestUtils.setTimeZone("GMT");
	}

	@AfterEach void after() {
		TestUtils.unsetTimeZone();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Type == NO_TYPE
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01a_noType_formatDefault() throws Exception {
		String in = "foo";
		HttpPartSchema ps = T_NONE;
		String s = serialize(ps, in);
		assertEquals("foo", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void a01b_noType_formatDefault_null() throws Exception {
		String in = null;
		HttpPartSchema ps = T_NONE;
		String s = serialize(ps, in);
		assertEquals("null", s);
	}

	@Test void a02a_noType_formatByte() throws Exception {
		String in = "foo";
		HttpPartSchema ps = tNone().fByte().build();
		String s = serialize(ps, in);
		assertEquals("Zm9v", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void a02b_noType_formatByte_null() throws Exception {
		String in = null;
		HttpPartSchema ps = tNone().fByte().build();
		String s = serialize(ps, in);
		assertEquals("null", s);
	}

	@Test void a03a_noType_formatBinary() throws Exception {
		String in = "foo";
		HttpPartSchema ps = tNone().fBinary().build();
		String s = serialize(ps, in);
		assertEquals("666F6F", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void a03b_noType_formatBinary_null() throws Exception {
		String in = null;
		HttpPartSchema ps = tNone().fBinary().build();
		String s = serialize(ps, in);
		assertEquals("null", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void a04a_noType_formatBinarySpaced() throws Exception {
		String in = "foo";
		HttpPartSchema ps = tNone().fBinarySpaced().build();
		String s = serialize(ps, in);
		assertEquals("66 6F 6F", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void a04b_noType_formatBinarySpaced_null() throws Exception {
		String in = null;
		HttpPartSchema ps = tNone().fBinarySpaced().build();
		String s = serialize(ps, in);
		assertEquals("null", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void a05_noType_formatDate_String() throws Exception {
		String in = "2012-12-21";
		HttpPartSchema ps = tNone().fDate().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void a06_noType_formatDate_Calendar() throws Exception {
		Calendar in = cal("2012-12-21");
		HttpPartSchema ps = tNone().fDate().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21Z", s);
	}

	@Test void a07_noType_formatDate_Date() throws Exception {
		Date in = cal("2012-12-21").getTime();
		HttpPartSchema ps = tNone().fDate().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21Z", s);
	}

	@Test void a08_noType_formatDate_Temporal() throws Exception {
		Instant in = cal("2012-12-21").toInstant();
		HttpPartSchema ps = tNone().fDate().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21Z", s);
	}

	@Test void a09_noType_formatDate_Other() throws Exception {
		var in = new StringBuilder("2012-12-21");
		HttpPartSchema ps = tNone().fDate().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21", s);
		String r = parse(ps, s, String.class);
		assertEquals(in.toString(), r);
	}

	@Test void a10_noType_formatDate_null() throws Exception {
		String in = null;
		HttpPartSchema ps = tNone().fDate().build();
		String s = serialize(ps, in);
		assertEquals("null", s);
		String r = parse(ps, s, String.class);
		assertEquals(null, r);
	}

	@Test void a11_noType_formatDateTime_String() throws Exception {
		String in = "2012-12-21T00:00:00";
		HttpPartSchema ps = tNone().fDateTime().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void a12_noType_formatDateTime_Calendar() throws Exception {
		Calendar in = cal("2012-12-21");
		HttpPartSchema ps = tNone().fDateTime().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21T00:00:00Z", s);
	}

	@Test void a13_noType_formatDateTime_Date() throws Exception {
		Date in = cal("2012-12-21").getTime();
		HttpPartSchema ps = tNone().fDateTime().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21T00:00:00Z", s);
	}

	@Test void a14_noType_formatDateTime_Temporal() throws Exception {
		Instant in = cal("2012-12-21").toInstant();
		HttpPartSchema ps = tNone().fDateTime().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21T00:00:00Z", s);
	}

	@Test void a15_noType_formatDate_Other() throws Exception {
		var in = new StringBuilder("2012-12-21T00:00:00");
		HttpPartSchema ps = tNone().fDateTime().build();
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00", s);
		String r = parse(ps, s, String.class);
		assertEquals(in.toString(), r);
	}

	@Test void a16_noType_formatDate_null() throws Exception {
		String in = null;
		HttpPartSchema ps = tNone().fDateTime().build();
		String s = serialize(ps, in);
		assertEquals("null", s);
		String r = parse(ps, s, String.class);
		assertEquals(null, r);
	}

	@Test void a17_noType_formatUon() throws Exception {
		String in = "foo,bar";
		HttpPartSchema ps = tNone().fUon().build();
		String s = serialize(ps, in);
		assertEquals("'foo,bar'", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Type == STRING
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_typeString_formatDefault() throws Exception {
		String in = "foo";
		HttpPartSchema ps = T_STRING;
		String s = serialize(ps, in);
		assertEquals("foo", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void b02_typeString_formatByte() throws Exception {
		String in = "foo";
		HttpPartSchema ps = T_BYTE;
		String s = serialize(ps, in);
		assertEquals("Zm9v", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void b03_typeString_formatBinary() throws Exception {
		String in = "foo";
		HttpPartSchema ps = T_BINARY;
		String s = serialize(ps, in);
		assertEquals("666F6F", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void b04_typeString_formatBinarySpaced() throws Exception {
		String in = "foo";
		HttpPartSchema ps = T_BINARY_SPACED;
		String s = serialize(ps, in);
		assertEquals("66 6F 6F", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void b05_typeString_formatDate_String() throws Exception {
		String in = "2012-12-21";
		HttpPartSchema ps = T_DATE;
		String s = serialize(ps, in);
		assertEquals("2012-12-21", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void b06_typeString_formatDate_Calendar() throws Exception {
		Calendar in = cal("2012-12-21");
		HttpPartSchema ps = T_DATE;
		String s = serialize(ps, in);
		assertEquals("2012-12-21Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21Z", s);
	}

	@Test void b07_typeString_formatDate_Date() throws Exception {
		Date in = cal("2012-12-21").getTime();
		HttpPartSchema ps = T_DATE;
		String s = serialize(ps, in);
		assertEquals("2012-12-21Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21Z", s);
	}

	@Test void b08_typeString_formatDate_Temporal() throws Exception {
		Instant in = cal("2012-12-21").toInstant();
		HttpPartSchema ps = T_DATE;
		String s = serialize(ps, in);
		assertEquals("2012-12-21Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21Z", s);
	}

	@Test void b09_typeString_formatDate_Other() throws Exception {
		var in = new StringBuilder("2012-12-21");
		HttpPartSchema ps = T_DATE;
		String s = serialize(ps, in);
		assertEquals("2012-12-21", s);
		String r = parse(ps, s, String.class);
		assertEquals(in.toString(), r);
	}

	@Test void b10_typeString_formatDate_null() throws Exception {
		String in = null;
		HttpPartSchema ps = T_DATE;
		String s = serialize(ps, in);
		assertEquals("null", s);
		String r = parse(ps, s, String.class);
		assertEquals(null, r);
	}

	@Test void b11_typeString_formatDateTime_String() throws Exception {
		String in = "2012-12-21T00:00:00";
		HttpPartSchema ps = T_DATETIME;
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void b12_typeString_formatDateTime_Calendar() throws Exception {
		Calendar in = cal("2012-12-21");
		HttpPartSchema ps = T_DATETIME;
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21T00:00:00Z", s);
	}

	@Test void b13_typeString_formatDateTime_Date() throws Exception {
		Date in = cal("2012-12-21").getTime();
		HttpPartSchema ps = T_DATETIME;
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21T00:00:00Z", s);
	}

	@Test void b14_typeString_formatDateTime_Temporal() throws Exception {
		Instant in = cal("2012-12-21").toInstant();
		HttpPartSchema ps = T_DATETIME;
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00Z", s);
		Calendar r = parse(ps, s, Calendar.class);
		s = serialize(ps, r);
		assertEquals("2012-12-21T00:00:00Z", s);
	}

	@Test void b15_typeString_formatDate_Other() throws Exception {
		var in = new StringBuilder("2012-12-21T00:00:00");
		HttpPartSchema ps = T_DATETIME;
		String s = serialize(ps, in);
		assertEquals("2012-12-21T00:00:00", s);
		String r = parse(ps, s, String.class);
		assertEquals(in.toString(), r);
	}

	@Test void b16_typeString_formatDate_null() throws Exception {
		String in = null;
		HttpPartSchema ps = T_DATETIME;
		String s = serialize(ps, in);
		assertEquals("null", s);
		String r = parse(ps, s, String.class);
		assertEquals(null, r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Type == BOOLEAN
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_typeBoolean_formatDefault_String() throws Exception {
		String in = "true";
		HttpPartSchema ps = T_BOOLEAN;
		String s = serialize(ps, in);
		assertEquals("true", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void c02_typeBoolean_formatDefault_Boolean() throws Exception {
		Boolean in = true;
		HttpPartSchema ps = T_BOOLEAN;
		String s = serialize(ps, in);
		assertEquals("true", s);
		Boolean r = parse(ps, s, Boolean.class);
		assertEquals(in, r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Type == INTEGER
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_typeInteger_formatDefault_String() throws Exception {
		String in = "123";
		HttpPartSchema ps = T_INTEGER;
		String s = serialize(ps, in);
		assertEquals("123", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void d02_typeInteger_formatDefault_Integer() throws Exception {
		Integer in = 123;
		HttpPartSchema ps = T_INTEGER;
		String s = serialize(ps, in);
		assertEquals("123", s);
		Integer r = parse(ps, s, Integer.class);
		assertEquals(in, r);
	}

	@Test void d03_typeInteger_formatInt32_String() throws Exception {
		String in = "123";
		HttpPartSchema ps = T_INT32;
		String s = serialize(ps, in);
		assertEquals("123", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void d04_typeInteger_formatInt32_Integer() throws Exception {
		Integer in = 123;
		HttpPartSchema ps = T_INT32;
		String s = serialize(ps, in);
		assertEquals("123", s);
		Integer r = parse(ps, s, Integer.class);
		assertEquals(in, r);
	}

	@Test void d05_typeInteger_formatInt64_String() throws Exception {
		String in = "123";
		HttpPartSchema ps = T_INT64;
		String s = serialize(ps, in);
		assertEquals("123", s);
		String r = parse(ps, s, String.class);
		assertEquals(in, r);
	}

	@Test void d06_typeInteger_formatInt64_Long() throws Exception {
		Long in = 123L;
		HttpPartSchema ps = T_INT64;
		String s = serialize(ps, in);
		assertEquals("123", s);
		Long r = parse(ps, s, Long.class);
		assertEquals(in, r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Type == NUMBER
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_tNumberDefault_String() throws Exception {
		String in = "123";
		HttpPartSchema ps = T_NUMBER;
		String s = serialize(ps, in);
		assertEquals("123.0", s);
		String r = parse(ps, s, String.class);
		assertEquals("123.0", r);
	}

	@Test void e02_tNumberDefault_Float() throws Exception {
		Float in = 123f;
		HttpPartSchema ps = T_NUMBER;
		String s = serialize(ps, in);
		assertEquals("123.0", s);
		Float r = parse(ps, s, Float.class);
		assertEquals(in, r);
	}

	@Test void e03_tNumberFloat_String() throws Exception {
		String in = "123";
		HttpPartSchema ps = T_FLOAT;
		String s = serialize(ps, in);
		assertEquals("123.0", s);
		String r = parse(ps, s, String.class);
		assertEquals("123.0", r);
	}

	@Test void e04_tNumberFloat_Integer() throws Exception {
		Float in = 123f;
		HttpPartSchema ps = T_FLOAT;
		String s = serialize(ps, in);
		assertEquals("123.0", s);
		Float r = parse(ps, s, Float.class);
		assertEquals(in, r);
	}

	@Test void e05_tNumberDouble_String() throws Exception {
		String in = "123";
		HttpPartSchema ps = T_DOUBLE;
		String s = serialize(ps, in);
		assertEquals("123.0", s);
		String r = parse(ps, s, String.class);
		assertEquals("123.0", r);
	}

	@Test void e06_tNumberDouble_Double() throws Exception {
		Double in = 123d;
		HttpPartSchema ps = T_DOUBLE;
		String s = serialize(ps, in);
		assertEquals("123.0", s);
		Double r = parse(ps, s, Double.class);
		assertEquals(in, r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Type == ARRAY
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_tArray_String() {
		String in = "123";
		HttpPartSchema ps = T_ARRAY;
		assertThrowsWithMessage(SerializeException.class, "Input is not a valid array type: java.lang.String", ()->serialize(ps, in));
	}

	@Test void f02a_tArray_StringList() throws Exception {
		List<String> in = list("123");
		HttpPartSchema ps = T_ARRAY;
		String s = serialize(ps, in);
		assertEquals("123", s);
		List<String> r = parse(ps, s, List.class, String.class);
		assertEquals(in, r);

		in = list("123","456");
		s = serialize(ps, in);
		assertEquals("123,456", s);
		r = parse(ps, s, List.class, String.class);
		assertEquals(in, r);
	}

	@Test void f02b_tArray_3dStringList() throws Exception {
		List<List<List<String>>> in = list(list(list("a")));
		HttpPartSchema ps = tArray().items(
			tArray().items(
				tArray()
			)
		).build();
		String s = serialize(ps, in);
		assertEquals("a", s);
		List<String> r = parse(ps, s, List.class, List.class, List.class, String.class);
		assertEquals(in, r);

		in =  list(list(list("a","b"),list("c","d")),list(list("e","f"),list("g","h")));
		s = serialize(ps, in);
		assertEquals("a\\\\\\,b\\,c\\\\\\,d,e\\\\\\,f\\,g\\\\\\,h", s);
		r = parse(ps, s, List.class, List.class, List.class, String.class);
		assertEquals(in, r);
	}

	@Test void f03a_tArray_IntArray() throws Exception {
		int[] in = {123};
		HttpPartSchema ps = T_ARRAY;
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[] r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));

		in = new int[]{123,456};
		s = serialize(ps, in);
		assertEquals("123,456", s);
		r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));
	}

	@Test void f03b_tArray_3dIntArray() throws Exception {
		int[][][] in = {{{123}}};
		HttpPartSchema ps = tArray().items(
			tArray().items(
				tArray()
			)
		).build();
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[][][] r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));

		int[][][] in2 = {{{1,2},{3,4}},{{5,6},{7,8}}};
		in = in2;
		s = serialize(ps, in);
		assertEquals("1\\\\\\,2\\,3\\\\\\,4,5\\\\\\,6\\,7\\\\\\,8", s);
		r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));
	}


	public static class F04 {
		private String[] args;

		public F04(String...args) {
			this.args = args;
		}

		public String[] toStringArray() {
			return args;
		}
	}

	@Test void f04_tArray_StringArrayMutator() throws Exception {
		var in = new F04("a");
		HttpPartSchema ps = T_ARRAY;
		String s = serialize(ps, in);
		assertEquals("a", s);
		F04 r = parse(ps, s, F04.class);
		assertEquals(json(r.toStringArray()), json(in.toStringArray()));

		in = new F04("a","b");
		s = serialize(ps, in);
		assertEquals("a,b", s);
		r = parse(ps, s, F04.class);
		assertEquals(json(r.toStringArray()), json(in.toStringArray()));
	}

	@Test void f05a_tArrayUon_IntArray() throws Exception {
		int[] in = {123};
		HttpPartSchema ps = T_ARRAY_UON;
		String s = serialize(ps, in);
		assertEquals("@(123)", s);
		int[] r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));

		in = new int[]{123,456};
		s = serialize(ps, in);
		assertEquals("@(123,456)", s);
		r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));
	}

	@Test void f05b_tArrayUon_3dIntArray() throws Exception {
		int[][][] in = {{{123}}};
		HttpPartSchema ps = T_ARRAY_UON;
		String s = serialize(ps, in);
		assertEquals("@(@(@(123)))", s);
		int[][][] r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));

		int[][][] in2 = {{{1,2},{3,4}},{{5,6},{7,8}}};
		in = in2;
		s = serialize(ps, in);
		assertEquals("@(@(@(1,2),@(3,4)),@(@(5,6),@(7,8)))", s);
		r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));
	}

	@Test void f06a_tArrayPipes_IntArray() throws Exception {
		int[] in = {123};
		HttpPartSchema ps = T_ARRAY_PIPES;
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[] r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));

		in = new int[]{123,456};
		s = serialize(ps, in);
		assertEquals("123|456", s);
		r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));
	}

	@Test void f06b_tArrayPipes_3dIntArray() throws Exception {
		int[][][] in = {{{123}}};
		HttpPartSchema ps = T_ARRAY_PIPES;
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[][][] r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));

		int[][][] in2 = {{{1,2},{3,4}},{{5,6},{7,8}}};
		in = in2;
		s = serialize(ps, in);
		assertEquals("1\\\\,2,3\\\\,4|5\\\\,6,7\\\\,8", s);
		r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));
	}

	@Test void f07a_tArraySsv_IntArray() throws Exception {
		int[] in = {123};
		HttpPartSchema ps = T_ARRAY_SSV;
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[] r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));

		in = new int[]{123,456};
		s = serialize(ps, in);
		assertEquals("123 456", s);
		r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));
	}

	@Test void f07b_tArraySsv_3dIntArray() throws Exception {
		int[][][] in = {{{123}}};
		HttpPartSchema ps = T_ARRAY_SSV;
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[][][] r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));

		int[][][] in2 = {{{1,2},{3,4}},{{5,6},{7,8}}};
		in = in2;
		s = serialize(ps, in);
		assertEquals("1\\,2,3\\,4 5\\,6,7\\,8", s);
		r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));
	}

	@Test void f08a_tArrayTsv_IntArray() throws Exception {
		int[] in = {123};
		HttpPartSchema ps = T_ARRAY_TSV;
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[] r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));

		in = new int[]{123,456};
		s = serialize(ps, in);
		assertEquals("123\t456", s);
		r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));
	}

	@Test void f08b_tArrayTsv_3dIntArray() throws Exception {
		int[][][] in = {{{123}}};
		HttpPartSchema ps = T_ARRAY_TSV;
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[][][] r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));

		int[][][] in2 = {{{1,2},{3,4}},{{5,6},{7,8}}};
		in = in2;
		s = serialize(ps, in);
		assertEquals("1\\,2,3\\,4\t5\\,6,7\\,8", s);
		r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));
	}

	@Test void f09a_tArrayCsv_IntArray() throws Exception {
		int[] in = {123};
		HttpPartSchema ps = T_ARRAY_CSV;
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[] r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));

		in = new int[]{123,456};
		s = serialize(ps, in);
		assertEquals("123,456", s);
		r = parse(ps, s, int[].class);
		assertEquals(json(r), json(in));
	}

	@Test void f09b_tArrayCsv_3dIntArray() throws Exception {
		int[][][] in = {{{123}}};
		HttpPartSchema ps = T_ARRAY_CSV;
		String s = serialize(ps, in);
		assertEquals("123", s);
		int[][][] r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));

		int[][][] in2 = {{{1,2},{3,4}},{{5,6},{7,8}}};
		in = in2;
		s = serialize(ps, in);
		assertEquals("1\\\\\\,2\\,3\\\\\\,4,5\\\\\\,6\\,7\\\\\\,8", s);
		r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));
	}


	@Test void f10_tArray_complexTypes() throws Exception {
		int[][][] in =  {{{1,2},{3,4}},{{5,6},{7,8}}};
		HttpPartSchema ps = tArrayCsv().items(
			tArrayPipes().items(
				tArraySsv()
			)
		).build();
		String s = serialize(ps, in);
		assertEquals("1 2|3 4,5 6|7 8", s);
		int[][][] r = parse(ps, s, int[][][].class);
		assertEquals(json(r), json(in));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Type == OBJECT, Map
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01a_objectType_formatDefault_Map() throws Exception {
		var in = JsonMap.of("a","b");
		HttpPartSchema ps = T_OBJECT;
		String s = serialize(ps, in);
		assertEquals("a=b", s);
		JsonMap r = parse(ps, s, JsonMap.class);
		assertEquals(json(r), json(in));

		in = JsonMap.of("a","b","c","d");
		s = serialize(ps, in);
		assertEquals("a=b,c=d", s);
		r = parse(ps, s, JsonMap.class);
		assertEquals(json(r), json(in));
	}

	@Test void g01b_objectType_formatDefault_Map_3d() throws Exception {
		var in = JsonMap.of("a",JsonMap.of("b",JsonMap.of("c","d")));
		HttpPartSchema ps = tObject()
			.p("a", tObject()
				.p("b", tObject())
				.p("e", tObject())
			)
			.build();
		String s = serialize(ps, in);
		assertEquals("a=b\\=c\\\\\\=d", s);
		JsonMap r = parse(ps, s, JsonMap.class);
		assertEquals(json(r), json(in));

		in = JsonMap.of("a",JsonMap.of("b",JsonMap.of("c","d"),"e",JsonMap.of("f","g")));
		s = serialize(ps, in);
		assertEquals("a=b\\=c\\\\\\=d\\,e\\=f\\\\\\=g", s);
		r = parse(ps, s, JsonMap.class);
		assertEquals(json(r), json(in));
	}

	//---------------------------------------------------------------------------------------------
	// Helpers
	//---------------------------------------------------------------------------------------------

	private Calendar cal(String in) {
		return DatatypeConverter.parseDateTime(toValidISO8601DT(in));
	}
}