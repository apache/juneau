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
 * Chromium canary for the DETAIL_SLOT empty-chevron regression: {@code buildDetailTemplate} must append chrome
 * into {@code template.content} (the fragment {@code expandDetailRow} clones), not the {@code <template>} light
 * DOM.  Chromium's {@code template.appendChild} leaves that fragment empty, so a chevron click cloned nothing and
 * painted a ~26px blank gap.
 *
 * <h5 class='section'>Why this exists (beyond the always-on Node coverage):</h5>
 * <p>
 * {@link ViewsJs_SlotMount_Test} proves the same light-DOM / {@code tpl.content} split under a DOM shim, which does
 * not reproduce Chromium's fragment-vs-light-DOM split.  This class mounts a real {@link ViewSlot} envelope through
 * {@code JuneauViews.regions.mount({ table })} (the production {@code initTableFromDef} slot path) in headless
 * Chromium, then clicks {@code .juneau-view-detail-toggle} &mdash; never the row body &mdash; and asserts the cloned
 * child has a header and real height.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code slot-detail-chevron-browser.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom
 * change is needed.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link TableClipFree_BrowserTest} &mdash; the sibling scroll-clip canary this reuses the
 * 		DataTables child-row stand-in from.
 * 	<li class='jc'>{@link ViewsJs_SlotMount_Test} &mdash; always-on source-shape + Node coverage for the same
 * 		{@code tpl.content} paint.
 * </ul>
 */
@EnabledIfSystemProperty(named=SlotDetailChevron_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class SlotDetailChevron_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	/**
	 * The blank expanded-chevron gap the Gacks panel showed when {@code tpl.content} was empty: a loading-status
	 * strip with no cloned header or sections.  A passing expand must be taller than this.
	 */
	private static final int BLANK_GAP_PX = 26;

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/**
	 * A slot envelope whose DETAIL_SLOT carries a header title plus one region body &mdash; the chrome
	 * {@code buildDetailTemplate} must place on {@code tpl.content}.
	 */
	private static ViewSlot envelope() {
		return ViewSlot.envelope(ViewDef.create("gacks")
			.columns(Column.of("name").title("Name"))
			.details(RowDetailDef.create()
				.endpoint("/data/{id}")
				.title("Incident detail")
				.region(RegionDef.create("d").allowPopulators("p").populate("p")))
			.build());
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent()
			.resolve("slot-detail-chevron-browser.cjs");

		// The body is an empty slot plus the real ViewSlot JSON; the scripts are the real served runtimes.  CSS is
		// required so a cloned header has layout height rather than a 0x0 unstyled box.  No jQuery/DataTables: the
		// prober synthesizes the one body row DataTables would have drawn, as CardActions / TableClipFree do.
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.VIEWS_CSS_RESOURCE)
			+ "\n</style></head><body>\n"
			+ "<div id=\"gacks\"></div>\n"
			+ "<script type=\"application/json\" id=\"slot-envelope\">"
			+ Json.of(envelope())
			+ "</script>\n<script>\n"
			+ resource(ViewsMixin.RENDERS_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.REGIONS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("slot-detail-chevron.html");
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
		var stdout = dir.resolve("slot-detail-chevron-stdout.json");
		var stderr = dir.resolve("slot-detail-chevron-stderr.txt");
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

	private static int num(Object v) {
		assertInstanceOf(Number.class, v, () -> "expected a number, got: " + v + " in " + report);
		return ((Number)v).intValue();
	}

	@Test void a01_runtimeLoadedWithoutErrors() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not export initTableFromDef: " + report);
		assertEquals(Boolean.TRUE, report.get("hasMount"), () -> "juneau-regions.js did not export regions.mount: " + report);
		assertEquals(Boolean.TRUE, report.get("hasRenders"), () -> "juneau-renders.js did not load: " + report);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	@Test void b01_templateContentHasHeaderAndRegion_lightDomEmpty() {
		assertEquals(Boolean.TRUE, report.get("hasTemplate"), () -> "mount did not emit a row-detail template: " + report);
		assertEquals(Boolean.TRUE, report.get("lightDomEmpty"),
			() -> "the <template> light DOM is not empty; Chromium expand clones tpl.content, not light DOM: " + report);
		assertTrue(num(report.get("contentChildCount")) > 0,
			() -> "tpl.content has no children; buildDetailTemplate must append into the fragment expand clones: " + report);
		assertEquals(Boolean.TRUE, report.get("contentHasHeader"),
			() -> "tpl.content has no .juneau-view-detail-header: " + report);
		assertEquals(Boolean.TRUE, report.get("contentHasRegion"),
			() -> "tpl.content has no [data-juneau-region]: " + report);
	}

	@Test void c01_chevronClickClonesPopulatedPanel() {
		assertEquals(Boolean.TRUE, report.get("hasToggle"), () -> "the first-column chevron was not painted: " + report);
		assertEquals(Boolean.TRUE, report.get("expanded"), () -> "chevron click did not expand a detail panel: " + report);
		assertEquals(Boolean.TRUE, report.get("clonedHasHeader"),
			() -> "the cloned panel has no .juneau-view-detail-header (empty tpl.content clone): " + report);
		assertTrue(num(report.get("headerHeight")) > 0,
			() -> "the cloned header has no layout height: " + report);
		assertTrue(num(report.get("panelHeight")) > BLANK_GAP_PX,
			() -> "the cloned panel is still a ~" + BLANK_GAP_PX + "px blank gap: " + report);
		var state = String.valueOf(report.get("detailState"));
		var headerText = String.valueOf(report.get("headerText"));
		var visibleText = String.valueOf(report.get("visibleText"));
		assertTrue("ok".equals(state) || !headerText.isBlank() || !visibleText.isBlank(),
			() -> "cloned panel has neither state=ok nor visible text: " + report);
	}

	@Test void d01_rowBodyClickDoesNotExpand() {
		assertEquals(Boolean.FALSE, report.get("bodyClickExpanded"),
			() -> "clicking the row body expanded the detail panel; expand is chevron-only: " + report);
	}
}
