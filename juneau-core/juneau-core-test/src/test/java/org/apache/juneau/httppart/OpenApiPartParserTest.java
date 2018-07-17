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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenApiPartParserTest {

	static OpenApiPartParserSession p = OpenApiPartParser.DEFAULT.createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Input validations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_inputValidations_nullInput() throws Exception {
		HttpPartSchema s = schema().build();
		assertNull(p.parse(s, null, String.class));

		s = schema().required(false).build();
		assertNull(p.parse(s, null, String.class));

		s = schema().required().build();
		try {
			p.parse(s, null, String.class);
			fail();
		} catch (Exception e) {
			assertEquals("No value specified.", e.getMessage());
		}

		s = schema().required(true).build();
		try {
			p.parse(s, null, String.class);
			fail();
		} catch (Exception e) {
			assertEquals("No value specified.", e.getMessage());
		}
	}

	@Test
	public void a02_inputValidations_emptyInput() throws Exception {

		HttpPartSchema s = schema().allowEmptyValue().build();
		assertEquals("", p.parse(s, "", String.class));

		s = schema().allowEmptyValue().build();
		assertEquals("", p.parse(s, "", String.class));

		s = schema().allowEmptyValue(false).build();
		try {
			p.parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Empty value not allowed.", e.getMessage());
		}

		try {
			p.parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Empty value not allowed.", e.getMessage());
		}

		assertEquals(" ", p.parse(s, " ", String.class));
	}

	@Test
	public void a03_inputValidations_pattern() throws Exception {
		HttpPartSchema s = schema().pattern("x.*").allowEmptyValue().build();
		assertEquals("x", p.parse(s, "x", String.class));
		assertEquals("xx", p.parse(s, "xx", String.class));
		assertEquals(null, p.parse(s, null, String.class));

		try {
			p.parse(s, "y", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match expected pattern.  Must match pattern: x.*", e.getMessage());
		}

		try {
			p.parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match expected pattern.  Must match pattern: x.*", e.getMessage());
		}

		// Blank/null patterns are ignored.
		s = schema().pattern("").allowEmptyValue().build();
		assertEquals("x", p.parse(s, "x", String.class));
		s = schema().pattern(null).allowEmptyValue().build();
		assertEquals("x", p.parse(s, "x", String.class));
	}

	@Test
	public void a04_inputValidations_enum() throws Exception {
		HttpPartSchema s = schema()._enum("foo").allowEmptyValue().build();

		assertEquals("foo", p.parse(s, "foo", String.class));
		assertEquals(null, p.parse(s, null, String.class));

		try {
			p.parse(s, "bar", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['foo']", e.getMessage());
		}

		try {
			p.parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Value does not match one of the expected values.  Must be one of the following: ['foo']", e.getMessage());
		}

		s = schema()._enum((Set<String>)null).build();
		assertEquals("foo", p.parse(s, "foo", String.class));
		s = schema()._enum((Set<String>)null).allowEmptyValue().build();
		assertEquals("foo", p.parse(s, "foo", String.class));

		s = schema()._enum("foo","foo").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
	}

	@Test
	public void a05_inputValidations_minMaxLength() throws Exception {
		HttpPartSchema s = schema().minLength(1l).maxLength(2l).allowEmptyValue().build();

		assertEquals(null, p.parse(s, null, String.class));
		assertEquals("1", p.parse(s, "1", String.class));
		assertEquals("12", p.parse(s, "12", String.class));

		try {
			p.parse(s, "", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Minimum length of value not met.", e.getMessage());
		}

		try {
			p.parse(s, "123", String.class);
			fail();
		} catch (Exception e) {
			assertEquals("Maximum length of value exceeded.", e.getMessage());
		}

		try {
			s = schema().minLength(2l).maxLength(1l).build();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("maxLength cannot be less than minLength."));
		}

		try {
			s = schema().minLength(-2l).build();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("minLength cannot be less than zero."));
		}

		try {
			s = schema().maxLength(-2l).build();
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

		assertEquals(null, p.parse(null, null, Boolean.class));
		assertEquals(false, p.parse(null, null, boolean.class));
		assertEquals(null, p.parse(null, null, Character.class));
		assertEquals("\0", p.parse(null, null, char.class).toString());
		assertEquals(null, p.parse(null, null, Short.class));
		assertEquals(0, p.parse(null, null, short.class).intValue());
		assertEquals(null, p.parse(null, null, Integer.class));
		assertEquals(0, p.parse(null, null, int.class).intValue());
		assertEquals(null, p.parse(null, null, Long.class));
		assertEquals(0, p.parse(null, null, long.class).intValue());
		assertEquals(null, p.parse(null, null, Float.class));
		assertEquals(0, p.parse(null, null, float.class).intValue());
		assertEquals(null, p.parse(null, null, Double.class));
		assertEquals(0, p.parse(null, null, double.class).intValue());
		assertEquals(null, p.parse(null, null, Byte.class));
		assertEquals(0, p.parse(null, null, byte.class).intValue());
	}

	@Test
	public void b02_primitiveDefaults_nullKeyword() throws Exception {
		assertEquals(null, p.parse(null, "null", Boolean.class));
		assertEquals(false, p.parse(null, "null", boolean.class));
		assertEquals(null, p.parse(null, "null", Character.class));
		assertEquals("\0", p.parse(null, "null", char.class).toString());
		assertEquals(null, p.parse(null, "null", Short.class));
		assertEquals(0, p.parse(null, "null", short.class).intValue());
		assertEquals(null, p.parse(null, "null", Integer.class));
		assertEquals(0, p.parse(null, "null", int.class).intValue());
		assertEquals(null, p.parse(null, "null", Long.class));
		assertEquals(0, p.parse(null, "null", long.class).intValue());
		assertEquals(null, p.parse(null, "null", Float.class));
		assertEquals(0, p.parse(null, "null", float.class).intValue());
		assertEquals(null, p.parse(null, "null", Double.class));
		assertEquals(0, p.parse(null, "null", double.class).intValue());
		assertEquals(null, p.parse(null, "null", Byte.class));
		assertEquals(0, p.parse(null, "null", byte.class).intValue());
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
		HttpPartSchema s = schema("string").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
	}

	@Test
	public void c02_stringType_default() throws Exception {
		HttpPartSchema s = schema("string")._default("x").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
		assertEquals("x", p.parse(s, null, String.class));
	}

	@Test
	public void c03_stringType_byteFormat() throws Exception {
		HttpPartSchema s = schema("string", "byte").build();
		String in = base64Encode("foo".getBytes());
		assertEquals("foo", p.parse(s, in, String.class));
		assertEquals("foo", IOUtils.read(p.parse(s, in, InputStream.class)));
		assertEquals("foo", IOUtils.read(p.parse(s, in, Reader.class)));
		assertEquals("C1-foo", p.parse(s, in, C1.class).toString());
	}

	@Test
	public void c04_stringType_binaryFormat() throws Exception {
		HttpPartSchema s = schema("string", "binary").build();
		String in = toHex("foo".getBytes());
		assertEquals("foo", p.parse(s, in, String.class));
		assertEquals("foo", IOUtils.read(p.parse(s, in, InputStream.class)));
		assertEquals("foo", IOUtils.read(p.parse(s, in, Reader.class)));
		assertEquals("C1-foo", p.parse(s, in, C1.class).toString());
	}

	@Test
	public void c05_stringType_binarySpacedFormat() throws Exception {
		HttpPartSchema s = schema("string", "binary-spaced").build();
		String in = toSpacedHex("foo".getBytes());
		assertEquals("foo", p.parse(s, in, String.class));
		assertEquals("foo", IOUtils.read(p.parse(s, in, InputStream.class)));
		assertEquals("foo", IOUtils.read(p.parse(s, in, Reader.class)));
		assertEquals("C1-foo", p.parse(s, in, C1.class).toString());
	}

	@Test
	public void c06_stringType_dateFormat() throws Exception {
		HttpPartSchema s = schema("string", "date").build();
		String in = "2012-12-21";
		assertTrue(p.parse(s, in, String.class).contains("2012"));
		assertTrue(p.parse(s, in, Date.class).toString().contains("2012"));
		assertEquals(2012, p.parse(s, in, Calendar.class).get(Calendar.YEAR));
		assertEquals(2012, p.parse(s, in, GregorianCalendar.class).get(Calendar.YEAR));
	}

	@Test
	public void c07_stringType_dateTimeFormat() throws Exception {
		HttpPartSchema s = schema("string", "date-time").build();
		String in = "2012-12-21T12:34:56.789";
		assertTrue(p.parse(s, in, String.class).contains("2012"));
		assertTrue(p.parse(s, in, Date.class).toString().contains("2012"));
		assertEquals(2012, p.parse(s, in, Calendar.class).get(Calendar.YEAR));
		assertEquals(2012, p.parse(s, in, GregorianCalendar.class).get(Calendar.YEAR));
	}

	@Test
	public void c08_stringType_uonFormat() throws Exception {
		HttpPartSchema s = schema("string", "uon").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
		assertEquals("foo", p.parse(s, "'foo'", String.class));
		assertEquals("C2-foo", p.parse(s, "'foo'", C2.class).toString());
		// UonPartParserTest should handle all other cases.
	}

	@Test
	public void c09_stringType_noneFormat() throws Exception {
		// If no format is specified, then we should transform directly from a string.
		HttpPartSchema s = schema("string").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
		assertEquals("'foo'", p.parse(s, "'foo'", String.class));
		assertEquals("C2-foo", p.parse(s, "foo", C2.class).toString());
	}

	@Test
	public void c10_stringType_noneFormat_2d() throws Exception {
		HttpPartSchema s = schema("array").items(schema("string")).build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, Object.class));
		Object o = p.parse(s, "foo,bar", Object.class);
		assertObjectEquals("['foo','bar']", o);
		assertClass(ObjectList.class, o);
		assertObjectEquals("['C2-foo','C2-bar']", p.parse(s, "foo,bar", C2[].class));
		assertObjectEquals("['C2-foo','C2-bar']", p.parse(s, "foo,bar", List.class, C2.class));
		assertEquals("C3-['foo','bar']", p.parse(s, "foo,bar", C3.class).toString());
	}

	@Test
	public void c11_stringType_noneFormat_3d() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("pipes").items(schema("array").items(schema("string"))).build();
		assertObjectEquals("[['foo','bar'],['baz']]", p.parse(s, "foo,bar|baz", String[][].class));
		assertObjectEquals("[['foo','bar'],['baz']]", p.parse(s, "foo,bar|baz", List.class, String[].class));
		assertObjectEquals("[['foo','bar'],['baz']]", p.parse(s, "foo,bar|baz", List.class, List.class, String.class));
		assertObjectEquals("[['foo','bar'],['baz']]", p.parse(s, "foo,bar|baz", Object[][].class));
		assertObjectEquals("[['foo','bar'],['baz']]", p.parse(s, "foo,bar|baz", List.class, Object[].class));
		assertObjectEquals("[['foo','bar'],['baz']]", p.parse(s, "foo,bar|baz", List.class, List.class, Object.class));
		Object o = p.parse(s, "foo,bar|baz", Object.class);
		assertObjectEquals("[['foo','bar'],['baz']]", o);
		assertClass(ObjectList.class, o);
		assertObjectEquals("[['C2-foo','C2-bar'],['C2-baz']]", p.parse(s, "foo,bar|baz", C2[][].class));
		assertObjectEquals("[['C2-foo','C2-bar'],['C2-baz']]", p.parse(s, "foo,bar|baz", List.class, C2[].class));
		assertObjectEquals("[['C2-foo','C2-bar'],['C2-baz']]", p.parse(s, "foo,bar|baz", List.class, List.class, C2.class));
		assertObjectEquals("['C3-[\\'foo\\',\\'bar\\']','C3-[\\'baz\\']']", p.parse(s, "foo,bar|baz", C3[].class));
		assertObjectEquals("['C3-[\\'foo\\',\\'bar\\']','C3-[\\'baz\\']']", p.parse(s, "foo,bar|baz", List.class, C3.class));
	}

	@Test
	public void c12a_stringType_nullKeyword_plain() throws Exception {
		HttpPartSchema s = schema("string").build();
		assertEquals("null", p.parse(s, "null", String.class));
	}

	@Test
	public void c12b_stringType_nullKeyword_plain_2d() throws Exception {
		HttpPartSchema s = schema("array").items(schema("string")).build();
		assertObjectEquals("['null']", p.parse(s, "null", String[].class));
		assertObjectEquals("[null]", p.parse(s, "@(null)", String[].class));
	}

	@Test
	public void c12c_stringType_nullKeyword_uon() throws Exception {
		HttpPartSchema s = schema("string","uon").build();
		assertEquals(null, p.parse(s, "null", String.class));
		assertEquals("null", p.parse(s, "'null'", String.class));
	}

	@Test
	public void c12d_stringType_nullKeyword_uon_2d() throws Exception {
		HttpPartSchema s = schema("array").items(schema("string","uon")).build();
		assertObjectEquals("[null,'x']", p.parse(s, "null,x", String[].class));
		assertObjectEquals("[null]", p.parse(s, "null", String[].class));
		assertObjectEquals("[null]", p.parse(s, "@(null)", String[].class));
		assertObjectEquals("['null']", p.parse(s, "'null'", String[].class));
		assertObjectEquals("['null']", p.parse(s, "@('null')", String[].class));
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
		HttpPartSchema s = schema("array").collectionFormat("csv").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo,bar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo,bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", ObjectList.class));
	}

	@Test
	public void d02_arrayType_collectionFormatPipes() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("pipes").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo|bar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo|bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", ObjectList.class));
	}

	@Test
	public void d03_arrayType_collectionFormatSsv() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("ssv").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo bar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", ObjectList.class));
	}

	@Test
	public void d04_arrayType_collectionFormatTsv() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("tsv").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo\tbar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo\tbar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", ObjectList.class));
	}

	@Test
	public void d05_arrayType_collectionFormatUon() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("uon").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "@(foo,bar)", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "@(foo,bar)", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", ObjectList.class));
	}

	@Test
	public void d06a_arrayType_collectionFormatNone() throws Exception {
		HttpPartSchema s = schema("array").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo,bar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "foo,bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", Object.class));
	}

	@Test
	public void d06b_arrayType_collectionFormatNone_autoDetectUon() throws Exception {
		HttpPartSchema s = schema("array").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", Object[].class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "@(foo,bar)", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, Object.class));
		assertObjectEquals("['D-foo','D-bar']", p.parse(s, "@(foo,bar)", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", Object.class));
	}

	@Test
	public void d07_arrayType_collectionFormatMulti() throws Exception {
		// collectionFormat=multi should not do any sort of splitting.
		HttpPartSchema s = schema("array").collectionFormat("multi").build();
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['D-foo,bar']", p.parse(s, "foo,bar", D[].class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", List.class, Object.class));
		assertObjectEquals("['D-foo,bar']", p.parse(s, "foo,bar", List.class, D.class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", Object.class));
	}

	@Test
	public void d08_arrayType_collectionFormatCsvAndPipes() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("pipes").items(schema("array").collectionFormat("csv")).build();
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", String[][].class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", Object[][].class));
		assertObjectEquals("[['D-foo','D-bar'],['D-baz','D-qux']]", p.parse(s, "foo,bar|baz,qux", D[][].class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", List.class, List.class, String.class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", List.class, List.class, Object.class));
		assertObjectEquals("[['D-foo','D-bar'],['D-baz','D-qux']]", p.parse(s, "foo,bar|baz,qux", List.class, List.class, D.class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", Object.class));
	}

	@Test
	public void d09_arrayType_itemsBoolean() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("csv").items(schema("boolean")).build();
		assertObjectEquals("[true,false]", p.parse(s, "true,false", boolean[].class));
		assertObjectEquals("[true,false,null]", p.parse(s, "true,false,null", Boolean[].class));
		assertObjectEquals("[true,false,null]", p.parse(s, "true,false,null", Object[].class));
		assertObjectEquals("[true,false,null]", p.parse(s, "true,false,null", List.class, Boolean.class));
		assertObjectEquals("[true,false,null]", p.parse(s, "true,false,null", List.class, Object.class));
		assertObjectEquals("[true,false,null]", p.parse(s, "true,false,null", Object.class));
	}

	@Test
	public void d10_arrayType_itemsInteger() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("csv").items(schema("integer")).build();
		assertObjectEquals("[1,2]", p.parse(s, "1,2", int[].class));
		assertObjectEquals("[1,2,null]", p.parse(s, "1,2,null", Integer[].class));
		assertObjectEquals("[1,2,null]", p.parse(s, "1,2,null", Object[].class));
		assertObjectEquals("[1,2,null]", p.parse(s, "1,2,null", List.class, Integer.class));
		assertObjectEquals("[1,2,null]", p.parse(s, "1,2,null", List.class, Object.class));
		assertObjectEquals("[1,2,null]", p.parse(s, "1,2,null", Object.class));
	}

	@Test
	public void d11_arrayType_itemsFloat() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("csv").items(schema("number")).build();
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1.0,2.0", float[].class));
		assertObjectEquals("[1.0,2.0,null]", p.parse(s, "1.0,2.0,null", Float[].class));
		assertObjectEquals("[1.0,2.0,null]", p.parse(s, "1.0,2.0,null", Object[].class));
		assertObjectEquals("[1.0,2.0,null]", p.parse(s, "1.0,2.0,null", List.class, Float.class));
		assertObjectEquals("[1.0,2.0,null]", p.parse(s, "1.0,2.0,null", List.class, Object.class));
		assertObjectEquals("[1.0,2.0,null]", p.parse(s, "1.0,2.0,null", Object.class));
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
		HttpPartSchema s = schema("boolean").build();
		assertEquals(true, p.parse(s, "true", boolean.class));
		assertEquals(true, p.parse(s, "true", Boolean.class));
		assertNull(p.parse(s, "null", Boolean.class));
		assertEquals(true, p.parse(s, "True", boolean.class));
		assertEquals(true, p.parse(s, "TRUE", boolean.class));
		assertEquals("true", p.parse(s, "true", String.class));
		assertNull(p.parse(s, "null", String.class));
		assertEquals(true, p.parse(s, "true", Object.class));
		assertNull(p.parse(s, "null", Object.class));
		assertObjectEquals("'E1-true'", p.parse(s, "true", E1.class));
		assertNull(p.parse(s, "null", E1.class));
	}

	@Test
	public void e02_booleanType_2d() throws Exception {
		HttpPartSchema s = schema("array").items(schema("boolean")).build();
		assertObjectEquals("[true,true]", p.parse(s, "true,true", boolean[].class));
		assertObjectEquals("[true,true,null]", p.parse(s, "true,true,null", Boolean[].class));
		assertObjectEquals("[true,true,null]", p.parse(s, "true,true,null", List.class, Boolean.class));
		assertObjectEquals("['true','true',null]", p.parse(s, "true,true,null", String[].class));
		assertObjectEquals("['true','true',null]", p.parse(s, "true,true,null", List.class, String.class));
		assertObjectEquals("[true,true,null]", p.parse(s, "true,true,null", Object[].class));
		assertObjectEquals("[true,true,null]", p.parse(s, "true,true,null", List.class, Object.class));
		assertObjectEquals("['E1-true','E1-true',null]", p.parse(s, "true,true,null", E1[].class));
		assertObjectEquals("['E1-true','E1-true',null]", p.parse(s, "true,true,null", List.class, E1.class));
		assertObjectEquals("'E2-[true,true,null]'", p.parse(s, "true,true,null", E2.class));

		assertObjectEquals("[true,true]", p.parse(s, "True,true", boolean[].class));
		assertObjectEquals("[true,true]", p.parse(s, "TRUE,true", boolean[].class));
	}

	@Test
	public void e03_booleanType_3d() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("pipes").items(schema("array").items(schema("boolean"))).build();
		assertObjectEquals("[[true,true],[false]]", p.parse(s, "true,true|false", boolean[][].class));
		assertObjectEquals("[[true,true],[false]]", p.parse(s, "true,true|false", List.class, boolean[].class));
		assertObjectEquals("[[true,true],[false,null]]", p.parse(s, "true,true|false,null", Boolean[][].class));
		assertObjectEquals("[[true,true],[false,null]]", p.parse(s, "true,true|false,null", List.class, Boolean[].class));
		assertObjectEquals("[[true,true],[false,null]]", p.parse(s, "true,true|false,null", List.class, List.class, Boolean.class));
		assertObjectEquals("[['true','true'],['false',null]]", p.parse(s, "true,true|false,null", String[][].class));
		assertObjectEquals("[['true','true'],['false',null]]", p.parse(s, "true,true|false,null", List.class, List.class, String.class));
		assertObjectEquals("[['true','true'],['false',null]]", p.parse(s, "true,true|false,null", List.class, String[].class));
		assertObjectEquals("[[true,true],[false,null]]", p.parse(s, "true,true|false,null", Object[][].class));
		assertObjectEquals("[[true,true],[false,null]]", p.parse(s, "true,true|false,null", List.class, List.class, Object.class));
		assertObjectEquals("[[true,true],[false,null]]", p.parse(s, "true,true|false,null", List.class, Object[].class));
		assertObjectEquals("[['E1-true','E1-true'],['E1-false',null]]", p.parse(s, "true,true|false,null", E1[][].class));
		assertObjectEquals("[['E1-true','E1-true'],['E1-false',null]]", p.parse(s, "true,true|false,null", List.class, List.class, E1.class));
		assertObjectEquals("[['E1-true','E1-true'],['E1-false',null]]", p.parse(s, "true,true|false,null", List.class, E1[].class));
		assertObjectEquals("['E2-[true,true]','E2-[false,null]']", p.parse(s, "true,true|false,null", E2[].class));
		assertObjectEquals("['E2-[true,true]','E2-[false,null]']", p.parse(s, "true,true|false,null", List.class, E2.class));

		assertObjectEquals("[[true,true],[false]]", p.parse(s, "True,true|false", boolean[][].class));
		assertObjectEquals("[[true,true],[false]]", p.parse(s, "TRUE,true|false", boolean[][].class));
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
		private String f;
		public F3(Long in) {
			this.f = "F3-" + in.toString();
		}
		@Override
		public String toString() {
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
		HttpPartSchema s = schema("integer", "int32").build();
		assertObjectEquals("1", p.parse(s, "1", int.class));
		assertObjectEquals("1", p.parse(s, "1", Integer.class));
		assertObjectEquals("1", p.parse(s, "1", short.class));
		assertObjectEquals("1", p.parse(s, "1", Short.class));
		assertObjectEquals("1", p.parse(s, "1", long.class));
		assertObjectEquals("1", p.parse(s, "1", Long.class));
		assertObjectEquals("'1'", p.parse(s, "1", String.class));
		Object o = p.parse(s, "1", Object.class);
		assertObjectEquals("1", o);
		assertClass(Integer.class, o);
		assertObjectEquals("'F1-1'", p.parse(s,  "1", F1.class));
	}

	@Test
	public void f02_integerType_int32_2d() throws Exception {
		HttpPartSchema s = schema("array").items(schema("integer", "int32")).build();
		assertObjectEquals("[1,2]", p.parse(s, "1,2", int[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Integer[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Integer.class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", short[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Short[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Short.class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", long[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Long[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Long.class));
		assertObjectEquals("['1','2']", p.parse(s, "1,2", String[].class));
		assertObjectEquals("['1','2']", p.parse(s, "1,2", List.class, String.class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Object[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Object.class));
		assertObjectEquals("['F1-1','F1-2']", p.parse(s,  "1,2", F1[].class));
		assertObjectEquals("['F1-1','F1-2']", p.parse(s,  "1,2", List.class, F1.class));
		assertObjectEquals("'F2-[1,2]'", p.parse(s,  "1,2", F2.class));
	}

	@Test
	public void f03_integerType_int32_3d() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("pipes").items(schema("array").items(schema("integer", "int32"))).build();
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", int[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, int[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", Integer[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, Integer[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, List.class, Integer.class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", short[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, short[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", Short[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, Short[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, List.class, Short.class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", long[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, long[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", Long[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, Long[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, List.class, Long.class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", String[][].class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", List.class, String[].class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", List.class, List.class, String.class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", Object[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, Object[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, List.class, Object.class));
		assertObjectEquals("[['F1-1','F1-2'],['F1-3']]", p.parse(s,  "1,2|3", F1[][].class));
		assertObjectEquals("[['F1-1','F1-2'],['F1-3']]", p.parse(s,  "1,2|3", List.class, F1[].class));
		assertObjectEquals("[['F1-1','F1-2'],['F1-3']]", p.parse(s,  "1,2|3", List.class, List.class, F1.class));
		assertObjectEquals("['F2-[1,2]','F2-[3]']", p.parse(s, "1,2|3", F2[].class));
		assertObjectEquals("['F2-[1,2]','F2-[3]']", p.parse(s, "1,2|3", List.class, F2.class));
	}

	@Test
	public void f04_integerType_int64() throws Exception {
		HttpPartSchema s = schema("integer", "int64").build();
		assertObjectEquals("1", p.parse(s, "1", int.class));
		assertObjectEquals("1", p.parse(s, "1", Integer.class));
		assertObjectEquals("1", p.parse(s, "1", short.class));
		assertObjectEquals("1", p.parse(s, "1", Short.class));
		assertObjectEquals("1", p.parse(s, "1", long.class));
		assertObjectEquals("1", p.parse(s, "1", Long.class));
		assertObjectEquals("'1'", p.parse(s, "1", String.class));
		Object o = p.parse(s, "1", Object.class);
		assertObjectEquals("1", o);
		assertClass(Long.class, o);
		assertObjectEquals("'F3-1'", p.parse(s,  "1", F3.class));
	}

	@Test
	public void f05_integerType_int64_2d() throws Exception {
		HttpPartSchema s = schema("array").items(schema("integer", "int64")).build();
		assertObjectEquals("[1,2]", p.parse(s, "1,2", int[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Integer[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Integer.class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", short[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Short[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Short.class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", long[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Long[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Long.class));
		assertObjectEquals("['1','2']", p.parse(s, "1,2", String[].class));
		assertObjectEquals("['1','2']", p.parse(s, "1,2", List.class, String.class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Object[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Object.class));
		assertObjectEquals("['F3-1','F3-2']", p.parse(s,  "1,2", F3[].class));
		assertObjectEquals("['F3-1','F3-2']", p.parse(s,  "1,2", List.class, F3.class));
		assertObjectEquals("'F4-[1,2]'", p.parse(s,  "1,2", F4.class));
	}

	@Test
	public void f06_integerType_int64_3d() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("pipes").items(schema("array").items(schema("integer", "int64"))).build();
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", int[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, int[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", Integer[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, Integer[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, List.class, Integer.class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", short[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, short[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", Short[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, Short[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, List.class, Short.class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", long[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, long[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", Long[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, Long[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, List.class, Long.class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", String[][].class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", List.class, String[].class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", List.class, List.class, String.class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", Object[][].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, Object[].class));
		assertObjectEquals("[[1,2],[3]]", p.parse(s, "1,2|3", List.class, List.class, Object.class));
		assertObjectEquals("[['F3-1','F3-2'],['F3-3']]", p.parse(s,  "1,2|3", F3[][].class));
		assertObjectEquals("[['F3-1','F3-2'],['F3-3']]", p.parse(s,  "1,2|3", List.class, F3[].class));
		assertObjectEquals("[['F3-1','F3-2'],['F3-3']]", p.parse(s,  "1,2|3", List.class, List.class, F3.class));
		assertObjectEquals("['F4-[1,2]','F4-[3]']", p.parse(s, "1,2|3", F4[].class));
		assertObjectEquals("['F4-[1,2]','F4-[3]']", p.parse(s, "1,2|3", List.class, F4.class));
	}


	//-----------------------------------------------------------------------------------------------------------------
	// type = number
	//-----------------------------------------------------------------------------------------------------------------

	public static class G1 {
		private String f;
		public G1(Float in) {
			this.f = "G1-" + in.toString();
		}
		@Override
		public String toString() {
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
		private String f;
		public G3(Double in) {
			this.f = "G3-" + in.toString();
		}
		@Override
		public String toString() {
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
		HttpPartSchema s = schema("number", "float").build();
		assertObjectEquals("1.0", p.parse(s, "1", float.class));
		assertObjectEquals("1.0", p.parse(s, "1", Float.class));
		assertObjectEquals("1.0", p.parse(s, "1", double.class));
		assertObjectEquals("1.0", p.parse(s, "1", Double.class));
		assertObjectEquals("'1'", p.parse(s, "1", String.class));
		Object o =  p.parse(s, "1", Object.class);
		assertObjectEquals("1.0",o);
		assertClass(Float.class, o);
		assertObjectEquals("'G1-1.0'", p.parse(s,  "1", G1.class));
	}

	@Test
	public void g02_numberType_float_2d() throws Exception {
		HttpPartSchema s = schema("array").items(schema("number", "float")).build();
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", float[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", Float[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", List.class, Float.class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", double[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", Double[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", List.class, Double.class));
		assertObjectEquals("['1','2']", p.parse(s, "1,2", String[].class));
		assertObjectEquals("['1','2']", p.parse(s, "1,2", List.class, String.class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", Object[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", List.class, Object.class));
		assertObjectEquals("['G1-1.0','G1-2.0']", p.parse(s,  "1,2", G1[].class));
		assertObjectEquals("['G1-1.0','G1-2.0']", p.parse(s,  "1,2", List.class, G1.class));
		assertObjectEquals("'G2-[1.0,2.0]'", p.parse(s,  "1,2", G2.class));
	}

	@Test
	public void g03_numberType_float_3d() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("pipes").items(schema("array").items(schema("number", "float"))).build();
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", float[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, float[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", Float[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, Float[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, List.class, Float.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", double[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, double[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", Double[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, Double[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, List.class, Double.class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", String[][].class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", List.class, String[].class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", List.class, List.class, String.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", Object[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, Object[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, List.class, Object.class));
		assertObjectEquals("[['G1-1.0','G1-2.0'],['G1-3.0']]", p.parse(s,  "1,2|3", G1[][].class));
		assertObjectEquals("[['G1-1.0','G1-2.0'],['G1-3.0']]", p.parse(s,  "1,2|3", List.class, G1[].class));
		assertObjectEquals("[['G1-1.0','G1-2.0'],['G1-3.0']]", p.parse(s,  "1,2|3", List.class, List.class, G1.class));
		assertObjectEquals("['G2-[1.0,2.0]','G2-[3.0]']", p.parse(s, "1,2|3", G2[].class));
		assertObjectEquals("['G2-[1.0,2.0]','G2-[3.0]']", p.parse(s, "1,2|3", List.class, G2.class));
	}

	@Test
	public void g04_numberType_double() throws Exception {
		HttpPartSchema s = schema("number", "double").build();
		assertObjectEquals("1.0", p.parse(s, "1", float.class));
		assertObjectEquals("1.0", p.parse(s, "1", Float.class));
		assertObjectEquals("1.0", p.parse(s, "1", double.class));
		assertObjectEquals("1.0", p.parse(s, "1", Double.class));
		assertObjectEquals("'1'", p.parse(s, "1", String.class));
		Object o = p.parse(s, "1", Object.class);
		assertObjectEquals("1.0", o);
		assertClass(Double.class, o);
		assertObjectEquals("'G3-1.0'", p.parse(s,  "1", G3.class));
	}

	@Test
	public void g05_numberType_double_2d() throws Exception {
		HttpPartSchema s = schema("array").items(schema("number", "double")).build();
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", float[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", Float[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", List.class, Float.class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", double[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", Double[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", List.class, Double.class));
		assertObjectEquals("['1','2']", p.parse(s, "1,2", String[].class));
		assertObjectEquals("['1','2']", p.parse(s, "1,2", List.class, String.class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", Object[].class));
		assertObjectEquals("[1.0,2.0]", p.parse(s, "1,2", List.class, Object.class));
		assertObjectEquals("['G3-1.0','G3-2.0']", p.parse(s,  "1,2", G3[].class));
		assertObjectEquals("['G3-1.0','G3-2.0']", p.parse(s,  "1,2", List.class, G3.class));
		assertObjectEquals("'G4-[1.0,2.0]'", p.parse(s,  "1,2", G4.class));
	}

	@Test
	public void g06_numberType_double_3d() throws Exception {
		HttpPartSchema s = schema("array").collectionFormat("pipes").items(schema("array").items(schema("number", "double"))).build();
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", float[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, float[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", Float[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, Float[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, List.class, Float.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", double[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, double[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", Double[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, Double[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, List.class, Double.class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", String[][].class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", List.class, String[].class));
		assertObjectEquals("[['1','2'],['3']]", p.parse(s, "1,2|3", List.class, List.class, String.class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", Object[][].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, Object[].class));
		assertObjectEquals("[[1.0,2.0],[3.0]]", p.parse(s, "1,2|3", List.class, List.class, Object.class));
		assertObjectEquals("[['G3-1.0','G3-2.0'],['G3-3.0']]", p.parse(s,  "1,2|3", G3[][].class));
		assertObjectEquals("[['G3-1.0','G3-2.0'],['G3-3.0']]", p.parse(s,  "1,2|3", List.class, G3[].class));
		assertObjectEquals("[['G3-1.0','G3-2.0'],['G3-3.0']]", p.parse(s,  "1,2|3", List.class, List.class, G3.class));
		assertObjectEquals("['G4-[1.0,2.0]','G4-[3.0]']", p.parse(s, "1,2|3", G4[].class));
		assertObjectEquals("['G4-[1.0,2.0]','G4-[3.0]']", p.parse(s, "1,2|3", List.class, G4.class));
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
		assertObjectEquals("{f:1}", p.parse(s, "(f=1)", H1.class));
		assertObjectEquals("{f:1}", p.parse(s, "(f=1)", ObjectMap.class));
		Object o = p.parse(s, "(f=1)", Object.class);
		assertObjectEquals("{f:1}", o);
		assertClass(ObjectMap.class, o);
	}

	@Test
	public void h02_objectType_2d() throws Exception {
		HttpPartSchema s = schema("array").format("uon").items(schema("object")).build();
		assertObjectEquals("[{f:1},{f:2}]", p.parse(s, "@((f=1),(f=2))", H1[].class));
		assertObjectEquals("[{f:1},{f:2}]", p.parse(s, "@((f=1),(f=2))", List.class, H1.class));
		assertObjectEquals("[{f:1},{f:2}]", p.parse(s, "@((f=1),(f=2))", ObjectMap[].class));
		assertObjectEquals("[{f:1},{f:2}]", p.parse(s, "@((f=1),(f=2))", List.class, ObjectMap.class));
		assertObjectEquals("[{f:1},{f:2}]", p.parse(s, "@((f=1),(f=2))", Object[].class));
		assertObjectEquals("[{f:1},{f:2}]", p.parse(s, "@((f=1),(f=2))", List.class, Object.class));
		Object o = p.parse(s, "@((f=1),(f=2))", Object.class);
		assertObjectEquals("[{f:1},{f:2}]", o);
		assertClass(ObjectList.class, o);
	}

	@Test
	public void h03_objectType_3d() throws Exception {
		HttpPartSchema s = schema("array").format("uon").items(schema("array").items(schema("object"))).build();
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", H1[][].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, H1[].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, H1.class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", ObjectMap[][].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, ObjectMap[].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, ObjectMap.class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", Object[][].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, Object[].class));
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", List.class, List.class, Object.class));
		Object o =  p.parse(s, "@(@((f=1),(f=2)),@((f=3)))", Object.class);
		assertObjectEquals("[[{f:1},{f:2}],[{f:3}]]", o);
		assertClass(ObjectList.class, o);
	}

	public static class H2 {
		public Object f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f99;
	}

	@Test
	public void h04_objectType_simpleProperties() throws Exception {
		HttpPartSchema s = schema("object")
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
		String in = "(f1=foo,f2="+base64Encode(foob)+",f4=2012-12-21T12:34:56Z,f5="+toHex(foob)+",f6='"+toSpacedHex(foob)+"',f7=foo,f8=1,f9=1,f10=1,f11=1,f12=true,f99=1)";

		H2 h2 = p.parse(s, in, H2.class);
		assertObjectEquals("{f1:'foo',f2:[102,111,111],f4:'2012-12-21T12:34:56Z',f5:[102,111,111],f6:[102,111,111],f7:'foo',f8:1,f9:1,f10:1.0,f11:1.0,f12:true,f99:1}", h2);
		assertClass(String.class, h2.f1);
		assertClass(byte[].class, h2.f2);
		assertClass(GregorianCalendar.class, h2.f4);
		assertClass(byte[].class, h2.f5);
		assertClass(byte[].class, h2.f6);
		assertClass(String.class, h2.f7);
		assertClass(Integer.class, h2.f8);
		assertClass(Long.class, h2.f9);
		assertClass(Float.class, h2.f10);
		assertClass(Double.class, h2.f11);
		assertClass(Boolean.class, h2.f12);
		assertClass(Integer.class, h2.f99);

		ObjectMap om = p.parse(s, in, ObjectMap.class);
		assertObjectEquals("{f1:'foo',f2:[102,111,111],f4:'2012-12-21T12:34:56Z',f5:[102,111,111],f6:[102,111,111],f7:'foo',f8:1,f9:1,f10:1.0,f11:1.0,f12:true,f99:1}", om);
		assertClass(String.class, om.get("f1"));
		assertClass(byte[].class, om.get("f2"));
		assertClass(GregorianCalendar.class, om.get("f4"));
		assertClass(byte[].class, om.get("f5"));
		assertClass(byte[].class, om.get("f6"));
		assertClass(String.class, om.get("f7"));
		assertClass(Integer.class, om.get("f8"));
		assertClass(Long.class, om.get("f9"));
		assertClass(Float.class, om.get("f10"));
		assertClass(Double.class, om.get("f11"));
		assertClass(Boolean.class, om.get("f12"));
		assertClass(Integer.class, om.get("f99"));

		om = (ObjectMap)p.parse(s, in, Object.class);
		assertObjectEquals("{f1:'foo',f2:[102,111,111],f4:'2012-12-21T12:34:56Z',f5:[102,111,111],f6:[102,111,111],f7:'foo',f8:1,f9:1,f10:1.0,f11:1.0,f12:true,f99:1}", om);
		assertClass(String.class, om.get("f1"));
		assertClass(byte[].class, om.get("f2"));
		assertClass(GregorianCalendar.class, om.get("f4"));
		assertClass(byte[].class, om.get("f5"));
		assertClass(byte[].class, om.get("f6"));
		assertClass(String.class, om.get("f7"));
		assertClass(Integer.class, om.get("f8"));
		assertClass(Long.class, om.get("f9"));
		assertClass(Float.class, om.get("f10"));
		assertClass(Double.class, om.get("f11"));
		assertClass(Boolean.class, om.get("f12"));
		assertClass(Integer.class, om.get("f99"));
	}

	@Test
	public void h05_objectType_arrayProperties() throws Exception {
		HttpPartSchema s = schema("object")
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
		String in = "(f1=foo,f2="+base64Encode(foob)+",f4=2012-12-21T12:34:56Z,f5="+toHex(foob)+",f6='"+toSpacedHex(foob)+"',f7=foo,f8=1,f9=1,f10=1,f11=1,f12=true,f99=1)";

		H2 h2 = p.parse(s, in, H2.class);
		assertObjectEquals("{f1:['foo'],f2:[[102,111,111]],f4:['2012-12-21T12:34:56Z'],f5:[[102,111,111]],f6:[[102,111,111]],f7:['foo'],f8:[1],f9:[1],f10:[1.0],f11:[1.0],f12:[true],f99:[1]}", h2);

		ObjectMap om = p.parse(s, in, ObjectMap.class);
		assertObjectEquals("{f1:['foo'],f2:[[102,111,111]],f4:['2012-12-21T12:34:56Z'],f5:[[102,111,111]],f6:[[102,111,111]],f7:['foo'],f8:[1],f9:[1],f10:[1.0],f11:[1.0],f12:[true],f99:[1]}", om);

		om = (ObjectMap)p.parse(s, in, Object.class);
		assertObjectEquals("{f1:['foo'],f2:[[102,111,111]],f4:['2012-12-21T12:34:56Z'],f5:[[102,111,111]],f6:[[102,111,111]],f7:['foo'],f8:[1],f9:[1],f10:[1.0],f11:[1.0],f12:[true],f99:[1]}", om);
	}

	@Test
	public void h06_objectType_arrayProperties_pipes() throws Exception {
		HttpPartSchema s = schema("object")
			.property("f1", schema("array").collectionFormat("pipes").items(schema("string")))
			.property("f2", schema("array").collectionFormat("pipes").items(schema("string", "byte")))
			.property("f4", schema("array").collectionFormat("pipes").items(schema("string", "date-time")))
			.property("f5", schema("array").collectionFormat("pipes").items(schema("string", "binary")))
			.property("f6", schema("array").collectionFormat("pipes").items(schema("string", "binary-spaced")))
			.property("f7", schema("array").collectionFormat("pipes").items(schema("string", "uon")))
			.property("f8", schema("array").collectionFormat("pipes").items(schema("integer")))
			.property("f9", schema("array").collectionFormat("pipes").items(schema("integer", "int64")))
			.property("f10", schema("array").collectionFormat("pipes").items(schema("number")))
			.property("f11", schema("array").collectionFormat("pipes").items(schema("number", "double")))
			.property("f12", schema("array").collectionFormat("pipes").items(schema("boolean")))
			.additionalProperties(schema("array").collectionFormat("pipes").items(schema("integer")))
			.build();

		byte[] foob = "foo".getBytes(), barb = "bar".getBytes();
		String in = "(f1=foo|bar,f2="+base64Encode(foob)+"|"+base64Encode(barb)+",f4=2012-12-21T12:34:56Z|2012-12-21T12:34:56Z,f5="+toHex(foob)+"|"+toHex(barb)+",f6='"+toSpacedHex(foob)+"|"+toSpacedHex(barb)+"',f7=foo|bar,f8=1|2,f9=1|2,f10=1|2,f11=1|2,f12=true|true,f99=1|2)";

		H2 h2 = p.parse(s, in, H2.class);
		assertObjectEquals("{f1:['foo','bar'],f2:[[102,111,111],[98,97,114]],f4:['2012-12-21T12:34:56Z','2012-12-21T12:34:56Z'],f5:[[102,111,111],[98,97,114]],f6:[[102,111,111],[98,97,114]],f7:['foo','bar'],f8:[1,2],f9:[1,2],f10:[1.0,2.0],f11:[1.0,2.0],f12:[true,true],f99:[1,2]}", h2);

		ObjectMap om = p.parse(s, in, ObjectMap.class);
		assertObjectEquals("{f1:['foo','bar'],f2:[[102,111,111],[98,97,114]],f4:['2012-12-21T12:34:56Z','2012-12-21T12:34:56Z'],f5:[[102,111,111],[98,97,114]],f6:[[102,111,111],[98,97,114]],f7:['foo','bar'],f8:[1,2],f9:[1,2],f10:[1.0,2.0],f11:[1.0,2.0],f12:[true,true],f99:[1,2]}", om);

		om = (ObjectMap)p.parse(s, in, Object.class);
		assertObjectEquals("{f1:['foo','bar'],f2:[[102,111,111],[98,97,114]],f4:['2012-12-21T12:34:56Z','2012-12-21T12:34:56Z'],f5:[[102,111,111],[98,97,114]],f6:[[102,111,111],[98,97,114]],f7:['foo','bar'],f8:[1,2],f9:[1,2],f10:[1.0,2.0],f11:[1.0,2.0],f12:[true,true],f99:[1,2]}", om);
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