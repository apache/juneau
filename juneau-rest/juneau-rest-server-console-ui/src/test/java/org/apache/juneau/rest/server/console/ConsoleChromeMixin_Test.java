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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Phase 3 gate: {@link ConsoleChromeMixin} + the dynamic {@code GET /juneau-console/chrome.css} endpoint.
 */
@SuppressWarnings({
	"java:S5961", // Contract test is intentionally dense; splitting would hide landing-page pins.
	"java:S8786", // Test regex is intentional; tightening would change match semantics.
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class ConsoleChromeMixin_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a) Opt-in / back-compat
	//-----------------------------------------------------------------------------------------------------------------

	public static class NoMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient cNoMixin = MockRestClient.buildLax(NoMixin.class);
	private static final MockRestClient cWithMixin = MockRestClient.buildLax(WithMixin.class);

	@Test void a01_hostWithoutMixin_chromeCssRouteIs404() throws Exception {
		cNoMixin.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(404);
	}

	@Test void a02_hostWithMixin_chromeCssRouteIs200_withCssContentTypeAndCacheControl() throws Exception {
		cWithMixin.get(ConsoleChromeMixin.CHROME_CSS_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/css")
			.assertHeader("Cache-Control").isContains("max-age");
	}

	@Test void a03_hostExistingRoute_unaffectedByMixin() throws Exception {
		cWithMixin.get("/items").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Theme selection: builder theme(String) > Theme.OPEN
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class DefaultHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class LightBrownHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme("light-brown").build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class GrayHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme("gray").build(); }
	}

	@Test void b01_noThemeConfigured_bodyCarriesOnlyOpenBlock() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(DefaultHost.class));
		assertEquals(1, countRootBlocks(body), () -> "expected exactly one :root{} block, body:\n" + body);
		assertTrue(body.contains("--jc-accent:#1589EE;"));
	}

	@Test void b02_selectedStockTheme_appendsExactlyItsOverrideBlock() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(LightBrownHost.class));
		assertEquals(2, countRootBlocks(body), () -> "expected Theme.OPEN block + light-brown's block, body:\n" + body);
		assertTrue(body.contains("\n" + ConsoleChromeMixin.overrideBlock(Theme.LIGHT_BROWN)), () -> body);
	}

	/** Two independently configured mounts must each serve their own palette, never the other's cached one. */
	@Test void b03_twoStockThemes_serveTheirOwnPalettes() throws Exception {
		var brown = bodyOf(MockRestClient.buildLax(LightBrownHost.class));
		var gray = bodyOf(MockRestClient.buildLax(GrayHost.class));
		var brownBg = "--jc-chrome-bg:" + Theme.LIGHT_BROWN.getTokens().get("--jc-chrome-bg") + ";";
		var grayBg = "--jc-chrome-bg:" + Theme.GRAY.getTokens().get("--jc-chrome-bg") + ";";
		assertNotEquals(brownBg, grayBg, "premise: the two palettes differ on --jc-chrome-bg");
		assertTrue(brown.contains(brownBg), () -> brown);
		assertFalse(brown.contains(grayBg), () -> brown);
		assertTrue(gray.contains(grayBg), () -> gray);
		assertFalse(gray.contains(brownBg), () -> gray);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Theme.OPEN <-> chrome.css bidirectional token cross-check
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_openBlock_containsOnlyJcNames() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(DefaultHost.class));
		var block = firstRootBlock(body);
		var m = Pattern.compile("([a-zA-Z-]++)\\s*:").matcher(block);
		while (m.find())
			assertTrue(m.group(1).startsWith("--jc-"), () -> "non --jc- name in :root{} block: " + m.group(1));
	}

	@Test void c02_everyChromeCssVarJc_isDefinedInThemeOpenOrAliasBlock() throws IOException {
		// Three-way check: chrome.css now consumes role-named tokens (--jc-header-bg, --jc-surface, ...)
		// that are declared by the framework-authored alias block (ConsoleChromeMixin.OPEN_ROLE_ALIASES), not by
		// Theme.OPEN. Every chrome.css reference must resolve to one of the two framework blocks.
		var referenced = referencedTokensInChromeCss();
		var defined = new LinkedHashSet<>(Theme.OPEN.getTokens().keySet());
		defined.addAll(aliasDefinedNames());
		for (var name : referenced)
			assertTrue(defined.contains(name), () -> "chrome.css references '" + name + "' but neither Theme.OPEN nor the alias block defines it");
	}

	@Test void c03_everyDefinedToken_isReferencedInChromeCssOrAliasBlock() throws IOException {
		// Three-way check: a legacy token that chrome.css no longer references directly (e.g. --jc-white)
		// is not an orphan - it is still consumed by the alias block as the source of a derived role token. A role
		// token declared by the alias block must in turn be consumed by chrome.css (or by a later alias link).
		var referenced = new LinkedHashSet<>(referencedTokensInChromeCss());
		referenced.addAll(aliasReferencedNames());
		var defined = new LinkedHashSet<>(Theme.OPEN.getTokens().keySet());
		defined.addAll(aliasDefinedNames());
		for (var name : defined)
			assertTrue(referenced.contains(name), () -> "orphan token '" + name + "' defined but never referenced by chrome.css or the alias block");
	}

	private static Set<String> referencedTokensInChromeCss() throws IOException {
		String css;
		try (var in = ConsoleChromeMixin_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			assertNotNull(in);
			css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		var stripped = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(css).replaceAll("");
		var out = new LinkedHashSet<String>();
		var m = Pattern.compile("var\\((--jc-[a-z0-9-]++)\\)").matcher(stripped);
		while (m.find())
			out.add(m.group(1));
		return out;
	}

	/** The role-token names DECLARED (left-hand side) by the framework-authored alias block. */
	private static Set<String> aliasDefinedNames() {
		var out = new LinkedHashSet<String>();
		var m = Pattern.compile("(--jc-[a-z0-9-]++)\\s*:").matcher(ConsoleChromeMixin.OPEN_ROLE_ALIASES);
		while (m.find())
			out.add(m.group(1));
		return out;
	}

	/** The token names REFERENCED (via var(...)) by the framework-authored alias block. */
	private static Set<String> aliasReferencedNames() {
		var out = new LinkedHashSet<String>();
		var m = Pattern.compile("var\\((--jc-[a-z0-9-]++)\\)").matcher(ConsoleChromeMixin.OPEN_ROLE_ALIASES);
		while (m.find())
			out.add(m.group(1));
		return out;
	}

	/**
	 * Header and both page-nav rows stay white on every theme: {@code --jc-header-bg} keys off {@code --jc-white},
	 * not {@code --jc-chrome-bg}. Themed {@code --jc-chrome-bg} still tints the page fallback / hover / dialogs;
	 * it must not wash {@code .jc-header} or {@code .juneau-page-nav}.
	 */
	@Test void c04_headerBgAlias_derivesFromWhite_soThemedChromeBgDoesNotWashHeaderNav() {
		var m = Pattern.compile("--jc-header-bg:(var\\(--jc-[a-z0-9-]++\\));").matcher(ConsoleChromeMixin.OPEN_ROLE_ALIASES);
		assertTrue(m.find(), () -> "no --jc-header-bg alias declaration found in OPEN_ROLE_ALIASES: " + ConsoleChromeMixin.OPEN_ROLE_ALIASES);
		assertEquals("var(--jc-white)", m.group(1),
			() -> "expected --jc-header-bg to key off --jc-white (header/page-nav stay white on every theme), got: " + m.group(1));
	}

	/** {@code --jc-nav-bg} derives from {@code --jc-header-bg}, so page-tab and page-subtab rows stay white too. */
	@Test void c05_navBgAlias_stillDerivesFromHeaderBg_soPageNavRowsStayWhiteToo() {
		var m = Pattern.compile("--jc-nav-bg:(var\\(--jc-[a-z0-9-]++\\));").matcher(ConsoleChromeMixin.OPEN_ROLE_ALIASES);
		assertTrue(m.find(), () -> "no --jc-nav-bg alias declaration found in OPEN_ROLE_ALIASES: " + ConsoleChromeMixin.OPEN_ROLE_ALIASES);
		assertEquals("var(--jc-header-bg)", m.group(1));
	}

	/** {@code --jc-page-nav-accent} defaults to {@code --jc-accent} so apps can retint the bar without buttons. */
	@Test void c05b_pageNavAccentAlias_derivesFromAccent() {
		var m = Pattern.compile("--jc-page-nav-accent:(var\\(--jc-[a-z0-9-]++\\));").matcher(ConsoleChromeMixin.OPEN_ROLE_ALIASES);
		assertTrue(m.find(), () -> "no --jc-page-nav-accent alias declaration found in OPEN_ROLE_ALIASES: " + ConsoleChromeMixin.OPEN_ROLE_ALIASES);
		assertEquals("var(--jc-accent)", m.group(1));
	}

	/**
	 * Stock {@link Theme}s must not declare {@code --jc-header-bg} / {@code --jc-nav-bg}: those roles live only
	 * in {@link ConsoleChromeMixin#OPEN_ROLE_ALIASES} (pinned to white). A per-theme override would retint the
	 * bars. {@code --jc-chrome-bg} may still differ per theme for other chrome.
	 */
	@Test void c06_stockThemes_doNotDeclareHeaderOrNavBg() {
		for (var theme : List.of(Theme.OPEN, Theme.LIGHT_BROWN, Theme.LIGHT_RED, Theme.RED, Theme.GRAY)) {
			assertFalse(theme.getTokens().containsKey("--jc-header-bg"),
				() -> theme.getName() + " must not declare --jc-header-bg");
			assertFalse(theme.getTokens().containsKey("--jc-nav-bg"),
				() -> theme.getName() + " must not declare --jc-nav-bg");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e) url-sink gate, end-to-end
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_bypassVector_throwsAtToken_notSilentlySwallowed() {
		var theme = Theme.create("x");
		assertThrows(IllegalArgumentException.class,
			() -> theme.token("--jc-page-bg", "url(https://evil)"));
		assertThrows(IllegalArgumentException.class,
			() -> theme.token("--jc-page-bg", "url (https://evil)"));
		assertThrows(IllegalArgumentException.class,
			() -> theme.token("--jc-page-bg", "url/**/(https://evil)"));
	}

	@Test void e02_structuralLayer_colorTokensNeverSinkIntoUrlCapableProperty() throws IOException {
		String css;
		try (var in = ConsoleChromeMixin_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertEquals(List.of(), ChromeCssScanner.scan(css));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f) cacheAssets(true) caching gate
	//-----------------------------------------------------------------------------------------------------------------

	static final ConsoleChromeMixin CACHED_MIXIN = ConsoleChromeMixin.create().build();
	static final ConsoleChromeMixin UNCACHED_MIXIN = ConsoleChromeMixin.create().cacheAssets(false).build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class CachedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return CACHED_MIXIN; }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class UncachedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return UNCACHED_MIXIN; }
	}

	@Test void f01_cacheAssetsTrue_bodyAssembledOnceAcrossRequests() throws Exception {
		var c = MockRestClient.buildLax(CachedHost.class);
		c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200);
		c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200);
		c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200);
		assertEquals(1, CACHED_MIXIN.debugBuildCount());
	}

	@Test void f02_cacheAssetsFalse_bodyReassembledEveryRequest() throws Exception {
		var c = MockRestClient.buildLax(UncachedHost.class);
		c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200);
		c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200);
		assertEquals(2, UNCACHED_MIXIN.debugBuildCount());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i) chrome.css composition: no asset/url() plumbing remains
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_servedBody_referencesNoAssetEndpoint() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(LightBrownHost.class));
		assertFalse(body.contains("/juneau-console/assets/"), () -> body);
		// NOTE: the second prescribed assertion (assertFalse(body.contains("url(")) is omitted here - the static
		// chrome.css legitimately carries a `background-image: url("data:...")` declaration (an inline SVG data
		// URI, not an asset-endpoint reference), so that assertion would fail unconditionally against real content.
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i8) Page-footer line: <@footer> is FTL-authored; chrome.css must not emit body::after
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i11_chromeCss_omitsBodyAfterContent() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(DefaultHost.class));
		assertFalse(body.contains("body::after{content:"),
			() -> "unset footer must not emit content, body:\n" + body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// j) Security regression: Theme/CssValueGrammar untouched by this feature
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_assembledResponses_passChromeCssScanner() throws Exception {
		for (var host : List.of(DefaultHost.class, LightBrownHost.class, GrayHost.class)) {
			var body = bodyOf(MockRestClient.buildLax(host));
			assertEquals(List.of(), ChromeCssScanner.scan(body), () -> "violations for " + host.getSimpleName() + ":\n" + body);
		}
	}

	@Test void j02_themeTokenPath_stillRejectsUrlProduction_evenWithAssetsFeaturePresent() {
		var theme = Theme.create("x");
		assertThrows(IllegalArgumentException.class,
			() -> theme.token("--jc-page-bg", "url(https://evil)"));
	}

	@Test void j03_themeOpenTokenCount_pinned_unaffectedByAssetsFeature() {
		// 82 = 70 (55 base + the 15 canonical --jc-pill-* status-chip tokens; the 15 legacy --jc-tag-* tokens are
		// RETAINED as var(--jc-pill-*) aliases for badges/avatars/frozen calendar) plus the 12 --jc-probe-* status
		// probe tokens (4 statuses x bg/text/dot); the selected-probe ring reuses the existing --jc-accent-selected
		// token, so the probe family adds exactly 12, not a ring token.
		assertBean(Theme.OPEN.getTokens(),
			"size,--jc-main-bg,--jc-card-bg,--jc-card-padding,--jc-chrome-icon,--jc-text,--jc-text-soft,--jc-card-shadow,--jc-page-nav-hairline,--jc-page-nav-section-font-size,--jc-page-nav-child-font-size",
			"82,#f5f6f9,#ffffff,16px 16px 8px,#666666,#080707,#080707cc,0 2px 2px rgba(0, 0, 0, 0.05),2px,13px,12px");
		assertFalse(Theme.OPEN.getTokens().containsKey("--jc-logo"));
		assertFalse(Theme.OPEN.getTokens().containsKey("--jc-page-bg-image"));
		assertFalse(Theme.OPEN.getTokens().containsKey("--jc-card-surface"));
	}

	@Test void j04_accentSelectedFace_meetsWcag1411NonTextContrast_andBothSelectorsConsumeToken() throws Exception {
		// OQ-A3's pinning test (LD-5, exit (b)): the selected face is a NEW, OPAQUE Theme.OPEN token, so this is a
		// plain two-literal comparison - no "assumed backdrop" caveat, unlike a translucent wash whose composited
		// colour depends on what sits behind it (WAVE-0013 SS6.2's compositing objection, which does not apply
		// to an opaque value).
		var face = Theme.OPEN.getTokens().get("--jc-accent-selected");
		assertEquals("#1589EE", face);
		// Unselected face: --jc-control-bg -> --jc-surface -> --jc-white (Theme.OPEN's --jc-white), i.e. #ffffff.
		var faceVsUnselected = contrastRatio(face, "#ffffff");
		assertTrue(faceVsUnselected >= 3.0, () -> "selected/unselected face contrast " + faceVsUnselected + ":1 is below WCAG 1.4.11's 3:1 floor");
		// --jc-on-accent (white) against the SAME new face must also clear 3:1 - both pairs are white-vs-#1589EE,
		// so both computations land at the identical ratio.
		var onAccentVsFace = contrastRatio("#ffffff", face);
		assertTrue(onAccentVsFace >= 3.0, () -> "--jc-on-accent/selected-face contrast " + onAccentVsFace + ":1 is below WCAG 1.4.11's 3:1 floor");

		// Substring pin: the two ribbon-format selected-state selectors consume var(--jc-accent-selected),
		// with no var() fallback. Page Tabs (.jc-nav-tab.active) use wash + top accent instead of this fill.
		var css = readChromeCss();
		assertContainsAll(css, ".jc-tab.jc-tab-active,\n.jc-subtab.jc-subtab-active {",
			".juneau-view-ribbon-group[data-juneau-strip-mode=\"tab\"] .juneau-view-ribbon-btn[aria-selected=\"true\"] {");
		assertEquals(2, countOccurrences(css, "background-color: var(--jc-accent-selected)"),
			() -> "expected exactly the two ribbon-format selected-state selectors (Page Tabs are wash + top accent), css:\n" + css);
	}

	@Test void j05_navTabActive_selectedAccentIsTopEdge_notBottomBarOrSolidFill() throws Exception {
		var css = readChromeCss();
		var navTabStart = css.indexOf("\n.jc-nav-tab {");
		assertNotEquals(-1, navTabStart, () -> "missing .jc-nav-tab rule, css:\n" + css);
		var navTabBlock = css.substring(navTabStart, css.indexOf("}", navTabStart));
		assertTrue(navTabBlock.contains("border-top: var(--jc-nav-indicator-width) solid transparent"),
			() -> "unselected Page Tab reserves the top indicator, block:\n" + navTabBlock);
		assertTrue(navTabBlock.contains("border-bottom: none"),
			() -> "Page Tab must not carry a bottom indicator, block:\n" + navTabBlock);

		var navTabActiveStart = css.indexOf(".jc-nav-tab.active {");
		assertNotEquals(-1, navTabActiveStart, () -> "missing .jc-nav-tab.active rule, css:\n" + css);
		var navTabActiveBlock = css.substring(navTabActiveStart, css.indexOf("}", navTabActiveStart));
		assertTrue(navTabActiveBlock.contains("background-color: var(--jc-accent-wash)"),
			() -> "selected Page Tab is a wash so the top accent is visible, block:\n" + navTabActiveBlock);
		assertTrue(navTabActiveBlock.contains("border-top-color: var(--jc-page-nav-accent)"),
			() -> "selected Page Tab accent sits on top, block:\n" + navTabActiveBlock);
		assertTrue(navTabActiveBlock.contains("border-bottom: none"),
			() -> "selected Page Tab must not carry a thick bar underneath, block:\n" + navTabActiveBlock);
		assertTrue(navTabActiveBlock.contains("color: var(--jc-text)"),
			() -> "wash fill needs dark ink, block:\n" + navTabActiveBlock);
		assertFalse(navTabActiveBlock.contains("var(--jc-accent-selected)"),
			() -> "opaque selected fill hides the top accent, block:\n" + navTabActiveBlock);
		assertFalse(navTabActiveBlock.contains("border-bottom-color"),
			() -> "selected Page Tab accent is the top edge, not a bottom underline, block:\n" + navTabActiveBlock);
		assertFalse(navTabActiveBlock.contains("var(--jc-on-accent)"),
			() -> "white-on-wash ink is illegible, block:\n" + navTabActiveBlock);
	}

	@Test void j06_tabBaseThemingRule_outranksTheViewsBaseRule_soLinkOrderCannotDecideIt() throws Exception {
		// juneau-views.css declares a bare ".jc-tab,\n.jc-subtab" base rule whose colourless "border-top: 1px
		// solid" shorthand resets the border to currentColor.  A single-class selector here would tie it at
		// (0,0,1,0), leaving the winner to whichever <link> a consumer places second; the doubled class lifts this
		// rule to (0,0,2,0) so the theming wins on specificity alone.
		var css = readChromeCss();
		assertTrue(css.contains(".jc-tab.jc-tab,\n.jc-subtab.jc-subtab {"),
			() -> "missing raised-specificity tab theming selector, css:\n" + css);
		assertFalse(css.contains("\n.jc-tab,\n.jc-subtab {"),
			() -> "the tab theming rule must not fall back to a single-class selector, css:\n" + css);
	}

	@Test void j07_htmlSlotPageNav_selectedSectionIsWashAndTopAccent_selectedChildIsWashNotAccentText() throws Exception {
		var css = readChromeCss();
		assertTrue(css.contains(".juneau-page-nav-section[aria-current=\"page\"] {"), css);
		var sectionStart = css.indexOf(".juneau-page-nav-section[aria-current=\"page\"] {");
		var sectionBlock = css.substring(sectionStart, css.indexOf("}", sectionStart));
		assertTrue(sectionBlock.contains("background-color: var(--jc-accent-wash)"), sectionBlock);
		assertTrue(sectionBlock.contains("border-top-color: var(--jc-page-nav-accent)"), sectionBlock);
		assertFalse(sectionBlock.contains("border-bottom-color: var(--jc-page-nav-accent)"),
			() -> "selected section accent is the top edge, not a bottom underline, block:\n" + sectionBlock);
		assertFalse(sectionBlock.contains("var(--jc-accent-selected)"),
			() -> "selected section must not use the pill-fill token, block:\n" + sectionBlock);

		assertTrue(css.contains(".juneau-page-nav-child[aria-current=\"page\"] {"), css);
		var childStart = css.indexOf(".juneau-page-nav-child[aria-current=\"page\"] {");
		var childBlock = css.substring(childStart, css.indexOf("}", childStart));
		assertTrue(childBlock.contains("background-color: var(--jc-accent-wash)"), childBlock);
		assertTrue(childBlock.contains("color: var(--jc-text)"), childBlock);
		assertFalse(childBlock.contains("color: var(--jc-accent)"),
			() -> "selected child is a wash, not accent type, block:\n" + childBlock);
		assertFalse(css.contains("juneau-page-nav-section-selected"), css);
		assertFalse(css.contains("juneau-page-nav-child-selected"), css);
		assertFalse(css.contains("juneau-page-nav-cloud"), css);
		var navMarker = "HTML-slot page nav (two text rows";
		var navStart = css.indexOf(navMarker);
		assertNotEquals(-1, navStart, () -> "missing HTML-slot page nav comment, css:\n" + css);
		var navEnd = css.indexOf("Page scaffolding", navStart);
		assertNotEquals(-1, navEnd, () -> "missing Page scaffolding marker after page-nav, css:\n" + css);
		var navRules = css.substring(navStart, navEnd);
		assertTrue(navRules.contains("\n.juneau-page-nav {"), navRules);
		var floorStart = navRules.indexOf("\n.juneau-page-nav {");
		var floorBlock = navRules.substring(floorStart, navRules.indexOf("}", floorStart));
		assertTrue(floorBlock.contains("background-color: var(--jc-nav-bg)"),
			() -> "nav rows must spend --jc-nav-bg, block:\n" + floorBlock);
		assertTrue(floorBlock.contains("border-bottom-color: var(--jc-page-nav-accent)"),
			() -> "the pair's floor must be --jc-page-nav-accent, block:\n" + floorBlock);
		assertFalse(floorBlock.contains("#1589EE"),
			() -> "floor colour is a theme token, not a hex literal, block:\n" + floorBlock);
		// The 2px hairline is gated on :not(:last-child): it paints under the sections row
		// only when a children (subtab) row follows. With no subtabs the sections row is
		// the last child, so it draws no hairline and the nav's 3px floor is the only
		// bottom border (not 3px + 2px = 5px stacked).
		assertFalse(navRules.contains(".juneau-page-nav-sections {"),
			() -> "sections hairline must be gated on :not(:last-child), not unconditional:\n" + navRules);
		assertTrue(navRules.contains(".juneau-page-nav-sections:not(:last-child) {"), navRules);
		var hairlineStart = navRules.indexOf(".juneau-page-nav-sections:not(:last-child) {");
		var hairlineBlock = navRules.substring(hairlineStart, navRules.indexOf("}", hairlineStart));
		assertTrue(hairlineBlock.contains("border-bottom-width: var(--jc-page-nav-hairline)"),
			() -> "hairline between tabs and children must be --jc-page-nav-hairline, block:\n" + hairlineBlock);
		assertTrue(hairlineBlock.contains("border-bottom-color: var(--jc-page-nav-accent)"),
			() -> "hairline between tabs and children must be --jc-page-nav-accent, block:\n" + hairlineBlock);
		var sectionTypeStart = navRules.indexOf("\n.juneau-page-nav-section {");
		assertTrue(sectionTypeStart >= 0, navRules);
		var sectionTypeBlock = navRules.substring(sectionTypeStart, navRules.indexOf("}", sectionTypeStart));
		assertTrue(sectionTypeBlock.contains("font-size: var(--jc-page-nav-section-font-size)"),
			() -> "sections spend --jc-page-nav-section-font-size, block:\n" + sectionTypeBlock);
		assertFalse(sectionTypeBlock.contains("--jc-chrome-font-size-2"),
			() -> "sections must not share --jc-chrome-font-size-2, block:\n" + sectionTypeBlock);
		var childTypeStart = navRules.indexOf("\n.juneau-page-nav-child {");
		assertTrue(childTypeStart >= 0, navRules);
		var childTypeBlock = navRules.substring(childTypeStart, navRules.indexOf("}", childTypeStart));
		assertTrue(childTypeBlock.contains("font-size: var(--jc-page-nav-child-font-size)"),
			() -> "children spend --jc-page-nav-child-font-size, block:\n" + childTypeBlock);
		assertFalse(childTypeBlock.contains("--jc-chrome-font-size-2"),
			() -> "children must not share --jc-chrome-font-size-2, block:\n" + childTypeBlock);
		assertFalse(navRules.contains("slds-"), () -> "chrome page-nav rules must not introduce slds-* classes:\n" + navRules);
		assertFalse(navRules.toLowerCase().contains("salesforce sans"), navRules);
	}

	@Test void j07b_htmlSlotPageNav_chromeCarriesShapeSoToolkitLessPagesDoNotConcatenateLabels() throws Exception {
		// <@node> emits adjacent <a> tags with no whitespace. Without flex + padding in always-on
		// chrome.css, toolkit-less pages (Setup, New Release) render as "SetupReleasesNew Release".
		var css = readChromeCss();
		var navMarker = "HTML-slot page nav (two text rows";
		var navStart = css.indexOf(navMarker);
		assertNotEquals(-1, navStart, () -> "missing HTML-slot page nav comment, css:\n" + css);
		var navEnd = css.indexOf("Page scaffolding", navStart);
		assertNotEquals(-1, navEnd, () -> "missing Page scaffolding marker after page-nav, css:\n" + css);
		var navRules = css.substring(navStart, navEnd);
		var floorStart = navRules.indexOf("\n.juneau-page-nav {");
		assertTrue(floorStart >= 0, navRules);
		var floorBlock = navRules.substring(floorStart, navRules.indexOf("}", floorStart));
		assertTrue(floorBlock.contains("display: flex"),
			() -> "chrome must own page-nav column flex, not just hue, block:\n" + floorBlock);
		assertTrue(floorBlock.contains("border-bottom-width: var(--jc-nav-indicator-width)"),
			() -> "floor width must paint without views.css, block:\n" + floorBlock);
		assertTrue(floorBlock.contains("border-bottom-style: solid"),
			() -> "floor style must paint without views.css, block:\n" + floorBlock);
		assertTrue(navRules.contains(".juneau-page-nav-sections, .juneau-page-nav-children {"),
			() -> "sections and children rows must share a flex row rule:\n" + navRules);
		var rowsStart = navRules.indexOf(".juneau-page-nav-sections, .juneau-page-nav-children {");
		var rowsBlock = navRules.substring(rowsStart, navRules.indexOf("}", rowsStart));
		assertTrue(rowsBlock.contains("display: flex"),
			() -> "sections/children must be a flex row so labels do not concatenate, block:\n" + rowsBlock);
		var sharedStart = navRules.indexOf(".juneau-page-nav-section, .juneau-page-nav-child {");
		assertTrue(sharedStart >= 0, () -> "missing shared section/child shape rule:\n" + navRules);
		var sharedBlock = navRules.substring(sharedStart, navRules.indexOf("}", sharedStart));
		assertTrue(sharedBlock.contains("display: inline-flex"),
			() -> "tab links must be inline-flex, block:\n" + sharedBlock);
		assertTrue(sharedBlock.contains("padding-left: var(--jc-space-3)"),
			() -> "tab links must have horizontal padding so labels do not run together, block:\n" + sharedBlock);
		var sectionTypeStart = navRules.indexOf("\n.juneau-page-nav-section {");
		assertTrue(sectionTypeStart >= 0, navRules);
		var sectionTypeBlock = navRules.substring(sectionTypeStart, navRules.indexOf("}", sectionTypeStart));
		assertTrue(sectionTypeBlock.contains("border-top-width: var(--jc-nav-indicator-width)"),
			() -> "selected-section top accent needs a width without views.css, block:\n" + sectionTypeBlock);
		assertTrue(sectionTypeBlock.contains("border-top-style: solid"),
			() -> "selected-section top accent needs a style without views.css, block:\n" + sectionTypeBlock);
	}

	@Test void j08_jcCard_keepsShadowAndRadius_dropsGreyStroke() throws Exception {
		var css = readChromeCss();
		var start = css.indexOf(".jc-card {");
		assertTrue(start >= 0, () -> "missing .jc-card rule, css:\n" + css);
		var block = css.substring(start, css.indexOf("}", start));
		assertTrue(block.contains("border: none"), () -> "default card must not paint a grey outline, block:\n" + block);
		assertFalse(block.contains("var(--jc-border)"),
			() -> "default card must not spend --jc-border as an outer stroke, block:\n" + block);
		assertTrue(block.contains("border-radius: var(--jc-radius)"), block);
		assertTrue(block.contains("box-shadow: var(--jc-card-shadow)"), block);
		assertTrue(block.contains("padding: var(--jc-card-padding)"),
			() -> "content cards spend --jc-card-padding, block:\n" + block);
		assertFalse(block.contains("padding: 18px 20px"),
			() -> "off-scale 18px 20px 8px inset must not remain, block:\n" + block);
	}

	/** WCAG 2.x contrast ratio between two {@code "#rrggbb"} literals: {@code (lighter+0.05)/(darker+0.05)}. */
	private static double contrastRatio(String hex1, String hex2) {
		var l1 = relativeLuminance(hex1);
		var l2 = relativeLuminance(hex2);
		return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
	}

	/** WCAG 2.x relative luminance of a {@code "#rrggbb"} literal. */
	private static double relativeLuminance(String hex) {
		var rgb = Integer.parseInt(hex.substring(1), 16);
		var r = srgbChannelToLinear((rgb >> 16) & 0xFF);
		var g = srgbChannelToLinear((rgb >> 8) & 0xFF);
		var b = srgbChannelToLinear(rgb & 0xFF);
		return 0.2126 * r + 0.7152 * g + 0.0722 * b;
	}

	private static double srgbChannelToLinear(int value8Bit) {
		var c = value8Bit / 255.0;
		return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}

	private static int countOccurrences(String haystack, String needle) {
		var count = 0;
		for (var idx = haystack.indexOf(needle); idx != -1; idx = haystack.indexOf(needle, idx + needle.length()))
			count++;
		return count;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// k) DataTables table visual parity (zebra striping, row hover, themed header)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void k01_chromeCss_themesDataTableZebraStriping_bothGenerations() throws Exception {
		var css = readChromeCss();
		// DT2.x bare markup (no "stripe" convenience class - what this app's tables actually render today).
		// DT1.x row classes.
		// DT2.x "stripe"/"display" convenience-class opt-in form.
		assertContainsAll(css, "table.dataTable > tbody > tr:nth-child(odd)", "table.dataTable > tbody > tr:nth-child(even)",
			"table.dataTable > tbody > tr.odd", "table.dataTable > tbody > tr.even",
			"table.dataTable.stripe > tbody > tr:nth-child(odd)", "table.dataTable.stripe > tbody > tr:nth-child(even)");
	}

	@Test void k02_chromeCss_themesDataTableRowHover_bothGenerations() throws Exception {
		var css = readChromeCss();
		assertContainsAll(css, "table.dataTable > tbody > tr:hover", "table.dataTable.hover > tbody > tr:hover");
	}

	@Test void k03_chromeCss_neutralizesVendoredStripeHoverCssVariables() throws Exception {
		var css = readChromeCss();
		assertContainsAll(css, "--dt-row-stripe:", "--dt-row-hover:");
	}

	@Test void k04_chromeCss_themesDataTableHeaderAndFont() throws Exception {
		var css = readChromeCss();
		assertContainsAll(css, "table.dataTable {", "font-family: var(--jc-font);", "table.dataTable > thead > tr > th",
			"border-color: var(--jc-table-border, #dee2e6);", "border-color: var(--jc-border);",
			"border-top-color: var(--jc-table-border, #dee2e6);", ".juneau-view-detail-control", "var(--jc-text-muted)");
	}

	/**
	 * Icon-button hover chrome: toolbar ribbons paint {@code --jc-accent} on the button's own border.
	 * Paging segments ARE the pill outline (no container stroke): idle gray on those edges, hover
	 * recolors the same top/bottom (plus end-cap sides) — not {@code border-color} on all four sides.
	 */
	@Test void k05_chromeCss_ribbonAndPagingHoverPaintAccentBorder() throws Exception {
		var css = readChromeCss();
		assertTrue(css.contains(".juneau-view-ribbon-btn:hover:not(:disabled) { background-color: var(--jc-accent-wash); color: var(--jc-accent); border-color: var(--jc-accent); }"),
			() -> "missing ribbon-btn hover accent border, css:\n" + css);
		assertTrue(css.contains(".juneau-view-pagingpill { background-color: var(--jc-control-bg); }"),
			() -> "pagingpill container must not stroke; fill only, css:\n" + css);
		assertFalse(css.contains(".juneau-view-pagingpill { border-color: var(--jc-control-border); background-color: var(--jc-control-bg); }"),
			() -> "container border-color would halo hover in a second outline, css:\n" + css);
		assertTrue(css.contains(".juneau-view-ribbon-btn { border-color: var(--jc-control-border); background-color: var(--jc-control-bg); color: var(--jc-chrome-icon); }"),
			() -> "idle ribbon glyph spends --jc-chrome-icon, not --jc-text-soft, css:\n" + css);
		assertTrue(css.contains(".juneau-view-pagingpill-btn { border-color: var(--jc-control-border); color: var(--jc-chrome-icon); }"),
			() -> "idle pagingpill-btn edges are the pill outline, css:\n" + css);
		assertTrue(css.contains(".juneau-view-pagingpill-btn:hover:not(:disabled) { background-color: var(--jc-accent-wash); color: var(--jc-accent); border-top-color: var(--jc-accent); border-bottom-color: var(--jc-accent); }"),
			() -> "pagingpill-btn hover must recolor top/bottom only, css:\n" + css);
		assertFalse(css.contains(".juneau-view-pagingpill-btn:hover:not(:disabled) { background-color: var(--jc-accent-wash); color: var(--jc-accent); border-color: var(--jc-accent); }"),
			() -> "shorthand border-color on hover invents chevron L/R, css:\n" + css);
		assertTrue(css.contains(".juneau-view-pagingpill > *:first-child:hover:not(:disabled) { border-left-color: var(--jc-accent); }"),
			() -> "missing first-child hover end-cap, css:\n" + css);
		assertTrue(css.contains(".juneau-view-pagingpill > *:last-child:hover:not(:disabled) { border-right-color: var(--jc-accent); }"),
			() -> "missing last-child hover end-cap, css:\n" + css);
		assertTrue(css.contains(".juneau-view-pagingpill-menuwrap { border-color: var(--jc-control-border); }"),
			() -> "idle paging menuwrap edges are the pill outline, css:\n" + css);
		assertTrue(css.contains(".juneau-view-pagingpill-menuwrap:hover { border-top-color: var(--jc-accent); border-bottom-color: var(--jc-accent); }"),
			() -> "menuwrap hover must recolor top/bottom only, css:\n" + css);
	}

	/**
	 * Toolbar Search focus paints the field's 1px border, not a second outset ring. The generic
	 * {@code :focus-visible} rule is 2px / offset 2px; these selectors (0,3,3) kill outline/box-shadow
	 * with literals and sink {@code --jc-accent} only into {@code border-color}.
	 */
	@Test void k08_chromeCss_toolbarSearchFocusRecolorsBorderNotOutline() throws Exception {
		var css = readChromeCss();
		assertTrue(css.contains("div.dt-container div.dt-search input:focus,"),
			() -> "missing search :focus selector, css:\n" + css);
		assertTrue(css.contains("div.dt-container div.dt-search input:focus-visible,"),
			() -> "missing search :focus-visible selector, css:\n" + css);
		var start = css.indexOf("div.dt-container div.dt-search input:focus,");
		var end = css.indexOf("}", start);
		var region = css.substring(start, end);
		assertTrue(region.contains("outline: none"), () -> "search focus must drop the outer ring, region:\n" + region);
		assertTrue(region.contains("box-shadow: none"), () -> "search focus must drop the outer shadow, region:\n" + region);
		assertTrue(region.contains("border-color: var(--jc-accent)"),
			() -> "search focus must recolor the field border, region:\n" + region);
		assertFalse(region.contains("outline: 2px"), () -> "must not restyle as a 2px ring, region:\n" + region);
		assertFalse(region.contains("outline-offset"), () -> "must not add outline-offset, region:\n" + region);
	}

	@Test void k07_chromeCss_dialogHeaderToggleAndFooterUseThemeTokens() throws Exception {
		var css = readChromeCss();
		assertContainsAll(css, ".juneau-view-dialog-header", ".juneau-view-dialog-dismiss:hover", ".juneau-view-dialog-confirm {",
			".juneau-view-toggle:checked {", "background-color: var(--jc-success);", "background-color: var(--jc-chrome-bg);",
			"input[aria-invalid=\"true\"]", "border-color: var(--jc-danger);");
	}

	/**
	 * Default ribbon hover keeps chrome wash (PD Ack / paging lock). {@code --icon} appearance is a
	 * separate opt-in that recolors the glyph only — no fill, no border shift.
	 */
	@Test void k06_chromeCss_iconAppearanceHoverRecolorsGlyphOnly() throws Exception {
		var css = readChromeCss();
		assertTrue(css.contains(".juneau-view-ribbon-btn:hover:not(:disabled) { background-color: var(--jc-accent-wash); color: var(--jc-accent); border-color: var(--jc-accent); }"),
			() -> "default ribbon hover must keep chrome wash (icon-only is opt-in --icon), css:\n" + css);
		var iconStart = css.indexOf(".juneau-view-ribbon-btn.juneau-view-ribbon-btn--icon:hover:not(:disabled)");
		assertTrue(iconStart >= 0, () -> "missing ribbon-btn--icon hover rule, css:\n" + css);
		var iconEnd = css.indexOf("}", iconStart);
		var iconRegion = css.substring(iconStart, iconEnd);
		assertTrue(iconRegion.contains("color: var(--jc-accent)"), iconRegion);
		assertTrue(iconRegion.contains("background-color: var(--jc-control-bg)"), iconRegion);
		assertTrue(iconRegion.contains("border-color: var(--jc-control-border)"), iconRegion);
		assertFalse(iconRegion.contains("var(--jc-accent-wash)"), iconRegion);
		assertTrue(css.contains(".juneau-view-helper-btn.juneau-view-helper-btn--icon:hover:not(:disabled)"),
			() -> "missing helper-btn--icon hover rule, css:\n" + css);
	}

	@Test void k09_chromeCss_headerSortSearchSpendAccentAndControlBorder() throws Exception {
		var css = readChromeCss();
		assertTrue(css.contains("th.dt-ordering-asc .juneau-sort-asc"),
			() -> "missing active sort-triangle theme, css:\n" + css);
		assertFalse(css.contains("th.dt-ordering-asc span.dt-column-order:before"),
			() -> "active sort color must target the Juneau SVG triangle, not DT ::before: " + css);
		assertTrue(css.contains(".juneau-view-col-search-icon.is-active { color: var(--jc-accent); }"),
			() -> "missing active column-search icon theme, css:\n" + css);
		assertTrue(css.contains(".juneau-view-col-search-popover {"),
			() -> "missing column-search popover theme, css:\n" + css);
		assertTrue(css.contains("border-color: var(--jc-control-border);"), css);
	}

	private static String readChromeCss() throws IOException {
		try (var in = ConsoleChromeMixin_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			assertNotNull(in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// l) Mount-style independence: standalone container mount at /juneau-console/* vs. composed onto a host
	//    mounted elsewhere.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * A container mount at url-pattern {@code /juneau-console/*} makes the container report
	 * {@code servletPath="/juneau-console"}, so the path Juneau matches against is only the remainder
	 * ({@code /chrome.css}). The mixin's endpoints must resolve at the stable
	 * {@code /juneau-console/chrome.css} URL in that arrangement without the host having to rewrite
	 * {@code getServletPath()}.
	 */
	private static MockRestClient standaloneMounted(Class<?> host) {
		return MockRestClient.createLax(host).servletPath("/juneau-console").build();
	}

	/** A host composed onto an existing application mount at {@code /rest/*}. */
	private static MockRestClient composedMounted(Class<?> host) {
		return MockRestClient.createLax(host).servletPath("/rest").build();
	}

	@Test void l01_standaloneMount_chromeCssResolvesAtStableUrl() throws Exception {
		standaloneMounted(DefaultHost.class).get("/chrome.css").run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/css");
	}

	@Test void l05_composedMount_servesChromeCssAndThemeStylesheets_andAssetEndpointsAreGone() throws Exception {
		// Back-compat guard for the documented composition style: a host mounted at /rest/* keeps serving the
		// mixin's chrome.css and theme stylesheet endpoints at <host-mount>/juneau-console/..., and the removed
		// logo/page-background asset endpoints no longer exist under either mount style.
		var c = MockRestClient.createLax(DefaultHost.class).servletPath("/rest").build();
		c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200);
		c.get(ConsoleChromeMixin.THEME_CSS_DIR + "juneau-theme-gray.css").run().assertStatus(200);
		c.get("/juneau-console/assets/logo").run().assertStatus(404);
	}

	@Test void l06_publicChromeCssAndThemeDirPathConstants_arePinned() {
		// These constants are the URLs consumers build <link> references from - changing a value is a silent break
		// for every deployed consumer, so pin them.
		assertEquals("/juneau-console/chrome.css", ConsoleChromeMixin.CHROME_CSS_PATH);
		assertEquals("/juneau-console/themes/", ConsoleChromeMixin.THEME_CSS_DIR);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// m) Per-mount body cache
	//-----------------------------------------------------------------------------------------------------------------

	static final ConsoleChromeMixin MOUNT_CACHE_MIXIN = ConsoleChromeMixin.create().theme("gray").build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class MountCacheHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return MOUNT_CACHE_MIXIN; }
	}

	/**
	 * {@code cacheAssets(true)} caches the assembled body, keyed by mount (see {@code mountKey}) even though the
	 * body no longer carries any mount-derived URL - so one mixin instance reached under two mount styles still
	 * assembles its (identical) body exactly once per distinct mount.
	 */
	@Test void m12_cachedBody_isKeyedByMount_assembledOncePerDistinctMount() throws Exception {
		var standalone1 = standaloneMounted(MountCacheHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		var composed = composedMounted(MountCacheHost.class).get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
		var standalone2 = standaloneMounted(MountCacheHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		assertEquals(standalone1, standalone2, "the standalone body must come back from cache unchanged");
		assertEquals(standalone1, composed, "stock chrome.css carries no mount-dependent URL");
		assertEquals(2, MOUNT_CACHE_MIXIN.debugBuildCount(), "expected exactly one assembly per distinct mount");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// n) Tag colour palette: the red triad that makes a four-state (pass/warn/fail/unknown) vocabulary expressible
	//-----------------------------------------------------------------------------------------------------------------

	/** Every colour family in the tag palette, in the order Theme.OPEN declares them. */
	private static final List<String> TAG_PALETTE = List.of("green", "blue", "amber", "neutral", "red");

	/** The three properties every tag colour family covers - a family missing one of them cannot paint a whole pill. */
	private static final List<String> TAG_TRIAD_PROPERTIES = List.of("bg", "text", "border");

	@Test void n01_everyTagColourFamily_isACompleteTriad_includingRed() {
		for (var colour : TAG_PALETTE)
			for (var property : TAG_TRIAD_PROPERTIES) {
				var name = "--jc-tag-" + colour + '-' + property;
				assertTrue(Theme.OPEN.getTokens().containsKey(name), () -> "Theme.OPEN is missing tag token '" + name + "'");
			}
	}

	@Test void n02_redTriadValues_areLiteralHexColours_andSurviveTheValueGrammar() {
		for (var property : TAG_TRIAD_PROPERTIES) {
			var value = Theme.OPEN.getTokens().get("--jc-tag-red-" + property);
			assertNotNull(value, () -> "no value for --jc-tag-red-" + property);
			assertTrue(value.matches("#[0-9a-f]{6}"), () -> "--jc-tag-red-" + property + " is not a 6-digit hex colour like its siblings: " + value);
			assertEquals(value, CssValueGrammar.normalizeAndValidate(value));
		}
	}

	@Test void n03_chromeCss_mapsAFailValueOntoTheRedTriad() throws IOException {
		var css = readChromeCss();
		assertTrue(css.contains(".tag.status.fail"), () -> "no .tag.status.fail mapping rule, css:\n" + css);
		for (var property : TAG_TRIAD_PROPERTIES)
			assertTrue(css.contains("var(--jc-tag-red-" + property + ")"), () -> "--jc-tag-red-" + property + " is defined but never consumed");
	}

	@Test void n04_servedChromeCss_emitsTheRedTriad_alongsideTheOtherFourFamilies() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(DefaultHost.class));
		for (var colour : TAG_PALETTE)
			for (var property : TAG_TRIAD_PROPERTIES) {
				var declaration = "--jc-tag-" + colour + '-' + property + ':';
				assertTrue(body.contains(declaration), () -> "served chrome.css never declares '" + declaration + "', body:\n" + body);
			}
	}

	/**
	 * The point of the red family: a four-state {@code pass}/{@code warn}/{@code fail}/{@code unknown} vocabulary
	 * has to reach four distinct colours. Sharing one between {@code warn} and {@code fail} would make a check that
	 * could not run indistinguishable from one that passed with a caveat.
	 */
	@Test void n06_fourStateStatusVocabulary_resolvesToFourDistinctFills() {
		var fills = new LinkedHashSet<String>();
		for (var colour : List.of("green", "amber", "red", "neutral")) {
			var fill = Theme.OPEN.getTokens().get("--jc-tag-" + colour + "-bg");
			assertNotNull(fill, () -> "no fill for the '" + colour + "' family");
			fills.add(fill);
		}
		assertEquals(4, fills.size(), () -> "pass/warn/fail/unknown collapse onto fewer than four fills: " + fills);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// n') Pill palette (Task 15): --jc-pill-* is the CANONICAL status-chip colour family the client `pill` catalog
	//     renderer paints from; the legacy --jc-tag-* triads are retained as var(--jc-pill-*) aliases (badges/avatars/
	//     frozen calendar), so no cross-module rename is needed and every value stays pixel-identical.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void n07_everyPillColourFamily_isACompleteTriad_ofLiteralHex() {
		// The pill family mirrors the tag family: green/blue/amber/neutral/red x bg/text/border, all 6-digit hex.
		for (var colour : TAG_PALETTE)
			for (var property : TAG_TRIAD_PROPERTIES) {
				var name = "--jc-pill-" + colour + '-' + property;
				var value = Theme.OPEN.getTokens().get(name);
				assertNotNull(value, () -> "Theme.OPEN is missing pill token '" + name + "'");
				assertTrue(value.matches("#[0-9a-f]{6}"),
					() -> name + " is not a 6-digit hex colour (canonical pill token must be a concrete literal): " + value);
			}
	}

	@Test void n08_tagTriads_resolveToTheirPillCounterparts_soTheAliasIsIntact() {
		// Each --jc-tag-<c>-<p> is authored as var(--jc-pill-<c>-<p>); build() resolves it, so getTokens() must carry
		// the SAME literal for both.  This is what lets badges/avatars keep --jc-tag-* while pills move to --jc-pill-*.
		for (var colour : TAG_PALETTE)
			for (var property : TAG_TRIAD_PROPERTIES) {
				var pill = Theme.OPEN.getTokens().get("--jc-pill-" + colour + '-' + property);
				var tag = Theme.OPEN.getTokens().get("--jc-tag-" + colour + '-' + property);
				assertEquals(pill, tag,
					() -> "--jc-tag-" + colour + '-' + property + " must resolve to its --jc-pill-* counterpart");
			}
	}

	@Test void n09_chromeCss_pillChips_paintFromThePillPalette_notTheTagPalette() throws IOException {
		// The .tag.<domain>.<value> pill-chip rules (the palette the `pill` renderer's markup lands in) must consume
		// var(--jc-pill-*): that is the migration.  (Dual-hat SLDS/lightning-naming freeze is covered separately.)
		var css = readChromeCss();
		assertTrue(css.contains(".tag.status.released"), () -> "no .tag.<domain>.<value> pill-chip palette rule, css:\n" + css);
		for (var colour : TAG_PALETTE)
			for (var property : TAG_TRIAD_PROPERTIES)
				assertTrue(css.contains("var(--jc-pill-" + colour + '-' + property + ")"),
					() -> "pill-chip palette never consumes var(--jc-pill-" + colour + '-' + property + "), css:\n" + css);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// o) Deterministic token emission: the :root{} block must be byte-stable for a given token set
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Pins that the emitted declaration order is exactly {@link Theme#getTokens()}'s iteration order, i.e. that
	 * the theme's ordering guarantee reaches the wire rather than being re-bucketed on the way out. The guarantee
	 * itself - that the iteration order is the declaration order and not a per-JVM hash order, which is what makes
	 * the response byte-stable enough to ever carry an {@code ETag} - is proved in {@code Theme_TokenOrdering_Test}.
	 */
	@Test void o01_openBlockDeclarationOrder_matchesThemeOpenThenAliasBlockDeclarationOrder() throws Exception {
		// The OPEN :root{} block emits Theme.OPEN's tokens in declaration order, followed by the framework-authored
		// role-token alias derivations in their declaration order.
		var block = firstRootBlock(bodyOf(MockRestClient.buildLax(DefaultHost.class)));
		var emitted = new ArrayList<String>();
		var m = Pattern.compile("(--jc-[a-z0-9-]++)\\s*:").matcher(block);
		while (m.find())
			emitted.add(m.group(1));
		var expected = new ArrayList<>(Theme.OPEN.getTokens().keySet());
		expected.addAll(aliasDefinedNames());
		assertEquals(expected, emitted);
	}

	@Test void o02_twoIndependentlyBuiltMixinsWithTheSameTheme_serveByteIdenticalBodies() throws Exception {
		assertEquals(bodyOf(MockRestClient.buildLax(DefaultHost.class)), bodyOf(MockRestClient.buildLax(DefaultHost.class)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// q) Emitted token blocks carry the html:root type prefix, so a theme token out-ranks a same-named token
	//    declared at :root by a separately linked stylesheet regardless of <link> order
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Both emission sites must carry the {@code html} type prefix, not just one.
	 *
	 * <p>
	 * Two blocks at plain {@code :root} in separately linked stylesheets tie at specificity {@code (0,0,1,0)} and
	 * are resolved by the order the consumer's {@code <link>} elements appear in &mdash; which this framework
	 * neither sets nor can observe, so a theme override is silently defeated whenever a consumer links the other
	 * way round. {@code html:root} scores {@code (0,0,1,1)} and wins in either order.
	 *
	 * <p>
	 * Deliberately asserted as an <i>anchored</i> prefix rather than a {@code contains("html:root&#123;")}: the
	 * pre-existing helpers in this class match {@code :root&#123;} as a substring, and {@code html:root&#123;} contains
	 * that, so every one of them stays green whether or not the prefix is emitted. A substring assertion here
	 * would inherit exactly that blind spot and pass against the unfixed emitter.
	 */
	@Test void q01_bothEmittedTokenBlocks_carryTheHtmlTypePrefix() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(LightBrownHost.class));
		assertEquals(2, countRootBlocks(body), () -> "expected Theme.OPEN block + light-brown's override block, body:\n" + body);
		var m = Pattern.compile("(.{0,5}):root\\{").matcher(body);
		var n = 0;
		while (m.find()) {
			n++;
			assertEquals("html", m.group(1), () -> "token block emitted without the html type prefix, body:\n" + body);
		}
		assertEquals(2, n, () -> "expected both emission sites to be checked, body:\n" + body);
	}

	/**
	 * The static {@code chrome.css} must keep shipping no token block of its own, which is what makes appending
	 * one work at all. Guards the other half of (q01): a {@code :root} appearing in the static file would be
	 * counted by (q01)'s matcher and would not carry the prefix.
	 */
	@Test void q02_staticChromeCss_shipsNoTokenBlockOfItsOwn() throws IOException {
		var css = readChromeCss();
		var m = Pattern.compile("^\\s*+(html)?+:root\\s*+\\{", Pattern.MULTILINE).matcher(css);
		assertFalse(m.find(), () -> "static chrome.css must ship no :root block of its own");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test helpers
	//-----------------------------------------------------------------------------------------------------------------

	private static String bodyOf(MockRestClient client) throws Exception {
		return client.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
	}

	private static int countRootBlocks(String body) {
		var m = Pattern.compile(":root\\{").matcher(body);
		var n = 0;
		while (m.find())
			n++;
		return n;
	}

	private static String firstRootBlock(String body) {
		var start = body.indexOf(":root{");
		var end = body.indexOf('}', start);
		return body.substring(start, end + 1);
	}

}
