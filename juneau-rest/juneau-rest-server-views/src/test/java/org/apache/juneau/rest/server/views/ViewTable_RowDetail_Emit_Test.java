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
import org.apache.juneau.rest.server.widgets.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * {@link ViewTable} emits one {@code <template data-juneau-row-detail>} whose section/field/action ids match the
 * bean, and never pours field values into the template.
 */
class ViewTable_RowDetail_Emit_Test extends TestBase {

	private static ViewDef view() {
		return ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.rowActions(
				RowAction.create("ack").label("Acknowledge").endpoint("/data/alerts/{id}/ack").method(RowAction.Method.POST),
				RowAction.create("esc").label("Escalate").endpoint("/data/alerts/{id}/esc").method(RowAction.Method.POST))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(
					DetailSection.create("overview", "Overview")
						.columns(2)
						.fields(DetailField.of("severity").title("Severity"), DetailField.of("title").title("Title"))
						.actions(ActionBar.create().items(ActionRef.of("ack"), SafeAction.COLLAPSE)),
					DetailSection.create("detail", "Detail")
						.fields(DetailField.of("summary").title("Summary"))
						.actions(ActionBar.create().items(ActionRef.of("esc")))))
			.build();
	}

	@Test void a01_emitsOneTemplate_withStampedUrlAndContract() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("data-juneau-row-detail"), html);
		assertTrue(html.contains("data-juneau-detail-contract=\"1\""), html);
		assertTrue(html.contains("data-juneau-detail-url=\"/data/alerts/{id}\""), html);
		assertEquals(html.indexOf("data-juneau-row-detail"), html.lastIndexOf("data-juneau-row-detail"));
	}

	@Test void a02_sectionFieldActionIdsMatchBean() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("data-juneau-detail-section=\"overview\""), html);
		assertTrue(html.contains("data-juneau-detail-section=\"detail\""), html);
		assertTrue(html.contains("data-juneau-field=\"severity\""), html);
		assertTrue(html.contains("data-juneau-field=\"title\""), html);
		assertTrue(html.contains("data-juneau-field=\"summary\""), html);
		assertTrue(html.contains("data-juneau-action=\"ack\""), html);
		assertTrue(html.contains("data-juneau-action=\"esc\""), html);
		assertTrue(html.contains("data-juneau-safe=\"collapse\""), html);
		assertTrue(html.contains("Acknowledge"), html);
		assertTrue(html.contains("Escalate"), html);
		assertTrue(html.contains("Collapse"), html);
	}

	@Test void a03_actionRefStartsDisabled_collapseDoesNot() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("data-juneau-action=\"ack\""), html);
		assertTrue(html.contains("disabled=\"disabled\"") || html.contains(" disabled"), html);
		// COLLAPSE button must not carry disabled.
		var collapseAt = html.indexOf("data-juneau-safe=\"collapse\"");
		assertTrue(collapseAt >= 0, html);
		var collapseTagEnd = html.indexOf('>', collapseAt);
		var collapseTagStart = html.lastIndexOf('<', collapseAt);
		var collapseTag = html.substring(collapseTagStart, collapseTagEnd);
		assertFalse(collapseTag.contains("disabled"), collapseTag);
	}

	@Test void a04_fieldSlotsAreEmpty_noValuesInTemplate() {
		var html = Html.of(ViewTable.of(view()));
		assertFalse(html.contains("innerHTML"), html);
		var fieldAt = html.indexOf("data-juneau-field=\"severity\"");
		var fieldTagEnd = html.indexOf('>', fieldAt);
		var close = html.indexOf('<', fieldTagEnd + 1);
		var inner = html.substring(fieldTagEnd + 1, close);
		assertTrue(inner.isBlank(), inner);
	}

	@Test void a05_noTemplateWhenDetailsUnset() {
		var v = ViewDef.create("plain").dataMode(DataMode.CLIENT).dataUrl("/u").columns(Column.of("a")).build();
		var html = Html.of(ViewTable.of(v));
		assertFalse(html.contains("data-juneau-row-detail"), html);
		assertFalse(html.contains(ViewTable.DETAIL_TH_CLASS), html);
		assertFalse(html.contains(ViewTable.DETAIL_CONTROL_CLASS), html);
	}

	@Test void a05b_emitsDedicatedExpanderColumn_whenDetailsSet() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("class=\"" + ViewTable.DETAIL_TH_CLASS + "\""), html);
		assertTrue(html.contains("aria-label=\"Expand\""), html);
		// Expander th is the FIRST header cell, before the first data column title.
		var expanderAt = html.indexOf(ViewTable.DETAIL_TH_CLASS);
		var idTitleAt = html.indexOf(">Id<");
		assertTrue(expanderAt >= 0 && idTitleAt > expanderAt, html);
	}

	/**
	 * The validated column count reaches the grid as a CLASS, not as an inline
	 * {@code grid-template-columns}.  The inline style this used to assert is what made the count fixed: it
	 * out-ranks every rule in {@code juneau-views.css}, so a container query could never step the grid down and
	 * a wide section stayed wide inside a narrow panel.  The class names the author's cap; the stylesheet's
	 * {@code @container} ladder decides how many of those columns a given panel width can afford.
	 */
	@Test void a06_gridColumnCap_fromValidatedColumns_isAClassNotAnInlineStyle() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("juneau-view-detail-fields-cols-2"), html);
		assertFalse(html.contains("grid-template-columns"), html);
		// INLINE is the default arrangement and is stamped explicitly rather than left as an absent class, so the
		// two arrangements are symmetric in the stylesheet.
		assertTrue(html.contains("juneau-view-detail-fields-inline"), html);
		// The cap is clamped to the widest step the ladder declares, so an over-large author value cannot emit a
		// class with no rule behind it.
		var wide = Html.of(ViewTable.of(ViewDef.create("wide")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/{id}")
				.sections(DetailSection.create("s", "S")
					.columns(9)
					.layout(FieldLayout.STACKED)
					.fields(DetailField.of("a").title("A"))))
			.build()));
		assertTrue(wide.contains("juneau-view-detail-fields-cols-4"), wide);
		assertTrue(wide.contains("juneau-view-detail-fields-stacked"), wide);
	}

	@Test void a07_pageTable_alsoEmitsChildTemplate() {
		var html = Html.of(PageTable.of(PageDef.create("p").tabs(Tab.create("t", "T").view(view())).build()));
		assertTrue(html.contains("data-juneau-row-detail"), html);
		assertTrue(html.contains("data-juneau-detail-section=\"overview\""), html);
	}

	@Test void a08_markdownFormat_stampsAttribute_textDoesNot() {
		var v = ViewDef.create("skills")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/{id}")
				.sections(DetailSection.create("body", "SKILL.md")
					.columns(1)
					.fields(
						DetailField.of("name").title("Name"),
						DetailField.of("body").title("").format(DetailField.Format.MARKDOWN))))
			.build();
		var html = Html.of(ViewTable.of(v));
		assertTrue(html.contains("data-juneau-field=\"name\""), html);
		assertFalse(html.contains("data-juneau-field-format=\"text\""), html);
		assertTrue(html.contains("data-juneau-field-format=\"markdown\""), html);
		assertTrue(html.contains("data-juneau-field=\"body\""), html);
		assertTrue(html.contains("juneau-view-detail-markdown"), html);
		assertTrue(html.contains("jc-prose"), html);
		assertFalse(html.contains(">body</div>"), "empty markdown title must not fall back to the data key: " + html);
	}

	/**
	 * SANITIZED_HTML stamps its own wire token and otherwise emits the same full-bleed prose shape as MARKDOWN:
	 * both are rich-text bodies, and they differ in which client-side allowlist the value is copied through, not
	 * in layout.  The token is asserted exactly, since the client dispatches on it and an unrecognized token
	 * falls back to textContent - a typo here would silently disable the format rather than fail.
	 */
	@Test void a08b_sanitizedHtmlFormat_stampsOwnToken_andProseShape() {
		var v = ViewDef.create("gacks")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/{id}")
				.sections(DetailSection.create("description", "Description")
					.columns(1)
					.fields(
						DetailField.of("name").title("Name"),
						DetailField.of("descriptionHtml").title("").format(DetailField.Format.SANITIZED_HTML))))
			.build();
		var html = Html.of(ViewTable.of(v));
		assertTrue(html.contains("data-juneau-field=\"descriptionHtml\""), html);
		assertTrue(html.contains("data-juneau-field-format=\"sanitizedHtml\""), html);
		assertFalse(html.contains("data-juneau-field-format=\"markdown\""), html);
		assertFalse(html.contains("data-juneau-field-format=\"text\""), html);
		// Same full-bleed prose treatment as markdown: span-full, prose classes, suppressible title.
		assertTrue(html.contains("juneau-view-detail-field-span-full"), html);
		assertTrue(html.contains("juneau-view-detail-markdown"), html);
		assertTrue(html.contains("jc-prose"), html);
		assertFalse(html.contains(">descriptionHtml</div>"),
			"empty SANITIZED_HTML title must not fall back to the data key: " + html);
		// The server emits the value slot only - it never inlines the field's HTML into the page.
		assertFalse(html.contains("innerHTML"), html);
	}

	/** Adding SANITIZED_HTML must not perturb what a plain TEXT field emits. */
	@Test void a08c_textFormat_unchangedByNewFormat() {
		var v = ViewDef.create("skills")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/{id}")
				.sections(DetailSection.create("s", "S")
					.fields(DetailField.of("name").title("Name").format(DetailField.Format.TEXT))))
			.build();
		var html = Html.of(ViewTable.of(v));
		assertFalse(html.contains("data-juneau-field-format"), html);
		assertFalse(html.contains("juneau-view-detail-field-span-full"), html);
		assertFalse(html.contains("jc-prose"), html);
	}

	@Test void a09_renderStamp_tagMeta_noFormat() {
		var v = ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data")
			.columns(Column.of("id"))
			.details(RowDetailDef.create()
				.endpoint("/data/{id}")
				.sections(DetailSection.create("s", "S")
					.fields(DetailField.of("status").title("Status").render("tag:status"))))
			.build();
		var html = Html.of(ViewTable.of(v));
		assertTrue(html.contains("data-juneau-field-render=\"tag\""), html);
		assertTrue(html.contains("data-juneau-field-render-meta="), html);
		assertTrue(html.contains("field") && html.contains("status"), html);
		assertFalse(html.contains("data-juneau-field-format"), html);
	}

	@Test void a10_renderStamp_linkedHref_andNullRenderOmits() {
		var v = ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data")
			.columns(Column.of("id"))
			.details(RowDetailDef.create()
				.endpoint("/data/{id}")
				.sections(DetailSection.create("s", "S")
					.fields(
						DetailField.of("name").title("Name").render("linked").href("/x/{id}"),
						DetailField.of("plain").title("Plain"))))
			.build();
		var html = Html.of(ViewTable.of(v));
		assertTrue(html.contains("data-juneau-field-render=\"linked\""), html);
		assertTrue(html.contains("data-juneau-field-render-href=\"/x/{id}\""), html);
		var plainAt = html.indexOf("data-juneau-field=\"plain\"");
		var plainTagEnd = html.indexOf('>', plainAt);
		var plainTagStart = html.lastIndexOf('<', plainAt);
		var plainTag = html.substring(plainTagStart, plainTagEnd);
		assertFalse(plainTag.contains("data-juneau-field-render"), plainTag);
	}

	@Test void a11_markdownStillStampsFormat_neverRender() {
		var v = ViewDef.create("skills")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data")
			.columns(Column.of("id"))
			.details(RowDetailDef.create()
				.endpoint("/data/{id}")
				.sections(DetailSection.create("body", "SKILL.md")
					.fields(DetailField.of("body").title("").format(DetailField.Format.MARKDOWN))))
			.build();
		var html = Html.of(ViewTable.of(v));
		assertTrue(html.contains("data-juneau-field-format=\"markdown\""), html);
		assertFalse(html.contains("data-juneau-field-render"), html);
	}

	@Test void a12_headerEmitsTitleTemplateAndIcon_beforeSections() {
		var v = ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data")
			.columns(Column.of("id").title("Id"))
			.rowActions(RowAction.create("ack").label("Acknowledge").endpoint("/data/{id}/ack").method(RowAction.Method.POST))
			.details(RowDetailDef.create()
				.endpoint("/data/{id}")
				.title("Incident #{number}")
				.icon("search")
				.headerActions(ActionBar.create().items(ActionRef.of("ack"), SafeAction.COLLAPSE))
				.sections(
					DetailSection.create("details", "Details").fields(DetailField.of("summary").title("Summary")),
					DetailSection.create("diagnose", "Diagnose").fields(DetailField.of("findings").title("Findings"))))
			.build();
		var html = Html.of(ViewTable.of(v));
		assertTrue(html.contains("juneau-view-detail-header"), html);
		assertTrue(html.contains("juneau-view-detail-title"), html);
		assertTrue(html.contains("data-juneau-detail-title"), html);
		assertTrue(html.contains("data-juneau-detail-title-template"), html);
		assertTrue(html.contains("Incident #{number}"), html);
		assertTrue(html.contains("data-juneau-detail-icon=\"search\"") || html.contains("data-juneau-detail-icon='search'"), html);
		assertTrue(html.contains("data-juneau-action=\"ack\""), html);
		assertFalse(html.contains("data-juneau-field=\"headerTitle\""), html);
		var headerAt = html.indexOf("juneau-view-detail-header");
		var sectionAt = html.indexOf("data-juneau-detail-section=\"details\"");
		assertTrue(headerAt >= 0 && sectionAt > headerAt, html);
	}

	@Test void a13_primaryEmphasisStampsClass_secondaryDefaultDoesNot() {
		var v = ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.rowActions(
				RowAction.create("ack").label("Acknowledge").endpoint("/data/alerts/{id}/ack").method(RowAction.Method.POST),
				RowAction.create("esc").label("Escalate").endpoint("/data/alerts/{id}/esc").method(RowAction.Method.POST))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(DetailSection.create("s", "S")
					.fields(DetailField.of("title").title("Title"))
					.actions(ActionBar.create().items(
						ActionRef.of("ack").emphasis(ActionRef.Emphasis.PRIMARY),
						ActionRef.of("esc"),
						SafeAction.COLLAPSE))))
			.build();
		var html = Html.of(ViewTable.of(v));
		var ackAt = html.indexOf("data-juneau-action=\"ack\"");
		var ackTagEnd = html.indexOf('>', ackAt);
		var ackTagStart = html.lastIndexOf('<', ackAt);
		var ackTag = html.substring(ackTagStart, ackTagEnd);
		assertTrue(ackTag.contains("juneau-view-detail-action-primary"), ackTag);
		var escAt = html.indexOf("data-juneau-action=\"esc\"");
		var escTagEnd = html.indexOf('>', escAt);
		var escTagStart = html.lastIndexOf('<', escAt);
		var escTag = html.substring(escTagStart, escTagEnd);
		assertFalse(escTag.contains("juneau-view-detail-action-primary"), escTag);
		// No data attribute for emphasis (D7): a class only.
		assertFalse(html.contains("data-juneau-emphasis"), html);
	}

	@Test void a14_moreThanOnePrimaryInABar_rejectedAtValidate() {
		var bar = ActionBar.create().items(
			ActionRef.of("ack").emphasis(ActionRef.Emphasis.PRIMARY),
			ActionRef.of("esc").emphasis(ActionRef.Emphasis.PRIMARY));
		assertThrows(IllegalArgumentException.class, bar::validate);
	}

	private static ViewDef gatedView() {
		return ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.rowActions(
				RowAction.create("ack").label("Acknowledge").endpoint("/data/alerts/{id}/ack").method(RowAction.Method.POST),
				RowAction.create("esc").label("Escalate").endpoint("/data/alerts/{id}/esc").method(RowAction.Method.POST))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(DetailSection.create("s", "S")
					.fields(DetailField.of("state").title("State"), DetailField.of("owner").title("Owner"))
					.actions(ActionBar.create().items(
						ActionRef.of("ack")
							.enabledWhen("state", Op.EQ, "open", "This alert is not open.")
							.enabledWhen("owner", Op.PRESENT, "This alert has no owner yet."),
						ActionRef.of("esc"),
						SafeAction.COLLAPSE))))
			.build();
	}

	private static String openTagAt(String html, String needle) {
		var at = html.indexOf(needle);
		assertTrue(at >= 0, () -> "not found: " + needle + " in " + html);
		return html.substring(html.lastIndexOf('<', at), html.indexOf('>', at));
	}

	@Test void a15_gatedActionRef_stampsItsRulesAsJsonOnTheButton() {
		var html = Html.of(ViewTable.of(gatedView()));
		var ackTag = openTagAt(html, "data-juneau-action=\"ack\"");
		assertTrue(ackTag.contains(ViewTable.DETAIL_ACTION_RULES_ATTR), ackTag);
		// Both rules travel, in declaration order, with the operator as its wire token and the reason included -
		// the reason is what the runtime shows, so it cannot be left behind on the server.
		assertTrue(ackTag.contains("state"), ackTag);
		assertTrue(ackTag.contains("open"), ackTag);
		assertTrue(ackTag.contains("This alert is not open."), ackTag);
		assertTrue(ackTag.contains("owner"), ackTag);
		assertTrue(ackTag.contains("present"), ackTag);
		assertTrue(ackTag.contains("This alert has no owner yet."), ackTag);
		assertTrue(ackTag.indexOf("This alert is not open.") < ackTag.indexOf("This alert has no owner yet."), ackTag);
		// An ungated sibling in the same bar carries no rules attribute, and neither does the SafeAction (D7).
		assertFalse(openTagAt(html, "data-juneau-action=\"esc\"").contains(ViewTable.DETAIL_ACTION_RULES_ATTR), html);
		assertFalse(openTagAt(html, "data-juneau-safe=\"collapse\"").contains(ViewTable.DETAIL_ACTION_RULES_ATTR), html);
	}

	@Test void a16_gatedActionRef_getsOneHiddenReasonNode_withNoIdAndNoText() {
		var html = Html.of(ViewTable.of(gatedView()));
		var descNeedle = ViewTable.DETAIL_ACTION_DESC_ATTR + "=\"ack\"";
		var descTag = openTagAt(html, descNeedle);
		// Hidden by the native attribute rather than a CSS utility class, so the template needs no stylesheet
		// support to keep the reason out of the visual bar.
		assertTrue(descTag.contains("hidden"), descTag);
		// No id here: the runtime mints a row-unique one per clone, because a template id would collide across
		// every expanded row.
		assertFalse(descTag.contains(" id="), descTag);
		// Exactly one per gated action, and none for the ungated ones.
		assertEquals(html.indexOf(descNeedle), html.lastIndexOf(descNeedle), html);
		assertFalse(html.contains(ViewTable.DETAIL_ACTION_DESC_ATTR + "=\"esc\""), html);
		// The template never carries the reason text - it is stamped once, in the rules JSON on the button.
		var descTagEnd = html.indexOf('>', html.indexOf(descNeedle));
		assertEquals("", html.substring(descTagEnd + 1, html.indexOf('<', descTagEnd + 1)), html);
	}

	@Test void a17_ungatedBar_emitsNeitherAttribute() {
		// A regression guard on the existing shape: authors who declare no rules must see byte-identical output.
		var html = Html.of(ViewTable.of(view()));
		assertFalse(html.contains(ViewTable.DETAIL_ACTION_RULES_ATTR), html);
		assertFalse(html.contains(ViewTable.DETAIL_ACTION_DESC_ATTR), html);
	}
}
