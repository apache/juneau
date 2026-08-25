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

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * {@link CalendarLayout} geometry: split {@code end} inclusivity in day terms, per-week spanning-bar segmentation
 * with continuation flags, month clipping, greedy first-fit lane seating against the derived budget, and the
 * {@code maxPerDay}/lane split that decides what "+N more" actually hides.
 *
 * <p>
 * August 2026 is the fixture month throughout: Aug 1 is a <b>Saturday</b>, so on a Sunday-start grid week row 0 is
 * Jul 26 &ndash; Aug 1 and Aug 2 opens week row 1.  That makes the week-boundary and month-clip cases easy to state.
 */
class CalendarLayout_Test extends TestBase {

	private static CalendarEvent ev(String id, String start, String end) {
		var e = CalendarEvent.create().id(id).title(id).start(start);
		return end == null ? e : e.end(end);
	}

	private static CalendarDef def(CalendarEvent...events) {
		return CalendarDef.create().id("cal1").initial(2026, 8).events(events);
	}

	private static List<CalendarLayout.Segment> segmentsOf(CalendarLayout.MonthLayout l, String id) {
		return l.segments().stream().filter(s -> s.event().id.equals(id)).toList();
	}

	/** A compact "week:startColumn-endColumn:flags" shape, so a whole segmentation reads on one line. */
	private static String shape(CalendarLayout.Segment s) {
		return s.week() + ":" + s.startColumn() + "-" + s.endColumn() + ":"
			+ (s.continuesLeft() ? "L" : "-") + (s.continuesRight() ? "R" : "-");
	}

	private static List<String> shapes(List<CalendarLayout.Segment> segments) {
		return segments.stream().map(CalendarLayout_Test::shape).toList();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Grid start.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_gridStart_bothWeekStarts() {
		assertEquals(LocalDate.of(2026, 7, 26), CalendarLayout.gridStart(2026, 8, CalendarDef.WeekStart.SUNDAY));
		assertEquals(LocalDate.of(2026, 7, 27), CalendarLayout.gridStart(2026, 8, CalendarDef.WeekStart.MONDAY));
	}

	//------------------------------------------------------------------------------------------------------------------
	// `end` inclusivity, expressed in day cells.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_allDayEndInclusive_occupiesThreeDayCells() {
		var l = CalendarLayout.of(def(ev("s", "2026-08-03", "2026-08-05")), 2026, 8);
		var covered = 0;
		for (var s : segmentsOf(l, "s"))
			covered += s.columnSpan();
		assertEquals(3, covered);
		assertEquals(List.of("1:1-3:--"), shapes(segmentsOf(l, "s")));
	}

	@Test void b02_timedExclusiveEnd_isAChipNotABar() {
		var l = CalendarLayout.of(def(ev("t", "2026-08-03T09:00", "2026-08-03T10:00")), 2026, 8);
		assertTrue(l.segments().isEmpty());
		assertEquals(List.of("t"), l.day(LocalDate.of(2026, 8, 3)).chips().stream().map(e -> e.id).toList());
	}

	@Test void b03_timedCrossingMidnight_isASpanningBar() {
		var l = CalendarLayout.of(def(ev("t", "2026-08-03T15:00", "2026-08-04T09:00")), 2026, 8);
		assertEquals(List.of("1:1-2:--"), shapes(segmentsOf(l, "t")));
		assertTrue(l.day(LocalDate.of(2026, 8, 3)).chips().isEmpty());
	}

	@Test void b04_omittedEnd_isAChipOnTheStartDay() {
		var l = CalendarLayout.of(def(ev("a", "2026-08-03", null), ev("t", "2026-08-03T15:00", null)), 2026, 8);
		assertTrue(l.segments().isEmpty());
		assertEquals(List.of("a", "t"), l.day(LocalDate.of(2026, 8, 3)).chips().stream().map(e -> e.id).toList());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Segmentation, continuation flags, and month clipping.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_weekBoundary_saturdayToTuesday_emitsTwoSegments() {
		// Aug 8 is a Saturday (row 1, column 6) and Aug 11 a Tuesday (row 2, column 2).
		var l = CalendarLayout.of(def(ev("wc", "2026-08-08", "2026-08-11")), 2026, 8);
		assertEquals(List.of("1:6-6:-R", "2:0-2:L-"), shapes(segmentsOf(l, "wc")));
		// Both pieces carry the SAME event, so hover/focus/filter act on the whole span from either one.
		assertEquals(1, segmentsOf(l, "wc").stream().map(s -> s.event().id).distinct().count());
	}

	@Test void c02_spanLongerThanTheMonth_clipsWithFlagsOnBothEnds() {
		var l = CalendarLayout.of(def(ev("om", "2026-07-20", "2026-09-10")), 2026, 8);
		var segs = segmentsOf(l, "om");
		assertEquals(6, segs.size());                       // Aug 1 (row 0) .. Aug 31 (row 5)
		assertTrue(segs.get(0).continuesLeft());
		assertTrue(segs.get(segs.size() - 1).continuesRight());
		assertEquals("0:6-6:LR", shape(segs.get(0)));                 // clipped to Aug 1, the first visible day
		assertEquals("5:0-1:LR", shape(segs.get(segs.size() - 1)));   // clipped to Aug 31, the last visible day
	}

	@Test void c03_adjacentMonthCellsNeverCarryEvents() {
		// A span reaching back into July is clipped at Aug 1 rather than painted into the leading adjacent cells.
		var l = CalendarLayout.of(def(ev("j", "2026-07-28", "2026-08-03")), 2026, 8);
		assertEquals(List.of("0:6-6:LR", "1:0-1:L-"), shapes(segmentsOf(l, "j")));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Lanes.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_greedyFirstFit_overlappingSpansStack_nonOverlappingReuseLaneZero() {
		var l = CalendarLayout.of(def(
			ev("b", "2026-08-03", "2026-08-05"),
			ev("a", "2026-08-02", "2026-08-04"),
			ev("c", "2026-08-06", "2026-08-07")), 2026, 8);
		assertEquals(Map.of("a", 0, "b", 1, "c", 0), laneById(l));
		assertEquals(2, l.laneCount(1));
	}

	@Test void d02_laneAssignmentIsStableAcrossARerender() {
		var events = new CalendarEvent[]{
			ev("z", "2026-08-04", "2026-08-06"),
			ev("y", "2026-08-02", "2026-08-05"),
			ev("x", "2026-08-03", "2026-08-08")};
		var first = laneById(CalendarLayout.of(def(events), 2026, 8));
		var reversed = new ArrayList<>(Arrays.asList(events));
		Collections.reverse(reversed);
		var second = laneById(CalendarLayout.of(def(reversed.toArray(new CalendarEvent[0])), 2026, 8));
		assertEquals(first, second);
	}

	@Test void d03_lanesBeyondTheBudgetOverflowIntoMoreRatherThanGrowingTheRow() {
		var d = def(
			ev("s1", "2026-08-03", "2026-08-05"),
			ev("s2", "2026-08-03", "2026-08-05"),
			ev("s3", "2026-08-03", "2026-08-05")).maxPerDay(2);
		var l = CalendarLayout.of(d, 2026, 8);
		assertEquals(2, l.laneBudget());
		assertEquals(2, l.segmentsForWeek(1).size());       // only the seated pieces are drawn
		assertEquals(2, l.laneCount(1));
		// The unseated bar collapses into the "+N more" of every day it crosses - and nothing else is hidden.
		var cell = l.day(LocalDate.of(2026, 8, 4));
		assertEquals(1, cell.overflow());
		assertEquals(List.of("s3"), cell.hidden().stream().map(e -> e.id).toList());
	}

	private static Map<String,Integer> laneById(CalendarLayout.MonthLayout l) {
		var out = new LinkedHashMap<String,Integer>();
		for (var s : l.segments())
			out.put(s.event().id, s.lane());
		return out;
	}

	//------------------------------------------------------------------------------------------------------------------
	// The maxPerDay / lane split (spec §2.4).
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_maxPerDaySplit_barsDoNotConsumeTheChipBudget() {
		// A day crossed by TWO spanning bars plus FOUR single-day events, with maxPerDay = 3:
		// both bars draw, three chips draw, and the overflow control reads exactly "+1 more".
		var l = CalendarLayout.of(def(
			ev("bar1", "2026-08-03", "2026-08-06"),
			ev("bar2", "2026-08-02", "2026-08-05"),
			ev("c1", "2026-08-04", null),
			ev("c2", "2026-08-04", null),
			ev("c3", "2026-08-04", null),
			ev("c4", "2026-08-04", null)).maxPerDay(3), 2026, 8);

		assertEquals(2, l.segmentsForWeek(1).size());
		assertEquals(2, l.laneCount(1));

		var cell = l.day(LocalDate.of(2026, 8, 4));
		assertEquals(List.of("c1", "c2", "c3"), cell.chips().stream().map(e -> e.id).toList());
		assertEquals(1, cell.overflow());
		assertEquals(List.of("c4"), cell.hidden().stream().map(e -> e.id).toList());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Chip ordering.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_allDayChipsPrecedeTimedChips_timedAscend() {
		var l = CalendarLayout.of(def(
			ev("t2", "2026-08-04T12:00", null),
			ev("t1", "2026-08-04T08:00", null),
			ev("a1", "2026-08-04", null)).maxPerDay(9), 2026, 8);
		assertEquals(List.of("a1", "t1", "t2"), l.day(LocalDate.of(2026, 8, 4)).chips().stream().map(e -> e.id).toList());
	}

	@Test void f02_malformedEventsAreDroppedFromTheLayout() {
		var l = CalendarLayout.of(def(
			ev("good", "2026-08-04", null),
			CalendarEvent.create().id("bad").title("Bad").start("2026-08-04").allDay(true).end("2026-08-04T10:00")),
			2026, 8);
		assertEquals(List.of("good"), l.day(LocalDate.of(2026, 8, 4)).chips().stream().map(e -> e.id).toList());
	}
}
