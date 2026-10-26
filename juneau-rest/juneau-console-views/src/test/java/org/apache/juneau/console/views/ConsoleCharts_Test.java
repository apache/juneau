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

import static org.apache.juneau.commons.utils.Shorts.f;
import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

/**
 * The chart-card runtime {@code juneau-charts.js} against the real shell, bus and DataRegistry, with a recording fake
 * {@code echarts}: lifecycle, data, refresh, errors, the registries, events, cmd ops, the theme bridge, a11y and
 * escaping.  Runs when node is on PATH.
 */
class ConsoleCharts_Test extends TestBase {

	private static Map<?,?> report() {
		var r = NodeHarness.report(Map.of("LC_ALL", "en_US.UTF-8", "TZ", "UTC"), "charts.cjs",
			ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.DATA_JS_RESOURCE, ConsoleChromeMixin.CONSOLE_JS_RESOURCE, ConsoleChromeMixin.CHARTS_JS_RESOURCE);
		assumeTrue(r != null, "node (or charts.cjs) not available - JS layer skipped");
		return r;
	}

	private static Map<?,?> part(String key) {
		var s = (Map<?,?>)report().get(key);
		assertNotNull(s, () -> "section '" + key + "' missing from the harness output");
		assertNull(s.get("crash"), () -> "section '" + key + "' crashed: " + s.get("crash"));
		return s;
	}

	// Structural compare (the ViewsJs_BusBridgeSse_Test form): key order and number types do not matter, nulls do.
	private static void assertJson(String expected, Object actual) {
		try {
			assertEquals(Json.to(expected, Object.class), actual, () -> String.valueOf(actual));
		} catch (Exception e) {
			fail("bad expected JSON: " + expected + ": " + e);
		}
	}

	private static int num(Map<?,?> r, String key) {
		return ((Number)r.get(key)).intValue();
	}

	@Test void a01_line_structureAndAccessibility() {
		var r = part("line");
		assertEquals(1, num(r, "figure"));
		assertEquals("Line chart, 2 series, 3 points, Mon to Wed", r.get("caption"));
		// The plot is labelled by the figcaption (read once), focusable for the keyboard walk.
		assertEquals("jc-chart-caption-c1", r.get("captionId"));
		assertEquals(r.get("captionId"), r.get("labelledby"));
		assertEquals("img", r.get("role"));
		assertEquals("0", r.get("tabindex"));
		assertEquals("240px", r.get("canvasHeight"));
		assertJson("[\"juneau-gray\",{\"renderer\":\"canvas\",\"locale\":\"EN\"}]", r.get("init"));
		assertEquals(List.of("placed", "delivered"), r.get("seriesIds"));
		assertEquals(List.of("Placed", "Delivered"), r.get("seriesNames"));
		assertEquals(List.of("juneau"), r.get("datasetIds"));
		assertJson("{\"enabled\":true}", r.get("aria"));
		// A literal "ok" color resolves through --jc-success.
		assertEquals("#2e8540", r.get("okColor"));
		// The first draw replaces everything; replaceMerge is only for redraws.
		assertJson("[{\"notMerge\":true}]", r.get("setOptionOpts"));
		assertEquals(true, r.get("bannerHidden"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a02_line_tableToggleAndKeyboard() {
		var r = part("table");
		// The label is fixed; aria-expanded carries the state.  The table body is built on open, not at the first draw.
		assertEquals(Map.of("expanded", "false", "text", "Show data", "hidden", true, "cells", 0), r.get("before"));
		assertEquals(Map.of("expanded", "true", "text", "Show data", "hidden", false), r.get("after"));
		// The toggle controls the table box; the table is named by the caption.
		assertEquals(Map.of("controls", "jc-chart-table-box-c1", "boxId", "jc-chart-table-box-c1", "tableName", "jc-chart-caption-c1", "captionId", "jc-chart-caption-c1"), r.get("wiring"));
		assertEquals(List.of("day", "placed", "delivered"), r.get("head"));
		assertEquals(List.of("Mon", "3", "1", "Tue", "5", "2", "Wed", "4", "4"), r.get("cells"));
		assertEquals(4, num(r, "rows"));
		assertEquals(true, r.get("noteHidden"));
		// Right, Right, Left: highlight + showTip on the new point, downplay on the old one.
		assertJson("["
			+ "{\"type\":\"highlight\",\"seriesIndex\":0,\"dataIndex\":0},{\"type\":\"showTip\",\"seriesIndex\":0,\"dataIndex\":0},"
			+ "{\"type\":\"downplay\",\"seriesIndex\":0,\"dataIndex\":0},{\"type\":\"highlight\",\"seriesIndex\":0,\"dataIndex\":1},"
			+ "{\"type\":\"showTip\",\"seriesIndex\":0,\"dataIndex\":1},{\"type\":\"downplay\",\"seriesIndex\":0,\"dataIndex\":1},"
			+ "{\"type\":\"highlight\",\"seriesIndex\":0,\"dataIndex\":0},{\"type\":\"showTip\",\"seriesIndex\":0,\"dataIndex\":0}]", r.get("keys"));
	}

	@Test void a03_bar_srcRowsStackedAndHorizontal() {
		var r = part("bar");
		assertEquals(List.of(Map.of("url", "/api/rows", "accept", "application/json")), r.get("fetches"));
		assertEquals(List.of("total", "total"), r.get("stack"));
		assertEquals("category", r.get("yAxis"));
		assertEquals("Bar chart, 2 series, 3 points, Mon to Wed", r.get("caption"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a04_donut_center() {
		var r = part("donut");
		assertEquals("Donut chart, 3 points", r.get("caption"));
		assertEquals("100", r.get("center"));
		assertEquals(List.of(), r.get("errors"));
	}

	@SuppressWarnings({
		"unchecked" // Casts a parsed JSON value to its known Map/List shape.
	})
	@Test void a04b_pieDonut_legendLeavesRoomForLabels() {
		var r = part("pieLegend");
		var donut = (Map<String,Object>)r.get("donut");
		assertEquals(Map.of("bottom", 8, "itemGap", 16), donut.get("legend"));
		assertEquals(List.of("50%", "42%"), donut.get("center"));
		assertEquals(List.of("39%", "60%"), donut.get("radius"));
		assertEquals(List.of("50%", "42%"), donut.get("graphic"));
		// The centre text hangs off a zero-size group on the ring's centre (raw bounds), so the text is centred on the ring.
		var group = (Map<String,Object>)donut.get("group");
		assertEquals("group", group.get("type"));
		assertEquals("raw", group.get("bounding"));
		assertEquals(true, group.get("silent"));
		var kids = (List<List<Object>>)group.get("text");
		assertSize(1, kids);
		assertEquals(List.of("text", "center", "middle"), kids.get(0).subList(0, 3));
		assertNull(kids.get(0).get(3));
		assertNull(kids.get(0).get(4));
		assertEquals("60%", ((Map<String,Object>)r.get("pie")).get("radius"));
		var right = (Map<String,Object>)r.get("right");
		assertEquals(Map.of("right", 0, "orient", "vertical", "itemGap", 16), right.get("legend"));
		assertEquals(List.of("40%", "50%"), right.get("center"));
		assertEquals(Map.of("show", false), ((Map<String,Object>)r.get("none")).get("legend"));
	}

	@Test void a05_cardErrors_reportedOnce() {
		var r = part("errors");
		assertEquals("E-JS-63: chart card 'm' series field 'missing' is not in the data rows", r.get("missing"));
		assertEquals("E-JS-62: chart card 'f' fetch failed: 'Request failed'", r.get("fetch"));
		assertEquals(List.of(
			"[juneau-charts] E-JS-63: chart card 'm' series field 'missing' is not in the data rows",
			"[juneau-charts] E-JS-62: chart card 'f' fetch failed: 'Request failed'"), r.get("errors"));
	}

	@Test void a06_refresh_failureGoesStaleThenRecovers() {
		var r = part("refresh");
		assertEquals(List.of(5000), r.get("ms"));
		assertEquals(1, num(r, "live"));
		assertEquals(List.of(1), r.get("first"));
		assertEquals(List.of(2), r.get("second"));
		// A failed refresh keeps the last picture, marks it stale and keeps polling.
		assertEquals(Map.of("cls", true, "title", "Showing the last data; the refresh failed: E-JS-62: chart card 'p' fetch failed: 'Request failed'"), r.get("stale"));
		assertEquals(true, r.get("recovered"));
		assertEquals(1, num(r, "figure"));
		assertEquals(1, num(r, "instances"));
		assertEquals(List.of("[juneau-charts] E-JS-62: chart card 'p' fetch failed: 'Request failed'"), r.get("log"));
	}

	@Test void a07_refreshMs_clamped() {
		var r = part("clamp");
		assertEquals(List.of(5000), r.get("ms"));
		assertEquals(List.of("[juneau-charts] E-JS-75: 'r2' refreshMs '1000' is below the minimum '5000'; using the minimum"), r.get("log"));
	}

	@Test void a08_lateRegistration_healsCycleAndSourceCards() {
		var r = part("lateRegistration");
		var e90 = "E-JS-90: '%s' references unknown $fn '%s'; registered: 'bytes, compact, datetime, duration, number, percent, symbolSizeBy, template'";
		var w1 = f(e90, "w1", "app.money");
		var sw = f(e90, "sw", "app.money");
		// Unregistered at DOMContentLoaded: E-JS-90 on the card, nothing drawn, for the cycle card and the source card alike.
		assertJson("{\"w1\":{\"error\":" + Json.of(w1) + ",\"drawn\":false},\"sw\":{\"error\":" + Json.of(sw) + ",\"drawn\":false}}", r.get("before"));
		// Registering it afterwards heals both (the source card re-draws from its current handle), with the fn resolved.
		assertJson("{\"w1\":{\"error\":null,\"drawn\":true},\"sw\":{\"error\":null,\"drawn\":true}}", r.get("after"));
		assertJson("{\"w1\":\"function\",\"sw\":\"function\"}", r.get("fns"));
		// A name nobody registers stays an error; a card destroyed while waiting never draws.
		assertJson("{\"error\":" + Json.of(f(e90, "w2", "app.never")) + ",\"drawn\":false}", r.get("w2"));
		assertEquals(false, ((Map<?,?>)r.get("w3")).get("drawn"));
		// Each card is reported exactly once (the source card draws nothing while it waits).
		assertEquals(List.of(
			"[juneau-charts] " + w1,
			"[juneau-charts] " + f(e90, "w2", "app.never"),
			"[juneau-charts] " + f(e90, "w3", "app.money"),
			"[juneau-charts] " + sw), r.get("log"));
	}

	@Test void a09_registries() {
		var r = part("registry");
		assertEquals(true, r.get("alias"));
		assertEquals(List.of("app.one"), r.get("fns"));
		assertEquals(List.of("app.dec"), r.get("decorators"));
		assertEquals(List.of(
			"[juneau-charts] E-JS-97: JuneauCharts.fn.register: name 'bare' is bare; app names are dotted ('app.bare')",
			"[juneau-charts] JuneauConsoleCharts entries are [registry, name, impl] triples; registry is one of fn, decorator, parser, builder, transform"),
			r.get("log"));
	}

	@Test void a10_chartsBeforeShell() {
		var r = part("chartsFirst");
		assertEquals(true, r.get("drawn"));
		assertEquals(true, r.get("alias"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a11_clicks() {
		var r = part("clicks");
		// A meta over 4 KiB is dropped to null; the row is the frame row at dataIndex.
		assertJson("["
			+ "{\"schemaVersion\":1,\"card\":\"k\",\"event\":\"click\",\"seriesId\":\"placed\",\"seriesName\":\"Placed\",\"dataIndex\":1,\"name\":\"Tue\",\"value\":5,"
			+ "\"row\":{\"day\":\"Tue\",\"placed\":5,\"delivered\":2},\"meta\":null},"
			+ "{\"schemaVersion\":1,\"card\":\"k\",\"event\":\"click\",\"seriesId\":\"placed\",\"seriesName\":\"Placed\",\"dataIndex\":2,\"name\":\"Tue\",\"value\":5,"
			+ "\"row\":{\"day\":\"Wed\",\"placed\":4,\"delivered\":4},\"meta\":{\"id\":7}}]", r.get("got"));
		assertEquals(List.of("/orders?day=Tue"), r.get("navigated"));
		// A modifier-click opens a new tab without an opener.
		assertEquals(List.of(List.of("/orders?day=Wed", "_blank", "noopener")), r.get("opened"));
		assertEquals(List.of("[juneau-charts] E-JS-79: chart card 'x' onClick.href resolved to a disallowed URL 'java\tscript:alert(Tue)'"), r.get("log"));
	}

	@Test void a12_escapingAndEmpty() {
		var r = part("escaping");
		assertEquals("<b>bold</b>", r.get("caption"));
		assertEquals(List.of("<i>Mon</i>", "1", "2"), r.get("cells"));
		assertEquals(0, num(r, "bold"));
		assertEquals("Nothing yet.", r.get("empty"));
		// An empty card never inits ECharts.
		assertEquals(true, r.get("zInst"));
	}

	@Test void a13_destroy_disposesAndStopsPolling() {
		var r = part("destroy");
		assertEquals(true, r.get("disposed"));
		assertEquals(0, num(r, "live"));
	}

	@Test void a14_setOptionThrows_isE_JS_80_once() {
		var r = part("setOptionThrows");
		assertEquals("E-JS-80: chart card 'b' setOption failed: 'kaput'", r.get("error"));
		assertEquals(List.of("[juneau-charts] E-JS-80: chart card 'b' setOption failed: 'kaput'"), r.get("log"));
	}

	@Test void a15_zoomAndLegendState_foldedRestoredPublished() {
		var r = part("state");
		assertJson("{\"zoom\":{\"type\":\"inside\",\"start\":10,\"end\":60},\"legend\":{\"Placed\":false}}", r.get("folded"));
		assertJson("{\"zoom\":{\"schemaVersion\":1,\"start\":10,\"end\":60},\"legend\":{\"schemaVersion\":1,\"selected\":{\"Placed\":false}}}", r.get("restored"));
		// Two datazoom events inside the 150 ms debounce publish once, nothing before it elapses (60 ms), the state read when the timer
		// fires (one read, so no second timer ran).
		assertEquals(0, num(r, "early"));
		assertEquals(1, num(r, "reads"));
		assertJson("[{\"schemaVersion\":1,\"start\":21,\"end\":80,\"startValue\":1,\"endValue\":2}]", r.get("seen"));
		assertJson("{\"schemaVersion\":1,\"selected\":{\"Placed\":true,\"Delivered\":false}}", r.get("legend"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a15c_legendOverStateCap_compactThenKeepsLastGood() {
		var r = part("legendOverCap");
		assertJson("[{\"schemaVersion\":1,\"selected\":{\"A\":false,\"B\":true}},{\"schemaVersion\":1,\"selected\":{\"series-name-3\":false,\"series-name-7\":false}}]", r.get("seen"));
		assertJson("{\"schemaVersion\":1,\"selected\":{\"series-name-3\":false,\"series-name-7\":false}}", r.get("last"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a15d_restoredLegendOverStateCap_compactOrDropped() {
		var r = part("legendRestoreOverCap");
		assertJson("{\"folded\":{\"series-name-3\":false,\"series-name-7\":false},\"published\":{\"schemaVersion\":1,\"selected\":{\"series-name-3\":false,\"series-name-7\":false}},\"errors\":[]}", r.get("compact"));
		assertJson("{\"folded\":null,\"published\":null,\"errors\":[]}", r.get("tooBig"));
	}

	@Test void a15e_legendEventTopicOverStateCap_compactOrNull() {
		var r = part("legendEventOverCap");
		assertJson("[{\"A\":false,\"B\":true},{\"series-name-3\":false,\"series-name-7\":false},null]", r.get("seen"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a15b_destroyWithPendingZoomTimer_publishesNothing() {
		var r = part("zoomDestroy");
		assertEquals(0, num(r, "seen"));
		assertEquals(true, r.get("disposed"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a16_cmdOps() {
		var r = part("ops");
		assertJson("[{\"type\":\"highlight\",\"seriesId\":\"placed\",\"dataIndex\":0},{\"type\":\"dataZoom\",\"start\":0,\"end\":50},{\"type\":\"dataZoom\",\"start\":0,\"end\":50}]", r.get("actions"));
		assertEquals(List.of("g", "g"), r.get("connect"));
		assertEquals(List.of("g", "g", ""), r.get("groups"));
		assertJson("[{\"href\":\"data:image/png;base64,RkFLRQ==\",\"download\":\"o3.png\"}]", r.get("downloads"));
		assertEquals(List.of(
			"[juneau-charts] E-JS-103: chart card 'o3' ignored cmd op 'downplay': target 'group' needs a card group",
			"[juneau-charts] E-JS-103: chart card 'o3' ignored cmd op 'showTip': options must be an object",
			"[juneau-charts] E-JS-103: chart card 'o3' ignored cmd op 'export': svg export needs renderer 'svg'"), r.get("log"));
	}

	@Test void a17_themeBridge_rethemesInPlace() {
		var r = part("theme");
		assertEquals(List.of("juneau-light-red"), r.get("registered"));
		assertEquals("#123456", r.get("color0"));
		// juneau:theme-changed registers the theme once more, from the tokens as they are now, for later inits.
		assertEquals(List.of("juneau-light-red", "juneau-light-red"), r.get("registeredAfter"));
		assertEquals("#654321", r.get("color0After"));
		// No card is re-initialised: the token-themed card gets the new theme on its own instance, the custom-themed card is left alone.
		assertJson("["
			+ "[\"juneau-light-red\",{\"renderer\":\"canvas\",\"locale\":\"ZH\"}],[\"custom\",{\"renderer\":\"svg\",\"locale\":\"ZH\"}]]", r.get("inits"));
		assertEquals(0, num(r, "reinit"));
		assertEquals(0, num(r, "disposed"));
		assertEquals(true, r.get("sameInstance"));
		assertJson("{\"t1\":1,\"t2\":0}", r.get("setThemeCalls"));
		assertEquals("#654321", r.get("setThemeColor0"));
		// One redraw re-applies the colours baked into the option; the components that carry them are replaced, not merged.
		assertJson("{\"t1\":1,\"t2\":0}", r.get("setOptionAfter"));
		assertJson("{\"replaceMerge\":[\"series\",\"dataset\",\"graphic\",\"visualMap\"]}", r.get("replaceMerge"));
		// The theme's tooltip sets no renderMode: the chart-wide tooltip mode is the glue's to enforce, not the theme's.
		assertEquals(true, r.get("tooltipRenderMode"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a17b_pendingDrawDefersRetheme() {
		var r = part("themePending");
		assertEquals(List.of(1), r.get("first"));
		// The refresh in flight is neither aborted nor re-run: it paints, and only then is the theme applied.
		assertEquals(0, num(r, "during"));
		assertEquals(1, num(r, "after"));
		assertEquals(List.of("setOption", "setTheme", "setOption"), r.get("order"));
		assertEquals(List.of(2), r.get("final"));
		assertEquals(2, num(r, "fetches"));
		assertEquals(1, num(r, "instances"));
		assertEquals(1, num(r, "live"));
		assertEquals(false, r.get("stale"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a17c_ec5FallsBackToDispose() {
		var r = part("themeEc5");
		assertEquals(1, num(r, "disposed"));
		assertEquals(1, num(r, "reinit"));
		assertEquals(List.of("juneau-gray"), r.get("reinitTheme"));
		assertEquals(true, r.get("customKept"));
		assertEquals(List.of(3, 5, 4), r.get("drawn"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a17d_themeNameSkipsDark() {
		var r = part("themeNameSkipsDark");
		assertEquals(List.of("juneau-light-red"), r.get("registered"));
		assertEquals("juneau-light-red", r.get("init"));
		assertEquals(List.of("juneau-open"), r.get("darkOnly"));
	}

	@Test void a17e_tonesPreferTextRole() {
		var r = part("tonesText");
		assertEquals("#f07a75", r.get("withText"));
		assertEquals("#aa1111", r.get("fillOnly"));
		assertEquals("#5cc275", r.get("okWithText"));
		assertEquals("#11aa11", r.get("okFillOnly"));
	}

	@Test void a17f_themeObjectCoversToolboxDataZoom() {
		var r = part("theme");
		assertJson("{\"iconStyle\":{\"borderColor\":\"#0a0b0c\"}}", r.get("toolbox"));
		assertJson("{\"textStyle\":{\"color\":\"#0a0b0c\"},\"borderColor\":\"#a1a2a3\",\"fillerColor\":\"rgba(1, 2, 3, 0.1)\"}", r.get("dataZoom"));
		assertJson("{\"brushStyle\":{\"color\":\"rgba(1, 2, 3, 0.1)\",\"borderColor\":\"#a1a2a3\"}}", r.get("brush"));
	}

	@Test void a17g_exportTransparentUsesSurface() {
		var r = part("exportSurface");
		assertEquals(List.of("#24272e"), r.get("transparent"));
		assertEquals(List.of("#010203"), r.get("solid"));
	}

	@Test void a17h_errorHandleRunsOwedRetheme() {
		var r = part("themeErrorHandle");
		// Held source draw, then a theme change, then an error handle: the kept picture is re-themed and stays stale.
		assertJson("{\"held\":0,\"themes\":1,\"sameInstance\":true,\"placed\":[1],\"stale\":true,\"error\":null,\"fetches\":3}", r.get("sourceDraw"));
		// The draw held is the re-theme draw itself: the error handle supersedes it, and the re-theme runs again.
		assertJson("{\"held\":0,\"themes\":1,\"sameInstance\":true,\"placed\":[1],\"stale\":true,\"error\":null,\"fetches\":3}", r.get("rethemeDraw"));
	}

	@Test void a17i_ec5SecondThemeChangeIsNotLost() {
		var r = part("themeEc5Twice");
		assertEquals("#000c00", r.get("okColor"));
		assertEquals(3, num(r, "instances"));
		assertEquals(1, num(r, "live"));
		assertEquals(3, num(r, "fetches"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a17j_failedRethemeKeepsPicture() {
		var r = part("themeFailure");
		assertJson("{\"themes\":0,\"sameInstance\":true,\"okColor\":\"#00000a\",\"stale\":true,\"error\":null,\"fetches\":2,\"logged\":1}", r.get("once"));
		// The theme moved while the failing re-theme was in flight: the newer theme is still applied.
		assertJson("{\"themes\":1,\"sameInstance\":true,\"okColor\":\"#000c00\",\"stale\":true,\"error\":null,\"fetches\":3,\"logged\":1}", r.get("moved"));
	}

	@Test void a17k_destroyDuringPendingRetheme() {
		var r = part("themeDestroyPending");
		assertEquals(List.of(), r.get("after"));
		assertEquals(0, num(r, "themes"));
		assertEquals(2, num(r, "fetches"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a18_sourceCard_drawsFromDataHandles() {
		var r = part("source");
		assertEquals("Loading…", r.get("loading"));
		assertEquals(Map.of("points", 3, "overlay", true), r.get("drawn"));
		assertEquals("Loading 25%", r.get("progress"));
		assertEquals(Map.of("points", 2, "stale", true), r.get("stale"));
		assertEquals(1, num(r, "refreshDrew"));
		assertJson("{\"replaceMerge\":[\"series\",\"dataset\"]}", r.get("refreshOpts"));
		assertEquals(2, num(r, "points"));
		assertEquals(List.of("[juneau-charts] E-JS-95: topic 'data:x' carries a malformed data handle: {\"schemaVersion\":1,\"nope\":true}"), r.get("log"));
	}

	@Test void a19_cardApi_tableCapExportAndBuild() {
		var r = part("api");
		assertEquals("Showing 5 of 7 rows", r.get("note"));
		assertEquals(6, num(r, "rows"));
		assertEquals("data:image/png;base64,RkFLRQ==", r.get("url"));
		assertEquals("svg export needs renderer 'svg'", r.get("svg"));
		assertEquals(Map.of("$fn", "percent"), r.get("marker"));
		assertEquals(true, r.get("chart"));
		assertEquals(true, r.get("noToggle"));
		assertEquals(true, r.get("noTable"));
	}

	@Test void a20_fetchIsolation() {
		var r = part("isolation");
		// A card src and two decorator srcs all complete; none aborts another.
		assertJson("{\"urls\":[\"/api/m1\",\"/api/m2\",\"/api/rows\"],\"aborted\":0,\"carriers\":[\"juneau-markers-0\",\"juneau-markers-1\"],\"errors\":[]}", r.get("done"));
		// Destroying the card mid-draw aborts every fetch still in flight, silently.
		assertJson("{\"fetched\":2,\"aborted\":2,\"errors\":[]}", r.get("midDraw"));
		assertJson("{\"fetched\":[\"/api/rows\"],\"aborted\":1,\"errors\":[]}", r.get("srcHeld"));
	}

	@Test void a21_largeFrame_boundByReference_notWalked() {
		var r = part("large");
		assertEquals(true, r.get("sameArray"));
		assertEquals(true, r.get("sameX"));
		assertEquals(0, num(r, "walked"));
		assertEquals(true, r.get("fn"));
		assertEquals("lttb", r.get("sampling"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a22_rowInference_andMissingHelperField() {
		var r = part("infer");
		// The bool column is sniffed from its first non-null value (row 2), so it stays boolean, not text.
		assertEquals(Arrays.asList(null, true, false), r.get("flagCol"));
		assertEquals(List.of("day", "placed", "flag"), r.get("head"));
		assertEquals(List.of("Mon", "1", "", "Tue", "2", "true", "Wed", "3", "false"), r.get("cells"));
		// A key first seen after INFER_ROWS (100) rows is not a column.
		assertEquals(List.of("day", "placed"), r.get("bigHead"));
		assertEquals(true, r.get("bigDrawn"));
		assertEquals("E-JS-63: chart card 'n3' series field 'absent' is not in the data rows", r.get("srcMissing"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a23_rendererThrow_keepsPolling() {
		var r = part("rendererThrow");
		assertEquals(List.of(1), r.get("first"));
		assertEquals(Map.of("stale", true, "live", 1), r.get("during"));
		assertEquals(List.of(3), r.get("after"));
		assertEquals(true, r.get("recovered"));
		assertEquals(List.of("[juneau-charts] E-JS-80: chart card 'q' setOption failed: 'kaput'"), r.get("log"));
	}

	@Test void a24_carrierSeries_areNotCounted() {
		var r = part("carriers");
		assertEquals("Line chart, 2 series, 3 points, Mon to Wed", r.get("plainCaption"));
		assertEquals(r.get("plainCaption"), r.get("withCaption"));
		assertEquals(List.of("placed", "delivered", "juneau-markers-0", "juneau-bands-1"), r.get("ids"));
		assertEquals(true, r.get("ariaEnabled"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a25_everyFetchedOrLinkedUrl_isSameOrigin() {
		var r = part("sameOrigin");
		assertEquals(List.of(), r.get("fetches"));
		var bad = List.of("https://evil.example/x", "//evil.example/x", "javascript:alert(1)", "/a/../b", "/a\\b");
		var expected = new ArrayList<String>();
		for (var i = 0; i < bad.size(); i++)
			expected.add("E-JS-62: chart card 'u" + i + "' fetch failed: ''" + bad.get(i) + "' is not a same-origin path'");
		assertEquals(expected, r.get("errors"));
		assertEquals("E-JS-62: chart card 'um' fetch failed: ''https://evil.example/m.json' is not a same-origin path'", r.get("map"));
		assertEquals("E-JS-98: 'ud' decorator 'markers' failed: ''//evil.example/m' is not a same-origin path'", r.get("dec"));
		// An export that is not an image data URL is never linked.
		assertEquals(List.of(), r.get("downloads"));
		assertEquals(List.of("[juneau-charts] E-JS-103: chart card 'ux' ignored cmd op 'export': the export is not an image data URL"), r.get("log"));
	}

	@Test void a26_contextThrow_isOneCardError() {
		var r = part("ctxThrow");
		// Not the fetch: E-JS-62 is only for the load.  A draw-phase failure with no code of its own is E-JS-98.
		assertEquals("E-JS-98: 'x1' draw 'pipeline' failed: 'ctx-boom'", r.get("error"));
		assertEquals(false, r.get("drawn"));
		assertEquals(List.of("[juneau-charts] E-JS-98: 'x1' draw 'pipeline' failed: 'ctx-boom'"), r.get("log"));
	}

	@Test void a27_clickPayload_isJsonSafe() {
		var r = part("meta");
		assertEquals(1, num(r, "published"));
		assertNull(r.get("err"));
		assertEquals(true, r.get("sameShape"));
		// Object.prototype is untouched by a "__proto__" key in the meta.
		assertEquals(true, r.get("polluted"));
		// Date to ISO text, function/undefined dropped (array holes null), NaN null, a cycle cut at the depth limit.
		var meta = (Map<?,?>)r.get("meta");
		assertEquals(1, ((Number)meta.get("ok")).intValue());
		assertEquals("1970-01-01T00:00:00.000Z", meta.get("d"));
		assertNull(meta.get("n"));
		assertTrue(meta.containsKey("n"));
		assertFalse(meta.containsKey("f"));
		assertFalse(meta.containsKey("u"));
		assertFalse(meta.containsKey("__proto__"));
		assertJson("{\"list\":[1,null,null]}", meta.get("nest"));
		assertJson("[1,null,\"1970-01-01T00:00:00.000Z\"]", r.get("value"));
	}

	@Test void a28_sourceErrorHandle_isOneCardError() {
		var r = part("sourceError");
		assertEquals("E-JS-62: chart card 'se' fetch failed: 'upstream down'", r.get("error"));
		assertEquals(false, r.get("drawn"));
		// The shell's role path does not honour cardReported, so the handler must not rethrow (no second E-JS-8).
		assertEquals(List.of("[juneau-charts] E-JS-62: chart card 'se' fetch failed: 'upstream down'"), r.get("errors"));
	}

	@Test void a29_contextThrow_onSourcePath_isOneCardError() {
		var r = part("ctxThrowSource");
		assertEquals("E-JS-98: 'sz' draw 'pipeline' failed: 'reg-boom'", r.get("error"));
		assertEquals(false, r.get("drawn"));
		assertEquals(List.of("[juneau-charts] E-JS-98: 'sz' draw 'pipeline' failed: 'reg-boom'"), r.get("errors"));
	}

	@Test void a30_sourceCard_refreshReseedsFromTheCurrentHandle() {
		var r = part("sourceRefresh");
		// an empty handle keeps its text across a cmd refresh instead of dropping to "Loading...".
		assertJson("{\"empty\":\"No data.\",\"loading\":null}", r.get("empty"));
		assertJson("{\"empty\":\"No data.\",\"loading\":null,\"drawn\":false}", r.get("emptyAfter"));
		// A drawn card whose source then errors keeps its picture, stale, through a refresh; a card whose FIRST handle is an error keeps the error.
		assertJson("{\"error\":null,\"drawn\":true,\"stale\":true}", r.get("err"));
		assertJson("{\"error\":null,\"loading\":null,\"stale\":true}", r.get("errAfter"));
		assertJson("{\"error\":\"E-JS-62: chart card 'src' fetch failed: 'first handle is an error'\",\"loading\":null}", r.get("firstError"));
		assertEquals(List.of("[juneau-charts] E-JS-62: chart card 'src' fetch failed: 'upstream down'",
			"[juneau-charts] E-JS-62: chart card 'src' fetch failed: 'upstream down'"), r.get("log"));
	}

	@Test void a31_resizeObserver_followsThePlot() {
		var r = part("resize");
		// One live observer on the drawn plot.
		assertJson("{\"ros\":[{\"live\":true,\"onPlot\":true}],\"firstOnPlot1\":true}", r.get("drawn"));
		// empty() disconnects it and cancels a pending frame request.
		assertJson("{\"ros\":[false],\"rafsCancelled\":[true],\"pendingBefore\":1}", r.get("empty"));
		// Drawing again observes the NEW plot with a new observer.
		assertJson("[{\"live\":false,\"onPlot\":false},{\"live\":true,\"onPlot\":true}]", r.get("redrawn"));
		assertJson("{\"resizes\":1}", r.get("afterFrame"));
		// destroy disconnects every observer and cancels the pending frame.
		assertJson("{\"ros\":[false,false],\"lastRafCancelled\":true,\"rafs\":3}", r.get("afterDestroy"));
	}

	@Test void a32_outOfRangeTimes_neverFailTheDraw() {
		var r = part("badTime");
		// 1e20 ms is outside the Date range: shown as its own text; an Infinity cell too (source path); the summary skips non-finite bounds.
		assertJson("{\"error\":null,\"drawn\":true,\"caption\":\"Line chart, 1 series, 3 points, 1970-01-01 to 100000000000000000000\","
			+ "\"cells\":[\"100000000000000000000\",\"1\",\"1970-01-01T00:00:00.005Z\",\"2\",\"1970-01-02T00:00:00.000Z\",\"3\"]}", r.get("t1"));
		assertJson("{\"error\":null,\"drawn\":true,\"caption\":\"Line chart, 1 series, 3 points, 1970-01-02 to 1970-01-03\","
			+ "\"cells\":[\"\",\"1\",\"1970-01-02T00:00:00.000Z\",\"2\",\"1970-01-03T00:00:00.000Z\",\"3\"]}", r.get("t2"));
		assertJson("{\"error\":null,\"drawn\":true,\"caption\":\"Line chart, 1 series, 3 points, 1970-01-02 to 100000000000000000000\","
			+ "\"cells\":[\"Infinity\",\"1\",\"1970-01-02T00:00:00.000Z\",\"2\",\"100000000000000000000\",\"3\"],\"errors\":[]}", r.get("source"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a33_sourceRedrawFailure_keepsThePicture() {
		var r = part("sourceRedrawThrow");
		assertEquals(List.of(1), r.get("first"));
		// On the source path: stale, last picture kept, logged (not painted), and a later good handle recovers.
		assertJson("{\"figure\":1,\"stale\":true,\"error\":null,\"points\":[1]}", r.get("during"));
		assertEquals(List.of(1, 2, 3), r.get("after"));
		assertEquals(true, r.get("recovered"));
		assertEquals(1, num(r, "figure"));
		assertEquals(List.of("[juneau-charts] E-JS-80: chart card 'src' setOption failed: 'kaput'"), r.get("log"));
	}

	@Test void a34_nonRetainedZoomAndLegend_areNotStateTopics() {
		var r = part("retainFalse");
		// retain false (the Java and bus-wiring rule): no restore from the dashboard, no datazoom/legend publishing, nothing folded in.
		assertEquals(List.of("zoom:yr"), r.get("restores"));
		assertEquals(0, num(r, "seen"));
		assertEquals(0, num(r, "legends"));
		assertJson("{\"type\":\"inside\"}", r.get("foldedZoom"));
		assertNull(r.get("foldedLegend"));
		assertJson("{\"drawn\":true,\"error\":null}", r.get("control"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a35_supersededDraws_areDroppedAndAborted() {
		var r = part("superseded");
		// A held fetch that ignores the abort: its late result must not paint.  The re-theme it owed runs after the refresh, on the same instance.
		assertJson("{\"held\":{\"placed\":[1],\"aborted\":false,\"themes\":0},\"painted\":{\"placed\":[2],\"olderAborted\":true,\"themes\":1,\"setOptions\":3},"
			+ "\"late\":{\"placed\":[2],\"setOptions\":3,\"sameInstance\":true,\"stale\":false,\"error\":null,\"errors\":[]}}", ((Map<?,?>)r.get("cycle")));
		// Source card: an older handle's late failure is neither painted nor marked stale.
		assertJson("{\"before\":{\"placed\":[1,2,3],\"olderAborted\":true},\"late\":{\"placed\":[1,2,3],\"stale\":false,\"error\":null,\"log\":[]}}", r.get("source"));
		// an error handle supersedes (and aborts) the draw in flight; its late result neither repaints nor clears the stale mark.
		assertJson("{\"placed\":[1],\"stale\":true,\"aborted\":true}", r.get("errorHandle"));
	}

	@Test void a36_jsonCopy_isBoundedByNodes() {
		var r = part("metaBudget");
		assertEquals(1, num(r, "published"));
		// Six self-referencing keys (6^8 paths under the depth limit alone) and a million-element value both stay small and fast.
		assertEquals(true, r.get("fast"));
		assertEquals(true, r.get("metaNull"));
		assertEquals(true, r.get("valueBounded"));
		assertEquals(4999, num(r, "valueLen"));
	}

	@Test void a37_carrierClick_enterWithoutWalk_andNullPrototypeHref() {
		var r = part("carrierClick");
		// A click on a decorator carrier series has no row.
		assertJson("{\"rows\":[null],\"seriesIds\":[\"juneau-markers-0\"],\"navigated\":[\"/o?c=&d=\"]}", r.get("afterCarrier"));
		// Enter before any arrow walk publishes and navigates nothing; after one arrow it clicks that point.
		assertJson("{\"published\":1,\"navigated\":1}", r.get("afterEnter"));
		assertJson("{\"published\":2,\"row\":{\"day\":\"Mon\",\"placed\":3,\"delivered\":1},\"dataIndex\":0,\"navigated\":[\"/o?c=&d=\",\"/o?c=&d=Mon\"]}", r.get("afterWalk"));
	}

	@Test void a38_export_clampsPixelRatio_andRejects() {
		var r = part("exportClamp");
		// 1e6 -> 4, 0.1 -> 1, 2.5 and 3 kept, -5 -> 1.
		assertJson("[4,1,2.5,3,1]", r.get("ratios"));
		assertNull(r.get("threw"));
		assertEquals(true, r.get("isPromise"));
		assertEquals("canvas-boom", r.get("rejected"));
		assertEquals(List.of("[juneau-charts] E-JS-103: chart card 'ex' ignored cmd op 'export': canvas-boom"), r.get("log"));
	}

	@Test void a39_refreshMs_aboveTheTimerMaximum_isTheMaximum() {
		var r = part("refreshMax");
		assertEquals(List.of(2147483647), r.get("ms"));
		assertEquals(List.of(), r.get("log"));
	}

	@Test void a40_dataTable_isBuiltOnOpen() {
		var r = part("lazyTable");
		// nothing is built while collapsed (not at draw, not at refresh); open builds it; a redraw while open rebuilds; closing stops it.
		assertEquals(Map.of("drawn", 0), r.get("closed"));
		assertEquals(0, num(r, "closedAfterRefresh"));
		assertEquals(9, num(r, "opened"));
		assertEquals(12, num(r, "openedAfterRefresh"));
		assertEquals(Map.of("expanded", "false", "text", "Show data", "hidden", true), r.get("closedAgain"));
		assertEquals(15, num(r, "reopened"));
	}

	@Test void a41_keyboardWalk_homeEndEscapeBlur() {
		var r = part("keys");
		assertEquals(List.of("highlight:0", "showTip:0", "downplay:0", "highlight:1", "showTip:1"), r.get("walk"));
		assertEquals(List.of("downplay:1", "highlight:2", "showTip:2"), r.get("end"));
		assertEquals(List.of("downplay:2", "highlight:0", "showTip:0"), r.get("home"));
		// Escape and blur end the walk: the highlight and the tip are cleared, once; the next arrow starts at an end again.
		assertEquals(List.of("downplay:0", "hideTip:"), r.get("escape"));
		assertEquals(List.of(), r.get("escapeAgain"));
		assertEquals(List.of("highlight:2", "showTip:2"), r.get("afterEscape"));
		assertEquals(List.of("downplay:2", "hideTip:"), r.get("blur"));
		assertEquals(0, num(r, "blurAgain"));
	}

	@Test void a43_paramsRerender_oldInFlightFetchDoesNotDrawASecondChart() {
		var r = part("paramRerender");
		assertJson("{\"fetches\":2,\"pending\":2,\"created\":0}", r.get("mid"));
		assertEquals(1, num(r, "created"));
		assertEquals(1, num(r, "live"));
		assertEquals(List.of(), r.get("errors"));
		assertNull(r.get("err"));
	}

	@Test void a44_glueImplicit_claimsOnlyRetainedPublishes() {
		var r = part("implicitPin");
		assertEquals(List.of(), r.get("none"));
		assertEquals(List.of("zoom:q"), r.get("zoom"));
		assertEquals(List.of("zoom:q", "legend:q"), r.get("both"));
		assertEquals(List.of("legend:q"), r.get("mixed"));
		assertEquals(List.of(), r.get("off"));
	}

	@Test void a45_liveCount_dropsToZeroWhenEveryCardIsDestroyed() {
		var r = part("liveCount");
		assertEquals(3, num(r, "before"));
		assertEquals(0, num(r, "after"));
		assertEquals(1, num(r, "rerender"));
		assertEquals(0, num(r, "rerenderAfter"));
	}

	@Test void a46_srcTimeout_hungFetchEndsInErrorAndNextRefreshRuns() {
		var r = part("srcTimeout");
		var hung = (Map<?,?>)r.get("afterHang");
		assertEquals("E-JS-62: chart card 'to' fetch failed: 'timed out after 10000 ms'", hung.get("error"));
		assertEquals(true, hung.get("aborted"));
		assertEquals(0, num(hung, "instances"));
		assertEquals(2, num(r, "fetches"));
		assertEquals(true, r.get("recovered"));
		assertJson("[7]", r.get("placed"));
		assertNull(r.get("errorAfter"));
	}

	@Test void a47_dataSource_bootDrawAndRefreshForwarding() {
		var r = part("dataSource");
		assertEquals(3, num(r, "points"));
		assertJson("[{\"schemaVersion\":1,\"op\":\"refresh\",\"options\":{\"clearCache\":true}}]", r.get("cmds"));
		// A refresh with no options forwards clearCache false.
		assertJson("[{\"schemaVersion\":1,\"op\":\"refresh\",\"options\":{\"clearCache\":false}}]", r.get("plainCmds"));
		// The boot load, then the two forwarded refreshes.
		assertJson("[\"/rest/orders\",\"/rest/orders\",\"/rest/orders\"]", r.get("fetches"));
		assertJson("{\"status\":\"ready\",\"version\":3}", r.get("handle"));
		assertJson("[]", r.get("errors"));
		assertJson("[\"[juneau-console] page has data sources but juneau-data.js is not loaded\"]", r.get("missing"));
	}

	@Test void a48_treemap_optionSnapshot() {
		var r = part("treemapOption");
		assertEquals("treemap", r.get("type"));
		assertEquals(true, r.get("roam"));
		// A tile click must not drill down by itself: the click is the card's, not the chart's.
		assertEquals(false, r.get("nodeClick"));
		assertJson("{\"show\":false}", r.get("breadcrumb"));
		assertJson("{\"show\":true}", r.get("upperLabel"));
		assertEquals(28, num(r, "bottom"));
		assertEquals(5, num(r, "levels"));
		// ECharts looks levels up by node depth with its own virtual root at depth 0, so the invisible top node is level 1.
		assertJson("{}", r.get("level0"));
		assertJson("{\"itemStyle\":{\"borderWidth\":0,\"gapWidth\":0},\"upperLabel\":{\"show\":false}}", r.get("level1"));
		assertJson("{\"itemStyle\":{\"borderWidth\":3,\"gapWidth\":3}}", r.get("level2"));
		assertEquals(1, num(r, "roots"));
		assertEquals("jc-root", r.get("rootId"));
		assertEquals(List.of("eu", "us"), r.get("groupIds"));
		assertEquals(List.of("eu/c1"), r.get("clusterIds"));
		assertEquals(List.of("eu/c1/h1", "eu/c1/h2", "us/c2/h3"), r.get("leafIds"));
		assertJson("{\"color\":\"#2e8540\"}", r.get("okColor"));
		assertJson("{\"color\":\"#c23934\"}", r.get("downColor"));
		assertEquals(true, r.get("unmatched"));
		assertJson("{\"$fn\":\"template\",\"args\":{\"text\":\"host: {meta.host}\\nhealth: {meta.health}\\ncpu: {meta.cpu}\"}}", r.get("formatter"));
		assertEquals("richText", r.get("renderMode"));
		assertTrue(String.valueOf(r.get("keyText")).contains("Healthy"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a49_treemap_treeBuild_orderWeightsMetaIds() {
		var r = part("treemapTree");
		// Group order is first appearance; a "/" inside a group value is encoded so it cannot forge a path.
		assertEquals(List.of("a%2Fb", "z"), r.get("groupOrder"));
		assertEquals(List.of("a/b", "z"), r.get("groupNames"));
		// Two leaves with the same label under one group get a stable numeric suffix.
		assertEquals(List.of("a%2Fb/x", "a%2Fb/x~2", "a%2Fb/y", "z/x"), r.get("leafIds"));
		// A non-positive or missing size is weight 0.
		assertJson("[5,3,0,0]", r.get("weights"));
		assertJson("[{\"h\":\"x\",\"w\":5},{\"h\":\"x\",\"w\":3},{\"h\":\"y\",\"w\":null},{\"h\":\"x\",\"w\":-2}]", r.get("meta"));
		// No size field: every tile weighs 1, and meta is empty without tileFields.
		assertJson("[1,1,1,1]", r.get("unsizedWeights"));
		assertJson("[{},{},{},{}]", r.get("unsizedMeta"));
		assertJson("[3,9,1]", r.get("cpuWeights"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a56_treemap_edgeCases_nullGroupsSentinelRootCollisionSizeAsLabelLateTileField() {
		var r = part("treemapEdge");
		// A null or empty group or label value gets the sentinel segment "%" (which encodeURIComponent never produces), so no id is empty
		// and the separator is never lost; a group named like the synthetic root cannot collide with it.
		assertEquals(List.of("%", "eu", "jc-root%"), r.get("groupIds"));
		assertEquals(List.of("", "eu", "jc-root"), r.get("groupNames"));
		assertEquals(List.of("%/eu"), r.get("nested"));
		assertEquals(List.of("%/eu/a", "eu/c1/%", "eu/c1/b", "jc-root%/q/c"), r.get("leafIds"));
		assertEquals(true, r.get("unique"));
		assertEquals(true, r.get("noneEmpty"));
		// size naming the label field still weighs the tiles.
		assertJson("[5,7]", r.get("sizedWeights"));
		// A tile field absent from the first rows is still a column.
		assertNull(r.get("firstExtra"));
		assertEquals("late", r.get("lastExtra"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a50_treemap_missingGroupField_isE_JS_63() {
		var r = part("treemapMissingField");
		assertEquals("E-JS-63: chart card 'bad' series field 'cluster' is not in the data rows", r.get("error"));
		assertEquals(false, r.get("drawn"));
		assertSize(1, ((List<?>)r.get("log")));
	}

	@Test void a51_treemap_refreshRebuildsTree() {
		var r = part("treemapRefresh");
		assertEquals(List.of("eu/c1/h1", "eu/c1/h2", "us/c2/h3"), r.get("before"));
		// Ids of unchanged rows are identical across the refresh; the new row is a new id.
		assertEquals(List.of("eu/c1/h1", "eu/c1/h2", "ap/c9/h9"), r.get("after"));
		assertEquals(2, num(r, "fetches"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a52_treemap_leafClick_rowNullMetaFromTileFields() {
		var r = part("treemapLeafClick");
		assertJson("["
			+ "{\"schemaVersion\":1,\"card\":\"tm\",\"event\":\"click\",\"seriesId\":\"tiles\",\"seriesName\":\"tiles\",\"dataIndex\":2,\"name\":\"h1\",\"value\":1,"
			+ "\"row\":null,\"meta\":{\"host\":\"h1\",\"health\":\"ok\",\"cpu\":3}},"
			// The control: the same index on a cartesian card still yields the frame row.
			+ "{\"schemaVersion\":1,\"card\":\"line\",\"event\":\"click\",\"seriesId\":\"placed\",\"dataIndex\":0,\"name\":\"Mon\",\"value\":3,"
			+ "\"row\":{\"day\":\"Mon\",\"placed\":3,\"delivered\":1},\"meta\":null}]", r.get("got"));
	}

	@Test void a53_treemap_groupClick_publishesNothing() {
		var r = part("treemapGroupClick");
		assertJson("{\"published\":0,\"navigated\":[],\"e79\":[]}", r.get("afterGroup"));
		// The leaf click on the same card publishes and navigates through the leaf meta.
		assertJson("{\"published\":1,\"navigated\":[\"/x/h1\"]}", r.get("afterLeaf"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a54_treemap_leafMeta_cappedAt4KiB() {
		var r = part("treemapMeta");
		assertEquals(1, num(r, "published"));
		assertEquals(true, r.get("metaNull"));
		assertEquals(true, r.get("rowNull"));
	}

	@Test void a55_treemap_keys_walkLeaves_enterPublishesLeafMeta_zoomKeys() {
		var r = part("treemapKeys");
		// The index is the leaf's pre-order position in the option tree with ECharts' virtual root as 0: jc-root 1, eu 2, eu/c1 3, h1 4, h2 5, us 6, us/c2 7, h3 8.
		assertEquals(List.of("highlight:4", "showTip:4", "downplay:4", "highlight:5", "showTip:5"), r.get("walk"));
		assertEquals(List.of(), r.get("enter"));
		assertEquals(List.of("downplay:5", "highlight:8", "showTip:8"), r.get("end"));
		assertEquals(List.of("downplay:8", "highlight:4", "showTip:4"), r.get("home"));
		assertEquals(List.of("downplay:4", "hideTip:undefined@undefined"), r.get("escape"));
		assertEquals(List.of(), r.get("zoomUnfocused"));
		assertEquals(List.of("highlight:4", "showTip:4"), r.get("focus"));
		assertEquals(List.of("treemapZoomToNode:eu/c1@0"), r.get("zoomIn"));
		assertEquals(List.of("treemapRootToNode:eu@0"), r.get("out1"));
		assertEquals(List.of("treemapRootToNode:jc-root@0"), r.get("out2"));
		assertEquals(List.of(), r.get("outAtTop"));
		assertEquals(List.of("treemapRootToNode:jc-root@0"), r.get("reset"));
		assertEquals(List.of(), r.get("afterReset"));
		// Enter on the walked leaf (h2) publishes its tileFields as meta and no row.
		var p = (List<?>)r.get("published");
		assertSize(1, p);
		var pub = (Map<?,?>)p.get(0);
		assertEquals(Map.of("host", "h2", "health", "down", "cpu", 9), pub.get("meta"));
		assertNull(pub.get("row"));
		assertEquals(5, num(pub, "dataIndex"));
		assertEquals("h2", pub.get("name"));
		assertEquals("tm", pub.get("card"));
		assertFalse(pub.containsKey("seriesType"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a56_action_clickPublishesAndRunsHookOnce_rowMergeOrder_metaNameWins() {
		var r = part("actClick");
		assertEquals(1, num(r, "runs"));
		assertEquals("ack", r.get("action"));
		assertEquals(true, r.get("host"));
		var row = (Map<?,?>)r.get("row");
		assertEquals("MetaName", row.get("name"));
		assertEquals("Mon", row.get("day"));
		assertEquals(3, num(row, "value"));
		assertEquals("Placed", row.get("seriesName"));
		assertEquals(1, num(row, "extra"));
		assertEquals(List.of("edit"), r.get("catalogIds"));
		assertEquals(true, r.get("hasRedraw"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a57_action_onRedrawCallsCardRefresh() {
		var r = part("actRedraw");
		assertEquals(true, r.get("redrew"));
		assertEquals(0, num(r, "releases"));
	}

	@Test void a58_action_enterOnFocusedPoint_sameCall() {
		var r = part("actEnter");
		assertEquals(1, num(r, "runs"));
		var row = (Map<?,?>)r.get("row");
		assertEquals("Mon", row.get("day"));
		assertEquals("Placed", row.get("seriesName"));
	}

	@Test void a59_action_secondClickWhileInFlight_ignored_releasedOnSettle() {
		var r = part("actInFlight");
		// aria-busy is on the plot, never the card: the card holds the action's live progress and outcome banner.
		assertEquals(Map.of("aria", "true", "flag", true, "cursor", "progress"), withoutCardAria((Map<?,?>)r.get("busyOn")));
		assertNull(((Map<?,?>)r.get("busyOn")).get("cardAria"));
		assertEquals(1, num(r, "whileBusy"));
		var off = (Map<?,?>)r.get("busyOff");
		assertNull(off.get("aria"));
		assertNull(off.get("cardAria"));
		assertEquals(false, off.get("flag"));
		assertEquals("", off.get("cursor"));
		assertEquals(2, num(r, "afterSettle"));
	}

	private static Map<?,?> withoutCardAria(Map<?,?> m) {
		var out = new LinkedHashMap<Object,Object>(m);
		out.remove("cardAria");
		return out;
	}

	@Test void a60_action_rejectedRunReleasesTheGuard() {
		var r = part("actRejectReleases");
		assertEquals(2, num(r, "runs"));
	}

	@Test void a61_action_synchronousThrowClearsBusy() {
		var r = part("actSyncThrow");
		assertEquals(1, num(r, "first"));
		assertEquals(2, num(r, "second"));
		assertNull(r.get("busy"));
		assertEquals(false, r.get("flag"));
	}

	@Test void a62_action_missingHook_logsE_JS_108_oncePerCard() {
		var r = part("actNoHook");
		assertEquals(List.of("[juneau-charts] E-JS-108: chart card 'nh' action hook JuneauViews.actions is unavailable",
			"[juneau-charts] E-JS-108: chart card 'nh2' action hook JuneauViews.actions is unavailable"), r.get("errors"));
		assertNull(r.get("busy"));
	}

	@Test void a63_action_disposeUnsubscribes_laterPublishNoRun_releasesHostOnce() {
		var r = part("actDispose");
		assertEquals(0, num(r, "runs"));
		assertEquals(1, num(r, "releases"));
		assertEquals(true, r.get("releasedHost"));
	}

	@Test void a64_action_noActionKey_noSubscribe() {
		assertEquals(0, num(part("actNone"), "runs"));
	}

	@Test void a65_action_catalogPassedThrough_catalogOnlyCardSubscribesNothing() {
		var r = part("actCatalogIdentity");
		assertEquals(true, r.get("sameLength"));
		assertEquals("edit", r.get("id"));
		assertEquals("PUT", r.get("method"));
		assertEquals(0, num(part("actNone"), "runs"));
	}

	@Test void a66_action_fullRerenderReleases_inPlaceRefreshDoesNot() {
		var r = part("actRerenderRelease");
		assertEquals(0, num(r, "afterFirst"));
		assertEquals(1, num(r, "afterSecond"));
		assertEquals(true, r.get("allSameHost"));
		assertEquals(0, num(part("actRedraw"), "releases"));
	}

	@Test void a66b_action_inPlaceRefreshKeepsTheBannerNodeAndDoesNotRelease() {
		var r = part("actInPlaceRefresh");
		assertEquals(0, num(r, "releases"));
		assertEquals(true, r.get("bannerKept"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a66c_action_aRedrawAfterDestroyDoesNotRefreshTheDeadCard() {
		var r = part("actDestroyedRedraw");
		assertEquals(0, num(r, "delta"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a67_action_treemapLeafClickAndEnter_runWithLeafMeta_groupClickRunsNothing() {
		var r = part("actTreemap");
		assertEquals(2, num(r, "runs"));
		var row = (Map<?,?>)r.get("row");
		assertEquals("h1", row.get("host"));
		assertEquals("ok", row.get("health"));
		assertEquals(3, num(row, "cpu"));
		assertEquals("h1", ((Map<?,?>)r.get("enterRow")).get("host"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a67b_treemap_groupNonClickEvents_publishAndRunNothing_leafStillDoes() {
		var r = part("treemapGroupEvents");
		assertJson("{\"published\":0,\"runs\":0}", r.get("afterGroup"));
		var after = (Map<?,?>)r.get("afterLeaf");
		assertEquals(1, num(after, "published"));
		assertEquals(1, num(after, "runs"));
		assertEquals("h1", ((Map<?,?>)after.get("row")).get("host"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a67c_a11y_actionPlotIsAGroupWithHint_walkAnnouncesThePoint() {
		var r = part("walkAnnounce");
		assertEquals("group", r.get("tmRole"));
		assertEquals("group", r.get("cartRole"));
		assertTrue(String.valueOf(r.get("tmHint")).contains("Enter"), String.valueOf(r.get("tmHint")));
		assertEquals("h1, health ok, cpu 3", r.get("tmFirst"));
		assertEquals("h2, health down, cpu 9", r.get("tmSecond"));
		assertEquals("", r.get("tmEscape"));
		assertEquals("day Mon, placed 3, delivered 1", r.get("cartFirst"));
		assertEquals("day Wed, placed 4, delivered 4", r.get("cartLast"));
		// A plain chart stays a picture: no hint, no live region.
		assertEquals("img", r.get("plainRole"));
		assertNull(r.get("plainHint"));
		assertNull(r.get("plainLive"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a67d_treemap_groupClickOnAnActionCard_hintsToPickATile() {
		var r = part("treemapGroupHint");
		assertEquals("Select a tile, not a group, to open it.", r.get("hint"));
		assertEquals(true, r.get("shown"));
		assertEquals(false, r.get("plainShown"));
		assertEquals(0, num(r, "runs"));
	}

	@Test void a67e_refresh_sameTopicNewAction_runsTheNewAction_changedTopicRebinds() {
		var r = part("refreshRebind");
		assertEquals(List.of("/y/{name}", "/y/{name}"), ((List<?>)r.get("afterNewAction")).subList(1, 3));
		assertEquals(0, num(r, "oldTopic"));
		assertEquals(1, num(r, "newTopic"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a67f_refresh_changeOfInteractivity_rebuildsTheA11yChrome() {
		var r = part("refreshInteractivity");
		assertEquals("img", r.get("first"));
		assertJson("{\"role\":\"group\",\"live\":true}", r.get("added"));
		assertJson("{\"role\":\"img\",\"live\":false}", r.get("removed"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a67g_topicActionsOnlyCard_isOperable_groupClickHints_crossFilterOnlyDoesNot() {
		var r = part("topicActionsOnly");
		assertEquals("group", r.get("tmRole"));
		assertEquals("group", r.get("cartRole"));
		assertEquals("group", r.get("xfRole"));
		assertEquals("day Mon, placed 3, delivered 1", r.get("cartLive"));
		assertEquals("Select a tile, not a group, to open it.", r.get("hint"));
		assertEquals(true, r.get("shown"));
		assertEquals(false, r.get("xfShown"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a67h_walkAnnounce_timeAxisReadsIso_skipsMissingValues() {
		var r = part("walkTime");
		assertEquals("day 2026-10-13T00:00:00.000Z, placed 3", r.get("first"));
		assertEquals("day 2026-10-14T00:00:00.000Z, placed 4, delivered 2", r.get("second"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a68_action_topicActions_runOnDeclaredMarkersTopic() {
		var r = part("actTopicActions");
		assertEquals(0, num(r, "afterClick"));
		assertEquals(1, num(r, "runs"));
		var row = (Map<?,?>)r.get("row");
		assertEquals("Marked", row.get("name"));
		assertEquals("v", row.get("k"));
	}

	@Test void a69_action_dialogHeld_secondClickIgnoredUntilRunSettles() {
		var r = part("actDialogHeld");
		assertEquals(1, num(r, "held"));
		assertEquals(2, num(r, "after"));
	}
}
