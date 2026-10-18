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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The {@link Theme} dark-accent channel: {@link Theme.Builder#dark(String, String)}, {@link Theme#getDarkTokens()},
 * and the stock themes' dark accent values.
 */
class Theme_DarkChannel_Test extends TestBase {

	private static Map<String,String> map(String... kv) {
		var m = new LinkedHashMap<String,String>();
		for (var i = 0; i < kv.length; i += 2)
			m.put(kv[i], kv[i + 1]);
		return m;
	}

	@Test void a01_builderDark() {
		var theme = Theme.create("x").token("--jc-accent", "#123456").dark("--jc-link", "#6cb4ff").build();
		assertEquals(map("--jc-link", "#6cb4ff"), theme.getDarkTokens());
		var target = theme.getDarkTokens();
		assertThrows(UnsupportedOperationException.class, () -> target.put("--jc-focus", "#fff"));
	}

	@Test void a02_darkRejectsVar() {
		var b = Theme.create("x");
		assertThrows(IllegalArgumentException.class, () -> b.dark("--jc-link", "var(--jc-accent)"));
	}

	@Test void a03_darkReservedRejected() {
		var b = Theme.create("x");
		assertThrows(IllegalArgumentException.class, () -> b.dark("--jc-chrome-bg-x", "#000000"));
		// --jc-chrome-bg is a Theme token today, so the reserved guard allows it.
		assertEquals(map("--jc-chrome-bg", "#7a1a05"), b.dark("--jc-chrome-bg", "#7a1a05").build().getDarkTokens());
	}

	@Test void a04_darkGrammar() {
		var b = Theme.create("x");
		assertThrows(IllegalArgumentException.class, () -> b.dark("--jc-link", "red; }"));
		assertThrows(IllegalArgumentException.class, () -> b.dark("--jc-link", "url(http://x/y)"));
		assertThrows(IllegalArgumentException.class, () -> b.dark("link", "#fff"));
	}

	@Test void a05_stockValues() {
		var open = map(
			"--jc-link", "#6cb4ff",
			"--jc-focus", "#4aa3ff",
			"--jc-accent-selected", "#4aa3ff",
			"--jc-accent-wash", "rgba(21,137,238,0.22)");
		assertEquals(open, Theme.OPEN.getDarkTokens());
		assertEquals(open, Theme.GRAY.getDarkTokens());
		assertEquals(map(
			"--jc-link", "#d9a65c",
			"--jc-focus", "#a9772f",
			"--jc-accent-wash", "rgba(169,119,47,0.25)"), Theme.LIGHT_BROWN.getDarkTokens());
		assertEquals(map(
			"--jc-link", "#f27a9a",
			"--jc-accent", "#e0557a",
			"--jc-focus", "#e0557a",
			"--jc-accent-selected", "#e0557a",
			"--jc-accent-wash", "rgba(224,85,122,0.22)"), Theme.LIGHT_RED.getDarkTokens());
		assertEquals(map(
			"--jc-link", "#ff8a70",
			"--jc-chrome-bg", "#7a1a05",
			"--jc-accent-wash", "rgba(255,86,48,0.22)"), Theme.RED.getDarkTokens());
	}

	@Test void a06_deriveFromCarriesDark() {
		// GRAY is derived from OPEN and declares no dark accents of its own, so it inherits OPEN's.
		assertFalse(Theme.OPEN.getDarkTokens().isEmpty());
		assertEquals(new ArrayList<>(Theme.OPEN.getDarkTokens().entrySet()), new ArrayList<>(Theme.GRAY.getDarkTokens().entrySet()));
	}

	@Test void a07_darkNeverLeaksIntoLightTokens() {
		var theme = Theme.create("x").token("--jc-link", "#111111").dark("--jc-link", "#eeeeee").build();
		assertEquals("#111111", theme.getTokens().get("--jc-link"));
		assertEquals("#eeeeee", theme.getDarkTokens().get("--jc-link"));
	}
}
