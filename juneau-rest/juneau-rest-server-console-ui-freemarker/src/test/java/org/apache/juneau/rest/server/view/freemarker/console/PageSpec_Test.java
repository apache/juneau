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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link PageSpec} core: template/attrs/headers passthrough, tab/toolkit/css/init/theme/title, the restricted header
 * setter, the card-id-uniqueness check, and {@code applyTo}.
 *
 * @since 10.0.0
 */
class PageSpec_Test extends TestBase {

	private static final String TAB_MSG = "PageSpec tab path must be a non-empty '/'-separated id path; got '%s'.";
	private static final String HEADER_MSG = "PageSpec.header may set only subtitle and userMenu; '%s' belongs in the chrome template.";

	@Test void a01_create_defaultsAreEmpty() {
		var spec = PageSpec.create();
		assertEquals("", spec.tab());
		assertNull(spec.template());
		assertEquals(List.of(), spec.toolkits());
	}

	@Test void a02_template_roundTrips() {
		assertEquals("slo", PageSpec.create().template("slo").template());
	}

	@Test void a03_attrAndHeader_areStored() {
		var spec = PageSpec.create().attr("caption", "Open alerts").header("X-Foo", "bar");
		assertEquals("Open alerts", spec.attrs.get("caption"));
		assertEquals(1, spec.headers.size());
		assertEquals("X-Foo", spec.headers.get(0).name());
		assertEquals("bar", spec.headers.get(0).value());
	}

	@Test void a04_tab_roundTrips() {
		assertEquals("home/skills/core", PageSpec.create().tab("home/skills/core").tab());
	}

	@Test void a05_tab_blank_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().tab(""));
		assertEquals(TAB_MSG.formatted(""), ex.getMessage());
	}

	@Test void a06_tab_null_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().tab(null));
		assertEquals(TAB_MSG.formatted("null"), ex.getMessage());
	}

	@Test void a07_tab_emptySegment_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().tab("home//core"));
		assertEquals(TAB_MSG.formatted("home//core"), ex.getMessage());
	}

	@Test void a08_tab_trailingSlash_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().tab("home/"));
		assertEquals(TAB_MSG.formatted("home/"), ex.getMessage());
	}

	@Test void a09_toolkitCssInit_accumulateAndDedupe() {
		var spec = PageSpec.create().toolkit("views").toolkit("views", "cmd-k").css("/css/a.css").css("/css/a.css", "/css/b.css")
			.init("/js/a.js");
		assertEquals(List.of("views", "cmd-k"), spec.toolkits());
		assertEquals(List.of("/css/a.css", "/css/b.css"), spec.css);
		assertEquals(List.of("/js/a.js"), spec.init);
	}

	@Test void a10_theme_valid_roundTrips() {
		assertEquals("light-red", PageSpec.create().theme("light-red").theme);
	}

	@Test void a11_theme_unknown_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().theme("teal"));
		assertEquals("Unknown stock theme name: 'teal'.  Built-in themes: open, light-red, light-brown, red, gray.", ex.getMessage());
	}

	@Test void a12_title_isSeeded() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().title("SLO").applyTo(cap, null);
		assertEquals("SLO", cap.title());
	}

	@Test void a13_header_subtitleAndUserMenu_allowed() {
		var spec = PageSpec.create().header(h -> h.subtitle("Queue health").userMenu(Map.of("label", "Jane")));
		assertEquals("Queue health", spec.headerSubtitle);
		assertEquals(Map.of("label", "Jane"), spec.headerUserMenu);
	}

	@Test void a14_header_title_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().header(h -> h.title("Nope")));
		assertEquals(HEADER_MSG.formatted("title"), ex.getMessage());
	}

	@Test void a15_header_logo_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().header(h -> h.logo("/img/logo.svg", "/", "Logo")));
		assertEquals(HEADER_MSG.formatted("logo"), ex.getMessage());
	}

	@Test void a16_header_chrome_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().header(h -> h.chrome(true)));
		assertEquals(HEADER_MSG.formatted("chrome"), ex.getMessage());
	}

	@Test void a17_header_link_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().header(h -> h.link("Docs", "/docs")));
		assertEquals(HEADER_MSG.formatted("link"), ex.getMessage());
	}

	@Test void a18_addCard_duplicateId_rejected() {
		var spec = PageSpec.create();
		spec.addCard(CardSpec.html("intro"));
		var ex = assertThrows(IllegalArgumentException.class, () -> spec.addCard(CardSpec.html("intro")));
		assertEquals("PageSpec card id 'intro' is already declared.", ex.getMessage());
	}

	@Test void a19_applyTo_seedsTabToolkitCssInit() throws Exception {
		var spec = PageSpec.create().tab("slo").toolkit("views").css("/css/slo.css").init("/js/slo.js")
			.theme("light-red").header(h -> h.subtitle("Queue health"));
		var cap = new PageCapture();
		spec.applyTo(cap, null);
		assertEquals("slo", cap.tab());
		assertEquals(List.of("views"), cap.toolkits());
		assertEquals(List.of("/css/slo.css"), cap.cssHrefs());
		assertEquals(List.of("/js/slo.js"), cap.initScripts());
		assertEquals("light-red", cap.theme());
		assertEquals("Queue health", cap.headerSubtitle());
	}

	@Test void a21_header_userMenuIsCopied() throws Exception {
		var menu = new LinkedHashMap<String,Object>();
		menu.put("label", "Ann");
		var spec = PageSpec.create().header(h -> h.userMenu(menu));
		menu.put("label", "Changed");
		menu.put("extra", 1);
		var cap = new PageCapture();
		spec.applyTo(cap, null);
		assertEquals(Map.of("label", "Ann"), cap.headerUserMenu());
	}

	@Test void a20_applyTo_withNothingSet_isNoOp() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().applyTo(cap, null);
		assertEquals("", cap.tab());
		assertEquals(List.of(), cap.toolkits());
	}

	@Test void a22_attr_reservedKey_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().attr(PageSpec.ATTR, "x"));
		assertEquals("PageSpec.attr key 'jcPageSpec' is reserved for the spec itself.", ex.getMessage());
	}

	@Test void a23_header_userMenuUnknownKey_rejected() {
		var ex = assertThrows(IllegalArgumentException.class,
			() -> PageSpec.create().header(h -> h.userMenu(Map.of("label", "Jane", "href", "/p"))));
		assertEquals("PageSpec.header userMenu key 'href' is not allowed; allowed: label, initials, avatar, items.", ex.getMessage());
	}

	@Test void a24_navUnder_queuesAnEntry_appliedToByCapture() throws Exception {
		var spec = PageSpec.create();
		var seen = new ArrayList<NavNode>();
		spec.navUnder("fleet", seen::add);
		var cap = new PageCapture();
		cap.navRoot().add("fleet", "Fleet", "/fleet");
		spec.applyTo(cap, null);
		cap.applyPendingNavAdds();
		assertEquals(1, seen.size());
		assertEquals("fleet", seen.get(0).id());
	}

	@Test void a25_navUnder_blankPath_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PageSpec.create().navUnder("", n -> {}));
		assertTrue(ex.getMessage().contains("navUnder"), () -> ex.getMessage());
	}

	@Test void a26_navUnder_reusedSpec_eachCaptureGetsItsOwnAddition() throws Exception {
		var spec = PageSpec.create().navUnder("fleet", n -> n.add("extra", "Extra", "/fleet/extra"));
		for (var i = 0; i < 2; i++) {
			var cap = new PageCapture();
			cap.navRoot().add("fleet", "Fleet", "/fleet");
			spec.applyTo(cap, null);
			cap.applyPendingNavAdds();
			var fleet = cap.navRoot().find(List.of("fleet")).get();
			assertEquals(1, fleet.children().size());
			assertEquals("extra", fleet.children().get(0).id());
		}
	}
}
