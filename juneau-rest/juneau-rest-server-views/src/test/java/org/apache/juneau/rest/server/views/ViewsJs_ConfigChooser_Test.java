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
 * Always-on coverage for the View-tab chooser in {@code juneau-config.js} (slice 6): source-shape pins
 * for the XSS {@code textContent} mandate, last-column refusal, pinned-disabled, and the
 * {@code mountChooser} seam, plus a Node behavioral harness when {@code node} is on PATH.
 */
class ViewsJs_ConfigChooser_Test extends TestBase {

	private static String configJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.CONFIG_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.CONFIG_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var i = body.indexOf('{', start);
		assertTrue(i >= 0, () -> "'" + signature + "' has no opening brace:\n" + body);
		var depth = 0;
		var j = i;
		for (; j < body.length(); j++) {
			var c = body.charAt(j);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0) { j++; break; }
			} else if (c == '"' || c == '\'' || c == '`') {
				var quote = c;
				j++;
				while (j < body.length() && body.charAt(j) != quote) { if (body.charAt(j) == '\\') j++; j++; }
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '/') {
				while (j < body.length() && body.charAt(j) != '\n') j++;
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '*') {
				j = body.indexOf("*/", j) + 1;
			}
		}
		return body.substring(start, j);
	}

	@Test void a01_paintUserText_usesTextContentOnly() throws Exception {
		var fn = functionBody(configJs(), "function paintUserText(");
		assertTrue(fn.contains("el.textContent"), fn);
		assertFalse(fn.contains("innerHTML"), fn);
		assertFalse(fn.contains(".html("), fn);
	}

	@Test void a02_paintUserInput_usesValueOnly() throws Exception {
		var fn = functionBody(configJs(), "function paintUserInput(");
		assertTrue(fn.contains("el.value"), fn);
		assertFalse(fn.contains("innerHTML"), fn);
	}

	@Test void a03_canHideColumn_refusesPinnedAndLastVisible() throws Exception {
		var fn = functionBody(configJs(), "function canHideColumn(");
		assertTrue(fn.contains("col?.pinned"), fn);
		assertTrue(fn.contains("visibleCount(draft) > 1"), fn);
	}

	@Test void a04_sanitizeAndPaintHeaders_neverHandTitleToDataTables() throws Exception {
		var body = configJs();
		var san = functionBody(body, "function sanitizeColumnTitlesForDataTables(");
		assertTrue(san.contains("c.title = \"\""), san);
		var paint = functionBody(body, "function paintHeaderTitles(");
		assertTrue(paint.contains("paintUserText(th, label)"), paint);
	}

	@Test void a05_chooserExportedOnNsConfig() throws Exception {
		var body = configJs();
		for (var name : new String[]{
			"NS.config.mountChooser = mountChooser",
			"NS.config.paintUserText = paintUserText",
			"NS.config.canHideColumn = canHideColumn",
			"NS.config.sanitizeColumnTitlesForDataTables = sanitizeColumnTitlesForDataTables",
			"NS.config.paintHeaderTitles = paintHeaderTitles",
			"NS.config.applyDraft = applyDraft"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "':\n" + body);
	}

	@Test void a06_viewsJsSeam_mountsChooserWhenColumnConfigAndConfigJsPresent() throws Exception {
		var fn = functionBody(viewsJs(), "function constructTable(");
		assertTrue(fn.contains("viewDef.columnConfig"), fn);
		assertTrue(fn.contains("NS.config.mountChooser"), fn);
		assertTrue(fn.contains("sanitizeColumnTitlesForDataTables"), fn);
		assertTrue(fn.contains("paintHeaderTitles"), fn);
	}

	@Test void a07_saveAsDefault_goesThroughValidateNameBasic() throws Exception {
		var fn = functionBody(configJs(), "function openChooser(");
		assertTrue(fn.contains("validateNameBasic(name)"), fn);
		assertTrue(fn.contains("Save as"), fn);
		assertTrue(fn.contains("paintUserText"), fn);
	}

	@Test void a08_configJs_containsNoInnerHtmlAssignment() throws Exception {
		var body = configJs();
		assertFalse(body.contains(".innerHTML ="), body);
		assertFalse(body.contains(".html("), body);
		assertTrue(body.contains(".textContent"), body);
	}

	@Test void a09_applyDraft_callsApplyView() throws Exception {
		var fn = functionBody(configJs(), "function applyDraft(");
		assertTrue(fn.contains("applyView(table, saved)"), fn);
		assertTrue(fn.contains("in-flight"), fn);
	}

	@Test void a10_chooserBackdrop_distinctFromActionDialogTeardownClass() throws Exception {
		var body = configJs();
		assertTrue(body.contains("CHOOSER_BACKDROP_CLASS = \"juneau-config-dialog-backdrop\""), body);
		assertFalse(body.contains("CHOOSER_BACKDROP_CLASS = \"juneau-view-dialog-backdrop\""), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Behavioral — Node harness (skipped when node is absent)
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var configFile = Files.createTempFile("juneau-config-", ".js");
		try {
			Files.writeString(configFile, configJs(), UTF_8);
			report = Json.to(runNode(harness, configFile), Map.class);
		} finally {
			Files.deleteIfExists(configFile);
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
			var p = Path.of(basedir, "src/test/js/config-chooser.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/config-chooser.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/config-chooser.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path configJs) throws Exception {
		var stdout = Files.createTempFile("config-chooser-stdout-", ".json");
		var stderr = Files.createTempFile("config-chooser-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), configJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("config-chooser.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("config-chooser.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or config-chooser.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_xssPaint_setsTextContent_leavesInnerHtmlUntouched() {
		var r = report();
		assertEquals("<img src=x onerror=alert(1)>", r.get("xssTextContent"));
		assertEquals(true, r.get("xssInnerHtmlUntouched"));
		assertEquals("<img src=x onerror=alert(1)>", r.get("xssInputValue"));
		assertEquals(true, r.get("xssInputInnerHtmlUntouched"));
	}

	@Test void b02_lastVisibleAndPinned_cannotHide() {
		var r = report();
		assertEquals(true, r.get("lastCannotHideA"));
		assertEquals(true, r.get("pinnedCannotHide"));
		assertEquals(true, r.get("unpinnedCanHide"));
		assertEquals(true, r.get("alreadyHiddenCanShow"));
	}

	@Test void b03_saveAsDefault_refused() {
		var r = report();
		assertEquals(true, r.get("defaultReserved"));
		assertEquals(true, r.get("defaultReservedCase"));
		assertEquals(true, r.get("saveAsDefaultRefused"));
	}

	@Test void b04_sanitizeTitles_andPaintHeadersViaTextContent() {
		var r = report();
		assertEquals(true, r.get("sanitizedDataTitleBlank"));
		assertEquals(true, r.get("sanitizedSelectionUntouched"));
		assertEquals("<img src=x onerror=alert(1)>", r.get("headerAText"));
		assertEquals(true, r.get("headerAInnerHtmlUntouched"));
		assertEquals("Col B", r.get("headerBText"));
	}

	@Test void b05_defaultDraftAndReorder() {
		var r = report();
		assertEquals(List.of("A", "B", "C"), r.get("defaultDraftOrder"));
		assertEquals(List.of("A", "B", "C"), r.get("defaultDraftVisible"));
		assertEquals(true, r.get("moved"));
		assertEquals(List.of("A", "C", "B"), r.get("orderAfterMove"));
	}
}
