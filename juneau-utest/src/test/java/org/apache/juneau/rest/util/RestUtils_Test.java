/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.util;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.urlencoding.*;
import org.junit.jupiter.api.*;

class RestUtils_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// decode(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_testDecode() {
		assertNull(urlDecode(null));
		assertEquals("foo/bar baz  bing", urlDecode("foo%2Fbar+baz++bing"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// encode(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEncode() {
		assertNull(urlEncode(null));
		assertEquals("foo%2Fbar+baz++bing", urlEncode("foo/bar baz  bing"));
		assertEquals("foobar", urlEncode("foobar"));
		assertEquals("+", urlEncode(" "));
		assertEquals("%2F", urlEncode("/"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// trimPathInfo(String,String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_testGetServletURI() {
		var e = "http://hostname";
		var sp = "";
		var cp = "";

		for (var s : a("http://hostname", "http://hostname/foo", "http://hostname?foo", "http://hostname/?foo"))
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		for (var s : a("http:/hostname?foo")) {
			assertThrows(Exception.class, ()->trimPathInfo(new StringBuffer(s), "", ""));
		}

		e = "http://hostname";
		sp = "/";
		cp = "/";

		for (var s : a("http://hostname", "http://hostname/foo", "http://hostname?foo", "http://hostname/?foo"))
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		e = "http://hostname/foo";
		sp = "/foo";
		cp = "/";

		for (var s : a("http://hostname/foo", "http://hostname/foo/bar", "http://hostname/foo?bar"))
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		for (var s : a("http://hostname/foo2", "http://hostname/fo2", "http://hostname?foo", "http://hostname/fo?bar", "http:/hostname/foo")) {
			assertThrows(Exception.class, ()->trimPathInfo(new StringBuffer(s), "/", "/foo"));
		}

		e = "http://hostname/foo/bar";
		sp = "/foo/bar";
		cp = "/";

		for (var s : a("http://hostname/foo/bar", "http://hostname/foo/bar/baz", "http://hostname/foo/bar?baz"))
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		for (var s : a("http://hostname/foo2/bar", "http://hostname/foo/bar2")) {
			assertThrows(Exception.class, ()->trimPathInfo(new StringBuffer(s), "/foo/bar", "/foo/bar"));
		}

		e = "http://hostname/foo/bar";
		sp = "/bar";
		cp = "/foo";

		for (var s : a("http://hostname/foo/bar", "http://hostname/foo/bar/baz", "http://hostname/foo/bar?baz"))
			assertEquals(e, trimPathInfo(new StringBuffer(s), cp, sp).toString());

		for (var s : a("http://hostname/foo2/bar", "http://hostname/foo/bar2")) {
			assertThrows(Exception.class, ()->trimPathInfo(new StringBuffer(s), "/foo", "/bar"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// trimSlashes(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_testTrimSlashes() {
		assertNull(trimSlashes(null));
		assertEquals("", trimSlashes(""));
		assertEquals("", trimSlashes("/"));
		assertEquals("", trimSlashes("//"));
		assertEquals("foo/bar", trimSlashes("foo/bar"));
		assertEquals("foo/bar", trimSlashes("foo/bar//"));
		assertEquals("foo/bar", trimSlashes("/foo/bar//"));
		assertEquals("foo/bar", trimSlashes("//foo/bar//"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// trimTrailingSlashes(String)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_testTrimTrailingSlashes() {
		assertNull(trimTrailingSlashes((String)null));
		assertEquals("", trimTrailingSlashes(""));
		assertEquals("", trimTrailingSlashes("/"));
		assertEquals("", trimTrailingSlashes("//"));
		assertEquals("foo/bar", trimTrailingSlashes("foo/bar"));
		assertEquals("foo/bar", trimTrailingSlashes("foo/bar//"));
		assertEquals("/foo/bar", trimTrailingSlashes("/foo/bar//"));
		assertEquals("//foo/bar", trimTrailingSlashes("//foo/bar//"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test URL-encoded strings parsed into plain-text values using UrlEncodingParser.parseIntoSimpleMap().
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_testParseIntoSimpleMap() {
		var s = "?f1=,()=&f2a=$b(true)&f2b=true&f3a=$n(123)&f3b=123&f4=$s(foo)";
		var m = parseQuery(s);
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

	//------------------------------------------------------------------------------------------------------------------
	// Test parsing URL-encoded strings with multiple values.
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_testParseIntoSimpleMapMultiValues() {
		var s = "?f1&f1&f2&f2=abc&f2=def&f2";
		var m = parseQuery(s);
		assertBean(m, "f1,f2", "<null>,[abc,def]");
	}

	@Test void h02_testEmptyString() throws Exception {
		var p = UrlEncodingParser.DEFAULT;

		var s = "";
		var b = p.parse(s, B.class);
		assertEquals("f1", b.f1);
	}

	public static class B {
		public String f1 = "f1";
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_testTrimContextPath() {
		assertEquals("/bar", trimContextPath("/foo", "/bar"));
		assertEquals("/", trimContextPath("/foo", "/"));
		assertEquals("", trimContextPath("/foo", ""));
		assertEquals(null, trimContextPath("/foo", null));

		assertEquals("/bar", trimContextPath("/foo", "/foo/bar"));
		assertEquals("/bar/baz", trimContextPath("/foo", "/foo/bar/baz"));
		assertEquals("/bar/", trimContextPath("/foo", "/foo/bar/"));
		assertEquals("/", trimContextPath("/foo", "/foo/"));
		assertEquals("/", trimContextPath("/foo", "/foo"));
	}

	@Test void i02_testIsValidContextPath() {
		assertTrue(isValidContextPath(""));
		assertTrue(isValidContextPath("/foo"));
		assertFalse(isValidContextPath("/"));
		assertFalse(isValidContextPath("/foo/"));
		assertFalse(isValidContextPath(null));
		assertFalse(isValidContextPath("foo"));
	}

	@Test void i03_testIsValidServletPath() {
		assertTrue(isValidServletPath(""));
		assertTrue(isValidServletPath("/foo"));
		assertFalse(isValidServletPath("/"));
		assertFalse(isValidServletPath("/foo/"));
		assertFalse(isValidServletPath(null));
		assertFalse(isValidServletPath("foo"));
	}

	@Test void i04_testIsValidPathInfo() {
		assertFalse(isValidPathInfo(""));
		assertTrue(isValidPathInfo("/foo"));
		assertTrue(isValidPathInfo("/"));
		assertTrue(isValidPathInfo("/foo/"));
		assertTrue(isValidPathInfo(null));
		assertFalse(isValidPathInfo("foo"));
	}
}