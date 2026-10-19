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
import java.util.concurrent.*;
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
	 * The shared host with a 1.5 s jobs hard timeout, so the timeout test does not take 30 s.  It sits between the two
	 * timed cases with room on both sides: e04's hold (1 s after the effect) ends before it, and e05's job (0.5 s
	 * wait plus 2 s hold) runs well past it.
	 */
	public static class Host extends PetstoreConsoleFixture.Host {
		private static final long serialVersionUID = 1L;

		/** @return The test's hard timeout. */
		@Bean public JobsRest.HardTimeout jobsHardTimeout() {
			return new JobsRest.HardTimeout(HARD_TIMEOUT);
		}
	}

	static final Duration HARD_TIMEOUT = Duration.ofMillis(1500);

	/** {@link ClockedHost}'s key-ledger clock; only g11 uses that host, so only g11 moves it. */
	static final AtomicReference<Instant> KEY_NOW = new AtomicReference<>(Instant.now());

	/** A host whose row-key ledger runs on {@link #KEY_NOW}, so a test can age a key past its TTL. */
	public static class ClockedHost extends Host {
		private static final long serialVersionUID = 1L;

		/** @return The ledger's clock. */
		@Bean public JobsRest.KeyClock jobsKeyClock() {
			return new JobsRest.KeyClock(new Clock() {
				@Override public ZoneId getZone() { return ZoneOffset.UTC; }
				@Override public Clock withZone(ZoneId zone) { return this; }
				@Override public Instant instant() { return KEY_NOW.get(); }
			});
		}
	}

	private static MockRestClient client() {
		return MockRestClient.create(Host.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
	}

	private static JsonMap submit(MockRestClient c, String key) throws Exception {
		return new JsonMap(c.post("/console/ops/jobs/restock", "{\"perSpecies\":{\"DOG\":2,\"CAT\":1},\"holdMs\":0,\"idempotencyKey\":\"" + key + "\"}")
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
			+ "\",\"holdMs\":0,\"fields\":{\"DOG\":\"1\",\"CAT\":\"0\",\"BIRD\":\"\"}}").contentType("application/json").run().assertStatus(202);
		var ref = new JsonMap(c.post("/console/ops/jobs/restock", "{\"action\":\"restock\",\"targetId\":\"" + key + "\",\"idempotencyKey\":\"" + key
			+ "\",\"holdMs\":0,\"fields\":{\"DOG\":\"1\",\"CAT\":\"0\",\"BIRD\":\"\"}}").contentType("application/json").run().assertStatus(202).getContent().asString());
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
			.hasCardOrder("restock-help", "restock", "jobs", "species", "groom-output", "groom-file", "groom-runs")
			.hasCard("restock", "job-log")
			.hasCardKey("restock", "/submit/endpoint", "/console/ops/jobs/restock")
			.hasCardKey("restock", "/urlParam", "job");
	}

	@Test void e02_flatCardShapeIsAccepted() throws Exception {
		var c = client();
		var ref = post(c, "{\"DOG\":1,\"CAT\":null,\"slowMs\":0,\"holdMs\":0,\"idempotencyKey\":\"k-e02\"}");
		assertBean(await(c, ref.getString("jobId")), "state,result{outcome,message}", "DONE,{success,Restocked 1 pets}");
	}

	@Test void e03_cancelBeforeTheEffectIsCancelled() throws Exception {
		var c = client();
		var before = petCount(c);
		var slowMs = 300;
		var posted = System.nanoTime();
		var ref = post(c, "{\"DOG\":1,\"slowMs\":" + slowMs + ",\"idempotencyKey\":\"k-e03\"}");
		assertBean(cancel(c, ref), "outcome", "cancelled");
		assertBean(await(c, ref.getString("jobId")), "state,result{outcome}", "DONE,{cancelled}");
		// A worker that ignored the cancel would restock once its wait ran out, so look only after that point.
		var left = Duration.ofMillis(slowMs + 200).minusNanos(System.nanoTime() - posted).toMillis();
		if (left > 0)
			Thread.sleep(left);
		assertString(String.valueOf(before), petCount(c));
	}

	@Test void e04_cancelAfterTheEffectSaysSo() throws Exception {
		var c = client();
		var before = petCount(c);
		// The hold keeps the job running for 1 s after its effect, ending half a second short of the hard timeout, so the
		// cancel lands in the hold whatever the timing; the pet count shows when the effect has happened.
		var ref = post(c, "{\"DOG\":1,\"slowMs\":0,\"holdMs\":1000,\"idempotencyKey\":\"k-e04\"}");
		for (var i = 0; i < 200 && petCount(c) == before; i++)
			Thread.sleep(10);
		assertString(String.valueOf(before + 1), petCount(c));
		assertBean(cancel(c, ref), "outcome", "cancelled-after-effect");
	}

	@Test void e05_slowJobHitsTheHardTimeout() throws Exception {
		var c = client();
		// The job would run 2.5 s; the 1.5 s timeout lands in its hold.  This passes even against a scheduler that fires
		// its timeout task a little late; the early- and on-the-deadline firing it once hung on is pinned
		// deterministically by AsyncJobRegistry_Test's k group.
		var ref = post(c, "{\"DOG\":1,\"slowMs\":500,\"holdMs\":2000,\"idempotencyKey\":\"k-e05\"}");
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
		var ref = post(c, "{\"DOG\":2,\"slowMs\":200,\"holdMs\":0,\"idempotencyKey\":\"k-e07\"}");
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

	//-----------------------------------------------------------------------------------------------------------------
	// g - row jobs: ledger keys bound to one row
	//-----------------------------------------------------------------------------------------------------------------

	private static JsonMap key(MockRestClient c, String url) throws Exception {
		return new JsonMap(c.post(url, "{}").contentType("application/json").run().assertStatus(200).getContent().asString());
	}

	private static String envelope(String action, String targetId, String key, String fields) {
		return "{\"action\":\"" + action + "\",\"targetId\":\"" + targetId + "\",\"idempotencyKey\":\"" + key + "\",\"fields\":" + fields + "}";
	}

	private static String restockSpecies(MockRestClient c, String species, String body, int status) throws Exception {
		return c.post("/console/ops/jobs/species/" + species + "/restock", body).contentType("application/json").run().assertStatus(status).getContent().asString();
	}

	private static long available(MockRestClient c, String species) throws Exception {
		for (var row : new JsonList(c.get("/console/ops/jobs/species").run().assertStatus(200).getContent().asString()).elements(JsonMap.class))
			if (species.equals(row.getString("id")))
				return row.getLong("available");
		return fail("No species row '" + species + "'");
	}

	@Test void g01_speciesRowsDetailAndKey() throws Exception {
		var c = client();
		var rows = new JsonList(c.get("/console/ops/jobs/species").run().assertStatus(200).getContent().asString());
		assertSize(Species.values().length, rows);
		assertContains("{\"id\":\"DOG\",\"species\":\"DOG\",\"available\":" + available(c, "DOG") + "}", rows.toString());
		var detail = new JsonMap(c.get("/console/ops/jobs/species/DOG").run().assertStatus(200).getContent().asString());
		assertBean(detail, "contractVersion", "1");
		assertString(String.valueOf(available(c, "DOG")), detail.getMap("fields").getLong("available"));
		assertBean(key(c, "/console/ops/jobs/species/DOG/key"), "contractVersion,action,targetId", "1,restock-species,DOG");
		c.get("/console/ops/jobs/species/WOLF").run().assertStatus(404);
		c.post("/console/ops/jobs/species/WOLF/key", "{}").contentType("application/json").run().assertStatus(404);
		c.post("/console/ops/jobs/species/dog/key", "{}").contentType("application/json").run().assertStatus(404);
	}

	@Test void g02_rowSubmitRunsReplaysAndReturnsTheRow() throws Exception {
		var c = client();
		var before = available(c, "DOG");
		var k = key(c, "/console/ops/jobs/species/DOG/key").getString("idempotencyKey");
		var ref = new JsonMap(restockSpecies(c, "DOG", envelope("restock-species", "DOG", k, "{\"count\":2}"), 202));
		assertString(ref.getString("jobId"), new JsonMap(restockSpecies(c, "DOG", envelope("restock-species", "DOG", k, "{\"count\":2}"), 202)).getString("jobId"));
		var done = await(c, ref.getString("jobId"));
		assertBean(done, "state,result{outcome,message,row{id,available}}", "DONE,{success,Restocked 2 pets,{DOG," + (before + 2) + "}}");
		// Settled but not yet reaped: the replay still answers the same job, and nothing restocks twice.
		assertString(ref.getString("jobId"), new JsonMap(restockSpecies(c, "DOG", envelope("restock-species", "DOG", k, "{\"count\":2}"), 202)).getString("jobId"));
		assertString(String.valueOf(before + 2), available(c, "DOG"));
		assertNotNull(new JsonMap(c.get("/console/ops/jobs/species/DOG").run().assertStatus(200).getContent().asString()).getMap("fields").getString("lastRestock"));
	}

	@Test void g03_aKeyForOneRowIsRefusedForAnother() throws Exception {
		var c = client();
		var k = key(c, "/console/ops/jobs/species/DOG/key").getString("idempotencyKey");
		var refusal = "outcome,refusalCode,message";
		var expected = "refusal," + JobKeyLedger.REFUSAL_CODE + ",This key is not valid for this row.";
		// The DOG key, retargeted to CAT.
		assertBean(new JsonMap(restockSpecies(c, "CAT", envelope("restock-species", "CAT", k, "{\"count\":1}"), 409)), refusal, expected);
		// The DOG envelope, posted to CAT's path.
		assertBean(new JsonMap(restockSpecies(c, "CAT", envelope("restock-species", "DOG", k, "{\"count\":1}"), 409)), refusal, expected);
		// A key the ledger never issued (as an expired one, which the ledger has forgotten), and the wrong action.
		assertBean(new JsonMap(restockSpecies(c, "DOG", envelope("restock-species", "DOG", "forged-" + k, "{\"count\":1}"), 409)), refusal, expected);
		assertBean(new JsonMap(restockSpecies(c, "DOG", envelope("restock", "DOG", k, "{\"count\":1}"), 409)), refusal, expected);
		// The key is still good for its own row.
		await(c, new JsonMap(restockSpecies(c, "DOG", envelope("restock-species", "DOG", k, "{\"count\":1}"), 202)).getString("jobId"));
	}

	@Test void g04_theSpeciesComesFromTheTargetNeverTheFields() throws Exception {
		var c = client();
		var dog = available(c, "DOG");
		var cat = available(c, "CAT");
		var k = key(c, "/console/ops/jobs/species/DOG/key").getString("idempotencyKey");
		var ref = new JsonMap(restockSpecies(c, "DOG", envelope("restock-species", "DOG", k, "{\"count\":1,\"species\":\"CAT\",\"CAT\":5}"), 202));
		await(c, ref.getString("jobId"));
		assertString(String.valueOf(dog + 1), available(c, "DOG"));
		assertString(String.valueOf(cat), available(c, "CAT"));
	}

	@Test void g05_badCount400() throws Exception {
		var c = client();
		for (var count : List.of("0", "51", "1.5", "\"many\"", "null")) {
			var k = key(c, "/console/ops/jobs/species/DOG/key").getString("idempotencyKey");
			restockSpecies(c, "DOG", envelope("restock-species", "DOG", k, "{\"count\":" + count + "}"), 400);
		}
	}

	@Test void g06_regroomIsLedgerOnlyAndBoundToItsRun() throws Exception {
		var c = client();
		var a = groom(c, "{\"pet\":\"Rex\"}").getMap("row").getString("id");
		var b = groom(c, "{\"pet\":\"Max\"}").getMap("row").getString("id");
		assertBean(new JsonMap(c.get("/console/ops/jobs/groom-runs/" + a).run().assertStatus(200).getContent().asString()), "fields{id,pet}", "{" + a + ",Rex}");
		var k = key(c, "/console/ops/jobs/groom-runs/" + a + "/key").getString("idempotencyKey");
		var url = "/console/ops/jobs/groom-runs/" + a + "/regroom";
		var first = new JsonMap(c.post(url, envelope("regroom", a, k, "{}")).contentType("application/json").run().assertStatus(200).getContent().asString());
		assertBean(first, "outcome,message,row{pet}", "success,Grooming Rex again,{Rex}");
		var again = first.getMap("row").getString("id");
		assertNotEquals(a, again);
		var replay = new JsonMap(c.post(url, envelope("regroom", a, k, "{}")).contentType("application/json").run().assertStatus(200).getContent().asString());
		assertString(again, replay.getMap("row").getString("id"));
		// Run A's key for run B, a restock-species key for a regroom, and a self-targeted key: all refused.
		c.post("/console/ops/jobs/groom-runs/" + b + "/regroom", envelope("regroom", b, k, "{}")).contentType("application/json").run().assertStatus(409);
		var species = key(c, "/console/ops/jobs/species/DOG/key").getString("idempotencyKey");
		c.post(url, envelope("restock-species", a, species, "{}")).contentType("application/json").run().assertStatus(409)
			.assertContent().isContains(JobKeyLedger.REFUSAL_CODE);
		c.post(url, envelope("regroom", a, species, "{}")).contentType("application/json").run().assertStatus(409);
		c.post(url, envelope("regroom", "k-g06", "k-g06", "{}")).contentType("application/json").run().assertStatus(409);
		c.get("/console/ops/jobs/groom-runs/nope").run().assertStatus(404);
		c.post("/console/ops/jobs/groom-runs/nope/key", "{}").contentType("application/json").run().assertStatus(404);
	}

	@Test void g07_aReapedJobsReplayIsSpentNeverASecondJob() {
		// The registry reaps a settled job RETENTION after it ends; a registry that no longer holds the claimed job is
		// that state.  The replay must refuse, not start the job again.
		try (var registry = new AsyncJobRegistry(Duration.ofSeconds(5))) {
			var r = JobsRest.replay("reaped-job", registry);
			assertBean(r, "outcome,refusalCode", "refusal," + JobKeyLedger.SPENT_CODE);
			assertString("0", registry.size());
			var live = registry.create();
			assertBean(JobsRest.replay(live.id(), registry), "jobId", live.id());
			assertString("1", registry.size());
		}
	}

	@Test void g09_aDoubleSubmitOfOneKeyRestocksOnce() throws Exception {
		// Two first submits of one key, released together: one starts the job, the other replays it.  Several rounds,
		// since a lost race only shows on some of them; each round awaits its job, so the job limit never refuses.
		var c = client();
		for (var round = 0; round < 3; round++) {
			var before = available(c, "BIRD");
			var k = key(c, "/console/ops/jobs/species/BIRD/key").getString("idempotencyKey");
			var body = envelope("restock-species", "BIRD", k, "{\"count\":3}");
			var go = new CountDownLatch(1);
			var pool = Executors.newFixedThreadPool(2);
			try {
				var a = pool.submit(() -> { go.await(); return new JsonMap(restockSpecies(c, "BIRD", body, 202)).getString("jobId"); });
				var b = pool.submit(() -> { go.await(); return new JsonMap(restockSpecies(c, "BIRD", body, 202)).getString("jobId"); });
				go.countDown();
				var id = a.get(10, TimeUnit.SECONDS);
				assertString(id, b.get(10, TimeUnit.SECONDS));
				assertBean(await(c, id), "state,result{row{available}}", "DONE,{{" + (before + 3) + "}}");
				assertString(String.valueOf(before + 3), available(c, "BIRD"));
			} finally {
				pool.shutdownNow();
			}
		}
	}

	@Test void g10_aRegroomReplayAnswersItsRunUntilEvictedThenIsSpent() throws Exception {
		var c = client();
		var a = groom(c, "{\"pet\":\"Rex\"}").getMap("row").getString("id");
		var k = key(c, "/console/ops/jobs/groom-runs/" + a + "/key").getString("idempotencyKey");
		var url = "/console/ops/jobs/groom-runs/" + a + "/regroom";
		var again = new JsonMap(c.post(url, envelope("regroom", a, k, "{}")).contentType("application/json").run().assertStatus(200).getContent().asString())
			.getMap("row").getString("id");
		// The run list evicts oldest first: the target goes before the run its regroom started.  The replay still
		// answers that run.
		while (c.get("/console/ops/jobs/groom-runs/" + a).run().getStatusCode() != 404)
			groom(c, "{\"pet\":\"Max\"}");
		c.get("/console/ops/jobs/groom-runs/" + again).run().assertStatus(200);
		var replay = new JsonMap(c.post(url, envelope("regroom", a, k, "{}")).contentType("application/json").run().assertStatus(200).getContent().asString());
		assertString(again, replay.getMap("row").getString("id"));
		// Once that run is evicted too, the key is spent: never a second run.
		while (c.get("/console/ops/jobs/groom-runs/" + again).run().getStatusCode() != 404)
			groom(c, "{\"pet\":\"Max\"}");
		var spent = new JsonMap(c.post(url, envelope("regroom", a, k, "{}")).contentType("application/json").run().assertStatus(409).getContent().asString());
		assertBean(spent, "outcome,refusalCode", "refusal," + JobKeyLedger.SPENT_CODE);
	}

	@Test void g11_anExpiredKeyIs409() throws Exception {
		var c = MockRestClient.create(ClockedHost.class).noTrace().ignoreErrors().header("Accept", "application/json").build();
		var k = key(c, "/console/ops/jobs/species/FISH/key").getString("idempotencyKey");
		KEY_NOW.updateAndGet(x -> x.plus(JobKeyLedger.DEFAULT_TTL).plusSeconds(1));
		assertBean(new JsonMap(restockSpecies(c, "FISH", envelope("restock-species", "FISH", k, "{\"count\":1}"), 409)),
			"outcome,refusalCode", "refusal," + JobKeyLedger.REFUSAL_CODE);
		// A key issued on the moved clock is good on it.
		var fresh = key(c, "/console/ops/jobs/species/FISH/key").getString("idempotencyKey");
		await(c, new JsonMap(restockSpecies(c, "FISH", envelope("restock-species", "FISH", fresh, "{\"count\":1}"), 202)).getString("jobId"));
	}

	@Test void g08_theKeyEndpointIsPostOnly() throws Exception {
		var c = client();
		var status = c.get("/console/ops/jobs/species/DOG/key").run().getStatusCode();
		assertTrue(status == 404 || status == 405, "GET on the key endpoint must not be served; got " + status);
		status = c.get("/console/ops/jobs/groom-runs/x/key").run().getStatusCode();
		assertTrue(status == 404 || status == 405, "GET on the key endpoint must not be served; got " + status);
	}
}
