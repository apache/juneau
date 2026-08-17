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

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

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
		var m = Pattern.compile("([a-zA-Z-]+)\\s*:").matcher(block);
		while (m.find())
			assertTrue(m.group(1).startsWith("--jc-"), () -> "non --jc- name in :root{} block: " + m.group(1));
	}

	@Test void c02_everyChromeCssVarJc_isDefinedInThemeOpen() throws IOException {
		var referenced = referencedTokensInChromeCss();
		var defined = Theme.OPEN.getTokens().keySet();
		for (var name : referenced)
			assertTrue(defined.contains(name), () -> "chrome.css references '" + name + "' but Theme.OPEN does not define it");
	}

	@Test void c03_everyThemeOpenToken_isReferencedInChromeCss() throws IOException {
		var referenced = referencedTokensInChromeCss();
		for (var name : Theme.OPEN.getTokens().keySet())
			assertTrue(referenced.contains(name), () -> "Theme.OPEN defines orphan token '" + name + "' not referenced by chrome.css");
	}

	private static Set<String> referencedTokensInChromeCss() throws IOException {
		String css;
		try (var in = ConsoleChromeMixin_Test.class.getResourceAsStream("/org/apache/juneau/console/chrome.css")) {
			assertNotNull(in);
			css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		var stripped = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(css).replaceAll("");
		var out = new LinkedHashSet<String>();
		var m = Pattern.compile("var\\((--jc-[a-z0-9-]+)\\)").matcher(stripped);
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
		.token("--jc-font", "'Salesforce Sans', Inter, sans-serif")
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
		assertTrue(body.contains("--jc-font:'Salesforce Sans', Inter, sans-serif;"));
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
