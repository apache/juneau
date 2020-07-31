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
package org.apache.juneau.rest.util;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class UrlPathPattern_Test {

	private void shouldMatch(UrlPathPattern p, String path, String expected) {
		assertObject(p.match(path)).json().is(expected);
	}

	private void shouldNotMatch(UrlPathPattern p, String path) {
		assertObject(p.match(path)).json().is("null");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_comparision() throws Exception {
		List<UrlPathPattern> l = new LinkedList<>();

		l.add(new UrlPathPattern(""));
		l.add(new UrlPathPattern("*"));
		l.add(new UrlPathPattern("/"));
		l.add(new UrlPathPattern("/*"));
		l.add(new UrlPathPattern("/foo"));
		l.add(new UrlPathPattern("/foo/*"));
		l.add(new UrlPathPattern("/foo/bar"));
		l.add(new UrlPathPattern("/foo/bar/*"));
		l.add(new UrlPathPattern("/foo/{id}"));
		l.add(new UrlPathPattern("/foo/{id}/*"));
		l.add(new UrlPathPattern("/foo/{id}/bar"));
		l.add(new UrlPathPattern("/foo/{id}/bar/*"));

		Collections.sort(l);
		assertEquals("['/foo/bar','/foo/bar/*','/foo/{id}/bar','/foo/{id}/bar/*','/foo/{id}','/foo/{id}/*','/foo','/foo/*','/','/*','/','/*']", SimpleJsonSerializer.DEFAULT.toString(l));
	}

	@Test
	public void a02_comparision() throws Exception {
		List<UrlPathPattern> l = new LinkedList<>();

		l.add(new UrlPathPattern("/foo/{id}/bar/*"));
		l.add(new UrlPathPattern("/foo/{id}/bar"));
		l.add(new UrlPathPattern("/foo/{id}/*"));
		l.add(new UrlPathPattern("/foo/{id}"));
		l.add(new UrlPathPattern("/foo/bar/*"));
		l.add(new UrlPathPattern("/foo/bar"));
		l.add(new UrlPathPattern("/foo/*"));
		l.add(new UrlPathPattern("/foo"));
		l.add(new UrlPathPattern("/*"));
		l.add(new UrlPathPattern("/"));
		l.add(new UrlPathPattern("*"));
		l.add(new UrlPathPattern(""));

		Collections.sort(l);
		assertEquals("['/foo/bar','/foo/bar/*','/foo/{id}/bar','/foo/{id}/bar/*','/foo/{id}','/foo/{id}/*','/foo','/foo/*','/','/*','/','/*']", SimpleJsonSerializer.DEFAULT.toString(l));
	}

	@Test
	public void a03_comparision() throws Exception {
		List<UrlPathPattern> l = new LinkedList<>();

		l.add(new UrlPathPattern("/foo"));
		l.add(new UrlPathPattern("/foo"));

		Collections.sort(l);
		assertEquals("['/foo','/foo']", SimpleJsonSerializer.DEFAULT.toString(l));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Simple pattern matching
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_simple_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo");
		shouldMatch(p, "/foo", "{}");
		shouldMatch(p, "/foo/", "{r:''}");
	}

	@Test
	public void b02_simple_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo");
		shouldNotMatch(p, "/fooo");
		shouldNotMatch(p, "/fo");
		shouldNotMatch(p, "/fooo/");
		shouldNotMatch(p, "/");
		shouldNotMatch(p, "/foo/bar");
	}

	@Test
	public void b03_simple_match_2parts() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/bar");
		shouldMatch(p, "/foo/bar", "{}");
		shouldMatch(p, "/foo/bar/", "{r:''}");
	}

	@Test
	public void b04_simple_noMatch_2parts() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/bar");
		shouldNotMatch(p, "/foo");
		shouldNotMatch(p, "/foo/baz");
		shouldNotMatch(p, "/foo/barr");
		shouldNotMatch(p, "/foo/bar/baz");
	}

	@Test
	public void b05_simple_match_0parts() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/");
		shouldMatch(p, "/", "{r:''}");
	}

	@Test
	public void b06_simple_noMatch_0parts() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/");
		shouldNotMatch(p, "/foo");
		shouldNotMatch(p, "/foo/bar");
	}

	@Test
	public void b07_simple_match_blank() throws Exception {
		UrlPathPattern p = new UrlPathPattern("");
		shouldMatch(p, "/", "{r:''}");
	}

	@Test
	public void b08_simple_noMatch_blank() throws Exception {
		UrlPathPattern p = new UrlPathPattern("");
		shouldNotMatch(p, "/foo");
		shouldNotMatch(p, "/foo/bar");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Simple pattern matching with remainder
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_simple_withRemainder_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/*");
		shouldMatch(p, "/foo", "{}");
		shouldMatch(p, "/foo/", "{r:''}");
		shouldMatch(p, "/foo/bar", "{r:'bar'}");
		shouldMatch(p, "/foo/bar/", "{r:'bar/'}");
		shouldMatch(p, "/foo/bar/baz", "{r:'bar/baz'}");
	}

	@Test
	public void c02_simple_withRemainder_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/*");
		shouldNotMatch(p, "/fooo");
		shouldNotMatch(p, "/fo");
		shouldNotMatch(p, "/fooo/");
		shouldNotMatch(p, "/");
		shouldNotMatch(p, "/fooo/bar");
	}

	@Test
	public void c03_simple_withRemainder_match_2parts() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/bar/*");
		shouldMatch(p, "/foo/bar", "{}");
		shouldMatch(p, "/foo/bar/baz", "{r:'baz'}");
		shouldMatch(p, "/foo/bar/baz/", "{r:'baz/'}");
	}

	@Test
	public void c04_simple_withRemainder_noMatch_2parts() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/bar/*");
		shouldNotMatch(p, "/");
		shouldNotMatch(p, "/foo");
		shouldNotMatch(p, "/foo/baz");
		shouldNotMatch(p, "/foo/barr");
		shouldNotMatch(p, "/foo/barr/");
	}

	@Test
	public void c05_simple_withRemainder_match_0parts() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/*");
		shouldMatch(p, "/", "{r:''}");
		shouldMatch(p, "/foo", "{r:'foo'}");
		shouldMatch(p, "/foo/bar", "{r:'foo/bar'}");
	}

	@Test
	public void c06_simple_withRemainder_match_blank() throws Exception {
		UrlPathPattern p = new UrlPathPattern("*");
		shouldMatch(p, "/", "{r:''}");
		shouldMatch(p, "/foo", "{r:'foo'}");
		shouldMatch(p, "/foo/bar", "{r:'foo/bar'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pattern with variables
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_1part1vars_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/{foo}");
		shouldMatch(p, "/bar", "{v:{foo:'bar'}}");
		shouldMatch(p, "/bar/", "{v:{foo:'bar'},r:''}");
	}

	@Test
	public void d02_1part1var_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/{foo}");
		shouldNotMatch(p, "/foo/bar");
		shouldNotMatch(p, "/");
	}

	@Test
	public void d03_2parts1var_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/{bar}");
		shouldMatch(p, "/foo/baz", "{v:{bar:'baz'}}");
		shouldMatch(p, "/foo/baz/", "{v:{bar:'baz'},r:''}");
	}

	@Test
	public void d04_2parts1var_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/{bar}");
		shouldNotMatch(p, "/fooo/baz");
		shouldNotMatch(p, "/fo/baz");
		shouldNotMatch(p, "/foo");
		shouldNotMatch(p, "/");
	}

	@Test
	public void d05_3vars_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/{a}/{b}/{c}");
		shouldMatch(p, "/A/B/C", "{v:{a:'A',b:'B',c:'C'}}");
		shouldMatch(p, "/A/B/C/", "{v:{a:'A',b:'B',c:'C'},r:''}");
	}

	@Test
	public void d06_3vars_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/{a}/{b}/{c}");
		shouldNotMatch(p, "/A/B");
		shouldNotMatch(p, "/A/B/C/D");
		shouldNotMatch(p, "/");
	}

	@Test
	public void d07_7parts3vars_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/a/{a}/b/{b}/c/{c}/d");
		shouldMatch(p, "/a/A/b/B/c/C/d", "{v:{a:'A',b:'B',c:'C'}}");
		shouldMatch(p, "/a/A/b/B/c/C/d/", "{v:{a:'A',b:'B',c:'C'},r:''}");
	}

	@Test
	public void d08_6parts3vars_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/a/{a}/b/{b}/c/{c}/d");
		shouldNotMatch(p, "/a/A/a/B/c/C/d");
		shouldNotMatch(p, "/a/A/b/B/c/C/dd");
		shouldNotMatch(p, "/a/b/c/d");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pattern with variables and remainder
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_1part1vars_withRemainder_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/{foo}/*");
		shouldMatch(p, "/bar", "{v:{foo:'bar'}}");
		shouldMatch(p, "/bar/", "{v:{foo:'bar'},r:''}");
		shouldMatch(p, "/bar/baz", "{v:{foo:'bar'},r:'baz'}");
		shouldMatch(p, "/bar/baz/qux", "{v:{foo:'bar'},r:'baz/qux'}");
	}

	@Test
	public void e02_1part1var_withRemainder_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/{foo}/*");
		shouldNotMatch(p, "/");
	}

	@Test
	public void e03_2parts1var_withRemainder_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/{bar}/*");
		shouldMatch(p, "/foo/baz", "{v:{bar:'baz'}}");
		shouldMatch(p, "/foo/baz/", "{v:{bar:'baz'},r:''}");
		shouldMatch(p, "/foo/baz/qux/", "{v:{bar:'baz'},r:'qux/'}");
	}

	@Test
	public void e04_2parts1var_withRemainder_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/{bar}/*");
		shouldNotMatch(p, "/fooo/baz");
		shouldNotMatch(p, "/fo/baz");
		shouldNotMatch(p, "/foo");
		shouldNotMatch(p, "/");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pattern with inner meta
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_innerMeta_withRemainder_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/*/*");
		shouldMatch(p, "/bar", "{}");
		shouldMatch(p, "/bar/", "{r:''}");
		shouldMatch(p, "/bar/baz", "{r:'baz'}");
		shouldMatch(p, "/bar/baz/qux", "{r:'baz/qux'}");
	}

	@Test
	public void f02_innerMeta_withRemainder_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/*/*");
		shouldNotMatch(p, "/");
	}

	@Test
	public void f03_innerMeta_withRemainder_match() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/*/bar/*");
		shouldMatch(p, "/foo/baz/bar", "{}");
		shouldMatch(p, "/foo/baz/bar/", "{r:''}");
		shouldMatch(p, "/foo/baz/bar/qux/", "{r:'qux/'}");
	}

	@Test
	public void f04_innerMeta_withRemainder_noMatch() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/*/bar/*");
		shouldNotMatch(p, "/fooo/baz/bar");
		shouldNotMatch(p, "/fo/baz/bar");
		shouldNotMatch(p, "/foo/bar");
		shouldNotMatch(p, "/");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Paths with encoded vars
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_encodedVars() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/{bar}/*");
		shouldMatch(p, "/foo/baz%2Fqux", "{v:{bar:'baz/qux'}}");
		shouldMatch(p, "/foo/baz+qux", "{v:{bar:'baz qux'}}");
		shouldMatch(p, "/foo/baz%2Fqux/quux%2Fquuux", "{v:{bar:'baz/qux'},r:'quux%2Fquuux'}");
		shouldMatch(p, "/foo/baz+qux/quux+quuux", "{v:{bar:'baz qux'},r:'quux+quuux'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Paths with not vars
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void h01_notVars() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/{bar/*");
		shouldMatch(p, "/foo/{bar", "{}");
		shouldMatch(p, "/foo/{bar/{baz", "{r:'{baz'}");
	}

	@Test
	public void h02_notVars() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/bar}/*");
		shouldMatch(p, "/foo/bar}", "{}");
		shouldMatch(p, "/foo/bar}/baz}", "{r:'baz}'}");
	}

	@Test
	public void h03_notVars() throws Exception {
		UrlPathPattern p = new UrlPathPattern("/foo/x{bar}x/*");
		shouldMatch(p, "/foo/x{bar}x", "{}");
		shouldMatch(p, "/foo/x{bar}x/x{baz}x", "{r:'x{baz}x'}");
	}
}
