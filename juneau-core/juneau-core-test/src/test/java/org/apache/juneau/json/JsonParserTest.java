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
package org.apache.juneau.json;

import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@SuppressWarnings({"javadoc"})
public class JsonParserTest {

	private static final JsonParser p = JsonParser.DEFAULT;
	private static final JsonParser sp = JsonParser.DEFAULT_STRICT;


	//====================================================================================================
	// Test invalid input
	//====================================================================================================
	@Test
	public void testInvalidJson() {
		try {
			p.parse("{\na:1,\nb:xxx\n}", Object.class);
			fail("Exception expected.");
		} catch (ParseException e) {}
	}

	@Test
	public void testNonExistentAttribute() throws Exception {
		String json = "{foo:,bar:}";
		ObjectMap m = p.parse(json, ObjectMap.class);
		assertEquals("{foo:null,bar:null}", m.toString());
	}

	@Test
	public void testNonStringAsString() throws Exception {
		String json = "123";
		String s;

		// Strict mode does not allow unquoted values.
		try {
			sp.parse(json, String.class);
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Did not find quote character"));
		}

		s = p.parse(json, String.class);
		assertEquals("123", s);

		json = " 123 ";
		// Strict mode does not allow unquoted values.
		try {
			sp.parse(json, String.class);
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Did not find quote character"));
		}

		s = p.parse(json, String.class);
		assertEquals("123", s);

		json = "{\"fa\":123}";
		try {
			sp.parse(json, A.class);
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Did not find quote character"));
		}

		A a = p.parse(json, A.class);
		assertEquals("123", a.fa);

		json = " { \"fa\" : 123 } ";
		try {
			sp.parse(json, A.class);
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Did not find quote character"));
		}

		a = p.parse(json, A.class);
		assertEquals("123", a.fa);

		json = "'123'";
		try {
			sp.parse(json, String.class);
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Invalid quote character"));
		}
	}

	public static class A {
		public String fa;
	}

	@Test
	public void testStrictMode() throws Exception {
		JsonParser p = sp;

		// Missing attribute values.
		String json = "{\"foo\":,\"bar\":}";
		try {
			p.parse(json, ObjectMap.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertEquals("Parse exception occurred at {currentClass:'Object',line:1,column:7}.  Missing value detected.", e.getRootCause().getMessage());
		}

		// Single quoted values.
		json = "{\"foo\":'bar'}";
		try {
			p.parse(json, ObjectMap.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertEquals("Parse exception occurred at {currentClass:'Object',line:1,column:8}.  Invalid quote character \"'\" being used.", e.getRootCause().getMessage());
		}

		// Single quoted attribute name.
		json = "{'foo':\"bar\"}";
		try {
			p.parse(json, ObjectMap.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertEquals("Parse exception occurred at {currentClass:'ObjectMap<String,Object>',line:1,column:2}.  Invalid quote character \"'\" being used.", e.getRootCause().getMessage());
		}

		// Unquoted attribute name.
		json = "{foo:\"bar\"}";
		try {
			p.parse(json, ObjectMap.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertEquals("Parse exception occurred at {currentClass:'ObjectMap<String,Object>',line:1,column:1}.  Unquoted attribute detected.", e.getRootCause().getMessage());
		}

		// Concatenated string
		json = "{\"foo\":\"bar\"+\"baz\"}";
		try {
			p.parse(json, ObjectMap.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertEquals("Parse exception occurred at {currentClass:'Object',line:1,column:12}.  String concatenation detected.", e.getRootCause().getMessage());
		}

		// Concatenated string 2
		json = "{\"foo\":\"bar\" + \"baz\"}";
		try {
			p.parse(json, ObjectMap.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertEquals("Parse exception occurred at {currentClass:'Object',line:1,column:13}.  String concatenation detected.", e.getRootCause().getMessage());
		}

		json = "{\"foo\":/*comment*/\"bar\"}";
		try {
			p.parse(json, ObjectMap.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertEquals("Parse exception occurred at {currentClass:'ObjectMap<String,Object>',line:1,column:8}.  Javascript comment detected.", e.getRootCause().getMessage());
		}
	}

	/**
	 * JSON numbers and booleans should be representable as strings and converted accordingly.
	 */
	@Test
	public void testPrimitivesAsStrings() throws Exception {
		String json;
		ReaderParser p = JsonParser.DEFAULT;
		WriterSerializer s = JsonSerializer.DEFAULT_LAX;

		json = "{f1:'1',f2:'1',f3:'true',f4:'true',f5:'1',f6:'1',f7:'1',f8:'1',f9:'1',f10:'1'}";
		B b = p.parse(json, B.class);
		assertEquals("{f1:1,f2:1,f3:true,f4:true,f5:1.0,f6:1.0,f7:1,f8:1,f9:1,f10:1}", s.toString(b));

		json = "{f1:'',f2:'',f3:'',f4:'',f5:'',f6:'',f7:'',f8:'',f9:'',f10:''}";
		b = p.parse(json, B.class);
		assertEquals("{f1:0,f2:0,f3:false,f4:false,f5:0.0,f6:0.0,f7:0,f8:0,f9:0,f10:0}", s.toString(b));
	}

	public static class B {
		public int f1;
		public Integer f2;
		public boolean f3;
		public Boolean f4;
		public float f5;
		public Float f6;
		public long f7;
		public Long f8;
		public byte f9;
		public Byte f10;
	}

	//====================================================================================================
	// testInvalidJsonNumbers
	// Lax parser allows octal and hexadecimal numbers.  Strict parser does not.
	//====================================================================================================
	@Test
	public void testInvalidJsonNumbers() throws Exception {
		JsonParser p1 = JsonParser.DEFAULT;
		JsonParser p2 = JsonParser.DEFAULT_STRICT;
		Number r;

		// Lax allows blank strings interpreted as 0, strict does not.
		String s = "\"\"";
		r = p1.parse(s, Number.class);
		assertEquals(0, r.intValue());
		assertTrue(r instanceof Integer);
		try {
			r = p2.parse(s, Number.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertTrue(e.getMessage().contains("Invalid JSON number"));
		}

		// Either should allow 0 or -0.
		s = "0";
		r = p1.parse(s, Number.class);
		assertEquals(0, r.intValue());
		assertTrue(r instanceof Integer);
		r = p2.parse(s, Number.class);
		assertEquals(0, r.intValue());
		assertTrue(r instanceof Integer);

		s = "-0";
		r = p1.parse(s, Number.class);
		assertEquals(0, r.intValue());
		assertTrue(r instanceof Integer);
		r = p2.parse(s, Number.class);
		assertEquals(0, r.intValue());
		assertTrue(r instanceof Integer);

		// Lax allows 0123 and -0123, strict does not.
		s = "0123";
		r = p1.parse(s, Number.class);
		assertEquals(0123, r.intValue());
		assertTrue(r instanceof Integer);
		try {
			r = p2.parse(s, Number.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertTrue(e.getMessage().contains("Invalid JSON number"));
		}
		s = "-0123";
		r = p1.parse(s, Number.class);
		assertEquals(-0123, r.intValue());
		assertTrue(r instanceof Integer);
		try {
			r = p2.parse(s, Number.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertTrue(e.getMessage().contains("Invalid JSON number"));
		}

		// Lax allows 0x123 and -0x123, strict does not.
		s = "0x123";
		r = p1.parse(s, Number.class);
		assertEquals(0x123, r.intValue());
		assertTrue(r instanceof Integer);
		try {
			r = p2.parse(s, Number.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertTrue(e.getMessage().contains("Invalid JSON number"));
		}
		s = "-0x123";
		r = p1.parse(s, Number.class);
		assertEquals(-0x123, r.intValue());
		assertTrue(r instanceof Integer);
		try {
			r = p2.parse(s, Number.class);
			fail("Exception expected");
		} catch (ParseException e) {
			assertTrue(e.getMessage().contains("Invalid JSON number"));
		}
	}

	//====================================================================================================
	// testUnquotedStrings
	// Lax parser allows unquoted strings if POJO can be converted from a string.
	//====================================================================================================
	@Test
	public void testUnquotedStrings() throws Exception {
		JsonParser p1 = JsonParser.DEFAULT;
		JsonParser p2 = JsonParser.DEFAULT_STRICT;

		String s = "foobar";
		C c = p1.parse(s, C.class);
		assertEquals("f=foobar", c.toString());

		try {
			p2.parse(s, C.class);
			fail("Exception expected");
		} catch (ParseException e) {
			// OK
		}
	}

	public static class C {
		String f;
		public static C valueOf(String s) {
			C c = new C();
			c.f = s;
			return c;
		}
		@Override /* Object */
		public String toString() {
			return "f="+f;
		}
	}
}
