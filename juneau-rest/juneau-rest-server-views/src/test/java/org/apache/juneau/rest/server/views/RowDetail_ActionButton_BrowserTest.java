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
 * Opt-in Chromium layout canary for the Detail View action button treatment: is the {@code PRIMARY} button
 * actually distinguishable from a default {@code SECONDARY} one, and is the disabled state actually distinct
 * from the enabled one, once real CSS resolves against the real served stylesheet.
 *
 * <p>
 * A fake DOM can prove the right class lands on the right button ({@link ViewTable_RowDetail_Emit_Test}) but
 * cannot prove the two classes paint two different colours, or that a disabled solid-fill button reads as
 * disabled &mdash; both require a real cascade.  The markup comes from the real {@link ViewTable} emitter and
 * the stylesheet is the real served {@code juneau-views.css}, so neither can drift from what a consumer gets.
 *
 * <p>
 * Disabled unless the {@value #GATE} system property is set (the module's {@code js-tests} Maven profile).
 */
@EnabledIfSystemProperty(named=RowDetail_ActionButton_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class RowDetail_ActionButton_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** A header action bar carrying one PRIMARY button, one default-SECONDARY button, and a SafeAction. */
	private static ViewDef view() {
		return ViewDef.create("alerts")
			.dataMode(ViewDef.DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.rowActions(
				RowAction.create("ack").label("Acknowledge").endpoint("/data/alerts/{id}/ack").method(RowAction.Method.POST),
				RowAction.create("esc").label("Escalate").endpoint("/data/alerts/{id}/esc").method(RowAction.Method.POST))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.headerActions(ActionBar.create().items(
					ActionRef.of("ack").emphasis(ActionRef.Emphasis.PRIMARY),
					ActionRef.of("esc"),
					SafeAction.COLLAPSE))
				.sections(DetailSection.create("s", "S").fields(DetailField.of("title").title("Title"))))
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
			.resolve("detail-action-button-browser.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.VIEWS_CSS_RESOURCE)
			+ "\n</style></head><body>\n"
			+ "<div class=\"juneau-view-detail-panel\" id=\"panel\">" + templateInner(view()) + "</div>\n"
			+ "</body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("detail-action-button.html");
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
		var stdout = dir.resolve("detail-action-button-browser-stdout.json");
		var stderr = dir.resolve("detail-action-button-browser-stderr.txt");
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

	@Test void a01_pageLoadedWithNoScriptErrors() {
		assertEquals(List.of(), report.get("jsFailures"), () -> "the fixture logged errors: " + report.get("jsFailures"));
	}

	@Test void a02_primaryIsVisuallyDistinctFromSecondary() {
		// Same base recipe (shape/density), different colour - the whole point of the two classes.
		assertNotEquals(report.get("secondaryBg"), report.get("primaryBg"),
			() -> "primary and secondary must not share a fill colour: " + report);
		assertNotEquals(report.get("secondaryColor"), report.get("primaryColor"),
			() -> "primary and secondary must not share a label colour: " + report);
		// Both known consumers of this shape ("Behavioural target") paint the primary label white.
		assertEquals("rgb(255, 255, 255)", report.get("primaryColor"), report::toString);
	}

	@Test void a03_disabledPrimaryIsVisuallyDistinctFromEnabledPrimary() {
		assertNotEquals(report.get("primaryBg"), report.get("primaryDisabledBg"),
			() -> "a disabled primary button must not paint the same solid fill as an enabled one: " + report);
	}

	@Test void a04_disabledSecondaryIsVisuallyDistinctFromEnabledSecondary() {
		assertNotEquals(report.get("secondaryOpacity"), report.get("secondaryDisabledOpacity"),
			() -> "a disabled secondary button must read differently from an enabled one: " + report);
	}

	@Test void a05_collapseNeverCarriesTheEmphasisClass() {
		assertEquals(Boolean.FALSE, report.get("collapseHasPrimaryClass"), report::toString);
	}

	@Test void a06_noDataAttributeForEmphasis_classOnly() {
		assertEquals(Boolean.FALSE, report.get("anyDataJuneauEmphasisAttr"), report::toString);
	}
}
