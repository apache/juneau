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
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

/**
 * Golden-HTML tests for the {@code <@page>} shared directive: capture nested markup, set chrome
 * variables, and include the consumer chrome template exactly once.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class PageDirective_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class)
	public static class Host extends BasicRestServlet {
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
		@RestGet(path="/assets")
		public View assets() {
			return FreemarkerView.of("admin/page-assets.ftlh");
		}
		@RestGet(path="/assets-seq")
		public View assetsSeq() {
			return FreemarkerView.of("admin/page-assets-seq.ftlh");
		}
		@RestGet(path="/page-unknown")
		public View pageUnknown() {
			return FreemarkerView.of("admin/page-unknown.ftlh");
		}
	}

	// Composes ViewsMixin so the "views" toolkit pack's asset routes are also mounted; <@page toolkit="views">
	// resolves the pack through ViewsMixin::viewAssetUrl against the in-flight request.
	@Rest(mixins={FreemarkerMixin.class, ViewsMixin.class})
	public static class ToolkitHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome-bare.ftlh")
				.build();
		}
		@RestGet(path="/toolkit")
		public View toolkit() {
			return FreemarkerView.of("admin/page-toolkit.ftlh");
		}
		@RestGet(path="/toolkit-server")
		public View toolkitServer() {
			return FreemarkerView.of("admin/page-toolkit-server-table.ftlh");
		}
		@RestGet(path="/toolkit-client")
		public View toolkitClient() {
			return FreemarkerView.of("admin/page-toolkit-client-table.ftlh");
		}
		@RestGet(path="/toolkit-url")
		public View toolkitUrl() {
			return FreemarkerView.of("admin/page-toolkit-url-table.ftlh");
		}
	}

	// A live RestRequest so resolveConfiguration(...) has a request to hand the pack resolvers.
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

	@Test void freeze_noViewSlotDirective() throws Exception {
		// The v1 framework registers page/card (and the Tag* method model); it never registered a
		// <@viewSlot> directive.  Freeze that: page and card are present, viewSlot never was.
		var cfg = ConsoleFreemarkerMixin.create().basePath("/templates/")
			.chromeTemplate("admin/console-chrome-bare.ftlh").build()
			.resolveConfiguration(dummyRequest());
		assertNull(cfg.getSharedVariable("viewSlot"));
		assertNotNull(cfg.getSharedVariable("page"));
		assertNotNull(cfg.getSharedVariable("card"));
	}

	@Test void freeze_demoDatatableMacro_isNotTypeDatatables() throws Exception {
		// The demo <@datatable> macro lives in the datatables module; this module must NOT define a shared
		// variable named "datatable".  type="datatables" on <@card> is the framework surface, not a directive.
		var cfg = ConsoleFreemarkerMixin.create().basePath("/templates/")
			.chromeTemplate("admin/console-chrome-bare.ftlh").build()
			.resolveConfiguration(dummyRequest());
		assertNull(cfg.getSharedVariable("datatable"));
	}

	@Test void a01_nakedHtml_isOneSegmentCard() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertPage(body).isValid().hasCardOrder("jc-seg-1")
			.templateContains("jc-seg-1", "<p class=\"naked-html\">hello</p>");
		assertFalse(body.contains("<main"), () -> body);
		assertFalse(body.contains("slds-"), "dual-hat: no slds-* in page output");
	}

	@Test void a02_tab_init_css_areEmittedGenerically() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/assets").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("name=\"page-tab\"") && body.contains("content=\"home\""), () -> body);
		assertTrue(body.contains("href=\"a.css\""), () -> body);
		assertTrue(body.contains("href=\"b.css\""), () -> body);
		assertTrue(body.indexOf("a.css") < body.indexOf("b.css"), () -> body);
		assertTrue(body.contains("src=\"one.js\""), () -> body);
		assertTrue(body.contains("src=\"two.js\""), () -> body);
		assertTrue(body.indexOf("one.js") < body.indexOf("two.js"), () -> body);
		assertFalse(body.contains("data-toolkit-js"), "omit toolkit= means no pack");
		assertPage(body).isValid().hasActiveNav("home");
	}

	@Test void a02b_sequenceLiterals_areFirstClass() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/assets-seq").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertTrue(body.contains("href=\"a.css\"") && body.contains("href=\"b.css\""), () -> body);
		assertTrue(body.indexOf("a.css") < body.indexOf("b.css"), () -> body);
		assertTrue(body.contains("src=\"one.js\"") && body.contains("src=\"two.js\""), () -> body);
		assertTrue(body.indexOf("one.js") < body.indexOf("two.js"), () -> body);
	}

	@Test void a03_omitToolkit_doesNotInferViewsPack() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/naked").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertFalse(body.contains("juneau-views.js"), () -> body);
		assertFalse(body.contains("juneau-page-cards.js"), () -> body);
	}

	private static String toolkitBody(String path) throws Exception {
		try (var c = MockRestClient.buildLax(ToolkitHost.class);
			var rsp = c.get(path).run()) {
			rsp.assertStatus(200);
			return rsp.getContent().asString();
		}
	}

	@Test void a05_toolkitViews_noCards_noVendor() throws Exception {
		var body = toolkitBody("/toolkit");
		// The "views" pack is emitted (marked so a consumer chrome can find it); page-cards is no longer part of it.
		assertTrue(body.contains("data-toolkit-js"), () -> body);
		assertTrue(body.contains("juneau-views.js"), () -> body);
		assertFalse(body.contains("juneau-page-cards.js"), () -> body);
		assertTrue(body.indexOf("juneau-views.js") < body.indexOf("juneau-helpers.js"), () -> body);
		// No table card: no glue, no DataTables, no jQuery.
		assertFalse(body.toLowerCase().contains("datatables"), () -> body);
		assertFalse(body.contains("/webjars/jquery/"), () -> body);
		assertFalse(body.contains("slds-"), () -> body);
	}

	@Test void a05b_toolkitViews_serverModeCard_emitsVendorBeforeViewsJs() throws Exception {
		for (var path : java.util.List.of("/toolkit-server", "/toolkit-url"))
			assertTableVendor(path, toolkitBody(path));
	}

	@Test void a05c_toolkitViews_clientModeCard_emitsVendorBeforeViewsJs() throws Exception {
		assertTableVendor("/toolkit-client", toolkitBody("/toolkit-client"));
	}

	// Every datatables card, client or server mode, pulls jQuery, DataTables and the glue, once each, before the runtime.
	private static void assertTableVendor(String path, String body) {
		var jq = body.indexOf("/jquery.min.js");
		var dt = body.indexOf("/js/dataTables.min.js");
		var glue = body.indexOf("juneau-datatables.js");
		var views = body.indexOf("juneau-views.js");
		assertTrue(jq >= 0 && jq < dt && dt < glue && glue < views, () -> path + ": " + body);
		assertTrue(body.contains("/css/dataTables.dataTables.min.css"), () -> path + ": " + body);
		for (var s : java.util.List.of("/jquery.min.js", "/js/dataTables.min.js", "juneau-datatables.js"))
			assertEquals(body.indexOf(s), body.lastIndexOf(s), () -> path + ": " + s + " emitted once: " + body);
	}

	@Test void a04_unknownAttr_isRejected() throws Exception {
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/page-unknown").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("unknown attribute") && body.contains("foo"), () -> body);
		}
	}
}
