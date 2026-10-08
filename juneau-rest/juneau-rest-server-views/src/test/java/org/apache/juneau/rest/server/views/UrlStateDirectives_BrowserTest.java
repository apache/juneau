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

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Runs the REAL served scripts in headless Chromium and asserts the {@code ?state=} directives against the real address
 * bar and history: a deep link seeds the request params, {@code setDirective} rewrites the bar and the params, and
 * browser Back restores the prior ids.
 *
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  The prober ({@code url-state-directives.html.cjs}) is resolved by name from the profile's {@code juneau.jsTests.harnessDir}.
 */
@EnabledIfSystemProperty(named=UrlStateDirectives_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class UrlStateDirectives_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harnessDir")).resolve("url-state-directives.html.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.RENDERS_JS_RESOURCE) + "\n</script>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE) + "\n</script>\n<script>\n" + resource(ViewsMixin.URLSTATE_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("url-state-directives.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("url-state-directives-stdout.json");
		var stderr = dir.resolve("url-state-directives-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	@Test void a01_runtimeLoadedWithoutScriptErrors() {
		assertEquals(Boolean.TRUE, report.get("hasSet"), () -> report.toString());
		assertEquals(List.of(), report.get("jsFailures"), () -> report.toString());
	}

	@Test void b01_deepLinkSeedsTheRequestParams() {
		assertEquals("a,b", ((Map<?,?>)report.get("seededParams")).get("ids"), () -> report.toString());
	}

	@Test void b02_setDirectiveRewritesBarAndParamsAndRefetches() {
		var address = (String)report.get("address");
		assertTrue(address.contains("window(start=2026-10-01T00:00:00Z;end=2026-10-02T00:00:00Z)"), address);
		var params = (Map<?,?>)report.get("paramsAfterSet");
		assertEquals("2026-10-01T00:00:00Z", params.get("from"), () -> report.toString());
		assertEquals("2026-10-02T00:00:00Z", params.get("to"), () -> report.toString());
		assertEquals(1, ((Number)report.get("reloadsAfterSet")).intValue(), () -> report.toString());
	}

	@Test void b03_backRestoresThePriorIds() {
		assertEquals("x,y", report.get("afterPushIds"), () -> report.toString());
		assertEquals("a,b", report.get("idsAfterBack"), () -> report.toString());
	}
}
