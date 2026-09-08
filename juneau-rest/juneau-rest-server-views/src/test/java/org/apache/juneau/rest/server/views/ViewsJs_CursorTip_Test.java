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
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the instant cursor tooltip on ribbon/paging icon chrome.
 *
 * <p>
 * Paging and ribbon buttons already set native {@code title} plus {@code aria-label}.  The helper promotes
 * {@code title} to {@code data-jc-tip} on first hover inside those hosts, paints one floating {@code .jc-tip}
 * bubble immediately, and leaves {@code aria-label} alone.  Titles on form fields and on nodes outside
 * ribbon/paging/toolbar chrome stay native.
 */
class ViewsJs_CursorTip_Test extends TestBase {

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsCss() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_CSS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Source shape
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_initCursorTooltip_isExported() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function initCursorTooltip("), body);
		assertTrue(body.contains("initCursorTooltip: initCursorTooltip"), body);
		assertTrue(body.contains("data-jc-tip"), body);
		assertTrue(body.contains("jc-cursor-tip"), body);
	}

	@Test void a02_doesNotUseInnerHtmlForTipText() throws Exception {
		var body = viewsJs();
		var start = body.indexOf("function cursorTipShow(");
		assertTrue(start >= 0, body);
		var end = body.indexOf("\n\tfunction ", start + 1);
		var fn = body.substring(start, end < 0 ? body.length() : end);
		assertTrue(fn.contains("textContent"), fn);
		assertFalse(fn.contains("innerHTML"), fn);
	}

	@Test void a03_css_hasJcTipAndNoSlds() throws Exception {
		var css = viewsCss();
		assertTrue(css.contains(".jc-tip {"), css);
		assertTrue(css.contains("--jc-tip-z:"), css);
		assertTrue(css.contains("var(--jc-popover-bg)"), css);
		assertFalse(css.contains("slds-"), css);
		assertFalse(css.contains("sbx-cursor-tip"), css);
		assertFalse(css.contains("data-sbx-tip"), css);
	}

	@Test void a04_js_hasNoCopiedIrsNames() throws Exception {
		var body = viewsJs();
		assertFalse(body.contains("sbx-cursor-tip"), body);
		assertFalse(body.contains("data-sbx-tip"), body);
		assertFalse(body.contains("u_sbxTip"), body);
		assertFalse(body.contains("slds-"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Behavioral harness
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			Files.writeString(rendersFile, rendersJs(), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(rendersFile);
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/chrome-tip.cjs");
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/chrome-tip.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/chrome-tip.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("chrome-tip-stdout-", ".json");
		var stderr = Files.createTempFile("chrome-tip-stderr-", ".txt");
		try {
			var p = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile())
				.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("chrome-tip.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("chrome-tip.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
					+ "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}

	private static Map<?,?> report() {
		assumeTrue(report != null, "node not available or chrome-tip.cjs not found — behavioral layer skipped");
		return report;
	}

	private static void assertTrueKey(String key) {
		assertEquals(true, report().get(key), () -> key + " -> " + report().get(key) + " in " + report());
	}

	@Test void b01_harnessLoadedTheHelper() {
		assertTrueKey("hasInit");
	}

	@Test void b02_pagingNextPage_titleMovedOff_tipTextShown_ariaKept() {
		var r = report();
		assertTrueKey("paging_titleBeforeHover");
		assertTrueKey("paging_ariaBeforeHover");
		assertTrueKey("paging_noTipAttrBeforeHover");
		assertTrueKey("paging_titleMovedOff");
		assertEquals("Next page", r.get("paging_tipAttr"));
		assertTrueKey("paging_ariaKept");
		assertEquals("Next page", r.get("paging_tipText"));
		assertTrueKey("paging_tipVisible");
		assertEquals("jc-tip", r.get("paging_tipClass"));
		assertEquals("52px", r.get("paging_tipLeft"));
		assertEquals("62px", r.get("paging_tipTop"));
	}

	@Test void b03_pagingNextPage_followsCursor_hidesOnLeave() {
		assertTrueKey("paging_followedCursor");
		assertTrueKey("paging_hiddenOnLeave");
	}

	@Test void b04_ribbonIcon_samePromotion() {
		assertTrueKey("ribbon_titleMovedOff");
		assertEquals("Refresh", report().get("ribbon_tipAttr"));
		assertTrueKey("ribbon_ariaKept");
		assertEquals("Refresh", report().get("ribbon_tipText"));
		assertTrueKey("ribbon_tipVisible");
	}

	@Test void b05_formFieldOutsideChrome_keepsNativeTitle() {
		assertTrueKey("form_titleKept");
		assertTrueKey("form_noTipAttr");
		assertTrueKey("form_tipHidden");
		assertTrueKey("form_ariaKept");
	}

	@Test void b06_randomTitleOutsideChrome_isNotPromoted() {
		assertTrueKey("outside_titleKept");
		assertTrueKey("outside_noTipAttr");
		assertTrueKey("outside_tipHidden");
	}

	@Test void b07_explicitDataJcTip_showsAndHides() {
		assertEquals("Explicit label", report().get("explicit_tipText"));
		assertTrueKey("explicit_tipVisible");
		assertTrueKey("explicit_hiddenOnLeave");
	}

	@Test void b08_initIsIdempotent() {
		assertTrueKey("reinit_stillWorks");
	}
}
