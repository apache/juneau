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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Pins that every icon name the chrome scripts ask the registry for is registered in {@code juneau-icons.js},
 * every registered stem exists in {@code juneau-symbols.svg}, and every shipped symbol is reachable by a name
 * (WORK-J0557 - one shipped set, no second inline/CSS path).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A new call site that asks for an unregistered name fails a01 and names the missing icon:</jc>
 * 	<jc>//   resolveIcon("my_new_icon")  -&gt;  a01 expected [] but was [my_new_icon]</jc>
 * </p>
 */
class ViewsJs_IconCallSites_Test extends TestBase {

	private static final String BASE = "/org/apache/juneau/views/";
	private static final Pattern REG = Pattern.compile("\\breg\\(\"([^\"]+)\",\\s*\"([^\"]+)\"");
	private static final Pattern CALL = Pattern.compile("resolveIcon\\??\\.?\\(\\s*\"([A-Za-z0-9_-]+)\"");
	private static final Pattern PILL = Pattern.compile("pagingPillButton\\(\"[^\"]*\",\\s*\"([A-Za-z0-9_-]+)\"");
	private static final Pattern DEFAULTS = Pattern.compile("DEFAULT_ICONS = \\{([^}]*)\\}");
	private static final Pattern DEFAULT_VALUE = Pattern.compile(":\\s*\"([A-Za-z0-9_-]+)\"");
	private static final Pattern SYMBOL = Pattern.compile("<symbol\\s+id=\"juneau-sym-([^\"]+)\"");

	private static String res(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(BASE + name)) {
			assertNotNull(in, () -> "missing resource " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static Set<String> all(Pattern p, String text, int group) {
		var out = new TreeSet<String>();
		var m = p.matcher(text);
		while (m.find())
			out.add(m.group(group));
		return out;
	}

	@Test void a01_everyCallSiteNameIsRegistered() throws Exception {
		var iconsJs = res("juneau-icons.js");
		var registered = all(REG, iconsJs, 1);
		var used = new TreeSet<String>();
		for (var f : new String[]{"juneau-views.js", "juneau-ribbon.js", "juneau-config.js", "juneau-helpers.js", "juneau-regions.js"}) {
			var src = res(f);
			used.addAll(all(CALL, src, 1));
			used.addAll(all(PILL, src, 1));
		}
		var defaults = DEFAULTS.matcher(res("juneau-ribbon.js"));
		assertTrue(defaults.find(),
			"the DEFAULT_ICONS table in juneau-ribbon.js no longer matches " + DEFAULTS.pattern()
				+ " - update the pattern so the ribbon's default icon names stay scanned");
		var defaultNames = all(DEFAULT_VALUE, defaults.group(1), 1);
		assertNotEmpty(() -> "DEFAULT_ICONS matched but no icon names were extracted from it", defaultNames);
		used.addAll(defaultNames);
		var missing = new TreeSet<>(used);
		missing.removeAll(registered);
		assertList(missing.stream().toList());
	}

	@Test void a02_everyRegisteredStemExistsInTheSprite() throws Exception {
		var stems = all(REG, res("juneau-icons.js"), 2);
		var shipped = all(SYMBOL, res("juneau-symbols.svg"), 1);
		var missing = new TreeSet<>(stems);
		missing.removeAll(shipped);
		assertList(missing.stream().toList());
	}

	@Test void a03_everyShippedSymbolHasAName() throws Exception {
		var stems = all(REG, res("juneau-icons.js"), 2);
		var shipped = all(SYMBOL, res("juneau-symbols.svg"), 1);
		var unnamed = new TreeSet<>(shipped);
		unnamed.removeAll(stems);
		assertList(unnamed.stream().toList());
	}

	// ---- U11 (unknown icons draw nothing): the second, non-sprite paths in juneau-views.js are gone.  These are
	// source pins; the behavioural proof for the ribbon button is ViewsJs_RibbonIconFallback_Test.

	/** Returns the body of {@code function name(...)} up to the next line that is a lone tab-indented closing brace. */
	private static String body(String src, String signature) {
		var start = src.indexOf(signature);
		assertTrue(start >= 0, () -> "function not found: " + signature);
		var end = src.indexOf("\n\t}\n", start);
		return src.substring(start, end);
	}

	@Test void a04_toolbarButtonHasNoTextFallbackAndKeepsItsAccessibleName() throws Exception {
		var fn = body(res("juneau-views.js"), "function toolbarButton(");
		assertFalse(fn.contains("textContent"), "toolbarButton must not fall back to label text");
		assertTrue(fn.contains("stampChromeTip(b, label)"), "toolbarButton must still stamp aria-label / tooltip");
	}

	@Test void a05_actionTriggerHasNoEllipsisFallbackAndKeepsItsAriaLabel() throws Exception {
		var src = res("juneau-views.js");
		var fn = body(src, "function actionTriggerMarkup(");
		assertFalse(fn.contains("u22EF") || fn.contains("\u22ef") || fn.contains("\u22EF"), "no ellipsis text fallback");
		assertTrue(fn.contains("resolveIcon(\"more_vert\")"), "trigger glyph resolves through the registry");
		assertTrue(fn.contains("aria-label=\"Row actions\""), "trigger keeps its accessible name");
	}

	@Test void a06_sortHeaderHasNoExpandMoreStandIn() throws Exception {
		var src = res("juneau-views.js");
		var at = src.indexOf("resolveIcon?.(\"sort\")");
		assertTrue(at >= 0, "sort header resolves the sort glyph");
		var window = src.substring(at, at + 200);
		assertFalse(window.contains("expand_more"), "no expand_more stand-in after the sort lookup");
		assertFalse(window.contains("||"), "no second resolution path after the sort lookup");
	}

	@Test void a07_rowExpandChevronsHaveNoUnicodeTriangleFallback() throws Exception {
		var fn = body(res("juneau-views.js"), "function detailsControlCellMarkup(");
		assertFalse(fn.contains("25B8") || fn.contains("25BE"), "no unicode triangle fallback");
		assertList(all(CALL, fn, 1).stream().toList(), "chevron_right", "expand_more");
	}

	@Test void a08_columnSearchGlyphResolvesOnlyThroughTheRegistry() throws Exception {
		var src = res("juneau-views.js");
		var at = src.indexOf("resolveIcon?.(\"search\")");
		assertTrue(at >= 0, "column search resolves the search glyph");
		assertFalse(src.substring(at, at + 120).contains("||"), "no second path after the search lookup");
	}
}
