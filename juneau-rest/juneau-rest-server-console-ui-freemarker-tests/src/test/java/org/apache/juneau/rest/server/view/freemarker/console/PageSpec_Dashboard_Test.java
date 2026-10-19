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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

class PageSpec_Dashboard_Test extends TestBase {

	private static final CardDirectiveModel MODEL = new CardDirectiveModel(
		CardRequirements.create().build(new ToolkitPackRegistry()), CardTypeRegistry.standard());

	private static final List<Map<String,Object>> ROWS = List.of(Map.of("d", "2026-10-01", "v", 3));

	private static DashboardSpec dash() {
		return DashboardSpec.create("orders")
			.rail(Rail.left().sections(SearchSection.create().publishes(Topics.filter("search"))))
			.panel(ChartDef.bar("by-reason").data(ROWS).x("d").series("v", "V"))
			.panel("notes", 2);
	}

	@Test void a01_addsCards_pack_andContract() throws Exception {
		var spec = PageSpec.create().html("notes", "Notes", "<p>n</p>").dashboard(dash());
		assertEquals(List.of("notes", "by-reason"), spec.cards.stream().map(CardSpec::id).toList());
		assertTrue(spec.toolkits.contains(ToolkitPackRegistry.PACK_DASHBOARD));
		var cap = new PageCapture();
		spec.applyTo(cap, MODEL);
		var c = JsonMap.ofString(cap.toContractJson());
		assertEquals("orders", c.getMap("dashboard").getString("id"));
		assertEquals(2, c.getMap("dashboard").getList("panels").size());
	}

	@Test void a02_packOrder_beforeCharts() {
		var spec = PageSpec.create().toolkit("views", "charts").html("notes", "<p>n</p>").dashboard(dash());
		assertEquals(List.of("views", "dashboard", "charts"), spec.toolkits);
		var again = PageSpec.create().toolkit("dashboard").html("notes", "<p>n</p>").dashboard(dash());
		assertEquals(List.of("dashboard"), again.toolkits);
	}

	@Test void a03_secondDashboard_isE77() {
		var spec = PageSpec.create().html("notes", "<p>n</p>").dashboard(dash());
		var arg = DashboardSpec.create("other");
		assertEquals("E-77: dashboard 'other': a page has at most one dashboard",
			assertThrows(IllegalArgumentException.class, () -> spec.dashboard(arg)).getMessage());
	}

	@Test void a04_byIdPanel_mustPrecedeDashboard() {
		var target = PageSpec.create();
		var arg2 = dash();
		assertEquals("E-77: dashboard 'orders': panel 'notes' names no card on this page",
			assertThrows(IllegalArgumentException.class, () -> target.dashboard(arg2)).getMessage());
	}

	@Test void a05_null_rejected() {
		var target2 = PageSpec.create();
		assertEquals("PageSpec.dashboard requires a dashboard.",
			assertThrows(IllegalArgumentException.class, () -> target2.dashboard(null)).getMessage());
	}

	@Test void a06_earlyWiring_seesDashboard() {
		// The search section owns filter:search, so a card subscribing to it wires clean.
		// The role is refresh: an html card accepts only params and refresh, so "filter" is E-44.
		var spec = PageSpec.create()
			.html("notes", "<p>n</p>", c -> c.subscribes(Subscription.to(Topics.filter("search")).as("refresh")))
			.dashboard(dash());
		assertDoesNotThrow(() -> spec.checkWiringEarly(CardTypeRegistry.standard(), null));
	}

	@Test void a07_noDashboard_noKey() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().html("notes", "<p>n</p>").applyTo(cap, MODEL);
		assertFalse(JsonMap.ofString(cap.toContractJson()).containsKey("dashboard"));
	}
}
