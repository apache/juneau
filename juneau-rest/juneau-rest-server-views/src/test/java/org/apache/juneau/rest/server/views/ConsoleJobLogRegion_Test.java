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
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Node behavioral checks for the job-log card in a row's detail panel: the {@code job-log} populator and
 * {@code JuneauJobLog.mount} in {@code juneau-joblog.js}, over the stock {@code JuneauViews.jobs} runtime.  Runs when
 * node is on PATH.
 */
class ConsoleJobLogRegion_Test {

	private static Map<?,?> report() {
		var r = RegionsHarness.reportWith("job-log-region.cjs", ViewsMixin.HELPERS_JS_RESOURCE, ConsoleChromeMixin.JOBLOG_JS_RESOURCE);
		assumeTrue(r != null, "node (or job-log-region.cjs) not available - JS layer skipped");
		return r;
	}

	private static Map<?,?> part(String key) {
		var s = (Map<?,?>)report().get(key);
		assertNotNull(s, () -> "section '" + key + "' missing from the harness output");
		assertNull(s.get("crash"), () -> "section '" + key + "' crashed: " + s.get("crash"));
		return s;
	}

	private static Map<?,?> m(Map<?,?> m, String key) { return (Map<?,?>)m.get(key); }
	private static int n(Map<?,?> m, String key) { return ((Number)m.get(key)).intValue(); }

	@Test void r01_registersWhateverTheScriptOrder_andKeepsAnAppsOwnPopulator() {
		var r = part("registration");
		assertEquals(true, r.get("immediate"));
		var late = m(r, "late");
		assertEquals(false, late.get("viewsBeforeJobLog"));
		assertEquals(true, late.get("registered"));
		assertEquals(false, late.get("appKept"));
		assertEquals(true, m(r, "appFirst").get("appKept"));
	}

	@Test void r02_withoutJuneauViews_mountThrowsAndARegionPageLogsOnce() {
		var r = part("missingViews");
		assertEquals("E-JS-111", r.get("thrown"));
		assertEquals(1, n(r, "mountLogged"));
		assertEquals(1, n(r, "regionLogged"));
		assertEquals(2, n(r, "logged"));
	}

	@Test void r03_keying_thenIdle_andRetryAfterARefusedKey() {
		var r = part("keying");
		var k = m(r, "keying");
		assertEquals("keying", k.get("phase"));
		assertEquals(true, k.get("submitDisabled"));
		assertEquals(List.of(false), k.get("inputsDisabled"));
		assertEquals(false, k.get("retryShown"));
		var ready = m(r, "ready");
		assertEquals("idle", ready.get("phase"));
		assertEquals(false, ready.get("submitDisabled"));
		var refused = m(r, "refused");
		assertEquals("keying", refused.get("phase"));
		assertEquals(true, refused.get("submitDisabled"));
		assertEquals(true, refused.get("retryShown"));
		assertEquals("E-JS-109: job-log card 'restock' could not get a submit key from '/species/{id}/key'", refused.get("banner"));
		assertEquals(1, n(r, "logged"));   // the views runtime's own line; the card adds the banner only
		var retried = m(r, "retried");
		assertEquals("idle", retried.get("phase"));
		assertNull(retried.get("banner"));
		assertEquals("k-b", r.get("retriedKey"));
	}

	@Test void r04_submit_actionEnvelopeWithTheRowKey_streamsFromTheStart_andSpendsTheKey() {
		var r = part("submit");
		assertEquals(Map.of("action", "restock-species", "targetId", "r1", "idempotencyKey", "k-1", "fields", Map.of("count", 3)), r.get("body"));
		assertEquals("/s/j-1?lastEventId=0", r.get("streamUrl"));
		assertEquals("running", m(r, "running").get("phase"));
		assertEquals("Cancel", m(r, "running").get("submitText"));
		var done = m(r, "done");
		assertEquals("done", done.get("phase"));
		assertEquals("Done. restocked", done.get("banner"));
		assertEquals(List.of("half way"), done.get("lines"));
		assertEquals(2, n(r, "keysFetched"));
		assertEquals("idle", m(r, "afterNewRun").get("phase"));
	}

	@Test void r05_presets_ownRowPropertyAtSubmit_inheritedAndObjectRefused_nullSent() {
		var r = part("sources");
		assertEquals(Map.of("species", "cat"), m(r, "own").get("sent"));
		assertEquals(Map.of("species", "dog"), m(r, "atSubmit").get("sent"));
		for (var k : List.of("proto", "ctor", "toStr", "object")) {
			var c = m(r, k);
			assertNull(c.get("sent"), k);
			assertEquals(1, n(c, "e110"), k);
			assertTrue(String.valueOf(c.get("banner")).startsWith("E-JS-110: "), k);
		}
		assertEquals(Map.of("p", "d"), m(r, "protoDefault").get("sent"));
		assertEquals(Map.of("o", 7), m(r, "objectDefault").get("sent"));
		var nul = new LinkedHashMap<String,Object>();
		nul.put("n", null);
		assertEquals(nul, m(r, "nul").get("sent"));
		// A visible preset is a prefill; a value it cannot use leaves the input empty, with no error.
		assertEquals(List.of("cat", ""), m(r, "prefill").get("values"));
		assertEquals(0, n(m(r, "prefill"), "e110"));
	}

	@Test void r06_output_writesTextWithinTheRegionOnly() {
		var r = part("output");
		var slot = m(r, "slot");
		assertEquals("9", slot.get("slot"));
		assertEquals(0, n(slot, "slotChildren"));
		assertEquals("old", m(r, "failureSkipped").get("slot"));
		assertEquals("m1", m(r, "anyWritten").get("slot"));
		assertEquals("old\nm1", m(r, "appendSlot").get("slot"));
		assertEquals("m1", m(r, "input").get("note"));
		assertEquals("typed\nm1", m(r, "appendInput").get("note"));
		assertEquals("old", m(r, "objectSkipped").get("slot"));
		var outside = m(r, "outsideOnly");
		assertEquals("", outside.get("outside"));
		assertEquals(1, n(outside, "e112"));
		for (var k : List.of("slot", "failureSkipped", "anyWritten", "appendSlot", "input", "appendInput", "objectSkipped"))
			assertEquals(0, n(m(r, k), "e112"), k);
	}

	@Test void r07_detailGrid_paintedAboveTheFormFromTheDeclaredPayload() {
		var r = part("detail");
		assertEquals(true, r.get("gridInDetail"));
		assertEquals("4", r.get("value"));
		assertEquals(true, r.get("beforeForm"));
		assertEquals("ok", r.get("regionState"));
	}

	@Test void r08_collapseAndReexpand_reattachesViewOnly() {
		var r = part("reattach");
		var c = m(r, "afterCollapse");
		assertEquals(true, c.get("jobStreamOpen"));
		assertEquals(true, c.get("active"));
		assertEquals(true, c.get("viewGone"));
		assertEquals("/s/j-1?lastEventId=0", r.get("viewStreamUrl"));
		assertEquals(true, r.get("distinctStream"));
		var a = m(r, "attached");
		assertEquals("running", a.get("phase"));
		assertEquals(true, a.get("submitHidden"));
		var done = m(r, "done");
		assertEquals("done", done.get("phase"));
		assertEquals("Done. ok", done.get("banner"));
		assertEquals(List.of("one", "two"), done.get("lines"));
		assertEquals(1, n(r, "submits"));
		assertEquals(2, n(r, "keyAfter"));
	}

	@Test void r09_reexpandWhileTheSubmitIsInFlight_waitsThenAttaches() {
		var r = part("reattachPending");
		var w = m(r, "waiting");
		assertEquals("submitting", w.get("phase"));
		assertEquals(true, w.get("submitHidden"));
		var a = m(r, "attached");
		assertEquals("running", a.get("phase"));
		assertEquals(true, a.get("submitHidden"));
		assertEquals(List.of("/s/j-1?lastEventId=0", "/s/j-1?lastEventId=0"), r.get("streams"));
	}

	@Test void r10_oneJobPerRow_aSecondViewIsRefused() {
		var r = part("oneJobPerRow");
		var s = m(r, "second");
		assertEquals("idle", s.get("phase"));
		assertEquals("A job is already running for this row.", s.get("banner"));
		assertEquals(1, n(r, "submits"));
		assertEquals(List.of("cancel", "destroy", "phase"), r.get("apiKeys"));
	}

	@Test void r11_aTransportFailureKeepsTheKey_anHttpErrorSpendsIt() {
		var r = part("keyLifecycle");
		var f = m(r, "failed");
		assertEquals("idle", f.get("phase"));
		assertEquals("The request could not be completed; try again.", f.get("banner"));
		assertEquals(List.of("k-1", "k-1"), r.get("keys"));
		assertEquals("The server refused the request (HTTP 500).", m(r, "http").get("banner"));
		assertEquals(2, n(r, "keysFetched"));
	}

	@Test void r12_cancel_postsThroughTheViewsRuntime_outcomeFromTheStream() {
		var r = part("cancel");
		assertEquals(false, m(r, "running").get("submitDisabled"));
		assertEquals("Cancelling…", m(r, "cancelling").get("submitText"));
		assertEquals(true, r.get("cancelPosted"));
		assertEquals("Cancelled.", m(r, "done").get("banner"));
	}
}
