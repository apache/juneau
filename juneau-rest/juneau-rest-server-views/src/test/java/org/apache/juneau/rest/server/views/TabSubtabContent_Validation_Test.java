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
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * TODO-420: validation-matrix tests for {@link Tab#content}/{@link Subtab#content}.
 *
 * <p>
 * Pins the panel-body matrices stated on {@link Tab} and {@link Subtab}'s javadoc and mirrored across all three
 * enforcement sites (design doc §"Findings resolution" MED-2):
 * <ul class='spaced-list'>
 * 	<li><b>{@code Tab = {view} | {subtabs} | {content} | {content+subtabs}}</b> &mdash; exactly one of
 * 		{@code view}/{@code subtabs}/{@code content}, EXCEPT {@code content} may co-occur with {@code subtabs}
 * 		(the preamble case).
 * 	<li><b>{@code Subtab = {view} | {content}}</b> &mdash; exactly one.
 * </ul>
 *
 * <p>
 * Also covers the {@link PageDef#build()} consequence of widening the matrix: a content-only tab (no
 * {@code view}, no {@code subtabs}) and a subtab carrying {@code content} instead of {@code view} must not throw
 * (the pre-420 {@code PageDef.validate()} unconditionally dereferenced {@code t.subtabs}/{@code s.view} and would
 * NPE on either shape).
 *
 * <p>
 * Deliberately kept in its own 420-owned file (not added to the pre-existing {@code PageDef_Validation_Test}), per
 * the item's constraint to keep every new assertion in a 420-owned fixture.
 */
class TabSubtabContent_Validation_Test extends TestBase {

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

	//------------------------------------------------------------------------------------------------------------------
	// Tab matrix: {view} | {subtabs} | {content} | {content+subtabs}
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_tabWithContentOnly_accepted() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("overview", "Overview").content("<p>Hello, world.</p>"))
			.build();
		assertEquals("<p>Hello, world.</p>", page.tabs.get(0).content);
	}

	@Test void a02_tabWithContentAndSubtabs_accepted() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog")
				.content("<p>Preamble.</p>")
				.subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		assertEquals("<p>Preamble.</p>", page.tabs.get(0).content);
		assertEquals(1, page.tabs.get(0).subtabs.size());
	}

	@Test void a03_tabWithViewAndContent_rejected() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("t", "T").view(view("v")).content("<p>x</p>"));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("exactly one"), e::getMessage);
	}

	@Test void a04_tabWithViewAndSubtabsAndContent_rejected() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("t", "T").view(view("v")).content("<p>x</p>")
				.subtabs(Subtab.create("s", "S").view(view("s"))));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("exactly one"), e::getMessage);
	}

	@Test void a05_tabWithNeitherViewNorSubtabsNorContent_rejected() {
		var page = PageDef.create("docs").tabs(Tab.create("t", "T"));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("exactly one"), e::getMessage);
	}

	@Test void a06_tabWithViewAndSubtabsNoContent_stillRejected() {
		// Regression: content's addition must not loosen the pre-existing view+subtabs exclusion.
		var page = PageDef.create("docs")
			.tabs(Tab.create("t", "T").view(view("v")).subtabs(Subtab.create("s", "S").view(view("s"))));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("exactly one"), e::getMessage);
	}

	@Test void a07_tabWithViewOnly_stillAccepted() {
		// Regression: the pre-420 leaf-view shape must still build cleanly.
		var page = PageDef.create("docs").tabs(Tab.create("t", "T").view(view("v"))).build();
		assertNotNull(page.tabs.get(0).view);
	}

	@Test void a08_tabWithSubtabsOnlyNoContent_stillAccepted() {
		// Regression: the pre-420 sub-tabbed shape (no preamble) must still build cleanly.
		var page = PageDef.create("docs")
			.tabs(Tab.create("t", "T").subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		assertNull(page.tabs.get(0).content);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Subtab matrix: {view} | {content}
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_subtabWithContentOnly_accepted() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("s", "S").content("<p>Body.</p>")))
			.build();
		assertEquals("<p>Body.</p>", page.tabs.get(0).subtabs.get(0).content);
		assertNull(page.tabs.get(0).subtabs.get(0).view);
	}

	@Test void b02_subtabWithViewOnly_stillAccepted() {
		// Regression: the pre-420 subtab-must-have-a-view shape still builds cleanly.
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("s", "S").view(view("s"))))
			.build();
		assertNull(page.tabs.get(0).subtabs.get(0).content);
	}

	@Test void b03_subtabWithBothViewAndContent_rejected() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog")
				.subtabs(Subtab.create("s", "S").view(view("s")).content("<p>x</p>")));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("exactly one"), e::getMessage);
	}

	@Test void b04_subtabWithNeitherViewNorContent_rejected() {
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("s", "S")));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("exactly one"), e::getMessage);
	}

	@Test void b05_mixedViewAndContentSubtabsUnderOneTab_accepted() {
		// A tab may mix view-backed and content-backed subtabs freely.
		var page = PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog").subtabs(
				Subtab.create("data", "Data").view(view("data")),
				Subtab.create("about", "About").content("<p>About this catalog.</p>")))
			.build();
		assertNotNull(page.tabs.get(0).subtabs.get(0).view);
		assertNotNull(page.tabs.get(0).subtabs.get(1).content);
	}

	//------------------------------------------------------------------------------------------------------------------
	// PageDef.build() consequences: must not NPE on content-only shapes (view-id dedup logic)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_contentOnlyTab_doesNotNpeOnViewIdDedup() {
		// Pre-420, PageDef.validate() unconditionally iterated t.subtabs when t.view was null - a content-only
		// tab (no subtabs at all) would NPE there. Must build cleanly instead.
		assertDoesNotThrow(() -> PageDef.create("docs")
			.tabs(Tab.create("overview", "Overview").content("<p>Hi.</p>"))
			.build());
	}

	@Test void c02_contentOnlySubtab_doesNotNpeOnViewIdDedup() {
		// Pre-420, PageDef.validate() unconditionally read s.view.id for every subtab - a content-only subtab
		// (s.view == null) would NPE there. Must build cleanly instead.
		assertDoesNotThrow(() -> PageDef.create("docs")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("s", "S").content("<p>Hi.</p>")))
			.build());
	}

	@Test void c03_multipleContentOnlyTabs_doNotCollideOnAbsentViewIds() {
		// Two content-only tabs (no ViewDef at all) must not be mistaken for a duplicate-view-id collision.
		var page = PageDef.create("docs")
			.tabs(
				Tab.create("a", "A").content("<p>A</p>"),
				Tab.create("b", "B").content("<p>B</p>"))
			.build();
		assertEquals(2, page.tabs.size());
	}

	@Test void c04_duplicateViewIdAcrossAContentTabAndAViewTab_stillDetected() {
		// The dedup rule itself must still fire when an actual duplicate view id IS present, alongside an
		// unrelated content-only tab.
		var page = PageDef.create("docs")
			.tabs(
				Tab.create("a", "A").content("<p>A</p>"),
				Tab.create("b", "B").view(view("shared")),
				Tab.create("c", "C").view(view("shared")));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("shared"), e::getMessage);
	}
}
