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
package org.apache.juneau.uon;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

@SuppressWarnings("rawtypes")
public class UonParserTest extends SimpleTestBase {

	static UonParser p = UonParser.DEFAULT;
	static UonParser pe = UonParser.DEFAULT_DECODING;

	//====================================================================================================
	// Basic test
	//====================================================================================================
	@Test void testBasic() throws Exception {

		String t;
		Map m;

		// Simple string
		// Top level
		t = "a";
		assertEquals("a", p.parse(t, String.class));
		assertEquals("a", p.parse(t, Object.class));
		assertEquals("a", pe.parse(t, String.class));
		t = "'a'";
		assertEquals("a", p.parse(t, String.class));
		assertEquals("a", p.parse(t, Object.class));
		t = " 'a' ";
		assertEquals("a", p.parse(t, String.class));

		// 2nd level
		t = "(a=a)";
		assertEquals("a", p.parse(t, Map.class).get("a"));
		assertEquals("a", pe.parse(t, Map.class).get("a"));

		t = "('a'='a')";
		assertEquals("a", p.parse(t, Map.class).get("a"));
		assertEquals("a", pe.parse(t, Map.class).get("a"));

		// Simple map
		// Top level
		t = "(a=b,c=123,d=false,e=true,f=null)";
		m = p.parse(t, Map.class);
		assertEquals("b", m.get("a"));
		assertTrue(m.get("c") instanceof Number);
		assertEquals(123, m.get("c"));
		assertTrue(m.get("d") instanceof Boolean);
		assertEquals(Boolean.FALSE, m.get("d"));
		assertTrue(m.get("e") instanceof Boolean);
		assertEquals(Boolean.TRUE, m.get("e"));
		m = pe.parse(t, Map.class);
		assertNull(m.get("f"));

		t = "(a=true)";
		m = p.parse(t, HashMap.class, String.class, Boolean.class);
		assertTrue(m.get("a") instanceof Boolean);
		assertEquals("true", m.get("a").toString());

		// null
		// Top level
		t = "null";
		assertNull(p.parse(t, Object.class));
		assertNull(pe.parse(t, Object.class));

		// 2nd level
		t = "(null=null)";
		m = p.parse(t, Map.class);
		assertTrue(m.containsKey(null));
		assertNull(m.get(null));
		m = pe.parse(t, Map.class);
		assertTrue(m.containsKey(null));
		assertNull(m.get(null));

		t = " ( null = null ) ";
		m = p.parse(t, Map.class);
		assertTrue(m.containsKey(null));
		assertNull(m.get(null));
		m = pe.parse(t, Map.class);
		assertTrue(m.containsKey(null));
		assertNull(m.get(null));

		// 3rd level
		t = "(null=(null=null))";
		m = p.parse(t, Map.class);
		assertTrue(((Map)m.get(null)).containsKey(null));
		assertNull(((Map)m.get(null)).get(null));
		m = pe.parse(t, Map.class);
		assertTrue(((Map)m.get(null)).containsKey(null));
		assertNull(((Map)m.get(null)).get(null));

		// Empty array
		// Top level
		t = "@()";
		List l = (List)p.parse(t, Object.class);
		assertTrue(l.isEmpty());
		t = " @( ) ";
		l = p.parse(t, List.class);
		assertTrue(l.isEmpty());

		// 2nd level in map
		t = "(x=@())";
		m = p.parse(t, HashMap.class, String.class, List.class);
		assertTrue(m.containsKey("x"));
		assertTrue(((List)m.get("x")).isEmpty());
		m = (Map)p.parse(t, Object.class);
		assertTrue(m.containsKey("x"));
		assertTrue(((List)m.get("x")).isEmpty());
		t = " ( x = @( ) )";
		m = p.parse(t, HashMap.class, String.class, List.class);
		assertTrue(m.containsKey("x"));
		assertTrue(((List)m.get("x")).isEmpty());

		// Empty 2 dimensional array
		t = "@(@())";
		l = (List)p.parse(t, Object.class);
		assertEquals(1, l.size());
		l = (List)l.get(0);
		assertTrue(l.isEmpty());
		t = " @( @( ) ) ";
		l = p.parse(t, LinkedList.class, List.class);
		assertEquals(1, l.size());
		l = (List)l.get(0);
		assertTrue(l.isEmpty());

		// Array containing empty string
		// Top level
		t = "@('')";
		l = (List)p.parse(t, Object.class);
		assertEquals(1, l.size());
		assertEquals("", l.get(0));
		t = " @( '' ) ";
		l = p.parse(t, List.class, String.class);
		assertEquals(1, l.size());
		assertEquals("", l.get(0));

		// 2nd level
		t = "(''=@(''))";
		m = (Map)p.parse(t, Object.class);
		assertEquals("", ((List)m.get("")).get(0));
		t = " ( '' = @( '' ) ) ";
		m = p.parse(t, HashMap.class, String.class, List.class);
		assertEquals("", ((List)m.get("")).get(0));

		// Array containing 3 empty strings
		t = "@('','','')";
		l = (List)p.parse(t, Object.class);
		assertEquals(3, l.size());
		assertEquals("", l.get(0));
		assertEquals("", l.get(1));
		assertEquals("", l.get(2));
		t = " @( '' , '' , '' ) ";
		l = p.parse(t, List.class, Object.class);
		assertEquals(3, l.size());
		assertEquals("", l.get(0));
		assertEquals("", l.get(1));
		assertEquals("", l.get(2));

		// String containing \u0000
		// Top level
		t = "'\u0000'";
		assertEquals("\u0000", p.parse(t, Object.class));
		t = " '\u0000' ";
		assertEquals("\u0000", p.parse(t, String.class));
		assertEquals("\u0000", p.parse(t, Object.class));

		// 2nd level
		t = "('\u0000'='\u0000')";
		m = (Map)p.parse(t, Object.class);
		assertEquals(1, m.size());
		assertEquals("\u0000", m.get("\u0000"));
		t = " ( '\u0000' = '\u0000' ) ";
		m = p.parse(t, HashMap.class, String.class, String.class);
		assertEquals(1, m.size());
		assertEquals("\u0000", m.get("\u0000"));
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals(1, m.size());
		assertEquals("\u0000", m.get("\u0000"));

		// Boolean
		// Top level
		t = "false";
		Boolean b = (Boolean)p.parse(t, Object.class);
		assertEquals(Boolean.FALSE, b);
		b = p.parse(t, Boolean.class);
		assertEquals(Boolean.FALSE, b);
		t = " false ";
		b = p.parse(t, Boolean.class);
		assertEquals(Boolean.FALSE, b);

		// 2nd level
		t = "(x=false)";
		m = (Map)p.parse(t, Object.class);
		assertEquals(Boolean.FALSE, m.get("x"));
		t = " ( x = false ) ";
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals(Boolean.FALSE, m.get("x"));

		// Number
		// Top level
		t = "123";
		Integer i = (Integer)p.parse(t, Object.class);
		assertEquals(123, i.intValue());
		i = p.parse(t, Integer.class);
		assertEquals(123, i.intValue());
		Double d = p.parse(t, Double.class);
		assertEquals(123, d.intValue());
		Float f = p.parse(t, Float.class);
		assertEquals(123, f.intValue());
		t = " 123 ";
		i = p.parse(t, Integer.class);
		assertEquals(123, i.intValue());

		// 2nd level
		t = "(x=123)";
		m = (Map)p.parse(t, Object.class);
		assertEquals(123, ((Integer)m.get("x")).intValue());
		t = " ( x = 123 ) ";
		m = p.parse(t, HashMap.class, String.class, Number.class);
		assertEquals(123, ((Integer)m.get("x")).intValue());
		m = p.parse(t, HashMap.class, String.class, Double.class);
		assertEquals(123, ((Double)m.get("x")).intValue());

		// Unencoded chars
		// Top level
		t = "x;/?:@-_.!*~'";
		assertEquals("x;/?:@-_.!*'", p.parse(t, Object.class));
		assertEquals("x;/?:@-_.!*'", pe.parse(t, Object.class));

		// 2nd level
		t = "(x;/?:@-_.!*~'=x;/?:@-_.!*~')";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x;/?:@-_.!*'", m.get("x;/?:@-_.!*'"));
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("x;/?:@-_.!*'", m.get("x;/?:@-_.!*'"));
		m = p.parse(t, HashMap.class, String.class, String.class);
		assertEquals("x;/?:@-_.!*'", m.get("x;/?:@-_.!*'"));

		// Encoded chars
		// Top level
		t = "x{}|\\^[]`<>#%\"&+";
		assertEquals("x{}|\\^[]`<>#%\"&+", p.parse(t, Object.class));
		assertEquals("x{}|\\^[]`<>#%\"&+", p.parse(t, String.class));
		assertThrown(()->pe.parse( "x{}|\\^[]`<>#%\"&+", Object.class)).isType(ParseException.class);
		t = "x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B";
		assertEquals("x{}|\\^[]`<>#%\"&+", pe.parse(t, Object.class));
		assertEquals("x{}|\\^[]`<>#%\"&+", pe.parse(t, String.class));

		// 2nd level
		t = "(x{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x{}|\\^[]`<>#%\"&+", m.get("x{}|\\^[]`<>#%\"&+"));
		assertThrown(()->pe.parse("(x{}|\\^[]`<>#%\"&+=x{}|\\^[]`<>#%\"&+)", Object.class)).isType(ParseException.class);
		t = "(x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B=x%7B%7D%7C%5C%5E%5B%5D%60%3C%3E%23%25%22%26%2B)";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("x{}|\\^[]`<>#%\"&+", m.get("x{}|\\^[]`<>#%\"&+"));

		// Special chars
		// Top level
		t = "'x$,()~''";
		assertEquals("x$,()'", p.parse(t, Object.class));
		t = " 'x$,()~'' ";
		assertEquals("x$,()'", p.parse(t, Object.class));

		// 2nd level
		t = "('x$,()~''='x$,()~'')";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x$,()'", m.get("x$,()'"));
		t = " ( 'x$,()~'' = 'x$,()~'' ) ";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x$,()'", m.get("x$,()'"));

		// Equals sign
		// Gets encoded at top level, and encoded+escaped at 2nd level.
		// Top level
		t = "x=";
		assertEquals("x=", p.parse(t, Object.class));
		t = "x%3D";
		assertEquals("x=", pe.parse(t, Object.class));

		// 2nd level
		t = "('x='='x=')";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x=", m.get("x="));
		t = "('x='='x=')";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x=", m.get("x="));
		t = " ( 'x=' = 'x=' ) ";
		m = (Map)p.parse(t, Object.class);
		assertEquals("x=", m.get("x="));
		t = "('x='='x=')";
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("x=", m.get("x="));
		t = " ( 'x=' = 'x=' ) ";
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("x=", m.get("x="));
		t = "('x%3D'='x%3D')";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("x=", m.get("x="));
		t = " ( 'x%3D' = 'x%3D' ) ";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("x=", m.get("x="));

		// String starting with parenthesis
		// Top level
		t = "'()'";
		assertEquals("()", p.parse(t, Object.class));
		assertEquals("()", p.parse(t, String.class));

		t = " '()' ";
		assertEquals("()", p.parse(t, Object.class));
		assertEquals("()", p.parse(t, String.class));

		// 2nd level
		t = "('()'='()')";
		m = (Map)p.parse(t, Object.class);
		assertEquals("()", m.get("()"));
		t = " ( '()' = '()' ) ";
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("()", m.get("()"));

		// String starting with $
		// Top level
		t = "$a";
		assertEquals("$a", p.parse(t, Object.class));
		t = "'$a'";
		assertEquals("$a", p.parse(t, Object.class));

		// 2nd level
		t = "($a=$a)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("$a", m.get("$a"));
		t = " ( $a = $a ) ";
		m = (Map)p.parse(t, Object.class);
		assertEquals("$a", m.get("$a"));
		t = "('$a'='$a')";
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("$a", m.get("$a"));
		t = " ( '$a' = '$a' ) ";
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("$a", m.get("$a"));

		// Blank string
		// Top level
		t = "";
		assertEquals("", p.parse(t, Object.class));
		assertEquals("", pe.parse(t, Object.class));

		// 2nd level
		t = "(=)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("", m.get(""));
		t = "(''='')";
		m = p.parse(t, HashMap.class, String.class, Object.class);
		assertEquals("", m.get(""));

		// 3rd level
		t = "(=(=))";
		m = (Map)p.parse(t, Object.class);
		assertEquals("", ((Map)m.get("")).get(""));
		t = " ( = ( = ) ) ";
		m = p.parse(t, HashMap.class, String.class, HashMap.class);
		assertEquals("", ((Map)m.get("")).get(""));

		// Newline character
		// Top level
		t = "'%0A'";
		assertEquals("\n", pe.parse(t, Object.class));
		assertEquals("%0A", p.parse(t, Object.class));

		// 2nd level
		t = "('%0A'='%0A')";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("\n", m.get("\n"));
		m = (Map)p.parse(t, Object.class);
		assertEquals("%0A", m.get("%0A"));

		// 3rd level
		t = "('%0A'=('%0A'='%0A'))";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("\n", ((Map)m.get("\n")).get("\n"));
	}

	//====================================================================================================
	// Unicode character test
	//====================================================================================================
	@Test void testUnicodeChars() throws Exception {
		String t;
		Map m;

		// 2-byte UTF-8 character
		// Top level
		t = "¢";
		assertEquals("¢", p.parse(t, Object.class));
		assertEquals("¢", p.parse(t, String.class));
		t = "%C2%A2";
		assertEquals("¢", pe.parse(t, Object.class));
		assertEquals("¢", pe.parse(t, String.class));

		// 2nd level
		t = "(¢=¢)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("¢", m.get("¢"));
		t = "(%C2%A2=%C2%A2)";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("¢", m.get("¢"));

		// 3rd level
		t = "(¢=(¢=¢))";
		m = (Map)p.parse(t, Object.class);
		assertEquals("¢", ((Map)m.get("¢")).get("¢"));
		t = "(%C2%A2=(%C2%A2=%C2%A2))";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("¢", ((Map)m.get("¢")).get("¢"));

		// 3-byte UTF-8 character
		// Top level
		t = "€";
		assertEquals("€", p.parse(t, Object.class));
		assertEquals("€", p.parse(t, String.class));
		t = "%E2%82%AC";
		assertEquals("€", pe.parse(t, Object.class));
		assertEquals("€", pe.parse(t, String.class));

		// 2nd level
		t = "(€=€)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("€", m.get("€"));
		t = "(%E2%82%AC=%E2%82%AC)";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("€", m.get("€"));

		// 3rd level
		t = "(€=(€=€))";
		m = (Map)p.parse(t, Object.class);
		assertEquals("€", ((Map)m.get("€")).get("€"));
		t = "(%E2%82%AC=(%E2%82%AC=%E2%82%AC))";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("€", ((Map)m.get("€")).get("€"));

		// 4-byte UTF-8 character
		// Top level
		t = "𤭢";
		assertEquals("𤭢", p.parse(t, Object.class));
		assertEquals("𤭢", p.parse(t, String.class));
		t = "%F0%A4%AD%A2";
		assertEquals("𤭢", pe.parse(t, Object.class));
		assertEquals("𤭢", pe.parse(t, String.class));

		// 2nd level
		t = "(𤭢=𤭢)";
		m = (Map)p.parse(t, Object.class);
		assertEquals("𤭢", m.get("𤭢"));
		t = "(%F0%A4%AD%A2=%F0%A4%AD%A2)";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("𤭢", m.get("𤭢"));

		// 3rd level
		t = "(𤭢=(𤭢=𤭢))";
		m = (Map)p.parse(t, Object.class);
		assertEquals("𤭢", ((Map)m.get("𤭢")).get("𤭢"));
		t = "(%F0%A4%AD%A2=(%F0%A4%AD%A2=%F0%A4%AD%A2))";
		m = (Map)pe.parse(t, Object.class);
		assertEquals("𤭢", ((Map)m.get("𤭢")).get("𤭢"));
	}

	//====================================================================================================
	// Test simple bean
	//====================================================================================================
	@Test void testSimpleBean() throws Exception {
		UonParser p2 = UonParser.DEFAULT;
		A t;

		String s = "(f1=foo,f2=123)";
		t = p2.parse(s, A.class);
		assertEquals("foo", t.f1);
		assertEquals(123, t.f2);
	}

	public static class A {
		public String f1;
		public int f2;
	}

	//====================================================================================================
	// testStreamsAutoClose
	// Validates PARSER_autoCloseStreams.
	//====================================================================================================
	@Test void testStreamsAutoClose() throws Exception {
		var p2 = UonParser.DEFAULT.copy().autoCloseStreams().build();
		Object x;
		Reader r;

		r = reader("(foo=bar)(foo=bar)");
		x = p2.parse(r, JsonMap.class);
		assertJson(x, "{foo:'bar'}");
		assertThrowsWithMessage(Exception.class, "Reader is closed", ()->p2.parse(r, JsonMap.class));
	}

	//====================================================================================================
	// testMultipleObjectsInStream
	// Validates that readers are not closed so that we can read streams of POJOs.
	//====================================================================================================
	@Test void testMultipleObjectsInStream() throws Exception {
		var p2 = UonParser.create().unbuffered().build();
		Object x;
		Reader r;

		r = reader("(foo=bar)(baz=qux)");
		x = p2.parse(r, JsonMap.class);
		assertJson(x, "{foo:'bar'}");
		x = p2.parse(r, JsonMap.class);
		assertJson(x, "{baz:'qux'}");

		r = reader("@(123)@(456)");
		x = p2.parse(r, JsonList.class);
		assertJson(x, "[123]");
		x = p2.parse(r, JsonList.class);
		assertJson(x, "[456]");
	}

	private Reader reader(String in) {
		return new CloseableStringReader(in);
	}
}