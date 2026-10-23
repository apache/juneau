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

import static org.apache.juneau.test.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.widgets.Op;
import org.apache.juneau.rest.server.widgets.StatusTone;
import org.junit.jupiter.api.*;

/** {@link TableSpec#toCardBody()}: key order, omit-when-absent, and the nested wire shapes (rowClassRules' {@code class}
 * key, defaultOrder's {@code key} key, selection/bulk, quickStats' {@code id}/{@code items}). */
@SuppressWarnings({
	"unchecked" // Assertions cast the card body's nested values to their known Map/List shapes.
})
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
		assertSize(1, ((List<?>) b.get("actions")));
	}

	@Test void a06_quickStats_shape() {
		var body = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.quickStats("strip", QuickStat.of("x", "X", 1)).toCardBody();
		var q = (Map<String,Object>) body.get("quickStats");
		assertEquals("strip", q.get("id"));
		assertSize(1, ((List<?>) q.get("items")));
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

	@Test void a13_quickStats_mixedKinds_inOrder() {
		var body = minimal().quickStats("strip", QuickStat.of("t", "T", 1), QuickStat.meter("m", "M", 1, 2),
			QuickStat.segmented("g", "G").segment("A", 1, StatusTone.INFO)).toCardBody();
		var items = (List<Map<String,Object>>)((Map<String,Object>) body.get("quickStats")).get("items");
		assertEquals(List.of("t", "m", "g"), items.stream().map(i -> i.get("id")).toList());
		assertFalse(items.get(0).containsKey("type"));
		assertEquals("meter", items.get(1).get("type"));
		assertEquals("A", ((Map<String,Object>)((List<?>)items.get(2).get("segments")).get(0)).get("label"));
	}

	@Test void a14_quickStats_emptySegmented_throwsAtToCardBody() {
		var t = minimal().quickStats("strip", QuickStat.segmented("g", "G"));
		var ex = assertThrows(IllegalArgumentException.class, t::toCardBody);
		assertTrue(ex.getMessage().contains("a segmented item needs at least one segment."), ex.getMessage());
	}

	private static TableSpec minimal() {
		return TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"));
	}

	@Test void b01_viewKeys_absentWhenUnset() {
		var body = minimal().toCardBody();
		for (var key : List.of("columnConfig", "copyLink", "cleanAddress", "pausePollingWhileEditing"))
			assertFalse(body.containsKey(key), key);
	}

	@Test void b02_viewKeys_emittedWhenSet() {
		var body = minimal().columnConfig(true).copyLink(true).cleanAddress(true).pausePollingWhileEditing(true).toCardBody();
		assertPlain(body);
		assertEquals(true, body.get("columnConfig"));
		assertEquals(true, body.get("copyLink"));
		assertEquals(true, body.get("cleanAddress"));
		assertEquals(true, body.get("pausePollingWhileEditing"));
	}

	@Test void b02b_urlState_absentWhenUnset_andEmittedAsDirectiveObjects() {
		assertFalse(minimal().toCardBody().containsKey("urlState"));
		var body = minimal().urlState("day", "ids").toCardBody();
		assertPlain(body);
		assertEquals(Map.of("day", Map.of(), "ids", Map.of()), body.get("urlState"));
	}

	@Test void b02d_urlStateDayParam_emitsTheParamName_andPlainDayStaysEmpty() {
		assertEquals(Map.of("day", Map.of("param", "on")), minimal().urlStateDayParam("on").toCardBody().get("urlState"));
		assertEquals(Map.of("day", Map.of()), minimal().urlState("day").toCardBody().get("urlState"));
		var both = minimal().urlState("ids").urlStateDayParam("on").urlState("day", "window").toCardBody();
		assertPlain(both);
		assertEquals(Map.of("ids", Map.of(), "day", Map.of("param", "on"), "window", Map.of()), both.get("urlState"));
		assertEquals(List.of("ids", "day", "window"), List.copyOf(((Map<?,?>)both.get("urlState")).keySet()));
	}

	@Test void b02e_urlStateDayParam_rejectsBlankAndMalformed() {
		var target = minimal();
		assertThrows(IllegalArgumentException.class, () -> target.urlStateDayParam(" "));
		var target2 = minimal();
		assertThrows(IllegalArgumentException.class, () -> target2.urlStateDayParam(null));
		var target3 = minimal();
		for (var bad : List.of("", "on&x=1", "a b", " on", "on ", "1on", "-on", "o.n", "a".repeat(65)))
			assertThrows(IllegalArgumentException.class, () -> target3.urlStateDayParam(bad), "'" + bad + "'");
		assertEquals(Map.of("day", Map.of("param", "a".repeat(64))), minimal().urlStateDayParam("a".repeat(64)).toCardBody().get("urlState"));
	}

	@Test void b02f_urlStateDayParam_lastCallWins_andPlainUrlStateKeepsIt() {
		var body = minimal().urlStateDayParam("a").urlStateDayParam("b").urlState("day").toCardBody();
		assertEquals(Map.of("day", Map.of("param", "b")), body.get("urlState"));
	}

	@Test void b02g_urlStateConfig_idsAndWindow_emittedAndMatchTheFtlMapForm() throws Exception {
		var body = minimal().urlState("ids", Map.of("param", "pid", "max", 50)).toCardBody();
		assertPlain(body);
		assertEquals(Map.of("ids", Map.of("param", "pid", "max", 50)), body.get("urlState"));
		// Twin: the map a page author writes in FTL / JSON5 is the same value the builder emits.
		assertEquals(Json5.to("{ids:{param:'pid',max:50}}", Map.class), body.get("urlState"));
		var w = minimal().urlState("window", Map.of("params", Map.of("start", "from", "end", "to"))).toCardBody();
		assertPlain(w);
		assertEquals(Map.of("window", Map.of("params", Map.of("start", "from", "end", "to"))), w.get("urlState"));
		assertEquals(Json5.to("{window:{params:{start:'from',end:'to'}}}", Map.class), w.get("urlState"));
		assertEquals(Map.of("day", Map.of("param", "on")), minimal().urlState("day", Map.of("param", "on")).toCardBody().get("urlState"));
	}

	@Test void b02h_urlStateConfig_mergesIntoExistingEntry_keepsFirstMentionPosition() {
		var body = minimal().urlState("ids", "day").urlState("ids", Map.of("param", "a")).urlState("ids", Map.of("max", 5)).urlState("ids", Map.of("param", "b")).toCardBody();
		assertEquals(Map.of("ids", Map.of("param", "b", "max", 5), "day", Map.of()), body.get("urlState"));
		assertEquals(List.of("ids", "day"), List.copyOf(((Map<?,?>)body.get("urlState")).keySet()));
		// A later plain mention never clears it, and day shares the entry with urlStateDayParam.
		var d = minimal().urlStateDayParam("on").urlState("day", Map.of()).urlState("day").toCardBody();
		assertEquals(Map.of("day", Map.of("param", "on")), d.get("urlState"));
		var wp = minimal().urlState("window", Map.of("params", Map.of("start", "from"))).urlState("window", Map.of("params", Map.of("end", "to"))).toCardBody();
		assertEquals(Map.of("window", Map.of("params", Map.of("start", "from", "end", "to"))), wp.get("urlState"));
		// New names are appended after existing ones.
		assertEquals(List.of("day", "ids"), List.copyOf(((Map<?,?>)minimal().urlState("day").urlState("ids", Map.of("max", 3)).toCardBody().get("urlState")).keySet()));
	}

	@Test void b02i_urlStateConfig_refusesInvalidOptions() {
		var t = minimal();
		for (var bad : List.<Map<String,Object>>of(
			Map.of("bogus", 1), Map.of("param", ""), Map.of("param", "a&b"), Map.of("param", 5), Map.of("param", "1x"),
			Map.of("max", 0), Map.of("max", -1), Map.of("max", 1.5), Map.of("max", "50"), Map.of("max", 2.0)))
			assertThrows(IllegalArgumentException.class, () -> t.urlState("ids", bad), "ids " + bad);
		for (var bad : List.<Map<String,Object>>of(
			Map.of("param", "a b"), Map.of("param", "x".repeat(65)), Map.of("max", 5), Map.of("bogus", "x")))
			assertThrows(IllegalArgumentException.class, () -> t.urlState("day", bad), "day " + bad);
		for (var bad : List.<Map<String,Object>>of(
			Map.of("param", "x"), Map.of("params", "x"), Map.of("params", Map.of("middle", "m")), Map.of("params", Map.of("start", "")),
			Map.of("params", Map.of("end", "a&b")), Map.of("params", Map.of("start", 1))))
			assertThrows(IllegalArgumentException.class, () -> t.urlState("window", bad), "window " + bad);
		Map<String,Object> arg2 = Map.of();
		assertThrows(IllegalArgumentException.class, () -> t.urlState(" ", arg2));
		Map<String,Object> arg3 = Map.of();
		assertThrows(IllegalArgumentException.class, () -> t.urlState((String)null, arg3));
		var arg4 = (Map<String,Object>)null;
		assertThrows(IllegalArgumentException.class, () -> t.urlState("ids", arg4));
		// A refused call leaves nothing behind.
		assertFalse(t.toCardBody().containsKey("urlState"));
		var partial = minimal().urlState("ids", Map.of("param", "ok"));
		Map<String,Object> invalid = Map.of("param", "also-ok", "max", 0);
		assertThrows(IllegalArgumentException.class, () -> partial.urlState("ids", invalid));
		assertEquals(Map.of("ids", Map.of("param", "ok")), partial.toCardBody().get("urlState"));
	}

	@Test void b02j_urlStateConfig_refusesNonBuiltInDirectives() {
		var target4 = minimal();
		Map<String,Object> none = Map.of();
		for (var name : List.of("custom", "Day", "tab", "filter", "sort"))
			assertThrows(IllegalArgumentException.class, () -> target4.urlState(name, none), name);
		var target5 = minimal();
		Map<String,Object> arg = Map.of("k", "v");
		assertThrows(IllegalArgumentException.class, () -> target5.urlState("custom", arg));
		// The names-only form refuses the same names, with the same message.
		for (var name : List.of("custom", "Day", "tab", "filter", "sort")) {
			var target6 = minimal();
			var ex = assertThrows(IllegalArgumentException.class, () -> target6.urlState(name), name);
			assertEquals("urlState '" + name + "' is not a built-in directive; expected day, ids or window.", ex.getMessage());
		}
		assertEquals(Map.of("day", Map.of(), "ids", Map.of(), "window", Map.of()), minimal().urlState("day", "ids", "window").toCardBody().get("urlState"));
	}

	@Test void b02j2_urlState_namesOnly_isAtomic() {
		var t = minimal();
		assertThrows(IllegalArgumentException.class, () -> t.urlState("ids", "bogus"));
		assertNull(t.toCardBody().get("urlState"));
		assertThrows(IllegalArgumentException.class, () -> t.urlState("ids", " "));
		assertNull(t.toCardBody().get("urlState"));
		var u = minimal().urlState("day");
		assertThrows(IllegalArgumentException.class, () -> u.urlState("ids", "window", "bogus"));
		assertEquals(Map.of("day", Map.of()), u.toCardBody().get("urlState"));
	}

	@Test void b02k_urlStateConfig_isDeepCopiedOnInputAndOnEmit() {
		var params = new java.util.HashMap<String,Object>();
		params.put("start", "from");
		var cfg = new java.util.HashMap<String,Object>();
		cfg.put("params", params);
		var t = minimal().urlState("window", cfg);
		params.put("start", "mutated");
		params.put("end", "extra");
		cfg.put("bogus", 1);
		var expected = Map.of("window", Map.of("params", Map.of("start", "from")));
		assertEquals(expected, t.toCardBody().get("urlState"));
		// Mutating what toCardBody() returned must not reach the spec's own state.
		var emitted = (Map<String,Object>)t.toCardBody().get("urlState");
		((Map<String,Object>)((Map<String,Object>)emitted.get("window")).get("params")).put("start", "emitted");
		((Map<String,Object>)emitted.get("window")).put("param", "x");
		assertEquals(expected, t.toCardBody().get("urlState"));
		// Same for a flat config.
		var flat = new java.util.HashMap<String,Object>();
		flat.put("param", "pid");
		var t2 = minimal().urlState("ids", flat);
		flat.put("param", "mutated");
		((Map<String,Object>)((Map<String,Object>)t2.toCardBody().get("urlState")).get("ids")).put("param", "emitted");
		assertEquals(Map.of("ids", Map.of("param", "pid")), t2.toCardBody().get("urlState"));
	}

	@Test void b02l_urlStateConfig_windowParams_sameKeyLastWins_otherKeyKept() {
		var body = minimal().urlState("window", Map.of("params", Map.of("start", "from", "end", "to")))
			.urlState("window", Map.of("params", Map.of("start", "FROM"))).toCardBody();
		assertEquals(Map.of("window", Map.of("params", Map.of("start", "FROM", "end", "to"))), body.get("urlState"));
	}

	@Test void b02c_urlState_rejectsABlankName() {
		var target7 = minimal();
		assertThrows(IllegalArgumentException.class, () -> target7.urlState(" "));
		var target8 = minimal();
		assertThrows(IllegalArgumentException.class, () -> target8.urlState((String)null));
	}

	@Test void b03_viewKeys_falseIsEmitted() {
		var body = minimal().columnConfig(false).copyLink(false).cleanAddress(false).pausePollingWhileEditing(false).toCardBody();
		assertEquals(false, body.get("columnConfig"));
		assertEquals(false, body.get("copyLink"));
		assertEquals(false, body.get("cleanAddress"));
		assertEquals(false, body.get("pausePollingWhileEditing"));
	}

	@Test void b04_columnConfig_objectForm() {
		var body = minimal().columnConfig(Map.of("chooser", true)).toCardBody();
		assertPlain(body);
		assertEquals(Map.of("chooser", true), body.get("columnConfig"));
	}

	@Test void b05_columnConfig_nullOptionsRejected() {
		var t = minimal();
		var arg5 = (Map<String,Object>)null;
		assertThrows(NullPointerException.class, () -> t.columnConfig(arg5));
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
