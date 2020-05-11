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
import static org.apache.juneau.testutils.TestUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenApiPartParserTest {

	static OpenApiParserSession p = OpenApiParser.DEFAULT.createSession();

	private static <T> T parse(HttpPartSchema schema, String input, Class<T> type) throws SchemaValidationException, ParseException {
		return p.parse(null, schema, input, type);
	}

	private static <T> T parse(HttpPartSchema schema, String input, Class<T> type, Type...args) throws SchemaValidationException, ParseException {
		return p.parse(null, schema, input, p.getClassMeta(type, args));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Input validations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_inputValidations_nullInput() throws Exception {
		HttpPartSchema s = tNone().build();
		assertNull(parse(s, null, String.class));

		s = tNone().required(false).build();
		assertNull(parse(s, null, String.class));

		s = tNone().required().build();
		try {
			parse(s, null, String.class);
			fail();
		} catch (Exception e) {
			assertEquals("No value specified.", e.getMessage());
		}

		s = tNone().required(true).build();
		try {
			parse(s, null, String.class);
			fail();
		} catch (Exception e) {
			assertEquals("No value specified.", e.getMessage());
		}
	}

	@Test
	public void a02_inputValidations_emptyInput() throws Exception {

		HttpPartSchema s = tNone().allowEmptyValue().build();
		assertEquals("", parse(s, "", String.class));

		s = tNone().allowEmptyValue().build();
		assertEquals("", parse(s, "", String.class));

		s = tNone().allowEmptyValue(false).build();
		try {
			parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Empty value not allowed.", e.getMessage());
		}

		try {
			parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Empty value not allowed.", e.getMessage());
		}

		assertEquals(" ", parse(s, " ", String.class));
	}

	@Test
	public void a03_inputValidations_pattern() throws Exception {
		HttpPartSchema s = tNone().pattern("x.*").allowEmptyValue().build();
		assertEquals("x", parse(s, "x", String.class));
		assertEquals("xx", parse(s, "xx", String.class));
		assertEquals(null, parse(s, null, String.class));

		try {
			parse(s, "y", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match expected pattern.  Must match pattern: x.*", e.getMessage());
		}

		try {
			parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match expected pattern.  Must match pattern: x.*", e.getMessage());
		}

		// Blank/null patterns are ignored.
		s = tNone().pattern("").allowEmptyValue().build();
		assertEquals("x", parse(s, "x", String.class));
		s = tNone().pattern(null).allowEmptyValue().build();
		assertEquals("x", parse(s, "x", String.class));
	}

	@Test
	public void a04_inputValidations_enum() throws Exception {
		HttpPartSchema s = tNone()._enum("foo").allowEmptyValue().build();

		assertEquals("foo", parse(s, "foo", String.class));
		assertEquals(null, parse(s, null, String.class));

		try {
			parse(s, "bar", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['foo']", e.getMessage());
		}

		try {
			parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['foo']", e.getMessage());
		}

		s = tNone()._enum((Set<String>)null).build();
		assertEquals("foo", parse(s, "foo", String.class));
		s = tNone()._enum((Set<String>)null).allowEmptyValue().build();
		assertEquals("foo", parse(s, "foo", String.class));

		s = tNone()._enum("foo","foo").build();
		assertEquals("foo", parse(s, "foo", String.class));
	}

	@Test
	public void a05_inputValidations_minMaxLength() throws Exception {
		HttpPartSchema s = tNone().minLength(1l).maxLength(2l).allowEmptyValue().build();

		assertEquals(null, parse(s, null, String.class));
		assertEquals("1", parse(s, "1", String.class));
		assertEquals("12", parse(s, "12", String.class));

		try {
			parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Minimum length of value not met.", e.getMessage());
		}

		try {
			parse(s, "123", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Maximum length of value exceeded.", e.getMessage());
		}

		try {
			s = tNone().minLength(2l).maxLength(1l).build();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("maxLength cannot be less than minLength."));
		}

		try {
			s = tNone().minLength(-2l).build();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("minLength cannot be less than zero."));
		}

		try {
			s = tNone().maxLength(-2l).build();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("maxLength cannot be less than zero."));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Primitive defaults
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_primitiveDefaults() throws Exception {

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

	@Test
	public void b02_primitiveDefaults_nullKeyword() throws Exception {
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
			f = "C3-" + SimpleJsonSerializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
			return f;
		}
	}


	@Test
	public void c01_stringType_simple() throws Exception {
		HttpPartSchema s = tString().build();
		assertEquals("foo", parse(s, "foo", String.class));
	}

	@Test
	public void c02_stringType_default() throws Exception {
		HttpPartSchema s = tString()._default("x").build();
		assertEquals("foo", parse(s, "foo", String.class));
		assertEquals("x", parse(s, null, String.class));
	}

	@Test
	public void c03_stringType_byteFormat() throws Exception {
		HttpPartSchema s = tByte().build();
		String in = base64Encode("foo".getBytes());
		assertEquals("foo", parse(s, in, String.class));
		assertEquals("foo", IOUtils.read(parse(s, in, InputStream.class)));
		assertEquals("foo", IOUtils.read(parse(s, in, Reader.class)));
		assertEquals("C1-foo", parse(s, in, C1.class).toString());
	}

	@Test
	public void c04_stringType_binaryFormat() throws Exception {
		HttpPartSchema s = tBinary().build();
		String in = toHex("foo".getBytes());
		assertEquals("foo", parse(s, in, String.class));
		assertEquals("foo", IOUtils.read(parse(s, in, InputStream.class)));
		assertEquals("foo", IOUtils.read(parse(s, in, Reader.class)));
		assertEquals("C1-foo", parse(s, in, C1.class).toString());
	}

	@Test
	public void c05_stringType_binarySpacedFormat() throws Exception {
		HttpPartSchema s = tBinarySpaced().build();
		String in = toSpacedHex("foo".getBytes());
		assertEquals("foo", parse(s, in, String.class));
		assertEquals("foo", IOUtils.read(parse(s, in, InputStream.class)));
		assertEquals("foo", IOUtils.read(parse(s, in, Reader.class)));
		assertEquals("C1-foo", parse(s, in, C1.class).toString());
	}

	@Test
	public void c06_stringType_dateFormat() throws Exception {
		HttpPartSchema s = tDate().build();
		String in = "2012-12-21";
		assertTrue(parse(s, in, String.class).contains("2012"));
		assertTrue(parse(s, in, Date.class).toString().contains("2012"));
		assertEquals(2012, parse(s, in, Calendar.class).get(Calendar.YEAR));
		assertEquals(2012, parse(s, in, GregorianCalendar.class).get(Calendar.YEAR));
	}

	@Test
	public void c07_stringType_dateTimeFormat() throws Exception {
		HttpPartSchema s = tDateTime().build();
		String in = "2012-12-21T12:34:56.789";
		assertTrue(parse(s, in, String.class).contains("2012"));
		assertTrue(parse(s, in, Date.class).toString().contains("2012"));
		assertEquals(2012, parse(s, in, Calendar.class).get(Calendar.YEAR));
		assertEquals(2012, parse(s, in, GregorianCalendar.class).get(Calendar.YEAR));
	}

	@Test
	public void c08_stringType_uonFormat() throws Exception {
		HttpPartSchema s = tUon().build();
		assertEquals("foo", parse(s, "foo", String.class));
		assertEquals("foo", parse(s, "'foo'", String.class));
		assertEquals("C2-foo", parse(s, "'foo'", C2.class).toString());
		// UonPartParserTest should handle all other cases.
	}

	@Test
	public void c09_stringType_noneFormat() throws Exception {
		// If no format is specified, then we should transform directly from a string.
		HttpPartSchema s = tString().build();
		assertEquals("foo", parse(s, "foo", String.class));
		assertEquals("'foo'", parse(s, "'foo'", String.class));
		assertEquals("C2-foo", parse(s, "foo", C2.class).toString());
	}

	@Test
	public void c10_stringType_noneFormat_2d() throws Exception {
		HttpPartSchema s = tArray(tString()).build();
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", List.class, Object.class));
		Object o = parse(s, "foo,bar", Object.class);
		assertObjectEquals("['foo','bar']", o);
		assertClass(OList.class, o);
		assertObjectEquals("['C2-foo','C2-bar']", parse(s, "foo,bar", C2[].class));
		assertObjectEquals("['C2-foo','C2-bar']", parse(s, "foo,bar", List.class, C2.class));
		assertEquals("C3-['foo','bar']", parse(s, "foo,bar", C3.class).toString());
	}

	@Test
	public void c11_stringType_noneFormat_3d() throws Exception {
		HttpPartSchema s = tArrayPipes(tArray(tString())).build();
		assertObjectEquals("[['foo','bar'],['baz']]", parse(s, "foo,bar|baz", String[][].class));
		assertObjectEquals("[['foo','bar'],['baz']]", parse(s, "foo,bar|baz", List.class, String[].class));
		assertObjectEquals("[['foo','bar'],['baz']]", parse(s, "foo,bar|baz", List.class, List.class, String.class));
		assertObjectEquals("[['foo','bar'],['baz']]", parse(s, "foo,bar|baz", Object[][].class));
		assertObjectEquals("[['foo','bar'],['baz']]", parse(s, "foo,bar|baz", List.class, Object[].class));
		assertObjectEquals("[['foo','bar'],['baz']]", parse(s, "foo,bar|baz", List.class, List.class, Object.class));
		Object o = parse(s, "foo,bar|baz", Object.class);
		assertObjectEquals("[['foo','bar'],['baz']]", o);
		assertClass(OList.class, o);
		assertObjectEquals("[['C2-foo','C2-bar'],['C2-baz']]", parse(s, "foo,bar|baz", C2[][].class));
		assertObjectEquals("[['C2-foo','C2-bar'],['C2-baz']]", parse(s, "foo,bar|baz", List.class, C2[].class));
		assertObjectEquals("[['C2-foo','C2-bar'],['C2-baz']]", parse(s, "foo,bar|baz", List.class, List.class, C2.class));
		assertObjectEquals("['C3-[\\'foo\\',\\'bar\\']','C3-[\\'baz\\']']", parse(s, "foo,bar|baz", C3[].class));
		assertObjectEquals("['C3-[\\'foo\\',\\'bar\\']','C3-[\\'baz\\']']", parse(s, "foo,bar|baz", List.class, C3.class));
	}

	@Test
	public void c12a_stringType_nullKeyword_plain() throws Exception {
		HttpPartSchema s = tString().build();
		assertEquals(null, parse(s, "null", String.class));
	}

	@Test
	public void c12b_stringType_nullKeyword_plain_2d() throws Exception {
		HttpPartSchema s = tArray(tString()).build();
		assertNull(parse(s, "null", String[].class));
		assertObjectEquals("[null]", parse(s, "@(null)", String[].class));
	}

	@Test
	public void c12c_stringType_nullKeyword_uon() throws Exception {
		HttpPartSchema s = tUon().build();
		assertEquals(null, parse(s, "null", String.class));
		assertEquals("null", parse(s, "'null'", String.class));
	}

	@Test
	public void c12d_stringType_nullKeyword_uon_2d() throws Exception {
		HttpPartSchema s = tArray(tUon()).build();
		assertObjectEquals("[null,'x']", parse(s, "null,x", String[].class));
		assertNull(parse(s, "null", String[].class));
		assertObjectEquals("[null]", parse(s, "@(null)", String[].class));
		assertObjectEquals("['null']", parse(s, "'null'", String[].class));
		assertObjectEquals("['null']", parse(s, "@('null')", String[].class));
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
		HttpPartSchema s = tArrayCsv().build();
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo,bar", D[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo,bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", Object.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", OList.class));
	}

	@Test
	public void d02_arrayType_collectionFormatPipes() throws Exception {
		HttpPartSchema s = tArrayPipes().build();
		assertObjectEquals("['foo','bar']", parse(s, "foo|bar", String[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo|bar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo|bar", D[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo|bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo|bar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo|bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo|bar", Object.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo|bar", OList.class));
	}

	@Test
	public void d03_arrayType_collectionFormatSsv() throws Exception {
		HttpPartSchema s = tArraySsv().build();
		assertObjectEquals("['foo','bar']", parse(s, "foo bar", String[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo bar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo bar", D[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo bar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo bar", Object.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo bar", OList.class));
	}

	@Test
	public void d04_arrayType_collectionFormatTsv() throws Exception {
		HttpPartSchema s = tArrayTsv().build();
		assertObjectEquals("['foo','bar']", parse(s, "foo\tbar", String[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo\tbar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo\tbar", D[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo\tbar", List.class, String.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo\tbar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo\tbar", List.class, D.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo\tbar", Object.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo\tbar", OList.class));
	}

	@Test
	public void d05_arrayType_collectionFormatUon() throws Exception {
		HttpPartSchema s = tArrayUon().build();
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", String[].class));
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "@(foo,bar)", D[].class));
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", List.class, String.class));
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "@(foo,bar)", List.class, D.class));
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", Object.class));
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", OList.class));
	}

	@Test
	public void d06a_arrayType_collectionFormatNone() throws Exception {
		HttpPartSchema s = tArray().build();
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo,bar", D[].class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "foo,bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", parse(s, "foo,bar", Object.class));
	}

	@Test
	public void d06b_arrayType_collectionFormatNone_autoDetectUon() throws Exception {
		HttpPartSchema s = tArray().build();
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", String[].class));
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "@(foo,bar)", D[].class));
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", List.class, String.class));
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", parse(s, "@(foo,bar)", List.class, D.class));
		assertObjectEquals("['foo','bar']", parse(s, "@(foo,bar)", Object.class));
	}

	@Test
	public void d07_arrayType_collectionFormatMulti() throws Exception {
		// collectionFormat=multi should not do any sort of splitting.
		HttpPartSchema s = tArrayMulti().build();
		assertObjectEquals("['foo,bar']", parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo,bar']", parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['D-foo,bar']", parse(s, "foo,bar", D[].class));
		assertObjectEquals("['foo,bar']", parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo,bar']", parse(s, "foo,bar", List.class, Object.class));
		assertObjectEquals("['D-foo,bar']", parse(s, "foo,bar", List.class, D.class));
		assertObjectEquals("['foo,bar']", parse(s, "foo,bar", Object.class));
	}

	@Test
	public void d08_arrayType_collectionFormatCsvAndPipes() throws Exception {
		HttpPartSchema s = tArrayPipes(tArrayCsv()).build();
		assertObjectEquals("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", String[][].class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", Object[][].class));
		assertObjectEquals("[['D-foo','D-bar'],['D-baz','D-qux']]", parse(s, "foo,bar|baz,qux", D[][].class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", List.class, List.class, String.class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", List.class, List.class, Object.class));
		assertObjectEquals("[['D-foo','D-bar'],['D-baz','D-qux']]", parse(s, "foo,bar|baz,qux", List.class, List.class, D.class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", parse(s, "foo,bar|baz,qux", Object.class));
	}

	@Test
	public void d09_arrayType_itemsBoolean() throws Exception {
		HttpPartSchema s = tArrayCsv(tBoolean()).build();
		assertObjectEquals("[true,false]", parse(s, "true,false", boolean[].class));
		assertObjectEquals("[true,false,null]", parse(s, "true,false,null", Boolean[].class));
		assertObjectEquals("[true,false,null]", parse(s, "true,false,null", Object[].class));
		assertObjectEquals("[true,false,null]", parse(s, "true,false,null", List.class, Boolean.class));
		assertObjectEquals("[true,false,null]", parse(s, "true,false,null", List.class, Object.class));
		assertObjectEquals("[true,false,null]", parse(s, "true,false,null", Object.class));
	}

	@Test
	public void d10_arrayType_itemsInteger() throws Exception {
		HttpPartSchema s = tArrayCsv(tInteger()).build();
		assertObjectEquals("[1,2]", parse(s, "1,2", int[].class));
		assertObjectEquals("[1,2,null]", parse(s, "1,2,null", Integer[].class));
		assertObjectEquals("[1,2,null]", parse(s, "1,2,null", Object[].class));
		assertObjectEquals("[1,2,null]", parse(s, "1,2,null", List.class, Integer.class));
		assertObjectEquals("[1,2,null]", parse(s, "1,2,null", List.class, Object.class));
		assertObjectEquals("[1,2,null]", parse(s, "1,2,null", Object.class));
	}

	@Test
	public void d11_arrayType_itemsFloat() throws Exception {
		HttpPartSchema s = tArrayCsv(tNumber()).build();
		assertObjectEquals("[1.0,2.0]", parse(s, "1.0,2.0", float[].class));
		assertObjectEquals("[1.0,2.0,null]", parse(s, "1.0,2.0,null", Float[].class));
		assertObjectEquals("[1.0,2.0,null]", parse(s, "1.0,2.0,null", Object[].class));
		assertObjectEquals("[1.0,2.0,null]", parse(s, "1.0,2.0,null", List.class, Float.class));
		assertObjectEquals("[1.0,2.0,null]", parse(s, "1.0,2.0,null", List.class, Object.class));
		assertObjectEquals("[1.0,2.0,null]", parse(s, "1.0,2.0,null", Object.class));
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
			this.f = "E2-" + SimpleJsonSerializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test
	public void e01_booleanType() throws Exception {
		HttpPartSchema s = tBoolean().build();
		assertEquals(true, parse(s, "true", boolean.class));
		assertEquals(true, parse(s, "true", Boolean.class));
		assertNull(parse(s, "null", Boolean.class));
		assertEquals(true, parse(s, "True", boolean.class));
		assertEquals(true, parse(s, "TRUE", boolean.class));
		assertEquals("true", parse(s, "true", String.class));
		assertNull(parse(s, "null", String.class));
		assertEquals(true, parse(s, "true", Object.class));
		assertNull(parse(s, "null", Object.class));
		assertObjectEquals("'E1-true'", parse(s, "true", E1.class));
		assertNull(parse(s, "null", E1.class));
	}

	@Test
	public void e02_booleanType_2d() throws Exception {
		HttpPartSchema s = tArray(tBoolean()).build();
		assertObjectEquals("[true,true]", parse(s, "true,true", boolean[].class));
		assertObjectEquals("[true,true,null]", parse(s, "true,true,null", Boolean[].class));
		assertObjectEquals("[true,true,null]", parse(s, "true,true,null", List.class, Boolean.class));
		assertObjectEquals("['true','true',null]", parse(s, "true,true,null", String[].class));
		assertObjectEquals("['true','true',null]", parse(s, "true,true,null", List.class, String.class));
		assertObjectEquals("[true,true,null]", parse(s, "true,true,null", Object[].class));
		assertObjectEquals("[true,true,null]", parse(s, "true,true,null", List.class, Object.class));
		assertObjectEquals("['E1-true','E1-true',null]", parse(s, "true,true,null", E1[].class));
		assertObjectEquals("['E1-true','E1-true',null]", parse(s, "true,true,null", List.class, E1.class));
		assertObjectEquals("'E2-[true,true,null]'", parse(s, "true,true,null", E2.class));

		assertObjectEquals("[true,true]", parse(s, "True,true", boolean[].class));
		assertObjectEquals("[true,true]", parse(s, "TRUE,true", boolean[].class));
	}

	@Test
	public void e03_booleanType_3d() throws Exception {
		HttpPartSchema s = tArrayPipes(tArray(tBoolean())).build();
		assertObjectEquals("[[true,true],[false]]", parse(s, "true,true|false", boolean[][].class));
		assertObjectEquals("[[true,true],[false]]", parse(s, "true,true|false", List.class, boolean[].class));
		assertObjectEquals("[[true,true],[false,null]]", parse(s, "true,true|false,null", Boolean[][].class));
		assertObjectEquals("[[true,true],[false,null]]", parse(s, "true,true|false,null", List.class, Boolean[].class));
		assertObjectEquals("[[true,true],[false,null]]", parse(s, "true,true|false,null", List.class, List.class, Boolean.class));
		assertObjectEquals("[['true','true'],['false',null]]", parse(s, "true,true|false,null", String[][].class));
		assertObjectEquals("[['true','true'],['false',null]]", parse(s, "true,true|false,null", List.class, List.class, String.class));
		assertObjectEquals("[['true','true'],['false',null]]", parse(s, "true,true|false,null", List.class, String[].class));
		assertObjectEquals("[[true,true],[false,null]]", parse(s, "true,true|false,null", Object[][].class));
		assertObjectEquals("[[true,true],[false,null]]", parse(s, "true,true|false,null", List.class, List.class, Object.class));
		assertObjectEquals("[[true,true],[false,null]]", parse(s, "true,true|false,null", List.class, Object[].class));
		assertObjectEquals("[['E1-true','E1-true'],['E1-false',null]]", parse(s, "true,true|false,null", E1[][].class));
		assertObjectEquals("[['E1-true','E1-true'],['E1-false',null]]", parse(s, "true,true|false,null", List.class, List.class, E1.class));
		assertObjectEquals("[['E1-true','E1-true'],['E1-false',null]]", parse(s, "true,true|false,null", List.class, E1[].class));
		assertObjectEquals("['E2-[true,true]','E2-[false,null]']", parse(s, "true,true|false,null", E2[].class));
		assertObjectEquals("['E2-[true,true]','E2-[false,null]']", parse(s, "true,true|false,null", List.class, E2.class));

		assertObjectEquals("[[true,true],[false]]", parse(s, "True,true|false", boolean[][].class));
		assertObjectEquals("[[true,true],[false]]", parse(s, "TRUE,true|false", boolean[][].class));
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
			this.f = "F2-" + SimpleJsonSerializer.DEFAULT.toString(in);
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
			this.f = "F4-" + SimpleJsonSerializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test
	public void f01_integerType_int32() throws Exception {
		HttpPartSchema s = tInt32().build();
		assertObjectEquals("1", parse(s, "1", int.class));
		assertObjectEquals("1", parse(s, "1", Integer.class));
		assertObjectEquals("1", parse(s, "1", short.class));
		assertObjectEquals("1", parse(s, "1", Short.class));
		assertObjectEquals("1", parse(s, "1", long.class));
		assertObjectEquals("1", parse(s, "1", Long.class));
		assertObjectEquals("'1'", parse(s, "1", String.class));
		Object o = parse(s, "1", Object.class);
		assertObjectEquals("1", o);
		assertClass(Integer.class, o);
		assertObjectEquals("'F1-1'", parse(s,  "1", F1.class));
	}

	@Test
	public void f02_integerType_int32_2d() throws Exception {
		HttpPartSchema s = tArray(tInt32()).build();
		assertObjectEquals("[1,2]", parse(s, "1,2", int[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", Integer[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", List.class, Integer.class));
		assertObjectEquals("[1,2]", parse(s, "1,2", short[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", Short[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", List.class, Short.class));
		assertObjectEquals("[1,2]", parse(s, "1,2", long[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", Long[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", List.class, Long.class));
		assertObjectEquals("['1','2']", parse(s, "1,2", String[].class));
		assertObjectEquals("['1','2']", parse(s, "1,2", List.class, String.class));
		assertObjectEquals("[1,2]", parse(s, "1,2", Object[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", List.class, Object.class));
		assertObjectEquals("['F1-1','F1-2']", parse(s,  "1,2", F1[].class));
		assertObjectEquals("['F1-1','F1-2']", parse(s,  "1,2", List.class, F1.class));
		assertObjectEquals("'F2-[1,2]'", parse(s,  "1,2", F2.class));
	}

	@Test
	public void f03_integerType_int32_3d() throws Exception {
		HttpPartSchema s = tArrayPipes(tArray(tInt32())).build();
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", int[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, int[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", Integer[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, Integer[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Integer.class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", short[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, short[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", Short[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, Short[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Short.class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", long[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, long[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", Long[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, Long[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Long.class));
		assertObjectEquals("[['1','2'],['3']]", parse(s, "1,2|3", String[][].class));
		assertObjectEquals("[['1','2'],['3']]", parse(s, "1,2|3", List.class, String[].class));
		assertObjectEquals("[['1','2'],['3']]", parse(s, "1,2|3", List.class, List.class, String.class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", Object[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, Object[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Object.class));
		assertObjectEquals("[['F1-1','F1-2'],['F1-3']]", parse(s,  "1,2|3", F1[][].class));
		assertObjectEquals("[['F1-1','F1-2'],['F1-3']]", parse(s,  "1,2|3", List.class, F1[].class));
		assertObjectEquals("[['F1-1','F1-2'],['F1-3']]", parse(s,  "1,2|3", List.class, List.class, F1.class));
		assertObjectEquals("['F2-[1,2]','F2-[3]']", parse(s, "1,2|3", F2[].class));
		assertObjectEquals("['F2-[1,2]','F2-[3]']", parse(s, "1,2|3", List.class, F2.class));
	}

	@Test
	public void f04_integerType_int64() throws Exception {
		HttpPartSchema s = tInt64().build();
		assertObjectEquals("1", parse(s, "1", int.class));
		assertObjectEquals("1", parse(s, "1", Integer.class));
		assertObjectEquals("1", parse(s, "1", short.class));
		assertObjectEquals("1", parse(s, "1", Short.class));
		assertObjectEquals("1", parse(s, "1", long.class));
		assertObjectEquals("1", parse(s, "1", Long.class));
		assertObjectEquals("'1'", parse(s, "1", String.class));
		Object o = parse(s, "1", Object.class);
		assertObjectEquals("1", o);
		assertClass(Long.class, o);
		assertObjectEquals("1", parse(s,  "1", F3.class));
	}

	@Test
	public void f05_integerType_int64_2d() throws Exception {
		HttpPartSchema s = tArray(tInt64()).build();
		assertObjectEquals("[1,2]", parse(s, "1,2", int[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", Integer[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", List.class, Integer.class));
		assertObjectEquals("[1,2]", parse(s, "1,2", short[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", Short[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", List.class, Short.class));
		assertObjectEquals("[1,2]", parse(s, "1,2", long[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", Long[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", List.class, Long.class));
		assertObjectEquals("['1','2']", parse(s, "1,2", String[].class));
		assertObjectEquals("['1','2']", parse(s, "1,2", List.class, String.class));
		assertObjectEquals("[1,2]", parse(s, "1,2", Object[].class));
		assertObjectEquals("[1,2]", parse(s, "1,2", List.class, Object.class));
		assertObjectEquals("[1,2]", parse(s,  "1,2", F3[].class));
		assertObjectEquals("[1,2]", parse(s,  "1,2", List.class, F3.class));
		assertObjectEquals("'F4-[1,2]'", parse(s,  "1,2", F4.class));
	}

	@Test
	public void f06_integerType_int64_3d() throws Exception {
		HttpPartSchema s = tArrayPipes(tArray(tInt64())).build();
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", int[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, int[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", Integer[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, Integer[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Integer.class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", short[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, short[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", Short[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, Short[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Short.class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", long[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, long[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", Long[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, Long[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Long.class));
		assertObjectEquals("[['1','2'],['3']]", parse(s, "1,2|3", String[][].class));
		assertObjectEquals("[['1','2'],['3']]", parse(s, "1,2|3", List.class, String[].class));
		assertObjectEquals("[['1','2'],['3']]", parse(s, "1,2|3", List.class, List.class, String.class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", Object[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, Object[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s, "1,2|3", List.class, List.class, Object.class));
		assertObjectEquals("[[1,2],[3]]", parse(s,  "1,2|3", F3[][].class));
		assertObjectEquals("[[1,2],[3]]", parse(s,  "1,2|3", List.class, F3[].class));
		assertObjectEquals("[[1,2],[3]]", parse(s,  "1,2|3", List.class, List.class, F3.class));
		assertObjectEquals("['F4-[1,2]','F4-[3]']", parse(s, "1,2|3", F4[].class));
		assertObjectEquals("['F4-[1,2]','F4-[3]']", parse(s, "1,2|3", List.class, F4.class));
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
			this.f = "G2-" + SimpleJsonSerializer.DEFAULT.toString(in);
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
			this.f = "G4-" + SimpleJsonSerializer.DEFAULT.toString(in);
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test
	public void g01_numberType_float() throws Exception {
		HttpPartSchema s = tFloat().build();
		assertObjectEquals("1.0", parse(s, "1", float.class));
		assertObjectEquals("1.0", parse(s, "1", Float.class));
		assertObjectEquals("1.0", parse(s, "1", double.class));
		assertObjectEquals("1.0", parse(s, "1", Double.class));
		assertObjectEquals("'1.0'", parse(s, "1", String.class));
		Object o =  parse(s, "1", Object.class);
		assertObjectEquals("1.0",o);
		assertClass(Float.class, o);
		assertObjectEquals("1.0", parse(s,  "1", G1.class));
	}

	@Test
	public void g02_numberType_float_2d() throws Exception {
		HttpPartSchema s = tArray(tFloat()).build();
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", float[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", Float[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", List.class, Float.class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", double[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", Double[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", List.class, Double.class));
		assertObjectEquals("['1.0','2.0']", parse(s, "1,2", String[].class));
		assertObjectEquals("['1.0','2.0']", parse(s, "1,2", List.class, String.class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", Object[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", List.class, Object.class));
		assertObjectEquals("[1.0,2.0]", parse(s,  "1,2", G1[].class));
		assertObjectEquals("[1.0,2.0]", parse(s,  "1,2", List.class, G1.class));
		assertObjectEquals("'G2-[1.0,2.0]'", parse(s,  "1,2", G2.class));
	}

	@Test
	public void g03_numberType_float_3d() throws Exception {
		HttpPartSchema s = tArrayPipes(tArray(tFloat())).build();
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", float[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, float[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Float[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Float[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Float.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", double[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, double[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Double[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Double[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Double.class));
		assertObjectEquals("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", String[][].class));
		assertObjectEquals("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", List.class, String[].class));
		assertObjectEquals("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", List.class, List.class, String.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Object[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Object[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Object.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", G1[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", List.class, G1[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", List.class, List.class, G1.class));
		assertObjectEquals("['G2-[1.0,2.0]','G2-[3.0]']", parse(s, "1,2|3", G2[].class));
		assertObjectEquals("['G2-[1.0,2.0]','G2-[3.0]']", parse(s, "1,2|3", List.class, G2.class));
	}

	@Test
	public void g04_numberType_double() throws Exception {
		HttpPartSchema s = tDouble().build();
		assertObjectEquals("1.0", parse(s, "1", float.class));
		assertObjectEquals("1.0", parse(s, "1", Float.class));
		assertObjectEquals("1.0", parse(s, "1", double.class));
		assertObjectEquals("1.0", parse(s, "1", Double.class));
		assertObjectEquals("'1.0'", parse(s, "1", String.class));
		Object o = parse(s, "1", Object.class);
		assertObjectEquals("1.0", o);
		assertClass(Double.class, o);
		assertObjectEquals("1.0", parse(s,  "1", G3.class));
	}

	@Test
	public void g05_numberType_double_2d() throws Exception {
		HttpPartSchema s = tArray(tDouble()).build();
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", float[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", Float[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", List.class, Float.class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", double[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", Double[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", List.class, Double.class));
		assertObjectEquals("['1.0','2.0']", parse(s, "1,2", String[].class));
		assertObjectEquals("['1.0','2.0']", parse(s, "1,2", List.class, String.class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", Object[].class));
		assertObjectEquals("[1.0,2.0]", parse(s, "1,2", List.class, Object.class));
		assertObjectEquals("[1.0,2.0]", parse(s,  "1,2", G3[].class));
		assertObjectEquals("[1.0,2.0]", parse(s,  "1,2", List.class, G3.class));
		assertObjectEquals("'G4-[1.0,2.0]'", parse(s,  "1,2", G4.class));
	}

	@Test
	public void g06_numberType_double_3d() throws Exception {
		HttpPartSchema s = tArrayPipes(tArray(tDouble())).build();
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", float[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, float[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Float[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Float[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Float.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", double[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, double[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Double[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Double[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Double.class));
		assertObjectEquals("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", String[][].class));
		assertObjectEquals("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", List.class, String[].class));
		assertObjectEquals("[['1.0','2.0'],['3.0']]", parse(s, "1,2|3", List.class, List.class, String.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", Object[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, Object[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s, "1,2|3", List.class, List.class, Object.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", G3[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", List.class, G3[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", parse(s,  "1,2|3", List.class, List.class, G3.class));
		assertObjectEquals("['G4-[1.0,2.0]','G4-[3.0]']", parse(s, "1,2|3", G4[].class));
		assertObjectEquals("['G4-[1.0,2.0]','G4-[3.0]']", parse(s, "1,2|3", List.class, G4.class));
	}


	//-----------------------------------------------------------------------------------------------------------------
	// type = object
	//-----------------------------------------------------------------------------------------------------------------

	public static class H1 {
		public int f;
	}

	@Test
	public void h01_objectType() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("object").build();
		assertObjectEquals("{f:1}", parse(s, "f=1", H1.class));
		assertObjectEquals("{f:'1'}", parse(s, "f=1", OMap.class));
		Object o = parse(s, "f=1", Object.class);
		assertObjectEquals("{f:'1'}", o);
		assertClass(OMap.class, o);
	}

	@Test
	public void h02_objectType_2d() throws Exception {
		HttpPartSchema s = tArrayUon(tObject()).build();
		assertObjectEquals("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", H1[].class));
		assertObjectEquals("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", List.class, H1.class));
		assertObjectEquals("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", OMap[].class));
		assertObjectEquals("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", List.class, OMap.class));
		assertObjectEquals("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", Object[].class));
		assertObjectEquals("[{f:1},{f:2}]", parse(s, "@((f=1),(f=2))", List.class, Object.class));
		Object o = parse(s, "@((f=1),(f=2))", Object.class);
		assertObjectEquals("[{f:1},{f:2}]", o);
		assertClass(OList.class, o);
	}

	@Test
	public void h03_objectType_3d() throws Exception {
		HttpPartSchema s = tArrayUon(tArray(tObject())).build();
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", H1[][].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, H1[].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, H1.class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", OMap[][].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, OMap[].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, OMap.class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", Object[][].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, Object[].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, Object.class));
		Object o =  parse(s, "@(@((f=1),(f=2)),@((f=3)))", Object.class);
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", o);
		assertClass(OList.class, o);
	}

	public static class H2 {
		public Object f01, f02, f03, f04, f05, f06, f07, f08, f09, f10, f11, f12, f99;
	}

	@Test
	public void h04_objectType_simpleProperties() throws Exception {
		HttpPartSchema s = tObject()
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

		byte[] foob = "foo".getBytes();
		String in = "f01=foo,f02="+base64Encode(foob)+",f04=2012-12-21T12:34:56Z,f05="+toHex(foob)+",f06="+toSpacedHex(foob)+",f07=foo,f08=1,f09=1,f10=1,f11=1,f12=true,f99=1";

		H2 h2 = parse(s, in, H2.class);
		assertObjectEquals("{f01:'foo',f02:[102,111,111],f04:'2012-12-21T12:34:56Z',f05:[102,111,111],f06:[102,111,111],f07:'foo',f08:1,f09:1,f10:1.0,f11:1.0,f12:true,f99:1}", h2);
		assertClass(String.class, h2.f01);
		assertClass(byte[].class, h2.f02);
		assertClass(GregorianCalendar.class, h2.f04);
		assertClass(byte[].class, h2.f05);
		assertClass(byte[].class, h2.f06);
		assertClass(String.class, h2.f07);
		assertClass(Integer.class, h2.f08);
		assertClass(Long.class, h2.f09);
		assertClass(Float.class, h2.f10);
		assertClass(Double.class, h2.f11);
		assertClass(Boolean.class, h2.f12);
		assertClass(Integer.class, h2.f99);

		OMap om = parse(s, in, OMap.class);
		assertObjectEquals("{f01:'foo',f02:[102,111,111],f04:'2012-12-21T12:34:56Z',f05:[102,111,111],f06:[102,111,111],f07:'foo',f08:1,f09:1,f10:1.0,f11:1.0,f12:true,f99:1}", om);
		assertClass(String.class, om.get("f01"));
		assertClass(byte[].class, om.get("f02"));
		assertClass(GregorianCalendar.class, om.get("f04"));
		assertClass(byte[].class, om.get("f05"));
		assertClass(byte[].class, om.get("f06"));
		assertClass(String.class, om.get("f07"));
		assertClass(Integer.class, om.get("f08"));
		assertClass(Long.class, om.get("f09"));
		assertClass(Float.class, om.get("f10"));
		assertClass(Double.class, om.get("f11"));
		assertClass(Boolean.class, om.get("f12"));
		assertClass(Integer.class, om.get("f99"));

		om = (OMap)parse(s, in, Object.class);
		assertObjectEquals("{f01:'foo',f02:[102,111,111],f04:'2012-12-21T12:34:56Z',f05:[102,111,111],f06:[102,111,111],f07:'foo',f08:1,f09:1,f10:1.0,f11:1.0,f12:true,f99:1}", om);
		assertClass(String.class, om.get("f01"));
		assertClass(byte[].class, om.get("f02"));
		assertClass(GregorianCalendar.class, om.get("f04"));
		assertClass(byte[].class, om.get("f05"));
		assertClass(byte[].class, om.get("f06"));
		assertClass(String.class, om.get("f07"));
		assertClass(Integer.class, om.get("f08"));
		assertClass(Long.class, om.get("f09"));
		assertClass(Float.class, om.get("f10"));
		assertClass(Double.class, om.get("f11"));
		assertClass(Boolean.class, om.get("f12"));
		assertClass(Integer.class, om.get("f99"));
	}

	@Test
	public void h05_objectType_arrayProperties() throws Exception {
		HttpPartSchema s = tObject()
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
		String in = "f01=foo,f02="+base64Encode(foob)+",f04=2012-12-21T12:34:56Z,f05="+toHex(foob)+",f06="+toSpacedHex(foob)+",f07=foo,f08=1,f09=1,f10=1,f11=1,f12=true,f99=1";

		H2 h2 = parse(s, in, H2.class);
		assertObjectEquals("{f01:['foo'],f02:[[102,111,111]],f04:['2012-12-21T12:34:56Z'],f05:[[102,111,111]],f06:[[102,111,111]],f07:['foo'],f08:[1],f09:[1],f10:[1.0],f11:[1.0],f12:[true],f99:[1]}", h2);

		OMap om = parse(s, in, OMap.class);
		assertObjectEquals("{f01:['foo'],f02:[[102,111,111]],f04:['2012-12-21T12:34:56Z'],f05:[[102,111,111]],f06:[[102,111,111]],f07:['foo'],f08:[1],f09:[1],f10:[1.0],f11:[1.0],f12:[true],f99:[1]}", om);

		om = (OMap)parse(s, in, Object.class);
		assertObjectEquals("{f01:['foo'],f02:[[102,111,111]],f04:['2012-12-21T12:34:56Z'],f05:[[102,111,111]],f06:[[102,111,111]],f07:['foo'],f08:[1],f09:[1],f10:[1.0],f11:[1.0],f12:[true],f99:[1]}", om);
	}

	@Test
	public void h06_objectType_arrayProperties_pipes() throws Exception {
		HttpPartSchema s = tObject()
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

		byte[] foob = "foo".getBytes(), barb = "bar".getBytes();
		String in = "f01=foo|bar,f02="+base64Encode(foob)+"|"+base64Encode(barb)+",f04=2012-12-21T12:34:56Z|2012-12-21T12:34:56Z,f05="+toHex(foob)+"|"+toHex(barb)+",f06="+toSpacedHex(foob)+"|"+toSpacedHex(barb)+",f07=foo|bar,f08=1|2,f09=1|2,f10=1|2,f11=1|2,f12=true|true,f99=1|2";

		H2 h2 = parse(s, in, H2.class);
		assertObjectEquals("{f01:['foo','bar'],f02:[[102,111,111],[98,97,114]],f04:['2012-12-21T12:34:56Z','2012-12-21T12:34:56Z'],f05:[[102,111,111],[98,97,114]],f06:[[102,111,111],[98,97,114]],f07:['foo','bar'],f08:[1,2],f09:[1,2],f10:[1.0,2.0],f11:[1.0,2.0],f12:[true,true],f99:[1,2]}", h2);

		OMap om = parse(s, in, OMap.class);
		assertObjectEquals("{f01:['foo','bar'],f02:[[102,111,111],[98,97,114]],f04:['2012-12-21T12:34:56Z','2012-12-21T12:34:56Z'],f05:[[102,111,111],[98,97,114]],f06:[[102,111,111],[98,97,114]],f07:['foo','bar'],f08:[1,2],f09:[1,2],f10:[1.0,2.0],f11:[1.0,2.0],f12:[true,true],f99:[1,2]}", om);

		om = (OMap)parse(s, in, Object.class);
		assertObjectEquals("{f01:['foo','bar'],f02:[[102,111,111],[98,97,114]],f04:['2012-12-21T12:34:56Z','2012-12-21T12:34:56Z'],f05:[[102,111,111],[98,97,114]],f06:[[102,111,111],[98,97,114]],f07:['foo','bar'],f08:[1,2],f09:[1,2],f10:[1.0,2.0],f11:[1.0,2.0],f12:[true,true],f99:[1,2]}", om);
	}
}