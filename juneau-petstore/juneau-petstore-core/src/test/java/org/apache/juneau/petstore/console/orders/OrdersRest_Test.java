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
package org.apache.juneau.petstore.console.orders;

import static org.apache.juneau.bct.BctAssertions.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class OrdersRest_Test extends TestBase {

	private static MockRestClient client() {
		return MockRestClient.create(PetstoreConsoleFixture.Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	/** Creates an order of our own through the API: the host store is shared across the fork. */
	private static long newOrder(MockRestClient c, String status) throws Exception {
		return new JsonMap(c.post("/petstore/orders", "{\"petId\":1,\"status\":\"" + status + "\"}").contentType("application/json").run().assertStatus(200).getContent().asString()).getLong("id");
	}

	/** Creates an order of our own on a ship date through the API. */
	private static long newOrderOn(MockRestClient c, String status, String shipDate) throws Exception {
		return new JsonMap(c.post("/petstore/orders", "{\"petId\":1,\"status\":\"" + status + "\",\"shipDate\":\"" + shipDate + "\"}").contentType("application/json").run().assertStatus(200).getContent().asString()).getLong("id");
	}

	private static final String ALL = "{\"draw\":1,\"length\":-1,\"columns\":[{\"data\":\"id\"},{\"data\":\"status\"},{\"data\":\"shipDate\"}]}";
	/** Newest first, so the server's page cap keeps the orders a test just created however many the shared store holds. */
	private static final String NEWEST = "{\"draw\":1,\"length\":-1,\"order\":[{\"column\":0,\"dir\":\"desc\"}],\"columns\":[{\"data\":\"id\"},{\"data\":\"status\"},{\"data\":\"shipDate\"}]}";
	private static final String PLACED_ONLY = "{\"draw\":1,\"length\":-1,\"columns\":[{\"data\":\"id\"},{\"data\":\"status\",\"search\":{\"value\":\"$eq(PLACED)\"}},{\"data\":\"shipDate\"}]}";

	private static JsonMap queryOn(String qs, String body) throws Exception {
		return new JsonMap(client().post("/console/orders/query" + qs, body).contentType("application/json").run().assertStatus(200).getContent().asString());
	}

	private static Set<Long> ids(JsonMap r) {
		var s = new HashSet<Long>();
		for (var row : r.getList("data").elements(JsonMap.class))
			s.add(row.getLong("id"));
		return s;
	}

	@Test void a03_dayKeepsOnlyThatDaysOrders() throws Exception {
		var c = client();
		var in = newOrderOn(c, "PLACED", "2031-03-04");
		var before = newOrderOn(c, "PLACED", "2031-03-03");
		var after = newOrderOn(c, "PLACED", "2031-03-05");
		var r = queryOn("?day=2031-03-04", ALL);
		var ids = ids(r);
		Assertions.assertTrue(ids.contains(in), "the day's own order is shown");
		Assertions.assertFalse(ids.contains(before), "the previous day is before the window");
		Assertions.assertFalse(ids.contains(after), "the next day is not in the day");
		for (var row : r.getList("data").elements(JsonMap.class))
			assertBean(row, "shipDate", "2031-03-04");
		Assertions.assertEquals(ids.size(), r.getInt("recordsFiltered"), "the count is of the filtered orders");
	}

	@Test void a03b_dayKeepsOrdersWhoseShipDateIsADateTime() throws Exception {
		var c = client();
		var utc = newOrderOn(c, "PLACED", "2031-06-04T12:00:00Z");
		var offset = newOrderOn(c, "PLACED", "2031-06-04T23:30:00+02:00");
		var local = newOrderOn(c, "PLACED", "2031-06-04T08:15:00");
		var next = newOrderOn(c, "PLACED", "2031-06-05T00:00:00Z");
		var ids = ids(queryOn("?day=2031-06-04", ALL));
		Assertions.assertTrue(ids.containsAll(Set.of(utc, offset, local)), "datetime ship dates on the day are kept");
		Assertions.assertFalse(ids.contains(next), "a datetime on the next day is not");
		Assertions.assertEquals(ids, ids(queryOn("?day=%202031-06-04%20", ALL)), "the day param is trimmed");
	}

	@Test void a04_dayAndsWithTheRibbonFilter() throws Exception {
		var c = client();
		var placed = newOrderOn(c, "PLACED", "2031-04-04");
		var delivered = newOrderOn(c, "DELIVERED", "2031-04-04");
		var otherDay = newOrderOn(c, "PLACED", "2031-04-05");
		var ids = ids(queryOn("?day=2031-04-04", PLACED_ONLY));
		Assertions.assertTrue(ids.contains(placed));
		Assertions.assertFalse(ids.contains(delivered), "the ribbon status filter still applies");
		Assertions.assertFalse(ids.contains(otherDay), "the day filter still applies");
	}

	@Test void a05_malformedDayIsIgnored() throws Exception {
		var c = client();
		newOrderOn(c, "PLACED", "2031-05-04");
		newOrderOn(c, "PLACED", "2031-05-05");
		var everything = queryOn("", ALL);
		var all = ids(everything);
		Assertions.assertTrue(queryOn("?day=2031-05-04", ALL).getInt("recordsFiltered") < everything.getInt("recordsFiltered"), "a real day does narrow");
		for (var bad : List.of("garbage", "2031-02-30", "2031-5-4", "2031-05-04T00:00:00Z", "", "%20", "20310504"))
			try {
				Assertions.assertEquals(all, ids(queryOn("?day=" + bad, ALL)), "day '" + bad + "' is ignored");
			} catch (RuntimeException e) {
				throw new AssertionError("day '" + bad + "': " + e.getMessage(), e);
			}
	}

	@Test void a07_windowKeepsOrdersShippedBetweenStartAndEndInclusive() throws Exception {
		var c = client();
		var before = newOrderOn(c, "PLACED", "2032-03-09");
		var first = newOrderOn(c, "PLACED", "2032-03-10");
		var middle = newOrderOn(c, "PLACED", "2032-03-12T15:00:00Z");
		var last = newOrderOn(c, "PLACED", "2032-03-14");
		var after = newOrderOn(c, "PLACED", "2032-03-15");
		var r = queryOn("?start=2032-03-10&end=2032-03-14", ALL);
		var ids = ids(r);
		Assertions.assertTrue(ids.containsAll(Set.of(first, middle, last)), "both ends and the inside are kept");
		Assertions.assertFalse(ids.contains(before), "a day before start is out");
		Assertions.assertFalse(ids.contains(after), "a day after end is out");
		Assertions.assertEquals(ids.size(), r.getInt("recordsFiltered"), "the count is of the filtered orders");
	}

	@Test void a08_windowWithOneBoundIsOpenOnTheOtherSide() throws Exception {
		var c = client();
		var early = newOrderOn(c, "PLACED", "2032-04-01");
		var late = newOrderOn(c, "PLACED", "2032-04-30");
		var from = ids(queryOn("?start=2032-04-15", NEWEST));
		Assertions.assertTrue(from.contains(late));
		Assertions.assertFalse(from.contains(early));
		var to = queryOn("?end=2032-04-15", NEWEST);
		Assertions.assertFalse(ids(to).contains(late));
		// Open below: everything shipped up to the bound, which is more than the one order of ours.
		Assertions.assertTrue(to.getInt("recordsFiltered") > 1);
		Assertions.assertTrue(to.getInt("recordsFiltered") < queryOn("", NEWEST).getInt("recordsFiltered"), "the bound narrows");
	}

	@Test void a09_windowAndsWithDayAndTheRibbonFilter() throws Exception {
		var c = client();
		var placed = newOrderOn(c, "PLACED", "2032-05-10");
		var delivered = newOrderOn(c, "DELIVERED", "2032-05-10");
		var otherDay = newOrderOn(c, "PLACED", "2032-05-11");
		var ids = ids(queryOn("?start=2032-05-01&end=2032-05-31&day=2032-05-10", PLACED_ONLY));
		Assertions.assertTrue(ids.contains(placed));
		Assertions.assertFalse(ids.contains(delivered), "the ribbon status filter still applies");
		Assertions.assertFalse(ids.contains(otherDay), "the day filter still applies");
	}

	@Test void a10_malformedWindowBoundsAreIgnored() throws Exception {
		var c = client();
		newOrderOn(c, "PLACED", "2032-06-10");
		newOrderOn(c, "PLACED", "2032-06-11");
		var all = queryOn("", NEWEST);
		for (var bad : List.of("garbage", "2032-02-30", "", "%20", "20320610"))
			Assertions.assertEquals(ids(all), ids(queryOn("?start=" + bad + "&end=" + bad, NEWEST)), "window bound '" + bad + "' is ignored");
		var oneBad = queryOn("?start=garbage&end=2032-06-10", NEWEST);
		Assertions.assertTrue(oneBad.getInt("recordsFiltered") < all.getInt("recordsFiltered"), "the usable bound still narrows");
	}

	@Test void a11_anOrderWithoutAShipDateIsOutOfAnyWindow() throws Exception {
		var c = client();
		var none = newOrder(c, "PLACED");
		Assertions.assertFalse(ids(queryOn("?start=2000-01-01&end=2099-12-31", NEWEST)).contains(none));
		Assertions.assertTrue(ids(queryOn("", NEWEST)).contains(none));
	}

	@Test void a06_aDayWithNoOrdersIsEmptyNotAnError() throws Exception {
		var r = queryOn("?day=1999-01-01", ALL);
		assertSize(0, r.getList("data"));
		Assertions.assertEquals(0, r.getInt("recordsFiltered"));
	}

	@Test void a01_queryByStatus() throws Exception {
		var body = "{\"draw\":1,\"length\":5,\"columns\":[{\"data\":\"id\"},{\"data\":\"status\",\"search\":{\"value\":\"$eq(PLACED)\"}},{\"data\":\"shipDate\"}]}";
		var r = new JsonMap(client().post("/console/orders/query", body).contentType("application/json").run().assertStatus(200).getContent().asString());
		assertNotEmpty(r.getList("data"));
		for (var row : r.getList("data").elements(JsonMap.class))
			assertBean(row, "status", "PLACED");
	}

	@Test void a02_badExpressionIsAnErrorEnvelope() throws Exception {
		var body = "{\"draw\":1,\"length\":5,\"columns\":[{\"data\":\"status\",\"search\":{\"value\":\"$nope(\"}}]}";
		client().post("/console/orders/query", body).contentType("application/json").run().assertStatus(200).assertContent().isContains("error");
	}

	@Test void b01_approveThenDeliver() throws Exception {
		var c = client();
		var id = newOrder(c, "PLACED");
		c.post("/console/orders/" + id + "/approve", "").run().assertStatus(200).assertContent().isContains("APPROVED");
		c.post("/console/orders/" + id + "/deliver", "").run().assertStatus(200).assertContent().isContains("DELIVERED");
		c.get("/petstore/orders/" + id).run().assertContent().isContains("DELIVERED");
	}

	@Test void b02_illegalTransition409() throws Exception {
		var c = client();
		var id = newOrder(c, "DELIVERED");
		c.post("/console/orders/" + id + "/approve", "").run()
			.assertStatus(409).assertContent().isContains("Order '" + id + "' is 'DELIVERED'; cannot 'approve'");
	}

	@Test void b03_unknown404() throws Exception {
		client().post("/console/orders/99999/approve", "").run().assertStatus(404).assertContent().isContains("Unknown order '99999'");
	}

	@Test void b04_deleteIsShared() throws Exception {
		var c = client();
		var id = newOrder(c, "PLACED");
		c.delete("/console/orders/" + id).run().assertStatus(200);
		c.get("/petstore/orders/" + id).run().assertStatus(404);
	}

	@Test void b05_cancelRecordsTheReason() throws Exception {
		var c = client();
		var id = newOrder(c, "PLACED");
		c.request("DELETE", "/console/orders/" + id, true).contentType("application/json").content("{\"fields\":{\"reason\":\"Out of stock\",\"notify\":true}}").run().assertStatus(200);
		var q = "{\"draw\":1,\"length\":50,\"columns\":[{\"data\":\"detail\",\"search\":{\"value\":\"$contains(Out of stock)\"}}]}";
		client().post("/console/ops/audit/query", q).contentType("application/json").run().assertStatus(200).assertContent().isContains("Out of stock");
	}

	@Test void b05b_plainApiReasonBody() throws Exception {
		var c = client();
		var id = newOrder(c, "PLACED");
		c.request("DELETE", "/console/orders/" + id, true).contentType("application/json").content("{\"reason\":\"Duplicate order\"}").run().assertStatus(200);
		var q = "{\"draw\":1,\"length\":50,\"columns\":[{\"data\":\"detail\",\"search\":{\"value\":\"$contains(Duplicate order)\"}}]}";
		client().post("/console/ops/audit/query", q).contentType("application/json").run().assertStatus(200).assertContent().isContains("cancelled: Duplicate order");
	}

	@Test void b05c_malformedBody400() throws Exception {
		var c = client();
		var id = newOrder(c, "PLACED");
		c.request("DELETE", "/console/orders/" + id, true).contentType("application/json").content("{not json").run().assertStatus(400).assertContent().isContains("Invalid cancel body");
		c.get("/petstore/orders/" + id).run().assertStatus(200);
	}

	@Test void b06_cancelForm() throws Exception {
		var c = client();
		var id = newOrder(c, "PLACED");
		c.get("/console/orders/" + id + "/cancel-form").run().assertStatus(200).assertContent().isContains("Cancel order '" + id + "'", "reason", "notify");
		c.get("/console/orders/99999/cancel-form").run().assertStatus(404).assertContent().isContains("Unknown order '99999'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// The two-step new-order wizard: pick a pet (step 1), then review it (a stacked step), both on the compact chrome.
	//-----------------------------------------------------------------------------------------------------------------

	private static String drafts(String json) {
		return "?juneauDrafts=" + URLEncoder.encode(json, StandardCharsets.UTF_8);
	}

	@Test void c01_newFormIsStepOne() throws Exception {
		var m = new JsonMap(client().get("/console/orders/new-form").run().assertStatus(200).getContent().asString());
		assertBean(m, "chrome,title", "compact,New order: choose a pet");
		assertBean(m.getMap("form"), "fields{#{name}}", "{[{petId},{shipDate},{review}]}");
		assertBean(m, "childActions{#{id,form,carryDrafts,endpoint,method,onSuccess}}", "{[{review,/console/orders/review-form,true,/console/orders,POST,redraw}]}");
	}

	@Test void c02_stepOneOffersOnlyPetsThatAreForSale() throws Exception {
		var m = new JsonMap(client().get("/console/orders/new-form").run().assertStatus(200).getContent().asString());
		var options = m.getMap("form").getList("fields").getMap(0).getList("options");
		assertNotEmpty(options);
		var c = client();
		for (var o : options.elements(JsonMap.class))
			c.get("/petstore/pets/" + o.getString("value")).run().assertStatus(200).assertContent().isContains("AVAILABLE");
	}

	/** Creates a pet of our own through the console API (sold when asked); the caller removes it with {@link #dropPet}. */
	private static long newPet(MockRestClient c, boolean sold) throws Exception {
		var created = new JsonMap(c.post("/console/pets", "{\"name\":\"Wizpet\",\"price\":3}").contentType("application/json").run().assertStatus(200).getContent().asString());
		var id = created.getMap("row").getLong("id");
		if (sold)
			c.post("/console/pets/" + id + "/sell", "").run().assertStatus(200);
		return id;
	}

	/** Removes a pet made by {@link #newPet}, so count-based tests are not skewed. */
	private static void dropPet(MockRestClient c, long id) throws Exception {
		c.delete("/console/pets/" + id).run().assertStatus(200);
	}

	@Test void c03_reviewFormIsTheStackedStepPrefilledFromTheDrafts() throws Exception {
		var c = client();
		var id = newPet(c, false);
		try {
			var m = new JsonMap(c.get("/console/orders/review-form" + drafts("{\"petId\":\"" + id + "\",\"shipDate\":\"2031-02-03\"}")).run().assertStatus(200).getContent().asString());
			assertBean(m, "chrome,title", "compact,Review order");
			assertContains("Wizpet", m.getList("fields").toString());
			assertContains("2031-02-03", m.getList("fields").toString());
			assertBean(m.getMap("form"), "fields{#{name,value}}", "{[{petId," + id + "},{shipDate,2031-02-03}]}");
		} finally {
			dropPet(c, id);
		}
	}

	@Test void c04_reviewFormRefusesMissingOrUnknownDrafts() throws Exception {
		var c = client();
		var id = newPet(c, false);
		try {
			c.get("/console/orders/review-form").run().assertStatus(400).assertContent().isContains("Missing order draft");
			c.get("/console/orders/review-form" + drafts("{\"petId\":\"99999\"}")).run().assertStatus(404).assertContent().isContains("Unknown pet '99999'");
			c.get("/console/orders/review-form" + drafts("{\"petId\":\"" + id + "\",\"shipDate\":\"soon\"}")).run().assertStatus(400).assertContent().isContains("Invalid ship date 'soon'");
			c.get("/console/orders/review-form" + drafts("{not json")).run().assertStatus(400).assertContent().isContains("Invalid order draft");
		} finally {
			dropPet(c, id);
		}
	}

	@Test void c05_placeOrderFromTheDialogEnvelope() throws Exception {
		var c = client();
		var pet = newPet(c, false);
		try {
			var r = new JsonMap(c.post("/console/orders", "{\"action\":\"new-order\",\"fields\":{\"petId\":\"" + pet + "\",\"shipDate\":\"2031-02-03\"}}")
				.contentType("application/json").run().assertStatus(200).getContent().asString());
			var id = r.getMap("row").getLong("id");
			assertBean(r.getMap("row"), "petId,status,shipDate", pet + ",PLACED,2031-02-03");
			c.get("/petstore/orders/" + id).run().assertStatus(200).assertContent().isContains("PLACED");
		} finally {
			dropPet(c, pet);
		}
	}

	@Test void c06_placeOrderRefusesBadInput() throws Exception {
		var c = client();
		var pet = newPet(c, false);
		try {
			c.post("/console/orders", "{\"fields\":{\"petId\":\"99999\"}}").contentType("application/json").run().assertStatus(404).assertContent().isContains("Unknown pet '99999'");
			c.post("/console/orders", "{\"fields\":{\"petId\":\"" + pet + "\",\"shipDate\":\"soon\"}}").contentType("application/json").run().assertStatus(400).assertContent().isContains("Invalid ship date 'soon'");
			c.post("/console/orders", "{\"fields\":{}}").contentType("application/json").run().assertStatus(400).assertContent().isContains("Missing pet");
		} finally {
			dropPet(c, pet);
		}
	}

	@Test void c07_aPetThatIsNotForSaleIs409() throws Exception {
		var c = client();
		var id = newPet(c, true);
		try {
			var msg = "Pet '" + id + "' is 'SOLD'; only an available pet can be ordered";
			c.get("/console/orders/review-form" + drafts("{\"petId\":\"" + id + "\"}")).run().assertStatus(409).assertContent().isContains(msg);
			c.post("/console/orders", "{\"fields\":{\"petId\":\"" + id + "\"}}").contentType("application/json").run().assertStatus(409).assertContent().isContains(msg);
		} finally {
			dropPet(c, id);
		}
	}

	@Test void c08_aMalformedBodyIs400() throws Exception {
		var c = client();
		c.post("/console/orders", "null").contentType("application/json").run().assertStatus(400).assertContent().isContains("Missing order body");
		c.post("/console/orders", "{\"fields\":\"x\"}").contentType("application/json").run().assertStatus(400).assertContent().isContains("Invalid order fields");
	}
}
