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

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class ChangesRest_Test extends TestBase {

	/** A lax client that asks for JSON (the console resources also render HTML for a browser Accept). */
	private static MockRestClient client() {
		return MockRestClient.create(PetstoreConsoleFixture.Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	private static JsonMap json(String s) throws Exception {
		return new JsonMap(s);
	}

	private static long newPet(MockRestClient c, String name) throws Exception {
		return json(c.post("/console/pets", "{\"name\":\"" + name + "\",\"price\":3}").contentType("application/json").run().assertStatus(200).getContent().asString()).getMap("row").getLong("id");
	}

	private static long stage(MockRestClient c, long petId, String field, String value) throws Exception {
		return json(c.post("/console/pets/" + petId + "/stage", "{\"field\":\"" + field + "\",\"value\":\"" + value + "\"}")
			.contentType("application/json").run().assertStatus(200).getContent().asString()).getMap("row").getLong("id");
	}

	@Test void a01_pendingBadgeSource() throws Exception {
		var c = client();
		var id = stage(c, 2, "name", "Badgepet");
		var r = json(c.get("/console/pets/changes/pending").run().assertStatus(200).getContent().asString());
		assertTrue(r.getInt("total") >= 1);
		assertContains("\"id\":\"" + id + "\"", r.toString());
		assertContains("/console/pets/2/summary", r.toString());
	}

	@Test void a02_pendingFlagsMineByAuthor() throws Exception {
		var c = client();
		var petId = newPet(c, "Authorpet");
		var id = json(c.post("/console/pets/" + petId + "/stage", "{\"field\":\"name\",\"value\":\"ByAdmin\"}")
			.contentType("application/json").header("Authorization", "Bearer petstore-admin").run().assertStatus(200).getContent().asString()).getMap("row").getLong("id");
		var anon = mineOf(json(c.get("/console/pets/changes/pending").run().assertStatus(200).getContent().asString()), id);
		var admin = mineOf(json(c.get("/console/pets/changes/pending").header("Authorization", "Bearer petstore-admin").run().assertStatus(200).getContent().asString()), id);
		assertFalse(anon);
		assertTrue(admin);
	}

	private static boolean mineOf(JsonMap r, long id) {
		for (var m : r.getList("items").elements(JsonMap.class))
			if (String.valueOf(id).equals(m.getString("id")))
				return m.getBoolean("mine");
		throw new AssertionError("no pending change '" + id + "'");
	}

	@Test void b01_applyWritesThePet() throws Exception {
		var c = client();
		var petId = newPet(c, "Applypet");
		var id = stage(c, petId, "price", "7.25");
		c.post("/console/pets/changes/" + id + "/apply", "").run().assertStatus(200).assertContent().isContains("APPLIED");
		c.get("/petstore/pets/" + petId).run().assertContent().isContains("7.25");
	}

	@Test void b02_applyTwice409() throws Exception {
		var c = client();
		var id = stage(c, 4, "name", "Twice");
		c.post("/console/pets/changes/" + id + "/apply", "").run().assertStatus(200);
		c.post("/console/pets/changes/" + id + "/apply", "").run().assertStatus(409).assertContent().isContains("Change '" + id + "' is not pending");
	}

	@Test void b03_applyAfterPetDeletedFailsThenRetry404Stays() throws Exception {
		var c = client();
		var petId = json(c.post("/console/pets", "{\"name\":\"Doomed\",\"price\":1}").contentType("application/json").run().getContent().asString()).getMap("row").getLong("id");
		var id = stage(c, petId, "name", "Ghost");
		c.delete("/console/pets/" + petId).run().assertStatus(200);
		c.post("/console/pets/changes/" + id + "/apply", "").run().assertStatus(200).assertContent().isContains("FAILED", "Unknown pet '" + petId + "'");
		c.post("/console/pets/changes/" + id + "/retry", "").run().assertStatus(200).assertContent().isContains("FAILED");
	}

	@Test void b04_applyUnknown404() throws Exception {
		client().post("/console/pets/changes/999999/apply", "").run().assertStatus(404).assertContent().isContains("Unknown change '999999'");
	}

	@Test void c01_aggregateDiscardBulkResult() throws Exception {
		var c = client();
		var a = stage(c, newPet(c, "Discard1"), "name", "D1");
		var b = stage(c, newPet(c, "Discard2"), "name", "D2");
		c.post("/console/pets/changes/" + b + "/apply", "").run().assertStatus(200);
		var r = json(c.post("/console/pets/changes/discard", "{\"ids\":[\"" + a + "\",\"" + b + "\",\"999999\"],\"idempotencyKey\":\"k-c01\"}")
			.contentType("application/json").run().assertStatus(200).getContent().asString());
		assertBean(r, "succeeded,notFound,failed{#{id,message}}", "[" + a + "],[999999],{[{" + b + ",Change '" + b + "' is not pending}]}");
	}

	@Test void c02_idempotentDiscardReplays() throws Exception {
		var c = client();
		var a = stage(c, 7, "name", "Once");
		var body = "{\"ids\":[\"" + a + "\"],\"idempotencyKey\":\"k-c02\"}";
		var first = c.post("/console/pets/changes/discard", body).contentType("application/json").run().getContent().asString();
		var second = c.post("/console/pets/changes/discard", body).contentType("application/json").run().getContent().asString();
		assertString(first, second);
	}

	@Test void c03_failDiscardToggle503() throws Exception {
		client().post("/console/pets/changes/discard?fail=discard", "{\"ids\":[\"1\"],\"idempotencyKey\":\"k-c03\"}").contentType("application/json").run()
			.assertStatus(503).assertContent().isContains("Discard is unavailable");
	}

	@Test void c04_discardWithoutKey400() throws Exception {
		client().post("/console/pets/changes/discard", "{\"ids\":[\"1\"]}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Missing 'idempotencyKey'");
	}

	@Test void c05_discardWithoutIds400() throws Exception {
		client().post("/console/pets/changes/discard", "{\"idempotencyKey\":\"k-c05\"}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Missing 'ids'");
	}

	@Test void b05_retryNotFailed409() throws Exception {
		var c = client();
		var id = stage(c, newPet(c, "Retrypet"), "name", "NoRetry");
		c.post("/console/pets/changes/" + id + "/retry", "").run().assertStatus(409).assertContent().isContains("Change '" + id + "' has not failed");
	}

	@Test void d01_query() throws Exception {
		var c = client();
		stage(c, 8, "name", "Queried");
		var body = "{\"draw\":1,\"length\":10,\"columns\":[{\"data\":\"id\"},{\"data\":\"state\",\"search\":{\"value\":\"$eq(PENDING)\"}},{\"data\":\"summary\"}]}";
		c.post("/console/pets/changes/query", body).contentType("application/json").run().assertStatus(200).assertContent().isContains("Queried");
	}

	@Test void d02_badSearchIsErrorEnvelope() throws Exception {
		var body = "{\"draw\":2,\"length\":10,\"columns\":[{\"data\":\"id\"},{\"data\":\"state\",\"search\":{\"value\":\"$eq(\"}}]}";
		client().post("/console/pets/changes/query", body).contentType("application/json").run().assertStatus(200).assertContent().isContains("\"error\"");
	}
}
