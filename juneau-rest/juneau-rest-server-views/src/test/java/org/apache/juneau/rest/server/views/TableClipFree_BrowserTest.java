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
 * The <b>"popups never clip"</b> half of the module's JavaScript-execution harness: runs the real served
 * {@code juneau-views.js} / {@code juneau-renders.js} / {@code juneau-views.css} in headless Chromium and proves that
 * a surface anchored in a table cell escapes the table's scroll region instead of being clipped by it.
 *
 * <h5 class='section'>The matrix:</h5>
 * <p>
 * Three surfaces (row-action menu, cell popover, timestamp popup) across three scroll boundaries, each with the
 * anchoring cell deliberately scrolled half out of view:
 * <ul class='spaced-list'>
 * 	<li><b>DT1</b> &mdash; the JS-inserted {@code .juneau-view-table-scroll} wrap.
 * 	<li><b>DT2</b> &mdash; the flex {@code .dt-layout-cell} the scoped CSS rule turns into the scroll box.
 * 	<li><b>Nested</b> &mdash; a nested table inside a row-detail panel's own scrolling
 * 		{@code .juneau-view-detail-nested} wrapper.
 * </ul>
 *
 * <h5 class='section'>Mechanical, never visual:</h5>
 * <p>
 * "Not clipped" is asserted as mechanism, so it cannot rot into a judgement call.  For each case the layer must be
 * {@code position: fixed}, parented to {@code document.body}, outside the scroll box, and have <b>no clipping
 * ancestor at all</b> &mdash; that last one is stronger than "outside that box", because it also catches a newly
 * introduced clipping ancestor somewhere else in the lineage.  Each case additionally proves its box really is a
 * clipping, actually-overflowing region, so a fixture that silently stopped scrolling cannot pass vacuously.
 *
 * <h5 class='section'>Scrolling RETAINS the layer:</h5>
 * <p>
 * Scrolling the region underneath an open layer neither repositions nor dismisses it &mdash; the layer keeps the same
 * viewport rect and stays in the document.  This matches the shipped {@code h4} menu behavior, and stating it as an
 * assertion is what stops a future "reposition on scroll" change from landing silently.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code table-clip-free-browser.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change
 * is needed.
 */
@EnabledIfSystemProperty(named=TableClipFree_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class TableClipFree_BrowserTest extends TestBase {

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
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent()
			.resolve("table-clip-free-browser.cjs");

		// The CSS matters here: the DT1/DT2/nested scroll boxes and the timestamp popup's position:fixed all come
		// from the real stylesheet, so a fixture without it would prove nothing about the shipped contract.
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.VIEWS_CSS_RESOURCE)
			+ "\n</style></head><body>\n<script>\n"
			+ resource(ViewsMixin.RENDERS_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("table-clip-free.html");
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
		var stdout = dir.resolve("table-clip-free-stdout.json");
		var stderr = dir.resolve("table-clip-free-stderr.txt");
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
	private static Map<String,Object> sub(String key) {
		var m = (Map<String,Object>) report.get(key);
		assertNotNull(m, () -> "the prober produced no `" + key + "` case: " + report);
		return m;
	}

	/**
	 * The one shared body of assertions, applied to every cell in the matrix.  Kept as a single helper on purpose:
	 * the contract is identical for every surface and every scroll boundary, and writing it once is what makes that
	 * sameness checkable rather than aspirational.
	 */
	private static void assertClipFree(String caseName) {
		var m = sub(caseName);
		assertEquals(Boolean.TRUE, m.get("opened"), () -> caseName + ": the surface never opened: " + m);
		// The fixture is honest: a real clipping box that really overflows.
		assertEquals(Boolean.TRUE, m.get("boxClips"), () -> caseName + ": the scroll box does not clip: " + m);
		assertEquals(Boolean.TRUE, m.get("boxActuallyScrolled"),
			() -> caseName + ": the scroll box is not actually overflowing, so it proves nothing: " + m);
		// h4 C: portalled to the body as position:fixed.
		assertEquals(Boolean.TRUE, m.get("onBody"), () -> caseName + ": the layer is not parented to <body>: " + m);
		assertEquals(Boolean.TRUE, m.get("positionFixed"), () -> caseName + ": the layer is not position:fixed: " + m);
		assertEquals(Boolean.TRUE, m.get("escapedScrollBox"),
			() -> caseName + ": the layer is still a descendant of the scroll box: " + m);
		assertEquals(List.of(), m.get("clippingAncestors"),
			() -> caseName + ": the layer has a clipping ancestor, so its overflow can still be cut: " + m);
		assertEquals(Boolean.TRUE, m.get("hasArea"), () -> caseName + ": the layer has no painted area: " + m);
		// Scrolling the region RETAINS the layer - it neither moves nor disappears.
		assertEquals(Boolean.TRUE, m.get("retainedInDom"), () -> caseName + ": scrolling dismissed the layer: " + m);
		assertEquals(Boolean.TRUE, m.get("retainedVisible"), () -> caseName + ": scrolling hid the layer: " + m);
		assertEquals(Boolean.TRUE, m.get("notRepositioned"),
			() -> caseName + ": scrolling repositioned the layer; the contract is that it is RETAINED: " + m);
	}

	@Test void a01_runtimeLoadedWithoutErrors() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not populate JuneauViews.init: " + report);
		assertEquals(Boolean.TRUE, report.get("hasRenders"), () -> "juneau-renders.js did not load: " + report);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// DT1 - the JS-inserted .juneau-view-table-scroll wrap.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_dt1_menuFromHalfScrolledCell() { assertClipFree("dt1Menu"); }
	@Test void b02_dt1_popoverFromHalfScrolledCell() { assertClipFree("dt1Popover"); }
	@Test void b03_dt1_timestampFromHalfScrolledCell() { assertClipFree("dt1Timestamp"); }

	//------------------------------------------------------------------------------------------------------------------
	// DT2 - the scoped flex .dt-layout-cell scroll box.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_dt2_menuFromHalfScrolledCell() { assertClipFree("dt2Menu"); }
	@Test void c02_dt2_popoverFromHalfScrolledCell() { assertClipFree("dt2Popover"); }
	@Test void c03_dt2_timestampFromHalfScrolledCell() { assertClipFree("dt2Timestamp"); }

	//------------------------------------------------------------------------------------------------------------------
	// Nested - a table inside a row-detail panel whose .juneau-view-detail-nested wrapper scrolls independently.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_nested_menuFromHalfScrolledCell() { assertClipFree("nestedMenu"); }
	@Test void d02_nested_popoverFromHalfScrolledCell() { assertClipFree("nestedPopover"); }
	@Test void d03_nested_timestampFromHalfScrolledCell() { assertClipFree("nestedTimestamp"); }

	/**
	 * The clip default must not maim the row-detail panel.  DataTables' native child row wraps the panel in a plain
	 * {@code <td colspan>}, and that cell is a descendant of the {@code .juneau-view-table} &mdash; so without an
	 * explicit opt-out the table's own {@code overflow:hidden} / {@code nowrap} / {@code max-width} would flatten a
	 * whole expanded detail panel into a single ellipsised line.
	 */
	@Test void e01_expandedDetailPanelCellIsNotClipped() {
		var m = sub("detailPanelCell");
		assertEquals(Boolean.TRUE, m.get("expanded"), () -> "the detail row never expanded: " + m);
		// The premise: the panel really is hosted by a <td> inside a clip-stamped table.
		assertEquals(Boolean.TRUE, m.get("hostIsTd"), () -> "the panel is not hosted by a <td>: " + m);
		assertEquals(Boolean.TRUE, m.get("insideViewTable"),
			() -> "the host cell is not inside a .juneau-view-table, so this proves nothing: " + m);
		assertEquals(Boolean.TRUE, m.get("wraps"), () -> "the detail panel's cell is nowrap - the panel is flattened: " + m);
		assertEquals(Boolean.TRUE, m.get("notOverflowHidden"), () -> "the detail panel's cell clips its overflow: " + m);
		assertEquals(Boolean.TRUE, m.get("noMaxWidthCap"), () -> "the detail panel's cell is width-capped: " + m);
		assertEquals(Boolean.TRUE, m.get("panelHasArea"), () -> "the detail panel has no height: " + m);
	}
}
