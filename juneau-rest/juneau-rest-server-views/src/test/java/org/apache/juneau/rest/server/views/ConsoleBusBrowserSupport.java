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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;

/**
 * Builds a console page from a {@code console-bus/*.json} contract and runs {@code console-bus.cjs} against it in
 * headless Chromium.
 *
 * <p>
 * The page is the real thing {@link ConsoleBrowserFixture#contractPage} builds: the real shell and the real views pack
 * (with {@code juneau-bus.js} first, or left out for the missing-bus case).  Before the pack it carries
 * {@code console-bus/prelude.js} (fake session and stream fetches, virtual clock) and after it
 * {@code console-bus/cards.js} (the test-only {@code recorder} card type).  Every assertion lives in the calling test.
 */
final class ConsoleBusBrowserSupport {

	static final String GATE = ConsoleBrowserFixture.GATE;

	static final String DISABLED = "JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-rest-server-views test`";

	static final String CSRF_TOKEN = "tok-4711";

	static final String CSRF_HEADER = "X-CSRF-Token";

	private ConsoleBusBrowserSupport() {}

	/**
	 * Runs one scenario of the driver and returns its report.
	 *
	 * @param scenario The driver scenario name.
	 * @param contractResource The {@code console-bus/} contract file.
	 * @param withBus Whether {@code juneau-bus.js} is part of the page.
	 * @param configJson The {@code window.__busTestConfig} object literal.
	 * @param query The page's query string (empty or starting with {@code ?}).
	 * @return The driver's JSON report.
	 * @throws Exception On any failure to build or run the page.
	 */
	@SuppressWarnings("unchecked")
	static Map<String,Object> run(String scenario, String contractResource, boolean withBus, String configJson, String query) throws Exception {
		var dir = Path.of(required("juneau.jsTests.dir"));
		var harness = Path.of(required("juneau.jsTests.harness")).getParent().resolve("console-bus.cjs");
		var fixture = Files.createDirectories(dir.resolve("fixtures")).resolve("console-bus-" + scenario + ".html");
		Files.writeString(fixture, page(contractResource, withBus, configJson));
		var assets = dir.resolve("fixtures").resolve("console-bus-" + scenario + "-assets.json");
		Files.writeString(assets, Json.of(assets()));
		return (Map<String,Object>)Json.to(node(dir, harness, scenario, fixture, assets, query), Map.class);
	}

	private static Map<String,Map<String,String>> assets() throws IOException {
		var m = new LinkedHashMap<String,Map<String,String>>();
		m.put(ConsoleBrowserFixture.SHELL, Map.of("type", "text/javascript", "body", ConsoleBrowserFixture.resource(ConsoleChromeMixin.CONSOLE_JS_RESOURCE)));
		m.put(ConsoleBrowserFixture.CHROME_CSS, Map.of("type", "text/css", "body", ConsoleBrowserFixture.resource("/org/apache/juneau/console/chrome.css")));
		m.put(ConsoleBrowserFixture.THEME_CSS, Map.of("type", "text/css", "body", ConsoleBrowserFixture.resource("/org/apache/juneau/console/juneau-theme-open.css")));
		m.put(ConsoleBrowserFixture.VIEWS_CSS, Map.of("type", "text/css", "body", ConsoleBrowserFixture.resource(ViewsMixin.VIEWS_CSS_RESOURCE)));
		return m;
	}

	static String page(String contractResource, boolean withBus, String configJson) throws IOException {
		var before = script("window.__busTestConfig = " + configJson + ";")
			+ script(ConsoleBrowserFixture.resource("/console-bus/prelude.js"))
			+ ConsoleBrowserFixture.viewsPack(withBus)
			+ (withBus ? script("JuneauViews.bus.config.timers = window.__busTest.clock;\n"
				+ "(window.__busTestConfig.declare || []).forEach(function (d) { JuneauViews.bus.declare(d.topic, { retain: d.retain, by: 'page' }); });") : "")
			+ script(ConsoleBrowserFixture.resource("/console-bus/cards.js"));
		var contract = ConsoleBrowserFixture.resource("/console-bus/" + contractResource).replace("</", "<\\/");
		return ConsoleBrowserFixture.contractPage(contract, "", before, "")
			.replace("<body>", "<body data-juneau-csrf=\"" + CSRF_TOKEN + "\" data-juneau-csrf-header=\"" + CSRF_HEADER + "\">");
	}

	private static String script(String body) {
		return "<script>\n" + body + "\n</script>\n";
	}

	private static String required(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static String node(Path dir, Path harness, String scenario, Path fixture, Path assets, String query) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), scenario, fixture.toString(), assets.toString(), query);
		var stdout = dir.resolve("console-bus-" + scenario + "-stdout.json");
		var stderr = dir.resolve("console-bus-" + scenario + "-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", required("juneau.jsTests.browsers"));
		var p = pb.start();
		if (! p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("console-bus.cjs did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "console-bus.cjs exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	@SuppressWarnings("unchecked")
	static Map<String,Object> map(Object o) {
		assertInstanceOf(Map.class, o);
		return (Map<String,Object>)o;
	}

	@SuppressWarnings("unchecked")
	static List<Object> list(Object o) {
		assertInstanceOf(List.class, o);
		return (List<Object>)o;
	}

	static int num(Object o) {
		return ((Number)o).intValue();
	}
}
