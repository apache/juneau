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
		var d = RowDetailDef.create().endpoint("/data/{id}");
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("at least one section"), e::getMessage);
	}

	@Test void a03_duplicateFieldKeys_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(
				DetailSection.create("a", "A").fields(DetailField.of("owner").title("O")),
				DetailSection.create("b", "B").fields(DetailField.of("owner").title("O2")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("duplicate field"), e::getMessage);
	}

	@Test void a04_unknownActionRef_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().actions(ActionBar.create().items(ActionRef.of("ack"))));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("ack"), e::getMessage);
	}

	@Test void a05_knownActionRef_accepted() {
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().actions(ActionBar.create().items(ActionRef.of("ack"), SafeAction.COLLAPSE)))
			.validate(java.util.List.of(ack()));
	}

	@Test void a06_missingIdPlaceholder_rejected() {
		var d = RowDetailDef.create().endpoint("/data/x").sections(oneSection());
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("{id}"), e::getMessage);
	}

	@Test void a07_blankEndpoint_rejected() {
		var d = RowDetailDef.create().endpoint("  ").sections(oneSection());
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void a08_absoluteUrl_rejected() {
		var d = RowDetailDef.create().endpoint("https://evil/{id}").sections(oneSection());
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("same-origin"), e::getMessage);
	}

	@Test void a09_dotDotSegment_rejected() {
		var d = RowDetailDef.create().endpoint("/data/../x/{id}").sections(oneSection());
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void a10_schemeColon_rejected() {
		var d = RowDetailDef.create().endpoint("servlet:/data/{id}").sections(oneSection());
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void a11_protocolRelative_rejected() {
		var d = RowDetailDef.create().endpoint("//evil/{id}").sections(oneSection());
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void a12_columnsLessThanOne_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("info", "Info").columns(0).fields(DetailField.of("a")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("columns"), e::getMessage);
	}

	@Test void a13_duplicateSectionIds_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(
				DetailSection.create("info", "A").fields(DetailField.of("a")),
				DetailSection.create("info", "B").fields(DetailField.of("b")));
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
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
		var p = PageDef.create("p").tabs(Tab.create("t", "T").view(v));
		var e = assertThrows(IllegalArgumentException.class, p::build);
		assertTrue(e.getMessage().contains("{id}"), e::getMessage);
	}

	@Test void a18_actionRefCount() {
		var bar = ActionBar.create().items(ActionRef.of("ack"), ActionRef.of("esc"), SafeAction.COLLAPSE);
		assertSize(3, bar.items);
	}

	@Test void a19_countNegative_rejected_nullAndZeroAccepted() {
		var negative = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("info", "Info").count(-1).fields(DetailField.of("a")));
		var e = assertThrows(IllegalArgumentException.class, () -> negative.validate(null));
		assertTrue(e.getMessage().contains("count"), e::getMessage);

		// A null count is the common case (no suffix), and must not trip the guard.
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("info", "Info").count(null).fields(DetailField.of("a")))
			.validate(null);

		// Zero is meaningful and still renders.
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("info", "Info").count(0).fields(DetailField.of("a")))
			.validate(null);
	}

	@Test void b01_renderPlusMarkdown_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(
				DetailField.of("body").format(DetailField.Format.MARKDOWN).render("tag")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("non-TEXT"), e::getMessage);
	}

	@Test void b02_blankRenderId_rejected() {
		var f = DetailField.of("cpu");
		f.render = new Render();
		f.render.id = "  ";
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(f));
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void b03_unknownRenderId_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(DetailField.of("cpu").render("nope")));
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void b04_customWithoutOptIn_rejected_withOptInAccepted() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(DetailField.of("cpu").render("spark")));
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.allowCustomRenderers("spark")
			.sections(DetailSection.create("s", "S").fields(DetailField.of("cpu").render("spark")))
			.validate(null);
	}

	@Test void b05_blankCustomEntry_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.allowCustomRenderers("spark", "  ")
			.sections(oneSection());
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
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
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().table(NestedTableDef.create(nested("events")).parentScopeParam("draw")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null, "alerts"));
		assertTrue(e.getMessage().contains("reserved"), e::getMessage);
	}

	@Test void c03_nestedViewId_collidesWithEnclosing_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().table(NestedTableDef.create(nested("alerts"))));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null, "alerts"));
		assertTrue(e.getMessage().contains("collides"), e::getMessage);
	}

	@Test void c04_duplicateNestedViewIds_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(
				DetailSection.create("a", "A").fields(DetailField.of("owner")).table(NestedTableDef.create(nested("events"))),
				DetailSection.create("b", "B").fields(DetailField.of("host")).table(NestedTableDef.create(nested("events"))));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null, "alerts"));
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

	@Test void d01_titleTemplate_doesNotCollideWithSectionField() {
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.title("Owner {owner}")
			.sections(oneSection())
			.validate(null);
	}

	@Test void d02_headerActions_unknownActionRef_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.headerActions(ActionBar.create().items(ActionRef.of("ack")))
			.sections(oneSection());
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("ack"), e::getMessage);
	}

	@Test void d03_titleIconAndHeaderActions_accepted() {
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.title("Incident #{number}")
			.icon("search")
			.headerActions(ActionBar.create().items(ActionRef.of("ack"), SafeAction.COLLAPSE))
			.sections(oneSection())
			.validate(java.util.List.of(ack()));
	}

	@Test void e01_enabledWhen_onUndeclaredField_rejected() {
		// A rule keyed on a field the expand GET will never return would fail closed at runtime and disable the
		// action forever with no way for the author to see why, so it is a startup error instead.
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().actions(ActionBar.create().items(
				ActionRef.of("ack").enabledWhen("state", Op.EQ, "open", "This record is not open."))));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(java.util.List.of(ack())));
		assertTrue(e.getMessage().contains("state"), e::getMessage);
		assertTrue(e.getMessage().contains("ack"), e::getMessage);
	}

	@Test void e02_enabledWhen_onDeclaredField_accepted() {
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(oneSection().actions(ActionBar.create().items(
				ActionRef.of("ack").enabledWhen("owner", Op.PRESENT, "This record has no owner yet."))))
			.validate(java.util.List.of(ack()));
	}

	@Test void e03_enabledWhen_mayKeyOnAFieldDeclaredByALaterSection() {
		// The expand GET returns the whole field map at once, so a rule in the first section legally keys on a field
		// the last one declares - the cross-check must run against the finished catalog, not a half-built one.
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(
				DetailSection.create("a", "A")
					.fields(DetailField.of("owner"))
					.actions(ActionBar.create().items(
						ActionRef.of("ack").enabledWhen("state", Op.EQ, "open", "This record is not open."))),
				DetailSection.create("b", "B").fields(DetailField.of("state")))
			.validate(java.util.List.of(ack()));
	}

	@Test void e04_headerActions_enabledWhen_onUndeclaredField_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.headerActions(ActionBar.create().items(
				ActionRef.of("ack").enabledWhen("state", Op.EQ, "open", "This record is not open.")))
			.sections(oneSection());
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(java.util.List.of(ack())));
		assertTrue(e.getMessage().contains("state"), e::getMessage);
		assertTrue(e.getMessage().contains("header"), e::getMessage);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) DetailField.actions - the THIRD ActionBar host.  Its rules live here rather than on DetailField, which has
	//    no validate() of its own and would have no caller for one.
	//------------------------------------------------------------------------------------------------------------------

	private static RowDetailDef withFieldBar(ActionBar bar) {
		return RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("info", "Info")
				.fields(DetailField.of("owner").title("Owner").actions(bar)));
	}

	/**
	 * The id cross-check the other two hosts already get.  Without it a typo'd id renders a real-looking button:
	 * the emitter falls back to the raw id as the label when no {@code RowAction} matches, so the author sees a
	 * plausible control whose click resolves to nothing instead of a startup failure.
	 */
	@Test void f01_fieldActions_unknownActionRef_rejected() {
		var d = withFieldBar(ActionBar.create().items(ActionRef.of("ack")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("ack"), e::getMessage);
		assertTrue(e.getMessage().contains("rowActions"), e::getMessage);
	}

	@Test void f02_fieldActions_knownActionRef_accepted() {
		withFieldBar(ActionBar.create().items(ActionRef.of("ack"), SafeAction.COLLAPSE))
			.validate(java.util.List.of(ack()));
	}

	/**
	 * The bar itself is validated too, not just its ids: reaching {@code validateActionBar} is what routes a field
	 * bar through {@code ActionBar.validate()} on the same terms as a header or section bar.
	 */
	@Test void f03_fieldActions_barsOwnRulesAreEnforced() {
		var d = withFieldBar(ActionBar.create().items(
			ActionRef.of("ack").emphasis(ActionRef.Emphasis.PRIMARY),
			ActionRef.of("esc").emphasis(ActionRef.Emphasis.PRIMARY)));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(java.util.List.of(ack(),
			RowAction.create("esc").endpoint("/y").method(RowAction.Method.POST))));
		assertTrue(e.getMessage().contains("PRIMARY"), e::getMessage);
	}

	/**
	 * The field-bar checks must sit ABOVE {@code validateDetailField}'s {@code render == null} early return.  A
	 * field with a bar and no renderer is the ordinary case, so a rule written below that return would be dead
	 * code for exactly the shape it exists to reject - and this method is what fails if it moves back down.
	 */
	@Test void f04_fieldActions_areCheckedOnAFieldWithNoRenderer() {
		var noRender = DetailField.of("owner").title("Owner").actions(ActionBar.create().items(ActionRef.of("ack")));
		assertNull(noRender.render);
		var d = RowDetailDef.create().endpoint("/data/{id}")
			.sections(DetailSection.create("info", "Info").fields(noRender));
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	/** The third {@code enabledWhen} traversal: a field-hosted bar's rules are cross-checked like the other two. */
	@Test void f05_fieldActions_enabledWhen_onUndeclaredField_rejected() {
		var d = withFieldBar(ActionBar.create().items(
			ActionRef.of("ack").enabledWhen("state", Op.EQ, "open", "This record is not open.")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(java.util.List.of(ack())));
		assertTrue(e.getMessage().contains("state"), e::getMessage);
		assertTrue(e.getMessage().contains("field 'owner'"), e::getMessage);
	}

	@Test void f06_fieldActions_enabledWhen_mayKeyOnAFieldDeclaredByALaterSection() {
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(
				DetailSection.create("a", "A").fields(DetailField.of("owner").actions(ActionBar.create().items(
					ActionRef.of("ack").enabledWhen("state", Op.EQ, "open", "This record is not open.")))),
				DetailSection.create("b", "B").fields(DetailField.of("state")))
			.validate(java.util.List.of(ack()));
	}

	/**
	 * A title-suppressed markdown body is a full-bleed prose column and a value-slot bar wants that same block, so
	 * declaring both is an authoring error.  A markdown field that keeps its title is fine.
	 */
	@Test void f07_actionsOnATitleSuppressedMarkdownBody_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(
				DetailField.of("body").title("").format(DetailField.Format.MARKDOWN)
					.actions(ActionBar.create().items(ActionRef.of("ack")))));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(java.util.List.of(ack())));
		assertTrue(e.getMessage().contains("MARKDOWN"), e::getMessage);
		assertTrue(e.getMessage().contains("body"), e::getMessage);
	}

	@Test void f08_actionsOnATitledMarkdownBody_accepted() {
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(
				DetailField.of("body").title("Notes").format(DetailField.Format.MARKDOWN)
					.actions(ActionBar.create().items(ActionRef.of("ack")))))
			.validate(java.util.List.of(ack()));
	}

	/** Rejected on the DECLARATION, not on the bar happening to have items today. */
	@Test void f09_emptyBarOnASuppressedTitleMarkdownBody_isStillRejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("s", "S").fields(
				DetailField.of("body").title("").format(DetailField.Format.MARKDOWN).actions(ActionBar.create())));
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void f10_viewTableOf_rejectsAnUnknownFieldActionRef() {
		var v = ViewDef.create("x").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u")
			.columns(Column.of("name"))
			.details(RowDetailDef.create().endpoint("/d/{id}")
				.sections(DetailSection.create("s", "S").fields(
					DetailField.of("owner").actions(ActionBar.create().items(ActionRef.of("nope"))))))
			.build();
		assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v));
	}
}
