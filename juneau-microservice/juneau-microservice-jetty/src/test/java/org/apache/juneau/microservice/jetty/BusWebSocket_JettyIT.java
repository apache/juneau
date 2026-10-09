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
package org.apache.juneau.microservice.jetty;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.bus.websocket.*;
import org.apache.juneau.rest.server.servlet.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.ee11.websocket.jakarta.server.config.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * A real Jetty 12 (ee11) + {@code jetty-ee11-websocket-jakarta-server} round trip for the bus's WebSocket transport:
 * session POST, Origin-checked upgrade, resync, a live frame, an upstream frame reaching its handler, and a missing
 * Origin refused with 403 before the upgrade.
 *
 * <p>
 * Binds a socket, so it is off by default.  Run it with {@code -Djuneau.busIt=true}, outside the sandbox.
 */
@EnabledIfSystemProperty(named=BusWebSocket_JettyIT.GATE, matches="true",
	disabledReason="binds a socket; run with -Dtest='BusWebSocket_JettyIT' -Djuneau.busIt=true")
class BusWebSocket_JettyIT extends TestBase {

	static final String GATE = "juneau.busIt";

	private static final CountDownLatch UPSTREAM = new CountDownLatch(1);
	private static Server server;
	private static String origin;
	private static ServerBus bus;

	@Rest
	public static class BusRest extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;
		@Override public ServerBus serverBus() { return bus; }
	}

	@BeforeAll static void start() throws Exception {
		server = new Server();
		var connector = new ServerConnector(server);
		connector.setHost("127.0.0.1");
		connector.setPort(0);
		server.addConnector(connector);
		connector.open();                       // the port, and so the exact Origin, is known before start
		origin = "http://127.0.0.1:" + connector.getLocalPort();
		bus = ServerBus.create(BusPolicy.create()
			.downstream("ops.jobs", true)
			.downstream("ops.audit", true)
			.upstream("ops.cancel-all", (s, t, p) -> UPSTREAM.countDown())
			.allowedOrigins(origin)
			.build());
		var ctx = new ServletContextHandler();
		ctx.setContextPath("/");
		ctx.addServlet(new ServletHolder(new BusRest()), "/rest/*");
		JakartaWebSocketServletContainerInitializer.configure(ctx, (sc, container) -> BusWebSockets.register(container, bus));
		server.setHandler(ctx);
		server.start();
		bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", List.of()));
		bus.publish("ops.audit", JsonMap.of("schemaVersion", 1, "entries", List.of()));
	}

	@AfterAll static void stop() throws Exception {
		if (bus != null)
			bus.close();
		if (server != null)
			server.stop();
	}

	private static JsonMap openSession(HttpClient http) throws Exception {
		return openSession(http, "ops.jobs");
	}

	private static JsonMap openSession(HttpClient http, String downstream) throws Exception {
		var body = "{\"v\":1,\"bridge\":\"ops\",\"transport\":\"websocket\",\"downstream\":[\"" + downstream + "\"],\"upstream\":[\"ops.cancel-all\"]}";
		var res = http.send(HttpRequest.newBuilder(URI.create(origin + "/rest" + BusEventsMixin.SESSION_PATH))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.header("Origin", origin)
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		return JsonMap.ofString(res.body());
	}

	private static URI wsUri(String path) {
		return URI.create("ws://127.0.0.1:" + URI.create(origin).getPort() + path);
	}

	/** Collects whole text frames. */
	static final class Frames implements WebSocket.Listener {
		final BlockingQueue<JsonMap> frames = new LinkedBlockingQueue<>();
		final CompletableFuture<Integer> closed = new CompletableFuture<>();
		private final StringBuilder partial = new StringBuilder();

		@Override public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
			partial.append(data);
			if (last) {
				try {
					frames.add(JsonMap.ofString(partial.toString()));
				} catch (Exception e) {
					closed.completeExceptionally(e);
				}
				partial.setLength(0);
			}
			ws.request(1);
			return null;
		}

		@Override public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
			closed.complete(statusCode);
			return null;
		}

		JsonMap next() throws InterruptedException {
			var f = frames.poll(10, TimeUnit.SECONDS);
			assertNotNull(f, "no frame within 10s");
			return f;
		}
	}

	@Test void a01_roundTrip() throws Exception {
		var http = HttpClient.newHttpClient();
		var grant = openSession(http);
		var id = grant.getString("sessionId");
		assertEquals(64, id.length());
		var wsPath = BusWebSockets.PREFIX + "/" + id;
		assertEquals(wsPath, grant.getMap("paths").getString("websocket"));

		var frames = new Frames();
		var ws = http.newWebSocketBuilder()
			.header("Origin", origin)
			.buildAsync(wsUri(wsPath), frames)
			.get(10, TimeUnit.SECONDS);

		assertEquals("resync-begin", frames.next().getString("type"));
		var retained = frames.next();
		assertEquals("pub", retained.getString("type"));
		assertEquals("ops.jobs", retained.getString("topic"));
		assertEquals(Boolean.TRUE, retained.get("retained"));
		assertEquals("resync-end", frames.next().getString("type"));

		bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", List.of(JsonMap.of("jobId", "j-1"))));
		var live = frames.next();
		assertEquals("pub", live.getString("type"));
		assertEquals("ops.jobs", live.getString("topic"));

		ws.sendText("{\"v\":1,\"type\":\"pub\",\"topic\":\"ops.cancel-all\",\"payload\":{}}", true).get(10, TimeUnit.SECONDS);
		assertTrue(UPSTREAM.await(10, TimeUnit.SECONDS), "upstream frame never reached its UpstreamHandler");

		ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
	}

	@Test void b01_missingOriginIs403BeforeUpgrade() throws Exception {
		var http = HttpClient.newHttpClient();
		var id = openSession(http).getString("sessionId");
		var e = assertThrows(ExecutionException.class, () -> http.newWebSocketBuilder()
			.buildAsync(wsUri(BusWebSockets.PREFIX + "/" + id), new Frames())
			.get(10, TimeUnit.SECONDS));
		var hs = assertInstanceOf(WebSocketHandshakeException.class, e.getCause());
		assertEquals(403, hs.getResponse().statusCode());
	}

	@Test void b02_unknownCapabilityClosesWith4401() throws Exception {
		var http = HttpClient.newHttpClient();
		var frames = new Frames();
		http.newWebSocketBuilder()
			.header("Origin", origin)
			.buildAsync(wsUri(BusWebSockets.PREFIX + "/" + "0".repeat(64)), frames)
			.get(10, TimeUnit.SECONDS);
		assertEquals(4401, frames.closed.get(10, TimeUnit.SECONDS));
	}

	/** Two handshakes in flight at once must not see each other's session: the container copies the user properties per handshake. */
	@Test void c01_concurrentSessionsReceiveOnlyTheirOwnFrames() throws Exception {
		var http = HttpClient.newHttpClient();
		var jobs = openSession(http, "ops.jobs").getString("sessionId");
		var audit = openSession(http, "ops.audit").getString("sessionId");
		var jobsFrames = new Frames();
		var auditFrames = new Frames();
		var jobsWs = http.newWebSocketBuilder().header("Origin", origin)
			.buildAsync(wsUri(BusWebSockets.PREFIX + "/" + jobs), jobsFrames);
		var auditWs = http.newWebSocketBuilder().header("Origin", origin)
			.buildAsync(wsUri(BusWebSockets.PREFIX + "/" + audit), auditFrames);
		var a = jobsWs.get(10, TimeUnit.SECONDS);
		var b = auditWs.get(10, TimeUnit.SECONDS);

		for (var frames : List.of(jobsFrames, auditFrames)) {
			assertEquals("resync-begin", frames.next().getString("type"));
			var retained = frames.next();
			assertEquals(frames == jobsFrames ? "ops.jobs" : "ops.audit", retained.getString("topic"));
			assertEquals("resync-end", frames.next().getString("type"));
		}

		bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", List.of(JsonMap.of("jobId", "j-2"))));
		bus.publish("ops.audit", JsonMap.of("schemaVersion", 1, "entries", List.of(JsonMap.of("who", "jb"))));
		assertEquals("ops.jobs", jobsFrames.next().getString("topic"));
		assertEquals("ops.audit", auditFrames.next().getString("topic"));
		assertNull(jobsFrames.frames.poll(500, TimeUnit.MILLISECONDS), "the jobs socket saw a frame that was not its own");
		assertNull(auditFrames.frames.poll(500, TimeUnit.MILLISECONDS), "the audit socket saw a frame that was not its own");

		a.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
		b.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
	}
}
