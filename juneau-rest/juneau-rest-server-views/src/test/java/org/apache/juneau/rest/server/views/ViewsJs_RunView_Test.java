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
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Drives {@code run-view.cjs}: the run-view module's checks, reducer, rendering, tooltip, notes, incremental
 * rendering and compact mode.  One method per case, plus a catch-all that fails when the harness reports a case
 * without a method of its own or a method names a case the harness does not have.
 */
class ViewsJs_RunView_Test extends TestBase {

	static final String VECTORS = "/org/apache/juneau/rest/server/views/run-view-vectors.json";

	/** Every case name the harness is expected to report; each task appends its own. */
	private static final Set<String> EXPECTED = new TreeSet<>(List.of(
		"chk01_vectors", "chk02_validateEventAccepts", "chk03_validateEventDrops",
		"chk04_unknownEvIsIgnored", "chk05_unknownMembersIgnored", "chk06_overlongStringsTruncated",
		"chk07_noteHrefRemovedNotDropped", "chk08_rawOffsetValidated", "step02b_rawOffsetLink", "blk03f_onBlockClickGetsOffset", "red01_stepUpsert", "red02_waitingThenRunning",
		"red03_stepAfterEndDropped", "red04_endSetsFinalAndReplacementEndWins", "red05_endUnknownStepDropped",
		"red06_suitePlaceholderThenReplaceCounts", "red07_testsWinOverPlaceholders", "red08_testsNotDeduplicated",
		"red09_replaceClearsStep", "red10_noteAttachment", "red11_doneLatestWinsAndLateNoteApplies",
		"red12_derivedClosing", "red13_runStatusTable", "red14_seqIdempotent",
		"red15_retentionCaps", "red16_structuralCaps", "red17_countsExactPerFw",
		"red18_stepCapDropsOnlyNewIds", "red19_terminalWithoutDoneIsStopped", "core01_createAndRoot",
		"core02_badOptionsThrowBeforeDom", "core03_oneRenderPerBatch", "core04_destroyIsIdempotent",
		"sum01_headlineAndStatus", "sum02_countsUseBuiltInLabels", "sum03_failuresListCaps",
		"sum04_busEmitsOnStatusChangeOnly", "step01_createdOnceUpdatedInPlace", "step02_rawLinkNeedsLineAndTemplate",
		"step03_exitCodeTooltip", "step04_doneClosesOpenSteps", "step05_notes",
		"esc01_hostileTextIsInert", "api01_resetStateStats", "suite01_cleanCollapsedFailingExpanded",
		"suite02_toggleAndPersistence", "suite03_placeholderShowsCounts", "blk01_blockClassesAndAttributes",
		"blk02_anchorOnlyWithRawLineAndTemplate", "blk03_eventCannotSupplyHref", "blk04_traceDisclosure",
		"blk05_traceReachableFromFailuresList", "blk03b_blocksCarryStepId",
		"blk03c_onBlockClickGetsStepLineKind", "blk03e_onBlockClickOnKeyboardNoDoubleFire", "blk03d_onBlockClickMustBeFunction", "tip01_showHide", "tip02_positionStaysInViewport",
		"tip03_stepExitTooltip", "cap01_domBounds", "cap02_collapsedBuildsNoBlocks",
		"inc01_untouchedStepKeepsItsNode", "inc02_rowRebuiltOnlyWhenSuiteChanged", "cmp01_compactMode",
		"gold01_fullGolden", "gold02_compactGolden", "gold03_replaceFlow",
		"gold04_idempotentAppend",
		"att01_groupingByIdAndTitle", "att02_collapsedByDefaultWithToggle", "att03_newAttemptRegroupsAndKeepsExpansion",
		"att04_noAttemptsNoToggle", "att05_failureLinkOpensEarlierAttempt", "att06_resetClearsAttemptState"
	));

	static Map<?,?> report(String harness) {
		var r = RegionsHarness.reportWith(harness, ViewsMixin.HELPERS_JS_RESOURCE, ViewsMixin.RUN_VIEW_JS_RESOURCE, VECTORS);
		assumeTrue(r != null, "node not available or " + harness + " not found - skipped");
		return r;
	}

	static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	private static Map<?,?> r() { return report("run-view.cjs"); }

	@Test void a01_vectors() { assertAllTrue(r(), "chk01_vectors"); }
	@Test void a02_validateEventAccepts() { assertAllTrue(r(), "chk02_validateEventAccepts"); }
	@Test void a03_validateEventDrops() { assertAllTrue(r(), "chk03_validateEventDrops"); }
	@Test void a04_unknownEvIsIgnored() { assertAllTrue(r(), "chk04_unknownEvIsIgnored"); }
	@Test void a05_unknownMembersIgnored() { assertAllTrue(r(), "chk05_unknownMembersIgnored"); }
	@Test void a06_overlongStringsTruncated() { assertAllTrue(r(), "chk06_overlongStringsTruncated"); }
	@Test void a07_noteHrefRemovedNotDropped() { assertAllTrue(r(), "chk07_noteHrefRemovedNotDropped"); }
	@Test void a08_rawOffsetValidated() { assertAllTrue(r(), "chk08_rawOffsetValidated"); }
	@Test void b01_stepUpsert() { assertAllTrue(r(), "red01_stepUpsert"); }
	@Test void b02_waitingThenRunning() { assertAllTrue(r(), "red02_waitingThenRunning"); }
	@Test void b03_stepAfterEndDropped() { assertAllTrue(r(), "red03_stepAfterEndDropped"); }
	@Test void b04_endSetsFinalAndReplacementEndWins() { assertAllTrue(r(), "red04_endSetsFinalAndReplacementEndWins"); }
	@Test void b05_endUnknownStepDropped() { assertAllTrue(r(), "red05_endUnknownStepDropped"); }
	@Test void b06_suitePlaceholderThenReplaceCounts() { assertAllTrue(r(), "red06_suitePlaceholderThenReplaceCounts"); }
	@Test void b07_testsWinOverPlaceholders() { assertAllTrue(r(), "red07_testsWinOverPlaceholders"); }
	@Test void b08_testsNotDeduplicated() { assertAllTrue(r(), "red08_testsNotDeduplicated"); }
	@Test void b09_replaceClearsStep() { assertAllTrue(r(), "red09_replaceClearsStep"); }
	@Test void b10_noteAttachment() { assertAllTrue(r(), "red10_noteAttachment"); }
	@Test void b11_doneLatestWinsAndLateNoteApplies() { assertAllTrue(r(), "red11_doneLatestWinsAndLateNoteApplies"); }
	@Test void b12_derivedClosing() { assertAllTrue(r(), "red12_derivedClosing"); }
	@Test void b13_runStatusTable() { assertAllTrue(r(), "red13_runStatusTable"); }
	@Test void b14_seqIdempotent() { assertAllTrue(r(), "red14_seqIdempotent"); }
	@Test void b15_retentionCaps() { assertAllTrue(r(), "red15_retentionCaps"); }
	@Test void b16_structuralCaps() { assertAllTrue(r(), "red16_structuralCaps"); }
	@Test void b17_countsExactPerFw() { assertAllTrue(r(), "red17_countsExactPerFw"); }
	@Test void b18_stepCapDropsOnlyNewIds() { assertAllTrue(r(), "red18_stepCapDropsOnlyNewIds"); }
	@Test void b19_terminalWithoutDoneIsStopped() { assertAllTrue(r(), "red19_terminalWithoutDoneIsStopped"); }
	@Test void c01_createAndRoot() { assertAllTrue(r(), "core01_createAndRoot"); }
	@Test void c02_badOptionsThrowBeforeDom() { assertAllTrue(r(), "core02_badOptionsThrowBeforeDom"); }
	@Test void c03_oneRenderPerBatch() { assertAllTrue(r(), "core03_oneRenderPerBatch"); }
	@Test void c04_destroyIsIdempotent() { assertAllTrue(r(), "core04_destroyIsIdempotent"); }
	@Test void d01_headlineAndStatus() { assertAllTrue(r(), "sum01_headlineAndStatus"); }
	@Test void d02_countsUseBuiltInLabels() { assertAllTrue(r(), "sum02_countsUseBuiltInLabels"); }
	@Test void d03_failuresListCaps() { assertAllTrue(r(), "sum03_failuresListCaps"); }
	@Test void d04_busEmitsOnStatusChangeOnly() { assertAllTrue(r(), "sum04_busEmitsOnStatusChangeOnly"); }
	@Test void e01_createdOnceUpdatedInPlace() { assertAllTrue(r(), "step01_createdOnceUpdatedInPlace"); }
	@Test void e02_rawLinkNeedsLineAndTemplate() { assertAllTrue(r(), "step02_rawLinkNeedsLineAndTemplate"); }
	@Test void e02b_rawOffsetLink() { assertAllTrue(r(), "step02b_rawOffsetLink"); }
	@Test void e03_exitCodeTooltip() { assertAllTrue(r(), "step03_exitCodeTooltip"); }
	@Test void e04_doneClosesOpenSteps() { assertAllTrue(r(), "step04_doneClosesOpenSteps"); }
	@Test void e05_notes() { assertAllTrue(r(), "step05_notes"); }
	@Test void f01_hostileTextIsInert() { assertAllTrue(r(), "esc01_hostileTextIsInert"); }
	@Test void g01_resetStateStats() { assertAllTrue(r(), "api01_resetStateStats"); }
	@Test void h01_cleanCollapsedFailingExpanded() { assertAllTrue(r(), "suite01_cleanCollapsedFailingExpanded"); }
	@Test void h02_toggleAndPersistence() { assertAllTrue(r(), "suite02_toggleAndPersistence"); }
	@Test void h03_placeholderShowsCounts() { assertAllTrue(r(), "suite03_placeholderShowsCounts"); }
	@Test void i01_blockClassesAndAttributes() { assertAllTrue(r(), "blk01_blockClassesAndAttributes"); }
	@Test void i02_anchorOnlyWithRawLineAndTemplate() { assertAllTrue(r(), "blk02_anchorOnlyWithRawLineAndTemplate"); }
	@Test void i03_eventCannotSupplyHref() { assertAllTrue(r(), "blk03_eventCannotSupplyHref"); }
	@Test void i04_traceDisclosure() { assertAllTrue(r(), "blk04_traceDisclosure"); }
	@Test void i05_traceReachableFromFailuresList() { assertAllTrue(r(), "blk05_traceReachableFromFailuresList"); }
	@Test void i06_blocksCarryStepId() { assertAllTrue(r(), "blk03b_blocksCarryStepId"); }
	@Test void i07_onBlockClickGetsStepLineKind() { assertAllTrue(r(), "blk03c_onBlockClickGetsStepLineKind"); }
	@Test void i09_onBlockClickOnKeyboardNoDoubleFire() { assertAllTrue(r(), "blk03e_onBlockClickOnKeyboardNoDoubleFire"); }
	@Test void i08_onBlockClickMustBeFunction() { assertAllTrue(r(), "blk03d_onBlockClickMustBeFunction"); }
	@Test void i10_onBlockClickGetsOffset() { assertAllTrue(r(), "blk03f_onBlockClickGetsOffset"); }
	@Test void j01_showHide() { assertAllTrue(r(), "tip01_showHide"); }
	@Test void j02_positionStaysInViewport() { assertAllTrue(r(), "tip02_positionStaysInViewport"); }
	@Test void j03_stepExitTooltip() { assertAllTrue(r(), "tip03_stepExitTooltip"); }
	@Test void k01_domBounds() { assertAllTrue(r(), "cap01_domBounds"); }
	@Test void k02_collapsedBuildsNoBlocks() { assertAllTrue(r(), "cap02_collapsedBuildsNoBlocks"); }
	@Test void l01_untouchedStepKeepsItsNode() { assertAllTrue(r(), "inc01_untouchedStepKeepsItsNode"); }
	@Test void l02_rowRebuiltOnlyWhenSuiteChanged() { assertAllTrue(r(), "inc02_rowRebuiltOnlyWhenSuiteChanged"); }
	@Test void m01_compactMode() { assertAllTrue(r(), "cmp01_compactMode"); }
	@Test void n01_fullGolden() { assertAllTrue(r(), "gold01_fullGolden"); }
	@Test void n02_compactGolden() { assertAllTrue(r(), "gold02_compactGolden"); }
	@Test void n03_replaceFlow() { assertAllTrue(r(), "gold03_replaceFlow"); }
	@Test void n04_idempotentAppend() { assertAllTrue(r(), "gold04_idempotentAppend"); }
	@Test void o01_groupingByIdAndTitle() { assertAllTrue(r(), "att01_groupingByIdAndTitle"); }
	@Test void o02_collapsedByDefaultWithToggle() { assertAllTrue(r(), "att02_collapsedByDefaultWithToggle"); }
	@Test void o03_newAttemptRegroupsAndKeepsExpansion() { assertAllTrue(r(), "att03_newAttemptRegroupsAndKeepsExpansion"); }
	@Test void o04_noAttemptsNoToggle() { assertAllTrue(r(), "att04_noAttemptsNoToggle"); }
	@Test void o05_failureLinkOpensEarlierAttempt() { assertAllTrue(r(), "att05_failureLinkOpensEarlierAttempt"); }
	@Test void o06_resetClearsAttemptState() { assertAllTrue(r(), "att06_resetClearsAttemptState"); }

	@Test void z01_everyCasePassesAndNoneIsMissing() {
		var r = r();
		assertFalse(r.isEmpty(), "harness reported no cases");
		for (var e : r.entrySet())
			assertEquals(true, e.getValue(), () -> e.getKey() + " -> " + e.getValue());
		assertEquals(EXPECTED, new TreeSet<>(r.keySet().stream().map(String::valueOf).toList()), "harness case names");
	}
}
