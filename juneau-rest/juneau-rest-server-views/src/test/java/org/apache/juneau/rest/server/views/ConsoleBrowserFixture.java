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

import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;

/**
 * Builds the pages for one {@code console-shell.cjs} run and returns its report. Every page is served from memory at
 * {@code http://juneau.test/}, next to the real shell, chrome CSS, stock {@code open} theme CSS and views assets.
 */
@SuppressWarnings({
	"unchecked" // The JSON report is parsed into a raw Map and cast to Map<String,Map<String,Object>>.
})
final class ConsoleBrowserFixture {

	/** System property the {@code js-tests} profile sets to enable the browser tests. */
	static final String GATE = "juneau.jsTests";

	static final String ORIGIN = "http://juneau.test";
	static final String SHELL = "/juneau-console/juneau-console.js";
	static final String CHROME_CSS = "/juneau-console/chrome.css";
	static final String THEME_CSS = "/juneau-console/juneau-theme-open.css";
	static final String VIEWS_JS = "/views/juneau-views.js";
	static final String VIEWS_CSS = "/views/juneau-views.css";
	static final String SHELL_PREFIX = "[juneau-console] ";

	private final String name;
	private final Map<String,Map<String,String>> assets = new LinkedHashMap<>();
	private final List<Map<String,Object>> cases = new ArrayList<>();

	private ConsoleBrowserFixture(String name) {
		this.name = name;
	}

	static ConsoleBrowserFixture create(String name) throws IOException {
		return new ConsoleBrowserFixture(name)
			.asset(SHELL, resource(ConsoleChromeMixin.CONSOLE_JS_RESOURCE), "text/javascript")
			.asset(CHROME_CSS, resource("/org/apache/juneau/console/chrome.css"), "text/css")
			.asset(THEME_CSS, resource("/org/apache/juneau/console/juneau-theme-open.css"), "text/css")
			.asset(VIEWS_JS, resource(ViewsMixin.VIEWS_JS_RESOURCE), "text/javascript")
			.asset(VIEWS_CSS, resource(ViewsMixin.VIEWS_CSS_RESOURCE), "text/css");
	}

	ConsoleBrowserFixture asset(String path, String body, String type) {
		assets.put(path, Map.of("type", type, "body", body));
		return this;
	}

	/**
	 * Adds a case. {@code queries} are key/selector pairs; the report holds each match's textContent, or null.
	 * The page is served at the URL's path; the query string is kept for the shell's prefix fallback.
	 */
	ConsoleBrowserFixture page(String caseName, String url, String html, String... queries) {
		return add(caseName, url, html, false, queries);
	}

	/** Like {@link #page}, and also records the Task 0 computed styles of the nav selectors. */
	ConsoleBrowserFixture computedPage(String caseName, String url, String html) {
		return add(caseName, url, html, true);
	}

	private ConsoleBrowserFixture add(String caseName, String url, String html, boolean computed, String... queries) {
		assertTrue(url.startsWith(ORIGIN + "/"), () -> "case URLs must be under " + ORIGIN + ": " + url);
		assertEquals(0, queries.length % 2, "queries are key/selector pairs");
		var path = url.substring(ORIGIN.length());
		var q = path.indexOf('?');
		var c = new LinkedHashMap<String,Object>();
		c.put("name", caseName);
		c.put("url", url);
		c.put("pages", Map.of(q < 0 ? path : path.substring(0, q), Map.of("type", "text/html", "body", html)));
		var qm = new LinkedHashMap<String,String>();
		for (var i = 0; i < queries.length; i += 2)
			qm.put(queries[i], queries[i + 1]);
		c.put("queries", qm);
		c.put("computed", computed);
		cases.add(c);
		return this;
	}

	/** Runs the harness and returns the report, keyed by case name. */
	Map<String,Map<String,Object>> run() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("console-shell.cjs");
		var spec = new LinkedHashMap<String,Object>();
		spec.put("assets", assets);
		spec.put("cases", cases);
		var file = Files.createDirectories(dir.resolve("fixtures")).resolve("console-" + name + ".json");
		Files.writeString(file, Json.of(spec), UTF_8);

		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), file.toString());
		var stdout = dir.resolve("console-" + name + "-stdout.json");
		var stderr = dir.resolve("console-" + name + "-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));
		var p = pb.start();
		if (! p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return (Map<String,Map<String,Object>>)Json.to(Files.readString(stdout), Map.class);
	}

	/**
	 * A console page the way the server renders it: the templates, then the contract island, then the shell. The
	 * head links the chrome CSS, the stock {@code open} theme and the views CSS.
	 */
	static String contractPage(String contractJson, String templates, String beforeShell, String afterShell) {
		return "<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\">\n"
			+ "<link rel=\"stylesheet\" href=\"" + CHROME_CSS + "\">\n"
			+ "<link rel=\"stylesheet\" href=\"" + THEME_CSS + "\">\n"
			+ "<link rel=\"stylesheet\" href=\"" + VIEWS_CSS + "\">\n"
			+ "</head><body>\n"
			+ templates + "\n"
			+ "<script type=\"application/json\" id=\"juneau-page\">" + contractJson + "</script>\n"
			+ beforeShell + "\n"
			+ "<script src=\"" + SHELL + "\"></script>\n"
			+ afterShell + "\n"
			+ "</body></html>\n";
	}

	/**
	 * The views pack as inline {@code <script>} blocks: jQuery, DataTables, then the {@code ViewsMixin.*_JS_RESOURCE}
	 * list in the order {@code ToolkitPackRegistry} registers it (pinned by {@code ToolkitPackRegistry_Test}).
	 */
	static String viewsPack() throws IOException {
		var nodeModules = Path.of(requiredProperty("juneau.jsTests.dir")).resolve("node_modules");
		var sb = new StringBuilder();
		sb.append("<script>\n").append(Files.readString(nodeModules.resolve("jquery/dist/jquery.min.js"))).append("\n</script>\n");
		sb.append("<script>\n").append(Files.readString(nodeModules.resolve("datatables.net/js/dataTables.min.js"))).append("\n</script>\n");
		for (var r : List.of(ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.ICONS_JS_RESOURCE, ViewsMixin.SEARCH_JS_RESOURCE,
				ViewsMixin.PAGESTATE_JS_RESOURCE, ViewsMixin.URLSTATE_JS_RESOURCE, ViewsMixin.RIBBON_JS_RESOURCE,
				ViewsMixin.DATATABLES_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.CONFIG_JS_RESOURCE,
				ViewsMixin.REGIONS_JS_RESOURCE, ViewsMixin.CONSOLE_OUTPUT_JS_RESOURCE, ViewsMixin.HELPERS_JS_RESOURCE))
			sb.append("<script>\n").append(resource(r)).append("\n</script>\n");
		return sb.toString();
	}

	static String contractPage(String contractJson, String templates) {
		return contractPage(contractJson, templates, "", "");
	}

	/** A classpath resource (main or test) as a string. */
	static String resource(String path) throws IOException {
		try (var in = ConsoleBrowserFixture.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** The {@code [juneau-console] } console.error lines, prefix removed. */
	static List<String> shellErrors(Map<String,Object> r) {
		return ((List<?>)r.get("consoleErrors")).stream().map(String::valueOf)
			.filter(s -> s.startsWith(SHELL_PREFIX)).map(s -> s.substring(SHELL_PREFIX.length())).toList();
	}

	static void assertNoShellErrors(Map<String,Object> r) {
		assertEquals(List.of(), r.get("banner"), () -> "unexpected banner: " + r);
		assertEquals(List.of(), shellErrors(r), () -> "unexpected shell errors: " + r);
		assertEquals(List.of(), r.get("pageErrors"), () -> "unexpected page errors: " + r);
	}

	/** The banner lists {@code code} with exactly {@code message}, and console.error logged it with the prefix. */
	static void assertFailure(Map<String,Object> r, String code, String message) {
		assertTrue(((List<?>)r.get("banner")).contains(Map.of("code", code, "text", message)),
			() -> "banner does not list " + code + " '" + message + "': " + r.get("banner"));
		assertTrue(shellErrors(r).contains(message), () -> "console.error did not log '" + message + "': " + r.get("consoleErrors"));
	}

	static Map<String,Object> queries(Map<String,Object> r) {
		return (Map<String,Object>)r.get("queries");
	}

	static Map<String,Object> probe(Map<String,Object> r) {
		return (Map<String,Object>)r.get("probe");
	}

	static Map<String,Object> mounted(Map<String,Object> r) {
		return (Map<String,Object>)r.get("mounted");
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}
}
