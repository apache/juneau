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
package org.apache.juneau.rest.server.console.test;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class PageContractAssert_Test extends TestBase {

	// The §3.1 contract as the server writes it ('<' escaped), plus its templates. One nested <template>
	// inside a segment proves the depth-aware template scan.
	static final String HTML = "<!DOCTYPE html><html><head><title>t</title></head><body data-juneau-csrf=\"x\">"
		+ "<script type=\"application/json\" id=\"juneau-page\">"
		+ "{\"version\":\"1\",\"title\":\"Support Enablement Console\",\"theme\":{\"name\":\"open\"},"
		+ "\"header\":{\"title\":\"Support Enablement Console\",\"chrome\":true,\"slots\":{\"banner\":\"header.banner\"}},"
		+ "\"footer\":{\"text\":\"Sandbox Support Console\"},"
		+ "\"nav\":[{\"id\":\"home\",\"label\":\"Home\",\"href\":\"/home\",\"children\":["
		+ "{\"id\":\"about\",\"label\":\"About\",\"href\":\"/home/about\"},{\"id\":\"setup\",\"label\":\"Setup\",\"href\":\"/home/setup\"}]},"
		+ "{\"id\":\"slo\",\"label\":\"SLO\",\"href\":\"/slo\"}],"
		+ "\"activeNav\":[\"home\",\"setup\"],"
		+ "\"cards\":[{\"id\":\"jc-seg-1\",\"type\":\"html\",\"template\":\"jc-seg-1\",\"bare\":true},"
		+ "{\"id\":\"releases\",\"type\":\"datatables\",\"table\":\"/rest/releases/data\"}]}"
		+ "</script>"
		+ "<template data-slot=\"header.banner\"><div class=\"demo\">\\u003cdemo</div></template>"
		+ "<template data-card=\"jc-seg-1\"><p id=\"ssc-table-slot\">x</p><template id=\"inner\"><i>i</i></template><b>after</b></template>"
		+ "<script src=\"/juneau-console/juneau-console.js\"></script></body></html>";

	@Test void a01_happyPath() {
		PageContractAssert.assertPage(HTML)
			.isValid()
			.hasVersion("1")
			.hasTitle("Support Enablement Console")
			.hasTheme("open")
			.hasActiveNav("home", "setup")
			.hasNavPath("home", "about")
			.hasNavHref("home/setup", "/home/setup")
			.hasNavHref("slo", "/slo")
			.hasNavChildren("home", "about", "setup")
			.hasHeaderTitle("Support Enablement Console")
			.hasHeaderSlot("banner")
			.hasFooterText("Sandbox Support Console")
			.hasCard("releases", "datatables")
			.hasCardOrder("jc-seg-1", "releases")
			.templateContains("jc-seg-1", "id=\"ssc-table-slot\"")
			.templateContains("jc-seg-1", "<b>after</b>");
		assertEquals("open", PageContractAssert.assertPage(HTML).contract().getMap("theme").getString("name"));
	}

	@Test void a02_failuresAreReadable() {
		var a = PageContractAssert.assertPage(HTML);
		assertMessage(() -> a.hasActiveNav("slo"), "activeNav: expected [slo] but was [home, setup]");
		assertMessage(() -> a.hasNavPath("home", "nope"), "nav path [home, nope] not found (failed at 'nope')");
		assertMessage(a::hasNavPath, "hasNavPath() requires at least one path segment");
		assertMessage(() -> a.hasNavHref("slo", "/x"), "nav 'slo' href: expected '/x' but was '/slo'");
		assertMessage(() -> a.hasNavChildren("home", "about"), "nav 'home' children: expected [about] but was [about, setup]");
		assertMessage(() -> a.hasFooterText("x"), "footer.text: expected 'x' but was 'Sandbox Support Console'");
		assertMessage(() -> a.hasFooterSlot("content"), "footer.slots has no 'content'");
		assertMessage(() -> a.hasHeaderSlot("brand"), "header.slots has no 'brand'");
		assertMessage(() -> a.hasCard("releases", "html"), "card 'releases' type: expected 'html' but was 'datatables'");
		assertMessage(() -> a.hasCard("nope", "html"), "no card 'nope'; cards: [jc-seg-1, releases]");
		assertMessage(() -> a.hasCardOrder("releases", "jc-seg-1"), "card order: expected [releases, jc-seg-1] but was [jc-seg-1, releases]");
		assertMessage(() -> a.templateContains("jc-seg-1", "zzz"), "template 'jc-seg-1' does not contain 'zzz'");
		assertMessage(() -> a.template("nope"), "no <template data-card|data-slot=\"nope\">; templates: [header.banner, jc-seg-1]");
		assertMessage(a::hasNoActiveNav, "activeNav: expected [] but was [home, setup]");
	}

	@Test void a03_missingIsland() {
		assertMessage(() -> PageContractAssert.assertPage("<html><body></body></html>"),
			"no <script type=\"application/json\" id=\"juneau-page\"> in page");
	}

	@Test void a04_isValidReportsSchemaAndR4() {
		var bad = HTML.replace("<template data-slot=\"header.banner\">", "<template data-slot=\"header.other\">");
		assertMessage(() -> PageContractAssert.assertPage(bad).isValid(),
			"page contract is invalid:\nR-4: template 'header.banner' is referenced but not present");
	}

	private static void assertMessage(Runnable r, String expected) {
		var e = assertThrows(AssertionError.class, r::run);
		assertEquals(expected, e.getMessage());
	}
}
