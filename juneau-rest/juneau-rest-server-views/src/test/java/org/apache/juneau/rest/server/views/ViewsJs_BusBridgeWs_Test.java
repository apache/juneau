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
 * Behavioral coverage for the stock WebSocket source (spec §11.3 step 2b, §11.6) and the shell hook
 * {@code JuneauViews.bus.wiring.attachBridges} (§11.5).  {@code bus-bridge-ws.cjs} drives
 * {@code JuneauViews.bus.sources.websocket} with a fake {@code WebSocket}, a scripted session {@code fetch} and a fake
 * clock, and reports the URL built from {@code location}, upstream text frames, the close-code table, per-message
 * errors, the watchdog, detach, and {@code attachBridges}.  Gated on {@code node} being on {@code PATH}.
 */
class ViewsJs_BusBridgeWs_Test extends TestBase {

	private static final String SID = "b".repeat(64);

	private static Map<String,Object> report;

	@BeforeAll
	static void runHarness() throws Exception {
		report = BusHarness.run("bus-bridge-ws.cjs", "/org/apache/juneau/views/juneau-bus.js");
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

	/** One row of the close-code table: what bridge:w looked like after the server closed with {@code code}. */
	private static void assertClose(String code, String state, int nextRetryMs, String error, String reported, String message,
			int posts, int sockets) {
		var r = section("close", "c" + code);
		var why = "close " + code + ": " + r;
		assertEquals(state, r.get("state"), why);
		assertEquals(nextRetryMs, ((Number) r.get("nextRetryMs")).intValue(), why);
		assertEquals(error, r.get("error"), why);
		assertEquals(reported == null ? List.of() : List.of(reported), r.get("codes"), why);
		assertEquals(message, r.get("message"), why);
		assertEquals(posts, ((Number) r.get("posts")).intValue(), why);
		assertEquals(sockets, ((Number) r.get("sockets")).intValue(), why);
	}

	@Test void a01_urlComesFromLocationAndUpstreamIsSentAsCanonicalText() {
		var r = section("happy");
		assertEquals("wss://console.example.test/rest/ops/juneau-bus/ws/" + SID, r.get("url"));
		assertEquals("ws://localhost:10000/rest/ops/juneau-bus/ws/" + SID, r.get("httpUrl"));
		assertJson("""
			{"v":1,"bridge":"w","transport":"websocket","downstream":["app.a"],"upstream":["app.up"]}""", r.get("post"));
		assertEquals("open", r.get("state"));
		assertJson("{\"n\":1}", r.get("a"));
		assertEquals(List.of("{\"v\":1,\"type\":\"pub\",\"topic\":\"app.up\",\"payload\":{\"x\":1}}"), r.get("sent"));
		assertJson("[]", r.get("errors"));
	}

	@Test void a02_upstreamBeforeOpenIsE_JS_55AndNothingIsSent() {
		var r = section("notOpen");
		assertJson("0", r.get("sent"));
		assertJson("[\"E-JS-55\"]", r.get("codes"));
		assertEquals("bridge 'w': upstream 'app.up' was refused by the server: bridge not open", r.get("message"));
	}

	@Test void a03_retryableCloseCodesBackOffOnTheSameSession() {
		for (var c : List.of("1001", "1006", "1011", "1013"))
			assertClose(c, "reconnecting", 1000, "E-JS-56", null,
				"bridge 'w': transport 'websocket' unavailable: closed (" + c + ")", 1, 2);
		assertClose("1000", "reconnecting", 1000, "E-JS-56", null,
			"bridge 'w': transport 'websocket' unavailable: closed (1000 bye)", 1, 2);
		assertClose("4429", "reconnecting", 1000, "E-JS-56", null,
			"bridge 'w': transport 'websocket' unavailable: closed (4429 bus:rate-limited)", 1, 2);
	}

	@Test void a04_close4401RePostsAtOnce() {
		assertClose("4401", "reconnecting", 0, "E-JS-53", null, "bridge 'w': session refused (4401): bus:unknown-session", 2, 2);
	}

	@Test void a05_policyClosesFail() {
		assertClose("4403", "failed", 0, "E-JS-53", "E-JS-53", "bridge 'w': session refused (4403): bus:refused", 1, 1);
		assertClose("1008", "failed", 0, "E-JS-56", "E-JS-56",
			"bridge 'w': transport 'websocket' unavailable: closed by policy (1008)", 1, 1);
		assertClose("1009", "failed", 0, "E-JS-56", "E-JS-56",
			"bridge 'w': transport 'websocket' unavailable: closed by policy (1009 frame too large)", 1, 1);
	}

	@Test void a06_close4409IsQuiet() {
		// Another tab took over this session: closed, recorded on bridge:w, never reported or retried.
		assertClose("4409", "closed", 0, "E-JS-56", null,
			"bridge 'w': transport 'websocket' unavailable: replaced by another connection (4409 bus:replaced)", 1, 1);
	}

	@Test void a07_threeCapabilityClosesInARowIsE_JS_57() {
		var r = section("capability");
		assertJson("3", r.get("posts"));
		assertEquals("failed", r.get("state"));
		assertJson("[\"E-JS-57\"]", r.get("codes"));
		assertEquals("bridge 'w' gave up after 3 attempts: capability refused 3 times in a row", r.get("message"));
	}

	@Test void a08_perMessageProblemsKeepTheSocketOpen() {
		var r = section("perMessage");
		assertJson("[\"E-JS-58\",\"E-JS-58\",\"E-JS-55\",\"E-JS-55\"]", r.get("codes"));
		assertEquals(List.of(
			"bridge 'w': bad frame (bad-version): v is 2, expected 1",
			"bridge 'w': bad frame (not-json): binary message",
			"bridge 'w': upstream 'app.up' was refused by the server: bus:rate-limited slow down",
			"bridge 'w': upstream 'app.up' was refused by the server: bus:upstream-failed handler threw"
		), r.get("messages"));
		assertEquals("open", r.get("state"));
		assertNull(r.get("closed"));
	}

	@Test void a09_watchdogClosesASilentSocket() {
		assertJson("{\"state\":\"reconnecting\",\"closedWith\":{\"code\":1000,\"reason\":\"detach\"}}", section("watchdog"));
	}

	@Test void a09b_watchdogIsArmedAtConnectAndEveryFrameRearmsIt() {
		var r = section("watchdogFeed");
		// A socket that never delivers a frame is closed too; a frame at 30s keeps a live one open at 60s.
		assertJson("{\"state\":\"reconnecting\",\"closedWith\":{\"code\":1000,\"reason\":\"detach\"}}", r.get("silent"));
		assertJson("{\"state\":\"open\",\"closedWith\":null}", r.get("afterFrame"));
	}

	@Test void a10_unavailableTransportFails() {
		var r = section("unavailable");
		assertJson("""
			{"state":"failed","codes":["E-JS-56"],"posts":0,
			 "message":"bridge 'w': transport 'websocket' unavailable: no WebSocket in this browser"}""", r.get("noWs"));
		assertJson("""
			{"state":"failed","codes":["E-JS-56"],
			 "message":"bridge 'w': transport 'websocket' unavailable: server answered 501 (bus:transport-disabled)"}""", r.get("s501"));
		// A protocol-relative path from the server is refused before any socket opens.
		assertJson("{\"state\":\"failed\",\"codes\":[\"E-JS-56\"],\"sockets\":0}", r.get("badPath"));
	}

	@Test void a11_detachClosesWith1000() {
		assertJson("{\"closedWith\":{\"code\":1000,\"reason\":\"detach\"},\"a\":null}", section("detach"));
	}

	@Test void a12_attachBridgesAttachesOnePerEntryAndDetachesOnPagehide() {
		var r = section("attachBridges");
		assertJson("""
			{"live":"sse","cmd":"websocket","odd":null,"links":2,"sockets":1,"codes":["E-JS-52"],
			 "message":"bridge 'odd': unknown transport 'carrier-pigeon'","banner":true,"sseMethods":["POST","GET"]}""",
			r.get("before"));
		assertJson("1", r.get("pagehideListeners"));
		assertJson("{\"live\":true,\"cmd\":true,\"socketClosed\":{\"code\":1000,\"reason\":\"detach\"}}", r.get("afterPagehide"));
		// A contract with no bridges[] attaches nothing (and registers no pagehide listener).
		assertJson("0", r.get("empty"));
		assertJson("3", r.get("fetchCalls"));
	}

	@Test void a13_pagehidePersistedKeepsTheBridgesAndAnUnloadDetachesThem() {
		var r = section("pagehidePersisted");
		assertJson("{\"state\":\"open\",\"closedWith\":null}", r.get("kept"));
		assertJson("{\"gone\":true,\"closedWith\":{\"code\":1000,\"reason\":\"detach\"}}", r.get("after"));
	}

	@Test void a14_noBridgeAttachedMeansNoPagehideListener() {
		var r = section("noListener");
		assertJson("{\"links\":0,\"listeners\":0,\"codes\":[],\"message\":null,\"banner\":null}", r.get("empty"));
		assertJson("{\"links\":0,\"listeners\":0,\"codes\":[],\"message\":null,\"banner\":null}", r.get("noBridges"));
		assertJson("""
			{"links":0,"listeners":0,"codes":["E-JS-52"],"message":"bridge 'odd': unknown transport 'carrier-pigeon'","banner":true}""",
			r.get("unknownOnly"));
	}

	@Test void a15_detachWhileThePostIsInFlightConstructsNoSocket() {
		assertJson("{\"posted\":1,\"sockets\":0,\"pending\":0,\"bridge\":null}", section("detachInFlight"));
	}

	@Test void a16_detachLeavesNoTimersWhetherOpenReconnectingOrPagehidden() {
		assertJson("""
			{"open":true,"reconnecting":{"state":"reconnecting","pending":1},"afterDetach":0,"pagehideBefore":true,"afterPagehide":0}""",
			section("noTimers"));
	}
}
