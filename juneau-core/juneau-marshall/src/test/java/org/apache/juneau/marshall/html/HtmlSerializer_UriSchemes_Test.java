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
package org.apache.juneau.marshall.html;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Tests the link scheme allow-list of {@link HtmlSerializer}.
 */
class HtmlSerializer_UriSchemes_Test extends TestBase {

	public static class A {
		@Uri public String link;
		A(String s) { link = s; }
	}

	private static String ser(HtmlSerializer s, Object o) throws Exception {
		return s.write(o);
	}

	@Test void a01_allowedSchemesAreLinked() throws Exception {
		var s = HtmlSerializer.DEFAULT;
		for (var u : new String[] { "http://x/y", "https://x/y", "mailto:a@b.c", "tel:123", "/foo", "foo/bar", "#frag", "?q=1", "HTTPS://x/y" }) {
			var out = ser(s, new A(u));
			assertTrue(out.contains("<a href="), u + " -> " + out);
		}
	}

	@Test void a02_otherSchemesAreText() throws Exception {
		var s = HtmlSerializer.DEFAULT;
		for (var u : new String[] { "javascript:foo()", "JaVaScRiPt:foo()", "data:text/html,foo", "vbscript:foo", "file:///etc/x", "java\tscript:foo()", " javascript:foo()" }) {
			var out = ser(s, new A(u));
			assertFalse(out.contains("<a href="), u + " -> " + out);
			assertFalse(out.contains("href"), u + " -> " + out);
		}
	}

	@Test void a03_configurableSchemes() throws Exception {
		var s = HtmlSerializer.create().uriSchemes("ftp").build();
		assertTrue(ser(s, new A("ftp://x/y")).contains("<a href="));
		assertFalse(ser(s, new A("https://x/y")).contains("<a href="));
	}

	@Test void a04_noSchemesAllowsOnlyRelative() throws Exception {
		var s = HtmlSerializer.create().uriSchemes().build();
		assertFalse(ser(s, new A("https://x/y")).contains("<a href="));
		assertTrue(ser(s, new A("/x/y")).contains("<a href="));
	}

	@Test void a05_resolutionSchemesAreLinked() throws Exception {
		var s = HtmlSerializer.DEFAULT;
		for (var u : new String[] { "servlet:/foo?method=VIEW", "context:/foo", "request:/foo", "SERVLET:/foo" }) {
			var out = ser(s, new A(u));
			assertTrue(out.contains("<a href="), u + " -> " + out);
		}
		assertFalse(ser(s, new A("servlet2:/foo")).contains("<a href="));
	}
}
