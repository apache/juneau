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
import org.apache.juneau.rest.server.filter.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The row-action half of the module's <b>JavaScript-execution harness</b>: runs the REAL served
 * {@code juneau-views.js} in a real headless browser and asserts the fail-closed CSRF submit behaves as a user
 * would experience it &mdash; a blank/absent/whitespace token issues NO request and renders a VISIBLE refusal, and
 * a valid token issues a JSON {@code POST} carrying the {@code X-Csrf-Token} header.
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape test):</h5>
 * <p>
 * {@link ViewsJs_RowActions_Test} proves the shipped script <i>contains</i> the fail-closed logic; it cannot prove
 * the logic actually suppresses a request and surfaces a visible refusal at runtime.  This canary drives the real
 * runtime in Chromium against a stubbed {@code fetch}, so "the request was not sent and a banner became visible" is
 * measured rather than inferred.  The client refusal is defense-against-consumer-omission; the server-side
 * {@link LoopbackBoundary} is the real control.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser, and derives its own prober
 * ({@code row-actions.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is needed
 * to add this second canary.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link PagePanelVisibility_BrowserTest} &mdash; the sibling {@code juneau-pages.js} canary.
 * </ul>
 */
@EnabledIfSystemProperty(named=RowActionCsrf_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class RowActionCsrf_BrowserTest extends TestBase {

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
		// The pom's js-tests profile provisions ONE harness property (the panel-visibility prober); this second
		// canary lives beside it in src/test/js, so it is derived from that property's directory rather than adding
		// a new pom property.
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("row-actions.cjs");

		// The fixture restates nothing under test: it loads the REAL served juneau-views.js.  No jQuery/DataTables
		// is needed - the prober drives the runtime's exposed pure + DOM helpers directly.
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("row-actions.html");
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
		var stdout = dir.resolve("row-actions-stdout.json");
		var stderr = dir.resolve("row-actions-stderr.txt");
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

	@SuppressWarnings("unchecked")
	private static Map<String,Object> req(String key) {
		return (Map<String,Object>) report.get(key);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The runtime loaded, at the current contract version
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_runtimeLoadedAtCurrentContractVersion() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not populate JuneauViews.init: " + report);
		assertEquals("4", report.get("contractVersion"), () -> report.toString());
		assertEquals(LoopbackBoundary.DEFAULT_CSRF_HEADER, report.get("defaultCsrfHeader"), () -> report.toString());
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) The pure request builder fails closed
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_whitespaceTokenRefuses() {
		assertEquals(Boolean.TRUE, req("reqBlankWhitespace").get("refuse"), () -> report.toString());
		assertEquals("missing-token", req("reqBlankWhitespace").get("reason"), () -> report.toString());
	}

	@Test void b02_absentTokenRefuses() {
		assertEquals(Boolean.TRUE, req("reqAbsent").get("refuse"), () -> report.toString());
		assertEquals("missing-token", req("reqAbsent").get("reason"), () -> report.toString());
	}

	@Test void b03_safeMethodRefuses() {
		assertEquals(Boolean.TRUE, req("reqSafeMethod").get("refuse"), () -> report.toString());
		assertEquals("safe-method", req("reqSafeMethod").get("reason"), () -> report.toString());
	}

	@Test void b04_validTokenYieldsJsonRequestWithCsrfHeader() {
		var r = req("reqValid");
		assertNull(r.get("refuse"), () -> report.toString());
		assertEquals("POST", r.get("method"), () -> report.toString());
		@SuppressWarnings("unchecked")
		var headers = (Map<String,Object>) r.get("headers");
		assertEquals("application/json", headers.get("Content-Type"), () -> report.toString());
		assertEquals("tok-123", headers.get(LoopbackBoundary.DEFAULT_CSRF_HEADER), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) The DOM submit path: blank token = visible refusal + NO request; valid token = a real request, no banner
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_blankTokenIssuesNoRequestAndShowsVisibleRefusal() {
		assertEquals(Boolean.FALSE, report.get("blankTokenFetchIssued"), () -> "a request was issued with no token: " + report);
		assertEquals(Boolean.TRUE, report.get("blankTokenRefusalVisible"), () -> "no VISIBLE refusal rendered: " + report);
		assertNotNull(report.get("blankTokenRefusalText"), () -> report.toString());
	}

	@Test void c02_validTokenIssuesTheRequestWithNoRefusal() {
		assertEquals(Boolean.TRUE, report.get("validTokenFetchIssued"), () -> "no request was issued with a valid token: " + report);
		assertEquals("POST", report.get("validTokenMethod"), () -> report.toString());
		assertEquals("application/json", report.get("validTokenContentType"), () -> report.toString());
		assertEquals("tok-123", report.get("validTokenCsrfHeader"), () -> report.toString());
		assertEquals(Boolean.FALSE, report.get("validTokenRefusalVisible"), () -> "a refusal rendered for a valid submit: " + report);
	}
}
