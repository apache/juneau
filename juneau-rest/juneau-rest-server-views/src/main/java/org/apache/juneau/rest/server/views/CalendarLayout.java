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

import java.time.*;
import java.util.*;

import org.apache.juneau.rest.server.widgets.*;
import org.apache.juneau.rest.server.widgets.CalendarDef.*;

/**
 * The DOM-free month-layout engine behind {@link CalendarTable} &mdash; it turns a {@link CalendarDef}'s events into
 * per-week spanning-bar segments with lane assignments plus per-day chip lists with overflow counts.
 *
 * <p>
 * The same rules are mirrored by the {@code juneau-calendar.js} runtime, which lays out every month the server did
 * not paint.  Keeping the engine here (rather than inline in the emitter) is what makes the server-painted initial
 * month and the client-painted navigated months provably agree.
 *
 * <h5 class='section'>Spanning vs chips:</h5>
 * <ul>
 * 	<li>An event covering more than one day cell (see {@link CalendarEvent#spanning()}) renders as a <b>bar</b>,
 * 		emitted as <b>one {@link Segment} per week row</b> so a span crossing a week boundary is drawn as two
 * 		continuous pieces.  Every segment of a span carries the same {@link CalendarEvent}, so hover, focus, and the
 * 		legend filter act on the whole event from any piece.
 * 	<li>A single-day event (including a timed event with an omitted {@code end}, which is a zero-width instant on
 * 		its start day) renders as a <b>chip</b> inside its day cell.
 * </ul>
 *
 * <h5 class='section'>Clipping and continuation:</h5>
 * <p>
 * A span is clipped to the month's first/last <b>visible in-month</b> day, so adjacent-month cells still carry no
 * events.  {@link Segment#continuesLeft()}/{@link Segment#continuesRight()} are computed against the event's
 * <b>true</b> bounds, so a week-boundary cut and a month clip both raise them &mdash; a span longer than the
 * rendered month therefore clips with a continuation flag on both ends.
 *
 * <h5 class='section'>Two separate caps:</h5>
 * <p>
 * Bars consume <b>lanes</b> against the derived per-week budget ({@link CalendarDef#effectiveLaneBudget()}), while
 * {@link CalendarDef#maxPerDay} caps the <b>non-spanning chips</b> in a day cell.  A day's {@code "+N more"} counts
 * only what it actually hides: the chips over the cap plus any bar crossing that day that could not get a lane.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link CalendarTable}
 * 	<li class='jc'>{@link CalendarDef}
 * </ul>
 *
 * @since 10.0.0
 */
public class CalendarLayout {

	/** The number of week rows a month grid always renders. */
	public static final int GRID_WEEKS = 6;

	/** The number of day cells in a week row. */
	public static final int WEEK_DAYS = 7;

	/** The lane value of a bar the week's lane budget could not seat; it collapses into the day cells' "+N more". */
	public static final int LANE_OVERFLOW = -1;

	/**
	 * Chip order inside a day cell: all-day chips first, then timed chips ascending, with the event id as the final
	 * tie-break so the order is total and therefore stable across a re-render.
	 */
	public static final Comparator<CalendarEvent> CHIP_ORDER =
		Comparator.comparing((CalendarEvent e) -> e.effectiveAllDay() ? 0 : 1)
			.thenComparing(e -> e.start == null ? "" : e.start)
			.thenComparing(e -> e.id == null ? "" : e.id);

	private CalendarLayout() {}

	/**
	 * One drawn piece of a spanning bar, confined to a single week row.
	 *
	 * @param event The whole event this piece belongs to; every piece of a span shares it.
	 * @param week The 0-based week row.
	 * @param startColumn The 0-based first column of the piece within its week row.
	 * @param endColumn The 0-based last (inclusive) column of the piece within its week row.
	 * @param continuesLeft Whether the event begins before this piece (a week cut or a month clip).
	 * @param continuesRight Whether the event ends after this piece (a week cut or a month clip).
	 * @param lane The 0-based lane within the week row, or {@link #LANE_OVERFLOW} when the budget could not seat it.
	 */
	public record Segment(CalendarEvent event, int week, int startColumn, int endColumn, boolean continuesLeft,
			boolean continuesRight, int lane) {

		/** @return The number of day columns this piece covers. */
		public int columnSpan() {
			return endColumn - startColumn + 1;
		}

		/** @return Whether the week's lane budget could not seat this piece. */
		public boolean overflowed() {
			return lane == LANE_OVERFLOW;
		}
	}

	/**
	 * One in-month day cell's chip content.
	 *
	 * @param date The civil date of the cell.
	 * @param chips The chips actually drawn, in {@link #CHIP_ORDER}, at most {@code maxPerDay} of them.
	 * @param hidden What the {@code "+N more"} affordance hides: the chips over the cap, then any bar crossing this
	 * 	day that the week's lane budget could not seat.
	 */
	public record DayCell(LocalDate date, List<CalendarEvent> chips, List<CalendarEvent> hidden) {

		/** @return The {@code "+N more"} count &mdash; only what is actually hidden. */
		public int overflow() {
			return hidden.size();
		}
	}

	/**
	 * A whole month's computed layout.
	 *
	 * @param gridStart The civil date of the grid's first cell (an adjacent-month day unless the 1st lands on the
	 * 	week-start column).
	 * @param laneBudget The per-week-row spanning-bar lane budget actually applied.
	 * @param segments Every drawn bar piece, ordered by week row then lane.
	 * @param days The in-month day cells, keyed by civil date.
	 */
	public record MonthLayout(LocalDate gridStart, int laneBudget, List<Segment> segments,
			Map<LocalDate,DayCell> days) {

		/**
		 * The bar pieces drawn in one week row.
		 *
		 * @param week The 0-based week row.
		 * @return The pieces, ordered by lane; never <jk>null</jk>.
		 */
		public List<Segment> segmentsForWeek(int week) {
			var out = new ArrayList<Segment>();
			for (var s : segments)
				if (s.week() == week && !s.overflowed())
					out.add(s);
			return out;
		}

		/**
		 * The number of lanes a week row actually needs (its tallest seated lane plus one).
		 *
		 * @param week The 0-based week row.
		 * @return The lane count, or {@code 0} when the row draws no bars.
		 */
		public int laneCount(int week) {
			var n = 0;
			for (var s : segmentsForWeek(week))
				n = Math.max(n, s.lane() + 1);
			return n;
		}

		/**
		 * The chip content of one day cell.
		 *
		 * @param date The civil date.
		 * @return The cell, or <jk>null</jk> for an adjacent-month day (which never carries events).
		 */
		public DayCell day(LocalDate date) {
			return days.get(date);
		}
	}

	/**
	 * Lays out the given definition's well-formed events for a (year, month) window.
	 *
	 * @param def The calendar definition supplying the events, week start, chip cap, and lane budget.  Must not be
	 * 	<jk>null</jk>.
	 * @param year The year being rendered.
	 * @param month The 1-based month being rendered.
	 * @return The computed layout.  Never <jk>null</jk>.
	 */
	public static MonthLayout of(CalendarDef def, int year, int month) {
		var gridStart = gridStart(year, month, def.effectiveWeekStart());
		var monthStart = LocalDate.of(year, month, 1);
		var monthEnd = monthStart.plusMonths(1).minusDays(1);
		var laneBudget = def.effectiveLaneBudget();

		var chipsByDay = new HashMap<LocalDate,List<CalendarEvent>>();
		var spans = new ArrayList<CalendarEvent>();
		for (var e : def.wellFormedEvents()) {
			if (e.spanning())
				spans.add(e);
			else if (!e.civilStart().isBefore(monthStart) && !e.civilStart().isAfter(monthEnd))
				chipsByDay.computeIfAbsent(e.civilStart(), k -> new ArrayList<>()).add(e);
		}

		var segments = segments(spans, gridStart, monthStart, monthEnd, laneBudget);

		var days = new LinkedHashMap<LocalDate,DayCell>();
		for (var d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1))
			days.put(d, dayCell(d, chipsByDay.get(d), segments, gridStart, def.effectiveMaxPerDay()));

		return new MonthLayout(gridStart, laneBudget, List.copyOf(segments), Collections.unmodifiableMap(days));
	}

	/** The civil date of the grid's first cell for a (year, month) and week-start column. */
	public static LocalDate gridStart(int year, int month, WeekStart weekStart) {
		var first = LocalDate.of(year, month, 1);
		var dow = first.getDayOfWeek().getValue();  // MONDAY=1 .. SUNDAY=7
		return first.minusDays(weekStart == WeekStart.MONDAY ? dow - 1L : dow % 7L);
	}

	/** Cuts every span into per-week pieces, then seats the pieces of each week row into lanes. */
	private static List<Segment> segments(List<CalendarEvent> spans, LocalDate gridStart, LocalDate monthStart,
			LocalDate monthEnd, int laneBudget) {
		var out = new ArrayList<Segment>();
		for (var w = 0; w < GRID_WEEKS; w++) {
			var rowStart = gridStart.plusDays((long) w * WEEK_DAYS);
			var rowEnd = rowStart.plusDays(WEEK_DAYS - 1L);
			var row = new ArrayList<Segment>();
			for (var e : spans) {
				// Clip to the month's visible in-month days first, so adjacent cells still carry no events.
				var from = max(e.civilStart(), monthStart, rowStart);
				var to = min(e.lastDay(), monthEnd, rowEnd);
				if (from.isAfter(to))
					continue;
				row.add(new Segment(e, w, (int)(from.toEpochDay() - rowStart.toEpochDay()),
					(int)(to.toEpochDay() - rowStart.toEpochDay()), from.isAfter(e.civilStart()),
					to.isBefore(e.lastDay()), LANE_OVERFLOW));
			}
			out.addAll(seatLanes(row, laneBudget));
		}
		return out;
	}

	/**
	 * Greedy first-fit lane sweep over one week row.
	 *
	 * <p>
	 * Pieces are swept left to right (ties broken by start value then event id, so the sweep order is total) and
	 * each is seated in the lowest lane whose previous occupant ends before it begins.  A piece whose lane would
	 * reach the budget is left at {@link #LANE_OVERFLOW} rather than growing the row.
	 */
	private static List<Segment> seatLanes(List<Segment> row, int laneBudget) {
		row.sort(Comparator.comparingInt(Segment::startColumn)
			.thenComparing(s -> s.event().start == null ? "" : s.event().start)
			.thenComparing(s -> s.event().id == null ? "" : s.event().id));
		var laneEnds = new ArrayList<Integer>();
		var out = new ArrayList<Segment>();
		for (var s : row) {
			var lane = LANE_OVERFLOW;
			for (var i = 0; i < laneEnds.size(); i++) {
				if (laneEnds.get(i) < s.startColumn()) {
					lane = i;
					break;
				}
			}
			if (lane == LANE_OVERFLOW && laneEnds.size() < laneBudget) {
				lane = laneEnds.size();
				laneEnds.add(s.endColumn());
			} else if (lane != LANE_OVERFLOW) {
				laneEnds.set(lane, s.endColumn());
			}
			out.add(new Segment(s.event(), s.week(), s.startColumn(), s.endColumn(), s.continuesLeft(),
				s.continuesRight(), lane));
		}
		out.sort(Comparator.comparingInt(Segment::lane).thenComparingInt(Segment::startColumn));
		return out;
	}

	/** Builds one in-month day cell: capped chips plus what the "+N more" affordance hides. */
	private static DayCell dayCell(LocalDate date, List<CalendarEvent> dayChips, List<Segment> segments,
			LocalDate gridStart, int maxPerDay) {
		var all = dayChips == null ? new ArrayList<CalendarEvent>() : new ArrayList<>(dayChips);
		all.sort(CHIP_ORDER);
		var shown = Math.min(Math.max(maxPerDay, 0), all.size());
		var chips = new ArrayList<>(all.subList(0, shown));
		var hidden = new ArrayList<>(all.subList(shown, all.size()));
		for (var s : segments)
			if (s.overflowed() && covers(s, date, gridStart) && !hidden.contains(s.event()))
				hidden.add(s.event());
		return new DayCell(date, List.copyOf(chips), List.copyOf(hidden));
	}

	/** Whether a bar piece crosses the given day. */
	private static boolean covers(Segment s, LocalDate date, LocalDate gridStart) {
		var offset = date.toEpochDay() - gridStart.plusDays((long) s.week() * WEEK_DAYS).toEpochDay();
		return offset >= s.startColumn() && offset <= s.endColumn();
	}

	private static LocalDate max(LocalDate a, LocalDate b, LocalDate c) {
		var m = a.isAfter(b) ? a : b;
		return m.isAfter(c) ? m : c;
	}

	private static LocalDate min(LocalDate a, LocalDate b, LocalDate c) {
		var m = a.isBefore(b) ? a : b;
		return m.isBefore(c) ? m : c;
	}
}
