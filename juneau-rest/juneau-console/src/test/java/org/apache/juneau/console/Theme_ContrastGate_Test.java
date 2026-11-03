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
 * WCAG 1.4.11 gate for the "selected = hover" indicator (ribbon toggles, dialog section tabs).
 *
 * <p>
 * The selected/hover state is an accent edge plus the accent wash, so the edge carries the 3:1 non-text contrast.  For
 * every stock palette in both schemes, {@code --jc-accent} must be at least 3:1 against the rest background
 * ({@code --jc-card-bg}) and against the washed button ({@code --jc-accent-wash} composited over {@code --jc-card-bg}).
 * A failing palette fixes its {@code --jc-accent} (darker in light, lighter in dark), never this rule.
 *
 * <p>
 * The secondary-button edge ({@code --jc-control-border}, which is {@code --jc-border-2} in light) must reach 3:1 against the card.
 *
 * <p>
 * The label on the washed button is text, so {@code --jc-link} must reach 4.5:1 against the same composite.
 */
class Theme_ContrastGate_Test extends TestBase {

	private static final double MIN = 3.0;
	private static final double MIN_TEXT = 4.5;

	private static final Pattern HEX = Pattern.compile("^#([0-9a-fA-F]{6})$");
	private static final Pattern RGBA = Pattern.compile("^rgba\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*([0-9.]+)\\s*\\)$");

	/** The value of one declaration in the shared dark neutral layer. */
	private static String darkLayer(String name) throws IOException {
		try (var in = Theme_ContrastGate_Test.class.getResourceAsStream("/org/apache/juneau/console/juneau-theme-dark.css")) {
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

	/** {@code over} (an rgba() or opaque hex) composited onto the opaque {@code base}. */
	private static double[] over(String over, double[] base) {
		var m = RGBA.matcher(over.trim());
		if (! m.matches())
			return rgb(over);
		var a = Double.parseDouble(m.group(4));
		var out = new double[3];
		for (var i = 0; i < 3; i++)
			out[i] = Double.parseDouble(m.group(i + 1)) * a + base[i] * (1 - a);
		return out;
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

	private static void assertGate(Theme theme, boolean dark) throws IOException {
		var t = new LinkedHashMap<>(theme.getTokens());
		var card = t.get("--jc-card-bg");
		if (dark) {
			t.putAll(theme.getDarkTokens());
			card = darkLayer("--jc-card-bg");
		}
		var scheme = theme.getName() + (dark ? " dark" : " light");
		var accent = rgb(t.get("--jc-accent"));
		var cardRgb = rgb(card);
		var washed = over(t.get("--jc-accent-wash"), cardRgb);
		var vsCard = ratio(accent, cardRgb);
		var vsWash = ratio(accent, washed);
		assertTrue(vsCard >= MIN, () -> scheme + ": --jc-accent vs --jc-card-bg is " + String.format("%.2f", vsCard) + ":1");
		assertTrue(vsWash >= MIN, () -> scheme + ": --jc-accent vs the washed button is " + String.format("%.2f", vsWash) + ":1");
		// The label on that washed button is --jc-link (WCAG 1.4.3 text), not --jc-accent.
		var link = rgb(t.get("--jc-link"));
		var linkVsWash = ratio(link, washed);
		assertTrue(linkVsWash >= MIN_TEXT, () -> scheme + ": --jc-link vs the washed button is " + String.format("%.2f", linkVsWash) + ":1");
		// Secondary buttons: their fill is the surface, so the edge alone identifies them (WCAG 1.4.11).
		var edge = rgb(dark ? darkLayer("--jc-control-border") : t.get("--jc-border-2"));
		var edgeVsCard = ratio(edge, cardRgb);
		assertTrue(edgeVsCard >= MIN, () -> scheme + ": --jc-control-border vs --jc-card-bg is " + String.format("%.2f", edgeVsCard) + ":1");
	}

	@Test void a01_open() throws Exception { assertGate(Theme.OPEN, false); assertGate(Theme.OPEN, true); }
	@Test void a02_gray() throws Exception { assertGate(Theme.GRAY, false); assertGate(Theme.GRAY, true); }
	@Test void a03_lightBrown() throws Exception { assertGate(Theme.LIGHT_BROWN, false); assertGate(Theme.LIGHT_BROWN, true); }
	@Test void a04_lightRed() throws Exception { assertGate(Theme.LIGHT_RED, false); assertGate(Theme.LIGHT_RED, true); }
	@Test void a05_red() throws Exception { assertGate(Theme.RED, false); assertGate(Theme.RED, true); }

	@Test void a06_ratioMath() {
		assertEquals(21.0, ratio(rgb("#000000"), rgb("#ffffff")), 0.01);
		assertArrayEquals(new double[] { 127.5, 127.5, 127.5 }, over("rgba(255,255,255,0.5)", rgb("#000000")), 0.001);
	}
}
