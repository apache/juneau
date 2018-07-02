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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.junit.*;

public class OpenApiPartParserTest {

	static OpenApiPartParser p = OpenApiPartParser.DEFAULT;

	//-----------------------------------------------------------------------------------------------------------------
	// Input validations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_inputValidations_nullInput() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().build();
		assertNull(p.parse(s, null, String.class));

		s = HttpPartSchema.create().required(false).build();
		assertNull(p.parse(s, null, String.class));

		s = HttpPartSchema.create().required().build();
		try {
			p.parse(s, null, String.class);
			fail();
		} catch (Exception e) {
			assertEquals("No value specified.", e.getMessage());
		}

		s = HttpPartSchema.create().required(true).build();
		try {
			p.parse(s, null, String.class);
			fail();
		} catch (Exception e) {
			assertEquals("No value specified.", e.getMessage());
		}
	}

	@Test
	public void a02_inputValidations_emptyInput() throws Exception {

		HttpPartSchema s = HttpPartSchema.create().allowEmptyValue().build();
		assertEquals("", p.parse(s, "", String.class));

		s = HttpPartSchema.create().allowEmptyValue().build();
		assertEquals("", p.parse(s, "", String.class));

		s = HttpPartSchema.create().allowEmptyValue(false).build();
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
		HttpPartSchema s = HttpPartSchema.create().pattern("x.*").allowEmptyValue().build();
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
		s = HttpPartSchema.create().pattern("").allowEmptyValue().build();
		assertEquals("x", p.parse(s, "x", String.class));
		s = HttpPartSchema.create().pattern(null).allowEmptyValue().build();
		assertEquals("x", p.parse(s, "x", String.class));
	}

	@Test
	public void a04_inputValidations_enum() throws Exception {
		HttpPartSchema s = HttpPartSchema.create()._enum("foo").allowEmptyValue().build();

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

		s = HttpPartSchema.create()._enum((Set<String>)null).build();
		assertEquals("foo", p.parse(s, "foo", String.class));
		s = HttpPartSchema.create()._enum((Set<String>)null).allowEmptyValue().build();
		assertEquals("foo", p.parse(s, "foo", String.class));

		s = HttpPartSchema.create()._enum("foo","foo").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
	}

	@Test
	public void a05_inputValidations_minMaxLength() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().minLength(1l).maxLength(2l).allowEmptyValue().build();

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
			s = HttpPartSchema.create().minLength(2l).maxLength(1l).build();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("maxLength cannot be less than minLength."));
		}

		try {
			s = HttpPartSchema.create().minLength(-1l).build();
			fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("minLength cannot be less than zero."));
		}

		try {
			s = HttpPartSchema.create().maxLength(-1l).build();
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

	//-----------------------------------------------------------------------------------------------------------------
	// type = string
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_stringType_simple() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("string").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
	}

	@Test
	public void c02_stringType_default() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("string")._default("x").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
		assertEquals("x", p.parse(s, null, String.class));
	}

	public static class C3 {
		private String f;
		public C3(byte[] b) {
			f = new String(b);
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test
	public void c03_stringType_byteFormat() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("string").format("byte").build();
		String in = StringUtils.base64Encode("foo".getBytes());
		assertEquals("foo", p.parse(s, in, String.class));
		assertEquals("foo", IOUtils.read(p.parse(s, in, InputStream.class)));
		assertEquals("foo", IOUtils.read(p.parse(s, in, Reader.class)));
		assertEquals("foo", p.parse(s, in, C3.class).toString());
	}

	@Test
	public void c04_stringType_binaryFormat() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("string").format("binary").build();
		String in = StringUtils.toHex("foo".getBytes());
		assertEquals("foo", p.parse(s, in, String.class));
		assertEquals("foo", IOUtils.read(p.parse(s, in, InputStream.class)));
		assertEquals("foo", IOUtils.read(p.parse(s, in, Reader.class)));
		assertEquals("foo", p.parse(s, in, C3.class).toString());
	}

	@Test
	public void c05_stringType_binarySpacedFormat() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("string").format("binary-spaced").build();
		String in = StringUtils.toSpacedHex("foo".getBytes());
		assertEquals("foo", p.parse(s, in, String.class));
		assertEquals("foo", IOUtils.read(p.parse(s, in, InputStream.class)));
		assertEquals("foo", IOUtils.read(p.parse(s, in, Reader.class)));
		assertEquals("foo", p.parse(s, in, C3.class).toString());
	}

	@Test
	public void c06_stringType_dateFormat() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("string").format("date").build();
		String in = "2012-12-21";
		assertTrue(p.parse(s, in, String.class).contains("2012"));
		assertTrue(p.parse(s, in, Date.class).toString().contains("2012"));
		assertEquals(2012, p.parse(s, in, Calendar.class).get(Calendar.YEAR));
		assertEquals(2012, p.parse(s, in, GregorianCalendar.class).get(Calendar.YEAR));
	}

	@Test
	public void c07_stringType_dateTimeFormat() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("string").format("date-time").build();
		String in = "2012-12-21T12:34:56.789";
		assertTrue(p.parse(s, in, String.class).contains("2012"));
		assertTrue(p.parse(s, in, Date.class).toString().contains("2012"));
		assertEquals(2012, p.parse(s, in, Calendar.class).get(Calendar.YEAR));
		assertEquals(2012, p.parse(s, in, GregorianCalendar.class).get(Calendar.YEAR));
	}

	public static class C8 {
		private String f;
		public C8(String s) {
			f = s;
		}
		@Override
		public String toString() {
			return f;
		}
	}

	@Test
	public void c08_stringType_uonFormat() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("string").format("uon").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
		assertEquals("foo", p.parse(s, "'foo'", String.class));
		assertEquals("foo", p.parse(s, "'foo'", C8.class).toString());
		assertEquals("C8", p.parse(s, "'foo'", C8.class).getClass().getSimpleName());
		// UonPartParserTest should handle all other cases.
	}

	@Test
	public void c08_stringType_noneFormat() throws Exception {
		// If no format is specified, then we should transform directly from a string.
		HttpPartSchema s = HttpPartSchema.create().type("string").build();
		assertEquals("foo", p.parse(s, "foo", String.class));
		assertEquals("'foo'", p.parse(s, "'foo'", String.class));
		assertEquals("foo", p.parse(s, "foo", C8.class).toString());
		assertEquals("C8", p.parse(s, "foo", C8.class).getClass().getSimpleName());
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
		HttpPartSchema s = HttpPartSchema.create().type("array").collectionFormat("csv").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", ObjectList.class));
	}

	@Test
	public void d02_arrayType_collectionFormatPipes() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("array").collectionFormat("pipes").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", Object[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", List.class, Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo|bar", ObjectList.class));
	}

	@Test
	public void d03_arrayType_collectionFormatSsv() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("array").collectionFormat("ssv").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", Object[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", List.class, Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo bar", ObjectList.class));
	}

	@Test
	public void d04_arrayType_collectionFormatTsv() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("array").collectionFormat("tsv").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", Object[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", List.class, Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo\tbar", ObjectList.class));
	}

	@Test
	public void d05_arrayType_collectionFormatUon() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("array").collectionFormat("uon").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", Object[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", ObjectList.class));
	}

	@Test
	public void d06a_arrayType_collectionFormatNone() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("array").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "foo,bar", Object.class));
	}

	@Test
	public void d06b_arrayType_collectionFormatNone_autoDetectUon() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("array").build();
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", String[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", Object[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", D[].class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, String.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, Object.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", List.class, D.class));
		assertObjectEquals("['foo','bar']", p.parse(s, "@(foo,bar)", Object.class));
	}

	@Test
	public void d07_arrayType_collectionFormatMulti() throws Exception {
		// collectionFormat=multi should not do any sort of splitting.
		HttpPartSchema s = HttpPartSchema.create().type("array").collectionFormat("multi").build();
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", String[].class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", Object[].class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", D[].class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", List.class, String.class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", List.class, Object.class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", List.class, D.class));
		assertObjectEquals("['foo,bar']", p.parse(s, "foo,bar", Object.class));
	}

	@Test
	public void d08_arrayType_collectionFormatCsvAndPipes() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("array").collectionFormat("pipes").items(HttpPartSchema.create().type("array").collectionFormat("csv")).build();
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", String[][].class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", Object[][].class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", D[][].class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", List.class, List.class, String.class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", List.class, List.class, Object.class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", List.class, List.class, D.class));
		assertObjectEquals("[['foo','bar'],['baz','qux']]", p.parse(s, "foo,bar|baz,qux", Object.class));
	}

	@Test
	public void d09_arrayType_itemsInteger() throws Exception {
		HttpPartSchema s = HttpPartSchema.create().type("array").collectionFormat("csv").items(HttpPartSchema.create().type("integer")).build();
		assertObjectEquals("[1,2]", p.parse(s, "1,2", int[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Integer[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Object[].class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Integer.class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", List.class, Object.class));
		assertObjectEquals("[1,2]", p.parse(s, "1,2", Object.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// type = boolean
	//-----------------------------------------------------------------------------------------------------------------

//	case BOOLEAN: {
//	if (type.isObject())
//		type = (ClassMeta<T>)getClassMeta(Boolean.class);
//	return super.parse(partType, schema, in, type);
//}

	//-----------------------------------------------------------------------------------------------------------------
	// type = integer
	//-----------------------------------------------------------------------------------------------------------------


//				case INTEGER: {
//					if (type.isObject()) {
//						switch (schema.getFormat()) {
//							case INT64:
//								type = (ClassMeta<T>)getClassMeta(Long.class);
//								break;
//							default:
//								type = (ClassMeta<T>)getClassMeta(Integer.class);
//
//						}
//					}
//					return super.parse(partType, schema, in, type);
//				}

	//-----------------------------------------------------------------------------------------------------------------
	// type = number
	//-----------------------------------------------------------------------------------------------------------------
//				case NUMBER: {
//					if (type.isObject()) {
//						switch (schema.getFormat()) {
//							case DOUBLE:
//								type = (ClassMeta<T>)getClassMeta(Double.class);
//								break;
//							default:
//								type = (ClassMeta<T>)getClassMeta(Float.class);
//						}
//					}
//					return super.parse(partType, schema, in, type);
//				}

	//-----------------------------------------------------------------------------------------------------------------
	// type = object
	//-----------------------------------------------------------------------------------------------------------------
//				case OBJECT: {
//					if (type.isObject())
//						type = (ClassMeta<T>)getClassMeta(ObjectMap.class);
//					switch (schema.getFormat()) {
//						default:
//							return super.parse(partType, schema, in, type);
//					}
//				}

	//-----------------------------------------------------------------------------------------------------------------
	// type = file
	//-----------------------------------------------------------------------------------------------------------------
//				case FILE: {
//					throw new ParseException("File part not supported.");
//				}

	//-----------------------------------------------------------------------------------------------------------------
	// type = none
	//-----------------------------------------------------------------------------------------------------------------
//				case NONE: {
//					throw new ParseException("Invalid type.");
//				}
//			}
//		}

}