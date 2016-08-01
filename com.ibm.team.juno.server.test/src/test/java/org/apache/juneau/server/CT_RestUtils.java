/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import static org.apache.juneau.server.RestUtils.*;
import static org.junit.Assert.*;

import org.junit.*;

public class CT_RestUtils {

	//====================================================================================================
	// decode(String)
	//====================================================================================================
	@Test
	public void testDecode() throws Exception {
		assertNull(decode(null));
		assertEquals("foo/bar baz  bing", decode("foo%2Fbar+baz++bing"));
	}

	//====================================================================================================
	// encode(String)
	//====================================================================================================
	@Test
	public void testEncode() throws Exception {
		assertNull(encode(null));
		assertEquals("foo%2Fbar+baz++bing", encode("foo/bar baz  bing"));
		assertEquals("foobar", encode("foobar"));
		assertEquals("+", encode(" "));
		assertEquals("%2F", encode("/"));
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
}
