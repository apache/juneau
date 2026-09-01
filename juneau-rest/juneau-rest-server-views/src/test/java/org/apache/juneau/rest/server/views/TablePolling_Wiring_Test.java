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
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Table polling + visible staleness indicator: Option-A (content-substring + function-body-extraction) coverage
 * for {@code juneau-views.js}/{@code juneau-views.css}, mirroring {@code PagingPill_Wiring_Test}'s established
 * idiom.
 */
@SuppressWarnings({
	"resource", // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
	"java:S5976" // Each test targets a distinct function/behavior via the shared functionBody(...) idiom; collapsing
					// into one @ParameterizedTest would obscure which specific behavior failed.
})
class TablePolling_Wiring_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient cWithMixin = MockRestClient.buildLax(WithMixin.class);

	/** Extracts a named function's body: from `function <name>(` to the next top-level `\n\t}`. */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pure layer: MIN_POLL_INTERVAL_MS / clampPollInterval / formatStalenessAge
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_minPollIntervalMs_mirrorsServerFloor() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("var MIN_POLL_INTERVAL_MS = 5000;"), body);
		assertEquals(5000L, ViewDef.MIN_POLL_INTERVAL_MS);
	}

	@Test void a02_clampPollInterval_flooredNotRejected() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function clampPollInterval(");
		assertTrue(fnBody.contains("Math.max(ms, MIN_POLL_INTERVAL_MS)"), fnBody);
	}

	@Test void a03_formatStalenessAge_boundaries() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function formatStalenessAge(");
		assertTrue(fnBody.contains("\"just now\""), fnBody);
		assertTrue(fnBody.contains("\"s ago\""), fnBody);
		assertTrue(fnBody.contains("\"m ago\""), fnBody);
		assertTrue(fnBody.contains("\"h ago\""), fnBody);
		// Pure - reads no Date.now()/clock itself; the caller supplies the elapsed ms.
		assertFalse(fnBody.contains("Date.now()"), fnBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Binding layer: hasInFlightRow / buildStalenessIndicator / initPolling
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_hasInFlightRow_queriesTheDataJuneauInflightMarker() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function hasInFlightRow(");
		assertTrue(fnBody.contains("tbody tr[data-juneau-inflight]"), fnBody);
	}

	@Test void b02_buildStalenessIndicator_startsInTheNeutralFreshState() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildStalenessIndicator(");
		assertTrue(fnBody.contains("juneau-view-staleness"), fnBody);
		assertTrue(fnBody.contains("el.dataset.testid = \"staleness\""), fnBody);
		assertTrue(fnBody.contains("el.dataset.state = \"fresh\""), fnBody);
	}

	/**
	 * Never overwrite an in-flight row: the poll tick itself must check {@code hasInFlightRow} and skip its
	 * redraw entirely rather than partially/fully overwrite it.
	 */
	@Test void b03_initPolling_pollTickSkipsWhenAnyRowIsInFlight() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("if (hasInFlightRow(table)) return;"), fnBody);
	}

	/** Polling pauses while the tab/page is not visible - no fetch cost while hidden. */
	@Test void b04_initPolling_pollTickSkipsWhileTabIsHidden() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("if (document.hidden) return;"), fnBody);
	}

	/**
	 * A plain interval fetch - not a streaming/SSE transport.
	 *
	 * <p>
	 * Scoped to the {@code initPolling} function body: the TABLE-POLLING transport must stay a plain
	 * {@code setInterval} + {@code dt.ajax.reload} loop and must never itself become an {@code EventSource} stream.
	 * The check is deliberately NOT over the whole file: the async-job feature legitimately opens an
	 * {@code EventSource} elsewhere (its own DISTINCT job-running affordance, HIGH-9), which must not freeze polling -
	 * so streaming may exist in the file, just never inside the polling loop.
	 */
	@Test void b05_initPolling_isAPlainIntervalFetch_notAStreamingTransport() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("setInterval(poll, intervalMs)"), fnBody);
		assertTrue(fnBody.contains("dt.ajax.reload(null, false)"), fnBody);
		assertFalse(fnBody.toLowerCase().contains("eventsource"), fnBody);
		assertFalse(fnBody.contains("text/event-stream"), fnBody);
	}

	/** A failed round trip must flip a DISTINCT visible state, not just leave a frozen "fresh" timestamp. */
	@Test void b06_initPolling_ajaxErrorFlipsADistinctFailedState_withoutTouchingLastSuccess() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("\"error.dt\""), fnBody);
		assertTrue(fnBody.contains("state.failed = true"), fnBody);
		assertTrue(fnBody.contains("\"draw.dt\""), fnBody);
		assertTrue(fnBody.contains("state.lastSuccessAt = Date.now()"), fnBody);
		assertTrue(fnBody.contains("state.failed = false"), fnBody);
	}

	@Test void b07_constructTable_onlyWiresPollingWhenViewDeclaresAPollInterval() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(functionBody(body, "function constructTable(").contains("wireTablePolling("), body);
		var initBody = functionBody(body, "function wireTablePolling(");
		assertTrue(initBody.contains("if (!viewDef.pollIntervalMs || !toolbarRow) return"), initBody);
		assertTrue(initBody.contains("buildStalenessIndicator()"), initBody);
		assertTrue(initBody.contains("initPolling(table, ctx.dataTable, viewDef, staleness, ctx)"), initBody);
		assertTrue(initBody.contains(".juneau-view-toolbar-right"), initBody);
		assertTrue(initBody.contains("rightCluster.appendChild(staleness)"), initBody);
	}

	@Test void b08_initPolling_storesIntervalIds_andClearsLeftovers() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("clearInterval(id)"), fnBody);
		assertTrue(fnBody.contains("ctx._pollTimers = [pollId, renderId]"), fnBody);
		var teardown = functionBody(body, "function teardownTable(");
		assertTrue(teardown.contains("clearInterval(id)"), teardown);
		assertTrue(teardown.contains("ctx._pollTimers"), teardown);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Suspension: the THIRD poll-skip condition (manual pausePolling toggle / open editing surface)
	//------------------------------------------------------------------------------------------------------------------

	/** The suspension check is a poll-tick skip, exactly like the two that precede it - not a cleared timer. */
	@Test void d01_initPolling_pollTickSkipsWhileSuspended() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("if (isPollSuspended(table, ctx, viewDef)) return;"), fnBody);
	}

	/**
	 * The opt-in gate must short-circuit BEFORE the DOM queries: a consumer that declared neither the flag nor the
	 * toggle has to take the same two branches it always did, at the same cost.
	 */
	@Test void d02_isPollSuspended_readsManualFlagThenTheOptInGate() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function isPollSuspended(");
		assertTrue(fnBody.contains("if (ctx?._pollPaused) return true;"), fnBody);
		assertTrue(fnBody.contains("if (! viewDef?.pausePollingWhileEditing) return false;"), fnBody);
		assertTrue(fnBody.indexOf("pausePollingWhileEditing") < fnBody.indexOf("hasOpenDetailRow"), fnBody);
		assertTrue(fnBody.contains("hasOpenDetailRow(table) || hasOpenActionDialog(ctx)"), fnBody);
	}

	/**
	 * Reads the panel, NOT the {@code .juneau-view-detail-open} marker class.  Nothing clears that class on
	 * {@code draw.dt}, and DataTables reuses its row nodes in client-side mode - so after any redraw that is not a
	 * collapse click the class outlives the panel it described, and suspending on it would freeze this view's
	 * polling permanently.  Pinned as a test because the class reads like the more obvious choice.
	 */
	@Test void d03_hasOpenDetailRow_readsThePanel_notTheStaleableMarkerClass() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function hasOpenDetailRow(");
		assertTrue(fnBody.contains("querySelector(\".juneau-view-detail-panel\")"), fnBody);
		assertFalse(fnBody.contains("juneau-view-detail-open"), fnBody);
	}

	/**
	 * A leaked dialog-stack entry would suspend this view forever, which is the "live but looks frozen" failure the
	 * indicator exists to prevent - so an entry only counts while it is still attached.
	 */
	@Test void d04_hasOpenActionDialog_readsTheDialogStack_andRequiresAttachment() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function hasOpenActionDialog(");
		assertTrue(fnBody.contains("ctx?._dialogStack"), fnBody);
		assertTrue(fnBody.contains("isAttachedNode(el)"), fnBody);
	}

	/** Suspension stops the FETCH only: a frozen age label is how a BROKEN poll looks, so the render tick stays on. */
	@Test void d05_initPolling_suspensionNeverStopsTheOneSecondRenderTick() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("setInterval(render, 1000)"), fnBody);
		// The render timer is armed unconditionally - no suspension check between it and its setInterval.
		var renderTimer = fnBody.substring(fnBody.indexOf("const renderId"));
		assertFalse(renderTimer.contains("isPollSuspended"), renderTimer);
	}

	/**
	 * A paused view says so, in a state distinct from both "fresh" and "error" - and an ALREADY-FAILED poll keeps
	 * saying "failed", because a pause the operator chose must not paper over a failure that happened to them.
	 */
	@Test void d06_initPolling_pausedLabelIsDistinct_andErrorStillWins() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("const paused = ! state.failed && isPollSuspended(table, ctx, viewDef);"), fnBody);
		assertTrue(fnBody.contains("state.failed ? \"error\" : (paused ? \"paused\" : \"fresh\")"), fnBody);
		assertTrue(fnBody.contains("\"Paused \\u2014 updated \""), fnBody);
		// The age keeps advancing while paused - the paused branch appends the SAME computed age, never a frozen one.
		assertTrue(fnBody.contains("\"Updated \") + age"), fnBody);
	}

	/** The ribbon toggle is built before polling is wired, so it repaints the pill through a late-bound hook. */
	@Test void d07_initPolling_exposesTheLateBoundRepaintHookForTheRibbonToggle() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("ctx._onPollPausedChange = render"), fnBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// The manual pausePolling ribbon toggle (juneau-ribbon.js + its RibbonAction factory)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_ribbonAction_pausePollingFactoryEmitsItsDiscriminator() {
		assertEquals("pausePolling", RibbonAction.pausePolling().type);
	}

	/** Not query-contributing: a poll toggle is view-local, and must not leak a request param. */
	@Test void e02_ribbonAction_pausePollingContributesNoQueryParam() {
		var view = ViewDef.create("v").rowType(Object.class).ribbon(RibbonAction.pausePolling()).poll(5000);
		assertTrue(RibbonAction.toQueryParams(view).isEmpty());
	}

	@Test void e03_viewDef_pausePollingWhileEditingIsOptIn_andOmittedWhenUnset() {
		assertNull(ViewDef.create("v").rowType(Object.class).poll(5000).pausePollingWhileEditing,
			"pausePollingWhileEditing must default to unset, not false - an existing view's behavior is unchanged.");
		assertEquals(Boolean.TRUE,
			ViewDef.create("v").rowType(Object.class).poll(5000).pausePollingWhileEditing().pausePollingWhileEditing);
	}

	/**
	 * The toggle flips the same {@code ctx._pollPaused} flag {@code isPollSuspended} reads, and initializes its
	 * pressed state FROM that flag - {@code ctx} outlives a column-config Apply, so a paused view must come back
	 * paused rather than silently resuming with an unpressed button.
	 */
	@Test void e04_buildRibbon_pausePollingTogglesTheFlagAndReflectsItInAriaPressed() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildRibbon(");
		assertTrue(fnBody.contains("if (a.type === \"pausePolling\") {"), fnBody);
		assertTrue(fnBody.contains("ctx._pollPaused = ! ctx._pollPaused;"), fnBody);
		assertTrue(fnBody.contains("ppBtn.setAttribute(\"aria-pressed\", ctx._pollPaused ? \"true\" : \"false\");"), fnBody);
		assertTrue(fnBody.contains("ctx._onPollPausedChange()"), fnBody);
	}

	/** No timer to hold means no button: a pause control on a non-polling view would be an inert lie. */
	@Test void e05_buildRibbon_pausePollingRendersNothingOnANonPollingView() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildRibbon(");
		assertTrue(fnBody.contains("if (! viewDef.pollIntervalMs) return;"), fnBody);
	}

	/**
	 * The pause control must NOT fall through to resolveButtonIcon's neutral "tune" default. "tune" resolves to the
	 * settings gear, which is exactly what the column chooser paints (juneau-config.js's mountChooser) - so on an
	 * icon-only ribbon a view declaring both pausePolling and columnConfig would show two identical gears, one
	 * pausing the poll and one opening the column list.
	 */
	@Test void e06_defaultIcons_pausePollingDoesNotCollideWithTheColumnChooserGear() throws Exception {
		var ribbon = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var start = ribbon.indexOf("const DEFAULT_ICONS = {");
		var defaults = ribbon.substring(start, ribbon.indexOf("};", start));
		assertTrue(defaults.contains("pausePolling:"), defaults);
		assertFalse(defaults.contains("pausePolling: \"tune\""), defaults);
		assertFalse(defaults.contains("pausePolling: \"settings\""), defaults);
	}

	//------------------------------------------------------------------------------------------------------------------
	// The in-flight draw cancel.
	//------------------------------------------------------------------------------------------------------------------

	/** The timer marks its own reload so the cancel can tell it apart from a draw the operator asked for. */
	@Test void f01_poll_marksItsOwnReloadAsTimerOriginated() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initPolling(");
		assertTrue(fnBody.contains("ctx._pollDrawPending = true;"), fnBody);
		assertTrue(fnBody.contains("return false;"), fnBody);
		// All three conditions, so the cancel cannot widen to user draws or to views that opted into nothing.
		assertTrue(fnBody.contains("!! ctx._pollDrawPending && !! viewDef.pausePollingWhileEditing && hasOpenDetailRow(table)"), fnBody);
	}

	/**
	 * Both preDraw.dt handlers gate on the SAME predicate, which is what makes their binding order irrelevant.
	 * It would not be otherwise: bindDetailInflightDrawGuards binds first and tears down nested tables at preDraw,
	 * so a cancel that did not also suppress the teardown would leave the still-open panel holding dead nested
	 * tables - a worse bug than the race being closed.
	 */
	@Test void f02_theNestedTeardownGuardSharesTheCancelPredicate() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function bindDetailInflightDrawGuards(");
		assertTrue(fnBody.contains("if (ctx._shouldCancelPollDraw?.()) return;"), fnBody);
		assertTrue(fnBody.indexOf("_shouldCancelPollDraw") < fnBody.indexOf("teardownNestedTables(table)"), fnBody);
	}

	/** An explicit refresh clears the marker, so a poll still in flight cannot make the cancel eat it. */
	@Test void f03_ctxRedrawClearsTheTimerMarker() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var start = body.indexOf("ctx.redraw = function () {");
		var fn = body.substring(start, body.indexOf("};", start));
		assertTrue(fn.contains("ctx._pollDrawPending = false;"), fn);
	}

	/**
	 * The automatic pause repaints the pill on the transition, not up to a second later on the next render tick.
	 * The manual toggle already got this hook precisely because the 1s delay was too slow; leaving the automatic
	 * path on it meant a just-expanded row could still read "Updated 3s ago" in the fresh state while ticks were
	 * already being skipped - a held view briefly claiming to be a live one.
	 */
	@Test void f04_theAutomaticPauseRepaintsThePillOnEveryTransition() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("function notifyPollPausedChange("), body);
		assertTrue(functionBody(body, "function expandDetailRow(").contains("notifyPollPausedChange(ctx)"));
		assertTrue(functionBody(body, "function toggleDetailRow(").contains("notifyPollPausedChange(ctx)"));
		assertTrue(functionBody(body, "function handleDetailSafeCollapseClick(").contains("notifyPollPausedChange(ctx)"));
		assertTrue(functionBody(body, "function showActionDialog(").contains("notifyPollPausedChange(ctx)"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// CSS shape (neutral, no palette color; distinct error-state emphasis)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_viewsCss_hasNeutralStalenessChipShape() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-staleness {"), body);
		var start = body.indexOf(".juneau-view-staleness {");
		var end = body.indexOf("}", start);
		var rule = body.substring(start, end);
		assertFalse(rule.contains("color:"), rule);
		assertFalse(rule.contains("background"), rule);
	}

	@Test void c02_viewsCss_errorStateIsVisiblyDistinctWithoutColor() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-staleness[data-state=\"error\"] {"), body);
		var start = body.indexOf(".juneau-view-staleness[data-state=\"error\"] {");
		var end = body.indexOf("}", start);
		var rule = body.substring(start, end);
		assertTrue(rule.contains("border-width: 2px") || rule.contains("font-weight: bold"), rule);
	}

	/** Paused is a third visible state: distinct from fresh AND from error, and still colorless. */
	@Test void c03_viewsCss_pausedStateIsVisiblyDistinctWithoutColor() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-staleness[data-state=\"paused\"] {"), body);
		var start = body.indexOf(".juneau-view-staleness[data-state=\"paused\"] {");
		var end = body.indexOf("}", start);
		var rule = body.substring(start, end);
		assertFalse(rule.contains("color:"), rule);
		assertFalse(rule.contains("background"), rule);
		assertTrue(rule.contains("border-style: dashed") || rule.contains("font-style: italic"), rule);
		// Distinct from the error rule's own emphasis, not merely a second copy of it.
		assertFalse(rule.contains("border-width: 2px"), rule);
	}
}
