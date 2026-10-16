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
package org.apache.juneau.rest.server.terminal.bus;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.bus.websocket.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.rest.server.terminal.*;
import org.apache.juneau.rest.server.views.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.ee11.websocket.jakarta.server.config.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

import jakarta.servlet.http.*;

/**
 * End to end for terminal input: typed keys in a terminal card travel over a real bus WebSocket to
 * {@link TerminalInput}, are authorized by a {@link TerminalAccess}, reach a job and show up in the terminal.
 *
 * <p>
 * Everything is real: a Jetty on a loopback port hosts the bus ({@link BusWebSockets}), the terminal and
 * run-view mixins and the static assets (the real {@code juneau-bus.js}, {@code juneau-terminal.js} and xterm.js), and
 * Chromium runs {@code terminal-input-browser.cjs}.  The page mounts the terminal directly, without the console shell: it
 * declares the two topics, attaches the bridge with {@code attachBridges} and hands {@code mount} a link built from the
 * page bus.
 *
 * <p>
 * Each job is a {@link TerminalProcess} started with input on, so the keys go through {@code juneau_run.py}'s
 * pseudo-terminal: Enter arrives as CR and the terminal turns it into LF, and {@code ^C} is SIGINT from the terminal's
 * line discipline.  The test needs {@code python3} on the {@code PATH} and is skipped without it.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Binds a loopback socket, so it is off by default, including under {@code -Pjs-tests}.  Run it with
 * {@code -Pjs-tests -Dtest='TerminalInput_BrowserTest' -D}{@value #GATE}{@code =true}, outside the sandbox; the profile
 * supplies the node, harness and browser properties.
 *
 * @since 10.0.0
 */
@DisabledOnOs(OS.WINDOWS)
@EnabledIfSystemProperty(named=TerminalInput_BrowserTest.GATE, matches="true",
	disabledReason="binds a socket; run with -Pjs-tests -Dtest='TerminalInput_BrowserTest' -Djuneau.terminalInput=true")
class TerminalInput_BrowserTest extends TestBase {

	/** System property that enables this class; nothing sets it by default. */
	static final String GATE = "juneau.terminalInput";

	private static final Pattern ID = Pattern.compile("[a-z][a-z-]{0,20}");

	private static final Map<String,TerminalProcess> JOBS = new ConcurrentHashMap<>();
	private static String scriptBefore;
	private static Server server;
	private static ServerBus bus;
	private static Path dir;
	private static Map<?,?> report;

	@Rest
	public static class R extends BasicRestServlet implements TerminalMixin, RunViewMixin, BusEventsMixin {
		private static final long serialVersionUID = 1L;

		@Override public ServerBus serverBus() { return bus; }

		@Override public Optional<TerminalSource> terminalSource(String id, RestRequest req) {
			return Optional.ofNullable(JOBS.get(id)).map(TerminalProcess::terminal);
		}

		@Override public Optional<RunViewSource> runViewSource(String runId, RestRequest req) {
			return Optional.ofNullable(JOBS.get(runId)).map(TerminalProcess::events);
		}
	}

	/** Serves the page and the assets from the classpath. */
	static final class Pages extends HttpServlet {
		private static final long serialVersionUID = 1L;

		@Override protected void doGet(HttpServletRequest req, jakarta.servlet.http.HttpServletResponse res) throws IOException {
			var path = req.getPathInfo();
			if (path == null || path.equals("/page")) {
				write(res, "text/html;charset=utf-8", page(req.getParameter("id"), "1".equals(req.getParameter("typist")), "1".equals(req.getParameter("input"))).getBytes(UTF_8));
				return;
			}
			var name = path.substring("/a/".length());
			var type = name.endsWith(".css") ? "text/css" : "text/javascript";
			write(res, type, asset(name));
		}

		private static void write(jakarta.servlet.http.HttpServletResponse res, String type, byte[] body) throws IOException {
			res.setContentType(type);
			res.getOutputStream().write(body);
		}
	}

	@BeforeAll static void setUp() throws Exception {
		var path = System.getenv("PATH");
		assumeTrue(path != null && Arrays.stream(path.split(File.pathSeparator)).anyMatch(d -> Files.isExecutable(Path.of(d, "python3"))),
			"python3 not on the PATH - skipped");
		scriptBefore = System.setProperty(TerminalProcess.SCRIPT_PROPERTY, script().toString());
		dir = Files.createTempDirectory("terminal-input");
		job("t-prompt", "printf 'Continue? [y/N] '; read a; echo \"got $a\"");
		job("t-sleep", "echo sleeping; exec sleep 300");
		job("t-idle", "echo idle; exec sleep 300");
		job("t-tick", "i=0; while true; do echo \"tick $i\"; i=$((i+1)); sleep 0.3; done");

		server = new Server();
		var connector = new ServerConnector(server);
		connector.setHost("127.0.0.1");
		connector.setPort(0);
		server.addConnector(connector);
		connector.open();                       // the port, and so the exact Origin, is known before start
		var origin = "http://127.0.0.1:" + connector.getLocalPort();

		// Everyone may watch; only a session opened with ?typist=1 gets a sink.
		TerminalAccess access = new TerminalAccess() {
			@Override public boolean canWatch(TerminalCaller caller, String id) { return JOBS.containsKey(id); }
			@Override public Optional<TerminalSink> inputSink(TerminalCaller caller, String id) {
				return Boolean.TRUE.equals(caller.attribute("typist")) ? Optional.ofNullable(JOBS.get(id)) : Optional.empty();
			}
		};
		var input = TerminalInput.create(access);
		bus = ServerBus.create(BusPolicy.create()
			.allowedOrigins(origin)
			.upstream(TerminalInput.TOPIC, input)
			.downstream(TerminalInput.STATUS_TOPIC, false)
			.sessionAttributes(r -> Map.of("typist", "1".equals(r.getParameter("typist"))))
			.build());
		input.bind(bus);

		var ctx = new ServletContextHandler();
		ctx.setContextPath("/");
		ctx.addServlet(new ServletHolder(new R()), "/rest/*");
		ctx.addServlet(new ServletHolder(new Pages()), "/*");
		JakartaWebSocketServletContainerInitializer.configure(ctx, (sc, container) -> BusWebSockets.register(container, bus));
		server.setHandler(ctx);
		server.start();

		var js = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harnessDir"), "terminal-input-browser.cjs");
		var out = runNode(
			List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), origin + "/"),
			Map.of("NODE_PATH", js.resolve("node_modules").toString(), "PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers")),
			300);
		report = Json.to(out, Map.class);
		assertNull(report.get("harnessError"), () -> "prober failed: " + report.get("harnessError"));
	}

	@AfterAll static void tearDown() throws Exception {
		try {
			if (server != null)
				server.stop();
		} finally {
			try {
				if (bus != null)
					bus.close();
			} finally {
				JOBS.values().forEach(TerminalProcess::close);
				if (scriptBefore == null)
					System.clearProperty(TerminalProcess.SCRIPT_PROPERTY);
				else
					System.setProperty(TerminalProcess.SCRIPT_PROPERTY, scriptBefore);
				if (dir != null) {
					try (var s = Files.walk(dir)) {
						for (var p : s.sorted(Comparator.reverseOrder()).toList())
							Files.deleteIfExists(p);
					}
				}
			}
		}
	}

	/** Starts {@code sh -c script} under the runner's pseudo-terminal, with input on, as terminal and run {@code id}. */
	private static void job(String id, String script) throws IOException {
		JOBS.put(id, TerminalProcess.start("generic", List.of("sh", "-c", script), 100, 30, dir.resolve(id), true));
	}

	/** {@code juneau-run/src/main/python/juneau_run.py}, from {@code basedir} or the working directory. */
	private static Path script() {
		var basedir = System.getProperty("basedir");
		for (var p : List.of(Path.of(basedir == null ? "." : basedir, "../../juneau-run/src/main/python/juneau_run.py"), Path.of("juneau-run/src/main/python/juneau_run.py")))
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		throw new AssertionError("cannot find juneau-run/src/main/python/juneau_run.py");
	}

	private static String page(String id, boolean typist, boolean input) {
		if (id == null || ! ID.matcher(id).matches() || ! JOBS.containsKey(id))
			throw new IllegalArgumentException("bad id");
		return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\">"
			+ "<link rel=\"stylesheet\" href=\"/a/xterm.css\"><link rel=\"stylesheet\" href=\"/a/juneau-terminal.css\">"
			+ "<script src=\"/a/juneau-bus.js\"></script><script src=\"/a/xterm.js\"></script><script src=\"/a/juneau-terminal.js\"></script>"
			+ "</head><body><div id=\"host\" style=\"width:1000px\"></div><script>"
			+ "const B = window.JuneauViews.bus;"
			+ "B.declare('term.input', {retain: false});"
			+ "B.declare('term.input-status', {retain: false});"
			+ "window.JuneauViews.bus.wiring.attachBridges({bridges: [{id: 'term', transport: 'websocket',"
			+ " session: '/rest" + BusEventsMixin.SESSION_PATH + (typist ? "?typist=1" : "") + "',"
			+ " downstream: ['term.input-status'], upstream: ['term.input']}]});"
			// The console routes the card's input-status role; here the page does it, through the registered card handler.
			+ "const role = window.JuneauConsoleCards.find(function (e) { return e[0] === 'terminal'; })[1].roles['input-status'];"
			+ "B.subscribe('term.input-status', function (p, m) { role(p, m, {id: '" + id + "'}); });"
			+ "window.JuneauTerminal.mount(document.getElementById('host'), {id: '" + id + "', refreshMs: 250,"
			+ (input ? " input: true, inputId: '" + id + "'," : "")
			+ " bytesUrl: '/rest" + TerminalMixin.TERMINAL_PREFIX + "/" + id + "/bytes',"
			+ " eventsUrl: '/rest" + RunViewMixin.RUN_VIEW_PREFIX + "/" + id + "/events'},"
			+ " {publish: function (p) { B.publish('term.input', p); },"
			+ " onConnected: function (fn) { return B.subscribe('bridge:term', function (s) { fn(!!s && s.state === 'open'); }); }});"
			+ "</script></body></html>";
	}

	/** The bytes of a classpath asset: this module's dependencies carry the three scripts, the stylesheet and xterm. */
	private static byte[] asset(String name) throws IOException {
		var path = switch (name) {
			case "xterm.js" -> WebJarResolver.WEBJARS_ROOT + WebJarResolver.resolvePath(TerminalMixin.XTERM_JS_ASSET);
			case "xterm.css" -> WebJarResolver.WEBJARS_ROOT + WebJarResolver.resolvePath(TerminalMixin.XTERM_CSS_ASSET);
			case "juneau-terminal.js" -> "org/apache/juneau/terminal/juneau-terminal.js";
			case "juneau-terminal.css" -> "org/apache/juneau/terminal/juneau-terminal.css";
			case "juneau-bus.js" -> "org/apache/juneau/views/juneau-bus.js";
			default -> throw new IllegalArgumentException(name);
		};
		try (var in = TerminalInput_BrowserTest.class.getClassLoader().getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return in.readAllBytes();
		}
	}

	/** Runs the token list (never a shell string) and returns its stdout; the child and its descendants are killed in {@code finally}. */
	private static String runNode(List<String> cmd, Map<String,String> env, int seconds) throws Exception {
		var stdout = Files.createTempFile("terminal-input-stdout-", ".json");
		var stderr = Files.createTempFile("terminal-input-stderr-", ".txt");
		Process p = null;
		try {
			var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
			pb.environment().putAll(env);
			p = pb.start();
			if (! p.waitFor(seconds, TimeUnit.SECONDS))
				fail(cmd + " did not finish within " + seconds + "s; stderr:\n" + Files.readString(stderr, UTF_8));
			if (p.exitValue() != 0)
				fail(cmd + " exited " + p.exitValue() + "; stderr:\n" + Files.readString(stderr, UTF_8) + "\nstdout:\n" + Files.readString(stdout, UTF_8));
			return Files.readString(stdout, UTF_8);
		} finally {
			if (p != null && p.isAlive()) {
				p.descendants().forEach(ProcessHandle::destroyForcibly);
				p.destroyForcibly();
			}
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
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

	@Test void a01_typedAnswerReachesTheJobAndItsOutputShows() { ok("i01_typedAnswerReachesTheJobAndItsOutputShows"); }
	@Test void a02_ctrlCReachesTheJobAndTheBadgeFails() { ok("i02_ctrlCReachesTheJobAndTheBadgeFails"); }
	@Test void a03_noToggleWhenTheCardHasNoInput() { ok("i03_noToggleWhenTheCardHasNoInput"); }
	@Test void a04_deniedKeepsTheTerminalUpdating() { ok("i04_deniedKeepsTheTerminalUpdating"); }

	@Test void z01_everyCaseHasAMethod() {
		var cases = (Map<?,?>)report.get("cases");
		assertEquals(4, cases.size(), () -> "case count changed; add a method per new case: " + cases.keySet());
	}
}
