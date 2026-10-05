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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.test.*;
import org.junit.jupiter.api.*;

class PageCapture_Unit_Test extends TestBase {

	@Test void a01_minimalContract() {
		var cap = new PageCapture();
		cap.title("T");
		assertEquals("{\"version\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[],\"cards\":[]}", cap.toContractJson());
	}

	@Test void a02_fullContract_isValid() throws Exception {
		var cap = new PageCapture();
		cap.title("Support Enablement Console").theme("open");
		cap.header(h -> h.title("Support Enablement Console").chrome(true));
		cap.footer(f -> f.text("Sandbox Support Console"));
		var home = cap.navRoot().add("home", "Home", "/home");
		home.add("about", "About", "/home/about");
		home.add("setup", "Setup", "/home/setup");
		cap.navRoot().add("slo", "SLO", "/slo");
		cap.tab("home/setup");
		cap.addSlot("header", "banner", "<div>demo</div>");
		cap.addCard(CardSpec.html("jc-seg-1").bare(true), "<p>x</p>");
		cap.resolveActiveNav();
		var html = write(cap, "/juneau-console/juneau-console.js");
		PageContractAssert.assertPage(html).isValid().hasActiveNav("home", "setup").hasHeaderSlot("banner")
			.hasFooterText("Sandbox Support Console").hasCard("jc-seg-1", "html").templateContains("jc-seg-1", "<p>x</p>");
		assertTrue(html.endsWith("<script src=\"/juneau-console/juneau-console.js\"></script>"), html);
	}

	@Test void a03_scriptSafe() {
		var cap = new PageCapture();
		cap.title("</script><script>alert(1)</script>");
		assertFalse(cap.toContractJson().contains("<"), cap.toContractJson());
		assertTrue(cap.toContractJson().contains("\\u003c/script>"), cap.toContractJson());
	}

	@Test void a04_activeNav_selectedWins() throws Exception {
		var cap = new PageCapture();
		cap.navRoot().add("a", "A", "/a");
		cap.navRoot().add("b", "B", "/b");
		cap.tab("a");
		cap.select(List.of("b"));
		cap.resolveActiveNav();
		assertEquals(List.of("b"), cap.activeNav());
	}

	@Test void a05_activeNav_unknownTab_isE7() throws Exception {
		var cap = new PageCapture();
		cap.navRoot().add("a", "A", "/a").add("x", "X", "/a/x");
		cap.tab("a/y");
		var e = assertThrows(Exception.class, cap::resolveActiveNav);
		assertEquals("<@page tab='a/y'> does not match a visible <@node> path; known paths: 'a, a/x'.", e.getMessage());
	}

	@Test void a06_activeNav_noTab_isEmpty() throws Exception {
		var cap = new PageCapture();
		cap.navRoot().add("a", "A", "/a");
		cap.resolveActiveNav();
		assertEquals(List.of(), cap.activeNav());
	}

	@Test void a07_duplicateCard_isE9() throws Exception {
		var cap = new PageCapture();
		cap.addCard(CardSpec.html("c"), "<p/>");
		var e = assertThrows(Exception.class, () -> cap.addCard(CardSpec.html("c"), "<p/>"));
		assertEquals("<@card id='c'> duplicates an existing card id.", e.getMessage());
	}

	@Test void a08_segments() throws Exception {
		var cap = new PageCapture();
		cap.pageBuffer().write("  \n ");
		cap.flushSegment();
		cap.pageBuffer().write("<p>one</p>");
		cap.flushSegment();
		cap.addCard(CardSpec.html("mid"), "<i/>");
		cap.pageBuffer().write("<p>two</p>");
		cap.flushSegment();
		assertBeans(cap.cards(), "id", "jc-seg-1", "mid", "jc-seg-2");
		assertEquals("<p>two</p>", cap.templates().get("jc-seg-2"));
	}

	@Test void a09_markMain() throws Exception {
		var cap = new PageCapture();
		cap.consoleBuffer().write("<div class=\"banner\">b</div>");
		cap.markMain();
		assertEquals("<div class=\"banner\">b</div>", cap.templates().get("header.banner"));
		cap.consoleBuffer().write("\n");
		cap.checkConsoleClose();   // whitespace only: fine
		cap.consoleBuffer().write("<p>late</p>");
		var e = assertThrows(Exception.class, cap::checkConsoleClose);
		assertEquals("<@console> has markup after <@main/>; move it into <@footer> or <@scripts>.", e.getMessage());
	}

	@Test void a10_mainCount_isE13() {
		var cap = new PageCapture();
		var e = assertThrows(Exception.class, cap::checkConsoleClose);
		assertEquals("<@console> requires exactly one <@main/>; found '0'.", e.getMessage());
	}

	@Test void a11_select_twiceIsE6() throws Exception {
		var cap = new PageCapture();
		cap.select(List.of("a"));
		var e = assertThrows(Exception.class, () -> cap.select(List.of("b")));
		assertEquals("<@node id='b'> selected=true but 'a' is already selected.", e.getMessage());
	}

	private static String write(PageCapture cap, String shellUrl) throws IOException {
		var sw = new StringWriter();
		cap.shellUrl(shellUrl);
		cap.writeBody(sw);
		return sw.toString();
	}
}
