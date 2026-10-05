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
 * The real-browser, real-server canary for Gap 1 end to end: a column hidden through the View Settings dialog
 * <i>stays</i> hidden after a real page reload, in both server-mode and client-mode tables.
 *
 * <p>
 * {@code ViewsJs_ConfigApplication_Test} and {@code ViewsJs_Reinit_Test} prove, against a recording fake DataTables,
 * that the persisted blob reaches the constructor options.  Only a real browser proves the whole loop: a real click
 * on the gear, a real checkbox, a real Apply, the blob landing in real {@code localStorage}, and the next page load
 * (same {@code http://} origin) rebuilding the real DataTables grid without the hidden column.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does: {@code mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewSettingsA11y_BrowserTest} &mdash; the sibling canary for the dialog's a11y behavior.
 * 	<li class='jc'>{@link ViewsServerNested_BrowserTest} &mdash; the sibling real-server canary this one borrows its
 * 		HTTP fixture from.
 * </ul>
 *
 * @since 10.0.0
 */
@EnabledIfSystemProperty(named=ViewSettingsReload_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ViewSettingsReload_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	/** A table row; a public static class with getters so the bean-query context infers its columns. */
	public static class Row {
		private final int id;
		private final String name;
		private final String status;

		Row(int id, String name, String status) {
			this.id = id;
			this.name = name;
			this.status = status;
		}

		public int getId() { return id; }
		public String getName() { return name; }
		public String getStatus() { return status; }
	}

	private static List<Row> rows() {
		return List.of(new Row(1, "Row 01", "OK"), new Row(2, "Row 02", "FAIL"));
	}

	private HttpServer server;
	private int port;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		port = server.getAddress().getPort();
		var context = InMemoryBeanQueryContext.create(Row.class).columns("id", "name", "status").build();

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
				try (var s = context.getSession(rows())) {
					json = Json.of(DataTablesQuery.run(Json.to(body, DataTablesRequest.class), s));
				}
				write(ex, "application/json", json);
			} catch (Exception e) {
				sendFailure(ex, e);
			}
		});
		server.createContext("/data", ex -> write(ex, "application/json", Json.of(rows())));
		server.createContext("/server", ex -> {
			try {
				write(ex, "text/html;charset=utf-8", fixturePage("server", "/query"));
			} catch (Exception e) {
				sendFailure(ex, e);
			}
		});
		server.createContext("/client", ex -> {
			try {
				write(ex, "text/html;charset=utf-8", fixturePage("client", "/data"));
			} catch (Exception e) {
				sendFailure(ex, e);
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

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

	private static void write(HttpExchange ex, String contentType, String body) throws IOException {
		var bytes = body.getBytes(UTF_8);
		ex.getResponseHeaders().add("Content-Type", contentType);
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

	/** Load order per the module's own doc comment: renders, views, urlstate, search, pagestate, config. */
	private static String fixturePage(String dataMode, String dataUrl) throws IOException {
		var nodeModules = Path.of(requiredProperty("juneau.jsTests.dir")).resolve("node_modules");
		var jquery = Files.readString(nodeModules.resolve("jquery/dist/jquery.min.js"));
		var dataTablesJs = Files.readString(nodeModules.resolve("datatables.net/js/dataTables.min.js"));
		var meta = "{\"contractVersion\":\"5\",\"id\":\"t1\",\"dataMode\":\"" + dataMode + "\",\"dataUrl\":\"" + dataUrl + "\","
			+ "\"columnConfig\":{},"
			+ "\"columns\":[{\"data\":\"id\",\"title\":\"ID\"},{\"data\":\"name\",\"title\":\"Name\"},"
			+ "{\"data\":\"status\",\"title\":\"Status\"}]}";
		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.class, ViewsMixin.VIEWS_CSS_RESOURCE) + "\n"
			+ resource(ViewsMixin.class, ViewsMixin.CONFIG_CSS_RESOURCE)
			+ "\n</style></head><body>\n"
			+ "<table data-juneau-view=\"t1\" id=\"t1\"></table>\n"
			+ "<script type=\"application/json\" id=\"juneau-view:t1\">" + meta + "</script>\n"
			+ "<script>\n" + jquery + "\n</script>\n"
			+ "<script>\n" + dataTablesJs + "\n</script>\n"
			+ "<script>\n" + resource(DataTablesMixin.class, "juneau-datatables.js") + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.class, ViewsMixin.RENDERS_JS_RESOURCE) + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.class, ViewsMixin.VIEWS_JS_RESOURCE) + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.class, ViewsMixin.URLSTATE_JS_RESOURCE) + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.class, ViewsMixin.SEARCH_JS_RESOURCE) + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.class, ViewsMixin.PAGESTATE_JS_RESOURCE) + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.class, ViewsMixin.CONFIG_JS_RESOURCE) + "\n</script>\n"
			+ "</body></html>";
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private Map<?,?> runHarness() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("view-settings-reload-browser.cjs");
		var baseUrl = "http://127.0.0.1:" + port + "/";

		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), baseUrl);
		var stdout = dir.resolve("view-settings-reload-stdout.json");
		var stderr = dir.resolve("view-settings-reload-stderr.txt");
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

	@Test void a01_serverMode_hiddenColumnStaysHiddenAfterReload() throws Exception {
		var report = runHarness();
		assertEmpty(report::toString, report.get("jsFailures"));
		assertBean(report::toString, report, "server{before,afterApply,afterReload}",
			"{[ID,Name,Status],[ID,Name],[ID,Name]}");
	}

	@Test void a02_clientMode_hiddenColumnStaysHiddenAfterReload() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "client{before,afterApply,afterReload}",
			"{[ID,Name,Status],[ID,Name],[ID,Name]}");
	}

	@Test void a03_reloadedRowsCarryNoStatusCell() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "server{rowBefore,rowAfterReload},client{rowBefore,rowAfterReload}",
			"{[1,Row 01,OK],[1,Row 01]},{[1,Row 01,OK],[1,Row 01]}");
	}

	@Test void a04_reopenedDialogShowsStoredColumnUnchecked_withColumnScopedAriaLabel() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "server{reopen{checked,aria}},client{reopen{checked,aria}}",
			"{{false,Show column Status}},{{false,Show column Status}}");
	}

	@Test void a05_untouchedApply_doesNotPersistEveryColumnAsTheSort() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "server{storedSort},client{storedSort}", "{[]},{[]}");
	}

	@Test void a06_staleBlob_showsResetNoticeEndToEnd_andDialogOpensOnDefaults() throws Exception {
		var report = runHarness();
		assertBean(report::toString, report, "server{stale{notice,statusChecked}},client{stale{notice,statusChecked}}",
			"{{Saved view settings were reset because the table changed.,true}},{{Saved view settings were reset because the table changed.,true}}");
	}
}
