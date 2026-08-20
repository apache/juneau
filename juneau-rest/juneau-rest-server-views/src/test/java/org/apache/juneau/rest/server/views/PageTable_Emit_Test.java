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
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * Markup + escaping tests for the {@link PageTable} emitter.
 *
 * <p>
 * Proves the emitter produces the {@code data-juneau-page} shell, a tab-bar entry per {@link Tab} (+ a sub-tab bar
 * per {@link Tab} with subtabs), a panel per referenced view carrying the <b>same</b> {@code data-juneau-view}
 * marker + VIEW_META sidecar {@code ViewTable.of(...)} already emits, and one PAGE_META sidecar &mdash; and that all
 * server-emitted labels/titles are HTML-escaped with no {@code url()}/inline-style injection.
 */
class PageTable_Emit_Test extends TestBase {

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

	private static PageDef leafPage() {
		return PageDef.create("admin")
			.title("Admin")
			.tabs(
				Tab.create("releases", "Releases").view(view("releases")),
				Tab.create("users", "Users").view(view("users")))
			.build();
	}

	private static PageDef pageWithSubtabs() {
		return PageDef.create("admin")
			.tabs(
				Tab.create("releases", "Releases").view(view("releases")),
				Tab.create("catalog", "Catalog").subtabs(
					Subtab.create("packages", "Packages").view(view("packages")),
					Subtab.create("bundles", "Bundles").view(view("bundles"))))
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

	//------------------------------------------------------------------------------------------------------------------
	// Structure (Task 3)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_emitsPageShellMarker() {
		var html = Html.of(PageTable.of(leafPage()));
		assertTrue(html.contains("data-juneau-page=\"admin\""), html);
		assertTrue(html.contains(PageTable.PAGE_CLASS), html);
	}

	@Test void a02_emitsOneTabBarEntryPerTab() {
		var html = Html.of(PageTable.of(leafPage()));
		assertTrue(html.contains(PageTable.TAB_ID_ATTR + "=\"releases\""), html);
		assertTrue(html.contains(PageTable.TAB_ID_ATTR + "=\"users\""), html);
		assertTrue(html.contains(PageTable.TAB_CLASS), html);
	}

	@Test void a03_emitsSubTabBarOnlyForTabsWithSubtabs() {
		var html = Html.of(PageTable.of(pageWithSubtabs()));
		assertTrue(html.contains(PageTable.SUBTAB_ID_ATTR + "=\"packages\""), html);
		assertTrue(html.contains(PageTable.SUBTAB_ID_ATTR + "=\"bundles\""), html);
		assertTrue(html.contains(PageTable.SUBTAB_CLASS), html);
		// The leaf "releases" tab has no subtabs -> no subtab-bar markers referencing it.
		assertFalse(html.contains(PageTable.PARENT_TAB_ATTR + "=\"releases\""), html);
	}

	@Test void a04_eachViewGetsItsOwnMarkerTableAndSidecar() {
		var html = Html.of(PageTable.of(pageWithSubtabs()));
		for (var id : List.of("releases", "packages", "bundles")) {
			assertTrue(html.contains("data-juneau-view=\"" + id + "\""), html);
			assertTrue(html.contains("id=\"juneau-view:" + id + "\""), html);
		}
	}

	@Test void a05_childViewOutputIsByteForByteIdenticalToStandaloneViewTable() {
		var v = view("releases");
		var standalone = Html.of(ViewTable.of(v));
		var page = PageDef.create("admin").tabs(Tab.create("releases", "Releases").view(v)).build();
		var pageHtml = Html.of(PageTable.of(page));
		assertTrue(pageHtml.contains(standalone), () -> "page output does not contain the standalone ViewTable output verbatim:\n" + pageHtml + "\n---standalone---\n" + standalone);
	}

	@Test void a06_emitsOnePageMetaSidecar() {
		var html = Html.of(PageTable.of(leafPage()));
		assertTrue(html.contains("id=\"juneau-page:admin\""), html);
	}

	@Test void a07_pageMetaCarriesTabTreeIds() {
		var html = Html.of(PageTable.of(pageWithSubtabs()));
		var body = sidecarBody(html, "juneau-page:admin");
		var meta = Json.to(body, Map.class);
		assertEquals("admin", meta.get("id"));
		var tabs = (List<?>) meta.get("tabs");
		assertEquals(2, tabs.size());
		var releasesTab = (Map<?,?>) tabs.get(0);
		assertEquals("releases", releasesTab.get("id"));
		assertEquals("Releases", releasesTab.get("label"));
		assertFalse(releasesTab.containsKey("subtabs"), releasesTab::toString);
		var catalogTab = (Map<?,?>) tabs.get(1);
		var subtabs = (List<?>) catalogTab.get("subtabs");
		assertEquals(2, subtabs.size());
		assertEquals("packages", ((Map<?,?>) subtabs.get(0)).get("id"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Escaping / chrome (Task 4)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_tabLabelsAreHtmlEscaped() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("t", "<script>alert(1)</script>").view(view("v")))
			.build();
		var html = Html.of(PageTable.of(page));
		assertFalse(html.contains("<script>alert(1)</script>"), html);
		assertTrue(html.contains("&lt;script&gt;"), html);
	}

	@Test void b02_subtabLabelsAreHtmlEscaped() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("t", "T").subtabs(Subtab.create("s", "<img src=x onerror=alert(1)>").view(view("v"))))
			.build();
		var html = Html.of(PageTable.of(page));
		assertFalse(html.contains("<img src=x onerror=alert(1)>"), html);
	}

	@Test void b03_pageTitleIsHtmlEscapedIfRendered() {
		var page = PageDef.create("admin").title("<b>Admin</b>").tabs(Tab.create("t", "T").view(view("v"))).build();
		var html = Html.of(PageTable.of(page));
		assertFalse(html.contains("<b>Admin</b>"), html);
	}

	@Test void b04_noInlineStyleAttributeEmitted() {
		// The emitter must never write a caller string into style="..." (a label containing "url(...)" is just
		// escaped display text here, not a CSS injection vector, since no style="" attribute exists at all).
		var page = PageDef.create("admin")
			.tabs(Tab.create("t", "url(javascript:alert(1))").view(view("v")))
			.build();
		var html = Html.of(PageTable.of(page));
		assertFalse(html.contains("style=\""), html);
	}

	@Test void b05_pageMetaCarriesContractVersion() {
		var html = Html.of(PageTable.of(leafPage()));
		var body = sidecarBody(html, "juneau-page:admin");
		var meta = Json.to(body, Map.class);
		assertEquals(PageDef.CONTRACT_VERSION, meta.get("contractVersion"));
	}

	@Test void b06_pageMetaScriptBreakoutIsNeutralized() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("t", "x</script><!-- <script>alert(1)</script>").view(view("v")))
			.build();
		var html = Html.of(PageTable.of(page));
		var body = sidecarBody(html, "juneau-page:admin");
		assertFalse(body.contains("<"), () -> "raw '<' leaked into the PAGE_META sidecar:\n" + body);
		assertTrue(body.contains("\\u003c"), () -> "expected \\u003c escaping in PAGE_META sidecar:\n" + body);
		// Still valid, round-trippable JSON carrying the original label.
		var meta = Json.to(body, Map.class);
		var tabs = (List<?>) meta.get("tabs");
		assertEquals("x</script><!-- <script>alert(1)</script>", ((Map<?,?>) tabs.get(0)).get("label"));
	}

	@Test void b07_hashHrefsUseIdsNotLabels() {
		var html = Html.of(PageTable.of(pageWithSubtabs()));
		assertTrue(html.contains("href=\"#admin/releases\""), html);
		assertTrue(html.contains("href=\"#admin/catalog/packages\""), html);
		assertTrue(html.contains("href=\"#admin/catalog/bundles\""), html);
	}

	@Test void c01_resolvedBase_stampsPageShell() {
		var html = Html.of(PageTable.of(MarshallingContext.DEFAULT, leafPage(), "/ctx/juneau-saved-views"));
		assertTrue(html.contains("data-juneau-saved-views="), html);
		assertTrue(html.contains("/ctx/juneau-saved-views"), html);
		assertEquals(ViewTable.SAVED_VIEWS_ATTR, PageTable.SAVED_VIEWS_ATTR);
	}

	@Test void c02_absentBase_doesNotStamp() {
		var html = Html.of(PageTable.of(leafPage()));
		assertFalse(html.contains("data-juneau-saved-views"), html);
	}
}
