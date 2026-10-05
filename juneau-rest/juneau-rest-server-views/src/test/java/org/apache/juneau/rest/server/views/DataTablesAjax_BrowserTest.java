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
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.http.BeanQueryRequest;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import com.sun.net.httpserver.*;

/**
 * The real-browser, real-server canary for the DataTables wire contract (design §9): the DataTables library,
 * {@code JuneauDataTables.ajax}, and {@link DataTablesQuery} agreeing on one JSON shape end to end.
 *
 * <p>
 * Unlike the module's other browser canaries (which stub {@code fetch} against a static fixture), this one starts a
 * real JDK {@link HttpServer} on the loopback interface serving the fixture page at {@code /} and a real
 * {@code POST /query} handler backed by {@link DataTablesQuery#run} over a 50-row in-memory context (same origin, no
 * CORS, no MockRest), then drives it with headless Chromium via {@code datatables-ajax.cjs}.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does: {@code mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test}.  That profile also
 * installs the {@code jquery} and {@code datatables.net} npm packages the fixture page inlines.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ModalResult_BrowserTest} &mdash; the sibling stubbed-fetch canary.
 * </ul>
 */
@EnabledIfSystemProperty(named=DataTablesAjax_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class DataTablesAjax_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	/** A fixture row; a public static class with getters so the bean-query context infers {@code id} and {@code name}. */
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

	private static List<Row> rows() {
		var l = new ArrayList<Row>();
		for (var i = 1; i <= 50; i++)
			l.add(new Row(i, String.format("Row %02d", i)));
		return l;
	}

	private final List<String> capturedTestHeader = new CopyOnWriteArrayList<>();
	private final List<String> capturedContentType = new CopyOnWriteArrayList<>();

	private HttpServer server;
	private int port;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		port = server.getAddress().getPort();
		var context = InMemoryBeanQueryContext.create(Row.class).columns("id", "name").build();
		var data = rows();

		server.createContext("/query", ex -> {
			try {
				var header = ex.getRequestHeaders().getFirst("X-Test-Header");
				if (header != null)
					capturedTestHeader.add(header);
				capturedContentType.add(String.valueOf(ex.getRequestHeaders().getFirst("Content-Type")));
				String body;
				try (var in = ex.getRequestBody()) {
					body = new String(in.readAllBytes(), UTF_8);
				}
				String json;
				try (var s = context.getSession(data)) {
					json = Json.of(DataTablesQuery.run(Json.to(body, DataTablesRequest.class), s));
				}
				var bytes = json.getBytes(UTF_8);
				ex.getResponseHeaders().add("Content-Type", "application/json");
				ex.sendResponseHeaders(200, bytes.length);
				try (var os = ex.getResponseBody()) {
					os.write(bytes);
				}
			} catch (BeanQuerySyntaxException e) {
				sendBadRequest(ex, e);
			} catch (Exception e) {
				sendFailure(ex, e);
			}
		});
		server.createContext("/", ex -> {
			var bytes = fixturePage().getBytes(UTF_8);
			ex.getResponseHeaders().add("Content-Type", "text/html;charset=utf-8");
			ex.sendResponseHeaders(200, bytes.length);
			try (var os = ex.getResponseBody()) {
				os.write(bytes);
			}
		});
		server.start();
	}

	/**
	 * Answers a query syntax error the way {@code RestContext.convertThrowable} does: a 400 carrying the error code in
	 * the {@code X-BeanQuery-Error} header and the message as the body.
	 */
	private static void sendBadRequest(HttpExchange ex, BeanQuerySyntaxException e) throws IOException {
		var bytes = e.getMessage().getBytes(UTF_8);
		ex.getResponseHeaders().add(BeanQueryRequest.ERROR_HEADER, e.code().name());
		ex.getResponseHeaders().add("Content-Type", "text/plain;charset=utf-8");
		ex.sendResponseHeaders(400, bytes.length);
		try (var os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}

	/**
	 * Answers a handler failure with a 500 carrying the stack trace, instead of throwing (which makes the JDK server
	 * drop the connection with no response) - so the prober's network diagnostics show why a draw failed.
	 */
	private static void sendFailure(HttpExchange ex, Exception e) throws IOException {
		var sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		System.err.println(sw);
		var bytes = sw.toString().getBytes(UTF_8);
		ex.getResponseHeaders().add("Content-Type", "text/plain;charset=utf-8");
		ex.sendResponseHeaders(500, bytes.length);
		try (var os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private static String fixturePage() throws IOException {
		var nodeModules = Path.of(requiredProperty("juneau.jsTests.dir")).resolve("node_modules");
		var jquery = Files.readString(nodeModules.resolve("jquery/dist/jquery.min.js"));
		var dataTablesJs = Files.readString(nodeModules.resolve("datatables.net/js/dataTables.min.js"));
		String glue;
		try (var in = DataTablesMixin.class.getResourceAsStream("juneau-datatables.js")) {
			assertNotNull(in, "missing classpath resource: juneau-datatables.js");
			glue = new String(in.readAllBytes(), UTF_8);
		}

		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n"
			+ "<table id=\"t1\"></table>\n"
			+ "<table id=\"t2\" data-juneau-datatable='"
			+ "{&quot;columns&quot;:[{&quot;data&quot;:&quot;id&quot;,&quot;title&quot;:&quot;ID&quot;},"
			+ "{&quot;data&quot;:&quot;name&quot;,&quot;title&quot;:&quot;Name&quot;}]}' "
			+ "data-juneau-datatable-ajax=\"/query\"></table>\n"
			+ "<table id=\"t3\"></table>\n"
			+ "<script>\n" + jquery + "\n</script>\n"
			+ "<script>\n" + dataTablesJs + "\n</script>\n"
			+ "<script>\n" + glue + "\n</script>\n"
			+ "<script>\n"
			+ "new DataTable('#t1', {serverSide:true, order:[[1,'asc']],"
			+ " ajax: JuneauDataTables.ajax('/query'),"
			+ " columns:[{data:'id',title:'ID'},{data:'name',title:'Name'}]});\n"
			+ "window.JuneauDataTables_t3 = { table: new DataTable('#t3', {serverSide:true,"
			+ " ajax: JuneauDataTables.ajax('/query', {headers:{'X-Test-Header':'present'}}),"
			+ " columns:[{data:'nope',title:'Nope'},{data:'name',title:'Name'}]}) };\n"
			+ "</script>\n"
			+ "</body></html>";
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private Map<?,?> runHarness() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("datatables-ajax.cjs");
		var baseUrl = "http://127.0.0.1:" + port + "/";

		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), baseUrl);
		var stdout = dir.resolve("datatables-ajax-stdout.json");
		var stderr = dir.resolve("datatables-ajax-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
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

	@SuppressWarnings({
		"unchecked" // sub() casts a nested report value to Map<String,Object>; the browser report JSON is known to nest objects there
	})
	private static Map<String,Object> sub(Map<?,?> report, String key) {
		return (Map<String,Object>) report.get(key);
	}

	@Test void a01_step1LoadsFirstPageViaRealAjax() throws Exception {
		var report = runHarness();
		assertEmpty(report::toString, report.get("jsFailures"));
		// Every probe step polls for its outcome; a step that never saw it is named here.
		assertEmpty(report::toString, report.get("timeouts"));
		assertBean(report::toString, report, "t1Loaded{firstRow}", "{Row 01}");
		assertContains(report::toString, "of 50 entries", sub(report, "t1Loaded").get("info"));
	}

	@Test void a02_step2AutoInitDataAttributeDrawsSameRows() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "t2Loaded{firstRow}", "{Row 01}");
	}

	@Test void a03_step3GlobalSearchNarrowsRecordsFiltered() throws Exception {
		var report = runHarness();
		// "Row 2" is a substring of exactly "Row 20" through "Row 29" (not "Row 02") = 10 rows filtered from 50.
		// Both parts are required: the unfiltered page-1 info ("Showing 1 to 10 of 50 entries") also mentions 10 and 50.
		assertContainsAll(report::toString, sub(report, "t1Search").get("info"), "of 10 entries", "filtered from 50");
	}

	@Test void a04_step4SortReversesFirstRow() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "t1Sort{before,after}", "{Row 01,Row 50}");
	}

	@Test void a05_step5NextPageShowsRows11Through20() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "t1Page2{firstRow}", "{Row 11}");
	}

	@Test void a06_step6BadColumnShowsUnknownColumnDialog() throws Exception {
		var report = runHarness();
		var dialogs = (List<?>)report.get("dialogs");
		assertTrue(dialogs.stream().anyMatch(d -> String.valueOf(d).contains("Unknown column 'nope'.")),
			() -> "no dialog carried the server's error message: " + report);
	}

	@Test void a07_step7ExtraHeadersReachTheServer() throws Exception {
		runHarness();
		assertContains(() -> "X-Test-Header from ajax(url, {headers:...}) never reached the server: " + capturedTestHeader,
			"present", capturedTestHeader);
	}

	@Test void a08_everyDrawIsAJsonPost() throws Exception {
		runHarness();
		assertTrue(!capturedContentType.isEmpty() && capturedContentType.stream().allMatch(x -> x.startsWith("application/json")),
			() -> "a draw reached /query without a JSON Content-Type: " + capturedContentType);
	}
}
