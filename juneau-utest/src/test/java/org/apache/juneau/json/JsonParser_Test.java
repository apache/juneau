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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

class JsonParser_Test extends SimpleTestBase {

	private static final JsonParser p = JsonParser.DEFAULT;
	private static final JsonParser sp = JsonParser.DEFAULT_STRICT;

	//====================================================================================================
	// Test invalid input
	//====================================================================================================
	@Test void a01_invalidJson() {
		assertThrows(ParseException.class, ()->p.parse("{\na:1,\nb:xxx\n}", Object.class));
	}

	@Test void a02_nonExistentAttribute() throws Exception {
		var json = "{foo:,bar:}";
		var m = p.parse(json, JsonMap.class);
		assertEquals("{foo:null,bar:null}", m.toString());
	}

	@Test void a03_nonStringAsString() throws Exception {
		var json = "123";
		var s = p.parse(json, String.class);

		// Strict mode does not allow unquoted values.
		assertThrowsWithMessage(Exception.class, "Did not find quote character", ()->sp.parse("123", String.class));

		assertEquals("123", s);

		json = " 123 ";
		// Strict mode does not allow unquoted values.
		assertThrowsWithMessage(Exception.class, "Did not find quote character", ()->sp.parse(" 123 ", String.class));

		s = p.parse(json, String.class);
		assertEquals("123", s);

		json = "{\"fa\":123}";
		assertThrowsWithMessage(Exception.class, "Did not find quote character", ()->sp.parse("{\"fa\":123}", A.class));

		var a = p.parse(json, A.class);
		assertEquals("123", a.fa);

		json = " { \"fa\" : 123 } ";
		assertThrowsWithMessage(Exception.class, "Did not find quote character", ()->sp.parse(" { \"fa\" : 123 } ", A.class));

		a = p.parse(json, A.class);
		assertEquals("123", a.fa);

		assertThrowsWithMessage(Exception.class, "Invalid quote character", ()->sp.parse("'123'", String.class));
	}

	public static class A {
		public String fa;
	}

	@Test void a04_strictMode() {
		var p2 = sp;
		assertThrowsWithMessage(Exception.class, "Missing value detected.", ()->p2.parse("{\"foo\":,\"bar\":}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "Invalid quote character", ()->p2.parse("{\"foo\":'bar'}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "Invalid quote character", ()->p2.parse("{'foo':\"bar\"}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "Unquoted attribute detected.", ()->p2.parse("{foo:\"bar\"}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "String concatenation detected.", ()->p2.parse("{\"foo\":\"bar\"+\"baz\"}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "String concatenation detected.", ()->p2.parse("{\"foo\":\"bar\" + \"baz\"}", JsonMap.class));
		assertThrowsWithMessage(Exception.class, "Javascript comment detected.", ()->p2.parse("{\"foo\":/*comment*/\"bar\"}", JsonMap.class));
	}

	/**
	 * JSON numbers and booleans should be representable as strings and converted accordingly.
	 */
	@Test void a05_primitivesAsStrings() throws Exception {
		var p2 = JsonParser.DEFAULT;
		var s = Json5Serializer.DEFAULT;

		var json = "{f01:'1',f02:'1',f03:'true',f04:'true',f05:'1',f06:'1',f07:'1',f08:'1',f09:'1',f10:'1'}";
		var b = p2.parse(json, B.class);
		assertEquals("{f01:1,f02:1,f03:true,f04:true,f05:1.0,f06:1.0,f07:1,f08:1,f09:1,f10:1}", s.toString(b));

		json = "{f01:'',f02:'',f03:'',f04:'',f05:'',f06:'',f07:'',f08:'',f09:'',f10:''}";
		b = p2.parse(json, B.class);
		assertEquals("{f01:0,f02:0,f03:false,f04:false,f05:0.0,f06:0.0,f07:0,f08:0,f09:0,f10:0}", s.toString(b));
	}

	public static class B {
		public int f01;
		public Integer f02;
		public boolean f03;
		public Boolean f04;
		public float f05;
		public Float f06;
		public long f07;
		public Long f08;
		public byte f09;
		public Byte f10;
	}

	//====================================================================================================
	// testInvalidJsonNumbers
	// Lax parser allows octal and hexadecimal numbers.  Strict parser does not.
	//====================================================================================================
	@Test void a06_invalidJsonNumbers() throws Exception {
		var p1 = JsonParser.DEFAULT;
		var p2 = JsonParser.DEFAULT_STRICT;

		// Lax allows blank strings interpreted as 0, strict does not.
		var s = "\"\"";
		var r = p1.parse(s, Number.class);
		assertEquals(0, r.intValue());
		assertTrue(r instanceof Integer);
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p2.parse("\"\"", Number.class));

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
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p2.parse("0123", Number.class));
		s = "-0123";
		r = p1.parse(s, Number.class);
		assertEquals(-0123, r.intValue());
		assertTrue(r instanceof Integer);
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p2.parse("-0123", Number.class));

		// Lax allows 0x123 and -0x123, strict does not.
		s = "0x123";
		r = p1.parse(s, Number.class);
		assertEquals(0x123, r.intValue());
		assertTrue(r instanceof Integer);
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p2.parse("0x123", Number.class));
		s = "-0x123";
		r = p1.parse(s, Number.class);
		assertEquals(-0x123, r.intValue());
		assertTrue(r instanceof Integer);
		assertThrowsWithMessage(Exception.class, "Invalid JSON number", ()->p2.parse("-0x123", Number.class));
	}

	//====================================================================================================
	// testUnquotedStrings
	// Lax parser allows unquoted strings if POJO can be converted from a string.
	//====================================================================================================
	@Test void a07_unquotedStrings() throws Exception {
		var p1 = JsonParser.DEFAULT;
		var p2 = JsonParser.DEFAULT_STRICT;

		var s = "foobar";
		var c = p1.parse(s, C.class);
		assertEquals("f=foobar", c.toString());

		assertThrows(ParseException.class, ()->p2.parse(s, C.class));
	}

	public static class C {
		String f;
		public static C valueOf(String s) {
			var c = new C();
			c.f = s;
			return c;
		}
		@Override /* Object */
		public String toString() {
			return "f="+f;
		}
	}

	//====================================================================================================
	// testStreamsAutoClose
	// Validates PARSER_autoCloseStreams.
	//====================================================================================================
	@Test void a08_streamsAutoClose() throws Exception {
		var p2 = JsonParser.DEFAULT.copy().autoCloseStreams().build();
		var r = reader("{foo:'bar'}{baz:'qux'}");

		var x = p2.parse(r, JsonMap.class);
		assertBean(x, "foo", "bar");
		assertThrowsWithMessage(Exception.class, "Reader is closed", ()->p2.parse(r, JsonMap.class));
	}

	//====================================================================================================
	// testMultipleObjectsInStream
	// Validates that readers are not closed so that we can read streams of POJOs.
	//====================================================================================================
	@Test void a09_multipleObjectsInStream() throws Exception {
		var p2 = JsonParser.create().unbuffered().build();
		var r = reader("{foo:'bar'}{baz:'qux'}");

		var x = (Object)p2.parse(r, JsonMap.class);
		assertBean(x, "foo", "bar");
		x = p2.parse(r, JsonMap.class);
		assertBean(x, "baz", "qux");

		r = reader("[123][456]");
		x = p2.parse(r, JsonList.class);
		assertList(x, "123");
		x = p2.parse(r, JsonList.class);
		assertList(x, "456");
	}

	private Reader reader(String in) {
		return new CloseableStringReader(in);
	}
}
