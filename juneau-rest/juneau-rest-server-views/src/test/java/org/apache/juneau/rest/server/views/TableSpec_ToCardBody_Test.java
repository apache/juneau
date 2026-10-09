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
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.rest.server.widgets.Op;
import org.junit.jupiter.api.*;

/** {@link TableSpec#toCardBody()}: key order, omit-when-absent, and the nested wire shapes (rowClassRules' {@code class}
 * key, defaultOrder's {@code key} key, selection/bulk, quickStats' {@code id}/{@code items}). */
@SuppressWarnings("unchecked")
class TableSpec_ToCardBody_Test extends TestBase {

	private static RowDetail detail() {
		return RowDetail.create("/rest/slo/data/{id}")
			.region(RegionDef.create("slo-detail").populate("slo-detail").allowPopulators("slo-detail"));
	}

	@Test void a01_minimal_keyOrderAndOmission() {
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod")).toCardBody();
		assertEquals(List.of("dataUrl", "columns"), List.copyOf(body.keySet()));
		assertEquals("/rest/slo/data", body.get("dataUrl"));
		assertFalse(body.containsKey("contractVersion"));
	}

	@Test void a02_fullTable_keyOrder() {
		var permit = WritePermit.forCapability("slo:bulk");
		var selection = SelectionDef.create("id");
		var bulk = BulkMutateDef.create(permit, selection).actions(RowAction.create("ack"));
		var body = TableSpec.create("slo")
			.dataUrl("/rest/slo/data")
			.columns(Column.create("pod"))
			.dataMode(TableSpec.DataMode.CLIENT)
			.defaultOrder("pod", TableSpec.Sort.ASC)
			.ribbon(RibbonItem.refresh())
			.rowType(String.class)
			.pollIntervalMs(5000)
			.primary(true)
			.rowClass("state", Op.EQ, "degraded", "row-degraded")
			.rowActions(RowAction.create("ack"))
			.selection(selection)
			.bulk(bulk)
			.detail(detail())
			.quickStats("strip", QuickStat.of("x", "X", 1))
			.toCardBody();
		assertEquals(List.of("dataUrl", "columns", "dataMode", "defaultOrder", "ribbon", "rowType",
			"pollIntervalMs", "primary", "rowClassRules", "rowActions", "selection", "bulk", "detail", "quickStats"),
			List.copyOf(body.keySet()));
	}

	@Test void a03_rowClassRule_usesClassKeyNotCssClass() {
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.rowClass("state", Op.EQ, "degraded", "row-degraded").toCardBody();
		var rules = (List<Map<String,Object>>) body.get("rowClassRules");
		assertEquals("row-degraded", rules.get(0).get("class"));
		assertFalse(rules.get(0).containsKey("cssClass"));
	}

	@Test void a04_defaultOrder_usesKeyNotData() {
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.defaultOrder("pod", TableSpec.Sort.DESC).toCardBody();
		var order = (List<Map<String,Object>>) body.get("defaultOrder");
		assertEquals("pod", order.get(0).get("key"));
		assertEquals("desc", order.get(0).get("dir"));
	}

	@Test void a05_selection_omitsBulkFields_bulk_omitsContractVersion() {
		var permit = WritePermit.forCapability("slo:bulk");
		var selection = SelectionDef.create("id").selectAll(false);
		var bulk = BulkMutateDef.create(permit, selection).actions(RowAction.create("ack"));
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.selection(selection).bulk(bulk).toCardBody();
		var s = (Map<String,Object>) body.get("selection");
		assertEquals("id", s.get("rowIdField"));
		assertEquals(false, s.get("selectAll"));
		var b = (Map<String,Object>) body.get("bulk");
		assertFalse(b.containsKey("contractVersion"));
		assertEquals(1, ((List<?>) b.get("actions")).size());
	}

	@Test void a06_quickStats_shape() {
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.quickStats("strip", QuickStat.of("x", "X", 1)).toCardBody();
		var q = (Map<String,Object>) body.get("quickStats");
		assertEquals("strip", q.get("id"));
		assertEquals(1, ((List<?>) q.get("items")).size());
	}

	@Test void a07_detail_delegatesToRowDetailToMap() {
		var detail = detail().title("{pod}");
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.detail(detail).toCardBody();
		assertEquals(detail.toMap(), body.get("detail"));
	}

	@Test void a08_detail_includesEndpoint() {
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.detail(detail()).toCardBody();
		assertEquals("/rest/slo/data/{id}", ((Map<String,Object>)body.get("detail")).get("endpoint"));
	}

	@Test void a09_selection_emitsScope() {
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.selection(SelectionDef.create("id").labelField("pod")).toCardBody();
		var s = (Map<String,Object>)body.get("selection");
		assertEquals(SelectionDef.Scope.PERSISTENT.wire(), s.get("scope"));
		assertEquals("pod", s.get("labelField"));
		assertFalse(s.containsKey("selectableWhen"));
	}

	@Test void a10_tableOperators_reachInheritingColumns() {
		var table = SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"));
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").operators(table)
			.columns(Column.create("pod").searchType(SearchType.TEXT), Column.create("note")).toCardBody();
		var cols = (List<Map<String,Object>>)body.get("columns");
		assertEquals(List.of("$eq"), cols.get(0).get("searchOperators"));
		assertFalse(cols.get(1).containsKey("searchOperators"));
	}

	@Test void a11_selection_scopePageAndSelectableWhen() {
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.selection(SelectionDef.create("id").scope(SelectionDef.Scope.PAGE)
				.selectableWhen(RowActionEnabledRule.of("state", Op.EQ, "open", "Not open")))
			.toCardBody();
		var s = (Map<String,Object>)body.get("selection");
		assertEquals("page", s.get("scope"));
		var rules = (List<Map<String,Object>>)s.get("selectableWhen");
		assertEquals("state", rules.get(0).get("field"));
		assertEquals("Not open", rules.get(0).get("reason"));
	}

	@Test void a12_bodyHoldsOnlyPlainData() {
		var permit = WritePermit.forCapability("slo:bulk");
		var selection = SelectionDef.create("id");
		var bulk = BulkMutateDef.create(permit, selection)
			.actions(RowAction.create("ack").endpoint("/rest/slo/ack").bulkMode(RowAction.BulkMode.AGGREGATE));
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.columns(Column.create("pod").render("tag:state").href("{url}").searchType(SearchType.TEXT))
			.rowActions(RowAction.create("go").endpoint("/rest/slo/go").enabledWhen("state", Op.EQ, "open", "Not open"))
			.selection(selection).bulk(bulk).toCardBody();
		assertPlain(body);
		var actions = (List<Map<String,Object>>)((Map<String,Object>)body.get("bulk")).get("actions");
		assertEquals("aggregate", actions.get(0).get("bulkMode"));
		var col = ((List<Map<String,Object>>)body.get("columns")).get(0);
		assertEquals("tag", ((Map<String,Object>)col.get("render")).get("id"));
		assertEquals(List.of("key", "label", "render", "href"), List.copyOf(col.keySet()).subList(0, 4));
	}

	private static void assertPlain(Object o) {
		if (o instanceof Map<?,?> m)
			m.values().forEach(TableSpec_ToCardBody_Test::assertPlain);
		else if (o instanceof List<?> l)
			l.forEach(TableSpec_ToCardBody_Test::assertPlain);
		else
			assertTrue(o == null || o instanceof String || o instanceof Number || o instanceof Boolean, String.valueOf(o));
	}
}
