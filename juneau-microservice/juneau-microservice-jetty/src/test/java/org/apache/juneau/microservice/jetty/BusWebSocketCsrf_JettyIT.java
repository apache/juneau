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

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.bus.websocket.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.ee11.websocket.jakarta.server.config.*;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.hpack.*;
import org.eclipse.jetty.http2.server.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import jakarta.servlet.*;
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
	private static final BlockingQueue<String> HANDSHAKES = new LinkedBlockingQueue<>();
	private static final List<String> FILTERED = new CopyOnWriteArrayList<>();
	private static Server server;
	private static int plainPort;
	private static int forwardedPort;
	private static int h2cPort;
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

	/** Records the handshake's request URI as the container reports it, and which of the RFC 6455 headers it carried. */
	static final class ProbeConfigurator extends ServerEndpointConfig.Configurator {
		@Override public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
			PROBED.add(String.valueOf(request.getRequestURI()));
			var names = new HashSet<String>();
			for (var k : request.getHeaders().keySet())
				names.add(k.toLowerCase(Locale.ROOT));
			HANDSHAKES.add("host=" + names.contains("host") + ";key=" + names.contains("sec-websocket-key") + ";upgrade=" + names.contains("upgrade"));
		}
	}

	/** Records the path of every request a servlet filter sees; a WebSocket handshake is not among them. */
	static final class RecordingFilter implements Filter {
		@Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
			FILTERED.add(((HttpServletRequest)req).getRequestURI());
			chain.doFilter(req, res);
		}
	}

	/** A bus REST servlet over a bus of its own, for the servers that vary the boundary. */
	@Rest
	public static class OwnBusRest extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;
		private final transient ServerBus own;
		OwnBusRest(ServerBus own) { this.own = own; }
		@Override public ServerBus serverBus() { return own; }
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
		var h2c = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()), new HTTP2CServerConnectionFactory(new HttpConfiguration()));
		h2c.setHost("127.0.0.1");
		h2c.setPort(0);
		server.addConnector(h2c);

		boundary = CsrfBoundary.create().tokens(CsrfTokens.perProcess()).build();
		bus = ServerBus.create(BusPolicy.create()
			.downstream("ops.jobs", true)
			.boundary(boundary)
			.build());
		var ctx = new ServletContextHandler();
		ctx.setContextPath("/");
		ctx.addFilter(new FilterHolder(new RecordingFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
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
		h2cPort = h2c.getLocalPort();
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

	//-----------------------------------------------------------------------------------------------------------------
	// RFC 8441: WebSocket over HTTP/2 (extended CONNECT)
	//-----------------------------------------------------------------------------------------------------------------

	/** What an extended CONNECT drew: the server's SETTINGS, the response status, and the first data frame's payload. */
	record Ext(boolean connectProtocol, int status, byte[] first, boolean reset) {

		/** The WebSocket close code carried by the first data frame, or -1 if it is not a close frame. */
		int closeCode() {
			return first != null && first.length >= 4 && (first[0] & 0x0F) == 0x8 ? ((first[2] & 0xFF) << 8) | (first[3] & 0xFF) : -1;
		}

		/** The first data frame's payload as text, for matching a text frame. */
		String text() {
			return first == null ? "" : new String(first, StandardCharsets.ISO_8859_1);
		}
	}

	/**
	 * Sends one RFC 8441 extended CONNECT over a prior-knowledge h2c connection and reads until the first data frame, a
	 * non-200 response, or a reset.  The request is hand-encoded: the JDK client speaks no HTTP/2 WebSocket.
	 *
	 * @param awaitData Whether to wait for a data frame after a 200 (an endpoint that sends nothing never produces one).
	 * @param extra Further request fields as name, value pairs (names lower-case).
	 */
	private static Ext extendedConnect(int port, String authority, String path, String origin, boolean awaitData, String...extra) throws Exception {
		var out = new ByteArrayOutputStream();
		out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
		out.write(CsrfBoundary_JettyIT.frame(0x4, 0, 0, new byte[0]));
		var h = new ByteArrayOutputStream();
		CsrfBoundary_JettyIT.indexedName(h, 2, "CONNECT");   // :method
		CsrfBoundary_JettyIT.literal(h, ":protocol", "websocket");
		h.write(0x86);                                       // :scheme http
		CsrfBoundary_JettyIT.indexedName(h, 4, path);        // :path
		if (authority != null)
			CsrfBoundary_JettyIT.indexedName(h, 1, authority); // :authority
		if (origin != null)
			CsrfBoundary_JettyIT.literal(h, "origin", origin);
		CsrfBoundary_JettyIT.literal(h, "sec-websocket-version", "13");
		for (var i = 0; i < extra.length; i += 2)
			CsrfBoundary_JettyIT.literal(h, extra[i], extra[i + 1]);
		out.write(CsrfBoundary_JettyIT.frame(0x1, 0x4, 1, h.toByteArray())); // HEADERS: END_HEADERS only, so the stream stays open

		try (var socket = new Socket("127.0.0.1", port)) {
			socket.setSoTimeout(10_000);
			socket.getOutputStream().write(out.toByteArray());
			socket.getOutputStream().flush();
			var in = new DataInputStream(socket.getInputStream());
			var decoder = new HpackDecoder(8192, () -> 0L);
			var connectProtocol = false;
			var status = -1;
			for (;;) {
				var head = new byte[9];
				try {
					in.readFully(head);
				} catch (EOFException e) {
					return new Ext(connectProtocol, status, null, true);
				}
				var len = ((head[0] & 0xFF) << 16) | ((head[1] & 0xFF) << 8) | (head[2] & 0xFF);
				var payload = new byte[len];
				in.readFully(payload);
				switch (head[3]) {
					case 0x4 -> {
						if ((head[4] & 0x1) == 0) {
							for (var i = 0; i + 6 <= len; i += 6)
								if ((((payload[i] & 0xFF) << 8) | (payload[i + 1] & 0xFF)) == 0x8)
									connectProtocol = payload[i + 5] == 1;
							socket.getOutputStream().write(CsrfBoundary_JettyIT.frame(0x4, 0x1, 0, new byte[0]));  // ACK
							socket.getOutputStream().flush();
						}
					}
					case 0x1 -> {
						var meta = (MetaData.Response)decoder.decode(java.nio.ByteBuffer.wrap(payload));
						status = meta.getStatus();
						if (status != 200 || ! awaitData)
							return new Ext(connectProtocol, status, null, false);
					}
					case 0x0 -> {
						return new Ext(connectProtocol, status, payload, false);
					}
					case 0x3, 0x7 -> {
						return new Ext(connectProtocol, status, null, true);
					}
					default -> {}
				}
			}
		}
	}

	/** Reads a whole HTTP/1 response (head and body) from a raw socket, to see what the container did with a request. */
	private static String raw(int port, String request) throws Exception {
		try (var socket = new Socket("127.0.0.1", port)) {
			socket.setSoTimeout(10_000);
			socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
			socket.getOutputStream().flush();
			try {
				return new String(socket.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
			} catch (SocketTimeoutException e) {
				return "(open)";
			}
		}
	}

	private static String authority(int port) {
		return "127.0.0.1:" + port;
	}

	@Test void h01_serverAdvertisesExtendedConnect() throws Exception {
		var path = openSession(HttpClient.newHttpClient());
		var res = extendedConnect(h2cPort, authority(h2cPort), path, origin(h2cPort), true);
		assertTrue(res.connectProtocol(), "the server did not advertise SETTINGS_ENABLE_CONNECT_PROTOCOL");
	}

	@Test void h02_sameOriginExtendedConnectIsAdmitted() throws Exception {
		var path = openSession(HttpClient.newHttpClient());
		var res = extendedConnect(h2cPort, authority(h2cPort), path, origin(h2cPort), true);
		assertEquals(200, res.status());
		assertTrue(res.text().contains("resync-begin"), res::text);
	}

	@Test void h03_crossSiteExtendedConnectCloses4403() throws Exception {
		var path = openSession(HttpClient.newHttpClient());
		var res = extendedConnect(h2cPort, authority(h2cPort), path, "http://evil.example", true);
		assertEquals(200, res.status());
		assertEquals(4403, res.closeCode(), res::text);
	}

	@Test void h04_extendedConnectWithKeyCloses4403() throws Exception {
		// RFC 8441 forbids Sec-WebSocket-Key; carrying one marks the request as RFC 6455, which needs a real Host.
		var path = openSession(HttpClient.newHttpClient());
		var res = extendedConnect(h2cPort, authority(h2cPort), path, origin(h2cPort), true, "sec-websocket-key", "dGhlIHNhbXBsZSBub25jZQ==");
		assertEquals(200, res.status());
		assertEquals(4403, res.closeCode(), res::text);
	}

	@Test void h05_handshakeCarriesNoHostAndTheAuthorityIsInTheUri() throws Exception {
		PROBED.clear();
		HANDSHAKES.clear();
		var res = extendedConnect(h2cPort, "app.example", "/probe", "http://app.example", false);
		assertEquals(200, res.status());
		assertEquals("http://app.example/probe", PROBED.poll(10, TimeUnit.SECONDS));
		assertEquals("host=false;key=false;upgrade=false", HANDSHAKES.poll(10, TimeUnit.SECONDS));
	}

	@Test void h06_noFilterSeesTheHandshake() throws Exception {
		FILTERED.clear();
		var path = openSession(HttpClient.newHttpClient());
		extendedConnect(h2cPort, authority(h2cPort), path, origin(h2cPort), true);
		var http = HttpClient.newHttpClient();
		var frames = new Frames();
		http.newWebSocketBuilder().header("Origin", origin(plainPort)).buildAsync(wsUri(plainPort, path), frames)
			.get(10, TimeUnit.SECONDS).sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
		assertTrue(FILTERED.stream().anyMatch(p -> p.startsWith("/rest")), FILTERED::toString);
		assertTrue(FILTERED.stream().noneMatch(p -> p.equals(path)), FILTERED::toString);
	}

	@Test void h07_http1CannotPassForAnExtendedConnect() throws Exception {
		PROBED.clear();
		HANDSHAKES.clear();
		var o = "Origin: http://app.example\r\n";
		var upgrade = "Upgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Version: 13\r\n";
		// An upgrade with no Host and no key, on HTTP/1.1 and on HTTP/1.0 (which needs no Host), then without Upgrade too,
		// then a CONNECT request line claiming HTTP/2.0: none may reach the handshake as a key-less, Host-less request.
		var a = raw(plainPort, "GET /probe HTTP/1.1\r\n" + upgrade + o + "\r\n");
		var b = raw(plainPort, "GET /probe HTTP/1.0\r\n" + upgrade + o + "\r\n");
		var c = raw(plainPort, "GET /probe HTTP/1.0\r\nConnection: Upgrade\r\nSec-WebSocket-Version: 13\r\n" + o + "\r\n");
		var d = raw(plainPort, "CONNECT /probe HTTP/2.0\r\nSec-WebSocket-Version: 13\r\n" + o + "\r\n");
		for (var r : List.of(a, b, c, d))
			assertFalse(r.startsWith("HTTP/1.1 101"), r);
		assertNull(HANDSHAKES.poll(1, TimeUnit.SECONDS), "a key-less, Host-less HTTP/1 request reached the configurator");
	}

	/** A server with an h2c connector whose bus runs under a boundary built once the port is known. */
	private record Own(Server server, ServerBus bus, int port) implements AutoCloseable {
		@Override public void close() throws Exception {
			bus.close();
			server.stop();
		}
	}

	private static Own startOwn(Function<Integer,RequestBoundary> boundaryFor) throws Exception {
		var srv = new Server();
		var conn = new ServerConnector(srv, new HttpConnectionFactory(new HttpConfiguration()), new HTTP2CServerConnectionFactory(new HttpConfiguration()));
		conn.setHost("127.0.0.1");
		conn.setPort(0);
		srv.addConnector(conn);
		conn.open();
		var b = ServerBus.create(BusPolicy.create().downstream("ops.jobs", true).boundary(boundaryFor.apply(conn.getLocalPort())).build());
		var ctx = new ServletContextHandler();
		ctx.setContextPath("/");
		ctx.addServlet(new ServletHolder(new OwnBusRest(b)), "/rest/*");
		JakartaWebSocketServletContainerInitializer.configure(ctx, (sc, container) -> BusWebSockets.register(container, b));
		srv.setHandler(ctx);
		srv.start();
		b.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", List.of()));
		return new Own(srv, b, conn.getLocalPort());
	}

	/** Opens a session on a server that runs a {@link LoopbackBoundary}, over HTTP/1, and returns its WebSocket path. */
	private static String openLoopbackSession(Own o, String token) throws Exception {
		var body = "{\"v\":1,\"bridge\":\"ops\",\"transport\":\"websocket\",\"downstream\":[\"ops.jobs\"],\"upstream\":[]}";
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(origin(o.port()) + "/rest" + BusEventsMixin.SESSION_PATH))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.header("Origin", origin(o.port()))
			.header(LoopbackBoundary.DEFAULT_CSRF_HEADER, token)
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		return JsonMap.ofString(res.body()).getMap("paths").getString("websocket");
	}

	@Test void h08_loopbackBoundaryAdmitsItsOwnAuthorityOverHttp2() throws Exception {
		var token = SynchronizerToken.generate();
		try (var o = startOwn(port -> LoopbackBoundary.create().authority("127.0.0.1", port).token(token).build())) {
			var path = openLoopbackSession(o, token.value());
			var ok = extendedConnect(o.port(), authority(o.port()), path, origin(o.port()), true);
			assertEquals(200, ok.status());
			assertTrue(ok.text().contains("resync-begin"), ok::text);

			// Another authority is not this server, even with this server's Origin (which is what gets past checkOrigin).
			var foreign = extendedConnect(o.port(), "evil.example:" + o.port(), path, origin(o.port()), true);
			assertEquals(4403, foreign.closeCode(), foreign::text);

			// A foreign Origin never reaches the Host check: the loopback origin is an exact match.
			var crossSite = extendedConnect(o.port(), authority(o.port()), path, "http://evil.example:" + o.port(), true);
			assertNotEquals(200, crossSite.status());

			// A key marks the request as RFC 6455, which has no synthesised Host.
			var keyed = extendedConnect(o.port(), authority(o.port()), path, origin(o.port()), true, "sec-websocket-key", "dGhlIHNhbXBsZSBub25jZQ==");
			assertEquals(4403, keyed.closeCode(), keyed::text);
		}
	}

	@Test void h09_allowedHostsAppliesToTheExtendedConnectAuthority() throws Exception {
		var csrf = CsrfBoundary.create().tokens(CsrfTokens.perProcess()).allowedHosts("127.0.0.1").build();
		try (var o = startOwn(port -> csrf)) {
			var http = HttpClient.newHttpClient();
			var body = "{\"v\":1,\"bridge\":\"ops\",\"transport\":\"websocket\",\"downstream\":[\"ops.jobs\"],\"upstream\":[]}";
			var req = (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
				(proxy, method, args) -> method.getReturnType() == boolean.class ? (Object) false : null);
			var res = http.send(HttpRequest.newBuilder(URI.create(origin(o.port()) + "/rest" + BusEventsMixin.SESSION_PATH))
				.header("Content-Type", "application/json").header("Accept", "application/json")
				.header("Origin", origin(o.port())).header(csrf.csrfHeader(), csrf.tokenFor(req))
				.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
			assertEquals(200, res.statusCode(), res::body);
			var path = JsonMap.ofString(res.body()).getMap("paths").getString("websocket");

			var ok = extendedConnect(o.port(), authority(o.port()), path, origin(o.port()), true);
			assertEquals(200, ok.status());
			assertTrue(ok.text().contains("resync-begin"), ok::text);

			var rebound = extendedConnect(o.port(), "evil.example:" + o.port(), path, "http://evil.example:" + o.port(), true);
			assertEquals(4403, rebound.closeCode(), rebound::text);
		}
	}
}
