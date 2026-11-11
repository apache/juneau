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
 * The ribbon's saved-views drop-down in a real browser: hidden until the table has a saved view, placed right before
 * the gear, applies the chosen view, reads "(modified)" once the table differs from it, and is absent when the table
 * turns it off.
 */
@EnabledIfSystemProperty(named=SavedViewsRibbon_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-console-views/pom.xml test`")
@SuppressWarnings({
	"unchecked" // Browser report values are cast to Map<String,Object>
})
class SavedViewsRibbon_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<String,Object> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String page() throws IOException {
		var sb = new StringBuilder("<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">\n");
		sb.append("<style>\n").append(ConsoleBrowserFixture.servedChromeCss()).append("\n</style>\n")
			.append("<style>\n").append(resource(ViewsMixin.VIEWS_CSS_RESOURCE)).append("\n</style>\n")
			.append("<style>\n").append(resource(ViewsMixin.CONFIG_CSS_RESOURCE)).append("\n</style>\n")
			.append("</head><body>\n<div id=\"host\"></div>\n");
		for (var r : List.of(ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE,
				ViewsMixin.URLSTATE_JS_RESOURCE, ViewsMixin.SEARCH_JS_RESOURCE, ViewsMixin.PAGESTATE_JS_RESOURCE, ViewsMixin.CONFIG_JS_RESOURCE))
			sb.append("<script>\n").append(resource(r)).append("\n</script>\n");
		return sb.append("</body></html>\n").toString();
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("saved-views-ribbon-browser.cjs");
		var fixtures = Files.createDirectories(dir.resolve("fixtures").resolve("saved-views-ribbon"));
		var fixture = fixtures.resolve("page.html");
		Files.writeString(fixture, page(), UTF_8);
		var stdout = dir.resolve("saved-views-ribbon-stdout.json");
		var stderr = dir.resolve("saved-views-ribbon-stderr.txt");
		var pb = new ProcessBuilder(List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString()))
			.redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));
		var p = pb.start();
		if (!p.waitFor(5, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 5m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		report = Json.to(Files.readString(stdout), Map.class);
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

	@Test void a01_noPageErrors() {
		assertEquals(List.of(), report.get("errors"));
	}

	@Test void a02_hiddenUntilTheTableHasASavedView() {
		assertEquals(true, report.get("hiddenWithoutViews"), report::toString);
		assertEquals(true, report.get("shownWithView"), report::toString);
	}

	@Test void a03_sitsImmediatelyBeforeTheGear() {
		assertEquals(true, report.get("beforeGear"), report::toString);
	}

	@Test void a04_listsDefaultAndSavedViews() {
		assertEquals(List.of("Default", "Alt"), report.get("labels"), report::toString);
		assertEquals("Alt", report.get("selected"), report::toString);
	}

	@Test void a05_selectingAViewAppliesIt() {
		assertNull(report.get("activeAfterDefault"), report::toString);
		assertEquals("Default", report.get("selectedAfterDefault"), report::toString);
	}

	@Test void a06_marksModifiedWhenTheTableDiffers() {
		assertEquals("Default (modified)", report.get("modifiedLabel"), report::toString);
	}

	@Test void a07_absentWhenTheFlagIsOff() {
		assertEquals(true, report.get("absentWhenOff"), report::toString);
	}
}
