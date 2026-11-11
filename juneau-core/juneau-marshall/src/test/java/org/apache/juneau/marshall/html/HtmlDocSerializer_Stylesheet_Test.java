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
import org.junit.jupiter.api.*;

/**
 * Tests that stylesheet page chrome is resolved like the sibling style/script chrome and that unsafe URIs are not emitted.
 */
class HtmlDocSerializer_Stylesheet_Test extends TestBase {

	private static String serialize(String...stylesheet) throws Exception {
		return HtmlDocSerializer.create().stylesheet(stylesheet).build().getSession().writeToString("x");
	}

	@Test void a01_normalStylesheet_emitted() throws Exception {
		var html = serialize("https://example.com/a.css", "/htdocs/b.css");
		assertTrue(html.contains("@import \"https://example.com/a.css\";"), () -> html);
		assertTrue(html.contains("@import \"/htdocs/b.css\";"), () -> html);
	}

	@Test void a02_quoteBreakout_dropped() throws Exception {
		var html = serialize("/a.css\"; } body { background:url(//evil.example/x) } /*");
		assertFalse(html.contains("evil.example"), () -> html);
		assertFalse(html.contains("@import"), () -> html);
	}

	@Test void a03_styleEndTag_dropped() throws Exception {
		var html = serialize("/a.css</style><script>alert(1)</script>");
		assertFalse(html.contains("alert(1)"), () -> html);
	}

	@Test void a04_dangerousScheme_dropped() throws Exception {
		var html = serialize("javascript:alert(1)", "data:text/css,body{}");
		assertFalse(html.contains("javascript:"), () -> html);
		assertFalse(html.contains("data:text/css"), () -> html);
	}
}
