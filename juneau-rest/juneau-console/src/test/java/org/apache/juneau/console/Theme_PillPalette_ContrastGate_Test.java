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
package org.apache.juneau.console;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Every pill palette text/background pair (status and categorical) must reach WCAG 1.4.3 text contrast, in the light
 * and the dark scheme of every stock theme.
 */
class Theme_PillPalette_ContrastGate_Test extends TestBase {

	private static final double MIN_TEXT = 4.5;

	private static final Pattern HEX = Pattern.compile("^#([0-9a-fA-F]{6})$");

	private static final List<String> NAMES = List.of(
		"green", "blue", "amber", "neutral", "red",
		"cat-1", "cat-2", "cat-3", "cat-4", "cat-5", "cat-6", "cat-7", "cat-8", "cat-9", "cat-10");

	/** The value of one declaration in the shared dark neutral layer. */
	private static String darkLayer(String name) throws IOException {
		try (var in = Theme_PillPalette_ContrastGate_Test.class.getResourceAsStream("/org/apache/juneau/console/juneau-theme-dark.css")) {
			assertNotNull(in);
			var m = Pattern.compile("(?m)^\\s*" + Pattern.quote(name) + "\\s*:\\s*([^;]+);").matcher(new String(in.readAllBytes(), UTF_8));
			assertTrue(m.find(), () -> "dark layer has no " + name);
			return m.group(1).trim();
		}
	}

	/** An opaque colour as {r, g, b} in 0..255. */
	private static double[] rgb(String v) {
		var m = HEX.matcher(v.trim());
		assertTrue(m.matches(), () -> "not an opaque #rrggbb colour: " + v);
		var h = Integer.parseInt(m.group(1), 16);
		return new double[] { h >> 16 & 0xff, h >> 8 & 0xff, h & 0xff };
	}

	private static double luminance(double[] c) {
		var l = new double[3];
		for (var i = 0; i < 3; i++) {
			var s = c[i] / 255;
			l[i] = s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
		}
		return 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2];
	}

	private static double ratio(double[] a, double[] b) {
		var la = luminance(a);
		var lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	private static void assertPairs(Theme theme, boolean dark) throws IOException {
		var scheme = theme.getName() + (dark ? " dark" : " light");
		for (var n : NAMES) {
			var bg = dark ? darkLayer("--jc-pill-" + n + "-bg") : theme.getTokens().get("--jc-pill-" + n + "-bg");
			var text = dark ? darkLayer("--jc-pill-" + n + "-text") : theme.getTokens().get("--jc-pill-" + n + "-text");
			assertNotNull(bg, () -> scheme + ": no --jc-pill-" + n + "-bg");
			assertNotNull(text, () -> scheme + ": no --jc-pill-" + n + "-text");
			var r = ratio(rgb(text), rgb(bg));
			assertTrue(r >= MIN_TEXT, () -> scheme + ": --jc-pill-" + n + "-text vs -bg is " + String.format("%.2f", r) + ":1");
		}
	}

	@Test void a01_open() throws Exception { assertPairs(Theme.OPEN, false); assertPairs(Theme.OPEN, true); }
	@Test void a02_gray() throws Exception { assertPairs(Theme.GRAY, false); assertPairs(Theme.GRAY, true); }
	@Test void a03_lightBrown() throws Exception { assertPairs(Theme.LIGHT_BROWN, false); assertPairs(Theme.LIGHT_BROWN, true); }
	@Test void a04_lightRed() throws Exception { assertPairs(Theme.LIGHT_RED, false); assertPairs(Theme.LIGHT_RED, true); }
	@Test void a05_red() throws Exception { assertPairs(Theme.RED, false); assertPairs(Theme.RED, true); }
}
