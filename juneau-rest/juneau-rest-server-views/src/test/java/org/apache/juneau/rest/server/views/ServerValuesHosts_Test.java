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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Serve-time {@code $FV} resolution for the {@link PageDef} and {@link RowDetailDef} hosts, alongside the shipped
 * {@link ViewDef} host: the two closed chrome allowlists, per-host isolation, absence of inheritance, LIFO restore
 * fidelity on the shared definitions, fail-soft / fail-closed, and the expand envelope staying chrome-free.
 *
 * <p>
 * Every definition below is a <b>shared static instance</b>, which is the realistic application pattern and the one
 * that exercises the mutate-and-restore window rather than a fresh per-request object graph.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
})
class ServerValuesHosts_Test extends TestBase {

	/** Reads the {@code env} query param so each response resolves to its own value. */
	private static ServerValues envValues(String suffix) {
		return ServerValues.create()
			.value("env", s -> s.getBean(RestRequest.class).map(r -> r.getQueryParam("env").orElse("?")).orElse("?")
				+ suffix);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Shared definitions: a page host, a row-detail host, and a view host, all live in one response.
	//------------------------------------------------------------------------------------------------------------------

	/** The row-detail host: {@code title} / {@code DetailSection.title} / {@code DetailField.title} are on the allowlist. */
	static final RowDetailDef DETAIL = RowDetailDef.create()
		.endpoint("/alerts/{id}/detail")
		.title("D-title:$FV{env}")
		.icon("D-icon:$FV{env}")           // NOT on the allowlist - must stay literal
		.sections(
			DetailSection.create("main", "D-section:$FV{env}")
				.fields(DetailField.of("k").title("D-field:$FV{env}")))
		.serverValues(envValues("/DETAIL"));

	/** The view host (the shipped v1 host), which must resolve independently of the page host. */
	static final ViewDef ALERTS = ViewDef.create("alerts")
		.columns(Column.of("name").title("V-col:$FV{env}"))
		.details(DETAIL)
		.serverValues(envValues("/VIEW"))
		.build();

	/** A page-hosted view that declares NO {@code serverValues}: it must not inherit the page's. */
	static final ViewDef ORPHAN = ViewDef.create("orphan")
		.columns(Column.of("name").title("O-col:$FV{env}"))
		.build();

	private static AppHeaderDef header() {
		return AppHeaderDef.create("app")
			.brand(Brand.create().logo(true).title("H-brand:$FV{env}").crumbs("H-crumb1:$FV{env}", "H-crumb2:$FV{env}"))
			.actions(
				HeaderAction.link("docs", "table", "H-tooltip:$FV{env}", "/docs"),
				// MenuItem.label is deliberately NOT on the allowlist - it must stay literal.
				HeaderAction.menu("more", "table", "H-menutip:$FV{env}")
					.menu(MenuItem.link("one", "H-menuitem:$FV{env}", "/one")))
			.avatar(AvatarChip.of("H-avatar:$FV{env}").initials("H-initials:$FV{env}"))
			.build();
	}

	private static BarSlot barSlot() {
		return BarSlot.create("ctx")
			.widgets(
				BarText.of("mode", "B-text:$FV{env}"),
				BarBadge.of("pending").label("B-label:$FV{env}").badge(Badge.count(3).tone(Tone.WARN)));
	}

	/** The one shared page instance carrying all three hosts. */
	static final PageDef PAGE = PageDef.create("admin")
		.title("P-title:$FV{env}")
		.tabs(
			Tab.create("alerts", "P-tab:$FV{titleProbe}").view(ALERTS),
			Tab.create("catalog", "P-tab2:$FV{env}").subtabs(
				Subtab.create("orphan", "P-subtab:$FV{env}").view(ORPHAN)))
		.header(header())
		.barSlot(barSlot())
		.serverValues(envValues("/PAGE")
			// PageDef.title is on the allowlist but the shell paints no title node, so the only way to observe its
			// resolved value is from inside the same resolve window.  Field order puts title before the tab labels.
			.value("titleProbe", s -> ServerValuesHosts_Test.PAGE.title))
		.build();

	//------------------------------------------------------------------------------------------------------------------
	// Hosts that fail closed.
	//------------------------------------------------------------------------------------------------------------------

	static final ViewDef THROWER_VIEW = ViewDef.create("boom")
		.columns(Column.of("name").title("Name"))
		.build();

	static final PageDef THROWING_PAGE = PageDef.create("boom-page")
		.title("T:$FV{bad}")
		.tabs(Tab.create("t", "T").view(THROWER_VIEW))
		.serverValues(ServerValues.create().value("bad", s -> {
			throw new IllegalStateException("provider blew up");
		}))
		.build();

	static final RowDetailDef THROWING_DETAIL = RowDetailDef.create()
		.endpoint("/boom/{id}")
		.title("T:$FV{bad}")
		.sections(DetailSection.create("main", "S").fields(DetailField.of("k")))
		.serverValues(ServerValues.create().value("bad", s -> {
			throw new IllegalStateException("provider blew up");
		}));

	static final ViewDef THROWING_DETAIL_VIEW = ViewDef.create("boom-detail")
		.columns(Column.of("name").title("Name"))
		.details(THROWING_DETAIL)
		.build();

	//------------------------------------------------------------------------------------------------------------------
	// Host servlet.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(mixins=ViewsMixin.class)
	public static class ServerValuesHostsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public VarResolver varResolver(VarResolver.Builder b) {
			return b.vars(ServerValuesVar.class).build();
		}

		@RestGet(path="/page") public HttpResource page(RestRequest req) {
			return html(Html.of(PageTable.of(req, PAGE)));
		}

		/** The row-detail host reached standalone (no enclosing page). */
		@RestGet(path="/view") public HttpResource view(RestRequest req) {
			return html(Html.of(ViewTable.of(req, ALERTS)));
		}

		/** The app-owned expand GET: data only, no chrome (umbrella decision 9). */
		@RestGet(path="/alerts/{id}/detail") public Map<String,Object> detail(@Path("id") String id) {
			var out = new LinkedHashMap<String,Object>();
			out.put("contractVersion", RowDetailDef.CONTRACT_VERSION);
			out.put("fields", Map.of("k", "row-value-" + id));
			return out;
		}

		@RestGet(path="/page-throws") public HttpResource pageThrows(RestRequest req) {
			return html(Html.of(PageTable.of(req, THROWING_PAGE)));
		}

		@RestGet(path="/detail-throws") public HttpResource detailThrows(RestRequest req) {
			return html(Html.of(ViewTable.of(req, THROWING_DETAIL_VIEW)));
		}

		private static HttpResource html(String markup) {
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(ServerValuesHostsHost.class);

	private static String body(String path) throws Exception {
		return c.get(path).run().assertStatus(200).getContent().asString();
	}

	/** Every author {@code $FV{...}} template across the three shared hosts, in a stable order. */
	private static List<String> authorTemplates() {
		var out = new ArrayList<String>();
		out.add(PAGE.title);
		for (var t : PAGE.tabs) {
			out.add(t.label);
			if (t.subtabs != null)
				for (var s : t.subtabs)
					out.add(s.label);
		}
		out.add(PAGE.header.brand.title);
		out.addAll(PAGE.header.brand.crumbs);
		for (var a : PAGE.header.actions) {
			out.add(a.tooltip);
			if (a.menu != null)
				for (var mi : a.menu)
					out.add(mi.label);
		}
		out.add(PAGE.header.avatar.displayName);
		out.add(PAGE.header.avatar.initials);
		for (var w : PAGE.barSlot.widgets) {
			if (w instanceof BarText t)
				out.add(t.text);
			else if (w instanceof BarBadge b)
				out.add(b.label);
		}
		out.add(DETAIL.title);
		out.add(DETAIL.icon);
		for (var s : DETAIL.sections) {
			out.add(s.title);
			if (s.fields != null)
				for (var f : s.fields)
					out.add(f.title);
		}
		out.add(ALERTS.columns.get(0).title);
		out.add(ORPHAN.columns.get(0).title);
		return out;
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) validate() cascades into ServerValues.validate() from both new hosts
	//------------------------------------------------------------------------------------------------------------------

	/** A declaration whose provider map holds a null entry - only reachable through {@code values(Map)}. */
	private static ServerValues brokenValues() {
		var m = new LinkedHashMap<String,ServerValuesValue>();
		m.put("x", null);
		return ServerValues.create().values(m);
	}

	@Test void a01_pageDefValidate_cascadesIntoServerValuesValidate() {
		var page = PageDef.create("p")
			.tabs(Tab.create("t", "T").view(ViewDef.create("v").columns(Column.of("n")).build()))
			.serverValues(brokenValues());
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("ServerValues"), e.getMessage());
	}

	@Test void a02_rowDetailDefValidate_cascadesIntoServerValuesValidate() {
		var d = RowDetailDef.create()
			.endpoint("/x/{id}")
			.sections(DetailSection.create("main", "Main").fields(DetailField.of("k")))
			.serverValues(brokenValues());
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("ServerValues"), e.getMessage());
	}

	@Test void a03_wellFormedHostsValidateCleanly() {
		assertDoesNotThrow(PAGE::build);
		assertDoesNotThrow(() -> DETAIL.validate(null, "alerts"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) PageDef allowlist: every listed field resolves
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_pageTitle_resolvedInPlaceBeforeTheRestOfTheChrome() throws Exception {
		// The probe provider returns PAGE.title as seen from inside the resolve window.
		assertTrue(body("/page?env=A").contains("P-tab:P-title:A/PAGE"), "PageDef.title must resolve in place first");
	}

	@Test void b02_tabAndSubtabLabelsResolve() throws Exception {
		var html = body("/page?env=A");
		assertTrue(html.contains("P-tab2:A/PAGE"), html);
		assertTrue(html.contains("P-subtab:A/PAGE"), html);
		assertFalse(html.contains("P-tab2:$FV{"), html);
		assertFalse(html.contains("P-subtab:$FV{"), html);
	}

	@Test void b03_brandTitleAndEveryCrumbResolve() throws Exception {
		var html = body("/page?env=A");
		assertTrue(html.contains("H-brand:A/PAGE"), html);
		assertTrue(html.contains("H-crumb1:A/PAGE"), html);
		assertTrue(html.contains("H-crumb2:A/PAGE"), html);
	}

	/**
	 * Header-action tooltips, the avatar's display name/initials, and the bar-slot text/badge labels each resolve
	 * as an independent pair of allowlisted fields on the page host.
	 */
	@ParameterizedTest
	@MethodSource("b04_pageHtmlContainsTwoResolvedTemplatesProvider")
	void b04_pageHtmlContainsTwoResolvedTemplates(String expected1, String expected2) throws Exception {
		var html = body("/page?env=A");
		assertTrue(html.contains(expected1), html);
		assertTrue(html.contains(expected2), html);
	}

	static Stream<Arguments> b04_pageHtmlContainsTwoResolvedTemplatesProvider() {
		return Stream.of(
			Arguments.of("H-tooltip:A/PAGE", "H-menutip:A/PAGE"),
			Arguments.of("H-avatar:A/PAGE", "H-initials:A/PAGE"),
			Arguments.of("B-text:A/PAGE", "B-label:A/PAGE"));
	}

	@Test void b07_pageMetaSidecarCarriesResolvedTabLabels() throws Exception {
		var html = body("/page?env=A");
		var open = html.indexOf("id=\"juneau-page:admin\"");
		assertTrue(open >= 0, () -> html);
		var start = html.indexOf('>', open) + 1;
		var meta = html.substring(start, html.indexOf("</script>", start));
		assertTrue(meta.contains("P-tab2:A/PAGE"), meta);
		assertFalse(meta.contains("$FV{"), meta);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) PageDef allowlist is CLOSED: an unlisted field keeps its literal template (guards allowlist creep)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_menuItemLabel_isNotOnTheAllowlist_staysLiteral() throws Exception {
		var html = body("/page?env=A");
		assertTrue(html.contains("H-menuitem:$FV{env}"), "MenuItem.label must not be interpolated");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) RowDetailDef allowlist, resolved into the server-emitted <template> at parent paint time
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_detailTitleSectionAndFieldTitlesResolve() throws Exception {
		var html = body("/page?env=A");
		assertTrue(html.contains("D-title:A/DETAIL"), html);
		assertTrue(html.contains("D-section:A/DETAIL"), html);
		assertTrue(html.contains("D-field:A/DETAIL"), html);
	}

	@Test void d02_detailIcon_isNotOnTheAllowlist_staysLiteral() throws Exception {
		var html = body("/page?env=A");
		assertTrue(html.contains("D-icon:$FV{env}"), "RowDetailDef.icon must not be interpolated");
	}

	@Test void d03_detailHostAlsoResolvesForAStandaloneViewTable() throws Exception {
		var html = body("/view?env=Z");
		assertTrue(html.contains("D-title:Z/DETAIL"), html);
		assertTrue(html.contains("D-section:Z/DETAIL"), html);
		assertTrue(html.contains("D-field:Z/DETAIL"), html);
	}

	@Test void d04_detailTitleTemplateAttributeCarriesTheResolvedString() throws Exception {
		var html = body("/page?env=A");
		assertTrue(html.contains(ViewTable.DETAIL_TITLE_TEMPLATE_ATTR + "=\"D-title:A/DETAIL\""), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Isolation between sibling hosts, and no inheritance into a child view
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_sameNameAcrossHostsResolvesIndependently() throws Exception {
		var html = body("/page?env=A");
		// One response, three hosts, one shared name: each host's own declaration wins.
		assertTrue(html.contains("P-tab2:A/PAGE"), "page host");
		assertTrue(html.contains("D-section:A/DETAIL"), "row-detail host");
		assertTrue(html.contains("V-col:A/VIEW"), "view host");
	}

	@Test void e02_viewWithoutServerValues_doesNotInheritThePageHost() throws Exception {
		var html = body("/page?env=A");
		assertTrue(html.contains("O-col:$FV{env}"), "a ViewDef with no serverValues must keep its literal templates");
		assertFalse(html.contains("O-col:A"), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Restore fidelity: LIFO, per host, before the caller returns
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_everyAuthorTemplateIsByteIdenticalAfterAFullPageRender() throws Exception {
		var before = authorTemplates();
		body("/page?env=A");
		assertEquals(before, authorTemplates(), "author $FV templates must be restored byte-identically");
		// And they really are templates, not already-resolved strings.
		assertTrue(before.contains("P-title:$FV{env}"), before::toString);
		assertTrue(before.contains("D-field:$FV{env}"), before::toString);
	}

	@Test void f02_twoSequentialRequestsResolveFreshly_noCarryOver() throws Exception {
		var first = body("/page?env=A");
		var second = body("/page?env=B");
		assertTrue(first.contains("P-tab2:A/PAGE"), first);
		assertTrue(second.contains("P-tab2:B/PAGE"), second);
		assertFalse(second.contains("P-tab2:A/PAGE"), "the first response's resolved chrome must not survive");
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) Fail-soft (unknown name) / fail-closed (throwing provider) on both new hosts
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_unknownName_isFailSoft_onBothNewHosts() {
		var page = PageDef.create("soft")
			.title("$FV{nope}")
			.tabs(Tab.create("t", "T:$FV{nope,dflt}").view(ViewDef.create("v").columns(Column.of("n")).build()))
			.serverValues(ServerValues.create().value("known", s -> "k"))
			.build();
		assertDoesNotThrow(page::build);
		var d = RowDetailDef.create()
			.endpoint("/x/{id}")
			.title("$FV{nope}")
			.sections(DetailSection.create("main", "$FV{nope,dflt}").fields(DetailField.of("k")))
			.serverValues(ServerValues.create().value("known", s -> "k"));
		assertDoesNotThrow(() -> d.validate(null));
	}

	@Test void g02_unknownName_resolvesToDefaultOrEmpty_neverThrows() throws Exception {
		// The shipped ServerValuesVar contract (fail-soft) is unchanged by the new hosts; assert it end-to-end.
		var html = body("/page?env=A");
		assertFalse(html.contains("P-title:$FV{"), html);
	}

	@Test void g03_throwingProvider_onThePageHost_failsClosed_andStillRestores() throws Exception {
		c.get("/page-throws").run().assertStatus(500);
		assertEquals("T:$FV{bad}", THROWING_PAGE.title, "the restore must run even when a provider throws");
	}

	@Test void g04_throwingProvider_onTheRowDetailHost_failsClosed_andStillRestores() throws Exception {
		c.get("/detail-throws").run().assertStatus(500);
		assertEquals("T:$FV{bad}", THROWING_DETAIL.title, "the restore must run even when a provider throws");
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) The expand GET carries data only - no chrome, no $FV template
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_expandEnvelopeCarriesNoFetchedValueTemplate() throws Exception {
		var json = c.get("/alerts/a1/detail").accept("application/json").run().assertStatus(200)
			.getContent().asString();
		assertFalse(json.contains("$FV{"), () -> json);
	}

	@Test void h02_expandEnvelopeCarriesNoDetailChromeAtAll() throws Exception {
		var json = c.get("/alerts/a1/detail").accept("application/json").run().assertStatus(200)
			.getContent().asString();
		// Chrome is painted into the server-emitted <template>; the expand GET is row data only.
		assertFalse(json.contains("D-title"), () -> json);
		assertFalse(json.contains("D-section"), () -> json);
		assertFalse(json.contains("D-field"), () -> json);
		assertTrue(json.contains("row-value-a1"), () -> json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// i) No contract-version widening by this slice
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_pageContractStillReusesTheViewContract() {
		assertEquals("4", ViewDef.CONTRACT_VERSION);
		assertEquals(ViewDef.CONTRACT_VERSION, PageDef.CONTRACT_VERSION);
	}

	@Test void i02_rowDetailContractUnchangedByThisSlice() {
		assertEquals("1", RowDetailDef.CONTRACT_VERSION);
	}

	@Test void i03_serverValuesStayOffTheWire() throws Exception {
		var html = body("/page?env=A");
		assertFalse(html.contains("serverValues"), "ServerValues is Java-only and must never marshal");
	}
}
