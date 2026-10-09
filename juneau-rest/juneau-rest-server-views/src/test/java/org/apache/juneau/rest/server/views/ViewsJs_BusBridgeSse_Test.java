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
 * Behavioral coverage for the stock SSE source (spec §11.3 step 2a, §11.6).  {@code bus-bridge-sse.cjs} drives
 * {@code JuneauViews.bus.sources.sse} with a scripted {@code fetch}, hand-fed response streams and a fake clock, and
 * reports the session POST, the stream GET, chunked frame parsing, the HTTP status table, session-body checks,
 * CSRF, the same-origin rule, the watchdog, per-message errors and detach.  Gated on {@code node} being on
 * {@code PATH}.
 */
class ViewsJs_BusBridgeSse_Test extends TestBase {

	private static final String SID = "a".repeat(64);

	private static Map<String,Object> report;

	@BeforeAll
	static void runHarness() throws Exception {
		report = BusHarness.run("bus-bridge-sse.cjs", "/org/apache/juneau/views/juneau-bus.js");
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> section(String... path) {
		assumeTrue(report != null, "node not on PATH");
		Map<String,Object> m = report;
		for (var p : path) {
			var next = (Map<String,Object>) m.get(p);
			assertNotNull(next, "report has no '" + String.join(".", path) + "': " + report);
			m = next;
		}
		return m;
	}

	private static void assertJson(String expected, Object actual) {
		try {
			assertEquals(Json.to(expected, Object.class), actual, () -> String.valueOf(actual));
		} catch (Exception e) {
			fail("bad expected JSON: " + expected + ": " + e);
		}
	}

	@Test void a01_sessionPostCarriesCsrfAndTheRequestedGrant() {
		assertJson("""
			{
				"method":"POST","url":"/rest/ops/juneau-bus/session",
				"headers":{"Content-Type":"application/json","Accept":"application/json","X-Csrf-Token":"tok-123"},
				"body":{"v":1,"bridge":"s","transport":"sse","downstream":["app.a","app.ev"],"upstream":[]},
				"credentials":"same-origin"
			}""", section("happy").get("post"));
	}

	@Test void a02_streamGetParsesChunkedFramesAndOpensAtResyncEnd() {
		var r = section("happy");
		assertJson("{\"method\":\"GET\",\"url\":\"/rest/ops/juneau-bus/stream/" + SID + "\",\"accept\":\"text/event-stream\"}", r.get("get"));
		// The first chunks end mid-resync: still connecting.
		assertEquals("connecting", r.get("stateMid"));
		assertJson("[\"connecting\",\"open\"]", r.get("states"));
		assertJson("{\"n\":1}", r.get("a"));
		assertJson("[{\"p\":{\"e\":1},\"from\":\"server\",\"bridge\":\"s\"}]", r.get("ev"));
		assertJson("[]", r.get("errors"));
	}

	@Test void a03_streamEndRetriesOnTheSameSession() {
		var r = section("reconnect");
		assertJson("[\"POST\",\"GET\",\"GET\"]", r.get("methods"));
		assertJson("""
			{"state":"reconnecting","nextRetryMs":1000,"error":"bridge 's': transport 'sse' unavailable: stream ended"}""",
			r.get("afterEnd"));
		assertEquals("open", r.get("final"));
		assertEquals(true, r.get("gap"));
	}

	@Test void a04_stream404RePostsAndThreeInARowIsE_JS_57() {
		var r = section("capability");
		assertJson("[\"POST\",\"GET\",\"POST\",\"GET\",\"POST\",\"GET\"]", r.get("methods"));
		assertEquals("failed", r.get("state"));
		assertJson("""
			[{"code":"E-JS-57","message":"bridge 's' gave up after 3 attempts: capability refused 3 times in a row","banner":true}]""",
			r.get("errors"));
	}

	@Test void a05_sessionPostStatusTable() {
		var r = section("postStatus");
		assertJson("""
			{"state":"failed","nextRetryMs":0,"error":"E-JS-53","codes":["E-JS-53"],
			 "message":"bridge 's': session refused (403): bus:topic-denied: not granted: denied app.ev"}""", r.get("s403"));
		assertJson("""
			{"state":"failed","nextRetryMs":0,"error":"E-JS-53","codes":["E-JS-53"],
			 "message":"bridge 's': session refused (400): bus:bad-request"}""", r.get("s400"));
		// 429 honours Retry-After (7 s beats the 1 s backoff); 5xx retries with backoff. Neither is reported.
		assertJson("""
			{"state":"reconnecting","nextRetryMs":7000,"error":"E-JS-53","codes":[],
			 "message":"bridge 's': session refused (429): retrying"}""", r.get("s429"));
		assertJson("""
			{"state":"reconnecting","nextRetryMs":1000,"error":"E-JS-53","codes":[],
			 "message":"bridge 's': session refused (503): retrying"}""", r.get("s503"));
		assertJson("""
			{"state":"failed","nextRetryMs":0,"error":"E-JS-56","codes":["E-JS-56"],
			 "message":"bridge 's': transport 'sse' unavailable: server answered 501 (bus:transport-disabled)"}""", r.get("s501"));
	}

	@Test void a06_streamStatusTable() {
		var r = section("streamStatus");
		assertJson("{\"state\":\"failed\",\"nextRetryMs\":0,\"error\":\"E-JS-53\",\"codes\":[\"E-JS-53\"]}", r.get("s403"));
		assertJson("{\"state\":\"reconnecting\",\"nextRetryMs\":12000,\"error\":\"E-JS-56\",\"codes\":[]}", r.get("s429"));
		assertJson("{\"state\":\"reconnecting\",\"nextRetryMs\":1000,\"error\":\"E-JS-56\",\"codes\":[]}", r.get("s500"));
		// 200 with no ReadableStream body: this browser cannot run the SSE source.
		assertJson("{\"state\":\"failed\",\"nextRetryMs\":0,\"error\":\"E-JS-56\",\"codes\":[\"E-JS-56\"]}", r.get("noBody"));
	}

	@Test void a07_sessionBodyIsCheckedAgainstTheRequest() {
		var r = section("sessionBody");
		assertJson("""
			{"state":"failed","codes":["E-JS-53"],"fetches":1,
			 "message":"bridge 's': session refused (bad-response): malformed session response"}""", r.get("badId"));
		assertJson("""
			{"state":"failed","codes":["E-JS-53"],"fetches":1,
			 "message":"bridge 's': session refused (grant): server granted [app.a], page requested [app.a,app.ev]"}""", r.get("grant"));
		assertJson("""
			{"state":"failed","codes":["E-JS-53"],"fetches":1,
			 "message":"bridge 's': session refused (retain): topic 'app.a' is retain=false on the server but retain=true on the page"}""",
			r.get("retain"));
		// A server-sent stream path on another origin is never fetched.
		assertJson("""
			{"state":"failed","codes":["E-JS-56"],"fetches":1,
			 "message":"bridge 's': transport 'sse' unavailable: server sent no same-origin sse path"}""", r.get("crossOriginStream"));
	}

	@Test void a08_csrfHeaderNameBlankTokenAndNoAttribute() {
		var r = section("csrf");
		assertEquals("tok-9", r.get("customHeader"));
		assertEquals(true, r.get("defaultAbsent"));
		// A stamped-but-blank token fails closed before any request.
		assertJson("""
			{"fetches":0,"state":"failed","codes":["E-JS-53"],
			 "message":"bridge 's': session refused (csrf): blank CSRF token on <body data-juneau-csrf>"}""", r.get("blank"));
		assertJson("[\"Accept\",\"Content-Type\"]", r.get("none"));
	}

	@Test void a09_sessionUrlMustBeSameOrigin() {
		var r = section("crossOrigin");
		assertJson("0", r.get("fetches"));
		assertJson("[\"E-JS-52\",\"E-JS-52\"]", r.get("codes"));
		assertEquals("bridge 's': session URL 'https://other.example.test/rest/ops/juneau-bus/session' is not same-origin", r.get("message"));
	}

	@Test void a10_watchdogFiresAtThreeHeartbeats() {
		var r = section("watchdog");
		assertJson("{\"state\":\"open\",\"cancelled\":false,\"error\":null}", r.get("at44999"));
		assertJson("""
			{"state":"reconnecting","cancelled":true,"error":"bridge 's': transport 'sse' unavailable: no frame for 45000ms"}""",
			r.get("at45000"));
		assertEquals("open", r.get("fedByComment"));
	}

	@Test @SuppressWarnings("unchecked")
	void a11_perMessageProblemsKeepTheStreamUp() {
		var r = section("perMessage");
		assertJson("[\"E-JS-58\",\"E-JS-55\",\"E-JS-59\",\"E-JS-54\"]", r.get("codes"));
		var m = (List<String>) r.get("messages");
		// The E-JS-58 detail is the engine's JSON.parse text, so only its prefix is pinned.
		assertTrue(m.get(0).startsWith("bridge 's': bad frame (not-json): "), m::toString);
		assertEquals(List.of(
			"bridge 's': upstream 'app.up' was refused by the server: bus:upstream-denied not granted",
			"bridge 's': server error bus:boom: kaput",
			"bridge 's' delivered 'app.secret', which it was not granted; dropped"
		), m.subList(1, 4));
		assertJson("[false,false,false,false]", r.get("banners"));
		assertEquals("open", r.get("state"));
		// 'event: other' and default 'message' events are not bus frames: app.a keeps its resync value.
		assertJson("{\"n\":1}", r.get("a"));
		assertNull(r.get("secret"));
		assertEquals(false, r.get("cancelled"));
	}

	@Test void a12_detachCancelsTheReaderAndClears() {
		assertJson("{\"cancelled\":true,\"a\":null,\"bridge\":null}", section("detach"));
	}

	@Test void a13_csrfFromOptionsAndBlankOptionToken() {
		var r = section("csrfOpts");
		assertJson("{\"Accept\":\"application/json\",\"Content-Type\":\"application/json\",\"X-Own\":\"tok-own\"}", r.get("headers"));
		assertJson("{\"fetches\":0,\"state\":\"failed\",\"codes\":[\"E-JS-53\"]}", r.get("blank"));
	}

	@Test void a14_controlCharactersAndBackslashesNeverPassTheSameOriginCheck() {
		assertJson("{\"fetches\":0,\"codes\":[\"E-JS-52\",\"E-JS-52\",\"E-JS-52\",\"E-JS-52\"]}", section("controlChars"));
	}

	@Test void a15_detachWhileTheStreamGetIsPendingAbortsAndCancels() {
		assertJson("{\"hasSignal\":true,\"abortedBefore\":false,\"abortedAfter\":true,\"cancelled\":true}", section("detachPending"));
	}

	@Test void a16_oversizedSseEventIsDroppedWithE_JS_58AndTheStreamStaysUp() {
		var r = section("oversize");
		assertJson("[\"E-JS-58\",\"E-JS-58\"]", r.get("codes"));
		assertEquals("open", r.get("state"));
		assertJson("{\"n\":2}", r.get("a"));
	}

	@Test void a17_aRejectedReadIsARetryableTransportClose() {
		assertJson("""
			{"state":"reconnecting","nextRetryMs":1000,"error":"bridge 's': transport 'sse' unavailable: network reset","codes":[]}""", section("readFails"));
	}
}
