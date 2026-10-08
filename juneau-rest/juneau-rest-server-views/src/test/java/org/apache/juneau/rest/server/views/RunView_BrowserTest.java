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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.runreport.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.runreport.RunEvent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import com.sun.net.httpserver.*;

/**
 * The real-browser canary for the run-view region: the real {@code juneau-run-view.js} populator, mounted through
 * the real region runtime, polling the real {@link RunViewMixin} endpoint.
 *
 * <p>
 * A JDK {@link HttpServer} serves the fixture pages, a control API the prober drives ({@code /ctl/<runId>/<action>}),
 * and {@code /juneau-run-view/*}, which it forwards to a {@link MockRestClient} over a resource implementing
 * {@link RunViewMixin}.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does: {@code mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test}.
 *
 * @since 10.0.0
 */
@EnabledIfSystemProperty(named=RunView_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class RunView_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	/** The five console-ui themes. */
	static final List<String> THEMES = List.of("gray", "light-brown", "light-red", "open", "red");

	@Rest
	public static class R extends BasicRestServlet implements RunViewMixin {
		private static final long serialVersionUID = 1L;
		static final Map<String,RunViewSource> SOURCES = new ConcurrentHashMap<>();

		@Override
		public Optional<RunViewSource> runViewSource(String runId, RestRequest req) {
			return Optional.ofNullable(SOURCES.get(runId));
		}
	}

	private static final Map<String,RunViewLog> LOGS = new ConcurrentHashMap<>();

	private static MockRestClient mock;
	private static HttpServer server;
	private static int port;
	private static Map<?,?> report;

	//-----------------------------------------------------------------------------------------------------------------
	// Fixture runs
	//-----------------------------------------------------------------------------------------------------------------

	private static RunViewLog run(String id) {
		var log = RunViewLog.create();
		LOGS.put(id, log);
		R.SOURCES.put(id, log);
		return log;
	}

	private static void failedRun(RunViewLog log) {
		log.append(RunEvent.step("build", "Build"));
		log.append(RunEvent.suite("build", "surefire", "FooTest", 1, 1, 0));
		log.append(RunEvent.test("build", "surefire", "FooTest", "a", TestStatus.PASS));
		log.append(RunEvent.test("build", "surefire", "FooTest", "b", TestStatus.FAIL).withMsg("expected 1\nbut was 2").withRawLine(12));
		log.append(RunEvent.end("build", EndStatus.FAIL));
		log.append(RunEvent.done(DoneStatus.FAIL));
	}

	private static void setUpRuns() {
		run("a01").append(RunEvent.step("build", "Build"));
		failedRun(run("a02"));
		failedRun(run("a03"));
		failedRun(run("a04"));
		run("a05");
		run("a06").append(RunEvent.step("old", "Before reset"));
		failedRun(run("a07"));

		var a08 = run("a08");
		a08.append(RunEvent.step("big", "Big"));
		a08.append(RunEvent.suite("big", "junit-xml", "BigTest", 12000, 0, 0));
		var batch = new ArrayList<RunEvent>();
		for (var i = 1; i <= 12000; i++)
			batch.add(RunEvent.test("big", "junit-xml", "BigTest", "t" + i, TestStatus.PASS));
		a08.appendAll(batch);
		a08.append(RunEvent.end("big", EndStatus.OK));
		a08.append(RunEvent.done(DoneStatus.OK));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fixture pages
	//-----------------------------------------------------------------------------------------------------------------

	private static String region(RunViewDef def) {
		return RegionTable.of(def.validate().toRegion()).toString();
	}

	private static RunViewDef def(String id, String runId) {
		return RunViewDef.forMixin(id, "", runId).refreshMs(1000).rawHref("#raw-L{line}");
	}

	private static String body(String c) {
		return switch (c) {
			case "a04" -> region(def("a04", "a04")) + "\n" + region(def("a04c", "a04").compact(true));
			case "a05" -> region(RunViewDef.create("a05").poll(false));
			default -> region(def(c, c).title("Run " + c));
		};
	}

	private static String resource(Class<?> anchor, String path) throws IOException {
		try (var in = anchor.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path + " (relative to " + anchor.getName() + ")");
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static List<String> scripts() throws IOException {
		var nodeModules = Path.of(requiredProperty("juneau.jsTests.dir")).resolve("node_modules");
		var out = new ArrayList<String>();
		out.add(Files.readString(nodeModules.resolve("jquery/dist/jquery.min.js")));
		out.add(Files.readString(nodeModules.resolve("datatables.net/js/dataTables.min.js")));
		// The whole views pack, in the order ToolkitPackRegistry registers it (ToolkitPackRegistry_Test pins that order).
		for (var r : List.of(ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.ICONS_JS_RESOURCE, ViewsMixin.SEARCH_JS_RESOURCE,
				ViewsMixin.PAGESTATE_JS_RESOURCE, ViewsMixin.URLSTATE_JS_RESOURCE, ViewsMixin.RIBBON_JS_RESOURCE,
				ViewsMixin.DATATABLES_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.CONFIG_JS_RESOURCE,
				ViewsMixin.REGIONS_JS_RESOURCE, ViewsMixin.CONSOLE_OUTPUT_JS_RESOURCE, ViewsMixin.RUN_VIEW_JS_RESOURCE,
				ViewsMixin.HELPERS_JS_RESOURCE))
			out.add(resource(ViewsMixin.class, r));
		return out;
	}

	private static String page(String c, String theme) throws IOException {
		if (! THEMES.contains(theme))
			throw new IllegalArgumentException("unknown theme: " + theme);
		var sb = new StringBuilder("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\">\n");
		sb.append("<style>\n").append(resource(ViewsMixin.class, ViewsMixin.VIEWS_CSS_RESOURCE)).append("\n</style>\n");
		sb.append("<style>\n").append(resource(ViewsMixin.class, ViewsMixin.CONFIG_CSS_RESOURCE)).append("\n</style>\n");
		sb.append("<style>\n").append(resource(ViewsMixin.class, "/org/apache/juneau/console/juneau-theme-" + theme + ".css")).append("\n</style>\n");
		sb.append("</head><body>\n").append(body(c)).append('\n');
		for (var js : scripts())
			sb.append("<script>\n").append(js).append("\n</script>\n");
		sb.append("<script>JuneauViews.regions.enrolIn(document.body);</script>\n</body></html>");
		return sb.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Server
	//-----------------------------------------------------------------------------------------------------------------

	@BeforeAll
	static void setUp() throws Exception {
		mock = MockRestClient.buildLax(R.class);
		setUpRuns();
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.setExecutor(Executors.newCachedThreadPool());
		port = server.getAddress().getPort();

		server.createContext(RunViewMixin.RUN_VIEW_PREFIX + "/", RunView_BrowserTest::bridge);
		server.createContext("/ctl/", RunView_BrowserTest::control);
		// The inlined icons script has no src, so it fetches its sprite relative to the page URL.
		server.createContext("/page/juneau-symbols.svg", ex -> {
			try {
				writeBody(ex, 200, "image/svg+xml", resource(ViewsMixin.class, "/org/apache/juneau/views/juneau-symbols.svg"));
			} catch (Exception e) {
				sendFailure(ex, e);
			}
		});
		server.createContext("/page/", ex -> {
			try {
				var c = ex.getRequestURI().getPath().substring("/page/".length());
				var theme = Optional.ofNullable(queryParam(ex.getRequestURI().getRawQuery(), "theme")).orElse("gray");
				writeBody(ex, 200, "text/html;charset=utf-8", page(c, theme));
			} catch (Exception e) {
				sendFailure(ex, e);
			}
		});
		server.start();
		report = runHarness();
	}

	@AfterAll
	static void tearDown() {
		if (server != null)
			server.stop(0);
	}

	/** Forwards /juneau-run-view/* to the mixin. */
	private static void bridge(HttpExchange ex) throws IOException {
		try {
			var uri = ex.getRequestURI();
			var path = uri.getRawPath();
			var target = uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
			int status;
			String type;
			String body;
			synchronized (LOGS) {
				var res = mock.get(target).run();
				status = res.getStatusCode();
				type = res.getStringHeader("Content-Type").orElse("application/octet-stream");
				body = res.getContent().asString();
			}
			writeBody(ex, status, type, body);
		} catch (Exception e) {
			sendFailure(ex, e);
		}
	}

	/** The prober's control API: GET /ctl/<runId>/<action>?... */
	private static void control(HttpExchange ex) throws IOException {
		try {
			var parts = ex.getRequestURI().getPath().split("/");
			var id = parts[2];
			var action = parts[3];
			var q = ex.getRequestURI().getRawQuery();
			var log = LOGS.get(id);
			switch (action) {
				case "script" -> {
					log.append(RunEvent.suite("build", "surefire", "FooTest", 1, 1, 0));
					log.append(RunEvent.test("build", "surefire", "FooTest", "a", TestStatus.PASS));
					log.append(RunEvent.test("build", "surefire", "FooTest", "b", TestStatus.FAIL).withMsg("expected 1\nbut was 2"));
					log.append(RunEvent.end("build", EndStatus.FAIL));
					log.append(RunEvent.done(DoneStatus.FAIL));
				}
				case "step" -> log.append(RunEvent.step(queryParam(q, "id"), queryParam(q, "title")));
				case "reset" -> log.reset();
				default -> throw new IllegalArgumentException("unknown action: " + action);
			}
			writeBody(ex, 200, "text/plain", "ok");
		} catch (Exception e) {
			sendFailure(ex, e);
		}
	}

	private static String queryParam(String query, String name) {
		if (query == null)
			return null;
		for (var pair : query.split("&")) {
			var i = pair.indexOf('=');
			if (i >= 0 && URLDecoder.decode(pair.substring(0, i), UTF_8).equals(name))
				return URLDecoder.decode(pair.substring(i + 1), UTF_8);
		}
		return null;
	}

	private static void writeBody(HttpExchange ex, int status, String type, String body) throws IOException {
		var bytes = body.getBytes(UTF_8);
		ex.getResponseHeaders().add("Content-Type", type);
		ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
		if (bytes.length > 0) {
			try (var os = ex.getResponseBody()) {
				os.write(bytes);
			}
		} else {
			ex.close();
		}
	}

	/** A 500 with the stack trace instead of a dropped connection, so the prober's diagnostics show the cause. */
	private static void sendFailure(HttpExchange ex, Exception e) throws IOException {
		var sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		System.err.println(sw);
		writeBody(ex, 500, "text/plain;charset=utf-8", sw.toString());
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static Map<?,?> runHarness() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("run-view-browser.cjs");
		var baseUrl = "http://127.0.0.1:" + port + "/";
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), baseUrl);
		var stdout = dir.resolve("run-view-browser-stdout.json");
		var stderr = dir.resolve("run-view-browser-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));
		var p = pb.start();
		if (! p.waitFor(5, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 5m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Json.to(Files.readString(stdout), Map.class);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Report access
	//-----------------------------------------------------------------------------------------------------------------

	private static Map<?,?> c(String name) {
		var cases = (Map<?,?>) report.get("cases");
		var m = (Map<?,?>) cases.get(name);
		assertNotNull(m, () -> "no report for " + name + ": " + report);
		assertNull(m.get("harnessError"), () -> name + " harness error: " + m.get("harnessError") + "\n" + report.get("diagnostics"));
		return m;
	}

	private static void noPageErrors(String name) {
		assertEquals(List.of(), ((Map<?,?>) report.get("pageErrors")).get(name), () -> name + " page errors");
	}

	private static void clean(String name) {
		noPageErrors(name);
		assertEquals(List.of(), ((Map<?,?>) report.get("consoleErrors")).get(name), () -> name + " console errors");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_scriptedRunEndsFailedAtTheStep() {
		clean("a01");
		assertBean(c("a01"), "running,final,status,failures", "Running,Failed at Build,fail,1");
	}

	@Test void a02_tooltipShowsAndEscapeHides() {
		clean("a02");
		var m = c("a02");
		assertBean(m, "role,hiddenAfterEscape", "tooltip,true");
		assertContains(() -> m.toString(), "b", (String) m.get("tipText"));
	}

	@Test void a03_blockLinkSetsTheHash() {
		clean("a03");
		assertBean(c("a03"), "hash", "#raw-L12");
	}

	@Test void a04_compactAndFullShowTheSameRun() {
		clean("a04");
		var m = c("a04");
		assertBean(m, "bothMounted,sameHeadline", "true,true");
		assertTrue(((Number) m.get("fullFailures")).intValue() >= 1, () -> "full failures: " + m);
	}

	@Test void a05_pushModeMakesNoEventsRequests() {
		clean("a05");
		assertBean(c("a05"), "headline,eventsRequests", "Failed at Pushed,0");
	}

	@Test void a06_resetReloadsWithoutABanner() {
		noPageErrors("a06");
		// The browser logs the expected 410 itself; it is the only console error allowed.
		assertEquals(List.of("Failed to load resource: the server responded with a status of 410 (Gone)"),
			((Map<?,?>) report.get("consoleErrors")).get("a06"), "a06 console errors");
		assertBean(c("a06"), "hasOldStep,banner", "false,0");
	}

	@Test void a07_contrastOnEveryTheme() {
		var all = (Map<?,?>) c("a07").get("themes");
		noPageErrors("a07");
		var failures = new ArrayList<String>();
		for (var theme : THEMES) {
			var ratios = (Map<?,?>) all.get(theme);
			assertNotNull(ratios, () -> "no ratios for " + theme);
			for (var e : ratios.entrySet())
				if (((Number) e.getValue()).doubleValue() < 4.5)
					failures.add(theme + "/" + e.getKey() + " " + e.getValue() + ":1 is below 4.5:1");
		}
		assertEquals(List.of(), failures, () -> "contrast: " + all);
	}

	@Test void a08_twelveThousandTestsStayBounded() {
		clean("a08");
		var m = c("a08");
		assertContains(() -> m.toString(), "12000", (String) m.get("counts"));
		assertTrue(((Number) m.get("blocks")).intValue() < 1500, () -> "blocks in the DOM: " + m);
	}
}
