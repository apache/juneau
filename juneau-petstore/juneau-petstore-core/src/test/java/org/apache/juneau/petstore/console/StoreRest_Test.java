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
package org.apache.juneau.petstore.console;

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.console.test.PageContractAssert.*;
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.data.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * The Store page: card order, the pending-changes badge, the {@code /stats/*} reads, the quick-stat tiles and the hours fragment.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class StoreRest_Test extends TestBase {

	/** A JSON client; the host store is shared across the fork, so tests compare against live API reads, not constants. */
	static MockRestClient json() {
		return MockRestClient.create(Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	static JsonList list(MockRestClient c, String path) throws Exception {
		return new JsonList(c.get(path).run().assertStatus(200).getContent().asString());
	}

	private static int sum(JsonList rows) {
		return StreamSupport.stream(rows.elements(JsonMap.class).spliterator(), false).mapToInt(x -> x.getInt("count")).sum();
	}

	/** The live pet counts by the given key, fetched from the API; keys with no pets are 0. */
	private static Map<String,Integer> live(MockRestClient c, String key) throws Exception {
		var m = new TreeMap<String,Integer>();
		for (var p : list(c, "/petstore/pets").elements(JsonMap.class))
			if (p.getString(key) != null) // other tests create pets without a species; the stats rows cannot count them
				m.merge(p.getString(key), 1, Integer::sum);
		return m;
	}

	/** The stats rows as key to count, with zero-count keys dropped to match {@link #live}. */
	private static Map<String,Integer> counts(JsonList rows, String key) {
		var m = new TreeMap<String,Integer>();
		for (var r : rows.elements(JsonMap.class))
			if (r.getInt("count") > 0)
				m.put(r.getString(key), r.getInt("count"));
		return m;
	}

	@Test void a01_cardOrder() throws Exception {
		assertPage(page(client(), "/console/store")).isValid()
			.hasCardOrder("welcome", "store-sw")
			.hasCard("welcome", "html")
			.hasCard("store-sw", "switcher").hasCardKey("store-sw", "/style", "slider")
			.hasCard("stats", "datatables")
			.hasCard("orders-trend", "chart")
			.hasCard("by-species", "chart")
			.hasCard("by-status", "chart")
			.hasCard("pets-map", "chart")
			.hasCard("revenue-chart", "chart")
			.hasCard("hours", "html");
		assertContainsAll(page(client(), "/console/store"), "/console/store/recent-orders", "/console/store/fragments/hours");
	}

	@Test void a02_badgeOnEveryPage() throws Exception {
		var c = client();
		for (var path : PetstorePages_ContractTest.PAGE_PATHS)
			assertContains("\"pending\"", page(c, path)); // the BadgeDef id in the contract
	}

	@Test void a03_badgeIsUnscoped() throws Exception {
		// A scope would hide the badge on every page but /console/pets, and /pending ignores 'kind'.
		var html = page(client(), "/console/about");
		var i = html.indexOf("\"id\":\"pending\"");
		assertTrue(i >= 0, html);
		var badge = html.substring(i, html.indexOf('}', i));
		assertNotContains(() -> badge, "scope", badge);
		assertNotContains(() -> badge, "stateFilter", badge);
		assertNotContains(() -> "no scope fields anywhere in the contract", "\"scope\"", html);
	}

	@Test void a03b_badgeRefreshesThePetsCardFromAnyPage() throws Exception {
		// The pets card lives on another page, so the badge opts out of the build-time card-exists check for them.
		var html = page(client(), "/console/about");
		assertContainsAll(html, "\"refreshes\":[\"pets\"]", "\"refreshesAllowMissing\":[\"pets\"]");
	}

	@Test void a04_orderIdLinksToThePetOrderDetail() throws Exception {
		assertPage(page(client(), "/console/store")).hasCardKey("stats", "/table/columns/0/href", "/console/pets/{petId}/orders/{id}");
	}

	@Test void a05_chartCardsReadTheStatsEndpoints() throws Exception {
		var html = page(client(), "/console/store");
		assertPage(html)
			.hasCardKey("orders-trend", "/kind", "line")
			.hasCardKey("orders-trend", "/src", "/console/store/stats/daily")
			.hasCardKey("orders-trend", "/x/field", "day")
			.hasCardKey("orders-trend", "/series/0/field", "placed")
			.hasCardKey("orders-trend", "/series/1/field", "delivered")
			.hasCardKey("by-species", "/kind", "donut")
			.hasCardKey("by-species", "/src", "/console/store/stats/species")
			.hasCardKey("by-species", "/label", "species")
			.hasCardKey("by-species", "/value", "count")
			.hasCardKey("by-status", "/kind", "bar")
			.hasCardKey("by-status", "/src", "/console/store/stats/status")
			.hasCardKey("by-status", "/x/field", "status")
			.hasCardKey("by-status", "/series/0/field", "count")
			.hasCardKey("by-species", "/dataTab", true)
			.hasCardKey("by-status", "/dataTab", true)
			.hasCardKey("pets-map", "/dataTab", true)
			.hasCardKey("orders-trend", "/x/label", "Day")
			.hasCardKey("orders-trend", "/series/0/label", "Placed")
			.hasCardKey("orders-trend", "/series/1/label", "Delivered")
			.hasCardKey("orders-trend", "/series/1/tone", "ok")
			.hasCardKey("by-species", "/center", "total")
			.hasCardKey("by-status", "/series/0/label", "Pets");
		var s = "/juneau-console/juneau-charts.js?v=";
		assertContains(s, html);
		assertEquals(html.indexOf(s), html.lastIndexOf(s), "the charts runtime is emitted once for five chart cards");
	}

	@Test void a06_chartFieldsExistInTheirRows() throws Exception {
		// Every field a chart card reads must be a key of its endpoint's rows, so renaming one on either side fails here.
		var cards = assertPage(page(client(), "/console/store")).contract().getList("cards");
		assertEquals(List.of("revenue-chart", "orders-trend", "by-species", "by-status", "pets-map"), assertChartFieldsInRows(json(), cards));
		var rev = list(json(), "/console/store/stats/revenue").getMap(0);
		var ord = list(json(), "/console/store/stats/daily").getMap(0);
		for (var k : List.of("day", "revenue"))
			assertTrue(rev.containsKey(k), k);
		for (var k : List.of("day", "placed", "delivered"))
			assertTrue(ord.containsKey(k), k);
	}

	@Test void a07_dataViewColumnsExistInTheirRows() throws Exception {
		// A table view's column keys must be keys of its endpoint's rows, so a rename on either side fails here.
		var cards = assertPage(page(client(), "/console/store")).contract().getList("cards");
		var checked = new ArrayList<String>();
		for (var top : cards.elements(JsonMap.class)) {
			if (! "switcher".equals(top.getString("type")))
				continue;
			var nested = new ArrayList<JsonMap>();
			for (var v : top.getList("views").elements(JsonMap.class)) {
				if (v.containsKey("card"))
					nested.add(v.getMap("card"));
				else
					for (var sec : v.getList("sections").elements(JsonMap.class))
						nested.add(sec.getMap("card"));
			}
			for (var card : nested) {
				if (! "datatables".equals(card.getString("type")))
					continue;
				var t = card.getMap("table");
				var row = list(json(), t.getString("dataUrl")).getMap(0);
				for (var col : t.getList("columns").elements(JsonMap.class))
					assertTrue(row.containsKey(col.getString("key")), card.getString("id") + " column " + col.getString("key"));
				checked.add(card.getString("id"));
			}
		}
		assertEquals(List.of("stats", "revenue-data", "orders-data"), checked);
	}

	/**
	 * Asserts that row 0 of each chart card's {@code src} has every field the card reads.
	 *
	 * @return The chart card ids checked, in page order.
	 */
	static List<String> assertChartFieldsInRows(MockRestClient c, JsonList cards) throws Exception {
		var checked = new ArrayList<String>();
		var flat = new ArrayList<JsonMap>();
		for (var top : cards.elements(JsonMap.class)) {
			if (! "switcher".equals(top.getString("type"))) {
				flat.add(top);
				continue;
			}
			for (var v : top.getList("views").elements(JsonMap.class)) {
				if (v.containsKey("card"))
					flat.add(v.getMap("card"));
				else
					for (var sec : v.getList("sections").elements(JsonMap.class))
						flat.add(sec.getMap("card"));
			}
		}
		for (var o : flat) {
			if (! "chart".equals(o.getString("type")))
				continue;
			var id = o.getString("id");
			var fields = new ArrayList<String>();
			if (o.containsKey("x"))
				fields.add(o.getMap("x").getString("field"));
			if (o.containsKey("series"))
				for (var x : o.getList("series").elements(JsonMap.class))
					fields.add(x.getString("field"));
			for (var k : List.of("label", "value"))
				if (o.containsKey(k))
					fields.add(o.getString(k));
			if (o.containsKey("helper") && "treemap".equals(o.getMap("helper").getString("kind"))) {
				var h = o.getMap("helper");
				fields.add(h.getString("label"));
				for (var g : h.getList("groupBy"))
					fields.add(String.valueOf(g));
				for (var g : h.getList("tileFields"))
					fields.add(String.valueOf(g));
				if (h.containsKey("color"))
					fields.add(h.getMap("color").getString("field"));
			}
			assertFalse(fields.isEmpty(), () -> id + " names no fields");
			var row = list(c, o.getString("src")).getMap(0);
			for (var f : fields)
				assertTrue(row.containsKey(f), () -> id + " reads '" + f + "' but " + o.getString("src") + " rows have " + row.keySet());
			checked.add(id);
		}
		return checked;
	}

	/** {@code assertBean} renders a null property as {@code <null>}; other tests create pets without a species. */
	private static String nullToMarker(String s) {
		return s == null ? "<null>" : s;
	}

	@Test void c01_statsPets_returnsIdNameSpeciesStatus_forEveryPet() throws Exception {
		var c = json();
		var rows = list(c, "/console/store/stats/pets");
		var pets = list(c, "/petstore/pets");
		assertSize(pets.size(), rows);
		assertTrue(pets.size() > 0);
		var byId = new HashMap<Long,JsonMap>();
		for (var r : rows.elements(JsonMap.class))
			byId.put(r.getLong("id"), r);
		assertEquals(pets.size(), byId.size());
		for (var p : pets.elements(JsonMap.class)) {
			var r = byId.get(p.getLong("id"));
			assertNotNull(r, () -> "no row for pet " + p.getLong("id"));
			assertEquals(new TreeSet<>(List.of("id", "name", "species", "status")), new TreeSet<>(r.keySet()));
			assertBean(r, "name,species,status", p.getString("name") + "," + nullToMarker(p.getString("species")) + "," + nullToMarker(p.getString("status")));
		}
	}

	@Test void c02_petsMap_isTreemap_onClickActionIsPetEditDialog() throws Exception {
		assertPage(page(client(), "/console/store"))
			.hasCard("pets-map", "chart")
			.hasCardKey("pets-map", "/helper/kind", "treemap")
			.hasCardKey("pets-map", "/src", "/console/store/stats/pets")
			.hasCardKey("pets-map", "/helper/groupBy/0", "species")
			.hasCardKey("pets-map", "/helper/groupBy/1", "status")
			.hasCardKey("pets-map", "/helper/label", "name")
			.hasCardKey("pets-map", "/helper/tileFields/0", "id")
			.hasCardKey("pets-map", "/on/click", "pets-map.click")
			.hasCardKey("pets-map", "/onClick/action/id", "edit")
			.hasCardKey("pets-map", "/onClick/action/present", "dialog")
			.hasCardKey("pets-map", "/onClick/action/form", "/console/pets/form?id={id}")
			.hasCardKey("pets-map", "/onClick/action/endpoint", "/console/pets/{id}")
			.hasCardKey("pets-map", "/onClick/action/method", "PUT")
			.hasCardKey("pets-map", "/onClick/action/onSuccess", "redraw");
	}

	@Test void c02b_ordersTrend_clickLandsOnTheOrdersPageForThatDay() throws Exception {
		for (var path : List.of("/console/store", "/console/store?builder"))
			assertPage(page(client(), path))
				.hasCard("orders-trend", "chart")
				.hasCardKey("orders-trend", "/onClick/href", "/console/orders?state=day({day})");
	}

	@Test void c03_petsMap_builderTwin_redrawsOnSuccess_whilePetsTableMergesRow() throws Exception {
		assertPage(page(client(), "/console/store?builder"))
			.hasCardKey("pets-map", "/onClick/action/onSuccess", "redraw")
			.hasCardKey("pets-map", "/onClick/action/present", "dialog");
		assertPage(page(client(), "/console/pets?builder"))
			.hasCardKey("pets", "/table/rowActions/0/onSuccess", "mergeRow");
	}

	@Test void b01_dailyStatsCoverThirtyDays() throws Exception {
		var rows = list(json(), "/console/store/stats/daily");
		assertSize(30, rows);
		assertBean(rows.getMap(29), "day", "2026-10-01"); // DEFAULT_CLOCK day, oldest first
		assertBean(rows.getMap(0), "day", "2026-09-02");
		assertTrue(rows.getMap(0).containsKey("placed") && rows.getMap(0).containsKey("delivered"));
	}

	@Test void b02_bySpeciesSumsToPetCount() throws Exception {
		var c = json();
		var rows = list(c, "/console/store/stats/species");
		assertEquals(Species.values().length, rows.size());
		assertBean(rows.getMap(0), "species", Species.values()[0].name());
		assertEquals(live(c, "species").values().stream().mapToInt(Integer::intValue).sum(), sum(rows));
		assertEquals(live(c, "species"), counts(rows, "species"));
	}

	@Test void b03_byStatusSumsToPetCount() throws Exception {
		var c = json();
		var rows = list(c, "/console/store/stats/status");
		assertEquals(PetStatus.values().length, rows.size());
		assertEquals(live(c, "status"), counts(rows, "status"));
		assertEquals(list(c, "/petstore/pets").size(), sum(rows));
	}

	private static List<?> stripItems(String path) throws Exception {
		var card = assertPage(page(client(), path)).card("stats");
		return (List<?>)((Map<?,?>)((Map<?,?>)card.get("table")).get("quickStats")).get("items");
	}

	private static long withStatus(JsonList rows, String status) {
		return StreamSupport.stream(rows.elements(JsonMap.class).spliterator(), false).filter(x -> status.equals(x.getString("status"))).count();
	}

	@Test void b04_quickStatsMatchTheStore() throws Exception {
		var c = json();
		var html = page(client(), "/console/store");
		var sold = StreamSupport.stream(list(c, "/petstore/pets").elements(JsonMap.class).spliterator(), false).filter(x -> "SOLD".equals(x.getString("status"))).count();
		assertContainsAll(html, "\"id\":\"sold\"", "\"value\":" + sold, "\"id\":\"available\"");
		var pets = list(c, "/petstore/pets");
		var total = pets.size();
		var available = withStatus(pets, "AVAILABLE");
		var orders = list(c, "/petstore/orders");
		var placed = withStatus(orders, "PLACED");
		var approved = withStatus(orders, "APPROVED");
		var delivered = withStatus(orders, "DELIVERED");
		assertTrue(total > 0);
		for (var h : List.of(html, page(client(), "/console/store?builder"))) // the FTL page and its builder twin
			assertContainsAll(h, "\"id\":\"orders\",\"type\":\"segmented\"",
				"\"label\":\"Placed\",\"count\":" + placed + "}",
				"\"label\":\"Approved\",\"count\":" + approved + ",\"tone\":\"info\"",
				"\"label\":\"Delivered\",\"count\":" + delivered + ",\"tone\":\"success\"",
				"\"id\":\"inventory\",\"type\":\"meter\"",
				"\"value\":" + available + ",\"max\":" + total,
				"\"text\":\"" + available + " of " + total + "\"");
		for (var pg : List.of("/console/store", "/console/store?builder")) {
			var segs = (List<?>)((Map<?,?>)stripItems(pg).get(3)).get("segments");
			var got = new ArrayList<String>();
			for (var o : segs) {
				var m = (Map<?,?>)o;
				got.add(m.get("label") + "=" + m.get("count") + "/" + m.get("tone"));
			}
			assertEquals(List.of("Placed=" + placed + "/null", "Approved=" + approved + "/info", "Delivered=" + delivered + "/success"), got, pg);
		}
		var rows = list(c, "/console/store/recent-orders");
		assertSize(10, rows);
		assertTrue(rows.getMap(0).getString("shipDate").compareTo(rows.getMap(9).getString("shipDate")) >= 0);
	}

	@Test void b05_hoursFragmentIsMarkupOnly() throws Exception {
		var html = client().get("/console/store/fragments/hours").run().assertStatus(200).getContent().asString();
		assertContains("jc-prose", html);
		assertNotContains(() -> html, "<html", html);
		assertNotContains(() -> html, "juneau-page", html);
	}

	@Test void b06_revenueToday() throws Exception {
		// Deliver one priced pet today so a tile stuck at zero cannot pass.
		var store = store();
		var pet = store.getPets().stream().filter(x -> x.getPrice() > 0).findFirst().orElseThrow();
		var today = LocalDate.ofInstant(store.clock().instant(), ZoneOffset.UTC).toString();
		var pinned = store.createOrder(new org.apache.juneau.petstore.dto.Order().setPetId(pet.getId()).setStatus(OrderStatus.DELIVERED).setShipDate(today), "test");
		try {
			var c = json();
			var daily = list(c, "/console/store/stats/daily");
			assertEquals(today, daily.getMap(daily.size() - 1).getString("day"));
			var prices = new HashMap<Long,Double>();
			for (var p : list(c, "/petstore/pets").elements(JsonMap.class))
				prices.put(p.getLong("id"), p.get("price", Double.class));
			var expected = 0d;
			var delivered = 0;
			for (var o : list(c, "/petstore/orders").elements(JsonMap.class)) {
				if ("DELIVERED".equals(o.getString("status")) && today.equals(o.getString("shipDate"))) {
					expected += prices.getOrDefault(o.getLong("petId"), 0d);
					delivered++;
				}
			}
			assertTrue(expected >= pet.getPrice() - 0.001, () -> "pinned order missing from the expected total");
			assertTrue(expected > 0);
			assertEquals(daily.getMap(daily.size() - 1).getInt("delivered"), delivered);
			var kpi = new JsonMap(c.get("/console/store/kpi/revenue").run().assertStatus(200).getContent().asString());
			assertBean(kpi, "label,unit", "Revenue today,USD");
			assertEquals(Math.round(expected * 100) / 100d, kpi.get("value", Double.class), 0.001);
		} finally {
			store.deleteOrder(pinned.getId(), "test");
		}
	}

	/** A host whose store is seeded against a later fixed clock, as the running apps seed theirs against the current time. */
	@Rest(children=PetstoreConsoleResource.class)
	public static class LaterHost extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
		static final Clock CLOCK = Clock.fixed(Instant.parse("2027-03-01T12:00:00Z"), ZoneOffset.UTC);
		static final PetStore STORE = PetstoreSeed.create().clock(CLOCK).populate(new PetStore(CLOCK));

		/** @return The later-clock store. */
		@Bean public PetStore petStore() {
			return STORE;
		}
	}

	@Test void d02_revenueSeriesOnLaterClockIsNonZero() throws Exception {
		var c = MockRestClient.create(LaterHost.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
		var rows = list(c, "/console/store/stats/revenue");
		assertSize(30, rows);
		assertEquals("2027-03-01", rows.getMap(29).getString("day"));
		var total = StreamSupport.stream(rows.elements(JsonMap.class).spliterator(), false).mapToDouble(r -> r.get("revenue", Double.class)).sum();
		assertTrue(total > 0, "revenue series was all zero");
	}

	@Test void d01_revenueFeed() throws Exception {
		// Deliver one priced pet today and one a few days back, plus a placed order that must not count.
		var store = store();
		var pet = store.getPets().stream().filter(x -> x.getPrice() > 0).findFirst().orElseThrow();
		var today = LocalDate.ofInstant(store.clock().instant(), ZoneOffset.UTC);
		var earlier = today.minusDays(3).toString();
		var made = new ArrayList<Long>();
		made.add(store.createOrder(new org.apache.juneau.petstore.dto.Order().setPetId(pet.getId()).setStatus(OrderStatus.DELIVERED).setShipDate(today.toString()), "test").getId());
		made.add(store.createOrder(new org.apache.juneau.petstore.dto.Order().setPetId(pet.getId()).setStatus(OrderStatus.DELIVERED).setShipDate(earlier), "test").getId());
		made.add(store.createOrder(new org.apache.juneau.petstore.dto.Order().setPetId(pet.getId()).setStatus(OrderStatus.PLACED).setShipDate(earlier), "test").getId());
		try {
			var c = json();
			var rows = list(c, "/console/store/stats/revenue");
			assertSize(30, rows);
			assertEquals(today.toString(), rows.getMap(29).getString("day"));
			assertEquals(today.minusDays(29).toString(), rows.getMap(0).getString("day"));
			var prices = new HashMap<Long,Double>();
			for (var p : list(c, "/petstore/pets").elements(JsonMap.class))
				prices.put(p.getLong("id"), p.get("price", Double.class));
			var expected = new HashMap<String,Double>();
			for (var o : list(c, "/petstore/orders").elements(JsonMap.class))
				if ("DELIVERED".equals(o.getString("status")) && prices.containsKey(o.getLong("petId")))
					expected.merge(o.getString("shipDate"), prices.get(o.getLong("petId")), Double::sum);
			for (var r : rows.elements(JsonMap.class)) {
				assertEquals(new TreeSet<>(List.of("day", "revenue")), new TreeSet<>(r.keySet()));
				assertEquals(Math.round(expected.getOrDefault(r.getString("day"), 0d) * 100) / 100d, r.get("revenue", Double.class), 0.001, r.getString("day"));
			}
			assertTrue(rows.getMap(29).get("revenue", Double.class) >= pet.getPrice() - 0.001);
			assertTrue(rows.getMap(26).get("revenue", Double.class) >= pet.getPrice() - 0.001);
		} finally {
			for (var id : made)
				store.deleteOrder(id, "test");
		}
	}
}
