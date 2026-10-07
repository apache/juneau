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
package org.apache.juneau.petstore.console.pets;

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.data.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetsRest_Test extends TestBase {

	private static final String Q = "/console/pets/query";

	private static String dt(int draw, String colSearchStatus, String global) {
		return "{\"draw\":" + draw + ",\"start\":0,\"length\":10,"
			+ "\"search\":{\"value\":\"" + (global == null ? "" : global) + "\"},"
			+ "\"order\":[{\"column\":0,\"dir\":\"asc\"}],"
			+ "\"columns\":[{\"data\":\"id\"},{\"data\":\"name\"},{\"data\":\"species\"},"
			+ "{\"data\":\"status\",\"search\":{\"value\":\"" + (colSearchStatus == null ? "" : colSearchStatus) + "\"}},{\"data\":\"price\"}]}";
	}

	/** A lax client that asks for JSON (the console resources also render HTML for a browser Accept). */
	private static MockRestClient client() {
		return MockRestClient.create(PetstoreConsoleFixture.Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	private static JsonMap json(String s) throws Exception {
		return new JsonMap(s);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - server-mode query (G-BQ6a)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_queryPagesAndCounts() throws Exception {
		var r = json(client().post(Q, dt(1, null, null)).contentType("application/json").run().assertStatus(200).getContent().asString());
		assertBean(r, "draw", "1");
		assertTrue(r.getInt("recordsTotal") >= PetstoreSeed.PETS, "recordsTotal covers the seed");
		assertTrue(r.getInt("recordsFiltered") >= PetstoreSeed.PETS, "recordsFiltered covers the seed");
		assertSize(10, r.getList("data"));
		assertBean(r.getList("data").getMap(0), "id,name", "1,Mr. Frisky");
	}

	@Test void a02_columnSearchEq() throws Exception {
		var r = json(client().post(Q, dt(2, "$eq(SOLD)", null)).contentType("application/json").run().assertStatus(200).getContent().asString());
		assertNotEmpty(r.getList("data"));
		for (var row : r.getList("data").elements(JsonMap.class))
			assertBean(row, "status", "SOLD");
	}

	@Test void a03_globalSearch() throws Exception {
		var r = json(client().post(Q, dt(3, null, "Frisky")).contentType("application/json").run().assertStatus(200).getContent().asString());
		assertBean(r, "recordsFiltered", "1");
	}

	@Test void a04_malformedExpressionIs200WithError() throws Exception {
		client().post(Q, dt(4, "$nope(x)", null)).contentType("application/json").run()
			.assertStatus(200).assertContent().isContains("\"draw\":4", "\"error\":");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - CRUD, 400/404/409, audit actor, secure flag
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_createIsSharedWithApiAndAudited() throws Exception {
		var c = client();
		var r = json(c.post("/console/pets", "{\"name\":\"Crudpet\",\"species\":\"BIRD\",\"price\":12.5}").contentType("application/json")
			.run().assertStatus(200).getContent().asString());
		assertBean(r, "outcome,message,row{name,status}", "success,Added 'Crudpet',{Crudpet,AVAILABLE}");
		var id = r.getMap("row").getLong("id");
		assertBean(json(c.get("/petstore/pets/" + id).run().assertStatus(200).getContent().asString()), "name,species,price,status", "Crudpet,BIRD,12.5,AVAILABLE");
	}

	@Test void b02_blankName400() throws Exception {
		client().post("/console/pets", "{\"name\":\" \",\"price\":1}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Name must not be blank");
	}

	@Test void b03_negativePrice400() throws Exception {
		client().post("/console/pets", "{\"name\":\"X\",\"price\":-1}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Price '-1.0' must be a non-negative number");
	}

	@Test void b04_updateUnknown404() throws Exception {
		client().put("/console/pets/99999", "{\"name\":\"X\",\"price\":1}").contentType("application/json").run()
			.assertStatus(404).assertContent().isContains("Unknown pet '99999'");
	}

	@Test void b05_updateAndDelete() throws Exception {
		var c = client();
		var id = json(c.post("/console/pets", "{\"name\":\"Gone\",\"price\":3}").contentType("application/json").run().getContent().asString()).getMap("row").getLong("id");
		c.put("/console/pets/" + id, "{\"name\":\"Gone2\",\"price\":4}").contentType("application/json").run().assertStatus(200).assertContent().isContains("Gone2");
		c.delete("/console/pets/" + id).run().assertStatus(200);
		c.get("/petstore/pets/" + id).run().assertStatus(404);
	}

	@Test void b06_markSoldThenConflict() throws Exception {
		var c = client();
		var id = json(c.post("/console/pets", "{\"name\":\"Sellme\",\"price\":3}").contentType("application/json").run().getContent().asString()).getMap("row").getLong("id");
		c.post("/console/pets/" + id + "/sell", "").run().assertStatus(200).assertContent().isContains("SOLD");
		c.post("/console/pets/" + id + "/sell", "").run().assertStatus(409).assertContent().isContains("Pet '" + id + "' is already 'SOLD'");
	}

	@Test void b07_sellUnknown404() throws Exception {
		client().post("/console/pets/99999/sell", "").run().assertStatus(404).assertContent().isContains("Unknown pet '99999'");
	}

	@Test void b08_secureFlagNeedsToken() throws Exception {
		System.setProperty(ConsoleWrites.SECURE_PROPERTY, "true");
		try {
			var c = client();
			c.post("/console/pets", "{\"name\":\"Locked\",\"price\":1}").contentType("application/json").run().assertStatus(401);
			c.post("/console/pets", "{\"name\":\"Unlocked\",\"price\":1}").contentType("application/json")
				.header("Authorization", "Bearer petstore-admin").run().assertStatus(200);
			c.get("/console/ops/audit/rows").run().assertContent().isContains("console:admin");
		} finally {
			System.clearProperty(ConsoleWrites.SECURE_PROPERTY);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - stage (R11) and the P4 JSON feeds
	//-----------------------------------------------------------------------------------------------------------------

	private static long create(MockRestClient c, String name) throws Exception {
		return json(c.post("/console/pets", "{\"name\":\"" + name + "\",\"price\":3}").contentType("application/json").run().getContent().asString()).getMap("row").getLong("id");
	}

	@Test void c01_stageRecordsChangeWithoutChangingPet() throws Exception {
		var c = client();
		var id = create(c, "Stagey");
		var r = json(c.post("/console/pets/" + id + "/stage", "{\"field\":\"price\",\"value\":\"9.5\"}").contentType("application/json")
			.run().assertStatus(200).getContent().asString());
		assertBean(r, "outcome", "success");
		assertContains("Staged '", r.getString("message"));
		assertBean(json(c.get("/petstore/pets/" + id).run().getContent().asString()), "name,price", "Stagey,3.0");
		assertBean(r.getMap("row"), "field,newValue", "price,9.5");
	}

	@Test void c02_stageAuthorIsAnonymousWhenUnsecured() throws Exception {
		var c = client();
		var id = create(c, "Authory");
		c.post("/console/pets/" + id + "/stage", "{\"field\":\"name\",\"value\":\"Renamed\"}").contentType("application/json")
			.run().assertStatus(200).assertContent().isContains("anonymous");
		assertString("anonymous", ConsoleWrites.author("console"));
		assertString("admin", ConsoleWrites.author("console:admin"));
	}

	@Test void c03_stageUnknownPet404() throws Exception {
		client().post("/console/pets/99999/stage", "{\"field\":\"name\",\"value\":\"X\"}").contentType("application/json")
			.run().assertStatus(404);
	}

	@Test void c04_stageBadPrice400() throws Exception {
		var c = client();
		var id = create(c, "Badprice");
		c.post("/console/pets/" + id + "/stage", "{\"field\":\"price\",\"value\":\"-1\"}").contentType("application/json")
			.run().assertStatus(400).assertContent().isContains("Price '-1' must be a non-negative number");
	}

	@Test void c05_photoIsNotStageable() throws Exception {
		var c = client();
		var id = create(c, "Photogenic");
		c.post("/console/pets/" + id + "/stage", "{\"field\":\"photo\",\"value\":\"x\"}").contentType("application/json")
			.run().assertStatus(400).assertContent().isContains("Field 'photo' cannot be staged; use one of: name, price, species, status, tags");
	}

	@Test void c06_historyFeedHasThisPetsAudit() throws Exception {
		var c = client();
		var id = create(c, "Historic");
		var rows = new JsonList(c.get("/console/pets/" + id + "/history/rows").run().assertStatus(200).getContent().asString());
		assertSize(1, rows);
		assertBean(rows.getMap(0), "entity,entityId,action,detail", "Pet," + id + ",CREATE,Historic");
		c.get("/console/pets/99999/history/rows").run().assertStatus(404);
	}

	@Test void c07_ordersFeed() throws Exception {
		var c = client();
		var fresh = new JsonList(c.get("/console/pets/" + create(c, "Ordered") + "/orders/rows").run().assertStatus(200).getContent().asString());
		assertSize(0, fresh);
		var seeded = new JsonList(c.get("/petstore/orders").run().assertStatus(200).getContent().asString()).getMap(0).getLong("petId");
		var rows = new JsonList(c.get("/console/pets/" + seeded + "/orders/rows").run().assertStatus(200).getContent().asString());
		assertTrue(rows.size() >= 1, "seeded pet has orders");
		for (var row : rows.elements(JsonMap.class))
			assertBean(row, "petId", String.valueOf(seeded));
		c.get("/console/pets/99999/orders/rows").run().assertStatus(404);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - write validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_putKeepsUnspecifiedFields() throws Exception {
		var c = client();
		var id = json(c.post("/console/pets", "{\"name\":\"Keeper\",\"species\":\"CAT\",\"price\":3,\"tags\":[\"a\"],\"photo\":\"p\"}").contentType("application/json").run().getContent().asString()).getMap("row").getLong("id");
		c.put("/console/pets/" + id, "{\"name\":\"Keeper2\",\"price\":4}").contentType("application/json").run().assertStatus(200);
		var pet = json(c.get("/petstore/pets/" + id).run().getContent().asString());
		assertBean(pet, "name,species,price,photo", "Keeper2,CAT,4.0,p");
		assertList(pet.getList("tags"), "a");
	}

	@Test void d02_badPrices400() throws Exception {
		var c = client();
		c.post("/console/pets", "{\"name\":\"X\",\"price\":\"NaN\"}").contentType("application/json").run().assertStatus(400);
		c.post("/console/pets", "{\"name\":\"X\",\"price\":\"Infinity\"}").contentType("application/json").run().assertStatus(400);
	}

	@Test void d03_lowercaseBearerPrefixAccepted() throws Exception {
		System.setProperty(ConsoleWrites.SECURE_PROPERTY, "true");
		try {
			client().post("/console/pets", "{\"name\":\"Lower\",\"price\":1}").contentType("application/json")
				.header("Authorization", "bearer petstore-admin").run().assertStatus(200);
		} finally {
			System.clearProperty(ConsoleWrites.SECURE_PROPERTY);
		}
	}
}
