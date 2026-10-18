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
import java.nio.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.filter.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.hpack.*;
import org.eclipse.jetty.http2.server.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * {@link RequestBoundaryFilter} applying a {@link CsrfBoundary} inside a real Jetty 12 (ee11) servlet container, over a
 * real socket: forwarded-header handling, dispatcher types, and a container-issued session cookie.
 *
 * <p>
 * Each test starts its own server on a loopback port chosen by the operating system, with only the configuration that
 * test is about, and stops it afterwards.  The resources behind the filter are plain servlets that record whether they
 * ran, so "refused" means the request never reached them.
 *
 * <p>
 * Binds a socket, so it is off by default.  Run it with {@code -Djuneau.busIt=true}, outside the sandbox.
 */
@EnabledIfSystemProperty(named=CsrfBoundary_JettyIT.GATE, matches="true",
	disabledReason="binds a socket; run with -Dtest='CsrfBoundary_JettyIT' -Djuneau.busIt=true")
@SuppressWarnings({
	"resource" // Servers, connectors and buses are fixtures started in @BeforeAll and stopped in @AfterAll; raw-socket chains and servlet writers are closed by the socket or owned by the container.
})
class CsrfBoundary_JettyIT extends TestBase {

	static final String GATE = "juneau.busIt";

	private static final String JSON = "application/json";

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	//-----------------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------------

	/** Counts the requests that reached the resources, and the dispatches the boundary was asked to judge. */
	static final class Record {
		final AtomicInteger writes = new AtomicInteger();
		final List<DispatcherType> checked = new CopyOnWriteArrayList<>();
		final List<String> servers = new CopyOnWriteArrayList<>();
		final List<List<String>> hosts = new CopyOnWriteArrayList<>();
		final List<String> protocols = new CopyOnWriteArrayList<>();

		void judged(HttpServletRequest req) {
			checked.add(req.getDispatcherType());
			servers.add(req.getServerName() + ":" + req.getServerPort());
			hosts.add(Collections.list(req.getHeaders("Host")));
			protocols.add(req.getProtocol());
		}
	}

	/** A {@link CsrfBoundary} that also records the dispatcher type of every request it judges. */
	static final class RecordingBoundary extends CsrfBoundary {
		private final Record rec;

		RecordingBoundary(CsrfBoundary.Builder b, Record rec) {
			super(b);
			this.rec = rec;
		}

		@Override /* CsrfBoundary */
		public BoundaryResult check(HttpServletRequest req) {
			rec.judged(req);
			return super.check(req);
		}
	}

	/** A {@link LoopbackBoundary} that records what the container reports about every request it judges. */
	static final class RecordingLoopback extends LoopbackBoundary {
		private final Record rec;

		RecordingLoopback(LoopbackBoundary.Builder b, Record rec) {
			super(b);
			this.rec = rec;
		}

		@Override /* LoopbackBoundary */
		public BoundaryResult check(HttpServletRequest req) {
			rec.judged(req);
			return super.check(req);
		}
	}

	/** Writes the token the filter published for this request, so a client can learn it the way a page would. */
	static final class TokenServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.setContentType("text/plain");
			res.getWriter().write(String.valueOf(req.getAttribute(RequestBoundaryFilter.TOKEN_ATTRIBUTE)));
		}
	}

	/** A state-changing resource that counts the requests reaching it. */
	static final class WriteServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private final transient Record rec;
		WriteServlet(Record rec) { this.rec = rec; }
		@Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
			rec.writes.incrementAndGet();
			res.setContentType("text/plain");
			res.getWriter().write("written");
		}
	}

	/** Forwards every request to the HTML view. */
	static final class ForwardServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
			req.getRequestDispatcher("/view/page").forward(req, res);
		}
	}

	/** An HTML view that says whether a session existed by the time it rendered. */
	static final class ViewServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.setContentType("text/html");
			res.getWriter().write("<p>view session=" + (req.getSession(false) != null) + "</p>");
		}
	}

	/** A write that passes the boundary and then fails, so the container dispatches to its error page. */
	static final class FailingServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private final transient Record rec;
		FailingServlet(Record rec) { this.rec = rec; }
		@Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
			rec.writes.incrementAndGet();
			res.sendError(500);
		}
	}

	/** The container's error page. */
	static final class ErrorServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.setStatus(500);
			res.setContentType("text/plain");
			res.getWriter().write("the error page");
		}
	}

	/** A running server and the base URL it answers on. */
	record App(Server server, String base, int port, Record rec) {
		String origin() { return base; }
	}

	private App app;

	@AfterEach void stopApp() throws Exception {
		if (app != null)
			app.server().stop();
		app = null;
	}

	/**
	 * Starts a server on a loopback port with the boundary filter in front of the fixture servlets.
	 *
	 * @param forwarded Whether the connector applies Jetty's {@link ForwardedRequestCustomizer}.
	 * @param boundary The boundary builder; the server's {@link Record} is attached to it.
	 * @param filterPath The path the filter is mapped at.
	 * @param types The dispatcher types the filter is registered for.
	 */
	private App start(boolean forwarded, CsrfBoundary.Builder boundary, String filterPath, EnumSet<DispatcherType> types) throws Exception {
		return start(forwarded, false, boundary, filterPath, types);
	}

	private App start(boolean forwarded, boolean h2c, CsrfBoundary.Builder boundary, String filterPath, EnumSet<DispatcherType> types) throws Exception {
		return start(forwarded, h2c, (port, rec) -> new RecordingBoundary(boundary, rec), filterPath, types);
	}

	/** Builds the boundary once the connector has its port, so a pinned authority can name it. */
	private interface BoundaryFactory {
		RequestBoundary create(int port, Record rec);
	}

	private App start(boolean forwarded, boolean h2c, BoundaryFactory boundary, String filterPath, EnumSet<DispatcherType> types) throws Exception {
		var rec = new Record();
		var server = new Server();
		var config = new HttpConfiguration();
		if (forwarded)
			config.addCustomizer(new ForwardedRequestCustomizer());
		var http1 = new HttpConnectionFactory(config);
		var connector = h2c ? new ServerConnector(server, http1, new HTTP2CServerConnectionFactory(config)) : new ServerConnector(server, http1);
		connector.setHost("127.0.0.1");
		connector.setPort(0);
		server.addConnector(connector);
		connector.open();

		var ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
		ctx.setContextPath("/");
		ctx.addFilter(new FilterHolder(new RequestBoundaryFilter(boundary.create(connector.getLocalPort(), rec))), filterPath, types);
		ctx.addServlet(new ServletHolder(new TokenServlet()), "/token");
		ctx.addServlet(new ServletHolder(new WriteServlet(rec)), "/write");
		ctx.addServlet(new ServletHolder(new ForwardServlet()), "/fwd");
		ctx.addServlet(new ServletHolder(new ViewServlet()), "/view/*");
		ctx.addServlet(new ServletHolder(new FailingServlet(rec)), "/boom");
		ctx.addServlet(new ServletHolder(new ErrorServlet()), "/error");
		var errors = new ErrorPageErrorHandler();
		errors.addErrorPage(500, "/error");
		ctx.setErrorHandler(errors);
		server.setHandler(ctx);
		server.start();
		var port = connector.getLocalPort();
		app = new App(server, "http://127.0.0.1:" + port, port, rec);
		return app;
	}

	private static HttpResponse<String> send(App a, String method, String path, String...headers) throws Exception {
		var b = HttpRequest.newBuilder(URI.create(a.base() + path)).timeout(Duration.ofSeconds(10));
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		b.method(method, "GET".equals(method) ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString("{}"));
		return HTTP.send(b.build(), BodyHandlers.ofString());
	}

	private static String token(App a, String...headers) throws Exception {
		var res = send(a, "GET", "/token", headers);
		assertEquals(200, res.statusCode(), res::body);
		assertTrue(res.body().matches("[0-9a-f]{128}"), res::body);
		return res.body();
	}

	private static String rejection(HttpResponse<String> res) {
		return res.headers().firstValue(RequestBoundaryFilter.REJECTION_HEADER).orElse(null);
	}

	/** The {@code JSESSIONID=...} pair from a response's {@code Set-Cookie}, or <jk>null</jk> if none was set. */
	private static String sessionCookie(HttpResponse<String> res) {
		for (var c : res.headers().allValues("Set-Cookie"))
			if (c.startsWith("JSESSIONID="))
				return c.substring(0, c.indexOf(';') < 0 ? c.length() : c.indexOf(';'));
		return null;
	}

	private static CsrfBoundary.Builder perProcess() {
		return CsrfBoundary.create().tokens(CsrfTokens.perProcess());
	}

	private static final EnumSet<DispatcherType> REQUEST = EnumSet.of(DispatcherType.REQUEST);

	//-----------------------------------------------------------------------------------------------------------------
	// a) Forwarded headers
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_forwardedRequestCustomizerWithContainerMode() throws Exception {
		var a = start(true, perProcess().originSource(CsrfBoundary.OriginSource.CONTAINER), "/*", REQUEST);
		var proxied = new String[] { "X-Forwarded-Proto", "https", "X-Forwarded-Host", "app.example" };
		var tok = token(a, proxied);

		// The page was served as https://app.example by the proxy, so that is the origin its writes carry.
		var ok = send(a, "POST", "/write", "X-Forwarded-Proto", "https", "X-Forwarded-Host", "app.example",
			"Origin", "https://app.example", "Content-Type", JSON, LoopbackBoundary.DEFAULT_CSRF_HEADER, tok);
		assertEquals(200, ok.statusCode(), ok::body);
		assertEquals(1, a.rec().writes.get());

		// The same write with no forwarded headers is judged against the socket's own origin, and refused.
		var direct = send(a, "POST", "/write", "Origin", "https://app.example", "Content-Type", JSON,
			LoopbackBoundary.DEFAULT_CSRF_HEADER, tok);
		assertEquals(403, direct.statusCode(), direct::body);
		assertEquals("ORIGIN_MISMATCH", rejection(direct));
		assertEquals(1, a.rec().writes.get());
	}

	@Test void a02_forwardedHeadersIgnoredWhenNoCustomizerUnderHostHeader() throws Exception {
		var a = start(false, perProcess(), "/*", REQUEST);
		var tok = token(a);
		var csrf = LoopbackBoundary.DEFAULT_CSRF_HEADER;

		// A spoofed scheme does not move the server's own origin to https.
		var scheme = send(a, "POST", "/write", "X-Forwarded-Proto", "https",
			"Origin", "https://127.0.0.1:" + a.port(), "Content-Type", JSON, csrf, tok);
		assertEquals(403, scheme.statusCode(), scheme::body);
		assertEquals("ORIGIN_MISMATCH", rejection(scheme));

		// A spoofed host does not make another site's page this server's own.
		var host = send(a, "POST", "/write", "X-Forwarded-Host", "evil.example", "X-Forwarded-Proto", "http",
			"Origin", "http://evil.example", "Content-Type", JSON, csrf, tok);
		assertEquals(403, host.statusCode(), host::body);
		assertEquals("ORIGIN_MISMATCH", rejection(host));
		assertEquals(0, a.rec().writes.get());

		// The real origin still passes with the same headers present.
		var ok = send(a, "POST", "/write", "X-Forwarded-Proto", "https", "X-Forwarded-Host", "evil.example",
			"Origin", a.origin(), "Content-Type", JSON, csrf, tok);
		assertEquals(200, ok.statusCode(), ok::body);
		assertEquals(1, a.rec().writes.get());
	}

	/**
	 * Lock for J0802: with the container's {@link ForwardedRequestCustomizer} and the default {@code HOST_HEADER}, the
	 * customizer, not the boundary, decides.  A forwarded host makes the request's own origin, and the boundary reads no
	 * forwarded header itself.
	 */
	@Test void a08_forwardedHostTrustedByCustomizerUnderHostHeader() throws Exception {
		var a = start(true, perProcess(), "/*", REQUEST);
		var tok = token(a);
		var ok = send(a, "POST", "/write", "X-Forwarded-Host", "evil.example", "X-Forwarded-Proto", "http",
			"Origin", "http://evil.example", "Content-Type", JSON, LoopbackBoundary.DEFAULT_CSRF_HEADER, tok);
		assertEquals(200, ok.statusCode(), ok::body);
		var plain = send(a, "POST", "/write", "Origin", "http://evil.example", "Content-Type", JSON,
			LoopbackBoundary.DEFAULT_CSRF_HEADER, tok);
		assertEquals(403, plain.statusCode(), plain::body);
		assertEquals("ORIGIN_MISMATCH", rejection(plain));
	}

	@Test void a03_http2AuthorityStandsInForHost() throws Exception {
		var a = start(false, true, perProcess(), "/*", REQUEST);
		var tok = token(a);
		a.rec().servers.clear();
		a.rec().hosts.clear();
		a.rec().protocols.clear();

		// Characterization: the container reports :authority only as the server name and port.  It surfaces no Host
		// header and reports the protocol as HTTP/2.0.  The boundary judges :authority in Host's place.
		var res = h2cPost(a, "app.example", "http://app.example", tok);
		assertEquals(200, res.status(), res::headers);
		assertEquals(List.of("app.example:80"), a.rec().servers);
		assertEquals(List.of(List.of()), a.rec().hosts);
		assertEquals(List.of("HTTP/2.0"), a.rec().protocols);
		assertEquals(1, a.rec().writes.get());

		// A page of another site is still refused: Origin must be the :authority's own origin.
		var foreign = h2cPost(a, "app.example", "http://evil.example", tok);
		assertEquals(403, foreign.status(), foreign::headers);
		assertEquals("ORIGIN_MISMATCH", foreign.rejection());
		assertEquals(1, a.rec().writes.get());
	}

	@Test void a04_http2AuthorityJudgedAgainstAllowedHosts() throws Exception {
		var a = start(false, true, perProcess().allowedHosts("app.example", "127.0.0.1"), "/*", REQUEST);
		var tok = token(a);

		var ok = h2cPost(a, "app.example", "http://app.example", tok);
		assertEquals(200, ok.status(), ok::headers);
		assertEquals(1, a.rec().writes.get());

		var rebound = h2cPost(a, "evil.example", "http://evil.example", tok);
		assertEquals(421, rebound.status(), rebound::headers);
		assertEquals("HOST_NOT_ALLOWED", rebound.rejection());
		assertEquals(1, a.rec().writes.get());
	}

	@Test void a05_http2WithNoAuthorityTakesTheSocketsOwnAddress() throws Exception {
		var a = start(false, true, perProcess(), "/*", REQUEST);
		var tok = token(a);
		a.rec().servers.clear();
		a.rec().hosts.clear();
		a.rec().protocols.clear();

		// Characterization: with no :authority the container reports its own bound address as the server name, still
		// with no Host header.  The boundary therefore judges the server against itself, which no browser can provoke
		// (a browser always sends :authority), and a page of another site is refused all the same.
		var foreign = h2cPost(a, null, null, "http://app.example", tok);
		assertEquals(403, foreign.status(), foreign::headers);
		assertEquals("ORIGIN_MISMATCH", foreign.rejection());
		assertEquals(List.of("127.0.0.1:" + a.port()), a.rec().servers);
		assertEquals(List.of(List.of()), a.rec().hosts);
		assertEquals(List.of("HTTP/2.0"), a.rec().protocols);
		assertEquals(0, a.rec().writes.get());

		var own = h2cPost(a, null, null, a.origin(), tok);
		assertEquals(200, own.status(), own::headers);
		assertEquals(1, a.rec().writes.get());
	}

	@Test void a06_http11RequestClaimingHttp2IsNotTrusted() throws Exception {
		var a = start(false, true, perProcess(), "/*", REQUEST);
		var tok = token(a);
		a.rec().servers.clear();
		a.rec().hosts.clear();
		a.rec().protocols.clear();

		// Spoof lock: the synthesis is guarded on getProtocol() being exactly HTTP/2.0, so it matters that an HTTP/1.1
		// socket cannot claim that.  Jetty refuses the request line itself (426), before any filter runs, so the
		// boundary never sees a Host-less request reporting HTTP/2.0.
		var head = rawHttp(a, "POST /write HTTP/2.0\r\nOrigin: " + a.origin() + "\r\nContent-Type: " + JSON
			+ "\r\n" + LoopbackBoundary.DEFAULT_CSRF_HEADER + ": " + tok + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
		assertTrue(head.startsWith("HTTP/1.1 426 "), head);
		assertEquals(List.of(), a.rec().servers);
		assertEquals(0, a.rec().writes.get());

		// An ordinary HTTP/1.1 request with no Host is refused by the container too (400), so the boundary's own
		// HOST_MISSING never needs the protocol guard for it.
		var noHost = rawHttp(a, "POST /write HTTP/1.1\r\nOrigin: " + a.origin() + "\r\nContent-Type: " + JSON
			+ "\r\n" + LoopbackBoundary.DEFAULT_CSRF_HEADER + ": " + tok + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
		assertTrue(noHost.startsWith("HTTP/1.1 400 "), noHost);
		assertEquals(0, a.rec().writes.get());
	}

	@Test void a07_loopbackBoundaryOverHttp2() throws Exception {
		var csrf = SynchronizerToken.generate();
		var a = start(false, true, (port, rec) -> new RecordingLoopback(
			LoopbackBoundary.create().authority("127.0.0.1", port).token(csrf), rec), "/*", REQUEST);
		a.rec().servers.clear();

		var ok = h2cPost(a, "127.0.0.1:" + a.port(), null, "http://127.0.0.1:" + a.port(), csrf.value());
		assertEquals(200, ok.status(), ok::headers);
		assertEquals(1, a.rec().writes.get());

		var foreign = h2cPost(a, "evil.example:" + a.port(), null, "http://evil.example:" + a.port(), csrf.value());
		assertEquals(421, foreign.status(), foreign::headers);
		assertEquals("HOST_MISMATCH", foreign.rejection());
		assertEquals(1, a.rec().writes.get());

		// With no :authority the container names its own bound address, which is this boundary's authority.  A browser
		// never sends such a request; the residual is a non-browser client that already holds the token.
		var none = h2cPost(a, null, null, "http://127.0.0.1:" + a.port(), csrf.value());
		assertEquals(200, none.status(), none::headers);
		assertEquals(2, a.rec().writes.get());

		var wrongPort = h2cPost(a, "127.0.0.1:" + (a.port() == 65535 ? 1 : a.port() + 1), null, "http://127.0.0.1:" + a.port(), csrf.value());
		assertEquals(421, wrongPort.status(), wrongPort::headers);
		assertEquals(2, a.rec().writes.get());
	}

	/** The status and headers of an HTTP/2 response. */
	record H2Response(int status, String headers, String rejection) {}

	/**
	 * Sends one {@code POST /write} over a prior-knowledge h2c connection to the server's own socket, with the given
	 * {@code :authority}, and returns the response head.  The request is encoded by hand: literal HPACK fields, no Huffman.
	 */
	private static H2Response h2cPost(App a, String authority, String origin, String token) throws Exception {
		return h2cPost(a, authority, null, origin, token);
	}

	/** As above, with an optional {@code :authority} (<jk>null</jk> omits it) and an optional literal {@code host} field. */
	private static H2Response h2cPost(App a, String authority, String host, String origin, String token) throws Exception {
		var out = new ByteArrayOutputStream();
		out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
		out.write(frame(0x4, 0, 0, new byte[0]));
		var h = new ByteArrayOutputStream();
		h.write(0x83);                                 // :method POST
		h.write(0x86);                                 // :scheme http
		indexedName(h, 4, "/write");                   // :path
		if (authority != null)
			indexedName(h, 1, authority);              // :authority
		if (host != null)
			literal(h, "host", host);
		literal(h, "origin", origin);
		literal(h, "content-type", JSON);
		literal(h, LoopbackBoundary.DEFAULT_CSRF_HEADER.toLowerCase(), token);
		literal(h, "content-length", "0");
		out.write(frame(0x1, 0x5, 1, h.toByteArray())); // HEADERS: END_STREAM | END_HEADERS, stream 1

		try (var socket = new Socket("127.0.0.1", a.port()); var in = new DataInputStream(socket.getInputStream())) {
			socket.setSoTimeout(10_000);
			socket.getOutputStream().write(out.toByteArray());
			socket.getOutputStream().flush();
			var decoder = new HpackDecoder(8192, () -> 0L);
			for (;;) {
				var head = new byte[9];
				in.readFully(head);
				var len = ((head[0] & 0xFF) << 16) | ((head[1] & 0xFF) << 8) | (head[2] & 0xFF);
				var payload = new byte[len];
				in.readFully(payload);
				if (head[3] != 0x1)  // Skip SETTINGS, WINDOW_UPDATE and the like until the response HEADERS.
					continue;
				var meta = (MetaData.Response)decoder.decode(ByteBuffer.wrap(payload));
				return new H2Response(meta.getStatus(), meta.getHttpFields().toString(), meta.getHttpFields().get(RequestBoundaryFilter.REJECTION_HEADER));
			}
		}
	}

	/** Writes raw bytes to the server's socket and returns everything it sends back until it closes. */
	private static String rawHttp(App a, String request) throws Exception {
		try (var socket = new Socket("127.0.0.1", a.port())) {
			socket.setSoTimeout(10_000);
			socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
			socket.getOutputStream().flush();
			return new String(socket.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
		}
	}

	static byte[] frame(int type, int flags, int stream, byte[] payload) {
		var b = ByteBuffer.allocate(9 + payload.length);
		b.put((byte)(payload.length >> 16)).put((byte)(payload.length >> 8)).put((byte)payload.length);
		b.put((byte)type).put((byte)flags).putInt(stream).put(payload);
		return b.array();
	}

	/** A literal field without indexing whose name is a static-table entry. */
	static void indexedName(ByteArrayOutputStream h, int index, String value) throws IOException {
		h.write(index);
		string(h, value);
	}

	/** A literal field without indexing and with a new name. */
	static void literal(ByteArrayOutputStream h, String name, String value) throws IOException {
		h.write(0);
		string(h, name);
		string(h, value);
	}

	/** A string literal without Huffman coding: a 7-bit-prefix length, then the octets. */
	static void string(ByteArrayOutputStream h, String value) throws IOException {
		var v = value.getBytes(StandardCharsets.US_ASCII);
		if (v.length < 127) {
			h.write(v.length);
		} else {
			h.write(127);
			var rest = v.length - 127;
			for (; rest >= 128; rest >>= 7)
				h.write((rest & 0x7F) | 0x80);
			h.write(rest);
		}
		h.write(v);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Dispatcher types
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a04_errorDispatchNotRechecked() throws Exception {
		var a = start(false, perProcess(), "/*", REQUEST);
		var tok = token(a);
		a.rec().checked.clear();
		var res = send(a, "POST", "/boom", "Origin", a.origin(), "Content-Type", JSON, LoopbackBoundary.DEFAULT_CSRF_HEADER, tok);
		assertEquals(500, res.statusCode(), res::body);
		assertEquals("the error page", res.body());
		assertNull(rejection(res));
		assertEquals(1, a.rec().writes.get());
		assertEquals(List.of(DispatcherType.REQUEST), a.rec().checked);
	}

	@Test void a04b_forwardDispatchToHtmlViewDoesNotMintOrBreak() throws Exception {
		// The filter covers the view, and the forwarding path lies outside it, so only the forward can be judged.
		var pageLoad = new String[] { "Accept", "text/html", "Sec-Fetch-Mode", "navigate", "Sec-Fetch-Dest", "document" };

		var a = start(false, CsrfBoundary.create(), "/view/*", REQUEST);
		var res = send(a, "GET", "/fwd", pageLoad);
		assertEquals(200, res.statusCode(), res::body);
		assertEquals("<p>view session=false</p>", res.body());
		assertNull(sessionCookie(res));
		assertEquals(List.of(), a.rec().checked);
		stopApp();

		var b = start(false, CsrfBoundary.create(), "/view/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		var res2 = send(b, "GET", "/fwd", pageLoad);
		assertEquals(200, res2.statusCode(), res2::body);
		assertEquals("<p>view session=true</p>", res2.body());
		assertNotNull(sessionCookie(res2), () -> res2.headers().map().toString());
		assertEquals(List.of(DispatcherType.FORWARD), b.rec().checked);
	}

	@Test void a04c_errorDispatchRegisteredIsChecked() throws Exception {
		var a = start(false, perProcess(), "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
		var tok = token(a);
		a.rec().checked.clear();
		var res = send(a, "POST", "/boom", "Origin", a.origin(), "Content-Type", JSON, LoopbackBoundary.DEFAULT_CSRF_HEADER, tok);
		assertEquals(500, res.statusCode(), res::body);
		assertEquals("the error page", res.body());
		assertEquals(List.of(DispatcherType.REQUEST, DispatcherType.ERROR), a.rec().checked);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Sessions
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a05_realSessionCookieIssuedAndValidated() throws Exception {
		var a = start(false, CsrfBoundary.create(), "/*", REQUEST);
		var csrf = LoopbackBoundary.DEFAULT_CSRF_HEADER;

		// A script's fetch gets no session; a page load does, before the page is written.
		assertNull(sessionCookie(send(a, "GET", "/token", "Accept", "*/*")));
		var page = send(a, "GET", "/token", "Accept", "text/html");
		assertEquals(200, page.statusCode(), page::body);
		var cookie = sessionCookie(page);
		assertNotNull(cookie, () -> page.headers().map().toString());
		var tok = page.body();

		var ok = send(a, "POST", "/write", "Cookie", cookie, "Origin", a.origin(), "Content-Type", JSON, csrf, tok);
		assertEquals(200, ok.statusCode(), ok::body);
		assertEquals(1, a.rec().writes.get());

		// The token belongs to that session: without the cookie, or with another session's, it is refused.
		var noSession = send(a, "POST", "/write", "Origin", a.origin(), "Content-Type", JSON, csrf, tok);
		assertEquals(403, noSession.statusCode(), noSession::body);
		assertEquals("CSRF_TOKEN_MISMATCH", rejection(noSession));

		var other = sessionCookie(send(a, "GET", "/token", "Accept", "text/html"));
		assertNotNull(other);
		assertNotEquals(cookie, other);
		var wrong = send(a, "POST", "/write", "Cookie", other, "Origin", a.origin(), "Content-Type", JSON, csrf, tok);
		assertEquals(403, wrong.statusCode(), wrong::body);
		assertEquals("CSRF_TOKEN_MISMATCH", rejection(wrong));
		assertEquals(1, a.rec().writes.get());
	}
}
