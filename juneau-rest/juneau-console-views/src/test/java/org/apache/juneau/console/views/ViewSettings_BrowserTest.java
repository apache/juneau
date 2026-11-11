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
import static org.apache.juneau.bct.BctAssertions.*;
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
 * The View Settings dialog in a real browser, with the stylesheets in production order (chrome, theme, views,
 * config): folder tabs View/Search/Sort/Options, the saved-views bar between the header and the tabs, the footer
 * order Reset/Cancel/Apply, the View tab's two panes and six-column grid, the stacked three-column layout at 400px,
 * drag reorder, Find, select-versus-add on the Available list, a restricted-tab dialog, and Rename's restore when
 * deleting the old name fails.  Each palette and scheme is also opened once, and the dialog captured on every tab
 * (plus 400px) into {@code <juneau.jsTests.dir>/view-settings-shots}.
 */
@EnabledIfSystemProperty(named=ViewSettings_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-console-views/pom.xml test`")
@SuppressWarnings({
	"unchecked" // Browser report values are cast to Map<String,Object>
})
class ViewSettings_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static final List<String> PALETTES = List.of("open", "gray", "light-brown", "light-red", "red");
	private static final List<String> SCHEMES = List.of("light", "dark");

	private static Map<String,Object> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String style(String css) {
		return "<style>\n" + css + "\n</style>\n";
	}

	private static String script(String js) {
		return "<script>\n" + js + "\n</script>\n";
	}

	private static String page(String palette, String scheme) throws IOException {
		var sb = new StringBuilder("<!DOCTYPE html>\n<html lang=\"en\" data-jc-scheme=\"" + scheme + "\" data-jc-scheme-pin=\"" + scheme
			+ "\"><head><meta charset=\"utf-8\">\n");
		// Production order: chrome, then the theme (shared dark layer, palette), then juneau-views.css, then juneau-config.css.
		sb.append(style(ConsoleBrowserFixture.servedChromeCss()))
			.append(style(resource("/org/apache/juneau/console/juneau-theme-dark.css")))
			.append(style(resource("/org/apache/juneau/console/juneau-theme-" + palette + ".css")))
			.append(style(resource(ViewsMixin.VIEWS_CSS_RESOURCE)))
			.append(style(resource(ViewsMixin.CONFIG_CSS_RESOURCE)))
			.append("</head><body>\n<div id=\"host\"></div>\n");
		for (var r : List.of(ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE,
				ViewsMixin.URLSTATE_JS_RESOURCE, ViewsMixin.SEARCH_JS_RESOURCE, ViewsMixin.PAGESTATE_JS_RESOURCE, ViewsMixin.CONFIG_JS_RESOURCE))
			sb.append(script(resource(r)));
		return sb.append("</body></html>\n").toString();
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("view-settings-browser.cjs");
		var fixtures = Files.createDirectories(dir.resolve("fixtures").resolve("view-settings"));
		for (var p : PALETTES)
			for (var s : SCHEMES)
				Files.writeString(fixtures.resolve(p + "-" + s + ".html"), page(p, s), UTF_8);
		report = Json.to(run(dir, harness, fixtures, dir.resolve("view-settings-shots")), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String run(Path dir, Path harness, Path fixtures, Path shots) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixtures.toString(), shots.toString());
		var stdout = dir.resolve("view-settings-stdout.json");
		var stderr = dir.resolve("view-settings-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));
		var p = pb.start();
		if (!p.waitFor(5, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 5m; stderr:\n" + quietRead(stderr));
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

	private static Map<String,Object> obj(Map<String,Object> m, String key) {
		var v = (Map<String,Object>)m.get(key);
		assertNotNull(v, () -> "no '" + key + "' in " + m);
		return v;
	}

	private static Map<String,Object> wide() { return obj(report, "wide"); }
	private static Map<String,Object> wideLayout() { return obj(wide(), "layout"); }
	private static Map<String,Object> narrowLayout() { return obj(obj(report, "narrow"), "layout"); }

	private static double num(Map<String,Object> m, String key) {
		return ((Number)m.get(key)).doubleValue();
	}

	@Test void a01_noPageErrors() {
		assertEquals(List.of(), wide().get("errors"));
		assertEquals(List.of(), obj(report, "narrow").get("errors"));
		assertEquals(List.of(), obj(report, "restricted").get("errors"));
		obj(report, "pages").forEach((k, v) -> assertEquals(List.of(), ((Map<String,Object>)v).get("errors"), k));
	}

	@Test void a02_folderTabsInOrder() {
		var l = wideLayout();
		assertEquals(List.of("View", "Search", "Sort", "Options"), l.get("tabs"));
		assertEquals("folder", l.get("nav"));
		obj(report, "pages").forEach((k, v) -> assertEquals("folder", ((Map<String,Object>)v).get("nav"), k));
	}

	@Test void a03_savedViewsBarSitsBetweenHeaderAndTabs() {
		var l = wideLayout();
		assertEquals(List.of("juneau-config-saved-bar", "juneau-config-tabs", "juneau-config-bodies", "juneau-config-status"), l.get("hostChildren"));
		assertEquals(true, l.get("headerBeforeBar"));
		assertEquals(List.of("Save as…", "Rename", "Delete"), l.get("barButtons"));
		assertEquals(true, l.get("deleteDanger"));
		// The Default view is active: it can be neither renamed nor deleted.
		assertEquals(true, l.get("renameDisabled"));
		assertEquals(true, l.get("deleteDisabled"));
	}

	@Test void c01_dragFromAvailableAndDetailStripAndHeaderIcon() {
		var c = obj(report, "chooserLight");
		assertBean(c, "findPlaceholder,detailDefault,resetIsLink", "Find...,Click a column to see details.,false");
		assertEquals(List.of("name", "created", "status", "owner"), c.get("afterRowDrop"), c::toString);
		assertEquals(List.of("name", "created", "status", "owner", "priority"), c.get("afterListDrop"), c::toString);
		assertEquals(List.of("region"), c.get("availAfterDrops"), c::toString);
		assertTrue(String.valueOf(c.get("detailAvail")).startsWith("Region"), c::toString);
		assertTrue(String.valueOf(c.get("detailSelected")).startsWith("Name"), c::toString);
	}

	@Test void c02_selectedTabJoinsDialogWithAccentBar() {
		var t = obj(obj(report, "chooserLight"), "tabs");
		assertNotEquals(t.get("selBg"), t.get("unBg"), t::toString);
		assertEquals(t.get("dlgBg"), t.get("selBg"), t::toString);
		assertTrue(String.valueOf(t.get("selBar")).contains("0px 3px 0px 0px inset"), t::toString);
	}

	@Test void c03_selectedTabJoinsDialogWithAccentBar_dark() {
		var t = obj(obj(report, "chooserDark"), "tabs");
		assertNotEquals(t.get("selBg"), t.get("unBg"), t::toString);
		assertEquals(t.get("dlgBg"), t.get("selBg"), t::toString);
		assertTrue(String.valueOf(t.get("selBar")).contains("0px 3px 0px 0px inset"), t::toString);
	}

	@Test void a04_footerIsResetThenCancelThenApply() {
		var l = wideLayout();
		assertEquals(List.of("Reset to defaults", "Cancel", "Apply"), l.get("footer"));
		assertEquals(false, l.get("resetLink"));
		// Reset is a bordered button that sits with Cancel and Apply on the right.
		assertTrue(num(l, "resetToCancel") < 20, () -> "Reset not adjacent to Cancel: " + l);
		assertTrue(num(l, "cancelToApply") < 20, () -> "Cancel/Apply not adjacent: " + l);
		assertTrue(num(l, "applyRightGap") < 20, () -> "Apply not at the right edge: " + l);
		// The standard kinds win inside the dialog: Apply is primary, Reset and Cancel secondary.
		var f = obj(l, "faces");
		assertEquals(f.get("primaryBg"), f.get("applyBg"), f::toString);
		assertEquals(f.get("onPrimary"), f.get("applyColor"), f::toString);
		assertEquals(f.get("controlBg"), f.get("resetBg"), f::toString);
		assertEquals(f.get("controlBg"), f.get("cancelBg"), f::toString);
	}

	@Test void a05_viewTabPanesAndSixColumnGrid() {
		var l = wideLayout();
		assertEquals("row", l.get("panesDirection"));
		var avail = obj(l, "availPane");
		var sel = obj(l, "selPane");
		assertTrue(num(avail, "right") <= num(sel, "left"), "Available pane sits left of the selected grid");
		var ratio = num(avail, "width") / num(sel, "width");
		assertEquals(0.7 / 1.6, ratio, 0.7 / 1.6 * 0.1, () -> "pane ratio " + ratio);
		var cols = String.valueOf(l.get("rowColumns")).split(" ");
		assertEquals(6, cols.length, () -> "row grid " + l.get("rowColumns"));
		assertEquals("20px", cols[0]);
		assertEquals("130px", cols[1]);
		assertEquals("112px", cols[3]);
		assertEquals("24px", cols[5]);
		assertEquals(l.get("rowColumns"), l.get("headColumns"));
		assertEquals("uppercase", l.get("headTransform"));
	}

	@Test void a06_stackedThreeColumnGridAt400px() {
		var l = narrowLayout();
		assertEquals(400.0, num(l, "viewportWidth"));
		assertTrue(num(l, "dialogWidth") <= 400, () -> "dialog wider than the viewport: " + l);
		assertEquals("column", l.get("panesDirection"));
		var cols = String.valueOf(l.get("rowColumns")).split(" ");
		assertEquals(3, cols.length, () -> "row grid " + l.get("rowColumns"));
		assertEquals("20px", cols[0]);
		assertEquals("24px", cols[2]);
		assertTrue(num(l, "dialogOverflowX") <= 0, () -> "dialog scrolls sideways: " + l);
	}

	@Test void a07_dragReordersTheShownColumns() {
		var b = obj(wide(), "behaviour");
		assertEquals(List.of("name", "status", "owner"), b.get("initialShown"));
		assertEquals(List.of("created", "priority", "region"), b.get("initialAvailable"));
		assertEquals(List.of("owner", "name", "status"), b.get("afterDrag"));
	}

	@Test void a08_findFiltersTheAvailableList() {
		var b = obj(wide(), "behaviour");
		assertEquals(List.of("priority"), b.get("findPri"));
		assertEquals("No matching columns.", b.get("findNone"));
		assertEquals(List.of("created", "priority", "region"), b.get("findCleared"));
	}

	@Test void a09_singleClickSelects_doubleClickAndEnterAdd() {
		var b = obj(wide(), "behaviour");
		assertEquals("true", b.get("clickSelected"));
		assertEquals(List.of("owner", "name", "status"), b.get("clickShown"));
		assertEquals(List.of("owner", "name", "status", "created"), b.get("dblclickShown"));
		assertEquals(List.of("owner", "name", "status", "created", "priority"), b.get("enterShown"));
		assertEquals(List.of("region"), b.get("enterAvailable"));
	}

	@Test void a10_saveAsIsAnInlineField() {
		var r = obj(wide(), "rename");
		assertEquals(true, r.get("saveAsFieldOpen"));
		assertEquals(true, r.get("saveAsFieldClosed"));
		var s = obj(r, "afterSaveAs");
		assertEquals(List.of("Mine"), s.get("views"));
		assertEquals("Mine", s.get("active"));
		assertEquals(false, s.get("renameDisabled"));
		assertEquals(false, s.get("deleteDisabled"));
	}

	@Test void a11_renameRestoresTheOriginalWhenDeletingTheOldNameFails() {
		var r = obj(wide(), "rename");
		assertEquals("Mine", r.get("renameFieldValue"));
		assertEquals("Rename", r.get("renameOkLabel"));
		assertEquals("Rename failed: storage is locked", r.get("failedStatus"));
		var s = obj(r, "afterFailedRename");
		assertEquals(List.of("Mine"), s.get("views"));
		assertEquals("Mine", s.get("active"));
		assertEquals("Mine", s.get("select"));
	}

	@Test void a12_renameGoesThroughWhenDeleteWorks() {
		var r = obj(wide(), "rename");
		assertEquals("Renamed.", r.get("okStatus"));
		var s = obj(r, "afterRename");
		assertEquals(List.of("Theirs"), s.get("views"));
		assertEquals("Theirs", s.get("active"));
		assertEquals("Theirs", s.get("select"));
	}

	@Test void a13_restrictedTabsKeepTheFixedOrderAndTheSavedBar() {
		var r = obj(report, "restricted");
		assertEquals(List.of("Search", "Sort"), r.get("tabs"));
		assertEquals("juneau-config-saved-bar", ((List<?>)r.get("hostChildren")).get(0));
	}
}
