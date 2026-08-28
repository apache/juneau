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
 * Always-on coverage for the row-detail JS helpers.  Source-shape always runs; behavioral Node harness runs
 * when {@code node} is on {@code PATH} (skipped otherwise — no {@code -Pjs-tests} required).
 */
class ViewsJs_RowDetail_Test extends TestBase {

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

	@Test void a01_helpersExportedOnNsInit() throws Exception {
		var body = viewsJs();
		for (var name : new String[]{
			"isSafeDetailUrl: isSafeDetailUrl",
			"substituteDetailUrl: substituteDetailUrl",
			"scalarFieldValue: scalarFieldValue",
			"isSafeMarkdownHref: isSafeMarkdownHref",
			"fillMarkdownSlot: fillMarkdownSlot",
			"fillRenderSlot: fillRenderSlot",
			"fillDetailSlots: fillDetailSlots",
			"resolveDetailHeaderIcon: resolveDetailHeaderIcon",
			"paintActionMessageIntoDetail: paintActionMessageIntoDetail",
			"findRowDetailTemplate: findRowDetailTemplate",
			"detailTabTargetIndex: detailTabTargetIndex",
			"activateDetailTab: activateDetailTab",
			"buildDetailStrip: buildDetailStrip",
			"JUNEAU_ROW_DETAIL_CONTRACT_VERSION: JUNEAU_ROW_DETAIL_CONTRACT_VERSION"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
		assertFalse(body.contains("function buildDetailFields("), body);
		assertFalse(body.contains("function buildDetailPanel("), body);
		assertTrue(body.contains("submitRowAction(action, table, parentTr"),
			"write path must target the expanded DataTables row, not expand JSON");
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
			report = Json.to(runNode(harness, viewsFile, rendersFile), Map.class);
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
			var p = Path.of(basedir, "src/test/js/row-detail.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/row-detail.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/row-detail.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path viewsJs, Path rendersJs) throws Exception {
		var stdout = Files.createTempFile("row-detail-stdout-", ".json");
		var stderr = Files.createTempFile("row-detail-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), viewsJs.toString(), rendersJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("row-detail.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("row-detail.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or row-detail.cjs not found — behavioral layer skipped");
		return report;
	}

	/** Compares a numeric report value regardless of whether the JSON parser boxed it as Integer or Long. */
	private static void assertNum(long expected, Object actual) {
		assertInstanceOf(Number.class, actual, () -> "expected a number, got: " + actual);
		assertEquals(expected, ((Number)actual).longValue());
	}

	@Test void b01_urlSafety() {
		var r = report();
		assertEquals(true, r.get("url_pathOk"));
		assertEquals(true, r.get("url_relativeOk"));
		assertEquals(false, r.get("url_absolute"));
		assertEquals(false, r.get("url_protoRel"));
		assertEquals(false, r.get("url_scheme"));
		assertEquals(false, r.get("url_dotdot"));
	}

	@Test void b02_hostileId_isEncoded_notPathTraversal() {
		var r = report();
		assertEquals(true, r.get("sub_hostileEncoded"));
		assertNull(r.get("sub_absoluteTpl"));
		assertEquals("/data/alerts/a1", r.get("sub_plain"));
		var hostileUrl = String.valueOf(r.get("sub_hostile"));
		assertFalse(hostileUrl.contains("../"), hostileUrl);
		assertTrue(hostileUrl.startsWith("/data/alerts/"), hostileUrl);
	}

	@Test void b03_scalarAndBasicFill_usesTextContent() {
		var r = report();
		assertEquals("hi", r.get("scalar_str"));
		assertEquals("7", r.get("scalar_num"));
		assertEquals("true", r.get("scalar_bool"));
		assertEquals("", r.get("scalar_null"));
		assertEquals("", r.get("scalar_obj"));
		assertEquals("", r.get("scalar_arr"));
		assertEquals(true, r.get("fill_xssNotInterpreted"));
		assertTrue(String.valueOf(r.get("fill_xss")).contains("<img"));
		assertEquals("42", r.get("fill_num"));
		assertEquals("", r.get("fill_missing"));
	}

	@Test void b17_markdownSlotFill_xssSafety() {
		var r = report();
		assertEquals(true, r.get("hasFillMarkdown"));
		assertEquals(false, r.get("md_hasScript"));
		assertEquals(false, r.get("md_hasImg"));
		assertEquals(false, r.get("md_jsHref"));
		assertEquals(true, r.get("md_httpsHref"));
		assertEquals(true, r.get("md_textHasOk"));
		assertEquals(true, r.get("md_textHasX"));
		assertEquals(true, r.get("md_textHasY"));
		assertEquals(false, r.get("md_textHasAlert"));
	}

	@Test void b18_hrefAndTitleFill_xssSafety() {
		var r = report();
		assertEquals(false, r.get("href_js"));
		assertEquals(true, r.get("href_https"));
		assertEquals(false, r.get("href_data"));
		assertEquals("Incident #42", r.get("title_filled"));
		assertEquals(true, r.get("title_xssNotInterpreted"));
		assertTrue(String.valueOf(r.get("title_xss")).contains("<img"));
	}

	@Test void b19_paintActionMessageAndHeaderIcon() {
		var r = report();
		assertEquals(true, r.get("hasPaintActionMessageIntoDetail"));
		assertEquals("<b>disk full</b>", r.get("paint_text"));
		assertEquals(true, r.get("paint_xssNotInterpreted"));
		assertEquals(true, r.get("hasResolveDetailHeaderIcon"));
		assertEquals(true, r.get("icon_unknownHidden"));
	}

	@Test void b07_fillRenderSlot_tagProgressLinkedAndCanary() {
		var r = report();
		assertEquals(true, r.get("hasFillRender"));
		assertEquals(true, r.get("rr_tagHasClass"));
		assertEquals(true, r.get("rr_progressWidth"));
		assertEquals(true, r.get("rr_linkedHref"));
		assertEquals(false, r.get("rr_jsHref"));
		assertEquals(false, r.get("rr_hasScript"));
		assertEquals(false, r.get("rr_hostileStyle"));
		assertEquals(true, r.get("rr_truncateTitle"));
		assertEquals(true, r.get("rr_jsonCode"));
		assertEquals(true, r.get("rr_malformedMetaOk"));
		assertEquals("", r.get("rr_missing"));
		assertEquals(true, r.get("rr_dispatchRenderFirst"));
	}

	/**
	 * Pins the fix for {@code javascript:S5852} in {@code copyRenderStyle}.  Its width pattern ended in three
	 * independent {@code \s*} runs separated by optional {@code %} and {@code ;}, so a single whitespace tail
	 * could be split among them in cubically many ways; a style attribute of {@code width:1} + 4,000 spaces +
	 * a non-terminator backtracked for ~10.7s before the fix and ~0ms after.  The attribute must still be
	 * rejected - it is not a bare width declaration - so this pins throughput, not a change in what is allowed.
	 */
	@Test void b08_fillRenderSlot_widthStylePatternIsLinearOnAWhitespaceTail() {
		var r = report();
		assertEquals(true, r.get("rr_slowStyleSawSpan"));    // the span survived parsing; the guard really ran
		assertEquals(true, r.get("rr_slowStyleRejected"));   // still not copied through as a style attribute
		var ms = ((Number) r.get("rr_slowStyleMs")).longValue();
		assertTrue(ms < 2000, () -> "copyRenderStyle took " + ms + "ms on a 4,000-space tail; expected linear");
	}

	@Test void b04_404500_actionRefButtonless_collapseRemains() {
		var r = report();
		assertEquals(true, r.get("fail_ackDisabled"));
		assertEquals(true, r.get("fail_ackHidden"));
		assertEquals(true, r.get("fail_escHidden"));
		assertEquals(true, r.get("fail_collapseEnabled"));
	}

	@Test void b05_coalesceKeyAndDropIfOrphaned() {
		var r = report();
		assertEquals("a1:3", r.get("key_a1"));
		assertEquals(true, r.get("drop_gone"));
		assertEquals(true, r.get("drop_gen"));
		assertEquals(false, r.get("drop_keep"));
	}

	@Test void b06_loudContractMismatch() {
		var r = report();
		assertEquals("1", r.get("contractVersion"));
		assertEquals(true, r.get("contract_ok"));
		assertEquals(false, r.get("contract_bad"));
		assertEquals(false, r.get("contract_missing"));
	}

	@Test void b08_tabTargetIndex_rovingKeyboardMath() {
		var r = report();
		assertEquals(true, r.get("hasDetailTabTargetIndex"));
		assertNum(1, r.get("tti_right"));
		assertNum(0, r.get("tti_rightWrap"));     // ArrowRight wraps to the first tab
		assertNum(2, r.get("tti_left"));          // ArrowLeft wraps to the last tab
		assertNum(0, r.get("tti_home"));
		assertNum(2, r.get("tti_end"));
		assertNum(-1, r.get("tti_other"));        // unhandled key
	}

	@Test void b09_multiSection_buildsTabStrip_oneVisiblePane() {
		var r = report();
		assertEquals(true, r.get("hasBuildDetailStrip"));
		assertEquals(true, r.get("strip_built"));
		assertEquals(true, r.get("strip_isFirstChild"));
		assertEquals("tablist", r.get("strip_role"));
		assertEquals("tab", r.get("strip_mode"));
		assertEquals(true, r.get("strip_hasRibbonGroupClass"));   // shared strip widget - not a new grammar
		assertNum(2, r.get("strip_tabCount"));
		assertEquals("Overview,Context", r.get("strip_labels"));
		assertEquals("juneau-view-ribbon-btn", r.get("strip_btnClass"));
		assertEquals("true", r.get("strip_firstSelected"));
		assertEquals("false", r.get("strip_secondSelected"));
		assertNum(0, r.get("strip_firstTabindex"));
		assertNum(-1, r.get("strip_secondTabindex"));         // roving tabindex
		assertEquals(false, r.get("strip_pane0Hidden"));          // first pane initially visible
		assertEquals(true, r.get("strip_pane1Hidden"));
		assertEquals("tabpanel", r.get("strip_pane0Role"));
		assertEquals(true, r.get("strip_pane0Labelledby"));
		assertEquals(true, r.get("strip_tab0Controls"));
		assertEquals(true, r.get("strip_titleHidden"));           // stacked <h2> hidden - the tab replaces it
	}

	@Test void b10_activateDetailTab_visibilityOnly() {
		var r = report();
		assertEquals(true, r.get("hasActivateDetailTab"));
		assertEquals(true, r.get("act_tab1Selected"));
		assertEquals(true, r.get("act_tab0Deselected"));
		assertEquals(true, r.get("act_pane1Visible"));
		assertEquals(true, r.get("act_pane0Hidden"));
	}

	@Test void b11_keyboardNavigation_moveSelectionAndFocus() {
		var r = report();
		assertEquals(true, r.get("kbd_right_tab1Selected"));
		assertEquals(true, r.get("kbd_right_tab0Deselected"));
		assertEquals(true, r.get("kbd_right_pane1Visible"));
		assertEquals(true, r.get("kbd_right_focusMoved"));
		assertEquals(true, r.get("kbd_home_tab0Selected"));
		assertEquals(true, r.get("kbd_end_tab1Selected"));
		assertEquals(true, r.get("kbd_left_tab0Selected"));
		assertEquals(true, r.get("kbd_enter_noop"));
	}

	@Test void b12_singleSection_staysStripLess() {
		var r = report();
		assertEquals(true, r.get("single_noStrip"));
		assertEquals(true, r.get("single_firstStillSection"));
		assertEquals(true, r.get("single_paneNotHidden"));
		assertEquals(true, r.get("single_noTabpanelRole"));       // no lone tab, no tabpanel role
		assertEquals(true, r.get("single_titleNotHidden"));
	}

	@Test void b13_skillsFixture_becomesTabs() {
		var r = report();
		assertEquals("Skill,SKILL.md", r.get("skills_labels"));
		assertNum(2, r.get("skills_tabCount"));
	}

	@Test void b14_tabSwitch_parentVisibilityOnly_firesNestedActivationSeam() {
		var r = report();
		assertNum(0, r.get("noRefetch_fetchCalls"));          // parent detail envelope is DOM-visibility only
		assertEquals(true, r.get("noRefetch_clickSelectedTab0"));
		// The onActivate seam (the hook a newly-shown pane's nested table uses to run its own GET) fires on every
		// tab activation - keyboard then click - carrying the correct section id + the activated pane.
		assertNum(2, r.get("noRefetch_onActivateCount"));
		assertEquals("b", r.get("noRefetch_onActivateFirstSid"));
		assertEquals("a", r.get("noRefetch_onActivateLastSid"));
		assertEquals(true, r.get("noRefetch_onActivatePaneMatches"));
	}

	@Test void b15_headerKeepsStripAfterHeader() {
		var r = report();
		assertEquals(true, r.get("header_firstIsHeader"));
		assertEquals(true, r.get("header_stripAfterHeader"));
	}

	@Test void b16_findRowDetailTemplate_survivesDataTablesWrap() {
		var r = report();
		assertEquals(true, r.get("hasFindRowDetailTemplate"));
		assertEquals(true, r.get("find_sibling"));
		assertEquals(true, r.get("find_dt2Wrap"));
		assertEquals(true, r.get("find_missing"));
	}

	@Test void c01_actionRuleHelpersExported_andEvaluatedAfterTheLifecycleGate() throws Exception {
		var body = viewsJs();
		for (var name : new String[]{
			"actionRuleMatches: actionRuleMatches",
			"firstFailingActionRule: firstFailingActionRule",
			"applyActionRefRules: applyActionRefRules",
			"mintActionDescIdentity: mintActionDescIdentity"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
		// Ordering is the whole composition story: the rule pass must run AFTER setActionRefEnabled on the success
		// path, so it can only narrow the lifecycle gate rather than fight it.
		var enableAt = body.indexOf("setActionRefEnabled(panel, true);");
		var rulesAt = body.indexOf("applyActionRefRules(panel, body.fields);");
		assertTrue(enableAt >= 0, "expand success path no longer enables ActionRefs through setActionRefEnabled");
		assertTrue(rulesAt > enableAt, "the rule pass must run after the lifecycle enable, never before it");
		// Disable-only (D2): no hide/show branch anywhere in the rule pass.
		var pass = body.substring(body.indexOf("function applyActionRefRules("));
		pass = pass.substring(0, pass.indexOf("\n\t}"));
		assertFalse(pass.contains("hidden"), pass);
		assertFalse(pass.contains("disabled = false"), pass);
	}

	@Test void c02_ruleEvaluation_isThreeWay_matchNoMatchAndAbsentField() {
		var r = report();
		assertEquals(true, r.get("hasApplyActionRefRules"));
		assertEquals(true, r.get("hasMintActionDescIdentity"));
		// Matching: untouched, and no reason left attached.
		assertEquals(true, r.get("rule_matchStaysEnabled"));
		assertEquals(true, r.get("rule_matchNoTitle"));
		assertEquals(true, r.get("rule_matchNoDescribedby"));
		assertEquals(true, r.get("rule_matchNoReasonText"));
		// Not matching: PRESENT and DISABLED - asserting both is what makes this test fail on a rule that never
		// evaluated, which a presence-only assertion would pass.
		assertEquals(true, r.get("rule_noMatchDisabled"));
		assertEquals(true, r.get("rule_noMatchStillPresent"));
		assertEquals("This alert is not open.", r.get("rule_noMatchTitle"));
		assertEquals("This alert is not open.", r.get("rule_noMatchReasonText"));
		assertEquals(true, r.get("rule_noMatchDescribedbyPointsAtNode"));
		// Field missing from the payload: fails closed, still present.
		assertEquals(true, r.get("rule_absentFieldDisabled"));
		assertEquals(true, r.get("rule_absentFieldStillPresent"));
		assertEquals("This alert is not open.", r.get("rule_absentFieldTitle"));
	}

	@Test void c03_operatorSemantics() {
		var r = report();
		assertEquals(true, r.get("rule_eqYes"));
		assertEquals(false, r.get("rule_eqNo"));
		assertEquals(false, r.get("rule_eqAbsentKey"));       // fail closed, not fail open
		assertEquals(true, r.get("rule_neYes"));
		assertEquals(false, r.get("rule_neNo"));
		assertEquals(true, r.get("rule_absentOnEmpty"));      // present/absent read emptiness, not key existence
		assertEquals(false, r.get("rule_absentOnValue"));
		assertEquals(true, r.get("rule_presentOnValueEnabled"));
		assertEquals(true, r.get("rule_presentOnEmptyDisabled"));
		assertEquals(true, r.get("rule_eqCoercesNumber"));
	}

	@Test void c04_firstDeclaredFailingRuleWins() {
		var r = report();
		// Two rules, both failing, DIFFERENT reasons, declared in both orders: the answer flips with the
		// declaration, which is the only way declaration order is observable at all.
		assertEquals("REASON-STATE", r.get("rule_firstDeclaredWins"));
		assertEquals("REASON-TIER", r.get("rule_firstDeclaredWinsReversed"));
		assertEquals("REASON-STATE", r.get("rule_firstFailingHelper"));
		assertNull(r.get("rule_firstFailingHelperAllPass"));
	}

	@Test void c05_bothReasonChannelsClearTogether() {
		var r = report();
		assertEquals(true, r.get("rule_clearPreconditionTitleSet"));
		assertEquals(true, r.get("rule_clearedTitle"));
		assertEquals(true, r.get("rule_clearedDescribedby"));
		assertEquals(true, r.get("rule_clearedReasonText"));
	}

	@Test void c06_rulePassNeverReEnablesAndNeverHides() {
		var r = report();
		assertEquals(true, r.get("rule_lifecycleDisabledStaysDisabled"));
		assertEquals(true, r.get("rule_hiddenStaysHidden"));
		assertEquals(true, r.get("rule_hiddenStaysDisabled"));
		assertEquals(true, r.get("rule_passNeverHides"));
	}

	@Test void c07_reasonNodeIdentityIsPerRowAndPerAction() {
		var r = report();
		assertEquals(true, r.get("rule_descIdsUnique"));
		assertEquals(true, r.get("rule_row1PointsAtOwnNode"));
		assertEquals(true, r.get("rule_row2PointsAtOwnNode"));
		assertEquals(true, r.get("rule_perActionIdsDiffer"));
		assertEquals(true, r.get("rule_bothActionsGated"));
		var id1 = String.valueOf(r.get("rule_descId1"));
		assertTrue(id1.contains("alerts") && id1.contains("a1") && id1.contains("ack"), id1);
	}

	@Test void c08_malformedOrEmptyRulesGateNothing() {
		var r = report();
		assertEquals(true, r.get("rule_malformedGatesNothing"));
		assertEquals(true, r.get("rule_emptyRulesGatesNothing"));
	}
}
