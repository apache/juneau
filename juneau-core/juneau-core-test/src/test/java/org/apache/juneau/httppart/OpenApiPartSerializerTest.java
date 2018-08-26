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
import static java.lang.String.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenApiPartSerializerTest {

	static OpenApiSerializerSession s = OpenApiSerializer.DEFAULT.createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Input validations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_outputValidations_nullOutput() throws Exception {
		HttpPartSchema ps = schema().build();
		assertEquals("null", s.serialize(ps, null));

		ps = schema().required(false).build();
		assertEquals("null", s.serialize(ps, null));

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
		assertEquals("null", s.serialize(ps, null));

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
		assertEquals("null", s.serialize(ps, null));

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

		assertEquals("null", s.serialize(ps, null));
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
			f = s;
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
		assertEquals("null", s.serialize(ps, new C1(null)));
		assertEquals("null", s.serialize(ps, null));
	}

	@Test
	public void c04_stringType_binaryFormat() throws Exception {
		HttpPartSchema ps = schema("string", "binary").build();
		byte[] foob = "foo".getBytes();
		String expected = toHex(foob);
		assertEquals(expected, s.serialize(ps, foob));
		assertEquals(expected, s.serialize(ps, new C1(foob)));
		assertEquals("null", s.serialize(ps, new C1(null)));
		assertEquals("null", s.serialize(ps, null));
	}

	@Test
	public void c05_stringType_binarySpacedFormat() throws Exception {
		HttpPartSchema ps = schema("string", "binary-spaced").build();
		byte[] foob = "foo".getBytes();
		String expected = toSpacedHex(foob);
		assertEquals(expected, s.serialize(ps, foob));
		assertEquals(expected, s.serialize(ps, new C1(foob)));
		assertEquals("null", s.serialize(ps, new C1(null)));
		assertEquals("null", s.serialize(ps, null));
	}

	@Test
	public void c06_stringType_dateFormat() throws Exception {
		HttpPartSchema ps = schema("string", "date").build();
		Calendar in = StringUtils.parseIsoCalendar("2012-12-21");
		assertTrue(s.serialize(ps, in).contains("2012"));
		assertEquals("null", s.serialize(ps, null));
	}

	@Test
	public void c07_stringType_dateTimeFormat() throws Exception {
		HttpPartSchema ps = schema("string", "date-time").build();
		Calendar in = StringUtils.parseIsoCalendar("2012-12-21T12:34:56.789");
		assertTrue(s.serialize(ps, in).contains("2012"));
		assertEquals("null", s.serialize(ps, null));
	}

	@Test
	public void c08_stringType_uonFormat() throws Exception {
		HttpPartSchema ps = schema("string", "uon").build();
		assertEquals("foo", s.serialize(ps, "foo"));
		assertEquals("'foo'", s.serialize(ps, "'foo'"));
		assertEquals("foo", s.serialize(ps, new C2("foo")));
		assertEquals("null", s.serialize(ps, new C2(null)));
		assertEquals("'null'", s.serialize(ps, new C2("null")));
		assertEquals("null", s.serialize(ps, null));
		// UonPartSerializerTest should handle all other cases.
	}

	@Test
	public void c09_stringType_noneFormat() throws Exception {
		// If no format is specified, then we should transform directly from a string.
		HttpPartSchema ps = schema("string").build();
		assertEquals("foo", s.serialize(ps, "foo"));
		assertEquals("'foo'", s.serialize(ps, "'foo'"));
		assertEquals("foo", s.serialize(ps, new C2("foo")));
		assertEquals("null", s.serialize(ps, new C2(null)));
		assertEquals("null", s.serialize(ps, new C2("null")));
		assertEquals("null", s.serialize(ps, null));
	}

	@Test
	public void c10_stringType_noneFormat_2d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("string")).build();
		assertEquals("foo,bar,null", s.serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo,bar,null", s.serialize(ps, AList.create("foo","bar",null)));
		assertEquals("foo,bar,null", s.serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo,bar,null", s.serialize(ps, AList.create((Object)"foo",(Object)"bar",null)));
		assertEquals("foo,bar,null,null", s.serialize(ps, new C2[]{new C2("foo"),new C2("bar"),new C2(null),null}));
		assertEquals("foo,bar,null,null", s.serialize(ps, AList.create(new C2("foo"),new C2("bar"),new C2(null),null)));
		assertEquals("foo,bar,null", s.serialize(ps, new C3("foo","bar",null)));
	}

	@Test
	public void c11_stringType_noneFormat_3d() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("string"))).build();
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, new String[][]{{"foo","bar"},{"baz",null},null}));
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, AList.create(new String[]{"foo","bar"}, new String[]{"baz",null},null)));
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, AList.create(AList.create("foo","bar"),AList.create("baz",null),null)));
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, new Object[][]{{"foo","bar"},{"baz",null},null}));
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, AList.create(new Object[]{"foo","bar"}, new String[]{"baz",null},null)));
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, AList.create(AList.create((Object)"foo",(Object)"bar"),AList.create((Object)"baz",null),null)));
		assertEquals("foo,bar|baz,null,null|null", s.serialize(ps, new C2[][]{{new C2("foo"),new C2("bar")},{new C2("baz"),new C2(null),null},null}));
		assertEquals("foo,bar|baz,null,null|null", s.serialize(ps, AList.create(new C2[]{new C2("foo"),new C2("bar")}, new C2[]{new C2("baz"),new C2(null),null},null)));
		assertEquals("foo,bar|baz,null,null|null", s.serialize(ps, AList.create(AList.create(new C2("foo"),new C2("bar")),AList.create(new C2("baz"),new C2(null),null),null)));
		assertEquals("foo,bar|baz,null|null|null", s.serialize(ps, new C3[]{new C3("foo","bar"),new C3("baz",null),new C3((String)null),null}));
		assertEquals("foo,bar|baz,null|null|null", s.serialize(ps, AList.create(new C3("foo","bar"),new C3("baz",null),new C3((String)null),null)));
	}

	@Test
	public void c12_stringType_uonKeywords_plain() throws Exception {
		HttpPartSchema ps = schema("string").build();
		// When serialized normally, the following should not be quoted.
		assertEquals("true", s.serialize(ps, "true"));
		assertEquals("false", s.serialize(ps, "false"));
		assertEquals("null", s.serialize(ps, "null"));
		assertEquals("null", s.serialize(ps, null));
		assertEquals("123", s.serialize(ps, "123"));
		assertEquals("1.23", s.serialize(ps, "1.23"));
	}

	@Test
	public void c13_stringType_uonKeywords_uon() throws Exception {
		HttpPartSchema ps = schema("string","uon").build();
		// When serialized as UON, the following should be quoted so that they're not confused with booleans or numbers.
		assertEquals("'true'", s.serialize(ps, "true"));
		assertEquals("'false'", s.serialize(ps, "false"));
		assertEquals("'null'", s.serialize(ps, "null"));
		assertEquals("null", s.serialize(ps, null));
		assertEquals("'123'", s.serialize(ps, "123"));
		assertEquals("'1.23'", s.serialize(ps, "1.23"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type = array
	//-----------------------------------------------------------------------------------------------------------------

	public static class D {
		private String f;
		public D(String in) {
			this.f = in;
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test
	public void d01_arrayType_collectionFormatCsv() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("csv").build();
		assertEquals("foo,bar,null", s.serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo,bar,null", s.serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo,bar,null,null", s.serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo,bar,null", s.serialize(ps, AList.create("foo","bar",null)));
		assertEquals("foo,bar,null", s.serialize(ps, AList.<Object>create("foo","bar",null)));
		assertEquals("foo,bar,null,null", s.serialize(ps, AList.create(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo,bar,null", s.serialize(ps, new ObjectList().append("foo","bar",null)));

		assertEquals("foo\\,bar,null", s.serialize(ps, new String[]{"foo,bar",null}));
	}

	@Test
	public void d02_arrayType_collectionFormatPipes() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").build();
		assertEquals("foo|bar|null", s.serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo|bar|null", s.serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo|bar|null|null", s.serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo|bar|null", s.serialize(ps, AList.create("foo","bar",null)));
		assertEquals("foo|bar|null", s.serialize(ps, AList.<Object>create("foo","bar",null)));
		assertEquals("foo|bar|null|null", s.serialize(ps, AList.create(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo|bar|null", s.serialize(ps, new ObjectList().append("foo","bar",null)));
	}

	@Test
	public void d03_arrayType_collectionFormatSsv() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("ssv").build();
		assertEquals("foo bar null", s.serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo bar null", s.serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo bar null null", s.serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo bar null", s.serialize(ps, AList.create("foo","bar",null)));
		assertEquals("foo bar null", s.serialize(ps, AList.<Object>create("foo","bar",null)));
		assertEquals("foo bar null null", s.serialize(ps, AList.create(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo bar null", s.serialize(ps, new ObjectList().append("foo","bar",null)));
	}

	@Test
	public void d04_arrayType_collectionFormatTsv() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("tsv").build();
		assertEquals("foo\tbar\tnull", s.serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo\tbar\tnull", s.serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo\tbar\tnull\tnull", s.serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo\tbar\tnull", s.serialize(ps, AList.create("foo","bar",null)));
		assertEquals("foo\tbar\tnull", s.serialize(ps, AList.<Object>create("foo","bar",null)));
		assertEquals("foo\tbar\tnull\tnull", s.serialize(ps, AList.create(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo\tbar\tnull", s.serialize(ps, new ObjectList().append("foo","bar",null)));
	}

	@Test
	public void d05_arrayType_collectionFormatUon() throws Exception {
		HttpPartSchema ps = schema("array","uon").build();
		assertEquals("@(foo,bar,'null',null)", s.serialize(ps, new String[]{"foo","bar","null",null}));
		assertEquals("@(foo,bar,'null',null)", s.serialize(ps, new Object[]{"foo","bar","null",null}));
		assertEquals("@(foo,bar,'null',null)", s.serialize(ps, new D[]{new D("foo"),new D("bar"),new D("null"),null}));
		assertEquals("@(foo,bar,'null',null)", s.serialize(ps, AList.create("foo","bar","null",null)));
		assertEquals("@(foo,bar,'null',null)", s.serialize(ps, AList.<Object>create("foo","bar","null",null)));
		assertEquals("@(foo,bar,'null',null)", s.serialize(ps, AList.create(new D("foo"),new D("bar"),new D("null"),null)));
		assertEquals("@(foo,bar,'null',null)", s.serialize(ps, new ObjectList().append("foo","bar","null",null)));
	}

	@Test
	public void d06a_arrayType_collectionFormatNone() throws Exception {
		HttpPartSchema ps = schema("array").build();
		assertEquals("foo,bar,null", s.serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo,bar,null", s.serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo,bar,null,null", s.serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo,bar,null", s.serialize(ps, AList.create("foo","bar",null)));
		assertEquals("foo,bar,null", s.serialize(ps, AList.<Object>create("foo","bar",null)));
		assertEquals("foo,bar,null,null", s.serialize(ps, AList.create(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo,bar,null", s.serialize(ps, new ObjectList().append("foo","bar",null)));
	}

	@Test
	public void d07_arrayType_collectionFormatMulti() throws Exception {
		// collectionFormat=multi really shouldn't be applicable to collections of values, so just use csv.
		HttpPartSchema ps = schema("array").collectionFormat("multi").build();
		assertEquals("foo,bar,null", s.serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo,bar,null", s.serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo,bar,null,null", s.serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo,bar,null", s.serialize(ps, AList.create("foo","bar",null)));
		assertEquals("foo,bar,null", s.serialize(ps, AList.<Object>create("foo","bar",null)));
		assertEquals("foo,bar,null,null", s.serialize(ps, AList.create(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo,bar,null", s.serialize(ps, new ObjectList().append("foo","bar",null)));
	}

	@Test
	public void d08_arrayType_collectionFormatCsvAndPipes() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").collectionFormat("csv")).build();
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, new String[][]{{"foo","bar"},{"baz",null},null}));
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, new Object[][]{{"foo","bar"},{"baz",null},null}));
		assertEquals("foo,bar|baz,null,null|null", s.serialize(ps, new D[][]{{new D("foo"),new D("bar")},{new D("baz"),new D(null),null},null}));
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, AList.create(AList.create("foo","bar"),AList.create("baz",null),null)));
		assertEquals("foo,bar|baz,null|null", s.serialize(ps, AList.create(AList.<Object>create("foo","bar"),AList.<Object>create("baz",null),null)));
		assertEquals("foo,bar|baz,null,null|null", s.serialize(ps, AList.create(AList.create(new D("foo"),new D("bar")),AList.create(new D("baz"),new D(null),null),null)));
	}

	@Test
	public void d09_arrayType_itemsInteger() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("csv").items(schema("integer")).build();
		assertEquals("1,2", s.serialize(ps, new int[]{1,2}));
		assertEquals("1,2,null", s.serialize(ps, new Integer[]{1,2,null}));
		assertEquals("1,2,null", s.serialize(ps, new Object[]{1,2,null}));
		assertEquals("1,2,null", s.serialize(ps, AList.create(1,2,null)));
	}

	@Test
	public void d10_arrayType_itemsInteger_2d() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").collectionFormat("csv").allowEmptyValue().items(schema("integer"))).build();
		assertEquals("1,2||null", s.serialize(ps, new int[][]{{1,2},{},null}));
		assertEquals("1,2,null||null", s.serialize(ps, new Integer[][]{{1,2,null},{},null}));
		assertEquals("1,2,null||null", s.serialize(ps, new Object[][]{{1,2,null},{},null}));
		assertEquals("1,2,null||null", s.serialize(ps, AList.create(AList.create(1,2,null),AList.create(),null)));
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
		assertEquals("true", s.serialize(ps, "true"));
		assertEquals("true", s.serialize(ps, new E1(true)));
		assertEquals("false", s.serialize(ps, false));
		assertEquals("false", s.serialize(ps, "false"));
		assertEquals("false", s.serialize(ps, new E1(false)));
		assertEquals("null", s.serialize(ps, null));
		assertEquals("null", s.serialize(ps, "null"));
		assertEquals("null", s.serialize(ps, new E1(null)));
	}

	@Test
	public void e02_booleanType_uon() throws Exception {
		HttpPartSchema ps = schema("boolean","uon").build();
		assertEquals("true", s.serialize(ps, true));
		assertEquals("true", s.serialize(ps, "true"));
		assertEquals("true", s.serialize(ps, new E1(true)));
		assertEquals("false", s.serialize(ps, false));
		assertEquals("false", s.serialize(ps, "false"));
		assertEquals("false", s.serialize(ps, new E1(false)));
		assertEquals("null", s.serialize(ps, null));
		assertEquals("null", s.serialize(ps, "null"));
		assertEquals("null", s.serialize(ps, new E1(null)));
	}

	@Test
	public void e03_booleanType_2d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("boolean")).build();
		assertEquals("true", s.serialize(ps, new boolean[]{true}));
		assertEquals("true,null", s.serialize(ps, new Boolean[]{true,null}));
		assertEquals("true,null", s.serialize(ps, AList.create(true,null)));
		assertEquals("true,null", s.serialize(ps, new String[]{"true",null}));
		assertEquals("true,null", s.serialize(ps, AList.create("true",null)));
		assertEquals("true,null", s.serialize(ps, new Object[]{true,null}));
		assertEquals("true,null", s.serialize(ps, AList.<Object>create(true,null)));
		assertEquals("true,null,null", s.serialize(ps, new E1[]{new E1(true),new E1(null),null}));
		assertEquals("true,null,null", s.serialize(ps, AList.create(new E1(true),new E1(null),null)));
		assertEquals("true,null", s.serialize(ps, new E2(true,null)));
	}

	@Test
	public void e04_booleanType_3d() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("boolean"))).build();
		assertEquals("true,true|false", s.serialize(ps, new boolean[][]{{true,true},{false}}));
		assertEquals("true,true|false", s.serialize(ps, AList.create(new boolean[]{true,true},new boolean[]{false})));
		assertEquals("true,true|false,null", s.serialize(ps, new Boolean[][]{{true,true},{false,null}}));
		assertEquals("true,true|false,null", s.serialize(ps, AList.create(new Boolean[]{true,true},new Boolean[]{false,null})));
		assertEquals("true,true|false,null", s.serialize(ps, AList.create(AList.create(true,true),AList.create(false,null))));
		assertEquals("true,true|false,null,null", s.serialize(ps, AList.create(AList.create("true","true"),AList.create("false","null",null))));
		assertEquals("true,true|false,null,null", s.serialize(ps, AList.create(new String[]{"true","true"},new String[]{"false","null",null})));
		assertEquals("true,true|false,null", s.serialize(ps, new Object[][]{{true,true},{false,null}}));
		assertEquals("true,true|false,null", s.serialize(ps, AList.create(AList.create((Object)true,(Object)true),AList.create((Object)false,null))));
		assertEquals("true,true|false,null", s.serialize(ps, AList.create(new Object[]{true,true},new Object[]{false,null})));
		assertEquals("true,true|false,null", s.serialize(ps, new E1[][]{{new E1(true),new E1(true)},{new E1(false),new E1(null)}}));
		assertEquals("true,true|false,null", s.serialize(ps, AList.create(AList.create(new E1(true),new E1(true)), AList.create(new E1(false),new E1(null)))));
		assertEquals("true,true|false,null", s.serialize(ps, AList.create(new E1[]{new E1(true),new E1(true)},new E1[]{new E1(false),new E1(null)})));
		assertEquals("true,true|false,null", s.serialize(ps, new E2[]{new E2(true,true),new E2(false,null)}));
		assertEquals("true,true|false,null", s.serialize(ps, AList.create(new E2(true,true),new E2(false,null))));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type = integer
	//-----------------------------------------------------------------------------------------------------------------

	public static class F1 {
		private Integer f;
		public F1(Integer in) {
			this.f = in;
		}
		public Integer toInteger() {
			return f;
		}
	}

	public static class F2 {
		private Integer[] f;
		public F2(Integer...in) {
			this.f = in;
		}
		public Integer[] toIntegerArray() {
			return f;
		}
	}

	public static class F3 {
		private Long f;
		public F3(Long in) {
			this.f = in;
		}
		public Long toLong() {
			return f;
		}
	}

	public static class F4 {
		private Long[] f;
		public F4(Long...in) {
			this.f = in;
		}
		public Long[] toLongArray() {
			return f;
		}
	}

	@Test
	public void f01_integerType_int32() throws Exception {
		HttpPartSchema ps = schema("integer", "int32").build();
		assertEquals("1", s.serialize(ps, 1));
		assertEquals("1", s.serialize(ps, new Integer(1)));
		assertEquals("1", s.serialize(ps, (short)1));
		assertEquals("1", s.serialize(ps, new Short((short)1)));
		assertEquals("1", s.serialize(ps, 1l));
		assertEquals("1", s.serialize(ps, new Long(1)));
		assertEquals("1", s.serialize(ps, "1"));
		assertEquals("1", s.serialize(ps, new F1(1)));
		assertEquals("null", s.serialize(ps, null));
		assertEquals("null", s.serialize(ps, "null"));
	}

	@Test
	public void f02_integerType_int32_2d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("integer", "int32")).build();
		assertEquals("1,2", s.serialize(ps, new int[]{1,2}));
		assertEquals("1,2,null", s.serialize(ps, new Integer[]{1,2,null}));
		assertEquals("1,2,null", s.serialize(ps, AList.create(1,2,null)));
		assertEquals("1,2", s.serialize(ps, new short[]{1,2}));
		assertEquals("1,2,null", s.serialize(ps, new Short[]{1,2,null}));
		assertEquals("1,2,null", s.serialize(ps, AList.create(new Short((short)1),new Short((short)2),null)));
		assertEquals("1,2", s.serialize(ps, new long[]{1l,2l}));
		assertEquals("1,2,null", s.serialize(ps, new Long[]{1l,2l,null}));
		assertEquals("1,2,null", s.serialize(ps, AList.create(1l,2l,null)));
		assertEquals("1,2,null,null", s.serialize(ps, new String[]{"1","2","null",null}));
		assertEquals("1,2,null,null", s.serialize(ps, AList.create("1","2","null",null)));
		assertEquals("1,2,null", s.serialize(ps, new Object[]{1,2,null}));
		assertEquals("1,2,null", s.serialize(ps, AList.<Object>create(1,2,null)));
		assertEquals("1,2,null,null", s.serialize(ps, new F1[]{new F1(1),new F1(2),new F1(null),null}));
		assertEquals("1,2,null,null", s.serialize(ps, AList.create(new F1(1),new F1(2),new F1(null),null)));
		assertEquals("1,2,null", s.serialize(ps, new F2(1,2,null)));
	}

	@Test
	public void f03_integerType_int32_3d() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("integer", "int32"))).build();
		assertEquals("1,2|3|null", s.serialize(ps, new int[][]{{1,2},{3},null}));
		assertEquals("1,2|3|null", s.serialize(ps, AList.create(new int[]{1,2},new int[]{3},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new Integer[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new Integer[]{1,2},new Integer[]{3,null},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(AList.create(1,2),AList.create(3,null),null)));
		assertEquals("1,2|3|null", s.serialize(ps, new short[][]{{1,2},{3},null}));
		assertEquals("1,2|3|null", s.serialize(ps, AList.create(new short[]{1,2},new short[]{3},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new Short[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new Short[]{1,2},new Short[]{3,null},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(AList.create(new Short((short)1),new Short((short)2)),AList.create(new Short((short)3),null),null)));
		assertEquals("1,2|3|null", s.serialize(ps, new long[][]{{1l,2l},{3l},null}));
		assertEquals("1,2|3|null", s.serialize(ps, AList.create(new long[]{1l,2l},new long[]{3l},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new Long[][]{{1l,2l},{3l,null},null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new Long[]{1l,2l},new Long[]{3l,null},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(AList.create(new Long(1),new Long(2)),AList.create(new Long(3),null),null)));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, new String[][]{{"1","2"},{"3","null",null},null}));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, AList.create(new String[]{"1","2"},new String[]{"3","null",null},null)));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, AList.create(AList.create("1","2"),AList.create("3","null",null),null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new Object[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new Object[]{1,2},new Object[]{3,null},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(AList.<Object>create(1,2),AList.<Object>create(3,null),null)));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, new F1[][]{{new F1(1),new F1(2)},{new F1(3),new F1(null),null},null}));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, AList.create(new F1[]{new F1(1),new F1(2)},new F1[]{new F1(3),new F1(null),null},null)));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, AList.create(AList.create(new F1(1),new F1(2)),AList.create(new F1(3),new F1(null),null),null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new F2[]{new F2(1,2),new F2(3,null),null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new F2(1,2),new F2(3,null),null)));
	}

	@Test
	public void f04_integerType_int64() throws Exception {
		HttpPartSchema ps = schema("integer", "int64").build();
		assertEquals("1", s.serialize(ps, 1));
		assertEquals("1", s.serialize(ps, new Integer(1)));
		assertEquals("1", s.serialize(ps, (short)1));
		assertEquals("1", s.serialize(ps, new Short((short)1)));
		assertEquals("1", s.serialize(ps, 1l));
		assertEquals("1", s.serialize(ps, new Long(1l)));
		assertEquals("1", s.serialize(ps, "1"));
		assertEquals("1", s.serialize(ps,  new F3(1l)));
		assertEquals("null", s.serialize(ps, null));
		assertEquals("null", s.serialize(ps, "null"));
	}

	@Test
	public void f05_integerType_int64_2d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("integer", "int64")).build();
		assertEquals("1,2", s.serialize(ps, new int[]{1,2}));
		assertEquals("1,2,null", s.serialize(ps, new Integer[]{1,2,null}));
		assertEquals("1,2,null", s.serialize(ps, AList.create(1,2,null)));
		assertEquals("1,2", s.serialize(ps, new short[]{1,2}));
		assertEquals("1,2,null", s.serialize(ps, new Short[]{1,2,null}));
		assertEquals("1,2,null", s.serialize(ps, AList.create((short)1,(short)2,null)));
		assertEquals("1,2", s.serialize(ps, new long[]{1l,2l}));
		assertEquals("1,2,null", s.serialize(ps, new Long[]{1l,2l,null}));
		assertEquals("1,2,null", s.serialize(ps, AList.create(1l,2l,null)));
		assertEquals("1,2,null,null", s.serialize(ps, new String[]{"1","2","null",null}));
		assertEquals("1,2,null,null", s.serialize(ps, AList.create("1","2","null",null)));
		assertEquals("1,2,null", s.serialize(ps, new Object[]{1,2,null}));
		assertEquals("1,2,null", s.serialize(ps, AList.create((Object)1,(Object)2,null)));
		assertEquals("1,2,null,null", s.serialize(ps, new F3[]{new F3(1l),new F3(2l),new F3(null),null}));
		assertEquals("1,2,null,null", s.serialize(ps, AList.create(new F3(1l),new F3(2l),new F3(null),null)));
		assertEquals("1,2,null", s.serialize(ps, new F4(1l,2l,null)));
	}

	@Test
	public void f06_integerType_int64_3d() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("integer", "int64"))).build();
		assertEquals("1,2|3|null", s.serialize(ps, new int[][]{{1,2},{3},null}));
		assertEquals("1,2|3|null", s.serialize(ps, AList.create(new int[]{1,2},new int[]{3},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new Integer[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new Integer[]{1,2},new Integer[]{3,null},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(AList.create(1,2),AList.create(3,null),null)));
		assertEquals("1,2|3|null", s.serialize(ps, new short[][]{{1,2},{3},null}));
		assertEquals("1,2|3|null", s.serialize(ps, AList.create(new short[]{1,2},new short[]{3},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new Short[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new Short[]{1,2},new Short[]{3,null},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(AList.create((short)1,(short)2),AList.create((short)3,null),null)));
		assertEquals("1,2|3|null", s.serialize(ps, new long[][]{{1l,2l},{3l},null}));
		assertEquals("1,2|3|null", s.serialize(ps, AList.create(new long[]{1l,2l},new long[]{3l},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new Long[][]{{1l,2l},{3l,null},null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new Long[]{1l,2l},new Long[]{3l,null},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(AList.create(1l,2l),AList.create(3l,null),null)));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, new String[][]{{"1","2"},{"3","null",null},null}));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, AList.create(new String[]{"1","2"},new String[]{"3","null",null},null)));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, AList.create(AList.create("1","2"),AList.create("3","null",null),null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new Object[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new Object[]{1,2},new Object[]{3,null},null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(AList.create((Object)1,(Object)2),AList.create((Object)3,null),null)));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, new F3[][]{{new F3(1l),new F3(2l)},{new F3(3l),new F3(null),null},null}));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, AList.create(new F3[]{new F3(1l),new F3(2l)},new F3[]{new F3(3l),new F3(null),null},null)));
		assertEquals("1,2|3,null,null|null", s.serialize(ps, AList.create(AList.create(new F3(1l),new F3(2l)),AList.create(new F3(3l),new F3(null),null),null)));
		assertEquals("1,2|3,null|null", s.serialize(ps, new F4[]{new F4(1l,2l),new F4(3l,null),null}));
		assertEquals("1,2|3,null|null", s.serialize(ps, AList.create(new F4(1l,2l),new F4(3l,null),null)));
	}


	//-----------------------------------------------------------------------------------------------------------------
	// type = number
	//-----------------------------------------------------------------------------------------------------------------

	public static class G1 {
		private Float f;
		public G1(Float in) {
			this.f = in;
		}
		public Float toFloat() {
			return f;
		}
	}

	public static class G2 {
		private Float[] f;
		public G2(Float...in) {
			this.f = in;
		}
		public Float[] toFloatArray() {
			return f;
		}
	}

	public static class G3 {
		private Double f;
		public G3(Double in) {
			this.f = in;
		}
		public Double toDouble() {
			return f;
		}
	}

	public static class G4 {
		private Double[] f;
		public G4(Double...in) {
			this.f = in;
		}
		public Double[] toDoubleArray() {
			return f;
		}
	}

	@Test
	public void g01_numberType_float() throws Exception {
		HttpPartSchema ps = schema("number", "float").build();
		assertEquals("1.0", s.serialize(ps, 1f));
		assertEquals("1.0", s.serialize(ps, new Float(1f)));
		assertEquals("1.0", s.serialize(ps, 1d));
		assertEquals("1.0", s.serialize(ps, new Double(1d)));
		assertEquals("1.0", s.serialize(ps, "1"));
		assertEquals("1.0", s.serialize(ps, new G1(1f)));
		assertEquals("null", s.serialize(ps, null));
		assertEquals("null", s.serialize(ps, "null"));
		assertEquals("null", s.serialize(ps, new G1(null)));
	}

	@Test
	public void g02_numberType_float_2d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("number", "float")).build();
		assertEquals("1.0,2.0", s.serialize(ps, new float[]{1,2}));
		assertEquals("1.0,2.0,null", s.serialize(ps, new Float[]{1f,2f,null}));
		assertEquals("1.0,2.0,null", s.serialize(ps, AList.create(1f,2f,null)));
		assertEquals("1.0,2.0", s.serialize(ps, new double[]{1,2}));
		assertEquals("1.0,2.0,null", s.serialize(ps, new Double[]{1d,2d,null}));
		assertEquals("1.0,2.0,null", s.serialize(ps, AList.create(1d,2d,null)));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, new String[]{"1","2","null",null}));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, AList.create("1","2","null",null)));
		assertEquals("1.0,2.0,null", s.serialize(ps, new Object[]{1,2,null}));
		assertEquals("1.0,2.0,null", s.serialize(ps, AList.create((Object)1,(Object)2,null)));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, new G1[]{new G1(1f),new G1(2f),new G1(null),null}));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, AList.create(new G1(1f),new G1(2f),new G1(null),null)));
		assertEquals("1.0,2.0,null", s.serialize(ps, new G2(1f,2f,null)));
	}

	@Test
	public void g03_numberType_float_3d() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("number", "float"))).build();
		assertEquals("1.0,2.0|3.0|null", s.serialize(ps, new float[][]{{1,2},{3},null}));
		assertEquals("1.0,2.0|3.0|null", s.serialize(ps, AList.create(new float[]{1,2},new float[]{3},null)));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, new Float[][]{{1f,2f},{3f,null},null}));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, AList.create(new Float[]{1f,2f},new Float[]{3f,null}, null)));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, AList.create(AList.create(1f,2f),AList.create(3f,null),null)));
		assertEquals("1.0,2.0|3.0|null", s.serialize(ps, new double[][]{{1d,2d},{3d},null}));
		assertEquals("1.0,2.0|3.0|null", s.serialize(ps, AList.create(new double[]{1d,2d},new double[]{3d},null)));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, new Double[][]{{1d,2d},{3d,null},null}));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, AList.create(new Double[]{1d,2d},new Double[]{3d,null},null)));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, AList.create(AList.create(1d,2d),AList.create(3d,null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, new String[][]{{"1","2"},{"3","null",null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(new String[]{"1","2"},new String[]{"3","null",null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(AList.create(1d,2d),AList.create(3f,"null",null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, new Object[][]{{1d,2d},{3f,"null",null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(new Object[]{1d,2d},new Object[]{3f,"null",null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(AList.create(1d,2d),AList.create(3f,"null",null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, new G1[][]{{new G1(1f),new G1(2f)},{new G1(3f),new G1(null),null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(new G1[]{new G1(1f),new G1(2f)},new G1[]{new G1(3f),new G1(null),null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(AList.create(new G1(1f),new G1(2f)),AList.create(new G1(3f),new G1(null),null),null)));
		assertEquals("1.0,2.0,null|null", s.serialize(ps, new G2[]{new G2(1f,2f,null),null}));
		assertEquals("1.0,2.0,null|null", s.serialize(ps, AList.create(new G2(1f,2f,null),null)));
	}

	@Test
	public void g04_numberType_double() throws Exception {
		HttpPartSchema ps = schema("number", "double").build();
		assertEquals("1.0", s.serialize(ps, 1f));
		assertEquals("1.0", s.serialize(ps, new Float(1f)));
		assertEquals("1.0", s.serialize(ps, 1d));
		assertEquals("1.0", s.serialize(ps, new Double(1d)));
		assertEquals("1.0", s.serialize(ps, "1"));
		assertEquals("1.0", s.serialize(ps, new G3(1d)));
		assertEquals("null", s.serialize(ps, null));
		assertEquals("null", s.serialize(ps, "null"));
		assertEquals("null", s.serialize(ps, new G3(null)));
	}

	@Test
	public void g05_numberType_double_2d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("number", "double")).build();
		assertEquals("1.0,2.0", s.serialize(ps, new float[]{1,2}));
		assertEquals("1.0,2.0,null", s.serialize(ps, new Float[]{1f,2f,null}));
		assertEquals("1.0,2.0,null", s.serialize(ps, AList.create(1f,2f,null)));
		assertEquals("1.0,2.0", s.serialize(ps, new double[]{1,2}));
		assertEquals("1.0,2.0,null", s.serialize(ps, new Double[]{1d,2d,null}));
		assertEquals("1.0,2.0,null", s.serialize(ps, AList.create(1d,2d,null)));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, new String[]{"1","2","null",null}));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, AList.create("1","2","null",null)));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, new Object[]{1d,2f,"null",null}));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, AList.create(1d,2f,"null",null)));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, new G3[]{new G3(1d),new G3(2d),new G3(null),null}));
		assertEquals("1.0,2.0,null,null", s.serialize(ps, AList.create(new G3(1d),new G3(2d),new G3(null),null)));
		assertEquals("1.0,2.0,null", s.serialize(ps, new G4(1d,2d,null)));
	}

	@Test
	public void g06_numberType_double_3d() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("array").items(schema("number", "double"))).build();
		assertEquals("1.0,2.0|3.0|null", s.serialize(ps, new float[][]{{1f,2f},{3f},null}));
		assertEquals("1.0,2.0|3.0|null", s.serialize(ps, AList.create(new float[]{1f,2f},new float[]{3f},null)));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, new Float[][]{{1f,2f},{3f,null},null}));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, AList.create(new Float[]{1f,2f},new Float[]{3f,null},null)));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, AList.create(AList.create(1f,2f),AList.create(3f,null),null)));
		assertEquals("1.0,2.0|3.0|null", s.serialize(ps, new double[][]{{1d,2d},{3d},null}));
		assertEquals("1.0,2.0|3.0|null", s.serialize(ps, AList.create(new double[]{1d,2d},new double[]{3d},null)));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, new Double[][]{{1d,2d},{3d,null},null}));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, AList.create(new Double[]{1d,2d},new Double[]{3d,null},null)));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, AList.create(AList.create(1d,2d),AList.create(3d,null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, new String[][]{{"1","2"},{"3","null",null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(new String[]{"1","2"},new String[]{"3","null",null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(AList.create("1","2"),AList.create("3","null",null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, new Object[][]{{1d,2d},{"3","null",null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(new Object[]{1d,2d},new Object[]{"3","null",null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(AList.create(1d,2f),AList.create(3d,"null",null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, new G3[][]{{new G3(1d),new G3(2d)},{new G3(3d),new G3(null),null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(new G3[]{new G3(1d),new G3(2d)},new G3[]{new G3(3d),new G3(null),null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", s.serialize(ps, AList.create(AList.create(new G3(1d),new G3(2d)),AList.create(new G3(3d),new G3(null),null),null)));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, new G4[]{new G4(1d,2d),new G4(3d,null),null}));
		assertEquals("1.0,2.0|3.0,null|null", s.serialize(ps, AList.create(new G4(1d,2d),new G4(3d,null),null)));
	}


	//-----------------------------------------------------------------------------------------------------------------
	// type = object
	//-----------------------------------------------------------------------------------------------------------------

	public static class H1 {
		public String f1;
		public Integer f2;
		public Boolean f3;
		public H1(String f1, Integer f2, Boolean f3) {
			this.f1 = f1;
			this.f2 = f2;
			this.f3 = f3;
		}
	}

	@Test
	public void h01_objectType() throws Exception {
		HttpPartSchema ps = schema("object").build();
		assertEquals("(f1='1',f2=2,f3=true)", s.serialize(ps, new H1("1",2,true)));
		assertEquals("()", s.serialize(ps, new H1(null,null,null)));
		assertEquals("(f1='1',f2=2,f3=true)", s.serialize(ps, new ObjectMap("{f1:'1',f2:2,f3:true}")));
		assertEquals("(f1=null,f2=null,f3=null)", s.serialize(ps, new ObjectMap("{f1:null,f2:null,f3:null}")));
		assertEquals("null", s.serialize(ps, null));
	}

	@Test
	public void h02_objectType_uon() throws Exception {
		HttpPartSchema ps = schema("object","uon").build();
		assertEquals("(f1='1',f2=2,f3=true)", s.serialize(ps, new H1("1",2,true)));
		assertEquals("()", s.serialize(ps, new H1(null,null,null)));
		assertEquals("(f1='1',f2=2,f3=true)", s.serialize(ps, new ObjectMap("{f1:'1',f2:2,f3:true}")));
		assertEquals("(f1=null,f2=null,f3=null)", s.serialize(ps, new ObjectMap("{f1:null,f2:null,f3:null}")));
		assertEquals("null", s.serialize(ps, null));
	}

	@Test
	public void h03_objectType_2d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("object")).build();
		assertEquals("(f1='1'\\,f2=2\\,f3=true),(),null", s.serialize(ps, new H1[]{new H1("1",2,true),new H1(null,null,null),null}));
		assertEquals("(f1='1'\\,f2=2\\,f3=true),(),null", s.serialize(ps, AList.create(new H1("1",2,true),new H1(null,null,null),null)));
		assertEquals("(f1='1'\\,f2=2\\,f3=true),(f1=null\\,f2=null\\,f3=null),null", s.serialize(ps, new ObjectMap[]{new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:null,f2:null,f3:null}"),null}));
		assertEquals("(f1='1'\\,f2=2\\,f3=true),(f1=null\\,f2=null\\,f3=null),null", s.serialize(ps, AList.create(new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:null,f2:null,f3:null}"),null)));
		assertEquals("(f1='1'\\,f2=2\\,f3=true),(f1='1'\\,f2=2\\,f3=true),null", s.serialize(ps, new Object[]{new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}"),null}));
		assertEquals("(f1='1'\\,f2=2\\,f3=true),(f1='1'\\,f2=2\\,f3=true),null", s.serialize(ps, AList.create(new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}"),null)));
	}

	@Test
	public void h03_objectType_2d_pipes() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("pipes").items(schema("object")).build();
		assertEquals("(f1='1',f2=2,f3=true)|()|null", s.serialize(ps, new H1[]{new H1("1",2,true),new H1(null,null,null),null}));
		assertEquals("(f1='1',f2=2,f3=true)|()|null", s.serialize(ps, AList.create(new H1("1",2,true),new H1(null,null,null),null)));
		assertEquals("(f1='1',f2=2,f3=true)|(f1=null,f2=null,f3=null)|null", s.serialize(ps, new ObjectMap[]{new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:null,f2:null,f3:null}"),null}));
		assertEquals("(f1='1',f2=2,f3=true)|(f1=null,f2=null,f3=null)|null", s.serialize(ps, AList.create(new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:null,f2:null,f3:null}"),null)));
		assertEquals("(f1='1',f2=2,f3=true)|(f1='1',f2=2,f3=true)|null", s.serialize(ps, new Object[]{new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}"),null}));
		assertEquals("(f1='1',f2=2,f3=true)|(f1='1',f2=2,f3=true)|null", s.serialize(ps, AList.create(new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}"),null)));
	}

	@Test
	public void h04_objectType_2d_uon() throws Exception {
		HttpPartSchema ps = schema("array","uon").items(schema("object")).build();
		assertEquals("@((f1='1',f2=2,f3=true),(),null)", s.serialize(ps, new H1[]{new H1("1",2,true),new H1(null,null,null),null}));
		assertEquals("@((f1='1',f2=2,f3=true),(),null)", s.serialize(ps, AList.create(new H1("1",2,true),new H1(null,null,null),null)));
		assertEquals("@((f1='1',f2=2,f3=true),(f1=null,f2=null,f3=null),null)", s.serialize(ps, new ObjectMap[]{new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:null,f2:null,f3:null}"),null}));
		assertEquals("@((f1='1',f2=2,f3=true),(f1=null,f2=null,f3=null),null)", s.serialize(ps, AList.create(new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:null,f2:null,f3:null}"),null)));
		assertEquals("@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true),null)", s.serialize(ps, new Object[]{new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}"),null}));
		assertEquals("@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true),null)", s.serialize(ps, AList.create(new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}"),null)));
	}

	@Test
	public void h03_objectType_3d() throws Exception {
		HttpPartSchema ps = schema("array").items(schema("array").items(schema("object"))).build();
		assertEquals("(f1='1'\\\\\\,f2=2\\\\\\,f3=true)\\,(f1=x\\\\\\,f2=3\\\\\\,f3=false),()\\,null,null", s.serialize(ps, new H1[][]{{new H1("1",2,true),new H1("x",3,false)},{new H1(null,null,null),null},null}));
		assertEquals("(f1='1'\\\\\\,f2=2\\\\\\,f3=true)\\,(f1=x\\\\\\,f2=3\\\\\\,f3=false),()\\,null,null", s.serialize(ps, AList.create(new H1[]{new H1("1",2,true),new H1("x",3,false)},new H1[]{new H1(null,null,null),null},null)));
		assertEquals("(f1='1'\\\\\\,f2=2\\\\\\,f3=true)\\,(f1=x\\\\\\,f2=3\\\\\\,f3=false),()\\,null,null", s.serialize(ps, AList.create(AList.create(new H1("1",2,true),new H1("x",3,false)),AList.create(new H1(null,null,null),null),null)));
		assertEquals("(f1='1'\\\\\\,f2=2\\\\\\,f3=true)\\,(f1=x\\\\\\,f2=4\\\\\\,f3=false),(f1=null\\\\\\,f2=null\\\\\\,f3=null)\\,null,null", s.serialize(ps, new ObjectMap[][]{{new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:'x',f2:4,f3:false}")},{new ObjectMap("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("(f1='1'\\\\\\,f2=2\\\\\\,f3=true)\\,(f1=x\\\\\\,f2=4\\\\\\,f3=false),(f1=null\\\\\\,f2=null\\\\\\,f3=null)\\,null,null", s.serialize(ps, AList.create(new ObjectMap[]{new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:'x',f2:4,f3:false}")},new ObjectMap[]{new ObjectMap("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("(f1='1'\\\\\\,f2=2\\\\\\,f3=true)\\,(f1=x\\\\\\,f2=4\\\\\\,f3=false),(f1=null\\\\\\,f2=null\\\\\\,f3=null)\\,null,null", s.serialize(ps, AList.create(AList.create(new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:'x',f2:4,f3:false}")),AList.create(new ObjectMap("{f1:null,f2:null,f3:null}"),null),null)));
		assertEquals("(f1='1'\\\\\\,f2=2\\\\\\,f3=true)\\,(f1='1'\\\\\\,f2=2\\\\\\,f3=true),()\\,(f1=null\\\\\\,f2=null\\\\\\,f3=null)\\,null,null", s.serialize(ps, new Object[][]{{new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}")},{new H1(null,null,null),new ObjectMap("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("(f1='1'\\\\\\,f2=2\\\\\\,f3=true)\\,(f1='1'\\\\\\,f2=2\\\\\\,f3=true),()\\,(f1=null\\\\\\,f2=null\\\\\\,f3=null)\\,null,null", s.serialize(ps, AList.create(new Object[]{new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}")},new Object[]{new H1(null,null,null),new ObjectMap("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("(f1='1'\\\\\\,f2=2\\\\\\,f3=true)\\,(f1='1'\\\\\\,f2=2\\\\\\,f3=true),()\\,(f1=null\\\\\\,f2=null\\\\\\,f3=null)\\,null,null", s.serialize(ps, AList.create(AList.create(new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}")),AList.create(new H1(null,null,null),new ObjectMap("{f1:null,f2:null,f3:null}"),null),null)));
	}

	@Test
	public void h03_objectType_3d_ssvAndPipes() throws Exception {
		HttpPartSchema ps = schema("array").collectionFormat("ssv").items(schema("array").collectionFormat("pipes").items(schema("object"))).build();
		assertEquals("(f1='1',f2=2,f3=true)|(f1=x,f2=3,f3=false) ()|null null", s.serialize(ps, new H1[][]{{new H1("1",2,true),new H1("x",3,false)},{new H1(null,null,null),null},null}));
		assertEquals("(f1='1',f2=2,f3=true)|(f1=x,f2=3,f3=false) ()|null null", s.serialize(ps, AList.create(new H1[]{new H1("1",2,true),new H1("x",3,false)},new H1[]{new H1(null,null,null),null},null)));
		assertEquals("(f1='1',f2=2,f3=true)|(f1=x,f2=3,f3=false) ()|null null", s.serialize(ps, AList.create(AList.create(new H1("1",2,true),new H1("x",3,false)),AList.create(new H1(null,null,null),null),null)));
		assertEquals("(f1='1',f2=2,f3=true)|(f1=x,f2=4,f3=false) (f1=null,f2=null,f3=null)|null null", s.serialize(ps, new ObjectMap[][]{{new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:'x',f2:4,f3:false}")},{new ObjectMap("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("(f1='1',f2=2,f3=true)|(f1=x,f2=4,f3=false) (f1=null,f2=null,f3=null)|null null", s.serialize(ps, AList.create(new ObjectMap[]{new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:'x',f2:4,f3:false}")},new ObjectMap[]{new ObjectMap("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("(f1='1',f2=2,f3=true)|(f1=x,f2=4,f3=false) (f1=null,f2=null,f3=null)|null null", s.serialize(ps, AList.create(AList.create(new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:'x',f2:4,f3:false}")),AList.create(new ObjectMap("{f1:null,f2:null,f3:null}"),null),null)));
		assertEquals("(f1='1',f2=2,f3=true)|(f1='1',f2=2,f3=true) ()|(f1=null,f2=null,f3=null)|null null", s.serialize(ps, new Object[][]{{new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}")},{new H1(null,null,null),new ObjectMap("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("(f1='1',f2=2,f3=true)|(f1='1',f2=2,f3=true) ()|(f1=null,f2=null,f3=null)|null null", s.serialize(ps, AList.create(new Object[]{new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}")},new Object[]{new H1(null,null,null),new ObjectMap("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("(f1='1',f2=2,f3=true)|(f1='1',f2=2,f3=true) ()|(f1=null,f2=null,f3=null)|null null", s.serialize(ps, AList.create(AList.create(new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}")),AList.create(new H1(null,null,null),new ObjectMap("{f1:null,f2:null,f3:null}"),null),null)));
	}

	@Test
	public void h03_objectType_3d_uon() throws Exception {
		HttpPartSchema ps = schema("array","uon").items(schema("array").items(schema("object"))).build();
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=3,f3=false)),@((),null),null)", s.serialize(ps, new H1[][]{{new H1("1",2,true),new H1("x",3,false)},{new H1(null,null,null),null},null}));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=3,f3=false)),@((),null),null)", s.serialize(ps, AList.create(new H1[]{new H1("1",2,true),new H1("x",3,false)},new H1[]{new H1(null,null,null),null},null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=3,f3=false)),@((),null),null)", s.serialize(ps, AList.create(AList.create(new H1("1",2,true),new H1("x",3,false)),AList.create(new H1(null,null,null),null),null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=4,f3=false)),@((f1=null,f2=null,f3=null),null),null)", s.serialize(ps, new ObjectMap[][]{{new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:'x',f2:4,f3:false}")},{new ObjectMap("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=4,f3=false)),@((f1=null,f2=null,f3=null),null),null)", s.serialize(ps, AList.create(new ObjectMap[]{new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:'x',f2:4,f3:false}")},new ObjectMap[]{new ObjectMap("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=4,f3=false)),@((f1=null,f2=null,f3=null),null),null)", s.serialize(ps, AList.create(AList.create(new ObjectMap("{f1:'1',f2:2,f3:true}"),new ObjectMap("{f1:'x',f2:4,f3:false}")),AList.create(new ObjectMap("{f1:null,f2:null,f3:null}"),null),null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true)),@((),(f1=null,f2=null,f3=null),null),null)", s.serialize(ps, new Object[][]{{new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}")},{new H1(null,null,null),new ObjectMap("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true)),@((),(f1=null,f2=null,f3=null),null),null)", s.serialize(ps, AList.create(new Object[]{new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}")},new Object[]{new H1(null,null,null),new ObjectMap("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true)),@((),(f1=null,f2=null,f3=null),null),null)", s.serialize(ps, AList.create(AList.create(new H1("1",2,true),new ObjectMap("{f1:'1',f2:2,f3:true}")),AList.create(new H1(null,null,null),new ObjectMap("{f1:null,f2:null,f3:null}"),null),null)));
	}

	public static class H2 {
		public Object f1, f2, f4, f5, f6, f7, f8, f9, f10, f11, f12, f99;
		public H2(Object f1, Object f2, Object f4, Object f5, Object f6, Object f7, Object f8, Object f9, Object f10, Object f11, Object f12, Object f99) {
			this.f1 = f1;
			this.f2 = f2;
			this.f4 = f4;
			this.f5 = f5;
			this.f6 = f6;
			this.f7 = f7;
			this.f8 = f8;
			this.f9 = f9;
			this.f10 = f10;
			this.f11 = f11;
			this.f12 = f12;
			this.f99 = f99;
		}
	}

	@Test
	public void h04_objectType_simpleProperties() throws Exception {
		HttpPartSchema ps = schema("object")
			.property("f1", schema("string"))
			.property("f2", schema("string", "byte"))
			.property("f4", schema("string", "date-time"))
			.property("f5", schema("string", "binary"))
			.property("f6", schema("string", "binary-spaced"))
			.property("f7", schema("string", "uon"))
			.property("f8", schema("integer"))
			.property("f9", schema("integer", "int64"))
			.property("f10", schema("number"))
			.property("f11", schema("number", "double"))
			.property("f12", schema("boolean"))
			.additionalProperties(schema("integer"))
			.build();

		byte[] foob = "foo".getBytes();

		assertEquals(
			"(f1=foo,f2=Zm9v,f4=2012-12-21T12:34:56Z,f5=666F6F,f6='66 6F 6F',f7=foo,f8=1,f9=2,f10=1.0,f11=1.0,f12=true,f99=1)",
			s.serialize(ps, new H2("foo",foob,parseIsoCalendar("2012-12-21T12:34:56Z"),foob,foob,"foo",1,2,1.0,1.0,true,1))
		);
		assertEquals("()", s.serialize(ps, new H2(null,null,null,null,null,null,null,null,null,null,null,null)));
		assertEquals("null", s.serialize(ps, null));
		assertEquals(
			"(f1=foo,f2=Zm9v,f4=2012-12-21T12:34:56Z,f5=666F6F,f6='66 6F 6F',f7=foo,f8=1,f9=2,f10=1.0,f11=1.0,f12=true,f99=1)",
			s.serialize(ps, new ObjectMap().append("f1","foo").append("f2",foob).append("f4",parseIsoCalendar("2012-12-21T12:34:56Z")).append("f5",foob).append("f6",foob).append("f7","foo").append("f8",1).append("f9",2).append("f10",1.0).append("f11",1.0).append("f12",true).append("f99",1))
		);
	}

	@Test
	public void h05_objectType_arrayProperties() throws Exception {
		HttpPartSchema ps = schema("object")
			.property("f1", schema("array").items(schema("string")))
			.property("f2", schema("array").items(schema("string", "byte")))
			.property("f4", schema("array").items(schema("string", "date-time")))
			.property("f5", schema("array").items(schema("string", "binary")))
			.property("f6", schema("array").items(schema("string", "binary-spaced")))
			.property("f7", schema("array").items(schema("string", "uon")))
			.property("f8", schema("array").items(schema("integer")))
			.property("f9", schema("array").items(schema("integer", "int64")))
			.property("f10", schema("array").items(schema("number")))
			.property("f11", schema("array").items(schema("number", "double")))
			.property("f12", schema("array").items(schema("boolean")))
			.additionalProperties(schema("array").items(schema("integer")))
			.build();

		byte[] foob = "foo".getBytes();

		assertEquals(
			"(f1=@('a,b',null),f2=@(Zm9v,null),f4=@(2012-12-21T12:34:56Z,null),f5=@(666F6F,null),f6=@('66 6F 6F',null),f7=@(a,b,null),f8=@(1,2,null),f9=@(3,4,null),f10=@(1.0,2.0,null),f11=@(3.0,4.0,null),f12=@(true,false,null),f99=@(1,x,null))",
			s.serialize(ps, new H2(new String[]{"a,b",null},new byte[][]{foob,null},new Calendar[]{parseIsoCalendar("2012-12-21T12:34:56Z"),null},new byte[][]{foob,null},new byte[][]{foob,null},new String[]{"a","b",null},new Integer[]{1,2,null},new Integer[]{3,4,null},new Float[]{1f,2f,null},new Float[]{3f,4f,null},new Boolean[]{true,false,null},new Object[]{1,"x",null}))
		);

	}

	//-----------------------------------------------------------------------------------------------------------------
	// No-schema tests
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void i01a_noSchemaTests_Integer() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		for (Integer v : AList.create(new Integer(1), Integer.MAX_VALUE, Integer.MIN_VALUE))
			assertEquals(valueOf(v), s.serialize(null, v));
	}
	@Test
	public void i01b_noSchemaTests_IntegerArray() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		assertEquals("1,2147483647,-2147483648", s.serialize(null, new Integer[]{new Integer(1), Integer.MAX_VALUE, Integer.MIN_VALUE}));
	}

	@Test
	public void i02a_noSchemaTests_Short() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		for (Short v : AList.create(new Short((short)1), Short.MAX_VALUE, Short.MIN_VALUE))
			assertEquals(valueOf(v), s.serialize(null, v));
	}

	@Test
	public void i02b_noSchemaTests_ShortArray() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		assertEquals("1,32767,-32768,null", s.serialize(null, new Short[]{new Short((short)1), Short.MAX_VALUE, Short.MIN_VALUE, null}));
	}

	@Test
	public void i03a_noSchemaTests_Long() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		for (Long v : AList.create(new Long(1), Long.MAX_VALUE, Long.MIN_VALUE))
			assertEquals(valueOf(v), s.serialize(null, v));
	}

	@Test
	public void i03b_noSchemaTests_LongArray() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		assertEquals("1,9223372036854775807,-9223372036854775808,null", s.serialize(null, new Long[]{new Long(1), Long.MAX_VALUE, Long.MIN_VALUE, null}));
	}

	@Test
	public void i04a_noSchemaTests_Float() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		for (Float v : AList.create(new Float(1f), Float.MAX_VALUE, Float.MIN_VALUE))
			assertEquals(valueOf(v), s.serialize(null, v));
	}

	@Test
	public void i04b_noSchemaTests_FloatArray() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		assertEquals("1.0,3.4028235E38,1.4E-45", s.serialize(null, new Float[]{new Float(1f), Float.MAX_VALUE, Float.MIN_VALUE}));
	}

	@Test
	public void i05a_noSchemaTests_Double() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		for (Double v : AList.create(new Double(1d), Double.MAX_VALUE, Double.MIN_VALUE))
			assertEquals(valueOf(v), s.serialize(null, v));
	}

	@Test
	public void i05b_noSchemaTests_DoubleArray() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		assertEquals("1.0,1.7976931348623157E308,4.9E-324", s.serialize(null, new Double[]{new Double(1), Double.MAX_VALUE, Double.MIN_VALUE}));
	}

	@Test
	public void i06a_noSchemaTests_Boolean() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		for (Boolean v : AList.create(Boolean.TRUE, Boolean.FALSE))
			assertEquals(valueOf(v), s.serialize(null, v));
	}

	@Test
	public void i06b_noSchemaTests_BooleanArray() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		assertEquals("true,false,null", s.serialize(null, new Boolean[]{Boolean.TRUE, Boolean.FALSE, null}));
	}

	@Test
	public void i07_noSchemaTests_Null() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		assertEquals("null", s.serialize(null, null));
	}

	@Test
	public void i08a_noSchemaTests_String() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		for (String v : AList.create("foo", "", null))
			assertEquals(valueOf(v), s.serialize(null, v));
	}
	@Test
	public void i08b_noSchemaTests_StringArray() throws Exception {
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		assertEquals("foo,,null", s.serialize(null, new String[]{"foo", "", null}));
	}

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