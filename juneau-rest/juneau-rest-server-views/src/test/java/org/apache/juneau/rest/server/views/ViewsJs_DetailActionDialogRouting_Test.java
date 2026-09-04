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
 * Regression coverage: a detail-header/detail-panel ActionBar ActionRef click
 * ({@code handleDetailActionRefClick}, {@code juneau-views.js}) must honor a {@code present=dialog} action the
 * SAME way the row-action menu / cell-pill paths already do ({@code isDialogAction(action) ?
 * openActionDialog(...) : submitRowAction(...)} - see {@code buildRowActionMenu} / {@code activatePillAction}).
 *
 * <p>Before the fix, the detail path called {@code submitRowAction(action, table, parentTr, ctx)} directly and
 * unconditionally, so a dialog-declared detail action never opened its confirmation/form, never attached
 * {@code extra.targetId}, and never carried a server-minted idempotency key into the submit -
 * {@code submitActionDialog} (the ONLY place that attaches them) was skipped entirely.
 *
 * <p>Source-shape (always runs, no Node required) plus a behavioral Node harness
 * ({@code detail-action-dialog-routing.cjs}) that runs when {@code node} is on {@code PATH} (skipped otherwise -
 * no {@code -Pjs-tests} required, mirroring {@code ViewsJs_RowDetail_Test} / {@code ViewsJs_ContractVersion_Test}).
 */
class ViewsJs_DetailActionDialogRouting_Test extends TestBase {

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.RENDERS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** Extracts a named function's body: from `function <name>(` to the next top-level `\n\t}`. */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	@Test void a01_sourceShape_detailPathRoutesDialogDeclaredActionsThroughOpenActionDialog() throws Exception {
		var fnBody = functionBody(viewsJs(), "function handleDetailActionRefClick(");
		assertTrue(fnBody.contains("isDialogAction(action)"), fnBody);
		assertTrue(fnBody.contains("openActionDialog(action, table, parentTr, ctx)"), fnBody);
		// The non-dialog branch keeps going through the pre-existing direct submit, unchanged.
		assertTrue(fnBody.contains("submitRowAction(action, table, parentTr, ctx)"), fnBody);
		assertTrue(fnBody.contains("panel?._juneauParentTr"), fnBody);
	}

	/** {@code submitActionDialog} (the targetId/idempotency seam WORK-J0512 owns) must stay byte-identical. */
	@Test void a02_submitActionDialogIsUntouched_thisFixOnlyRoutesIntoIt() throws Exception {
		var fnBody = functionBody(viewsJs(), "function submitActionDialog(");
		assertTrue(fnBody.contains("const targetId = tr?.dataset?.juneauRowId ?? null;"), fnBody);
		assertTrue(fnBody.contains("if (modal?.idempotencyKey != null) extra.idempotencyKey = modal.idempotencyKey;"), fnBody);
		assertTrue(fnBody.contains("submitRowAction(action, table, tr, ctx, extra);"), fnBody);
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
			var p = Path.of(basedir, "src/test/js/detail-action-dialog-routing.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/detail-action-dialog-routing.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/detail-action-dialog-routing.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("detail-action-dialog-routing-stdout-", ".json");
		var stderr = Files.createTempFile("detail-action-dialog-routing-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("detail-action-dialog-routing.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("detail-action-dialog-routing.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null,
			"node not available or detail-action-dialog-routing.cjs not found — behavioral layer skipped");
		return report;
	}

	/** The core regression: a dialog-declared detail action must open the dialog, not fetch before confirmation. */
	@Test void b01_dialogDeclaredDetailAction_opensDialogInsteadOfSubmittingDirectly() {
		var r = report();
		assertEquals(0L, ((Number) r.get("dialogAction_fetchCallsBeforeConfirm")).longValue(),
			"clicking a present=dialog detail action must NOT fetch before the dialog is confirmed");
		assertEquals(true, r.get("dialogAction_dialogOpened"));
		assertEquals(true, r.get("dialogAction_backdropPortalledToBody"));
		assertEquals(true, r.get("dialogAction_ctxTracksDialog"));
		assertEquals(true, r.get("dialogAction_titleIsConfirmText"));
	}

	/** Confirming the opened dialog submits through the untouched submitActionDialog seam, carrying targetId. */
	@Test void b02_confirmingTheDialog_submitsThroughTheSeam_carryingTargetId() {
		var r = report();
		assertEquals(true, r.get("dialogAction_confirmBtnPresent"));
		assertEquals(true, r.get("dialogAction_submitFiredOnConfirm"));
		assertEquals(true, r.get("dialogAction_submitCarriesAction"));
		assertEquals(true, r.get("dialogAction_submitCarriesTargetId"),
			"submitActionDialog's targetId attachment must be reached, not bypassed");
		assertEquals(true, r.get("dialogAction_dialogClosedAfterConfirm"));
	}

	/** Control: a non-dialog detail action keeps going straight through submitRowAction, unchanged. */
	@Test void b03_nonDialogDetailAction_stillSubmitsDirectly_unaffectedByTheFix() {
		var r = report();
		assertEquals(1L, ((Number) r.get("nonDialogAction_fetchCallsAtClick")).longValue());
		assertEquals(true, r.get("nonDialogAction_noDialogOpened"));
		assertEquals(true, r.get("nonDialogAction_bodyIsBareAction"));
	}

	/** A form-bearing dialog action: the read-only confirmation GET runs first; the submit carries BOTH targetId
	 * AND the server-minted idempotencyKey the GET returned (HIGH-8) - exactly what Hank's report named missing. */
	@Test void b04_formBearingDialogAction_getsConfirmationFirst_thenCarriesIdempotencyKeyAndTargetId() {
		var r = report();
		assertEquals(true, r.get("formDialogAction_getFetchedForConfirmation"));
		assertEquals(true, r.get("formDialogAction_noPostBeforeConfirm"));
		assertEquals(true, r.get("formDialogAction_dialogOpened"));
		assertEquals(true, r.get("formDialogAction_postFiredOnConfirm"));
		assertEquals(true, r.get("formDialogAction_targetIdAttached"));
		assertEquals(true, r.get("formDialogAction_idempotencyKeyAttached"));
	}
}
