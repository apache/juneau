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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.Op;
import org.apache.juneau.rest.server.widgets.StatusTone;
import org.junit.jupiter.api.*;

class DatatablesCardType_Test extends TestBase {

	private final DatatablesCardType h = new DatatablesCardType();

	private static CardSource src(String id, String body) {
		return CardSource.create("datatables", id).body(body).build();
	}

	@Test void registeredUnderDatatables() {
		assertEquals("datatables", h.type());
	}

	@Test void srcOnly_emptyFragment() {
		var s = CardSource.create("datatables", "t").src("/rest/t").build();
		assertTrue(h.toFragment(s).isEmpty());
	}

	@Test void srcAndBody_throwsE26() {
		var s = CardSource.create("datatables", "t").src("/rest/t").body("{dataUrl:'/x', columns:[{key:'a'}]}").build();
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(s));
		assertEquals("<@card id='t'> type='datatables' takes src= or a body, not both.", ex.getMessage());
	}

	@Test void neitherSrcNorBody_throwsE27() {
		var s = CardSource.create("datatables", "t").build();
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(s));
		assertEquals("<@card id='t'> type='datatables' requires src= or a body with 'columns' and exactly one of 'dataUrl' or 'rows'.", ex.getMessage());
	}

	@Test void bodyMap_isAcceptedAsTheCatalog() {
		var catalog = Map.<String,Object>of("dataUrl", "/rest/t/data", "columns", List.of(Map.of("key", "a")));
		var f = h.toFragment(CardSource.create("datatables", "t").bodyMap(catalog).build());
		assertEquals("/rest/t/data", ((Map<?,?>)f.get("table")).get("dataUrl"));
	}

	@Test void bodyMap_fromTableSpec_withBulkRowActionsAndSelection() {
		var permit = WritePermit.forCapability("slo:bulk");
		var selection = SelectionDef.create("id").selectableWhen(RowActionEnabledRule.of("state", Op.EQ, "open", "Not open"));
		var bulk = BulkMutateDef.create(permit, selection)
			.actions(RowAction.create("abort").endpoint("/rest/slo/abort").bulkMode(RowAction.BulkMode.AGGREGATE));
		var spec = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.rowActions(RowAction.create("ack").endpoint("/rest/slo/ack"))
			.selection(selection).bulk(bulk)
			.detail(RowDetail.create("/rest/slo/data/{id}").region(RegionDef.create("d").populate("d").allowPopulators("d")));
		spec.validate();
		var f = h.toFragment(CardSource.create("datatables", "slo").bodyMap(spec.toCardBody()).build());
		assertEquals("/rest/slo/data", ((Map<?,?>)f.get("table")).get("dataUrl"));
	}

	@Test void bodyVisibleWhen_isLiftedOnce_notLeftInTheCatalog() {
		var catalog = new JsonMap().append("dataUrl", "/rest/t/data").append("columns", List.of(Map.of("key", "a")))
			.append("visibleWhen", List.of(Map.of("field", "x", "op", "present")));
		var card = CardTypeRegistry.standard().toCard(CardSource.create("datatables", "t").bodyMap(catalog).build());
		assertNotNull(card.get("visibleWhen"));
		assertFalse(((Map<?,?>)card.get("table")).containsKey("visibleWhen"), card.toString());
	}

	@Test void bodyMap_withoutColumns_throwsE27() {
		var s = CardSource.create("datatables", "t").bodyMap(Map.of("dataUrl", "/x")).build();
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(s));
		assertEquals("<@card id='t'> type='datatables' requires src= or a body with 'columns' and exactly one of 'dataUrl' or 'rows'.", ex.getMessage());
	}

	@Test void bodyMapAndSrc_throwsE26() {
		var s = CardSource.create("datatables", "t").src("/rest/t").bodyMap(Map.of("dataUrl", "/x")).build();
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(s));
		assertEquals("<@card id='t'> type='datatables' takes src= or a body, not both.", ex.getMessage());
	}

	@Test void missingDataUrlOrColumns_throwsE27() {
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t", "{columns:[{key:'a'}]}")));
		assertEquals("<@card id='t'> type='datatables' requires src= or a body with 'columns' and exactly one of 'dataUrl' or 'rows'.", ex.getMessage());
	}

	@Test void preBuiltSlotMeta_throwsE28() {
		var expected = "<@card id='t'> type='datatables' body is a pre-built SLOT_META envelope; "
			+ "remove 'contractVersion', 'layout' and 'view' and author the catalog form.";
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{contractVersion:'1', view:{id:'t', dataUrl:'/x', columns:[]}}")));
		assertEquals(expected, ex.getMessage());
		ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{view:{id:'t', dataUrl:'/x', columns:[]}}")));
		assertEquals(expected, ex.getMessage());
	}

	@Test void columnMissingKeyAndData_throwsE29() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{label:'No key'}]}")));
		assertEquals("<@card id='t'> columns['0'] requires 'key'.", ex.getMessage());
	}

	@Test void duplicateColumnKey_throwsE30() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'},{key:'a'}]}")));
		assertEquals("<@card id='t'> declares column 'a' twice.", ex.getMessage());
	}

	@Test void contractVersionAnywhere_throwsE31() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], detail:{contractVersion:'1', endpoint:'/x'}}")));
		assertTrue(ex.getMessage().contains("sets contractVersion; remove it"), ex.getMessage());
	}

	@Test void unknownSearchType_throwsE32() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a', searchType:'bogus'}]}")));
		assertTrue(ex.getMessage().startsWith("<@card id='t'> column 'a' searchType 'bogus' is unknown; known: '"), ex.getMessage());
	}

	@Test void unknownSearchOperator_throwsE33() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a', searchType:'text', searchOperators:['$bogus']}]}")));
		assertTrue(ex.getMessage().startsWith("<@card id='t'> column 'a' searchOperators names unknown operator '$bogus'; known: '"),
			ex.getMessage());
	}

	@Test void searchResolution_setsSearchAndStripsInputKeys() {
		var card = h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a', searchType:'text'}]}"));
		var col = ((JsonMap) card.get("table")).getList("columns").getMap(0);
		assertNotNull(col.get("search"));
		assertNull(col.get("searchType"));
	}

	@Test void bulkWithoutSelection_throwsE34() {
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t",
			"{dataUrl:'/x', columns:[{key:'a'}], bulk:{actions:[{id:'x',endpoint:'/x'}]}}")));
		assertEquals("<@card id='t'> sets bulk without selection.", ex.getMessage());
	}

	@Test void aggregateEndpointWithFieldToken_throwsE35_noCardPrefix() {
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t",
			"{dataUrl:'/x', columns:[{key:'a'}], selection:{rowIdField:'a'}, "
			+ "bulk:{actions:[{id:'abort', endpoint:'/rest/{id}/abort', mode:'aggregate'}]}}")));
		assertEquals("Bulk action 'abort' has mode 'aggregate' but its endpoint '/rest/{id}/abort' contains a "
			+ "{field} token; aggregate endpoints receive {ids} in the body.", ex.getMessage());
	}

	@Test void unknownColumnKey_throwsE36() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a', bogus:1}]}")));
		assertTrue(ex.getMessage().startsWith("<@card id='t'> column 'a' has unknown key 'bogus'; known: '"), ex.getMessage());
	}

	@Test void ribbonUnknownType_isRejected_columnSearchToggle() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], ribbon:[{type:'columnSearchToggle'}]}")));
		assertTrue(ex.getMessage().startsWith("<@card id='t'> RibbonItem type 'columnSearchToggle' is not one of '"), ex.getMessage());
	}

	@Test void ribbonUnknownKey_isRejected() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], ribbon:[{type:'refresh', bogus:1}]}")));
		assertEquals("<@card id='t'> RibbonItem refresh does not accept 'bogus'.", ex.getMessage());
	}

	@Test void ribbonExportWithoutButtons_isRejected() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], ribbon:[{type:'export'}]}")));
		assertEquals("<@card id='t'> RibbonItem export requires at least one button.", ex.getMessage());
	}

	@Test void ribbonDivider_isAccepted() {
		var table = (Map<?,?>)h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], ribbon:[{type:'refresh'}, {type:'divider'}]}")).get("table");
		assertEquals(2, ((List<?>)table.get("ribbon")).size());
	}

	@Test void ribbonDividerWithGroup_isRejected() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], ribbon:[{type:'divider', group:'g'}]}")));
		assertEquals("<@card id='t'> RibbonItem divider does not accept 'group'.", ex.getMessage());
	}

	@Test void ribbonVisibleWhen_isChecked() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], ribbon:[{type:'refresh', visibleWhen:5}]}")));
		assertTrue(ex.getMessage().contains("visibleWhen must be a list"), ex.getMessage());
	}

	@Test void immutableBodyMap_isNotMutated_andColumnsAreNormalized() {
		var col = Map.<String,Object>of("key", "a", "render", "tag:status");
		var cols = List.<Object>of(col);
		var body = Map.<String,Object>of("dataUrl", "/x", "columns", cols);
		var s = CardSource.create("datatables", "t").bodyMap(body).build();
		var table = (Map<?,?>)h.toFragment(s).get("table");
		assertEquals("tag:status", col.get("render"));
		var out = (Map<?,?>)((List<?>)table.get("columns")).get(0);
		assertEquals("tag", ((Map<?,?>)out.get("render")).get("id"));
	}

	@Test void ribbonBadOptionGroupMemberKey_isRejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t",
			"{dataUrl:'/x', columns:[{key:'a'}], ribbon:[{type:'optionGroup', id:'g', options:[{id:'a', persist:true}]}]}")));
		assertEquals("<@card id='t'> RibbonItem optionGroup 'g' member 'a' does not accept 'persist'.", ex.getMessage());
	}

	@Test void ribbonNonMapItem_isRejected() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], ribbon:['refresh']}")));
		assertEquals("<@card id='t'> ribbon each item must be an object.", ex.getMessage());
	}

	@Test void ribbonMissingType_saysSo() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], ribbon:[{id:'x'}]}")));
		assertTrue(ex.getMessage().startsWith("<@card id='t'> RibbonItem requires a 'type'"), ex.getMessage());
	}

	@Test void ribbonOptionWithoutScope_isRejected() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], ribbon:[{type:'option', id:'o', value:'v'}]}")));
		assertEquals("<@card id='t'> RibbonItem option 'o' sets neither column nor param.", ex.getMessage());
	}

	@Test void renderStringSugar_normalizesToObject() {
		var frag = h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a', render:'tag:status'}, {key:'b', render:'linked'}]}"));
		var cols = ((Map<?,?>)frag.get("table")).get("columns");
		assertEquals("[{key:'a',render:{id:'tag',meta:{field:'status'}}},{key:'b',render:{id:'linked'}}]",
			org.apache.juneau.marshall.marshaller.Json5.DEFAULT.write(cols));
	}

	@Test void hrefWithoutRender_impliesLinked() {
		var frag = h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a', href:'/p/{id}'}, {key:'b', href:'/q', render:'tag:status'}, {key:'c', href:'/r', render:{id:'pill'}}, {key:'d'}]}"));
		var cols = ((Map<?,?>)frag.get("table")).get("columns");
		assertEquals("[{key:'a',href:'/p/{id}',render:{id:'linked'}},{key:'b',href:'/q',render:{id:'tag',meta:{field:'status'}}},{key:'c',href:'/r',render:{id:'pill'}},{key:'d'}]",
			org.apache.juneau.marshall.marshaller.Json5.DEFAULT.write(cols));
	}

	@Test void validCatalog_returnsTableFragmentInAuthorShape() {
		var card = h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a', label:'A'}]}"));
		var table = (JsonMap) card.get("table");
		assertEquals("/x", table.get("dataUrl"));
		assertEquals("a", table.getList("columns").getMap(0).get("key"));
	}

	@Test void inlineRows_only_ok() {
		var table = (JsonMap) h.toFragment(src("t", "{rows:[{a:1}], columns:[{key:'a'}]}")).get("table");
		assertEquals(1, table.getList("rows").size());
		assertFalse(table.containsKey("dataUrl"));
	}

	@Test void inlineRows_empty_ok() {
		var table = (JsonMap) h.toFragment(src("t", "{rows:[], columns:[{key:'a'}]}")).get("table");
		assertEquals(0, table.getList("rows").size());
	}

	@Test void inlineRows_andDataUrl_throwsE27() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/d', rows:[], columns:[{key:'a'}]}")));
		assertTrue(ex.getMessage().contains("exactly one of 'dataUrl' or 'rows'"), ex.getMessage());
	}

	@Test void missingColumns_throwsE27() {
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t", "{dataUrl:'/d'}")));
		assertTrue(ex.getMessage().contains("'columns'"), ex.getMessage());
	}

	@Test void inlineRows_malformed_rejected() {
		assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t", "{rows:'nope', columns:[{key:'a'}]}")));
		assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t", "{rows:[1], columns:[{key:'a'}]}")));
	}

	@Test void inlineRows_withServerModeOrPolling_rejected() {
		var server = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{rows:[], dataMode:'server', columns:[{key:'a'}]}")));
		assertTrue(server.getMessage().contains("dataMode"), server.getMessage());
		var poll = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{rows:[], pollIntervalMs:5000, columns:[{key:'a'}]}")));
		assertTrue(poll.getMessage().contains("pollIntervalMs"), poll.getMessage());
		assertDoesNotThrow(() -> h.toFragment(src("t", "{rows:[], dataMode:'client', columns:[{key:'a'}]}")));
	}

	@Test void pageKey_isHoistedToFragment() {
		var frag = h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], page:{size:25}}"));
		assertNotNull(frag.get("page"));
		assertFalse(((JsonMap) frag.get("table")).containsKey("page"));
	}

	@Test void bareUrlBody_isE25() {
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t", "/rest/t")));
		assertEquals("<@card id='t'> type='datatables' requires a JSON5 object body; got '/rest/t'.", ex.getMessage());
	}

	@Test void customOperatorNotAnObject_throws() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a', searchType:'text', customOperators:['x']}]}")));
		assertTrue(ex.getMessage().contains("each customOperator must be an object."), ex.getMessage());
	}

	// The main sources live here; the test CWD is the module directory (surefire).
	private static final Path MAIN =
		Path.of("src/main/java/org/apache/juneau/rest/server/view/freemarker/console");
	private static final Path CONSOLE_UI_MAIN =
		Path.of("../juneau-rest-server-console-ui/src/main/java/org/apache/juneau/rest/server/console");

	@Test void dualHat_cardSource_usesJson5ParserOnly() throws Exception {
		// The card body is parsed with the Juneau Json5Parser, never a third-party Json5 reader, and the file
		// carries no Salesforce package/SLDS markers.
		var s = Files.readString(CONSOLE_UI_MAIN.resolve("CardSource.java"));
		assertTrue(s.contains("Json5Parser"), () -> s);
		assertFalse(s.contains("Json5l"), () -> s);
		assertFalse(s.contains("com.sfdc"), () -> s);
		assertFalse(s.contains("slds-"), () -> s);
	}

	@Test void a01_detailRegionWithoutDataUrlGetsTheEndpoint() {
		var f = h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], detail:{endpoint:'/x/{id}', region:{id:'out', populate:'p'}}}"));
		var region = (Map<?,?>)((Map<?,?>)((Map<?,?>)f.get("table")).get("detail")).get("region");
		assertEquals("/x/{id}", region.get("dataUrl"));
		assertEquals("row-detail", region.get("type"));
		assertEquals("p", region.get("populate"));
	}

	@Test void a02_explicitDetailRegionDataUrlAndTypeAreKept() {
		var f = h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], detail:{endpoint:'/x/{id}', region:{id:'out', type:'custom', dataUrl:'/y/{id}'}}}"));
		var region = (Map<?,?>)((Map<?,?>)((Map<?,?>)f.get("table")).get("detail")).get("region");
		assertEquals("/y/{id}", region.get("dataUrl"));
		assertEquals("custom", region.get("type"));
	}

	@Test void a03_detailWithoutRegionStaysWithoutOne() {
		var f = h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'a'}], detail:{endpoint:'/x/{id}'}}"));
		assertFalse(((Map<?,?>)((Map<?,?>)f.get("table")).get("detail")).containsKey("region"));
	}

	// ---- E-37: quickStats ----

	private static String qs(String items) {
		return "{dataUrl:'/x', columns:[{key:'id', label:'Id'}], quickStats:{id:'s', items:" + items + "}}";
	}

	private void refused(String body, String msg) {
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t", body)));
		assertEquals("<@card id='t'> " + msg, ex.getMessage());
	}

	@Test void quickStats_itemsMissingOrEmpty_refused() {
		var m = "quickStats items must be a non-empty array.";
		refused("{dataUrl:'/x', columns:[{key:'id'}], quickStats:{id:'s'}}", m);
		refused(qs("[]"), m);
		refused(qs("'x'"), m);
		refused("{dataUrl:'/x', columns:[{key:'id'}], quickStats:'x'}", m);
	}

	@Test void quickStats_itemNotObject_refused() {
		refused(qs("[1]"), "quickStats items must be objects.");
	}

	@Test void quickStats_itemIdBlank_refused() {
		var m = "quickStats item ids must be non-blank strings.";
		refused(qs("[{label:'A', value:1}]"), m);
		refused(qs("[{id:' ', value:1}]"), m);
	}

	@Test void quickStats_duplicateId_refused() {
		refused(qs("[{id:'a', value:1}, {id:'a', value:2}]"), "quickStats item 'a': duplicate id.");
	}

	@Test void quickStats_unknownType_refused() {
		refused(qs("[{id:'a', type:'chart'}]"), "quickStats item 'a': unknown type 'chart'.");
	}

	@Test void quickStats_typeShapeMismatch_refused() {
		var seg = "segments:[{label:'x', count:1}]";
		refused(qs("[{id:'a', type:'tile', " + seg + "}]"), "quickStats item 'a': type 'tile' does not match its shape.");
		refused(qs("[{id:'a', type:'tile', max:2, value:1}]"), "quickStats item 'a': type 'tile' does not match its shape.");
		refused(qs("[{id:'a', type:'meter', label:'L', value:1}]"), "quickStats item 'a': type 'meter' does not match its shape.");
		refused(qs("[{id:'a', type:'meter', label:'L', value:1, max:2, " + seg + "}]"), "quickStats item 'a': type 'meter' does not match its shape.");
		refused(qs("[{id:'a', type:'segmented', max:2, " + seg + "}]"), "quickStats item 'a': type 'segmented' does not match its shape.");
		refused(qs("[{id:'a', type:'segmented'}]"), "quickStats item 'a': type 'segmented' does not match its shape.");
	}

	@Test void quickStats_shapeOnlySegmentsAndMax_refused() {
		refused(qs("[{id:'a', max:2, value:1, segments:[{label:'x', count:1}]}]"), "quickStats item 'a': has both segments and max; set type.");
	}

	@Test void quickStats_meterNumbers_refused() {
		var v = "quickStats item 'a': meter value must be a number >= 0.";
		var m = "quickStats item 'a': meter max must be a number > 0.";
		refused(qs("[{id:'a', type:'meter', label:'L', value:'1', max:2}]"), v);
		refused(qs("[{id:'a', type:'meter', label:'L', value:-1, max:2}]"), v);
		refused(qs("[{id:'a', type:'meter', label:'L', value:1, max:0}]"), m);
		refused(qs("[{id:'a', type:'meter', label:'L', value:1, max:-2}]"), m);
		refused(qs("[{id:'a', type:'meter', label:'L', value:1, max:'4'}]"), m);
	}

	@Test void quickStats_meterTextNotString_refused() {
		refused(qs("[{id:'a', type:'meter', label:'L', value:1, max:2, text:5}]"), "quickStats item 'a': text must be a string.");
	}

	@Test void quickStats_segmentsCountOutOfRange_refused() {
		var m = "quickStats item 'a': segments must be an array of 1 to 8 objects.";
		refused(qs("[{id:'a', type:'segmented', label:'L', segments:[]}]"), m);
		var nine = String.join(",", Collections.nCopies(9, "{label:'x', count:1}"));
		refused(qs("[{id:'a', type:'segmented', label:'L', segments:[" + nine + "]}]"), m);
		var eight = String.join(",", Collections.nCopies(8, "{label:'x', count:1}"));
		assertDoesNotThrow(() -> h.toFragment(src("t", qs("[{id:'a', type:'segmented', label:'L', segments:[" + eight + "]}]"))));
	}

	@Test void quickStats_segmentBad_refused() {
		var m = "quickStats item 'a': segment 1 must be an object with a non-blank label and a whole count >= 0.";
		for (var seg : List.of("1", "{count:1}", "{label:' ', count:1}", "{label:'x', count:-1}", "{label:'x', count:3.5}", "{label:'x', count:'3'}"))
			refused(qs("[{id:'a', type:'segmented', label:'L', segments:[" + seg + "]}]"), m);
	}

	@Test void quickStats_segmentIntegralDouble_passes() {
		assertDoesNotThrow(() -> h.toFragment(src("t", qs("[{id:'a', type:'segmented', label:'L', segments:[{label:'x', count:3.0}]}]"))));
	}

	@Test void quickStats_segmentedItemLevelKeys_refused() {
		var m = "quickStats item 'a': a segmented item takes no tone or text; tone each segment.";
		refused(qs("[{id:'a', type:'segmented', label:'L', segments:[{label:'x', count:1}], tone:'info'}]"), m);
		refused(qs("[{id:'a', type:'segmented', label:'L', segments:[{label:'x', count:1}], text:'x'}]"), m);
	}

	@Test void quickStats_offPaletteTone_refused_everyKind() {
		var m = "quickStats item 'a': tone 'accent' is not one of neutral, info, success, warning, error.";
		refused(qs("[{id:'a', value:1, tone:'accent'}]"), m);
		refused(qs("[{id:'a', type:'meter', label:'L', value:1, max:2, tone:'accent'}]"), m);
		refused(qs("[{id:'a', type:'segmented', label:'L', segments:[{label:'x', count:1, tone:'accent'}]}]"), "quickStats item 'a' segment 1: tone 'accent' is not one of neutral, info, success, warning, error.");
	}

	@Test void quickStats_nonFiniteNumbers_refused() {
		var v = "quickStats item 'a': meter value must be a number >= 0.";
		var m = "quickStats item 'a': meter max must be a number > 0.";
		refused(qs("[{id:'a', type:'meter', label:'L', value:Infinity, max:2}]"), v);
		refused(qs("[{id:'a', type:'meter', label:'L', value:NaN, max:2}]"), v);
		refused(qs("[{id:'a', type:'meter', label:'L', value:1, max:Infinity}]"), m);
		refused(qs("[{id:'a', type:'meter', label:'L', value:1, max:NaN}]"), m);
		refused(qs("[{id:'a', type:'segmented', label:'L', segments:[{label:'x', count:Infinity}]}]"),
			"quickStats item 'a': segment 1 must be an object with a non-blank label and a whole count >= 0.");
	}

	@Test void quickStats_meterAndSegmentedNeedLabel_refused() {
		for (var kind : List.of("meter", "segmented")) {
			var m = "quickStats item 'a': a " + kind + " item needs a non-blank label.";
			var rest = kind.equals("meter") ? "value:1, max:2" : "segments:[{label:'x', count:1}]";
			refused(qs("[{id:'a', type:'" + kind + "', " + rest + "}]"), m);
			refused(qs("[{id:'a', type:'" + kind + "', label:' ', " + rest + "}]"), m);
			refused(qs("[{id:'a', type:'" + kind + "', label:5, " + rest + "}]"), m);
		}
		assertDoesNotThrow(() -> h.toFragment(src("t", qs("[{id:'a', label:null, value:1}]"))));
	}

	@Test void quickStats_segmentToneNamesSegment_refused() {
		refused(qs("[{id:'a', type:'segmented', label:'L', segments:[{label:'x', count:1}, {label:'y', count:1, tone:'accent'}]}]"),
			"quickStats item 'a' segment 2: tone 'accent' is not one of neutral, info, success, warning, error.");
	}

	@Test void quickStats_validKinds_pass() {
		var items = "[{id:'t1', value:1, tone:'neutral'}, {id:'t2', value:1, tone:'info'}, {id:'t3', value:1, tone:'success'},"
			+ " {id:'t4', value:1, tone:'warning'}, {id:'t5', value:1, tone:'error'}, {id:'t6', type:'tile', label:'T', value:'n/a'},"
			+ " {id:'m1', type:'meter', label:'L', value:1, max:2, text:'x', tone:'success'}, {id:'g1', type:'segmented', label:'L', segments:[{label:'x', count:1, tone:'info'}]},"
			+ " {id:'m2', label:'L', value:1, max:2}, {id:'m0', type:'meter', label:'L', value:0, max:2}, {id:'g2', label:'L', segments:[{label:'x', count:0}]}]";
		var table = (Map<?,?>)h.toFragment(src("t", qs(items))).get("table");
		assertEquals(11, ((List<?>)((Map<?,?>)table.get("quickStats")).get("items")).size());
	}

	@Test void quickStats_contractVersion_e31Wins() {
		var ex = assertThrows(IllegalArgumentException.class, () -> h.toFragment(src("t",
			"{dataUrl:'/x', columns:[{key:'id'}], quickStats:{contractVersion:'1', items:[{id:'a', type:'chart'}]}}")));
		assertTrue(ex.getMessage().contains("'quickStats' sets contractVersion"), ex.getMessage());
	}

	@Test void quickStats_srcCard_unaffected() {
		assertTrue(h.toFragment(CardSource.create("datatables", "t").src("/x").build()).isEmpty());
	}

	@Test void quickStats_absent_unaffected() {
		assertDoesNotThrow(() -> h.toFragment(src("t", "{dataUrl:'/x', columns:[{key:'id'}]}")));
	}

	@Test void quickStats_bodyMapFromTableSpec_passes() {
		var body = TableSpec.create("t").dataUrl("/x").columns(Column.create("id"))
			.quickStats("s", QuickStat.of("a", "A", 1).statusTone(StatusTone.INFO), QuickStat.meter("m", "M", 1, 2),
				QuickStat.segmented("g", "G").segment("A", 1).segment("B", 2, StatusTone.SUCCESS))
			.toCardBody();
		assertDoesNotThrow(() -> h.toFragment(CardSource.create("datatables", "t").bodyMap(body).build()));
	}

	@Test void dualHat_newMainSources_haveNoSalesforceMarkers() throws Exception {
		var files = List.of(MAIN.resolve("PageDirectiveModel.java"), MAIN.resolve("CardDirectiveModel.java"),
			MAIN.resolve("DatatablesCardType.java"), MAIN.resolve("TerminalCardType.java"),
			MAIN.resolve("ToolkitPackRegistry.java"), MAIN.resolve("FtlAttrLists.java"));
		for (var p : files) {
			assertTrue(Files.exists(p), () -> "missing source: " + p);
			var s = Files.readString(p);
			assertFalse(s.contains("slds-"), () -> p + " contains slds-");
			assertFalse(s.contains("lightning"), () -> p + " contains lightning");
			assertFalse(s.contains("Salesforce Sans"), () -> p + " contains Salesforce Sans");
			assertFalse(s.contains("Json5l"), () -> p + " contains Json5l");
		}
	}
}
