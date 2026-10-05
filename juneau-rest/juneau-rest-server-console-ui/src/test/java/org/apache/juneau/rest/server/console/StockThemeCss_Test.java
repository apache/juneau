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
package org.apache.juneau.rest.server.console;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Drift guard for the shipped {@code juneau-theme-<name>.css} stock theme stylesheets the FTL {@code <@theme>}
 * directive links: each stylesheet's {@code html:root{}} token block must be <b>provably identical</b> (same tokens,
 * same order, same values) to its {@link Theme} constant, so a change to a {@link Theme} that is not mirrored
 * into its stylesheet (or vice-versa) fails the build rather than silently serving a stale palette.
 *
 * @since 10.0.0
 */
class StockThemeCss_Test extends TestBase {

	/** Parses a stylesheet's {@code html:root{ … }} block into an insertion-ordered token map. */
	private static LinkedHashMap<String,String> parse(String rawCss) {
		// Strip block comments first - the drift-guard header mentions "html:root{}", whose braces would
		// otherwise fool the brace-locating below.
		var css = rawCss.replaceAll("(?s)/\\*.*?\\*/", "");
		var open = css.indexOf('{');
		var close = css.lastIndexOf('}');
		assertTrue(open >= 0 && close > open, () -> "no html:root{} block:\n" + css);
		var body = css.substring(open + 1, close);
		var out = new LinkedHashMap<String,String>();
		for (var decl : body.split(";")) {
			var d = decl.trim();
			if (d.isEmpty())
				continue;
			var colon = d.indexOf(':');
			assertTrue(colon > 0, () -> "malformed declaration: '" + d + "'");
			out.put(d.substring(0, colon).trim(), d.substring(colon + 1).trim());
		}
		return out;
	}

	private static String read(String name) throws IOException {
		var resource = "/org/apache/juneau/console/juneau-theme-" + name + ".css";
		try (var in = StockThemeCss_Test.class.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing classpath resource: " + resource);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private void assertStockCssMatchesTheme(String name, Theme theme) throws IOException {
		var parsed = parse(read(name));
		// Same key set, same iteration order, same values as the Theme constant.
		assertEquals(new ArrayList<>(parsed.keySet()), new ArrayList<>(theme.getTokens().keySet()),
			() -> "token order/keys drift in juneau-theme-" + name + ".css");
		assertEquals(parsed, theme.getTokens(),
			() -> "token value drift in juneau-theme-" + name + ".css");
	}

	@Test void a01_open() throws Exception { assertStockCssMatchesTheme("open", Theme.OPEN); }
	@Test void a02_lightRed() throws Exception { assertStockCssMatchesTheme("light-red", Theme.LIGHT_RED); }
	@Test void a03_lightBrown() throws Exception { assertStockCssMatchesTheme("light-brown", Theme.LIGHT_BROWN); }
	@Test void a04_red() throws Exception { assertStockCssMatchesTheme("red", Theme.RED); }
	@Test void a05_gray() throws Exception { assertStockCssMatchesTheme("gray", Theme.GRAY); }

	@Test void a06_builtinNamesMatchThemeConstants() {
		assertEquals(
			ConsoleChromeMixin.BUILTIN_THEME_NAMES,
			List.of("open", "light-red", "light-brown", "red", "gray"));
	}

	@Test void a07_stockThemes_doNotDeclareHeaderOrNavBg() throws Exception {
		for (var name : ConsoleChromeMixin.BUILTIN_THEME_NAMES) {
			var parsed = parse(read(name));
			assertFalse(parsed.containsKey("--jc-header-bg"),
				() -> "juneau-theme-" + name + ".css must not declare --jc-header-bg");
			assertFalse(parsed.containsKey("--jc-nav-bg"),
				() -> "juneau-theme-" + name + ".css must not declare --jc-nav-bg");
		}
	}
}
