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
 * Drift guard for the shipped {@code juneau-theme-<name>.css} stock-theme packs the FTL {@code <@theme>}
 * directive links: each pack's {@code html:root{}} token block must be <b>provably identical</b> (same tokens,
 * same order, same values) to its {@link Theme} constant, so a change to a {@link Theme} that is not mirrored
 * into its pack file (or vice-versa) fails the build rather than silently serving a stale palette.
 *
 * @since 10.0.0
 */
class ThemePackAssets_Test extends TestBase {

	/** Parses a pack file's {@code html:root{ … }} block into an insertion-ordered token map. */
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
		try (var in = ThemePackAssets_Test.class.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing classpath resource: " + resource);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private void assertPackMatchesTheme(String name, Theme theme) throws IOException {
		var parsed = parse(read(name));
		// Same key set, same iteration order, same values as the Theme constant.
		assertEquals(new ArrayList<>(theme.getTokens().keySet()), new ArrayList<>(parsed.keySet()),
			() -> "token order/keys drift in juneau-theme-" + name + ".css");
		assertEquals(theme.getTokens(), parsed,
			() -> "token value drift in juneau-theme-" + name + ".css");
	}

	@Test void a01_open() throws Exception { assertPackMatchesTheme("open", Theme.OPEN); }
	@Test void a02_lightRed() throws Exception { assertPackMatchesTheme("light-red", Theme.LIGHT_RED); }
	@Test void a03_lightBrown() throws Exception { assertPackMatchesTheme("light-brown", Theme.LIGHT_BROWN); }
	@Test void a04_red() throws Exception { assertPackMatchesTheme("red", Theme.RED); }
	@Test void a05_gray() throws Exception { assertPackMatchesTheme("gray", Theme.GRAY); }

	@Test void a06_builtinNamesMatchThemeConstants() {
		assertEquals(
			List.of("open", "light-red", "light-brown", "red", "gray"),
			ConsoleChromeMixin.BUILTIN_THEME_NAMES);
	}
}
