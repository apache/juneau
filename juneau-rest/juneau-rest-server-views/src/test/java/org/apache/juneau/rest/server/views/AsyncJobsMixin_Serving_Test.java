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

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.http.message.*;
import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Serving test for {@link AsyncJobsMixin} &mdash; the SSE progress stream and cancel endpoints.
 *
 * <p>
 * Drives the two endpoints through a {@link MockRestClient}: a fresh attach is live-only (an ended job sends only its
 * terminal {@code result} event); a {@code Last-Event-ID} header (or, failing a non-empty one, a {@code lastEventId}
 * query parameter) resumes from that position, with each progress event carrying {@code id: i+1}; a bad or
 * past-the-end resume value is a {@code 400}; an unknown capability id is a {@code 404} (enumeration-resistant);
 * a job already at its subscriber cap is a {@code 429}; and cancel returns the terminal
 * {@code cancelled} / {@code cancelled-after-effect} outcome.
 *
 * <p>
 * Most SSE assertions complete the job <b>before</b> subscribing, so the stream drains in one deterministic pass.  The
 * running-job cases run the stream GET on a background thread and only produce progress once
 * {@link AsyncJob#subscriberCount()} shows the subscriber attached: the stream fixes its cursor before it takes the
 * subscriber slot, so from that point on the outcome is deterministic.
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

	/** Starts a job, buffers {@code e0..e(n-1)} and completes it. */
	private static String endedJob(int n) throws Exception {
		var jobId = startJob();
		var job = R.lastJob;
		for (var i = 0; i < n; i++)
			job.progress("e" + i);
		job.complete(ActionResult.success(Map.of("id", "INC-1")));
		return jobId;
	}

	/** GETs the stream with an optional Last-Event-ID header and lastEventId query value (null = not sent). */
	private static org.apache.juneau.rest.client.classic.RestResponse stream(String jobId, String header, String query) throws Exception {
		var req = c.get(AsyncJobsMixin.streamPath(jobId)).header("Accept", "text/event-stream");
		if (header != null)
			req.header(new BasicHeader("Last-Event-ID", header));
		if (query != null)
			req.queryData("lastEventId", query);
		return req.run();
	}

	private static List<SseEvent> parse(String body) throws IOException {
		try (var r = new SseEventReader(new StringReader(body))) {
			return r.toList();
		}
	}

	/** Each event as {@code type#id=data} ({@code #id} omitted when the event has no id), result data dropped. */
	private static List<String> events(String body) throws IOException {
		return parse(body).stream()
			.map(e -> e.getEvent() + (e.getId() == null ? "" : "#" + e.getId()) + ("result".equals(e.getEvent()) ? "" : "=" + e.getData()))
			.toList();
	}

	private static List<String> streamEvents(String jobId, String header, String query) throws Exception {
		return events(stream(jobId, header, query).assertStatus(200).getContent().asString());
	}

	static List<String> badValues() {
		return List.of("4", "-1", "+1", "1.0", "1e2", "0x2", "abc", "\n2", "12345678901", "2147483648");
	}

	/** Waits (bounded by the caller's preemptive timeout) until the job has {@code n} subscribers. */
	private static void awaitSubscribers(AsyncJob job, int n) throws InterruptedException {
		while (job.subscriberCount() != n)
			Thread.sleep(5);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The job-started envelope is an AsyncJobRef with capability URLs embedding the job id (no RowAction field)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_startReturnsAsyncJobRefWithCapabilityUrls() throws Exception {
		var body = c.post("/start", "").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		var ref = Json.to(body, Map.class);
		var jobId = String.valueOf(ref.get("jobId"));
		assertTrue(jobId.matches("[0-9a-f]{64}"), jobId);
		// streamUrl/cancelUrl are @Uri-annotated: Juneau's serializer resolves the "servlet:"
		// pseudo-scheme against the request's UriContext before the value ever reaches the wire, so under this
		// root-mounted MockRestClient (no context/servlet path) the resolved, browser-usable form is identical
		// to the un-prefixed mixin path - see a02 below for the literal-scheme regression guard.
		assertEquals(AsyncJobsMixin.streamPath(jobId), ref.get("streamUrl"));
		assertEquals(AsyncJobsMixin.cancelPath(jobId), ref.get("cancelUrl"));
		// The pointer carries NO ActionResult outcome discriminator - the two 2xx shapes are disjoint.
		assertNull(ref.get("outcome"));
	}

	@Test void a02_streamUrlAndCancelUrl_areResolved_notLiteralServletScheme() throws Exception {
		// AsyncJobRef.streamUrl/cancelUrl used to be plain, un-annotated Strings, so
		// no serializer ever resolved the "servlet:" pseudo-scheme - the browser EventSource received the literal
		// "servlet:/juneau-jobs/<id>/stream" and silently rejected the unsupported scheme (Hank's report). @Uri
		// opts these fields into the same per-request URI resolution ViewsMixin's asset URLs already rely on
		// (SerializedPojoProcessor wires req.getUriContext() into every serializer session, not just HTML's).
		var body = c.post("/start", "").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		var ref = Json.to(body, Map.class);
		var streamUrl = String.valueOf(ref.get("streamUrl"));
		var cancelUrl = String.valueOf(ref.get("cancelUrl"));
		assertFalse(streamUrl.startsWith("servlet:"), streamUrl);
		assertFalse(cancelUrl.startsWith("servlet:"), cancelUrl);
		// Root-relative ("/...") or absolute (has a scheme+authority) are both browser-usable EventSource targets.
		assertTrue(streamUrl.startsWith("/") || streamUrl.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"), streamUrl);
		assertTrue(cancelUrl.startsWith("/") || cancelUrl.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"), cancelUrl);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) The stream: live-only fresh attach, id-tagged progress, resume from Last-Event-ID / lastEventId, one result
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_freshAttachToEndedJob_sendsOnlyResult() throws Exception {
		var jobId = startJob();
		var job = R.lastJob;
		job.progress("step 1 of 2");
		job.progress("step 2 of 2");
		job.complete(ActionResult.success(Map.of("id", "INC-1", "status", "acknowledged")));

		var resp = stream(jobId, null, null).assertStatus(200);
		resp.assertHeader("Content-Type").isContains("text/event-stream");
		var body = resp.getContent().asString();
		assertEquals(List.of("result"), events(body), body);
		assertFalse(body.contains("step 1 of 2"), body);
		assertTrue(body.contains("\"outcome\":\"success\""), body);
		assertTrue(body.contains("INC-1"), body);
	}

	@Test void b02_streamOfCancelledJobCarriesCancelledResult() throws Exception {
		var jobId = startJob();
		R.lastJob.progress("working");
		R.lastJob.cancel();
		var body = c.get(AsyncJobsMixin.streamPath(jobId)).header("Accept", "text/event-stream").run()
			.assertStatus(200).getContent().asString();
		assertEquals(List.of("result"), events(body), body);   // fresh attach: the result only, no replay
		assertFalse(body.contains("working"), body);
		assertTrue(body.contains("\"outcome\":\"cancelled\""), body);
	}

	@Test void b03_header1_replaysIds2and3_thenResultWithoutId() throws Exception {
		var jobId = endedJob(3);
		var body = stream(jobId, "1", null).assertStatus(200).getContent().asString();
		assertEquals(List.of("progress#2=e1", "progress#3=e2", "result"), events(body), body);
		var result = parse(body).get(2);
		assertNull(result.getId(), body);
		assertTrue(result.getData().contains("\"outcome\":\"success\""), body);
		assertFalse(body.contains("id: 1\n"), body);
	}

	@Test void b04_queryZero_replaysIds1to3() throws Exception {
		assertEquals(List.of("progress#1=e0", "progress#2=e1", "progress#3=e2", "result"), streamEvents(endedJob(3), null, "0"));
	}

	@Test void b05_headerBeatsQuery() throws Exception {
		assertEquals(List.of("progress#3=e2", "result"), streamEvents(endedJob(3), "2", "0"));
	}

	@Test void b06_emptyHeader_fallsBackToQuery() throws Exception {
		var jobId = endedJob(3);
		assertEquals(List.of("progress#1=e0", "progress#2=e1", "progress#3=e2", "result"), streamEvents(jobId, "", "0"));
		assertEquals(List.of("progress#1=e0", "progress#2=e1", "progress#3=e2", "result"), streamEvents(jobId, " ", "0"));
		// An empty header with no query is a fresh attach.
		assertEquals(List.of("result"), streamEvents(jobId, "", null));
		// An empty query is no value at all.
		assertEquals(List.of("result"), streamEvents(jobId, null, ""));
	}

	@Test void b07_valueAtEnd_sendsOnlyResult() throws Exception {
		var jobId = endedJob(3);
		assertEquals(List.of("result"), streamEvents(jobId, "3", null));
		assertEquals(List.of("result"), streamEvents(jobId, null, "3"));
	}

	@ParameterizedTest
	@MethodSource("badValues")
	void b08_badValue_is400_header(String value) throws Exception {
		var jobId = endedJob(3);
		assertEquals("", stream(jobId, value, null).assertStatus(400).getContent().asString());
		assertEquals(0, R.lastJob.subscriberCount());
	}

	@ParameterizedTest
	@MethodSource("badValues")
	void b08_badValue_is400_query(String value) throws Exception {
		var jobId = endedJob(3);
		assertEquals("", stream(jobId, null, value).assertStatus(400).getContent().asString());
		// A bad query is still bad when a valid header is absent, and a bad header beats a valid query.
		stream(jobId, value, "0").assertStatus(400);
		assertEquals(0, R.lastJob.subscriberCount());
	}

	@Test void b09_pastEnd_is400() throws Exception {
		var jobId = endedJob(3);
		stream(jobId, "4", null).assertStatus(400);
		stream(jobId, null, "4").assertStatus(400);
		stream(jobId, null, "999").assertStatus(400);
		stream(endedJob(0), null, "1").assertStatus(400);
	}

	@Test void b10_400_doesNotConsumeASubscriberSlot() throws Exception {
		var jobId = endedJob(3);
		var job = R.lastJob;
		// Two 400s on a job with a cap of two would otherwise exhaust it.
		stream(jobId, "abc", null).assertStatus(400);
		stream(jobId, null, "99").assertStatus(400);
		assertEquals(0, job.subscriberCount());
		assertEquals(List.of("progress#3=e2", "result"), streamEvents(jobId, "2", null));
		assertEquals(0, job.subscriberCount());
	}

	@Test void b11_unknownJob_withBadValue_is404() throws Exception {
		var unknown = "00000000000000000000000000000000";
		stream(unknown, "abc", null).assertStatus(404);
		stream(unknown, null, "999").assertStatus(404);
	}

	@Test void b11b_badValue_beats429() throws Exception {
		var jobId = endedJob(3);
		var job = R.lastJob;
		assertTrue(job.acquireSubscriber());
		assertTrue(job.acquireSubscriber());
		try {
			stream(jobId, null, "abc").assertStatus(400);
			stream(jobId, null, "0").assertStatus(429);
		} finally {
			job.releaseSubscriber();
			job.releaseSubscriber();
		}
	}

	@Test void b12_idIsIndexPlusOne_whateverTheStartCursor() throws Exception {
		var n = 3;
		var jobId = endedJob(n);
		var buffered = R.lastJob.eventsFrom(0);
		for (var k = 0; k <= n; k++) {
			var body = stream(jobId, String.valueOf(k), null).assertStatus(200).getContent().asString();
			var progress = parse(body).stream().filter(e -> "progress".equals(e.getEvent())).toList();
			assertEquals(n - k, progress.size(), body);
			for (var i = 0; i < progress.size(); i++) {
				var e = progress.get(i);
				var id = Integer.parseInt(e.getId());
				assertEquals(k + i + 1, id, body);
				assertEquals(buffered.get(id - 1), e.getData(), body);
			}
		}
	}

	@Test void b13_freshAttachToRunningJob_isLiveOnly() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			var jobId = startJob();
			var job = R.lastJob;
			job.progress("early");
			var pool = Executors.newSingleThreadExecutor();
			try {
				var a = pool.submit(() -> streamEvents(jobId, null, null));
				awaitSubscribers(job, 1);
				job.progress("live");
				job.complete(ActionResult.success(null));
				assertEquals(List.of("progress#2=live", "result"), a.get());
			} finally {
				pool.shutdownNow();
			}
			assertEquals(0, job.subscriberCount());
		});
	}

	@Test void b14_twoSubscribersAtDifferentCursors() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			var jobId = startJob();
			var job = R.lastJob;
			job.progress("early");
			var pool = Executors.newFixedThreadPool(2);
			try {
				var a = pool.submit(() -> streamEvents(jobId, null, null));
				awaitSubscribers(job, 1);
				var b = pool.submit(() -> streamEvents(jobId, null, "0"));
				awaitSubscribers(job, 2);
				job.progress("live");
				job.complete(ActionResult.success(null));
				assertEquals(List.of("progress#2=live", "result"), a.get());
				assertEquals(List.of("progress#1=early", "progress#2=live", "result"), b.get());
			} finally {
				pool.shutdownNow();
			}
			assertEquals(0, job.subscriberCount());
		});
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Capability gating: an unknown id is 404 (enumeration-resistant)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_unknownJobIdStreamIs404() throws Exception {
		c.get(AsyncJobsMixin.streamPath("00000000000000000000000000000000")).header("Accept", "text/event-stream")
			.run().assertStatus(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Subscriber cap: a job already at its cap answers 429
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
	// e) Cancel returns the terminal outcome, distinguishing cancelled from cancelled-after-effect
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
