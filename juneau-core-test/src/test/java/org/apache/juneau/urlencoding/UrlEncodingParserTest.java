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
package org.apache.juneau.urlencoding;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.junit.*;

@SuppressWarnings({"rawtypes","javadoc"})
public class UrlEncodingParserTest {

	static UrlEncodingParser p = UrlEncodingParser.DEFAULT;

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
		t = "_value=a";
		assertEquals("a", p.parse(t, Object.class));
		assertEquals("a", p.parse(t, String.class));
		t = "_value=(a)";
		assertEquals("a", p.parse(t, String.class));
		assertEquals("a", p.parse(t, Object.class));
		t = "_value=$s(a)";
		assertEquals("a", p.parse(t, String.class));

		t = "a";
		assertEquals("a", p.parseParameter(t, Object.class));
		assertEquals("a", p.parseParameter(t, String.class));
		t = "(a)";
		assertEquals("a", p.parseParameter(t, String.class));
		assertEquals("a", p.parseParameter(t, Object.class));
		t = "$s(a)";
		assertEquals("a", p.parseParameter(t, String.class));

		// 2nd level
		t = "?a=a";
		assertEquals("a", ((Map)p.parse(t, Map.class)).get("a"));

		// Simple map
		// Top level
		t = "?a=b&c=$n(123)&d=$b(false)&e=$b(true)&f=%00";
		m = p.parse(t, Map.class);
		assertEquals("b", m.get("a"));
		assertTrue(m.get("c") instanceof Number);
		assertEquals(123, m.get("c"));
		assertTrue(m.get("d") instanceof Boolean);
		assertEquals(Boolean.FALSE, m.get("d"));
		assertTrue(m.get("e") instanceof Boolean);
		assertEquals(Boolean.TRUE, m.get("e"));
		assertNull(m.get("f"));

		t = "$o(a=b,c=$n(123),d=$b(false),e=$b(true),f=%00)";
		m = p.parseParameter(t, Map.class);
		assertEquals("b", m.get("a"));
		assertTrue(m.get("c") instanceof Number);
		assertEquals(123, m.get("c"));
		assertTrue(m.get("d") instanceof Boolean);
		assertEquals(Boolean.FALSE, m.get("d"));
		assertTrue(m.get("e") instanceof Boolean);
		assertEquals(Boolean.TRUE, m.get("e"));
		assertEquals("%00", m.get("f"));

		t = "$o(a=b,c=$n(123),d=$b(false),e=$b(true),f=\u0000)";
		m = p.parseParameter(t, Map.class);
		assertTrue(m.containsKey("f"));
		assertNull(m.get("f"));

		t = "?a=true";
		m = p.parse(t, HashMap.class, String.class, Boolean.class);
		assertTrue(m.get("a") instanceof Boolean);
		assertEquals("true", m.get("a").toString());

		// null
		// Top level
		t = "_value=%00";
		assertNull(p.parse(t, Object.class));
		t = "\u0000";
		assertNull(p.parseParameter(t, Object.class));
		t = "%00";
		assertEquals("%00", p.parseParameter(t, Object.class));

		// 2nd level
		t = "?%00=%00";
		m = p.parse(t, Map.class);
		assertTrue(m.containsKey(null));
		assertNull(m.get(null));

		t = "?\u0000=\u0000";
		m = p.parse(t, Map.class);
		assertTrue(m.containsKey(null));
		assertNull(m.get(null));

		// 3rd level
		t = "?%00=$o(%00=%00)";
		m = p.parse(t, Map.class);
		assertTrue(((Map)m.get(null)).containsKey(null));
		assertNull(((Map)m.get(null)).get(null));

		// Empty array
		// Top level
		t = "_value=$a()";
		l = (List)p.parse(t, Object.class);
		assertTrue(l.isEmpty());
		t = "_value=()";
		l = p.parse(t, List.class);
		assertTrue(l.isEmpty());
		t = "$a()";
		l = (List)p.parseParameter(t, Object.class);
		assertTrue(l.isEmpty());
		t = "()";
		l = p.parseParameter(t, List.class);
		assertTrue(l.isEmpty());

		// 2nd level in map
		t = "?x=$a()";
		m = p.parse(t, HashMap.class, String.class, List.class);
		assertTrue(m.containsKey("x"));
		assertTrue(((List)m.get("x")).isEmpty());
		m = (Map)p.parse(t, Object.class);
		assertTrue(m.containsKey("x"));
		assertTrue(((List)m.get("x")).isEmpty());
		t = "?x=()";
		m = p.parse(t, HashMap.class, String.class, List.class);
		assertTrue(m.containsKey("x"));
		assertTrue(((List)m.get("x")).isEmpty());

		// Empty 2 dimensional array
		t = "_value=$a($a())";
		l = (List)p.parse(t, Object.class);
		assertTrue(l.size() == 1);
		l = (List)l.get(0);
		assertTrue(l.isEmpty());
		t = "0=()";
		l = p.parse(t, LinkedList.class, List.class);
		assertTrue(l.size() == 1);
		l = (List)l.get(0);
		assertTrue(l.isEmpty());
		t = "$a($a())";
		l = (List)p.parseParameter(t, Object.class);
		assertTrue(l.size() == 1);
		l = (List)l.get(0);
		assertTrue(l.isEmpty());
		t = "(())";
		l = (List)p.parseParameter(t, LinkedList.class, List.class);
		assertTrue(l.size() == 1);
		l = (List)l.get(0);
		assertTrue(l.isEmpty());

		// Array containing empty string
		// Top level
		t = "_value=$a(())";
		l = (List)p.parse(t, Object.class);
		assertTrue(l.size() == 1);
		assertEquals("", l.get(0));
		t = "0=()";
		l = p.parse(t, List.class, String.class);
		assertTrue(l.size() == 1);
		assertEquals("", l.get(0));
		t = "$a(())";
		l = (List)p.parseParameter(t, Object.class);
		assertTrue(l.size() == 1);
		assertEquals("", l.get(0));
		t = "(())";
		l = (List)p.parseParameter(t, List.class, String.class);
		assertTrue(l.size() == 1);
		assertEquals("", l.get(0));

		// 2nd level
		t = "?()=$a(())";
		m = (Map)p.parse(t, Object.class);
		assertEquals("", ((List)m.get("")).get(0));
		t = "?()=(())";
		m = p.parse(t, HashMap.class, String.class, List.class);
		assertEquals("", ((List)m.get("")).get(0));

		// Array containing 3 empty strings
		t = "_value=$a(,,)";
		l = (List)p.parse(t, Object.class);
		assertTrue(l.size() == 3);
		assertEquals("", l.get(0));
		assertEquals("", l.get(1));
		assertEquals("", l.get(2));
		t = "0=&1=&2=";
		l = p.parse(t, List.class, Object.class);
		assertTrue(l.size() == 3);
		assertEquals("", l.get(0));
		assertEquals("", l.get(1));
		assertEquals("", l.get(2));
		t = "$a(,,)";
		l = (List)p.parseParameter(t, Object.class);
		assertTrue(l.size() == 3);
		assertEquals("", l.get(0));
		assertEquals("", l.get(1));
		assertEquals("", l.get(2));
		t = "(,,)";
		l = (List)p.parseParameter(t, List.class, Object.class);
		assertTrue(l.size() == 3);
		assertEquals("", l.get(0));
		assertEquals("", l.get(1));
		assertEquals("", l.get(2));

		// String containing \u0000
		// Top level
		t = "_value=$s(\u0000)";
		assertEquals("\u0000", p.parse(t, Object.class));
		t = "_value=(\u0000)";
		assertEquals("\u0000", p.parse(t, String.class));
		assertEquals("\u0000", p.parse(t, Object.class));
		t = "$s(\u0000)";
		assertEquals("\u0000", p.parseParameter(t, Object.class));
		t = "(\u0000)";
		assertEquals("\u0000", p.parseParameter(t, String.class));
		assertEquals("\u0000", p.parseParameter(t, Object.class));

		// 2nd level
		t = "?(\u0000)=(\u0000)";
		m = (Map)p.parse(t, Object.class);
		assertTrue(m.size() == 1);
		assertEquals("\u0000", m.get("\u0000"));
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertTrue(m.size() == 1);
		assertEquals("\u0000", m.get("\u0000"));

		// Boolean
		// Top level
		t = "_value=$b(false)";
		Boolean b = (Boolean)p.parse(t, Object.class);
		assertEquals(Boolean.FALSE, b);
		b = p.parse(t, Boolean.class);
		assertEquals(Boolean.FALSE, b);
		t = "_value=false";
		b = p.parse(t, Boolean.class);
		assertEquals(Boolean.FALSE, b);
		t = "$b(false)";
		b = (Boolean)p.parseParameter(t, Object.class);
		assertEquals(Boolean.FALSE, b);
		b = p.parseParameter(t, Boolean.class);
		assertEquals(Boolean.FALSE, b);
		t = "false";
		b = p.parseParameter(t, Boolean.class);
		assertEquals(Boolean.FALSE, b);

		// 2nd level
		t = "?x=$b(false)";
		m = (Map)p.parse(t, Object.class);
		assertEquals(Boolean.FALSE, m.get("x"));
		t = "?x=false";
		m = p.parse(t, HashMap.class, String.class, Boolean.class);
		assertEquals(Boolean.FALSE, m.get("x"));

		// Number
		// Top level
		t = "_value=$n(123)";
		Integer i = (Integer)p.parse(t, Object.class);
		assertEquals(123, i.intValue());
		i = p.parse(t, Integer.class);
		assertEquals(123, i.intValue());
		Double d = p.parse(t, Double.class);
		assertEquals(123, d.intValue());
		Float f = p.parse(t, Float.class);
		assertEquals(123, f.intValue());
		t = "_value=123";
		i = p.parse(t, Integer.class);
		assertEquals(123, i.intValue());
		t = "$n(123)";
		i = (Integer)p.parseParameter(t, Object.class);
		assertEquals(123, i.intValue());
		i = p.parseParameter(t, Integer.class);
		assertEquals(123, i.intValue());
		d = p.parseParameter(t, Double.class);
		assertEquals(123, d.intValue());
		f = p.parseParameter(t, Float.class);
		assertEquals(123, f.intValue());
		t = "123";
		i = p.parseParameter(t, Integer.class);
		assertEquals(123, i.intValue());

		// 2nd level
		t = "?x=$n(123)";
		m = (Map)p.parse(t, Object.class);
		assertEquals(123, ((Integer)m.get("x")).intValue());
		m = p.parse(t, HashMap.class, String.class, Double.class);
		assertEquals(123, ((Double)m.get("x")).intValue());

		// Unencoded chars
		// Top level
		t = "_value=x;/?:@-_.!*'";
		assertEquals("x;/?:@-_.!*'", p.parse(t, Object.class));
		t = "x;/?:@-_.!*'";
		assertEquals("x;/?:@-_.!*'", p.parseParameter(t, Object.class));

		// 2nd level
		t = "?x;/?:@-_.!*'=x;/?:@-_.!*'";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x;/?:@-_.!*'", m.get("x;/?:@-_.!*'"));
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("x;/?:@-_.!*'", m.get("x;/?:@-_.!*'"));
		m = p.parse(t, HashMap.class, String.class, String.class);
		assertEquals("x;/?:@-_.!*'", m.get("x;/?:@-_.!*'"));

		// Encoded chars
		// Top level
		t = "_value=x{}|\\^[]`<>#%\"&+";
		try {
			assertEquals("x{}|\\^[]`<>#%\"&+", p.parse(t, Object.class));
			fail("Expected parse exception from invalid hex sequence.");
		} catch (ParseException e) {
			// Good.
		}
		t = "_value=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B";
		assertEquals("x{}|\\^[]`<>#%\"&+", p.parse(t, Object.class));
		assertEquals("x{}|\\^[]`<>#%\"&+", p.parse(t, String.class));
		t = "x{}|\\^[]`<>#%\"&+";
		assertEquals("x{}|\\^[]`<>#%\"&+", p.parseParameter(t, Object.class));
		t = "x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B";
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", p.parseParameter(t, Object.class));
		assertEquals("x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B", p.parseParameter(t, String.class));

		// 2nd level
		t = "?x{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+";
		try {
			m = (Map)p.parse(t, Object.class);
			fail("Expected parse exception from invalid hex sequence.");
		} catch (ParseException e) {
			// Good.
		}
		t = "?x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x{}|\\^[]`<>#%\"&+", m.get("x{}|\\^[]`<>#%\"&+"));

		// Special chars
		// These characters are escaped and not encoded.
		// Top level
		t = "_value=x~$~,~(~)";
		assertEquals("x$,()", p.parse(t, Object.class));
		t = "x~$~,~(~)";
		assertEquals("x$,()", p.parseParameter(t, Object.class));
		t = "_value=x~~$~~,~~(~~)";
		assertEquals("x~$~,~(~)", p.parse(t, Object.class));
		t = "x~~$~~,~~(~~)";
		assertEquals("x~$~,~(~)", p.parseParameter(t, Object.class));

		// At secondary levels, these characters are escaped and not encoded.
		// 2nd level
		t = "?x~$~,~(~)=x~$~,~(~)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x$,()", m.get("x$,()"));
		t = "?x~~$~~,~~(~~)=x~~$~~,~~(~~)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x~$~,~(~)", m.get("x~$~,~(~)"));

		// Equals sign
		// Gets encoded at top level, and encoded+escaped at 2nd level.
		// Top level
		t = "_value=x=";
		assertEquals("x=", p.parse(t, Object.class));
		t = "_value=x%3D";
		assertEquals("x=", p.parse(t, Object.class));
		t = "x=";
		assertEquals("x=", p.parseParameter(t, Object.class));
		t = "x%3D";
		assertEquals("x%3D", p.parseParameter(t, Object.class));

		// 2nd level
		t = "?x~%3D=x~%3D";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x=", m.get("x="));
		t = "?x~~%3D=x~~%3D";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x~=", m.get("x~="));

		// String starting with parenthesis
		// Top level
		t = "_value=~(~)";
		assertEquals("()", p.parse(t, Object.class));
		assertEquals("()", p.parse(t, String.class));
		t = "_value=(~(~))";
		assertEquals("()", p.parse(t, Object.class));
		assertEquals("()", p.parse(t, String.class));
		t = "_value=(~(~))";
		assertEquals("()", p.parse(t, Object.class));
		assertEquals("()", p.parse(t, String.class));
		t = "~(~)";
		assertEquals("()", p.parseParameter(t, Object.class));
		assertEquals("()", p.parseParameter(t, String.class));
		t = "(~(~))";
		assertEquals("()", p.parseParameter(t, Object.class));
		assertEquals("()", p.parseParameter(t, String.class));
		t = "(~(~))";
		assertEquals("()", p.parseParameter(t, Object.class));
		assertEquals("()", p.parseParameter(t, String.class));

		// 2nd level
		t = "?(~(~))=(~(~))";
		m = (Map)p.parse(t, Object.class);
		assertEquals("()", m.get("()"));
		t = "?(~(~))=(~(~))";
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("()", m.get("()"));

		// String starting with $
		// Top level
		t = "_value=(~$a)";
		assertEquals("$a", p.parse(t, Object.class));
		t = "_value=(~$a)";
		assertEquals("$a", p.parse(t, Object.class));
		t = "(~$a)";
		assertEquals("$a", p.parseParameter(t, Object.class));
		t = "(~$a)";
		assertEquals("$a", p.parseParameter(t, Object.class));

		// 2nd level
		t = "?(~$a)=(~$a)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("$a", m.get("$a"));
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("$a", m.get("$a"));

		// Blank string
		// Top level
		t = "_value=";
		assertEquals("", p.parse(t, Object.class));
		t = "";
		assertEquals("", p.parseParameter(t, Object.class));

		// 2nd level
		t = "?=";
		m = (Map)p.parse(t, Object.class);
		assertEquals("", m.get(""));
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("", m.get(""));

		// 3rd level
		t = "?=$o(=)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("", ((Map)m.get("")).get(""));
		t = "?=(=)";
		m = p.parse(t, HashMap.class, String.class, HashMap.class);
		assertEquals("", ((Map)m.get("")).get(""));

		// Newline character
		// Top level
		t = "_value=(%0A)";
		assertEquals("\n", p.parse(t, Object.class));
		t = "(%0A)";
		assertEquals("%0A", p.parseParameter(t, Object.class));
		t = "(\n)";
		assertEquals("\n", p.parseParameter(t, Object.class));

		// 2nd level
		t = "?%0A=(%0A)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("\n", m.get("\n"));

		// 3rd level
		t = "?%0A=$o((%0A)=(%0A))";
		m = (Map)p.parse(t, Object.class);
		assertEquals("\n", ((Map)m.get("\n")).get("\n"));
	}

	//====================================================================================================
	// Unicode character test
	//====================================================================================================
	@Test
	public void testUnicodeChars() throws Exception {
		String t;
		Map m;

		// 2-byte UTF-8 character
		// Top level
		t = "_value=¢";
		assertEquals("¢", p.parse(t, Object.class));
		assertEquals("¢", p.parse(t, String.class));
		t = "_value=%C2%A2";
		assertEquals("¢", p.parse(t, Object.class));
		assertEquals("¢", p.parse(t, String.class));
		t = "¢";
		assertEquals("¢", p.parseParameter(t, Object.class));
		assertEquals("¢", p.parseParameter(t, String.class));
		t = "%C2%A2";
		assertEquals("%C2%A2", p.parseParameter(t, Object.class));
		assertEquals("%C2%A2", p.parseParameter(t, String.class));

		// 2nd level
		t = "?%C2%A2=%C2%A2";
		m = (Map)p.parse(t, Object.class);
		assertEquals("¢", m.get("¢"));

		// 3rd level
		t = "?%C2%A2=$o(%C2%A2=%C2%A2)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("¢", ((Map)m.get("¢")).get("¢"));

		// 3-byte UTF-8 character
		// Top level
		t = "_value=€";
		assertEquals("€", p.parse(t, Object.class));
		assertEquals("€", p.parse(t, String.class));
		t = "_value=%E2%82%AC";
		assertEquals("€", p.parse(t, Object.class));
		assertEquals("€", p.parse(t, String.class));
		t = "€";
		assertEquals("€", p.parseParameter(t, Object.class));
		assertEquals("€", p.parseParameter(t, String.class));
		t = "%E2%82%AC";
		assertEquals("%E2%82%AC", p.parseParameter(t, Object.class));
		assertEquals("%E2%82%AC", p.parseParameter(t, String.class));

		// 2nd level
		t = "?%E2%82%AC=%E2%82%AC";
		m = (Map)p.parse(t, Object.class);
		assertEquals("€", m.get("€"));

		// 3rd level
		t = "?%E2%82%AC=$o(%E2%82%AC=%E2%82%AC)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("€", ((Map)m.get("€")).get("€"));

		// 4-byte UTF-8 character
		// Top level
		t = "_value=𤭢";
		assertEquals("𤭢", p.parse(t, Object.class));
		assertEquals("𤭢", p.parse(t, String.class));
		t = "_value=%F0%A4%AD%A2";
		assertEquals("𤭢", p.parse(t, Object.class));
		assertEquals("𤭢", p.parse(t, String.class));
		t = "𤭢";
		assertEquals("𤭢", p.parseParameter(t, Object.class));
		assertEquals("𤭢", p.parseParameter(t, String.class));
		t = "%F0%A4%AD%A2";
		assertEquals("%F0%A4%AD%A2", p.parseParameter(t, Object.class));
		assertEquals("%F0%A4%AD%A2", p.parseParameter(t, String.class));

		// 2nd level
		t = "?%F0%A4%AD%A2=%F0%A4%AD%A2";
		m = (Map)p.parse(t, Object.class);
		assertEquals("𤭢", m.get("𤭢"));

		// 3rd level
		t = "?%F0%A4%AD%A2=$o(%F0%A4%AD%A2=%F0%A4%AD%A2)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("𤭢", ((Map)m.get("𤭢")).get("𤭢"));
	}

	//====================================================================================================
	// Test simple bean
	//====================================================================================================
	@Test
	public void testSimpleBean() throws Exception {
		UrlEncodingParser p = UrlEncodingParser.DEFAULT;
		A t;

		String s = "?f1=foo&f2=123";
		t = p.parse(s, A.class);
		assertEquals("foo", t.f1);
		assertEquals(123, t.f2);

		s = "(f1=foo,f2=123)";
		t = p.parseParameter(s, A.class);
		assertEquals("foo", t.f1);
		assertEquals(123, t.f2);

		s = "$o(f1=foo,f2=123)";
		t = p.parseParameter(s, A.class);
		assertEquals("foo", t.f1);
		assertEquals(123, t.f2);
	}

	public static class A {
		public String f1;
		public int f2;
	}

	//====================================================================================================
	// Test URL-encoded strings with no-value parameters.
	//====================================================================================================
	@Test
	public void testNoValues() throws Exception {
		UrlEncodingParser p = UrlEncodingParser.DEFAULT;
		ObjectMap m;

		String s = "?f1";
		m = p.parse(s, ObjectMap.class);
		assertTrue(m.containsKey("f1"));
		assertNull(m.get("f1"));
		s = "?f1=f2&f3";
		m = p.parse(s, ObjectMap.class);
		assertEquals("f2", m.get("f1"));
		assertTrue(m.containsKey("f3"));
		assertNull(m.get("f3"));
	}

	//====================================================================================================
	// Test URL-encoded strings parsed into plain-text values using UrlEncodingParser.parseIntoSimpleMap().
	//====================================================================================================
	@Test
	public void testParseIntoSimpleMap() throws Exception {
		UrlEncodingParser p = UrlEncodingParser.DEFAULT;
		Map<String,String[]> m;

		String s = "?f1=,()=&f2a=$b(true)&f2b=true&f3a=$n(123)&f3b=123&f4=$s(foo)";
		m = p.parseIntoSimpleMap(s);
		assertEquals(",()=", m.get("f1")[0]);
		assertEquals("$b(true)", m.get("f2a")[0]);
		assertEquals("true", m.get("f2b")[0]);
		assertEquals("$n(123)", m.get("f3a")[0]);
		assertEquals("123", m.get("f3b")[0]);
		assertEquals("$s(foo)", m.get("f4")[0]);

		s = "f1=v1&=";
		m = p.parseIntoSimpleMap(s);
		assertEquals("v1", m.get("f1")[0]);
		assertEquals("", m.get("")[0]);

		s = "f1=v1&f2&f3";
		m = p.parseIntoSimpleMap(s);
		assertEquals("v1", m.get("f1")[0]);
		assertTrue(m.containsKey("f2"));
		assertTrue(m.containsKey("f3"));
		assertNull(m.get("f2"));
		assertNull(m.get("f3"));
	}

	//====================================================================================================
	// Test parsing URL-encoded strings with multiple values.
	//====================================================================================================
	@Test
	public void testParseIntoSimpleMapMultiValues() throws Exception {
		UrlEncodingParser p = UrlEncodingParser.DEFAULT;
		Map<String,String[]> m;

		String s = "?f1&f1&f2&f2=abc&f2=def&f2";
		m = p.parseIntoSimpleMap(s);
		assertObjectEquals("{f1:null,f2:['abc','def']}", m);
	}

	@Test
	public void testEmptyString() throws Exception {
		UrlEncodingParser p = UrlEncodingParser.DEFAULT;

		String s = "";
		B b = p.parse(s, B.class);
		assertEquals("f1", b.f1);
	}

	public static class B {
		public String f1 = "f1";
	}

	//====================================================================================================
	// Test comma-delimited list parameters.
	//====================================================================================================
	@Test
	public void testCommaDelimitedLists() throws Exception {
		UrlEncodingParser p = UrlEncodingParser.DEFAULT;

		String s = "?f1=1,2,3&f2=a,b,c&f3=true,false&f4=&f5";
		C c = p.parse(s, C.class);
		assertObjectEquals("{f1:[1,2,3],f2:['a','b','c'],f3:[true,false],f4:[]}", c);
	}

	public static class C {
		public int[] f1;
		public String[] f2;
		public boolean[] f3;
		public String[] f4;
		public String[] f5;
	}

	//====================================================================================================
	// Test comma-delimited list parameters with special characters.
	//====================================================================================================
	@Test
	public void testCommaDelimitedListsWithSpecialChars() throws Exception {
		UrlEncodingParser p = UrlEncodingParser.DEFAULT;
		String s;
		C1 c;

		// In the string below, the ~ character should not be interpreted as an escape.
		s = "?f1=a~b,a~b";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:['a~b','a~b']}", c);

		s = "?f1=(a~b,a~b)";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:['a~b','a~b']}", c);

		s = "?f1=((a~b),(a~b))";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:['a~b','a~b']}", c);

		s = "?f1=($s(a~b),$s(a~b))";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:['a~b','a~b']}", c);

		s = "?f1=$a($s(a~b),$s(a~b))";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:['a~b','a~b']}", c);

		s = "?f1=~~,~~";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:['~','~']}", c);

		s = "?f1=(~~,~~)";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:['~','~']}", c);

		s = "?f1=(~~~~~~,~~~~~~)";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:['~~~','~~~']}", c);

		s = "?f1=((~~~~~~),(~~~~~~))";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:['~~~','~~~']}", c);

		// The ~ should be treated as an escape if followed by any of the following characters:  ,()~=
		s = "?f1=~,~(~)~~~=~$,~,~(~)~~~=~$";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:[',()~=$',',()~=$']}", c);

		s = "?f1=(~,~(~)~~~=~$,~,~(~)~~~=~$)";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:[',()~=$',',()~=$']}", c);

		s = "?f1=((~,~(~)~~~=~$),(~,~(~)~~~=~$))";
		c = p.parse(s, C1.class);
		assertObjectEquals("{f1:[',()~=$',',()~=$']}", c);

		s = "?a~b=a~b";
		ObjectMap m = p.parse(s, ObjectMap.class);
		assertEquals("{'a~b':'a~b'}", m.toString());

		s = "?(a~b)=(a~b)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{'a~b':'a~b'}", m.toString());

		s = "?$s(a~b)=$s(a~b)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{'a~b':'a~b'}", m.toString());

		s = "?~~=~~";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{'~':'~'}", m.toString());

		s = "?(~~)=(~~)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{'~':'~'}", m.toString());

		s = "?~~~~~~=~~~~~~";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{'~~~':'~~~'}", m.toString());

		s = "?(~~~~~~)=(~~~~~~)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{'~~~':'~~~'}", m.toString());

		s = "?$s(~~~~~~)=$s(~~~~~~)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{'~~~':'~~~'}", m.toString());

		s = "?~,~(~)~~~=~$=~,~(~)~~~=~$";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{',()~=$':',()~=$'}", m.toString());

		s = "?(~,~(~)~~~=~$)=(~,~(~)~~~=~$)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{',()~=$':',()~=$'}", m.toString());

		s = "?$s(~,~(~)~~~=~$)=$s(~,~(~)~~~=~$)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{',()~=$':',()~=$'}", m.toString());

		s = "?%7E%2C%7E%28%7E%29%7E%7E%7E%3D%7E%24=%7E%2C%7E%28%7E%29%7E%7E%7E%3D%7E%24";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{',()~=$':',()~=$'}", m.toString());

		s = "?(%7E%2C%7E%28%7E%29%7E%7E%7E%3D%7E%24)=(%7E%2C%7E%28%7E%29%7E%7E%7E%3D%7E%24)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{',()~=$':',()~=$'}", m.toString());

		s = "?$s(%7E%2C%7E%28%7E%29%7E%7E%7E%3D%7E%24)=$s(%7E%2C%7E%28%7E%29%7E%7E%7E%3D%7E%24)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{',()~=$':',()~=$'}", m.toString());
	}

	public static class C1 {
		public String[] f1;
	}

	//====================================================================================================
	// Test comma-delimited list parameters.
	//====================================================================================================
	@Test
	public void testWhitespace() throws Exception {
		UrlEncodingParser p = UrlEncodingParser.DEFAULT_WS_AWARE;
		String s;
		ObjectMap m;

		s = "?f1=foo\n\t&f2=bar\n\t";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{f1:'foo',f2:'bar'}", m.toString());

		s = "?f1=(\n\t)&f2=(\n\t)";
		m = p.parse(s, ObjectMap.class);
		assertEquals("\n\t", m.getString("f1"));
		assertEquals("\n\t", m.getString("f2"));

		s = "?f1=(\n\t)\n\t&f2=(\n\t)\n\t";
		m = p.parse(s, ObjectMap.class);
		assertEquals("\n\t", m.getString("f1"));
		assertEquals("\n\t", m.getString("f2"));
		assertEquals("{f1:'\\n\\t',f2:'\\n\\t'}", m.toString());  // Note that JsonSerializer escapes newlines and tabs.

		s = "?f1=$s(\n\t)\n\t&f2=$s(\n\t)\n\t";
		m = p.parse(s, ObjectMap.class);
		assertEquals("\n\t", m.getString("f1"));
		assertEquals("\n\t", m.getString("f2"));
		assertEquals("{f1:'\\n\\t',f2:'\\n\\t'}", m.toString());  // Note that JsonSerializer escapes newlines and tabs.

		s = "?f1=$o(\n\tf1a=a,\n\tf1b=b\n\t)\n\t&f2=$o(\n\tf2a=a,\n\tf2b=b\n\t)\n\t";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{f1:{f1a:'a',f1b:'b'},f2:{f2a:'a',f2b:'b'}}", m.toString());  // Note that JsonSerializer escapes newlines and tabs.
		D d = p.parse(s, D.class);
		assertObjectEquals("{f1:{f1a:'a',f1b:'b'},f2:{f2a:'a',f2b:'b'}}", d);  // Note that JsonSerializer escapes newlines and tabs.

		s = "?f1=$o(\n\tf1a=(\n\t),\n\tf1b=(\n\t)\n\t)\n\t&f2=$o(\n\tf2a=(\n\t),\n\tf2b=(\n\t)\n\t)\n\t";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{f1:{f1a:'\\n\\t',f1b:'\\n\\t'},f2:{f2a:'\\n\\t',f2b:'\\n\\t'}}", m.toString());  // Note that JsonSerializer escapes newlines and tabs.
		d = p.parse(s, D.class);
		assertObjectEquals("{f1:{f1a:'\\n\\t',f1b:'\\n\\t'},f2:{f2a:'\\n\\t',f2b:'\\n\\t'}}", d);  // Note that JsonSerializer escapes newlines and tabs.

		s = "?f1=$a(\n\tfoo,\n\tbar\n\t)\n\t&f2=$a(\n\tfoo,\n\tbar\n\t)\n\t";
		m = p.parse(s, ObjectMap.class);
		assertEquals("{f1:['foo','bar'],f2:['foo','bar']}", m.toString());  // Note that JsonSerializer escapes newlines and tabs.

		s = "f1=a,\n\tb,\n\tc\n\t&f2=1,\n\t2,\n\t3\n\t&f3=true,\n\tfalse\n\t";
		E e = p.parse(s, E.class);
		assertObjectEquals("{f1:['a','b','c'],f2:[1,2,3],f3:[true,false]}", e);

		s = "f1=a%2C%0D%0Ab%2C%0D%0Ac%0D%0A&f2=1%2C%0D%0A2%2C%0D%0A3%0D%0A&f3=true%2C%0D%0Afalse%0D%0A";
		e = p.parse(s, E.class);
		assertObjectEquals("{f1:['a','b','c'],f2:[1,2,3],f3:[true,false]}", e);
	}

	public static class D {
		public D1 f1;
		public D2 f2;
	}

	public static class D1 {
		public String f1a, f1b;
	}

	public static class D2 {
		public String f2a, f2b;
	}

	public static class E {
		public String[] f1;
		public int[] f2;
		public boolean[] f3;
	}

	//====================================================================================================
	// Multi-part parameters on beans via URLENC_expandedParams
	//====================================================================================================
	@Test
	public void testMultiPartParametersOnBeansViaProperty() throws Exception {
		UrlEncodingParser p;
		String in;

		p = UrlEncodingParser.DEFAULT.clone().setExpandedParams(true);
		in = ""
			+ "f01=a&f01=b"
			+ "&f02=c&f02=d"
			+ "&f03=1&f03=2"
			+ "&f04=3&f04=4"
			+ "&f05=(e,f)&f05=(g,h)"
			+ "&f06=(i,j)&f06=(k,l)"
			+ "&f07=(a=a,b=1,c=true)&f07=(a=b,b=2,c=false)"
			+ "&f08=(a=a,b=1,c=true)&f08=(a=b,b=2,c=false)"
			+ "&f09=((a=a,b=1,c=true))&f09=((a=b,b=2,c=false))"
			+ "&f10=((a=a,b=1,c=true))&f10=((a=b,b=2,c=false))"
			+ "&f11=a&f11=b"
			+ "&f12=c&f12=d"
			+ "&f13=1&f13=2"
			+ "&f14=3&f14=4"
			+ "&f15=(e,f)&f15=(g,h)"
			+ "&f16=(i,j)&f16=(k,l)"
			+ "&f17=(a=a,b=1,c=true)&f17=(a=b,b=2,c=false)"
			+ "&f18=(a=a,b=1,c=true)&f18=(a=b,b=2,c=false)"
			+ "&f19=((a=a,b=1,c=true))&f19=((a=b,b=2,c=false))"
			+ "&f20=((a=a,b=1,c=true))&f20=((a=b,b=2,c=false))";

		DTOs.B t = p.parse(in, DTOs.B.class);
		String e = "{"
			+ "f01:['a','b'],"
			+ "f02:['c','d'],"
			+ "f03:[1,2],"
			+ "f04:[3,4],"
			+ "f05:[['e','f'],['g','h']],"
			+ "f06:[['i','j'],['k','l']],"
			+ "f07:[{a:'a',b:1,c:true},{a:'b',b:2,c:false}],"
			+ "f08:[{a:'a',b:1,c:true},{a:'b',b:2,c:false}],"
			+ "f09:[[{a:'a',b:1,c:true}],[{a:'b',b:2,c:false}]],"
			+ "f10:[[{a:'a',b:1,c:true}],[{a:'b',b:2,c:false}]],"
			+ "f11:['a','b'],"
			+ "f12:['c','d'],"
			+ "f13:[1,2],"
			+ "f14:[3,4],"
			+ "f15:[['e','f'],['g','h']],"
			+ "f16:[['i','j'],['k','l']],"
			+ "f17:[{a:'a',b:1,c:true},{a:'b',b:2,c:false}],"
			+ "f18:[{a:'a',b:1,c:true},{a:'b',b:2,c:false}],"
			+ "f19:[[{a:'a',b:1,c:true}],[{a:'b',b:2,c:false}]],"
			+ "f20:[[{a:'a',b:1,c:true}],[{a:'b',b:2,c:false}]]"
		+"}";
		assertSortedObjectEquals(e, t);
	}

	//====================================================================================================
	// Multi-part parameters on beans via @UrlEncoding.expandedParams on class
	//====================================================================================================
	@Test
	public void testMultiPartParametersOnBeansViaAnnotationOnClass() throws Exception {
		UrlEncodingParser p;
		String in;
		p = UrlEncodingParser.DEFAULT;
		in = ""
			+ "f01=a&f01=b"
			+ "&f02=c&f02=d"
			+ "&f03=1&f03=2"
			+ "&f04=3&f04=4"
			+ "&f05=(e,f)&f05=(g,h)"
			+ "&f06=(i,j)&f06=(k,l)"
			+ "&f07=(a=a,b=1,c=true)&f07=(a=b,b=2,c=false)"
			+ "&f08=(a=a,b=1,c=true)&f08=(a=b,b=2,c=false)"
			+ "&f09=((a=a,b=1,c=true))&f09=((a=b,b=2,c=false))"
			+ "&f10=((a=a,b=1,c=true))&f10=((a=b,b=2,c=false))"
			+ "&f11=a&f11=b"
			+ "&f12=c&f12=d"
			+ "&f13=1&f13=2"
			+ "&f14=3&f14=4"
			+ "&f15=(e,f)&f15=(g,h)"
			+ "&f16=(i,j)&f16=(k,l)"
			+ "&f17=(a=a,b=1,c=true)&f17=(a=b,b=2,c=false)"
			+ "&f18=(a=a,b=1,c=true)&f18=(a=b,b=2,c=false)"
			+ "&f19=((a=a,b=1,c=true))&f19=((a=b,b=2,c=false))"
			+ "&f20=((a=a,b=1,c=true))&f20=((a=b,b=2,c=false))";

		DTOs.C t = p.parse(in, DTOs.C.class);
		String e = "{"
			+ "f01:['a','b'],"
			+ "f02:['c','d'],"
			+ "f03:[1,2],"
			+ "f04:[3,4],"
			+ "f05:[['e','f'],['g','h']],"
			+ "f06:[['i','j'],['k','l']],"
			+ "f07:[{a:'a',b:1,c:true},{a:'b',b:2,c:false}],"
			+ "f08:[{a:'a',b:1,c:true},{a:'b',b:2,c:false}],"
			+ "f09:[[{a:'a',b:1,c:true}],[{a:'b',b:2,c:false}]],"
			+ "f10:[[{a:'a',b:1,c:true}],[{a:'b',b:2,c:false}]],"
			+ "f11:['a','b'],"
			+ "f12:['c','d'],"
			+ "f13:[1,2],"
			+ "f14:[3,4],"
			+ "f15:[['e','f'],['g','h']],"
			+ "f16:[['i','j'],['k','l']],"
			+ "f17:[{a:'a',b:1,c:true},{a:'b',b:2,c:false}],"
			+ "f18:[{a:'a',b:1,c:true},{a:'b',b:2,c:false}],"
			+ "f19:[[{a:'a',b:1,c:true}],[{a:'b',b:2,c:false}]],"
			+ "f20:[[{a:'a',b:1,c:true}],[{a:'b',b:2,c:false}]]"
		+"}";
		assertSortedObjectEquals(e, t);
	}
}