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

import java.lang.reflect.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.bus.websocket.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.ee11.websocket.jakarta.server.config.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import jakarta.servlet.http.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;

/**
 * The bus's WebSocket handshake under a {@link CsrfBoundary}, over a real Jetty 12 (ee11) socket.
 *
 * <p>
 * Two connectors share one server: a plain one, and one with Jetty's {@link ForwardedRequestCustomizer}, the
 * container's own forwarded-header support.  A probe endpoint records what {@link HandshakeRequest#getRequestURI()}
 * reports on each, which is where the handshake takes its own scheme from.
 *
 * <p>
 * Binds sockets, so it is off by default.  Run it with {@code -Djuneau.busIt=true}, outside the sandbox.
 */
@EnabledIfSystemProperty(named=BusWebSocketCsrf_JettyIT.GATE, matches="true",
	disabledReason="binds a socket; run with -Dtest='BusWebSocketCsrf_JettyIT' -Djuneau.busIt=true")
class BusWebSocketCsrf_JettyIT extends TestBase {

	static final String GATE = "juneau.busIt";

	private static final BlockingQueue<String> PROBED = new LinkedBlockingQueue<>();
	private static Server server;
	private static int plainPort;
	private static int forwardedPort;
	private static ServerBus bus;
	private static CsrfBoundary boundary;

	@Rest
	public static class BusRest extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;
		@Override public ServerBus serverBus() { return bus; }
	}

	/** Accepts and does nothing; the probe configurator is what matters. */
	public static class ProbeEndpoint extends Endpoint {
		@Override public void onOpen(jakarta.websocket.Session session, EndpointConfig config) {}
	}

	/** Records the handshake's request URI as the container reports it. */
	static final class ProbeConfigurator extends ServerEndpointConfig.Configurator {
		@Override public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
			PROBED.add(String.valueOf(request.getRequestURI()));
		}
	}

	@BeforeAll static void start() throws Exception {
		server = new Server();
		var plain = new ServerConnector(server);
		plain.setHost("127.0.0.1");
		plain.setPort(0);
		server.addConnector(plain);
		var config = new HttpConfiguration();
		config.addCustomizer(new ForwardedRequestCustomizer());
		var forwarded = new ServerConnector(server, new HttpConnectionFactory(config));
		forwarded.setHost("127.0.0.1");
		forwarded.setPort(0);
		server.addConnector(forwarded);

		boundary = CsrfBoundary.create().tokens(CsrfTokens.perProcess()).build();
		bus = ServerBus.create(BusPolicy.create()
			.downstream("ops.jobs", true)
			.boundary(boundary)
			.build());
		var ctx = new ServletContextHandler();
		ctx.setContextPath("/");
		ctx.addServlet(new ServletHolder(new BusRest()), "/rest/*");
		JakartaWebSocketServletContainerInitializer.configure(ctx, (sc, container) -> {
			BusWebSockets.register(container, bus);
			container.addEndpoint(ServerEndpointConfig.Builder.create(ProbeEndpoint.class, "/probe")
				.configurator(new ProbeConfigurator())
				.build());
		});
		server.setHandler(ctx);
		server.start();
		plainPort = plain.getLocalPort();
		forwardedPort = forwarded.getLocalPort();
		bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", List.of()));
	}

	@AfterAll static void stop() throws Exception {
		if (bus != null)
			bus.close();
		if (server != null)
			server.stop();
	}

	private static String origin(int port) {
		return "http://127.0.0.1:" + port;
	}

	/** The process-wide token; the per-process source does not look at the request. */
	private static String token() {
		var req = (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
			(proxy, method, args) -> method.getReturnType() == boolean.class ? (Object) false : null);
		return boundary.tokenFor(req);
	}

	/** Opens a WebSocket session through the plain connector and returns its WebSocket path. */
	private static String openSession(HttpClient http) throws Exception {
		var body = "{\"v\":1,\"bridge\":\"ops\",\"transport\":\"websocket\",\"downstream\":[\"ops.jobs\"],\"upstream\":[]}";
		var res = http.send(HttpRequest.newBuilder(URI.create(origin(plainPort) + "/rest" + BusEventsMixin.SESSION_PATH))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.header("Origin", origin(plainPort))
			.header(boundary.csrfHeader(), token())
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		return JsonMap.ofString(res.body()).getMap("paths").getString("websocket");
	}

	private static URI wsUri(int port, String path) {
		return URI.create("ws://127.0.0.1:" + port + path);
	}

	/** Collects whole text frames and the close code. */
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
	}

	/** The HTTP status of a handshake the container refused before the upgrade. */
	private static int handshakeStatus(HttpClient http, URI uri, String...headers) {
		var b = http.newWebSocketBuilder();
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		var e = assertThrows(ExecutionException.class, () -> b.buildAsync(uri, new Frames()).get(10, TimeUnit.SECONDS));
		return assertInstanceOf(WebSocketHandshakeException.class, e.getCause()).getResponse().statusCode();
	}

	/**
	 * A well-formed cross-site Origin.  {@code checkOrigin} sees only the Origin value, not the Host it must match, so
	 * the upgrade completes (101) and the endpoint closes at once with {@code 4403} before any frame.
	 */
	@Test void a01_crossSiteWsUpgradeCloses4403BeforeAnyFrame() throws Exception {
		var http = HttpClient.newHttpClient();
		var path = openSession(http);
		var frames = new Frames();
		http.newWebSocketBuilder()
			.header("Origin", "http://evil.example")
			.buildAsync(wsUri(plainPort, path), frames)
			.get(10, TimeUnit.SECONDS);
		assertEquals(4403, frames.closed.get(10, TimeUnit.SECONDS));
		assertTrue(frames.frames.isEmpty(), "a refused handshake saw a frame");
	}

	@Test void a02_originNullRefused() throws Exception {
		var http = HttpClient.newHttpClient();
		assertEquals(403, handshakeStatus(http, wsUri(plainPort, openSession(http)), "Origin", "null"));
	}

	@Test void a03_missingOriginRefused() throws Exception {
		var http = HttpClient.newHttpClient();
		assertEquals(403, handshakeStatus(http, wsUri(plainPort, openSession(http))));
	}

	@Test void a04_sameOriginWsConnects() throws Exception {
		var http = HttpClient.newHttpClient();
		var frames = new Frames();
		var ws = http.newWebSocketBuilder()
			.header("Origin", origin(plainPort))
			.buildAsync(wsUri(plainPort, openSession(http)), frames)
			.get(10, TimeUnit.SECONDS);
		var first = frames.frames.poll(10, TimeUnit.SECONDS);
		assertNotNull(first, "no frame within 10s");
		assertEquals("resync-begin", first.getString("type"));
		ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
	}

	/**
	 * A {@code wss} page behind a TLS-terminating proxy: the proxy sends {@code X-Forwarded-Proto: https}, and Jetty's
	 * {@link ForwardedRequestCustomizer} must carry that into the handshake's request URI for the page's
	 * {@code https} Origin to match.
	 */
	@Test void a05_wssBehindNativeForwardingResolvesScheme() throws Exception {
		var http = HttpClient.newHttpClient();
		var frames = new Frames();
		var ws = http.newWebSocketBuilder()
			.header("Origin", "https://127.0.0.1:" + forwardedPort)
			.header("X-Forwarded-Proto", "https")
			.buildAsync(wsUri(forwardedPort, openSession(http)), frames)
			.get(10, TimeUnit.SECONDS);
		var first = frames.frames.poll(10, TimeUnit.SECONDS);
		assertNotNull(first, "no frame within 10s; closed with " + frames.closed.getNow(null));
		assertEquals("resync-begin", first.getString("type"));
		ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
	}

	/** Opens a socket on the forwarded connector with the given headers and returns the first frame, after which it closes. */
	private static Frames openForwarded(String...headers) throws Exception {
		var http = HttpClient.newHttpClient();
		var frames = new Frames();
		var b = http.newWebSocketBuilder();
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		var ws = b.buildAsync(wsUri(forwardedPort, openSession(http)), frames).get(10, TimeUnit.SECONDS);
		var first = frames.frames.poll(10, TimeUnit.SECONDS);
		assertNotNull(first, "no frame within 10s; closed with " + frames.closed.getNow(null));
		assertEquals("resync-begin", first.getString("type"));
		assertNull(frames.closed.getNow(null), "the socket was closed");
		ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
		return frames;
	}

	/**
	 * Jetty's {@link ForwardedRequestCustomizer} rewrites the {@code Host} the handshake sees from
	 * {@code X-Forwarded-Host}, so when the container is configured to honor forwarded headers the forwarded host is
	 * trusted: the Origin and that host match and the socket is admitted.  Enable the customizer only behind a proxy
	 * that overwrites {@code X-Forwarded-*}.  A browser cannot set the header, so this is not reachable cross-site.
	 */
	@Test void a07_forwardedHostFromContainerCustomizerIsTrusted() throws Exception {
		openForwarded("Origin", "http://evil.example", "X-Forwarded-Host", "evil.example");
	}

	/** As {@code a07}, with an {@code https} Origin carried by {@code X-Forwarded-Proto} as well. */
	@Test void a08_forwardedHostAndProtoFromContainerCustomizerAreTrusted() throws Exception {
		openForwarded("Origin", "https://evil.example", "X-Forwarded-Host", "evil.example", "X-Forwarded-Proto", "https");
	}

	/** The real cross-site row on the customizer connector: no forwarded host, so the Host is the connector's own. */
	@Test void a07c_crossSiteOriginWithoutForwardedHostRefused() throws Exception {
		var http = HttpClient.newHttpClient();
		var frames = new Frames();
		http.newWebSocketBuilder()
			.header("Origin", "http://evil.example")
			.buildAsync(wsUri(forwardedPort, openSession(http)), frames)
			.get(10, TimeUnit.SECONDS);
		assertEquals(4403, frames.closed.get(10, TimeUnit.SECONDS));
		assertTrue(frames.frames.isEmpty(), "a refused handshake saw a frame");
	}

	/**
	 * What Jetty reports from {@code HandshakeRequest.getRequestURI()}, plain and behind a forwarded rewrite: always an
	 * {@code http} or {@code https} URI, never {@code ws} or {@code wss}, with the forwarded proto applied.
	 */
	@Test void a06_requestUriBehindForwardedRewriteRecorded() throws Exception {
		var http = HttpClient.newHttpClient();
		PROBED.clear();
		http.newWebSocketBuilder().buildAsync(wsUri(plainPort, "/probe"), new Frames()).get(10, TimeUnit.SECONDS)
			.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
		var plain = PROBED.poll(10, TimeUnit.SECONDS);
		http.newWebSocketBuilder().header("X-Forwarded-Proto", "https").buildAsync(wsUri(forwardedPort, "/probe"), new Frames())
			.get(10, TimeUnit.SECONDS).sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
		var forwarded = PROBED.poll(10, TimeUnit.SECONDS);
		// Jetty reports the HTTP scheme of the upgrade request, not ws/wss, and carries the forwarded proto into it.
		assertEquals("http://127.0.0.1:" + plainPort + "/probe", plain);
		assertEquals("https://127.0.0.1:" + forwardedPort + "/probe", forwarded);
	}
}
