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

import java.io.*;
import java.nio.charset.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Guards the folder-tab look, the switcher slider/in-switcher rules, the chart data-tab rules and the popover rules
 * written into {@code chrome.css} and {@code juneau-views.css}.
 */
class FolderTabs_Css_Test extends TestBase {

	private static final String TAB_GROUP = ".juneau-view-ribbon-group[data-juneau-strip-mode=\"tab\"]";
	private static final String SEL = TAB_GROUP + " .juneau-view-ribbon-btn[aria-selected=\"true\"]";

	private static String css(String path) throws IOException {
		try (var in = FolderTabs_Css_Test.class.getResourceAsStream(path)) {
			assertNotNull(in, path + " not found on the test classpath");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String viewsCss() throws IOException {
		return css("/org/apache/juneau/views/juneau-views.css");
	}

	private static String chromeCss() throws IOException {
		return css("/org/apache/juneau/console/chrome.css");
	}

	/** Returns the body of the first rule whose selector list is exactly {@code selector} (at the start of a line). */
	private static String ruleBody(String css, String selector) {
		var m = Pattern.compile("(?m)^" + Pattern.quote(selector) + "\\s*\\{([^}]*)\\}").matcher(css);
		return m.find() ? m.group(1) : "";
	}

	@Test void a01_selectedTab_hasAccentEdgeAndNoInsetShadow() throws IOException {
		var body = ruleBody(chromeCss(), SEL);
		assertTrue(body.contains("border-color: var(--jc-accent)"), body);
		assertFalse(body.contains("box-shadow"), body);
		assertTrue(ruleBody(viewsCss(), TAB_GROUP + " .juneau-view-ribbon-btn").contains("border-width: 3px 1px 0"));
	}

	@Test void a02_selectedTab_washFill_joinsPanel() throws IOException {
		var body = ruleBody(chromeCss(), SEL);
		assertTrue(body.contains("background-color: var(--jc-tab-selected-bg)"), body);
		assertTrue(body.contains("border-bottom-color: transparent"), body);
	}

	@Test void a03_selectedTab_overlapsPanelByOnePixel() throws IOException {
		assertTrue(ruleBody(viewsCss(), SEL).contains("margin-bottom: -1px"));
	}

	@Test void a04_unselectedTab_whiteFill_neutralTopBorder() throws IOException {
		var body = ruleBody(chromeCss(), TAB_GROUP + " .juneau-view-ribbon-btn");
		assertTrue(body.contains("background-color: var(--jc-card-bg)"), body);
		assertFalse(body.contains("border-top-color"), body);
		assertTrue(body.contains("border-color: var(--jc-border)"), body);
	}

	@Test void a05_panel_hasAccentBorderAndCardFill() throws IOException {
		var body = chromeCss();
		var i = body.indexOf(".juneau-view-helper-tabstrip-wrapper[data-juneau-strip-panel] > .juneau-view-helper-tabstrip-panes {");
		assertTrue(i >= 0);
		var rule = body.substring(i, body.indexOf('}', i));
		assertTrue(rule.contains("border-color: var(--jc-accent)") && rule.contains("background-color: var(--jc-card-bg)"), rule);
	}

	@Test void a06_compactTokensDeclared() throws IOException {
		var css = viewsCss();
		assertTrue(css.contains("--jc-tab-compact-font:") && css.contains("--jc-tab-compact-pad-y:") && css.contains("--jc-tab-compact-pad-x:"));
	}

	@Test void a07_detailTabsUseSharedRules() throws IOException {
		var chrome = chromeCss();
		assertFalse(chrome.contains(".juneau-view-detail-tabs {") || chrome.contains(".juneau-view-detail-tabs["), "detail tabs must not get their own colour rules");
		assertFalse(chrome.contains(".juneau-view-dialog-sections {") || chrome.contains(".juneau-view-dialog-sections["), "dialog sections must not get their own colour rules");
	}

	private static final String SLIDER = ".juneau-switcher-selector[data-juneau-switcher-style=\"slider\"]";

	@Test void b01_sliderTrackAndThumbRules() throws IOException {
		var chrome = chromeCss();
		var track = ruleBody(chrome, SLIDER + " > .juneau-view-ribbon-group");
		assertTrue(track.contains("background-color: var(--jc-pill-neutral-bg)") && track.contains("border-radius"), track);
		var thumb = ruleBody(chrome, ".juneau-switcher-thumb");
		assertTrue(thumb.contains("background-color: var(--jc-card-bg)") && thumb.contains("transition: transform"), thumb);
		assertTrue(thumb.contains("box-shadow: var(--jc-switcher-thumb-shadow, var(--jc-card-shadow))"), thumb);
	}

	@Test void b02_reducedMotion() throws IOException {
		var m = Pattern.compile("@media \\(prefers-reduced-motion: reduce\\)\\s*\\{[^@]*?\\.juneau-switcher-thumb\\s*\\{\\s*transition: none;").matcher(chromeCss());
		assertTrue(m.find());
	}

	@Test void b03_inSwitcherBorderless() throws IOException {
		var body = ruleBody(chromeCss(), ".jc-card[data-juneau-in-switcher]");
		assertTrue(body.contains("box-shadow: none") && body.contains("padding: 0") && body.contains("background-color: transparent"), body);
	}

	@Test void c01_chartToggleGone() throws IOException {
		assertFalse(chromeCss().contains(".jc-chart-toggle"));
	}

	@Test void c02_srOnlyPattern() throws IOException {
		var body = ruleBody(chromeCss(), ".jc-chart-sr");
		for (var d : new String[]{"position: absolute", "width: 1px", "height: 1px", "overflow: hidden", "clip-path: inset(50%)", "white-space: nowrap"})
			assertTrue(body.contains(d), d + " missing in " + body);
		assertFalse(body.contains("display: none"), body);
	}

	private static final String POP = ".juneau-view-col-search-popover";

	@Test void d01_popoverRounded() throws IOException {
		assertTrue(ruleBody(chromeCss(), POP).contains("border-radius: var(--jc-radius)"));
	}

	@Test void d01b_popoverFrameIsThinBorderWithPopoverShadow() throws IOException {
		var body = ruleBody(chromeCss(), POP);
		assertTrue(body.contains("border-color: var(--jc-border)"), body);
		assertTrue(body.contains("box-shadow: var(--jc-popover-shadow"), body);
	}

	@Test void d01c_titleIsBodyTextBold() throws IOException {
		assertTrue(ruleBody(chromeCss(), POP + "-title").contains("color: var(--jc-text)"));
		assertTrue(ruleBody(viewsCss(), POP + "-title").contains("font-weight: 700"));
	}

	@Test void d01d_helpUsesTheSearchHelpToken() throws IOException {
		assertTrue(ruleBody(chromeCss(), POP + "-more-list").contains("color: var(--jc-search-help-fg)"));
		assertTrue(ruleBody(viewsCss(), POP + "-more-list").contains("font-size: var(--jc-chrome-font-size-1)"));
	}

	@Test void d01e_inputAndButtonsAre28pxAndButtonsNormalWeight() throws IOException {
		assertTrue(ruleBody(viewsCss(), POP + "-input").contains("height: var(--jc-chrome-control-height-compact)"));
		var btn = ruleBody(viewsCss(), POP + "-footer .jc-btn");
		assertTrue(btn.contains("height: var(--jc-chrome-control-height-compact)") && btn.contains("font-weight: 400"), btn);
	}

	@Test void d02_popoverWider() throws IOException {
		var body = ruleBody(viewsCss(), POP);
		assertFalse(body.contains("max-width: 340px"), body);
		assertTrue(body.contains("max-width: min(440px, 92vw)"), body);
	}

	@Test void d03_footerRules() throws IOException {
		assertTrue(ruleBody(viewsCss(), POP + "-footer").contains("display: flex"));
		assertTrue(ruleBody(viewsCss(), POP + "-more").contains("margin-left: auto"));
	}

	@Test void d04_typeLine() throws IOException {
		assertTrue(ruleBody(chromeCss(), POP + "-type").contains("color: var(--jc-text-muted)"));
		assertFalse(ruleBody(viewsCss(), POP + "-type").isEmpty());
	}

	@Test void d05_moreListHiddenAttr() throws IOException {
		assertTrue(ruleBody(viewsCss(), POP + "-more-list[hidden]").contains("display: none"));
	}

	@Test void e01_jcInputDefined() throws IOException {
		var body = ruleBody(chromeCss(), ".jc-input");
		for (var d : new String[]{"background-color: var(--jc-control-bg)", "color: var(--jc-text)", "border-color: var(--jc-control-border)", "border-radius: var(--jc-radius)"})
			assertTrue(body.contains(d), d + " missing in " + body);
	}
}
