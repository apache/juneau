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
import org.junit.jupiter.api.*;

/** {@link RibbonItem}: the ribbon-item vocabulary {@code juneau-ribbon.js} reads. */
class RibbonItem_Test extends TestBase {

	@Test void a01_refresh_toMap() {
		var m = RibbonItem.refresh().toMap();
		assertEquals("refresh", m.get("type"));
		assertEquals(Set.of("type"), m.keySet());
	}

	@Test void a02_collapseAll_pausePolling_toMap() {
		assertEquals("collapseAll", RibbonItem.collapseAll().toMap().get("type"));
		assertEquals("pausePolling", RibbonItem.pausePolling().toMap().get("type"));
	}

	@Test void a03_export_toMap() {
		var m = RibbonItem.export("copy", "csv", "print", "pdf").toMap();
		assertEquals("export", m.get("type"));
		assertEquals(List.of("copy", "csv", "print", "pdf"), m.get("buttons"));
		assertFalse(m.containsKey("optional"));
	}

	@Test void a04_export_optional() {
		var m = RibbonItem.export("copy").optional("excel", "pdf").toMap();
		assertEquals(List.of("excel", "pdf"), m.get("optional"));
	}

	@Test void a05_export_unknownButton_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> RibbonItem.export("copy", "xml"));
		assertEquals("RibbonItem export button 'xml' is not one of 'copy, csv, print, excel, pdf'.", ex.getMessage());
	}

	@Test void a06_export_emptyButtons_buildsButToMapRejects() {
		var item = RibbonItem.export();
		var ex = assertThrows(IllegalArgumentException.class, item::toMap);
		assertEquals("RibbonItem export requires at least one button.", ex.getMessage());
	}

	@Test void a07_optional_unknown_rejected() {
		var item = RibbonItem.export("copy");
		var ex = assertThrows(IllegalArgumentException.class, () -> item.optional("csv"));
		assertEquals("RibbonItem optional export button 'csv' is not one of 'excel, pdf'.", ex.getMessage());
	}

	@Test void a08_dialog_fullChain_toMap() {
		var m = RibbonItem.dialog("add")
			.title("Add repo")
			.form("/rest/skill-repos/add-modal")
			.endpoint("/rest/skill-repos/new")
			.method(RowAction.Method.POST)
			.onSuccess(RowAction.OnSuccess.REDRAW)
			.toMap();
		assertEquals("dialog", m.get("type"));
		assertEquals("add", m.get("id"));
		assertEquals("Add repo", m.get("title"));
		assertEquals("/rest/skill-repos/add-modal", m.get("form"));
		assertEquals("/rest/skill-repos/new", m.get("endpoint"));
		assertEquals("POST", m.get("method"));
		assertEquals("redraw", m.get("onSuccess"));
	}

	@Test void a09_dialog_blankActionId_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> RibbonItem.dialog(" "));
		assertEquals("RibbonItem dialog action id must not be null or blank.", ex.getMessage());
	}

	@Test void a10_dialog_minimal_omitsUnsetKeys() {
		assertEquals(Set.of("type", "id"), RibbonItem.dialog("add").toMap().keySet());
	}

	@Test void a11_title_onNonDialogItem_stillAllowed() {
		assertEquals("Reload", RibbonItem.refresh().title("Reload").toMap().get("title"));
	}

	@Test void a12_commonFields_groupAppearanceSymbol() {
		var m = RibbonItem.refresh().group("g").appearance("icon").symbol("sync").toMap();
		assertEquals("g", m.get("group"));
		assertEquals("icon", m.get("appearance"));
		assertEquals("sync", m.get("symbol"));
	}

	@Test void a13_appearance_unknown_rejected() {
		var item = RibbonItem.refresh();
		var ex = assertThrows(IllegalArgumentException.class, () -> item.appearance("text"));
		assertEquals("RibbonItem appearance 'text' is not one of 'icon'.", ex.getMessage());
	}

	@Test void a14_type_isFirstKey() {
		assertEquals("type", RibbonItem.refresh().title("x").toMap().keySet().iterator().next());
	}

	@Test void a15_option_column() {
		var m = RibbonItem.option("dropped-only").title("Dropped only").column("status").value("$eq(DROPPED)")
			.persist(true).defaultOn().toMap();
		assertEquals("option", m.get("type"));
		assertEquals("status", m.get("column"));
		assertEquals("$eq(DROPPED)", m.get("value"));
		assertEquals(true, m.get("persist"));
		assertEquals(true, m.get("default"));
		assertFalse(m.containsKey("param"));
	}

	@Test void a16_option_param() {
		var m = RibbonItem.option("mine").param("owner").value("me").toMap();
		assertEquals("owner", m.get("param"));
		assertFalse(m.containsKey("column"));
	}

	@Test void a17_option_blankId_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> RibbonItem.option(""));
		assertEquals("RibbonItem option id must not be null or blank.", ex.getMessage());
	}

	@Test void a18_option_noScope_rejected() {
		var item = RibbonItem.option("x").value("v");
		var ex = assertThrows(IllegalArgumentException.class, item::toMap);
		assertEquals("RibbonItem option 'x' sets neither column nor param.", ex.getMessage());
	}

	@Test void a19_option_bothScopes_rejected() {
		var item = RibbonItem.option("x").column("c").param("p");
		var ex = assertThrows(IllegalArgumentException.class, item::toMap);
		assertEquals("RibbonItem option 'x' sets both column 'c' and param 'p'.", ex.getMessage());
	}

	@Test void a20_optionGroup_toMap_membersHaveNoType() {
		var m = RibbonItem.optionGroup("phase",
				RibbonItem.option("all").title("All"),
				RibbonItem.option("pending").column("phase").value("$eq(Waiting)"))
			.persist(true).deselectable(true).defaultOption("pending").toMap();
		assertEquals("optionGroup", m.get("type"));
		assertEquals("pending", m.get("default"));
		assertEquals(true, m.get("deselectable"));
		var options = (List<?>)m.get("options");
		assertEquals(2, options.size());
		assertEquals(Map.of("id", "all", "title", "All"), options.get(0));
		assertFalse(((Map<?,?>)options.get(1)).containsKey("type"));
	}

	@Test void a21_optionGroup_defaultNamesNoMember_rejected() {
		var g = RibbonItem.optionGroup("phase", RibbonItem.option("a")).defaultOption("zzz");
		var ex = assertThrows(IllegalArgumentException.class, g::toMap);
		assertEquals("RibbonItem optionGroup 'phase' default 'zzz' is not one of its options.", ex.getMessage());
	}

	@Test void a22_optionGroup_empty_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> RibbonItem.optionGroup("phase"));
		assertEquals("RibbonItem optionGroup 'phase' requires at least one option.", ex.getMessage());
	}

	@Test void a23_optionGroup_nonOptionMember_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> RibbonItem.optionGroup("phase", RibbonItem.refresh()));
		assertEquals("RibbonItem optionGroup 'phase' member must be an option, not 'refresh'.", ex.getMessage());
	}

	@Test void a24_optionGroup_memberBothScopes_rejected() {
		var g = RibbonItem.optionGroup("phase", RibbonItem.option("a").column("c").param("p"));
		assertThrows(IllegalArgumentException.class, g::toMap);
	}

	@Test void a25_export_optionalOnly_allowed() {
		var m = RibbonItem.export().optional("excel").toMap();
		assertFalse(m.containsKey("buttons"));
		assertEquals(List.of("excel"), m.get("optional"));
	}

	@Test void a26_export_buttonBothRequiredAndOptional_rejected() {
		var item = RibbonItem.export("excel");
		var ex = assertThrows(IllegalArgumentException.class, () -> item.optional("excel"));
		assertEquals("RibbonItem export button 'excel' is both required and optional.", ex.getMessage());
	}

	@Test void a27_method_null_rejected() {
		var item = RibbonItem.dialog("add");
		var ex = assertThrows(IllegalArgumentException.class, () -> item.method(null));
		assertEquals("RibbonItem method must not be null.", ex.getMessage());
	}

	@Test void a28_onSuccess_null_rejected() {
		var item = RibbonItem.dialog("add");
		var ex = assertThrows(IllegalArgumentException.class, () -> item.onSuccess(null));
		assertEquals("RibbonItem onSuccess must not be null.", ex.getMessage());
	}

	@Test void a29_defaultOption_onPlainOption_rejected() {
		var item = RibbonItem.option("a").column("c").value("v");
		var ex = assertThrows(IllegalArgumentException.class, () -> item.defaultOption("a"));
		assertEquals("RibbonItem option 'a' cannot take defaultOption(); use defaultOn().", ex.getMessage());
	}

	@Test void a30_defaultOn_onGroup_rejected() {
		var g = RibbonItem.optionGroup("phase", RibbonItem.option("a")).defaultOn();
		assertThrows(IllegalArgumentException.class, g::toMap);
	}

	@Test void a31_topLevelOption_requiresValue() {
		var ex = assertThrows(IllegalArgumentException.class, RibbonItem.option("a").column("c")::toMap);
		assertEquals("RibbonItem option 'a' requires a value.", ex.getMessage());
	}

	@Test void a32_keysTheRuntimeIgnores_rejected() {
		var dlg = RibbonItem.dialog("d");
		assertIgnored("refresh", RibbonItem.refresh().form("f"), "form");
		assertIgnored("refresh", RibbonItem.refresh().endpoint("/x"), "endpoint");
		assertIgnored("refresh", RibbonItem.refresh().method(RowAction.Method.POST), "method");
		assertIgnored("collapseAll", RibbonItem.collapseAll().onSuccess(RowAction.OnSuccess.REDRAW), "onSuccess");
		assertIgnored("pausePolling", RibbonItem.pausePolling().optional("excel"), "optional");
		assertIgnored("dialog", dlg.persist(true), "persist");
		assertIgnored("export", RibbonItem.export("copy").title("t"), "title");
		assertIgnored("export", RibbonItem.export("copy").symbol("s"), "symbol");
		assertIgnored("refresh", RibbonItem.refresh().column("c"), "column");
		assertIgnored("option", RibbonItem.option("a").column("c").value("v").deselectable(true), "deselectable");
		assertIgnored("option", RibbonItem.option("a").column("c").value("v").form("f"), "form");
	}

	private static void assertIgnored(String type, RibbonItem item, String key) {
		var ex = assertThrows(IllegalArgumentException.class, item::toMap, key);
		assertTrue(ex.getMessage().startsWith("RibbonItem " + type), ex.getMessage());
		assertTrue(ex.getMessage().endsWith("does not accept '" + key + "'."), ex.getMessage());
	}

	@Test void a33_optionGroup_ownIgnoredKeys_rejected() {
		for (var k : List.of("title", "symbol", "group")) {
			var g = RibbonItem.optionGroup("phase", RibbonItem.option("a"));
			g = "title".equals(k) ? g.title("t") : "symbol".equals(k) ? g.symbol("s") : g.group("x");
			assertIgnored("optionGroup", g, k);
		}
	}

	@Test void a34_groupMember_ignoredKeys_rejected() {
		var g = RibbonItem.optionGroup("phase", RibbonItem.option("a").persist(true));
		var ex = assertThrows(IllegalArgumentException.class, g::toMap);
		assertEquals("RibbonItem optionGroup 'phase' member 'a' does not accept 'persist'.", ex.getMessage());
		var g2 = RibbonItem.optionGroup("phase", RibbonItem.option("a").group("x"));
		assertThrows(IllegalArgumentException.class, g2::toMap);
		var g3 = RibbonItem.optionGroup("phase", RibbonItem.option("a").defaultOn());
		assertThrows(IllegalArgumentException.class, g3::toMap);
	}
}
