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
package org.apache.juneau.console.views;

import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

/**
 * The job-log runtime {@code juneau-joblog.js} against the real shell: the form, submit, streaming, outcomes,
 * cancel, re-attach, not-found, interruption and lifecycle.  Runs when node is on PATH.
 */
class ConsoleJobLog_Test extends TestBase {

	private static final Map<String,Object> REF = Map.of("jobId", "j1", "streamUrl", "/jobs/j1/stream", "cancelUrl", "/jobs/j1/cancel");
	private static final String NOT_FOUND = "Job not found; it may have finished or the server restarted.";
	private static final String ATTACH_FAILED = "Could not attach to the job's progress stream; the server may be busy. Reload the page to try again.";
	private static final String INTERRUPTED = "The progress stream was interrupted; the job may still be running. Reload the page to reattach.";

	private static Map<?,?> report() {
		var r = NodeHarness.report("job-log-card.cjs", ConsoleChromeMixin.CONSOLE_JS_RESOURCE, ConsoleChromeMixin.JOBLOG_JS_RESOURCE);
		assumeTrue(r != null, "node (or job-log-card.cjs) not available - JS layer skipped");
		return r;
	}

	private static Map<?,?> part(String key) {
		var s = (Map<?,?>)report().get(key);
		assertNotNull(s, () -> "section '" + key + "' missing from the harness output");
		assertNull(s.get("crash"), () -> "section '" + key + "' crashed: " + s.get("crash"));
		return s;
	}

	private static Map<?,?> state(String key) {
		return (Map<?,?>)part(key).get("state");
	}

	@Test void a01_form() {
		var r = part("form");
		assertEquals("Rebuild index", r.get("title"));
		assertEquals(List.of("Reason", "Shards", "Notes"), r.get("labels"));
		assertEquals(List.of("reason", "shards", "notes"), r.get("names"));
		assertEquals(List.of("text", "number", "textarea"), r.get("types"));
		assertEquals(List.of(true, false, false), r.get("required"));
		assertEquals(List.of("", "4", ""), r.get("values"));
		assertEquals("Optional", r.get("placeholder"));
		assertEquals("log", r.get("outputRole"));
		assertEquals("polite", r.get("ariaLive"));
		assertEquals("0", r.get("tabindex"));
		assertEquals("320px", r.get("height"));
		assertEquals(List.of("cancel", "job", "phase"), r.get("api"));
		assertEquals("1", r.get("version"));
		var s = (Map<?,?>)r.get("state");
		assertEquals("idle", s.get("phase"));
		assertNull(s.get("banner"));
		assertEquals("Rebuild", s.get("submitText"));
		assertEquals(false, s.get("submitHidden"));
		assertEquals(true, s.get("newRunHidden"));
		assertEquals(List.of(false, false, false), s.get("inputsDisabled"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a02_defaults() {
		var r = part("defaults");
		assertEquals("Run", r.get("submitText"));
		assertEquals("240px", r.get("height"));
		assertEquals(0, ((Number)r.get("inputs")).intValue());
	}

	@Test void a03_submitStreamSuccess() {
		var r = part("success");
		var submitting = (Map<?,?>)r.get("submitting");
		assertEquals("submitting", submitting.get("phase"));
		assertEquals(true, submitting.get("submitDisabled"));
		var running = (Map<?,?>)r.get("running");
		assertEquals("running", running.get("phase"));
		assertEquals("Cancel", running.get("submitText"));
		assertEquals(false, running.get("submitDisabled"));
		assertEquals(List.of(true, true, true), running.get("inputsDisabled"));
		var fetches = (List<?>)r.get("fetches");
		assertSize(1, fetches);
		var f = (Map<?,?>)fetches.get(0);
		assertEquals("/jobs/rebuild", f.get("url"));
		assertEquals("POST", f.get("method"));
		assertEquals("application/json", f.get("contentType"));
		assertEquals(true, f.get("csrf"));
		var body = (Map<?,?>)f.get("body");
		assertEquals(Set.of("reason", "shards", "notes", "idempotencyKey"), body.keySet());
		assertEquals("nightly", body.get("reason"));
		// JSON numbers may parse as Integer, Long or Double; compare the value, not the boxed type.
		assertEquals(4, ((Number)body.get("shards")).intValue());
		assertEquals("", body.get("notes"));
		assertEquals("uuid-1", body.get("idempotencyKey"));
	}

	@Test void a03b_submitStreamSuccess_finalState() {
		var r = part("success");
		assertEquals(List.of("/jobs/j1/stream?lastEventId=0"), r.get("streams"));
		assertEquals(List.of("/?job=j1"), r.get("replaced"));
		assertEquals(REF, r.get("stored"));
		assertEquals(List.of("step 1 <b>bold</b>", "step 2"), r.get("lines"));
		assertEquals(0, ((Number)r.get("boldTags")).intValue());
		assertEquals(2, ((Number)r.get("closed")).intValue());
		var s = (Map<?,?>)r.get("state");
		assertEquals("done", s.get("phase"));
		assertEquals("Done. Rebuilt 4 shards.", s.get("banner"));
		assertEquals("ok", s.get("tone"));
		// Non-error banners are not live regions; the shell's announce region reads them (one channel, read once).
		assertNull(s.get("role"));
		assertEquals("Done. Rebuilt 4 shards.", r.get("announced"));
		assertEquals(true, s.get("submitHidden"));
		assertEquals(false, s.get("newRunHidden"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a04_outcomes() {
		var r = part("outcomes");
		assertEquals(Map.of("banner", "The job failed. disk full", "tone", "error", "role", "alert"), r.get("failure"));
		assertEquals(Map.of("banner", "The job was refused.", "tone", "error", "role", "alert"), r.get("refusal"));
		assertEquals(Map.of("banner", "The job ended with an unknown outcome.", "tone", "error", "role", "alert"), r.get("unknown"));
		assertEquals(Map.of("banner", "Cancelled.", "tone", "error", "role", "alert"), r.get("cancelled"));
		assertEquals(Map.of("banner", "Cancelled after the job had already made changes.", "tone", "error", "role", "alert"), r.get("cancelled-after-effect"));
		assertEquals(Map.of("banner", "The job ended with an unknown outcome.", "tone", "error", "role", "alert"), r.get("weird"));
		// Error banners are role=alert and are not also sent to the announce region.
		var announced = (Map<?,?>)report().get("outcomesAnnounced");
		for (var o : List.of("failure", "refusal", "unknown", "cancelled", "cancelled-after-effect", "weird"))
			assertNull(announced.get(o), o);
	}

	@Test void a05_badRef_isInlineAndKeepsTheForm() {
		var r = part("badRef");
		var s = (Map<?,?>)r.get("state");
		assertEquals("E-JS-77: job-log card 'rebuild' submit response is not a valid AsyncJobRef: '{\"nope\":1}'", s.get("banner"));
		assertEquals("error", s.get("tone"));
		assertEquals("idle", s.get("phase"));
		assertEquals(false, s.get("submitDisabled"));
		assertEquals(0, ((Number)r.get("streams")).intValue());
		assertNull(r.get("stored"));
		assertEquals(true, r.get("form"));
		assertTrue(String.valueOf(r.get("errors")).contains("[juneau-joblog] E-JS-77: job-log card 'rebuild'"), String.valueOf(r.get("errors")));
	}

	@Test void a06_submitError_showsTheResponseMessage() {
		var s = state("submitError");
		assertEquals("Too many jobs are running; try again later.", s.get("banner"));
		assertEquals("error", s.get("tone"));
		assertEquals("idle", s.get("phase"));
		assertEquals(0, ((Number)part("submitError").get("streams")).intValue());
	}

	@Test void a07_unparsableResult() {
		var r = part("badEvent");
		var s = (Map<?,?>)r.get("state");
		assertEquals("E-JS-78: job-log card 'rebuild' stream event could not be parsed: 'not json'", s.get("banner"));
		assertEquals("error", s.get("tone"));
		assertEquals("done", s.get("phase"));
		assertEquals(2, ((Number)r.get("closed")).intValue());
		assertTrue(String.valueOf(r.get("errors")).contains("[juneau-joblog] E-JS-78"));
	}

	@Test void a08_cancel_firstTerminalWins() {
		var r = part("cancel");
		var c = (Map<?,?>)r.get("cancelling");
		assertEquals("Cancelling\u2026", c.get("submitText"));
		assertEquals(true, c.get("submitDisabled"));
		assertEquals(List.of(
			Map.of("url", "/jobs/rebuild", "method", "POST", "csrf", true, "contentType", "application/json"),
			// A JSON content type and an empty object, so a boundary that requires JSON on every write lets it through.
			Map.of("url", "/jobs/j1/cancel", "method", "POST", "csrf", true, "contentType", "application/json", "body", Map.of())), r.get("fetches"));
		var s = (Map<?,?>)r.get("state");
		assertEquals("Cancelled.", s.get("banner"));
		assertEquals("done", s.get("phase"));
		assertEquals(2, ((Number)r.get("closed")).intValue());
	}

	@Test void a09_reattach() {
		var r = part("reattach");
		assertEquals(List.of("/jobs/j1/stream?lastEventId=0"), r.get("streams"));
		assertEquals(0, ((Number)r.get("fetches")).intValue());
		var s = (Map<?,?>)r.get("state");
		assertEquals("running", s.get("phase"));
		assertEquals("Cancel", s.get("submitText"));
		assertEquals(List.of(true, true, true), s.get("inputsDisabled"));
		assertEquals(List.of("replayed"), r.get("lines"));
		var n = part("reattachNoParam");
		assertEquals(List.of("/jobs/j1/stream?lastEventId=0"), n.get("streams"));
		assertEquals("running", n.get("phase"));
	}

	@Test void a10_mismatch_fallsBackToTheForm() {
		var r = part("mismatch");
		assertEquals(0, ((Number)r.get("streams")).intValue());
		assertEquals("idle", ((Map<?,?>)r.get("state")).get("phase"));
		assertEquals(REF, r.get("stored"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a11_notFound_replacesTheLogAndClearsTheJob() {
		var r = part("notFound");
		assertEquals(List.of(NOT_FOUND), r.get("lines"));
		assertEquals(1, ((Number)r.get("notice")).intValue());
		assertNull(r.get("stored"));
		assertEquals(List.of("/?tab=a"), r.get("replaced"));
		var s = (Map<?,?>)r.get("state");
		assertEquals("idle", s.get("phase"));
		assertEquals(List.of(false, false, false), s.get("inputsDisabled"));
	}

	@Test void a11b_attachRefusedWithoutA404_isAnAttachFailureNotNotFound() {
		var r = part("attachFailed");
		for (var k : List.of("busy", "server", "network")) {
			var c = (Map<?,?>)r.get(k);
			var s = (Map<?,?>)c.get("state");
			assertEquals(List.of(), c.get("lines"), k);
			assertEquals("done", s.get("phase"), k);
			assertEquals("warning", s.get("tone"), k);
			assertEquals(ATTACH_FAILED, s.get("banner"), k);
			assertEquals(REF, c.get("stored"), k);
			assertEquals(List.of(), c.get("replaced"), k);
			assertEquals(List.of("/jobs/j1/stream?lastEventId=0"), c.get("probes"), k);
			assertEquals("polite", c.get("live"), k);
			assertEquals(ATTACH_FAILED, c.get("announced"), k);   // the warning banner only, never a replay summary
		}
	}

	@Test void a12_interrupted_warnsAndKeepsTheJob() {
		var r = part("interrupted");
		assertEquals("running", r.get("reconnecting"));
		var s = (Map<?,?>)r.get("state");
		assertEquals(INTERRUPTED, s.get("banner"));
		assertEquals("warning", s.get("tone"));
		assertNull(s.get("role"));
		assertEquals(INTERRUPTED, r.get("announced"));
		assertEquals("done", s.get("phase"));
		assertEquals(REF, r.get("stored"));
	}

	@Test void a13_noEventSource() {
		var r = part("noSse");
		var s = (Map<?,?>)r.get("state");
		assertEquals("Live progress is unavailable in this browser.", s.get("banner"));
		assertEquals("warning", s.get("tone"));
		assertNull(s.get("role"));
		assertEquals("Live progress is unavailable in this browser.", r.get("announced"));
		assertEquals("done", s.get("phase"));
		assertEquals(REF, r.get("stored"));
		assertEquals(1, ((Number)r.get("fetches")).intValue());
	}

	@Test void a14_newRun_andClearOnSubmit() {
		var r = part("newRun");
		var a = (Map<?,?>)r.get("afterNewRun");
		var s = (Map<?,?>)a.get("state");
		assertEquals("idle", s.get("phase"));
		assertNull(s.get("banner"));
		assertEquals("Rebuild", s.get("submitText"));
		assertEquals(false, s.get("submitHidden"));
		assertEquals(true, s.get("newRunHidden"));
		assertEquals(List.of(false, false, false), s.get("inputsDisabled"));
		assertNull(a.get("stored"));
		assertEquals(List.of("/?job=j1", "/"), a.get("replaced"));
		assertEquals(List.of("first"), a.get("lines"));
		assertEquals(List.of("second"), r.get("lines"));
		assertEquals("again", ((Map<?,?>)r.get("body")).get("reason"));
		assertEquals(List.of("uuid-1", "uuid-2"), r.get("uuids"));
		assertEquals(List.of("first", "second"), part("keepLog").get("lines"));
	}

	@Test void a15_refreshIsNoOp_destroyClosesTheStream() {
		var r = part("lifecycle");
		var a = (Map<?,?>)r.get("afterRefresh");
		assertEquals(1, ((Number)a.get("streams")).intValue());
		assertEquals(1, ((Number)a.get("open")).intValue());
		assertEquals(List.of("step 1"), a.get("lines"));
		assertEquals("running", a.get("phase"));
		assertEquals(2, ((Number)r.get("closedOnDestroy")).intValue());
	}

	@Test void a16_urlJobWithEmptyStorage_staysOnTheForm() {
		var r = part("urlNoStored");
		assertEquals(0, ((Number)r.get("streams")).intValue());
		assertEquals(0, ((Number)r.get("fetches")).intValue());
		assertEquals("idle", ((Map<?,?>)r.get("state")).get("phase"));
		assertNull(r.get("stored"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a17_noCancelUrl_cancelIsDisabled() {
		var r = part("noCancelUrl");
		var running = (Map<?,?>)r.get("running");
		assertEquals("running", running.get("phase"));
		assertEquals("Cancel", running.get("submitText"));
		assertEquals(true, running.get("submitDisabled"));
		assertEquals(1, ((Number)r.get("fetches")).intValue());
		assertEquals("running", ((Map<?,?>)r.get("state")).get("phase"));
		assertEquals(1, ((Number)r.get("open")).intValue());
	}

	@Test void a18_failedCancel_keepsTheJobRunning() {
		var r = part("cancelFailed");
		var a = (Map<?,?>)r.get("afterFailedCancel");
		assertEquals("running", a.get("phase"));
		assertEquals("cancel failed", a.get("banner"));
		assertEquals("submit", a.get("focus"));
		assertEquals("error", a.get("tone"));
		assertEquals("Cancel", a.get("submitText"));
		assertEquals(false, a.get("submitDisabled"));
		assertEquals(1, ((Number)a.get("open")).intValue());
		assertEquals(List.of("/jobs/rebuild", "/jobs/j1/cancel"), r.get("fetches"));
		var s = (Map<?,?>)r.get("state");
		assertEquals("done", s.get("phase"));
		assertEquals("Done.", s.get("banner"));
		assertEquals("ok", s.get("tone"));
	}

	@Test void a19_requiredFields_blockTheSubmit() {
		var r = part("required");
		var b = (Map<?,?>)r.get("blocked");
		assertEquals(0, ((Number)b.get("fetches")).intValue());
		var s = (Map<?,?>)b.get("state");
		assertEquals("idle", s.get("phase"));
		assertEquals("Fill in the required fields: 'Reason'.", s.get("banner"));
		assertEquals("error", s.get("tone"));
		assertEquals("alert", s.get("role"));
		assertEquals(List.of(false, false, false), s.get("inputsDisabled"));
		assertEquals("true", b.get("invalid"));
		assertEquals(true, b.get("focused"));
		assertEquals(1, ((Number)r.get("fetches")).intValue());
		assertNull(r.get("invalidAfter"));
		assertEquals("running", r.get("phase"));
	}

	@Test void a20_reconnect_resumesById() {
		var r = part("reconnect");
		var d = (Map<?,?>)r.get("dropped");
		assertEquals("running", d.get("phase"));
		assertEquals(0, ((Number)d.get("readyState")).intValue());
		assertNull(d.get("banner"));
		assertEquals(List.of("p1", "p2", "p3"), r.get("afterFirst"));
		assertEquals(List.of("p1", "p2", "p3", "p4"), r.get("lines"));
		assertEquals(1, ((Number)r.get("streams")).intValue());
		var s = (Map<?,?>)r.get("state");
		assertEquals("done", s.get("phase"));
		assertEquals("Done.", s.get("banner"));
	}

	@Test void a21_crossOriginUrls_areRejected() {
		var r = part("crossOrigin");
		for (var k : List.of("cancel", "stream")) {
			var c = (Map<?,?>)r.get(k);
			var s = (Map<?,?>)c.get("state");
			assertEquals("idle", s.get("phase"), k);
			assertEquals("error", s.get("tone"), k);
			assertTrue(String.valueOf(s.get("banner")).startsWith("E-JS-77: job-log card 'rebuild' submit response is not a valid AsyncJobRef: "), k + ": " + s.get("banner"));
			assertEquals(0, ((Number)c.get("streams")).intValue(), k);
			assertEquals(List.of("/jobs/rebuild"), c.get("fetches"), k);
			assertNull(c.get("stored"), k);
			assertEquals(1, ((Number)c.get("errors")).intValue(), k);
		}
		var p = (Map<?,?>)r.get("poisoned");
		assertEquals("idle", p.get("phase"));
		assertEquals(0, ((Number)p.get("streams")).intValue());
		assertEquals(0, ((Number)p.get("fetches")).intValue());
		assertEquals(List.of(), p.get("errors"));
		assertNull(p.get("stored"));
		assertEquals("", p.get("search"));
		var a = (Map<?,?>)r.get("absolute");
		assertEquals(List.of("http://juneau.test/jobs/j1/stream?lastEventId=0"), a.get("streams"));
		assertEquals(List.of(
			Map.of("url", "/jobs/rebuild", "csrf", true),
			Map.of("url", "http://juneau.test/jobs/j1/cancel", "csrf", true)), a.get("fetches"));
		assertEquals("done", a.get("phase"));
	}

	@Test void a22_focus_followsTheVisibleControl() {
		var r = part("focus");
		assertEquals("output", r.get("whileSubmitting"));
		assertEquals("newRun", r.get("afterDone"));
		assertEquals("submit", r.get("afterNewRun"));
		assertEquals("output", r.get("whileCancelling"));
		assertEquals("done", r.get("phase"));
	}

	@Test void a23_crossOriginEndpoint_isRefusedBeforeTheFetch() {
		var r = part("crossEndpoint");
		for (var e : Map.of("backslash", "\\evil.test/x", "protocolRelative", "//evil.test", "leadingSpace", " //evil.test",
				"absolute", "https://evil.test/jobs/rebuild").entrySet()) {
			var k = e.getKey();
			var c = (Map<?,?>)r.get(k);
			var s = (Map<?,?>)c.get("state");
			assertEquals(List.of(), c.get("fetches"), k);
			assertEquals(0, ((Number)c.get("streams")).intValue(), k);
			assertEquals("idle", s.get("phase"), k);
			assertEquals("error", s.get("tone"), k);
			assertEquals("alert", s.get("role"), k);
			assertEquals("E-JS-81: job-log card 'rebuild' submit.endpoint is not a same-origin path: '" + e.getValue() + "'", s.get("banner"), k);
			assertEquals(List.of(false, false, false), s.get("inputsDisabled"), k);
			assertEquals(List.of("[juneau-joblog] E-JS-81: job-log card 'rebuild' submit.endpoint is not a same-origin path: '" + e.getValue() + "'"), c.get("errors"), k);
		}
		for (var e : Map.of("sameOrigin", "/jobs/rebuild", "sameOriginQuery", "/jobs/x?a=b", "relative", "jobs/x").entrySet()) {
			var k = e.getKey();
			var ok = (Map<?,?>)r.get(k);
			assertEquals(List.of(e.getValue()), ok.get("fetches"), k);
			assertEquals(1, ((Number)ok.get("streams")).intValue(), k);
			assertEquals("running", ((Map<?,?>)ok.get("state")).get("phase"), k);
			assertEquals(List.of(), ok.get("errors"), k);
		}
	}

	@Test void a27_reattachReplay_isSilencedThenSummarised() {
		var r = part("replayQuiet");
		assertEquals("off", r.get("before"));
		var d = (Map<?,?>)r.get("during");
		assertEquals("off", d.get("live"));
		assertNull(d.get("announced"));
		assertEquals(List.of("a", "b", "c"), d.get("lines"));
		var a = (Map<?,?>)r.get("after");
		assertEquals("polite", a.get("live"));
		assertEquals("Reattached to the running job; 3 earlier lines shown.", a.get("announced"));
		assertEquals(List.of("a", "b", "c", "d"), r.get("lines"));
		assertEquals("polite", r.get("freshLive"));
	}

	@Test void a28_replayEnds_onResultCapRefusalAndProbeTimeout() {
		var r = part("replayEnds");
		var d = (Map<?,?>)r.get("resultDuring");
		assertEquals("polite", d.get("live"));
		assertEquals("Done.", d.get("announced"));   // the outcome banner only, never a replay summary
		assertEquals("done", ((Map<?,?>)d.get("state")).get("phase"));
		var c = (Map<?,?>)r.get("atCap");
		assertEquals("off", c.get("before"));
		assertEquals("polite", c.get("live"));
		assertEquals("Reattached to the running job; 2 earlier lines shown.", c.get("announced"));
		var p = (Map<?,?>)r.get("probing");
		assertEquals("running", p.get("phase"));
		assertEquals("polite", p.get("live"));
		var t = (Map<?,?>)r.get("timedOut");
		var s = (Map<?,?>)t.get("state");
		assertEquals("done", s.get("phase"));
		assertEquals(ATTACH_FAILED, s.get("banner"));
		assertEquals(REF, t.get("stored"));
		assertEquals(1, ((Number)t.get("aborted")).intValue());
		assertEquals(1, ((Number)r.get("abortedOnTeardown")).intValue());
	}

	@Test void a24_banner_roleFirst_andTextClearedSoARepeatReannounces() {
		var r = part("bannerOrder");
		var t = "text:Fill in the required fields: 'Reason'.";
		var ev = (List<?>)r.get("events");
		assertEquals(List.of("role:alert", "text:", t, "hidden:false"), ev);
		var s = (Map<?,?>)r.get("state");
		assertEquals("Fill in the required fields: 'Reason'.", s.get("banner"));
		assertEquals("alert", s.get("role"));
	}

	@Test void a25_idlessStream_allShown() {
		var r = part("idless");
		assertEquals(List.of("p1", "p2", "p3"), r.get("lines"));
		assertEquals(1, ((Number)r.get("streams")).intValue());
		assertEquals("done", ((Map<?,?>)r.get("state")).get("phase"));
	}

	@Test void a26_repeatedId_isShownAgain_failLoud() {
		var r = part("repeatedId");
		assertEquals(List.of("p1", "p2", "p2", "p3"), r.get("lines"));
		assertEquals(1, ((Number)r.get("streams")).intValue());
		assertEquals("done", ((Map<?,?>)r.get("state")).get("phase"));
	}

	@Test void a29_hiddenField_notShown_sentWithItsDefault_andEveryInputIsAddressable() {
		var r = part("pageHidden");
		var f = (Map<?,?>)r.get("form");
		assertEquals(List.of("Reason", "Shards", "Notes"), f.get("labels"));
		assertEquals("hidden", f.get("type"));
		assertEquals("full", f.get("value"));
		assertEquals(false, f.get("inLabel"));
		assertEquals(List.of("reason", "shards", "notes", "mode"), f.get("fieldAttrs"));
		// The flat body is the pre-row-aware body plus the hidden value, in field order, then the key.
		assertEquals("{\"reason\":\"nightly\",\"shards\":4,\"notes\":\"\",\"mode\":\"full\",\"idempotencyKey\":\"uuid-1\"}", String.valueOf(r.get("body")));
		var bare = (Map<?,?>)r.get("noDefault");
		assertEquals("E-JS-110: job-log card 'rebuild' hidden field 'mode' has no default", bare.get("banner"));
		assertEquals(0, ((Number)bare.get("sent")).intValue());
	}

	@Test void a30_actionEnvelope_isSelfTargeted() {
		var r = part("pageEnvelope");
		assertEquals(Map.of("action", "rebuild-index", "targetId", "uuid-1", "idempotencyKey", "uuid-1", "fields", Map.of("reason", "nightly", "shards", 2, "notes", "")), r.get("body"));
		assertEquals("running", r.get("phase"));
	}

	@Test void a31_output_writesTheCardsOwnFieldOnSuccessOnly_andNeverOutsideTheCard() {
		var r = part("pageOutput");
		var ok = (Map<?,?>)r.get("success");
		assertEquals("indexed 4 shards", ok.get("notes"));
		assertEquals(0, ((Number)ok.get("errors")).intValue());
		assertEquals("", ((Map<?,?>)r.get("failure")).get("notes"));
		var outside = (Map<?,?>)r.get("outside");
		assertEquals("", outside.get("outside"));
		assertEquals(1, ((Number)outside.get("errors")).intValue());
	}

	@Test void a32_pageTransport_neverReadsJuneauViews() {
		var r = part("pageNoViews");
		assertEquals(0, ((Number)r.get("reads")).intValue());
		assertEquals("done", r.get("phase"));
	}
}
