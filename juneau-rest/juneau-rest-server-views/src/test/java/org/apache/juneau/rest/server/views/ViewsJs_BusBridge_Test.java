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

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Behavioral coverage for the bridge runtime (spec §11.5, §11.6).  {@code bus-bridge.cjs} drives
 * {@code JuneauViews.bus.attachSource} with hand-driven test-double sources and a fake clock, and reports source
 * validation (E-JS-52), grant enforcement (E-JS-54), the {@code bridge:<id>} state machine, backoff and its reset,
 * Retry-After, resync clearing, upstream forwarding (E-JS-55), detach, and the two give-up paths (E-JS-57).
 * Gated on {@code node} being on {@code PATH}.
 */
class ViewsJs_BusBridge_Test extends TestBase {

	private static Map<String,Object> report;

	@BeforeAll
	static void runHarness() throws Exception {
		report = BusHarness.run("bus-bridge.cjs", "/org/apache/juneau/views/juneau-bus.js");
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> section(String name) {
		assumeTrue(report != null, "node not on PATH");
		var m = (Map<String,Object>) report.get(name);
		assertNotNull(m, () -> "report has no '" + name + "': " + report);
		return m;
	}

	/** Compares with strict JSON parsed by the same parser as the report, so numbers compare by type and value. */
	private static void assertJson(String expected, Object actual) {
		try {
			assertEquals(Json.to(expected, Object.class), actual, () -> String.valueOf(actual));
		} catch (Exception e) {
			fail("bad expected JSON: " + expected + ": " + e);
		}
	}

	@Test void a01_invalidSourcesAreRefusedWithBannerErrors() {
		var r = section("validation");
		assertJson("""
			["E-JS-52","E-JS-52","E-JS-52","E-JS-52","E-JS-52","E-JS-52"]""", r.get("codes"));
		assertEquals(List.of(
			"bridge 'null': source must be an object",
			"bridge 'x': connect must be a function",
			"bridge 'fw': downstream 'selection:t1' is a framework state topic owned by the page",
			"bridge 'ov': topic 'app.a' is both downstream and upstream",
			"bridge 'd': duplicate bridge id 'd'",
			"bridge 'nosend': send missing for a source with upstream topics"
		), r.get("messages"));
		assertJson("[true,true,true,true,true,true]", r.get("banners"));
		// A bad source with a usable id still gets a bridge:<id> record, in state failed.
		assertEquals("failed", r.get("xState"));
		assertEquals("failed", r.get("noSendState"));
		// The duplicate is refused; the first 'd' is untouched.
		assertEquals("connecting", r.get("firstDStillConnecting"));
		assertJson("1", r.get("dConnects"));
	}

	@Test void a02_ungrantedTopicIsDroppedAndMetaNamesTheBridge() {
		var r = section("grants");
		assertJson("[\"E-JS-54\"]", r.get("codes"));
		assertEquals("bridge 'g' delivered 'app.zzz', which it was not granted; dropped", r.get("message"));
		assertEquals(true, r.get("zzz"));
		assertJson("{\"n\":1}", r.get("a"));
		assertJson("[{\"from\":\"server\",\"bridge\":\"g\"}]", r.get("metas"));
		// A per-message error does not close the connection.
		assertEquals("open", r.get("state"));
	}

	@Test void a03_lifecycleConnectingOpenReconnectingOpenWithGap() {
		var r = section("lifecycle");
		assertJson("""
			[
				{"state":"connecting","attempt":0,"nextRetryMs":0,"gap":false,"error":null},
				{"state":"open","attempt":0,"nextRetryMs":0,"gap":false,"error":null},
				{"state":"reconnecting","attempt":1,"nextRetryMs":1000,"gap":false,"error":"E-JS-56"},
				{"state":"connecting","attempt":1,"nextRetryMs":1000,"gap":false,"error":"E-JS-56"},
				{"state":"open","attempt":1,"nextRetryMs":0,"gap":true,"error":null}
			]""", r.get("seen"));
		assertJson("2", r.get("connects"));
	}

	@Test void a04_backoffDoublesFromOneSecondAndCapsAtThirty() {
		assumeTrue(report != null, "node not on PATH");
		assertJson("[1000,2000,4000,8000,16000,30000,30000,30000]", report.get("backoffTable"));
		// +/-20% jitter around the 4 s step.
		assertJson("{\"low\":3200,\"high\":4800}", report.get("backoffBounds"));
		// The runtime uses the same schedule (random = 0.5 is the midpoint).
		assertJson("[1000,2000,4000,8000,16000,30000,30000]", report.get("backoffRuntime"));
		assertJson("24000", report.get("backoffJitterLow"));
	}

	@Test void a05_attemptCounterResetsOnlyAfterThirtySecondsOpen() {
		var r = section("reset");
		assertJson("{\"attempt\":1,\"nextRetryMs\":1000}", r.get("after30s"));
		assertJson("{\"attempt\":4,\"nextRetryMs\":8000}", r.get("after10s"));
	}

	@Test void a06_retryAfterIsAFloorNotAReplacement() {
		var r = section("retryAfter");
		assertJson("10000", r.get("big"));
		assertJson("1000", r.get("small"));
	}

	@Test void a07_resyncClearsRetainedTopicsTheServerDidNotResend() {
		var r = section("resync");
		assertJson("{\"v\":1}", r.get("a"));
		assertEquals(true, r.get("bCleared"));
		// An identical re-sent value is deduplicated by the core: no second callback.
		assertJson("1", r.get("callsAfterFirst"));
		assertJson("1", r.get("callsAfterResync"));
		assertEquals(true, r.get("gap"));
	}

	@Test void a08_upstreamIsForwardedOnlyWhileOpen() {
		var r = section("upstream");
		assertJson("[\"E-JS-55\"]", r.get("codes"));
		assertEquals("bridge 'u': upstream 'app.up' was refused by the server: bridge not open", r.get("message"));
		assertEquals("page", r.get("from"));
		assertJson("0", r.get("sentWhileConnecting"));
		assertJson("[{\"v\":1,\"type\":\"pub\",\"topic\":\"app.up\",\"payload\":{\"x\":1}}]", r.get("sent"));
	}

	@Test void a09_detachClosesOnceAndClearsEverything() {
		var r = section("detach");
		assertEquals(true, r.get("aCleared"));
		assertEquals(true, r.get("bridgeCleared"));
		assertJson("1", r.get("closes"));
		assertJson("""
			[{"state":"closed","attempt":0,"nextRetryMs":0,"gap":false,"error":null},{"cleared":true}]""", r.get("tail"));
	}

	@Test void a10_maxAttemptsGivesUpWithE_JS_57() {
		var r = section("maxAttempts");
		assertEquals("failed", r.get("state"));
		assertJson("3", r.get("connects"));
		assertJson("[\"E-JS-57\"]", r.get("codes"));
		assertEquals("bridge 'm' gave up after 2 attempts: transport 'custom' unavailable: connection closed", r.get("message"));
		assertEquals(true, r.get("banner"));
	}

	@Test void a11_capabilityRefusalRetriesAtOnceAndGivesUpAfterThree() {
		var r = section("capability");
		assertJson("[0,0]", r.get("delays"));
		assertEquals("failed", r.get("state"));
		assertJson("3", r.get("connects"));
		assertJson("[\"E-JS-57\"]", r.get("codes"));
		assertEquals("bridge 'c' gave up after 3 attempts: capability refused 3 times in a row", r.get("message"));
		// An open between refusals resets the count.
		assertEquals("reconnecting", r.get("resetState"));
	}

	@Test void a12_nonRetryableFailsAndQuietCloseDoesNotReport() {
		var n = section("nonRetryable");
		assertEquals("failed", n.get("state"));
		assertEquals("E-JS-53", n.get("error"));
		assertJson("""
			[{"code":"E-JS-53","message":"bridge 'n': session refused (403): bus:topic-denied","banner":true,"from":null}]""",
			n.get("errors"));
		var q = section("quiet");
		assertEquals("closed", q.get("state"));
		assertJson("0", q.get("errors"));
	}

	@Test void a13_perMessageErrorHasNoBannerAndStaleContextIsIgnored() {
		var r = section("perMessage");
		assertJson("[\"E-JS-59\"]", r.get("codes"));
		assertEquals("bridge 'p': server error bus:boom: kaput", r.get("message"));
		assertEquals(false, r.get("banner"));
		assertEquals(true, r.get("staleIgnored"));
		assertEquals("connecting", r.get("state"));
	}

	@Test void b01_retainDeclaredTopicsAreTrackedWithoutTheSourceFlag() {
		var r = section("declaredRetain");
		assertEquals(true, r.get("bCleared"));
		assertEquals(true, r.get("aKept"));
		assertEquals(true, r.get("aStillThere"));
		assertJson("{\"before\":true,\"after\":true}", r.get("detach"));
	}

	@Test void b02_openResetRepublishesTheBridgeState() {
		var r = section("resetRepublish");
		assertJson("1", r.get("before"));
		assertJson("0", r.get("after"));
		assertEquals("open", r.get("state"));
		assertJson("0", r.get("stored"));
	}

	@Test void b03_detachWhileReconnectingCancelsTheRetry() {
		var r = section("detachReconnecting");
		assertEquals("reconnecting", r.get("stateBefore"));
		assertJson("1", r.get("connects"));
		// The dead connection was already released when it dropped.
		assertJson("0", r.get("closes"));
		assertEquals(true, r.get("cleared"));
	}

	@Test void b04_upstreamNeverEchoesTheBridgesOwnDownstream() {
		var r = section("echo");
		assertJson("0", r.get("echoed"));
		assertJson("[2,3]", r.get("sent"));
	}

	@Test void b05_commandsCanComeDownstream() {
		var r = section("cmd");
		assertJson("[{\"op\":\"refresh\",\"from\":\"server\",\"bridge\":\"cm\"}]", r.get("got"));
		assertJson("[]", r.get("errors"));
		assertEquals(false, r.get("retained"));
	}

	@Test void b06_connectThrowingFailsTheBridge() {
		var r = section("connectThrows");
		assertEquals("failed", r.get("state"));
		assertJson("[\"E-JS-52\"]", r.get("codes"));
		assertEquals(List.of("bridge 'ct': connect threw: boom"), r.get("messages"));
		assertJson("[true]", r.get("banners"));
	}

	@Test void b07_aFailedBridgeIdCanBeReplaced() {
		var r = section("replaceFailed");
		assertEquals("failed", r.get("failedState"));
		assertEquals("open", r.get("state"));
		assertJson("1", r.get("connects"));
		// Detaching the stale link must not touch the replacement.
		assertJson("0", r.get("closes"));
		assertJson("[\"E-JS-52\"]", r.get("codes"));
	}
}
