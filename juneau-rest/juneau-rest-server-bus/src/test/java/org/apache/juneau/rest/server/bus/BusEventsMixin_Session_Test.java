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
package org.apache.juneau.rest.server.bus;

import static org.apache.juneau.test.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.JsonParser;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Drives {@link BusEventsMixin} through a {@link MockRestClient}: the session POST's grant and
 * refusals, the SSE stream's resync and live frames, the 404 cases, and the boundary checks.
 */
@SuppressWarnings({
	"resource" // The shared MockRestClient lives for the whole class; each test's bus is closed in @AfterEach.
})
class BusEventsMixin_Session_Test extends TestBase {

	@Rest
	public static class R extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;
		static volatile ServerBus bus;
		@Override public ServerBus serverBus() { return bus; }
	}

	private static final MockRestClient C = MockRestClient.buildLax(R.class);
	private static final Principal JB = () -> "jb";
	private static final Principal EVE = () -> "eve";

	private static final String AUTHORITY = "127.0.0.1:8790";
	private static final SynchronizerToken TOKEN = SynchronizerToken.of("the-real-token");

	private static BusPolicy.Builder policy() {
		return BusPolicy.create().downstream("ops.jobs", true).downstream("ops.alert:*", false);
	}

	private static void use(BusPolicy p) {
		R.bus = ServerBus.create(p);
	}

	@BeforeEach void setUp() {
		use(policy().build());
	}

	@AfterEach void tearDown() {
		R.bus.close();
	}

	private static String body(String transport, String...downstream) {
		return Json.of(JsonMap.of("v", 1, "bridge", "ops", "transport", transport, "downstream", List.of(downstream)));
	}

	private static JsonMap post(Principal p, String json, int status) throws Exception {
		var text = C.post(BusEventsMixin.SESSION_PATH).userPrincipal(p).contentString(json).contentType("application/json")
			.header("Accept", "application/json").run().assertStatus(status).getContent().asString();
		return JsonMap.ofString(text, JsonParser.DEFAULT);
	}

	private static org.apache.juneau.rest.client.classic.RestResponse stream(Principal p, String id, int status) throws Exception {
		return C.get(BusEventsMixin.STREAM_PATH.replace("{sessionId}", id)).userPrincipal(p)
			.header("Accept", "text/event-stream").run().assertStatus(status);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) POST juneau-bus/session
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_theGrant() throws Exception {
		var g = post(JB, body("sse", "ops.jobs", "ops.alert:*"), 200);
		var id = g.getString("sessionId");
		assertTrue(id.matches("[0-9a-f]{64}"), id);
		assertEquals(1, ((Number) g.get("v")).intValue());
		assertEquals(15000, ((Number) g.get("heartbeatMs")).intValue());
		assertEquals(60000, ((Number) g.get("graceMs")).intValue());
		assertEquals(List.of(JsonMap.of("topic", "ops.jobs", "retain", true), JsonMap.of("topic", "ops.alert:*", "retain", false)),
			g.get("downstream"));
		assertEquals(List.of(), g.get("upstream"));
		assertEquals(JsonMap.of("sse", "/juneau-bus/stream/" + id), g.get("paths"), "no websocket path until enableWebSocket");

		var s = R.bus.sessions();
		assertSize(1, s);
		assertEquals("jb", s.get(0).principalName());
		assertEquals(id, ServerBus.idOf(s.get(0)));
	}

	@Test void a02_theWebSocketPathIsContextRelative() throws Exception {
		R.bus.enableWebSocket("/juneau-bus/ws");
		var g = post(JB, body("websocket", "ops.jobs"), 200);
		var id = g.getString("sessionId");
		assertEquals("/juneau-bus/ws/" + id, g.getMap("paths").getString("websocket"));
		assertEquals("/juneau-bus/stream/" + id, g.getMap("paths").getString("sse"));
	}

	@Test void a02b_theSsePathIsRootRelativeAndTheWebSocketPathIsContextRelative() throws Exception {
		R.bus.enableWebSocket("/juneau-bus/ws");
		var c = MockRestClient.create(R.class).contextPath("/ctx").ignoreErrors().noTrace().build();
		var text = c.post(BusEventsMixin.SESSION_PATH).userPrincipal(JB).contentString(body("websocket", "ops.jobs"))
			.contentType("application/json").header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		var g = JsonMap.ofString(text, JsonParser.DEFAULT);
		var id = g.getString("sessionId");
		assertEquals("/ctx/juneau-bus/ws/" + id, g.getMap("paths").getString("websocket"));
		assertEquals("/ctx/juneau-bus/stream/" + id, g.getMap("paths").getString("sse"));
	}

	@Test void a03_aDeniedTopicIs403WithTheList() throws Exception {
		var r = post(JB, body("sse", "ops.jobs", "ops.secret"), 403);
		assertEquals("bus:topic-denied", r.getString("code"));
		assertEquals(List.of("ops.secret"), r.get("denied"));
		assertTrue(R.bus.sessions().isEmpty());
	}

	@Test void a04_malformedBodiesAre400() throws Exception {
		for (var b : List.of("not json", "[1]", "{}", "", "{\"bridge\":\"ops\",\"downstream\":\"ops.jobs\"}"))
			assertEquals("bus:bad-request", post(JB, b, 400).getString("code"), b);
	}

	@Test void a05_atMaxSessionsIs429WithRetryAfter() throws Exception {
		R.bus.close();
		use(policy().maxSessions(1).build());
		post(JB, body("sse", "ops.jobs"), 200);
		var r = C.post(BusEventsMixin.SESSION_PATH).userPrincipal(JB).contentString(body("sse", "ops.jobs"))
			.contentType("application/json").header("Accept", "application/json").run().assertStatus(429);
		r.assertHeader("Retry-After").is("5");
		assertTrue(r.getContent().asString().contains("bus:too-many-sessions"));
	}

	@Test void a06_websocketWithoutRegistrationIs501() throws Exception {
		assertEquals("bus:transport-unavailable", post(JB, body("websocket", "ops.jobs"), 501).getString("code"));
	}

	@Test void a07_aBodyOver64KbIs400() throws Exception {
		// Otherwise a valid request: only its size is wrong.
		var big = body("sse", "ops.jobs") + " ".repeat(BusStream.MAX_SESSION_BODY_BYTES);
		assertEquals("bus:bad-request", post(JB, big, 400).getString("code"));
		assertTrue(R.bus.sessions().isEmpty());
		post(JB, body("sse", "ops.jobs"), 200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) GET juneau-bus/stream/<sessionId>
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"java:S2925" // Deadline-bounded poll for the stream to connect
	})
	@Test void b01_resyncThenLiveThenClose() throws Exception {
		R.bus.publish("ops.jobs", JsonMap.of("n", 1));
		var id = post(JB, body("sse", "ops.jobs", "ops.alert:*"), 200).getString("sessionId");

		// The GET holds the request thread until the stream ends, so a helper publishes and then closes the bus.
		var failure = new AtomicReference<Throwable>();
		var helper = new Thread(() -> {
			try {
				var deadline = System.nanoTime() + 10_000_000_000L;
				while (! R.bus.sessions().get(0).isConnected()) {
					if (System.nanoTime() > deadline)
						throw new AssertionError("the stream never connected");
					Thread.sleep(10);
				}
				R.bus.publish("ops.alert:east", JsonMap.of("msg", "live"));
				R.bus.close();
			} catch (Throwable t) {
				failure.set(t);
				R.bus.close();
			}
		});
		helper.start();
		var res = stream(JB, id, 200);
		var body = res.getContent().asString();
		helper.join();
		assertNull(failure.get());
		res.assertHeader("Content-Type").isContains("text/event-stream");
		res.assertHeader("Cache-Control").is("no-cache");
		res.assertHeader("X-Accel-Buffering").is("no");

		var marks = List.of("event: bus", "\"type\":\"resync-begin\"", "id: 1", "\"topic\":\"ops.jobs\"", "id: 2",
			"\"type\":\"resync-end\"", "id: 3", "\"msg\":\"live\"", "id: 4");
		var at = -1;
		for (var m : marks) {
			var next = body.indexOf(m, at + 1);
			assertTrue(next > at, "'" + m + "' missing or out of order in:\n" + body);
			at = next;
		}
		assertTrue(body.contains("\"retained\":true"), body);
		assertTrue(body.contains("\"retained\":false"), body);
	}

	@Test void b02_anUnknownIdIs404() throws Exception {
		stream(JB, "0".repeat(64), 404);
	}

	@Test void b03_anotherPrincipalIs404() throws Exception {
		var id = post(JB, body("sse", "ops.jobs"), 200).getString("sessionId");
		stream(EVE, id, 404);
		stream(null, id, 404);
		assertFalse(R.bus.sessions().get(0).isConnected());
	}

	@Test void b04_aSlowConsumerGetsTheErrorFrameLastAndTheStreamEnds() throws Exception {
		R.bus.close();
		use(policy().downstream("ops.other", true).maxQueuedFrames(1).build());
		R.bus.publish("ops.jobs", JsonMap.of("n", 1));
		R.bus.publish("ops.other", JsonMap.of("n", 2));
		var id = post(JB, body("sse", "ops.jobs", "ops.other"), 200).getString("sessionId");

		// The resync (begin, two retained pubs, end) overflows a one-frame queue, so connect() closes the sink with 4429.
		var body = stream(JB, id, 200).getContent().asString().stripTrailing();
		var last = body.substring(body.lastIndexOf("event: bus"));
		assertTrue(last.contains("\"code\":\"bus:slow-consumer\""), body);
		assertEquals(1, body.split("bus:slow-consumer", -1).length - 1, body);
		assertFalse(body.contains("\"type\":\"resync-end\""), body);
	}

	@SuppressWarnings({
		"java:S2925" // Deadline-bounded poll for the stream to connect
	})
	@Test void b05_aSecondStreamReplacesTheFirstQuietly() throws Exception {
		var id = post(JB, body("sse", "ops.jobs"), 200).getString("sessionId");
		var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
		try {
			var first = pool.submit(() -> stream(JB, id, 200).getContent().asString());
			var deadline = System.nanoTime() + 10_000_000_000L;
			while (! R.bus.sessions().get(0).isConnected()) {
				assertTrue(System.nanoTime() < deadline, "the first stream never connected");
				Thread.sleep(10);
			}
			var second = pool.submit(() -> stream(JB, id, 200).getContent().asString());
			// The second stream's connect replaces the first, which ends within about 50 ms and without an error frame.
			var firstBody = first.get(10, java.util.concurrent.TimeUnit.SECONDS);
			assertTrue(firstBody.contains("\"type\":\"resync-end\""), firstBody);
			assertFalse(firstBody.contains("bus:slow-consumer"), firstBody);
			assertFalse(second.isDone(), "the replacement keeps streaming");
			R.bus.close();
			assertTrue(second.get(10, java.util.concurrent.TimeUnit.SECONDS).contains("\"type\":\"resync-end\""));
		} finally {
			R.bus.close();
			pool.shutdownNow();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) BusPolicy.boundary: the LoopbackBoundary and CsrfBoundary checks
	//------------------------------------------------------------------------------------------------------------------

	private static void useBoundary() {
		R.bus.close();
		use(policy().boundary(LoopbackBoundary.create().authority(AUTHORITY).token(TOKEN).build()).build());
	}

	private static MockRestRequest boundaryPost(boolean csrf) throws Exception {
		var r = C.post(BusEventsMixin.SESSION_PATH).userPrincipal(JB).contentString(body("sse", "ops.jobs"))
			.contentType("application/json").header("Accept", "application/json")
			.header("Host", AUTHORITY).header("Origin", "http://" + AUTHORITY).header("Sec-Fetch-Site", "same-origin");
		return csrf ? r.header("X-Csrf-Token", TOKEN.value()) : r;
	}

	@Test void c01_aPostWithoutTheCsrfTokenIs403() throws Exception {
		useBoundary();
		var text = boundaryPost(false).run().assertStatus(403).getContent().asString();
		assertTrue(text.contains("bus:refused"), text);
		assertTrue(R.bus.sessions().isEmpty());
	}

	@Test void c02_aGoodPostIsGranted() throws Exception {
		useBoundary();
		var text = boundaryPost(true).run().assertStatus(200).getContent().asString();
		assertTrue(text.contains("sessionId"), text);
	}

	@Test void c03_theStreamChecksHost() throws Exception {
		useBoundary();
		C.get(BusEventsMixin.STREAM_PATH.replace("{sessionId}", "0".repeat(64))).userPrincipal(JB)
			.header("Accept", "text/event-stream").header("Host", "evil.example:8790").run().assertStatus(421);
		C.get(BusEventsMixin.STREAM_PATH.replace("{sessionId}", "0".repeat(64))).userPrincipal(JB)
			.header("Accept", "text/event-stream").header("Host", AUTHORITY).run().assertStatus(404);
	}

	@Test void c04_sessionPostWithMethodParamJudgedAsWrite() throws Exception {
		useBoundary();
		// A method parameter cannot turn the POST into a read: the request line is the floor.
		var text = C.post(BusEventsMixin.SESSION_PATH + "?method=GET").userPrincipal(JB).contentString(body("sse", "ops.jobs"))
			.contentType("application/json").header("Accept", "application/json")
			.header("Host", AUTHORITY).header("Origin", "http://" + AUTHORITY).header("Sec-Fetch-Site", "same-origin")
			.run().assertStatus(403).getContent().asString();
		assertTrue(text.contains("bus:refused"), text);
		assertTrue(R.bus.sessions().isEmpty());
		// A GET claiming DELETE on the stream is judged as the write it claims, so it needs an Origin.
		C.get(BusEventsMixin.STREAM_PATH.replace("{sessionId}", "0".repeat(64)) + "?method=DELETE").userPrincipal(JB)
			.header("Accept", "text/event-stream").header("Host", AUTHORITY).run().assertStatus(403);
	}

	private static final String APP_HOST = "app.example";
	private static final String APP_ORIGIN = "http://" + APP_HOST;

	private static CsrfBoundary useCsrfBoundary() {
		R.bus.close();
		var b = CsrfBoundary.create().tokens(CsrfTokens.perProcess()).allowedHosts(APP_HOST).build();
		use(policy().boundary(b).build());
		return b;
	}

	/** A token the boundary accepts: a per-process strategy never reads the request it is handed. */
	private static String tokenOf(CsrfBoundary b) {
		var req = (jakarta.servlet.http.HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(
			jakarta.servlet.http.HttpServletRequest.class.getClassLoader(), new Class<?>[]{jakarta.servlet.http.HttpServletRequest.class},
			(proxy, method, args) -> method.getReturnType() == boolean.class ? (Object) false : null);
		return b.tokenFor(req);
	}

	private static MockRestRequest csrfPost(String origin) throws Exception {
		return C.post(BusEventsMixin.SESSION_PATH).userPrincipal(JB).contentString(body("sse", "ops.jobs"))
			.contentType("application/json").header("Accept", "application/json")
			.header("Host", APP_HOST).header("Origin", origin);
	}

	@Test void c05_csrfBoundaryAcceptedAsPolicy() throws Exception {
		var b = useCsrfBoundary();
		assertSame(b, R.bus.policy().boundary());

		var ok = csrfPost(APP_ORIGIN).header("X-Csrf-Token", tokenOf(b)).run().assertStatus(200).getContent().asString();
		assertTrue(ok.contains("sessionId"), ok);

		var refused = JsonMap.ofString(csrfPost("http://evil.example").header("X-Csrf-Token", tokenOf(b)).run()
			.assertStatus(403).getContent().asString(), JsonParser.DEFAULT);
		assertEquals("bus:refused", refused.getString("code"));
		assertEquals(Set.of("code", "message"), refused.keySet(), "the reason name is not part of the bus refusal");
		assertFalse(refused.getString("message").contains("evil.example"), "the presented Origin is never echoed");
		assertSize(1, R.bus.sessions());
	}

	@Test void c05b_sameOriginPostWithoutTokenRefused() throws Exception {
		useCsrfBoundary();
		var text = csrfPost(APP_ORIGIN).run().assertStatus(403).getContent().asString();
		assertTrue(text.contains("bus:refused"), text);
		assertTrue(R.bus.sessions().isEmpty());
	}

	@Test void c06_streamAppliesCsrfBoundary() throws Exception {
		useCsrfBoundary();
		var stream = BusEventsMixin.STREAM_PATH.replace("{sessionId}", "0".repeat(64));
		// allowedHosts applies to the read as well.
		C.get(stream).userPrincipal(JB).header("Accept", "text/event-stream").header("Host", "rebind.example").run().assertStatus(421);
		C.get(stream).userPrincipal(JB).header("Accept", "text/event-stream").header("Host", APP_HOST).run().assertStatus(404);
		// A read claiming a write gets the write checks.
		C.get(stream + "?method=DELETE").userPrincipal(JB).header("Accept", "text/event-stream").header("Host", APP_HOST)
			.header("Origin", "http://evil.example").run().assertStatus(403);
	}
}
