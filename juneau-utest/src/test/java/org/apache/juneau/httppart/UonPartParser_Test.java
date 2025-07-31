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
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;
import org.junit.*;

@SuppressWarnings({"rawtypes"})
@FixMethodOrder(NAME_ASCENDING)
public class UonPartParser_Test {

	private static UonParserSession p = UonParser.DEFAULT.getSession();
	private static BeanSession bs = p;

	private static <T> T parse(String input, ClassMeta<T> type) throws SchemaValidationException, ParseException {
		return p.parse((HttpPartType)null, (HttpPartSchema)null, input, type);
	}

	//====================================================================================================
	// Basic test
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {

		String t;
		Map m;
		List l;

		// Simple string
		// Top level
		t = "a";
		assertEquals("a", parse(t, bs.object()));
		assertEquals("a", parse(t, bs.string()));
		t = "'a'";
		assertEquals("a", parse(t, bs.string()));
		assertEquals("a", parse(t, bs.object()));
		t = " 'a' ";
		assertEquals("a", parse(t, bs.string()));

		// Simple map
		// Top level
		t = "(a=b,c=123,d=false,e=true,f=%00)";
		m = parse(t, bs.getClassMeta(Map.class));
		assertEquals("b", m.get("a"));
		assertTrue(m.get("c") instanceof Number);
		assertEquals(123, m.get("c"));
		assertTrue(m.get("d") instanceof Boolean);
		assertEquals(Boolean.FALSE, m.get("d"));
		assertTrue(m.get("e") instanceof Boolean);
		assertEquals(Boolean.TRUE, m.get("e"));
		assertEquals("%00", m.get("f"));

		t = "(a=b,c=123,d=false,e=true,f=null)";
		m = parse(t, bs.getClassMeta(Map.class));
		assertTrue(m.containsKey("f"));
		assertNull(m.get("f"));

		// null
		// Top level
		t = "null";
		assertNull(parse(t, bs.object()));

		// Empty array
		// Top level
		t = "@()";
		l = (List)parse(t, bs.object());
		assertTrue(l.isEmpty());
		t = " @( ) ";
		l = parse(t, bs.getClassMeta(List.class));
		assertTrue(l.isEmpty());

		// Empty 2 dimensional array
		t = "@(@())";
		l = (List)parse(t, bs.object());
		assertEquals(l.size(), 1);
		l = (List)l.get(0);
		assertTrue(l.isEmpty());
		t = "@(@())";
		l = (List)parse(t, bs.getClassMeta(LinkedList.class, List.class));
		assertEquals(l.size(), 1);
		l = (List)l.get(0);
		assertTrue(l.isEmpty());

		// Array containing empty string
		// Top level
		t = "@('')";
		l = (List)parse(t, bs.object());
		assertEquals(l.size(), 1);
		assertEquals("", l.get(0));
		t = "@('')";
		l = (List)parse(t, bs.getClassMeta(List.class, String.class));
		assertEquals(l.size(), 1);
		assertEquals("", l.get(0));

		// Array containing 3 empty strings
		t = "@('','','')";
		l = (List)parse(t, bs.object());
		assertEquals(l.size(), 3);
		assertEquals("", l.get(0));
		assertEquals("", l.get(1));
		assertEquals("", l.get(2));
		t = "@('','','')";
		l = (List)parse(t, bs.getClassMeta(List.class, Object.class));
		assertEquals(l.size(), 3);
		assertEquals("", l.get(0));
		assertEquals("", l.get(1));
		assertEquals("", l.get(2));

		// String containing \u0000
		// Top level
		t = "'\u0000'";
		assertEquals("\u0000", parse(t, bs.object()));
		t = "'\u0000'";
		assertEquals("\u0000", parse(t, bs.string()));
		assertEquals("\u0000", parse(t, bs.object()));

		// Boolean
		// Top level
		t = "_value=false";
		Boolean b = false;
		t = "false";
		b = (Boolean)parse(t, bs.object());
		assertEquals(Boolean.FALSE, b);
		b = parse(t, bs.getClassMeta(Boolean.class));
		assertEquals(Boolean.FALSE, b);
		t = "false";
		b = parse(t, bs.getClassMeta(Boolean.class));
		assertEquals(Boolean.FALSE, b);

		// Number
		// Top level
		t = "123";
		Integer i = -1;
		Double d = -1d;
		Float f = -1f;
		i = (Integer)parse(t, bs.object());
		assertEquals(123, i.intValue());
		i = parse(t, bs.getClassMeta(Integer.class));
		assertEquals(123, i.intValue());
		d = parse(t, bs.getClassMeta(Double.class));
		assertEquals(123, d.intValue());
		f = parse(t, bs.getClassMeta(Float.class));
		assertEquals(123, f.intValue());
		t = "123";
		i = parse(t, bs.getClassMeta(Integer.class));
		assertEquals(123, i.intValue());

		// Unencoded chars
		// Top level
		t = "x;/?:@-_.!*'";
		assertEquals("x;/?:@-_.!*'", parse(t, bs.object()));

		// Encoded chars
		// Top level
		t = "x{}|\\^[]`<>#%\"&+";
		assertEquals("x{}|\\^[]`<>#%\"&+", parse(t, bs.object()));
		t = "x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B";
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", parse(t, bs.object()));
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", parse(t, bs.string()));

		// Special chars
		// These characters are escaped and not encoded.
		// Top level
		t = "'x$,()'";
		assertEquals("x$,()", parse(t, bs.object()));

		// Equals sign
		// Gets encoded at top level, and encoded+escaped at 2nd level.
		// Top level
		t = "'x='";
		assertEquals("x=", parse(t, bs.object()));
		t = "'x%3D'";
		assertEquals("x%3D", parse(t, bs.object()));

		// String starting with parenthesis
		// Top level
		t = "'()'";
		assertEquals("()", parse(t, bs.object()));
		assertEquals("()", parse(t, bs.string()));

		// String starting with $
		// Top level
		t = "$a";
		assertEquals("$a", parse(t, bs.object()));
		t = "$a";
		assertEquals("$a", parse(t, bs.object()));

		// Blank string
		// Top level
		t = "";
		assertEquals("", parse(t, bs.object()));

		// Newline character
		// Top level
		t = "'%0A'";
		assertEquals("%0A", parse(t, bs.object()));
		t = "'\n'";
		assertEquals("\n", parse(t, bs.object()));
	}

	//====================================================================================================
	// Unicode character test
	//====================================================================================================
	@Test
	public void testUnicodeChars() throws Exception {
		String t;

		// 2-byte UTF-8 character
		// Top level
		t = "¢";
		assertEquals("¢", parse(t, bs.object()));
		assertEquals("¢", parse(t, bs.string()));
		t = "%C2%A2";
		assertEquals("%C2%A2", parse(t, bs.object()));
		assertEquals("%C2%A2", parse(t, bs.string()));

		// 3-byte UTF-8 character
		// Top level
		t = "€";
		assertEquals("€", parse(t, bs.object()));
		assertEquals("€", parse(t, bs.string()));
		t = "%E2%82%AC";
		assertEquals("%E2%82%AC", parse(t, bs.object()));
		assertEquals("%E2%82%AC", parse(t, bs.string()));

		// 4-byte UTF-8 character
		// Top level
		t = "𤭢";
		assertEquals("𤭢", parse(t, bs.object()));
		assertEquals("𤭢", parse(t, bs.string()));
		t = "%F0%A4%AD%A2";
		assertEquals("%F0%A4%AD%A2", parse(t, bs.object()));
		assertEquals("%F0%A4%AD%A2", parse(t, bs.string()));
	}

	//====================================================================================================
	// Test simple bean
	//====================================================================================================
	@Test
	public void testSimpleBean() throws Exception {
		A t;
		String s = null;

		s = "(f1=foo,f2=123)";
		t = parse(s, bs.getClassMeta(A.class));
		assertEquals("foo", t.f1);
		assertEquals(123, t.f2);

		s = "('f1'='foo','f2'=123)";
		t = parse(s, bs.getClassMeta(A.class));
		assertEquals("foo", t.f1);
		assertEquals(123, t.f2);
	}

	public static class A {
		public String f1;
		public int f2;
	}

	@Test
	public void testParseParameterJsonMap() throws Exception {
		String in = "(name='foo bar')";

		JsonMap r =  parse(in, BeanContext.DEFAULT.getClassMeta(JsonMap.class));

		assertEquals("{name:'foo bar'}", Json5Serializer.DEFAULT.toString(r));
	}

}