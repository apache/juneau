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
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * {@link NestedTableDef#validate()} matrix: happy path, parent-scope param grammar / reserved keys, the nested
 * {@code dataUrl} rule ({@code servlet:} and relative allowed; {@code ://} / {@code //} / {@code javascript:} /
 * {@code data:} / {@code ..} rejected), depth-1 enforcement, and the read-only ({@code g4}) forbids.
 */
class NestedTableDef_Test extends TestBase {

	private static ViewDef nestedView() {
		return ViewDef.create("events").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/events")
			.columns(Column.of("name")).build();
	}

	private static ViewDef nestedView(String dataUrl) {
		return ViewDef.create("events").dataMode(ViewDef.DataMode.CLIENT).dataUrl(dataUrl)
			.columns(Column.of("name")).build();
	}

	@Test void a01_valid_minimal() {
		NestedTableDef.create(nestedView()).validate();
	}

	@Test void a02_contractVersion_isOne_andIndependent() {
		assertEquals("1", NestedTableDef.CONTRACT_VERSION);
		assertEquals("4", ViewDef.CONTRACT_VERSION);
		assertEquals("1", RowDetailDef.CONTRACT_VERSION);
	}

	@Test void a03_defaultScopeParam_isParentId() {
		assertEquals("parentId", NestedTableDef.create(nestedView()).parentScopeParam);
	}

	@Test void a04_nullView_rejected() {
		var e = assertThrows(IllegalArgumentException.class, () -> NestedTableDef.create(null).validate());
		assertTrue(e.getMessage().contains("view"), e::getMessage);
	}

	@Test void a05_blankScopeParam_rejected() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> NestedTableDef.create(nestedView()).parentScopeParam("  ").validate());
		assertTrue(e.getMessage().contains("parentScopeParam"), e::getMessage);
	}

	@Test void a06_illegalScopeParamChars_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> NestedTableDef.create(nestedView()).parentScopeParam("1abc").validate());
		assertThrows(IllegalArgumentException.class,
			() -> NestedTableDef.create(nestedView()).parentScopeParam("a-b").validate());
		assertThrows(IllegalArgumentException.class,
			() -> NestedTableDef.create(nestedView()).parentScopeParam("a.b").validate());
	}

	@Test void a07_reservedDataTablesKeys_rejected() {
		for (var k : new String[]{"draw", "start", "length", "search", "columns", "order", "_"}) {
			var e = assertThrows(IllegalArgumentException.class,
				() -> NestedTableDef.create(nestedView()).parentScopeParam(k).validate(),
				() -> "expected '" + k + "' to be rejected");
			assertTrue(e.getMessage().contains("reserved"), e::getMessage);
		}
	}

	@Test void a08_customScopeParam_accepted() {
		NestedTableDef.create(nestedView()).parentScopeParam("alertId").validate();
		NestedTableDef.create(nestedView()).parentScopeParam("parent_id_1").validate();
	}

	@Test void a09_blankDataUrl_rejected() {
		var v = ViewDef.create("events").dataMode(ViewDef.DataMode.CLIENT).columns(Column.of("name")).build();
		var e = assertThrows(IllegalArgumentException.class, () -> NestedTableDef.create(v).validate());
		assertTrue(e.getMessage().contains("dataUrl"), e::getMessage);
	}

	@Test void a10_servletDataUrl_allowed() {
		NestedTableDef.create(nestedView("servlet:/data/events")).validate();
	}

	@Test void a11_relativeDataUrl_allowed() {
		NestedTableDef.create(nestedView("data/events")).validate();
		NestedTableDef.create(nestedView("/data/events")).validate();
	}

	@Test void a12_absoluteDataUrl_rejected() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> NestedTableDef.create(nestedView("https://evil/data")).validate());
		assertTrue(e.getMessage().contains("dataUrl"), e::getMessage);
	}

	@Test void a13_protocolRelativeDataUrl_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> NestedTableDef.create(nestedView("//evil/data")).validate());
	}

	@Test void a14_javascriptScheme_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> NestedTableDef.create(nestedView("javascript:alert(1)")).validate());
	}

	@Test void a15_dataScheme_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> NestedTableDef.create(nestedView("data:text/html,x")).validate());
	}

	@Test void a16_dotDotSegment_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> NestedTableDef.create(nestedView("/data/../secrets/events")).validate());
	}

	@Test void a17_depthOneOnly_nestedDetailsRejected() {
		var deep = ViewDef.create("events").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/events")
			.columns(Column.of("name"))
			.details(RowDetailDef.create().endpoint("/data/events/{id}")
				.sections(DetailSection.create("info", "Info").fields(DetailField.of("owner"))))
			.build();
		var e = assertThrows(IllegalArgumentException.class, () -> NestedTableDef.create(deep).validate());
		assertTrue(e.getMessage().contains("depth"), e::getMessage);
	}

	@Test void a18_readOnly_rowActionsRejected() {
		var v = ViewDef.create("events").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/events")
			.columns(Column.of("name"))
			.rowActions(RowAction.create("ack").endpoint("/x").method(RowAction.Method.POST))
			.build();
		var e = assertThrows(IllegalArgumentException.class, () -> NestedTableDef.create(v).validate());
		assertTrue(e.getMessage().contains("read-only"), e::getMessage);
	}

	@Test void a19_readOnly_columnConfigRejected() {
		var v = ViewDef.create("events").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/events")
			.columns(Column.of("name"))
			.columnConfig(ColumnConfig.create())
			.build();
		var e = assertThrows(IllegalArgumentException.class, () -> NestedTableDef.create(v).validate());
		assertTrue(e.getMessage().contains("read-only"), e::getMessage);
	}

	@Test void a20_isSafeNestedDataUrl_matrix() {
		assertTrue(NestedTableDef.isSafeNestedDataUrl("/data/events"));
		assertTrue(NestedTableDef.isSafeNestedDataUrl("data/events"));
		assertTrue(NestedTableDef.isSafeNestedDataUrl("servlet:/data/events"));
		assertFalse(NestedTableDef.isSafeNestedDataUrl("https://evil/data"));
		assertFalse(NestedTableDef.isSafeNestedDataUrl("//evil/data"));
		assertFalse(NestedTableDef.isSafeNestedDataUrl("javascript:alert(1)"));
		assertFalse(NestedTableDef.isSafeNestedDataUrl("data:text/html,x"));
		assertFalse(NestedTableDef.isSafeNestedDataUrl("/data/../x"));
		assertFalse(NestedTableDef.isSafeNestedDataUrl("  "));
		assertFalse(NestedTableDef.isSafeNestedDataUrl(null));
	}
}
