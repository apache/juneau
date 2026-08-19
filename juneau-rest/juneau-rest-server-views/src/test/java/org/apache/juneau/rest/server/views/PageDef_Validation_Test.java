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
 * Validation contract test for {@link PageDef}/{@link Tab}/{@link Subtab} (Phase C, Task 2, design doc §"Bean
 * model").
 *
 * <p>
 * Pins the four rejection rules: a {@link Tab} declaring both/neither {@code view}/{@code subtabs}; duplicate
 * {@code Tab.id} within a page; duplicate {@code Subtab.id} within a tab; and a referenced {@link ViewDef#id}
 * repeated anywhere across the page (so hash routing + sidecar lookup stay unambiguous).
 *
 * <p>
 * <b>Resolution of the plan's "unique Tab.id / Subtab.id within a page" wording:</b> {@code Tab.id} is required
 * unique across the whole page (it is the second hash segment, {@code #pageId/tabId/...}); {@code Subtab.id} is
 * required unique only within its own parent {@code Tab} (it is only ever resolved <i>after</i> the tab id has
 * already disambiguated the parent, per the {@code #pageId/tabId/subtabId} hash shape) &mdash; so the same subtab
 * id may safely recur under two different tabs.
 */
class PageDef_Validation_Test extends TestBase {

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
	// Tab must have exactly one of view/subtabs
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_tabWithBothViewAndSubtabs_rejected() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("t", "T").view(view("v")).subtabs(Subtab.create("s", "S").view(view("s"))));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("exactly one"), e::getMessage);
	}

	@Test void a02_tabWithNeitherViewNorSubtabs_rejected() {
		var page = PageDef.create("admin").tabs(Tab.create("t", "T"));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("exactly one"), e::getMessage);
	}

	@Test void a03_tabWithEmptySubtabsListAndNoView_rejected() {
		// An explicitly-empty subtabs() call is equivalent to "no subtabs" -> still neither.
		var page = PageDef.create("admin").tabs(Tab.create("t", "T").subtabs());
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("exactly one"), e::getMessage);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Duplicate tab ids (page-wide)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_duplicateTabId_rejected() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("releases", "R1").view(view("r1")), Tab.create("releases", "R2").view(view("r2")));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("releases"), e::getMessage);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Duplicate subtab ids (within one tab)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_duplicateSubtabIdWithinSameTab_rejected() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("catalog", "Catalog").subtabs(
				Subtab.create("packages", "P1").view(view("p1")),
				Subtab.create("packages", "P2").view(view("p2"))));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("packages"), e::getMessage);
	}

	@Test void c02_sameSubtabIdAcrossDifferentTabs_allowed() {
		// Different parent tabs -> no collision (the hash's tabId segment already disambiguates).
		var page = PageDef.create("admin")
			.tabs(
				Tab.create("a", "A").subtabs(Subtab.create("x", "X").view(view("ax"))),
				Tab.create("b", "B").subtabs(Subtab.create("x", "X").view(view("bx"))))
			.build();
		assertEquals("x", page.tabs.get(0).subtabs.get(0).id);
		assertEquals("x", page.tabs.get(1).subtabs.get(0).id);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Duplicate referenced ViewDef.id across the whole page
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_duplicateViewIdAcrossTabs_rejected() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("a", "A").view(view("shared")), Tab.create("b", "B").view(view("shared")));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("shared"), e::getMessage);
	}

	@Test void d02_duplicateViewIdBetweenTabAndSubtab_rejected() {
		var page = PageDef.create("admin")
			.tabs(
				Tab.create("a", "A").view(view("shared")),
				Tab.create("b", "B").subtabs(Subtab.create("s", "S").view(view("shared"))));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("shared"), e::getMessage);
	}

	@Test void d03_uniqueViewIds_accepted() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("a", "A").view(view("va")), Tab.create("b", "B").view(view("vb")))
			.build();
		assertEquals(2, page.tabs.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PageDef requires at least one tab
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_noTabs_rejected() {
		var page = PageDef.create("admin");
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("tab"), e::getMessage);
	}

	@Test void e02_emptyTabsList_rejected() {
		var page = PageDef.create("admin").tabs();
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("tab"), e::getMessage);
	}
}
