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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * {@code juneau-pages.js} pure hash-routing logic + DOM binding shim tests.
 *
 * <p>
 * <b>Scope:</b> these tests extract each pure function's source body from the served script and assert on its
 * control-flow markers.  They are cheap, always-on tripwires that need no Node, and they are deliberately <i>not</i>
 * the proof that the runtime works: source shape cannot distinguish a working page from a blank one.  That proof
 * lives in {@link PagePanelVisibility_BrowserTest}, which executes this script in a real browser and asserts on
 * rendered visibility &mdash; opt-in, behind the module's {@code js-tests} Maven profile.
 * <p>
 * The functions remain written as a DOM-free pure layer (see the class-header comment in {@code juneau-pages.js}),
 * so the harness could also drive them directly should these particular assertions ever need that depth.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class JuneauPagesJs_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends org.apache.juneau.rest.server.servlet.BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String pagesJs() throws Exception {
		return c.get(ViewsMixin.PAGES_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	/** Extracts a top-level function's source body (from its `function name(` line to the next top-level `\n\t}`). */
	private static String functionBody(String src, String functionName) {
		var start = src.indexOf("function " + functionName + "(");
		assertTrue(start >= 0, () -> functionName + " not found:\n" + src);
		var end = src.indexOf("\n\t}", start);
		return src.substring(start, end < 0 ? src.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Task 7: pure hash-routing logic - parseHash / resolveInitial / hashFor
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_parseHashIsPureAndPubliclyExposed() throws Exception {
		var body = pagesJs();
		assertTrue(body.contains("NS.pages = {"), body);
		assertTrue(body.contains("parseHash: parseHash"), body);
		var fn = functionBody(body, "parseHash");
		// DOM-free: splits the hash on "/" - no document/window DOM reads inside the function itself.
		assertTrue(fn.contains("split(\"/\")"), fn);
		assertFalse(fn.contains("document."), fn);
	}

	@Test void a02_resolveInitialExposedAndFallsBackToFirstTabAndSubtab() throws Exception {
		var body = pagesJs();
		assertTrue(body.contains("resolveInitial: resolveInitial"), body);
		var fn = functionBody(body, "resolveInitial");
		// Fallback-to-first-tab-when-missing/unknown (pt.1 of the design doc's page-runtime responsibilities).
		assertTrue(fn.contains("tabs[0]"), fn);
		// A tab with subtabs falls back to ITS first subtab too.
		assertTrue(fn.contains("tab.subtabs[0]"), fn);
		assertFalse(fn.contains("document."), fn);
	}

	@Test void a03_resolveInitialRejectsHashFromADifferentPage() throws Exception {
		var body = pagesJs();
		var fn = functionBody(body, "resolveInitial");
		// A hash targeting a different pageId must not be treated as this page's active tab.
		assertTrue(fn.contains("parsed.pageId === pageMeta.id"), fn);
	}

	@Test void a04_hashForExposedAndBuildsThreeSegmentHash() throws Exception {
		var body = pagesJs();
		assertTrue(body.contains("hashFor: hashFor"), body);
		var fn = functionBody(body, "hashFor");
		assertTrue(fn.contains("\"#\" + pageId"), fn);
		assertTrue(fn.contains("subtabId"), fn);
	}

	@Test void a05_findByIdIsPureIdEqualityLookup() throws Exception {
		var body = pagesJs();
		var fn = functionBody(body, "findById");
		assertTrue(fn.contains("list[i].id === id"), fn);
		assertFalse(fn.contains("document."), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Task 8: DOM binding shim - lazy init, columns.adjust() on re-show, hashchange wiring, no-selector-injection
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_activatePanelViews_lazyInitsFirstThenAdjustsColumnsOnReShow() throws Exception {
		var body = pagesJs();
		var fn = functionBody(body, "activatePanelViews");
		assertTrue(fn.contains("isDataTable(t)"), fn);
		assertTrue(fn.contains("NS.init.initTable"), fn);
		assertTrue(fn.contains("Promise.resolve(NS.init.initTable(t))"), fn);
		assertTrue(fn.contains("columns.adjust()"), fn);
	}

	@Test void b02_hashchangeListenerWired() throws Exception {
		var body = pagesJs();
		assertTrue(body.contains("addEventListener(\"hashchange\""), body);
	}

	@Test void b03_activeStateReflectedViaAttributeComparisonNotSelectorInjection() throws Exception {
		// Security: hash-derived tabId/subtabId must be compared against existing elements' attribute VALUES
		// (getAttribute(...) === ...), never interpolated into a querySelector(...) string or innerHTML.
		var body = pagesJs();
		var fn = functionBody(body, "showActive");
		// Spelled from the emitter's constants, so these double as part of the name-correspondence pin.
		assertTrue(fn.contains("getAttribute(\"" + PageTable.TAB_ID_ATTR + "\") === tabId"), fn);
		assertTrue(fn.contains("getAttribute(\"" + PageTable.SUBTAB_ID_ATTR + "\") === subtabId"), fn);
		assertFalse(fn.contains("querySelector(\"[" + PageTable.TAB_ID_ATTR + "=\""), fn);
		assertFalse(fn.contains("innerHTML"), fn);
	}

	@Test void b04_panelVisibilityToggledViaClassNotInlineStyle() throws Exception {
		// No caller/hash-derived string is ever written into style="..." - only a static class name is toggled.
		var body = pagesJs();
		var fn = functionBody(body, "showActive");
		assertTrue(fn.contains("classList.toggle(\"jc-active\""), fn);
		assertFalse(fn.contains(".style."), fn);
	}

	@Test void b05_pageContractVersionMismatchRefusesToInitAndRendersBanner() throws Exception {
		var body = pagesJs();
		assertTrue(body.contains("contractVersion !== JUNEAU_PAGE_CONTRACT_VERSION"), body);
		assertTrue(body.contains("renderBanner("), body);
		assertTrue(body.contains("jc-page-error"), body);
	}

	@Test void b06_missingOrMalformedSidecarRefusesToInitRatherThanThrow() throws Exception {
		var body = pagesJs();
		var fn = functionBody(body, "initPage");
		assertTrue(fn.contains("if (!sidecar)"), fn);
		assertTrue(fn.contains("JSON.parse(sidecar.textContent)"), fn);
		assertTrue(fn.contains("catch (e)"), fn);
	}

	@Test void b07_initAllPagesScansDataJuneauPageShellsOnDomContentLoaded() throws Exception {
		var body = pagesJs();
		assertTrue(body.contains("querySelectorAll(\"[data-juneau-page]\")"), body);
		assertTrue(body.contains("DOMContentLoaded"), body);
	}
}
