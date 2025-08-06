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

import static java.lang.String.valueOf;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

class OpenApiPartSerializer_Test extends SimpleTestBase {

	static OpenApiSerializerSession s = OpenApiSerializer.DEFAULT.getSession();

	private static String serialize(HttpPartSchema schema, Object o) throws SchemaValidationException, SerializeException {
		return s.serialize(null, schema, o);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Input validations
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_outputValidations_nullOutput() throws Exception {
		assertEquals("null", serialize(T_NONE, null));
		assertEquals("null", serialize(tNone().required(false).build(), null));
		assertThrows(SchemaValidationException.class, ()->serialize(tNone().required().build(), null), "Required value not provided.");
		assertThrows(SchemaValidationException.class, ()->serialize(tNone().required(true).build(), null), "Required value not provided.");
	}

	@Test void a02_outputValidations_emptyOutput() throws Exception {
		assertEquals("", serialize(tNone().allowEmptyValue().build(), ""));
		assertEquals("", serialize(tNone().allowEmptyValue().build(), ""));
		assertThrows(SchemaValidationException.class, ()->serialize(tNone().allowEmptyValue(false).build(), ""), "Empty value not allowed.");
		assertThrows(SchemaValidationException.class, ()->serialize(tNone().allowEmptyValue(false).build(), ""), "Empty value not allowed.");
		assertEquals(" ", serialize(tNone().allowEmptyValue(false).build(), " "));
	}

	@Test void a03_outputValidations_pattern() throws Exception {
		final HttpPartSchema ps = tNone().pattern("x.*").allowEmptyValue().build();
		assertEquals("x", serialize(ps, "x"));
		assertEquals("xx", serialize(ps, "xx"));
		assertEquals("null", serialize(ps, null));

		assertThrows(SchemaValidationException.class, ()->serialize(ps, "y"), "Value does not match expected pattern.  Must match pattern: x.*");
		assertThrows(SchemaValidationException.class, ()->serialize(ps, ""), "Value does not match expected pattern.  Must match pattern: x.*");

		// Blank/null patterns are ignored.
		assertEquals("x", serialize(tNone().pattern("").allowEmptyValue().build(), "x"));
		assertEquals("x", serialize(tNone().pattern(null).allowEmptyValue().build(), "x"));
	}

	@Test void a04_outputValidations_enum() throws Exception {
		final HttpPartSchema ps = tNone()._enum("foo").allowEmptyValue().build();

		assertEquals("foo", serialize(ps, "foo"));
		assertEquals("null", serialize(ps, null));

		assertThrows(SchemaValidationException.class, ()->serialize(ps, "bar"), "Value does not match one of the expected values.  Must be one of the following:  foo");
		assertThrows(SchemaValidationException.class, ()->serialize(ps, ""), "Value does not match one of the expected values.  Must be one of the following:  foo");

		assertEquals("foo", serialize(tNone()._enum((Set<String>)null).build(), "foo"));
		assertEquals("foo", serialize(tNone()._enum((Set<String>)null).allowEmptyValue().build(), "foo"));
		assertEquals("foo", serialize(tNone()._enum("foo","foo").build(), "foo"));
	}

	@Test void a05_outputValidations_minMaxLength() throws Exception {
		HttpPartSchema ps = tNone().minLength(1L).maxLength(2L).allowEmptyValue().build();

		assertEquals("null", serialize(ps, null));
		assertEquals("1", serialize(ps, "1"));
		assertEquals("12", serialize(ps, "12"));

		assertThrows(SchemaValidationException.class, ()->serialize(ps, ""), "Minimum length of value not met.");
		assertThrows(SchemaValidationException.class, ()->serialize(ps, "123"), "Maximum length of value exceeded.");
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


	@Test void c01_stringType_simple() throws Exception {
		HttpPartSchema ps = T_STRING;
		assertEquals("foo", serialize(ps, "foo"));
	}

	@Test void c02_stringType_default() throws Exception {
		HttpPartSchema ps = tString()._default("x").build();
		assertEquals("foo", serialize(ps, "foo"));
		assertEquals("x", serialize(ps, null));
	}

	@Test void c03_stringType_byteFormat() throws Exception {
		HttpPartSchema ps = T_BYTE;
		byte[] foob = "foo".getBytes();
		String expected = base64Encode(foob);
		assertEquals(expected, serialize(ps, foob));
		assertEquals(expected, serialize(ps, new C1(foob)));
		assertEquals("null", serialize(ps, new C1(null)));
		assertEquals("null", serialize(ps, null));
	}

	@Test void c04_stringType_binaryFormat() throws Exception {
		HttpPartSchema ps = T_BINARY;
		byte[] foob = "foo".getBytes();
		String expected = toHex(foob);
		assertEquals(expected, serialize(ps, foob));
		assertEquals(expected, serialize(ps, new C1(foob)));
		assertEquals("null", serialize(ps, new C1(null)));
		assertEquals("null", serialize(ps, null));
	}

	@Test void c05_stringType_binarySpacedFormat() throws Exception {
		HttpPartSchema ps = T_BINARY_SPACED;
		byte[] foob = "foo".getBytes();
		String expected = toSpacedHex(foob);
		assertEquals(expected, serialize(ps, foob));
		assertEquals(expected, serialize(ps, new C1(foob)));
		assertEquals("null", serialize(ps, new C1(null)));
		assertEquals("null", serialize(ps, null));
	}

	@Test void c06_stringType_dateFormat() throws Exception {
		HttpPartSchema ps = T_DATE;
		Calendar in = StringUtils.parseIsoCalendar("2012-12-21");
		assertTrue(serialize(ps, in).contains("2012"));
		assertEquals("null", serialize(ps, null));
	}

	@Test void c07_stringType_dateTimeFormat() throws Exception {
		HttpPartSchema ps = T_DATETIME;
		Calendar in = StringUtils.parseIsoCalendar("2012-12-21T12:34:56.789");
		assertTrue(serialize(ps, in).contains("2012"));
		assertEquals("null", serialize(ps, null));
	}

	@Test void c08_stringType_uonFormat() throws Exception {
		HttpPartSchema ps = T_UON;
		assertEquals("foo", serialize(ps, "foo"));
		assertEquals("~'foo~'", serialize(ps, "'foo'"));
		assertEquals("foo", serialize(ps, new C2("foo")));
		assertEquals("null", serialize(ps, new C2(null)));
		assertEquals("'null'", serialize(ps, new C2("null")));
		assertEquals("null", serialize(ps, null));
		// UonPartSerializerTest should handle all other cases.
	}

	@Test void c09_stringType_noneFormat() throws Exception {
		// If no format is specified, then we should transform directly from a string.
		HttpPartSchema ps = T_STRING;
		assertEquals("foo", serialize(ps, "foo"));
		assertEquals("'foo'", serialize(ps, "'foo'"));
		assertEquals("foo", serialize(ps, new C2("foo")));
		assertEquals("null", serialize(ps, new C2(null)));
		assertEquals("null", serialize(ps, new C2("null")));
		assertEquals("null", serialize(ps, null));
	}

	@Test void c10_stringType_noneFormat_2d() throws Exception {
		HttpPartSchema ps = tArray(tString()).build();
		assertEquals("foo,bar,null", serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo,bar,null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo,bar,null", serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo,bar,null", serialize(ps, list((Object)"foo",(Object)"bar",null)));
		assertEquals("foo,bar,null,null", serialize(ps, new C2[]{new C2("foo"),new C2("bar"),new C2(null),null}));
		assertEquals("foo,bar,null,null", serialize(ps, list(new C2("foo"),new C2("bar"),new C2(null),null)));
		assertEquals("foo,bar,null", serialize(ps, new C3("foo","bar",null)));
	}

	@Test void c11_stringType_noneFormat_3d() throws Exception {
		HttpPartSchema ps = tArrayPipes(tArray(tString())).build();
		assertEquals("foo,bar|baz,null|null", serialize(ps, new String[][]{{"foo","bar"},{"baz",null},null}));
		assertEquals("foo,bar|baz,null|null", serialize(ps, list(new String[]{"foo","bar"}, new String[]{"baz",null},null)));
		assertEquals("foo,bar|baz,null|null", serialize(ps, list(list("foo","bar"),list("baz",null),null)));
		assertEquals("foo,bar|baz,null|null", serialize(ps, new Object[][]{{"foo","bar"},{"baz",null},null}));
		assertEquals("foo,bar|baz,null|null", serialize(ps, list(new Object[]{"foo","bar"}, new String[]{"baz",null},null)));
		assertEquals("foo,bar|baz,null|null", serialize(ps, list(list((Object)"foo",(Object)"bar"),list((Object)"baz",null),null)));
		assertEquals("foo,bar|baz,null,null|null", serialize(ps, new C2[][]{{new C2("foo"),new C2("bar")},{new C2("baz"),new C2(null),null},null}));
		assertEquals("foo,bar|baz,null,null|null", serialize(ps, list(new C2[]{new C2("foo"),new C2("bar")}, new C2[]{new C2("baz"),new C2(null),null},null)));
		assertEquals("foo,bar|baz,null,null|null", serialize(ps, list(list(new C2("foo"),new C2("bar")),list(new C2("baz"),new C2(null),null),null)));
		assertEquals("foo,bar|baz,null|null|null", serialize(ps, new C3[]{new C3("foo","bar"),new C3("baz",null),new C3((String)null),null}));
		assertEquals("foo,bar|baz,null|null|null", serialize(ps, list(new C3("foo","bar"),new C3("baz",null),new C3((String)null),null)));
	}

	@Test void c12_stringType_uonKeywords_plain() throws Exception {
		HttpPartSchema ps = T_STRING;
		// When serialized normally, the following should not be quoted.
		assertEquals("true", serialize(ps, "true"));
		assertEquals("false", serialize(ps, "false"));
		assertEquals("null", serialize(ps, "null"));
		assertEquals("null", serialize(ps, null));
		assertEquals("123", serialize(ps, "123"));
		assertEquals("1.23", serialize(ps, "1.23"));
	}

	@Test void c13_stringType_uonKeywords_uon() throws Exception {
		HttpPartSchema ps = T_UON;
		// When serialized as UON, the following should be quoted so that they're not confused with booleans or numbers.
		assertEquals("'true'", serialize(ps, "true"));
		assertEquals("'false'", serialize(ps, "false"));
		assertEquals("'null'", serialize(ps, "null"));
		assertEquals("null", serialize(ps, null));
		assertEquals("'123'", serialize(ps, "123"));
		assertEquals("'1.23'", serialize(ps, "1.23"));
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

	@Test void d01_arrayType_collectionFormatCsv() throws Exception {
		HttpPartSchema ps = T_ARRAY_CSV;
		assertEquals("foo,bar,null", serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo,bar,null", serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo,bar,null,null", serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo,bar,null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo,bar,null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo,bar,null,null", serialize(ps, list(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo,bar,null", serialize(ps, JsonList.of("foo","bar",null)));

		assertEquals("foo\\,bar,null", serialize(ps, new String[]{"foo,bar",null}));
	}

	@Test void d02_arrayType_collectionFormatPipes() throws Exception {
		HttpPartSchema ps = T_ARRAY_PIPES;
		assertEquals("foo|bar|null", serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo|bar|null", serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo|bar|null|null", serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo|bar|null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo|bar|null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo|bar|null|null", serialize(ps, list(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo|bar|null", serialize(ps, JsonList.of("foo","bar",null)));
	}

	@Test void d03_arrayType_collectionFormatSsv() throws Exception {
		HttpPartSchema ps = T_ARRAY_SSV;
		assertEquals("foo bar null", serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo bar null", serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo bar null null", serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo bar null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo bar null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo bar null null", serialize(ps, list(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo bar null", serialize(ps, JsonList.of("foo","bar",null)));
	}

	@Test void d04_arrayType_collectionFormatTsv() throws Exception {
		HttpPartSchema ps = T_ARRAY_TSV;
		assertEquals("foo\tbar\tnull", serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo\tbar\tnull", serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo\tbar\tnull\tnull", serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo\tbar\tnull", serialize(ps, list("foo","bar",null)));
		assertEquals("foo\tbar\tnull", serialize(ps, list("foo","bar",null)));
		assertEquals("foo\tbar\tnull\tnull", serialize(ps, list(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo\tbar\tnull", serialize(ps, JsonList.of("foo","bar",null)));
	}

	@Test void d05_arrayType_collectionFormatUon() throws Exception {
		HttpPartSchema ps = T_ARRAY_UON;
		assertEquals("@(foo,bar,'null',null)", serialize(ps, new String[]{"foo","bar","null",null}));
		assertEquals("@(foo,bar,'null',null)", serialize(ps, new Object[]{"foo","bar","null",null}));
		assertEquals("@(foo,bar,'null',null)", serialize(ps, new D[]{new D("foo"),new D("bar"),new D("null"),null}));
		assertEquals("@(foo,bar,'null',null)", serialize(ps, list("foo","bar","null",null)));
		assertEquals("@(foo,bar,'null',null)", serialize(ps, list("foo","bar","null",null)));
		assertEquals("@(foo,bar,'null',null)", serialize(ps, list(new D("foo"),new D("bar"),new D("null"),null)));
		assertEquals("@(foo,bar,'null',null)", serialize(ps, JsonList.of("foo","bar","null",null)));
	}

	@Test void d06a_arrayType_collectionFormatNone() throws Exception {
		HttpPartSchema ps = T_ARRAY;
		assertEquals("foo,bar,null", serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo,bar,null", serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo,bar,null,null", serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo,bar,null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo,bar,null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo,bar,null,null", serialize(ps, list(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo,bar,null", serialize(ps, JsonList.of("foo","bar",null)));
	}

	@Test void d07_arrayType_collectionFormatMulti() throws Exception {
		// collectionFormat=multi really shouldn't be applicable to collections of values, so just use csv.
		HttpPartSchema ps = T_ARRAY_MULTI;
		assertEquals("foo,bar,null", serialize(ps, new String[]{"foo","bar",null}));
		assertEquals("foo,bar,null", serialize(ps, new Object[]{"foo","bar",null}));
		assertEquals("foo,bar,null,null", serialize(ps, new D[]{new D("foo"),new D("bar"),new D(null),null}));
		assertEquals("foo,bar,null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo,bar,null", serialize(ps, list("foo","bar",null)));
		assertEquals("foo,bar,null,null", serialize(ps, list(new D("foo"),new D("bar"),new D(null),null)));
		assertEquals("foo,bar,null", serialize(ps, JsonList.of("foo","bar",null)));
	}

	@Test void d08_arrayType_collectionFormatCsvAndPipes() throws Exception {
		HttpPartSchema ps = tArrayPipes(tArrayCsv()).build();
		assertEquals("foo,bar|baz,null|null", serialize(ps, new String[][]{{"foo","bar"},{"baz",null},null}));
		assertEquals("foo,bar|baz,null|null", serialize(ps, new Object[][]{{"foo","bar"},{"baz",null},null}));
		assertEquals("foo,bar|baz,null,null|null", serialize(ps, new D[][]{{new D("foo"),new D("bar")},{new D("baz"),new D(null),null},null}));
		assertEquals("foo,bar|baz,null|null", serialize(ps, list(list("foo","bar"),list("baz",null),null)));
		assertEquals("foo,bar|baz,null|null", serialize(ps, list(list("foo","bar"),list("baz",null),null)));
		assertEquals("foo,bar|baz,null,null|null", serialize(ps, list(list(new D("foo"),new D("bar")),list(new D("baz"),new D(null),null),null)));
	}

	@Test void d09_arrayType_itemsInteger() throws Exception {
		HttpPartSchema ps = tArrayCsv(tInteger()).build();
		assertEquals("1,2", serialize(ps, new int[]{1,2}));
		assertEquals("1,2,null", serialize(ps, new Integer[]{1,2,null}));
		assertEquals("1,2,null", serialize(ps, new Object[]{1,2,null}));
		assertEquals("1,2,null", serialize(ps, list(1,2,null)));
	}

	@Test void d10_arrayType_itemsInteger_2d() throws Exception {
		HttpPartSchema ps = tArrayPipes(tArrayCsv(tInteger()).allowEmptyValue()).build();
		assertEquals("1,2||null", serialize(ps, new int[][]{{1,2},{},null}));
		assertEquals("1,2,null||null", serialize(ps, new Integer[][]{{1,2,null},{},null}));
		assertEquals("1,2,null||null", serialize(ps, new Object[][]{{1,2,null},{},null}));
		assertEquals("1,2,null||null", serialize(ps, list(list(1,2,null),list(),null)));
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

	@Test void e01_booleanType() throws Exception {
		HttpPartSchema ps = T_BOOLEAN;
		assertEquals("true", serialize(ps, true));
		assertEquals("true", serialize(ps, "true"));
		assertEquals("true", serialize(ps, new E1(true)));
		assertEquals("false", serialize(ps, false));
		assertEquals("false", serialize(ps, "false"));
		assertEquals("false", serialize(ps, new E1(false)));
		assertEquals("null", serialize(ps, null));
		assertEquals("null", serialize(ps, "null"));
		assertEquals("null", serialize(ps, new E1(null)));
	}

	@Test void e03_booleanType_2d() throws Exception {
		HttpPartSchema ps = tArray(tBoolean()).build();
		assertEquals("true", serialize(ps, new boolean[]{true}));
		assertEquals("true,null", serialize(ps, new Boolean[]{true,null}));
		assertEquals("true,null", serialize(ps, list(true,null)));
		assertEquals("true,null", serialize(ps, new String[]{"true",null}));
		assertEquals("true,null", serialize(ps, list("true",null)));
		assertEquals("true,null", serialize(ps, new Object[]{true,null}));
		assertEquals("true,null", serialize(ps, list(true,null)));
		assertEquals("true,null,null", serialize(ps, new E1[]{new E1(true),new E1(null),null}));
		assertEquals("true,null,null", serialize(ps, list(new E1(true),new E1(null),null)));
		assertEquals("true,null", serialize(ps, new E2(true,null)));
	}

	@Test void e04_booleanType_3d() throws Exception {
		HttpPartSchema ps = tArrayPipes(tArray(tBoolean())).build();
		assertEquals("true,true|false", serialize(ps, new boolean[][]{{true,true},{false}}));
		assertEquals("true,true|false", serialize(ps, list(new boolean[]{true,true},new boolean[]{false})));
		assertEquals("true,true|false,null", serialize(ps, new Boolean[][]{{true,true},{false,null}}));
		assertEquals("true,true|false,null", serialize(ps, list(new Boolean[]{true,true},new Boolean[]{false,null})));
		assertEquals("true,true|false,null", serialize(ps, list(list(true,true),list(false,null))));
		assertEquals("true,true|false,null,null", serialize(ps, list(list("true","true"),list("false","null",null))));
		assertEquals("true,true|false,null,null", serialize(ps, list(new String[]{"true","true"},new String[]{"false","null",null})));
		assertEquals("true,true|false,null", serialize(ps, new Object[][]{{true,true},{false,null}}));
		assertEquals("true,true|false,null", serialize(ps, list(list((Object)true,(Object)true),list((Object)false,null))));
		assertEquals("true,true|false,null", serialize(ps, list(new Object[]{true,true},new Object[]{false,null})));
		assertEquals("true,true|false,null", serialize(ps, new E1[][]{{new E1(true),new E1(true)},{new E1(false),new E1(null)}}));
		assertEquals("true,true|false,null", serialize(ps, list(list(new E1(true),new E1(true)), list(new E1(false),new E1(null)))));
		assertEquals("true,true|false,null", serialize(ps, list(new E1[]{new E1(true),new E1(true)},new E1[]{new E1(false),new E1(null)})));
		assertEquals("true,true|false,null", serialize(ps, new E2[]{new E2(true,true),new E2(false,null)}));
		assertEquals("true,true|false,null", serialize(ps, list(new E2(true,true),new E2(false,null))));
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

	@Test void f01_integerType_int32() throws Exception {
		HttpPartSchema ps = T_INT32;
		assertEquals("1", serialize(ps, 1));
		assertEquals("1", serialize(ps, Integer.valueOf(1)));
		assertEquals("1", serialize(ps, (short)1));
		assertEquals("1", serialize(ps, Short.valueOf((short)1)));
		assertEquals("1", serialize(ps, 1L));
		assertEquals("1", serialize(ps, Long.valueOf(1)));
		assertEquals("1", serialize(ps, "1"));
		assertEquals("1", serialize(ps, new F1(1)));
		assertEquals("null", serialize(ps, null));
		assertEquals("null", serialize(ps, "null"));
	}

	@Test void f02_integerType_int32_2d() throws Exception {
		HttpPartSchema ps = tArray(tInt32()).build();
		assertEquals("1,2", serialize(ps, new int[]{1,2}));
		assertEquals("1,2,null", serialize(ps, new Integer[]{1,2,null}));
		assertEquals("1,2,null", serialize(ps, list(1,2,null)));
		assertEquals("1,2", serialize(ps, new short[]{1,2}));
		assertEquals("1,2,null", serialize(ps, new Short[]{1,2,null}));
		assertEquals("1,2,null", serialize(ps, list(Short.valueOf((short)1),Short.valueOf((short)2),null)));
		assertEquals("1,2", serialize(ps, new long[]{1L,2L}));
		assertEquals("1,2,null", serialize(ps, new Long[]{1L,2L,null}));
		assertEquals("1,2,null", serialize(ps, list(1L,2L,null)));
		assertEquals("1,2,null,null", serialize(ps, new String[]{"1","2","null",null}));
		assertEquals("1,2,null,null", serialize(ps, list("1","2","null",null)));
		assertEquals("1,2,null", serialize(ps, new Object[]{1,2,null}));
		assertEquals("1,2,null", serialize(ps, list(1,2,null)));
		assertEquals("1,2,null,null", serialize(ps, new F1[]{new F1(1),new F1(2),new F1(null),null}));
		assertEquals("1,2,null,null", serialize(ps, list(new F1(1),new F1(2),new F1(null),null)));
		assertEquals("1,2,null", serialize(ps, new F2(1,2,null)));
	}

	@Test void f03_integerType_int32_3d() throws Exception {
		HttpPartSchema ps = tArrayPipes(tArray(tInt32())).build();
		assertEquals("1,2|3|null", serialize(ps, new int[][]{{1,2},{3},null}));
		assertEquals("1,2|3|null", serialize(ps, list(new int[]{1,2},new int[]{3},null)));
		assertEquals("1,2|3,null|null", serialize(ps, new Integer[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new Integer[]{1,2},new Integer[]{3,null},null)));
		assertEquals("1,2|3,null|null", serialize(ps, list(list(1,2),list(3,null),null)));
		assertEquals("1,2|3|null", serialize(ps, new short[][]{{1,2},{3},null}));
		assertEquals("1,2|3|null", serialize(ps, list(new short[]{1,2},new short[]{3},null)));
		assertEquals("1,2|3,null|null", serialize(ps, new Short[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new Short[]{1,2},new Short[]{3,null},null)));
		assertEquals("1,2|3,null|null", serialize(ps, list(list(Short.valueOf((short)1),Short.valueOf((short)2)),list(Short.valueOf((short)3),null),null)));
		assertEquals("1,2|3|null", serialize(ps, new long[][]{{1L,2L},{3L},null}));
		assertEquals("1,2|3|null", serialize(ps, list(new long[]{1L,2L},new long[]{3L},null)));
		assertEquals("1,2|3,null|null", serialize(ps, new Long[][]{{1L,2L},{3L,null},null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new Long[]{1L,2L},new Long[]{3L,null},null)));
		assertEquals("1,2|3,null|null", serialize(ps, list(list(Long.valueOf(1),Long.valueOf(2)),list(Long.valueOf(3),null),null)));
		assertEquals("1,2|3,null,null|null", serialize(ps, new String[][]{{"1","2"},{"3","null",null},null}));
		assertEquals("1,2|3,null,null|null", serialize(ps, list(new String[]{"1","2"},new String[]{"3","null",null},null)));
		assertEquals("1,2|3,null,null|null", serialize(ps, list(list("1","2"),list("3","null",null),null)));
		assertEquals("1,2|3,null|null", serialize(ps, new Object[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new Object[]{1,2},new Object[]{3,null},null)));
		assertEquals("1,2|3,null|null", serialize(ps, list(list(1,2),list(3,null),null)));
		assertEquals("1,2|3,null,null|null", serialize(ps, new F1[][]{{new F1(1),new F1(2)},{new F1(3),new F1(null),null},null}));
		assertEquals("1,2|3,null,null|null", serialize(ps, list(new F1[]{new F1(1),new F1(2)},new F1[]{new F1(3),new F1(null),null},null)));
		assertEquals("1,2|3,null,null|null", serialize(ps, list(list(new F1(1),new F1(2)),list(new F1(3),new F1(null),null),null)));
		assertEquals("1,2|3,null|null", serialize(ps, new F2[]{new F2(1,2),new F2(3,null),null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new F2(1,2),new F2(3,null),null)));
	}

	@Test void f04_integerType_int64() throws Exception {
		HttpPartSchema ps = T_INT64;
		assertEquals("1", serialize(ps, 1));
		assertEquals("1", serialize(ps, Integer.valueOf(1)));
		assertEquals("1", serialize(ps, (short)1));
		assertEquals("1", serialize(ps, Short.valueOf((short)1)));
		assertEquals("1", serialize(ps, 1L));
		assertEquals("1", serialize(ps, Long.valueOf(1L)));
		assertEquals("1", serialize(ps, "1"));
		assertEquals("1", serialize(ps,  new F3(1L)));
		assertEquals("null", serialize(ps, null));
		assertEquals("null", serialize(ps, "null"));
	}

	@Test void f05_integerType_int64_2d() throws Exception {
		HttpPartSchema ps = tArray(tInt64()).build();
		assertEquals("1,2", serialize(ps, new int[]{1,2}));
		assertEquals("1,2,null", serialize(ps, new Integer[]{1,2,null}));
		assertEquals("1,2,null", serialize(ps, list(1,2,null)));
		assertEquals("1,2", serialize(ps, new short[]{1,2}));
		assertEquals("1,2,null", serialize(ps, new Short[]{1,2,null}));
		assertEquals("1,2,null", serialize(ps, list((short)1,(short)2,null)));
		assertEquals("1,2", serialize(ps, new long[]{1L,2L}));
		assertEquals("1,2,null", serialize(ps, new Long[]{1L,2L,null}));
		assertEquals("1,2,null", serialize(ps, list(1L,2L,null)));
		assertEquals("1,2,null,null", serialize(ps, new String[]{"1","2","null",null}));
		assertEquals("1,2,null,null", serialize(ps, list("1","2","null",null)));
		assertEquals("1,2,null", serialize(ps, new Object[]{1,2,null}));
		assertEquals("1,2,null", serialize(ps, list((Object)1,(Object)2,null)));
		assertEquals("1,2,null,null", serialize(ps, new F3[]{new F3(1L),new F3(2L),new F3(null),null}));
		assertEquals("1,2,null,null", serialize(ps, list(new F3(1L),new F3(2L),new F3(null),null)));
		assertEquals("1,2,null", serialize(ps, new F4(1L,2L,null)));
	}

	@Test void f06_integerType_int64_3d() throws Exception {
		HttpPartSchema ps = tArrayPipes(tArray(tInt64())).build();
		assertEquals("1,2|3|null", serialize(ps, new int[][]{{1,2},{3},null}));
		assertEquals("1,2|3|null", serialize(ps, list(new int[]{1,2},new int[]{3},null)));
		assertEquals("1,2|3,null|null", serialize(ps, new Integer[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new Integer[]{1,2},new Integer[]{3,null},null)));
		assertEquals("1,2|3,null|null", serialize(ps, list(list(1,2),list(3,null),null)));
		assertEquals("1,2|3|null", serialize(ps, new short[][]{{1,2},{3},null}));
		assertEquals("1,2|3|null", serialize(ps, list(new short[]{1,2},new short[]{3},null)));
		assertEquals("1,2|3,null|null", serialize(ps, new Short[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new Short[]{1,2},new Short[]{3,null},null)));
		assertEquals("1,2|3,null|null", serialize(ps, list(list((short)1,(short)2),list((short)3,null),null)));
		assertEquals("1,2|3|null", serialize(ps, new long[][]{{1L,2L},{3L},null}));
		assertEquals("1,2|3|null", serialize(ps, list(new long[]{1L,2L},new long[]{3L},null)));
		assertEquals("1,2|3,null|null", serialize(ps, new Long[][]{{1L,2L},{3L,null},null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new Long[]{1L,2L},new Long[]{3L,null},null)));
		assertEquals("1,2|3,null|null", serialize(ps, list(list(1L,2L),list(3L,null),null)));
		assertEquals("1,2|3,null,null|null", serialize(ps, new String[][]{{"1","2"},{"3","null",null},null}));
		assertEquals("1,2|3,null,null|null", serialize(ps, list(new String[]{"1","2"},new String[]{"3","null",null},null)));
		assertEquals("1,2|3,null,null|null", serialize(ps, list(list("1","2"),list("3","null",null),null)));
		assertEquals("1,2|3,null|null", serialize(ps, new Object[][]{{1,2},{3,null},null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new Object[]{1,2},new Object[]{3,null},null)));
		assertEquals("1,2|3,null|null", serialize(ps, list(list((Object)1,(Object)2),list((Object)3,null),null)));
		assertEquals("1,2|3,null,null|null", serialize(ps, new F3[][]{{new F3(1L),new F3(2L)},{new F3(3L),new F3(null),null},null}));
		assertEquals("1,2|3,null,null|null", serialize(ps, list(new F3[]{new F3(1L),new F3(2L)},new F3[]{new F3(3L),new F3(null),null},null)));
		assertEquals("1,2|3,null,null|null", serialize(ps, list(list(new F3(1L),new F3(2L)),list(new F3(3L),new F3(null),null),null)));
		assertEquals("1,2|3,null|null", serialize(ps, new F4[]{new F4(1L,2L),new F4(3L,null),null}));
		assertEquals("1,2|3,null|null", serialize(ps, list(new F4(1L,2L),new F4(3L,null),null)));
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

	@Test void g01_numberType_float() throws Exception {
		HttpPartSchema ps = T_FLOAT;
		assertEquals("1.0", serialize(ps, 1f));
		assertEquals("1.0", serialize(ps, Float.valueOf(1f)));
		assertEquals("1.0", serialize(ps, 1d));
		assertEquals("1.0", serialize(ps, Double.valueOf(1d)));
		assertEquals("1.0", serialize(ps, "1"));
		assertEquals("1.0", serialize(ps, new G1(1f)));
		assertEquals("null", serialize(ps, null));
		assertEquals("null", serialize(ps, "null"));
		assertEquals("null", serialize(ps, new G1(null)));
	}

	@Test void g02_numberType_float_2d() throws Exception {
		HttpPartSchema ps = tArray(tFloat()).build();
		assertEquals("1.0,2.0", serialize(ps, new float[]{1,2}));
		assertEquals("1.0,2.0,null", serialize(ps, new Float[]{1f,2f,null}));
		assertEquals("1.0,2.0,null", serialize(ps, list(1f,2f,null)));
		assertEquals("1.0,2.0", serialize(ps, new double[]{1,2}));
		assertEquals("1.0,2.0,null", serialize(ps, new Double[]{1d,2d,null}));
		assertEquals("1.0,2.0,null", serialize(ps, list(1d,2d,null)));
		assertEquals("1.0,2.0,null,null", serialize(ps, new String[]{"1","2","null",null}));
		assertEquals("1.0,2.0,null,null", serialize(ps, list("1","2","null",null)));
		assertEquals("1.0,2.0,null", serialize(ps, new Object[]{1,2,null}));
		assertEquals("1.0,2.0,null", serialize(ps, list((Object)1,(Object)2,null)));
		assertEquals("1.0,2.0,null,null", serialize(ps, new G1[]{new G1(1f),new G1(2f),new G1(null),null}));
		assertEquals("1.0,2.0,null,null", serialize(ps, list(new G1(1f),new G1(2f),new G1(null),null)));
		assertEquals("1.0,2.0,null", serialize(ps, new G2(1f,2f,null)));
	}

	@Test void g03_numberType_float_3d() throws Exception {
		HttpPartSchema ps = tArrayPipes(tArray(tFloat())).build();
		assertEquals("1.0,2.0|3.0|null", serialize(ps, new float[][]{{1,2},{3},null}));
		assertEquals("1.0,2.0|3.0|null", serialize(ps, list(new float[]{1,2},new float[]{3},null)));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, new Float[][]{{1f,2f},{3f,null},null}));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, list(new Float[]{1f,2f},new Float[]{3f,null}, null)));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, list(list(1f,2f),list(3f,null),null)));
		assertEquals("1.0,2.0|3.0|null", serialize(ps, new double[][]{{1d,2d},{3d},null}));
		assertEquals("1.0,2.0|3.0|null", serialize(ps, list(new double[]{1d,2d},new double[]{3d},null)));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, new Double[][]{{1d,2d},{3d,null},null}));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, list(new Double[]{1d,2d},new Double[]{3d,null},null)));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, list(list(1d,2d),list(3d,null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, new String[][]{{"1","2"},{"3","null",null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(new String[]{"1","2"},new String[]{"3","null",null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(list(1d,2d),list(3f,"null",null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, new Object[][]{{1d,2d},{3f,"null",null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(new Object[]{1d,2d},new Object[]{3f,"null",null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(list(1d,2d),list(3f,"null",null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, new G1[][]{{new G1(1f),new G1(2f)},{new G1(3f),new G1(null),null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(new G1[]{new G1(1f),new G1(2f)},new G1[]{new G1(3f),new G1(null),null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(list(new G1(1f),new G1(2f)),list(new G1(3f),new G1(null),null),null)));
		assertEquals("1.0,2.0,null|null", serialize(ps, new G2[]{new G2(1f,2f,null),null}));
		assertEquals("1.0,2.0,null|null", serialize(ps, list(new G2(1f,2f,null),null)));
	}

	@Test void g04_numberType_double() throws Exception {
		HttpPartSchema ps = T_DOUBLE;
		assertEquals("1.0", serialize(ps, 1f));
		assertEquals("1.0", serialize(ps, Float.valueOf(1f)));
		assertEquals("1.0", serialize(ps, 1d));
		assertEquals("1.0", serialize(ps, Double.valueOf(1d)));
		assertEquals("1.0", serialize(ps, "1"));
		assertEquals("1.0", serialize(ps, new G3(1d)));
		assertEquals("null", serialize(ps, null));
		assertEquals("null", serialize(ps, "null"));
		assertEquals("null", serialize(ps, new G3(null)));
	}

	@Test void g05_numberType_double_2d() throws Exception {
		HttpPartSchema ps = tArray(tDouble()).build();
		assertEquals("1.0,2.0", serialize(ps, new float[]{1,2}));
		assertEquals("1.0,2.0,null", serialize(ps, new Float[]{1f,2f,null}));
		assertEquals("1.0,2.0,null", serialize(ps, list(1f,2f,null)));
		assertEquals("1.0,2.0", serialize(ps, new double[]{1,2}));
		assertEquals("1.0,2.0,null", serialize(ps, new Double[]{1d,2d,null}));
		assertEquals("1.0,2.0,null", serialize(ps, list(1d,2d,null)));
		assertEquals("1.0,2.0,null,null", serialize(ps, new String[]{"1","2","null",null}));
		assertEquals("1.0,2.0,null,null", serialize(ps, list("1","2","null",null)));
		assertEquals("1.0,2.0,null,null", serialize(ps, new Object[]{1d,2f,"null",null}));
		assertEquals("1.0,2.0,null,null", serialize(ps, list(1d,2f,"null",null)));
		assertEquals("1.0,2.0,null,null", serialize(ps, new G3[]{new G3(1d),new G3(2d),new G3(null),null}));
		assertEquals("1.0,2.0,null,null", serialize(ps, list(new G3(1d),new G3(2d),new G3(null),null)));
		assertEquals("1.0,2.0,null", serialize(ps, new G4(1d,2d,null)));
	}

	@Test void g06_numberType_double_3d() throws Exception {
		HttpPartSchema ps = tArrayPipes(tArray(tDouble())).build();
		assertEquals("1.0,2.0|3.0|null", serialize(ps, new float[][]{{1f,2f},{3f},null}));
		assertEquals("1.0,2.0|3.0|null", serialize(ps, list(new float[]{1f,2f},new float[]{3f},null)));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, new Float[][]{{1f,2f},{3f,null},null}));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, list(new Float[]{1f,2f},new Float[]{3f,null},null)));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, list(list(1f,2f),list(3f,null),null)));
		assertEquals("1.0,2.0|3.0|null", serialize(ps, new double[][]{{1d,2d},{3d},null}));
		assertEquals("1.0,2.0|3.0|null", serialize(ps, list(new double[]{1d,2d},new double[]{3d},null)));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, new Double[][]{{1d,2d},{3d,null},null}));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, list(new Double[]{1d,2d},new Double[]{3d,null},null)));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, list(list(1d,2d),list(3d,null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, new String[][]{{"1","2"},{"3","null",null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(new String[]{"1","2"},new String[]{"3","null",null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(list("1","2"),list("3","null",null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, new Object[][]{{1d,2d},{"3","null",null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(new Object[]{1d,2d},new Object[]{"3","null",null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(list(1d,2f),list(3d,"null",null),null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, new G3[][]{{new G3(1d),new G3(2d)},{new G3(3d),new G3(null),null},null}));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(new G3[]{new G3(1d),new G3(2d)},new G3[]{new G3(3d),new G3(null),null},null)));
		assertEquals("1.0,2.0|3.0,null,null|null", serialize(ps, list(list(new G3(1d),new G3(2d)),list(new G3(3d),new G3(null),null),null)));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, new G4[]{new G4(1d,2d),new G4(3d,null),null}));
		assertEquals("1.0,2.0|3.0,null|null", serialize(ps, list(new G4(1d,2d),new G4(3d,null),null)));
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

	@Test void h01_objectType() throws Exception {
		HttpPartSchema ps = tObject().allowEmptyValue().build();
		assertEquals("f1=1,f2=2,f3=true", serialize(ps, new H1("1",2,true)));
		assertEquals("", serialize(ps, new H1(null,null,null)));
		assertEquals("f1=1,f2=2,f3=true", serialize(ps, JsonMap.ofJson("{f1:'1',f2:2,f3:true}")));
		assertEquals("f1=null,f2=null,f3=null", serialize(ps, JsonMap.ofJson("{f1:null,f2:null,f3:null}")));
		assertEquals("null", serialize(ps, null));
	}

	@Test void h02_objectType_uon() throws Exception {
		HttpPartSchema ps = T_OBJECT_UON;
		assertEquals("(f1='1',f2=2,f3=true)", serialize(ps, new H1("1",2,true)));
		assertEquals("()", serialize(ps, new H1(null,null,null)));
		assertEquals("(f1='1',f2=2,f3=true)", serialize(ps, JsonMap.ofJson("{f1:'1',f2:2,f3:true}")));
		assertEquals("(f1=null,f2=null,f3=null)", serialize(ps, JsonMap.ofJson("{f1:null,f2:null,f3:null}")));
		assertEquals("null", serialize(ps, null));
	}

	@Test void h03_objectType_2d() throws Exception {
		HttpPartSchema ps = tArray(tObject().allowEmptyValue()).build();
		assertEquals("f1=1\\,f2=2\\,f3=true,,null", serialize(ps, new H1[]{new H1("1",2,true),new H1(null,null,null),null}));
		assertEquals("f1=1\\,f2=2\\,f3=true,,null", serialize(ps, list(new H1("1",2,true),new H1(null,null,null),null)));
		assertEquals("f1=1\\,f2=2\\,f3=true,f1=null\\,f2=null\\,f3=null,null", serialize(ps, new JsonMap[]{JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null}));
		assertEquals("f1=1\\,f2=2\\,f3=true,f1=null\\,f2=null\\,f3=null,null", serialize(ps, list(JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null)));
		assertEquals("f1=1\\,f2=2\\,f3=true,f1=1\\,f2=2\\,f3=true,null", serialize(ps, new Object[]{new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),null}));
		assertEquals("f1=1\\,f2=2\\,f3=true,f1=1\\,f2=2\\,f3=true,null", serialize(ps, list(new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),null)));
	}

	@Test void h03_objectType_2d_pipes() throws Exception {
		HttpPartSchema ps = tArrayPipes(tObject().allowEmptyValue()).build();
		assertEquals("f1=1,f2=2,f3=true||null", serialize(ps, new H1[]{new H1("1",2,true),new H1(null,null,null),null}));
		assertEquals("f1=1,f2=2,f3=true||null", serialize(ps, list(new H1("1",2,true),new H1(null,null,null),null)));
		assertEquals("f1=1,f2=2,f3=true|f1=null,f2=null,f3=null|null", serialize(ps, new JsonMap[]{JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null}));
		assertEquals("f1=1,f2=2,f3=true|f1=null,f2=null,f3=null|null", serialize(ps, list(JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null)));
		assertEquals("f1=1,f2=2,f3=true|f1=1,f2=2,f3=true|null", serialize(ps, new Object[]{new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),null}));
		assertEquals("f1=1,f2=2,f3=true|f1=1,f2=2,f3=true|null", serialize(ps, list(new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),null)));
	}

	@Test void h04_objectType_2d_uon() throws Exception {
		HttpPartSchema ps = tArrayUon(tObject()).build();
		assertEquals("@((f1='1',f2=2,f3=true),(),null)", serialize(ps, new H1[]{new H1("1",2,true),new H1(null,null,null),null}));
		assertEquals("@((f1='1',f2=2,f3=true),(),null)", serialize(ps, list(new H1("1",2,true),new H1(null,null,null),null)));
		assertEquals("@((f1='1',f2=2,f3=true),(f1=null,f2=null,f3=null),null)", serialize(ps, new JsonMap[]{JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null}));
		assertEquals("@((f1='1',f2=2,f3=true),(f1=null,f2=null,f3=null),null)", serialize(ps, list(JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null)));
		assertEquals("@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true),null)", serialize(ps, new Object[]{new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),null}));
		assertEquals("@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true),null)", serialize(ps, list(new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),null)));
	}

	@Test void h03_objectType_3d() throws Exception {
		HttpPartSchema ps = tArray(tArray(tObject().allowEmptyValue())).build();
		assertEquals("f1=1\\\\\\,f2=2\\\\\\,f3=true\\,f1=x\\\\\\,f2=3\\\\\\,f3=false,\\,null,null", serialize(ps, new H1[][]{{new H1("1",2,true),new H1("x",3,false)},{new H1(null,null,null),null},null}));
		assertEquals("f1=1\\\\\\,f2=2\\\\\\,f3=true\\,f1=x\\\\\\,f2=3\\\\\\,f3=false,\\,null,null", serialize(ps, list(new H1[]{new H1("1",2,true),new H1("x",3,false)},new H1[]{new H1(null,null,null),null},null)));
		assertEquals("f1=1\\\\\\,f2=2\\\\\\,f3=true\\,f1=x\\\\\\,f2=3\\\\\\,f3=false,\\,null,null", serialize(ps, list(list(new H1("1",2,true),new H1("x",3,false)),list(new H1(null,null,null),null),null)));
		assertEquals("f1=1\\\\\\,f2=2\\\\\\,f3=true\\,f1=x\\\\\\,f2=4\\\\\\,f3=false,f1=null\\\\\\,f2=null\\\\\\,f3=null\\,null,null", serialize(ps, new JsonMap[][]{{JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:'x',f2:4,f3:false}")},{JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("f1=1\\\\\\,f2=2\\\\\\,f3=true\\,f1=x\\\\\\,f2=4\\\\\\,f3=false,f1=null\\\\\\,f2=null\\\\\\,f3=null\\,null,null", serialize(ps, list(new JsonMap[]{JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:'x',f2:4,f3:false}")},new JsonMap[]{JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("f1=1\\\\\\,f2=2\\\\\\,f3=true\\,f1=x\\\\\\,f2=4\\\\\\,f3=false,f1=null\\\\\\,f2=null\\\\\\,f3=null\\,null,null", serialize(ps, list(list(JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:'x',f2:4,f3:false}")),list(JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null),null)));
		assertEquals("f1=1\\\\\\,f2=2\\\\\\,f3=true\\,f1=1\\\\\\,f2=2\\\\\\,f3=true,\\,f1=null\\\\\\,f2=null\\\\\\,f3=null\\,null,null", serialize(ps, new Object[][]{{new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}")},{new H1(null,null,null),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("f1=1\\\\\\,f2=2\\\\\\,f3=true\\,f1=1\\\\\\,f2=2\\\\\\,f3=true,\\,f1=null\\\\\\,f2=null\\\\\\,f3=null\\,null,null", serialize(ps, list(new Object[]{new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}")},new Object[]{new H1(null,null,null),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("f1=1\\\\\\,f2=2\\\\\\,f3=true\\,f1=1\\\\\\,f2=2\\\\\\,f3=true,\\,f1=null\\\\\\,f2=null\\\\\\,f3=null\\,null,null", serialize(ps, list(list(new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}")),list(new H1(null,null,null),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null),null)));
	}

	@Test void h03_objectType_3d_ssvAndPipes() throws Exception {
		HttpPartSchema ps = tArraySsv(tArrayPipes(tObject().allowEmptyValue())).build();
		assertEquals("null|null null|null null null", serialize(ps, new String[][]{{null,null},{null,null},null,null}));
//f1=1,f2=2,f3=true|f1=x,f2=3,f3=false null null
		assertEquals("f1=1,f2=2,f3=true|f1=x,f2=3,f3=false |null null", serialize(ps, new H1[][]{{new H1("1",2,true),new H1("x",3,false)},{new H1(null,null,null),null},null}));
		assertEquals("f1=1,f2=2,f3=true|f1=x,f2=3,f3=false |null null", serialize(ps, list(new H1[]{new H1("1",2,true),new H1("x",3,false)},new H1[]{new H1(null,null,null),null},null)));
		assertEquals("f1=1,f2=2,f3=true|f1=x,f2=3,f3=false |null null", serialize(ps, list(list(new H1("1",2,true),new H1("x",3,false)),list(new H1(null,null,null),null),null)));
		assertEquals("f1=1,f2=2,f3=true|f1=x,f2=4,f3=false f1=null,f2=null,f3=null|null null", serialize(ps, new JsonMap[][]{{JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:'x',f2:4,f3:false}")},{JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("f1=1,f2=2,f3=true|f1=x,f2=4,f3=false f1=null,f2=null,f3=null|null null", serialize(ps, list(new JsonMap[]{JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:'x',f2:4,f3:false}")},new JsonMap[]{JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("f1=1,f2=2,f3=true|f1=x,f2=4,f3=false f1=null,f2=null,f3=null|null null", serialize(ps, list(list(JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:'x',f2:4,f3:false}")),list(JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null),null)));
		assertEquals("f1=1,f2=2,f3=true|f1=1,f2=2,f3=true |f1=null,f2=null,f3=null|null null", serialize(ps, new Object[][]{{new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}")},{new H1(null,null,null),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("f1=1,f2=2,f3=true|f1=1,f2=2,f3=true |f1=null,f2=null,f3=null|null null", serialize(ps, list(new Object[]{new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}")},new Object[]{new H1(null,null,null),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("f1=1,f2=2,f3=true|f1=1,f2=2,f3=true |f1=null,f2=null,f3=null|null null", serialize(ps, list(list(new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}")),list(new H1(null,null,null),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null),null)));
	}

	@Test void h03_objectType_3d_uon() throws Exception {
		HttpPartSchema ps = tArrayUon(tArray(tObject())).build();
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=3,f3=false)),@((),null),null)", serialize(ps, new H1[][]{{new H1("1",2,true),new H1("x",3,false)},{new H1(null,null,null),null},null}));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=3,f3=false)),@((),null),null)", serialize(ps, list(new H1[]{new H1("1",2,true),new H1("x",3,false)},new H1[]{new H1(null,null,null),null},null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=3,f3=false)),@((),null),null)", serialize(ps, list(list(new H1("1",2,true),new H1("x",3,false)),list(new H1(null,null,null),null),null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=4,f3=false)),@((f1=null,f2=null,f3=null),null),null)", serialize(ps, new JsonMap[][]{{JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:'x',f2:4,f3:false}")},{JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=4,f3=false)),@((f1=null,f2=null,f3=null),null),null)", serialize(ps, list(new JsonMap[]{JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:'x',f2:4,f3:false}")},new JsonMap[]{JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1=x,f2=4,f3=false)),@((f1=null,f2=null,f3=null),null),null)", serialize(ps, list(list(JsonMap.ofJson("{f1:'1',f2:2,f3:true}"),JsonMap.ofJson("{f1:'x',f2:4,f3:false}")),list(JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null),null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true)),@((),(f1=null,f2=null,f3=null),null),null)", serialize(ps, new Object[][]{{new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}")},{new H1(null,null,null),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null}));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true)),@((),(f1=null,f2=null,f3=null),null),null)", serialize(ps, list(new Object[]{new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}")},new Object[]{new H1(null,null,null),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null},null)));
		assertEquals("@(@((f1='1',f2=2,f3=true),(f1='1',f2=2,f3=true)),@((),(f1=null,f2=null,f3=null),null),null)", serialize(ps, list(list(new H1("1",2,true),JsonMap.ofJson("{f1:'1',f2:2,f3:true}")),list(new H1(null,null,null),JsonMap.ofJson("{f1:null,f2:null,f3:null}"),null),null)));
	}

	public static class H2 {
		public Object f01, f02, f04, f05, f06, f07, f08, f09, f10, f11, f12, f99;
		public H2(Object f01, Object f02, Object f04, Object f05, Object f06, Object f07, Object f08, Object f09, Object f10, Object f11, Object f12, Object f99) {
			this.f01 = f01;
			this.f02 = f02;
			this.f04 = f04;
			this.f05 = f05;
			this.f06 = f06;
			this.f07 = f07;
			this.f08 = f08;
			this.f09 = f09;
			this.f10 = f10;
			this.f11 = f11;
			this.f12 = f12;
			this.f99 = f99;
		}
	}

	@Test void h04_objectType_simpleProperties() throws Exception {
		HttpPartSchema ps = tObject()
			.p("f01", tString())
			.p("f02", tByte())
			.p("f04", tDateTime())
			.p("f05", tBinary())
			.p("f06", tBinarySpaced())
			.p("f07", tUon())
			.p("f08", tInteger())
			.p("f09", tInt64())
			.p("f10", tNumber())
			.p("f11", tDouble())
			.p("f12", tBoolean())
			.ap(tInteger())
			.allowEmptyValue()
			.build();

		byte[] foob = "foo".getBytes();

		assertEquals(
			"f01=foo,f02=Zm9v,f04=2012-12-21T12:34:56Z,f05=666F6F,f06=66 6F 6F,f07=foo,f08=1,f09=2,f10=1.0,f11=1.0,f12=true,f99=1",
			serialize(ps, new H2("foo",foob,parseIsoCalendar("2012-12-21T12:34:56Z"),foob,foob,"foo",1,2,1.0,1.0,true,1))
		);
		assertEquals("", serialize(ps, new H2(null,null,null,null,null,null,null,null,null,null,null,null)));
		assertEquals("null", serialize(ps, null));
		assertEquals(
			"f01=foo,f02=Zm9v,f04=2012-12-21T12:34:56Z,f05=666F6F,f06=66 6F 6F,f07=foo,f08=1,f09=2,f10=1.0,f11=1.0,f12=true,f99=1",
			serialize(ps, JsonMap.of("f01","foo","f02",foob,"f04",parseIsoCalendar("2012-12-21T12:34:56Z"),"f05",foob,"f06",foob,"f07","foo","f08",1,"f09",2,"f10",1.0,"f11",1.0,"f12",true,"f99",1))
		);
	}

	@Test void h05_objectType_arrayProperties() throws Exception {
		HttpPartSchema ps = tObjectUon()
			.p("f01", tArray(tString()))
			.p("f02", tArray(tByte()))
			.p("f04", tArray(tDateTime()))
			.p("f05", tArray(tBinary()))
			.p("f06", tArray(tBinarySpaced()))
			.p("f07", tArray(tUon()))
			.p("f08", tArray(tInteger()))
			.p("f09", tArray(tInt64()))
			.p("f10", tArray(tNumber()))
			.p("f11", tArray(tDouble()))
			.p("f12", tArray(tBoolean()))
			.ap(tArray(tInteger()))
			.build();

		byte[] foob = "foo".getBytes();

		assertEquals(
			"(f01=@('a,b',null),f02=@(Zm9v,null),f04=@(2012-12-21T12:34:56Z,null),f05=@(666F6F,null),f06=@('66 6F 6F',null),f07=@(a,b,null),f08=@(1,2,null),f09=@(3,4,null),f10=@(1.0,2.0,null),f11=@(3.0,4.0,null),f12=@(true,false,null),f99=@(1,x,null))",
			serialize(ps, new H2(new String[]{"a,b",null},new byte[][]{foob,null},new Calendar[]{parseIsoCalendar("2012-12-21T12:34:56Z"),null},new byte[][]{foob,null},new byte[][]{foob,null},new String[]{"a","b",null},new Integer[]{1,2,null},new Integer[]{3,4,null},new Float[]{1f,2f,null},new Float[]{3f,4f,null},new Boolean[]{true,false,null},new Object[]{1,"x",null}))
		);

	}

	//-----------------------------------------------------------------------------------------------------------------
	// No-schema tests
	//-----------------------------------------------------------------------------------------------------------------
	@Test void i01a_noSchemaTests_Integer() throws Exception {
		for (Integer v : list(Integer.valueOf(1), Integer.MAX_VALUE, Integer.MIN_VALUE))
			assertEquals(valueOf(v), serialize((HttpPartSchema)null, v));
	}
	@Test void i01b_noSchemaTests_IntegerArray() throws Exception {
		assertEquals("1,2147483647,-2147483648", serialize((HttpPartSchema)null, new Integer[]{Integer.valueOf(1), Integer.MAX_VALUE, Integer.MIN_VALUE}));
	}

	@Test void i02a_noSchemaTests_Short() throws Exception {
		for (Short v : list(Short.valueOf((short)1), Short.MAX_VALUE, Short.MIN_VALUE))
			assertEquals(valueOf(v), serialize((HttpPartSchema)null, v));
	}

	@Test void i02b_noSchemaTests_ShortArray() throws Exception {
		assertEquals("1,32767,-32768,null", serialize((HttpPartSchema)null, new Short[]{Short.valueOf((short)1), Short.MAX_VALUE, Short.MIN_VALUE, null}));
	}

	@Test void i03a_noSchemaTests_Long() throws Exception {
		for (Long v : list(Long.valueOf(1), Long.MAX_VALUE, Long.MIN_VALUE))
			assertEquals(valueOf(v), serialize((HttpPartSchema)null, v));
	}

	@Test void i03b_noSchemaTests_LongArray() throws Exception {
		assertEquals("1,9223372036854775807,-9223372036854775808,null", serialize((HttpPartSchema)null, new Long[]{Long.valueOf(1), Long.MAX_VALUE, Long.MIN_VALUE, null}));
	}

	@Test void i04a_noSchemaTests_Float() throws Exception {
		for (Float v : list(Float.valueOf(1f), Float.MAX_VALUE, Float.MIN_VALUE))
			assertEquals(valueOf(v), serialize((HttpPartSchema)null, v));
	}

	@Test void i04b_noSchemaTests_FloatArray() throws Exception {
		assertEquals("1.0,3.4028235E38,1.4E-45", serialize((HttpPartSchema)null, new Float[]{Float.valueOf(1f), Float.MAX_VALUE, Float.MIN_VALUE}));
	}

	@Test void i05a_noSchemaTests_Double() throws Exception {
		for (Double v : list(Double.valueOf(1d), Double.MAX_VALUE, Double.MIN_VALUE))
			assertEquals(valueOf(v), serialize((HttpPartSchema)null, v));
	}

	@Test void i05b_noSchemaTests_DoubleArray() throws Exception {
		assertEquals("1.0,1.7976931348623157E308,4.9E-324", serialize((HttpPartSchema)null, new Double[]{Double.valueOf(1), Double.MAX_VALUE, Double.MIN_VALUE}));
	}

	@Test void i06a_noSchemaTests_Boolean() throws Exception {
		for (Boolean v : list(Boolean.TRUE, Boolean.FALSE))
			assertEquals(valueOf(v), serialize((HttpPartSchema)null, v));
	}

	@Test void i06b_noSchemaTests_BooleanArray() throws Exception {
		assertEquals("true,false,null", serialize((HttpPartSchema)null, new Boolean[]{Boolean.TRUE, Boolean.FALSE, null}));
	}

	@Test void i07_noSchemaTests_Null() throws Exception {
		assertEquals("null", serialize((HttpPartSchema)null, null));
	}

	@Test void i08a_noSchemaTests_String() throws Exception {
		for (String v : list("foo", ""))
			assertEquals(v, serialize((HttpPartSchema)null, v));
	}
	@Test void i08b_noSchemaTests_StringArray() throws Exception {
		assertEquals("foo,,null", serialize((HttpPartSchema)null, new String[]{"foo", "", null}));
	}
}