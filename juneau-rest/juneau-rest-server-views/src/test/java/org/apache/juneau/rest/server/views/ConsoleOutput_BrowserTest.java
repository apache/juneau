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

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.imageio.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import com.sun.net.httpserver.*;

/**
 * The real-browser canary for the console-output region: the real {@code juneau-console-output.js} populator,
 * mounted through the real region runtime, polling the real {@link ConsoleOutputMixin} endpoints.
 *
 * <p>
 * A JDK {@link HttpServer} serves the fixture pages, a control API the prober drives ({@code /ctl/<logId>/<action>}),
 * and {@code /juneau-console-output/*}, which it forwards to a {@link MockRestClient} over a resource implementing
 * {@link ConsoleOutputMixin}.  The forwarder counts lines requests per log, which is how a case proves that polling
 * stopped.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does: {@code mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test}.
 *
 * @since 10.0.0
 */
@EnabledIfSystemProperty(named=ConsoleOutput_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleOutput_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	/** The five console-ui themes (a09). */
	static final List<String> THEMES = List.of("gray", "light-brown", "light-red", "open", "red");

	/** The a09 samples; each is a line whose text contains {@code sample:<key>}. */
	static final List<String> SAMPLES = List.of(
		"level-severe", "level-warning", "level-info", "level-fine",
		"style-success", "style-warn", "style-error", "style-muted", "style-accent",
		"marker-dim", "ansi-35", "ansi-95", "ansi-36", "ansi-96");

	/** The GONE text the client shows for a forward-poll 410 (Task 14). */
	static final String GONE_TEXT = "the log was replaced or truncated (410 Gone); reload to view it from the start";

	@Rest
	public static class R extends BasicRestServlet implements ConsoleOutputMixin {
		private static final long serialVersionUID = 1L;
		static final Map<String,ConsoleOutputSource> SOURCES = new ConcurrentHashMap<>();

		@Override
		public Optional<ConsoleOutputSource> consoleOutputSource(String logId, RestRequest req) {
			return Optional.ofNullable(SOURCES.get(logId));
		}
	}

	/** An a06 root-table row; public with getters so the bean-query context infers its columns. */
	public static class Row {
		private final int id;
		private final String name;

		Row(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() { return id; }
		public String getName() { return name; }
	}

	private static final Map<String,ConsoleOutputLog> LOGS = new ConcurrentHashMap<>();
	private static final Map<String,AtomicInteger> LINE_REQUESTS = new ConcurrentHashMap<>();
	private static final Object MOCK_LOCK = new Object();

	private static MockRestClient mock;
	private static HttpServer server;
	private static int port;
	private static Path tempDir;
	private static Path file;
	private static byte[] slowPng;
	private static Map<?,?> report;

	//-----------------------------------------------------------------------------------------------------------------
	// Fixture logs
	//-----------------------------------------------------------------------------------------------------------------

	private static ConsoleOutputLog log(String id, int lines, boolean started) {
		var log = new ConsoleOutputLog();
		if (started)
			log.start();
		for (var i = 1; i <= lines; i++)
			log.append(ConsoleOutputLine.info(id + " line " + i));
		LOGS.put(id, log);
		R.SOURCES.put(id, log);
		return log;
	}

	private static void setUpLogs() throws IOException {
		log("a01", 30, true);
		log("a02", 40, true);
		log("a03", 5, true);
		log("a04", 5, true);

		var a05 = log("a05", 0, true);
		a05.append(ConsoleOutputLine.frags(
			Frag.text("steps "),
			Frag.block().style(Style.SUCCESS).tooltip("first\nsecond"),
			Frag.block().style(Style.ERROR).href("#co-a05-L2").label("go to line 2")));
		a05.append(ConsoleOutputLine.info("a05 line 2"));

		log("1", 5, true);
		log("2", 5, true);

		var a07 = log("a07", 0, true);
		for (var i = 1; i <= 10; i++)
			a07.append(i == 3 || i == 7 ? ConsoleOutputLine.info("##run step " + i).marker(true) : ConsoleOutputLine.info("a07 line " + i));

		log("a08", 60, true);

		var a09 = log("a09", 0, true);
		a09.append(ConsoleOutputLine.severe("sample:level-severe"));
		a09.append(ConsoleOutputLine.warning("sample:level-warning"));
		a09.append(ConsoleOutputLine.info("sample:level-info"));
		a09.append(ConsoleOutputLine.fine("sample:level-fine"));
		for (var s : Style.values())
			a09.append(ConsoleOutputLine.info("sample:style-" + s.wire()).style(s));
		a09.append(ConsoleOutputLine.info("sample:marker-dim").marker(true));
		for (var code : List.of("35", "95", "36", "96"))
			a09.appendAnsi(Level.INFO, "\u001b[" + code + "msample:ansi-" + code + "\u001b[0m");
		a09.complete("SUCCEEDED", Style.SUCCESS);

		log("a12", 12000, true).complete("SUCCEEDED", Style.SUCCESS);
		log("a13", 12000, true).complete("SUCCEEDED", Style.SUCCESS);

		var a14 = log("a14", 0, true);
		a14.appendAnsi(Level.INFO, "\u001b[31mred\u001b[0m \u001b[1;32mgreen\u001b[0m \u001b]8;;https://evil.example/\u0007link\u001b]8;;\u0007 tail");

		tempDir = Files.createTempDirectory("console-output-browser");
		file = tempDir.resolve("a15.log");
		var sb = new StringBuilder();
		for (var i = 1; i <= 10; i++)
			sb.append("file line ").append(i).append('\n');
		Files.writeString(file, sb.toString(), UTF_8);
		R.SOURCES.put("a15", FileConsoleOutputSource.create(file).status(() -> FileConsoleOutputSource.Status.RUNNING).build());

		log("a16", 30, true);
		log("a17", 0, false);
		log("a18", 10, true);
		log("a19", 2, true);
		log("a20", 0, true);
	}

	private static byte[] png() throws IOException {
		var img = new BufferedImage(200, 300, BufferedImage.TYPE_INT_RGB);
		var bos = new ByteArrayOutputStream();
		ImageIO.write(img, "png", bos);
		return bos.toByteArray();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fixture pages
	//-----------------------------------------------------------------------------------------------------------------

	/** One console, placed the card-body way: {@link RegionTable#of(RegionDef)} stamps the declared descriptor. */
	private static String region(ConsoleOutputDef def) {
		return RegionTable.of(def.validate().toRegion()).toString();
	}

	private static ConsoleOutputDef def(String id) {
		return ConsoleOutputDef.forMixin(id, "", id).refreshMs(1000);
	}

	private static String body(String c) {
		return switch (c) {
			case "a04", "a13" -> region(def(c).anchorPrefix("L"));
			case "a10" -> region(ConsoleOutputDef.create("a10").linesUrl("/bad/lines").refreshMs(1000));
			case "a06" -> a06Body();
			default -> region(def(c).title("Console " + c));
		};
	}

	private static String a06Body() {
		var detail = ConsoleOutputDef.forMixin("output", "", "{id}").type("row-detail").compact(true).refreshMs(1000);
		var rootMeta = "{\"contractVersion\":\"5\",\"id\":\"runs\",\"dataMode\":\"server\",\"dataUrl\":\"/query\","
			+ "\"columns\":[{\"data\":\"id\",\"title\":\"ID\"},{\"data\":\"name\",\"title\":\"Name\"}]}";
		return "<table data-juneau-view=\"runs\" id=\"runs\"></table>\n"
			+ "<script type=\"application/json\" id=\"juneau-view:runs\">" + rootMeta + "</script>\n"
			+ "<template data-juneau-row-detail=\"1\" data-juneau-detail-url=\"/query/{id}\">\n"
			+ region(detail) + "\n</template>\n";
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
				ViewsMixin.REGIONS_JS_RESOURCE, ViewsMixin.CONSOLE_OUTPUT_JS_RESOURCE, ViewsMixin.HELPERS_JS_RESOURCE))
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
		// Idempotent per node (juneau-regions.js:1347), so a second enrolment walk by the views runtime is harmless.
		sb.append("<script>JuneauViews.regions.enrolIn(document.body);</script>\n</body></html>");
		return sb.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Server
	//-----------------------------------------------------------------------------------------------------------------

	@BeforeAll
	static void setUp() throws Exception {
		System.setProperty("java.awt.headless", "true");
		slowPng = png();
		mock = MockRestClient.buildLax(R.class);
		setUpLogs();
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.setExecutor(Executors.newCachedThreadPool());
		port = server.getAddress().getPort();

		server.createContext(ConsoleOutputMixin.CONSOLE_OUTPUT_PREFIX + "/", ConsoleOutput_BrowserTest::bridge);
		server.createContext("/ctl/", ConsoleOutput_BrowserTest::control);
		server.createContext("/bad/lines", ex -> writeBody(ex, 200, "application/json", "{\"contractVersion\":\"2\",\"lines\":[]}"));
		server.createContext("/img/slow.png", ex -> {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			ex.getResponseHeaders().add("Content-Type", "image/png");
			ex.sendResponseHeaders(200, slowPng.length);
			try (var os = ex.getResponseBody()) {
				os.write(slowPng);
			}
		});
		var rootContext = InMemoryBeanQueryContext.create(Row.class).columns("id", "name").build();
		server.createContext("/query", ex -> {
			try {
				String body;
				try (var in = ex.getRequestBody()) {
					body = new String(in.readAllBytes(), UTF_8);
				}
				String json;
				try (var s = rootContext.getSession(List.of(new Row(1, "Run 1"), new Row(2, "Run 2")))) {
					json = Json.of(DataTablesQuery.run(Json.to(body, DataTablesRequest.class), s));
				}
				writeBody(ex, 200, "application/json", json);
			} catch (Exception e) {
				sendFailure(ex, e);
			}
		});
		// Longest-prefix match: the row-detail GET envelope at /query/{id} (an empty-fields 200 is all
		// expandDetailRow needs before it enrols the panel's regions).
		server.createContext("/query/", ex -> writeBody(ex, 200, "application/json", "{\"contractVersion\":\"1\",\"fields\":{}}"));
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
	static void tearDown() throws IOException {
		if (server != null)
			server.stop(0);
		if (tempDir != null) {
			try (var s = Files.walk(tempDir)) {
				for (var p : s.sorted(Comparator.reverseOrder()).toList())
					Files.deleteIfExists(p);
			}
		}
	}

	/** Forwards /juneau-console-output/* to the mixin, counting lines requests per log. */
	private static void bridge(HttpExchange ex) throws IOException {
		try {
			var uri = ex.getRequestURI();
			var path = uri.getRawPath();
			var parts = path.split("/");
			if (parts.length == 4 && "lines".equals(parts[3]))
				LINE_REQUESTS.computeIfAbsent(parts[2], k -> new AtomicInteger()).incrementAndGet();
			var target = uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
			int status;
			String type;
			String body;
			synchronized (MOCK_LOCK) {
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

	/** The prober's control API: GET /ctl/<logId>/<action>?... */
	private static void control(HttpExchange ex) throws IOException {
		try {
			var parts = ex.getRequestURI().getPath().split("/");
			var id = parts[2];
			var action = parts[3];
			var q = ex.getRequestURI().getRawQuery();
			var log = LOGS.get(id);
			switch (action) {
				case "append" -> {
					var n = Integer.parseInt(Optional.ofNullable(queryParam(q, "n")).orElse("1"));
					for (var i = 0; i < n; i++)
						log.append(ConsoleOutputLine.info(id + " more " + (i + 1)));
				}
				case "image" -> log.append(ConsoleOutputLine.info("chart").image("/img/slow.png", "slow chart"));
				case "start" -> log.start();
				case "complete" -> log.complete("SUCCEEDED", Style.SUCCESS);
				case "fileappend" -> {
					var n = Integer.parseInt(Optional.ofNullable(queryParam(q, "n")).orElse("1"));
					var sb = new StringBuilder();
					for (var i = 0; i < n; i++)
						sb.append("file more ").append(i + 1).append('\n');
					Files.writeString(file, sb.toString(), UTF_8, StandardOpenOption.APPEND);
				}
				case "truncate" -> Files.writeString(file, "a different log\n", UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
				case "open" -> log.openLine(ConsoleOutputLine.info(Optional.ofNullable(queryParam(q, "text")).orElse(id + " filling")));
				case "dot" -> log.dot();
				case "settail" -> log.setTail(queryParam(q, "text"));
				case "closeline" -> log.closeLine(Optional.ofNullable(queryParam(q, "text")).orElse(" done"));
				case "count" -> {
					var m = new TreeMap<String,Integer>();
					LINE_REQUESTS.forEach((k, v) -> m.put(k, v.get()));
					writeBody(ex, 200, "application/json", Json.of(m));
					return;
				}
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
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("console-output-browser.cjs");
		var baseUrl = "http://127.0.0.1:" + port + "/";
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), baseUrl);
		var stdout = dir.resolve("console-output-browser-stdout.json");
		var stderr = dir.resolve("console-output-browser-stderr.txt");
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

	private static double num(Map<?,?> m, String key) {
		var v = m.get(key);
		assertNotNull(v, () -> "missing " + key + " in " + m);
		return ((Number) v).doubleValue();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_tailOrderStuckAndTicking() {
		clean("a01");
		assertBean(c("a01"), "first,last,count,ascending,stuck,ticking", "1,30,30,true,true,true");
	}

	@Test void a02_newLinesWhileScrolledUp_jumpThenReStick() {
		clean("a02");
		assertBean(c("a02"), "unstuck,jumpText,afterJumpAtBottom,afterJumpHidden,reStuck",
			"true,Jump to latest (5 new),true,true,true");
	}

	@Test void a03_terminalShowsDurationAndStopsPolling() {
		clean("a03");
		var m = c("a03");
		assertBean(m, "state,durationFormat", "SUCCEEDED,true");
		assertEquals(0d, num(m, "requestsAfterTerminal"), () -> "lines requests after the terminal page: " + m);
	}

	@Test void a04_hashTargetBeforeTheLineExists() {
		clean("a04");
		assertBean(c("a04"), "found,target,visible", "true,true,true");
	}

	@Test void a05_keyboardTooltip() {
		clean("a05");
		assertBean(c("a05"), "firstIsBlock,tipShown,tipText,describedBy,tipHiddenAfterEscape,describedByAfterEscape,secondIsLink,secondHref",
			"true,true,first\nsecond,true,true,false,true,#co-a05-L2");
	}

	@Test void a06_compactRowDetail_teardownStopsPolling() {
		clean("a06");
		var m = c("a06");
		assertBean(m, "compact,firstText", "true,1 line 1");
		assertTrue(num(m, "requestsWhileOpen") >= 2, () -> "polling while open: " + m);
		assertEquals(0d, num(m, "requestsAfterCollapse"), () -> "lines requests after collapse: " + m);
	}

	@Test void a07_markersHideAndPersistAcrossReload() {
		clean("a07");
		assertBean(c("a07"), "dimAtStart,toggleVisible,hidden,markerDisplay,markerInDom,hiddenAfterReload",
			"true,true,true,none,true,true");
	}

	@Test void a08_twentyRowsAndResizable() {
		clean("a08");
		var m = c("a08");
		assertEquals(20d, num(m, "rows"), 0.6, () -> "pane rows: " + m);
		assertTrue(num(m, "grew") >= 60, () -> "drag-resize grew the pane: " + m);
	}

	@Test void a09_contrastOnEveryTheme() {
		var all = (Map<?,?>) c("a09").get("themes");
		var failures = new ArrayList<String>();
		for (var theme : THEMES) {
			noPageErrors("a09-" + theme);
			var ratios = (Map<?,?>) all.get(theme);
			assertNotNull(ratios, () -> "no ratios for " + theme);
			for (var key : SAMPLES) {
				var v = (Number) ratios.get(key);
				if (v == null) {
					failures.add(theme + "/" + key + " missing");
					continue;
				}
				if (v.doubleValue() < 4.5)
					failures.add(theme + "/" + key + " " + v + ":1 is below 4.5:1");
			}
		}
		assertEquals(List.of(), failures, () -> "contrast: " + all);
	}

	@Test void a10_contractMismatchBanner() {
		noPageErrors("a10");
		var m = c("a10");
		assertBean(m, "code,role,regionState", "E-CO-2,alert,error");
		assertContains(() -> m.toString(), "contractVersion '2'", (String) m.get("text"));
	}

	@Test void a12_tailThenLoadEarlierKeepsPosition() {
		clean("a12");
		var m = c("a12");
		assertList((List<?>) m.get("firsts"), "7001", "5001", "3001", "1001", "1");
		assertBean(m, "initialCount,controlGone", "5000,true");
		assertTrue(num(m, "maxShift") <= 2, () -> "the anchored row moved: " + m);
	}

	@Test void a13_hashBeyondTheTailAutoLoadsEarlier() {
		clean("a13");
		var m = c("a13");
		assertBean(m, "found,target", "true,true");
		assertTrue(num(m, "first") <= 100, () -> "first loaded line: " + m);
	}

	@Test void a14_ansiStylesWithoutEscapesOrLinks() {
		clean("a14");
		assertBean(c("a14"), "text,hasEsc,hasReplacement,links,redIsError,greenIsSuccessBold",
			"red green link tail,false,false,0,true,true");
	}

	@Test void a15_fileSourceTailsThenGoneOnTruncate() {
		noPageErrors("a15");
		var m = c("a15");
		assertBean(m, "initial,afterAppend,code", "10,13,E-CO-4");
		assertContains(() -> m.toString(), GONE_TEXT, (String) m.get("text"));
	}

	@Test void a16_slowImageKeepsThePanePinned() {
		clean("a16");
		var m = c("a16");
		assertBean(m, "loaded,atBottom", "true,true");
		assertTrue(num(m, "imgHeight") > 100, () -> "image laid out: " + m);
	}

	@Test void a17_pendingThenRunningWithZeroLines() {
		clean("a17");
		assertBean(c("a17"), "pending,running,lines,elapsedFormat", "true,true,0,true");
	}

	@Test void a18_selectionSurvivesAnAppend() {
		clean("a18");
		var m = c("a18");
		assertBean(m, "before,after,grew", "a18 line 3,a18 line 3,true");
	}

	@Test void a19_dotsGrowOnOneRowThenClose() {
		clean("a19");
		assertBean(c("a19"), "openClass,sameNode,rows,dupes", "true,true,3,1");
	}

	@Test void a20_counterRewritesItsTailOnOneRow() {
		clean("a20");
		assertBean(c("a20"), "text,rows", "Performing task x: 2 of 2 complete,1");
	}
}
