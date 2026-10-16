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
	}

	/** A {@link CsrfBoundary} that also records the dispatcher type of every request it judges. */
	static final class RecordingBoundary extends CsrfBoundary {
		private final Record record;

		RecordingBoundary(CsrfBoundary.Builder b, Record record) {
			super(b);
			this.record = record;
		}

		@Override /* CsrfBoundary */
		public BoundaryResult check(HttpServletRequest req) {
			record.checked.add(req.getDispatcherType());
			record.servers.add(req.getServerName() + ":" + req.getServerPort());
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
		private final transient Record record;
		WriteServlet(Record record) { this.record = record; }
		@Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
			record.writes.incrementAndGet();
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
		private final transient Record record;
		FailingServlet(Record record) { this.record = record; }
		@Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
			record.writes.incrementAndGet();
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
	record App(Server server, String base, int port, Record record) {
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
		var record = new Record();
		var server = new Server();
		var config = new HttpConfiguration();
		if (forwarded)
			config.addCustomizer(new ForwardedRequestCustomizer());
		var http1 = new HttpConnectionFactory(config);
		var connector = h2c ? new ServerConnector(server, http1, new HTTP2CServerConnectionFactory(config)) : new ServerConnector(server, http1);
		connector.setHost("127.0.0.1");
		connector.setPort(0);
		server.addConnector(connector);

		var ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
		ctx.setContextPath("/");
		ctx.addFilter(new FilterHolder(new RequestBoundaryFilter(new RecordingBoundary(boundary, record))), filterPath, types);
		ctx.addServlet(new ServletHolder(new TokenServlet()), "/token");
		ctx.addServlet(new ServletHolder(new WriteServlet(record)), "/write");
		ctx.addServlet(new ServletHolder(new ForwardServlet()), "/fwd");
		ctx.addServlet(new ServletHolder(new ViewServlet()), "/view/*");
		ctx.addServlet(new ServletHolder(new FailingServlet(record)), "/boom");
		ctx.addServlet(new ServletHolder(new ErrorServlet()), "/error");
		var errors = new ErrorPageErrorHandler();
		errors.addErrorPage(500, "/error");
		ctx.setErrorHandler(errors);
		server.setHandler(ctx);
		server.start();
		var port = connector.getLocalPort();
		app = new App(server, "http://127.0.0.1:" + port, port, record);
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
		assertEquals(1, a.record().writes.get());

		// The same write with no forwarded headers is judged against the socket's own origin, and refused.
		var direct = send(a, "POST", "/write", "Origin", "https://app.example", "Content-Type", JSON,
			LoopbackBoundary.DEFAULT_CSRF_HEADER, tok);
		assertEquals(403, direct.statusCode(), direct::body);
		assertEquals("ORIGIN_MISMATCH", rejection(direct));
		assertEquals(1, a.record().writes.get());
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
		assertEquals(0, a.record().writes.get());

		// The real origin still passes with the same headers present.
		var ok = send(a, "POST", "/write", "X-Forwarded-Proto", "https", "X-Forwarded-Host", "evil.example",
			"Origin", a.origin(), "Content-Type", JSON, csrf, tok);
		assertEquals(200, ok.statusCode(), ok::body);
		assertEquals(1, a.record().writes.get());
	}

	@Test void a03_http2AuthorityIsServerNameButNotHostHeader() throws Exception {
		var a = start(false, true, perProcess(), "/*", REQUEST);
		var tok = token(a);
		a.record().servers.clear();

		// The container reports :authority as the server name, but surfaces no Host header, so a boundary that judges
		// Host refuses the request rather than guessing: even a write whose Origin matches :authority.
		var res = h2cPost(a, "app.example", "http://app.example", tok);
		assertEquals(400, res.status(), res::headers);
		assertEquals("HOST_MISSING", res.rejection());
		assertEquals(List.of("app.example:80"), a.record().servers);
		assertEquals(0, a.record().writes.get());
	}

	/** The status and headers of an HTTP/2 response. */
	record H2Response(int status, String headers, String rejection) {}

	/**
	 * Sends one {@code POST /write} over a prior-knowledge h2c connection to the server's own socket, with the given
	 * {@code :authority}, and returns the response head.  The request is encoded by hand: literal HPACK fields, no Huffman.
	 */
	private static H2Response h2cPost(App a, String authority, String origin, String token) throws Exception {
		var out = new ByteArrayOutputStream();
		out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
		out.write(frame(0x4, 0, 0, new byte[0]));
		var h = new ByteArrayOutputStream();
		h.write(0x83);                                 // :method POST
		h.write(0x86);                                 // :scheme http
		indexedName(h, 4, "/write");                   // :path
		indexedName(h, 1, authority);                  // :authority
		literal(h, "origin", origin);
		literal(h, "content-type", JSON);
		literal(h, LoopbackBoundary.DEFAULT_CSRF_HEADER.toLowerCase(), token);
		literal(h, "content-length", "0");
		out.write(frame(0x1, 0x5, 1, h.toByteArray())); // HEADERS: END_STREAM | END_HEADERS, stream 1

		try (var socket = new Socket("127.0.0.1", a.port())) {
			socket.setSoTimeout(10_000);
			socket.getOutputStream().write(out.toByteArray());
			socket.getOutputStream().flush();
			var in = new DataInputStream(socket.getInputStream());
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

	private static byte[] frame(int type, int flags, int stream, byte[] payload) {
		var b = ByteBuffer.allocate(9 + payload.length);
		b.put((byte)(payload.length >> 16)).put((byte)(payload.length >> 8)).put((byte)payload.length);
		b.put((byte)type).put((byte)flags).putInt(stream).put(payload);
		return b.array();
	}

	/** A literal field without indexing whose name is a static-table entry. */
	private static void indexedName(ByteArrayOutputStream h, int index, String value) throws IOException {
		h.write(index);
		string(h, value);
	}

	/** A literal field without indexing and with a new name. */
	private static void literal(ByteArrayOutputStream h, String name, String value) throws IOException {
		h.write(0);
		string(h, name);
		string(h, value);
	}

	/** A string literal without Huffman coding: a 7-bit-prefix length, then the octets. */
	private static void string(ByteArrayOutputStream h, String value) throws IOException {
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
		a.record().checked.clear();
		var res = send(a, "POST", "/boom", "Origin", a.origin(), "Content-Type", JSON, LoopbackBoundary.DEFAULT_CSRF_HEADER, tok);
		assertEquals(500, res.statusCode(), res::body);
		assertEquals("the error page", res.body());
		assertNull(rejection(res));
		assertEquals(1, a.record().writes.get());
		assertEquals(List.of(DispatcherType.REQUEST), a.record().checked);
	}

	@Test void a04b_forwardDispatchToHtmlViewDoesNotMintOrBreak() throws Exception {
		// The filter covers the view, and the forwarding path lies outside it, so only the forward can be judged.
		var pageLoad = new String[] { "Accept", "text/html", "Sec-Fetch-Mode", "navigate", "Sec-Fetch-Dest", "document" };

		var a = start(false, CsrfBoundary.create(), "/view/*", REQUEST);
		var res = send(a, "GET", "/fwd", pageLoad);
		assertEquals(200, res.statusCode(), res::body);
		assertEquals("<p>view session=false</p>", res.body());
		assertNull(sessionCookie(res));
		assertEquals(List.of(), a.record().checked);
		stopApp();

		var b = start(false, CsrfBoundary.create(), "/view/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		var res2 = send(b, "GET", "/fwd", pageLoad);
		assertEquals(200, res2.statusCode(), res2::body);
		assertEquals("<p>view session=true</p>", res2.body());
		assertNotNull(sessionCookie(res2), () -> res2.headers().map().toString());
		assertEquals(List.of(DispatcherType.FORWARD), b.record().checked);
	}

	@Test void a04c_errorDispatchRegisteredIsChecked() throws Exception {
		var a = start(false, perProcess(), "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
		var tok = token(a);
		a.record().checked.clear();
		var res = send(a, "POST", "/boom", "Origin", a.origin(), "Content-Type", JSON, LoopbackBoundary.DEFAULT_CSRF_HEADER, tok);
		assertEquals(500, res.statusCode(), res::body);
		assertEquals("the error page", res.body());
		assertEquals(List.of(DispatcherType.REQUEST, DispatcherType.ERROR), a.record().checked);
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
		assertEquals(1, a.record().writes.get());

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
		assertEquals(1, a.record().writes.get());
	}
}
