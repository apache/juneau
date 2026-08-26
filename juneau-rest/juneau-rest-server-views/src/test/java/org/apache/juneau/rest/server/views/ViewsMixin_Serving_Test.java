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
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.views.ViewDef.*;
import org.apache.juneau.rest.server.widgets.*;
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
@SuppressWarnings({
	"resource", // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
	"deprecation" // Section b) deliberately exercises the deprecated compatibility mounts for the four relocated widget assets.
})
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

	/**
	 * Request-bearing {@link ViewTable}/{@link PageTable} {@code of(req, ...)} host so the
	 * {@code data-juneau-saved-views} stamp is resolved through a real {@link RestRequest} URI resolver
	 * (the emit tests only exercise the already-resolved-base overload).
	 */
	@Rest(mixins=ViewsMixin.class)
	public static class StampHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/releases") public Div releases(RestRequest req) {
			return ViewTable.of(req, releasesView());
		}
		@RestGet(path="/admin") public Div admin(RestRequest req) {
			return PageTable.of(req, PageDef.create("admin")
				.tabs(Tab.create("releases", "Releases").view(releasesView()))
				.build());
		}
	}

	private static final MockRestClient cNoMixin = MockRestClient.buildLax(NoMixin.class);
	private static final MockRestClient cWithMixin = MockRestClient.buildLax(WithMixin.class);
	private static final MockRestClient cViewHost = MockRestClient.buildLax(ViewHost.class);
	private static final MockRestClient cViewBeanHost = MockRestClient.buildLax(ViewBeanHost.class);
	private static final MockRestClient cStampHost = MockRestClient.buildLax(StampHost.class);

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
		cNoMixin.get(ViewsMixin.CONFIG_JS_PATH).run().assertStatus(404);
		cNoMixin.get(ViewsMixin.CONFIG_CSS_PATH).run().assertStatus(404);
	}

	@Test void h01_hostWithoutMixin_iconsJsRouteIs404() throws Exception {
		cNoMixin.get(ViewsMixin.ICONS_JS_PATH).run().assertStatus(404);
	}

	@Test void h01b_hostWithoutMixin_symbolsSvgRouteIs404() throws Exception {
		cNoMixin.get(ViewsMixin.SYMBOLS_SVG_PATH).run().assertStatus(404);
	}

	@Test void h02_hostWithoutMixin_configAssetsAre404() throws Exception {
		cNoMixin.get(ViewsMixin.CONFIG_JS_PATH).run().assertStatus(404);
		cNoMixin.get(ViewsMixin.CONFIG_CSS_PATH).run().assertStatus(404);
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

	@Test void b06_configJs_served() throws Exception {
		cWithMixin.get(ViewsMixin.CONFIG_JS_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/javascript")
			.assertHeader("Cache-Control").isContains("max-age")
			.assertContent().asString().isContains("juneau-config.js");
	}

	@Test void b07_configCss_served() throws Exception {
		cWithMixin.get(ViewsMixin.CONFIG_CSS_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/css")
			.assertHeader("Cache-Control").isContains("max-age")
			.assertContent().asString().isContains("juneau-config.css");
	}

	/**
	 * The four widget-owned runtime assets have now been <b>relocated</b> into {@code juneau-rest-server-widgets},
	 * beside the bean contracts that drive them, so ownership of the "who ships these bytes" assertion moved with
	 * them to {@code WidgetsMixin_Serving_Test}.  What this module still owes is the compatibility promise: an
	 * application composing only this mixin keeps getting a {@code 200} with the right content type and caching
	 * headers at the same URL it always used.  This is a stricter check than the pre-relocation one it replaces,
	 * because it additionally pins the served body to the widget module's classpath bytes &mdash; so a stale copy
	 * left behind in this module could not satisfy it.
	 */
	@Test void b08_relocatedWidgetAssets_stillServeFromTheDeprecatedViewsMount() throws Exception {
		for (var e : java.util.Map.of(
				ViewsMixin.CARDS_JS_PATH, "/org/apache/juneau/widgets/juneau-cards.js",
				ViewsMixin.CALENDAR_JS_PATH, "/org/apache/juneau/widgets/juneau-calendar.js",
				ViewsMixin.CALENDAR_CSS_PATH, "/org/apache/juneau/widgets/juneau-calendar.css",
				ViewsMixin.CHROME_JS_PATH, "/org/apache/juneau/widgets/juneau-chrome.js").entrySet()) {
			var path = e.getKey();
			var served = cWithMixin.get(path).run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains(path.endsWith(".css") ? "text/css" : "text/javascript")
				.assertHeader("Cache-Control").isContains("max-age")
				.getContent().asBytes();
			assertArrayEquals(widgetClasspathBytes(e.getValue()), served, () -> path + ": served body is not the widget module's bytes");
		}
	}

	/** The compatibility mount is a mount: it only exists where this mixin is composed. */
	@Test void b09_relocatedWidgetAssets_are404WithoutThisMixin() throws Exception {
		cNoMixin.get(ViewsMixin.CARDS_JS_PATH).run().assertStatus(404);
		cNoMixin.get(ViewsMixin.CALENDAR_JS_PATH).run().assertStatus(404);
		cNoMixin.get(ViewsMixin.CALENDAR_CSS_PATH).run().assertStatus(404);
		cNoMixin.get(ViewsMixin.CHROME_JS_PATH).run().assertStatus(404);
	}

	/**
	 * The relocated four hash and version through the <b>widget</b> module's asset cache, so this mixin's deprecated
	 * URL for one of them is character-identical to {@code WidgetsMixin}'s URL for it.  Two differently-busted URLs
	 * for one body would make a page composing both mixins cache the same script twice.
	 */
	@Test void b10_relocatedWidgetAssets_carryTheSameCacheBusterAsTheWidgetMixin() throws Exception {
		for (var path : new String[]{
				ViewsMixin.CARDS_JS_PATH, ViewsMixin.CALENDAR_JS_PATH, ViewsMixin.CALENDAR_CSS_PATH,
				ViewsMixin.CHROME_JS_PATH}) {
			var servedBytes = cWithMixin.get(path).run().assertStatus(200).getContent().asBytes();
			var expectedHash = ChecksumUtils.hash8(servedBytes);
			var url = ViewsMixin.viewAssetUrl(path);
			assertTrue(url.endsWith("-" + expectedHash), () -> path + ": expected suffix '-" + expectedHash + "' in '" + url + "'");
			assertEquals(WidgetsMixin.widgetAssetUrl(path), url, () -> path + ": deprecated views URL diverged from the widget mixin's");
		}
	}

	/**
	 * The move must be a move, not a copy.  A duplicated asset would keep every test above green while quietly
	 * shipping two divergent bodies, so this asserts the old coordinates are gone from this module's resources and
	 * that the new coordinates resolve to exactly one classpath entry.
	 */
	@Test void b11_relocatedWidgetAssets_areGoneFromThisModulesResources() throws Exception {
		for (var name : new String[]{"juneau-cards.js", "juneau-calendar.js", "juneau-calendar.css", "juneau-chrome.js"}) {
			assertNull(ViewsMixin.class.getResourceAsStream("/org/apache/juneau/views/" + name),
				() -> name + ": still present at the old views coordinates - the move left a copy behind");
			var found = java.util.Collections.list(ViewsMixin.class.getClassLoader().getResources("org/apache/juneau/widgets/" + name));
			assertEquals(1, found.size(), () -> name + ": expected exactly one classpath entry, found " + found);
		}
	}

	private static byte[] widgetClasspathBytes(String resource) throws Exception {
		try (var in = WidgetsMixin.class.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing classpath resource: " + resource);
			return in.readAllBytes();
		}
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
		for (var path : new String[]{ViewsMixin.VIEWS_JS_PATH, ViewsMixin.RIBBON_JS_PATH, ViewsMixin.RENDERS_JS_PATH, ViewsMixin.VIEWS_CSS_PATH, ViewsMixin.ICONS_JS_PATH, ViewsMixin.SYMBOLS_SVG_PATH, ViewsMixin.CONFIG_JS_PATH, ViewsMixin.CONFIG_CSS_PATH})
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

	/**
	 * Pins {@link ViewsMixin#viewAssetUrl(String)}'s content-hash suffix against an independently-computed
	 * {@link ChecksumUtils#hash8} of each served asset's actual bytes (fetched over the mock client, not read off
	 * the classpath directly): proves the shared {@code ClasspathAssetCache} helper produces byte-for-byte the same
	 * hash the hand-rolled per-mixin {@code hash8}/{@code contentHash} pair it replaced did, so no served URL's
	 * cache-buster shifted when the hashing moved into a shared class.
	 */
	@Test void c04_viewAssetUrl_contentHash_matchesIndependentlyComputedHash8OfServedBytes() throws Exception {
		for (var path : new String[]{
				ViewsMixin.VIEWS_JS_PATH, ViewsMixin.RIBBON_JS_PATH, ViewsMixin.RENDERS_JS_PATH,
				ViewsMixin.VIEWS_CSS_PATH, ViewsMixin.ICONS_JS_PATH, ViewsMixin.SYMBOLS_SVG_PATH, ViewsMixin.PAGES_JS_PATH,
				ViewsMixin.CONFIG_JS_PATH, ViewsMixin.CONFIG_CSS_PATH}) {
			var servedBytes = cWithMixin.get(path).run().assertStatus(200).getContent().asBytes();
			var expectedHash = ChecksumUtils.hash8(servedBytes);
			var url = ViewsMixin.viewAssetUrl(path);
			assertTrue(url.endsWith("-" + expectedHash), () -> path + ": expected suffix '-" + expectedHash + "' in '" + url + "'");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Contract-version handshake constant (single source of truth = ViewDef.CONTRACT_VERSION)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_contractVersion_equalsViewDefContractVersion() {
		assertEquals(ViewDef.CONTRACT_VERSION, ViewsMixin.CONTRACT_VERSION);
		assertEquals("4", ViewsMixin.CONTRACT_VERSION);
	}

	@Test void d02_viewsJs_bakesInContractVersionHandshake() throws Exception {
		// The initializer must carry the handshake constant so the client can fail-loud on a contractVersion mismatch.
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("JUNEAU_VIEW_CONTRACT_VERSION"), body);
		assertTrue(body.contains("\"" + ViewsMixin.CONTRACT_VERSION + "\""), body);
	}

	/**
	 * Compatibility re-exports for widget envelopes must stay aliased to {@code WidgetsMixin} / the beans,
	 * and must stay distinct from {@link #CONTRACT_VERSION} (VIEW_META).  Behavioral JS coverage of those
	 * runtimes lives in {@code juneau-rest-server-widgets}; this pin is the views leftover.
	 */
	@Test void d03_widgetContractReexports_trackWidgetsMixinAndStayDistinctFromViewMeta() {
		assertEquals(WidgetsMixin.HEADER_CONTRACT_VERSION, ViewsMixin.HEADER_CONTRACT_VERSION);
		assertEquals(AppHeaderDef.CONTRACT_VERSION, ViewsMixin.HEADER_CONTRACT_VERSION);
		assertEquals(WidgetsMixin.BAR_CONTRACT_VERSION, ViewsMixin.BAR_CONTRACT_VERSION);
		assertEquals(BarSlot.CONTRACT_VERSION, ViewsMixin.BAR_CONTRACT_VERSION);
		assertEquals(WidgetsMixin.CARDS_CONTRACT_VERSION, ViewsMixin.CARDS_CONTRACT_VERSION);
		assertEquals(CardFieldList.CONTRACT_VERSION, ViewsMixin.CARDS_CONTRACT_VERSION);
		assertNotSame(ViewsMixin.CONTRACT_VERSION, ViewsMixin.HEADER_CONTRACT_VERSION);
		assertNotSame(ViewsMixin.CONTRACT_VERSION, ViewsMixin.BAR_CONTRACT_VERSION);
		assertNotSame(ViewsMixin.CONTRACT_VERSION, ViewsMixin.CARDS_CONTRACT_VERSION);
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
		assertTrue(body.contains("formatUtcZulu("), body);
		assertTrue(body.contains("formatCalifornia("), body);
		assertTrue(body.contains("America/Los_Angeles"), body);
		assertTrue(body.contains("textContent"), body);   // popup lines must not use innerHTML
		assertTrue(body.contains("createElement"), body);
		assertFalse(body.contains(".innerHTML"), body);
	}

	@Test void e02_viewsCss_hasNeutralTagChip() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		// A real base-chip rule (not just the header comment): neutral shape/padding, no colors.
		assertTrue(body.contains(".tag {"), body);
		assertTrue(body.contains("border-radius:"), body);
		assertTrue(body.contains(".juneau-ts-popup {"), body);
		assertTrue(body.contains("position: fixed"), body);
	}

	@Test void e04_viewsCss_hasNeutralRibbonButtonShape() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-ribbon-btn {"), body);
		assertTrue(body.contains("min-width: 32px"), body);
		// The button's height now spends the shared control-height step rather than repeating its literal, so the
		// shape is pinned by asserting BOTH halves - the reference here and the step's declared value - rather
		// than dropping to the reference alone, which would pass even if the step were re-valued.
		assertTrue(body.contains("height: var(--jc-chrome-control-height)"), body);
		assertTrue(body.contains("--jc-chrome-control-height: 32px"), body);
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
		// Ungrouped icon buttons (export, columnSearchToggle, etc.) share one synthetic cluster so they render as
		// a connected ribbon rather than orphan glyphs.  refresh is excluded from that cluster by default - it is
		// normalized into its own trailing group instead (see normalizeRibbon) - unless it declares an explicit
		// .group() of its own, in which case it joins that group like any other action.
		assertTrue(fnBody.contains("__ungrouped"), fnBody);
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
		assertTrue(body.contains("juneau-symbols.svg"), body);
		for (var name : new String[]{
				"content_copy", "csv", "table", "picture_as_pdf", "refresh", "manage_search", "unfold_less",
				"first_page", "chevron_left", "chevron_right", "last_page", "tune", "filter_alt", "expand_more"})
			assertTrue(body.contains("\"" + name + "\""), () -> "missing bundled glyph '" + name + "':\n" + body);
	}

	@Test void h04_symbolsSvg_served_andKeyFileIsNot() throws Exception {
		cWithMixin.get(ViewsMixin.SYMBOLS_SVG_PATH).run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("image/svg+xml")
			.assertHeader("Cache-Control").isContains("max-age")
			.assertContent().asString().isContains("juneau-sym-copy");
		cWithMixin.get("/juneau-symbols-key.svg").run().assertStatus(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// i) viewAssetUrl(RestRequest, path): a real, resolved, browser-fetchable URL (not "servlet:...")
	//------------------------------------------------------------------------------------------------------------------

	/** Echoes the request-aware overload's resolved URL as a plain-text response, for assertion below. */
	@Rest(mixins=ViewsMixin.class)
	public static class AssetUrlHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/echo-asset-url") public String echoAssetUrl(RestRequest req) {
			return ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH);
		}
	}

	@Test void i01_requestAwareOverload_resolvesToARealUrl_notAServletPrefixedLiteral() throws Exception {
		var url = MockRestClient.buildLax(AssetUrlHost.class).get("/echo-asset-url").accept("text/plain").run()
			.assertStatus(200).getContent().asString();
		// The whole point of FG-4: the literal "servlet:" scheme must NOT leak into the resolved result - a
		// template-rendering consumer downstream of Juneau's own HTML serializer would otherwise receive it
		// verbatim and fetch nothing.
		assertFalse(url.startsWith("servlet:"), url);
		assertTrue(url.endsWith(ViewsMixin.VIEWS_JS_PATH.substring(1)) || url.contains(ViewsMixin.VIEWS_JS_PATH + "?v="), url);
	}

	@Test void i02_requestAwareOverload_carriesTheSameVersionAndContentHashCacheBuster() throws Exception {
		var url = MockRestClient.buildLax(AssetUrlHost.class).get("/echo-asset-url").accept("text/plain").run()
			.assertStatus(200).getContent().asString();
		var staticForm = ViewsMixin.viewAssetUrl(ViewsMixin.VIEWS_JS_PATH);
		var expectedSuffix = staticForm.substring(staticForm.indexOf("?v="));
		assertTrue(url.endsWith(expectedSuffix), url);
	}

	@Test void i03_requestAwareOverload_resolvesUnderANonRootHostMount() throws Exception {
		var c = MockRestClient.createLax(AssetUrlHost.class).servletPath("/rest/admin").build();
		var url = c.get("/echo-asset-url").accept("text/plain").run().assertStatus(200).getContent().asString();
		assertTrue(url.startsWith("/rest/admin" + ViewsMixin.VIEWS_JS_PATH), url);
	}

	@Test void i04_requestAwareOverload_resolvedUrlIsActuallyFetchableAtThatMount() throws Exception {
		// Round-trip proof: the URL this overload hands a template is not just shaped right - fetching it (minus
		// the query string) against the SAME mount actually serves the asset.
		var c = MockRestClient.createLax(AssetUrlHost.class).servletPath("/rest/admin").build();
		var url = c.get("/echo-asset-url").accept("text/plain").run().assertStatus(200).getContent().asString();
		var pathOnly = url.substring(0, url.indexOf('?'));
		var relative = pathOnly.substring("/rest/admin".length());
		c.get(relative).run().assertStatus(200);
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
		assertTrue(body.contains("function findViewWrapper("), body);
		assertTrue(body.contains("juneau-view-toolbar-row"), body);
		assertTrue(body.contains(".querySelector(\".dataTables_filter, .dt-search\""), body);
		assertTrue(body.contains(".dt-container, .dataTables_wrapper"), body);
	}

	@Test void n03_viewsCss_controlRowsWrapAndDividerIsSpacing() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-toolbar-row {"), body);
		var rowStart = body.indexOf(".juneau-view-toolbar-row {");
		var rowEnd = body.indexOf("}", rowStart);
		var rowRegion = body.substring(rowStart, rowEnd);
		assertTrue(rowRegion.contains("flex-wrap: nowrap"), rowRegion);
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
		assertTrue(region.contains("flex-wrap: nowrap"), region);

		assertTrue(body.contains(".juneau-view-toolbar-right .dataTables_filter,"), body);
		assertTrue(body.contains(".juneau-view-toolbar-right .dt-search {"), body);
		var searchStart = body.indexOf(".juneau-view-toolbar-right .dt-search {");
		var searchEnd = body.indexOf("}", searchStart);
		assertTrue(body.substring(searchStart, searchEnd).contains("display: inline-flex"), body);
	}

	/**
	 * Control-row layout item 2/5 (single segmented ribbon): the group is one OUTER chrome - member buttons
	 * drop their inner vertical borders (left/right none; first restores left, last restores right) with
	 * rounding only on the outer ends.  A gap or a per-button box border would draw inner outlines.
	 */
	@Test void o02_viewsCss_ribbonGroupCollapsesSharedBordersRoundedOnlyOnOuterEnds() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-ribbon-group { display: inline-flex;"), body);

		assertTrue(body.contains(".juneau-view-ribbon-group .juneau-view-ribbon-btn {"), body);
		var memberStart = body.indexOf(".juneau-view-ribbon-group .juneau-view-ribbon-btn {");
		var memberEnd = body.indexOf("}", memberStart);
		var memberRegion = body.substring(memberStart, memberEnd);
		assertTrue(memberRegion.contains("border-radius: 0"), memberRegion);
		assertTrue(memberRegion.contains("border-left: none"), memberRegion);
		assertTrue(memberRegion.contains("border-right: none"), memberRegion);
		assertFalse(memberRegion.contains("margin-left: -1px"), memberRegion);

		assertTrue(body.contains(".juneau-view-ribbon-group .juneau-view-ribbon-btn:first-child {"), body);
		assertTrue(body.contains(".juneau-view-ribbon-group .juneau-view-ribbon-btn:last-child {"), body);
		assertTrue(body.contains(".juneau-view-ribbon-group .juneau-view-ribbon-btn:only-child {"), body);
	}

	/**
	 * Tab-mode strip shape: the SAME {@code .juneau-view-ribbon-group} carries a {@code data-juneau-strip-mode="tab"}
	 * visual variant (floor line under the strip, square bottom corners on the end caps, the active tab overlapping
	 * the divider) reused by multi-section row-details.  The neutral shape lives here; color lives in chrome.css.
	 */
	@Test void o05_viewsCss_ribbonGroupHasTabModeStripVariant() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		var sel = ".juneau-view-ribbon-group[data-juneau-strip-mode=\"tab\"]";
		assertTrue(body.contains(sel + " {"), body);
		var start = body.indexOf(sel + " {");
		var region = body.substring(start, body.indexOf("}", start));
		assertTrue(region.contains("border-bottom: 1px solid"), region);   // the floor line
		assertTrue(region.contains("overflow-x: auto"), region);           // scroll, no "more" menu
		assertTrue(region.contains("white-space: nowrap"), region);

		// The active tab overlaps the divider (margin-bottom: -1px).
		assertTrue(body.contains(sel + " .juneau-view-ribbon-btn {"), body);
		var btnStart = body.indexOf(sel + " .juneau-view-ribbon-btn {");
		assertTrue(body.substring(btnStart, body.indexOf("}", btnStart)).contains("margin-bottom: -1px"), body);

		// End caps square off their BOTTOM corners in tab-mode (top radii stay).
		var firstSel = sel + " .juneau-view-ribbon-btn:first-child {";
		assertTrue(body.contains(firstSel), body);
		var fcStart = body.indexOf(firstSel);
		assertTrue(body.substring(fcStart, body.indexOf("}", fcStart)).contains("border-bottom-left-radius: 0"), body);
		var lastSel = sel + " .juneau-view-ribbon-btn:last-child {";
		assertTrue(body.contains(lastSel), body);
		var lcStart = body.indexOf(lastSel);
		assertTrue(body.substring(lcStart, body.indexOf("}", lcStart)).contains("border-bottom-right-radius: 0"), body);
	}

	/**
	 * Detail tabs are no longer INDEPENDENT of the generic strip - they are the generic tab-mode strip,
	 * unmodified.  The pill override this used to assert the existence of is what made the two diverge, and
	 * deleting it is the whole of the fix, so what is pinned here is its ABSENCE.
	 *
	 * <p>Absence is asserted against the rule headers rather than against the bare selector, because the
	 * bar-slot host rule below still legitimately starts with that same selector text and would satisfy a naive
	 * {@code contains} check - the reason the original assertion here would have stayed green through the
	 * deletion instead of failing as the change that removed it expected.
	 */
	@Test void o06_viewsCss_hasDetailHeaderAndNoDetailTabOverride() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-detail-header {"), body);
		assertTrue(body.contains(".juneau-view-detail-title {"), body);
		assertTrue(body.contains(".juneau-view-detail-icon {"), body);
		var pill = ".juneau-view-ribbon-group.juneau-view-detail-tabs[data-juneau-strip-mode=\"tab\"]";
		assertFalse(body.contains(pill + " {"), body);
		assertFalse(body.contains(pill + " .juneau-view-ribbon-btn"), body);
		// The one detail-tabs selector that survives positions a trailing bar-slot region; it does not repaint
		// the strip.
		assertTrue(body.contains(pill + "[data-juneau-strip-trailed]"), body);
		assertTrue(body.contains("border-radius: 999px"), body);
		assertTrue(body.contains(".juneau-sym-flip-x"), body);
	}

	@Test void o03_viewsCss_hasNeutralColumnSearchRowAndInputShape() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-columnsearch-row > th {"), body);
		assertTrue(body.contains(".juneau-view-columnsearch-input {"), body);
		var start = body.indexOf(".juneau-view-columnsearch-input {");
		var end = body.indexOf("}", start);
		assertTrue(body.substring(start, end).contains("border: 1px solid"), body);
	}

	@Test void o04_viewsCss_hasCompactDataTableDensityAndHairlineGrid() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("padding: 4px 5px"), body);
		assertTrue(body.contains("font-size: 0.75rem"), body);
		assertTrue(body.contains("font-weight: normal"), body);
		assertTrue(body.contains("flex-direction: row !important"), body);
		assertTrue(body.contains("content: none"), body);
		assertTrue(body.contains("border-collapse: collapse"), body);
		assertTrue(body.contains("border-top-width: 2px"), body);
		assertTrue(body.contains("border: 1px solid"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// i) data-juneau-saved-views stamp via a real RestRequest URI resolver
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_requestBearingViewTable_stampsWrapperNotTable() throws Exception {
		var html = cStampHost.get("/releases").accept("text/html").run().assertStatus(200).getContent().asString();
		assertTrue(html.contains("data-juneau-saved-views="), html);
		assertTrue(html.contains(SavedViewsMixin.SAVED_VIEWS_PREFIX), html);
		assertFalse(html.contains("<table id=\"releases\" data-juneau-saved-views"), html);
		assertFalse(html.contains("<table id='releases' data-juneau-saved-views"), html);
	}

	@Test void i02_requestBearingPageTable_stampsPageShell() throws Exception {
		var html = cStampHost.get("/admin").accept("text/html").run().assertStatus(200).getContent().asString();
		assertTrue(html.contains("data-juneau-saved-views="), html);
		assertTrue(html.contains(SavedViewsMixin.SAVED_VIEWS_PREFIX), html);
		assertTrue(html.contains("data-juneau-page"), html);
	}
}
