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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * Golden-HTML tests for the {@code <@console>} document-shell directive and its capture slots
 * ({@code <@head>}/{@code <@scripts>}/{@code <@brand>}/{@code <@actions>}/{@code <@title>}/{@code <@footer>}/
 * {@code <@body>}), the positional {@code <@main/>}, the in-{@code <@console>} {@code <@theme>}/{@code <@token>}
 * ThemePack path, and the CSRF document-shell channel. An app chrome authors only the thin
 * {@code <@console>…</@console>}; Juneau emits the whole {@code <!DOCTYPE html>} document, its head cascade, and its
 * body scaffold.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class ConsoleDirective_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class ConsoleHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome.ftlh")
				.build();
		}
		@RestGet(path="/assets")
		public View assets() {
			return FreemarkerView.of("admin/page-assets.ftlh");
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
		@RestGet(path="/csrf")
		public View csrf(RestRequest req) {
			// Mirror the LoopbackBoundaryFilter's allowed-path publication so <@console> emits the CSRF meta/attrs.
			req.setAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, "tok-123");
			req.setAttribute(LoopbackBoundaryFilter.HEADER_ATTRIBUTE, "X-Csrf-Token");
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class TitleHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome-title.ftlh")
				.build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class SlotsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome-slots.ftlh")
				.build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class StickyHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome-sticky.ftlh")
				.build();
		}
		@RestGet(path="/assets")
		public View assets() {
			return FreemarkerView.of("admin/page-assets.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class BareHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome-bare.ftlh")
				.build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class ThemePackHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome-themepack.ftlh")
				.build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class OrphanHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-slot-orphan.ftlh")
				.build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class FormatHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-format.ftlh")
				.build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class MainBodyHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-main-body.ftlh")
				.build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class BogusThemeHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-bogus-theme.ftlh")
				.build();
		}
		@RestGet(path="/naked")
		public View naked() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	// A live RestRequest so resolveConfiguration(...) has a request to hand the fill-missing walk.
	private static RestRequest dummyRequest() throws Exception {
		try (var c = MockRestClient.buildLax(DummyHost.class);
			var rsp = c.get("/x").run()) {
			return DummyHost.CAPTURED.get();
		}
	}

	public static class DummyHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final ThreadLocal<RestRequest> CAPTURED = new ThreadLocal<>();
		@RestGet(path="/x")
		public String x(RestRequest req) {
			CAPTURED.set(req);
			return "x";
		}
	}

	static int count(String body, String needle) {
		var n = 0;
		for (var i = body.indexOf(needle); i >= 0; i = body.indexOf(needle, i + needle.length()))
			n++;
		return n;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Fill-missing registration
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c00_consoleDirectivesAreRegistered() throws Exception {
		var cfg = ConsoleFreemarkerMixin.create().basePath("/templates/")
			.chromeTemplate("admin/console-chrome.ftlh").build()
			.resolveConfiguration(dummyRequest());
		assertNotNull(cfg.getSharedVariable("console"));
		assertNotNull(cfg.getSharedVariable("main"));
		assertNotNull(cfg.getSharedVariable("token"));
		assertNotNull(cfg.getSharedVariable("head"));
		assertNotNull(cfg.getSharedVariable("scripts"));
		assertNotNull(cfg.getSharedVariable("brand"));
		assertNotNull(cfg.getSharedVariable("actions"));
		assertNotNull(cfg.getSharedVariable("title"));
		assertNotNull(cfg.getSharedVariable("footer"));
		assertNotNull(cfg.getSharedVariable("body"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Golden document shell
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_emitsFullDocumentShell() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(ConsoleHost.class);
			var rsp = c.get("/assets").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}

		// Document skeleton Juneau owns (the app never wrote any of this).
		assertTrue(body.contains("<!DOCTYPE html>"), () -> body);
		assertTrue(body.contains("<meta charset=\"utf-8\">"), () -> body);
		assertTrue(body.contains("name=\"viewport\""), () -> body);

		// Document <title> falls back to brand= when title= is absent.
		assertTrue(body.contains("<title>My App</title>"), () -> body);

		// Head cascade.
		assertTrue(body.contains("name=\"page-tab\"") && body.contains("releases"), () -> body);
		assertTrue(body.contains("chrome.css"), () -> body);
		assertEquals(1, count(body, "juneau-theme-light-red.css"), () -> body);
		assertFalse(body.contains("juneau-theme-open.css"), () -> body);  // theme= wins; no default block
		assertTrue(body.contains("href=\"a.css\"") && body.contains("href=\"b.css\""), () -> body);
		assertTrue(body.contains("extra-head.css"), () -> body);  // <@head> pass-through

		// Header composed from icon= + brand=.
		assertTrue(body.contains("<header class=\"jc-header\">"), () -> body);
		// icon= paints .jc-logo as a background <div> (matches chrome.css), NOT an <img>.
		assertTrue(body.contains("class=\"jc-logo\""), () -> body);
		assertTrue(body.contains("background-image:url('/app/logo.svg')"), () -> body);
		assertFalse(body.contains("<img class=\"jc-logo\""), () -> body);
		assertTrue(body.contains("jc-brand-title") && body.contains("My App"), () -> body);

		// Body: exactly one <main>, the page body inside it, the nav landmark before it.
		assertEquals(1, count(body, "<main class=\"jc-main\">"), () -> body);
		assertTrue(body.contains("<p>assets</p>"), () -> body);
		assertTrue(body.contains("class=\"juneau-page-nav\""), () -> body);
		assertTrue(body.indexOf("juneau-page-nav") < body.indexOf("<main"), () -> body);

		// No .jc-chrome wrapper unless the app opts in.
		assertFalse(body.contains("jc-chrome"), () -> body);

		// Footer + end-of-body scripts.
		assertTrue(body.contains("jc-page-footer") && body.contains("ACME"), () -> body);
		assertTrue(body.contains("src=\"one.js\"") && body.contains("src=\"two.js\""), () -> body);
		assertTrue(body.contains("extra-tail.js"), () -> body);  // <@scripts> pass-through

		// Head closes before the body opens; main precedes footer.
		assertTrue(body.indexOf("</head>") < body.indexOf("<body"), () -> body);
		assertTrue(body.indexOf("<main") < body.indexOf("jc-page-footer"), () -> body);

		// Dual-hat: no Salesforce/SLDS leakage in the emitted shell.
		assertFalse(body.contains("slds-"), () -> body);
	}

	@Test void c02_noCsrf_omitsMetaAndBodyAttrs() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(ConsoleHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertFalse(body.contains("csrf-token"), () -> body);
		assertFalse(body.contains("data-juneau-csrf"), () -> body);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// CSRF document-shell channel
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c03_csrf_emitsMetaAndBodyAttrs() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(ConsoleHost.class);
			var rsp = c.get("/csrf").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("<meta name=\"csrf-token\" content=\"tok-123\">"), () -> body);
		assertTrue(body.contains("data-juneau-csrf=\"tok-123\""), () -> body);
		assertTrue(body.contains("data-juneau-csrf-header=\"X-Csrf-Token\""), () -> body);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Header slots
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c04_title_replacesWholeHeader() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(TitleHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("<header class=\"my-custom-header\">WHOLE HEADER</header>"), () -> body);
		assertFalse(body.contains("jc-header"), () -> body);          // default header suppressed
		assertFalse(body.contains("Ignored When Title Set"), () -> body);  // brand= ignored (title= wins the doc title, <@title> the header)
		assertTrue(body.contains("<title>Doc Title Here</title>"), () -> body);  // title= drives the document <title>
	}

	@Test void c05_brandAndActions_subSlots() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(SlotsHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("<header class=\"jc-header\">"), () -> body);
		assertTrue(body.contains("<div class=\"jc-brand\">"), () -> body);
		assertTrue(body.contains("CUSTOM BRAND REGION"), () -> body);
		assertFalse(body.contains("jc-brand-title"), () -> body);  // <@brand> replaces the default brand region markup
		assertTrue(body.contains("jc-header-actions") && body.contains("Sign out"), () -> body);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Optional .jc-chrome sticky wrapper + body attrs + phased head/scripts + title=
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c11_stickyChrome_wrapsHeaderAndNav_phasesAndBodyAttrs() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(StickyHost.class);
			var rsp = c.get("/assets").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}

		// title= wins the document <title> over brand=.
		assertTrue(body.contains("<title>Doc Title</title>"), () -> body);

		// <@body> attributes land on the emitted <body> tag.
		assertTrue(body.contains("data-runtime-tab=\"open\""), () -> body);

		// .jc-chrome wraps the header + pre-main nav region; the <main> lands after the wrapper closes.
		assertTrue(body.contains("<div class=\"jc-chrome\">"), () -> body);
		assertTrue(body.indexOf("<div class=\"jc-chrome\">") < body.indexOf("jc-header"), () -> body);
		assertTrue(body.indexOf("jc-header") < body.indexOf("juneau-page-nav"), () -> body);  // header, then nav, inside the wrapper
		assertTrue(body.indexOf("juneau-page-nav") < body.indexOf("<main"), () -> body);
		assertTrue(body.contains("</div>\n<main class=\"jc-main\">"), () -> body);  // wrapper closes immediately before <main>

		// <@head phase="before-page-css"> lands app CSS BEFORE the page-local pageCss (a.css).
		assertTrue(body.contains("app-early.css"), () -> body);
		assertTrue(body.indexOf("app-early.css") < body.indexOf("a.css"), () -> body);

		// <@scripts phase="after-toolkit"> lands BEFORE pageInit (one.js).
		assertTrue(body.contains("mid-toolkit.js"), () -> body);
		assertTrue(body.indexOf("mid-toolkit.js") < body.indexOf("one.js"), () -> body);

		// Footer is a body-level child after <main>, outside the .jc-chrome wrapper.
		assertTrue(body.indexOf("<main") < body.indexOf("jc-page-footer"), () -> body);
	}

	@Test void c12_bareConsole_omitsHeaderEntirely() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(BareHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		// No icon=/brand=/<@title>/<@brand>/<@actions> authored: emit no <header> at all (no empty sticky bar).
		assertFalse(body.contains("<header"), () -> body);
		assertFalse(body.contains("jc-header"), () -> body);
		// The rest of the document still renders.
		assertEquals(1, count(body, "<main class=\"jc-main\">"), () -> body);
		assertTrue(body.contains("class=\"juneau-page-nav\""), () -> body);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// In-<@console> <@theme>/<@token> ThemePack path
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c06_themePack_emitsPackLinkThenOverrideStyle() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(ThemePackHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("juneau-theme-open.css"), () -> body);
		assertTrue(body.contains("<style>"), () -> body);
		assertTrue(body.contains("html:root{"), () -> body);
		assertTrue(body.contains("--jc-surface"), () -> body);           // leaf token
		assertTrue(body.contains("--jc-brand-accent:var(--jc-surface)"), () -> body);  // alias survives to the wire verbatim
		// The FTL-constructed override block lands AFTER the stock pack link (head cascade order).
		assertTrue(body.indexOf("juneau-theme-open.css") < body.indexOf("<style>"), () -> body);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Fail-closed guards
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c07_orphanSlot_isRejected() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(OrphanHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(500);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("must be nested inside <@console>"), () -> body);
	}

	@Test void c08_formatAttr_isRejected() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(FormatHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(500);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("no format=") && body.contains("<@console>"), () -> body);
	}

	@Test void c09_mainWithBody_isRejected() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(MainBodyHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(500);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("self-closing"), () -> body);
	}

	@Test void c10_unknownThemeName_isRejected() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(BogusThemeHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(500);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("unknown theme name") && body.contains("chartreuse"), () -> body);
	}
}
