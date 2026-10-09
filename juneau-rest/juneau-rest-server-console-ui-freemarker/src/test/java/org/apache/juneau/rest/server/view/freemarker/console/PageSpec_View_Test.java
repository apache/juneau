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
import org.junit.jupiter.api.*;

/**
 * {@link PageSpec#view(RestRequest)}, the {@code PageCapture.of(env)} self-seed hook, and the adoption check.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class PageSpec_View_Test extends TestBase {

	public static class NoMixinHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final ThreadLocal<RestRequest> CAPTURED = new ThreadLocal<>();
		@RestGet(path="/x")
		public String x(RestRequest req) {
			CAPTURED.set(req);
			return "x";
		}
	}

	@Rest(mixins=FreemarkerMixin.class)
	public static class PlainMixinHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final ThreadLocal<RestRequest> CAPTURED = new ThreadLocal<>();
		@Bean public FreemarkerMixin freemarker() {
			return FreemarkerMixin.create().basePath("/templates/").build();
		}
		@RestGet(path="/x")
		public String x(RestRequest req) {
			CAPTURED.set(req);
			return "x";
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class ConsoleHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final ThreadLocal<RestRequest> CAPTURED = new ThreadLocal<>();
		static final PageSpec CACHED = PageSpec.create().csrf("cached", null);
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/")
				.chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		@RestGet(path="/built")
		public View built(RestRequest req) {
			return PageSpec.create().csrf("tok", "X-CSRF").view(req);
		}
		@RestGet(path="/cached")
		public View cached(RestRequest req) {
			return CACHED.view(req);
		}
		@RestGet(path="/plain-with-spec")
		public View plainWithSpec(RestRequest req) {
			return PageSpec.create().template("admin/plain-no-console.ftlh").view(req);
		}
		@RestGet(path="/x")
		public String x(RestRequest req) {
			CAPTURED.set(req);
			return "x";
		}
	}

	// The bean is declared with the subtype, so the exact-type lookup misses and the registered-subtype fallback finds it.
	@Rest(mixins=FreemarkerMixin.class)
	public static class SubtypeBeanHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		static final ThreadLocal<RestRequest> CAPTURED = new ThreadLocal<>();
		@Bean public ConsoleFreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/")
				.chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		@RestGet(path="/x")
		public String x(RestRequest req) {
			CAPTURED.set(req);
			return "x";
		}
	}

	private static RestRequest capture(Class<?> host, ThreadLocal<RestRequest> slot) throws Exception {
		try (var c = MockRestClient.buildLax(host); var rsp = c.get("/x").run()) {
			rsp.assertStatus(200);
			return slot.get();
		}
	}

	private static String get(String path, int status) throws Exception {
		try (var c = MockRestClient.buildLax(ConsoleHost.class); var rsp = c.get(path).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		}
	}

	@Test void a01_view_noMixinBean_rejectedWithFoundNone() throws Exception {
		var req = capture(NoMixinHost.class, NoMixinHost.CAPTURED);
		var ex = assertThrows(IllegalStateException.class, () -> PageSpec.create().view(req));
		assertEquals("PageSpec.view needs a ConsoleFreemarkerMixin bean; found 'none'.", ex.getMessage());
	}

	@Test void a02_view_plainFreemarkerMixinBean_rejected() throws Exception {
		var req = capture(PlainMixinHost.class, PlainMixinHost.CAPTURED);
		var ex = assertThrows(IllegalStateException.class, () -> PageSpec.create().view(req));
		assertEquals("PageSpec.view needs a ConsoleFreemarkerMixin bean; found 'FreemarkerMixin'.", ex.getMessage());
	}

	@Test void a03_view_consoleMixinBean_defaultsToItsChromeTemplate() throws Exception {
		var req = capture(ConsoleHost.class, ConsoleHost.CAPTURED);
		assertEquals("admin/console-chrome-bare.ftlh", PageSpec.create().view(req).getTemplateName());
	}

	@Test void a04_view_explicitTemplate_winsOverChromeTemplate() throws Exception {
		var req = capture(ConsoleHost.class, ConsoleHost.CAPTURED);
		assertEquals("admin/page-naked.ftlh", PageSpec.create().template("admin/page-naked.ftlh").view(req).getTemplateName());
	}

	@Test void a05_view_carriesTheSpecUnderAttr() throws Exception {
		var req = capture(ConsoleHost.class, ConsoleHost.CAPTURED);
		var spec = PageSpec.create();
		var v = spec.view(req);
		var seed = (PageSpec.Seed)v.getAttributes().get(PageSpec.ATTR);
		assertSame(spec, seed.spec());
		assertFalse(seed.consumed());
	}

	@Test void a06_view_eachCallGetsItsOwnAdoptionState() throws Exception {
		var req = capture(ConsoleHost.class, ConsoleHost.CAPTURED);
		var spec = PageSpec.create();
		var s1 = (PageSpec.Seed)spec.view(req).getAttributes().get(PageSpec.ATTR);
		var s2 = (PageSpec.Seed)spec.view(req).getAttributes().get(PageSpec.ATTR);
		s1.markConsumed();
		assertTrue(s1.consumed());
		assertFalse(s2.consumed());
	}

	@Test void a07_view_passesThroughAttrAndHeader() throws Exception {
		var req = capture(ConsoleHost.class, ConsoleHost.CAPTURED);
		var v = PageSpec.create().attr("caption", "Hi").header("X-Foo", "bar").view(req);
		assertEquals("Hi", v.getAttributes().get("caption"));
		assertEquals("bar", v.getResponseHeaders().get("X-Foo"));
	}

	@Test void a08_notConsumedMessage() {
		var msg = new PageSpec.Seed(PageSpec.create()).notConsumedMessage("admin/plain-no-console.ftlh");
		assertEquals("Template 'admin/plain-no-console.ftlh' was rendered with a PageSpec, but the spec was never "
			+ "adopted; the template must use <@page> or include the console chrome.", msg);
	}

	@Test void a09_builtPage_rendersThroughConsoleChrome_bodyAttrsAreSeeded() throws Exception {
		var body = get("/built", 200);
		assertPage(body).isValid();
		assertTrue(body.contains("data-juneau-csrf=\"tok\" data-juneau-csrf-header=\"X-CSRF\""), body);
	}

	@Test void a10_cachedSpec_rendersTwice() throws Exception {
		try (var c = MockRestClient.buildLax(ConsoleHost.class)) {
			for (var i = 0; i < 2; i++) {
				try (var rsp = c.get("/cached").run()) {
					rsp.assertStatus(200);
					var body = rsp.getContent().asString();
					assertPage(body).isValid();
					assertTrue(body.contains("data-juneau-csrf=\"cached\""), body);
				}
			}
		}
	}

	@Test void a11_templateThatNeverReachesConsole_failsAdoptionCheck() throws Exception {
		var body = get("/plain-with-spec", 500);
		assertTrue(body.contains("Template 'admin/plain-no-console.ftlh' was rendered with a PageSpec, but the "
			+ "spec was never adopted; the template must use <@page> or include the console chrome."), body);
	}

	@Test void a12_view_nullRequest_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().view(null));
		assertEquals("PageSpec.view requires a non-null request.", ex.getMessage());
	}

	@Test void a13_view_subtypeBean_foundThroughRegisteredSubtypes() throws Exception {
		var req = capture(SubtypeBeanHost.class, SubtypeBeanHost.CAPTURED);
		assertEquals("admin/console-chrome-bare.ftlh", PageSpec.create().view(req).getTemplateName());
	}
}
