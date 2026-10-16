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

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.MockServletRequest;
import org.apache.juneau.rest.mock.MockServletResponse;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.util.*;
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
		@RestGet(path="/spec-csrf")
		public View specCsrf(RestRequest req) {
			return PageSpec.create().csrf("spec-tok", "X-Spec").bodyAttr("data-spec-keep", "kept").template("admin/page-naked.ftlh").view(req);
		}
		@RestGet(path="/body-csrf-single")
		public View bodyCsrfSingle() {
			return FreemarkerView.of("admin/page-csrf-body-single.ftlh");
		}
		@RestGet(path="/body-csrf-unquoted")
		public View bodyCsrfUnquoted() {
			return FreemarkerView.of("admin/page-csrf-body-unquoted.ftlh");
		}
		@RestGet(path="/body-csrf-upper")
		public View bodyCsrfUpper() {
			return FreemarkerView.of("admin/page-csrf-body-upper.ftlh");
		}
		@RestGet(path="/body-attrs-csrf-lookalike")
		public View bodyAttrsCsrfLookalike() {
			return FreemarkerView.of("admin/page-body-attrs-csrf-lookalike.ftlh");
		}
		@RestGet(path="/body-attrs-plain")
		public View bodyAttrsPlain() {
			return FreemarkerView.of("admin/page-body-attrs-plain.ftlh");
		}
		@RestGet(path="/csrf-masked")
		public View csrfMasked(RestRequest req, RestResponse res) throws Exception {
			// The real filter publishes the attributes; its chain is a no-op because this request is already inside it.
			new RequestBoundaryFilter(MASKING).doFilter(req.getHttpServletRequest(), res.getHttpServletResponse(), (q, s) -> {});
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
		@RestGet(path="/csrf-supplier-only")
		public View csrfSupplierOnly(RestRequest req, RestResponse res) throws Exception {
			// The real filter publishes the supplier; the string attribute is removed so only the supplier remains.
			new RequestBoundaryFilter(MASKING).doFilter(req.getHttpServletRequest(), res.getHttpServletResponse(), (q, s) -> {});
			req.getHttpServletRequest().removeAttribute(RequestBoundaryFilter.TOKEN_ATTRIBUTE);
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
		@RestGet(path="/csrf-supplier-overwritten")
		public View csrfSupplierOverwritten(RestRequest req, RestResponse res) throws Exception {
			// A later filter or the servlet replaces the boundary's supplier with its own.
			new RequestBoundaryFilter(MASKING).doFilter(req.getHttpServletRequest(), res.getHttpServletResponse(), (q, s) -> {});
			Supplier<String> attacker = () -> "ATTACKER-TOKEN";
			req.getHttpServletRequest().setAttribute(RequestBoundaryFilter.TOKEN_SUPPLIER_ATTRIBUTE, attacker);
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
		@RestGet(path="/csrf-loopback")
		public View csrfLoopback(RestRequest req, RestResponse res) throws Exception {
			new LoopbackBoundaryFilter(LOOPBACK).doFilter(req.getHttpServletRequest(), res.getHttpServletResponse(), (q, s) -> {});
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	/** The per-process token behind {@link #MASKING}, so a test can unmask an embed by hand and compare. */
	static final SynchronizerToken PROCESS_TOKEN = SynchronizerToken.generate();

	static final CsrfBoundary MASKING = CsrfBoundary.create().tokens(CsrfTokens.perProcess(PROCESS_TOKEN)).build();

	static final String LOOPBACK_AUTHORITY = "127.0.0.1:8790";

	static final LoopbackBoundary LOOPBACK = LoopbackBoundary.create().authority(LOOPBACK_AUTHORITY).token(PROCESS_TOKEN).build();

	/** Reverses the published mask by hand: the second 32 bytes XOR the first. */
	static String unmask(String masked) {
		var b = HexFormat.of().parseHex(masked);
		var out = new byte[32];
		for (var i = 0; i < 32; i++)
			out[i] = (byte)(b[i] ^ b[32 + i]);
		return HexFormat.of().formatHex(out);
	}

	static String metaToken(String body) {
		return between(body, "<meta name=\"csrf-token\" content=\"", "\"");
	}

	static String bodyToken(String body) {
		return between(body, " data-juneau-csrf=\"", "\"");
	}

	private static String between(String body, String start, String end) {
		var i = body.indexOf(start);
		assertTrue(i >= 0, () -> "missing " + start + " in: " + body);
		var from = i + start.length();
		return body.substring(from, body.indexOf(end, from));
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
		@RestGet(path="/wire-order")
		public View wireOrder() {
			return FreemarkerView.of("admin/page-wire-order.ftlh");
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

		// Document skeleton and head cascade (server-rendered).
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

		// Header, nav, page body and footer are contract data now, not server HTML.
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
	// Token-bearing responses are not cacheable
	// -----------------------------------------------------------------------------------------------------------------

	/** Response that records whether any body byte had reached the stream when {@code Cache-Control} was set. */
	static class RecordingResponse extends MockServletResponse {
		boolean written;
		Boolean writtenWhenCacheControlSet;
		boolean committed;

		@Override public boolean isCommitted() { return committed; }

		@Override public void setHeader(String name, String value) {
			if ("Cache-Control".equalsIgnoreCase(name))
				writtenWhenCacheControlSet = written;
			super.setHeader(name, value);
		}

		@Override public jakarta.servlet.ServletOutputStream getOutputStream() throws IOException {
			return new FinishableServletOutputStream(new FilterOutputStream(super.getOutputStream()) {
				@Override public void write(int b) throws IOException { written = true; out.write(b); }
				@Override public void write(byte[] b, int o, int l) throws IOException { written = true; out.write(b, o, l); }
			});
		}
	}

	private static String dispatch(Class<? extends BasicRestServlet> host, String path, RecordingResponse res) throws Exception {
		var resource = host.getDeclaredConstructor().newInstance();
		var ctx = new RestContext(new RestContext.Args(host, null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT, null)).postInit().postInitChildFirst();
		ctx.execute(resource, MockServletRequest.create("GET", path), res);
		assertEquals(200, res.getStatus());
		return new String(res.getContent(), "UTF-8");
	}

	@Test void t01_tokenEmbedMarksResponsePrivateNoStore() throws Exception {
		try (var c = MockRestClient.buildLax(ConsoleHost.class);
			var rsp = c.get("/csrf").run()) {
			rsp.assertStatus(200).assertHeader("Cache-Control").is("private, no-store");
		}
	}

	@Test void t02_noTokenNoCacheHeader() throws Exception {
		try (var c = MockRestClient.buildLax(ConsoleHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200).assertHeader("Cache-Control").isNull();
		}
	}

	@Test void t03_pageSpecCsrfBodyAttrMarksNoStore() throws Exception {
		try (var c = MockRestClient.buildLax(ConsoleHost.class);
			var rsp = c.get("/spec-csrf").run()) {
			rsp.assertStatus(200).assertHeader("Cache-Control").is("private, no-store");
			assertTrue(rsp.getContent().asString().contains("data-juneau-csrf=\"spec-tok\""));
		}
	}

	@Test void t04_headerSetBeforeFirstWrite() throws Exception {
		var res = new RecordingResponse();
		dispatch(ConsoleHost.class, "/csrf", res);
		assertEquals(Boolean.FALSE, res.writtenWhenCacheControlSet);
	}

	@Test void t05_committedResponseOmitsTokenEmbeds() throws Exception {
		var logger = Logger.getLogger(ConsoleDirectiveModel.class.getName());
		var records = new ArrayList<LogRecord>();
		var handler = new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() { /* no buffering */ }
			@Override public void close() { /* nothing to release */ }
		};
		var oldLevel = logger.getLevel();
		logger.setLevel(Level.FINE);
		handler.setLevel(Level.FINE);
		logger.addHandler(handler);
		try {
			var res = new RecordingResponse();
			res.committed = true;
			var body = dispatch(ConsoleHost.class, "/csrf", res);
			assertFalse(body.contains("tok-123"), () -> body);
			assertFalse(body.contains("csrf-token"), () -> body);
			assertFalse(body.contains("data-juneau-csrf"), () -> body);
			assertFalse(body.contains("X-Csrf-Token"), () -> body);
			assertNull(res.writtenWhenCacheControlSet);
			assertEquals(1, records.stream().filter(r -> r.getLevel() == Level.FINE).count());
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(oldLevel);
		}
	}

	@Test void t05b_committedResponseOmitsSpecAuthoredTokenAttrs() throws Exception {
		var logger = Logger.getLogger(ConsoleDirectiveModel.class.getName());
		var records = new ArrayList<LogRecord>();
		var handler = new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() { /* no buffering */ }
			@Override public void close() { /* nothing to release */ }
		};
		var oldLevel = logger.getLevel();
		logger.setLevel(Level.FINE);
		handler.setLevel(Level.FINE);
		logger.addHandler(handler);
		try {
			var res = new RecordingResponse();
			res.committed = true;
			var body = dispatch(ConsoleHost.class, "/spec-csrf", res);
			assertFalse(body.contains("spec-tok"), () -> body);
			assertFalse(body.contains("data-juneau-csrf"), () -> body);
			assertFalse(body.contains("X-Spec"), () -> body);
			assertTrue(body.contains("data-spec-keep=\"kept\""), () -> body);
			assertNull(res.writtenWhenCacheControlSet);
			assertEquals(1, records.stream().filter(r -> r.getLevel() == Level.FINE).count());
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(oldLevel);
		}
	}

	@Test void t05c_committedResponseOmitsSingleQuotedBodyToken() throws Exception {
		var logger = Logger.getLogger(ConsoleDirectiveModel.class.getName());
		var records = new ArrayList<LogRecord>();
		var handler = new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() { /* no buffering */ }
			@Override public void close() { /* nothing to release */ }
		};
		var oldLevel = logger.getLevel();
		logger.setLevel(Level.FINE);
		handler.setLevel(Level.FINE);
		logger.addHandler(handler);
		try {
			var res = new RecordingResponse();
			res.committed = true;
			var body = dispatch(ConsoleHost.class, "/body-csrf-single", res);
			assertFalse(body.contains("spec-tok"), () -> body);
			assertFalse(body.contains("data-juneau-csrf"), () -> body);
			assertTrue(body.contains("data-spec-keep=\"kept\""), () -> body);
			assertNull(res.writtenWhenCacheControlSet);
			assertEquals(1, records.stream().filter(r -> r.getLevel() == Level.FINE).count());
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(oldLevel);
		}
	}

	@Test void t05e_committedResponseOmitsUnquotedBodyToken() throws Exception {
		var logger = Logger.getLogger(ConsoleDirectiveModel.class.getName());
		var records = new ArrayList<LogRecord>();
		var handler = new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() { /* no buffering */ }
			@Override public void close() { /* nothing to release */ }
		};
		var oldLevel = logger.getLevel();
		logger.setLevel(Level.FINE);
		handler.setLevel(Level.FINE);
		logger.addHandler(handler);
		try {
			var res = new RecordingResponse();
			res.committed = true;
			var body = dispatch(ConsoleHost.class, "/body-csrf-unquoted", res);
			assertFalse(body.contains("spec-tok"), () -> body);
			assertFalse(body.contains("data-juneau-csrf"), () -> body);
			assertTrue(body.contains("data-spec-keep=\"kept\""), () -> body);
			assertNull(res.writtenWhenCacheControlSet);
			assertEquals(1, records.stream().filter(r -> r.getLevel() == Level.FINE).count());
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(oldLevel);
		}
	}

	@Test void t05f_committedResponseOmitsUpperCaseBodyToken() throws Exception {
		var logger = Logger.getLogger(ConsoleDirectiveModel.class.getName());
		var records = new ArrayList<LogRecord>();
		var handler = new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() { /* no buffering */ }
			@Override public void close() { /* nothing to release */ }
		};
		var oldLevel = logger.getLevel();
		logger.setLevel(Level.FINE);
		handler.setLevel(Level.FINE);
		logger.addHandler(handler);
		try {
			var res = new RecordingResponse();
			res.committed = true;
			var body = dispatch(ConsoleHost.class, "/body-csrf-upper", res);
			assertFalse(body.contains("spec-tok"), () -> body);
			assertFalse(body.contains("data-juneau-csrf"), () -> body);
			assertTrue(body.contains("data-spec-keep=\"kept\""), () -> body);
			assertNull(res.writtenWhenCacheControlSet);
			assertEquals(1, records.stream().filter(r -> r.getLevel() == Level.FINE).count());
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(oldLevel);
		}
	}

	@Test void t05g_committedResponseKeepsAttributeWhoseValueMentionsCsrfAttr() throws Exception {
		var res = new RecordingResponse();
		res.committed = true;
		var body = dispatch(ConsoleHost.class, "/body-attrs-csrf-lookalike", res);
		assertTrue(body.contains("<body title=\"data-juneau-csrf=x\" data-spec-keep=\"kept\">"), () -> body);
	}

	@Test void t05d_committedPageWithoutCsrfContentLogsNothingAndKeepsBodyAttrs() throws Exception {
		var logger = Logger.getLogger(ConsoleDirectiveModel.class.getName());
		var records = new ArrayList<LogRecord>();
		var handler = new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() { /* no buffering */ }
			@Override public void close() { /* nothing to release */ }
		};
		var oldLevel = logger.getLevel();
		logger.setLevel(Level.FINE);
		handler.setLevel(Level.FINE);
		logger.addHandler(handler);
		try {
			var res = new RecordingResponse();
			res.committed = true;
			var body = dispatch(ConsoleHost.class, "/body-attrs-plain", res);
			assertTrue(body.contains("<body data-spec-keep=\"kept\" data-other=plain>"), () -> body);
			assertEquals(0, records.stream().filter(r -> r.getLevel() == Level.FINE).count());
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(oldLevel);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Each embed gets its own mask
	// -----------------------------------------------------------------------------------------------------------------

	private static String get(String path, String...headers) throws Exception {
		try (var c = MockRestClient.buildLax(ConsoleHost.class)) {
			var call = c.get(path);
			for (var i = 0; i < headers.length; i += 2)
				call.header(headers[i], headers[i + 1]);
			try (var rsp = call.run()) {
				rsp.assertStatus(200);
				return rsp.getContent().asString();
			}
		}
	}

	@Test void t06_twoEmbedsCarryIndependentPads() throws Exception {
		var body = get("/csrf-masked");
		var meta = metaToken(body);
		var attr = bodyToken(body);
		assertTrue(meta.matches("[0-9a-f]{128}"), meta);
		assertTrue(attr.matches("[0-9a-f]{128}"), attr);
		assertNotEquals(meta, attr, () -> body);
		assertEquals(PROCESS_TOKEN.value(), unmask(meta));
		assertEquals(PROCESS_TOKEN.value(), unmask(attr));
	}

	@Test void t06b_supplierAttributeNeverStringified() throws Exception {
		var body = get("/csrf-supplier-only");
		assertEquals(PROCESS_TOKEN.value(), unmask(metaToken(body)));
		assertEquals(PROCESS_TOKEN.value(), unmask(bodyToken(body)));
		for (var s : List.of("Lambda", "Supplier", "java.util.function", "$$"))
			assertFalse(body.contains(s), () -> "'" + s + "' reached the page: " + body);
	}

	@Test void t06c_overwrittenSupplierIsNotTrustedAndFailsClosed() throws Exception {
		var body = get("/csrf-supplier-overwritten");
		assertFalse(body.contains("ATTACKER-TOKEN"), () -> body);
		assertFalse(body.contains("csrf-token"), () -> body);
		assertFalse(body.contains("data-juneau-csrf"), () -> body);
	}

	@Test void t07_fallsBackToStringAttribute() throws Exception {
		var body = get("/csrf");
		assertEquals("tok-123", metaToken(body));
		assertEquals("tok-123", bodyToken(body));
	}

	@Test void t08_loopbackPageStillEmbeds64Hex() throws Exception {
		var body = get("/csrf-loopback", "Host", LOOPBACK_AUTHORITY);
		assertEquals(PROCESS_TOKEN.value(), metaToken(body));
		assertEquals(PROCESS_TOKEN.value(), bodyToken(body));
		assertTrue(PROCESS_TOKEN.value().matches("[0-9a-f]{64}"));
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
		// The shell ignores header.title when slots.replace is present.
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

	@Test void c11b_stickyChrome_vendorAssetsKeepThePhaseOrder() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(StickyHost.class);
			var rsp = c.get("/wire-order").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		var order = List.of("app-early.css", "dataTables.dataTables.min.css", "href=\"page.css\"", "juneau-views.css", "</head>",
			"juneau-console.js", "/jquery.min.js", "/js/dataTables.min.js", "juneau-datatables.js", "juneau-renders.js",
			"juneau-views.js", "mid-toolkit.js", "src=\"page-init.js\"");
		var last = -1;
		for (var s : order) {
			var i2 = body.indexOf(s);
			var prev = last;
			assertTrue(i2 > prev, () -> "'" + s + "' out of order in:\n" + body);
			last = i2;
		}
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
		// A chrome template that never renders <@console> is the removed legacy path.
		try (var c = MockRestClient.buildLax(NoConsoleHost.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("<@page> chrome template 'admin/no-console-chrome.ftlh' rendered no <@console>; "
				+ "the legacy chrome was removed in 10.0.0."), () -> body);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Colour scheme: html attributes, the pre-paint script, the dark layer link, CSP nonces
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class SchemeHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/").chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		@RestGet(path="/t")
		public View page(RestRequest req) {
			var nonce = req.getQueryParam("nonce").asString().orElse(null);
			if (nonce != null)
				req.setAttribute(RestRequest.CSP_NONCE_ATTR, nonce);
			return FreemarkerView.of("admin/scheme-" + req.getQueryParam("n").asString().orElseThrow() + ".ftlh");
		}
		@RestGet(path="/spec-light")
		public View specLight(RestRequest req) {
			return PageSpec.create().scheme("light").template("admin/page-naked.ftlh").view(req);
		}
		@RestGet(path="/spec-light-attr-dark")
		public View specLightAttrDark(RestRequest req) {
			return PageSpec.create().scheme("light").template("admin/scheme-dark.ftlh").view(req);
		}
		@RestGet(path="/spec-toggle")
		public View specToggle(RestRequest req) {
			return PageSpec.create().schemeToggle(true).template("admin/page-naked.ftlh").view(req);
		}
		@RestGet(path="/spec-toggle-attr-false")
		public View specToggleAttrFalse(RestRequest req) {
			return PageSpec.create().schemeToggle(true).template("admin/scheme-toggle-false.ftlh").view(req);
		}
	}

	private static String scheme(String path, int status) throws Exception {
		try (var c = MockRestClient.buildLax(SchemeHost.class); var rsp = c.get(path).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		}
	}

	/** The {@code <html ...>} start tag of a rendered document. */
	private static String htmlTag(String body) {
		var i = body.indexOf("<html");
		return body.substring(i, body.indexOf('>', i) + 1);
	}

	@Test void s01_unpinnedNoAttrs() throws Exception {
		var tag = htmlTag(scheme("/t?n=none", 200));
		assertEquals("<html lang=\"en\">", tag);
		assertFalse(tag.contains("data-jc-scheme"), tag);
	}

	@Test void s02_pinnedAttrs() throws Exception {
		assertEquals("<html lang=\"en\" data-jc-scheme=\"dark\" data-jc-scheme-pin=\"dark\">", htmlTag(scheme("/t?n=dark", 200)));
		assertEquals("<html lang=\"en\" data-jc-scheme=\"light\" data-jc-scheme-pin=\"light\">", htmlTag(scheme("/t?n=light", 200)));
		assertEquals("<html lang=\"en\">", htmlTag(scheme("/t?n=auto", 200)));
	}

	@Test void s03_toggleAttr() throws Exception {
		assertEquals("<html lang=\"en\" data-jc-scheme-toggle>", htmlTag(scheme("/t?n=toggle-true", 200)));
		assertEquals("<html lang=\"en\">", htmlTag(scheme("/t?n=toggle-false", 200)));
		assertEquals("<html lang=\"en\">", htmlTag(scheme("/t?n=none", 200)));
	}

	@Test void s04_scriptFirstInHead() throws Exception {
		var body = scheme("/t?n=dark", 200);
		var afterHead = body.substring(body.indexOf("<head>") + "<head>".length()).stripLeading();
		assertTrue(afterHead.startsWith("<script>" + ConsoleChromeMixin.SCHEME_SCRIPT + "</script>"), () -> body);
		assertEquals(1, count(body, ConsoleChromeMixin.SCHEME_SCRIPT), () -> body);
	}

	@Test void s05_linkOrder() throws Exception {
		var body = scheme("/t?n=tokens", 200);
		var chrome = body.indexOf("/juneau-console/chrome.css");
		var dark = body.indexOf("juneau-theme-dark.css?v=");
		var palette = body.indexOf("juneau-theme-open.css?v=");
		var style = body.indexOf("<style>");
		assertTrue(chrome >= 0 && chrome < dark && dark < palette && palette < style, () -> body);
		assertEquals(1, count(body, "juneau-theme-dark.css"), () -> body);
		// Unpinned pages link the dark layer too: the pre-paint script may still pick dark.
		assertTrue(scheme("/t?n=none", 200).contains("juneau-theme-dark.css?v="));
	}

	@Test void s06_nonce() throws Exception {
		var body = scheme("/t?n=tokens&nonce=abc", 200);
		assertTrue(body.contains("<script nonce=\"abc\">" + ConsoleChromeMixin.SCHEME_SCRIPT + "</script>"), () -> body);
		assertTrue(body.contains("<style nonce=\"abc\">html:root{"), () -> body);
		var plain = scheme("/t?n=tokens", 200);
		assertFalse(plain.contains("nonce"), () -> plain);
		assertTrue(plain.contains("<script>" + ConsoleChromeMixin.SCHEME_SCRIPT), () -> plain);
		assertTrue(plain.contains("<style>html:root{"), () -> plain);
		var escaped = scheme("/t?n=tokens&nonce=a%22b", 200);
		assertTrue(escaped.contains("nonce=\"a&quot;b\""), () -> escaped);
	}

	@Test void s07_schemeValidation() throws Exception {
		var body = scheme("/t?n=bogus", 500);
		assertTrue(body.contains("<@console> unknown scheme 'dim'.  Schemes: auto, light, dark."), () -> body);
		var theme = scheme("/t?n=theme-dark", 500);
		assertTrue(theme.contains("<@console> unknown theme name 'dark'.  Built-in themes: "), () -> theme);
	}

	@Test void s08_explicitBeatsSpec() throws Exception {
		assertEquals("<html lang=\"en\" data-jc-scheme=\"dark\" data-jc-scheme-pin=\"dark\">", htmlTag(scheme("/spec-light-attr-dark", 200)));
		assertEquals("<html lang=\"en\" data-jc-scheme=\"light\" data-jc-scheme-pin=\"light\">", htmlTag(scheme("/spec-light", 200)));
	}

	@Test void s09_specToggle_explicitAttrWins() throws Exception {
		assertEquals("<html lang=\"en\" data-jc-scheme-toggle>", htmlTag(scheme("/spec-toggle", 200)));
		assertEquals("<html lang=\"en\">", htmlTag(scheme("/spec-toggle-attr-false", 200)));
	}
}
