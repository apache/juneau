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

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
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
 * theme override path, and the CSRF document-shell channel. An app chrome authors only the thin
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
	public static class ThemeOverrideHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome-theme-override.ftlh")
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

	// A chrome that renders no <@console>.
	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class NoConsoleHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/no-console-chrome.ftlh").build();
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

		// Document skeleton and head cascade (server-rendered, D3).
		assertTrue(body.contains("<!DOCTYPE html>"), () -> body);
		assertTrue(body.contains("<meta charset=\"utf-8\">"), () -> body);
		assertTrue(body.contains("name=\"viewport\""), () -> body);
		assertTrue(body.contains("<title>My App</title>"), () -> body);
		assertTrue(body.contains("<meta name=\"page-tab\" content=\"home\">"), () -> body);
		assertTrue(body.contains("href=\"/juneau-console/chrome.css"), () -> body);
		assertTrue(body.contains("href=\"/juneau-console/themes/juneau-theme-light-red.css"), () -> body);
		assertEquals(1, count(body, "juneau-theme-light-red.css"), () -> body);
		assertFalse(body.contains("juneau-theme-open.css"), () -> body);
		assertTrue(body.contains("href=\"a.css\"") && body.contains("href=\"b.css\""), () -> body);
		assertTrue(body.contains("extra-head.css"), () -> body);
		assertTrue(body.indexOf("</head>") < body.indexOf("<body"), () -> body);

		// Header, nav, page body and footer are contract data now (D3), not server HTML.
		assertPage(body).isValid()
			.hasTitle("My App").hasHeaderTitle("My App").hasTheme("light-red")
			.hasNavHref("home", "/home").hasActiveNav("home")
			.hasCard("jc-seg-1", "html").templateContains("jc-seg-1", "<p>assets</p>")
			.hasFooterSlot("content").templateContains("footer.content", "&copy; ACME");
		assertEquals("/app/logo.svg", assertPage(body).contract().getMap("header").getMap("logo").getString("src"));
		assertFalse(body.contains("<header class=\"jc-header\""), () -> body);
		assertFalse(body.contains("<main class=\"jc-main\""), () -> body);

		// Shell, then the end-of-body scripts in order.
		assertTrue(body.contains("/juneau-console/juneau-console.js"), () -> body);
		assertTrue(body.indexOf("juneau-console.js") < body.indexOf("src=\"one.js\""), () -> body);
		assertTrue(body.indexOf("src=\"one.js\"") < body.indexOf("src=\"two.js\""), () -> body);
		assertTrue(body.indexOf("src=\"two.js\"") < body.indexOf("extra-tail.js"), () -> body);
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
		assertPage(body).isValid().hasHeaderSlot("replace")
			.templateContains("header.replace", "<header class=\"my-custom-header\">WHOLE HEADER</header>");
		assertTrue(body.contains("<title>Doc Title Here</title>"), () -> body);
		assertPage(body).hasTitle("Doc Title Here").hasHeaderTitle("Ignored When Title Set");
		// The shell ignores header.title when slots.replace is present; spec §5.2.
	}

	@Test void c05_brandAndActions_subSlots() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(SlotsHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertPage(body).isValid().hasHeaderTitle("Default Brand")
			.hasHeaderSlot("brand").templateContains("header.brand", "CUSTOM BRAND REGION")
			.hasHeaderSlot("actions").templateContains("header.actions", "Sign out");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Optional .jc-chrome sticky wrapper + body attrs + phased head/scripts + title=
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c11_stickyChrome_phasesAndBodyAttrs() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(StickyHost.class);
			var rsp = c.get("/assets").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}

		assertTrue(body.contains("<title>Doc Title</title>"), () -> body);
		assertTrue(body.contains("<body data-runtime-tab=\"open\">"), () -> body);
		assertEquals(Boolean.TRUE, assertPage(body).contract().getMap("header").get("chrome"));
		assertTrue(body.indexOf("app-early.css") < body.indexOf("href=\"a.css\""), () -> body);
		assertTrue(body.indexOf("mid-toolkit.js") < body.indexOf("src=\"one.js\""), () -> body);
		assertPage(body).isValid().hasFooterSlot("content");
	}

	@Test void c13_consoleAssetHrefs_areContextRootAbsoluteUnderANestedPagePath() throws Exception {
		// The live regression: a page resource at /rest/setup reports that path as its servlet path, so
		// servlet:/juneau-console/chrome.css becomes /rest/setup/juneau-console/chrome.css (404). The shell
		// must emit context-root-absolute /juneau-console/... regardless of the page resource's path.
		String body;
		try (var c = MockRestClient.createLax(ConsoleHost.class).servletPath("/rest/setup").build();
			var rsp = c.get("/assets").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("href=\"/juneau-console/chrome.css"), () -> body);
		assertTrue(body.contains("href=\"/juneau-console/themes/juneau-theme-light-red.css"), () -> body);
		assertFalse(body.contains("/rest/setup/juneau-console/"), () -> body);
		assertFalse(body.contains("href=\"juneau-console/chrome.css"), () -> body);  // no leading-slash-less relative
	}

	@Test void c12_bareConsole_omitsHeaderEntirely() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(BareHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		// No icon=/brand=/<@title>/<@brand>/<@actions> authored: no header in the contract at all.
		var a = assertPage(body).isValid();
		assertNull(a.contract().get("header"), () -> body);
		assertFalse(body.contains("<header"), () -> body);
		a.hasNavHref("home", "/home").hasCard("jc-seg-1", "html");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// In-<@console> <@theme>/<@token> override path
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c06_themeOverride_emitsStockLinkThenOverrideStyle() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(ThemeOverrideHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("juneau-theme-open.css"), () -> body);
		assertTrue(body.contains("<style>"), () -> body);
		assertTrue(body.contains("html:root{"), () -> body);
		assertTrue(body.contains("--jc-surface"), () -> body);           // leaf token
		assertTrue(body.contains("--jc-brand-accent:var(--jc-surface)"), () -> body);  // alias survives to the wire verbatim
		// The FTL-constructed override block lands AFTER the stock theme link (head cascade order).
		assertTrue(body.indexOf("juneau-theme-open.css") < body.indexOf("<style>"), () -> body);
		assertPage(body).isValid().hasTheme("open");
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
		assertTrue(body.contains("<@head> must be nested inside <@console>."), () -> body);
	}

	@Test void c08_formatAttr_isRejected() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(FormatHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(500);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("<@console> has no format= attribute."), () -> body);
	}

	@Test void c09_mainWithBody_isRejected() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(MainBodyHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(500);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("<@main/> is self-closing; the page body is the main content and takes no nested body."), () -> body);
	}

	@Test void c10_unknownThemeName_isRejected() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(BogusThemeHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(500);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("<@console> unknown theme name 'chartreuse'.  Built-in themes: "), () -> body);
	}

	@Test void c14_pageWithoutConsoleChrome_isRejected() throws Exception {
		// P16: a chrome template that never renders <@console> is the removed legacy path.
		try (var c = MockRestClient.buildLax(NoConsoleHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("<@page> chrome template 'admin/no-console-chrome.ftlh' rendered no <@console>; "
				+ "the legacy chrome was removed in 10.0.0."), () -> body);
		}
	}
}
