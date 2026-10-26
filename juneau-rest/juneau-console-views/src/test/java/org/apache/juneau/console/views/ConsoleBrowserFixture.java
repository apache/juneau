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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.console.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Builds the pages for one {@code console-shell.cjs} run and returns its report. Every page is served from memory at
 * {@code http://juneau.test/}, next to the real shell, chrome CSS, stock {@code open} theme CSS and views assets.
 * The chrome CSS is the body {@link ConsoleChromeMixin} serves, so it carries the generated {@code Theme.OPEN} block
 * and role aliases as well as the static file.
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
	static final String DARK_CSS = "/juneau-console/juneau-theme-dark.css";
	static final String VIEWS_JS = "/views/juneau-views.js";
	static final String VIEWS_CSS = "/views/juneau-views.css";
	static final String SHELL_PREFIX = "[juneau-console] ";

	/** A host that mounts {@link ConsoleChromeMixin}, so the fixture reads chrome.css from the real endpoint. */
	@Rest(mixins=ConsoleChromeMixin.class)
	public static class ChromeHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static volatile String servedChromeCss;

	private final String name;
	private final Map<String,Map<String,Object>> assets = new LinkedHashMap<>();
	private final Map<String,List<Map<String,String>>> sequences = new LinkedHashMap<>();
	private final List<Map<String,Object>> cases = new ArrayList<>();

	private ConsoleBrowserFixture(String name) {
		this.name = name;
	}

	static ConsoleBrowserFixture create(String name) throws IOException {
		return new ConsoleBrowserFixture(name)
			.asset(SHELL, resource(ConsoleChromeMixin.CONSOLE_JS_RESOURCE), "text/javascript")
			.asset(CHROME_CSS, servedChromeCss(), "text/css")
			.asset(THEME_CSS, resource("/org/apache/juneau/console/juneau-theme-open.css"), "text/css")
			.asset(DARK_CSS, resource("/org/apache/juneau/console/juneau-theme-dark.css"), "text/css")
			.asset(VIEWS_JS, resource(ViewsMixin.VIEWS_JS_RESOURCE), "text/javascript")
			.asset(VIEWS_CSS, resource(ViewsMixin.VIEWS_CSS_RESOURCE), "text/css");
	}

	/** The chrome stylesheet exactly as {@code GET /juneau-console/chrome.css} serves it; read once. */
	@SuppressWarnings({
		"resource" // assertOk() returns the same response the try already closes.
	})
	static String servedChromeCss() throws IOException {
		var s = servedChromeCss;
		if (s == null) {
			try (var c = MockRestClient.create(ChromeHost.class); var r = c.get(ConsoleChromeMixin.CHROME_CSS_PATH).run()) {
				r.assertOk();
				s = r.getBodyAsString();
			}
			servedChromeCss = s;
		}
		return s;
	}

	ConsoleBrowserFixture asset(String path, String body, String type) {
		assets.put(path, Map.of("type", type, "body", body));
		return this;
	}

	/** Like {@link #asset(String,String,String)}, and the harness holds the response back for {@code delayMs} (a slow script keeps the parser blocked). */
	ConsoleBrowserFixture delayedAsset(String path, String body, String type, int delayMs) {
		assets.put(path, Map.of("type", type, "body", body, "delayMs", delayMs));
		return this;
	}

	/**
	 * Answers the n-th request to {@code path} in each case with {@code bodies[n]} (status 200, {@code type}), and any
	 * later request with a 404.  The count restarts for every case; each answered request is recorded in the case's
	 * {@code requests} report entry as {@code {path, search, lastEventId}}.
	 */
	ConsoleBrowserFixture sequence(String path, String type, String... bodies) {
		var l = new ArrayList<Map<String,String>>();
		for (var b : bodies)
			l.add(Map.of("type", type, "body", b));
		sequences.put(path, l);
		return this;
	}

	/**
	 * Adds a case. {@code queries} are key/selector pairs; the report holds each match's textContent, or null.
	 * The page is served at the URL's path; the query string is kept for the shell's prefix fallback.
	 */
	ConsoleBrowserFixture page(String caseName, String url, String html, String... queries) {
		return add(caseName, url, html, false, queries);
	}

	/**
	 * Makes the most recently added case wait (up to 10s) for {@code selector} to match before it is probed, for
	 * content a card paints after an asynchronous fetch.
	 */
	ConsoleBrowserFixture waitFor(String selector) {
		cases.get(cases.size() - 1).put("waitFor", selector);
		return this;
	}

	/** Makes the most recently added case click {@code selector} (after any wait) before it is probed; may be repeated. */
	ConsoleBrowserFixture click(String selector) {
		((List<String>)cases.get(cases.size() - 1).computeIfAbsent("clicks", k -> new ArrayList<String>())).add(selector);
		return this;
	}

	/** Makes the most recently added case emulate the {@code prefers-color-scheme} value {@code scheme} ({@code light} or {@code dark}). */
	ConsoleBrowserFixture colorScheme(String scheme) {
		cases.get(cases.size() - 1).put("colorScheme", scheme);
		return this;
	}

	/** Makes the most recently added case seed {@code localStorage} with {@code values} before any page script runs. */
	ConsoleBrowserFixture storage(Map<String,String> values) {
		cases.get(cases.size() - 1).put("storage", new LinkedHashMap<>(values));
		return this;
	}

	/**
	 * Appends an ordered step to the most recently added case, run after any {@link #click} and before the probe:
	 * {@code press} sends a key to the focused element, {@code emulate} switches {@code prefers-color-scheme} while the
	 * page is open, {@code forcedColors} switches the {@code forced-colors} media feature, and {@code eval} runs a script
	 * in the page.
	 */
	private ConsoleBrowserFixture step(String kind, String value) {
		((List<Map<String,String>>)cases.get(cases.size() - 1).computeIfAbsent("steps", k -> new ArrayList<Map<String,String>>())).add(Map.of("kind", kind, "value", value));
		return this;
	}

	/** Step: presses {@code key} (a Playwright key name such as {@code ArrowRight}). */
	ConsoleBrowserFixture press(String key) {
		return step("press", key);
	}

	/** Step: emulates {@code prefers-color-scheme} {@code scheme} on the open page. */
	ConsoleBrowserFixture emulate(String scheme) {
		return step("emulate", scheme);
	}

	/**
	 * Step: emulates the {@code forced-colors} media feature on the open page.
	 *
	 * @param value {@code "active"} or {@code "none"}.
	 * @return This object.
	 */
	ConsoleBrowserFixture forcedColors(String value) {
		return step("forcedColors", value);
	}

	/** Step: evaluates {@code script} in the page. */
	ConsoleBrowserFixture eval(String script) {
		return step("eval", script);
	}

	/**
	 * Makes the most recently added case record, in the first animation frame (before the first paint), the root
	 * {@code data-jc-scheme} attribute and the body's computed background color.  The report's {@code firstFrame}
	 * entry holds {@code {scheme, bodyBg, hasBody}}.
	 */
	ConsoleBrowserFixture firstFrameProbe() {
		cases.get(cases.size() - 1).put("firstFrame", true);
		return this;
	}

	/**
	 * Makes the most recently added case measure WCAG contrast for every element matching {@code selector}, after
	 * any clicks and steps; may be repeated.  {@code kind} is {@code text}, {@code placeholder}, {@code border},
	 * {@code outline} or {@code focus-border} (for the last two the element is focused first, in keyboard modality).  The report's {@code contrast} entry maps {@code name} to
	 * {@code {count, worst: {fg, bg, ratio, fgRaw, text}}} for the worst rendered match (hidden matches are skipped).
	 */
	ConsoleBrowserFixture probeContrast(String name, String kind, String selector) {
		((List<Map<String,String>>)cases.get(cases.size() - 1).computeIfAbsent("contrast", k -> new ArrayList<Map<String,String>>()))
			.add(Map.of("name", name, "kind", kind, "selector", selector));
		return this;
	}

	/**
	 * Makes the most recently added case log every request the page makes, as {@code {path, phase}} in the report's
	 * {@code requestLog} entry.  {@code phase} is {@code load} until the page has loaded, any wait has matched and
	 * the network has gone quiet, and {@code steps} from the first click or step on; after the steps the harness
	 * waits for the network to go quiet again, so a request still in flight is logged.
	 */
	ConsoleBrowserFixture watchRequests() {
		cases.get(cases.size() - 1).put("watchRequests", true);
		return this;
	}

	static List<Map<String,Object>> requestLog(Map<String,Object> r) {
		var l = (List<Map<String,Object>>)r.get("requestLog");
		assertNotNull(l, () -> "no request log: " + r);
		return l;
	}

	static Map<String,Object> firstFrame(Map<String,Object> r) {
		return (Map<String,Object>)r.get("firstFrame");
	}

	static Map<String,Map<String,Object>> contrast(Map<String,Object> r) {
		return (Map<String,Map<String,Object>>)r.get("contrast");
	}

	/** Like {@link #page}, and also records the baseline computed styles of the nav selectors. */
	ConsoleBrowserFixture computedPage(String caseName, String url, String html) {
		return add(caseName, url, html, true);
	}

	private ConsoleBrowserFixture add(String caseName, String url, String html, boolean computed, String... queries) {
		assertTrue(url.startsWith(ORIGIN + "/"), () -> "case URLs must be under " + ORIGIN + ": " + url);
		assertEquals(0, queries.length % 2, "queries are key/selector pairs");
		var path = url.substring(ORIGIN.length());
		var q = path.indexOf('?');
		Map<String,Object> c = map(
			"name", caseName,
			"url", url,
			"pages", Map.of(q < 0 ? path : path.substring(0, q), Map.of("type", "text/html", "body", html))
		);
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
		Map<String,Object> spec = map("assets", assets, "sequences", sequences, "cases", cases);
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
		return contractPage(contractJson, templates, beforeShell, afterShell, "", false);
	}

	/**
	 * Like {@link #contractPage(String,String,String,String)}, with {@code htmlAttrs} written into the {@code <html>} tag
	 * (for example {@code data-jc-scheme-toggle}), {@link ConsoleChromeMixin#SCHEME_SCRIPT} first in the head, and, when
	 * {@code darkLink} is set, the dark layer linked between the chrome CSS and the palette.
	 */
	static String contractPage(String contractJson, String templates, String beforeShell, String afterShell, String htmlAttrs, boolean darkLink) {
		return "<!DOCTYPE html>\n<html lang=\"en\"" + (htmlAttrs.isEmpty() ? "" : " " + htmlAttrs) + "><head><meta charset=\"utf-8\">"
			+ (htmlAttrs.isEmpty() && ! darkLink ? "" : "<script>" + ConsoleChromeMixin.SCHEME_SCRIPT + "</script>")
			+ "<link rel=\"icon\" href=\"data:,\">\n"
			+ "<link rel=\"stylesheet\" href=\"" + CHROME_CSS + "\">\n"
			+ (darkLink ? "<link rel=\"stylesheet\" href=\"" + DARK_CSS + "\">\n" : "")
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
		return viewsPack(true);
	}

	/** Like {@link #viewsPack()}, and without {@code juneau-bus.js} when {@code withBus} is false (for the missing-bus case). */
	static String viewsPack(boolean withBus) throws IOException {
		var nodeModules = Path.of(requiredProperty("juneau.jsTests.dir")).resolve("node_modules");
		var sb = new StringBuilder();
		sb.append("<script>\n").append(Files.readString(nodeModules.resolve("jquery/dist/jquery.min.js"))).append("\n</script>\n");
		sb.append("<script>\n").append(Files.readString(nodeModules.resolve("datatables.net/js/dataTables.min.js"))).append("\n</script>\n");
		for (var r : List.of(ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.ICONS_JS_RESOURCE, ViewsMixin.SEARCH_JS_RESOURCE,
				ViewsMixin.PAGESTATE_JS_RESOURCE, ViewsMixin.URLSTATE_JS_RESOURCE, ViewsMixin.RIBBON_JS_RESOURCE,
				ViewsMixin.DATATABLES_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.CONFIG_JS_RESOURCE,
				ViewsMixin.REGIONS_JS_RESOURCE, ViewsMixin.HELPERS_JS_RESOURCE))
			if (withBus || ! r.equals(ViewsMixin.BUS_JS_RESOURCE))
				sb.append("<script>\n").append(resource(r)).append("\n</script>\n");
		return sb.toString();
	}

	/**
	 * A page built from a {@code pagespec-corpus/<name>.json} fixture (what {@code PageSpec} produced for a parity case;
	 * kept fresh by {@code PageSpec_BrowserCorpus_Test}): its contract island and its {@code <template data-card>} set.
	 */
	static String corpusPage(String name, String beforeShell, String afterShell) throws IOException {
		var corpus = (Map<String,Object>)Json.to(resource("/pagespec-corpus/" + name + ".json"), Map.class);
		var templates = new StringBuilder();
		((Map<String,Object>)corpus.get("templates")).forEach((id, html) ->
			templates.append("<template data-card=\"").append(id).append("\">").append(html).append("</template>\n"));
		return contractPage(Json.of(corpus.get("contract")), templates.toString(), beforeShell, afterShell);
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
