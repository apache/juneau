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
 * Action-bound pill dispatch wired on {@code initRowActions} (NOT details-gated).  Source-shape checks always run; the
 * Node behavioral layer (DOM shim) runs when {@code node} is on {@code PATH}.  Proves an action pill submits its
 * RowAction through the same fail-closed handler the row-action menu uses on a grid with rowActions and NO row-detail
 * template, that Enter/Space activate it (Space {@code preventDefault}-ed), that {@code present=dialog} opens the modal
 * instead of submitting, and that a disabled / in-flight / unknown / display-only pill is inert.
 */
class ViewsJs_PillDispatch_Test extends TestBase {

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

	@Test void a01_sourceShape_pillDispatchHostedOnInitRowActions_notDetailsGated() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function activatePillAction("), body);
		// The pill click + keydown must be wired inside initRowActions (always available when rowActions exist),
		// never only inside initDetailsExpander (which returns early unless a row-detail template is present).
		var start = body.indexOf("function initRowActions(");
		assertTrue(start >= 0, "missing initRowActions");
		var end = body.indexOf("function removeEl(", start);
		var host = body.substring(start, end > start ? end : start + 2000);
		assertTrue(host.contains("addEventListener(\"keydown\""), "initRowActions must bind a table-level keydown");
		assertTrue(host.contains("[data-juneau-pill]"), "initRowActions must dispatch [data-juneau-pill]");
		assertTrue(host.contains("activatePillAction("), host);
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
			var p = Path.of(basedir, "src/test/js/pill.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/pill.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/pill.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("pill-stdout-", ".json");
		var stderr = Files.createTempFile("pill-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("pill.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("pill.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or pill.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_clickSubmitsThroughTheHandler_noRowDetailTemplate() {
		var r = report();
		assertEquals(true, r.get("hasInit"));
		assertEquals(true, r.get("click_fetchIssued"));
		assertEquals("/x/ack", r.get("click_url"));
		assertEquals("POST", r.get("click_method"));
	}

	@Test void b02_keyboardEnterAndSpaceActivate_spacePreventsDefault() {
		var r = report();
		assertEquals(true, r.get("enter_prevented"));
		assertEquals(true, r.get("enter_fetchIssued"));
		assertEquals(true, r.get("space_prevented"));
		assertEquals(true, r.get("space_fetchIssued"));
		assertEquals(false, r.get("otherKey_prevented"));   // a non-activation key is left alone
		assertEquals(false, r.get("otherKey_fetchIssued"));
	}

	@Test void b03_presentDialogOpensModalInsteadOfSubmitting() {
		var r = report();
		assertEquals(false, r.get("dialog_fetchIssued"));   // confirm/dialog branch, not a direct submit
		assertEquals(true, r.get("dialog_layerOpen"));
	}

	@Test void b04_disabledInflightUnknownAndDisplayOnlyPillsAreInert() {
		var r = report();
		assertEquals(false, r.get("disabled_fetchIssued"));
		assertEquals(false, r.get("inflight_fetchIssued"));
		assertEquals(false, r.get("unknownAction_fetchIssued"));
		assertEquals(false, r.get("displayOnly_fetchIssued"));
	}

	@Test void b05_setActionRefEnabledReflectsDisabledStateViaAriaOnTheSpanButton() {
		var r = report();
		assertEquals(true, r.get("setDisabled_aria"));
		assertEquals(false, r.get("setDisabled_fetchIssued"));
		assertEquals(true, r.get("setEnabled_ariaCleared"));
		assertEquals(true, r.get("setEnabled_fetchIssued"));
	}

	@Test void b06_pillDispatchesEvenWhenDetailsExpanderAlsoWired() {
		assertEquals(true, report().get("withDetails_fetchIssued"));
	}
}
