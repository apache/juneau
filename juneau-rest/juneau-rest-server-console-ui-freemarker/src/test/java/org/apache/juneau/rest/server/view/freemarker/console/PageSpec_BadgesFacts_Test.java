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
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

/**
 * Header badges and page facts: {@code PageSpec}-seeded sets, the {@code <@badge>} and {@code <@facts>} twins, the
 * {@code <@console facts=>} attribute, badge-table validation and fact collisions.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class PageSpec_BadgesFacts_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/")
				.chromeTemplate("admin/console-chrome-bare.ftlh").build();
		}
		@RestGet(path="/happy")
		public View happy(RestRequest req) {
			return PageSpec.create()
				.html("c1", "<p>one</p>")
				.badge(BadgeDef.create("health").src("/rest/health"))
				.facts(Map.of("env", "prod"))
				.template("admin/page-badges-facts-happy.ftlh").view(req);
		}
		@RestGet(path="/conflict")
		public View conflict(RestRequest req) {
			return PageSpec.create().facts(Map.of("env", "prod"))
				.template("admin/page-badges-facts-conflict.ftlh").view(req);
		}
		@RestGet(path="/table-mismatch")
		public View tableMismatch(RestRequest req) {
			return PageSpec.create().template("admin/page-badges-table-mismatch.ftlh").view(req);
		}
		@RestGet(path="/bad-body")
		public View badBody(RestRequest req) {
			return PageSpec.create().template("admin/page-badges-bad-body.ftlh").view(req);
		}
		@RestGet(path="/nav-bad")
		public View navBad(RestRequest req) {
			return PageSpec.create().template("admin/page-badges-nav-bad.ftlh").view(req);
		}
		@RestGet(path="/nav-ok")
		public View navOk(RestRequest req) {
			return PageSpec.create().html("c1", "<p/>").template("admin/page-badges-nav-ok.ftlh").view(req);
		}
		@RestGet(path="/refreshes-bad")
		public View refreshesBad(RestRequest req) {
			return PageSpec.create().template("admin/page-badges-refreshes-bad.ftlh").view(req);
		}
		@RestGet(path="/facts-bad-type")
		public View factsBadType(RestRequest req) {
			return PageSpec.create().template("admin/page-badges-facts-bad-type.ftlh").view(req);
		}
		@RestGet(path="/java-refreshes")
		public View javaRefreshes(RestRequest req) {
			return PageSpec.create().html("c1", "<p/>")
				.badge(BadgeDef.create("rb").src("/rest/r").table("c1").refreshes("ghost")).view(req);
		}
		@RestGet(path="/nested-conflict")
		public View nestedConflict(RestRequest req) {
			return PageSpec.create().facts(Map.of("viewer", Map.of("roles", List.of("b"))))
				.template("admin/page-badges-facts-nested.ftlh").view(req);
		}
		@RestGet(path="/nested-merge")
		public View nestedMerge(RestRequest req) {
			return PageSpec.create().facts(Map.of("viewer", Map.of("name", "n")))
				.template("admin/page-badges-facts-nested.ftlh").view(req);
		}
		@RestGet(path="/ops")
		public View ops(RestRequest req) {
			return PageSpec.create().template("admin/page-badges-ops.ftlh").view(req);
		}
		@RestGet(path="/bad-op")
		public View badOp(RestRequest req) {
			return PageSpec.create().template("admin/page-badges-bad-op.ftlh").view(req);
		}
		@RestGet(path="/scope")
		public View scope(RestRequest req) {
			return PageSpec.create().html("c1", "<p/>").template("admin/page-badges-scope.ftlh").view(req);
		}
		@RestGet(path="/dup")
		public View dup(RestRequest req) {
			return PageSpec.create().badge(BadgeDef.create("queue").src("/a"))
				.template("admin/page-badges-facts-happy.ftlh").view(req);
		}
		@RestGet(path="/java-mismatch")
		public View javaMismatch(RestRequest req) {
			return PageSpec.create().html("c1", "<p/>")
				.badge(BadgeDef.create("health").src("/rest/health").table("no-such-card")).view(req);
		}
	}

	private static String get(String path, int status) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get(path).run()) {
			rsp.assertStatus(status);
			return rsp.getContent().asString();
		}
	}

	@Test void a01_badgesAndFacts_flowThroughRealRender() throws Exception {
		var contract = assertPage(get("/happy", 200)).isValid().contract();
		var badges = contract.getMap("header").getList("badges");
		assertEquals(2, badges.size());
		assertEquals("health", ((Map<?,?>)badges.get(0)).get("id"));
		var queue = (Map<?,?>)badges.get(1);
		assertEquals("queue", queue.get("id"));
		assertEquals("warning", queue.get("tone"));
		assertEquals(Map.of("one", "{total} queued"), queue.get("label"));
		assertEquals(Map.of("q", "1"), queue.get("params"));
		assertEquals(1, ((List<?>)queue.get("visibleWhen")).size());
		var facts = contract.getMap("facts");
		assertEquals("prod", facts.getString("env"));
		assertEquals("1.0", facts.getString("buildVersion"));
	}

	@Test void b01_factsJavaVsChrome_sameLeaf_isRejected() throws Exception {
		var body = get("/conflict", 500);
		assertTrue(body.contains("Fact 'env' is set by both PageSpec.facts and the chrome; set it in one place."), body);
	}

	@Test void b02_badgeTableMismatch_isRejected() throws Exception {
		var body = get("/table-mismatch", 500);
		assertTrue(body.contains("<@badge id='health'> references card 'no-such-card', which is not on the page."), body);
	}

	@Test void b03_badgeBodyUnknownKey_isRejected() throws Exception {
		var body = get("/bad-body", 500);
		assertTrue(body.contains("body only supports 'visibleWhen', 'scopeValues' and 'params' keys; found 'tone'."), body);
	}

	@Test void b04_duplicateBadgeId_isRejected() throws Exception {
		var body = get("/dup", 500);
		assertTrue(body.contains("Badge id 'queue' is declared twice"), body);
	}

	@Test void b05_javaOnlyPage_tableMismatch_failsEarly() throws Exception {
		var body = get("/java-mismatch", 500);
		assertTrue(body.contains("&lt;@badge id='health'&gt; references card 'no-such-card', which is not on the page."), body);
	}

	@Test void c01_badLeafType_isRejected() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> PageSpec.create().facts(Map.of("owner", new java.util.concurrent.atomic.AtomicInteger(1))));
		assertTrue(e.getMessage().contains("Fact 'owner' has unsupported value type 'AtomicInteger'"), e.getMessage());
	}

	@Test void c02_nestedLeafType_namesDottedPath() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> PageSpec.create().facts(Map.of("viewer", Map.of("roles", List.of("oncall", 42)))));
		assertTrue(e.getMessage().contains("Fact 'viewer.roles'"), e.getMessage());
	}

	@Test void c03_allAllowedShapes_accepted() {
		assertDoesNotThrow(() -> PageSpec.create().facts(Map.of("env", "prod", "replicas", 3, "stable", true,
			"roles", List.of("oncall"), "nested", Map.of("k", "v"))));
	}

	@Test void c04_badgeValidate_runsAtSetter() {
		assertThrows(IllegalArgumentException.class, () -> PageSpec.create().badge(BadgeDef.create("x")));
	}

	@Test void d01_navScopedBadge_unknownNavKey_isE68() throws Exception {
		var body = get("/nav-bad", 500);
		assertTrue(body.contains("<@badge id='n1'> scope names nav node 'nope', which is not in the nav tree."), body);
	}

	@Test void d02_navScopedBadge_tableIsACardId_notANavId() throws Exception {
		var contract = assertPage(get("/nav-ok", 200)).isValid().contract();
		assertEquals("c1", ((Map<?,?>)contract.getMap("header").getList("badges").get(0)).get("table"));
	}

	@Test void d03_refreshesUnknownCard_isE67() throws Exception {
		var body = get("/refreshes-bad", 500);
		assertTrue(body.contains("<@badge id='r1'> references card 'ghost', which is not on the page."), body);
	}

	@Test void d04_javaPath_refreshesUnknownCard_isE67() throws Exception {
		var body = get("/java-refreshes", 500);
		assertTrue(body.contains("&lt;@badge id='rb'&gt; references card 'ghost', which is not on the page."), body);
	}

	@Test void e01_ftlFacts_badLeafType_isRejected() throws Exception {
		var body = get("/facts-bad-type", 500);
		assertTrue(body.contains("Fact 'owner' has unsupported value type 'null'"), body);
	}

	@Test void e02_facts_areDeepCopied() {
		var roles = new ArrayList<String>(List.of("a"));
		var nested = new HashMap<String,Object>(Map.of("roles", roles));
		var spec = PageSpec.create().facts(Map.of("viewer", nested));
		roles.add("b");
		nested.put("extra", "x");
		@SuppressWarnings("unchecked")
		var viewer = (Map<String,Object>)spec.facts.get("viewer");
		assertEquals(Map.of("roles", List.of("a")), viewer);
		assertThrows(UnsupportedOperationException.class, () -> viewer.put("k", "v"));
	}

	@Test void e03_nullRules_areRejected() {
		assertThrows(IllegalArgumentException.class, () -> CardSpec.html("c").visibleWhen((VisibilityRule[])null));
		assertThrows(IllegalArgumentException.class, () -> CardSpec.html("c").visibleWhen((VisibilityRule)null));
	}

	@SuppressWarnings("unchecked")
	@Test void e04_wiringFrom_replacesVisibleWhen() {
		var a = CardSpec.html("c").visibleWhen(VisibilityRule.when("a").present());
		var b = CardSpec.html("c").visibleWhen(VisibilityRule.when("b").present());
		a.wiringFrom(b);
		assertEquals("b", ((List<Map<String,Object>>)a.toMap().get("visibleWhen")).get(0).get("field"));
		assertEquals(1, ((List<?>)a.toMap().get("visibleWhen")).size());
		a.wiringFrom(CardSpec.html("c"));
		assertEquals(1, ((List<?>)a.toMap().get("visibleWhen")).size(), "an empty other leaves the rules alone");
	}

	@Test void f01_nestedFactsCollision_namesDottedPath() throws Exception {
		var body = get("/nested-conflict", 500);
		assertTrue(body.contains("Fact 'viewer.roles' is set by both PageSpec.facts and the chrome; set it in one place."), body);
	}

	@Test void f02_nestedFacts_disjointLeaves_deepMerge() throws Exception {
		var facts = assertPage(get("/nested-merge", 200)).isValid().contract().getMap("facts");
		assertEquals(Map.of("name", "n", "roles", List.of("a")), facts.get("viewer"));
	}

	@Test void g01_visibleWhenOps_inContainsPresent() throws Exception {
		var contract = assertPage(get("/ops", 200)).isValid().contract();
		var rules = (List<?>)((Map<?,?>)contract.getMap("header").getList("badges").get(0)).get("visibleWhen");
		assertEquals(List.of(
			Map.of("field", "role", "op", "in", "value", List.of("a", "b")),
			Map.of("field", "roles", "op", "contains", "value", "x"),
			Map.of("field", "z", "op", "present")), rules);
	}

	@Test void g02_visibleWhenUnknownOp_isRejected() throws Exception {
		var body = get("/bad-op", 500);
		assertTrue(body.contains("visibleWhen op 'like' is not one of 'eq, ne, present, absent, in, contains'."), body);
	}

	@Test void g03_scopeAttrs_withBodyScopeValuesAndParams() throws Exception {
		var contract = assertPage(get("/scope", 200)).isValid().contract();
		var b = (Map<?,?>)contract.getMap("header").getList("badges").get(0);
		assertEquals(Map.of("param", "kind", "by", "view", "values", Map.of("c1", List.of("x", "y"))), b.get("scope"));
		assertEquals(Map.of("q", "1"), b.get("params"));
		assertEquals(List.of("c1"), b.get("refreshes"));
		assertEquals("state", ((Map<?,?>)b.get("stateFilter")).get("column"));
	}
}
