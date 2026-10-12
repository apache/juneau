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
import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * The Store page: card order, the pending-changes badge, the {@code /stats/*} reads, the quick-stat tiles and the hours fragment.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class StoreRest_Test extends TestBase {

	/** A JSON client; the host store is shared across the fork, so tests compare against live API reads, not constants. */
	private static MockRestClient json() {
		return MockRestClient.create(Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	private static JsonList list(MockRestClient c, String path) throws Exception {
		return new JsonList(c.get(path).run().assertStatus(200).getContent().asString());
	}

	private static int sum(JsonList rows) {
		return StreamSupport.stream(rows.elements(JsonMap.class).spliterator(), false).mapToInt(x -> x.getInt("count")).sum();
	}

	/** The live pet counts by the given key, fetched from the API; keys with no pets are 0. */
	private static Map<String,Integer> live(MockRestClient c, String key) throws Exception {
		var m = new TreeMap<String,Integer>();
		for (var p : list(c, "/petstore/pets").elements(JsonMap.class))
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
		// Blocked: ChartDef -- orders-trend, by-species and by-status join this list once the chart type lands.
		assertPage(page(client(), "/console/store")).isValid()
			.hasCardOrder("welcome", "stats", "hours")
			.hasCard("welcome", "html")
			.hasCard("stats", "datatables")
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
		assertEquals(list(c, "/petstore/pets").size(), sum(rows));
		assertEquals(live(c, "species"), counts(rows, "species"));
	}

	@Test void b03_byStatusSumsToPetCount() throws Exception {
		var c = json();
		var rows = list(c, "/console/store/stats/status");
		assertEquals(PetStatus.values().length, rows.size());
		assertEquals(live(c, "status"), counts(rows, "status"));
		assertEquals(list(c, "/petstore/pets").size(), sum(rows));
	}

	@Test void b04_quickStatsMatchTheStore() throws Exception {
		var c = json();
		var html = page(client(), "/console/store");
		var sold = StreamSupport.stream(list(c, "/petstore/pets").elements(JsonMap.class).spliterator(), false).filter(x -> "SOLD".equals(x.getString("status"))).count();
		assertContainsAll(html, "\"id\":\"sold\"", "\"value\":" + sold, "\"id\":\"available\"", "\"id\":\"orders\"", "\"id\":\"inventory\"");
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
}
