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

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Wiring tests for the TODO-445m page chrome on {@link PageTable}: the optional {@link PageDef#header} whole-header
 * lead + its refresh sidecar, and the optional {@link PageDef#barSlot} region emitted as a <b>trailing sibling of
 * {@code .jc-subtab-bar}</b> (never into the archived {@code .juneau-view-toolbar-*} DataTables control row).
 *
 * <p>
 * These are the m2 B locked-attachment regressions: the header/bar are Java-only builder fields (absent from the
 * wire), the page's own {@code PageDef.CONTRACT_VERSION} / {@link ViewDef#CONTRACT_VERSION} must not bump, and a
 * page that declares neither must emit byte-identical output to before this feature existed.
 */
class PageTableChrome_Test extends TestBase {

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

	private static AppHeaderDef header() {
		return AppHeaderDef.create("app")
			.brand(Brand.create().logo(true).title("Admin"))
			.actions(HeaderAction.link("docs", "table", "Docs", "/docs"))
			.refreshUrl("/chrome/counts")
			.build();
	}

	private static BarSlot barSlot() {
		return BarSlot.create("ctx")
			.widgets(
				BarText.of("mode", "Editing"),
				BarBadge.of("pending").label("pending").badge(Badge.count(3).tone(Tone.WARN)));
	}

	private static PageDef leafPage() {
		return PageDef.create("admin").title("Admin").tabs(
			Tab.create("releases", "Releases").view(view("releases"))).build();
	}

	private static PageDef subtabPage() {
		return PageDef.create("admin").tabs(
			Tab.create("catalog", "Catalog").subtabs(
				Subtab.create("packages", "Packages").view(view("packages")),
				Subtab.create("bundles", "Bundles").view(view("bundles")))).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header lead + refresh sidecar
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_headerLeadsTheShellWhenSet() {
		var html = Html.of(PageTable.of(leafPage().header(header())));
		assertTrue(html.contains("data-juneau-app-header=\"app\""), html);
		// The whole <header> leads: it appears before the tab bar.
		assertTrue(html.indexOf("data-juneau-app-header") < html.indexOf(PageTable.TAB_BAR_CLASS), html);
	}

	@Test void a02_headerRefreshSidecarEmittedOnceWhenRefreshUrlSet() {
		var html = Html.of(PageTable.of(leafPage().header(header())));
		assertTrue(html.contains("id=\"juneau-header:app\""), html);
		var first = html.indexOf("juneau-header:app");
		assertEquals(-1, html.indexOf("juneau-header:app", first + 1), "header sidecar must be emitted exactly once");
	}

	@Test void a03_noHeaderMeansNoHeaderMarkup() {
		var html = Html.of(PageTable.of(leafPage()));
		assertFalse(html.contains("data-juneau-app-header"), html);
		assertFalse(html.contains("juneau-header:"), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Bar slot: trailing sibling of .jc-subtab-bar, never the archived control row
	//------------------------------------------------------------------------------------------------------------------

	@Test void a04_barRegionIsTrailingSiblingOfSubtabBar() {
		var html = Html.of(PageTable.of(subtabPage().barSlot(barSlot())));
		assertTrue(html.contains("data-juneau-bar-slot=\"ctx\""), html);
		// The region follows the .jc-subtab-bar it trails.
		assertTrue(html.indexOf(PageTable.SUBTAB_BAR_CLASS) < html.indexOf("data-juneau-bar-slot"), html);
		assertTrue(html.contains(">Editing<"), "bar text rendered as escaped text");
		assertTrue(html.contains("data-juneau-badge=\"bar:pending\""), "bar badge namespaced bar:<id>");
	}

	@Test void a05_barRegionNeverInArchivedToolbarControlRow() {
		var html = Html.of(PageTable.of(subtabPage().barSlot(barSlot())));
		assertFalse(html.contains("juneau-view-toolbar"), "bar slot must never emit the archived .juneau-view-toolbar-* row");
	}

	@Test void a06_barSidecarEmittedOnceAtShellLevel() {
		var html = Html.of(PageTable.of(subtabPage().barSlot(barSlot())));
		assertTrue(html.contains("id=\"juneau-bar:ctx\""), html);
		var first = html.indexOf("id=\"juneau-bar:ctx\"");
		assertEquals(-1, html.indexOf("id=\"juneau-bar:ctx\"", first + 1), "bar sidecar must be emitted exactly once");
	}

	@Test void a07_leafOnlyPageEmitsNoBarRegion() {
		// The locked attachment is a trailing sibling of .jc-subtab-bar; a page with only leaf tabs has no sub-tab bar.
		var html = Html.of(PageTable.of(leafPage().barSlot(barSlot())));
		assertFalse(html.contains("data-juneau-bar-slot"), "no sub-tab bar -> no bar region (accepted locked limitation)");
		// The single data-only bar sidecar is still emitted once at shell level.
		assertTrue(html.contains("id=\"juneau-bar:ctx\""), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// No wire/contract drift
	//------------------------------------------------------------------------------------------------------------------

	@Test void a08_pageAndViewContractVersionsUnchanged() {
		assertEquals(ViewDef.CONTRACT_VERSION, PageDef.CONTRACT_VERSION, "PageDef contract must still reuse ViewDef's");
	}

	@Test void a09_pageWithoutChromeIsByteIdenticalToBefore() {
		var plain = Html.of(PageTable.of(leafPage()));
		var withNullChrome = Html.of(PageTable.of(leafPage().header(null).barSlot(null)));
		assertEquals(plain, withNullChrome, "null header/barSlot must not perturb the shell output");
		assertFalse(plain.contains("data-juneau-app-header"), plain);
		assertFalse(plain.contains("data-juneau-bar-slot"), plain);
	}
}
