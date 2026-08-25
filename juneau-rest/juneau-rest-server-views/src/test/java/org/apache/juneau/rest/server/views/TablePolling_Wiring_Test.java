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
		assertTrue(initBody.contains("insertBefore(staleness, rightCluster.firstChild)"), initBody);
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
}
