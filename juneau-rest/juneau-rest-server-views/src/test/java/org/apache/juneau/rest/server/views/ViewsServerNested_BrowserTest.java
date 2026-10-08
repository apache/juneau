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
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import com.sun.net.httpserver.*;

/**
 * The real-browser, real-server canary for design §3.1/D8: the REAL {@code juneau-views.js} runtime booting a
 * server-mode root table (the pre-built VIEW_META/sidecar shape of a
 * {@code <@card type="datatables">} card) with a nested table in its row detail, against a JDK {@link HttpServer}
 * backed by {@link DataTablesQuery#run}.
 *
 * <p>
 * {@link ViewsJs_NestedTable_Test} proves {@code buildOptions}/{@code applyNestedScope} build the right objects against
 * DOM-shim mocks; only a real browser proves mutating {@code settings.url} inside {@code beforeSend} actually changes
 * the URL a real jQuery/DataTables transport sends, re-derived per expand (row 1's nested table is scoped to
 * {@code parentId=1}; after it is torn down, row 2's fresh nested table is scoped to {@code parentId=2} &mdash; never
 * a frozen URL from the first expand).
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does: {@code mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link DataTablesAjax_BrowserTest} &mdash; the sibling real-server canary for the bare wire contract.
 * </ul>
 *
 * @since 10.0.0
 */
@EnabledIfSystemProperty(named=ViewsServerNested_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ViewsServerNested_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	/** A root-table row; a public static class with getters so the bean-query context infers its columns. */
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

	/** A nested-table row, scoped to its parent root row by {@code parentId}. */
	public static class Event {
		private final int parentId;
		private final String what;

		Event(int parentId, String what) {
			this.parentId = parentId;
			this.what = what;
		}

		public int getParentId() { return parentId; }
		public String getWhat() { return what; }
	}

	private static List<Row> rootRows() {
		return List.of(new Row(1, "Row 01"), new Row(2, "Row 02"));
	}

	private static List<Event> allEvents() {
		return List.of(new Event(1, "detail for 1"), new Event(2, "detail for 2"));
	}

	/** Every {@code /nested} request's raw query string, in arrival order; proves the scope param is re-derived per expand. */
	private final List<String> nestedQueries = new CopyOnWriteArrayList<>();

	private HttpServer server;
	private int port;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		port = server.getAddress().getPort();
		var rootContext = InMemoryBeanQueryContext.create(Row.class).columns("id", "name").build();
		var eventContext = InMemoryBeanQueryContext.create(Event.class).columns("parentId", "what").build();

		server.createContext("/query", ex -> {
			try {
				if (neq(ex.getRequestMethod(), "POST")) {
					ex.sendResponseHeaders(405, -1);
					return;
				}
				String body;
				try (var in = ex.getRequestBody()) {
					body = new String(in.readAllBytes(), UTF_8);
				}
				String json;
				try (var s = rootContext.getSession(rootRows())) {
					json = Json.of(DataTablesQuery.run(Json.to(body, DataTablesRequest.class), s));
				}
				writeJson(ex, json);
			} catch (Exception e) {
				sendFailure(ex, e);
			}
		});
		// A distinct "/query/" context (longest-prefix match) serves the row-detail GET envelope at /query/{id};
		// an empty-fields 200 is all expandDetailRow (juneau-views.js) requires to proceed to the nested table.
		server.createContext("/query/", ex -> writeJson(ex, "{\"contractVersion\":\"1\",\"fields\":{}}"));
		server.createContext("/nested", ex -> {
			try {
				if (neq(ex.getRequestMethod(), "POST")) {
					ex.sendResponseHeaders(405, -1);
					return;
				}
				var query = ex.getRequestURI().getRawQuery();
				nestedQueries.add(String.valueOf(query));
				var parentId = queryParam(query, "parentId");
				var scoped = allEvents().stream().filter(e -> String.valueOf(e.getParentId()).equals(parentId)).toList();
				String body;
				try (var in = ex.getRequestBody()) {
					body = new String(in.readAllBytes(), UTF_8);
				}
				String json;
				try (var s = eventContext.getSession(scoped)) {
					json = Json.of(DataTablesQuery.run(Json.to(body, DataTablesRequest.class), s));
				}
				writeJson(ex, json);
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

	@AfterEach
	void stopServer() {
		server.stop(0);
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

	private static void writeJson(HttpExchange ex, String json) throws IOException {
		var bytes = json.getBytes(UTF_8);
		ex.getResponseHeaders().add("Content-Type", "application/json");
		ex.sendResponseHeaders(200, bytes.length);
		try (var os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static String resource(Class<?> anchor, String path) throws IOException {
		try (var in = anchor.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path + " (relative to " + anchor.getName() + ")");
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String fixturePage() throws IOException {
		var nodeModules = Path.of(requiredProperty("juneau.jsTests.dir")).resolve("node_modules");
		var jquery = Files.readString(nodeModules.resolve("jquery/dist/jquery.min.js"));
		var dataTablesJs = Files.readString(nodeModules.resolve("datatables.net/js/dataTables.min.js"));
		var datatablesGlue = resource(DataTablesMixin.class, "juneau-datatables.js");
		var viewsJs = resource(ViewsMixin.class, ViewsMixin.VIEWS_JS_RESOURCE);

		var rootMeta = "{\"contractVersion\":\"5\",\"id\":\"root\",\"dataMode\":\"server\",\"dataUrl\":\"/query\","
			+ "\"columns\":[{\"data\":\"id\",\"title\":\"ID\"},{\"data\":\"name\",\"title\":\"Name\"}]}";
		var nestedMeta = "{\"contractVersion\":\"5\",\"id\":\"events\",\"dataMode\":\"server\",\"dataUrl\":\"/nested\","
			+ "\"columns\":[{\"data\":\"what\",\"title\":\"What\"}]}";

		// The nested sidecar carries type="application/json" so the browser never executes it as script once the
		// row-detail template is cloned into the live document.
		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n"
			+ "<table data-juneau-view=\"root\" id=\"root\"></table>\n"
			+ "<script type=\"application/json\" id=\"juneau-view:root\">" + rootMeta + "</script>\n"
			+ "<template data-juneau-row-detail=\"1\" data-juneau-detail-url=\"/query/{id}\">\n"
			+ "  <div class=\"juneau-view-detail-nested\" data-juneau-nested=\"1\" data-juneau-nested-contract=\"2\""
			+ " data-juneau-nested-scope-param=\"parentId\">\n"
			+ "    <table data-juneau-view=\"events\"></table>\n"
			+ "    <script type=\"application/json\" data-juneau-nested-meta=\"events\">" + nestedMeta + "</script>\n"
			+ "  </div>\n"
			+ "</template>\n"
			+ "<script>\n" + jquery + "\n</script>\n"
			+ "<script>\n" + dataTablesJs + "\n</script>\n"
			+ "<script>\n" + datatablesGlue + "\n</script>\n"
			+ "<script>\n" + viewsJs + "\n</script>\n"
			+ "</body></html>";
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private Map<?,?> runHarness() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("views-server-nested.cjs");
		var baseUrl = "http://127.0.0.1:" + port + "/";

		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), baseUrl);
		var stdout = dir.resolve("views-server-nested-stdout.json");
		var stderr = dir.resolve("views-server-nested-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (! p.waitFor(3, TimeUnit.MINUTES)) {
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

	@Test void a01_rootServerModeTableLoadsViaRealPostJsonAjax() throws Exception {
		var report = runHarness();
		assertEmpty(report::toString, report.get("jsFailures"));
		assertBean(report::toString, report, "root{firstRow,secondRow}", "{Row 01,Row 02}");
	}

	@Test void a02_expandingRow1BootsNestedTableScopedToParentId1() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "expand1{nestedRow,nestedHasExpander}", "{detail for 1,false}");
	}

	@Test void a03_collapseThenExpandRow2BootsFreshNestedTableScopedToParentId2() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "expand2{nestedRow,nestedHasExpander}", "{detail for 2,false}");
	}

	@Test void a04_scopeParamReachedTheServerAsAUrlQueryParamReDerivedPerExpand() throws Exception {
		runHarness();
		assertTrue(nestedQueries.size() >= 2, () -> "expected at least 2 /nested requests, got: " + nestedQueries);
		assertContains(() -> "first /nested request: " + nestedQueries, "parentId=1", nestedQueries.get(0));
		assertContains(() -> "last /nested request: " + nestedQueries, "parentId=2", nestedQueries.get(nestedQueries.size() - 1));
	}
}
