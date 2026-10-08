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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The row-selection + bulk-mutation half of the module's <b>JavaScript-execution harness</b>:
 * runs the REAL served {@code juneau-views.js} in a real headless browser and asserts, as a user would experience
 * it, that:
 * <ul>
 * 	<li>per-row selection and select-all toggle a live selection set keyed by the STABLE row id (never a DOM index);
 * 	<li>selection is persistent by default (a draw that removes a row keeps its id selected) and pruned per draw only
 * 		under the {@code page} scope; the select-all header is tri-state and skips {@code selectableWhen}-disabled rows;
 * 	<li>row-selection and bulk-mutation are two INDEPENDENT opt-ins - a selection-only (e.g. export) table never
 * 		carries the bulk marker, even though both are declared via the same {@code hasSelection}/{@code hasBulk}
 * 		DOM-attribute mechanism (HIGH-5);
 * 	<li>a bulk action goes through a mandatory confirm dialog and then runs either as N independent per-row writes
 * 		(at most four in flight) or, for {@code aggregate} mode, as one POST of {@code {ids, idempotencyKey}} answered
 * 		by a {@code BulkResult}; either way the run ends in one summary toast, succeeded and not-found ids leave the
 * 		selection and failed ids stay selected;
 * 	<li>the bulk toolbar sits in the right cluster, is hidden at a zero count, and the independently-versioned bulk
 * 		sidecar is read/contract-checked correctly at runtime (R2).
 * </ul>
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape test):</h5>
 * <p>
 * {@link ViewsJs_Selection_Test} proves the shipped script <i>contains</i> the selection/bulk logic in the right
 * shape; it cannot prove a checkbox click actually updates the live selection set, that a draw actually prunes an
 * off-screen id, or that a bulk action actually issues N independent requests with N independent settled outcomes.
 * This canary drives the real runtime in Chromium against a stubbed {@code fetch}, so those user-visible facts are
 * measured rather than inferred.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code row-selection-bulk.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change is
 * needed.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ModalResult_BrowserTest} &mdash; the sibling declarative-modal canary this reuses the settle
 * 		path from.
 * </ul>
 */
@EnabledIfSystemProperty(named=RowSelectionBulk_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
@SuppressWarnings({
	"unchecked" // Report sections are parsed JSON Maps cast to Map<String,Object>.
})
class RowSelectionBulk_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("row-selection-bulk.cjs");

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n<script>\n"
			+ resource(ViewsMixin.RENDERS_JS_RESOURCE) + "\n</script>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("row-selection-bulk.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("row-selection-bulk-stdout.json");
		var stderr = dir.resolve("row-selection-bulk-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	private static Map<String,Object> sub(String key) {
		return (Map<String,Object>) report.get(key);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The runtime loaded, at the bulk contract's OWN version (independent of VIEW_META's)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_runtimeLoadedAtBulkContractVersion() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not populate JuneauViews.init: " + report);
		assertEquals(BulkMutateDef.CONTRACT_VERSION, report.get("bulkContractVersion"), () -> report.toString());
		assertEquals("2", BulkMutateDef.CONTRACT_VERSION);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Per-row selection + select-all, keyed by the stable row id
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_checkingTwoRowsSelectsExactlyThoseStableIds() {
		assertEquals(List.of("1", "2"), report.get("afterTwoChecked"), () -> report.toString());
	}

	@Test void b02_selectAllSelectsEveryRowCurrentlyOnScreen() {
		assertEquals(Boolean.TRUE, report.get("hasSelectAllCheckbox"), () -> report.toString());
		assertEquals(List.of("1", "2", "3"), report.get("afterSelectAll"), () -> report.toString());
	}

	@Test void b03_deselectAllClearsTheSelection() {
		assertEquals(List.of(), report.get("afterDeselectAll"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Scope: persistent (default) survives a draw that removes the row; page prunes it
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_persistentScope_aDrawRemovingTheRowDoesNotDropIt() {
		assertEquals(List.of("1", "2", "3"), report.get("persistentSelectedAfterOffScreenDraw"), () -> report.toString());
	}

	@Test void c02_pageScope_aDrawRemovingTheRowDropsIt() {
		assertEquals(List.of("1", "2"), report.get("pageScopeSelectedAfterOffScreenDraw"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Tri-state select-all header and selectableWhen-disabled rows
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_selectAllCheckbox_startsUnchecked_notIndeterminate() {
		assertEquals(Boolean.FALSE, report.get("headerCheckedInitially"), () -> report.toString());
		assertEquals(Boolean.FALSE, report.get("headerIndeterminateInitially"), () -> report.toString());
	}

	@Test void d02_selectingSomeButNotAllRows_setsIndeterminate() {
		assertEquals(Boolean.FALSE, report.get("headerCheckedAfterSomeSelected"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("headerIndeterminateAfterSomeSelected"), () -> report.toString());
	}

	@Test void d03_selectingEverySelectableRow_setsCheckedNotIndeterminate() {
		assertEquals(Boolean.TRUE, report.get("headerCheckedAfterAllSelected"), () -> report.toString());
		assertEquals(Boolean.FALSE, report.get("headerIndeterminateAfterAllSelected"), () -> report.toString());
	}

	@Test void d04_selectAll_skipsDisabledRows() {
		assertEquals(List.of("1", "2"), report.get("afterSelectAllWithOneDisabledRow"), () -> report.toString());
	}

	@Test void d05_disabledRow_checkboxCarriesTitleAndAriaDescribedBy() {
		assertEquals(Boolean.TRUE, report.get("disabledCheckboxHasTitle"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("disabledCheckboxHasAriaDescribedBy"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Two INDEPENDENT opt-ins (HIGH-5) - verified against the ACTUAL DOM-attribute detection at runtime
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_selectionOnlyTableNeverCarriesTheBulkMarker() {
		assertEquals(Boolean.TRUE, report.get("selectOnlyHasSelection"), () -> report.toString());
		assertEquals(Boolean.FALSE, report.get("selectOnlyHasBulk"),
			() -> "a selection-only (export) table must never surface a bulk-mutate control: " + report);
	}

	@Test void e02_bulkTableCarriesBothMarkers() {
		assertEquals(Boolean.TRUE, report.get("withBulkHasSelection"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("withBulkHasBulk"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Toolbar: single-action button vs. dropdown + Go, right cluster, hidden at a zero count
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_singleBulkAction_rendersButtonNotDropdown_inRightCluster() {
		assertEquals(Boolean.TRUE, report.get("singleActionIsButton"), () -> report.toString());
		assertEquals(Boolean.FALSE, report.get("singleActionHasDropdown"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("toolbarIsInRightCluster"), () -> report.toString());
		assertTrue(String.valueOf(report.get("singleActionButtonTextWithTwoSelected")).contains("selected (2)"), () -> report.toString());
	}

	@Test void f02_multipleBulkActions_rendersDropdownAndDisabledGoUntilChosen() {
		assertEquals(Boolean.TRUE, report.get("multiActionHasDropdown"), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("goDisabledBeforeChoice"), () -> report.toString());
		assertEquals(Boolean.FALSE, report.get("goDisabledAfterChoice"), () -> report.toString());
	}

	@Test void f03_toolbarIsHiddenUntilSomethingIsSelected() {
		var toolbar = sub("toolbar");
		assertEquals(Boolean.TRUE, toolbar.get("hiddenInitially"), () -> report.toString());
		assertEquals(Boolean.FALSE, toolbar.get("hiddenWithSelection"), () -> report.toString());
		assertTrue(String.valueOf(toolbar.get("countTextWithSelection")).contains("2"), () -> report.toString());
		assertEquals(Boolean.TRUE, toolbar.get("hiddenAfterCleared"), () -> report.toString());
	}

	@Test void f04_clearSelectionLink_clearsSelection() {
		assertEquals(List.of(), report.get("afterClearSelectionLinkClicked"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) The mandatory confirm dialog gates every bulk submit (built-in fallback: this page defines no
	//    JuneauViews.dialogs.confirm; the host-dialog path is covered by bulk-selection.cjs)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_clickingTheBulkButton_showsFallbackConfirmModal_beforeAnyFetch() {
		assertEquals(Boolean.TRUE, report.get("confirmModalShown"), () -> report.toString());
		assertEquals(0L, ((Number)report.get("fetchCountBeforeConfirm")).longValue(), () -> report.toString());
	}

	@Test void g02_confirmModalListsSelectedRowsCappedAtTwenty() {
		var text = String.valueOf(report.get("confirmModalListText"));
		assertTrue(text.contains("Change 1"), () -> text);
		assertTrue(text.contains("and 5 more"), () -> text);
		assertFalse(text.contains("Change 25"), () -> text);
	}

	@Test void g03_cancellingTheConfirmModal_submitsNothing() {
		assertEquals(0L, ((Number)report.get("fetchCountAfterCancel")).longValue(), () -> report.toString());
		assertEquals(Boolean.TRUE, report.get("modalGoneAfterCancel"), () -> report.toString());
		assertEquals(25L, ((Number)report.get("selectionKeptAfterCancel")).longValue(), () -> report.toString());
	}

	@Test void g04_actionWithConfirmFalse_skipsTheDialogEntirely() {
		assertEquals(Boolean.FALSE, report.get("noConfirmActionShowedModal"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) PER_ROW (default bulkMode): N independent writes and ONE summary
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_perRowMode_issuesExactlyOneRequestPerSelectedTarget() {
		var bulk = sub("perRowBulk");
		assertEquals(3L, ((Number)bulk.get("fetchCount")).longValue(), () -> report.toString());
		assertEquals(List.of("1", "2", "3"), bulk.get("targetIds"), () -> report.toString());
		assertEquals(List.of("abort", "abort", "abort"), bulk.get("actionIds"), () -> report.toString());
		assertEquals(List.of("/x/1/abort", "/x/2/abort", "/x/3/abort"), bulk.get("urls"), () -> report.toString());
	}

	@Test void h02_perRowMode_capsAtFourInFlight() {
		var bulk = sub("perRowConcurrency");
		assertEquals(4L, ((Number)bulk.get("maxConcurrentInFlight")).longValue(), () -> report.toString());
		assertEquals(0L, ((Number)bulk.get("selectionAfter")).longValue(), () -> report.toString());
	}

	@Test void h03_perRowMode_resultsAreIndependent_oneFailureIsNeverMaskedBySuccess() {
		// '1' succeeds (200), '2' fails (500), '3' is gone (404): each is counted on its own.
		var bulk = sub("perRowBulk");
		assertEquals("Abort: 1 succeeded, 1 not found, 1 failed (ids: 2)", bulk.get("toastText"), () -> report.toString());
		assertEquals("alert", bulk.get("toastRole"), () -> report.toString());
	}

	@Test void h04_succeededAndNotFoundLeaveTheSelection_failedStaysSelectedAndChecked() {
		var bulk = sub("perRowBulk");
		assertEquals(List.of("2"), bulk.get("selectionAfterApply"), () -> report.toString());
		assertEquals(List.of("1", "3"), bulk.get("uncheckedIds"), () -> report.toString());
	}

	@Test void h05_runEndsInOneAnnouncementAndAReload() {
		var bulk = sub("perRowBulk");
		assertEquals(bulk.get("toastText"), bulk.get("announceText"), () -> report.toString());
		assertEquals(Boolean.TRUE, bulk.get("reloadCalled"), () -> report.toString());
	}

	@Test void h06_cleanRun_isAStatusToastThatAutoDismissesAfterSixSeconds() {
		var bulk = sub("perRowBulkOk");
		assertEquals("status", bulk.get("toastRole"), () -> report.toString());
		assertEquals("Abort: 3 succeeded", bulk.get("toastText"), () -> report.toString());
		assertEquals(6000L, ((Number)bulk.get("toastAutoDismissMs")).longValue(), () -> report.toString());
	}

	@Test void h07_onSuccessNone_skipsTheReload() {
		assertEquals(Boolean.FALSE, sub("perRowBulkOnSuccessNone").get("reloadCalled"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// i) AGGREGATE: one CSRF-protected POST of {ids, idempotencyKey}, answered by a BulkResult
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_aggregateMode_issuesExactlyOnePost_withIdsAndIdempotencyKey() {
		var bulk = sub("aggregateBulk");
		assertEquals(1L, ((Number)bulk.get("fetchCount")).longValue(), () -> report.toString());
		assertEquals(List.of("1", "2", "3"), bulk.get("requestBody_ids"), () -> report.toString());
		assertNotNull(bulk.get("requestBody_idempotencyKey"), () -> report.toString());
		assertEquals(List.of("idempotencyKey", "ids"), bulk.get("requestBodyKeys"), () -> report.toString());
		assertEquals(Boolean.TRUE, bulk.get("requestHadCsrfHeader"), () -> report.toString());
	}

	@Test void i02_aggregateMode_appliesTheReturnedBulkResult() {
		var bulk = sub("aggregateBulk");
		assertEquals("Abort: 2 succeeded, 1 not found, 1 failed (ids: 3)", bulk.get("toastText"), () -> report.toString());
		assertEquals(bulk.get("toastText"), bulk.get("announceText"), () -> report.toString());
		assertEquals(List.of("3"), bulk.get("selectionAfterApply"), () -> report.toString());
	}

	@Test void i03_aggregateMode_transportFailure_isAStickyAlertToast_notABanner() {
		var bulk = sub("aggregateTransportFailure");
		assertEquals(List.of(), bulk.get("banner"), () -> "a whole-run failure must not show in the page banner: " + bulk);
		assertEquals("alert", bulk.get("toastRole"), () -> report.toString());
		assertEquals(Boolean.TRUE, bulk.get("toastIsSticky"), () -> report.toString());
		assertTrue(String.valueOf(bulk.get("consoleErrorText")).startsWith("bulk action 'abort' on table"), () -> report.toString());
		assertEquals(List.of("1", "2"), bulk.get("selectionAfter"), () -> report.toString());
	}

	@Test void i04_aggregateMode_nonBulkResultResponse_isRefusedAndKeepsTheSelection() {
		var bulk = sub("aggregateBadResponseShape");
		assertTrue(String.valueOf(bulk.get("consoleErrorText")).contains("response is not a BulkResult"), () -> report.toString());
		assertEquals(List.of("1", "2"), bulk.get("selectionAfter"), () -> report.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// j) The independently-versioned bulk sidecar is actually read + contract-checked at runtime (R2)
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_bulkSidecarIsReadAndContractChecked() {
		var sidecar = sub("bulkSidecar");
		assertEquals(BulkMutateDef.CONTRACT_VERSION, sidecar.get("contractVersion"), () -> report.toString());
		assertEquals(1L, ((Number) sidecar.get("actionCount")).longValue(), () -> report.toString());
		assertEquals(Boolean.TRUE, sidecar.get("missingSidecarReturnsNull"), () -> report.toString());
	}
}
