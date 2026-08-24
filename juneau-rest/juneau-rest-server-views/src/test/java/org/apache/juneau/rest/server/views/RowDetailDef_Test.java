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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * {@link RowDetailDef#validate(java.util.List)} matrix: empty sections, duplicate keys, unknown ActionRef,
 * missing {@code {id}}, blank / absolute / {@code ..} / scheme endpoints, {@code columns >= 1}.
 */
class RowDetailDef_Test extends TestBase {

	private static DetailSection oneSection() {
		return DetailSection.create("info", "Info").fields(DetailField.of("owner").title("Owner"));
	}

	private static RowAction ack() {
		return RowAction.create("ack").endpoint("/x").method(RowAction.Method.POST);
	}

	@Test void a01_valid_minimal() {
		RowDetailDef.create().endpoint("/data/{id}").sections(oneSection()).validate(null);
	}

	@Test void a02_emptySections_rejected() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> RowDetailDef.create().endpoint("/data/{id}").validate(null));
		assertTrue(e.getMessage().contains("at least one section"), e::getMessage);
	}

	@Test void a03_duplicateFieldKeys_rejected() {
		var e = assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(
				DetailSection.create("a", "A").fields(DetailField.of("owner").title("O")),
				DetailSection.create("b", "B").fields(DetailField.of("owner").title("O2")))
			.validate(null));
		assertTrue(e.getMessage().contains("duplicate field"), e::getMessage);
	}

	@Test void a04_unknownActionRef_rejected() {
		var e = assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().actions(ActionBar.create().items(ActionRef.of("ack"))))
			.validate(null));
		assertTrue(e.getMessage().contains("ack"), e::getMessage);
	}

	@Test void a05_knownActionRef_accepted() {
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().actions(ActionBar.create().items(ActionRef.of("ack"), SafeAction.COLLAPSE)))
			.validate(java.util.List.of(ack()));
	}

	@Test void a06_missingIdPlaceholder_rejected() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> RowDetailDef.create().endpoint("/data/x").sections(oneSection()).validate(null));
		assertTrue(e.getMessage().contains("{id}"), e::getMessage);
	}

	@Test void a07_blankEndpoint_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> RowDetailDef.create().endpoint("  ").sections(oneSection()).validate(null));
	}

	@Test void a08_absoluteUrl_rejected() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> RowDetailDef.create().endpoint("https://evil/{id}").sections(oneSection()).validate(null));
		assertTrue(e.getMessage().contains("same-origin"), e::getMessage);
	}

	@Test void a09_dotDotSegment_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> RowDetailDef.create().endpoint("/data/../x/{id}").sections(oneSection()).validate(null));
	}

	@Test void a10_schemeColon_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> RowDetailDef.create().endpoint("servlet:/data/{id}").sections(oneSection()).validate(null));
	}

	@Test void a11_protocolRelative_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> RowDetailDef.create().endpoint("//evil/{id}").sections(oneSection()).validate(null));
	}

	@Test void a12_columnsLessThanOne_rejected() {
		var e = assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("info", "Info").columns(0).fields(DetailField.of("a")))
			.validate(null));
		assertTrue(e.getMessage().contains("columns"), e::getMessage);
	}

	@Test void a13_duplicateSectionIds_rejected() {
		assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(
				DetailSection.create("info", "A").fields(DetailField.of("a")),
				DetailSection.create("info", "B").fields(DetailField.of("b")))
			.validate(null));
	}

	@Test void a14_contractVersion_isOne() {
		assertEquals("1", RowDetailDef.CONTRACT_VERSION);
		assertEquals("4", ViewDef.CONTRACT_VERSION);
		assertEquals("1", ActionBar.CONTRACT_VERSION);
		assertEquals("1", ActionResult.CONTRACT_VERSION);
		assertEquals("1", BulkMutateDef.CONTRACT_VERSION);
	}

	@Test void a15_isSafeDetailEndpoint_matrix() {
		assertTrue(RowDetailDef.isSafeDetailEndpoint("/data/{id}"));
		assertTrue(RowDetailDef.isSafeDetailEndpoint("data/{id}"));
		assertFalse(RowDetailDef.isSafeDetailEndpoint("http://x/{id}"));
		assertFalse(RowDetailDef.isSafeDetailEndpoint("//x/{id}"));
		assertFalse(RowDetailDef.isSafeDetailEndpoint("servlet:/x/{id}"));
		assertFalse(RowDetailDef.isSafeDetailEndpoint("/a/../b/{id}"));
	}

	@Test void a16_viewDefValidate_isInvokedFromViewTableOf() {
		var v = ViewDef.create("x").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u")
			.columns(Column.of("name"))
			.details(RowDetailDef.create().endpoint("https://evil/{id}").sections(oneSection()))
			.build();
		assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v));
	}

	@Test void a17_pageDefValidate_recursesIntoNestedView() {
		var v = ViewDef.create("x").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u")
			.columns(Column.of("name"))
			.details(RowDetailDef.create().endpoint("/no-placeholder").sections(oneSection()))
			.build();
		var e = assertThrows(IllegalArgumentException.class, () -> PageDef.create("p")
			.tabs(Tab.create("t", "T").view(v))
			.build());
		assertTrue(e.getMessage().contains("{id}"), e::getMessage);
	}

	@Test void a18_actionRefCount() {
		var bar = ActionBar.create().items(ActionRef.of("ack"), ActionRef.of("esc"), SafeAction.COLLAPSE);
		assertSize(3, bar.items);
	}

	@Test void b01_renderPlusMarkdown_rejected() {
		var e = assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(
				DetailField.of("body").format(DetailField.Format.MARKDOWN).render("tag")))
			.validate(null));
		assertTrue(e.getMessage().contains("non-TEXT"), e::getMessage);
	}

	@Test void b02_blankRenderId_rejected() {
		var f = DetailField.of("cpu");
		f.render = new Render();
		f.render.id = "  ";
		assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(f))
			.validate(null));
	}

	@Test void b03_unknownRenderId_rejected() {
		assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(DetailField.of("cpu").render("nope")))
			.validate(null));
	}

	@Test void b04_customWithoutOptIn_rejected_withOptInAccepted() {
		assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(DetailField.of("cpu").render("spark")))
			.validate(null));
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.allowCustomRenderers("spark")
			.sections(DetailSection.create("s", "S").fields(DetailField.of("cpu").render("spark")))
			.validate(null);
	}

	@Test void b05_blankCustomEntry_rejected() {
		assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.allowCustomRenderers("spark", "  ")
			.sections(oneSection())
			.validate(null));
	}

	@Test void b06_everyBuiltinId_accepted_withDefaultText() {
		for (var id : SinkRenderAllowlist.BUILTIN_IDS) {
			RowDetailDef.create()
				.endpoint("/data/{id}")
				.sections(DetailSection.create("s", "S").fields(DetailField.of("f-" + id).render(id)))
				.validate(null);
		}
	}

	@Test void b07_viewTableOf_rejectsBadDetailRender() {
		var v = ViewDef.create("x").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u")
			.columns(Column.of("name"))
			.details(RowDetailDef.create().endpoint("/d/{id}")
				.sections(DetailSection.create("s", "S").fields(DetailField.of("cpu").render("evil"))))
			.build();
		assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v));
	}

	private static ViewDef nested(String id) {
		return ViewDef.create(id).dataMode(ViewDef.DataMode.CLIENT).dataUrl("/data/" + id)
			.columns(Column.of("name")).build();
	}

	@Test void c01_nestedTable_valid() {
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().table(NestedTableDef.create(nested("events"))))
			.validate(null, "alerts");
	}

	@Test void c02_nestedTable_delegatesValidation() {
		var e = assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().table(NestedTableDef.create(nested("events")).parentScopeParam("draw")))
			.validate(null, "alerts"));
		assertTrue(e.getMessage().contains("reserved"), e::getMessage);
	}

	@Test void c03_nestedViewId_collidesWithEnclosing_rejected() {
		var e = assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().table(NestedTableDef.create(nested("alerts"))))
			.validate(null, "alerts"));
		assertTrue(e.getMessage().contains("collides"), e::getMessage);
	}

	@Test void c04_duplicateNestedViewIds_rejected() {
		var e = assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(
				DetailSection.create("a", "A").fields(DetailField.of("owner")).table(NestedTableDef.create(nested("events"))),
				DetailSection.create("b", "B").fields(DetailField.of("host")).table(NestedTableDef.create(nested("events"))))
			.validate(null, "alerts"));
		assertTrue(e.getMessage().contains("duplicate nested"), e::getMessage);
	}

	@Test void c05_viewTableOf_rejectsCollidingNestedViewId() {
		var v = ViewDef.create("alerts").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u")
			.columns(Column.of("name"))
			.details(RowDetailDef.create().endpoint("/d/{id}")
				.sections(DetailSection.create("s", "S").fields(DetailField.of("owner")).table(NestedTableDef.create(nested("alerts")))))
			.build();
		assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v));
	}
}
