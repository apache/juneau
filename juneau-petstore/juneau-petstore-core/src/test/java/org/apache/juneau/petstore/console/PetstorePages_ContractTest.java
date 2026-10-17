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

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.test.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Every console page renders a valid {@code #juneau-page} contract with the expected nav, cards and order.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
public class PetstorePages_ContractTest extends TestBase {

	record Page(String id, String path, Consumer<PageContractAssert> expect) {
		@Override public String toString() { return id; }
	}

	static final List<Page> PAGES = List.of(
		new Page("P1", "/console/store", p -> p.hasActiveNav("store")
			.hasCardOrder("welcome", "stats", "revenue", "orders-trend", "by-species", "by-status", "pets-map", "hours")
			.hasCard("welcome", "html").hasCard("stats", "datatables").hasCard("revenue", "kpi").hasCardKey("stats", "/table/quickStats/id", "store")
			.hasCardKey("stats", "/table/quickStats/items/3/type", "segmented").hasCardKey("stats", "/table/quickStats/items/4/type", "meter")
			.hasCard("orders-trend", "chart").hasCard("by-species", "chart").hasCard("by-status", "chart")
			.hasCard("pets-map", "chart")
			.hasCard("hours", "html")),
		new Page("jobs", "/console/ops/jobs", p -> p.hasActiveNav("ops", "jobs")
			.hasCardOrder("restock-help", "restock", "jobs", "species", "groom-output", "groom-file", "groom-runs")
			.hasCard("restock", "job-log").hasCard("jobs", "datatables").hasCard("species", "datatables")
			.hasCard("groom-output", "terminal").hasCard("groom-file", "terminal").hasCard("groom-runs", "datatables")),
		new Page("audit", "/console/ops/audit", p -> p.hasActiveNav("ops", "audit").hasCard("audit", "datatables")),
		new Page("flavors-html", "/console/dev/flavors/html", p -> p.hasActiveNav("dev", "flavors", "html").hasCardOrder("caption", "flavor")),
		new Page("flavors-freemarker", "/console/dev/flavors/freemarker", p -> p.hasActiveNav("dev", "flavors", "freemarker").hasCardOrder("caption", "flavor")),
		new Page("flavors-mustache", "/console/dev/flavors/mustache", p -> p.hasActiveNav("dev", "flavors", "mustache").hasCardOrder("caption", "flavor")),
		new Page("flavors-react", "/console/dev/flavors/react", p -> p.hasActiveNav("dev", "flavors", "react").hasCardOrder("caption", "flavor")),
		new Page("secure", "/console/dev/secure", p -> p.hasActiveNav("dev", "secure").hasCardOrder("about", "try")),
		new Page("custom-cards", "/console/dev/cards", p -> p.hasActiveNav("dev", "cards").hasCardOrder("available", "gauge", "bands", "prices", "toolbar", "how")
			.hasCard("available", "kpi").hasCard("gauge", "inventory-gauge")),
		new Page("about", "/console/about", p -> p.hasActiveNav("about").hasCardOrder("overview", "links", "beans")),
		new Page("themes", "/console/dev/themes", p -> p.hasActiveNav("dev", "themes").hasCardOrder("swatches", "picker", "authoring").hasTheme("open")),
		new Page("P2", "/console/pets", p -> p.hasActiveNav("pets")
			.hasCard("pets", "datatables")
			.hasCardKey("pets", "/table/dataMode", "server")
			.hasCardKey("pets", "/table/selection/scope", "persistent")
			.hasCardKey("pets", "/table/columnConfig", true)
			.hasCardKey("pets", "/table/urlState/ids", Map.of())),
		new Page("P3", "/console/pets/sold", p -> p.hasNoActiveNav().hasNavPath("pets", "sold")
			.hasCard("sold", "datatables")),
		new Page("P10", "/console/pets/changes", p -> p.hasActiveNav("pets", "changes")
			.hasCard("changes", "datatables")
			.hasCardKey("changes", "/table/dataMode", "server")
			.hasCardKey("changes", "/table/selection/scope", "persistent")
			.hasCardKey("changes", "/table/selection/selectableWhen/0/value", "PENDING")
			.hasCardKey("changes", "/table/bulk/actions/0/bulkMode", "aggregate")),
		new Page("P4", "/console/pets/1/summary", p -> p.hasActiveNav("pets", "p1", "summary")
			.hasCard("pet", "datatables")
			.hasCardKey("pet", "/table/rowActions/0/endpoint", "/console/pets/{id}/stage")),
		new Page("P4p", "/console/pets/1/photo", p -> p.hasActiveNav("pets", "p1", "photo").hasCardOrder("photo")),
		new Page("P4o", "/console/pets/1/orders", p -> p.hasActiveNav("pets", "p1", "orders")
			.hasCard("pet-orders", "datatables")
			.hasCardKey("pet-orders", "/table/detail/endpoint", "/console/pets/1/orders/{id}/detail")),
		new Page("P4h", "/console/pets/1/history", p -> p.hasActiveNav("pets", "p1", "history").hasCard("history", "datatables")),
		new Page("P5", "/console/orders", p -> p.hasActiveNav("orders").hasCard("orders", "datatables").hasCardKey("orders", "/table/dataMode", "server")
			.hasCardKey("orders", "/table/rowActions/3/confirm", "Delete order '{id}'?")
			.hasCardKey("orders", "/table/urlState/day", Map.of())
			.hasCardKey("orders", "/table/urlState/window", Map.of())
			.hasCardKey("orders", "/table/columnConfig", Map.of("tabs", List.of("columns", "sort")))),
		new Page("P11", "/console/users", p -> p.hasActiveNav("users").hasCard("users", "datatables").hasCardKey("users", "/table/dataMode", "server"))
		// Later tasks append rows here.
	);

	/**
	 * Builder-only developer pages: no FTL twin and no nav node of their own, so they are not in {@link #PAGES}.
	 * Their own tests ({@code ChartsRest_Test}, {@code DataRest_Test}, {@code DashboardRest_Test}) assert the contract.
	 */
	public static final List<String> BUILDER_ONLY = List.of("/console/dev/charts", "/console/dev/data", "/console/dev/dashboard");

	/** Page paths for the vocabulary, asset-order and twin gates (the browser twin test reads it too). */
	public static final List<String> PAGE_PATHS = PAGES.stream().map(Page::path).toList();

	static Stream<Page> pages() {
		return PAGES.stream();
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("pages")
	void a01_ftl(Page p) throws Exception {
		var a = assertPage(page(client(), p.path())).isValid().hasContractVersion("1");
		p.expect().accept(a);
	}

	/** The builder twins meet the same expectations; pages with an upstream gap are covered by {@link PetstoreTwins_Test}. */
	static Stream<Page> builderPages() {
		var gapped = PetstoreTwins_Test.GAPS.stream().map(PetstoreTwins_Test.Gap::path).toList();
		return PAGES.stream().filter(x -> ! gapped.contains(x.path()));
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("builderPages")
	void a03_builder(Page p) throws Exception {
		var a = assertPage(page(client(), p.path() + "?builder")).isValid().hasContractVersion("1");
		p.expect().accept(a);
	}

	@Test
	void a02_p2Body() throws Exception {
		var t = assertPage(page(client(), "/console/pets")).card("pets").getMap("table");
		assertBean(t, "dataMode,dataUrl,selection{rowIdField,scope}", "server,/console/pets/query,{id,persistent}");
		assertBean(t, "bulk{actions{#{id,bulkMode}}}", "{{[{sell,perRow}]}}");
		var ribbon = t.getList("ribbon");
		var status = find(ribbon, "id", "status");
		assertBean(status, "type,default,deselectable", "optionGroup,available,true");
		assertBean(status.getList("options").getMap(0), "id,value", "available,$eq(AVAILABLE)");
		var species = find(ribbon, "id", "species");
		assertBean(species, "default,deselectable", "mammals,true");
		assertBean(species.getList("options").getMap(0), "value", "$in(DOG,CAT,MOUSE,RABBIT)");
		var price = find(t.getList("columns"), "key", "price");
		assertBean(price, "label", "Price (USD)");
		var export = find(ribbon, "type", "export");
		assertBean(export, "buttons,optional", "[copy,csv],[excel,pdf]");
	}

	private static JsonMap find(JsonList list, String key, String value) {
		for (var m : list.elements(JsonMap.class))
			if (value.equals(m.getString(key)))
				return m;
		throw new AssertionError("no element with " + key + "='" + value + "'");
	}
}
