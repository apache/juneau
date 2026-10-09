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
package org.apache.juneau.rest.server.terminal;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.terminal.TerminalProcess.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import com.sun.net.httpserver.*;

/**
 * End to end: the real {@code juneau_run.py --pty} under {@link TerminalProcess}, the real {@link TerminalMixin} and
 * {@link RunViewMixin} endpoints, and the real xterm.js and {@code juneau-terminal.js} in Chromium, driven by
 * {@code terminal-smoke.cjs}.
 *
 * <p>
 * A JDK {@link HttpServer} on a loopback ephemeral port serves the page and the four assets, and forwards
 * {@code /juneau-terminal/*} and {@code /juneau-run-view/*} to a {@link MockRestClient} over a resource implementing
 * both mixins, copying the status and the headers the card reads.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Binds a loopback socket, so it is off by default, including under {@code -Pjs-tests}.  Run it with
 * {@code -Pjs-tests -Dtest='TerminalSmoke_BrowserTest' -D}{@value #GATE}{@code =true}, outside the sandbox; the profile
 * supplies the node, harness and browser properties.  Skipped where {@code python3} is not on the {@code PATH}.
 *
 * <p>
 * The tool is a generated executable script run as a one-token command, not a shell string.  {@link #tearDown()} stops
 * the server, shuts its executor down with {@code shutdownNow()} and kills the run with {@code destroyForcibly()}, each in
 * its own {@code finally}, so a failing setup leaves no thread, socket or process behind.
 *
 * @since 10.0.0
 */
@DisabledOnOs(OS.WINDOWS)
@EnabledIfSystemProperty(named=TerminalSmoke_BrowserTest.GATE, matches="true",
	disabledReason="binds a socket; run with -Pjs-tests -Dtest='TerminalSmoke_BrowserTest' -Djuneau.terminalSmoke=true")
class TerminalSmoke_BrowserTest extends TestBase {

	/** System property that enables this class; nothing sets it by default. */
	static final String GATE = "juneau.terminalSmoke";

	/** The run id the page mounts. */
	static final String ID = "smoke";

	/** The response headers the bridge copies from the mock response. */
	static final List<String> COPIED = List.of("Content-Type", "Cache-Control", "Content-Disposition",
		"Term-Next", "Term-End", "Term-Cols", "Term-Rows", "Term-Done", "Term-Truncated", "Term-Error");

	@Rest
	public static class R extends BasicRestServlet implements TerminalMixin, RunViewMixin {
		private static final long serialVersionUID = 1L;
		static volatile TerminalProcess process;

		@Override
		public Optional<TerminalSource> terminalSource(String id, RestRequest req) {
			return ID.equals(id) ? Optional.of(process.terminal()) : Optional.empty();
		}

		@Override
		public Optional<RunViewSource> runViewSource(String runId, RestRequest req) {
			return ID.equals(runId) ? Optional.of(process.events()) : Optional.empty();
		}
	}

	private static final Object MOCK_LOCK = new Object();

	private static MockRestClient mock;
	private static HttpServer server;
	private static ExecutorService serverExecutor;
	private static Path dir;
	private static Map<?,?> report;

	@BeforeAll static void setUp() throws Exception {
		var path = System.getenv("PATH");
		assumeTrue(TerminalProcess.findOnPath("python3", path) != null, "python3 not on the PATH - skipped");
		var script = TerminalProcessPty_Test.script();
		dir = Files.createTempDirectory("terminal-smoke");

		// 150 filler lines put the Maven steps below the first screen; the progress and red lines at the end are on screen
		// while following.
		var feed = new StringBuilder();
		for (var i = 0; i < 150; i++)
			feed.append("filler ").append(String.format("%03d", i)).append('\n');
		feed.append(Files.readString(script.getParent().getParent().getParent().resolve("test/python/fixtures/maven-serial-success.log"), UTF_8));
		feed.append("progress 10%\rprogress 100%\n");
		feed.append("\u001b[31mred tail\u001b[0m\n");
		var feedFile = dir.resolve("feed.txt");
		Files.writeString(feedFile, feed, UTF_8);
		var tool = dir.resolve("tool");
		Files.writeString(tool, "#!/bin/sh\ncat '" + feedFile + "'\nexit 3\n", UTF_8);
		Files.setPosixFilePermissions(tool, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));

		var host = new Host(System.getProperty("os.name"), script.toString(), null, path);
		R.process = TerminalProcess.start("maven", List.of(tool.toString()), 100, 30, dir.resolve("run"), host);
		assertEquals(3, R.process.waitFor(60, TimeUnit.SECONDS));

		mock = MockRestClient.buildLax(R.class);
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		serverExecutor = Executors.newCachedThreadPool();
		server.setExecutor(serverExecutor);
		server.createContext(TerminalMixin.TERMINAL_PREFIX + "/", TerminalSmoke_BrowserTest::bridge);
		server.createContext(RunViewMixin.RUN_VIEW_PREFIX + "/", TerminalSmoke_BrowserTest::bridge);
		server.createContext("/a/", ex -> {
			try {
				var name = ex.getRequestURI().getPath().substring("/a/".length());
				var type = name.endsWith(".css") ? "text/css" : "text/javascript";
				writeBody(ex, 200, Map.of("Content-Type", type), TerminalHarness.asset(name));
			} catch (Exception e) {
				sendFailure(ex, e);
			}
		});
		server.createContext("/page", ex -> writeBody(ex, 200, Map.of("Content-Type", "text/html;charset=utf-8"), page().getBytes(UTF_8)));
		server.start();

		var js = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harnessDir"), "terminal-smoke.cjs");
		var out = TerminalHarness.run(
			List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), "http://127.0.0.1:" + server.getAddress().getPort() + "/"),
			List.of(),
			Map.of("NODE_PATH", js.resolve("node_modules").toString(), "PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers")),
			300);
		report = Json.to(out, Map.class);
		assertNull(report.get("harnessError"), () -> "prober failed: " + report.get("harnessError"));
	}

	@AfterAll static void tearDown() throws IOException {
		try {
			if (server != null)
				server.stop(0);
		} finally {
			try {
				if (serverExecutor != null)
					serverExecutor.shutdownNow();
			} finally {
				try {
					if (R.process != null)
						R.process.close();
				} finally {
					if (dir != null) {
						try (var s = Files.walk(dir)) {
							for (var p : s.sorted(Comparator.reverseOrder()).toList())
								Files.deleteIfExists(p);
						}
					}
				}
			}
		}
	}

	private static String page() {
		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\">"
			+ "<link rel=\"stylesheet\" href=\"/a/xterm.css\"><link rel=\"stylesheet\" href=\"/a/juneau-terminal.css\">"
			+ "<script src=\"/a/xterm.js\"></script><script src=\"/a/juneau-terminal.js\"></script>"
			+ "</head><body><div id=\"host\" style=\"width:1000px\"></div><script>"
			+ "window.JuneauTerminal.mount(document.getElementById('host'), {id: '" + ID + "', refreshMs: 250,"
			+ " bytesUrl: '" + TerminalMixin.TERMINAL_PREFIX + "/" + ID + "/bytes',"
			+ " eventsUrl: '" + RunViewMixin.RUN_VIEW_PREFIX + "/" + ID + "/events'});"
			+ "</script></body></html>";
	}

	/** Forwards one GET to the mixins, copying the status, the body bytes and {@link #COPIED}. */
	private static void bridge(HttpExchange ex) throws IOException {
		try {
			var uri = ex.getRequestURI();
			var target = uri.getRawQuery() == null ? uri.getRawPath() : uri.getRawPath() + "?" + uri.getRawQuery();
			int status;
			byte[] body;
			var headers = new LinkedHashMap<String,String>();
			synchronized (MOCK_LOCK) {
				var res = mock.get(target).run();
				status = res.getStatusCode();
				for (var h : COPIED)
					res.getStringHeader(h).ifPresent(v -> headers.put(h, v));
				body = res.getContent().asBytes();
			}
			writeBody(ex, status, headers, body);
		} catch (Exception e) {
			sendFailure(ex, e);
		}
	}

	private static void writeBody(HttpExchange ex, int status, Map<String,String> headers, byte[] body) throws IOException {
		headers.forEach((k, v) -> ex.getResponseHeaders().add(k, v));
		ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
		if (body.length > 0) {
			try (var os = ex.getResponseBody()) {
				os.write(body);
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
		writeBody(ex, 500, Map.of("Content-Type", "text/plain;charset=utf-8"), sw.toString().getBytes(UTF_8));
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Asserts that case {@code name} passed with no page or console errors. */
	private static void ok(String name) {
		var cases = (Map<?,?>)report.get("cases");
		assertEquals(true, cases.get(name), () -> name + " -> " + cases.get(name));
		assertEquals(List.of(), ((Map<?,?>)report.get("pageErrors")).get(name), () -> name + " page errors");
		assertEquals(List.of(), ((Map<?,?>)report.get("consoleErrors")).get(name), () -> name + " console errors");
	}

	@Test void a01_badgeShowsTheExitCodeProgressAndColour() { ok("s01_badgeShowsTheExitCodeProgressAndColour"); }
	@Test void a02_stepHashScrollsToItsBuildingLine() { ok("s02_stepHashScrollsToItsBuildingLine"); }
	@Test void a03_rawIsTheWholeLogAsAnAttachment() { ok("s03_rawIsTheWholeLogAsAnAttachment"); }

	@Test void z01_everyCaseHasAMethod() {
		var cases = (Map<?,?>)report.get("cases");
		assertEquals(3, cases.size(), () -> "case count changed; add a method per new case: " + cases.keySet());
	}
}
