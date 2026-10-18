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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * A {@link CsrfBoundary} behind a real {@link RequestBoundaryFilter}, attacked and used from a real headless browser.
 *
 * <p>
 * The application is a Juneau {@link RestServlet} in a Jetty servlet container, with the boundary filter mapped at
 * {@code /*} in front of it.  Its pages embed the token the filter publishes, the way a page renderer does, and load
 * the real {@code juneau-views.js} and {@code juneau-console.js}.  A second Jetty server on another port plays the
 * attacker: its pages make the browser send forms and fetches to the application.  Every attack is made by the
 * browser itself, so the {@code Origin} and {@code Sec-Fetch-*} headers the boundary judges are the ones a browser
 * really sends.
 *
 * <p>
 * Both servers listen on {@code 127.0.0.1}, so the attacker is a different origin (another port) but the same site:
 * the application's session cookie still rides on the attacker's requests, exactly as it would for a sibling
 * application on the same host.  The resources the attacks target are plain counters that need no session, so the
 * only thing standing between an attack and a changed counter is the boundary.  A recording filter in front of the
 * boundary notes every request and the answer it got, so an attack the browser never sent cannot pass as refused.
 *
 * <p>
 * Loopback addresses are one site, so those attacks arrive as {@code same-site}.  For a {@code cross-site} attack the
 * browser is told to resolve two registrable names, {@value #SITE_HOST} and {@value #EVIL_HOST}, to {@code 127.0.0.1}:
 * a third Jetty server, behind a boundary that answers only to {@value #SITE_HOST}, is the application, and the
 * attacker's server is reached as {@value #EVIL_HOST}.  Nothing leaves the machine.  A browser sends
 * {@code Sec-Fetch-Site} only to a trustworthy origin, so that browser treats those two origins, and no others, as
 * secure, as it would over https.  Without that switch the browser sends no {@code Sec-Fetch-*} headers to a plain
 * http host name, and the headless shell a bare Playwright launch starts ignores it, so these rows need the full
 * Chromium Playwright installs (channel {@code chromium}).  If only the headless shell is installed, the prober fails
 * at launch and the whole class fails.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  The prober is {@code csrf-boundary.cjs}, next to the profile's harness.
 */
@EnabledIfSystemProperty(named=CsrfBoundary_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
@SuppressWarnings({
	"resource" // Servlet response writers belong to the container, and each connector belongs to the server it is added to; none is closed here.
})
class CsrfBoundary_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static final String CSRF_HEADER = LoopbackBoundary.DEFAULT_CSRF_HEADER;

	/** The application's public host name for the cross-site rows; the browser resolves it to {@code 127.0.0.1}. */
	static final String SITE_HOST = "app.test";

	/** The attacker's host name for the cross-site rows: another registrable domain, so another site. */
	static final String EVIL_HOST = "evil.test";

	//-----------------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------------

	/** One request as it arrived in front of the boundary, and the answer it got. */
	record Seen(String method, String path, String host, String origin, String fetchSite, int status, String rejection,
		boolean corsHeaders) {}

	/** Every request the application received, in arrival order. */
	static final List<Seen> SEEN = new CopyOnWriteArrayList<>();

	/** The counters behind {@code POST /count/{name}}: the state the attacks try to change. */
	static final Map<String,AtomicInteger> COUNTS = new ConcurrentHashMap<>();

	/** Records each request and its answer; registered ahead of the boundary filter. */
	static final class Recorder implements Filter {
		@Override /* Filter */
		public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
			try {
				chain.doFilter(req, res);
			} finally {
				var q = (HttpServletRequest)req;
				var s = (HttpServletResponse)res;
				var cors = s.getHeaderNames().stream().anyMatch(n -> n.toLowerCase(Locale.ROOT).startsWith("access-control-allow-"));
				SEEN.add(new Seen(q.getMethod(), q.getRequestURI(), q.getHeader("Host"), q.getHeader("Origin"),
					q.getHeader("Sec-Fetch-Site"), s.getStatus(), s.getHeader(RequestBoundaryFilter.REJECTION_HEADER), cors));
			}
		}
	}

	/** The application: two token-bearing pages, the counters, and a session-id change. */
	@Rest
	public static class AppResource extends RestServlet {
		private static final long serialVersionUID = 1L;

		/** A toolkit page: the token in a meta tag, on the body, and on a table, plus a plain HTML form. */
		@RestGet("/page")
		public void page(RestRequest req, RestResponse res) throws IOException {
			var tokens = tokens(req.getHttpServletRequest());
			writeHtml(res.getHttpServletResponse(), "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
				+ "<meta name=\"csrf-token\" content=\"" + attr(tokens.get()) + "\">"
				+ "<link rel=\"icon\" href=\"data:,\"></head>\n"
				+ "<body data-juneau-csrf=\"" + attr(tokens.get()) + "\" data-juneau-csrf-header=\"" + CSRF_HEADER + "\">\n"
				+ "<table id=\"rows\" data-juneau-csrf=\"" + attr(tokens.get()) + "\"><tbody><tr>"
				+ "<td class=\"juneau-view-actions-cell\"></td></tr></tbody></table>\n"
				+ "<form id=\"plain-form\" method=\"post\" action=\"/count/a08\"><input name=\"x\" value=\"1\"></form>\n"
				+ "<script src=\"/assets/juneau-views.js\"></script>\n"
				+ "</body></html>\n");
		}

		/** A console page: the token on the body, and one card whose handler hands its context to the prober. */
		@RestGet("/console")
		public void console(RestRequest req, RestResponse res) throws IOException {
			var tokens = tokens(req.getHttpServletRequest());
			writeHtml(res.getHttpServletResponse(), "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
				+ "<link rel=\"icon\" href=\"data:,\"></head>\n"
				+ "<body data-juneau-csrf=\"" + attr(tokens.get()) + "\" data-juneau-csrf-header=\"" + CSRF_HEADER + "\">\n"
				+ "<script type=\"application/json\" id=\"juneau-page\">"
				+ "{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[{\"id\":\"probe\",\"type\":\"probe\"}]}</script>\n"
				+ "<script>window.JuneauConsoleCards = [[\"probe\", function (card, el, ctx) { window.cardContext = ctx; }]];</script>\n"
				+ "<script src=\"/assets/juneau-console.js\"></script>\n"
				+ "</body></html>\n");
		}

		/** A state-changing resource that needs no session, so only the boundary stands in front of it. */
		@RestPost("/count/{name}")
		public void count(@org.apache.juneau.http.Path("name") String name, RestResponse res) throws IOException {
			if (! name.matches("[a-z0-9]{1,8}"))
				throw new IllegalArgumentException("bad counter name");
			var n = COUNTS.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
			writeJson(res.getHttpServletResponse(), "{\"outcome\":\"success\",\"message\":\"counted\",\"count\":" + n + "}");
		}

		/** Gives the caller's session a new id, as an application does after a login. */
		@RestGet("/rotate")
		public void rotate(RestRequest req, RestResponse res) throws IOException {
			req.getHttpServletRequest().changeSessionId();
			writeJson(res.getHttpServletResponse(), "{}");
		}

		private static Supplier<?> tokens(HttpServletRequest req) {
			if (req.getAttribute(RequestBoundaryFilter.TOKEN_SUPPLIER_ATTRIBUTE) instanceof Supplier<?> s)
				return s;
			throw new IllegalStateException("the boundary filter did not publish a token supplier");
		}

		private static void writeHtml(HttpServletResponse res, String html) throws IOException {
			RequestBoundaryFilter.markTokenBearing(res);
			res.setContentType("text/html;charset=utf-8");
			res.getWriter().write(html);
		}

		private static void writeJson(HttpServletResponse res, String json) throws IOException {
			res.setContentType("application/json");
			res.getWriter().write(json);
		}

		private static String attr(Object value) {
			return String.valueOf(value).replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
		}
	}

	/** Serves the real shipped scripts from the classpath. */
	static final class AssetServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			var path = switch (String.valueOf(req.getPathInfo())) {
				case "/juneau-views.js" -> ViewsMixin.VIEWS_JS_RESOURCE;
				case "/juneau-console.js" -> ConsoleChromeMixin.CONSOLE_JS_RESOURCE;
				default -> null;
			};
			if (path == null) {
				res.sendError(404);
				return;
			}
			res.setContentType("text/javascript;charset=utf-8");
			res.getWriter().write(resource(path));
		}
	}

	/** The attacker's pages, each aimed at its own counter on the application. */
	static final class AttackServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private final String app;
		private final String site;
		AttackServlet(String app, String site) {
			this.app = app;
			this.site = site;
		}
		@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			var body = switch (String.valueOf(req.getPathInfo())) {
				// A form that submits itself as soon as the page loads.
				case "/form" -> "<form id=\"f\" method=\"post\" action=\"" + app + "/count/a03\"><input name=\"x\" value=\"1\"></form>"
					+ "<script>document.getElementById('f').submit();</script>";
				// The same form, aimed at the application on another site.
				case "/cross-site-form" -> "<form id=\"f\" method=\"post\" action=\"" + site + "/count/a12\"><input name=\"x\" value=\"1\"></form>"
					+ "<script>document.getElementById('f').submit();</script>";
				// A fire-and-forget fetch: the response is opaque to the attacker, but the request is still sent.
				case "/no-cors" -> "<script>fetch('" + app + "/count/a04', { method: 'POST', mode: 'no-cors', credentials: 'include',"
					+ " headers: { 'Content-Type': 'text/plain' }, body: 'x=1' })"
					+ ".then(function (r) { window.attackDone = 'sent:' + r.type; }, function (e) { window.attackDone = 'failed:' + e; });</script>";
				// A sandboxed frame has an opaque origin, so its form is sent with 'Origin: null'.
				case "/sandboxed" -> "<iframe sandbox=\"allow-scripts allow-forms\" srcdoc=\"<form method='post' action='" + app
					+ "/count/a05'><input name='x' value='1'></form><script>document.forms[0].submit();</script>\"></iframe>";
				// A JSON write needs a preflight; the application never answers one with CORS permission.
				case "/json" -> "<script>fetch('" + app + "/count/a06', { method: 'POST', credentials: 'include',"
					+ " headers: { 'Content-Type': 'application/json' }, body: '{}' })"
					+ ".then(function (r) { window.attackDone = 'answered:' + r.status; }, function (e) { window.attackDone = 'failed:' + e; });</script>";
				default -> null;
			};
			if (body == null) {
				res.sendError(404);
				return;
			}
			res.setContentType("text/html;charset=utf-8");
			res.getWriter().write("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\"></head><body>"
				+ body + "</body></html>");
		}
	}

	/** Answers anything; it sits behind a loopback boundary that should refuse the request first. */
	static final class PlainServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		@Override protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
			res.setContentType("text/plain");
			res.getWriter().write("reached");
		}
	}

	private static final List<Server> SERVERS = new ArrayList<>();
	private static int appPort;
	private static String appOrigin;
	private static String attackOrigin;
	private static int sitePort;
	private static String siteOrigin;
	private static String crossSiteAttackOrigin;
	private static Map<?,?> report;

	/** A server with one connector on {@code 127.0.0.1}, already listening, so its port is known before its handlers. */
	private static ServerConnector listen(Server server) throws Exception {
		var connector = new ServerConnector(server);
		connector.setHost("127.0.0.1");
		connector.setPort(0);
		server.addConnector(connector);
		connector.open();
		SERVERS.add(server);
		return connector;
	}

	@BeforeAll
	static void startAndProbe() throws Exception {
		var app = new Server();
		appPort = listen(app).getLocalPort();
		appOrigin = "http://127.0.0.1:" + appPort;
		var appCtx = new ServletContextHandler(ServletContextHandler.SESSIONS);
		appCtx.setContextPath("/");
		var request = EnumSet.of(DispatcherType.REQUEST);
		appCtx.addFilter(new FilterHolder(new Recorder()), "/*", request);
		appCtx.addFilter(new FilterHolder(new RequestBoundaryFilter(CsrfBoundary.create().build())), "/*", request);
		appCtx.addServlet(new ServletHolder(new AssetServlet()), "/assets/*");
		appCtx.addServlet(new ServletHolder(new AppResource()), "/*");
		app.setHandler(appCtx);
		app.start();

		// The same application on its public host name, behind a boundary that answers to that name only.
		var site = new Server();
		sitePort = listen(site).getLocalPort();
		siteOrigin = "http://" + SITE_HOST + ":" + sitePort;
		var siteCtx = new ServletContextHandler(ServletContextHandler.SESSIONS);
		siteCtx.setContextPath("/");
		siteCtx.addFilter(new FilterHolder(new Recorder()), "/*", request);
		siteCtx.addFilter(new FilterHolder(new RequestBoundaryFilter(CsrfBoundary.create().allowedHosts(SITE_HOST).build())), "/*", request);
		siteCtx.addServlet(new ServletHolder(new AssetServlet()), "/assets/*");
		siteCtx.addServlet(new ServletHolder(new AppResource()), "/*");
		site.setHandler(siteCtx);
		site.start();

		var attacker = new Server();
		var attackPort = listen(attacker).getLocalPort();
		attackOrigin = "http://127.0.0.1:" + attackPort;
		crossSiteAttackOrigin = "http://" + EVIL_HOST + ":" + attackPort;
		var attackCtx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		attackCtx.setContextPath("/");
		attackCtx.addServlet(new ServletHolder(new AttackServlet(appOrigin, siteOrigin)), "/attack/*");
		attacker.setHandler(attackCtx);
		attacker.start();

		// The contrast for the host-name row: a loopback boundary that answers only to its own 127.0.0.1 authority.
		var loopback = new Server();
		var loopbackPort = listen(loopback).getLocalPort();
		var loopbackCtx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		loopbackCtx.setContextPath("/");
		loopbackCtx.addFilter(new FilterHolder(new LoopbackBoundaryFilter(
			LoopbackBoundary.create().authority("127.0.0.1", loopbackPort).token(SynchronizerToken.generate()).build())), "/*", request);
		loopbackCtx.addServlet(new ServletHolder(new PlainServlet()), "/*");
		loopback.setHandler(loopbackCtx);
		loopback.start();

		report = Json.to(runProber(appOrigin, "http://localhost:" + appPort, attackOrigin, "http://localhost:" + loopbackPort,
			siteOrigin, crossSiteAttackOrigin), Map.class);
	}

	@AfterAll
	static void stopServers() throws Exception {
		for (var s : SERVERS)
			s.stop();
		SERVERS.clear();
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String runProber(String...args) throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("csrf-boundary.cjs");
		var cmd = new ArrayList<>(List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString()));
		cmd.addAll(List.of(args));
		var stdout = dir.resolve("csrf-boundary-stdout.json");
		var stderr = dir.resolve("csrf-boundary-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (! p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	@SuppressWarnings({
		"unchecked" // row() casts an entry of the JS-harness JSON report to Map<String,Object>
	})
	private static Map<String,Object> row(String key) {
		var m = (Map<String,Object>)report.get(key);
		assertNotNull(m, () -> "no '" + key + "' entry in the prober report: " + report);
		return m;
	}

	@SuppressWarnings({
		"unchecked" // sub() casts a nested entry of the JS-harness JSON report to Map<String,Object>
	})
	private static Map<String,Object> sub(Map<String,Object> m, String key) {
		var v = (Map<String,Object>)m.get(key);
		assertNotNull(v, () -> "no '" + key + "' in " + m);
		return v;
	}

	private static int count(String name) {
		var c = COUNTS.get(name);
		return c == null ? 0 : c.get();
	}

	/** The requests the application received for one method and path. */
	private static List<Seen> seen(String method, String path) {
		return SEEN.stream().filter(s -> s.method().equals(method) && s.path().equals(path)).toList();
	}

	/** The one request the application received for a method and path. */
	private static Seen seenOnce(String method, String path) {
		var l = seen(method, path);
		assertEquals(1, l.size(), () -> method + " " + path + " arrived " + l.size() + " times: " + SEEN);
		return l.get(0);
	}

	private static int num(Object o) {
		return ((Number)o).intValue();
	}

	@Test void a00_noPageErrors() {
		assertEquals(List.of(), report.get("jsFailures"), () -> report.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) The application's own pages can write
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_toolkitRowActionSucceedsOnNonLoopbackHostName() {
		var r = row("a01");
		// A loopback boundary refuses the 'localhost' spelling of a 127.0.0.1 server outright.
		assertEquals(421, num(r.get("loopbackStatus")), r::toString);
		assertEquals("HOST_MISMATCH", r.get("loopbackReason"), r::toString);

		// The same spelling serves the page and admits its row action under the CSRF boundary.
		assertEquals(200, num(r.get("pageStatus")), r::toString);
		var action = sub(r, "action");
		assertEquals("success", action.get("state"), r::toString);
		assertEquals(Boolean.TRUE, action.get("visible"), r::toString);
		var s = seenOnce("POST", "/count/a01");
		assertEquals("localhost:" + appPort, s.host());
		assertEquals("http://localhost:" + appPort, s.origin());
		assertEquals(200, s.status());
		assertEquals(1, count("a01"));
	}

	@Test void a02_consoleWriteSucceeds() {
		var r = row("a02");
		assertEquals(Boolean.TRUE, r.get("mounted"), r::toString);
		assertEquals(Boolean.TRUE, r.get("ok"), r::toString);
		assertEquals(1, num(r.get("count")), r::toString);
		var s = seenOnce("POST", "/count/a02");
		assertEquals(appOrigin, s.origin());
		assertEquals(200, s.status());
		assertEquals(1, count("a02"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Another origin's pages cannot
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a03_attackerAutoSubmitFormDoesNotChangeState() {
		var r = row("a03");
		assertEquals(403, num(r.get("status")), r::toString);
		var s = seenOnce("POST", "/count/a03");
		assertEquals(attackOrigin, s.origin());
		assertEquals("same-site", s.fetchSite());
		assertEquals(403, s.status());
		assertEquals("ORIGIN_MISMATCH", s.rejection());
		assertEquals(0, count("a03"));
	}

	@Test void a04_attackerNoCorsFetchRefused() {
		var r = row("a04");
		assertEquals("sent:opaque", r.get("result"), r::toString);
		var s = seenOnce("POST", "/count/a04");
		assertEquals(attackOrigin, s.origin());
		assertEquals(403, s.status());
		assertEquals("ORIGIN_MISMATCH", s.rejection());
		assertEquals(0, count("a04"));
	}

	@Test void a05_attackerSandboxedIframeOriginNullRefused() {
		var r = row("a05");
		assertEquals(403, num(r.get("status")), r::toString);
		var s = seenOnce("POST", "/count/a05");
		assertEquals("null", s.origin());
		assertEquals(403, s.status());
		assertEquals("ORIGIN_NULL", s.rejection());
		assertEquals(0, count("a05"));
	}

	@Test void a06_attackerJsonFetchPreflightedAndNeverAnswered() {
		var r = row("a06");
		assertTrue(String.valueOf(r.get("result")).startsWith("failed:"), r::toString);
		// The browser asked first, was given no CORS permission, and so never sent the write.
		var preflight = seenOnce("OPTIONS", "/count/a06");
		assertEquals(attackOrigin, preflight.origin());
		assertFalse(preflight.corsHeaders(), preflight::toString);
		assertEquals(List.of(), seen("POST", "/count/a06"));
		assertEquals(0, count("a06"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Failures on the application's own pages are visible and closed
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a07_pageOpenAcrossSessionIdChangeShowsReason() {
		var r = row("a07");
		assertEquals("success", sub(r, "before").get("state"), r::toString);
		assertEquals(200, num(r.get("rotateStatus")), r::toString);
		assertEquals(Boolean.TRUE, r.get("sessionChanged"), r::toString);
		// The page still holds the old session's token: the write is refused, and the page says why.
		var after = sub(r, "after");
		assertEquals("refusal", after.get("state"), r::toString);
		assertEquals(Boolean.TRUE, after.get("visible"), r::toString);
		assertTrue(String.valueOf(after.get("text")).contains("CSRF_TOKEN_MISMATCH"), r::toString);
		var writes = seen("POST", "/count/a07");
		assertEquals(List.of(200, 403), writes.stream().map(Seen::status).toList(), writes::toString);
		assertEquals(1, count("a07"));
	}

	@Test void a08_htmlFormOnOwnPageFailsClosed() {
		var r = row("a08");
		assertEquals(403, num(r.get("status")), r::toString);
		var s = seenOnce("POST", "/count/a08");
		assertEquals(appOrigin, s.origin());
		assertEquals("CSRF_TOKEN_MISSING", s.rejection());
		assertEquals(0, count("a08"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d) The embedded tokens
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a09_metaAndBodyTokensDiffer() {
		var r = row("a09");
		assertEquals(Boolean.TRUE, r.get("present"), r::toString);
		assertEquals(Boolean.TRUE, r.get("differ"), r::toString);
		assertEquals(200, num(r.get("metaStatus")), r::toString);
		assertEquals(200, num(r.get("bodyStatus")), r::toString);
		assertEquals(2, count("a09"));
	}

	@Test void a10_tokenPageNavigationResponseIsNoStore() {
		var r = row("a10");
		assertEquals(200, num(r.get("status")), r::toString);
		assertEquals(Boolean.TRUE, r.get("sessionCookie"), r::toString);
		// Exactly this value: a second Cache-Control value (say, a public one) would appear joined onto it.
		assertEquals("private, no-store", r.get("cacheControl"), r::toString);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e) Another site's pages
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a11_ownSiteRowActionAdmitted() {
		// The control for a12: the boundary is configured for this host name, so its own page can write.
		var r = row("a11");
		assertEquals(200, num(r.get("pageStatus")), r::toString);
		assertEquals("success", sub(r, "action").get("state"), r::toString);
		var s = seenOnce("POST", "/count/a11");
		assertEquals(SITE_HOST + ":" + sitePort, s.host());
		assertEquals(siteOrigin, s.origin());
		assertEquals("same-origin", s.fetchSite());
		assertEquals(200, s.status());
		assertNull(s.rejection());
		assertEquals(1, count("a11"));
	}

	// The Origin check refuses this before the fetch-site check runs; fetchSite proves the browser saw it as cross-site.
	// FETCH_SITE_NOT_SAME_ORIGIN itself is covered by CsrfBoundary_Test.
	@Test void a12_crossSiteAttackerFormRefused() {
		var r = row("a12");
		assertEquals(403, num(r.get("status")), r::toString);
		var s = seenOnce("POST", "/count/a12");
		assertEquals(SITE_HOST + ":" + sitePort, s.host());
		assertEquals(crossSiteAttackOrigin, s.origin());
		assertEquals("cross-site", s.fetchSite());
		assertEquals(403, s.status());
		assertEquals("ORIGIN_MISMATCH", s.rejection());
		assertEquals(0, count("a12"));
	}
}
