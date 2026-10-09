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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the JuneauViews.bus wraps in juneau-views.js (probe group, async job).
 *
 * <p>
 * The load-bearing fact: {@code probe:<group>} is published ONLY on a user-driven selection change.  The default
 * selection at enhance time, an imperative {@code select()}, a no-op re-click and a status repaint publish nothing,
 * so a subscriber (a page's probe listener) can never mistake a page load for a user pick.
 */
class ViewsJs_BusWraps_Test extends TestBase {

	private static Map<String,Object> report;

	@BeforeAll static void run() {
		report = BusHarness.run("bus-wraps.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.BUS_JS_RESOURCE);
	}

	private static Object r(String k) {
		assumeTrue(report != null, "node not on PATH");
		assertEquals(true, report.get("hasBus"), () -> "juneau-bus.js did not define JuneauViews.bus: " + report);
		assertEquals(true, report.get("hasHelpers"), () -> "probeGroupTopicKey/publishJobEvent not exported on NS.init: " + report);
		return report.get(k);
	}

	private static int i(String k) {
		return ((Number)r(k)).intValue();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Source shape: the probe publish lives inside emit(), behind selectId's (changed && notify) gate.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_probePublish_isInsideEmit_only() throws IOException {
		var js = BusHarness.source(ViewsMixin.VIEWS_JS_RESOURCE);
		var emitStart = js.indexOf("function emit(id) {");
		assertTrue(emitStart > 0, "enhanceProbeGroup's emit(id) not found");
		var emitEnd = js.indexOf("\n\t\t}\n", emitStart);
		var emit = js.substring(emitStart, emitEnd);
		assertTrue(emit.contains("publishProbeSelection(group, id)"), emit);
		// One definition plus one call site: nothing else publishes probe:.
		assertEquals(2, js.split("publishProbeSelection\\(", -1).length - 1, "publishProbeSelection must be defined once and called once");
		assertTrue(js.contains("if (changed && notify) emit(id);"), "selectId's user-driven gate must be unchanged");
	}

	@Test void a02_jobPublish_wiredIntoStartJobStream() throws IOException {
		var js = BusHarness.source(ViewsMixin.VIEWS_JS_RESOURCE);
		var s = js.indexOf("function startJobStream(started, action, table, tr, ctx) {");
		assertTrue(s > 0, "startJobStream not found");
		var body = js.substring(s, js.indexOf("\n\t}\n", s));
		assertTrue(body.contains("publishJobEvent(started, \"start\")"), body);
		assertTrue(body.contains("publishJobEvent(started, \"progress\", e.data)"), body);
		assertTrue(body.contains("if (first) publishJobEvent(started, \"result\", result)"), body);
		assertTrue(body.indexOf("finish(result,") < body.indexOf("if (first) publishJobEvent"), "the result must publish after finish()");
		assertTrue(js.contains("if (kind === \"result\") JOB_OWNERS.delete(jobId)"), "a terminal result must release the owner entry");
		assertFalse(body.contains("addEventListener(\"error\", function () {\n\t\t\tpublishJobEvent"), "a stream error must not publish a job state");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Probe wrap
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_defaultSelection_publishesNothing() {
		assertEquals(0, i("init_checked_historyCount"), "markup aria-checked must not publish");
		assertNull(r("init_checked_get"));
		assertEquals(0, i("init_checked_subscriberCalls"));
		assertEquals(0, i("init_checked_onSelect"));
		assertEquals(0, i("init_fallback_historyCount"), "first-enabled fallback must not publish");
	}

	@Test void b02_imperativeSelect_publishesNothing() {
		assertEquals(0, i("select_historyCount"));
		assertEquals(0, i("select_onSelect"));
	}

	@Test void b03_click_publishesOnce() {
		assertEquals(1, i("click_historyCount"));
		assertEquals(Map.of("schemaVersion", 1, "group", "grp-a", "id", "fail"), normalize(r("click_payload")));
		assertEquals("probe:grp-a", r("click_from"));
		assertEquals(1, i("click_subscriberCalls"));
		assertEquals("fail", r("click_onSelect"));
	}

	@Test void b03b_onSelectRunsBeforeTheBusMessage() {
		assertEquals("0", r("order_historyAtOnSelect"), "the probe message must be published after onSelect returns");
		assertEquals(1, i("order_historyAfter"));
	}

	@Test void b04_reclickAndRepaint_publishNothing() {
		assertEquals(1, i("reclick_repaint_historyCount"));
	}

	@Test void b05_keyboard_publishes() {
		assertEquals(2, i("key_historyCount"));
		assertEquals("ok", r("key_lastId"));
	}

	@Test void b06_groupKey_attributeThenId() {
		assertEquals("named", r("key_attr"));
		assertEquals(1, i("named_historyCount"));
		assertEquals(0, i("named_idTopicCount"));
	}

	@Test void b07_noOrBadKey_publishNothing_onSelectStillFires() {
		assertNull(r("key_none"));
		assertNull(r("key_bad"));
		assertEquals(0, i("nokey_published"));
		assertEquals("q", r("nokey_onSelect"));
		assertEquals("q", r("badkey_onSelect"));
	}

	@Test void b08_sscShape() {
		assertEquals(0, i("ssc_initCount"));
		assertEquals(Map.of("schemaVersion", 1, "group", "ssc-probe-row", "id", "tls"), normalize(r("ssc_clickPayload")));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Job wrap
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_job_startProgressResult() {
		var p = (List<?>)r("job_j1");
		assertEquals(3, p.size(), p::toString);
		assertEquals(Map.of("schemaVersion", 1, "jobId", "j-1", "state", "running"), normalize(p.get(0)));
		assertEquals(Map.of("schemaVersion", 1, "jobId", "j-1", "state", "running", "message", "Copying 3/10"), normalize(p.get(1)));
		assertEquals(Map.of("schemaVersion", 1, "jobId", "j-1", "state", "succeeded",
			"result", Map.of("outcome", "success", "message", "done")), normalize(p.get(2)));
		assertEquals("job:j-1", r("job_from"));
	}

	@Test void c02_job_outcomeMapping() {
		assertEquals(List.of("cancelled", "cancelled", "failed", "failed", "failed"), r("job_states"));
	}

	@Test void c03_job_unreadableResult_isFailed() {
		var p = (List<?>)r("job_nullResult");
		assertEquals(List.of(Map.of("schemaVersion", 1, "jobId", "j-null", "state", "failed",
			"message", "the job produced no readable result")), p.stream().map(ViewsJs_BusWraps_Test::normalize).toList());
	}

	@Test void c04_job_guards() {
		assertNull(r("job_guard_threw"));
		assertEquals(0, i("job_badId_count"));
		assertEquals(0, i("job_claimed_localCount"), "a job:<id> topic a bridge owns must not get local publishes");
		assertEquals(0, i("job_bogusKind_count"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// No bus on the page
	//------------------------------------------------------------------------------------------------------------------

	@Test void c05_job_busFailure_neverReachesTheHost() {
		assertNull(r("job_busThrows_threw"));
	}

	@Test void c06_stream_oneTerminal_publishedAfterTheRowSettled() {
		assertEquals("running", r("stream_afterStart"));
		// start, one progress, ONE result: the duplicate result and the progress after settle publish nothing.
		assertEquals("running,running,cancelled", r("stream_states"));
		// start and progress publish while the row is still running; the result only after finish() settled it.
		assertEquals("false,false,true", r("stream_settledAtPublish"));
		assertEquals(true, r("stream_closed"));
	}

	@Test void c07_stream_sameJobIdRunsAgainAfterATerminalResult() {
		// The retained "cancelled" from the first run is replayed to the new subscriber, then the second run's own messages.
		assertEquals("cancelled,running,cancelled", r("stream_reuseStates"));
	}

	@Test void c08_stream_errorPublishesNoTerminalState() {
		assertEquals("running", r("stream_errorStates"));
		assertEquals(true, r("stream_errorSettled"));
	}

	@Test void d01_noBus_unchanged() {
		assertEquals(false, r("nobus_hasBus"));
		assertEquals("q", r("nobus_onSelect"));
		assertNull(r("nobus_threw"));
	}

	/** JSON numbers come back as Integer/Long/Double depending on the parser; compare them as ints. */
	private static Object normalize(Object o) {
		if (o instanceof Map<?,?> m) {
			var out = new LinkedHashMap<String,Object>();
			m.forEach((k, v) -> out.put((String)k, normalize(v)));
			return out;
		}
		if (o instanceof Number n && n.doubleValue() == n.intValue())
			return n.intValue();
		return o;
	}
}
