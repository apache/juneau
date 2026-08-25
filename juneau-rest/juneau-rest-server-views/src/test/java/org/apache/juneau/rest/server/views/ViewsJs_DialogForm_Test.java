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
 * Always-on coverage for the dialog {@code FormDef} painter: {@code appendDialogForm} builds a label+control row per
 * known type via createElement (never innerHTML), skips unknown types, paints a missing-id {@code type=action} button
 * disabled, and {@code collectDialogFormFields} reads text/textarea/select via {@code .value} and checkbox/toggle as
 * explicit {@code "true"}/{@code "false"} while skipping action buttons.
 */
class ViewsJs_DialogForm_Test extends TestBase {

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

	@Test void a01_sourceShape_painterUsesCreateElementNotInnerHtml() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function appendDialogForm("), body);
		assertTrue(body.contains("function paintFormControl("), body);
		assertTrue(body.contains("function collectDialogFormFields("), body);
		assertTrue(body.contains("function buildSelectFormControl("), body);
		assertTrue(body.contains("function buildActionFormControl("), body);
		assertTrue(body.contains("createElement(\"textarea\")"), body);
		assertTrue(body.contains("createElement(\"select\")"), body);
		assertTrue(body.contains("createElement(\"option\")"), body);
		assertTrue(body.contains("createElement(\"button\")"), body);
		assertTrue(body.contains(".textContent"), body);
		var start = body.indexOf("function paintFormControl(");
		var painter = body.substring(start, start + 1500);
		assertFalse(painter.contains("innerHTML"), "paintFormControl must not assign innerHTML");
	}

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
			var p = Path.of(basedir, "src/test/js/dialog-form.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/dialog-form.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/dialog-form.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("dialog-form-stdout-", ".json");
		var stderr = Files.createTempFile("dialog-form-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("dialog-form.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("dialog-form.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or dialog-form.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_textAndTextareaPaintedWithLabelHelpAndError() {
		var r = report();
		assertEquals(true, r.get("notes_id"));
		assertEquals(true, r.get("notes_value"));
		assertEquals(true, r.get("notes_required"));
		assertEquals(true, r.get("notes_maxLength"));
		assertEquals(true, r.get("notes_helpText"));
		assertEquals(true, r.get("notes_describedByHelp"));
		assertEquals(true, r.get("notes_errorSibling"));
		assertEquals(true, r.get("title_value"));
		assertEquals(true, r.get("title_patternAttr"));
	}

	@Test void b02_toggleAndCheckboxPaintedAsCheckboxInputs() {
		var r = report();
		assertEquals(true, r.get("notify_isCheckboxInput"));
		assertEquals(true, r.get("notify_roleSwitch"));
		assertEquals(true, r.get("notify_toggleClass"));
		assertEquals(true, r.get("notify_checkedFromToken"));
		assertEquals(true, r.get("agree_required"));
		assertEquals(true, r.get("agree_unchecked"));
	}

	@Test void b03_selectOptionsViaTextContent() {
		var r = report();
		assertEquals(true, r.get("sev_optionCount"));
		assertEquals(true, r.get("sev_optionTextViaTextContent"));
		assertEquals(true, r.get("sev_prefillValue"));
	}

	@Test void b04_actionButtonAndUnknownTypeHandling() {
		var r = report();
		assertEquals(true, r.get("esc_isButton"));
		assertEquals(true, r.get("esc_enabled"));
		assertEquals(true, r.get("esc_field"));
		assertEquals(true, r.get("esc_noLabelForAction"));
		assertEquals(true, r.get("weird_skipped"));
		assertEquals(true, r.get("missing_disabled"));
		assertEquals(true, r.get("missing_ariaDisabled"));
		assertEquals(true, r.get("missing_marker"));
	}

	@Test void b05_collectReadsTypedValuesAndSkipsActions() {
		var r = report();
		assertEquals(true, r.get("collect_notes"));
		assertEquals(true, r.get("collect_notify_true"));
		assertEquals(true, r.get("collect_agree_false"));
		assertEquals(true, r.get("collect_sev"));
		assertEquals(true, r.get("collect_title"));
		assertEquals(true, r.get("collect_esc_skipped"));
		assertEquals(true, r.get("collect_weird_absent"));
	}
}
