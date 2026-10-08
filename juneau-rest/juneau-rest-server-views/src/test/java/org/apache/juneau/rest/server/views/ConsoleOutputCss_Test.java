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
 * Pins the console-output section of {@code juneau-views.css}: the spec's style-to-token map, the block geometry,
 * the marker modes, the sticky control row, and the "no new palette colours" rule.
 */
class ConsoleOutputCss_Test extends TestBase {

	private static final String MUTED = "var(--jc-text-muted, #6b6b6b)";
	private static final String CO_MUTED = "var(--juneau-co-muted)";

	private static String css;
	private static Map<String,Map<String,String>> rules;

	@BeforeAll static void load() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_CSS_RESOURCE)) {
			assertNotNull(in, "missing " + ViewsMixin.VIEWS_CSS_RESOURCE);
			css = new String(in.readAllBytes(), UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
		}
		rules = parse(css);
	}

	/** Selector (each comma-separated selector, whitespace-normalised) to its declarations, later rules overriding. */
	private static Map<String,Map<String,String>> parse(String text) {
		var out = new LinkedHashMap<String,Map<String,String>>();
		var m = Pattern.compile("([^{}]+)\\{([^{}]*)\\}").matcher(text);
		while (m.find()) {
			var decls = new LinkedHashMap<String,String>();
			for (var d : m.group(2).split(";")) {
				var i = d.indexOf(':');
				if (i > 0)
					decls.put(d.substring(0, i).trim(), d.substring(i + 1).trim().replaceAll("\\s+", " "));
			}
			for (var s : m.group(1).split(","))
				out.computeIfAbsent(s.trim().replaceAll("\\s+", " "), k -> new LinkedHashMap<>()).putAll(decls);
		}
		return out;
	}

	private static Map<String,String> rule(String selector) {
		var r = rules.get(selector);
		assertNotNull(r, () -> "no rule for '" + selector + "'");
		return r;
	}

	private static void assertDecl(String selector, String property, String value) {
		assertEquals(value, rule(selector).get(property), () -> selector + " { " + property + " } in " + rule(selector));
	}

	private static List<String> consoleSelectors() {
		// juneau-co not followed by a letter, so the existing .juneau-code rules are not console rules.
		var console = Pattern.compile("\\.juneau-co(?![A-Za-z])");
		return rules.keySet().stream().filter(s -> console.matcher(s).find()).toList();
	}

	@Test void a01_baseSurfaceReusesCodeFontAndPopoverTokens() {
		assertDecl(".juneau-co", "font-family", "ui-monospace, SFMono-Regular, Consolas, \"Liberation Mono\", Menlo, monospace");
		assertDecl(".juneau-co", "font-size", "var(--jc-chrome-font-size-2)");
		assertDecl(".juneau-co", "line-height", "1.4");
		assertDecl(".juneau-co", "background", "var(--jc-popover-bg)");
		assertDecl(".juneau-co", "border", "1px solid var(--jc-popover-border)");
	}

	@Test void a02_rowsWrap() {
		assertDecl(".juneau-co-text", "white-space", "pre-wrap");
		assertDecl(".juneau-co-text", "overflow-wrap", "anywhere");
	}

	@Test void a03_styleToTokenMap() {
		assertDecl(".juneau-co-s-success", "color", "var(--jc-tone-success)");
		assertDecl(".juneau-co-s-warn", "color", "var(--jc-tone-warning)");
		assertDecl(".juneau-co-s-error", "color", "var(--jc-tone-error)");
		assertDecl(".juneau-co-s-muted", "color", CO_MUTED);
		assertDecl(".juneau-co-s-accent", "color", "var(--jc-tone-info)");
		assertDecl(".juneau-co-fill-success", "background-color", "var(--jc-tone-success)");
		assertDecl(".juneau-co-fill-warn", "background-color", "var(--jc-tone-warning)");
		assertDecl(".juneau-co-fill-error", "background-color", "var(--jc-tone-error)");
		assertDecl(".juneau-co-fill-muted", "background-color", CO_MUTED);
		assertDecl(".juneau-co-fill-accent", "background-color", "var(--jc-tone-info)");
	}

	@Test void a04_blockGeometryAndFocusRing() {
		assertDecl(".juneau-co-block", "display", "inline-block");
		assertDecl(".juneau-co-block", "width", "10px");
		assertDecl(".juneau-co-block", "height", "var(--jc-chrome-glyph-size-small)");
		assertDecl(".juneau-co-block", "vertical-align", "-1px");
		assertDecl(".juneau-co-block", "border-radius", "2px");
		assertDecl(".juneau-co-block:focus-visible", "outline", "2px solid var(--jc-tone-info)");
	}

	@Test void a05_boldFragment() {
		assertDecl(".juneau-co-b", "font-weight", "600");
	}

	@Test void a06_paneHeightResizeAndScroll() {
		var pane = rule(".juneau-co-pane");
		assertEquals("auto", pane.get("overflow-y"));
		assertEquals("vertical", pane.get("resize"));
		// The parser keeps the last height, which must be the lh form; the 1.4em fallback is declared first.
		assertEquals("calc(var(--juneau-co-rows, 20) * 1lh + 2 * var(--jc-space-1))", pane.get("height"));
		var from = css.substring(css.indexOf(".juneau-co-pane {"));
		var body = from.substring(0, from.indexOf('}'));
		assertTrue(body.indexOf("1.4em") >= 0 && body.indexOf("1.4em") < body.indexOf("1lh"), () -> "em fallback before lh: " + body);
	}

	@Test void a07_controlRowIsSticky() {
		assertDecl(".juneau-co-control", "position", "sticky");
		assertDecl(".juneau-co-control", "top", "0");
		assertDecl(".juneau-co-control", "color", MUTED);
		assertDecl(".juneau-co-control", "border-bottom", "1px solid var(--jc-popover-border)");
	}

	@Test void a08_markerModes() {
		assertDecl(".juneau-co-markers-dim .juneau-co-marker", "color", CO_MUTED);
		assertNull(rule(".juneau-co-markers-dim .juneau-co-marker").get("opacity"), "dim is a colour, not an opacity");
		assertDecl(".juneau-co-markers-hide .juneau-co-marker", "display", "none");
		assertDecl(".juneau-co-markers-hide .juneau-co-marker.juneau-co-reveal", "display", "flex");
	}

	@Test void a09_hiddenAttributeWins() {
		assertDecl(".juneau-co [hidden]", "display", "none !important");
		var last = consoleSelectors().get(consoleSelectors().size() - 1);
		assertEquals(".juneau-co [hidden]", last, "the [hidden] override must be the section's last rule");
	}

	@Test void a10_targetHighlightHasALeftBorder() {
		assertDecl(".juneau-co-target", "border-left-color", "var(--jc-tone-info)");
		assertNotNull(rule(".juneau-co-line").get("border-left"), "rows reserve the border so the target does not shift text");
	}

	@Test void a11_tooltip() {
		var tip = rule(".juneau-co-tooltip");
		assertEquals("fixed", tip.get("position"));
		assertEquals("pre-line", tip.get("white-space"));
		assertEquals("var(--jc-tip-z)", tip.get("z-index"));
		assertEquals("none", tip.get("pointer-events"));
	}

	@Test void a12_visuallyHiddenMatchesTheAnnouncer() {
		var sr = rule(".juneau-co-sr");
		var announcer = rule(".juneau-view-announcer");
		for (var k : List.of("position", "width", "height", "overflow", "clip-path", "white-space"))
			assertEquals(announcer.get(k), sr.get(k), k);
	}

	@Test void a13_compactStepsTheFontDown() {
		assertDecl(".juneau-co-compact", "font-size", "var(--jc-chrome-font-size-1)");
		assertDecl(".juneau-co-compact .juneau-co-title", "display", "none");
	}

	@Test void a14_imagesReserveSpaceAndFit() {
		assertDecl(".juneau-co-image", "max-width", "100%");
		assertDecl(".juneau-co-image", "min-height", "4em");
		assertDecl(".juneau-co-image", "object-fit", "contain");
	}

	@Test void a15_noNewPaletteColours() {
		var start = css.indexOf(".juneau-co {");
		assertTrue(start > 0, "section missing");
		var section = css.substring(start);
		var hex = Pattern.compile("#[0-9A-Fa-f]{3,8}\\b").matcher(section);
		var found = new ArrayList<String>();
		while (hex.find())
			found.add(hex.group());
		assertEquals(Set.of("#6b6b6b", "#5f5f5f"), new HashSet<>(found), () -> "only the muted fallback and the console muted token may be literals: " + found);
		assertFalse(section.matches("(?s).*\\brgba?\\(.*"), "no rgb()/rgba() literals");
	}

	@Test void a16_singleRootAndPrefix() {
		assertEquals(1, css.split(":root\\b", -1).length - 1, "juneau-views.css keeps a single :root");
		for (var s : consoleSelectors())
			for (var cls : Pattern.compile("\\.([A-Za-z0-9_-]+)").matcher(s).results().map(r -> r.group(1)).toList())
				assertTrue(cls.equals("juneau-co") || cls.startsWith("juneau-co-"), () -> "'" + s + "' uses a class outside the juneau-co- prefix: " + cls);
	}
}
