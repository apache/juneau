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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

class OpenApiPartParser_Test extends TestBase {

	static OpenApiParserSession p = OpenApiParser.DEFAULT.getSession();

	private static <T> T parse(HttpPartSchema schema, String input, Class<T> type) throws SchemaValidationException, ParseException {
		return p.parse(null, schema, input, p.getClassMeta(type));
	}

	private static <T> T parse(HttpPartSchema schema, String input, Class<T> type, Type...args) throws SchemaValidationException, ParseException {
		return p.parse(null, schema, input, p.getClassMeta(type, args));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Input validations
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_inputValidations_nullInput() throws Exception {
		assertNull(parse(T_NONE, null, String.class));
		assertNull(parse(tNone().required(false).build(), null, String.class));
		assertThrowsWithMessage(SchemaValidationException.class, "No value specified.", ()->parse(tNone().required().build(), null, String.class));
		assertThrowsWithMessage(SchemaValidationException.class, "No value specified.", ()->parse(tNone().required(true).build(), null, String.class));
	}

	@Test void a02_inputValidations_emptyInput() throws Exception {

		var s = tNone().allowEmptyValue().build();
		assertEquals("", parse(s, "", String.class));

		s = tNone().allowEmptyValue().build();
		assertEquals("", parse(s, "", String.class));

		assertThrowsWithMessage(SchemaValidationException.class, "Empty value not allowed.", ()->parse(tNone().allowEmptyValue(false).build(), "", String.class));

		assertEquals(" ", parse(s, " ", String.class));
	}

	@Test void a03_inputValidations_pattern() throws Exception {
		final HttpPartSchema s = tNone().pattern("x.*").allowEmptyValue().build();
		assertEquals("x", parse(s, "x", String.class));
		assertEquals("xx", parse(s, "xx", String.class));
		assertEquals(null, parse(s, null, String.class));

		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern.  Must match pattern: x.*", ()->parse(s, "y", String.class));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected pattern.  Must match pattern: x.*", ()->parse(s, "", String.class));

		// Blank/null patterns are ignored.
		assertEquals("x", parse(tNone().pattern("").allowEmptyValue().build(), "x", String.class));
		assertEquals("x", parse(tNone().pattern(null).allowEmptyValue().build(), "x", String.class));
	}

	@Test void a04_inputValidations_enum() throws Exception {
		var s = tNone()._enum("foo").allowEmptyValue().build();

		assertEquals("foo", parse(s, "foo", String.class));
		assertEquals(null, parse(s, null, String.class));

		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  foo", ()->parse(s, "bar", String.class));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match one of the expected values.  Must be one of the following:  foo", ()->parse(s, "", String.class));

		assertEquals("foo", parse(tNone()._enum((Set<String>)null).build(), "foo", String.class));
		assertEquals("foo", parse(tNone()._enum((Set<String>)null).allowEmptyValue().build(), "foo", String.class));
		assertEquals("foo", parse(tNone()._enum("foo","foo").build(), "foo", String.class));
	}

	@Test void a05_inputValidations_minMaxLength() throws Exception {
		var s = tNone().minLength(1L).maxLength(2L).allowEmptyValue().build();

		assertEquals(null, parse(s, null, String.class));
		assertEquals("1", parse(s, "1", String.class));
		assertEquals("12", parse(s, "12", String.class));

		assertThrowsWithMessage(SchemaValidationException.class, "Minimum length of value not met.", ()->parse(s, "", String.class));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum length of value exceeded.", ()->parse(s, "123", String.class));
		assertThrowsWithMessage(Exception.class, "maxLength cannot be less than minLength.", ()->tNone().minLength(2L).maxLength(1L).build());
		assertThrowsWithMessage(Exception.class, "minLength cannot be less than zero.", ()->tNone().minLength(-2L).build());
		assertThrowsWithMessage(Exception.class, "maxLength cannot be less than zero.", ()->tNone().maxLength(-2L).build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Primitive defaults
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_primitiveDefaults() throws Exception {
		assertEquals(null, parse(null, null, Boolean.class));
		assertEquals(false, parse(null, null, boolean.class));
		assertEquals(null, parse(null, null, Character.class));
		assertEquals("\0", parse(null, null, char.class).toString());
		assertEquals(null, parse(null, null, Short.class));
		assertEquals(0, parse(null, null, short.class).intValue());
		assertEquals(null, parse(null, null, Integer.class));
		assertEquals(0, parse(null, null, int.class).intValue());
		assertEquals(null, parse(null, null, Long.class));
		assertEquals(0, parse(null, null, long.class).intValue());
		assertEquals(null, parse(null, null, Float.class));
		assertEquals(0, parse(null, null, float.class).intValue());
		assertEquals(null, parse(null, null, Double.class));
		assertEquals(0, parse(null, null, double.class).intValue());
		assertEquals(null, parse(null, null, Byte.class));
		assertEquals(0, parse(null, null, byte.class).intValue());
	}

	@Test void b02_primitiveDefaults_nullKeyword() throws Exception {
		assertEquals(null, parse(null, "null", Boolean.class));
		assertEquals(false, parse(null, "null", boolean.class));
		assertEquals(null, parse(null, "null", Character.class));
		assertEquals("\0", parse(null, "null", char.class).toString());
		assertEquals(null, parse(null, "null", Short.class));
		assertEquals(0, parse(null, "null", short.class).intValue());
		assertEquals(null, parse(null, "null", Integer.class));
		assertEquals(0, parse(null, "null", int.class).intValue());
		assertEquals(null, parse(null, "null", Long.class));
		assertEquals(0, parse(null, "null", long.class).intValue());
		assertEquals(null, parse(null, "null", Float.class));
		assertEquals(0, parse(null, "null", float.class).intValue());
		assertEquals(null, parse(null, "null", Double.class));
		assertEquals(0, parse(null, "null", double.class).intValue());
		assertEquals(null, parse(null, "null", Byte.class));
		assertEquals(0, parse(null, "null", byte.class).intValue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type = string
	//-----------------------------------------------------------------------------------------------------------------

	public static class C1 {
		private String f;
		public C1(byte[] b) {
			f = "C1-" + new String(b);
		}
		@Override
		public String toString() {
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
		private String f;
		public C3(String[] in) {
			f = "C3-" + Json5Serializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
			return f;
		}
	}


	@Test void c01_stringType_simple() throws Exception {
		var s = T_STRING;
		assertEquals("foo", parse(s, "foo", String.class));
	}

	@Test void c02_stringType_default() throws Exception {
		var s = tString()._default("x").build();
		assertEquals("foo", parse(s, "foo", String.class));
		assertEquals("x", parse(s, null, String.class));
	}

	@Test void c03_stringType_byteFormat() throws Exception {
		var s = T_BYTE;
		var in = base64Encode("foo".getBytes());
		assertEquals("foo", parse(s, in, String.class));
		assertEquals("foo", read(parse(s, in, InputStream.class)));
		assertEquals("foo", read(parse(s, in, Reader.class)));
		assertEquals("C1-foo", parse(s, in, C1.class).toString());
	}

	@Test void c04_stringType_binaryFormat() throws Exception {
		var s = T_BINARY;
		var in = toHex("foo".getBytes());
		assertEquals("foo", parse(s, in, String.class));
		assertEquals("foo", read(parse(s, in, InputStream.class)));
		assertEquals("foo", read(parse(s, in, Reader.class)));
		assertEquals("C1-foo", parse(s, in, C1.class).toString());
	}

	@Test void c05_stringType_binarySpacedFormat() throws Exception {
		var s = T_BINARY_SPACED;
		var in = toSpacedHex("foo".getBytes());
		assertEquals("foo", parse(s, in, String.class));
		assertEquals("foo", read(parse(s, in, InputStream.class)));
		assertEquals("foo", read(parse(s, in, Reader.class)));
		assertEquals("C1-foo", parse(s, in, C1.class).toString());
	}

	@Test void c06_stringType_dateFormat() throws Exception {
		var s = T_DATE;
		var in = "2012-12-21";
		assertTrue(parse(s, in, String.class).contains("2012"));
		assertTrue(parse(s, in, Date.class).toString().contains("2012"));
		assertEquals(2012, parse(s, in, Calendar.class).get(Calendar.YEAR));
		assertEquals(2012, parse(s, in, GregorianCalendar.class).get(Calendar.YEAR));
	}

	@Test void c07_stringType_dateTimeFormat() throws Exception {
		var s = T_DATETIME;
		var in = "2012-12-21T12:34:56.789";
		assertTrue(parse(s, in, String.class).contains("2012"));
		assertTrue(parse(s, in, Date.class).toString().contains("2012"));
		assertEquals(2012, parse(s, in, Calendar.class).get(Calendar.YEAR));
		assertEquals(2012, parse(s, in, GregorianCalendar.class).get(Calendar.YEAR));
	}

	@Test void c08_stringType_uonFormat() throws Exception {
		var s = T_UON;
		assertEquals("foo", parse(s, "foo", String.class));
		assertEquals("foo", parse(s, "'foo'", String.class));
		assertEquals("C2-foo", parse(s, "'foo'", C2.class).toString());
		// UonPartParserTest should handle all other cases.
	}

	@Test void c09_stringType_noneFormat() throws Exception {
		// If no format is specified, then we should transform directly from a string.
		var s = T_STRING;
		assertEquals("foo", parse(s, "foo", String.class));
		assertEquals("'foo'", parse(s, "'foo'", String.class));
		assertEquals("C2-foo", parse(s, "foo", C2.class).toString());
	}

	@Test void c10_stringType_noneFormat_2d() throws Exception {
		var s = tArray(tString()).build();
		assertList(parse(s, "foo,bar", String[].class), "foo", "bar");
		assertList(parse(s, "foo,bar", List.class, String.class), "foo", "bar");
		assertList(parse(s, "foo,bar", Object[].class), "foo", "bar");
		assertList(parse(s, "foo,bar", List.class, Object.class), "foo", "bar");
		var o = parse(s, "foo,bar", Object.class);
		assertList(o, "foo", "bar");
		assertInstanceOf(JsonList.class, o);
		assertList(parse(s, "foo,bar", C2[].class), "C2-foo", "C2-bar");
		assertList(parse(s, "foo,bar", List.class, C2.class), "C2-foo", "C2-bar");
		assertEquals("C3-['foo','bar']", parse(s, "foo,bar", C3.class).toString());
	}

	@Test void c11_stringType_noneFormat_3d() throws Exception {
		var s = tArrayPipes(tArray(tString())).build();
		assertList(parse(s, "foo,bar|baz", String[][].class), "[foo,bar]", "[baz]");
		assertList(parse(s, "foo,bar|baz", List.class, String[].class), "[foo,bar]", "[baz]");
		assertList(parse(s, "foo,bar|baz", List.class, List.class, String.class), "[foo,bar]", "[baz]");
		assertList(parse(s, "foo,bar|baz", Object[][].class), "[foo,bar]", "[baz]");
		assertList(parse(s, "foo,bar|baz", List.class, Object[].class), "[foo,bar]", "[baz]");
		assertList(parse(s, "foo,bar|baz", List.class, List.class, Object.class), "[foo,bar]", "[baz]");
		var o = parse(s, "foo,bar|baz", Object.class);
		assertList(o, "[foo,bar]", "[baz]");
		assertInstanceOf(JsonList.class, o);
		assertList(parse(s, "foo,bar|baz", C2[][].class), "[C2-foo,C2-bar]", "[C2-baz]");
		assertList(parse(s, "foo,bar|baz", List.class, C2[].class), "[C2-foo,C2-bar]", "[C2-baz]");
		assertList(parse(s, "foo,bar|baz", List.class, List.class, C2.class), "[C2-foo,C2-bar]", "[C2-baz]");
		assertList(parse(s, "foo,bar|baz", C3[].class), "C3-['foo','bar']", "C3-['baz']");
		assertList(parse(s, "foo,bar|baz", List.class, C3.class), "C3-['foo','bar']", "C3-['baz']");
	}

	@Test void c12a_stringType_nullKeyword_plain() throws Exception {
		var s = T_STRING;
		assertEquals(null, parse(s, "null", String.class));
	}

	@Test void c12b_stringType_nullKeyword_plain_2d() throws Exception {
		var s = tArray(tString()).build();
		assertNull(parse(s, "null", String[].class));
		assertList(parse(s, "@(null)", String[].class), (String) null);
	}

	@Test void c12c_stringType_nullKeyword_uon() throws Exception {
		var s = T_UON;
		assertNull(parse(s, "null", String.class));
		assertEquals("null", parse(s, "'null'", String.class));
	}

	@Test void c12d_stringType_nullKeyword_uon_2d() throws Exception {
		var s = tArray(tUon()).build();
		assertList(parse(s, "null,x", String[].class), null, "x");
		assertNull(parse(s, "null", String[].class));
		assertList(parse(s, "@(null)", String[].class), (String) null);
		assertList(parse(s, "'null'", String[].class), "null");
		assertList(parse(s, "@('null')", String[].class), "null");
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

	@Test void d01_arrayType_collectionFormatCsv() throws Exception {
		var s = T_ARRAY_CSV;
		assertList(parse(s, "foo,bar", String[].class), "foo", "bar");
		assertList(parse(s, "foo,bar", Object[].class), "foo", "bar");
		assertList(parse(s, "foo,bar", D[].class), "D-foo", "D-bar");
		assertList(parse(s, "foo,bar", List.class, String.class), "foo", "bar");
		assertList(parse(s, "foo,bar", List.class, Object.class), "foo", "bar");
		assertList(parse(s, "foo,bar", List.class, D.class), "D-foo", "D-bar");
		assertList(parse(s, "foo,bar", Object.class), "foo", "bar");
		assertList(parse(s, "foo,bar", JsonList.class), "foo", "bar");
	}

	@Test void d02_arrayType_collectionFormatPipes() throws Exception {
		var s = T_ARRAY_PIPES;
		assertList(parse(s, "foo|bar", String[].class), "foo", "bar");
		assertList(parse(s, "foo|bar", Object[].class), "foo", "bar");
		assertList(parse(s, "foo|bar", D[].class), "D-foo", "D-bar");
		assertList(parse(s, "foo|bar", List.class, String.class), "foo", "bar");
		assertList(parse(s, "foo|bar", List.class, Object.class), "foo", "bar");
		assertList(parse(s, "foo|bar", List.class, D.class), "D-foo", "D-bar");
		assertList(parse(s, "foo|bar", Object.class), "foo", "bar");
		assertList(parse(s, "foo|bar", JsonList.class), "foo", "bar");
	}

	@Test void d03_arrayType_collectionFormatSsv() throws Exception {
		var s = T_ARRAY_SSV;
		assertList(parse(s, "foo bar", String[].class), "foo", "bar");
		assertList(parse(s, "foo bar", Object[].class), "foo", "bar");
		assertList(parse(s, "foo bar", D[].class), "D-foo", "D-bar");
		assertList(parse(s, "foo bar", List.class, String.class), "foo", "bar");
		assertList(parse(s, "foo bar", List.class, Object.class), "foo", "bar");
		assertList(parse(s, "foo bar", List.class, D.class), "D-foo", "D-bar");
		assertList(parse(s, "foo bar", Object.class), "foo", "bar");
		assertList(parse(s, "foo bar", JsonList.class), "foo", "bar");
	}

	@Test void d04_arrayType_collectionFormatTsv() throws Exception {
		var s = T_ARRAY_TSV;
		assertList(parse(s, "foo\tbar", String[].class), "foo", "bar");
		assertList(parse(s, "foo\tbar", Object[].class), "foo", "bar");
		assertList(parse(s, "foo\tbar", D[].class), "D-foo", "D-bar");
		assertList(parse(s, "foo\tbar", List.class, String.class), "foo", "bar");
		assertList(parse(s, "foo\tbar", List.class, Object.class), "foo", "bar");
		assertList(parse(s, "foo\tbar", List.class, D.class), "D-foo", "D-bar");
		assertList(parse(s, "foo\tbar", Object.class), "foo", "bar");
		assertList(parse(s, "foo\tbar", JsonList.class), "foo", "bar");
	}

	@Test void d05_arrayType_collectionFormatUon() throws Exception {
		var s = T_ARRAY_UON;
		assertList(parse(s, "@(foo,bar)", String[].class), "foo", "bar");
		assertList(parse(s, "@(foo,bar)", Object[].class), "foo", "bar");
		assertList(parse(s, "@(foo,bar)", D[].class), "D-foo", "D-bar");
		assertList(parse(s, "@(foo,bar)", List.class, String.class), "foo", "bar");
		assertList(parse(s, "@(foo,bar)", List.class, Object.class), "foo", "bar");
		assertList(parse(s, "@(foo,bar)", List.class, D.class), "D-foo", "D-bar");
		assertList(parse(s, "@(foo,bar)", Object.class), "foo", "bar");
		assertList(parse(s, "@(foo,bar)", JsonList.class), "foo", "bar");
	}

	@Test void d06a_arrayType_collectionFormatNone() throws Exception {
		var s = T_ARRAY;
		assertList(parse(s, "foo,bar", String[].class), "foo", "bar");
		assertList(parse(s, "foo,bar", Object[].class), "foo", "bar");
		assertList(parse(s, "foo,bar", D[].class), "D-foo", "D-bar");
		assertList(parse(s, "foo,bar", List.class, String.class), "foo", "bar");
		assertList(parse(s, "foo,bar", List.class, Object.class), "foo", "bar");
		assertList(parse(s, "foo,bar", List.class, D.class), "D-foo", "D-bar");
		assertList(parse(s, "foo,bar", Object.class), "foo", "bar");
	}

	@Test void d06b_arrayType_collectionFormatNone_autoDetectUon() throws Exception {
		var s = T_ARRAY;
		assertJson("['foo','bar']", parse(s, "@(foo,bar)", String[].class));
		assertJson("['foo','bar']", parse(s, "@(foo,bar)", Object[].class));
		assertJson("['D-foo','D-bar']", parse(s, "@(foo,bar)", D[].class));
		assertJson("['foo','bar']", parse(s, "@(foo,bar)", List.class, String.class));
		assertJson("['foo','bar']", parse(s, "@(foo,bar)", List.class, Object.class));
		assertJson("['D-foo','D-bar']", parse(s, "@(foo,bar)", List.class, D.class));
		assertJson("['foo','bar']", parse(s, "@(foo,bar)", Object.class));
	}

	@Test void d07_arrayType_collectionFormatMulti() throws Exception {
		// collectionFormat=multi should not do any sort of splitting.
		var s = T_ARRAY_MULTI;
		assertList(parse(s, "foo,bar", String[].class), "foo,bar");
		assertList(parse(s, "foo,bar", Object[].class), "foo,bar");
		assertList(parse(s, "foo,bar", D[].class), "D-foo,bar");
		assertList(parse(s, "foo,bar", List.class, String.class), "foo,bar");
		assertList(parse(s, "foo,bar", List.class, Object.class), "foo,bar");
		assertList(parse(s, "foo,bar", List.class, D.class), "D-foo,bar");
		assertList(parse(s, "foo,bar", Object.class), "foo,bar");
	}

	@Test void d08_arrayType_collectionFormatCsvAndPipes() throws Exception {
		var s = tArrayPipes(tArrayCsv()).build();
		assertJson("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", String[][].class));
		assertJson("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", Object[][].class));
		assertJson("[['D-foo','D-bar'],['D-baz','D-qux']]", parse(s, "foo,bar|baz,qux", D[][].class));
		assertJson("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", List.class, List.class, String.class));
		assertJson("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", List.class, List.class, Object.class));
		assertJson("[['D-foo','D-bar'],['D-baz','D-qux']]", parse(s, "foo,bar|baz,qux", List.class, List.class, D.class));
		assertJson("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", Object.class));
	}

	@Test void d09_arrayType_itemsBoolean() throws Exception {
		var s = tArrayCsv(tBoolean()).build();
		assertList(parse(s, "true,false", boolean[].class), true, false);
		assertList(parse(s, "true,false,null", Boolean[].class), true, false, null);
		assertList(parse(s, "true,false,null", Object[].class), true, false, null);
		assertList(parse(s, "true,false,null", List.class, Boolean.class), true, false, null);
		assertList(parse(s, "true,false,null", List.class, Object.class), true, false, null);
		assertList(parse(s, "true,false,null", Object.class), true, false, null);
	}

	@Test void d10_arrayType_itemsInteger() throws Exception {
		var s = tArrayCsv(tInteger()).build();
		assertList(parse(s, "1,2", int[].class), 1, 2);
		assertList(parse(s, "1,2,null", Integer[].class), 1, 2, null);
		assertList(parse(s, "1,2,null", Object[].class), 1, 2, null);
		assertList(parse(s, "1,2,null", List.class, Integer.class), 1, 2, null);
		assertList(parse(s, "1,2,null", List.class, Object.class), 1, 2, null);
		assertList(parse(s, "1,2,null", Object.class), 1, 2, null);
	}

	@Test void d11_arrayType_itemsFloat() throws Exception {
		var s = tArrayCsv(tNumber()).build();
		assertList(parse(s, "1.0,2.0", float[].class), 1.0f, 2.0f);
		assertList(parse(s, "1.0,2.0,null", Float[].class), 1.0f, 2.0f, null);
		assertList(parse(s, "1.0,2.0,null", Object[].class), 1.0f, 2.0f, null);
		assertList(parse(s, "1.0,2.0,null", List.class, Float.class), 1.0f, 2.0f, null);
		assertList(parse(s, "1.0,2.0,null", List.class, Object.class), 1.0f, 2.0f, null);
		assertList(parse(s, "1.0,2.0,null", Object.class), 1.0f, 2.0f, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type = boolean
	//-----------------------------------------------------------------------------------------------------------------

	public static class E1 {
		private String f;
		public E1(Boolean in) {
			this.f = "E1-" + in.toString();
		}
		@Override
		public String toString() {
			return f;
		}
	}

	public static class E2 {
		private String f;
		public E2(Boolean[] in) {
			this.f = "E2-" + Json5Serializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test void e01_booleanType() throws Exception {
		var s = T_BOOLEAN;
		assertEquals(true, parse(s, "true", boolean.class));
		assertEquals(true, parse(s, "true", Boolean.class));
		assertNull(parse(s, "null", Boolean.class));
		assertEquals(true, parse(s, "True", boolean.class));
		assertEquals(true, parse(s, "TRUE", boolean.class));
		assertEquals("true", parse(s, "true", String.class));
		assertNull(parse(s, "null", String.class));
		assertEquals(true, parse(s, "true", Object.class));
		assertNull(parse(s, "null", Object.class));
		assertJson("'E1-true'", parse(s, "true", E1.class));
		assertNull(parse(s, "null", E1.class));
	}

	@Test void e02_booleanType_2d() throws Exception {
		var s = tArray(tBoolean()).build();
		assertList(parse(s, "true,true", boolean[].class), true, true);
		assertList(parse(s, "true,true,null", Boolean[].class), true, true, null);
		assertList(parse(s, "true,true,null", List.class, Boolean.class), true, true, null);
		assertList(parse(s, "true,true,null", String[].class), "true", "true", null);
		assertList(parse(s, "true,true,null", List.class, String.class), "true", "true", null);
		assertList(parse(s, "true,true,null", Object[].class), true, true, null);
		assertList(parse(s, "true,true,null", List.class, Object.class), true, true, null);
		assertList(parse(s, "true,true,null", E1[].class), "E1-true", "E1-true", null);
		assertList(parse(s, "true,true,null", List.class, E1.class), "E1-true", "E1-true", null);
		assertJson("'E2-[true,true,null]'", parse(s, "true,true,null", E2.class));

		assertList(parse(s, "True,true", boolean[].class), true, true);
		assertList(parse(s, "TRUE,true", boolean[].class), true, true);
	}

	@Test void e03_booleanType_3d() throws Exception {
		var s = tArrayPipes(tArray(tBoolean())).build();
		assertJson("[[true,true],[false]]", parse(s, "true,true|false", boolean[][].class));
		assertJson("[[true,true],[false]]", parse(s, "true,true|false", List.class, boolean[].class));
		assertJson("[[true,true],[false,null]]", parse(s, "true,true|false,null", Boolean[][].class));
		assertJson("[[true,true],[false,null]]", parse(s, "true,true|false,null", List.class, Boolean[].class));
		assertJson("[[true,true],[false,null]]", parse(s, "true,true|false,null", List.class, List.class, Boolean.class));
		assertJson("[['true','true'],['false',null]]", parse(s, "true,true|false,null", String[][].class));
		assertJson("[['true','true'],['false',null]]", parse(s, "true,true|false,null", List.class, List.class, String.class));
		assertJson("[['true','true'],['false',null]]", parse(s, "true,true|false,null", List.class, String[].class));
		assertJson("[[true,true],[false,null]]", parse(s, "true,true|false,null", Object[][].class));
		assertJson("[[true,true],[false,null]]", parse(s, "true,true|false,null", List.class, List.class, Object.class));
		assertJson("[[true,true],[false,null]]", parse(s, "true,true|false,null", List.class, Object[].class));
		assertJson("[['E1-true','E1-true'],['E1-false',null]]", parse(s, "true,true|false,null", E1[][].class));
		assertJson("[['E1-true','E1-true'],['E1-false',null]]", parse(s, "true,true|false,null", List.class, List.class, E1.class));
		assertJson("[['E1-true','E1-true'],['E1-false',null]]", parse(s, "true,true|false,null", List.class, E1[].class));
		assertJson("['E2-[true,true]','E2-[false,null]']", parse(s, "true,true|false,null", E2[].class));
		assertJson("['E2-[true,true]','E2-[false,null]']", parse(s, "true,true|false,null", List.class, E2.class));

		assertJson("[[true,true],[false]]", parse(s, "True,true|false", boolean[][].class));
		assertJson("[[true,true],[false]]", parse(s, "TRUE,true|false", boolean[][].class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type = integer
	//-----------------------------------------------------------------------------------------------------------------

	public static class F1 {
		private String f;
		public F1(Integer in) {
			this.f = "F1-" + in.toString();
		}
		@Override
		public String toString() {
			return f;
		}
	}

	public static class F2 {
		private String f;
		public F2(Integer[] in) {
			this.f = "F2-" + Json5Serializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
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
		private String f;
		public F4(Long[] in) {
			this.f = "F4-" + Json5Serializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test void f01_integerType_int32() throws Exception {
		var s = T_INT32;
		assertJson("1", parse(s, "1", int.class));
		assertJson("1", parse(s, "1", Integer.class));
		assertJson("1", parse(s, "1", short.class));
		assertJson("1", parse(s, "1", Short.class));
		assertJson("1", parse(s, "1", long.class));
		assertJson("1", parse(s, "1", Long.class));
		assertJson("'1'", parse(s, "1", String.class));
		var o = parse(s, "1", Object.class);
		assertEquals(1, o);
		assertInstanceOf(Integer.class, o);
		assertJson("'F1-1'", parse(s,  "1", F1.class));
	}

	@Test void f02_integerType_int32_2d() throws Exception {
		var s = tArray(tInt32()).build();
		assertList(parse(s, "1,2", int[].class), 1, 2);
		assertList(parse(s, "1,2", Integer[].class), 1, 2);
		assertList(parse(s, "1,2", List.class, Integer.class), 1, 2);
		assertList(parse(s, "1,2", short[].class), (short)1, (short)2);
		assertList(parse(s, "1,2", Short[].class), (short)1, (short)2);
		assertList(parse(s, "1,2", List.class, Short.class), (short)1, (short)2);
		assertList(parse(s, "1,2", long[].class), 1l, 2l);
		assertList(parse(s, "1,2", Long[].class), 1l, 2l);
		assertList(parse(s, "1,2", List.class, Long.class), 1l, 2l);
		assertList(parse(s, "1,2", String[].class), "1", "2");
		assertList(parse(s, "1,2", List.class, String.class), "1", "2");
		assertList(parse(s, "1,2", Object[].class), 1, 2);
		assertList(parse(s, "1,2", List.class, Object.class), 1, 2);
		assertList(parse(s,  "1,2", F1[].class), "F1-1", "F1-2");
		assertList(parse(s,  "1,2", List.class, F1.class), "F1-1", "F1-2");
		assertJson("'F2-[1,2]'", parse(s,  "1,2", F2.class));
	}

	@Test void f03_integerType_int32_3d() throws Exception {
		var s = tArrayPipes(tArray(tInt32())).build();
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", int[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, int[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", Integer[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, Integer[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Integer.class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", short[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, short[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", Short[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, Short[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Short.class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", long[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, long[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", Long[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, Long[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Long.class));
		assertJson("[['1','2'],['3']]", parse(s, "1,2|3", String[][].class));
		assertJson("[['1','2'],['3']]", parse(s, "1,2|3", List.class, String[].class));
		assertJson("[['1','2'],['3']]", parse(s, "1,2|3", List.class, List.class, String.class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", Object[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, Object[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Object.class));
		assertJson("[['F1-1','F1-2'],['F1-3']]", parse(s,  "1,2|3", F1[][].class));
		assertJson("[['F1-1','F1-2'],['F1-3']]", parse(s,  "1,2|3", List.class, F1[].class));
		assertJson("[['F1-1','F1-2'],['F1-3']]", parse(s,  "1,2|3", List.class, List.class, F1.class));
		assertJson("['F2-[1,2]','F2-[3]']", parse(s, "1,2|3", F2[].class));
		assertJson("['F2-[1,2]','F2-[3]']", parse(s, "1,2|3", List.class, F2.class));
	}

	@Test void f04_integerType_int64() throws Exception {
		var s = T_INT64;
		assertJson("1", parse(s, "1", int.class));
		assertJson("1", parse(s, "1", Integer.class));
		assertJson("1", parse(s, "1", short.class));
		assertJson("1", parse(s, "1", Short.class));
		assertJson("1", parse(s, "1", long.class));
		assertJson("1", parse(s, "1", Long.class));
		assertJson("'1'", parse(s, "1", String.class));
		var o = parse(s, "1", Object.class);
		assertString("1", o);
		assertInstanceOf(Long.class, o);
		assertJson("1", parse(s,  "1", F3.class));
	}

	@Test void f05_integerType_int64_2d() throws Exception {
		var s = tArray(tInt64()).build();
		assertList(parse(s, "1,2", int[].class), 1, 2);
		assertList(parse(s, "1,2", Integer[].class), 1, 2);
		assertList(parse(s, "1,2", List.class, Integer.class), 1, 2);
		assertList(parse(s, "1,2", short[].class), (short)1, (short)2);
		assertList(parse(s, "1,2", Short[].class), (short)1, (short)2);
		assertList(parse(s, "1,2", List.class, Short.class), (short)1, (short)2);
		assertList(parse(s, "1,2", long[].class), 1l, 2l);
		assertList(parse(s, "1,2", Long[].class), 1l, 2l);
		assertList(parse(s, "1,2", List.class, Long.class), 1l, 2l);
		assertList(parse(s, "1,2", String[].class), "1", "2");
		assertList(parse(s, "1,2", List.class, String.class), "1", "2");
		assertList(parse(s, "1,2", Object[].class), "1", "2");
		assertList(parse(s, "1,2", List.class, Object.class), "1", "2");
		assertBeans(parse(s,  "1,2", F3[].class), "f", "1", "2");
		assertBeans(parse(s,  "1,2", List.class, F3.class), "f", "1", "2");
		assertJson("'F4-[1,2]'", parse(s,  "1,2", F4.class));
	}

	@Test void f06_integerType_int64_3d() throws Exception {
		var s = tArrayPipes(tArray(tInt64())).build();
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", int[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, int[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", Integer[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, Integer[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Integer.class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", short[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, short[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", Short[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, Short[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Short.class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", long[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, long[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", Long[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, Long[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Long.class));
		assertJson("[['1','2'],['3']]", parse(s, "1,2|3", String[][].class));
		assertJson("[['1','2'],['3']]", parse(s, "1,2|3", List.class, String[].class));
		assertJson("[['1','2'],['3']]", parse(s, "1,2|3", List.class, List.class, String.class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", Object[][].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, Object[].class));
		assertJson("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Object.class));
		assertJson("[[1,2],[3]]", parse(s,  "1,2|3", F3[][].class));
		assertJson("[[1,2],[3]]", parse(s,  "1,2|3", List.class, F3[].class));
		assertJson("[[1,2],[3]]", parse(s,  "1,2|3", List.class, List.class, F3.class));
		assertJson("['F4-[1,2]','F4-[3]']", parse(s, "1,2|3", F4[].class));
		assertJson("['F4-[1,2]','F4-[3]']", parse(s, "1,2|3", List.class, F4.class));
	}


	//-----------------------------------------------------------------------------------------------------------------
	// type = number
	//-----------------------------------------------------------------------------------------------------------------

	public static class G1 {
		private float f;
		public G1(float in) {
			this.f = in;
		}
		public float toFloat() {
			return f;
		}
	}

	public static class G2 {
		private String f;
		public G2(Float[] in) {
			this.f = "G2-" + Json5Serializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
			return f;
		}
	}

	public static class G3 {
		private Double f;
		public G3(double in) {
			this.f = in;
		}
		public double toDouble() {
			return f;
		}
	}

	public static class G4 {
		private String f;
		public G4(Double[] in) {
			this.f = "G4-" + Json5Serializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test void g01_numberType_float() throws Exception {
		var s = T_FLOAT;
		assertJson("1.0", parse(s, "1", float.class));
		assertJson("1.0", parse(s, "1", Float.class));
		assertJson("1.0", parse(s, "1", double.class));
		assertJson("1.0", parse(s, "1", Double.class));
		assertJson("'1.0'", parse(s, "1", String.class));
		var o = parse(s, "1", Object.class);
		assertEquals(1.0f, o);
		assertInstanceOf(Float.class, o);
		assertJson("1.0", parse(s,  "1", G1.class));
	}

	@Test void g02_numberType_float_2d() throws Exception {
		var s = tArray(tFloat()).build();
		assertList(parse(s, "1,2", float[].class), 1.0f, 2.0f);
		assertList(parse(s, "1,2", Float[].class), 1.0f, 2.0f);
		assertList(parse(s, "1,2", List.class, Float.class), 1.0f, 2.0f);
		assertList(parse(s, "1,2", double[].class), 1.0, 2.0);
		assertList(parse(s, "1,2", Double[].class), 1.0, 2.0);
		assertList(parse(s, "1,2", List.class, Double.class), 1.0, 2.0);
		assertList(parse(s, "1,2", String[].class), "1.0", "2.0");
		assertList(parse(s, "1,2", List.class, String.class), "1.0", "2.0");
		assertList(parse(s, "1,2", Object[].class), 1.0f, 2.0f);
		assertList(parse(s, "1,2", List.class, Object.class), 1.0f, 2.0f);
		assertBeans(parse(s,  "1,2", G1[].class), "f", "1.0", "2.0");
		assertBeans(parse(s,  "1,2", List.class, G1.class), "f", "1.0", "2.0");
		assertJson("'G2-[1.0,2.0]'", parse(s,  "1,2", G2.class));
	}

	@Test void g03_numberType_float_3d() throws Exception {
		var s = tArrayPipes(tArray(tFloat())).build();
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", float[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, float[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Float[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Float[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Float.class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", double[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, double[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Double[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Double[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Double.class));
		assertJson("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", String[][].class));
		assertJson("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", List.class, String[].class));
		assertJson("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", List.class, List.class, String.class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Object[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Object[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Object.class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", G1[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", List.class, G1[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", List.class, List.class, G1.class));
		assertJson("['G2-[1.0,2.0]','G2-[3.0]']", parse(s, "1,2|3", G2[].class));
		assertJson("['G2-[1.0,2.0]','G2-[3.0]']", parse(s, "1,2|3", List.class, G2.class));
	}

	@Test void g04_numberType_double() throws Exception {
		var s = T_DOUBLE;
		assertJson("1.0", parse(s, "1", float.class));
		assertJson("1.0", parse(s, "1", Float.class));
		assertJson("1.0", parse(s, "1", double.class));
		assertJson("1.0", parse(s, "1", Double.class));
		assertJson("'1.0'", parse(s, "1", String.class));
		var o = parse(s, "1", Object.class);
		assertString("1.0", o);
		assertInstanceOf(Double.class, o);
		assertJson("1.0", parse(s,  "1", G3.class));
	}

	@Test void g05_numberType_double_2d() throws Exception {
		var s = tArray(tDouble()).build();
		assertList(parse(s, "1,2", float[].class), 1.0f, 2.0f);
		assertList(parse(s, "1,2", Float[].class), 1.0f, 2.0f);
		assertList(parse(s, "1,2", List.class, Float.class), 1.0f, 2.0f);
		assertList(parse(s, "1,2", double[].class), 1.0, 2.0);
		assertList(parse(s, "1,2", Double[].class), 1.0, 2.0);
		assertList(parse(s, "1,2", List.class, Double.class), 1.0, 2.0);
		assertList(parse(s, "1,2", String[].class), "1.0", "2.0");
		assertList(parse(s, "1,2", List.class, String.class), "1.0", "2.0");
		assertList(parse(s, "1,2", Object[].class), 1.0, 2.0);
		assertList(parse(s, "1,2", List.class, Object.class), 1.0, 2.0);
		assertBeans(parse(s,  "1,2", G3[].class), "f", "1.0", "2.0");
		assertBeans(parse(s,  "1,2", List.class, G3.class), "f", "1.0", "2.0");
		assertJson("'G4-[1.0,2.0]'", parse(s,  "1,2", G4.class));
	}

	@Test void g06_numberType_double_3d() throws Exception {
		var s = tArrayPipes(tArray(tDouble())).build();
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", float[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, float[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Float[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Float[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Float.class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", double[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, double[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Double[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Double[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Double.class));
		assertJson("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", String[][].class));
		assertJson("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", List.class, String[].class));
		assertJson("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", List.class, List.class, String.class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Object[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Object[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Object.class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", G3[][].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", List.class, G3[].class));
		assertJson("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", List.class, List.class, G3.class));
		assertJson("['G4-[1.0,2.0]','G4-[3.0]']", parse(s, "1,2|3", G4[].class));
		assertJson("['G4-[1.0,2.0]','G4-[3.0]']", parse(s, "1,2|3", List.class, G4.class));
	}


	//-----------------------------------------------------------------------------------------------------------------
	// type = object
	//-----------------------------------------------------------------------------------------------------------------

	public static class H1 {
		public int f;
	}

	@Test void h01_objectType() throws Exception {
		var s = HttpPartSchema.create().type("object").build();
		assertJson("{f:1}", parse(s, "f=1", H1.class));
		assertJson("{f:'1'}", parse(s, "f=1", JsonMap.class));
		var o = parse(s, "f=1", Object.class);
		assertBean(o, "f", "1");
		assertInstanceOf(JsonMap.class, o);
	}

	@Test void h02_objectType_2d() throws Exception {
		var s = tArrayUon(tObject()).build();
		assertJson("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", H1[].class));
		assertJson("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", List.class, H1.class));
		assertJson("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", JsonMap[].class));
		assertJson("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", List.class, JsonMap.class));
		assertJson("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", Object[].class));
		assertJson("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", List.class, Object.class));
		var o = parse(s, "@((f=1),(f=2))", Object.class);
		assertBeans(o, "f", "1", "2");
		assertInstanceOf(JsonList.class, o);
	}

	@Test void h03_objectType_3d() throws Exception {
		var s = tArrayUon(tArray(tObject())).build();
		assertJson("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", H1[][].class));
		assertJson("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, H1[].class));
		assertJson("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, H1.class));
		assertJson("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", JsonMap[][].class));
		assertJson("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, JsonMap[].class));
		assertJson("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, JsonMap.class));
		assertJson("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", Object[][].class));
		assertJson("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, Object[].class));
		assertJson("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, Object.class));
		var o = parse(s, "@(@((f=1),(f=2)),@((f=3)))", Object.class);
		assertBeans(o, "#{f}", "[{1},{2}]", "[{3}]");
		assertInstanceOf(JsonList.class, o);
	}

	public static class H2 {
		public Object f01, f02, f03, f04, f05, f06, f07, f08, f09, f10, f11, f12, f99;
	}

	@Test void h04_objectType_simpleProperties() throws Exception {
		var s = tObject()
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
			.build();

		var foob = "foo".getBytes();
		var in = "f01=foo,f02="+base64Encode(foob)+",f04=2012-12-21T12:34:56Z,f05="+toHex(foob)+",f06="+toSpacedHex(foob)+",f07=foo,f08=1,f09=1,f10=1,f11=1,f12=true,f99=1";

		var h2 = parse(s, in, H2.class);
		assertBean(h2, "f01,f02,f04,f05,f06,f07,f08,f09,f10,f11,f12,f99", "foo,666F6F,2012-12-21T12:34:56Z,666F6F,666F6F,foo,1,1,1.0,1.0,true,1");
		assertInstanceOf(String.class, h2.f01);
		assertInstanceOf(byte[].class, h2.f02);
		assertInstanceOf(GregorianCalendar.class, h2.f04);
		assertInstanceOf(byte[].class, h2.f05);
		assertInstanceOf(byte[].class, h2.f06);
		assertInstanceOf(String.class, h2.f07);
		assertInstanceOf(Integer.class, h2.f08);
		assertInstanceOf(Long.class, h2.f09);
		assertInstanceOf(Float.class, h2.f10);
		assertInstanceOf(Double.class, h2.f11);
		assertInstanceOf(Boolean.class, h2.f12);
		assertInstanceOf(Integer.class, h2.f99);

		var om = parse(s, in, JsonMap.class);
		assertMap(om, "f01,f02,f04,f05,f06,f07,f08,f09,f10,f11,f12,f99", "foo,666F6F,2012-12-21T12:34:56Z,666F6F,666F6F,foo,1,1,1.0,1.0,true,1");
		assertInstanceOf(String.class, om.get("f01"));
		assertInstanceOf(byte[].class, om.get("f02"));
		assertInstanceOf(GregorianCalendar.class, om.get("f04"));
		assertInstanceOf(byte[].class, om.get("f05"));
		assertInstanceOf(byte[].class, om.get("f06"));
		assertInstanceOf(String.class, om.get("f07"));
		assertInstanceOf(Integer.class, om.get("f08"));
		assertInstanceOf(Long.class, om.get("f09"));
		assertInstanceOf(Float.class, om.get("f10"));
		assertInstanceOf(Double.class, om.get("f11"));
		assertInstanceOf(Boolean.class, om.get("f12"));
		assertInstanceOf(Integer.class, om.get("f99"));

		om = (JsonMap)parse(s, in, Object.class);
		assertMap(om, "f01,f02,f04,f05,f06,f07,f08,f09,f10,f11,f12,f99", "foo,666F6F,2012-12-21T12:34:56Z,666F6F,666F6F,foo,1,1,1.0,1.0,true,1");
		assertInstanceOf(String.class, om.get("f01"));
		assertInstanceOf(byte[].class, om.get("f02"));
		assertInstanceOf(GregorianCalendar.class, om.get("f04"));
		assertInstanceOf(byte[].class, om.get("f05"));
		assertInstanceOf(byte[].class, om.get("f06"));
		assertInstanceOf(String.class, om.get("f07"));
		assertInstanceOf(Integer.class, om.get("f08"));
		assertInstanceOf(Long.class, om.get("f09"));
		assertInstanceOf(Float.class, om.get("f10"));
		assertInstanceOf(Double.class, om.get("f11"));
		assertInstanceOf(Boolean.class, om.get("f12"));
		assertInstanceOf(Integer.class, om.get("f99"));
	}

	@Test void h05_objectType_arrayProperties() throws Exception {
		var s = tObject()
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

		var foob = "foo".getBytes();
		var in = "f01=foo,f02="+base64Encode(foob)+",f04=2012-12-21T12:34:56Z,f05="+toHex(foob)+",f06="+toSpacedHex(foob)+",f07=foo,f08=1,f09=1,f10=1,f11=1,f12=true,f99=1";

		var h2 = parse(s, in, H2.class);
		assertBean(h2, "f01,f02,f04,f05,f06,f07,f08,f09,f10,f11,f12,f99", "[foo],[666F6F],[2012-12-21T12:34:56Z],[666F6F],[666F6F],[foo],[1],[1],[1.0],[1.0],[true],[1]");

		var om = parse(s, in, JsonMap.class);
		assertMap(om, "f01,f02,f04,f05,f06,f07,f08,f09,f10,f11,f12,f99", "[foo],[666F6F],[2012-12-21T12:34:56Z],[666F6F],[666F6F],[foo],[1],[1],[1.0],[1.0],[true],[1]");

		om = (JsonMap)parse(s, in, Object.class);
		assertMap(om, "f01,f02,f04,f05,f06,f07,f08,f09,f10,f11,f12,f99", "[foo],[666F6F],[2012-12-21T12:34:56Z],[666F6F],[666F6F],[foo],[1],[1],[1.0],[1.0],[true],[1]");
	}

	@Test void h06_objectType_arrayProperties_pipes() throws Exception {
		var s = tObject()
			.p("f01", tArrayPipes(tString()))
			.p("f02", tArrayPipes(tByte()))
			.p("f04", tArrayPipes(tDateTime()))
			.p("f05", tArrayPipes(tBinary()))
			.p("f06", tArrayPipes(tBinarySpaced()))
			.p("f07", tArrayPipes(tUon()))
			.p("f08", tArrayPipes(tInteger()))
			.p("f09", tArrayPipes(tInt64()))
			.p("f10", tArrayPipes(tNumber()))
			.p("f11", tArrayPipes(tDouble()))
			.p("f12", tArrayPipes(tBoolean()))
			.ap(tArrayPipes(tInteger()))
			.build();

		var foob = "foo".getBytes();
		var barb = "bar".getBytes();
		var in = "f01=foo|bar,f02="+base64Encode(foob)+"|"+base64Encode(barb)+",f04=2012-12-21T12:34:56Z|2012-12-21T12:34:56Z,f05="+toHex(foob)+"|"+toHex(barb)+",f06="+toSpacedHex(foob)+"|"+toSpacedHex(barb)+",f07=foo|bar,f08=1|2,f09=1|2,f10=1|2,f11=1|2,f12=true|true,f99=1|2";

		var h2 = parse(s, in, H2.class);
		assertBean(h2, "f01,f02,f04,f05,f06,f07,f08,f09,f10,f11,f12,f99", "[foo,bar],[666F6F,626172],[2012-12-21T12:34:56Z,2012-12-21T12:34:56Z],[666F6F,626172],[666F6F,626172],[foo,bar],[1,2],[1,2],[1.0,2.0],[1.0,2.0],[true,true],[1,2]");

		var om = parse(s, in, JsonMap.class);
		assertMap(om, "f01,f02,f04,f05,f06,f07,f08,f09,f10,f11,f12,f99", "[foo,bar],[666F6F,626172],[2012-12-21T12:34:56Z,2012-12-21T12:34:56Z],[666F6F,626172],[666F6F,626172],[foo,bar],[1,2],[1,2],[1.0,2.0],[1.0,2.0],[true,true],[1,2]");

		om = (JsonMap)parse(s, in, Object.class);
		assertMap(om, "f01,f02,f04,f05,f06,f07,f08,f09,f10,f11,f12,f99", "[foo,bar],[666F6F,626172],[2012-12-21T12:34:56Z,2012-12-21T12:34:56Z],[666F6F,626172],[666F6F,626172],[foo,bar],[1,2],[1,2],[1.0,2.0],[1.0,2.0],[true,true],[1,2]");
	}
}