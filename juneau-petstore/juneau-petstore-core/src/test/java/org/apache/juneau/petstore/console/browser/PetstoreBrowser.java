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
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.marshall.marshaller.*;

/**
 * Runs {@code petstore-browser.cjs} over a list of cases and returns its per-case report.
 *
 * <p>
 * A case is a JSON object: {@code name}, {@code path} (appended to the base URL), optional {@code actions}
 * (each {@code {click|fill|press|waitFor|evaluate|screenshot: ...}}), {@code queries} (name to a JS expression
 * evaluated in the page after the actions), {@code fetches} (name to a URL fetched from the page) and
 * {@code capture} (a URL substring whose requests are recorded with their POST bodies) and
 * {@code allowFailedLoads} (ignore Chromium's "Failed to load resource" line for a deliberate 401/404).
 *
 * <p>
 * The report for each case has {@code status} (the navigation's HTTP status), {@code jsFailures} (page errors
 * and {@code console.error} lines), {@code deprecations} ({@code console.warn} lines matching
 * {@code is deprecated}), {@code queries}, {@code fetches}, {@code requests} and {@code timeouts}.
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
		if (! p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("Harness '" + suite + "' did not finish within 3m; stderr:\n" + quietRead(err));
		}
		assertEquals(0, p.exitValue(), () -> "Harness '" + suite + "' exited non-zero; stderr:\n" + quietRead(err));

		@SuppressWarnings("unchecked") // The harness writes {cases:{name:{...}}}.
		var r = (Map<String,Map<String,Object>>) Json.to(Files.readString(out), Map.class).get("cases");
		assertNotNull(r, () -> "Harness '" + suite + "' wrote no 'cases' object; stdout:\n" + quietRead(out));
		return r;
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
	@SuppressWarnings("unchecked") // queries is always a JSON object.
	public static Object query(Map<String,Object> c, String key) {
		return ((Map<String,Object>) c.get("queries")).get(key);
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
