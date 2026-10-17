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
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
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

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * {@link CsrfBoundary.Builder#trustedForwarding(CsrfBoundary.ForwardedSource) Trusted forwarding} over a real Jetty 12
 * (ee11) socket, on HTTP writes and on the bus WebSocket handshake.
 *
 * <p>
 * Each test starts a server of its own, because the boundary is the thing that varies.  Without the container's
 * {@link ForwardedRequestCustomizer} the container does no forwarding at all, which is the case the setting exists for;
 * with it (and {@code originSource(CONTAINER)}) the boundary cross-checks the container's rewrite, and those rows record
 * what Jetty reports before asserting.
 *
 * <p>
 * Binds a socket, so it is off by default.  Run it with {@code -Djuneau.busIt=true}, outside the sandbox.
 */
@EnabledIfSystemProperty(named=CsrfBoundary_JettyIT.GATE, matches="true",
	disabledReason="binds a socket; run with -Dtest='CsrfBoundaryForwarded_JettyIT' -Djuneau.busIt=true")
class CsrfBoundaryForwarded_JettyIT extends TestBase {

	private static final String PUBLIC = "https://app.example";
	private static final String JSON = "application/json";
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	@Rest
	public static class BusRest extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;
		private final transient ServerBus bus;
		BusRest(ServerBus bus) { this.bus = bus; }
		@Override public ServerBus serverBus() { return bus; }
	}

	/** Counts the writes that reached it. */
	static final class WriteServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		static final java.util.concurrent.atomic.AtomicInteger WRITES = new java.util.concurrent.atomic.AtomicInteger();
		@Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
			WRITES.incrementAndGet();
			res.getWriter().write("written");
		}
	}

	/** Reports the peer address the container sees. */
	static final class PeerServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.getWriter().write(req.getRemoteAddr());
		}
	}

	/** Reports the container's own view of the request: scheme, server name and server port. */
	static final class SeenServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.getWriter().write(req.getScheme() + "|" + req.getServerName() + "|" + req.getServerPort());
		}
	}

	record App(Server server, int port, CsrfBoundary boundary, ServerBus bus, String host) {
		String authority() { return (host.indexOf(':') >= 0 ? "[" + host + "]" : host) + ":" + port; }
		String origin() { return "http://" + authority(); }
	}

	private App app;

	@AfterEach void stop() throws Exception {
		if (app != null) {
			app.bus().close();
			app.server().stop();
		}
		app = null;
	}

	private App start(boolean customizer, CsrfBoundary.Builder b) throws Exception {
		return start("127.0.0.1", customizer, b);
	}

	private App start(String host, boolean customizer, CsrfBoundary.Builder b) throws Exception {
		var boundary = b.tokens(CsrfTokens.perProcess()).build();
		var bus = ServerBus.create(BusPolicy.create().downstream("ops.jobs", true).boundary(boundary).build());
		var server = new Server();
		var config = new HttpConfiguration();
		if (customizer)
			config.addCustomizer(new ForwardedRequestCustomizer());
		var connector = new ServerConnector(server, new HttpConnectionFactory(config));
		connector.setHost(host);
		connector.setPort(0);
		server.addConnector(connector);
		var ctx = new ServletContextHandler();
		ctx.setContextPath("/");
		ctx.addFilter(new FilterHolder(new RequestBoundaryFilter(boundary)), "/write", EnumSet.of(DispatcherType.REQUEST));
		ctx.addServlet(new ServletHolder(new WriteServlet()), "/write");
		ctx.addServlet(new ServletHolder(new SeenServlet()), "/seen");
		ctx.addServlet(new ServletHolder(new PeerServlet()), "/peer");
		ctx.addServlet(new ServletHolder(new BusRest(bus)), "/rest/*");
		JakartaWebSocketServletContainerInitializer.configure(ctx, (sc, container) -> BusWebSockets.register(container, bus));
		server.setHandler(ctx);
		server.start();
		bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", List.of()));
		app = new App(server, connector.getLocalPort(), boundary, bus, host);
		WriteServlet.WRITES.set(0);
		return app;
	}

	private static String token(CsrfBoundary boundary) {
		var req = (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
			new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> method.getReturnType() == boolean.class ? (Object) false : null);
		return boundary.tokenFor(req);
	}

	private static HttpResponse<String> post(App a, String path, String body, String...headers) throws Exception {
		var b = HttpRequest.newBuilder(URI.create("http://" + a.authority() + path)).timeout(Duration.ofSeconds(10))
			.header("Content-Type", JSON).header("Accept", JSON).header(a.boundary().csrfHeader(), token(a.boundary()));
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		return HTTP.send(b.POST(HttpRequest.BodyPublishers.ofString(body)).build(), BodyHandlers.ofString());
	}

	private static String[] forwarded(String origin, String host, String proto) {
		return new String[] { "Origin", origin, "X-Forwarded-Host", host, "X-Forwarded-Proto", proto };
	}

	private static final String SESSION = "{\"v\":1,\"bridge\":\"ops\",\"transport\":\"websocket\",\"downstream\":[\"ops.jobs\"],\"upstream\":[]}";

	/** Opens a bus session with the given headers; returns the response. */
	private static HttpResponse<String> session(App a, String...headers) throws Exception {
		return post(a, "/rest" + BusEventsMixin.SESSION_PATH, SESSION, headers);
	}

	private static String wsPath(HttpResponse<String> res) throws Exception {
		assertEquals(200, res.statusCode(), res::body);
		return JsonMap.ofString(res.body()).getMap("paths").getString("websocket");
	}

	/** Collects the first frame and the close code. */
	static final class Frames implements WebSocket.Listener {
		final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
		final CompletableFuture<Integer> closed = new CompletableFuture<>();
		@Override public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
			frames.add(data.toString());
			ws.request(1);
			return null;
		}
		@Override public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
			closed.complete(statusCode);
			return null;
		}
	}

	/** Connects and reports whether the bus delivered a frame (admitted) or closed 4403 (refused). */
	private static boolean connects(App a, String path, String...headers) throws Exception {
		var frames = new Frames();
		var b = HttpClient.newHttpClient().newWebSocketBuilder();
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		var ws = b.buildAsync(URI.create("ws://" + a.authority() + path), frames).get(10, TimeUnit.SECONDS);
		var first = frames.frames.poll(5, TimeUnit.SECONDS);
		if (first != null) {
			ws.sendClose(WebSocket.NORMAL_CLOSURE, "").get(10, TimeUnit.SECONDS);
			return true;
		}
		assertEquals(4403, frames.closed.get(10, TimeUnit.SECONDS));
		return false;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No customizer: the boundary is the forwarding authority
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_forwardedWriteAndHandshakeAdmittedWithoutAnyContainerForwarding() throws Exception {
		var a = start(false, CsrfBoundary.create().trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED));
		var write = post(a, "/write", "{}", forwarded(PUBLIC, "app.example", "https"));
		assertEquals(200, write.statusCode(), write::body);
		var path = wsPath(session(a, forwarded(PUBLIC, "app.example", "https")));
		assertTrue(connects(a, path, forwarded(PUBLIC, "app.example", "https")));
	}

	@Test void b02_forwardedHostNotMatchingOriginRefused() throws Exception {
		var a = start(false, CsrfBoundary.create().trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED));
		var write = post(a, "/write", "{}", forwarded("https://evil.example", "app.example", "https"));
		assertEquals(403, write.statusCode(), write::body);
		assertEquals("ORIGIN_MISMATCH", write.headers().firstValue(RequestBoundaryFilter.REJECTION_HEADER).orElse(null));
		assertEquals(0, WriteServlet.WRITES.get());
		var path = wsPath(session(a, forwarded(PUBLIC, "app.example", "https")));
		assertFalse(connects(a, path, forwarded("https://evil.example", "app.example", "https")));
	}

	@Test void b03_settingOffRefusesTheSameRequests() throws Exception {
		var a = start(false, CsrfBoundary.create());
		var write = post(a, "/write", "{}", forwarded(PUBLIC, "app.example", "https"));
		assertEquals(403, write.statusCode(), write::body);
		assertEquals("ORIGIN_MISMATCH", write.headers().firstValue(RequestBoundaryFilter.REJECTION_HEADER).orElse(null));
		var path = wsPath(session(a, "Origin", a.origin()));
		assertFalse(connects(a, path, forwarded(PUBLIC, "app.example", "https")));
	}

	@Test void b04_missingHeaderRefused() throws Exception {
		var a = start(false, CsrfBoundary.create().trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED));
		var write = post(a, "/write", "{}", "Origin", a.origin());
		assertEquals(403, write.statusCode(), write::body);
		assertEquals("FORWARDED_HOST_MISSING", write.headers().firstValue(RequestBoundaryFilter.REJECTION_HEADER).orElse(null));
		// The session POST is judged the same way, so there is no session to hand a handshake: a proto-only request too.
		var proto = post(a, "/write", "{}", "Origin", a.origin(), "X-Forwarded-Proto", "http");
		assertEquals("FORWARDED_HOST_MISSING", proto.headers().firstValue(RequestBoundaryFilter.REJECTION_HEADER).orElse(null));
		assertEquals(0, WriteServlet.WRITES.get());
	}

	@Test void b05_forwardedFamilyNotNamedIsNotRead() throws Exception {
		var a = start(false, CsrfBoundary.create().trustedForwarding(CsrfBoundary.ForwardedSource.FORWARDED));
		var write = post(a, "/write", "{}", "Origin", PUBLIC, "Forwarded", "host=app.example;proto=https");
		assertEquals(200, write.statusCode(), write::body);
		var other = post(a, "/write", "{}", forwarded(PUBLIC, "app.example", "https"));
		assertEquals("FORWARDED_HOST_MISSING", other.headers().firstValue(RequestBoundaryFilter.REJECTION_HEADER).orElse(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Customizer plus CONTAINER: the boundary cross-checks the container's rewrite
	//-----------------------------------------------------------------------------------------------------------------

	private static String seen(App a, String...headers) throws Exception {
		var b = HttpRequest.newBuilder(URI.create("http://" + a.authority() + "/seen")).timeout(Duration.ofSeconds(10));
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		return HTTP.send(b.GET().build(), BodyHandlers.ofString()).body();
	}

	@Test void b06_containerAgreesAndHandshakeIsAdmitted() throws Exception {
		var a = start(true, CsrfBoundary.create().originSource(CsrfBoundary.OriginSource.CONTAINER)
			.trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED));
		// Recorded Jetty behavior: the customizer rewrites scheme, server name and port from the forwarded headers.
		assertEquals("https|app.example|443", seen(a, "X-Forwarded-Host", "app.example", "X-Forwarded-Proto", "https"));
		var write = post(a, "/write", "{}", forwarded(PUBLIC, "app.example", "https"));
		assertEquals(200, write.statusCode(), write::body);
		var path = wsPath(session(a, forwarded(PUBLIC, "app.example", "https")));
		assertTrue(connects(a, path, forwarded(PUBLIC, "app.example", "https")));
		assertFalse(connects(a, path, forwarded("https://evil.example", "app.example", "https")));
	}

	@Test void b07_containerTakingTheLeftmostEntryIsCaught() throws Exception {
		var a = start(true, CsrfBoundary.create().originSource(CsrfBoundary.OriginSource.CONTAINER)
			.trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED));
		// Recorded Jetty behavior: with a list, the customizer takes the leftmost (client-writable) entry.
		assertEquals("https|left.example|443", seen(a, "X-Forwarded-Host", "left.example, app.example", "X-Forwarded-Proto", "https, https"));
		var write = post(a, "/write", "{}", forwarded(PUBLIC, "left.example, app.example", "https, https"));
		assertEquals(403, write.statusCode(), write::body);
		assertEquals("FORWARDED_CONTAINER_MISMATCH", write.headers().firstValue(RequestBoundaryFilter.REJECTION_HEADER).orElse(null));
		assertEquals(0, WriteServlet.WRITES.get());
	}

	/** Lock for the unverified point: the handshake header map keeps repeated lines in wire order, so the rightmost wins. */
	@Test void b09_handshakeKeepsRepeatedForwardedLinesInWireOrder() throws Exception {
		var a = start(false, CsrfBoundary.create().trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED));
		var path = wsPath(session(a, "Origin", PUBLIC, "X-Forwarded-Host", "evil.example", "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "http", "X-Forwarded-Proto", "https"));
		assertTrue(connects(a, path, "Origin", PUBLIC, "X-Forwarded-Host", "evil.example", "X-Forwarded-Host", "app.example",
			"X-Forwarded-Proto", "http", "X-Forwarded-Proto", "https"));
		// The same lines the other way round name evil.example as the rightmost entry: the Origin no longer matches.
		assertFalse(connects(a, path, "Origin", PUBLIC, "X-Forwarded-Host", "app.example", "X-Forwarded-Host", "evil.example",
			"X-Forwarded-Proto", "https", "X-Forwarded-Proto", "http"));
	}

	@Test void b08_handshakeUnderHostHeaderWithCustomizerRewritesAgree() throws Exception {
		var a = start(true, CsrfBoundary.create().trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED));
		var write = post(a, "/write", "{}", forwarded(PUBLIC, "app.example", "https"));
		assertEquals(200, write.statusCode(), write::body);
		var path = wsPath(session(a, forwarded(PUBLIC, "app.example", "https")));
		assertTrue(connects(a, path, forwarded(PUBLIC, "app.example", "https")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// trustedProxies: the peer is the TCP peer Jetty reports
	//-----------------------------------------------------------------------------------------------------------------

	private static CsrfBoundary.Builder proxies(String...cidrs) {
		return CsrfBoundary.create().trustedForwarding(CsrfBoundary.ForwardedSource.X_FORWARDED).trustedProxies(cidrs);
	}

	/** Proves Jetty supplies the peer property to the handshake: if it stops, the handshake half fails. */
	@Test void c01_listedPeerIsBelievedOnWriteAndHandshake() throws Exception {
		var a = start(false, proxies("127.0.0.1/32"));
		var write = post(a, "/write", "{}", forwarded(PUBLIC, "app.example", "https"));
		assertEquals(200, write.statusCode(), write::body);
		var path = wsPath(session(a, forwarded(PUBLIC, "app.example", "https")));
		assertTrue(connects(a, path, forwarded(PUBLIC, "app.example", "https")));
	}

	@Test void c02_unlistedPeerWithForwardedHeaderRefused() throws Exception {
		var a = start(false, proxies("10.0.0.0/8"));
		var write = post(a, "/write", "{}", forwarded(PUBLIC, "app.example", "https"));
		assertEquals(403, write.statusCode(), write::body);
		assertEquals("FORWARDED_PEER_NOT_TRUSTED", write.headers().firstValue(RequestBoundaryFilter.REJECTION_HEADER).orElse(null));
		assertEquals(0, WriteServlet.WRITES.get());
		var path = wsPath(session(a, "Origin", a.origin()));
		assertFalse(connects(a, path, forwarded(PUBLIC, "app.example", "https")));
	}

	@Test void c03_unlistedPeerWithoutHeaderJudgedOnContainerView() throws Exception {
		var a = start(false, proxies("10.0.0.0/8"));
		var write = post(a, "/write", "{}", "Origin", a.origin());
		assertEquals(200, write.statusCode(), write::body);
		var path = wsPath(session(a, "Origin", a.origin()));
		assertTrue(connects(a, path, "Origin", a.origin()));
	}

	/** Records which spelling Jetty reports for an IPv6 loopback peer, then runs a listed write over it. */
	@Test void c04_ipv6LoopbackPeer() throws Exception {
		Assumptions.assumeTrue(ipv6Loopback(), "no IPv6 loopback on this host");
		var a = start("::1", false, proxies("::1/128"));
		var peer = HTTP.send(HttpRequest.newBuilder(URI.create("http://" + a.authority() + "/peer")).build(), BodyHandlers.ofString()).body();
		assertTrue(Set.of("::1", "0:0:0:0:0:0:0:1", "[::1]", "[0:0:0:0:0:0:0:1]", "0:0:0:0:0:0:0:1%0").contains(peer), "Jetty reported: " + peer);
		var write = post(a, "/write", "{}", forwarded(PUBLIC, "app.example", "https"));
		assertEquals(200, write.statusCode(), () -> "peer " + peer + ": " + write.body());
	}

	private static boolean ipv6Loopback() {
		try (var s = new ServerSocket(0, 1, InetAddress.getByName("::1"))) {
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Under {@code ForwardedRequestCustomizer} the HTTP peer is rewritten from {@code X-Forwarded-For}, so a client
	 * chooses it (the reason the docs warn against the combination).  The handshake does not see the rewritten peer (it is
	 * either unknown or the real socket peer, and neither is listed), so the handshake is stricter: it refuses.  This
	 * locks that asymmetry, and fails loudly if Jetty starts rewriting the handshake peer too.
	 */
	@Test void c05_customizerRewrittenPeerIsWhatTheBoundarySees() throws Exception {
		var a = start(true, proxies("10.0.0.0/8"));
		var peer = HTTP.send(HttpRequest.newBuilder(URI.create("http://" + a.authority() + "/peer"))
			.header("X-Forwarded-For", "10.1.2.3").build(), BodyHandlers.ofString()).body();
		var write = post(a, "/write", "{}", "Origin", PUBLIC, "X-Forwarded-For", "10.1.2.3", "X-Forwarded-Host", "app.example", "X-Forwarded-Proto", "https");
		var httpTrusted = write.statusCode() == 200;
		assertEquals("10.1.2.3".equals(peer), httpTrusted, "peer " + peer + ", write " + write.statusCode());
		var path = wsPath(session(a, "Origin", a.origin()));
		var wsTrusted = connects(a, path, "Origin", PUBLIC, "X-Forwarded-For", "10.1.2.3", "X-Forwarded-Host", "app.example", "X-Forwarded-Proto", "https");
		assertTrue(httpTrusted, "HTTP sees the rewritten peer");
		assertFalse(wsTrusted, "the handshake does not see a listed peer");
	}
}
