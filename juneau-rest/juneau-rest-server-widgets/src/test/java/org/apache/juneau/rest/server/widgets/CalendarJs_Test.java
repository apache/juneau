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
package org.apache.juneau.rest.server.widgets;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
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
 * Always-on coverage for the {@code juneau-calendar.js} runtime owned by this module.  The source-shape
 * layer always runs (the pure helpers must be exported on {@code JuneauCalendar.pure} and event fill must
 * be {@code textContent}, never {@code innerHTML}); the behavioral Node harness runs when {@code node} is
 * on {@code PATH} (skipped otherwise — no {@code -Pjs-tests} required).
 */
class CalendarJs_Test extends TestBase {

	private static String calendarJs() throws IOException {
		try (var in = WidgetsMixin.class.getResourceAsStream(WidgetsMixin.CALENDAR_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + WidgetsMixin.CALENDAR_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_pureHelpersExportedOnNamespace() throws Exception {
		var body = calendarJs();
		for (var name : new String[]{
			"pad2: pad2",
			"daysInMonth: daysInMonth",
			"dayOfWeek: dayOfWeek",
			"firstWeekdayOffset: firstWeekdayOffset",
			"toEpochDay: toEpochDay",
			"fromEpochDay: fromEpochDay",
			"dateKey: dateKey",
			"buildMonthCells: buildMonthCells",
			"civilKey: civilKey",
			"contractOk: contractOk",
			"echoOk: echoOk",
			"sanitizeEvents: sanitizeEvents",
			"colorToken: colorToken",
			"isSafeDocumentUrl: isSafeDocumentUrl",
			"substituteEndpoint: substituteEndpoint",
			"eventsForDay: eventsForDay",
			"applyCap: applyCap",
			"coalesceKey: coalesceKey",
			"lastDayKey: lastDayKey",
			"spanning: spanning",
			"startTimeLabel: startTimeLabel",
			"malformedReason: malformedReason",
			"chipCompare: chipCompare",
			"laneBudgetFor: laneBudgetFor",
			"buildSegments: buildSegments"
		})
			assertTrue(body.contains(name), () -> "missing pure export '" + name + "'");
		// The DOM entry points are exposed for the harness/browser runtime.
		assertTrue(body.contains("NS.initInstance = initInstance"), body);
		assertTrue(body.contains("NS.fillEventNode = fillEventNode"), body);
	}

	/** Strips {@code //} line comments and block comments so shape assertions see executable code only. */
	private static String stripComments(String js) {
		return js.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
	}

	@Test void a02_eventFill_usesTextContent_neverInnerHtml() throws Exception {
		var body = calendarJs();
		// Security-critical: chip/title text goes in via textContent so an "<img onerror>" title stays literal.
		assertTrue(body.contains("node.textContent = event.title"), body);
		var code = stripComments(body);
		assertFalse(code.contains("innerHTML"), "runtime must never assign innerHTML: " + code);
	}

	@Test void a03_civilDate_neverDateParse() throws Exception {
		// Civil-date bucketing must be field-wise; Date.parse of a date-only string would timezone-shift it.
		var code = stripComments(calendarJs());
		assertFalse(code.contains("Date.parse"), code);
		assertFalse(code.contains("new Date("), code);
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var calendarFile = Files.createTempFile("juneau-calendar-", ".js");
		try {
			Files.writeString(calendarFile, calendarJs(), UTF_8);
			report = Json.to(runNode(harness, calendarFile), Map.class);
		} finally {
			Files.deleteIfExists(calendarFile);
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
			var p = Path.of(basedir, "src/test/js/juneau-calendar.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/juneau-calendar.cjs",
			"juneau-rest/juneau-rest-server-widgets/src/test/js/juneau-calendar.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path calendarJs) throws Exception {
		var stdout = Files.createTempFile("juneau-calendar-stdout-", ".json");
		var stderr = Files.createTempFile("juneau-calendar-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), calendarJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("juneau-calendar.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("juneau-calendar.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or juneau-calendar.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_contractAndMinPoll() {
		var r = report();
		assertBean(r, "contractVersion,minPoll", "2,5000");
	}

	/** Contract lockstep: the JS literal and the bean constant must move together, or old JS misrenders silently. */
	@Test void a04_contractVersion_inLockstepWithTheBean() throws Exception {
		var body = calendarJs();
		assertTrue(body.contains("JUNEAU_CALENDAR_CONTRACT_VERSION = \"" + CalendarDef.CONTRACT_VERSION + "\""), body);
	}

	@Test void a05_noLocalLayerStack_popoverUsesTheSharedOne() throws Exception {
		var code = stripComments(calendarJs());
		// Rec 8 / rec F: the "+N more" popover is a CLIENT of the one shared stack, never a second stack.
		assertTrue(code.contains("window.JuneauViews?.init"), code);
		assertTrue(code.contains("stack.pushLayer(pop,"), code);
		assertFalse(code.contains("function pushLayer"), "the calendar must not define its own pushLayer: " + code);
		assertFalse(code.contains("function popLayer"), "the calendar must not define its own popLayer: " + code);
	}

	@Test void b02_daysInMonth_leapYearCorrect() {
		var r = report();
		assertBean(r, "dim_feb2024,dim_feb2026,dim_apr,dim_jan", "29,28,30,31");
	}

	@Test void b03_firstWeekdayOffset_bothWeekStarts() {
		var r = report();
		// dow_aug1: Aug 1 2026 is a Saturday.
		assertBean(r, "dow_aug1,off_sunday,off_monday", "6,6,5");
	}

	@Test void b04_buildMonthCells_always42_adjacentTagged() {
		var r = report();
		// cells_firstOfMonthAt6: Sunday-start, 6 leading adjacent cells; cellsMon_firstOfMonthAt5: Monday-start, 5.
		assertBean(r, "cells_count,cellsMon_count,cells_firstInMonth,cells_leadingAdjacent,cells_firstOfMonthAt6,"
			+ "cellsMon_firstOfMonthAt5,cells_inMonthCount", "42,42,2026-08-01,true,true,true,31");
	}

	@Test void b05_civilBucketing_neverDateParseShift() {
		var r = report();
		// civil_dateTime: leading date only, no tz shift.
		assertBean(r, "civil_dateOnly,civil_dateTime,civil_bad,civil_short,civil_badSep",
			"2026-08-14,2026-08-14,<null>,<null>,<null>");
	}

	@Test void b06_contractHandshake_strictStringTwo() {
		var r = report();
		// contract_badNum: numeric 2 must fail strict ===; contract_bad1: so must the superseded v1 string.
		assertBean(r, "contract_okStr,contract_badNum,contract_bad1", "true,false,false");
	}

	@Test void b07_echoCheck() {
		var r = report();
		assertBean(r, "echo_ok,echo_badMonth,echo_null", "true,false,false");
	}

	@Test void b08_sanitize_dropsMissingAndDupWithWarn() {
		var r = report();
		// sanitize_ids's value embeds a comma ("a,b") - harmless, assertBean compares the whole joined string.
		assertBean(r, "sanitize_ids,sanitize_warned", "a,b,true");
	}

	@Test void b09_colorToken_unknownFallsToNeutralWithWarn() {
		var r = report();
		assertBean(r, "color_known,color_unknown,color_unknownWarned,color_none", "blue,neutral,true,neutral");
	}

	@Test void b10_documentUrlSafety() {
		var r = report();
		assertBean(r, "url_path,url_rel,url_abs,url_protoRel,url_scheme,url_dotdot",
			"true,true,false,false,false,false");
	}

	@Test void b11_substituteAndCapAndCoalesceKey() {
		var r = report();
		// eventsForDay_ids's value embeds a comma ("1,2") - harmless, see note on b08 above.
		assertBean(r, "sub,eventsForDay_ids,cap_shown,cap_overflow,coalesce",
			"/events/2026/8,1,2,3,2,cal1:2026-8:4");
	}

	@Test void b12_eventFill_usesTextContent_noMarkup() {
		var r = report();
		assertBean(r, "fill_tag,fill_noChildEls,fill_class,fill_linkedTag,fill_linkedHref,fill_unsafeTag,"
			+ "fill_unsafeNoHref", "SPAN,0,jc-cal-event jc-cal-cat--blue,A,/events/1,SPAN,true");
		assertTrue(String.valueOf(r.get("fill_text")).contains("<img"));   // literal text, not parsed markup
	}

	@Test void b13_readCategoryMap_skipsColumnHeader() {
		var r = report();
		assertBean(r, "map_team,map_review,map_noHeader", "blue,green,true");
	}

	@Test void b14_seedMonth_paintedFromSidecar_noFetch() {
		var r = report();
		assertBean(r, "seed_weeks,seed_painted,seed_noFetch,seed_todayCell", "6,true,true,true");
	}

	@Test void b15_contractMismatch_failsLoud_noFetchNoPaint() {
		var r = report();
		assertBean(r, "badContract_error,badContract_noFetch,badContract_notInit,badContract_loud",
			"true,true,true,true");
	}

	@Test void b16_liveBodyNumericContract_refused() {
		var r = report();
		assertEquals(true, r.get("liveNumeric_error"));
	}

	@Test void b17_echoCheck_wrongMonthBodyDropped() {
		var r = report();
		assertEquals(true, r.get("echo_dropped"));
	}

	@Test void b18_coalesceAndAbort_staleMonthDropped() {
		var r = report();
		assertBean(r, "coalesce_aborted,coalesce_octPainted,coalesce_staleDropped", "true,true,true");
	}

	@Test void b19_fetchError_singleAttempt_emptyMonthVisibleError() {
		var r = report();
		assertBean(r, "err_visible,err_singleAttempt,err_emptyMonth,html_error", "true,true,true,true");
	}

	//------------------------------------------------------------------------------------------------------------------
	// `end` split inclusivity and the closed malformed set, mirrored from CalendarEvent.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_endInclusivity_allFourCases() {
		var r = report();
		// last_allDayInclusive: date-only end is INCLUSIVE. last_timedExclusive: [09:00, 10:00) is the start day
		// only. last_timedEndsAtMidnight: exclusive, so midnight lands the day before.
		assertBean(r, "last_allDayInclusive,last_timedExclusive,last_timedMidnightCrossing,last_timedEndsAtMidnight,"
			+ "last_allDaySameDay,last_omittedEnd,last_offsetIgnored",
			"2026-03-04,2026-03-02,2026-03-03,2026-03-02,2026-03-02,2026-03-02,2026-03-02");
	}

	@Test void c02_spanning_onlyWhenMoreThanOneDayCellIsCovered() {
		var r = report();
		// span_timedMidnight: a timed crossing is a BAR. span_omittedEnd: start-only is never a bar.
		assertBean(r, "span_allDayThreeDay,span_timedHour,span_timedMidnight,span_omittedEnd", "true,false,true,false");
	}

	@Test void c03_malformedSet_isExactlyTheClosedList() {
		var r = report();
		assertNull(r.get("mal_ok"));
		for (var k : new String[]{"mal_noId", "mal_noTitle", "mal_noStart", "mal_badStart", "mal_badEnd",
			"mal_allDayTrueWithTimedEnd", "mal_allDayFalseWithDateEnd", "mal_mixedShapesNullAllDay",
			"mal_timedZeroDuration", "mal_endBeforeStart"})
			assertEquals(true, r.get(k), k);
		// And these are NOT malformed - no new kinds were invented.
		for (var k : new String[]{"mal_omittedEndOk", "mal_nullAllDayDateOnlyOk", "mal_nullAllDayTimedOk",
			"mal_offsetOk", "mal_allDaySameDayOk"})
			assertEquals(true, r.get(k), k);
	}

	@Test void c04_malformedEventsAreDropped_theRestStillPaint() {
		assertEquals("good1,good2", report().get("dropMalformed_ids"));
	}

	@Test void c05_timeLabels() {
		var r = report();
		assertBean(r, "timeLabel_timed,timeLabel_allDay,timeLabel_offset", "09:05,<null>,14:30");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ordering, lanes, segmentation.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_chipOrdering_allDayFirstThenTimedAscending() {
		var r = report();
		// chipOrder_ids's value embeds commas ("a1,a2,t1,t2") - harmless, see note on b08 above. A spanning event
		// is a bar, not a chip, so it's excluded from chipOrder_spanExcluded.
		assertBean(r, "chipOrder_ids,chipOrder_spanExcluded", "a1,a2,t1,t2,ch");
	}

	@Test void d02_laneBudget_derivedAndHardCapped() {
		var r = report();
		assertBean(r, "laneBudget_default,laneBudget_capped", "3,8");
	}

	@Test void d03_weekBoundarySegmentation_withContinuationFlags() {
		var r = report();
		assertBean(r, "seg_weekCross_count,seg_weekCross_shape,seg_weekCross_sameEvent",
			"2,1:6-6:-R 2:0-2:L-,true");
	}

	@Test void d04_spanLongerThanTheMonth_clipsWithFlagsOnBothEnds() {
		var r = report();
		assertBean(r, "seg_clip_count,seg_clip_leftmost,seg_clip_rightmost,seg_clip_allInMonth", "6,true,true,true");
	}

	@Test void d05_laneSeating_andStabilityAcrossRerender() {
		var r = report();
		// seg_lanes's value embeds commas ("a@0,c@0,b@1") - harmless, see note on b08 above.
		assertBean(r, "seg_lanes,seg_stable", "a@0,c@0,b@1,true");
	}

	@Test void d06_lanesBeyondBudgetOverflowIntoMore() {
		var r = report();
		assertBean(r, "seg_budget_seated,seg_budget_laneCount,seg_budget_overflowAtMon", "2,2,s3");
	}

	@Test void d06b_barsDoNotConsumeTheChipBudget() {
		// Ported from the deleted CalendarLayout_Test#e01: two bars plus four chips on one day, maxPerDay = 3.
		// split_shown's value embeds commas ("c1,c2,c3") - harmless, see note on b08 above.
		var r = report();
		assertBean(r, "split_seated,split_laneCount,split_shown,split_overflow,split_hidden", "2,2,c1,c2,c3,1,c4");
	}

	@Test void d07_timedChipsAndBarsPaint() {
		var r = report();
		assertBean(r, "timed_barTitles,timed_chipTitles,timed_label,timed_barEventId,timed_barSpan",
			"Sprint,09:30Standup,09:30,sp,3");
		assertTrue(String.valueOf(r.get("timed_chipClass")).contains("jc-cal-event--timed"), r.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Legend toggle-filter.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_legendToggle_hidesChipsAndSpanningSegments_noRefetch() {
		var r = report();
		// Both chips are all-day on the same day, so the total order falls through to the event id: rc before tc.
		// filter_initialChips/filter_hiddenChips embed a comma - harmless, see note on b08 above. filter_hiddenBars
		// is the empty string (and so is the team spanning bar).
		assertBean(r, "filter_hasToggles,filter_initialChips,filter_initialBars,filter_hiddenChips,filter_hiddenBars,"
			+ "filter_pressedFalse,filter_noRefetch", "true,ReviewChip,TeamChip,TeamBar,ReviewChip,,true,true");
	}

	@Test void e02_legendToggle_revealsAgain() {
		var r = report();
		// filter_revealedChips embeds a comma - harmless, see note on b08 above.
		assertBean(r, "filter_revealedChips,filter_revealedBars,filter_pressedTrueAgain",
			"ReviewChip,TeamChip,TeamBar,true");
	}

	@Test void e03_filterResetsOnMonthNavigation() {
		var r = report();
		assertBean(r, "navReset_painted,navReset_pressed", "SepTeam,true");
	}

	@Test void e04_failedNavigationPreservesTheFilter() {
		var r = report();
		assertBean(r, "navFail_error,navFail_pressed", "true,false");
	}

	//------------------------------------------------------------------------------------------------------------------
	// "+N more" on the ONE shared layer stack.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_moreButton_countsOnlyWhatItHides() {
		assertEquals("+1 more", report().get("pop_moreLabel"));
	}

	@Test void f02_missingSharedStack_failsLoud_noLocalSecondStack() {
		var r = report();
		assertBean(r, "pop_noStackLoud,pop_noStackNoPopover,pop_noStackVisibleError", "true,true,true");
	}

	@Test void f03_popoverRegistersOnSharedStack_escapePopsAndRestoresFocus() {
		var r = report();
		assertBean(r, "pop_registered,pop_lightDismiss,pop_zOrdered,pop_expanded,pop_listsHidden,pop_escapePopped,"
			+ "pop_detached,pop_focusRestored,pop_collapsed", "true,true,true,true,1,true,true,true,false");
	}
}
