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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Pins the wide-table "full horizontal real estate" CSS contract in {@code chrome.css}: the
 * {@code data-juneau-layout="wide"} stamp widens the wrapper {@code <div>} {@code ViewTable} emits, and the
 * ancestor card/main expand via {@code :has(...)}.
 *
 * <p>
 * {@code data-juneau-layout} is a first-class public {@code data-juneau-*} convention (allowed value: {@code wide};
 * set by {@code ViewTable} on its wrapper {@code <div>}; NOT stamped on {@code .jc-card} / {@code .jc-main}).
 * The wide-stamp matrix (which node gets widened, and when) is documented in the spec and asserted here.
 */
class ChromeCss_FullBleed_Test extends TestBase {

	private static String readChromeCss() throws IOException {
		try (var in = ChromeCss_FullBleed_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			assertNotNull(in, "missing classpath resource: /org/apache/juneau/console/chrome.css");
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String flat() throws IOException {
		return readChromeCss().replaceAll("\\s+", " ");
	}

	/** The stamp on the wrapper div widens it (self-match, not a descendant selector). */
	@Test void a01_wideStampWidensSelf() throws Exception {
		var c = flat();
		assertTrue(c.contains("[data-juneau-layout=\"wide\"]"),
			"the data-juneau-layout=\"wide\" stamp must have a widening rule");
		assertTrue(c.contains("[data-juneau-layout=\"wide\"] { max-width: none")
			|| ruleBodyContains(c, "[data-juneau-layout=\"wide\"]", "max-width: none"),
			"the wide stamp must set max-width:none on itself");
	}

	/**
	 * Returns whether the {@code selector { ... }} rule immediately following {@code selector} in {@code css} has
	 * a body containing {@code needle}. A plain substring scan (rather than a chained-quantifier regex) for this
	 * "selector, then eventually needle, before the closing brace" shape, since a regex here would need
	 * backtracking to bridge {@code needle}'s overlap with the body's not-yet-closing-brace characters.
	 */
	private static boolean ruleBodyContains(String css, String selector, String needle) {
		var selIdx = css.indexOf(selector);
		if (selIdx < 0)
			return false;
		var braceIdx = css.indexOf('{', selIdx + selector.length());
		if (braceIdx < 0)
			return false;
		var endIdx = css.indexOf('}', braceIdx + 1);
		return css.substring(braceIdx + 1, endIdx < 0 ? css.length() : endIdx).contains(needle);
	}

	/** The ancestor card widens only when the wide wrapper is a DIRECT child (position-dependent matrix). */
	@Test void a02_cardExpandsViaHasDirectChild() throws Exception {
		var c = flat();
		assertTrue(c.contains(".jc-card:has(> [data-juneau-layout=\"wide\"])"),
			"the card must expand via :has(> [data-juneau-layout=\"wide\"]) - direct-child only (wide-stamp matrix)");
	}

	/** The main region widens when any descendant wrapper is wide. */
	@Test void a03_mainExpandsViaHasDescendant() throws Exception {
		var c = flat();
		assertTrue(c.contains(".jc-main:has([data-juneau-layout=\"wide\"])"),
			"the main region must expand via :has([data-juneau-layout=\"wide\"])");
	}

	/** ViewTable never stamps .jc-card / .jc-main themselves - the selector must not key off those classes carrying the attr. */
	@Test void a04_doesNotKeyOffCardOrMainCarryingTheAttr() throws Exception {
		var c = flat();
		assertFalse(c.contains(".jc-card[data-juneau-layout"),
			"ViewTable never stamps .jc-card - do not match .jc-card[data-juneau-layout] (BLK-2)");
		assertFalse(c.contains(".jc-main[data-juneau-layout"),
			"ViewTable never stamps .jc-main - do not match .jc-main[data-juneau-layout]");
	}
}
