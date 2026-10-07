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
package org.apache.juneau.petstore.console.ops;

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
class AuditRest_Test extends TestBase {

	private static MockRestClient client() {
		return MockRestClient.create(PetstoreConsoleFixture.Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	private static String q(String tsSearch, String actorSearch) {
		return "{\"draw\":1,\"length\":1000,\"order\":[{\"column\":0,\"dir\":\"desc\"}],\"columns\":["
			+ "{\"data\":\"at\",\"search\":{\"value\":\"" + tsSearch + "\"}},"
			+ "{\"data\":\"actor\",\"search\":{\"value\":\"" + actorSearch + "\"}},"
			+ "{\"data\":\"action\"},{\"data\":\"entity\"},{\"data\":\"entityId\"},{\"data\":\"detail\"}]}";
	}

	@Test void a01_seedAuditIsQueryable() throws Exception {
		var r = new JsonMap(client().post("/console/ops/audit/query", q("", "")).contentType("application/json").run().assertStatus(200).getContent().asString());
		assertTrue(r.getInt("recordsTotal") >= PetstoreSeed.AUDIT, "recordsTotal");
	}

	@Test void a02_timestampBetween() throws Exception {
		var range = "$between(2026-09-30T00:00:00Z,2026-10-01T12:00:00Z)";
		var r = new JsonMap(client().post("/console/ops/audit/query", q(range, "")).contentType("application/json").run().assertStatus(200).getContent().asString());
		assertTrue(r.getInt("recordsFiltered") > 0, "some entries in the window");
		assertTrue(r.getInt("recordsFiltered") < r.getInt("recordsTotal"), "not all entries in the window");
		for (var row : r.getList("data").elements(JsonMap.class))
			assertTrue(row.getString("at").compareTo("2026-09-30") >= 0, row.toString());
	}

	@Test void a03_consoleWriteShowsUp() throws Exception {
		var c = client();
		c.post("/console/pets", "{\"name\":\"Audited\",\"price\":1}").contentType("application/json").run().assertStatus(200);
		var r = c.post("/console/ops/audit/query", q("", "$eq(console)")).contentType("application/json").run().getContent().asString();
		assertContains("Audited", r);
	}

	@Test void a04_rowsEndpointNewestFirst() throws Exception {
		var list = new JsonList(client().get("/console/ops/audit/rows").run().assertStatus(200).getContent().asString());
		assertTrue(list.getMap(0).getString("at").compareTo(list.getMap(list.size() - 1).getString("at")) >= 0, "newest first");
	}

	private static JsonMap globalSearch(String value) throws Exception {
		var body = "{\"draw\":1,\"length\":25,\"search\":{\"value\":\"" + value + "\"},\"columns\":["
			+ "{\"data\":\"at\"},{\"data\":\"actor\"},{\"data\":\"action\"},{\"data\":\"entity\"},{\"data\":\"entityId\"},{\"data\":\"detail\"}]}";
		return new JsonMap(client().post("/console/ops/audit/query", body).contentType("application/json").run().assertStatus(200).getContent().asString());
	}

	@Test void a05_globalSearchFindsSeededEntitiesAndOnlyRestockJobEntries() throws Exception {
		// The browser test searches the audit table; the seed has Pet/Order/User entries.
		var hit = globalSearch("Order");
		assertTrue(hit.getInt("recordsFiltered") > 0, "seed has Order entries");
		for (var row : hit.getList("data").elements(JsonMap.class))
			assertString("Order", row.getString("entity"));
		// The store is shared with JobsRest_Test, so restock jobs may already have run; no seed entry may match.
		for (var row : globalSearch("restock").getList("data").elements(JsonMap.class))
			assertTrue(row.getString("actor").startsWith("job:restock"), row.toString());
	}
}
