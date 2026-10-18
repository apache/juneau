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
 * The views and config stylesheets spend {@code --jc-*} tokens (not light literals) on every surface a dark scheme
 * must repaint, declare the light defaults of the tokens the dark layer redeclares, and the console chrome uses
 * the text-ink roles for danger and success text.
 *
 * @since 10.0.0
 */
class ViewsCssTokens_Test extends TestBase {

	@SuppressWarnings({
		"java:S8786" // Test-only regex over a short fixture.
	})
	private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

	@SuppressWarnings({
		"java:S5852", // Possessive quantifiers: no backtracking; input is the module's own bundled stylesheet.
		"java:S8786" // Test-only regex over a short fixture.
	})
	private static final Pattern RULE = Pattern.compile("([^{}]++)\\{([^}]*+)\\}");

	private static final Pattern LITERAL_COLOR = Pattern.compile("#[0-9a-fA-F]{3,8}\\b|\\brgba?\\(|\\bhsla?\\(");

	private static final Pattern VAR_CALL = Pattern.compile("var\\([^()]*+\\)");

	private static String read(Class<?> anchor, String path) throws IOException {
		try (var in = anchor.getResourceAsStream(path)) {
			assertNotNull(in, path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String views() throws IOException {
		return read(ViewsMixin.class, "/org/apache/juneau/views/juneau-views.css");
	}

	private static String config() throws IOException {
		return read(ViewsMixin.class, "/org/apache/juneau/views/juneau-config.css");
	}

	private static String chrome() throws IOException {
		return read(ConsoleChromeMixin.class, "/org/apache/juneau/console/chrome.css");
	}

	private static String norm(String s) {
		return s.trim().replaceAll("\\s++", " ");
	}

	// Every declaration of 'prop' in a rule whose selector list contains exactly 'selector', whitespace-normalized.
	private static List<String> decls(String css, String selector, String prop) {
		var out = new ArrayList<String>();
		var m = RULE.matcher(COMMENT.matcher(css).replaceAll(""));
		while (m.find()) {
			var matches = false;
			for (var s : m.group(1).split(","))
				if (norm(s).equals(selector))
					matches = true;
			if (! matches)
				continue;
			for (var d : m.group(2).split(";")) {
				var i = d.indexOf(':');
				if (i > 0 && d.substring(0, i).trim().equals(prop))
					out.add(norm(d.substring(i + 1)));
			}
		}
		return out;
	}

	private static void assertTokenSpent(String css, String selector, String prop) {
		var values = decls(css, selector, prop);
		assertFalse(values.isEmpty(), () -> selector + " declares no " + prop);
		for (var v : values) {
			assertTrue(v.contains("var(--jc-"), () -> selector + " { " + prop + ": " + v + " } does not spend a --jc- token");
			var bare = VAR_CALL.matcher(v).replaceAll("");
			assertFalse(LITERAL_COLOR.matcher(bare).find(), () -> selector + " { " + prop + ": " + v + " } carries a bare colour literal");
		}
	}

	// The compact value of every :root declaration in the stylesheet.
	private static Map<String,String> rootDecls(String css) {
		var out = new LinkedHashMap<String,String>();
		var m = RULE.matcher(COMMENT.matcher(css).replaceAll(""));
		while (m.find()) {
			if (! norm(m.group(1)).equals(":root"))
				continue;
			for (var d : m.group(2).split(";")) {
				var i = d.indexOf(':');
				if (i > 0)
					out.put(d.substring(0, i).trim(), d.substring(i + 1).replaceAll("\\s++", ""));
			}
		}
		return out;
	}

	@Test void a01_newLightTokens() throws Exception {
		var root = rootDecls(views());
		var expected = new LinkedHashMap<String,String>();
		expected.put("--jc-label-text", "#555555");
		expected.put("--jc-placeholder", "#b4b5b6");
		expected.put("--jc-on-tone", "#ffffff");
		expected.put("--jc-term-bg", "#000000");
		expected.put("--jc-term-fg", "#e5e5e5");
		expected.put("--jc-term-cursor", "#e5e5e5");
		expected.put("--jc-term-selection", "rgba(255,255,255,0.3)");
		for (var e : expected.entrySet())
			assertEquals(e.getValue(), root.get(e.getKey()), e.getKey());
	}

	@Test void a02_noBareLightLiterals() throws Exception {
		var v = views();
		assertTokenSpent(v, "table.dataTable > thead > tr > td", "background-color");
		assertTokenSpent(v, "div.dt-container div.dt-search input", "background");
		assertTokenSpent(v, "div.dataTables_wrapper div.dataTables_filter input", "background");
		for (var sel : List.of(
				".juneau-view-toolbar-right .dataTables_filter input::placeholder",
				".juneau-view-toolbar-right .dataTables_filter input::-webkit-input-placeholder",
				".juneau-view-toolbar-right .dt-search input::placeholder",
				".juneau-view-toolbar-right .dt-search input::-webkit-input-placeholder",
				"div.dt-container div.dt-search input::placeholder",
				"div.dt-container div.dt-search input::-webkit-input-placeholder",
				"div.dataTables_wrapper div.dataTables_filter input::placeholder",
				"div.dataTables_wrapper div.dataTables_filter input::-webkit-input-placeholder"))
			assertTokenSpent(v, sel, "color");
		assertTokenSpent(v, ".juneau-view-dialog", "background");
		assertTokenSpent(v, ".juneau-ts-popup", "border");
		assertTokenSpent(v, ".juneau-ts-popup", "background");
		assertTokenSpent(v, ".juneau-ts-popup", "color");
		assertTokenSpent(v, ".jc-cell-popover", "color");
		assertTokenSpent(v, ".jc-cell-popover-label", "color");
		assertTokenSpent(v, ".juneau-view-card-status", "color");
		assertTokenSpent(v, ".juneau-view-card-field dt", "color");
		assertTokenSpent(config(), ".juneau-config-dialog", "background");
	}

	@Test void a03_textRolesUsed() throws Exception {
		var c = chrome();
		assertEquals(List.of("var(--jc-danger-text)"), decls(c, ".jc-btn-danger", "color"));
		assertEquals(List.of("var(--jc-success-text)"), decls(c, ".jc-btn-success", "color"));
		assertEquals(List.of("var(--jc-danger-text)"), decls(c, ".jc-editable-field-error", "color"));
		assertEquals(List.of("var(--jc-danger-text)"), decls(c, ".jc-toast.is-error", "color"));
		assertEquals(List.of("var(--jc-success-text)"), decls(c, ".jc-toast.is-success", "color"));
		assertEquals(List.of("var(--jc-danger-text)"), decls(c, ".juneau-view-dialog-dismiss:hover", "color"));
		assertEquals(List.of("var(--jc-danger-text)"), decls(c, ".jc-dash-error", "color"));
		assertEquals(List.of("var(--jc-danger)"), decls(c, ".juneau-view-dialog-confirm.juneau-view-dialog-confirm-danger", "background-color"));
		assertEquals(List.of("var(--jc-success)"), decls(c, ".juneau-view-toggle:checked", "background-color"));
		assertEquals(List.of("var(--jc-surface)"), decls(c, ".juneau-view-dialog-cancel", "background-color"));
		assertEquals(List.of("var(--jc-surface)"), decls(c, ".juneau-view-dialog-close", "background-color"));
	}

	@Test void a04_commentUpdated() throws Exception {
		assertFalse(views().contains("No dark console theme ships today"), "stale dark-theme comment");
		assertTrue(views().contains("juneau-theme-dark.css"), "dark-theme comment does not name the dark file");
	}
}
