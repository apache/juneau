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
 * The nested-popup / layer-stack half of the module's <b>JavaScript-execution harness</b>: runs the REAL served
 * {@code juneau-views.js} in a real headless browser and asserts the layer-stack behavior as a user would
 * experience it &mdash; a dialog opens as a portalled, focus-trapping, z-stamped layer on the body; Escape unwinds
 * exactly ONE layer; an outside click dismisses only a light-dismiss popover and never the modal beneath; a row-action
 * menu is portalled to the body as {@code position:fixed} (so an overflow ancestor cannot clip it); the dialog-kind
 * depth cap is two (a third dialog is a visible refusal inside the current top dialog); and the timestamp popup is a
 * plain show/hide element that never registers as a layer.
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape + Node harness tests):</h5>
 * <p>
 * {@link ViewsJs_PopupLayer_Test} proves the stack's push/pop/Escape/cap behavior under a DOM shim; only a real
 * browser proves the portal reparenting is unclipped, real keydown/pointerdown events route to the top layer, and the
 * focus trap engages.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code nested-popup-browser.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is
 * needed.
 */
@EnabledIfSystemProperty(named=NestedPopup_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class NestedPopup_BrowserTest extends TestBase {

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
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("nested-popup-browser.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("nested-popup.html");
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
		var stdout = dir.resolve("nested-popup-stdout.json");
		var stderr = dir.resolve("nested-popup-stderr.txt");
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
		return (Map<String,Object>) report.get(key);
	}

	@Test void a01_runtimeLoadedWithoutErrors() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not populate JuneauViews.init: " + report);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	@Test void b01_dialogOpensAsPortalledFocusTrappingLayer() {
		var d = sub("dialogLayer");
		assertEquals(Boolean.TRUE, d.get("onBody"), () -> "dialog backdrop was not portalled to body: " + report);
		assertEquals(Boolean.TRUE, d.get("positionFixed"), () -> report.toString());
		assertEquals(Boolean.TRUE, d.get("hasZIndex"), () -> report.toString());
		assertEquals(Boolean.TRUE, d.get("dataLayer0"), () -> report.toString());
		assertEquals(Boolean.TRUE, d.get("focusTrapped"), () -> "focus did not move into the dialog: " + report);
	}

	@Test void b02_escapeUnwindsExactlyOneLayer() {
		var e = sub("escape");
		assertEquals(2L, ((Number) e.get("before")).longValue(), () -> report.toString());
		assertEquals(1L, ((Number) e.get("after")).longValue(), () -> "Escape did not pop exactly one layer: " + report);
		assertEquals(Boolean.TRUE, e.get("innerDetached"), () -> "the inner dialog was not removed: " + report);
		assertEquals(Boolean.TRUE, e.get("outerStays"), () -> "Escape removed the outer dialog too: " + report);
	}

	@Test void c01_outsideClickDismissesOnlyTheLightDismissPopover() {
		var o = sub("outsideClick");
		assertEquals(Boolean.TRUE, o.get("popoverDismissed"), () -> "the light-dismiss popover was not dismissed: " + report);
		assertEquals(Boolean.TRUE, o.get("modalSurvives"), () -> "an outside click dismissed the modal beneath: " + report);
		assertEquals(Boolean.TRUE, o.get("topIsDialogAfter"), () -> report.toString());
	}

	@Test void d01_rowActionMenuIsPortalledFixedAndClosesOnOutsideClick() {
		var m = sub("menu");
		assertEquals(Boolean.TRUE, m.get("opened"), () -> "the row-action menu did not open: " + report);
		assertEquals(Boolean.TRUE, m.get("onBody"), () -> "the menu was not portalled to body (would be clipped): " + report);
		assertEquals(Boolean.TRUE, m.get("positionFixed"), () -> "the menu is not position:fixed: " + report);
		assertEquals(Boolean.TRUE, m.get("closedOnOutsideClick"), () -> report.toString());
	}

	@Test void e01_dialogDepthCapIsTwoAndThirdIsRefusedInsideTopDialog() {
		var d = sub("depthCap");
		assertEquals(2L, ((Number) d.get("max")).longValue(), () -> report.toString());
		assertEquals(2L, ((Number) d.get("atCap")).longValue(), () -> report.toString());
		assertEquals(2L, ((Number) d.get("afterThirdPush")).longValue(), () -> "a third dialog exceeded the cap: " + report);
		assertEquals(Boolean.TRUE, d.get("refusalInTopDialog"), () -> "the depth refusal was not painted into the top dialog: " + report);
	}

	@Test void f01_timestampPopupIsNotARegisteredLayer() {
		var t = sub("timestamp");
		assertEquals(Boolean.TRUE, t.get("notOnStack"), () -> "the timestamp popup registered as a layer: " + report);
		assertEquals(Boolean.TRUE, t.get("dialogCountZero"), () -> report.toString());
	}

	@Test void g01_lastColumnMenuInScrolledOverflowBoxIsNotClipped() {
		// The concrete clip case: a menu opened from a last-column trigger inside a scrolled-right .dt-layout-cell
		// overflow box must escape that box (portalled to body, position:fixed) and land within the viewport, so the
		// box's overflow cannot clip it.
		var m = sub("scrolledMenu");
		assertEquals(Boolean.TRUE, m.get("opened"), () -> "the last-column row-action menu did not open: " + report);
		assertEquals(Boolean.TRUE, m.get("onBody"), () -> "the menu was not portalled to body: " + report);
		assertEquals(Boolean.TRUE, m.get("escapedScrollBox"), () -> "the menu is still inside the .dt-layout-cell overflow box (clipped): " + report);
		assertEquals(Boolean.TRUE, m.get("positionFixed"), () -> "the menu is not position:fixed: " + report);
		assertEquals(Boolean.TRUE, m.get("withinViewportX"), () -> "the menu extends outside the viewport horizontally: " + report);
		assertEquals(Boolean.TRUE, m.get("withinViewportY"), () -> "the menu extends outside the viewport vertically: " + report);
	}
}
