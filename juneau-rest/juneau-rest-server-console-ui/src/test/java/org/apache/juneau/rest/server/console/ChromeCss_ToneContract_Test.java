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

import java.io.*;
import java.nio.charset.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Resource-scan contract for the status-tone vocabulary in {@code chrome.css}: badge tone selectors use the
 * {@link StatusTone} wire tokens, each tone maps to its tag-palette triad, and the probe classes and tokens use the
 * same names.  Non-browser.
 */
class ChromeCss_ToneContract_Test extends TestBase {

	private static final String TONE_SELECTOR = ".jc-badge[data-juneau-badge-tone=\"%s\"]";

	@Test void a01_everyStatusToneHasABadgeSelector() throws IOException {
		var css = readChromeCss();
		for (var tone : StatusTone.values())
			assertTrue(css.contains(TONE_SELECTOR.formatted(tone.wire())), () -> "missing badge selector for " + tone.wire());
	}

	@Test void a02_retiredBadgeToneSelectorsAreGone() throws IOException {
		var css = readChromeCss();
		for (var retired : new String[] {"accent", "warn", "danger"})
			assertFalse(css.contains("data-juneau-badge-tone=\"" + retired + "\""), () -> "retired badge tone selector: " + retired);
	}

	@Test void a03_toneTriadsMapToTheTagPalette() throws IOException {
		var css = readChromeCss();
		assertRuleUses(css, StatusTone.INFO, "--jc-tag-blue-");
		assertRuleUses(css, StatusTone.SUCCESS, "--jc-tag-green-");
		assertRuleUses(css, StatusTone.NEUTRAL, "--jc-tag-neutral-");
		assertRuleUses(css, StatusTone.WARNING, "--jc-tag-amber-");
		assertRuleUses(css, StatusTone.ERROR, "--jc-tag-red-");
	}

	@Test void a04_probeClassesAndTokensUseStatusToneNames() throws IOException {
		var css = readChromeCss();
		for (var name : new String[] {"success", "warning", "error", "neutral"}) {
			assertTrue(css.contains(".jc-probe-" + name), () -> "missing probe class: " + name);
			assertTrue(css.contains("--jc-probe-" + name + "-bg"), () -> "missing probe token: " + name);
		}
		assertFalse(Pattern.compile("jc-probe-(ok|warn|fail)\\b").matcher(css).find(), "retired probe name remains");
	}

	private static void assertRuleUses(String css, StatusTone tone, String prefix) {
		var selector = TONE_SELECTOR.formatted(tone.wire());
		var start = css.indexOf(selector);
		assertTrue(start >= 0, () -> "missing badge selector for " + tone.wire());
		var end = css.indexOf('}', start);
		assertTrue(css.substring(start, end).contains(prefix), () -> tone.wire() + " badge must use " + prefix);
	}

	private static String readChromeCss() throws IOException {
		try (var in = ChromeCss_ToneContract_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			assertNotNull(in, "chrome.css classpath resource not found");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
