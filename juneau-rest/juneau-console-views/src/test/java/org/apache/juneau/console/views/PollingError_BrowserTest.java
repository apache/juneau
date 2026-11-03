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
package org.apache.juneau.console.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.console.datatables.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import com.sun.net.httpserver.*;

/**
 * The status dot on a FAILING poll, against a real server and the real DataTables library.
 *
 * <p>
 * The table-polling Node harness fires {@code error.dt} by hand, so it cannot tell whether a real failed request ever
 * raises that event.  DataTables 2 does not: a failed request raises {@code xhr.dt} (with no JSON), then
 * {@code dt-error.dt} only when the request completed, and nothing else when it was aborted.  This test drives the
 * real failures (500 with a JSON body, 500 with an HTML body, a dropped connection) in both data modes and requires
 * the dot to reach {@code error}, then return to {@code idle} when the server recovers.
 *
 * <p>
 * Off by default: runs only under the {@code js-tests} profile.
 */
@EnabledIfSystemProperty(named=PollingError_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-console-views/pom.xml test`")
class PollingError_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private volatile String mode = "ok";
	private HttpServer server;
	private int port;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		port = server.getAddress().getPort();
		server.createContext("/mode", ex -> {
			var q = ex.getRequestURI().getRawQuery();
			mode = q == null ? "ok" : q.substring(q.indexOf('=') + 1);
			writeBody(ex, 200, "text/plain", "ok");
		});
		server.createContext("/query", ex -> {
			String body;
			try (var in = ex.getRequestBody()) {
				body = new String(in.readAllBytes(), UTF_8);
			}
			if (failed(ex))
				return;
			writeBody(ex, 200, "application/json", "{\"draw\":" + drawOf(body) + ",\"recordsTotal\":1,\"recordsFiltered\":1,\"data\":[{\"id\":1,\"name\":\"Row 01\"}]}");
		});
		server.createContext("/rows", ex -> {
			if (failed(ex))
				return;
			writeBody(ex, 200, "application/json", "[{\"id\":1,\"name\":\"Row 01\"}]");
		});
		server.createContext("/", ex -> {
			try {
				writeBody(ex, 200, "text/html;charset=utf-8", fixturePage(ex.getRequestURI().getPath().substring(1)));
			} catch (Exception e) {
				writeBody(ex, 500, "text/plain", String.valueOf(e));
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	/** DataTables drops a response whose {@code draw} is older than its own counter, so the reply must echo the request's. */
	private static String drawOf(String body) {
		var m = java.util.regex.Pattern.compile("\"draw\"\\s*:\\s*(\\d+)").matcher(body);
		return m.find() ? m.group(1) : "1";
	}

	private boolean failed(HttpExchange ex) throws IOException {
		switch (mode) {
			case "json500":
				writeBody(ex, 500, "application/json", "{\"message\":\"boom\"}");
				return true;
			case "html500":
				writeBody(ex, 500, "text/html", "<html><body>boom</body></html>");
				return true;
			case "abort":
				ex.close();
				return true;
			default:
				return false;
		}
	}

	private static void writeBody(HttpExchange ex, int status, String type, String body) throws IOException {
		var bytes = body.getBytes(UTF_8);
		ex.getResponseHeaders().add("Content-Type", type);
		ex.sendResponseHeaders(status, bytes.length);
		try (var os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static String resource(Class<?> anchor, String path) throws IOException {
		try (var in = anchor.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String fixturePage(String dataMode) throws IOException {
		var nodeModules = Path.of(requiredProperty("juneau.jsTests.dir")).resolve("node_modules");
		var jquery = Files.readString(nodeModules.resolve("jquery/dist/jquery.min.js"));
		var dataTablesJs = Files.readString(nodeModules.resolve("datatables.net/js/dataTables.min.js"));
		var glue = resource(DataTablesMixin.class, "juneau-datatables.js");
		var viewsJs = resource(ViewsMixin.class, ViewsMixin.VIEWS_JS_RESOURCE);
		var meta = "{\"contractVersion\":\"5\",\"id\":\"root\",\"dataMode\":\"" + dataMode + "\","
			+ "\"dataUrl\":\"" + (dataMode.equals("server") ? "/query" : "/rows") + "\",\"pollIntervalMs\":60000,"
			+ "\"columns\":[{\"data\":\"id\",\"title\":\"ID\"},{\"data\":\"name\",\"title\":\"Name\"}]}";
		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n"
			+ "<table data-juneau-view=\"root\" id=\"root\"></table>\n"
			+ "<script type=\"application/json\" id=\"juneau-view:root\">" + meta + "</script>\n"
			+ "<script>\n" + jquery + "\n</script>\n<script>\n" + dataTablesJs + "\n</script>\n"
			+ "<script>\n" + glue + "\n</script>\n<script>\n" + viewsJs + "\n</script>\n</body></html>";
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private Map<?,?> runHarness() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("polling-error.cjs");
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), "http://127.0.0.1:" + port + "/");
		var stdout = dir.resolve("polling-error-stdout.json");
		var stderr = dir.resolve("polling-error-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));
		var p = pb.start();
		if (! p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m");
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + read(stderr));
		return Json.to(Files.readString(stdout), Map.class);
	}

	private static String read(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	private void check(String key) throws Exception {
		var report = runHarness();
		assertEquals(List.of(), report.get("jsFailures"));
		var c = (Map<?,?>)((Map<?,?>)report.get("cases")).get(key);
		assertNotNull(c, () -> "no case " + key + ": " + report);
		assertEquals("idle", c.get("initial"), key + ": starts idle");
		assertEquals("error", c.get("failed"), key + ": a failed poll shows error (" + c + ")");
		assertEquals("idle", c.get("recovered"), key + ": recovers to idle on the next good draw (" + c + ")");
	}

	@Test void a01_serverMode500Json() throws Exception { check("server-json500"); }
	@Test void a02_serverMode500Html() throws Exception { check("server-html500"); }
	@Test void a03_serverModeAbortedRequest() throws Exception { check("server-abort"); }
	@Test void a04_clientMode500Json() throws Exception { check("client-json500"); }
	@Test void a05_clientMode500Html() throws Exception { check("client-html500"); }
	@Test void a06_clientModeAbortedRequest() throws Exception { check("client-abort"); }
}
