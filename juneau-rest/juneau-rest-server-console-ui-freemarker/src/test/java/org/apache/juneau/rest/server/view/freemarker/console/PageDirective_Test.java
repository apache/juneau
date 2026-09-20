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
class PageDirective_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class)
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/test-chrome.ftlh")
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
				.chromeTemplate("admin/test-chrome.ftlh")
				.build();
		}
		@RestGet(path="/toolkit")
		public View toolkit() {
			return FreemarkerView.of("admin/page-toolkit.ftlh");
		}
	}

	// A live RestRequest so resolveConfiguration(...) has a request to hand the pack resolvers.
	private static RestRequest dummyRequest() throws Exception {
		var c = MockRestClient.buildLax(DummyHost.class);
		c.get("/x").run();
		return DummyHost.CAPTURED.get();
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
			.chromeTemplate("admin/test-chrome.ftlh").build()
			.resolveConfiguration(dummyRequest());
		assertNull(cfg.getSharedVariable("viewSlot"));
		assertNotNull(cfg.getSharedVariable("page"));
		assertNotNull(cfg.getSharedVariable("card"));
	}

	@Test void freeze_demoDatatableMacro_isNotTypeDatatables() throws Exception {
		// The demo <@datatable> macro lives in the datatables module; this module must NOT define a shared
		// variable named "datatable".  type="datatables" on <@card> is the framework surface, not a directive.
		var cfg = ConsoleFreemarkerMixin.create().basePath("/templates/")
			.chromeTemplate("admin/test-chrome.ftlh").build()
			.resolveConfiguration(dummyRequest());
		assertNull(cfg.getSharedVariable("datatable"));
	}

	@Test void a01_nakedHtml_isWrappedInSingleMain_noSecondMain() throws Exception {
		var body = MockRestClient.buildLax(Host.class).get("/naked").run()
			.assertStatus(200).getContent().asString();
		assertTrue(body.contains("<p class=\"naked-html\">hello</p>")
			|| body.contains("<p class='naked-html'>hello</p>"), () -> body);
		assertEquals(1, count(body, "<main"), () -> body);
		assertTrue(body.contains("class=\"jc-main\"") || body.contains("class='jc-main'"), () -> body);
		assertFalse(body.contains("slds-"), "dual-hat: no slds-* in page output");
	}

	@Test void a02_tab_init_css_areEmittedGenerically() throws Exception {
		var body = MockRestClient.buildLax(Host.class).get("/assets").run()
			.assertStatus(200).getContent().asString();
		assertTrue(body.contains("name=\"page-tab\"") && body.contains("releases"), () -> body);
		assertTrue(body.contains("href=\"a.css\""), () -> body);
		assertTrue(body.contains("href=\"b.css\""), () -> body);
		assertTrue(body.indexOf("a.css") < body.indexOf("b.css"), () -> body);
		assertTrue(body.contains("src=\"one.js\""), () -> body);
		assertTrue(body.contains("src=\"two.js\""), () -> body);
		assertTrue(body.indexOf("one.js") < body.indexOf("two.js"), () -> body);
		assertFalse(body.contains("data-toolkit-js"), "omit toolkit= means no pack");
	}

	@Test void a02b_sequenceLiterals_areFirstClass() throws Exception {
		var body = MockRestClient.buildLax(Host.class).get("/assets-seq").run()
			.assertStatus(200).getContent().asString();
		assertTrue(body.contains("href=\"a.css\"") && body.contains("href=\"b.css\""), () -> body);
		assertTrue(body.indexOf("a.css") < body.indexOf("b.css"), () -> body);
		assertTrue(body.contains("src=\"one.js\"") && body.contains("src=\"two.js\""), () -> body);
		assertTrue(body.indexOf("one.js") < body.indexOf("two.js"), () -> body);
	}

	@Test void a03_omitToolkit_doesNotInferViewsPack() throws Exception {
		var body = MockRestClient.buildLax(Host.class).get("/naked").run()
			.assertStatus(200).getContent().asString();
		assertFalse(body.contains("juneau-views.js"), () -> body);
		assertFalse(body.contains("juneau-page-cards.js"), () -> body);
	}

	@Test void a05_toolkitViews_emitsViewsPack_pageCardsLast_noDatatables() throws Exception {
		var body = MockRestClient.buildLax(ToolkitHost.class).get("/toolkit").run()
			.assertStatus(200).getContent().asString();
		// The "views" pack is emitted (marked so a consumer chrome can find it), page-cards is present, and
		// page-cards.js is the LAST toolkit JS entry.
		assertTrue(body.contains("data-toolkit-js"), () -> body);
		assertTrue(body.contains("juneau-views.js"), () -> body);
		assertTrue(body.contains("juneau-page-cards.js"), () -> body);
		assertTrue(body.indexOf("juneau-views.js") < body.indexOf("juneau-page-cards.js"), () -> body);
		assertTrue(body.indexOf("juneau-helpers.js") < body.indexOf("juneau-page-cards.js"), () -> body);
		// Dual-hat / anti-pattern: the first-party pack pulls in neither DataTables nor jQuery.
		assertFalse(body.contains("datatables"), () -> body);
		assertFalse(body.contains("jquery"), () -> body);
		assertFalse(body.contains("slds-"), () -> body);
	}

	@Test void a04_unknownAttr_isRejected() throws Exception {
		var rsp = MockRestClient.buildLax(Host.class).get("/page-unknown").run();
		rsp.assertStatus(500);
		var body = rsp.getContent().asString();
		assertTrue(body.contains("unknown attribute") && body.contains("foo"), () -> body);
	}

	static int count(String body, String needle) {
		int n = 0, i = 0;
		while ((i = body.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
		return n;
	}
}
