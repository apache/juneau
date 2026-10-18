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
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Guards for the shipped {@code juneau-theme-dark.css} neutral layer: its shape, that it declares only known
 * colour custom properties with grammar-valid values, and that no colour token is left light by accident.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S8786" // test-only regex over a short CSS fixture
})
class DarkThemeCss_Test extends TestBase {

	private static final String DARK_RESOURCE = "/org/apache/juneau/console/juneau-theme-dark.css";

	private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

	private static final Pattern WRAPPER = Pattern.compile(
		"^\\s*@media not print\\s*\\{\\s*html:root\\[data-jc-scheme=\"dark\"\\]\\s*\\{(.*)\\}\\s*\\}\\s*$", Pattern.DOTALL);

	/**
	 * Colour tokens the dark layer deliberately leaves alone: accents (owned by the theme or app dark block), the
	 * danger/success fills, the white and on-* inks, and derived aliases that follow a live {@code var()} to a
	 * token this layer does redefine or deliberately leaves.
	 */
	private static final Set<String> ACCENT_OR_INVARIANT = Set.of(
		"--jc-accent", "--jc-accent-wash", "--jc-link", "--jc-focus", "--jc-accent-selected",
		"--jc-btn-primary", "--jc-btn-primary-hover", "--jc-avatar-bg", "--jc-danger", "--jc-success", "--jc-white",
		"--jc-on-accent", "--jc-on-btn-primary",
		// Derived aliases that stay live.
		"--jc-btn-primary-bg", "--jc-btn-primary-bg-hover", "--jc-page-nav-accent", "--jc-nav-bg",
		"--jc-header-icon-text", "--jc-tone-info", "--jc-tone-success", "--jc-tone-warning", "--jc-tone-error",
		"--jc-chrome-button-border", "--jc-chrome-disabled-border", "--jc-chrome-control-border");

	private static String read(String resource) throws IOException {
		try (var in = DarkThemeCss_Test.class.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing classpath resource: " + resource);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String readSibling(String relative) throws IOException {
		var f = Path.of(System.getProperty("basedir", "."), relative);
		assertTrue(Files.isRegularFile(f), () -> "missing sibling resource: " + f.toAbsolutePath().normalize());
		return Files.readString(f, UTF_8);
	}

	private static final String VIEWS_CSS =
		"../juneau-rest-server-views/src/main/resources/org/apache/juneau/views/juneau-views.css";

	/** The declarations of the dark block, in source order. */
	private static LinkedHashMap<String,String> darkDecls() throws IOException {
		var css = COMMENT.matcher(read(DARK_RESOURCE)).replaceAll("");
		var m = WRAPPER.matcher(css);
		assertTrue(m.matches(), () -> "dark file is not exactly one @media not print { html:root[data-jc-scheme=\"dark\"] { ... } }:\n" + css);
		return decls(m.group(1));
	}

	private static LinkedHashMap<String,String> decls(String body) {
		var out = new LinkedHashMap<String,String>();
		for (var decl : body.split(";")) {
			var d = decl.trim();
			if (d.isEmpty())
				continue;
			var colon = d.indexOf(':');
			assertTrue(colon > 0, () -> "malformed declaration: '" + d + "'");
			// A later duplicate wins, as in the cascade.
			out.put(d.substring(0, colon).trim(), d.substring(colon + 1).trim());
		}
		return out;
	}

	/** The first {@code :root{}} block of the views stylesheet. */
	private static LinkedHashMap<String,String> viewsRoot() throws IOException {
		var css = COMMENT.matcher(readSibling(VIEWS_CSS)).replaceAll("");
		var m = Pattern.compile("(?m)^:root\\s*\\{").matcher(css);
		assertTrue(m.find(), "views stylesheet has no :root block");
		var start = m.end();
		return decls(css.substring(start, css.indexOf("\n}", start)));
	}

	private static LinkedHashMap<String,String> aliases() {
		return decls(ConsoleChromeMixin.OPEN_ROLE_ALIASES);
	}

	private static Set<String> chartsFallbackKeys() throws IOException {
		var js = read("/org/apache/juneau/console/juneau-charts.js");
		var m = Pattern.compile("const TOKEN_FALLBACKS = Object\\.freeze\\(\\{(.*?)\\}\\);", Pattern.DOTALL).matcher(js);
		assertTrue(m.find(), "juneau-charts.js has no TOKEN_FALLBACKS");
		var out = new LinkedHashSet<String>();
		var k = Pattern.compile("\"(--jc-[a-z0-9-]+)\"\\s*:").matcher(m.group(1));
		while (k.find())
			out.add(k.group(1));
		return out;
	}

	private static final Pattern VAR_ONLY = Pattern.compile("^var\\((--jc-[a-z0-9-]+)\\)$");
	private static final Pattern VAR_WITH_FALLBACK = Pattern.compile("^var\\((--jc-[a-z0-9-]+)\\s*,\\s*(.+)\\)$");
	private static final Pattern COLOUR = Pattern.compile(
		"^(#[0-9a-fA-F]{3,8}|(?i:rgba?|hsla?)\\([^)]*\\)|transparent|linear-gradient\\(.*\\))$");

	/** True when the value is a colour or a {@code var()} chain that ends in one. */
	private static boolean isColour(String value, Map<String,String> all, int depth) {
		if (depth > 10)
			return false;
		var v = value.trim();
		var m = VAR_ONLY.matcher(v);
		if (m.matches()) {
			var target = all.get(m.group(1));
			return target != null && isColour(target, all, depth + 1);
		}
		m = VAR_WITH_FALLBACK.matcher(v);
		if (m.matches()) {
			var target = all.get(m.group(1));
			return isColour(target != null ? target : m.group(2), all, depth + 1);
		}
		return COLOUR.matcher(v).matches();
	}

	@Test void a01_shape() throws Exception {
		var d = darkDecls();
		assertEquals("color-scheme", d.keySet().iterator().next());
		assertEquals("dark", d.get("color-scheme"));
	}

	@Test void a02_onlyCustomPropertiesBesidesColorScheme() throws Exception {
		for (var name : darkDecls().keySet())
			assertTrue(name.equals("color-scheme") || name.startsWith("--jc-"), () -> "unexpected declaration: " + name);
	}

	@Test void a03_valuesPassGrammar() throws Exception {
		for (var e : darkDecls().entrySet()) {
			if (e.getKey().equals("color-scheme") || VAR_ONLY.matcher(e.getValue()).matches())
				continue;
			var key = e.getKey();
			var val = e.getValue();
			assertDoesNotThrow(() -> CssValueGrammar.normalizeAndValidate(val), () -> key + ": " + val);
		}
	}

	@Test void a04_namesKnown() throws Exception {
		var known = new LinkedHashSet<String>(Theme.OPEN.getTokens().keySet());
		known.addAll(aliases().keySet());
		known.addAll(viewsRoot().keySet());
		known.addAll(chartsFallbackKeys());
		for (var name : darkDecls().keySet())
			if (! name.equals("color-scheme"))
				assertTrue(known.contains(name), () -> "unknown token declared in the dark file: " + name);
	}

	@Test void a05_noDimensions() throws Exception {
		var dim = Pattern.compile("\\d(px|rem|em)\\b");
		for (var e : darkDecls().entrySet()) {
			if (e.getKey().endsWith("-shadow"))
				continue;
			assertFalse(dim.matcher(e.getValue()).find(), () -> "dimension in " + e.getKey() + ": " + e.getValue());
		}
	}

	@Test void a06_completeness() throws Exception {
		var all = new LinkedHashMap<String,String>(Theme.OPEN.getTokens());
		all.putAll(aliases());
		all.putAll(viewsRoot());
		var declared = darkDecls().keySet();
		var missing = new TreeSet<String>();
		for (var e : all.entrySet())
			if (isColour(e.getValue(), all, 0) && ! declared.contains(e.getKey()) && ! ACCENT_OR_INVARIANT.contains(e.getKey()))
				missing.add(e.getKey());
		assertEquals(Set.of(), missing, "colour tokens neither redefined in the dark file nor on the accent/invariant list");
	}

	@Test void a07_tagsStayLive() throws Exception {
		var d = darkDecls();
		var count = 0;
		for (var c : List.of("green", "blue", "amber", "neutral", "red"))
			for (var part : List.of("bg", "text", "border")) {
				assertEquals("var(--jc-pill-" + c + "-" + part + ")", d.get("--jc-tag-" + c + "-" + part));
				count++;
			}
		assertEquals(15, count);
	}

	@Test void a08_valuesMatchSpec() throws Exception {
		var d = darkDecls();
		var expected = new LinkedHashMap<String,String>();
		expected.put("--jc-main-bg", "#16181d");
		expected.put("--jc-surface", "#24272e");
		expected.put("--jc-text", "#e6e8eb");
		expected.put("--jc-text-muted", "#a3a9b3");
		expected.put("--jc-control-border", "#7a818c");
		expected.put("--jc-placeholder", "#7d838d");
		expected.put("--jc-danger-text", "#f07a75");
		expected.put("--jc-success-text", "#5cc275");
		expected.put("--jc-warn", "#e8a33d");
		expected.put("--jc-chart-1", "#5b9cf0");
		expected.put("--jc-term-bg", "#0d0f12");
		expected.put("--jc-on-tone", "#16181d");
		expected.forEach((k, v) -> assertEquals(v, d.get(k), k));
	}
}
