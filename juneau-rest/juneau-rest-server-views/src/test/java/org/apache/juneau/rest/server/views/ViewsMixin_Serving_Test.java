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

import static org.junit.jupiter.api.Assertions.*;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.html5.Div;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.views.ViewDef.*;
import org.junit.jupiter.api.*;

/**
 * Serving smoke + versioned-URL + contract-handshake tests for {@link ViewsMixin} (design doc §6.1, Task B.4).
 *
 * <p>
 * Option-A coverage (mirrors {@code DataTablesMixin}/{@code ConsoleChromeMixin}'s serving tests): a host composing
 * the mixin exposes each of the four runtime assets at its stable path with a {@code 200} + correct content-type +
 * {@code Cache-Control}; a host without the mixin {@code 404}s the same paths.  The {@code ?v=<buildVersion>}
 * cache-buster and the {@code CONTRACT_VERSION} handshake constant are asserted directly.
 */
class ViewsMixin_Serving_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Hosts
	//------------------------------------------------------------------------------------------------------------------

	public static class NoMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	/** Row bean for the emitted view page. */
	public static class Rel {
		public String name;
		public String status;
		public String date;
	}

	/** The §6.10 golden-shape view the page-consumption test emits + round-trips. */
	static ViewDef releasesView() {
		return ViewDef.create("releases")
			.rowType(Rel.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.defaultOrder("date", Dir.DESC)
			.columns(
				Column.of("name").title("Name").render("linked").href("servlet:/releases/{name}"),
				Column.of("status").title("Status").render("tag:status"),
				Column.of("date").title("Date").render("date"))
			.ribbon(
				RibbonAction.export("copy", "csv").optional("excel", "pdf"),
				RibbonAction.columnSearchToggle(),
				RibbonAction.option("show-superseded").title("Show superseded").column("status").value("superseded").persist(true),
				RibbonAction.refresh())
			.build();
	}

	// Emits the view the documented way (OD-2(a)): pre-serialize the ViewTable markup and serve it as trusted markup,
	// rather than returning the DOM bean through the full HtmlDoc page path (the sidecar's one-shot Reader content is
	// intended to be serialized exactly once - see ViewTable's class javadoc).
	@Rest(mixins=ViewsMixin.class)
	public static class ViewHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/releases") public HttpResource releasesPage() {
			var markup = Html.of(ViewTable.of(releasesView()));
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(StandardCharsets.UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	// Regression (Task 1): returns the ViewTable Div bean DIRECTLY through the servlet's full HtmlDoc page path.
	// Before the rawText swap this 500'd because the sidecar's one-shot StringReader content could not survive that
	// serializer (it worked only via HtmlSerializer/Html.of); the String-backed rawText makes the bean re-serializable.
	@Rest(mixins=ViewsMixin.class)
	public static class ViewBeanHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/releases") public Div releasesPage() {
			return ViewTable.of(releasesView());
		}
	}

	private static final MockRestClient cNoMixin = MockRestClient.buildLax(NoMixin.class);
	private static final MockRestClient cWithMixin = MockRestClient.buildLax(WithMixin.class);
	private static final MockRestClient cViewHost = MockRestClient.buildLax(ViewHost.class);
	private static final MockRestClient cViewBeanHost = MockRestClient.buildLax(ViewBeanHost.class);

	/** Extracts a named function's body: from `function <name>(` to the next top-level `\n\t}`. */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	/** Extracts the raw text between the sidecar's opening and closing {@code <script>} tags. */
	private static String sidecarBody(String html) {
		var open = html.indexOf("id=\"juneau-view:releases\"");
		assertTrue(open >= 0, () -> "sidecar script tag not found:\n" + html);
		var contentStart = html.indexOf('>', open) + 1;
		var contentEnd = html.indexOf("</script>", contentStart);
		return html.substring(contentStart, contentEnd);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Opt-in / back-compat
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_hostWithoutMixin_assetRoutesAre404() throws Exception {
		cNoMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(404);
		cNoMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(404);
		cNoMixin.get(ViewsMixin.RENDERS_JS_PATH).run().assertStatus(404);
		cNoMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(404);
	}

	@Test void h01_hostWithoutMixin_iconsJsRouteIs404() throws Exception {
		cNoMixin.get(ViewsMixin.ICONS_JS_PATH).run().assertStatus(404);
	}

	@Test void a02_hostExistingRoute_unaffectedByMixin() throws Exception {
		cWithMixin.get("/items").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Each asset serves 200 + content-type + Cache-Control + a content substring
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_viewsJs_served() throws Exception {
		cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/javascript")
			.assertHeader("Cache-Control").isContains("max-age")
			.assertContent().asString().isContains("juneau-views.js");
	}

	@Test void b02_ribbonJs_served() throws Exception {
		cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/javascript")
			.assertHeader("Cache-Control").isContains("max-age")
			.assertContent().asString().isContains("juneau-ribbon.js");
	}

	@Test void b03_rendersJs_served() throws Exception {
		cWithMixin.get(ViewsMixin.RENDERS_JS_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/javascript")
			.assertHeader("Cache-Control").isContains("max-age")
			.assertContent().asString().isContains("juneau-renders.js");
	}

	@Test void b04_viewsCss_served() throws Exception {
		cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/css")
			.assertHeader("Cache-Control").isContains("max-age")
			.assertContent().asString().isContains("juneau-views.css");
	}

	@Test void b05_arbitraryVersionQueryString_stillServes200() throws Exception {
		// The ?v=... cache-buster is a browser cache key, not part of mixin routing - any value still resolves.
		cWithMixin.get(ViewsMixin.VIEWS_JS_PATH + "?v=whatever").run().assertStatus(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Versioned asset URL (?v=<buildVersion>) via the viewAssetUrl(...) helper
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_viewAssetUrl_carriesVersionAndContentHashCacheBuster() {
		var v = ViewsMixin.class.getPackage().getImplementationVersion();
		var expectedPrefix = "servlet:" + ViewsMixin.VIEWS_JS_PATH + "?v=" + (v == null ? "dev" : v) + "-";
		var url = ViewsMixin.viewAssetUrl(ViewsMixin.VIEWS_JS_PATH);
		assertTrue(url.startsWith(expectedPrefix), url);
		assertTrue(url.substring(expectedPrefix.length()).matches("[0-9a-f]{8}"), url);
	}

	@Test void c02_viewAssetUrl_worksForEveryAssetPath() {
		for (var path : new String[]{ViewsMixin.VIEWS_JS_PATH, ViewsMixin.RIBBON_JS_PATH, ViewsMixin.RENDERS_JS_PATH, ViewsMixin.VIEWS_CSS_PATH, ViewsMixin.ICONS_JS_PATH})
			assertTrue(ViewsMixin.viewAssetUrl(path).contains("?v="), path);
	}

	@Test void c03_viewAssetUrl_contentHash_isStableAcrossCalls_andDiffersAcrossDistinctAssets() {
		// The content-hash cache-buster (Task 1) is computed once from each asset's own bytes - it must be stable
		// across repeated calls (not recomputed per-call) and must differ between distinct assets.
		var url1 = ViewsMixin.viewAssetUrl(ViewsMixin.VIEWS_JS_PATH);
		var url2 = ViewsMixin.viewAssetUrl(ViewsMixin.VIEWS_JS_PATH);
		assertEquals(url1, url2, "hash must be stable across repeated calls");
		assertNotEquals(url1, ViewsMixin.viewAssetUrl(ViewsMixin.RIBBON_JS_PATH), "distinct assets must not collide on their content hash");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Contract-version handshake constant (single source of truth = ViewDef.CONTRACT_VERSION)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_contractVersion_equalsViewDefContractVersion() {
		assertEquals(ViewDef.CONTRACT_VERSION, ViewsMixin.CONTRACT_VERSION);
		assertEquals("2", ViewsMixin.CONTRACT_VERSION);
	}

	@Test void d02_viewsJs_bakesInContractVersionHandshake() throws Exception {
		// The initializer must carry the handshake constant so the client can fail-loud on a contractVersion mismatch.
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("JUNEAU_VIEW_CONTRACT_VERSION"), body);
		assertTrue(body.contains("\"" + ViewsMixin.CONTRACT_VERSION + "\""), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) B.6 - juneau-renders.js registry + base juneau-views.css .tag chip
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_rendersJs_hasRegistryAndGenericRenderers() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RENDERS_JS_PATH).run().assertStatus(200).getContent().asString();
		// Concrete API surface (call sites / registered ids), not just the header-comment prose.
		assertTrue(body.contains("registerRenderer("), body);
		assertTrue(body.contains("parseRenderId("), body);
		assertTrue(body.contains("\"ts-zulu\""), body);   // the ts-zulu renderer is actually registered
	}

	@Test void e02_viewsCss_hasNeutralTagChip() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		// A real base-chip rule (not just the header comment): neutral shape/padding, no colors.
		assertTrue(body.contains(".tag {"), body);
		assertTrue(body.contains("border-radius:"), body);
	}

	@Test void e04_viewsCss_hasNeutralRibbonButtonShape() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-ribbon-btn {"), body);
		assertTrue(body.contains("width: 32px"), body);
		assertTrue(body.contains("height: 32px"), body);
		assertTrue(body.contains("display: flex"), body);
		assertTrue(body.contains("border: 1px solid"), body);
		assertTrue(body.contains(".juneau-view-ribbon-btn svg {"), body);
		assertTrue(body.contains("fill: currentColor"), body);
	}

	@Test void e03_rendersJs_tagRendererNormalizesCssTokenToMatchConsoleUi() throws Exception {
		// Regression: the `tag` renderer must mirror console-ui's Tag#normalize/TagHtmlRender token algorithm
		// (lowercase both <domain> and <value> into the `.tag.<domain>.<value>` CSS token) so themed chrome.css
		// rules (e.g. `.tag.status.released`) match - a raw "RELEASED" cell must no longer render as an
		// unthemed neutral chip.  Content-substring coverage only: the module's browser harness
		// (PagePanelVisibility_BrowserTest) covers the page runtime, not the renderer registry, so executing this
		// renderer to check its exact lowercased/hyphenated output would need a second fixture there.
		var body = cWithMixin.get(ViewsMixin.RENDERS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("normalizeTagToken("), body);
		var tagRendererStart = body.indexOf("registerRenderer(\"tag\"");
		assertTrue(tagRendererStart >= 0, () -> "tag renderer not found:\n" + body);
		var tagRendererEnd = body.indexOf("registerRenderer(", tagRendererStart + 1);
		var tagRendererRegion = body.substring(tagRendererStart, tagRendererEnd < 0 ? body.length() : tagRendererEnd);
		assertTrue(tagRendererRegion.contains("normalizeTagToken(domain)"), tagRendererRegion);
		assertTrue(tagRendererRegion.contains("normalizeTagToken(value)"), tagRendererRegion);
		// The RAW value (not the normalized token) must still be what the user sees as display text.
		assertTrue(tagRendererRegion.contains("escHtml(value)"), tagRendererRegion);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) B.7 - juneau-ribbon.js runtime (feature-detected export + column-search-value contribution)
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_ribbonJs_hasFeatureDetectedExportAndParamContribution() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("Buttons"), body);       // feature-detect DataTables Buttons
		assertTrue(body.contains("feature"), body);        // feature-detection markers
		assertTrue(body.contains("columns["), body);       // mirrors RibbonAction.toQueryParams' columns[N][search][value]
	}

	@Test void f02_ribbonJs_rendersIconOnlyButtonsWithAriaLabel() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("setAttribute(\"aria-label\""), body);
		assertTrue(body.contains(".title = "), body);
		assertTrue(body.contains("resolveButtonIcon("), body);
		assertTrue(body.contains("NS.icons.resolveIcon("), body);
		var buttonFnStart = body.indexOf("function button(");
		assertTrue(buttonFnStart >= 0, () -> "function button( not found:\n" + body);
		var buttonFnEnd = body.indexOf("\n\t}", buttonFnStart);
		var buttonFnBody = body.substring(buttonFnStart, buttonFnEnd < 0 ? body.length() : buttonFnEnd);
		assertFalse(buttonFnBody.contains("b.textContent = label"), buttonFnBody);
	}

	@Test void f03_ribbonJs_exportButtonsTriggerDataTablesButtonsApi() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var exportStart = body.indexOf("a.type === \"export\"");
		assertTrue(exportStart >= 0, () -> "export branch not found:\n" + body);
		var exportEnd = body.indexOf("if (a.type ===", exportStart + 1);
		var exportRegion = body.substring(exportStart, exportEnd < 0 ? body.length() : exportEnd);
		assertTrue(exportRegion.contains(".button("), exportRegion);
		assertTrue(exportRegion.contains(".trigger("), exportRegion);
		assertFalse(exportRegion.contains(".buttons().container().appendTo(bar)"), exportRegion);
	}

	/**
	 * Control-row layout item 2/5 (single segmented ribbon): the export cluster's buttons, and any two adjacent
	 * actions sharing a {@code group} id, must be wrapped in ONE ".juneau-view-ribbon-group" span via the local
	 * {@code place(el, groupId)} helper - not appended as loose siblings of "bar" (which would render as
	 * individually-bordered buttons rather than one shared-border segmented cluster).
	 */
	@Test void f04_ribbonJs_groupsExportButtonsAndSharedGroupIdActionsIntoOneSegmentedCluster() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildRibbon(");
		assertTrue(fnBody.contains("function place("), fnBody);
		assertTrue(fnBody.contains("juneau-view-ribbon-group"), fnBody);
		// export's own buttons always cluster (synthetic per-index id when no explicit .group is set).
		assertTrue(fnBody.contains("__export"), fnBody);
		assertTrue(fnBody.contains("place("), fnBody);
		// A divider always closes any open cluster (a divider is a deliberate visual break, not a grouping seam).
		var dividerStart = fnBody.indexOf("a.type === \"divider\"");
		var dividerEnd = fnBody.indexOf("return;", dividerStart);
		assertTrue(fnBody.substring(dividerStart, dividerEnd).contains("openGroup = null"), fnBody);
	}

	/**
	 * Root-cause regression for the broken columnSearchToggle button (control-row layout item 4): the button's
	 * click handler previously flipped `ctx.columnSearchOn` and invoked `ctx.onColumnSearchToggle` if present, but
	 * NOTHING in juneau-views.js ever assigned that callback (grep-confirmed absent before this fix) - so toggling
	 * silently did nothing, and the button never reflected state via `aria-pressed` either (unlike `optionToggle`'s
	 * button, which already did). Asserts the ribbon-side half of the fix: the button's `aria-pressed` is now set
	 * from `toggleColumnSearch(...)`'s return value, both initially and on every click.
	 */
	@Test void f05_ribbonJs_columnSearchToggleButtonReflectsAriaPressed() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var branchStart = body.indexOf("a.type === \"columnSearchToggle\"");
		assertTrue(branchStart >= 0, () -> "columnSearchToggle branch not found:\n" + body);
		var branchEnd = body.indexOf("if (a.type ===", branchStart + 1);
		var branch = body.substring(branchStart, branchEnd < 0 ? body.length() : branchEnd);
		assertTrue(branch.contains("csBtn.setAttribute(\"aria-pressed\""), branch);
		assertTrue(branch.contains("toggleColumnSearch(viewDef, ctx)"), branch);

		var fnBody = functionBody(body, "function toggleColumnSearch(");
		assertTrue(fnBody.contains("return ctx.columnSearchOn;"), fnBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) B.8 - juneau-views.js initializer
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_viewsJs_hasInitializerLogic() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("JSON.parse"), body);     // reads the sidecar
		assertTrue(body.contains("serverSide"), body);     // wires server-side ajax mode
		assertTrue(body.contains("createdRow"), body);     // applies rowClassRules
	}

	@Test void g04_viewsJs_columnDefsSetDefaultContentForUndefinedSafety() throws Exception {
		// Regression: a nullable column's value is OMITTED (not null) by the server's JSON serializer, so
		// DataTables' data accessor sees `undefined` and throws "Requested unknown parameter" (datatables.net/tn/4)
		// before any renderer runs.  Every generated column def must set defaultContent so DataTables substitutes
		// that value instead of warning - content-substring coverage only: proving the undefined-safe behavior would
		// mean booting DataTables itself, which the module's browser harness deliberately stubs out rather than loads.
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var buildColumnDefStart = body.indexOf("function buildColumnDef(");
		assertTrue(buildColumnDefStart >= 0, () -> "buildColumnDef not found:\n" + body);
		var buildColumnDefEnd = body.indexOf("\n\t}", buildColumnDefStart);
		var buildColumnDefBody = body.substring(buildColumnDefStart, buildColumnDefEnd < 0 ? body.length() : buildColumnDefEnd);
		assertTrue(buildColumnDefBody.contains("defaultContent:"), buildColumnDefBody);
	}

	@Test void g05_viewsJs_hasSearchLanguagePolishAndPageSizeVocabulary() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("searchPlaceholder"), body);
		assertTrue(body.contains("\"Search\""), body);
		assertTrue(body.contains("\"25 rows\""), body);
		assertTrue(body.contains("\"100 rows\""), body);
		assertTrue(body.contains("\"All rows\""), body);
		assertTrue(body.contains("pageLength"), body);
	}

	@Test void g02_hostPageEmitsSidecarThatRoundTrips() throws Exception {
		// A ViewsMixin host page that emits a ViewTable carries the marker + sidecar the initializer consumes.
		var html = cViewHost.get("/releases").accept("text/html").run().assertStatus(200).getContent().asString();
		assertTrue(html.contains("data-juneau-view=\"releases\""), html);
		assertTrue(html.contains("id=\"juneau-view:releases\""), html);
		// The sidecar JSON round-trips back to the same VIEW_META the model emits (structural compare).
		var body = sidecarBody(html);
		assertEquals(Json.to(Json.of(releasesView()), java.util.Map.class), Json.to(body, java.util.Map.class), body);
	}

	@Test void g03_beanReturnedThroughFullHtmlDocPagePath_is200AndReserializable() throws Exception {
		// The reported bug: returning the ViewTable Div bean through the servlet's full HtmlDoc page path 500'd
		// because the one-shot StringReader sidecar could not survive that serializer.  With rawText it is 200.
		var html = cViewBeanHost.get("/releases").accept("text/html").run().assertStatus(200).getContent().asString();
		assertTrue(html.contains("data-juneau-view=\"releases\""), html);
		assertTrue(html.contains("id=\"juneau-view:releases\""), html);

		// Repeated requests re-emit + re-serialize a fresh bean each time through the page path (not one-shot).
		var html2 = cViewBeanHost.get("/releases").accept("text/html").run().assertStatus(200).getContent().asString();
		assertEquals(html, html2, "full-page HtmlDoc output must be stable across repeated serializations");
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) Ribbon visual-parity Task 1 - juneau-icons.js icon registry
	//------------------------------------------------------------------------------------------------------------------

	@Test void h02_iconsJs_served() throws Exception {
		cWithMixin.get(ViewsMixin.ICONS_JS_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/javascript")
			.assertHeader("Cache-Control").isContains("max-age")
			.assertContent().asString().isContains("juneau-icons.js");
	}

	@Test void h03_iconsJs_hasRegistryAndBundledGlyphs() throws Exception {
		var body = cWithMixin.get(ViewsMixin.ICONS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("registerIcon("), body);
		assertTrue(body.contains("resolveIcon("), body);
		for (var name : new String[]{
				"content_copy", "csv", "table", "picture_as_pdf", "refresh", "manage_search", "unfold_less",
				"first_page", "chevron_left", "chevron_right", "last_page", "tune", "filter_alt", "expand_more"})
			assertTrue(body.contains("\"" + name + "\""), () -> "missing bundled glyph '" + name + "':\n" + body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// n) Ribbon visual-parity Task 12/13 - responsive cleanup (tab/sub-tab scroll, control-row wrap, toolbar wrapper)
	//------------------------------------------------------------------------------------------------------------------

	@Test void n01_viewsCss_tabBarScrollsInsteadOfWraps() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		var barStart = body.indexOf(".jc-tab-bar,\n.jc-subtab-bar {");
		assertTrue(barStart >= 0, () -> ".jc-tab-bar,\\n.jc-subtab-bar { not found:\n" + body);
		var barEnd = body.indexOf("}", barStart);
		var barRegion = body.substring(barStart, barEnd);
		assertTrue(barRegion.contains("flex-wrap: nowrap"), barRegion);
		assertTrue(barRegion.contains("overflow-x: auto"), barRegion);
		assertFalse(barRegion.contains("flex-wrap: wrap;"), barRegion);

		var tabStart = body.indexOf(".jc-tab,\n.jc-subtab {");
		assertTrue(tabStart >= 0, () -> ".jc-tab,\\n.jc-subtab { not found:\n" + body);
		var tabEnd = body.indexOf("}", tabStart);
		var tabRegion = body.substring(tabStart, tabEnd);
		assertTrue(tabRegion.contains("flex: 0 0 auto"), tabRegion);
	}

	@Test void n02_viewsJs_wrapsRibbonAndSearchInToolbarRow() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("function buildToolbarRow("), body);
		assertTrue(body.contains("juneau-view-toolbar-row"), body);
		assertTrue(body.contains(".querySelector(\".dataTables_filter, .dt-search\""), body);
	}

	@Test void n03_viewsCss_controlRowsWrapAndDividerIsSpacing() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-toolbar-row {"), body);
		var rowStart = body.indexOf(".juneau-view-toolbar-row {");
		var rowEnd = body.indexOf("}", rowStart);
		var rowRegion = body.substring(rowStart, rowEnd);
		assertTrue(rowRegion.contains("flex-wrap: wrap"), rowRegion);
		// Control-row layout: LEFT cluster + RIGHT cluster, pushed apart so the right cluster stays right-aligned
		// (superseded the old single-cluster "justify-content: flex-start").
		assertTrue(rowRegion.contains("justify-content: space-between"), rowRegion);

		assertTrue(body.contains(".juneau-view-ribbon-divider {"), body);
		var divStart = body.indexOf(".juneau-view-ribbon-divider {");
		var divEnd = body.indexOf("}", divStart);
		var divRegion = body.substring(divStart, divEnd);
		assertFalse(divRegion.contains("border"), divRegion);
	}

	//------------------------------------------------------------------------------------------------------------------
	// o) Control-row layout restructure (LEFT page-controls / RIGHT search+ribbon+refresh clusters)
	//------------------------------------------------------------------------------------------------------------------

	@Test void o01_viewsCss_hasLeftAndRightToolbarClusterShapes() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-toolbar-left,\n.juneau-view-toolbar-right {"), body);
		var start = body.indexOf(".juneau-view-toolbar-left,\n.juneau-view-toolbar-right {");
		var end = body.indexOf("}", start);
		var region = body.substring(start, end);
		assertTrue(region.contains("display: flex"), region);
		assertTrue(region.contains("flex-wrap: wrap"), region);
	}

	/**
	 * Control-row layout item 2/5 (single segmented ribbon): the group wrapper must NOT carry a `gap` (a gap would
	 * leave a visible seam between adjacent buttons instead of a collapsed shared border), and its member buttons
	 * must collapse borders via a negative margin with rounding only on the outer ends.
	 */
	@Test void o02_viewsCss_ribbonGroupCollapsesSharedBordersRoundedOnlyOnOuterEnds() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-ribbon-group { display: inline-flex; }"), body);

		assertTrue(body.contains(".juneau-view-ribbon-group .juneau-view-ribbon-btn {"), body);
		var memberStart = body.indexOf(".juneau-view-ribbon-group .juneau-view-ribbon-btn {");
		var memberEnd = body.indexOf("}", memberStart);
		var memberRegion = body.substring(memberStart, memberEnd);
		assertTrue(memberRegion.contains("border-radius: 0"), memberRegion);
		assertTrue(memberRegion.contains("margin-left: -1px"), memberRegion);

		assertTrue(body.contains(".juneau-view-ribbon-group .juneau-view-ribbon-btn:first-child {"), body);
		assertTrue(body.contains(".juneau-view-ribbon-group .juneau-view-ribbon-btn:last-child {"), body);
	}

	@Test void o03_viewsCss_hasNeutralColumnSearchRowAndInputShape() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-columnsearch-row > th {"), body);
		assertTrue(body.contains(".juneau-view-columnsearch-input {"), body);
		var start = body.indexOf(".juneau-view-columnsearch-input {");
		var end = body.indexOf("}", start);
		assertTrue(body.substring(start, end).contains("border: 1px solid"), body);
	}
}
