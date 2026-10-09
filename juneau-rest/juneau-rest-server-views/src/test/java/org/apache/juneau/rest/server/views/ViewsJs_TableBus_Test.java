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
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The datatables card type's bus surface (message bus addendum, spec §4.2-§4.5, §5.3, §5.4): the five publishers, the
 * {@code cmd:<id>} ops, the {@code filter} role, and the per-card gating of {@code selection}/{@code detail}/{@code bulk}.
 * Runs {@code table-bus.cjs} through {@link BusHarness}.
 */
class ViewsJs_TableBus_Test extends TestBase {

	static Map<String,Object> report;

	@BeforeAll static void runHarness() {
		report = BusHarness.run("table-bus.cjs",
			ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.RIBBON_JS_RESOURCE, ViewsMixin.SEARCH_JS_RESOURCE);
	}

	@BeforeEach void needsNode() {
		assumeTrue(report != null, "node not on PATH");
		assertNull(report.get("crash"), () -> "harness crashed: " + report.get("crash"));
		assertEquals(true, report.get("hasTableBus"), "NS.tableBus is not exported");
	}

	@Test void a01_ops() {
		assertEquals("[\"reload\",\"clear-selection\",\"select\",\"set-filter\",\"pause-polling\",\"resume-polling\",\"collapse-all\"]", report.get("ops"));
	}

	@Test void a02_selectionPayload() {
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"v\",\"ids\":[\"b\",\"c\"],\"rows\":[{\"id\":\"b\"},{\"id\":\"c\"}],\"added\":[\"c\"],\"removed\":[\"a\"],\"count\":2}", report.get("selPure"));
		assertEquals(Map.of("ids", 3, "rows", 2, "rowsTruncated", true), report.get("selCap"));
		assertEquals(200, report.get("selDefaultCap"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"v\",\"ids\":[],\"rows\":[],\"added\":[],\"removed\":[\"a\"],\"count\":0}", report.get("selEmpty"));
	}

	@Test void a03_otherPayloads() {
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"v\",\"search\":\"ssc\",\"columns\":{\"owner\":\"jb\",\"state\":\"open\"},\"options\":{\"mine\":true}}", report.get("filterPure"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"v\",\"rowCount\":412,\"page\":{\"index\":1,\"size\":25,\"total\":17},\"nested\":false}", report.get("redrawPure"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"v\",\"rowId\":\"c-17\",\"expanded\":true,\"generation\":3}", report.get("detailPure"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"v\",\"action\":\"close\",\"succeeded\":[\"c-17\"],\"notFound\":[],\"failed\":[{\"id\":\"c-21\",\"status\":409}]}", report.get("bulkPure"));
	}

	/** Mirrors {@code DatatablesCardType.implicitTopics}: a card with no {@code dataUrl} (catalog-driven) gets all five. */
	@Test void a04_implicitTopicsFollowTheJavaRule() {
		assertEquals("[\"selection:changes\",\"filter:changes\",\"redraw:changes\",\"detail:changes\",\"bulk:changes\"]", report.get("implicitFull"));
		assertEquals("[\"filter:t\",\"redraw:t\"]", report.get("implicitCatalog"));
		assertEquals("[\"filter:f\",\"redraw:f\",\"bulk:f\"]", report.get("implicitFlat"));
		assertEquals("[\"selection:s\",\"filter:s\",\"redraw:s\",\"detail:s\",\"bulk:s\"]", report.get("implicitSrcOnly"));
	}

	@Test void a05_optionStateListsEveryDeclaredOption() {
		assertEquals("{\"mine\":false,\"openOnly\":false,\"age\":null}", report.get("optionStateEmpty"));
	}

	@Test void b01_bindPublishesInitialFilterAndEmptySelection() {
		assertEquals("changes", report.get("ctxBusCardId"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"search\":\"\",\"columns\":{},\"options\":{\"mine\":false,\"openOnly\":false,\"age\":null}}", report.get("bindFilter"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"ids\":[],\"rows\":[],\"added\":[],\"removed\":[],\"count\":0}", report.get("bindSelection"));
		assertEquals(0, report.get("bindRedraws"), "an ajax table's first redraw: comes from its own draw.dt");
	}

	@Test void b02_columnsAndSearchPatch() {
		var m = map(report.get("clientColumns"));
		assertEquals("[{\"index\":2,\"value\":\"jb\",\"regex\":false,\"smart\":true}]", m.get("searches"));
		assertEquals("[\"ssc\"]", m.get("globalSearch"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"search\":\"ssc\",\"columns\":{\"owner\":\"jb\"},\"options\":{\"mine\":false,\"openOnly\":false,\"age\":\"all\"}}", m.get("filter"));
	}

	@Test void b03_aColumnAnOptionAlsoFiltersIsAnOrdinaryColumn() {
		var m = map(report.get("optionColumn"));
		assertEquals("[{\"index\":3,\"value\":\"x\",\"regex\":false,\"smart\":true}]", m.get("searches"));
		assertEquals("{\"owner\":\"jb\",\"state\":\"x\"}", m.get("columns"));
	}

	@Test void b04_unknownOptionMemberAndColumnAreRefusedLoudly() {
		var m = map(report.get("refused"));
		var errs = list(m.get("errors"));
		assertTrue(errs.stream().anyMatch(e -> e.toString().contains("unknown ribbon option 'nope'")), () -> errs.toString());
		assertTrue(errs.stream().anyMatch(e -> e.toString().contains("optionGroup 'age' has no member 'nope'")), () -> errs.toString());
		assertTrue(errs.stream().anyMatch(e -> e.toString().contains("unknown column 'nope'")), () -> errs.toString());
		assertEquals("[]", m.get("searches"));
		assertEquals("all", m.get("ageStill"));
		assertEquals(0, m.get("newFilters"), "a refused patch changes nothing, so nothing is republished");
	}

	@Test void b05_drawPublishesRedrawButNotAnUnchangedFilter() {
		var m = map(report.get("draw"));
		assertEquals(0, m.get("newFilters"));
		assertEquals(1, m.get("newRedraws"), "a nested table's draw.dt must not publish the parent's redraw");
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"rowCount\":3,\"page\":{\"index\":0,\"size\":25,\"total\":1},\"nested\":false}", m.get("redraw"));
	}

	@Test void c01_reloadDefaultsToKeepingThePage() {
		assertEquals("[false,null]", report.get("reloads"));
	}

	@Test void c02_selectAndClear() {
		var s = map(report.get("select"));
		assertEquals("[\"c-21\"]", s.get("selected"), "select keeps only ids on the current page");
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"ids\":[\"c-21\"],\"rows\":[{\"id\":\"c-21\",\"owner\":\"ak\",\"state\":\"closed\"}],\"added\":[\"c-21\"],\"removed\":[],\"count\":1}", s.get("payload"));
		assertEquals(1, s.get("toolbar"));
		var c = map(report.get("clear"));
		assertEquals(0, c.get("selected"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"ids\":[],\"rows\":[],\"added\":[],\"removed\":[\"c-21\"],\"count\":0}", c.get("payload"));
	}

	@Test void c03_pauseResumeCollapse() {
		assertEquals(true, report.get("paused"));
		assertEquals(false, report.get("resumed"));
		assertEquals(2, report.get("pauseNotes"));
		assertEquals(1, report.get("collapses"));
	}

	@Test void c04_unknownOpIsEJS48() {
		var m = map(report.get("unknownOp"));
		assertNotNull(m, "an unknown op must throw, not be ignored");
		assertEquals("E-JS-48", m.get("code"));
		assertTrue(m.get("message").toString().contains("card 'changes' (type 'datatables') has no op 'explode'"), () -> m.toString());
	}

	@Test void d01_checkboxChangePublishesSelection() {
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"ids\":[\"c-30\"],\"rows\":[{\"id\":\"c-30\",\"owner\":\"jb\",\"state\":\"open\"}],\"added\":[\"c-30\"],\"removed\":[],\"count\":1}", report.get("checkbox"));
	}

	@Test void e01_detailAndBulk() {
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"rowId\":\"c-17\",\"expanded\":true,\"generation\":3}", report.get("detail"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"action\":\"close\",\"succeeded\":[\"c-17\"],\"notFound\":[],\"failed\":[{\"id\":\"c-21\",\"status\":409}]}", report.get("bulk"));
	}

	@Test void e02_optionalTopicsAreGatedOnCardKeys() {
		assertEquals(Map.of("detail", 0, "bulk", 0, "selection", 0, "filter", 1, "redraw", 0), report.get("bare"));
	}

	@Test void f01_filterRoleAppliesMappedFieldsAndIgnoresAClear() {
		var m = map(report.get("role"));
		assertEquals("[{\"index\":5,\"value\":\"east\",\"regex\":false,\"smart\":true}]", m.get("searches"));
		assertEquals("east", m.get("region"));
		assertEquals("new", m.get("age"));
		assertEquals("zz", m.get("search"));
		assertEquals(0, report.get("roleCleared"));
	}

	@Test void g01_nestedTableHasNoBus() {
		assertNull(report.get("nestedBus"));
		assertTrue(report.containsKey("nestedBus"));
	}

	@Test void h01_rebuildRepublishesTheUnchangedFilter() {
		assertEquals(1, map(report.get("rebuild")).get("newFilters"), "a rebuild resets the changed-only check, so Apply re-publishes");
	}

	@Test void h02_inlineRowsTableAnnouncesItsFirstDraw() {
		assertEquals(1, map(report.get("inline")).get("redraws"));
	}

	@Test void i01_cardHandlerWiring() {
		assertEquals(1, report.get("registrations"));
		var h = map(report.get("handler"));
		assertEquals("[\"reload\",\"clear-selection\",\"select\",\"set-filter\",\"pause-polling\",\"resume-polling\",\"collapse-all\"]", h.get("ops"));
		assertEquals("[\"filter\"]", h.get("roles"));
		assertEquals(true, h.get("implicitIsTableBus"));
		assertEquals(true, h.get("filterRoleIsTableBus"));
		var r = map(report.get("render"));
		assertEquals("mounted", r.get("result"));
		assertEquals("rc", r.get("stamped"));
		assertEquals("cleared", r.get("afterDestroy"));
		assertEquals("[false]", report.get("cardOpRan"));
		assertEquals("all", report.get("cardRole"));
	}

	@Test void i02_opOnAnUnmountedCardIsEJS48() {
		var m = map(report.get("unmounted"));
		assertNotNull(m);
		assertEquals("E-JS-48", m.get("code"));
		assertTrue(m.get("message").toString().contains("before its table is mounted"), () -> m.toString());
	}

	@Test void j01_dslColumnFilterIsInstalledAsAFixedPredicate() {
		var m = map(report.get("dslSet"));
		assertEquals("[]", m.get("columnSearches"), "a DSL column never takes a native column search");
		assertEquals("[{\"index\":6,\"name\":\"juneau-dsl\",\"installed\":true}]", m.get("fixed"));
		assertEquals("$eq(open)", m.get("store"));
		assertEquals("[null]", m.get("draws"), "one draw for the whole patch");
		assertEquals("[true,false]", m.get("matches"), "the installed predicate evaluates the row's raw cell");
		assertEquals("{\"stage\":\"$eq(open)\"}", m.get("filterColumns"));
	}

	@Test void j02_invalidDslColumnFilterIsRefusedAndKeepsTheOldOne() {
		var m = map(report.get("dslBad"));
		var errs = list(m.get("errors"));
		assertEquals(1, errs.size(), () -> errs.toString());
		assertTrue(errs.get(0).toString().contains("column 'stage'"), () -> errs.toString());
		assertEquals("$eq(open)", m.get("store"));
		assertEquals(0, m.get("newFixed"));
		assertEquals(0, m.get("newFilters"));
		assertEquals("{\"stage\":\"$eq(open)\"}", m.get("filterColumns"));
	}

	@Test void j03_filterPayloadReadsADslColumnFromTheExpressionStore() {
		var m = map(report.get("dslRead"));
		assertEquals("{\"stage\":\"$eq(closed)\"}", m.get("filterColumns"));
		assertEquals("[]", m.get("columnSearches"));
	}

	@Test void j04_blankExpressionRemovesTheDslPredicate() {
		var m = map(report.get("dslClear"));
		assertEquals("{\"index\":6,\"name\":\"juneau-dsl\",\"installed\":false}", m.get("fixed"));
		assertNull(m.get("store"));
		assertEquals("{}", m.get("filterColumns"));
	}

	@Test void j05_serverModeKeepsTheNativeColumnSearch() {
		var m = map(report.get("dslServer"));
		assertEquals("[{\"index\":6,\"value\":\"$eq(open)\",\"regex\":false,\"smart\":true}]", m.get("searches"));
		assertEquals("[]", m.get("fixed"));
	}

	@Test void j06_dslSectionLogsOnlyTheRefusal() {
		assertEquals(1, list(report.get("dslErrors")).size(), () -> report.get("dslErrors").toString());
	}

	@Test void k01_roleSkipsForeignOptionsAndColumnsQuietly() {
		var m = map(report.get("roleForeign"));
		assertEquals(List.of(), m.get("errors"), "a foreign payload is expected to carry extras");
		assertEquals("[{\"index\":2,\"value\":\"jb\",\"regex\":false,\"smart\":true}]", m.get("searches"));
	}

	@Test void k02_roleClearsWhatTheSourceDroppedAndLeavesTheTargetsOwnFilters() {
		var m = map(report.get("roleClearing"));
		assertEquals("{\"owner\":\"jb\",\"region\":\"west\"}", m.get("appliedColumns"));
		assertEquals("{\"mine\":true,\"openOnly\":true,\"age\":null}", m.get("appliedOptions"));
		assertEquals("{\"region\":\"west\"}", m.get("columns"), "owner came from the source and is now gone from it; region is the target's own");
		assertEquals("{\"mine\":true,\"openOnly\":false,\"age\":null}", m.get("options"));
		assertEquals("[{\"index\":2,\"value\":\"jb\",\"regex\":false,\"smart\":true},{\"index\":2,\"value\":\"\",\"regex\":false,\"smart\":true}]", m.get("searches"));
		assertEquals(List.of(), report.get("roleErrors"));
	}

	@Test void l01_expandAndCollapseARowPublishDetail() {
		var m = map(report.get("detailToggle"));
		assertEquals("[[\"c-17\",true,1],[\"c-17\",false,1]]", m.get("expanded"));
	}

	@Test void l02_collapseAllPublishesEveryOpenRowClosing() {
		assertEquals("[[\"c-17\",true,2],[\"c-21\",true,1],[\"c-17\",false,2],[\"c-21\",false,1]]", report.get("detailCollapseAll"));
	}

	@Test void l03_theSafeCollapseButtonPublishesToo() {
		assertEquals("[[\"c-21\",true,2],[\"c-21\",false,2]]", report.get("detailSafeCollapse"));
	}

	@Test void m01_aPageScopedPruneOnDrawPublishesTheSelection() {
		var m = map(report.get("selectionPrune"));
		assertEquals(1, m.get("published"));
		assertTrue(m.get("last").toString().contains("\"ids\":[\"c-17\"]") && m.get("last").toString().contains("\"removed\":[\"c-99\"]"), () -> m.toString());
	}

	@Test void m02_clearSelectionLinkPublishesTheEmptySelection() {
		var m = map(report.get("clearLink"));
		assertEquals(0, m.get("selected"));
		assertEquals(1, m.get("published"));
		assertTrue(m.get("last").toString().contains("\"ids\":[]") && m.get("last").toString().contains("\"removed\":[\"c-17\",\"c-21\"]"), () -> m.toString());
	}

	@Test void n01_perRowBulkRunPublishesOutcomeWithHttpStatusAfterItsToast() {
		var m = map(report.get("bulkPerRow"));
		assertEquals(1, m.get("published"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"action\":\"close\",\"succeeded\":[\"c-17\"],\"notFound\":[\"c-30\"],\"failed\":[{\"id\":\"c-21\",\"status\":409}]}", m.get("payload"));
		assertEquals("[\"c-21\"]", m.get("selected"), "a failed id stays selected for a retry");
		assertEquals("[1]", m.get("bulkPublishedAfterToast"), "bulk: goes out after its summary toast (spec 4.4)");
	}

	@Test void n02_aggregateBulkRunReportsZeroForAFailureWithNoStatus() {
		var m = map(report.get("bulkAggregate"));
		assertEquals(1, m.get("published"));
		assertEquals("{\"schemaVersion\":1,\"viewId\":\"changes\",\"action\":\"close\",\"succeeded\":[\"c-17\"],\"notFound\":[],\"failed\":[{\"id\":\"c-21\",\"status\":409},{\"id\":\"c-30\",\"status\":0}]}", m.get("payload"));
		assertEquals("[1]", m.get("bulkPublishedAfterToast"));
	}

	@Test void o01_driveSectionLogsNothing() {
		assertEquals(List.of(), report.get("driveErrors"));
	}

	@Test void z_noUnexpectedConsoleErrors() {
		var errs = list(report.get("errors"));
		assertEquals(3, errs.size(), () -> "only the three deliberate refusals may log: " + errs);
	}

	@SuppressWarnings("unchecked")
	static Map<String,Object> map(Object o) { return (Map<String,Object>)o; }

	static List<?> list(Object o) { return (List<?>)o; }
}
