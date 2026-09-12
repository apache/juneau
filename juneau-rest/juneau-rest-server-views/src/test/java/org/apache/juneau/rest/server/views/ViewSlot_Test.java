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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.views.ViewDef.Dir;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Golden-fixture and envelope-behavior tests for {@link ViewSlot} / {@code SLOT_META}.
 */
@SuppressWarnings({
	"resource" // Closeable MockRestClient fixtures held for the class lifetime.
})
class ViewSlot_Test extends TestBase {

	public static class Release {
		public String name;
		public String status;
		public String date;
	}

	private static ViewDef releasesView() {
		return ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.defaultOrder("date", Dir.DESC)
			.columns(
				Column.of("name").title("Name").render("linked").href("servlet:/releases/{id}"),
				Column.of("status").title("Status").render("tag:status"),
				Column.of("date").title("Date").render("date"))
			.ribbon(
				RibbonAction.export("copy", "csv").optional("excel", "pdf"),
				RibbonAction.columnSearchToggle(),
				RibbonAction.option("show-superseded").title("Show superseded").column("status").value("superseded").persist(true),
				RibbonAction.refresh())
			.rowClassRule("deleted", Op.EQ, true, "row-deleted")
			.rowClassRule("error", Op.PRESENT, "row-flagged")
			.build();
	}

	private static ViewDef fvView() {
		return ViewDef.create("releases")
			.columns(Column.of("count").title("Failures ($FV{failedCount})"))
			.serverValues(ServerValues.create().value("failedCount", s -> "7"))
			.build();
	}

	private static ViewDef lView() {
		return ViewDef.create("releases")
			.columns(
				Column.of("name").titleKey("col.name"),
				Column.of("status").title("$L{col.status}"),
				Column.of("plain").title("Plain"))
			.build();
	}

	@Rest(mixins=ViewsMixin.class)
	public static class SlotHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public org.apache.juneau.commons.svl.VarResolver varResolver(org.apache.juneau.commons.svl.VarResolver.Builder b) {
			return b.vars(ServerValuesVar.class).build();
		}

		@RestGet(path="/releases")
		public ViewSlot releases(RestRequest req) {
			return ViewSlot.envelope(req, releasesView());
		}

		@RestGet(path="/fv")
		public ViewSlot fv(RestRequest req) {
			return ViewSlot.envelope(req, fvView());
		}

		@RestGet(path="/l")
		public ViewSlot l(RestRequest req) {
			return ViewSlot.envelope(req, lView());
		}

		@RestGet(path="/saved")
		public ViewSlot saved(RestRequest req) {
			return ViewSlot.envelope(req, ViewDef.create("releases")
				.columns(Column.of("name").title("Name"))
				.columnConfig(ColumnConfig.create())
				.build());
		}

		@RestGet(path="/bulk")
		public ViewSlot bulk(RestRequest req) {
			var sel = SelectionDef.create("id");
			var bulk = BulkMutateDef.create(WritePermit.forCapability("x:bulk"), sel)
				.actions(RowAction.create("ack").label("Ack").endpoint("servlet:/x/ack").method(RowAction.Method.POST));
			return ViewSlot.envelope(req, ViewDef.create("x").columns(Column.of("id").title("Id")).build(), bulk);
		}

		@RestGet(path="/mismatch")
		public ViewSlot mismatch(RestRequest req) {
			var selA = SelectionDef.create("id");
			var selB = SelectionDef.create("id");
			var bulk = BulkMutateDef.create(WritePermit.forCapability("x:bulk"), selA)
				.actions(RowAction.create("ack").label("Ack").endpoint("/ack").method(RowAction.Method.POST));
			return ViewSlot.envelope(req, ViewDef.create("x").columns(Column.of("id").title("Id")).build(), selB, bulk);
		}
	}

	private static final MockRestClient CLIENT = MockRestClient.buildLax(SlotHost.class);

	private static Map<?,?> jsonMap(String path) throws Exception {
		var body = CLIENT.get(path).json().run().assertStatus(200).getContent().asString();
		return Json.to(body, Map.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Request-free golden: view subtree matches VIEW_META including servlet:
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_requestFreeView_matchesViewMetaFixture() {
		var slot = ViewSlot.envelope(releasesView());
		var json = Json.of(slot);
		var actual = Json.to(json, Map.class);
		assertEquals(List.of("contractVersion", "view", "layout"), new ArrayList<>(actual.keySet()), json);
		assertEquals(ViewSlot.CONTRACT_VERSION, actual.get("contractVersion"));
		assertEquals(ViewTable.LAYOUT_WIDE, actual.get("layout"));
		var expectedView = Json.to(Json.of(releasesView()), Map.class);
		assertEquals(expectedView, actual.get("view"), json);
		assertTrue(json.contains("servlet:/releases/data"), json);
		assertFalse(json.contains("csrf"), json.toLowerCase(Locale.ROOT));
		assertFalse(json.contains("slds-"), json);
		assertFalse(json.toLowerCase(Locale.ROOT).contains("salesforce"), json);
	}

	@Test void a02_requestFree_doesNotResolveFv() {
		var slot = ViewSlot.envelope(fvView());
		var json = Json.of(slot);
		assertTrue(json.contains("$FV{failedCount}"), json);
		assertFalse(json.contains("Failures (7)"), json);
	}

	@Test void a03_requestFreeMessages_resolvesLSameAsViewTable() {
		var messages = Messages.of(LocalizationChromeResolution_Test.LocalizationChromeResolutionHost.class);
		var slot = ViewSlot.envelope(messages, lView());
		var html = Html.of(ViewTable.of(messages, lView()));
		@SuppressWarnings("unchecked")
		var view = (Map<String,Object>) slot.view;
		@SuppressWarnings("unchecked")
		var cols = (List<Map<String,Object>>) view.get("columns");
		assertEquals("Name", cols.get(0).get("title"));
		assertEquals("Status", cols.get(1).get("title"));
		assertEquals("Plain", cols.get(2).get("title"));
		assertTrue(html.contains("<th>Name</th>"), html);
		assertTrue(html.contains("<th>Status</th>"), html);
		assertFalse(Json.of(slot).contains("$L{col.name}"), Json.of(slot));
	}

	@Test void a04_javadocDistinguishesPaintViewSlotAndDetailSlotClass() throws Exception {
		var rel = Path.of("src/main/java/org/apache/juneau/rest/server/views/ViewSlot.java");
		var fromRepo = Path.of("juneau-rest/juneau-rest-server-views").resolve(rel);
		var src = Files.readString(Files.exists(rel) ? rel : fromRepo, UTF_8);
		assertTrue(src.contains("paintViewSlot"), src);
		assertFalse(src.contains("DETAIL_SLOT class"), src);
		assertTrue(src.contains("BarSlotTable#DETAIL_SLOT_CLASS") || src.contains("DETAIL_SLOT_CLASS"), src);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Request-bearing: servlet: resolved, $FV/$L match ViewTable
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_requestBearing_resolvesServletUrls() throws Exception {
		var map = jsonMap("/releases");
		@SuppressWarnings("unchecked")
		var view = (Map<String,Object>) map.get("view");
		var dataUrl = String.valueOf(view.get("dataUrl"));
		assertFalse(dataUrl.startsWith("servlet:"), dataUrl);
		assertTrue(dataUrl.contains("/releases/data"), dataUrl);
		@SuppressWarnings("unchecked")
		var cols = (List<Map<String,Object>>) view.get("columns");
		var href = String.valueOf(cols.get(0).get("href"));
		assertFalse(href.startsWith("servlet:"), href);
		assertTrue(href.contains("/releases/{id}"), href);
	}

	@Test void b02_requestBearingFv_matchesViewTableTitles() throws Exception {
		var map = jsonMap("/fv");
		@SuppressWarnings("unchecked")
		var view = (Map<String,Object>) map.get("view");
		@SuppressWarnings("unchecked")
		var cols = (List<Map<String,Object>>) view.get("columns");
		assertEquals("Failures (7)", cols.get(0).get("title"));
		assertFalse(String.valueOf(cols.get(0).get("title")).contains("$FV"), String.valueOf(cols.get(0).get("title")));
	}

	@Test void b03_requestBearingL_matchesViewTableTitles() throws Exception {
		var map = jsonMap("/l");
		@SuppressWarnings("unchecked")
		var view = (Map<String,Object>) map.get("view");
		@SuppressWarnings("unchecked")
		var cols = (List<Map<String,Object>>) view.get("columns");
		assertEquals("Name", cols.get(0).get("title"));
		assertEquals("Status", cols.get(1).get("title"));
	}

	@Test void b04_savedViewsBase_presentWhenColumnConfigSet() throws Exception {
		var map = jsonMap("/saved");
		assertTrue(map.containsKey("savedViewsBase"), map::toString);
		assertNotNull(map.get("savedViewsBase"));
		assertFalse(String.valueOf(map.get("savedViewsBase")).isBlank());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Selection / bulk / rows
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_mismatchedSelection_throws() throws Exception {
		CLIENT.get("/mismatch").run().assertStatus(500);
	}

	@Test void c02_bulkWire_omitsPermit() throws Exception {
		var map = jsonMap("/bulk");
		assertTrue(map.containsKey("selection"), map::toString);
		assertTrue(map.containsKey("bulk"), map::toString);
		var bulkJson = Json.of(map.get("bulk"));
		assertFalse(bulkJson.contains("permit"), bulkJson);
		assertFalse(bulkJson.contains("WritePermit"), bulkJson);
		assertTrue(bulkJson.contains("\"contractVersion\":\"1\""), bulkJson);
		assertTrue(bulkJson.contains("\"actions\""), bulkJson);
		@SuppressWarnings("unchecked")
		var actions = (List<Map<String,Object>>) ((Map<String,Object>) map.get("bulk")).get("actions");
		var endpoint = String.valueOf(actions.get(0).get("endpoint"));
		assertFalse(endpoint.startsWith("servlet:"), endpoint);
	}

	@Test void c03_rowsPlusDataUrl_throws() {
		var slot = ViewSlot.envelope(releasesView());
		var e = assertThrows(IllegalArgumentException.class, () -> slot.rows(List.of(Map.of("name", "a"))));
		assertTrue(e.getMessage().contains("dataUrl"), e::getMessage);
	}

	@Test void c04_rowsWithoutDataUrl_accepted() {
		var view = ViewDef.create("x").columns(Column.of("name").title("Name")).build();
		var slot = ViewSlot.envelope(view).rows(List.of(Map.of("name", "a")));
		assertNotNull(slot.rows);
		assertEquals(1, slot.rows.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) DETAIL_SLOT
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_jsonOfRowDetailDef_isNotTheWire() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.title("Incident #{number}")
			.region(RegionDef.create("pd-mine-detail").allowPopulators("pagerduty-detail").populate("pagerduty-detail"));
		var view = ViewDef.create("pd").columns(Column.of("id").title("Id")).details(d).build();
		var slot = ViewSlot.envelope(view);
		var wire = Json.of(slot.detail);
		var raw = Json.of(d);
		assertNotEquals(raw, wire);
		assertFalse(wire.contains("serverValues"), wire);
		assertFalse(wire.contains("\"lock\""), wire);
		assertTrue(wire.contains("\"type\":\"row-detail\""), wire);
		assertTrue(wire.contains("\"dataUrl\":\"/data/{id}\""), wire);
		assertEquals("/data/{id}", slot.detail.region.dataUrl);
		assertEquals(RegionDef.TYPE_ROW_DETAIL, slot.detail.region.type);
		assertEquals("pagerduty-detail", slot.detail.region.populate);
	}

	@Test void d02_explicitRegionDataUrl_isKept() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.region(RegionDef.create("r").allowPopulators("p").populate("p").dataUrl("/other/{id}"));
		var view = ViewDef.create("x").columns(Column.of("id").title("Id")).details(d).build();
		var slot = ViewSlot.envelope(view);
		assertEquals("/other/{id}", slot.detail.region.dataUrl);
	}

	@Test void d03_countNull_isOmitted() {
		var d = RowDetailDef.create()
			.endpoint("/q/{id}")
			.region(RegionDef.create("d").allowPopulators("p").populate("p"));
		var view = ViewDef.create("q").columns(Column.of("id").title("Id")).details(d).build();
		var json = Json.of(ViewSlot.envelope(view).detail);
		assertFalse(json.contains("\"count\""), json);
		assertFalse(json.contains("null"), json);
	}

	@Test void d04_titleFieldsRideTheRegionWire() {
		var d = RowDetailDef.create()
			.endpoint("/q/{id}")
			.region(RegionDef.create("d").allowPopulators("p").populate("p").titleFields("body"));
		var view = ViewDef.create("q").columns(Column.of("id").title("Id")).details(d).build();
		var json = Json.of(ViewSlot.envelope(view).detail);
		assertTrue(json.contains("\"titleFields\""), json);
		assertTrue(json.contains("body"), json);
	}

	@Test void d05_noHeaderActionsOnWire() {
		var d = RowDetailDef.create()
			.endpoint("/d/{id}")
			.title("T")
			.region(RegionDef.create("d").allowPopulators("p").populate("p"));
		var view = ViewDef.create("q").columns(Column.of("id").title("Id")).details(d).build();
		var json = Json.of(ViewSlot.envelope(view).detail);
		assertFalse(json.contains("headerActions"), json);
		assertTrue(json.contains("\"title\":\"T\""), json);
	}
}
