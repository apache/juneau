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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** Tests for {@link Ribbons}: the standard table ribbon preset. */
class Ribbons_Test extends TestBase {

	private static List<Object> ids(List<RibbonItem> items) {
		return items.stream().map(i -> i.toMap().get("id")).toList();
	}

	private static List<Object> groups(List<RibbonItem> items) {
		return items.stream().map(i -> i.toMap().get("group")).toList();
	}

	private static RibbonItem find(List<RibbonItem> items, String id) {
		return items.stream().filter(i -> id.equals(i.toMap().get("id"))).findFirst().orElse(null);
	}

	@Test void a01_default_order_groups_and_ids() {
		var items = Ribbons.standard().build();
		assertEquals(List.of("search", "export", "link", "collapse", "columns", "reset", "refresh"), ids(items));
		assertEquals(List.of("search", "export", "export", "viewing", "viewing", "viewing", "page"), groups(items));
	}

	@Test void a02_create_and_filter_slots_regroup_and_order() {
		var items = Ribbons.standard()
			.create(RibbonItem.dialog("add-pet").title("Add pet"))
			.filter(RibbonItem.optionGroup("status", RibbonItem.option("a").title("A")))
			.build();
		assertEquals(List.of("search", "add-pet", "status", "export", "link", "collapse", "columns", "reset", "refresh"), ids(items));
		assertEquals("entity", items.get(1).toMap().get("group"));
		assertEquals("filter", items.get(2).toMap().get("group"));
	}

	@Test void a03_create_filter_repeatable() {
		var items = Ribbons.standard()
			.create(RibbonItem.dialog("c1")).create(RibbonItem.dialog("c2"))
			.filter(RibbonItem.optionGroup("f1", RibbonItem.option("a"))).filter(RibbonItem.optionGroup("f2", RibbonItem.option("b")))
			.build();
		assertEquals(List.of("search", "c1", "c2", "f1", "f2"), ids(items).subList(0, 5));
	}

	@Test void a04_export_extras_are_optional() {
		var exp = find(Ribbons.standard().export("excel", "pdf").build(), "export").toMap();
		assertEquals(List.of("copy", "csv"), exp.get("buttons"));
		assertEquals(List.of("excel", "pdf"), exp.get("optional"));
	}

	@Test void a05_without_removes_buttons_and_items() {
		assertEquals(List.of("csv"), find(Ribbons.standard().without("copy").build(), "export").toMap().get("buttons"));
		assertFalse(ids(Ribbons.standard().without("link").build()).contains("link"));
		assertFalse(ids(Ribbons.standard().without("export").build()).contains("export"));
		assertTrue(ids(Ribbons.standard().without("export").build()).contains("link"));
		assertNull(find(Ribbons.standard().without("copy").without("csv").build(), "export"));
		assertFalse(ids(Ribbons.standard().without("search").build()).contains("search"));
	}

	@Test void a06_without_excel_removes_extra() {
		var exp = find(Ribbons.standard().export("excel", "pdf").without("excel").build(), "export").toMap();
		assertEquals(List.of("pdf"), exp.get("optional"));
	}

	@Test void a07_without_rejects_unknown_and_type_aliases() {
		for (var bad : List.of("nope", "collapseAll", "resetDefaults", "columnSettings", "linkButton", "create")) {
			var e = assertThrows(IllegalArgumentException.class, () -> Ribbons.standard().without(bad).build(), bad);
			assertTrue(e.getMessage().contains("collapse") && e.getMessage().contains("columns"), "lists valid ids: " + e.getMessage());
		}
	}

	@Test void a08_refreshId_override_and_without_accepts_override() {
		var items = Ribbons.standard().refreshId("groom-refresh").build();
		assertTrue(ids(items).contains("groom-refresh"));
		assertFalse(ids(Ribbons.standard().refreshId("groom-refresh").without("groom-refresh").build()).contains("groom-refresh"));
		assertFalse(ids(Ribbons.standard().without("groom-refresh").refreshId("groom-refresh").build()).contains("groom-refresh"));
	}

	@Test void a09_refresh_has_page_group() {
		var items = Ribbons.standard().build();
		assertEquals("page", items.get(items.size() - 1).toMap().get("group"));
	}

	@Test void a10_every_item_has_an_id_and_validates() {
		for (var i : Ribbons.standard().export("excel").build()) {
			assertNotNull(i.toMap().get("id"));
			RibbonItem.validate(i.toMap());
		}
	}

	@Test void a11_duplicate_id_between_custom_and_preset_throws_at_tableSpec() {
		var items = Ribbons.standard().create(RibbonItem.dialog("refresh")).build();
		assertThrows(IllegalArgumentException.class, () -> TableSpec.create("t").ribbon(items));
	}

	@Test void a12_tableSpec_ribbon_list_equals_varargs() {
		var items = Ribbons.standard().build();
		var a = TableSpec.create("t").dataUrl("/d").ribbon(items);
		var b = TableSpec.create("t").dataUrl("/d").ribbon(items.toArray(RibbonItem[]::new));
		a.columns(Column.create("x")); b.columns(Column.create("x"));
		assertEquals(a.toCardBody(), b.toCardBody());
	}

	@Test void a13_preset_validates_in_a_table() {
		TableSpec.create("t").dataUrl("/d").ribbon(Ribbons.standard().export("excel").build());
	}

	@Test void z01_preset_order_matches_the_runtime_canonical_group_order() throws Exception {
		String js;
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RIBBON_JS_RESOURCE)) {
			js = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
		var m = java.util.regex.Pattern.compile("const CANONICAL_GROUPS = \\[([^\\]]*)\\]").matcher(js);
		assertTrue(m.find());
		var canonical = Arrays.stream(m.group(1).split(",")).map(x -> x.trim().replace("\"", "")).toList();
		var items = Ribbons.standard().create(RibbonItem.dialog("c")).filter(RibbonItem.optionGroup("f", RibbonItem.option("a")))
			.export("excel").build();
		int last = -1;
		for (var g : groups(items)) {
			int r = canonical.indexOf(g);
			assertTrue(r >= 0, "group not canonical: " + g);
			assertTrue(r >= last, "preset order disagrees with the runtime at group " + g);
			last = r;
		}
	}
}
