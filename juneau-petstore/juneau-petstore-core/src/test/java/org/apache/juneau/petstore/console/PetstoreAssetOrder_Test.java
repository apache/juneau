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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Build-time guard for the stylesheet/script asset order the console chrome emits for every petstore page.
 *
 * <p>
 * Ported from the deleted {@code AssetLoadOrderBand_Test}.  The console chrome (not this sample) owns the emitted
 * order, and it differs from the five-position band that test enforced: the chrome stylesheet and theme are linked
 * <i>first</i> (their {@code html:root} override block makes the cascade order irrelevant), then vendor, then
 * {@code juneau-views.css}, then widget-layer stylesheets, then any page-local {@code <style>}:
 *
 * <pre>
 * 1. console chrome.css and theme            (emitted first by the chrome)
 * 2. vendor stylesheets
 * 3. juneau-views.css                        (the views base layer)
 * 4. first-party widget stylesheets          (e.g. juneau-datatables.css, juneau-config.css)
 * 5. page-local &lt;style&gt;
 * </pre>
 *
 * <p>
 * The {@code b0x} methods are synthetic: they prove the guard can fail.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstoreAssetOrder_Test extends TestBase {

	private static final int CHROME_THEME = 1;
	private static final int VENDOR = 2;
	private static final int VIEWS_BASE = 3;
	private static final int WIDGET = 4;
	private static final int PAGE_LOCAL = 5;

	private static final Pattern LINK_HREF = Pattern.compile("<link\\s+rel=\"stylesheet\"\\s+href=\"([^\"]*)\"");
	private static final Pattern SCRIPT_SRC = Pattern.compile("<script\\s+src=\"([^\"]*)\"");
	private static final Pattern STYLE_TAG = Pattern.compile("<style>");

	/** One classified asset found in an emitted page, in document order. */
	private record BandHit(int position, String label, int index) {}

	/** Classifies a console stylesheet onto the band; unknown assets fail, so new ones get placed. */
	private static int cssBandPosition(String href) {
		if (href.contains("/juneau-console/chrome.css") || href.contains("/juneau-console/themes/"))
			return CHROME_THEME;
		if (href.contains("dataTables.dataTables") || href.contains("/vendor/"))
			return VENDOR;
		if (href.contains("/juneau-views.css"))
			return VIEWS_BASE;
		if (href.contains("/juneau-datatables.css") || href.contains("/juneau-widgets") || href.contains("/juneau-calendar.css") || href.contains("/juneau-config.css"))
			return WIDGET;
		throw new AssertionError("Unclassified stylesheet - the band doesn't name this asset: " + href);
	}

	private static List<BandHit> cssBandSequence(String html) {
		var out = new ArrayList<BandHit>();
		var links = LINK_HREF.matcher(html);
		while (links.find())
			out.add(new BandHit(cssBandPosition(links.group(1)), links.group(1), links.start()));
		var style = STYLE_TAG.matcher(html);
		if (style.find())
			out.add(new BandHit(PAGE_LOCAL, "<style> (page-local)", style.start()));
		out.sort(Comparator.comparingInt(BandHit::index));
		return out;
	}

	private static List<BandHit> scriptSequence(String html, Map<String,Integer> roleOf) {
		var out = new ArrayList<BandHit>();
		var scripts = SCRIPT_SRC.matcher(html);
		while (scripts.find()) {
			var src = scripts.group(1);
			for (var e : roleOf.entrySet())
				if (src.contains(e.getKey()))
					out.add(new BandHit(e.getValue(), src, scripts.start()));
		}
		out.sort(Comparator.comparingInt(BandHit::index));
		return out;
	}

	private static void assertBandOrder(List<BandHit> hits, String pageLabel) {
		for (var i = 1; i < hits.size(); i++) {
			var prev = hits.get(i - 1);
			var cur = hits.get(i);
			assertTrue(prev.position() <= cur.position(),
				() -> pageLabel + ": asset order violates the band - \"" + cur.label() + "\" (position "
					+ cur.position() + ") must not load before \"" + prev.label() + "\" (position "
					+ prev.position() + ")");
		}
	}

	@Test void a01_storeCssBandOrder() throws Exception {
		var hits = cssBandSequence(page(client(), "/console/store"));
		assertTrue(hits.size() >= 3, () -> "expected the chrome, theme and views stylesheets: " + hits);
		assertBandOrder(hits, "P1 Store");
	}

	@Test void a02_everyPageCssBandOrder() throws Exception {
		var c = client();
		for (var path : PetstorePages_ContractTest.PAGE_PATHS)
			assertBandOrder(cssBandSequence(page(c, path)), path);
	}

	/** The chrome script loads first, then the views layer, then the region runtime, then the helpers that reuse both. */
	@Test void a03_scriptOrder() throws Exception {
		var hits = scriptSequence(page(client(), "/console/store"),
			Map.of("/juneau-console.js", 1, "/juneau-views.js", 2, "/juneau-regions.js", 3, "/juneau-helpers.js", 4));
		assertSize(4, hits);
		assertBandOrder(hits, "P1 script order (console, views, regions, helpers)");
	}

	@Test void b01_syntheticCssBandViolation_isCaught() {
		var reversed = "<link rel=\"stylesheet\" href=\"/juneau-config.css\">"
			+ "<link rel=\"stylesheet\" href=\"/juneau-views.css\">";
		var seq = cssBandSequence(reversed);
		var err = assertThrows(AssertionError.class, () -> assertBandOrder(seq, "synthetic"));
		assertTrue(err.getMessage().contains("juneau-views.css"), err.getMessage());
	}

	@Test void b02_syntheticScriptBandViolation_isCaught() {
		var reversed = "<script src=\"/juneau-regions.js\"></script><script src=\"/juneau-views.js\"></script>";
		var hits = scriptSequence(reversed, Map.of("/juneau-views.js", 2, "/juneau-regions.js", 3));
		var err = assertThrows(AssertionError.class, () -> assertBandOrder(hits, "synthetic"));
		assertTrue(err.getMessage().contains("juneau-views.js"), err.getMessage());
	}

	@Test void b03_conformingSyntheticFullBand_isNotFlagged() {
		var conforming = "<link rel=\"stylesheet\" href=\"/juneau-console/chrome.css\">"
			+ "<link rel=\"stylesheet\" href=\"/juneau-console/themes/juneau-theme-open.css\">"
			+ "<link rel=\"stylesheet\" href=\"https://cdn.example/dataTables.dataTables.min.css\">"
			+ "<link rel=\"stylesheet\" href=\"/juneau-views.css\">"
			+ "<link rel=\"stylesheet\" href=\"/juneau-datatables.css\">"
			+ "<style>body{}</style>";
		assertBandOrder(cssBandSequence(conforming), "synthetic conforming");
	}
}
