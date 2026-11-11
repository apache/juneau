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
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Expandable row details across the console tables: the bean-backed detail endpoints (200 for a known row, an empty 404
 * for any other), the projection that keeps unexposed properties off the wire, the scoped rows behind the nested-table
 * tabs, and the detail each table's card declares.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstoreRowDetails_Test extends TestBase {

	private static JsonMap json(MockRestClient c, String path, int status) throws Exception {
		return new JsonMap(c.get(path).accept("application/json").run().assertStatus(status).getContent().asString());
	}

	private static JsonList list(MockRestClient c, String path) throws Exception {
		return new JsonList(c.get(path).accept("application/json").run().assertStatus(200).getContent().asString());
	}

	private static void assertEmpty404(MockRestClient c, String path) throws Exception {
		var r = c.get(path).accept("application/json").run().assertStatus(404);
		assertEquals("{}", r.getContent().asString().replaceAll("\\s", ""), path);
		assertEquals("no-store", r.getStringHeader("Cache-Control").orElse(null), path);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - the bean-backed detail endpoints
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_petDetail() throws Exception {
		var m = json(client(), "/console/pets/row-detail/pet/1", 200);
		assertEquals("1", m.getString("contractVersion"));
		var f = m.getMap("fields");
		assertEquals("Mr. Frisky", f.getString("name"));
		assertNotNull(f.get("price"));
	}

	@Test void a02_orderDetail() throws Exception {
		var f = json(client(), "/console/orders/row-detail/order/101", 200).getMap("fields");
		assertEquals(101L, f.getLong("id"));
		assertNotNull(f.get("status"));
	}

	@Test void a03_userDetailNeverCarriesPrivateProperties() throws Exception {
		var body = client().get("/console/users/row-detail/user/mwatson").accept("application/json").run().assertStatus(200).getContent().asString();
		assertEquals("mwatson", new JsonMap(body).getMap("fields").getString("username"));
		for (var secret : new String[] {"password", "email", "phone"})
			assertFalse(body.contains(secret), "leaked " + secret);
	}

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = {
		"/console/pets/row-detail/pet/999999", "/console/pets/row-detail/pet/abc", "/console/pets/row-detail/nope/1",
		"/console/orders/row-detail/order/999999", "/console/users/row-detail/user/nobody"
	})
	void a04_unknownRowIsAnEmpty404(String path) throws Exception {
		assertEmpty404(client(), path);
	}

	@Test void a05_tabsAndCounts() throws Exception {
		var c = client();
		assertEquals("Mr. Frisky", json(c, "/console/pets/row-detail/pet/1/tab/details", 200).getMap("fields").getString("name"));
		assertTrue(json(c, "/console/pets/row-detail/pet/1/counts", 200).getMap("counts").isEmpty()); // the nested tabs count their own rows
		assertEmpty404(c, "/console/pets/row-detail/pet/999999/counts");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - the scoped rows behind the nested-table tabs
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_petOrdersAreScopedToThePet() throws Exception {
		var c = client();
		var all = list(c, "/petstore/orders");
		var petId = all.getMap(0).getLong("petId");
		var rows = list(c, "/console/pets/orders/rows?petId=" + petId);
		assertFalse(rows.isEmpty());
		for (var i = 0; i < rows.size(); i++)
			assertEquals(petId, rows.getMap(i).getLong("petId"));
	}

	@Test void b02_petHistoryIsScopedToThePet() throws Exception {
		var c = client();
		var rows = list(c, "/console/pets/history/rows?petId=1");
		for (var i = 0; i < rows.size(); i++) {
			assertEquals("Pet", rows.getMap(i).getString("entity"));
			assertEquals("1", rows.getMap(i).getString("entityId"));
		}
	}

	@Test void b03_userOrdersAreThoseTheUserPlaced() throws Exception {
		var c = client();
		store().createUser(new org.apache.juneau.petstore.dto.User().setUsername("orderer1").setPassword("x"), "test");
		var placed = store().createOrder(new org.apache.juneau.petstore.dto.Order().setPetId(1), "console:orderer1");
		var rows = list(c, "/console/users/orders/rows?username=orderer1");
		assertEquals(1, rows.size());
		assertEquals(placed.getId(), rows.getMap(0).getLong("id"));
		assertEquals(0, list(c, "/console/users/orders/rows?username=mwatson").size());
	}

	@Test void b04_orderPetIsTheOrdersPet() throws Exception {
		var c = client();
		var o = list(c, "/petstore/orders").getMap(0);
		var rows = list(c, "/console/orders/pet/rows?orderId=" + o.getLong("id"));
		assertEquals(1, rows.size());
		assertEquals(o.getLong("petId"), rows.getMap(0).getLong("id"));
	}

	@ParameterizedTest(name="{0}")
	@ValueSource(strings = {
		"/console/pets/orders/rows?petId=999999", "/console/pets/orders/rows", "/console/pets/history/rows?petId=999999",
		"/console/users/orders/rows?username=nobody", "/console/users/orders/rows", "/console/orders/pet/rows?orderId=999999"
	})
	void b05_unknownParentIs404(String path) throws Exception {
		client().get(path).accept("application/json").run().assertStatus(404);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - the detail each table declares
	//-----------------------------------------------------------------------------------------------------------------

	static Stream<org.junit.jupiter.params.provider.Arguments> tables() {
		return Stream.of(
			Arguments.of("/console/pets", "pets", "/console/pets/row-detail/pet/{id}"),
			Arguments.of("/console/pets/sold", "sold", "/console/pets/row-detail/soldPet/{id}"),
			Arguments.of("/console/orders", "orders", "/console/orders/row-detail/order/{id}"),
			Arguments.of("/console/store", "stats", "/console/orders/row-detail/order/{id}"),
			Arguments.of("/console/users", "users", "/console/users/row-detail/user/{id}"));
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("tables")
	void c01_tableDeclaresItsDetail(String path0, String card, String endpoint) throws Exception {
		for (var suffix : new String[] {"", "?builder"}) {
			var page = path(path0, suffix);
			assertPage(page(client(), page)).isValid().hasCard(card, "datatables")
				.hasCardKey(card, "/table/detail/endpoint", endpoint)
				.hasCardKey(card, "/table/detail/mode", "bean");
		}
	}

	@Test void c02_inlineDetails() throws Exception {
		for (var suffix : new String[] {"", "?builder"}) {
			assertPage(page(client(), path("/console/pets/changes", suffix))).hasCardKey("changes", "/table/detail/mode", "inline");
			assertPage(page(client(), path("/console/ops/audit", suffix))).hasCardKey("audit", "/table/detail/mode", "inline");
			assertPage(page(client(), path("/console/pets/1/history", suffix))).hasCardKey("history", "/table/detail/mode", "inline");
		}
	}

	@Test void c03_detailsCarryTheirNestedTabs() throws Exception {
		var pets = card(page(client(), "/console/pets?builder"), "pets");
		var kinds = pets.getMap("table").getMap("detail").getList("tabs").stream().map(x -> ((java.util.Map<?,?>)x).get("id")).toList();
		assertEquals(java.util.List.of("details", "orders", "history"), kinds);
	}

	private static String path(String base, String suffix) {
		return base + suffix;
	}

	private static JsonMap card(String html, String id) {
		return assertPage(html).card(id);
	}
}
