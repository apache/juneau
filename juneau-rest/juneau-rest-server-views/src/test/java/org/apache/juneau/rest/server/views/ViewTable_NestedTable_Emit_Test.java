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
import org.junit.jupiter.api.*;

/**
 * {@link ViewTable} emits a nested read-only table shell inside a detail section: a {@code data-juneau-view} table
 * with no HTML {@code id}, an {@code id}-less sibling VIEW_META sidecar found via {@code data-juneau-nested-meta}, and
 * the wrapper carrying the independent nested contract version + parent-scope parameter.
 */
class ViewTable_NestedTable_Emit_Test extends TestBase {

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
		assertTrue(html.contains("data-juneau-nested-contract=\"1\""), html);
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
}
