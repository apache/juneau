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
import org.junit.jupiter.api.*;

/**
 * Golden-HTML tests for the {@code <@navigation>} / recursive {@code <@node>} chrome directives: one
 * {@code .juneau-page-nav} landmark emitted outside {@code <main>}, {@code aria-current} on the active
 * ancestor path (from the page's {@code tab=}), and {@code visible=false} nodes omitted with their
 * subtree.
 *
 * @since 10.0.0
 */
class NavigationDirective_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/nav-chrome.ftlh")
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
				.chromeTemplate("admin/nav-bogus-chrome.ftlh")
				.build();
		}
		@RestGet(path="/nav-bogus")
		public View navBogus() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	static int count(String body, String needle) {
		var n = 0;
		for (var i = body.indexOf(needle); i >= 0; i = body.indexOf(needle, i + needle.length()))
			n++;
		return n;
	}

	@Test void c01_nav_isChromeOutsideMain_highlightsPath_omitsHiddenNode() throws Exception {
		var body = MockRestClient.buildLax(Host.class).get("/nav").run()
			.assertStatus(200).getContent().asString();
		// Exactly one nav landmark, emitted BEFORE <main> (outside it), and it is not a card.
		assertEquals(1, count(body, "class=\"juneau-page-nav\""), () -> body);
		assertTrue(body.indexOf("juneau-page-nav") < body.indexOf("<main"), () -> body);
		assertFalse(body.contains("jc-card") && body.indexOf("jc-card") < body.indexOf("<main"), () -> body);
		// tab="home/skill-repos" -> aria-current on the ancestor (home) and the leaf (skill-repos).
		assertTrue(body.matches("(?s).*home.*aria-current=\"page\".*skill-repos.*aria-current=\"page\".*"), () -> body);
		// A sibling that is not on the active path is NOT highlighted.
		assertFalse(body.matches("(?s).*settings.*aria-current=\"page\".*"), () -> body);
		// visible=isAdmin with isAdmin=false -> the Setup node and its subtree are absent from the HTML.
		assertFalse(body.contains(">Setup<"), () -> body);
		assertFalse(body.contains(">Users<"), () -> body);
		assertFalse(body.contains("slds-"), () -> body);
	}

	@Test void c02_unknownNodeAttr_isRejected() throws Exception {
		var rsp = MockRestClient.buildLax(BogusHost.class).get("/nav-bogus").run();
		rsp.assertStatus(500);
		var body = rsp.getContent().asString();
		assertTrue(body.contains("unknown attribute") && body.contains("bogus"), () -> body);
	}
}
