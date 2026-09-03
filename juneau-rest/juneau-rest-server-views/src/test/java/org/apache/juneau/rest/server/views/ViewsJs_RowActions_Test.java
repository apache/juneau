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
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Always-on source-shape coverage for the {@code juneau-views.js} row-action + fail-closed CSRF plumbing, plus
 * (WORK-J0509) the {@code RowAction.endpoint} {@code {property}} substitution.  Source-shape always runs;
 * the substitution's behavioral proof runs via a Node harness when {@code node} is on {@code PATH} (no
 * {@code -Pjs-tests}/browser required - {@code buildActionRequest} is pure).  Mirrors
 * {@code ViewsMixin_Serving_Test}'s served-script substring style: proves the load-bearing pieces of the
 * runtime's row-menu/submit contract are present in the shipped asset, without booting a browser (the
 * fail-closed-CSRF behavioral proof lives in the opt-in {@code RowActionCsrf_BrowserTest} canary).
 *
 * <p>
 * The client refusal these tests pin is <b>defense-against-consumer-omission</b>, not the security control: the
 * landed server-side {@link LoopbackBoundary} is.  The value of pinning the shapes here is that the two halves must
 * agree by construction &mdash; same default header, same JSON content type, same {@code isBlank}-not-{@code isEmpty}
 * fail-closed test, same non-safe-method rule.
 */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_RowActions_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static String viewsJs() throws Exception {
		return c.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	@Test void a01_contractVersionPinnedToCurrent() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("JUNEAU_VIEW_CONTRACT_VERSION = \"4\""), body);
		assertEquals("4", ViewDef.CONTRACT_VERSION);
	}

	@Test void a02_defaultCsrfHeaderMatchesTheServerBoundary() throws Exception {
		// The runtime's default header MUST equal LoopbackBoundary.DEFAULT_CSRF_HEADER or the two halves disagree.
		var body = viewsJs();
		assertTrue(body.contains("DEFAULT_CSRF_HEADER = \"" + LoopbackBoundary.DEFAULT_CSRF_HEADER + "\""), body);
		assertEquals("X-Csrf-Token", LoopbackBoundary.DEFAULT_CSRF_HEADER);
	}

	@Test void a03_failClosedTokenTestUsesBlankNotEmpty() throws Exception {
		// isBlankToken must match the boundary's check() (isBlank), so a WHITESPACE token also refuses - not the
		// SynchronizerToken.matches() isEmpty(), which would let whitespace through to a confusing 403.
		var body = viewsJs();
		var start = body.indexOf("function isBlankToken(");
		assertTrue(start >= 0, () -> "isBlankToken not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		var fn = body.substring(start, end < 0 ? body.length() : end);
		assertTrue(fn.contains(".trim()"), fn);   // whitespace-sensitive
	}

	@Test void a04_actionSubmitSendsJsonBodyAndCsrfHeader() throws Exception {
		var body = viewsJs();
		var start = body.indexOf("function buildActionRequest(");
		assertTrue(start >= 0, () -> "buildActionRequest not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		var fn = body.substring(start, end < 0 ? body.length() : end);
		// JSON content type (so the write passes LoopbackBoundary.isJson) + the token under the header name.
		assertTrue(fn.contains("\"Content-Type\": \"application/json\""), fn);
		assertTrue(fn.contains("JSON.stringify("), fn);
		assertTrue(fn.contains("headerName || DEFAULT_CSRF_HEADER"), fn);
		// Both fail-closed refusals: safe/absent method, and blank token.
		assertTrue(fn.contains("isSafeMethod(action.method)"), fn);
		assertTrue(fn.contains("isBlankToken(token)"), fn);
		assertTrue(fn.contains("refuse: true"), fn);
	}

	@Test void a05_safeMethodSetMirrorsMethodSafety() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("const SAFE_METHODS = { GET: 1, HEAD: 1, OPTIONS: 1, TRACE: 1 }"), body);
	}

	@Test void a06_rowMenuAndTriggerAreWired() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function initRowActions("), body);
		assertTrue(body.contains("function buildRowActionMenu("), body);
		assertTrue(body.contains("juneau-view-action-trigger"), body);
		assertTrue(body.contains("juneau-view-action-menu"), body);
	}

	@Test void a07_refusalIsVisibleNotSilent() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function renderRowActionRefusal("), body);
		assertTrue(body.contains("juneau-view-action-refusal"), body);
		assertTrue(body.contains("role\", \"alert\""), body);
	}

	@Test void a08_readsAutoEmbeddedCsrfAttribute() throws Exception {
		// The runtime reads the token ViewTable auto-embeds under data-juneau-csrf (ViewTable.CSRF_ATTR).
		var body = viewsJs();
		assertTrue(body.contains("data-juneau-csrf"), body);
		assertEquals("data-juneau-csrf", ViewTable.CSRF_ATTR);
	}

	@Test void a09_enabledWhenGateReusesTheActionRefEvaluator_notRowClassRules() throws Exception {
		// firstFailingRowActionRule must delegate to the SAME evaluator the detail-panel ActionBar uses
		// (firstFailingActionRule/actionRuleMatches), never the row-class-highlight evaluator - the two rule
		// families are unrelated (row.class painting vs. action disable-with-reason).
		var body = viewsJs();
		var start = body.indexOf("function firstFailingRowActionRule(");
		assertTrue(start >= 0, () -> "firstFailingRowActionRule not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		var fn = body.substring(start, end < 0 ? body.length() : end);
		assertTrue(fn.contains("firstFailingActionRule("), fn);
		assertFalse(fn.contains("evaluateRowClassRules"), fn);
	}

	@Test void a10_enabledWhenGateCoversAllThreeActivationSurfaces() throws Exception {
		// The gate must be re-checked, per row, on every surface that can fire a RowAction: the row-menu item, the
		// action-bound pill (both draw-time paint and click-time re-check), and the dialog-form action button.
		var body = viewsJs();
		assertTrue(functionBody(body, "function buildRowActionMenu(").contains("firstFailingRowActionRule("), body);
		assertTrue(functionBody(body, "function activatePillAction(").contains("firstFailingRowActionRule("), body);
		assertTrue(functionBody(body, "function applyRowActionPillGates(").contains("firstFailingRowActionRule("), body);
		assertTrue(functionBody(body, "function openFormActionDialog(").contains("firstFailingRowActionRule("), body);
	}

	@Test void a11_gatedActionIsDisabledNeverHidden() throws Exception {
		// Disabled, never hidden - a gated-and-failing action must stay visible in place, only inert.
		var body = viewsJs();
		var control = functionBody(body, "function disableRowActionControl(");
		var pill = functionBody(body, "function disableRowActionPill(");
		assertFalse(control.contains(".hidden = true") || control.contains(".hidden=true"), control);
		assertFalse(pill.contains(".hidden = true") || pill.contains(".hidden=true"), pill);
	}

	@Test void a12_gatedActionCarriesBothTitleAndAriaDescribedbyReasonChannels() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function attachRowActionDescNode(");
		assertTrue(fn.contains("setAttribute(\"title\""), fn);
		assertTrue(fn.contains("aria-describedby"), fn);
	}

	// -----------------------------------------------------------------------------------------------------------
	// WORK-J0509: RowAction.endpoint `{property}` substitution, mirroring Column.href's `linked`-renderer
	// mechanism exactly (same interpolateHref helper, same token grammar, same escaping, same no-value behavior).
	// -----------------------------------------------------------------------------------------------------------

	@Test void a13_buildActionRequestSubstitutesEndpointViaTheSameHelperColumnHrefUses() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function buildActionRequest(");
		// The 5th param + the substitution call - not `action.endpoint` issued verbatim anymore.
		assertTrue(fn.contains("buildActionRequest(action, token, headerName, extra, rowData)"), fn);
		assertTrue(fn.contains("url: substituteRowActionEndpoint(action.endpoint, rowData)"), fn);
		assertFalse(fn.contains("url: action.endpoint"), fn);
	}

	@Test void a14_substituteRowActionEndpointDelegatesToTheSharedColumnHrefHelper() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function substituteRowActionEndpoint(");
		// The SAME interpolateHref helper juneau-renders.js exposes for Column.href's `linked` renderer - not a
		// second, divergent template implementation.
		assertTrue(fn.contains("NS._render?.interpolateHref"), fn);
		assertTrue(fn.contains("NS._render.interpolateHref(endpoint, rowData)"), fn);
		// A null/undefined endpoint is returned as-is, never stringified.
		assertTrue(fn.contains("if (endpoint == null) return endpoint;"), fn);
	}

	@Test void a15_submitRowActionResolvesTheClickedRowsOwnDataForSubstitution() throws Exception {
		var body = viewsJs();
		var fn = functionBody(body, "function submitRowAction(");
		// Must pass THIS row's own already-fetched data (the same lookup the enabledWhen gate uses) - never a
		// stale/global row, and never re-implementing its own DataTables lookup.
		assertTrue(fn.contains("rowDataForTr(ctx, tr)"), fn);
		assertTrue(fn.contains("buildActionRequest("), fn);
	}

	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> signature + " not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	// -----------------------------------------------------------------------------------------------------------
	// Node behavioral harness (row-action-endpoint.cjs) - pure, DOM/browser-free; runs when `node` is on PATH.
	// -----------------------------------------------------------------------------------------------------------

	private static String rendersJs() throws Exception {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.RENDERS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static Map<?,?> report;
	private static Map<?,?> reportNoRendersJs;

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
			report = Json.to(runNode(harness, viewsFile, rendersFile), Map.class);
			reportNoRendersJs = Json.to(runNode(harness, viewsFile, null), Map.class);
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
			var p = Path.of(basedir, "src/test/js/row-action-endpoint.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/row-action-endpoint.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/row-action-endpoint.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path viewsJs, Path rendersJs) throws Exception {
		var stdout = Files.createTempFile("row-action-endpoint-stdout-", ".json");
		var stderr = Files.createTempFile("row-action-endpoint-stderr-", ".txt");
		try {
			var args = new ArrayList<String>(List.of("node", harness.toString(), viewsJs.toString()));
			if (rendersJs != null) args.add(rendersJs.toString());
			var pb = new ProcessBuilder(args)
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("row-action-endpoint.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("row-action-endpoint.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or row-action-endpoint.cjs not found — behavioral layer skipped");
		return report;
	}

	private static Map<?,?> reportNoRendersJs() {
		assumeTrue(reportNoRendersJs != null, "node not available or row-action-endpoint.cjs not found — behavioral layer skipped");
		return reportNoRendersJs;
	}

	@Test void b01_idTemplateResolvesAgainstTheCurrentRow_exactlyLikeColumnHref() {
		assertEquals("servlet:/incidents/a1/ack", report().get("idTemplate_resolved"));
	}

	@Test void b02_literalEndpointWithNoTokenIsPreservedByteIdentical_backwardCompat() {
		var r = report();
		// With rowData, without rowData, and via the pre-J0509 4-argument call signature - all three unaffected.
		assertEquals("servlet:/incidents/ack", r.get("literal_withRowData"));
		assertEquals("servlet:/incidents/ack", r.get("literal_noRowData"));
		assertEquals("servlet:/incidents/ack", r.get("literal_preFeatureCallSignature"));
	}

	@Test void b03_noIdOrNullIdRowSubstitutesToEmptyString_matchingColumnHref() {
		var r = report();
		// Missing key, explicit null, absent rowData, and undefined rowData all substitute `{id}` to "" - the
		// SAME behavior Column.href's interpolateHref has for a row with no id - never a thrown error.
		assertEquals("servlet:/incidents//ack", r.get("noId_missingKey"));
		assertEquals("servlet:/incidents//ack", r.get("noId_explicitNull"));
		assertEquals("servlet:/incidents//ack", r.get("noId_absentRowData"));
		assertEquals("servlet:/incidents//ack", r.get("noId_undefinedRowData"));
	}

	@Test void b04_substitutedValueIsUrlEncodedPerToken_matchingColumnHref() {
		assertEquals("servlet:/incidents/a%2F1%20b/ack", report().get("encoded_slashAndSpace"));
	}

	@Test void b05_genericPropertyGrammar_notAHardcodedIdOnlyToken() {
		// Column.href's template is `{property}` (any row key), not a hardcoded `{id}` - proving RowAction.endpoint
		// resolves a second, unrelated token in the same template confirms this mirrors the generic grammar.
		assertEquals("/x/a1/status/open", report().get("multiToken_resolved"));
	}

	@Test void b06_nullOrUndefinedEndpointIsReturnedAsIs_neverStringified() {
		var r = report();
		assertNull(r.get("nullEndpoint_helper"));
		assertEquals(true, r.get("undefinedEndpointIsUndefined_helper"));
	}

	@Test void b07_refusalNeverReachesSubstitution() {
		var r = report();
		assertEquals(true, ((Map<?,?>) r.get("refusal_safeMethod")).get("refuse"));
		assertEquals("safe-method", ((Map<?,?>) r.get("refusal_safeMethod")).get("reason"));
		assertEquals(true, ((Map<?,?>) r.get("refusal_blankToken")).get("refuse"));
		assertEquals("missing-token", ((Map<?,?>) r.get("refusal_blankToken")).get("reason"));
		assertFalse(((Map<?,?>) r.get("refusal_safeMethod")).containsKey("url"));
		assertFalse(((Map<?,?>) r.get("refusal_blankToken")).containsKey("url"));
	}

	@Test void b08_substituteRowActionEndpointHelperDirect() {
		var r = report();
		assertEquals("/x/a1", r.get("helper_direct"));
		assertEquals("/x/ack", r.get("helper_noToken"));
	}

	@Test void b09_gracefullyDegradesToVerbatimEndpoint_whenRendersJsIsNotLoaded() {
		// If a caller ships juneau-views.js without its juneau-renders.js peer, substitution is unavailable -
		// the SAME graceful-degradation shape as viewEscAttr - and the endpoint is issued verbatim (the token
		// left in place) rather than throwing or silently corrupting the URL.
		var r = reportNoRendersJs();
		assertEquals(false, r.get("hasInterpolateHref"));
		assertEquals("servlet:/incidents/{id}/ack", r.get("idTemplate_resolved"));
		assertEquals("servlet:/incidents/ack", r.get("literal_withRowData"));
	}
}
