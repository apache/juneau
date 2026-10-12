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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * The chart-card runtime {@code juneau-charts.js} against the real shell: renderers, data sources, refresh, errors,
 * the kind registry, clicks, keyboard and escaping.  Runs when node is on PATH.
 */
class ConsoleCharts_Test extends TestBase {

	private static Map<?,?> report() {
		var r = NodeHarness.report("charts.cjs", ConsoleChromeMixin.CONSOLE_JS_RESOURCE, ConsoleChromeMixin.CHARTS_JS_RESOURCE);
		assumeTrue(r != null, "node (or charts.cjs) not available - JS layer skipped");
		return r;
	}

	private static Map<?,?> part(String key) {
		var s = (Map<?,?>)report().get(key);
		assertNotNull(s, () -> "section '" + key + "' missing from the harness output");
		assertNull(s.get("crash"), () -> "section '" + key + "' crashed: " + s.get("crash"));
		return s;
	}

	@Test void a01_line_structureAndAccessibility() {
		var r = part("line");
		assertEquals("Orders", r.get("title"));
		assertEquals(2, ((Number)r.get("lines")).intValue());
		assertEquals(6, ((Number)r.get("marks")).intValue());
		assertEquals(3, ((Number)r.get("hits")).intValue());
		assertEquals("img", r.get("role"));
		// The summary is read once: the svg is labelled by the figcaption, not by a duplicate aria-label.
		assertNull(r.get("aria"));
		assertEquals("jc-chart-caption-c1", r.get("captionId"));
		assertEquals(r.get("captionId"), r.get("labelledby"));
		assertEquals("Line chart, 2 series, 3 points, Day from 'Mon' to 'Wed'", r.get("caption"));
		// Without onClick the hit rects are not focusable buttons.
		assertEquals(Arrays.asList(null, null, null), r.get("hitTabindex"));
		assertEquals(Arrays.asList(null, null, null), r.get("hitRoles"));
		assertEquals(List.of("Placed", "Delivered"), r.get("legend"));
		assertEquals(4, ((Number)r.get("tableRows")).intValue());
		assertEquals("var(--jc-chart-1, #1b6ac9)", r.get("firstStroke"));
		assertEquals("var(--jc-success, #2e8540)", r.get("okStroke"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a02_line_tableToggleAndKeyboard() {
		var r = part("line");
		assertEquals(Map.of("expanded", "false", "text", "Show data", "hidden", true), r.get("toggleBefore"));
		assertEquals(Map.of("expanded", "true", "text", "Hide data", "hidden", false), r.get("toggleAfter"));
		// With onClick the svg is a group of focusable buttons with a roving tabindex.
		assertEquals("group", r.get("clickRole"));
		assertEquals(List.of("button", "button", "button"), r.get("clickHitRoles"));
		assertEquals(List.of("-1", "0", "-1"), r.get("tabindex"));
		assertEquals(true, r.get("focused"));
	}

	@Test void a03_bar_srcRowsStackedAndHorizontal() {
		var r = part("bar");
		assertEquals(6, ((Number)r.get("stackedBars")).intValue());
		assertEquals(6, ((Number)r.get("horizontalBars")).intValue());
		assertEquals(List.of(Map.of("url", "/stats", "accept", "application/json")), r.get("fetches"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a04_donut_foldAndCenter() {
		var r = part("donut");
		assertEquals(3, ((Number)r.get("slices")).intValue());
		assertEquals(List.of("a", "b", "Other"), r.get("legend"));
		assertEquals("100", r.get("center"));
		assertEquals(2, ((Number)r.get("noFoldSlices")).intValue());
		assertEquals("Mix", r.get("literalCenter"));
		assertEquals("Donut chart, 5 slices", r.get("caption"));
		assertEquals(List.of(), r.get("errors"));
	}

	@Test void a04b_donut_focusOnlyWhenClickable_otherSliceInert() {
		var r = part("donut");
		assertEquals("img", r.get("plainRole"));
		assertEquals(Arrays.asList(null, null, null), r.get("plainTabindex"));
		assertEquals(Arrays.asList(null, null, null), r.get("plainRoles"));
		assertEquals("group", r.get("clickRole"));
		assertEquals(List.of("a", "b", "Other"), r.get("clickLegend"));
		assertEquals(Arrays.asList("0", "0", null), r.get("clickTabindex"));
		assertEquals(Arrays.asList("button", "button", null), r.get("clickRoles"));
		// The folded Other slice stands for several rows, so it neither navigates nor fires juneau:chart-click.
		assertEquals(List.of(), r.get("otherNav"));
		assertEquals(0, ((Number)r.get("otherClicks")).intValue());
		assertEquals(List.of("/o?s=a"), r.get("navigated"));
	}

	@Test void a05_cardErrors_reportedOnce() {
		var r = part("errors");
		assertEquals("E-JS-63: chart card 'm' series field 'missing' is not in the data rows", r.get("missing"));
		assertEquals("E-JS-62: chart card 'f' fetch failed: 'boom'", r.get("fetch"));
		var log = String.valueOf(r.get("log"));
		assertTrue(log.contains("[juneau-charts] E-JS-63: chart card 'm'"), log);
		assertTrue(log.contains("[juneau-charts] E-JS-62: chart card 'f'"), log);
		assertFalse(log.contains("E-JS-8"), log);
	}

	@Test void a06_refresh_redrawsThenGoesStale() {
		var r = part("refresh");
		assertEquals(List.of(5000), r.get("ms"));
		assertEquals(5, ((Number)r.get("grown")).intValue());
		assertEquals("Line chart, 2 series, 4 points, Day from 'Mon' to 'Thu'", r.get("grownCaption"));
		assertEquals(true, r.get("stale"));
		assertEquals("Showing the last data; the refresh failed: down", r.get("staleTitle"));
		assertEquals(true, r.get("stillDrawn"));
		assertTrue(String.valueOf(r.get("log")).contains("E-JS-62: chart card 'p' fetch failed: 'down'"));
	}

	@Test void a07_refreshMs_clamped() {
		var r = part("clamp");
		assertEquals(List.of(5000), r.get("ms"));
		assertTrue(String.valueOf(r.get("log")).contains("[juneau-charts] E-JS-75: 'r2' refreshMs '1000' is below the minimum '5000'; using the minimum"));
	}

	@Test void a08_unknownKind_failsAtDomContentLoaded_thenLateRegisterRedraws() {
		var r = part("unknown");
		assertEquals(true, r.get("loading"));
		assertEquals("E-JS-60: chart card 'u' has unknown kind 'gauge'; registered kinds: 'bar, donut, line'", r.get("e60"));
		assertEquals("2", r.get("redrawn"));
		assertEquals(true, r.get("errorGone"));
	}

	@Test void a09_registry() {
		var r = part("registry");
		assertEquals("E-JS-64: chart kind 'line' is already registered", r.get("dup"));
		assertEquals("TypeError", r.get("badKind"));
		assertEquals(List.of("bar", "donut", "line", "pie2", "spark"), r.get("kinds"));
		assertEquals(true, r.get("alias"));
		assertEquals("1", r.get("version"));
	}

	@Test void a10_chartsBeforeShell() {
		var r = part("chartsFirst");
		assertEquals(2, ((Number)r.get("lines")).intValue());
		assertEquals(true, r.get("alias"));
	}

	@Test void a11_clicks() {
		var r = part("click");
		var mon = Map.of("day", "Mon", "placed", 3, "delivered", 1);
		var cards = List.of("k", "k", "x", "x2", "x3");
		var rows = List.of(Map.of("day", "a b&c", "placed", 1, "delivered", 1), Map.of("day", "Tue", "placed", 2, "delivered", 2), mon, mon, mon);
		var clicks = (List<?>)r.get("clicks");
		assertEquals(cards.size(), clicks.size(), String.valueOf(clicks));
		for (var i = 0; i < cards.size(); i++) {
			var c = (Map<?,?>)clicks.get(i);
			assertEquals(cards.get(i), c.get("card"), "click " + i);
			assertEquals(rows.get(i), c.get("row"), "click " + i);
			assertTrue(c.containsKey("series"), "click " + i);
			assertNull(c.get("series"), "click " + i);
		}
		assertEquals(List.of("/orders?day=a%20b%26c&q=", "/orders?day=Tue&q="), r.get("navigated"));
		// Match list elements, not String.valueOf(list): the parsed list re-serializes as JSON, which escapes the tab.
		var log = (List<?>)r.get("log");
		assertTrue(log.contains("[juneau-charts] E-JS-79: chart card 'x' onClick.href resolved to a disallowed URL 'java\tscript:alert(Mon)'"), String.valueOf(log));
		assertTrue(log.contains("[juneau-charts] E-JS-79: chart card 'x2' onClick.href resolved to a disallowed URL 'https://evil.test/x?d=Mon'"), String.valueOf(log));
		assertTrue(log.contains("[juneau-charts] E-JS-79: chart card 'x3' onClick.href resolved to a disallowed URL '//evil.test/Mon'"), String.valueOf(log));
	}

	@Test void a12_escapingAndEmpty() {
		var r = part("escape");
		assertEquals("<b>x</b>", r.get("legend"));
		assertEquals(0, ((Number)r.get("boldTags")).intValue());
		assertEquals("Nothing yet.", r.get("empty"));
		assertEquals("No data.", r.get("emptyDefault"));
	}

	@Test void a13_destroy_stopsPolling() {
		var r = part("destroy");
		assertEquals(1, ((Number)r.get("liveBefore")).intValue());
		assertEquals(0, ((Number)r.get("liveAfter")).intValue());
	}

	@Test void a14_rendererThrows_isE_JS_80_once() {
		var r = part("boom");
		assertEquals("E-JS-80: chart card 'b' kind 'boom' renderer failed: 'kaput'", r.get("error"));
		var log = String.valueOf(r.get("log"));
		assertTrue(log.contains("[juneau-charts] E-JS-80: chart card 'b' kind 'boom' renderer failed: 'kaput'"), log);
		assertFalse(log.contains("E-JS-8:"), log);
	}

	@Test void a15_repaint_keepsFocusAndCaption() {
		var r = part("repaint");
		assertEquals(4, ((Number)r.get("hits")).intValue());
		assertEquals(List.of("-1", "0", "-1", "-1"), r.get("tabindex"));
		assertEquals(1, ((Number)r.get("focusedIndex")).intValue());
		assertEquals("Line chart, 2 series, 4 points, Day from 'Mon' to 'Thu'", r.get("caption"));
		assertEquals(List.of(), r.get("log"));
	}

	@Test void a16_updateThrows_isE_JS_80_once_andStopsPolling() {
		var r = part("flaky");
		assertEquals(1, ((Number)r.get("liveBefore")).intValue());
		assertEquals(0, ((Number)r.get("liveAfter")).intValue());
		assertEquals("E-JS-80: chart card 'fl' kind 'flaky' renderer failed: 'later'", r.get("error"));
		assertEquals(false, r.get("stale"));
		assertEquals(List.of("fl"), r.get("failed"));
		assertEquals(List.of("[juneau-charts] E-JS-80: chart card 'fl' kind 'flaky' renderer failed: 'later'"), r.get("log"));
	}

	@Test void a17_refreshDrawFailure_failsCardOnce() {
		var r = part("redrawFail");
		assertEquals("E-JS-63: chart card 'dr' series field 'placed' is not in the data rows", r.get("error"));
		assertEquals(List.of("dr"), r.get("failed"));
		assertEquals(0, ((Number)r.get("liveAfter")).intValue());
		assertEquals(false, r.get("stale"));
		assertEquals(List.of("[juneau-charts] E-JS-63: chart card 'dr' series field 'placed' is not in the data rows"), r.get("log"));
	}
}
