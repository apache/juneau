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
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Path;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.test.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Proves a {@link PageSpec}-built page and the equivalent hand-written FTL page produce the same
 * {@code #juneau-page} contract and the same {@code <template>} set, for every feature the page builder exposes.
 *
 * <p>
 * Each case is one FTL fixture under {@code templates/parity/c3} and one {@link Cases} factory; both render through
 * the shared {@code parity-chrome.ftlh}.  The comparison is on key-sorted JSON (list order is kept, because card, nav
 * and template order is part of what parity must prove), then on template ids and exact template markup.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	var <jv>ftl</jv> = <jsm>assertPage</jsm>(<jsm>renderFtl</jsm>(<js>"03-table-every-feature"</js>)).isValid();
 * 	var <jv>java</jv> = <jsm>assertPage</jsm>(<jsm>renderSpec</jsm>(<js>"03-table-every-feature"</js>)).isValid();
 * 	<jsm>assertEquals</jsm>(<jsm>canonical</jsm>(<jv>ftl</jv>.contract()), <jsm>canonical</jsm>(<jv>java</jv>.contract()));
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient/RestResponse are closed in try-with-resources; fluent assertStatus returns this.
})
class PageSpec_Parity_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().basePath("/templates/")
				.chromeTemplate("parity/c3/parity-chrome.ftlh").cardType(new KpiCardType()).build();
		}
		@RestGet(path="/ftl/{name}")
		public FreemarkerView ftl(@Path String name) {
			return FreemarkerView.of("parity/c3/" + name + ".ftlh");
		}
		@RestGet(path="/spec/{name}")
		public View spec(@Path String name, RestRequest req) {
			var f = CASES.get(name);
			if (f == null)
				throw new IllegalArgumentException("No such case: '" + name + "'.");
			return f.apply(req);
		}
	}

	private static final Map<String,Function<RestRequest,View>> CASES = Map.ofEntries(
		Map.entry("01-tab-only", Cases::tabOnly),
		Map.entry("02-html-cards-and-segments", Cases::htmlCardsAndSegments),
		Map.entry("03-table-every-feature", Cases::tableEveryFeature),
		Map.entry("04-ribbon-every-type", Cases::ribbonEveryType),
		Map.entry("05-detail-plus-region", Cases::detailPlusRegion),
		Map.entry("06-rowclass-every-op", Cases::rowClassEveryOp),
		Map.entry("07-row-actions-enabled-when", Cases::rowActionsEnabledWhen),
		Map.entry("08-searchable-column-operators", Cases::searchableColumnOperators),
		Map.entry("09-server-data-mode", Cases::serverDataMode),
		Map.entry("10-quick-stats", Cases::quickStats),
		Map.entry("11-card-ref", Cases::cardRefMiddle),
		Map.entry("12-unplaced-appended", Cases::cardRefUnplacedAppend),
		Map.entry("13-navunder-depth3", Cases::navUnderDepth3),
		Map.entry("14-theme", Cases::theme),
		Map.entry("15-custom-card-type", Cases::customCardType),
		Map.entry("16-csrf-body-attrs", Cases::csrfBodyAttrs),
		Map.entry("17-badge-vs-directive", Cases::badgeVsDirective),
		Map.entry("18-facts-deep-merge", Cases::factsDeepMerge),
		Map.entry("19-visible-when-passthrough", Cases::visibleWhenPassthrough),
		Map.entry("20-theme-inherit", Cases::themeInherit));

	private static String get(String path) throws Exception {
		try (var c = MockRestClient.buildLax(Host.class); var rsp = c.get(path).run()) {
			var body = rsp.getContent().asString();
			assertEquals(200, rsp.getStatusCode(), () -> path + ": " + body);
			return body;
		}
	}

	/** Recursively key-sorts every {@link Map}; {@link List} order is kept. */
	private static Object canonical(Object v) {
		if (v instanceof Map<?,?> m) {
			var out = new TreeMap<String,Object>();
			for (var e : m.entrySet())
				out.put(String.valueOf(e.getKey()), canonical(e.getValue()));
			return out;
		}
		if (v instanceof List<?> l)
			return l.stream().map(PageSpec_Parity_Test::canonical).toList();
		return v;
	}

	private static void assertParity(String ftlFixture, String specCase, Consumer<PageContractAssert> javaGuard) throws Exception {
		var ftl = assertPage(get("/ftl/" + ftlFixture)).isValid();
		var java = assertPage(get("/spec/" + specCase)).isValid();
		javaGuard.accept(java);  // Non-vacuity: the Java side really produced the thing being compared.
		var diff = firstDiff("$", canonical(ftl.contract()), canonical(java.contract()));
		assertNull(diff, () -> "case '" + specCase + "' contract mismatch (ftl vs java) at " + diff);
		assertEquals(trimmed(ftl.templates()), trimmed(java.templates()), () -> "case '" + specCase + "' template mismatch");
	}

	/** @return A description of the first place the two canonical trees differ (FTL first), or {@code null}. */
	private static String firstDiff(String path, Object ftl, Object java) {
		if (Objects.equals(ftl, java))
			return null;
		if (ftl instanceof Map<?,?> a && java instanceof Map<?,?> b) {
			var keys = new TreeSet<Object>(a.keySet());
			keys.addAll(b.keySet());
			for (var k : keys) {
				var d = firstDiff(path + "." + k, a.get(k), b.get(k));
				if (d != null)
					return d;
			}
		}
		if (ftl instanceof List<?> a && java instanceof List<?> b && a.size() == b.size())
			for (var i = 0; i < a.size(); i++) {
				var d = firstDiff(path + "[" + i + "]", a.get(i), b.get(i));
				if (d != null)
					return d;
			}
		return path + ": ftl=" + ftl + " java=" + java;
	}

	/** Template markup keyed by id, with the whitespace an FTL author's layout puts around a card stripped. */
	private static Map<String,String> trimmed(Map<String,String> templates) {
		var out = new TreeMap<String,String>();
		templates.forEach((k, v) -> out.put(k, v.strip()));
		return out;
	}

	private static void assertParity(String caseName, Consumer<PageContractAssert> javaGuard) throws Exception {
		assertParity(caseName, caseName, javaGuard);
	}

	/** A guard for a table case: the Java side has card {@code id} of type datatables, with the given key present. */
	private static Consumer<PageContractAssert> table(String id, String... tableKeys) {
		return a -> {
			a.hasCard(id, "datatables");
			for (var k : tableKeys)
				assertNotNull(a.card(id).getMap("table").get(k), () -> "Java table '" + id + "' has no '" + k + "'");
		};
	}

	/** Java templates carry no unresolved {@code ref=}, and the cards are in the stated order. */
	private static Consumer<PageContractAssert> placed(String... order) {
		return a -> {
			a.hasCardOrder(order);
			a.templates().forEach((k, v) -> assertFalse(java.util.regex.Pattern.compile("<@card[^>]*\bref=").matcher(v).find(), () -> "template '" + k + "' has an unresolved ref: " + v));
		};
	}

	@Test void case01_tabOnly() throws Exception { assertParity("01-tab-only", a -> a.hasActiveNav("activity", "today")); }
	@Test void case02_htmlCardsAndSegments() throws Exception { assertParity("02-html-cards-and-segments", a -> a.hasCardOrder("jc-seg-1", "intro")); }
	@Test void case03_tableEveryFeature() throws Exception { assertParity("03-table-every-feature", table("slo", "columns", "ribbon", "rowClassRules", "rowActions", "detail", "defaultOrder")); }
	@Test void case04_ribbonEveryType() throws Exception { assertParity("04-ribbon-every-type", table("t", "ribbon")); }
	@Test void case05_detailPlusRegion() throws Exception { assertParity("05-detail-plus-region", table("t", "detail")); }
	@Test void case06_rowClassEveryOp() throws Exception { assertParity("06-rowclass-every-op", table("t", "rowClassRules")); }
	@Test void case07_rowActionsEnabledWhen() throws Exception { assertParity("07-row-actions-enabled-when", table("t", "rowActions")); }
	@Test void case08_searchableColumnOperators() throws Exception { assertParity("08-searchable-column-operators", table("t", "columns")); }
	@Test void case09_serverDataMode() throws Exception { assertParity("09-server-data-mode", table("t", "dataMode")); }
	@Test void case10_quickStats() throws Exception { assertParity("10-quick-stats", table("t", "quickStats")); }
	@Test void case11_cardRefMiddle() throws Exception { assertParity("11-card-ref-allftl", "11-card-ref", placed("jc-seg-1", "slo", "jc-seg-2")); }
	@Test void case12_unplacedAppendOrder() throws Exception { assertParity("12-unplaced-appended-allftl", "12-unplaced-appended", placed("jc-seg-1", "first", "second")); }
	@Test void case13_navUnderDepth3() throws Exception { assertParity("13-navunder-depth3", a -> a.hasActiveNav("fleet", "instances", "i-1", "logs")); }
	@Test void case14_theme() throws Exception { assertParity("14-theme", a -> a.hasTheme("light-red")); }
	@Test void case15_customCardType() throws Exception { assertParity("15-custom-card-type", a -> a.hasCard("slo-trend", "kpi")); }
	@Test void case16_csrfBodyAttrs() throws Exception { { assertParity("16-csrf-body-attrs", a -> {}); var html = get("/spec/16-csrf-body-attrs"); assertTrue(html.contains("data-app-flag") && html.contains("tok-123"), html); }; }
	@Test void case17_badgeVsDirective() throws Exception { assertParity("17-badge-vs-directive", a -> assertNotNull(a.contract().getMap("header").get("badges"), "header.badges")); }
	@Test void case18_factsDeepMerge() throws Exception { assertParity("18-facts-deep-merge", a -> assertNotNull(a.contract().get("facts"), "facts")); }
	@Test void case20_themeInherit() throws Exception { assertParity("20-theme-inherit", a -> assertEquals("open", a.contract().getMap("theme").get("name"))); }
	@Test void case19_visibleWhenPassthrough() throws Exception { assertParity("19-visible-when-passthrough", table("t", "rowActions", "detail")); }

	/** The Java side of every case.  Every method leaves {@code template(...)} unset (the chrome runs
	 * {@code <@console>} directly) except {@link #cardRefMiddle} and {@link #cardRefUnplacedAppend}, where a page
	 * template is the only way to place a built card. */
	static final class Cases {
		private Cases() {}

		private static final SearchOperatorSet STD = SearchOperatorSet.standard();

		static View tabOnly(RestRequest req) {
			return PageSpec.create().tab("activity/today").view(req);
		}

		static View htmlCardsAndSegments(RestRequest req) {
			return PageSpec.create()
				// jc-seg-1 is the id the FTL side auto-generates for its first bare segment.
				.html("jc-seg-1", "<p class=\"free-segment\">A bare segment, auto-carded.</p>", c -> c.bare(true))
				.html("intro", "Welcome &mdash; this is an explicit html card.")
				.view(req);
		}

		static View tableEveryFeature(RestRequest req) {
			var t = TableSpec.create("slo")
				.dataUrl("/rest/slo/data")
				.defaultOrder("pod", TableSpec.Sort.ASC)
				.columns(
					Column.create("pod").label("Pod").className("ssc-id-cell"),
					Column.create("incidentKey").label("Incident").render("linked").href("{incidentUrl}"),
					Column.create("rootCause").label("Root cause").defaultVisible(false))
				.ribbon(RibbonItem.refresh(), RibbonItem.export("copy", "csv"))
				.rowClass("analysisState", Op.EQ, "degraded", "row-degraded")
				.rowActions(RowAction.create("analyze").label("Analyze pod")
					.endpoint("/rest/slo/analyze").method(RowAction.Method.POST))
				.detail(RowDetail.create("/rest/slo/data/{id}").title("{pod}")
					.region(RegionDef.create("slo-detail").populate("slo-detail").lazy(false)));
			return PageSpec.create().toolkit("views").table(t).view(req);
		}

		static View ribbonEveryType(RestRequest req) {
			var t = TableSpec.create("t").dataUrl("/rest/t/data").columns(Column.create("id").label("Id"))
				.ribbon(
					RibbonItem.refresh(),
					RibbonItem.export("copy", "csv", "print"),
					RibbonItem.collapseAll(),
					RibbonItem.pausePolling(),
					RibbonItem.dialog("add").title("Add repo").form("/rest/t/add-modal")
						.endpoint("/rest/t/new").method(RowAction.Method.POST).onSuccess(RowAction.OnSuccess.REDRAW));
			return PageSpec.create().toolkit("views").table(t).view(req);
		}

		static View detailPlusRegion(RestRequest req) {
			var t = TableSpec.create("t").dataUrl("/rest/t/data").columns(Column.create("id").label("Id"))
				.detail(RowDetail.create("/rest/t/data/{id}").title("{id}").icon("search")
					.region(RegionDef.create("t-detail").populate("t-detail").lazy(false)));
			return PageSpec.create().toolkit("views").table(t).view(req);
		}

		static View rowClassEveryOp(RestRequest req) {
			var t = TableSpec.create("t").dataUrl("/rest/t/data").columns(Column.create("id").label("Id"))
				.rowClass("state", Op.EQ, "degraded", "row-degraded")
				.rowClass("state", Op.NE, "ok", "row-not-ok")
				.rowClass("owner", Op.PRESENT, "row-owned")
				.rowClass("owner", Op.ABSENT, "row-unowned");
			return PageSpec.create().toolkit("views").table(t).view(req);
		}

		static View rowActionsEnabledWhen(RestRequest req) {
			var t = TableSpec.create("t").dataUrl("/rest/t/data").columns(Column.create("id").label("Id"))
				.rowActions(RowAction.create("close").label("Close").endpoint("/rest/t/close").method(RowAction.Method.POST)
					.enabledWhen("state", Op.NE, "closed", "Already closed"));
			return PageSpec.create().toolkit("views").table(t).view(req);
		}

		static View searchableColumnOperators(RestRequest req) {
			var t = TableSpec.create("t").dataUrl("/rest/t/data")
				.columns(Column.create("state").label("State").searchType(SearchType.ENUM)
					.operators(SearchOperatorSet.of(STD.get("$eq"), STD.get("$ne"), STD.get("$in"))));
			return PageSpec.create().toolkit("views").table(t).view(req);
		}

		static View serverDataMode(RestRequest req) {
			var t = TableSpec.create("t").dataUrl("/rest/t/data").dataMode(TableSpec.DataMode.SERVER)
				.columns(Column.create("id").label("Id"));
			return PageSpec.create().toolkit("views").table(t).view(req);
		}

		static View quickStats(RestRequest req) {
			var t = TableSpec.create("t").dataUrl("/rest/t/data").columns(Column.create("id").label("Id"))
				.quickStats("work-unavailable",
					QuickStat.of("unavailable", "Work data", "unavailable").tone(QuickStat.Tone.ERROR));
			return PageSpec.create().toolkit("views").table(t).view(req);
		}

		static View cardRefMiddle(RestRequest req) {
			return PageSpec.create().template("parity/c3/11-card-ref.ftlh")
				.html("slo", "An inline card, placed exactly where the Java side places it with ref=.").view(req);
		}

		static View cardRefUnplacedAppend(RestRequest req) {
			return PageSpec.create().template("parity/c3/12-unplaced-appended.ftlh")
				.html("first", "first").html("second", "second").view(req);
		}

		static View navUnderDepth3(RestRequest req) {
			return PageSpec.create().tab("fleet/instances/i-1/logs")
				.navUnder("fleet/instances", n -> {
					var i1 = n.add("i-1", "i-1", "/fleet/instances/i-1");
					i1.add("overview", "Overview", "/fleet/instances/i-1");
					i1.add("logs", "Logs", "/fleet/instances/i-1/logs");
				}).view(req);
		}

		static View theme(RestRequest req) {
			return PageSpec.create().theme("light-red").view(req);
		}

		static View themeInherit(RestRequest req) {
			return PageSpec.create().view(req);
		}

		static View customCardType(RestRequest req) {
			return PageSpec.create().card(CardSpec.of("kpi", "slo-trend").title("Alerts per day").body("value", 42)).view(req);
		}

		static View csrfBodyAttrs(RestRequest req) {
			return PageSpec.create().csrf("tok-123", "X-CSRF").bodyAttr("data-app-flag", "on").view(req);
		}

		static View badgeVsDirective(RestRequest req) {
			return PageSpec.create()
				.badge(BadgeDef.create("health").src("/rest/health").refreshMs(30_000)).view(req);
		}

		static View factsDeepMerge(RestRequest req) {
			return PageSpec.create().facts(Map.of("build", Map.of("version", "1.0"))).view(req);
		}

		static View visibleWhenPassthrough(RestRequest req) {
			var t = TableSpec.create("t").dataUrl("/rest/t/data").columns(Column.create("id").label("Id"))
				.rowActions(RowAction.create("close").label("Close").endpoint("/rest/t/close").method(RowAction.Method.POST)
					.tone(RowAction.Tone.DANGER).confirmLabel("Really close?").confirmRenderer("markdown")
					.visibleWhen(VisibilityRule.when("state").ne("closed")))
				.detail(RowDetail.create("/rest/t/data/{id}").title("{id}")
					.region(RegionDef.create("t-detail").populate("t-detail").lazy(false)
						.visibleWhen(VisibilityRule.when("viewer.roles").contains("oncall"))));
			return PageSpec.create().toolkit("views").table(t).view(req);
		}
	}
}
