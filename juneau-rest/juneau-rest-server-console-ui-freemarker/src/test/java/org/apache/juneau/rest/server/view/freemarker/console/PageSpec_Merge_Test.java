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

import java.util.*;

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
 * Tests how a {@link PageSpec} merges with the same fields set from FTL: tab, theme, toolkit/css/init and title.
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class PageSpec_Merge_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-chrome-bare.ftlh")
				.registerToolkitPack(ToolkitPack.create("alpha").kind(ToolkitPack.Kind.VENDOR).resolver((req, path) -> path).css("/alpha.css").js("/alpha.js").build())
				.registerToolkitPack(ToolkitPack.create("beta").kind(ToolkitPack.Kind.VENDOR).resolver((req, path) -> path).css("/beta.css").js("/beta.js").build())
				.build();
		}
		@RestGet(path="/tab-equal")
		public View tabEqual(RestRequest req) {
			return PageSpec.create().tab("home").template("admin/page-merge-tab.ftlh").view(req);
		}
		@RestGet(path="/tab-differ")
		public View tabDiffer(RestRequest req) {
			return PageSpec.create().tab("other").template("admin/page-merge-tab.ftlh").view(req);
		}
		@RestGet(path="/tab-inherit")
		public View tabInherit(RestRequest req) {
			return PageSpec.create().tab("home").template("admin/page-merge-notab.ftlh").view(req);
		}
		@RestGet(path="/theme-equal")
		public View themeEqual(RestRequest req) {
			return PageSpec.create().theme("light-brown").template("admin/page-merge-theme.ftlh").view(req);
		}
		@RestGet(path="/theme-differ")
		public View themeDiffer(RestRequest req) {
			return PageSpec.create().theme("open").template("admin/page-merge-theme.ftlh").view(req);
		}
		@RestGet(path="/theme-inherit")
		public View themeInherit(RestRequest req) {
			return PageSpec.create().theme("light-brown").template("admin/page-merge-notheme.ftlh").view(req);
		}
		@RestGet(path="/theme-two-directives")
		public View themeTwoDirectives(RestRequest req) {
			return PageSpec.create().template("admin/page-merge-theme-two.ftlh").view(req);
		}
		@RestGet(path="/theme-attr-wins")
		public View themeAttrWins(RestRequest req) {
			return PageSpec.create().theme("light-brown").template("admin/page-merge-theme-attr.ftlh").view(req);
		}
		@RestGet(path="/theme-default")
		public View themeDefault(RestRequest req) {
			return PageSpec.create().template("admin/page-merge-notheme.ftlh").view(req);
		}
		@RestGet(path="/title-chrome-kept")
		public View titleChromeKept(RestRequest req) {
			return PageSpec.create().template("admin/page-merge-title.ftlh").view(req);
		}
		@RestGet(path="/css-dup")
		public View cssDup(RestRequest req) {
			return PageSpec.create().template("admin/page-merge-css-dup.ftlh").view(req);
		}
		@RestGet(path="/bodyattrs-conflict")
		public View bodyAttrsConflict(RestRequest req) {
			return PageSpec.create().bodyAttr("data-x", "1").template("admin/page-merge-bodyattrs.ftlh").view(req);
		}
		@RestGet(path="/bodyattrs-union")
		public View bodyAttrsUnion(RestRequest req) {
			return PageSpec.create().bodyAttr("data-y", "1").template("admin/page-merge-bodyattrs.ftlh").view(req);
		}
		@RestGet(path="/csrf-no-double-emit")
		public View csrfNoDoubleEmit(RestRequest req) {
			// Publish what the loopback filter would, so <@console> auto-detects a token.
			req.setAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, "req-tok");
			req.setAttribute(LoopbackBoundaryFilter.HEADER_ATTRIBUTE, "X-Req");
			return PageSpec.create().csrf("spec-tok", "X-Spec").template("admin/page-merge-csrf.ftlh").view(req);
		}
		@RestGet(path="/csrf-baseline")
		public View csrfBaseline(RestRequest req) {
			publishCsrf(req);
			return FreemarkerView.of("admin/page-merge-csrf.ftlh");
		}
		@RestGet(path="/csrf-ftl-token")
		public View csrfFtlToken(RestRequest req) {
			publishCsrf(req);
			return FreemarkerView.of("admin/page-merge-csrf-body-token.ftlh");
		}
		@RestGet(path="/csrf-ftl-header")
		public View csrfFtlHeader(RestRequest req) {
			publishCsrf(req);
			return FreemarkerView.of("admin/page-merge-csrf-body-header.ftlh");
		}
		@RestGet(path="/csrf-spec-null-header")
		public View csrfSpecNullHeader(RestRequest req) {
			publishCsrf(req);
			return PageSpec.create().csrf("spec-tok", null).template("admin/page-merge-csrf.ftlh").view(req);
		}
		@RestGet(path="/csrf-both")
		public View csrfBoth(RestRequest req) {
			publishCsrf(req);
			return PageSpec.create().csrf("spec-tok", null).template("admin/page-merge-csrf-body-token.ftlh").view(req);
		}
		private static void publishCsrf(RestRequest req) {
			req.setAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, "req-tok");
			req.setAttribute(LoopbackBoundaryFilter.HEADER_ATTRIBUTE, "X-Req");
		}
		@RestGet(path="/assets-union")
		public View assetsUnion(RestRequest req) {
			return PageSpec.create().toolkit("alpha").css("a.css").init("one.js")
				.template("admin/page-merge-assets.ftlh").view(req);
		}
		@RestGet(path="/title-spec-wins")
		public View titleSpecWins(RestRequest req) {
			return PageSpec.create().title("Spec Title").template("admin/page-merge-title.ftlh").view(req);
		}
		@RestGet(path="/navunder-fleet")
		public View navUnderFleet(RestRequest req) {
			return PageSpec.create().navUnder("fleet", n -> n.add("extra", "Extra", "/fleet/extra"))
				.template("admin/page-merge-navunder.ftlh").view(req);
		}
		@RestGet(path="/navunder-dup")
		public View navUnderDup(RestRequest req) {
			return PageSpec.create().navUnder("fleet", n -> {
				n.add("extra", "Extra", "/fleet/extra");
				n.add("extra", "Extra2", "/fleet/extra2");
			}).template("admin/page-merge-navunder.ftlh").view(req);
		}
		@RestGet(path="/navunder-reuse")
		public View navUnderReuse(RestRequest req) {
			return REUSED.view(req);
		}
		@RestGet(path="/navunder-missing")
		public View navUnderMissing(RestRequest req) {
			return PageSpec.create().navUnder("nope", n -> n.add("extra", "Extra", "/x"))
				.template("admin/page-merge-navunder.ftlh").view(req);
		}
	}

	// One reusable spec rendered more than once: each render must get its own copy of the addition.
	static final PageSpec REUSED = PageSpec.create().navUnder("fleet", n -> n.add("extra", "Extra", "/fleet/extra"))
		.template("admin/page-merge-navunder.ftlh");

	private static String get(String path, int status) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get(path).run()) {
			var body = rsp.getContent().asString();
			assertEquals(status, rsp.getStatusCode(), () -> body);
			return body;
		}
	}

	@Test void a01_tab_specAndFtl_equal_isRejected() throws Exception {
		var body = get("/tab-equal", 500);
		assertTrue(body.contains("PageSpec sets tab='home' and <@page tab='home'> also sets it; set it in one place."), () -> body);
	}

	@Test void a02_tab_specAndFtl_differ_isRejected() throws Exception {
		var body = get("/tab-differ", 500);
		assertTrue(body.contains("PageSpec sets tab='other' and <@page tab='home'> also sets it; set it in one place."), () -> body);
	}

	@Test void a03_tab_specOnly_isInherited() throws Exception {
		assertPage(get("/tab-inherit", 200)).isValid().hasActiveNav("home");
	}

	@Test void a04_theme_specAndDirective_equal_noConflict() throws Exception {
		assertPage(get("/theme-equal", 200)).isValid().hasTheme("light-brown");
	}

	@Test void a05_theme_specAndDirective_differ_isRejected() throws Exception {
		var body = get("/theme-differ", 500);
		assertTrue(body.contains(
			"PageSpec theme 'open' conflicts with <@theme name='light-brown'>; remove name= from <@theme> to inherit the page theme."), () -> body);
	}

	@Test void a06_theme_specOnly_noDirective_isInherited() throws Exception {
		assertPage(get("/theme-inherit", 200)).isValid().hasTheme("light-brown");
	}

	@Test void a07_toolkitCssInit_union_specFirstThenFtl_andBothPacksResolve() throws Exception {
		var body = get("/assets-union", 200);
		// "alpha" comes from the spec alone, so this also proves pack resolution runs over the merged list.
		assertTrue(body.contains("alpha.css") && body.contains("beta.css"), () -> body);
		assertTrue(body.indexOf("alpha.css") < body.indexOf("beta.css"), () -> body);
		assertTrue(body.contains("alpha.js") && body.contains("beta.js"), () -> body);
		assertTrue(body.indexOf("alpha.js") < body.indexOf("beta.js"), () -> body);
		assertTrue(body.contains("href=\"a.css\"") && body.contains("href=\"b.css\""), () -> body);
		assertTrue(body.indexOf("a.css") < body.indexOf("b.css"), () -> body);
		assertTrue(body.contains("src=\"one.js\"") && body.contains("src=\"two.js\""), () -> body);
		assertTrue(body.indexOf("one.js") < body.indexOf("two.js"), () -> body);
	}

	@Test void a08_title_spec_winsOverChromeAttr() throws Exception {
		var body = get("/title-spec-wins", 200);
		assertPage(body).isValid().hasTitle("Spec Title");
		assertTrue(body.contains("<title>Spec Title</title>"), () -> body);
	}

	@Test void a09_theme_twoDirectivesDiffer_ftlOnly_hasOwnMessage() throws Exception {
		var body = get("/theme-two-directives", 500);
		assertTrue(body.contains("<@theme name='open'> conflicts with an earlier <@theme name='light-brown'> on this page; use one."), () -> body);
	}

	@Test void a10_theme_consoleAttr_beatsSpecTheme() throws Exception {
		assertPage(get("/theme-attr-wins", 200)).isValid().hasTheme("open");
	}

	@Test void a11_theme_noSpecNoDirective_fallsBackToOpen() throws Exception {
		assertPage(get("/theme-default", 200)).isValid().hasTheme("open");
	}

	@Test void a12_title_noSpecTitle_keepsChromeTitle() throws Exception {
		var body = get("/title-chrome-kept", 200);
		assertPage(body).isValid().hasTitle("Chrome Title");
		assertTrue(body.contains("<title>Chrome Title</title>"), () -> body);
	}

	@Test void a13_css_duplicateWithinOneList_emittedOnce() throws Exception {
		var body = get("/css-dup", 200);
		assertEquals(body.indexOf("href=\"a.css\""), body.lastIndexOf("href=\"a.css\""), () -> body);
		assertTrue(body.contains("href=\"a.css\""), () -> body);
	}

	@Test void a14_bodyAttrs_specAndFtl_sameName_isRejected() throws Exception {
		var body = get("/bodyattrs-conflict", 500);
		assertTrue(body.contains("PageSpec body attribute 'data-x' is also set by <@body>; set it in one place."), () -> body);
	}

	@Test void a15_bodyAttrs_specAndFtl_disjointNames_union() throws Exception {
		var body = get("/bodyattrs-union", 200);
		assertTrue(body.contains("data-y=\"1\"") && body.contains("data-x=\"2\""), () -> body);
		assertTrue(body.indexOf("data-y=") < body.indexOf("data-x="), () -> body);
	}

	@Test void a16_csrf_specAuthored_isNeverDoubleEmitted() throws Exception {
		var body = get("/csrf-no-double-emit", 200);
		assertEquals(1, countOccurrences(body, "data-juneau-csrf=\"spec-tok\""), () -> body);
		assertEquals(1, countOccurrences(body, "data-juneau-csrf-header=\"X-Spec\""), () -> body);
		assertFalse(body.contains("data-juneau-csrf=\"req-tok\"") || body.contains("data-juneau-csrf-header=\"X-Req\""), () -> body);
	}

	@Test void a17_csrf_requestToken_ftlOnly_emittedUnchanged() throws Exception {
		var body = get("/csrf-baseline", 200);
		assertEquals(1, countOccurrences(body, "data-juneau-csrf=\"req-tok\""), () -> body);
		assertEquals(1, countOccurrences(body, "data-juneau-csrf-header=\"X-Req\""), () -> body);
	}

	@Test void a18_csrf_bodyAuthoredToken_winsOverRequestToken_andSuppressesAutoHeader() throws Exception {
		var body = get("/csrf-ftl-token", 200);
		assertEquals(1, countOccurrences(body, "data-juneau-csrf=\"ftl-tok\""), () -> body);
		assertFalse(body.contains("data-juneau-csrf=\"req-tok\""), () -> body);
		assertFalse(body.contains("data-juneau-csrf-header="), () -> body);
	}

	@Test void a19_csrf_bodyAuthoredHeaderOnly_suppressesAutoToken() throws Exception {
		var body = get("/csrf-ftl-header", 200);
		assertEquals(1, countOccurrences(body, "data-juneau-csrf-header=\"X-Ftl\""), () -> body);
		assertFalse(body.contains("data-juneau-csrf=\"req-tok\""), () -> body);
		assertFalse(body.contains("X-Req"), () -> body);
	}

	@Test void a20_csrf_specTokenWithoutHeader_getsNoAutoHeader() throws Exception {
		var body = get("/csrf-spec-null-header", 200);
		assertEquals(1, countOccurrences(body, "data-juneau-csrf=\"spec-tok\""), () -> body);
		assertFalse(body.contains("data-juneau-csrf-header="), () -> body);
	}

	@Test void a21_csrf_specAndBodyBothSetToken_isRejected() throws Exception {
		var body = get("/csrf-both", 500);
		assertTrue(body.contains("PageSpec body attribute 'data-juneau-csrf' is also set by <@body>; set it in one place."), () -> body);
	}

	@Test void a22_navUnder_specEntry_landsUnderFtlAncestor() throws Exception {
		var body = get("/navunder-fleet", 200);
		assertPage(body).isValid().hasNavChildren("fleet", "extra").hasNavHref("fleet/extra", "/fleet/extra");
	}

	@Test void a23_navUnder_unknownParent_isRejected() throws Exception {
		var body = get("/navunder-missing", 500);
		assertTrue(body.contains(
			"Nav addition under 'nope' does not match a <@node> path in the chrome; known paths: 'fleet'."), () -> body);
	}

	@Test void a24_navUnder_duplicateSiblingInAdder_isE3() throws Exception {
		var body = get("/navunder-dup", 500);
		assertTrue(body.contains("<@node id='extra'> duplicates a sibling id under 'fleet'."), () -> body);
	}

	@Test void a25_navUnder_reusedSpec_rendersIdenticallyEachTime() throws Exception {
		var first = get("/navunder-reuse", 200);
		var second = get("/navunder-reuse", 200);
		assertPage(first).isValid().hasNavChildren("fleet", "extra");
		assertPage(second).isValid().hasNavChildren("fleet", "extra");
		assertEquals(1, countOccurrences(second, "\"id\":\"extra\""), () -> second);
		assertEquals(first, second);
	}

	private static int countOccurrences(String haystack, String needle) {
		var count = 0;
		for (var i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1))
			count++;
		return count;
	}
}
