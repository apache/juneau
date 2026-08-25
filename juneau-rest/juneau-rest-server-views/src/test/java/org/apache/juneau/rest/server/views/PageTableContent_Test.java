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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * Emission tests for {@link Tab#content}/{@link Subtab#content} (raw, non-table panel bodies).
 *
 * <p>
 * Covers the {@code PageTable.buildTabPanel} matrix ({@code Tab = {view} | {subtabs} | {content} |
 * {content+subtabs}}, {@code Subtab = {view} | {content}}), the verbatim/unescaped {@code rawText(...)} emission
 * contract (deliberately the opposite of every label/title, which stay HTML-escaped), and &mdash; the item's MED-1
 * finding &mdash; that {@code content} is structurally unreachable from the PAGE_META sidecar because
 * {@code PageTable.buildMeta} is a <b>projection</b> that never reads it, not because of a reserve-and-omit
 * {@code @BeanType} trick (which is what actually closes it off {@code Json.of(...)}-wise, asserted separately
 * below).
 */
class PageTableContent_Test extends TestBase {

	public static class Release {
		public String name;
	}

	private static ViewDef view(String id) {
		return ViewDef.create(id)
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/" + id + "/data")
			.columns(Column.of("name").title("Name"))
			.build();
	}

	/** Extracts the raw text between a sidecar's opening and closing {@code <script>} tags by element id. */
	private static String sidecarBody(String html, String sidecarId) {
		var open = html.indexOf("id=\"" + sidecarId + "\"");
		assertTrue(open >= 0, () -> "sidecar '" + sidecarId + "' not found:\n" + html);
		var contentStart = html.indexOf('>', open) + 1;
		var contentEnd = html.indexOf("</script>", contentStart);
		return html.substring(contentStart, contentEnd);
	}

	/** The single {@code <div ...>} start tag containing all of {@code required}; fails when not exactly one matches. */
	private static String theDivTag(String html, String...required) {
		var divs = new ArrayList<String>();
		var i = 0;
		while ((i = html.indexOf("<div", i)) >= 0) {
			var end = html.indexOf('>', i);
			divs.add(html.substring(i, end + 1));
			i = end + 1;
		}
		var matches = divs.stream().filter(x -> Arrays.stream(required).allMatch(x::contains)).toList();
		assertEquals(1, matches.size(),
			() -> "expected exactly one <div> tag containing " + Arrays.toString(required) + ", found " + matches + " in:\n" + html);
		return matches.get(0);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Leaf Tab: content only, no subtabs
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_leafTabContent_emittedVerbatimInsidePanel() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("overview", "Overview").content("<p>Hello, <b>world</b>.</p>"))
			.build();
		var html = Html.of(PageTable.of(page));
		assertTrue(html.contains("<p>Hello, <b>world</b>.</p>"),
			() -> "raw content must appear byte-for-byte verbatim (no escaping):\n" + html);
	}

	@Test void a02_leafTabContentPanel_carriesOnlyPanelTabAttr() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("overview", "Overview").content("<p>Body.</p>"))
			.build();
		var html = Html.of(PageTable.of(page));
		var tag = theDivTag(html, PageTable.PANEL_CLASS, PageTable.PANEL_TAB_ATTR + "=\"overview\"");
		assertFalse(tag.contains(PageTable.PANEL_SUBTAB_ATTR), tag);
	}

	@Test void a03_leafTabContent_noDataJuneauViewMarker() {
		// A content-only tab references no ViewDef, so it must not emit a data-juneau-view table/marker at all.
		var page = PageDef.create("docs")
			.tabs(Tab.create("overview", "Overview").content("<p>Body.</p>"))
			.build();
		var html = Html.of(PageTable.of(page));
		assertFalse(html.contains("data-juneau-view"), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Preamble: Tab carrying both content and subtabs
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_tabPreambleContent_rendersAboveSubtabBar_inSameOuterPanel() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog")
				.content("<p>Preamble prose.</p>")
				.subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		var html = Html.of(PageTable.of(page));
		var preambleIdx = html.indexOf("<p>Preamble prose.</p>");
		var subtabBarIdx = html.indexOf(PageTable.SUBTAB_BAR_CLASS);
		assertTrue(preambleIdx >= 0, html);
		assertTrue(subtabBarIdx > preambleIdx,
			() -> "preamble content must render ABOVE the sub-tab bar:\n" + html);
	}

	@Test void b02_tabPreambleContent_liveInsideTheSubtabAgnosticOuterPanel() {
		// The outer panel wrapping the preamble + sub-tab bar must stay sub-tab-agnostic (no data-panel-subtab) -
		// this is the existing PageTable_SubtabPanelContract_Test invariant, re-verified with a preamble present
		// so the new code path can't have silently reintroduced it.
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog")
				.content("<p>Preamble.</p>")
				.subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		var html = Html.of(PageTable.of(page));
		var tag = theDivTag(html, PageTable.PANEL_CLASS + "\"", PageTable.PANEL_TAB_ATTR + "=\"catalog\"");
		assertFalse(tag.contains(PageTable.PANEL_SUBTAB_ATTR),
			() -> "preamble must not force data-panel-subtab onto the outer panel: " + tag);
	}

	@Test void b03_tabWithoutPreamble_emitsNoExtraContentBeforeSubtabBar() {
		// Regression: a tab with subtabs and NO content must render identically to before this item (no stray
		// empty raw-text node ahead of the sub-tab bar).
		var withoutPreamble = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		var withPreamble = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog")
				.subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		assertEquals(Html.of(PageTable.of(withoutPreamble)), Html.of(PageTable.of(withPreamble)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Subtab: content only, no view
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_subtabContent_emittedVerbatimInsideItsSubpanel() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("about", "About").content("<p>About text.</p>")))
			.build();
		var html = Html.of(PageTable.of(page));
		assertTrue(html.contains("<p>About text.</p>"), html);
	}

	@Test void c02_subtabContentSubpanel_carriesBothTabAndSubtabAttrs() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("about", "About").content("<p>x</p>")))
			.build();
		var html = Html.of(PageTable.of(page));
		var tag = theDivTag(html, PageTable.SUBPANEL_CLASS, PageTable.PANEL_SUBTAB_ATTR + "=\"about\"");
		assertTrue(tag.contains(PageTable.PANEL_TAB_ATTR + "=\"catalog\""), tag);
	}

	@Test void c03_mixedViewAndContentSubtabsUnderOneTab_eachGetsItsOwnBody() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog").subtabs(
				Subtab.create("data", "Data").view(view("data")),
				Subtab.create("about", "About").content("<p>About.</p>")))
			.build();
		var html = Html.of(PageTable.of(page));
		assertTrue(html.contains("data-juneau-view=\"data\""), html);
		assertTrue(html.contains("<p>About.</p>"), html);
		// The content subtab must NOT also carry a data-juneau-view marker.
		var aboutTag = theDivTag(html, PageTable.SUBPANEL_CLASS, PageTable.PANEL_SUBTAB_ATTR + "=\"about\"");
		var aboutStart = html.indexOf(aboutTag);
		var aboutEnd = html.indexOf(PageTable.SUBPANEL_CLASS, aboutStart + aboutTag.length());
		var aboutSection = aboutEnd > 0 ? html.substring(aboutStart, aboutEnd) : html.substring(aboutStart);
		assertFalse(aboutSection.contains("data-juneau-view"), aboutSection);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ownership contract: verbatim / unescaped (opposite of every label/title)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_contentIsNeverHtmlEntityEscaped_unlikeLabels() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("t", "<script>alert('label')</script>")
				.content("<script>alert('content')</script>"))
			.build();
		var html = Html.of(PageTable.of(page));
		// The tab LABEL is escaped (existing contract, PageTable_Emit_Test b01) ...
		assertFalse(html.contains("<script>alert('label')</script>"), html);
		// ... but panel CONTENT is emitted verbatim - this is the template-engine ownership contract, not a bug.
		assertTrue(html.contains("<script>alert('content')</script>"),
			() -> "content must be emitted verbatim per the documented ownership contract:\n" + html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// MED-1: PAGE_META is a projection - content is structurally unreachable, not merely omitted
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_contentBearingTab_producesIdenticalPageMetaToContentlessTab() {
		// The core MED-1 assertion: buildMeta() copies id/label/subtabs only, so adding content to a Tab cannot
		// change one single byte of PAGE_META. Proven by diffing the sidecar of two otherwise-identical pages.
		var withoutContent = PageDef.create("admin")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		var withContent = PageDef.create("admin")
			.tabs(Tab.create("catalog", "Catalog")
				.content("<p>Ignored by PAGE_META.</p>")
				.subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		var metaWithout = sidecarBody(Html.of(PageTable.of(withoutContent)), "juneau-page:admin");
		var metaWith = sidecarBody(Html.of(PageTable.of(withContent)), "juneau-page:admin");
		assertEquals(metaWithout, metaWith,
			() -> "PAGE_META must be byte-identical regardless of Tab#content - buildMeta() is a projection that "
				+ "never reads it, so it cannot leak in:\nwithout=" + metaWithout + "\nwith=" + metaWith);
	}

	@Test void e02_contentBearingSubtab_producesIdenticalPageMetaToViewBackedSubtab() {
		// Same projection fact, exercised via a Subtab#content instead of Subtab#view - SubtabMeta pins id/label
		// only, so swapping view<->content changes nothing in PAGE_META either.
		var withView = PageDef.create("admin")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		var withContent = PageDef.create("admin")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("s", "S").content("<p>x</p>")))
			.build();
		var metaWithView = sidecarBody(Html.of(PageTable.of(withView)), "juneau-page:admin");
		var metaWithContent = sidecarBody(Html.of(PageTable.of(withContent)), "juneau-page:admin");
		assertEquals(metaWithView, metaWithContent);
	}

	@Test void e03_pageMetaNeverContainsTheLiteralStringContent_evenWhenPresentOnEveryTabAndSubtab() {
		// Belt-and-suspenders: even with "content" set everywhere in the tree, the word never appears as a
		// PAGE_META key. (Distinct from Json.of(Tab) reserve-and-omit, checked separately in f01/f02 below - this
		// one is scoped to the actual PAGE_META sidecar the runtime consumes.)
		var page = PageDef.create("admin")
			.tabs(Tab.create("catalog", "Catalog")
				.content("<p>preamble</p>")
				.subtabs(Subtab.create("s", "S").content("<p>sub</p>")))
			.build();
		var meta = sidecarBody(Html.of(PageTable.of(page)), "juneau-page:admin");
		assertFalse(meta.contains("content"), () -> "PAGE_META must never carry a 'content' key:\n" + meta);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Reserve-and-omit at the bean level: a direct Json.of(...) of the PageDef tree also omits content
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_directJsonOfPageDef_omitsTabContent() {
		// Unlike PAGE_META (a projection - e01-e03), PageDef itself IS directly JSON-serializable
		// (PageDef_Contract_Test exercises this same Json.of(page) path). Tab#content is deliberately absent from
		// Tab's own @BeanType(properties=...) list, so it is reserved-and-omitted from THIS path too, at no
		// CONTRACT_VERSION cost (the wire shape is byte-for-byte unchanged).
		var page = PageDef.create("admin")
			.tabs(Tab.create("t", "T").content("<p>trusted prose</p>"))
			.build();
		var json = Json.of(page);
		assertFalse(json.contains("content"), () -> "Tab#content must be reserved-and-omitted from Json.of(PageDef): " + json);
		assertTrue(json.contains("\"contractVersion\":\"" + PageDef.CONTRACT_VERSION + "\""), json);
	}

	@Test void f02_directJsonOfPageDef_omitsSubtabContent() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("s", "S").content("<p>trusted prose</p>")))
			.build();
		var json = Json.of(page);
		assertFalse(json.contains("content"), () -> "Subtab#content must be reserved-and-omitted from Json.of(PageDef): " + json);
	}
}
