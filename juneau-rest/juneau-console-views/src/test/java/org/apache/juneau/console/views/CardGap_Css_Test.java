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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Guards the vertical gap between stacked cards, at the top level and inside a switcher view.
 */
class CardGap_Css_Test extends TestBase {

	private static String css(String path) throws IOException {
		try (var in = CardGap_Css_Test.class.getResourceAsStream(path)) {
			assertNotNull(in, path + " not found on the test classpath");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String ruleBody(String css, String selector) {
		var m = Pattern.compile("(?m)^" + Pattern.quote(selector) + "\\s*\\{([^}]*)\\}").matcher(css);
		return m.find() ? m.group(1) : "";
	}

	@Test void a01_topLevelStackRule() throws IOException {
		var body = ruleBody(css("/org/apache/juneau/console/chrome.css"), ".jc-main > .jc-card + .jc-card");
		assertTrue(body.contains("margin-top: var(--jc-card-gap, 1rem)"), body);
	}

	@Test void a02_nestedStackRule() throws IOException {
		var body = ruleBody(css("/org/apache/juneau/console/chrome.css"),
			".juneau-switcher-view > .jc-card[data-juneau-in-switcher] + .jc-card[data-juneau-in-switcher]");
		assertTrue(body.contains("margin-top: var(--jc-card-gap, 1rem)"), body);
	}

	@Test void a03_tokenDeclaredOnce() throws IOException {
		var m = Pattern.compile("--jc-card-gap:").matcher(css("/org/apache/juneau/views/juneau-views.css"));
		var n = 0;
		while (m.find()) n++;
		assertEquals(1, n);
	}

	@Test void b01_stackedViewGap() throws IOException {
		assertTrue(css("/org/apache/juneau/console/chrome.css").contains(".juneau-switcher-view > .jc-card[data-juneau-in-switcher]"));
		assertTrue(css("/org/apache/juneau/console/juneau-switcher.js").contains("\"juneau-switcher-view\""));
	}
}
