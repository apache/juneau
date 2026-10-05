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
 * Tests for the {@code <@navigation>} / recursive {@code <@node>} chrome directives: they build the
 * contract's {@code nav[]} tree (to any depth), {@code tab=} on the page resolves {@code activeNav},
 * {@code visible=false} drops a node and its whole subtree from the contract, and an unknown
 * {@code <@node>} attribute is rejected. The shell renders the nav markup and {@code aria-current} from
 * this contract data (covered by {@code ConsoleNavDepth_BrowserTest}, a later task), not server HTML.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class NavigationDirective_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-nav.ftlh")
				.build();
		}
		@RestGet(path="/nav")
		public View nav() {
			return FreemarkerView.of("admin/page-nav-highlight.ftlh").attr("isAdmin", false);
		}
	}

	// A distinct chrome whose nav carries an unknown attribute, to exercise the rejection path.
	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class BogusHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-nav-bogus.ftlh")
				.build();
		}
		@RestGet(path="/nav-bogus")
		public View navBogus() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class DeepHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-nav-deep.ftlh")
				.build();
		}
		@RestGet(path="/nav-deep")
		public View navDeep() {
			return FreemarkerView.of("admin/page-nav-deep.ftlh");
		}
		@RestGet(path="/nav-leaf")
		public View navLeaf() {
			return FreemarkerView.of("admin/page-nav-leaf.ftlh");
		}
	}

	@Test void c01_navTree_activePath_hiddenSubtreeOmitted() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(Host.class);
			var rsp = c.get("/nav").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		var a = assertPage(body).isValid()
			.hasNavChildren("home", "skill-repos", "settings")
			.hasNavHref("home/skill-repos", "/home/skill-repos")
			.hasActiveNav("home", "skill-repos");
		// visible=isAdmin with isAdmin=false: Setup and its subtree are not in the contract at all.
		assertEquals(1, a.contract().getList("nav").size(), () -> body);
		assertFalse(a.contract().toString().contains("users"), () -> body);
		// No server-rendered nav and no aria-current: the shell sets it (D2).
		assertFalse(body.contains("juneau-page-nav"), () -> body);
		assertFalse(body.contains("aria-current"), () -> body);
	}

	@Test void c02_unknownNodeAttr_isRejected() throws Exception {
		try (var c = MockRestClient.buildLax(BogusHost.class);
			var rsp = c.get("/nav-bogus").run()) {
			rsp.assertStatus(500);
			var body = rsp.getContent().asString();
			assertTrue(body.contains("<@node> unknown attribute 'bogus'."), () -> body);
		}
	}

	@Test void c03_deepTab_resolvesFullPath() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(DeepHost.class);
			var rsp = c.get("/nav-deep").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertPage(body).isValid()
			.hasNavChildren("home/skill-repos", "alpha", "beta")
			.hasActiveNav("home", "skill-repos", "alpha");
	}

	@Test void c04_topLevelLeafTab() throws Exception {
		String body;
		try (var c = MockRestClient.buildLax(DeepHost.class);
			var rsp = c.get("/nav-leaf").run()) {
			rsp.assertStatus(200);
			body = rsp.getContent().asString();
		}
		assertPage(body).isValid().hasActiveNav("pagerduty").hasNavHref("pagerduty", "/pd");
	}
}
