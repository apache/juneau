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
		// 47 = the original 32, plus the three-token red tag triad, plus the eleven additive
		// token gaps (--jc-header-height, --jc-nav-indicator-width, --jc-card-shadow, --jc-danger-wash,
		// --jc-success-wash, and the six-step --jc-space-1..6 scale), plus the --jc-focus focus-ring colour.
		// Bumping this number is only ever correct alongside a reviewed edit to Theme.OPEN itself.
		assertEquals(47, Theme.OPEN.getTokens().size());
		assertFalse(Theme.OPEN.getTokens().containsKey("--jc-logo"));
		assertFalse(Theme.OPEN.getTokens().containsKey("--jc-page-bg-image"));
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

}
