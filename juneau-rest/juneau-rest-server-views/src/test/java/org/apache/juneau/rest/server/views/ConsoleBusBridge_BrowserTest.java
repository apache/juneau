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

import static org.apache.juneau.rest.server.views.ConsoleBusBrowserSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * An SSE bridge in real Chromium (spec 7): the CSRF-stamped session POST, a retained resync as the first state, live frames repainting a card,
 * a dropped stream reconnecting on the virtual clock with a resync that repairs state ({@code gap:true}), and a denied session as the page
 * banner E-JS-53.  The session and stream are faked in-page by {@code console-bus/prelude.js}; the clock is injected, so nothing sleeps.
 */
@EnabledIfSystemProperty(named=ConsoleBusBrowserSupport.GATE, matches="true", disabledReason=ConsoleBusBrowserSupport.DISABLED)
class ConsoleBusBridge_BrowserTest extends TestBase {

	private static Map<String,Object> r;
	private static Map<String,Object> denied;

	@BeforeAll
	static void probe() throws Exception {
		r = run("bridge", "bridge.json", true, "{}", "");
		denied = run("bridge-denied", "bridge.json", true, "{session:{status:403,body:{}}}", "");
	}

	@Test void a01_noJsFailures() {
		assertEquals(List.of(), list(r.get("jsFailures")), () -> r.toString());
		assertEquals(List.of(), list(r.get("consoleErrors")), () -> r.toString());
	}

	@Test void a02_theSessionPostCarriesTheCsrfHeaderAndTheDeclaredTopics() {
		var p = map(r.get("sessionPost"));
		assertEquals("/rest/ops/jobs/juneau-bus/session", p.get("path"));
		assertEquals("POST", p.get("method"));
		assertEquals(CSRF_TOKEN, p.get("csrf"));
		var b = map(p.get("body"));
		assertEquals("ops", b.get("bridge"));
		assertEquals("sse", b.get("transport"));
		assertEquals(List.of("ops.jobs"), list(b.get("downstream")));
	}

	@Test void a03_theFirstResyncOpensTheBridgeWithTheRetainedState() {
		var f = map(r.get("firstOpen"));
		var b = map(f.get("bridge"));
		assertEquals("open", b.get("state"));
		assertEquals(0, num(b.get("attempt")));
		assertEquals(Boolean.FALSE, b.get("gap"));
		assertEquals(List.of(), list(map(f.get("opsJobs")).get("running")));
	}

	@Test void a04_aLiveFrameRepaintsTheSubscribingCard() {
		var l = map(r.get("live"));
		var running = list(map(l.get("opsJobs")).get("running"));
		assertEquals(1, running.size());
		assertEquals("j-1", map(running.get(0)).get("jobId"));
		assertTrue(num(l.get("jobsRendersAfter")) > num(l.get("jobsRendersBefore")));
	}

	@Test void a05_aDroppedStreamReconnectsRetryably() {
		var d = map(r.get("dropped"));
		var b = map(d.get("bridge"));
		assertEquals("reconnecting", b.get("state"));
		assertEquals(1, num(b.get("attempt")));
		assertTrue(num(b.get("nextRetryMs")) > 0);
		assertEquals("E-JS-56", map(b.get("error")).get("code"));
		assertEquals(1, num(d.get("streams")));
		assertNull(r.get("statusError"), "a retryable drop must not paint a page banner");
	}

	@Test void a06_theVirtualClockFiresTheReconnectAndTheResyncRepairsState() {
		var rec = map(r.get("recovered"));
		var b = map(rec.get("bridge"));
		assertEquals("open", b.get("state"));
		assertEquals(Boolean.TRUE, b.get("gap"));
		assertEquals(List.of(), list(map(rec.get("opsJobs")).get("running")), "j-1 finished while the stream was down");
		assertEquals(2, list(rec.get("streamPaths")).size());
	}

	@Test void a07_reconnectReusesTheSession() {
		assertEquals(1, num(map(r.get("recovered")).get("sessionPosts")));
	}

	@Test void a08_theStatusCardSawEveryStateInOrder() {
		assertEquals(List.of("connecting", "open", "reconnecting", "connecting", "open"), list(r.get("states")));
	}

	@Test void b01_aDeniedSessionIsTheBannerEJS53AndNoStreamOpens() {
		var p = map(denied.get("pageError"));
		assertEquals("alert", p.get("role"));
		assertTrue(list(p.get("codes")).contains("E-JS-53"), () -> denied.toString());
		assertEquals("failed", map(denied.get("bridge")).get("state"));
		assertEquals(0, num(denied.get("streams")));
		assertTrue(list(denied.get("consoleErrors")).stream().anyMatch(l -> l.toString().contains("E-JS-53")), () -> denied.toString());
	}
}
