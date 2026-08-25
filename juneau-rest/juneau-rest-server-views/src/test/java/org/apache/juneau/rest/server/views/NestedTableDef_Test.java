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
import org.junit.jupiter.api.*;

/**
 * {@link NestedTableDef#validate()} matrix: happy path, parent-scope param grammar / reserved keys, the nested
 * {@code dataUrl} rule ({@code servlet:} and relative allowed; {@code ://} / {@code //} / {@code javascript:} /
 * {@code data:} / {@code ..} rejected), the {@link NestedTableDef#MAX_DEPTH} cap with path-scoped cycle detection,
 * and the remaining parent-only forbid ({@code columnConfig}).
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

	@Test void a02_contractVersion_isTwo_andIndependent() {
		assertEquals("2", NestedTableDef.CONTRACT_VERSION);
		// Per-widget versioning: widening the nested shell must not drag the view/detail contracts along.
		assertEquals("4", ViewDef.CONTRACT_VERSION);
		assertEquals("1", RowDetailDef.CONTRACT_VERSION);
	}

	@Test void a03_defaultScopeParam_isParentId() {
		assertEquals("parentId", NestedTableDef.create(nestedView()).parentScopeParam);
	}

	@Test void a04_nullView_rejected() {
		var nt = NestedTableDef.create(null);
		var e = assertThrows(IllegalArgumentException.class, nt::validate);
		assertTrue(e.getMessage().contains("view"), e::getMessage);
	}

	@Test void a05_blankScopeParam_rejected() {
		var nt = NestedTableDef.create(nestedView()).parentScopeParam("  ");
		var e = assertThrows(IllegalArgumentException.class, nt::validate);
		assertTrue(e.getMessage().contains("parentScopeParam"), e::getMessage);
	}

	@Test void a06_illegalScopeParamChars_rejected() {
		var nt1 = NestedTableDef.create(nestedView()).parentScopeParam("1abc");
		assertThrows(IllegalArgumentException.class, nt1::validate);
		var nt2 = NestedTableDef.create(nestedView()).parentScopeParam("a-b");
		assertThrows(IllegalArgumentException.class, nt2::validate);
		var nt3 = NestedTableDef.create(nestedView()).parentScopeParam("a.b");
		assertThrows(IllegalArgumentException.class, nt3::validate);
	}

	@Test void a07_reservedDataTablesKeys_rejected() {
		for (var k : new String[]{"draw", "start", "length", "search", "columns", "order", "_"}) {
			var nt = NestedTableDef.create(nestedView()).parentScopeParam(k);
			var e = assertThrows(IllegalArgumentException.class, nt::validate,
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
		var nt = NestedTableDef.create(v);
		var e = assertThrows(IllegalArgumentException.class, nt::validate);
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
		var nt = NestedTableDef.create(nestedView("https://evil/data"));
		var e = assertThrows(IllegalArgumentException.class, nt::validate);
		assertTrue(e.getMessage().contains("dataUrl"), e::getMessage);
	}

	@Test void a13_protocolRelativeDataUrl_rejected() {
		var nt = NestedTableDef.create(nestedView("//evil/data"));
		assertThrows(IllegalArgumentException.class, nt::validate);
	}

	@Test void a14_javascriptScheme_rejected() {
		var nt = NestedTableDef.create(nestedView("javascript:alert(1)"));
		assertThrows(IllegalArgumentException.class, nt::validate);
	}

	@Test void a15_dataScheme_rejected() {
		var nt = NestedTableDef.create(nestedView("data:text/html,x"));
		assertThrows(IllegalArgumentException.class, nt::validate);
	}

	@Test void a16_dotDotSegment_rejected() {
		var nt = NestedTableDef.create(nestedView("/data/../secrets/events"));
		assertThrows(IllegalArgumentException.class, nt::validate);
	}

	@Test void a17_nestedDetailSections_permitted() {
		// A nested view may declare its own detail sections, as long as none of them is another nested table.
		NestedTableDef.create(withDetails("events", null)).validate();
	}

	@Test void a18_rowActions_permitted() {
		NestedTableDef.create(ViewDef.create("events").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/events")
			.columns(Column.of("name"))
			.rowActions(RowAction.create("ack").endpoint("/x").method(RowAction.Method.POST))
			.build()).validate();
	}

	@Test void a19_columnConfigRejected() {
		var v = ViewDef.create("events").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/events")
			.columns(Column.of("name"))
			.columnConfig(ColumnConfig.create())
			.build();
		var nt = NestedTableDef.create(v);
		var e = assertThrows(IllegalArgumentException.class, nt::validate);
		assertTrue(e.getMessage().contains("columnConfig"), e::getMessage);
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

	/**
	 * A view with one detail section whose nested table is {@code inner} (or which is table-less when
	 * {@code inner} is <jk>null</jk>).
	 */
	private static ViewDef withDetails(String id, NestedTableDef inner) {
		var section = DetailSection.create("info", "Info").fields(DetailField.of(id + "Owner"));
		if (inner != null)
			section.table(inner);
		return ViewDef.create(id).dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/" + id)
			.columns(Column.of("name"))
			.details(RowDetailDef.create().endpoint("/data/" + id + "/{id}").sections(section))
			.build();
	}

	@Test void b01_maxDepth_isTwo() {
		assertEquals(2, NestedTableDef.MAX_DEPTH);
	}

	@Test void b02_depthTwo_passes() {
		// Root table = depth 1, this nested table = depth 2 - the cap, not a violation.
		NestedTableDef.create(nestedView()).validate();
	}

	@Test void b03_depthThree_fails() {
		// A nested view that itself declares a nested table would put that table at depth 3.
		var leaf = NestedTableDef.create(ViewDef.create("hosts").dataMode(ViewDef.DataMode.CLIENT)
			.dataUrl("/data/hosts").columns(Column.of("name")).build());
		var nt = NestedTableDef.create(withDetails("events", leaf));
		var e = assertThrows(IllegalArgumentException.class, nt::validate);
		assertTrue(e.getMessage().contains("depth"), e::getMessage);
	}

	@Test void b04_noAuthorMaxDepthField() {
		// Topology IS depth-2: there must be no author-visible knob to declare or clamp it.
		for (var f : NestedTableDef.class.getFields())
			assertNotEquals("maxDepth", f.getName(), "NestedTableDef must not expose an author maxDepth field");
		for (var m : NestedTableDef.class.getMethods())
			assertNotEquals("maxDepth", m.getName(), "NestedTableDef must not expose a maxDepth setter");
	}

	@Test void b05_noBulkMutateField() {
		// Bulk mutation stays on the parent table id only.
		for (var f : NestedTableDef.class.getFields())
			assertNotEquals("bulkMutate", f.getName(), "bulk mutation is parent-table only");
		for (var m : NestedTableDef.class.getMethods())
			assertNotEquals("bulkMutate", m.getName(), "bulk mutation is parent-table only");
	}

	@Test void b06_selfCycle_fails() {
		// A nested view whose own detail section points back at itself.
		var self = ViewDef.create("events").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/events")
			.columns(Column.of("name")).build();
		var back = NestedTableDef.create(self);
		self.details(RowDetailDef.create().endpoint("/data/events/{id}")
			.sections(DetailSection.create("info", "Info").fields(DetailField.of("owner")).table(back)));
		var nt = NestedTableDef.create(self);
		assertThrows(IllegalArgumentException.class, nt::validate);
	}

	@Test void b07_mutualCycle_fails() {
		// a -> b -> a.
		var a = ViewDef.create("a").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/a")
			.columns(Column.of("name")).build();
		var b = ViewDef.create("b").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/b")
			.columns(Column.of("name")).build();
		a.details(RowDetailDef.create().endpoint("/data/a/{id}")
			.sections(DetailSection.create("sa", "A").fields(DetailField.of("aOwner")).table(NestedTableDef.create(b))));
		b.details(RowDetailDef.create().endpoint("/data/b/{id}")
			.sections(DetailSection.create("sb", "B").fields(DetailField.of("bOwner")).table(NestedTableDef.create(a))));
		var ntA = NestedTableDef.create(a);
		assertThrows(IllegalArgumentException.class, ntA::validate);
		var ntB = NestedTableDef.create(b);
		assertThrows(IllegalArgumentException.class, ntB::validate);
	}

	@Test void b08_siblingDag_reusingOneViewDefInstance_passes() {
		// Cycle detection is PATH-scoped (pushed on descent, popped on unwind), not a global visited set: the same
		// nested ViewDef instance reached from two different parents is a legal DAG, not a cycle.
		var shared = nestedView();
		var p1 = withDetails("alerts", NestedTableDef.create(shared));
		var p2 = withDetails("hosts", NestedTableDef.create(shared));
		p1.validate();
		p2.validate();
		p2.validate();
		p1.validate();
	}

	@Test void b09_selection_isCarriedAndCascadesIntoValidate() {
		var selection = SelectionDef.create("id");
		var nt = NestedTableDef.create(nestedView()).selection(selection);
		assertSame(selection, nt.selection);
		nt.validate();
		// The whole nested path is still validated with a selection present (it is not a short-circuit).
		var bad = NestedTableDef.create(nestedView("https://evil/data")).selection(selection);
		assertThrows(IllegalArgumentException.class, bad::validate);
	}
}
