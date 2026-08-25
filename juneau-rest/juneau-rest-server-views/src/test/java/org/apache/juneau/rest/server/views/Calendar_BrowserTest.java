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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.widgets.*;
import org.apache.juneau.rest.server.widgets.EventCategory.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The calendar half of the module's <b>JavaScript-execution harness</b>: runs the REAL served
 * {@code juneau-views.js} + {@code juneau-calendar.js} over a REAL server-painted {@link CalendarTable} in a headless
 * browser, and asserts the three behaviors only a browser can prove &mdash; the legend category toggles are
 * keyboard-reachable and operable with {@code aria-pressed} tracking the filter; a spanning bar's {@code href} and
 * tooltip work from <b>any</b> of its segments; and the {@code "+N more"} popover is a real layer on the ONE shared
 * stack (portalled, z-stamped, Escape pops it, focus returns to the trigger).
 *
 * <h5 class='section'>Load order is part of the fixture, not an accident:</h5>
 * <p>
 * The page loads {@code juneau-views.js} <b>before</b> {@code juneau-calendar.js} and records, in an inline script
 * between the two tags, that the shared stack was already published.  That is the rec-F load-order contract stated in
 * {@link ViewsMixin#CALENDAR_JS_PATH}: the calendar defines no stack of its own, so views must come first.
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape + Node harness tests):</h5>
 * <p>
 * {@link CalendarJs_Test} proves segmentation, lane seating, filtering and the popover contract under a DOM shim;
 * only a real browser proves a real Enter keypress on a real {@code <button>} drives the filter, that a bar segment
 * is a genuinely clickable same-origin anchor, and that the popover truly participates in the shared stack's
 * z-order and focus return.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code calendar-browser.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is
 * needed.
 */
@EnabledIfSystemProperty(named=Calendar_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class Calendar_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	/** A fixed August-2026 clock so the grid, the week-boundary cut and the stamped today are deterministic. */
	private static final Clock AUG_2026 = Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC);

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/**
	 * A seed-only August 2026: one all-day span cut by the Sat/Sun week boundary (Thu Aug 6 &rarr; Tue Aug 11, so it
	 * emits two segments), and four single-day review chips on Aug 12 against the default {@code maxPerDay} of 3, so
	 * exactly one chip is hidden behind a {@code "+1 more"}.
	 */
	private static CalendarDef seed() {
		return CalendarDef.create()
			.id("cal")
			.categories(
				EventCategory.create().id("team").label("Team").color(CategoryColor.BLUE),
				EventCategory.create().id("review").label("Review").color(CategoryColor.GREEN))
			.initial(2026, 8)
			.events(
				CalendarEvent.create().id("span1").title("Long haul").start("2026-08-06").end("2026-08-11")
					.categoryId("team").href("/events/span1").tooltip("Long haul, Aug 6-11"),
				CalendarEvent.create().id("c1").title("Chip one").start("2026-08-12").categoryId("review"),
				CalendarEvent.create().id("c2").title("Chip two").start("2026-08-12").categoryId("review"),
				CalendarEvent.create().id("c3").title("Chip three").start("2026-08-12").categoryId("review"),
				CalendarEvent.create().id("c4").title("Chip four").start("2026-08-12").categoryId("review"));
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("calendar-browser.cjs");

		// juneau-views.js FIRST, then a witness of the shared stack, then juneau-calendar.js: the load-order contract.
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.CALENDAR_CSS_RESOURCE)
			+ "\n</style></head><body>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script>\n<script>\nwindow.__viewsBeforeCalendar = !!(window.JuneauViews && window.JuneauViews.init"
			+ " && typeof window.JuneauViews.init.pushLayer === \"function\");\n</script>\n<script>\n"
			+ resource(ViewsMixin.CALENDAR_JS_RESOURCE)
			+ "\n</script>\n"
			+ Html.of(CalendarTable.of(seed(), AUG_2026))
			+ "\n</body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("calendar.html");
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
		var stdout = dir.resolve("calendar-stdout.json");
		var stderr = dir.resolve("calendar-stderr.txt");
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

	private static long num(Map<String,Object> m, String key) {
		return ((Number) m.get(key)).longValue();
	}

	@Test void a01_bothRuntimesLoaded_viewsFirst_withoutErrors() {
		assertEquals(Boolean.TRUE, report.get("hasViews"), () -> "juneau-views.js did not publish JuneauViews: " + report);
		assertEquals(Boolean.TRUE, report.get("hasCalendar"), () -> "juneau-calendar.js did not publish JuneauCalendar: " + report);
		assertEquals(Boolean.TRUE, report.get("viewsBeforeCalendar"),
			() -> "the shared layer stack must exist BEFORE juneau-calendar.js runs: " + report);
		assertEquals(CalendarDef.CONTRACT_VERSION, report.get("contract"), () -> report.toString());
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	@Test void b01_legendToggleIsKeyboardReachableAndOperable_ariaPressedTracksTheFilter() {
		var l = sub("legend");
		assertEquals(Boolean.TRUE, l.get("reachable"), () -> "the legend toggle did not take focus: " + report);
		assertEquals(Boolean.TRUE, l.get("tabbable"), () -> "the legend toggle is not in the tab order: " + report);
		assertEquals("true", l.get("pressedInitially"), () -> report.toString());
		assertEquals("false", l.get("pressedAfterEnter"), () -> "Enter did not un-press the toggle: " + report);
		assertEquals("true", l.get("pressedAfterSecondEnter"), () -> "Enter did not re-press the toggle: " + report);
	}

	@Test void b02_legendToggleHidesAndRevealsThatCategoryOnly() {
		var l = sub("legend");
		assertEquals(3L, num(l, "chipsBefore"), () -> "expected 3 painted review chips (4 events, maxPerDay 3): " + report);
		assertEquals(0L, num(l, "chipsWhileHidden"), () -> "the filtered category still paints chips: " + report);
		assertEquals(3L, num(l, "chipsAfterReveal"), () -> "the category did not come back: " + report);
		// The team span is a different category, so a review-only filter must leave both of its segments alone.
		assertEquals(2L, num(l, "barsWhileHidden"), () -> "filtering review disturbed the team span: " + report);
	}

	@Test void c01_spanningBarIsClickableAndTooltippedFromAnySegment() {
		var b = sub("bar");
		assertEquals(2L, num(b, "segments"), () -> "the Thu-to-Tue span must be cut into two week-row segments: " + report);
		assertEquals(Boolean.TRUE, b.get("allAnchors"), () -> "a bar segment is not a real anchor: " + report);
		assertEquals(List.of("/events/span1", "/events/span1"), b.get("hrefs"),
			() -> "every segment must carry the whole event's href: " + report);
		assertEquals(List.of("Long haul, Aug 6-11", "Long haul, Aug 6-11"), b.get("tooltips"),
			() -> "every segment must carry the whole event's tooltip: " + report);
		// The cut edges are flagged: the first piece continues right, the second continues left.
		assertEquals(List.of("r", "l"), b.get("continuations"), () -> report.toString());
	}

	@Test void d01_morePopoverIsALayerOnTheSharedStack() {
		var p = sub("popover");
		assertEquals(Boolean.TRUE, p.get("opened"), () -> "the '+N more' popover did not open: " + report);
		assertEquals(Boolean.TRUE, p.get("onSharedStack"), () -> "the popover is not the shared stack's top layer: " + report);
		assertEquals("popover", p.get("kind"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("lightDismiss"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("portalledToBody"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("positionFixed"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("hasZIndex"), () -> "the popover got no stack z-order: " + report);
		assertEquals("0", p.get("dataLayer"), () -> report.toString());
		assertEquals("true", p.get("triggerExpanded"), () -> report.toString());
	}

	@Test void d02_escapePopsThePopoverAndRestoresFocusToTheTrigger() {
		var e = sub("escape");
		assertEquals(Boolean.TRUE, e.get("detached"), () -> "Escape did not remove the popover: " + report);
		assertEquals(Boolean.TRUE, e.get("stackEmpty"), () -> "the popover layer outlived its element: " + report);
		assertEquals(Boolean.TRUE, e.get("focusRestored"), () -> "focus did not return to the '+N more' trigger: " + report);
		assertEquals("false", e.get("triggerCollapsed"), () -> report.toString());
	}
}
