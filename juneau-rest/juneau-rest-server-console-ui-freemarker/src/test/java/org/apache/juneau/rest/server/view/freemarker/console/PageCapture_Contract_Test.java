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

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.rest.server.view.freemarker.console.C1Fixtures.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * One test per row of the spec §4.2 directive-to-contract table (cards are covered in Task 8's additions).
 *
 * @since 10.0.0
 */
class PageCapture_Contract_Test extends TestBase {

	@Test void a01_consoleTitleAndBrand() {
		var html = render("console-full");
		assertPage(html).isValid().hasVersion("1").hasTitle("Doc Title").hasHeaderTitle("Brand");
		assertTrue(html.contains("<title>Doc Title</title>\n"), html);
	}

	@Test void a02_titleDefaultsToBrand() {
		var html = render("console-brand");
		assertPage(html).isValid().hasTitle("Brand").hasHeaderTitle("Brand");
		assertTrue(html.contains("<title>Brand</title>\n"), html);
	}

	@Test void a03_iconBecomesLogo() {
		var logo = assertPage(render("console-full")).contract().getMap("header").getMap("logo");
		assertBean(logo, "src,alt", "/app/logo.svg,Brand");
		assertNotNull(logo.getString("href"));
		assertTrue(logo.getString("href").startsWith("/"), logo.getString("href"));
	}

	@Test void a04_faviconIsHeadOnly() {
		var html = render("console-full");
		assertTrue(html.contains("<link rel=\"icon\" href=\"/fav.ico\">\n"), html);
		assertFalse(assertPage(html).contract().toString().contains("fav.ico"));
	}

	@Test void a05_themeAttr() {
		assertPage(render("console-full")).isValid().hasTheme("light-red");
	}

	@Test void a06_themeDefaultsToOpen() {
		assertPage(render("console-min")).isValid().hasTheme("open");
	}

	@Test void a07_themeDirective() {
		var html = render("console-theme");
		assertPage(html).isValid().hasTheme("gray");
		assertTrue(html.contains("juneau-theme-gray.css"), html);
		assertTrue(html.contains("<style>"), html);
		assertTrue(html.contains("--jc-surface"), html);
	}

	@Test void a08_chrome() {
		assertEquals(Boolean.TRUE, assertPage(render("console-full")).contract().getMap("header").get("chrome"));
		assertNull(assertPage(render("console-brand")).contract().getMap("header").get("chrome"));
	}

	@Test void a09_brandSlot() {
		assertPage(render("console-full")).isValid().hasHeaderSlot("brand")
			.templateContains("header.brand", "<span class=\"b\">B</span>");
	}

	@Test void a10_actionsSlot() {
		assertPage(render("console-full")).isValid().hasHeaderSlot("actions")
			.templateContains("header.actions", "<button class=\"help\">Help</button>");
	}

	@Test void a11_titleSlotIsReplace() {
		assertPage(render("console-title-slot")).isValid().hasHeaderSlot("replace")
			.templateContains("header.replace", "my-custom-header");
	}

	@Test void a12_freeMarkupBeforeMainIsBanner() {
		assertPage(render("console-full")).isValid().hasHeaderSlot("banner")
			.templateContains("header.banner", "<div class=\"demo-banner\">Demo</div>");
	}

	@Test void a13_footerText() {
		var a = assertPage(render("console-full")).isValid().hasFooterText("Footer text");
		assertNull(a.contract().getMap("footer").get("slots"));
	}

	@Test void a14_footerBodyIsContentSlot() {
		var a = assertPage(render("console-footer-body")).isValid().hasFooterSlot("content")
			.templateContains("footer.content", "<b>ACME</b>");
		assertNull(a.contract().getMap("footer").get("text"));
	}

	@Test void a15_plainTextFooterBodyIsStillASlot() {
		var a = assertPage(render("console-footer-plain")).isValid().hasFooterSlot("content");
		assertNull(a.contract().getMap("footer").get("text"));
	}

	@Test void a16_serverSlotsStayOutOfTheContract() {
		var html = render("console-full");
		assertTrue(html.contains("<link rel=\"stylesheet\" href=\"extra-head.css\">"), html);
		assertTrue(html.indexOf("extra-head.css") < html.indexOf("</head>"), html);
		assertTrue(html.contains("<body data-app=\"c1\">\n"), html);
		assertTrue(html.contains("<script src=\"extra-tail.js\"></script>"), html);
		var json = assertPage(html).contract().toString();
		assertFalse(json.contains("extra-head"), json);
		assertFalse(json.contains("extra-tail"), json);
	}

	@Test void a17_navLayout() {
		assertEquals("vertical", assertPage(render("console-full")).contract().getString("navLayout"));
		assertNull(assertPage(render("console-min")).contract().get("navLayout"));
	}

	@Test void a18_navTree() {
		assertPage(render("console-full")).isValid()
			.hasNavPath("home", "about")
			.hasNavHref("home", "/home")
			.hasNavHref("home/about", "/home/about")
			.hasNavChildren("home", "about");
	}

	@Test void a19_pageTabDrivesActiveNav() {
		var html = render("page-tab-setup");
		assertPage(html).isValid().hasActiveNav("home", "setup");
		assertTrue(html.contains("<meta name=\"page-tab\" content=\"home/setup\">\n"), html);
	}

	@Test void a20_pageBodyIsASegment() {
		assertPage(render("page-plain")).isValid().hasCard("jc-seg-1", "html")
			.templateContains("jc-seg-1", "<p class=\"c1-body\">plain</p>");
	}

	@Test void a21_pageAssets() {
		var html = render("page-assets");
		assertTrue(html.contains("<link rel=\"stylesheet\" href=\"a.css\">\n<link rel=\"stylesheet\" href=\"b.css\">\n"), html);
		assertTrue(html.contains("data-toolkit-css>"), html);
		assertTrue(html.contains("data-toolkit-js></script>"), html);
		assertTrue(html.indexOf("data-toolkit-js") < html.indexOf("<script src=\"one.js\">"), html);
		assertTrue(html.indexOf("<script src=\"one.js\">") < html.indexOf("<script src=\"two.js\">"), html);
	}

	@Test void a22_wireOrder() {
		var html = render("console-full");
		var order = List.of("</head>", "<body", "<script type=\"application/json\" id=\"juneau-page\">",
			"<template data-slot=\"header.brand\">", "juneau-console.js", "extra-tail.js", "</body>\n</html>\n");
		var last = -1;
		for (var s : order) {
			var i = html.indexOf(s);
			assertTrue(i > last, () -> "'" + s + "' out of order in:\n" + html);
			last = i;
		}
		assertTrue(html.endsWith("\n</body>\n</html>\n"), html);
	}

	@Test void a23_consoleWithoutPage() {
		var a = assertPage(render("console-min")).isValid().hasNoActiveNav();
		assertEquals(List.of(), a.contract().getList("nav"));
		assertEquals(List.of(), a.contract().getList("cards"));
		assertNull(a.contract().get("header"));
		assertNull(a.contract().get("footer"));
	}

	@Test void a24_csrf() {
		var html = render("csrf-min");
		assertTrue(html.contains("<meta name=\"csrf-token\" content=\"tok-123\">\n"), html);
		assertTrue(html.contains("<body data-juneau-csrf=\"tok-123\" data-juneau-csrf-header=\"X-Csrf-Token\">\n"), html);
		assertTrue(render("console-min").contains("<body>\n"));
	}

	@Test void a25_noLegacyChromeMarkup() {
		var html = render("console-full");
		assertFalse(html.contains("<header class=\"jc-header\""), html);
		assertFalse(html.contains("<nav class=\"juneau-page-nav"), html);
		assertFalse(html.contains("<footer class=\"jc-page-footer\""), html);
		assertFalse(html.contains("<main class=\"jc-main\""), html);
	}
}
