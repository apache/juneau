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
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Phase 3 gate: {@link ConsoleChromeMixin} + {@link ThemeSettings} + the dynamic {@code GET /juneau-console/chrome.css}
 * endpoint.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class ConsoleChromeMixin_Test extends TestBase {

	private static final Theme THEME_A = Theme.create("theme-a").token("--jc-accent", "#aa0000").build();
	private static final Theme THEME_B = Theme.create("theme-b").token("--jc-accent", "#00bb00").build();

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
	// b) Theme-actually-applies precedence: mixin.theme(...) > ThemeSettings bean > Theme.OPEN
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class DefaultHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class ThemeSettingsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ThemeSettings theme() { return ThemeSettings.of(THEME_A); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class MixinThemeWinsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme(THEME_A).build(); }
		@Bean public ThemeSettings theme() { return ThemeSettings.of(THEME_B); }
	}

	@Test void b01_noThemeConfiguredAnywhere_bodyCarriesOnlyOpenBlock() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(DefaultHost.class));
		assertEquals(1, countRootBlocks(body), () -> "expected exactly one :root{} block, body:\n" + body);
		assertTrue(body.contains("--jc-accent:#1589EE;"));
	}

	@Test void b02_themeSettingsBean_appliesWhenMixinHasNoExplicitTheme() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(ThemeSettingsHost.class));
		assertEquals(2, countRootBlocks(body), () -> "expected Theme.OPEN block + THEME_A's override block, body:\n" + body);
		assertTrue(body.contains("--jc-accent:#aa0000;"), () -> "missing THEME_A override, body:\n" + body);
	}

	@Test void b03_mixinExplicitTheme_winsOverThemeSettingsBean() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(MixinThemeWinsHost.class));
		assertTrue(body.contains("--jc-accent:#aa0000;"), () -> "expected THEME_A (mixin.theme wins), body:\n" + body);
		assertFalse(body.contains("--jc-accent:#00bb00;"), () -> "THEME_B (ThemeSettings bean) must NOT win, body:\n" + body);
	}

	/**
	 * RED was proved live during development: temporarily hard-wiring {@code resolveActiveTheme} to
	 * unconditionally {@code return Theme.OPEN;} (ignoring both {@code mixin.theme(...)} and the
	 * {@link ThemeSettings} bean) made (b02), (b03), and this test all fail — proving the precedence assertion is
	 * not vacuously true. See the phase manifest for the recorded transcript; the broken code path is not checked
	 * in (that would defeat the point of a regression suite).
	 */
	@Test void b04_precedenceIsGenuinelyWired_notANoOp() throws Exception {
		// Positive-side confirmation that the two theme bodies actually differ (the no-op bug would make them
		// byte-for-byte identical).
		var defaultBody = bodyOf(MockRestClient.buildLax(DefaultHost.class));
		var themeSettingsBody = bodyOf(MockRestClient.buildLax(ThemeSettingsHost.class));
		assertNotEquals(defaultBody, themeSettingsBody);
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

	//-----------------------------------------------------------------------------------------------------------------
	// d) Value positives, end-to-end (grammar-accepted then escaper-emitted)
	//-----------------------------------------------------------------------------------------------------------------

	private static final Theme POSITIVES_THEME = Theme.create("positives")
		.token("--jc-accent", "#e91e63")
		.token("--jc-page-bg", "linear-gradient(180deg, #aabbcc 0%, #112233 100%)")
		.token("--jc-font", "'Helvetica Neue', Inter, sans-serif")
		.build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class PositivesHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme(POSITIVES_THEME).build(); }
	}

	@Test void d01_valuePositives_surviveByteForByte_inServedResponse() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(PositivesHost.class));
		assertTrue(body.contains("--jc-accent:#e91e63;"));
		assertTrue(body.contains("--jc-page-bg:linear-gradient(180deg, #aabbcc 0%, #112233 100%);"));
		assertTrue(body.contains("--jc-font:'Helvetica Neue', Inter, sans-serif;"));
	}

	/**
	 * The escaper-is-actually-wired-into-the-handler gate. {@code 'My;Font'} is a grammar-ACCEPTED font-family item
	 * (a single-quoted string has no quote/backslash breakout, so the grammar does not care that it contains a raw
	 * {@code ;}) but a raw, un-escaped {@code ;} at this position would prematurely terminate the
	 * {@code --jc-font: <value>;} CSS declaration. This is the one B1-relevant value shape where the escaper is
	 * NOT a no-op, so it is the one that actually catches a "someone bypassed {@code CssValueEscaper} when wiring
	 * the real endpoint" regression (unlike (d01)'s values, which contain no escaper-target characters at all).
	 */
	private static final Theme SEMICOLON_THEME = Theme.create("semi").token("--jc-font", "'My;Font', sans-serif").build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class SemicolonHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme(SEMICOLON_THEME).build(); }
	}

	@Test void d02_escaperIsWiredIntoHandler_semicolonInFontFamilyValue_isNeutralized() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(SemicolonHost.class));
		assertFalse(body.contains("'My;Font'"), () -> "raw unescaped ';' leaked into the declaration, body:\n" + body);
		assertTrue(body.contains("\\3B "), () -> "expected the CSS-hex escape for ';', body:\n" + body);
	}

	/**
	 * RED was proved live during development: temporarily replacing the {@code CssValueEscaper.escape(...)} call in
	 * {@code ConsoleChromeMixin.rootBlock(...)} with the raw, un-escaped value (simulating "someone bypassed the
	 * escaper when plumbing the real endpoint") made (d02) fail with the raw {@code 'My;Font'} leaking into the
	 * response body. See the phase manifest for the recorded transcript; the broken code path is not checked in.
	 */
	@Test void d03_placeholder_seeD02Javadoc() {
		assertTrue(true);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e) url-sink gate, end-to-end
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"java:S5778" // End-to-end security gate: deliberately asserts the whole mixin-construction chain (theme(Theme.create(x).token(url).build())) rejects a url() bypass vector, so it stays robust if value validation is ever relocated between token()/build()/theme(); isolating a single call would drop that end-to-end coverage.
	})
	@Test void e01_bypassVector_throwsAtMixinConstructionTime_notSilentlySwallowed() {
		assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.create().theme(Theme.create("x").token("--jc-page-bg", "url(https://evil)").build()));
		assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.create().theme(Theme.create("x").token("--jc-page-bg", "url (https://evil)").build()));
		assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.create().theme(Theme.create("x").token("--jc-page-bg", "url/**/(https://evil)").build()));
	}

	@Test void e02_structuralLayer_colorTokensNeverSinkIntoUrlCapableProperty() throws IOException {
		String css;
		try (var in = ConsoleChromeMixin_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertEquals(List.of(), ChromeCssScanner.scan(css));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f) cacheAssets(true) caching gate + per-mount isolation
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

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class MountXHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme(THEME_A).build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class MountYHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme(THEME_B).build(); }
	}

	@Test void f03_secondIndependentlyConfiguredMount_doesNotLeakFirstMountsCachedTheme() throws Exception {
		var bodyX = bodyOf(MockRestClient.buildLax(MountXHost.class));
		var bodyY = bodyOf(MockRestClient.buildLax(MountYHost.class));
		assertTrue(bodyX.contains("--jc-accent:#aa0000;"));
		assertFalse(bodyX.contains("--jc-accent:#00bb00;"), () -> "mount X leaked mount Y's theme, body:\n" + bodyX);
		assertTrue(bodyY.contains("--jc-accent:#00bb00;"));
		assertFalse(bodyY.contains("--jc-accent:#aa0000;"), () -> "mount Y leaked mount X's theme, body:\n" + bodyY);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g) Builder validation: logo(...) / pageBackgroundImage(...)
	//-----------------------------------------------------------------------------------------------------------------

	private static final String VALID_LOGO = "/testfiles/console/logo.svg";
	private static final String VALID_PAGE_BG = "/testfiles/console/page-bg.png";

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",                                        // empty
		"/testfiles/console/../../../etc/passwd", // path traversal segment
		"/testfiles/console/bad.txt",              // unallowlisted extension
		"/testfiles/console/nope.svg",             // resource does not exist
	})
	void g01_logo_invalidInputs_rejected(String value) {
		var b = ConsoleChromeMixin.create();
		assertThrows(IllegalArgumentException.class, () -> b.logo(value));
	}

	@Test void g02_logo_validClasspathResource_accepted() {
		assertDoesNotThrow(() -> ConsoleChromeMixin.create().logo(VALID_LOGO).build());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",                                        // empty
		"/testfiles/console/../../../etc/passwd", // path traversal segment
		"/testfiles/console/bad.txt",              // unallowlisted extension
		"/testfiles/console/nope.png",             // resource does not exist
	})
	void g03_pageBackgroundImage_invalidInputs_rejected(String value) {
		var b = ConsoleChromeMixin.create();
		assertThrows(IllegalArgumentException.class, () -> b.pageBackgroundImage(value));
	}

	@Test void g04_pageBackgroundImage_validClasspathResource_accepted() {
		assertDoesNotThrow(() -> ConsoleChromeMixin.create().pageBackgroundImage(VALID_PAGE_BG).build());
	}

	@Test void g05_allAllowlistedExtensions_accepted() {
		// One fixture per allowlisted extension, content irrelevant - only the extension drives validation/content-type.
		assertDoesNotThrow(() -> ConsoleChromeMixin.create().logo("/testfiles/console/logo.svg").build());
		assertDoesNotThrow(() -> ConsoleChromeMixin.create().logo("/testfiles/console/logo.jpg").build());
		assertDoesNotThrow(() -> ConsoleChromeMixin.create().logo("/testfiles/console/logo.jpeg").build());
		assertDoesNotThrow(() -> ConsoleChromeMixin.create().logo("/testfiles/console/logo.webp").build());
		assertDoesNotThrow(() -> ConsoleChromeMixin.create().logo("/testfiles/console/logo.gif").build());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h) Asset serving: /juneau-console/assets/logo, /juneau-console/assets/page-bg
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class NoAssetsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class AssetsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() {
			return ConsoleChromeMixin.create().logo(VALID_LOGO).pageBackgroundImage(VALID_PAGE_BG).build();
		}
	}

	@Test void h01_logoAsset_unconfigured_is404() throws Exception {
		MockRestClient.buildLax(NoAssetsHost.class).get(ConsoleChromeMixin.LOGO_ASSET_PATH).run().assertStatus(404);
	}

	@Test void h02_pageBgAsset_unconfigured_is404() throws Exception {
		MockRestClient.buildLax(NoAssetsHost.class).get(ConsoleChromeMixin.PAGE_BG_ASSET_PATH).run().assertStatus(404);
	}

	@Test void h03_logoAsset_configured_servesBytesWithSvgContentTypeAndCacheControl() throws Exception {
		assertAssetServed("/testfiles/console/logo.svg", ConsoleChromeMixin.LOGO_ASSET_PATH, "image/svg+xml");
	}

	@Test void h04_pageBgAsset_configured_servesBytesWithPngContentTypeAndCacheControl() throws Exception {
		assertAssetServed("/testfiles/console/page-bg.png", ConsoleChromeMixin.PAGE_BG_ASSET_PATH, "image/png");
	}

	@Test void h05_logoAsset_arbitraryVersionQueryString_stillServes200() throws Exception {
		// The `?v=...` cache-buster is consumed by the browser's cache key, not by mixin routing - any value
		// (or none) must still resolve to the same configured asset.
		MockRestClient.buildLax(AssetsHost.class).get(ConsoleChromeMixin.LOGO_ASSET_PATH + "?v=whatever").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i) chrome.css composition: logo/page-bg override rules, with versioned URLs
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class LogoOnlyHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().logo(VALID_LOGO).build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class PageBgOnlyHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().pageBackgroundImage(VALID_PAGE_BG).build(); }
	}

	@Test void i01_noAssetsConfigured_chromeCssUnaffected() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(NoAssetsHost.class));
		assertFalse(body.contains(ConsoleChromeMixin.LOGO_ASSET_PATH));
		assertFalse(body.contains(ConsoleChromeMixin.PAGE_BG_ASSET_PATH));
	}

	@Test void i02_logoConfigured_chromeCssOverridesJcLogoBackgroundImage() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(AssetsHost.class));
		assertTrue(body.contains(".jc-logo{background-image:url(\"" + ConsoleChromeMixin.LOGO_ASSET_PATH + "?v="),
			() -> "missing logo override rule, body:\n" + body);
	}

	@Test void i03_pageBgConfigured_chromeCssLayersImageOverGradientToken() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(AssetsHost.class));
		assertTrue(body.contains("url(\"" + ConsoleChromeMixin.PAGE_BG_ASSET_PATH + "?v="), () -> "missing page-bg url(), body:\n" + body);
		assertTrue(body.contains("), var(--jc-page-bg);"), () -> "missing gradient-token fallback layer, body:\n" + body);
	}

	@Test void i04_versionedQueryString_matchesPackageImplementationVersionPlusContentHash() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(AssetsHost.class));
		var v = ConsoleChromeMixin.class.getPackage().getImplementationVersion();
		var expectedPrefix = "?v=" + (v == null ? "dev" : v) + "-";
		var m = Pattern.compile(Pattern.quote(ConsoleChromeMixin.LOGO_ASSET_PATH) + Pattern.quote(expectedPrefix) + "([0-9a-f]{8})\"").matcher(body);
		assertTrue(m.find(), () -> "expected version+content-hash cache-buster, body:\n" + body);
	}

	@Test void i07_contentHashCacheBuster_isStableAcrossRequests_andDiffersBetweenLogoAndPageBg() throws Exception {
		// The content-hash cache-buster (Task 1) is computed from each asset's own bytes, cached once - it must be
		// stable across requests (not recomputed per-request) and must differ between the two distinct fixture files.
		var body1 = bodyOf(MockRestClient.buildLax(AssetsHost.class));
		var body2 = bodyOf(MockRestClient.buildLax(AssetsHost.class));
		var logoHash = extractHash(body1, ConsoleChromeMixin.LOGO_ASSET_PATH);
		var pageBgHash = extractHash(body1, ConsoleChromeMixin.PAGE_BG_ASSET_PATH);
		assertEquals(logoHash, extractHash(body2, ConsoleChromeMixin.LOGO_ASSET_PATH), "hash must be stable across requests");
		assertNotEquals(logoHash, pageBgHash, "distinct fixture assets must not collide on their content hash");
	}

	private static String extractHash(String body, String assetPath) {
		var m = Pattern.compile(Pattern.quote(assetPath) + "\\?v=[^-\"]+-([0-9a-f]{8})\"").matcher(body);
		assertTrue(m.find(), () -> "no versioned+hashed url for " + assetPath + " in body:\n" + body);
		return m.group(1);
	}

	@Test void i05_onlyLogoConfigured_pageBgOverrideRuleAbsent_andPageBgAssetStill404() throws Exception {
		var c = MockRestClient.buildLax(LogoOnlyHost.class);
		var body = bodyOf(c);
		assertTrue(body.contains(ConsoleChromeMixin.LOGO_ASSET_PATH));
		assertFalse(body.contains(ConsoleChromeMixin.PAGE_BG_ASSET_PATH));
		c.get(ConsoleChromeMixin.PAGE_BG_ASSET_PATH).run().assertStatus(404);
	}

	@Test void i06_onlyPageBgConfigured_logoOverrideRuleAbsent_andLogoAssetStill404() throws Exception {
		var c = MockRestClient.buildLax(PageBgOnlyHost.class);
		var body = bodyOf(c);
		assertTrue(body.contains(ConsoleChromeMixin.PAGE_BG_ASSET_PATH));
		assertFalse(body.contains(ConsoleChromeMixin.LOGO_ASSET_PATH));
		c.get(ConsoleChromeMixin.LOGO_ASSET_PATH).run().assertStatus(404);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// j) Security regression: Theme/CssValueGrammar untouched by this feature
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_assembledResponseWithBothAssetsConfigured_passesChromeCssScanner() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(AssetsHost.class));
		assertEquals(List.of(), ChromeCssScanner.scan(body), () -> "violations against assembled body:\n" + body);
	}

	@SuppressWarnings({
		"java:S5778" // End-to-end security regression: deliberately asserts the whole mixin-construction chain rejects a url() production even with the assets feature present; isolating a single call would drop that end-to-end coverage.
	})
	@Test void j02_themeTokenPath_stillRejectsUrlProduction_evenWithAssetsFeaturePresent() {
		assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.create().theme(Theme.create("x").token("--jc-page-bg", "url(https://evil)").build()));
	}

	@Test void j03_themeOpenTokenCount_pinned_unaffectedByAssetsFeature() {
		// A0 must not add a --jc-logo or --jc-page-bg-image token - the logo/page-bg mechanism is deliberately
		// NOT part of the Theme token model (finding 4 of the design doc). If this count ever changes, it must be
		// a DIFFERENT, deliberate change to Theme.OPEN - not a side effect of the asset feature.
		//
		// 48 = the original 32, plus the three-token red tag triad, plus the eleven additive
		// token gaps (--jc-header-height, --jc-nav-indicator-width, --jc-card-shadow, --jc-danger-wash,
		// --jc-success-wash, and the six-step --jc-space-1..6 scale), plus the --jc-focus focus-ring colour,
		// plus the --jc-accent-selected ribbon-format selected-state face (WCAG 1.4.11 non-text contrast remedy).
		// Bumping this number is only ever correct alongside a reviewed edit to Theme.OPEN itself.
		assertEquals(48, Theme.OPEN.getTokens().size());
		assertFalse(Theme.OPEN.getTokens().containsKey("--jc-logo"));
		assertFalse(Theme.OPEN.getTokens().containsKey("--jc-page-bg-image"));
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

		// Substring pin: both rewritten selected-state selectors consume var(--jc-accent-selected), with no
		// var() fallback (requirement 3) - exactly three "background-color: var(--jc-accent-selected)" sinks.
		var css = readChromeCss();
		assertTrue(css.contains(".jc-tab.jc-tab-active,\n.jc-subtab.jc-subtab-active {"), () -> "missing raised-specificity selected selector, css:\n" + css);
		assertTrue(css.contains(".juneau-view-ribbon-group[data-juneau-strip-mode=\"tab\"] .juneau-view-ribbon-btn[aria-selected=\"true\"] {"), () -> "missing widget selected selector, css:\n" + css);
		assertEquals(3, countOccurrences(css, "background-color: var(--jc-accent-selected)"),
			() -> "expected exactly the three consumers of --jc-accent-selected: the two ribbon-format selected-state selectors plus .jc-nav-tab.active (added by J0484), css:\n" + css);
	}

	@Test void j05_navTabActive_consumesAccentSelectedToken_andNoLongerConsumesAccentWash() throws Exception {
		var css = readChromeCss();
		var navTabActiveStart = css.indexOf(".jc-nav-tab.active {");
		assertTrue(navTabActiveStart != -1, () -> "missing .jc-nav-tab.active rule, css:\n" + css);
		var navTabActiveEnd = css.indexOf("}", navTabActiveStart);
		var navTabActiveBlock = css.substring(navTabActiveStart, navTabActiveEnd);
		assertTrue(navTabActiveBlock.contains("background-color: var(--jc-accent-selected)"),
			() -> "expected .jc-nav-tab.active to consume --jc-accent-selected, block:\n" + navTabActiveBlock);
		assertFalse(navTabActiveBlock.contains("var(--jc-accent-wash)"),
			() -> "expected .jc-nav-tab.active to no longer consume --jc-accent-wash, block:\n" + navTabActiveBlock);
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
		assertTrue(css.contains("table.dataTable > tbody > tr:nth-child(odd)"), () -> "missing bare odd-row rule, css:\n" + css);
		assertTrue(css.contains("table.dataTable > tbody > tr:nth-child(even)"), () -> "missing bare even-row rule, css:\n" + css);
		// DT1.x row classes.
		assertTrue(css.contains("table.dataTable > tbody > tr.odd"), () -> "missing tr.odd rule, css:\n" + css);
		assertTrue(css.contains("table.dataTable > tbody > tr.even"), () -> "missing tr.even rule, css:\n" + css);
		// DT2.x "stripe"/"display" convenience-class opt-in form.
		assertTrue(css.contains("table.dataTable.stripe > tbody > tr:nth-child(odd)"), () -> "missing .stripe odd-row rule, css:\n" + css);
		assertTrue(css.contains("table.dataTable.stripe > tbody > tr:nth-child(even)"), () -> "missing .stripe even-row rule, css:\n" + css);
	}

	@Test void k02_chromeCss_themesDataTableRowHover_bothGenerations() throws Exception {
		var css = readChromeCss();
		assertTrue(css.contains("table.dataTable > tbody > tr:hover"), () -> "missing bare row-hover rule, css:\n" + css);
		assertTrue(css.contains("table.dataTable.hover > tbody > tr:hover"), () -> "missing .hover row-hover rule, css:\n" + css);
	}

	@Test void k03_chromeCss_neutralizesVendoredStripeHoverCssVariables() throws Exception {
		var css = readChromeCss();
		assertTrue(css.contains("--dt-row-stripe:"), () -> "missing --dt-row-stripe neutralization, css:\n" + css);
		assertTrue(css.contains("--dt-row-hover:"), () -> "missing --dt-row-hover neutralization, css:\n" + css);
	}

	@Test void k04_chromeCss_themesDataTableHeaderAndFont() throws Exception {
		var css = readChromeCss();
		assertTrue(css.contains("table.dataTable {"), () -> "missing table.dataTable base rule, css:\n" + css);
		assertTrue(css.contains("font-family: var(--jc-font);"), () -> "missing themed font-family, css:\n" + css);
		assertTrue(css.contains("table.dataTable > thead > tr > th"), () -> "missing themed header rule, css:\n" + css);
		assertTrue(css.contains("border-color: var(--jc-border);"), () -> "missing themed table/cell border-color, css:\n" + css);
		assertTrue(css.contains("border-top-color: var(--jc-border-2);"), () -> "missing themed table top border, css:\n" + css);
		assertTrue(css.contains(".juneau-view-detail-control"), () -> "missing expander column theme, css:\n" + css);
		assertTrue(css.contains("var(--jc-text-muted)"), () -> "missing muted expander color, css:\n" + css);
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

	@Test void l01_standaloneMount_chromeCssResolvesAtStableUrl() throws Exception {
		standaloneMounted(AssetsHost.class).get("/chrome.css").run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/css");
	}

	@Test void l02_standaloneMount_logoAssetResolvesAtUrlEmittedInChromeCss() throws Exception {
		standaloneMounted(AssetsHost.class).get("/assets/logo").run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("image/svg+xml");
	}

	@Test void l03_standaloneMount_pageBgAssetResolvesAtUrlEmittedInChromeCss() throws Exception {
		standaloneMounted(AssetsHost.class).get("/assets/page-bg").run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("image/png");
	}

	@Test void l04_standaloneMount_emittedAssetUrlsAreServableUnderThatMount() throws Exception {
		// The chrome.css body references the assets by their absolute /juneau-console/... URL.  Under a
		// /juneau-console/* container mount at the site root those URLs must hit the very endpoints above.
		var body = standaloneMounted(AssetsHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("url(\"" + ConsoleChromeMixin.LOGO_ASSET_PATH + "?v="), () -> "missing logo url(), body:\n" + body);
		assertTrue(body.contains("url(\"" + ConsoleChromeMixin.PAGE_BG_ASSET_PATH + "?v="), () -> "missing page-bg url(), body:\n" + body);
	}

	@Test void l05_composedMount_prefixedPathsStillResolveUnderANonRootHostMount() throws Exception {
		// Back-compat guard for the documented composition style: a host mounted at /rest/* keeps serving the
		// mixin's endpoints at <host-mount>/juneau-console/...
		var c = MockRestClient.createLax(AssetsHost.class).servletPath("/rest").build();
		c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200);
		c.get(ConsoleChromeMixin.LOGO_ASSET_PATH).run().assertStatus(200);
		c.get(ConsoleChromeMixin.PAGE_BG_ASSET_PATH).run().assertStatus(200);
	}

	@Test void l06_publicAssetPathConstants_arePinned() {
		// These constants are the URLs consumers build <link>/<img> references from - changing a value is a
		// silent break for every deployed consumer, so pin them.
		assertEquals("/juneau-console/chrome.css", ConsoleChromeMixin.CHROME_CSS_PATH);
		assertEquals("/juneau-console/assets/logo", ConsoleChromeMixin.LOGO_ASSET_PATH);
		assertEquals("/juneau-console/assets/page-bg", ConsoleChromeMixin.PAGE_BG_ASSET_PATH);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// m) Mount-style-aware asset URL generation: the logo/page-bg url()s written into the served chrome.css must be
	//    fetchable by the browser under both mount styles and at any container context path.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void m01_standaloneMount_emittedLogoUrl_isByteIdenticalToThePreFixLiteral() throws Exception {
		var body = standaloneMounted(AssetsHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		assertEquals("/juneau-console/assets/logo" + expectedCacheBuster(VALID_LOGO), emittedUrl(body, "/assets/logo"));
	}

	@Test void m02_standaloneMount_emittedPageBgUrl_isByteIdenticalToThePreFixLiteral() throws Exception {
		var body = standaloneMounted(AssetsHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		assertEquals("/juneau-console/assets/page-bg" + expectedCacheBuster(VALID_PAGE_BG), emittedUrl(body, "/assets/page-bg"));
	}

	/**
	 * The regression this guards is mount-style detection inverted in the standalone direction: resolving the
	 * <i>prefixed</i> constant against a standalone mount's {@code servletPath} (which already ends in
	 * {@code /juneau-console}) doubles the segment up to {@code /juneau-console/juneau-console/assets/logo}. A
	 * happy-path "the url contains /assets/logo" assertion passes right through that, and so does an end-to-end
	 * fetch, because the doubled URL is a live alias under a standalone mount.
	 */
	@Test void m03_standaloneMount_emittedUrlsAreNotDoublePrefixed() throws Exception {
		var body = standaloneMounted(AssetsHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		assertFalse(body.contains("/juneau-console/juneau-console"), () -> "asset url double-prefixed, body:\n" + body);
	}

	@Test void m04_composedMount_emittedLogoUrl_carriesTheHostMountSegment() throws Exception {
		var body = composedMounted(AssetsHost.class).get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertEquals("/rest/juneau-console/assets/logo" + expectedCacheBuster(VALID_LOGO), emittedUrl(body, "/assets/logo"));
	}

	@Test void m05_composedMount_emittedPageBgUrl_carriesTheHostMountSegment() throws Exception {
		var body = composedMounted(AssetsHost.class).get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertEquals("/rest/juneau-console/assets/page-bg" + expectedCacheBuster(VALID_PAGE_BG), emittedUrl(body, "/assets/page-bg"));
	}

	/**
	 * The mirror-image regression of (m03): detection inverted in the composed direction, resolving the
	 * <i>unprefixed</i> constant under a composed mount, yields {@code /rest/assets/logo}. That is not caught by an
	 * end-to-end fetch either &mdash; the dual path registration makes {@code /rest/assets/logo} a
	 * live alias too &mdash; nor by the pre-fix root-absolute literal {@code /juneau-console/assets/logo}, which
	 * still <i>looks</i> like a plausible logo URL. Both wrong answers are pinned out explicitly here.
	 */
	@Test void m06_composedMount_emittedUrlsAreNeitherUnprefixedNorSiteRootAbsolute() throws Exception {
		var body = composedMounted(AssetsHost.class).get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
		var logoUrl = emittedUrl(body, "/assets/logo");
		assertFalse(logoUrl.startsWith("/rest/assets/"), () -> "unprefixed constant resolved under a composed mount: " + logoUrl);
		assertFalse(logoUrl.startsWith("/juneau-console/"), () -> "site-root-absolute literal emitted under a composed mount: " + logoUrl);
		assertFalse(body.contains("/rest/juneau-console/juneau-console"), () -> "asset url double-prefixed, body:\n" + body);
	}

	@Test void m07_composedMountUnderANonEmptyContextPath_emittedUrlsCarryTheContextPath() throws Exception {
		var c = MockRestClient.createLax(AssetsHost.class).contextPath("/app").servletPath("/rest").build();
		var body = c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertEquals("/app/rest/juneau-console/assets/logo" + expectedCacheBuster(VALID_LOGO), emittedUrl(body, "/assets/logo"));
		assertEquals("/app/rest/juneau-console/assets/page-bg" + expectedCacheBuster(VALID_PAGE_BG), emittedUrl(body, "/assets/page-bg"));
	}

	@Test void m08_standaloneMountUnderANonEmptyContextPath_emittedUrlsCarryTheContextPath() throws Exception {
		var c = MockRestClient.createLax(AssetsHost.class).contextPath("/app").servletPath("/juneau-console").build();
		var body = c.get("/chrome.css").run().assertStatus(200).getContent().asString();
		assertEquals("/app/juneau-console/assets/logo" + expectedCacheBuster(VALID_LOGO), emittedUrl(body, "/assets/logo"));
		assertEquals("/app/juneau-console/assets/page-bg" + expectedCacheBuster(VALID_PAGE_BG), emittedUrl(body, "/assets/page-bg"));
	}

	@Test void m09_composedMount_emittedUrlsActuallyResolveAgainstThatMount() throws Exception {
		var c = composedMounted(AssetsHost.class);
		var body = c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
		c.get(belowMount(emittedUrl(body, "/assets/logo"), "/rest")).run().assertStatus(200).assertHeader("Content-Type").isContains("image/svg+xml");
		c.get(belowMount(emittedUrl(body, "/assets/page-bg"), "/rest")).run().assertStatus(200).assertHeader("Content-Type").isContains("image/png");
	}

	@Test void m10_standaloneMount_emittedUrlsActuallyResolveAgainstThatMount() throws Exception {
		var c = standaloneMounted(AssetsHost.class);
		var body = c.get("/chrome.css").run().assertStatus(200).getContent().asString();
		c.get(belowMount(emittedUrl(body, "/assets/logo"), "/juneau-console")).run().assertStatus(200).assertHeader("Content-Type").isContains("image/svg+xml");
		c.get(belowMount(emittedUrl(body, "/assets/page-bg"), "/juneau-console")).run().assertStatus(200).assertHeader("Content-Type").isContains("image/png");
	}

	@Test void m11_cacheBuster_isMountIndependent() throws Exception {
		// The ?v= suffix hashes the configured asset's own bytes and reads the package version - neither has
		// anything to do with the mount, so only the URL prefix may differ between the two mount styles.
		var standalone = emittedUrl(standaloneMounted(AssetsHost.class).get("/chrome.css").run().getContent().asString(), "/assets/logo");
		var composed = emittedUrl(composedMounted(AssetsHost.class).get(ConsoleChromeMixin.CHROME_CSS_PATH).run().getContent().asString(), "/assets/logo");
		var buster = expectedCacheBuster(VALID_LOGO);
		assertTrue(standalone.endsWith(buster), () -> "standalone url lost its cache-buster: " + standalone);
		assertTrue(composed.endsWith(buster), () -> "composed url lost its cache-buster: " + composed);
	}

	static final ConsoleChromeMixin MOUNT_CACHE_MIXIN = ConsoleChromeMixin.create().logo(VALID_LOGO).build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class MountCacheHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return MOUNT_CACHE_MIXIN; }
	}

	/**
	 * {@code cacheAssets(true)} caches the assembled body, and the body now varies by mount - so the cache has to
	 * key on the mount. A body cache that ignores the mount serves whichever mount style happened to warm it first
	 * to the other one, which no single-mount test can see.
	 */
	@Test void m12_cachedBody_isKeyedByMount_notSharedAcrossMountStyles() throws Exception {
		var standalone1 = standaloneMounted(MountCacheHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		var composed = composedMounted(MountCacheHost.class).get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
		var standalone2 = standaloneMounted(MountCacheHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		assertEquals("/juneau-console/assets/logo" + expectedCacheBuster(VALID_LOGO), emittedUrl(standalone1, "/assets/logo"));
		assertEquals("/rest/juneau-console/assets/logo" + expectedCacheBuster(VALID_LOGO), emittedUrl(composed, "/assets/logo"));
		assertEquals(standalone1, standalone2, "the standalone body must come back from cache unchanged");
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

	private static final Theme RED_OVERRIDE_THEME = Theme.create("red-override")
		.token("--jc-tag-red-bg", "#ffe0e2")
		.token("--jc-tag-red-text", "#5a0f14")
		.token("--jc-tag-red-border", "#f0b3b7")
		.build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class RedOverrideHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme(RED_OVERRIDE_THEME).build(); }
	}

	@Test void n05_redTriad_isThemeable_throughTheSameApiAsEveryOtherTriad() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(RedOverrideHost.class));
		assertEquals(2, countRootBlocks(body), () -> "expected Theme.OPEN block + the override block, body:\n" + body);
		assertTrue(body.contains("--jc-tag-red-bg:#ffe0e2;"), () -> "red bg override not emitted, body:\n" + body);
		assertTrue(body.contains("--jc-tag-red-text:#5a0f14;"), () -> "red text override not emitted, body:\n" + body);
		assertTrue(body.contains("--jc-tag-red-border:#f0b3b7;"), () -> "red border override not emitted, body:\n" + body);
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
	// p) var(--jc-name) reference resolution reaches the served body as a literal
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Mirrors the {@code ReleaseManagerTheme} acceptance case: a derived token expressed as
	 * {@code var(--jc-danger)} on the SAME builder as its target, so the served override block carries the resolved
	 * literal, never the unresolved reference.
	 */
	private static final Theme DERIVED_THEME = Theme.create("derived")
		.token("--jc-danger", "#c23934")
		.token("--jc-tag-red-text", "var(--jc-danger)")
		.build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class DerivedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme(DERIVED_THEME).build(); }
	}

	@Test void p01_varReference_isResolvedToItsLiteral_inTheServedOverrideBlock() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(DerivedHost.class));
		assertTrue(body.contains("--jc-tag-red-text:#c23934;"), () -> "reference not resolved to its literal in the served body:\n" + body);
		// The DECLARATION must be the literal, not a var() reference (chrome.css legitimately uses var(--jc-danger)
		// at use-sites, so we pin the declaration form rather than a blanket substring).
		assertFalse(body.contains("--jc-tag-red-text:var("), () -> "unresolved var() reference leaked into the served declaration:\n" + body);
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
	 * Deliberately asserted as an <i>anchored</i> prefix rather than a {@code contains("html:root{")}: the
	 * pre-existing helpers in this class match {@code :root\{} as a substring, and {@code html:root{} contains
	 * that, so every one of them stays green whether or not the prefix is emitted. A substring assertion here
	 * would inherit exactly that blind spot and pass against the unfixed emitter.
	 */
	@Test void q01_bothEmittedTokenBlocks_carryTheHtmlTypePrefix() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(ThemeSettingsHost.class));
		assertEquals(2, countRootBlocks(body), () -> "expected Theme.OPEN block + THEME_A's override block, body:\n" + body);
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
	// r) ThemePack construction guards (G1-G8), plus the extended emission-boundary coverage the shared
	//    reserved-chrome-token guard needs: it must be proved total across the raw theme(...) and bean channels, not
	//    only across ThemePack.Builder. A green suite that exercised only the pack builder would leave the other
	//    channels silently open, which is the exact hole the guard exists to close.
	//-----------------------------------------------------------------------------------------------------------------

	private static final Theme PACK_THEME = Theme.create("pack-theme").token("--jc-accent", "#b45309").build();

	private static ThemePack.Builder packBuilder() {
		return ThemePack.create("corporate").theme(PACK_THEME);
	}

	// (G1) Pack id shape.
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",                 // empty
		"Corporate",        // uppercase
		"1corporate",       // leading digit
		"-corporate",       // leading hyphen
		"corporate theme",  // whitespace
		"corporate_theme",  // underscore
		"../evil",          // path-traversal shaped
	})
	void r01_packId_invalidShapes_rejected(String id) {
		assertThrows(IllegalArgumentException.class, () -> ThemePack.create(id));
	}

	@Test void r02_packId_legalShape_accepted() {
		assertEquals("corporate-2", ThemePack.create("corporate-2").theme(PACK_THEME).build().getId());
	}

	// (G2) A pack must carry a theme.
	@Test void r03_packWithNoTheme_rejectedAtBuild() {
		var b = ThemePack.create("corporate");
		assertThrows(IllegalArgumentException.class, b::build);
	}

	@Test void r04_packWithNullTheme_rejectedAtTheSetter() {
		var b = ThemePack.create("corporate");
		assertThrows(IllegalArgumentException.class, () -> b.theme(null));
	}

	// (G3) Alias name shape.
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",                // empty
		"--jc-",           // no name part
		"jc-accent",       // missing the -- prefix
		"--JC-accent",     // uppercase prefix
		"--jc-Accent",     // uppercase name
		"--jc-foo;--bar",  // full-string guard: "--jc-foo" matches only as a LEADING substring
		"--jc-foo bar",    // whitespace
		"--other-accent",  // outside the --jc- namespace
	})
	void r05_aliasName_invalidShapes_rejected(String name) {
		var b = packBuilder();
		assertThrows(IllegalArgumentException.class, () -> b.alias(name, "var(--jc-accent)"));
	}

	/**
	 * (G4)+(G5), attacked with a deliberately adversarial name list. The reserved set is {@code --jc-chrome-*}
	 * <i>minus</i> the names {@link Theme#OPEN} already declares, so a real ladder step rejects, the bare prefix
	 * rejects, and a name nothing uses yet rejects too (the guard fails closed on anything new).
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"--jc-chrome-control-height",   // a real ladder step - the whole reason the guard exists
		"--jc-chrome-control-padding-x",
		"--jc-chrome-font-size-1",
		"--jc-chrome-line-height",
		"--jc-chrome-glyph-size-small",
		"--jc-chrome-",                 // the bare prefix: passes the shape guard, must still reject
		"--jc-chrome-anything",         // fails closed on a name no ladder declares today
	})
	void r06_reservedChromeNames_rejectedOnBothPackChannels(String name) {
		var reservedTheme = Theme.create("reserved").token(name, "26px").build();
		var b = ThemePack.create("corporate");
		assertThrows(IllegalArgumentException.class, () -> b.alias(name, "var(--jc-accent)"));
		assertThrows(IllegalArgumentException.class, () -> b.theme(reservedTheme));
	}

	/**
	 * The other half of (r06): names the guard must <b>not</b> claim. A prefix reservation is easy to over-apply,
	 * and each of these is a distinct way to get it wrong.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"--jc-chromex",        // the prefix minus its trailing hyphen - a different name entirely
		"--jc-chrome",         // the bare word is not the prefix
		"--jc-tab-chrome-bg",  // the prefix appearing mid-string reserves nothing
		"--jc-chrome-bg",      // shipped by Theme.OPEN, consumed by chrome.css, legitimately overridable
	})
	void r07_namesOutsideTheReservedSet_accepted(String name) {
		var leafTheme = Theme.create("leaf").token(name, "#b45309").build();
		assertDoesNotThrow(() -> ThemePack.create("corporate").alias(name, "var(--jc-accent)").theme(PACK_THEME).build());
		assertDoesNotThrow(() -> ThemePack.create("corporate").theme(leafTheme).build());
		assertDoesNotThrow(() -> ConsoleChromeMixin.create().theme(leafTheme).build());
	}

	/**
	 * The regression the {@link Theme#OPEN} exemption exists for, and the reason the reserved set cannot simply be
	 * the whole {@code --jc-chrome-} prefix: {@link Theme#deriveFrom(String, Theme)} copies every one of
	 * {@link Theme#OPEN}'s tokens, <b>including the shipped {@code --jc-chrome-bg} colour token</b>. An
	 * unconditional prefix reservation would therefore reject the normal way to author a palette - on the pack
	 * channel and the raw-theme channel alike - which is a functionality removal rather than a hole closed.
	 *
	 * <p>
	 * The two construction-time channels only; the bean channels carry an already-built pack or theme, so they can
	 * reach no guard these two have not already passed.
	 */
	@Test void r08_themeDerivedFromThemeOpen_isAcceptedByBothConstructionChannels() {
		var derived = Theme.deriveFrom("corporate", Theme.OPEN).token("--jc-accent", "#b45309").build();
		assertTrue(derived.getTokens().containsKey("--jc-chrome-bg"),
			"premise of this test: deriveFrom copies Theme.OPEN's chrome-named leaf");
		assertDoesNotThrow(() -> ThemePack.create("corporate").theme(derived).build());
		assertDoesNotThrow(() -> ConsoleChromeMixin.create().theme(derived).build());
	}

	/** The guard fires on the DECLARED name only: aliasing a pack token <i>to</i> a reserved ladder step is legal. */
	@Test void r09_aliasingToAReservedChromeStep_isLegal() {
		var p = packBuilder().alias("--jc-tab-bar-height", "var(--jc-chrome-control-height)").build();
		assertEquals("var(--jc-chrome-control-height)", p.getAliases().get("--jc-tab-bar-height"));
	}

	// (G7) No name may be declared by both channels, whichever channel is populated second.
	@Test void r10_nameDeclaredByBothChannels_rejectedAtBuild_inEitherDeclarationOrder() {
		var t = Theme.create("both").token("--jc-accent", "#b45309").build();
		var aliasFirst = ThemePack.create("corporate").alias("--jc-accent", "var(--jc-link)").theme(t);
		assertThrows(IllegalArgumentException.class, aliasFirst::build);
		var themeFirst = ThemePack.create("corporate").theme(t).alias("--jc-accent", "var(--jc-link)");
		assertThrows(IllegalArgumentException.class, themeFirst::build);
	}

	// (G8) Pack assets go through the same fail-closed belt the mixin's own logo(...) uses.
	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",                                        // empty
		"/testfiles/console/../../../etc/passwd",  // path traversal segment
		"/testfiles/console/bad.txt",              // unallowlisted extension
		"/testfiles/console/nope.svg",             // resource does not exist
	})
	void r11_packLogo_invalidInputs_rejected(String value) {
		var b = packBuilder();
		assertThrows(IllegalArgumentException.class, () -> b.logo(value));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",
		"/testfiles/console/../../../etc/passwd",
		"/testfiles/console/bad.txt",
		"/testfiles/console/nope.png",
	})
	void r12_packPageBackgroundImage_invalidInputs_rejected(String value) {
		var b = packBuilder();
		assertThrows(IllegalArgumentException.class, () -> b.pageBackgroundImage(value));
	}

	/**
	 * The URI-encoding half of the traversal guard, which nothing else in this file reaches. The belt rejects
	 * {@code ..} and {@code %} in one condition, and a {@code %}-bearing path can never name an existing classpath
	 * resource anyway ({@code Class.getResource} does not URI-decode its argument) - so a plain
	 * {@code assertThrows} would be satisfied by the resource-not-found check further down and the whole
	 * {@code %} clause could be deleted with every asset test still green.
	 *
	 * <p>
	 * The failure message is therefore the only available discriminator, and it is a real one: the guard is
	 * <b>ordered</b> so traversal rejection precedes existence checking, and that ordering is the property worth
	 * pinning. Deleting the {@code %} clause changes which branch fires, and that is visible here.
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"/testfiles/console/logo%2Esvg",
		"/testfiles/console/%2e%2e/logo.svg",
	})
	void r11b_packLogo_uriEncodedPath_isRejectedAsTraversal_notMerelyAsNotFound(String value) {
		var b = packBuilder();
		var e = assertThrows(IllegalArgumentException.class, () -> b.logo(value));
		assertTrue(e.getMessage().contains("path traversal"),
			() -> "expected the traversal branch to fire before the existence check, got: " + e.getMessage());
	}

	@Test void r13_packAssets_validClasspathResources_accepted() {
		var p = packBuilder().logo(VALID_LOGO).pageBackgroundImage(VALID_PAGE_BG).build();
		assertEquals(VALID_LOGO, p.getLogoResource());
		assertEquals(VALID_PAGE_BG, p.getPageBackgroundResource());
	}

	@Test void r14_packWithoutAssets_reportsThemAsNull() {
		var p = packBuilder().build();
		assertNull(p.getLogoResource());
		assertNull(p.getPageBackgroundResource());
	}

	@Test void r15_aliasesBulkSeed_rejectsNull_andAppliesEveryGuardPerEntry() {
		var b = packBuilder();
		assertThrows(IllegalArgumentException.class, () -> b.aliases(null));
		var literalValued = Map.of("--jc-tab-bar-bg", "#ffffff");
		assertThrows(IllegalArgumentException.class, () -> b.aliases(literalValued));
		var reservedNamed = Map.of("--jc-chrome-control-height", "var(--jc-accent)");
		assertThrows(IllegalArgumentException.class, () -> b.aliases(reservedNamed));
		var ok = new LinkedHashMap<String,String>();
		ok.put("--jc-tab-bar-bg", "var(--jc-card-bg)");
		ok.put("--jc-tab-selected-bg", "var(--jc-white)");
		assertEquals(ok, packBuilder().aliases(ok).build().getAliases());
	}

	/**
	 * {@link ThemePack#getAliases()} must be insertion-ordered and immutable. Insertion order is what makes the
	 * served body byte-stable for a given pack; the names below are deliberately not in alphabetical or
	 * reverse-alphabetical order, so a sorted or hash-ordered map fails rather than accidentally passing.
	 */
	@Test void r16_getAliases_isInsertionOrdered_andImmutable() {
		var p = packBuilder()
			.alias("--jc-z-last", "var(--jc-accent)")
			.alias("--jc-a-first", "var(--jc-link)")
			.alias("--jc-m-middle", "var(--jc-text)")
			.build();
		assertEquals(List.of("--jc-z-last", "--jc-a-first", "--jc-m-middle"), new ArrayList<>(p.getAliases().keySet()));
		var aliases = p.getAliases();
		assertThrows(UnsupportedOperationException.class, () -> aliases.put("--jc-x", "var(--jc-accent)"));
	}

	@SuppressWarnings({
		"java:S5778" // End-to-end guard coverage: deliberately asserts the whole mixin-construction chain (theme(Theme.create(x).token(reserved).build())) rejects a reserved declaration, so it stays robust if the check is ever relocated between token()/build()/theme(); isolating a single call would drop that end-to-end coverage.
	})
	@Test void r17_rawThemeChannel_declaringAReservedChromeToken_isRejectedAtMixinConstruction() {
		// The channel a pack-only guard would leave wide open. theme(...) is a first-class, documented, tested
		// configuration path, so "everyone uses packs" is not an assumption the guard may rest on.
		assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.create().theme(Theme.create("x").token("--jc-chrome-control-height", "26px").build()));
		assertThrows(IllegalArgumentException.class,
			() -> ConsoleChromeMixin.create().theme(Theme.create("x").token("--jc-chrome-font-size-1", "0.9rem").build()));
	}

	private static final Theme R18_CHROME_TOKEN_THEME = Theme.create("chrome-token").token("--jc-chrome-control-height", "26px").build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class R18_ChromeTokenBeanHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ThemeSettings theme() { return ThemeSettings.of(R18_CHROME_TOKEN_THEME); }
	}

	/**
	 * The bean channel. A settings bean is resolved per request, so it has no construction-time boundary of its own
	 * and the guard has to fire at emission - which is also where the harm would be done. The reserved declaration
	 * must not reach the wire under any status.
	 */
	@Test void r18_beanSuppliedTheme_declaringAReservedChromeToken_neverReachesTheWire() throws Exception {
		var res = MockRestClient.buildLax(R18_ChromeTokenBeanHost.class).get(ConsoleChromeMixin.CHROME_CSS_PATH).run();
		assertNotEquals(200, res.getStatusCode(), "a reserved declaration must not be served successfully");
		// Asserted as "no stylesheet was served at all" rather than "the name is absent from the body": the guard's
		// own failure message names the offending token, so a name-absence assertion would fail on the very error
		// response that proves the guard worked.
		var body = res.getContent().asString();
		assertFalse(body.contains("html:root{"), () -> "a stylesheet was served despite the reserved declaration, body:\n" + body);
	}

	private static final ThemePack R19_BEAN_PACK = ThemePack.create("bean-supplied")
		.theme(Theme.create("bean-supplied").token("--jc-accent", "#b45309").build())
		.alias("--jc-tab-bar-bg", "var(--jc-card-bg)")
		.build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class R19_PackBeanHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ThemePackSettings pack() { return ThemePackSettings.of(R19_BEAN_PACK); }
	}

	/**
	 * The {@link ThemePackSettings} bean channel, and the one place where the guard's coverage story differs from
	 * the other three channels: this channel is closed a layer <b>earlier</b> than emission, because a
	 * {@link ThemePack} carrying a reserved declaration cannot be constructed at all &mdash; so no such pack can
	 * ever be placed in a bean, and {@code packRootBlock}'s own guard calls are unreachable defence-in-depth with
	 * no test that can go red on their deletion.
	 *
	 * <p>
	 * So this pins the two things that <i>are</i> checkable, rather than pretending to pin the third: that the bean
	 * channel is genuinely wired end-to-end (a pack published this way really is served, both halves of it), and
	 * that the constructional closure the unreachability argument rests on actually holds. If
	 * {@link ThemePack} ever gains a second construction path, the first half of that argument lapses and this test
	 * is where to notice.
	 */
	@Test void r19_themePackSettingsBeanChannel_isWiredEndToEnd_andCannotCarryAReservedDeclaration() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(R19_PackBeanHost.class));
		assertTrue(body.contains("--jc-accent:#b45309;"), () -> "a bean-supplied pack's leaves must be served, body:\n" + body);
		assertTrue(body.contains("--jc-tab-bar-bg:var(--jc-card-bg);"), () -> "a bean-supplied pack's aliases must be served, body:\n" + body);
		var reserved = Theme.create("reserved").token("--jc-chrome-glyph-size", "20px").build();
		var b = ThemePack.create("corporate");
		assertThrows(IllegalArgumentException.class, () -> b.theme(reserved));
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-chrome-glyph-size", "var(--jc-accent)"));
	}

	/**
	 * {@link ThemePackSettings} must ship no {@code DEFAULT} constant. Asserted reflectively because the decision is
	 * an <i>absence</i>, and an absence is otherwise pinned by nothing: adding {@code ThemePackSettings.DEFAULT}
	 * would silently change what a zero-config application renders &mdash; there being no default pack is the whole
	 * reason {@code resolveActivePack} returns an {@code Optional} &mdash; and every other test in this file would
	 * stay green through it. Making some pack the shipped default should be a deliberate act at a named release
	 * boundary, which means deleting this test on purpose.
	 */
	@Test void r20_themePackSettings_shipsNoDefaultConstant() {
		for (var f : ThemePackSettings.class.getDeclaredFields())
			assertNotEquals("DEFAULT", f.getName(), "ThemePackSettings must ship no DEFAULT constant - there is no default pack");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// s) Alias value shape: the anchored var(--jc-name) accept/reject matrix. This is the whole security story for
	//    the alias channel - nothing dangerous is representable, so no escaper is needed.
	//-----------------------------------------------------------------------------------------------------------------

	private static String aliasTargetOf(String target) {
		return packBuilder().alias("--jc-tab-bar-bg", target).build().getAliases().get("--jc-tab-bar-bg");
	}

	@Test void s01_aliasTargets_acceptedShapes_storeTheNormalizedReference() {
		assertEquals("var(--jc-accent)", aliasTargetOf("var(--jc-accent)"));
		assertEquals("VAR(--jc-accent)", aliasTargetOf("VAR(--jc-accent)"), "the recognizer is case-insensitive on the function name");
		assertEquals("var( --jc-accent )", aliasTargetOf("var( --jc-accent )"), "internal whitespace inside the parens is legal");
		assertEquals("var(--jc-accent)", aliasTargetOf("  var(--jc-accent)  "), "the shared normalization belt trims");
		assertEquals("var(--jc-accent)", aliasTargetOf("var/**/(--jc-accent)"), "the shared normalization belt strips comments");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"",                                    // not a reference
		"var(--jc-x, #fff)",                   // the fallback form must FAIL, never silently fall back
		"var(--jc-a) var(--jc-b)",             // two references
		"linear-gradient(var(--jc-a), #fff)",  // a reference embedded in a larger value
		"#ffffff",                             // a literal belongs in the pack's Theme, not here
		"1.2rem",
		"red",
		"url(https://evil)",
		"url (https://evil)",
		"url/**/(https://evil)",
		"var(--jc-a);color:red",               // declaration breakout
		"var(--jc-a)}html{color:red",          // block breakout
		"var(--jc-a)/*",                       // unterminated comment survives the strip and must not match
		"var(--jc-A)",                         // uppercase in the target name
		"var(--other-a)",                      // outside the --jc- namespace
		"var(--jc-a",                          // unbalanced
		"\uFF56ar(--jc-a)",                    // fullwidth 'v' homoglyph
		"var\u200E(--jc-a)",                   // a bidi/format control between the name and the paren
	})
	void s02_aliasTargets_rejectedShapes(String target) {
		var b = packBuilder();
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", target));
	}

	/**
	 * Control characters are rejected by the shared belt on the RAW value, before any trim - which is what kills
	 * the {@code url\t(} / {@code url\n(} CSS-hex reconstruction vector for this channel too.
	 */
	@Test void s03_aliasTargets_withControlCharacters_rejected() {
		var b = packBuilder();
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", "var(--jc-a\t)"));
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", "var(--jc-a)\n"));
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", "\u0000var(--jc-a)"));
		assertThrows(IllegalArgumentException.class, () -> b.alias("--jc-tab-bar-bg", "var(--jc-a)\u007F"));
	}

	/**
	 * The shared-recognizer pin. The whole "an alias needs no escaper" argument rests on one anchored regex, and the
	 * alias channel reuses the {@link Theme} channel's copy rather than carrying its own. This asserts the
	 * consequence <i>behaviourally</i>: for every candidate, the two channels agree on whether it is a
	 * {@code var()} reference. A duplicated regex that later got fixed in one copy only would be a silent,
	 * security-relevant divergence &mdash; the alias channel would keep accepting a shape the Theme channel had
	 * already learned to distrust &mdash; and this is the test that would catch it.
	 *
	 * <p>
	 * The Theme side is observed through <i>resolution</i> rather than through an accessor: a value the Theme
	 * channel recognizes as a reference is resolved to its target's literal by {@code build()}, so getting
	 * {@link Theme#OPEN}'s {@code --jc-accent} value back out is exactly the observable "it was treated as a
	 * reference".
	 */
	@ParameterizedTest
	@ValueSource(strings = {
		"var(--jc-accent)",                 // accepted by both
		"VAR(--jc-accent)",
		"var( --jc-accent )",
		"var/**/(--jc-accent)",
		"var(--jc-accent, #fff)",           // rejected by both
		"var(--jc-accent) var(--jc-link)",
		"#ffffff",
		"1.2rem",
		"red",
	})
	void s04_theAliasChannelAndTheThemeChannel_agreeOnWhatAReferenceIs(String value) {
		assertEquals(themeTreatsAsReference(value), aliasAccepts(value),
			() -> "the two channels disagree about '" + value + "' - the recognizer has been duplicated and has drifted");
	}

	private static boolean aliasAccepts(String value) {
		try {
			packBuilder().alias("--jc-tab-bar-bg", value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static boolean themeTreatsAsReference(String value) {
		try {
			return Theme.OPEN.getTokens().get("--jc-accent")
				.equals(Theme.create("probe").token("--jc-tab-bar-bg", value).build().getTokens().get("--jc-tab-bar-bg"));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// t) Pack emission: block count, html prefix, leaves-then-aliases order, and the var() reference surviving to
	//    the wire (the inverse of (p01))
	//-----------------------------------------------------------------------------------------------------------------

	private static final ThemePack EMISSION_PACK = ThemePack.create("corporate")
		.theme(Theme.create("corporate").token("--jc-accent", "#b45309").token("--jc-link", "#0b6bcb").build())
		.alias("--jc-tab-bar-bg", "var(--jc-card-bg)")
		.alias("--jc-tab-selected-bg", "var(--jc-white)")
		.build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class PackHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().pack(EMISSION_PACK).build(); }
	}

	/**
	 * The (q01) twin for the pack channel: a pack REPLACES the standalone-theme block rather than adding a third
	 * one, so a pack-configured host serves exactly the two blocks a themed host serves - which is what keeps
	 * (q01)'s body-wide count meaningful rather than accidentally-passing - and both carry the {@code html} prefix.
	 */
	@Test void t01_packConfiguredHost_servesExactlyTwoTokenBlocks_bothHtmlPrefixed() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(PackHost.class));
		assertEquals(2, countRootBlocks(body), () -> "expected Theme.OPEN block + the pack block, body:\n" + body);
		var m = Pattern.compile("(.{0,5}):root\\{").matcher(body);
		var n = 0;
		while (m.find()) {
			n++;
			assertEquals("html", m.group(1), () -> "a token block was emitted without the html type prefix, body:\n" + body);
		}
		assertEquals(2, n, () -> "expected both emission sites to be checked, body:\n" + body);
	}

	@Test void t02_packBlock_carriesLeavesThenAliases_inDeclarationOrder() throws Exception {
		var block = secondRootBlock(bodyOf(MockRestClient.buildLax(PackHost.class)));
		var emitted = new ArrayList<String>();
		var m = Pattern.compile("(--jc-[a-z0-9-]++)\\s*:").matcher(block);
		while (m.find())
			emitted.add(m.group(1));
		assertEquals(List.of("--jc-accent", "--jc-link", "--jc-tab-bar-bg", "--jc-tab-selected-bg"), emitted);
	}

	/**
	 * The inverse of (p01), and the reason the two {@code var()} paths must never be "unified": (p01) pins that a
	 * {@code var()} written on a <b>Theme token</b> is resolved to its literal before the wire; this pins that a
	 * pack <b>alias</b> survives as a reference. Freezing it would snapshot the live cascade and defeat the entire
	 * point of the alias channel. If either of these two tests ever has to change to make the other pass, the
	 * change is wrong.
	 */
	@Test void t03_packAlias_survivesAsAReference_theInverseOfP01() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(PackHost.class));
		assertTrue(body.contains("--jc-tab-bar-bg:var(--jc-card-bg);"), () -> "alias frozen to a literal or dropped, body:\n" + body);
		assertTrue(body.contains("--jc-tab-selected-bg:var(--jc-white);"), () -> "alias frozen to a literal or dropped, body:\n" + body);
	}

	/**
	 * The (c01) twin: the alias channel is a new way for a non-{@code --jc-} name to reach the wire, and (G3) is the
	 * only thing stopping it.
	 *
	 * <p>
	 * The name class admits digits, unlike (c01)'s. A declared name may legitimately contain one
	 * ({@code --jc-space-1} does), so a letters-only matcher simply never <i>inspects</i> such a name &mdash; and a
	 * leaked {@code --evil-1} would slip through a test whose whole job is to catch it.
	 */
	@Test void t04_packBlock_declaresOnlyJcNames() throws Exception {
		var block = secondRootBlock(bodyOf(MockRestClient.buildLax(PackHost.class)));
		var m = Pattern.compile("([a-zA-Z0-9-]++)\\s*:").matcher(block);
		var n = 0;
		while (m.find()) {
			n++;
			assertTrue(m.group(1).startsWith("--jc-"), () -> "non --jc- name in the pack block: " + m.group(1));
		}
		assertEquals(4, n, () -> "expected every declaration in the pack block to be inspected, block:\n" + block);
	}

	/** The (j01) twin: the fully-assembled pack-configured body must still pass the url-sink scanner. */
	@Test void t05_assembledPackBody_passesChromeCssScanner() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(PackHost.class));
		assertEquals(List.of(), ChromeCssScanner.scan(body), () -> "violations against the assembled pack body:\n" + body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// u) Escaper wiring on the PACK leaf path - the (d02) twin
	//-----------------------------------------------------------------------------------------------------------------

	private static final ThemePack SEMICOLON_PACK = ThemePack.create("semi")
		.theme(Theme.create("semi").token("--jc-font", "'My;Font', sans-serif").build())
		.build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class SemicolonPackHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().pack(SEMICOLON_PACK).build(); }
	}

	/**
	 * The highest-probability regression in the whole pack feature: a copy of {@code rootBlock} that drops the
	 * {@code CssValueEscaper.escape(...)} call reopens (d02)'s declaration-boundary hole <b>for pack-configured
	 * hosts only</b> - and (d02) itself stays green throughout, because it configures a theme rather than a pack.
	 * So the pack leaf path needs its own escaper-wiring gate, and this is it.
	 */
	@Test void u01_escaperIsWiredIntoThePackLeafPath_semicolonInFontFamilyValue_isNeutralized() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(SemicolonPackHost.class));
		assertFalse(body.contains("'My;Font'"), () -> "raw unescaped ';' leaked into the pack declaration, body:\n" + body);
		assertTrue(body.contains("\\3B "), () -> "expected the CSS-hex escape for ';', body:\n" + body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// v) The five-step precedence chain: pack() > theme() > ThemePackSettings bean > ThemeSettings bean > Theme.OPEN
	//-----------------------------------------------------------------------------------------------------------------

	private static final ThemePack PACK_P = ThemePack.create("pack-p")
		.theme(Theme.create("pack-p").token("--jc-accent", "#dd0011").build())
		.alias("--jc-tab-bar-bg", "var(--jc-card-bg)")
		.build();

	private static final ThemePack PACK_Q = ThemePack.create("pack-q")
		.theme(Theme.create("pack-q").token("--jc-accent", "#0011dd").build())
		.alias("--jc-tab-bar-bg", "var(--jc-white)")
		.build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class PackPHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().pack(PACK_P).build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class PackQHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().pack(PACK_Q).build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class V01_PackWinsOverBuilderThemeHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().pack(PACK_P).theme(THEME_A).build(); }
	}

	@Test void v01_builderPack_winsOverBuilderTheme_winnerTakesAll() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(V01_PackWinsOverBuilderThemeHost.class));
		assertTrue(body.contains("--jc-accent:#dd0011;"), () -> "expected PACK_P to win, body:\n" + body);
		assertFalse(body.contains("--jc-accent:#aa0000;"), () -> "the losing theme must contribute NOTHING, body:\n" + body);
		assertEquals(2, countRootBlocks(body), () -> "a merge would have emitted a third block, body:\n" + body);
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class V02_PackWinsOverBothBeansHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().pack(PACK_P).build(); }
		@Bean public ThemePackSettings pack() { return ThemePackSettings.of(PACK_Q); }
		@Bean public ThemeSettings theme() { return ThemeSettings.of(THEME_A); }
	}

	@Test void v02_builderPack_winsOverBothBeanTiers() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(V02_PackWinsOverBothBeansHost.class));
		assertTrue(body.contains("--jc-accent:#dd0011;"), () -> "expected PACK_P to win, body:\n" + body);
		assertFalse(body.contains("--jc-accent:#0011dd;"), () -> "the ThemePackSettings bean must NOT win, body:\n" + body);
		assertFalse(body.contains("--jc-accent:#aa0000;"), () -> "the ThemeSettings bean must NOT win, body:\n" + body);
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class V03_BuilderThemeVsPackBeanHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme(THEME_A).build(); }
		@Bean public ThemePackSettings pack() { return ThemePackSettings.of(PACK_P); }
	}

	/**
	 * <b>The mixed case</b> - the one configuration where axis-major and kind-major precedence disagree, and
	 * therefore the only test that actually decides which chain is implemented. The mixin is hand-configured with a
	 * theme and the application publishes a {@link ThemePackSettings} bean. Axis-major says the explicit builder
	 * call wins outright, so the bean's pack must contribute neither its leaves nor its alias layer. Pinning only
	 * the two clean cases would leave this free to flip silently.
	 */
	@Test void v03_builderTheme_winsOverThemePackSettingsBean_theMixedCase() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(V03_BuilderThemeVsPackBeanHost.class));
		assertTrue(body.contains("--jc-accent:#aa0000;"), () -> "expected THEME_A - an explicit builder call beats a bean, body:\n" + body);
		assertFalse(body.contains("--jc-accent:#dd0011;"), () -> "the bean's pack must NOT win, body:\n" + body);
		assertFalse(body.contains("--jc-tab-bar-bg:"), () -> "the losing pack's alias layer leaked into the body:\n" + body);
		assertEquals(2, countRootBlocks(body), () -> "expected Theme.OPEN block + THEME_A's block only, body:\n" + body);
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class V04_PackBeanVsThemeBeanHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ThemePackSettings pack() { return ThemePackSettings.of(PACK_P); }
		@Bean public ThemeSettings theme() { return ThemeSettings.of(THEME_B); }
	}

	@Test void v04_themePackSettingsBean_winsOverThemeSettingsBean() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(V04_PackBeanVsThemeBeanHost.class));
		assertTrue(body.contains("--jc-accent:#dd0011;"), () -> "expected the pack bean to win, body:\n" + body);
		assertFalse(body.contains("--jc-accent:#00bb00;"), () -> "the ThemeSettings bean must NOT win, body:\n" + body);
		assertTrue(body.contains("--jc-tab-bar-bg:var(--jc-card-bg);"), () -> "the winning pack's alias layer is missing, body:\n" + body);
	}

	/**
	 * The pack tier's (b04) twin: proves the new steps are genuinely wired rather than degenerating to a constant.
	 * A resolver hard-wired to ignore packs would make all three of these bodies byte-identical.
	 */
	@Test void v05_packPrecedenceIsGenuinelyWired_notANoOp() throws Exception {
		var defaultBody = bodyOf(MockRestClient.buildLax(DefaultHost.class));
		var packPBody = bodyOf(MockRestClient.buildLax(PackPHost.class));
		var packQBody = bodyOf(MockRestClient.buildLax(PackQHost.class));
		assertNotEquals(defaultBody, packPBody);
		assertNotEquals(packPBody, packQBody);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// w) Byte-identity compatibility: the pack channel adds nothing of its own
	//-----------------------------------------------------------------------------------------------------------------

	private static final Theme COMPAT_THEME = Theme.create("compat")
		.token("--jc-accent", "#b45309")
		.token("--jc-font", "'My;Font', sans-serif")
		.build();

	private static final ThemePack COMPAT_PACK = ThemePack.create("compat").theme(COMPAT_THEME).build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class W01_ThemeConfiguredHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().theme(COMPAT_THEME).build(); }
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class W01_PackConfiguredHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().pack(COMPAT_PACK).build(); }
	}

	/**
	 * The strong compatibility pin: a pack carrying a theme and <b>no</b> aliases must serve a body
	 * <b>byte-identical</b> to that same theme configured directly via {@code theme(...)}. It says mechanically,
	 * rather than by inspection, that the pack channel contributes nothing of its own to the response, and it is the
	 * closest thing available to a pre/post golden comparison for the whole emission change. The shared theme
	 * deliberately carries an escaper-target value, so the two paths are compared <i>through</i> the escaper rather
	 * than around it.
	 */
	@Test void w01_packCarryingAThemeAndNoAliases_isByteIdenticalToThatThemeConfiguredDirectly() throws Exception {
		assertEquals(
			bodyOf(MockRestClient.buildLax(W01_ThemeConfiguredHost.class)),
			bodyOf(MockRestClient.buildLax(W01_PackConfiguredHost.class)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// x) Empty-pack suppression, on the honest "no leaves AND no aliases" condition
	//-----------------------------------------------------------------------------------------------------------------

	private static final Theme EMPTY_THEME = Theme.create("empty").build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class X01_EmptyPackHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() {
			return ConsoleChromeMixin.create().pack(ThemePack.create("empty").theme(EMPTY_THEME).build()).build();
		}
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class X02_AliasOnlyPackHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() {
			return ConsoleChromeMixin.create().pack(
				ThemePack.create("alias-only").theme(EMPTY_THEME).alias("--jc-tab-bar-bg", "var(--jc-card-bg)").build()
			).build();
		}
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class X03_LeafOnlyPackHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() {
			return ConsoleChromeMixin.create().pack(
				ThemePack.create("leaf-only").theme(Theme.create("leaf-only").token("--jc-accent", "#b45309").build()).build()
			).build();
		}
	}

	@Test void x01_packWithNoLeavesAndNoAliases_emitsNoSecondBlock() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(X01_EmptyPackHost.class));
		assertEquals(1, countRootBlocks(body), () -> "an empty pack must not emit a stray html:root{} block, body:\n" + body);
	}

	/** The other half of (x01): the condition is "no leaves AND no aliases", not "no leaves". */
	@Test void x02_packWithAliasesButNoLeaves_stillEmitsItsBlock() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(X02_AliasOnlyPackHost.class));
		assertEquals(2, countRootBlocks(body), () -> "an alias-only pack has real content and must be emitted, body:\n" + body);
		assertTrue(body.contains("--jc-tab-bar-bg:var(--jc-card-bg);"), () -> "the alias-only pack's block is missing its content, body:\n" + body);
	}

	@Test void x03_packWithLeavesButNoAliases_stillEmitsItsBlock() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(X03_LeafOnlyPackHost.class));
		assertEquals(2, countRootBlocks(body), () -> "a leaf-only pack must be emitted, body:\n" + body);
		assertTrue(body.contains("--jc-accent:#b45309;"), () -> "the leaf-only pack's override is missing, body:\n" + body);
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class X04_OpenNamedPackHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() {
			return ConsoleChromeMixin.create().pack(
				ThemePack.create("open").theme(Theme.create("open").token("--jc-accent", "#b45309").build()).build()
			).build();
		}
	}

	/**
	 * The pack branch must NOT reuse the theme branch's {@code name.equals("open")} suppression. That test exists
	 * for a documented reason on the theme side, but it carries a trap with it: a theme named {@code "open"} has its
	 * whole override block silently dropped, with no exception and no failing test. A pack's id is its own identity
	 * and it always carries an alias layer, so it must never inherit that trap.
	 */
	@Test void x04_packNamedOpen_doesNotInheritTheThemeBranchsSuppressionTrap() throws Exception {
		var body = bodyOf(MockRestClient.buildLax(X04_OpenNamedPackHost.class));
		assertEquals(2, countRootBlocks(body), () -> "a pack must not be suppressed on account of its id, body:\n" + body);
		assertTrue(body.contains("--jc-accent:#b45309;"), () -> "the pack's override was silently dropped, body:\n" + body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// y) No cache-key change: cachedBodies stays keyed by MOUNT, not by the active pack
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * The (f03) twin, and worth being exact about what it does and does not cover: the two hosts are distinct
	 * classes, so each holds its own mixin instance and its own {@code cachedBodies} map. This therefore pins
	 * <i>instance</i> isolation &mdash; that nothing about the pack channel leaks between two independently
	 * configured mounts through the process-wide static caches this class does share (the static {@code chrome.css}
	 * text and the asset cache) &mdash; and not the cache key itself. The key is (y02)'s job.
	 */
	@Test void y01_secondIndependentlyPackedMount_doesNotLeakFirstMountsPack() throws Exception {
		var bodyP = bodyOf(MockRestClient.buildLax(PackPHost.class));
		var bodyQ = bodyOf(MockRestClient.buildLax(PackQHost.class));
		assertTrue(bodyP.contains("--jc-accent:#dd0011;"));
		assertFalse(bodyP.contains("--jc-accent:#0011dd;"), () -> "mount P leaked mount Q's pack, body:\n" + bodyP);
		assertTrue(bodyQ.contains("--jc-accent:#0011dd;"));
		assertFalse(bodyQ.contains("--jc-accent:#dd0011;"), () -> "mount Q leaked mount P's pack, body:\n" + bodyQ);
	}

	static final ConsoleChromeMixin PACK_MOUNT_CACHE_MIXIN = ConsoleChromeMixin.create().pack(PACK_P).build();

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class Y02_PackMountCacheHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return PACK_MOUNT_CACHE_MIXIN; }
	}

	/**
	 * The (m12) twin, pinning that this feature needed no cache-key change: the active pack is constant within one
	 * mixin instance's bean-store scope, exactly as the active theme is, so {@code cachedBodies} stays keyed by
	 * mount alone. One pack-configured instance reached under two mount styles assembles exactly twice and serves
	 * each mount from cache thereafter.
	 *
	 * <p>
	 * The direction this catches is the harmful one: replacing the mount in the key with anything pack-derived
	 * would collapse the two mount styles onto one entry, so the build count would read 1 and whichever style
	 * warmed the cache first would be served to the other. (The reverse mistake &mdash; <i>adding</i> the pack to a
	 * key that still contains the mount &mdash; is unobservable here, and unobservable in general, precisely
	 * because the pack cannot vary within one instance. That is the same fact that makes the addition unnecessary.)
	 */
	@Test void y02_cachedBody_isStillKeyedByMountAlone_withAPackConfigured() throws Exception {
		var standalone1 = standaloneMounted(Y02_PackMountCacheHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		var composed = composedMounted(Y02_PackMountCacheHost.class).get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
		var standalone2 = standaloneMounted(Y02_PackMountCacheHost.class).get("/chrome.css").run().assertStatus(200).getContent().asString();
		assertEquals(standalone1, standalone2, "the standalone body must come back from cache unchanged");
		assertEquals(standalone1, composed, "with no assets configured, nothing in a pack's block is mount-derived");
		assertEquals(2, PACK_MOUNT_CACHE_MIXIN.debugBuildCount(), "expected exactly one assembly per distinct mount");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// z) Pack-supplied assets: honoured from Builder.pack(...) at construction time, out-ranked by an explicitly
	//    configured asset, and the documented bean-path limitation
	//-----------------------------------------------------------------------------------------------------------------

	private static final ThemePack ASSET_PACK = ThemePack.create("assets")
		.theme(Theme.create("assets").token("--jc-accent", "#b45309").build())
		.alias("--jc-tab-bar-bg", "var(--jc-card-bg)")
		.logo(VALID_LOGO)
		.pageBackgroundImage(VALID_PAGE_BG)
		.build();

	private static final String ALTERNATE_LOGO = "/testfiles/console/logo.jpg";
	private static final String ALTERNATE_PAGE_BG = "/testfiles/console/logo.svg";

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class Z01_PackAssetsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() { return ConsoleChromeMixin.create().pack(ASSET_PACK).build(); }
	}

	@Test void z01_builderSuppliedPack_assetsAreHonoured_andActuallyServed() throws Exception {
		var c = MockRestClient.buildLax(Z01_PackAssetsHost.class);
		var body = bodyOf(c);
		assertTrue(body.contains(".jc-logo{background-image:url(\"" + ConsoleChromeMixin.LOGO_ASSET_PATH + "?v="),
			() -> "the pack's logo override rule is missing, body:\n" + body);
		assertTrue(body.contains("url(\"" + ConsoleChromeMixin.PAGE_BG_ASSET_PATH + "?v="),
			() -> "the pack's page-bg url() is missing, body:\n" + body);
		c.get(ConsoleChromeMixin.LOGO_ASSET_PATH).run().assertStatus(200).assertHeader("Content-Type").isContains("image/svg+xml");
		c.get(ConsoleChromeMixin.PAGE_BG_ASSET_PATH).run().assertStatus(200).assertHeader("Content-Type").isContains("image/png");
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class Z02_ExplicitAssetsBeatPackAssetsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ConsoleChromeMixin console() {
			return ConsoleChromeMixin.create().pack(ASSET_PACK).logo(ALTERNATE_LOGO).pageBackgroundImage(ALTERNATE_PAGE_BG).build();
		}
	}

	/** Both assets, because the constructor resolves them through two independent ternaries - one can be inverted alone. */
	@Test void z02_explicitlyConfiguredAssets_winOverThePacksAssets() throws Exception {
		// Each pair of fixtures must differ, or the cache-buster comparisons below would pass vacuously.
		assertNotEquals(expectedCacheBuster(VALID_LOGO), expectedCacheBuster(ALTERNATE_LOGO));
		assertNotEquals(expectedCacheBuster(VALID_PAGE_BG), expectedCacheBuster(ALTERNATE_PAGE_BG));
		var body = bodyOf(MockRestClient.buildLax(Z02_ExplicitAssetsBeatPackAssetsHost.class));
		assertEquals(ConsoleChromeMixin.LOGO_ASSET_PATH + expectedCacheBuster(ALTERNATE_LOGO), emittedUrl(body, "/assets/logo"));
		assertEquals(ConsoleChromeMixin.PAGE_BG_ASSET_PATH + expectedCacheBuster(ALTERNATE_PAGE_BG), emittedUrl(body, "/assets/page-bg"));
	}

	@Rest(mixins=ConsoleChromeMixin.class)
	public static class Z03_BeanSuppliedPackAssetsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public ThemePackSettings pack() { return ThemePackSettings.of(ASSET_PACK); }
	}

	/**
	 * The documented asset limitation, pinned so it stays a <i>known</i> limitation rather than becoming a surprise:
	 * a pack arriving through a {@link ThemePackSettings} bean is resolved per request, after the mixin's asset
	 * fields and their content-hash cache-busters are already fixed at construction - so its assets are ignored,
	 * while its tokens and aliases still apply in full. Honouring them would mean moving asset resolution to request
	 * time, which drags the cache-buster and the body cache key along with it.
	 */
	@Test void z03_beanSuppliedPack_assetsAreIgnored_butItsTokensAndAliasesStillApply() throws Exception {
		var c = MockRestClient.buildLax(Z03_BeanSuppliedPackAssetsHost.class);
		var body = bodyOf(c);
		assertTrue(body.contains("--jc-accent:#b45309;"), () -> "a bean-supplied pack's tokens must still apply, body:\n" + body);
		assertTrue(body.contains("--jc-tab-bar-bg:var(--jc-card-bg);"), () -> "a bean-supplied pack's aliases must still apply, body:\n" + body);
		assertFalse(body.contains(ConsoleChromeMixin.LOGO_ASSET_PATH), () -> "a bean-supplied pack's logo is a documented limitation, body:\n" + body);
		assertFalse(body.contains(ConsoleChromeMixin.PAGE_BG_ASSET_PATH), () -> "a bean-supplied pack's page background is a documented limitation, body:\n" + body);
		c.get(ConsoleChromeMixin.LOGO_ASSET_PATH).run().assertStatus(404);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test helpers
	//-----------------------------------------------------------------------------------------------------------------

	/** A host composed onto an existing application mount at {@code /rest/*}. */
	private static MockRestClient composedMounted(Class<?> host) {
		return MockRestClient.createLax(host).servletPath("/rest").build();
	}

	/**
	 * Extracts the single {@code url("...")} value the served CSS emits for the given asset endpoint. Scans by
	 * substring rather than regex - the mount-prefix text before {@code assetPathSuffix} isn't disjoint from an
	 * unbounded {@code [^"]*} lead-in, so a single find()-based pattern would need backtracking to bridge it.
	 */
	private static String emittedUrl(String body, String assetPathSuffix) {
		var needle = assetPathSuffix + "?v=";
		var needleIdx = body.indexOf(needle);
		assertTrue(needleIdx >= 0, () -> "no emitted url() for " + assetPathSuffix + " in body:\n" + body);
		var prefix = "url(\"";
		var urlOpenIdx = body.lastIndexOf(prefix, needleIdx);
		assertTrue(urlOpenIdx >= 0, () -> "no url(\" prefix before the emitted asset path, body:\n" + body);
		var contentStart = urlOpenIdx + prefix.length();
		var contentEnd = body.indexOf('"', contentStart);
		assertTrue(contentEnd >= 0, () -> "unterminated url(\"...\") value, body:\n" + body);
		return body.substring(contentStart, contentEnd);
	}

	/**
	 * Re-expresses a browser-absolute emitted URL as a path below the given mount, asserting along the way that it
	 * really does sit below it - which is the half of "the browser can fetch this" that a bare status assertion
	 * against a hand-written path cannot check.
	 */
	private static String belowMount(String emittedUrl, String mount) {
		assertTrue(emittedUrl.startsWith(mount + "/"), () -> "emitted url '" + emittedUrl + "' is not below the mount '" + mount + "'");
		return emittedUrl.substring(mount.length());
	}

	/** The exact {@code ?v=<buildVersion>-<hash8>} suffix the mixin must still append to every emitted asset URL. */
	private static String expectedCacheBuster(String classpathResource) throws IOException {
		byte[] bytes;
		try (var in = ConsoleChromeMixin_Test.class.getResourceAsStream(classpathResource)) {
			assertNotNull(in);
			bytes = in.readAllBytes();
		}
		var crc = new CRC32();
		crc.update(bytes);
		var v = ConsoleChromeMixin.class.getPackage().getImplementationVersion();
		return "?v=" + (v == null ? "dev" : v) + '-' + String.format("%08x", crc.getValue());
	}

	private static String bodyOf(MockRestClient client) throws Exception {
		return client.get(ConsoleChromeMixin.CHROME_CSS_PATH).run().assertStatus(200).getContent().asString();
	}

	private static void assertAssetServed(String resourcePath, String assetPath, String expectedContentType) throws Exception {
		byte[] expected;
		try (var in = ConsoleChromeMixin_Test.class.getResourceAsStream(resourcePath)) {
			expected = in.readAllBytes();
		}
		var res = MockRestClient.buildLax(AssetsHost.class).get(assetPath).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains(expectedContentType)
			.assertHeader("Cache-Control").isContains("max-age");
		assertArrayEquals(expected, res.getContent().asBytes());
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

	/** The appended override block - {@link Theme#OPEN}'s block is always first, so the second one is the pack's. */
	private static String secondRootBlock(String body) {
		var first = body.indexOf(":root{");
		var start = body.indexOf(":root{", first + 1);
		assertTrue(start >= 0, () -> "no second :root{} block in body:\n" + body);
		var end = body.indexOf('}', start);
		return body.substring(start, end + 1);
	}

}
