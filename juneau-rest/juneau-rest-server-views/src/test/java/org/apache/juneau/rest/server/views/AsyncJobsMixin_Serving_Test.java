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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Serving test for {@link AsyncJobsMixin} &mdash; the SSE progress stream and cancel endpoints (design doc §6.3;
 * {@code TODO-425}).
 *
 * <p>
 * Drives the two endpoints through a {@link MockRestClient}: the stream replays a completed job's buffered progress
 * plus its terminal {@code result} event (the page-reload re-attach path, Q3); an unknown capability id is a
 * {@code 404} (enumeration-resistant, HIGH-4); a job already at its subscriber cap is a {@code 429} (MED-6); and
 * cancel returns the terminal {@code cancelled} / {@code cancelled-after-effect} outcome (Q4).
 *
 * <p>
 * Each SSE assertion completes the job <b>before</b> subscribing, so the stream drains in one deterministic pass
 * with no wall-clock waits (a still-running job would legitimately hold the stream open until its hard timeout).
 */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class AsyncJobsMixin_Serving_Test extends TestBase {

	@Rest
	public static class R extends BasicRestServlet implements AsyncJobsMixin {
		private static final long serialVersionUID = 1L;

		// No background scheduler: the serving tests do not exercise wall-clock timeout, so nothing runs off-thread.
		static final AsyncJobRegistry REGISTRY = new AsyncJobRegistry(Clock.systemUTC(), Duration.ofSeconds(120),
			AsyncJobRegistry.MAX_OUTPUT_BYTES, AsyncJobRegistry.MAX_SUBSCRIBERS_PER_JOB, null);

		static volatile AsyncJob lastJob;

		@Override public AsyncJobRegistry asyncJobRegistry() { return REGISTRY; }

		/** Test-only: starts a job (as a consumer's async row-action POST would) and returns its AsyncJobRef pointer. */
		@RestPost(path="/start") public AsyncJobRef start() {
			var job = REGISTRY.create();
			lastJob = job;
			return AsyncJobRef.of(job);
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(R.class);

	private static String startJob() throws Exception {
		var body = c.post("/start", "").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		return String.valueOf(Json.to(body, Map.class).get("jobId"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The job-started envelope is an AsyncJobRef with capability URLs embedding the job id (no RowAction field)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_startReturnsAsyncJobRefWithCapabilityUrls() throws Exception {
		var body = c.post("/start", "").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		var ref = Json.to(body, Map.class);
		var jobId = String.valueOf(ref.get("jobId"));
		assertTrue(jobId.matches("[0-9a-f]{64}"), jobId);
		assertEquals("servlet:" + AsyncJobsMixin.streamPath(jobId), ref.get("streamUrl"));
		assertEquals("servlet:" + AsyncJobsMixin.cancelPath(jobId), ref.get("cancelUrl"));
		// The pointer carries NO ActionResult outcome discriminator - the two 2xx shapes are disjoint.
		assertNull(ref.get("outcome"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) The stream replays buffered progress + one terminal result event (re-attach, Q3)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_streamReplaysProgressThenResult() throws Exception {
		var jobId = startJob();
		var job = R.lastJob;
		job.progress("step 1 of 2");
		job.progress("step 2 of 2");
		job.complete(ActionResult.success(Map.of("id", "INC-1", "status", "acknowledged")));

		var resp = c.get(AsyncJobsMixin.streamPath(jobId)).header("Accept", "text/event-stream").run().assertStatus(200);
		resp.assertHeader("Content-Type").isContains("text/event-stream");
		var body = resp.getContent().asString();
		assertTrue(body.contains("event: progress"), body);
		assertTrue(body.contains("data: step 1 of 2"), body);
		assertTrue(body.contains("data: step 2 of 2"), body);
		assertTrue(body.contains("event: result"), body);
		assertTrue(body.contains("\"outcome\":\"success\""), body);
		assertTrue(body.contains("INC-1"), body);
	}

	@Test void b02_streamOfCancelledJobCarriesCancelledResult() throws Exception {
		var jobId = startJob();
		R.lastJob.progress("working");
		R.lastJob.cancel();
		var body = c.get(AsyncJobsMixin.streamPath(jobId)).header("Accept", "text/event-stream").run()
			.assertStatus(200).getContent().asString();
		assertTrue(body.contains("event: result"), body);
		assertTrue(body.contains("\"outcome\":\"cancelled\""), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Capability gating: an unknown id is 404 (enumeration-resistant, HIGH-4)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_unknownJobIdStreamIs404() throws Exception {
		c.get(AsyncJobsMixin.streamPath("00000000000000000000000000000000")).header("Accept", "text/event-stream")
			.run().assertStatus(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Subscriber cap: a job already at its cap answers 429 (MED-6)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_subscriberCapReturns429() throws Exception {
		var jobId = startJob();
		var job = R.lastJob;
		// Pre-occupy both subscriber slots so the endpoint's own acquire fails; complete so the stream would drain.
		assertTrue(job.acquireSubscriber());
		assertTrue(job.acquireSubscriber());
		job.complete(ActionResult.success(null));
		c.get(AsyncJobsMixin.streamPath(jobId)).header("Accept", "text/event-stream").run().assertStatus(429);
		job.releaseSubscriber();
		job.releaseSubscriber();
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Cancel returns the terminal outcome, distinguishing cancelled from cancelled-after-effect (Q4)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_cancelReturnsCancelled() throws Exception {
		var jobId = startJob();
		var body = c.post(AsyncJobsMixin.cancelPath(jobId), "").header("Accept", "application/json").run()
			.assertStatus(200).getContent().asString();
		assertTrue(body.contains("\"outcome\":\"cancelled\""), body);
		assertFalse(body.contains("cancelled-after-effect"), body);
	}

	@Test void e02_cancelAfterEffectReturnsCancelledAfterEffect() throws Exception {
		var jobId = startJob();
		R.lastJob.markEffectStarted();
		var body = c.post(AsyncJobsMixin.cancelPath(jobId), "").header("Accept", "application/json").run()
			.assertStatus(200).getContent().asString();
		assertTrue(body.contains("\"outcome\":\"cancelled-after-effect\""), body);
	}

	@Test void e03_cancelUnknownJobIs404AndNamedRefusal() throws Exception {
		var body = c.post(AsyncJobsMixin.cancelPath("00000000000000000000000000000000"), "")
			.header("Accept", "application/json").run().assertStatus(404).getContent().asString();
		assertTrue(body.contains("\"outcome\":\"refusal\""), body);
		assertTrue(body.contains("app:unknown-job"), body);
	}
}
