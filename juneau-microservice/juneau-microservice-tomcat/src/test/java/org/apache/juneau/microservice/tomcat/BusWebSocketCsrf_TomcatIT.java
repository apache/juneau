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
package org.apache.juneau.microservice.tomcat;

import static org.apache.juneau.commons.utils.Shorts.f;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.catalina.*;
import org.apache.catalina.startup.*;
import org.apache.catalina.valves.*;
import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.console.bus.*;
import org.apache.juneau.console.bus.websocket.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import jakarta.servlet.http.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;

/**
 * The bus's WebSocket handshake under a {@link CsrfBoundary}, over a real Tomcat 11 socket; the counterpart of the
 * Jetty IT of the same name.
 *
 * <p>
 * Three Tomcat instances share one bus and boundary: a plain one; one with a {@link RemoteIpValve} (protocol header
 * pinned to {@code X-Forwarded-Proto}), the container's own forwarded-header support; and one whose valve also reads
 * {@code X-Forwarded-Host}.  A probe endpoint records what {@link HandshakeRequest#getRequestURI()} and {@code Host}
 * report on each.  The handshake does not consult {@link CsrfBoundary.OriginSource}: its scheme comes from the
 * (valve-rewritten) handshake URI and its host and port from the {@code Host} header, so the boundary here uses the
 * default source.  The valved rows model a trusted proxy (loopback is in the valve's {@code internalProxies}), not an
 * untrusted client; a client that can reach Tomcat directly and is trusted by the valve could forge the forwarded
 * headers.
 *
 * <h5 class='section'>What Tomcat reports (Tomcat 11.0.25), unlike Jetty:</h5>
 * <p>
 * Tomcat reports the WebSocket scheme, {@code ws} or {@code wss}, where Jetty reports {@code http} or {@code https}.
 * With the client's Origin and Host as shown, the request URI is:
 * <table class='styled'>
 * 	<tr><th>Case</th><th>Request URI</th><th>Headers visible</th></tr>
 * 	<tr><td>(a) no valve, plain</td><td>{@code ws://127.0.0.1:<port>/probe}</td><td>{@code Host} {@code 127.0.0.1:<port>}</td></tr>
 * 	<tr><td>(b) {@code RemoteIpValve}, {@code X-Forwarded-Proto: https}</td><td>{@code wss://127.0.0.1/probe} (scheme
 * 		rewritten, port dropped)</td><td>{@code Host} {@code 127.0.0.1:<port>}, {@code X-Forwarded-Proto}</td></tr>
 * 	<tr><td>(c) {@code RemoteIpValve}, no forwarded header</td><td>{@code ws://127.0.0.1:<port>/probe}</td><td>{@code Host}</td></tr>
 * 	<tr><td>(d) no valve, {@code X-Forwarded-Proto: https}</td><td>{@code ws://127.0.0.1:<port>/probe} (header ignored)</td>
 * 		<td>{@code Host}, {@code X-Forwarded-Proto}</td></tr>
 * 	<tr><td>(e) {@code RemoteIpValve} with {@code hostHeader=X-Forwarded-Host}, {@code X-Forwarded-Host: evil.example}</td>
 * 		<td>{@code ws://evil.example:<port>/probe} (URI host rewritten, scheme and port kept)</td><td>{@code Host} still {@code 127.0.0.1:<port>}</td></tr>
 * </table>
 *
 * <p>
 * {@link CsrfBoundary#checkHandshake} normalizes {@code ws} to {@code http} and {@code wss} to {@code https}, so the
 * fail-closed path for an unknown scheme is never reached on Tomcat.  The port always comes from the {@code Host}
 * header, never from the URI, which is why the valve dropping it in (b) does not matter: an {@code https} Origin
 * without a port (implied 443) is refused there.  Only the {@code RemoteIpValve} carries {@code X-Forwarded-Proto} into
 * the scheme.  The default valve ignores {@code X-Forwarded-Host}, so a forged forwarded host is refused.
 *
 * <p>
 * Binds sockets, so it is off by default.  Run it with {@code -Djuneau.busIt=true}, outside the sandbox.
 */
@EnabledIfSystemProperty(named=BusWebSocketCsrf_TomcatIT.GATE, matches="true",
	disabledReason="binds a socket; run with -Dtest='BusWebSocketCsrf_TomcatIT' -Djuneau.busIt=true")
@SuppressWarnings({
	"resource" // The Tomcat servers and buses are static fixtures started in @BeforeAll and stopped in @AfterAll; raw-socket chains are closed with the socket.
})
class BusWebSocketCsrf_TomcatIT extends TestBase {

	static final String GATE = "juneau.busIt";

	private static final BlockingQueue<String> PROBED = new LinkedBlockingQueue<>();
	private static final BlockingQueue<List<String>> FORWARDED_HOSTS = new LinkedBlockingQueue<>();
	private static Tomcat plain;
	private static Tomcat valved;
	private static Tomcat hostValved;
	private static Tomcat trusted;
	private static final List<Path> TEMP = new ArrayList<>();
	private static int plainPort;
	private static int valvedPort;
	private static int hostValvedPort;
	private static int trustedPort;
	private static ServerBus bus;
	private static CsrfBoundary boundary;
	private static ServerBus trustedBus;
	private static CsrfBoundary trustedBoundary;

	@Rest
	public static class BusRest extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;
		@Override public ServerBus serverBus() { return bus; }
	}

	/** The same REST resource over the bus that sits behind a {@code trustedProxies} boundary. */
	@Rest
	public static class TrustedBusRest extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;
		@Override public ServerBus serverBus() { return trustedBus; }
	}

	/** Accepts and does nothing; the probe configurator is what matters. */
	public static class ProbeEndpoint extends Endpoint {
		@Override public void onOpen(jakarta.websocket.Session session, EndpointConfig config) { /* no-op by design */ }
	}

	/** Records the handshake's request URI as the container reports it. */
	static final class ProbeConfigurator extends ServerEndpointConfig.Configurator {
		@Override public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
			PROBED.add(request.getRequestURI() + " Host=" + request.getHeaders().get("Host"));
			var lines = new ArrayList<String>();
			request.getHeaders().forEach((k, v) -> {
				if ("X-Forwarded-Host".equalsIgnoreCase(k))
					lines.addAll(v);
			});
			FORWARDED_HOSTS.add(lines);
		}
	}

	/** Boots a Tomcat, with a {@link RemoteIpValve} when {@code valve} is set (reading the host from {@code hostHeader} when non-null). */
	private static Tomcat boot(boolean valve, String hostHeader) throws Exception {
		return boot(valve, hostHeader, new BusRest(), bus);
	}

	private static Tomcat boot(boolean valve, String hostHeader, HttpServlet rest, ServerBus serverBus) throws Exception {
		var tomcat = new Tomcat();
		var base = Files.createTempDirectory("tc-bus-it");
		TEMP.add(base);
		tomcat.setBaseDir(base.toString());
		var c = tomcat.getConnector();
		c.setPort(0);
		c.setProperty("address", "127.0.0.1");
		c.addUpgradeProtocol(new org.apache.coyote.http2.Http2Protocol());
		tomcat.getHost().setAppBase(base.toString());
		if (valve) {
			var v = new RemoteIpValve();
			v.setProtocolHeader("X-Forwarded-Proto");
			v.setProtocolHeaderHttpsValue("https");
			if (hostHeader != null)
				v.setHostHeader(hostHeader);
			tomcat.getEngine().getPipeline().addValve(v);
		}
		Context ctx = tomcat.addContext("", base.toString());
		// The WebSocket filter only runs for a request that maps to a servlet.
		var dflt = Tomcat.addServlet(ctx, "default", new HttpServlet() {
			private static final long serialVersionUID = 1L;
		});
		dflt.addMapping("/");
		var wrapper = Tomcat.addServlet(ctx, "bus", rest);
		wrapper.setLoadOnStartup(1);
		wrapper.addMapping("/rest/*");
		ctx.addApplicationListener(org.apache.tomcat.websocket.server.WsContextListener.class.getName());
		tomcat.start();
		var sc = (ServerContainer) ctx.getServletContext().getAttribute(ServerContainer.class.getName());
		BusWebSockets.register(sc, serverBus);
		sc.addEndpoint(ServerEndpointConfig.Builder.create(ProbeEndpoint.class, "/probe")
			.configurator(new ProbeConfigurator())
			.build());
		return tomcat;
	}

	@BeforeAll static void start() throws Exception {
		boundary = CsrfBoundary.create().tokens(CsrfTokens.perProcess()).build();
		bus = ServerBus.create(BusPolicy.create()
			.downstream("ops.jobs", true)
			.boundary(boundary)
			.build());
		trustedBoundary = CsrfBoundary.create().tokens(CsrfTokens.perProcess())
			.trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED).trustedProxies("127.0.0.1/32").build();
		trustedBus = ServerBus.create(BusPolicy.create()
			.downstream("ops.jobs", true)
			.boundary(trustedBoundary)
			.build());
		plain = boot(false, null);
		trusted = boot(false, null, new TrustedBusRest(), trustedBus);
		valved = boot(true, null);
		hostValved = boot(true, "X-Forwarded-Host");
		plainPort = plain.getConnector().getLocalPort();
		valvedPort = valved.getConnector().getLocalPort();
		hostValvedPort = hostValved.getConnector().getLocalPort();
		trustedPort = trusted.getConnector().getLocalPort();
		assertNotEquals(plainPort, valvedPort);
		assertNotEquals(plainPort, hostValvedPort);
		assertNotEquals(valvedPort, hostValvedPort);
		assertNotEquals(trustedPort, plainPort);
		assertNotEquals(trustedPort, valvedPort);
		assertNotEquals(trustedPort, hostValvedPort);
		bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", List.of()));
		trustedBus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", List.of()));
	}

	@AfterAll static void stop() throws Exception {
		if (bus != null)
			bus.close();
		if (trustedBus != null)
			trustedBus.close();
		for (var t : new Tomcat[]{plain, valved, hostValved, trusted})
			if (t != null) {
				t.stop();
				t.destroy();
			}
		for (var d : TEMP)
			try (var walk = Files.walk(d)) {
				walk.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
			}
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

	/** Opens a WebSocket session through the plain instance and returns its WebSocket path. */
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

	/** Connects with the given headers, expects the first frame {@code resync-begin} and an open socket, then closes. */
	private static void assertAdmitted(int port, String...headers) throws Exception {
		var http = HttpClient.newHttpClient();
		var frames = new Frames();
		var b = http.newWebSocketBuilder();
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		var ws = b.buildAsync(wsUri(port, openSession(http)), frames).get(10, TimeUnit.SECONDS);
		var first = frames.frames.poll(10, TimeUnit.SECONDS);
		assertNotNull(first, "no frame within 10s; closed with " + frames.closed.getNow(null));
		assertEquals("resync-begin", first.getString("type"));
		assertNull(frames.closed.getNow(null), "the socket was closed");
		ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
	}

	/** Connects with the given headers and expects the upgrade to complete and the endpoint to close at once with {@code 4403}. */
	private static void assertRefused4403(int port, String...headers) throws Exception {
		var http = HttpClient.newHttpClient();
		var frames = new Frames();
		var b = http.newWebSocketBuilder();
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		b.buildAsync(wsUri(port, openSession(http)), frames).get(10, TimeUnit.SECONDS);
		assertEquals(4403, frames.closed.get(10, TimeUnit.SECONDS));
		assertTrue(frames.frames.isEmpty(), "a refused handshake saw a frame");
	}

	/** A same-origin {@code Origin} on a plain Tomcat is admitted and the first frame arrives. */
	@Test void a01_sameOriginWsConnects() throws Exception {
		assertAdmitted(plainPort, "Origin", origin(plainPort));
	}

	/**
	 * A well-formed cross-site Origin.  {@code checkOrigin} sees only the Origin value, not the Host it must match, so
	 * the upgrade completes (101) and the endpoint closes at once with {@code 4403} before any frame.
	 */
	@Test void a02_crossSiteOriginCloses4403BeforeAnyFrame() throws Exception {
		assertRefused4403(plainPort, "Origin", "http://evil.example");
	}

	/** No {@code Origin} at all is refused before the upgrade with {@code 403}. */
	@Test void a03_missingOriginRefused() throws Exception {
		var http = HttpClient.newHttpClient();
		assertEquals(403, handshakeStatus(http, wsUri(plainPort, openSession(http))));
	}

	/** The opaque {@code Origin: null} is refused before the upgrade with {@code 403}. */
	@Test void a03b_originNullRefused() throws Exception {
		var http = HttpClient.newHttpClient();
		assertEquals(403, handshakeStatus(http, wsUri(plainPort, openSession(http)), "Origin", "null"));
	}

	/**
	 * A {@code Host} that is not the Origin's host.  The JDK client cannot set {@code Host}, so this speaks the handshake
	 * over a raw socket: the upgrade completes and the endpoint's first frame is a close with {@code 4403}.
	 */
	@Test void a04_hostOriginMismatchCloses4403() throws Exception {
		var path = openSession(HttpClient.newHttpClient());
		try (var socket = new Socket("127.0.0.1", plainPort); var out = socket.getOutputStream(); var in = new BufferedInputStream(socket.getInputStream())) {
			socket.setSoTimeout(10_000);
			out.write(("GET " + path + " HTTP/1.1\r\n"
				+ "Host: evil.example\r\n"
				+ "Origin: " + origin(plainPort) + "\r\n"
				+ "Connection: Upgrade\r\n"
				+ "Upgrade: websocket\r\n"
				+ "Sec-WebSocket-Version: 13\r\n"
				+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
			out.flush();
			var head = new StringBuilder();
			while (! head.toString().endsWith("\r\n\r\n")) {
				var b = in.read();
				assertTrue(b >= 0, "connection closed during handshake: " + head);
				head.append((char) b);
			}
			assertTrue(head.toString().startsWith("HTTP/1.1 101"), head::toString);
			assertEquals(0x88, in.read(), "first frame is not a close frame");
			var len = in.read();
			assertTrue(len >= 2 && len < 126, "unexpected close payload length " + len);
			assertEquals(4403, (in.read() << 8) | in.read());
		}
	}

	/**
	 * A {@code wss} page behind a TLS-terminating proxy: the proxy sends {@code X-Forwarded-Proto: https}, and the
	 * {@link RemoteIpValve} carries that into the handshake's request URI, so an {@code https} Origin on the real port
	 * matches (the port comes from {@code Host}) and an {@code http} Origin does not.
	 */
	@Test void a05_forwardedProtoBehindRemoteIpValve() throws Exception {
		assertAdmitted(valvedPort, "Origin", "https://127.0.0.1:" + valvedPort, "X-Forwarded-Proto", "https");
		assertRefused4403(valvedPort, "Origin", origin(valvedPort), "X-Forwarded-Proto", "https");
	}

	/** A forwarded {@code https} but a cross-site Origin through the valve is still refused. */
	@Test void a05b_forwardedProtoCrossSiteOriginRefused() throws Exception {
		assertRefused4403(valvedPort, "Origin", "http://evil.example", "X-Forwarded-Proto", "https");
	}

	/**
	 * The valve rewrites the URI to {@code wss} without a port, but the port comes from {@code Host}: an {@code https}
	 * Origin with the implied port 443 is not this server's origin and is refused.
	 */
	@Test void a05c_forwardedProtoImpliedPortOriginRefused() throws Exception {
		assertRefused4403(valvedPort, "Origin", "https://127.0.0.1", "X-Forwarded-Proto", "https");
	}

	/** The default valve ignores {@code X-Forwarded-Host}, so a forged forwarded host and a matching Origin are refused. */
	@Test void a07_forwardedHostIgnoredByDefaultValve() throws Exception {
		assertRefused4403(valvedPort, "Origin", "http://evil.example", "X-Forwarded-Host", "evil.example");
	}

	/**
	 * With {@code hostHeader=X-Forwarded-Host} the valve rewrites the host of the handshake's request URI, but the
	 * {@code Host} header the handshake sees stays the real one (case (e) in the class Javadoc).  The boundary reads
	 * {@code Host}, so the forged forwarded host and its matching Origin are still refused.
	 */
	@Test void a08_forwardedHostHeaderValveStillRefusesForgedHost() throws Exception {
		assertRefused4403(hostValvedPort, "Origin", "http://evil.example", "X-Forwarded-Host", "evil.example");
	}

	/** Records the request URI and Host of an upgrade to the probe endpoint. */
	private static String probe(int port, String...headers) throws Exception {
		var b = HttpClient.newHttpClient().newWebSocketBuilder();
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		PROBED.clear();
		b.buildAsync(wsUri(port, "/probe"), new Frames()).get(10, TimeUnit.SECONDS)
			.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
		return PROBED.poll(10, TimeUnit.SECONDS);
	}

	/**
	 * Pins what Tomcat reports from {@code HandshakeRequest.getRequestURI()} (the class Javadoc table): always
	 * {@code ws} or {@code wss}, never {@code http} or {@code https}; only the {@code RemoteIpValve} applies
	 * {@code X-Forwarded-Proto}, and it drops the port.
	 */
	@Test void a06_requestUriSchemeIsWsOrWss() throws Exception {
		assertEquals("ws://127.0.0.1:" + plainPort + "/probe Host=[127.0.0.1:" + plainPort + "]", probe(plainPort, "Origin", origin(plainPort)), "(a)");
		assertEquals("wss://127.0.0.1/probe Host=[127.0.0.1:" + valvedPort + "]", probe(valvedPort, "X-Forwarded-Proto", "https"), "(b)");
		assertEquals("ws://127.0.0.1:" + valvedPort + "/probe Host=[127.0.0.1:" + valvedPort + "]", probe(valvedPort), "(c)");
		assertEquals("ws://127.0.0.1:" + plainPort + "/probe Host=[127.0.0.1:" + plainPort + "]", probe(plainPort, "X-Forwarded-Proto", "https"), "(d)");
		assertEquals("ws://evil.example:" + hostValvedPort + "/probe Host=[127.0.0.1:" + hostValvedPort + "]", probe(hostValvedPort, "X-Forwarded-Host", "evil.example"), "(e)");
	}

	/**
	 * Trusted forwarding picks the rightmost entry, which relies on the handshake's repeated header lines keeping their
	 * wire order.  Three lines, sent raw because the JDK client cannot repeat a header, come back in order.
	 */
	@Test void a09_repeatedForwardedHostLinesKeepWireOrder() throws Exception {
		FORWARDED_HOSTS.clear();
		try (var socket = new Socket("127.0.0.1", plainPort); var out = socket.getOutputStream()) {
			socket.setSoTimeout(10_000);
			out.write(("GET /probe HTTP/1.1\r\n"
				+ "Host: 127.0.0.1:" + plainPort + "\r\n"
				+ "X-Forwarded-Host: first.example\r\n"
				+ "X-Forwarded-Host: second.example\r\n"
				+ "X-Forwarded-Host: third.example\r\n"
				+ "Connection: Upgrade\r\n"
				+ "Upgrade: websocket\r\n"
				+ "Sec-WebSocket-Version: 13\r\n"
				+ "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
			out.flush();
			var seen = FORWARDED_HOSTS.poll(10, TimeUnit.SECONDS);
			assertEquals(List.of("first.example", "second.example", "third.example"), seen);
		}
	}

	/** Opens a session through the {@code trustedProxies} instance with a forwarded write, as a proxy on the loopback peer would send it. */
	private static String openTrustedSession(HttpClient http) throws Exception {
		var body = "{\"v\":1,\"bridge\":\"ops\",\"transport\":\"websocket\",\"downstream\":[\"ops.jobs\"],\"upstream\":[]}";
		var req = (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
			(proxy, method, args) -> method.getReturnType() == boolean.class ? (Object) false : null);
		var res = http.send(HttpRequest.newBuilder(URI.create(origin(trustedPort) + "/rest" + BusEventsMixin.SESSION_PATH))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.header("Origin", origin(trustedPort))
			.header("X-Forwarded-Host", "127.0.0.1:" + trustedPort)
			.header("X-Forwarded-Proto", "http")
			.header(trustedBoundary.csrfHeader(), trustedBoundary.tokenFor(req))
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		return JsonMap.ofString(res.body()).getMap("paths").getString("websocket");
	}

	/**
	 * With {@code trustedProxies} set, Tomcat reports the peer on HTTP (the loopback proxy is believed) but supplies none
	 * on a handshake, so the peer there is unknown and untrusted.  A handshake that carries a forwarded host is refused
	 * with {@code 4403}; the documented recipe (same scheme, {@code Host} preserved, no forwarded header on the socket)
	 * connects.  This fails loudly if a future Tomcat starts supplying the peer.
	 */
	@Test void i01_trustedProxiesOnTomcat() throws Exception {
		var http = HttpClient.newHttpClient();
		var path = openTrustedSession(http);

		var refused = new Frames();
		http.newWebSocketBuilder()
			.header("Origin", origin(trustedPort))
			.header("X-Forwarded-Host", "127.0.0.1:" + trustedPort)
			.header("X-Forwarded-Proto", "http")
			.buildAsync(wsUri(trustedPort, path), refused).get(10, TimeUnit.SECONDS);
		assertEquals(4403, refused.closed.get(10, TimeUnit.SECONDS));
		assertTrue(refused.frames.isEmpty(), "a refused handshake saw a frame");

		var frames = new Frames();
		var ws = http.newWebSocketBuilder()
			.header("Origin", origin(trustedPort))
			.buildAsync(wsUri(trustedPort, openTrustedSession(http)), frames).get(10, TimeUnit.SECONDS);
		var first = frames.frames.poll(10, TimeUnit.SECONDS);
		assertNotNull(first, "no frame within 10s; closed with " + frames.closed.getNow(null));
		assertEquals("resync-begin", first.getString("type"));
		ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
	}

	/** What an h2c connection did with an RFC 8441 extended CONNECT: the SETTINGS it sent and how the stream ended. */
	record Ext(boolean settingsSeen, boolean connectProtocolAdvertised, String frames) {}

	private static void field(ByteArrayOutputStream h, String name, String value) throws IOException {
		h.write(0);
		str(h, name);
		str(h, value);
	}

	private static void str(ByteArrayOutputStream h, String value) throws IOException {
		var v = value.getBytes(StandardCharsets.US_ASCII);
		h.write(v.length);
		h.write(v);
	}

	private static byte[] frame(int type, int flags, int stream, byte[] payload) {
		var b = java.nio.ByteBuffer.allocate(9 + payload.length);
		b.put((byte)(payload.length >> 16)).put((byte)(payload.length >> 8)).put((byte)payload.length);
		b.put((byte)type).put((byte)flags).putInt(stream).put(payload);
		return b.array();
	}

	/**
	 * Sends a hand-encoded extended CONNECT over prior-knowledge h2c and reads until the stream is reset or the
	 * connection ends or goes quiet.  The frame types seen are returned for the record.
	 */
	private static Ext extendedConnect(int port, String path) throws Exception {
		var out = new ByteArrayOutputStream();
		out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
		out.write(frame(0x4, 0, 0, new byte[0]));
		var h = new ByteArrayOutputStream();
		h.write(2);                                    // :method, name from the static table
		str(h, "CONNECT");
		field(h, ":protocol", "websocket");
		h.write(0x86);                                 // :scheme http
		h.write(4);                                    // :path
		str(h, path);
		h.write(1);                                    // :authority
		str(h, "127.0.0.1:" + port);
		field(h, "origin", origin(port));
		field(h, "sec-websocket-version", "13");
		out.write(frame(0x1, 0x4, 1, h.toByteArray()));

		var seen = new StringBuilder();
		var settings = false;
		var advertised = false;
		try (var socket = new Socket("127.0.0.1", port); var in = new DataInputStream(socket.getInputStream())) {
			socket.setSoTimeout(3_000);
			socket.getOutputStream().write(out.toByteArray());
			socket.getOutputStream().flush();
			try {
				for (;;) {
					var head = new byte[9];
					in.readFully(head);
					var len = ((head[0] & 0xFF) << 16) | ((head[1] & 0xFF) << 8) | (head[2] & 0xFF);
					var payload = new byte[len];
					in.readFully(payload);
					seen.append(f("%02x ", head[3]));
					if (head[3] == 0x4 && (head[4] & 0x1) == 0) {
						settings = true;
						for (var i = 0; i + 6 <= len; i += 6)
							if ((((payload[i] & 0xFF) << 8) | (payload[i + 1] & 0xFF)) == 0x8 && payload[i + 5] == 1)
								advertised = true;
						socket.getOutputStream().write(frame(0x4, 0x1, 0, new byte[0]));
						socket.getOutputStream().flush();
					}
					if (head[3] == 0x3 || head[3] == 0x7)
						break;
				}
			} catch (EOFException | SocketTimeoutException e) {
				seen.append(e instanceof EOFException ? "eof" : "quiet");
			}
		}
		return new Ext(settings, advertised, seen.toString().trim());
	}

	/**
	 * Tomcat 11 has no RFC 8441 support: it does not advertise {@code SETTINGS_ENABLE_CONNECT_PROTOCOL}, and an extended
	 * CONNECT never reaches a WebSocket endpoint, so there is no HTTP/2 handshake for the boundary to judge.
	 */
	@Test void h01_tomcatHasNoExtendedConnect() throws Exception {
		var path = openSession(HttpClient.newHttpClient());
		PROBED.clear();
		var res = extendedConnect(plainPort, "/probe");
		var real = extendedConnect(plainPort, path);
		assertTrue(res.settingsSeen(), res::toString);
		assertFalse(res.connectProtocolAdvertised(), res::toString);
		assertFalse(real.connectProtocolAdvertised(), real::toString);
		assertNull(PROBED.poll(1, TimeUnit.SECONDS), "an extended CONNECT reached the configurator");
	}
}
