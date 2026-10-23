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
package org.apache.juneau.petstore.console.browser;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.Shorts.f;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.juneau.marshall.marshaller.*;

/**
 * Runs {@code petstore-browser.cjs} over a list of cases and returns its per-case report.
 *
 * <p>
 * A case is a JSON object: {@code name}, {@code path} (appended to the base URL), optional {@code waitUntil} (the
 * navigation's Playwright wait state, {@code load} by default, so start with a {@code waitFor}; a case that asserts
 * straight after the navigation passes {@code networkidle}), optional {@code actions}
 * (each {@code {click|fill|press|waitFor|waitForCaptured|evaluate|screenshot|download: ...}}, or {@code {reload|back|forward: true}};
 * {@code reload} waits for the case's {@code waitUntil} and {@code back}/{@code forward} only for the navigation to
 * commit, so follow them with a {@code waitFor}), {@code queries} (name to a JS expression
 * evaluated in the page after the actions), {@code fetches} (name to a URL fetched from the page),
 * {@code capture} (a URL substring whose requests are recorded with their POST bodies) and
 * {@code allowFailedLoads} (ignore Chromium's "Failed to load resource" line for a deliberate 401/404).
 * A case may also set {@code viewport} ({@code [width, height]}), {@code deviceScaleFactor} (default 1) and
 * {@code initScript} (JS run at the start of every document).  A {@code screenshot} action may carry a {@code selector}
 * to capture one element instead of the whole page.  The extra actions are {@code {goto:path}} and
 * {@code {follow:jsExpression}}, which wait for the case's {@code waitUntil} as {@code reload} does, and
 * {@code {drag:[selector, dx]}}.
 *
 * <p>
 * The report for each case has {@code status} (the navigation's HTTP status), {@code jsFailures} (page errors
 * and {@code console.error} lines), {@code deprecations} ({@code console.warn} lines matching
 * {@code is deprecated}), {@code queries}, {@code fetches}, {@code requests}, {@code downloads} (by the {@code download} action's
 * {@code as} name: {@code {name, bytes, head}}), {@code timeouts} and {@code ms} (the case's wall time in
 * milliseconds, including its browser context).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>report</jv> = PetstoreBrowser.<jsm>run</jsm>(<js>"nav"</js>, <jv>base</jv>, List.<jsm>of</jsm>(
 * 		Map.<jsm>of</jsm>(<js>"name"</js>, <js>"store"</js>, <js>"path"</js>, <js>"/console/store"</js>,
 * 			<js>"queries"</js>, Map.<jsm>of</jsm>(<js>"title"</js>, <js>"document.title"</js>))));
 * 	PetstoreBrowser.<jsm>assertClean</jsm>(<jv>report</jv>, <js>"store"</js>);
 * </p>
 */
public final class PetstoreBrowser {

	/** System property the {@code js-tests} profile sets to enable the {@code *_BrowserTest} classes. */
	public static final String GATE = "juneau.jsTests";

	/** The skip reason shown when the gate is off. */
	public static final String DISABLED = "Browser tests are opt-in; run with `mvn -Pjs-tests -pl juneau-petstore/juneau-petstore-core -am test`";

	private PetstoreBrowser() {}

	/**
	 * @param suite A short name for the files this run writes under {@code target/js}.
	 * @param baseUrl The server base URL.
	 * @param cases The cases.
	 * @return Case name to that case's report.
	 * @throws Exception If the harness can't run or exits non-zero.
	 */
	public static Map<String,Map<String,Object>> run(String suite, String baseUrl, List<Map<String,Object>> cases) throws Exception {
		var dir = Path.of(required("juneau.jsTests.dir"));
		var harness = Path.of(required("juneau.jsTests.harness"));
		var in = dir.resolve(suite + "-cases.json");
		var out = dir.resolve(suite + "-stdout.json");
		var err = dir.resolve(suite + "-stderr.txt");
		Files.createDirectories(dir.resolve("snapshots"));
		Files.writeString(in, Json.of(Map.of("baseUrl", baseUrl, "snapshots", dir.resolve("snapshots").toString(), "cases", cases)), UTF_8);

		var pb = new ProcessBuilder(List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), in.toString()))
			.redirectOutput(out.toFile()).redirectError(err.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", required("juneau.jsTests.browsers"));
		var p = pb.start();
		if (! p.waitFor(8, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("Harness '" + suite + "' did not finish within 8m; stderr:\n" + quietRead(err));
		}
		assertEquals(0, p.exitValue(), () -> "Harness '" + suite + "' exited non-zero; stderr:\n" + quietRead(err));

		@SuppressWarnings({
			"unchecked" // The harness writes {cases:{name:{...}}}.
		})
		var r = (Map<String,Map<String,Object>>) Json.to(Files.readString(out), Map.class).get("cases");
		assertNotNull(r, () -> "Harness '" + suite + "' wrote no 'cases' object; stdout:\n" + quietRead(out));
		printSlowest(suite, r);
		return r;
	}

	@SuppressWarnings({
		"java:S106" // The slowest cases are the measurement this class exists to report.
	})
	private static void printSlowest(String suite, Map<String,Map<String,Object>> report) {
		System.out.println("[petstore-browser] " + suite + ": " + report.size() + " cases, slowest: " + slowest(report, 5));
	}

	/**
	 * @param report The {@link #run} result.
	 * @param n How many cases to list.
	 * @return The {@code n} slowest cases as {@code "name 1234 ms"}, longest first, comma separated.
	 */
	static String slowest(Map<String,Map<String,Object>> report, int n) {
		return report.entrySet().stream()
			.filter(e -> e.getValue().get("ms") instanceof Number)
			.sorted((a, b) -> Long.compare(((Number) b.getValue().get("ms")).longValue(), ((Number) a.getValue().get("ms")).longValue()))
			.limit(n)
			.map(e -> e.getKey() + " " + ((Number) e.getValue().get("ms")).longValue() + " ms")
			.collect(Collectors.joining(", "));
	}

	/**
	 * Asserts the case loaded with status 200, no script errors, no deprecation warnings and no timeouts.
	 *
	 * @param report The {@link #run} result.
	 * @param name The case name.
	 * @return The case's report.
	 */
	public static Map<String,Object> assertClean(Map<String,Map<String,Object>> report, String name) {
		var c = report.get(name);
		assertNotNull(c, () -> "No report for case '" + name + "'; cases: " + report.keySet());
		assertEquals(200, ((Number) c.get("status")).intValue(), () -> "Case '" + name + "' status: " + c);
		assertEquals(List.of(), c.get("jsFailures"), () -> "Case '" + name + "' logged script errors: " + c.get("jsFailures"));
		assertEquals(List.of(), c.get("deprecations"), () -> "Case '" + name + "' used deprecated client APIs: " + c.get("deprecations"));
		assertEquals(List.of(), c.get("timeouts"), () -> "Case '" + name + "' timed out waiting for: " + c.get("timeouts"));
		return c;
	}

	/**
	 * @param c One case's report.
	 * @param key The query name.
	 * @return The query's value as returned by the page.
	 */
	@SuppressWarnings({
		"unchecked" // queries is always a JSON object.
	})
	public static Object query(Map<String,Object> c, String key) {
		return ((Map<String,Object>) c.get("queries")).get(key);
	}

	/**
	 * Returns the JSON body of the last captured POST whose URL contains the given text.
	 *
	 * @param c One case's report (its {@code capture} must cover the URL).
	 * @param urlPart The URL substring.
	 * @return The parsed POST body.
	 * @throws Exception If the body is not JSON.
	 */
	@SuppressWarnings({
		"unchecked" // requests is a JSON array of {url, method, postData}.
	})
	public static Map<String,Object> lastCaptured(Map<String,Object> c, String urlPart) throws Exception {
		var reqs = ((List<Map<String,Object>>) c.get("requests")).stream()
			.filter(r -> "POST".equals(r.get("method")) && String.valueOf(r.get("url")).contains(urlPart)).toList();
		assertFalse(reqs.isEmpty(), () -> f("No captured POST matching '%s'", urlPart));
		return Json.to((String) reqs.get(reqs.size() - 1).get("postData"), Map.class);
	}

	private static String required(String name) {
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
