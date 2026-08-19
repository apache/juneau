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

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Always-on source-shape coverage for the {@code juneau-views.js} async-job + SSE-streaming plumbing (TODO-425).
 * Mirrors {@link ViewsJs_ModalResult_Test}'s served-script substring style: proves the load-bearing pieces of the
 * async contract are present in the shipped asset without booting a browser (the behavioral proof lives in the opt-in
 * {@code AsyncJob_BrowserTest} canary).
 *
 * <p>
 * The single most important invariant asserted here is HIGH-9: the job-running affordance is a DISTINCT marker
 * ({@code data-juneau-job}) that {@code hasInFlightRow} does NOT read, so a long job never freezes the table's
 * polling the way the synchronous {@code data-juneau-inflight} marker does.
 */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_AsyncJobs_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String viewsJs() throws Exception {
		return c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	private static String fn(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> signature + " not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) HIGH-9: the job-running affordance is a DISTINCT marker that never freezes polling
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_setRowJobRunning_setsDistinctJobMarker_neverTheInflightMarker() throws Exception {
		var s = fn(viewsJs(), "function setRowJobRunning(");
		assertTrue(s.contains("setAttribute(\"data-juneau-job\""), s);
		assertTrue(s.contains("removeAttribute(\"data-juneau-job\")"), s);
		// The whole point of HIGH-9: a job must NEVER set the synchronous in-flight marker that freezes polling.
		assertFalse(s.contains("data-juneau-inflight"), () -> "job affordance must NOT touch data-juneau-inflight:\n" + s);
		// A second job can't be launched on the same row while one runs.
		assertTrue(s.contains("trigger.disabled"), s);
	}

	@Test void a02_hasInFlightRow_ignoresTheJobMarker_soPollingKeepsRunningDuringAJob() throws Exception {
		// hasInFlightRow reads ONLY data-juneau-inflight; it must NOT read data-juneau-job, or a job would freeze
		// the whole table's polling for up to the 120s hard timeout (hiding a resurrected incident) - exactly the
		// failure HIGH-9 forbids.
		var h = fn(viewsJs(), "function hasInFlightRow(");
		assertTrue(h.contains("data-juneau-inflight"), h);
		assertFalse(h.contains("data-juneau-job"), () -> "hasInFlightRow must ignore the job marker:\n" + h);
	}

	@Test void a03_startJobStream_usesTheJobAffordance_neverTheInflightMarker() throws Exception {
		var s = fn(viewsJs(), "function startJobStream(");
		assertTrue(s.contains("setRowJobRunning(tr, true)"), s);
		assertTrue(s.contains("setRowJobRunning(tr, false)"), s);
		// It must not co-opt the synchronous in-flight lifecycle.
		assertFalse(s.contains("setRowInFlight("), () -> "startJobStream must not use the sync in-flight marker:\n" + s);
		assertFalse(s.contains("data-juneau-inflight"), s);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) The async start is detected by RESPONSE SHAPE (a job pointer), not a new RowAction wire field
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_parseJobStarted_requiresANonBlankStreamUrl_andIsDisjointFromATypedResult() throws Exception {
		var p = fn(viewsJs(), "function parseJobStarted(");
		assertTrue(p.contains("isBlankToken(o.streamUrl)"), p);
		// The two 2xx shapes are disjoint: a job pointer is recognized by streamUrl, a terminal result by outcome.
		var settle = fn(viewsJs(), "function settleActionResponse(");
		assertTrue(settle.contains("parseJobStarted(text)"), settle);
		assertTrue(settle.contains("startJobStream("), settle);
		// The job branch is checked BEFORE the typed-result branch.
		assertTrue(settle.indexOf("parseJobStarted(text)") < settle.indexOf("parseActionResult(text)"), settle);
	}

	@Test void b02_settleClearsInflightBeforeRoutingToTheJobStream_soPollingResumesFirst() throws Exception {
		// The in-flight marker is cleared as the FIRST statement of settle (417), so table polling has already
		// resumed before the (possibly 120s) job even starts streaming - the job then only ever carries the
		// distinct data-juneau-job marker.
		var settle = fn(viewsJs(), "function settleActionResponse(");
		var open = settle.indexOf("{");
		var firstStmt = settle.substring(open + 1, settle.indexOf(";", open));
		assertTrue(firstStmt.contains("setRowInFlight(tr, false)"), () -> "settle must clear the in-flight marker first:\n" + settle);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) The stream URL IS the capability (HIGH-4): an EventSource GET, no CSRF header on the stream
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_startJobStream_opensAnEventSourceOnTheCapabilityStreamUrl() throws Exception {
		var s = fn(viewsJs(), "function startJobStream(");
		assertTrue(s.contains("new EventSource(started.streamUrl)"), s);
		// A browser EventSource cannot set headers, so the stream carries NO CSRF header - unguessability is the gate.
		assertFalse(s.contains(DEFAULT_CSRF_HEADER_LITERAL), () -> "the SSE GET must not attach a CSRF header:\n" + s);
		// Progress + a single terminal result event; a stream error is itself a non-optimistic terminal outcome.
		assertTrue(s.contains("addEventListener(\"progress\""), s);
		assertTrue(s.contains("addEventListener(\"result\""), s);
		assertTrue(s.contains("addEventListener(\"error\""), s);
	}

	/** The exact header token the runtime uses for the CSRF header (so c01's negative assertion is meaningful). */
	private static final String DEFAULT_CSRF_HEADER_LITERAL = "X-Csrf-Token";

	@Test void c02_missingEventSourceDegradesToAVisibleUnknown_notASilentHang() throws Exception {
		var s = fn(viewsJs(), "function startJobStream(");
		assertTrue(s.contains("typeof EventSource === \"undefined\""), s);
		assertTrue(s.contains("outcome: \"unknown\""), s);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Live progress is painted with textContent ONLY (never innerHTML) - the streamed content is customer-adjacent
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_renderJobProgress_usesTextContentNeverInnerHtml_andWiresCancel() throws Exception {
		var body = viewsJs();
		var r = fn(body, "function renderJobProgress(");
		assertTrue(r.contains(".textContent"), r);
		assertFalse(r.contains(".innerHTML"), () -> "job progress must never use innerHTML:\n" + r);
		assertFalse(r.contains("insertAdjacentHTML"), r);
		assertTrue(r.contains("juneau-view-job-progress"), r);
		assertTrue(r.contains("juneau-view-job-cancel"), r);
		assertTrue(r.contains("cancelJob(started, table, tr)"), r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Terminal result reuses the SAME contract handshake + outcome render (cancelled / cancelled-after-effect)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_finishJobFromResult_reusesContractHandshakeAndOutcomeRender() throws Exception {
		var f = fn(viewsJs(), "function finishJobFromResult(");
		assertTrue(f.contains("result.contractVersion !== JUNEAU_ACTION_RESULT_CONTRACT_VERSION"), f);
		assertTrue(f.contains("normalizeOutcome(result)"), f);
		assertTrue(f.contains("applySuccessBehavior("), f);
		assertTrue(f.contains("renderActionOutcome("), f);
	}

	@Test void e02_actionOutcomeMessage_rendersBothReservedAsyncOutcomes() throws Exception {
		// The frozen ActionResult contract reserved cancelled / cancelled-after-effect; the runtime renders both
		// without any new UI (Q4: the two are DIFFERENT outcomes and must not be collapsed).
		var body = viewsJs();
		var m = fn(body, "function actionOutcomeMessage(");
		assertTrue(m.contains("case \"cancelled\":"), m);
		assertTrue(m.contains("case \"cancelled-after-effect\":"), m);
		// And they are recognized outcomes (not normalized away to unknown).
		assertTrue(body.contains("\"cancelled\": 1"), body);
		assertTrue(body.contains("\"cancelled-after-effect\": 1"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Cancel is a fail-closed non-safe POST; the SERVER is authoritative
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_buildJobCancelRequest_isAFailClosedCsrfPost() throws Exception {
		var b = fn(viewsJs(), "function buildJobCancelRequest(");
		assertTrue(b.contains("no-cancel-url"), b);
		assertTrue(b.contains("missing-token"), b);           // fail-closed: a blank token sends nothing
		assertTrue(b.contains("method: \"POST\""), b);         // a non-safe write, so the boundary's full checks apply
		assertTrue(b.contains(DEFAULT_CSRF_HEADER_LITERAL) || b.contains("DEFAULT_CSRF_HEADER"), b);
	}

	@Test void f02_asyncFunctionsAreExposedOnThePublicApi() throws Exception {
		var body = viewsJs();
		for (var name : new String[]{"parseJobStarted", "buildJobCancelRequest", "setRowJobRunning", "renderJobProgress",
				"clearJobProgress", "startJobStream", "finishJobFromResult", "cancelJob"})
			assertTrue(body.contains(name + ": " + name), () -> "async function not exposed on NS.init: " + name);
	}
}
