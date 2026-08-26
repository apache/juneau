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
 * Opt-in Chromium layout canary for the detail field grid.
 *
 * <p>
 * Every property here is one a fake DOM cannot evaluate - a container query, a cascade, a {@code ::after}, and an
 * overflow measurement - which is why this class exists alongside the always-on emit coverage in
 * {@link ViewTable_RowDetail_Emit_Test} rather than instead of it.  The markup comes from the real
 * {@link ViewTable} emitter and the stylesheet is the real served {@code juneau-views.css}, so neither can drift
 * from what a consumer gets.
 *
 * <p>
 * <b>The viewport is held at 1200px for the whole run; only the panel's host element is resized.</b>  That is the
 * assertion that distinguishes a container query from a media query: the panel is nested inside a table cell, so
 * its width and the window's are unrelated, and a ladder that only responded to the window would be measuring the
 * wrong thing while looking correct.
 *
 * <p>
 * Disabled unless the {@value #GATE} system property is set (the module's {@code js-tests} Maven profile).
 */
@EnabledIfSystemProperty(named=RowDetail_FieldGrid_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class RowDetail_FieldGrid_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** A four-column INLINE section carrying a FULL-span field, an unset field, and an unbreakable value. */
	private static ViewDef gridView() {
		return ViewDef.create("alerts")
			.dataMode(ViewDef.DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(DetailSection.create("grid", "Grid")
					.columns(4)
					.fields(
						DetailField.of("name").title("Name"),
						DetailField.of("owner").title("Owner"),
						DetailField.of("region").title("Region"),
						DetailField.of("state").title("State"),
						DetailField.of("url").title("Runbook URL"),
						DetailField.of("missing").title("Missing"),
						DetailField.of("summary").title("Summary").span(FieldSpan.FULL))))
			.build();
	}

	private static ViewDef stackedView() {
		return ViewDef.create("notes")
			.dataMode(ViewDef.DataMode.CLIENT)
			.dataUrl("/data/notes")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/notes/{id}")
				.sections(DetailSection.create("body", "Body")
					.columns(1)
					.layout(FieldLayout.STACKED)
					.fields(DetailField.of("note").title("Note"))))
			.build();
	}

	/** The server-painted detail template's contents - what the runtime clones into a panel div. */
	private static String templateInner(ViewDef v) {
		var html = Html.of(ViewTable.of(v));
		var at = html.indexOf(ViewTable.DETAIL_TEMPLATE_ATTR);
		assertTrue(at >= 0, html);
		var open = html.indexOf('>', at);
		var close = html.indexOf("</template>", open);
		assertTrue(close > open, html);
		return html.substring(open + 1, close);
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent()
			.resolve("detail-field-grid-browser.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.VIEWS_CSS_RESOURCE)
			+ "\n</style></head><body>\n"
			+ "<div id=\"host\" style=\"width:1200px\">"
			+ "<div class=\"juneau-view-detail-panel\" id=\"panel\">" + templateInner(gridView()) + "</div>"
			+ "</div>\n"
			+ "<div class=\"juneau-view-detail-panel\" id=\"stacked\" style=\"width:900px\">"
			+ templateInner(stackedView()) + "</div>\n"
			+ "</body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("detail-field-grid.html");
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
		var stdout = dir.resolve("detail-field-grid-browser-stdout.json");
		var stderr = dir.resolve("detail-field-grid-browser-stderr.txt");
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

	private static Map<?,?> at(int hostWidth) {
		var m = (Map<?,?>)report.get("w" + hostWidth);
		assertNotNull(m, () -> "no measurement at host width " + hostWidth + ": " + report);
		return m;
	}

	private static int cols(int hostWidth) {
		return ((Number)at(hostWidth).get("cols")).intValue();
	}

	@Test void a01_pageLoadedWithNoScriptErrors() {
		assertEquals(List.of(), report.get("jsFailures"), () -> "the fixture logged errors: " + report.get("jsFailures"));
	}

	/** The host widths G-1 measured on {@code master}, re-checked in the delivered tree. */
	private static final List<Integer> WIDTHS = List.of(1200, 900, 700, 520, 380);

	@Test void a02_columnsStepDownWithTheContainer_notTheWindow() {
		// One section, declaring columns(4), measured at five host widths behind an unchanged 1200px window.
		assertEquals(4, cols(1200), report::toString);
		assertEquals(3, cols(900), report::toString);
		assertEquals(2, cols(700), report::toString);
		assertEquals(1, cols(520), report::toString);
		assertEquals(1, cols(380), report::toString);
		// columns(4) is a cap: no width may produce a fifth track, and no track may be a degenerate orphan the
		// way an auto-fit repetition raised by a span would be.
		for (var w : WIDTHS) {
			assertTrue(cols(w) <= 4, () -> "a span created a column beyond the declared cap at " + w + ": " + report);
			assertTrue(((Number)at(w).get("narrowestTrackPx")).doubleValue() > 60,
				() -> "degenerate column track at host width " + w + ": " + report);
		}
	}

	@Test void a03_theContainerIsThePanel_soAWideePanelIgnoresANarrowWindow() {
		// Host pinned at 1200px, window squeezed to 400px.  If container-type had been declared on :root this
		// would report 1 and the whole ladder would be a media query in disguise.
		assertEquals(4, ((Number)report.get("colsWithWideHostNarrowWindow")).intValue(),
			() -> "the query container must be .juneau-view-detail-panel, not the viewport: " + report);
	}

	@Test void a04_fullSpanOccupiesEveryRenderedColumn() {
		for (var w : WIDTHS)
			assertEquals(Boolean.TRUE, at(w).get("spanIsFullWidth"),
				() -> "a FULL span must reach the grid's full width at host width " + w + ": " + report);
		// At one column FULL and ONE are the same thing, which is what makes "1 / -1" the only span value that
		// is well-defined without knowing the rendered count.
		assertEquals(Boolean.TRUE, at(380).get("spanEqualsOne"), report::toString);
		assertEquals(Boolean.FALSE, at(1200).get("spanEqualsOne"), report::toString);
	}

	@Test void a05_anUnbreakableValueDoesNotOverflow_atEitherLevel() {
		// On master this measured scrollWidth 684 against clientWidth 620 at a 700px host, and the overflow
		// propagated out through the panel to the host table.
		for (var w : WIDTHS) {
			assertEquals(Boolean.TRUE, at(w).get("noOverflow"),
				() -> "the fields grid overflowed at host width " + w + ": " + report);
			assertEquals(Boolean.TRUE, at(w).get("noPanelOverflow"),
				() -> "the detail panel overflowed at host width " + w + ": " + report);
		}
	}

	@Test void a06_anUnsetValueKeepsItsRow_itsSeparatorAndAnEmDash() {
		var m = at(900);
		assertEquals("solid 1px", m.get("emptySeparator"), report::toString);
		assertTrue(String.valueOf(m.get("emptyAfter")).contains("\u2014"),
			() -> "an unset value must render the generated em-dash: " + report);
		var empty = ((Number)m.get("emptyValueHeight")).doubleValue();
		var filled = ((Number)m.get("filledValueHeight")).doubleValue();
		assertTrue(empty > 0 && Math.abs(empty - filled) <= 1,
			() -> "an unset value slot must measure the same as a filled one (" + empty + " vs " + filled
				+ "); without the derived min-height it contributes no line box and the row rides short: " + report);
	}

	@Test void a07_labelIsSmallerThanValue_andSitsBesideItUnderInline() {
		var m = at(1200);
		var label = ((Number)m.get("labelPx")).doubleValue();
		var value = ((Number)m.get("valuePx")).doubleValue();
		assertTrue(label < value, () -> "the label must be the smaller of the pair: " + report);
		assertEquals(Boolean.TRUE, m.get("labelBesideValue"), report::toString);
		assertEquals(Boolean.TRUE, at(380).get("labelBesideValue"),
			() -> "a one-column grid must not break the INLINE pairing: " + report);
	}

	@Test void a08_stackedPutsTheLabelAboveTheValue() {
		assertEquals(Boolean.TRUE, report.get("stackedLabelAboveValue"), report::toString);
	}
}
