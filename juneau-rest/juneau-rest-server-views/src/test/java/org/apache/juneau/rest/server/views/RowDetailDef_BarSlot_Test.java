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
 * Bean contract for the <b>second</b> named {@link BarSlot} attachment, {@link RowDetailDef#barSlot}.
 *
 * <p>
 * The type is deliberately shared with {@link PageDef#barSlot} while the <i>host</i> is not: a detail slot rides the
 * client-built detail ribbon inside the row-expand {@code <template>}, a page slot trails the page sub-tab bar.  This
 * class pins three things: the cascade into {@link BarSlot#validate()}; the cross-host duplicate-id rejection, which
 * can only live on {@link PageDef} because that is the only scope seeing both hosts; and that adding a Java-only
 * template field bumps <b>no</b> contract version (the row-expand envelope handshake is unchanged).
 */
class RowDetailDef_BarSlot_Test extends TestBase {

	private static RowDetailDef details(BarSlot bar) {
		return RowDetailDef.create()
			.endpoint("/data/alerts/{id}")
			.sections(DetailSection.create("overview", "Overview").fields(DetailField.of("title").title("Title")))
			.barSlot(bar);
	}

	private static ViewDef view(String id, BarSlot bar) {
		return ViewDef.create(id)
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/" + id)
			.columns(Column.of("id").title("Id"))
			.details(details(bar));
	}

	private static BarSlot bar(String id) {
		return BarSlot.create(id).widgets(BarBadge.of("open").label("Open").badge(Badge.count(3)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Field + fluent setter + validate cascade
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_fluentSetter_storesSlot() {
		var d = details(bar("detail-bar"));
		assertNotNull(d.barSlot);
		assertEquals("detail-bar", d.barSlot.id);
	}

	@Test void a02_nullSlot_isLegal() {
		assertDoesNotThrow(() -> details(null).validate(null));
	}

	@Test void a03_validSlot_passes() {
		assertDoesNotThrow(() -> details(bar("detail-bar")).validate(null));
	}

	@Test void a04_cascadesIntoBarSlotValidate_blankId() {
		var d = details(BarSlot.create(" ").widgets(BarText.of("x", "X")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("BarSlot"), e::getMessage);
	}

	@Test void a05_cascadesIntoBarSlotValidate_noWidgets() {
		var d = details(BarSlot.create("detail-bar"));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("BarSlot"), e::getMessage);
	}

	@Test void a06_cascadesIntoBarSlotValidate_badRefreshUrl() {
		var d = details(bar("detail-bar").refreshUrl("http://evil.example/counts"));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("BarSlot"), e::getMessage);
	}

	@Test void a07_cascadeSurvivesTheViewLevelEntryPoint() {
		var v = view("alerts", BarSlot.create("detail-bar"));
		var e = assertThrows(IllegalArgumentException.class, v::validate);
		assertTrue(e.getMessage().contains("BarSlot"), e::getMessage);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Cross-host duplicate id: rejected on PageDef, since only PageDef sees both hosts
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_duplicateIdAcrossHosts_rejected() {
		var page = PageDef.create("admin")
			.barSlot(bar("ctx"))
			.tabs(Tab.create("t", "T").view(view("alerts", bar("ctx"))));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("ctx"), e::getMessage);
		assertTrue(e.getMessage().contains("bar slot"), e::getMessage);
	}

	@Test void b02_duplicateIdUnderASubtabView_rejected() {
		var page = PageDef.create("admin")
			.barSlot(bar("ctx"))
			.tabs(Tab.create("t", "T").subtabs(Subtab.create("s", "S").view(view("alerts", bar("ctx")))));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("ctx"), e::getMessage);
	}

	@Test void b03_duplicateIdInsideANestedDetailTableView_rejected() {
		var nested = ViewDef.create("nested")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/nested")
			.columns(Column.of("id").title("Id"))
			.details(details(bar("ctx")));
		var outer = ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(DetailSection.create("overview", "Overview")
					.fields(DetailField.of("title").title("Title"))
					.table(NestedTableDef.create(nested))));
		var page = PageDef.create("admin").barSlot(bar("ctx")).tabs(Tab.create("t", "T").view(outer));
		var e = assertThrows(IllegalArgumentException.class, page::build);
		assertTrue(e.getMessage().contains("ctx"), e::getMessage);
	}

	@Test void b04_distinctIdsAcrossHosts_accepted() {
		var page = PageDef.create("admin")
			.barSlot(bar("page-ctx"))
			.tabs(Tab.create("t", "T").view(view("alerts", bar("detail-ctx"))));
		assertDoesNotThrow(page::build);
	}

	@Test void b05_pageWithoutBarSlot_hasNothingToCollideWith() {
		var page = PageDef.create("admin").tabs(Tab.create("t", "T").view(view("alerts", bar("ctx"))));
		assertDoesNotThrow(page::build);
	}

	@Test void b06_topLevelViewWithNoEnclosingPage_isALegalNoOp() {
		// No page => no page bar slot => no cross-host collision to check.  This must NOT be an error, and the
		// detail slot's own cascade must still run.
		assertDoesNotThrow(() -> view("alerts", bar("ctx")).validate());
		assertDoesNotThrow(() -> Html.of(ViewTable.of(view("alerts", bar("ctx")).build())));
	}

	//------------------------------------------------------------------------------------------------------------------
	// No contract bump anywhere
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_rowDetailContractVersionUnchanged() {
		assertEquals("1", RowDetailDef.CONTRACT_VERSION);
	}

	@Test void c02_pageContractVersionUnchanged() {
		assertEquals("4", PageDef.CONTRACT_VERSION);
	}

	@Test void c03_barSlotContractVersionUnchanged() {
		assertEquals("1", BarSlot.CONTRACT_VERSION);
	}
}
