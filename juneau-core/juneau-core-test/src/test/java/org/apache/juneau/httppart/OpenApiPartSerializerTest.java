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
package org.apache.juneau.httppart;

import static org.junit.Assert.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;
import org.junit.*;

public class OpenApiPartSerializerTest {

	static OpenApiPartSerializer s = OpenApiPartSerializer.DEFAULT;

	//-----------------------------------------------------------------------------------------------------------------
	// Input validations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_outputValidations_nullOutput() throws Exception {
		HttpPartSchema ps = schema().build();
		assertNull(s.serialize(ps, null));

		ps = schema().required(false).build();
		assertNull(s.serialize(ps, null));

		ps = schema().required().build();
		try {
			s.serialize(ps, null);
			fail();
		} catch (Exception e) {
			assertEquals("Required value not provided.", e.getMessage());
		}

		ps = schema().required(true).build();
		try {
			s.serialize(ps, null);
			fail();
		} catch (Exception e) {
			assertEquals("Required value not provided.", e.getMessage());
		}
	}

	@Test
	public void a02_outputValidations_emptyOutput() throws Exception {

		HttpPartSchema ps = schema().allowEmptyValue().build();
		assertEquals("", s.serialize(ps, ""));

		ps = schema().allowEmptyValue().build();
		assertEquals("", s.serialize(ps, ""));

		ps = schema().allowEmptyValue(false).build();
		try {
			s.serialize(ps, "");
			fail();
		} catch (Exception e) {
			assertEquals("Empty value not allowed.", e.getMessage());
		}

		try {
			s.serialize(ps, "");
			fail();
		} catch (Exception e) {
			assertEquals("Empty value not allowed.", e.getMessage());
		}

		assertEquals(" ", s.serialize(ps, " "));
	}

	@Test
	public void a03_outputValidations_pattern() throws Exception {
		HttpPartSchema ps = schema().pattern("x.*").allowEmptyValue().build();
		assertEquals("x", s.serialize(ps, "x"));
		assertEquals("xx", s.serialize(ps, "xx"));
		assertEquals(null, s.serialize(ps, null));

		try {
			s.serialize(ps, "y");
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match expected pattern.  Must match pattern: x.*", e.getMessage());
		}

		try {
			s.serialize(ps, "");
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match expected pattern.  Must match pattern: x.*", e.getMessage());
		}

		// Blank/null patterns are ignored.
		ps = schema().pattern("").allowEmptyValue().build();
		assertEquals("x", s.serialize(ps, "x"));
		ps = schema().pattern(null).allowEmptyValue().build();
		assertEquals("x", s.serialize(ps, "x"));
	}

	@Test
	public void a04_outputValidations_enum() throws Exception {
		HttpPartSchema ps = schema()._enum("foo").allowEmptyValue().build();

		assertEquals("foo", s.serialize(ps, "foo"));
		assertEquals(null, s.serialize(ps, null));

		try {
			s.serialize(ps, "bar");
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['foo']", e.getMessage());
		}

		try {
			s.serialize(ps, "");
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['foo']", e.getMessage());
		}

		ps = schema()._enum((Set<String>)null).build();
		assertEquals("foo", s.serialize(ps, "foo"));
		ps = schema()._enum((Set<String>)null).allowEmptyValue().build();
		assertEquals("foo", s.serialize(ps, "foo"));

		ps = schema()._enum("foo","foo").build();
		assertEquals("foo", s.serialize(ps, "foo"));
	}

	@Test
	public void a05_outputValidations_minMaxLength() throws Exception {
		HttpPartSchema ps = schema().minLength(1l).maxLength(2l).allowEmptyValue().build();

		assertEquals(null, s.serialize(ps, null));
		assertEquals("1", s.serialize(ps, "1"));
		assertEquals("12", s.serialize(ps, "12"));

		try {
			s.serialize(ps, "");
			fail();
		} catch (Exception e) {
			assertEquals("Minimum length of value not met.", e.getMessage());
		}

		try {
			s.serialize(ps, "123");
			fail();
		} catch (Exception e) {
			assertEquals("Maximum length of value exceeded.", e.getMessage());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type = string
	//-----------------------------------------------------------------------------------------------------------------

	public static class C1 {
		private byte[] f;
		public C1(byte[] f) {
			this.f = f;
		}
		public byte[] toByteArray() {
			return f;
		}
	}

	public static class C2 {
		private String f;
		public C2(String s) {
			f = "C2-" + s;
		}
		@Override
		public String toString() {
			return f;
		}
	}

	public static class C3 {
		private String[] f;
		public C3(String...in) {
			f = in;
		}
		public String[] toStringArray() {
			return f;
		}
	}


	@Test
	public void c01_stringType_simple() throws Exception {
		HttpPartSchema ps = schema("string").build();
		assertEquals("foo", s.serialize(ps, "foo"));
	}

	@Test
	public void c02_stringType_default() throws Exception {
		HttpPartSchema ps = schema("string")._default("x").build();
		assertEquals("foo", s.serialize(ps, "foo"));
		assertEquals("x", s.serialize(ps, null));
	}

	@Test
	public void c03_stringType_byteFormat() throws Exception {
		HttpPartSchema ps = schema("string", "byte").build();
		byte[] foob = "foo".getBytes();
		String expected = base64Encode(foob);
		assertEquals(expected, s.serialize(ps, foob));
		assertEquals(expected, s.serialize(ps, new C1(foob)));
	}

	@Test
	public void c04_stringType_binaryFormat() throws Exception {
		HttpPartSchema ps = schema("string", "binary").build();
		byte[] foob = "foo".getBytes();
		String expected = toHex(foob);
		assertEquals(expected, s.serialize(ps, foob));
		assertEquals(expected, s.serialize(ps, new C1(foob)));
	}

	@Test
	public void c05_stringType_binarySpacedFormat() throws Exception {
		HttpPartSchema ps = schema("string", "binary-spaced").build();
		byte[] foob = "foo".getBytes();
		String expected = toSpacedHex(foob);
		assertEquals(expected, s.serialize(ps, foob));
		assertEquals(expected, s.serialize(ps, new C1(foob)));
	}

	@Test
	public void c06_stringType_dateFormat() throws Exception {
		HttpPartSchema ps = schema("string", "date").build();
		Calendar in = StringUtils.parseIsoCalendar("2012-12-21");
		assertTrue(s.serialize(ps, in).contains("2012"));
	}

	@Test
	public void c07_stringType_dateTimeFormat() throws Exception {
		HttpPartSchema ps = schema("string", "date-time").build();
		Calendar in = StringUtils.parseIsoCalendar("2012-12-21T12:34:56.789");
		assertTrue(s.serialize(ps, in).contains("2012"));
	}

	@Test
	public void c08_stringType_uonFormat() throws Exception {
		HttpPartSchema ps = schema("string", "uon").build();
		assertEquals("foo", s.serialize(ps, "foo"));
		assertEquals("'foo'", s.serialize(ps, "'foo'"));
		assertEquals("C2-foo", s.serialize(ps, new C2("foo")));
		// UonPartSerializerTest should handle all other cases.
	}

	@Test
	public void c09_stringType_noneFormat() throws Exception {
		// If no format is specified, then we should transform directly from a string.
		HttpPartSchema ps = schema("string").build();
		assertEquals("foo", s.serialize(ps, "foo"));
		assertEquals("'foo'", s.serialize(ps, "'foo'"));
		assertEquals("C2-foo", s.serialize(ps, new C2("foo")));
	}

	@Test
	public void c10_stringType_noneFormat_2d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("string")).build();
		assertEquals("foo,bar", s.serialize(ps, new String[]{"foo","bar"}));
		assertEquals("foo,bar", s.serialize(ps, AList.create("foo","bar")));
		assertEquals("foo,bar", s.serialize(ps, new Object[]{"foo","bar"}));
		assertEquals("foo,bar", s.serialize(ps, AList.create((Object)"foo",(Object)"bar")));
		assertEquals("C2-foo,C2-bar", s.serialize(ps, new C2[] {new C2("foo"), new C2("bar")}));
		assertEquals("C2-foo,C2-bar", s.serialize(ps, AList.create(new C2("foo"), new C2("bar"))));
		assertEquals("foo,bar", s.serialize(ps, new C3("foo","bar")));
	}

	@Test
	public void c11_stringType_noneFormat_3d() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("string"))).build();
		assertEquals("foo,bar|baz", s.serialize(ps, new String[][]{{"foo","bar"},{"baz"}}));
		assertEquals("foo,bar|baz", s.serialize(ps, AList.create(new String[]{"foo","bar"}, new String[]{"baz"})));
		assertEquals("foo,bar|baz", s.serialize(ps, AList.create(AList.create("foo","bar"),AList.create("baz"))));
		assertEquals("foo,bar|baz", s.serialize(ps, new Object[][]{{"foo","bar"},{"baz"}}));
		assertEquals("foo,bar|baz", s.serialize(ps, AList.create(new Object[]{"foo","bar"}, new String[]{"baz"})));
		assertEquals("foo,bar|baz", s.serialize(ps, AList.create(AList.create((Object)"foo",(Object)"bar"),AList.create((Object)"baz"))));
		assertEquals("C2-foo,C2-bar|C2-baz", s.serialize(ps, new C2[][]{{new C2("foo"),new C2("bar")},{new C2("baz")}}));
		assertEquals("C2-foo,C2-bar|C2-baz", s.serialize(ps, AList.create(new C2[]{new C2("foo"),new C2("bar")}, new C2[]{new C2("baz")})));
		assertEquals("C2-foo,C2-bar|C2-baz", s.serialize(ps, AList.create(AList.create(new C2("foo"),new C2("bar")),AList.create(new C2("baz")))));
		assertEquals("foo,bar|baz", s.serialize(ps, new C3[]{new C3("foo","bar"),new C3("baz")}));
		assertEquals("foo,bar|baz", s.serialize(ps, AList.create(new C3("foo","bar"), new C3("baz"))));
	}

	@Test
	public void c12a_stringType_uonKeywords_plain() throws Exception {
		HttpPartSchema ps = schema("string").build();
		// When serialized normally, the following should not be quoted.
		assertEquals("true", s.serialize(ps, "true"));
		assertEquals("false", s.serialize(ps, "false"));
		assertEquals("null", s.serialize(ps, "null"));
		assertEquals("123", s.serialize(ps, "123"));
		assertEquals("1.23", s.serialize(ps, "1.23"));
	}

	@Test
	public void c12b_stringType_uonKeywords_uon() throws Exception {
		HttpPartSchema ps = schema("string","uon").build();
		// When serialized as UON, the following should be quoted so that they're not confused with booleans or numbers.
		assertEquals("'true'", s.serialize(ps, "true"));
		assertEquals("'false'", s.serialize(ps, "false"));
		assertEquals("'null'", s.serialize(ps, "null"));
		assertEquals("'123'", s.serialize(ps, "123"));
		assertEquals("'1.23'", s.serialize(ps, "1.23"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type = array
	//-----------------------------------------------------------------------------------------------------------------

	public static class D {
		private String f;
		public D(String in) {
			this.f = "D-" + in;
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test
	public void d01_arrayType_collectionFormatCsv() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("csv").build();
		assertEquals("foo,bar", s.serialize(ps, new String[]{"foo","bar"}));
		assertEquals("foo,bar", s.serialize(ps, new Object[]{"foo","bar"}));
		assertEquals("D-foo,D-bar", s.serialize(ps, new D[]{new D("foo"),new D("bar")}));
		assertEquals("foo,bar", s.serialize(ps, AList.create("foo","bar")));
		assertEquals("foo,bar", s.serialize(ps, AList.<Object>create("foo","bar")));
		assertEquals("D-foo,D-bar", s.serialize(ps, AList.create(new D("foo"),new D("bar"))));
		assertEquals("foo,bar", s.serialize(ps, new ObjectList().append("foo","bar")));
	}

	@Test
	public void d02_arrayType_collectionFormatPipes() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").build();
		assertEquals("foo|bar", s.serialize(ps, new String[]{"foo","bar"}));
		assertEquals("foo|bar", s.serialize(ps, new Object[]{"foo","bar"}));
		assertEquals("D-foo|D-bar", s.serialize(ps, new D[]{new D("foo"),new D("bar")}));
		assertEquals("foo|bar", s.serialize(ps, AList.create("foo","bar")));
		assertEquals("foo|bar", s.serialize(ps, AList.<Object>create("foo","bar")));
		assertEquals("D-foo|D-bar", s.serialize(ps, AList.create(new D("foo"),new D("bar"))));
		assertEquals("foo|bar", s.serialize(ps, new ObjectList().append("foo","bar")));
	}

	@Test
	public void d03_arrayType_collectionFormatSsv() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("ssv").build();
		assertEquals("foo bar", s.serialize(ps, new String[]{"foo","bar"}));
		assertEquals("foo bar", s.serialize(ps, new Object[]{"foo","bar"}));
		assertEquals("D-foo D-bar", s.serialize(ps, new D[]{new D("foo"),new D("bar")}));
		assertEquals("foo bar", s.serialize(ps, AList.create("foo","bar")));
		assertEquals("foo bar", s.serialize(ps, AList.<Object>create("foo","bar")));
		assertEquals("D-foo D-bar", s.serialize(ps, AList.create(new D("foo"),new D("bar"))));
		assertEquals("foo bar", s.serialize(ps, new ObjectList().append("foo","bar")));
	}

	@Test
	public void d04_arrayType_collectionFormatTsv() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("tsv").build();
		assertEquals("foo\tbar", s.serialize(ps, new String[]{"foo","bar"}));
		assertEquals("foo\tbar", s.serialize(ps, new Object[]{"foo","bar"}));
		assertEquals("D-foo\tD-bar", s.serialize(ps, new D[]{new D("foo"),new D("bar")}));
		assertEquals("foo\tbar", s.serialize(ps, AList.create("foo","bar")));
		assertEquals("foo\tbar", s.serialize(ps, AList.<Object>create("foo","bar")));
		assertEquals("D-foo\tD-bar", s.serialize(ps, AList.create(new D("foo"),new D("bar"))));
		assertEquals("foo\tbar", s.serialize(ps, new ObjectList().append("foo","bar")));
	}

	@Test
	public void d05_arrayType_collectionFormatUon() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("uon").build();
		assertEquals("@(foo,bar)", s.serialize(ps, new String[]{"foo","bar"}));
		assertEquals("@(foo,bar)", s.serialize(ps, new Object[]{"foo","bar"}));
		assertEquals("@(D-foo,D-bar)", s.serialize(ps, new D[]{new D("foo"),new D("bar")}));
		assertEquals("@(foo,bar)", s.serialize(ps, AList.create("foo","bar")));
		assertEquals("@(foo,bar)", s.serialize(ps, AList.<Object>create("foo","bar")));
		assertEquals("@(D-foo,D-bar)", s.serialize(ps, AList.create(new D("foo"),new D("bar"))));
		assertEquals("@(foo,bar)", s.serialize(ps, new ObjectList().append("foo","bar")));
	}

	@Test
	public void d06a_arrayType_collectionFormatNone() throws Exception {
		HttpPartSchema ps = schema("array").build();
		assertEquals("foo,bar", s.serialize(ps, new String[]{"foo","bar"}));
		assertEquals("foo,bar", s.serialize(ps, new Object[]{"foo","bar"}));
		assertEquals("D-foo,D-bar", s.serialize(ps, new D[]{new D("foo"),new D("bar")}));
		assertEquals("foo,bar", s.serialize(ps, AList.create("foo","bar")));
		assertEquals("foo,bar", s.serialize(ps, AList.<Object>create("foo","bar")));
		assertEquals("D-foo,D-bar", s.serialize(ps, AList.create(new D("foo"),new D("bar"))));
		assertEquals("foo,bar", s.serialize(ps, new ObjectList().append("foo","bar")));
	}

	@Test
	public void d07_arrayType_collectionFormatMulti() throws Exception {
		// collectionFormat=multi really shouldn't be applicable to collections of values, so just use csv.
		HttpPartSchema ps = schema("array").collectionFormat("multi").build();
		assertEquals("foo,bar", s.serialize(ps, new String[]{"foo","bar"}));
		assertEquals("foo,bar", s.serialize(ps, new Object[]{"foo","bar"}));
		assertEquals("D-foo,D-bar", s.serialize(ps, new D[]{new D("foo"),new D("bar")}));
		assertEquals("foo,bar", s.serialize(ps, AList.create("foo","bar")));
		assertEquals("foo,bar", s.serialize(ps, AList.<Object>create("foo","bar")));
		assertEquals("D-foo,D-bar", s.serialize(ps, AList.create(new D("foo"),new D("bar"))));
		assertEquals("foo,bar", s.serialize(ps, new ObjectList().append("foo","bar")));
	}

	@Test
	public void d08_arrayType_collectionFormatCsvAndPipes() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").collectionFormat("csv")).build();
		assertEquals("foo,bar|baz,qux", s.serialize(ps, new String[][]{{"foo","bar"},{"baz","qux"}}));
		assertEquals("foo,bar|baz,qux", s.serialize(ps, new Object[][]{{"foo","bar"},{"baz","qux"}}));
		assertEquals("D-foo,D-bar|D-baz,D-qux", s.serialize(ps, new D[][]{{new D("foo"),new D("bar")},{new D("baz"),new D("qux")}}));
		assertEquals("foo,bar|baz,qux", s.serialize(ps, AList.create(AList.create("foo","bar"),AList.create("baz","qux"))));
		assertEquals("foo,bar|baz,qux", s.serialize(ps, AList.create(AList.<Object>create("foo","bar"),AList.<Object>create("baz","qux"))));
		assertEquals("D-foo,D-bar|D-baz,D-qux", s.serialize(ps, AList.create(AList.create(new D("foo"),new D("bar")),AList.create(new D("baz"),new D("qux")))));
	}

	@Test
	public void d09_arrayType_itemsInteger() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("csv").items(schema("integer")).build();
		assertEquals("1,2", s.serialize(ps, new int[]{1,2}));
		assertEquals("1,2", s.serialize(ps, new Integer[]{1,2}));
		assertEquals("1,2", s.serialize(ps, new Object[]{1,2}));
		assertEquals("1,2", s.serialize(ps, AList.create(1,2)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type = boolean
	//-----------------------------------------------------------------------------------------------------------------

	public static class E1 {
		private Boolean f;
		public E1(Boolean in) {
			this.f = in;
		}
		public Boolean toBoolean() {
			return f;
		}
	}

	public static class E2 {
		private Boolean[] f;
		public E2(Boolean...in) {
			this.f = in;
		}
		public Boolean[] toBooleanArray() {
			return f;
		}
	}

	@Test
	public void e01_booleanType() throws Exception {
		HttpPartSchema ps = schema("boolean").build();
		assertEquals("true", s.serialize(ps, true));
		assertEquals("false", s.serialize(ps, false));
		assertEquals("null", s.serialize(ps, null));
	}

	@Test
	public void e02_booleanType_2d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("boolean")).build();
		assertEquals("true,true", s.serialize(ps, new boolean[]{true,true}));
		assertEquals("true,true", s.serialize(ps, new Boolean[]{true,true}));
		assertEquals("true,true", s.serialize(ps, AList.create(true,true)));
		assertEquals("true,true", s.serialize(ps, new String[]{"true","true"}));
		assertEquals("true,true", s.serialize(ps, AList.create("true","true")));
		assertEquals("true,true", s.serialize(ps, new Object[]{true,true}));
		assertEquals("true,true", s.serialize(ps, AList.<Object>create(true,true)));
		assertEquals("true,true", s.serialize(ps, new E1[]{new E1(true),new E1(true)}));
		assertEquals("true,true", s.serialize(ps, AList.create(new E1(true),new E1(true))));
		assertEquals("true,true", s.serialize(ps, new E2(true,true)));
	}

	//	@Test
//	public void e03_booleanType_3d() throws Exception {
//		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("boolean"))).build();
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "true,true|false", boolean[][].class));
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "true,true|false", List.class, boolean[].class));
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "true,true|false", Boolean[][].class));
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "true,true|false", List.class, Boolean[].class));
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "true,true|false", List.class, List.class, Boolean.class));
//		assertEquals("[['true','true'],['false']]", s.serialize(ps, "true,true|false", String[][].class));
//		assertEquals("[['true','true'],['false']]", s.serialize(ps, "true,true|false", List.class, List.class, String.class));
//		assertEquals("[['true','true'],['false']]", s.serialize(ps, "true,true|false", List.class, String[].class));
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "true,true|false", Object[][].class));
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "true,true|false", List.class, List.class, Object.class));
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "true,true|false", List.class, Object[].class));
//		assertEquals("[['E1-true','E1-true'],['E1-false']]", s.serialize(ps, "true,true|false", E1[][].class));
//		assertEquals("[['E1-true','E1-true'],['E1-false']]", s.serialize(ps, "true,true|false", List.class, List.class, E1.class));
//		assertEquals("[['E1-true','E1-true'],['E1-false']]", s.serialize(ps, "true,true|false", List.class, E1[].class));
//		assertEquals("['E2-[true,true]','E2-[false]']", s.serialize(ps, "true,true|false", E2[].class));
//		assertEquals("['E2-[true,true]','E2-[false]']", s.serialize(ps, "true,true|false", List.class, E2.class));
//
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "True,true|false", boolean[][].class));
//		assertEquals("[[true,true],[false]]", s.serialize(ps, "TRUE,true|false", boolean[][].class));
//	}
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// type = integer
//	//-----------------------------------------------------------------------------------------------------------------
//
//	public static class F1 {
//		private String f;
//		public F1(Integer in) {
//			this.f = "F1-" + in.toString();
//		}
//		@Override
//		public String toString() {
//			return f;
//		}
//	}
//
//	public static class F2 {
//		private String f;
//		public F2(Integer[] in) {
//			this.f = "F2-" + JsonSerializer.DEFAULT_LAX.toString(in);
//		}
//		@Override
//		public String toString() {
//			return f;
//		}
//	}
//
//	public static class F3 {
//		private String f;
//		public F3(Long in) {
//			this.f = "F3-" + in.toString();
//		}
//		@Override
//		public String toString() {
//			return f;
//		}
//	}
//
//	public static class F4 {
//		private String f;
//		public F4(Long[] in) {
//			this.f = "F4-" + JsonSerializer.DEFAULT_LAX.toString(in);
//		}
//		@Override
//		public String toString() {
//			return f;
//		}
//	}
//
//	@Test
//	public void f01_integerType_int32() throws Exception {
//		HttpPartSchema ps = schema("integer", "int32").build();
//		assertEquals("1", s.serialize(ps, "1", int.class));
//		assertEquals("1", s.serialize(ps, "1", Integer.class));
//		assertEquals("1", s.serialize(ps, "1", short.class));
//		assertEquals("1", s.serialize(ps, "1", Short.class));
//		assertEquals("1", s.serialize(ps, "1", long.class));
//		assertEquals("1", s.serialize(ps, "1", Long.class));
//		assertEquals("'1'", s.serialize(ps, "1", String.class));
//		Object o = s.serialize(ps, "1", Object.class);
//		assertEquals("1", o);
//		assertClass(Integer.class, o);
//		assertEquals("'F1-1'", s.serialize(ps,  "1", F1.class));
//	}
//
//	@Test
//	public void f02_integerType_int32_2d() throws Exception {
//		HttpPartSchema ps = schema("array").items(schema("integer", "int32")).build();
//		assertEquals("[1,2]", s.serialize(ps, "1,2", int[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", Integer[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", List.class, Integer.class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", short[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", Short[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", List.class, Short.class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", long[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", Long[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", List.class, Long.class));
//		assertEquals("['1','2']", s.serialize(ps, "1,2", String[].class));
//		assertEquals("['1','2']", s.serialize(ps, "1,2", List.class, String.class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", Object[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", List.class, Object.class));
//		assertEquals("['F1-1','F1-2']", s.serialize(ps,  "1,2", F1[].class));
//		assertEquals("['F1-1','F1-2']", s.serialize(ps,  "1,2", List.class, F1.class));
//		assertEquals("'F2-[1,2]'", s.serialize(ps,  "1,2", F2.class));
//	}
//
//	@Test
//	public void f03_integerType_int32_3d() throws Exception {
//		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("integer", "int32"))).build();
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", int[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, int[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", Integer[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, Integer[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, List.class, Integer.class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", short[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, short[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", Short[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, Short[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, List.class, Short.class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", long[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, long[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", Long[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, Long[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, List.class, Long.class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", String[][].class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", List.class, String[].class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", List.class, List.class, String.class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", Object[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, Object[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, List.class, Object.class));
//		assertEquals("[['F1-1','F1-2'],['F1-3']]", s.serialize(ps,  "1,2|3", F1[][].class));
//		assertEquals("[['F1-1','F1-2'],['F1-3']]", s.serialize(ps,  "1,2|3", List.class, F1[].class));
//		assertEquals("[['F1-1','F1-2'],['F1-3']]", s.serialize(ps,  "1,2|3", List.class, List.class, F1.class));
//		assertEquals("['F2-[1,2]','F2-[3]']", s.serialize(ps, "1,2|3", F2[].class));
//		assertEquals("['F2-[1,2]','F2-[3]']", s.serialize(ps, "1,2|3", List.class, F2.class));
//	}
//
//	@Test
//	public void f04_integerType_int64() throws Exception {
//		HttpPartSchema ps = schema("integer", "int64").build();
//		assertEquals("1", s.serialize(ps, "1", int.class));
//		assertEquals("1", s.serialize(ps, "1", Integer.class));
//		assertEquals("1", s.serialize(ps, "1", short.class));
//		assertEquals("1", s.serialize(ps, "1", Short.class));
//		assertEquals("1", s.serialize(ps, "1", long.class));
//		assertEquals("1", s.serialize(ps, "1", Long.class));
//		assertEquals("'1'", s.serialize(ps, "1", String.class));
//		Object o = s.serialize(ps, "1", Object.class);
//		assertEquals("1", o);
//		assertClass(Long.class, o);
//		assertEquals("'F3-1'", s.serialize(ps,  "1", F3.class));
//	}
//
//	@Test
//	public void f05_integerType_int64_2d() throws Exception {
//		HttpPartSchema ps = schema("array").items(schema("integer", "int64")).build();
//		assertEquals("[1,2]", s.serialize(ps, "1,2", int[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", Integer[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", List.class, Integer.class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", short[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", Short[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", List.class, Short.class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", long[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", Long[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", List.class, Long.class));
//		assertEquals("['1','2']", s.serialize(ps, "1,2", String[].class));
//		assertEquals("['1','2']", s.serialize(ps, "1,2", List.class, String.class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", Object[].class));
//		assertEquals("[1,2]", s.serialize(ps, "1,2", List.class, Object.class));
//		assertEquals("['F3-1','F3-2']", s.serialize(ps,  "1,2", F3[].class));
//		assertEquals("['F3-1','F3-2']", s.serialize(ps,  "1,2", List.class, F3.class));
//		assertEquals("'F4-[1,2]'", s.serialize(ps,  "1,2", F4.class));
//	}
//
//	@Test
//	public void f06_integerType_int64_3d() throws Exception {
//		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("integer", "int64"))).build();
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", int[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, int[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", Integer[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, Integer[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, List.class, Integer.class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", short[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, short[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", Short[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, Short[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, List.class, Short.class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", long[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, long[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", Long[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, Long[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, List.class, Long.class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", String[][].class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", List.class, String[].class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", List.class, List.class, String.class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", Object[][].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, Object[].class));
//		assertEquals("[[1,2],[3]]", s.serialize(ps, "1,2|3", List.class, List.class, Object.class));
//		assertEquals("[['F3-1','F3-2'],['F3-3']]", s.serialize(ps,  "1,2|3", F3[][].class));
//		assertEquals("[['F3-1','F3-2'],['F3-3']]", s.serialize(ps,  "1,2|3", List.class, F3[].class));
//		assertEquals("[['F3-1','F3-2'],['F3-3']]", s.serialize(ps,  "1,2|3", List.class, List.class, F3.class));
//		assertEquals("['F4-[1,2]','F4-[3]']", s.serialize(ps, "1,2|3", F4[].class));
//		assertEquals("['F4-[1,2]','F4-[3]']", s.serialize(ps, "1,2|3", List.class, F4.class));
//	}
//
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// type = number
//	//-----------------------------------------------------------------------------------------------------------------
//
//	public static class G1 {
//		private String f;
//		public G1(Float in) {
//			this.f = "G1-" + in.toString();
//		}
//		@Override
//		public String toString() {
//			return f;
//		}
//	}
//
//	public static class G2 {
//		private String f;
//		public G2(Float[] in) {
//			this.f = "G2-" + JsonSerializer.DEFAULT_LAX.toString(in);
//		}
//		@Override
//		public String toString() {
//			return f;
//		}
//	}
//
//	public static class G3 {
//		private String f;
//		public G3(Double in) {
//			this.f = "G3-" + in.toString();
//		}
//		@Override
//		public String toString() {
//			return f;
//		}
//	}
//
//	public static class G4 {
//		private String f;
//		public G4(Double[] in) {
//			this.f = "G4-" + JsonSerializer.DEFAULT_LAX.toString(in);
//		}
//		@Override
//		public String toString() {
//			return f;
//		}
//	}
//
//	@Test
//	public void g01_numberType_float() throws Exception {
//		HttpPartSchema ps = schema("number", "float").build();
//		assertEquals("1.0", s.serialize(ps, "1", float.class));
//		assertEquals("1.0", s.serialize(ps, "1", Float.class));
//		assertEquals("1.0", s.serialize(ps, "1", double.class));
//		assertEquals("1.0", s.serialize(ps, "1", Double.class));
//		assertEquals("'1'", s.serialize(ps, "1", String.class));
//		Object o =  s.serialize(ps, "1", Object.class);
//		assertEquals("1.0",o);
//		assertClass(Float.class, o);
//		assertEquals("'G1-1.0'", s.serialize(ps,  "1", G1.class));
//	}
//
//	@Test
//	public void g02_numberType_float_2d() throws Exception {
//		HttpPartSchema ps = schema("array").items(schema("number", "float")).build();
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", float[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", Float[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", List.class, Float.class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", double[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", Double[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", List.class, Double.class));
//		assertEquals("['1','2']", s.serialize(ps, "1,2", String[].class));
//		assertEquals("['1','2']", s.serialize(ps, "1,2", List.class, String.class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", Object[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", List.class, Object.class));
//		assertEquals("['G1-1.0','G1-2.0']", s.serialize(ps,  "1,2", G1[].class));
//		assertEquals("['G1-1.0','G1-2.0']", s.serialize(ps,  "1,2", List.class, G1.class));
//		assertEquals("'G2-[1.0,2.0]'", s.serialize(ps,  "1,2", G2.class));
//	}
//
//	@Test
//	public void g03_numberType_float_3d() throws Exception {
//		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("number", "float"))).build();
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", float[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, float[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", Float[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, Float[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, List.class, Float.class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", double[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, double[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", Double[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, Double[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, List.class, Double.class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", String[][].class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", List.class, String[].class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", List.class, List.class, String.class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", Object[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, Object[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, List.class, Object.class));
//		assertEquals("[['G1-1.0','G1-2.0'],['G1-3.0']]", s.serialize(ps,  "1,2|3", G1[][].class));
//		assertEquals("[['G1-1.0','G1-2.0'],['G1-3.0']]", s.serialize(ps,  "1,2|3", List.class, G1[].class));
//		assertEquals("[['G1-1.0','G1-2.0'],['G1-3.0']]", s.serialize(ps,  "1,2|3", List.class, List.class, G1.class));
//		assertEquals("['G2-[1.0,2.0]','G2-[3.0]']", s.serialize(ps, "1,2|3", G2[].class));
//		assertEquals("['G2-[1.0,2.0]','G2-[3.0]']", s.serialize(ps, "1,2|3", List.class, G2.class));
//	}
//
//	@Test
//	public void g04_numberType_double() throws Exception {
//		HttpPartSchema ps = schema("number", "double").build();
//		assertEquals("1.0", s.serialize(ps, "1", float.class));
//		assertEquals("1.0", s.serialize(ps, "1", Float.class));
//		assertEquals("1.0", s.serialize(ps, "1", double.class));
//		assertEquals("1.0", s.serialize(ps, "1", Double.class));
//		assertEquals("'1'", s.serialize(ps, "1", String.class));
//		Object o = s.serialize(ps, "1", Object.class);
//		assertEquals("1.0", o);
//		assertClass(Double.class, o);
//		assertEquals("'G3-1.0'", s.serialize(ps,  "1", G3.class));
//	}
//
//	@Test
//	public void g05_numberType_double_2d() throws Exception {
//		HttpPartSchema ps = schema("array").items(schema("number", "double")).build();
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", float[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", Float[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", List.class, Float.class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", double[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", Double[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", List.class, Double.class));
//		assertEquals("['1','2']", s.serialize(ps, "1,2", String[].class));
//		assertEquals("['1','2']", s.serialize(ps, "1,2", List.class, String.class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", Object[].class));
//		assertEquals("[1.0,2.0]", s.serialize(ps, "1,2", List.class, Object.class));
//		assertEquals("['G3-1.0','G3-2.0']", s.serialize(ps,  "1,2", G3[].class));
//		assertEquals("['G3-1.0','G3-2.0']", s.serialize(ps,  "1,2", List.class, G3.class));
//		assertEquals("'G4-[1.0,2.0]'", s.serialize(ps,  "1,2", G4.class));
//	}
//
//	@Test
//	public void g06_numberType_double_3d() throws Exception {
//		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("number", "double"))).build();
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", float[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, float[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", Float[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, Float[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, List.class, Float.class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", double[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, double[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", Double[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, Double[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, List.class, Double.class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", String[][].class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", List.class, String[].class));
//		assertEquals("[['1','2'],['3']]", s.serialize(ps, "1,2|3", List.class, List.class, String.class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", Object[][].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, Object[].class));
//		assertEquals("[[1.0,2.0],[3.0]]", s.serialize(ps, "1,2|3", List.class, List.class, Object.class));
//		assertEquals("[['G3-1.0','G3-2.0'],['G3-3.0']]", s.serialize(ps,  "1,2|3", G3[][].class));
//		assertEquals("[['G3-1.0','G3-2.0'],['G3-3.0']]", s.serialize(ps,  "1,2|3", List.class, G3[].class));
//		assertEquals("[['G3-1.0','G3-2.0'],['G3-3.0']]", s.serialize(ps,  "1,2|3", List.class, List.class, G3.class));
//		assertEquals("['G4-[1.0,2.0]','G4-[3.0]']", s.serialize(ps, "1,2|3", G4[].class));
//		assertEquals("['G4-[1.0,2.0]','G4-[3.0]']", s.serialize(ps, "1,2|3", List.class, G4.class));
//	}
//
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// type = object
//	//-----------------------------------------------------------------------------------------------------------------
//
//	public static class H1 {
//		public int f;
//	}
//
//	@Test
//	public void h01_objectType() throws Exception {
//		HttpPartSchema ps = HttpPartSchema.create().type("object").build();
//		assertEquals("{f:1}", s.serialize(ps, "(f=1)", H1.class));
//		assertEquals("{f:1}", s.serialize(ps, "(f=1)", ObjectMap.class));
//		Object o = s.serialize(ps, "(f=1)", Object.class);
//		assertEquals("{f:1}", o);
//		assertClass(ObjectMap.class, o);
//	}
//
//	@Test
//	public void h02_objectType_2d() throws Exception {
//		HttpPartSchema ps = schema("array").format("uon").items(schema("object")).build();
//		assertEquals("[{f:1},{f:2}]", s.serialize(ps, "@((f=1),(f=2))", H1[].class));
//		assertEquals("[{f:1},{f:2}]", s.serialize(ps, "@((f=1),(f=2))", List.class, H1.class));
//		assertEquals("[{f:1},{f:2}]", s.serialize(ps, "@((f=1),(f=2))", ObjectMap[].class));
//		assertEquals("[{f:1},{f:2}]", s.serialize(ps, "@((f=1),(f=2))", List.class, ObjectMap.class));
//		assertEquals("[{f:1},{f:2}]", s.serialize(ps, "@((f=1),(f=2))", Object[].class));
//		assertEquals("[{f:1},{f:2}]", s.serialize(ps, "@((f=1),(f=2))", List.class, Object.class));
//		Object o = s.serialize(ps, "@((f=1),(f=2))", Object.class);
//		assertEquals("[{f:1},{f:2}]", o);
//		assertClass(ObjectList.class, o);
//	}
//
//	@Test
//	public void h03_objectType_3d() throws Exception {
//		HttpPartSchema ps = schema("array").format("uon").items(schema("array").items(schema("object"))).build();
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", H1[][].class));
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", List.class, H1[].class));
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, H1.class));
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", ObjectMap[][].class));
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", List.class, ObjectMap[].class));
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, ObjectMap.class));
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", Object[][].class));
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", List.class, Object[].class));
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, Object.class));
//		Object o =  s.serialize(ps, "@(@((f=1),(f=2)),@((f=3)))", Object.class);
//		assertEquals("[[{f:1},{f:2}],[{f:3}]]", o);
//		assertClass(ObjectList.class, o);
//	}
//
//	public static class H2 {
//		public Object f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f99;
//	}
//
//	@Test
//	public void h04_objectType_simpleProperties() throws Exception {
//		HttpPartSchema ps = schema("object")
//			.property("f1", schema("string"))
//			.property("f2", schema("string", "byte"))
//			.property("f4", schema("string", "date-time"))
//			.property("f5", schema("string", "binary"))
//			.property("f6", schema("string", "binary-spaced"))
//			.property("f7", schema("string", "uon"))
//			.property("f8", schema("integer"))
//			.property("f9", schema("integer", "int64"))
//			.property("f10", schema("number"))
//			.property("f11", schema("number", "double"))
//			.property("f12", schema("boolean"))
//			.additionalProperties(schema("integer"))
//			.build();
//
//		byte[] foob = "foo".getBytes();
//		String in = "(f1=foo,f2="+base64Encode(foob)+",f4=2012-12-21T12:34:56Z,f5="+toHex(foob)+",f6='"+toSpacedHex(foob)+"',f7=foo,f8=1,f9=1,f10=1,f11=1,f12=true,f99=1)";
//
//		H2 h2 = s.serialize(ps, in, H2.class);
//		assertEquals("{f1:'foo',f2:[102,111,111],f4:'2012-12-21T12:34:56Z',f5:[102,111,111],f6:[102,111,111],f7:'foo',f8:1,f9:1,f10:1.0,f11:1.0,f12:true,f99:1}", h2);
//		assertClass(String.class, h2.f1);
//		assertClass(byte[].class, h2.f2);
//		assertClass(GregorianCalendar.class, h2.f4);
//		assertClass(byte[].class, h2.f5);
//		assertClass(byte[].class, h2.f6);
//		assertClass(String.class, h2.f7);
//		assertClass(Integer.class, h2.f8);
//		assertClass(Long.class, h2.f9);
//		assertClass(Float.class, h2.f10);
//		assertClass(Double.class, h2.f11);
//		assertClass(Boolean.class, h2.f12);
//		assertClass(Integer.class, h2.f99);
//
//		ObjectMap om = s.serialize(ps, in, ObjectMap.class);
//		assertEquals("{f1:'foo',f2:[102,111,111],f4:'2012-12-21T12:34:56Z',f5:[102,111,111],f6:[102,111,111],f7:'foo',f8:1,f9:1,f10:1.0,f11:1.0,f12:true,f99:1}", om);
//		assertClass(String.class, om.get("f1"));
//		assertClass(byte[].class, om.get("f2"));
//		assertClass(GregorianCalendar.class, om.get("f4"));
//		assertClass(byte[].class, om.get("f5"));
//		assertClass(byte[].class, om.get("f6"));
//		assertClass(String.class, om.get("f7"));
//		assertClass(Integer.class, om.get("f8"));
//		assertClass(Long.class, om.get("f9"));
//		assertClass(Float.class, om.get("f10"));
//		assertClass(Double.class, om.get("f11"));
//		assertClass(Boolean.class, om.get("f12"));
//		assertClass(Integer.class, om.get("f99"));
//
//		om = (ObjectMap)s.serialize(ps, in, Object.class);
//		assertEquals("{f1:'foo',f2:[102,111,111],f4:'2012-12-21T12:34:56Z',f5:[102,111,111],f6:[102,111,111],f7:'foo',f8:1,f9:1,f10:1.0,f11:1.0,f12:true,f99:1}", om);
//		assertClass(String.class, om.get("f1"));
//		assertClass(byte[].class, om.get("f2"));
//		assertClass(GregorianCalendar.class, om.get("f4"));
//		assertClass(byte[].class, om.get("f5"));
//		assertClass(byte[].class, om.get("f6"));
//		assertClass(String.class, om.get("f7"));
//		assertClass(Integer.class, om.get("f8"));
//		assertClass(Long.class, om.get("f9"));
//		assertClass(Float.class, om.get("f10"));
//		assertClass(Double.class, om.get("f11"));
//		assertClass(Boolean.class, om.get("f12"));
//		assertClass(Integer.class, om.get("f99"));
//	}
//
//	@Test
//	public void h05_objectType_arrayProperties() throws Exception {
//		HttpPartSchema ps = schema("object")
//			.property("f1", schema("array").items(schema("string")))
//			.property("f2", schema("array").items(schema("string", "byte")))
//			.property("f4", schema("array").items(schema("string", "date-time")))
//			.property("f5", schema("array").items(schema("string", "binary")))
//			.property("f6", schema("array").items(schema("string", "binary-spaced")))
//			.property("f7", schema("array").items(schema("string", "uon")))
//			.property("f8", schema("array").items(schema("integer")))
//			.property("f9", schema("array").items(schema("integer", "int64")))
//			.property("f10", schema("array").items(schema("number")))
//			.property("f11", schema("array").items(schema("number", "double")))
//			.property("f12", schema("array").items(schema("boolean")))
//			.additionalProperties(schema("array").items(schema("integer")))
//			.build();
//
//		byte[] foob = "foo".getBytes();
//		String in = "(f1=foo,f2="+base64Encode(foob)+",f4=2012-12-21T12:34:56Z,f5="+toHex(foob)+",f6='"+toSpacedHex(foob)+"',f7=foo,f8=1,f9=1,f10=1,f11=1,f12=true,f99=1)";
//
//		H2 h2 = s.serialize(ps, in, H2.class);
//		assertEquals("{f1:['foo'],f2:[[102,111,111]],f4:['2012-12-21T12:34:56Z'],f5:[[102,111,111]],f6:[[102,111,111]],f7:['foo'],f8:[1],f9:[1],f10:[1.0],f11:[1.0],f12:[true],f99:[1]}", h2);
//
//		ObjectMap om = s.serialize(ps, in, ObjectMap.class);
//		assertEquals("{f1:['foo'],f2:[[102,111,111]],f4:['2012-12-21T12:34:56Z'],f5:[[102,111,111]],f6:[[102,111,111]],f7:['foo'],f8:[1],f9:[1],f10:[1.0],f11:[1.0],f12:[true],f99:[1]}", om);
//
//		om = (ObjectMap)s.serialize(ps, in, Object.class);
//		assertEquals("{f1:['foo'],f2:[[102,111,111]],f4:['2012-12-21T12:34:56Z'],f5:[[102,111,111]],f6:[[102,111,111]],f7:['foo'],f8:[1],f9:[1],f10:[1.0],f11:[1.0],f12:[true],f99:[1]}", om);
//	}
//
//	@Test
//	public void h06_objectType_arrayProperties_pipes() throws Exception {
//		HttpPartSchema ps = schema("object")
//			.property("f1", schema("array").collectionFormat("pipes").items(schema("string")))
//			.property("f2", schema("array").collectionFormat("pipes").items(schema("string", "byte")))
//			.property("f4", schema("array").collectionFormat("pipes").items(schema("string", "date-time")))
//			.property("f5", schema("array").collectionFormat("pipes").items(schema("string", "binary")))
//			.property("f6", schema("array").collectionFormat("pipes").items(schema("string", "binary-spaced")))
//			.property("f7", schema("array").collectionFormat("pipes").items(schema("string", "uon")))
//			.property("f8", schema("array").collectionFormat("pipes").items(schema("integer")))
//			.property("f9", schema("array").collectionFormat("pipes").items(schema("integer", "int64")))
//			.property("f10", schema("array").collectionFormat("pipes").items(schema("number")))
//			.property("f11", schema("array").collectionFormat("pipes").items(schema("number", "double")))
//			.property("f12", schema("array").collectionFormat("pipes").items(schema("boolean")))
//			.additionalProperties(schema("array").collectionFormat("pipes").items(schema("integer")))
//			.build();
//
//		byte[] foob = "foo".getBytes(), barb = "bar".getBytes();
//		String in = "(f1=foo|bar,f2="+base64Encode(foob)+"|"+base64Encode(barb)+",f4=2012-12-21T12:34:56Z|2012-12-21T12:34:56Z,f5="+toHex(foob)+"|"+toHex(barb)+",f6='"+toSpacedHex(foob)+"|"+toSpacedHex(barb)+"',f7=foo|bar,f8=1|2,f9=1|2,f10=1|2,f11=1|2,f12=true|true,f99=1|2)";
//
//		H2 h2 = s.serialize(ps, in, H2.class);
//		assertEquals("{f1:['foo','bar'],f2:[[102,111,111],[98,97,114]],f4:['2012-12-21T12:34:56Z','2012-12-21T12:34:56Z'],f5:[[102,111,111],[98,97,114]],f6:[[102,111,111],[98,97,114]],f7:['foo','bar'],f8:[1,2],f9:[1,2],f10:[1.0,2.0],f11:[1.0,2.0],f12:[true,true],f99:[1,2]}", h2);
//
//		ObjectMap om = s.serialize(ps, in, ObjectMap.class);
//		assertEquals("{f1:['foo','bar'],f2:[[102,111,111],[98,97,114]],f4:['2012-12-21T12:34:56Z','2012-12-21T12:34:56Z'],f5:[[102,111,111],[98,97,114]],f6:[[102,111,111],[98,97,114]],f7:['foo','bar'],f8:[1,2],f9:[1,2],f10:[1.0,2.0],f11:[1.0,2.0],f12:[true,true],f99:[1,2]}", om);
//
//		om = (ObjectMap)s.serialize(ps, in, Object.class);
//		assertEquals("{f1:['foo','bar'],f2:[[102,111,111],[98,97,114]],f4:['2012-12-21T12:34:56Z','2012-12-21T12:34:56Z'],f5:[[102,111,111],[98,97,114]],f6:[[102,111,111],[98,97,114]],f7:['foo','bar'],f8:[1,2],f9:[1,2],f10:[1.0,2.0],f11:[1.0,2.0],f12:[true,true],f99:[1,2]}", om);
//	}
//
	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private static HttpPartSchemaBuilder schema() {
		return HttpPartSchema.create();
	}

	private static HttpPartSchemaBuilder schema(String type) {
		return HttpPartSchema.create(type);
	}

	private static HttpPartSchemaBuilder schema(String type, String format) {
		return HttpPartSchema.create(type, format);
	}
}