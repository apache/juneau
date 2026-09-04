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
 * The dialog-scoped child-action catalog (WORK-J0513 Scope B): {@code ModalDef.childActions} lets a
 * {@code type="action"} input inside one dialog's form open a STACKED step that is not a row action, and therefore
 * appears in no row's action menu and in no ribbon.
 *
 * <p>Two layers:
 * <ul class='spaced-list'>
 * 	<li><b>Default-gate source pins</b> (a-group): the catalog's scoping is structural rather than a filter some
 * 		menu builder has to remember, the catalog reaches the click site as a threaded parameter rather than as
 * 		framework state on a DOM node, and the resolution ORDER puts the row-action check first so a served
 * 		payload can never shadow - and thereby bypass the gating of - a declared row action.
 * 	<li><b>A behavioral harness</b> ({@code dialog-child-catalog.cjs}) driving real clicks through both
 * 		row-painting paths.  Runs when {@code node} is on {@code PATH}.
 * </ul>
 */
class ViewsJs_DialogChildCatalog_Test extends TestBase {

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, () -> "missing classpath resource: " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException { return resource(ViewsMixin.VIEWS_JS_RESOURCE); }
	private static String rendersJs() throws IOException { return resource(ViewsMixin.RENDERS_JS_RESOURCE); }

	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found in juneau-views.js");
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Default-gate source pins
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * The catalog is DIALOG-SCOPED structurally: it is read from the per-open payload, so the row-action menu and
	 * the ribbon resolver never see it.
	 *
	 * <p>This is the same disjoint-catalog argument WORK-J0512 made for ribbon dialogs, and it matters for the same
	 * reason: an exclusion filter in {@code buildRowActionMenu} would be a thing to maintain, and a thing to
	 * forget.  Nothing to filter is stronger than a filter that works.
	 */
	@Test void a01_theCatalogIsReadFromThePerOpenPayloadOnly() throws Exception {
		var views = viewsJs();
		var overlay = functionBody(views, "\tfunction buildDialogOverlay(");
		assertTrue(overlay.contains("const childCatalog = modal?.childActions ?? null;"), overlay);
		// The three catalogs stay disjoint: neither existing resolver was widened to look at childActions.
		for (var fn : List.of("\tfunction dialogActionIsOpenable(", "\tfunction findRibbonDialogAction(",
				"\tfunction buildRowActionMenu(", "\tfunction activatePillAction(")) {
			var body = functionBody(views, fn);
			assertFalse(body.contains("childActions"),
				() -> fn + " must not see the dialog-scoped catalog:\n" + body);
			assertFalse(body.contains("childActionById"),
				() -> fn + " must not resolve against the dialog-scoped catalog:\n" + body);
		}
	}

	/**
	 * The catalog reaches the click site as a THREADED PARAMETER.
	 *
	 * <p>Deliberately not stamped on the dialog element - framework state on a DOM node is forgeable from the
	 * document - and deliberately not looked up from {@code topLayer()} at click time, which would assume the
	 * button's own dialog is topmost.  That is true today and is exactly the kind of assumption a later stacked
	 * surface breaks silently.
	 */
	@Test void a02_theCatalogIsThreadedNotStampedOnTheDom() throws Exception {
		var views = viewsJs();
		// One trailing optional parameter on each of the six functions in the chain, so every existing positional
		// call site keeps working unchanged.
		assertTrue(views.contains("function appendDialogForm(dialog, form, table, tr, ctx, seq, childCatalog) {"), views);
		assertTrue(views.contains("function appendSectionedDialogForm(dialog, form, table, tr, ctx, seq, childCatalog) {"), views);
		assertTrue(views.contains("function appendDialogFormRow(host, dialog, f, table, tr, ctx, seq, childCatalog) {"), views);
		assertTrue(views.contains("function paintFormControl(row, f, id, table, tr, ctx, childCatalog) {"), views);
		assertTrue(views.contains("function buildActionFormControl(f, table, tr, ctx, row, childCatalog) {"), views);
		assertTrue(views.contains("function openFormActionDialog(actionId, table, tr, ctx, childCatalog) {"), views);
		// Both row-painting paths thread it (a sectioned form is the second one, and is easy to miss).
		assertTrue(functionBody(views, "\tfunction appendDialogForm(")
			.contains("appendSectionedDialogForm(dialog, form, table, tr, ctx, seq, childCatalog)"), views);
		assertTrue(functionBody(views, "\tfunction appendSectionedDialogForm(")
			.contains("appendDialogFormRow(pane, dialog, field, table, tr, ctx, seq, childCatalog)"), views);
		// It is never stamped onto an element.
		var resolver = functionBody(views, "\tfunction childActionById(");
		assertFalse(resolver.contains("dataset"), resolver);
		assertFalse(resolver.contains("topLayer("), resolver);
	}

	/**
	 * The resolution ORDER, which is the whole security property.
	 *
	 * <p>The existing row-action check runs FIRST and unchanged, {@code enabledWhen} included, so a served payload
	 * can never shadow a declared row action.  A collision with a NON-dialog row action is a fail-closed miss and
	 * the child catalog is not consulted at all - rescuing it there would be the same shadowing hole through a
	 * narrower door.  Only an id that resolves to nothing today reaches the catalog.
	 */
	@Test void a03_theRowActionCheckRunsFirstAndTheCatalogIsTheFallback() throws Exception {
		var views = viewsJs();
		for (var fn : List.of("\tfunction buildActionFormControl(", "\tfunction openFormActionDialog(")) {
			var body = functionBody(views, fn);
			var rowCheck = body.indexOf("dialogActionIsOpenable(ctx, actionId)");
			var shadow = body.indexOf("rowActionIdExists(ctx, actionId)");
			var child = body.indexOf("childActionById(childCatalog, actionId)");
			assertTrue(rowCheck >= 0, () -> fn + " must still run the row-action check:\n" + body);
			assertTrue(shadow >= 0, () -> fn + " must refuse a non-dialog row-action collision:\n" + body);
			assertTrue(child >= 0, () -> fn + " must fall back to the dialog catalog:\n" + body);
			assertTrue(rowCheck < shadow && shadow < child,
				() -> fn + " must check the row catalog, then the shadow case, then the child catalog:\n" + body);
			// The gate on the row-action branch was NOT relaxed to accommodate the new fallback.
			assertTrue(body.contains("firstFailingRowActionRule(target, rowDataForTr(ctx, tr))")
				|| body.contains("firstFailingRowActionRule(target,"), body);
		}
		// rowActionIdExists is deliberately BROADER than dialogActionIsOpenable - it is what makes the
		// non-dialog collision refusable.
		var exists = functionBody(views, "\tfunction rowActionIdExists(");
		assertTrue(exists.contains("ctx?.viewDef?.rowActions"), exists);
		assertFalse(exists.contains("isDialogAction("),
			() -> "rowActionIdExists must see NON-dialog row actions too, or a gated direct-submit action can be "
				+ "shadowed:\n" + exists);
	}

	/** A `type=action` click on a dialog whose write is in flight or already a receipt is a no-op. */
	@Test void a04_aStaleChildClickOnABusyDialogIsANoOp() throws Exception {
		var body = functionBody(viewsJs(), "\tfunction openFormActionDialog(");
		assertTrue(body.contains("topDialogEl()?.dataset?.juneauDialogBusy === \"1\""), body);
		// The guard is the FIRST thing it does - after any resolution work it would be checking too late.
		var guard = body.indexOf("juneauDialogBusy");
		var resolve = body.indexOf("dialogActionIsOpenable(ctx, actionId)");
		assertTrue(guard >= 0 && resolve >= 0 && guard < resolve, body);
		// The SAME marker the hold paints, so there is no second piece of state to keep in sync.
		assertTrue(functionBody(viewsJs(), "\tfunction beginDialogResultHold(")
			.contains("ui.dialog.dataset.juneauDialogBusy = \"1\""), body);
	}

	/**
	 * The draft cap is measured on the ENCODED value.
	 *
	 * <p>Percent-encoding is up to 3x expansion per byte, so a cap applied to the raw JSON would let a ~2KB draft
	 * become a ~6KB query and blow a server or proxy request-line limit AFTER the client had already told the
	 * operator it was fine - a refusal arriving as an opaque transport error instead of a visible, actionable one.
	 */
	@Test void a05_theDraftCapIsMeasuredOnTheEncodedValue() throws Exception {
		var views = viewsJs();
		assertTrue(views.contains("const MAX_DRAFT_QUERY_BYTES = 2048;"), views);
		var fn = functionBody(views, "\tfunction withDraftQuery(");
		var encode = fn.indexOf("encodeURIComponent(JSON.stringify(drafts");
		var measure = fn.indexOf("draftQueryByteLength(encoded) > MAX_DRAFT_QUERY_BYTES");
		assertTrue(encode >= 0 && measure >= 0 && encode < measure,
			() -> "the cap must be applied to the ENCODED value, not the raw JSON:\n" + fn);
		// It returns a COPY rather than mutating the descriptor it was handed.
		assertTrue(fn.contains("const copy = {};"), fn);
		// Over the cap it returns null so the caller refuses VISIBLY - never a silently-truncated (and therefore
		// silently-wrong) prefill.
		assertTrue(fn.contains("return null;"), fn);
		assertTrue(functionBody(views, "\tfunction renderDialogDraftRefusal(")
			.contains("MAX_DRAFT_QUERY_BYTES"), views);
	}

	/** The child is adapted to the shape openActionDialog already takes, rather than reimplementing the machinery. */
	@Test void a06_aChildRidesTheSameReviewedDialogMachinery() throws Exception {
		var fn = functionBody(viewsJs(), "\tfunction openChildActionDialog(");
		assertTrue(fn.contains("openActionDialog({"), fn);
		assertTrue(fn.contains("type: \"dialog\""), fn);
		// No parallel fetch / submit / settle path.
		for (var forbidden : List.of("fetch(", "submitRowAction(", "settleActionResponse(", "pushLayer("))
			assertFalse(fn.contains(forbidden),
				() -> "a child must reuse the shared opener, not re-plumb it - it grew '" + forbidden + "':\n" + fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Behavioral harness
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		try {
			Files.writeString(rendersFile, rendersJs(), UTF_8);
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(rendersFile);
			Files.deleteIfExists(viewsFile);
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
			var p = Path.of(basedir, "src/test/js/dialog-child-catalog.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/dialog-child-catalog.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/dialog-child-catalog.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("dialog-child-catalog-stdout-", ".json");
		var stderr = Files.createTempFile("dialog-child-catalog-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("dialog-child-catalog.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("dialog-child-catalog.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or dialog-child-catalog.cjs not found — behavioral layer skipped");
		return report;
	}

	private static void assertAllTrue(String... keys) {
		var r = report();
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " was " + r.get(k) + " in " + r);
	}

	@Test void b00_theCatalogSeamIsWired() {
		assertEquals(true, report().get("hasCatalogSeam"), report()::toString);
	}

	/** The case that fails closed today: an id in neither row catalog, now reachable from its own dialog. */
	@Test void b01_aChildActionOpensAStackedStep() {
		assertAllTrue("open_parentOpened", "open_childButtonPainted", "open_childButtonEnabled",
			"open_childButtonNotMarkedMissing", "open_childStackedALayer", "open_childFormWasFetched",
			"open_noRefusalPainted");
	}

	/** Scoping, observed: the catalog rides the per-open payload, so no other resolver can see it. */
	@Test void b02_aChildActionIsInvisibleToEveryOtherCatalog() {
		assertAllTrue("scope_rowMenuExcludesChild", "scope_ribbonResolverExcludesChild",
			"scope_rowResolverExcludesChild");
		assertEquals("step1", report().get("scope_rowMenuIds"),
			() -> "the row menu must contain the row action and nothing from the dialog catalog: " + report());
	}

	/** Precedence: a declared DIALOG row action of the same id wins, and its enabledWhen gate still applies. */
	@Test void b03_aDeclaredRowActionWinsAnIdCollisionAndStaysGated() {
		assertAllTrue("precedence_rowActionWinsAndStaysGated", "precedence_gateReasonSurfaced",
			"precedence_reasonOnTheControlTitle", "precedence_notMarkedMissing");
	}

	/**
	 * A collision with a NON-dialog row action is a fail-closed refusal, not a fall-through.
	 *
	 * <p>The dangerous case is a gated direct-submit action - a Delete, say.  Rescuing the collision by consulting
	 * the child catalog would let a served payload shadow it and bypass its gating through a narrower door, which
	 * is the same hole the precedence rule exists to close.
	 */
	@Test void b04_aNonDialogRowActionCollisionIsRefusedNotRescued() {
		assertAllTrue("shadow_buttonPaintedDisabled", "shadow_buttonMarkedMissing", "shadow_ariaDisabled",
			"shadow_clickIsAVisibleRefusal", "shadow_refusalNamesTheId", "shadow_noChildOpened");
	}

	/** An id in neither catalog stays today's paint, and a click is a visible refusal - never a throw. */
	@Test void b05_anUnresolvableIdStaysFailClosed() {
		assertAllTrue("unknown_buttonDisabled", "unknown_buttonMarkedMissing", "unknown_clickIsAVisibleRefusal",
			"unknown_noChildOpened", "unknown_absentCatalogSameAsEmpty");
	}

	/** A child consumes a real depth slot, so the cap refuses in the same place with the same visible notice. */
	@Test void b06_aChildIsARealLayerAndTheDepthCapStillHolds() {
		assertAllTrue("depth_childIsARealLayer", "depth_thirdIsRefused", "depth_refusalPainted");
	}

	/** carryDrafts: without it a Review step opens EMPTY and loses everything the operator typed. */
	@Test void b07_carryDraftsCarriesTheParentsUnsubmittedValues() {
		assertAllTrue("drafts_queryParamPresent", "drafts_carriedTheEditedValue", "drafts_childOpened");
		assertEquals("/x/review-form?juneauDrafts=%7B%22note%22%3A%22edited-in-place%22%7D",
			report().get("drafts_childGetUrl"), report()::toString);
	}

	@Test void b08_theDraftCapIsEnforcedOnTheEncodedValue() {
		assertEquals(2048, report().get("cap_value"), report()::toString);
		assertAllTrue("cap_smallDraftPasses", "cap_overCapIsRefused", "cap_returnsACopyAndDoesNotMutate",
			"cap_appendsToAnExistingQuery");
		// The case that distinguishes the two measurements: raw JSON under the cap whose encoded form is over it.
		assertAllTrue("cap_rawJsonWouldHaveFit", "cap_encodedFormIsRefused");
	}

	/** Over the cap the child does NOT open, and the operator is told why rather than getting a wrong prefill. */
	@Test void b09_anOverCapDraftIsAVisibleRefusal() {
		assertAllTrue("capRefusal_noChildOpened", "capRefusal_visible", "capRefusal_namesTheCap");
	}

	@Test void b10_aChildClickOnABusyDialogIsANoOp() {
		assertAllTrue("busy_noChildStacked", "busy_noRefusalPainted", "busy_worksAgainOnceCleared");
	}

	/** The sectioned form is the second row-painting path, and threading it is easy to miss. */
	@Test void b11_aSectionedParentFormThreadsTheCatalogToo() {
		assertAllTrue("sectioned_buttonPainted", "sectioned_buttonEnabledFromCatalog",
			"sectioned_withoutCatalogStaysDisabled");
	}

	@Test void b12_theResolverHelpersInIsolation() {
		assertAllTrue("helpers_childActionByIdFindsFirst", "helpers_childActionByIdMissIsNull",
			"helpers_rowActionIdExistsSeesNonDialog", "helpers_dialogActionIsOpenableDoesNot",
			"helpers_rowActionIdExistsMiss", "helpers_rowActionIdExistsNullSafe");
	}
}
