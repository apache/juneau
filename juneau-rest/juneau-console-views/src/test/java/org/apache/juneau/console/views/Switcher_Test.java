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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The switcher card ({@code juneau-switcher.js}) driven through {@code switcher.cjs} against the real page scripts;
 * gated on {@code node} being on {@code PATH}.
 */
class Switcher_Test extends TestBase {

	private static final String SWITCHER_JS = "/org/apache/juneau/console/juneau-switcher.js";

	private static Map<String,Object> report;

	@BeforeAll
	static void runHarness() {
		report = BusHarness.run("switcher.cjs", ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE,
			ViewsMixin.URLSTATE_JS_RESOURCE, ViewsMixin.HELPERS_JS_RESOURCE,
			"/org/apache/juneau/console/juneau-console.js", ViewsMixin.MODEL_JS_RESOURCE, SWITCHER_JS);
	}

	@SuppressWarnings({
		"unchecked" // Casts a parsed JSON value to its known Map shape.
	})
	private static Map<String,Object> scenario(String name) {
		assumeTrue(report != null, "node not on PATH");
		assertNull(report.get("harnessError"), () -> String.valueOf(report.get("harnessError")));
		var s = (Map<String,Object>) report.get(name);
		assertNotNull(s, name);
		return s;
	}

	@Test void s01_onlyTheInitialViewIsBuiltAndEachViewBuildsOnce() {
		var s = scenario("s1");
		assertNull(s.get("threw"));
		assertEquals(List.of("c1"), s.get("afterMount"));
		assertEquals(List.of("c1", "c4"), s.get("afterV3"));
		assertEquals(List.of("c1", "c4"), s.get("final"));
	}

	@Test void s02_displayIsALabelledTabPanelAndTheSelectorHasNoPanes() {
		var s = scenario("s2");
		assertEquals("tabpanel", s.get("role"));
		assertEquals("0", s.get("tabindex"));
		assertEquals(true, s.get("first"));
		assertEquals(true, s.get("second"));
		assertEquals(true, s.get("mode"));
		assertEquals("sw1-display", s.get("controls"));
		assertEquals(1, ((Number)s.get("panes")).intValue());
	}

	@Test void s03_subtabsOnlyForMultiSectionViews() {
		var s = scenario("s3");
		assertEquals(1, ((Number)s.get("one")).intValue());
		assertEquals(2, ((Number)s.get("two")).intValue());
		assertEquals(true, s.get("subMode"));
	}

	@Test void s12_styleChoosesTabsOrSliderLayout() {
		var s = scenario("s12");
		@SuppressWarnings("unchecked") var tabs = (java.util.Map<String,Object>)s.get("tabs");
		@SuppressWarnings("unchecked") var slider = (java.util.Map<String,Object>)s.get("slider");
		assertEquals("tabs", tabs.get("style"));
		assertEquals("tab", tabs.get("mode"));
		assertEquals(true, tabs.get("panel"));
		assertEquals(true, tabs.get("inside"));
		assertEquals(true, tabs.get("viewHost"));
		assertEquals("slider", slider.get("style"));
		assertEquals("ribbon", slider.get("mode"));
		assertEquals(false, slider.get("panel"));
		assertEquals(true, slider.get("sibling"));
	}

	@Test void s13_sliderThumbFollowsSelectionResizeAndReducedMotion() {
		var s = scenario("s13");
		assertEquals(false, s.get("tabsHasThumb"));
		assertEquals(true, s.get("created"));
		@SuppressWarnings("unchecked") var moved = (java.util.Map<String,Object>)s.get("moved");
		assertEquals("translateX(100px)", moved.get("transform"));
		assertEquals("90px", moved.get("width"));
		assertEquals("translateX(57px)", s.get("resized"));
		assertEquals("none", s.get("calm"));
		assertEquals("", s.get("normal"));
		assertEquals(true, s.get("listenerGone"));
	}

	@Test void s14_stackedCardsMountInOrderLazilyWithOneTeardown() {
		var s = scenario("s14");
		assertEquals(List.of("k1", "k2"), s.get("afterMount"));
		assertEquals("juneau-switcher-view", s.get("viewHostClass"));
		assertEquals(List.of(true, true), s.get("inSwitcher"));
		assertEquals(List.of(true, true), s.get("order"));
		assertEquals(List.of("k1", "k2"), s.get("handles"));
		assertEquals(List.of("k1", "k2"), s.get("beforeV2"));
		assertEquals(List.of("k1", "k2", "k3", "k4"), s.get("afterV2"));
		assertEquals("v3", s.get("addedId"));
		assertNotNull(s.get("bad"));
		assertEquals(List.of("k1", "k2", "k3", "k4"), s.get("destroyed"));
	}

	@Test void s04_stateComesFromAddressThenPrefsThenSpecThenFirst() {
		var s = scenario("s4");
		assertEquals("v2", s.get("url"));
		assertEquals("v3", s.get("prefs"));
		assertEquals("v2", s.get("initial"));
		assertEquals("v1", s.get("none"));
		assertEquals("v3", s.get("stale"));
	}

	@Test void s05_sectionRestoresFromPrefsAndAddress() {
		var s = scenario("s5");
		assertEquals("v2.s2", s.get("prefs"));
		assertEquals("v2.s1", s.get("url"));
	}

	@Test void s06_swDirectiveOmitsDefaultsAndSharesOneDirective() {
		var s = scenario("s6");
		assertEquals(true, s.get("none"));
		assertTrue(String.valueOf(s.get("afterA")).contains("sw(a:v2.s1)"), String.valueOf(s.get("afterA")));
		assertTrue(String.valueOf(s.get("both")).contains("sw(a:v2.s1~b:v3)"), String.valueOf(s.get("both")));
		assertFalse(String.valueOf(s.get("cleared")).contains("sw("), String.valueOf(s.get("cleared")));
	}

	@Test void s07_loadingTwiceRegistersTheDirectiveOnce() {
		var s = scenario("s7");
		assertEquals(true, s.get("has"));
		assertEquals(false, s.get("dup67"));
		assertNull(s.get("secondThrew"));
	}

	@Test void s08_handlesActivateAndAddViews() {
		var s = scenario("s8");
		@SuppressWarnings("unchecked") var h = (Map<String,Object>)s.get("handles");
		assertEquals("switcher", h.get("kind"));
		assertEquals(3, ((Number)h.get("views")).intValue());
		assertEquals(true, h.get("v2Path"));
		assertEquals(List.of("detailtab", "detailtab"), h.get("sectionKinds"));
		assertEquals(List.of("s1", "s2"), h.get("sectionIds"));
		assertEquals(true, h.get("segment"));
		assertEquals("v2", s.get("active"));
		assertEquals(true, s.get("synced"));
		assertEquals(List.of("v2"), s.get("fired"));
		assertEquals(4, ((Number)s.get("added")).intValue());
		assertNotNull(s.get("dup"));
		assertEquals(true, s.get("v4Built"));
	}

	@Test void s08b_selectAddressesSectionsAsViewSlashSection() {
		var s = scenario("s15");
		assertEquals(List.of(false, false, false), s.get("bad"));
		assertEquals(true, s.get("viewOnly"));
		assertEquals("v2.s1", s.get("afterViewOnly"));
		assertEquals(true, s.get("ok"));
		assertEquals("v2.s2", s.get("built"));
		assertEquals("v2.s1", s.get("back"));
		assertEquals(true, s.get("direct"));
		assertEquals("v2.s2", s.get("directKey"));
	}

	@Test void s09_refreshTouchesTheActiveViewOnlyAndNeverMounts() {
		var s = scenario("s9");
		assertEquals(List.of("c1"), s.get("refreshed"));
		assertEquals(true, s.get("rendersUnchanged"));
	}

	@Test void s10_destroyReleasesNestedCardsAndTheDirectiveEntry() {
		var s = scenario("s10");
		assertEquals(List.of("c1", "c4"), s.get("destroyed"));
		assertEquals("E-JS-22", s.get("api"));
		assertEquals(true, s.get("source"));
	}

	@Test void s11_nestedCardsKeepTheirApiAndANestedSwitcherIsRejected() {
		var s = scenario("s11");
		assertEquals(0, ((Number)s.get("before")).intValue());
		assertEquals("E-JS-22", s.get("apiBefore"));
		assertEquals(true, s.get("same"));
		assertEquals("c4", s.get("hostId"));
		assertEquals("E-JS-126", s.get("rejected"));
	}
}
