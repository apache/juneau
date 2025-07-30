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
import static org.apache.juneau.rest.util.UrlPathMatcher.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class UrlPathMatcher_Test {

	private void check(UrlPathMatcher p, String path, String expected) {
		assertObject(p.match(UrlPath.of(path))).asString().is(expected);
	}

	private void shouldNotMatch(UrlPathMatcher p, String...paths) {
		for (String path : paths)
			assertObject(p.match(UrlPath.of(path))).setMsg("path={0}", path).asJson().is("null");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_comparision() throws Exception {
		List<UrlPathMatcher> l = new LinkedList<>();

		l.add(of(""));
		l.add(of("*"));
		l.add(of("/"));
		l.add(of("/*"));
		l.add(of("/foo"));
		l.add(of("/foo/*"));
		l.add(of("/foo/bar"));
		l.add(of("/foo/bar/*"));
		l.add(of("/foo/{id}"));
		l.add(of("/foo/{id}/*"));
		l.add(of("/foo/{id}/bar"));
		l.add(of("/foo/{id}/bar/*"));
		l.add(of("foo.txt"));
		l.add(of("*.txt"));
		l.add(of("foo.*"));

		Collections.sort(l);
		assertEquals("['foo.txt','foo.*','*.txt','/foo/bar','/foo/bar/*','/foo/{id}/bar','/foo/{id}/bar/*','/foo/{id}','/foo/{id}/*','/foo','/foo/*','/','/*','','*']", Json5Serializer.DEFAULT.toString(l));
	}

	@Test
	public void a02_comparision() throws Exception {
		List<UrlPathMatcher> l = new LinkedList<>();

		l.add(of("foo.txt"));
		l.add(of("*.txt"));
		l.add(of("foo.*"));
		l.add(of("/foo/{id}/bar/*"));
		l.add(of("/foo/{id}/bar"));
		l.add(of("/foo/{id}/*"));
		l.add(of("/foo/{id}"));
		l.add(of("/foo/bar/*"));
		l.add(of("/foo/bar"));
		l.add(of("/foo/*"));
		l.add(of("/foo"));
		l.add(of("/*"));
		l.add(of("/"));
		l.add(of("*"));
		l.add(of(""));

		Collections.sort(l);
		assertEquals("['foo.txt','foo.*','*.txt','/foo/bar','/foo/bar/*','/foo/{id}/bar','/foo/{id}/bar/*','/foo/{id}','/foo/{id}/*','/foo','/foo/*','/','/*','','*']", Json5Serializer.DEFAULT.toString(l));
	}

	@Test
	public void a03_comparision() throws Exception {
		List<UrlPathMatcher> l = new LinkedList<>();

		l.add(of("/foo"));
		l.add(of("/foo"));

		Collections.sort(l);
		assertEquals("['/foo','/foo']", Json5Serializer.DEFAULT.toString(l));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Simple pattern matching
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_simple_match() throws Exception {
		UrlPathMatcher p = of("/foo");
		check(p, "/foo", "{}");
		check(p, "/foo/", "{r:''}");
	}

	@Test
	public void b02_simple_noMatch() throws Exception {
		UrlPathMatcher p = of("/foo");
		shouldNotMatch(p, "/fooo", "/fo", "/fooo/", "/", "/foo/bar");
	}

	@Test
	public void b03_simple_match_2parts() throws Exception {
		UrlPathMatcher p = of("/foo/bar");
		check(p, "/foo/bar", "{}");
		check(p, "/foo/bar/", "{r:''}");
	}

	@Test
	public void b04_simple_noMatch_2parts() throws Exception {
		UrlPathMatcher p = of("/foo/bar");
		shouldNotMatch(p, "/foo", "/foo/baz", "/foo/barr", "/foo/bar/baz");
	}

	@Test
	public void b05_simple_match_0parts() throws Exception {
		UrlPathMatcher p = of("/");
		check(p, "/", "{r:''}");
	}

	@Test
	public void b06_simple_noMatch_0parts() throws Exception {
		UrlPathMatcher p = of("/");
		shouldNotMatch(p, "/foo", "/foo/bar");
	}

	@Test
	public void b07_simple_match_blank() throws Exception {
		UrlPathMatcher p = of("");
		check(p, "/", "{r:''}");
	}

	@Test
	public void b08_simple_noMatch_blank() throws Exception {
		UrlPathMatcher p = of("");
		shouldNotMatch(p, "/foo", "/foo/bar");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Simple pattern matching with remainder
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_simple_withRemainder_match() throws Exception {
		UrlPathMatcher p = of("/foo/*");
		check(p, "/foo", "{}");
		check(p, "/foo/", "{r:''}");
		check(p, "/foo/bar", "{r:'bar'}");
		check(p, "/foo/bar/", "{r:'bar/'}");
		check(p, "/foo/bar/baz", "{r:'bar/baz'}");
	}

	@Test
	public void c02_simple_withRemainder_noMatch() throws Exception {
		UrlPathMatcher p = of("/foo/*");
		shouldNotMatch(p, "/fooo", "/fo", "/fooo/", "/", "/fooo/bar");
	}

	@Test
	public void c03_simple_withRemainder_match_2parts() throws Exception {
		UrlPathMatcher p = of("/foo/bar/*");
		check(p, "/foo/bar", "{}");
		check(p, "/foo/bar/baz", "{r:'baz'}");
		check(p, "/foo/bar/baz/", "{r:'baz/'}");
	}

	@Test
	public void c04_simple_withRemainder_noMatch_2parts() throws Exception {
		UrlPathMatcher p = of("/foo/bar/*");
		shouldNotMatch(p, "/", "/foo", "/foo/baz", "/foo/barr", "/foo/barr/");
	}

	@Test
	public void c05_simple_withRemainder_match_0parts() throws Exception {
		UrlPathMatcher p = of("/*");
		check(p, "/", "{r:''}");
		check(p, "/foo", "{r:'foo'}");
		check(p, "/foo/bar", "{r:'foo/bar'}");
	}

	@Test
	public void c06_simple_withRemainder_match_blank() throws Exception {
		UrlPathMatcher p = of("*");
		check(p, "/", "{r:''}");
		check(p, "/foo", "{r:'foo'}");
		check(p, "/foo/bar", "{r:'foo/bar'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pattern with variables
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_1part1vars_match() throws Exception {
		UrlPathMatcher p = of("/{foo}");
		check(p, "/bar", "{v:{foo:'bar'}}");
		check(p, "/bar/", "{v:{foo:'bar'},r:''}");
	}

	@Test
	public void d02_1part1var_noMatch() throws Exception {
		UrlPathMatcher p = of("/{foo}");
		shouldNotMatch(p, "/foo/bar", "/");
	}

	@Test
	public void d03_2parts1var_match() throws Exception {
		UrlPathMatcher p = of("/foo/{bar}");
		check(p, "/foo/baz", "{v:{bar:'baz'}}");
		check(p, "/foo/baz/", "{v:{bar:'baz'},r:''}");
	}

	@Test
	public void d04_2parts1var_noMatch() throws Exception {
		UrlPathMatcher p = of("/foo/{bar}");
		shouldNotMatch(p, "/fooo/baz", "/fo/baz", "/foo", "/");
	}

	@Test
	public void d05_3vars_match() throws Exception {
		UrlPathMatcher p = of("/{a}/{b}/{c}");
		check(p, "/A/B/C", "{v:{a:'A',b:'B',c:'C'}}");
		check(p, "/A/B/C/", "{v:{a:'A',b:'B',c:'C'},r:''}");
	}

	@Test
	public void d06_3vars_noMatch() throws Exception {
		UrlPathMatcher p = of("/{a}/{b}/{c}");
		shouldNotMatch(p, "/A/B", "/A/B/C/D", "/");
	}

	@Test
	public void d07_7parts3vars_match() throws Exception {
		UrlPathMatcher p = of("/a/{a}/b/{b}/c/{c}/d");
		check(p, "/a/A/b/B/c/C/d", "{v:{a:'A',b:'B',c:'C'}}");
		check(p, "/a/A/b/B/c/C/d/", "{v:{a:'A',b:'B',c:'C'},r:''}");
	}

	@Test
	public void d08_6parts3vars_noMatch() throws Exception {
		UrlPathMatcher p = of("/a/{a}/b/{b}/c/{c}/d");
		shouldNotMatch(p, "/a/A/a/B/c/C/d", "/a/A/b/B/c/C/dd", "/a/b/c/d");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pattern with variables and remainder
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_1part1vars_withRemainder_match() throws Exception {
		UrlPathMatcher p = of("/{foo}/*");
		check(p, "/bar", "{v:{foo:'bar'}}");
		check(p, "/bar/", "{v:{foo:'bar'},r:''}");
		check(p, "/bar/baz", "{v:{foo:'bar'},r:'baz'}");
		check(p, "/bar/baz/qux", "{v:{foo:'bar'},r:'baz/qux'}");
	}

	@Test
	public void e02_1part1var_withRemainder_noMatch() throws Exception {
		UrlPathMatcher p = of("/{foo}/*");
		shouldNotMatch(p, "/");
	}

	@Test
	public void e03_2parts1var_withRemainder_match() throws Exception {
		UrlPathMatcher p = of("/foo/{bar}/*");
		check(p, "/foo/baz", "{v:{bar:'baz'}}");
		check(p, "/foo/baz/", "{v:{bar:'baz'},r:''}");
		check(p, "/foo/baz/qux/", "{v:{bar:'baz'},r:'qux/'}");
	}

	@Test
	public void e04_2parts1var_withRemainder_noMatch() throws Exception {
		UrlPathMatcher p = of("/foo/{bar}/*");
		shouldNotMatch(p, "/fooo/baz", "/fo/baz", "/foo", "/");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pattern with inner meta
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_innerMeta_withRemainder_match() throws Exception {
		UrlPathMatcher p = of("/*/*");
		check(p, "/bar", "{}");
		check(p, "/bar/", "{r:''}");
		check(p, "/bar/baz", "{r:'baz'}");
		check(p, "/bar/baz/qux", "{r:'baz/qux'}");
	}

	@Test
	public void f02_innerMeta_withRemainder_noMatch() throws Exception {
		UrlPathMatcher p = of("/*/*");
		shouldNotMatch(p, "/");
	}

	@Test
	public void f03_innerMeta_withRemainder_match() throws Exception {
		UrlPathMatcher p = of("/foo/*/bar/*");
		check(p, "/foo/baz/bar", "{}");
		check(p, "/foo/baz/bar/", "{r:''}");
		check(p, "/foo/baz/bar/qux/", "{r:'qux/'}");
	}

	@Test
	public void f04_innerMeta_withRemainder_noMatch() throws Exception {
		UrlPathMatcher p = of("/foo/*/bar/*");
		shouldNotMatch(p, "/fooo/baz/bar", "/fo/baz/bar", "/foo/bar", "/");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Paths with encoded vars
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_encodedVars() throws Exception {
		UrlPathMatcher p = of("/foo/{bar}/*");
		check(p, "/foo/baz%2Fqux", "{v:{bar:'baz/qux'}}");
		check(p, "/foo/baz+qux", "{v:{bar:'baz qux'}}");
		check(p, "/foo/baz%2Fqux/quux%2Fquuux", "{v:{bar:'baz/qux'},r:'quux%2Fquuux'}");
		check(p, "/foo/baz+qux/quux+quuux", "{v:{bar:'baz qux'},r:'quux+quuux'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Paths with not vars
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void h01_notVars() throws Exception {
		UrlPathMatcher p = of("/foo/{bar/*");
		check(p, "/foo/{bar", "{}");
		check(p, "/foo/{bar/{baz", "{r:'{baz'}");
	}

	@Test
	public void h02_notVars() throws Exception {
		UrlPathMatcher p = of("/foo/bar}/*");
		check(p, "/foo/bar}", "{}");
		check(p, "/foo/bar}/baz}", "{r:'baz}'}");
	}

	@Test
	public void h03_notVars() throws Exception {
		UrlPathMatcher p = of("/foo/x{bar}x/*");
		check(p, "/foo/x{bar}x", "{}");
		check(p, "/foo/x{bar}x/x{baz}x", "{r:'x{baz}x'}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Filename matches
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void i01_filenameMatcher() throws Exception {
		UrlPathMatcher p = of("foo.bar");
		check(p, "/foo.bar", "{}");
		check(p, "/foo/foo.bar", "{}");
		shouldNotMatch(p, "/foo.baz", "/foo", "/foo.barx", "/foo", "/foo.*", "/*.bar", "/*.*", "/*", null);

		p = of("*.bar");
		check(p, "/foo.bar", "{}");
		check(p, "/foo/foo.bar", "{}");
		check(p, "/*.bar", "{}");
		shouldNotMatch(p, "/foo.baz", "/foo", "/foo", "/foo.*", "/*.*", "/*", null);

		p = of("foo.*");
		check(p, "/foo.bar", "{}");
		check(p, "/foo/foo.bar", "{}");
		check(p, "/foo.*", "{}");
		shouldNotMatch(p, "/foo", "/foo", "/*.*", "/*", null);

		p = of("*.*");
		check(p, "/foo.bar", "{}");
		check(p, "/foo/foo.bar", "{}");
		check(p, "/foo.*", "{}");
		check(p, "/*.bar", "{}");
		check(p, "/*.*", "{}");
		shouldNotMatch(p, "/foo", "/foo", "/*", null);
	}
}