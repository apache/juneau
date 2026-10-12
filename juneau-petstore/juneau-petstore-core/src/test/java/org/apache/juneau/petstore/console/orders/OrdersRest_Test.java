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

import static org.apache.juneau.test.bct.BctAssertions.*;

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
}
