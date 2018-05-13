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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.urlencoding.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests for the RestUtils class.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestUtilsTest {

	//====================================================================================================
	// decode(String)
	//====================================================================================================
	@Test
	public void testDecode() throws Exception {
		assertNull(urlDecode(null));
		assertEquals("foo/bar baz  bing", urlDecode("foo%2Fbar+baz++bing"));
	}

	//====================================================================================================
	// encode(String)
	//====================================================================================================
	@Test
	public void testEncode() throws Exception {
		assertNull(urlEncode(null));
		assertEquals("foo%2Fbar+baz++bing", urlEncode("foo/bar baz  bing"));
		assertEquals("foobar", urlEncode("foobar"));
		assertEquals("+", urlEncode(" "));
		assertEquals("%2F", urlEncode("/"));
	}

	//====================================================================================================
	// trimPathInfo(String,String)
	//====================================================================================================
	@Test
	public void testGetServletURI() throws Exception {
		String e, sp, cp;

		e = "http://hostname";
		sp = "";
		cp = "";

		for (String s : new String[]{
				"http://hostname",
				"http://hostname/foo",
				"http://hostname?foo",
				"http://hostname/?foo"})
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		for (String s : new String[]{
				"http:/hostname?foo"}) {
			try {
				trimPathInfo(new StringBuffer(s), cp, sp);
				fail("Exception expected - " + s);
			} catch (RuntimeException ex) {}
		}


		e = "http://hostname";
		sp = "/";
		cp = "/";

		for (String s : new String[]{
				"http://hostname",
				"http://hostname/foo",
				"http://hostname?foo",
				"http://hostname/?foo"})
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		e = "http://hostname/foo";
		sp = "/foo";
		cp = "/";

		for (String s : new String[]{
				"http://hostname/foo",
				"http://hostname/foo/bar",
				"http://hostname/foo?bar"})
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		for (String s : new String[]{
				"http://hostname/foo2",
				"http://hostname/fo2",
				"http://hostname?foo",
				"http://hostname/fo?bar",
				"http:/hostname/foo"}) {
			try {
				trimPathInfo(new StringBuffer(s), cp, sp);
				fail("Exception expected - " + s);
			} catch (RuntimeException ex) {}
		}

		e = "http://hostname/foo/bar";
		sp = "/foo/bar";
		cp = "/";

		for (String s : new String[]{
				"http://hostname/foo/bar",
				"http://hostname/foo/bar/baz",
				"http://hostname/foo/bar?baz"})
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		for (String s : new String[]{
				"http://hostname/foo2/bar",
				"http://hostname/foo/bar2"
			}) {
			try {
				trimPathInfo(new StringBuffer(s), cp, sp);
				fail("Exception expected - " + s);
			} catch (RuntimeException ex) {}
		}

		e = "http://hostname/foo/bar";
		sp = "/bar";
		cp = "/foo";

		for (String s : new String[]{
				"http://hostname/foo/bar",
				"http://hostname/foo/bar/baz",
				"http://hostname/foo/bar?baz"})
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		for (String s : new String[]{
				"http://hostname/foo2/bar",
				"http://hostname/foo/bar2"
			}) {
			try {
				trimPathInfo(new StringBuffer(s), cp, sp);
				fail("Exception expected - " + s);
			} catch (RuntimeException ex) {}
		}
	}

	//====================================================================================================
	// trimSlashes(String)
	//====================================================================================================
	@Test
	public void testTrimSlashes() throws Exception {
		assertNull(trimSlashes(null));
		assertEquals("", trimSlashes(""));
		assertEquals("", trimSlashes("/"));
		assertEquals("", trimSlashes("//"));
		assertEquals("foo/bar", trimSlashes("foo/bar"));
		assertEquals("foo/bar", trimSlashes("foo/bar//"));
		assertEquals("foo/bar", trimSlashes("/foo/bar//"));
		assertEquals("foo/bar", trimSlashes("//foo/bar//"));
	}

	//====================================================================================================
	// trimTrailingSlashes(String)
	//====================================================================================================
	@Test
	public void testTrimTrailingSlashes() throws Exception {
		assertNull(trimTrailingSlashes((String)null));
		assertEquals("", trimTrailingSlashes(""));
		assertEquals("", trimTrailingSlashes("/"));
		assertEquals("", trimTrailingSlashes("//"));
		assertEquals("foo/bar", trimTrailingSlashes("foo/bar"));
		assertEquals("foo/bar", trimTrailingSlashes("foo/bar//"));
		assertEquals("/foo/bar", trimTrailingSlashes("/foo/bar//"));
		assertEquals("//foo/bar", trimTrailingSlashes("//foo/bar//"));
	}

	//====================================================================================================
	// trimTrailingSlashes(StringBuffer)
	//====================================================================================================
	@Test
	public void testTrimTrailingSlashes2() throws Exception {
		assertNull(trimTrailingSlashes((StringBuffer)null));
		assertEquals("", trimTrailingSlashes(new StringBuffer("")).toString());
		assertEquals("", trimTrailingSlashes(new StringBuffer("/")).toString());
		assertEquals("", trimTrailingSlashes(new StringBuffer("//")).toString());
		assertEquals("foo/bar", trimTrailingSlashes(new StringBuffer("foo/bar")).toString());
		assertEquals("foo/bar", trimTrailingSlashes(new StringBuffer("foo/bar//")).toString());
		assertEquals("/foo/bar", trimTrailingSlashes(new StringBuffer("/foo/bar//")).toString());
		assertEquals("//foo/bar", trimTrailingSlashes(new StringBuffer("//foo/bar//")).toString());
	}
	
	//====================================================================================================
	// Test URL-encoded strings parsed into plain-text values using UrlEncodingParser.parseIntoSimpleMap().
	//====================================================================================================
	@Test
	public void testParseIntoSimpleMap() throws Exception {
		Map<String,String[]> m;

		String s = "?f1=,()=&f2a=$b(true)&f2b=true&f3a=$n(123)&f3b=123&f4=$s(foo)";
		m = parseQuery(s);
		assertEquals(",()=", m.get("f1")[0]);
		assertEquals("$b(true)", m.get("f2a")[0]);
		assertEquals("true", m.get("f2b")[0]);
		assertEquals("$n(123)", m.get("f3a")[0]);
		assertEquals("123", m.get("f3b")[0]);
		assertEquals("$s(foo)", m.get("f4")[0]);

		s = "f1=v1&=";
		m = parseQuery(s);
		assertEquals("v1", m.get("f1")[0]);
		assertEquals("", m.get("")[0]);

		s = "f1=v1&f2&f3";
		m = parseQuery(s);
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
		Map<String,String[]> m;

		String s = "?f1&f1&f2&f2=abc&f2=def&f2";
		m = parseQuery(s);
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

}
