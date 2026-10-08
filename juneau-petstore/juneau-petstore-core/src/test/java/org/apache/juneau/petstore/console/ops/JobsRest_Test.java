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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S2925", // Polls an asynchronous job; the REST API has no completion hook to wait on.
	"resource" // MockRestClient is a no-op close.
})
class JobsRest_Test extends TestBase {

	private static MockRestClient client() {
		return MockRestClient.create(PetstoreConsoleFixture.Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	private static JsonMap submit(MockRestClient c, String key) throws Exception {
		return new JsonMap(c.post("/console/ops/jobs/restock", "{\"perSpecies\":{\"DOG\":2,\"CAT\":1},\"idempotencyKey\":\"" + key + "\"}")
			.contentType("application/json").run().assertStatus(202).getContent().asString());
	}

	private static JsonMap await(MockRestClient c, String id) throws Exception {
		for (var i = 0; i < 200; i++) {
			var j = new JsonMap(c.get("/console/ops/jobs/" + id).run().assertStatus(200).getContent().asString());
			if (! "RUNNING".equals(j.getString("state")))
				return j;
			Thread.sleep(20);
		}
		return fail("Job '" + id + "' did not finish");
	}

	private static int petCount(MockRestClient c) throws Exception {
		return new JsonList(c.get("/petstore/pets").run().assertStatus(200).getContent().asString()).size();
	}

	@Test void a01_restockRunsAndAudits() throws Exception {
		var c = client();
		var before = petCount(c);
		var ref = submit(c, "k-a01");
		assertContains("/juneau-jobs/", ref.getString("streamUrl"));
		var done = await(c, ref.getString("jobId"));
		assertBean(done, "state,result{message}", "DONE,{Restocked 3 pets}");
		assertString(String.valueOf(before + 3), petCount(c));
	}

	@Test void a02_sameKeySameJob() throws Exception {
		var c = client();
		var a = submit(c, "k-a02");
		var b = submit(c, "k-a02");
		assertString(a.getString("jobId"), b.getString("jobId"));
		await(c, a.getString("jobId"));
	}

	@Test void a03_rowsListsSubmittedJobs() throws Exception {
		var c = client();
		var ref = submit(c, "k-a03");
		c.get("/console/ops/jobs/rows").run().assertStatus(200).assertContent().isContains(ref.getString("jobId"));
		await(c, ref.getString("jobId"));
	}

	@Test void a04_dialogFormShapeIsAccepted() throws Exception {
		var c = client();
		var modal = new JsonMap(c.get("/console/ops/jobs/restock-form").run().assertStatus(200).getContent().asString());
		assertBean(modal, "selfTargeted", "true");
		var key = modal.getString("idempotencyKey");
		c.post("/console/ops/jobs/restock", "{\"action\":\"restock\",\"targetId\":\"" + key + "\",\"idempotencyKey\":\"" + key
			+ "\",\"fields\":{\"DOG\":\"1\",\"CAT\":\"0\",\"BIRD\":\"\"}}").contentType("application/json").run().assertStatus(202);
		var ref = new JsonMap(c.post("/console/ops/jobs/restock", "{\"action\":\"restock\",\"targetId\":\"" + key + "\",\"idempotencyKey\":\"" + key
			+ "\",\"fields\":{\"DOG\":\"1\",\"CAT\":\"0\",\"BIRD\":\"\"}}").contentType("application/json").run().assertStatus(202).getContent().asString());
		assertBean(await(c, ref.getString("jobId")), "state,result{message}", "DONE,{Restocked 1 pets}");
	}

	@Test void a05_formLabelsCoverEverySpecies() throws Exception {
		var body = client().get("/console/ops/jobs/restock-form").run().assertStatus(200).getContent().asString();
		assertContainsAll(body, "Dog (0-50)", "Fish (0-50)", "Snake (0-50)");
		assertFalse(body.contains("Fishs"), "no 'Fishs' typo");
	}

	@Test void a06_restockNamesAreUniqueAndAuditNamesActor() throws Exception {
		var c = client();
		var first = await(c, submit(c, "k-a06a").getString("jobId"));
		var second = await(c, submit(c, "k-a06b").getString("jobId"));
		assertBean(first, "state", "DONE");
		assertBean(second, "state", "DONE");
		var names = new java.util.ArrayList<String>();
		for (var pet : new JsonList(c.get("/petstore/pets").run().assertStatus(200).getContent().asString()).elements(JsonMap.class))
			if (pet.getString("name").startsWith("DOG #") || pet.getString("name").startsWith("CAT #"))
				names.add(pet.getString("name"));
		assertTrue(names.size() >= 6, "restocked pets present");
		assertSize(names.size(), names.stream().distinct().toList());
		c.get("/console/ops/audit/rows").run().assertStatus(200).assertContent().isContains("job:restock:console");
	}

	@Test void b05_replayAtTheLimitReturnsTheSameJob() throws Exception {
		var c = client();
		var a = submit(c, "k-b05");
		await(c, a.getString("jobId"));
		System.setProperty(JobsRest.LIMIT_PROPERTY, "0");
		try {
			assertString(a.getString("jobId"), submit(c, "k-b05").getString("jobId"));
			c.post("/console/ops/jobs/restock", "{\"perSpecies\":{\"DOG\":1},\"idempotencyKey\":\"k-b05-new\"}").contentType("application/json").run()
				.assertStatus(429);
		} finally {
			System.clearProperty(JobsRest.LIMIT_PROPERTY);
		}
	}

	@Test void b06_nonMapPerSpecies400() throws Exception {
		client().post("/console/ops/jobs/restock", "{\"perSpecies\":5,\"idempotencyKey\":\"k-b06\"}").contentType("application/json").run()
			.assertStatus(400);
		client().post("/console/ops/jobs/restock", "{\"perSpecies\":{\"DOG\":\"many\"},\"idempotencyKey\":\"k-b06b\"}").contentType("application/json").run()
			.assertStatus(400);
	}

	@Test void b01_badCounts400() throws Exception {
		client().post("/console/ops/jobs/restock", "{\"perSpecies\":{\"DOG\":-1},\"idempotencyKey\":\"k-b01\"}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Count for 'DOG' must be between 1 and 50");
	}

	@Test void b02_unknownJob404() throws Exception {
		client().get("/console/ops/jobs/nope").run().assertStatus(404).assertContent().isContains("Unknown job 'nope'");
	}

	@Test void b03_missingKey400() throws Exception {
		client().post("/console/ops/jobs/restock", "{\"perSpecies\":{\"DOG\":1}}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Missing 'idempotencyKey'");
	}

	@Test void b04_limit429() throws Exception {
		System.setProperty(JobsRest.LIMIT_PROPERTY, "0");
		try {
			client().post("/console/ops/jobs/restock", "{\"perSpecies\":{\"DOG\":1},\"idempotencyKey\":\"k-b04\"}").contentType("application/json").run()
				.assertStatus(429).assertContent().isContains("Job limit reached; '0' running");
		} finally {
			System.clearProperty(JobsRest.LIMIT_PROPERTY);
		}
	}

	@Test void c01_opsIndexRedirects() throws Exception {
		PetstoreConsoleFixture.rawClient().get("/console/ops").run().assertStatus(303).assertHeader("Location").isContains("/console/ops/jobs");
	}

	private static JsonMap groom(MockRestClient c, String body) throws Exception {
		return new JsonMap(c.post("/console/ops/jobs/groom", body).contentType("application/json").run().assertStatus(200).getContent().asString());
	}

	@Test void d01_groomStartsARunAndListsIt() throws Exception {
		var c = client();
		var r = groom(c, "{\"pet\":\"Rex\"}");
		assertBean(r, "outcome,message,row{pet,state,verbose}", "success,Grooming Rex,{Rex,RUNNING,false}");
		var id = r.getMap("row").getString("id");
		assertTrue(id.matches("[0-9a-f]{32}"), id);
		c.get("/console/ops/jobs/groom-rows").run().assertStatus(200).assertContent().isContains(id);
	}

	@Test void d02_dialogShapeAndKeyReplay() throws Exception {
		var c = client();
		var body = "{\"action\":\"groom\",\"targetId\":\"k-d02\",\"idempotencyKey\":\"k-d02\",\"fields\":{\"pet\":\"Max\",\"verbose\":true}}";
		var a = groom(c, body);
		var b = groom(c, body);
		assertBean(a, "row{pet,verbose}", "{Max,true}");
		assertString(a.getMap("row").getString("id"), b.getMap("row").getString("id"));
	}

	@Test void d03_mixinServesBothSources() throws Exception {
		var c = client();
		var id = groom(c, "{\"pet\":\"Rex\",\"verbose\":true}").getMap("row").getString("id");
		var p = new JsonMap(c.get("/console/ops/jobs/juneau-console-output/" + id + "/lines?tail=5000").run().assertStatus(200).getContent().asString());
		assertBean(p, "contractVersion,hasEarlier,terminal,state", "1,true,false,RUNNING");
		c.get("/console/ops/jobs/juneau-console-output/" + id + "-file/lines").run().assertStatus(200).assertContent().isContains("\"contractVersion\":\"1\"");
		c.get("/console/ops/jobs/juneau-console-output/" + id + "/download").run().assertStatus(200).assertHeader("Content-Type").isContains("application/jsonl");
		c.get("/console/ops/jobs/juneau-console-output/nope/lines").run().assertStatus(404);
	}

	@Test void d04_rowDetailEnvelope() throws Exception {
		var c = client();
		var id = groom(c, "{\"pet\":\"Rex\"}").getMap("row").getString("id");
		var d = new JsonMap(c.get("/console/ops/jobs/groom-runs/" + id).run().assertStatus(200).getContent().asString());
		assertBean(d, "contractVersion,fields{id,pet}", "1,{" + id + ",Rex}");
		c.get("/console/ops/jobs/groom-runs/nope").run().assertStatus(404).assertContent().isContains("Unknown groom run 'nope'");
	}

	@Test void d05_groomFormHasPetAndVerbose() throws Exception {
		client().get("/console/ops/jobs/groom-form").run().assertStatus(200)
			.assertContent().isContains("\"pet\"", "\"verbose\"", "\"checkbox\"", "idempotencyKey");
	}

	@Test void d06_photoIsSvg() throws Exception {
		client().get("/console/ops/jobs/groom-photo.svg").header("Accept", "*/*").run().assertStatus(200)
			.assertHeader("Content-Type").isContains("image/svg+xml").assertContent().isContains("<svg");
	}

	@Test void d07_pageShowsTheRequestedRun() throws Exception {
		var c = client();
		var id = groom(c, "{\"pet\":\"Rex\"}").getMap("row").getString("id");
		var lines = "/console/ops/jobs/juneau-console-output/" + id + "/lines";
		c.get("/console/ops/jobs").header("Accept", "text/html").run().assertStatus(200).assertContent().isContains(lines, lines.replace("/lines", "-file/lines"));
		c.get("/console/ops/jobs?run=nope").header("Accept", "text/html").run().assertStatus(200).assertContent().isContains("/juneau-console-output/");
	}

	@Test void d08_inlinedParamsCannotBreakOutOfTheScript() {
		assertString("{\"a\":\"<\\/script>\\u2028\\u2029\"}", JobsRest.scriptSafe("{\"a\":\"</script>\u2028\u2029\"}"));
	}
}
