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
 * same order, same values) to its {@link Theme} constant, and the {@code @media not print} dark block that follows it must be
 * identical to {@link Theme#getDarkTokens()}, so a change to a {@link Theme} that is not mirrored
 * into its stylesheet (or vice-versa) fails the build rather than silently serving a stale palette.
 *
 * @since 10.0.0
 */
class StockThemeCss_Test extends TestBase {

	/** The selector that opens a stylesheet's dark accent block. */
	private static final String DARK_SELECTOR = "html:root[data-jc-scheme=\"dark\"]";

	/**
	 * Parses a stylesheet into its two token blocks: the {@code html:root{ ... }} light block, then the
	 * {@code @media not print{ html:root[data-jc-scheme="dark"]{ ... } }} dark block that must follow it.
	 *
	 * @return A two-element list: the light tokens, then the dark tokens, each insertion-ordered.
	 */
	private static List<LinkedHashMap<String,String>> parse(String rawCss) {
		// Strip block comments first - the drift-guard header mentions "html:root{}", whose braces would
		// otherwise fool the brace-locating below.
		var css = rawCss.replaceAll("(?s)/\\*.*?\\*/", "");
		var media = css.indexOf("@media not print");
		assertTrue(media > 0, () -> "no '@media not print' dark block after the light block:\n" + css);
		var light = css.substring(0, media);
		var open = light.indexOf('{');
		var close = light.lastIndexOf('}');
		assertTrue(open >= 0 && close > open, () -> "no html:root{} block:\n" + light);
		assertEquals("html:root", light.substring(0, open).trim(), () -> "light block must come first:\n" + light);
		var dark = css.substring(media);
		var sel = dark.indexOf(DARK_SELECTOR);
		assertTrue(sel > 0, () -> "no " + DARK_SELECTOR + " rule in the @media block:\n" + dark);
		var dOpen = dark.indexOf('{', sel);
		var dClose = dark.indexOf('}', dOpen);
		assertTrue(dOpen > sel && dClose > dOpen, () -> "unterminated dark block:\n" + dark);
		return List.of(
			declarations(light.substring(open + 1, close)),
			declarations(dark.substring(dOpen + 1, dClose)));
	}

	private static LinkedHashMap<String,String> declarations(String body) {
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
		var blocks = parse(read(name));
		var light = blocks.get(0);
		var dark = blocks.get(1);
		// Same key set, same iteration order, same values as the Theme constant, in each block.
		assertEquals(new ArrayList<>(light.keySet()), new ArrayList<>(theme.getTokens().keySet()),
			() -> "token order/keys drift in juneau-theme-" + name + ".css");
		assertEquals(light, theme.getTokens(),
			() -> "token value drift in juneau-theme-" + name + ".css");
		assertEquals(new ArrayList<>(dark.keySet()), new ArrayList<>(theme.getDarkTokens().keySet()),
			() -> "dark token order/keys drift in juneau-theme-" + name + ".css");
		assertEquals(dark, theme.getDarkTokens(),
			() -> "dark token value drift in juneau-theme-" + name + ".css");
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
			for (var parsed : parse(read(name))) {
				assertFalse(parsed.containsKey("--jc-header-bg"),
					() -> "juneau-theme-" + name + ".css must not declare --jc-header-bg");
				assertFalse(parsed.containsKey("--jc-nav-bg"),
					() -> "juneau-theme-" + name + ".css must not declare --jc-nav-bg");
			}
		}
	}

	@Test void a08_stockThemes_doNotClaimToBeGenerated() throws Exception {
		for (var name : ConsoleChromeMixin.BUILTIN_THEME_NAMES)
			assertFalse(read(name).toLowerCase().contains("generated"),
				() -> "juneau-theme-" + name + ".css is hand-maintained and must not claim to be generated");
	}
}
