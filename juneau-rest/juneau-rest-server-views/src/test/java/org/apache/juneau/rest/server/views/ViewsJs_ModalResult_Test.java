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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Always-on source-shape coverage for the {@code juneau-views.js} declarative-modal, typed-action-result and
 * in-flight-row plumbing (TODO-416/417).  Mirrors {@link ViewsJs_RowActions_Test}'s served-script substring style:
 * proves the load-bearing pieces of the modal/result/in-flight contract are present in the shipped asset, without
 * booting a browser (the behavioral proof lives in the opt-in {@code ModalResult_BrowserTest} canary).
 */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_ModalResult_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String viewsJs() throws Exception {
		return c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	private static String fn(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> signature + " not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The typed action-result contract version is its OWN constant, not aliased to the view contract (MED-7)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_actionResultContractVersion_isItsOwnConstant() throws Exception {
		var body = viewsJs();
		// The runtime constant must equal the server's ActionResult.CONTRACT_VERSION, and must NOT be the view one.
		assertTrue(body.contains("JUNEAU_ACTION_RESULT_CONTRACT_VERSION = \"" + ActionResult.CONTRACT_VERSION + "\""), body);
		assertEquals("1", ActionResult.CONTRACT_VERSION);
		assertNotEquals(ViewDef.CONTRACT_VERSION, ActionResult.CONTRACT_VERSION);
	}

	@Test void a02_contractMismatchOn2xxRendersUnknown() throws Exception {
		// A 2xx typed result whose contractVersion differs is a visible, non-optimistic UNKNOWN - never silently misread.
		var body = viewsJs();
		var settle = fn(body, "function settleActionResponse(");
		assertTrue(settle.contains("result.contractVersion !== JUNEAU_ACTION_RESULT_CONTRACT_VERSION"), settle);
		assertTrue(settle.contains("outcome: \"unknown\""), settle);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) The modal-open confirmation renders TYPED FIELDS via textContent - never innerHTML / raw markup (BLK-1/MED-9)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_dialogOverlayUsesTextContentNeverInnerHtml() throws Exception {
		var body = viewsJs();
		var build = fn(body, "function buildDialogOverlay(");
		assertTrue(build.contains(".textContent"), build);
		assertFalse(build.contains(".innerHTML"), () -> "dialog overlay must never use innerHTML:\n" + build);
		assertFalse(build.contains("insertAdjacentHTML"), build);
		// The typed confirmation fields are painted as dt/dd label+value pairs.
		assertTrue(build.contains("juneau-view-dialog-fields"), build);
	}

	@Test void b02_settleAndOutcomePathsNeverUseInnerHtml() throws Exception {
		// The write-result render path (settle -> outcome banner) must not use an HTML sink; the pre-existing
		// innerHTML uses live only in the trusted icon/caret render helpers, never in the action-result path.
		var body = viewsJs();
		for (var sig : new String[]{"function settleActionResponse(", "function renderActionOutcome(",
				"function showActionDialog(", "function submitActionDialog(", "function openActionDialog("}) {
			var f = fn(body, sig);
			assertFalse(f.contains(".innerHTML"), () -> sig + " must never use innerHTML:\n" + f);
			assertFalse(f.contains("insertAdjacentHTML"), () -> sig + " must never use insertAdjacentHTML:\n" + f);
		}
	}

	@Test void b03_dialogIsOpenedForPresentDialogActions() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function isDialogAction("), body);
		assertTrue(body.contains("action.present === \"dialog\""), body);
		assertTrue(body.contains("function openActionDialog("), body);
		// The modal-open confirmation fetch is a read-only GET.
		var open = fn(body, "function openActionDialog(");
		assertTrue(open.contains("method: \"GET\""), open);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) A non-2xx is a VISIBLE transport refusal read from the boundary envelope - not "HTTP 200 + schema" (HIGH-3)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_nonOkReadsLoopbackBoundaryEnvelope() throws Exception {
		var body = viewsJs();
		var settle = fn(body, "function settleActionResponse(");
		assertTrue(settle.contains("! resp.ok"), settle);
		assertTrue(settle.contains("X-Loopback-Boundary"), settle);
		assertTrue(settle.contains("transport: true"), settle);
	}

	@Test void c02_transportRefusalPrefersBoundaryReasonThenEnvelope() throws Exception {
		var body = viewsJs();
		var t = fn(body, "function transportRefusal(");
		assertTrue(t.contains("boundaryReason"), t);
		assertTrue(t.contains("envelope"), t);
		// The header name the boundary filter actually sets on a rejection.
		assertEquals("X-Loopback-Boundary", LoopbackBoundaryFilter.REJECTION_HEADER);
	}

	@Test void c03_transportStatusMessagesCoverTheMappedCodes() throws Exception {
		// 403/415/421 all map to a visible non-optimistic message.
		var body = viewsJs();
		var m = fn(body, "function transportStatusMessage(");
		assertTrue(m.contains("403"), m);
		assertTrue(m.contains("415"), m);
		assertTrue(m.contains("421"), m);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) All four outcomes render; the outcome banner is visible + assistive
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_renderActionOutcomePaintsVisibleBanner() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function renderActionOutcome("), body);
		assertTrue(body.contains("juneau-view-action-outcome"), body);
		var r = fn(body, "function renderActionOutcome(");
		assertTrue(r.contains(".textContent"), r);
		assertTrue(r.contains("\"status\""), r);   // success -> role=status
		assertTrue(r.contains("\"alert\""), r);     // everything else -> role=alert
	}

	@Test void d02_outcomeMessageCoversAllFourSyncOutcomes() throws Exception {
		var body = viewsJs();
		var m = fn(body, "function actionOutcomeMessage(");
		for (var o : new String[]{"success", "failure", "refusal"})
			assertTrue(m.contains("case \"" + o + "\":"), () -> o + " not handled:\n" + m);
		// unknown is the default branch.
		assertTrue(m.contains("Outcome unknown"), m);
	}

	@Test void d03_normalizeOutcomeFallsBackToUnknown() throws Exception {
		var body = viewsJs();
		var n = fn(body, "function normalizeOutcome(");
		assertTrue(n.contains("\"unknown\""), n);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) 417: in-flight marker set on the SYNC write, cleared on EVERY terminal outcome (incl. UNKNOWN) (MED-4)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_setRowInFlightTogglesMarkerAndDisablesTrigger() throws Exception {
		var body = viewsJs();
		var s = fn(body, "function setRowInFlight(");
		assertTrue(s.contains("setAttribute(\"data-juneau-inflight\""), s);
		assertTrue(s.contains("removeAttribute(\"data-juneau-inflight\")"), s);
		assertTrue(s.contains("trigger.disabled"), s);
	}

	@Test void e02_settleClearsMarkerFirstOnEveryOutcome() throws Exception {
		// The very first statement of settle must clear the marker so polling ALWAYS resumes - the UNKNOWN clear is
		// a hard requirement (hasInFlightRow/initPolling freeze polling while ANY marker is set).
		var body = viewsJs();
		var settle = fn(body, "function settleActionResponse(");
		var open = settle.indexOf("{");
		var firstStmt = settle.substring(open + 1, settle.indexOf(";", open));
		assertTrue(firstStmt.contains("setRowInFlight(tr, false)"),
			() -> "settleActionResponse must clear the in-flight marker FIRST:\n" + settle);
	}

	@Test void e03_submitSetsMarkerAndNetworkFailureClearsIt() throws Exception {
		var body = viewsJs();
		var submit = fn(body, "function submitRowAction(");
		assertTrue(submit.contains("setRowInFlight(tr, true)"), submit);
		// A network-level failure is itself a terminal outcome: clear the marker (polling resumes) + visible refusal.
		assertTrue(submit.contains("setRowInFlight(tr, false)"), submit);
		assertTrue(submit.contains("request-failed"), submit);
	}

	@Test void e04_pollingFreezeStillKeyedOnTheSameMarker() throws Exception {
		// The marker attribute the 417 lifecycle toggles is the exact one hasInFlightRow reads to freeze polling.
		var body = viewsJs();
		var h = fn(body, "function hasInFlightRow(");
		assertTrue(h.contains("data-juneau-inflight"), h);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Re-render from the authoritative result payload (MERGE_ROW)
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_mergeRowFromResultReRendersFromPayload() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function mergeRowFromResult("), body);
		var apply = fn(body, "function applySuccessBehavior(");
		assertTrue(apply.contains("mergeRow"), apply);
		assertTrue(apply.contains("result.row"), apply);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) The submit carries the idempotency key + target id bound at modal-open
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_submitDialogCarriesIdempotencyKeyAndTargetId() throws Exception {
		var body = viewsJs();
		var s = fn(body, "function submitActionDialog(");
		assertTrue(s.contains("idempotencyKey"), s);
		assertTrue(s.contains("targetId"), s);
	}
}
