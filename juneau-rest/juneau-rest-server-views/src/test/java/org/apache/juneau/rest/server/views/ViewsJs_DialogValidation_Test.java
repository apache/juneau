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
 * Always-on coverage for the dialog's advisory client-side validation: required-empty / pattern-mismatch /
 * maxLength-exceeded block a confirm submit and mark {@code aria-invalid}; a confirm paints {@code role=alert},
 * concatenates {@code aria-describedby} (help + error) and focuses the first invalid control; an advisory pass
 * leaves {@code role} off; a valid form submits; and a Java-only pattern that throws in {@code new RegExp} fails
 * open (does not block).
 */
class ViewsJs_DialogValidation_Test extends TestBase {

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

	@Test void a01_sourceShape_patternFailsOpenAndConfirmFocusesFirstInvalid() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function validateDialogForm("), body);
		assertTrue(body.contains("function validateOneControl("), body);
		assertTrue(body.contains("function applyControlValidity("), body);
		assertTrue(body.contains("function validateTextControlValue("), body);
		var start = body.indexOf("function validateTextControlValue(");
		var v = body.substring(start, start + 900);
		assertTrue(v.contains("new RegExp"), v);
		assertTrue(v.contains("catch"), "pattern compile must fail-open in a try/catch");
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
			var p = Path.of(basedir, "src/test/js/dialog-validation.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/dialog-validation.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/dialog-validation.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("dialog-validation-stdout-", ".json");
		var stderr = Files.createTempFile("dialog-validation-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("dialog-validation.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("dialog-validation.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or dialog-validation.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_confirmOnInvalidFormBlocksSubmitAndMarksAllInvalid() {
		var r = report();
		assertEquals(true, r.get("invalid_blocksSubmit"));
		assertEquals(true, r.get("req_ariaInvalid"));
		assertEquals(true, r.get("pat_ariaInvalid"));
		assertEquals(true, r.get("lim_ariaInvalid"));
		assertEquals(true, r.get("chk_ariaInvalid"));
	}

	@Test void b02_confirmAnnouncesAlertConcatDescribedByAndFocusesFirstInvalid() {
		var r = report();
		assertEquals(true, r.get("confirm_roleAlert"));
		assertEquals(true, r.get("confirm_errorTextSet"));
		assertEquals(true, r.get("describedby_concatHelpAndError"));
		assertEquals(true, r.get("focus_firstInvalid"));
	}

	@Test void b03_advisoryPassLeavesRoleOff() {
		var r = report();
		assertEquals(true, r.get("advisory_noRoleAlert"));
	}

	@Test void b04_validFormSubmitsAndClearsInvalidMarks() {
		var r = report();
		assertEquals(true, r.get("valid_allowsSubmit"));
		assertEquals(true, r.get("valid_noInvalidMarks"));
	}

	@Test void b05_javaOnlyPatternFailsOpen() {
		var r = report();
		assertEquals(true, r.get("pattern_failOpen"));
	}
}
