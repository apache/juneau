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
 * The in-dialog result <b>receipt</b> (WORK-J0513 Scope A): a dialog that opted in with
 * {@code ModalDef.keepOpenOnSubmit} keeps its layer across the submit, and the runtime paints the write's result
 * into that already-open dialog instead of closing it and painting a row banner.
 *
 * <p>Two layers, the same split {@code ViewsJs_RibbonDialog_Test} uses:
 * <ul class='spaced-list'>
 * 	<li><b>Default-gate source pins</b> (a-group): the <b>coupling guard</b> that keeps
 * 		{@code submitActionDialog} byte-identical, the <b>enforcement by absence</b> that makes the receipt
 * 		non-submittable, the hold's one-settle discipline, and the narrow allowlist of sites permitted to tear the
 * 		dialog stack down automatically.
 * 	<li><b>A behavioral harness</b> ({@code dialog-receipt.cjs}) walking all nine submit terminals and checking
 * 		the disposition each settles to.  The interesting failures here - a stuck busy modal, a Confirm left
 * 		disabled on a retryable refusal, a receipt painted over a still-populated form, one outcome rendered twice
 * 		- are DOM-shaped and invisible to a source pin.  Runs when {@code node} is on {@code PATH}.
 * </ul>
 */
class ViewsJs_DialogReceipt_Test extends TestBase {

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

	private static int countOf(String body, String needle) {
		var n = 0;
		for (var i = body.indexOf(needle); i >= 0; i = body.indexOf(needle, i + needle.length()))
			n++;
		return n;
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Default-gate source pins
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * The COUPLING GUARD, and the single most load-bearing pin in this file.
	 *
	 * <p>The receipt needs to know, at settle time, which dialog to paint into - and the obvious way to arrange
	 * that is a callback parameter on the submit.  {@code submitActionDialog} is the one function EVERY
	 * {@code present=dialog} submit goes through, and WORK-J0512 put its {@code targetId} precedence rule and that
	 * rule's tripwire there precisely because it is the only path.  Threading a receipt parameter through it would
	 * re-open a reviewed function to add a feature that has nothing to do with what it decides.  The coupling
	 * therefore runs through a ctx-scoped hold registered at CLICK time instead, which leaves this function
	 * byte-identical.
	 */
	@Test void a01_submitActionDialogIsUntouched() throws Exception {
		var fn = functionBody(viewsJs(), "\tfunction submitActionDialog(");
		// The J0512 precedence rule and its opt-in gate, quoted verbatim.
		assertTrue(fn.contains("const targetId = (modal?.selfTargeted && modal?.idempotencyKey != null)"), fn);
		assertTrue(fn.contains(": (tr?.dataset?.juneauRowId ?? null);"), fn);
		// Nothing about the receipt reached it: no hold, no callback, no result-form parameter.
		for (var forbidden : List.of("_resultHold", "beginDialogResultHold", "settleDialogResultHold",
				"resultForm", "keepOpenOnSubmit", "onResult", "callback")) {
			assertFalse(fn.contains(forbidden),
				() -> "submitActionDialog must stay byte-identical - it grew '" + forbidden + "':\n" + fn);
		}
		// Its signature is unchanged too: no trailing parameter was appended.
		assertTrue(viewsJs().contains("function submitActionDialog(modal, action, table, tr, ctx, fields) {"), fn);
	}

	/** {@code startJobStream} is likewise untouched - T5 closes the dialog BEFORE handing off to it. */
	@Test void a02_startJobStreamIsUntouched() throws Exception {
		var fn = functionBody(viewsJs(), "\tfunction startJobStream(");
		for (var forbidden : List.of("_resultHold", "settleDialogResultHold", "resultForm", "keepOpenOnSubmit"))
			assertFalse(fn.contains(forbidden), () -> "startJobStream grew '" + forbidden + "':\n" + fn);
		var settle = functionBody(viewsJs(), "\tfunction settleActionResponse(");
		// The ORDER is load-bearing: everything startJobStream paints is row-anchored, so a surviving backdrop
		// would hide the whole progress affordance behind a modal that can never settle.
		var close = settle.indexOf("settleDialogResultHold(ctx, \"closeCommitted\")");
		var stream = settle.indexOf("startJobStream(started");
		assertTrue(close >= 0 && stream >= 0, settle);
		assertTrue(close < stream, () -> "T5 must close the held dialog BEFORE streaming:\n" + settle);
	}

	/**
	 * The receipt painter enforces its non-submittability by ABSENCE.
	 *
	 * <p>A structural marker would enforce by veto - a later change can delete the check, and a DOM attribute a
	 * fixture can forge.  An absence is what a test can pin cheaply and what a careless edit cannot silently
	 * undo: this painter creates no confirm control and names no submit function, so there is nothing to disable
	 * and nothing to re-enable.
	 */
	@Test void a03_theReceiptPainterCreatesNoSubmitControl() throws Exception {
		var fn = functionBody(viewsJs(), "\tfunction paintDialogReceipt(");
		for (var forbidden : List.of("juneau-view-dialog-confirm", "submitActionDialog", "submitRowAction",
				"appendDialogForm", "validateDialogForm", "collectDialogFormFields", "innerHTML"))
			assertFalse(fn.contains(forbidden),
				() -> "the receipt host must be non-submittable BY CONSTRUCTION - it grew '" + forbidden + "':\n" + fn);
		// It tears the submitted write's body down rather than painting over it: a terminal post-commit surface
		// must not carry the live, editable inputs of a write that already landed.
		assertTrue(fn.contains("removeDialogActionsRow(dialog)"), fn);
		assertTrue(fn.contains("teardownCommittedWriteBody(dialog)"), fn);
		// And it opens no layer - the forbidden shape this whole mechanism exists to avoid.
		for (var forbidden : List.of("pushLayer(", "showActionDialog(", "buildDialogOverlay(", "openActionDialog("))
			assertFalse(fn.contains(forbidden), () -> "a receipt is a SWAP, not a new layer - it grew '"
				+ forbidden + "':\n" + fn);
	}

	/** The body teardown must take the FORM wrapper, not merely the actions row. */
	@Test void a04_theCommittedWriteBodyTeardownTakesTheInputsToo() throws Exception {
		var fn = functionBody(viewsJs(), "\tfunction teardownCommittedWriteBody(");
		// Removing the actions row alone is not sufficient: the submitted inputs live in a SEPARATE wrapper, and
		// leaving it standing renders the server's result underneath a still-editable form for a committed write.
		assertTrue(fn.contains("juneau-view-dialog-form"), fn);
		assertTrue(fn.contains("juneau-view-dialog-fields"), fn);
	}

	/**
	 * One hand-off per submit, guaranteed by clearing the hold FIRST.
	 *
	 * <p>The submit graph has nine terminals; they are mutually-exclusive paths through one {@code submitRowAction}
	 * call, so clearing before dispatching means a second settle on the same hold finds nothing. That ordering is
	 * the whole mechanism, so it is pinned as an ordering and not just as a presence.
	 */
	@Test void a05_theHoldIsClearedBeforeItIsDispatched() throws Exception {
		var fn = functionBody(viewsJs(), "\tfunction settleDialogResultHold(");
		var read = fn.indexOf("const hold = ctx?._resultHold");
		var clear = fn.indexOf("ctx._resultHold = null");
		var live = fn.indexOf("heldLayerIsLive(ctx, hold)");
		assertTrue(read >= 0 && clear >= 0 && live >= 0, fn);
		assertTrue(read < clear && clear < live,
			() -> "the hold must be READ, then CLEARED, then dispatched - or a terminal can settle twice:\n" + fn);
	}

	/**
	 * Every terminal in the submit graph hands off exactly once, and the F19 exemption is per-disposition.
	 *
	 * <p>{@code closeCommitted} returns {@code false} on purpose: the dialog is gone, so the row / ribbon banner is
	 * the only surviving signal and suppressing it would leave a committed write with no visible confirmation
	 * anywhere.  Every other disposition returns {@code true}, because the dialog is still on screen and painting
	 * the row banner too would render one outcome twice, once behind a backdrop.
	 */
	@Test void a06_allNineTerminalsHandOffExactlyOnce() throws Exception {
		var views = viewsJs();
		var submit = functionBody(views, "\tfunction submitRowAction(");
		var settle = functionBody(views, "\tfunction settleActionResponse(");
		// T1 (client refuse) + T2 (network reject).
		assertEquals(2, countOf(submit, "settleDialogResultHold(ctx, "),
			() -> "submitRowAction has exactly two terminals:\n" + submit);
		// T3..T8 - six terminals, six hand-offs, one each.
		assertEquals(6, countOf(settle, "settleDialogResultHold(ctx, "),
			() -> "settleActionResponse has exactly six terminals:\n" + settle);
		// T9 is shared by BOTH readBodyText chains, so it is one function reached from two `.catch` arms.
		assertEquals(2, countOf(settle, "settleBodyReadFailure(action, table, tr, ctx)"),
			() -> "both body-read chains must route to T9, or a held dialog hangs forever:\n" + settle);
		assertEquals(1, countOf(functionBody(views, "\tfunction settleBodyReadFailure("),
			"settleDialogResultHold(ctx, "), views);
		// The dispositions actually used, and no others.
		for (var d : List.of("\"retryable\"", "\"terminal\"", "\"closeCommitted\"", "\"receipt\""))
			assertTrue(views.contains("settleDialogResultHold(ctx, " + d + ")")
				|| views.contains("settleDialogResultHold(ctx, disposition)"), d);
		var hold = functionBody(views, "\tfunction settleDialogResultHold(");
		assertTrue(hold.contains("if (disposition === \"closeCommitted\")"), hold);
		assertTrue(hold.contains("closeActionDialog(ctx);\n\t\t\treturn false;"),
			() -> "closeCommitted must return FALSE so the caller's row painter still runs:\n" + hold);
	}

	/**
	 * The receipt terminal FETCHES before it settles.
	 *
	 * <p>Settling first would clear {@code ctx._resultHold} before the GET resolved, so every arm inside
	 * {@code fetchResultForm} would take the layer-gone escape and a committed write with a perfectly good receipt
	 * payload would paint no receipt at all - silently.
	 */
	@Test void a07_theReceiptTerminalFetchesBeforeItSettles() throws Exception {
		var settle = functionBody(viewsJs(), "\tfunction settleActionResponse(");
		var fetch = settle.indexOf("fetchResultForm(result.resultForm");
		assertTrue(fetch >= 0, settle);
		// The receipt branch returns immediately after the fetch: it must not also fall through to a settle.
		var tail = settle.substring(fetch);
		var nextSettle = tail.indexOf("settleDialogResultHold(");
		var ret = tail.indexOf("return;");
		assertTrue(ret >= 0 && (nextSettle < 0 || ret < nextSettle),
			() -> "the receipt branch must fetch and RETURN, never fetch and then settle:\n" + tail);
		// And the fetch is gated on a HELD success carrying a resultForm - never on a failure, refusal or unknown,
		// so no result host can appear for a write that did not commit.
		assertTrue(settle.contains("if (held && outcome === \"success\" && ! isBlankToken(result.resultForm))"),
			settle);
	}

	/**
	 * The automatic-teardown allowlist.
	 *
	 * <p>Exactly three NEW sites may call {@code closeActionDialog(} - the {@code closeCommitted} branch, the
	 * receipt's Close-button wiring, and the shared Close-only row helper - plus the pre-existing grandfathered
	 * {@code teardownTable} caller.  Every OTHER terminal path must end with the dialog OPEN and delegate its exit
	 * to a Close button the operator presses, because a terminal that closes the stack behind the operator's back
	 * destroys the message it was about to show them.
	 */
	@Test void a08_noTerminalPathTearsTheStackDownAutomatically() throws Exception {
		var views = viewsJs();
		// The BAN, stated as absences on the automatic paths: none of these may close the stack, because a
		// terminal that closes it behind the operator's back destroys the message it was about to show them.
		for (var fn : List.of("\tfunction settleBodyReadFailure(", "\tfunction fetchResultForm(",
				"\tfunction renderHeldResultNotice(", "\tfunction renderResultFormIgnored(",
				"\tfunction beginDialogResultHold(", "\tfunction removeDialogActionsRow(",
				"\tfunction teardownCommittedWriteBody(", "\tfunction restoreHeldChildActions(")) {
			var body = functionBody(views, fn);
			assertFalse(body.contains("closeActionDialog("),
				() -> fn + " must not tear the stack down automatically:\n" + body);
		}
		// The three permitted NEW sites.  Two of them are CLICK LISTENERS - a Close button the operator presses is
		// that operator's exit, not an automatic teardown - and the third is the one disposition whose write is
		// known-committed and whose dialog is therefore meant to go.
		assertTrue(functionBody(views, "\tfunction appendDialogCloseOnlyRow(")
			.contains("close.addEventListener(\"click\", function () { closeActionDialog(ctx); });"), views);
		var receipt = functionBody(views, "\tfunction paintDialogReceipt(");
		assertTrue(receipt.contains("close.addEventListener(\"click\", function () { closeActionDialog(ctx); });"),
			receipt);
		assertEquals(1, countOf(receipt, "closeActionDialog("),
			() -> "the receipt's ONLY teardown is its Close button's listener:\n" + receipt);
		var settle = functionBody(views, "\tfunction settleDialogResultHold(");
		assertEquals(1, countOf(settle, "closeActionDialog("),
			() -> "exactly one disposition may close the stack:\n" + settle);
		var branch = settle.indexOf("if (disposition === \"closeCommitted\")");
		assertTrue(branch >= 0 && branch < settle.indexOf("closeActionDialog("),
			() -> "the one teardown must sit inside the closeCommitted branch:\n" + settle);
	}

	/** Every terminal Close-only row comes from the ONE shared helper, so the operator's exit is identical. */
	@Test void a09_terminalSurfacesShareOneCloseOnlyRow() throws Exception {
		var views = viewsJs();
		var helper = functionBody(views, "\tfunction appendDialogCloseOnlyRow(");
		assertTrue(helper.contains("juneau-view-dialog-actions"), helper);
		assertTrue(helper.contains("dialog-result-close"), helper);
		assertFalse(helper.contains("juneau-view-dialog-confirm"),
			() -> "a Close-only row is Close ONLY:\n" + helper);
		// The `terminal` disposition removes the live actions row FIRST, so Confirm is detached (listener and all)
		// rather than merely disabled, and then appends the shared row.
		var settle = functionBody(views, "\tfunction settleDialogResultHold(");
		var remove = settle.indexOf("removeDialogActionsRow(hold.dialog)");
		var append = settle.indexOf("appendDialogCloseOnlyRow(hold.dialog, ctx)");
		assertTrue(remove >= 0 && append >= 0 && remove < append, settle);
		assertTrue(functionBody(views, "\tfunction removeDialogActionsRow(").contains("removeEl(el)"), views);
	}

	/** Painters use createElement + textContent only - live result data is attacker-influenceable. */
	@Test void a10_noInnerHtmlInAnyNewPainter() throws Exception {
		var views = viewsJs();
		for (var fn : List.of("\tfunction paintDialogReceipt(", "\tfunction buildModalFieldValueNode(",
				"\tfunction buildModalFieldList(", "\tfunction buildFieldCopyButton(",
				"\tfunction appendDialogCloseOnlyRow(", "\tfunction renderResultFormIgnored(")) {
			var body = functionBody(views, fn);
			assertFalse(body.contains("innerHTML"), () -> fn + " must not assign innerHTML:\n" + body);
		}
		assertTrue(functionBody(views, "\tfunction buildModalFieldValueNode(").contains("pre.textContent = value"),
			views);
	}

	/** The copy affordance is feature-detected, caught, and wrapped - it can never throw out of a click handler. */
	@Test void a11_theCopyButtonCannotThrow() throws Exception {
		var fn = functionBody(viewsJs(), "\tfunction buildFieldCopyButton(");
		assertTrue(fn.contains("typeof clip?.writeText !== \"function\""), fn);
		assertTrue(fn.contains(".catch(function ()"), fn);
		assertTrue(fn.contains("try {") && fn.contains("catch (e)"), fn);
	}

	/** The three constants that carry the terminal wording, so a diagnosis is never an empty banner. */
	@Test void a12_theTerminalNoticesAreNamedConstants() throws Exception {
		var views = viewsJs();
		for (var c : List.of("REQUEST_UNCONFIRMED_NOTICE", "BODY_UNREADABLE_NOTICE",
				"RESULT_FORM_UNAVAILABLE_NOTICE", "RESULT_FORM_REFUSED_NOTICE"))
			assertTrue(views.contains("const " + c + " ="), () -> "missing named notice constant: " + c);
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
			var p = Path.of(basedir, "src/test/js/dialog-receipt.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/dialog-receipt.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/dialog-receipt.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("dialog-receipt-stdout-", ".json");
		var stderr = Files.createTempFile("dialog-receipt-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("dialog-receipt.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("dialog-receipt.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or dialog-receipt.cjs not found — behavioral layer skipped");
		return report;
	}

	private static void assertAllTrue(String... keys) {
		var r = report();
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " was " + r.get(k) + " in " + r);
	}

	@Test void b00_theReceiptSeamIsWired() {
		assertEquals(true, report().get("hasReceiptSeam"), report()::toString);
	}

	/** T8-receipt: the swap.  Same layer, no submit controls, the submitted form gone. */
	@Test void b01_aHeldSuccessWithAResultFormSwapsInPlace() {
		assertAllTrue("receipt_dialogOpened", "receipt_stillOpenAtSameDepth", "receipt_marked",
			"receipt_titleFromPayload", "receipt_fieldsRepainted", "receipt_exactlyOneFieldList",
			"receipt_closeButtonPresent", "receipt_closeTearsDownTheStack");
	}

	/** The enforcement, observed rather than asserted about the source: there is nothing to submit with. */
	@Test void b02_theReceiptHostCarriesNoSubmitControls() {
		assertAllTrue("receipt_noConfirmButton", "receipt_noInputsAtAll", "receipt_submittedFormTornDown");
	}

	@Test void b03_aReceiptSuppressesTheRowBannerAndClearsBusy() {
		// The dialog is on screen, so the outcome belongs in it - rendering the row banner too would show one
		// outcome twice, once behind a backdrop.
		assertAllTrue("receipt_noRowBanner", "receipt_busyMarkerCleared");
	}

	@Test void b04_codeKindFieldsPaintAsSelectablePreWithCopy() {
		assertAllTrue("receipt_codeFieldIsPre", "receipt_codeFieldHasCopyButton",
			"kinds_ddCount", "kinds_plainIsText", "kinds_codeIsPre", "kinds_codeHasCopy",
			"kinds_explicitTextIsText", "kinds_allowlistIsExactlyTwo");
		// An UNRECOGNIZED token falls back to text rather than being trusted - the runtime half of the
		// allowlist that ModalDef.validate enforces server-side.
		assertAllTrue("kinds_unknownFallsBackToText");
	}

	/** T8-commit / T6: with the dialog gone, the row banner is the only surviving signal and must still render. */
	@Test void b05_aHeldSuccessWithoutAResultFormClosesAndStillPaintsTheRow() {
		assertAllTrue("commit_dialogClosed", "commit_noDialogInDom", "commit_rowOutcomeRendered", "commit_noReceipt");
		assertEquals("success", report().get("commit_rowOutcomeState"), report()::toString);
	}

	/**
	 * F4: a resultForm on a dialog that never opted in.
	 *
	 * <p>The write SUCCEEDED, so the success banner stays byte-identical and this adds exactly one non-alarming
	 * {@code role=status} diagnostic.  Never a new layer (the forbidden shape), and never a refusal - which would
	 * make a successful write look failed for what is purely a consumer-authoring mistake.
	 */
	@Test void b06_anUnheldResultFormIsDiagnosedNotOpened() {
		assertAllTrue("f4_dialogClosedAsToday", "f4_noFollowUpGet", "f4_successBannerUnchanged",
			"f4_diagnosticPresent", "f4_diagnosticIsStatusNotAlert", "f4_noNewLayer");
	}

	/** F1c: a receipt payload carrying a form is the forbidden shape arriving through the receipt door. */
	@Test void b07_aReceiptPayloadCarryingAFormIsRefusedWithItsOwnWording() {
		assertAllTrue("f1c_stillOpen", "f1c_notPaintedAsReceipt", "f1c_noInputsPainted", "f1c_closeOnlyRow",
			"f1c_noConfirm", "f1c_busyCleared");
		var r = report();
		// Its own wording, not folded into the transport-failure message: a consumer authoring bug that reads as
		// a network blip is a bug nobody fixes.
		assertEquals("The action completed, but its result was not shown: a result form must not carry inputs.",
			r.get("f1c_noticeText"), r::toString);
	}

	/**
	 * F12: the follow-up read failed three different ways, all of them post-commit.
	 *
	 * <p>The rejected-promise arm matters as much as the other two: without it a single dropped packet on the
	 * follow-up read leaves the promise rejected, the terminal unreached, and Confirm stuck disabled + busy on a
	 * write that had ALREADY COMMITTED.
	 */
	@Test void b08_aFailedReceiptReadIsTerminalAndNeverStuck() {
		assertAllTrue("f12_stillOpen", "f12_closeOnlyRow", "f12_busyCleared", "f12_submittedFormTornDown",
			"f12reject_stillOpen", "f12reject_closeOnlyRow", "f12reject_busyCleared",
			"f12reject_noConfirmStuckDisabled", "f12parse_closeOnlyRow", "f12parse_notPaintedAsReceipt");
		var r = report();
		var expected = "The action completed, but its result could not be loaded.";
		for (var k : List.of("f12_noticeText", "f12reject_noticeText", "f12parse_noticeText"))
			assertEquals(expected, r.get(k), () -> k + " in " + r);
	}

	/** T1: nothing was even sent, so the operator can fix the declaration and press Confirm again. */
	@Test void b09_aPreflightClientRefusalIsRetryable() {
		assertAllTrue("t1_nothingSent", "t1_stillOpen", "t1_noticePresent", "t1_confirmReEnabled",
			"t1_busyCleared", "t1_noCloseOnlyRow", "t1_noRowBanner", "t1_formStillStanding");
	}

	/**
	 * T2: a network reject is AMBIGUOUS, not retryable.
	 *
	 * <p>{@code fetch} rejects on a connection dropped AFTER the request was fully sent and processed, so this
	 * does not prove the write didn't land - re-enabling Confirm would hand the operator a one-click duplicate of
	 * a non-idempotent write.
	 */
	@Test void b10_aNetworkRejectIsTerminalNotRetryable() {
		assertAllTrue("t2_stillOpen", "t2_noConfirmButton", "t2_closeOnlyRow", "t2_busyCleared",
			"t2_formStillStanding", "t2_noRowBanner");
		assertEquals("The request was sent but no response came back.  It may or may not have been applied - "
			+ "re-check before retrying.", report().get("t2_noticeText"), report()::toString);
	}

	/** T4 / T8-refusal: a gate said no and nothing was written, so retry is safe and is the point (F11). */
	@Test void b11_refusalsReEnableConfirm() {
		assertAllTrue("t4_stillOpen", "t4_noticePresent", "t4_confirmReEnabled", "t4_noCloseOnlyRow",
			"t4_busyCleared", "t8refuse_confirmReEnabled", "t8refuse_noCloseOnlyRow", "t8refuse_noticePresent");
	}

	@Test void b12_aTypedFailureIsTerminal() {
		assertAllTrue("t8fail_stillOpen", "t8fail_terminalCloseOnly", "t8fail_noConfirm", "t8fail_noticePresent");
	}

	/** A resultForm on a NON-success outcome is ignored outright: no receipt for a write that did not happen. */
	@Test void b13_aResultFormOnANonSuccessOutcomeIsIgnored() {
		assertAllTrue("failResultForm_noGetIssued", "failResultForm_noReceiptPainted",
			"failResultForm_terminalCloseOnly", "failResultForm_noDiagnostic");
	}

	/** T5: the close happens first, so the row-anchored job affordance is not painted behind a live backdrop. */
	@Test void b14_aJobPointerClosesTheDialogBeforeStreaming() {
		assertAllTrue("t5_dialogClosed", "t5_noDialogInDom", "t5_rowAnchoredSignalVisible",
			"t5_rowSignalIsNotInADialog");
	}

	/**
	 * Cancel stays enabled during the hold, and that is safe.
	 *
	 * <p>Escape pops the top layer through the shared stack regardless, so the design has to tolerate "dialog gone
	 * before settle" anyway; given that, disabling Cancel would only remove the operator's obvious exit.  The
	 * layer-gone case is handled at settle time by falling back to the row banner.
	 */
	@Test void b15_cancellingMidFlightFallsBackToTheRowBanner() {
		assertAllTrue("cancel_layerGone", "cancel_outcomeFellBackToRow", "cancel_noThrowAndNoDialog");
	}

	@Test void b16_aHoldSettlesExactlyOnce() {
		assertAllTrue("once_holdRegistered", "once_confirmDisabledAtClickTime", "once_busyMarked",
			"once_firstSettleHandsOff", "once_holdCleared", "once_secondSettleIsANoOp",
			"once_closeCommittedDoesNotSuppressRowPaint", "once_noHoldIsFalse", "once_noCtxIsFalse");
	}

	/**
	 * Child-action buttons are disabled for the duration of the hold, and restored to their PRIOR state.
	 *
	 * <p>Recording which were already disabled is what makes the restore safe: it can never enable a child that
	 * was gated before the submit started.  They are disabled at all because a live child button during the
	 * in-flight parent write lets an operator push a child layer onto a dialog that is about to be swapped into a
	 * receipt.
	 */
	@Test void b17_childActionButtonsAreHeldAndRestoredToTheirPriorState() {
		assertAllTrue("children_liveOneDisabled", "children_gatedOneStaysDisabled", "children_liveOneRestored",
			"children_gatedOneStillDisabled");
	}
}
