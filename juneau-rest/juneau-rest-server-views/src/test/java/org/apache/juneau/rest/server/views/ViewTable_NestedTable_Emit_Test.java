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
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * {@link ViewTable} emits a nested table shell inside a detail section: a {@code data-juneau-view} table with no HTML
 * {@code id}, an {@code id}-less sibling VIEW_META sidecar found via {@code data-juneau-nested-meta}, and the wrapper
 * carrying the independent nested contract version + parent-scope parameter.
 *
 * <p>
 * The {@code b*} cases cover the depth-2 widening: the enclosing response's CSRF token painted onto the nested table,
 * nested row actions and nested selection, the nested view's own row-detail template, the token-less fail-closed path,
 * and the parent-only affordances (no nested chooser host, no nested bulk sidecar).
 */
class ViewTable_NestedTable_Emit_Test extends TestBase {

	private static final String TOKEN = "tok-123";

	private static ViewDef nested() {
		return ViewDef.create("events")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/events")
			.columns(Column.of("when").title("When"), Column.of("what").title("What"))
			.build();
	}

	private static ViewDef view() {
		return view(NestedTableDef.create(nested()));
	}

	private static ViewDef view(NestedTableDef nt) {
		return ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(DetailSection.create("related", "Related events")
					.fields(DetailField.of("owner").title("Owner"))
					.table(nt)))
			.build();
	}

	@Test void a01_emitsNestedWrapper_withContractAndScopeParam() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("juneau-view-detail-nested"), html);
		assertTrue(html.contains("data-juneau-nested=\"1\""), html);
		assertTrue(html.contains("data-juneau-nested-contract=\"2\""), html);
		assertTrue(html.contains("data-juneau-nested-scope-param=\"parentId\""), html);
	}

	@Test void a02_nestedTableCarriesAuthorId_noHtmlId() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("data-juneau-view=\"events\""), html);
		// The nested <table> must not mint an HTML id (a <template> clone would collide on it).
		var at = html.indexOf("data-juneau-view=\"events\"");
		var tagStart = html.lastIndexOf('<', at);
		var tagEnd = html.indexOf('>', at);
		var tag = html.substring(tagStart, tagEnd);
		assertFalse(tag.contains("id="), tag);
	}

	@Test void a03_nestedSidecar_isSiblingWithMetaAttr_noHtmlId() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("data-juneau-nested-meta=\"events\""), html);
		// The nested sidecar must not carry the top-level "juneau-view:<id>" html id.
		assertFalse(html.contains("id=\"juneau-view:events\""), html);
		// It still carries the serialized nested VIEW_META (its own contractVersion + column data keys).
		assertTrue(html.contains("\"when\"") && html.contains("\"what\""), html);
	}

	@Test void a04_nestedTable_appendedAfterFields() {
		var html = Html.of(ViewTable.of(view()));
		var fieldsAt = html.indexOf("juneau-view-detail-fields");
		var nestedAt = html.indexOf("juneau-view-detail-nested");
		assertTrue(fieldsAt >= 0 && nestedAt >= 0, html);
		assertTrue(fieldsAt < nestedAt, "nested table must be appended after the fields grid: " + html);
	}

	@Test void a05_noNestedMarkup_whenSectionHasNoTable() {
		var v = ViewDef.create("alerts").dataMode(DataMode.CLIENT).dataUrl("/data/alerts")
			.columns(Column.of("id"))
			.details(RowDetailDef.create().endpoint("/data/alerts/{id}")
				.sections(DetailSection.create("s", "S").fields(DetailField.of("owner"))))
			.build();
		var html = Html.of(ViewTable.of(v));
		assertFalse(html.contains("data-juneau-nested"), html);
	}

	@Test void a06_customScopeParam_isStamped() {
		var html = Html.of(ViewTable.of(view(NestedTableDef.create(nested()).parentScopeParam("alertId"))));
		assertTrue(html.contains("data-juneau-nested-scope-param=\"alertId\""), html);
	}

	/** A request carrying the boundary-stamped CSRF token (the auto-embed entry point of the token contract). */
	private static MockServletRequest tokenRequest() {
		return MockServletRequest.create().attribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, TOKEN);
	}

	/** The nested {@code <table>}'s opening tag (the parent table carries the same attributes with other values). */
	private static String nestedTableTag(String html) {
		var at = html.indexOf("data-juneau-view=\"events\"");
		assertTrue(at >= 0, html);
		return html.substring(html.lastIndexOf('<', at), html.indexOf('>', at));
	}

	private static ViewDef nestedWithRowActions() {
		return ViewDef.create("events")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/events")
			.columns(Column.of("when").title("When"))
			.rowActions(RowAction.create("ack").label("Ack").endpoint("/data/events/{id}/ack")
				.method(RowAction.Method.POST))
			.build();
	}

	@Test void b01_requestPath_stampsTokenOnNestedTable() {
		var html = Html.of(ViewTable.of(tokenRequest(), view()));
		assertTrue(nestedTableTag(html).contains(ViewTable.CSRF_ATTR + "=\"" + TOKEN + "\""), html);
	}

	@Test void b02_requestPath_nestedRowActionsSurviveIntoTheSidecar() {
		var html = Html.of(ViewTable.of(tokenRequest(), view(NestedTableDef.create(nestedWithRowActions()))));
		assertTrue(html.contains("\"ack\""), "the nested sidecar must carry the declared row action: " + html);
		assertTrue(nestedTableTag(html).contains(ViewTable.CSRF_ATTR), html);
	}

	@Test void b03_nonRequestPath_failsClosed_noTokenAndNoNestedRowActions() {
		var nested = nestedWithRowActions();
		var html = Html.of(ViewTable.of(view(NestedTableDef.create(nested))));
		assertFalse(nestedTableTag(html).contains(ViewTable.CSRF_ATTR), nestedTableTag(html));
		assertFalse(html.contains("\"ack\""),
			"a token-less nested view must not ship a mutating action the runtime could never submit: " + html);
		// The withhold is a guarded serialize window, not a lasting edit of the author's shared definition.
		assertNotNull(nested.rowActions);
		assertEquals(1, nested.rowActions.size());
	}

	@Test void b04_selection_stampsAttributesAndPaintsSelectHeaderCell() {
		var nt = NestedTableDef.create(nested()).selection(SelectionDef.create("id").selectAll(true));
		var html = Html.of(ViewTable.of(tokenRequest(), view(nt)));
		var tag = nestedTableTag(html);
		assertTrue(tag.contains(ViewTable.SELECT_ATTR + "=\"1\""), tag);
		assertTrue(tag.contains(ViewTable.ROW_ID_FIELD_ATTR + "=\"id\""), tag);
		assertTrue(tag.contains(ViewTable.SELECT_ALL_ATTR + "=\"1\""), tag);
		assertTrue(html.contains("juneau-view-select-th"), html);
	}

	@Test void b05_noSelection_stampsNothing() {
		var tag = nestedTableTag(Html.of(ViewTable.of(tokenRequest(), view())));
		assertFalse(tag.contains(ViewTable.SELECT_ATTR), tag);
		assertFalse(tag.contains(ViewTable.SELECT_ALL_ATTR), tag);
	}

	@Test void b06_parentOnly_noNestedChooserHostAndNoNestedBulkSidecar() {
		var nt = NestedTableDef.create(nested()).selection(SelectionDef.create("id"));
		var html = Html.of(ViewTable.of(tokenRequest(), view(nt)));
		assertFalse(html.contains("juneau-view-nested-config"), html);
		assertFalse(html.contains(ViewTable.BULK_SIDECAR_ID_PREFIX + "events"), html);
		assertFalse(html.contains(ViewTable.BULK_ATTR), html);
	}

	@Test void b07_nestedDetailSections_emitTheirOwnTemplateAndExpanderCell() {
		var inner = ViewDef.create("events")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/events")
			.columns(Column.of("when").title("When"))
			.details(RowDetailDef.create().endpoint("/data/events/{id}")
				.sections(DetailSection.create("payload", "Payload").fields(DetailField.of("body"))))
			.build();
		var html = Html.of(ViewTable.of(tokenRequest(), view(NestedTableDef.create(inner))));
		// Two row-detail templates now: the parent's, and the nested view's own inside the nested wrapper.
		assertEquals(2, count(html, "data-juneau-row-detail=\"1\""), html);
		assertTrue(html.contains("data-juneau-detail-url=\"/data/events/{id}\""), html);
		assertTrue(html.contains("data-juneau-detail-section=\"payload\""), html);
		assertEquals(2, count(html, ViewTable.DETAIL_TH_CLASS), html);
	}

	private static int count(String s, String sub) {
		var n = 0;
		for (var i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + sub.length()))
			n++;
		return n;
	}
}
