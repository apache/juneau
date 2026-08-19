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
 * Builder + JSON-serialization contract test for {@link PageDef}/{@link Tab}/{@link Subtab} (Phase C, Task 1).
 *
 * <p>
 * Mirrors {@code ViewMeta_Contract_Test}'s structural-compare style: pins the builder round-trip, the ordered
 * tab/subtab lists, and the serialized shape (baked {@code contractVersion}, preserved ids) without over-specifying
 * whitespace/key-order beyond the top-level discriminator fields.
 */
class PageDef_Contract_Test extends TestBase {

	public static class Release {
		public String name;
		public String status;
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
	// Builder round-trip
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_builderRoundTrip_leafTabsOnly() {
		var page = PageDef.create("admin")
			.title("Admin")
			.tabs(
				Tab.create("releases", "Releases").view(view("releases")),
				Tab.create("users", "Users").view(view("users")))
			.build();

		assertEquals("admin", page.id);
		assertEquals("Admin", page.title);
		assertEquals(2, page.tabs.size());
		assertEquals("releases", page.tabs.get(0).id);
		assertEquals("Releases", page.tabs.get(0).label);
		assertEquals("releases", page.tabs.get(0).view.id);
		assertEquals("users", page.tabs.get(1).id);
	}

	@Test void a02_builderRoundTrip_withSubtabs() {
		var page = PageDef.create("admin")
			.tabs(
				Tab.create("releases", "Releases").view(view("releases")),
				Tab.create("catalog", "Catalog").subtabs(
					Subtab.create("packages", "Packages").view(view("packages")),
					Subtab.create("bundles", "Bundles").view(view("bundles"))))
			.build();

		var catalog = page.tabs.get(1);
		assertNull(catalog.view);
		assertEquals(2, catalog.subtabs.size());
		assertEquals("packages", catalog.subtabs.get(0).id);
		assertEquals("Packages", catalog.subtabs.get(0).label);
		assertEquals("packages", catalog.subtabs.get(0).view.id);
		assertEquals("bundles", catalog.subtabs.get(1).id);
	}

	@Test void a03_contractVersionDefaultsToViewDefContractVersion() {
		var page = PageDef.create("admin").tabs(Tab.create("t", "T").view(view("v"))).build();
		assertEquals(ViewDef.CONTRACT_VERSION, page.contractVersion);
	}

	@Test void a04_tabOrderPreserved() {
		var page = PageDef.create("admin")
			.tabs(
				Tab.create("c", "C").view(view("c")),
				Tab.create("a", "A").view(view("a")),
				Tab.create("b", "B").view(view("b")))
			.build();
		assertEquals(List.of("c", "a", "b"), page.tabs.stream().map(t -> t.id).toList());
	}

	//------------------------------------------------------------------------------------------------------------------
	// JSON serialization shape
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_serializesWithContractVersionAndIds() {
		var page = PageDef.create("admin")
			.title("Admin")
			.tabs(Tab.create("releases", "Releases").view(view("releases")))
			.build();
		var json = Json.of(page);
		assertTrue(json.contains("\"contractVersion\":\"3\""), json);
		assertTrue(json.contains("\"id\":\"admin\""), json);
		assertTrue(json.contains("\"title\":\"Admin\""), json);
		assertTrue(json.contains("\"tabs\":["), json);
	}

	@Test void b02_leafTabSerializesViewNotSubtabs() {
		var page = PageDef.create("admin").tabs(Tab.create("releases", "Releases").view(view("releases"))).build();
		var json = Json.of(page);
		assertTrue(json.contains("\"view\":"), json);
		assertFalse(json.contains("\"subtabs\""), json);
	}

	@Test void b03_subtabbedTabSerializesSubtabsNotView() {
		var page = PageDef.create("admin")
			.tabs(Tab.create("catalog", "Catalog").subtabs(Subtab.create("packages", "Packages").view(view("packages"))))
			.build();
		var json = Json.of(page);
		assertTrue(json.contains("\"subtabs\":["), json);
		// The Tab's own "view" key must be omitted (not null) when subtabs are used instead.
		assertFalse(json.contains("\"view\":null"), json);
	}

	@Test void b04_titleOmittedWhenUnset() {
		// NOTE: checked structurally (not via a raw substring search) because the referenced view's own Column
		// carries an unrelated "title" key - a substring check would false-positive on that nested field.
		var page = PageDef.create("admin").tabs(Tab.create("t", "T").view(view("v"))).build();
		var top = Json.to(Json.of(page), java.util.Map.class);
		assertFalse(top.containsKey("title"), () -> Json.of(page));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent-setter null-safety on Tab/Subtab labels
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_tabCreateNullIdThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> Tab.create(null, "Label"));
		assertTrue(e.getMessage().contains("null"), e::getMessage);
	}

	@Test void c02_subtabCreateBlankIdThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> Subtab.create("   ", "Label"));
		assertTrue(e.getMessage().contains("blank"), e::getMessage);
	}

	@Test void c03_pageDefCreateNullIdThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> PageDef.create(null));
		assertTrue(e.getMessage().contains("null"), e::getMessage);
	}
}
