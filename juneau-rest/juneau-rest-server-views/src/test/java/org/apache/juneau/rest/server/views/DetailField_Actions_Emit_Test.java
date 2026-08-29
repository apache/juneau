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
 * {@link DetailField#actions} &mdash; the third {@link ActionBar} host &mdash; as {@link ViewTable} emits it.
 *
 * <p>
 * The shape under test is a plain <b>sibling</b>: the bar is a third child of the field block, beside the
 * {@code [data-juneau-field]} value slot rather than inside it and rather than wrapped with it.  That is what lets
 * the expand-fill painter and the panel-scoped action lifecycles keep working untouched, so the assertions here
 * pin the sibling relationship itself and not merely the bar's presence.
 *
 * <p>
 * What a JVM test can prove is which nodes are emitted, in which order, with which attributes.  Where the
 * stylesheet then puts the third child is asserted against the served stylesheet in
 * {@code ViewsMixin_Serving_Test.o07}, and measured for real in the browser suite.
 */
class DetailField_Actions_Emit_Test extends TestBase {

	private static final String FIELD = "assignee";

	private static ViewDef view(FieldLayout layout, DetailField...fields) {
		return ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.rowActions(
				RowAction.create("ack").label("Acknowledge").endpoint("/data/alerts/{id}/ack")
					.method(RowAction.Method.POST),
				RowAction.create("esc").label("Escalate").endpoint("/data/alerts/{id}/esc")
					.method(RowAction.Method.POST))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(DetailSection.create("ctx", "Context").layout(layout).fields(fields)))
			.build();
	}

	private static ViewDef view(DetailField...fields) {
		return view(null, fields);
	}

	private static String html(FieldLayout layout, DetailField...fields) {
		return Html.of(ViewTable.of(view(layout, fields)));
	}

	private static String html(DetailField...fields) {
		return html(null, fields);
	}

	/** The one section's fields grid, which for these single-field fixtures is exactly one field block. */
	private static String grid(String html) {
		var start = html.indexOf("juneau-view-detail-fields-cols-");
		assertTrue(start >= 0, html);
		var end = html.indexOf("</section>", start);
		assertTrue(end > start, html);
		return html.substring(start, end);
	}

	private static int count(String haystack, String needle) {
		var n = 0;
		for (var i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1))
			n++;
		return n;
	}

	private static DetailField bare() {
		return DetailField.of(FIELD).title("Assignee");
	}

	private static DetailField withBar(ActionBarItem...items) {
		return DetailField.of(FIELD).title("Assignee").actions(ActionBar.create().items(items));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The additive half: a field that declares no bar is unchanged
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_noActions_emitsByteIdenticalMarkup() {
		var out = html(bare());
		assertFalse(out.contains("juneau-view-detail-actions"), out);
		// Two children, exactly as before: the field block plus its title and value divs, and nothing else.
		assertEquals(3, count(grid(out), "<div"), grid(out));
	}

	@Test void a02_nullBarAndEmptyBarBothEmitNothing() {
		assertFalse(html(DetailField.of(FIELD).title("Assignee").actions(null)).contains("juneau-view-detail-actions"));
		assertFalse(html(withBar()).contains("juneau-view-detail-actions"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) The sibling shape
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_barIsTheThirdChild_afterTitleThenValue() {
		var g = grid(html(withBar(ActionRef.of("esc"))));
		var titleAt = g.indexOf("juneau-view-detail-field-title");
		var valueAt = g.indexOf("data-juneau-field=\"" + FIELD + "\"");
		var barAt = g.indexOf("juneau-view-detail-actions");
		assertTrue(titleAt >= 0 && valueAt > titleAt && barAt > valueAt,
			() -> "expected title, then value slot, then bar: " + g);
	}

	/**
	 * The bar is a sibling, not a child of the value slot.  Asserted as the value slot still being <b>empty</b>,
	 * which is the same property the expand-fill painter relies on: it assigns {@code textContent}, so anything
	 * emitted inside that div would be destroyed on the first paint.
	 */
	@Test void b02_valueSlotStaysEmpty_soTheFillPathAndTheBarDoNotShareANode() {
		var out = html(withBar(ActionRef.of("esc")));
		var fieldAt = out.indexOf("data-juneau-field=\"" + FIELD + "\"");
		var tagEnd = out.indexOf('>', fieldAt);
		var inner = out.substring(tagEnd + 1, out.indexOf('<', tagEnd + 1));
		assertTrue(inner.isBlank(), () -> "the bar must not be emitted inside the value slot: " + inner);
	}

	/**
	 * No wrapper level was introduced around the value slot and the bar.  Counted rather than described, because a
	 * wrapper is the one alternative shape that would satisfy every other assertion in this class.
	 */
	@Test void b03_noWrapperDivAroundTheValueSlotAndTheBar() {
		var g = grid(html(withBar(ActionRef.of("esc"))));
		// field block + title + value + bar = 4, and a wrapper would make it 5.
		assertEquals(4, count(g, "<div"), g);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) It reaches the SHARED emitter, so it inherits every host-independent behaviour
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_sameActionMarkupAsTheOtherTwoHosts() {
		var g = grid(html(withBar(ActionRef.of("esc"), SafeAction.COLLAPSE)));
		assertTrue(g.contains("data-juneau-action=\"esc\""), g);
		assertTrue(g.contains("data-juneau-safe=\"collapse\""), g);
		// Label resolved from the enclosing view's rowActions, not the raw id.
		assertTrue(g.contains("Escalate"), g);
		assertTrue(g.contains("juneau-view-detail-action"), g);
	}

	@Test void c02_actionRefStartsDisabled_untilTheExpandGetSucceeds() {
		var g = grid(html(withBar(ActionRef.of("esc"))));
		var btnAt = g.indexOf("data-juneau-action=\"esc\"");
		var tag = g.substring(g.lastIndexOf('<', btnAt), g.indexOf('>', btnAt));
		assertTrue(tag.contains("disabled"), tag);
	}

	@Test void c03_enabledWhenRulesRideAlong_andSoDoesTheirReasonNode() {
		var g = grid(html(DetailField.of(FIELD).title("Assignee")
			.actions(ActionBar.create().items(
				ActionRef.of("esc").enabledWhen(FIELD, Op.PRESENT, "No assignee yet.")))));
		assertTrue(g.contains("data-juneau-action-rules="), g);
		assertTrue(g.contains("data-juneau-action-desc=\"esc\""), g);
	}

	/** Emphasis reaches the field bar through the shared class the base recipe owns; it is not forked per host. */
	@Test void c04_primaryEmphasisIsTheSharedClass() {
		var g = grid(html(withBar(ActionRef.of("esc").emphasis(ActionRef.Emphasis.PRIMARY))));
		assertTrue(g.contains("juneau-view-detail-action juneau-view-detail-action-primary"), g);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) LD-4: a value AND a bar at once, which is the case a blank/non-blank default could not express
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * The three-state case.  A field carrying a bar still carries a real, fillable value slot bound to its own
	 * {@code data} key, so the runtime paints the id and the bar is beside it rather than instead of it.  Pinned
	 * again behaviourally, after a non-blank paint, by {@code ViewsJs_RowDetail_Test.d02}.
	 */
	@Test void d01_valueSlotAndBarCoexistOnOneField() {
		var g = grid(html(withBar(ActionRef.of("esc"))));
		assertTrue(g.contains("data-juneau-field=\"" + FIELD + "\""), g);
		assertTrue(g.contains("juneau-view-detail-field-value"), g);
		assertTrue(g.contains("juneau-view-detail-actions"), g);
		assertTrue(g.contains("data-juneau-action=\"esc\""), g);
	}

	/**
	 * The headline "no value, offer the actions" case does <b>not</b> suppress the empty-value affordance: the
	 * value slot is still emitted, still empty, so the stylesheet's {@code :empty::after} em-dash still applies
	 * beside the bar.  There genuinely is no value, and hiding that signal is not this host's call.
	 */
	@Test void d02_anActionsOnlyFieldStillEmitsAnEmptyValueSlot() {
		var g = grid(html(withBar(ActionRef.of("esc"))));
		assertTrue(g.contains("class=\"juneau-view-detail-field-value\""), g);
		assertEquals(1, count(g, "juneau-view-detail-field-value"), g);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Composition with the field block's other variants
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_aRenderedFieldMayAlsoCarryABar() {
		var g = grid(html(DetailField.of(FIELD).title("Assignee").render("tag").href("/u/{id}")
			.actions(ActionBar.create().items(ActionRef.of("esc")))));
		assertTrue(g.contains("data-juneau-field-render=\"tag\""), g);
		assertTrue(g.contains("juneau-view-detail-actions"), g);
	}

	@Test void e02_aFullSpanFieldKeepsItsSpanClass_andStillGetsTheBar() {
		var g = grid(html(DetailField.of(FIELD).title("Assignee").span(FieldSpan.FULL)
			.actions(ActionBar.create().items(ActionRef.of("esc")))));
		assertTrue(g.contains("juneau-view-detail-field-span-full"), g);
		assertTrue(g.contains("juneau-view-detail-actions"), g);
	}

	/** A titled markdown field is legal with a bar; only the title-SUPPRESSED markdown body is rejected. */
	@Test void e03_titledMarkdownFieldKeepsItsTitleDivAndItsBar() {
		var g = grid(html(DetailField.of(FIELD).title("Assignee").format(DetailField.Format.MARKDOWN)
			.actions(ActionBar.create().items(ActionRef.of("esc")))));
		assertTrue(g.contains("juneau-view-detail-field-title"), g);
		assertTrue(g.contains("juneau-view-detail-field-markdown"), g);
		assertTrue(g.contains("juneau-view-detail-actions"), g);
	}

	/**
	 * A field-hosted bar under {@link FieldLayout#STACKED} is fenced from widening the row entirely,
	 * for the same one-column-ancestor root cause as the titled-markdown case above. The bar is still a plain
	 * sibling here, exactly as under {@code INLINE} - the CSS fix ({@code ViewsMixin_Serving_Test.o08}) is what
	 * stops it widening the row, not a change to what gets emitted.
	 */
	@Test void e04_stackedLayoutFieldKeepsItsBar() {
		var out = html(FieldLayout.STACKED, withBar(ActionRef.of("esc")));
		// The "stacked" class sits earlier in the same attribute than grid()'s "-cols-" capture window starts at.
		assertTrue(out.contains("juneau-view-detail-fields-stacked"), out);
		assertTrue(grid(out).contains("juneau-view-detail-actions"), out);
	}
}
