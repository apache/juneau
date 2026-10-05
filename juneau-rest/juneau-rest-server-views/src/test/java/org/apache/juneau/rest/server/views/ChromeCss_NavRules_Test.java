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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Spec §5.6 and C1-D6: {@code chrome.css} carries the {@code .juneau-page-nav*} selectors, so pages that load
 * only the console chrome get the nav. The views copy was removed in the gated cleanup.
 * {@code ConsoleVisual_BrowserTest} proves the computed styles did not change.
 *
 * @since 10.0.0
 */
class ChromeCss_NavRules_Test extends TestBase {

	private static final Pattern RULE = Pattern.compile("([^{}]++)\\{[^}]*+\\}");

	private static String read(Class<?> anchor, String path) throws IOException {
		try (var in = anchor.getResourceAsStream(path)) {
			assertNotNull(in, path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String stripComments(String css) {
		return css.replaceAll("/\\*[^*]*+\\*++(?:[^/*][^*]*+\\*++)*+/", "");
	}

	// Every selector in the file that mentions .juneau-page-nav, one per comma-separated entry, whitespace-normalized.
	private static Set<String> navSelectors(String css) {
		var out = new TreeSet<String>();
		var m = RULE.matcher(stripComments(css));
		while (m.find())
			for (var s : m.group(1).split(","))
				if (s.contains(".juneau-page-nav"))
					out.add(s.trim().replaceAll("\\s++", " "));
		return out;
	}

	@Test void a01_navRulesLiveOnlyInChromeCss() throws Exception {
		var chrome = navSelectors(read(ConsoleChromeMixin.class, "/org/apache/juneau/console/chrome.css"));
		var views = navSelectors(read(ViewsMixin.class, "/org/apache/juneau/views/juneau-views.css"));
		assertFalse(chrome.isEmpty(), "chrome.css lost its nav rules");
		assertEquals(Set.of(), views, () -> "juneau-views.css still carries nav rules: " + views);
	}

	@Test void a02_newShellRules() throws Exception {
		var css = stripComments(read(ConsoleChromeMixin.class, "/org/apache/juneau/console/chrome.css"));
		for (var sel : List.of(".jc-brand-subtitle", ".jc-card-title", ".jc-console-error", ".jc-console-error-item",
				".jc-user-menu", ".jc-user-menu > .jc-menu"))
			assertTrue(Pattern.compile("(^|[},])\\s*+" + Pattern.quote(sel) + "\\s*+\\{").matcher(css).find(), () -> "missing rule " + sel);
	}
}
