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
 * Tests for {@code <@node under="...">}: a page-scoped nav addition appended under an existing chrome nav node at
 * {@code </@console>}, before {@code activeNav} resolves.  Covers the happy path (any depth, deferred
 * {@code selected=true}), E-B14 (wrong position), E-B8 (unknown {@code under=} path), E-3 (duplicate sibling under
 * the resolved parent) and E-6 (double selection within the subtree).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class NodeDirective_Under_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-nav-under.ftlh")
				.build();
		}
		@RestGet(path="/happy")
		public View happy() {
			return FreemarkerView.of("admin/page-nav-under-happy.ftlh");
		}
		@RestGet(path="/unknown")
		public View unknown() {
			return FreemarkerView.of("admin/page-nav-under-unknown.ftlh");
		}
		@RestGet(path="/duplicate")
		public View duplicate() {
			return FreemarkerView.of("admin/page-nav-under-duplicate.ftlh");
		}
		@RestGet(path="/double-selected")
		public View doubleSelected() {
			return FreemarkerView.of("admin/page-nav-under-double-selected.ftlh");
		}
		@RestGet(path="/empty")
		public View empty() {
			return FreemarkerView.of("admin/page-nav-under-empty.ftlh");
		}
		@RestGet(path="/order")
		public View order(RestRequest req) {
			return PageSpec.create().navUnder("fleet", n -> n.add("a", "A", "/fleet/a"))
				.template("admin/page-nav-under-order.ftlh").view(req);
		}
		@RestGet(path="/forward")
		public View forward(RestRequest req) {
			return PageSpec.create().navUnder("fleet", n -> n.add("x", "X", "/fleet/x"))
				.template("admin/page-nav-under-forward.ftlh").view(req);
		}
		@RestGet(path="/reverse")
		public View reverse(RestRequest req) {
			return PageSpec.create().navUnder("fleet/z", n -> n.add("w", "W", "/fleet/z/w"))
				.template("admin/page-nav-under-reverse.ftlh").view(req);
		}
		@RestGet(path="/in-card")
		public View inCard() {
			return FreemarkerView.of("admin/page-nav-under-in-card.ftlh");
		}
	}

	// A distinct chrome with an illegal <@node under> literally inside <@navigation>, to exercise E-B14.
	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class BogusHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-nav-under-bogus.ftlh")
				.build();
		}
		@RestGet(path="/inside-navigation")
		public View insideNavigation() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class SlotHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-nav-under-slot.ftlh")
				.build();
		}
		@RestGet(path="/page")
		public View page() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	// under= directly inside <@console>, after a closed </@navigation>.
	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class DirectHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-nav-under-direct.ftlh")
				.build();
		}
		@RestGet(path="/page")
		public View page() {
			return FreemarkerView.of("admin/page-naked.ftlh");
		}
	}

	// A chrome that already selected a node, so a selected=true under= subtree hits E-6.
	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class SelectedHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create()
				.basePath("/templates/")
				.chromeTemplate("admin/console-nav-under-selected.ftlh")
				.build();
		}
		@RestGet(path="/happy")
		public View happy() {
			return FreemarkerView.of("admin/page-nav-under-happy.ftlh");
		}
	}

	private static String get(Class<?> host, String path, int status) throws Exception {
		try (var c = MockRestClient.buildLax(host);
			var rsp = c.get(path).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		}
	}

	@Test void f01_happyPath_unionAnyDepth_deferredSelection() throws Exception {
		var body = get(Host.class, "/happy", 200);
		assertPage(body).isValid()
			.hasNavChildren("fleet/instances", "logs")
			.hasNavChildren("fleet/instances/logs", "overview")
			.hasNavHref("fleet/instances/logs/overview", "/fleet/instances/logs/overview")
			.hasActiveNav("fleet", "instances", "logs", "overview");
	}

	@Test void f02_insideNavigation_isEB14() throws Exception {
		var body = get(BogusHost.class, "/inside-navigation", 500);
		assertTrue(body.contains(
			"<@node id='extra' under='fleet'> must be a direct child of <@page> or <@console>, outside <@navigation>."), () -> body);
	}

	@Test void f03_insideCard_isEB14() throws Exception {
		var body = get(Host.class, "/in-card", 500);
		assertTrue(body.contains(
			"<@node id='extra' under='fleet'> must be a direct child of <@page> or <@console>, outside <@navigation>."), () -> body);
	}

	@Test void f04_unknownUnderPath_isEB8() throws Exception {
		var body = get(Host.class, "/unknown", 500);
		assertTrue(body.contains(
			"Nav addition under 'nope' does not match a <@node> path in the chrome; known paths: 'fleet, fleet/instances, pagerduty'."), () -> body);
	}

	@Test void f05_duplicateSibling_isE3() throws Exception {
		var body = get(Host.class, "/duplicate", 500);
		assertTrue(body.contains("<@node id='instances'> duplicates a sibling id under 'fleet'."), () -> body);
	}

	@Test void f06_doubleSelectedWithinSubtree_isRejected() throws Exception {
		var body = get(Host.class, "/double-selected", 500);
		assertTrue(body.contains("<@node id='logs'> selected=true but 'fleet/instances/logs/overview' is already selected."), () -> body);
	}

	@Test void f07_inConsoleSlot_isEB14() throws Exception {
		var body = get(SlotHost.class, "/page", 500);
		assertTrue(body.contains(
			"<@node id='extra' under='fleet'> must be a direct child of <@page> or <@console>, outside <@navigation>."), () -> body);
	}

	@Test void f08_emptyUnder_isRejected() throws Exception {
		var body = get(Host.class, "/empty", 500);
		assertTrue(body.contains("<@node id='extra'> under must name a '/'-separated nav id path; got ''."), () -> body);
	}

	@Test void f09_javaThenFtl_childOrderIsDeclarationOrder() throws Exception {
		var body = get(Host.class, "/order", 200);
		assertPage(body).isValid().hasNavChildren("fleet", "instances", "a", "b");
	}

	@Test void f10_ftlCanTargetNodeCreatedByEarlierJavaAdd() throws Exception {
		var body = get(Host.class, "/forward", 200);
		assertPage(body).isValid().hasNavChildren("fleet/x", "y");
	}

	@Test void f11_javaCannotTargetNodeCreatedByLaterFtlAdd_isEB8() throws Exception {
		var body = get(Host.class, "/reverse", 500);
		assertTrue(body.contains("Nav addition under 'fleet/z' does not match a <@node> path in the chrome;"), () -> body);
	}

	@Test void f12_underDirectlyInConsole_afterClosedNavigation_isAccepted() throws Exception {
		var body = get(DirectHost.class, "/page", 200);
		assertPage(body).isValid().hasNavChildren("fleet", "c");
	}

	@Test void f13_selectedUnderSubtree_whenChromeAlreadySelected_isE6() throws Exception {
		var body = get(SelectedHost.class, "/happy", 500);
		assertTrue(body.contains("<@node id='overview'> selected=true but 'pagerduty' is already selected."), () -> body);
	}
}
