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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class JsonParserTest {

	private static final JsonParser p = JsonParser.DEFAULT;
	private static final JsonParser sp = JsonParser.DEFAULT_STRICT;


	//====================================================================================================
	// Test invalid input
	//====================================================================================================
	@Test
	public void testInvalidJson() {
		assertThrown(()->p.parse("{\na:1,\nb:xxx\n}", Object.class)).isType(ParseException.class);
	}

	@Test
	public void testNonExistentAttribute() throws Exception {
		String json = "{foo:,bar:}";
		JsonMap m = p.parse(json, JsonMap.class);
		assertEquals("{foo:null,bar:null}", m.toString());
	}

	@Test
	public void testNonStringAsString() throws Exception {
		String json = "123";
		String s;

		// Strict mode does not allow unquoted values.
		assertThrown(()->sp.parse("123", String.class)).asMessage().isContains("Did not find quote character");

		s = p.parse(json, String.class);
		assertEquals("123", s);

		json = " 123 ";
		// Strict mode does not allow unquoted values.
		assertThrown(()->sp.parse(" 123 ", String.class)).asMessage().isContains("Did not find quote character");

		s = p.parse(json, String.class);
		assertEquals("123", s);

		json = "{\"fa\":123}";
		assertThrown(()->sp.parse("{\"fa\":123}", A.class)).asMessage().isContains("Did not find quote character");

		A a = p.parse(json, A.class);
		assertEquals("123", a.fa);

		json = " { \"fa\" : 123 } ";
		assertThrown(()->sp.parse(" { \"fa\" : 123 } ", A.class)).asMessage().isContains("Did not find quote character");

		a = p.parse(json, A.class);
		assertEquals("123", a.fa);

		json = "'123'";
		assertThrown(()->sp.parse("'123'", String.class)).asMessage().isContains("Invalid quote character");
	}

	public static class A {
		public String fa;
	}

	@Test
	public void testStrictMode() {
		JsonParser p2 = sp;
		assertThrown(()->p2.parse("{\"foo\":,\"bar\":}", JsonMap.class)).asMessage().isContains("Missing value detected.");
		assertThrown(()->p2.parse("{\"foo\":'bar'}", JsonMap.class)).asMessage().isContains("Invalid quote character");
		assertThrown(()->p2.parse("{'foo':\"bar\"}", JsonMap.class)).asMessage().isContains("Invalid quote character");
		assertThrown(()->p2.parse("{foo:\"bar\"}", JsonMap.class)).asMessage().isContains("Unquoted attribute detected.");
		assertThrown(()->p2.parse("{\"foo\":\"bar\"+\"baz\"}", JsonMap.class)).asMessage().isContains("String concatenation detected.");
		assertThrown(()->p2.parse("{\"foo\":\"bar\" + \"baz\"}", JsonMap.class)).asMessage().isContains("String concatenation detected.");
		assertThrown(()->p2.parse("{\"foo\":/*comment*/\"bar\"}", JsonMap.class)).asMessage().isContains("Javascript comment detected.");
	}

	/**
	 * JSON numbers and booleans should be representable as strings and converted accordingly.
	 */
	@Test
	public void testPrimitivesAsStrings() throws Exception {
		String json;
		ReaderParser p2 = JsonParser.DEFAULT;
		WriterSerializer s = Json5Serializer.DEFAULT;

		json = "{f01:'1',f02:'1',f03:'true',f04:'true',f05:'1',f06:'1',f07:'1',f08:'1',f09:'1',f10:'1'}";
		B b = p2.parse(json, B.class);
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
		assertThrown(()->p2.parse("\"\"", Number.class)).asMessage().isContains("Invalid JSON number");

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
		assertThrown(()->p2.parse("0123", Number.class)).asMessage().isContains("Invalid JSON number");
		s = "-0123";
		r = p1.parse(s, Number.class);
		assertEquals(-0123, r.intValue());
		assertTrue(r instanceof Integer);
		assertThrown(()->p2.parse("-0123", Number.class)).asMessage().isContains("Invalid JSON number");

		// Lax allows 0x123 and -0x123, strict does not.
		s = "0x123";
		r = p1.parse(s, Number.class);
		assertEquals(0x123, r.intValue());
		assertTrue(r instanceof Integer);
		assertThrown(()->p2.parse("0x123", Number.class)).asMessage().isContains("Invalid JSON number");
		s = "-0x123";
		r = p1.parse(s, Number.class);
		assertEquals(-0x123, r.intValue());
		assertTrue(r instanceof Integer);
		assertThrown(()->p2.parse("-0x123", Number.class)).asMessage().isContains("Invalid JSON number");
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

		assertThrown(()->p2.parse(s, C.class)).isType(ParseException.class);
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

	//====================================================================================================
	// testStreamsAutoClose
	// Validates PARSER_autoCloseStreams.
	//====================================================================================================
	@Test
	public void testStreamsAutoClose() throws Exception {
		ReaderParser p2 = JsonParser.DEFAULT.copy().autoCloseStreams().build();
		Object x;
		Reader r;

		r = reader("{foo:'bar'}{baz:'qux'}");
		x = p2.parse(r, JsonMap.class);
		assertObject(x).asJson().is("{foo:'bar'}");
		assertThrown(()->p2.parse(r, JsonMap.class)).asMessage().isContains("Reader is closed");
	}

	//====================================================================================================
	// testMultipleObjectsInStream
	// Validates that readers are not closed so that we can read streams of POJOs.
	//====================================================================================================
	@Test
	public void testMultipleObjectsInStream() throws Exception {
		ReaderParser p2 = JsonParser.create().unbuffered().build();
		Object x;
		Reader r;

		r = reader("{foo:'bar'}{baz:'qux'}");
		x = p2.parse(r, JsonMap.class);
		assertObject(x).asJson().is("{foo:'bar'}");
		x = p2.parse(r, JsonMap.class);
		assertObject(x).asJson().is("{baz:'qux'}");

		r = reader("[123][456]");
		x = p2.parse(r, JsonList.class);
		assertObject(x).asJson().is("[123]");
		x = p2.parse(r, JsonList.class);
		assertObject(x).asJson().is("[456]");
	}

	private Reader reader(String in) {
		return new CloseableStringReader(in);
	}
}