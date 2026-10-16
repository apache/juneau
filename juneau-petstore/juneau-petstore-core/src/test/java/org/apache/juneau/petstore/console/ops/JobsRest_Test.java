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

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S2925", // Polls an asynchronous job; the REST API has no completion hook to wait on.
	"resource" // MockRestClient is a no-op close.
})
class JobsRest_Test extends TestBase {

	/**
	 * The shared host with a 12 s jobs hard timeout, so the timeout test does not take 30 s.  It sits between the two
	 * timed cases with room on both sides: e04's hold (6 s after the effect) ends well before it, and e05's job (10 s
	 * wait plus 10 s hold) runs well past it.
	 */
	public static class Host extends PetstoreConsoleFixture.Host {
		private static final long serialVersionUID = 1L;

		/** @return The test's hard timeout. */
		@Bean public JobsRest.HardTimeout jobsHardTimeout() {
			return new JobsRest.HardTimeout(HARD_TIMEOUT);
		}
	}

	static final Duration HARD_TIMEOUT = Duration.ofSeconds(12);

	private static MockRestClient client() {
		return MockRestClient.create(Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	private static JsonMap submit(MockRestClient c, String key) throws Exception {
		return new JsonMap(c.post("/console/ops/jobs/restock", "{\"perSpecies\":{\"DOG\":2,\"CAT\":1},\"idempotencyKey\":\"" + key + "\"}")
			.contentType("application/json").run().assertStatus(202).getContent().asString());
	}

	private static JsonMap await(MockRestClient c, String id) throws Exception {
		var deadline = System.nanoTime() + HARD_TIMEOUT.plusSeconds(10).toNanos();
		while (System.nanoTime() < deadline) {
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
		assertNotContains(() -> "no 'Fishs' typo", "Fishs", body);
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
			var body = client().post("/console/ops/jobs/restock", "{\"perSpecies\":{\"DOG\":1},\"idempotencyKey\":\"k-b04\"}").contentType("application/json").run()
				.assertStatus(429).getContent().asString();
			// A JSON refusal with a message, so the job-log card shows the reason rather than "HTTP 429".
			assertBean(new JsonMap(body), "outcome,refusalCode,message", "refusal,app:job-limit,Job limit reached; '0' running");
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
		var bytes = c.get("/console/ops/jobs/juneau-terminal/" + id + "/bytes?from=0").run().assertStatus(200);
		bytes.assertHeader("Content-Type").isContains("application/octet-stream");
		bytes.assertHeader("Term-Cols").is(String.valueOf(GroomRuns.COLS));
		bytes.assertHeader("Term-Done").is("false");
		bytes.assertContent().isContains("verbose: inspected coat patch 1 of ");
		c.get("/console/ops/jobs/juneau-terminal/" + id + "-file/bytes").run().assertStatus(200).assertHeader("Term-Rows").is(String.valueOf(GroomRuns.ROWS));
		c.get("/console/ops/jobs/juneau-terminal/" + id + "/raw").run().assertStatus(200).assertHeader("Content-Disposition").isContains("attachment");
		c.get("/console/ops/jobs/juneau-terminal/nope/bytes").run().assertStatus(404);
	}

	@Test void d05_groomFormHasPetAndVerbose() throws Exception {
		client().get("/console/ops/jobs/groom-form").run().assertStatus(200)
			.assertContent().isContains("\"pet\"", "\"verbose\"", "\"checkbox\"", "idempotencyKey");
	}

	@Test void d07_pageShowsTheRequestedRun() throws Exception {
		var c = client();
		var id = groom(c, "{\"pet\":\"Rex\"}").getMap("row").getString("id");
		var bytes = "/console/ops/jobs/juneau-terminal/" + id + "/bytes";
		c.get("/console/ops/jobs?run=" + id).header("Accept", "text/html").run().assertStatus(200).assertContent().isContains(bytes, bytes.replace("/bytes", "-file/bytes"));
		c.get("/console/ops/jobs?run=nope").header("Accept", "text/html").run().assertStatus(200).assertContent().isContains("/juneau-terminal/");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e - the job-log card and its slow, cancellable, timed-out restocks
	//-----------------------------------------------------------------------------------------------------------------

	private static JsonMap post(MockRestClient c, String body) throws Exception {
		return new JsonMap(c.post("/console/ops/jobs/restock", body).contentType("application/json").run().assertStatus(202).getContent().asString());
	}

	private static JsonMap cancel(MockRestClient c, JsonMap ref) throws Exception {
		return new JsonMap(c.post(ref.getString("cancelUrl"), "").run().assertStatus(200).getContent().asString());
	}

	@Test void e01_restockCardIsAJobLog() throws Exception {
		assertPage(PetstoreConsoleFixture.page(client(), "/console/ops/jobs"))
			.hasCardOrder("restock-help", "restock", "jobs", "groom-output", "groom-file", "groom-runs")
			.hasCard("restock", "job-log")
			.hasCardKey("restock", "/submit/endpoint", "/console/ops/jobs/restock")
			.hasCardKey("restock", "/urlParam", "job");
	}

	@Test void e02_flatCardShapeIsAccepted() throws Exception {
		var c = client();
		var ref = post(c, "{\"DOG\":1,\"CAT\":null,\"slowMs\":0,\"idempotencyKey\":\"k-e02\"}");
		assertBean(await(c, ref.getString("jobId")), "state,result{outcome,message}", "DONE,{success,Restocked 1 pets}");
	}

	@Test void e03_cancelBeforeTheEffectIsCancelled() throws Exception {
		var c = client();
		var before = petCount(c);
		var slowMs = 1500;
		var posted = System.nanoTime();
		var ref = post(c, "{\"DOG\":1,\"slowMs\":" + slowMs + ",\"idempotencyKey\":\"k-e03\"}");
		assertBean(cancel(c, ref), "outcome", "cancelled");
		assertBean(await(c, ref.getString("jobId")), "state,result{outcome}", "DONE,{cancelled}");
		// A worker that ignored the cancel would restock once its wait ran out, so look only after that point.
		var left = Duration.ofMillis(slowMs + 500).minusNanos(System.nanoTime() - posted).toMillis();
		if (left > 0)
			Thread.sleep(left);
		assertString(String.valueOf(before), petCount(c));
	}

	@Test void e04_cancelAfterTheEffectSaysSo() throws Exception {
		var c = client();
		var before = petCount(c);
		// The hold keeps the job running for 6 s after its effect, ending 6 s short of the hard timeout, so the cancel
		// lands in the hold whatever the timing; the pet count shows when the effect has happened.
		var ref = post(c, "{\"DOG\":1,\"slowMs\":0,\"holdMs\":6000,\"idempotencyKey\":\"k-e04\"}");
		for (var i = 0; i < 200 && petCount(c) == before; i++)
			Thread.sleep(10);
		assertString(String.valueOf(before + 1), petCount(c));
		assertBean(cancel(c, ref), "outcome", "cancelled-after-effect");
	}

	@Test void e05_slowJobHitsTheHardTimeout() throws Exception {
		var c = client();
		// The job would run 20 s; the 12 s timeout lands in its hold.  This passes even against a scheduler that fires
		// its timeout task a little late; the early- and on-the-deadline firing it once hung on is pinned
		// deterministically by AsyncJobRegistry_Test's k group.
		var ref = post(c, "{\"DOG\":1,\"slowMs\":10000,\"holdMs\":10000,\"idempotencyKey\":\"k-e05\"}");
		assertBean(await(c, ref.getString("jobId")), "state,result{outcome,message}", "DONE,{unknown,app:hard-timeout-exceeded}");
	}

	@Test void e06_badSlowMs400() throws Exception {
		var c = client();
		c.post("/console/ops/jobs/restock", "{\"DOG\":1,\"slowMs\":20000,\"idempotencyKey\":\"k-e06\"}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Invalid slowMs '20000'; must be a whole number 0-10000");
		c.post("/console/ops/jobs/restock", "{\"DOG\":1,\"slowMs\":\"soon\",\"idempotencyKey\":\"k-e06b\"}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Invalid slowMs 'soon'; must be a whole number 0-10000");
		// A decimal is rejected rather than truncated: the value is a whole number of milliseconds.
		c.post("/console/ops/jobs/restock", "{\"DOG\":1,\"slowMs\":1.5,\"idempotencyKey\":\"k-e06c\"}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Invalid slowMs '1.5'; must be a whole number 0-10000");
	}

	@Test void e07_progressLinesAreStreamed() throws Exception {
		var c = client();
		var ref = post(c, "{\"DOG\":2,\"slowMs\":200,\"idempotencyKey\":\"k-e07\"}");
		await(c, ref.getString("jobId"));
		c.get(ref.getString("streamUrl") + "?lastEventId=0").header("Accept", "text/event-stream").run().assertStatus(200)
			.assertContent().isContains("event: progress", "Waiting 200 ms", "Restocking 2 pets", "Added DOG #", "event: result");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f - the restock steps
	//-----------------------------------------------------------------------------------------------------------------

	private static final JobsRest.Restock ONE_DOG = new JobsRest.Restock(Map.of(Species.DOG, 1), "k-f", 0, 0);

	@Test void f01_jobSettledBeforeTheEffectDoesNotRestock() {
		// A cancel or timeout that lands after the slow wait but before the effect starts must win.
		try (var r = new AsyncJobRegistry(Duration.ofSeconds(5))) {
			var job = r.create();
			job.cancel();
			var calls = new AtomicInteger();
			JobsRest.run(job, ONE_DOG, () -> { calls.incrementAndGet(); return List.of(); });
			assertBean(job.result(), "outcome", "cancelled");
			assertString("0", calls.get());
		}
	}

	@Test void f02_holdMsDefaultsAndIsBounded() throws Exception {
		assertBean(JobsRest.Restock.of(JsonMap.of("DOG", 1)), "slowMs,holdMs", "0,1000");
		assertBean(JobsRest.Restock.of(JsonMap.of("DOG", 1, "holdMs", "0")), "holdMs", "0");
		var c = client();
		c.post("/console/ops/jobs/restock", "{\"DOG\":1,\"holdMs\":20000,\"idempotencyKey\":\"k-f02\"}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Invalid holdMs '20000'; must be a whole number 0-10000");
		c.post("/console/ops/jobs/restock", "{\"DOG\":1,\"holdMs\":2.5,\"idempotencyKey\":\"k-f02b\"}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Invalid holdMs '2.5'; must be a whole number 0-10000");
	}
}
