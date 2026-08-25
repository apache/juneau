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
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Opt-in Chromium layout canary for a {@link BarSlot} hosted on the row-detail ribbon.  The always-on behavioral
 * coverage lives in {@link ViewsJs_RowDetail_BarSlot_Test}; this class proves the one thing a fake DOM cannot: with
 * the real {@code juneau-views.css} applied, the bar slot's badge is <b>visible beside the ribbon</b> at a wide
 * viewport and <b>never overlaps a ribbon tab</b> at a narrow one (it wraps below instead).
 *
 * <p>
 * The bar-slot region markup is produced by the real {@link BarSlotTable#detailRegion} emitter, so the fixture cannot
 * drift from what the server actually paints.
 *
 * <p>
 * Disabled unless the {@value #GATE} system property is set (the module's {@code js-tests} Maven profile).
 */
@EnabledIfSystemProperty(named=RowDetail_BarSlot_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class RowDetail_BarSlot_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** A 2-section detail panel in its SERVER-painted shape: stacked sections, bar region as the last child. */
	private static String panelHtml() {
		var bar = BarSlot.create("detail-ctx").widgets(
			BarText.of("state", "Region us-east"),
			BarBadge.of("open").label("Open").badge(Badge.count(12)));
		var region = Html.of(BarSlotTable.detailRegion(bar, BarSlotTable.ANCHOR_RIBBON));
		var sections = new StringBuilder();
		for (var s : List.of("overview", "history", "attachments"))
			sections.append("<section class=\"juneau-view-detail-section\" data-juneau-detail-section=\"").append(s)
				.append("\"><h2 class=\"juneau-view-detail-section-title\">").append(s)
				.append("</h2><div class=\"juneau-view-detail-fields\"></div></section>");
		return "<div class=\"juneau-view-detail-panel\" id=\"panel\">" + sections + region + "</div>";
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent()
			.resolve("detail-bar-slot-browser.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.VIEWS_CSS_RESOURCE)
			+ "\n</style></head><body>\n" + panelHtml() + "\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("detail-bar-slot.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("detail-bar-slot-browser-stdout.json");
		var stderr = dir.resolve("detail-bar-slot-browser-stderr.txt");
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

	@Test void a01_runtimeLoadedWithNoScriptErrors() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not load: " + report);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	@Test void a02_slotIsRelocatedToTheRibbonsTrailingPosition() {
		assertEquals(Boolean.TRUE, report.get("stripBuilt"), report::toString);
		assertEquals(Boolean.TRUE, report.get("slotTrailsStrip"), report::toString);
		assertEquals("1", report.get("stripTrailed"), report::toString);
	}

	@Test void a03_badgeIsVisibleBesideTheRibbon() {
		assertEquals(Boolean.TRUE, report.get("wide_slotVisible"), report::toString);
		assertEquals(Boolean.TRUE, report.get("wide_badgeVisible"), report::toString);
		assertEquals(Boolean.TRUE, report.get("wide_slotBesideRibbon"), report::toString);
		assertEquals(Boolean.TRUE, report.get("wide_sameLineAsRibbon"), report::toString);
		assertEquals("inline-flex", report.get("wide_computedDisplay"), report::toString);
	}

	@Test void a04_neverOverlapsRibbonItems_wideOrNarrow() {
		assertEquals(Boolean.TRUE, report.get("wide_noOverlap"), report::toString);
		assertEquals(Boolean.TRUE, report.get("narrow_noOverlap"),
			() -> "the slot must WRAP below the ribbon at a narrow width, never overlap it: " + report);
		assertEquals(Boolean.TRUE, report.get("narrow_badgeVisible"), report::toString);
		assertEquals(Boolean.TRUE, report.get("narrow_slotWithinViewport"), report::toString);
	}
}
