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
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Runs the REAL served {@code juneau-badges.js} (and {@code chrome.css}) in headless Chromium: a count badge renders
 * visibly in the header with its label, hovering lists the items, Escape closes the list, clicking navigates, and a
 * 403 removes the badge.  Lives in this module for the same reason as {@code ConsoleBadges_Test}.
 *
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  The prober ({@code badges.html.cjs}) is resolved by name from the profile's {@code juneau.jsTests.harnessDir}.
 */
@EnabledIfSystemProperty(named=PendingBadge_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class PendingBadge_BrowserTest extends TestBase {

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
		var harness = Path.of(requiredProperty("juneau.jsTests.harnessDir")).resolve("badges.html.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource("/org/apache/juneau/console/chrome.css")
			+ "\n</style></head><body><div class=\"jc-header-actions\"></div>\n<script>\n"
			+ resource(ConsoleChromeMixin.BADGES_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("pending-badge.html");
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
		var stdout = dir.resolve("pending-badge-stdout.json");
		var stderr = dir.resolve("pending-badge-stderr.txt");
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

	@Test void a01_scriptLoadsWithoutErrors() {
		assertEquals(Boolean.TRUE, report.get("hasMount"), () -> report.toString());
		assertEquals(List.of(), report.get("jsFailures"), () -> report.toString());
	}

	@Test void b01_badgeRendersVisiblyInTheHeader() {
		assertEquals("3 changes pending (1 yours)", report.get("label"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("visible"), () -> report.toString());
		assertEquals("jc-header-badges", report.get("hostClass"), () -> report.toString());
	}

	@Test void b02_hoverListsItemsAndEscapeCloses() {
		assertEquals(2, ((Number)report.get("popoverItems")).intValue(), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("popoverClosedOnEscape"), () -> report.toString());
	}

	@Test void b03_clickNavigates() {
		assertEquals("#changes", report.get("hash"), () -> report.toString());
	}

	@Test void b04_forbiddenRemovesTheBadge() {
		assertEquals(Boolean.TRUE, report.get("deniedRemoved"), () -> report.toString());
	}
}
