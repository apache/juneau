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
import org.junit.jupiter.api.*;

/**
 * Pins the run-view section of {@code juneau-views.css}: the class surface the module emits, the status and
 * state tones, and the "no literal colours, no new tokens" rule.
 */
class RunViewCss_Test extends TestBase {

	private static final String MARKER = ".juneau-rv {";

	private static String css;
	private static String section;
	private static String before;
	private static Map<String,Map<String,String>> rules;

	@BeforeAll static void load() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_CSS_RESOURCE)) {
			assertNotNull(in, "missing " + ViewsMixin.VIEWS_CSS_RESOURCE);
			css = new String(in.readAllBytes(), UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
		}
		var start = css.indexOf(MARKER);
		assertTrue(start > 0, "run-view section missing");
		before = css.substring(0, start);
		section = css.substring(start);
		rules = new LinkedHashMap<>();
		var m = Pattern.compile("([^{}]+)\\{([^{}]*)\\}").matcher(section);
		while (m.find()) {
			var decls = new LinkedHashMap<String,String>();
			for (var d : m.group(2).split(";")) {
				var i = d.indexOf(':');
				if (i > 0)
					decls.put(d.substring(0, i).trim(), d.substring(i + 1).trim().replaceAll("\\s+", " "));
			}
			for (var s : m.group(1).split(","))
				rules.computeIfAbsent(s.trim().replaceAll("\\s+", " "), k -> new LinkedHashMap<>()).putAll(decls);
		}
	}

	private static Map<String,String> rule(String selector) {
		var r = rules.get(selector);
		assertNotNull(r, () -> "no rule for '" + selector + "'");
		return r;
	}

	private static void assertDecl(String selector, String property, String value) {
		assertEquals(value, rule(selector).get(property), () -> selector + " { " + property + " } in " + rule(selector));
	}

	@Test void a01_everyClassTheModuleEmitsHasARule() {
		for (var c : List.of("juneau-rv", "juneau-rv-summary", "juneau-rv-headline", "juneau-rv-counts", "juneau-rv-failures",
				"juneau-rv-steps", "juneau-rv-step", "juneau-rv-step-head", "juneau-rv-glyph", "juneau-rv-state-ok",
				"juneau-rv-state-fail", "juneau-rv-state-skip", "juneau-rv-state-running", "juneau-rv-state-waiting",
				"juneau-rv-suite", "juneau-rv-strip", "juneau-rv-notes", "juneau-rv-trace", "juneau-rv-compact"))
			assertTrue(rules.keySet().stream().anyMatch(s -> s.matches(".*\\." + Pattern.quote(c) + "(?![A-Za-z0-9-]).*")), () -> "no rule mentions ." + c);
	}

	@Test void a02_headlineToneByStatus() {
		assertDecl(".juneau-rv[data-juneau-rv-status=\"ok\"] .juneau-rv-headline", "color", "var(--jc-tone-success)");
		assertDecl(".juneau-rv[data-juneau-rv-status=\"fail\"] .juneau-rv-headline", "color", "var(--jc-tone-error)");
		assertDecl(".juneau-rv[data-juneau-rv-status=\"waiting\"] .juneau-rv-headline", "color", "var(--jc-tone-warning)");
		assertDecl(".juneau-rv[data-juneau-rv-status=\"running\"] .juneau-rv-headline", "color", "var(--jc-tone-info)");
		assertDecl(".juneau-rv[data-juneau-rv-status=\"cancelled\"] .juneau-rv-headline", "color", "var(--jc-text-muted)");
		assertDecl(".juneau-rv[data-juneau-rv-status=\"empty\"] .juneau-rv-headline", "color", "var(--jc-text-muted)");
	}

	@Test void a03_glyphToneByStepState() {
		assertDecl(".juneau-rv-state-ok", "color", "var(--jc-tone-success)");
		assertDecl(".juneau-rv-state-fail", "color", "var(--jc-tone-error)");
		assertDecl(".juneau-rv-state-skip", "color", "var(--jc-text-muted)");
		assertDecl(".juneau-rv-state-running", "color", "var(--jc-tone-info)");
		assertDecl(".juneau-rv-state-waiting", "color", "var(--jc-tone-warning)");
	}

	@Test void a04_listsAreUnstyledAndStripWraps() {
		assertDecl(".juneau-rv-steps", "list-style", "none");
		assertDecl(".juneau-rv-failures", "list-style", "none");
		assertDecl(".juneau-rv-strip", "display", "flex");
		assertDecl(".juneau-rv-strip", "flex-wrap", "wrap");
	}

	@Test void a05_compactStepsTheFontDown() {
		assertDecl(".juneau-rv-compact", "font-size", "var(--jc-chrome-font-size-1)");
	}

	@Test void a06_focusRingsAndHiddenOverride() {
		assertDecl(".juneau-rv button:focus-visible", "outline", "2px solid var(--jc-tone-info)");
		assertDecl(".juneau-rv a:focus-visible", "outline", "2px solid var(--jc-tone-info)");
		assertDecl(".juneau-rv [hidden]", "display", "none !important");
	}

	@Test void a07_noLiteralColoursAndNoAnimation() {
		assertFalse(Pattern.compile("#[0-9A-Fa-f]{3,8}\\b").matcher(section).find(), "no hex literals");
		assertFalse(Pattern.compile("\\b(rgba?|hsla?)\\(").matcher(section).find(), "no rgb()/hsl() literals");
		assertFalse(section.contains("animation") || section.contains("transition"), "no animation");
	}

	@Test void a08_onlyTokensAlreadyUsedEarlierInTheFile() {
		var used = Pattern.compile("var\\((--[A-Za-z0-9-]+)").matcher(section).results().map(r -> r.group(1)).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
		assertFalse(used.isEmpty());
		for (var t : used)
			assertTrue(before.contains("var(" + t), () -> "token " + t + " is not used earlier in the stylesheet");
	}

	@Test void a09_placedAfterTheWholeConsoleOutputBlock() {
		assertTrue(before.contains(".juneau-co [hidden]"), "run-view block must follow the console-output block");
		assertTrue(before.contains(".juneau-co-compact"));
	}
}
